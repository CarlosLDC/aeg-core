#!/usr/bin/env python3
"""
Fase 0 — Sonda USB para impresoras nuevas AEG (fiscalización / provisión de broker).

Uso:
  python3 scripts/tools_usb_probe_fiscalizacion.py --port COM3
  python3 scripts/tools_usb_probe_fiscalizacion.py --port /dev/ttyUSB0 --set-new-broker
"""

import argparse
import json
import re
import sys
import time

import serial

sys.stdout.reconfigure(encoding="utf-8", errors="replace")

NEW_BROKER_PAYLOAD = {
    "cmd": "configMqttAEG",
    "data": {
        "addrBroker": "206.189.231.128",
        "portBroker": 1883,
        "userBroker": "aegptrfiscal2024",
        "passwBroker": "aegseniat2024",
        "topicRx": "Comando",
        "topicTx": "Respuesta",
        "topicDoc": "Documento",
    },
}


def extract_json(buf: bytes) -> tuple[dict | None, bytes]:
    text = buf.decode("utf-8", "replace")
    start = next((i for i, ch in enumerate(text) if not ch.isspace()), -1)
    if start < 0 or text[start] != "{":
        return None, buf
    depth = 0
    in_string = False
    escaped = False
    for index in range(start, len(text)):
        ch = text[index]
        if in_string:
            if escaped:
                escaped = False
            elif ch == "\\":
                escaped = True
            elif ch == '"':
                in_string = False
            continue
        if ch == '"':
            in_string = True
            continue
        if ch == "{":
            depth += 1
            continue
        if ch == "}":
            depth -= 1
            if depth == 0:
                obj = json.loads(text[start : index + 1])
                rest = text[index + 1 :].lstrip("\r\n").encode("utf-8")
                return obj, rest
    return None, buf


def exchange(port: str, baud: int, cmd: dict, timeout: float = 12.0) -> dict | None:
    payload = (json.dumps(cmd, separators=(",", ":")) + "\n").encode("utf-8")
    ser = serial.Serial(port, baud, timeout=0.5, write_timeout=2)
    try:
        ser.reset_input_buffer()
        ser.write(payload)
        ser.flush()
        deadline = time.time() + timeout
        buf = b""
        while time.time() < deadline:
            chunk = ser.read(ser.in_waiting or 1)
            if chunk:
                buf += chunk
                obj, buf = extract_json(buf)
                if obj is not None:
                    return obj
            else:
                time.sleep(0.05)
        if buf:
            print(f"  PARTIAL: {buf.decode('utf-8', 'replace')[:500]!r}")
        return None
    finally:
        ser.close()


def extract_mac_from_response(resp: dict | None) -> str | None:
    if not resp:
        return None
    raw = json.dumps(resp)
    m = re.search(r"/([0-9A-Fa-f]{12})/AEG_Fiscal", raw)
    if m:
        compact = m.group(1).upper()
        return ":".join(compact[i : i + 2] for i in range(0, 12, 2))
    return None


def main() -> None:
    parser = argparse.ArgumentParser(description="Sonda USB de diagnóstico y broker para impresoras AEG")
    parser.add_argument("--port", default="COM3", help="Puerto serial (ej. COM3 o /dev/ttyUSB0)")
    parser.add_argument("--baud", type=int, default=115200, help="Baudrate (default: 115200)")
    parser.add_argument(
        "--set-new-broker",
        action="store_true",
        help="Enviar configMqttAEG por USB apuntando al broker nuevo y verificar con EstCredMqtt1",
    )
    args = parser.parse_args()

    steps = [
        ("1. Estado general (Wi-Fi / IP)", {"cmd": "StaInf", "data": {"status": "StaConexionSinDNF"}}),
        ("2. Credenciales MQTT y MAC (EstCredMqtt1)", {"cmd": "StaInf", "data": {"status": "EstCredMqtt1"}}),
        ("3. Versión de firmware (statFirmSinDNF)", {"cmd": "statFirmSinDNF", "data": 0}),
    ]

    for label, cmd in steps:
        print(f"\n=== {label}")
        print(f"TX: {json.dumps(cmd, ensure_ascii=False)}")
        resp = exchange(args.port, args.baud, cmd)
        if resp is None:
            print("RX: TIMEOUT / sin respuesta")
            continue
        print(f"RX: {json.dumps(resp, ensure_ascii=False)}")
        mac = extract_mac_from_response(resp)
        if mac:
            print(f"  -> MAC detectada en topic: {mac}")

    if args.set_new_broker:
        print("\n=== 4. Configurar broker nuevo por USB (configMqttAEG)")
        print(f"TX: {json.dumps(NEW_BROKER_PAYLOAD, ensure_ascii=False)}")
        resp = exchange(args.port, args.baud, NEW_BROKER_PAYLOAD, timeout=20.0)
        print(f"RX: {json.dumps(resp, ensure_ascii=False) if resp else 'TIMEOUT / sin respuesta'}")

        print("\n=== 5. Re-verificar broker (EstCredMqtt1)")
        verify = exchange(args.port, args.baud, {"cmd": "StaInf", "data": {"status": "EstCredMqtt1"}})
        print(f"RX: {json.dumps(verify, ensure_ascii=False) if verify else 'TIMEOUT / sin respuesta'}")


if __name__ == "__main__":
    main()
