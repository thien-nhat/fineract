#!/usr/bin/env bash

source "$(dirname "$0")/common.sh"

echo "Managing Datatables (Task 9)..."

# 1. Create Datatable for Clients
DATATABLE_NAME="extra_client_info_$(date +%s)"
RESPONSE_DT=$(fin_curl -X POST "$FIN_URL/datatables" -d "{
  \"datatableName\": \"$DATATABLE_NAME\",
  \"apptableName\": \"m_client\",
  \"multiRow\": false,
  \"columns\": [
    {
      \"name\": \"BusinessDescription\",
      \"type\": \"String\",
      \"mandatory\": true,
      \"length\": 100
    },
    {
      \"name\": \"YearsInBusiness\",
      \"type\": \"Number\",
      \"mandatory\": false
    }
  ]
}")
echo "Created Datatable: $DATATABLE_NAME"

# 2. Create a Client
RESPONSE_CLIENT=$(fin_curl -X POST "$FIN_URL/clients" -d '{
  "officeId": 1,
  "legalFormId": 1,
  "firstname": "Task9",
  "lastname": "DatatableClient",
  "active": true,
  "activationDate": "01 June 2026",
  "submittedOnDate": "01 June 2026",
  "dateFormat": "dd MMMM yyyy",
  "locale": "en"
}')
CLIENT_ID=$(echo "$RESPONSE_CLIENT" | python -c "import sys, json; print(json.load(sys.stdin)['clientId'])")
echo "Created Client with ID: $CLIENT_ID"

# 3. Add Datatable Entry
fin_curl -X POST "$FIN_URL/datatables/$DATATABLE_NAME/$CLIENT_ID" -d '{
  "BusinessDescription": "Small retail shop",
  "YearsInBusiness": 3
}' > /dev/null
echo "Added entry to datatable $DATATABLE_NAME for client $CLIENT_ID"

# 4. Read Datatable Entry
ENTRY=$(fin_curl "$FIN_URL/datatables/$DATATABLE_NAME/$CLIENT_ID")
echo "Datatable Entry: $ENTRY"

# 5. Update Datatable Entry
fin_curl -X PUT "$FIN_URL/datatables/$DATATABLE_NAME/$CLIENT_ID" -d '{
  "BusinessDescription": "Expanded retail shop",
  "YearsInBusiness": 4
}' > /dev/null
echo "Updated entry in datatable $DATATABLE_NAME"

# 6. Delete Datatable Entry
fin_curl -X DELETE "$FIN_URL/datatables/$DATATABLE_NAME/$CLIENT_ID" > /dev/null
echo "Deleted entry from datatable $DATATABLE_NAME"
