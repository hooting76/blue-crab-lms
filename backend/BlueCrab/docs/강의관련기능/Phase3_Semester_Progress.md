# Phase 3: 학기 진행 단계 API 가이드

> **단계**: 학기 진행 (교수 + 학생)
> **주요 액터**: 교수, 학생
> **목적**: 학기 중 출결, 과제, 학습 활동 관리

## 📋 단계 개요

학기가 시작된 후 교수와 학생이 참여하는 주요 활동 단계입니다.

### 주요 기능
- 출결 관리 (교수 승인 방식)
- 과제 관리 (생성/제출/채점)
- 학생 목록 조회 (교수용)

---

## 🔧 API 명세서

### 1. 교수: 강의별 학생 목록 조회

**엔드포인트**: `GET /api/lectures/{lecSerial}/students`

**목적**: 교수님이 담당 강의의 수강생 목록을 조회합니다.

**Path Parameter**:
- `lecSerial`: 강의 코드 (예: "CS101")

**Query Parameter**:
- `page`: 페이지 번호 (기본: 0)
- `size`: 페이지 크기 (기본: 20)

**Response (성공)**:
```json
{
  "content": [
    {
      "studentIdx": 100,
      "userEmail": "student1@univ.edu",
      "userName": "김학생",
      "enrollmentDate": "2025-03-01T00:00:00Z",
      "lecSerial": "CS101",
      "lecTit": "자료구조",
      "lecProf": "김교수",
      // 참고: lecYear, lecSemester는 LEC_SERIAL에서 파싱하거나 별도 테이블에서 관리
      // attendanceCount, totalSessions 등은 출결 테이블에서 조회
      "attendanceCount": 12,
      "totalSessions": 15,
      "attendanceRate": 80.0,
      "assignmentSubmitted": 8,
      "totalAssignments": 10,
      "submissionRate": 80.0,
      "currentGrade": "B+"
    }
  ],
  "totalElements": 25,
  "totalPages": 2,
  "size": 20,
  "number": 0
}
```

**프론트엔드 호출 예시**:
```javascript
const studentsResponse = await fetch(`/api/lectures/${lecSerial}/students?page=0&size=20`, {
  method: 'GET',
  headers: {
    'Authorization': `Bearer ${token}`
  }
});

const students = await studentsResponse.json();
// 학생 목록 표시 + 출결/과제 통계
```

---

### 2. 출결 관리: 출결 요청 (학생)

**엔드포인트**: `POST /api/attendance/request`

**목적**: 학생이 출석을 요청합니다.

**Request Body**:
```json
{
  "studentIdx": 100,                    // 학생 ID (필수)
  "lecSerial": "CS101",      // 강의 코드 (필수)
  "requestType": "ATTENDANCE",          // 요청 타입 (기본: ATTENDANCE)
  "notes": "지각 사유"                   // 비고 (선택)
}
```

**Response (성공)**:
```json
{
  "success": true,
  "message": "출결 요청이 접수되었습니다.",
  "data": {
    "requestIdx": 456,
    "studentIdx": 100,
    "lecIdx": 1,
    "requestType": "ATTENDANCE",
    "status": "PENDING",
    "requestDate": "2025-10-17T10:00:00Z"
  }
}
```

**프론트엔드 호출 예시**:
```javascript
const attendanceRequest = await fetch('/api/attendance/request', {
  method: 'POST',
  headers: {
    'Authorization': `Bearer ${token}`,
    'Content-Type': 'application/json'
  },
  body: JSON.stringify({
    studentIdx: currentUser.userIdx,
    lecSerial: currentLecture.lecSerial,
    notes: "교통 체증으로 지각"
  })
});
```

---

### 3. 출결 관리: 출결 승인 (교수)

**엔드포인트**: `PUT /api/attendance/{requestIdx}/approve`

**목적**: 교수님이 학생의 출결 요청을 승인/거부합니다.

**Path Parameter**:
- `requestIdx`: 출결 요청 ID

**Request Body**:
```json
{
  "approved": true,     // 승인 여부 (true: 승인, false: 거부)
  "notes": "승인 완료"   // 교수 비고 (선택)
}
```

**Response (성공)**:
```json
{
  "success": true,
  "message": "출결 요청이 승인되었습니다.",
  "data": {
    "requestIdx": 456,
    "studentIdx": 100,
    "lecIdx": 1,
    "status": "APPROVED",
    "approvedBy": 22,
    "approvedDate": "2025-10-17T10:30:00Z"
  }
}
```

**프론트엔드 호출 예시**:
```javascript
// 승인
const approveResponse = await fetch(`/api/attendance/${requestIdx}/approve`, {
  method: 'PUT',
  headers: {
    'Authorization': `Bearer ${token}`,
    'Content-Type': 'application/json'
  },
  body: JSON.stringify({
    approved: true,
    notes: "출석 인정"
  })
});

// 거부
const rejectResponse = await fetch(`/api/attendance/${requestIdx}/approve`, {
  method: 'PUT',
  headers: {
    'Authorization': `Bearer ${token}`,
    'Content-Type': 'application/json'
  },
  body: JSON.stringify({
    approved: false,
    notes: "사유 불충분"
  })
});
```

---

### 4. 과제 관리: 과제 목록 조회

**엔드포인트**: `POST /api/assignments/list`

**목적**: 강의별 과제 목록을 조회합니다.

**Request Body**:
```json
{
  "lecSerial": "CS101",  // 강의 코드 (필수)
  "page": 0,                        // 페이지 번호 (기본: 0)
  "size": 20                        // 페이지 크기 (기본: 20)
}
```

**Response (성공)**:
```json
{
  "content": [
    {
      "assignmentIdx": 1,
      "lecIdx": 1,
      "lecSerial": "CS101",
      "title": "자료구조 과제 1",
      "description": "연결 리스트 구현",
      "dueDate": "2025-03-15T23:59:00Z",
      "maxScore": 100,
      "createdDate": "2025-03-01T00:00:00Z",
      "submissionCount": 23,
      "totalStudents": 25
    }
  ],
  "totalElements": 5,
  "totalPages": 1
}
```

---

### 5. 과제 관리: 과제 생성 (교수)

**엔드포인트**: `POST /api/assignments`

**목적**: 교수님이 새로운 과제를 생성합니다.

**Request Body**:
```json
{
  "lecSerial": "CS101",      // 강의 코드 (필수)
  "title": "자료구조 과제 2",           // 과제 제목 (필수)
  "description": "트리 구조 구현",      // 과제 설명 (필수)
  "dueDate": "2025-03-22T23:59:00Z",   // 제출 마감일 (필수)
  "maxScore": 100                      // 만점 점수 (필수)
}
```

**Response (성공)**:
```json
{
  "success": true,
  "message": "과제가 생성되었습니다.",
  "data": {
    "assignmentIdx": 2,
    "lecIdx": 1,
    "title": "자료구조 과제 2",
    "description": "트리 구조 구현",
    "dueDate": "2025-03-22T23:59:00Z",
    "maxScore": 100,
    "createdBy": 22,
    "createdDate": "2025-10-17T11:00:00Z"
  }
}
```

---

### 6. 과제 관리: 과제 제출 (학생)

**엔드포인트**: `POST /api/assignments/{assignmentIdx}/submit`

**목적**: 학생이 과제를 제출합니다.

**Path Parameter**:
- `assignmentIdx`: 과제 ID

**Request Body**:
```json
{
  "studentIdx": 100,          // 학생 ID (필수)
  "content": "과제 내용...",   // 제출 내용 (필수)
  "fileUrl": "파일 URL"        // 첨부 파일 URL (선택)
}
```

**Response (성공)**:
```json
{
  "success": true,
  "message": "과제가 제출되었습니다.",
  "data": {
    "submissionIdx": 789,
    "assignmentIdx": 2,
    "studentIdx": 100,
    "content": "과제 내용...",
    "submittedDate": "2025-10-17T12:00:00Z",
    "status": "SUBMITTED"
  }
}
```

---

### 7. 과제 관리: 과제 채점 (교수)

**엔드포인트**: `PUT /api/assignments/{assignmentIdx}/grade`

**목적**: 교수님이 제출된 과제를 채점합니다.

**Path Parameter**:
- `assignmentIdx`: 과제 ID

**Request Body**:
```json
{
  "studentIdx": 100,      // 학생 ID (필수)
  "score": 95,            // 점수 (필수)
  "feedback": "잘 구현됨"  // 피드백 (선택)
}
```

**Response (성공)**:
```json
{
  "success": true,
  "message": "채점이 완료되었습니다.",
  "data": {
    "submissionIdx": 789,
    "assignmentIdx": 2,
    "studentIdx": 100,
    "score": 95,
    "feedback": "잘 구현됨",
    "gradedBy": 22,
    "gradedDate": "2025-10-17T14:00:00Z"
  }
}
```

---

## 🔄 플로우 다이어그램

### 교수 플로우
```
교수 로그인
    ↓
강의별 학생 목록 조회
    ↓
출결 요청 승인/거부
    ↓
과제 생성
    ↓
제출물 채점 및 피드백
```

### 학생 플로우
```
학생 로그인
    ↓
출결 요청
    ↓ (교수 승인 대기)
출결 승인 확인
    ↓
과제 제출
    ↓ (교수 채점 대기)
채점 결과 확인
```

## 📝 구현 가이드

### 프론트엔드 구현 포인트
1. **실시간 알림**: 출결 승인, 채점 완료 시 푸시 알림
2. **상태 관리**: PENDING → APPROVED/REJECTED 상태 전환
3. **마감일 관리**: dueDate 기반 UI 표시 (마감 임박, 마감됨 등)
4. **파일 업로드**: 과제 제출 시 파일 첨부 기능

### 출결 관리 UI/UX
```javascript
// 출결 상태 표시
const getAttendanceStatus = (status) => {
  switch(status) {
    case 'PENDING': return { text: '승인 대기', color: 'yellow' };
    case 'APPROVED': return { text: '출석', color: 'green' };
    case 'REJECTED': return { text: '결석', color: 'red' };
    default: return { text: '미정', color: 'gray' };
  }
};
```

### 과제 관리 고려사항
- **마감일 검증**: 제출 시 현재 시간과 dueDate 비교
- **재제출 허용**: 설정에 따라 다중 제출 가능
- **파일 크기 제한**: 프론트엔드에서 사전 검증
- **채점 상태**: 미채점/채점중/채점완료 구분

### 성능 및 사용자 경험
- **페이징 필수**: 학생 목록, 과제 목록 등 대량 데이터
- **캐싱 전략**: 자주 조회되는 데이터 캐시
- **오프라인 지원**: 제출 대기 중 오프라인 저장

---

**다음 단계**: [Phase4_Evaluation_Closure.md](../Phase4_Evaluation_Closure.md)에서 평가 및 종료 단계 API를 확인하세요.</content>
<parameter name="filePath">F:\main_project\works\blue-crab-lms\backend\BlueCrab\docs\강의관련기능\Phase3_Semester_Progress.md