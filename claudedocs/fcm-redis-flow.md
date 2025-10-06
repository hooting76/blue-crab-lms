# FCM Redis 임시 저장소 플로우

## 📋 핵심 개념

```
DB 토큰: 등록된 기기 (영구 저장)
Redis 토큰: 임시 기기 (로그인 중에만 유효)

알림 전송: DB 토큰 + Redis 토큰 모두에게 전송
로그아웃: Redis 토큰 무조건 삭제, DB 토큰은 keepSignedIn에 따라 결정
```

---

## 🎯 시나리오별 동작

### 시나리오 1: 집 PC만 사용 (일반적인 경우)

```
[집 PC]
1. 로그인 → keepSignedIn=true
2. DB 저장: FCM_TOKEN_WEB = "token_home"
3. Redis: (없음)
4. 알림 발송 → 집 PC만 받음 ✅
5. 로그아웃 → DB 토큰 유지 (keepSignedIn=true)
6. 로그아웃 후 알림 → 집 PC만 받음 ✅
```

---

### 시나리오 2: 집 PC + 학교 PC 동시 로그인

```
[집 PC - 등록된 기기]
1. 로그인 → keepSignedIn=true
2. DB: FCM_TOKEN_WEB = "token_home"

[학교 PC - 임시 기기]
3. 로그인 시도 → CONFLICT (토큰 다름)
4. 사용자 선택: "기기 변경하지 않고 임시로 로그인"
5. temporaryOnly=true로 재요청
6. Redis 저장: fcm:temp:123:WEB = "token_school"

[알림 발송]
7. DB 확인: "token_home" 있음 → 집 PC로 전송 ✅
8. Redis 확인: "token_school" 있음 → 학교 PC로 전송 ✅
9. 결과: 둘 다 알림 받음! 🎉

[학교 PC 로그아웃]
10. unregister() 호출
11. Redis에서 "token_school" 삭제
12. DB는 그대로 (집 PC 토큰 유지)

[로그아웃 후 알림]
13. DB 확인: "token_home" 있음 → 집 PC만 전송 ✅
14. Redis 확인: 없음 → 학교 PC 전송 안 함 🔒
```

---

### 시나리오 3: 기기 강제 변경

```
[집 PC]
1. DB: FCM_TOKEN_WEB = "token_home"

[학교 PC]
2. 로그인 → CONFLICT
3. 사용자: "기기 변경"
4. registerForce() 호출
5. DB: FCM_TOKEN_WEB = "token_school" (덮어씀)

결과:
- 집 PC: 알림 안 받음 (DB에서 제거됨)
- 학교 PC: 알림 받음 ✅
```

---

## 🔧 프론트엔드 구현

### 1. 로그인 플로우

```javascript
async function loginAndRegisterFcm(email, password) {
  // 로그인
  const authResponse = await api.post('/api/auth/login', { email, password });
  localStorage.setItem('accessToken', authResponse.data.accessToken);

  // FCM 권한 확인
  const permission = await Notification.requestPermission();
  if (permission !== 'granted') return;

  // FCM 토큰 발급
  const fcmToken = await getFcmToken();

  // 서버에 등록 시도
  const result = await registerFcmToken(fcmToken);

  if (result.status === 'conflict') {
    // 충돌 발생 - 사용자에게 선택권 제공
    await handleConflict(fcmToken, result);
  }
}

async function registerFcmToken(fcmToken, temporaryOnly = false, keepSignedIn = null) {
  const response = await api.post('/api/fcm/register', {
    fcmToken: fcmToken,
    platform: detectPlatform(),
    temporaryOnly: temporaryOnly,  // ← 임시 등록 여부
    keepSignedIn: keepSignedIn
  });

  return response.data;
}

async function handleConflict(fcmToken, conflictInfo) {
  const { platform, lastUsed } = conflictInfo;
  const lastUsedDate = new Date(lastUsed).toLocaleString('ko-KR');

  // 사용자에게 3가지 선택권 제공
  const choice = window.confirm(
    `이미 다른 기기에서 알림을 받고 있습니다.\n` +
    `플랫폼: ${platform}\n` +
    `마지막 사용: ${lastUsedDate}\n\n` +
    `[확인] 이 기기로 변경 (기존 기기는 알림 안 받음)\n` +
    `[취소] 둘 다 알림 받기 (로그인 중에만)`
  );

  if (choice) {
    // 기기 변경
    const keepSignedIn = window.confirm(
      `이 기기를 개인 기기로 등록하시겠습니까?\n\n` +
      `[확인] 로그아웃 후에도 알림 받기\n` +
      `[취소] 로그아웃 시 알림 차단`
    );

    await api.post('/api/fcm/register-force', {
      fcmToken: fcmToken,
      platform: detectPlatform(),
      keepSignedIn: keepSignedIn
    });

    console.log('기기 변경 완료');
  } else {
    // 임시 등록 (둘 다 알림 받기)
    await registerFcmToken(fcmToken, true); // temporaryOnly=true

    console.log('임시 기기로 등록 - 로그인 중에만 알림 받음');
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

### 2. 로그아웃 플로우

```javascript
async function logout() {
  const fcmToken = await getFcmToken();

  if (fcmToken) {
    // FCM 토큰 삭제 (Redis 임시 토큰 또는 DB 토큰)
    await api.post('/api/fcm/unregister', {
      fcmToken: fcmToken,
      platform: detectPlatform(),
      forceDelete: false // keepSignedIn에 따라 결정
    });
  }

  localStorage.removeItem('accessToken');
  window.location.href = '/login';
}
```

---

## 📊 Redis 키 구조

```
# 패턴
fcm:temp:{userIdx}:{platform}

# 예시
fcm:temp:123:ANDROID → "fcm_token_galaxy_s23"
fcm:temp:123:WEB → "fcm_token_chrome_school"
fcm:temp:456:IOS → "fcm_token_iphone_14"
```

---

## 🎯 응답 상태

### REGISTERED (최초 등록)
```json
{
  "status": "registered",
  "message": "알림이 활성화되었습니다",
  "keepSignedIn": true
}
```

### RENEWED (재로그인)
```json
{
  "status": "renewed",
  "message": "토큰이 갱신되었습니다",
  "keepSignedIn": true
}
```

### CONFLICT (충돌)
```json
{
  "status": "conflict",
  "message": "이미 다른 기기에서 알림을 받고 있습니다",
  "platform": "WEB",
  "lastUsed": "2025-10-04T14:30:00"
}
```

### TEMPORARY (임시 등록) ✨
```json
{
  "status": "temporary",
  "message": "임시 알림이 활성화되었습니다 (로그인 중에만 알림 받음)",
  "keepSignedIn": false
}
```

### REPLACED (기기 변경)
```json
{
  "status": "replaced",
  "message": "기기가 변경되었습니다",
  "keepSignedIn": false
}
```

---

## 🔍 알림 전송 로직

```java
// sendToPlatform() 메서드
1. DB 토큰 확인 → "token_home" 있음
   → 전송 ✅

2. Redis 임시 토큰 확인 → "token_school" 있음
   → DB 토큰과 다르면 전송 ✅

3. 결과: 2개 기기 모두 알림 받음!
```

---

## ✅ 장점

1. **동시 로그인 지원**: 여러 기기에서 동시 알림 가능
2. **로그아웃 후 구분**: DB 토큰은 keepSignedIn에 따라, Redis는 무조건 삭제
3. **1인 1플랫폼 원칙 유지**: DB는 여전히 1개만 저장
4. **유연한 선택**: 사용자가 "기기 변경" vs "둘 다 알림" 선택 가능

---

## 🚀 테스트 체크리스트

- [ ] 집 PC 로그인 → keepSignedIn=true → 로그아웃 후 알림 받음
- [ ] 학교 PC 로그인 (충돌) → "둘 다 알림" 선택 → 둘 다 알림 받음
- [ ] 학교 PC 로그아웃 → Redis 토큰 삭제 → 학교 PC만 알림 안 받음
- [ ] 집 PC는 계속 알림 받음
- [ ] 기기 변경 → 이전 기기 알림 안 받음, 새 기기만 받음
