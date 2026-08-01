#!/usr/bin/env python3
"""
Simulador de impresora fiscal AEG para fiscalización remota MQTT.

1) Publica ptrFiscalizar en CmdServer (--initiate)
2) Escucha RxPtrFiscalizarRemoto en Comando
3) Responde en Respuesta (éxito o error) tras un delay opcional

Requisitos:
  pip install -r scripts/requirements-mqtt-sim.txt

Ejemplo:
  python3 scripts/fiscalizacion_printer_simulator.py \\
    --mac 20:6E:F1:88:4C:68 \\
    --fiscal-serial GRA0000017 \\
    --precinto G1B0033 \\
    --precinto-color Azul \\
    --model AEG-R1 \\
    --firmware-version 1.1.0 \\
    --broker tcp://localhost:1883 \\
    --initiate

Latencia del paso resultado (>1 min en hardware real):
  python3 scripts/fiscalizacion_printer_simulator.py ... --delay-result-ms 90000
"""
from __future__ import annotations

import argparse
import json
import os
import signal
import sys
import time
from pathlib import Path


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


def ptr_fiscalizar_payload(
    fiscal_serial: str,
    mac: str,
    precinto: str,
    color: str,
    firmware: str,
    model: str,
) -> str:
    body = {
        "cmd": "ptrFiscalizar",
        "data": {
            "ptrReg": fiscal_serial,
            "macAddr": colon_mac(mac),
            "PrecintoNro": precinto,
            "PrecintoColor": color,
            "firmwareVersion": firmware,
            "model": model,
        },
    }
    return json.dumps(body, separators=(",", ":"))


def result_success() -> str:
    return json.dumps(
        {"cmd": "RxPtrFiscalizarRemoto", "code": 0, "dataS": {"error": "Impresora Fiscalizando"}},
        separators=(",", ":"),
    )


def result_error() -> str:
    return json.dumps(
        {"cmd": "RxPtrFiscalizarRemoto", "code": 1, "dataS": {"error": "ERROR Fiscalizando"}},
        separators=(",", ":"),
    )


def main() -> int:
    root = Path(__file__).resolve().parent.parent
    load_env_file(root / ".env")

    parser = argparse.ArgumentParser(description="Simulador MQTT impresora AEG (fiscalización)")
    parser.add_argument("--mac", required=True, help="MAC impresora, ej. 20:6E:F1:88:4C:68")
    parser.add_argument("--fiscal-serial", required=True, help="Registro fiscal, ej. GRA0000017")
    parser.add_argument("--precinto", required=True, help="Serial del precinto, ej. G1B0033")
    parser.add_argument("--precinto-color", default="Azul", help="Color del precinto (default Azul)")
    parser.add_argument("--model", default="AEG-R1", help="codigo_modelo en BD")
    parser.add_argument("--firmware-version", default="1.1.0")
    parser.add_argument(
        "--broker",
        default=os.getenv("MQTT_URL")
        or f"tcp://{os.getenv('MQTT_HOST', 'localhost')}:{os.getenv('MQTT_PORT', '1883')}",
    )
    parser.add_argument("--user", default=os.getenv("MQTT_USER"))
    parser.add_argument("--password", default=os.getenv("MQTT_PASS"))
    parser.add_argument(
        "--initiate",
        action="store_true",
        help="Publica ptrFiscalizar al conectar (simula solicitud de la impresora)",
    )
    parser.add_argument(
        "--delay-result-ms",
        type=int,
        default=0,
        help="Retraso (ms) antes de publicar la respuesta de resultado en Respuesta",
    )
    parser.add_argument(
        "--fail-result",
        action="store_true",
        help="Publica resultado con code=1 en lugar de éxito",
    )
    parser.add_argument("--client-id", default=None)
    args = parser.parse_args()

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
    client_id = args.client_id or f"aeg-fiscalizacion-sim-{mac_compact.lower()}"

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
            payload = ptr_fiscalizar_payload(
                args.fiscal_serial,
                args.mac,
                args.precinto,
                args.precinto_color,
                args.firmware_version,
                args.model,
            )
            client.publish(cmd_server, payload, qos=1)
            print(f"Publicado ptrFiscalizar -> {cmd_server}")

    def on_message(client, userdata, msg):
        payload = msg.payload.decode("utf-8", errors="replace")
        print(f"\n<< Comando recibido [{msg.topic}]")
        print(payload[:500] + ("..." if len(payload) > 500 else ""))
        try:
            data = json.loads(payload)
        except json.JSONDecodeError as ex:
            print(f"JSON inválido: {ex}", file=sys.stderr)
            return
        if not isinstance(data, dict) or data.get("cmd") != "RxPtrFiscalizarRemoto":
            print("Ignorado (no es RxPtrFiscalizarRemoto)")
            return
        code = data.get("code")
        msj = (data.get("data") or {}).get("msj")
        print(f"ACK code={code} msj={msj}")
        if code != 0:
            print("Validación falló en servidor; no se publica resultado.")
            return
        delay_s = max(0, args.delay_result_ms) / 1000.0
        if delay_s > 0:
            print(f"… esperando {delay_s:.1f}s antes de responder resultado")
            time.sleep(delay_s)
        response = result_error() if args.fail_result else result_success()
        client.publish(respuesta, response, qos=1)
        print(f">> Resultado publicado en {respuesta}")

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
    print("Simulador de fiscalización activo. Ctrl+C para salir.")
    while True:
        time.sleep(1)


if __name__ == "__main__":
    raise SystemExit(main())
