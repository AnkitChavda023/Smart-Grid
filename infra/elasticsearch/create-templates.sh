#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ES_URL="${ES_URL:-http://localhost:9200}"

echo "Creating component template 'smartgrid-defaults'"
curl -s -X PUT "$ES_URL/_component_template/smartgrid-defaults" \
  -H "Content-Type: application/json" \
  --data-binary "@$SCRIPT_DIR/templates/smartgrid-defaults.json" > /dev/null

echo "Creating ILM policy 'analytics-ilm-policy'"
curl -s -X PUT "$ES_URL/_ilm/policy/analytics-ilm-policy" \
  -H "Content-Type: application/json" \
  --data-binary "@$SCRIPT_DIR/policies/analytics-ilm-policy.json" > /dev/null

echo "Lowering ILM poll interval to 5s (local dev only, default is 10m — too slow to verify rollover in a test run)"
curl -s -X PUT "$ES_URL/_cluster/settings" \
  -H "Content-Type: application/json" \
  --data-binary '{"transient":{"indices.lifecycle.poll_interval":"5s"}}' > /dev/null

for name in vendor-catalog contracts analytics-orders analytics-vendors rag-chunks; do
  echo "Creating index template '$name'"
  curl -s -X PUT "$ES_URL/_index_template/$name" \
    -H "Content-Type: application/json" \
    --data-binary "@$SCRIPT_DIR/templates/$name-template.json" > /dev/null
done

for alias in analytics_orders analytics_vendors; do
  if curl -s -o /dev/null -w "%{http_code}" "$ES_URL/_alias/$alias" | grep -q "^200$"; then
    echo "Rollover-alias '$alias' already exists, skipping bootstrap"
  else
    echo "Bootstrapping rollover-alias index '$alias-000001' (alias: $alias)"
    curl -s -X PUT "$ES_URL/$alias-000001" \
      -H "Content-Type: application/json" \
      --data-binary "{ \"aliases\": { \"$alias\": { \"is_write_index\": true } } }" > /dev/null
  fi
done

echo ""
echo "Registered index templates:"
curl -s "$ES_URL/_index_template" | grep -o '"name":"[^"]*"'
