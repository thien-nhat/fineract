#!/usr/bin/env bash

source "$(dirname "$0")/common.sh"

echo "Creating Savings Product..."

# 1. Create Savings Product
RESPONSE_PRODUCT=$(fin_curl -X POST "$FIN_URL/savingsproducts" -d '{
  "name": "Practice Savings Product",
  "shortName": "PSP1",
  "description": "Savings product for practice task 4",
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

# 2. List Savings Products
echo "Current Savings Products:"
fin_curl "$FIN_URL/savingsproducts" | python -m json.tool
