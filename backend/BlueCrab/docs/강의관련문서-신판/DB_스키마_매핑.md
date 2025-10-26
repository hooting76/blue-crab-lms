# DB 스키마 매핑 문서

## 📋 개요

강의 관련 API와 데이터베이스 테이블 간의 매핑 관계를 정리한 문서입니다.

---

## 🗄️ 테이블 목록 및 용도

### 1. LEC_TBL (강의 테이블)

**용도**: 강의 기본 정보 저장

**관련 API**: 강의 API 전체

**주요 컬럼**:
```sql
LEC_IDX          INT PRIMARY KEY AUTO_INCREMENT
LEC_SERIAL       VARCHAR(50)   -- 강의 코드 (예: CS284)
LEC_TIT          VARCHAR(255)  -- 강의명
LEC_PROF         INT           -- 교수 USER_IDX (FK)
LEC_POINT        INT           -- 학점
LEC_MAJOR        INT           -- 전공 여부 (1: 전공, 0: 교양)
LEC_MUST         INT           -- 필수 여부 (1: 필수, 0: 선택)
LEC_SUMMARY      TEXT          -- 강의 설명
LEC_TIME         VARCHAR(100)  -- 강의 시간 (예: 월1월2수1수2)
LEC_ASSIGN       INT           -- 과제 유무
LEC_OPEN         INT           -- 개설 여부 (1: 개설, 0: 미개설)
LEC_MANY         INT           -- 정원
LEC_MCODE        VARCHAR(10)   -- 학부 코드 (FK → FACULTY)
LEC_MCODE_DEP    VARCHAR(10)   -- 학과 코드 (FK → DEPARTMENT)
LEC_MIN          INT           -- 최소 인원
LEC_REG          VARCHAR(50)   -- 등록 일시
LEC_IP           VARCHAR(50)   -- 등록 IP
LEC_CURRENT      INT           -- 현재 수강 인원
LEC_YEAR         INT           -- 학년
LEC_SEMESTER     INT           -- 학기 (1: 1학기, 2: 2학기)
```

**외래 키**:
- `LEC_PROF` → `USER_TBL.USER_IDX`
- `LEC_MCODE` → `FACULTY.faculty_code`
- `LEC_MCODE_DEP` → `DEPARTMENT.dept_code`

**인덱스**:
- `LEC_SERIAL` (UNIQUE)
- `LEC_PROF`
- `LEC_OPEN`

---

### 2. ENROLLMENT_EXTENDED_TBL (수강신청 테이블)

**용도**: 수강신청 및 성적, 출석 데이터 저장

**관련 API**: 수강신청 API, 출석 API

**주요 컬럼**:
```sql
ENROLLMENT_IDX   INT PRIMARY KEY AUTO_INCREMENT
LEC_IDX          INT NOT NULL   -- 강의 ID (FK)
STUDENT_IDX      INT NOT NULL   -- 학생 ID (FK)
ENROLLMENT_DATA  TEXT           -- JSON 형식 데이터
```

**ENROLLMENT_DATA 구조 (JSON)**:
```json
{
  "attendance": "1출2출3결4지5출",
  "attendanceRate": "4/5",
  "midterm": 85,
  "final": 90,
  "assignment": 95,
  "totalGrade": 92.5,
  "letterGrade": "A+"
}
```

**외래 키**:
- `LEC_IDX` → `LEC_TBL.LEC_IDX`
- `STUDENT_IDX` → `USER_TBL.USER_IDX`

**인덱스**:
- `LEC_IDX`
- `STUDENT_IDX`
- `(STUDENT_IDX, LEC_IDX)` (복합 인덱스)

---

### 3. ASSIGNMENT_EXTENDED_TBL (과제 테이블)

**용도**: 과제 정보 저장

**관련 API**: 과제 API 전체

**주요 컬럼**:
```sql
ASSIGN_IDX       INT PRIMARY KEY AUTO_INCREMENT
LEC_IDX          INT NOT NULL         -- 강의 ID (FK)
ASSIGN_TITLE     VARCHAR(255)         -- 과제 제목
ASSIGN_CONTENT   TEXT                 -- 과제 내용
ASSIGN_DUE_DATE  DATETIME             -- 마감일
ASSIGN_MAX_SCORE INT                  -- 배점
ASSIGN_CREATED_AT DATETIME            -- 생성 일시
ASSIGN_FILES     TEXT                 -- 첨부파일 (JSON)
```

**ASSIGN_FILES 구조 (JSON)**:
```json
[
  {
    "name": "template.zip",
    "url": "/files/assignments/1/template.zip"
  }
]
```

**외래 키**:
- `LEC_IDX` → `LEC_TBL.LEC_IDX`

**인덱스**:
- `LEC_IDX`
- `ASSIGN_DUE_DATE`

---

### 4. ATTENDANCE_REQUEST_TBL (출석 요청 테이블)

**용도**: 출석 인정 요청 정보 저장

**관련 API**: 출석 API (요청/승인)

**주요 컬럼**:
```sql
request_idx      INT PRIMARY KEY AUTO_INCREMENT
lec_idx          INT NOT NULL         -- 강의 ID (FK)
student_idx      INT NOT NULL         -- 학생 ID (FK)
session_number   INT NOT NULL         -- 회차 번호
request_reason   TEXT                 -- 요청 사유
status           VARCHAR(20)          -- 상태 (PENDING, APPROVED, REJECTED)
rejection_reason TEXT                 -- 반려 사유
requested_at     DATETIME             -- 요청 일시
processed_at     DATETIME             -- 처리 일시
```

**외래 키**:
- `lec_idx` → `LEC_TBL.LEC_IDX`
- `student_idx` → `USER_TBL.USER_IDX`

**인덱스**:
- `lec_idx`
- `student_idx`
- `status`

---

### 5. COURSE_APPLY_NOTICE (수강신청 안내문 테이블)

**용도**: 수강신청 안내문 저장

**관련 API**: 공지사항 API

**주요 컬럼**:
```sql
notice_idx       INT PRIMARY KEY AUTO_INCREMENT
message          TEXT              -- 안내문 내용 (HTML)
updated_at       DATETIME          -- 최종 수정 일시
updated_by       VARCHAR(255)      -- 최종 수정자 (이메일)
created_at       DATETIME          -- 생성 일시
```

**인덱스**:
- `updated_at` (DESC)

---

### 6. FACULTY (학부 테이블)

**용도**: 학부 정보 저장

**관련 API**: 강의 API (조회 시 JOIN)

**주요 컬럼**:
```sql
faculty_id       INT PRIMARY KEY AUTO_INCREMENT
faculty_code     VARCHAR(10)       -- 학부 코드 (예: 01, 02)
faculty_name     VARCHAR(100)      -- 학부명
established_at   VARCHAR(10)       -- 설립 연도
capacity         INT               -- 정원
```

**샘플 데이터**:
```sql
(1, '01', '해양학부', '2025', 410)
(2, '02', '보건학부', '2025', 340)
(3, '03', '자연과학부', '2025', 120)
(4, '04', '인문학부', '2025', 320)
(5, '05', '공학부', '2025', 320)
```

---

### 7. DEPARTMENT (학과 테이블)

**용도**: 학과 정보 저장

**관련 API**: 강의 API (조회 시 JOIN)

**주요 컬럼**:
```sql
dept_id          INT PRIMARY KEY AUTO_INCREMENT
dept_code        VARCHAR(10)       -- 학과 코드 (예: 01, 02)
dept_name        VARCHAR(100)      -- 학과명
faculty_id       INT               -- 학부 ID (FK)
established_at   VARCHAR(10)       -- 설립 연도
capacity         INT               -- 정원
```

**외래 키**:
- `faculty_id` → `FACULTY.faculty_id`

**샘플 데이터**:
```sql
(1, '01', '항해학과', 1, '2025', 50)
(2, '02', '해양경찰', 1, '2025', 70)
(25, '01', '컴퓨터공학', 5, '2025', 125)
(28, '04', 'ICT융합', 5, '2025', 125)
```

---

### 8. USER_TBL (사용자 테이블)

**용도**: 학생, 교수, 관리자 정보 저장

**관련 API**: 모든 API (인증/권한)

**주요 컬럼**:
```sql
USER_IDX         INT PRIMARY KEY AUTO_INCREMENT
USER_EMAIL       VARCHAR(100)      -- 이메일 (로그인 ID)
USER_PW          VARCHAR(255)      -- 비밀번호 (해시)
USER_NAME        VARCHAR(100)      -- 이름
USER_CODE        VARCHAR(50)       -- 학번/교번
USER_PHONE       VARCHAR(20)       -- 전화번호
USER_STUDENT     INT               -- 유형 (0: 학생, 1: 교수, 2: 관리자)
PROFILE_IMAGE_KEY VARCHAR(255)     -- 프로필 이미지 경로
```

**인덱스**:
- `USER_EMAIL` (UNIQUE)
- `USER_CODE` (UNIQUE)
- `USER_STUDENT`

---

### 9. PROFILE_VIEW (프로필 뷰)

**용도**: 사용자 프로필 정보 조회 (VIEW)

**관련 API**: 강의 API (교수 정보 조회)

**SELECT 쿼리**:
```sql
SELECT 
  u.USER_IDX,
  u.USER_NAME,
  u.USER_EMAIL,
  u.USER_CODE,
  u.PROFILE_IMAGE_KEY,
  d.dept_name,
  f.faculty_name
FROM USER_TBL u
LEFT JOIN SERIAL_CODE_TABLE s ON u.USER_IDX = s.USER_IDX
LEFT JOIN DEPARTMENT d ON s.SERIAL_CODE = d.dept_code
LEFT JOIN FACULTY f ON d.faculty_id = f.faculty_id
```

---

## 🔗 API별 테이블 매핑

### 강의 API
| API 기능 | 주 테이블 | JOIN 테이블 |
|---------|---------|-----------|
| 강의 조회 | LEC_TBL | USER_TBL, PROFILE_VIEW |
| 강의 생성 | LEC_TBL | - |
| 강의 수정 | LEC_TBL | - |
| 강의 삭제 | LEC_TBL | - |
| 강의 통계 | LEC_TBL, ENROLLMENT_EXTENDED_TBL | - |

### 수강신청 API
| API 기능 | 주 테이블 | JOIN 테이블 |
|---------|---------|-----------|
| 수강신청 조회 | ENROLLMENT_EXTENDED_TBL | LEC_TBL, USER_TBL |
| 수강신청 등록 | ENROLLMENT_EXTENDED_TBL | LEC_TBL |
| 수강신청 취소 | ENROLLMENT_EXTENDED_TBL | LEC_TBL |
| 성적 입력 | ENROLLMENT_EXTENDED_TBL | - |

### 과제 API
| API 기능 | 주 테이블 | JOIN 테이블 |
|---------|---------|-----------|
| 과제 조회 | ASSIGNMENT_EXTENDED_TBL | LEC_TBL |
| 과제 생성 | ASSIGNMENT_EXTENDED_TBL | LEC_TBL |
| 과제 제출 | ASSIGNMENT_SUBMISSION_TBL | ASSIGNMENT_EXTENDED_TBL |
| 과제 채점 | ASSIGNMENT_SUBMISSION_TBL | - |

### 출석 API
| API 기능 | 주 테이블 | JOIN 테이블 |
|---------|---------|-----------|
| 출석 조회 | ENROLLMENT_EXTENDED_TBL | LEC_TBL, USER_TBL |
| 출석 요청 | ATTENDANCE_REQUEST_TBL | LEC_TBL, ENROLLMENT_EXTENDED_TBL |
| 출석 승인 | ATTENDANCE_REQUEST_TBL, ENROLLMENT_EXTENDED_TBL | - |
| 출석 체크 | ENROLLMENT_EXTENDED_TBL | - |

### 공지사항 API
| API 기능 | 주 테이블 | JOIN 테이블 |
|---------|---------|-----------|
| 안내문 조회 | COURSE_APPLY_NOTICE | - |
| 안내문 저장 | COURSE_APPLY_NOTICE | - |

---

## 📊 ER 다이어그램 (간소화)

```
┌─────────────┐
│  FACULTY    │
│  (학부)      │
└──────┬──────┘
       │ 1
       │
       │ N
┌──────┴──────┐
│ DEPARTMENT  │
│  (학과)      │
└──────┬──────┘
       │
       │
       │
┌──────┴──────────┐        ┌─────────────────┐
│    USER_TBL     │───────│  PROFILE_VIEW   │
│   (사용자)       │  VIEW  │   (프로필 뷰)     │
└──────┬──────────┘        └─────────────────┘
       │
       │ 교수(1) ────────────── 강의(N)
       │                      │
       │                      │
       │               ┌──────┴──────┐
       │               │   LEC_TBL   │
       │               │   (강의)     │
       │               └──────┬──────┘
       │                      │
       │                      │ 1
       │                      │
       │                      │ N
       │               ┌──────┴───────────────────┐
       │               │                          │
       │        ┌──────┴──────────┐    ┌─────────┴────────────┐
       │        │ ENROLLMENT_     │    │  ASSIGNMENT_         │
       │        │ EXTENDED_TBL    │    │  EXTENDED_TBL        │
       │        │  (수강신청)      │    │   (과제)             │
       │        └──────┬──────────┘    └──────────────────────┘
       │ 학생(1)       │ N
       └───────────────┘
                       │
                       │
                ┌──────┴──────────┐
                │ ATTENDANCE_     │
                │ REQUEST_TBL     │
                │ (출석 요청)      │
                └─────────────────┘

┌─────────────────────┐
│ COURSE_APPLY_NOTICE │
│  (수강신청 안내문)    │
└─────────────────────┘
```

---

## ⚠️ 주의사항

1. **CASCADE 삭제**: 강의 삭제 시 관련 수강신청, 과제, 출석 데이터 처리 필요
2. **트랜잭션**: 수강신청 등록/취소 시 `LEC_CURRENT` 업데이트 동기화
3. **JSON 필드**: `ENROLLMENT_DATA`, `ASSIGN_FILES` 등은 JSON 형식으로 저장
4. **VIEW 갱신**: `PROFILE_VIEW`는 실시간 데이터이므로 캐싱 주의

---

© 2025 BlueCrab LMS Development Team
