-- Create Bank Z schema and users
CREATE SCHEMA IF NOT EXISTS bankz;
SET search_path TO bankz;

-- Create users table
CREATE TABLE IF NOT EXISTS bankz_users (
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(255) UNIQUE NOT NULL,
    password VARCHAR(255) NOT NULL,
    email VARCHAR(255) UNIQUE NOT NULL,
    first_name VARCHAR(255),
    last_name VARCHAR(255),
    role VARCHAR(50),
    enabled BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Insert default users with BCrypt encoded passwords
-- admin123 = $2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lFpoNlxREU.GyTm4K
-- operator123 = $2a$10$8K1p/wqDd2KSCxlHkqP2O.L1.YtZ5S6HZSx8CYo.31OGdh8zu2/Uy
-- auditor123 = $2a$10$DvzIFauvW5c6lL2.rOXOhOehsjjVR5hJe5XvZOcX5/SI1.Q.2/Uy

INSERT INTO bankz_users (username, password, email, first_name, last_name, role, enabled) VALUES
('admin', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lFpoNlxREU.GyTm4K', 'admin@bankz.com', 'System', 'Administrator', 'ADMIN', true),
('operator', '$2a$10$8K1p/wqDd2KSCxlHkqP2O.L1.YtZ5S6HZSx8CYo.31OGdh8zu2/Uy', 'operator@bankz.com', 'Bank', 'Operator', 'OPERATOR', true),
('auditor', '$2a$10$DvzIFauvW5c6lL2.rOXOhOehsjjVR5hJe5XvZOcX5/SI1.Q.2/Uy', 'auditor@bankz.com', 'System', 'Auditor', 'AUDITOR', true)
ON CONFLICT (username) DO NOTHING;

-- Create customers table
CREATE TABLE IF NOT EXISTS bankz_customers (
    id BIGSERIAL PRIMARY KEY,
    first_name VARCHAR(255) NOT NULL,
    last_name VARCHAR(255) NOT NULL,
    email VARCHAR(255) UNIQUE NOT NULL,
    phone_number VARCHAR(50),
    id_number VARCHAR(50) UNIQUE,
    address TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Create accounts table
CREATE TABLE IF NOT EXISTS bankz_accounts (
    id BIGSERIAL PRIMARY KEY,
    customer_id BIGINT REFERENCES bankz_customers(id),
    account_number VARCHAR(255) UNIQUE NOT NULL,
    account_type VARCHAR(50),
    balance DECIMAL(19,2) DEFAULT 0.00,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Create transactions table
CREATE TABLE IF NOT EXISTS bankz_transactions (
    id BIGSERIAL PRIMARY KEY,
    transaction_id VARCHAR(255) UNIQUE,
    account_id BIGINT REFERENCES bankz_accounts(id),
    transaction_type VARCHAR(50),
    amount DECIMAL(19,2),
    fee DECIMAL(19,2) DEFAULT 0.00,
    description TEXT,
    reference VARCHAR(255),
    balance_before DECIMAL(19,2),
    balance_after DECIMAL(19,2),
    status VARCHAR(50) DEFAULT 'COMPLETED',
    sent_to_bankx BOOLEAN DEFAULT false,
    bankx_response TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Insert sample customers
INSERT INTO bankz_customers (first_name, last_name, email, phone_number, id_number, address) VALUES
('John', 'Doe', 'john.doe@example.com', '+27123456789', '8001015009087', '123 Main St, Cape Town'),
('Jane', 'Smith', 'jane.smith@example.com', '+27123456790', '8505125009088', '456 Oak Ave, Johannesburg'),
('Bob', 'Johnson', 'bob.johnson@example.com', '+27123456791', '7012255009089', '789 Pine Rd, Durban')
ON CONFLICT (email) DO NOTHING;

-- Insert sample accounts
INSERT INTO bankz_accounts (customer_id, account_number, account_type, balance) VALUES
(1, 'BZ-ACC-001', 'CURRENT', 1000.00),
(1, 'BZ-ACC-002', 'SAVINGS', 5000.00),
(2, 'BZ-ACC-003', 'CURRENT', 2500.00),
(2, 'BZ-ACC-004', 'SAVINGS', 7500.00),
(3, 'BZ-ACC-005', 'CURRENT', 1500.00),
(3, 'BZ-ACC-006', 'SAVINGS', 3000.00)
ON CONFLICT (account_number) DO NOTHING;