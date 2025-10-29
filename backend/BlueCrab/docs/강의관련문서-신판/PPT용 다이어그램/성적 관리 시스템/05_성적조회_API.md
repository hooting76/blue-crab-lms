# 05. 성적 조회 API

> 학생/교수가 현재 성적을 조회하는 API

---

## 📌 기본 정보

### 학생용 성적 조회

- **엔드포인트**: `POST /api/enrollments/grade-info`
- **Action**: `get-grade`
- **권한**: 학생 (본인만 조회)

### 교수용 개별 학생 성적 조회

- **엔드포인트**: `POST /api/enrollments/grade-info`
- **Action**: `professor-view`
- **권한**: 교수

### 전체 성적 목록 조회

- **엔드포인트**: `POST /api/enrollments/grade-list`
- **Action**: `list-all`
- **권한**: 교수 (해당 강의 전체 수강생)

**⚠️ 중요**: 
- 성적 데이터는 **ENROLLMENT_EXTENDED_TBL.ENROLLMENT_DATA** JSON 필드에 저장됩니다
- 모든 성적 정보(출석, 과제, 총점, 등급)는 JSON 내부에 구조화되어 있습니다

---

## 1️⃣ 학생용 성적 조회

### 📥 Request

```json
{
  "action": "get-grade",
  "lecSerial": "ETH201",
  "studentIdx": 33
}
```

**⚠️ 주의**: `lecSerial` + `studentIdx` 조합으로 조회합니다 (enrollmentIdx 아님!)

### 📤 Response

```json
{
  "success": true,
  "data": {
    "studentName": "김철수",
    "lecName": "블록체인 기초",
    "lecSerial": "ETH201",
    "gradeConfig": {
      "attendanceMaxScore": 120,
      "assignmentTotalScore": 157,
      "totalMaxScore": 277
    },
    "attendance": {
      "summary": {
        "presentCount": 50,
        "lateCount": 5,
        "absentCount": 2,
        "pendingCount": 0
      }
    },
    "grade": {
      "attendanceScore": {
        "currentScore": 75.5,
        "latePenalty": -3.75,
        "presentCount": 50,
        "lateCount": 5,
        "absentCount": 2
      },
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
}
```

---

## 2️⃣ 교수용 개별 학생 성적 조회

### 📥 Request

```json
{
  "action": "professor-view",
  "lecSerial": "ETH201",
  "studentIdx": 33
}
```

**⚠️ 주의**: 
- `professorIdx`는 JWT 토큰에서 자동으로 추출됩니다
- 교수는 자신이 담당하는 강의의 학생 성적만 조회 가능

### 📤 Response

```json
{
  "success": true,
  "data": {
    "studentName": "김철수",
    "studentIdx": 33,
    "lecName": "블록체인 기초",
    "lecSerial": "ETH201",
    "totalScore": 120.5,
    "percentage": 43.5,
    "grade": null,
    "classStats": {
      "average": 65.3,
      "highest": 98.5,
      "lowest": 23.0
    }
  }
}
```

---

## 3️⃣ 전체 성적 목록 조회

### 📥 Request (교수)

```json
{
  "action": "list-all",
  "lecSerial": "ETH201",
  "page": 0,
  "size": 20,
  "sortBy": "percentage",
  "sortOrder": "desc"
}
```

**필드 설명**:
- `sortBy`: 정렬 기준 (`percentage` | `name` | `studentId`)
- `sortOrder`: 정렬 순서 (`desc` | `asc`)

### 📤 Response (교수)

```json
{
  "success": true,
  "data": {
    "content": [
      {
        "enrollmentIdx": 1,
        "studentIdx": 33,
        "studentName": "김철수",
        "percentage": 71.5,
        "grade": null
      },
      {
        "enrollmentIdx": 2,
        "studentIdx": 34,
        "studentName": "이영희",
        "percentage": 43.5,
        "grade": null
      }
    ],
    "totalElements": 42,
    "totalPages": 3,
    "number": 0
  }
}
```

**⚠️ 주의**: 
- 기본 정렬은 `percentage` 내림차순 (높은 점수 우선)
- 페이징 처리 지원

---

## 🔄 시퀀스 다이어그램

```plaintext
[학생 조회]
학생 → API: 내 성적 조회 (action: get-grade, lecSerial, studentIdx)
API → DB: ENROLLMENT_EXTENDED_TBL 조회 (lecSerial + studentIdx)
DB → API: ENROLLMENT_DATA 반환
API → 학생: 출석/과제/총점 상세 정보

[교수 개별 조회]
교수 → API: 학생 성적 조회 (action: professor-view, lecSerial, studentIdx)
API → JWT: professorIdx 추출
API → DB: ENROLLMENT_EXTENDED_TBL 조회 (lecSerial + studentIdx)
API → DB: 반 통계 계산 (같은 강의 전체 평균)
DB → API: ENROLLMENT_DATA + classStats 반환
API → 교수: 학생 성적 + 반 통계

[전체 목록 조회]
교수 → API: 전체 성적 목록 (action: list-all, lecSerial, sortBy, sortOrder)
API → DB: ENROLLMENT_EXTENDED_TBL 조회 (LEC_IDX + 정렬)
DB → API: 전체 수강생 ENROLLMENT_DATA 반환
API → 교수: 페이징된 성적 목록
```

---

## 💡 주요 차이점

### 학생 View (action: get-grade)

- **엔드포인트**: `POST /api/enrollments/grade-info`
- **상세 정보**: 출석 세부 내역, 과제별 점수, 피드백
- **제한**: 본인 성적만 조회 가능 (lecSerial + studentIdx)
- **letterGrade**: 최종 등급 배정 전까지 `null`

### 교수 개별 View (action: professor-view)

- **엔드포인트**: `POST /api/enrollments/grade-info`
- **추가 정보**: 반 통계 (평균, 최고점, 최저점)
- **권한**: 담당 강의 학생만 조회 가능
- **JWT**: professorIdx 자동 추출

### 교수 전체 목록 (action: list-all)

- **엔드포인트**: `POST /api/enrollments/grade-list`
- **전체 목록**: 모든 수강생의 성적 요약
- **간략 정보**: 학생명, 백분율, 등급만 표시
- **정렬/페이징**: sortBy, sortOrder, page, size 지원
- **활용**: 최종 등급 배정을 위한 전체 분포 확인

---

## 📊 성적 구성 예시

| 항목 | 배점 | 실제 점수 |
|------|------|----------|
| 출석 | 120점 | 75.5점 |
| 과제 | 157점 | 45점 |
| **합계** | **277점** | **120.5점 (43.5%)** |
| 등급 | - | null (미배정) |

---

## 📋 다음 단계

학기 종료 후 교수가 **최종 등급 배정**을 진행합니다.

→ [06. 최종 등급 배정 API](./06_최종등급배정_API.md)
