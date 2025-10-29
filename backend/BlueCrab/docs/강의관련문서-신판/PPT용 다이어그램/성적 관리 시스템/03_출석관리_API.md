# 03. 출석 관리 API

> 학생이 출석 요청하고, 교수가 승인/거부하는 API

---

## 📌 기본 정보

## 출석 요청 (학생)

- **엔드포인트**: `POST /api/attendance/request`
- **권한**: 학생 (JWT 인증)

## 출석 승인/거부 (교수)

- **엔드포인트**: `POST /api/attendance/approve`
- **권한**: 교수 (JWT 인증)

## 출석 현황 조회 (학생)

- **엔드포인트**: `POST /api/attendance/student/view`
- **권한**: 학생 (JWT 인증)

## 출석 현황 조회 (교수)

- **엔드포인트**: `POST /api/attendance/professor/view`
- **권한**: 교수 (JWT 인증)

**⚠️ 중요**: 
- 출석 데이터는 **ENROLLMENT_EXTENDED_TBL.ENROLLMENT_DATA** JSON 필드에 저장됩니다
- **ATTENDANCE_REQUEST_TBL 테이블은 존재하지 않습니다**

---

## 1️⃣ 출석 요청 (학생)

### 📥 Request

```json
{
  "lecSerial": "ETH201",
  "sessionNumber": 5,
  "requestReason": "수업 참여"
}
```

| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| lecSerial | String | ✅ | 강의 코드 (LEC_SERIAL) |
| sessionNumber | Integer | ✅ | 차시 번호 (1~N) |
| requestReason | String | ❌ | 요청 사유 (선택) |

**인증**: JWT 토큰에서 학생 정보 자동 추출

### 📤 Response

```json
{
  "success": true,
  "message": "출석 요청이 완료되었습니다.",
  "data": {
    "attendanceStr": "출출출지결",
    "summary": {
      "attended": 3,
      "late": 1,
      "absent": 1,
      "attendanceRate": 80.0
    },
    "details": [
      {
        "sessionNumber": 1,
        "status": "출",
        "date": "2025-01-08"
      },
      {
        "sessionNumber": 5,
        "status": "출",
        "date": "2025-01-15",
        "requestDate": "2025-01-15T10:00:00"
      }
    ]
  }
}
```

**주요 필드**:
- `attendanceStr`: 출석 상태 문자열 (출/지/결 조합)
- `summary.attended`: 출석 횟수
- `summary.late`: 지각 횟수
- `summary.absent`: 결석 횟수
- `summary.attendanceRate`: 출석률 (%)
- `details[]`: 상세 출석 기록 배열

### 🗄️ DB 변화

**ENROLLMENT_EXTENDED_TBL.ENROLLMENT_DATA** JSON 필드 업데이트:

```json
{
  "attendance": {
    "summary": {
      "attended": 1,
      "late": 0,
      "absent": 0,
      "attendanceRate": 100.0
    },
    "pendingRequests": [
      {
        "sessionNumber": 5,
        "requestDate": "2025-01-15T10:00:00",
        "expiresAt": "2025-01-22T00:00:00",
        "tempApproved": true,
        "requestReason": "수업 참여"
      }
    ],
    "sessions": [
      {
        "sessionNumber": 5,
        "status": "출",
        "date": "2025-01-15",
        "requestDate": "2025-01-15T10:00:00"
      }
    ]
  }
}
```

**⚠️ 주의**: 
- 출석 데이터는 ENROLLMENT_DATA JSON 내부에 저장됩니다
- `attendanceStr`은 API 응답 시 동적으로 생성되며 DB에는 저장되지 않을 수 있습니다
- 필드명이 `presentCount` 대신 `attended`, `lateCount` 대신 `late` 등으로 단순화되어 있습니다

---

## 3️⃣ 출석 현황 조회 (학생)

### 📥 Request

```json
{
  "lecSerial": "ETH201"
}
```

| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| lecSerial | String | ✅ | 강의 코드 |

**인증**: JWT 토큰에서 학생 정보 자동 추출

### 📤 Response

```json
{
  "success": true,
  "data": {
    "attendanceStr": "출출출지결",
    "summary": {
      "attended": 3,
      "late": 1,
      "absent": 1,
      "attendanceRate": 80.0
    },
    "details": [
      {
        "sessionNumber": 1,
        "status": "출",
        "date": "2025-01-08"
      },
      {
        "sessionNumber": 2,
        "status": "출",
        "date": "2025-01-09"
      },
      {
        "sessionNumber": 3,
        "status": "출",
        "date": "2025-01-10"
      },
      {
        "sessionNumber": 4,
        "status": "지",
        "date": "2025-01-11"
      },
      {
        "sessionNumber": 5,
        "status": "결",
        "date": "2025-01-15"
      }
    ]
  }
}
```

**주요 필드**:
- `attendanceStr`: 출석 현황 요약 문자열 (예: "출출출지결")
- `summary`: 출석/지각/결석 통계
- `details`: 회차별 상세 출석 기록

---

## 4️⃣ 출석 현황 조회 (교수)

### 📥 Request

```json
{
  "lecSerial": "ETH201"
}
```

| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| lecSerial | String | ✅ | 강의 코드 |

**인증**: JWT 토큰에서 교수 정보 자동 추출 및 권한 확인

### 📤 Response

```json
{
  "success": true,
  "data": [
    {
      "studentIdx": 33,
      "studentName": "김철수",
      "studentCode": "20210001",
      "attendanceData": {
        "summary": {
          "attended": 3,
          "late": 1,
          "absent": 1,
          "attendanceRate": 80.0
        }
      }
    },
    {
      "studentIdx": 34,
      "studentName": "이영희",
      "studentCode": "20210002",
      "attendanceData": {
        "summary": {
          "attended": 5,
          "late": 0,
          "absent": 0,
          "attendanceRate": 100.0
        }
      }
    }
  ]
}
```

**주요 필드**:
- `data[]`: 전체 수강생 배열
- `studentIdx`: 학생 USER_IDX
- `studentName`: 학생 이름
- `studentCode`: 학번
- `attendanceData.summary`: 각 학생의 출석 통계

---

## 2️⃣ 출석 승인/거부 (교수)

### 📥 Request

```json
{
  "lecSerial": "ETH201",
  "sessionNumber": 5,
  "attendanceRecords": [
    {
      "studentIdx": 33,
      "status": "출",
      "rejectReason": null
    }
  ]
}
```

| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| lecSerial | String | ✅ | 강의 코드 |
| sessionNumber | Integer | ✅ | 차시 번호 |
| attendanceRecords[].studentIdx | Integer | ✅ | 학생 USER_IDX |
| attendanceRecords[].status | String | ✅ | `"출"` (출석) / `"지"` (지각) / `"결"` (결석) |
| attendanceRecords[].rejectReason | String | ❌ | 거부 사유 (`"결"` 처리 시 선택) |

**인증**: JWT 토큰에서 교수 정보 추출 및 권한 확인

### 📤 Response (승인)

```json
{
  "success": true,
  "message": "출석 승인이 완료되었습니다. (1/1)"
}
```

### 🗄️ DB 변화 (승인)

**ENROLLMENT_EXTENDED_TBL.ENROLLMENT_DATA**의 출석 JSON이 확정 출석과 요약 정보로 갱신됩니다.

```json
{
  "attendance": {
    "summary": {
      "attended": 1,
      "late": 0,
      "absent": 0,
      "attendanceRate": 100.0
    },
    "sessions": [
      {
        "sessionNumber": 5,
        "status": "출",
        "requestDate": "2025-01-15T10:00:00",
        "approvedAt": "2025-01-15T14:30:00",
        "approvedBy": 17,
        "date": "2025-01-15"
      }
    ],
    "pendingRequests": []
  },
  "grade": {
    "attendanceScore": {
      "currentScore": 0.25,
      "latePenalty": 0,
      "presentCount": 1,
      "lateCount": 0,
      "absentCount": 0
    }
  }
}
```

**주요 변경사항**:
- `pendingRequests`에서 해당 요청 제거
- `sessions`에 확정 출석 기록 추가
- `summary` 통계 업데이트 (필드명: attended, late, absent)
- `grade.attendanceScore` 자동 계산 및 업데이트

---

## 🔄 시퀀스 다이어그램

```plaintext
[학생 출석 요청]
학생 → API: 5차시 출석 요청
API → DB: ENROLLMENT_DATA.pendingRequests 추가
API → 학생: 요청 완료

[교수 승인]
교수 → API: 요청 승인
API → DB: ENROLLMENT_DATA.sessions 추가 + summary 재계산
API → GradeCalculationService: calculateStudentGrade 실행
API → DB: grade.attendanceScore 업데이트
API → 교수: 승인 완료
```

---

## 💡 주요 로직

### 출석 점수 자동 계산

출석 점수는 **성적 구성 설정(02번 API)**에서 설정한 값을 기준으로 계산됩니다:

```javascript
// 성적 구성 설정 예시: attendanceMaxScore = 20점
const scorePerSession = attendanceMaxScore / totalSessions;  // 예: 20 / 80 = 0.25점/회

// 출석 점수 계산
출석 1회: 0.25점
지각 1회: 0.25점 - latePenaltyPerSession (예: 0.25 - 0.3 = -0.05점 또는 0점)
결석: 0점
```

**예시** (출석 만점 20점, 전체 80회차 기준):
- **출석 1회**: 20 ÷ 80 = 0.25점
- **지각 1회**: 0.25 - 0.3(지각감점) = 최소 0점 처리
- **결석**: 0점

**⚠️ 중요**: 
- 실제 점수는 `gradeConfig.attendanceMaxScore`와 전체 회차 수에 따라 달라집니다
- 지각 감점(`latePenaltyPerSession`)은 성적 구성 설정에서 지정된 값을 사용합니다

### 승인 시 성적 재계산

```java
gradeCalculationService.calculateStudentGrade(lecIdx, studentIdx);
```

- 트랜잭션 커밋 후 `GradeCalculationService`가 출석 반영 점수를 재계산
- `attendanceScore`, `total`, `letterGrade`가 모두 최신 상태로 갱신
- `summary` 통계도 자동으로 업데이트 (attended, late, absent, attendanceRate)

---

## 📋 다음 단계

출석 관리와 병행하여 **과제 관리**도 진행됩니다.

→ [04. 과제 관리 API](./04_과제관리_API.md)
