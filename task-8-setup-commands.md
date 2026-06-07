# Task 8 Setup Commands

Muc tieu: dung data de lam `loan transactions`.

Task 8 can:
- it nhat 1 loan da `approved + disbursed`
- ly tuong la 2 loan:
  - `LOAN_A_ID`: dung cho `repayment` va `waiveinterest`
  - `LOAN_B_ID`: dung rieng cho `writeoff`

File nay dung data toi muc do tren, khong thuc hien repayment/waiveinterest/writeoff thay anh.

## 0. Bien moi truong dung chung

```bash
export BASE_URL='https://localhost:8443/fineract-provider/api/v1'
export AUTH='Basic bWlmb3M6cGFzc3dvcmQ='
export TENANT='default'
```

## 1. Bat business date

```bash
curl -k -X PUT "$BASE_URL/configurations/name/enable-business-date" \
  -H "Authorization: $AUTH" \
  -H "Fineract-Platform-TenantId: $TENANT" \
  -H "Content-Type: application/json" \
  -d '{"enabled": true}'
```

## 2. Dat business date = 05 June 2026

```bash
curl -k -X POST "$BASE_URL/businessdate" \
  -H "Authorization: $AUTH" \
  -H "Fineract-Platform-TenantId: $TENANT" \
  -H "Content-Type: application/json" \
  -d '{
    "type": "BUSINESS_DATE",
    "date": "05 June 2026",
    "dateFormat": "dd MMMM yyyy",
    "locale": "en"
  }'
```

## 3. Tao client active

```bash
curl -k -X POST "$BASE_URL/clients" \
  -H "Authorization: $AUTH" \
  -H "Fineract-Platform-TenantId: $TENANT" \
  -H "Content-Type: application/json" \
  -d '{
    "officeId": 1,
    "legalFormId": 1,
    "firstname": "Task8",
    "lastname": "TransactionsClient",
    "externalId": "TASK8-CLIENT-001",
    "active": true,
    "activationDate": "05 June 2026",
    "dateFormat": "dd MMMM yyyy",
    "locale": "en"
  }'
```

Lay `clientId` trong response roi gan:

```bash
export CLIENT_ID=<paste_client_id_here>
```

## 4. Tao loan product

```bash
curl -k -X POST "$BASE_URL/loanproducts" \
  -H "Authorization: $AUTH" \
  -H "Fineract-Platform-TenantId: $TENANT" \
  -H "Content-Type: application/json" \
  --data-binary @fineract-doc/src/docs/en/examples/loan-products/flat-interest-monthly.json
```

Lay `resourceId` trong response roi gan:

```bash
export LOAN_PRODUCT_ID=<paste_loan_product_id_here>
```

## 5. Tao loan A de test `repayment` va `waiveinterest`

```bash
curl -k -X POST "$BASE_URL/loans" \
  -H "Authorization: $AUTH" \
  -H "Fineract-Platform-TenantId: $TENANT" \
  -H "Content-Type: application/json" \
  -d "{
    \"clientId\": $CLIENT_ID,
    \"productId\": $LOAN_PRODUCT_ID,
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
  }"
```

Lay `loanId` trong response roi gan:

```bash
export LOAN_A_ID=<paste_loan_a_id_here>
```

## 6. Approve loan A

```bash
curl -k -X POST "$BASE_URL/loans/$LOAN_A_ID?command=approve" \
  -H "Authorization: $AUTH" \
  -H "Fineract-Platform-TenantId: $TENANT" \
  -H "Content-Type: application/json" \
  -d '{
    "approvedOnDate": "05 June 2026",
    "approvedLoanAmount": 1000,
    "expectedDisbursementDate": "05 June 2026",
    "dateFormat": "dd MMMM yyyy",
    "locale": "en"
  }'
```

## 7. Disburse loan A

```bash
curl -k -X POST "$BASE_URL/loans/$LOAN_A_ID?command=disburse" \
  -H "Authorization: $AUTH" \
  -H "Fineract-Platform-TenantId: $TENANT" \
  -H "Content-Type: application/json" \
  -d '{
    "actualDisbursementDate": "05 June 2026",
    "transactionAmount": 1000,
    "dateFormat": "dd MMMM yyyy",
    "locale": "en"
  }'
```

## 8. Tao loan B de test rieng `writeoff`

```bash
curl -k -X POST "$BASE_URL/loans" \
  -H "Authorization: $AUTH" \
  -H "Fineract-Platform-TenantId: $TENANT" \
  -H "Content-Type: application/json" \
  -d "{
    \"clientId\": $CLIENT_ID,
    \"productId\": $LOAN_PRODUCT_ID,
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
  }"
```

Lay `loanId` trong response roi gan:

```bash
export LOAN_B_ID=<paste_loan_b_id_here>
```

## 9. Approve loan B

```bash
curl -k -X POST "$BASE_URL/loans/$LOAN_B_ID?command=approve" \
  -H "Authorization: $AUTH" \
  -H "Fineract-Platform-TenantId: $TENANT" \
  -H "Content-Type: application/json" \
  -d '{
    "approvedOnDate": "05 June 2026",
    "approvedLoanAmount": 800,
    "expectedDisbursementDate": "05 June 2026",
    "dateFormat": "dd MMMM yyyy",
    "locale": "en"
  }'
```

## 10. Disburse loan B

```bash
curl -k -X POST "$BASE_URL/loans/$LOAN_B_ID?command=disburse" \
  -H "Authorization: $AUTH" \
  -H "Fineract-Platform-TenantId: $TENANT" \
  -H "Content-Type: application/json" \
  -d '{
    "actualDisbursementDate": "05 June 2026",
    "transactionAmount": 800,
    "dateFormat": "dd MMMM yyyy",
    "locale": "en"
  }'
```

## 11. Kiem tra 2 loan da active

```bash
curl -k "$BASE_URL/loans/$LOAN_A_ID?associations=transactions" \
  -H "Authorization: $AUTH" \
  -H "Fineract-Platform-TenantId: $TENANT"
```

```bash
curl -k "$BASE_URL/loans/$LOAN_B_ID?associations=transactions" \
  -H "Authorization: $AUTH" \
  -H "Fineract-Platform-TenantId: $TENANT"
```

## 12. Day la diem bat dau cua Task 8

Loan A:
- repayment
- waive interest

Loan B:
- write-off

Endpoints anh se dung trong task 8:

Repayment:

```bash
curl -k -X POST "$BASE_URL/loans/$LOAN_A_ID/transactions?command=repayment" \
  -H "Authorization: $AUTH" \
  -H "Fineract-Platform-TenantId: $TENANT" \
  -H "Content-Type: application/json" \
  -d '{
    "transactionDate": "05 June 2026",
    "transactionAmount": 200,
    "dateFormat": "dd MMMM yyyy",
    "locale": "en"
  }'
```

Waive interest:

```bash
curl -k -X POST "$BASE_URL/loans/$LOAN_A_ID/transactions?command=waiveinterest" \
  -H "Authorization: $AUTH" \
  -H "Fineract-Platform-TenantId: $TENANT" \
  -H "Content-Type: application/json" \
  -d '{
    "transactionDate": "05 June 2026",
    "dateFormat": "dd MMMM yyyy",
    "locale": "en"
  }'
```

Write-off:

```bash
curl -k -X POST "$BASE_URL/loans/$LOAN_B_ID/transactions?command=writeoff" \
  -H "Authorization: $AUTH" \
  -H "Fineract-Platform-TenantId: $TENANT" \
  -H "Content-Type: application/json" \
  -d '{
    "transactionDate": "05 June 2026",
    "dateFormat": "dd MMMM yyyy",
    "locale": "en",
    "note": "Write Off"
  }'
```
