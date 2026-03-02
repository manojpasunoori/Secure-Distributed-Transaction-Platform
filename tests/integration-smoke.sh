#!/usr/bin/env bash
set -euo pipefail

BASE_URL="${BASE_URL:-https://localhost:8443}"

health=$(curl -sk "$BASE_URL/api/health")
echo "$health" | rg -q '"status":"UP"'

response=$(curl -sk -X POST "$BASE_URL/api/transactions" \
  -H 'Content-Type: application/json' \
  -d '{"sender":"qa","receiver":"merchant","amount":25.00,"currency":"USD","reference":"smoke-1"}')

echo "$response" | rg -q '"status"'
echo "Smoke test passed"
