# 02. 성적 구성 설정 API

> 교수가 출석/과제 배점을 설정하는 API

---

## 📌 기본 정보

- **엔드포인트**: `POST /api/enrollments/grade-config`
- **권한**: 교수
- **설명**: 출석 만점, 과제 총점을 설정하여 전체 수강생의 성적 구조 초기화

---

## 📥 Request

```json
{
  "action": "set-config",
  "lecSerial": "ETH201",
  "attendanceMaxScore": 120,
  "assignmentTotalScore": 157
}
```

| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| action | String | ✅ | 반드시 `"set-config"` |
| lecSerial | String | ✅ | 강의 코드 |
| attendanceMaxScore | Integer | ✅ | 출석 만점 (예: 120점) |
| assignmentTotalScore | Integer | ✅ | 과제 총점 (자동 계산 권장) |

---

## 📤 Response

```json
{
  "success": true,
  "message": "성적 구성이 설정되었습니다.",
  "data": {
    "lecIdx": 42,
    "updatedCount": 42,
    "gradeConfig": {
      "attendanceMaxScore": 120,
      "assignmentTotalScore": 157,
      "totalMaxScore": 277
    }
  }
}
```

---

## 🗄️ DB 변화

### ENROLLMENT_EXTENDED_TBL

**모든 수강생의 `ENROLLMENT_DATA` 업데이트**:

```json
{
  "gradeConfig": {
    "attendanceMaxScore": 120,
    "assignmentTotalScore": 157,
    "totalMaxScore": 277
  },
  "attendance": {
    "summary": { "presentCount": 0, "lateCount": 0, "absentCount": 0, "pendingCount": 0 },
    "sessions": []
  },
  "grade": {
    "attendanceScore": {
      "currentScore": 0,
      "latePenalty": 0,
      "presentCount": 0,
      "lateCount": 0,
      "absentCount": 0
    },
    "assignments": [],
    "total": { "currentScore": 0, "percentage": 0 },
    "letterGrade": null
  }
}
```

---

## 🔄 시퀀스 다이어그램

```plaintext
교수 → API: 출석/과제 배점 설정
API → DB: LEC_TBL에서 해당 강의의 모든 수강생 조회
API → DB: 각 수강생의 ENROLLMENT_DATA 업데이트
DB → API: 업데이트 완료 (42건)
API → 교수: 성공 메시지
```

---

## 💡 주요 로직

1. **totalMaxScore 자동 계산**: `attendanceMaxScore + assignmentTotalScore`
2. **일괄 업데이트**: 해당 강의의 모든 수강생 ENROLLMENT_DATA 초기화
3. **이미 설정된 경우**: 기존 데이터 덮어쓰기 (주의!)

---

## ⚠️ 주의사항

- `assignmentTotalScore`는 **ASSIGNMENT_EXTENDED_TBL의 TASK_SCORE 합계**로 자동 계산 권장
- 수동 설정 시 실제 과제 점수와 불일치 가능

---

## 📋 다음 단계

성적 구성 설정 완료 후:

1. 교수가 **출석 관리** 시작 가능
2. 교수가 **과제 생성** 시작 가능

→ [03. 출석 관리 API](./03_출석관리_API.md)  
→ [04. 과제 관리 API](./04_과제관리_API.md)
