# 03. 출석 관리 API

> 학생이 출석 요청하고, 교수가 승인/거부하는 API

---

## 📌 기본 정보

### 출석 요청 (학생)

- **엔드포인트**: `POST /api/attendance/request`
- **권한**: 학생

### 출석 승인/거부 (교수)

- **엔드포인트**: `POST /api/attendance/approve`
- **권한**: 교수

---

## 1️⃣ 출석 요청 (학생)

### 📥 Request

```json
{
  "enrollmentIdx": 1,
  "sessionNumber": 5,
  "status": "출",
  "requestReason": "수업 참여"
}
```

| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| enrollmentIdx | Integer | ✅ | 수강 ID |
| sessionNumber | Integer | ✅ | 차시 번호 (1~80) |
| status | String | ✅ | `"출"` / `"지"` / `"결"` |
| requestReason | String | ❌ | 요청 사유 |

### 📤 Response

```json
{
  "success": true,
  "message": "5차시 출석 요청이 등록되었습니다.",
  "requestIdx": 123
}
```

### 🗄️ DB 변화

**ATTENDANCE_REQUEST_TBL 새 레코드 생성**:

```json
{
  "REQUEST_IDX": 123,
  "ENROLLMENT_IDX": 1,
  "SESSION_NUMBER": 5,
  "STATUS": "출",
  "REQUEST_REASON": "수업 참여",
  "APPROVAL_STATUS": "대기중"
}
```

**ENROLLMENT_DATA 업데이트**:

```json
{
  "attendance": {
    "summary": {
      "pendingCount": 1
    },
    "pendingRequests": [
      {
        "requestIdx": 123,
        "sessionNumber": 5,
        "status": "출",
        "requestReason": "수업 참여",
        "approvalStatus": "대기중"
      }
    ]
  }
}
```

---

## 2️⃣ 출석 승인/거부 (교수)

### 📥 Request

```json
{
  "requestIdx": 123,
  "approvalStatus": "승인",
  "rejectionReason": ""
}
```

| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| requestIdx | Integer | ✅ | 출석 요청 ID |
| approvalStatus | String | ✅ | `"승인"` / `"거부"` |
| rejectionReason | String | ❌ | 거부 사유 (거부 시 필수) |

### 📤 Response (승인)

```json
{
  "success": true,
  "message": "5차시 출석이 승인되었습니다."
}
```

### 🗄️ DB 변화 (승인)

**ATTENDANCE_REQUEST_TBL 업데이트**:

```json
{
  "APPROVAL_STATUS": "승인",
  "APPROVED_AT": "2025-01-15T14:30:00"
}
```

**ENROLLMENT_DATA 업데이트**:

```json
{
  "attendance": {
    "summary": {
      "presentCount": 1,
      "pendingCount": 0
    },
    "sessions": [
      {
        "sessionNumber": 5,
        "status": "출",
        "requestedAt": "2025-01-15T10:00:00",
        "approvedAt": "2025-01-15T14:30:00"
      }
    ],
    "pendingRequests": []
  },
  "grade": {
    "attendanceScore": {
      "currentScore": 1.5,
      "presentCount": 1
    }
  }
}
```

---

## 🔄 시퀀스 다이어그램

```plaintext
[학생 출석 요청]
학생 → API: 5차시 출석 요청
API → DB: ATTENDANCE_REQUEST_TBL INSERT
API → DB: ENROLLMENT_DATA.pendingRequests 추가
API → 학생: 요청 완료

[교수 승인]
교수 → API: 요청 승인
API → DB: ATTENDANCE_REQUEST_TBL 승인 상태 업데이트
API → DB: ENROLLMENT_DATA.sessions 추가
API → 이벤트: GradeUpdateEvent 발행
이벤트 → API: 출석 점수 자동 재계산
API → DB: grade.attendanceScore 업데이트
API → 교수: 승인 완료
```

---

## 💡 주요 로직

### 출석 점수 자동 계산

- **출석 1회**: `attendanceMaxScore / 80` (예: 120 ÷ 80 = 1.5점)
- **지각 1회**: 출석 점수의 50% (예: 0.75점)
- **결석**: 0점

### 승인 시 이벤트 발행

```java
eventPublisher.publishEvent(new GradeUpdateEvent(enrollmentIdx));
```

- GradeCalculationService가 자동으로 전체 성적 재계산
- `attendanceScore`, `total`, `letterGrade` 모두 업데이트

---

## 📋 다음 단계

출석 관리와 병행하여 **과제 관리**도 진행됩니다.

→ [04. 과제 관리 API](./04_과제관리_API.md)
