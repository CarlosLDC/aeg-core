import time

import serial

CMD_VARIANTS = [
    b'{"cmd":"StaInf","data":{"status":"StaConexionSinDNF"}}\n',
    b'{"cmd":"StaInf","data":{"status":"StaConexionSinDNF"}}\r\n',
    b'{"cmd":"StaInf","data":{"status":"StaConexionSinDNF"}}',
]


def dump(label: str, data: bytes) -> None:
    hexed = data[:120].hex(" ")
    ascii_safe = "".join(chr(b) if 32 <= b < 127 else "." for b in data[:120])
    print(f"{label}: len={len(data)} hex={hexed}")
    print(f"  ascii={ascii_safe!r}")


def probe(baud: int, cmd: bytes) -> None:
    print(f"\n=== baud={baud} ending={cmd[-2:]!r}")
    try:
        ser = serial.Serial("COM3", baud, timeout=0.5, write_timeout=2)
    except Exception as exc:
        print(f"open fail: {exc}")
        return
    try:
        ser.reset_input_buffer()
        ser.write(cmd)
        ser.flush()
        deadline = time.time() + 4
        buf = b""
        while time.time() < deadline:
            chunk = ser.read(ser.in_waiting or 1)
            if chunk:
                buf += chunk
            else:
                time.sleep(0.05)
        dump("RX", buf)
    finally:
        ser.close()


if __name__ == "__main__":
    for baud in (115200, 19200, 57600):
        for cmd in CMD_VARIANTS:
            probe(baud, cmd)
