# Phase 1: 학기 준비 단계 API 가이드

> **단계**: 학기 준비 (관리자)
> **주요 액터**: 관리자
> **목적**: 강의 등록 및 기본 정보 설정

## 📋 단계 개요

학기 시작 전에 관리자가 강의를 등록하고 기본 정보를 설정하는 단계입니다.

### 주요 기능
- 강의 등록/수정/삭제
- 강의 목록 조회 및 필터링
- 강의 통계 조회

---

## 🔧 API 명세서

### 1. 강의 목록 조회 및 검색

**엔드포인트**: `POST /api/lectures`

**목적**: 강의 목록을 조회하거나 검색합니다. 다양한 필터링 옵션 지원.

**Request Body**:
```json
{
  "serial": "CS101",     // 강의코드로 단일 조회 (다른 필드와 함께 사용 불가)
  "professor": "김교수",             // 교수명 필터 (단독 사용 시 교수별 조회)
  "year": 1,                        // 대상 학년 필터 (복합 검색 시 사용)
  "semester": 1,                    // 학기 필터 (복합 검색 시 사용)
  "title": "자료구조",               // 강의명 검색 (단독 사용 시 제목 검색)
  "major": 1,                       // 전공/교양 구분 필터 (LEC_MAJOR: 1=전공, 0=교양, 복합 검색 시 사용)
  "open": 1,                        // 개설 상태 필터 (LEC_OPEN: 1=개설, 0=폐강, 복합 검색 시 사용)
  "page": 0,                        // 페이지 번호 (기본: 0)
  "size": 20                        // 페이지 크기 (기본: 20)
}
```

**조회 방식**:
- **단일 강의 조회**: `serial` 필드만 사용
- **교수별 조회**: `professor` 필드만 사용 (다른 필터와 함께 사용 불가)
- **강의명 검색**: `title` 필드만 사용 (다른 필터와 함께 사용 불가)
- **복합 검색**: `year`, `semester`, `major`, `open` 중 하나 이상 사용
- **전체 목록**: 모든 필드 생략 또는 `page`, `size`만 사용

**Response (성공)**:
```json
{
  "content": [
    {
      "lecIdx": 1,
      "lecSerial": "CS101",
      "lecTit": "자료구조",
      "lecProf": 22,
      "lecProfName": "김교수",
      "lecYear": 1,
      "lecSemester": 1,
      "lecMany": 30,
      "lecCurrent": 25,
      "lecPoint": 3,
      "lecTime": "월1,수1",
      "lecMcode": "01",
      "lecMcodeDep": "001",
      "availableSeats": 5
    }
  ],
  "totalElements": 1,
  "totalPages": 1,
  "size": 20,
  "number": 0,
  "first": true,
  "last": true
}
```

**Response (에러)**:
```json
{
  "success": false,
  "message": "강의 조회 중 오류가 발생했습니다.",
  "timestamp": "2025-10-17T10:00:00Z"
}
```

**프론트엔드 호출 예시**:
```javascript
// 1. 전체 강의 목록 조회 (페이징)
const allLectures = await fetch('/api/lectures', {
  method: 'POST',
  headers: {
    'Authorization': `Bearer ${token}`,
    'Content-Type': 'application/json'
  },
  body: JSON.stringify({
    page: 0,
    size: 10
  })
});

// 2. 단일 강의 조회 (강의코드로)
const singleLecture = await fetch('/api/lectures', {
  method: 'POST',
  headers: {
    'Authorization': `Bearer ${token}`,
    'Content-Type': 'application/json'
  },
  body: JSON.stringify({
    serial: "CS101"
  })
});

// 3. 교수별 강의 조회
const profLectures = await fetch('/api/lectures', {
  method: 'POST',
  headers: {
    'Authorization': `Bearer ${token}`,
    'Content-Type': 'application/json'
  },
  body: JSON.stringify({
    professor: "김교수"
  })
});

// 4. 강의명 검색
const titleSearch = await fetch('/api/lectures', {
  method: 'POST',
  headers: {
    'Authorization': `Bearer ${token}`,
    'Content-Type': 'application/json'
  },
  body: JSON.stringify({
    title: "자료구조"
  })
});

// 5. 복합 검색 (학년 + 학기 + 전공)
const complexSearch = await fetch('/api/lectures', {
  method: 'POST',
  headers: {
    'Authorization': `Bearer ${token}`,
    'Content-Type': 'application/json'
  },
  body: JSON.stringify({
    year: 1,
    semester: 1,
    major: 1,
    page: 0,
    size: 20
  })
});
```

---

### 2. 강의 상세 조회

**엔드포인트**: `POST /api/lectures/detail`

**목적**: 특정 강의의 상세 정보를 조회합니다.

**Request Body**:
```json
{
  "lecSerial": "CS101"  // 강의 코드 (필수)
}
```

**Response (성공)**:
```json
{
  "lecIdx": 1,
  "lecSerial": "CS101",
  "lecTit": "자료구조",
  "lecProf": 22,
  "lecProfName": "김교수",
  "lecYear": 1,
  "lecSemester": 1,
  "lecMany": 30,
  "lecCurrent": 25,
  "lecPoint": 3,
  "lecTime": "월1,수1",
  "lecMcode": "01",
  "lecMcodeDep": "001",
  "lecDesc": "자료구조에 대한 심층 학습",
  "lecRoom": "공학관 101호"
}
```

**프론트엔드 호출 예시**:
```javascript
const response = await fetch('/api/lectures/detail', {
  method: 'POST',
  headers: {
    'Authorization': `Bearer ${token}`,
    'Content-Type': 'application/json'
  },
  body: JSON.stringify({
    lecSerial: "CS101-001-2025-1"
  })
});
```

---

### 3. 강의 통계 조회

**엔드포인트**: `POST /api/lectures/stats`

**목적**: 강의별 수강생 통계 및 현황을 조회합니다.

**Request Body**:
```json
{
  "lecSerial": "CS101"  // 강의 코드 (필수)
}
```

**Response (성공)**:
```json
{
  "lectureInfo": {
    "lecSerial": "CS101",
    "lecTit": "자료구조",
    "lecProfName": "김교수"
  },
  "enrollmentStats": {
    "totalEnrolled": 25,
    "capacity": 30,
    "availableSeats": 5,
    "enrollmentRate": 83.3
  },
  "attendanceStats": {
    "totalSessions": 15,
    "averageAttendance": 22.5,
    "attendanceRate": 90.0
  },
  "assignmentStats": {
    "totalAssignments": 5,
    "averageSubmissions": 23,
    "submissionRate": 92.0
  }
}
```

**프론트엔드 호출 예시**:
```javascript
const response = await fetch('/api/lectures/stats', {
  method: 'POST',
  headers: {
    'Authorization': `Bearer ${token}`,
    'Content-Type': 'application/json'
  },
  body: JSON.stringify({
    lecSerial: "CS101-001-2025-1"
  })
});
```

---

## 🔄 플로우 다이어그램

```
관리자 로그인
    ↓
강의 목록 조회 (필터링)
    ↓
강의 상세 조회 (선택)
    ↓
강의 통계 확인
    ↓
강의 등록/수정 (필요시)
```

## 📝 구현 가이드

### 프론트엔드 구현 포인트
1. **페이징 처리**: Spring Page 객체 구조 이해
2. **필터링 UI**: 다중 필터 옵션 제공
3. **실시간 검색**: debounced 입력 처리
4. **통계 시각화**: 차트 라이브러리 활용

### 데이터 처리 팁
- `lecSerial`을 강의 식별자로 사용 (프론트엔드에서 저장)
- `availableSeats` = `lecMany` - `lecCurrent` 계산
- 필터링 시 빈 값은 전체 조회 의미

### 에러 처리
- 401: 토큰 만료 → 재로그인
- 404: 강의 없음 → 목록으로 리다이렉트
- 500: 서버 오류 → 사용자 친화적 메시지

---

**다음 단계**: [Phase2_Enrollment_Process.md](../Phase2_Enrollment_Process.md)에서 수강신청 단계 API를 확인하세요.</content>
<parameter name="filePath">F:\main_project\works\blue-crab-lms\backend\BlueCrab\docs\강의관련기능\Phase1_Lecture_Preparation.md