import json
import sys
import time

import serial

PORT = "COM3"
BAUD = 115200
CMD = '{"cmd":"StaInf","data":{"status":"StaConexionSinDNF"}}\n'


def main() -> int:
    print(f"Opening {PORT} @ {BAUD}...")
    try:
        ser = serial.Serial(PORT, BAUD, timeout=2, write_timeout=2)
    except Exception as exc:
        print(f"OPEN_FAIL: {exc}")
        return 1

    ser.reset_input_buffer()
    ser.write(CMD.encode("utf-8"))
    ser.flush()
    print("Sent StaInf status request")

    deadline = time.time() + 15
    buf = b""
    lines: list[str] = []

    while time.time() < deadline:
        chunk = ser.read(ser.in_waiting or 1)
        if chunk:
            buf += chunk
            while b"\n" in buf:
                line, buf = buf.split(b"\n", 1)
                text = line.decode("utf-8", "replace").strip()
                if not text:
                    continue
                lines.append(text)
                print(f"RX: {text[:500]}")
                try:
                    obj = json.loads(text)
                except Exception:
                    continue
                if obj.get("cmd") == "StaInf":
                    print("OK_STATUS_RESPONSE")
                    print(json.dumps(obj, ensure_ascii=False)[:800])
                    ser.close()
                    return 0
        else:
            time.sleep(0.05)

    ser.close()
    print(f"TIMEOUT; lines={len(lines)}")
    return 2


if __name__ == "__main__":
    raise SystemExit(main())
