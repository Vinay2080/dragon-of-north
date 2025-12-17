# Dragon of North

A modern Spring Boot application built with Java 25 and the latest enterprise technologies.

## Overview

Dragon of North is a comprehensive Spring Boot 4.0.0 application demonstrating best practices in microservices architecture, security, and cloud-native development.

## Technology Stack

### Core Framework
- **Java**: 25
- **Spring Boot**: 4.0.0
- **Build Tool**: Maven/Gradle

### Security & Authentication
- **JWT Authentication**: RSA-based encryption
- **Token Management**: Secure token generation and validation
- **Authorization**: Role-based access control (RBAC)

### Communication & Notifications
- **OTP Management**: 
  - AWS Simple Notification Service (SNS) for SMS delivery
  - AWS Simple Email Service (SES) for email delivery
  - Time-based OTP (TOTP) implementation
  - Secure OTP validation and expiration handling

### Data Management
- **Database**: JPA/Hibernate ORM
- **Caching**: Spring Cache abstractions
- **Data Validation**: Bean Validation (Jakarta Validation)

## Project Structure

dragon-of-north/ ├── src/ │ ├── main/ │ │ ├── java/ │ │ │ └── com/ │ │ │ └── dragonofthenorth/ │ │ │ ├── config/ # Configuration classes │ │ │ ├── controller/ # REST controllers │ │ │ ├── service/ # Business logic │ │ │ ├── repository/ # Data access layer │ │ │ ├── entity/ # JPA entities │ │ │ ├── dto/ # Data transfer objects │ │ │ ├── security/ # Security & JWT handling │ │ │ ├── notification/ # OTP & AWS SNS/SES │ │ │ ├── exception/ # Custom exceptions │ │ │ └── util/ # Utility classes │ │ └── resources/ │ │ ├── application.yml # Main configuration │ │ ├── application-dev.yml # Development profile │ │ ├── application-prod.yml # Production profile │ │ └── db/ │ │ └── migration/ # Database migrations │ └── test/ │ ├── java/ # Unit and integration tests │ └── resources/ # Test resources ├── pom.xml # Maven configuration ├── README.md # This file └── .gitignore

Code

## Key Features

### 1. JWT Authentication with RSA
- RSA key pair management
- Token generation with expiration
- Token validation and refresh mechanisms
- Claims-based authorization

### 2. OTP Management System
- Multi-channel OTP delivery (SMS via SNS, Email via SES)
- Configurable OTP length and expiration
- Rate limiting and attempt tracking
- Secure OTP storage with encryption

### 3. Proper Layered Architecture
- **Controller Layer**: REST API endpoints
- **Service Layer**: Business logic and OTP/JWT management
- **Repository Layer**: Database operations
- **Configuration Layer**: Security, AWS, and application configs
- **Security Layer**: JWT filters, RSA encryption, Authorization

### 4. AWS Integration
- SNS for SMS-based OTP delivery
- SES for email notifications
- IAM role-based access
- Environment-based configuration

## Dependencies

Key Spring Boot 4.0.0 dependencies:
- spring-boot-starter-web
- spring-boot-starter-security
- spring-boot-starter-data-jpa
- spring-boot-starter-validation
- jjwt (JWT library with RSA support)
- aws-java-sdk-sns
- aws-java-sdk-ses
- lombok (optional, for reducing boilerplate)

## Configuration

### Application Properties
```yaml
spring:
  application:
    name: dragon-of-north
  jpa:
    hibernate:
      ddl-auto: validate
    properties:
      hibernate:
        dialect: org.hibernate.dialect.PostgreSQL15Dialect

  datasource:
    url: jdbc:postgresql://localhost:5432/dragon_db
    username: ${DB_USER}
    password: ${DB_PASSWORD}

jwt:
  secret: ${JWT_SECRET}
  expiration: 3600000  # 1 hour in milliseconds

aws:
  region: ${AWS_REGION}
  sns:
    topic-arn: ${AWS_SNS_TOPIC_ARN}
  ses:
    from-email: ${AWS_SES_FROM_EMAIL}

otp:
  length: 6
  expiration-time: 300  # 5 minutes in seconds
  max-attempts: 3
Getting Started
Prerequisites
Java 25 or higher
Maven 3.8+
PostgreSQL 15+ (or your preferred database)
AWS Account with SNS and SES configured
Installation
Clone the repository:
bash
git clone https://github.com/Vinay2080/dragon-of-north.git
cd dragon-of-north
Configure environment variables:
bash
export DB_USER=your_db_user
export DB_PASSWORD=your_db_password
export JWT_SECRET=your_jwt_secret
export AWS_REGION=your_aws_region
export AWS_SNS_TOPIC_ARN=your_sns_topic_arn
export AWS_SES_FROM_EMAIL=your_ses_from_email
Build the project:
bash
mvn clean install
Run the application:
bash
mvn spring-boot:run
The application will start on http://localhost:8080

API Endpoints
Authentication
POST /api/auth/register - Register new user
POST /api/auth/login - User login
POST /api/auth/refresh-token - Refresh JWT token
POST /api/auth/logout - User logout
OTP Management
POST /api/otp/send - Send OTP via SMS or Email
POST /api/otp/verify - Verify OTP
POST /api/otp/resend - Resend OTP
User Profile
GET /api/users/profile - Get user profile
PUT /api/users/profile - Update user profile
Testing
Run unit and integration tests:

bash
mvn test
Run specific test class:

bash
mvn test -Dtest=YourTestClass
Security Considerations
All sensitive data is stored with encryption
JWT tokens use RSA encryption
OTP data is time-limited and encrypted
AWS credentials are managed via IAM roles
Input validation is performed at all layers
CORS is properly configured for API access
Contributing
Create a feature branch (git checkout -b feature/amazing-feature)
Commit your changes (git commit -m 'Add some amazing feature')
Push to the branch (git push origin feature/amazing-feature)
Open a Pull Request
License
This project is licensed under the MIT License - see the LICENSE file for details.
