#!/bin/bash

# SMS Testing Script for Bank X Application
# Make sure the application is running on port 9090

BASE_URL="http://localhost:9090"

echo "=== Bank X SMS Notification Testing ==="
echo ""

# Test 1: Basic SMS Test
echo "1. Testing Basic SMS..."
curl -X POST $BASE_URL/api/sms/test \
  -H "Content-Type: application/json" \
  -d '{
    "phoneNumber": "+27821234567",
    "message": "Hello from Bank X! This is a test SMS."
  }' \
  -w "\nStatus: %{http_code}\n" \
  -s | jq '.'

echo ""
echo "---"
echo ""

# Test 2: Transaction SMS Test (requires customer ID 1 to exist)
echo "2. Testing Transaction SMS..."
curl -X POST $BASE_URL/api/sms/transaction-test \
  -H "Content-Type: application/json" \
  -d '{
    "customerId": 1,
    "transactionType": "credit",
    "amount": 100.0,
    "accountType": "Savings",
    "newBalance": 600.0
  }' \
  -w "\nStatus: %{http_code}\n" \
  -s | jq '.'

echo ""
echo "---"
echo ""

# Test 3: Welcome SMS Test (requires customer ID 1 to exist)
echo "3. Testing Welcome SMS..."
curl -X POST $BASE_URL/api/sms/welcome-test \
  -H "Content-Type: application/json" \
  -d '{
    "customerId": 1,
    "bonusAmount": 500.0
  }' \
  -w "\nStatus: %{http_code}\n" \
  -s | jq '.'

echo ""
echo "---"
echo ""

# Test 4: OTP SMS Test (requires customer ID 1 to exist)
echo "4. Testing OTP SMS..."
curl -X POST $BASE_URL/api/sms/otp-test \
  -H "Content-Type: application/json" \
  -d '{
    "customerId": 1,
    "otp": "123456"
  }' \
  -w "\nStatus: %{http_code}\n" \
  -s | jq '.'

echo ""
echo "=== SMS Testing Complete ==="
echo ""
echo "Note: For tests 2-4, make sure customer ID 1 exists in the database."
echo "You can create a customer first using the registration endpoint."