# 🚀 Dragon of North - Cloud-Native Authentication Platform

A Spring Boot authentication and OTP verification service (email/phone) with a clean modular structure (Spring Modulith) and a PostgreSQL-backed user store.

## Why this project exists 🎯
This project focuses on building a practical auth foundation:
- Signup flow with **OTP verification**
- User lifecycle/status handling (e.g., `CREATED`, `VERIFIED`, `NOT_EXIST`)
- Sensible **rate limiting / abuse controls**
- Clean separation of concerns (controller/service/repository)

---

## Tech Stack 🧰
| Layer           | What                 | Notes                               |
|-----------------|----------------------|-------------------------------------|
| Runtime         | Java 25 ☕            | Set in `pom.xml`                    |
| Framework       | Spring Boot 4 🚀     | WebMVC + Security                   |
| Persistence     | Spring Data JPA 🗃️  | PostgreSQL runtime dependency       |
| Modularity      | Spring Modulith 🧩   | For module boundaries               |
| API Docs        | springdoc-openapi 📘 | Swagger UI enabled                  |
| Tokens          | JJWT 🎟️             | JWT parsing/validation deps present |
| Email/SMS hooks | AWS SDK (SES/SNS) ☁️ | Optional integration points         |

---

## Feature Snapshot ✨
| Feature                                       | Status                                |
|-----------------------------------------------|---------------------------------------|
| Email OTP request/verify ✉️                   | ✅ Implemented                         |
| Phone OTP request/verify 📱                   | ✅ Implemented                         |
| Signup flow (email identifier) 🧾             | ✅ Implemented                         |
| Login (JWT access + refresh) 🔐               | ✅ Implemented                         |
| Refresh access token (JWT refresh) ♻️         | ✅ Implemented                         |
| Default role assignment 🧑‍💼                 | ✅ Implemented                         |
| Swagger UI 📚                                 | ✅ Available                           |
| “Full AWS infra deployment” (ECS/CDK/etc.) ☁️ | 🧊 Not in this repo (future/optional) |

---

## API Endpoints 🛣️
Base paths:
- `/api/v1/auth`
- `/api/v1/otp`

JSON naming:
- This project configures Jackson with `SNAKE_CASE` in `application.yaml`.
- That means multi-word JSON fields are expected in snake_case (example: `identifier_type`, `otp_purpose`).
- **Exception:** `RefreshTokenRequest` currently uses `@JsonProperty("refreshToken")`, so `/jwt/refresh` expects `refreshToken` (camelCase).

### Auth
| Method | Endpoint                         | Purpose                                                                               |
|--------|----------------------------------|---------------------------------------------------------------------------------------|
| GET    | `/api/v1/auth/identifier/status` | Returns user status for an identifier *(current implementation reads a request body)* |
| POST   | `/api/v1/auth/identifier/sign-up`  | Sign up user                                                                           |
| POST   | `/api/v1/auth/identifier/sign-up/complete` | Complete signup (status update after OTP verification)                                |
| POST   | `/api/v1/auth/identifier/login` | Login and return JWT access + refresh tokens                                           |
| POST   | `/api/v1/auth/jwt/refresh`       | Exchange refresh token for a new access token                                          |

### OTP
| Method | Endpoint                    | Purpose               |
|--------|-----------------------------|-----------------------|
| POST   | `/api/v1/otp/email/request` | Request OTP for email |
| POST   | `/api/v1/otp/email/verify`  | Verify OTP for email  |
| POST   | `/api/v1/otp/phone/request` | Request OTP for phone |
| POST   | `/api/v1/otp/phone/verify`  | Verify OTP for phone  |

---

## Dragon Metrics 🐲📊 (from `application.yaml`)
### OTP rules
| Setting             | Value          |
|---------------------|----------------|
| OTP length          | `6`            |
| TTL                 | `10 minutes`   |
| Max verify attempts | `3`            |
| Request window      | `3600 seconds` |
| Max requests/window | `10`           |
| Resend cooldown     | `60 seconds`   |
| Block duration      | `15 minutes`   |

### Signup limiter
| Setting             | Value          |
|---------------------|----------------|
| Max requests/window | `5`            |
| Request window      | `3600 seconds` |
| Block duration      | `30 minutes`   |

### Tiny “bar chart” for vibe
OTP TTL: `██████████` 10m  
Resend cooldown: `█` 60s  
Max verify attempts: `███` 3  

---

## Run Locally 🏃‍♂️💨
### Prerequisites
- Java 25
- Maven 3.9+
- PostgreSQL (local)

### Configure
This project imports environment from `.env`:
- `spring.config.import: optional:file:.env[.properties]`

Minimal variables needed (match `application.yaml`):
- `db_username`
- `db_password`

Database URL is currently:
- `jdbc:postgresql://localhost:5432/dragon_of_north`

### Start
```bash
mvn spring-boot:run
```

---

## Swagger / OpenAPI 📚
Once the app is running, try:
- `http://localhost:8080/swagger-ui/index.html`

---

## Project Structure

```
dragon-of-north/
├── src/
│   ├── main/
│   │   ├── java/org/miniProjectTwo/DragonOfNorth/
│   │   │   ├── common/            # Shared utilities and constants
│   │   │   ├── config/            # Configuration classes
│   │   │   │   ├── OtpConfig/     # OTP specific configurations
│   │   │   │   └── security/      # Security configurations
│   │   │   ├── controller/        # REST API endpoints
│   │   │   ├── dto/               # Data Transfer Objects
│   │   │   │   ├── api/           # API response structures
│   │   │   │   ├── auth/          # Authentication DTOs
│   │   │   │   └── otp/           # OTP related DTOs
│   │   │   ├── enums/             # Enumerations
│   │   │   ├── exception/         # Custom exceptions
│   │   │   ├── impl/              # Service implementations
│   │   │   ├── mapper/            # Object mappers (MapStruct)
│   │   │   ├── model/             # Domain models
│   │   │   ├── repositories/      # Data access layer
│   │   │   └── services/          # Business logic interfaces
│   │   └── resources/
│   │       ├── META-INF/            # Spring resources
│   │       ├── application.yaml     # Main configuration
│   │       └── local-keys/          # Local RSA keys for JWT signing
│   └── test/                     # Test suites
└── pom.xml                      # Maven configuration
```

---

## Roadmap 🗺️
- Tighten endpoint semantics (e.g., avoid GET with request body)
- Add integration tests for OTP + signup flows
- Optional: production deployment guide (Docker + cloud)

---

## License 📄
MIT (see `LICENSE`)