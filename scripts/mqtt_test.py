#!/usr/bin/env python3
import os
import time
import sys
import socket
from pathlib import Path

def load_env_file(path):
    if not path.exists():
        return
    for line in path.read_text().splitlines():
        line = line.strip()
        if not line or line.startswith('#') or '=' not in line:
            continue
        k, v = line.split('=', 1)
        k = k.strip()
        v = v.strip().strip('"').strip("'")
        if k and v and k not in os.environ:
            os.environ[k] = v

def find_project_root():
    # assume script is in scripts/ under project root
    return Path(__file__).resolve().parent.parent

def main():
    root = find_project_root()
    env_path = root / '.env'
    load_env_file(env_path)

    host = os.getenv('MQTT_HOST')
    port = int(os.getenv('MQTT_PORT', '1883'))
    username = os.getenv('MQTT_USER')
    password = os.getenv('MQTT_PASS')

    if not host or not username or not password:
        print('Missing MQTT config. Copy .env.example to .env and fill values.')
        sys.exit(2)

    try:
        import paho.mqtt.client as mqtt
    except Exception as e:
        print('paho-mqtt not available:', e)
        sys.exit(3)

    result = []

    def on_connect(client, userdata, flags, rc):
        print('on_connect rc=' + str(rc))
        result.append(rc)

    client = mqtt.Client()
    client.username_pw_set(username, password)
    client.on_connect = on_connect

    # quick DNS/socket test before MQTT connect
    try:
        sock = socket.create_connection((host, port), timeout=5)
        sock.close()
    except Exception as e:
        print('socket_connect_exception:', e)
        sys.exit(4)

    try:
        client.connect(host, port, 10)
    except Exception as e:
        print('connect_exception:', e)
        sys.exit(5)

    client.loop_start()
    for _ in range(12):
        if result:
            break
        time.sleep(1)
    client.loop_stop()

    if result and result[0] == 0:
        print('CONNECTED_OK')
        sys.exit(0)
    elif result:
        print('CONNECTED_BAD_RC', result[0])
        sys.exit(6)
    else:
        print('NO_RESPONSE')
        sys.exit(7)

if __name__ == '__main__':
    main()
