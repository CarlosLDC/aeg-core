import json
import time

import serial

CMD = b'{"cmd":"StaInf","data":{"status":"StaConexionSinDNF"}}\n'
BAUDS = [19200, 9600, 57600, 38400, 115200]


def probe(baud: int) -> None:
    print(f"=== baud {baud}")
    try:
        ser = serial.Serial("COM3", baud, timeout=1, write_timeout=2)
    except Exception as exc:
        print(f"open fail: {exc}")
        return

    ser.reset_input_buffer()
    ser.write(CMD)
    ser.flush()
    deadline = time.time() + 5
    buf = b""
    got = False
    while time.time() < deadline:
        chunk = ser.read(ser.in_waiting or 1)
        if chunk:
            buf += chunk
            if b"\n" in buf:
                line = buf.split(b"\n", 1)[0].decode("utf-8", "replace").strip()
                print(f"RX: {line[:400]}")
                try:
                    print("JSON:", json.dumps(json.loads(line), ensure_ascii=False)[:400])
                except Exception:
                    pass
                got = True
                break
        else:
            time.sleep(0.05)
    if not got:
        print("no response")
        if buf:
            print(f"raw bytes: {buf[:80]!r}")
    ser.close()


if __name__ == "__main__":
    for baud in BAUDS:
        probe(baud)
