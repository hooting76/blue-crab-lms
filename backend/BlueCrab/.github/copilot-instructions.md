# BlueCrab Codebase AI Agent Instructions

## Project Overview
BlueCrab is a Spring Boot 2.7.x web application built on the Korean eGov framework, implementing JWT authentication with RESTful APIs. It's packaged as a WAR for Tomcat deployment behind an Nginx reverse proxy, connecting to an external MariaDB database.

## Architecture & Core Technologies

### Framework Stack
- **Spring Boot 2.7.x** with eGov framework parent (`org.egovframe.boot:egovframe-boot-parent-web-tomcat`)
- **WAR packaging** for Tomcat 9.0.108 deployment
- **Maven** build system with environment-specific profiles
- **MariaDB** external database at `121.165.24.26:55511`
- **JWT Authentication** with HMAC-SHA256 tokens
- **Nginx** reverse proxy for HTTPS termination

### Key Configuration Patterns
- Environment-based properties: `application.properties`, `application-dev.properties`, `application-prod.properties`
- Centralized configuration via `AppConfig.java` with nested `@ConfigurationProperties` classes
- External database connection with HikariCP pooling
- JWT secrets managed through environment variables

## Critical Code Patterns

### 1. Configuration Architecture
```java
@Configuration
@EnableConfigurationProperties({AppConfig.Security.class, AppConfig.Database.class, AppConfig.Jwt.class})
public class AppConfig {
    // Nested configuration classes with @ConfigurationProperties
}
```

### 2. Standardized API Response Format
**ALWAYS** use `ApiResponse<T>` for REST endpoints:
```java
public class ApiResponse<T> {
    private boolean success;
    private String message;
    private T data;
    private String timestamp;
}
```

### 3. JWT Authentication Flow
- **Access tokens**: 15-minute expiration
- **Refresh tokens**: 24-hour expiration with rotation
- **Headers**: `Authorization: Bearer <token>`
- **Validation**: Custom JWT filters in Spring Security chain

### 4. Security Configuration
- JWT-based authentication with custom filters
- CORS handling (avoid duplication with Nginx)
- OPTIONS request support for preflight
- Public endpoints: `/auth/**`, `/api/status`, `/api/health`

## Development Workflows

### Build & Deployment
```bash
# Development build
mvn clean package -Pdev

# Production build  
mvn clean package -Pprod

# WAR deployment to Tomcat
# Result: target/BlueCrab-1.0.0.war
```

### Database Integration
- **Connection**: External MariaDB with environment-specific credentials
- **Pool**: HikariCP with configurable settings
- **Scripts**: Sample data in `src/main/resources/db/sampledb.sql`

### CORS Configuration
- **Primary**: Configured in `SecurityConfig.java`
- **Nginx**: Headers handled at proxy level
- **Rule**: Avoid duplication between Spring and Nginx layers

## File Structure Conventions

### Source Organization
```
src/main/java/BlueCrab/com/          # Main application packages
src/main/resources/                  # Configuration and static resources
src/main/webapp/                     # Web application resources
docs/                               # Comprehensive documentation
```

### Key Files to Understand
- `BlueCrabApplication.java`: Main Spring Boot class extending `SpringBootServletInitializer`
- `AppConfig.java`: Centralized configuration management
- `SecurityConfig.java`: Security and CORS configuration
- `AuthController.java`: Authentication endpoints
- `ApiResponse.java`: Standard response wrapper

## Common Development Tasks

### Adding New REST Endpoints
1. Use `@RestController` annotation
2. Return `ApiResponse<T>` for consistency
3. Handle authentication via JWT tokens
4. Follow existing URL patterns (`/api/v1/...`)

### Configuration Changes
1. Add properties to appropriate `application-{env}.properties`
2. Create or update `@ConfigurationProperties` classes
3. Inject via `AppConfig` nested classes
4. Consider environment-specific variations

### Database Operations
1. Use existing HikariCP connection pool
2. Follow repository pattern if using JPA
3. Handle transactions appropriately
4. Consider connection limits for external DB

### Security Modifications
1. Update `SecurityConfig.java` for new endpoints
2. Consider JWT token requirements
3. Handle CORS for new API routes
4. Test authentication flows thoroughly

## Testing & Debugging

### API Testing
- Use provided `api-test.html` for interactive testing
- JWT tokens stored in localStorage
- Test both authentication and protected endpoints

### Logging
- Log4j2 configuration in `src/main/resources/log4j2.xml`
- Environment-specific log levels
- Monitor templates: `log-monitor.html`, `status.html`

### Common Issues
- **CORS**: Check both Spring and Nginx configurations
- **JWT**: Verify token expiration and refresh flows
- **Database**: Monitor connection pool metrics
- **Deployment**: Ensure WAR packaging for Tomcat

## Documentation Standards

### Code Comments
- Class-level: Purpose, dependencies, configuration
- Method-level: Parameters, return values, exceptions
- Complex logic: Inline explanations
- Configuration: Environment-specific notes

### API Documentation
- Endpoint descriptions with examples
- Request/response formats
- Authentication requirements
- Error handling patterns

## Integration Points

### External Dependencies
- **eGov Framework**: Korean government framework integration
- **MariaDB**: External database server
- **Nginx**: Reverse proxy and CORS handling
- **Tomcat**: Application server deployment

### Key Interfaces
- JWT authentication flow
- Database connection management
- CORS policy coordination
- Environment-specific configuration

## Best Practices for AI Agents

1. **Always check existing patterns** before implementing new features
2. **Use `ApiResponse<T>`** for all REST endpoints
3. **Consider CORS implications** when adding new APIs
4. **Follow environment-specific configuration** patterns
5. **Test JWT authentication flows** for new endpoints
6. **Check database connection limits** for new operations
7. **Maintain documentation consistency** with existing standards
8. **Consider WAR deployment requirements** for new dependencies

## Quick Reference

### Common Commands
```bash
# Build for development
mvn clean package -Pdev

# Run tests
mvn test

# Check dependencies
mvn dependency:tree

# Deploy WAR
cp target/BlueCrab-1.0.0.war $TOMCAT_HOME/webapps/
```

### Important Endpoints
- `POST /auth/login` - User authentication
- `POST /auth/refresh` - Token refresh
- `GET /api/status` - Health check
- `GET /api/v1/user/profile` - User profile (protected)

### Configuration Files Priority
1. `application-{profile}.properties` (environment-specific)
2. `application.properties` (default)
3. Environment variables (highest priority)

This guide provides the essential knowledge for immediate productivity in the BlueCrab codebase. Focus on understanding the JWT authentication flow, standardized API responses, and environment-specific configuration patterns.
