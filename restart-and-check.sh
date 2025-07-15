#!/bin/bash
echo "Restarting Bank X..."
cd /opt/absa
./bankx.sh restart

echo "Waiting for startup..."
sleep 10

echo "Checking logs for debug output..."
tail -20 /opt/absa/bank-x-app/bankx.log

echo "Bank X should now be running with debug output enabled."
echo "Access admin pages at:"
echo "- http://41.76.107.45:9090/admin/customers"
echo "- http://41.76.107.45:9090/admin/accounts" 
echo "- http://41.76.107.45:9090/admin/transactions"