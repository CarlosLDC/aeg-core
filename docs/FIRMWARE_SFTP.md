# Firmware storage (App Platform → droplet)

## Por qué falla el puerto 22

DigitalOcean **App Platform bloquea conexiones salientes a TCP 22**.
Por eso SFTP a `10.116.0.4:22` o `206.189.231.128:22` hace timeout.

## Setup recomendado

### 1. Droplet — sshd en puerto 2222

```bash
# /etc/ssh/sshd_config (añadir o cambiar)
Port 2222
# puedes dejar también Port 22 para admin humano, o solo 2222

sudo systemctl restart sshd
# o: sudo service ssh restart
```

Firewall (Cloud Firewall o ufw): permitir **TCP 2222** desde la VPC de App Platform
(o temporalmente desde cualquier origen para probar).

Comprobar:

```bash
ss -tlnp | grep 2222
```

### 2. App Platform — variables

| Variable | Valor |
|----------|--------|
| `FIRMWARE_SFTP_HOST` | `10.116.0.4` (misma VPC) o `206.189.231.128` si aún no hay VPC en la app |
| `FIRMWARE_SFTP_PORT` | `2222` |
| `FIRMWARE_SFTP_USER` | p. ej. `root` |
| `FIRMWARE_SFTP_PASSWORD` | password SSH |
| `FIRMWARE_SFTP_REMOTE_DIR` | `/var/www/firmware` |
| `FIRMWARE_PUBLIC_BASE_URL` | `http://206.189.231.128/downloads` (IoT; siempre público) |

La app debe estar **conectada al mismo VPC** que el droplet si usas la IP privada
(Networking → Connect app to VPC network).

### 3. Permisos en disco

El usuario SFTP debe poder escribir en `/var/www/firmware/` (y nginx servir
ese directorio o un alias en `/downloads/`).
