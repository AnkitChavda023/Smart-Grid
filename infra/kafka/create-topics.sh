#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"
COMPOSE_FILE="$PROJECT_ROOT/docker-compose.yml"

declare -a TOPICS=(
  "order-events:12:604800000"
  "vendor-events:6:604800000"
  "disruption-signals:3:259200000"
  "disruption-detected:3:604800000"
  "reroute-decisions:3:2592000000"
  "notifications:6:86400000"
  "inventory-events:6:604800000"
  "shipment-events:6:1209600000"
  "quote-events:3:259200000"
  "sla-events:3:2592000000"
  "analytics-aggregates:3:604800000"
  "dlq-all:3:1209600000"
  "contract-sync-events:3:604800000"
)

for entry in "${TOPICS[@]}"; do
  IFS=':' read -r name partitions retention_ms <<< "$entry"
  echo "Creating topic '$name' (partitions=$partitions, retention.ms=$retention_ms)"
  docker compose -f "$COMPOSE_FILE" exec -T kafka \
    kafka-topics --bootstrap-server kafka:29092 \
    --create --if-not-exists \
    --topic "$name" \
    --partitions "$partitions" \
    --replication-factor 1 \
    --config "retention.ms=$retention_ms"
done

echo ""
echo "Current topic list:"
docker compose -f "$COMPOSE_FILE" exec -T kafka kafka-topics --bootstrap-server kafka:29092 --list
