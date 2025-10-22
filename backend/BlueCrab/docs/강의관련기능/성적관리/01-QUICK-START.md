# 🚀 빠른 시작 가이드# 📦 빌드 & 테스트



서버 빌드 및 브라우저 콘솔 테스트Phase 4: 서버 빌드 및 브라우저 테스트



------



## ⚡ 3분 퀵 스타트## 🔧 1단계: 서버 빌드



```powershell### Maven 사용 시

# 1. 서버 실행

cd f:\main_project\team_work\blue-crab-lms\backend\BlueCrab```powershell

mvn spring-boot:run# 프로젝트 루트로 이동

cd f:\main_project\team_work\blue-crab-lms\backend\BlueCrab

# 2. 브라우저에서 https://bluecrab.chickenkiller.com 접속

# 3. F12 → Console 탭# 클린 빌드 (테스트 스킵)

# 4. 테스트 파일 로드 → 실행mvn clean package -DskipTests

```

# 또는 컴파일만

---mvn clean compile

```

## 📦 Phase 1 테스트 (5개 API)

### Gradle 사용 시

### 1단계: 테스트 파일 로드

```javascript```powershell

// 01-grade-phase1-tests.js 내용을 콘솔에 붙여넣기# 프로젝트 루트로 이동

```cd f:\main_project\team_work\blue-crab-lms\backend\BlueCrab



### 2단계: 설정 확인# 클린 빌드 (테스트 스킵)

```javascript.\gradlew clean build -x test

gradePhase1.getConfig()

// → lecSerial: null, studentIdx: null 확인# 또는 컴파일만

```.\gradlew clean compileJava

```

### 3단계: 강의 설정

```javascript### ✅ 빌드 성공 확인

gradePhase1.setLecture('ETH201', 6)

// lecSerial: 'ETH201'컴파일 성공 메시지 확인:

// studentIdx: 6

``````

BUILD SUCCESS

### 4단계: 전체 테스트 실행```

```javascript

await gradePhase1.runAll()또는

// → 5개 테스트 실행 (10-15초 소요)

``````

BUILD SUCCESSFUL

### ✅ 예상 결과```

```text

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━### ❌ 빌드 실패 시

📊 Phase 1 테스트 결과

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━1. **컴파일 에러 확인**:

총 테스트: 5개   - 빌드 로그에서 에러 메시지 확인

✅ 성공: 5개   - 파일 경로와 라인 번호 확인

❌ 실패: 0개

📈 성공률: 100.0%2. **의존성 문제**:

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━   ```powershell

```   # Maven

   mvn dependency:resolve

---   

   # Gradle

## ⚡ Phase 3 테스트 (2개 API)   .\gradlew dependencies

   ```

### 1단계: 테스트 파일 로드

```javascript3. **캐시 삭제**:

// 02-grade-phase3-tests.js 내용을 콘솔에 붙여넣기   ```powershell

```   # Maven

   mvn clean

### 2단계: 학생 목록 조회 (선택)   

```javascript   # Gradle

await gradePhase3.listStudents()   .\gradlew clean

// → 수강 중인 학생 목록 조회   ```

// → studentIdx 확인

```---



### 3단계: 강의 설정## 🚀 2단계: 서버 실행

```javascript

gradePhase3.setLecture('ETH201', 6, 1)### Spring Boot 실행

// lecSerial: 'ETH201'

// studentIdx: 6```powershell

// assignmentIdx: 1# Maven

```mvn spring-boot:run



### 4단계: 전체 테스트 실행# Gradle

```javascript.\gradlew bootRun

await gradePhase3.runAll()

// → 2개 테스트 실행 (5-10초 소요)# JAR 파일 실행

// → enrollmentIdx 자동 조회java -jar target/BlueCrab-0.0.1-SNAPSHOT.jar

``````



### ✅ 예상 결과### ✅ 서버 실행 확인

```text

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━서버가 정상적으로 시작되면 다음 로그 확인:

📊 Phase 3 테스트 결과

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━```

총 테스트: 2개Started BlueCrabApplication in X.XXX seconds

✅ 성공: 2개```

❌ 실패: 0개

📈 성공률: 100.0%---

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

✅ enrollmentIdx 자동 조회: 성공## 🧪 3단계: 브라우저 콘솔 테스트

✅ 출석 업데이트 → 성적 재계산

✅ 과제 채점 → 성적 재계산### 테스트 페이지 접속

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

```1. 브라우저에서 다음 URL 접속:

   ```

---   http://localhost:8080

   ```

## 🔧 서버 빌드 (최초 1회)

2. 로그인 (교수 계정 사용)

### Maven 빌드

```powershell### 테스트 스크립트 로드

cd f:\main_project\team_work\blue-crab-lms\backend\BlueCrab

mvn clean package -DskipTests브라우저 개발자 도구(F12) → Console 탭에서 다음 실행:

```

```javascript

### 빌드 성공 확인// 테스트 스크립트 로드

```textconst script = document.createElement('script');

[INFO] BUILD SUCCESSscript.src = '/test/04-grade/grade-management-test.js';

```script.onload = () => console.log('✅ 성적 관리 테스트 스크립트 로드 완료');

script.onerror = () => console.error('❌ 스크립트 로드 실패');

---document.head.appendChild(script);

```

## 🛠️ 개별 테스트

### 전체 테스트 실행

### Phase 1 개별 실행

```javascript```javascript

// 1. 성적 구성 설정// 모든 테스트 실행

await gradePhase1.config()gradeTests.runAll();

```

// 2. 학생 성적 조회

await gradePhase1.studentInfo()### 개별 테스트 실행



// 3. 교수용 성적 조회```javascript

await gradePhase1.professorView()// 1. 성적 구성 설정 테스트

gradeTests.configureGrade();

// 4. 성적 목록 조회

await gradePhase1.gradeList()// 2. 교수용 성적 조회 테스트

gradeTests.professorGradeView();

// 5. 최종 등급 배정

await gradePhase1.finalize()// 3. 전체 성적 목록 조회 테스트

```gradeTests.gradeList();



### Phase 3 개별 실행// 4. 최종 등급 배정 테스트

```javascriptgradeTests.finalizeGrades();

// 1. 출석 업데이트

await gradePhase3.testAttendanceUpdate()// 5. 이벤트 시스템 테스트 (출석 업데이트)

gradeTests.testAttendanceEvent();

// 2. 과제 채점

await gradePhase3.testAssignmentGrade()// 6. 이벤트 시스템 테스트 (과제 채점)

```gradeTests.testAssignmentEvent();

```

---

---

## 🔍 설정 변경

## ✅ 4단계: 최소 검증

### Phase 1 설정 변경

```javascript### 필수 확인 사항

// 방법 1: 직접 설정

gradePhase1.setLecture('ETH201', 6)#### 1. API 응답 확인



// 방법 2: 대화형 입력각 API 호출 후 다음 확인:

await gradePhase1.promptLecture()

- ✅ 상태 코드 200 (성공)

// 확인- ✅ 응답 데이터 존재

gradePhase1.getConfig()- ✅ 에러 없음

```

#### 2. 성적 계산 정확성

### Phase 3 설정 변경

```javascript```javascript

// 설정// 콘솔에서 수동 확인

gradePhase3.setLecture('ETH201', 6, 1)// lecSerial 사용 예시

gradePhase1.setLecture('ETH201', 100)  // lecSerial, studentIdx

// 확인await gradePhase1.professorView()

gradePhase3.getConfig()

```// 또는 lecIdx 사용

gradePhase1.config.lecIdx = 1

---gradePhase1.config.studentIdx = 100

await gradePhase1.professorView()

## ❌ 문제 해결```



### 문제 1: "gradePhase1 is not defined"**예상 결과**:

**원인**: 테스트 파일을 로드하지 않음  - 출석 점수: 0-80점 (또는 설정한 maxScore)

**해결**: 파일 내용을 콘솔에 복사-붙여넣기- 출석 백분율: 0.00-100.00%

- 과제 점수: 각 과제별 0-10점

### 문제 2: "401 Unauthorized"- 총점: 출석 + 과제 합계

**원인**: 로그인하지 않았거나 토큰 만료  - 최종 백분율: 0.00-100.00% (소수점 둘째자리 반올림)

**해결**: `await login()` 실행 후 재시도

#### 3. 지각 페널티 확인

### 문제 3: "enrollmentIdx 조회 실패"

**원인**: 해당 학생이 강의에 수강 등록되지 않음  ```javascript

**해결**: `await gradePhase3.listStudents()`로 실제 수강생 확인// 지각 페널티 테스트

// 1. 성적 구성 설정 시 latePenaltyPerSession 파라미터 확인

### 문제 4: "lecSerial not found"gradeTests.configureGrade({

**원인**: 존재하지 않는 강의 코드    latePenaltyPerSession: 0.5  // 지각 1회당 0.5점 감점

**해결**: 실제 강의 코드 사용 (예: ETH201, CS101)});



---// 2. 출석 업데이트 (지각 포함)

// PUT /api/enrollments/{enrollmentIdx}/attendance

## 📊 성능 참고// Body: { attended: 30, late: 5, absent: 5 }



| 테스트 | 소요 시간 | 비고 |// 3. 성적 조회로 페널티 적용 확인

|--------|-----------|------|gradeTests.professorGradeView().then(data => {

| Phase 1 전체 | 10-15초 | 5개 API 순차 실행 |  console.log('지각 횟수:', data.lateCount);

| Phase 3 전체 | 5-10초 | 2개 API + 1초 대기 |  console.log('출석 백분율:', data.attendancePercentage);  // (30+5)/80 * 100 = 43.75%

| 개별 테스트 | 1-3초 | 단일 API 호출 |  console.log('지각 페널티:', data.lateCount * 0.5);       // 5 * 0.5 = 2.5점

  console.log('최종 출석 점수:', data.attendanceScore);    // 백분율 점수 - 2.5점

---});

```

## 💡 유용한 팁

#### 4. 등급 배정 확인

### Tip 1: 학생 목록 먼저 조회

```javascript```javascript

// Phase 3 테스트 전 학생 목록 확인// 최종 등급 배정 테스트

await gradePhase3.listStudents()gradeTests.finalizeGrades().then(data => {

// → studentIdx 확인 후 setLecture() 호출  console.log('배정된 학생 수:', data.students.length);

```  

  // 등급별 인원 확인

### Tip 2: 설정 백업  const grades = data.students.reduce((acc, s) => {

```javascript    acc[s.grade] = (acc[s.grade] || 0) + 1;

// 현재 설정 저장    return acc;

const backup = gradePhase1.getConfig()  }, {});

  

// 나중에 복원  console.log('등급 분포:', grades);

gradePhase1.setLecture(backup.lecSerial, backup.studentIdx)  // 예: { A+: 3, A: 5, B+: 7, B: 10, C: 5, D: 2, F: 3 }

```});

```

### Tip 3: 서버 로그 확인

```text**등급 배정 규칙 확인**:

# 이벤트 처리 로그 확인- ✅ 60% 미만 → F 등급

[INFO] 성적 재계산 시작: lecIdx=6, studentIdx=6- ✅ 60% 이상 → A+, A, B+, B, C, D 비율 적용

[INFO] 성적 재계산 완료: updateType=ATTENDANCE- ✅ 동점자 → 같은 등급 배정

```- ✅ 하위 침범 방식 적용



---#### 5. 이벤트 시스템 확인



## 🎯 다음 단계서버 로그에서 다음 메시지 확인:



1. ✅ Phase 1 테스트 완료```

2. ✅ Phase 3 테스트 완료[INFO] 출석 업데이트로 인한 성적 재계산 이벤트 발행: lecIdx=1, studentIdx=2

3. ⏳ 실제 데이터로 검증[INFO] 과제 채점으로 인한 성적 재계산 이벤트 발행: lecIdx=1, studentIdx=2

4. ⏳ 프론트엔드 통합[INFO] GradeUpdateEventListener - 성적 재계산 시작: lecIdx=1, studentIdx=2, type=ATTENDANCE

[INFO] GradeUpdateEventListener - 성적 재계산 완료: lecIdx=1, studentIdx=2

---```



> **참고**: 자세한 사용법은 [브라우저 콘솔 테스트 가이드](../브라우저콘솔테스트/04-grade/06-USAGE-GUIDE.md) 참조**이벤트 동작 확인**:

- ✅ 출석 업데이트 시 이벤트 발행
- ✅ 과제 채점 시 이벤트 발행
- ✅ 비동기 재계산 실행
- ✅ 에러 없이 완료

---

## 🐛 5단계: 문제 해결

### 컴파일 에러

#### "Cannot resolve symbol" 에러

```powershell
# Maven
mvn clean install -U

# Gradle
.\gradlew clean build --refresh-dependencies
```

#### Import 에러

- `GradeUpdateEvent`가 없다면:
  - 파일 위치 확인: `src/main/java/BlueCrab/com/example/event/GradeUpdateEvent.java`
  - 패키지 경로 확인: `package BlueCrab.com.example.event;`

### 런타임 에러

#### NullPointerException

1. **Autowired 실패**:
   - `@Service` 어노테이션 확인
   - 컴포넌트 스캔 범위 확인

2. **데이터 없음**:
   - DB에 강의 데이터 존재 확인
   - 수강신청 데이터 존재 확인

#### JSON 파싱 에러

```java
// ENROLLMENT_DATA 컬럼에 유효한 JSON 확인
SELECT ENROLLMENT_DATA FROM ENROLLMENT_EXTENDED_TBL WHERE LEC_IDX = 1;
```

### 성적 계산 오류

#### 백분율이 100% 초과

- 과제 maxScore 확인 (기본값: 10)
- 출석 maxScore 확인 (기본값: 80)

#### 지각 페널티 미적용

- `gradeConfig`에 `latePenaltyPerSession` 설정 확인
- `GradeCalculationService`에서 페널티 계산 로직 확인

---

## 📊 6단계: 완료 체크리스트

### 빌드 & 실행

- [ ] Maven/Gradle 빌드 성공
- [ ] 컴파일 에러 없음
- [ ] 서버 정상 시작
- [ ] 포트 충돌 없음 (8080)

### API 테스트

- [ ] POST `/api/enrollments/grade-config` (성적 구성 설정)
- [ ] GET `/api/enrollments/professor/grade` (교수용 성적 조회)
- [ ] GET `/api/enrollments/grade-list` (전체 성적 목록)
- [ ] POST `/api/enrollments/finalize-grades` (최종 등급 배정)

### 성적 계산

- [ ] 출석 점수 계산 정확
- [ ] 출석 백분율 계산 정확 (지각 포함)
- [ ] 지각 페널티 적용 확인
- [ ] 과제 점수 계산 정확
- [ ] 총점/백분율 계산 정확
- [ ] 소수점 둘째자리 반올림 확인

### 등급 배정

- [ ] 60% 미만 F 등급 배정
- [ ] 60% 이상 상대평가 적용
- [ ] 등급 비율 정확
- [ ] 동점자 처리 정확
- [ ] 하위 침범 방식 동작

### 이벤트 시스템

- [ ] 출석 업데이트 시 이벤트 발행
- [ ] 과제 채점 시 이벤트 발행
- [ ] 비동기 재계산 실행
- [ ] 서버 로그에 이벤트 메시지 출력
- [ ] 에러 없이 완료

---

## 🎉 완료!

모든 체크리스트가 완료되면 **성적 관리 시스템 Phase 4 완료**!

다음 단계:
- ✅ 백업G 문서 최종 업데이트
- ✅ 작업 완료 보고서 작성 (선택)
- ✅ 다음 작업으로 이동
