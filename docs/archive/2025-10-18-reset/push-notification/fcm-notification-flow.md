# FCM 통합 시나리오 흐름

이 문서는 BlueCrab LMS에서 Firebase Cloud Messaging(FCM) 관련 클라이언트 ↔ 백엔드 상호작용을 한눈에 정리한 **인덱스 문서**입니다. 세부 가이드는 사용자용·관리자용 문서로 분리되어 있습니다.

---

## 1. 문서 구조

- [FCM 사용자 흐름](./fcm-notification-user.md)  
  로그인 직후 초기화, 충돌 처리, 임시 등록, 로그아웃 시 토큰 정리, 알림 UX, 플랫폼 감지 등 **사용자 단말 중심** 흐름을 다룹니다.

- [FCM 관리자 흐름](./fcm-notification-admin.md)  
  단건/다중/일괄/브로드캐스트 전송, 통계 조회 등 **관리자 알림 발송**과 관련된 시나리오를 정리했습니다.

---

## 6. 요약 플로우

| 단계 | 사용자/단말 | 관리자/운영 |
| --- | --- | --- |
| 로그인 직후 | `/api/fcm/register` 호출 → 충돌 시 임시 등록 또는 강제 교체 | - |
| 알림 수신 | Service Worker + 포그라운드 이벤트 처리 | - |
| 로그아웃 | `/api/fcm/unregister` 호출 | - |
| 알림 발송 | - | `/api/fcm/send`, `/batch`, `/broadcast` |
| 모니터링 | - | `/api/fcm/stats`, `cleanupInactiveTokens` 스케줄러 |

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
