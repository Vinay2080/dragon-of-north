# Dragon of North

A production-style **authentication and session management platform** built with Spring Boot + React.
This project demonstrates backend engineering depth for internship/recruiter evaluation: secure auth design, OTP verification, session lifecycle control, abuse prevention, cloud integrations, and test coverage.

## Why this project stands out

- End-to-end auth system (signup → OTP verification → login → refresh → logout → session revoke)
- Security-first architecture (JWT + HTTP-only cookies, Redis rate limiting, account protection)
- Real infrastructure integrations (PostgreSQL, Redis, AWS SES/SNS)
- Strong engineering hygiene (clean layering, DTO validation, global exception handling, automated tests)
- Interview-friendly API docs (OpenAPI/Swagger with request/response examples)

---

## Core Features

### Authentication & User Lifecycle
- Identifier-based auth using **email** or **phone**.
- Signup and signup-complete flow with explicit user lifecycle statuses:
  - `NOT_EXIST`, `CREATED`, `VERIFIED`, `DELETED`
- Secure login with credential validation and device-aware session tracking.

### OTP Engine
- Email OTP and phone OTP generation.
- Purpose-aware OTP workflows:
  - `SIGNUP`, `LOGIN`, `PASSWORD_RESET`, `TWO_FACTOR_AUTH`
- OTP verification outcomes include states like `SUCCESS`, `INVALID_OTP`, `EXPIRED_OTP`, `ALREADY_USED`, etc.

### Session Management
- Multiple-device session support.
- List current user sessions.
- Revoke single session by ID.
- Revoke all other sessions except current device.

### Security & Abuse Prevention
- JWT-based auth (access + refresh token flow).
- HTTP-only secure cookies for token delivery.
- Redis-backed rate limiting with Bucket4j.
- Endpoint-specific controls for signup/login/OTP flows.
- Strong DTO validation and centralized error handling.

### Operations & Production Readiness
- PostgreSQL persistence via Spring Data JPA.
- Redis cache/rate-limit store.
- AWS SES/SNS integration for messaging.
- Prometheus metrics + Actuator endpoints.
- Docker-ready backend and Vite-based frontend.

---

## Tech Stack

### Backend
- Java 21
- Spring Boot 4
- Spring Security
- Spring Data JPA
- Springdoc OpenAPI (Swagger UI)
- PostgreSQL
- Redis + Bucket4j
- Micrometer + Prometheus
- AWS SDK (SES/SNS)

### Frontend
- React
- Vite
- TailwindCSS

### Testing
- JUnit 5
- Spring Boot Test
- Testcontainers

---

## High-Level Architecture

```text
React Frontend
   ↓
Spring Boot REST API
   ├── Controllers (Auth / OTP / Sessions)
   ├── Services (Business Logic)
   ├── Repositories (JPA)
   ├── Security (JWT filter, token services)
   ├── Rate Limiter (Redis + Bucket4j)
   └── Integrations (AWS SES/SNS)
        ↓
PostgreSQL + Redis
```

---

## API Overview

Base path: ` /api/v1 `

### Authentication
- `POST /auth/identifier/status`
- `POST /auth/identifier/sign-up`
- `POST /auth/identifier/sign-up/complete`
- `POST /auth/identifier/login`
- `POST /auth/jwt/refresh`
- `POST /auth/identifier/logout`

### OTP
- `POST /otp/email/request`
- `POST /otp/email/verify`
- `POST /otp/phone/request`
- `POST /otp/phone/verify`

### Sessions
- `GET /sessions/get/all`
- `DELETE /sessions/delete/{sessionId}`
- `POST /sessions/revoke-others`

Swagger UI: `/swagger-ui/index.html`
OpenAPI JSON: `/v3/api-docs`

---

## Getting Started (Local)

### 1) Clone the repository
```bash
git clone https://github.com/Vinay2080/dragon-of-north.git
cd dragon-of-north
```

### 2) Configure environment
Create a `.env` file in project root with values referenced in `application.yaml`.
Important groups:
- DB: `db_name`, `db_url`, `db_port`, `db_username`, `db_password`
- JWT: `access_token`, `refresh_token`, `public_key_path`, `private_key_path`
- Redis: `redis_host`, `redis_password`
- OTP/auth throttling values
- AWS: `aws_region`, `aws_ses_sender`

### 3) Run backend
```bash
./mvnw spring-boot:run
```

### 4) Run frontend
```bash
cd frontend
npm install
npm run dev
```

---

## Example API Requests

### Sign up
```bash
curl -X POST http://localhost:8080/api/v1/auth/identifier/sign-up \
  -H "Content-Type: application/json" \
  -d '{
    "identifier": "intern.candidate@example.com",
    "identifier_type": "EMAIL",
    "password": "Intern@123"
  }'
```

### Request OTP (email)
```bash
curl -X POST http://localhost:8080/api/v1/otp/email/request \
  -H "Content-Type: application/json" \
  -d '{
    "email": "intern.candidate@example.com",
    "otp_purpose": "SIGNUP"
  }'
```

### Verify OTP (email)
```bash
curl -X POST http://localhost:8080/api/v1/otp/email/verify \
  -H "Content-Type: application/json" \
  -d '{
    "email": "intern.candidate@example.com",
    "otp": "123456",
    "otp_purpose": "SIGNUP"
  }'
```

### Login
```bash
curl -X POST http://localhost:8080/api/v1/auth/identifier/login \
  -H "Content-Type: application/json" \
  -d '{
    "identifier": "intern.candidate@example.com",
    "password": "Intern@123",
    "device_id": "web-chrome-macos"
  }' \
  -c cookies.txt
```

---

## Testing

Run all tests:
```bash
./mvnw test
```

---

## Project Structure

```text
src/main/java/org/miniProjectTwo/DragonOfNorth
├── config
├── controller
├── dto
├── enums
├── exception
├── model
├── ratelimit
├── repositories
├── resolver
├── serviceInterfaces
└── services
```

---

## Career/Interview Talking Points

If you're presenting this for internships, highlight:
- How you designed secure cookie + JWT refresh flows
- How rate limiting strategy differs by endpoint risk profile
- How session revocation supports real multi-device account security
- How you modeled account lifecycle and OTP purpose as enums for safety
- How your tests protect auth-critical paths

---

## License

MIT
