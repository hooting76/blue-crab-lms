# 📝 수강 관리 및 성적 API 명세서

> **Base URL**: `/api/enrollments`
> 
> **작성일**: 2025-10-24
> 
> **컨트롤러**: `EnrollmentController.java`

---

## 📋 목차
1. [수강 목록 조회](#1-수강-목록-조회)
2. [수강 상세 조회](#2-수강-상세-조회)
3. [수강 데이터 조회](#3-수강-데이터-조회)
4. [수강 신청](#4-수강-신청)
5. [수강 취소](#5-수강-취소)
6. [출석 정보 업데이트](#6-출석-정보-업데이트)
7. [성적 업데이트](#7-성적-업데이트)
8. [성적 설정](#8-성적-설정)
9. [성적 조회](#9-성적-조회)
10. [성적 목록 조회](#10-성적-목록-조회)
11. [성적 확정](#11-성적-확정)

---

## 1. 수강 목록 조회

### `POST /api/enrollments/list`

수강 목록을 조회합니다. 강의별/학생별 필터, 페이징을 지원합니다.

#### Request Body
```json
{
  "lecSerial": "ETH201",        // [선택] 강의 코드로 필터
  "studentIdx": 6,              // [선택] 학생 IDX로 필터
  "action": "list",             // [선택] 액션 타입
  "page": 0,                    // [선택] 페이지 번호
  "size": 20                    // [선택] 페이지 크기
}
```

#### Response
```json
{
  "content": [
    {
      "enrollmentIdx": 3,
      "lecIdx": 48,
      "studentIdx": 6,
      "enrollmentData": "{...}",
      "enrollmentDate": "2025-10-14T11:50:07",
      "status": "ENROLLED"
    }
  ],
  "pageable": {...},
  "totalElements": 15,
  "totalPages": 1
}
```

---

## 2. 수강 상세 조회

### `POST /api/enrollments/detail`

특정 수강 정보의 상세를 조회합니다.

#### Request Body
```json
{
  "enrollmentIdx": 3,           // [선택] 수강 IDX
  "lecSerial": "ETH201",        // [선택] 강의 코드
  "studentIdx": 6,              // [선택] 학생 IDX
  "action": "detail"
}
```

#### Response
```json
{
  "enrollmentIdx": 3,
  "lecIdx": 48,
  "studentIdx": 6,
  "enrollmentData": "{\"grade\":{...},\"attendance\":{...}}",
  "enrollmentDate": "2025-10-14T11:50:07"
}
```

---

## 3. 수강 데이터 조회

### `POST /api/enrollments/data`

수강의 JSON 데이터를 파싱하여 반환합니다.

#### Request Body
```json
{
  "enrollmentIdx": 3,           // [필수] 수강 IDX
  "action": "data"
}
```

#### Response
```json
{
  "grade": {
    "total": {
      "maxScore": 110.0,
      "score": 28.63,
      "percentage": 26.03
    },
    "attendance": {...},
    "assignments": [...]
  },
  "attendance": {
    "summary": {...},
    "sessions": [...],
    "pendingRequests": [...]
  },
  "enrollment": {
    "status": "ENROLLED",
    "enrollmentDate": "2025-10-14 11:50:07"
  }
}
```

---

## 4. 수강 신청

### `POST /api/enrollments/enroll`

학생이 강의를 수강 신청합니다.

#### Request Body
```json
{
  "lecSerial": "ETH201",        // [필수] 강의 코드
  "studentIdx": 6,              // [필수] 학생 IDX
  "action": "enroll"
}
```

#### Response
```json
{
  "success": true,
  "message": "수강 신청이 완료되었습니다.",
  "data": {
    "enrollmentIdx": 19,
    "lecIdx": 48,
    "studentIdx": 6,
    "enrollmentData": "{...}"
  }
}
```

---

## 5. 수강 취소

### `DELETE /api/enrollments/{enrollmentIdx}`

수강을 취소합니다.

#### Request
- URL Parameter: `enrollmentIdx` (수강 IDX)

#### Response
```json
{
  "success": true,
  "message": "수강이 취소되었습니다."
}
```

---

## 6. 출석 정보 업데이트

### `PUT /api/enrollments/{enrollmentIdx}/attendance`

수강의 출석 정보를 업데이트합니다 (교수 전용).

#### Request Body
```json
{
  "sessionNumber": 2,           // [필수] 회차 번호
  "status": "출",               // [필수] 출석 상태 (출/지/결)
  "requestDate": "2025-10-24 11:13:54",
  "approvedDate": "2025-10-24 11:36:50",
  "approvedBy": 25
}
```

#### Response
```json
{
  "success": true,
  "message": "출석 정보가 업데이트되었습니다.",
  "data": {
    "enrollmentIdx": 3,
    "enrollmentData": "{...}"
  }
}
```

---

## 7. 성적 업데이트

### `PUT /api/enrollments/{enrollmentIdx}/grade`

수강의 성적 정보를 직접 업데이트합니다 (교수 전용).

#### Request Body
```json
{
  "attendanceScore": 9.63,      // [선택] 출석 점수
  "assignmentScore": 19.0,      // [선택] 과제 점수
  "examScore": 0.0,             // [선택] 시험 점수
  "totalScore": 28.63           // [선택] 총점
}
```

#### Response
```json
{
  "success": true,
  "message": "성적이 업데이트되었습니다.",
  "data": {
    "enrollmentIdx": 3,
    "enrollmentData": "{...}"
  }
}
```

---

## 8. 성적 설정

### `POST /api/enrollments/grade-config`

강의의 성적 설정을 저장합니다 (교수 전용).

#### Request Body
```json
{
  "lecSerial": "ETH201",        // [필수] 강의 코드
  "attendanceMaxScore": 77,     // [필수] 출석 만점
  "assignmentTotalScore": 50,   // [필수] 과제 총점
  "examTotalScore": 30,         // [필수] 시험 총점
  "latePenaltyPerSession": 0.6, // [필수] 지각 벌점
  "gradeDistribution": {        // [필수] 성적 분포
    "A": 30,
    "B": 40,
    "C": 20,
    "D": 10
  }
}
```

#### Response
```json
{
  "success": true,
  "message": "성적 설정이 완료되었습니다.",
  "data": {
    "lecIdx": 48,
    "totalMaxScore": 157,
    "configuredAt": "2025-10-24 11:07:11"
  }
}
```

---

## 9. 성적 조회

### `POST /api/enrollments/grade-info`

특정 학생의 성적을 조회합니다.

#### Request Body
```json
{
  "action": "get-grade",        // [필수] 액션 타입
  "lecSerial": "ETH201",        // [필수] 강의 코드
  "studentIdx": 6               // [필수] 학생 IDX
}
```

#### Response
```json
{
  "success": true,
  "message": "성적 조회가 완료되었습니다.",
  "data": {
    "lecIdx": 48,
    "studentIdx": 6,
    "enrollmentIdx": 3,
    "grade": {
      "total": {
        "maxScore": 110.0,
        "score": 28.63,
        "percentage": 26.03
      },
      "attendance": {
        "currentScore": 9.63,
        "maxScore": 77.0,
        "percentage": 12.51,
        "presentCount": 10,
        "lateCount": 0,
        "latePenalty": 0.0,
        "absentCount": 0,
        "attendanceRate": 10
      },
      "assignments": [
        {
          "name": "총장실을 박살내자.",
          "score": 10.0,
          "maxScore": 23.0,
          "percentage": 43.48,
          "submitted": true
        },
        {
          "name": "실험실을 박살내기",
          "score": 9.0,
          "maxScore": 10.0,
          "percentage": 90.0,
          "submitted": true
        }
      ]
    },
    "updatedAt": "2025-10-24T18:02:32.773645717"
  }
}
```

---

## 10. 성적 목록 조회

### `POST /api/enrollments/grade-list`

강의의 전체 학생 성적 목록을 조회합니다 (교수 전용).

#### Request Body
```json
{
  "action": "get-grade-list",   // [필수] 액션 타입
  "lecSerial": "ETH201"         // [필수] 강의 코드
}
```

#### Response
```json
{
  "success": true,
  "message": "성적 목록 조회가 완료되었습니다.",
  "data": [
    {
      "studentIdx": 6,
      "studentId": "20241234",
      "studentName": "김철수",
      "totalScore": 28.63,
      "maxScore": 110.0,
      "percentage": 26.03,
      "grade": "F",
      "attendanceScore": 9.63,
      "assignmentScore": 19.0,
      "examScore": 0.0
    }
  ]
}
```

---

## 11. 성적 확정

### `POST /api/enrollments/grade-finalize`

강의의 모든 학생 성적을 확정합니다 (교수 전용).

#### Request Body
```json
{
  "action": "finalize-grades",  // [필수] 액션 타입
  "lecSerial": "ETH201"         // [필수] 강의 코드
}
```

#### Response
```json
{
  "success": true,
  "message": "성적 확정이 완료되었습니다.",
  "data": {
    "totalStudents": 15,
    "gradeDistribution": {
      "A": 4,
      "B": 6,
      "C": 3,
      "D": 2,
      "F": 0
    },
    "finalizedAt": "2025-10-24T18:30:00"
  }
}
```

---

## 🔧 공통 에러 응답

### 400 Bad Request
```json
{
  "success": false,
  "message": "lecSerial과 studentIdx는 필수입니다."
}
```

### 404 Not Found
```json
{
  "success": false,
  "message": "존재하지 않는 수강 정보입니다: 999"
}
```

### 500 Internal Server Error
```json
{
  "success": false,
  "message": "성적 조회 중 오류가 발생했습니다."
}
```

---

## 📊 주요 비즈니스 로직

### 1. 성적 자동 재계산
- 출석 승인 시 → `GradeUpdateEvent` 발행 → 출석 점수 재계산
- 과제 채점 시 → `GradeUpdateEvent` 발행 → 과제 점수 재계산
- `@Transactional(propagation = REQUIRES_NEW)` 보장

### 2. 성적 구성
```javascript
총점 = 출석 점수 + 과제 점수 + 시험 점수

출석 점수 = (출석 횟수 - 지각 벌점) * (출석 만점 / 총 회차)
과제 점수 = Σ(각 과제 점수)
시험 점수 = 중간고사 + 기말고사
```

### 3. 등급 산출
- 성적 확정 시 상대평가 적용
- 성적 분포: A(30%), B(40%), C(20%), D(10%)

---

## 🧪 통합 테스트 스크립트

### 출석 → 성적 확인 (전체 플로우)

```javascript
// 1. 로그인 (교수)
await loginProfessor();

// 2. 초기 성적 확인
const initialGrade = await apiCall('/api/enrollments/grade-info', 'POST', {
    action: 'get-grade',
    lecSerial: 'ETH201',
    studentIdx: 6
});
console.log('초기 총점:', initialGrade.data.data.grade.total.score);

// 3. 출석 승인 (교수)
await apiCall('/api/attendance/approve', 'POST', {
    lecSerial: 'ETH201',
    sessionNumber: 2,
    attendanceRecords: [{
        studentIdx: 6,
        status: '출'
    }]
});

// 4. 3초 대기 (비동기 처리)
await sleep(3000);

// 5. 업데이트된 성적 확인
const updatedGrade = await apiCall('/api/enrollments/grade-info', 'POST', {
    action: 'get-grade',
    lecSerial: 'ETH201',
    studentIdx: 6
});
console.log('업데이트된 총점:', updatedGrade.data.data.grade.total.score);
```

---

## 📌 참고 사항

1. **인증 필요**: 모든 API는 `Authorization: Bearer {token}` 헤더 필요
2. **권한 구분**:
   - 학생: 본인 성적 조회, 수강 신청/취소
   - 교수: 모든 기능 (성적 설정, 확정, 목록 조회)
3. **JSON 필드**: `enrollmentData`는 JSON 문자열로 저장됨 (압축 형식)
4. **비동기 처리**: 성적 재계산은 이벤트 기반 비동기 처리 (3초 대기 권장)

---

**작성자**: GitHub Copilot  
**최종 수정**: 2025-10-24
