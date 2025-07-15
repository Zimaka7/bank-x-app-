#!/bin/bash

# Bank X Application API Test Examples
# Make sure the application is running on http://localhost:8080

BASE_URL="http://localhost:8080/api"

echo "=== Bank X Application API Test Examples ==="
echo "Base URL: $BASE_URL"
echo ""

# Test 1: Get API Information
echo "1. Getting API Information..."
curl -X GET "$BASE_URL" \
  -H "Content-Type: application/json" | jq '.'
echo ""

# Test 2: Register a new customer
echo "2. Registering a new customer..."
CUSTOMER_RESPONSE=$(curl -s -X POST "$BASE_URL/customers" \
  -H "Content-Type: application/json" \
  -d '{
    "firstName": "John",
    "lastName": "Doe",
    "email": "john.doe@example.com",
    "phoneNumber": "+27123456789"
  }')

echo "$CUSTOMER_RESPONSE" | jq '.'

# Extract customer ID and account numbers
CUSTOMER_ID=$(echo "$CUSTOMER_RESPONSE" | jq -r '.data.id')
CURRENT_ACCOUNT=$(echo "$CUSTOMER_RESPONSE" | jq -r '.data.accounts[] | select(.accountType == "CURRENT") | .accountNumber')
SAVINGS_ACCOUNT=$(echo "$CUSTOMER_RESPONSE" | jq -r '.data.accounts[] | select(.accountType == "SAVINGS") | .accountNumber')

echo "Customer ID: $CUSTOMER_ID"
echo "Current Account: $CURRENT_ACCOUNT"
echo "Savings Account: $SAVINGS_ACCOUNT"
echo ""

# Test 3: Get customer details
echo "3. Getting customer details..."
curl -X GET "$BASE_URL/customers/$CUSTOMER_ID" \
  -H "Content-Type: application/json" | jq '.'
echo ""

# Test 4: Check account balances
echo "4. Checking account balances..."
echo "Current Account Balance:"
curl -X GET "$BASE_URL/accounts/$CURRENT_ACCOUNT/balance" \
  -H "Content-Type: application/json" | jq '.'

echo "Savings Account Balance:"
curl -X GET "$BASE_URL/accounts/$SAVINGS_ACCOUNT/balance" \
  -H "Content-Type: application/json" | jq '.'
echo ""

# Test 5: Register second customer for payment testing
echo "5. Registering second customer..."
CUSTOMER2_RESPONSE=$(curl -s -X POST "$BASE_URL/customers" \
  -H "Content-Type: application/json" \
  -d '{
    "firstName": "Jane",
    "lastName": "Smith",
    "email": "jane.smith@example.com",
    "phoneNumber": "+27987654321"
  }')

CUSTOMER2_ID=$(echo "$CUSTOMER2_RESPONSE" | jq -r '.data.id')
CUSTOMER2_CURRENT=$(echo "$CUSTOMER2_RESPONSE" | jq -r '.data.accounts[] | select(.accountType == "CURRENT") | .accountNumber')

echo "Second Customer ID: $CUSTOMER2_ID"
echo "Second Customer Current Account: $CUSTOMER2_CURRENT"
echo ""

# Test 6: Transfer money between accounts (same customer)
echo "6. Transferring money from Savings to Current account..."
curl -X POST "$BASE_URL/accounts/transfer" \
  -H "Content-Type: application/json" \
  -d "{
    \"fromAccountNumber\": \"$SAVINGS_ACCOUNT\",
    \"toAccountNumber\": \"$CURRENT_ACCOUNT\",
    \"amount\": 100.00,
    \"description\": \"Transfer to current account\"
  }" | jq '.'
echo ""

# Test 7: Make payment to another customer
echo "7. Making payment to another customer..."
curl -X POST "$BASE_URL/transactions/payment" \
  -H "Content-Type: application/json" \
  -d "{
    \"fromAccountNumber\": \"$CURRENT_ACCOUNT\",
    \"toAccountNumber\": \"$CUSTOMER2_CURRENT\",
    \"amount\": 50.00,
    \"description\": \"Payment for services\",
    \"reference\": \"PAY-2024-001\"
  }" | jq '.'
echo ""

# Test 8: Get transaction history
echo "8. Getting transaction history for current account..."
curl -X GET "$BASE_URL/transactions/account/$CURRENT_ACCOUNT" \
  -H "Content-Type: application/json" | jq '.'
echo ""

# Test 9: Bank Z single transaction
echo "9. Processing Bank Z single transaction..."
curl -X POST "$BASE_URL/bankz/transaction" \
  -H "Content-Type: application/json" \
  -d "{
    \"accountNumber\": \"$CURRENT_ACCOUNT\",
    \"amount\": 75.00,
    \"transactionType\": \"CREDIT\",
    \"reference\": \"BANKZ-TXN-001\"
  }" | jq '.'
echo ""

# Test 10: Bank Z batch transactions
echo "10. Processing Bank Z batch transactions..."
curl -X POST "$BASE_URL/bankz/transactions/batch" \
  -H "Content-Type: application/json" \
  -d "[
    {
      \"accountNumber\": \"$CURRENT_ACCOUNT\",
      \"amount\": 25.00,
      \"transactionType\": \"DEBIT\",
      \"reference\": \"BANKZ-BATCH-001\"
    },
    {
      \"accountNumber\": \"$SAVINGS_ACCOUNT\",
      \"amount\": 150.00,
      \"transactionType\": \"CREDIT\",
      \"reference\": \"BANKZ-BATCH-002\"
    }
  ]" | jq '.'
echo ""

# Test 11: Get customer notifications
echo "11. Getting customer notifications..."
curl -X GET "$BASE_URL/notifications/customer/$CUSTOMER_ID" \
  -H "Content-Type: application/json" | jq '.'
echo ""

# Test 12: Final account balances
echo "12. Final account balances..."
echo "Customer 1 - Current Account Balance:"
curl -X GET "$BASE_URL/accounts/$CURRENT_ACCOUNT/balance" \
  -H "Content-Type: application/json" | jq '.'

echo "Customer 1 - Savings Account Balance:"
curl -X GET "$BASE_URL/accounts/$SAVINGS_ACCOUNT/balance" \
  -H "Content-Type: application/json" | jq '.'

echo "Customer 2 - Current Account Balance:"
curl -X GET "$BASE_URL/accounts/$CUSTOMER2_CURRENT/balance" \
  -H "Content-Type: application/json" | jq '.'
echo ""

echo "=== API Testing Complete ==="