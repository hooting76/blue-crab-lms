# 🦀 BlueCrab Backend Application

[![Java](https://img.shields.io/badge/Java-11+-orange.svg)](https://openjdk.java.net/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-2.7.x-green.svg)](https://spring.io/projects/spring-boot)
[![Oracle](https://img.shields.io/badge/Database-Oracle-red.svg)](https://www.oracle.com/database/)

## 📋 **프로젝트 개요**

BlueCrab은 Spring Boot 기반의 웹 애플리케이션으로, JWT 인증과 사용자 관리 기능을 제공하는 백엔드 서비스입니다.

## � **문서 가이드**

전체 프로젝트 문서는 [`docs/`](docs/) 폴더에 체계적으로 정리되어 있습니다:

### 📖 **주요 문서**
- **📘 [전체 문서 목차](docs/README.md)** - 모든 문서의 인덱스
- **🏗️ [시스템 아키텍처](docs/architecture/SYSTEM_ARCHITECTURE.md)** - 전체 시스템 설계
- **📡 [API 레퍼런스](docs/api/API_REFERENCE.md)** - 완전한 API 가이드
- **🚀 [배포 가이드](docs/operations/DEPLOYMENT_GUIDE.md)** - 운영 배포 방법

### 📁 **문서 카테고리**
- **🏛️ `docs/architecture/`** - 시스템 아키텍처 및 설계
- **🛠️ `docs/development/`** - 개발 가이드 및 표준
- **📡 `docs/api/`** - API 문서 및 사용법
- **🚀 `docs/operations/`** - 배포, 모니터링, 로그 관리

## 🚀 **빠른 시작**

### **1. 개발 환경 요구사항**
- Java 11+
- Maven 3.6+
- Oracle Database

### **2. 로컬 실행**
```bash
# 프로젝트 클론
git clone <repository-url>
cd BlueCrab

# 빌드 및 실행
mvn clean install
mvn spring-boot:run -Dspring.profiles.active=dev
```

### **3. 접속 확인**
```bash
# Health Check
curl http://localhost:8080/api/ping

# API 테스트
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"test@bluecrab.com","password":"test123!"}'
```
- **Spring Security** - 보안 프레임워크
- **JWT (JSON Web Token)** - 토큰 기반 인증
- **BCrypt** - 비밀번호 암호화

### Build & Deployment
- **Maven** - 빌드 도구
- **WAR** - 배포 패키징 방식

## 📁 디렉토리 구조

```
src/
├── main/
│   ├── java/BlueCrab/com/example/
│   │   ├── BlueCrabApplication.java          # 메인 애플리케이션 클래스
│   │   ├── config/                           # 설정 클래스들
│   │   │   ├── AppConfig.java               # 애플리케이션 설정
│   │   │   ├── DataLoader.java              # 초기 데이터 로딩
│   │   │   ├── SecurityConfig.java          # Spring Security 설정
│   │   │   └── WebConfig.java               # Web MVC 설정
│   │   ├── controller/                       # REST API 컨트롤러
│   │   │   ├── AuthController.java          # 인증 관련 API
│   │   │   ├── UserController.java          # 사용자 관리 API
│   │   │   ├── DatabaseController.java      # 데이터베이스 관리 API
│   │   │   └── ...
│   │   ├── dto/                             # 데이터 전송 객체
│   │   │   ├── ApiResponse.java             # 표준 API 응답 형식
│   │   │   ├── LoginRequest.java            # 로그인 요청 DTO
│   │   │   └── LoginResponse.java           # 로그인 응답 DTO
│   │   ├── entity/                          # JPA 엔티티
│   │   │   └── UserTbl.java                 # 사용자 테이블 엔티티
│   │   ├── repository/                      # 데이터 액세스 레이어
│   │   │   └── UserTblRepository.java       # 사용자 리포지토리
│   │   ├── service/                         # 비즈니스 로직
│   │   │   ├── AuthService.java             # 인증 서비스
│   │   │   └── UserTblService.java          # 사용자 관리 서비스
│   │   ├── security/                        # 보안 관련 클래스
│   │   │   ├── JwtAuthenticationFilter.java # JWT 필터
│   │   │   └── CustomUserDetailsService.java # 사용자 세부정보 서비스
│   │   ├── util/                           # 유틸리티 클래스
│   │   │   ├── JwtUtil.java                # JWT 토큰 유틸리티
│   │   │   └── RequestUtils.java           # 요청 처리 유틸리티
│   │   └── exception/                      # 예외 처리
│   │       ├── GlobalExceptionHandler.java  # 전역 예외 처리
│   │       └── ...
│   └── resources/
│       ├── application.properties           # 메인 설정 파일
│       ├── application-dev.properties       # 개발 환경 설정
│       ├── log4j2.xml                      # 로깅 설정
│       └── templates/                      # Thymeleaf 템플릿
└── test/                                   # 테스트 코드
```

## 🔧 주요 구성 요소

### 1. Authentication & Security

#### AuthController
- JWT 기반 로그인/로그아웃 처리
- 토큰 갱신 및 검증 API 제공
- 통일된 ApiResponse 형식으로 응답 반환

#### AuthService  
- 사용자 인증 로직 처리
- JWT 액세스/리프레시 토큰 발급
- 비밀번호 검증 및 사용자 정보 관리

#### JwtUtil
- JWT 토큰 생성, 검증, 파싱
- 액세스 토큰 (15분) 및 리프레시 토큰 (24시간) 관리

### 2. User Management

#### UserTbl Entity
- 사용자 정보 저장을 위한 JPA 엔티티
- 이메일, 비밀번호, 개인정보 필드 포함
- 학생/교수 구분 기능

#### UserController & UserService
- 사용자 등록, 조회, 수정 기능
- CRUD 작업 및 비즈니스 로직 처리

### 3. Database Configuration

#### 연결 정보
- **Host**: 121.165.24.26:55511
- **Database**: blue_crab
- **Connection Pool**: HikariCP (최대 20개 연결)
- **JPA DDL**: update 모드

### 4. API Response Format

모든 API는 다음과 같은 통일된 형식으로 응답합니다:

```json
{
  "success": true,
  "message": "요청이 성공적으로 처리되었습니다.",
  "data": {
    // 실제 응답 데이터
  },
  "timestamp": "2025-08-27T12:00:00Z"
}
```

## 🚀 실행 방법

### 1. 개발 환경에서 실행
```bash
mvn spring-boot:run
```

### 2. WAR 파일로 빌드 및 배포
```bash
mvn clean package
# target/BlueCrab-1.0.0.war 파일이 생성됨
```

### 3. 프로필 설정
- **개발**: `spring.profiles.active=dev` (기본값)
- **운영**: 환경변수 또는 설정 파일에서 프로필 변경

## 📊 주요 API 엔드포인트

### Authentication APIs
- `POST /api/auth/login` - 사용자 로그인
- `POST /api/auth/refresh` - 토큰 갱신  
- `POST /api/auth/logout` - 로그아웃
- `GET /api/auth/validate` - 토큰 검증

### User Management APIs
- `GET /api/users` - 사용자 목록 조회
- `POST /api/users` - 사용자 등록
- `GET /api/users/{id}` - 사용자 상세 조회
- `PUT /api/users/{id}` - 사용자 정보 수정

### System APIs  
- `GET /api/metrics` - 시스템 메트릭 조회
- `GET /log-monitor` - 로그 모니터링 화면

## 🔐 보안 설정

### JWT Configuration
- **액세스 토큰**: 15분 (900초)
- **리프레시 토큰**: 24시간 (86400초)  
- **알고리즘**: HS256
- **Secret Key**: 환경변수 또는 설정 파일에서 관리

### CORS 설정
- 허용된 Origins: `http://localhost:3000`, `http://localhost:8080`
- 개발/운영 환경별 설정 가능

### 비밀번호 보안
- **암호화**: BCrypt 
- **해싱 알고리즘**: SHA-256

## 📝 로깅 & 모니터링

### Log4j2 설정
- 로그 레벨별 출력 설정
- 파일 로테이션 및 보관 정책
- 시스템 및 애플리케이션 로그 분리

### 느린 쿼리 모니터링  
- `SlowQueryLogger` 클래스로 성능 이슈 추적
- 임계값 초과 쿼리 자동 로깅

## 🧪 테스트

```bash
# 단위 테스트 실행
mvn test

# 통합 테스트 실행  
mvn integration-test
```

## 📋 추가 문서

프로젝트 루트 디렉토리에서 다음 문서들을 확인할 수 있습니다:

- `README_AUTH.md` - 인증 시스템 상세 가이드
- `README_ENTITY_UNIFICATION.md` - 엔티티 통합 가이드  
- `README_REFACTORING.md` - 리팩토링 가이드
- `LOG_MANAGEMENT_GUIDE.md` - 로그 관리 가이드

## 🤝 기여 방법

1. 이 리포지토리를 Fork 합니다
2. 새로운 기능 브랜치를 생성합니다 (`git checkout -b feature/AmazingFeature`)
3. 변경사항을 커밋합니다 (`git commit -m 'Add some AmazingFeature'`)  
4. 브랜치에 Push합니다 (`git push origin feature/AmazingFeature`)
5. Pull Request를 생성합니다

## 📄 라이선스

이 프로젝트는 Apache License 2.0 하에 배포됩니다.