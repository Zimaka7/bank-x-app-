# Bank Z Interface Documentation

## Overview

The Bank Z Interface provides a comprehensive REST API layer that enables Bank Z to integrate with our banking system for transaction processing, batch operations, and reconciliation.

## 🔸 Interface Type: REST API

Bank Z uses this interface to:
- Send single/batch transactions
- Send reconciliation files
- Receive success/failure responses
- Check transaction status

## 🔸 API Endpoints

### Base URL
```
http://localhost:9090/api/bankz
```

### 1. Single Transaction Processing
**Endpoint:** `POST /api/bankz/transaction`

**Description:** Accept a single transaction to credit/debit a user account

**Request Body:**
```json
{
    "accountNumber": "ACC001234567890",
    "amount": 100.50,
    "transactionType": "CREDIT",
    "reference": "BZ-TXN-001"
}
```

**Response:**
```json
{
    "success": true,
    "message": "Bank Z transaction processed successfully",
    "data": null,
    "timestamp": "2024-01-15T10:30:00"
}
```

### 2. Batch Transaction Processing
**Endpoint:** `POST /api/bankz/transactions/batch`

**Description:** Accept a list of transactions for immediate processing

**Request Body:**
```json
[
    {
        "accountNumber": "ACC001234567890",
        "amount": 100.50,
        "transactionType": "CREDIT",
        "reference": "BZ-BATCH-001"
    },
    {
        "accountNumber": "ACC001234567891",
        "amount": 50.25,
        "transactionType": "DEBIT",
        "reference": "BZ-BATCH-002"
    }
]
```

**Response:**
```json
{
    "success": true,
    "message": "Bank Z batch transactions processed successfully (2 transactions)",
    "data": null,
    "timestamp": "2024-01-15T10:30:00"
}
```

### 3. Reconciliation Processing
**Endpoint:** `POST /api/bankz/reconciliation`

**Description:** Accept a batch of end-of-day transactions for reconciliation

**Request Body:**
```json
[
    {
        "accountNumber": "ACC001234567890",
        "amount": 1000.00,
        "transactionType": "CREDIT",
        "reference": "BZ-RECON-001"
    },
    {
        "accountNumber": "ACC001234567891",
        "amount": 500.00,
        "transactionType": "DEBIT",
        "reference": "BZ-RECON-002"
    }
]
```

**Response:**
```json
{
    "success": true,
    "message": "Bank Z reconciliation transactions processed successfully (2 transactions)",
    "data": null,
    "timestamp": "2024-01-15T10:30:00"
}
```

### 4. Transaction Status Check (Optional)
**Endpoint:** `GET /api/bankz/status/{reference}`

**Description:** Check status of a transaction by reference

**Example:** `GET /api/bankz/status/BZ-TXN-001`

**Response:**
```json
{
    "success": true,
    "message": "Bank Z transaction status retrieved successfully",
    "data": [
        {
            "id": 1,
            "accountNumber": "ACC001234567890",
            "amount": 100.50,
            "transactionType": "CREDIT",
            "reference": "BZ-TXN-001",
            "processedAt": "2024-01-15T10:30:00",
            "batchId": null
        }
    ],
    "timestamp": "2024-01-15T10:35:00"
}
```

## 🔸 Additional Query Endpoints

### Get Customer Bank Z Transactions
**Endpoint:** `GET /api/bankz/transactions/customer/{customerId}`

### Get Account Bank Z Transactions
**Endpoint:** `GET /api/bankz/transactions/account/{accountNumber}`

### Get Batch Transactions
**Endpoint:** `GET /api/bankz/transactions/batch/{batchId}`

## 🔸 Data Models

### BankZTransactionRequest
```json
{
    "accountNumber": "string (required, max 20 chars)",
    "amount": "decimal (required, min 0.01)",
    "transactionType": "enum (CREDIT|DEBIT)",
    "reference": "string (required, max 100 chars, unique)"
}
```

### BankZTransaction Response
```json
{
    "id": "long",
    "accountNumber": "string",
    "amount": "decimal",
    "transactionType": "enum",
    "reference": "string",
    "processedAt": "datetime",
    "batchId": "string (nullable)"
}
```

## 🛡️ Security Considerations

### Authentication & Authorization
- **API Keys**: Implement API key authentication for Bank Z endpoints
- **OAuth2**: Consider OAuth2 for enhanced security
- **IP Whitelisting**: Restrict access to known Bank Z IP addresses

### Input Validation
- All requests undergo comprehensive validation
- Duplicate transaction prevention using unique reference IDs
- Amount validation (positive values only)
- Account number format validation

### Logging & Monitoring
- All Bank Z transactions are logged with full audit trail
- Failed transactions are logged with error details
- Performance monitoring for batch operations
- Real-time alerting for failed transactions

### Error Handling
- Structured error responses with appropriate HTTP status codes
- Detailed error messages for debugging
- Graceful handling of partial batch failures

## 🔸 Transaction Processing Flow

### Single Transaction Flow
1. Bank Z sends POST request to `/api/bankz/transaction`
2. System validates request data
3. Account existence is verified
4. BankZTransaction record is created
5. Account balance is updated (credit/debit)
6. Customer notification is sent
7. Success response is returned to Bank Z

### Batch Transaction Flow
1. Bank Z sends POST request to `/api/bankz/transactions/batch`
2. System generates unique batch ID
3. Each transaction is processed individually
4. Failed transactions are logged but don't stop batch processing
5. Batch completion status is returned

### Reconciliation Flow
1. Bank Z sends end-of-day transactions to `/api/bankz/reconciliation`
2. System generates reconciliation batch ID with "RECON-" prefix
3. Transactions are recorded for reconciliation purposes
4. No immediate account updates (reconciliation is offline process)
5. Reconciliation completion status is returned

## 🔸 Error Codes

| HTTP Status | Error Code | Description |
|-------------|------------|-------------|
| 200 | SUCCESS | Transaction processed successfully |
| 400 | BAD_REQUEST | Invalid request data or validation failure |
| 404 | NOT_FOUND | Account not found |
| 409 | CONFLICT | Duplicate transaction reference |
| 500 | INTERNAL_ERROR | System error during processing |

## 🔸 Rate Limiting & Performance

### Recommended Limits
- Single transactions: 1000 requests/minute
- Batch transactions: 100 batches/minute (max 1000 transactions per batch)
- Status queries: 5000 requests/minute

### Performance Considerations
- Batch processing is optimized for high throughput
- Database transactions ensure data consistency
- Asynchronous notification processing
- Connection pooling for database operations

## 🔸 Testing

Use the provided test script to validate Bank Z interface functionality:
```bash
./bank-z-api-test.sh
```

This script tests all endpoints with sample data and validates responses.

## 🔸 Monitoring & Health Checks

### Health Check Endpoint
```
GET /actuator/health
```

### Metrics Endpoint
```
GET /actuator/metrics
```

### Bank Z Specific Metrics
- Total transactions processed
- Batch processing times
- Failed transaction rates
- Average response times

## 🔸 Support & Troubleshooting

### Common Issues
1. **Account Not Found**: Verify account number format and existence
2. **Duplicate Reference**: Ensure unique reference IDs for each transaction
3. **Invalid Amount**: Check amount is positive and properly formatted
4. **Batch Timeout**: Large batches may require increased timeout settings

### Log Locations
- Application logs: `/logs/bank-x-app.log`
- Bank Z transaction logs: Search for "Bank Z" in application logs
- Error logs: Check ERROR level logs for failed transactions

### Contact Information
For technical support regarding Bank Z integration:
- Email: support@bankx.com
- Phone: +27-11-XXX-XXXX
- Emergency: +27-82-XXX-XXXX (24/7)