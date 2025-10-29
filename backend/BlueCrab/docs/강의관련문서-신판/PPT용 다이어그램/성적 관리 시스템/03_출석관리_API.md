# 03. 출석 관리 API

> 학생이 출석 요청하고, 교수가 승인/거부하는 API

---

## 📌 기본 정보

## 출석 요청 (학생)

- **엔드포인트**: `POST /api/attendance/request`
- **권한**: 학생

## 출석 승인/거부 (교수)

- **엔드포인트**: `POST /api/attendance/approve`
- **권한**: 교수

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
| sessionNumber | Integer | ✅ | 차시 번호 (1~80) |
| requestReason | String | ❌ | 요청 사유 (선택) |

### 📤 Response

```json
{
  "success": true,
  "message": "출석 요청이 완료되었습니다.",
  "data": {
    "summary": {
      "presentCount": 0,
      "lateCount": 0,
      "absentCount": 0,
      "totalSessions": 80,
      "attendanceRate": 0.0
    },
    "pendingRequests": [
      {
        "sessionNumber": 5,
        "requestDate": "2025-01-15T10:00:00",
        "expiresAt": "2025-01-22T00:00:00",
        "tempApproved": true
      }
    ],
    "sessions": []
  }
}
```

### 🗄️ DB 변화

**ENROLLMENT_EXTENDED_TBL.ENROLLMENT_DATA**에 `attendance.pendingRequests[]` 항목이 추가됩니다.

```json
{
  "attendance": {
    "summary": {
      "pendingCount": 1
    },
    "pendingRequests": [
      {
        "sessionNumber": 5,
        "requestDate": "2025-01-15T10:00:00",
        "expiresAt": "2025-01-22T00:00:00",
        "tempApproved": true
      }
    ],
    "sessions": []
  }
}
```

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
| attendanceRecords[].status | String | ✅ | `"출"` / `"지"` / `"결"` |
| attendanceRecords[].rejectReason | String | ❌ | 거부 사유 (`"결"` 처리 시 선택) |

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
      "presentCount": 1,
      "pendingCount": 0
    },
    "sessions": [
      {
        "sessionNumber": 5,
        "status": "출",
        "requestDate": "2025-01-15T10:00:00",
        "approvedAt": "2025-01-15T14:30:00",
        "approvedBy": 17
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

- **출석 1회**: `attendanceMaxScore / 80` (예: 120 ÷ 80 = 1.5점)
- **지각 1회**: 출석 점수의 50% (예: 0.75점)
- **결석**: 0점

### 승인 시 성적 재계산

```java
gradeCalculationService.calculateStudentGrade(lecIdx, studentIdx);
```

- 트랜잭션 커밋 후 `GradeCalculationService`가 출석 반영 점수를 재계산
- `attendanceScore`, `total`, `letterGrade`가 모두 최신 상태로 갱신

---

## 📋 다음 단계

출석 관리와 병행하여 **과제 관리**도 진행됩니다.

→ [04. 과제 관리 API](./04_과제관리_API.md)
