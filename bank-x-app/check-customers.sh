#!/bin/bash

echo "=== Customer Database Check ==="
echo ""

# Check database directly
echo "1. Database Records:"
PGPASSWORD=bankx_password psql -h localhost -p 5433 -U bankx_user -d bankx_db -c "SELECT id, first_name, last_name, email, created_at FROM customers ORDER BY id;"

echo ""
echo "2. API Response:"
curl -s http://localhost:9090/api/customers | jq '.data[] | {id, firstName, lastName, email, createdAt}'

echo ""
echo "3. Customer Count:"
echo "Database: $(PGPASSWORD=bankx_password psql -h localhost -p 5433 -U bankx_user -d bankx_db -t -c "SELECT COUNT(*) FROM customers;" | tr -d ' ')"
echo "API: $(curl -s http://localhost:9090/api/customers | jq '.data | length')"

echo ""
echo "=== Check Complete ==="