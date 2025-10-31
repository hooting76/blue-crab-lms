# Blue Crab LMS 백엔드 아키텍처 분석 보고서
## PPT 발표용 상세 분석

---

## 📋 목차

1. [프로젝트 개요](#1-프로젝트-개요)
2. [기술 스택](#2-기술-스택)
3. [아키텍처 패턴](#3-아키텍처-패턴)
4. [데이터베이스 설계](#4-데이터베이스-설계)
5. [보안 아키텍처](#5-보안-아키텍처)
6. [주요 기능 모듈](#6-주요-기능-모듈)
7. [인프라 구성](#7-인프라-구성)
8. [성능 최적화](#8-성능-최적화)

---

## 1. 프로젝트 개요

### 1.1 프로젝트 정보
- **프로젝트명**: Blue Crab LMS (Learning Management System)
- **버전**: 1.0.0
- **패키징**: WAR (Web Application Archive)
- **총 Java 파일**: 326개
- **개발 기간**: 2025년 9월 1일 ~ 2025년 10월 29일

### 1.2 시스템 목적
- 학원 및 교육기관을 위한 종합 학습관리 시스템
- 강의 관리, 출결 관리, 성적 관리, 시설 예약 등 통합 서비스 제공
- 학생, 교수, 관리자 역할 분리 기반 다중 사용자 시스템

### 1.3 주요 도메인
```
├── 사용자 관리 (User Management)
├── 강의 관리 (Lecture Management)
├── 출결 관리 (Attendance Management)
├── 성적 관리 (Grade Management)
├── 학적 관리 (Registry Management)
├── 시설 예약 (Facility Reservation)
├── 게시판 (Board System)
├── 증명서 발급 (Certificate Issuance)
├── 상담 관리 (Consultation Management)
└── 알림 시스템 (Notification System)
```

---

## 2. 기술 스택

### 2.1 Core Framework

#### Spring Boot Ecosystem
```xml
기반 프레임워크: eGovFrame Boot 4.3.0
├── Spring Boot 2.7.x (Spring Framework 5.3.37)
├── Spring Boot Starter Web
├── Spring Boot Starter Data JPA
├── Spring Boot Starter Security
├── Spring Boot Starter WebSocket
├── Spring Boot Starter Mail
├── Spring Boot Starter Validation
└── Spring Boot DevTools
```

**선택 이유**:
- 전자정부 표준프레임워크 기반 (공공기관 호환성)
- 안정적인 엔터프라이즈 개발 환경
- Spring Boot의 자동 구성 + eGovFrame의 공통 컴포넌트

### 2.2 Database & Persistence

#### ORM & Database
```
ORM: Spring Data JPA (Hibernate 5.6.15.Final)
└── Database Dialect: MariaDBDialect

Database: MariaDB
└── JDBC Driver: mariadb-java-client 3.1.4

Connection Pool: HikariCP
├── Maximum Pool Size: 20
├── Minimum Idle: 5
├── Connection Timeout: 20000ms
└── Idle Timeout: 300000ms
```

**JPA 설정**:
- DDL Auto: `update` (개발 환경)
- Show SQL: `false` (프로덕션)
- Format SQL: `true`
- Circular References: `true` (AssignmentService ↔ GradeManagementService)

### 2.3 Security Stack

#### Authentication & Authorization
```
Spring Security 5.7.x
├── JWT: jjwt 0.12.6 (API + Impl + Jackson)
├── Password Encoding: Plain SHA-256 (커스텀) → BCrypt (마이그레이션 예정)
└── OAuth2 Resource Server (의존성만 추가, 현재 구성은 커스텀 JWT 필터 기반)
```

**JWT 정책**:
- Access Token: 15분 (900,000ms)
- Refresh Token: 24시간 (86,400,000ms)
- 서명 알고리즘: HMAC SHA-256

### 2.4 Cache & Session

#### Redis
```
Spring Data Redis (Lettuce Client)
├── Host: 127.0.0.1 (기본)
├── Port: 6379
├── Database: 0
├── Timeout: 2000ms
└── Connection Pool:
    ├── Max Active: 8
    ├── Max Idle: 8
    └── Min Idle: 0
```

**사용 목적**:
- JWT Refresh Token 저장
- 이메일 인증 코드 임시 저장 (TTL 관리)
- 이미지 캐싱 (프로필 이미지 등)
- 세션 없는 Stateless 아키텍처 지원

### 2.5 File Storage

#### MinIO Object Storage
```
MinIO 8.5.2
├── Endpoint: http://localhost:9000 (기본)
├── Buckets:
│   ├── profile-img (프로필 이미지)
│   ├── board-attached (게시판 첨부파일)
│   └── consultation-chats (상담 채팅 첨부)
└── 파일 정책:
    ├── Max File Size: 15MB
    ├── Max Request Size: 20MB
    ├── Max Files per Post: 5개
    └── File Expire: 30일
```

**허용 파일 형식**:
- 문서: pdf, doc, docx, xls, xlsx, ppt, pptx, txt
- 이미지: jpg, jpeg, png, gif
- 압축: zip

### 2.6 Real-time Communication

#### WebSocket
```
Spring WebSocket
└── STOMP Protocol over WebSocket
    └── 채팅, 알림 실시간 전송
```

#### Firebase Cloud Messaging (FCM)
```
Firebase Admin SDK 9.7.0
├── Push Notification (브라우저, 모바일)
├── VAPID Keys (Web Push)
└── Realtime Database URL 연동
```

### 2.7 Email Service
```
Spring Boot Starter Mail
├── SMTP Server: smtp.gmail.com
├── Port: 587 (STARTTLS)
├── Protocol: SMTP
└── Encoding: UTF-8
```

### 2.8 Development Tools
```
Lombok 1.18.34 (보일러플레이트 코드 제거)
DevTools (LiveReload + Auto Restart)
Thymeleaf (서버사이드 템플릿 엔진)
```

### 2.9 Testing
```
Spring Boot Starter Test
Selenium 4.13.0 (E2E 테스트)
```

---

## 3. 아키텍처 패턴

### 3.1 Layered Architecture (계층형 아키텍처)

```
┌─────────────────────────────────────────┐
│         Presentation Layer              │
│      (Controller, DTO, View)            │
│  - REST API 엔드포인트                    │
│  - 요청/응답 데이터 변환                   │
│  - 입력 유효성 검증                        │
└──────────────┬──────────────────────────┘
               │
┌──────────────▼──────────────────────────┐
│         Business Logic Layer            │
│         (Service, Business Logic)       │
│  - 비즈니스 규칙 처리                      │
│  - 트랜잭션 관리                          │
│  - 도메인 로직 구현                        │
└──────────────┬──────────────────────────┘
               │
┌──────────────▼──────────────────────────┐
│         Persistence Layer               │
│      (Repository, Entity, DAO)          │
│  - 데이터베이스 CRUD                       │
│  - JPA 쿼리 실행                          │
│  - 영속성 관리                            │
└──────────────┬──────────────────────────┘
               │
┌──────────────▼──────────────────────────┐
│          Database Layer                 │
│           (MariaDB)                     │
│  - 실제 데이터 저장소                      │
└─────────────────────────────────────────┘
```

### 3.2 디렉토리 구조

```
backend/BlueCrab/src/main/java/BlueCrab/com/example/
├── controller/              # REST API 엔드포인트
│   ├── AuthController.java
│   ├── AdminController.java
│   ├── UserController.java
│   ├── ProfileController.java
│   ├── FacilityReservationController.java
│   ├── ChatController.java
│   ├── ReadingRoomController.java
│   ├── Lecture/
│   │   ├── LectureController.java
│   │   ├── EnrollmentController.java
│   │   ├── AssignmentController.java
│   │   ├── StudentAttendanceController.java
│   │   └── ProfessorAttendanceController.java
│   └── Board/
│       └── BoardController.java
│
├── service/                 # 비즈니스 로직
│   ├── AuthService.java
│   ├── AdminService.java
│   ├── UserTblService.java
│   ├── ProfileService.java
│   ├── EmailService.java
│   ├── EmailVerificationService.java
│   ├── TokenBlacklistService.java
│   ├── FacilityReservationService.java
│   ├── ChatService.java
│   ├── ReadingRoomService.java
│   ├── MinIOService.java
│   ├── RedisService.java
│   ├── FirebasePushService.java
│   ├── NotificationService.java
│   ├── Lecture/
│   │   ├── LectureService.java
│   │   ├── EnrollmentService.java
│   │   ├── AssignmentService.java
│   │   ├── AttendanceService.java
│   │   ├── GradeCalculationService.java
│   │   └── GradeManagementService.java
│   └── Board/
│       └── BoardService.java
│
├── repository/              # 데이터 접근 계층
│   ├── AdminTblRepository.java
│   ├── UserTblRepository.java
│   ├── RegistryRepository.java
│   ├── FacilityReservationRepository.java
│   ├── FacilityRepository.java
│   ├── ReadingSeatRepository.java
│   ├── FcmTokenRepository.java
│   ├── NotificationRepository.java
│   ├── Lecture/
│   │   ├── LectureRepository.java
│   │   ├── EnrollmentRepository.java
│   │   ├── AssignmentRepository.java
│   │   └── AttendanceRepository.java
│   └── Board/
│       ├── BoardRepository.java
│       └── AttachmentRepository.java
│
├── entity/                  # JPA 엔티티 (도메인 모델)
│   ├── UserTbl.java         # 사용자 정보
│   ├── AdminTbl.java        # 관리자 정보
│   ├── RegistryTbl.java     # 학적 정보
│   ├── FacilityTbl.java     # 시설 정보
│   ├── FacilityReservationTbl.java  # 시설 예약
│   ├── ReadingSeat.java     # 열람실 좌석
│   ├── FcmToken.java        # FCM 토큰
│   ├── NotificationEntity.java  # 알림
│   ├── Lecture/
│   │   ├── LecTbl.java      # 강의 정보
│   │   ├── EnrollmentExtendedTbl.java  # 수강 신청
│   │   ├── AssignmentExtendedTbl.java  # 과제
│   │   ├── AttendanceRequestTbl.java   # 출결 요청
│   │   ├── Faculty.java     # 학부/단과대학
│   │   ├── Department.java  # 학과
│   │   └── CourseApplyNotice.java  # 수강신청 공지
│   └── Board/
│       ├── BoardTbl.java    # 게시판
│       └── Attachment/
│
├── dto/                     # 데이터 전송 객체
│   ├── AuthDTO.java
│   ├── UserDTO.java
│   ├── ProfileDTO.java
│   ├── FacilityDTO.java
│   ├── Lecture/
│   └── Board/
│
├── config/                  # 설정 클래스
│   ├── SecurityConfig.java  # Spring Security 설정
│   ├── AppConfig.java       # 애플리케이션 공통 설정
│   ├── WebSocketConfig.java # WebSocket 설정
│   └── RedisConfig.java     # Redis 설정
│
├── security/                # 보안 컴포넌트
│   ├── JwtAuthenticationFilter.java
│   ├── JwtAuthenticationEntryPoint.java
│   ├── CustomUserDetailsService.java
│   └── PlainSha256PasswordEncoder.java
│
└── util/                    # 유틸리티 클래스
    ├── JwtUtil.java         # JWT 토큰 생성/검증
    ├── AdminJwtTokenBuilder.java
    └── AdminTokenValidator.java
```

### 3.3 패키지 명명 규칙

**도메인 중심 패키징**:
- 주요 도메인(Lecture, Board)은 별도 패키지로 분리
- 각 패키지 내 Controller-Service-Repository-Entity 구조 유지
- 응집도(Cohesion) 향상, 결합도(Coupling) 감소

**파일 명명 규칙**:
- Controller: `{도메인}Controller.java`
- Service: `{도메인}Service.java` / `{도메인}ServiceImpl.java`
- Repository: `{도메인}Repository.java`
- Entity: `{도메인}Tbl.java` (테이블 기반)
- DTO: `{도메인}DTO.java` / `{도메인}Request.java` / `{도메인}Response.java`

---

## 4. 데이터베이스 설계

### 4.1 주요 엔티티 관계도 (ERD)

```
┌──────────────┐
│   UserTbl    │ (사용자 테이블)
├──────────────┤
│ userIdx (PK) │◄─────────┐
│ userEmail    │          │
│ userPw       │          │
│ userName     │          │ One-to-One
│ userCode     │          │
│ userStudent  │          │
└──────┬───────┘          │
       │                  │
       │ One-to-Many      │
       │              ┌───┴───────────────┐
       ▼              │ SerialCodeTable   │
┌──────────────┐      ├───────────────────┤
│ RegistryTbl  │      │ serialIdx (PK)    │
├──────────────┤      │ userIdx (FK)      │
│ regIdx (PK)  │      │ majorFacultyCode  │
│ userIdx (FK) │      │ majorDeptCode     │
│ userCode     │      │ minorFacultyCode  │
│ stdStat      │      │ minorDeptCode     │
│ cntTerm      │      └───────────────────┘
└──────────────┘

┌──────────────┐
│  AdminTbl    │ (관리자 테이블)
├──────────────┤
│ adminIdx(PK) │
│ adminId      │
│ adminSys     │
│ password     │
│ name         │
└──────────────┘

┌──────────────┐
│   LecTbl     │ (강의 테이블)
├──────────────┤
│ lecIdx (PK)  │◄─────────┐
│ lecSerial    │          │
│ lecTit       │          │ Many-to-One
│ lecProf(FK)  │          │
│ lecPoint     │          │
│ lecTime      │          ▼
│ lecRoom      │  ┌────────────────────┐
│ lecMax       │  │ EnrollmentExtended │ (수강신청)
│ lecNow       │  ├────────────────────┤
└──────────────┘  │ enrIdx (PK)        │
                  │ lecIdx (FK)        │
                  │ userIdx (FK)       │
                  │ enrStat            │
                  │ totalGrade         │
                  │ finalGradeConfirmed│
                  └─────────┬──────────┘
                            │
                            │ One-to-Many
                            ▼
                  ┌────────────────────┐
                  │ AssignmentExtended │ (과제)
                  ├────────────────────┤
                  │ assignmentIdx (PK) │
                  │ enrIdx (FK)        │
                  │ assignmentTitle    │
                  │ submitStatus       │
                  │ score              │
                  └────────────────────┘

┌──────────────┐
│ FacilityTbl  │ (시설 정보)
├──────────────┤
│ facIdx (PK)  │◄─────────┐
│ facName      │          │
│ facType      │          │ Many-to-One
│ facCapacity  │          │
└──────────────┘          │
                          │
              ┌───────────┴────────────┐
              │ FacilityReservationTbl │ (시설 예약)
              ├────────────────────────┤
              │ resIdx (PK)            │
              │ facIdx (FK)            │
              │ userIdx (FK)           │
              │ resDate                │
              │ resStartTime           │
              │ resEndTime             │
              │ resStatus              │
              └────────────────────────┘

┌──────────────┐
│  BoardTbl    │ (게시판)
├──────────────┤
│ boardIdx(PK) │◄─────────┐
│ category     │          │
│ title        │          │ One-to-Many
│ userIdx (FK) │          │
│ createdDate  │          │
└──────────────┘          │
                          │
              ┌───────────┴────────────┐
              │    Attachment          │ (첨부파일)
              ├────────────────────────┤
              │ attachmentId (PK)      │
              │ boardIdx (FK)          │
              │ fileKey                │
              │ fileName               │
              │ fileSize               │
              └────────────────────────┘

┌──────────────┐
│ ReadingSeat  │ (열람실 좌석)
├──────────────┤
│ seatId (PK)  │
│ seatNumber   │
│ status       │
│ userIdx (FK) │
│ startTime    │
│ endTime      │
└──────────────┘

┌──────────────┐
│  FcmToken    │ (FCM 토큰)
├──────────────┤
│ id (PK)      │
│ userIdx (FK) │
│ token        │
│ deviceType   │
│ createdAt    │
└──────────────┘
```

### 4.2 주요 테이블 상세

#### 4.2.1 USER_TBL (사용자)
```sql
CREATE TABLE USER_TBL (
    USER_IDX INT AUTO_INCREMENT PRIMARY KEY,
    USER_EMAIL VARCHAR(200) NOT NULL,        -- 이메일 (로그인 ID)
    USER_PW VARCHAR(200) NOT NULL,           -- 비밀번호 (해시)
    USER_NAME VARCHAR(50) NOT NULL,          -- 사용자 이름
    USER_CODE VARCHAR(50) NOT NULL,          -- 학번/교번
    USER_PHONE CHAR(11) NOT NULL,            -- 전화번호
    USER_BIRTH VARCHAR(100) NOT NULL,        -- 생년월일
    USER_STUDENT INT NOT NULL,               -- 0: 학생, 1: 교수
    USER_ZIP INT,                            -- 우편번호
    USER_FIRST_ADD VARCHAR(200),             -- 주소1
    USER_LAST_ADD VARCHAR(100),              -- 주소2
    USER_REG VARCHAR(100),                   -- 등록일
    USER_REG_IP VARCHAR(100),                -- 등록 IP
    PROFILE_IMAGE_KEY VARCHAR(255),          -- 프로필 이미지 MinIO Key
    LECTURE_EVALUATIONS LONGTEXT             -- 강의 평가 (JSON)
);
```

**특징**:
- `USER_STUDENT`: 학생(0) / 교수(1) 구분
- `PROFILE_IMAGE_KEY`: MinIO 오브젝트 스토리지 키
- `LECTURE_EVALUATIONS`: JSON 형태 강의 평가 데이터

#### 4.2.2 ADMIN_TBL (관리자)
```sql
CREATE TABLE ADMIN_TBL (
    ADMIN_IDX INT AUTO_INCREMENT PRIMARY KEY,
    ADMIN_ID VARCHAR(100) NOT NULL UNIQUE,   -- 관리자 이메일
    ADMIN_SYS INT NOT NULL DEFAULT 0,        -- 0: 일반, 1: 시스템 관리자
    ADMIN_PW VARCHAR(255) NOT NULL,          -- 비밀번호 (BCrypt)
    ADMIN_NAME VARCHAR(100) NOT NULL,        -- 관리자 이름
    ADMIN_PHONE VARCHAR(11),                 -- 전화번호
    ADMIN_OFFICE VARCHAR(11),                -- 사무실
    ADMIN_LATEST VARCHAR(100),               -- 최종 로그인 시간
    ADMIN_LATEST_IP VARCHAR(50),             -- 최종 로그인 IP
    ADMIN_REG VARCHAR(100),                  -- 등록일
    ADMIN_REG_IP VARCHAR(100)                -- 등록 IP
);
```

**특징**:
- `ADMIN_SYS`: 슈퍼 관리자 권한 (1)
- 일반 사용자와 별도 테이블 관리 (권한 분리)

#### 4.2.3 REGIST_TABLE (학적)
```sql
CREATE TABLE REGIST_TABLE (
    REG_IDX INT AUTO_INCREMENT PRIMARY KEY,
    USER_IDX INT NOT NULL,                   -- 사용자 FK
    USER_CODE VARCHAR(50) NOT NULL,          -- 학번
    JOIN_PATH VARCHAR(100) NOT NULL,         -- 입학 경로 (신규/편입/...)
    STD_STAT VARCHAR(100) NOT NULL,          -- 재학/휴학/졸업
    STD_REST_DATE VARCHAR(200),              -- 휴학 기간
    CNT_TERM INT NOT NULL DEFAULT 0,         -- 이수 학기 수
    ADMIN_NAME VARCHAR(200),                 -- 처리 관리자
    ADMIN_REG DATETIME,                      -- 처리 일시
    ADMIN_IP VARCHAR(45),                    -- 처리 IP
    FOREIGN KEY (USER_IDX) REFERENCES USER_TBL(USER_IDX)
);
```

**특징**:
- Many-to-One 관계 (학생 1명 → 여러 학적 이력)
- 학적 변경 이력 추적 (처리자, 일시, IP 기록)
- 증명서 발급 기준 데이터

#### 4.2.4 LEC_TBL (강의)
```sql
CREATE TABLE LEC_TBL (
    LEC_IDX INT AUTO_INCREMENT PRIMARY KEY,
    LEC_SERIAL VARCHAR(50) NOT NULL,         -- 강의 코드
    LEC_TIT VARCHAR(50) NOT NULL,            -- 강의명
    LEC_PROF VARCHAR(50) NOT NULL,           -- 담당 교수 USER_IDX
    LEC_POINT INT NOT NULL DEFAULT 0,        -- 학점
    LEC_TIME VARCHAR(50),                    -- 강의 시간 (월1월2수3...)
    LEC_ROOM VARCHAR(50),                    -- 강의실
    LEC_MAX INT NOT NULL DEFAULT 0,          -- 최대 정원
    LEC_NOW INT NOT NULL DEFAULT 0,          -- 현재 수강 인원
    LEC_START VARCHAR(100),                  -- 강의 시작일
    LEC_END VARCHAR(100),                    -- 강의 종료일
    LEC_MAJOR VARCHAR(10) NOT NULL,          -- 전공(1)/교양(0)
    LEC_REQUIRED VARCHAR(10) NOT NULL        -- 필수(1)/선택(0)
);
```

**특징**:
- 강의 시간 포맷: "월1월2수3수4" (요일+교시 조합)
- 정원 관리: `LEC_MAX`, `LEC_NOW`
- 전공/교양, 필수/선택 구분

#### 4.2.5 ENROLLMENT_EXTENDED_TBL (수강신청)
```sql
CREATE TABLE ENROLLMENT_EXTENDED_TBL (
    ENR_IDX INT AUTO_INCREMENT PRIMARY KEY,
    LEC_IDX INT NOT NULL,                    -- 강의 FK
    USER_IDX INT NOT NULL,                   -- 학생 FK
    ENR_STAT VARCHAR(10) NOT NULL,           -- 수강 상태
    ATTENDANCE_SCORE DECIMAL(5,2),           -- 출석 점수
    ASSIGNMENT_SCORE DECIMAL(5,2),           -- 과제 점수
    MIDTERM_SCORE DECIMAL(5,2),              -- 중간고사 점수
    FINAL_SCORE DECIMAL(5,2),                -- 기말고사 점수
    TOTAL_GRADE DECIMAL(5,2),                -- 총점
    LETTER_GRADE VARCHAR(2),                 -- 등급 (A+, A, B+, ...)
    FINAL_GRADE_CONFIRMED BOOLEAN,           -- 최종 등급 확정 여부
    FOREIGN KEY (LEC_IDX) REFERENCES LEC_TBL(LEC_IDX),
    FOREIGN KEY (USER_IDX) REFERENCES USER_TBL(USER_IDX)
);
```

**특징**:
- 성적 구성: 출석 + 과제 + 중간 + 기말
- 최종 등급 확정 시스템 (`FINAL_GRADE_CONFIRMED`)
- 학점(Letter Grade) 자동 계산

#### 4.2.6 FACILITY_RESERVATION_TBL (시설 예약)
```sql
CREATE TABLE FACILITY_RESERVATION_TBL (
    RES_IDX INT AUTO_INCREMENT PRIMARY KEY,
    FAC_IDX INT NOT NULL,                    -- 시설 FK
    USER_IDX INT NOT NULL,                   -- 예약자 FK
    RES_DATE DATE NOT NULL,                  -- 예약 날짜
    RES_START_TIME TIME NOT NULL,            -- 시작 시간
    RES_END_TIME TIME NOT NULL,              -- 종료 시간
    RES_PURPOSE TEXT,                        -- 예약 목적
    RES_STATUS VARCHAR(20) NOT NULL,         -- 상태 (PENDING/APPROVED/...)
    CREATED_AT DATETIME DEFAULT NOW(),
    UPDATED_AT DATETIME DEFAULT NOW(),
    FOREIGN KEY (FAC_IDX) REFERENCES FACILITY_TBL(FAC_IDX),
    FOREIGN KEY (USER_IDX) REFERENCES USER_TBL(USER_IDX)
);
```

**특징**:
- 시설 예약 정책: 최대 30일 전 예약, 최소 30분, 최대 8시간
- 예약 상태 관리: PENDING → APPROVED → COMPLETED
- 자동 완료 처리: 1시간 후

### 4.3 데이터베이스 설계 특징

#### 4.3.1 외래키 관계
- **User ↔ Registry**: One-to-Many (학생 1명 → 여러 학적 이력)
- **User ↔ SerialCode**: One-to-One (학생 1명 → 학과 코드 1개)
- **Lecture ↔ Enrollment**: One-to-Many (강의 1개 → 여러 수강생)
- **Enrollment ↔ Assignment**: One-to-Many (수강 1건 → 여러 과제)
- **Facility ↔ Reservation**: One-to-Many (시설 1개 → 여러 예약)
- **Board ↔ Attachment**: One-to-Many (게시글 1개 → 여러 첨부파일)

#### 4.3.2 인덱스 전략
- **기본키 자동 인덱스**: 모든 PK는 AUTO_INCREMENT + 클러스터드 인덱스
- **외래키 인덱스**: JPA가 외래키 필드에 자동 인덱스 생성
- **유니크 제약**: `ADMIN_ID` (관리자 이메일)

#### 4.3.3 정규화 vs 비정규화
**정규화**:
- 사용자, 강의, 수강신청 등 핵심 테이블은 3NF 수준
- 중복 데이터 최소화, 데이터 무결성 보장

**의도적 비정규화**:
- `RegistryTbl.userCode`: USER_TBL의 USER_CODE 중복 저장 (조회 성능)
- `UserTbl.lectureEvaluations`: JSON LONGTEXT (NoSQL 스타일 저장)

---

## 5. 보안 아키텍처

### 5.1 인증 (Authentication) 플로우

#### 5.1.1 사용자 로그인 플로우
```
[클라이언트]                [백엔드]
     │
     ├─POST /api/auth/login
     │  {email, password}
     │                         ┌──────────────────────┐
     │                         │ AuthController       │
     │                         └──────┬───────────────┘
     │                                │
     │                         ┌──────▼───────────────┐
     │                         │ AuthService          │
     │                         │  1. email로 user 조회 │
     │                         │  2. 비밀번호 검증      │
     │                         │     (SHA-256 해시)    │
     │                         └──────┬───────────────┘
     │                                │
     │                         ┌──────▼───────────────┐
     │                         │ JwtUtil              │
     │                         │  1. Access Token 생성 │
     │                         │     (15분 유효)       │
     │                         │  2. Refresh Token 생성│
     │                         │     (24시간 유효)     │
     │                         └──────┬───────────────┘
     │                                │
     │                         ┌──────▼───────────────┐
     │                         │ RedisService         │
     │                         │  Refresh Token 저장   │
     │                         │  (TTL: 24시간)       │
     │                         └──────┬───────────────┘
     │                                │
     ◄────────────────────────────────┘
     │  Response:
     │  {
     │    accessToken: "eyJhbGc...",
     │    refreshToken: "eyJhbGc...",
     │    userId: 123,
     │    email: "user@example.com"
     │  }
     │
```

#### 5.1.2 인증된 API 요청 플로우
```
[클라이언트]                [백엔드]
     │
     ├─GET /api/profile/me
     │  Header: Authorization: Bearer {accessToken}
     │                         ┌──────────────────────┐
     │                         │ JwtAuthenticationFilter│
     │                         │  1. Bearer 토큰 추출   │
     │                         │  2. JWT 파싱          │
     │                         │  3. 서명 검증         │
     │                         │  4. 만료 체크         │
     │                         └──────┬───────────────┘
     │                                │
     │                         ┌──────▼───────────────┐
     │                         │ CustomUserDetailsService│
     │                         │  1. email로 User 조회  │
     │                         │  2. 권한(Role) 조회    │
     │                         │  3. UserDetails 생성   │
     │                         └──────┬───────────────┘
     │                                │
     │                         ┌──────▼───────────────┐
     │                         │ SecurityContext 설정  │
     │                         │  Authentication 저장   │
     │                         └──────┬───────────────┘
     │                                │
     │                         ┌──────▼───────────────┐
     │                         │ ProfileController     │
     │                         │  @PreAuthorize 체크   │
     │                         │  비즈니스 로직 실행    │
     │                         └──────┬───────────────┘
     │                                │
     ◄────────────────────────────────┘
     │  Response: { profile data }
     │
```

#### 5.1.3 토큰 갱신 플로우
```
[클라이언트]                [백엔드]
     │
     ├─POST /api/auth/refresh
     │  {refreshToken}
     │                         ┌──────────────────────┐
     │                         │ AuthController       │
     │                         └──────┬───────────────┘
     │                                │
     │                         ┌──────▼───────────────┐
     │                         │ RedisService         │
     │                         │  1. Refresh Token 검증│
     │                         │  2. Redis 저장소 확인 │
     │                         └──────┬───────────────┘
     │                                │
     │                         ┌──────▼───────────────┐
     │                         │ JwtUtil              │
     │                         │  새 Access Token 발급 │
     │                         └──────┬───────────────┘
     │                                │
     ◄────────────────────────────────┘
     │  Response:
     │  {newAccessToken: "eyJhbGc..."}
     │
```

### 5.2 관리자 인증 (2단계 이메일 인증)

#### 5.2.1 관리자 로그인 플로우
```
Step 1: 이메일 제출
[클라이언트] → POST /api/admin/login
                  {email, password}
                  ↓
             [AdminController]
                  ↓
             [AdminService]
             - 이메일로 AdminTbl 조회
             - 비밀번호 검증 (BCrypt)
             - 6자리 인증코드 생성 (Random)
                  ↓
             [EmailService]
             - 인증코드 이메일 발송
                  ↓
             [RedisService]
             - 인증코드 저장 (TTL: 5분)
             - Key: "admin:verify:{email}"
                  ↓
             Response: {step: "EMAIL_SENT"}

Step 2: 인증코드 검증
[클라이언트] → POST /api/admin/verify-email
                  {email, code}
                  ↓
             [AdminEmailAuthController]
                  ↓
             [RedisService]
             - Redis에서 인증코드 조회
             - 코드 일치 확인
                  ↓
             [AdminJwtTokenBuilder]
             - Admin용 JWT 생성
             - adminId, adminSys 포함
                  ↓
             Response:
             {
               accessToken: "eyJhbGc...",
               refreshToken: "eyJhbGc...",
               adminId: "prof01@example.com",
               isSuperAdmin: false
             }
```

### 5.3 권한 (Authorization) 시스템

#### 5.3.1 역할(Role) 정의
```java
// CustomUserDetailsService.java
public UserDetails loadUserByUsername(String email) {
    List<GrantedAuthority> authorities = new ArrayList<>();

    // 1. 기본 역할
    authorities.add(new SimpleGrantedAuthority("ROLE_USER"));

    // 2. 학생/교수 역할
    if (user.getUserStudent() == 0) {
        authorities.add(new SimpleGrantedAuthority("ROLE_STUDENT"));
    } else if (user.getUserStudent() == 1) {
        authorities.add(new SimpleGrantedAuthority("ROLE_PROFESSOR"));
    }

    // 3. 관리자 역할 (이메일 prefix 또는 AdminTbl)
    if (email.startsWith("prof01") || isInAdminTable(email)) {
        authorities.add(new SimpleGrantedAuthority("ROLE_ADMIN"));
    }

    // 4. 슈퍼 관리자 (adminSys > 0)
    if (adminSys > 0) {
        authorities.add(new SimpleGrantedAuthority("ROLE_SUPER_ADMIN"));
    }

    return new User(email, password, authorities);
}
```

**역할 계층**:
```
ROLE_SUPER_ADMIN (슈퍼 관리자)
    │
    ├── ROLE_ADMIN (일반 관리자)
    │       │
    │       ├── ROLE_PROFESSOR (교수)
    │       │       │
    │       │       └── ROLE_USER (기본 사용자)
    │       │
    │       └── ROLE_STUDENT (학생)
    │               │
    │               └── ROLE_USER (기본 사용자)
    │
    └── ROLE_USER (모든 인증된 사용자)
```

#### 5.3.2 엔드포인트 권한 매핑
```java
// SecurityConfig.java
http.authorizeHttpRequests(auth -> auth
    // Public 엔드포인트
    .antMatchers("/api/auth/**").permitAll()
    .antMatchers("/api/admin/login").permitAll()
    .antMatchers("/api/admin/verify-email").permitAll()
    .antMatchers("/ws/**").permitAll()

    // 인증 필요
    .antMatchers("/api/profile/me/**").authenticated()
    .antMatchers("/api/consultation/**").authenticated()

    // 역할 기반 권한
    .antMatchers("/api/attendance/approve").hasAnyRole("PROFESSOR", "ADMIN")
    .antMatchers("/api/consultation/approve").hasAnyRole("PROFESSOR", "ADMIN")
    .antMatchers("/notice/course-apply/save").hasAnyRole("ADMIN", "PROFESSOR")

    // 관리자 전용
    .antMatchers("/api/admin/**").hasRole("ADMIN")

    // 나머지 모든 요청
    .anyRequest().authenticated()
);
```

#### 5.3.3 메서드 레벨 보안
```java
// @EnableMethodSecurity(prePostEnabled = true) in SecurityConfig

@RestController
public class AttendanceController {

    @PreAuthorize("hasAnyRole('PROFESSOR', 'ADMIN')")
    @PostMapping("/api/attendance/approve")
    public ResponseEntity<?> approveAttendance(@RequestBody ApproveRequest request) {
        // 교수 또는 관리자만 접근 가능
    }

    @PreAuthorize("hasRole('STUDENT')")
    @PostMapping("/api/attendance/request")
    public ResponseEntity<?> requestAttendance(@RequestBody AttendanceRequest request) {
        // 학생만 접근 가능
    }
}
```

### 5.4 JWT 토큰 상세 구조

#### 5.4.1 Access Token 구조
```json
{
  "header": {
    "alg": "HS256",
    "typ": "JWT"
  },
  "payload": {
    "userId": 123,                          // 사용자 ID
    "sub": "user@example.com",              // 이메일 (username)
    "type": "access",                       // 토큰 타입
    "iat": 1704067200,                      // 발급 시간
    "exp": 1704068100                       // 만료 시간 (15분 후)
  },
  "signature": "HMAC-SHA256(base64Url(header) + '.' + base64Url(payload), secret)"
}
```

#### 5.4.2 Refresh Token 구조
```json
{
  "payload": {
    "userId": 123,
    "sub": "user@example.com",
    "type": "refresh",                      // 토큰 타입
    "iat": 1704067200,
    "exp": 1704153600                       // 만료 시간 (24시간 후)
  }
}
```

#### 5.4.3 Session Token (이메일 인증용)
```json
{
  "payload": {
    "userId": 123,
    "sub": "user@example.com",
    "type": "session",                      // 세션 토큰
    "iat": 1704067200,
    "exp": 1704067500                       // 만료 시간 (5분 후)
  }
}
```

### 5.5 보안 필터 체인

```
HTTP Request
    │
    ▼
┌─────────────────────────┐
│ CORS Filter             │ (web.xml에서 처리)
└────────┬────────────────┘
         │
    ▼
┌─────────────────────────┐
│ JwtAuthenticationFilter │
│ - Bearer 토큰 추출       │
│ - JWT 파싱 및 검증       │
│ - UserDetails 로드       │
│ - SecurityContext 설정   │
└────────┬────────────────┘
         │
    ▼
┌─────────────────────────┐
│ ExceptionTranslation    │
│ Filter                  │
└────────┬────────────────┘
         │
    ▼
┌─────────────────────────┐
│ FilterSecurityInterceptor│
│ - URL 권한 체크          │
│ - @PreAuthorize 체크     │
└────────┬────────────────┘
         │
    ▼
┌─────────────────────────┐
│ DispatcherServlet       │
│ (Controller 실행)        │
└─────────────────────────┘
```

### 5.6 보안 고려사항

#### 5.6.1 현재 구현된 보안 기능
✅ JWT 기반 Stateless 인증
✅ Access/Refresh 토큰 분리
✅ Redis를 통한 토큰 관리
✅ 역할 기반 접근 제어 (RBAC)
✅ 관리자 2단계 이메일 인증
✅ CORS 설정
✅ 비밀번호 해싱 (SHA-256)
✅ IP 주소 기록 (감사 로그)
✅ Redis 기반 Token Blacklist (로그아웃 시 Access/Refresh 무효화)

#### 5.6.2 개선 필요 사항
⚠️ SHA-256 → BCrypt/Argon2 마이그레이션 (Salt 추가)
⚠️ 관리자 역할 이메일 prefix 기반 → DB 기반으로 개선
⚠️ Rate Limiting 적용 범위 확대 (로그인 등 핵심 API에 추가 필요)
⚠️ HTTPS 강제 적용 (현재 설정 없음)
⚠️ XSS 방어 헤더 설정 (Content-Security-Policy 등)

---

## 6. 주요 기능 모듈

### 6.1 강의 관리 (Lecture Management)

#### 6.1.1 주요 기능
- 강의 개설 (관리자/교수)
- 수강 신청 (학생)
- 과제 제출 및 채점
- 출결 관리 (출석/지각/결석/공결)
- 성적 산출 및 확정

#### 6.1.2 서비스 구조
```
LectureService
├── createLecture()         # 강의 개설
├── updateLecture()         # 강의 정보 수정
├── deleteLecture()         # 강의 삭제
├── getLectureList()        # 강의 목록 조회
└── getLectureDetail()      # 강의 상세 조회

EnrollmentService
├── enrollLecture()         # 수강 신청
├── cancelEnrollment()      # 수강 취소
├── getMyEnrollments()      # 내 수강 목록
└── getEnrollmentsByLecture() # 강의별 수강생 목록

AssignmentService
├── createAssignment()      # 과제 등록 (교수)
├── submitAssignment()      # 과제 제출 (학생)
├── gradeAssignment()       # 과제 채점 (교수)
└── getAssignmentList()     # 과제 목록 조회

AttendanceService
├── requestAttendance()     # 출석 요청 (학생)
├── approveAttendance()     # 출석 승인 (교수)
├── recordAttendance()      # 출석 기록 (교수)
└── getAttendanceReport()   # 출석 현황 조회

GradeManagementService
├── calculateGrade()        # 성적 계산 (자동)
├── adjustGrade()           # 성적 조정 (교수)
├── finalizeGrade()         # 최종 등급 확정
└── getGradeReport()        # 성적 조회
```

#### 6.1.3 성적 계산 로직
```java
// GradeCalculationService.java
public void calculateTotalGrade(EnrollmentExtendedTbl enrollment) {
    double attendanceScore = enrollment.getAttendanceScore();    // 출석: 20%
    double assignmentScore = enrollment.getAssignmentScore();    // 과제: 30%
    double midtermScore = enrollment.getMidtermScore();          // 중간: 25%
    double finalScore = enrollment.getFinalScore();              // 기말: 25%

    double totalGrade = (attendanceScore * 0.2) +
                        (assignmentScore * 0.3) +
                        (midtermScore * 0.25) +
                        (finalScore * 0.25);

    enrollment.setTotalGrade(totalGrade);

    // Letter Grade 변환
    String letterGrade = convertToLetterGrade(totalGrade);
    enrollment.setLetterGrade(letterGrade);
}

private String convertToLetterGrade(double score) {
    if (score >= 95.0) return "A+";
    if (score >= 90.0) return "A";
    if (score >= 85.0) return "B+";
    if (score >= 80.0) return "B";
    if (score >= 75.0) return "C+";
    if (score >= 70.0) return "C";
    if (score >= 65.0) return "D+";
    if (score >= 60.0) return "D";
    return "F";
}
```

### 6.2 시설 예약 (Facility Reservation)

#### 6.2.1 시설 유형
- 강의실 (Classroom)
- 스터디룸 (Study Room)
- 세미나실 (Seminar Room)
- 실습실 (Lab)

#### 6.2.2 예약 정책
```java
// application.properties
reservation.policy.max-days-in-advance=30       # 최대 30일 전 예약
reservation.policy.min-duration-minutes=30      # 최소 30분
reservation.policy.max-duration-minutes=480     # 최대 8시간
reservation.policy.auto-complete-hours=1        # 1시간 후 자동 완료
```

#### 6.2.3 예약 상태 관리
```
PENDING (대기)
    ↓
APPROVED (승인)
    ↓
IN_USE (사용 중)
    ↓
COMPLETED (완료)

REJECTED (거절)
CANCELLED (취소)
```

#### 6.2.4 서비스 구조
```
FacilityReservationService
├── createReservation()         # 예약 생성
├── approveReservation()        # 예약 승인 (관리자)
├── rejectReservation()         # 예약 거절
├── cancelReservation()         # 예약 취소
├── checkAvailability()         # 시설 이용 가능 여부 체크
├── getMyReservations()         # 내 예약 목록
└── autoCompleteReservations()  # 자동 완료 처리 (스케줄러)
```

### 6.3 열람실 관리 (Reading Room)

#### 6.3.1 좌석 상태
- AVAILABLE (이용 가능)
- OCCUPIED (사용 중)
- RESERVED (예약됨)
- MAINTENANCE (정비 중)

#### 6.3.2 실시간 좌석 관리
```java
// ReadingRoomService.java
public void occupySeat(Integer seatNumber, Integer userIdx) {
    ReadingSeat seat = readingSeatRepository.findBySeatNumber(seatNumber);

    // 좌석 중복 체크
    if (seat.getStatus() == SeatStatus.OCCUPIED) {
        throw new AlreadyOccupiedException("이미 사용 중인 좌석");
    }

    // 좌석 배정
    seat.setStatus(SeatStatus.OCCUPIED);
    seat.setUserIdx(userIdx);
    seat.setStartTime(LocalDateTime.now());
    seat.setEndTime(LocalDateTime.now().plusHours(4));  // 4시간 제한

    readingSeatRepository.save(seat);

    // Firebase 알림 전송 (실시간 업데이트)
    firebasePushService.notifySeatStatusChange(seatNumber, "OCCUPIED");
}
```

### 6.4 게시판 시스템 (Board System)

#### 6.4.1 게시판 카테고리
- 공지사항 (NOTICE)
- 학사 공지 (ACADEMIC_NOTICE)
- 관리 공지 (ADMIN_NOTICE)
- 기타 공지 (ETC_NOTICE)
- 자유게시판 (FREE_BOARD)

#### 6.4.2 첨부파일 관리
```java
// Attachment 정책
- 최대 파일 크기: 15MB
- 최대 파일 개수: 5개/게시글
- 허용 확장자: pdf, doc, docx, xls, xlsx, ppt, pptx, txt, jpg, jpeg, png, gif, zip
- 저장소: MinIO (board-attached bucket)
- 만료 정책: 30일 후 자동 삭제
```

#### 6.4.3 서비스 구조
```
BoardService
├── createBoard()           # 게시글 작성
├── updateBoard()           # 게시글 수정
├── deleteBoard()           # 게시글 삭제
├── getBoardList()          # 게시글 목록 조회
├── getBoardDetail()        # 게시글 상세 조회
├── uploadAttachment()      # 첨부파일 업로드
└── deleteAttachment()      # 첨부파일 삭제

MinIOService
├── uploadFile()            # 파일 업로드
├── downloadFile()          # 파일 다운로드
├── deleteFile()            # 파일 삭제
├── generatePresignedUrl()  # 임시 다운로드 URL 생성 (15분)
└── listFiles()             # 파일 목록 조회
```

### 6.5 알림 시스템 (Notification System)

#### 6.5.1 알림 유형
- 강의 관련 (과제 등록, 성적 발표 등)
- 출결 관련 (출석 승인, 결석 경고 등)
- 시설 예약 (예약 승인/거절)
- 게시판 (공지사항 등록)
- 상담 (상담 요청 승인)

#### 6.5.2 알림 전송 채널
```
NotificationService
├── sendPushNotification()      # FCM Push (모바일/웹)
├── sendEmailNotification()     # 이메일 알림
└── sendWebSocketNotification() # WebSocket 실시간 알림

Firebase Cloud Messaging (FCM)
├── Browser Push (PWA)
└── Mobile Push (Android/iOS)

WebSocket (STOMP)
└── Real-time In-App Notification
```

#### 6.5.3 배치 알림 정책
```java
// application.properties
app.chat.notification.enabled=true
app.chat.notification.batch-window-seconds=6    # 6초 내 메시지 배치
app.chat.notification.max-messages=5            # 최대 5개 메시지 요약
app.chat.notification.push-read-receipts=false  # 읽음 알림 비활성화
```

### 6.6 증명서 발급 (Certificate Issuance)

#### 6.6.1 발급 가능 증명서
- 재학증명서 (stdStat = "재학")
- 졸업예정증명서 (stdStat = "졸업예정", cntTerm >= 7)
- 졸업증명서 (stdStat = "졸업")
- 성적증명서 (Transcript)

#### 6.6.2 발급 조건 검증
```java
// CertIssueService.java
public void issueEnrollmentCertificate(Integer userIdx) {
    RegistryTbl registry = registryRepository
        .findTopByUser_UserIdxOrderByAdminRegDescRegIdxDesc(userIdx);

    // 재학 상태 체크
    if (!"재학".equals(registry.getStdStat())) {
        throw new InvalidStatusException("재학 중인 학생만 발급 가능");
    }

    // 증명서 생성 (PDF)
    generatePdfCertificate(registry, "재학증명서");
}
```

---

## 7. 인프라 구성

### 7.1 배포 아키텍처

```
┌─────────────────────────────────────────┐
│          Client (Browser/Mobile)        │
│      - React 19.1.1 (Frontend)          │
│      - PWA (Progressive Web App)        │
└──────────────┬──────────────────────────┘
               │ HTTPS
               │
┌──────────────▼──────────────────────────┐
│        Load Balancer (Optional)         │
└──────────────┬──────────────────────────┘
               │
┌──────────────▼──────────────────────────┐
│         Apache Tomcat 9.x               │
│      (WAR Deployment)                   │
│  - Context Path: /BlueCrab-1.0.0        │
│  - Session Timeout: 30분                │
└──────────────┬──────────────────────────┘
               │
┌──────────────▼──────────────────────────┐
│      Spring Boot Application            │
│   (Blue Crab LMS Backend)               │
│  - Port: 8080 (내부)                     │
│  - Profile: dev/prod                    │
└─────┬──────┬──────┬──────┬──────────────┘
      │      │      │      │
      │      │      │      └──────────────┐
      │      │      │                     │
┌─────▼──────▼──────▼──────▼──────────┐   │
│         MariaDB                     │   │
│  - Host: <DB_HOST>:<DB_PORT>        │   │
│  - Database: blue_crab              │   │
│  - Connection Pool: HikariCP (20)   │   │
└─────────────────────────────────────┘   │
                                          │
┌─────────────────────────────────────┐   │
│         Redis 6.x                   │   │
│  - Host: 127.0.0.1:6379             │   │
│  - DB: 0                            │   │
│  - Use: Token Storage, Cache        │   │
└─────────────────────────────────────┘   │
                                          │
┌─────────────────────────────────────┐   │
│         MinIO Object Storage        │◄──┘
│  - Endpoint: localhost:9000         │
│  - Buckets: profile-img,            │
│             board-attached,         │
│             consultation-chats      │
└─────────────────────────────────────┘

┌─────────────────────────────────────┐
│    Firebase Cloud (External)        │
│  - FCM (Push Notification)          │
│  - Realtime Database                │
│  - VAPID Keys (Web Push)            │
└─────────────────────────────────────┘

┌─────────────────────────────────────┐
│      Gmail SMTP (External)          │
│  - smtp.gmail.com:587               │
│  - STARTTLS                         │
└─────────────────────────────────────┘
```

### 7.2 서버 설정

#### 7.2.1 Tomcat 설정
```xml
<!-- server.xml -->
<Connector port="8080" protocol="HTTP/1.1"
           connectionTimeout="20000"
           redirectPort="8443"
           maxThreads="200"
           minSpareThreads="25"
           enableLookups="false"
           URIEncoding="UTF-8" />

<!-- context.xml -->
<Context path="/BlueCrab-1.0.0">
    <Resource name="jdbc/bluecrabs"
              auth="Container"
              type="javax.sql.DataSource"
              maxTotal="20"
              maxIdle="10"
              maxWaitMillis="10000"
              username="${DB_USERNAME}"
              password="********"
              driverClassName="org.mariadb.jdbc.Driver"
              url="jdbc:mariadb://${DB_HOST}:${DB_PORT}/blue_crab" />
</Context>
```

#### 7.2.2 JVM 옵션
```bash
JAVA_OPTS="-Xms512m -Xmx2048m \
           -XX:+UseG1GC \
           -XX:MaxGCPauseMillis=200 \
           -Dfile.encoding=UTF-8 \
           -Dspring.profiles.active=prod \
           -DGOOGLE_APPLICATION_CREDENTIALS=/path/to/firebase-service-account.json"
```

### 7.3 환경별 설정

#### 7.3.1 개발 환경 (dev)
```properties
spring.profiles.active=dev
spring.jpa.show-sql=true
spring.jpa.hibernate.ddl-auto=update
logging.level.BlueCrab.com.example=DEBUG
```

#### 7.3.2 프로덕션 환경 (prod)
```properties
spring.profiles.active=prod
spring.jpa.show-sql=false
spring.jpa.hibernate.ddl-auto=none
logging.level.BlueCrab.com.example=INFO
logging.level.org.springframework.security=WARN
```
> prod 프로필용 별도 설정 파일은 레포지토리에 포함되어 있지 않으며, 배포 환경에서 환경 변수로 주입하는 것을 전제로 한다.

---

## 8. 성능 최적화

### 8.1 데이터베이스 최적화

#### 8.1.1 Connection Pooling (HikariCP)
```properties
spring.datasource.hikari.maximum-pool-size=20
spring.datasource.hikari.minimum-idle=5
spring.datasource.hikari.idle-timeout=300000
spring.datasource.hikari.connection-timeout=20000
spring.datasource.hikari.max-lifetime=1800000
```

**효과**:
- 커넥션 재사용으로 오버헤드 감소
- 동시 요청 처리 능력 향상 (최대 20개 동시 쿼리)

#### 8.1.2 JPA 쿼리 최적화
```java
// 1. Fetch Join (N+1 문제 해결)
@Query("SELECT r FROM RegistryTbl r JOIN FETCH r.user WHERE r.user.userIdx = :userIdx")
RegistryTbl findByUserIdxWithUser(@Param("userIdx") Integer userIdx);

// 2. Lazy Loading (필요 시에만 로딩)
@OneToOne(fetch = FetchType.LAZY)
@JoinColumn(name = "USER_IDX")
private SerialCodeTable serialCodeTable;

// 3. Projection (필요한 컬럼만 조회)
public interface LectureProjection {
    Integer getLecIdx();
    String getLecTit();
    String getLecProf();
}

@Query("SELECT l.lecIdx as lecIdx, l.lecTit as lecTit, l.lecProf as lecProf FROM LecTbl l")
List<LectureProjection> findAllProjections();
```

### 8.2 캐싱 전략

#### 8.2.1 Redis 캐싱
Redis는 로그인 토큰 블랙리스트, 이메일 인증 코드, 상담/열람실 RateLimit 키 등 주요 세션 정보를 저장해 무상태 인증 구조를 유지한다.  
이미지 캐싱은 `ImageCacheService`가 MinIO 스트림을 바이트 배열로 변환해 Redis에 적재하도록 설계했으나, Spring Cache 타입 캐스팅 이슈로 `@Cacheable`은 임시 비활성화된 상태다.

```java
// ImageCacheService.java
// @Cacheable 어노테이션은 캐스팅 오류 해결 시 재활성화 예정
public byte[] getCachedImageBytes(String imageKey, InputStream imageStream) {
    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    byte[] buffer = new byte[8192];
    int bytesRead;

    while ((bytesRead = imageStream.read(buffer)) != -1) {
        outputStream.write(buffer, 0, bytesRead);
    }

    byte[] imageBytes = outputStream.toByteArray();
    logger.info("이미지 캐시 저장 - Key: {}, Size: {} bytes", imageKey, imageBytes.length);
    return imageBytes;
}
```

**Redis 캐시 설정 (application.properties)**:
```properties
spring.cache.type=redis
spring.cache.redis.time-to-live=600000      # 10분
spring.cache.redis.cache-null-values=false  # NULL 값 캐싱 안 함
```

#### 8.2.2 이미지 캐싱 설정
```properties
app.image.cache-enabled=true
app.image.cache-max-size=100        # 최대 100개 이미지
app.image.cache-expire-hours=24     # 24시간 TTL
```
> 현재는 캐시 일관성 점검을 위해 `app.image.cache-enabled` 플래그로 손쉽게 온·오프 가능하며, MinIO 업로드/삭제 시 `@CacheEvict`를 통해 수동 무효화한다.

### 8.3 비동기 처리

#### 8.3.1 이메일 발송 비동기화
현재 `EmailService`는 인증 코드 메일을 동기 방식으로 전송한다. `@Async` 기반 비동기 처리와 전용 큐는 추후 개선 항목으로 관리 중이다.

```java
@Service
public class EmailService {

    public void sendMIMEMessage(String from, String to, String subject, String text) {
        MimeMessagePreparator preparator = message -> {
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(from);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(text, true);
        };

        emailSender.send(preparator);
    }
}
```

#### 8.3.2 배치/스케줄러 처리
상담·열람실·예약 등 장기 동작 도메인은 스케줄러로 후속 정리를 수행한다. 예시는 상담 자동 종료 플로우다.

```java
// ConsultationAutoCloseScheduler.java
@Scheduled(cron = "0 0 * * * *")
public void autoEndInactiveConsultations() {
    int count = consultationRequestService.autoEndInactiveConsultations();
    if (count > 0) {
        log.info("[스케줄러] 비활성 상담 자동 종료 완료: {}건", count);
    }
}
```

### 8.4 파일 업로드 최적화

#### 8.4.1 Multipart 설정
```properties
spring.servlet.multipart.max-file-size=15MB
spring.servlet.multipart.max-request-size=20MB
spring.servlet.multipart.file-size-threshold=2KB    # 2KB 이상 디스크 저장
```

#### 8.4.2 Presigned URL 방식
게시글 첨부파일은 `MinIOFileUtil.uploadFile`을 통해 서버가 직접 업로드하고, 프로필 이미지는 Presigned GET URL로 제공한다.

```java
// MinIOFileUtil.java
public ObjectWriteResponse uploadFile(MultipartFile file, String filePath) {
    try (InputStream inputStream = file.getInputStream()) {
        PutObjectArgs args = PutObjectArgs.builder()
            .bucket(bucketName)
            .object(filePath)
            .stream(inputStream, file.getSize(), -1)
            .contentType(file.getContentType())
            .build();

        return minioClient.putObject(args);
    }
}

// MinIOService.java
public String getProfileImageUrl(String imageKey) {
    return minioClient.getPresignedObjectUrl(
        GetPresignedObjectUrlArgs.builder()
            .method(Method.GET)
            .bucket(bucketName)
            .object(buildObjectName(imageKey))
            .expiry(24, TimeUnit.HOURS)
            .build()
    );
}
```

---

## 📊 PPT 슬라이드 구성 제안

### 슬라이드 1: 표지
- 프로젝트명: Blue Crab LMS 백엔드 아키텍처
- 부제: 학습관리시스템 백엔드 기술 분석

### 슬라이드 2: 프로젝트 개요
- 시스템 목적 및 주요 도메인
- 규모: 325개 Java 파일, 다층 아키텍처

### 슬라이드 3: 기술 스택 요약
- 프레임워크: eGovFrame Boot 4.3.0 + Spring Boot
- ORM: JPA (Hibernate)
- 데이터베이스: MariaDB
- 보안: Spring Security + JWT
- 캐시: Redis
- 스토리지: MinIO
- 알림: Firebase FCM

### 슬라이드 4: 아키텍처 패턴
- 계층형 아키텍처 다이어그램
- Controller → Service → Repository → Entity
- 도메인 중심 패키징 (Lecture, Board, Facility 등)

### 슬라이드 5: 데이터베이스 설계
- 주요 엔티티 ERD
- User, Lecture, Enrollment, Facility 관계도
- 정규화 수준 및 비정규화 전략

### 슬라이드 6: 보안 아키텍처
- JWT 인증 플로우
- 역할 기반 접근 제어 (RBAC)
- 관리자 2단계 이메일 인증
- 보안 필터 체인

### 슬라이드 7: 주요 기능 모듈
- 강의 관리 (수강신청, 과제, 출결, 성적)
- 시설 예약 시스템
- 게시판 및 첨부파일 관리
- 알림 시스템 (FCM, WebSocket)

### 슬라이드 8: 인프라 구성
- 배포 아키텍처 다이어그램
- Tomcat WAR 배포
- MariaDB + Redis + MinIO 연동
- Firebase 외부 서비스 통합

### 슬라이드 9: 성능 최적화
- HikariCP 커넥션 풀링
- Redis 캐싱 전략
- JPA 쿼리 최적화 (Fetch Join, Lazy Loading)
- 스케줄러 기반 후속 처리 (상담/열람실)

### 슬라이드 10: 향후 개선 방향
- BCrypt 비밀번호 마이그레이션
- Rate Limiting 적용 범위 확대
- HTTPS 강제 적용
- 모니터링 시스템 구축

---

## 📝 결론

Blue Crab LMS 백엔드는:

✅ **안정적인 엔터프라이즈 아키텍처**
- eGovFrame + Spring Boot 기반
- 계층형 아키텍처 + 도메인 중심 설계

✅ **확장 가능한 기술 스택**
- JPA/Hibernate ORM
- Redis 캐싱
- MinIO 오브젝트 스토리지
- Firebase FCM 실시간 알림

✅ **강력한 보안 시스템**
- JWT 기반 Stateless 인증
- 역할 기반 접근 제어
- 관리자 2단계 인증

✅ **실용적인 성능 최적화**
- 커넥션 풀링 (HikariCP)
- Redis 캐싱
- 스케줄러 자동화
- JPA 쿼리 최적화

⚠️ **개선 필요 영역**
- 비밀번호 해싱 강화 (BCrypt)
- Rate Limiting 적용 범위 확대
- 보안 헤더 설정
- 모니터링 시스템

---

**작성일**: 2025년 10월 30일
**분석 대상**: Blue Crab LMS Backend v1.0.0
**총 Java 파일**: 325개
**주요 기술**: Spring Boot, JPA, MariaDB, Redis, MinIO, Firebase
