# 04. 과제 관리 API

> 교수가 과제를 생성하고 채점하는 API

---

## 📌 기본 정보

### 과제 목록 조회

- **엔드포인트**: `POST /api/assignments/list`
- **권한**: 학생/교수

### 과제 상세 조회

- **엔드포인트**: `POST /api/assignments/detail`
- **권한**: 학생/교수

### 과제 생성 (교수)

- **엔드포인트**: `POST /api/assignments`
- **권한**: 교수

### 과제 채점 (교수)

- **엔드포인트**: `PUT /api/assignments/{assignmentIdx}/grade`
- **권한**: 교수

**⚠️ 중요**: 
- 과제 데이터는 **ASSIGNMENT_EXTENDED_TBL.ASSIGNMENT_DATA** JSON 필드에 저장됩니다
- 테이블에는 `ASSIGNMENT_IDX`, `LEC_IDX`, `ASSIGNMENT_DATA` 컬럼만 존재합니다
- `TASK_IDX`, `TASK_NAME`, `TASK_SCORE` 같은 개별 컬럼은 없고 모두 JSON 내부에 저장됩니다

---

## 1️⃣ 과제 목록 조회

### 📥 Request

```json
{
  "lecSerial": "ETH201",
  "page": 0,
  "size": 20
}
```

### 📤 Response

```json
{
  "content": [
    {
      "assignmentIdx": 15,
      "assignmentData": "{\"assignment\":{\"title\":\"중간과제\",\"description\":\"Servlet 구현\",\"maxScore\":50,\"dueDate\":\"2025-02-15T23:59:59\",\"status\":\"ACTIVE\",\"createdAt\":\"2025-02-01T10:00:00\"},\"submissions\":[]}"
    }
  ],
  "totalElements": 1,
  "totalPages": 1
}
```

**⚠️ 주의**: `assignmentData`는 **문자열로 반환**되므로 프론트엔드에서 `JSON.parse()` 필요

---

## 2️⃣ 과제 상세 조회

### 📥 Request

```json
{
  "assignmentIdx": 15
}
```

### 📤 Response

```json
{
  "assignmentIdx": 15,
  "assignmentData": "{\"assignment\":{\"title\":\"중간과제\",\"description\":\"Servlet 구현\",\"maxScore\":50,\"dueDate\":\"2025-02-15T23:59:59\",\"status\":\"ACTIVE\",\"createdAt\":\"2025-02-01T10:00:00\"},\"submissions\":[]}"
}
```

**⚠️ 주의**: `assignmentData`는 **문자열로 반환**되므로 프론트엔드에서 `JSON.parse()` 필요

---

## 3️⃣ 과제 생성 (교수)

### 📥 Request

```json
{
  "lecSerial": "ETH201",
  "title": "중간과제",
  "description": "Servlet 구현",
  "maxScore": 50,
  "dueDate": "2025-02-15"
}
```

**⚠️ 필드명 확인**: 실제 컨트롤러 코드에서는 `description`, `maxScore`를 사용합니다 (taskDescription, taskScore가 아님)

### 📤 Response

```json
{
  "success": true,
  "message": "과제가 생성되었습니다.",
  "assignmentIdx": 15
}
```

**⚠️ 응답 필드**: `taskIdx`가 아닌 `assignmentIdx`를 반환합니다

### 🗄️ DB 변화

**ASSIGNMENT_EXTENDED_TBL 새 레코드 생성**:

| 컬럼 | 값 |
|------|-----|
| ASSIGNMENT_IDX | 15 (자동 생성) |
| LEC_IDX | 42 |
| ASSIGNMENT_DATA | JSON (아래 참조) |

**ASSIGNMENT_DATA JSON 구조**:
```json
{
  "assignment": {
    "title": "중간과제",
    "description": "Servlet 구현",
    "maxScore": 50,
    "dueDate": "2025-02-15T23:59:59",
    "status": "ACTIVE",
    "createdAt": "2025-02-01T10:00:00"
  },
  "submissions": []
}
```

**모든 수강생의 ENROLLMENT_DATA 업데이트**:

```json
{
  "gradeConfig": {
    "assignmentTotalScore": 157
  },
  "grade": {
    "assignments": [
      {
        "assignmentIdx": 15,
        "title": "중간과제",
        "score": null,
        "maxScore": 50
      }
    ]
  }
}
```

**⚠️ 참고**: 실제 구현에서 과제 생성 시 모든 수강생의 ENROLLMENT_DATA를 자동 업데이트하는지 확인 필요

---

## 4️⃣ 과제 채점 (교수)

### 📥 채점 Request

```json
{
  "studentIdx": 1,
  "score": 45,
  "feedback": "잘했습니다."
}
```

**⚠️ 주의**: 
- Path variable로 `assignmentIdx` 전달 (URL: `/api/assignments/{assignmentIdx}/grade`)
- Request body에는 `studentIdx` 사용 (enrollmentIdx 아님!)

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
        "assignmentIdx": 15,
        "title": "중간과제",
        "score": 45,
        "maxScore": 50,
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
[과제 목록 조회]
사용자 → API: 과제 목록 요청 (lecSerial, page, size)
API → DB: ASSIGNMENT_EXTENDED_TBL 조회
API → 사용자: assignmentData (JSON 문자열) 배열 반환

[과제 상세 조회]
사용자 → API: 과제 상세 요청 (assignmentIdx)
API → DB: ASSIGNMENT_EXTENDED_TBL 조회
API → 사용자: assignmentData (JSON 문자열) 반환

[과제 생성]
교수 → API: 과제 생성 (50점)
API → DB: ASSIGNMENT_EXTENDED_TBL INSERT
API → DB: 모든 수강생 ENROLLMENT_DATA.assignments 추가
API → DB: gradeConfig.assignmentTotalScore 업데이트
API → 교수: 생성 완료

[과제 채점]
교수 → API: 45점 부여 (studentIdx)
API → DB: ENROLLMENT_DATA.assignments[].score 업데이트
API → 이벤트: GradeUpdateEvent 발행
이벤트 → API: 전체 성적 재계산
API → DB: grade.total 업데이트
API → 교수: 채점 완료
```

---

## 💡 주요 로직

### assignmentTotalScore 자동 계산

```javascript
// ⚠️ 주의: ASSIGNMENT_EXTENDED_TBL은 ASSIGNMENT_DATA(JSON)만 저장
// 실제로는 각 과제의 JSON에서 maxScore를 추출하여 합산해야 함
```

- 과제 생성 시 모든 수강생의 `gradeConfig.assignmentTotalScore` 자동 업데이트 (구현 확인 필요)

### 전체 점수 자동 계산

- **출석 점수**: 75.5점 (출석 50회 × 1.5점)
- **과제 점수**: 45점 (중간과제 채점)
- **합계**: 120.5점
- **퍼센테이지**: 120.5 ÷ 277 × 100 = 43.5%

---

## 📋 다음 단계

채점 완료 후 학생/교수 모두 **성적 조회** 가능합니다.

→ [05. 성적 조회 API](./05_성적조회_API.md)
