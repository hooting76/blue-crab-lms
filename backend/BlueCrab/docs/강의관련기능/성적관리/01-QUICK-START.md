# 📦 빌드 & 테스트

Phase 4: 서버 빌드 및 브라우저 테스트

---

## 🔧 1단계: 서버 빌드

### Maven 사용 시

```powershell
# 프로젝트 루트로 이동
cd f:\main_project\team_work\blue-crab-lms\backend\BlueCrab

# 클린 빌드 (테스트 스킵)
mvn clean package -DskipTests

# 또는 컴파일만
mvn clean compile
```

### Gradle 사용 시

```powershell
# 프로젝트 루트로 이동
cd f:\main_project\team_work\blue-crab-lms\backend\BlueCrab

# 클린 빌드 (테스트 스킵)
.\gradlew clean build -x test

# 또는 컴파일만
.\gradlew clean compileJava
```

### ✅ 빌드 성공 확인

컴파일 성공 메시지 확인:

```
BUILD SUCCESS
```

또는

```
BUILD SUCCESSFUL
```

### ❌ 빌드 실패 시

1. **컴파일 에러 확인**:
   - 빌드 로그에서 에러 메시지 확인
   - 파일 경로와 라인 번호 확인

2. **의존성 문제**:
   ```powershell
   # Maven
   mvn dependency:resolve
   
   # Gradle
   .\gradlew dependencies
   ```

3. **캐시 삭제**:
   ```powershell
   # Maven
   mvn clean
   
   # Gradle
   .\gradlew clean
   ```

---

## 🚀 2단계: 서버 실행

### Spring Boot 실행

```powershell
# Maven
mvn spring-boot:run

# Gradle
.\gradlew bootRun

# JAR 파일 실행
java -jar target/BlueCrab-0.0.1-SNAPSHOT.jar
```

### ✅ 서버 실행 확인

서버가 정상적으로 시작되면 다음 로그 확인:

```
Started BlueCrabApplication in X.XXX seconds
```

---

## 🧪 3단계: 브라우저 콘솔 테스트

### 테스트 페이지 접속

1. 브라우저에서 다음 URL 접속:
   ```
   http://localhost:8080
   ```

2. 로그인 (교수 계정 사용)

### 테스트 스크립트 로드

브라우저 개발자 도구(F12) → Console 탭에서 다음 실행:

```javascript
// 테스트 스크립트 로드
const script = document.createElement('script');
script.src = '/test/04-grade/grade-management-test.js';
script.onload = () => console.log('✅ 성적 관리 테스트 스크립트 로드 완료');
script.onerror = () => console.error('❌ 스크립트 로드 실패');
document.head.appendChild(script);
```

### 전체 테스트 실행

```javascript
// 모든 테스트 실행
gradeTests.runAll();
```

### 개별 테스트 실행

```javascript
// 1. 성적 구성 설정 테스트
gradeTests.configureGrade();

// 2. 교수용 성적 조회 테스트
gradeTests.professorGradeView();

// 3. 전체 성적 목록 조회 테스트
gradeTests.gradeList();

// 4. 최종 등급 배정 테스트
gradeTests.finalizeGrades();

// 5. 이벤트 시스템 테스트 (출석 업데이트)
gradeTests.testAttendanceEvent();

// 6. 이벤트 시스템 테스트 (과제 채점)
gradeTests.testAssignmentEvent();
```

---

## ✅ 4단계: 최소 검증

### 필수 확인 사항

#### 1. API 응답 확인

각 API 호출 후 다음 확인:

- ✅ 상태 코드 200 (성공)
- ✅ 응답 데이터 존재
- ✅ 에러 없음

#### 2. 성적 계산 정확성

```javascript
// 콘솔에서 수동 확인
// lecSerial 사용 예시
gradePhase1.setLecture('ETH201', 100)  // lecSerial, studentIdx
await gradePhase1.professorView()

// 또는 lecIdx 사용
gradePhase1.config.lecIdx = 1
gradePhase1.config.studentIdx = 100
await gradePhase1.professorView()
```

**예상 결과**:
- 출석 점수: 0-80점 (또는 설정한 maxScore)
- 출석 백분율: 0.00-100.00%
- 과제 점수: 각 과제별 0-10점
- 총점: 출석 + 과제 합계
- 최종 백분율: 0.00-100.00% (소수점 둘째자리 반올림)

#### 3. 지각 페널티 확인

```javascript
// 지각 페널티 테스트
// 1. 성적 구성 설정 시 latePenaltyPerSession 파라미터 확인
gradeTests.configureGrade({
  latePenaltyPerSession: 0.5  // 지각 1회당 0.5점 감점
});

// 2. 출석 업데이트 (지각 포함)
// PUT /api/enrollments/{enrollmentIdx}/attendance
// Body: { attended: 30, late: 5, absent: 5 }

// 3. 성적 조회로 페널티 적용 확인
gradeTests.professorGradeView().then(data => {
  console.log('지각 횟수:', data.lateCount);
  console.log('출석 백분율:', data.attendancePercentage);  // (30+5)/80 * 100 = 43.75%
  console.log('지각 페널티:', data.lateCount * 0.5);       // 5 * 0.5 = 2.5점
  console.log('최종 출석 점수:', data.attendanceScore);    // 백분율 점수 - 2.5점
});
```

#### 4. 등급 배정 확인

```javascript
// 최종 등급 배정 테스트
gradeTests.finalizeGrades().then(data => {
  console.log('배정된 학생 수:', data.students.length);
  
  // 등급별 인원 확인
  const grades = data.students.reduce((acc, s) => {
    acc[s.grade] = (acc[s.grade] || 0) + 1;
    return acc;
  }, {});
  
  console.log('등급 분포:', grades);
  // 예: { A+: 3, A: 5, B+: 7, B: 10, C: 5, D: 2, F: 3 }
});
```

**등급 배정 규칙 확인**:
- ✅ 60% 미만 → F 등급
- ✅ 60% 이상 → A+, A, B+, B, C, D 비율 적용
- ✅ 동점자 → 같은 등급 배정
- ✅ 하위 침범 방식 적용

#### 5. 이벤트 시스템 확인

서버 로그에서 다음 메시지 확인:

```
[INFO] 출석 업데이트로 인한 성적 재계산 이벤트 발행: lecIdx=1, studentIdx=2
[INFO] 과제 채점으로 인한 성적 재계산 이벤트 발행: lecIdx=1, studentIdx=2
[INFO] GradeUpdateEventListener - 성적 재계산 시작: lecIdx=1, studentIdx=2, type=ATTENDANCE
[INFO] GradeUpdateEventListener - 성적 재계산 완료: lecIdx=1, studentIdx=2
```

**이벤트 동작 확인**:
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
