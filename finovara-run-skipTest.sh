#!/bin/bash

set -e

ROOT_DIR="$(cd "$(dirname "$0")" && pwd)"

echo "======================================"
echo "  Finovara — Build & Run"
echo "======================================"

# 1. Maven build
echo ""
echo "[1/3] Building all modules..."

cd "$ROOT_DIR"

~/.m2/wrapper/dists/apache-maven-3.9.11-bin/6mqf5t809d9geo83kj4ttckcbc/apache-maven-3.9.11/bin/mvn clean package -DskipTests

# 2. Docker network
echo ""
echo "[2/3] Creating Docker network (if not exists)..."

docker network create finovara-network 2>/dev/null || echo "Network already exists, skipping."

# 3. Start services
echo ""
echo "[3/3] Starting services..."

cd "$ROOT_DIR/api-gateway"
docker compose -f docker.yaml up --build -d

cd "$ROOT_DIR/activity-log-backend"
docker compose -f docker.yaml up --build -d

cd "$ROOT_DIR/core-backend"
docker compose -f docker.yaml up --build -d

echo ""
echo "======================================"
echo "  All services started!"
echo "======================================"
echo "  contracts-backend loaded as shared module"
echo "  gateway: https://localhost:443"
echo "  core-Backend: https://localhost:8443"
echo "  activity-log-backend: https://localhost:8082"
echo "======================================"