#!/usr/bin/env bash
# Configura el droplet para SFTP de firmwares desde DigitalOcean App Platform.
#
# App Platform bloquea outbound TCP/22 → sshd debe escuchar también en 2222.
# Nginx ya puede estar sirviendo /downloads/; este script no sustituye el
# default site completo. Solo asegura directorio + snippet de location.
#
# Uso (como root en el droplet):
#   sudo bash setup-firmware-sftp-droplet.sh
set -euo pipefail

if [[ "${EUID}" -ne 0 ]]; then
  echo "Ejecuta como root (sudo)." >&2
  exit 1
fi

SSHD_CONFIG="/etc/ssh/sshd_config"
FIRMWARE_DIR="/var/www/firmware"
SNIPPET="/etc/nginx/snippets/aeg-firmware-downloads.conf"

echo "==> Asegurando Port 2222 en ${SSHD_CONFIG}"
if grep -Eq '^[[:space:]]*Port[[:space:]]+2222[[:space:]]*$' "${SSHD_CONFIG}"; then
  echo "    ya presente"
else
  printf '\n# AEG firmware SFTP (App Platform no puede usar TCP 22)\nPort 2222\n' >> "${SSHD_CONFIG}"
fi

echo "==> Validando sshd_config y reiniciando ssh"
sshd -t
if systemctl restart ssh 2>/dev/null || systemctl restart sshd 2>/dev/null; then
  echo "    ssh reiniciado"
else
  service ssh restart 2>/dev/null || service sshd restart
fi

echo "==> Firewall ufw (si está activo): allow 2222/tcp"
if command -v ufw >/dev/null 2>&1; then
  ufw allow 2222/tcp comment 'AEG firmware SFTP' || true
  ufw status numbered 2>/dev/null | head -30 || true
fi

echo "==> Directorio ${FIRMWARE_DIR}"
mkdir -p "${FIRMWARE_DIR}"
# www-data lectura; el usuario SFTP (p.ej. root) escribe
if getent group www-data >/dev/null 2>&1; then
  chown root:www-data "${FIRMWARE_DIR}"
  chmod 775 "${FIRMWARE_DIR}"
else
  chmod 755 "${FIRMWARE_DIR}"
fi

if command -v nginx >/dev/null 2>&1; then
  echo "==> Snippet Nginx ${SNIPPET}"
  mkdir -p /etc/nginx/snippets
  cat > "${SNIPPET}" <<EOF
# Incluir dentro del server { ... } que escucha en :80:
#   include /etc/nginx/snippets/aeg-firmware-downloads.conf;
location /downloads/ {
    alias ${FIRMWARE_DIR}/;
    autoindex off;
    default_type application/octet-stream;
}
EOF
  if grep -Rqs 'aeg-firmware-downloads.conf' /etc/nginx/sites-enabled /etc/nginx/conf.d 2>/dev/null \
    || grep -Rqs 'alias /var/www/firmware' /etc/nginx/sites-enabled /etc/nginx/conf.d 2>/dev/null \
    || grep -Rqs '/downloads/' /etc/nginx/sites-enabled /etc/nginx/conf.d 2>/dev/null; then
    echo "    /downloads/ ya referenciado en la config de Nginx"
  else
    echo "    AVISO: añade 'include ${SNIPPET};' al server block de :80 y luego:"
    echo "           nginx -t && systemctl reload nginx"
  fi
else
  echo "==> nginx no instalado; omite snippet /downloads/"
fi

echo "==> Verificación local"
ss -tlnp | grep -E ':22|:2222' || true
echo
echo "Desde fuera del droplet comprueba:"
echo "  nc -vz <IP_PUBLICA> 2222"
echo "App Platform:"
echo "  FIRMWARE_SFTP_HOST=<IP_PUBLICA_O_PRIVADA_VPC>"
echo "  FIRMWARE_SFTP_PORT=2222"
echo "  FIRMWARE_SFTP_REMOTE_DIR=${FIRMWARE_DIR}"
echo "  FIRMWARE_PUBLIC_BASE_URL=http://<IP_PUBLICA>/downloads"
