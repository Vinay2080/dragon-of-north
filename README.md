# Dragon of North - Multi-Factor Authentication & Verification System

A robust and scalable multi-factor authentication (MFA) and verification system designed for enterprise-grade security. Dragon of North provides comprehensive authentication mechanisms including OTP, biometric verification, email/SMS verification, and secure session management.

## 🚀 Features

### Core Authentication Methods
- **One-Time Password (OTP)** - Time-based (TOTP) and HMAC-based (HOTP) OTP generation and validation
- **Two-Factor Authentication (2FA)** - Multi-layered security with optional fallback methods
- **Email Verification** - Secure email-based identity verification with token expiration
- **SMS Verification** - Phone number verification with rate limiting and abuse prevention
- **Biometric Authentication** - Fingerprint and facial recognition support
- **Security Questions** - Customizable security questions for account recovery

### Advanced Features
- **Session Management** - Secure session creation, validation, and expiration
- **Rate Limiting** - Protection against brute force attacks
- **Device Fingerprinting** - Track and manage trusted devices
- **Audit Logging** - Comprehensive logging of all authentication events
- **Risk Assessment** - Intelligent risk detection and adaptive authentication
- **Account Recovery** - Secure backup codes and recovery mechanisms
- **Single Sign-On (SSO)** - OAuth 2.0 and SAML 2.0 support

## 📋 Requirements

- Node.js >= 14.0.0
- npm >= 6.0.0 or yarn >= 1.22.0
- PostgreSQL >= 12 or MongoDB >= 4.4
- Redis >= 5.0 (optional, for caching and rate limiting)

## 💻 Installation

### Clone the Repository
```bash
git clone https://github.com/Vinay2080/dragon-of-north.git
cd dragon-of-north
```

### Install Dependencies
```bash
npm install
# or
yarn install
```

### Environment Configuration
Create a `.env` file in the root directory:

```env
# Server Configuration
PORT=3000
NODE_ENV=development
API_URL=http://localhost:3000

# Database Configuration
DB_HOST=localhost
DB_PORT=5432
DB_NAME=dragon_of_north
DB_USER=postgres
DB_PASSWORD=your_password

# Authentication Configuration
JWT_SECRET=your_jwt_secret_key_change_this
JWT_EXPIRATION=24h
REFRESH_TOKEN_SECRET=your_refresh_token_secret
REFRESH_TOKEN_EXPIRATION=7d

# OTP Configuration
OTP_WINDOW=30
OTP_DIGITS=6
OTP_ALGORITHM=SHA1

# Email Configuration
SMTP_HOST=smtp.gmail.com
SMTP_PORT=587
SMTP_USER=your_email@gmail.com
SMTP_PASSWORD=your_app_password
SMTP_FROM=noreply@dragonnorth.com

# SMS Configuration
TWILIO_ACCOUNT_SID=your_account_sid
TWILIO_AUTH_TOKEN=your_auth_token
TWILIO_PHONE_NUMBER=+1234567890

# Redis Configuration (Optional)
REDIS_HOST=localhost
REDIS_PORT=6379
REDIS_PASSWORD=

# Security Configuration
RATE_LIMIT_WINDOW=15m
RATE_LIMIT_MAX_ATTEMPTS=5
SESSION_TIMEOUT=30m
TOKEN_EXPIRATION=15m

# Logging
LOG_LEVEL=info
LOG_FORMAT=json
```

### Setup Database
```bash
npm run db:migrate
npm run db:seed
```

### Start the Server
```bash
# Development
npm run dev

# Production
npm run build
npm run start
```

## 🔧 API Documentation

### Authentication Endpoints

#### Register User
```http
POST /api/auth/register
Content-Type: application/json

{
  "email": "user@example.com",
  "password": "SecurePassword123!",
  "firstName": "John",
  "lastName": "Doe"
}
```

**Response:**
```json
{
  "success": true,
  "message": "User registered successfully",
  "userId": "uuid",
  "requiresEmailVerification": true
}
```

#### Verify Email
```http
POST /api/auth/verify-email
Content-Type: application/json

{
  "email": "user@example.com",
  "token": "verification_token"
}
```

#### Login
```http
POST /api/auth/login
Content-Type: application/json

{
  "email": "user@example.com",
  "password": "SecurePassword123!"
}
```

**Response:**
```json
{
  "success": true,
  "requiresMFA": true,
  "mfaMethods": ["totp", "email", "sms"],
  "sessionId": "session_uuid",
  "expiresIn": 300
}
```

#### Enable TOTP (2FA)
```http
POST /api/mfa/totp/setup
Authorization: Bearer {token}
Content-Type: application/json

{}
```

**Response:**
```json
{
  "success": true,
  "secret": "JBSWY3DPEBLW64TMMQ======",
  "qrCode": "data:image/png;base64,...",
  "backupCodes": ["code1", "code2", "code3", ...]
}
```

#### Verify TOTP
```http
POST /api/mfa/totp/verify
Content-Type: application/json

{
  "sessionId": "session_uuid",
  "token": "123456"
}
```

#### Send OTP via Email
```http
POST /api/verification/email/send-otp
Content-Type: application/json

{
  "email": "user@example.com"
}
```

#### Verify OTP via Email
```http
POST /api/verification/email/verify-otp
Content-Type: application/json

{
  "email": "user@example.com",
  "otp": "123456"
}
```

#### Send OTP via SMS
```http
POST /api/verification/sms/send-otp
Content-Type: application/json

{
  "phoneNumber": "+1234567890"
}
```

#### Verify OTP via SMS
```http
POST /api/verification/sms/verify-otp
Content-Type: application/json

{
  "phoneNumber": "+1234567890",
  "otp": "123456"
}
```

#### Manage Trusted Devices
```http
POST /api/devices/trust
Authorization: Bearer {token}
Content-Type: application/json

{
  "deviceName": "My iPhone",
  "trustForDays": 30
}
```

#### List Trusted Devices
```http
GET /api/devices/list
Authorization: Bearer {token}
```

#### Revoke Device Trust
```http
POST /api/devices/revoke
Authorization: Bearer {token}
Content-Type: application/json

{
  "deviceId": "device_uuid"
}
```

## 🔐 Security Best Practices

### Password Requirements
- Minimum 12 characters
- At least one uppercase letter
- At least one lowercase letter
- At least one number
- At least one special character (!@#$%^&*)

### Rate Limiting
- 5 login attempts per 15 minutes per IP
- 3 OTP verification attempts per 10 minutes
- 10 API requests per minute for authenticated users

### Token Management
- JWT tokens expire after 15 minutes
- Refresh tokens expire after 7 days
- Session tokens expire after 30 minutes of inactivity

### Data Protection
- All sensitive data encrypted at rest
- HTTPS/TLS 1.2+ for all communications
- CORS properly configured
- CSRF protection enabled
- SQL injection prevention through parameterized queries

### Audit Trail
- All authentication events logged
- IP addresses and device information tracked
- Failed login attempts monitored
- Admin access audit trail maintained

## 📦 Project Structure

```
dragon-of-north/
├── src/
│   ├── controllers/
│   │   ├── authController.js
│   │   ├── mfaController.js
│   │   └── verificationController.js
│   ├── middleware/
│   │   ├── authMiddleware.js
│   │   ├── rateLimitMiddleware.js
│   │   └── errorHandler.js
│   ├── models/
│   │   ├── User.js
│   │   ├── Session.js
│   │   └── AuditLog.js
│   ├── routes/
│   │   ├── authRoutes.js
│   │   ├── mfaRoutes.js
│   │   └── verificationRoutes.js
│   ├── services/
│   │   ├── authService.js
│   │   ├── mfaService.js
│   │   ├── emailService.js
│   │   └── smsService.js
│   ├── utils/
│   │   ├── validators.js
│   │   ├── cryptography.js
│   │   └── logger.js
│   └── app.js
├── tests/
│   ├── unit/
│   ├── integration/
│   └── e2e/
├── migrations/
├── .env.example
├── package.json
├── README.md
└── LICENSE
```

## 🧪 Testing

### Run All Tests
```bash
npm test
```

### Run Unit Tests
```bash
npm run test:unit
```

### Run Integration Tests
```bash
npm run test:integration
```

### Run End-to-End Tests
```bash
npm run test:e2e
```

### Generate Coverage Report
```bash
npm run test:coverage
```

## 🚀 Deployment

### Docker Deployment
```bash
# Build Docker image
docker build -t dragon-of-north:latest .

# Run container
docker run -p 3000:3000 --env-file .env dragon-of-north:latest
```

### Docker Compose
```bash
docker-compose up -d
```

### Kubernetes Deployment
```bash
kubectl apply -f k8s/
```

## 📊 Performance

- **Response Time**: < 200ms for 95th percentile
- **Throughput**: 1000+ requests per second
- **Availability**: 99.9% uptime SLA
- **Database Queries**: Optimized with proper indexing

## 🔄 API Response Format

All API responses follow a standard format:

**Success Response:**
```json
{
  "success": true,
  "message": "Operation completed successfully",
  "data": {
    "key": "value"
  },
  "timestamp": "2025-12-17T14:21:14Z"
}
```

**Error Response:**
```json
{
  "success": false,
  "error": {
    "code": "ERROR_CODE",
    "message": "Error description",
    "details": []
  },
  "timestamp": "2025-12-17T14:21:14Z"
}
```

## 🤝 Contributing

We welcome contributions! Please follow these steps:

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

### Code Style
- Use ESLint for code formatting
- Follow the existing code conventions
- Write unit tests for new features
- Update documentation

## 📝 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## 🐛 Bug Reports & Feature Requests

Please report bugs and request features by opening an issue on GitHub:
[Issues](https://github.com/Vinay2080/dragon-of-north/issues)

## 📞 Support

For support, please contact:
- Email: support@dragonnorth.com
- Documentation: [docs/](docs/)
- Issues: [GitHub Issues](https://github.com/Vinay2080/dragon-of-north/issues)

## 🙏 Acknowledgments

- Built with Node.js and Express
- Security inspired by OWASP guidelines
- Authentication flows based on industry standards

## 📈 Roadmap

- [ ] WebAuthn/FIDO2 support
- [ ] Push notification verification
- [ ] Advanced analytics dashboard
- [ ] Machine learning-based risk assessment
- [ ] Integration with popular identity providers
- [ ] Multi-language support
- [ ] Mobile app for authentication management

---

**Last Updated:** 2025-12-17 14:21:14 UTC

For more information, visit our [GitHub repository](https://github.com/Vinay2080/dragon-of-north)
