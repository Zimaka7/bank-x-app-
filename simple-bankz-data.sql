-- Create real Bank Z customers
INSERT INTO bankz.bankz_customers (first_name, last_name, email, phone_number, id_number, address, created_at)
VALUES 
    ('Michael', 'Johnson', 'michael.johnson@email.com', '+27123456789', '8001015009087', '123 Cape Town Street, Cape Town', NOW()),
    ('Sarah', 'Williams', 'sarah.williams@email.com', '+27123456790', '8505125009088', '456 Johannesburg Ave, Johannesburg', NOW()),
    ('David', 'Brown', 'david.brown@email.com', '+27123456791', '7012255009089', '789 Durban Road, Durban', NOW());

-- Create accounts for customers
INSERT INTO bankz.bankz_accounts (account_number, account_type, balance, customer_id, is_active, created_at)
VALUES 
    ('BZ-000001', 'CURRENT', 15000.00, 1, true, NOW()),
    ('BZ-000002', 'SAVINGS', 25000.00, 1, true, NOW()),
    ('BZ-000003', 'CURRENT', 8500.00, 2, true, NOW()),
    ('BZ-000004', 'SAVINGS', 35000.00, 2, true, NOW()),
    ('BZ-000005', 'CURRENT', 12000.00, 3, true, NOW()),
    ('BZ-000006', 'SAVINGS', 18000.00, 3, true, NOW());

-- Create some transactions
INSERT INTO bankz.bankz_transactions (
    transaction_id, transaction_type, amount, description, reference, 
    account_id, balance_before, balance_after, status, sent_to_bankx, 
    bankx_response, created_at
)
VALUES 
    ('BZ-TXN-001', 'CREDIT', 1000.00, 'Salary Payment', 'SAL-001', 1, 14000.00, 15000.00, 'COMPLETED', true, 'SUCCESS', NOW() - INTERVAL '1 hour'),
    ('BZ-TXN-002', 'DEBIT', 500.00, 'ATM Withdrawal', 'ATM-001', 1, 15000.00, 14500.00, 'COMPLETED', true, 'SUCCESS', NOW() - INTERVAL '30 minutes'),
    ('BZ-TXN-003', 'CREDIT', 2000.00, 'Transfer from Bank X', 'BX-001', 3, 6500.00, 8500.00, 'COMPLETED', false, NULL, NOW() - INTERVAL '2 hours'),
    ('BZ-TXN-004', 'CREDIT', 500.00, 'Interest Payment', 'INT-001', 2, 24500.00, 25000.00, 'COMPLETED', true, 'SUCCESS', NOW() - INTERVAL '1 day'),
    ('BZ-TXN-005', 'DEBIT', 200.00, 'Online Purchase', 'PUR-001', 5, 12200.00, 12000.00, 'COMPLETED', true, 'SUCCESS', NOW() - INTERVAL '3 hours');

SELECT 'Data created successfully' as result;