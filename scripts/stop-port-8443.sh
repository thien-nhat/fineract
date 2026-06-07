#!/usr/bin/env bash

set -euo pipefail

port="${1:-8443}"

find_pids() {
    if command -v lsof >/dev/null 2>&1; then
        lsof -t -iTCP:"$port" -sTCP:LISTEN 2>/dev/null | sort -u
        return
    fi

    if command -v ss >/dev/null 2>&1; then
        ss -ltnp "( sport = :$port )" 2>/dev/null \
            | awk -F 'pid=' 'NR > 1 && NF > 1 { split($2, parts, ","); print parts[1] }' \
            | sort -u
        return
    fi

    echo "Cannot inspect port $port: neither lsof nor ss is available." >&2
    exit 1
}

pids="$(find_pids)"

if [[ -z "$pids" ]]; then
    echo "No listening process found on port $port."
    exit 0
fi

echo "Stopping process(es) on port $port: $pids"
kill $pids
