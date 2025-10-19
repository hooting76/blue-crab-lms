# Blue Crab LMS 데이터베이스 스키마 문서

## 📋 개요

Blue Crab LMS (Learning Management System)는 MariaDB를 기반으로 하는 종합 교육 플랫폼입니다. 본 문서는 데이터베이스 스키마의 구조와 각 테이블의 상세 정보를 정리한 문서입니다.

**데이터베이스 정보:**
- **DBMS**: MariaDB 10.11.6
- **데이터베이스명**: blue_crab
- **호스트**: 121.165.24.26:55511
- **총 테이블 수**: 22개
- **특징**: JSON 데이터 저장, 외래키 제약조건, 트리거 활용

---

## 🏗️ 데이터베이스 구조 개요

### 주요 기능 영역별 테이블 분류

1. **사용자 관리** (User Management)
2. **강의 관리** (Lecture Management)
3. **수강신청 및 성적** (Enrollment & Grading)
4. **게시판** (Board System)
5. **시설 예약** (Facility Reservation)
6. **열람실 관리** (Reading Room)
7. **시스템 관리** (System Administration)

---

## 👥 1. 사용자 관리 (User Management)

### USER_TBL (사용자 기본 정보)
```sql
CREATE TABLE `USER_TBL` (
  `USER_IDX` int(200) NOT NULL AUTO_INCREMENT,           -- 사용자 고유 ID (PK)
  `USER_EMAIL` varchar(200) NOT NULL,                    -- 이메일 (계정)
  `USER_PW` varchar(200) NOT NULL,                       -- 비밀번호
  `USER_NAME` varchar(50) NOT NULL,                      -- 이름
  `USER_CODE` varchar(255) NOT NULL DEFAULT '',          -- 학번/교번
  `USER_PHONE` char(11) NOT NULL,                        -- 전화번호
  `USER_BIRTH` varchar(100) NOT NULL,                    -- 생년월일
  `USER_STUDENT` int(1) NOT NULL,                        -- 구분값 (0:학생, 1:교수)
  `USER_LATEST` varchar(100) DEFAULT NULL,               -- 마지막 접속일
  `USER_ZIP` int(5) DEFAULT NULL,                        -- 우편번호
  `USER_FIRST_ADD` varchar(200) DEFAULT NULL,            -- 기본주소
  `USER_LAST_ADD` varchar(100) DEFAULT NULL,             -- 상세주소
  `USER_REG` varchar(100) DEFAULT NULL,                  -- 등록일
  `USER_REG_IP` varchar(100) DEFAULT NULL,               -- 등록 IP
  `PROFILE_IMAGE_KEY` varchar(255) DEFAULT NULL,         -- 프로필 이미지 MinIO 키
  `LECTURE_EVALUATIONS` longtext DEFAULT NULL,           -- 강의 평가 데이터 (JSON)
  PRIMARY KEY (`USER_IDX`),
  KEY `idx_user_code` (`USER_CODE`)
)
```

**주요 특징:**
- 이메일을 계정으로 사용
- 학생/교수 구분 (USER_STUDENT: 0=학생, 1=교수)
- 프로필 이미지 MinIO 연동
- 강의 평가 데이터를 JSON으로 저장

### ADMIN_TBL (관리자 정보)
```sql
CREATE TABLE `ADMIN_TBL` (
  `ADMIN_IDX` int(200) NOT NULL AUTO_INCREMENT,
  `ADMIN_SYS` int(1) NOT NULL DEFAULT 0,                 -- 시스템/학사 어드민 구분
  `ADMIN_ID` varchar(50) NOT NULL,                       -- 계정 ID
  `ADMIN_PW` varchar(200) NOT NULL,                      -- 비밀번호
  `ADMIN_NAME` varchar(100) NOT NULL,                    -- 이름
  `ADMIN_PHONE` varchar(11) NOT NULL,                    -- 전화번호
  `ADMIN_OFFICE` varchar(11) NOT NULL,                   -- 사무실 번호
  `ADMIN_LATEST` varchar(100) DEFAULT NULL,              -- 마지막 접속 시간
  `ADMIN_LATEST_IP` varchar(50) DEFAULT NULL,            -- 마지막 접속 IP
  `ADMIN_REG` varchar(100) DEFAULT NULL,                 -- 등록일자
  `ADMIN_REG_IP` varchar(100) DEFAULT NULL,              -- 등록 IP
  PRIMARY KEY (`ADMIN_IDX`)
)
```

**관리자 유형:**
- `ADMIN_SYS = 0`: 학사 어드민
- `ADMIN_SYS = 1`: 시스템 어드민

### REGIST_TABLE (학적 정보)
```sql
CREATE TABLE `REGIST_TABLE` (
  `REG_IDX` int(11) NOT NULL AUTO_INCREMENT,             -- 학적 이력 ID
  `USER_IDX` int(11) NOT NULL,                           -- 학생 ID (FK)
  `USER_CODE` varchar(50) NOT NULL DEFAULT '',           -- 학번 (조회용)
  `JOIN_PATH` varchar(100) NOT NULL DEFAULT '신규',       -- 입학경로
  `STD_STAT` varchar(100) NOT NULL DEFAULT '재학',        -- 학적상태
  `STD_REST_DATE` varchar(200) DEFAULT NULL,             -- 휴학기간
  `CNT_TERM` int(11) NOT NULL DEFAULT 0,                 -- 이수완료 학기 수
  `ADMIN_NAME` varchar(200) DEFAULT NULL,                -- 처리 관리자명
  `ADMIN_REG` datetime DEFAULT NULL,                     -- 처리일시
  `ADMIN_IP` varchar(45) DEFAULT NULL,                   -- 처리 IP
  PRIMARY KEY (`REG_IDX`),
  KEY `FK_REG_USER` (`USER_IDX`),
  CONSTRAINT `FK_REG_USER` FOREIGN KEY (`USER_IDX`) REFERENCES `USER_TBL` (`USER_IDX`)
)
```

**학적 상태 예시:**
- 재학, 휴학, 졸업, 제적 등

### SERIAL_CODE_TABLE (전공/부전공 정보)
```sql
CREATE TABLE `SERIAL_CODE_TABLE` (
  `SERIAL_IDX` int(11) NOT NULL AUTO_INCREMENT,          -- 코드 ID
  `USER_IDX` int(11) NOT NULL,                           -- 사용자 ID (FK)
  `SERIAL_CODE` char(2) NOT NULL,                        -- 전공 학부 코드
  `SERIAL_SUB` char(2) NOT NULL,                         -- 전공 학과 코드
  `SERIAL_CODE_ND` char(2) DEFAULT NULL,                 -- 부전공 학부 코드
  `SERIAL_SUB_ND` char(2) DEFAULT NULL,                  -- 부전공 학과 코드
  `SERIAL_REG` varchar(50) NOT NULL,                     -- 전공 등록일
  `SERIAL_REG_ND` varchar(50) DEFAULT NULL,              -- 부전공 등록일
  PRIMARY KEY (`SERIAL_IDX`),
  UNIQUE KEY `uq_user_major` (`USER_IDX`,`SERIAL_CODE`,`SERIAL_SUB`),
  KEY `idx_user` (`USER_IDX`),
  CONSTRAINT `fk_sct_user` FOREIGN KEY (`USER_IDX`) REFERENCES `USER_TBL` (`USER_IDX`)
)
```

### FACULTY (학부 마스터)
```sql
CREATE TABLE `FACULTY` (
  `faculty_id` int(11) NOT NULL AUTO_INCREMENT,
  `faculty_code` char(2) NOT NULL,                       -- 학부 코드
  `faculty_name` varchar(50) NOT NULL,                   -- 학부명
  `established_at` year(4) NOT NULL,                     -- 설립연도
  `capacity` int(11) NOT NULL DEFAULT 0,                 -- 정원
  PRIMARY KEY (`faculty_id`),
  UNIQUE KEY `uq_faculty_code` (`faculty_code`)
)
```

### DEPARTMENT (학과 마스터)
```sql
CREATE TABLE `DEPARTMENT` (
  `dept_id` int(11) NOT NULL AUTO_INCREMENT,
  `dept_code` char(2) NOT NULL,                          -- 학과 코드
  `dept_name` varchar(100) NOT NULL,                     -- 학과명
  `faculty_id` int(11) NOT NULL,                         -- 학부 ID (FK)
  `established_at` year(4) NOT NULL,                     -- 설립연도
  `capacity` int(11) NOT NULL DEFAULT 0,                 -- 정원
  PRIMARY KEY (`dept_id`),
  UNIQUE KEY `uq_faculty_dept` (`faculty_id`,`dept_code`),
  KEY `idx_dept_code` (`dept_code`),
  CONSTRAINT `fk_dept_faculty` FOREIGN KEY (`faculty_id`) REFERENCES `FACULTY` (`faculty_id`) ON UPDATE CASCADE
)
```

---

## 📚 2. 강의 관리 (Lecture Management)

### LEC_TBL (강의 정보)
```sql
CREATE TABLE `LEC_TBL` (
  `LEC_IDX` int(200) NOT NULL AUTO_INCREMENT,            -- 강의 ID (PK)
  `LEC_SERIAL` varchar(50) NOT NULL,                     -- 강의 코드
  `LEC_TIT` varchar(50) NOT NULL,                        -- 강의명
  `LEC_PROF` varchar(50) NOT NULL,                       -- 담당 교수명
  `LEC_POINT` int(10) NOT NULL DEFAULT 0,                -- 이수 학점
  `LEC_MAJOR` int(1) NOT NULL DEFAULT 1,                 -- 전공 구분 (1:전공, 0:교양)
  `LEC_MUST` int(1) NOT NULL DEFAULT 1,                  -- 필수/선택 (1:필수, 0:선택)
  `LEC_SUMMARY` text DEFAULT NULL,                       -- 강의 개요
  `LEC_TIME` varchar(50) NOT NULL,                       -- 강의 시간
  `LEC_ASSIGN` int(1) NOT NULL DEFAULT 0,                -- 과제 유무 (1:있음, 0:없음)
  `LEC_OPEN` int(1) NOT NULL DEFAULT 0,                  -- 수강신청 상태 (1:개설, 0:미개설)
  `LEC_MANY` int(10) NOT NULL DEFAULT 0,                 -- 수강 정원
  `LEC_MCODE` varchar(50) NOT NULL,                      -- 학부 코드
  `LEC_MCODE_DEP` varchar(50) NOT NULL,                  -- 학과 코드
  `LEC_MIN` int(10) NOT NULL DEFAULT 0,                  -- 최저 학년 제한
  `LEC_REG` varchar(100) DEFAULT NULL,                   -- 등록일
  `LEC_IP` varchar(100) DEFAULT NULL,                    -- 등록 IP
  `LEC_CURRENT` int(11) DEFAULT 0,                       -- 현재 수강인원
  `LEC_YEAR` int(11) DEFAULT NULL,                       -- 대상 학년
  `LEC_SEMESTER` int(1) DEFAULT NULL,                    -- 학기 (1:1학기, 2:2학기)
  PRIMARY KEY (`LEC_IDX`)
)
```

**주요 필드 설명:**
- `LEC_OPEN`: 수강신청 가능 상태
- `LEC_CURRENT`: 실시간 수강인원 카운트
- `LEC_MANY`: 최대 수강 정원

### ASSIGNMENT_EXTENDED_TBL (과제 정보)
```sql
CREATE TABLE `ASSIGNMENT_EXTENDED_TBL` (
  `ASSIGNMENT_IDX` int(200) NOT NULL AUTO_INCREMENT,     -- 과제 ID (PK)
  `LEC_IDX` int(200) NOT NULL,                           -- 강의 ID (FK)
  `ASSIGNMENT_DATA` longtext DEFAULT NULL,               -- 과제 데이터 (JSON)
  PRIMARY KEY (`ASSIGNMENT_IDX`),
  KEY `FK_ASSIGNMENT_LEC` (`LEC_IDX`),
  CONSTRAINT `FK_ASSIGNMENT_LEC` FOREIGN KEY (`LEC_IDX`) REFERENCES `LEC_TBL` (`LEC_IDX`) ON DELETE CASCADE ON UPDATE CASCADE
)
```

**ASSIGNMENT_DATA JSON 구조 예시:**
```json
{
  "assignment": {
    "title": "데이터베이스 설계 과제",
    "description": "ERD 다이어그램 작성",
    "dueDate": "2025-12-01",
    "maxScore": 100
  },
  "submissions": [
    {
      "studentIdx": 123,
      "content": "제출 내용",
      "filePath": "/uploads/assignment_123.pdf",
      "submittedAt": "2025-11-28T10:00:00",
      "score": 95,
      "feedback": "잘 작성되었습니다."
    }
  ]
}
```

---

## 🎓 3. 수강신청 및 성적 관리 (Enrollment & Grading)

### ENROLLMENT_EXTENDED_TBL (수강신청 정보)
```sql
CREATE TABLE `ENROLLMENT_EXTENDED_TBL` (
  `ENROLLMENT_IDX` int(200) NOT NULL AUTO_INCREMENT,     -- 수강 ID (PK)
  `LEC_IDX` int(200) NOT NULL,                           -- 강의 ID (FK)
  `STUDENT_IDX` int(200) NOT NULL,                       -- 학생 ID (FK)
  `ENROLLMENT_DATA` longtext DEFAULT NULL,               -- 수강 데이터 (JSON)
  PRIMARY KEY (`ENROLLMENT_IDX`),
  KEY `FK_LEC` (`LEC_IDX`),
  KEY `FK_STUDENT` (`STUDENT_IDX`),
  CONSTRAINT `FK_ENROLLMENT_LEC` FOREIGN KEY (`LEC_IDX`) REFERENCES `LEC_TBL` (`LEC_IDX`) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT `FK_ENROLLMENT_STUDENT` FOREIGN KEY (`STUDENT_IDX`) REFERENCES `USER_TBL` (`USER_IDX`) ON DELETE CASCADE ON UPDATE CASCADE
)
```

**ENROLLMENT_DATA JSON 구조 예시:**
```json
{
  "enrollment": {
    "enrolledAt": "2025-03-01T09:00:00",
    "status": "ACTIVE"
  },
  "attendance": [
    {
      "date": "2025-03-05",
      "status": "PRESENT",
      "checkTime": "2025-03-05T09:00:00"
    }
  ],
  "grades": {
    "midterm": 85,
    "final": 92,
    "assignment": 88,
    "total": 89,
    "grade": "A"
  }
}
```

---

## 📋 4. 게시판 시스템 (Board System)

### BOARD_TBL (게시글)
```sql
CREATE TABLE `BOARD_TBL` (
  `BOARD_IDX` int(200) NOT NULL AUTO_INCREMENT,
  `BOARD_CODE` int(10) NOT NULL DEFAULT 0,              -- 게시판 유형
  `BOARD_ON` int(10) NOT NULL DEFAULT 1,                -- 공개여부 (1:공개, 0:비공개)
  `BOARD_WRITER` varchar(250) NOT NULL,                 -- 작성자명
  `BOARD_TIT` text DEFAULT NULL,                        -- 제목 (base64)
  `BOARD_CONT` text DEFAULT NULL,                       -- 내용 (base64)
  `BOARD_IMG` varchar(250) DEFAULT NULL,                -- 이미지 경로
  `BOARD_FILE` varchar(500) DEFAULT NULL,               -- 첨부파일 목록 (JSON)
  `BOARD_VIEW` int(250) NOT NULL DEFAULT 0,             -- 조회수
  `BOARD_REG` varchar(250) DEFAULT NULL,                -- 작성일
  `BOARD_LATEST` varchar(250) DEFAULT NULL,             -- 최종수정일
  `BOARD_IP` varchar(250) DEFAULT NULL,                 -- IP
  `BOARD_WRITER_IDX` int(11) NOT NULL,                  -- 작성자 ID
  `BOARD_WRITER_TYPE` int(11) NOT NULL DEFAULT 0,       -- 작성자 유형 (0:일반, 1:관리자)
  PRIMARY KEY (`BOARD_IDX`)
)
```

**게시판 유형 (BOARD_CODE):**
- 0: 학교공지
- 1: 학사공지
- 2: 학과공지
- 3: 교수공지

### BOARD_ATTACHMENT_TBL (게시글 첨부파일)
```sql
CREATE TABLE `BOARD_ATTACHMENT_TBL` (
  `ATTACHMENT_IDX` int(11) NOT NULL AUTO_INCREMENT,
  `BOARD_IDX` int(11) NOT NULL,                         -- 게시글 ID (FK)
  `ORIGINAL_FILE_NAME` varchar(255) NOT NULL,           -- 원본 파일명
  `FILE_PATH` varchar(500) NOT NULL,                    -- 저장 경로
  `FILE_SIZE` bigint(20) NOT NULL,                      -- 파일 크기
  `MIME_TYPE` varchar(100) NOT NULL,                    -- MIME 타입
  `UPLOAD_DATE` varchar(50) NOT NULL,                   -- 업로드 일자
  `IS_ACTIVE` tinyint(1) NOT NULL DEFAULT 1,            -- 활성화 여부
  `EXPIRY_DATE` varchar(50) DEFAULT NULL,               -- 만료일
  PRIMARY KEY (`ATTACHMENT_IDX`),
  KEY `idx_board_attachment_board_idx` (`BOARD_IDX`),
  KEY `idx_board_attachment_active` (`IS_ACTIVE`),
  KEY `idx_board_attachment_upload_date` (`UPLOAD_DATE`),
  KEY `idx_board_attachment_board_active` (`BOARD_IDX`,`IS_ACTIVE`),
  CONSTRAINT `BOARD_ATTACHMENT_TBL_ibfk_1` FOREIGN KEY (`BOARD_IDX`) REFERENCES `BOARD_TBL` (`BOARD_IDX`) ON DELETE CASCADE
)
```

---

## 🏢 5. 시설 예약 시스템 (Facility Reservation)

### FACILITY_TBL (시설 정보)
```sql
CREATE TABLE `FACILITY_TBL` (
  `FACILITY_IDX` int(11) NOT NULL AUTO_INCREMENT,
  `FACILITY_NAME` varchar(100) NOT NULL,                -- 시설명
  `FACILITY_TYPE` varchar(20) NOT NULL,                 -- 시설 유형
  `FACILITY_DESC` text DEFAULT NULL,                    -- 설명
  `CAPACITY` int(11) DEFAULT NULL,                      -- 수용인원
  `LOCATION` varchar(200) DEFAULT NULL,                 -- 위치
  `DEFAULT_EQUIPMENT` text DEFAULT NULL,                -- 기본 장비
  `IS_ACTIVE` tinyint(4) NOT NULL DEFAULT 1,            -- 활성화 여부
  `CREATED_AT` datetime NOT NULL DEFAULT current_timestamp(),
  `UPDATED_AT` datetime NOT NULL DEFAULT current_timestamp() ON UPDATE current_timestamp(),
  PRIMARY KEY (`FACILITY_IDX`),
  KEY `idx_facility_type_active` (`FACILITY_TYPE`,`IS_ACTIVE`),
  KEY `idx_facility_name` (`FACILITY_NAME`)
)
```

### FACILITY_RESERVATION_TBL (시설 예약)
```sql
CREATE TABLE `FACILITY_RESERVATION_TBL` (
  `RESERVATION_IDX` int(11) NOT NULL AUTO_INCREMENT,
  `FACILITY_IDX` int(11) NOT NULL,                      -- 시설 ID (FK)
  `USER_CODE` varchar(50) NOT NULL,                     -- 예약자 학번
  `USER_EMAIL` varchar(255) DEFAULT NULL,               -- 예약자 이메일
  `START_TIME` datetime NOT NULL,                       -- 시작 시간
  `END_TIME` datetime NOT NULL,                         -- 종료 시간
  `PARTY_SIZE` int(11) DEFAULT NULL,                    -- 인원 수
  `PURPOSE` text DEFAULT NULL,                          -- 사용 목적
  `REQUESTED_EQUIPMENT` text DEFAULT NULL,              -- 요청 장비
  `STATUS` varchar(20) NOT NULL DEFAULT 'PENDING',      -- 상태
  `ADMIN_NOTE` text DEFAULT NULL,                       -- 관리자 메모
  `REJECTION_REASON` text DEFAULT NULL,                 -- 거부 사유
  `APPROVED_BY` varchar(50) DEFAULT NULL,               -- 승인자
  `APPROVED_AT` datetime DEFAULT NULL,                  -- 승인 일시
  `CREATED_AT` datetime NOT NULL DEFAULT current_timestamp(),
  `UPDATED_AT` datetime NOT NULL DEFAULT current_timestamp() ON UPDATE current_timestamp(),
  PRIMARY KEY (`RESERVATION_IDX`),
  KEY `idx_reservation_facility_time` (`FACILITY_IDX`,`START_TIME`,`END_TIME`),
  KEY `idx_reservation_user_status` (`USER_CODE`,`STATUS`),
  KEY `idx_reservation_status_time` (`STATUS`,`START_TIME`),
  KEY `idx_reservation_created` (`CREATED_AT`),
  CONSTRAINT `fk_reservation_facility` FOREIGN KEY (`FACILITY_IDX`) REFERENCES `FACILITY_TBL` (`FACILITY_IDX`) ON UPDATE CASCADE
)
```

**예약 상태 (STATUS):**
- PENDING: 승인 대기
- APPROVED: 승인됨
- REJECTED: 거부됨
- CANCELLED: 취소됨

### FACILITY_POLICY_TBL (시설 정책)
```sql
CREATE TABLE `FACILITY_POLICY_TBL` (
  `POLICY_IDX` int(11) NOT NULL AUTO_INCREMENT,
  `FACILITY_IDX` int(11) NOT NULL,                      -- 시설 ID (FK)
  `REQUIRES_APPROVAL` tinyint(4) NOT NULL DEFAULT 1,    -- 승인 필요 여부
  `MIN_DURATION_MINUTES` int(11) DEFAULT NULL,          -- 최소 예약 시간(분)
  `MAX_DURATION_MINUTES` int(11) DEFAULT NULL,          -- 최대 예약 시간(분)
  `MIN_DAYS_IN_ADVANCE` int(11) DEFAULT NULL,           -- 최소 사전 예약 일수
  `MAX_DAYS_IN_ADVANCE` int(11) DEFAULT NULL,           -- 최대 사전 예약 일수
  `CANCELLATION_DEADLINE_HOURS` int(11) DEFAULT NULL,   -- 취소 마감 시간(시간)
  `MAX_RESERVATIONS_PER_USER` int(11) DEFAULT NULL,     -- 사용자당 최대 예약 수
  `ALLOW_WEEKEND_BOOKING` tinyint(1) DEFAULT NULL,      -- 주말 예약 허용
  `CREATED_AT` datetime NOT NULL DEFAULT current_timestamp(),
  `UPDATED_AT` datetime NOT NULL DEFAULT current_timestamp() ON UPDATE current_timestamp(),
  PRIMARY KEY (`POLICY_IDX`),
  UNIQUE KEY `FACILITY_IDX` (`FACILITY_IDX`),
  KEY `idx_policy_facility` (`FACILITY_IDX`),
  KEY `idx_policy_approval` (`REQUIRES_APPROVAL`),
  KEY `idx_policy_updated` (`UPDATED_AT`),
  CONSTRAINT `fk_policy_facility` FOREIGN KEY (`FACILITY_IDX`) REFERENCES `FACILITY_TBL` (`FACILITY_IDX`) ON DELETE CASCADE ON UPDATE CASCADE
)
```

### FACILITY_BLOCK_TBL (시설 차단 시간)
```sql
CREATE TABLE `FACILITY_BLOCK_TBL` (
  `BLOCK_IDX` int(11) NOT NULL AUTO_INCREMENT,
  `FACILITY_IDX` int(11) NOT NULL,                      -- 시설 ID (FK)
  `BLOCK_START` datetime NOT NULL,                      -- 차단 시작
  `BLOCK_END` datetime NOT NULL,                        -- 차단 종료
  `BLOCK_REASON` varchar(200) NOT NULL,                 -- 차단 사유
  `BLOCK_TYPE` varchar(20) DEFAULT 'MAINTENANCE',       -- 차단 유형
  `CREATED_BY` varchar(50) NOT NULL,                    -- 생성자
  `CREATED_AT` datetime NOT NULL DEFAULT current_timestamp(),
  PRIMARY KEY (`BLOCK_IDX`),
  KEY `idx_block_facility_time` (`FACILITY_IDX`,`BLOCK_START`,`BLOCK_END`),
  KEY `idx_block_time_range` (`BLOCK_START`,`BLOCK_END`),
  CONSTRAINT `fk_block_facility` FOREIGN KEY (`FACILITY_IDX`) REFERENCES `FACILITY_TBL` (`FACILITY_IDX`) ON DELETE CASCADE ON UPDATE CASCADE
)
```

### FACILITY_RESERVATION_LOG (예약 로그)
```sql
CREATE TABLE `FACILITY_RESERVATION_LOG` (
  `LOG_IDX` int(11) NOT NULL AUTO_INCREMENT,
  `RESERVATION_IDX` int(11) NOT NULL,                   -- 예약 ID (FK)
  `EVENT_TYPE` varchar(50) NOT NULL,                    -- 이벤트 유형
  `ACTOR_TYPE` varchar(20) DEFAULT NULL,                -- 액터 유형
  `ACTOR_CODE` varchar(50) DEFAULT NULL,                -- 액터 코드
  `PAYLOAD` text DEFAULT NULL,                          -- 페이로드
  `CREATED_AT` datetime NOT NULL DEFAULT current_timestamp(),
  PRIMARY KEY (`LOG_IDX`),
  KEY `idx_reservationlog_reservation` (`RESERVATION_IDX`),
  KEY `idx_reservationlog_created` (`CREATED_AT`),
  CONSTRAINT `fk_log_reservation` FOREIGN KEY (`RESERVATION_IDX`) REFERENCES `FACILITY_RESERVATION_TBL` (`RESERVATION_IDX`) ON DELETE CASCADE ON UPDATE CASCADE
)
```

---

## 📖 6. 열람실 관리 (Reading Room)

### LAMP_TBL (좌석 현황)
```sql
CREATE TABLE `LAMP_TBL` (
  `LAMP_IDX` int(11) NOT NULL COMMENT '좌석 번호 (1~80)',
  `LAMP_ON` int(11) NOT NULL DEFAULT 0 COMMENT '사용 여부 (0:빈자리, 1:사용중)',
  `USER_CODE` varchar(255) DEFAULT NULL COMMENT '현재 사용자 학번/교번',
  `start_time` datetime DEFAULT NULL COMMENT '사용 시작 시간',
  `end_time` datetime DEFAULT NULL COMMENT '예정 종료 시간 (2시간 후)',
  `updated_at` datetime DEFAULT current_timestamp() ON UPDATE current_timestamp() COMMENT '최종 업데이트 시간',
  PRIMARY KEY (`LAMP_IDX`),
  KEY `idx_user_code` (`USER_CODE`),
  KEY `idx_lamp_on` (`LAMP_ON`),
  CONSTRAINT `fk_lamp_user` FOREIGN KEY (`USER_CODE`) REFERENCES `USER_TBL` (`USER_CODE`) ON DELETE SET NULL
)
```

**특징:**
- 좌석 번호 1~80 (총 80석)
- 실시간 사용 현황 관리
- 2시간 사용 제한

### LAMP_USAGE_LOG (열람실 사용 로그)
```sql
CREATE TABLE `LAMP_USAGE_LOG` (
  `log_id` int(11) NOT NULL AUTO_INCREMENT,
  `lamp_idx` int(11) NOT NULL COMMENT '좌석 번호',
  `USER_CODE` varchar(255) NOT NULL COMMENT '사용자 학번/교번',
  `start_time` datetime NOT NULL COMMENT '입실 시간',
  `end_time` datetime DEFAULT NULL COMMENT '퇴실 시간 (NULL이면 사용중)',
  `pre_notice_sent_at` datetime DEFAULT NULL,
  `pre_notice_token_count` int(11) DEFAULT NULL,
  `created_at` datetime DEFAULT current_timestamp(),
  PRIMARY KEY (`log_id`),
  KEY `idx_user_date` (`USER_CODE`,`start_time`),
  KEY `idx_seat_date` (`lamp_idx`,`start_time`),
  KEY `idx_end_time` (`end_time`),
  CONSTRAINT `fk_log_lamp` FOREIGN KEY (`lamp_idx`) REFERENCES `LAMP_TBL` (`LAMP_IDX`),
  CONSTRAINT `fk_log_user` FOREIGN KEY (`USER_CODE`) REFERENCES `USER_TBL` (`USER_CODE`)
)
```

---

## 🔧 7. 시스템 관리 (System Administration)

### FCM_TOKEN_TABLE (FCM 토큰 관리)
```sql
CREATE TABLE `FCM_TOKEN_TABLE` (
  `FCM_IDX` int(11) NOT NULL AUTO_INCREMENT,
  `USER_IDX` int(11) NOT NULL,                          -- 사용자 ID (FK)
  `USER_CODE` varchar(255) NOT NULL,                    -- 학번/교번
  `FCM_TOKEN_ANDROID` varchar(255) DEFAULT NULL,        -- 안드로이드 토큰
  `FCM_TOKEN_ANDROID_LAST_USED` datetime DEFAULT NULL,  -- 안드로이드 마지막 사용
  `FCM_TOKEN_IOS` varchar(255) DEFAULT NULL,            -- iOS 토큰
  `FCM_TOKEN_IOS_LAST_USED` datetime DEFAULT NULL,      -- iOS 마지막 사용
  `FCM_TOKEN_WEB` varchar(255) DEFAULT NULL,            -- 웹 토큰
  `FCM_TOKEN_WEB_LAST_USED` datetime DEFAULT NULL,      -- 웹 마지막 사용
  `UPDATED_AT` datetime NOT NULL DEFAULT current_timestamp() ON UPDATE current_timestamp(),
  `FCM_TOKEN_ANDROID_KEEP_SIGNED_IN` tinyint(1) DEFAULT NULL,
  `FCM_TOKEN_IOS_KEEP_SIGNED_IN` tinyint(1) DEFAULT NULL,
  `FCM_TOKEN_WEB_KEEP_SIGNED_IN` tinyint(1) DEFAULT NULL,
  PRIMARY KEY (`FCM_IDX`),
  UNIQUE KEY `uq_user` (`USER_IDX`),
  KEY `idx_user_code` (`USER_CODE`),
  CONSTRAINT `fk_fcm_user` FOREIGN KEY (`USER_IDX`) REFERENCES `USER_TBL` (`USER_IDX`) ON DELETE CASCADE ON UPDATE CASCADE
)
```

**특징:**
- 플랫폼별 토큰 관리 (Android, iOS, Web)
- 자동 로그인 상태 관리
- 마지막 사용 시간 추적

### RENT_TABLE (시설 이용 테이블 - 구버전)
```sql
CREATE TABLE `RENT_TABLE` (
  `RENT_IDX` int(200) NOT NULL AUTO_INCREMENT,
  `RENT_USER` int(200) NOT NULL,                        -- 사용자 ID (FK)
  `RENT_CODE` varchar(200) NOT NULL DEFAULT '',         -- 시설 코드
  `RENT_TITLE` varchar(200) NOT NULL DEFAULT '',        -- 대여 이유
  `RENT_DETAIL` varchar(200) DEFAULT NULL,              -- 상세 내용
  `RENT_DATE` varchar(200) NOT NULL DEFAULT '',         -- 대여 시간
  `RENT_REG` varchar(50) DEFAULT NULL,                  -- 등록 시간
  `RENT_IP` varchar(50) DEFAULT NULL,                   -- IP
  `RENT_OK` int(5) DEFAULT NULL,                        -- 승인 상태
  `RENT_RES` varchar(100) DEFAULT NULL,                 -- 승인 결과
  `RENT_ADMIN` varchar(100) NOT NULL,                   -- 처리 관리자
  `RENT_CONFIRM` varchar(100) DEFAULT NULL,             -- 승인 일시
  `RENT_CONF_IP` varchar(100) DEFAULT NULL,             -- 승인 IP
  PRIMARY KEY (`RENT_IDX`),
  KEY `USER_IDX` (`RENT_USER`),
  CONSTRAINT `USER_IDX` FOREIGN KEY (`RENT_USER`) REFERENCES `USER_TBL` (`USER_IDX`) ON DELETE NO ACTION ON UPDATE NO ACTION
)
```

**승인 상태 (RENT_OK):**
- NULL: 처리중
- 1: 승인됨
- 0: 거부됨

---

## 👁️ 8. 뷰 (Views)

### PROFILE_VIEW (프로필 뷰)
```sql
CREATE VIEW `PROFILE_VIEW` AS
SELECT
  u.USER_EMAIL as user_email,
  u.USER_NAME as user_name,
  u.USER_PHONE as user_phone,
  u.USER_STUDENT as user_type,
  u.USER_CODE as major_code,
  LPAD(COALESCE(u.USER_ZIP, 0), 5, '0') as zip_code,
  u.USER_FIRST_ADD as main_address,
  u.USER_LAST_ADD as detail_address,
  u.PROFILE_IMAGE_KEY as profile_image_key,
  u.USER_BIRTH as birth_date,
  r.STD_STAT as academic_status,
  r.JOIN_PATH as admission_route,
  mf.faculty_code as major_faculty_code,
  md.dept_code as major_dept_code,
  minf.faculty_code as minor_faculty_code,
  mind.dept_code as minor_dept_code
FROM USER_TBL u
LEFT JOIN REGIST_TABLE r ON u.USER_IDX = r.USER_IDX
LEFT JOIN SERIAL_CODE_TABLE sc ON u.USER_IDX = sc.USER_IDX
LEFT JOIN FACULTY mf ON sc.SERIAL_CODE = mf.faculty_code
LEFT JOIN DEPARTMENT md ON mf.faculty_id = md.faculty_id AND sc.SERIAL_SUB = md.dept_code
LEFT JOIN FACULTY minf ON sc.SERIAL_CODE_ND = minf.faculty_code
LEFT JOIN DEPARTMENT mind ON minf.faculty_id = mind.faculty_id AND sc.SERIAL_SUB_ND = mind.dept_code
```

**제공 정보:**
- 기본 사용자 정보
- 학적 상태 및 입학 정보
- 전공/부전공 학부 및 학과 코드

---

## 🔗 9. 주요 관계 및 제약조건

### 외래키 관계
```
USER_TBL.USER_IDX
├── REGIST_TABLE.USER_IDX
├── SERIAL_CODE_TABLE.USER_IDX
├── ENROLLMENT_EXTENDED_TBL.STUDENT_IDX
├── FCM_TOKEN_TABLE.USER_IDX
├── LAMP_TBL.USER_CODE
├── LAMP_USAGE_LOG.USER_CODE
└── RENT_TABLE.RENT_USER

LEC_TBL.LEC_IDX
├── ENROLLMENT_EXTENDED_TBL.LEC_IDX
└── ASSIGNMENT_EXTENDED_TBL.LEC_IDX

FACULTY.faculty_id
└── DEPARTMENT.faculty_id

DEPARTMENT.dept_id (참조 없음 - 코드로 사용)

BOARD_TBL.BOARD_IDX
└── BOARD_ATTACHMENT_TBL.BOARD_IDX

FACILITY_TBL.FACILITY_IDX
├── FACILITY_RESERVATION_TBL.FACILITY_IDX
├── FACILITY_POLICY_TBL.FACILITY_IDX
└── FACILITY_BLOCK_TBL.FACILITY_IDX

FACILITY_RESERVATION_TBL.RESERVATION_IDX
└── FACILITY_RESERVATION_LOG.RESERVATION_IDX

LAMP_TBL.LAMP_IDX
└── LAMP_USAGE_LOG.lamp_idx
```

### 트리거
```sql
-- 학생 등록 시 자동으로 학적 정보 생성
CREATE TRIGGER auto_student_regist
AFTER INSERT ON USER_TBL
FOR EACH ROW
BEGIN
  IF NEW.USER_STUDENT = 0 THEN  -- 학생인 경우
    INSERT INTO REGIST_TABLE (
      USER_IDX, USER_CODE, JOIN_PATH, STD_STAT, CNT_TERM
    ) VALUES (
      NEW.USER_IDX, NEW.USER_CODE, '신규', '재학', 0
    );
  END IF;
END
```

---

## 📊 10. 인덱스 현황

### 주요 인덱스 목록
- `USER_TBL.idx_user_code`: 사용자 코드 검색
- `BOARD_ATTACHMENT_TBL.idx_board_attachment_*`: 첨부파일 검색 최적화
- `FACILITY_RESERVATION_TBL.idx_reservation_*`: 예약 검색 및 필터링
- `LAMP_TBL.idx_user_code`, `idx_lamp_on`: 열람실 좌석 관리
- `DEPARTMENT.uq_faculty_dept`: 학부-학과 유니크 제약

---

## 💾 11. 데이터 타입 및 저장 방식

### JSON 필드 활용
- `USER_TBL.LECTURE_EVALUATIONS`: 강의 평가 데이터
- `ENROLLMENT_EXTENDED_TBL.ENROLLMENT_DATA`: 수강/출결/성적 정보
- `ASSIGNMENT_EXTENDED_TBL.ASSIGNMENT_DATA`: 과제 정보 및 제출 목록

### Base64 인코딩 필드
- `BOARD_TBL.BOARD_TIT`: 게시글 제목
- `BOARD_TBL.BOARD_CONT`: 게시글 내용

### 날짜/시간 형식
- `varchar` 타입의 날짜 필드: `YYYY-MM-DD HH:mm:ss` 형식
- `datetime` 타입: MySQL 표준 datetime 형식
- `year` 타입: 학부/학과 설립연도

---

## 🔒 12. 보안 및 제약사항

### 데이터 무결성
- 모든 외래키에 CASCADE 옵션 적용 (참조 무결성 유지)
- 유니크 제약조건으로 데이터 중복 방지
- NOT NULL 제약으로 필수 데이터 보장

### 민감정보 처리
- 비밀번호: SHA-256 해시 저장
- 개인정보: 암호화 저장 고려
- IP 주소: 접근 로그 기록

---

## 📈 13. 확장성 고려사항

### 현재 설계의 장점
- JSON 필드로 유연한 데이터 확장 가능
- 모듈화된 테이블 구조로 기능별 독립성 확보
- 인덱스 최적화로 조회 성능 확보

### 개선 가능 영역
- 대용량 데이터 처리를 위한 파티셔닝 고려
- 읽기 부하 분산을 위한 리플리케이션 구성
- 캐시 전략 수립 (Redis 활용)

---

*본 문서는 Blue Crab LMS 데이터베이스 스키마의 구조를 정리한 문서입니다. 실제 운영 시 상세한 튜닝과 모니터링이 필요합니다.*