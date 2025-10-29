# 04. 과제 관리 API

> 교수가 과제를 생성하고 채점하는 API

---

## 📌 기본 정보

### 과제 생성 (교수)

- **엔드포인트**: `POST /api/assignments`
- **권한**: 교수

### 과제 채점 (교수)

- **엔드포인트**: `PUT /api/assignments/{id}/grade`
- **권한**: 교수

---

## 1️⃣ 과제 생성 (교수)

### 📥 Request

```json
{
  "lecIdx": 42,
  "taskName": "중간과제",
  "taskDescription": "Servlet 구현",
  "taskScore": 50,
  "dueDate": "2025-02-15"
}
```

### 📤 Response

```json
{
  "success": true,
  "message": "과제가 생성되었습니다.",
  "taskIdx": 15
}
```

### 🗄️ DB 변화

**ASSIGNMENT_EXTENDED_TBL 새 레코드 생성**:

| 컬럼 | 값 |
|------|-----|
| TASK_IDX | 15 |
| LEC_IDX | 42 |
| TASK_NAME | "중간과제" |
| TASK_SCORE | 50 |
| DUE_DATE | "2025-02-15" |

**모든 수강생의 ENROLLMENT_DATA 업데이트**:

```json
{
  "gradeConfig": {
    "assignmentTotalScore": 157
  },
  "grade": {
    "assignments": [
      {
        "taskIdx": 15,
        "taskName": "중간과제",
        "score": null
      }
    ]
  }
}
```

---

## 2️⃣ 과제 채점 (교수)

### 📥 채점 Request

```json
{
  "taskIdx": 15,
  "enrollmentIdx": 1,
  "score": 45,
  "feedback": "잘했습니다."
}
```

### 📤 채점 Response

```json
{
  "success": true,
  "message": "채점이 완료되었습니다."
}
```

### 🗄️ 채점 후 DB 변화

**ENROLLMENT_DATA 업데이트**:

```json
{
  "grade": {
    "assignments": [
      {
        "taskIdx": 15,
        "taskName": "중간과제",
        "score": 45,
        "gradedAt": "2025-02-16T10:00:00",
        "feedback": "잘했습니다."
      }
    ],
    "total": {
      "currentScore": 120.5,
      "percentage": 43.5
    },
    "letterGrade": null
  }
}
```

**GradeUpdateEvent 발행** → 전체 성적 자동 재계산

---

## 🔄 시퀀스 다이어그램

```plaintext
[과제 생성]
교수 → API: 과제 생성 (50점)
API → DB: ASSIGNMENT_EXTENDED_TBL INSERT
API → DB: 모든 수강생 ENROLLMENT_DATA.assignments 추가
API → DB: gradeConfig.assignmentTotalScore 업데이트
API → 교수: 생성 완료

[과제 채점]
교수 → API: 45점 부여
API → DB: ENROLLMENT_DATA.assignments[0].score 업데이트
API → 이벤트: GradeUpdateEvent 발행
이벤트 → API: 전체 성적 재계산
API → DB: grade.total 업데이트
API → 교수: 채점 완료
```

---

## 💡 주요 로직

### assignmentTotalScore 자동 계산

```sql
SELECT SUM(TASK_SCORE) FROM ASSIGNMENT_EXTENDED_TBL WHERE LEC_IDX = 42
```

- 과제 생성 시 모든 수강생의 `gradeConfig.assignmentTotalScore` 자동 업데이트

### 전체 점수 자동 계산

- **출석 점수**: 75.5점 (출석 50회 × 1.5점)
- **과제 점수**: 45점 (중간과제 채점)
- **합계**: 120.5점
- **퍼센테이지**: 120.5 ÷ 277 × 100 = 43.5%

---

## 📋 다음 단계

채점 완료 후 학생/교수 모두 **성적 조회** 가능합니다.

→ [05. 성적 조회 API](./05_성적조회_API.md)
