#!/usr/bin/env bash

source "$(dirname "$0")/common.sh"

echo "Managing Codes and Code Values (Task 10)..."

# 1. Create a Code
CODE_NAME="CustomerType_$(date +%s)"
RESPONSE_CODE=$(fin_curl -X POST "$FIN_URL/codes" -d "{
  \"name\": \"$CODE_NAME\"
}")
CODE_ID=$(echo "$RESPONSE_CODE" | python -c "import sys, json; print(json.load(sys.stdin)['resourceId'])")
echo "Created Code: $CODE_NAME with ID: $CODE_ID"

# 2. Add Code Values
RESPONSE_VAL1=$(fin_curl -X POST "$FIN_URL/codes/$CODE_ID/codevalues" -d '{
  "name": "VIP",
  "description": "Very Important Person",
  "position": 1
}')
VAL1_ID=$(echo "$RESPONSE_VAL1" | python -c "import sys, json; print(json.load(sys.stdin)['subResourceId'])")
echo "Added Code Value: VIP with ID: $VAL1_ID"

RESPONSE_VAL2=$(fin_curl -X POST "$FIN_URL/codes/$CODE_ID/codevalues" -d '{
  "name": "Standard",
  "description": "Regular customer",
  "position": 2
}')
VAL2_ID=$(echo "$RESPONSE_VAL2" | python -c "import sys, json; print(json.load(sys.stdin)['subResourceId'])")
echo "Added Code Value: Standard with ID: $VAL2_ID"

# 3. List Code Values
echo "Code Values for $CODE_NAME:"
fin_curl "$FIN_URL/codes/$CODE_ID/codevalues" | python -m json.tool
