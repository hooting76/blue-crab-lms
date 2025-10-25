# 📝 출석 관리 교수용 API 명세서

> **Base URL**: `/api/professor/attendance`
> 
> **작성일**: 2025-10-24
> 
> **컨트롤러**: `ProfessorAttendanceController.java`

---

## 📋 목차
1. [출석 인정 요청 목록 조회](#1-출석-인정-요청-목록-조회)
2. [대기 중인 요청 개수 조회](#2-대기-중인-요청-개수-조회)
3. [출석 인정 요청 승인](#3-출석-인정-요청-승인)
4. [출석 인정 요청 반려](#4-출석-인정-요청-반려)
5. [출석 체크](#5-출석-체크)

---

## 1. 출석 인정 요청 목록 조회

### `POST /api/professor/attendance/requests`

교수가 강의별 출석 인정 요청 목록을 조회합니다. 페이징과 상태 필터링을 지원합니다.

#### Request Body
```json
{
  "lecIdx": 1,              // [필수] 강의 ID
  "page": 0,                // [선택] 페이지 번호 (기본값: 0)
  "size": 20,               // [선택] 페이지 크기 (기본값: 20)
  "status": "PENDING"       // [선택] 상태 필터 ("PENDING" 또는 생략 시 전체)
}
```

#### Response (성공)
```json
{
  "success": true,
  "message": "출석 인정 요청 목록 조회 성공",
  "data": {
    "content": [
      {
        "requestIdx": 5,
        "enrollmentIdx": 1,
        "sessionNumber": 3,
        "requestReason": "병원 진료로 인한 결석",
        "status": "PENDING",
        "requestDate": "2025-10-24T10:30:00",
        "approvedDate": null,
        "approvedBy": null,
        "rejectReason": null,
        "studentName": "김학생",
        "studentId": "20241001"
      },
      {
        "requestIdx": 6,
        "enrollmentIdx": 2,
        "sessionNumber": 5,
        "requestReason": "가족 행사 참석",
        "status": "PENDING",
        "requestDate": "2025-10-24T11:45:00",
        "approvedDate": null,
        "approvedBy": null,
        "rejectReason": null,
        "studentName": "이학생",
        "studentId": "20241002"
      }
    ],
    "pageable": {
      "sort": {
        "sorted": false,
        "empty": true
      },
      "pageNumber": 0,
      "pageSize": 20
    },
    "totalElements": 2,
    "totalPages": 1,
    "last": true,
    "numberOfElements": 2,
    "first": true,
    "empty": false
  }
}
```

#### Response (실패)
```json
{
  "success": false,
  "message": "lecIdx는 필수입니다.",
  "data": null
}
```

#### HTTP Status Codes
- `200 OK`: 조회 성공
- `400 Bad Request`: 파라미터 오류

#### 상태 필터
- **"PENDING"**: 대기 중인 요청만 조회
- **생략 또는 다른 값**: 모든 상태 요청 조회

#### 테스트 예제 (JavaScript)
```javascript
// 출석 인정 요청 목록 조회
function getAttendanceRequests(lecIdx, page = 0, size = 20, status = null) {
    const body = {
        lecIdx: lecIdx,
        page: page,
        size: size
    };
    
    if (status) {
        body.status = status;
    }
    
    return fetch(`${API_BASE_URL}/professor/attendance/requests`, {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json',
            'Authorization': `Bearer ${token}`
        },
        body: JSON.stringify(body)
    });
}

// 사용 예시 - 대기 중인 요청만 조회
getAttendanceRequests(1, 0, 20, "PENDING")
    .then(response => response.json())
    .then(data => {
        if (data.success) {
            console.log(`대기 중인 요청: ${data.data.totalElements}건`);
            data.data.content.forEach(req => {
                console.log(`${req.studentName} - ${req.sessionNumber}회차: ${req.requestReason}`);
            });
        }
    });
```

---

## 2. 대기 중인 요청 개수 조회

### `POST /api/professor/attendance/requests/count`

교수가 특정 강의의 대기 중인 출석 인정 요청 개수를 조회합니다.

#### Request Body
```json
{
  "lecIdx": 1               // [필수] 강의 ID
}
```

#### Response (성공)
```json
{
  "success": true,
  "message": "대기 중인 요청 개수 조회 성공",
  "data": {
    "pendingCount": 5
  }
}
```

#### Response (실패)
```json
{
  "success": false,
  "message": "lecIdx는 필수입니다.",
  "data": null
}
```

#### HTTP Status Codes
- `200 OK`: 조회 성공
- `400 Bad Request`: 파라미터 오류

#### 테스트 예제 (JavaScript)
```javascript
// 대기 중인 요청 개수 조회
function getPendingRequestCount(lecIdx) {
    return fetch(`${API_BASE_URL}/professor/attendance/requests/count`, {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json',
            'Authorization': `Bearer ${token}`
        },
        body: JSON.stringify({
            lecIdx: lecIdx
        })
    });
}

// 사용 예시
getPendingRequestCount(1)
    .then(response => response.json())
    .then(data => {
        if (data.success) {
            console.log(`처리 대기 중인 요청: ${data.data.pendingCount}건`);
        }
    });
```

---

## 3. 출석 인정 요청 승인

### `PUT /api/professor/attendance/requests/{requestIdx}/approve`

교수가 학생의 출석 인정 요청을 승인합니다. 승인 시 해당 회차의 출석 상태가 "결" → "출"로 변경됩니다.

#### URL Parameters
- `requestIdx`: 요청 ID (Long)

#### Request Body
```json
{
  "professorIdx": 1         // [필수] 교수 ID
}
```

#### Response (성공)
```json
{
  "success": true,
  "message": "출석 인정 승인 완료",
  "data": {
    "requestIdx": 5,
    "enrollmentIdx": 1,
    "sessionNumber": 3,
    "requestReason": "병원 진료로 인한 결석",
    "status": "APPROVED",
    "requestDate": "2025-10-24T10:30:00",
    "approvedDate": "2025-10-24T14:15:00",
    "approvedBy": 1,
    "rejectReason": null
  }
}
```

#### Response (실패)
```json
{
  "success": false,
  "message": "professorIdx는 필수입니다.",
  "data": null
}
```

#### HTTP Status Codes
- `200 OK`: 승인 성공
- `400 Bad Request`: 파라미터 오류
- `404 Not Found`: 요청을 찾을 수 없음

#### 비즈니스 로직
1. 요청 상태를 "PENDING" → "APPROVED"로 변경
2. 승인 일시와 승인자 정보 기록
3. **해당 회차의 출석 상태를 "결" → "출"로 자동 변경**
4. **GradeUpdateEvent 발생으로 성적 자동 재계산**

#### 테스트 예제 (JavaScript)
```javascript
// 출석 인정 요청 승인
function approveAttendanceRequest(requestIdx, professorIdx) {
    return fetch(`${API_BASE_URL}/professor/attendance/requests/${requestIdx}/approve`, {
        method: 'PUT',
        headers: {
            'Content-Type': 'application/json',
            'Authorization': `Bearer ${token}`
        },
        body: JSON.stringify({
            professorIdx: professorIdx
        })
    });
}

// 사용 예시
approveAttendanceRequest(5, 1)
    .then(response => response.json())
    .then(data => {
        if (data.success) {
            console.log(`승인 완료: ${data.data.sessionNumber}회차 출석 인정`);
        } else {
            console.error('승인 실패:', data.message);
        }
    });
```

---

## 4. 출석 인정 요청 반려

### `PUT /api/professor/attendance/requests/{requestIdx}/reject`

교수가 학생의 출석 인정 요청을 반려합니다. 반려 사유를 함께 기록합니다.

#### URL Parameters
- `requestIdx`: 요청 ID (Long)

#### Request Body
```json
{
  "professorIdx": 1,                // [필수] 교수 ID
  "rejectReason": "사유 불충분"     // [필수] 반려 사유
}
```

#### Response (성공)
```json
{
  "success": true,
  "message": "출석 인정 요청 반려 완료",
  "data": {
    "requestIdx": 7,
    "enrollmentIdx": 1,
    "sessionNumber": 10,
    "requestReason": "개인 사정",
    "status": "REJECTED",
    "requestDate": "2025-10-24T12:00:00",
    "approvedDate": "2025-10-24T15:30:00",
    "approvedBy": 1,
    "rejectReason": "사유 불충분"
  }
}
```

#### Response (실패)
```json
{
  "success": false,
  "message": "professorIdx와 rejectReason은 필수입니다.",
  "data": null
}
```

#### HTTP Status Codes
- `200 OK`: 반려 성공
- `400 Bad Request`: 파라미터 오류
- `404 Not Found`: 요청을 찾을 수 없음

#### 비즈니스 로직
1. 요청 상태를 "PENDING" → "REJECTED"로 변경
2. 반려 일시와 반려자 정보 기록
3. 반려 사유 저장
4. **출석 상태는 변경되지 않음** (기존 "결" 상태 유지)

#### 테스트 예제 (JavaScript)
```javascript
// 출석 인정 요청 반려
function rejectAttendanceRequest(requestIdx, professorIdx, rejectReason) {
    return fetch(`${API_BASE_URL}/professor/attendance/requests/${requestIdx}/reject`, {
        method: 'PUT',
        headers: {
            'Content-Type': 'application/json',
            'Authorization': `Bearer ${token}`
        },
        body: JSON.stringify({
            professorIdx: professorIdx,
            rejectReason: rejectReason
        })
    });
}

// 사용 예시
rejectAttendanceRequest(7, 1, "증빙 서류 미제출")
    .then(response => response.json())
    .then(data => {
        if (data.success) {
            console.log(`반려 완료: ${data.data.rejectReason}`);
        } else {
            console.error('반려 실패:', data.message);
        }
    });
```

---

## 5. 출석 체크

### `POST /api/professor/attendance/mark`

교수가 특정 학생의 특정 회차 출석을 직접 기록합니다.

#### Request Body
```json
{
  "enrollmentIdx": 1,       // [필수] 수강신청 ID
  "sessionNumber": 3,       // [필수] 회차 번호 (1-80)
  "status": "출"           // [필수] 출석 상태 ("출", "결", "지")
}
```

#### Response (성공)
```json
{
  "success": true,
  "message": "출석 체크 완료",
  "data": {
    "sessionNumber": 3,
    "status": "출"
  }
}
```

#### Response (실패)
```json
{
  "success": false,
  "message": "status는 '출', '결', '지' 중 하나여야 합니다.",
  "data": null
}
```

#### HTTP Status Codes
- `200 OK`: 기록 성공
- `400 Bad Request`: 파라미터 오류

#### 출석 상태 값
- **"출"**: 출석
- **"결"**: 결석
- **"지"**: 지각

#### 비즈니스 로직
1. 해당 학생의 enrollmentData에서 attendance 문자열 조회
2. 지정된 회차의 출석 상태 업데이트
3. **출석률 자동 재계산**
4. **GradeUpdateEvent 발생으로 성적 자동 재계산**

#### 테스트 예제 (JavaScript)
```javascript
// 출석 체크
function markAttendance(enrollmentIdx, sessionNumber, status) {
    return fetch(`${API_BASE_URL}/professor/attendance/mark`, {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json',
            'Authorization': `Bearer ${token}`
        },
        body: JSON.stringify({
            enrollmentIdx: enrollmentIdx,
            sessionNumber: sessionNumber,
            status: status
        })
    });
}

// 사용 예시 - 일괄 출석 체크
const attendanceData = [
    { enrollmentIdx: 1, sessionNumber: 5, status: "출" },
    { enrollmentIdx: 2, sessionNumber: 5, status: "결" },
    { enrollmentIdx: 3, sessionNumber: 5, status: "지" }
];

Promise.all(attendanceData.map(data => 
    markAttendance(data.enrollmentIdx, data.sessionNumber, data.status)
)).then(responses => {
    console.log('일괄 출석 체크 완료');
});
```

---

## 💡 중요 참고사항

### 권한 체크
- 모든 교수용 API는 교수 권한(userStudent = 1) 필요
- JWT 토큰에서 사용자 정보 추출하여 권한 확인

### 출석 상태 변경 흐름
```
1. 학생 출석 인정 신청 (PENDING)
   ↓
2. 교수 승인/반려 결정
   ↓
3a. 승인 시: "결" → "출" 변경 + 성적 재계산
3b. 반려 시: 출석 상태 유지
```

### 자동 이벤트 처리
- **출석 상태 변경** → GradeUpdateEvent 발생
- **성적 자동 재계산** → 출석 점수 반영
- **실시간 업데이트** → 학생이 즉시 확인 가능

### 데이터 무결성
- 회차 번호는 1-80 범위 내에서만 허용
- 출석 상태는 "출", "결", "지"만 허용
- 중복 요청 방지 로직 적용

### 페이징 지원
- 요청 목록 조회 시 Page 객체 반환
- totalElements, totalPages 등 페이징 정보 포함
- 대용량 데이터 처리 최적화

### 비즈니스 연계
- 출석 변경 시 **성적 관리 시스템**과 자동 연동
- **과제 관리 시스템**과 별도 운영
- **수강 관리 시스템**의 enrollmentData 활용