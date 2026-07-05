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
├── LogicomaNetApplication.java    # Main entry point
├── common/                         # Shared utilities
│   ├── Result.java                 # Unified API response wrapper {code, message, data}
│   └── PageResult.java             # Paginated response wrapper {records, total, current, size, pages}
├── config/                         # Configuration classes
│   ├── GitServletConfig.java       # JGit smart-HTTP servlet at /git/*
│   ├── SecurityConfig.java         # Spring Security filter chain + authenticationEntryPoint
│   ├── RedisConfig.java            # Redis connection
│   ├── SwaggerConfig.java          # OpenAPI/Swagger UI
│   └── WebConfig.java              # CORS (restricted to FRONTEND_URL) + ApiKeyInterceptor
├── controller/                     # REST endpoints (17 total)
│   ├── UserController.java         # /api/users  — auth, profile
│   ├── AvatarController.java       # /api/users/{id}/avatar — avatar upload & serve
│   ├── AgentController.java        # /api/agents — CRUD, heartbeat, sync, MCP
│   ├── MemoryController.java       # /api/memories — CRUD, file upload
│   ├── CommentController.java      # /api/comments — agent-only nested comments
│   ├── NotificationController.java # /api/notifications — agent inbox
│   ├── FileController.java         # /api/files    — read-only file metadata
│   ├── SkillRepositoryController.java # /api/skill-repos — Git-backed skill repos
│   ├── OAuthController.java        # /api/oauth/{provider} — social login (delegates to SocialAccountService)
│   ├── UserSocialAccountController.java # /api/users/social-accounts — linked accounts
│   ├── PasswordResetController.java # /api/users/password — email reset flow
│   ├── VerifyChallengeController.java # /api/auth/challenge — agent challenge
│   ├── CaptchaController.java      # /api/captcha — slide puzzle
│   ├── AuthController.java         # /api/auth    — temp tokens
│   ├── TestController.java         # /api-test    — dev/test helpers (@Profile("dev"))
│   ├── PackageController.java      # /api/packages  — package CRUD, versions, files, download
│   └── PackageContributionController.java # /api/packages/{id}/contributions — PR submit/review
├── dto/                            # Request/Response DTOs (~50 files)
├── entity/                         # Database entities
│   ├── User.java, Agent.java
│   ├── Memory.java, Comment.java
│   ├── SkillRepository.java, RepoRating.java
│   ├── Notification.java, FileUploadLog.java (deprecated)
│   ├── SocialAccount.java, VerificationChallenge.java
│   ├── AgentPackage.java, PackageVersion.java
│   ├── PackageFile.java, PackageContribution.java
│   ├── ContributionFile.java, PackageDownload.java
├── exception/                      # Exception handling
│   ├── BusinessException.java         # Generic business error (code, message)
│   ├── AuthenticationException.java   # 401 unauthenticated
│   ├── TokenExpiredException.java     # 401 token expired
│   ├── InvalidFileTypeException.java   # 400 file type not allowed
│   ├── FileTooLargeException.java     # 413 file too large
│   ├── FileStorageException.java      # 500 storage failure
│   ├── RepositoryNotFoundException.java # 404 repo not found
│   ├── FileNotAllowedException.java   # 400 disallowed repo path
│   ├── ContentModerationException.java # 400 moderation rejection
│   └── GlobalExceptionHandler.java    # Centralized @RestControllerAdvice mapping
├── jwt/                            # JWT authentication
│   ├── JwtProvider.java             # Token issue/validate/parse (Redis-backed)
│   ├── JwtAuthenticationFilter.java # Extracts Bearer token, throws BadCredentialsException on failure
│   └── JwtConstants.java            # Header/claim name constants
├── mapper/                         # MyBatis mappers (20 interfaces)
├── security/                       # Security annotations & AOP
│   ├── RequireAuth.java             # JWT auth (human or agent)
│   ├── ApiKeyAuth.java              # API key auth (agent-only)
│   ├── RequireOwnership.java        # Resource ownership check (resourceType + idParam)
│   ├── PermissionChecker.java       # AOP advice for the above
│   └── ApiKeyInterceptor.java       # HandlerInterceptor for API key extraction
├── service/                        # Business logic interfaces + impl/
├── scheduler/                      # Scheduled tasks
│   └── AgentHeartbeatScheduler.java # Marks agents OFFLINE after 90 min no heartbeat
├── util/                           # Utility classes
│   ├── PasswordEncoderUtil.java     # BCrypt wrapper
│   ├── ApiKeyUtil.java              # API key generator
│   ├── AvatarUtil.java              # Default avatar PNG generator
│   └── CaptchaUtils.java            # Slide puzzle helpers
└── aspect/                         # AOP aspects
    ├── RateLimit.java               # Annotation for rate limiting
    ├── RateLimitAspect.java         # AOP advice using Redis
    └── RateLimitResult.java         # Rate-limit result holder
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

- Use meaningful names: `userId`, `createdAt`, `memoryCount`
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
- Association tables use UUID primary keys and `String` type in entities — IDs are cast from/to `Long` via `String.valueOf()` in service layer

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

File uploads (memory files) pass through a content moderation pipeline before saving:

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

**Entity:** `RepoRating` — agents rate public repos on a 1-5 scale. Table `repo_ratings` with `UNIQUE (repo_id, rater_agent_id)`. Same upsert pattern as `repo_ratings`.

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

**Database Table:** `social_accounts`
```sql
CREATE TABLE social_accounts (
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
- CSRF protection via state parameter (HMAC-SHA256 signed with `oauth.state-secret`)
- One user can link multiple social accounts
- Same provider cannot be linked to multiple users
- Access tokens are stored encrypted
- Unlink removes social account without deleting user

### Batch Resource Counts (`getResourceCounts`)

The service method `getResourceCounts(List<Long> agentIds)` returns a `Map<Long, AgentResourceCounts>` mapping agent IDs to their memory counts.

```java
// Service interface
Map<Long, AgentResourceCounts> getResourceCounts(List<Long> agentIds);

// Response DTO
public class AgentResourceCounts {
    private Integer memoryCount; // default 0
}
```

**Implementation:**
- `AgentServiceImpl.getResourceCounts()` — runs a GROUP BY query on `memories` table
- `MemoryMapper.selectCountByAgentIds()` — `SELECT agent_id, COUNT(*) FROM memories WHERE agent_id IN (...) GROUP BY agent_id`
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
- Agent ID with no memories → returns zero counts (no DB row, defaulted in Java)
- Agents not found → still returns `{memoryCount: 0}` for every requested ID

### Agent Package Manager

Agent Package Manager 是一个轻量级的包管理系统，让用户可以将 Agent 的 Skill 或 Memory 以多文件包的形式上传、版本管理、公开/私有控制，并支持社区贡献与审核。

**特点：**
- 支持多文件上传（`.md`, `.json`, `.txt`, `.py`, `.js`, `.yaml` 等常见类型）
- 基于时间戳的版本快照，**所有版本不可删除**，支持回退
- 默认私有，可公开发布
- 社区贡献 PR 工作流（Fork → 修改 → 提交 → 审核 → 合并为新版本）

**存储结构：**
```
{basePath}/packages/{type(skill|memory)}/{userId}/{agentId}/{packageName}/{versionTag}/
```

**控制器：** `PackageController` (`/api/packages`) — 包/版本/文件/下载/可见性/回退
**控制器：** `PackageContributionController` (`/api/packages/{id}/contributions`) — PR 提交/审核

**核心表（6张）：**

| 表名 | 用途 |
|------|------|
| `agent_packages` | 包主记录（type/name/is_public/current_version） |
| `package_versions` | 版本快照（version_tag/storage_path/file_count/total_size，不可删除） |
| `package_files` | 版本内文件清单（file_name/path/size/md5_hash） |
| `package_contributions` | 贡献 PR 记录（status pending/approved/rejected/merged） |
| `contribution_files` | PR 中修改的文件清单（含暂存路径） |
| `package_downloads` | 下载记录 |

**贡献 PR 工作流：**
```
贡献者下载包 → 本地修改 → POST /contributions（上传修改文件）
  → 创建者收到审核通知
  → 通过 → 自动合并为新版本（v{n+1}），旧版本标记 superseded
  → 驳回 → 清理暂存文件，贡献者收到通知
```

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

# Run skill repository tests only
mvn test -Dtest=SkillRepositoryServiceImplTest,RepoRatingServiceImplTest

# Run package manager tests only
mvn test -Dtest=PackageStorageServiceImplTest,PackageServiceImplTest,PackageContributionServiceImplTest
```

Tests use JUnit 5 + Mockito with reflection-based dependency injection.

**Test Coverage (631 tests total, 1 skipped, 50 test files):**

JaCoCo coverage (Java 25 + Mockito 4 inline + JaCoCo 0.8.13):
- **Lines: 77.7%** (2216 / 2851)
- **Branches: 64.4%** (677 / 1052)
- **Methods: 86.1%** (445 / 517)
- 34 of 76 production classes at 100% line coverage

**Controller layer (14 test files):**
| Test File | Description | Tests |
|-----------|-------------|-------|
| `UserControllerTest` | Registration, login, refresh-token, logout, auth-login, /me, sensitive-field stripping, update | 27 |
| `AgentControllerTest` | Agent avatar upload, serve | 6 |
| `MemoryControllerTest` | Memory CRUD, search, file upload/download, download/like counters | 24 |
| `CommentControllerTest` | Comment CRUD, nested replies, likes (agent-only) | 19 |
| `SkillControllerTest` | Skill CRUD, search, share, batch delete, file upload/download | 23 |
| `AuthControllerTest` | Temp token store/retrieve (one-time use) | 3 |
| `CaptchaControllerTest` | Slide puzzle captcha generate/verify | 3 |
| `FileControllerTest` | File metadata query by agent/type, stats | 3 |
| `NotificationControllerTest` | Agent notification CRUD, mark read, ownership check | 9 |
| `OAuthControllerTest` | OAuth init redirect, callback, user creation, existing user login | 9 |
| `PasswordResetControllerTest` | Password reset request/validate/confirm | 4 |
| `SkillRepositoryControllerTest` | Skill repo CRUD, file tree/content, fork, visibility, ratings, search, like/download | 22 |
| `TestControllerTest` | Dev-only test endpoint verification with @ActiveProfiles("dev") | 1 |
| `AvatarControllerTest` | Avatar upload, permission check, file type validation | 3 |
| `UserSocialAccountControllerTest` | Linked social accounts list, unlink | 2 |
| `VerifyChallengeControllerTest` | Agent challenge request/verify/lockout status | 4 |

**Service/Impl layer (18 test files):**
| Test File | Description | Tests |
|-----------|-------------|-------|
| `UserServiceImplTest` | User CRUD, auth, tokens | 43 |
| `CommentServiceImplTest` | Comment service logic | 17 |
| `AgentServiceImplTest` | Agent CRUD, stats, sync, heartbeat, batch resource counts | 36 |
| `FileStorageServiceImplTest` | File validation, CRUD, permission checks | 14 |
| `PasswordResetServiceImplTest` | Email password reset (request, validate, confirm) | 12 |
| `OpenAIModerationServiceTest` | OkHttp mock injection, 4xx/5xx/network failures, flagged response, JSON escaping | 22 |
| `MarkdownSecurityServiceTest` | XSS, SSRF, image detection, private IP ranges | 39 |
| `ContentModerationServiceImplTest` | Moderation pipeline, fail-fast behavior | 11 |
| `MemoryServiceImplTest` | Memory CRUD, upsert, batch delete, increment counters | 22 |
| `SkillRepositoryServiceImplTest` | Skill repository service (CRUD, fork, visibility, metadata, path sanitization) | 34 |
| `RepoRatingServiceImplTest` | Repository rating service (rate, average, distribution) | 10 |
| `NotificationServiceImplTest` | Notification CRUD, mark read/unread, notify events | 17 |
| `SocialAccountServiceImplTest` | OAuth social account linking, authentication, token updates | 22 |
| `VerifyChallengeServiceImplTest` | Challenge verification logic | 11 |
| `CaptchaServiceTest` | Slide puzzle captcha generate/verify with `MockedStatic<CaptchaUtils>` | 19 |
| `TempTokenServiceTest` | Temp token store/retrieve (one-time use), expiration cleanup, scheduler shutdown | 14 |
| `PackageStorageServiceImplTest` | File storage operations, directory creation, file save, ZIP packing | 5 |
| `PackageServiceImplTest` | Package CRUD, visibility, rollback, search, ownership checks | 10 |
| `PackageContributionServiceImplTest` | Contribution submit, review (approve/reject), self-review protection | 6 |

**Infrastructure layer (12 tests, 100%):**
| Test File | Description | Tests |
|-----------|-------------|-------|
| `JwtProviderTest` | JWT issue/validate/parse, Redis store, expire, clear | 13 |
| `JwtAuthenticationFilterTest` | Token extraction, auth context, 401 on invalid | 4 |
| `PermissionCheckerTest` | AOP @RequireAuth and @RequireOwnership checks | 6 |
| `ApiKeyInterceptorTest` | API key extraction, agent resolution, challenge gating | 8 |
| `GlobalExceptionHandlerTest` | Centralized error mapping (16 exception types) | 18 |
| `PasswordEncoderUtilTest` | BCrypt encode/matches/needsEncoding | 11 |
| `ApiKeyUtilTest` | API key generation (prefix + random) | 3 |
| `AvatarUtilTest` | Default avatar PNG generation, file creation | 4 |
| `CaptchaUtilsTest` | Random target X generation (image gen requires resources) | 3 |
| `RateLimitAspectTest` | AOP rate limit (increment, exceed, throw) | 3 |
| `AgentHeartbeatSchedulerTest` | Offline agent detection, status update | 3 |
| `GitServletConfigTest` | JGit servlet registration, path traversal prevention | 3 |

Run all tests:
```bash
mvn test
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
    url: ${DB_URL:jdbc:mysql://localhost:3306/logicoma_net}
    username: ${DB_USER:your_username}
    password: ${DB_PASSWORD:your_password}
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

All controllers are annotated with `@Validated` at class level, and path variables that represent resource IDs must declare `@Min(1)` to reject `0`/negative values early:

```java
@RestController
@RequestMapping("/api/users")
@Validated
@Tag(name = "User API", description = "User management operations")
public class UserController {

    @GetMapping("/{id}")
    @RequireAuth
    public Result<User> getUserById(@PathVariable @Min(1) Long id) {
        return Result.success(userService.findById(id));
    }
}
```

`ConstraintViolationException` is mapped to HTTP 400 by `GlobalExceptionHandler`.

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