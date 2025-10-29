# 출석 API 문서

## 📋 개요

출석 체크, 출석 인정 요청, 승인/반려, 통계 기능을 제공하는 API 문서입니다.

**컨트롤러**: 
- `AttendanceController.java` (통합 출석 요청/승인)
- `ProfessorAttendanceController.java` (교수용)
- `StudentAttendanceController.java` (학생용)

**기본 경로**: 
- `/api/attendance` (통합)
- `/api/professor/attendance` (교수)
- `/api/student/attendance` (학생)

**관련 DB 테이블**: `ATTENDANCE_REQUEST_TBL`, `ENROLLMENT_EXTENDED_TBL`, `LEC_TBL`

---

## 🔍 API 목록

## A. 학생용 API

### 1. 출석 요청

**엔드포인트**: `POST /api/attendance/request`

**권한**: 학생

**설명**: 특정 회차에 대한 출석 인정을 요청합니다.

#### Request Body
```json
{
  "lecSerial": "CS284",
  "sessionNumber": 5,
  "requestReason": "병원 진료로 인한 결석"
}
```

#### 응답 예시 (성공)
```json
{
  "success": true,
  "message": "출석 요청이 제출되었습니다.",
  "data": {
    "requestIdx": 10,
    "lecSerial": "CS284",
    "sessionNumber": 5,
    "status": "PENDING",
    "currentAttendance": "1출2출3출4출5결",
    "attendanceRate": "4/5"
  }
}
```

#### 응답 예시 (실패)
```json
{
  "success": false,
  "message": "이미 출석 처리된 회차입니다."
}
```

---

### 2. 내 출석 조회

**엔드포인트**: `POST /api/student/attendance/detail`

**권한**: 학생

#### Request Body
```json
{
  "enrollmentIdx": 1
}
```

#### 응답 예시
```json
{
  "success": true,
  "message": "출석 조회 성공",
  "data": {
    "summary": {
      "attended": 6,
      "late": 1,
      "absent": 1,
      "totalSessions": 8,
      "attendanceRate": 75.0,
      "updatedAt": "2025-10-29 10:47:34"
    },
    "sessions": [
      {"sessionNumber": 1, "status": "출", "requestDate": "2025-10-29 10:42:00", "approvedDate": "2025-10-29 10:44:35", "approvedBy": 23},
      {"sessionNumber": 2, "status": "출", "requestDate": "2025-10-29 10:42:01", "approvedDate": "2025-10-29 10:44:36", "approvedBy": 23},
      {"sessionNumber": 3, "status": "결", "requestDate": "2025-10-29 10:42:02", "approvedDate": "2025-10-29 10:44:37", "approvedBy": 23},
      {"sessionNumber": 4, "status": "지", "requestDate": "2025-10-29 10:42:03", "approvedDate": "2025-10-29 10:44:38", "approvedBy": 23},
      {"sessionNumber": 5, "status": "출", "requestDate": "2025-10-29 10:42:04", "approvedDate": "2025-10-29 10:44:39", "approvedBy": 23},
      {"sessionNumber": 6, "status": "출", "requestDate": "2025-10-29 10:42:05", "approvedDate": "2025-10-29 10:44:40", "approvedBy": 23},
      {"sessionNumber": 7, "status": "출", "requestDate": "2025-10-29 10:42:06", "approvedDate": "2025-10-29 10:44:41", "approvedBy": 23},
      {"sessionNumber": 8, "status": "출", "requestDate": "2025-10-29 10:42:07", "approvedDate": "2025-10-29 10:44:42", "approvedBy": 23}
    ],
    "pendingRequests": []
  }
}
```

---

### 3. 내 출석 요청 목록

**엔드포인트**: `POST /api/student/attendance/requests`

**권한**: 학생

#### Request Body
```json
{
  "studentIdx": 6,
  "lecSerial": "CS284"
}
```

#### 응답 예시
```json
{
  "success": true,
  "message": "출석 요청 목록 조회 성공",
  "data": [
    {
      "requestIdx": 10,
      "lecSerial": "CS284",
      "sessionNumber": 5,
      "requestReason": "병원 진료",
      "status": "PENDING",
      "requestedAt": "2025-10-25T10:00:00",
      "processedAt": null,
      "rejectionReason": null
    },
    {
      "requestIdx": 8,
      "lecSerial": "CS284",
      "sessionNumber": 3,
      "requestReason": "가족 경조사",
      "status": "APPROVED",
      "requestedAt": "2025-10-20T14:00:00",
      "processedAt": "2025-10-21T09:00:00",
      "rejectionReason": null
    }
  ]
}
```

---

## B. 교수용 API

### 4. 출석 인정 요청 목록

**엔드포인트**: `POST /api/professor/attendance/requests`

**권한**: 교수

#### Request Body (전체 조회)
```json
{
  "lecIdx": 1,
  "page": 0,
  "size": 20
}
```

#### Request Body (대기 중만)
```json
{
  "lecIdx": 1,
  "status": "PENDING",
  "page": 0,
  "size": 20
}
```

#### 응답 예시
```json
{
  "success": true,
  "message": "출석 인정 요청 목록 조회 성공",
  "data": {
    "content": [
      {
        "requestIdx": 10,
        "lecIdx": 1,
        "studentIdx": 6,
        "studentCode": "240105045",
        "studentName": "집갈래",
        "sessionNumber": 5,
        "requestReason": "병원 진료로 인한 결석",
        "status": "PENDING",
        "requestedAt": "2025-10-25T10:00:00",
        "processedAt": null
      }
    ],
    "totalElements": 5,
    "totalPages": 1
  }
}
```

---

### 5. 대기 중인 요청 개수

**엔드포인트**: `POST /api/professor/attendance/requests/count`

**권한**: 교수

#### Request Body
```json
{
  "lecIdx": 1
}
```

#### 응답 예시
```json
{
  "success": true,
  "message": "대기 중인 요청 개수 조회 성공",
  "data": {
    "pendingCount": 3
  }
}
```

---

### 6. 출석 요청 승인

**엔드포인트**: `PUT /api/professor/attendance/requests/{requestIdx}/approve`

**권한**: 교수

#### Request Body (선택사항)
```json
{
  "approvalNote": "인정합니다."
}
```

#### 응답 예시
```json
{
  "success": true,
  "message": "출석 요청이 승인되었습니다.",
  "data": {
    "requestIdx": 10,
    "status": "APPROVED",
    "processedAt": "2025-10-26T09:30:00"
  }
}
```

---

### 7. 출석 요청 반려

**엔드포인트**: `PUT /api/professor/attendance/requests/{requestIdx}/reject`

**권한**: 교수

#### Request Body
```json
{
  "rejectionReason": "사유가 불충분합니다."
}
```

#### 응답 예시
```json
{
  "success": true,
  "message": "출석 요청이 반려되었습니다.",
  "data": {
    "requestIdx": 10,
    "status": "REJECTED",
    "rejectionReason": "사유가 불충분합니다.",
    "processedAt": "2025-10-26T09:30:00"
  }
}
```

---

### 8. 출석 체크 (직접 입력)

**엔드포인트**: `POST /api/professor/attendance/mark`

**권한**: 교수

**설명**: 교수가 직접 학생의 출석을 입력/수정합니다.

#### Request Body
```json
{
  "enrollmentIdx": 1,
  "sessionNumber": 8,
  "status": "출"
}
```

**status 값**: `"출"` (출석), `"결"` (결석), `"지"` (지각), `"조"` (조퇴)

#### 응답 예시
```json
{
  "success": true,
  "message": "출석이 입력되었습니다.",
  "data": {
    "enrollmentIdx": 1,
    "sessionNumber": 8,
    "status": "출",
    "currentAttendance": "1출2출3출4출5출6출7출8출",
    "attendanceRate": "8/8"
  }
}
```

---

### 9. 전체 수강생 출석 현황

**엔드포인트**: `POST /api/attendance/professor/view`

**권한**: 교수

#### Request Body
```json
{
  "lecSerial": "CS284"
}
```

#### 응답 예시
```json
{
  "success": true,
  "message": "전체 수강생 출석 현황 조회 성공",
  "data": {
    "lecSerial": "CS284",
    "lecTit": "컴퓨터과학개론",
    "totalSessions": 16,
    "students": [
      {
        "enrollmentIdx": 1,
        "studentIdx": 6,
        "studentCode": "240105045",
        "studentName": "집갈래",
        "attendanceStr": "1출2출3결4지5출6출7출8출",
        "attendanceRate": "6/8",
        "attendancePercentage": 75.0
      }
    ]
  }
}
```

---

## C. 통합 출석 조회 API

### 10. 학생 출석 현황 조회

**엔드포인트**: `POST /api/attendance/student/view`

**권한**: 학생 (본인만)

#### Request Body
```json
{
  "lecSerial": "CS284",
  "studentIdx": 6
}
```

#### 응답 예시
```json
{
  "success": true,
  "message": "출석 현황 조회 성공",
  "data": {
    "lecSerial": "CS284",
    "lecTit": "컴퓨터과학개론",
    "studentIdx": 6,
    "attendanceStr": "1출2출3결4지5출6출7출8출",
    "attendanceRate": "6/8",
    "attendancePercentage": 75.0,
    "details": [
      {"sessionNumber": 1, "status": "출"},
      {"sessionNumber": 2, "status": "출"},
      {"sessionNumber": 3, "status": "결"},
      ...
    ]
  }
}
```

---

## 📊 DTO 구조

### AttendanceRequestDto
```java
{
  "requestIdx": Integer,
  "lecIdx": Integer,
  "lecSerial": String,
  "studentIdx": Integer,
  "studentCode": String,
  "studentName": String,
  "sessionNumber": Integer,
  "requestReason": String,
  "status": String,           // "PENDING", "APPROVED", "REJECTED"
  "rejectionReason": String,
  "requestedAt": String (ISO-8601),
  "processedAt": String (ISO-8601)
}
```

### AttendanceDto
```java
{
  "sessionNumber": Integer,
  "status": String            // "출", "결", "지", "조"
}
```

### StudentAttendanceDto
```java
{
  "enrollmentIdx": Integer,
  "studentIdx": Integer,
  "studentCode": String,
  "studentName": String,
  "attendanceStr": String,    // "1출2출3결4지..."
  "attendanceRate": String,   // "6/8"
  "attendancePercentage": Double
}
```

---

## 🔗 관련 테이블

### ATTENDANCE_REQUEST_TBL
- **기본 키**: `request_idx`
- **주요 컬럼**:
  - `lec_idx`: 강의 ID (FK → LEC_TBL)
  - `student_idx`: 학생 ID (FK → USER_TBL)
  - `session_number`: 회차 번호
  - `request_reason`: 요청 사유
  - `status`: 상태 (PENDING, APPROVED, REJECTED)
  - `rejection_reason`: 반려 사유
  - `requested_at`: 요청 일시
  - `processed_at`: 처리 일시

### ENROLLMENT_EXTENDED_TBL
**enrollmentData 내 출석 정보**:
```json
{
  "attendance": {
    "summary": {
      "attended": 75,
      "late": 4,
      "absent": 1,
      "totalSessions": 80,
      "attendanceRate": 95.25,
      "updatedAt": "2025-10-29 10:47:34"
    },
    "sessions": [
      {
        "sessionNumber": 1,
        "status": "출",
        "requestDate": "2025-10-29 10:42:00",
        "approvedDate": "2025-10-29 10:44:35",
        "approvedBy": 23,
        "tempApproved": false
      }
    ],
    "pendingRequests": []
  }
}
```

---

## 📈 비즈니스 로직

### 출석 데이터 구조
**sessions 배열**: 각 회차별 상세 정보
- `sessionNumber`: 회차 번호 (1~80)
- `status`: 출석 상태 (`"출"`, `"결"`, `"지"`, `"조"`)
- `requestDate`: 출석 요청 일시
- `approvedDate`: 승인 일시
- `approvedBy`: 승인한 교수 USER_IDX
- `tempApproved`: 임시 승인 여부

**summary 객체**: 자동 계산되는 통계
- `attended`: 출석 횟수
- `late`: 지각 횟수
- `absent`: 결석 횟수
- `totalSessions`: 전체 세션 수
- `attendanceRate`: 출석률 (%)
- `updatedAt`: 마지막 업데이트 일시

### 출석률 계산

```java
출석 인정 = attended + late  // "출", "지", "조" 카운트
전체 회차 = totalSessions
출석률 (%) = (출석 인정 / 전체 회차) × 100
```

### 출석 요청 승인 프로세스

1. 요청 상태 확인 (PENDING만 처리 가능)
2. 권한 확인 (담당 교수만 가능)
3. `sessions` 배열에 새로운 출석 레코드 추가
4. `summary` 통계 자동 업데이트
5. 요청 상태 변경 (APPROVED)
6. 처리 일시 기록
7. 알림 발송 (선택)

---

## ⚠️ 주의사항

1. **lecSerial 사용**: `lecIdx` 대신 `lecSerial` 권장
2. **출석 데이터 구조**: sessions 배열 방식으로 관리 (문자열 파싱 불필요)
3. **동시성 제어**: 출석 데이터 업데이트 시 트랜잭션 처리 필요
4. **권한 검증**:
   - 학생: 본인 출석만 조회/요청 가능
   - 교수: 담당 강의 출석만 관리 가능
5. **중복 요청 방지**: 같은 회차에 대한 중복 요청 차단
6. **자동 계산**: summary 통계는 백엔드에서 자동 계산

---

## 🔄 이벤트

### AttendanceApprovedEvent

출석 요청 승인 시 발행되는 이벤트 (알림 전송용)

```java
eventPublisher.publishEvent(
    new AttendanceApprovedEvent(this, requestIdx, studentIdx, lecIdx)
);
```

---

© 2025 BlueCrab LMS Development Team
