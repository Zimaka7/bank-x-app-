-- Create accounts with correct customer IDs
INSERT INTO bankz.bankz_accounts (account_number, account_type, balance, customer_id, is_active, created_at)
VALUES 
    ('BZ-000001', 'CURRENT', 15000.00, 16, true, NOW()),
    ('BZ-000002', 'SAVINGS', 25000.00, 16, true, NOW()),
    ('BZ-000003', 'CURRENT', 8500.00, 17, true, NOW()),
    ('BZ-000004', 'SAVINGS', 35000.00, 17, true, NOW()),
    ('BZ-000005', 'CURRENT', 12000.00, 18, true, NOW()),
    ('BZ-000006', 'SAVINGS', 18000.00, 18, true, NOW());

-- Get the account IDs that were created
SELECT id, account_number, customer_id FROM bankz.bankz_accounts ORDER BY id;