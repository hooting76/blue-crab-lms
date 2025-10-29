# 성적 관리 시스템 API 명세서

> PPT 다이어그램 제작을 위한 핵심 API 문서

## 📋 목차

1. [**수강신청 API**](./01_수강신청_API.md) - 강의 등록
2. [**성적 구성 설정 API**](./02_성적구성설정_API.md) - 출석/과제 배점 설정
3. [**출석 관리 API**](./03_출석관리_API.md) - 출석 요청/승인
4. [**과제 관리 API**](./04_과제관리_API.md) - 과제 생성/채점
5. [**성적 조회 API**](./05_성적조회_API.md) - 학생/교수 성적 확인
6. [**최종 등급 배정 API**](./06_최종등급배정_API.md) - 상대평가 등급 부여

---

## 🔄 전체 프로세스 흐름

```
학기 시작
    ↓
① 수강신청 (학생)
    ↓
② 성적 구성 설정 (교수)
    ↓
③ 출석 관리 (학생 요청 → 교수 승인)
    ↓
④ 과제 관리 (교수 출제 → 학생 제출 → 교수 채점)
    ↓
⑤ 성적 조회 (학생/교수)
    ↓
⑥ 최종 등급 배정 (교수)
    ↓
학기 종료
```

---

## 📊 핵심 데이터베이스

### ENROLLMENT_EXTENDED_TBL

수강신청 정보 및 성적 데이터를 저장하는 핵심 테이블

**주요 컬럼**:
- `ENROLLMENT_IDX`: 수강신청 ID (PK, AUTO_INCREMENT)
- `LEC_IDX`: 강의 ID (FK → LEC_TBL.LEC_IDX)
- `STUDENT_IDX`: 학생 ID (FK → USER_TBL.USER_IDX)
- `ENROLLMENT_DATA`: 성적 데이터 (LONGTEXT, JSON 형식)

**ENROLLMENT_DATA 구조**:
```json
{
  "gradeConfig": { /* 성적 구성 설정 */ },
  "attendance": { 
    "summary": { /* 출석 통계 */ },
    "sessions": [ /* 확정 출석 기록 */ ],
    "pendingRequests": [ /* 대기 중인 출석 요청 */ ]
  },
  "grade": { /* 계산된 성적 */ }
}
```

### ASSIGNMENT_EXTENDED_TBL

과제 정보를 저장하는 테이블

**주요 컬럼**:
- `ASSIGNMENT_IDX`: 과제 ID (PK, AUTO_INCREMENT)
- `LEC_IDX`: 강의 ID (FK → LEC_TBL.LEC_IDX)
- `ASSIGNMENT_DATA`: 과제 데이터 (LONGTEXT, JSON 형식)

**ASSIGNMENT_DATA 구조**:
```json
{
  "assignment": {
    "title": "과제명",
    "description": "과제 설명",
    "maxScore": 50,
    "dueDate": "2025-02-15T23:59:59",
    "status": "ACTIVE"
  },
  "submissions": [ /* 학생 제출 목록 */ ]
}
```

**⚠️ 중요**: 
- **ATTENDANCE_REQUEST_TBL 테이블은 존재하지 않습니다**
- 출석 데이터는 ENROLLMENT_DATA JSON에 저장됩니다
- 과제 데이터도 모두 JSON으로 관리되며, TASK_IDX, TASK_NAME 같은 개별 컬럼은 없습니다

---

## 🎯 주요 특징

### 1. JSON 기반 데이터 관리
- 출석, 과제, 성적 데이터를 `ENROLLMENT_DATA` JSON 컬럼에 저장
- 유연한 데이터 구조 확장 가능

### 2. 자동 성적 계산
- 출석/과제 데이터 입력 시 자동 재계산
- `GradeUpdateEvent` 이벤트 기반 처리

### 3. 과제 총점 자동 합산
- `ASSIGNMENT_EXTENDED_TBL`에서 실제 과제 배점 조회
- `assignmentTotalScore` 자동 계산

### 4. 상대평가 지원
- 백분율 기준 정렬
- 등급 분포(A/B/C/D %) 적용

---

## 🔗 관련 테이블

| 테이블 | 설명 |
|--------|------|
| `ENROLLMENT_EXTENDED_TBL` | 수강신청 및 성적 데이터 (출석 데이터 포함) |
| `LEC_TBL` | 강의 정보 |
| `USER_TBL` | 사용자 정보 (학생/교수) |
| `ASSIGNMENT_EXTENDED_TBL` | 과제 정보 (JSON 형식) |
| `FACULTY` | 단과대학 정보 |
| `DEPARTMENT` | 학과 정보 |

**⚠️ 존재하지 않는 테이블**: 
- `ATTENDANCE_REQUEST_TBL` - 출석은 ENROLLMENT_DATA JSON으로 관리
- 과제 테이블에 `TASK_IDX`, `TASK_NAME` 등의 개별 컬럼 없음 (모두 JSON)

---

## 📌 API 엔드포인트 요약

### 수강신청 및 설정

| 기능 | 엔드포인트 | 메서드 | 권한 | Request | 비고 |
|------|------------|--------|------|---------|------|
| 수강신청 | `/api/enrollments/enroll` | POST | 학생 | `lecSerial` | ENROLLMENT_DATA 초기화 |
| 성적 구성 설정 | `/api/enrollments/grade-config` | POST | 교수 | `lecSerial`, `attendanceMaxScore`, `latePenaltyPerSession`, `gradeDistribution` | 과제 총점은 자동 계산 |

### 출석 관리

| 기능 | 엔드포인트 | 메서드 | 권한 | Request | 비고 |
|------|------------|--------|------|---------|------|
| 출석 요청 | `/api/attendance/request` | POST | 학생 | `lecSerial`, `sessionDate`, `status` | JWT에서 studentIdx 추출 |
| 출석 승인 | `/api/attendance/approve` | POST | 교수 | `lecSerial`, `studentIdx`, `sessionDate`, `status` | pendingRequests → sessions |
| 학생 출석 조회 | `/api/attendance/student/view` | POST | 학생 | `lecSerial` | JWT에서 studentIdx 추출 |
| 교수 출석 조회 | `/api/attendance/professor/view` | POST | 교수 | `lecSerial`, `studentIdx` | 특정 학생 출석 내역 |

### 과제 관리

| 기능 | 엔드포인트 | 메서드 | 권한 | Request | 비고 |
|------|------------|--------|------|---------|------|
| 과제 목록 조회 | `/api/assignments/list` | POST | 공통 | `lecSerial`, `page`, `size` | assignmentData는 JSON 문자열 |
| 과제 상세 조회 | `/api/assignments/detail` | POST | 공통 | `assignmentIdx` | assignmentData는 JSON 문자열 |
| 과제 생성 | `/api/assignments` | POST | 교수 | `lecSerial`, `title`, `description`, `maxScore`, `dueDate` | assignmentTotalScore 자동 업데이트 |
| 과제 채점 | `/api/assignments/{assignmentIdx}/grade` | PUT | 교수 | `studentIdx`, `score`, `feedback` | GradeUpdateEvent 발행 |

### 성적 조회

| 기능 | 엔드포인트 | 메서드 | 권한 | Request | 비고 |
|------|------------|--------|------|---------|------|
| 학생 성적 조회 | `/api/enrollments/grade-info` | POST | 학생 | `action: "get-grade"`, `lecSerial`, `studentIdx` | 상세 정보 (과제별 점수, 피드백) |
| 교수 개별 조회 | `/api/enrollments/grade-info` | POST | 교수 | `action: "professor-view"`, `lecSerial`, `studentIdx` | 반 통계 포함 |
| 전체 성적 목록 | `/api/enrollments/grade-list` | POST | 교수 | `action: "list-all"`, `lecSerial`, `page`, `size`, `sortBy`, `sortOrder` | 정렬/페이징 지원 |

### 최종 등급 배정

| 기능 | 엔드포인트 | 메서드 | 권한 | Request | 비고 |
|------|------------|--------|------|---------|------|
| 최종 등급 배정 | `/api/enrollments/grade-finalize` | POST | 교수 | `action: "finalize"`, `lecSerial`, `passingThreshold`, `gradeDistribution` (선택) | F는 전체 학생 기준으로 배정 |

**⚠️ 중요**: 
- **action 기반 라우팅**: `/grade-info`, `/grade-list`, `/grade-finalize`는 Request Body에 `action` 필드 필요
- **JWT 인증**: 모든 API는 JWT 토큰 기반, `studentIdx`/`professorIdx` 자동 추출되는 경우 있음
- **JSON 문자열**: `assignmentData`는 문자열로 반환되므로 프론트엔드에서 `JSON.parse()` 필요
- **자동 계산**: `assignmentTotalScore`는 ASSIGNMENT_EXTENDED_TBL에서 자동 조회/합산

---

## 💡 사용 예시

### 학생 관점

```javascript
// 1. 수강신청
POST /api/enrollments/enroll
{
  "lecSerial": "ETH201"
}

// 2. 출석 요청
POST /api/attendance/request
{
  "lecSerial": "ETH201",
  "sessionDate": "2025-02-01",
  "status": "PRESENT"  // PRESENT, LATE, ABSENT
}

// 3. 내 출석 현황 조회
POST /api/attendance/student/view
{
  "lecSerial": "ETH201"
}

// 4. 내 성적 조회
POST /api/enrollments/grade-info
{
  "action": "get-grade",
  "lecSerial": "ETH201",
  "studentIdx": 33
}
```

### 교수 관점

```javascript
// 1. 성적 구성 설정 (강의 전체 대상)
POST /api/enrollments/grade-config
{
  "lecSerial": "ETH201",
  "attendanceMaxScore": 20,
  "latePenaltyPerSession": 0.3,
  "gradeDistribution": {
    "A": 30, "B": 40, "C": 20, "D": 10
  }
  // assignmentTotalScore는 자동 계산 (ASSIGNMENT_EXTENDED_TBL에서 조회)
}

// 2. 출석 승인
POST /api/attendance/approve
{
  "lecSerial": "ETH201",
  "studentIdx": 33,
  "sessionDate": "2025-02-01",
  "status": "PRESENT"
}

// 3. 학생 출석 현황 조회
POST /api/attendance/professor/view
{
  "lecSerial": "ETH201",
  "studentIdx": 33
}

// 4. 과제 생성
POST /api/assignments
{
  "lecSerial": "ETH201",
  "title": "중간과제",
  "description": "Servlet 구현",
  "maxScore": 50,
  "dueDate": "20250215"
}

// 5. 과제 목록 조회
POST /api/assignments/list
{
  "lecSerial": "ETH201",
  "page": 0,
  "size": 20
}

// 6. 과제 채점
PUT /api/assignments/15/grade
{
  "studentIdx": 33,
  "score": 45,
  "feedback": "잘했습니다."
}

// 7. 개별 학생 성적 조회
POST /api/enrollments/grade-info
{
  "action": "professor-view",
  "lecSerial": "ETH201",
  "studentIdx": 33
}

// 8. 전체 성적 목록 조회
POST /api/enrollments/grade-list
{
  "action": "list-all",
  "lecSerial": "ETH201",
  "page": 0,
  "size": 20,
  "sortBy": "percentage",
  "sortOrder": "desc"
}

// 9. 최종 등급 배정
POST /api/enrollments/grade-finalize
{
  "action": "finalize",
  "lecSerial": "ETH201",
  "passingThreshold": 60,
  "gradeDistribution": {  // 선택사항 (설정된 값 사용 가능)
    "A": 30, "B": 40, "C": 20, "D": 10
  }
}
```

---

## 🎓 성적 계산 로직

### 1. 출석 점수 계산

```
출석 점수 = (출석 횟수 × 출석 만점 / 전체 세션 수) - (지각 횟수 × 지각 감점)
```

**예시**:
- 출석 만점: 20점
- 전체 세션: 15회
- 출석: 10회, 지각: 3회, 결석: 2회
- 지각 감점: 0.3점/회

```
출석 점수 = (10 × 20 / 15) - (3 × 0.3) = 13.33 - 0.9 = 12.43점
```

### 2. 총점 계산

```
총점 = 출석 점수 + Σ(과제 점수)
백분율 = (총점 / 총 만점) × 100
```

**총 만점 계산**:
```
총 만점 = 출석 만점 + Σ(모든 과제의 maxScore)
```

### 3. 최종 등급 배정

**절대평가 (F 배정)**:
```
if (백분율 < 60%) → F
```

**상대평가 (A~D 배정)**:
```
백분율 >= 60%인 학생들을 대상으로 등급 분포 적용
```

**⚠️ 중요**: F 학생도 **전체 학생 수**에 포함되어 A~D 배정 인원 계산
- 총 100명, F가 40명일 경우
- A~D는 60명이 아닌 **100명 기준**으로 배정
- 예: A 30% → 30명 (60명의 50%가 아님!)
- F가 많으면 D부터 차례로 사라짐 (D→C→B 순으로 감소)

**배정 로직**:
```javascript
1. 전체 학생을 백분율 기준 내림차순 정렬
2. passingThreshold(60%) 미만 → F 배정
3. 60% 이상 학생들에게 순서대로 A→B→C→D 배정
4. 각 등급 인원 = Math.floor(전체 학생 수 × 등급 비율)
5. 남은 학생은 가장 낮은 등급(D)에 배정
```

---

## 📊 데이터 흐름 예시

### 시나리오: 학생 김철수의 성적 계산

1. **수강신청** (학생)
   - `lecSerial`: ETH201
   - `ENROLLMENT_DATA` 초기화

2. **성적 구성 설정** (교수)
   - 출석 만점: 20점
   - 지각 감점: 0.3점/회
   - 과제 총점: 자동 계산 (ASSIGNMENT_EXTENDED_TBL에서 조회)

3. **과제 생성** (교수)
   - 중간과제: 50점
   - 기말과제: 50점
   - → `assignmentTotalScore` 자동 업데이트: 100점
   - → `totalMaxScore` = 20 + 100 = 120점

4. **출석 기록**
   - 15회 중 출석 10회, 지각 3회, 결석 2회
   - 출석 점수: (10 × 20 / 15) - (3 × 0.3) = 12.43점

5. **과제 채점**
   - 중간과제: 45점
   - 기말과제: 40점
   - 과제 합계: 85점

6. **총점 계산**
   - 총점: 12.43 + 85 = 97.43점
   - 백분율: 97.43 / 120 × 100 = 81.2%

7. **최종 등급 배정**
   - 81.2% >= 60% → 상대평가 대상
   - 전체 순위에 따라 A/B/C/D 배정

---

© 2025 BlueCrab LMS - Grade Management System
