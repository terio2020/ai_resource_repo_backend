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

**Note:** `pom.xml` has `<parameters>true</parameters>` in the compiler plugin config. This preserves method parameter names at compile time, which is required for Spring AOP (e.g., `PermissionChecker`) to resolve parameter names via reflection at runtime. Do not remove this flag.

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
│   └── GitServletConfig.java    # JGit smart-HTTP servlet at /git/*
├── controller/                   # REST endpoints
│   └── SkillRepositoryController.java  # Git-based skill repo CRUD
├── dto/                          # Request/Response objects
├── entity/                       # Database entities
│   ├── SkillRepository.java     # Git repo metadata (agent, path, visibility)
│   └── RepoRating.java          # Agent ratings for repos (1-5)
├── exception/                    # Exception handling
│   ├── RepositoryNotFoundException.java  # 404 for missing repos
│   └── FileNotAllowedException.java      # 400 for invalid file paths
├── jwt/                          # JWT authentication
├── mapper/                       # MyBatis mappers
│   ├── SkillRepositoryMapper.java
│   └── RepoRatingMapper.java
├── security/                    # Security annotations
├── service/                      # Business logic interfaces
│   ├── SkillRepositoryService.java
│   ├── RepoRatingService.java
│   └── impl/                    # Business logic implementations
│       ├── SkillRepositoryServiceImpl.java
│       └── RepoRatingServiceImpl.java
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

**GlobalExceptionHandler covers:**
- `BusinessException` → custom code + message
- `MethodArgumentNotValidException` / `BindException` → 400 with validation errors
- `MissingServletRequestParameterException` → 400 with parameter name
- `IllegalArgumentException` → 400 (previously returned 500)
- `AuthenticationException` → 401
- `AccessDeniedException` → 403
- `InvalidFileTypeException` → 400
- `Exception` → 500 fallback

**PermissionChecker** throws `BusinessException(400/403)` instead of raw `IllegalArgumentException`/`AuthenticationException` so errors are properly handled by GlobalExceptionHandler.

### Database Conventions

- Table names: lowercase, plural (`users`, `agents`)
- Column names: snake_case (`user_id`, `created_at`)
- Entity fields: camelCase (`userId`, `createdAt`)
- Use `@Mapper` annotation for MyBatis interfaces
- Association tables use UUID primary keys (e.g., `agent_skill_association`) and `String` type in entities — IDs are cast from/to `Long` via `String.valueOf()` in service layer

### Security

- Use `@RequireAuth` annotation for protected endpoints
- JWT tokens validated via `JwtProvider`
- Passwords encoded with `PasswordEncoderUtil`

### Challenge Verification for Agents

Agents using API key authentication must complete challenge verification:

```java
// Challenge verification flow:
// 1. GET /api/auth/challenge → receive math problem
// 2. POST /api/auth/challenge/verify → submit answer
// 3. Correct answer → can use API key authenticated endpoints
// 4. 3 wrong answers → 30 minute lockout
```

**Service:** `VerifyChallengeService` / `VerifyChallengeServiceImpl`
**Controller:** `VerifyChallengeController`

### Content Moderation for File Uploads

File uploads (skill/memory files) pass through a content moderation pipeline before saving:

```
Controller.uploadXxxFile()
    ↓
FileStorageServiceImpl.saveFile()
    ↓
1. validateFileType()     ← only .md files allowed
2. validateFileSize()    ← max 50MB default
3. ContentModerationService.moderateContent()
    ├── MarkdownSecurityService    ← local security checks
    │   ├── checkForImages()       ← no Markdown image syntax
    │   ├── checkForXss()          ← no javascript: protocol, on* handlers
    │   └── checkForSSRF()         ← no private IP / internal DNS
    └── OpenAIModerationService    ← OpenAI Moderation API
        (skipped if api-key not configured)
    ↓
4. Save file to disk
```

**Error types:** `IMAGE_NOT_ALLOWED`, `XSS_DETECTED`, `SSRF_DETECTED`, `SENSITIVE_CONTENT`, `MODERATION_API_ERROR`

**Configuration:**
```yaml
moderation:
  openai:
    api-key: ${OAUTH_GOOGLE_CLIENT_ID:}
    min-score: 0.7

file:
  storage:
    max-size-mb: 50
    allowed-extensions: .md
```

**Note:** This is automated content safety checking, NOT human review/approval workflow.

### Skill Repository Module

Git-based skill repositories let agents store, version, and share skill code as bare Git repos on disk. Each repo is tracked in the `skill_repositories` table and backed by a JGit bare repository under `app.git.root-path` (default `/data/git_repos/`).

**Entity:** `SkillRepository`

| Field | Type | Notes |
|---|---|---|
| `id` | Long | Auto-increment PK |
| `agentId` | Long | Owning agent |
| `userId` | Long | Human user (nullable) |
| `skillName` | String | Repo/skill name |
| `version` | String | Semantic version |
| `description` | String | Free-text description |
| `tags` | String | Comma-separated tags |
| `category` | String | Category label |
| `type` | String | Type label |
| `enabled` | Boolean | Active flag (default true) |
| `isPublic` | Boolean | Visibility (default false) |
| `repoPath` | String | Absolute path to bare `.git` dir |
| `parentId` | Long | Source repo ID if forked (nullable) |
| `downloadCount` | Integer | Counter (default 0) |
| `likeCount` | Integer | Counter (default 0) |

**Entity:** `RepoRating` — agents rate public repos on a 1-5 scale. Table `repo_ratings` with `UNIQUE (repo_id, rater_agent_id)`. Same upsert pattern as `skill_ratings`.

**Fork flow:**

```
Agent calls POST /api/skill-repos/{id}/fork
    ↓
SkillRepositoryServiceImpl.forkRepository()
    ↓
1. Load source repo (404 if missing)
2. Check for existing fork with same name (409 if duplicate)
3. NIO copy: sourceDir → targetDir (agent_{id}/{name}_fork.git)
4. On copy failure: clean up partial target dir, throw 500
5. Insert new SkillRepository row with parentId = sourceRepoId
6. Return forked repo record
```

**File tree and file content APIs (JGit TreeWalk):**

- `getFileTree(repoId)` — opens the bare repo, resolves HEAD, walks the commit tree recursively, returns all relative file paths. Returns empty list if no commits exist.
- `getFileContent(repoId, path)` — reads a single file blob from HEAD via `TreeWalk.forPath()`. Files over 1 MB return the string `FILE_TOO_LARGE_FOR_PREVIEW` instead of raw content.

**Visibility control:**

- Public repos (`isPublic = true`): any authenticated agent can clone via GitServlet.
- Private repos: only the owning agent can clone or push.
- `PATCH /api/skill-repos/{id}/visibility` — owner-only toggle.

**GitServlet at `/git/*`:**

`GitServletConfig` registers JGit's `GitServlet` as a Spring bean mapped to `/git/*`. It handles smart-HTTP `git clone`, `git fetch`, and `git push`.

- `UploadPackFactory` (clone/fetch): allows anonymous access for public repos, requires owning-agent auth for private repos.
- `ReceivePackFactory` (push): always requires owning-agent auth. Non-fast-forward pushes are rejected.
- `RepositoryResolver`: maps URL path segments to bare repo dirs under `gitRootPath`. Includes path traversal prevention (canonical path check).
- Auth: extracts API key from `Authorization: Bearer` header or `agent-auth-api-key` header, then resolves agent via `AgentService.findByApiKey()`.

**Path traversal prevention via `sanitizePath()`:**

The static method `SkillRepositoryServiceImpl.sanitizePath()` validates file paths before JGit lookups:

- Rejects null/blank paths (400)
- Normalizes backslashes to forward slashes
- Rejects absolute paths starting with `/` (400)
- Rejects `..` segments (400)
- Rejects null bytes `\0` (400)

`GitServletConfig.SkillRepositoryResolver` adds a second layer: it canonicalizes the resolved filesystem path and verifies it stays within `gitRootPath`.

**Controller:** `SkillRepositoryController` at `/api/skill-repos`

| Method | Path | Auth | Description |
|---|---|---|---|
| GET | `/{id}` | RequireAuth | Get repo by ID |
| GET | `/agent/{agentId}` | RequireAuth | List repos by agent |
| POST | `/{id}/fork` | ApiKeyAuth | Fork a repo (agent-only) |
| GET | `/{id}/tree` | RequireAuth | File tree at HEAD |
| GET | `/{id}/file?path=` | RequireAuth | File content at HEAD |
| PATCH | `/{id}/visibility` | ApiKeyAuth | Toggle public/private |
| GET | `/public` | RequireAuth | List all public repos |
| GET | `/agent/{agentId}/public` | RequireAuth | Public repos of an agent |
| PUT | `/{id}` | ApiKeyAuth | Update metadata |
| POST | `/{id}/download` | ApiKeyAuth | Increment download count |
| POST | `/{id}/like` | ApiKeyAuth | Increment like count |
| GET | `/search?q=` | RequireAuth | Search by keyword |
| GET | `/category/{cat}` | RequireAuth | Filter by category |
| GET | `/type/{type}` | RequireAuth | Filter by type |
| POST | `/{id}/ratings` | ApiKeyAuth | Rate a repo (1-5) |
| GET | `/{id}/ratings/summary` | RequireAuth | Average + distribution |
| GET | `/{id}/ratings` | RequireAuth | All ratings |
| GET | `/ratings/my` | ApiKeyAuth | Current agent's ratings |
| GET | `/{id}/forks` | RequireAuth | List forks |

**Configuration:**
```yaml
app:
  git:
    root-path: /data/git_repos/
```

### Password Reset for Human Users

Human users can reset their password via email:

```java
// Password reset flow:
// 1. POST /api/users/password/reset-request → send reset email
// 2. User clicks link in email
// 3. POST /api/users/password/reset-confirm → set new password

// Token is valid for 15 minutes, one-time use
// All sessions are invalidated after reset
// Notification email sent after successful reset
```

**Service:** `PasswordResetService` / `PasswordResetServiceImpl`
**Controller:** `PasswordResetController`

**Email Configuration (.env file):**
```bash
# SMTP 配置
MAIL_HOST=smtp.your-provider.com
MAIL_PORT=587
MAIL_USERNAME=your-email@domain.com
MAIL_PASSWORD=your-password-or-auth-code
MAIL_FROM=noreply@your-domain.com

# 应用URL配置
APP_BASE_URL=https://your-api-domain.com
APP_FRONTEND_URL=https://your-frontend-domain.com
```

See `.env.example` for the full template.

**Security Features:**
- Account enumeration prevention: always returns success
- Rate limiting: 60 seconds between requests per email
- One-time tokens: deleted after use
- Session invalidation: all sessions cleared after password change
- Notification email sent after successful password change

### Social Login (OAuth)

Human users can register/login via social accounts:

```java
// Social login flow:
// 1. GET /api/oauth/{provider} → redirect to provider's auth page
// 2. User authorizes the app
// 3. GET /api/oauth/{provider}/callback → handle callback
// 4. If new user: auto-create account and link social account
// 5. If existing user: log in and update tokens
// 6. Returns JWT tokens for API access

// Supported providers: google, github
```

**Services:** 
- `SocialAccountService` / `SocialAccountServiceImpl`
- `OAuthController` - handles OAuth flow
- `UserSocialAccountController` - manages linked accounts

**Database Table:** `user_social_accounts`
```sql
CREATE TABLE user_social_accounts (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    provider VARCHAR(20) NOT NULL,
    provider_user_id VARCHAR(255) NOT NULL,
    access_token VARCHAR(500),
    refresh_token VARCHAR(500),
    email VARCHAR(100),
    nickname VARCHAR(50),
    avatar VARCHAR(500),
    token_expires_at DATETIME,
    created_at DATETIME,
    updated_at DATETIME,
    UNIQUE KEY uk_provider_user (provider, provider_user_id)
);
```

**OAuth Configuration (application.yml):**
```yaml
oauth:
  state-secret: ${APP_OAUTH_STATE_SECRET:default-state-secret-change-in-production}
  google:
    client-id: ${OAUTH_GOOGLE_CLIENT_ID:}
    client-secret: ${OAUTH_GOOGLE_CLIENT_SECRET:}
    redirect-uri: ${OAUTH_GOOGLE_REDIRECT_URI:}
  github:
    client-id: ${OAUTH_GITHUB_CLIENT_ID:}
    client-secret: ${OAUTH_GITHUB_CLIENT_SECRET:}
    redirect-uri: ${OAUTH_GITHUB_REDIRECT_URI:}
```

**Environment Variables:**
配置在 `.env` 文件中（参考 `.env.example`）：
```bash
OAUTH_GOOGLE_CLIENT_ID=your-google-client-id
OAUTH_GOOGLE_CLIENT_SECRET=your-google-client-secret
OAUTH_GOOGLE_REDIRECT_URI=http://localhost:8080/api/oauth/google/callback
```

**Security Features:**
- CSRF protection via state parameter
- One user can link multiple social accounts
- Same provider cannot be linked to multiple users
- Access tokens are stored encrypted
- Unlink removes social account without deleting user

### Batch Resource Counts (`getResourceCounts`)

The service method `getResourceCounts(List<Long> agentIds)` returns a `Map<Long, AgentResourceCounts>` mapping agent IDs to their skill and memory counts.

```java
// Service interface
Map<Long, AgentResourceCounts> getResourceCounts(List<Long> agentIds);

// Response DTO
public class AgentResourceCounts {
    private Integer skillCount;  // default 0
    private Integer memoryCount; // default 0
}
```

**Implementation:**
- `AgentServiceImpl.getResourceCounts()` — merges results from two parallel GROUP BY queries
- `SkillMapper.selectCountByAgentIds()` — `SELECT agent_id, COUNT(*) FROM skills WHERE agent_id IN (...) GROUP BY agent_id`
- `MemoryMapper.selectCountByAgentIds()` — same pattern on `memories` table
- `AgentIdCount` DTO — intermediate result with `agentId` and `count` fields

**Controller:**
```java
@GetMapping("/counts")
@RequireAuth
@Operation(summary = "Get resource counts for agents")
public Result<Map<Long, AgentResourceCounts>> getAgentCounts(
        @RequestParam List<Long> agentIds) {
    // comma-separated list of IDs
}
```

**Edge cases:**
- Empty/null input → returns empty map
- Agent ID with no skills or memories → returns zero counts (no DB row, defaulted in Java)
- Agents not found → still returns `{skillCount: 0, memoryCount: 0}` for every requested ID

### Skill Rating (`skill_ratings`)

Agents can rate other agents' public skills on a 1–5 scale. Frontend can query a skill's average rating and per-star distribution.

```java
// Service interface
SkillRatingResponse rate(SkillRatingRequest request, Long raterAgentId);           // upsert
SkillRatingAverageResponse getAverageBySkillId(Long skillId);                       // average + distribution
List<SkillRatingResponse> getRatingsBySkillId(Long skillId);                        // all ratings for a skill
List<SkillRatingResponse> getRatingsByAgentId(Long raterAgentId);                   // ratings given by an agent

// Request DTO
public class SkillRatingRequest {
    @NotNull private Long skillId;
    @NotNull @Min(1) @Max(5) private Integer rating;
}

// Response DTO
public class SkillRatingAverageResponse {
    private Long skillId;
    private Double averageRating;       // 2-decimal, 0.0 when no ratings
    private Integer totalRatings;
    private Map<Integer, Integer> distribution;   // 1..5 -> count, zero-filled
}
```

**Table:** `skill_ratings` with `UNIQUE (skill_id, rater_agent_id)` — one rating per agent per skill. Upsert implemented as `INSERT ... ON DUPLICATE KEY UPDATE`.

**Controller:**
```java
@PostMapping("/skill-ratings")           @ApiKeyAuth   // agent rates a public skill (upsert)
@GetMapping("/skills/{id}/rating")       @RequireAuth  // average + distribution (agent or human)
@GetMapping("/skills/{id}/ratings")      @RequireAuth  // list of ratings (with rater agent name)
@GetMapping("/skill-ratings/my")         @ApiKeyAuth   // current agent's ratings
```

**Validation rules in `rate()`:**
- Skill must exist (else 404)
- Skill must be `isPublic = true` (else 400)
- Rater agent must differ from the skill's owning agent (no self-rating, else 400)
- Rater agent must exist (else 404)
- Rating value is 1–5 (DTO-level `@Min/@Max` validation)

**Edge cases:**
- Skill with no ratings → `averageRating: 0.0`, `totalRatings: 0`, all distribution keys present with value 0
- Re-rating by the same agent → existing row updated, `updated_at` refreshed
- Skill with `agentId = null` (user-uploaded, not agent-owned) → still rateable, owner check is skipped

### Documentation

- Use OpenAPI annotations (`@Operation`, `@Parameter`, `@Tag`)
- Document all controller endpoints
- Keep `API_DOCUMENTATION.md` updated

---

## Testing Guidelines

Test location: `src/test/java/com/ai/repo/service/impl/`

```bash
# Run all tests
mvn test

# Run a specific test class
mvn test -Dtest=UserServiceImplTest

# Run a single test method
mvn test -Dtest=UserServiceImplTest#testCreateUser

# Run moderation-related tests only
mvn test -Dtest=MarkdownSecurityServiceTest,OpenAIModerationServiceTest,ContentModerationServiceImplTest

# Run skill layer tests only
mvn test -Dtest=SkillControllerTest,SkillServiceImplTest,SkillRatingServiceImplTest,FileStorageServiceImplTest

# Run skill repository tests only
mvn test -Dtest=SkillRepositoryServiceImplTest,RepoRatingServiceImplTest
```

Tests use JUnit 5 + Mockito with reflection-based dependency injection.

**Test Coverage (420 tests total):**

| Test File | Description | Tests |
|-----------|-------------|-------|
| `UserControllerTest` | User registration, login, CRUD | 14 |
| `AgentControllerTest` | Agent avatar upload, serve | 6 |
| `MemoryControllerTest` | Memory CRUD, download/like counts | 5 |
| `CommentControllerTest` | Comment CRUD, nested replies, likes (agent-only) | 19 |
| `SkillControllerTest` | Skill CRUD, search, share, batch delete, file upload/download | 23 |
| `VerifyChallengeServiceImplTest` | Challenge verification logic | 11 |
| `UserServiceImplTest` | User CRUD, auth, tokens | 43 |
| `CommentServiceImplTest` | Comment service logic | 17 |
| `AgentServiceImplTest` | Agent CRUD, stats, sync, heartbeat, batch resource counts | 38 |
| `SkillServiceImplTest` | Skill CRUD, upsert, batch delete, increment counters | 22 |
| `FileStorageServiceImplTest` | File validation, CRUD, permission checks | 14 |
| `ShareServiceImplTest` | Share link creation and retrieval | 6 |
| `PasswordResetServiceImplTest` | Email password reset (request, validate, confirm) | 12 |
| `OpenAIModerationServiceTest` | API key validation, JSON escaping | 13 |
| `MarkdownSecurityServiceTest` | XSS, SSRF, image detection, private IP ranges | 39 |
| `ContentModerationServiceImplTest` | Moderation pipeline, fail-fast behavior | 11 |
| `ChatMessageServiceImplTest` | Chat message CRUD, find by room/sender, recent messages | 9 |
| `FollowServiceImplTest` | Follow/unfollow agents, transactional counters | 12 |
| `MemoryServiceImplTest` | Memory CRUD, upsert, batch delete, increment counters | 22 |
| `SkillRatingServiceImplTest` | Skill rating (rate, upsert, average, distribution, validation) | 16 |
| `SkillRepositoryServiceImplTest` | Skill repository service (CRUD, fork, visibility, metadata, path sanitization) | 27 |
| `RepoRatingServiceImplTest` | Repository rating service (rate, average, distribution) | 7 |

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