# AGENTS.md - Development Guidelines for LOGICOMA_NET Backend

## Overview

This is a Spring Boot 3.2.5 REST API backend using Java 17, MyBatis 3.0.3, and MySQL. This file provides guidelines for agents working in this codebase.

---

## Build & Test Commands

### Build

```bash
# Clean and build
mvn clean install

# Build without tests
mvn clean package -DskipTests
```

### Run

```bash
# Run application
mvn spring-boot:run

# Or run the JAR
java -jar target/logicoma-net-2.0.0.jar
```

### Test

```bash
# Run all tests
mvn test

# Run a single test class
mvn test -Dtest=UserServiceImplTest

# Run a single test method
mvn test -Dtest=UserServiceImplTest#testCreateUser
```

### Other

```bash
# Run linter (if available)
mvn checkstyle:check

# Generate Javadoc
mvn javadoc:javadoc
```

---

## Code Style Guidelines

### General Principles

- **Language**: Java 17
- **Framework**: Spring Boot 3.2.5
- **Build Tool**: Maven
- **Architecture**: REST API with Controller → Service → Mapper layers

### Project Structure

```
src/main/java/com/ai/repo/
├── LogicomaNetApplication.java  # Main entry point
├── common/                       # Shared utilities (Result, PageResult)
├── config/                       # Configuration classes
├── controller/                   # REST endpoints
├── dto/                          # Request/Response objects
├── entity/                       # Database entities
├── exception/                    # Exception handling
├── jwt/                          # JWT authentication
├── mapper/                       # MyBatis mappers
├── security/                    # Security annotations
├── service/                      # Business logic interfaces
├── service/impl/                # Business logic implementations
├── util/                        # Utility classes
└── aspect/                     # AOP aspects
```

### Naming Conventions

| Component | Convention | Example |
|---|---|---|
| Controllers | `{Entity}Controller` | `UserController.java` |
| Services | `{Entity}Service` | `UserService.java` |
| Service Impl | `{Entity}ServiceImpl` | `UserServiceImpl.java` |
| Mappers | `{Entity}Mapper` | `UserMapper.java` |
| Entities | Single noun, PascalCase | `User.java`, `Agent.java` |
| DTOs | `{Entity}{Request/Response}` | `UserCreateRequest.java` |
| Exceptions | `{Context}Exception` | `BusinessException.java` |

### Package Naming

- Use lowercase: `com.ai.repo.controller`
- No underscores in package names
- Group by layer (controller/, service/, entity/, etc.)

### Class Structure

```java
// Standard controller pattern
@RestController
@RequestMapping("/api/{resource}")
@Tag(name = "{Resource} API", description = "...")
public class {Entity}Controller {

    @Resource
    private {Entity}Service {entity}Service;

    @PostMapping
    @Operation(summary = "...", description = "...")
    public Result<Void> create(@RequestBody {Entity}CreateRequest request) {
        // implementation
        return Result.success();
    }
}
```

### Method Naming

| Operation | Method Name |
|---|---|
| Create | `create`, `createUser` |
| Update | `update`, `updateUser` |
| Delete | `delete`, `deleteById` |
| Get Single | `findById`, `findByUsername` |
| Get List | `findAll`, `findByStatus` |
| Custom Query | `search`, `filter`, `sync` |

### Import Order (Strict)

1. `java.*`
2. `javax.*`
3. `org.springframework.*`
4. `org.mybatis.*`
5. `com.ai.repo.*`
6. `com.fasterxml.jackson.*`
7. Other third-party

```java
import java.io.Serializable;
import java.util.List;

import org.springframework.web.bind.annotation.*;

import com.ai.repo.common.Result;
import com.ai.repo.entity.User;
import com.ai.repo.service.UserService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
```

### Field Naming in Entities

- Use meaningful names: `userId`, `createdAt`, `skillCount`
- Avoid abbreviations: `usr` → `user`
- Boolean: `isActive`, `hasPermission` (prefix `is`/`has`)
- Lists: `skills`, `users` (plural)

### Lombok Usage

```java
@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {
    private Long id;
    private String username;
    private String email;
    private String password;
    private String role;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
```

### Response Format

All API responses MUST use `Result<T>` wrapper:

```java
// Success with data
return Result.success(user);
return Result.success("User created", user);

// Success without data
return Result.success();

// Error
return Result.error(400, "Invalid request");
return Result.error(500, "Internal server error");
```

`Result<T>` structure:
```json
{
  "code": 200,
  "message": "Success",
  "data": { }
}
```

### Error Handling

- Use `BusinessException` for application errors
- Use `GlobalExceptionHandler` for centralized handling
- Return appropriate HTTP status codes in `Result.code`

```java
// In service layer
if (user == null) {
    throw new BusinessException(404, "User not found");
}
```

### Database Conventions

- Table names: lowercase, plural (`users`, `agents`)
- Column names: snake_case (`user_id`, `created_at`)
- Entity fields: camelCase (`userId`, `createdAt`)
- Use `@Mapper` annotation for MyBatis interfaces

### Security

- Use `@RequireAuth` annotation for protected endpoints
- JWT tokens validated via `JwtProvider`
- Passwords encoded with `PasswordEncoderUtil`

### Documentation

- Use OpenAPI annotations (`@Operation`, `@Parameter`, `@Tag`)
- Document all controller endpoints
- Keep `API_DOCUMENTATION.md` updated

---

## Testing Guidelines

There is currently no test directory. When adding tests:

```bash
# Test location
src/test/java/com/ai/repo/

# Test naming
{Entity}ServiceImplTest.java
{Entity}ControllerTest.java

# Use Spring Boot Test
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.SpringBootTest;
```

---

## Database Setup

```bash
# Create database
mysql -u root -p -e "CREATE DATABASE logicoma_net CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"

# Run migrations
mysql -u root -p logicoma_net < sql.txt
```

Configuration in `src/main/resources/application.yml`:

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/logicoma_net
    username: your_username
    password: your_password
```

---

## Common Patterns

### Pagination

Use `PageResult<T>` for paginated responses:

```java
@GET
public Result<PageResult<Agent>> getAgents(
    @RequestParam(defaultValue = "1") int page,
    @RequestParam(defaultValue = "10") int size
) {
    List<Agent> list = agentService.findPage(page, size);
    long total = agentService.count();
    return Result.success(new PageResult<>(list, page, size, total));
}
```

### Validation

Use Jakarta Validation annotations in DTOs:

```java
public class UserCreateRequest {
    @NotBlank
    @Size(min = 3, max = 50)
    private String username;

    @NotBlank
    @Email
    private String email;

    @NotBlank
    @Size(min = 6)
    private String password;
}
```

### Transaction Management

```java
@Service
public class UserServiceImpl {

    @Transactional
    public User create(User user) {
        // ...
    }
}
```

---

## Additional Notes

- All timestamps in UTC
- IDs are auto-incremented Long
- Foreign key CASCADE deletes enabled
- Swagger UI available at `/swagger-ui.html`
- Actuator health at `/actuator/health`