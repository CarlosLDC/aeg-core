# Firmware storage (App Platform → droplet)

## Diagnóstico (2026-07-31)

Comprobaciones desde fuera del droplet:

| Check | Resultado |
|-------|-----------|
| `206.189.231.128:22` (SSH) | Abierto (`OpenSSH_9.6`) |
| `206.189.231.128:2222` (SFTP App Platform) | **Connection refused** — sshd no escucha en 2222 |
| `206.189.231.128:80` `/downloads/test.bin` | **200** — Nginx sirve binarios |
| `FIRMWARE_SFTP_HOST=10.116.0.4` | Inconsistente: `MQTT_URL` ya usa la IP **pública**; sin VPC la privada no es alcanzable desde App Platform |
| CORS `Origin: https://www.aeg-admin.tech` | OK |
| CORS `Origin: https://aeg-admin.tech` / Vercel / `aeg-tech.com` | **403 Invalid CORS request** en producción |

Conclusión: las subidas fallan en el tramo **App Platform → SFTP**, no en Nginx. Hay que (1) exponer sshd en **2222**, (2) apuntar SFTP al host alcanzable (IP pública si no hay VPC), (3) corregir CORS para el origen real del panel.

## Por qué falla el puerto 22

DigitalOcean **App Platform bloquea conexiones salientes a TCP 22**.
Por eso SFTP a `:22` hace timeout aunque el droplet responda bien a SSH humano.

## Setup recomendado

### 1. Droplet — script

En el droplet (como root), ejecutar el script versionado:

```bash
sudo bash scripts/setup-firmware-sftp-droplet.sh
```

Está en [`scripts/setup-firmware-sftp-droplet.sh`](../scripts/setup-firmware-sftp-droplet.sh).

Hace:

- Añade `Port 2222` a sshd (deja 22 para admin humano)
- Abre ufw TCP 2222
- Crea `/var/www/firmware` y un site Nginx en `/downloads/`

Comprobar:

```bash
ss -tlnp | grep 2222
curl -I http://127.0.0.1/downloads/
```

### 2. App Platform — variables

| Variable | Valor |
|----------|--------|
| `FIRMWARE_SFTP_HOST` | `206.189.231.128` (público; mismo criterio que MQTT) **o** `10.116.0.4` solo si la app está en la misma VPC |
| `FIRMWARE_SFTP_PORT` | `2222` |
| `FIRMWARE_SFTP_USER` | p. ej. `root` (secret real, no el placeholder) |
| `FIRMWARE_SFTP_PASSWORD` | password SSH (secret real) |
| `FIRMWARE_SFTP_REMOTE_DIR` | `/var/www/firmware` |
| `FIRMWARE_PUBLIC_BASE_URL` | `http://206.189.231.128/downloads` |
| `APP_CORS_ALLOWED_ORIGINS` | Incluir apex + www del admin y previews Vercel |

Firewall (Cloud Firewall o ufw): permitir **TCP 2222** al menos desde Internet o desde los rangos de salida de App Platform.

### 3. Permisos en disco

El usuario SFTP debe poder escribir en `/var/www/firmware/`. Nginx debe servir ese mismo directorio en `/downloads/`.

### 4. Flujo de subida (recordatorio)

1. Browser → `POST https://core-xgfvw.ondigitalocean.app/api/firmwares` (directo, sin proxy Vercel)
2. API responde `202` + `jobId` y hace SFTP en background
3. Browser hace poll `GET /api/firmwares/uploads/{jobId}`
4. IoT descarga por HTTP público `/downloads/<file>.bin`
