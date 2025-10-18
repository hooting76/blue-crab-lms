# FCM 관리자 흐름 (프런트엔드/백오피스)

이 문서는 BlueCrab LMS에서 **관리자**가 Firebase Cloud Messaging(FCM)을 통해 알림을 발송할 때 필요한 클라이언트 ↔ 백엔드 상호작용을 정리했습니다. 사용자는 반드시 JWT 인증이 된 상태여야 하며, 모든 엔드포인트는 `Authorization: Bearer {accessToken}` 헤더를 요구합니다.

---

## 1. 단건/다중 사용자 전송 (`POST /api/fcm/send`)

### 요청 형식
- **단일 사용자**:
  ```json
  {
    "userCode": "20241234",
    "title": "테스트 알림",
    "body": "내용",
    "data": { "type": "TEST" }
  }
  ```
- **다중 사용자(User 타입)**:
  ```json
  {
    "targetType": "USER",
    "targeta": ["20241234", "20241235"],
    "title": "테스트 알림",
    "body": "내용",
    "data": { "type": "TEST" }
  }
  ```

### 응답
- 단일 모드: 플랫폼별 성공 여부 (android/ios/web)
- 다중 모드: 사용자별 성공 여부
- 실패 시 `failedReasons` 필드에 원인 포함 (예: 토큰 미등록)

### 구현 참고
- 백엔드는 사용자별 FCM 토큰을 조회해 플랫폼별로 전송하며, Redis 임시 토큰도 포함합니다.
- ⚠️ 전송 결과를 UI에 표시하고 재시도/무효 토큰 정리를 안내하는 UX는 아직 정의되지 않았습니다.

참고 코드:  
`backend/BlueCrab/src/main/java/BlueCrab/com/example/service/FcmTokenService.java:345-438`

---

## 2. 일괄 전송 (`POST /api/fcm/send/batch`)

```json
{
  "userCodes": ["20241234", "20241235"],
  "title": "공지",
  "body": "전체 공지",
  "platforms": ["ANDROID", "WEB"],
  "data": { "category": "NOTICE" }
}
```

- 지정한 플랫폼에 대해서만 전송합니다(누락 시 전체 플랫폼).
- 응답에는 전체 전송 수, 성공/실패 수, 사용자별 상세 결과(`details`)가 포함됩니다.
- UI에서는 사용자별 실패 사유를 보여주고 관리자 재전송/무시 등 후속 조치를 제공하는 흐름이 필요합니다.

참고 코드:  
`backend/BlueCrab/src/main/java/BlueCrab/com/example/service/FcmTokenService.java:508-615`

---

## 3. 브로드캐스트 (`POST /api/fcm/send/broadcast`)

```json
{
  "title": "전체 방송",
  "body": "모두에게 전달",
  "platforms": ["WEB"],
  "filter": { "userType": "STUDENT" },
  "data": { "deeplink": "/notice" }
}
```

- 플랫폼별 DB **영구 토큰**만 대상으로 하며, 500개씩 분할 전송합니다.
- 전송 결과에 `invalidTokens` 목록이 포함되어 자동 삭제됩니다.
- ⚠️ UI에서는 총 전송/성공/실패 수와 함께 무효 토큰을 확인하거나, 일부 실패 시 재전송 플로우를 제공하는 것이 좋습니다.

참고 코드:  
`backend/BlueCrab/src/main/java/BlueCrab/com/example/service/FcmTokenService.java:639-737`

---

## 4. 통계 조회 (`GET /api/fcm/stats`)

- 응답 항목
  - `totalUsers`: 전체 사용자 수
  - `registeredUsers`: FCM 토큰 등록 사용자 수
  - `byPlatform`: 플랫폼별 토큰 수 (android/ios/web)
  - `activeTokens`: 최근 30일 활성 토큰 수
  - `inactiveTokens`: 90일 이상 미사용 토큰 수
- 관리자 화면에서는 등록률, 플랫폼별 비율, 비활성 사용자 관리 등을 시각화할 수 있습니다.

참고 코드:  
`backend/BlueCrab/src/main/java/BlueCrab/com/example/service/FcmTokenService.java:740-812`

---

## 5. 관리자 관점의 알려진 이슈 & TODO

| 항목 | 상태 | 설명 |
| --- | --- | --- |
| 전송 실패 UX | ⚠️ 미정 | 실패 사유, 재시도, 무효 토큰 알림 등 후속 처리 필요 |
| 브로드캐스트 중복 감시 | ✅ 처리됨 | 임시 토큰 제외 전송 (DB 영구 토큰만 사용) |
| 전송 로그/추적 | ⚠️ 미정 | 성공/실패 로그를 저장·조회하는 UI 미구현 |
| 통계 화면 | ⚠️ 미정 | API는 존재하나 시각화/필터 UI 없음 |

---

## 6. 관련 문서

- [FCM 사용자 흐름](./fcm-notification-user.md)
- [백엔드 FCM 상세 가이드](../backend/BlueCrab/FCM_PUSH_NOTIFICATION_GUIDE.md)
- [중복 알림 수정 내역](../claudedocs/bugfix-duplicate-notifications.md)
