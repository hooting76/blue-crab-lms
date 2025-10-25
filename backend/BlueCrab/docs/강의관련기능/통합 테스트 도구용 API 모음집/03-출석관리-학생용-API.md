# 📝 출석 관리 학생용 API 명세서

> **Base URL**: `/api/student/attendance`
> 
> **작성일**: 2025-10-24
> 
> **컨트롤러**: `StudentAttendanceController.java`

---

## 📋 목차
1. [내 출석 조회](#1-내-출석-조회)
2. [출석 인정 신청](#2-출석-인정-신청)
3. [내 신청 목록 조회](#3-내-신청-목록-조회)

---

## 1. 내 출석 조회

### `POST /api/student/attendance/detail`

학생이 자신의 출석 현황을 조회합니다. 출석 문자열, 출석률, 상세 정보를 제공합니다.

#### Request Body
```json
{
  "enrollmentIdx": 1        // [필수] 수강신청 ID
}
```

#### Response (성공)
```json
{
  "success": true,
  "message": "출석 조회 성공",
  "data": {
    "attendanceStr": "1출2출3결4지5출6출7결8출",
    "attendanceRate": "6/8",
    "details": [
      {
        "sessionNumber": 1,
        "status": "출"
      },
      {
        "sessionNumber": 2,
        "status": "출"
      },
      {
        "sessionNumber": 3,
        "status": "결"
      },
      {
        "sessionNumber": 4,
        "status": "지"
      }
    ]
  }
}
```

#### Response (실패)
```json
{
  "success": false,
  "message": "enrollmentIdx는 필수입니다.",
  "data": null
}
```

#### HTTP Status Codes
- `200 OK`: 조회 성공
- `400 Bad Request`: 파라미터 오류

#### 테스트 예제 (JavaScript)
```javascript
// 내 출석 조회
function getMyAttendance(enrollmentIdx) {
    return fetch(`${API_BASE_URL}/student/attendance/detail`, {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json',
            'Authorization': `Bearer ${token}`
        },
        body: JSON.stringify({
            enrollmentIdx: enrollmentIdx
        })
    });
}

// 사용 예시
getMyAttendance(1)
    .then(response => response.json())
    .then(data => console.log('출석 현황:', data));
```

---

## 2. 출석 인정 신청

### `POST /api/student/attendance/request`

학생이 결석/지각에 대한 출석 인정을 신청합니다.

#### Request Body
```json
{
  "enrollmentIdx": 1,                    // [필수] 수강신청 ID
  "sessionNumber": 3,                    // [필수] 회차 번호 (1-80)
  "requestReason": "병원 진료로 인한 결석"  // [필수] 신청 사유
}
```

#### Response (성공)
```json
{
  "success": true,
  "message": "출석 인정 신청 완료",
  "data": {
    "requestIdx": 5,
    "enrollmentIdx": 1,
    "sessionNumber": 3,
    "requestReason": "병원 진료로 인한 결석",
    "status": "PENDING",
    "requestDate": "2025-10-24T10:30:00",
    "approvedDate": null,
    "approvedBy": null,
    "rejectReason": null
  }
}
```

#### Response (실패)
```json
{
  "success": false,
  "message": "sessionNumber는 1~80 사이여야 합니다.",
  "data": null
}
```

#### HTTP Status Codes
- `200 OK`: 신청 성공
- `400 Bad Request`: 파라미터 오류

#### 중요 사항
- **회차 범위**: sessionNumber는 1~80 사이여야 함
- **중복 신청**: 같은 회차에 대해 중복 신청 불가
- **신청 상태**: 기본값은 "PENDING" (대기 중)

#### 테스트 예제 (JavaScript)
```javascript
// 출석 인정 신청
function requestAttendanceExcuse(enrollmentIdx, sessionNumber, reason) {
    return fetch(`${API_BASE_URL}/student/attendance/request`, {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json',
            'Authorization': `Bearer ${token}`
        },
        body: JSON.stringify({
            enrollmentIdx: enrollmentIdx,
            sessionNumber: sessionNumber,
            requestReason: reason
        })
    });
}

// 사용 예시
requestAttendanceExcuse(1, 3, "병원 진료로 인한 결석")
    .then(response => response.json())
    .then(data => {
        if (data.success) {
            console.log('신청 완료:', data.data.requestIdx);
        } else {
            console.error('신청 실패:', data.message);
        }
    });
```

---

## 3. 내 신청 목록 조회

### `POST /api/student/attendance/requests`

학생이 자신이 제출한 출석 인정 신청 목록을 조회합니다.

#### Request Body
```json
{
  "enrollmentIdx": 1        // [필수] 수강신청 ID
}
```

#### Response (성공)
```json
{
  "success": true,
  "message": "출석 인정 신청 목록 조회 성공",
  "data": [
    {
      "requestIdx": 5,
      "enrollmentIdx": 1,
      "sessionNumber": 3,
      "requestReason": "병원 진료로 인한 결석",
      "status": "APPROVED",
      "requestDate": "2025-10-24T10:30:00",
      "approvedDate": "2025-10-24T14:15:00",
      "approvedBy": 2,
      "rejectReason": null
    },
    {
      "requestIdx": 6,
      "enrollmentIdx": 1,
      "sessionNumber": 7,
      "requestReason": "교통사고로 인한 지각",
      "status": "PENDING",
      "requestDate": "2025-10-24T11:45:00",
      "approvedDate": null,
      "approvedBy": null,
      "rejectReason": null
    },
    {
      "requestIdx": 7,
      "enrollmentIdx": 1,
      "sessionNumber": 10,
      "requestReason": "개인 사정",
      "status": "REJECTED",
      "requestDate": "2025-10-24T12:00:00",
      "approvedDate": "2025-10-24T15:30:00",
      "approvedBy": 2,
      "rejectReason": "사유 불충분"
    }
  ]
}
```

#### Response (실패)
```json
{
  "success": false,
  "message": "enrollmentIdx는 필수입니다.",
  "data": null
}
```

#### HTTP Status Codes
- `200 OK`: 조회 성공
- `400 Bad Request`: 파라미터 오류

#### 신청 상태 값
- **PENDING**: 승인 대기 중
- **APPROVED**: 승인됨 (출석 처리)
- **REJECTED**: 반려됨

#### 테스트 예제 (JavaScript)
```javascript
// 내 신청 목록 조회
function getMyRequests(enrollmentIdx) {
    return fetch(`${API_BASE_URL}/student/attendance/requests`, {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json',
            'Authorization': `Bearer ${token}`
        },
        body: JSON.stringify({
            enrollmentIdx: enrollmentIdx
        })
    });
}

// 사용 예시
getMyRequests(1)
    .then(response => response.json())
    .then(data => {
        if (data.success) {
            data.data.forEach(request => {
                console.log(`${request.sessionNumber}회차: ${request.status} - ${request.requestReason}`);
            });
        }
    });
```

---

## 💡 중요 참고사항

### 출석 상태 코드
- **출**: 출석
- **결**: 결석  
- **지**: 지각

### 출석률 계산
- 출석률은 "출석 횟수/전체 회차" 형태로 표시
- 지각은 출석으로 계산되지만 별도 표시
- 출석 인정 승인 시 "결" → "출"로 변경

### 비즈니스 로직
1. **출석 조회**: enrollmentData의 attendance 필드 파싱
2. **신청 제출**: PENDING 상태로 요청 생성
3. **중복 방지**: 같은 회차에 대한 중복 신청 불가
4. **자동 업데이트**: 승인 시 출석 문자열 자동 업데이트

### 데이터 구조
```json
{
  "enrollmentData": {
    "attendance": "1출2출3결4지5출",
    "attendanceRate": "4/5",
    // ... 기타 수강 데이터
  }
}
```