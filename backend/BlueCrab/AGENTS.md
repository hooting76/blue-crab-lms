# Blue Crab Backend Agent Guide

> **Created**: 2025-10-21
> **Audience**: Backend Agent
> **Scope**: `backend/BlueCrab/` Spring Boot + Maven Module

---

## Project Overview

**Blue Crab LMS (Learning Management System)**

This is a **portfolio project** developing a comprehensive LMS platform for a fictional university called "Blue Crab University". The system provides:

- **Lecture Management**: Course creation, assignment submission, attendance tracking, grade calculation
- **Facility Reservation**: Reading room and facility booking system
- **User Management**: Authentication, authorization, and profile management
- **Push Notifications**: Firebase-based real-time notifications
- **Admin Features**: System administration and monitoring tools

**Project Context:**
- **Purpose**: Portfolio demonstration project
- **Scope**: Full-stack web application (Backend + Frontend)
- **Tech Philosophy**: Production-quality code with modern best practices
- **Standards**: Korean e-Government Framework (eGovFrame) compliance
- **Target Audience**: Educational institutions, portfolio reviewers, potential employers

**Quality Expectations:**
- Clean, maintainable code suitable for portfolio presentation
- Comprehensive documentation and testing
- Security best practices (JWT, BCrypt, secure credential management)
- Performance optimization and monitoring
- Professional API design and error handling

**Development Status:**
- Active development phase
- Backend: eGovFrame Boot 4.3.0 + Spring Boot
- Frontend: React-based SPA (separate codebase)
- Integration: RESTful APIs with JWT authentication

**Infrastructure Architecture:**

```
┌─────────────────────────────┐       ┌──────────────────────────────┐
│   Backend Server            │       │   Frontend Server            │
│                             │       │                              │
│  ┌──────────────────────┐   │       │  ┌───────────────────────┐   │
│  │   Apache Tomcat      │   │       │  │   Apache Web Server   │   │
│  │   (WAR Deployment)   │   │       │  │   (React SPA)         │   │
│  └──────────────────────┘   │       │  └───────────────────────┘   │
│                             │       │                              │
│  ┌──────────────────────┐   │       │  ┌───────────────────────┐   │
│  │   Redis              │   │       │  │   MariaDB             │   │
│  │   (Cache/Session)    │   │       │  │   (Primary DB)        │   │
│  └──────────────────────┘   │       │  └───────────────────────┘   │
│                             │       │                              │
│  ┌──────────────────────┐   │       └──────────────────────────────┘
│  │   MinIO              │   │
│  │   (Object Storage)   │   │              REST API
│  └──────────────────────┘   │       ◄─────────────────►
│                             │
└─────────────────────────────┘
```

**Key Infrastructure Details:**
- **Backend Server**: Hosts Tomcat (Spring Boot app), Redis, MinIO
- **Frontend Server**: Hosts Apache (React app), MariaDB database
- **Separation**: Backend and frontend are deployed on separate servers
- **Communication**: RESTful APIs over HTTP/HTTPS with JWT authentication

---

## 1. Onboarding Checklist

### Quick Start Steps
1. **Read Documentation First**
   - Review root `README.md` and `docs/agent-readme.md` to understand the overall repository context and common working principles
   - Familiarize yourself with `backend/BlueCrab/README.md` for environment variables, Firebase setup, and deployment workflows

2. **Check Git Status**
   - Run `git status` to check current branch status
   - Review `git diff` to avoid overwriting existing changes
   - Verify you're not on `main`/`master` branch directly

3. **Understand Tech Stack** (See Section 2 for details)
   - eGovFrame Boot 4.3.0 (Korean e-Government Framework)
   - MariaDB database
   - JWT authentication
   - Redis, Firebase, MinIO integrations

4. **Set Up Local Environment** (if needed for verification)
   ```bash
   # Verify Java version
   java -version  # Should be 11+ or 17+

   # Verify Maven
   mvn -version   # Should be 3.6+

   # Check database connectivity (optional)
   # See Section 9 for MCP SSH access to remote DB
   ```

5. **Environment Variables Reference**
   Key environment variables you may need to know about (DO NOT set these locally as agent):
   ```bash
   # Database (handled by dev team)
   SPRING_DATASOURCE_URL=jdbc:mariadb://121.165.24.26:55511/blue_crab
   SPRING_DATASOURCE_USERNAME=KDT_project
   SPRING_DATASOURCE_PASSWORD=<see_secure_storage>

   # Firebase (if enabled)
   FIREBASE_ENABLED=true
   FIREBASE_CREDENTIALS_JSON=<json_string_or_file_path>
   FIREBASE_DATABASE_URL=https://<project-id>.firebaseio.com

   # Redis (if needed)
   SPRING_REDIS_HOST=localhost
   SPRING_REDIS_PORT=6379

   # MinIO (if used)
   MINIO_ENDPOINT=http://localhost:9000
   MINIO_ACCESS_KEY=<access_key>
   MINIO_SECRET_KEY=<secret_key>
   ```
   **Note**: Actual credentials are managed by the dev team in Eclipse. Agents work with code only.

6. **Plan Your Work**
   - Define the scope and impact of the task
   - Establish a standard plan (≥2 steps)
   - Identify external resources needed (MariaDB, Firebase, etc.)
   - Prepare mock value strategies if live services are unavailable

## 2. Development Environment & Essential Tools

### Core Technology Stack
- **Framework**: eGovFrame Boot 4.3.0 (Korean e-Government Standard Framework based on Spring Boot)
- **JDK**: 11+ (per README) / 17+ recommended, `JAVA_HOME` must be set
- **Build Tool**: Maven 3.6+ (No wrapper provided → use local Maven)
- **Packaging**: WAR (for Tomcat deployment)

### Database & Storage
- **Primary Database**: MariaDB 3.1.4 (`blue_crab` instance, connection details in `backend/BlueCrab/README.md`)
- **Connection Pool**: HikariCP (max 20 connections)
- **ORM**: Spring Data JPA + Hibernate 5.6.15
- **Object Storage**: MinIO 8.5.2 (for file storage)

### Authentication & Security
- **Spring Security**: JWT-based authentication/authorization system
- **JWT Library**: jjwt 0.12.6 (Access token 15min, Refresh token 24h)
- **OAuth2**: Resource Server support
- **Password Encryption**: BCrypt

### External Service Integration
- **Redis**: Session management and caching (spring-boot-starter-data-redis)
- **Firebase Admin SDK 9.7.0**: FCM push notifications, Realtime DB integration
- **Spring Mail**: Email delivery functionality
- All external services inject credentials via environment variables; never commit JSON key files to the repository.

### Key Dependencies
- **Lombok 1.18.34**: Reduce boilerplate code
- **Thymeleaf**: Server-side template engine
- **Selenium 4.13.0**: E2E testing (test scope)
- **Apache HttpClient5**: Firebase SDK dependency

### Environment-Specific Notes
- TLS, MariaDB driver, and Korean locale issues may occur depending on the OS. Check logs during initial execution.
- For local testing, verify whether Mock/Memory DB usage is feasible.

## 3. Project Structure Overview

### Directory Structure
- `src/main/java/BlueCrab/com/example/` — Core business logic
  - `controller/` — REST API endpoints (@RestController)
  - `service/` — Business logic layer (@Service)
  - `repository/` — Data access layer (@Repository, JPA)
  - `entity/` — JPA entity classes
  - `dto/` — Data Transfer Objects (Request/Response)
  - `config/` — Configuration classes (Security, DataSource, WebMvc, etc.)
  - `security/` — JWT filters, UserDetailsService, etc.
  - `util/` — Utility classes (JwtUtil, RequestUtils, etc.)
  - `exception/` — Global exception handling (@ControllerAdvice)
  - `scheduler/` — Scheduled tasks (@Scheduled)
  - `listener/` — Event listeners (@EventListener)
- `src/main/resources/` — `application-*.properties`, Log4j2 config, etc.
- `src/test/java/` — Unit and integration tests. Enhance tests as new features are added.
- `docs/` — Architecture, API, operations guides. Update relevant docs when specs change.
- `api-endpoints-documentation.json` — Single source of REST spec. Sync with frontend API wrappers (`src/api/`) when endpoints are added/changed.

### Key Domain Modules
The project consists of the following major functional areas:

#### 1. Lecture Management (강의 관리)
- **Controllers**: `LectureController`, `AssignmentController`, `EnrollmentController`
- **Services**: `AssignmentService`, `EnrollmentService`, `AttendanceService`, `GradeCalculationService`, `GradeManagementService`
- **Features**: Course creation/management, assignment submission, enrollment, attendance, grade calculation/management
- **Events**: `GradeUpdateEventListener` - auto-processing on grade updates

#### 2. Reading Room Reservation System (열람실 예약)
- **Repositories**: `ReadingSeatRepository`, `ReadingUsageLogRepository`
- **Services**: `ReadingRoomNotificationFactory`
- **Schedulers**: `ReadingRoomPreExpiryNotifier` - expiry warning notifications
- **Features**: Seat reservation, usage log tracking, expiry alerts

#### 3. Facility Reservation (시설 예약)
- **Services**: `FacilityReservationService`, `AdminFacilityReservationService`
- **Features**: User reservations, admin reservation management

#### 4. Authentication & User Management (인증 & 사용자 관리)
- **Controllers**: `AuthController`, `UserController`
- **Services**: `AuthService`, `UserTblService`, `CustomUserDetailsService`
- **Utils**: `JwtUtil`
- **Features**: Login/logout, token refresh, user CRUD

#### 5. Push Notifications (푸시 알림)
- **Controllers**: `PushNotificationController`, `FcmTokenController`
- **Services**: `FcmTokenService`, Firebase Admin SDK integration
- **Repositories**: `FcmTokenRepository`
- **Features**: FCM token management, push notification delivery

### Shared Utilities & Components
Re-use the established helpers below before adding new infrastructure code:

**Response & Error Handling**
- `src/main/java/BlueCrab/com/example/dto/ApiResponse.java` — Standard success/error wrapper. Always return controllers with this helper instead of ad-hoc `Map`/`ResponseEntity`.
- `src/main/java/BlueCrab/com/example/exception/GlobalExceptionHandler.java` — Centralized exception mapping. Prefer throwing domain-specific exceptions (`DuplicateResourceException`, `ResourceNotFoundException`, etc.) and let the handler shape the response.

**Authentication & Verification**
- `src/main/java/BlueCrab/com/example/util/JwtUtil.java` — Token generation/validation. Relies on `AppConfig.jwt.*` values; do not cache the signing key yourself.
- `src/main/java/BlueCrab/com/example/util/UserVerificationUtils.java` — Common identity checks (학번·이름·전화). Use in account recovery flows to avoid duplicated repository queries.
- `src/main/java/BlueCrab/com/example/util/MailAuthRateLimitUtils.java` & `src/main/java/BlueCrab/com/example/util/PasswordResetRedisUtil.java` — Redis-backed throttling and recovery session helpers. Require `RedisConfig` and running Redis; guard calls in tests with Redis availability checks.
- `src/main/java/BlueCrab/com/example/util/SHA256Util.java` — Legacy SHA-256 password checker for 관리자 콘솔. For new features prefer Spring `PasswordEncoder`, but keep this for backward compatibility.
- `src/main/java/BlueCrab/com/example/util/Base64ValidationUtil.java` — Length validation + Base64 encoding for 게시판 제목/본문. Returns `null` when validation fails; surface user-friendly messages using the provided error builders.

**Email & Notification**
- `src/main/java/BlueCrab/com/example/util/EmailTemplateUtils.java` — HTML template builder for 학생/관리자 인증 메일. Extensible via helper methods (e.g., `buildAuthCodeSection`). Update constants here instead of duplicating inline HTML.

**Request & Rate Limiting**
- Annotation: `src/main/java/BlueCrab/com/example/annotation/RateLimit.java`
- Interceptors: `src/main/java/BlueCrab/com/example/interceptor/RateLimitInterceptor.java`, `src/main/java/BlueCrab/com/example/interceptor/RequestTrackingInterceptor.java`
- Config: `src/main/java/BlueCrab/com/example/config/RateLimitConfig.java`, `src/main/java/BlueCrab/com/example/config/WebConfig.java`
  - Use `@RateLimit` on controller methods needing throttling; the interceptors handle keying (IP/user). Request tracking interceptor auto-populates MDC (`requestId`, URI, method) for log correlation—no manual UUID handling needed.
- `src/main/java/BlueCrab/com/example/util/RequestUtils.java` — Extracts client IP respecting proxy headers; use inside controllers/services instead of duplicating header parsing.

**Storage & External Services**
- `src/main/java/BlueCrab/com/example/util/MinIOFileUtil.java` — Upload/delete helpers. Bucket name comes from `app.minio.board-bucket-name`; catch `RuntimeException` wrappers to translate into API errors.
- Config classes under `src/main/java/BlueCrab/com/example/config/` (`AppConfig`, `FirebaseConfig`, `ObjectStorageConfig`, `RedisConfig`, `AsyncConfig`) centralize property binding and bean setup. Reference these rather than instantiating SDK clients manually.

> ⚠️ When touching these helpers, update this table and note any new prerequisites (env vars, beans) so incoming agents know the expected setup.

### Layer Responsibilities
Follow these layer rules when adding new classes:
- **Controller**: Handle HTTP request/response, input validation (@Valid), return unified ApiResponse format
- **Service**: Business logic, transaction management (@Transactional)
- **Repository**: Database queries, JPA interface definitions
- **Entity**: Database table mapping, leverage Lombok
- **DTO**: API request/response objects, separated from entities

### API Design Standards

**⚠️ Architecture Decision: POST-Only API Pattern**

**Team Policy (Team Leader Decision):**
All authenticated API endpoints must use the **POST method** for consistency and security.

**Implementation Rules:**
1. **Method Standardization**: Use `@PostMapping` for all authenticated user operations
2. **Authentication Required**: All business APIs require JWT authentication via `Authorization: Bearer <token>` header
3. **Request Body**: Send all parameters via request body (JSON format), not query strings
4. **Response Format**: Return standardized `ApiResponse<T>` wrapper for all endpoints

**Example Pattern:**
```java
@RestController
@RequestMapping("/api/lectures")
public class LectureController {

    // ✅ Correct: POST with authentication
    @PostMapping("/list")
    public ResponseEntity<ApiResponse<List<LectureDTO>>> getLectureList(
        @RequestBody LectureSearchRequest request) {
        // Implementation
    }

    // ✅ Correct: POST for creation
    @PostMapping("/create")
    public ResponseEntity<ApiResponse<LectureDTO>> createLecture(
        @Valid @RequestBody LectureCreateRequest request) {
        // Implementation
    }

    // ✅ Correct: POST for updates
    @PostMapping("/update")
    public ResponseEntity<ApiResponse<LectureDTO>> updateLecture(
        @Valid @RequestBody LectureUpdateRequest request) {
        // Implementation
    }

    // ✅ Correct: POST for deletion
    @PostMapping("/delete")
    public ResponseEntity<ApiResponse<Void>> deleteLecture(
        @RequestBody LectureDeleteRequest request) {
        // Implementation
    }
}
```

**Exceptions (Public Endpoints):**
- `/api/test/**` - Test endpoints (no authentication)
- `/api/auth/login` - Login endpoint (public access)
- Health check or status monitoring endpoints

**Rationale:**
- **Security Consistency**: Uniform authentication mechanism across all endpoints
- **CSRF Protection**: POST methods work naturally with CSRF token validation
- **Data Privacy**: No sensitive data exposed in URL parameters or browser history
- **Cache Control**: POST requests prevent unintended caching of authenticated data
- **Simplified Configuration**: Single security policy in Spring Security configuration

**Agent Guidelines:**
- When creating new controller methods, default to `@PostMapping` unless explicitly instructed otherwise
- Always include JWT authentication checks for business logic endpoints
- Document any public endpoints clearly with security annotations
- Follow the request/response pattern shown in existing controllers

## 4. Testing & Build Environment

### ⚠️ Important: Development Workflow
**This project follows a separated build and test workflow:**

#### Local Environment (Developer/Agent)
- **Purpose**: Code development and local builds only
- **Activities**:
  - Write/modify code in Eclipse IDE
  - Build WAR file locally (`mvn clean install`)
  - Static code analysis and review
- **Restrictions**:
  - **DO NOT run `mvn test` locally** (limited local infrastructure)
  - **DO NOT start local Tomcat/Redis/MinIO** (use server environment)

#### Server Environment (Deployment & Testing)
**Testing is performed on actual servers after deployment:**

1. **Build Locally**
   ```bash
   # In Eclipse or via Maven
   mvn clean install
   # Produces: target/BlueCrab-1.0.0.war
   ```

2. **Deploy to Backend Server**
   ```bash
   # Deploy WAR to Backend Server's Tomcat
   # Backend Server has: Tomcat + Redis + MinIO
   ```

3. **Test on Server**
   - Access deployed application via browser or API client
   - Use `/status` test page for API testing
   - Check MCP SSH logs for debugging
   - Verify integration with MariaDB (on Frontend Server)

**Why This Workflow?**
- Backend and Frontend servers are separate
- Redis, MinIO are only on Backend Server
- MariaDB is only on Frontend Server
- Full integration testing requires server environment

### Reference Maven Commands (For Development Team)
```bash
# Local build only (no testing)
mvn clean install -DskipTests

# Run application locally (limited functionality)
mvn spring-boot:run -Dspring.profiles.active=dev
# Note: Redis, MinIO connections will fail without server infrastructure

# Testing (only on server after deployment)
# Access: http://backend-server:8080/status
```

**Agent Workflow Summary:**
1. ✅ Write/modify code locally
2. ✅ Build WAR locally (`mvn clean install -DskipTests`)
3. ✅ Review code via static analysis
4. ❌ DO NOT run `mvn test` locally
5. ✅ Request deployment to server for testing
6. ✅ Test on server via `/status` page or MCP SSH

## 5. Standard Work Procedures

- **Analysis**: Use `rg`, `mvn dependency:tree`, etc. to identify affected packages and define the scope of changes.
- **Design**: Review data flow, transaction boundaries, and exception handling to maintain the principle of minimal change.
- **Implementation**: Match existing code style and logging conventions (Log4j2); add brief comments for complex logic.
- **Verification**: Write/update relevant tests. **Agents should NOT run `mvn test` locally** (testing is done in Eclipse by the dev team). If verification is needed, perform static analysis (code review) and specify manual verification procedures.
- **Documentation**: Keep `backend/BlueCrab/docs/` and `api-endpoints-documentation.json` up to date when APIs or processes change.
- **Reporting**: Briefly summarize changes, impact, and follow-up suggestions (e.g., additional tests, deployment checklists).

## 6. Quality & Safety Guidelines

- Check `git diff` before and after code changes; avoid unnecessary formatting changes.
- Do not commit sensitive information (environment variables, secret keys, production URLs); leave explanations only.
- For DB schema changes, specify migration strategy and rollback plan, and verify on test DB first.
- Maintain log level at `INFO` or below by default; mask or avoid logging PII-containing data.
- For long-running batches/transactions, verify timeout and retry policies before modifications.

## 7. eGovFrame Specifics Guide

### eGovFrame Core Concepts
This project is based on **eGovFrame (Korean e-Government Standard Framework) 4.3.0**. While similar to Spring Boot, note the following differences:

#### Dependency Management
- `pom.xml` uses `org.egovframe.boot:org.egovframe.boot.starter.parent` 4.3.0
- eGovFrame runtime libraries (`org.egovframe.rte.*`) are automatically included:
  - `org.egovframe.rte.ptl.mvc` - MVC pattern support
  - `org.egovframe.rte.psl.dataaccess` - Data access abstraction
  - `org.egovframe.rte.fdl.idgnr` - ID generation service
  - `org.egovframe.rte.fdl.property` - Property management

#### Configuration Rules
- Environment-specific settings use `application-{profile}.properties` format (same as Spring Boot standard)
- Log4j2-based logging (not Spring Boot's default Logback)
- Can leverage eGovFrame-specific components (ID generation, Property service, etc.)

#### Precautions
- eGovFrame is a public sector standard, so strictly adhere to security policies (authentication, encryption, logging).
- When adding new libraries, check for potential dependency conflicts with eGovFrame first.
- Duplicate logging frameworks like `commons-logging` have been excluded.

## 8. External Service Integration Guide

### Firebase Admin SDK
- **Purpose**: FCM push notifications, Realtime Database integration
- **Setup**: Environment variable `FIREBASE_CREDENTIALS_JSON` (JSON string) or `GOOGLE_APPLICATION_CREDENTIALS` (file path)
- **Security**: Do not commit JSON key files to repository; store in secure paths like `/opt/firebase/`
- **Testing**: `FirebasePushServiceIntegrationTest` requires actual FCM tokens
- **Reference**: `backend/BlueCrab/README.md` "Firebase Admin SDK Setup" section

### MinIO (Object Storage)
- **Purpose**: File upload/download (assignment submissions, attachments, etc.)
- **Setup**: Configure MinIO endpoint and access keys in `application-*.properties`
- **Note**: For production, recommend MinIO cluster or S3-compatible storage

### Redis
- **Purpose**: Session management, refresh token storage, caching
- **Setup**: Configure Redis host/port in `application-*.properties`
- **Note**: Application won't start if Redis connection fails. For local environments, run Redis via Docker:
  ```bash
  docker run -d -p 6379:6379 redis:latest
  ```

### Spring Mail
- **Purpose**: Email notifications (password reset, announcements, etc.)
- **Setup**: Configure SMTP server info in `application-*.properties`
- **Testing**: For local testing, use test SMTP servers like Mailtrap or Mailhog

## 9. Remote Server Access Guide (MCP SSH)

### Tomcat Log Inspection
Agents can use **MCP SSH tools** to check Tomcat server logs in real-time.

#### Log File Location
```
/tomcat/apache-tomcat-9.0.108/logs
```

#### Key Log Files
- `catalina.out` - Main Tomcat log, application output
- `localhost_access_log.YYYY-MM-DD.txt` - HTTP request access log
- `catalina.YYYY-MM-DD.log` - Tomcat system log
- `localhost.YYYY-MM-DD.log` - Localhost-related log

#### MCP SSH Usage Examples
```bash
# Check recent logs via MCP SSH
mcp__ssh-remote__exec "tail -n 100 /tomcat/apache-tomcat-9.0.108/logs/catalina.out"

# Real-time log monitoring
mcp__ssh-remote__exec "tail -f /tomcat/apache-tomcat-9.0.108/logs/catalina.out"

# Search for specific errors
mcp__ssh-remote__exec "grep -i 'error\|exception' /tomcat/apache-tomcat-9.0.108/logs/catalina.out | tail -n 50"
```

### External Database Access

#### Database Connection Info
Agents can access MariaDB via SSH using the following credentials:

```properties
# Database Configuration
Host: 121.165.24.26
Port: 55511
Database: blue_crab
Username: KDT_project
Password: Kdtkdt!1120
Driver: org.mariadb.jdbc.Driver
JDBC URL: jdbc:mariadb://121.165.24.26:55511/blue_crab?characterEncoding=UTF-8&useUnicode=true
```

#### DB Query Execution via MCP SSH
```bash
# Connect to MariaDB
mcp__ssh-remote__exec "mysql -h 121.165.24.26 -P 55511 -u KDT_project -p'Kdtkdt!1120' blue_crab -e 'SHOW TABLES;'"

# Check table schema
mcp__ssh-remote__exec "mysql -h 121.165.24.26 -P 55511 -u KDT_project -p'Kdtkdt!1120' blue_crab -e 'DESCRIBE user_tbl;'"

# Query data (recent 5 records)
mcp__ssh-remote__exec "mysql -h 121.165.24.26 -P 55511 -u KDT_project -p'Kdtkdt!1120' blue_crab -e 'SELECT * FROM user_tbl LIMIT 5;'"
```

#### Precautions
- **Production Environment**: Always backup before database modification operations.
- **Sensitive Info**: Never expose passwords in commit messages or documentation.
- **Query Execution**: SELECT queries are safe, but use UPDATE/DELETE cautiously.
- **Transactions**: Wrap critical operations in transactions and prepare for rollback.

### MinIO Object Storage Access

Agents can inspect MinIO storage using **mc (MinIO Client)** via MCP SSH for debugging file uploads, profile images, and chat logs.

#### MinIO Client (mc) Setup

##### Installation Status
```bash
# Check if mc is installed (✅ Already installed on server)
mcp__ssh-remote__exec "~/mc --version"

# Verify alias configuration
mcp__ssh-remote__exec "~/mc alias list"
```

##### Server Configuration
```
Alias: local
Endpoint: http://127.0.0.1:9000
Access Key: minioadmin
Secret Key: minioadmin (⚠️ production should use stronger credentials)
```

##### Current Buckets
- `board-attached/` - Board file attachments
- `profile-img/` - User profile images
- `consultation-chats/` - Chat logs (when consultation feature is implemented)

#### MinIO Operations via MCP SSH

**List all buckets:**
```bash
mcp__ssh-remote__exec "~/mc ls local"
```

**Check bucket contents:**
```bash
# Profile images
mcp__ssh-remote__exec "~/mc ls local/profile-img"

# Board attachments
mcp__ssh-remote__exec "~/mc ls local/board-attached"

# Chat logs (if exists)
mcp__ssh-remote__exec "~/mc ls local/consultation-chats"
```

**Download file for inspection:**
```bash
# Download to temporary location
mcp__ssh-remote__exec "~/mc cp local/profile-img/profile_123_1234567890.jpg /tmp/debug.jpg"

# View file content (for text files like chat logs)
mcp__ssh-remote__exec "~/mc cp local/consultation-chats/chat_456_final.txt -"
```

**Check file metadata:**
```bash
mcp__ssh-remote__exec "~/mc stat local/profile-img/profile_123_1234567890.jpg"
```

**Find files by pattern:**
```bash
# Find all .jpg files
mcp__ssh-remote__exec "~/mc find local/profile-img --name '*.jpg'"

# Find files uploaded today
mcp__ssh-remote__exec "~/mc find local/board-attached --newer-than 1d"

# Find specific user's profile images
mcp__ssh-remote__exec "~/mc ls local/profile-img | grep 'profile_123'"
```

#### Troubleshooting Scenarios

**Debug profile image upload:**
```bash
# 1. Check if file exists in MinIO
mcp__ssh-remote__exec "~/mc ls local/profile-img | grep 'profile_123'"

# 2. Verify file size and metadata
mcp__ssh-remote__exec "~/mc stat local/profile-img/profile_123_xxx.jpg"

# 3. Check recent uploads (last 1 hour)
mcp__ssh-remote__exec "~/mc find local/profile-img --newer-than 1h"
```

**Debug board file attachments:**
```bash
# List recent attachments
mcp__ssh-remote__exec "~/mc ls local/board-attached --recursive"

# Check specific file
mcp__ssh-remote__exec "~/mc stat local/board-attached/assignment_123_file.pdf"
```

**Debug chat log archiving:**
```bash
# Check if consultation chat was archived
mcp__ssh-remote__exec "~/mc ls local/consultation-chats | grep 'chat_456'"

# View chat log content
mcp__ssh-remote__exec "~/mc cp local/consultation-chats/chat_456_final.txt -"
```

**Verify bucket policy:**
```bash
# Check bucket access policy
mcp__ssh-remote__exec "~/mc policy list local/profile-img"

# View detailed policy (JSON)
mcp__ssh-remote__exec "~/mc policy get local/profile-img"
```

#### Precautions
- **Read-Only Operations**: Prefer `ls`, `stat`, `find` over `rm`, `cp` for debugging
- **Production Data**: Never delete files without backup verification
- **Access Control**: MinIO credentials should be rotated regularly in production
- **File Size**: Use `mc stat` to check file size before downloading large files
- **Concurrent Access**: MinIO handles concurrent reads/writes, but avoid manual modifications during active uploads

## 10. Backend Test Endpoints

### Development/Debugging API Endpoints
The project provides several test endpoints for development and debugging. Agents can use these to verify system status and diagnose issues.

#### 1. System Health Checks (ApiController)

**`GET /api/health`** - No auth required ✅
```bash
curl http://localhost:8080/api/health
```
**Response Info:**
- Server status (UP/DOWN)
- Memory usage (warning threshold: 80%)
- Database connection status
- Database product name and URL

**`GET /api/ping`** - No auth required ✅
```bash
curl http://localhost:8080/api/ping
```
Lightweight endpoint for quick connection testing

**`GET /api/test`** - No auth required ✅
```bash
curl http://localhost:8080/api/test
```
Returns API server connection and timestamp

**`GET /api/test-auth`** - Auth required 🔒
For JWT token authentication testing

#### 2. Firebase Testing (FirebaseTestController)
⚠️ Only active when `firebase.enabled=true`

**`GET /api/test/firebase-status`** - No auth required ✅
```bash
curl http://localhost:8080/api/test/firebase-status
```
Check Firebase initialization status and VAPID public key

**`GET /api/test/vapid-key`** - No auth required ✅
```bash
curl http://localhost:8080/api/test/vapid-key
```
Retrieve VAPID public key for FCM Web Push

**`POST /api/test/send-test-push`** - Test key required
```bash
curl -X POST "http://localhost:8080/api/test/send-test-push" \
  -d "token=FCM_DEVICE_TOKEN" \
  -d "title=Test Notification" \
  -d "body=Test Message" \
  -d "testKey=test123"
```
Send test push notification to individual device

**`POST /api/test/send-topic-test`** - Test key required
Topic-based push notification testing

**`POST /api/test/send-data-only`** - Test key required
Data-only push (for duplicate notification prevention verification)

#### 3. Database Testing (DatabaseTestController)

**`GET /api/test/lamp-table-status`**
```bash
curl http://localhost:8080/api/test/lamp-table-status
```
Check reading room seat table status:
- Total seat count
- Available/occupied seat counts
- Sample data (max 5 records)

**`GET /api/test/lamp-table-simple`**
```bash
curl http://localhost:8080/api/test/lamp-table-simple
```
Table access test (returns record counts only)

**`GET /api/test/initialize-seats`**
```bash
curl http://localhost:8080/api/test/initialize-seats
```
Initialize seats 1-80 (creates only missing seats)

**`GET /api/test/reset-all-seats`**
```bash
curl http://localhost:8080/api/test/reset-all-seats
```
⚠️ Release all seats (use only in test/dev environments)

### Test Endpoint Usage Scenarios

#### Post-Deployment System Verification
```bash
# 1. Verify server connection
curl http://121.165.24.26:8080/api/ping

# 2. System health check
curl http://121.165.24.26:8080/api/health

# 3. Firebase initialization check (if using Firebase)
curl http://121.165.24.26:8080/api/test/firebase-status

# 4. Database connection check
curl http://121.165.24.26:8080/api/test/lamp-table-simple
```

#### Problem Diagnosis Flow
1. `/api/health` - Check overall system status
2. MCP SSH to check Tomcat logs (`/tomcat/apache-tomcat-9.0.108/logs/catalina.out`)
3. `/api/test/lamp-table-status` - Check DB connection and data status
4. If needed, execute direct DB queries via MCP SSH

### Precautions
- **Production Environment**: Recommend disabling `/api/test/*` endpoints in SecurityConfig
- **testKey**: Test key is hardcoded as `test123`; should be removed for production
- **Data Modification**: `initialize-seats`, `reset-all-seats` modify DB, use with caution

---

## 10-1. Integrated API Test Page (Swagger UI Style)

### Overview
The project provides an **integrated API testing tool similar to Swagger UI**. You can test all APIs directly in the browser and manage JWT authentication.

### Access Information
```
URL: http://{server}:{port}/status
Examples: http://localhost:8080/status
          http://bluecrab.chickenkiller.com/BlueCrab-1.0.0/status
```

### Key Features

#### 1. Authentication & Token Management
- **General User Login**: Email/password authentication
- **Admin 2-Step Authentication**: Auto-login (automatic email verification code handling)
- **Token Management**:
  - Access Token / Refresh Token auto-save
  - Token refresh
  - Logout
  - Token set save/load (switch between multiple accounts)

#### 2. API Endpoint Testing
**Category-based Endpoint Organization:**
- **게시판 (Board)**: Post creation/deletion, board list
- **관리자 (Admin)**: Admin login, email verification, facility reservation management, facility reservation search
- **사용자 · 프로필 (User · Profile)**: Profile view, my profile view, user details, user list (admin)

**Information Provided per Endpoint:**
- HTTP method (GET/POST/PUT/DELETE)
- Request parameter templates
- Request body (JSON) templates
- Response examples
- Authentication requirements

#### 3. Advanced Features
- **Request Body Template Management**: Save/load frequently used JSON templates
- **Request History**: View recent API request records
- **Token Set Management**: Store and quickly switch between multiple user account tokens
- **Real-time Response Display**: Syntax-highlighted JSON responses

### API Template JSON Structure
API endpoint information is defined in `/config/api-templates.json` with the following structure:

```json
{
  "endpointKey": {
    "category": "Category Name",
    "name": "Endpoint Name",
    "method": "GET|POST|PUT|DELETE",
    "url": "/api/endpoint/path",
    "requiresAuth": true,
    "body": {
      "param1": "value1",
      "param2": "value2"
    },
    "description": "Endpoint description"
  }
}
```

### Required Tasks When Developing New Features

#### ⚠️ Important: Test Page Update Mandatory After API Development
After developing a new API endpoint, you **must** perform the following tasks:

**1. Update API Template JSON**
```json
// src/main/resources/static/config/api-templates.json
{
  "yourNewEndpoint": {
    "category": "Appropriate Category",
    "name": "New Feature Name",
    "method": "POST",
    "url": "/api/your/new/endpoint",
    "requiresAuth": true,
    "body": {
      "exampleParam": "exampleValue"
    },
    "description": "Description of what this API does"
  }
}
```

**2. Category Definition**
Use existing categories or add new ones:
- **게시판 (Board)** - Post-related features
- **관리자 (Admin)** - Admin-only features
- **사용자 · 프로필 (User · Profile)** - User information management
- **강의 (Lecture)** - Lecture/assignment/grade related
- **열람실 (Reading Room)** - Reading room reservation management
- **시설예약 (Facility Reservation)** - Facility reservation management
- **알림 (Notification)** - Push notifications, FCM
- **기타 (Other)** - Unclassified features

**3. Required Checklist**
- [ ] Add endpoint to `api-templates.json`
- [ ] Specify category (existing or new)
- [ ] Accurately specify HTTP method
- [ ] Set authentication requirement (`requiresAuth`)
- [ ] Write request body template (for POST/PUT, etc.)
- [ ] Write endpoint description
- [ ] Verify actual operation on test page

**4. Test Page Operation Verification**
```bash
# 1. Run server
mvn spring-boot:run

# 2. Access via browser
http://localhost:8080/status

# 3. Check new API in endpoint selection dropdown
# 4. Test actual request with template data
# 5. Verify response and fix any issues
```

### File Locations
```
src/main/resources/
├── static/
│   ├── js/
│   │   ├── api-tester.js              # Main test logic
│   │   └── api-test-standalone.js     # Standalone version
│   ├── css/
│   │   └── api-tester.css             # Stylesheet
│   └── config/
│       └── api-templates.json         # ⭐ API endpoint definitions
└── templates/
    └── status.html                     # Thymeleaf template
```

### Development Workflow
```
1. Write new endpoint in Controller
   ↓
2. Add endpoint info to api-templates.json
   ↓
3. Check dropdown on /status page
   ↓
4. Test directly in browser
   ↓
5. Fix issues and re-verify immediately
```

### Advantages
- **No Swagger Setup Required**: Use without separate Swagger dependencies or annotations
- **Quick Testing**: Test APIs immediately in browser
- **Auto JWT Management**: Auto token refresh and header injection
- **Frontend Dev Support**: Visually verify and test API specs

## 11. Troubleshooting & References

- When execution fails, **check Tomcat logs via MCP SSH first**, then track the occurrence point with `rg`.
- **Use test endpoints** (`/api/health`, `/api/test/*`) to diagnose system status first.
- Identify dependency conflicts with `mvn dependency:tree -Dincludes=<groupId>`.
- For eGovFrame-related issues, refer to the [Korean e-Government Standard Framework Portal](https://www.egovframe.go.kr)
- For Spring configuration, deployment, and monitoring guidance, check documents under `backend/BlueCrab/docs/operations/` first.
- If legacy materials are needed, refer to `docs-legacy/2025-10-18/backend-guide/`, but review for conflicts with current policies.

## 12. Checklist: When Adding New Features

When implementing new features, agents should check the following items:

### Code Writing
- [ ] Follow existing package structure and naming conventions
- [ ] Separate layer responsibilities (Controller → Service → Repository)
- [ ] Leverage Lombok annotations (@Getter, @Setter, @RequiredArgsConstructor, etc.)
- [ ] Return responses in unified ApiResponse format
- [ ] Input validation (@Valid, @NotNull, @Size, etc.)

### Security & Authentication
- [ ] Verify Spring Security configuration (SecurityConfig)
- [ ] Review JWT authentication requirements
- [ ] Check permissions (@PreAuthorize, etc.)
- [ ] Prohibit logging sensitive information

### Database
- [ ] Write entity classes (JPA, leverage Lombok)
- [ ] Define Repository interfaces
- [ ] Set transaction scope (@Transactional)
- [ ] Establish migration plan for DB schema changes

### Testing
- [ ] Write unit tests (`src/test/java/`)
- [ ] Prepare environment variables for integration tests if needed
- [ ] Verify test execution and success in Eclipse (agents prohibited from local mvn test execution)

### Documentation
- [ ] Update `api-endpoints-documentation.json` when APIs change
- [ ] **Important**: Add new APIs to `src/main/resources/static/config/api-templates.json` (for test page)
- [ ] Verify synchronization with frontend API wrappers
- [ ] Update relevant documents in `backend/BlueCrab/docs/`
- [ ] Record major changes in `CHANGELOG.md`, etc. (if exists)

### External Services
- [ ] Provide environment variable setup guide when using Firebase/MinIO/Redis
- [ ] Review fallback strategies for service failures
- [ ] Write integration test scenarios

---

**Update History**
- 2025-10-21: Added eGovFrame specifics, external service integration guide, checklist
- 2025-10-21: Updated tech stack info (MariaDB, major domain modules)
- 2025-10-21: Translated entire document to English for agent token efficiency

Update this document as needed to share new procedures, warnings, and checklists, and add links to the `docs/agent-readme.md` index.
