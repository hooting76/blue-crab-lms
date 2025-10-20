# 🔧 문제 해결 가이드

성적 관리 시스템 테스트 중 발생할 수 있는 문제와 해결 방법입니다.

---

## 📖 목차

- [로그인 관련](#-로그인-관련)
- [모듈 로드 오류](#-모듈-로드-오류)
- [API 호출 오류](#-api-호출-오류)
- [이벤트 시스템 오류](#-이벤트-시스템-오류)
- [기타 문제](#-기타-문제)

---

## 🔐 로그인 관련

### ❌ 문제 1: "로그인이 필요합니다"

**증상:**
```
⚠️ 로그인이 필요합니다!
🔧 docs/일반유저 로그인+게시판/test-1-login.js 실행
   await login() (교수 계정 사용)
```

**원인:**
- 로그인 안 됨
- JWT 토큰 만료
- 세션 타임아웃

**해결 방법:**

```javascript
// 1. 교수 계정으로 로그인
await login()

// 2. 로그인 상태 확인
checkStatus()

// 3. 예상 출력
// 로그인됨: true
// 사용자: 홍길동
// 유형: 교수
```

---

### ❌ 문제 2: "교수 권한이 필요합니다"

**증상:**
```
❌ HTTP 403 - Forbidden
메시지: 교수만 접근 가능합니다
```

**원인:**
- 학생 계정으로 로그인
- 권한 부족

**해결 방법:**

```javascript
// 1. 로그아웃
await logout()

// 2. 교수 계정으로 재로그인
await login()
// → 교수 이메일/비밀번호 입력

// 3. 권한 확인
checkStatus()
// → 유형: 교수 확인
```

---

## 🧩 모듈 로드 오류

### ❌ 문제 3: "Cannot read property of undefined"

**증상:**
```
Uncaught TypeError: Cannot read property 'checkAuth' of undefined
    at testGradeConfig (...)
```

**원인:**
- 모듈 로드 순서 오류
- 이전 모듈 로드 실패
- 의존성 모듈 미로드

**해결 방법:**

```javascript
// 1. 순서대로 다시 로드
// ① Utils
// 01-grade-test-utils.js 복사 → 붙여넣기
window.gradeTestUtils  // 확인

// ② Phase 1
// 02-grade-phase1-tests.js 복사 → 붙여넣기
window.gradePhase1Tests  // 확인

// ③ Phase 3
// 03-grade-phase3-tests.js 복사 → 붙여넣기
window.gradePhase3Tests  // 확인

// ④ Runner
// 04-grade-test-runner.js 복사 → 붙여넣기
window.gradeTests  // 확인
```

---

### ❌ 문제 4: "gradePhase1Tests is not defined"

**증상:**
```
Uncaught ReferenceError: gradePhase1Tests is not defined
    at 04-grade-test-runner.js:10
```

**원인:**
- Runner 로드 전 Phase 1/3 미로드

**해결 방법:**

```javascript
// 1. Phase 1 로드 확인
window.gradePhase1Tests
// → undefined면 02번 파일 로드 필요

// 2. Phase 3 로드 확인
window.gradePhase3Tests
// → undefined면 03번 파일 로드 필요

// 3. 둘 다 로드 후 Runner 로드
// 04-grade-test-runner.js 복사 → 붙여넣기
```

---

### ❌ 문제 5: 브라우저 새로고침 후 모듈 사라짐

**증상:**
```
window.gradeTests
// → undefined
```

**원인:**
- 브라우저 새로고침 시 메모리 초기화
- 모듈 재로드 필요

**해결 방법:**

```javascript
// 순서대로 재로드 (4개 파일)
// 01 → 02 → 03 → 04
// 각 파일 복사 → 붙여넣기 → 확인
```

**팁:** 브라우저 새로고침 하지 말고 유지!

---

## 🌐 API 호출 오류

### ❌ 문제 6: "404 Not Found"

**증상:**
```
❌ HTTP 404 - Not Found
URL: https://bluecrab.chickenkiller.com/BlueCrab-1.0.0/api/enrollments/...
```

**원인:**
- 서버 미실행
- 빌드 미완료
- API 엔드포인트 오류
- 포트 충돌

**해결 방법:**

```bash
# 1. Eclipse에서 서버 실행 확인
# Servers 탭 → 실행 중 확인

# 2. 서버 재시작
# 서버 우클릭 → Restart

# 3. 빌드 확인
# 프로젝트 우클릭 → Run As → Maven build
# Goals: clean package

# 4. Console 확인
# "Started BlueCrabApplication" 확인
```

---

### ❌ 문제 7: "500 Internal Server Error"

**증상:**
```
❌ HTTP 500 - Internal Server Error
메시지: NullPointerException
```

**원인:**
- 서버 로직 오류
- 데이터베이스 연결 오류
- 필수 파라미터 누락

**해결 방법:**

```javascript
// 1. Eclipse Console 확인
// 에러 스택 트레이스 확인

// 2. 테스트 데이터 확인
gradeTests.getData()
// → 필수 값 누락 여부 확인

// 3. 데이터 수정 후 재시도
gradeTests.setData({
    lecIdx: 1,      // 올바른 강의 ID
    studentIdx: 100 // 올바른 학생 ID
})
await gradeTests.config()
```

---

### ❌ 문제 8: "CORS policy" 오류

**증상:**
```
Access to fetch at 'https://...' from origin 'https://...' 
has been blocked by CORS policy
```

**원인:**
- CORS 설정 문제
- 도메인 불일치

**해결 방법:**

```javascript
// 1. 같은 도메인에서 테스트
// bluecrab.chickenkiller.com에서 테스트

// 2. 서버 CORS 설정 확인
// CorsConfig.java 확인

// 3. 로컬 테스트 시
// localhost:8080에서 테스트
```

---

## 🔔 이벤트 시스템 오류

### ❌ 문제 9: 이벤트 로그가 안 보임

**증상:**
Eclipse Console에서 `[GradeUpdateEventListener]` 로그 없음

**원인:**
- 빌드에 최신 코드 미반영
- 이벤트 리스너 미등록
- 비동기 처리 대기 시간 부족
- 로그 레벨 설정 문제

**해결 방법:**

```bash
# 1. Eclipse에서 재빌드
# 프로젝트 우클릭 → Run As → Maven build
# Goals: clean package

# 2. 서버 완전 재시작
# 서버 Stop → Start

# 3. Console 초기화
# Eclipse Console 우클릭 → Clear

# 4. 테스트 재실행
```

```javascript
// 브라우저에서
await gradeTests.phase3()

// 3초 대기 후 Eclipse Console 확인
```

```bash
# 5. 로그 검색
# Eclipse Console에서
# Ctrl + F → "GradeUpdateEventListener"
# 총 2개 로그 확인
```

---

### ❌ 문제 10: 이벤트는 발행되지만 재계산 안 됨

**증상:**
- 이벤트 로그 출력됨
- 성적 재계산 안 됨

**원인:**
- 리스너 로직 오류
- DB 트랜잭션 롤백
- 비동기 처리 실패

**해결 방법:**

```bash
# 1. Eclipse Console에서 에러 확인
# Exception 스택 트레이스 확인

# 2. DB 연결 확인
# application.properties 확인

# 3. 수동 성적 조회
```

```javascript
// 브라우저에서
await gradeTests.studentInfo()
// → 성적 변경 여부 확인
```

---

## ⚙️ 기타 문제

### ❌ 문제 11: 테스트가 너무 느림

**증상:**
- `runAll()` 실행 시 30초 이상 소요

**원인:**
- 네트워크 지연
- 서버 성능 문제
- 대기 시간 설정 문제

**해결 방법:**

```javascript
// 1. 개별 테스트로 분리
await gradeTests.phase1()  // Phase 1만
// ... 확인 후
await gradeTests.phase3()  // Phase 3만

// 2. 네트워크 확인
// 개발자 도구 → Network 탭
// 응답 시간 확인

// 3. 서버 성능 확인
// Eclipse Console에서 로그 시간 확인
```

---

### ❌ 문제 12: 등급 배정 결과가 이상함

**증상:**
- 모든 학생이 같은 등급
- 등급 분포가 설정과 다름

**원인:**
- 테스트 데이터 부족
- 등급 분포 설정 오류
- 동점자 많음

**해결 방법:**

```javascript
// 1. 등급 분포 확인
gradeTests.getData()
// → gradeDistribution 확인

// 2. 등급 분포 수정
gradeTests.setData({
    gradeDistribution: {
        "A+": 10,
        "A": 15,
        "B+": 15,
        "B": 25,
        "C": 25,
        "D": 10
    }
})

// 3. 재배정
await gradeTests.finalize()
```

---

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
