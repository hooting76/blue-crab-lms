# 🧪 브라우저 콘솔 테스트 가이드# 🧪 브라우저 콘솔 테스트 가이드# 🧪 Phase 4 테스트 가이드



성적 관리 시스템을 브라우저에서 테스트하는 완전 가이드입니다.



------성적 관리 시스템을 브라우저에서 테스트하는 완전 가이드입니다.성적 관리 시스템 브라우저 콘솔 테스트 완전 가이드입니다.



## 📖 목차



- [사전 준비](#사전-준비)------

- [테스트 순서](#테스트-순서)

- [결과 검증](#결과-검증)



------## 📖 목차## 📖 목차



## 📋 사전 준비



### 체크리스트- [사전 준비](#-사전-준비)- [사전 준비](#-사전 준비)



- [ ] Eclipse에서 서버 실행 중 (Spring Boot)- [테스트 실행](#-테스트-실행)- [테스트 실행](#-테스트-실행)

- [ ] 브라우저(Chrome/Edge) 준비

- [ ] 교수 계정 정보 준비- [예상 결과](#-예상-결과)- [결과 검증](#-결과-검증)



------- [문제 해결](#-문제-해결)- [성공 기준](#-성공-기준)



## 🚀 테스트 순서



### 1단계: 브라우저 개발자 도구 열기------



1. **사이트 접속**: `https://bluecrab.chickenkiller.com`

2. **F12** 키 → Console 탭

3. **Console 탭 활성화**## 📋 사전 준비## 📋 사전 준비



### 2단계: 교수 계정 로그인



```javascript### 체크리스트### 체크리스트

// 1. 로그인 스크립트 로드

// 00-login/user-login.js 복사 → 콘솔 붙여넣기



// 2. 로그인 실행- [ ] 서버 실행 (Spring Boot)- [ ] Eclipse에서 프로젝트 빌드 완료 (`clean package`)

await login()

- [ ] Chrome/Edge 브라우저- [ ] 서버 실행 중 (`Spring Boot Application` 실행)

// 3. 성공 확인

// ✅ 로그인 성공!- [ ] 강의 코드 준비 (예: ETH201)- [ ] 브라우저(Chrome/Edge) 준비

// 🎓 교수 계정으로 로그인되었습니다.

```- [ ] 학생 IDX 준비 (예: 6)- [ ] 교수 계정 정보 준비 (이메일, 비밀번호



### 3단계: Phase 1 테스트



```javascript------

// 1. 파일 로드

// 01-grade-phase1-tests.js 복사 → 콘솔 붙여넣기



// 2. 로드 확인## 🚀 테스트 실행## 🚀 테스트 실행 순서

typeof gradePhase1  // → "object"



// 3. 강의 설정

gradePhase1.setLecture('ETH201')### 1️⃣ 브라우저 개발자 도구 열기### ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━



// 4. 전체 테스트### 1단계: 브라우저 개발자 도구 열기

await gradePhase1.runAll()

```1. 사이트 접속: `https://bluecrab.chickenkiller.com`### ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━



**예상 소요 시간**: 10-15초2. **F12** 키 → Console 탭



**실행 테스트**:1. **브라우저에서 사이트 접속**

1. 성적 구성 설정

2. 학생 성적 조회### 2️⃣ 교수 로그인   ```

3. 교수용 성적 조회

4. 성적 목록 조회   https://bluecrab.chickenkiller.com

5. 최종 등급 배정

#### 로그인 스크립트 로드   ```

### 4단계: Phase 3 테스트 (선택)



```javascript

// 1. 파일 로드```2. **개발자 도구 열기**

// 02-grade-phase3-tests.js 복사 → 콘솔 붙여넣기

VSCode에서 복사:   - **F12** 키 누르기

// 2. 로드 확인

typeof gradePhase3  // → "object"docs/강의관련기능/브라우저콘솔테스트/00-login/user-login.js   - 또는 **Ctrl + Shift + I**



// 3. 수강생 목록 확인 (선택)   - 또는 마우스 우클릭 → **검사**

gradePhase3.setLecture('ETH201')

await gradePhase3.listStudents()→ 브라우저 콘솔에 붙여넣기 + Enter



// 4. 강의+학생 설정```3. **Console 탭으로 이동**

gradePhase3.setLecture('ETH201', 6)

   - 상단 탭에서 **Console** 클릭

// 5. 전체 테스트

await gradePhase3.runAll()#### 로그인 실행   - 하단 입력창이 활성화됨

```



**예상 소요 시간**: 5-10초

```javascript---

**실행 테스트**:

1. 출석 업데이트 → 성적 재계산await login()

2. 과제 점수 → 성적 재계산

```### ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

------

### 2단계: 교수 계정 로그인 ⭐

## 📊 결과 검증

**교수 계정 입력**:### ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

### Phase 1 성공 예시

- 이메일 입력 → 비밀번호 입력

```

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━#### A. 로그인 스크립트 로드

🎉 전체 테스트 완료!

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━**성공 확인**:

📊 최종 통계:

   총: 5개```1. **VSCode에서 파일 열기**

   ✅ 성공: 5개

   ❌ 실패: 0개✅ 로그인 성공!   ```

   성공률: 100%

```🎓 교수 계정으로 로그인되었습니다.   docs/강의관련기능/브라우저콘솔테스트/00-login/user-login.js



### Phase 3 성공 예시```   ```



```

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

📊 Phase 3 결과---2. **파일 전체 복사**

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

총: 2개   - **Ctrl + A** (전체 선택)

✅ 성공: 2개

❌ 실패: 0개### 3️⃣ 테스트 스크립트 로드   - **Ctrl + C** (복사)

📈 성공률: 100.0%

```



------```3. **브라우저 콘솔에 붙여넣기**



## 📚 관련 문서VSCode에서 복사:   - 브라우저 Console 탭 클릭



- [빠른 시작](./05-QUICK-START.md) - 5분 퀵 스타트docs/강의관련기능/브라우저콘솔테스트/04-grade/01-grade-phase1-tests.js   - **Ctrl + V** (붙여넣기)

- [사용법 가이드](./06-USAGE-GUIDE.md) - 전체 기능 설명

- [문제 해결](./09-TROUBLESHOOTING.md) - 오류 해결   - **Enter** 키 (실행)


→ 브라우저 콘솔에 붙여넣기 + Enter

```4. **로드 확인**

   ```

**로드 확인**:   콘솔 출력:

```javascript   🔐 일반 유저 로그인

typeof gradePhase1  // → "object"   ===================

```   사용 방법:

   1. await login()    - 로그인

---   ...

   ```

### 4️⃣ 전체 테스트 실행

#### B. 로그인 실행

```javascript

gradePhase1.setLecture('ETH201')1. **로그인 함수 실행**

await gradePhase1.runAll()   ```javascript

```   await login()

   ```

**예상 소요 시간**: 10-15초

2. **교수 계정 정보 입력**

**실행 항목**:   - 첫 번째 입력창: **이메일** 입력

1. 성적 구성 설정     ```

2. 학생 성적 조회 (ETH201, 학생 6)     예: professor@example.com

3. 교수용 성적 조회     ```

4. 성적 목록 조회   - 두 번째 입력창: **비밀번호** 입력

5. 최종 등급 배정

3. **로그인 성공 확인**

---   ```

   콘솔 출력:

### 5️⃣ 개별 테스트 (선택)   ✅ 로그인 성공!

   메시지: 로그인에 성공했습니다

#### 성적 구성 설정   사용자 정보: { userId: 100, userName: "홍길동", ... }

   🎓 교수 계정으로 로그인되었습니다.

```javascript   ```

gradePhase1.setLecture('ETH201')

4. **상태 확인 (선택)**

// 간편 설정   ```javascript

gradePhase1.quickAttendanceConfig(80, 0.5)   checkStatus()

await gradePhase1.config()   ```

```   ```

   예상 출력:

#### 학생 성적 조회   📋 현재 상태:

   로그인됨: true

```javascript   사용자: 홍길동

gradePhase1.setLecture('ETH201', 6)   유형: 교수

await gradePhase1.studentInfo()   ```

```

---

#### 교수용 성적 조회

### ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

```javascript### 3단계: 성적 관리 테스트 모듈 로드 (단계별)

gradePhase1.setLecture('ETH201', 6)### ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

await gradePhase1.professorView()

```**중요**: 반드시 **순서대로** 로드하고, **각 단계마다 테스트**하세요!



#### 성적 목록 조회#### ① `01-grade-test-utils.js` 로드



```javascript1. **VSCode에서 파일 열기**

gradePhase1.setLecture('ETH201')   ```

await gradePhase1.gradeList()   docs/강의관련기능/브라우저콘솔테스트/04-grade/01-grade-test-utils.js

```   ```



#### 최종 등급 배정2. **전체 복사 → 브라우저 콘솔에 붙여넣기 → Enter**



```javascript3. **로드 확인**

gradePhase1.setLecture('ETH201')   ```javascript

await gradePhase1.finalize()   window.gradeTestUtils

```   ```

   ```

---   예상 출력:

   {checkAuth: ƒ, apiCall: ƒ, apiGet: ƒ, apiPut: ƒ, testData: {...}, ...}

## 📊 예상 결과   ```



### ✅ 전체 테스트 성공4. **✅ 확인 완료하면 다음 단계로!**



```---

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

🎉 전체 테스트 완료!#### ② `02-grade-phase1-tests.js` 로드

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

📊 최종 통계:1. **VSCode에서 파일 열기**

   총 테스트: 5개   ```

   성공: 5개   docs/강의관련기능/브라우저콘솔테스트/04-grade/02-grade-phase1-tests.js

   실패: 0개   ```

   성공률: 100%

```2. **전체 복사 → 브라우저 콘솔에 붙여넣기 → Enter**



### 개별 테스트 출력 예시3. **로드 확인**

   ```javascript

**성적 구성 설정**:   window.gradePhase1Tests

```   ```

🧪 테스트 1/5: 성적 구성 설정   ```

POST /api/enrollments/grade-config   예상 출력:

✅ HTTP 200 - 성공   {testGradeConfig: ƒ, testStudentGradeInfo: ƒ, ..., runPhase1Tests: ƒ}

📝 메시지: 성적 구성이 성공적으로 저장되었습니다.   ```

```

4. **개별 테스트 실행 (선택)** - 각 기능 확인

**학생 성적 조회**:   ```javascript

```   // 성적 구성 설정

🧪 테스트 2/5: 학생 성적 조회   await window.gradePhase1Tests.testGradeConfig()

GET /api/enrollments/ETH201/6/grade   ```

✅ HTTP 200 - 성공

📊 성적 상세:5. **✅ 확인 완료하면 다음 단계로!**

  📅 출석: 75.5 / 80 (94.4%)

  📝 과제: 91.0 / 100 (91.0%)---

  📈 총점: 166.5 / 180 (92.5%)

```#### ③ `03-grade-phase3-tests.js` 로드



**최종 등급 배정**:1. **VSCode에서 파일 열기**

```   ```

🧪 테스트 5/5: 최종 등급 배정   docs/강의관련기능/브라우저콘솔테스트/04-grade/03-grade-phase3-tests.js

POST /api/enrollments/finalize-grades   ```

✅ HTTP 200 - 성공

📊 등급 배정 완료:2. **전체 복사 → 브라우저 콘솔에 붙여넣기 → Enter**

  A: 8명 (30%)

  B: 10명 (40%)3. **로드 확인**

  C: 5명 (20%)   ```javascript

  D: 2명 (10%)   window.gradePhase3Tests

  F: 0명   ```

```   ```

   예상 출력:

---   {testAttendanceUpdate: ƒ, testAssignmentGrade: ƒ, runPhase3Tests: ƒ}

   ```

## 🔧 문제 해결

4. **개별 테스트 실행 (선택)** - 이벤트 확인

### ❌ "로그인이 필요합니다"   ```javascript

   // 출석 업데이트 (이벤트 발행)

**원인**: 토큰 만료 또는 로그인 안 됨   await window.gradePhase3Tests.testAttendanceUpdate()

   ```

**해결**:   

```javascript   **Eclipse Console 확인**: `[GradeUpdateEventListener]` 로그 확인!

await login()  // 재로그인

```5. **✅ 확인 완료하면 다음 단계로!**



------



### ❌ "404 Not Found"#### ④ `04-grade-test-runner.js` 로드



**원인**: 강의 코드 오류 또는 서버 미실행1. **VSCode에서 파일 열기**

   ```

**해결**:   docs/강의관련기능/브라우저콘솔테스트/04-grade/04-grade-test-runner.js

1. 강의 코드 확인 (DB에 존재하는지)   ```

2. 서버 재시작 (Eclipse Console 확인)

2. **전체 복사 → 브라우저 콘솔에 붙여넣기 → Enter**

---

3. **로드 확인**

### ❌ "Cannot read property of undefined"   ```javascript

   window.gradeTests

**원인**: 스크립트 로드 실패   ```

   ```

**해결**:   예상 출력:

1. 01-grade-phase1-tests.js 재로드   {config: ƒ, studentInfo: ƒ, ..., runAll: ƒ, scenario: ƒ}

2. 로드 확인: `typeof gradePhase1`   ```



---4. **사용법 안내 출력**

   ```

### ❌ CORS 에러   🎯 성적 관리 테스트 통합 인터페이스 준비 완료!

   ═════════════════════════════════════════

**원인**: 다른 도메인에서 접속   📝 개별 테스트:

   ...

**해결**:   ```

https://bluecrab.chickenkiller.com 에서 테스트

5. **✅ 이제 통합 인터페이스로 테스트 가능!**

---

---

## 🎯 성공 기준

**💡 팁: 단계별 진행의 장점**

### ✅ 완전 성공

- ✅ **각 모듈 로드 확인 가능** - 오류 발생 시 즉시 수정

- ✅ 로그인 성공- ✅ **개별 기능 테스트 가능** - 문제 있는 부분만 확인

- ✅ 스크립트 로드 완료- ✅ **디버깅 용이** - 어느 단계에서 문제인지 명확

- ✅ Phase 1 테스트 5/5 성공- ✅ **학습 효과** - 각 모듈의 역할 이해

- ✅ 통계: 5/5 성공 (100%)

---

---

### ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

## 📚 다음 단계### 4단계: 테스트 실행 🚀

### ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

- [빠른 시작 가이드](./05-QUICK-START.md)

- [상세 사용법](./06-USAGE-GUIDE.md)#### 옵션 A: 전체 테스트 실행 (추천) ⭐

- [문제 해결](./09-TROUBLESHOOTING.md)

```javascript
await gradeTests.runAll()
```

**예상 소요 시간**: 약 15-20초

**실행 순서**:
1. Phase 1 테스트 5개 (순차 실행)
2. 3초 대기
3. Phase 3 테스트 2개 (이벤트 포함)

**예상 출력**:
```
🎯 전체 테스트 실행 시작...
══════════════════════════════════════

📊 Phase 1: 핵심 기능 테스트 (5개)
──────────────────────────────────────

🧪 테스트 1/5: 성적 구성 설정
POST https://bluecrab.chickenkiller.com/BlueCrab-1.0.0/api/enrollments/grade-config
✅ HTTP 200 - 성공
...

⏳ Phase 1 완료. 3초 후 Phase 3 시작...

📊 Phase 3: 이벤트 시스템 테스트 (2개)
──────────────────────────────────────

🧪 테스트 1/2: 출석 업데이트 (이벤트 발행)
PUT https://bluecrab.chickenkiller.com/BlueCrab-1.0.0/api/enrollments/1/attendance
✅ HTTP 200 - 성공
🔔 이벤트 발행됨: ATTENDANCE
...

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
🎉 전체 테스트 완료!
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
📊 최종 통계:
   총 테스트: 7개
   성공: 7개
   실패: 0개
   성공률: 100%
```

#### 옵션 B: Phase별 실행

**Phase 1만 실행** (핵심 기능 5개):
```javascript
await gradeTests.phase1()
```

**Phase 3만 실행** (이벤트 시스템 2개):
```javascript
await gradeTests.phase3()
```

#### 옵션 C: 개별 테스트 실행

```javascript
// 1. 성적 구성 설정
await gradeTests.config()

// 2. 학생 성적 조회
await gradeTests.studentInfo()

// 3. 교수용 성적 조회
await gradeTests.professorView()

// 4. 성적 목록 조회
await gradeTests.gradeList()

// 5. 최종 등급 배정
await gradeTests.finalize()

// 6. 출석 업데이트 (이벤트)
await gradeTests.attendance()

// 7. 과제 채점 (이벤트)
await gradeTests.assignment()
```

#### 옵션 D: 시나리오 테스트 (전체 워크플로우)

```javascript
await gradeTests.scenario()
```

**6단계 워크플로우**:
1. 성적 구성 설정
2. 출석 기록 (이벤트 발행)
3. 과제 채점 (이벤트 발행)
4. 학생 성적 확인
5. 전체 성적 목록 조회
6. 최종 등급 배정

---

### ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
### 5단계: Eclipse 서버 로그 확인 (이벤트 검증) ⭐
### ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

Phase 3 테스트 실행 후 **Eclipse Console**에서 확인:

#### 확인할 로그 1: 출석 업데이트 이벤트

```
[GradeUpdateEventListener] 성적 업데이트 이벤트 수신
- 타입: ATTENDANCE
- 수강 ID: 1
- 상세 정보: Attendance updated
```

#### 확인할 로그 2: 과제 채점 이벤트

```
[GradeUpdateEventListener] 성적 업데이트 이벤트 수신
- 타입: ASSIGNMENT
- 수강 ID: 1
- 상세 정보: Assignment graded
```

#### 로그 찾기 팁

Eclipse Console에서:
1. **Ctrl + F** (검색)
2. 검색어: `GradeUpdateEventListener`
3. 총 **2개의 로그**가 있어야 함

---

### ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
### 6단계: 결과 검증
### ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

#### ✅ 브라우저 콘솔 체크리스트

- [ ] **Phase 1 테스트 5개 모두 성공**
  - [ ] 성적 구성 설정 - `200 OK`
  - [ ] 학생 성적 조회 - 출석/과제/총점 정상
  - [ ] 교수용 성적 조회 - 학생 목록 정상
  - [ ] 성적 목록 조회 - 페이징 정상
  - [ ] 최종 등급 배정 - 등급 분포 정상

- [ ] **Phase 3 테스트 2개 모두 성공**
  - [ ] 출석 업데이트 - `200 OK`
  - [ ] 과제 채점 - `200 OK`

- [ ] **통계 확인**
  - [ ] 총 테스트: 7개
  - [ ] 성공: 7개
  - [ ] 실패: 0개
  - [ ] 성공률: 100%

#### ✅ Eclipse 서버 로그 체크리스트

- [ ] **이벤트 리스너 실행 확인**
  - [ ] `[GradeUpdateEventListener]` 로그 **2회** 출력
  - [ ] ATTENDANCE 타입 이벤트 **1회**
  - [ ] ASSIGNMENT 타입 이벤트 **1회**

---

## 🔧 문제 해결

### ❌ 문제 1: "로그인이 필요합니다"

**증상**:
```
⚠️ 로그인이 필요합니다!
🔧 docs/일반유저 로그인+게시판/test-1-login.js 실행
   await login() (교수 계정 사용)
```

**원인**: 로그인 안 됨 또는 토큰 만료

**해결**:
1. 2단계로 돌아가서 `await login()` 재실행
2. 교수 계정으로 로그인

---

### ❌ 문제 2: "Cannot read property of undefined"

**증상**:
```
Uncaught TypeError: Cannot read property 'checkAuth' of undefined
```

**원인**: 모듈 로드 순서 오류

**해결**:
1. 3단계로 돌아가기
2. **반드시 순서대로** 모듈 로드:
   - ① utils → ② phase1 → ③ phase3 → ④ runner

---

### ❌ 문제 3: "404 Not Found"

**증상**:
```
❌ HTTP 404 - Not Found
URL: https://bluecrab.chickenkiller.com/BlueCrab-1.0.0/api/enrollments/...
```

**원인**: 
- 서버 미실행
- 빌드 미완료
- API 엔드포인트 오류

**해결**:
1. Eclipse에서 서버 실행 확인
2. Console에서 에러 확인
3. 필요시 서버 재시작

---

### ❌ 문제 4: 이벤트 로그가 안 보임

**증상**:
Eclipse Console에서 `[GradeUpdateEventListener]` 로그 없음

**원인**:
1. 빌드에 최신 코드 미반영
2. 이벤트 리스너 미등록
3. 비동기 처리 대기 시간 부족

**해결**:
1. **Eclipse에서 재빌드**
   - 프로젝트 우클릭 → Run As → Maven build
   - Goals: `clean package`
   
2. **서버 재시작**
   - 서버 Stop → Start

3. **테스트 재실행**
   - 브라우저에서 `await gradeTests.phase3()` 재실행
   
4. **Eclipse Console 확인**
   - Ctrl + F → 검색: `GradeUpdateEventListener`

---

### ❌ 문제 5: "CORS policy" 오류

**증상**:
```
Access to fetch at '...' from origin '...' has been blocked by CORS policy
```

**원인**: CORS 설정 문제

**해결**:
1. 서버의 CORS 설정 확인
2. 같은 도메인에서 테스트 (https://bluecrab.chickenkiller.com)

---

## 📊 테스트 데이터 확인/수정

### 현재 테스트 데이터 확인

```javascript
gradeTests.getData()
```

**출력 예시**:
```javascript
{
    lecIdx: 1,
    studentIdx: 100,
    professorIdx: 22,
    enrollmentIdx: 1,
    assignmentIdx: 1,
    passingThreshold: 60.0,
    attendanceMaxScore: 80,
    assignmentTotalMaxScore: 100,
    latePenaltyPerSession: 0.5,
    gradeDistribution: { "A+": 10, "A": 15, ... }
}
```

### 테스트 데이터 수정

```javascript
gradeTests.setData({
    lecIdx: 2,              // 강의 ID 변경
    studentIdx: 101,        // 학생 ID 변경
    attendanceMaxScore: 100 // 출석 만점 변경
})
```

### 커스텀 데이터로 테스트

```javascript
await gradeTests.customTest()
```

---

## 🎯 성공 기준

### ✅ 완전 성공 (100%)

- ✅ 로그인 성공
- ✅ 모듈 4개 모두 로드 완료
- ✅ Phase 1 테스트 5/5 성공
- ✅ Phase 3 테스트 2/2 성공
- ✅ 이벤트 로그 2개 확인
- ✅ 통계: 7/7 성공 (100%)

---

## � 최종 보고

**Phase 4 테스트 완료 시:**

```
✅ Phase 1: 5/5 성공 (100%)
✅ Phase 3: 2/2 성공 (100%)
✅ 이벤트: 2/2 발행 (100%)
```

**검증 완료 항목:**
- 성적 구성 설정, 학생 성적 조회, 교수용 성적 조회
- 성적 목록 조회, 최종 등급 배정
- 출석 업데이트 + 이벤트, 과제 채점 + 이벤트
