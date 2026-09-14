#!/usr/bin/env bash
set -euo pipefail

CONFIG_FILE="$1"
CONNECT_URL="${2:-http://localhost:18083}"

NAME=$(grep -o '"name"[[:space:]]*:[[:space:]]*"[^"]*"' "$CONFIG_FILE" | head -1 | sed -E 's/.*"([^"]+)"$/\1/')

if curl -sf "$CONNECT_URL/connectors/$NAME" > /dev/null 2>&1; then
  echo "Connector '$NAME' already exists, updating config"
  CONFIG_ONLY=$(python3 -c "import json,sys; print(json.dumps(json.load(open('$CONFIG_FILE'))['config']))")
  curl -s -X PUT "$CONNECT_URL/connectors/$NAME/config" -H "Content-Type: application/json" -d "$CONFIG_ONLY" > /dev/null
else
  echo "Registering connector '$NAME'"
  curl -s -X POST "$CONNECT_URL/connectors" -H "Content-Type: application/json" --data-binary "@$CONFIG_FILE" > /dev/null
fi

sleep 2
curl -s "$CONNECT_URL/connectors/$NAME/status"
