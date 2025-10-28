# 성적확인서 API - 전체 구조

## 📁 폴더 구조

```
src/main/java/BlueCrab/com/example/
├── controller/Lecture/Certificate/
│   └── CertificateController.java      # REST API 컨트롤러
├── service/Lecture/Certificate/
│   └── CertificateService.java         # 비즈니스 로직
├── dto/Lecture/Certificate/
│   ├── GradeRecord.java                # 성적 레코드 DTO
│   └── TranscriptResponse.java         # 성적확인서 응답 DTO
├── util/Lecture/Certificate/
│   └── GradeCalculator.java            # 성적 계산 유틸리티
└── exception/Lecture/Certificate/
    ├── StudentNotFoundException.java   # 학생 조회 실패 예외
    ├── LectureNotFoundException.java   # 강의 조회 실패 예외
    ├── InvalidGradeDataException.java  # 성적 데이터 오류 예외
    └── GradeCalculationException.java  # 성적 계산 오류 예외
```

## 🎯 주요 기능

### 1. 단일 API로 전체 성적 조회
- 기존: 1 + 2N번의 API 호출 필요
- 개선: 1번의 API 호출로 모든 정보 제공

### 2. 자동 성적 등급 계산
- `ENROLLMENT_DATA` JSON의 `percentage` 자동 추출
- A+, A0, B+, B0, C+, C0, D+, D0, F 자동 계산
- GPA (4.5 만점) 자동 계산

### 3. 상세 통계 제공
- 학기별 통계 (GPA, 등급 분포)
- 전체 통계 (누적 GPA, 평균 백분율)
- 학점 취득률, 졸업 요구 학점 대비 진행률

### 4. JWT 인증 및 권한 관리
- 모든 API 엔드포인트 JWT 토큰 검증
- 학생은 본인 성적만 조회 가능
- 교수/관리자는 모든 학생 성적 조회 가능

### 5. 안전한 데이터 처리
- NullPointerException 방지
- NumberFormatException 방지 (안전한 JSON 파싱)
- LazyInitializationException 방지
- 커스텀 예외를 통한 명확한 오류 처리

## 📋 파일 설명

### 1. CertificateController.java
**역할**: REST API 엔드포인트 제공

**주요 엔드포인트**:
- `POST /api/certificate/transcript` - 학생 본인 성적확인서
- `POST /api/certificate/transcript/student` - 특정 학생 성적확인서 (교수/관리자)
- `POST /api/certificate/semester` - 학기별 성적
- `POST /api/certificate/transcript/download` - PDF 다운로드 (TODO)

**JWT 인증**:
- JwtUtil을 사용한 토큰 검증
- Authorization 헤더에서 Bearer 토큰 추출
- 토큰에서 사용자 정보 추출 및 검증

**권한 관리**:
- 학생: 본인 것만 조회
- 교수/관리자: 모든 학생 조회

**예외 처리**:
- StudentNotFoundException → 404 NOT_FOUND
- LectureNotFoundException → 500 INTERNAL_SERVER_ERROR
- InvalidGradeDataException → 500 INTERNAL_SERVER_ERROR
- IllegalArgumentException → 400 BAD_REQUEST

---

### 2. CertificateService.java
**역할**: 성적확인서 생성 비즈니스 로직

**주요 메서드**:
- `generateTranscript(studentIdx)` - 성적확인서 생성 (@Transactional)
- `createGradeRecord(enrollment)` - 개별 성적 레코드 생성
- `calculateSemesterSummaries()` - 학기별 통계
- `calculateOverallSummary()` - 전체 통계
- `parseSafeBigDecimal()` - 안전한 BigDecimal 파싱
- `parseSafeInteger()` - 안전한 Integer 파싱
- `parseStringToInteger()` - String → Integer 안전 변환

**데이터 처리 흐름**:
1. 학생 정보 조회 (USER_TBL) - 없으면 StudentNotFoundException
2. 수강 이력 조회 (ENROLLMENT_EXTENDED_TBL)
3. Lazy 엔티티 강제 로딩 (LazyInitializationException 방지)
4. 강의 정보 조회 (LEC_TBL) - 없으면 LectureNotFoundException
5. JSON 파싱 및 성적 추출 (안전한 파싱으로 NumberFormatException 방지)
6. 등급 계산 및 통계 생성

**성능 모니터링**:
- 성적확인서 생성 시작/종료 시간 로깅
- 소요 시간 측정 및 로그 기록

**에러 로깅**:
- JSON 파싱 실패 시 상세 로그 (100자 미리보기)
- 예외 발생 시 스택 트레이스 포함

---

### 3. GradeCalculator.java
**역할**: 성적 등급 계산 유틸리티

**주요 메서드**:
- `calculateLetterGrade(percentage)` - 백분율 → 학점 등급
- `convertToGpa(letterGrade)` - 학점 등급 → GPA
- `calculateWeightedGpa()` - 가중 평균 GPA (null 체크 포함)
- `calculateAveragePercentage()` - 평균 백분율 (null 체크 포함)
- `calculateCompletionRate()` - 학점 취득률
- `calculateRankPercentile()` - 석차 백분위 계산
- `countGradesByLetter()` - 등급별 과목 수 (null 필터링)

**성적 기준**:
| 백분율 | 등급 | GPA |
|--------|------|-----|
| 95%↑   | A+   | 4.5 |
| 90~95% | A0   | 4.0 |
| 85~90% | B+   | 3.5 |
| 80~85% | B0   | 3.0 |
| 75~80% | C+   | 2.5 |
| 70~75% | C0   | 2.0 |
| 65~70% | D+   | 1.5 |
| 60~65% | D0   | 1.0 |
| <60%   | F    | 0.0 |

---

### 4. GradeRecord.java
**역할**: 개별 강의의 성적 정보 DTO

**주요 필드**:
```java
- lecSerial: 강의 코드 (CS284, ETH201 등)
- lecTitle: 강의명
- credits: 이수학점
- percentage: 백분율 점수 (0~100)
- letterGrade: 학점 등급 (A+~F)
- totalScore, maxScore: 실제 점수
- attendanceScore: 출석 점수
- assignmentScore: 과제 점수
- gradeStatus: 성적 상태 (IN_PROGRESS, COMPLETED, NOT_GRADED)
- includedInGpa: GPA 계산 포함 여부
```

---

### 5. TranscriptResponse.java
**역할**: 성적확인서 전체 응답 DTO

**구조**:
```java
TranscriptResponse
├── StudentInfo (학생 정보)
│   ├── studentIdx, studentCode, name
│   └── grade, admissionYear
├── List<GradeRecord> (성적 레코드 목록)
├── Map<String, SemesterSummary> (학기별 통계)
│   ├── courseCount, earnedCredits
│   ├── averagePercentage, semesterGpa
│   └── gradeACount, gradeBCount, ...
├── OverallSummary (전체 통계)
│   ├── totalCourses, totalEarnedCredits
│   ├── cumulativeGpa, averagePercentage
│   ├── completionRate, remainingCredits
│   └── gradeACount, gradeBCount, ...
├── issuedAt (발급 시각)
└── certificateNumber (발급 번호)
```

### 6. 커스텀 예외 클래스

**StudentNotFoundException.java**
- 학생을 찾을 수 없을 때 발생
- 필드: `studentIdx` (학생 IDX)
- HTTP 상태: 404 NOT_FOUND

**LectureNotFoundException.java**
- 강의를 찾을 수 없을 때 발생
- 필드: `lecIdx` (강의 IDX)
- HTTP 상태: 500 INTERNAL_SERVER_ERROR

**InvalidGradeDataException.java**
- 성적 데이터가 유효하지 않을 때 발생
- 필드: `enrollmentIdx`, `fieldName`
- HTTP 상태: 500 INTERNAL_SERVER_ERROR

**GradeCalculationException.java**
- 성적 계산 중 오류 발생 시
- 필드: `calculationType` (계산 유형)
- HTTP 상태: 500 INTERNAL_SERVER_ERROR

---

## 🔌 API 사용 예시

### 1. 학생이 본인 성적 조회
```bash
curl -X POST "http://localhost:8080/api/certificate/transcript" \
  -H "Authorization: Bearer {JWT_TOKEN}" \
  -H "Content-Type: application/json" \
  -d '{"action": "get-my-transcript"}'
```

### 2. 교수가 학생 성적 조회
```bash
curl -X POST "http://localhost:8080/api/certificate/transcript/student" \
  -H "Authorization: Bearer {PROFESSOR_JWT_TOKEN}" \
  -H "Content-Type: application/json" \
  -d '{"action": "get-student-transcript", "studentIdx": 33}'
```

### 3. 특정 학기 성적 조회
```bash
curl -X POST "http://localhost:8080/api/certificate/semester" \
  -H "Authorization: Bearer {JWT_TOKEN}" \
  -H "Content-Type: application/json" \
  -d '{"action": "get-semester-grades", "year": 2025, "semester": 1}'
```

---

## 📊 응답 예시

```json
{
  "success": true,
  "message": "성적확인서 조회 성공",
  "data": {
    "student": {
      "studentIdx": 33,
      "studentCode": "202500106114",
      "name": "김민준",
      "grade": 1,
      "admissionYear": 2025
    },
    "gradeRecords": [
      {
        "lecSerial": "CS284",
        "lecTitle": "컴퓨터과학개론",
        "credits": 3,
        "percentage": 92.50,
        "letterGrade": "A0",
        "totalScore": 185.0,
        "maxScore": 200.0,
        "gradeStatus": "COMPLETED"
      },
      {
        "lecSerial": "ETH201",
        "lecTitle": "현대윤리학",
        "credits": 3,
        "percentage": 22.79,
        "letterGrade": "F",
        "totalScore": 67.0,
        "maxScore": 294.0,
        "gradeStatus": "COMPLETED"
      }
    ],
    "semesterSummaries": {
      "2025-1": {
        "year": 2025,
        "semester": 1,
        "courseCount": 2,
        "earnedCredits": 3,
        "attemptedCredits": 6,
        "averagePercentage": 57.65,
        "semesterGpa": 2.0,
        "gradeACount": 1,
        "gradeFCount": 1
      }
    },
    "overallSummary": {
      "totalCourses": 2,
      "totalEarnedCredits": 3,
      "totalAttemptedCredits": 6,
      "cumulativeGpa": 2.0,
      "averagePercentage": 57.65,
      "requiredCredits": 140,
      "remainingCredits": 137,
      "completionRate": 50.0,
      "totalGradeACount": 1,
      "totalGradeFCount": 1
    },
    "issuedAt": "2025-10-28T15:30:00",
    "certificateNumber": "TR-202500106114-20251028153000"
  }
}
```

---

## 🗄️ 데이터베이스 의존성

### 필요한 테이블:
1. **LEC_TBL** - 강의 정보
   - LEC_IDX, LEC_SERIAL, LEC_TIT
   - LEC_POINT (이수학점)
   - LEC_PROF (교수 IDX - String 타입, Integer로 변환)
   - LEC_YEAR, LEC_SEMESTER

2. **ENROLLMENT_EXTENDED_TBL** - 수강 정보
   - ENROLLMENT_IDX, LEC_IDX, STUDENT_IDX
   - ENROLLMENT_DATA (JSON 형식)

3. **USER_TBL** - 사용자 정보
   - USER_IDX, USER_NAME, USER_CODE
   - USER_STUDENT (권한: 0=학생, 1=교수, 2=관리자)

### Repository 의존성:
```java
- EnrollmentExtendedTblRepository
- LecTblRepository
- UserTblRepository
```

### 기타 의존성:
```java
- JwtUtil (JWT 토큰 검증 및 사용자 정보 추출)
- ObjectMapper (Jackson - JSON 파싱)
```

---

## ⚙️ 설치 및 설정

### 1. 파일 배치
```bash
# 각 폴더 생성
mkdir -p src/main/java/BlueCrab/com/example/controller/Lecture/Certificate
mkdir -p src/main/java/BlueCrab/com/example/service/Lecture/Certificate
mkdir -p src/main/java/BlueCrab/com/example/dto/Lecture/Certificate
mkdir -p src/main/java/BlueCrab/com/example/util/Lecture/Certificate
mkdir -p src/main/java/BlueCrab/com/example/exception/Lecture/Certificate

# 파일 복사
cp CertificateController.java src/main/java/BlueCrab/com/example/controller/Lecture/Certificate/
cp CertificateService.java src/main/java/BlueCrab/com/example/service/Lecture/Certificate/
cp GradeRecord.java src/main/java/BlueCrab/com/example/dto/Lecture/Certificate/
cp TranscriptResponse.java src/main/java/BlueCrab/com/example/dto/Lecture/Certificate/
cp GradeCalculator.java src/main/java/BlueCrab/com/example/util/Lecture/Certificate/
cp StudentNotFoundException.java src/main/java/BlueCrab/com/example/exception/Lecture/Certificate/
cp LectureNotFoundException.java src/main/java/BlueCrab/com/example/exception/Lecture/Certificate/
cp InvalidGradeDataException.java src/main/java/BlueCrab/com/example/exception/Lecture/Certificate/
cp GradeCalculationException.java src/main/java/BlueCrab/com/example/exception/Lecture/Certificate/
```

### 2. 의존성 확인
```java
// 필요한 의존성
- Spring Boot Web
- Spring Data JPA
- Jackson (JSON 처리)
- Lombok
- JWT 인증 (JwtUtil)
```

### 3. 빌드 및 실행
```bash
mvn clean install
mvn spring-boot:run
```

---

## 🔒 보안 고려사항

1. **JWT 인증 필수**: 모든 엔드포인트는 JWT 토큰 검증
2. **권한 기반 접근 제어**: 학생/교수/관리자 역할 구분
3. **본인 확인**: 학생은 본인 성적만 조회 가능
4. **SQL Injection 방지**: JPA 사용으로 자동 방어
5. **민감 정보 보호**: 학생 개인정보는 권한이 있는 경우만 조회

## 🛡️ 안정성 개선사항

1. **Null 안전성**:
   - 모든 Entity 조회 후 null 체크
   - JSON 파싱 시 안전한 헬퍼 메서드 사용
   - GPA 계산 시 null 레코드 필터링

2. **예외 처리**:
   - 커스텀 예외로 명확한 오류 메시지 전달
   - 적절한 HTTP 상태 코드 반환
   - 상세한 오류 로깅

3. **성능 최적화**:
   - Lazy 로딩 엔티티 강제 로딩으로 N+1 문제 방지
   - @Transactional(readOnly = true) 사용
   - 성능 메트릭 로깅

4. **데이터 무결성**:
   - NumberFormatException 방지 (안전한 파싱)
   - Type mismatch 방지 (String → Integer 안전 변환)
   - LazyInitializationException 방지

---

## 🚀 향후 개선 사항

### 1. PDF 다운로드 기능 (현재 NOT_IMPLEMENTED)
```java
// iText 또는 Apache PDFBox 사용
@PostMapping("/transcript/download")
public ResponseEntity<byte[]> downloadPdf(HttpServletRequest request) {
    // PDF 생성 로직 구현 필요
}
```

### 2. 석차 계산 기능
```java
// 동일 학년 내 석차 계산
private Integer calculateRank(Integer studentIdx, Integer admissionYear) {
    // 전체 학생의 GPA를 비교하여 석차 산출
}
```

### 3. 엑셀 내보내기
```java
// Apache POI 사용
@PostMapping("/transcript/export/excel")
public ResponseEntity<byte[]> exportToExcel(HttpServletRequest request) {
    // Excel 파일 생성
}
```

### 4. 학과별 평균 비교
```java
// 학과 평균 GPA와 비교
private BigDecimal getDepartmentAverageGpa(String departmentCode) {
    // 학과 평균 계산
}
```

### 5. InvalidGradeDataException 활용
- 현재 생성되어 있으나 미사용
- JSON 파싱 실패 시 더 구체적인 예외 처리에 활용 가능

### 6. GradeCalculationException 활용
- 현재 생성되어 있으나 미사용
- GPA 계산 오류 시 더 명확한 오류 추적에 활용 가능

---

## 📞 문서 정보

**최종 수정일**: 2025-10-28  
**버전**: 2.0.0  

**관련 문서**:
- API_GUIDE.md - 상세 API 사용 가이드
- 각 Java 파일의 JavaDoc 주석 참조

**주요 변경사항 (v2.0.0)**:
- JWT 인증 통합 (JwtUtil 사용)
- 커스텀 예외 클래스 추가
- Null 안전성 개선
- LazyInitializationException 방지
- 성능 모니터링 및 상세 로깅 추가

---

## ✅ 체크리스트

설치 전 확인사항:
- [ ] Java 8 이상 설치
- [ ] Spring Boot 프로젝트 설정 완료
- [ ] MariaDB 10.11.6 데이터베이스 연결
- [ ] JPA Repository 설정 완료
- [ ] JwtUtil 구현 완료
- [ ] Lombok 플러그인 설치
- [ ] Jackson ObjectMapper 설정 완료

설치 후 확인사항:
- [ ] 모든 파일이 올바른 패키지에 배치 (exception/Lecture/Certificate 포함)
- [ ] 의존성 주입 오류 없음
- [ ] API 엔드포인트 정상 동작
- [ ] JWT 인증 테스트 통과
- [ ] 권한 기반 접근 제어 동작
- [ ] 성적 계산 로직 정확성 확인
- [ ] NullPointerException 발생하지 않음
- [ ] JSON 파싱 오류 처리 정상 동작
- [ ] 커스텀 예외 HTTP 상태 코드 확인

---

## 🎓 사용 시나리오

### 시나리오 1: 학생이 학기말에 성적 확인
1. 학생 로그인 → JWT 토큰 발급
2. `/api/certificate/transcript` 호출
3. 전체 성적 확인서 확인
4. 학기별 GPA 및 누적 GPA 확인

### 시나리오 2: 교수가 학생 상담 전 성적 확인
1. 교수 로그인 → JWT 토큰 발급
2. `/api/certificate/transcript/33` 호출 (학생 IDX 33)
3. 해당 학생의 전체 성적 이력 확인
4. 학업 성취도 분석 및 상담 자료로 활용

### 시나리오 3: 장학금 심사
1. 관리자 로그인
2. 대상 학생들의 성적확인서 일괄 조회
3. `cumulativeGpa` 및 `averagePercentage` 기준으로 필터링
4. 장학금 수혜자 선정

---

## 📚 참고사항

### JSON 데이터 구조
ENROLLMENT_DATA의 예상 구조:
```json
{
  "grade": {
    "total": {
      "score": 185.0,
      "maxScore": 200.0,
      "percentage": 92.50
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

### 성적 상태 값
- `IN_PROGRESS`: 현재 수강 중 (성적 미확정)
- `COMPLETED`: 수강 완료 (성적 확정)
- `NOT_GRADED`: 성적 미입력 상태

### GPA 계산 규칙
- F 등급은 GPA 계산에서 제외 (`includedInGpa: false`)
- 가중 평균 사용 (학점 수를 고려)
- 소수점 둘째 자리까지 반올림
