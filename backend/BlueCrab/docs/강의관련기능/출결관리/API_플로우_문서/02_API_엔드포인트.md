# 02. API 엔드포인트

> 🌐 출석 시스템의 4개 RESTful API 명세

---

## API 목록

| 번호 | 엔드포인트 | 메서드 | 설명 | 권한 |
|-----|-----------|--------|------|------|
| 1 | `/api/attendance/request` | POST | 학생 출석 요청 | 학생 |
| 2 | `/api/attendance/approve` | POST | 교수 출석 승인 | 교수/관리자 |
| 3 | `/api/attendance/student/view` | POST | 학생 출석 조회 | 학생 |
| 4 | `/api/attendance/professor/view` | POST | 교수 출석 조회 | 교수/관리자 |

---

## 공통 사항

### Base URL
```
https://bluecrab.chickenkiller.com/BlueCrab-1.0.0
```

### 공통 헤더
```http
Content-Type: application/json
Authorization: Bearer {JWT_TOKEN}
Accept: application/json
```

### 공통 응답 구조
```json
{
  "success": true,
  "message": "성공 메시지",
  "data": { /* 실제 데이터 */ },
  "timestamp": "2025-10-23T06:13:37.521069387Z",
  "errorCode": null
}
```

### 에러 응답 구조
```json
{
  "success": false,
  "message": "에러 메시지",
  "data": null,
  "timestamp": "2025-10-23T06:13:37.521069387Z",
  "errorCode": "ERROR_CODE"
}
```

### HTTP 상태 코드

| 코드 | 상태 | 설명 |
|------|------|------|
| 200 | OK | 요청 성공 |
| 400 | Bad Request | 잘못된 요청 (유효성 검사 실패) |
| 401 | Unauthorized | 인증 실패 (토큰 없음/만료) |
| 403 | Forbidden | 권한 없음 |
| 404 | Not Found | 리소스를 찾을 수 없음 |
| 500 | Internal Server Error | 서버 오류 |

---

## 1. 학생 출석 요청 API

### 기본 정보
- **엔드포인트**: `POST /api/attendance/request`
- **권한**: 학생 (authenticated)
- **설명**: 학생이 수강 중인 강의의 특정 회차 출석을 요청

### 요청 파라미터
```json
{
  "lecSerial": "ETH201",
  "sessionNumber": 1,
  "requestReason": "교통체증으로 지각" // 선택사항
}
```

| 필드 | 타입 | 필수 | 설명 | 제약 |
|-----|------|------|------|------|
| `lecSerial` | String | ✅ | 강의 코드 | 영문/숫자, 최대 20자 |
| `sessionNumber` | Integer | ✅ | 회차 번호 | 1~80 |
| `requestReason` | String | ⬜ | 요청 사유 | 최대 200자 |

**참고**: `studentIdx`는 JWT 토큰에서 자동 추출

### 응답 예시
```json
{
  "success": true,
  "message": "출석 요청이 접수되었습니다.",
  "data": {
    "summary": {
      "attended": 10,
      "late": 2,
      "absent": 1,
      "totalSessions": 14,
      "attendanceRate": 71.4,
      "updatedAt": "2025-10-23 15:30:00"
    },
    "sessions": [ /* 기존 출석 기록 */ ],
    "pendingRequests": [
      {
        "sessionNumber": 1,
        "requestDate": "2025-10-23 15:30:00",
        "expiresAt": "2025-10-30 00:00:00",
        "requestReason": "교통체증으로 지각",
        "tempApproved": true
      }
    ]
  }
}
```

자세한 내용: **[03. 학생 출석 요청](./03_학생_출석_요청.md)**

---

## 2. 교수 출석 승인 API

### 기본 정보
- **엔드포인트**: `POST /api/attendance/approve`
- **권한**: 교수, 관리자
- **설명**: 교수가 학생들의 출석 요청을 승인/지각/결석 처리

### 요청 파라미터
```json
{
  "lecSerial": "ETH201",
  "sessionNumber": 1,
  "attendanceRecords": [
    {
      "studentIdx": 6,
      "status": "출"
    },
    {
      "studentIdx": 7,
      "status": "지"
    }
  ]
}
```

| 필드 | 타입 | 필수 | 설명 | 제약 |
|-----|------|------|------|------|
| `lecSerial` | String | ✅ | 강의 코드 | 영문/숫자, 최대 20자 |
| `sessionNumber` | Integer | ✅ | 회차 번호 | 1~80 |
| `attendanceRecords` | Array | ✅ | 승인 레코드 배열 | 최소 1개 |
| `attendanceRecords[].studentIdx` | Integer | ✅ | 학생 USER_IDX | 양수 |
| `attendanceRecords[].status` | String | ✅ | 출석 상태 | "출", "지", "결" |

**참고**: `professorIdx`는 JWT 토큰에서 자동 추출

### 응답 예시
```json
{
  "success": true,
  "message": "출석 승인이 완료되었습니다. (2/2)",
  "data": null
}
```

자세한 내용: **[04. 교수 출석 승인](./04_교수_출석_승인.md)**

---

## 3. 학생 출석 조회 API

### 기본 정보
- **엔드포인트**: `POST /api/attendance/student/view`
- **권한**: 학생 (authenticated)
- **설명**: 학생이 본인의 출석 현황 조회

### 요청 파라미터
```json
{
  "lecSerial": "ETH201"
}
```

| 필드 | 타입 | 필수 | 설명 | 제약 |
|-----|------|------|------|------|
| `lecSerial` | String | ✅ | 강의 코드 | 영문/숫자, 최대 20자 |

**참고**: `studentIdx`는 JWT 토큰에서 자동 추출

### 응답 예시
```json
{
  "success": true,
  "message": "출석 현황 조회 성공",
  "data": {
    "summary": {
      "attended": 10,
      "late": 2,
      "absent": 1,
      "totalSessions": 13,
      "attendanceRate": 76.9,
      "updatedAt": "2025-10-23 15:30:00"
    },
    "sessions": [
      {
        "sessionNumber": 1,
        "status": "출",
        "requestDate": "2025-10-15 10:00:00",
        "approvedDate": "2025-10-15 10:30:00",
        "approvedBy": 25,
        "tempApproved": false
      }
      // ... 더 많은 sessions
    ],
    "pendingRequests": [
      {
        "sessionNumber": 14,
        "requestDate": "2025-10-23 15:30:00",
        "expiresAt": "2025-10-30 00:00:00",
        "requestReason": "교통체증",
        "tempApproved": true
      }
    ]
  }
}
```

---

## 4. 교수 출석 조회 API

### 기본 정보
- **엔드포인트**: `POST /api/attendance/professor/view`
- **권한**: 교수, 관리자
- **설명**: 교수가 담당 강의의 전체 학생 출석 현황 조회

### 요청 파라미터
```json
{
  "lecSerial": "ETH201"
}
```

| 필드 | 타입 | 필수 | 설명 | 제약 |
|-----|------|------|------|------|
| `lecSerial` | String | ✅ | 강의 코드 | 영문/숫자, 최대 20자 |

**참고**: `professorIdx`는 JWT 토큰에서 자동 추출

### 응답 예시
```json
{
  "success": true,
  "message": "출석 현황 조회 성공",
  "data": [
    {
      "studentIdx": 6,
      "studentCode": "20210001",
      "studentName": "테스터",
      "attendanceData": {
        "summary": {
          "attended": 10,
          "late": 2,
          "absent": 1,
          "totalSessions": 13,
          "attendanceRate": 76.9,
          "updatedAt": "2025-10-23 15:30:00"
        },
        "sessions": [ /* ... */ ],
        "pendingRequests": [ /* ... */ ]
      }
    }
    // ... 더 많은 학생
  ]
}
```

**주의**: 교수용 API는 `attendanceData` 중첩 구조 사용

---

## JWT 토큰 처리

### 토큰 구조
```json
{
  "type": "access",
  "userId": 25,
  "sub": "prof.octopus@univ.edu",
  "iat": 1729666417,
  "exp": 1729667317
}
```

### 자동 추출 필드

| API | 추출 필드 | 설명 |
|-----|----------|------|
| 학생 출석 요청 | `userId` → `studentIdx` | 학생 USER_IDX |
| 교수 출석 승인 | `userId` → `professorIdx` | 교수 USER_IDX |
| 학생 출석 조회 | `userId` → `studentIdx` | 학생 USER_IDX |
| 교수 출석 조회 | `userId` → `professorIdx` | 교수 USER_IDX |

### 브라우저에서의 토큰 우선순위
```javascript
function getToken() {
    return window.authToken || 
           localStorage.getItem('accessToken') || 
           sessionStorage.getItem('accessToken');
}
```

---

## 에러 케이스

### 1. 인증 실패 (401)
```json
{
  "success": false,
  "message": "JWT 토큰이 유효하지 않습니다.",
  "errorCode": "UNAUTHORIZED"
}
```

### 2. 권한 없음 (403)
```json
{
  "success": false,
  "message": "해당 강의의 담당 교수가 아닙니다.",
  "errorCode": "FORBIDDEN"
}
```

### 3. 유효성 검사 실패 (400)
```json
{
  "success": false,
  "message": "sessionNumber는 1에서 80 사이의 값이어야 합니다.",
  "errorCode": "VALIDATION_ERROR"
}
```

### 4. 리소스 없음 (404)
```json
{
  "success": false,
  "message": "해당 강의를 찾을 수 없습니다.",
  "errorCode": "NOT_FOUND"
}
```

---

## API 테스트

### 브라우저 콘솔 테스트
4개의 브라우저 콘솔 테스트 스크립트 제공:

1. `01-attendance-request.js` - 학생 출석 요청
2. `02-attendance-approve.js` - 교수 출석 승인
3. `03-student-view.js` - 학생 출석 조회
4. `04-professor-view.js` - 교수 출석 조회

위치: `docs/강의관련기능/브라우저콘솔테스트/06-attendance/`

### Postman 컬렉션
준비 중 (선택사항)

---

## 다음 단계

- **[03. 학생 출석 요청](./03_학생_출석_요청.md)**: 학생 API 상세 가이드
- **[04. 교수 출석 승인](./04_교수_출석_승인.md)**: 교수 API 상세 가이드
- **[07. 데이터 구조](./07_데이터_구조.md)**: JSON 데이터 구조 이해

---

**📚 [목차로 돌아가기](./README.md)**
