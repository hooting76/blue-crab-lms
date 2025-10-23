# 🔧 백엔드 업데이트 가이드

## 📋 변경 사항 요약

### 2025-10-23: 성적 관리 시스템 개선

#### 🎯 목적
- **문제**: gradeConfig 설정 시 DB에 저장되지 않음
- **문제**: 출석 업데이트 시 기존 데이터 덮어쓰기 (sessions, summary 삭제)
- **해결**: JSON 병합 로직 적용 + DB 자동 저장

---

## 🛠️ 수정된 파일

### 1. `GradeManagementService.java`
**경로**: `src/main/java/BlueCrab/com/example/service/Lecture/GradeManagementService.java`

**변경 내용**:
```java
// ✅ configureGrade() 메서드에 DB 저장 로직 추가

// 기존: JSON 응답만 반환
return Map.of("success", true, "data", gradeConfig);

// 변경: 모든 수강생의 ENROLLMENT_DATA에 gradeConfig 병합 저장
List<EnrollmentExtendedTbl> enrollments = enrollmentRepository.findStudentsByLecture(lecIdx);
for (EnrollmentExtendedTbl enrollment : enrollments) {
    Map<String, Object> currentData = parseEnrollmentData(enrollment.getEnrollmentData());
    currentData.put("gradeConfig", gradeConfig);  // 병합
    
    // grade 객체 초기화 (최초 설정 시)
    if (!currentData.containsKey("grade")) {
        currentData.put("grade", {...});
    }
    
    enrollment.setEnrollmentData(objectMapper.writeValueAsString(currentData));
    enrollmentRepository.save(enrollment);
}
```

**효과**:
- ✅ `gradePhase1.config()` 실행 시 DB에 자동 저장
- ✅ 모든 수강생의 ENROLLMENT_DATA에 gradeConfig 추가
- ✅ grade 객체 자동 초기화 (attendance, assignments, total)

---

### 2. `EnrollmentService.java`
**경로**: `src/main/java/BlueCrab/com/example/service/Lecture/EnrollmentService.java`

**변경 내용**:
```java
// ✅ updateAttendance() 메서드에 병합 로직 적용

// 기존: 새로운 객체로 덮어쓰기
Map<String, Object> attendanceInfo = new HashMap<>();
currentData.put("attendance", attendanceInfo);  // ❌ sessions, summary 삭제됨

// 변경: 기존 attendance 객체 병합
Map<String, Object> existingAttendance = currentData.getOrDefault("attendance", new HashMap<>());
existingAttendance.put("updatedAt", getCurrentDateTime());
// sessions, summary, pendingRequests 유지
currentData.put("attendance", existingAttendance);
```

**효과**:
- ✅ 출석 업데이트 시 기존 데이터 유지 (sessions, summary, pendingRequests)
- ✅ Phase 5 출석 시스템과 Phase 1 성적 시스템 통합 가능

---

## 🚀 백엔드 재시작 방법

### 방법 1: IntelliJ IDEA 사용 (권장)

1. **빌드 실행**
   ```
   [Ctrl+F9] 또는 Build > Build Project
   ```

2. **애플리케이션 재시작**
   ```
   [Shift+F10] 또는 Run > Run 'BlueCrabApplication'
   ```

3. **로그 확인**
   ```
   - 콘솔에서 "Started BlueCrabApplication" 메시지 확인
   - 포트 충돌 없는지 확인 (8080 또는 설정한 포트)
   ```

---

### 방법 2: Maven 명령어 (터미널)

**Windows PowerShell**:
```powershell
# 백엔드 디렉토리로 이동
cd f:\main_project\team_work\blue-crab-lms\backend\BlueCrab

# 빌드 (테스트 스킵)
mvn clean package -DskipTests

# 실행
java -jar target/BlueCrab-1.0.0.jar
```

**또는 Maven Wrapper 사용**:
```powershell
./mvnw clean package -DskipTests
./mvnw spring-boot:run
```

---

### 방법 3: 기존 서버 재시작 (배포 환경)

```bash
# 서버 SSH 접속 후
sudo systemctl restart bluecrab
# 또는
sudo service bluecrab restart

# 로그 확인
sudo journalctl -u bluecrab -f
```

---

## ✅ 테스트 절차

### 1단계: gradeConfig 저장 테스트

**브라우저 콘솔**:
```javascript
// 1. 로그인
await login()  // prof.octopus@univ.edu

// 2. Phase 1 테스트 로드
// 파일: 01-grade-phase1-tests.js 복사 & 실행

// 3. 성적 구성 설정
gradePhase1.setLecture('ETH201')
gradePhase1.quickAttendanceConfig(20, 0.5)
await gradePhase1.config()

// 4. DB 확인 (응답에서 updatedEnrollments 확인)
// 예상 결과: { success: true, updatedEnrollments: 2 }
```

**DB 확인**:
```sql
SELECT 
    STUDENT_IDX,
    JSON_EXTRACT(ENROLLMENT_DATA, '$.gradeConfig') AS gradeConfig,
    JSON_EXTRACT(ENROLLMENT_DATA, '$.grade') AS grade
FROM ENROLLMENT_EXTENDED_TBL
WHERE LEC_IDX = (SELECT LEC_IDX FROM LEC_TBL WHERE LEC_SERIAL = 'ETH201');
```

**예상 결과**:
```json
{
  "gradeConfig": {
    "attendanceMaxScore": 20,
    "latePenaltyPerSession": 0.5,
    "configuredAt": "2025-10-23 17:00:00"
  },
  "grade": {
    "attendance": {
      "maxScore": 20,
      "currentScore": 0,
      "percentage": 0
    }
  }
}
```

---

### 2단계: 출석 병합 테스트

**브라우저 콘솔**:
```javascript
// 1. Phase 3 테스트 로드
// 파일: 02-grade-phase3-tests.js 복사 & 실행

// 2. 출석 승인 테스트
gradePhase3.setLecture('ETH201', 6)
await gradePhase3.attendance()

// 3. DB 확인 (attendance 객체 확인)
```

**DB 확인**:
```sql
SELECT 
    STUDENT_IDX,
    JSON_EXTRACT(ENROLLMENT_DATA, '$.attendance.sessions') AS sessions,
    JSON_EXTRACT(ENROLLMENT_DATA, '$.attendance.summary') AS summary,
    JSON_EXTRACT(ENROLLMENT_DATA, '$.grade.attendance') AS gradeAttendance
FROM ENROLLMENT_EXTENDED_TBL
WHERE STUDENT_IDX = 6 
  AND LEC_IDX = (SELECT LEC_IDX FROM LEC_TBL WHERE LEC_SERIAL = 'ETH201');
```

**예상 결과**:
```json
{
  "attendance": {
    "sessions": [
      {"sessionNumber": 1, "status": "출", "approvedBy": 25, "approvedAt": "..."}
    ],
    "summary": {
      "attended": 1,
      "late": 0,
      "absent": 0,
      "totalSessions": 1,
      "attendanceRate": 100.0
    },
    "updatedAt": "2025-10-23 17:05:00"
  },
  "grade": {
    "attendance": {
      "maxScore": 20,
      "currentScore": 20.0,
      "percentage": 100.0
    }
  }
}
```

---

## 🔍 문제 해결

### 컴파일 에러 발생 시

**증상**: `HashMap cannot be resolved to a type`

**해결**:
```java
// 수정 전
new HashMap<>()

// 수정 후
new java.util.HashMap<>()
```

---

### JSON 파싱 에러 발생 시

**증상**: `JsonProcessingException` 또는 `Unexpected character`

**해결**:
1. ENROLLMENT_DATA가 유효한 JSON인지 확인
2. 기존 데이터 백업 후 테스트 계정으로 재테스트
3. objectMapper 설정 확인

---

### gradeConfig 저장 안 됨

**증상**: `await gradePhase1.config()` 실행해도 DB 변화 없음

**확인 사항**:
1. 백엔드 재시작 여부
2. 강의에 수강생이 있는지 확인 (`updatedEnrollments: 0`)
3. 트랜잭션 롤백 여부 (로그 확인)

**디버깅**:
```java
// GradeManagementService.configureGrade()에 로그 추가
logger.info("✅ 수강생 {}의 ENROLLMENT_DATA 업데이트 완료", enrollment.getEnrollmentIdx());
```

---

## 📊 변경 전후 비교

### ❌ 변경 전

**문제 1**: gradeConfig 미저장
```json
{
  "attendance": {...},
  // gradeConfig 없음
  // grade 없음
}
```

**문제 2**: 출석 데이터 덮어쓰기
```json
{
  "attendance": {
    "updatedAt": "2025-10-23 16:20:14"
    // sessions 삭제됨!
    // summary 삭제됨!
  }
}
```

---

### ✅ 변경 후

**해결 1**: gradeConfig 자동 저장
```json
{
  "gradeConfig": {
    "attendanceMaxScore": 20,
    "latePenaltyPerSession": 0.5,
    "configuredAt": "2025-10-23 17:00:00"
  },
  "grade": {
    "attendance": {"maxScore": 20, "currentScore": 10, "percentage": 50},
    "assignments": [],
    "total": {"maxScore": 20, "score": 10, "percentage": 50}
  }
}
```

**해결 2**: 출석 데이터 병합
```json
{
  "attendance": {
    "sessions": [{"sessionNumber": 1, "status": "출", ...}],
    "summary": {"attended": 1, "totalSessions": 2, ...},
    "pendingRequests": [],
    "updatedAt": "2025-10-23 17:05:00"
  }
}
```

---

## 📝 체크리스트

- [ ] 백엔드 재시작 완료
- [ ] gradePhase1.config() 테스트 성공
- [ ] DB에 gradeConfig 저장 확인
- [ ] DB에 grade 객체 초기화 확인
- [ ] gradePhase3.attendance() 테스트 성공
- [ ] attendance.sessions 유지 확인
- [ ] attendance.summary 유지 확인
- [ ] grade.attendance.currentScore 계산 확인

---

## 🎉 완료 후

성적 관리 시스템이 출석 시스템과 완전히 통합되었습니다!

**다음 단계**:
1. ✅ Phase 1: 성적 구성 설정 (gradeConfig)
2. ✅ Phase 3: 출석 승인 → 성적 자동 반영
3. 🔄 Phase 3: 과제 채점 → 성적 자동 반영
4. 🔄 Phase 1: 최종 등급 배정 (gradeFinalize)

모든 테스트가 성공하면 성적 관리 시스템이 완성됩니다! 🚀
