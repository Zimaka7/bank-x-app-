#!/bin/bash

# Customer Registration Testing Script for Bank X Application
# Make sure the application is running on port 9090

BASE_URL="http://localhost:9090"

echo "=== Bank X Customer Registration Testing ==="
echo ""

# Test 1: Register customer without SA ID (should work now)
echo "1. Testing Customer Registration without SA ID..."
curl -X POST $BASE_URL/api/customers \
  -H "Content-Type: application/json" \
  -d '{
    "firstName": "John",
    "lastName": "Doe",
    "email": "john.doe@test.com",
    "phoneNumber": "+27123456789"
  }' \
  -w "\nStatus: %{http_code}\n" \
  -s | jq '.'

echo ""
echo "---"
echo ""

# Test 2: Register customer with SA ID using DTO endpoint
echo "2. Testing Customer Registration with SA ID (DTO endpoint)..."
curl -X POST $BASE_URL/api/customers/register \
  -H "Content-Type: application/json" \
  -d '{
    "firstName": "Jane",
    "lastName": "Smith",
    "email": "jane.smith@test.com",
    "phoneNumber": "+27987654321",
    "saIdNumber": "9001015009087"
  }' \
  -w "\nStatus: %{http_code}\n" \
  -s | jq '.'

echo ""
echo "---"
echo ""

# Test 3: Get all customers
echo "3. Testing Get All Customers..."
curl -X GET $BASE_URL/api/customers \
  -H "Content-Type: application/json" \
  -w "\nStatus: %{http_code}\n" \
  -s | jq '.'

echo ""
echo "---"
echo ""

# Test 4: Test invalid email
echo "4. Testing Invalid Email Registration..."
curl -X POST $BASE_URL/api/customers \
  -H "Content-Type: application/json" \
  -d '{
    "firstName": "Invalid",
    "lastName": "Email",
    "email": "invalid-email",
    "phoneNumber": "+27111222333"
  }' \
  -w "\nStatus: %{http_code}\n" \
  -s | jq '.'

echo ""
echo "=== Customer Registration Testing Complete ==="
echo ""
echo "Note: Make sure the application is running before executing this script."
echo "Start the application with: mvn spring-boot:run"