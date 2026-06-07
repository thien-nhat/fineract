#!/usr/bin/env bash

source "$(dirname "$0")/common.sh"

echo "Creating Office Hierarchy..."

# 1. Create Head Office Branch A
RESPONSE_A=$(fin_curl -X POST "$FIN_URL/offices" -d '{
  "name": "Branch A",
  "openingDate": "01 June 2026",
  "dateFormat": "dd MMMM yyyy",
  "locale": "en",
  "parentId": 1
}')
OFFICE_A_ID=$(echo "$RESPONSE_A" | python -c "import sys, json; print(json.load(sys.stdin)['resourceId'])")
echo "Created Branch A with ID: $OFFICE_A_ID"

# 2. Create Sub-branch A.1
RESPONSE_A1=$(fin_curl -X POST "$FIN_URL/offices" -d "{
  \"name\": \"Sub-branch A.1\",
  \"openingDate\": \"01 June 2026\",
  \"dateFormat\": \"dd MMMM yyyy\",
  \"locale\": \"en\",
  \"parentId\": $OFFICE_A_ID
}")
OFFICE_A1_ID=$(echo "$RESPONSE_A1" | python -c "import sys, json; print(json.load(sys.stdin)['resourceId'])")
echo "Created Sub-branch A.1 with ID: $OFFICE_A1_ID"

# 3. Create Sub-branch A.2
RESPONSE_A2=$(fin_curl -X POST "$FIN_URL/offices" -d "{
  \"name\": \"Sub-branch A.2\",
  \"openingDate\": \"01 June 2026\",
  \"dateFormat\": \"dd MMMM yyyy\",
  \"locale\": \"en\",
  \"parentId\": $OFFICE_A_ID
}")
OFFICE_A2_ID=$(echo "$RESPONSE_A2" | python -c "import sys, json; print(json.load(sys.stdin)['resourceId'])")
echo "Created Sub-branch A.2 with ID: $OFFICE_A2_ID"

echo "Listing offices ordered by name:"
fin_curl -G "$FIN_URL/offices" --data-urlencode "orderBy=name" --data-urlencode "sortOrder=ASC" | python -m json.tool
