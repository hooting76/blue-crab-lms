# BlueCrab API 인증 시스템

## 개요
Argon2id + PHC 문자열 형식 + JWT(액세스/리프레시 토큰) 기반 인증 시스템이 구현된 Spring Boot 애플리케이션입니다.

## 기술 스택
- Spring Boot 2.x (eGovFrame 4.3.0 기반)
- Spring Security + JWT
- Argon2id 암호화 (PHC 문자열 형식)
- MariaDB + Spring Data JPA
- Maven

## 인증 API 엔드포인트

### 1. 로그인
```http
POST /api/auth/login
Content-Type: application/json

{
    "username": "admin",
    "password": "admin123!"
}
```

**응답 (성공):**
```json
{
    "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "tokenType": "Bearer",
    "expiresIn": 900,
    "user": {
        "id": 1,
        "username": "admin",
        "name": "시스템 관리자",
        "email": "admin@bluecrab.com"
    }
}
```

### 2. 토큰 리프레시
```http
POST /api/auth/refresh
Content-Type: application/json

{
    "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
}
```

### 3. 로그아웃
```http
POST /api/auth/logout
Authorization: Bearer {accessToken}
```

### 4. 토큰 검증
```http
GET /api/auth/validate
Authorization: Bearer {accessToken}
```

## 보호된 API 사용

모든 `/api/**` 엔드포인트(인증 제외)는 액세스 토큰이 필요합니다:

```http
GET /api/test-auth
Authorization: Bearer {accessToken}
```

## 개발용 계정 (dev 프로파일)

`spring.profiles.active=dev` 설정 시 자동 생성되는 테스트 계정들:

| Username | Password | 설명 |
|----------|----------|------|
| admin | admin123! | 관리자 계정 (ROLE_ADMIN, ROLE_USER) |
| testuser | test123! | 일반 사용자 (ROLE_USER) |
| inactive | inactive123! | 비활성화된 계정 |

## 비밀번호 암호화

현재 **SHA-256** 알고리즘을 사용하여 비밀번호를 암호화합니다:
- 빠른 개발 및 테스트를 위한 임시 구현
- 추후 Argon2id로 업그레이드 예정
- Spring Security의 MessageDigestPasswordEncoder 사용

## 설정

### JWT 설정 (application.properties)
```properties
# JWT 설정
jwt.secret=mySecretKey12345678901234567890123456789012345678901234567890
jwt.access-token-expiration=900000  # 15분
jwt.refresh-token-expiration=86400000  # 24시간

# 프로파일 설정
spring.profiles.active=dev
```

### Argon2id 파라미터
현재는 SHA-256을 사용하고 있습니다. 추후 Argon2id 업그레이드 시 다음 파라미터 적용 예정:
- Salt Length: 16 bytes
- Hash Length: 32 bytes
- Parallelism: 1
- Memory: 65536 KiB (64 MiB)
- Iterations: 3

## 배포

### WAR 파일 생성
```bash
mvn clean package
```

### Tomcat 배포
1. `target/BlueCrab-1.0.0.war` 파일을 Tomcat webapps에 복사
2. 외부 설정 파일 준비: `/home/project01/tomcat/conf/BlueCrab/application.properties`
3. `setenv.sh`에 외부 설정 경로 추가:
   ```bash
   export CATALINA_OPTS="$CATALINA_OPTS -Dspring.config.additional-location=file:/home/project01/tomcat/conf/BlueCrab/"
   ```

## 에러 응답

인증 실패 시:
```json
{
    "error": "Authentication failed",
    "message": "Invalid username or password"
}
```

토큰 만료 시:
```json
{
    "status": 401,
    "error": "Unauthorized",
    "message": "Full authentication is required to access this resource",
    "path": "/api/test-auth"
}
```

## 테스트

### Postman 컬렉션 예시

1. **로그인**
   - POST `http://localhost:8080/BlueCrab-1.0.0/api/auth/login`
   - Body: `{"username": "admin", "password": "admin123!"}`

2. **보호된 API 호출**
   - GET `http://localhost:8080/BlueCrab-1.0.0/api/test-auth`
   - Header: `Authorization: Bearer {accessToken}`

3. **토큰 리프레시**
   - POST `http://localhost:8080/BlueCrab-1.0.0/api/auth/refresh`
   - Body: `{"refreshToken": "{refreshToken}"}`
