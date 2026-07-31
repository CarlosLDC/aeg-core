import json
import sys
import time

import serial

sys.stdout.reconfigure(encoding="utf-8", errors="replace")

PORT = "COM3"
BAUD = 115200


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


def exchange(cmd: dict, timeout: float = 12.0) -> dict | None:
    payload = (json.dumps(cmd, separators=(",", ":")) + "\n").encode("utf-8")
    ser = serial.Serial(PORT, BAUD, timeout=0.5, write_timeout=2)
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
            print(f"PARTIAL: {buf.decode('utf-8', 'replace')[:500]!r}")
        return None
    finally:
        ser.close()


def run(label: str, cmd: dict) -> None:
    print(f"\n=== {label}")
    print(f"TX: {cmd}")
    resp = exchange(cmd)
    if resp is None:
        print("TIMEOUT / sin respuesta")
        return
    print(f"RX: {json.dumps(resp, ensure_ascii=False)}")


if __name__ == "__main__":
    run(
        "status",
        {"cmd": "StaInf", "data": {"status": "StaConexionSinDNF"}},
    )
    run(
        "header",
        {"cmd": "StaInf", "data": {"status": "staEncFij"}},
    )
    run(
        "footer",
        {"cmd": "StaInf", "data": {"status": "staPieFij"}},
    )
    run(
        "last Z",
        {"cmd": "getRepZ", "data": -1},
    )
