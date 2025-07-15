#!/bin/bash

# SMS Testing Script with Real Twilio Configuration
# Make sure the application is running on port 9090

BASE_URL="http://localhost:9090"

echo "=== Bank X SMS Testing with Twilio Trial Account ==="
echo "Twilio Phone Number: +14846420893"
echo ""

# Test 1: Basic SMS Test (Note: In trial mode, you can only send to verified numbers)
echo "1. Testing Basic SMS to a verified number..."
echo "Note: In Twilio trial mode, you can only send SMS to verified phone numbers."
echo "Please verify your phone number in Twilio Console first."
echo ""

read -p "Enter your verified phone number (with country code, e.g., +27821234567): " PHONE_NUMBER

if [ -z "$PHONE_NUMBER" ]; then
    echo "No phone number provided. Skipping SMS test."
    exit 1
fi

echo "Sending test SMS to: $PHONE_NUMBER"

curl -X POST $BASE_URL/api/sms/test \
  -H "Content-Type: application/json" \
  -d "{
    \"phoneNumber\": \"$PHONE_NUMBER\",
    \"message\": \"Hello from Bank X! This is a test SMS from your Twilio trial account.\"
  }" \
  -w "\nStatus: %{http_code}\n" \
  -s | jq '.'

echo ""
echo "---"
echo ""

# Test 2: Check if customer exists and send transaction notification
echo "2. Testing Transaction SMS (if customer exists)..."

# First check if there are any customers
CUSTOMER_COUNT=$(curl -s $BASE_URL/api/customers | jq '.data | length')

if [ "$CUSTOMER_COUNT" -gt 0 ]; then
    echo "Found $CUSTOMER_COUNT customer(s). Testing transaction notification..."
    
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
else
    echo "No customers found. Please register a customer first."
fi

echo ""
echo "=== SMS Testing Complete ==="
echo ""
echo "Important Notes for Twilio Trial Account:"
echo "1. You can only send SMS to verified phone numbers"
echo "2. All SMS messages will include 'Sent from your Twilio trial account' prefix"
echo "3. To verify a phone number, go to: https://console.twilio.com/us1/develop/phone-numbers/manage/verified"
echo "4. To remove trial limitations, upgrade your Twilio account"