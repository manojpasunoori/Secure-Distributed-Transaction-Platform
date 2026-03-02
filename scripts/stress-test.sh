#!/usr/bin/env bash
set -euo pipefail

TOTAL_REQUESTS="${1:-100}"
CONCURRENCY="${2:-10}"
URL="${URL:-https://localhost:8443/api/transactions}"

run_one() {
  local i="$1"
  curl -sk -X POST "$URL" \
    -H 'Content-Type: application/json' \
    -d "{\"sender\":\"user-$i\",\"receiver\":\"merchant\",\"amount\":$(( (i % 500) + 1 )),\"currency\":\"USD\",\"reference\":\"load-$i\"}" >/dev/null
}

export -f run_one
export URL

seq 1 "$TOTAL_REQUESTS" | xargs -I{} -P "$CONCURRENCY" bash -c 'run_one "$@"' _ {}
echo "Completed $TOTAL_REQUESTS requests at concurrency $CONCURRENCY"
