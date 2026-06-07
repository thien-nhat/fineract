#!/usr/bin/env bash

source "$(dirname "$0")/common.sh"

echo "Managing Savings Account Lifecycle..."

# 1. Create Active Client
RESPONSE_CLIENT=$(fin_curl -X POST "$FIN_URL/clients" -d '{
  "officeId": 1,
  "legalFormId": 1,
  "firstname": "Task5",
  "lastname": "SavingsClient",
  "active": true,
  "activationDate": "01 June 2026",
  "submittedOnDate": "01 June 2026",
  "dateFormat": "dd MMMM yyyy",
  "locale": "en"
}')
CLIENT_ID=$(echo "$RESPONSE_CLIENT" | python -c "import sys, json; print(json.load(sys.stdin)['clientId'])")
echo "Created Client with ID: $CLIENT_ID"

# 2. Create Savings Product
RESPONSE_PRODUCT=$(fin_curl -X POST "$FIN_URL/savingsproducts" -d '{
  "name": "Savings Product Task 5",
  "shortName": "SPT5",
  "currencyCode": "USD",
  "digitsAfterDecimal": 2,
  "inMultiplesOf": 0,
  "nominalAnnualInterestRate": 5.0,
  "interestCompoundingPeriodType": 1,
  "interestPostingPeriodType": 4,
  "interestCalculationType": 1,
  "interestCalculationDaysInYearType": 365,
  "accountingRule": 1,
  "locale": "en"
}')
PRODUCT_ID=$(echo "$RESPONSE_PRODUCT" | python -c "import sys, json; print(json.load(sys.stdin)['resourceId'])")
echo "Created Savings Product with ID: $PRODUCT_ID"

# 3. Submit Savings Account Application
RESPONSE_ACCOUNT=$(fin_curl -X POST "$FIN_URL/savingsaccounts" -d "{
  \"clientId\": $CLIENT_ID,
  \"productId\": $PRODUCT_ID,
  \"submittedOnDate\": \"02 June 2026\",
  \"dateFormat\": \"dd MMMM yyyy\",
  \"locale\": \"en\"
}")
ACCOUNT_ID=$(echo "$RESPONSE_ACCOUNT" | python -c "import sys, json; print(json.load(sys.stdin)['savingsId'])")
echo "Submitted Savings Account Application with ID: $ACCOUNT_ID"

# 4. Approve Savings Account
fin_curl -X POST "$FIN_URL/savingsaccounts/$ACCOUNT_ID?command=approve" -d '{
  "approvedOnDate": "03 June 2026",
  "dateFormat": "dd MMMM yyyy",
  "locale": "en"
}' > /dev/null
echo "Approved Savings Account with ID: $ACCOUNT_ID"

# 5. Activate Savings Account
fin_curl -X POST "$FIN_URL/savingsaccounts/$ACCOUNT_ID?command=activate" -d '{
  "activatedOnDate": "03 June 2026",
  "dateFormat": "dd MMMM yyyy",
  "locale": "en"
}' > /dev/null
echo "Activated Savings Account with ID: $ACCOUNT_ID"

# 6. Verify Status
STATUS=$(fin_curl "$FIN_URL/savingsaccounts/$ACCOUNT_ID" | python -c "import sys, json; print(json.load(sys.stdin)['status']['value'])")
echo "Savings Account Status: $STATUS"
