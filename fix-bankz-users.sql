-- Delete existing Bank Z users
DELETE FROM bankz.bankz_users;

-- Insert users with properly BCrypt encoded passwords
-- Password 'admin123' encoded with BCrypt
INSERT INTO bankz.bankz_users (username, email, password, first_name, last_name, role, enabled, created_at)
VALUES 
    ('admin', 'admin@bankz.com', '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2uheWG/igi.', 'System', 'Administrator', 'ADMIN', true, NOW());

-- Password 'operator123' encoded with BCrypt  
INSERT INTO bankz.bankz_users (username, email, password, first_name, last_name, role, enabled, created_at)
VALUES 
    ('operator', 'operator@bankz.com', '$2a$10$VEjxf0jfc2WnNzKWHBTtVebtFtkdz2IdAotTstVWoalQ1lhHgdWW2', 'Bank', 'Operator', 'OPERATOR', true, NOW());

-- Password 'auditor123' encoded with BCrypt
INSERT INTO bankz.bankz_users (username, email, password, first_name, last_name, role, enabled, created_at)
VALUES 
    ('auditor', 'auditor@bankz.com', '$2a$10$ZenpA7.WCVX2GceAomUOaOkjrjzgEWLBaYBzubeisf4zklNYz1QAa', 'System', 'Auditor', 'AUDITOR', true, NOW());

-- Verify the users were created
SELECT username, role, enabled FROM bankz.bankz_users;