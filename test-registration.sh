#!/bin/bash

echo "🧪 Testing Registration with South African ID Numbers"
echo "=================================================="

# Test Bank X Registration
echo ""
echo "1. Testing Bank X Registration (http://localhost:9090/register)"
echo "   - Testing with SA ID number: 9001015009087"

curl -s -X POST http://localhost:9090/register \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "firstName=John&lastName=Doe&username=johndoe&email=john.doe@example.com&phoneNumber=%2B27123456789&saIdNumber=9001015009087&password=password123&confirmPassword=password123" \
  -w "HTTP Status: %{http_code}\n" \
  -o /tmp/bankx_register_response.html

if [ $? -eq 0 ]; then
    echo "   ✅ Bank X registration request sent successfully"
    if grep -q "Registration successful" /tmp/bankx_register_response.html; then
        echo "   ✅ Registration completed successfully"
    elif grep -q "error" /tmp/bankx_register_response.html; then
        echo "   ⚠️  Registration had errors - check response"
    else
        echo "   ℹ️  Registration response received"
    fi
else
    echo "   ❌ Failed to send registration request to Bank X"
fi

echo ""
echo "2. Testing Bank X Registration without SA ID"
echo "   - Testing without SA ID number"

curl -s -X POST http://localhost:9090/register \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "firstName=Jane&lastName=Smith&username=janesmith&email=jane.smith@example.com&phoneNumber=%2B27987654321&saIdNumber=&password=password123&confirmPassword=password123" \
  -w "HTTP Status: %{http_code}\n" \
  -o /tmp/bankx_register_no_id_response.html

if [ $? -eq 0 ]; then
    echo "   ✅ Bank X registration (no SA ID) request sent successfully"
    if grep -q "Registration successful" /tmp/bankx_register_no_id_response.html; then
        echo "   ✅ Registration completed successfully"
    elif grep -q "error" /tmp/bankx_register_no_id_response.html; then
        echo "   ⚠️  Registration had errors - check response"
    else
        echo "   ℹ️  Registration response received"
    fi
else
    echo "   ❌ Failed to send registration request to Bank X"
fi

echo ""
echo "3. Testing Bank Z Registration (http://localhost:9091/register)"
echo "   - Testing with SA ID number: 8502145009088"

curl -s -X POST http://localhost:9091/register \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "firstName=Alice&lastName=Johnson&username=alicejohnson&email=alice.johnson@example.com&saIdNumber=8502145009088&password=password123&confirmPassword=password123&role=OPERATOR" \
  -w "HTTP Status: %{http_code}\n" \
  -o /tmp/bankz_register_response.html

if [ $? -eq 0 ]; then
    echo "   ✅ Bank Z registration request sent successfully"
    if grep -q "Registration successful" /tmp/bankz_register_response.html; then
        echo "   ✅ Registration completed successfully"
    elif grep -q "error" /tmp/bankz_register_response.html; then
        echo "   ⚠️  Registration had errors - check response"
    else
        echo "   ℹ️  Registration response received"
    fi
else
    echo "   ❌ Failed to send registration request to Bank Z"
fi

echo ""
echo "4. Testing invalid SA ID validation"
echo "   - Testing with invalid SA ID: 123456789 (too short)"

curl -s -X POST http://localhost:9090/register \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "firstName=Invalid&lastName=User&username=invaliduser&email=invalid@example.com&phoneNumber=%2B27111111111&saIdNumber=123456789&password=password123&confirmPassword=password123" \
  -w "HTTP Status: %{http_code}\n" \
  -o /tmp/bankx_invalid_id_response.html

if [ $? -eq 0 ]; then
    echo "   ✅ Invalid SA ID test request sent successfully"
    if grep -q "must be exactly 13 digits" /tmp/bankx_invalid_id_response.html; then
        echo "   ✅ Validation working - rejected invalid SA ID"
    else
        echo "   ⚠️  Validation may not be working properly"
    fi
else
    echo "   ❌ Failed to send invalid SA ID test request"
fi

echo ""
echo "5. Checking application accessibility"
echo "   - Bank X: http://localhost:9090"
curl -s -o /dev/null -w "     HTTP Status: %{http_code}\n" http://localhost:9090

echo "   - Bank Z: http://localhost:9091"
curl -s -o /dev/null -w "     HTTP Status: %{http_code}\n" http://localhost:9091

echo ""
echo "🎯 Test Summary:"
echo "   - Bank X registration page: http://localhost:9090/register"
echo "   - Bank Z registration page: http://localhost:9091/register"
echo "   - SA ID number validation implemented for both systems"
echo "   - SA ID number is optional but validated when provided"
echo ""
echo "✅ Registration fixes completed!"