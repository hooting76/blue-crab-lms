# 자동 토큰 리프레시 시스템 가이드

## 개요

BlueCrab LMS는 JWT 토큰 기반 인증을 사용하며, **자동 토큰 리프레시** 기능을 제공합니다.
액세스 토큰이 만료되면 사용자의 개입 없이 자동으로 새로운 토큰을 발급받아 원래 요청을 재시도합니다.

---

## 시스템 구성

### 백엔드 (Spring Boot)

#### 1. JWT 필터 (`JwtAuthenticationFilter.java`)

토큰 만료 감지 시 특별한 응답 헤더를 추가합니다:

```java
catch (ExpiredJwtException e) {
    logger.warn("JWT Token has expired: {}", e.getMessage());
    response.setHeader("X-Token-Expired", "true");  // ⭐ 클라이언트에게 만료 알림
    isTokenExpired = true;
}
```

**응답 헤더:**
- `X-Token-Expired: true` - 토큰이 만료되었음을 클라이언트에게 알림

#### 2. 토큰 리프레시 API

**엔드포인트:** `POST /api/auth/refresh`

**요청 바디:**
```json
{
  "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
}
```

**성공 응답 (200 OK):**
```json
{
  "success": true,
  "message": "토큰이 성공적으로 갱신되었습니다.",
  "data": {
    "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "tokenType": "Bearer",
    "expiresIn": 900,
    "user": {
      "userIdx": 1,
      "userEmail": "user@example.com",
      "userName": "홍길동"
    }
  },
  "timestamp": "2025-10-21T12:00:00Z"
}
```

**실패 응답 (401 Unauthorized):**
```json
{
  "success": false,
  "message": "리프레시 토큰이 유효하지 않습니다.",
  "timestamp": "2025-10-21T12:00:00Z"
}
```

---

### 프론트엔드 (JavaScript)

#### API 클라이언트 (`api-client.js`)

자동 토큰 리프레시 기능이 내장된 API 클라이언트입니다.

**주요 기능:**
1. ✅ 모든 요청에 자동으로 `Authorization` 헤더 추가
2. ✅ 401 에러 + `X-Token-Expired` 헤더 감지 시 자동 리프레시
3. ✅ 동시 다발적 요청 시 중복 리프레시 방지 (큐잉 시스템)
4. ✅ 리프레시 성공 후 실패했던 요청 자동 재시도
5. ✅ 리프레시 토큰도 만료된 경우 자동 로그아웃

---

## 사용 방법

### 1. HTML에 스크립트 포함

```html
<!-- 토큰 관리 모듈 (기존) -->
<script src="/js/token-manager.js"></script>

<!-- API 클라이언트 (신규) -->
<script src="/js/api-client.js"></script>
```

### 2. API 호출

#### 기본 GET 요청
```javascript
try {
    const courses = await apiClient.get('/api/courses');
    console.log('강의 목록:', courses.data);
} catch (error) {
    console.error('API 호출 실패:', error);
}
```

#### POST 요청
```javascript
const newCourse = await apiClient.post('/api/courses', {
    name: '새 강의',
    description: '강의 설명'
});
```

#### PUT 요청
```javascript
const updated = await apiClient.put('/api/courses/1', {
    name: '수정된 강의명'
});
```

#### DELETE 요청
```javascript
const result = await apiClient.delete('/api/courses/1');
```

#### 인증 없는 요청 (로그인 API 등)
```javascript
const loginResult = await apiClient.post('/api/auth/login',
    { username: 'user', password: 'pass' },
    { skipAuth: true }  // Authorization 헤더 제외
);
```

#### 커스텀 헤더 추가
```javascript
const data = await apiClient.get('/api/users', {
    headers: {
        'X-Custom-Header': 'value'
    }
});
```

---

## 동작 플로우

### 정상 요청 플로우

```
사용자 → API 요청
         ↓
    apiClient.get()
         ↓
    Authorization 헤더 자동 추가
         ↓
    서버 응답 (200 OK)
         ↓
    결과 반환
```

### 토큰 만료 시 자동 리프레시 플로우

```
사용자 → API 요청 (만료된 토큰)
         ↓
    apiClient.get()
         ↓
    서버 응답 (401 + X-Token-Expired: true)
         ↓
    자동 리프레시 감지
         ↓
    /api/auth/refresh 호출
         ↓
    새 토큰 받기 및 저장
         ↓
    원래 요청 재시도 (새 토큰으로)
         ↓
    서버 응답 (200 OK)
         ↓
    결과 반환 (사용자는 만료 사실을 모름)
```

### 동시 다발적 요청 시 중복 리프레시 방지

```
요청 1 (만료) → 401 감지 → 리프레시 시작
요청 2 (만료) → 401 감지 → 큐에 대기 (중복 리프레시 방지)
요청 3 (만료) → 401 감지 → 큐에 대기
         ↓
    리프레시 완료
         ↓
    큐의 모든 요청에 새 토큰 전달
         ↓
    모든 요청 재시도 (새 토큰으로)
```

### 리프레시 토큰도 만료된 경우

```
사용자 → API 요청 (만료된 토큰)
         ↓
    자동 리프레시 시도
         ↓
    /api/auth/refresh 실패 (401)
         ↓
    자동 로그아웃 처리
         ↓
    로컬스토리지 토큰 삭제
         ↓
    (선택적) 로그인 페이지 리다이렉트
```

---

## 토큰 저장소

### 로컬스토리지 키

| 키 | 설명 |
|---|---|
| `bluecrab_access_token` | 액세스 토큰 (짧은 유효기간) |
| `bluecrab_refresh_token` | 리프레시 토큰 (긴 유효기간) |

### 자동 동기화

API 클라이언트는 다음 전역 변수와 자동으로 동기화됩니다:
- `window.accessToken`
- `window.refreshToken`

---

## 에러 처리

### 1. 네트워크 오류
```javascript
try {
    const data = await apiClient.get('/api/courses');
} catch (error) {
    if (error.message.includes('Failed to fetch')) {
        alert('네트워크 연결을 확인해주세요.');
    }
}
```

### 2. 서버 오류 (500)
```javascript
const response = await apiClient.request('/api/courses', { method: 'GET' });
if (!response.ok) {
    const error = await response.json();
    console.error('서버 오류:', error.message);
}
```

### 3. 권한 오류 (403)
```javascript
const result = await apiClient.delete('/api/admin/users/1');
if (result.success === false) {
    alert('권한이 없습니다.');
}
```

---

## 보안 고려사항

### 1. 토큰 저장 위치
- **현재:** 로컬스토리지 (XSS 공격에 취약)
- **권장 (향후):** HttpOnly 쿠키 (XSS 방어)

### 2. HTTPS 사용 필수
- 프로덕션 환경에서는 반드시 HTTPS를 사용하세요.
- HTTP에서는 토큰이 평문으로 전송되어 중간자 공격(MITM)에 노출됩니다.

### 3. 토큰 만료 시간
- **액세스 토큰:** 15분 (짧게 유지)
- **리프레시 토큰:** 24시간

### 4. 리프레시 토큰 로테이션
- 현재 시스템은 리프레시 시 새 리프레시 토큰도 발급합니다.
- 오래된 리프레시 토큰은 자동으로 무효화됩니다.

---

## 마이그레이션 가이드

### 기존 fetch 코드를 apiClient로 변경

**변경 전:**
```javascript
const response = await fetch('/api/courses', {
    method: 'GET',
    headers: {
        'Authorization': `Bearer ${accessToken}`
    }
});
const data = await response.json();
```

**변경 후:**
```javascript
const data = await apiClient.get('/api/courses');
```

**장점:**
- ✅ Authorization 헤더 자동 추가
- ✅ 토큰 만료 시 자동 리프레시
- ✅ 코드 간소화

---

## 테스트 시나리오

### 1. 정상 요청
```javascript
// 유효한 토큰으로 API 호출
const courses = await apiClient.get('/api/courses');
console.log('강의 목록:', courses);
// ✅ 정상 응답
```

### 2. 토큰 만료 후 자동 리프레시
```javascript
// 만료된 토큰으로 API 호출
const courses = await apiClient.get('/api/courses');
// ⚠️ 401 + X-Token-Expired 감지
// 🔄 자동 리프레시
// ✅ 원래 요청 재시도
// ✅ 정상 응답 (사용자는 만료 사실을 모름)
```

### 3. 리프레시 토큰도 만료
```javascript
// 만료된 토큰으로 API 호출
try {
    const courses = await apiClient.get('/api/courses');
} catch (error) {
    // ❌ 리프레시 실패
    // 🚪 자동 로그아웃
    console.error('다시 로그인해주세요.');
}
```

### 4. 동시 다발적 요청
```javascript
// 3개의 요청을 동시에 보냄 (모두 만료된 토큰)
const [courses, users, assignments] = await Promise.all([
    apiClient.get('/api/courses'),
    apiClient.get('/api/users'),
    apiClient.get('/api/assignments')
]);
// 🔄 리프레시는 한 번만 실행 (중복 방지)
// ✅ 모든 요청 성공
```

---

## 로깅

API 클라이언트는 다음 상황에서 콘솔 로그를 출력합니다:

- `⚠️ 토큰이 만료되었습니다. 자동 갱신을 시도합니다...`
- `✅ 토큰이 자동으로 갱신되었습니다.`
- `❌ 토큰 갱신 실패. 다시 로그인해주세요.`
- `로그아웃되었습니다. 다시 로그인해주세요.`

---

## FAQ

### Q: 기존 코드를 모두 수정해야 하나요?
**A:** 아니요. 기존 `fetch` 코드는 그대로 사용할 수 있습니다. 하지만 새로운 코드에서는 `apiClient`를 사용하는 것을 권장합니다.

### Q: 리프레시 토큰도 만료되면 어떻게 되나요?
**A:** 자동으로 로그아웃 처리되며, 사용자에게 다시 로그인하라는 메시지가 표시됩니다.

### Q: 여러 요청을 동시에 보낼 때 리프레시가 여러 번 실행되나요?
**A:** 아니요. 첫 번째 요청만 리프레시를 실행하고, 나머지 요청은 큐에서 대기했다가 새 토큰을 받아 재시도합니다.

### Q: 로그인 API에도 apiClient를 사용할 수 있나요?
**A:** 네. `skipAuth: true` 옵션을 사용하면 Authorization 헤더 없이 요청할 수 있습니다.

```javascript
const result = await apiClient.post('/api/auth/login',
    { username: 'user', password: 'pass' },
    { skipAuth: true }
);
```

### Q: 토큰이 만료되기 전에 미리 갱신할 수 있나요?
**A:** 현재는 만료 후 갱신 방식입니다. 사전 갱신이 필요하면 다음과 같이 구현할 수 있습니다:

```javascript
// token-manager.js에 추가
function isTokenExpiringSoon(token, minutesThreshold = 5) {
    try {
        const decoded = decodeJWT(token);
        const expiryTime = decoded.exp * 1000;
        const currentTime = Date.now();
        const timeLeft = expiryTime - currentTime;
        const minutesLeft = timeLeft / 1000 / 60;
        return minutesLeft < minutesThreshold;
    } catch (error) {
        return false;
    }
}

// 5분 전에 미리 갱신
if (isTokenExpiringSoon(accessToken, 5)) {
    await apiClient.refreshAccessToken();
}
```

---

## 관련 파일

- **백엔드:**
  - `JwtAuthenticationFilter.java` - JWT 필터 (만료 감지)
  - `AuthController.java` - 인증 API
  - `JwtUtil.java` - JWT 유틸리티

- **프론트엔드:**
  - `api-client.js` - API 클라이언트 (자동 리프레시)
  - `token-manager.js` - 토큰 관리 모듈

---

## 문의

문제가 발생하거나 기능 개선이 필요한 경우 개발팀에 문의해주세요.
