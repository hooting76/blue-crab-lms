# FCM 토큰 등록 플로우 - 프론트엔드 구현 가이드

## 📋 전체 플로우

```
1. 사용자 로그인 성공
2. 알림 권한 확인/요청
3. FCM 토큰 발급
4. 서버에 토큰 등록 요청
5. 서버 응답 처리
   - REGISTERED/RENEWED: 로그인 완료
   - CONFLICT: 사용자에게 기기 변경 여부 확인
6. 필요시 강제 등록 (registerForce)
```

---

## 🔧 구현 예시 (JavaScript/React)

### 1. FCM 초기화 및 토큰 발급

```javascript
// firebase-config.js
import { initializeApp } from 'firebase/app';
import { getMessaging, getToken, onMessage } from 'firebase/messaging';

const firebaseConfig = {
  apiKey: "YOUR_API_KEY",
  projectId: "YOUR_PROJECT_ID",
  messagingSenderId: "YOUR_SENDER_ID",
  appId: "YOUR_APP_ID"
};

const app = initializeApp(firebaseConfig);
const messaging = getMessaging(app);

export { messaging };

// FCM 토큰 발급
export async function getFcmToken() {
  try {
    const token = await getToken(messaging, {
      vapidKey: 'YOUR_VAPID_KEY'
    });

    if (token) {
      console.log('FCM 토큰 발급:', token);
      return token;
    } else {
      console.log('알림 권한이 거부되었습니다.');
      return null;
    }
  } catch (error) {
    console.error('FCM 토큰 발급 실패:', error);
    return null;
  }
}

// 포그라운드 메시지 수신
onMessage(messaging, (payload) => {
  console.log('메시지 수신:', payload);
  // 알림 표시 로직
});
```

---

### 2. 로그인 후 FCM 토큰 등록

```javascript
// auth.js
import { getFcmToken } from './firebase-config';
import api from './api';

/**
 * 로그인 후 FCM 토큰 등록
 */
export async function loginAndRegisterFcm(email, password) {
  try {
    // 1. 로그인
    const authResponse = await api.post('/api/auth/login', {
      email,
      password
    });

    const { accessToken, refreshToken } = authResponse.data;

    // 토큰 저장
    localStorage.setItem('accessToken', accessToken);
    localStorage.setItem('refreshToken', refreshToken);

    // 2. 알림 권한 확인
    const permission = await Notification.requestPermission();

    if (permission !== 'granted') {
      console.log('알림 권한이 거부되었습니다. FCM 등록 생략');
      return { success: true, fcmRegistered: false };
    }

    // 3. FCM 토큰 발급
    const fcmToken = await getFcmToken();

    if (!fcmToken) {
      console.log('FCM 토큰 발급 실패. FCM 등록 생략');
      return { success: true, fcmRegistered: false };
    }

    // 4. 서버에 FCM 토큰 등록
    const fcmResult = await registerFcmToken(fcmToken);

    return {
      success: true,
      fcmRegistered: true,
      fcmStatus: fcmResult.status
    };

  } catch (error) {
    console.error('로그인 실패:', error);
    throw error;
  }
}

/**
 * FCM 토큰 등록
 */
async function registerFcmToken(fcmToken, keepSignedIn = null) {
  try {
    const response = await api.post('/api/fcm/register', {
      fcmToken: fcmToken,
      platform: detectPlatform(),
      keepSignedIn: keepSignedIn // null이면 서버가 기존 설정 유지
    });

    const result = response.data;

    // 응답 상태별 처리
    switch (result.status) {
      case 'registered':
        console.log('FCM 토큰 최초 등록 완료');
        return result;

      case 'renewed':
        console.log('FCM 토큰 갱신 완료 (동일 기기)');
        console.log('keepSignedIn:', result.keepSignedIn);
        return result;

      case 'conflict':
        console.log('FCM 토큰 충돌 감지');
        // 사용자에게 확인 받기
        return await handleTokenConflict(fcmToken, result);

      default:
        console.warn('알 수 없는 응답:', result);
        return result;
    }

  } catch (error) {
    console.error('FCM 토큰 등록 실패:', error);
    throw error;
  }
}

/**
 * 토큰 충돌 처리
 */
async function handleTokenConflict(fcmToken, conflictInfo) {
  const { platform, lastUsed } = conflictInfo;

  const lastUsedDate = new Date(lastUsed).toLocaleString('ko-KR');

  // 사용자에게 확인
  const changeDevice = window.confirm(
    `이미 다른 기기에서 알림을 받고 있습니다.\n` +
    `플랫폼: ${platform}\n` +
    `마지막 사용: ${lastUsedDate}\n\n` +
    `이 기기로 알림을 받도록 변경하시겠습니까?`
  );

  if (!changeDevice) {
    console.log('사용자가 기기 변경을 취소했습니다.');
    return { status: 'cancelled' };
  }

  // 로그인 유지 여부 확인
  const keepSignedIn = window.confirm(
    `이 기기를 개인 기기로 등록하시겠습니까?\n\n` +
    `[예] 내 개인 기기입니다 (로그아웃 후에도 알림 받기)\n` +
    `[아니오] 공용 PC입니다 (로그아웃 시 알림 차단)`
  );

  // 강제 등록
  try {
    const response = await api.post('/api/fcm/register-force', {
      fcmToken: fcmToken,
      platform: detectPlatform(),
      keepSignedIn: keepSignedIn
    });

    console.log('기기 변경 완료:', response.data);
    return response.data;

  } catch (error) {
    console.error('기기 변경 실패:', error);
    throw error;
  }
}

/**
 * 플랫폼 감지
 */
function detectPlatform() {
  const userAgent = navigator.userAgent.toLowerCase();

  if (/android/.test(userAgent)) {
    return 'ANDROID';
  } else if (/iphone|ipad|ipod/.test(userAgent)) {
    return 'IOS';
  } else {
    return 'WEB';
  }
}
```

---

### 3. React 컴포넌트 예시

```jsx
// LoginPage.jsx
import React, { useState } from 'react';
import { loginAndRegisterFcm } from './auth';

function LoginPage() {
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [loading, setLoading] = useState(false);

  const handleLogin = async (e) => {
    e.preventDefault();
    setLoading(true);

    try {
      const result = await loginAndRegisterFcm(email, password);

      if (result.success) {
        alert('로그인 성공!');

        if (result.fcmRegistered) {
          console.log('FCM 등록 완료:', result.fcmStatus);
        } else {
          console.log('FCM 등록 생략됨');
        }

        // 메인 페이지로 이동
        window.location.href = '/dashboard';
      }

    } catch (error) {
      alert('로그인 실패: ' + error.message);
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="login-page">
      <h1>로그인</h1>
      <form onSubmit={handleLogin}>
        <input
          type="email"
          placeholder="이메일"
          value={email}
          onChange={(e) => setEmail(e.target.value)}
          required
        />
        <input
          type="password"
          placeholder="비밀번호"
          value={password}
          onChange={(e) => setPassword(e.target.value)}
          required
        />
        <button type="submit" disabled={loading}>
          {loading ? '로그인 중...' : '로그인'}
        </button>
      </form>
    </div>
  );
}

export default LoginPage;
```

---

### 4. 로그아웃 처리

```javascript
// logout.js
import api from './api';
import { getFcmToken } from './firebase-config';

/**
 * 로그아웃
 */
export async function logout(forceDelete = false) {
  try {
    const fcmToken = await getFcmToken();

    if (fcmToken) {
      // FCM 토큰 삭제 요청
      await api.post('/api/fcm/unregister', {
        fcmToken: fcmToken,
        platform: detectPlatform(),
        forceDelete: forceDelete // true면 keepSignedIn 무시하고 강제 삭제
      });

      console.log('FCM 토큰 삭제 요청 완료');
    }

    // 로컬 스토리지 정리
    localStorage.removeItem('accessToken');
    localStorage.removeItem('refreshToken');

    // 로그인 페이지로 이동
    window.location.href = '/login';

  } catch (error) {
    console.error('로그아웃 실패:', error);
    // 에러가 나도 로컬 토큰은 삭제
    localStorage.removeItem('accessToken');
    localStorage.removeItem('refreshToken');
    window.location.href = '/login';
  }
}

function detectPlatform() {
  const userAgent = navigator.userAgent.toLowerCase();
  if (/android/.test(userAgent)) return 'ANDROID';
  if (/iphone|ipad|ipod/.test(userAgent)) return 'IOS';
  return 'WEB';
}
```

---

## 📊 응답 상태별 처리

### REGISTERED (최초 등록)
```json
{
  "status": "registered",
  "message": "알림이 활성화되었습니다",
  "keepSignedIn": true
}
```
→ 그냥 로그인 완료

### RENEWED (같은 기기 재로그인)
```json
{
  "status": "renewed",
  "message": "토큰이 갱신되었습니다",
  "keepSignedIn": true
}
```
→ 그냥 로그인 완료
→ `keepSignedIn` 값으로 현재 설정 확인 가능

### CONFLICT (다른 기기 충돌)
```json
{
  "status": "conflict",
  "message": "이미 다른 기기에서 알림을 받고 있습니다",
  "platform": "WEB",
  "lastUsed": "2025-10-04T14:30:00"
}
```
→ 사용자에게 확인
→ 기기 변경 원하면 `/api/fcm/register-force` 호출

### REPLACED (강제 변경 완료)
```json
{
  "status": "replaced",
  "message": "기기가 변경되었습니다",
  "keepSignedIn": false
}
```
→ 기기 변경 완료

---

## 🎯 핵심 포인트

1. **최초 로그인**: 사용자에게 `keepSignedIn` 선택받기
2. **재로그인**: 서버에서 기존 설정 유지 (자동)
3. **충돌 발생**: 사용자에게 기기 변경 여부 확인
4. **강제 변경**: 사용자가 선택한 `keepSignedIn` 값으로 등록

---

## 🔒 보안 고려사항

- FCM 토큰은 민감 정보이므로 HTTPS 필수
- `accessToken` 만료 시 자동 갱신 로직 필요
- 공용 PC에서는 `keepSignedIn=false` 권장

---

## 🧪 테스트 시나리오

### 시나리오 1: 최초 로그인
1. 로그인 → 알림 권한 요청
2. FCM 토큰 발급
3. `keepSignedIn` 선택
4. 서버 응답: `REGISTERED`

### 시나리오 2: 같은 기기 재로그인
1. 로그인 → FCM 토큰 발급 (동일)
2. 서버 응답: `RENEWED` (keepSignedIn 포함)
3. 사용자에게 묻지 않음

### 시나리오 3: 다른 기기 로그인
1. 로그인 → FCM 토큰 발급 (다름)
2. 서버 응답: `CONFLICT`
3. 사용자 확인 → 기기 변경
4. `/api/fcm/register-force` 호출
5. 서버 응답: `REPLACED`

### 시나리오 4: 로그아웃
1. 로그아웃 버튼 클릭
2. `/api/fcm/unregister` 호출
3. `keepSignedIn=true`면 토큰 유지
4. `keepSignedIn=false`면 토큰 삭제
