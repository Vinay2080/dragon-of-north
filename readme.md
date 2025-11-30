# Dragon of North

A Spring Boot application for managing and tracking entities with JPA auditing capabilities.

## Features

- **Spring Boot** - Latest Spring Boot framework for building the application
- **JPA Auditing** - Automatic auditing of entity changes (created date, modified date, etc.)
- **RESTful API** - Exposes REST endpoints for entity management
- **Database Integration** - Seamless integration with relational databases

## Tech Stack

- Java 17+
- Spring Boot 3.x
- Spring Data JPA
- Hibernate
- Maven

## Prerequisites

- JDK 17 or higher
- Maven 3.6.3 or higher
- Your preferred IDE (IntelliJ IDEA recommended)
- Database (H2/MySQL/PostgreSQL - as per your configuration)

## Getting Started

1. **Clone the repository**
   ```bash
   git clone https://github.com/yourusername/dragon-of-north.git
   cd dragon-of-north
   ```

2. **Build the project**
   ```bash
   mvn clean install
   ```

3. **Run the application**
   ```bash
   mvn spring-boot:run
   ```

4. **Access the application**
   - The application will be available at `http://localhost:8080`
   - Swagger/OpenAPI documentation (if configured) will be at `http://localhost:8080/swagger-ui.html`

## Project Structure

```
src/main/java/org/miniProjectTwo/DragonOfNorth/
├── common/           # Common components and base entities
├── config/           # Configuration classes
├── enums/            # Enumeration types
├── impl/             # Implementation classes
├── model/            # Entity models
└── DragonOfNorthApplication.java  # Main application class
```

## Configuration

Update the following in `application.properties` or `application.yml`:

```properties
# Database Configuration
spring.datasource.url=jdbc:h2:mem:testdb
spring.datasource.driverClassName=org.h2.Driver
spring.datasource.username=sa
spring.datasource.password=password

# JPA/Hibernate
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true

# H2 Console (for development)
spring.h2.console.enabled=true
spring.h2.console.path=/h2-console
```

## API Documentation

API documentation is available using Swagger/OpenAPI at:
- Swagger UI: `http://localhost:8080/swagger-ui.html`
- OpenAPI JSON: `http://localhost:8080/v3/api-docs`

## Contributing

1. Fork the repository
2. Create your feature branch (`git checkout -b feature/AmazingFeature`)
3. Commit your changes (`git commit -m 'Add some AmazingFeature'`)
4. Push to the branch (`git push origin feature/AmazingFeature`)
5. Open a Pull Request

## License

This project is licensed under the MIT License—see the [LICENSE](LICENSE) file for details.

## Acknowledgments

- Spring Boot Team
- Open Source Community