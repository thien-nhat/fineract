#!/usr/bin/env bash

source "$(dirname "$0")/common.sh"

echo "Managing Loan Account Lifecycle (Task 7)..."

# 1. Create Active Client
RESPONSE_CLIENT=$(fin_curl -X POST "$FIN_URL/clients" -d '{
  "officeId": 1,
  "legalFormId": 1,
  "firstname": "Task7",
  "lastname": "LoanClient",
  "active": true,
  "activationDate": "01 June 2026",
  "submittedOnDate": "01 June 2026",
  "dateFormat": "dd MMMM yyyy",
  "locale": "en"
}')
CLIENT_ID=$(echo "$RESPONSE_CLIENT" | python -c "import sys, json; print(json.load(sys.stdin)['clientId'])")
echo "Created Client with ID: $CLIENT_ID"

# 2. Create Loan Product
RESPONSE_PRODUCT=$(fin_curl -X POST "$FIN_URL/loanproducts" -d '{
  "name": "Practice Loan Product",
  "shortName": "PLP7",
  "currencyCode": "USD",
  "digitsAfterDecimal": 2,
  "inMultiplesOf": 0,
  "installmentAmountInMultiplesOf": 0,
  "principal": 1000.0,
  "numberOfInstallments": 12,
  "repaymentEvery": 1,
  "repaymentFrequencyType": 2,
  "interestRatePerPeriod": 1.0,
  "interestRateFrequencyType": 2,
  "amortizationType": 1,
  "interestType": 0,
  "interestCalculationPeriodType": 1,
  "transactionProcessingStrategyCode": "mifos-standard-strategy",
  "accountingRule": 1,
  "locale": "en",
  "daysInMonthType": 1,
  "daysInYearType": 1,
  "isInterestRecalculationEnabled": false
}')
PRODUCT_ID=$(echo "$RESPONSE_PRODUCT" | python -c "import sys, json; print(json.load(sys.stdin)['resourceId'])")
echo "Created Loan Product with ID: $PRODUCT_ID"

# 3. Submit Loan Application
RESPONSE_LOAN=$(fin_curl -X POST "$FIN_URL/loans" -d "{
  \"clientId\": $CLIENT_ID,
  \"productId\": $PRODUCT_ID,
  \"principal\": 1000.0,
  \"numberOfInstallments\": 12,
  \"repaymentEvery\": 1,
  \"repaymentFrequencyType\": 2,
  \"interestRatePerPeriod\": 1,
  \"amortizationType\": 1,
  \"interestType\": 0,
  \"interestCalculationPeriodType\": 1,
  \"loanTermFrequency\": 12,
  \"loanTermFrequencyType\": 2,
  \"submittedOnDate\": \"01 June 2026\",
  \"expectedDisbursementDate\": \"01 June 2026\",
  \"locale\": \"en\",
  \"dateFormat\": \"dd MMMM yyyy\",
  \"loanType\": \"individual\"
}")
LOAN_ID=$(echo "$RESPONSE_LOAN" | python -c "import sys, json; print(json.load(sys.stdin)['loanId'])")
echo "Submitted Loan Application with ID: $LOAN_ID"

# 4. Approve Loan
fin_curl -X POST "$FIN_URL/loans/$LOAN_ID?command=approve" -d '{
  "approvedOnDate": "02 June 2026",
  "dateFormat": "dd MMMM yyyy",
  "locale": "en"
}' > /dev/null
echo "Approved Loan with ID: $LOAN_ID"

# 5. Disburse Loan
fin_curl -X POST "$FIN_URL/loans/$LOAN_ID?command=disbursement" -d '{
  "actualDisbursementDate": "02 June 2026",
  "dateFormat": "dd MMMM yyyy",
  "locale": "en"
}' > /dev/null
echo "Disbursed Loan with ID: $LOAN_ID"

# 6. Verify Status
STATUS=$(fin_curl "$FIN_URL/loans/$LOAN_ID" | python -c "import sys, json; print(json.load(sys.stdin)['status']['value'])")
echo "Loan Status: $STATUS"
