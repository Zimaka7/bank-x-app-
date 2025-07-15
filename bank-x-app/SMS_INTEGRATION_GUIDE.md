# SMS Notification Integration Guide

## Overview

This document describes the SMS notification functionality integrated into the Bank X application using Twilio.

## Features

- **Real-time SMS notifications** for all banking transactions
- **Welcome SMS** for new customer onboarding
- **OTP SMS** for authentication
- **Retry mechanism** for failed SMS deliveries
- **Configurable SMS settings** via application properties
- **Formatted messages** for different notification types

## Configuration

### 1. Twilio Credentials

Add the following to your `application.properties`:

```properties
# Twilio SMS Configuration
twilio.account.sid=AC29c35e2eb903c1bf8e73632688707c4a
twilio.auth.token=07b30f8929d9ead6f7d769c0deffc43e
twilio.phone.number=+1234567890

# SMS Notification Configuration
bankx.sms.enabled=true
```

### 2. Dependencies

The following dependency has been added to `pom.xml`:

```xml
<dependency>
    <groupId>com.twilio.sdk</groupId>
    <artifactId>twilio</artifactId>
    <version>9.14.1</version>
</dependency>
```

## Components

### 1. TwilioConfig.java

Configuration class that initializes Twilio with your credentials.

### 2. SMSNotificationService.java

Core service for sending SMS messages with the following features:
- Basic SMS sending
- Retry mechanism with exponential backoff
- Phone number formatting (supports South African numbers)
- Pre-formatted message templates

### 3. Updated NotificationService.java

Enhanced notification service that:
- Integrates SMS delivery with existing notification system
- Provides specialized methods for different notification types
- Maintains notification history in database

### 4. SMSTestController.java

Test endpoints for validating SMS functionality:
- `/api/sms/test` - Send basic SMS
- `/api/sms/transaction-test` - Send transaction notification
- `/api/sms/welcome-test` - Send welcome message
- `/api/sms/otp-test` - Send OTP message

## Usage Examples

### 1. Send Transaction Notification

```java
notificationService.sendTransactionNotification(
    customerId, 
    "credit", 
    100.0, 
    "Savings", 
    600.0
);
```

### 2. Send Welcome Message

```java
notificationService.sendWelcomeNotification(customerId, 500.0);
```

### 3. Send OTP

```java
notificationService.sendOTPNotification(customerId, "123456");
```

### 4. Direct SMS

```java
smsNotificationService.sendSMS("+27821234567", "Your custom message");
```

## Testing

### Using cURL

1. **Test Basic SMS:**
```bash
curl -X POST http://localhost:9090/api/sms/test \
  -H "Content-Type: application/json" \
  -d '{"phoneNumber": "+27821234567", "message": "Test SMS from Bank X"}'
```

2. **Test Transaction SMS:**
```bash
curl -X POST http://localhost:9090/api/sms/transaction-test \
  -H "Content-Type: application/json" \
  -d '{
    "customerId": 1,
    "transactionType": "credit",
    "amount": 100.0,
    "accountType": "Savings",
    "newBalance": 600.0
  }'
```

3. **Test Welcome SMS:**
```bash
curl -X POST http://localhost:9090/api/sms/welcome-test \
  -H "Content-Type: application/json" \
  -d '{"customerId": 1, "bonusAmount": 500.0}'
```

4. **Test OTP SMS:**
```bash
curl -X POST http://localhost:9090/api/sms/otp-test \
  -H "Content-Type: application/json" \
  -d '{"customerId": 1, "otp": "123456"}'
```

## Message Templates

### Transaction Message
```
Hi [CustomerName], your [AccountType] account has been [TransactionType] with R[Amount]. New balance: R[Balance]. Bank X
```

### Welcome Message
```
Welcome to Bank X, [CustomerName]! Your accounts have been created and you've received a R[BonusAmount] welcome bonus. Thank you for choosing Bank X!
```

### OTP Message
```
Hi [CustomerName], your Bank X OTP is: [OTP]. This code expires in 5 minutes. Do not share this code with anyone.
```

## Phone Number Format

The system automatically handles phone number formatting:
- Accepts numbers with or without country code
- Automatically adds +27 for South African numbers if no country code is provided
- Removes leading zeros when adding country code

Examples:
- `0821234567` → `+27821234567`
- `+27821234567` → `+27821234567` (no change)
- `821234567` → `+27821234567`

## Error Handling

- **Invalid phone numbers**: Logged as warnings, SMS not sent
- **Twilio API errors**: Logged with full error details
- **Network failures**: Automatic retry with exponential backoff
- **Configuration errors**: Application startup failure with clear error message

## Monitoring

All SMS activities are logged with the following information:
- Recipient phone number
- Message content
- Success/failure status
- Twilio message SID (for successful sends)
- Error details (for failures)

## Security Considerations

1. **Credentials**: Store Twilio credentials securely (environment variables in production)
2. **Rate Limiting**: Consider implementing rate limiting for SMS endpoints
3. **Phone Number Validation**: Validate phone numbers before sending
4. **Message Content**: Sanitize message content to prevent injection attacks
5. **Audit Trail**: All SMS notifications are logged and stored in database

## Troubleshooting

### Common Issues

1. **SMS not sending**:
   - Check Twilio credentials
   - Verify phone number format
   - Check Twilio account balance
   - Review application logs

2. **Invalid phone number errors**:
   - Ensure phone numbers include country code
   - Check for special characters in phone numbers

3. **Configuration errors**:
   - Verify all required properties are set
   - Check property names for typos

### Logs to Check

- Application startup logs for Twilio initialization
- SMS delivery logs in `NotificationService`
- Error logs in `SMSNotificationService`

## Future Enhancements

1. **Message Templates**: Database-driven message templates
2. **Delivery Reports**: Track SMS delivery status
3. **Bulk SMS**: Support for sending bulk notifications
4. **SMS Scheduling**: Schedule SMS for future delivery
5. **Multi-language Support**: Support for multiple languages
6. **Cost Tracking**: Track SMS costs and usage
7. **Fallback Mechanisms**: Email fallback when SMS fails