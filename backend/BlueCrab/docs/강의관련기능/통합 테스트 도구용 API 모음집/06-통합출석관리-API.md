# 📝 통합 출석 관리 API 명세서

> **Base URL**: `/api/attendance`
> 
> **작성일**: 2025-10-25
> 
> **컨트롤러**: `AttendanceController.java`

---

## 📋 목차
1. [출석 요청 (학생용)](#1-출석-요청-학생용)
2. [출석 승인 (교수용)](#2-출석-승인-교수용)
3. [학생 출석 현황 조회](#3-학생-출석-현황-조회)
4. [교수 출석 현황 조회](#4-교수-출석-현황-조회)

---

## 1. 출석 요청 (학생용)

### `POST /api/attendance/request`

학생이 특정 회차에 대한 출석 요청을 제출합니다. JWT 토큰에서 학생 정보를 자동으로 추출하여 처리합니다.

#### Request Body
```json
{
  "lecSerial": "ETH201",            // [필수] 강의 코드
  "sessionNumber": 3,               // [필수] 회차 번호 (1-80)
  "requestReason": "병원 진료로 인한 결석"  // [필수] 요청 사유
}
```

#### Response (성공)
```json
{
  "success": true,
  "message": "출석 요청이 성공적으로 제출되었습니다.",
  "data": {
    "summary": {
      "totalSessions": 16,
      "attendedSessions": 12,
      "absentSessions": 3,
      "lateSessions": 1,
      "attendanceRate": 81.25
    },
    "sessions": [
      {
        "sessionNumber": 1,
        "status": "출석",
        "date": "2025-03-03"
      },
      {
        "sessionNumber": 2,
        "status": "출석",
        "date": "2025-03-05"
      },
      {
        "sessionNumber": 3,
        "status": "결석",
        "date": "2025-03-10",
        "hasRequest": true
      }
    ],
    "pendingRequests": [
      {
        "sessionNumber": 3,
        "requestReason": "병원 진료로 인한 결석",
        "requestDate": "2025-10-25T10:30:00",
        "status": "PENDING"
      }
    ]
  }
}
```

#### Response (실패)
```json
{
  "success": false,
  "message": "인증이 필요합니다.",
  "data": null
}
```

#### HTTP Status Codes
- `200 OK`: 요청 성공
- `400 Bad Request`: 파라미터 오류
- `401 Unauthorized`: 인증 실패
- `500 Internal Server Error`: 서버 오류

#### 비즈니스 로직
1. JWT 토큰에서 사용자 정보 추출
2. lecSerial로 강의 정보 조회 및 수강 자격 확인
3. 출석 요청 생성 (PENDING 상태)
4. 현재 출석 데이터와 함께 응답

#### 테스트 예제 (JavaScript)
```javascript
// 출석 요청 제출
function requestAttendance(lecSerial, sessionNumber, reason) {
    return fetch(`${API_BASE_URL}/attendance/request`, {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json',
            'Authorization': `Bearer ${token}`
        },
        body: JSON.stringify({
            lecSerial: lecSerial,
            sessionNumber: sessionNumber,
            requestReason: reason
        })
    });
}

// 사용 예시
requestAttendance("ETH201", 3, "병원 진료로 인한 결석")
    .then(response => response.json())
    .then(data => {
        if (data.success) {
            console.log('출석 요청 완료');
            console.log(`현재 출석률: ${data.data.summary.attendanceRate}%`);
        } else {
            console.error('요청 실패:', data.message);
        }
    });
```

---

## 2. 출석 승인 (교수용)

### `POST /api/attendance/approve`

교수가 학생들의 출석 요청을 승인하거나 반려합니다. 여러 학생의 출석을 한 번에 처리할 수 있습니다.

#### Request Body
```json
{
  "lecSerial": "ETH201",           // [필수] 강의 코드
  "sessionNumber": 3,              // [필수] 회차 번호
  "attendanceRecords": [           // [필수] 승인/반려 기록 배열
    {
      "studentIdx": 1,
      "status": "APPROVED",
      "rejectReason": null
    },
    {
      "studentIdx": 2,
      "status": "REJECTED",
      "rejectReason": "증빙 서류 미제출"
    }
  ]
}
```

#### Response (성공)
```json
{
  "success": true,
  "message": "출석 승인 처리가 완료되었습니다.",
  "data": null
}
```

#### Response (실패)
```json
{
  "success": false,
  "message": "교수 권한이 필요합니다.",
  "data": null
}
```

#### HTTP Status Codes
- `200 OK`: 승인 성공
- `400 Bad Request`: 파라미터 오류
- `401 Unauthorized`: 인증 실패
- `403 Forbidden`: 교수 권한 없음
- `500 Internal Server Error`: 서버 오류

#### 승인 상태 값
- **APPROVED**: 승인 (결석 → 출석으로 변경)
- **REJECTED**: 반려 (결석 상태 유지)

#### 비즈니스 로직
1. JWT 토큰에서 교수 정보 추출 및 권한 확인 (userStudent = 1)
2. 각 학생별 승인/반려 처리
3. 승인 시 출석 상태 자동 업데이트
4. **GradeUpdateEvent 발생으로 성적 자동 재계산**

#### 테스트 예제 (JavaScript)
```javascript
// 출석 승인 처리
function approveAttendance(lecSerial, sessionNumber, approvalRecords) {
    return fetch(`${API_BASE_URL}/attendance/approve`, {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json',
            'Authorization': `Bearer ${token}`
        },
        body: JSON.stringify({
            lecSerial: lecSerial,
            sessionNumber: sessionNumber,
            attendanceRecords: approvalRecords
        })
    });
}

// 사용 예시 - 일괄 승인/반려
const approvalData = [
    { studentIdx: 1, status: "APPROVED", rejectReason: null },
    { studentIdx: 2, status: "REJECTED", rejectReason: "증빙 서류 미제출" }
];

approveAttendance("ETH201", 3, approvalData)
    .then(response => response.json())
    .then(data => {
        if (data.success) {
            console.log('승인 처리 완료');
        }
    });
```

---

## 3. 학생 출석 현황 조회

### `POST /api/attendance/student/view`

학생이 자신의 출석 현황을 조회합니다. 출석 통계, 세션별 기록, 대기 중인 요청을 모두 포함합니다.

#### Request Body
```json
{
  "lecSerial": "ETH201"            // [필수] 강의 코드
}
```

#### Response (성공)
```json
{
  "success": true,
  "message": "출석 현황 조회가 완료되었습니다.",
  "data": {
    "summary": {
      "totalSessions": 16,
      "attendedSessions": 13,
      "absentSessions": 2,
      "lateSession": 1,
      "attendanceRate": 87.5,
      "requiredAttendanceRate": 75.0
    },
    "sessions": [
      {
        "sessionNumber": 1,
        "status": "출석",
        "date": "2025-03-03",
        "hasRequest": false
      },
      {
        "sessionNumber": 2,
        "status": "출석",
        "date": "2025-03-05",
        "hasRequest": false
      },
      {
        "sessionNumber": 3,
        "status": "결석",
        "date": "2025-03-10",
        "hasRequest": true
      },
      {
        "sessionNumber": 4,
        "status": "지각",
        "date": "2025-03-12",
        "hasRequest": false
      }
    ],
    "pendingRequests": [
      {
        "sessionNumber": 3,
        "requestReason": "병원 진료로 인한 결석",
        "requestDate": "2025-10-25T10:30:00",
        "status": "PENDING"
      }
    ]
  }
}
```

#### Response (실패)
```json
{
  "success": false,
  "message": "강의 코드(lecSerial)는 필수입니다.",
  "data": null
}
```

#### HTTP Status Codes
- `200 OK`: 조회 성공
- `400 Bad Request`: 파라미터 오류
- `401 Unauthorized`: 인증 실패
- `500 Internal Server Error`: 서버 오류

#### 출석 상태 값
- **출석**: 정상 출석
- **결석**: 결석
- **지각**: 지각

#### 테스트 예제 (JavaScript)
```javascript
// 학생 출석 현황 조회
function getStudentAttendance(lecSerial) {
    return fetch(`${API_BASE_URL}/attendance/student/view`, {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json',
            'Authorization': `Bearer ${token}`
        },
        body: JSON.stringify({
            lecSerial: lecSerial
        })
    });
}

// 사용 예시
getStudentAttendance("ETH201")
    .then(response => response.json())
    .then(data => {
        if (data.success) {
            const summary = data.data.summary;
            console.log(`출석률: ${summary.attendanceRate}% (필요: ${summary.requiredAttendanceRate}%)`);
            console.log(`출석: ${summary.attendedSessions}회, 결석: ${summary.absentSessions}회`);
        }
    });
```

---

## 4. 교수 출석 현황 조회

### `POST /api/attendance/professor/view`

교수가 해당 강의의 전체 수강생 출석 현황을 조회합니다. 각 학생의 출석 통계와 대기 중인 요청을 포함합니다.

#### Request Body
```json
{
  "lecSerial": "ETH201"            // [필수] 강의 코드
}
```

#### Response (성공)
```json
{
  "success": true,
  "message": "전체 출석 현황 조회가 완료되었습니다.",
  "data": [
    {
      "studentInfo": {
        "userIdx": 1,
        "userName": "김학생",
        "userEmail": "student1@example.com",
        "studentId": "20241001"
      },
      "summary": {
        "totalSessions": 16,
        "attendedSessions": 13,
        "absentSessions": 2,
        "lateSessions": 1,
        "attendanceRate": 87.5
      },
      "sessions": [
        {
          "sessionNumber": 1,
          "status": "출석",
          "date": "2025-03-03"
        },
        {
          "sessionNumber": 2,
          "status": "출석",
          "date": "2025-03-05"
        },
        {
          "sessionNumber": 3,
          "status": "결석",
          "date": "2025-03-10"
        }
      ],
      "pendingRequests": [
        {
          "sessionNumber": 3,
          "requestReason": "병원 진료로 인한 결석",
          "requestDate": "2025-10-25T10:30:00",
          "status": "PENDING"
        }
      ]
    },
    {
      "studentInfo": {
        "userIdx": 2,
        "userName": "이학생",
        "userEmail": "student2@example.com",
        "studentId": "20241002"
      },
      "summary": {
        "totalSessions": 16,
        "attendedSessions": 15,
        "absentSessions": 1,
        "lateSessions": 0,
        "attendanceRate": 93.75
      },
      "sessions": [
        {
          "sessionNumber": 1,
          "status": "출석",
          "date": "2025-03-03"
        },
        {
          "sessionNumber": 2,
          "status": "출석",
          "date": "2025-03-05"
        }
      ],
      "pendingRequests": []
    }
  ]
}
```

#### Response (실패)
```json
{
  "success": false,
  "message": "교수 권한이 필요합니다.",
  "data": null
}
```

#### HTTP Status Codes
- `200 OK`: 조회 성공
- `400 Bad Request`: 파라미터 오류
- `401 Unauthorized`: 인증 실패
- `403 Forbidden`: 교수 권한 없음
- `500 Internal Server Error`: 서버 오류

#### 권한 검증
- 교수 권한 확인 (userStudent = 1)
- 해당 강의의 담당 교수인지 확인

#### 테스트 예제 (JavaScript)
```javascript
// 교수 출석 현황 조회
function getProfessorAttendance(lecSerial) {
    return fetch(`${API_BASE_URL}/attendance/professor/view`, {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json',
            'Authorization': `Bearer ${token}`
        },
        body: JSON.stringify({
            lecSerial: lecSerial
        })
    });
}

// 사용 예시
getProfessorAttendance("ETH201")
    .then(response => response.json())
    .then(data => {
        if (data.success) {
            console.log(`총 수강생: ${data.data.length}명`);
            data.data.forEach(student => {
                console.log(`${student.studentInfo.userName}: ${student.summary.attendanceRate}%`);
                if (student.pendingRequests.length > 0) {
                    console.log(`  대기 중인 요청: ${student.pendingRequests.length}건`);
                }
            });
        }
    });
```

---

## 💡 중요 참고사항

### 인증 및 권한
- **JWT 토큰**: 모든 API에서 Bearer 토큰 인증 필요
- **학생 권한**: userStudent = 0
- **교수 권한**: userStudent = 1
- **자동 추출**: JWT에서 사용자 정보 자동 추출

### 출석 요청 워크플로우
```
1. 학생이 출석 요청 제출 (PENDING)
   ↓
2. 교수가 승인/반려 결정
   ↓
3a. 승인 시: 출석 상태 변경 + 성적 재계산
3b. 반려 시: 기존 상태 유지
```

### 데이터 구조
- **출석 상태**: "출석", "결석", "지각"
- **요청 상태**: "PENDING", "APPROVED", "REJECTED"
- **회차 번호**: 1-80 범위

### 자동 이벤트 처리
- **출석 변경** → GradeUpdateEvent 발생
- **성적 재계산** → 출석 점수 자동 업데이트
- **실시간 반영** → 즉시 성적에 반영

### 비즈니스 규칙
1. **출석률 계산**: (출석 + 지각) / 총 회차 × 100
2. **최소 출석률**: 일반적으로 75% 이상 필요
3. **중복 요청 방지**: 같은 회차에 대한 중복 요청 불가
4. **승인 권한**: 해당 강의 담당 교수만 승인 가능

### 연관 시스템
- **수강 관리**: 수강신청 정보 기반 출석 관리
- **성적 관리**: 출석 점수 자동 계산 및 반영
- **과제 관리**: 별도 시스템으로 독립 운영