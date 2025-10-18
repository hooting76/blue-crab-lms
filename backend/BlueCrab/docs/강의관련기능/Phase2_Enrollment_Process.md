# Phase 2: 수강신청 단계 API 가이드

> **단계**: 수강신청 (학생)
> **주요 액터**: 학생
> **목적**: 수강 가능 강의 조회 및 신청

## 📋 단계 개요

학생들이 학기 시작 전에 수강하고자 하는 강의를 신청하는 단계입니다.

### 주요 기능
- 수강 가능 강의 조회 (학부/학과 필터링)
- 수강신청 처리
- 수강목록 조회 및 관리

### 핵심 로직: 0값 규칙
- `lecMcode = "0"` 또는 `lecMcodeDep = "0"`: 모든 학생 수강 가능
- 학생의 전공(Mcode) 또는 부전공(McodeSub)이 강의 코드와 일치해야 수강 가능

---

## 🔧 API 명세서

### 1. 수강 가능 강의 조회

**엔드포인트**: `POST /api/lectures/eligible`

**목적**: 학생이 수강할 수 있는 강의 목록을 조회합니다. 학부/학과 기반 필터링 적용.

**Request Body**:
```json
{
  "studentId": 100,  // 학생 ID (필수)
  "page": 0,         // 페이지 번호 (기본: 0)
  "size": 20         // 페이지 크기 (기본: 20)
}
```

**Response (성공)**:
```json
{
  "eligibleLectures": [
    {
      "lecSerial": "CS101",
      "lecTit": "자료구조",
      "lecProf": "22",
      "lecPoint": 3,
      "lecTime": "월1수1",
      "lecCurrent": 25,
      "lecMany": 30,
      "lecMcode": "01",
      "lecMcodeDep": "001",
      "lecMin": 0,
      "isEligible": true,
      "eligibilityReason": "수강 가능 (전공 일치: 01-001)"
    }
  ],
  "totalCount": 45,
  "eligibleCount": 30,
  "ineligibleCount": 15,
  "pagination": {
    "currentPage": 0,
    "pageSize": 20,
    "totalElements": 30,
    "totalPages": 2
  },
  "studentInfo": {
    "userIdx": 100,
    "userName": "홍길동",
    "majorFacultyCode": "01",
    "majorDeptCode": "001"
  }
}
```

**⚠️ 주의**: 실제 응답은 배열이 아닌 객체이며, `eligibleLectures` 키로 강의 목록을 반환합니다.

**Response (에러)**:
```json
{
  "success": false,
  "message": "studentId는 필수입니다.",
  "timestamp": "2025-10-17T10:00:00Z"
}
```

**프론트엔드 호출 예시**:
```javascript
const response = await fetch('/api/lectures/eligible', {
  method: 'POST',
  headers: {
    'Authorization': `Bearer ${token}`,
    'Content-Type': 'application/json'
  },
  body: JSON.stringify({
    studentId: currentUser.userIdx,
    page: 0,
    size: 10
  })
});

const eligibleLectures = await response.json();
// 필터링된 강의 목록 표시
```

---

### 2. 수강신청

**엔드포인트**: `POST /api/enrollments/enroll`

**목적**: 선택한 강의에 수강신청을 합니다.

**Request Body**:
```json
{
  "studentIdx": 100,        // 학생 ID (필수)
  "lecSerial": "CS101"      // 강의 코드 (필수)
}
```

**Response (성공)**:
```json
{
  "enrollmentIdx": 1,
  "lecIdx": 1,
  "studentIdx": 100,
  "enrollmentData": "{\"enrollment\":{\"status\":\"ENROLLED\",\"enrollmentDate\":\"2025-03-01T09:00:00\"}}"
}
```

**⚠️ 주의**: 실제 응답은 `EnrollmentExtendedTbl` 엔티티를 직접 반환하며, `success`/`message` 래퍼가 없습니다.

**Response (에러 케이스)**:
```json
// 정원 초과
{
  "success": false,
  "message": "강의 정원이 초과되었습니다.",
  "timestamp": "2025-10-17T10:00:00Z"
}

// 중복 신청
{
  "success": false,
  "message": "이미 수강신청한 강의입니다.",
  "timestamp": "2025-10-17T10:00:00Z"
}
```

**프론트엔드 호출 예시**:
```javascript
const enrollResponse = await fetch('/api/enrollments/enroll', {
  method: 'POST',
  headers: {
    'Authorization': `Bearer ${token}`,
    'Content-Type': 'application/json'
  },
  body: JSON.stringify({
    studentIdx: currentUser.userIdx,
    lecSerial: selectedLecture.lecSerial
  })
});

if (enrollResponse.ok) {
  const enrollment = await enrollResponse.json();
  console.log('수강신청 완료:', enrollment);
  alert('수강신청 완료!');
  // 목록 새로고침
} else {
  const error = await enrollResponse.json();
  alert(error.message || '수강신청 실패');
}
```

---

### 3. 수강목록 조회

**엔드포인트**: `POST /api/enrollments/list`

**목적**: 학생의 수강중인 강의 목록을 조회합니다.

**Request Body**:
```json
{
  "studentIdx": 100,  // 학생 ID (필수)
  "enrolled": true,   // 현재 수강중인 것만 (필수)
  "page": 0,          // 페이지 번호 (기본: 0)
  "size": 20          // 페이지 크기 (기본: 20)
}
```

**Response (성공)**:
```json
{
  "content": [
    {
      "enrollmentIdx": 123,
      "studentIdx": 100,
      "lecIdx": 1,
      "lecSerial": "CS101",
      "lecTit": "자료구조",
      "lecProfName": "김교수",
      "lecYear": 1,
      "lecSemester": 1,
      "lecPoint": 3,
      "lecTime": "월1,수1",
      "enrollmentDate": "2025-03-01T00:00:00Z",
      "status": "ACTIVE"
    }
  ],
  "totalElements": 5,
  "totalPages": 1,
  "size": 20,
  "number": 0
}
```

**프론트엔드 호출 예시**:
```javascript
const myCoursesResponse = await fetch('/api/enrollments/list', {
  method: 'POST',
  headers: {
    'Authorization': `Bearer ${token}`,
    'Content-Type': 'application/json'
  },
  body: JSON.stringify({
    studentIdx: currentUser.userIdx,
    enrolled: true
  })
});

const myCourses = await myCoursesResponse.json();
// 수강목록 표시
```

---

### 4. 수강신청 여부 확인

**엔드포인트**: `POST /api/enrollments/list`

**목적**: 특정 강의에 대한 수강신청 여부를 확인합니다.

**Request Body**:
```json
{
  "studentIdx": 100,                    // 학생 ID (필수)
  "lecSerial": "CS101",      // 강의 코드 (필수)
  "checkEnrollment": true               // 수강 여부 확인 플래그 (필수)
}
```

**Response (성공)**:
```json
{
  "enrolled": true,
  "studentIdx": 100,
  "lecSerial": "CS101"
}
```

**프론트엔드 호출 예시**:
```javascript
const checkResponse = await fetch('/api/enrollments/list', {
  method: 'POST',
  headers: {
    'Authorization': `Bearer ${token}`,
    'Content-Type': 'application/json'
  },
  body: JSON.stringify({
    studentIdx: currentUser.userIdx,
    lecSerial: selectedLecture.lecSerial,
    checkEnrollment: true
  })
});

const checkResult = await checkResponse.json();
if (checkResult.enrolled) {
  // 이미 신청한 강의 표시
}
```

---

## 🔄 플로우 다이어그램

```
학생 로그인
    ↓
수강 가능 강의 조회
    ↓ (강의 선택)
수강신청 여부 확인
    ↓ (미신청 시)
수강신청 처리
    ↓
수강목록 조회 및 확인
```

## 📝 구현 가이드

### 프론트엔드 구현 포인트
1. **실시간 정원 확인**: 수강신청 전 availableSeats 체크
2. **중복 신청 방지**: checkEnrollment API로 사전 확인
3. **즉시 피드백**: 신청 결과에 따른 UI 업데이트
4. **에러 처리**: 정원 초과, 중복 신청 등 다양한 에러 케이스

### 수강 자격 검증 로직
```javascript
function isEligible(student, lecture) {
  // 0값 규칙: "0"은 전체 허용 (모든 학생 수강 가능)
  if (lecture.lecMcode === "0" || lecture.lecMcodeDep === "0") {
    return true;
  }

  // 학생의 전공 정보 조회 (SERIAL_CODE_TABLE에서 가져옴)
  // student.majorCodes: [전공 학부코드, 부전공 학부코드] (null 가능)
  // student.deptCodes: [전공 학과코드, 부전공 학과코드] (null 가능)

  // 전공 학부 일치 확인
  const majorFacultyMatch = student.majorCodes?.some(code =>
    code === lecture.lecMcode
  );

  // 전공 학과 일치 확인
  const majorDeptMatch = student.deptCodes?.some(code =>
    code === lecture.lecMcodeDep
  );

  return majorFacultyMatch || majorDeptMatch;
}
```

**백엔드 구현 참고**:
```java
// 학생 전공 정보 조회 (SERIAL_CODE_TABLE)
List<String> majorCodes = new ArrayList<>();
List<String> deptCodes = new ArrayList<>();

if (student.getSerialCode() != null) {
    majorCodes.add(student.getSerialCode());  // 전공 학부
    deptCodes.add(student.getSerialSub());    // 전공 학과
}
if (student.getSerialCodeNd() != null) {
    majorCodes.add(student.getSerialCodeNd()); // 부전공 학부
    deptCodes.add(student.getSerialSubNd());   // 부전공 학과
}

// 강의 수강 자격 검증
boolean isEligible = false;
if ("0".equals(lecture.getLecMcode()) || "0".equals(lecture.getLecMcodeDep())) {
    isEligible = true;  // 전체 허용
} else {
    isEligible = majorCodes.contains(lecture.getLecMcode()) ||
                 deptCodes.contains(lecture.getLecMcodeDep());
}
```

### UI/UX 고려사항
- 수강 가능 강의와 불가능 강의를 시각적으로 구분
- 실시간 정원 정보 표시
- 신청 버튼 상태: 신청 가능/불가능/이미 신청
- 신청 완료 후 목록 자동 새로고침

### 성능 최적화
- 페이징을 통한 대량 데이터 처리
- 캐싱으로 반복 조회 최소화
- WebSocket으로 실시간 정원 정보 (선택)

---

**다음 단계**: [Phase3_Semester_Progress.md](../Phase3_Semester_Progress.md)에서 학기 진행 단계 API를 확인하세요.</content>
<parameter name="filePath">F:\main_project\works\blue-crab-lms\backend\BlueCrab\docs\강의관련기능\Phase2_Enrollment_Process.md