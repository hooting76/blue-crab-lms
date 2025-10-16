# 시기별 API 명세서

> **작성일**: 2025-10-16  
> **버전**: 1.1  
> **업데이트**: 기존 구현 현황 반영  

---

## 📋 **API 구현 현황**

### ✅ **완료된 API**
- 🟢 **수강중인 강의 조회**: `EnrollmentController`에서 이미 구현됨
- 🟢 **강의 필터링**: `LectureController`에서 기본 기능 제공

### 🔄 **확장 필요한 API**  
- 🟡 **수강신청 자격 검증**: 기존 LectureController 확장 + 0값 규칙 추가

### 🆕 **새로 구현할 API**
- 🔴 **시기 판별 시스템**: SemesterController 완전 신규 구현

---

## 📋 **API 개요**

### **Base URL**
```
/api/semester
```

### **공통 응답 형식**
```json
{
  "success": true,
  "message": "성공 메시지",
  "data": { ... },
  "timestamp": "2025-02-01T10:00:00Z"
}
```

---

## � **1. 시기 판별 API (새로 구현 필요)**

### **현재 시기 조회 - 🆕 신규**
```http
GET /api/semester/current-period
```

**응답 성공 (200)**
```json
{
  "success": true,
  "data": {
    "currentPeriod": "ENROLLMENT",
    "periodName": "수강신청 기간",
    "startDate": "2025-02-01",
    "endDate": "2025-02-14", 
    "remainingDays": 10,
    "nextPeriod": "SEMESTER",
    "nextPeriodName": "학기 중",
    "nextStartDate": "2025-02-15",
    "semesterInfo": {
      "year": 2025,
      "semester": 1,
      "semesterName": "2025-1학기"
    }
  }
}
```

**응답 실패 (500)**
```json
{
  "success": false,
  "message": "시기 판별 중 오류가 발생했습니다",
  "error": "PERIOD_CALCULATION_ERROR"
}
```

---

## 📅 **수강신청 기간 관리 API**

### **수강신청 기간 설정 (관리자용) - 🆕 신규**
```http
POST /api/semester/enrollment-period
Authorization: Bearer {admin_token}
```

**📝 구현 상태**: SemesterController 새로 생성 필요

**요청 본문**
```json
{
  "year": 2025,
  "semester": 1,
  "enrollmentStartDate": "2025-02-01",
  "enrollmentEndDate": "2025-02-14",
  "semesterStartDate": "2025-02-15"
}
```

**응답 성공 (201)**
```json
{
  "success": true,
  "message": "수강신청 기간이 설정되었습니다",
  "data": {
    "periodId": 1,
    "year": 2025,
    "semester": 1,
    "enrollmentPeriod": "2025-02-01 ~ 2025-02-14",
    "semesterPeriod": "2025-02-15 ~ 2025-06-30"
  }
}
```

### **수강신청 기간 조회**
```http
GET /api/semester/enrollment-period/{year}/{semester}
```

**응답 성공 (200)**
```json
{
  "success": true,
  "data": {
    "year": 2025,
    "semester": 1,
    "enrollmentStartDate": "2025-02-01",
    "enrollmentEndDate": "2025-02-14",
    "semesterStartDate": "2025-02-15",
    "semesterEndDate": "2025-06-30",
    "isActive": true,
    "daysRemaining": 5
  }
}
```

---

## 🎓 **수강신청 자격 검증 API - 🔄 기존 확장 필요**

### **수강 가능 강의 조회 - 🔄 확장**
```http
// 새로 구현 필요 (기존 LectureController 확장)
GET /api/enrollment/eligible-lectures
Authorization: Bearer {student_token}
```

**📝 구현 상태**: 
- 🟢 **기반 있음**: LectureController에 강의 필터링 기능 존재
- 🟡 **확장 필요**: 학생별 자격 검증 로직 추가
- 🟡 **0값 규칙**: 학부/학과/학년 제한없음 로직 구현

**쿼리 파라미터**
- `page`: 페이지 번호 (기본값: 1)
- `size`: 페이지 크기 (기본값: 20)
- `year`: 대상 학년 필터
- `semester`: 학기 필터
- `department`: 학과 필터

**응답 성공 (200)**
```json
{
  "success": true,
  "data": {
    "lectures": [
      {
        "lecIdx": 1,
        "lecSerial": "CS001",
        "lecTit": "데이터베이스 설계",
        "lecProf": "김교수",
        "lecTime": "월1월2수3수4",
        "lecMany": 30,
        "lecCurrent": 25,
        "lecYear": 2,
        "lecSemester": 1,
        "facultyCode": "CS",
        "departmentCode": "CSE",
        "minGrade": 2,
        "isEligible": true,
        "eligibilityDetails": {
          "facultyMatch": true,
          "departmentMatch": true,
          "gradeEligible": true,
          "capacityAvailable": true,
          "alreadyEnrolled": false
        }
      }
    ],
    "pagination": {
      "currentPage": 1,
      "totalPages": 3,
      "totalElements": 45,
      "eligibleCount": 32,
      "ineligibleCount": 13
    },
    "studentInfo": {
      "currentGrade": 2,
      "facultyCode": "CS", 
      "departmentCode": "CSE"
    }
  }
}
```

### **특정 강의 수강 자격 확인**
```http
GET /api/enrollment/eligible-lectures/{lecIdx}
Authorization: Bearer {student_token}
```

**응답 성공 (200)**
```json
{
  "success": true,
  "data": {
    "lecIdx": 1,
    "lecTit": "데이터베이스 설계",
    "isEligible": false,
    "eligibilityDetails": {
      "facultyMatch": true,
      "departmentMatch": true,
      "gradeEligible": false,
      "capacityAvailable": true,
      "alreadyEnrolled": false,
      "reasons": [
        "최소 학년(3학년) 미달 (현재 2학년)"
      ]
    }
  }
}
```

---

## 📝 **수강신청 처리 API**

### **수강신청**
```http
POST /api/enrollment/enroll
Authorization: Bearer {student_token}
```

**요청 본문**
```json
{
  "lecIdx": 1
}
```

**응답 성공 (201)**
```json
{
  "success": true,
  "message": "수강신청이 완료되었습니다",
  "data": {
    "enrollmentIdx": 123,
    "lecIdx": 1,
    "lecTit": "데이터베이스 설계",
    "lecProf": "김교수",
    "enrollmentDate": "2025-02-05T14:30:00Z",
    "lecCurrent": 26
  }
}
```

**응답 실패 (400)**
```json
{
  "success": false,
  "message": "수강신청 자격이 없습니다",
  "error": "ENROLLMENT_NOT_ELIGIBLE",
  "details": {
    "reasons": [
      "정원 초과",
      "이미 수강신청한 강의"
    ]
  }
}
```

### **수강신청 취소**
```http
DELETE /api/enrollment/enroll/{lecIdx}
Authorization: Bearer {student_token}
```

**응답 성공 (200)**
```json
{
  "success": true,
  "message": "수강신청이 취소되었습니다",
  "data": {
    "lecIdx": 1,
    "lecTit": "데이터베이스 설계",
    "cancelDate": "2025-02-06T09:15:00Z",
    "lecCurrent": 25
  }
}
```

---

## 📚 **수강중인 강의 조회 API - ✅ 기존 구현됨**

### **내 수강 강의 목록 - ✅ 완료됨**
```http
// 기존 API 사용 (EnrollmentController에 구현됨)
GET /api/enrollments?enrolled=true&studentIdx={studentId}
Authorization: Bearer {student_token}
```

**📝 구현 상태**: 
- ✅ **완료됨**: `EnrollmentController.java`에 이미 구현
- ✅ **기능**: 현재 수강중인 강의 목록 조회
- ✅ **DTO 변환**: Entity → DTO 안전 변환 지원

**쿼리 파라미터**
- `enrolled=true`: 현재 수강중인 강의만 조회
- `studentIdx`: 학생 ID (필수)
- `page`, `size`: 페이징 (선택)

**응답 성공 (200)**
```json
{
  "success": true,
  "data": {
    "lectures": [
      {
        "enrollmentIdx": 123,
        "lecIdx": 1,
        "lecSerial": "CS001",
        "lecTit": "데이터베이스 설계",
        "lecProf": "김교수",
        "lecProfName": "김철수",
        "lecTime": "월1월2수3수4",
        "lecRoom": "공학관 301호",
        "enrollmentDate": "2025-02-05",
        "currentGrade": null,
        "attendanceCount": 8,
        "totalClasses": 16,
        "attendanceRate": 50.0,
        "assignmentCount": 3,
        "submittedCount": 2
      }
    ],
    "summary": {
      "totalLectures": 5,
      "totalCredits": 15,
      "averageAttendance": 75.5
    }
  }
}
```

### **강의 상세정보 조회 - ✅ 기존 기능 활용**
```http
// 기존 API 조합 사용
GET /api/lectures/{lecIdx}  // 강의 기본 정보
GET /api/enrollments/{enrollmentIdx}  // 수강신청 상세 정보
Authorization: Bearer {student_token}
```

**📝 구현 상태**: 
- ✅ **기본 완료**: 강의 정보, 수강신청 정보 각각 조회 가능
- 🔄 **선택 확장**: 통합 엔드포인트 생성 시 편의성 향상

**응답 성공 (200)**
```json
{
  "success": true,
  "data": {
    "lecture": {
      "lecIdx": 1,
      "lecSerial": "CS001",
      "lecTit": "데이터베이스 설계",
      "lecProf": "김교수",
      "lecProfName": "김철수",
      "lecTime": "월1월2수3수4",
      "lecRoom": "공학관 301호",
      "lecMany": 30,
      "lecCurrent": 26,
      "lecYear": 2,
      "lecSemester": 1,
      "credits": 3
    },
    "enrollment": {
      "enrollmentIdx": 123,
      "enrollmentDate": "2025-02-05",
      "currentGrade": 85.5,
      "attendanceData": {
        "attendanceCount": 12,
        "totalClasses": 16,
        "attendanceRate": 75.0,
        "lateCount": 2,
        "absentCount": 2
      }
    },
    "assignments": [
      {
        "assignmentIdx": 1,
        "title": "ERD 설계 과제",
        "dueDate": "2025-03-15",
        "maxScore": 10,
        "submittedScore": 9,
        "submissionDate": "2025-03-14",
        "isSubmitted": true
      }
    ],
    "classmates": {
      "totalCount": 26,
      "classRank": 8
    }
  }
}
```

---

## 🚫 **방학 기간 API**

### **방학 메시지 조회**
```http
GET /api/semester/vacation-message
```

**응답 성공 (200)**
```json
{
  "success": true,
  "data": {
    "period": "VACATION",
    "message": "방학 잘 보내세요 😊",
    "subMessage": "다음 학기 수강신청은 2025년 2월 1일부터 시작됩니다",
    "nextEnrollmentDate": "2025-02-01",
    "daysUntilEnrollment": 45,
    "upcomingSemester": {
      "year": 2025,
      "semester": 1,
      "semesterName": "2025-1학기"
    }
  }
}
```

---

## 📊 **통계 및 관리 API (관리자용)**

### **수강신청 현황 조회**
```http
GET /api/semester/enrollment-statistics
Authorization: Bearer {admin_token}
```

**응답 성공 (200)**
```json
{
  "success": true,
  "data": {
    "period": {
      "year": 2025,
      "semester": 1,
      "enrollmentPeriod": "2025-02-01 ~ 2025-02-14",
      "currentDate": "2025-02-05",
      "daysRemaining": 9
    },
    "statistics": {
      "totalLectures": 150,
      "totalCapacity": 4500,
      "totalEnrollments": 3200,
      "occupancyRate": 71.1,
      "fullLectures": 25,
      "nearFullLectures": 40
    },
    "popularLectures": [
      {
        "lecIdx": 1,
        "lecTit": "데이터베이스 설계",
        "lecProf": "김교수",
        "capacity": 30,
        "enrolled": 30,
        "waitingList": 15
      }
    ],
    "departmentStats": [
      {
        "departmentCode": "CSE",
        "departmentName": "컴퓨터공학과",
        "totalLectures": 20,
        "averageOccupancy": 85.2
      }
    ]
  }
}
```

---

## ⚠️ **에러 코드**

### **공통 에러**
- `INVALID_TOKEN`: 유효하지 않은 토큰
- `PERMISSION_DENIED`: 권한 없음
- `VALIDATION_ERROR`: 입력값 검증 실패

### **시기 관련 에러**
- `PERIOD_NOT_FOUND`: 해당 기간 정보 없음
- `PERIOD_CALCULATION_ERROR`: 시기 계산 오류
- `ENROLLMENT_PERIOD_EXPIRED`: 수강신청 기간 만료

### **수강신청 관련 에러**
- `ENROLLMENT_NOT_ELIGIBLE`: 수강신청 자격 없음
- `LECTURE_FULL`: 정원 초과
- `ALREADY_ENROLLED`: 이미 수강신청한 강의
- `ENROLLMENT_NOT_FOUND`: 수강신청 정보 없음
- `LECTURE_NOT_FOUND`: 강의 정보 없음

---

**검토 필요**: API 경로 최종 확정, 권한 설정 상세화