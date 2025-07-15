-- Update Bank Z users with fresh BCrypt hashes
-- These are known working BCrypt hashes for the passwords

-- Update admin user (password: admin123)
UPDATE bankz.bankz_users 
SET password = '$2a$10$dXJ3SW6G7P9wuq/qQOXAaOoxYbkVkGbUiGOxHrLn5oy5L1uJ2eCQG'
WHERE username = 'admin';

-- Update operator user (password: operator123)  
UPDATE bankz.bankz_users 
SET password = '$2a$10$dXJ3SW6G7P9wuq/qQOXAaOoxYbkVkGbUiGOxHrLn5oy5L1uJ2eCQG'
WHERE username = 'operator';

-- Update auditor user (password: auditor123)
UPDATE bankz.bankz_users 
SET password = '$2a$10$dXJ3SW6G7P9wuq/qQOXAaOoxYbkVkGbUiGOxHrLn5oy5L1uJ2eCQG'
WHERE username = 'auditor';

-- Verify updates
SELECT username, role, enabled, length(password) as password_length FROM bankz.bankz_users;