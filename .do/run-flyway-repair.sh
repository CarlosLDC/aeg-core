#!/usr/bin/env bash
set -euo pipefail

# Align flyway_schema_history checksums with current migration files (V10/V11 drift).
# Run once when validate-on-migrate fails after editing migrations post-deploy.
#
# Usage:
#   export DATABASE_URL="postgresql://user:pass@host:port/db?sslmode=require"
#   ./.do/run-flyway-repair.sh

if [ -z "${DATABASE_URL:-}" ]; then
  echo "ERROR: DATABASE_URL is not set. Export DATABASE_URL and retry."
  exit 1
fi

PROTO_STRIPPED=${DATABASE_URL#*://}

if [[ "$PROTO_STRIPPED" == *"@"* ]]; then
  USERPASS=${PROTO_STRIPPED%%@*}
  HOSTPATH=${PROTO_STRIPPED#*@}
else
  echo "ERROR: DATABASE_URL missing userinfo (user:pass@)."
  exit 1
fi

HOSTPORT=${HOSTPATH%%/*}
DB_AND_QUERY=${HOSTPATH#*/}
DB=${DB_AND_QUERY%%\?*}

if [[ "$DB_AND_QUERY" == *"?"* ]]; then
  QUERY=${DB_AND_QUERY#*\?}
else
  QUERY="sslmode=require"
fi

JDBC_URL="jdbc:postgresql://${HOSTPORT}/${DB}?${QUERY}"
USER=${USERPASS%%:*}
PASSWORD=${USERPASS#*:}

echo "Running Flyway repair against: ${JDBC_URL} (user=${USER})"

docker run --rm \
  --entrypoint="/flyway/flyway" \
  flyway/flyway:latest \
  -url="${JDBC_URL}" -user="${USER}" -password="${PASSWORD}" repair

echo "Repair complete. Redeploy the API (with APP_FLYWAY_REPAIR_ON_STARTUP unset)."
