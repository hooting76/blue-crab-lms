# BlueCrab LMS API 테스트 도구 가이드

---

## 🌟 권장: 웹 기반 테스트 페이지 사용

**가장 쉽고 편리한 방법입니다!**

### 접속 방법
```
로컬 개발: http://localhost:8080/  (또는 /status)
운영 서버: https://bluecrab.chickenkiller.com/BlueCrab-1.0.0/
```

### 주요 기능
- ✅ **일반 사용자 로그인** (이메일/비밀번호)
- ✅ **관리자 2단계 인증** (임시토큰 → 이메일 코드 → JWT 발급)
- ✅ 토큰 자동 저장/로드 (localStorage)
- ✅ JWT 디코딩 및 만료 시간 표시
- ✅ 토큰 갱신 및 로그아웃
- ✅ 커스텀 API 호출 (GET/POST/PUT/DELETE)
- ✅ 멋진 UI와 실시간 응답 표시

### 사용 예시

#### 1. 일반 사용자 로그인
1. "로그인 & 인증" 섹션에서 이메일/비밀번호 입력
2. [로그인] 버튼 클릭
3. 토큰이 자동으로 저장되고 표시됨

#### 2. 관리자 인증 (2단계)
1. "관리자 인증" 섹션에서 임시 토큰 입력
2. [Step 1: 인증코드 요청] 버튼 클릭
3. 이메일로 받은 6자리 코드 입력
4. [Step 2: 인증코드 검증] 버튼 클릭
5. 관리자 JWT 토큰 발급 및 저장

#### 3. API 호출
1. "API 테스트" 섹션에서 엔드포인트 선택
2. HTTP 메소드 선택
3. [API 호출] 버튼 클릭
4. 응답 결과 확인

### 장점
- ✅ URL만 알면 바로 접속
- ✅ 복사-붙여넣기 불필요
- ✅ 팀원과 쉽게 공유
- ✅ 서버 재시작 시 자동 업데이트
- ✅ 버전 관리 자동 (Git)

---

## 📋 대안: 브라우저 콘솔 스크립트

웹 페이지가 더 편리하지만, 필요시 콘솔 스크립트도 사용 가능합니다.

## 🚀 콘솔 스크립트 시작하기

### 1. 스크립트 로드

1. `src/utils/consoleApiTester.js` 파일 열기
2. 전체 내용 복사 (Ctrl+A → Ctrl+C)
3. 브라우저 개발자 도구 열기 (F12)
4. Console 탭에서 붙여넣기 (Ctrl+V) 후 Enter

성공하면 다음 메시지가 표시됩니다:

```
╔════════════════════════════════════════════════════════════════╗
║     ✅ BlueCrab Console API Tester Loaded Successfully!       ║
╚════════════════════════════════════════════════════════════════╝
```

### 2. 도움말 확인

```javascript
testAPI.help()
```

## 📚 주요 기능

### 🔐 로그인

#### 일반 사용자 로그인

```javascript
await testAPI.login('student@example.com', 'password123')
```

**응답 예시:**
- ✅ 성공 시: 토큰이 자동으로 localStorage에 저장됨
- 사용자 정보, 액세스 토큰, 리프레시 토큰이 콘솔에 출력됨

#### 관리자 로그인 (2단계)

**Step 1: 인증코드 요청**
```javascript
await testAPI.adminRequestCode('임시토큰여기입력')
```

**Step 2: 인증코드 검증**
```javascript
await testAPI.adminVerify('임시토큰여기입력', 'ABC123')
```

- 이메일로 받은 6자리 인증코드 입력
- 성공 시 관리자 JWT 토큰 발급 및 자동 저장

### 🔑 토큰 관리

#### 저장된 토큰 조회

```javascript
testAPI.getTokens()
```

**출력 예시:**
```
Access Token: eyJhbGciOiJIUzI1NiIsInR5cC...
Refresh Token: eyJhbGciOiJIUzI1NiIsInR5c...
Has Valid Tokens: true
```

#### 현재 사용자 정보 조회

```javascript
testAPI.getCurrentUser()
```

#### 모든 토큰 삭제

```javascript
testAPI.clearTokens()
```

### 🌐 API 호출

#### 범용 API 호출

Authorization 헤더가 자동으로 추가됩니다.

```javascript
// GET 요청
await testAPI.call('/api/profile')

// POST 요청
await testAPI.call('/api/some-endpoint', {
  method: 'POST',
  body: { key: 'value' }
})

// PUT 요청
await testAPI.call('/api/update', {
  method: 'PUT',
  body: { data: 'updated' }
})
```

#### 특수 API 호출

```javascript
// 로그아웃
await testAPI.logout()

// 토큰 갱신
await testAPI.refresh()

// 토큰 검증
await testAPI.validate()
```

## 🎯 실전 시나리오

### 시나리오 1: 일반 사용자 로그인 → API 테스트

```javascript
// 1. 로그인
await testAPI.login('student@example.com', 'password123')

// 2. 토큰 확인
testAPI.getTokens()

// 3. 프로필 조회
await testAPI.call('/api/profile')

// 4. 독서실 예약 조회
await testAPI.call('/api/readingroom/reservations')

// 5. 로그아웃
await testAPI.logout()
```

### 시나리오 2: 관리자 로그인 → 시설 관리

```javascript
// 1. 인증코드 요청
await testAPI.adminRequestCode('temporary_token_here')

// 2. 이메일에서 코드 확인 후 검증
await testAPI.adminVerify('temporary_token_here', 'A1B2C3')

// 3. 시설 목록 조회
await testAPI.call('/api/facilities')

// 4. 예약 현황 확인
await testAPI.call('/api/admin/reservations')
```

### 시나리오 3: 토큰 만료 처리

```javascript
// 1. 로그인
await testAPI.login('user@example.com', 'password')

// 2. API 호출 (성공)
await testAPI.call('/api/profile')

// ... 시간 경과 후 토큰 만료 ...

// 3. API 호출 (401 에러 발생 가능)
await testAPI.call('/api/profile')

// 4. 토큰 갱신
await testAPI.refresh()

// 5. 다시 API 호출 (성공)
await testAPI.call('/api/profile')
```

## 🔧 고급 기능

### 커스텀 헤더 추가

```javascript
await testAPI.call('/api/endpoint', {
  method: 'POST',
  headers: {
    'X-Custom-Header': 'value'
  },
  body: { data: 'test' }
})
```

### Authorization 헤더 제외

```javascript
await testAPI.call('/api/public-endpoint', {
  skipAuth: true
})
```

### 외부 API 호출

```javascript
await testAPI.call('https://external-api.com/endpoint', {
  method: 'GET'
})
```

## 📊 로그 출력 설명

스크립트는 컬러풀한 로그를 제공합니다:

- 🔄 **파란색**: API 요청 정보
- ✅ **초록색**: 성공 응답
- ❌ **빨간색**: 에러 응답
- 🔐 **제목**: 현재 수행 중인 작업

## ⚙️ 설정

### API Base URL 확인

```javascript
testAPI.config.API_BASE
// 출력: "https://bluecrab.chickenkiller.com/BlueCrab-1.0.0/api"
```

### 저장소 키 확인

```javascript
testAPI.config.STORAGE_KEYS
// 출력: { ACCESS_TOKEN: 'accessToken', REFRESH_TOKEN: 'refreshToken', ... }
```

## 🐛 문제 해결

### 토큰이 저장되지 않음

```javascript
// 1. 로그인 응답 확인
const response = await testAPI.login('user@example.com', 'password')
console.log(response)

// 2. 수동으로 토큰 저장 (응급 처치)
localStorage.setItem('accessToken', 'your_token_here')
localStorage.setItem('refreshToken', 'your_refresh_token_here')
```

### API 호출 시 401 에러

```javascript
// 1. 토큰 유효성 확인
await testAPI.validate()

// 2. 토큰 갱신
await testAPI.refresh()

// 3. 안 되면 재로그인
await testAPI.login('user@example.com', 'password')
```

### CORS 에러

- 브라우저에서 직접 테스트하므로 CORS 정책의 영향을 받습니다
- 개발 중에는 백엔드에서 CORS를 허용해야 합니다

## 📝 참고사항

### 보안

- **프로덕션 환경**: 절대로 실제 비밀번호를 콘솔에 입력하지 마세요
- **테스트 계정**: 개발/테스트 전용 계정 사용 권장
- **토큰 노출**: 콘솔 로그는 스크린샷 찍을 때 주의

### 데이터 저장

- 토큰은 localStorage에 저장됩니다
- 브라우저를 닫아도 토큰이 유지됩니다
- 개발자 도구 → Application → Local Storage에서 수동 확인 가능

### 호환성

- 모던 브라우저 (Chrome, Firefox, Edge, Safari)
- ES6+ 문법 사용
- Fetch API 사용 (IE11 미지원)

## 🎓 학습 자료

### API 응답 구조

모든 API 응답은 다음 구조를 따릅니다:

```javascript
{
  success: true,           // 성공 여부
  message: "로그인 성공",  // 메시지
  data: { ... },           // 실제 데이터
  timestamp: "2025-10-10T..." // 타임스탬프
}
```

### 로그인 응답 예시

```javascript
{
  success: true,
  message: "로그인에 성공했습니다.",
  data: {
    accessToken: "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    refreshToken: "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    tokenType: "Bearer",
    expiresIn: 900,
    user: {
      id: 1,
      name: "홍길동",
      email: "user@example.com",
      userStudent: 1
    }
  },
  timestamp: "2025-10-10T12:00:00Z"
}
```

## 🔗 관련 문서

- [Facility Reservation API Documentation](./feature-docs/facility-reservation/API_DOCUMENTATION_COMPLETE.md)
- [Backend Guide](./backend-guide/README.md)

## 📞 지원

문제가 발생하면 개발팀에 문의하거나 GitHub Issues를 활용하세요.

---

**작성일**: 2025-10-10
**버전**: 1.0.0
**작성자**: Claude (AI Assistant)
