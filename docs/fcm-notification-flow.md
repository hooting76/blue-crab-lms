# FCM 통합 시나리오 흐름

이 문서는 BlueCrab LMS에서 Firebase Cloud Messaging(FCM) 관련 클라이언트 ↔ 백엔드 상호작용을 단계별로 정리합니다. 로그인 직후 토큰 등록부터 알림 전송, 로그아웃 시 정리까지의 전체 흐름을 담았습니다.

---

## 1. 공통 준비

- **프런트 진입**: `firebase/PushNotification.jsx`의 `PushNotificationService.initialize()` 호출로 Service Worker 등록, 알림 권한 요청, FCM 토큰 발급, `/api/fcm/register` 요청이 순차 실행됩니다.
- **사용자 인증**: `/api/fcm/**` 엔드포인트는 JWT 인증이 필요하므로, `Authorization: Bearer {accessToken}` 헤더를 항상 포함합니다.
- **플랫폼 구분**: 현재 구현은 `navigator.userAgent` 를 기반으로 `ANDROID` / `IOS` / `WEB` 값을 추정합니다. 필요 시 UI 또는 UA Detection을 보완해 전달합니다.

---

## 2. 토큰 등록 시나리오

### 2.1 최초 등록 (`POST /api/fcm/register`)

1. **요청**  
   ```json
   {
     "fcmToken": "<FCM_DEVICE_TOKEN>",
     "platform": "WEB",
     "keepSignedIn": true
   }
   ```
2. **로직 (`FcmTokenService.register`)**  
   - 토큰이 이미 다른 사용자에게 등록되어 있는지 검사.  
   - 현재 사용자에 대해 플랫폼별 슬롯이 비어 있으면 새 토큰을 저장.  
   - `keepSignedIn` 값이 null이면 false로 간주.
3. **응답 (`FcmRegisterResponse`)**
   - `status=registered`
   - `keepSignedIn` 현재 설정값 포함.

### 2.2 동일 기기 재등록

- 같은 토큰이 다시 등록되면 마지막 사용 일시를 갱신하고 `status=renewed` 반환.

### 2.3 기기 충돌 감지

1. 다른 토큰이 들어오면 기존 토큰과 충돌로 판단.
2. 응답:  
   ```json
   {
     "status": "conflict",
     "message": "이미 다른 기기에서 알림을 받고 있습니다",
     "platform": "WEB",
     "lastUsed": "2025-01-15T12:34:56"
   }
   ```
3. **프런트 처리**  
   - 사용자에게 안내 모달 표시.  
   - 선택지: 임시 등록 또는 강제 교체.

### 2.4 임시 등록 (`temporaryOnly`)

- `/api/fcm/register` 호출 시 `temporaryOnly: true` 전달.
- Redis 임시 토큰으로 저장, DB는 유지.
- 응답 `status=temporary` → 로그인 세션 동안만 알림을 받음.

### 2.5 강제 기기 변경 (`POST /api/fcm/register/force`)

1. 기존 토큰을 사용하는 다른 사용자가 있으면 해당 플랫폼 토큰을 제거.
2. Redis 임시 토큰 제거.
3. 현재 사용자 DB 슬롯에 새 토큰 저장.
4. 응답 `status=replaced`, `keepSignedIn` 반영.

---

## 3. 로그아웃 / 토큰 삭제 (`DELETE /api/fcm/unregister`)

1. 프런트에서 로그아웃 시 현재 토큰과 플랫폼을 본문에 담아 호출:
   ```json
   {
     "fcmToken": "<FCM_DEVICE_TOKEN>",
     "platform": "WEB",
     "forceDelete": true
   }
   ```
2. 로직:
   - Redis 임시 토큰을 먼저 제거.
   - DB의 토큰이 요청 토큰과 일치하면 `keepSignedIn` 여부를 확인.  
     - `forceDelete=true` 이거나 `keepSignedIn`이 false/미설정이면 슬롯을 비움.
3. 사용자 휴면 토큰 자동 정리는 배치(`cleanupInactiveTokens`)가 담당.

---

## 4. 알림 전송 시나리오

### 4.1 단건 전송 (`POST /api/fcm/send`)

**요청 방식 1 – 기존 단일 사용자**
```json
{
  "userCode": "20241234",
  "title": "테스트 알림",
  "body": "내용",
  "data": { "type": "TEST" }
}
```

**요청 방식 2 – 신규 다중 사용자**
```json
{
  "targetType": "USER",
  "targeta": ["20241234", "20241235"],
  "title": "테스트 알림",
  "body": "내용",
  "data": { "type": "TEST" }
}
```

- 서비스는 `targetType + targeta` 가 있으면 다중 사용자 모드(`sendToMultipleUsers`)로 분기.
- 각 사용자/플랫폼별로 DB 토큰 및 Redis 임시 토큰에 순차 전송.
- 응답:
  - 단일 모드: `{ "sent": { "android": false, "web": true }, "failedReasons": { "android": "토큰이 등록되지 않았습니다" } }`
  - 다중 모드: `{ "sent": { "20241234": true, "20241235": false }, "failedReasons": { "20241235": "android: 토큰이 등록되지 않았습니다" } }`

### 4.2 일괄 전송 (`POST /api/fcm/send/batch`)

```json
{
  "userCodes": ["20241234", "20241235"],
  "title": "공지",
  "body": "전체 공지",
  "platforms": ["ANDROID", "WEB"],
  "data": { "category": "NOTICE" }
}
```

- 지정 플랫폼에 대해서만 전송.
- 사용자별 상세 결과(`details`)를 포함.

### 4.3 브로드캐스트 (`POST /api/fcm/send/broadcast`)

```json
{
  "title": "전체 방송",
  "body": "모두에게 전달",
  "platforms": ["WEB"],
  "filter": { "userType": "STUDENT" },
  "data": { "deeplink": "/notice" }
}
```

- 전체 토큰을 수집해 500개씩 분할 전송.
- 실패한 토큰 중 무효 토큰은 자동 제거.
- 응답은 총 토큰 수, 성공/실패 수, 무효화된 토큰 목록을 포함.

### 4.4 통계 조회 (`GET /api/fcm/stats`)

- 전체 사용자 수, 등록 사용자 수.
- 플랫폼별 토큰 수.
- 최근 30일 활성 / 90일 이상 미사용 토큰 수.

---

## 5. 프런트엔드 처리 권장 사항

1. **알림 초기화**  
   - 로그인 성공 시점에서 `PushNotificationService.initialize(userCode)` 호출 권장.  
   - 응답 `status`에 따라 충돌, 임시 등록 안내를 분기 처리.

2. **충돌 UX**  
   - `conflict` 응답일 경우 사용자 선택 UI 제공:  
     - 임시 등록: `temporaryOnly=true` 재요청.  
     - 강제 교체: `/api/fcm/register/force` 호출.

3. **알림 수신 UI**  
   - **포그라운드**: `setupForegroundListener()`가 발생시키는 `fcm-message` 이벤트를 구독해 UI 업데이트 (토스트, 배지 등).
     - ⚠️ **중요**: 중복 알림 방지를 위해 포그라운드에서는 `new Notification()` 호출을 제거하고 UI 이벤트만 발생시킵니다.
     - Service Worker가 모든 알림 표시를 담당합니다.
   - **백그라운드**: `firebase-messaging-sw.js`의 `onBackgroundMessage`가 자동으로 알림 표시.
     - 중복 방지를 위해 내용 기반 고유 `tag` 사용 권장 (예: `'notification-' + title + body.substring(0,20)`).
     - `renotify: false` 설정으로 재알림 시 진동 방지.
   - **클릭 이벤트**: `notificationclick`에서 `payload.data`를 활용해 특정 화면으로 라우팅.

4. **토큰 관리**  
   - `getCurrentToken()`으로 마지막 토큰을 보관해 로그아웃 시 `/unregister`에 전달.  
   - 토큰 갱신 필요 시 `refreshToken()` 호출.
   - 권장: 24시간마다 토큰 유효성 체크 로직 추가.

5. **환경 분리**  
   - VAPID 키와 백엔드 URL을 환경 변수로 분리하는 것을 권장.

6. **플랫폼 감지**  
   - User Agent 기반 플랫폼 자동 감지:
     ```javascript
     function detectPlatform() {
         const ua = navigator.userAgent.toLowerCase();
         if (/android/.test(ua)) return 'ANDROID';
         if (/iphone|ipad|ipod/.test(ua)) return 'IOS';
         return 'WEB';  // Windows, Mac, Linux 등
     }
     ```
   - ⚠️ User Agent는 변조 가능하므로 보안이 중요한 경우 추가 검증 필요.

---

## 6. 요약 플로우

1. **로그인 후 초기화**: `/register` → `registered` or `conflict` → 필요 시 `/register/force`/`temporaryOnly`.  
2. **알림 수신**: 포그라운드(Event) + 백그라운드(Service Worker).  
3. **관리자 발송**: `/send` 또는 `/send/batch`/`/send/broadcast`.  
4. **로그아웃**: `/unregister` 로 토큰 정리.  
5. **주기 관리**: `/stats`, `cleanupInactiveTokens`(스케줄러)로 상태 점검.

---

## 7. 알려진 이슈 및 해결책

### 7.1 중복 알림 이슈

#### 문제 1: 브로드캐스트 중복 전송
- **증상**: 같은 사용자에게 알림이 2번 도착
- **원인**: DB 토큰과 Redis 임시 토큰이 모두 전송 대상에 포함
- **해결**: 브로드캐스트(`/send/broadcast`)에서는 DB 영구 토큰만 사용
  - 임시 토큰은 개별 전송(`/send`)에서만 사용하도록 수정됨

#### 문제 2: 포그라운드/백그라운드 중복
- **증상**: 브라우저 탭이 열려있을 때 알림이 2번 표시
- **원인**: `onMessage` 리스너와 Service Worker가 둘 다 알림 표시
- **해결**: 포그라운드에서는 브라우저 알림 표시 제거, UI 이벤트만 발생
  ```javascript
  // ✅ 올바른 구현
  setupForegroundListener() {
      onMessage(messaging, (payload) => {
          // new Notification(...); ← 주석 처리 (중복 방지)
          
          // UI 이벤트만 발생
          window.dispatchEvent(new CustomEvent('fcm-message', {
              detail: payload
          }));
      });
  }
  ```

#### 문제 3: Service Worker 태그 중복
- **증상**: 같은 알림이 연속으로 여러 번 표시
- **원인**: `tag: 'notification-' + Date.now()` 사용으로 매번 새로운 알림 생성
- **해결**: 내용 기반 고유 태그 사용
  ```javascript
  // ✅ 올바른 구현
  const uniqueTag = 'notification-' + 
      (payload.notification?.title || '') + 
      (payload.notification?.body || '').substring(0, 20);
  
  const notificationOptions = {
      tag: uniqueTag,  // 같은 태그는 덮어쓰기
      renotify: false  // 재알림 시 진동 안함
  };
  ```

### 7.2 토큰 만료 이슈

- **증상**: 모바일 기기에서 알림이 오지 않음 (브로드캐스트 응답에 `invalidTokens` 포함)
- **원인**: FCM 토큰 만료 (앱 재설치, 데이터 삭제, 장기 미사용 등)
- **해결**: 
  - 백엔드가 자동으로 무효 토큰 감지 및 DB에서 제거
  - 사용자는 앱 재실행 또는 재로그인으로 새 토큰 등록
  - 권장: 프론트엔드에서 24시간마다 토큰 유효성 체크 및 재등록

### 7.3 플랫폼 감지 정확도

- **문제**: User Agent 기반 플랫폼 감지가 불완전 (Mac, Linux, iPad 등)
- **개선**: 포괄적인 패턴 매칭 사용
  ```javascript
  function detectPlatform() {
      const ua = navigator.userAgent.toLowerCase();
      
      if (/android/.test(ua)) {
          return 'ANDROID';
      } else if (/iphone|ipad|ipod/.test(ua)) {
          return 'IOS';
      } else {
          return 'WEB';  // Windows, Mac, Linux 모두 포함
      }
  }
  ```

### 7.4 네트워크 장애 대응

- **문제**: 알림 전송 실패 시 재시도 로직 부재
- **권장**: 
  - 백엔드: FCM 전송 실패 시 큐에 저장 후 재시도
  - 프론트엔드: 네트워크 복구 시 토큰 재등록 로직 추가

---

## 8. 부록

### 관련 문서
- [FCM_PUSH_NOTIFICATION_GUIDE.md](../backend/BlueCrab/FCM_PUSH_NOTIFICATION_GUIDE.md) - 상세 기술 문서
- [bugfix-duplicate-notifications.md](../claudedocs/bugfix-duplicate-notifications.md) - 중복 알림 수정 내역
- [Firebase Cloud Messaging 공식 문서](https://firebase.google.com/docs/cloud-messaging)

### 변경 이력
- **2025-10-14**: 중복 알림 이슈 섹션 추가, 프론트엔드 권장사항 업데이트

---

이 문서를 기반으로 프런트엔드와 백엔드가 동일한 시나리오를 공유하면, 기기 충돌, 다중 사용자 알림, 로그아웃 정리 등 세부 절차가 명확해집니다.
