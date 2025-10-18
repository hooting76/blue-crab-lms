# Blue Crab LMS - 발표용 다이어그램 제작 가이드

이 문서는 Blue Crab LMS 프로젝트 발표를 위한 다이어그램 제작에 필요한 모든 정보를 포함하고 있습니다.

---

## 목차

1. [시스템 아키텍처 다이어그램](#1-시스템-아키텍처-다이어그램)
2. [ERD (Entity Relationship Diagram)](#2-erd-entity-relationship-diagram)
3. [패키지 구조 다이어그램](#3-패키지-구조-다이어그램)
4. [주요 기능 플로우 다이어그램](#4-주요-기능-플로우-다이어그램)
5. [API 엔드포인트 그룹 다이어그램](#5-api-엔드포인트-그룹-다이어그램)

---

## 1. 시스템 아키텍처 다이어그램

### 개요
전체 시스템의 구성요소와 상호작용을 보여주는 다이어그램

### 구성 요소

#### Frontend Layer
- **기술 스택**: React 19.1.1 + Vite 7.1.2
- **주요 라이브러리**:
  - React Router DOM 7.8.2 (라우팅)
  - Toast UI Editor 3.2.3 (에디터)
  - React Big Calendar 1.19.4 (일정 관리)
  - Firebase 12.3.0 (푸시 알림)
  - Workbox (PWA 지원)
- **배포**: 정적 파일 서빙

#### Backend Layer
- **프레임워크**: Spring Boot (eGovFrame 4.3.0 기반)
- **주요 기술**:
  - Spring Web MVC
  - Spring Data JPA
  - Spring Security
  - Spring Boot Thymeleaf
- **패키징**: WAR 파일 (Tomcat 배포)

#### Database Layer
- **DBMS**: MariaDB
- **연결**: HikariCP (Connection Pool)
- **위치**:
  - Host: 121.165.24.26:55511
  - Database: blue_crab

#### External Services Layer

**1. MinIO (Object Storage)**
- 용도: 파일 저장소
- 버킷:
  - `profile-img`: 프로필 이미지
  - `board-attached`: 게시판 첨부파일
- 엔드포인트: 환경변수 설정 (MINIO_ENDPOINT)

**2. Firebase Cloud Messaging (FCM)**
- 용도: 푸시 알림
- 설정:
  - Database URL: https://lms-project-b8489-default-rtdb.firebaseio.com
  - VAPID 키 기반 웹 푸시
- 서비스: Admin SDK 9.7.0

**3. Gmail SMTP**
- 용도: 이메일 발송
- 설정:
  - Host: smtp.gmail.com:587
  - Account: bluecrabacademy@gmail.com
  - TLS 필수

**4. Redis**
- 용도: 세션 관리, 캐싱
- 기본 포트: 6379
- TTL: 600초 (10분)

#### Security Layer
- **인증**: JWT (Access Token 15분 / Refresh Token 24시간)
- **암호화**: SHA-256 (비밀번호)
- **세션**: Redis 기반

### 다이어그램 제작 포인트
```
[Browser/Mobile Client]
         |
         | HTTPS
         v
   [React Frontend]
         |
         | REST API (JSON)
         v
   [Spring Boot Backend]
         |
    +----+----+----+----+
    |    |    |    |    |
    v    v    v    v    v
  [JWT][JPA][Mail][MinIO][FCM]
              |
              v
         [MariaDB]
              |
              v
          [Redis]
```

---

## 2. ERD (Entity Relationship Diagram)

### 데이터베이스 스키마 정보
- 위치: `tmp/blue_crab_schema.sql`
- 총 테이블 수: 약 20개

### 주요 엔티티 및 관계

#### 1. 사용자 관리 영역

**USER_TBL** (사용자)
- PK: USER_IDX
- 속성:
  - USER_EMAIL (이메일)
  - USER_PW (비밀번호)
  - USER_NAME (이름)
  - USER_CODE (학번/교번)
  - USER_STUDENT (0: 교수, 1: 학생)
  - PROFILE_IMAGE_KEY (프로필 이미지)
  - LECTURE_EVALUATIONS (강의평가 JSON)

**ADMIN_TBL** (관리자)
- PK: ADMIN_IDX
- 속성: ADMIN_ID, ADMIN_PW, ADMIN_NAME

**REGIST_TABLE** (학적 정보)
- PK: REG_IDX
- FK: USER_IDX → USER_TBL
- 속성:
  - JOIN_PATH (입학경로)
  - STD_STAT (학적상태: 재학/휴학/졸업)
  - CNT_TERM (이수학기수)
  - STD_REST_DATE (휴학기간)
- 관계: Many-to-One (USER_TBL)

**FCM_TOKEN** (푸시 토큰)
- PK: ID
- FK: USER_CODE → USER_TBL.USER_CODE
- 속성: FCM_TOKEN, CREATED_AT

#### 2. 강의 관리 영역

**LEC_TBL** (강의)
- PK: LEC_IDX
- 속성:
  - LEC_SERIAL (강의코드)
  - LEC_TIT (강의명)
  - LEC_PROF (담당교수명)
  - LEC_POINT (학점)
  - LEC_TIME (강의시간: "월1월2수3수4" 형식)
  - LEC_MANY (최대인원)
  - LEC_CURRENT (현재인원)
  - LEC_OPEN (수강신청 열림 여부)
  - LEC_MAJOR (1: 전공, 0: 교양)
  - LEC_MUST (1: 필수, 0: 선택)

**ENROLLMENT_EXTENDED_TBL** (수강신청)
- PK: ENROLLMENT_IDX
- FK:
  - LEC_IDX → LEC_TBL
  - STUDENT_IDX → USER_TBL
- 속성:
  - ENROLLMENT_DATA (JSON: 수강신청/출결/성적 정보)
- 관계:
  - Many-to-One (LEC_TBL)
  - Many-to-One (USER_TBL)

**ASSIGNMENT_EXTENDED_TBL** (과제)
- PK: ASSIGNMENT_IDX
- FK: LEC_IDX → LEC_TBL
- 속성:
  - ASSIGNMENT_DATA (JSON: 과제정보/제출목록/채점)
- 관계: Many-to-One (LEC_TBL)

**ATTENDANCE_REQUEST_TBL** (출결 정정 요청)
- PK: REQUEST_IDX
- FK:
  - ENROLLMENT_IDX → ENROLLMENT_EXTENDED_TBL
  - STUDENT_IDX → USER_TBL
- 속성: REQUEST_REASON, APPROVAL_STATUS, APPROVED_BY

**DEPARTMENT** (학과)
- PK: DEP_CODE
- 속성: DEP_NAME, FAC_CODE

**FACULTY** (학부)
- PK: FAC_CODE
- 속성: FAC_NAME

#### 3. 게시판 영역

**BOARD_TBL** (게시글)
- PK: BOARD_IDX
- 속성:
  - BOARD_CODE (0: 학교공지 / 1: 학사공지 / 2: 학과공지 / 3: 교수공지)
  - BOARD_TIT (제목)
  - BOARD_CONT (내용)
  - BOARD_FILE (첨부파일 IDX 목록, 쉼표 구분)
  - BOARD_WRITER_IDX (작성자 IDX)
  - BOARD_WRITER_TYPE (0: 교수, 1: 관리자)
  - BOARD_ON (1: 활성, 0: 비활성)

**BOARD_ATTACHMENT_TBL** (게시판 첨부파일)
- PK: ATTACHMENT_IDX
- FK: BOARD_IDX → BOARD_TBL
- 속성:
  - ORIGINAL_FILE_NAME (원본파일명)
  - FILE_PATH (MinIO 경로)
  - FILE_SIZE (파일크기)
  - MIME_TYPE (MIME 타입)
  - UPLOAD_DATE (업로드일)
  - EXPIRY_DATE (만료일)
  - IS_ACTIVE (활성화 여부)
- 관계: Many-to-One (BOARD_TBL)

#### 4. 시설 예약 영역

**FACILITY_TBL** (시설)
- PK: FACILITY_IDX
- 속성:
  - FACILITY_NAME (시설명)
  - FACILITY_TYPE (시설유형)
  - CAPACITY (수용인원)
  - LOCATION (위치)
  - AVAILABLE_EQUIPMENT (이용가능장비)
  - STATUS (상태)

**FACILITY_RESERVATION_TBL** (시설 예약)
- PK: RESERVATION_IDX
- FK: FACILITY_IDX → FACILITY_TBL
- 속성:
  - USER_CODE (예약자 학번/교번)
  - START_TIME, END_TIME (예약 시간)
  - PARTY_SIZE (인원수)
  - PURPOSE (사용목적)
  - STATUS (PENDING/APPROVED/REJECTED/COMPLETED/CANCELLED)
  - APPROVED_BY (승인자)
  - REJECTION_REASON (거부사유)
- 관계: Many-to-One (FACILITY_TBL)

**FACILITY_POLICY_TBL** (시설 정책)
- PK: POLICY_IDX
- FK: FACILITY_IDX → FACILITY_TBL
- 속성: 예약정책 (최대예약일, 최소/최대 사용시간)

**FACILITY_BLOCK_TBL** (시설 차단 시간)
- PK: BLOCK_IDX
- FK: FACILITY_IDX → FACILITY_TBL
- 속성: 블록 시작/종료 시간, 사유

**FACILITY_RESERVATION_LOG** (예약 로그)
- PK: LOG_IDX
- FK: RESERVATION_IDX → FACILITY_RESERVATION_TBL
- 속성: 상태변경 이력

#### 5. 열람실 좌석 영역

**LAMP_TBL** (열람실 좌석)
- PK: LAMP_IDX (좌석번호 1~80)
- FK: USER_CODE → USER_TBL.USER_CODE
- 속성:
  - LAMP_ON (0: 빈자리, 1: 사용중)
  - start_time (사용시작시간)
  - end_time (종료예정시간, 기본 2시간)
  - updated_at (최종 업데이트)

**READING_USAGE_LOG** (열람실 사용 로그)
- PK: LOG_IDX
- 속성: 사용 이력 기록

#### 6. 증명서 발급 영역

**CERT_ISSUE_TBL** (증명서 발급)
- PK: CERT_IDX
- FK: USER_IDX → USER_TBL
- 속성:
  - CERT_TYPE (증명서 유형)
  - ISSUE_DATE (발급일)
  - PURPOSE (용도)
  - STATUS (발급상태)

#### 7. 프로필 뷰

**PROFILE_VIEW** (프로필 뷰)
- 읽기 전용 뷰
- USER_TBL + REGIST_TABLE 조인 결과

### ERD 제작 포인트

**핵심 관계**:
1. USER_TBL ← (1:N) → REGIST_TABLE
2. USER_TBL ← (1:N) → ENROLLMENT_EXTENDED_TBL → (N:1) → LEC_TBL
3. LEC_TBL ← (1:N) → ASSIGNMENT_EXTENDED_TBL
4. BOARD_TBL ← (1:N) → BOARD_ATTACHMENT_TBL
5. FACILITY_TBL ← (1:N) → FACILITY_RESERVATION_TBL
6. USER_TBL.USER_CODE ← (1:1) → LAMP_TBL.USER_CODE

**JSON 저장 필드** (LONGTEXT):
- USER_TBL.LECTURE_EVALUATIONS
- ENROLLMENT_EXTENDED_TBL.ENROLLMENT_DATA
- ASSIGNMENT_EXTENDED_TBL.ASSIGNMENT_DATA

---

## 3. 패키지 구조 다이어그램

### Backend 패키지 구조

```
BlueCrab.com.example
├── config/                 # 설정 클래스
│   ├── SecurityConfig
│   ├── JwtConfig
│   ├── RedisConfig
│   ├── MinIOConfig
│   └── FirebaseConfig
│
├── controller/             # REST API 컨트롤러
│   ├── AuthController
│   ├── UserController
│   ├── BoardController
│   ├── FacilityController
│   ├── ReadingSeatController
│   ├── CertificateController
│   ├── Lecture/            # 강의 관련 컨트롤러
│   │   ├── LectureController
│   │   ├── EnrollmentController
│   │   ├── AssignmentController
│   │   └── AttendanceController
│   └── ... (기타 컨트롤러)
│
├── service/                # 비즈니스 로직
│   ├── AuthService
│   ├── UserService
│   ├── BoardService
│   ├── FacilityService
│   ├── ReadingSeatService
│   ├── CertificateService
│   ├── Lecture/            # 강의 관련 서비스
│   │   ├── LectureService
│   │   ├── EnrollmentService
│   │   ├── AssignmentService
│   │   └── AttendanceService
│   └── ... (기타 서비스)
│
├── repository/             # 데이터 액세스
│   ├── UserRepository
│   ├── BoardRepository
│   ├── BoardAttachmentRepository
│   ├── FacilityRepository
│   ├── FacilityReservationRepository
│   ├── ReadingSeatRepository
│   ├── Lecture/            # 강의 관련 리포지토리
│   │   ├── LectureRepository
│   │   ├── EnrollmentRepository
│   │   └── AssignmentRepository
│   └── projection/         # JPA Projection 인터페이스
│
├── entity/                 # JPA 엔티티
│   ├── UserTbl
│   ├── AdminTbl
│   ├── RegistryTbl
│   ├── BoardTbl
│   ├── BoardAttachmentTbl
│   ├── FacilityTbl
│   ├── FacilityReservationTbl
│   ├── FacilityPolicyTbl
│   ├── FacilityBlockTbl
│   ├── FacilityReservationLog
│   ├── ReadingSeat
│   ├── ReadingUsageLog
│   ├── CertIssueTbl
│   ├── FcmToken
│   ├── ProfileView
│   ├── Lecture/            # 강의 관련 엔티티
│   │   ├── LecTbl
│   │   ├── EnrollmentExtendedTbl
│   │   ├── AssignmentExtendedTbl
│   │   ├── AttendanceRequestTbl
│   │   ├── Department
│   │   └── Faculty
│   └── ... (기타 엔티티)
│
├── dto/                    # 데이터 전송 객체
│   ├── LoginRequestDto
│   ├── LoginResponseDto
│   ├── UserProfileDto
│   ├── BoardRequestDto
│   ├── BoardResponseDto
│   ├── FacilityReservationDto
│   ├── SeatReserveResponseDto
│   ├── Lecture/            # 강의 관련 DTO
│   │   ├── LectureDto
│   │   ├── EnrollmentDto
│   │   └── AssignmentDto
│   └── ... (기타 DTO)
│
├── security/               # 보안 관련
│   ├── JwtTokenProvider
│   ├── JwtAuthenticationFilter
│   └── CustomUserDetailsService
│
├── util/                   # 유틸리티
│   ├── DateUtils
│   ├── FileUtils
│   └── ValidationUtils
│
├── exception/              # 예외 처리
│   ├── CustomException
│   ├── ErrorCode
│   └── GlobalExceptionHandler
│
└── enums/                  # 열거형
    ├── ReservationStatus
    ├── BoardCode
    └── ... (기타 Enum)
```

### Frontend 주요 구조

```
src/
├── components/             # React 컴포넌트
│   ├── Auth/
│   ├── Board/
│   ├── Lecture/
│   ├── Facility/
│   ├── ReadingSeat/
│   └── Common/
│
├── pages/                  # 페이지 컴포넌트
│
├── services/               # API 호출 서비스
│   ├── authService.js
│   ├── lectureService.js
│   └── ...
│
├── hooks/                  # Custom Hooks
│
├── utils/                  # 유틸리티
│
└── firebase/               # Firebase 설정
```

---

## 4. 주요 기능 플로우 다이어그램

### 4.1 수강신청 프로세스

**참여자**:
- Student (학생)
- Frontend
- LectureController
- EnrollmentService
- LectureRepository
- EnrollmentRepository

**플로우**:

```
Student → Frontend: 수강신청 페이지 접속
Frontend → LectureController: GET /api/lectures (수강 가능 강의 조회)
LectureController → LectureService: findAvailableLectures()
LectureService → LectureRepository: findByLecOpenAndLecCurrentLessThanLecMany()
LectureRepository → LectureService: List<LecTbl>
LectureService → LectureController: List<LectureDto>
LectureController → Frontend: 강의 목록 반환
Frontend → Student: 강의 목록 표시

Student → Frontend: 강의 선택 및 수강신청 버튼 클릭
Frontend → LectureController: POST /api/enrollments {lecIdx, studentIdx}
LectureController → EnrollmentService: enrollLecture(lecIdx, studentIdx)

EnrollmentService: 1. 중복 수강 체크
EnrollmentService → EnrollmentRepository: findByLecIdxAndStudentIdx()
EnrollmentRepository → EnrollmentService: Optional<EnrollmentExtendedTbl>
EnrollmentService: 2. 시간표 중복 체크
EnrollmentService → LectureRepository: findById(lecIdx)
LectureService: 3. 정원 체크 (lecCurrent < lecMany)
EnrollmentService: 4. 수강신청 생성
EnrollmentService → EnrollmentRepository: save(new EnrollmentExtendedTbl())
EnrollmentService: 5. 강의 현재인원 증가
EnrollmentService → LectureRepository: updateLecCurrent(lecIdx, +1)
EnrollmentService → LectureController: EnrollmentDto
LectureController → Frontend: 수강신청 성공 응답
Frontend → Student: 수강신청 완료 메시지
```

**시퀀스 다이어그램 포인트**:
- 동시성 제어 필요 (정원 체크 + 신청 사이)
- 트랜잭션 처리 필요
- 실패 시 롤백 처리

### 4.2 열람실 좌석 예약 프로세스

**참여자**:
- Student (학생)
- Frontend
- ReadingSeatController
- ReadingSeatService
- ReadingSeatRepository

**플로우**:

```
Student → Frontend: 열람실 좌석 현황 페이지 접속
Frontend → ReadingSeatController: GET /api/reading-seats
ReadingSeatController → ReadingSeatService: getAllSeats()
ReadingSeatService → ReadingSeatRepository: findAll()
ReadingSeatRepository → ReadingSeatService: List<ReadingSeat>
ReadingSeatService: 만료된 좌석 자동 해제 (end_time 체크)
ReadingSeatService → ReadingSeatController: List<SeatDto>
ReadingSeatController → Frontend: 좌석 현황 반환
Frontend → Student: 좌석 배치도 표시 (빈자리/사용중)

Student → Frontend: 좌석 번호 선택 및 예약 버튼 클릭
Frontend → ReadingSeatController: POST /api/reading-seats/reserve {seatNumber, userCode}
ReadingSeatController → ReadingSeatService: reserveSeat(seatNumber, userCode)

ReadingSeatService: 1. 1인 1좌석 제한 체크
ReadingSeatService → ReadingSeatRepository: findByUserCode(userCode)
ReadingSeatRepository → ReadingSeatService: Optional<ReadingSeat>
ReadingSeatService: 2. 좌석 사용 가능 여부 체크
ReadingSeatService → ReadingSeatRepository: findById(seatNumber)
ReadingSeatRepository → ReadingSeatService: Optional<ReadingSeat>
ReadingSeatService: 3. 좌석 예약 처리
ReadingSeatService: seat.reserve(userCode) → isOccupied=1, startTime=now, endTime=now+2h
ReadingSeatService → ReadingSeatRepository: save(seat)
ReadingSeatService → ReadingSeatController: SeatReserveResponseDto
ReadingSeatController → Frontend: 예약 성공 응답
Frontend → Student: 예약 완료 (좌석번호, 종료시간 표시)
```

**시퀀스 다이어그램 포인트**:
- 실시간 좌석 현황 업데이트 (WebSocket 또는 Polling)
- 만료 시간 자동 처리 (스케줄러)
- 1인 1좌석 제한 검증

### 4.3 시설 예약 승인 워크플로우

**참여자**:
- Student (학생)
- Admin (관리자)
- Frontend
- FacilityController
- FacilityService
- FacilityReservationRepository

**플로우**:

```
# 단계 1: 예약 신청
Student → Frontend: 시설 예약 신청
Frontend → FacilityController: POST /api/facilities/reservations
FacilityController → FacilityService: createReservation(dto)
FacilityService: 1. 시설 존재 체크
FacilityService: 2. 시간 중복 체크
FacilityService: 3. 정책 검증 (최대 예약일, 사용시간)
FacilityService: 4. 예약 생성 (STATUS=PENDING)
FacilityService → FacilityReservationRepository: save(reservation)
FacilityService → FacilityController: ReservationDto
FacilityController → Frontend: 예약 신청 완료 (승인 대기)
Frontend → Student: 예약 신청 완료 메시지

# 단계 2: 관리자 승인/거부
Admin → Frontend: 예약 관리 페이지 접속
Frontend → FacilityController: GET /api/facilities/reservations?status=PENDING
FacilityController → FacilityService: getPendingReservations()
FacilityService → FacilityReservationRepository: findByStatus(PENDING)
FacilityReservationRepository → Frontend: 승인 대기 목록

Admin → Frontend: 예약 승인 또는 거부 선택
Frontend → FacilityController: PUT /api/facilities/reservations/{id}/approve
FacilityController → FacilityService: approveReservation(id, adminCode)
FacilityService: STATUS=APPROVED, approvedBy=adminCode, approvedAt=now
FacilityService → FacilityReservationRepository: save(reservation)
FacilityService → FacilityReservationLogRepository: save(log)
FacilityService → FCMService: sendApprovalNotification(userCode)
FacilityService → FacilityController: 승인 완료
FacilityController → Frontend: 승인 완료 응답
Frontend → Admin: 승인 완료 메시지

# 단계 3: 알림 수신
FCMService → Firebase: 푸시 알림 전송
Firebase → Student Device: "시설 예약이 승인되었습니다"
```

**워크플로우 다이어그램 포인트**:
- 상태 전이: PENDING → APPROVED / REJECTED
- 승인 이력 로그 저장
- 푸시 알림 연동

### 4.4 과제 제출 및 채점 프로세스

**참여자**:
- Professor (교수)
- Student (학생)
- Frontend
- AssignmentController
- AssignmentService
- AssignmentRepository

**플로우**:

```
# 단계 1: 과제 등록 (교수)
Professor → Frontend: 과제 등록 페이지
Frontend → AssignmentController: POST /api/assignments
AssignmentController → AssignmentService: createAssignment(lecIdx, assignmentDto)
AssignmentService: JSON 생성 {assignment: {title, description, dueDate, ...}, submissions: []}
AssignmentService → AssignmentRepository: save(AssignmentExtendedTbl)
AssignmentController → Frontend: 과제 등록 완료

# 단계 2: 과제 제출 (학생)
Student → Frontend: 과제 제출 페이지
Frontend → AssignmentController: POST /api/assignments/{id}/submit
AssignmentController → AssignmentService: submitAssignment(assignmentIdx, studentIdx, content, file)
AssignmentService: 1. 파일 업로드 (MinIO)
AssignmentService → MinIOService: uploadFile(file)
MinIOService → AssignmentService: filePath
AssignmentService: 2. JSON 데이터 업데이트
AssignmentService: submissions.push({studentIdx, content, filePath, submittedAt, ...})
AssignmentService → AssignmentRepository: save(assignment)
AssignmentService → FCMService: notifyProfessor(lecIdx, "과제 제출됨")
AssignmentController → Frontend: 제출 완료

# 단계 3: 채점 (교수)
Professor → Frontend: 채점 페이지
Frontend → AssignmentController: PUT /api/assignments/{id}/grade
AssignmentController → AssignmentService: gradeAssignment(assignmentIdx, studentIdx, score, feedback)
AssignmentService: submissions[i].update({score, feedback, gradedAt})
AssignmentService → AssignmentRepository: save(assignment)
AssignmentService → FCMService: notifyStudent(studentIdx, "과제 채점 완료")
AssignmentController → Frontend: 채점 완료
```

**시퀀스 다이어그램 포인트**:
- MinIO 파일 업로드 통합
- JSON 데이터 파싱 및 업데이트
- 푸시 알림 전송

---

## 5. API 엔드포인트 그룹 다이어그램

### API 목록 정보
- 위치: `tmp/api-mappings.txt`
- 총 엔드포인트 수: 100개 이상

### 도메인별 API 그룹

#### 1. 인증 및 사용자 관리
**Base Path**: `/api/auth`, `/api/users`

**주요 엔드포인트**:
- `POST /api/auth/login` - 로그인
- `POST /api/auth/logout` - 로그아웃
- `POST /api/auth/refresh` - 토큰 갱신
- `GET /api/users/me` - 내 정보 조회
- `PUT /api/users/me` - 내 정보 수정
- `POST /api/users/profile-image` - 프로필 이미지 업로드
- `GET /api/users/profile-image` - 프로필 이미지 조회

#### 2. 강의 관리
**Base Path**: `/api/lectures`, `/api/enrollments`

**주요 엔드포인트**:
- `GET /api/lectures` - 강의 목록 조회
- `GET /api/lectures/{id}` - 강의 상세 조회
- `POST /api/lectures` - 강의 등록 (교수/관리자)
- `PUT /api/lectures/{id}` - 강의 수정
- `DELETE /api/lectures/{id}` - 강의 삭제
- `POST /api/enrollments` - 수강신청
- `DELETE /api/enrollments/{id}` - 수강 취소
- `GET /api/enrollments/my` - 내 수강 목록

#### 3. 과제 관리
**Base Path**: `/api/assignments`

**주요 엔드포인트**:
- `GET /api/assignments/lecture/{lecId}` - 강의별 과제 목록
- `POST /api/assignments` - 과제 등록 (교수)
- `POST /api/assignments/{id}/submit` - 과제 제출 (학생)
- `PUT /api/assignments/{id}/grade` - 과제 채점 (교수)
- `GET /api/assignments/{id}/submissions` - 제출 목록 조회

#### 4. 출결 관리
**Base Path**: `/api/attendance`

**주요 엔드포인트**:
- `GET /api/attendance/lecture/{lecId}` - 강의별 출결 현황
- `POST /api/attendance/check` - 출석 체크 (교수)
- `POST /api/attendance/request` - 출결 정정 요청 (학생)
- `PUT /api/attendance/request/{id}/approve` - 정정 요청 승인 (교수)

#### 5. 게시판
**Base Path**: `/api/boards`

**주요 엔드포인트**:
- `GET /api/boards?code={boardCode}` - 게시글 목록 (코드별)
- `GET /api/boards/{id}` - 게시글 상세
- `POST /api/boards` - 게시글 작성
- `PUT /api/boards/{id}` - 게시글 수정
- `DELETE /api/boards/{id}` - 게시글 삭제 (soft delete)
- `POST /api/boards/{id}/attachments` - 첨부파일 업로드
- `GET /api/boards/attachments/{id}` - 첨부파일 다운로드

#### 6. 시설 예약
**Base Path**: `/api/facilities`

**주요 엔드포인트**:
- `GET /api/facilities` - 시설 목록
- `GET /api/facilities/{id}/availability` - 예약 가능 시간 조회
- `POST /api/facilities/reservations` - 예약 신청
- `GET /api/facilities/reservations/my` - 내 예약 목록
- `PUT /api/facilities/reservations/{id}/approve` - 예약 승인 (관리자)
- `PUT /api/facilities/reservations/{id}/reject` - 예약 거부 (관리자)
- `DELETE /api/facilities/reservations/{id}` - 예약 취소

#### 7. 열람실 좌석
**Base Path**: `/api/reading-seats`

**주요 엔드포인트**:
- `GET /api/reading-seats` - 전체 좌석 현황
- `POST /api/reading-seats/reserve` - 좌석 예약
- `POST /api/reading-seats/release` - 좌석 반납
- `GET /api/reading-seats/my` - 내 좌석 정보

#### 8. 학적 관리
**Base Path**: `/api/registry`

**주요 엔드포인트**:
- `GET /api/registry/me` - 내 학적 정보 조회
- `POST /api/registry` - 학적 정보 등록 (관리자)
- `PUT /api/registry/{id}` - 학적 정보 수정 (관리자)

#### 9. 증명서 발급
**Base Path**: `/api/certificates`

**주요 엔드포인트**:
- `POST /api/certificates/request` - 증명서 발급 신청
- `GET /api/certificates/my` - 내 발급 이력
- `GET /api/certificates/{id}/download` - 증명서 다운로드

#### 10. 푸시 알림
**Base Path**: `/api/fcm`

**주요 엔드포인트**:
- `POST /api/fcm/token` - FCM 토큰 등록
- `DELETE /api/fcm/token` - FCM 토큰 삭제
- `POST /api/fcm/send` - 푸시 알림 전송 (관리자)

---

## 다이어그램 제작 도구 추천

### 온라인 도구
1. **Draw.io (diagrams.net)**: 무료, 강력한 다이어그램 도구
2. **Lucidchart**: 협업 기능 우수
3. **PlantUML**: 텍스트 기반 UML 생성
4. **Mermaid**: 마크다운 기반 다이어그램

### 데스크톱 도구
1. **StarUML**: 전문 UML 도구
2. **Visual Paradigm**: 엔터프라이즈급 모델링
3. **ERDPlus**: ERD 전문 도구

---

## 추가 정보

### 주요 비즈니스 룰

1. **수강신청**:
   - 시간표 중복 불가
   - 정원 초과 불가
   - 학년 제한 적용

2. **열람실 좌석**:
   - 1인 1좌석 제한
   - 기본 사용시간 2시간
   - 종료 시간 경과 시 자동 해제

3. **시설 예약**:
   - 최대 30일 전까지 예약 가능
   - 최소 사용시간 30분
   - 최대 사용시간 8시간
   - 관리자 승인 필수

4. **증명서 발급**:
   - 재학증명서: `stdStat = "재학"`
   - 졸업예정증명서: `stdStat = "졸업예정" AND cntTerm >= 7`

### 성능 고려사항

1. **캐싱**: Redis 활용 (TTL 10분)
2. **연결 풀**: HikariCP (최대 20개)
3. **파일 저장**: MinIO 분산 스토리지
4. **푸시 알림**: FCM 비동기 처리

---

## 문서 버전 정보
- 생성일: 2025-10-18
- 작성자: Claude Code (AI Assistant)
- 프로젝트: Blue Crab LMS
- 목적: 발표용 다이어그램 제작 가이드
