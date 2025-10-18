# Blue Crab LMS 데이터베이스 스키마

> **작성일**: 2025-10-10  
> **DB**: MariaDB 10.x  
> **데이터베이스명**: blue_crab  
> **확인 방법**: SSH 접속 후 실제 DB 구조 확인

---

## 📊 데이터베이스 정보

### 연결 정보

| 항목 | 값 |
|------|-----|
| **호스트** | 121.165.24.26 |
| **포트** | 55511 |
| **데이터베이스** | blue_crab |
| **문자셋** | utf8mb3, utf8mb4 |
| **엔진** | InnoDB |

### HikariCP 연결 풀 설정

```properties
spring.datasource.hikari.maximum-pool-size=10
spring.datasource.hikari.minimum-idle=5
spring.datasource.hikari.connection-timeout=30000
spring.datasource.hikari.idle-timeout=600000
spring.datasource.hikari.max-lifetime=1800000
spring.datasource.hikari.leak-detection-threshold=60000
```

---

## 🗂️ 테이블 목록 (18개)

| # | 테이블명 | 설명 | 주요 기능 |
|---|---------|------|----------|
| 1 | USER_TBL | 사용자 | 학생/교수 정보 |
| 2 | ADMIN_TBL | 관리자 | 시스템/학사 관리자 |
| 3 | BOARD_TBL | 게시판 | 게시글 관리 |
| 4 | BOARD_ATTACHMENT_TBL | 게시글 첨부파일 | 파일 메타데이터 |
| 5 | FACILITY_TBL | 시설 | 시설 정보 |
| 6 | FACILITY_RESERVATION_TBL | 시설 예약 | 예약 관리 |
| 7 | FACILITY_BLOCK_TBL | 시설 마감 | 점검/행사 마감 |
| 8 | FACILITY_RESERVATION_LOG | 시설 예약 로그 | 예약 이력 |
| 9 | FCM_TOKEN_TABLE | FCM 토큰 | 푸시 알림 토큰 |
| 10 | LEC_TBL | 강의 | 강의 정보 |
| 11 | REGIST_TABLE | 학적 | 학생 학적 이력 |
| 12 | LAMP_TBL | 열람실 좌석 | 좌석 현황 |
| 13 | LAMP_USAGE_LOG | 열람실 사용 로그 | 입퇴실 기록 |
| 14 | RENT_TABLE | 시설물 대여 | 레거시 대여 시스템 |
| 15 | DEPARTMENT | 학과 | 학과 마스터 |
| 16 | FACULTY | 학부 | 학부 마스터 |
| 17 | SERIAL_CODE_TABLE | 전공/부전공 | 학생 전공 정보 |
| 18 | PROFILE_VIEW | 프로필 뷰 | 사용자 통합 프로필 |

---

## 📐 ERD (Entity Relationship Diagram)

### 사용자 관리 (User Management)

```
┌──────────────┐
│  USER_TBL    │
├──────────────┤
│ USER_IDX (PK)│◄──┐
│ USER_EMAIL   │   │
│ USER_CODE    │   │  ┌──────────────────┐
│ ...          │   ├──│ FCM_TOKEN_TABLE  │
└──────────────┘   │  ├──────────────────┤
       ▲           │  │ FCM_IDX (PK)     │
       │           └──│ USER_IDX (FK)    │
       │              │ FCM_TOKEN_WEB    │
       │              └──────────────────┘
       │
       │  ┌──────────────────┐
       ├──│ REGIST_TABLE     │
       │  ├──────────────────┤
       │  │ REG_IDX (PK)     │
       │  │ USER_IDX (FK)    │
       │  │ STD_STAT         │
       │  └──────────────────┘
       │
       │  ┌──────────────────┐
       └──│ SERIAL_CODE_TABLE│
          ├──────────────────┤
          │ SERIAL_IDX (PK)  │
          │ USER_IDX (FK)    │
          │ SERIAL_CODE      │
          └──────────────────┘

┌──────────────┐
│  ADMIN_TBL   │
├──────────────┤
│ ADMIN_IDX(PK)│
│ ADMIN_ID     │
│ ADMIN_SYS    │
└──────────────┘
```

### 게시판 시스템 (Board System)

```
┌──────────────┐         ┌─────────────────────────┐
│  BOARD_TBL   │◄────────│ BOARD_ATTACHMENT_TBL    │
├──────────────┤         ├─────────────────────────┤
│ BOARD_IDX(PK)│         │ ATTACHMENT_IDX (PK)     │
│ BOARD_CODE   │         │ BOARD_IDX (FK)          │
│ BOARD_WRITER │         │ ORIGINAL_FILE_NAME      │
│ BOARD_TIT    │         │ FILE_PATH               │
│ BOARD_CONT   │         │ FILE_SIZE               │
│ ...          │         │ MIME_TYPE               │
└──────────────┘         └─────────────────────────┘
```

### 시설 예약 시스템 (Facility Reservation)

```
┌──────────────────┐
│  FACILITY_TBL    │
├──────────────────┤
│ FACILITY_IDX (PK)│◄──┐
│ FACILITY_NAME    │   │
│ FACILITY_TYPE    │   │  ┌─────────────────────────────┐
│ CAPACITY         │   ├──│ FACILITY_RESERVATION_TBL    │
│ REQUIRES_APPROVAL│   │  ├─────────────────────────────┤
└──────────────────┘   │  │ RESERVATION_IDX (PK)        │
         ▲             │  │ FACILITY_IDX (FK)           │
         │             │  │ USER_CODE                   │
         │             │  │ START_TIME                  │
         │             │  │ END_TIME                    │
         │             │  │ STATUS                      │
         │             │  └─────────────────────────────┘
         │             │           ▲
         │             │           │
         │             │  ┌─────────────────────────────┐
         │             │  │ FACILITY_RESERVATION_LOG    │
         │             │  ├─────────────────────────────┤
         │             │  │ LOG_IDX (PK)                │
         │             │  │ RESERVATION_IDX (FK)        │
         │             │  │ EVENT_TYPE                  │
         │             │  │ ACTOR_CODE                  │
         │             │  └─────────────────────────────┘
         │             │
         │  ┌─────────────────────────┐
         └──│ FACILITY_BLOCK_TBL      │
            ├─────────────────────────┤
            │ BLOCK_IDX (PK)          │
            │ FACILITY_IDX (FK)       │
            │ BLOCK_START             │
            │ BLOCK_END               │
            │ BLOCK_REASON            │
            └─────────────────────────┘
```

### 열람실 시스템 (Reading Room)

```
┌──────────────┐         ┌──────────────────┐
│  LAMP_TBL    │◄────────│ LAMP_USAGE_LOG   │
├──────────────┤         ├──────────────────┤
│ LAMP_IDX (PK)│         │ log_id (PK)      │
│ LAMP_ON      │         │ lamp_idx (FK)    │
│ USER_CODE    │         │ USER_CODE (FK)   │
│ start_time   │         │ start_time       │
│ end_time     │         │ end_time         │
└──────────────┘         └──────────────────┘
```

### 학사 시스템 (Academic)

```
┌──────────────┐         ┌──────────────┐
│  FACULTY     │◄────────│ DEPARTMENT   │
├──────────────┤         ├──────────────┤
│ faculty_id(PK)│         │ dept_id (PK) │
│ faculty_code │         │ dept_code    │
│ faculty_name │         │ dept_name    │
└──────────────┘         │ faculty_id(FK)│
                         └──────────────┘

┌──────────────┐
│  LEC_TBL     │
├──────────────┤
│ LEC_IDX (PK) │
│ LEC_SERIAL   │
│ LEC_TIT      │
│ LEC_PROF     │
│ LEC_MCODE    │
└──────────────┘
```

---

## 📋 테이블 상세

### 1. USER_TBL (사용자)

**용도**: 학생 및 교수 정보 관리

| 컬럼명 | 타입 | 제약조건 | 설명 |
|--------|------|----------|------|
| USER_IDX | int(200) | PK, AUTO_INCREMENT | 사용자 ID |
| USER_EMAIL | varchar(200) | NOT NULL | 이메일 (계정) |
| USER_PW | varchar(200) | NOT NULL | 비밀번호 (암호화) |
| USER_NAME | varchar(50) | NOT NULL | 이름 |
| USER_CODE | varchar(255) | NOT NULL, INDEX | 학번/교번 |
| USER_PHONE | char(11) | NOT NULL | 전화번호 (- 제외) |
| USER_BIRTH | varchar(100) | NOT NULL | 생년월일 |
| USER_STUDENT | int(1) | NOT NULL | 학생(1)/교수(0) |
| USER_LATEST | varchar(100) | NULL | 마지막 접속일 |
| USER_ZIP | int(5) | NULL | 우편번호 |
| USER_FIRST_ADD | varchar(200) | NULL | 기본 주소 |
| USER_LAST_ADD | varchar(100) | NULL | 상세 주소 |
| USER_REG | varchar(100) | NULL | 등록일 |
| USER_REG_IP | varchar(100) | NULL | 등록 IP |
| PROFILE_IMAGE_KEY | varchar(255) | NULL | MinIO 프로필 이미지 키 |

**인덱스**:
- PRIMARY KEY (`USER_IDX`)
- KEY `idx_user_code` (`USER_CODE`)

**엔진**: InnoDB, AUTO_INCREMENT=16

---

### 2. ADMIN_TBL (관리자)

**용도**: 시스템 및 학사 관리자 정보

| 컬럼명 | 타입 | 제약조건 | 설명 |
|--------|------|----------|------|
| ADMIN_IDX | int(200) | PK, AUTO_INCREMENT | 관리자 ID |
| ADMIN_SYS | int(1) | NOT NULL, DEFAULT 0 | 시스템(1)/학사(0) 구분 |
| ADMIN_ID | varchar(50) | NOT NULL | 관리자 계정 ID |
| ADMIN_PW | varchar(200) | NOT NULL | 비밀번호 (암호화) |
| ADMIN_NAME | varchar(100) | NOT NULL | 이름 |
| ADMIN_PHONE | varchar(11) | NOT NULL | 전화번호 |
| ADMIN_OFFICE | varchar(11) | NOT NULL | 사무실 번호 |
| ADMIN_LATEST | varchar(100) | NULL | 마지막 접속 시간 |
| ADMIN_LATEST_IP | varchar(50) | NULL | 마지막 접속 IP |
| ADMIN_REG | varchar(100) | NULL | 등록일자 |
| ADMIN_REG_IP | varchar(100) | NULL | 등록 IP |

**엔진**: InnoDB, AUTO_INCREMENT=6

---

### 3. BOARD_TBL (게시판)

**용도**: 게시글 관리

| 컬럼명 | 타입 | 제약조건 | 설명 |
|--------|------|----------|------|
| BOARD_IDX | int(200) | PK, AUTO_INCREMENT | 게시글 ID |
| BOARD_CODE | int(10) | NOT NULL, DEFAULT 0 | 게시판 코드 |
| BOARD_ON | int(10) | NOT NULL, DEFAULT 1 | 공개(1)/비공개(0) |
| BOARD_WRITER | varchar(250) | NOT NULL | 작성자명 |
| BOARD_TIT | text | NULL | 제목 (base64) |
| BOARD_CONT | text | NULL | 본문 (base64) |
| BOARD_IMG | varchar(250) | NULL | 이미지 경로 |
| BOARD_FILE | varchar(500) | NULL | 첨부파일 IDX (JSON) |
| BOARD_VIEW | int(250) | NOT NULL, DEFAULT 0 | 조회수 |
| BOARD_REG | varchar(250) | NULL | 최초 게시일 |
| BOARD_LATEST | varchar(250) | NULL | 최근 수정일 |
| BOARD_IP | varchar(250) | NULL | 작성자 IP |
| BOARD_WRITER_IDX | int(11) | NOT NULL | 작성자 IDX |
| BOARD_WRITER_TYPE | int(11) | NOT NULL, DEFAULT 0 | 사용자(0)/관리자(1) |

**특징**:
- 제목/본문 Base64 인코딩
- 첨부파일 IDX는 JSON 또는 콤마 구분

**엔진**: InnoDB, AUTO_INCREMENT=31

---

### 4. BOARD_ATTACHMENT_TBL (게시글 첨부파일)

**용도**: 게시글 첨부파일 메타데이터 관리

| 컬럼명 | 타입 | 제약조건 | 설명 |
|--------|------|----------|------|
| ATTACHMENT_IDX | int(11) | PK, AUTO_INCREMENT | 첨부파일 ID |
| BOARD_IDX | int(11) | NOT NULL, FK | 게시글 ID |
| ORIGINAL_FILE_NAME | varchar(255) | NOT NULL | 원본 파일명 |
| FILE_PATH | varchar(500) | NOT NULL | MinIO 파일 경로 |
| FILE_SIZE | bigint(20) | NOT NULL | 파일 크기 (bytes) |
| MIME_TYPE | varchar(100) | NOT NULL | MIME 타입 |
| UPLOAD_DATE | varchar(50) | NOT NULL | 업로드 일자 |
| IS_ACTIVE | tinyint(1) | NOT NULL, DEFAULT 1 | 활성화 여부 |
| EXPIRY_DATE | varchar(50) | NULL | 만료일 |

**인덱스**:
- KEY `idx_board_attachment_board_idx` (`BOARD_IDX`)
- KEY `idx_board_attachment_active` (`IS_ACTIVE`)
- KEY `idx_board_attachment_upload_date` (`UPLOAD_DATE`)
- KEY `idx_board_attachment_board_active` (`BOARD_IDX`,`IS_ACTIVE`)

**외래키**:
- FOREIGN KEY (`BOARD_IDX`) REFERENCES `BOARD_TBL` (`BOARD_IDX`) ON DELETE CASCADE

**엔진**: InnoDB, utf8mb4_unicode_ci

---

### 5. FACILITY_TBL (시설)

**용도**: 시설 정보 관리

| 컬럼명 | 타입 | 제약조건 | 설명 |
|--------|------|----------|------|
| FACILITY_IDX | int(11) | PK, AUTO_INCREMENT | 시설 ID |
| FACILITY_NAME | varchar(100) | NOT NULL | 시설명 |
| FACILITY_TYPE | varchar(20) | NOT NULL | 시설 유형 |
| FACILITY_DESC | text | NULL | 시설 설명 |
| CAPACITY | int(11) | NULL | 수용 인원 |
| LOCATION | varchar(200) | NULL | 위치 |
| DEFAULT_EQUIPMENT | text | NULL | 기본 장비 |
| IS_ACTIVE | tinyint(4) | NOT NULL, DEFAULT 1 | 활성화 여부 |
| CREATED_AT | datetime | NOT NULL, DEFAULT CURRENT_TIMESTAMP | 생성일시 |
| UPDATED_AT | datetime | NOT NULL, DEFAULT CURRENT_TIMESTAMP ON UPDATE | 수정일시 |
| REQUIRES_APPROVAL | tinyint(4) | NOT NULL, DEFAULT 1 | 승인 필요(1)/즉시예약(0) |

**인덱스**:
- KEY `idx_facility_type_active` (`FACILITY_TYPE`,`IS_ACTIVE`)
- KEY `idx_facility_name` (`FACILITY_NAME`)

**엔진**: InnoDB, AUTO_INCREMENT=23

---

### 6. FACILITY_RESERVATION_TBL (시설 예약)

**용도**: 시설 예약 관리

| 컬럼명 | 타입 | 제약조건 | 설명 |
|--------|------|----------|------|
| RESERVATION_IDX | int(11) | PK, AUTO_INCREMENT | 예약 ID |
| FACILITY_IDX | int(11) | NOT NULL, FK | 시설 ID |
| USER_CODE | varchar(50) | NOT NULL | 예약자 학번/교번 |
| START_TIME | datetime | NOT NULL | 시작 시간 |
| END_TIME | datetime | NOT NULL | 종료 시간 |
| PARTY_SIZE | int(11) | NULL | 사용 인원 |
| PURPOSE | text | NULL | 사용 목적 |
| REQUESTED_EQUIPMENT | text | NULL | 요청 장비 |
| STATUS | varchar(20) | NOT NULL, DEFAULT 'PENDING' | 예약 상태 |
| ADMIN_NOTE | text | NULL | 관리자 메모 |
| REJECTION_REASON | text | NULL | 거부 사유 |
| APPROVED_BY | varchar(50) | NULL | 승인자 코드 |
| APPROVED_AT | datetime | NULL | 승인 일시 |
| CREATED_AT | datetime | NOT NULL, DEFAULT CURRENT_TIMESTAMP | 생성일시 |
| UPDATED_AT | datetime | NOT NULL, DEFAULT CURRENT_TIMESTAMP ON UPDATE | 수정일시 |

**STATUS 값**:
- `PENDING`: 승인 대기
- `APPROVED`: 승인됨
- `REJECTED`: 거부됨
- `CANCELLED`: 취소됨
- `COMPLETED`: 완료됨

**인덱스**:
- KEY `idx_reservation_facility_time` (`FACILITY_IDX`,`START_TIME`,`END_TIME`)
- KEY `idx_reservation_user_status` (`USER_CODE`,`STATUS`)
- KEY `idx_reservation_status_time` (`STATUS`,`START_TIME`)
- KEY `idx_reservation_created` (`CREATED_AT`)

**외래키**:
- FOREIGN KEY (`FACILITY_IDX`) REFERENCES `FACILITY_TBL` (`FACILITY_IDX`) ON UPDATE CASCADE

**엔진**: InnoDB

---

### 7. FACILITY_BLOCK_TBL (시설 마감)

**용도**: 시설 점검/행사 마감 관리

| 컬럼명 | 타입 | 제약조건 | 설명 |
|--------|------|----------|------|
| BLOCK_IDX | int(11) | PK, AUTO_INCREMENT | 마감 ID |
| FACILITY_IDX | int(11) | NOT NULL, FK | 시설 ID |
| BLOCK_START | datetime | NOT NULL | 마감 시작 |
| BLOCK_END | datetime | NOT NULL | 마감 종료 |
| BLOCK_REASON | varchar(200) | NOT NULL | 마감 사유 |
| BLOCK_TYPE | varchar(20) | DEFAULT 'MAINTENANCE' | 마감 유형 |
| CREATED_BY | varchar(50) | NOT NULL | 관리자 코드 |
| CREATED_AT | datetime | NOT NULL, DEFAULT CURRENT_TIMESTAMP | 생성일시 |

**BLOCK_TYPE 값**:
- `MAINTENANCE`: 점검
- `EMERGENCY`: 긴급
- `EVENT`: 행사

**인덱스**:
- KEY `idx_block_facility_time` (`FACILITY_IDX`,`BLOCK_START`,`BLOCK_END`)
- KEY `idx_block_time_range` (`BLOCK_START`,`BLOCK_END`)

**외래키**:
- FOREIGN KEY (`FACILITY_IDX`) REFERENCES `FACILITY_TBL` (`FACILITY_IDX`) ON DELETE CASCADE ON UPDATE CASCADE

**엔진**: InnoDB

---

### 8. FACILITY_RESERVATION_LOG (시설 예약 로그)

**용도**: 시설 예약 이력 추적

| 컬럼명 | 타입 | 제약조건 | 설명 |
|--------|------|----------|------|
| LOG_IDX | int(11) | PK, AUTO_INCREMENT | 로그 ID |
| RESERVATION_IDX | int(11) | NOT NULL, FK | 예약 ID |
| EVENT_TYPE | varchar(50) | NOT NULL | 이벤트 유형 |
| ACTOR_TYPE | varchar(20) | NULL | 행위자 유형 |
| ACTOR_CODE | varchar(50) | NULL | 행위자 코드 |
| PAYLOAD | text | NULL | 추가 데이터 (JSON) |
| CREATED_AT | datetime | NOT NULL, DEFAULT CURRENT_TIMESTAMP | 생성일시 |

**EVENT_TYPE 값**:
- `CREATED`: 예약 생성
- `APPROVED`: 승인
- `REJECTED`: 거부
- `CANCELLED`: 취소
- `MODIFIED`: 수정

**인덱스**:
- KEY `idx_reservationlog_reservation` (`RESERVATION_IDX`)
- KEY `idx_reservationlog_created` (`CREATED_AT`)

**외래키**:
- FOREIGN KEY (`RESERVATION_IDX`) REFERENCES `FACILITY_RESERVATION_TBL` (`RESERVATION_IDX`) ON DELETE CASCADE ON UPDATE CASCADE

**엔진**: InnoDB

---

### 9. FCM_TOKEN_TABLE (FCM 토큰)

**용도**: Firebase Cloud Messaging 푸시 알림 토큰 관리

| 컬럼명 | 타입 | 제약조건 | 설명 |
|--------|------|----------|------|
| FCM_IDX | int(11) | PK, AUTO_INCREMENT | FCM 토큰 ID |
| USER_IDX | int(11) | NOT NULL, UNIQUE, FK | 사용자 ID |
| USER_CODE | varchar(255) | NOT NULL, INDEX | 학번/교번 |
| FCM_TOKEN_ANDROID | varchar(255) | NULL | 안드로이드 토큰 |
| FCM_TOKEN_ANDROID_LAST_USED | datetime | NULL | 안드로이드 마지막 사용 |
| FCM_TOKEN_IOS | varchar(255) | NULL | iOS 토큰 |
| FCM_TOKEN_IOS_LAST_USED | datetime | NULL | iOS 마지막 사용 |
| FCM_TOKEN_WEB | varchar(255) | NULL | 웹 토큰 |
| FCM_TOKEN_WEB_LAST_USED | datetime | NULL | 웹 마지막 사용 |
| UPDATED_AT | datetime | NOT NULL, DEFAULT CURRENT_TIMESTAMP ON UPDATE | 최종 수정일시 |
| FCM_TOKEN_ANDROID_KEEP_SIGNED_IN | tinyint(1) | NULL | 안드로이드 로그인 유지 |
| FCM_TOKEN_IOS_KEEP_SIGNED_IN | tinyint(1) | NULL | iOS 로그인 유지 |
| FCM_TOKEN_WEB_KEEP_SIGNED_IN | tinyint(1) | NULL | 웹 로그인 유지 |

**특징**:
- 사용자당 1개의 FCM 레코드 (UNIQUE KEY on USER_IDX)
- 다중 기기 지원 (안드로이드/iOS/웹 각각)
- Redis와 함께 사용하여 세션 관리

**인덱스**:
- UNIQUE KEY `uq_user` (`USER_IDX`)
- KEY `idx_user_code` (`USER_CODE`)

**외래키**:
- FOREIGN KEY (`USER_IDX`) REFERENCES `USER_TBL` (`USER_IDX`) ON DELETE CASCADE ON UPDATE CASCADE

**엔진**: InnoDB, AUTO_INCREMENT=4

---

### 10. LEC_TBL (강의)

**용도**: 강의 정보 관리

| 컬럼명 | 타입 | 제약조건 | 설명 |
|--------|------|----------|------|
| LEC_IDX | int(200) | PK, AUTO_INCREMENT | 강의 ID |
| LEC_SERIAL | varchar(50) | NOT NULL | 강의 코드 |
| LEC_TIT | varchar(50) | NOT NULL | 강의명 |
| LEC_PROF | varchar(50) | NOT NULL | 담당 교수 |
| LEC_POINT | int(10) | NOT NULL, DEFAULT 0 | 이수 학점 |
| LEC_MAJOR | int(1) | NOT NULL, DEFAULT 1 | 전공(1)/교양(0) |
| LEC_MUST | int(1) | NOT NULL, DEFAULT 1 | 필수(1)/선택(0) |
| LEC_SUMMARY | text | NULL | 강의 개요 |
| LEC_TIME | varchar(50) | NOT NULL | 강의 시간 |
| LEC_ASSIGN | int(1) | NOT NULL, DEFAULT 0 | 과제 유무 |
| LEC_OPEN | int(1) | NOT NULL, DEFAULT 0 | 수강신청 열림(1)/닫힘(0) |
| LEC_MANY | int(10) | NOT NULL, DEFAULT 0 | 수강 가능 인원 |
| LEC_MCODE | varchar(50) | NOT NULL | 학부 코드 |
| LEC_MCODE_DEP | varchar(50) | NOT NULL | 학과 코드 |
| LEC_MIN | int(10) | NOT NULL, DEFAULT 0 | 최저 학기 제한 |
| LEC_REG | varchar(100) | NULL | 등록일 |
| LEC_IP | varchar(100) | NULL | 등록 IP |

**엔진**: InnoDB, AUTO_INCREMENT=6

---

### 11. REGIST_TABLE (학적)

**용도**: 학생 학적 이력 관리

| 컬럼명 | 타입 | 제약조건 | 설명 |
|--------|------|----------|------|
| REG_IDX | int(11) | PK, AUTO_INCREMENT | 학적 이력 ID |
| USER_IDX | int(11) | NOT NULL, FK | 학생 ID |
| USER_CODE | varchar(50) | NOT NULL, DEFAULT '' | 학번 (조회용) |
| JOIN_PATH | varchar(100) | NOT NULL, DEFAULT '신규' | 입학 경로 |
| STD_STAT | varchar(100) | NOT NULL, DEFAULT '재학' | 학적 상태 |
| STD_REST_DATE | varchar(200) | NULL | 휴학 기간 |
| CNT_TERM | int(11) | NOT NULL, DEFAULT 0 | 이수 완료 학기 수 |
| ADMIN_NAME | varchar(200) | NULL | 처리 관리자 |
| ADMIN_REG | datetime | NULL | 처리 일시 |
| ADMIN_IP | varchar(45) | NULL | 처리 IP |

**특징**:
- 학적 변동 이력을 시계열로 관리
- REG_IDX는 생성 순서

**인덱스**:
- KEY `FK_REG_USER` (`USER_IDX`)

**외래키**:
- FOREIGN KEY (`USER_IDX`) REFERENCES `USER_TBL` (`USER_IDX`)

**엔진**: InnoDB, AUTO_INCREMENT=42

---

### 12. LAMP_TBL (열람실 좌석)

**용도**: 열람실 좌석 현황 관리

| 컬럼명 | 타입 | 제약조건 | 설명 |
|--------|------|----------|------|
| LAMP_IDX | int(11) | PK | 좌석 번호 (1~80) |
| LAMP_ON | int(11) | NOT NULL, DEFAULT 0, INDEX | 빈자리(0)/사용중(1) |
| USER_CODE | varchar(255) | NULL, INDEX, FK | 사용자 학번/교번 |
| start_time | datetime | NULL | 사용 시작 시간 |
| end_time | datetime | NULL | 예정 종료 시간 (2시간 후) |
| updated_at | datetime | DEFAULT CURRENT_TIMESTAMP ON UPDATE | 최종 업데이트 |

**특징**:
- 좌석 번호 1~80 고정
- 2시간 자동 반납 시스템

**인덱스**:
- KEY `idx_user_code` (`USER_CODE`)
- KEY `idx_lamp_on` (`LAMP_ON`)

**외래키**:
- FOREIGN KEY (`USER_CODE`) REFERENCES `USER_TBL` (`USER_CODE`) ON DELETE SET NULL

**엔진**: InnoDB

---

### 13. LAMP_USAGE_LOG (열람실 사용 로그)

**용도**: 열람실 입퇴실 기록

| 컬럼명 | 타입 | 제약조건 | 설명 |
|--------|------|----------|------|
| log_id | int(11) | PK, AUTO_INCREMENT | 로그 ID |
| lamp_idx | int(11) | NOT NULL, FK | 좌석 번호 |
| USER_CODE | varchar(255) | NOT NULL, FK | 사용자 학번/교번 |
| start_time | datetime | NOT NULL | 입실 시간 |
| end_time | datetime | NULL | 퇴실 시간 (NULL=사용중) |
| created_at | datetime | DEFAULT CURRENT_TIMESTAMP | 생성일시 |

**인덱스**:
- KEY `idx_user_date` (`USER_CODE`,`start_time`)
- KEY `idx_seat_date` (`lamp_idx`,`start_time`)
- KEY `idx_end_time` (`end_time`)

**외래키**:
- FOREIGN KEY (`lamp_idx`) REFERENCES `LAMP_TBL` (`LAMP_IDX`)
- FOREIGN KEY (`USER_CODE`) REFERENCES `USER_TBL` (`USER_CODE`)

**엔진**: InnoDB, AUTO_INCREMENT=18

---

### 14. RENT_TABLE (시설물 대여)

**용도**: 레거시 시설물 대여 시스템

| 컬럼명 | 타입 | 제약조건 | 설명 |
|--------|------|----------|------|
| RENT_IDX | int(200) | PK, AUTO_INCREMENT | 대여 ID |
| RENT_USER | int(200) | NOT NULL, FK | 회원 ID |
| RENT_CODE | varchar(200) | NOT NULL, DEFAULT '' | 시설물 코드 |
| RENT_TITLE | varchar(200) | NOT NULL, DEFAULT '' | 대여 이유 |
| RENT_DETAIL | varchar(200) | NULL | 상세 이유 |
| RENT_DATE | varchar(200) | NOT NULL, DEFAULT '' | 대여 시간 |
| RENT_REG | varchar(50) | NULL | 등록 시간 |
| RENT_IP | varchar(50) | NULL | 등록 IP |
| RENT_OK | int(5) | NULL | 처리중(NULL)/허가(1)/불가(0) |
| RENT_RES | varchar(100) | NULL | 승인 사유 |
| RENT_ADMIN | varchar(100) | NOT NULL | 응답 관리자 |
| RENT_CONFIRM | varchar(100) | NULL | 응답 일시 |
| RENT_CONF_IP | varchar(100) | NULL | 응답 IP |

**참고**: FACILITY_RESERVATION_TBL로 대체 예정 (레거시)

**외래키**:
- FOREIGN KEY (`RENT_USER`) REFERENCES `USER_TBL` (`USER_IDX`)

**엔진**: InnoDB

---

### 15. DEPARTMENT (학과)

**용도**: 학과 마스터 데이터

| 컬럼명 | 타입 | 제약조건 | 설명 |
|--------|------|----------|------|
| dept_id | int(11) | PK, AUTO_INCREMENT | 학과 ID |
| dept_code | char(2) | NOT NULL, INDEX | 학과 코드 |
| dept_name | varchar(100) | NOT NULL | 학과명 |
| faculty_id | int(11) | NOT NULL, FK | 학부 ID |
| established_at | year(4) | NOT NULL | 설립 연도 |
| capacity | int(11) | NOT NULL, DEFAULT 0 | 정원 |

**인덱스**:
- UNIQUE KEY `uq_faculty_dept` (`faculty_id`,`dept_code`)
- KEY `idx_dept_code` (`dept_code`)

**외래키**:
- FOREIGN KEY (`faculty_id`) REFERENCES `FACULTY` (`faculty_id`) ON UPDATE CASCADE

**엔진**: InnoDB, AUTO_INCREMENT=32

---

### 16. FACULTY (학부)

**용도**: 학부 마스터 데이터

| 컬럼명 | 타입 | 제약조건 | 설명 |
|--------|------|----------|------|
| faculty_id | int(11) | PK, AUTO_INCREMENT | 학부 ID |
| faculty_code | char(2) | NOT NULL, UNIQUE | 학부 코드 |
| faculty_name | varchar(50) | NOT NULL | 학부명 |
| established_at | year(4) | NOT NULL | 설립 연도 |
| capacity | int(11) | NOT NULL, DEFAULT 0 | 정원 |

**인덱스**:
- UNIQUE KEY `uq_faculty_code` (`faculty_code`)

**엔진**: InnoDB, AUTO_INCREMENT=6

---

### 17. SERIAL_CODE_TABLE (전공/부전공)

**용도**: 학생 전공 및 부전공 정보

| 컬럼명 | 타입 | 제약조건 | 설명 |
|--------|------|----------|------|
| SERIAL_IDX | int(11) | PK, AUTO_INCREMENT | 전공 관리 ID |
| USER_IDX | int(11) | NOT NULL, FK | 사용자 ID |
| SERIAL_CODE | char(2) | NOT NULL | 전공 학부 코드 |
| SERIAL_SUB | char(2) | NOT NULL | 전공 학과 코드 |
| SERIAL_CODE_ND | char(2) | NULL | 부전공 학부 코드 |
| SERIAL_SUB_ND | char(2) | NULL | 부전공 학과 코드 |
| SERIAL_REG | varchar(50) | NOT NULL | 전공 등록일 |
| SERIAL_REG_ND | varchar(50) | NULL | 부전공 등록일 |

**인덱스**:
- UNIQUE KEY `uq_user_major` (`USER_IDX`,`SERIAL_CODE`,`SERIAL_SUB`)
- KEY `idx_user` (`USER_IDX`)

**외래키**:
- FOREIGN KEY (`USER_IDX`) REFERENCES `USER_TBL` (`USER_IDX`)

**엔진**: InnoDB, AUTO_INCREMENT=2

---

### 18. PROFILE_VIEW (프로필 뷰)

**용도**: 사용자 통합 프로필 조회 (VIEW)

**조인 테이블**:
- USER_TBL (기본)
- REGIST_TABLE (학적)
- SERIAL_CODE_TABLE (전공)
- FACULTY (학부)
- DEPARTMENT (학과)

**주요 컬럼**:
| 컬럼명 | 설명 |
|--------|------|
| user_email | 이메일 |
| user_name | 이름 |
| user_phone | 전화번호 |
| user_type | 학생/교수 구분 |
| major_code | 전공 코드 |
| zip_code | 우편번호 (5자리 LPAD) |
| main_address | 기본 주소 |
| detail_address | 상세 주소 |
| profile_image_key | 프로필 이미지 키 |
| birth_date | 생년월일 |
| academic_status | 학적 상태 |
| admission_route | 입학 경로 |
| major_faculty_code | 전공 학부 코드 |
| major_dept_code | 전공 학과 코드 |
| minor_faculty_code | 부전공 학부 코드 |
| minor_dept_code | 부전공 학과 코드 |

**활용**:
- 프로필 조회 API에서 사용
- 복잡한 조인을 단순화

---

## 🔐 보안 및 제약조건

### 외래키 제약조건

| 자식 테이블 | 부모 테이블 | 제약조건 | ON DELETE | ON UPDATE |
|------------|------------|---------|-----------|-----------|
| BOARD_ATTACHMENT_TBL | BOARD_TBL | FK_BOARD | CASCADE | - |
| FCM_TOKEN_TABLE | USER_TBL | fk_fcm_user | CASCADE | CASCADE |
| FACILITY_RESERVATION_TBL | FACILITY_TBL | fk_reservation_facility | - | CASCADE |
| FACILITY_BLOCK_TBL | FACILITY_TBL | fk_block_facility | CASCADE | CASCADE |
| FACILITY_RESERVATION_LOG | FACILITY_RESERVATION_TBL | fk_log_reservation | CASCADE | CASCADE |
| REGIST_TABLE | USER_TBL | FK_REG_USER | - | - |
| SERIAL_CODE_TABLE | USER_TBL | fk_sct_user | - | - |
| LAMP_TBL | USER_TBL | fk_lamp_user | SET NULL | - |
| LAMP_USAGE_LOG | LAMP_TBL | fk_log_lamp | - | - |
| LAMP_USAGE_LOG | USER_TBL | fk_log_user | - | - |
| RENT_TABLE | USER_TBL | USER_IDX | NO ACTION | NO ACTION |
| DEPARTMENT | FACULTY | fk_dept_faculty | - | CASCADE |

### 인덱스 전략

**복합 인덱스**:
- `idx_facility_type_active` (시설 조회 최적화)
- `idx_reservation_facility_time` (예약 충돌 체크)
- `idx_reservation_user_status` (사용자 예약 목록)
- `idx_board_attachment_board_active` (활성 첨부파일)

**성능 최적화**:
- 날짜 범위 검색: `idx_reservation_status_time`
- 로그 조회: `idx_reservationlog_created`
- 사용자 조회: `idx_user_code`

---

## 📊 데이터 통계

```sql
-- 테이블별 행 수 확인
SELECT 
    TABLE_NAME,
    TABLE_ROWS,
    DATA_LENGTH,
    INDEX_LENGTH,
    ROUND((DATA_LENGTH + INDEX_LENGTH) / 1024 / 1024, 2) AS 'SIZE_MB'
FROM information_schema.TABLES
WHERE TABLE_SCHEMA = 'blue_crab'
ORDER BY (DATA_LENGTH + INDEX_LENGTH) DESC;
```

---

## 🔄 마이그레이션 이력

| 날짜 | 변경 내용 | 작성자 |
|------|----------|--------|
| 2025-10-10 | SSH 접속으로 실제 DB 스키마 확인 및 문서화 | GitHub Copilot |

---

## 📝 주의사항

### Base64 인코딩

**BOARD_TBL**의 `BOARD_TIT`, `BOARD_CONT`는 Base64 인코딩되어 저장됨
```java
// 저장 시
String encodedTitle = Base64.getEncoder().encodeToString(title.getBytes());

// 조회 시
String decodedTitle = new String(Base64.getDecoder().decode(encodedTitle));
```

### 날짜/시간 형식

- **datetime**: MySQL DATETIME 타입 사용
- **varchar**: 일부 레거시 테이블에서 문자열로 저장
- **타임존**: `Asia/Seoul` 고정

### 문자 인코딩

- **utf8mb3**: 레거시 테이블 (USER_TBL, ADMIN_TBL, BOARD_TBL, LEC_TBL, LAMP_TBL, RENT_TABLE)
- **utf8mb4**: 신규 테이블 (이모지 지원)

---

## 🔗 관련 문서

- [기술 스택 및 버전 정보](../tech-stack/기술스택_및_버전정보.md)
- [백엔드 폴더 구조](../backend-guide/백엔드_폴더구조_빠른참조.md)
- [API 문서](../api-endpoints/api-documentation.md)
- [시설 예약 시스템](../feature-docs/facility-reservation/README.md)

---

**작성자**: GitHub Copilot  
**최종 수정일**: 2025-10-10  
**버전**: 1.0  
**다음 업데이트**: 스키마 변경 시
