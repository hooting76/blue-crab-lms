# 🔧 문제 해결 가이드# 🔧 문제 해결 가이드# 🔧 문제 해결 가이드



성적 관리 시스템 테스트 중 발생할 수 있는 문제와 해결 방법입니다.



------성적 관리 테스트 중 발생할 수 있는 문제와 해결 방법입니다.성적 관리 시스템 테스트 중 발생할 수 있는 문제와 해결 방법입니다.



## 📖 목차



- [로그인 관련](#로그인-관련)------

- [모듈 로드 오류](#모듈-로드-오류)

- [API 호출 오류](#api-호출-오류)



------## 📖 목차## 📖 목차



## 🔐 로그인 관련



### ❌ 문제 1: "로그인이 필요합니다"- [로그인 문제](#-로그인-문제)- [로그인 관련](#-로그인-관련)



**증상:**- [스크립트 로드 문제](#-스크립트-로드-문제)- [모듈 로드 오류](#-모듈-로드-오류)

```

⚠️ 로그인이 필요합니다!- [API 호출 오류](#-api-호출-오류)- [API 호출 오류](#-api-호출-오류)

```

- [데이터 문제](#-데이터-문제)- [이벤트 시스템 오류](#-이벤트-시스템-오류)

**원인:**

- 로그인 안 됨- [기타 문제](#-기타-문제)

- JWT 토큰 만료

- 세션 타임아웃---



**해결:**---

```javascript

// 1. 재로그인## 🔐 로그인 문제

await login()

## 🔐 로그인 관련

// 2. 로그인 상태 확인

checkStatus()### ❌ "로그인이 필요합니다"

```

### ❌ 문제 1: "로그인이 필요합니다"

### ❌ 문제 2: "교수 권한이 필요합니다"

**증상:**

**증상:**

``````**증상:**

❌ HTTP 403 - Forbidden

메시지: 교수만 접근 가능합니다⚠️ 로그인이 필요합니다!```

```

```⚠️ 로그인이 필요합니다!

**원인:**

- 학생 계정으로 로그인🔧 docs/일반유저 로그인+게시판/test-1-login.js 실행

- 권한 부족

**원인:**   await login() (교수 계정 사용)

**해결:**

```javascript- 로그인 안 됨```

// 1. 로그아웃

await logout()- JWT 토큰 만료



// 2. 교수 계정으로 재로그인**원인:**

await login()

**해결:**- 로그인 안 됨

// 3. 권한 확인

checkStatus()```javascript- JWT 토큰 만료

// → 유형: 교수 확인

```await login()  // 재로그인- 세션 타임아웃



------```



## 🧩 모듈 로드 오류**해결 방법:**



### ❌ 문제 3: "Cannot read property of undefined"---



**증상:**```javascript

```

Uncaught TypeError: Cannot read property 'setLecture' of undefined### ❌ "교수 권한이 필요합니다"// 1. 교수 계정으로 로그인

```

await login()

**원인:**

- 스크립트 로드 실패**증상:**

- 파일 경로 오류

```// 2. 로그인 상태 확인

**해결:**

```javascript❌ HTTP 403 - ForbiddencheckStatus()

// 1. 파일 재로드

// 01-grade-phase1-tests.js 복사 → 붙여넣기```



// 2. 로드 확인// 3. 예상 출력

typeof gradePhase1  // → "object"

```**원인:**// 로그인됨: true



### ❌ 문제 4: 브라우저 새로고침 후 모듈 사라짐학생 계정으로 로그인// 사용자: 홍길동



**증상:**// 유형: 교수

```javascript

window.gradePhase1**해결:**```

// → undefined

``````javascript



**원인:**await logout()---

- 브라우저 새로고침 시 메모리 초기화

- 모듈 재로드 필요await login()  // 교수 계정 입력



**해결:**```### ❌ 문제 2: "교수 권한이 필요합니다"

```javascript

// 파일 복사 → 붙여넣기 → 확인

typeof gradePhase1  // → "object"

```---**증상:**



**팁**: 브라우저 새로고침 하지 말고 유지!```



------### ❌ 토큰이 계속 만료됨❌ HTTP 403 - Forbidden



## 🌐 API 호출 오류메시지: 교수만 접근 가능합니다



### ❌ 문제 5: "404 Not Found"**해결:**```



**증상:**```javascript

```

❌ HTTP 404 - Not Found// 토큰 확인**원인:**

URL: https://bluecrab.chickenkiller.com/BlueCrab-1.0.0/api/enrollments/...

```localStorage.getItem('jwtToken')- 학생 계정으로 로그인



**원인:**- 권한 부족

- 서버 미실행

- 빌드 미완료// 토큰 삭제 후 재로그인

- API 엔드포인트 오류

localStorage.removeItem('jwtToken')**해결 방법:**

**해결:**

```bashawait login()

# 1. Eclipse에서 서버 실행 확인

# Servers 탭 → 실행 중 확인``````javascript



# 2. 서버 재시작// 1. 로그아웃

# 서버 우클릭 → Restart

---await logout()

# 3. Console 확인

# "Started BlueCrabApplication" 확인

```

## 🧩 스크립트 로드 문제// 2. 교수 계정으로 재로그인

### ❌ 문제 6: "500 Internal Server Error"

await login()

**증상:**

```### ❌ "Cannot read property of undefined"// → 교수 이메일/비밀번호 입력

❌ HTTP 500 - Internal Server Error

메시지: NullPointerException

```

**증상:**// 3. 권한 확인

**원인:**

- 서버 로직 오류```checkStatus()

- 데이터베이스 연결 오류

- 필수 파라미터 누락Uncaught TypeError: Cannot read property 'setLecture' of undefined// → 유형: 교수 확인



**해결:**``````

```javascript

// 1. Eclipse Console 확인

// 에러 스택 트레이스 확인

**원인:**---

// 2. 테스트 데이터 확인

gradePhase1.getConfig()스크립트 로드 안 됨

// → 필수 값 누락 여부 확인

## 🧩 모듈 로드 오류

// 3. 데이터 수정 후 재시도

gradePhase1.setLecture('ETH201', 6)**해결:**

await gradePhase1.config()

``````javascript### ❌ 문제 3: "Cannot read property of undefined"



### ❌ 문제 7: "CORS policy" 오류// 1. 스크립트 재로드



**증상:**// VSCode에서 01-grade-phase1-tests.js 복사**증상:**

```

Access to fetch at 'https://...' has been blocked by CORS policy// → 브라우저 콘솔 붙여넣기 + Enter```

```

Uncaught TypeError: Cannot read property 'checkAuth' of undefined

**원인:**

- CORS 설정 문제// 2. 로드 확인    at testGradeConfig (...)

- 도메인 불일치

typeof gradePhase1  // → "object" 나와야 함```

**해결:**

```javascript```

// 같은 도메인에서 테스트

// bluecrab.chickenkiller.com에서 테스트**원인:**

```

---- 모듈 로드 순서 오류

------

- 이전 모듈 로드 실패

## 🔔 Phase 3 관련

### ❌ "gradePhase1 is not defined"- 의존성 모듈 미로드

### ❌ 문제 8: enrollmentIdx 조회 실패



**증상:**

```**원인:****해결 방법:**

❌ enrollmentIdx 조회 실패

```스크립트 로드 실패



**원인:**```javascript

- lecSerial 오류

- studentIdx 오류**해결:**// 1. 순서대로 다시 로드

- 수강 데이터 없음

```javascript// ① Utils

**해결:**

```javascript// 스크립트 파일 경로 확인// 01-grade-test-utils.js 복사 → 붙여넣기

// 1. 수강생 목록 확인

gradePhase3.setLecture('ETH201')// docs/강의관련기능/브라우저콘솔테스트/04-grade/01-grade-phase1-tests.jswindow.gradeTestUtils  // 확인

await gradePhase3.listStudents()



// 2. 올바른 studentIdx 사용

gradePhase3.setLecture('ETH201', 6)// 전체 복사 → 콘솔 붙여넣기// ② Phase 1



// 3. 다시 테스트```// 02-grade-phase1-tests.js 복사 → 붙여넣기

await gradePhase3.attendance()

```window.gradePhase1Tests  // 확인



---------



## 📚 관련 문서// ③ Phase 3



- [빠른 시작](./05-QUICK-START.md) - 5분 퀵 스타트### ❌ 브라우저 새로고침 후 사라짐// 03-grade-phase3-tests.js 복사 → 붙여넣기

- [사용법 가이드](./06-USAGE-GUIDE.md) - 전체 기능 설명

- [테스트 가이드](./07-TESTING-GUIDE.md) - 브라우저 테스트window.gradePhase3Tests  // 확인


**원인:**

브라우저 새로고침 시 메모리 초기화// ④ Runner

// 04-grade-test-runner.js 복사 → 붙여넣기

**해결:**window.gradeTests  // 확인

```javascript```

// 로그인 + 스크립트 재로드

await login()---

// 01-grade-phase1-tests.js 다시 붙여넣기

```### ❌ 문제 4: "gradePhase1Tests is not defined"



---**증상:**

```

## 🌐 API 호출 오류Uncaught ReferenceError: gradePhase1Tests is not defined

    at 04-grade-test-runner.js:10

### ❌ "404 Not Found"```



**증상:****원인:**

```- Runner 로드 전 Phase 1/3 미로드

❌ HTTP 404 - Not Found

URL: .../api/enrollments/ETH201/6/grade**해결 방법:**

```

```javascript

**원인:**// 1. Phase 1 로드 확인

- 강의 코드 오류window.gradePhase1Tests

- 학생 IDX 오류// → undefined면 02번 파일 로드 필요

- 서버 미실행

// 2. Phase 3 로드 확인

**해결:**window.gradePhase3Tests

```javascript// → undefined면 03번 파일 로드 필요

// 1. 강의 코드 확인 (DB에 존재하는지)

gradePhase1.setLecture('ETH201')  // 정확한 코드 입력// 3. 둘 다 로드 후 Runner 로드

// 04-grade-test-runner.js 복사 → 붙여넣기

// 2. 학생 IDX 확인```

gradePhase1.setLecture('ETH201', 6)  // 정확한 IDX 입력

---

// 3. 서버 실행 확인 (Eclipse Console)

```### ❌ 문제 5: 브라우저 새로고침 후 모듈 사라짐



---**증상:**

```

### ❌ "500 Internal Server Error"window.gradeTests

// → undefined

**증상:**```

```

❌ HTTP 500 - Internal Server Error**원인:**

```- 브라우저 새로고침 시 메모리 초기화

- 모듈 재로드 필요

**원인:**

서버 내부 오류**해결 방법:**



**해결:**```javascript

1. **Eclipse Console 확인** → 에러 로그 분석// 순서대로 재로드 (4개 파일)

2. **서버 재시작** (Stop → Start)// 01 → 02 → 03 → 04

3. **DB 연결 확인**// 각 파일 복사 → 붙여넣기 → 확인

```

---

**팁:** 브라우저 새로고침 하지 말고 유지!

### ❌ "CORS policy" 오류

---

**증상:**

```## 🌐 API 호출 오류

Access to fetch at '...' has been blocked by CORS policy

```### ❌ 문제 6: "404 Not Found"



**원인:****증상:**

다른 도메인에서 접속```

❌ HTTP 404 - Not Found

**해결:**URL: https://bluecrab.chickenkiller.com/BlueCrab-1.0.0/api/enrollments/...

``````

https://bluecrab.chickenkiller.com 에서 테스트

```**원인:**

- 서버 미실행

---- 빌드 미완료

- API 엔드포인트 오류

### ❌ "Network request failed"- 포트 충돌



**원인:****해결 방법:**

- 네트워크 연결 끊김

- 서버 다운```bash

# 1. Eclipse에서 서버 실행 확인

**해결:**# Servers 탭 → 실행 중 확인

1. 인터넷 연결 확인

2. 서버 실행 상태 확인# 2. 서버 재시작

3. 방화벽 설정 확인# 서버 우클릭 → Restart



---# 3. 빌드 확인

# 프로젝트 우클릭 → Run As → Maven build

## 📊 데이터 문제# Goals: clean package



### ❌ "등급 분포 합계가 100%가 아닙니다"# 4. Console 확인

# "Started BlueCrabApplication" 확인

**증상:**```

```

⚠️ 등급 분포 합계가 100%가 아닙니다 (현재: 85%)---

```

### ❌ 문제 7: "500 Internal Server Error"

**원인:**

A + B + C + D ≠ 100**증상:**

```

**해결:**❌ HTTP 500 - Internal Server Error

```javascript메시지: NullPointerException

// 방법 1: promptConfig()로 재입력```

gradePhase1.promptConfig()

**원인:**

// 방법 2: updateConfig()로 수정- 서버 로직 오류

gradePhase1.updateConfig({- 데이터베이스 연결 오류

    gradeDistribution: { A: 30, B: 40, C: 20, D: 10 }  // 합계 100%- 필수 파라미터 누락

})

```**해결 방법:**



---```javascript

// 1. Eclipse Console 확인

### ❌ assignmentTotalMaxScore 설정 경고// 에러 스택 트레이스 확인



**증상:**// 2. 테스트 데이터 확인

```gradeTests.getData()

⚠️ 경고: assignmentTotalMaxScore는 과제 생성 시 자동 누적됩니다.// → 필수 값 누락 여부 확인

```

// 3. 데이터 수정 후 재시도

**원인:**gradeTests.setData({

자동 계산 항목을 수동 설정 시도    lecIdx: 1,      // 올바른 강의 ID

    studentIdx: 100 // 올바른 학생 ID

**해결:**})

```javascriptawait gradeTests.config()

// 이 항목은 설정하지 마세요 (서버 자동 계산)```

gradePhase1.updateConfig({

    attendanceMaxScore: 90,  // ✅ 가능---

    // assignmentTotalMaxScore: 100  // ❌ 불필요

})### ❌ 문제 8: "CORS policy" 오류

```

**증상:**

---```

Access to fetch at 'https://...' from origin 'https://...' 

### ❌ 학생 성적이 조회 안 됨has been blocked by CORS policy

```

**증상:**

```**원인:**

❌ HTTP 404 - Not Found- CORS 설정 문제

```- 도메인 불일치



**원인:****해결 방법:**

- 강의 코드 미설정

- 학생 IDX 미설정```javascript

- DB에 데이터 없음// 1. 같은 도메인에서 테스트

// bluecrab.chickenkiller.com에서 테스트

**해결:**

```javascript// 2. 서버 CORS 설정 확인

// 1. 강의 + 학생 설정 확인// CorsConfig.java 확인

gradePhase1.setLecture('ETH201', 6)

// 3. 로컬 테스트 시

// 2. DB 확인 (ENROLLMENT_TBL에 수강 데이터 존재)// localhost:8080에서 테스트

``````



------



### ❌ 성적 목록이 비어있음## 🔔 이벤트 시스템 오류



**증상:**### ❌ 문제 9: 이벤트 로그가 안 보임

```

📋 성적 목록 (총 0명)**증상:**

```Eclipse Console에서 `[GradeUpdateEventListener]` 로그 없음



**원인:****원인:**

- 강의에 수강생 없음- 빌드에 최신 코드 미반영

- 성적 데이터 미생성- 이벤트 리스너 미등록

- 비동기 처리 대기 시간 부족

**해결:**- 로그 레벨 설정 문제

1. DB에서 수강 데이터 확인 (ENROLLMENT_TBL)

2. 성적 구성 설정 먼저 실행: `await gradePhase1.config()`**해결 방법:**



---```bash

# 1. Eclipse에서 재빌드

## 🔍 디버깅 팁# 프로젝트 우클릭 → Run As → Maven build

# Goals: clean package

### 현재 설정 확인

# 2. 서버 완전 재시작

```javascript# 서버 Stop → Start

gradePhase1.getConfig()

// → config 객체 전체 출력# 3. Console 초기화

```# Eclipse Console 우클릭 → Clear



### 강의 설정 확인# 4. 테스트 재실행

```

```javascript

// 설정 없을 경우```javascript

gradePhase1.setLecture('ETH201')  // 강의 설정// 브라우저에서

await gradeTests.phase3()

// 학생도 필요한 경우

gradePhase1.setLecture('ETH201', 6)  // 강의 + 학생// 3초 대기 후 Eclipse Console 확인

``````



### 네트워크 요청 확인```bash

# 5. 로그 검색

```# Eclipse Console에서

F12 → Network 탭# Ctrl + F → "GradeUpdateEventListener"

→ API 요청 클릭# 총 2개 로그 확인

→ Headers/Response 확인```

```

---

### 콘솔 로그 확인

### ❌ 문제 10: 이벤트는 발행되지만 재계산 안 됨

```

F12 → Console 탭**증상:**

→ 에러 메시지 확인- 이벤트 로그 출력됨

→ 빨간색 에러 클릭 → 상세 정보- 성적 재계산 안 됨

```

**원인:**

---- 리스너 로직 오류

- DB 트랜잭션 롤백

## 🎯 자주 묻는 질문 (FAQ)- 비동기 처리 실패



### Q1. 과제 총점을 수동 설정할 수 없나요?**해결 방법:**



**A:** 불가능합니다. 과제는 생성 시 개별 만점이 자동 누적됩니다.```bash

# 1. Eclipse Console에서 에러 확인

```javascript# Exception 스택 트레이스 확인

// ❌ 불가능

gradePhase1.updateConfig({ assignmentTotalMaxScore: 100 })# 2. DB 연결 확인

# application.properties 확인

// ✅ 가능

// 과제 생성 시 maxScore 설정 → 서버 자동 합산# 3. 수동 성적 조회

``````



---```javascript

// 브라우저에서

### Q2. 테스트 데이터를 변경하려면?await gradeTests.studentInfo()

// → 성적 변경 여부 확인

**A:** 설정 함수를 사용하세요.```



```javascript---

// 강의 변경

gradePhase1.setLecture('CS101')## ⚙️ 기타 문제



// 출석 만점 변경### ❌ 문제 11: 테스트가 너무 느림

gradePhase1.quickAttendanceConfig(90, 1.0)

**증상:**

// 등급 분포 변경- `runAll()` 실행 시 30초 이상 소요

gradePhase1.updateConfig({

    gradeDistribution: { A: 25, B: 45, C: 20, D: 10 }**원인:**

})- 네트워크 지연

```- 서버 성능 문제

- 대기 시간 설정 문제

---

**해결 방법:**

### Q3. 브라우저를 닫으면 다시 로드해야 하나요?

```javascript

**A:** 네, 새로고침이나 종료 시 스크립트가 사라집니다.// 1. 개별 테스트로 분리

await gradeTests.phase1()  // Phase 1만

```javascript// ... 확인 후

// 재시작 시await gradeTests.phase3()  // Phase 3만

1. await login()

2. 01-grade-phase1-tests.js 붙여넣기// 2. 네트워크 확인

3. gradePhase1.setLecture('ETH201')// 개발자 도구 → Network 탭

4. await gradePhase1.runAll()// 응답 시간 확인

```

// 3. 서버 성능 확인

---// Eclipse Console에서 로그 시간 확인

```

### Q4. 등급 배정이 이상하게 나와요

---

**A:** 60% 기준 + 상대평가 로직입니다.

### ❌ 문제 12: 등급 배정 결과가 이상함

```

1단계: 60% 미만 → F등급 확정**증상:**

2단계: 60% 이상 → A/B/C/D 배정 (상위부터)- 모든 학생이 같은 등급

3단계: 하위 침범 방식 (남은 학생 → 다음 등급)- 등급 분포가 설정과 다름

```

**원인:**

**예시:**- 테스트 데이터 부족

```- 등급 분포 설정 오류

100명 중 75명 낙제 (60% 미만)- 동점자 많음

→ 합격 25명 전원 A등급

(A 30명, B 40명 배정 불가 → 모두 최상위)**해결 방법:**

```

```javascript

---// 1. 등급 분포 확인

gradeTests.getData()

### Q5. 테스트 실행 중 멈췄어요// → gradeDistribution 확인



**A:** 비동기 함수 `await` 확인하세요.// 2. 등급 분포 수정

gradeTests.setData({

```javascript    gradeDistribution: {

// ❌ 잘못됨 (await 없음)        "A+": 10,

gradePhase1.config()  // Promise 반환만 하고 대기 안 함        "A": 15,

        "B+": 15,

// ✅ 올바름        "B": 25,

await gradePhase1.config()  // 완료까지 대기        "C": 25,

```        "D": 10

    }

---})



## 📚 추가 도움말// 3. 재배정

await gradeTests.finalize()

- [빠른 시작](./05-QUICK-START.md)```

- [상세 사용법](./06-USAGE-GUIDE.md)

- [API 참조](./08-MODULE-REFERENCE.md)---


### ❌ 문제 13: 브라우저 콘솔 출력이 깨짐

**증상:**
```
������ ��� ������
```

**원인:**
- 인코딩 문제
- 브라우저 설정 문제

**해결 방법:**

```javascript
// 1. 브라우저 재시작

// 2. 다른 브라우저 시도
// Chrome → Edge 또는 Firefox

// 3. 콘솔 초기화
// Console 우클릭 → Clear console
```

---

## 🆘 긴급 해결 절차

### 모든 게 안 될 때

```javascript
// 1. 브라우저 새로고침 (F5)

// 2. 재로그인
await login()

// 3. 모듈 순서대로 재로드
// 01 → 02 → 03 → 04
// 각 파일 복사 → 붙여넣기 → window 확인

// 4. 서버 재시작
// Eclipse → 서버 Stop → Start

// 5. 전체 재빌드
// Maven clean package

// 6. 캐시 삭제
// Ctrl + Shift + Delete → 캐시 삭제

// 7. 브라우저 재시작
```

---

## 📊 디버깅 체크리스트

### 모듈 로드 확인

```javascript
// ① Utils
window.gradeTestUtils !== undefined  // ✅
window.gradeTestUtils.checkAuth      // ✅

// ② Phase 1
window.gradePhase1Tests !== undefined  // ✅
window.gradePhase1Tests.testGradeConfig  // ✅

// ③ Phase 3
window.gradePhase3Tests !== undefined  // ✅
window.gradePhase3Tests.testAttendanceUpdate  // ✅

// ④ Runner
window.gradeTests !== undefined  // ✅
window.gradeTests.runAll  // ✅
```

### 로그인 상태 확인

```javascript
// 로그인 확인
checkStatus()

// 예상 출력:
// 로그인됨: true
// 사용자: 홍길동
// 유형: 교수
```

### 서버 상태 확인

```bash
# Eclipse Console에서
# "Started BlueCrabApplication" 확인
# 포트: 8080 확인
```

### 테스트 데이터 확인

```javascript
// 데이터 조회
gradeTests.getData()

// 필수 값 확인
// lecIdx, studentIdx, professorIdx, enrollmentIdx
```

---

## 💡 자주하는 실수

### 1. 로그인 안 하고 테스트
```javascript
❌ await gradeTests.runAll()
   // → "로그인이 필요합니다"

✅ await login()
   await gradeTests.runAll()
```

### 2. 모듈 순서 안 지킴
```javascript
❌ 04-grade-test-runner.js 먼저 로드
   // → "gradePhase1Tests is not defined"

✅ 01 → 02 → 03 → 04 순서대로
```

### 3. 브라우저 새로고침 후 재로드 안 함
```javascript
❌ F5 → await gradeTests.runAll()
   // → "gradeTests is not defined"

✅ F5 → 모듈 4개 재로드 → await gradeTests.runAll()
```

### 4. 이벤트 로그 즉시 확인
```javascript
❌ await gradeTests.attendance()
   // Eclipse Console 즉시 확인
   // → 로그 없음 (비동기 처리 중)

✅ await gradeTests.attendance()
   // 2-3초 대기
   // Eclipse Console 확인
```

### 5. 서버 미실행
```javascript
❌ Eclipse 서버 Stop 상태
   await gradeTests.runAll()
   // → "404 Not Found"

✅ Eclipse 서버 Start
   await gradeTests.runAll()
```

---

## 📞 추가 도움이 필요하면

- [빠른 시작 가이드](./05-QUICK-START.md)
- [사용법 가이드](./06-USAGE-GUIDE.md)
- [모듈 참조](./08-MODULE-REFERENCE.md)
- [전체 테스트 가이드](./07-TESTING-GUIDE.md)
