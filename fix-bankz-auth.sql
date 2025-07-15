-- Fix Bank Z Authentication Issues
-- Update enabled status for all Bank Z users
UPDATE bankz.bankz_users SET enabled = true WHERE enabled IS NULL OR enabled = false;

-- Verify the users exist and are enabled
SELECT username, role, enabled, created_at FROM bankz.bankz_users;

-- If users don't exist, create them with proper BCrypt passwords
-- Note: These are BCrypt hashed versions of the passwords
INSERT INTO bankz.bankz_users (username, email, password, first_name, last_name, role, enabled, created_at)
VALUES 
    ('admin', 'admin@bankz.com', '$2a$10$8.UnVuG9HHgffUDAlk8qfOuVGkqRdvX1HP3K6tAjqeMXZx4GhixIy', 'System', 'Administrator', 'ADMIN', true, NOW()),
    ('operator', 'operator@bankz.com', '$2a$10$8.UnVuG9HHgffUDAlk8qfOuVGkqRdvX1HP3K6tAjqeMXZx4GhixIy', 'Bank', 'Operator', 'OPERATOR', true, NOW()),
    ('auditor', 'auditor@bankz.com', '$2a$10$8.UnVuG9HHgffUDAlk8qfOuVGkqRdvX1HP3K6tAjqeMXZx4GhixIy', 'System', 'Auditor', 'AUDITOR', true, NOW())
ON CONFLICT (username) DO UPDATE SET 
    enabled = true,
    password = EXCLUDED.password;