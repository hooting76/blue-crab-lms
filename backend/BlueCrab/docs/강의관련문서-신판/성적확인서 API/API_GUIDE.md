# 성적확인서 API 사용 가이드

## 📋 개요

학생의 수강 이력을 기반으로 성적확인서를 발급하는 전용 API입니다. 기존 API를 여러 번 호출하는 대신, 단일 API 호출로 학생의 전체 성적 정보를 취합하여 제공합니다.

**패키지 구조**: 
- Controller: `BlueCrab.com.example.controller.Lecture.Certificate`
- Service: `BlueCrab.com.example.service.Lecture.Certificate`
- DTO: `BlueCrab.com.example.dto.Lecture.Certificate`
- Util: `BlueCrab.com.example.util.Lecture.Certificate`
- Exception: `BlueCrab.com.example.exception.Lecture.Certificate`

**주요 기능**:
- 학생 본인의 성적확인서 조회 (JWT 인증)
- 교수/관리자의 학생 성적확인서 조회 (JWT 인증)
- 학기별 성적 조회
- 학점 등급 자동 계산 (A+, A0, B+, B0, C+, C0, D+, D0, F)
- GPA 계산 (4.5 만점)
- 학기별/전체 성적 통계
- 안전한 데이터 처리 (Null 체크, 예외 처리)

---

## 🏗️ 클래스 구조

### 1. CertificateController.java
REST API 엔드포인트를 제공하는 컨트롤러

**주요 엔드포인트**:
- `POST /api/certificate/transcript` - 학생 본인 성적확인서 조회
- `POST /api/certificate/transcript/student` - 특정 학생 성적확인서 조회 (교수/관리자)
- `POST /api/certificate/semester` - 학기별 성적 조회
- `POST /api/certificate/transcript/download` - PDF 다운로드 (현재 NOT_IMPLEMENTED)

**인증 및 권한**:
- JwtUtil을 통한 JWT 토큰 검증
- 학생: 본인 성적만 조회
- 교수/관리자: 모든 학생 성적 조회

### 2. CertificateService.java
성적확인서 생성 비즈니스 로직

**주요 메서드**:
- `generateTranscript(studentIdx)` - 성적확인서 생성 (@Transactional)
- `createGradeRecord(enrollment)` - 개별 성적 레코드 생성
- `calculateSemesterSummaries()` - 학기별 통계 계산
- `calculateOverallSummary()` - 전체 통계 계산
- `parseSafeBigDecimal()`, `parseSafeInteger()` - 안전한 JSON 파싱
- `parseStringToInteger()` - String → Integer 안전 변환

**안전성 기능**:
- Lazy 엔티티 강제 로딩 (LazyInitializationException 방지)
- Null 체크 및 안전한 파싱
- 성능 모니터링 (시작/종료 시간 로깅)
- 상세 에러 로깅

### 3. GradeCalculator.java
성적 등급 계산 유틸리티

**주요 메서드**:
- `calculateLetterGrade(percentage)` - 백분율을 학점 등급으로 변환
- `convertToGpa(letterGrade)` - 학점 등급을 GPA로 변환
- `calculateWeightedGpa(gradeRecords)` - 가중 평균 GPA 계산 (null 안전)
- `calculateAveragePercentage()` - 평균 백분율 계산 (null 안전)
- `countGradesByLetter()` - 등급별 과목 수 (null 필터링)

### 4. GradeRecord.java
개별 강의의 성적 정보를 담는 DTO

### 5. TranscriptResponse.java
성적확인서 전체 정보를 담는 응답 DTO

### 6. 커스텀 예외 클래스
- `StudentNotFoundException` - 학생 조회 실패 (404)
- `LectureNotFoundException` - 강의 조회 실패 (500)
- `InvalidGradeDataException` - 성적 데이터 오류 (500)
- `GradeCalculationException` - 성적 계산 오류 (500)

---

## 📊 성적 등급 기준

백분율 점수를 기반으로 학점 등급을 자동 계산합니다:

| 백분율 범위 | 학점 등급 | GPA (4.5) |
|------------|----------|-----------|
| 95% 이상   | A+       | 4.5       |
| 90~95%    | A0       | 4.0       |
| 85~90%    | B+       | 3.5       |
| 80~85%    | B0       | 3.0       |
| 75~80%    | C+       | 2.5       |
| 70~75%    | C0       | 2.0       |
| 65~70%    | D+       | 1.5       |
| 60~65%    | D0       | 1.0       |
| 60% 미만   | F        | 0.0       |

---

## 🔌 API 엔드포인트

### 1. 학생 본인 성적확인서 조회

**엔드포인트**: `POST /api/certificate/transcript`

**권한**: 학생 (JWT 토큰 필수)

**요청 헤더**:
```http
Authorization: Bearer {JWT_TOKEN}
Content-Type: application/json
```

**요청 본문**:
```json
{
  "action": "get-my-transcript"
}
```

**성공 응답** (200 OK):
```json
{
  "success": true,
  "message": "성적확인서 조회 성공",
  "data": {
    "student": {
      "studentIdx": 33,
      "studentCode": "202500106114",
      "name": "김민준",
      "departmentCode": null,
      "departmentName": null,
      "grade": 1,
      "admissionYear": 2025
    },
    "gradeRecords": [
      {
        "lecIdx": 48,
        "lecSerial": "ETH201",
        "lecTitle": "현대윤리학",
        "credits": 3,
        "professorIdx": 25,
        "professorName": "문어",
        "year": 2025,
        "semester": 1,
        "totalScore": 67.0,
        "maxScore": 294.0,
        "percentage": 22.79,
        "letterGrade": "F",
        "attendanceScore": 67.0,
        "attendanceMaxScore": 67.0,
        "assignmentScore": 0.0,
        "assignmentMaxScore": 227.0,
        "attendanceRate": 80,
        "gradeStatus": "COMPLETED",
        "includedInGpa": false,
        "remarks": null
      }
    ],
    "semesterSummaries": {
      "2025-1": {
        "semesterKey": "2025-1",
        "year": 2025,
        "semester": 1,
        "courseCount": 1,
        "earnedCredits": 0,
        "attemptedCredits": 3,
        "averagePercentage": 22.79,
        "semesterGpa": 0.0,
        "gradeACount": 0,
        "gradeBCount": 0,
        "gradeCCount": 0,
        "gradeDCount": 0,
        "gradeFCount": 1
      }
    },
    "overallSummary": {
      "totalCourses": 1,
      "totalEarnedCredits": 0,
      "totalAttemptedCredits": 3,
      "cumulativeGpa": 0.0,
      "averagePercentage": 22.79,
      "requiredCredits": 140,
      "remainingCredits": 140,
      "completionRate": 0.0,
      "totalGradeACount": 0,
      "totalGradeBCount": 0,
      "totalGradeCCount": 0,
      "totalGradeDCount": 0,
      "totalGradeFCount": 1,
      "rank": null,
      "totalStudents": null,
      "rankPercentile": null
    },
    "issuedAt": "2025-10-29T15:30:00",
    "certificateNumber": "TR-202500106114-20251029153000"
  }
}
```

**에러 응답**:
- 400 BAD_REQUEST: JWT 토큰 누락
- 401 UNAUTHORIZED: 유효하지 않은 JWT 토큰
- 404 NOT_FOUND: 학생을 찾을 수 없음 (StudentNotFoundException)
- 500 INTERNAL_SERVER_ERROR: 서버 오류

---

### 2. 특정 학생 성적확인서 조회 (교수/관리자)

**엔드포인트**: `POST /api/certificate/transcript/student`

**권한**: 교수 또는 관리자 (JWT 토큰 필수)

**요청 헤더**:
```http
Authorization: Bearer {JWT_TOKEN}
Content-Type: application/json
```

**요청 본문**:
```json
{
  "action": "get-student-transcript",
  "studentIdx": 33
}
```

**성공 응답** (200 OK): 위의 응답과 동일

**에러 응답**:
- 400 BAD_REQUEST: studentIdx 누락 또는 유효하지 않음
- 401 UNAUTHORIZED: 유효하지 않은 JWT 토큰
- 403 FORBIDDEN: 권한 없음 (학생이 다른 학생 조회 시도)
- 404 NOT_FOUND: 학생을 찾을 수 없음
- 500 INTERNAL_SERVER_ERROR: 서버 오류

---

### 3. 학기별 성적 조회

**엔드포인트**: `POST /api/certificate/semester`

**권한**: 학생 본인 (JWT 토큰 필수)

**요청 헤더**:
```http
Authorization: Bearer {JWT_TOKEN}
Content-Type: application/json
```

**요청 본문**:
```json
{
  "action": "get-semester-grades",
  "year": 2025,
  "semester": 1
}
```

**성공 응답** (200 OK):
```json
{
  "success": true,
  "message": "학기별 성적 조회 성공",
  "data": {
    "semester": {
      "semesterKey": "2025-1",
      "year": 2025,
      "semester": 1,
      "courseCount": 5,
      "earnedCredits": 12,
      "attemptedCredits": 15,
      "averagePercentage": 85.50,
      "semesterGpa": 3.8,
      "gradeACount": 3,
      "gradeBCount": 1,
      "gradeCCount": 0,
      "gradeDCount": 0,
      "gradeFCount": 1
    },
    "gradeRecords": [
      {
        "lecSerial": "CS284",
        "lecTitle": "컴퓨터과학개론",
        "credits": 3,
        "percentage": 92.50,
        "letterGrade": "A0",
        "gradeStatus": "COMPLETED"
      }
    ]
  }
}
```

**에러 응답**:
- 400 BAD_REQUEST: year 또는 semester 누락/유효하지 않음
- 401 UNAUTHORIZED: 유효하지 않은 JWT 토큰
- 404 NOT_FOUND: 해당 학기 성적 없음
- 500 INTERNAL_SERVER_ERROR: 서버 오류

---

### 4. PDF 다운로드 (미구현)

**엔드포인트**: `POST /api/certificate/transcript/download`

**권한**: 학생 본인 (JWT 토큰 필수)

**요청 본문**:
```json
{
  "action": "download-pdf"
}
```

**현재 응답** (501 NOT_IMPLEMENTED):
```json
{
  "success": false,
  "message": "PDF 다운로드 기능은 아직 구현되지 않았습니다"
}
```

---

## 💡 주요 특징

### 1. 단일 API 호출로 전체 성적 조회
기존 방식:
- 수강 목록 조회 API
- 각 강의별 성적 조회 API (N번)
- 강의 정보 조회 API (N번)
- 총 1 + 2N번의 API 호출

새로운 방식:
- 성적확인서 API 1번 호출로 모든 정보 취합

### 2. 자동 성적 등급 계산
- `ENROLLMENT_DATA` JSON의 `grade.total.percentage` 자동 추출
- 백분율 기준으로 A+~F 등급 자동 계산
- 교수가 최종 성적을 반영하지 않아도 임시 등급 확인 가능

### 3. 상세 통계 제공
- 학기별 통계 (학점, GPA, 등급 분포)
- 전체 통계 (누적 GPA, 평균 백분율, 학점 취득률)
- 졸업 요구 학점 대비 진행률

### 4. 성적 상태 관리
- `IN_PROGRESS`: 수강 중
- `COMPLETED`: 수강 완료 (성적 확정)
- `NOT_GRADED`: 성적 미입력

### 5. 안전한 데이터 처리
- NullPointerException 방지 (모든 Entity 조회 후 null 체크)
- NumberFormatException 방지 (안전한 JSON 파싱)
- LazyInitializationException 방지 (강제 로딩)
- 명확한 커스텀 예외 처리

### 6. JWT 인증 및 권한 관리
- JwtUtil을 통한 토큰 검증
- 사용자 역할에 따른 접근 제어
- 학생은 본인 성적만 조회 가능

---

## 🔒 권한 관리

### 학생 (USER_STUDENT = 0)
- 본인의 성적확인서만 조회 가능
- `/api/certificate/transcript` 엔드포인트 사용
- `/api/certificate/semester/{year}/{semester}` 사용 가능

### 교수 (USER_STUDENT = 1)
- 모든 학생의 성적확인서 조회 가능
- `/api/certificate/transcript/{studentIdx}` 엔드포인트 사용

### 관리자 (USER_STUDENT = 2)
- 모든 학생의 성적확인서 조회 가능
- 모든 엔드포인트 접근 가능

---

## 📦 데이터 소스

### 1. LEC_TBL
- `LEC_POINT`: 이수학점
- `LEC_SERIAL`: 강의 코드
- `LEC_TIT`: 강의명
- `LEC_PROF`: 교수 IDX (String → Integer 변환)
- `LEC_YEAR`, `LEC_SEMESTER`: 개설 년도/학기

### 2. ENROLLMENT_EXTENDED_TBL
- `ENROLLMENT_DATA` JSON 구조:
  ```json
  {
    "grade": {
      "total": {
        "maxScore": 294.0,
        "score": 67.0,
        "percentage": 22.79
      },
      "assignments": [
        {
          "title": "과제1",
          "score": 45.0,
          "maxScore": 50.0
        }
      ],
      "attendanceScore": {
        "score": 67.0,
        "maxScore": 67.0,
        "rate": 80
      }
    }
  }
  ```

### 3. USER_TBL
- `USER_NAME`: 학생/교수 이름
- `USER_CODE`: 학번
- `USER_STUDENT`: 권한 (0: 학생, 1: 교수, 2: 관리자)

---

## 🚀 사용 예시

### 1. 학생이 본인 성적 조회
```javascript
// JavaScript Fetch API 예시
const response = await fetch('/api/certificate/transcript', {
  method: 'POST',
  headers: {
    'Authorization': `Bearer ${token}`,
    'Content-Type': 'application/json'
  },
  body: JSON.stringify({
    action: 'get-my-transcript'
  })
});

const result = await response.json();
console.log('전체 GPA:', result.data.overallSummary.cumulativeGpa);
console.log('평균 백분율:', result.data.overallSummary.averagePercentage);
```

### 2. 교수가 학생 성적 조회
```javascript
const studentIdx = 33;
const response = await fetch('/api/certificate/transcript/student', {
  method: 'POST',
  headers: {
    'Authorization': `Bearer ${professorToken}`,
    'Content-Type': 'application/json'
  },
  body: JSON.stringify({
    action: 'get-student-transcript',
    studentIdx: studentIdx
  })
});
```

### 3. 특정 학기 성적만 조회
```javascript
const response = await fetch('/api/certificate/semester', {
  method: 'POST',
  headers: {
    'Authorization': `Bearer ${token}`,
    'Content-Type': 'application/json'
  },
  body: JSON.stringify({
    action: 'get-semester-grades',
    year: 2025,
    semester: 1
  })
});
```

---

## ⚠️ 주의사항

1. **JWT 토큰 필수**: 모든 엔드포인트는 인증 필요
2. **권한 확인**: 학생은 본인 것만, 교수/관리자는 모든 학생 조회 가능
3. **성적 미입력 처리**: `ENROLLMENT_DATA`가 비어있거나 없는 경우 기본값 반환
4. **F 등급 GPA**: F 등급은 `includedInGpa: false`로 GPA 계산에서 제외
5. **졸업 학점**: 기본값 140학점 (필요시 수정 가능)
6. **Null 처리**: 모든 데이터는 null 체크 후 안전하게 처리됨
7. **에러 로깅**: 모든 예외는 로그에 상세 기록됨
8. **성능**: Lazy 로딩 최적화로 N+1 문제 방지

## 🔐 보안 고려사항

1. **JWT 토큰 검증**: JwtUtil을 통한 엄격한 토큰 검증
2. **권한 기반 접근**: 사용자 역할에 따른 데이터 접근 제어
3. **민감 정보 보호**: 학생은 본인 정보만 조회 가능
4. **SQL Injection 방지**: JPA를 통한 안전한 쿼리 실행
5. **에러 메시지**: 보안에 민감한 정보 노출 방지

---

## 🔧 확장 가능성

### 1. PDF 다운로드 기능 (현재 미구현)
```java
@PostMapping("/transcript/download")
public ResponseEntity<byte[]> downloadTranscriptPdf(HttpServletRequest request) {
    // iText 또는 Apache PDFBox를 사용한 PDF 생성
    // 현재는 501 NOT_IMPLEMENTED 반환
}
```

### 2. 석차 계산 기능
```java
// OverallSummary에 석차 정보 추가
.rank(calculateRank(studentIdx, admissionYear))
.totalStudents(getTotalStudents(admissionYear))
.rankPercentile(calculatePercentile(rank, totalStudents))
```

### 3. 학과별 필터링
```java
@PostMapping("/transcript/department")
public ResponseEntity<List<TranscriptResponse>> getDepartmentTranscripts(
    @RequestBody Map<String, Object> requestBody) {
    // 학과별 성적확인서 목록 반환
}
```

### 4. 엑셀 내보내기
```java
@PostMapping("/transcript/export/excel")
public ResponseEntity<byte[]> exportToExcel(HttpServletRequest request) {
    // Apache POI를 사용한 Excel 파일 생성
}
```

### 5. 커스텀 예외 활용 확대
- InvalidGradeDataException: JSON 파싱 실패 시 더 구체적인 필드 정보
- GradeCalculationException: 특정 계산 유형별 오류 추적

---

## 📝 응답 필드 설명

### StudentInfo
- `studentIdx`: 학생 고유 ID
- `studentCode`: 학번
- `name`: 이름
- `departmentCode`: 학과 코드 (현재 null)
- `departmentName`: 학과명 (현재 null)
- `grade`: 현재 학년 (1~4)
- `admissionYear`: 입학년도

### GradeRecord
- `lecIdx`: 강의 고유 ID
- `lecSerial`: 강의 코드 (예: CS284, ETH201)
- `lecTitle`: 강의명
- `credits`: 이수학점
- `professorIdx`: 교수 IDX
- `professorName`: 교수 이름
- `year`, `semester`: 수강 년도/학기
- `totalScore`, `maxScore`: 실제 획득 점수/만점
- `percentage`: 백분율 점수 (0~100)
- `letterGrade`: 학점 등급 (A+~F)
- `attendanceScore`, `attendanceMaxScore`: 출석 점수/만점
- `assignmentScore`, `assignmentMaxScore`: 과제 점수/만점
- `attendanceRate`: 출석률 (%)
- `gradeStatus`: 성적 상태 (IN_PROGRESS, COMPLETED, NOT_GRADED)
- `includedInGpa`: GPA 계산 포함 여부 (F는 false)
- `remarks`: 비고 (재수강, 계절학기 등)

### SemesterSummary
- `semesterKey`: 학기 식별자 (예: "2025-1")
- `year`, `semester`: 년도/학기
- `courseCount`: 수강 과목 수
- `earnedCredits`: 취득 학점 (F 제외)
- `attemptedCredits`: 신청 학점 (전체)
- `averagePercentage`: 학기 평균 백분율
- `semesterGpa`: 학기 평균 등급 (4.5 만점)
- `gradeACount`, `gradeBCount`, ...: 등급별 과목 수

### OverallSummary
- `totalCourses`: 총 수강 과목 수
- `totalEarnedCredits`: 총 취득 학점 (F 제외)
- `totalAttemptedCredits`: 총 신청 학점
- `cumulativeGpa`: 누적 평점 (4.5 만점)
- `averagePercentage`: 전체 평균 백분율
- `requiredCredits`: 졸업 요구 학점 (140)
- `remainingCredits`: 졸업까지 남은 학점
- `completionRate`: 학점 취득률 (%)
- `totalGradeACount`, ...: 전체 등급별 과목 수
- `rank`: 석차 (현재 null)
- `totalStudents`: 전체 학생 수 (현재 null)
- `rankPercentile`: 석차 백분위 (현재 null)

### 기타
- `issuedAt`: 성적확인서 발급 시각 (LocalDateTime)
- `certificateNumber`: 발급 번호 (형식: TR-{학번}-{YYYYMMDDHHMMSS})

---

## 🎯 결론

성적확인서 API는 학생의 성적 관리를 효율적으로 처리하기 위한 전용 엔드포인트입니다.

**장점**:
1. ✅ 서버 부하 감소 (여러 API 호출 → 단일 API 호출)
2. ✅ 자동 성적 등급 계산 (백분율 기준)
3. ✅ 상세 통계 제공 (학기별/전체)
4. ✅ JWT 기반 권한 관리
5. ✅ 안전한 데이터 처리 (Null 체크, 예외 처리)
6. ✅ 성능 최적화 (Lazy 로딩, @Transactional)
7. ✅ 명확한 오류 처리 (커스텀 예외)
8. ✅ 확장 가능한 구조 (PDF, 석차 등)

**권장 사용처**:
- 학생 포털의 성적 조회 페이지
- 교수 포털의 학생 성적 확인
- 졸업 사정 시스템
- 장학금 심사 시스템

**기술적 안정성**:
- NullPointerException 방지
- NumberFormatException 방지 (안전한 JSON 파싱)
- LazyInitializationException 방지 (강제 로딩)
- 명확한 HTTP 상태 코드 반환

---

## 📚 문서 정보

**최종 수정일**: 2025-10-29  
**버전**: 2.0.0

**주요 변경사항 (v2.0.0)**:
- JWT 인증 통합 완료
- 커스텀 예외 클래스 4개 추가
- Null 안전성 개선 (모든 파싱 메서드)
- LazyInitializationException 방지 로직 추가
- 성능 모니터링 및 상세 로깅
- HTTP 상태 코드 명확화
- 에러 응답 형식 표준화

**관련 파일**:
- README.md - 전체 구조 및 설치 가이드
- 각 Java 파일의 JavaDoc 주석
