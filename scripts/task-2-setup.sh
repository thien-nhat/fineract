#!/usr/bin/env bash

source "$(dirname "$0")/common.sh"

echo "Creating Client and managing lifecycle..."

# 1. Create Pending Client
RESPONSE_PENDING=$(fin_curl -X POST "$FIN_URL/clients" -d '{
  "officeId": 1,
  "legalFormId": 1,
  "firstname": "Task2",
  "lastname": "PendingClient",
  "active": false,
  "submittedOnDate": "01 June 2026",
  "dateFormat": "dd MMMM yyyy",
  "locale": "en"
}')
CLIENT_ID=$(echo "$RESPONSE_PENDING" | python -c "import sys, json; print(json.load(sys.stdin)['clientId'])")
echo "Created Pending Client with ID: $CLIENT_ID"

# 2. Verify status is Pending
STATUS=$(fin_curl "$FIN_URL/clients/$CLIENT_ID" | python -c "import sys, json; print(json.load(sys.stdin)['status']['value'])")
echo "Initial Client Status: $STATUS"

# 3. Activate Client
fin_curl -X POST "$FIN_URL/clients/$CLIENT_ID?command=activate" -d '{
  "activationDate": "02 June 2026",
  "dateFormat": "dd MMMM yyyy",
  "locale": "en"
}' > /dev/null
echo "Activated Client with ID: $CLIENT_ID"

# 4. Verify status is Active
STATUS_ACTIVE=$(fin_curl "$FIN_URL/clients/$CLIENT_ID" | python -c "import sys, json; print(json.load(sys.stdin)['status']['value'])")
echo "Updated Client Status: $STATUS_ACTIVE"
