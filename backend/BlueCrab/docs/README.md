# 📚 BlueCrab 프로젝트 문서

## 🏗️ **프로젝트 개요**
BlueCrab는 Spring Boot 기반의 웹 애플리케이션으로, JWT 인증과 사용자 관리 기능을 제공합니다.

## 📁 **문서 구조**

### 🏛️ **아키텍처 문서** (`docs/architecture/`)
- [`README_ENTITY_UNIFICATION.md`](docs/architecture/README_ENTITY_UNIFICATION.md) - 엔티티 통합 완료 보고서
- [`SYSTEM_ARCHITECTURE.md`](docs/architecture/SYSTEM_ARCHITECTURE.md) - 시스템 아키텍처 설계서
- [`DATABASE_DESIGN.md`](docs/architecture/DATABASE_DESIGN.md) - 데이터베이스 설계서

### 🛠️ **개발 문서** (`docs/development/`)
- [`README_REFACTORING.md`](docs/development/README_REFACTORING.md) - 리팩토링 가이드
- [`CODING_STANDARDS.md`](docs/development/CODING_STANDARDS.md) - 코딩 표준
- [`SETUP_GUIDE.md`](docs/development/SETUP_GUIDE.md) - 개발 환경 설정 가이드

### 📡 **API 문서** (`docs/api/`)
- [`README_AUTH.md`](docs/api/README_AUTH.md) - 인증 API 가이드
- [`USER_API.md`](docs/api/USER_API.md) - 사용자 관리 API
- [`API_REFERENCE.md`](docs/api/API_REFERENCE.md) - 전체 API 레퍼런스

### 🚀 **운영 문서** (`docs/operations/`)
- [`LOG_MANAGEMENT_GUIDE.md`](docs/operations/LOG_MANAGEMENT_GUIDE.md) - 로그 관리 가이드
- [`DEPLOYMENT_GUIDE.md`](docs/operations/DEPLOYMENT_GUIDE.md) - 배포 가이드
- [`MONITORING_GUIDE.md`](docs/operations/MONITORING_GUIDE.md) - 모니터링 가이드

## 🚀 **빠른 시작**

### 1. 개발 환경 설정
```bash
# 프로젝트 클론
git clone <repository-url>
cd BlueCrab

# 의존성 설치
mvn clean install

# 개발 서버 실행
mvn spring-boot:run -Dspring.profiles.active=dev
```

### 2. 주요 기능
- ✅ JWT 기반 인증 시스템
- ✅ 사용자 관리 (CRUD)
- ✅ 역할 기반 접근 제어
- ✅ 로그 모니터링
- ✅ API 문서화

### 3. 기술 스택
- **Backend**: Spring Boot 2.7.x, Spring Security, JPA/Hibernate
- **Database**: Oracle Database
- **Authentication**: JWT (JSON Web Token)
- **Logging**: Log4j2
- **Build**: Maven
- **Server**: Apache Tomcat

## 📞 **연락처**
프로젝트 관련 문의사항이 있으시면 아래로 연락해주세요.

---

**마지막 업데이트**: 2025-08-27  
**버전**: 1.0.0
