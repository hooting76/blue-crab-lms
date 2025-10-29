# 05. 성적 조회 API

> 학생/교수가 현재 성적을 조회하는 API

---

## 📌 기본 정보

### 학생용 성적 조회

- **엔드포인트**: `GET /api/enrollments/my-grade?enrollmentIdx={enrollmentIdx}`
- **권한**: 학생 (본인만 조회)

### 교수용 전체 성적 조회

- **엔드포인트**: `GET /api/enrollments/grades?lecSerial={lecSerial}`
- **권한**: 교수 (해당 강의 전체 수강생)

---

## 1️⃣ 학생용 성적 조회

### 📥 Request

```http
GET /api/enrollments/my-grade?enrollmentIdx=1
```

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
}
```

---

## 2️⃣ 교수용 전체 성적 조회

### 📥 Request

```http
GET /api/enrollments/grades?lecSerial=ETH201
```

### 📤 Response

```json
{
  "success": true,
  "data": [
    {
      "enrollmentIdx": 1,
      "studentIdx": 33,
      "studentName": "김철수",
      "grade": {
        "attendanceScore": {
          "currentScore": 75.5
        },
        "total": {
          "currentScore": 120.5,
          "percentage": 43.5
        },
        "letterGrade": null
      }
    },
    {
      "enrollmentIdx": 2,
      "studentIdx": 34,
      "studentName": "이영희",
      "grade": {
        "attendanceScore": {
          "currentScore": 105.0
        },
        "total": {
          "currentScore": 198.0,
          "percentage": 71.5
        },
        "letterGrade": null
      }
    }
  ]
}
```

---

## 🔄 시퀀스 다이어그램

```plaintext
[학생 조회]
학생 → API: 내 성적 조회
API → DB: ENROLLMENT_EXTENDED_TBL WHERE ENROLLMENT_IDX = 1
DB → API: ENROLLMENT_DATA 반환
API → 학생: 출석/과제/총점 정보

[교수 조회]
교수 → API: 전체 성적 조회
API → DB: ENROLLMENT_EXTENDED_TBL WHERE LEC_IDX = 42
DB → API: 42명 ENROLLMENT_DATA 반환
API → 교수: 전체 수강생 성적 목록
```

---

## 💡 주요 차이점

### 학생 View

- **상세 정보**: 출석 세부 내역, 과제별 점수, 피드백
- **제한**: 본인 성적만 조회 가능
- **letterGrade**: 최종 등급 배정 전까지 `null`

### 교수 View

- **전체 목록**: 모든 수강생의 성적 요약
- **간략 정보**: 출석 점수, 총점, 퍼센테이지
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
