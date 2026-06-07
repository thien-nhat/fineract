#!/usr/bin/env bash

source "$(dirname "$0")/common.sh"

echo "Setting up data for Loan Transactions (Task 8)..."

# 1. Enable Business Date
fin_curl -X PUT "$FIN_URL/configurations/name/enable-business-date" -d '{"enabled": true}' > /dev/null

# 2. Set Business Date
fin_curl -X POST "$FIN_URL/businessdate" -d '{
  "type": "BUSINESS_DATE",
  "date": "05 June 2026",
  "dateFormat": "dd MMMM yyyy",
  "locale": "en"
}' > /dev/null

# 3. Create Active Client
RESPONSE_CLIENT=$(fin_curl -X POST "$FIN_URL/clients" -d '{
  "officeId": 1,
  "legalFormId": 1,
  "firstname": "Task8",
  "lastname": "TransactionsClient",
  "active": true,
  "activationDate": "05 June 2026",
  "dateFormat": "dd MMMM yyyy",
  "locale": "en"
}')
CLIENT_ID=$(echo "$RESPONSE_CLIENT" | python -c "import sys, json; print(json.load(sys.stdin)['clientId'])")
echo "Created Client with ID: $CLIENT_ID"

# 4. Create Loan Product
RESPONSE_PRODUCT=$(fin_curl -X POST "$FIN_URL/loanproducts" -d '{
  "name": "Task 8 Loan Product",
  "shortName": "T8LP",
  "currencyCode": "USD",
  "digitsAfterDecimal": 2,
  "inMultiplesOf": 0,
  "principal": 1000.0,
  "numberOfInstallments": 12,
  "repaymentEvery": 1,
  "repaymentFrequencyType": 2,
  "interestRatePerPeriod": 1.5,
  "interestRateFrequencyType": 2,
  "amortizationType": 1,
  "interestType": 1,
  "interestCalculationPeriodType": 1,
  "transactionProcessingStrategyCode": "mifos-standard-strategy",
  "accountingRule": 1,
  "locale": "en",
  "daysInMonthType": 1,
  "daysInYearType": 1
}')
PRODUCT_ID=$(echo "$RESPONSE_PRODUCT" | python -c "import sys, json; print(json.load(sys.stdin)['resourceId'])")
echo "Created Loan Product with ID: $PRODUCT_ID"

# 5. Create Loan A (for Repayment)
RESPONSE_LOAN_A=$(fin_curl -X POST "$FIN_URL/loans" -d "{
  \"clientId\": $CLIENT_ID,
  \"productId\": $PRODUCT_ID,
  \"principal\": 1000,
  \"loanTermFrequency\": 12,
  \"loanTermFrequencyType\": 2,
  \"loanType\": \"individual\",
  \"numberOfRepayments\": 12,
  \"repaymentEvery\": 1,
  \"repaymentFrequencyType\": 2,
  \"interestRatePerPeriod\": 1.5,
  \"interestRateFrequencyType\": 2,
  \"amortizationType\": 1,
  \"interestType\": 1,
  \"interestCalculationPeriodType\": 1,
  \"transactionProcessingStrategyCode\": \"mifos-standard-strategy\",
  \"expectedDisbursementDate\": \"05 June 2026\",
  \"submittedOnDate\": \"05 June 2026\",
  \"dateFormat\": \"dd MMMM yyyy\",
  \"locale\": \"en\"
}")
LOAN_A_ID=$(echo "$RESPONSE_LOAN_A" | python -c "import sys, json; print(json.load(sys.stdin)['loanId'])")

fin_curl -X POST "$FIN_URL/loans/$LOAN_A_ID?command=approve" -d '{
  "approvedOnDate": "05 June 2026",
  "dateFormat": "dd MMMM yyyy",
  "locale": "en"
}' > /dev/null

fin_curl -X POST "$FIN_URL/loans/$LOAN_A_ID?command=disburse" -d '{
  "actualDisbursementDate": "05 June 2026",
  "transactionAmount": 1000,
  "dateFormat": "dd MMMM yyyy",
  "locale": "en"
}' > /dev/null
echo "Created and Disbursed Loan A with ID: $LOAN_A_ID"

# 6. Create Loan B (for Write-off)
RESPONSE_LOAN_B=$(fin_curl -X POST "$FIN_URL/loans" -d "{
  \"clientId\": $CLIENT_ID,
  \"productId\": $PRODUCT_ID,
  \"principal\": 800,
  \"loanTermFrequency\": 12,
  \"loanTermFrequencyType\": 2,
  \"loanType\": \"individual\",
  \"numberOfRepayments\": 12,
  \"repaymentEvery\": 1,
  \"repaymentFrequencyType\": 2,
  \"interestRatePerPeriod\": 1.5,
  \"interestRateFrequencyType\": 2,
  \"amortizationType\": 1,
  \"interestType\": 1,
  \"interestCalculationPeriodType\": 1,
  \"transactionProcessingStrategyCode\": \"mifos-standard-strategy\",
  \"expectedDisbursementDate\": \"05 June 2026\",
  \"submittedOnDate\": \"05 June 2026\",
  \"dateFormat\": \"dd MMMM yyyy\",
  \"locale\": \"en\"
}")
LOAN_B_ID=$(echo "$RESPONSE_LOAN_B" | python -c "import sys, json; print(json.load(sys.stdin)['loanId'])")

fin_curl -X POST "$FIN_URL/loans/$LOAN_B_ID?command=approve" -d '{
  "approvedOnDate": "05 June 2026",
  "dateFormat": "dd MMMM yyyy",
  "locale": "en"
}' > /dev/null

fin_curl -X POST "$FIN_URL/loans/$LOAN_B_ID?command=disburse" -d '{
  "actualDisbursementDate": "05 June 2026",
  "transactionAmount": 800,
  "dateFormat": "dd MMMM yyyy",
  "locale": "en"
}' > /dev/null
echo "Created and Disbursed Loan B with ID: $LOAN_B_ID"

# 7. Perform Repayment on Loan A
fin_curl -X POST "$FIN_URL/loans/$LOAN_A_ID/transactions?command=repayment" -d '{
  "transactionDate": "05 June 2026",
  "transactionAmount": 200,
  "dateFormat": "dd MMMM yyyy",
  "locale": "en"
}' > /dev/null
echo "Performed Repayment of 200 on Loan A"

# 8. Perform Write-off on Loan B
fin_curl -X POST "$FIN_URL/loans/$LOAN_B_ID/transactions?command=writeoff" -d '{
  "transactionDate": "05 June 2026",
  "dateFormat": "dd MMMM yyyy",
  "locale": "en",
  "note": "Write Off for Task 8"
}' > /dev/null
echo "Performed Write-off on Loan B"

echo "Data setup for Task 8 complete."
