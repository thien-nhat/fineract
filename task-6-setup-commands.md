# Task 6 Setup Commands

Muc tieu: dung data de lam `savings transaction search`.

Task 6 can:
- bat `business date`
- tao `client` active
- tao `savings product`
- tao `savings account`
- approve + activate
- tao 3 transactions tren 3 business dates khac nhau

Sau khi xong file nay, anh co:
- `CLIENT_ID`
- `SAVINGS_PRODUCT_ID`
- `SAVINGS_ACCOUNT_ID`
- 3 transactions de test search theo date, amount, type

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

## 2. Dat business date = 03 June 2026

Endpoint nay den tu `BusinessDateApiResource` trong repo: `POST /v1/businessdate`.

```bash
curl -k -X POST "$BASE_URL/businessdate" \
  -H "Authorization: $AUTH" \
  -H "Fineract-Platform-TenantId: $TENANT" \
  -H "Content-Type: application/json" \
  -d '{
    "type": "BUSINESS_DATE",
    "date": "03 June 2026",
    "dateFormat": "dd MMMM yyyy",
    "locale": "en"
  }'
```

Kiem tra nhanh:

```bash
curl -k "$BASE_URL/businessdate/BUSINESS_DATE" \
  -H "Authorization: $AUTH" \
  -H "Fineract-Platform-TenantId: $TENANT"
```

## 3. Kiem tra `officeId`

Neu tenant local cua anh khong dung office mac dinh `1`, lay office id that su truoc:

```bash
curl -k "$BASE_URL/offices?includeAllOffices=true" \
  -H "Authorization: $AUTH" \
  -H "Fineract-Platform-TenantId: $TENANT"
```

Trong cac lenh duoi day, mac dinh dung `officeId = 1` nhu E2E test fixtures.

## 4. Tao 1 client active

Lenh nay tao thang client active de khong can goi them `POST /clients/{id}?command=activate`.

```bash
curl -k -X POST "$BASE_URL/clients" \
  -H "Authorization: $AUTH" \
  -H "Fineract-Platform-TenantId: $TENANT" \
  -H "Content-Type: application/json" \
  -d '{
    "officeId": 1,
    "legalFormId": 1,
    "firstname": "Task6",
    "lastname": "SearchClient",
    "externalId": "TASK6-CLIENT-001",
    "active": true,
    "activationDate": "03 June 2026",
    "dateFormat": "dd MMMM yyyy",
    "locale": "en"
  }'
```

Lay `clientId` trong response roi gan:

```bash
export CLIENT_ID=<paste_client_id_here>
```

## 5. Tao savings product

Payload nay theo `SavingsProductRequestFactory.defaultSavingsProductRequest()` trong E2E tests.

```bash
curl -k -X POST "$BASE_URL/savingsproducts" \
  -H "Authorization: $AUTH" \
  -H "Fineract-Platform-TenantId: $TENANT" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "CEUR-TASK6",
    "shortName": "T6EU",
    "description": "",
    "currencyCode": "EUR",
    "digitsAfterDecimal": 2,
    "inMultiplesOf": 0,
    "nominalAnnualInterestRate": 0.0,
    "interestCompoundingPeriodType": 1,
    "interestPostingPeriodType": 4,
    "interestCalculationType": 1,
    "interestCalculationDaysInYearType": 365,
    "accountingRule": 1,
    "charges": [],
    "locale": "en"
  }'
```

Lay `resourceId` trong response roi gan:

```bash
export SAVINGS_PRODUCT_ID=<paste_savings_product_id_here>
```

## 6. Submit savings account

```bash
curl -k -X POST "$BASE_URL/savingsaccounts" \
  -H "Authorization: $AUTH" \
  -H "Fineract-Platform-TenantId: $TENANT" \
  -H "Content-Type: application/json" \
  -d "{
    \"clientId\": $CLIENT_ID,
    \"productId\": $SAVINGS_PRODUCT_ID,
    \"submittedOnDate\": \"03 June 2026\",
    \"dateFormat\": \"dd MMMM yyyy\",
    \"locale\": \"en\"
  }"
```

Lay `savingsId` trong response roi gan:

```bash
export SAVINGS_ACCOUNT_ID=<paste_savings_account_id_here>
```

## 7. Approve savings account

```bash
curl -k -X POST "$BASE_URL/savingsaccounts/$SAVINGS_ACCOUNT_ID?command=approve" \
  -H "Authorization: $AUTH" \
  -H "Fineract-Platform-TenantId: $TENANT" \
  -H "Content-Type: application/json" \
  -d '{
    "approvedOnDate": "03 June 2026",
    "dateFormat": "dd MMMM yyyy",
    "locale": "en"
  }'
```

## 8. Activate savings account

```bash
curl -k -X POST "$BASE_URL/savingsaccounts/$SAVINGS_ACCOUNT_ID?command=activate" \
  -H "Authorization: $AUTH" \
  -H "Fineract-Platform-TenantId: $TENANT" \
  -H "Content-Type: application/json" \
  -d '{
    "activatedOnDate": "03 June 2026",
    "dateFormat": "dd MMMM yyyy",
    "locale": "en"
  }'
```

## 9. Tao transaction 1: deposit vao 03 June 2026

```bash
curl -k -X POST "$BASE_URL/savingsaccounts/$SAVINGS_ACCOUNT_ID/transactions?command=deposit" \
  -H "Authorization: $AUTH" \
  -H "Fineract-Platform-TenantId: $TENANT" \
  -H "Content-Type: application/json" \
  -d '{
    "transactionDate": "03 June 2026",
    "transactionAmount": 1000,
    "dateFormat": "dd MMMM yyyy",
    "locale": "en"
  }'
```

## 10. Chuyen business date sang 04 June 2026

```bash
curl -k -X POST "$BASE_URL/businessdate" \
  -H "Authorization: $AUTH" \
  -H "Fineract-Platform-TenantId: $TENANT" \
  -H "Content-Type: application/json" \
  -d '{
    "type": "BUSINESS_DATE",
    "date": "04 June 2026",
    "dateFormat": "dd MMMM yyyy",
    "locale": "en"
  }'
```

## 11. Tao transaction 2: withdrawal vao 04 June 2026

```bash
curl -k -X POST "$BASE_URL/savingsaccounts/$SAVINGS_ACCOUNT_ID/transactions?command=withdrawal" \
  -H "Authorization: $AUTH" \
  -H "Fineract-Platform-TenantId: $TENANT" \
  -H "Content-Type: application/json" \
  -d '{
    "transactionDate": "04 June 2026",
    "transactionAmount": 125,
    "dateFormat": "dd MMMM yyyy",
    "locale": "en"
  }'
```

## 12. Chuyen business date sang 05 June 2026

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

## 13. Tao transaction 3: deposit vao 05 June 2026

```bash
curl -k -X POST "$BASE_URL/savingsaccounts/$SAVINGS_ACCOUNT_ID/transactions?command=deposit" \
  -H "Authorization: $AUTH" \
  -H "Fineract-Platform-TenantId: $TENANT" \
  -H "Content-Type: application/json" \
  -d '{
    "transactionDate": "05 June 2026",
    "transactionAmount": 50,
    "dateFormat": "dd MMMM yyyy",
    "locale": "en"
  }'
```

## 14. Kiem tra data vua tao

```bash
curl -k "$BASE_URL/savingsaccounts/$SAVINGS_ACCOUNT_ID?associations=transactions" \
  -H "Authorization: $AUTH" \
  -H "Fineract-Platform-TenantId: $TENANT"
```

## 15. Search examples de lam Task 6

Search theo date range:

```bash
curl -k "$BASE_URL/savingsaccounts/$SAVINGS_ACCOUNT_ID/transactions/search?fromDate=2026-06-04&toDate=2026-06-05&dateFormat=yyyy-MM-dd&locale=en" \
  -H "Authorization: $AUTH" \
  -H "Fineract-Platform-TenantId: $TENANT"
```

Search theo amount:

```bash
curl -k "$BASE_URL/savingsaccounts/$SAVINGS_ACCOUNT_ID/transactions/search?fromAmount=100&toAmount=1000" \
  -H "Authorization: $AUTH" \
  -H "Fineract-Platform-TenantId: $TENANT"
```

Search theo credit/debit:

```bash
curl -k "$BASE_URL/savingsaccounts/$SAVINGS_ACCOUNT_ID/transactions/search?credit=true" \
  -H "Authorization: $AUTH" \
  -H "Fineract-Platform-TenantId: $TENANT"
```

```bash
curl -k "$BASE_URL/savingsaccounts/$SAVINGS_ACCOUNT_ID/transactions/search?debit=true" \
  -H "Authorization: $AUTH" \
  -H "Fineract-Platform-TenantId: $TENANT"
```
