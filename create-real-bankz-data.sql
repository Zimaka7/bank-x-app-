-- Create Real Bank Z Data with Bank X Integration
-- This script creates realistic customers, accounts, and transactions

-- First, let's see what Bank X customers exist to create corresponding Bank Z customers
-- We'll create Bank Z customers that can interact with Bank X

-- Clear existing demo data
DELETE FROM bankz.bankz_transactions;
DELETE FROM bankz.bankz_accounts;
DELETE FROM bankz.bankz_customers;

-- Create real Bank Z customers based on Bank X customers
INSERT INTO bankz.bankz_customers (first_name, last_name, email, phone_number, id_number, address, created_at)
VALUES 
    ('Michael', 'Johnson', 'michael.johnson@email.com', '+27123456789', '8001015009087', '123 Cape Town Street, Cape Town', NOW()),
    ('Sarah', 'Williams', 'sarah.williams@email.com', '+27123456790', '8505125009088', '456 Johannesburg Ave, Johannesburg', NOW()),
    ('David', 'Brown', 'david.brown@email.com', '+27123456791', '7012255009089', '789 Durban Road, Durban', NOW()),
    ('Lisa', 'Davis', 'lisa.davis@email.com', '+27123456792', '9208305009090', '321 Pretoria Lane, Pretoria', NOW()),
    ('James', 'Wilson', 'james.wilson@email.com', '+27123456793', '8107145009091', '654 Port Elizabeth St, Port Elizabeth', NOW());

-- Create accounts for each customer (Current and Savings)
INSERT INTO bankz.bankz_accounts (account_number, account_type, balance, customer_id, is_active, created_at)
SELECT 
    'BZ-' || LPAD(ROW_NUMBER() OVER (ORDER BY c.id, account_type), 6, '0'),
    account_type,
    CASE 
        WHEN account_type = 'CURRENT' THEN 5000.00 + (RANDOM() * 15000)::DECIMAL(10,2)
        WHEN account_type = 'SAVINGS' THEN 10000.00 + (RANDOM() * 50000)::DECIMAL(10,2)
    END,
    c.id,
    true,
    NOW()
FROM bankz.bankz_customers c
CROSS JOIN (VALUES ('CURRENT'), ('SAVINGS')) AS account_types(account_type);

-- Create realistic transactions for the past 30 days
INSERT INTO bankz.bankz_transactions (
    transaction_id, transaction_type, amount, description, reference, 
    account_id, balance_before, balance_after, status, sent_to_bankx, 
    bankx_response, created_at
)
SELECT 
    'BZ-TXN-' || LPAD(ROW_NUMBER() OVER (ORDER BY RANDOM()), 12, '0'),
    CASE 
        WHEN RANDOM() < 0.6 THEN 'CREDIT'
        ELSE 'DEBIT'
    END,
    (50 + RANDOM() * 2000)::DECIMAL(10,2),
    CASE 
        WHEN RANDOM() < 0.3 THEN 'Salary Payment'
        WHEN RANDOM() < 0.5 THEN 'Online Purchase'
        WHEN RANDOM() < 0.7 THEN 'ATM Withdrawal'
        WHEN RANDOM() < 0.8 THEN 'Transfer from Bank X'
        WHEN RANDOM() < 0.9 THEN 'Interest Payment'
        ELSE 'Service Fee'
    END,
    'REF-' || LPAD((RANDOM() * 999999)::INT::TEXT, 6, '0'),
    a.id,
    a.balance,
    CASE 
        WHEN RANDOM() < 0.6 THEN a.balance + (50 + RANDOM() * 2000)::DECIMAL(10,2)
        ELSE a.balance - (50 + RANDOM() * 500)::DECIMAL(10,2)
    END,
    CASE 
        WHEN RANDOM() < 0.95 THEN 'COMPLETED'
        WHEN RANDOM() < 0.98 THEN 'PENDING'
        ELSE 'FAILED'
    END,
    RANDOM() < 0.7, -- 70% sent to Bank X
    CASE 
        WHEN RANDOM() < 0.9 THEN 'SUCCESS'
        ELSE 'FAILED'
    END,
    NOW() - (RANDOM() * INTERVAL '30 days')
FROM bankz.bankz_accounts a
CROSS JOIN generate_series(1, 5) -- 5 transactions per account

UNION ALL

-- Add some recent transactions for today
SELECT 
    'BZ-TXN-TODAY-' || LPAD(ROW_NUMBER() OVER (ORDER BY RANDOM()), 8, '0'),
    CASE 
        WHEN RANDOM() < 0.5 THEN 'CREDIT'
        ELSE 'DEBIT'
    END,
    (100 + RANDOM() * 1000)::DECIMAL(10,2),
    CASE 
        WHEN RANDOM() < 0.4 THEN 'Bank X Transfer'
        WHEN RANDOM() < 0.6 THEN 'Online Payment'
        WHEN RANDOM() < 0.8 THEN 'Mobile Banking'
        ELSE 'Branch Deposit'
    END,
    'TODAY-REF-' || LPAD((RANDOM() * 999999)::INT::TEXT, 6, '0'),
    a.id,
    a.balance,
    CASE 
        WHEN RANDOM() < 0.5 THEN a.balance + (100 + RANDOM() * 1000)::DECIMAL(10,2)
        ELSE a.balance - (100 + RANDOM() * 300)::DECIMAL(10,2)
    END,
    'COMPLETED',
    true, -- All today's transactions sent to Bank X
    'SUCCESS',
    NOW() - (RANDOM() * INTERVAL '12 hours')
FROM bankz.bankz_accounts a
CROSS JOIN generate_series(1, 2); -- 2 transactions per account for today

-- Update account balances based on transactions
UPDATE bankz.bankz_accounts 
SET balance = (
    SELECT COALESCE(SUM(
        CASE 
            WHEN t.transaction_type = 'CREDIT' THEN t.amount
            ELSE -t.amount
        END
    ), 0) + 10000 -- Starting balance
    FROM bankz.bankz_transactions t 
    WHERE t.account_id = bankz.bankz_accounts.id
    AND t.status = 'COMPLETED'
);

-- Ensure no negative balances
UPDATE bankz.bankz_accounts 
SET balance = 100.00 
WHERE balance < 0;

-- Create some Bank X integration logs
INSERT INTO bankz.bankz_transaction_logs (
    transaction_id, bankx_customer_id, bankx_account_type, request_payload,
    response_payload, status, created_at
)
SELECT 
    t.transaction_id,
    'BANKX-CUST-' || LPAD((RANDOM() * 9999)::INT::TEXT, 4, '0'),
    CASE WHEN RANDOM() < 0.5 THEN 'CURRENT' ELSE 'SAVINGS' END,
    '{"amount": ' || t.amount || ', "type": "' || t.transaction_type || '", "description": "' || t.description || '"}',
    '{"status": "' || t.bankx_response || '", "bankx_transaction_id": "BX-TXN-' || LPAD((RANDOM() * 999999)::INT::TEXT, 6, '0') || '"}',
    CASE WHEN t.bankx_response = 'SUCCESS' THEN 'SUCCESS' ELSE 'FAILED' END,
    t.created_at
FROM bankz.bankz_transactions t
WHERE t.sent_to_bankx = true
LIMIT 50;

-- Show summary of created data
SELECT 
    'Customers' as entity_type, 
    COUNT(*) as count 
FROM bankz.bankz_customers
UNION ALL
SELECT 
    'Accounts' as entity_type, 
    COUNT(*) as count 
FROM bankz.bankz_accounts
UNION ALL
SELECT 
    'Transactions' as entity_type, 
    COUNT(*) as count 
FROM bankz.bankz_transactions
UNION ALL
SELECT 
    'Integration Logs' as entity_type, 
    COUNT(*) as count 
FROM bankz.bankz_transaction_logs;