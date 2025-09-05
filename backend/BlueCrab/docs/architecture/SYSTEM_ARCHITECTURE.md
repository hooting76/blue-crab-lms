# 🏗️ BlueCrab 시스템 아키텍처

## 📋 **개요**
BlueCrab은 레이어드 아키텍처 패턴을 기반으로 설계된 Spring Boot 웹 애플리케이션입니다.

## 🏛️ **시스템 아키텍처**

```
┌─────────────────────────────────────────────────┐
│                 Presentation Layer               │
├─────────────────────────────────────────────────┤
│  Controllers  │  Interceptors  │  Exception     │
│  - UserController  - RequestTracking  - Global │
│  - AuthController  - Security        - Handler │
└─────────────────────────────────────────────────┘
                        │
┌─────────────────────────────────────────────────┐
│                 Service Layer                   │
├─────────────────────────────────────────────────┤
│  Business Logic  │  Security      │  Utilities  │
│  - UserTblService   - AuthService   - JwtUtil   │
│  - Validation       - UserDetails   - Logger    │
└─────────────────────────────────────────────────┘
                        │
┌─────────────────────────────────────────────────┐
│                 Repository Layer                │
├─────────────────────────────────────────────────┤
│  Data Access Objects                            │
│  - UserTblRepository                           │
│  - JPA/Hibernate                               │
└─────────────────────────────────────────────────┘
                        │
┌─────────────────────────────────────────────────┐
│                 Database Layer                  │
├─────────────────────────────────────────────────┤
│  Oracle Database                               │
│  - USER_TBL                                    │
└─────────────────────────────────────────────────┘
```

## 🔐 **보안 아키텍처**

### **인증 플로우**
```
Client Request → Security Filter Chain → JWT Filter → Controller
     ↓
JWT Validation → UserDetailsService → Database → User Authority
     ↓
SecurityContext → Method Security → Business Logic
```

### **주요 보안 컴포넌트**
- `JwtAuthenticationFilter`: JWT 토큰 검증
- `CustomUserDetailsService`: 사용자 정보 로드
- `SecurityConfig`: 보안 설정 통합 관리

## 📊 **데이터 플로우**

### **사용자 등록/조회 플로우**
```
1. Client Request (JSON)
   ↓
2. Controller Validation
   ↓
3. Service Business Logic
   ↓
4. Repository Data Access
   ↓
5. Database Operation
   ↓
6. Response (ApiResponse<T>)
```

## 🎯 **설계 원칙**

### **1. 단일 책임 원칙 (SRP)**
- Controller: HTTP 요청/응답 처리
- Service: 비즈니스 로직
- Repository: 데이터 접근

### **2. 의존성 역전 원칙 (DIP)**
- Interface 기반 설계
- Spring IoC Container 활용

### **3. 관심사 분리**
- 인증/인가 로직 분리
- 예외 처리 중앙화
- 로깅 AOP 적용

## 🔧 **주요 패턴**

### **1. Repository Pattern**
```java
@Repository
public interface UserTblRepository extends JpaRepository<UserTbl, Integer>
```

### **2. Service Layer Pattern**
```java
@Service
@Transactional
public class UserTblService
```

### **3. DTO Pattern**
```java
public class ApiResponse<T> {
    private boolean success;
    private String message;
    private T data;
}
```

## 📦 **패키지 구조**

```
src/main/java/BlueCrab/com/example/
├── config/          # 설정 클래스
│   ├── AppConfig.java
│   ├── SecurityConfig.java
│   └── WebConfig.java
├── controller/      # REST 컨트롤러
│   ├── UserController.java
│   └── AuthController.java
├── service/         # 비즈니스 로직
│   ├── UserTblService.java
│   └── AuthService.java
├── repository/      # 데이터 접근
│   └── UserTblRepository.java
├── entity/          # JPA 엔티티
│   └── UserTbl.java
├── dto/             # 데이터 전송 객체
│   ├── ApiResponse.java
│   └── LoginRequest.java
├── security/        # 보안 관련
│   ├── JwtAuthenticationFilter.java
│   └── CustomUserDetailsService.java
├── util/            # 유틸리티
│   ├── JwtUtil.java
│   └── SlowQueryLogger.java
└── exception/       # 예외 처리
    ├── GlobalExceptionHandler.java
    └── ResourceNotFoundException.java
```

## 🚀 **성능 고려사항**

### **1. 데이터베이스 최적화**
- Connection Pool 설정 (HikariCP)
- 쿼리 최적화
- 인덱스 활용

### **2. 캐싱 전략**
- JWT 토큰 검증 캐시
- 사용자 권한 정보 캐시

### **3. 로깅 최적화**
- 비동기 로깅
- 로그 레벨 분리
- 느린 쿼리 모니터링

---

**작성일**: 2025-08-27  
**버전**: 1.0.0
