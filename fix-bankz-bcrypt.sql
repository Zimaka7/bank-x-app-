-- Fix Bank Z BCrypt password hashes
-- Delete and recreate users with proper BCrypt hashes

DELETE FROM bankz.bankz_users;

-- Insert admin user with proper BCrypt hash for 'admin123'
INSERT INTO bankz.bankz_users (username, email, password, first_name, last_name, role, enabled, created_at)
VALUES ('admin', 'admin@bankz.com', '$2a$10$N.zmdr9k7uOCQb0bta/OOe5vbdKZBrx5fHdXpwo8pNkSfp4f7VW/K', 'System', 'Administrator', 'ADMIN', true, NOW());

-- Insert operator user with proper BCrypt hash for 'operator123'  
INSERT INTO bankz.bankz_users (username, email, password, first_name, last_name, role, enabled, created_at)
VALUES ('operator', 'operator@bankz.com', '$2a$10$VEjxf0jfc2WnNzKWHBTtVebtFtkdz2IdAotTstVWoalQ1lhHgdWW2', 'Bank', 'Operator', 'OPERATOR', true, NOW());

-- Insert auditor user with proper BCrypt hash for 'auditor123'
INSERT INTO bankz.bankz_users (username, email, password, first_name, last_name, role, enabled, created_at)  
VALUES ('auditor', 'auditor@bankz.com', '$2a$10$ZenpA7.WCVX2GceAomUOaOkjrjzgEWLBaYBzubeisf4zklNYz1QAa', 'System', 'Auditor', 'AUDITOR', true, NOW());

-- Verify the users
SELECT username, role, enabled, substring(password, 1, 10) as password_prefix FROM bankz.bankz_users;