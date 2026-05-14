#!/bin/bash

# Script para ejecutar tests de Postman de AEG Core
# Uso: ./test-api.sh [local|cloud]

COLLECTION="aeg-core.postman_collection.json"
ENV_TYPE=${1:-local}

if [ "$ENV_TYPE" == "cloud" ]; then
    ENVIRONMENT="aeg-core.cloud.postman_environment.json"
    echo "🌐 Usando entorno CLOUD (DigitalOcean)"
else
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
