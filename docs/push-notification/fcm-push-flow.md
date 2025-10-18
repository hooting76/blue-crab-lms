# BlueCrab FCM Push Flow

## 1. Initialization Sequence (Web Frontend)
1. `PushNotificationService.initialize(userId)` 호출
2. Service Worker 등록 → `/firebase-messaging-sw.js`
3. 브라우저 알림 권한 요청 (`Notification.requestPermission()`)
4. FCM 토큰 발급 (`getToken` with VAPID)
5. 토큰을 백엔드에 등록 `POST /api/fcm/register`
6. 성공 시 세션 스토리지에 토큰 보관 (`sessionStorage.setItem('fcm', token)`)
7. 필요 시 토큰 갱신 로직(`refreshToken`) 통해 재등록

> **참고**: 포그라운드 메시지 리스너(`onMessage`)는 현재 비활성화되어 있으며 UI 요구사항 확정 후 재연결 필요.

## 2. `/api/fcm/register` 응답 분기 처리

| status 값 | 설명/조건 | 프론트 처리 | 후속 API |
| --- | --- | --- | --- |
| `registered` | 신규 기기/플랫폼 최초 등록 | 알림 활성화 완료 안내, 별도 액션 없음 | 없음 |
| `renewed` | 동일 기기 재로그인 혹은 토큰 갱신 | “알림이 갱신되었습니다” 안내, 기존 UI 유지 | 필요 시 토큰 갱신 주기 기록 |
| `conflict` | 동일 플랫폼에 다른 기기가 이미 사용 중 | 기존 기기 정보(`platform`, `lastUsed`) 표시 → 사용자에게 교체/임시 등록 선택 요청 | 교체 승인 시 `POST /api/fcm/register/force`, 거부 시 `temporaryOnly` 플래그로 재등록 |
| `temporary` | 임시 로그인(예: keepSignedIn=false)으로 등록 | "임시로 알림 수신 중" 토스트/배너, 장기 사용 시 재요청 안내 | 임시 모드 연장/종료 UX 확정 필요 |
| `replaced` | 강제 교체 성공(`force`) 응답 | 교체 완료 안내, 이전 기기 로그아웃 안내 | 없음 |
| 기타 | 미정의 응답 | 오류 토스트 및 로그 기록 | QA 후 백엔드와 조율 |

## 3. 기타 관련 API 플로우

### 3.1 토큰 강제 교체
- Endpoint: `POST /api/fcm/register/force`
- 사용 시점: UI에서 `conflict` 응답 후 사용자가 "현재 기기로 변경" 선택 시
- 기대 응답: `status = replaced`

### 3.2 임시 토큰 등록
- Endpoint: `POST /api/fcm/register`
- 요청 시 Body 예시:
  ```json
  {
    "fcmToken": "CURRENT_TOKEN",
    "platform": "WEB",
    "temporaryOnly": true,
    "keepSignedIn": false
  }
  ```
- 사용 시점: `conflict` 응답 후 사용자가 기존 기기로 계속 수신하길 원할 때, 현재 기기에서는 로그인 세션 동안만 알림 수신
- 기대 응답: `status = temporary`

### 3.3 로그아웃/토큰 해제
- Endpoint: `DELETE /api/fcm/unregister`
- 호출 조건: 사용자 로그아웃, 토큰 무효화 필요 시
- 요청 본문 예시:
  ```json
  {
    "fcmToken": "CURRENT_TOKEN",
    "platform": "WEB"
  }
  ```

### 3.4 토큰 갱신 루틴
- 함수: `pushNotificationService.refreshToken(userId)`
- 동작: `getFCMToken` → `/api/fcm/register` 다시 호출 → 성공 시 기존 분기 로직 재사용
- 트리거: 서비스워커 갱신, 권한 갱신, 백엔드에서 토큰 만료 감지 시

## 4. 관리자 발송 시 참고
- 단일/다중 사용자: `POST /api/fcm/send`
- 다량 전송: `POST /api/fcm/send/batch`
- 전체 브로드캐스트: `POST /api/fcm/send/broadcast`
- 응답의 `sent`/`failedReasons`를 UI에 반영하여 성공/실패 플랫폼 확인 가능

## 5. 다음 작업 제안
- 프런트: 분기별 UX(토스트, 모달) 구현 및 QA 케이스 작성
- 백엔드/QA: 상태별 응답 예시 캡처, 테스트 스크립트 공유
- 문서: 실제 화면 적용 후 스크린샷 보완 예정
