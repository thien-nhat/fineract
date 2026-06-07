#!/usr/bin/env bash

source "$(dirname "$0")/common.sh"

echo "Managing Client Notes..."

# 1. Create a Client first (Active)
RESPONSE_CLIENT=$(fin_curl -X POST "$FIN_URL/clients" -d '{
  "officeId": 1,
  "legalFormId": 1,
  "firstname": "Note",
  "lastname": "TestClient",
  "active": true,
  "activationDate": "01 June 2026",
  "submittedOnDate": "01 June 2026",
  "dateFormat": "dd MMMM yyyy",
  "locale": "en"
}')
CLIENT_ID=$(echo "$RESPONSE_CLIENT" | python -c "import sys, json; print(json.load(sys.stdin)['clientId'])")
echo "Created Client with ID: $CLIENT_ID"

# 2. Add a Note
RESPONSE_NOTE=$(fin_curl -X POST "$FIN_URL/clients/$CLIENT_ID/notes" -d '{
  "note": "This is an initial note for Task 3."
}')
NOTE_ID=$(echo "$RESPONSE_NOTE" | python -c "import sys, json; print(json.load(sys.stdin)['resourceId'])")
echo "Added Note with ID: $NOTE_ID"

# 3. Update the Note
fin_curl -X PUT "$FIN_URL/clients/$CLIENT_ID/notes/$NOTE_ID" -d '{
  "note": "Updated note content for Task 3."
}' > /dev/null
echo "Updated Note with ID: $NOTE_ID"

# 4. Verify the Note
NOTE_CONTENT=$(fin_curl "$FIN_URL/clients/$CLIENT_ID/notes/$NOTE_ID" | python -c "import sys, json; print(json.load(sys.stdin)['note'])")
echo "Note Content: $NOTE_CONTENT"

# 5. Delete the Note
fin_curl -X DELETE "$FIN_URL/clients/$CLIENT_ID/notes/$NOTE_ID" > /dev/null
echo "Deleted Note with ID: $NOTE_ID"
