-- Bank X Application Database Setup Script
-- PostgreSQL Database Setup

-- Create database (run as postgres superuser)
CREATE DATABASE bankx_db;

-- Create user (run as postgres superuser)
CREATE USER bankx_user WITH PASSWORD 'bankx_password';

-- Grant privileges
GRANT ALL PRIVILEGES ON DATABASE bankx_db TO bankx_user;

-- Connect to bankx_db and grant schema privileges
\c bankx_db;
GRANT ALL ON SCHEMA public TO bankx_user;
GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA public TO bankx_user;
GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA public TO bankx_user;

-- The application will automatically create tables using Hibernate DDL
-- But here are the table structures for reference:

/*
-- Customers table
CREATE TABLE customers (
    id BIGSERIAL PRIMARY KEY,
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    email VARCHAR(255) UNIQUE NOT NULL,
    phone_number VARCHAR(20) NOT NULL,
    sa_id_number VARCHAR(13) UNIQUE NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Accounts table
CREATE TABLE accounts (
    id BIGSERIAL PRIMARY KEY,
    customer_id BIGINT NOT NULL REFERENCES customers(id),
    account_number VARCHAR(20) UNIQUE NOT NULL,
    account_type VARCHAR(20) NOT NULL CHECK (account_type IN ('CURRENT', 'SAVINGS')),
    balance DECIMAL(15,2) DEFAULT 0.00,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    version BIGINT DEFAULT 0
);

-- Transactions table
CREATE TABLE transactions (
    id BIGSERIAL PRIMARY KEY,
    account_id BIGINT NOT NULL REFERENCES accounts(id),
    transaction_id VARCHAR(50) UNIQUE NOT NULL,
    amount DECIMAL(15,2) NOT NULL,
    transaction_type VARCHAR(20) NOT NULL CHECK (transaction_type IN ('CREDIT', 'DEBIT', 'TRANSFER')),
    description TEXT,
    reference_number VARCHAR(100),
    status VARCHAR(20) DEFAULT 'COMPLETED',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    balance_after DECIMAL(15,2)
);

-- Notifications table
CREATE TABLE notifications (
    id BIGSERIAL PRIMARY KEY,
    customer_id BIGINT NOT NULL REFERENCES customers(id),
    message TEXT NOT NULL,
    type VARCHAR(50) NOT NULL,
    sent_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    status VARCHAR(20) DEFAULT 'SENT'
);

-- Bank Z Transactions table
CREATE TABLE bankz_transactions (
    id BIGSERIAL PRIMARY KEY,
    customer_id BIGINT NOT NULL REFERENCES customers(id),
    account_number VARCHAR(20) NOT NULL,
    amount DECIMAL(15,2) NOT NULL,
    transaction_type VARCHAR(20) NOT NULL,
    reference VARCHAR(100) NOT NULL,
    processed_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    batch_id VARCHAR(50)
);

-- Indexes for better performance
CREATE INDEX idx_customers_email ON customers(email);
CREATE INDEX idx_customers_sa_id_number ON customers(sa_id_number);
CREATE INDEX idx_accounts_customer_id ON accounts(customer_id);
CREATE INDEX idx_accounts_account_number ON accounts(account_number);
CREATE INDEX idx_transactions_account_id ON transactions(account_id);
CREATE INDEX idx_transactions_transaction_id ON transactions(transaction_id);
CREATE INDEX idx_transactions_created_at ON transactions(created_at);
CREATE INDEX idx_notifications_customer_id ON notifications(customer_id);
CREATE INDEX idx_bankz_transactions_customer_id ON bankz_transactions(customer_id);
CREATE INDEX idx_bankz_transactions_account_number ON bankz_transactions(account_number);
CREATE INDEX idx_bankz_transactions_batch_id ON bankz_transactions(batch_id);
*/