#!/usr/bin/env bash

source "$(dirname "$0")/common.sh"

echo "Managing Charges (Task 13)..."

# 1. Create a Loan Charge
CHARGE_NAME="LoanProcessingFee_$(date +%s)"
RESPONSE_CHARGE=$(fin_curl -X POST "$FIN_URL/charges" -d "{
  \"name\": \"$CHARGE_NAME\",
  \"amount\": 50,
  \"currencyCode\": \"USD\",
  \"chargeAppliesTo\": 1,
  \"chargeCalculationType\": 1,
  \"chargeTimeType\": 1,
  \"active\": true,
  \"locale\": \"en\"
}")
CHARGE_ID=$(echo "$RESPONSE_CHARGE" | python -c "import sys, json; print(json.load(sys.stdin)['resourceId'])")
echo "Created Charge: $CHARGE_NAME with ID: $CHARGE_ID"

# 2. Create Active Client
RESPONSE_CLIENT=$(fin_curl -X POST "$FIN_URL/clients" -d '{
  "officeId": 1,
  "legalFormId": 1,
  "firstname": "Task13",
  "lastname": "ChargeClient",
  "active": true,
  "activationDate": "01 June 2026",
  "submittedOnDate": "01 June 2026",
  "dateFormat": "dd MMMM yyyy",
  "locale": "en"
}')
CLIENT_ID=$(echo "$RESPONSE_CLIENT" | python -c "import sys, json; print(json.load(sys.stdin)['clientId'])")
echo "Created Client with ID: $CLIENT_ID"

# 3. Create Loan Product (Accounting Cash Based)
RESPONSE_PRODUCT=$(fin_curl -X POST "$FIN_URL/loanproducts" -d '{
  "name": "Charge Loan Product",
  "shortName": "CLP13",
  "currencyCode": "USD",
  "digitsAfterDecimal": 2,
  "inMultiplesOf": 0,
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
  "daysInYearType": 1
}')
PRODUCT_ID=$(echo "$RESPONSE_PRODUCT" | python -c "import sys, json; print(json.load(sys.stdin)['resourceId'])")
echo "Created Loan Product with ID: $PRODUCT_ID"

# 4. Submit Loan Application with Charge
RESPONSE_LOAN=$(fin_curl -X POST "$FIN_URL/loans" -d "{
  \"clientId\": $CLIENT_ID,
  \"productId\": $PRODUCT_ID,
  \"principal\": 1000,
  \"loanTermFrequency\": 12,
  \"loanTermFrequencyType\": 2,
  \"numberOfRepayments\": 12,
  \"repaymentEvery\": 1,
  \"repaymentFrequencyType\": 2,
  \"interestRatePerPeriod\": 1,
  \"amortizationType\": 1,
  \"interestType\": 0,
  \"interestCalculationPeriodType\": 1,
  \"expectedDisbursementDate\": \"01 June 2026\",
  \"submittedOnDate\": \"01 June 2026\",
  \"loanType\": \"individual\",
  \"dateFormat\": \"dd MMMM yyyy\",
  \"locale\": \"en\",
  \"charges\": [
    {
      \"chargeId\": $CHARGE_ID,
      \"amount\": 50
    }
  ]
}")
LOAN_ID=$(echo "$RESPONSE_LOAN" | python -c "import sys, json; print(json.load(sys.stdin)['loanId'])")
echo "Submitted Loan Application with ID: $LOAN_ID and Charge ID: $CHARGE_ID"

# 5. Approve and Disburse
fin_curl -X POST "$FIN_URL/loans/$LOAN_ID?command=approve" -d '{
  "approvedOnDate": "01 June 2026",
  "dateFormat": "dd MMMM yyyy",
  "locale": "en"
}' > /dev/null

fin_curl -X POST "$FIN_URL/loans/$LOAN_ID?command=disbursement" -d '{
  "actualDisbursementDate": "01 June 2026",
  "dateFormat": "dd MMMM yyyy",
  "locale": "en"
}' > /dev/null
echo "Loan Disbursed. Charge should be active."

# 6. List Loan Charges
echo "Loan Charges for Loan ID $LOAN_ID:"
fin_curl "$FIN_URL/loans/$LOAN_ID/charges" | python -m json.tool
