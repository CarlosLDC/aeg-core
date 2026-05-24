#!/bin/bash

# Script para ejecutar tests de Postman de AEG Core
# Uso:
#   ./test-api.sh local
#   ./test-api.sh cloud-safe --allow-prod
#   ./test-api.sh cloud --allow-prod --destructive

COLLECTION="aeg-core.safe.postman_collection.json"
ENV_TYPE=${1:-cloud-safe}

ALLOW_PROD_FLAG=${2:-}
DESTRUCTIVE_FLAG=${3:-}

if [ "$ENV_TYPE" == "cloud" ] || [ "$ENV_TYPE" == "cloud-safe" ]; then
    if [ "$ALLOW_PROD_FLAG" != "--allow-prod" ]; then
        echo "🛑 Ejecución contra CLOUD bloqueada por seguridad."
        echo "   Usa: ./test-api.sh cloud-safe --allow-prod"
        echo "      o: ./test-api.sh cloud --allow-prod --destructive"
        exit 1
    fi
    ENVIRONMENT="aeg-core.cloud.postman_environment.json"
    if [ "$ENV_TYPE" == "cloud" ]; then
        if [ "$DESTRUCTIVE_FLAG" != "--destructive" ]; then
            echo "🛑 Suite destructiva en CLOUD bloqueada."
            echo "   Usa: ./test-api.sh cloud --allow-prod --destructive"
            exit 1
        fi
        COLLECTION="aeg-core.postman_collection.json"
        echo "🌐 Usando entorno CLOUD (DigitalOcean)"
        echo "⚠️  Modo DESTRUCTIVO habilitado"
    else
        COLLECTION="aeg-core.safe.postman_collection.json"
        echo "🌐 Usando entorno CLOUD (DigitalOcean) en modo SAFE (read-only)"
    fi
else
    COLLECTION="aeg-core.postman_collection.json"
    ENVIRONMENT="aeg-core.local.postman_environment.json"
    echo "🏠 Usando entorno LOCAL"
fi

echo "🚀 Iniciando suite de tests de AEG Core..."

# Usamos npx para no requerir instalación global de newman
npx -y newman run "$COLLECTION" \
    -e "$ENVIRONMENT" \
    --reporters cli \
    --color on

if [ $? -eq 0 ]; then
    echo "✅ Todos los tests pasaron con éxito."
else
    echo "❌ Algunos tests fallaron. Revisa el reporte arriba."
    exit 1
fi
