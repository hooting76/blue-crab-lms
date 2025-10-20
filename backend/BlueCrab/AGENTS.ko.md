# Blue Crab Backend Agent Guide

> **작성일**: 2025-10-21  
> **대상**: 백엔드 전담 에이전트  
> **적용 범위**: `backend/BlueCrab/` Spring Boot + Maven 모듈

---

## 1. 온보딩 체크리스트
- 루트 `README.md`와 `docs/agent-readme.md`를 훑어 레포 전체 컨텍스트와 공통 작업 원칙을 파악합니다.
- `backend/BlueCrab/README.md`에서 환경 변수, Firebase 설정, 배포 흐름을 먼저 숙지합니다.
- 현재 브랜치의 상태를 `git status`로 확인하고, 기존 변경 사항을 덮어쓰지 않도록 diff를 점검합니다.
- 작업 요청의 범위·영향도를 정리하고 간단한 표준 계획(≥2단계)을 수립합니다.
- MariaDB, Firebase 등 외부 자원 접근 권한을 확보하거나 모의값으로 대체 전략을 마련합니다.

## 2. 개발 환경 및 필수 도구

### 핵심 기술 스택
- **프레임워크**: eGovFrame Boot 4.3.0 (전자정부 표준프레임워크 기반 Spring Boot)
- **JDK**: 11+ (README 기준) / 17+ 권장, `JAVA_HOME` 설정 필수
- **빌드 도구**: Maven 3.6+ (Wrapper 미제공 → 로컬 Maven 사용)
- **패키징**: WAR (Tomcat 배포용)

### 데이터베이스 & 스토리지
- **주 데이터베이스**: MariaDB 3.1.4 (`blue_crab` 인스턴스, 접속 정보는 `backend/BlueCrab/README.md` 참고)
- **커넥션 풀**: HikariCP (최대 20개 연결)
- **ORM**: Spring Data JPA + Hibernate 5.6.15
- **객체 스토리지**: MinIO 8.5.2 (파일 저장용)

### 인증 & 보안
- **Spring Security**: JWT 기반 인증/인가 시스템
- **JWT 라이브러리**: jjwt 0.12.6 (액세스 토큰 15분, 리프레시 토큰 24시간)
- **OAuth2**: Resource Server 지원
- **비밀번호 암호화**: BCrypt

### 외부 서비스 통합
- **Redis**: 세션 관리 및 캐싱 (spring-boot-starter-data-redis)
- **Firebase Admin SDK 9.7.0**: FCM 푸시 알림, Realtime DB 연동
- **Spring Mail**: 이메일 발송 기능
- 모든 외부 서비스는 환경 변수로 자격 증명을 주입하며, JSON 키 파일을 저장소에 추가하지 않습니다.

### 주요 의존성
- **Lombok 1.18.34**: 보일러플레이트 코드 감소
- **Thymeleaf**: 서버 사이드 템플릿 엔진
- **Selenium 4.13.0**: E2E 테스트 (test scope)
- **Apache HttpClient5**: Firebase SDK 의존성

### 환경별 주의사항
- OS에 따라 TLS, MariaDB 드라이버, 한글 로케일 관련 이슈가 발생할 수 있으므로 최초 실행 시 로그를 확인합니다.
- 로컬 테스트 시 Mock/Memory DB 사용 가능 여부를 먼저 확인합니다.

## 3. 프로젝트 구조 스냅샷

### 디렉토리 구조
- `src/main/java/BlueCrab/com/example/` — 핵심 비즈니스 로직
  - `controller/` — REST API 엔드포인트 (@RestController)
  - `service/` — 비즈니스 로직 계층 (@Service)
  - `repository/` — 데이터 액세스 계층 (@Repository, JPA)
  - `entity/` — JPA 엔티티 클래스
  - `dto/` — 데이터 전송 객체 (Request/Response)
  - `config/` — 설정 클래스 (Security, DataSource, WebMvc 등)
  - `security/` — JWT 필터, UserDetailsService 등 보안 컴포넌트
  - `util/` — 유틸리티 클래스 (JwtUtil, RequestUtils 등)
  - `exception/` — 전역 예외 처리 (@ControllerAdvice)
  - `scheduler/` — 스케줄링 작업 (@Scheduled)
  - `listener/` — 이벤트 리스너 (@EventListener)
- `src/main/resources/` — `application-*.properties`, Log4j2 설정 등 환경별 설정 파일
- `src/test/java/` — 단위·통합 테스트. 새로운 기능 추가 시 가능한 범위에서 테스트를 보완합니다.
- `docs/` — 아키텍처, API, 운영 가이드. 스펙 변경 시 관련 문서를 함께 갱신합니다.
- `api-endpoints-documentation.json` — REST 스펙 단일 소스. 엔드포인트 추가/변경 시 이 파일과 프론트엔드 API 래퍼(`src/api/`)를 동기화합니다.

### 주요 도메인 모듈
프로젝트는 다음과 같은 주요 기능 영역으로 구성되어 있습니다:

#### 1. 강의 관리 (Lecture)
- **컨트롤러**: `LectureController`, `AssignmentController`, `EnrollmentController`
- **서비스**: `AssignmentService`, `EnrollmentService`, `AttendanceService`, `GradeCalculationService`, `GradeManagementService`
- **기능**: 강의 개설/관리, 과제 제출, 수강 신청, 출석 관리, 성적 계산 및 관리
- **이벤트**: `GradeUpdateEventListener` - 성적 업데이트 시 자동 처리

#### 2. 열람실 예약 시스템 (ReadingRoom)
- **리포지토리**: `ReadingSeatRepository`, `ReadingUsageLogRepository`
- **서비스**: `ReadingRoomNotificationFactory`
- **스케줄러**: `ReadingRoomPreExpiryNotifier` - 만료 임박 알림
- **기능**: 좌석 예약, 사용 로그 추적, 만료 알림

#### 3. 시설 예약 (FacilityReservation)
- **서비스**: `FacilityReservationService`, `AdminFacilityReservationService`
- **기능**: 일반 사용자 예약, 관리자 예약 관리

#### 4. 인증 & 사용자 관리 (Auth & User)
- **컨트롤러**: `AuthController`, `UserController`
- **서비스**: `AuthService`, `UserTblService`, `CustomUserDetailsService`
- **유틸**: `JwtUtil`
- **기능**: 로그인/로그아웃, 토큰 갱신, 사용자 CRUD

#### 5. 푸시 알림 (Push Notification)
- **컨트롤러**: `PushNotificationController`, `FcmTokenController`
- **서비스**: `FcmTokenService`, Firebase Admin SDK 통합
- **리포지토리**: `FcmTokenRepository`
- **기능**: FCM 토큰 관리, 푸시 알림 발송

### 레이어별 책임
신규 클래스를 추가할 때는 다음 레이어 규칙을 준수합니다:
- **Controller**: HTTP 요청/응답 처리, 입력 검증 (@Valid), 통일된 ApiResponse 형식 반환
- **Service**: 비즈니스 로직, 트랜잭션 관리 (@Transactional)
- **Repository**: 데이터베이스 쿼리, JPA 인터페이스 정의
- **Entity**: 데이터베이스 테이블 매핑, Lombok 활용
- **DTO**: API 요청/응답 객체, 엔티티와 분리하여 사용

## 4. 테스트 & 빌드 환경

### ⚠️ 중요: 로컬 테스트/빌드 제한
**이 프로젝트의 테스트와 빌드는 Eclipse IDE에서 별도로 진행됩니다.**
- 에이전트는 **로컬 환경에서 `mvn test`, `mvn clean install` 등을 실행하지 마세요.**
- 코드 검증이 필요한 경우 정적 분석(코드 리뷰)만 수행하고, 실제 빌드는 개발 팀에 위임합니다.

### 참고용 Maven 명령어 (개발 팀용)
```bash
# 의존성 다운로드 및 정적 분석 포함 빌드
mvn clean install

# 개발 프로필(기본 dev)로 애플리케이션 실행
mvn spring-boot:run -Dspring.profiles.active=dev

# 단위 테스트
mvn test

# 필요 시 통합 테스트 (환경 변수 준비 필수)
mvn integration-test
```
- 특정 테스트만 실행하려면 `-Dtest=ClassNameTest` 플래그를 사용합니다.
- Firebase 통합 테스트는 FCM 토큰과 자격 증명을 모두 설정한 후 수행합니다(README 참조).

## 5. 표준 작업 절차
- **분석**: `rg`, `mvn dependency:tree` 등으로 영향 패키지를 파악하고 변경 범위를 정의합니다.
- **설계**: 데이터 흐름, 트랜잭션 경계, 예외 처리를 검토하여 최소 변경 원칙을 유지합니다.
- **구현**: 기존 코드 스타일과 로깅 규칙(Log4j2)을 맞추며, 복잡한 로직에는 짧은 설명 주석을 추가합니다.
- **검증**: 관련 테스트를 작성·갱신하고 `mvn test`를 실행합니다. 실행이 어려울 경우 이유와 수동 검증 절차를 명시합니다.
- **문서화**: API나 공정 변경 시 `backend/BlueCrab/docs/`와 `api-endpoints-documentation.json`을 최신 상태로 유지합니다.
- **보고**: 변경 요약, 영향도, 후속 제안(예: 추가 테스트, 배포 체크리스트)을 간단히 정리해 전달합니다.

## 6. 품질 및 안전 가이드
- 코드 변경 전후로 `git diff`를 확인하고, 불필요한 포맷 변경은 피합니다.
- 환경 변수, 비밀 키, 운영 URL 등 민감 정보는 커밋하지 않고 설명만 남깁니다.
- DB 스키마 변경 시 마이그레이션 전략과 롤백 플랜을 명시하고, 테스트 DB에서 먼저 검증합니다.
- 로그 레벨은 `INFO` 이하를 기본으로 유지하고, PII가 포함될 수 있는 데이터는 마스킹하거나 로깅하지 않습니다.
- 장시간 실행되는 배치/트랜잭션은 타임아웃·재시도 정책을 확인 후 수정합니다.

## 7. eGovFrame 특수사항 가이드

### eGovFrame 핵심 개념
이 프로젝트는 **전자정부 표준프레임워크 (eGovFrame) 4.3.0**을 기반으로 합니다. Spring Boot와 유사하지만 다음 차이점을 인지해야 합니다:

#### 의존성 관리
- `pom.xml`에서 `org.egovframe.boot:org.egovframe.boot.starter.parent` 4.3.0 사용
- eGovFrame 런타임 라이브러리 (`org.egovframe.rte.*`) 자동 포함:
  - `org.egovframe.rte.ptl.mvc` - MVC 패턴 지원
  - `org.egovframe.rte.psl.dataaccess` - 데이터 액세스 추상화
  - `org.egovframe.rte.fdl.idgnr` - ID 생성 서비스
  - `org.egovframe.rte.fdl.property` - 프로퍼티 관리

#### 설정 규칙
- 환경별 설정은 `application-{profile}.properties` 형식 사용 (Spring Boot 표준과 동일)
- Log4j2 기반 로깅 사용 (Spring Boot 기본 Logback이 아님)
- eGovFrame 특화 컴포넌트 (ID 생성, Property 서비스 등) 활용 가능

#### 주의사항
- eGovFrame은 공공기관 표준이므로 보안 정책 (인증, 암호화, 로깅)을 엄격히 준수해야 합니다.
- 신규 라이브러리 추가 시 eGovFrame과의 의존성 충돌 가능성을 먼저 검토하세요.
- `commons-logging` 등 중복 로깅 프레임워크는 exclusion 처리되어 있습니다.

## 8. 외부 서비스 통합 가이드

### Firebase Admin SDK
- **목적**: FCM 푸시 알림, Realtime Database 연동
- **설정**: 환경 변수 `FIREBASE_CREDENTIALS_JSON` (JSON 문자열) 또는 `GOOGLE_APPLICATION_CREDENTIALS` (파일 경로)
- **보안**: JSON 키 파일을 저장소에 커밋하지 말고 `/opt/firebase/` 등 안전한 경로에 저장
- **테스트**: `FirebasePushServiceIntegrationTest` 실행 시 실제 FCM 토큰 필요
- **참고**: `backend/BlueCrab/README.md` "Firebase Admin SDK 설정" 섹션

### MinIO (객체 스토리지)
- **목적**: 파일 업로드/다운로드 (과제 제출물, 첨부파일 등)
- **설정**: `application-*.properties`에서 MinIO 엔드포인트, 액세스 키 설정
- **주의**: 프로덕션 환경에서는 MinIO 클러스터 또는 S3 호환 스토리지 사용 권장

### Redis
- **목적**: 세션 관리, 리프레시 토큰 저장, 캐싱
- **설정**: `application-*.properties`에서 Redis 호스트/포트 설정
- **주의**: Redis 연결 실패 시 애플리케이션이 시작되지 않으므로 로컬 환경에서는 Docker로 Redis 실행 권장
  ```bash
  docker run -d -p 6379:6379 redis:latest
  ```

### Spring Mail
- **목적**: 이메일 알림 (비밀번호 재설정, 공지사항 등)
- **설정**: SMTP 서버 정보를 `application-*.properties`에 설정
- **테스트**: 로컬 테스트 시 Mailtrap, Mailhog 등 테스트 SMTP 서버 활용

## 9. 원격 서버 접근 가이드 (MCP SSH)

### Tomcat 로그 확인
에이전트는 **MCP SSH 도구**를 활용하여 원격 Tomcat 서버의 로그를 실시간으로 확인할 수 있습니다.

#### 로그 파일 위치
```
/tomcat/apache-tomcat-9.0.108/logs
```

#### 주요 로그 파일
- `catalina.out` - Tomcat 주 로그, 애플리케이션 출력
- `localhost_access_log.YYYY-MM-DD.txt` - HTTP 요청 액세스 로그
- `catalina.YYYY-MM-DD.log` - Tomcat 시스템 로그
- `localhost.YYYY-MM-DD.log` - 로컬 호스트 관련 로그

#### MCP SSH 사용 예시
```bash
# MCP SSH를 통해 최근 로그 확인
mcp__ssh-remote__exec "tail -n 100 /tomcat/apache-tomcat-9.0.108/logs/catalina.out"

# 실시간 로그 모니터링
mcp__ssh-remote__exec "tail -f /tomcat/apache-tomcat-9.0.108/logs/catalina.out"

# 특정 에러 검색
mcp__ssh-remote__exec "grep -i 'error\|exception' /tomcat/apache-tomcat-9.0.108/logs/catalina.out | tail -n 50"
```

### 외부 데이터베이스 접근

#### 데이터베이스 연결 정보
에이전트는 SSH를 통해 다음 정보로 MariaDB에 접근할 수 있습니다:

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

#### MCP SSH를 통한 DB 쿼리 실행
```bash
# MariaDB 클라이언트로 데이터베이스 접속
mcp__ssh-remote__exec "mysql -h 121.165.24.26 -P 55511 -u KDT_project -p'Kdtkdt!1120' blue_crab -e 'SHOW TABLES;'"

# 특정 테이블 스키마 확인
mcp__ssh-remote__exec "mysql -h 121.165.24.26 -P 55511 -u KDT_project -p'Kdtkdt!1120' blue_crab -e 'DESCRIBE user_tbl;'"

# 데이터 조회 (최근 5개 레코드)
mcp__ssh-remote__exec "mysql -h 121.165.24.26 -P 55511 -u KDT_project -p'Kdtkdt!1120' blue_crab -e 'SELECT * FROM user_tbl LIMIT 5;'"
```

#### 주의사항
- **프로덕션 환경**: 데이터베이스 변경 작업은 반드시 백업 후 진행하세요.
- **민감 정보**: 커밋 메시지나 문서에 비밀번호를 노출하지 마세요.
- **쿼리 실행**: SELECT 쿼리는 안전하지만, UPDATE/DELETE는 신중히 실행하세요.
- **트랜잭션**: 중요한 작업은 트랜잭션으로 감싸고 롤백 가능하도록 준비하세요.

## 10. 백엔드 테스트 엔드포인트

### 개발/디버깅용 API 엔드포인트
프로젝트에는 개발 및 디버깅을 위한 여러 테스트 엔드포인트가 제공됩니다. 에이전트는 이들을 활용하여 시스템 상태를 확인하고 문제를 진단할 수 있습니다.

#### 1. 시스템 헬스체크 (ApiController)

**`GET /api/health`** - 인증 불필요 ✅
```bash
curl http://localhost:8080/api/health
```
**응답 정보:**
- 서버 상태 (UP/DOWN)
- 메모리 사용량 (경고 임계값: 80%)
- 데이터베이스 연결 상태
- 데이터베이스 제품명 및 URL

**`GET /api/ping`** - 인증 불필요 ✅
```bash
curl http://localhost:8080/api/ping
```
빠른 연결 테스트용 경량 엔드포인트

**`GET /api/test`** - 인증 불필요 ✅
```bash
curl http://localhost:8080/api/test
```
API 서버 연결 및 타임스탬프 반환

**`GET /api/test-auth`** - 인증 필요 🔒
JWT 토큰 인증 테스트용

#### 2. Firebase 테스트 (FirebaseTestController)
⚠️ `firebase.enabled=true` 일 때만 활성화

**`GET /api/test/firebase-status`** - 인증 불필요 ✅
```bash
curl http://localhost:8080/api/test/firebase-status
```
Firebase 초기화 상태 및 VAPID 공개키 확인

**`GET /api/test/vapid-key`** - 인증 불필요 ✅
```bash
curl http://localhost:8080/api/test/vapid-key
```
FCM Web Push용 VAPID 공개키 조회

**`POST /api/test/send-test-push`** - 테스트 키 필요
```bash
curl -X POST "http://localhost:8080/api/test/send-test-push" \
  -d "token=FCM_DEVICE_TOKEN" \
  -d "title=테스트 알림" \
  -d "body=테스트 메시지" \
  -d "testKey=test123"
```
개별 기기로 푸시 알림 테스트 전송

**`POST /api/test/send-topic-test`** - 테스트 키 필요
토픽 기반 푸시 알림 테스트

**`POST /api/test/send-data-only`** - 테스트 키 필요
Data-only 방식 푸시 (중복 알림 방지 검증용)

#### 3. 데이터베이스 테스트 (DatabaseTestController)

**`GET /api/test/lamp-table-status`**
```bash
curl http://localhost:8080/api/test/lamp-table-status
```
열람실 좌석 테이블 상태 확인:
- 전체 좌석 수
- 사용 가능/점유 좌석 수
- 샘플 데이터 (최대 5개)

**`GET /api/test/lamp-table-simple`**
```bash
curl http://localhost:8080/api/test/lamp-table-simple
```
테이블 접근 테스트 (레코드 수만 반환)

**`GET /api/test/initialize-seats`**
```bash
curl http://localhost:8080/api/test/initialize-seats
```
열람실 좌석 1~80번 초기화 (없는 좌석만 생성)

**`GET /api/test/reset-all-seats`**
```bash
curl http://localhost:8080/api/test/reset-all-seats
```
⚠️ 모든 좌석 해제 (테스트/개발 환경에서만 사용)

### 테스트 엔드포인트 활용 시나리오

#### 배포 후 시스템 검증
```bash
# 1. 서버 연결 확인
curl http://121.165.24.26:8080/api/ping

# 2. 시스템 헬스체크
curl http://121.165.24.26:8080/api/health

# 3. Firebase 초기화 확인 (Firebase 사용 시)
curl http://121.165.24.26:8080/api/test/firebase-status

# 4. 데이터베이스 연결 확인
curl http://121.165.24.26:8080/api/test/lamp-table-simple
```

#### 문제 진단 플로우
1. `/api/health` - 전반적 시스템 상태 확인
2. MCP SSH로 Tomcat 로그 확인 (`/tomcat/apache-tomcat-9.0.108/logs/catalina.out`)
3. `/api/test/lamp-table-status` - DB 연결 및 데이터 상태 확인
4. 필요 시 MCP SSH로 직접 DB 쿼리 실행

### 주의사항
- **프로덕션 환경**: `/api/test/*` 엔드포인트는 SecurityConfig에서 비활성화 권장
- **testKey**: 테스트 키는 `test123`으로 하드코딩되어 있으며, 프로덕션에서는 제거 필요
- **데이터 변경**: `initialize-seats`, `reset-all-seats`는 DB를 수정하므로 신중히 사용

---

## 10-1. 통합 API 테스트 페이지 (Swagger UI 스타일)

### 개요
프로젝트에는 **Swagger UI와 유사한 통합 API 테스트 도구**가 제공됩니다. 브라우저에서 직접 모든 API를 테스트하고 JWT 인증을 관리할 수 있습니다.

### 접속 정보
```
URL: http://{server}:{port}/status
예시: http://localhost:8080/status
      http://bluecrab.chickenkiller.com/BlueCrab-1.0.0/status
```

### 주요 기능

#### 1. 인증 & 토큰 관리
- **일반 사용자 로그인**: 이메일/비밀번호 인증
- **관리자 2단계 인증**: 자동 로그인 (이메일 인증코드 자동 처리)
- **토큰 관리**:
  - Access Token / Refresh Token 자동 저장
  - 토큰 갱신 (Refresh)
  - 로그아웃
  - 토큰 세트 저장/불러오기 (여러 계정 전환 가능)

#### 2. API 엔드포인트 테스트
**카테고리별 엔드포인트 구성:**
- **게시판**: 게시글 생성/삭제, 게시판 목록
- **관리자**: 관리자 로그인, 이메일 인증, 시설 예약 관리, 시설 예약 검색
- **사용자 · 프로필**: 프로필 조회, 내 프로필 조회, 사용자 상세, 사용자 목록 (관리자용)

**각 엔드포인트별 제공 정보:**
- HTTP 메소드 (GET/POST/PUT/DELETE)
- 요청 파라미터 템플릿
- 요청 본문 (JSON) 템플릿
- 응답 예시
- 인증 요구 여부

#### 3. 고급 기능
- **요청 본문 템플릿 관리**: 자주 사용하는 JSON 템플릿 저장/불러오기
- **요청 히스토리**: 최근 실행한 API 요청 기록 조회
- **토큰 세트 관리**: 여러 사용자 계정의 토큰을 저장하고 빠르게 전환
- **실시간 응답 표시**: JSON 응답을 구문 강조 표시

### API 템플릿 JSON 구조
API 엔드포인트 정보는 `/config/api-templates.json`에 정의되어 있으며, 다음 구조를 따릅니다:

```json
{
  "endpointKey": {
    "category": "카테고리명",
    "name": "엔드포인트 이름",
    "method": "GET|POST|PUT|DELETE",
    "url": "/api/endpoint/path",
    "requiresAuth": true,
    "body": {
      "param1": "value1",
      "param2": "value2"
    },
    "description": "엔드포인트 설명"
  }
}
```

### 신규 기능 개발 시 필수 작업

#### ⚠️ 중요: API 개발 후 테스트 페이지 업데이트 필수
새로운 API 엔드포인트를 개발한 후에는 **반드시** 다음 작업을 수행해야 합니다:

**1. API 템플릿 JSON 업데이트**
```json
// src/main/resources/static/config/api-templates.json
{
  "yourNewEndpoint": {
    "category": "적절한 카테고리",
    "name": "새로운 기능 이름",
    "method": "POST",
    "url": "/api/your/new/endpoint",
    "requiresAuth": true,
    "body": {
      "exampleParam": "exampleValue"
    },
    "description": "이 API가 수행하는 작업 설명"
  }
}
```

**2. 카테고리 정의**
기존 카테고리를 사용하거나 새 카테고리를 추가합니다:
- **게시판** - 게시글 관련 기능
- **관리자** - 관리자 전용 기능
- **사용자 · 프로필** - 사용자 정보 관리
- **강의** - 강의/과제/성적 관련
- **열람실** - 열람실 예약 관리
- **시설예약** - 시설 예약 관리
- **알림** - 푸시 알림, FCM
- **기타** - 분류되지 않은 기능

**3. 필수 체크리스트**
- [ ] `api-templates.json`에 엔드포인트 추가
- [ ] 카테고리 지정 (기존 또는 신규)
- [ ] HTTP 메소드 정확히 명시
- [ ] 인증 요구 여부 (`requiresAuth`) 설정
- [ ] 요청 본문 템플릿 작성 (POST/PUT 등)
- [ ] 엔드포인트 설명 작성
- [ ] 테스트 페이지에서 실제 동작 검증

**4. 테스트 페이지 동작 검증**
```bash
# 1. 서버 실행
mvn spring-boot:run

# 2. 브라우저에서 접속
http://localhost:8080/status

# 3. 엔드포인트 선택 드롭다운에서 새 API 확인
# 4. 템플릿 데이터로 실제 요청 테스트
# 5. 응답 확인 및 문제 수정
```

### 파일 위치
```
src/main/resources/
├── static/
│   ├── js/
│   │   ├── api-tester.js              # 메인 테스트 로직
│   │   └── api-test-standalone.js     # 독립형 버전
│   ├── css/
│   │   └── api-tester.css             # 스타일시트
│   └── config/
│       └── api-templates.json         # ⭐ API 엔드포인트 정의
└── templates/
    └── status.html                     # Thymeleaf 템플릿
```

### 개발 워크플로우
```
1. Controller에 새 엔드포인트 작성
   ↓
2. api-templates.json에 엔드포인트 정보 추가
   ↓
3. /status 페이지에서 드롭다운 확인
   ↓
4. 브라우저에서 직접 테스트
   ↓
5. 문제 발견 시 즉시 수정 및 재검증
```

### 장점
- **Swagger 설정 불필요**: 별도의 Swagger 의존성이나 어노테이션 없이 사용
- **빠른 테스트**: 브라우저에서 즉시 API 테스트 가능
- **JWT 자동 관리**: 토큰 갱신 및 헤더 자동 주입
- **프론트엔드 개발 지원**: API 스펙을 시각적으로 확인하고 테스트 가능

## 11. 문제 해결 & 참고 자료
- 실행 실패 시 **MCP SSH로 Tomcat 로그**를 먼저 확인하고, 발생 지점을 `rg`로 추적합니다.
- **테스트 엔드포인트** (`/api/health`, `/api/test/*`)로 시스템 상태를 먼저 진단합니다.
- 의존성 충돌은 `mvn dependency:tree -Dincludes=<groupId>`로 파악합니다.
- eGovFrame 관련 이슈는 [전자정부 표준프레임워크 포털](https://www.egovframe.go.kr) 참고
- 스프링 설정, 배포, 모니터링 지침은 `backend/BlueCrab/docs/operations/` 하위 문서를 먼저 확인합니다.
- 레거시 자료가 필요한 경우 `docs-legacy/2025-10-18/backend-guide/`를 참조하되 최신 정책과 충돌하지 않는지 검토합니다.

## 12. 체크리스트: 새로운 기능 추가 시

에이전트가 새로운 기능을 구현할 때 다음 항목을 체크합니다:

### 코드 작성
- [ ] 기존 패키지 구조와 네이밍 규칙 준수
- [ ] 레이어별 책임 분리 (Controller → Service → Repository)
- [ ] Lombok 어노테이션 활용 (@Getter, @Setter, @RequiredArgsConstructor 등)
- [ ] 통일된 ApiResponse 형식으로 응답 반환
- [ ] 입력 검증 (@Valid, @NotNull, @Size 등)

### 보안 & 인증
- [ ] Spring Security 설정 확인 (SecurityConfig)
- [ ] JWT 인증 필요 여부 검토
- [ ] 권한 체크 (@PreAuthorize 등)
- [ ] 민감 정보 로깅 금지

### 데이터베이스
- [ ] 엔티티 클래스 작성 (JPA, Lombok 활용)
- [ ] Repository 인터페이스 정의
- [ ] 트랜잭션 범위 설정 (@Transactional)
- [ ] DB 스키마 변경 시 마이그레이션 계획 수립

### 테스트
- [ ] 단위 테스트 작성 (`src/test/java/`)
- [ ] 통합 테스트 필요 시 환경 변수 준비
- [ ] Eclipse에서 테스트 실행 및 성공 확인 (에이전트는 로컬 mvn test 실행 금지)

### 문서화
- [ ] API 변경 시 `api-endpoints-documentation.json` 업데이트
- [ ] **중요**: `src/main/resources/static/config/api-templates.json`에 신규 API 추가 (테스트 페이지용)
- [ ] 프론트엔드 API 래퍼와 동기화 확인
- [ ] `backend/BlueCrab/docs/` 관련 문서 갱신
- [ ] 주요 변경사항은 `CHANGELOG.md` 등에 기록 (존재 시)

### 외부 서비스
- [ ] Firebase/MinIO/Redis 사용 시 환경 변수 설정 가이드 제공
- [ ] 서비스 장애 시 Fallback 전략 검토
- [ ] 연동 테스트 시나리오 작성

---

**업데이트 이력**
- 2025-10-21: eGovFrame 특수사항, 외부 서비스 통합 가이드, 체크리스트 추가
- 2025-10-21: 기술 스택 정보 업데이트 (MariaDB, 주요 도메인 모듈 추가)

필요 시 이 문서를 업데이트하여 새로운 절차, 경고, 체크리스트를 공유하고 `docs/agent-readme.md` 인덱스에도 추가 링크를 남깁니다.
