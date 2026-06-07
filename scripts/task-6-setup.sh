#!/usr/bin/env bash

source "$(dirname "$0")/common.sh"

echo "Setting up data for Savings Transaction Search (Task 6)..."

# 1. Enable Business Date
fin_curl -X PUT "$FIN_URL/configurations/name/enable-business-date" -d '{"enabled": true}' > /dev/null
echo "Enabled Business Date"

# 2. Set Business Date to 03 June 2026
fin_curl -X POST "$FIN_URL/businessdate" -d '{
  "type": "BUSINESS_DATE",
  "date": "03 June 2026",
  "dateFormat": "dd MMMM yyyy",
  "locale": "en"
}' > /dev/null
echo "Set Business Date to 03 June 2026"

# 3. Create Active Client
RESPONSE_CLIENT=$(fin_curl -X POST "$FIN_URL/clients" -d '{
  "officeId": 1,
  "legalFormId": 1,
  "firstname": "Task6",
  "lastname": "SearchClient",
  "active": true,
  "activationDate": "03 June 2026",
  "dateFormat": "dd MMMM yyyy",
  "locale": "en"
}')
CLIENT_ID=$(echo "$RESPONSE_CLIENT" | python -c "import sys, json; print(json.load(sys.stdin)['clientId'])")
echo "Created Client with ID: $CLIENT_ID"

# 4. Create Savings Product
RESPONSE_PRODUCT=$(fin_curl -X POST "$FIN_URL/savingsproducts" -d '{
  "name": "Search Savings Product",
  "shortName": "SSP6",
  "currencyCode": "EUR",
  "digitsAfterDecimal": 2,
  "inMultiplesOf": 0,
  "nominalAnnualInterestRate": 0.0,
  "interestCompoundingPeriodType": 1,
  "interestPostingPeriodType": 4,
  "interestCalculationType": 1,
  "interestCalculationDaysInYearType": 365,
  "accountingRule": 1,
  "locale": "en"
}')
PRODUCT_ID=$(echo "$RESPONSE_PRODUCT" | python -c "import sys, json; print(json.load(sys.stdin)['resourceId'])")
echo "Created Savings Product with ID: $PRODUCT_ID"

# 5. Submit, Approve, Activate Savings Account
RESPONSE_ACCOUNT=$(fin_curl -X POST "$FIN_URL/savingsaccounts" -d "{
  \"clientId\": $CLIENT_ID,
  \"productId\": $PRODUCT_ID,
  \"submittedOnDate\": \"03 June 2026\",
  \"dateFormat\": \"dd MMMM yyyy\",
  \"locale\": \"en\"
}")
ACCOUNT_ID=$(echo "$RESPONSE_ACCOUNT" | python -c "import sys, json; print(json.load(sys.stdin)['savingsId'])")

fin_curl -X POST "$FIN_URL/savingsaccounts/$ACCOUNT_ID?command=approve" -d '{
  "approvedOnDate": "03 June 2026",
  "dateFormat": "dd MMMM yyyy",
  "locale": "en"
}' > /dev/null

fin_curl -X POST "$FIN_URL/savingsaccounts/$ACCOUNT_ID?command=activate" -d '{
  "activatedOnDate": "03 June 2026",
  "dateFormat": "dd MMMM yyyy",
  "locale": "en"
}' > /dev/null
echo "Created and Activated Savings Account with ID: $ACCOUNT_ID"

# 6. Transaction 1: Deposit (03 June 2026)
fin_curl -X POST "$FIN_URL/savingsaccounts/$ACCOUNT_ID/transactions?command=deposit" -d '{
  "transactionDate": "03 June 2026",
  "transactionAmount": 1000,
  "dateFormat": "dd MMMM yyyy",
  "locale": "en"
}' > /dev/null
echo "Transaction 1: Deposit 1000 on 03 June 2026"

# 7. Set Business Date to 04 June 2026
fin_curl -X POST "$FIN_URL/businessdate" -d '{
  "type": "BUSINESS_DATE",
  "date": "04 June 2026",
  "dateFormat": "dd MMMM yyyy",
  "locale": "en"
}' > /dev/null

# 8. Transaction 2: Withdrawal (04 June 2026)
fin_curl -X POST "$FIN_URL/savingsaccounts/$ACCOUNT_ID/transactions?command=withdrawal" -d '{
  "transactionDate": "04 June 2026",
  "transactionAmount": 125,
  "dateFormat": "dd MMMM yyyy",
  "locale": "en"
}' > /dev/null
echo "Transaction 2: Withdrawal 125 on 04 June 2026"

# 9. Set Business Date to 05 June 2026
fin_curl -X POST "$FIN_URL/businessdate" -d '{
  "type": "BUSINESS_DATE",
  "date": "05 June 2026",
  "dateFormat": "dd MMMM yyyy",
  "locale": "en"
}' > /dev/null

# 10. Transaction 3: Deposit (05 June 2026)
fin_curl -X POST "$FIN_URL/savingsaccounts/$ACCOUNT_ID/transactions?command=deposit" -d '{
  "transactionDate": "05 June 2026",
  "transactionAmount": 50,
  "dateFormat": "dd MMMM yyyy",
  "locale": "en"
}' > /dev/null
echo "Transaction 3: Deposit 50 on 05 June 2026"

echo "Data setup complete. You can now test search on Account ID: $ACCOUNT_ID"
