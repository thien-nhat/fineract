#!/usr/bin/env bash

# Common variables for Fineract API calls
export FIN_URL="https://localhost:8443/fineract-provider/api/v1"
export FIN_AUTH="Authorization: Basic bWlmb3M6cGFzc3dvcmQ="
export FIN_TENANT="Fineract-Platform-TenantId: default"
export FIN_JSON="Content-Type: application/json"

# Function to parse JSON using Python
parse_json() {
    python3 -c "import sys, json; print(json.load(sys.stdin)$1)"
}

# Wrapper for curl to handle common headers and --insecure
fin_curl() {
    curl --insecure -s -H "$FIN_AUTH" -H "$FIN_TENANT" -H "$FIN_JSON" "$@"
}
