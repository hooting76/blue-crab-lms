# FCM 푸시 알림 이중 표시 플로우

## 개요
- 증상: Chrome DevTools `Notification displayed` 로그가 하나의 푸시 이벤트에 대해 두 번씩 기록되며, 실제 브라우저 알림도 2회 뜸.
- 대상 경로: 백엔드 `FirebasePushService`, 프론트 `public/firebase-messaging-sw.js`.
- 핵심 원인: **백엔드가 notification 페이로드를 포함해 보낸 알림을 브라우저가 자동 표시**하고, **서비스워커가 같은 메시지를 `showNotification()`으로 다시 표시**함.

## 실행 흐름 상세
1. **FCM 메세지 전송 (백엔드)**  
   - 위치: `backend/BlueCrab/src/main/java/BlueCrab/com/example/service/FirebasePushService.java:42-99`  
   - 동작: `Message.builder()`에 `setWebpushConfig(buildWebpushConfig(title, body))`가 포함됨.  
   - 효과: FCM 메세지에 `notification` 블록이 포함되어 브라우저가 기본 UI 알림을 자동으로 띄움.

2. **브라우저 수신 & 기본 알림 표시**  
   - Chrome은 서비스워커가 활성화된 상태라도 `notification` 페이로드가 있으면 `Notification displayed` 이벤트를 자동으로 발생시키며 알림을 1회 출력.  
   - DevTools 로그 예: `Notification displayed` (Instance ID 비어 있음).

3. **Service Worker 백그라운드 리스너 실행**  
   - 위치: `public/firebase-messaging-sw.js:21-39`  
   - 동작: `messaging.onBackgroundMessage(payload => self.registration.showNotification(...))`.  
   - 같은 payload를 이용해 명시적으로 알림을 생성하면서 `tag` 값에 `notification-${Date.now()}`를 부여.

4. **두 번째 알림 출력**  
   - Service Worker가 호출한 `showNotification()` 때문에 같은 푸시 메세지가 다시 알림으로 렌더링됨.  
   - DevTools 로그 예: `Notification displayed` (Instance ID `notification-...`).

5. **사용자 체감**  
   - 화면에 시각적으로 동일한 알림이 약 0.01~0.02초 간격으로 연속 두 번 등장함.  
   - 로그에서도 동일 타임스탬프로 두 건의 `Notification displayed`가 기록됨.

## 해결 방향
- 방법 1: Service Worker에서 `showNotification()` 호출을 제거하고 브라우저 기본 표시만 사용한다.  
- 방법 2: 백엔드에서 `WebpushNotification`을 제거하고 data-only 페이로드로 전송한 뒤 Service Worker에서만 알림을 생성한다.  
- 핵심: `notification` 페이로드와 `showNotification()`을 동시에 사용하지 않도록 플로우를 단순화한다.

## 참고 로그
| 구분 | Timestamp | Event | 비고 |
| --- | --- | --- | --- |
| Push | 2025-10-17 11:40:35.790 | Push event dispatched | 1회만 발생 |
| Notification | 2025-10-17 11:40:35.791 | Notification displayed | 브라우저 자동 표시 |
| Notification | 2025-10-17 11:40:35.797 | Notification displayed | Service Worker `showNotification()` 결과 |

