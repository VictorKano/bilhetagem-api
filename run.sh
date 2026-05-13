#!/bin/bash
# ============================================================
# run.sh — Build and run api-cobranca-bilhetagem (Linux/macOS)
# ============================================================

set -e

# --------------- Configurable environment variables --------
export DB_HOST="${DB_HOST:-localhost}"
export DB_PORT="${DB_PORT:-5432}"
export DB_NAME="${DB_NAME:-cobranca}"
export DB_USER="${DB_USER:-postgres}"
export DB_PASSWORD="${DB_PASSWORD:-postgres}"

export REDIS_HOST="${REDIS_HOST:-localhost}"
export REDIS_PORT="${REDIS_PORT:-6379}"

export KAFKA_BOOTSTRAP_SERVERS="${KAFKA_BOOTSTRAP_SERVERS:-localhost:9092}"

export SERVER_PORT="${SERVER_PORT:-8080}"
# -----------------------------------------------------------

JAR="target/api-cobranca-bilhetagem-1.0.0-SNAPSHOT.jar"

echo "=================================================="
echo "  api-cobranca-bilhetagem"
echo "=================================================="
echo "  DB   : jdbc:postgresql://$DB_HOST:$DB_PORT/$DB_NAME"
echo "  Redis: $REDIS_HOST:$REDIS_PORT"
echo "  Kafka: $KAFKA_BOOTSTRAP_SERVERS"
echo "  Port : $SERVER_PORT"
echo "=================================================="

# Build if JAR is missing or --build flag is passed
if [ ! -f "$JAR" ] || [ "$1" = "--build" ]; then
  echo ""
  echo "[1/2] Building with Maven (skipping tests)..."
  ./mvnw clean package -DskipTests
  echo "[1/2] Build complete."
fi

echo ""
echo "[2/2] Starting application..."
java -jar "$JAR" --server.port="$SERVER_PORT"
