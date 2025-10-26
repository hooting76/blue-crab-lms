# 실시간 상담 프론트엔드 연동 가이드

본 문서는 백엔드 실시간 상담 기능을 웹/모바일 클라이언트에서 활용하기 위한 연동 절차를 정리합니다.

---

## 1. 인증 및 공통 규칙
- **JWT 토큰 필수**: 모든 REST 호출과 WebSocket 연결 시 `Authorization: Bearer {token}` 필요.
- **사용자 식별**: 토큰에 들어있는 이메일 또는 userCode를 사용해 UI에서 접근 권한을 선행 검증.
- **시간 포맷**: API는 `yyyy-MM-dd HH:mm:ss` 문자열 반환 → 클라이언트에서 `Date` 변환 후 사용.
- **에러 응답**: 실패 시 `{ success: false, message: "...", timestamp: ... }` 형태. UI에서 `message` 노출.

---

## 2. 주요 REST API 요약

| HTTP | Endpoint | 설명 | 비고 |
|------|----------|------|------|
| POST | `/api/consultation/request` | 학생 상담 요청 생성 | Body: `ConsultationRequestCreateDto` |
| POST | `/api/consultation/approve` | 교수 상담 승인 | `requestIdx`, `acceptMessage` |
| POST | `/api/consultation/reject` | 교수 상담 반려 | `requestIdx`, `rejectionReason` |
| POST | `/api/consultation/cancel` | 학생 상담 취소 | `requestIdx`, `cancelReason` |
| POST | `/api/consultation/start` | 상담 시작 처리 | `ConsultationIdDto` |
| POST | `/api/consultation/end` | 상담 종료 처리 | `ConsultationIdDto` |
| POST | `/api/consultation/memo` | 교수 메모 작성/수정 | `requestIdx`, `memo` |
| POST | `/api/consultation/my-requests` | 학생이 신청한 상담 목록 | Query: `page`, `size`, `status` |
| POST | `/api/consultation/received` | 교수가 받은 요청 목록 | 위와 동일 |
| POST | `/api/consultation/active` | 진행 중 상담 목록 | Query: `page`, `size` |
| POST | `/api/consultation/history` | 완료 상담 이력 | Body: `userCode` 자동 세팅 |
| GET  | `/api/consultation/{requestIdx}` | 상담 상세 조회 | 참여자만 접근 허용 |
| GET  | `/api/consultation/unread-count` | 교수 미처리 요청 수 | 교수 전용 |
| POST | `/api/consultation/read` | 상담 읽음 처리 | Body: `requestIdx` |
| GET  | `/api/chat/messages/{requestIdx}` | Redis에 있는 메시지 전체 조회 | 참여자 확인 수행 |
| POST | `/api/chat/history/download/{requestIdx}` | 현재 Redis 로그 텍스트 다운로드 | 파일 응답 |
| GET  | `/api/chat/archive/download/{requestIdx}` | 종료 후 MinIO 저장 로그 다운로드 | 존재하지 않으면 404 |

### 2.1 DTO 필드 참고
`ConsultationRequestDto` 주요 필드:
- `requestIdx`, `requesterUserCode`, `requesterName`, `recipientUserCode`, `recipientName`
- `consultationType`, `title`, `content`, `desiredDate`
- `requestStatus` (`PENDING`, `APPROVED`, `REJECTED`, `CANCELLED`)
- `consultationStatus` (`SCHEDULED`, `IN_PROGRESS`, `COMPLETED`, `CANCELLED`)
- `startedAt`, `endedAt`, `durationMinutes`, `lastActivityAt`
- `hasUnreadMessages` (현재 백엔드에서 null 반환 가능, 추후 확장 고려)

---

## 3. WebSocket 연동

### 3.1 연결 및 구독
1. SockJS/STOMP 클라이언트 생성 → 엔드포인트: `/ws/chat`.
2. 연결 시 `Authorization` 헤더 또는 query string으로 JWT 전달.
3. 구독 채널:
   - `/user/queue/chat` : 수신 메시지
   - `/user/queue/errors` : 서버 에러 알림

### 3.2 메시지 전송
- 기본 전송 destination: `/pub/chat/send` (Spring STOMP 설정에 맞춰 확인).
- Payload 예시:
  ```json
  {
    "requestIdx": 123,
    "sender": "STU0001",
    "content": "안녕하세요 교수님"
  }
  ```
- 서버는 전송 후:
  - Redis 저장 (`ChatServiceImpl.saveMessage`)
  - 상대방에게 `/user/queue/chat` 브로드캐스트
  - `lastActivityAt` 갱신, 필요한 경우 상담 자동 시작 보정

### 3.3 읽음 처리
- 채팅 화면 진입 시 REST `POST /api/consultation/read` 호출 → 읽음 상태 갱신.
- WebSocket으로도 읽음 이벤트가 전송될 수 있으니 후처리 대비.

### 3.4 재연결 전략
- 36시간 TTL로 인해 장시간 미사용 시 Redis 키 만료 가능 → 주기적으로 heartbeat 또는 최신 메시지 재요청.
- 재연결 시 현재 상담 상태/최근 메시지를 REST로 먼저 동기화 추천.

---

## 4. 화면 설계 시 유의 사항
1. **상담 목록**
   - 상태별 탭(PENDING/APPROVED/IN_PROGRESS/COMPLETED) 구성.
   - `requestStatus`, `consultationStatus` 값으로 버튼 활성화 제어(예: 승인된 상담만 `시작` 표시).
2. **상담 상세/채팅 뷰**
   - 상단: 상담 정보(제목, 참여자, 희망일시, 메모).
   - 중단: 메시지 리스트(WebSocket 실시간 반영).
   - 하단: 입력창 + 전송 버튼.
   - 옵션: 파일 다운로드 버튼(`history`, `archive`).
3. **알림 처리**
   - FCM 수신 시 해당 상담 상세 페이지로 라우팅.
   - 백엔드에서 PENDING 상담 건수 제공 (`/unread-count`), 뱃지 표시 가능.
4. **자동 종료 안내**
   - 2시간 비활성 후 자동 종료, 24시간 초과 시 강제 종료 → UI에서 경고 배너 혹은 툴팁 제공.

---

## 5. 간단 예제 흐름 (학생)
1. 상담 요청 페이지에서 폼 제출 → `POST /api/consultation/request`.
2. 내 상담 목록(`my-requests`)에서 상태 확인.
3. 승인 알림 수신 후 채팅 화면 이동, WebSocket 연결.
4. 메시지 송수신 → WebSocket.
5. 상담 종료 버튼 → `POST /api/consultation/end`.
6. 완료 후 `history` 탭으로 조회, 필요 시 아카이브 다운로드.

## 6. 간단 예제 흐름 (교수)
1. 받은 요청 목록(`received`)에서 PENDING 확인.
2. 승인/반려 처리 → `/approve` 또는 `/reject`.
3. 승인된 상담은 `active` 목록에서 확인 후 WebSocket 참여.
4. 상담 종료 또는 자동 종료 결과 확인.
5. 메모 작성 → `/memo`.

---

## 7. 후속 적용 체크리스트
- [ ] REST API 클라이언트 모듈 작성 (axios 등)
- [ ] 상태 전환에 맞는 UI 컴포넌트 구성
- [ ] WebSocket 연결 모듈 (STOMP client) 구현
- [ ] 읽음 처리 및 알림 뱃지 연동
- [ ] 다운로드 기능(Blob 처리) 테스트
- [ ] 장시간 세션 시나리오 QA (자동 종료 메시지, TTL 만료 대응)

---

질문이나 추가 가이드가 필요하면 백엔드 담당자와 `ConsultationRequestServiceImpl`, `ChatController`의 실제 필드를 확인해 주세요.
