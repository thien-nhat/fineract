# Task 7 Setup Commands

Muc tieu: dung prerequisites de lam `loan lifecycle`.

Task 7 nen bat dau khi anh da co:
- `CLIENT_ID` active
- `LOAN_PRODUCT_ID` hop le

File nay co chu y khong approve/disburse loan thay anh.
No chi dung lai o muc "du data de vao task 7".

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

## 3. Kiem tra `officeId`

```bash
curl -k "$BASE_URL/offices?includeAllOffices=true" \
  -H "Authorization: $AUTH" \
  -H "Fineract-Platform-TenantId: $TENANT"
```

Mac dinh cac lenh duoi day dung `officeId = 1`.

## 4. Tao client active

```bash
curl -k -X POST "$BASE_URL/clients" \
  -H "Authorization: $AUTH" \
  -H "Fineract-Platform-TenantId: $TENANT" \
  -H "Content-Type: application/json" \
  -d '{
    "officeId": 1,
    "legalFormId": 1,
    "firstname": "Task7",
    "lastname": "LifecycleClient",
    "externalId": "TASK7-CLIENT-001",
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

## 5. Tao loan product

Repo da co san file example chinh thong:
- `fineract-doc/src/docs/en/examples/loan-products/flat-interest-monthly.json`

Lenh tao:

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

Neu muon xem lai danh sach:

```bash
curl -k "$BASE_URL/loanproducts?fields=id,name,shortName" \
  -H "Authorization: $AUTH" \
  -H "Fineract-Platform-TenantId: $TENANT"
```

## 6. Sanity check template truoc khi vao task 7

```bash
curl -k "$BASE_URL/loans/template?clientId=$CLIENT_ID&productId=$LOAN_PRODUCT_ID" \
  -H "Authorization: $AUTH" \
  -H "Fineract-Platform-TenantId: $TENANT"
```

## 7. Day la diem bat dau cua Task 7

Khi vao task 7, anh moi bat dau goi:
- `POST /loans`
- `POST /loans/{loanId}?command=approve`
- `POST /loans/{loanId}?command=disburse`

Payload submit loan toi thieu, de anh dung lam diem xuat phat:

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

Neu tenant cua anh khong nhan `mifos-standard-strategy`, kiem tra trong template va thay bang strategy code dang co trong tenant do.
