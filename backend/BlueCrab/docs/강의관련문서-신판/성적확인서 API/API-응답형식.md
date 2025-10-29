# API 응답 형식

성적확인서 API의 요청/응답 형식 및 필드 설명

---

## 📤 성공 응답 예시

### HTTP 200 OK

```json
{
  "status": "success",
  "message": "성적확인서 조회 성공",
  "data": {
    "student": {
      "studentIdx": 2,
      "studentCode": "202500106114",
      "name": "박대현",
      "admissionYear": 2025,
      "currentGrade": 1
    },
    "courses": [
      {
        "lecIdx": 101,
        "lecSerial": "CS101",
        "lecTitle": "자료구조",
        "credits": 3,
        "professorName": "김교수",
        "year": 2025,
        "semester": 1,
        "totalScore": 281.76,
        "maxScore": 294.0,
        "percentage": 95.84,
        "letterGrade": "A",
        "gpa": 4.0,
        "rank": 3,
        "totalStudents": 25,
        "status": "COMPLETED",
        "attendanceScore": 65.2,
        "attendanceMaxScore": 67.0,
        "attendanceRate": 97,
        "assignmentScore": 216.56,
        "assignmentMaxScore": 227.0
      },
      {
        "lecIdx": 102,
        "lecSerial": "MATH201",
        "lecTitle": "선형대수학",
        "credits": 3,
        "professorName": "이교수",
        "year": 2025,
        "semester": 1,
        "totalScore": 165.4,
        "maxScore": 294.0,
        "percentage": 56.26,
        "letterGrade": "F",
        "gpa": 0.0,
        "rank": 23,
        "totalStudents": 25,
        "status": "FAILED",
        "attendanceScore": 45.8,
        "attendanceMaxScore": 67.0,
        "attendanceRate": 68,
        "assignmentScore": 119.6,
        "assignmentMaxScore": 227.0
      },
      {
        "lecIdx": 103,
        "lecSerial": "ENG101",
        "lecTitle": "영어회화",
        "credits": 2,
        "professorName": "최교수",
        "year": 2025,
        "semester": 1,
        "totalScore": 198.5,
        "maxScore": 294.0,
        "percentage": 67.52,
        "letterGrade": "진행중",
        "gpa": 0.0,
        "rank": null,
        "totalStudents": null,
        "status": "IN_PROGRESS",
        "attendanceScore": 52.3,
        "attendanceMaxScore": 67.0,
        "attendanceRate": 78,
        "assignmentScore": 146.2,
        "assignmentMaxScore": 227.0
      }
    ],
    "semesterSummaries": {
      "2025-1": {
        "semesterKey": "2025-1",
        "year": 2025,
        "semester": 1,
        "courseCount": 3,
        "earnedCredits": 3,
        "attemptedCredits": 8,
        "semesterGpa": 4.0,
        "averagePercentage": 73.21,
        "gradeACount": 1,
        "gradeBCount": 0,
        "gradeCCount": 0,
        "gradeDCount": 0,
        "gradeFCount": 1
      }
    },
    "overallSummary": {
      "totalCourses": 3,
      "totalEarnedCredits": 3,
      "totalAttemptedCredits": 8,
      "cumulativeGpa": 4.0,
      "averagePercentage": 73.21,
      "completionRate": 37.5,
      "totalGradeACount": 1,
      "totalGradeBCount": 0,
      "totalGradeCCount": 0,
      "totalGradeDCount": 0,
      "totalGradeFCount": 1
    },
    "issuedAt": "2025-10-29T14:35:22",
    "certificateNumber": "TR-202500106114-20251029143522"
  }
}
```

---

## ❌ 실패 응답 예시

### 401 Unauthorized - 인증 실패

```json
{
  "status": "error",
  "message": "인증이 필요합니다"
}
```

### 403 Forbidden - 권한 없음

```json
{
  "status": "error",
  "message": "본인의 성적만 조회할 수 있습니다"
}
```

### 404 Not Found - 학생 없음

```json
{
  "status": "error",
  "message": "학생을 찾을 수 없습니다: 99999"
}
```

### 500 Internal Server Error - 서버 오류

```json
{
  "status": "error",
  "message": "서버 오류가 발생했습니다"
}
```

---

## 📊 필드 설명

### StudentInfo

| 필드 | 타입 | 설명 | 예시 |
|------|------|------|------|
| studentIdx | Integer | 학생 IDX | 2 |
| studentCode | String | 학번 | "202500106114" |
| name | String | 이름 | "박대현" |
| admissionYear | Integer | 입학년도 | 2025 |
| currentGrade | Integer | 현재 학년 (1~4) | 1 |

### CourseDto

| 필드 | 타입 | 설명 | 예시 |
|------|------|------|------|
| lecIdx | Integer | 강의 IDX | 101 |
| lecSerial | String | 강의 코드 | "CS101" |
| lecTitle | String | 강의명 | "자료구조" |
| credits | Integer | 학점 | 3 |
| professorName | String | 교수명 | "김교수" |
| year | Integer | 년도 | 2025 |
| semester | Integer | 학기 | 1 |
| totalScore | Double | 총점 | 281.76 |
| maxScore | Double | 만점 | 294.0 |
| percentage | Double | 백분율 | 95.84 |
| letterGrade | String | 학점 등급 (A/B/C/D/F) | "A" |
| gpa | Double | GPA (4.0 만점) | 4.0 |
| rank | Integer | 등수 (확정 시에만) | 3 |
| totalStudents | Integer | 전체 학생 수 (확정 시에만) | 25 |
| status | String | 상태 | "COMPLETED" |
| attendanceScore | Double | 출석 점수 | 65.2 |
| attendanceMaxScore | Double | 출석 만점 | 67.0 |
| attendanceRate | Integer | 출석률 (%) | 97 |
| assignmentScore | Double | 과제 점수 | 216.56 |
| assignmentMaxScore | Double | 과제 만점 | 227.0 |

### SemesterSummary

| 필드 | 타입 | 설명 | 예시 |
|------|------|------|------|
| semesterKey | String | 학기 키 | "2025-1" |
| year | Integer | 년도 | 2025 |
| semester | Integer | 학기 | 1 |
| courseCount | Integer | 과목 수 | 3 |
| earnedCredits | Integer | 취득 학점 | 3 |
| attemptedCredits | Integer | 신청 학점 | 8 |
| semesterGpa | Double | 학기 GPA | 4.0 |
| averagePercentage | Double | 평균 백분율 | 73.21 |
| gradeACount | Integer | A 학점 수 | 1 |
| gradeBCount | Integer | B 학점 수 | 0 |
| gradeCCount | Integer | C 학점 수 | 0 |
| gradeDCount | Integer | D 학점 수 | 0 |
| gradeFCount | Integer | F 학점 수 | 1 |

### OverallSummary

| 필드 | 타입 | 설명 | 예시 |
|------|------|------|------|
| totalCourses | Integer | 총 과목 수 | 3 |
| totalEarnedCredits | Integer | 총 취득 학점 | 3 |
| totalAttemptedCredits | Integer | 총 신청 학점 | 8 |
| cumulativeGpa | Double | 누적 GPA | 4.0 |
| averagePercentage | Double | 평균 백분율 | 73.21 |
| completionRate | Double | 학점 취득률 (%) | 37.5 |
| totalGradeACount | Integer | 총 A 학점 수 | 1 |
| totalGradeBCount | Integer | 총 B 학점 수 | 0 |
| totalGradeCCount | Integer | 총 C 학점 수 | 0 |
| totalGradeDCount | Integer | 총 D 학점 수 | 0 |
| totalGradeFCount | Integer | 총 F 학점 수 | 1 |

---

## 🏷️ 상태 코드 (status)

| 코드 | 설명 | 조건 |
|------|------|------|
| COMPLETED | ✅ 수료 | finalized=true, letterGrade≠F |
| FAILED | ❌ 낙제 | finalized=true, letterGrade=F |
| IN_PROGRESS | ⏳ 진행중 | finalized=false, percentage≥60% |
| NOT_GRADED | ⏸️ 미확정 | finalized=false, percentage<60% |
| DROPPED | 🚫 중도포기 | 별도 플래그 필요 (미구현) |

---

## 💡 참고사항

### Null 처리
- `rank`, `totalStudents`: 성적 미확정 시 null
- `letterGrade`: "진행중", "미확정" 등 임시 값 가능

### 소수점 처리
- `gpa`: 소수점 둘째 자리까지
- `percentage`: 소수점 둘째 자리까지
- `completionRate`: 소수점 둘째 자리까지

### 날짜 형식
- `issuedAt`: ISO 8601 형식 (예: "2025-10-29T14:35:22")

### 발급 번호 형식
- `certificateNumber`: "TR-{학번}-{YYYYMMDDHHMMSS}"
- 예: "TR-202500106114-20251029143522"
