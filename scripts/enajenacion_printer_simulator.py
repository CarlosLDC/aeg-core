#!/usr/bin/env python3
"""
Simulador de impresora fiscal AEG para pruebas del protocolo de enajenación MQTT.

Responde automáticamente a los comandos publicados por AEG Core en
  /{mac}/AEG_Fiscal/Integracion/Comando
y envía respuestas a
  /{mac}/AEG_Fiscal/Integracion/Respuesta
(solo ptrEnajenar va a CmdServer)

Requisitos:
  pip install -r scripts/requirements-mqtt-sim.txt

Ejemplo (broker local sin auth):
  python3 scripts/enajenacion_printer_simulator.py \\
    --mac 20:6E:F1:88:4C:68 \\
    --fiscal-serial GRA0000017 \\
    --broker tcp://localhost:1883 \\
    --initiate

Reproducir latencia en paso 3a (timeout con default 120 s en core):
  python3 scripts/enajenacion_printer_simulator.py ... --delay-fiscal-rif-ms 150000

Éxito tardío con timeout ampliado (p. ej. MQTT_ENAJENACION_TIMEOUT_FISCAL_RIF=300):
  python3 scripts/enajenacion_printer_simulator.py ... --delay-fiscal-rif-ms 90000

Variables de entorno (alternativa a flags): MQTT_HOST, MQTT_PORT, MQTT_USER, MQTT_PASS
"""
from __future__ import annotations

import argparse
import json
import os
import signal
import sys
import time
from pathlib import Path

DNF_END_OK = 7
INVOICE_END_OK = 8
CREDIT_NOTE_END_OK = 10
SUBTOTAL_DATA_D = 555
PROD_NC_LINE_DATA_D = 9


def load_env_file(path: Path) -> None:
    if not path.exists():
        return
    for line in path.read_text().splitlines():
        line = line.strip()
        if not line or line.startswith("#") or "=" not in line:
            continue
        key, value = line.split("=", 1)
        key = key.strip()
        value = value.strip().strip('"').strip("'")
        if key and value and key not in os.environ:
            os.environ[key] = value


def compact_mac(mac: str) -> str:
    return mac.replace(":", "").upper()


def colon_mac(mac: str) -> str:
    compact = compact_mac(mac)
    if len(compact) != 12:
        raise ValueError(f"MAC inválida: {mac}")
    return ":".join(compact[i : i + 2] for i in range(0, 12, 2))


def parse_broker(url: str) -> tuple[str, int]:
    raw = url.strip()
    for prefix in ("tcp://", "mqtt://", "ssl://", "mqtts://"):
        if raw.startswith(prefix):
            raw = raw[len(prefix) :]
            break
    if ":" in raw:
        host, port_str = raw.rsplit(":", 1)
        return host, int(port_str)
    return raw, 1883


def dnf_success() -> list[dict]:
    items = [{"cmd": "aperDNF", "code": 0, "dataD": 0}]
    items.append({"cmd": "efeNeDAnJuCeDNF", "code": 0, "dataD": 0})
    items.extend({"cmd": "efeNoDAnJuCeDNF", "code": 0, "dataD": 0} for _ in range(8))
    items.append({"cmd": "endDNF", "code": 0, "dataD": DNF_END_OK})
    return items


def invoice_success() -> list[dict]:
    items = [{"cmd": "proF", "code": 0, "dataD": 0} for _ in range(5)]
    items.append({"cmd": "subToF", "code": 0, "dataD": SUBTOTAL_DATA_D})
    items.append({"cmd": "fpaF", "code": 0, "dataD": 0})
    items.append({"cmd": "endFac", "code": 0, "dataD": INVOICE_END_OK})
    return items


def credit_note_success() -> list[dict]:
    items = [
        {"cmd": "nroFacNC", "code": 0, "dataD": 0},
        {"cmd": "fechFacNC", "code": 0, "dataD": 0},
        {"cmd": "conSerNC", "code": 0, "dataD": 0},
        {"cmd": "rifCiNC", "code": 0, "dataD": 0},
        {"cmd": "razSocNC", "code": 0, "dataD": 0},
    ]
    items.extend({"cmd": "prodNC", "code": 0, "dataD": PROD_NC_LINE_DATA_D} for _ in range(5))
    items.append({"cmd": "endPoNC", "code": 0, "dataD": SUBTOTAL_DATA_D})
    items.append({"cmd": "fpaNC", "code": 0, "dataD": 0})
    items.append({"cmd": "endNC", "code": 0, "dataD": CREDIT_NOTE_END_OK})
    return items


def object_success(cmd: str) -> dict:
    return {"cmd": cmd, "code": 0, "dataD": 0}


def classify_command(payload: str) -> str:
    data = json.loads(payload)
    if isinstance(data, list):
        if not data:
            return "unknown"
        first_cmd = data[0].get("cmd", "")
        if first_cmd == "aperDNF":
            return "dnf"
        if first_cmd == "proF":
            return "invoice"
        if first_cmd == "nroFacNC":
            return "credit_note"
        return "array:" + first_cmd
    if isinstance(data, dict):
        cmd = data.get("cmd", "")
        if cmd == "fiscalAEG":
            return "fiscal_rif"
        if cmd == "wFileSPIFF":
            name = (data.get("data") or {}).get("nameFile", "")
            if name == "paramFacSPIFF.json":
                return "header"
            if name == "configSPIFFS.json":
                return "config"
            return "wfile:" + name
        if cmd == "genImpRepZ":
            return "report_z"
        if cmd == "StaInf":
            return "reg_status"
        return "object:" + cmd
    return "unknown"


def sta_inf_success(fiscal_serial: str) -> dict:
    return {"cmd": " StaInf ", "code": 0, "dataS": fiscal_serial}


def build_response(kind: str, fiscal_serial: str) -> str:
    if kind == "dnf":
        return json.dumps(dnf_success(), separators=(",", ":"))
    if kind == "fiscal_rif":
        return json.dumps(object_success("fiscalAEG"), separators=(",", ":"))
    if kind in ("header", "config"):
        return json.dumps(object_success("wFileSPIFF"), separators=(",", ":"))
    if kind == "reg_status":
        return json.dumps(sta_inf_success(fiscal_serial), separators=(",", ":"))
    if kind == "invoice":
        return json.dumps(invoice_success(), separators=(",", ":"))
    if kind == "credit_note":
        return json.dumps(credit_note_success(), separators=(",", ":"))
    if kind == "report_z":
        return json.dumps(object_success("genImpRepZ"), separators=(",", ":"))
    raise ValueError(f"Comando no soportado por el simulador: {kind}")


def ptr_enajenar_payload(fiscal_serial: str, mac: str) -> str:
    body = {
        "cmd": "ptrEnajenar",
        "data": {"ptrReg": fiscal_serial, "macAddr": colon_mac(mac)},
    }
    return json.dumps(body, separators=(",", ":"))


def main() -> int:
    root = Path(__file__).resolve().parent.parent
    load_env_file(root / ".env")

    parser = argparse.ArgumentParser(description="Simulador MQTT impresora AEG (enajenación)")
    parser.add_argument("--mac", required=True, help="MAC impresora, ej. 20:6E:F1:88:4C:68")
    parser.add_argument("--fiscal-serial", required=True, help="Registro fiscal, ej. GRA0000017")
    parser.add_argument(
        "--broker",
        default=os.getenv("MQTT_URL") or f"tcp://{os.getenv('MQTT_HOST', 'localhost')}:{os.getenv('MQTT_PORT', '1883')}",
        help="URL broker MQTT",
    )
    parser.add_argument("--user", default=os.getenv("MQTT_USER"))
    parser.add_argument("--password", default=os.getenv("MQTT_PASS"))
    parser.add_argument(
        "--initiate",
        action="store_true",
        help="Publica ptrEnajenar al conectar (simula arranque de impresora)",
    )
    parser.add_argument("--client-id", default=None, help="Client ID MQTT (default auto)")
    parser.add_argument(
        "--delay-ms",
        type=int,
        default=0,
        help="Retraso global (ms) antes de publicar cada respuesta simulada",
    )
    parser.add_argument(
        "--delay-fiscal-rif-ms",
        type=int,
        default=None,
        help="Retraso (ms) solo para fiscalAEG (paso 3a); por defecto usa --delay-ms",
    )
    args = parser.parse_args()

    def response_delay_seconds(kind: str) -> float:
        if kind == "fiscal_rif" and args.delay_fiscal_rif_ms is not None:
            return max(0, args.delay_fiscal_rif_ms) / 1000.0
        return max(0, args.delay_ms) / 1000.0

    try:
        import paho.mqtt.client as mqtt
    except ImportError:
        print("Instala dependencias: pip install -r scripts/requirements-mqtt-sim.txt", file=sys.stderr)
        return 2

    mac_compact = compact_mac(args.mac)
    cmd_server = f"/{mac_compact}/AEG_Fiscal/Integracion/CmdServer"
    respuesta = f"/{mac_compact}/AEG_Fiscal/Integracion/Respuesta"
    comando = f"/{mac_compact}/AEG_Fiscal/Integracion/Comando"
    host, port = parse_broker(args.broker)
    client_id = args.client_id or f"aeg-printer-sim-{mac_compact.lower()}"

    print(f"Broker: {host}:{port}")
    print(f"Suscribiendo: {comando}")
    print(f"Respuestas en: {respuesta}")

    def on_connect(client, userdata, flags, rc, properties=None):
        if rc != 0:
            print(f"Error de conexión MQTT rc={rc}", file=sys.stderr)
            return
        client.subscribe(comando, qos=1)
        print("Conectado y suscrito.")
        if args.initiate:
            payload = ptr_enajenar_payload(args.fiscal_serial, args.mac)
            client.publish(cmd_server, payload, qos=1)
            print(f"Publicado ptrEnajenar -> {cmd_server}")

    def on_message(client, userdata, msg):
        payload = msg.payload.decode("utf-8", errors="replace")
        print(f"\n<< Comando recibido [{msg.topic}]")
        print(payload[:500] + ("..." if len(payload) > 500 else ""))
        try:
            kind = classify_command(payload)
            delay_s = response_delay_seconds(kind)
            if delay_s > 0:
                print(f"… esperando {delay_s:.1f}s antes de responder ({kind})")
                time.sleep(delay_s)
            response = build_response(kind, args.fiscal_serial)
            client.publish(respuesta, response, qos=1)
            print(f">> Respuesta ({kind}) publicada en {respuesta}")
        except Exception as ex:
            print(f"Error procesando comando: {ex}", file=sys.stderr)

    if hasattr(mqtt, "CallbackAPIVersion"):
        client = mqtt.Client(
            mqtt.CallbackAPIVersion.VERSION2,
            client_id=client_id,
            protocol=mqtt.MQTTv311,
        )
    else:
        client = mqtt.Client(client_id=client_id, protocol=mqtt.MQTTv311)

    if args.user:
        client.username_pw_set(args.user, args.password or "")

    client.on_connect = on_connect
    client.on_message = on_message

    def shutdown(_signum, _frame):
        print("\nDeteniendo simulador...")
        client.loop_stop()
        client.disconnect()
        sys.exit(0)

    signal.signal(signal.SIGINT, shutdown)
    signal.signal(signal.SIGTERM, shutdown)

    try:
        client.connect(host, port, keepalive=60)
    except Exception as ex:
        print(f"No se pudo conectar al broker: {ex}", file=sys.stderr)
        return 3

    client.loop_start()
    print("Simulador activo. Ctrl+C para salir.")
    while True:
        time.sleep(1)


if __name__ == "__main__":
    raise SystemExit(main())
