#!/usr/bin/env bash
set -uo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"
COMPOSE_FILE="$PROJECT_ROOT/docker-compose.yml"

PASS=0
FAIL=0

check() {
  local name="$1"
  shift
  if "$@" > /dev/null 2>&1; then
    echo "PASS  $name"
    PASS=$((PASS + 1))
  else
    echo "FAIL  $name"
    FAIL=$((FAIL + 1))
  fi
}

check_kafka_topics() {
  local topics
  topics="$(docker compose -f "$COMPOSE_FILE" exec -T kafka kafka-topics --bootstrap-server kafka:29092 --list)"
  for t in order-events vendor-events disruption-signals disruption-detected reroute-decisions notifications inventory-events shipment-events quote-events sla-events analytics-aggregates dlq-all contract-sync-events; do
    echo "$topics" | grep -qx "$t" || return 1
  done
}

check_redis_ping() {
  local pong
  pong="$(docker compose -f "$COMPOSE_FILE" exec -T redis redis-cli ping | tr -d '\r')"
  [ "$pong" = "PONG" ]
}

check_es_health() {
  curl -s http://localhost:9200/_cluster/health | grep -Eq '"status":"(green|yellow)"'
}

check_postgres_ready() {
  docker compose -f "$COMPOSE_FILE" exec -T postgres pg_isready -U smartgrid | grep -q "accepting connections"
}

check() "All containers running" bash -c "[ \"\$(docker compose -f '$COMPOSE_FILE' ps --status running -q | wc -l)\" -gt 0 ]"
check "Kafka: all 13 topics exist" check_kafka_topics
check "Schema Registry reachable" curl -sf http://localhost:18081/subjects
check "Elasticsearch cluster health is green or yellow" check_es_health
check "Redis PING returns PONG" check_redis_ping
check "Zipkin UI reachable on :9411" curl -sf http://localhost:9411/zipkin/
check "Postgres accepting connections" check_postgres_ready
check "Prometheus healthy" curl -sf http://localhost:9090/-/healthy
check "Grafana healthy" curl -sf http://localhost:3001/api/health

echo ""
echo "$PASS passed, $FAIL failed"
[ "$FAIL" -eq 0 ]
