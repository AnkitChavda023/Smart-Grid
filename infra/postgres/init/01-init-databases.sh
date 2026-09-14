#!/bin/bash
set -euo pipefail

DATABASES=(
  auth_db
  order_db
  vendor_db
  inventory_db
  pricing_db
  shipment_db
  contract_db
  notification_db
  analytics_db
  rag_db
  disruption_detector_db
  reroute_planner_db
  vendor_evaluator_db
  sla_breach_analyst_db
  demand_forecaster_db
  contract_negotiation_db
)

for db in "${DATABASES[@]}"; do
  psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" <<-EOSQL
    SELECT 'CREATE DATABASE $db' WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = '$db')\gexec
EOSQL
done

psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" -d shipment_db <<-EOSQL
  CREATE EXTENSION IF NOT EXISTS postgis;
EOSQL

psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" -d rag_db <<-EOSQL
  CREATE EXTENSION IF NOT EXISTS vector;
EOSQL
