#!/usr/bin/env bash
# Diagnóstico del paso 3a (fiscalAEG) durante enajenación MQTT.
#
# Uso:
#   ./scripts/enajenacion_diag_step3a.sh --mac 206EF1884C68 --api http://localhost:8080
#   ./scripts/enajenacion_diag_step3a.sh --mac 20:6E:F1:88:4C:68 --api https://api.example.com --token "$JWT"
#
# En paralelo (otra terminal), suscribirse a respuestas de la impresora:
#   mosquitto_sub -h "$MQTT_HOST" -p 1883 -u "$MQTT_USER" -P "$MQTT_PASS" \
#     -t "/206EF1884C68/AEG_Fiscal/Integracion/Respuesta" -v

set -euo pipefail

API_BASE=""
MAC=""
TOKEN=""
INTERVAL=5
MAX_ROUNDS=36

usage() {
  sed -n '2,12p' "$0" | sed 's/^# \{0,1\}//'
  exit 1
}

compact_mac() {
  echo "$1" | tr -d ':' | tr '[:lower:]' '[:upper:]'
}

while [[ $# -gt 0 ]]; do
  case "$1" in
    --api) API_BASE="${2%/}"; shift 2 ;;
    --mac) MAC="$2"; shift 2 ;;
    --token) TOKEN="$2"; shift 2 ;;
    --interval) INTERVAL="$2"; shift 2 ;;
    --max-rounds) MAX_ROUNDS="$2"; shift 2 ;;
    -h|--help) usage ;;
    *) echo "Opción desconocida: $1" >&2; usage ;;
  esac
done

[[ -n "$API_BASE" && -n "$MAC" ]] || usage

MAC_COMPACT="$(compact_mac "$MAC")"
AUTH_HEADER=()
if [[ -n "$TOKEN" ]]; then
  AUTH_HEADER=(-H "Authorization: Bearer $TOKEN")
fi

fetch_json() {
  curl -sfS "${AUTH_HEADER[@]}" "$1"
}

print_section() {
  echo ""
  echo "=== $1 ==="
}

for ((round = 1; round <= MAX_ROUNDS; round++)); do
  print_section "$(date -Iseconds) — ronda $round/$MAX_ROUNDS"
  echo "MAC: $MAC_COMPACT"

  echo ""
  echo "--- Sesiones activas ---"
  sessions="$(fetch_json "$API_BASE/api/mqtt/enajenacion/sessions" || echo '[]')"
  echo "$sessions" | python3 -c "
import json, sys
mac = '$MAC_COMPACT'
data = json.load(sys.stdin)
matches = [s for s in data if s.get('mac','').upper() == mac]
if not matches:
    print('(sin sesión activa para esta MAC)')
else:
    for s in matches:
        print(json.dumps(s, indent=2))
"

  echo ""
  echo "--- Actividad reciente (últimas 30 entradas) ---"
  activity="$(fetch_json "$API_BASE/api/mqtt/enajenacion/activity?mac=$MAC_COMPACT&limit=30" || echo '{"entries":[]}')"
  echo "$activity" | python3 -c "
import json, sys
data = json.load(sys.stdin)
entries = data.get('entries', [])
for e in entries[:15]:
    ts = e.get('recordedAt') or e.get('at') or ''
    direction = e.get('direction', '?')
    result = e.get('result', '?')
    state = e.get('sessionState') or ''
    detail = (e.get('detail') or '')[:120]
    topic = e.get('topic') or ''
    print(f'{ts}  {direction:8} {result:10} state={state:20} {detail}')
    if topic:
        print(f'  topic: {topic}')
"

  state="$(echo "$sessions" | python3 -c "
import json, sys
mac = '$MAC_COMPACT'
data = json.load(sys.stdin)
for s in data:
    if s.get('mac','').upper() == mac:
        print(s.get('state',''))
        break
" 2>/dev/null || true)"

  if [[ "$state" == "FISCAL_RIF_SENT" ]]; then
    echo ""
    echo ">> Backend esperando respuesta fiscalAEG (paso 3a)."
    echo ">> Verifica mosquitto_sub en /$MAC_COMPACT/AEG_Fiscal/Integracion/Respuesta"
  elif [[ -z "$state" ]]; then
    failed="$(echo "$activity" | python3 -c "
import json, sys
data = json.load(sys.stdin)
for e in data.get('entries', []):
    if e.get('result') == 'FAILED':
        print(e.get('detail') or 'FAILED')
        break
" 2>/dev/null || true)"
    if [[ -n "$failed" ]]; then
      echo ""
      echo ">> Sesión terminada. Último FAILED: $failed"
      exit 0
    fi
  fi

  sleep "$INTERVAL"
done

echo ""
echo "Fin del monitoreo (${MAX_ROUNDS} rondas). Revisa actividad IGNORED/FAILED y mosquitto_sub."
