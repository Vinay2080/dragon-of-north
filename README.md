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

## Testing 🧪✅

- **Frameworks**: JUnit 5 🧫 + Mockito 🎭
- **Style**: AAA (Arrange → Act → Assert) 🧩
- **Goal**: Unit tests for core services (happy paths + edge cases) 🔍

### Where are the tests? 🗂️
- `src/test/java/...`

### Run tests locally 🏃‍♂️⚡
```bash
mvn test
```

### Example (Service Unit Tests) 🧠
- Uses `@ExtendWith(MockitoExtension.class)`
- Mocks repositories / dependencies and verifies interactions via `verify(...)`

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
| Method | Endpoint                                   | Purpose                                                                               |
|--------|--------------------------------------------|---------------------------------------------------------------------------------------|
| GET    | `/api/v1/auth/identifier/status`           | Returns user status for an identifier *(current implementation reads a request body)* |
| POST   | `/api/v1/auth/identifier/sign-up`          | Sign up user                                                                          |
| POST   | `/api/v1/auth/identifier/sign-up/complete` | Complete signup (status update after OTP verification)                                |
| POST   | `/api/v1/auth/identifier/login`            | Login and return JWT access + refresh tokens                                          |
| POST   | `/api/v1/auth/jwt/refresh`                 | Exchange refresh token for a new access token                                         |

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

## Data Transfer Objects (DTOs) and Validation 🔍

The project follows a clean DTO pattern to ensure proper separation between API contracts and domain models. Here's how DTOs are structured and validated:

### Request DTOs
- Located in `dto/auth/request/` and `dto/otp/request/`
- Each API endpoint has a dedicated request DTO
- Uses Jakarta Bean Validation annotations for input validation
- Example validation rules:
  - `@NotBlank` - Ensures string is not null and has non-whitespace characters
  - `@Size` - Validates string length
  - `@Pattern` - Validates against regex patterns (e.g., password complexity)
  - `@Valid` - Triggers nested object validation

### Response DTOs
- Located in `dto/auth/response/`
- Each response follows a consistent structure
- Sensitive data is never exposed in responses
- Example response classes:
  - `AuthenticationResponse` - For login/signup responses
  - `AppUserStatusFinderResponse` - For user status checks
  - `RefreshTokenResponse` - For token refresh operations

### Standardized API Responses
All API responses follow a consistent format through `ApiResponse<T>` class:
```json
{
    "message": "string | null",
    "apiResponseStatus": "SUCCESS | ERROR | ...",
    "data": "T",
    "time": "ISO-8601 timestamp"
}
```

### Error Handling
- Custom exceptions are mapped to appropriate HTTP status codes
- Validation errors are automatically transformed into consistent error responses
- Example error response:
```json
{
    "message": "Validation failed",
    "apiResponseStatus": "ERROR",
    "data": {
        "fieldName": "Error message"
    },
    "time": "2025-01-05T16:43:29.12345Z"
}
```

## Exception Handling 🚨

The application implements a robust exception handling mechanism using Spring's `@ControllerAdvice` pattern. Here's how it's structured:

### BusinessException
- Custom runtime exception for all business-level errors
- Wraps an `ErrorCode` with associated HTTP status
- Supports both static and dynamic error messages
- Example usage:
  ```java
throw new BusinessException(ErrorCode.USER_NOT_FOUND) {
      return ErrorCode
  }
  // or with dynamic values
  throw new BusinessException(ErrorCode.STATUS_MISMATCH, "VERIFIED");
  ```

### ErrorCode Enum
- Centralized error definitions with consistent structure
- Each error includes:
  - Unique error code (e.g., `AUTH_001`)
  - Default error message (with optional placeholders)
  - Appropriate HTTP status code
- Organized by domain (AUTH, USER, TOK, etc.)

### Global Exception Handler
- `ApplicationExceptionHandler` handles all exceptions globally
- Transforms exceptions into standardized error responses
- Handles different exception types:
  - `BusinessException`: Custom business logic errors
  - `MethodArgumentNotValidException`: Request validation errors
  - `BadCredentialsException`: Authentication failures
  - Generic exception fallback

### Error Response Format
All error responses follow this structure:
```json
{
  "message": "Error message",
  "apiResponseStatus": "ERROR",
  "data": {
    "code": "AUTH_001",
    "defaultMessage": "Error description"
  },
  "time": "2025-01-05T16:50:29.12345Z"
}
```

### Common Error Codes
| Code      | HTTP Status | Description                                      |
|-----------|-------------|--------------------------------------------------|
| TOK_001   | 401         | Invalid or expired JWT token                     |
| AUTH_001  | 400         | Identifier type mismatch                         |
| AUTH_002  | 429         | Too many requests                                |
| USER_001  | 404         | User not found                                   |
| USER_002  | 409         | User already verified                            |
| ROL_009   | 404         | Role not found                                   |

## Factory and Resolver Pattern Implementation 🏭

The application uses both the Factory and Resolver patterns to manage different authentication methods (email/phone) and OTP senders. This combination provides a clean way to handle multiple implementations of the same interface.

### Design Decision: Resolver vs. Direct Injection

1. **Authentication Layer (Using Resolver)**
   - Uses `AuthenticationServiceResolver` to dynamically select the appropriate service
   - Better suited for authentication where the same operations are performed differently based on an identifier type
   - Reduces code duplication in controllers

2. **OTP Layer (Direct Injection)**
   - Uses direct injection of specific OTP senders in `OtpService`
   - More appropriate when operations differ significantly between channels
   - Provides better type safety and compile-time checks
   - Makes the API more explicit about supported channels

### Authentication Service Factory

#### 1. Service Interface
```java
public interface AuthenticationService {
    IdentifierType supports();
    AppUserStatusFinderResponse getUserStatus(String identifier);
    AppUserStatusFinderResponse signUpUser(AppUserSignUpRequest request);
    AppUserStatusFinderResponse completeSignUp(String identifier);
}
```

#### 2. Concrete Implementations
- `EmailAuthenticationServiceImpl` - Handles email-based authentication
- `PhoneAuthenticationServiceImpl` - Handles phone-based authentication

#### 3. Service Resolver (Factory)
The `AuthenticationServiceResolver` acts as a factory that provides the appropriate service based on the identifier type:

```java
@Service
public class AuthenticationServiceResolver {
    private final Map<IdentifierType, AuthenticationService> serviceMap;

    public AuthenticationServiceResolver(List<AuthenticationService> services) {
        this.serviceMap = services.stream()
            .collect(Collectors.toMap(
                AuthenticationService::supports,
                Function.identity()
            ));
    }

    public AuthenticationService resolve(String identifier, IdentifierType type) {
        // Validation and service resolution logic
        return serviceMap.get(type);
    }
}
```

### OTP Sender Factory

#### 1. Sender Interface
```java
public interface OtpSender {
    void send(String identifier, String otp, int ttlMinutes);
}
```

#### 2. Concrete Implementations
- `EmailOtpSender` - Sends OTP via email using AWS SES
- `PhoneOtpSender` - Sends OTP via SMS using AWS SNS

#### 3. Usage in Service
```java
@Service
@RequiredArgsConstructor
public class OtpService {
    private final OtpSender emailOtpSender;
    private final OtpSender phoneOtpSender;
    
    public void createEmailOtp(String email, OtpPurpose otpPurpose) {
        createOtp(emailOtpSender, otpPurpose, email, EMAIL, e -> e.trim().toLowerCase());
    }
    
    public void createPhoneOtp(String phone, OtpPurpose otpPurpose) {
        createOtp(phoneOtpSender, otpPurpose, phone, PHONE, p -> p.replace(" ", ""));
    }
}
```

### Benefits of This Approach
1. **Extensibility**: Easy to add new authentication methods or OTP channels
2. **Single Responsibility**: Each service handles one specific type of authentication
3. **Loose Coupling**: Services are decoupled from their consumers
4. **Testability**: Each implementation can be tested in isolation
5. **Maintainability**: Changes to one authentication method don't affect others

## BaseEntity and Javadoc Standards 📝

The application uses a `BaseEntity` class to provide common fields and functionality across all entities, following DRY (Don't Repeat Yourself) principles and ensuring consistent auditing.

### BaseEntity Implementation

#### 1. Core Features
```java
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
public class BaseEntity {
    @Id
    @GeneratedValue
    @UuidGenerator(style = UuidGenerator.Style.TIME)
    private UUID id;
    
    @CreationTimestamp
    private Instant createdAt;
    
    @UpdateTimestamp
    private Instant updatedAt;
    
    @CreatedBy
    private String createdBy;
    
    @LastModifiedBy
    private String updatedBy;
    
    private Boolean deleted = false;
    
    @Version
    private Long version;
}
```

#### 2. Key Benefits
- **Consistent Audit Fields**: All entities automatically track creation/modification timestamps and users
- **Soft Delete Support**: Built-in soft delete functionality
- **Optimistic Locking**: Version field for concurrent modification control
- **Type-Safe IDs**: UUID-based primary keys

### Entity Implementation Example
```java
import org.miniProjectTwo.DragonOfNorth.enums.AppUserStatus;
import org.miniProjectTwo.DragonOfNorth.model.BaseEntity;

/**
 * Represents a user in the system.
 * This entity extends {@link BaseEntity} to inherit common audit fields.
 *
 * <p>This class includes user authentication and profile information
 * and uses JPA annotations for object-relational mapping.</p>
 *
 * @see BaseEntity
 * @see AppUserStatus
 */
@Entity
@Table(name = "users")
public class AppUser extends BaseEntity {
    @Column(unique = true)
    private String email;
    
    @Column(name = "phone_number", unique = true)
    private String phone;
    
    // Other fields and methods
}
```

### Javadoc Standards

#### 1. Class-Level Documentation
```java
/**
 * Brief description of the class.
 * 
 * <p>Additional detailed description that may span multiple
 * paragraphs if needed.</p>
 *
 * <p>Use HTML tags for formatting and paragraphs. Include examples
 * when they help clarify usage.</p>
 *
 * @see RelatedClass
 * @author Author Name
 * @since 1.0
 */
```

#### 2. Method Documentation
```java
/**
 * Performs user authentication with the given credentials.
 *
 * @param username the username for authentication (must not be null or empty)
 * @param password the password for authentication (must not be null)
 * @return AuthenticationResponse containing JWT tokens if successful
 * @throws AuthenticationException if authentication fails
 * @throws IllegalArgumentException if the username or password is invalid
 * @since 1.0
 */
```

#### 3. Field Documentation
```java
/**
 * The user's email address.
 * Must be unique across all users and follow the standard email format.
 * Used as the primary identifier for email-based authentication.
 */
@Column(unique = true, nullable = false)
private String email;
```

#### 4. Best Practices
- Document all public and protected members
- Use `{@code }` for code references
- Include `@param`, `@return`, and `@throws` where applicable
- Document thread safety and nullability
- Include version information with `@since`
- Use `@see` for related classes/methods

## Enums and Initializers (Dev/Test Data) 🧩

Enums are used heavily to make flows explicit, reduce magic strings, and keep controller/service logic readable and scalable.

### Enums (Where and Why)
- **`IdentifierType`**
  - Drives the authentication factory/resolver selection (e.g., `EMAIL`, `PHONE`).
  - Keeps identifier-routing type-safe and extensible.
- **`AppUserStatus`**
  - Models the user lifecycle (e.g., `CREATED`, `VERIFIED`, `ACTIVE`, `BLOCKED`, `DELETED`).
  - Used in auth flows and also in dev/test seeded users to simulate real-world states.
- **`OtpPurpose`**
  - Scopes OTP generation and verification by intent (e.g., `SIGNUP`, `LOGIN`, `PASSWORD_RESET`, `TWO_FACTOR_AUTH`).
  - Prevents OTP reuse across flows and keeps OTP logic consistent.
- **`OtpVerificationStatus`**
  - Standardizes OTP verify outcomes and messaging via a single enum (includes a boolean `success`).
  - Helps controllers return a consistent response while still being expressive about failure reasons.
- **`RoleName`**
  - Central list of system roles (e.g., `USER`, `ADMIN`).
  - Used by role initialization logic so role creation stays consistent.
- **`ApiResponseStatus`**
  - Ensures API responses use a controlled status vocabulary (e.g., `success`, `failed`).

### Initializers

The project uses Spring Boot startup initializers to ensure essential data exists and to speed up local development.

- **Roles initialization (always on)**
  - Implemented in `config/initializer/RolesInitializer`.
  - Runs as a `CommandLineRunner` with `@Order(1)`.
  - Creates missing roles based on `RoleName.values()`.

- **Dev/Test data seeding (profile-based)**
  - Implemented in `config/initializer/TestDataInitializer`.
  - Enabled only for profiles: `dev` and `test` via `@Profile({"dev", "test"})`.
  - Runs after roles via `@Order(2)`.
  - Seeds users with multiple statuses (e.g., `CREATED`, `VERIFIED`, `ACTIVE`, `BLOCKED`) to test different auth flows.

To enable seeded data locally, run with an active profile:
- `spring.profiles.active=dev`

## Configuration Highlights ⚙️ (from `application.yaml`)

Some important tunable (beyond OTP + signup limiter metrics listed above):

- **Database / JPA**
  - `spring.jpa.hibernate.ddl-auto: create` (recreates schema on startup; use carefully).
  - DB credentials are read from `.env` via `${db_username}` and `${db_password}`.
- **JWT**
  - `app.security.jwt.expiration.access-token: 900000` (15 minutes)
  - `app.security.jwt.expiration.refresh-token: 604800000` (7 days)
- **OTP cleanup job**
  - `otp.cleanup.delay-ms: 3599999`
- **Login abuse limiter**
  - `auth.login.max-failed-attempts: 5`
  - `auth.login.block-duration-minutes: 15`
- **AWS integration hooks (optional)**
  - `aws.region: us-east-1`
  - `aws.ses.sender: <configured email>`

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