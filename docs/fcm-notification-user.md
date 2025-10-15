# FCM 사용자 흐름 (프런트엔드)

이 문서는 BlueCrab LMS에서 **로그인한 사용자** 입장에서 필요한 Firebase Cloud Messaging(FCM) 처리 과정을 정리합니다. 현재 구현과 실제 요구 사항 사이의 차이를 ⚠️ 으로 표기했습니다.

---

## 1. 로그인 직후 초기화

- `PushNotificationService.initialize(userCode)` 호출 → Service Worker 등록 → 알림 권한 요청 → FCM 토큰 발급 → `/api/fcm/register` 호출 순서로 진행합니다.
- 요청 본문(현재 구현):
  ```json
  {
    "fcmToken": "<FCM_DEVICE_TOKEN>",
    "platform": "WEB"
  }
  ```
- ⚠️ `keepSignedIn`·`temporaryOnly` 값을 아직 전달하지 않으므로, 백엔드 기본값(둘 다 false)으로 처리됩니다.
- 백엔드는 `FcmRegisterResponse`의 `status`(registered/renewed/conflict/temporary/replaced)와 `keepSignedIn` 값을 반환합니다. **현재 프런트는 응답을 콘솔에만 남기므로, UI 분기/추가 호출이 필요합니다.**

---

## 2. 기기 충돌 및 임시 등록

1. 다른 기기 토큰과 충돌하면 응답이 `status=conflict` 로 내려옵니다. (`FcmTokenService.register`)
2. 사용자 선택 UX 제공 권장:
   - **임시 등록**: `/api/fcm/register` 호출 시 `temporaryOnly: true` 전달 → Redis 임시 토큰으로만 저장 → 로그인 세션 동안만 알림 수신 (`status=temporary`).
   - **강제 교체**: `/api/fcm/register/force` 호출 → 기존 토큰 제거 후 현재 기기로 교체 (`status=replaced`).
3. ⚠️ 임시 등록/강제 교체 요청은 아직 프런트 구현에 포함되어 있지 않습니다. 모달/토스트 등으로 응답 상태를 안내하고, 선택에 따라 API를 호출하도록 보완해야 합니다.

참고 백엔드 로직:  
`backend/BlueCrab/src/main/java/BlueCrab/com/example/service/FcmTokenService.java:180-239`, `:246-282`

---

## 3. 로그아웃 시 토큰 정리

```json
DELETE /api/fcm/unregister
{
  "fcmToken": "<FCM_DEVICE_TOKEN>",
  "platform": "WEB",
  "forceDelete": true
}
```

- Redis 임시 토큰을 먼저 삭제하고, DB 토큰이 일치하면 `keepSignedIn` 상태나 `forceDelete`에 따라 제거됩니다.
- ⚠️ 현재 로그아웃 플로우에는 `/unregister` 호출이 포함되어 있지 않습니다. `PushNotificationService.getCurrentToken()` 값을 사용해 토큰과 플랫폼을 함께 전달하도록 수정이 필요합니다.

참고 백엔드 로직:  
`backend/BlueCrab/src/main/java/BlueCrab/com/example/service/FcmTokenService.java:290-339`

---

## 4. 알림 수신 UX

- **포그라운드**: `setupForegroundListener()`가 `onMessage`로 수신한 payload를 처리합니다.  
  ⚠️ 현재 코드가 `new Notification()`을 호출하며, `tag: 'notification-' + Date.now()`를 사용해 동일 알림이 중복 노출됩니다. UI 이벤트(`CustomEvent`)만 발생시키고 브라우저 알림은 Service Worker에게 맡기는 방식으로 변경해야 합니다.
- **백그라운드 / Service Worker** (`public/firebase-messaging-sw.js`):
  - 메시지 수신 시 `tag: 'notification-' + Date.now()`로 알림을 등록합니다.
  - ⚠️ 동일한 내용도 항상 새로운 알림으로 분류되며, `renotify` 설정이 없어 진동/사운드가 반복됩니다.
  - **개선 권장**: 내용 기반 고유 `tag`, `renotify: false`, `payload.data` 기반 라우팅 링크 사용.
- **클릭 이벤트**: 현재는 고정 URL (`https://dtmch.synology.me:56000/`)만 열도록 구현되어 있습니다. payload의 `data.deeplink` 등을 활용해 적절한 SPA 라우팅을 수행하도록 수정하세요.

---

## 5. 플랫폼 감지 & 환경 변수

- 플랫폼은 `navigator.userAgent` 문자열을 기반으로 `WEB` / `ANDROID` / `IOS` 를 추정합니다.  
  ⚠️ iPad·Mac Safari 등 추가 케이스에 대한 보완이 필요하며, UA 변조 가능성도 대비해야 합니다.
- 백엔드 API URL과 VAPID 키를 환경 변수로 분리하는 것을 권장합니다. 현재는 `firebase/PushNotification.jsx`에 하드코딩돼 있습니다.

---

## 6. 사용자 측 알려진 이슈 & TODO

| 항목 | 상태 | 설명 |
| --- | --- | --- |
| 충돌 응답 처리 | ⚠️ 미구현 | `conflict` 응답 시 UI/추가 API 분기 필요 |
| 임시 토큰 등록 | ⚠️ 미지원 | `temporaryOnly` 전달 로직 없음 |
| 로그아웃 토큰 삭제 | ⚠️ 미구현 | `/unregister` 요청을 로그아웃 플로우에 편입 필요 |
| 중복 알림 | ⚠️ 개선 필요 | 포그라운드 `new Notification()` 제거, SW `tag`/`renotify` 보완 |
| 알림 클릭 라우팅 | ⚠️ 개선 필요 | payload 기반 화면 이동 로직 구현 |
| 환경 변수 분리 | ⚠️ 개선 필요 | VAPID/API URL 하드코딩 제거 |

---

## 7. 관련 문서

- [FCM 관리자 흐름](./fcm-notification-admin.md)
- [백엔드 FCM 상세 가이드](../backend/BlueCrab/FCM_PUSH_NOTIFICATION_GUIDE.md)
- [중복 알림 수정 내역](../claudedocs/bugfix-duplicate-notifications.md)
