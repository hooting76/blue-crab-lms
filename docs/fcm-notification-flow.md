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
   - `setupForegroundListener()` 가 발생시키는 `fcm-message` 이벤트를 구독해 토스트/배지 업데이트.  
   - `firebase-messaging-sw.js` 의 `notificationclick`에서 `payload.data`를 활용해 특정 화면으로 라우팅.

4. **토큰 관리**  
   - `getCurrentToken()`으로 마지막 토큰을 보관해 로그아웃 시 `/unregister`에 전달.  
   - 토큰 갱신 필요 시 `refreshToken()` 호출.

5. **환경 분리**  
   - VAPID 키와 백엔드 URL을 환경 변수로 분리하는 것을 권장.

---

## 6. 요약 플로우

1. **로그인 후 초기화**: `/register` → `registered` or `conflict` → 필요 시 `/register/force`/`temporaryOnly`.  
2. **알림 수신**: 포그라운드(Event) + 백그라운드(Service Worker).  
3. **관리자 발송**: `/send` 또는 `/send/batch`/`/send/broadcast`.  
4. **로그아웃**: `/unregister` 로 토큰 정리.  
5. **주기 관리**: `/stats`, `cleanupInactiveTokens`(스케줄러)로 상태 점검.

이 문서를 기반으로 프런트엔드와 백엔드가 동일한 시나리오를 공유하면, 기기 충돌, 다중 사용자 알림, 로그아웃 정리 등 세부 절차가 명확해집니다.
