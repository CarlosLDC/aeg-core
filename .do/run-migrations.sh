#!/usr/bin/env bash
set -euo pipefail

# Run Flyway migrations using the official Flyway Docker image.
# Requires: Docker or an environment that can reach the managed DB (VPC).
# Usage:
#   export DATABASE_URL="postgresql://user:pass@host:port/db?sslmode=require"
#   ./.do/run-migrations.sh

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

echo "Running Flyway migrate against: ${JDBC_URL} (user=${USER})"

docker run --rm \
  --entrypoint="/flyway/flyway" \
  flyway/flyway:latest \
  -url="${JDBC_URL}" -user="${USER}" -password="${PASSWORD}" migrate
