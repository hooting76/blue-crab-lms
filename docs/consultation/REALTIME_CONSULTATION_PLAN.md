# 실시간 상담 기능 설계 계획 (2025-10-22)

## 1. 목표 및 범위
- 학생-교수 간 1:1 실시간 텍스트 상담을 제공하고 상담 요청 → 채팅 → 종료 → 이력 관리까지 일련의 흐름을 지원한다.
- 기존 인증(JWT), 사용자 프로필, FCM 푸시 인프라와 자연스럽게 통합한다.
- MVP(Phase 1)부터 확장(Phase 4)까지 단계적으로 전달할 수 있는 아키텍처와 백로그를 정의한다.

## 2. 아키텍처 개요
- **백엔드**: Spring Boot 모놀리식 내부에 새로운 `consultation` 패키지를 추가한다. REST API + STOMP WebSocket 엔드포인트를 함께 제공하고, 상담 상태/메시지 저장은 Oracle DB를 사용한다. Redis(기존 인프라 재사용)로 세션 및 실시간 이벤트 보조 캐시를 지원한다.
- **프론트엔드**: Vite + React 기반 앱에 상담 전용 컨텍스트/Hook을 추가하고, WebSocket 클라이언트와 REST 클라이언트를 조합한다. 컴포넌트 분리는 요청 리스트, 상담방, 메시지 타임라인, 모달 알림으로 구성한다.
- **실시간 채널**: Spring WebSocket(STOMP) + SockJS fallback. 채팅/상태 이벤트는 `/topic/consultations/{roomId}` 브로드캐스트, 사용자별 큐는 `/queue/users/{userId}`를 활용한다.
- **알림**: 상담 상태 변경, 자동 종료 경고 등은 기존 FCM 토큰 인프라(`FcmTokenService`, `FirebasePushService`)를 재사용한다.

## 3. 도메인 모델
| 엔티티 | 주요 컬럼 | 설명 |
| --- | --- | --- |
| `ConsultationRequest` | `request_id`, `requester_id`, `responder_id`, `type`, `title`, `content`, `preferred_date`, `preferred_time`, `status`, `rejection_reason`, `created_at`, `updated_at` | 상담 요청 정보와 상태(`PENDING/ACCEPTED/REJECTED/CANCELLED`). |
| `ConsultationRoom` | `room_id`, `request_id`, `status`, `started_at`, `ended_at`, `closed_by`, `auto_closed`, `last_activity_at`, `created_at` | 상담이 수락되면 생성되는 1:1 방. 상태(`ACTIVE/COMPLETED/CLOSED_AUTO`). |
| `ConsultationMessage` | `message_id`, `room_id`, `sender_id`, `message_type`, `payload`, `sent_at`, `read_at`, `system_event` | 텍스트/시스템 메시지 저장. 읽음 처리와 시스템 이벤트 기록. |
| `ConsultationMemo` | `memo_id`, `room_id`, `author_id`, `content`, `created_at`, `updated_at` | 상담 종료 후 작성 가능한 메모. |
| `ConsultationAlarmLog` | `alarm_id`, `room_id`, `alarm_type`, `sent_at`, `acknowledged` | 자동 종료 경고, 리마인더 등 알림 기록. |
| (선택) `ConsultationParticipantState` | `room_id`, `user_id`, `has_unread`, `last_read_message_id`, `typing`, `presence` | 읽음/입력 표시를 위한 캐시 테이블 혹은 Redis 구조. |

## 4. 백엔드 구현 계획
### 4.1 패키지 구조
```
consultation/
├── controller/
│   ├── ConsultationRequestController.java
│   ├── ConsultationRoomController.java
│   ├── ConsultationWebSocketController.java (STOMP @MessageMapping)
├── dto/
├── entity/
├── repository/
├── service/
│   ├── ConsultationRequestService.java
│   ├── ConsultationRoomService.java
│   ├── ConsultationMessageService.java
│   ├── ConsultationNotificationService.java
│   └── ConsultationAutoCloseService.java
├── scheduler/
│   └── ConsultationAutoCloseScheduler.java
├── event/
│   └── ConsultationEventPublisher.java
└── config/
    └── ConsultationWebSocketConfig.java
```

### 4.2 REST API 설계 (초안)
| HTTP | 경로 | 설명 | Phase |
| --- | --- | --- | --- |
| `POST` | `/api/consultations/requests` | 상담 요청 생성 | 1 |
| `GET` | `/api/consultations/requests/incoming` | 받은 요청 목록 (필터/페이지) | 1 |
| `GET` | `/api/consultations/requests/outgoing` | 보낸 요청 목록 | 1 |
| `POST` | `/api/consultations/requests/{id}/accept` | 요청 수락 및 방 생성 | 1 |
| `POST` | `/api/consultations/requests/{id}/reject` | 요청 거절 | 1 |
| `POST` | `/api/consultations/requests/{id}/cancel` | 내가 보낸 대기중 요청 취소 | 1 |
| `PATCH` | `/api/consultations/requests/{id}` | 대기중 요청 수정 | 1 |
| `GET` | `/api/consultations/rooms` | 내 상담방 목록 | 1 |
| `GET` | `/api/consultations/rooms/{id}` | 상담방 상세 | 1 |
| `POST` | `/api/consultations/rooms/{id}/close` | 수동 종료 | 1 |
| `GET` | `/api/consultations/rooms/{id}/history` | 메시지 페이지네이션 조회 | 2 |
| `POST` | `/api/consultations/rooms/{id}/memo` | 상담 메모 작성/수정 | 3 |
| `GET` | `/api/consultations/history` | 완료된 상담 이력 + 필터 | 2 |
| `GET` | `/api/consultations/stats` | 상담 통계 | 3 |

### 4.3 WebSocket/STOMP 설계
- Endpoint: `ws://.../ws/consultations` + SockJS fallback.
- Subscriptions:
  - `/topic/consultations/{roomId}`: 채팅 메시지, 시스템 이벤트 브로드캐스트.
  - `/queue/consultations/{userId}`: 개인 알림(요청 수락/거절, 알림 뱃지).
- Messages:
  - **Client → Server**: `SendMessage`, `TypingStatus`, `ReadReceipt`, `ExtendSession`.
  - **Server → Client**: `MessageReceived`, `TypingUpdate`, `ReadReceiptUpdate`, `RoomStatusChanged`, `AutoCloseWarning`.
- Security: `JwtChannelInterceptor`로 Stomp 연결 시 토큰 검증 후 사용자 코드/ID를 Principal에 주입.

### 4.4 데이터베이스 및 마이그레이션
- 기존 `ddl/` 폴더에 Liquibase XML 또는 SQL 스크립트를 추가하여 테이블 생성.
- 인덱스 계획:
  - `ConsultationMessage`: `(room_id, sent_at)` 복합 인덱스.
  - `ConsultationRequest`: `(responder_id, status)`, `(requester_id, status)`.
  - `ConsultationRoom`: `(status)`, `(last_activity_at)`.
- 읽음/타이핑 정보는 초기에 Redis Hash나 Sorted set으로 저장하고, 정산용 스냅샷만 DB에 기록한다.

### 4.5 서비스 & 비즈니스 로직
- `ConsultationRequestService`: 상태 머신(`PENDING → ACCEPTED/REJECTED/CANCELLED`), 알림 트리거.
- `ConsultationRoomService`: 방 생성, 수동 종료, 상태 변경, `lastActivityAt` 업데이트.
- `ConsultationMessageService`: 메시지 저장, 페이지네이션, 시스템 메시지 삽입.
- `ConsultationNotificationService`: FCM/내부 알림 발송(요청 수락, 메시지 도착, 종료 알림, 자동 종료 경고).
- `ConsultationAutoCloseService`: 비활성 2시간, 24시간 최대 시간 제한 감시, `ConsultationAutoCloseScheduler`가 주기적으로 체크.

### 4.6 자동 종료 로직
1. 메시지 전송/읽음/연장 버튼 이벤트마다 `lastActivityAt` 갱신.
2. Scheduler가 5분 전에 경고 알림 이벤트를 생성(`AutoCloseWarning`).
3. 경고 후 연장 요청이 없으면 상담 종료 → 시스템 메시지 기록 + 방 상태 `CLOSED_AUTO`.
4. 로그(`ConsultationAlarmLog`)에 경고/종료 기록을 남긴다.

### 4.7 알림 채널 통합
- PUSH: `FcmTokenService.collectTokensByUserCodes`로 대상 토큰 조회 후 `FirebasePushService`로 전송.
- WebSocket: 동일 이벤트를 STOMP broadcast.
- UI Badge: `/queue/consultations/{userId}`로 읽지 않은 메시지/요청 수신 시 카운트 업데이트.

### 4.8 보안 및 권한
- 사용자 역할 기반 접근 제어(`@PreAuthorize` 혹은 커스텀 메서드 검증).
- 상담방 참여자만 메시지 읽기/쓰기 가능.
- 관리자(상담 운영자)만 특정 통계/이력 bulk 조회 접근 허용.

### 4.9 모니터링 & 감사
- MDC에 `roomId`, `requestId` 추가하여 로그 상관관계 확보.
- 중요 이벤트는 `ConsultationEventPublisher` → `ApplicationEvent` 로 발행 후 별도 감사 테이블 혹은 ELK로 전달.

## 5. 프론트엔드 구현 계획
### 5.1 상태 관리
- 새 `ConsultationContext`와 `useConsultation()` Hook 생성.
- Zustand/Redux를 사용 중인지 확인 후 동일 패턴 유지.
- 요청 목록, 방 상태, 읽음 상태, 알림 뱃지를 전역 상태로 관리.

### 5.2 UI 컴포넌트 초안
- `ConsultationRequestList.jsx`: 받은/보낸 요청 탭, 필터, 승인/거절/수정/취소 액션.
- `ConsultationRoomList.jsx`: 상담방 카드, 마지막 메시지, 읽지 않은 수 표시.
- `ConsultationChatWindow.jsx`: 메시지 타임라인, 입력 창, 읽음/타이핑 표시.
- `ConsultationRoomHeader.jsx`: 상대방 프로필, 상태, 남은 시간.
- `ConsultationMemoPanel.jsx`: 메모 작성/수정 UI.
- `ConsultationHistory.jsx`: 종료된 상담 검색/필터.
- 공통 모달: 자동 종료 경고, 수락/거절 확인.

### 5.3 WebSocket 클라이언트
- `ServiceWorkerFunc`와 충돌 없도록 별도 `ConsultationSocket.ts` (혹은 `.js`).
- Reconnect 전략: 토큰 만료 시 자동 재로그인 흐름(`AuthFunc`, `TokenAuth`)과 연계.
- 메시지 큐를 Store에 저장하여 optimistic UI 제공.

### 5.4 알림/타이머 표시
- 남은 시간/자동 종료 경고는 `useCountdownTimer` Hook 재사용 또는 신규 작성.
- 브라우저 푸시를 요청 상태/새 메시지에 연결 (권한 허용 시).

### 5.5 접근성 & 국제화
- 주요 텍스트는 i18n 리소스와 연동.
- 키보드 내비게이션, 스크린리더 아리아 속성 고려.

## 6. 배포 및 데이터 마이그레이션 전략
1. DB 스키마 → 스테이징 환경에 Liquibase 배포.
2. 백엔드 기능 플래그(`consultation.enabled`) 도입하여 단계적으로 노출.
3. 초기 베타에서는 교수/학생 파일럿 그룹만 접근 허용 (Role/Feature Flag).
4. 로그 및 모니터링을 통해 오류/성능 지표 수집 후 전체 배포.

## 7. 단계별 로드맵 (Phase 기준)
### Phase 1 (MVP)
- 상담 요청 CRUD, 상담방 생성/목록, 기본 메시지 송수신(WebSocket).
- 수동 종료, 읽지 않은 카운트, 최소한의 푸시 알림.
- 프론트: 요청/방/채팅 기본 UI.

### Phase 2 (핵심 기능)
- 읽음 확인, 알림 목록/필터, 상담 이력, 검색, 자동 종료(비활성/24시간) + 경고 모달.
- 푸시 + WebSocket 병행 알림, 상담 기록 PDF/Export 설계 착수.

### Phase 3 (부가 기능)
- 타이핑 표시, 메모 기능, 남은 시간 표시, 통계(월별/유형별 그래프).
- 알림 커스터마이징 옵션, 관리자용 대시보드 통합.

### Phase 4 (확장 기능)
- 파일/이미지 첨부, 화면 공유(WebRTC 고려), 예약 기능(캘린더와 연동), 상담 평가 시스템.
- 필요 시 별도 미디어 서비스(예: S3/MinIO)에 파일 저장.

## 8. 통합 고려사항
- **인증**: 기존 `JwtUtil` 확장을 통해 WebSocket Handshake에서 사용자 식별.
- **푸시**: FCM 토큰과 충돌하지 않도록 상담 알림 토픽/데이터 키 정의 (`type: consultation` 등).
- **로그인 흐름**: 기존 세션 타임아웃/자동 로그아웃 로직과 상담 자동 종료 로직 간 충돌 여부 확인.
- **보안**: 상담 메시지는 PII를 포함하므로 암호화(전송 TLS, 저장 시 민감 데이터 마스킹) 정책 검토.

## 9. 미해결 이슈 & 질문
1. 상담 요청 승인 권한: 교수만? 혹은 관리자 대리 승인 허용?
2. 상담 유형/메타데이터를 사용자 정의할 필요가 있는가?
3. 상담 메시지 보존 기간 및 개인정보 규정에 따른 삭제 정책은?
4. 상담 통계 대시보드 이용자(교수/관리자) 범위와 권한 정책?
5. 외부 알림(이메일/SMS) 필요 여부.

## 10. 다음 단계
1. 이해관계자 리뷰 후 도메인 모델과 API 초안 확정.
2. Liquibase 스크립트 및 엔티티 클래스 작성.
3. WebSocket 인프라 설정 + 기본 메시징 서비스 구현.
4. 프론트엔드 MVP 화면 와이어프레임/디자인 확정.
5. Phase 1 기능을 스테이징에 배포하여 조기 사용자 테스트 진행.
