# 성적 관리 API 문서

## 📋 개요

성적 구성 설정, 조회, 계산, 최종 등급 배정 기능을 제공하는 API 문서입니다.

**컨트롤러**: `EnrollmentController.java` (성적 관련 엔드포인트 통합)  
**서비스**: `GradeManagementService.java`, `GradeCalculationService.java`, `GradeFinalizer.java`  
**기본 경로**: `/api/enrollments`  
**관련 DB 테이블**: `ENROLLMENT_EXTENDED_TBL`, `LEC_TBL`, `ASSIGNMENT_EXTENDED_TBL`

---

## 🔍 API 목록

### 1. 성적 구성 설정

**엔드포인트**: `POST /api/enrollments/grade-config`

**권한**: 교수 (담당 강의만)

**설명**: 강의의 성적 구성 비율 및 등급 분포 기준을 설정합니다.

#### Request Body
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

**필드 설명**:
- `action`: "set-config" (고정값)
- `lecSerial`: 강의 코드
- `attendanceMaxScore`: 출석 배점 (기본값: 20)
- `assignmentTotalScore`: 과제 총점 (기본값: 50)
- `latePenaltyPerSession`: 지각당 감점 (기본값: 0.0)
- `gradeDistribution`: 등급 분포 비율 (A/B/C/D %)

#### 응답 예시
```json
{
  "success": true,
  "message": "성적 구성이 설정되었습니다.",
  "data": {
    "lecIdx": 1,
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
      "configuredAt": "2025-10-26T10:30:00"
    },
    "updatedEnrollments": 23,
    "failedEnrollments": 0
  }
}
```

---

### 2. 학생 성적 조회

**엔드포인트**: `POST /api/enrollments/grade-info`

**권한**: 학생 (본인만), 교수 (담당 강의)

**설명**: 특정 학생의 성적 정보를 조회합니다.

#### Request Body
```json
{
  "action": "get-grade",
  "lecSerial": "CS284",
  "studentIdx": 6
}
```

#### 응답 예시
```json
{
  "success": true,
  "message": "성적 조회가 완료되었습니다.",
  "data": {
    "studentIdx": 6,
    "lecIdx": 1,
    "lecSerial": "CS284",
    "grade": {
      "attendance": {
        "score": 18.5,
        "maxScore": 20,
        "percentage": 92.5,
        "details": "1출2출3결4지5출6출7출8출"
      },
      "assignment": {
        "score": 45,
        "maxScore": 50,
        "percentage": 90.0,
        "submissions": [
          {
            "assignIdx": 1,
            "score": 95,
            "maxScore": 100
          },
          {
            "assignIdx": 2,
            "score": 85,
            "maxScore": 100
          }
        ]
      },
      "total": {
        "score": 63.5,
        "maxScore": 70,
        "percentage": 90.71,
        "letterGrade": "A+"
      }
    }
  }
}
```

---

### 3. 교수용 성적 조회 (통계 포함)

**엔드포인트**: `POST /api/enrollments/grade-info`

**권한**: 교수 (담당 강의만)

**설명**: 학생 성적 + 추가 통계(순위, 평균) 조회

**⚠️ 참고**: `professorIdx`는 JWT 토큰에서 자동으로 추출되므로 요청 본문에 포함할 필요가 없습니다.

#### Request Body
```json
{
  "action": "professor-view",
  "lecSerial": "CS284",
  "studentIdx": 6
}
```

**필드 설명**:
- `professorIdx`: ~~요청 본문에 포함 불필요~~ → JWT 토큰에서 자동 추출
- 인증된 교수의 `USER_IDX`가 자동으로 사용됩니다

#### 응답 예시
```json
{
  "success": true,
  "message": "교수용 성적 조회가 완료되었습니다.",
  "data": {
    "studentIdx": 6,
    "lecIdx": 1,
    "grade": {
      "attendance": { /* 동일 */ },
      "assignment": { /* 동일 */ },
      "total": {
        "score": 63.5,
        "maxScore": 70,
        "percentage": 90.71,
        "letterGrade": "A+"
      }
    },
    "professorView": true,
    "statistics": {
      "rank": 2,
      "totalStudents": 23,
      "classAverage": 78.5
    }
  }
}
```

---

### 4. 전체 수강생 성적 목록

**엔드포인트**: `POST /api/enrollments/grade-list`

**권한**: 교수 (담당 강의만)

**설명**: 강의의 전체 수강생 성적 목록을 조회합니다.

#### Request Body
```json
{
  "action": "list-all",
  "lecSerial": "CS284",
  "page": 0,
  "size": 50,
  "sortBy": "percentage",
  "sortOrder": "desc"
}
```

**sortBy 옵션**:
- `percentage`: 성적 순
- `name`: 이름 순
- `studentId`: 학번 순

**sortOrder**:
- `desc`: 내림차순
- `asc`: 오름차순

#### 응답 예시
```json
{
  "success": true,
  "message": "성적 목록 조회가 완료되었습니다.",
  "data": {
    "content": [
      {
        "studentIdx": 6,
        "studentCode": "240105045",
        "studentName": "집갈래",
        "attendanceScore": 18.5,
        "assignmentScore": 45,
        "totalScore": 63.5,
        "totalPercentage": 90.71,
        "letterGrade": "A+",
        "rank": 2
      },
      {
        "studentIdx": 7,
        "studentCode": "240105046",
        "studentName": "홍길동",
        "attendanceScore": 20,
        "assignmentScore": 42,
        "totalScore": 62,
        "totalPercentage": 88.57,
        "letterGrade": "A0",
        "rank": 3
      }
    ],
    "totalElements": 23,
    "totalPages": 1,
    "classStatistics": {
      "average": 78.5,
      "highest": 95.2,
      "lowest": 45.3
    }
  }
}
```

---

### 5. 성적 수동 수정

**엔드포인트**: `PUT /api/enrollments/{enrollmentIdx}/grade`

**권한**: 교수 (담당 강의만)

**설명**: 특정 학생의 성적을 수동으로 수정합니다.

#### Request Body
```json
{
  "grade": "B+",
  "score": 82.5
}
```

#### 응답 예시
```json
{
  "enrollmentIdx": 1,
  "lecIdx": 6,
  "studentIdx": 6,
  "enrollmentData": "{\"grade\":{\"total\":{\"letterGrade\":\"B+\",\"score\":82.5}}}"
}
```

---

### 6. 최종 등급 배정

**엔드포인트**: `POST /api/enrollments/grade-finalize`

**권한**: 교수 (담당 강의만)

**설명**: 상대평가 기준에 따라 최종 등급을 일괄 배정합니다.

#### Request Body
```json
{
  "action": "finalize",
  "lecSerial": "CS284",
  "passingThreshold": 60.0,
  "gradeDistribution": {
    "A": 30,
    "B": 40,
    "C": 20,
    "D": 10
  }
}
```

**필드 설명**:
- `passingThreshold`: 통과 기준 점수 (기본값: 60.0)
- `gradeDistribution`: 등급 분포 (선택사항, 기본값은 설정값 사용)

#### 응답 예시
```json
{
  "success": true,
  "message": "최종 등급 배정이 완료되었습니다.",
  "data": {
    "lecIdx": 1,
    "totalStudents": 23,
    "gradeDistribution": {
      "A+": 3,
      "A0": 4,
      "B+": 5,
      "B0": 4,
      "C+": 3,
      "C0": 2,
      "D+": 1,
      "D0": 1,
      "F": 0
    },
    "passRate": 100.0,
    "finalizedAt": "2025-10-26T15:30:00"
  }
}
```

---

## 📊 성적 계산 로직

### 1. 출석 점수 계산
```
출석 점수 = (출석 인정 회차 / 전체 회차) × attendanceMaxScore
- 출석 인정: "출", "지", "조"
- 지각 감점: latePenaltyPerSession × 지각 횟수
```

**예시**:
- 전체 16회차, 출석 12회, 지각 2회, 결석 2회
- attendanceMaxScore = 20, latePenaltyPerSession = 0.5
- 출석 점수 = (14/16) × 20 - (2 × 0.5) = 17.5 - 1 = 16.5점

### 2. 과제 점수 계산
```
과제 점수 = Σ(각 과제 점수) / Σ(각 과제 배점) × assignmentTotalScore
```

**예시**:
- 과제1: 95/100, 과제2: 85/100
- assignmentTotalScore = 50
- 과제 점수 = (95+85)/(100+100) × 50 = 45점

### 3. 총점 계산
```
총점 = 출석 점수 + 과제 점수
백분율 = (총점 / totalMaxScore) × 100
```

### 4. 등급 배정 (상대평가)

**방법**: 백분율 순으로 정렬 후 등급 분포에 따라 배정

**예시** (A=30%, B=40%, C=20%, D=10%, 총 20명):
- A+ (상위 15%): 3명
- A0 (상위 15~30%): 3명
- B+ (상위 30~50%): 4명
- B0 (상위 50~70%): 4명
- C+ (상위 70~80%): 2명
- C0 (상위 80~90%): 2명
- D+ (상위 90~95%): 1명
- D0 (상위 95~100%): 1명
- F (60점 미만): 0명

---

## 📈 DTO 구조

### GradeConfig (JSON)
```json
{
  "attendanceMaxScore": 20,
  "assignmentTotalScore": 50,
  "latePenaltyPerSession": 0.5,
  "totalMaxScore": 70,
  "gradeDistribution": {
    "A": 30,
    "B": 40,
    "C": 20,
    "D": 10
  },
  "configuredAt": "2025-10-26T10:30:00"
}
```

### Grade (JSON - enrollmentData 내)
```json
{
  "attendance": {
    "score": 18.5,
    "maxScore": 20,
    "percentage": 92.5,
    "details": "1출2출3결4지5출6출7출8출"
  },
  "assignment": {
    "score": 45,
    "maxScore": 50,
    "percentage": 90.0,
    "submissions": [...]
  },
  "total": {
    "score": 63.5,
    "maxScore": 70,
    "percentage": 90.71,
    "letterGrade": "A+",
    "rank": 2
  }
}
```

---

## 🔗 관련 테이블

### ENROLLMENT_EXTENDED_TBL
**enrollmentData 내 성적 데이터**:
```json
{
  "gradeConfig": { /* 성적 구성 설정 */ },
  "grade": { /* 계산된 성적 정보 */ },
  "attendance": "1출2출3결4지...",
  "attendanceRate": "14/16"
}
```

---

## 🔄 이벤트

### GradeUpdateEvent
성적 변경 시 자동으로 재계산을 트리거하는 이벤트

**발행 상황**:
- 출석 업데이트 시
- 과제 채점 시
- 성적 구성 변경 시

**처리**:
- `GradeUpdateEventListener`가 수신
- `GradeCalculationService.calculateStudentGrade()` 호출
- 성적 자동 재계산 및 저장

---

## ⚠️ 주의사항

1. **lecSerial 사용**: `lecIdx` 대신 `lecSerial` 권장
2. **트랜잭션**: 성적 계산 중 데이터 무결성 보장 필요
3. **권한 검증**: 교수는 담당 강의만, 학생은 본인만 조회 가능
4. **재계산**: 출석/과제 업데이트 시 자동 재계산됨
5. **최종 등급**: 한 번 배정 후 수정 시 주의 필요

---

## 💡 사용 시나리오

### 시나리오 1: 학기 초 성적 구성 설정
```javascript
// 1. 성적 구성 설정
POST /api/enrollments/grade-config
{
  "action": "set-config",
  "lecSerial": "CS284",
  "attendanceMaxScore": 20,
  "assignmentTotalScore": 50,
  "latePenaltyPerSession": 0.5
}
```

### 시나리오 2: 학기 중 성적 확인
```javascript
// 2. 학생이 자신의 성적 조회
POST /api/enrollments/grade-info
{
  "action": "get-grade",
  "lecSerial": "CS284",
  "studentIdx": 6
}

// 3. 교수가 전체 성적 목록 조회
POST /api/enrollments/grade-list
{
  "action": "list-all",
  "lecSerial": "CS284",
  "sortBy": "percentage",
  "sortOrder": "desc"
}
```

### 시나리오 3: 학기 말 최종 등급 배정
```javascript
// 4. 최종 등급 배정
POST /api/enrollments/grade-finalize
{
  "action": "finalize",
  "lecSerial": "CS284",
  "passingThreshold": 60.0
}
```

---

## 📝 참고사항

### 성적 자동 재계산
- 출석 체크/수정 시 → 출석 점수 재계산
- 과제 채점 시 → 과제 점수 재계산
- 성적 구성 변경 시 → 전체 재계산

### 등급 기준
- A+: 95 이상
- A0: 90~94
- B+: 85~89
- B0: 80~84
- C+: 75~79
- C0: 70~74
- D+: 65~69
- D0: 60~64
- F: 60 미만

---

© 2025 BlueCrab LMS Development Team
