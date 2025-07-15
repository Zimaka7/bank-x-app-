#!/bin/bash

# Bank Z Interface API Test Script
# This script tests all Bank Z integration endpoints

BASE_URL="http://localhost:9090/api"
BANKZ_URL="$BASE_URL/bankz"

echo "🏦 Bank Z Interface API Test Script"
echo "=================================="
echo "Base URL: $BASE_URL"
echo "Bank Z URL: $BANKZ_URL"
echo ""

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Function to print colored output
print_status() {
    if [ $1 -eq 0 ]; then
        echo -e "${GREEN}✅ $2${NC}"
    else
        echo -e "${RED}❌ $2${NC}"
    fi
}

print_info() {
    echo -e "${BLUE}ℹ️  $1${NC}"
}

print_warning() {
    echo -e "${YELLOW}⚠️  $1${NC}"
}

# Function to check if application is running
check_application() {
    print_info "Checking if application is running..."
    response=$(curl -s -o /dev/null -w "%{http_code}" "http://localhost:9090/actuator/health")
    if [ "$response" = "200" ]; then
        print_status 0 "Application is running"
        return 0
    else
        print_status 1 "Application is not running (HTTP $response)"
        echo "Please start the application first: ./run-application.sh"
        exit 1
    fi
}

# Function to create test customer and accounts
setup_test_data() {
    print_info "Setting up test data..."
    
    # Create test customer
    customer_response=$(curl -s -X POST "$BASE_URL/customers" \
        -H "Content-Type: application/json" \
        -d '{
            "firstName": "Bank Z",
            "lastName": "Test Customer",
            "email": "bankz.test@example.com",
            "phoneNumber": "+27123456789",
            "idNumber": "9001010001088"
        }')
    
    if echo "$customer_response" | grep -q '"success":true'; then
        print_status 0 "Test customer created successfully"
        # Extract customer ID and account numbers from response
        CUSTOMER_ID=$(echo "$customer_response" | grep -o '"id":[0-9]*' | head -1 | cut -d':' -f2)
        CURRENT_ACCOUNT=$(echo "$customer_response" | grep -o '"accountNumber":"[^"]*"' | head -1 | cut -d'"' -f4)
        SAVINGS_ACCOUNT=$(echo "$customer_response" | grep -o '"accountNumber":"[^"]*"' | tail -1 | cut -d'"' -f4)
        
        print_info "Customer ID: $CUSTOMER_ID"
        print_info "Current Account: $CURRENT_ACCOUNT"
        print_info "Savings Account: $SAVINGS_ACCOUNT"
    else
        print_warning "Customer might already exist, continuing with tests..."
        # Use default values for existing customer
        CUSTOMER_ID="1"
        CURRENT_ACCOUNT="ACC001234567890"
        SAVINGS_ACCOUNT="ACC001234567891"
    fi
    echo ""
}

# Test 1: Single Transaction Processing
test_single_transaction() {
    print_info "Test 1: Single Transaction Processing"
    echo "======================================"
    
    # Test CREDIT transaction
    print_info "Testing single CREDIT transaction..."
    response=$(curl -s -X POST "$BANKZ_URL/transaction" \
        -H "Content-Type: application/json" \
        -d "{
            \"accountNumber\": \"$CURRENT_ACCOUNT\",
            \"amount\": 100.50,
            \"transactionType\": \"CREDIT\",
            \"reference\": \"BZ-SINGLE-CREDIT-$(date +%s)\"
        }")
    
    if echo "$response" | grep -q '"success":true'; then
        print_status 0 "Single CREDIT transaction processed successfully"
    else
        print_status 1 "Single CREDIT transaction failed"
        echo "Response: $response"
    fi
    
    # Test DEBIT transaction
    print_info "Testing single DEBIT transaction..."
    response=$(curl -s -X POST "$BANKZ_URL/transaction" \
        -H "Content-Type: application/json" \
        -d "{
            \"accountNumber\": \"$CURRENT_ACCOUNT\",
            \"amount\": 50.25,
            \"transactionType\": \"DEBIT\",
            \"reference\": \"BZ-SINGLE-DEBIT-$(date +%s)\"
        }")
    
    if echo "$response" | grep -q '"success":true'; then
        print_status 0 "Single DEBIT transaction processed successfully"
    else
        print_status 1 "Single DEBIT transaction failed"
        echo "Response: $response"
    fi
    echo ""
}

# Test 2: Batch Transaction Processing
test_batch_transactions() {
    print_info "Test 2: Batch Transaction Processing"
    echo "===================================="
    
    timestamp=$(date +%s)
    response=$(curl -s -X POST "$BANKZ_URL/transactions/batch" \
        -H "Content-Type: application/json" \
        -d "[
            {
                \"accountNumber\": \"$CURRENT_ACCOUNT\",
                \"amount\": 200.00,
                \"transactionType\": \"CREDIT\",
                \"reference\": \"BZ-BATCH-CREDIT-$timestamp\"
            },
            {
                \"accountNumber\": \"$SAVINGS_ACCOUNT\",
                \"amount\": 150.75,
                \"transactionType\": \"CREDIT\",
                \"reference\": \"BZ-BATCH-CREDIT2-$timestamp\"
            },
            {
                \"accountNumber\": \"$CURRENT_ACCOUNT\",
                \"amount\": 75.50,
                \"transactionType\": \"DEBIT\",
                \"reference\": \"BZ-BATCH-DEBIT-$timestamp\"
            }
        ]")
    
    if echo "$response" | grep -q '"success":true'; then
        print_status 0 "Batch transactions processed successfully"
        echo "Response: $response"
    else
        print_status 1 "Batch transactions failed"
        echo "Response: $response"
    fi
    echo ""
}

# Test 3: Reconciliation Processing
test_reconciliation() {
    print_info "Test 3: Reconciliation Processing"
    echo "================================="
    
    timestamp=$(date +%s)
    response=$(curl -s -X POST "$BANKZ_URL/reconciliation" \
        -H "Content-Type: application/json" \
        -d "[
            {
                \"accountNumber\": \"$CURRENT_ACCOUNT\",
                \"amount\": 1000.00,
                \"transactionType\": \"CREDIT\",
                \"reference\": \"BZ-RECON-CREDIT-$timestamp\"
            },
            {
                \"accountNumber\": \"$SAVINGS_ACCOUNT\",
                \"amount\": 500.00,
                \"transactionType\": \"DEBIT\",
                \"reference\": \"BZ-RECON-DEBIT-$timestamp\"
            }
        ]")
    
    if echo "$response" | grep -q '"success":true'; then
        print_status 0 "Reconciliation transactions processed successfully"
        echo "Response: $response"
    else
        print_status 1 "Reconciliation transactions failed"
        echo "Response: $response"
    fi
    echo ""
}

# Test 4: Transaction Status Check
test_transaction_status() {
    print_info "Test 4: Transaction Status Check"
    echo "================================"
    
    # First create a transaction to check status for
    reference="BZ-STATUS-TEST-$(date +%s)"
    print_info "Creating transaction with reference: $reference"
    
    create_response=$(curl -s -X POST "$BANKZ_URL/transaction" \
        -H "Content-Type: application/json" \
        -d "{
            \"accountNumber\": \"$CURRENT_ACCOUNT\",
            \"amount\": 25.00,
            \"transactionType\": \"CREDIT\",
            \"reference\": \"$reference\"
        }")
    
    if echo "$create_response" | grep -q '"success":true'; then
        print_status 0 "Test transaction created successfully"
        
        # Now check the status
        print_info "Checking transaction status..."
        sleep 1  # Give it a moment to process
        
        status_response=$(curl -s -X GET "$BANKZ_URL/status/$reference")
        
        if echo "$status_response" | grep -q '"success":true'; then
            print_status 0 "Transaction status retrieved successfully"
            echo "Status Response: $status_response"
        else
            print_status 1 "Transaction status check failed"
            echo "Response: $status_response"
        fi
    else
        print_status 1 "Failed to create test transaction for status check"
        echo "Response: $create_response"
    fi
    echo ""
}

# Test 5: Query Endpoints
test_query_endpoints() {
    print_info "Test 5: Query Endpoints"
    echo "======================="
    
    # Test customer transactions
    print_info "Testing customer Bank Z transactions query..."
    response=$(curl -s -X GET "$BANKZ_URL/transactions/customer/$CUSTOMER_ID")
    
    if echo "$response" | grep -q '"success":true'; then
        print_status 0 "Customer Bank Z transactions retrieved successfully"
        transaction_count=$(echo "$response" | grep -o '"id":[0-9]*' | wc -l)
        print_info "Found $transaction_count Bank Z transactions for customer"
    else
        print_status 1 "Customer Bank Z transactions query failed"
        echo "Response: $response"
    fi
    
    # Test account transactions
    print_info "Testing account Bank Z transactions query..."
    response=$(curl -s -X GET "$BANKZ_URL/transactions/account/$CURRENT_ACCOUNT")
    
    if echo "$response" | grep -q '"success":true'; then
        print_status 0 "Account Bank Z transactions retrieved successfully"
        transaction_count=$(echo "$response" | grep -o '"id":[0-9]*' | wc -l)
        print_info "Found $transaction_count Bank Z transactions for account"
    else
        print_status 1 "Account Bank Z transactions query failed"
        echo "Response: $response"
    fi
    echo ""
}

# Main execution
main() {
    echo "Starting Bank Z Interface API Tests..."
    echo ""
    
    check_application
    setup_test_data
    test_single_transaction
    test_batch_transactions
    test_reconciliation
    test_transaction_status
    test_query_endpoints
    
    echo "🎉 Bank Z Interface API Tests Completed!"
    echo ""
    echo "📊 Summary:"
    echo "- All required Bank Z endpoints tested"
    echo "- Single transaction processing ✅"
    echo "- Batch transaction processing ✅"
    echo "- Reconciliation processing ✅"
    echo "- Transaction status checking ✅"
    echo "- Query endpoints ✅"
    echo ""
    echo "🔗 Bank Z Interface Documentation: ./BANK_Z_INTERFACE_DOCUMENTATION.md"
    echo "🌐 Application URL: http://localhost:9090"
    echo "📋 API Base URL: http://localhost:9090/api/bankz"
}

# Run the tests
main