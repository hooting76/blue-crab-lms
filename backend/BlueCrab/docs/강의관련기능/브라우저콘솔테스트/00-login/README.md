# 🔐 로그인 테스트 스크립트

브라우저 콘솔에서 로그인 테스트를 수행하는 스크립트 모음입니다.

---

## 📁 파일 구조

```
00-login/
├── README.md           # 이 파일
├── user-login.js       # 일반 유저 로그인 (교수/학생)
└── admin-login.js      # 관리자 로그인 (2단계 인증)
```

---

## 🎯 사용 방법

### 1. 일반 유저 로그인 (교수/학생)

**파일**: `user-login.js`

#### 단계별 로그인

```javascript
// 1. 브라우저 콘솔에서 user-login.js 파일 전체 복사-붙여넣기

// 2. 로그인
await login()
// → 이메일 입력 (예: professor@example.com)
// → 비밀번호 입력

// 3. 상태 확인
checkStatus()

// 4. 로그아웃
await logout()
```

#### 교수 vs 학생 계정

- **교수 계정**: `userStudent === 1` → 게시글 작성 가능
- **학생 계정**: `userStudent === 0` → 조회만 가능

---

### 2. 관리자 로그인 (2단계 인증)

**파일**: `admin-login.js`

#### 방법 A: 통합 로그인 (자동) ⭐

```javascript
// 1. 브라우저 콘솔에서 admin-login.js 파일 전체 복사-붙여넣기

// 2. 통합 로그인 (3단계 자동 진행)
await quickAdminLogin()
// → 관리자 ID 입력 (이메일)
// → 비밀번호 입력
// → 이메일로 인증 코드 수신
// → 3초 후 인증 코드 입력 요청
// → 6자리 인증 코드 입력

// 3. 완료!
```

#### 방법 B: 단계별 로그인 (수동)

```javascript
// 1. 브라우저 콘솔에서 admin-login.js 파일 전체 복사-붙여넣기

// 2. 1차 로그인 (ID/PW 검증)
await adminLogin()
// → 관리자 ID 입력
// → 비밀번호 입력

// 3. 인증 코드 발송
await sendAuthCode()
// → 이메일 확인

// 4. 인증 코드 검증 (JWT 토큰 발급)
await verifyAuthCode("123456")
// → 이메일에서 받은 6자리 코드 입력
// → 또는 함수 호출 시 자동 prompt

// 5. 완료!
```

#### 상태 확인 및 로그아웃

```javascript
// 로그인 상태 확인
checkLoginStatus()

// 로그아웃 (모든 인증 정보 초기화)
logout()
```

---

## 🔑 인증 토큰 관리

### 전역 변수

두 로그인 스크립트 모두 다음 전역 변수를 사용합니다:

```javascript
window.authToken       // JWT 액세스 토큰
window.currentUser     // 사용자 정보
window.refreshToken    // 리프레시 토큰 (일반 유저만)
window.adminSessionToken // 세션 토큰 (관리자만)
```

### 토큰 공유

로그인 후 전역 변수에 저장된 토큰은 **다른 테스트 스크립트에서 자동으로 사용**됩니다.

**예시**:
```javascript
// 1. 로그인
await login()

// 2. 다른 테스트 실행 (성적 관리 등)
await gradeTests.runAll()
// → window.authToken이 자동으로 사용됨
```

---

## 📊 로그인 결과 확인

### 일반 유저 로그인 성공 시

```
✅ 로그인 성공!
메시지: 로그인에 성공했습니다
사용자 정보: {
  userId: 100,
  userName: "홍길동",
  userEmail: "professor@example.com",
  userStudent: 1
}
토큰 타입: Bearer
만료 시간: 3600초
🔗 전역 변수에 토큰 저장됨
🎓 교수 계정으로 로그인되었습니다.
```

### 관리자 로그인 성공 시

```
✅ 인증 코드 검증 성공!
🔑 JWT 액세스 토큰 획득!
   - 길이: 256자
   - 앞부분: eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
👤 관리자 정보: {
  adminId: "admin@example.com",
  name: "관리자",
  role: "ADMIN"
}
💾 JWT 토큰 및 사용자 정보 저장 완료!
🎯 로그인 완료! 이제 API 테스트 가능
```

---

## ❌ 문제 해결

### 일반 유저 로그인 오류

#### 1. "로그인 실패"
```
❌ 로그인 실패
응답: { message: "아이디 또는 비밀번호가 틀렸습니다" }
```
**해결**: 이메일과 비밀번호 확인

#### 2. "Request failed: NetworkError"
```
💥 Request failed: TypeError: Failed to fetch
```
**해결**: 
- 인터넷 연결 확인
- 서버 실행 상태 확인
- CORS 설정 확인

---

### 관리자 로그인 오류

#### 1. "세션 토큰이 없습니다"
```
❌ 세션 토큰이 없습니다!
🔧 해결 방법:
   1. adminLogin()을 먼저 실행하세요
```
**해결**: `adminLogin()` 부터 다시 시작

#### 2. "인증 코드가 틀렸습니다"
```
❌ HTTP 401 - 인증 실패
🔍 원인: 인증 코드 불일치
   - 입력한 코드: ABC123
💡 해결: 이메일의 6자리 코드를 정확히 복사
```
**해결**: 이메일에서 6자리 코드 다시 확인

#### 3. "인증 코드 만료"
```
❌ HTTP 401 - 인증 실패
🔍 원인: 인증 코드 발송 후 5분 초과
💡 해결: sendAuthCode() 재실행 후 5분 내 검증
```
**해결**: 
1. `sendAuthCode()` 재실행
2. 이메일 확인
3. 5분 이내에 `verifyAuthCode()` 실행

#### 4. "세션 토큰 무효"
```
❌ HTTP 401 - 인증 실패
🔍 원인: 세션 토큰 무효
💡 해결: logout() 후 1단계부터 재시작
```
**해결**:
1. `logout()` 실행
2. `adminLogin()` 부터 재시작

---

## 🔄 로그아웃 및 초기화

### 일반 유저 로그아웃

```javascript
await logout()
```
**초기화 항목**:
- `window.authToken`
- `window.refreshToken`
- `window.currentUser`

### 관리자 로그아웃

```javascript
logout()
```
**초기화 항목**:
- `window.authToken`
- `window.currentUser`
- `window.adminSessionToken`
- `localStorage` 모든 인증 정보

---

## 💡 팁

### 1. 빠른 재로그인

전역 변수는 페이지를 새로고침하면 초기화됩니다.

**재로그인 없이 유지하려면**:
- 브라우저 탭을 닫지 말고 유지
- 또는 `localStorage`에서 토큰 복구

### 2. 다중 계정 테스트

여러 계정을 테스트할 때:

```javascript
// 계정 A 로그인
await login()  // professor@example.com

// 테스트...

// 로그아웃
await logout()

// 계정 B 로그인
await login()  // student@example.com

// 테스트...
```

### 3. 토큰 만료 처리

JWT 토큰은 시간이 지나면 만료됩니다.

**만료 시**:
```
❌ HTTP 401 - Unauthorized
```

**해결**:
```javascript
await logout()
await login()
```

---

## 🎯 다음 단계

로그인 완료 후:

1. **성적 관리 테스트** → `04-grade/` 폴더
2. **시설 예약 테스트** → 해당 폴더
3. **게시판 테스트** → 해당 폴더

---

## 📝 참고

- **일반 유저 로그인**: 단일 단계 (ID/PW)
- **관리자 로그인**: 2단계 인증 (ID/PW + 이메일 인증 코드)
- **토큰 유효기간**: 일반 유저 3600초(1시간), 관리자는 서버 설정 따름
- **인증 코드 유효기간**: 5분

---

**작성**: GitHub Copilot  
**날짜**: 2024  
**버전**: v1.0
