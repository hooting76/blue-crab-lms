# 프론트엔드 토큰 만료 처리 가이드

## 개요

BlueCrab LMS API 호출 시 토큰이 만료되면 다음과 같은 응답을 받게 됩니다:

**HTTP Status:** `401 Unauthorized`
**응답 헤더:** `X-Token-Expired: true`
**응답 본문:**
```json
{
  "success": false,
  "message": "토큰이 만료되었습니다. 리프레시 토큰을 사용하여 새 토큰을 발급받아주세요.",
  "data": null,
  "timestamp": "2025-10-22T01:04:16.8638770972",
  "errorCode": null
}
```

이 문서는 프론트엔드에서 이러한 토큰 만료 상황을 **어떻게 처리해야 하는지** 설명합니다.

---

## 처리 방법 (3가지 옵션)

### 옵션 1: 자동 토큰 갱신 (권장)

이미 구현된 `api-client.js`를 사용하면 **사용자 개입 없이 자동으로** 토큰을 갱신하고 요청을 재시도합니다.

#### 사용법

```javascript
// 1. HTML에 스크립트 포함
<script src="/js/api-client.js"></script>

// 2. apiClient 사용
const courses = await apiClient.get('/api/courses');
// 토큰이 만료되어도 자동으로 갱신 후 재시도 → 사용자는 모름
```

#### 장점
- ✅ **사용자 경험 최고**: 토큰 만료를 인지하지 못함
- ✅ **코드 간결**: Authorization 헤더, 토큰 갱신 모두 자동 처리
- ✅ **동시 요청 최적화**: 여러 요청이 동시에 만료되어도 갱신은 1번만 실행
- ✅ **이미 구현됨**: 추가 개발 불필요

#### 단점
- ⚠️ 기존 `fetch` 코드를 `apiClient`로 마이그레이션 필요

#### 자세한 설명
→ [AUTO_TOKEN_REFRESH_GUIDE.md](../backend/BlueCrab/docs/api/AUTO_TOKEN_REFRESH_GUIDE.md)

---

### 옵션 2: fetch 인터셉터 구현 (커스텀)

`fetch`를 래핑하여 토큰 만료를 감지하고 자동 갱신하는 방식입니다.

#### 구현 예시

```javascript
// fetchWithAuth.js
let isRefreshing = false;
let failedQueue = [];

const processQueue = (error, token = null) => {
    failedQueue.forEach(prom => {
        if (error) {
            prom.reject(error);
        } else {
            prom.resolve(token);
        }
    });
    failedQueue = [];
};

async function refreshToken() {
    const refreshToken = localStorage.getItem('bluecrab_refresh_token');

    const response = await fetch('/api/auth/refresh', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ refreshToken })
    });

    if (!response.ok) {
        throw new Error('Refresh failed');
    }

    const data = await response.json();
    const newAccessToken = data.data.accessToken;
    const newRefreshToken = data.data.refreshToken;

    // 새 토큰 저장
    localStorage.setItem('bluecrab_access_token', newAccessToken);
    localStorage.setItem('bluecrab_refresh_token', newRefreshToken);

    return newAccessToken;
}

async function fetchWithAuth(url, options = {}) {
    const accessToken = localStorage.getItem('bluecrab_access_token');

    // Authorization 헤더 자동 추가
    if (!options.skipAuth) {
        options.headers = options.headers || {};
        options.headers['Authorization'] = `Bearer ${accessToken}`;
    }

    let response = await fetch(url, options);

    // 토큰 만료 감지
    if (response.status === 401 && response.headers.get('X-Token-Expired') === 'true') {

        // 이미 갱신 중이면 대기
        if (isRefreshing) {
            return new Promise((resolve, reject) => {
                failedQueue.push({ resolve, reject });
            }).then(token => {
                options.headers['Authorization'] = `Bearer ${token}`;
                return fetch(url, options);
            });
        }

        isRefreshing = true;

        try {
            const newToken = await refreshToken();
            processQueue(null, newToken);

            // 원래 요청 재시도
            options.headers['Authorization'] = `Bearer ${newToken}`;
            response = await fetch(url, options);

        } catch (error) {
            processQueue(error, null);

            // 리프레시 실패 → 로그아웃
            localStorage.removeItem('bluecrab_access_token');
            localStorage.removeItem('bluecrab_refresh_token');
            window.location.href = '/login';

            throw error;
        } finally {
            isRefreshing = false;
        }
    }

    return response;
}

// 사용
const response = await fetchWithAuth('/api/courses');
const data = await response.json();
```

#### 장점
- ✅ 기존 `fetch` 사용 패턴과 유사
- ✅ 자동 갱신 + 재시도

#### 단점
- ⚠️ 직접 구현 필요
- ⚠️ `apiClient`와 기능 중복

---

### 옵션 3: 수동 처리 (기본 방식)

토큰 만료 시 사용자에게 알리고 재로그인을 유도합니다.

#### 구현 예시

```javascript
async function callApi(url) {
    const accessToken = localStorage.getItem('bluecrab_access_token');

    const response = await fetch(url, {
        headers: {
            'Authorization': `Bearer ${accessToken}`
        }
    });

    // 토큰 만료 감지
    if (response.status === 401) {
        const isExpired = response.headers.get('X-Token-Expired') === 'true';

        if (isExpired) {
            // 방법 A: 알림 후 로그인 페이지로 이동
            alert('세션이 만료되었습니다. 다시 로그인해주세요.');
            localStorage.removeItem('bluecrab_access_token');
            localStorage.removeItem('bluecrab_refresh_token');
            window.location.href = '/login';
            return;
        }
    }

    return await response.json();
}
```

#### 장점
- ✅ 구현 간단
- ✅ 명확한 사용자 피드백

#### 단점
- ❌ 사용자 경험 저하 (작업 중 중단)
- ❌ 재로그인 필요

---

## 토큰 갱신 API 명세

### 엔드포인트
```
POST /api/auth/refresh
```

### 요청 본문
```json
{
  "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
}
```

### 성공 응답 (200 OK)
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
  "timestamp": "2025-10-22T12:00:00Z"
}
```

### 실패 응답 (401 Unauthorized)
```json
{
  "success": false,
  "message": "리프레시 토큰이 유효하지 않습니다.",
  "data": null,
  "timestamp": "2025-10-22T12:00:00Z"
}
```

**리프레시 토큰도 만료된 경우:**
- 사용자 세션 종료
- localStorage 토큰 삭제
- 로그인 페이지로 리다이렉트

---

## 권장 처리 흐름도

```
┌─────────────────────────────────────┐
│  사용자 → API 요청                    │
│  (예: GET /api/courses)              │
└─────────────┬───────────────────────┘
              ▼
┌─────────────────────────────────────┐
│  서버 응답 확인                        │
│  - 200 OK? → 정상 처리               │
│  - 401 + X-Token-Expired?            │
└─────────────┬───────────────────────┘
              ▼
┌─────────────────────────────────────┐
│  토큰 만료 감지                        │
│  console.warn('토큰 만료됨')          │
└─────────────┬───────────────────────┘
              ▼
┌─────────────────────────────────────┐
│  리프레시 토큰 확인                     │
│  - localStorage에서 가져오기          │
└─────────────┬───────────────────────┘
              ▼
        ┌─────┴─────┐
        │           │
    있음 │           │ 없음
        ▼           ▼
┌──────────┐   ┌────────────┐
│ 갱신 시도 │   │  로그아웃   │
│ POST     │   │  처리      │
│ /auth/   │   │           │
│ refresh  │   └────────────┘
└─────┬────┘
      │
      ▼
 ┌────┴────┐
 │         │
성공│       │실패
 ▼         ▼
┌────┐  ┌─────────┐
│새   │  │로그아웃  │
│토큰 │  │처리     │
│저장 │  └─────────┘
└─┬──┘
  ▼
┌──────────────┐
│원래 요청 재시도│
│(새 토큰 사용) │
└──────┬───────┘
       ▼
┌──────────────┐
│  정상 응답    │
│  반환        │
└──────────────┘
```

---

## 체크리스트

프론트엔드 구현 시 다음 사항을 확인하세요:

### 기본 사항
- [ ] 401 응답 + `X-Token-Expired: true` 헤더 감지 로직 구현
- [ ] 토큰 만료 시 리프레시 토큰으로 갱신 시도
- [ ] 갱신 성공 시 새 토큰을 localStorage에 저장
- [ ] 갱신 성공 시 원래 요청을 새 토큰으로 재시도

### 예외 처리
- [ ] 리프레시 토큰도 만료된 경우 로그아웃 처리
- [ ] 로그아웃 시 localStorage 토큰 삭제
- [ ] 로그아웃 시 로그인 페이지로 리다이렉트
- [ ] 사용자에게 적절한 안내 메시지 표시

### 동시성 제어 (권장)
- [ ] 여러 요청이 동시에 만료된 경우 갱신은 1번만 실행
- [ ] 갱신 중인 요청들은 큐에서 대기
- [ ] 갱신 완료 후 모든 대기 요청에 새 토큰 전달

### 사용자 경험
- [ ] 토큰 갱신 중 로딩 상태 표시 (선택)
- [ ] 갱신 실패 시 명확한 안내 메시지
- [ ] 재로그인 후 원래 작업으로 복귀 가능 (선택)

---

## 자주 묻는 질문 (FAQ)

### Q1: `errorCode`가 `null`인 이유는?
**A:** 현재 백엔드에서 토큰 만료 시 `errorCode`를 설정하지 않습니다. 대신 `X-Token-Expired` 헤더와 `message` 필드로 판단하세요.

만약 `errorCode`가 필요하다면 백엔드에 다음과 같이 수정 요청할 수 있습니다:
```json
{
  "success": false,
  "message": "토큰이 만료되었습니다...",
  "errorCode": "TOKEN_EXPIRED"  // 추가
}
```

### Q2: 토큰 만료를 어떻게 감지하나요?
**A:** 두 가지 방법으로 감지합니다:
1. **HTTP 상태 코드**: `response.status === 401`
2. **응답 헤더**: `response.headers.get('X-Token-Expired') === 'true'`

둘 다 확인하는 것이 가장 확실합니다.

### Q3: 왜 `api-client.js`를 사용해야 하나요?
**A:**
- 이미 구현되어 있어 개발 시간 절약
- 동시 요청 최적화 (큐잉 시스템)
- 테스트 완료된 안정적인 코드
- 코드 중복 방지

### Q4: 리프레시 토큰도 만료되면 어떻게 되나요?
**A:**
1. `/api/auth/refresh` 호출 실패 (401)
2. 자동 로그아웃 처리
3. localStorage 토큰 삭제
4. 로그인 페이지로 리다이렉트

### Q5: 토큰 만료 시간은 얼마인가요?
**A:**
- **액세스 토큰**: 15분
- **리프레시 토큰**: 24시간

(서버 설정에 따라 다를 수 있음)

### Q6: React, Vue 등의 프레임워크에서는?
**A:**

#### React 예시 (Axios 인터셉터)
```javascript
import axios from 'axios';

axios.interceptors.response.use(
  response => response,
  async error => {
    const originalRequest = error.config;

    if (error.response?.status === 401 &&
        error.response?.headers['x-token-expired'] === 'true' &&
        !originalRequest._retry) {

      originalRequest._retry = true;

      try {
        const refreshToken = localStorage.getItem('bluecrab_refresh_token');
        const { data } = await axios.post('/api/auth/refresh', { refreshToken });

        localStorage.setItem('bluecrab_access_token', data.data.accessToken);

        originalRequest.headers['Authorization'] = `Bearer ${data.data.accessToken}`;
        return axios(originalRequest);
      } catch (refreshError) {
        localStorage.clear();
        window.location.href = '/login';
        return Promise.reject(refreshError);
      }
    }

    return Promise.reject(error);
  }
);
```

#### Vue 3 (Composable)
```javascript
// useApi.js
import { ref } from 'vue';
import { useRouter } from 'vue-router';

export function useApi() {
  const router = useRouter();
  const isRefreshing = ref(false);

  async function request(url, options = {}) {
    const token = localStorage.getItem('bluecrab_access_token');

    const response = await fetch(url, {
      ...options,
      headers: {
        'Authorization': `Bearer ${token}`,
        ...options.headers
      }
    });

    if (response.status === 401 &&
        response.headers.get('X-Token-Expired') === 'true') {

      if (!isRefreshing.value) {
        isRefreshing.value = true;

        try {
          const refreshToken = localStorage.getItem('bluecrab_refresh_token');
          const refreshRes = await fetch('/api/auth/refresh', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ refreshToken })
          });

          const data = await refreshRes.json();
          localStorage.setItem('bluecrab_access_token', data.data.accessToken);

          // 재시도
          return await request(url, options);
        } catch (error) {
          localStorage.clear();
          router.push('/login');
          throw error;
        } finally {
          isRefreshing.value = false;
        }
      }
    }

    return response;
  }

  return { request };
}
```

---

## 참고 문서

- [AUTO_TOKEN_REFRESH_GUIDE.md](../backend/BlueCrab/docs/api/AUTO_TOKEN_REFRESH_GUIDE.md) - 자동 토큰 갱신 시스템 상세 가이드
- [token-expiry-handling.md](../docs/token-expiry-handling.md) - 백엔드 토큰 만료 규약
- [api-client.js](../backend/BlueCrab/src/main/resources/static/js/api-client.js) - 구현 코드

---

## 요약

| 방법 | 사용자 경험 | 구현 난이도 | 권장도 |
|-----|-----------|-----------|-------|
| **옵션 1: api-client.js** | ⭐⭐⭐⭐⭐ | ⭐ (이미 구현됨) | ✅ **강력 권장** |
| **옵션 2: 커스텀 인터셉터** | ⭐⭐⭐⭐ | ⭐⭐⭐ | 프레임워크 필요 시 |
| **옵션 3: 수동 처리** | ⭐⭐ | ⭐⭐ | 간단한 프로젝트만 |

**결론:** 대부분의 경우 `apiClient` 사용을 권장합니다.
