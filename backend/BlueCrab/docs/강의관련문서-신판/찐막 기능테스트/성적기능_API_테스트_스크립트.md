# 성적 기능 API 테스트 스크립트

**작성일**: 2025-10-27  
**목적**: 성적 API 실제 테스트를 위한 요청 예시

---

## 🎯 테스트 대상 강의 및 학생

### 테스트 데이터 확인 필요
```sql
-- 1. 테스트용 강의 확인 (LEC_TBL)
SELECT LEC_IDX, LEC_SERIAL, LEC_TIT, LEC_PROF 
FROM LEC_TBL 
WHERE LEC_SERIAL = 'CS284' OR LEC_IDX = 6;

-- 2. 수강생 확인 (ENROLLMENT_EXTENDED_TBL)
SELECT e.ENROLLMENT_IDX, e.LEC_IDX, e.STUDENT_IDX, u.USER_NAME, u.USER_CODE
FROM ENROLLMENT_EXTENDED_TBL e
JOIN USER_TBL u ON e.STUDENT_IDX = u.USER_IDX
WHERE e.LEC_IDX = 6
LIMIT 5;

-- 3. 현재 ENROLLMENT_DATA 상태 확인
SELECT ENROLLMENT_IDX, STUDENT_IDX, 
       CASE 
           WHEN ENROLLMENT_DATA IS NULL OR ENROLLMENT_DATA = '' THEN 'NULL/EMPTY'
           WHEN ENROLLMENT_DATA = '{}' THEN 'EMPTY_JSON'
           WHEN ENROLLMENT_DATA LIKE '%attendance%' AND ENROLLMENT_DATA NOT LIKE '%grade%' THEN 'ATTENDANCE_ONLY'
           WHEN ENROLLMENT_DATA LIKE '%grade%' THEN 'HAS_GRADE'
           ELSE 'OTHER'
       END AS DATA_STATUS,
       LENGTH(ENROLLMENT_DATA) AS DATA_LENGTH
FROM ENROLLMENT_EXTENDED_TBL
WHERE LEC_IDX = 6;

-- 4. 과제 확인
SELECT a.ASSIGNMENT_IDX, a.LEC_IDX, 
       JSON_EXTRACT(a.ASSIGNMENT_DATA, '$.assignment.title') AS TITLE,
       JSON_EXTRACT(a.ASSIGNMENT_DATA, '$.assignment.maxScore') AS MAX_SCORE,
       JSON_LENGTH(JSON_EXTRACT(a.ASSIGNMENT_DATA, '$.submissions')) AS SUBMISSION_COUNT
FROM ASSIGNMENT_EXTENDED_TBL a
WHERE a.LEC_IDX = 6;
```

---

## 📝 API 테스트 순서

### ✅ STEP 1: 성적 구성 설정 (필수)

**엔드포인트**: `POST http://localhost:8080/api/enrollments/grade-config`

**헤더**:
```
Content-Type: application/json
Authorization: Bearer {교수_토큰}
```

**Request Body**:
```json
{
  "action": "set-config",
  "lecSerial": "CS284",
  "attendanceMaxScore": 20,
  "assignmentTotalScore": 50,
  "latePenaltyPerSession": 0.5,
  "gradeDistribution": {
    "A": 30,
    "B": 40,
    "C": 20,
    "D": 10
  }
}
```

**예상 응답**:
```json
{
  "success": true,
  "message": "성적 구성이 설정되었습니다.",
  "data": {
    "lecIdx": 6,
    "gradeConfig": {
      "attendanceMaxScore": 20,
      "assignmentTotalScore": 50,
      "latePenaltyPerSession": 0.5,
      "gradeDistribution": {
        "A": 30,
        "B": 40,
        "C": 20,
        "D": 10
      },
      "totalMaxScore": 70,
      "configuredAt": "2025-10-27T..."
    },
    "updatedEnrollments": 5,
    "failedEnrollments": 0
  }
}
```

**검증 SQL**:
```sql
-- gradeConfig가 추가되었는지 확인
SELECT ENROLLMENT_IDX, STUDENT_IDX,
       JSON_EXTRACT(ENROLLMENT_DATA, '$.gradeConfig.attendanceMaxScore') AS ATTENDANCE_MAX,
       JSON_EXTRACT(ENROLLMENT_DATA, '$.gradeConfig.assignmentTotalScore') AS ASSIGNMENT_TOTAL,
       JSON_EXTRACT(ENROLLMENT_DATA, '$.gradeConfig.latePenaltyPerSession') AS LATE_PENALTY
FROM ENROLLMENT_EXTENDED_TBL
WHERE LEC_IDX = 6;
```

---

### ✅ STEP 2: 개별 학생 성적 조회 (데이터 없는 경우)

**엔드포인트**: `POST http://localhost:8080/api/enrollments/grade-info`

**Request Body**:
```json
{
  "action": "get-grade",
  "lecSerial": "CS284",
  "studentIdx": 6
}
```

**예상 응답** (출석/과제 데이터 없을 때):
```json
{
  "success": true,
  "message": "성적 조회가 완료되었습니다.",
  "data": {
    "studentIdx": 6,
    "lecIdx": 6,
    "lecSerial": "CS284",
    "grade": {
      "attendanceScore": {
        "maxScore": 20.0,
        "currentScore": 0.0,
        "percentage": 0.00,
        "presentCount": 0,
        "lateCount": 0,
        "absentCount": 0,
        "attendanceRate": 0
      },
      "assignments": [],
      "total": {
        "score": 0.0,
        "maxScore": 20.0,
        "percentage": 0.00
      }
    }
  }
}
```

**검증 SQL**:
```sql
-- grade 필드가 추가되었는지 확인
SELECT ENROLLMENT_IDX, STUDENT_IDX,
       JSON_EXTRACT(ENROLLMENT_DATA, '$.grade.total.score') AS TOTAL_SCORE,
       JSON_EXTRACT(ENROLLMENT_DATA, '$.grade.total.maxScore') AS TOTAL_MAX,
       JSON_EXTRACT(ENROLLMENT_DATA, '$.grade.total.percentage') AS PERCENTAGE
FROM ENROLLMENT_EXTENDED_TBL
WHERE LEC_IDX = 6 AND STUDENT_IDX = 6;
```

---

### ✅ STEP 3: 출석 데이터 추가 후 재조회

**시나리오**: 출석 데이터를 수동으로 추가 (또는 출석 API로 처리)

**테스트용 출석 데이터 추가 SQL**:
```sql
-- ENROLLMENT_IDX = 1 (LEC_IDX=6, STUDENT_IDX=6)에 출석 데이터 추가
UPDATE ENROLLMENT_EXTENDED_TBL
SET ENROLLMENT_DATA = JSON_SET(
  COALESCE(ENROLLMENT_DATA, '{}'),
  '$.attendance.summary.attended', 7,
  '$.attendance.summary.late', 2,
  '$.attendance.summary.absent', 1,
  '$.attendance.summary.totalSessions', 10,
  '$.attendance.summary.attendanceRate', 70.0,
  '$.attendance.summary.updatedAt', NOW()
)
WHERE LEC_IDX = 6 AND STUDENT_IDX = 6;
```

**재조회 Request**:
```json
{
  "action": "get-grade",
  "lecSerial": "CS284",
  "studentIdx": 6
}
```

**예상 응답** (출석 데이터 있을 때):
```json
{
  "success": true,
  "message": "성적 조회가 완료되었습니다.",
  "data": {
    "grade": {
      "attendanceScore": {
        "maxScore": 20.0,
        "currentScore": 14.0,  // 7출석 (1점/회) * 20점 만점
        "percentage": 70.00,
        "presentCount": 7,
        "lateCount": 2,
        "absentCount": 1,
        "latePenalty": 1.0,  // 지각 2회 * 0.5점 감점
        "attendanceRate": 70
      },
      "assignments": [],
      "total": {
        "score": 14.0,
        "maxScore": 20.0,
        "percentage": 70.00
      }
    }
  }
}
```

---

### ✅ STEP 4: 과제 제출 데이터 추가 후 재조회

**테스트용 과제 제출 데이터 추가 SQL**:
```sql
-- ASSIGNMENT_IDX = 1에 학생 제출물 추가
UPDATE ASSIGNMENT_EXTENDED_TBL
SET ASSIGNMENT_DATA = JSON_INSERT(
  ASSIGNMENT_DATA,
  '$.submissions[0]', JSON_OBJECT(
    'studentIdx', 6,
    'studentName', '집갈래',
    'submittedAt', NOW(),
    'content', '테스트 제출물',
    'score', 20,
    'feedback', '잘했습니다'
  )
)
WHERE ASSIGNMENT_IDX = 1 AND LEC_IDX = 6;
```

**재조회 Request**:
```json
{
  "action": "get-grade",
  "lecSerial": "CS284",
  "studentIdx": 6
}
```

**예상 응답** (출석+과제 모두 있을 때):
```json
{
  "success": true,
  "message": "성적 조회가 완료되었습니다.",
  "data": {
    "grade": {
      "attendanceScore": {
        "currentScore": 14.0,
        "maxScore": 20.0,
        "percentage": 70.00,
        "latePenalty": 1.0
      },
      "assignments": [
        {
          "assignmentIdx": 1,
          "name": "식인대게의 생태조사",
          "score": 20.0,
          "maxScore": 25.0,
          "percentage": 80.00
        }
      ],
      "total": {
        "score": 34.0,  // 14 (출석) + 20 (과제)
        "maxScore": 45.0,  // 20 (출석) + 25 (과제)
        "percentage": 75.56
      }
    }
  }
}
```

---

### ✅ STEP 5: 전체 수강생 성적 목록 조회

**엔드포인트**: `POST http://localhost:8080/api/enrollments/grade-list`

**Request Body**:
```json
{
  "action": "list-all",
  "lecSerial": "CS284",
  "professorIdx": 6
}
```

**예상 응답**:
```json
{
  "success": true,
  "message": "전체 수강생 성적 목록이 조회되었습니다.",
  "data": {
    "lecIdx": 6,
    "lecSerial": "CS284",
    "students": [
      {
        "rank": 1,
        "studentIdx": 10,
        "studentName": "김철수",
        "studentCode": "240105050",
        "grade": {
          "total": {
            "score": 65.0,
            "maxScore": 70.0,
            "percentage": 92.86
          }
        }
      },
      {
        "rank": 2,
        "studentIdx": 6,
        "studentName": "집갈래",
        "studentCode": "240105045",
        "grade": {
          "total": {
            "score": 34.0,
            "maxScore": 45.0,
            "percentage": 75.56
          }
        }
      }
    ],
    "statistics": {
      "totalStudents": 5,
      "averageScore": 68.5,
      "highestScore": 92.86,
      "lowestScore": 45.20
    }
  }
}
```

---

## 🧪 테스트 체크포인트

### 1. 출석 점수 계산
- [ ] 출석만 있을 때: `(출석수 / 총회차) * maxScore`
- [ ] 지각 감점 적용: `currentScore - (지각수 * latePenaltyPerSession)`
- [ ] 0점 미만 방지: `Math.max(0.0, adjustedScore)`
- [ ] 소수점 반올림: `Math.round(score * 100.0) / 100.0`

### 2. 과제 점수 계산
- [ ] 제출물의 score 합산
- [ ] 미제출 과제 처리 (0점)
- [ ] maxScore 정확성

### 3. 총점 계산
- [ ] 출석 + 과제 합산
- [ ] 백분율 계산: `(totalScore / totalMax) * 100`
- [ ] 소수점 둘째자리 반올림

### 4. JSON 업데이트
- [ ] `grade.attendanceScore` 필드 추가
- [ ] `grade.assignments` 배열 추가
- [ ] `grade.total` 필드 추가
- [ ] 기존 `attendance` 데이터 보존

### 5. 에러 처리
- [ ] 존재하지 않는 강의
- [ ] 존재하지 않는 학생
- [ ] 권한 없는 접근
- [ ] null/empty 데이터 처리

---

## 📊 DB 초기화 여부 결정

### 옵션 A: 전체 초기화 (Clean Start)
```sql
-- 모든 ENROLLMENT_DATA를 빈 JSON으로 초기화
UPDATE ENROLLMENT_EXTENDED_TBL
SET ENROLLMENT_DATA = '{}'
WHERE LEC_IDX = 6;
```

**장점**: 깨끗한 테스트 환경  
**단점**: 기존 출석 데이터 손실

---

### 옵션 B: 기존 데이터 유지 (Progressive Update)
- 기존 `attendance` 필드 보존
- `grade`, `gradeConfig` 필드만 추가
- 점진적 업데이트 방식

**장점**: 출석 데이터 보존, 실제 운영 환경과 유사  
**단점**: 혼합된 상태로 테스트

---

## 🚀 권장 테스트 순서

1. ✅ **DB 백업** (현재 상태 보존)
2. ✅ **테스트 데이터 확인** (SQL 실행)
3. ✅ **성적 구성 설정** (STEP 1)
4. ✅ **개별 성적 조회** (STEP 2-4, 점진적)
5. ✅ **전체 성적 목록** (STEP 5)
6. ✅ **DB 검증** (각 단계마다 SQL 확인)
7. ✅ **에러 케이스 테스트**

---

## 💡 다음 작업

- [ ] Thunder Client / Postman 컬렉션 생성
- [ ] 테스트 자동화 스크립트 작성
- [ ] 성능 테스트 (다수 수강생)
- [ ] 프론트엔드 연동 테스트
