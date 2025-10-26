# 프론트엔드 상담 기능 연동 가이드 (PC 우선)

> **목적**: 백엔드에 구현된 상담 관리·실시간 채팅 기능을 React 프론트엔드에 안정적으로 연결하기 위한 실무형 지침입니다.  
> **대상**: 마이페이지 `실시간 상담` 화면을 구현하는 프론트엔드 개발자.  
> **범위**: 데스크톱(PC) 뷰에 집중해 1차 기능 완성 → 이후 반응형 확장 예정.

---

## 1. 시스템 개요

- **참여자**
  - *학생*: 상담 요청 생성·취소, 진행 중 상담 참여, 이력 조회.
  - *교수/관리자*: 상담 요청 승인·반려, 메모 작성, 읽지 않은 건수 관리.
- **구현 축**
  - REST API (`/api/consultation/**`, `/api/chat/**`)로 상담 메타데이터 관리.
  - STOMP WebSocket (`/ws/chat`)으로 실시간 메시지 및 읽음 이벤트 처리.
  - Redis(메시지 36h TTL), Oracle DB(상태), MinIO(상담 종료 시 로그 아카이브) 백엔드에서 책임.
- **권한 판별**
  - JWT 기반 인증. `user.data.user.userStudent === 1`이면 교수, 0이면 학생(`context/UserContext.jsx:63` 기준).
- **1차 목표**
  - 상담 목록/상세/채팅/메모/상태 전환이 모두 동작하는 PC 전용 화면.
  - 에러/재연결/자동 종료(2h 비활성, 24h 장시간) 등 핵심 예외 처리.

---

## 2. 준비 사항

| 항목 | 내용 |
|------|------|
| API Base URL | `https://bluecrab.chickenkiller.com/BlueCrab-1.0.0` (기존 로그인 도메인과 동일) |
| 인증 토큰 | `localStorage.user` 안의 `data.accessToken`, `data.refreshToken` (`component/auth/TokenAuth.jsx:4`) |
| 필수 라이브러리 | `axios`, `@stomp/stompjs`, `sockjs-client`, (선택) `dayjs` 또는 `date-fns` |
| 공통 헤더 | `Authorization: Bearer ${accessToken}` |
| 오류 처리 | 401/403 시 로그인 만료 알림 → 강제 로그아웃/재로그인 유도 |

추가 설치 예시:

```bash
npm install axios @stomp/stompjs sockjs-client dayjs
```

---

## 3. 데이터 모델 한눈에 보기

### 3.1 상태 Enum (백엔드 기준)

| Enum | 값 | 의미 |
|------|-----|------|
| `RequestStatus` | `PENDING`, `APPROVED`, `REJECTED`, `CANCELLED` | 상담 요청 승인 전 단계 상태 |
| `ConsultationStatus` | `SCHEDULED`, `IN_PROGRESS`, `COMPLETED`, `CANCELLED` | 상담 진행 상태 |
| `ConsultationType` | `ACADEMIC`, `CAREER`, `CAMPUS_LIFE`, `ETC` | 상담 유형 (`ConsultationType.java`) |

### 3.2 주요 DTO 요약

| DTO | 필수 필드 | 선택 필드 | 비고 |
|-----|-----------|-----------|------|
| `ConsultationRequestCreateDto` | `recipientUserCode`, `consultationType`, `title` | `content`, `desiredDate` (`yyyy-MM-dd HH:mm:ss`) | `requesterUserCode`는 서버가 JWT로 설정 |
| `ConsultationRequestDto` (응답) | `requestIdx`, `requesterUserCode`, `recipientUserCode`, `consultationType`, `title`, `requestStatus`, `consultationStatus`, `createdAt` | `content`, `desiredDate`, `acceptMessage`, `rejectionReason`, `cancelReason`, `scheduledStartAt`, `startedAt`, `endedAt`, `durationMinutes`, `lastActivityAt`, `hasUnreadMessages` | 전 목록/상세 응답 공통 포맷 |
| `ConsultationApproveDto` | `requestIdx` | `acceptMessage` | 교수용 |
| `ConsultationRejectDto` | `requestIdx`, `rejectionReason` |  | 교수용 |
| `ConsultationCancelDto` | `requestIdx`, `cancelReason` |  | 학생용 |
| `ConsultationIdDto` | `requestIdx` | | `/start`, `/end`, `/read` 등 공통 |
| `ConsultationMemoDto` | `requestIdx` | `memo` | 교수만 작성 가능 |
| `ConsultationHistoryRequestDto` | (`userCode`는 서버 세팅) | `startDate`, `endDate`, `page`, `size` | 날짜는 `yyyy-MM-dd HH:mm:ss` |
| `ChatMessageDto` | `requestIdx`, `content` | `sender`, `senderName`, `sentAt` | 전송 시 `sender*`는 서버 할당 |
| `ChatReadReceiptDto` (WebSocket) | `requestIdx`, `reader`, `readerName`, `readAt`, `lastActivityAt`, `allMessagesRead` | | 읽음 이벤트 payload |

---

## 4. REST API 연동 가이드

### 4.1 목록·상세·상태 관리

| 구분 | Method & Path | 권한 | 주요 Query/Body | 성공 응답 요약 | 사용 시점 |
|------|---------------|------|-----------------|----------------|-----------|
| 상담 요청 생성 | `POST /api/consultation/request` | 학생 | Body: `recipientUserCode`, `consultationType`, `title`, `content?`, `desiredDate?` | `ConsultationRequestDto` (PENDING) | 상담 신청 폼 제출 |
| 요청 승인 | `POST /api/consultation/approve` | 교수 | Body: `requestIdx`, `acceptMessage?` | 승인된 DTO (`requestStatus=APPROVED`, `consultationStatus=SCHEDULED`) | 교수 승인 액션 |
| 요청 반려 | `POST /api/consultation/reject` | 교수 | Body: `requestIdx`, `rejectionReason` | 반려 DTO (`requestStatus=REJECTED`) | 교수 반려 |
| 요청 취소 | `POST /api/consultation/cancel` | 학생 | Body: `requestIdx`, `cancelReason` | 취소 DTO (`requestStatus=CANCELLED`) | 학생이 PENDING/APPROVED 취소 |
| 상담 시작 | `POST /api/consultation/start` | 학생·교수 | Body: `requestIdx` (`ConsultationIdDto`) | DTO (`consultationStatus=IN_PROGRESS`, `startedAt`) | 상담방 입장 직후(수동 시작 필요 시) |
| 상담 종료 | `POST /api/consultation/end` | 학생·교수 | Body: `requestIdx` | DTO (`consultationStatus=COMPLETED`, `endedAt`) | 상담 종료 버튼 |
| 상담 메모 | `POST /api/consultation/memo` | 교수 | Body: `requestIdx`, `memo` | DTO 갱신 | 상담 후 메모 저장 |
| 내가 보낸 요청 | `POST /api/consultation/my-requests?page&size&status?` | 학생 | Body: 없음 | Page 객체(`content` 배열 + `totalElements`, etc.) | 좌측 목록(학생) |
| 받은 요청 | `POST /api/consultation/received?page&size&status?` | 교수 | - | Page 응답 | 좌측 목록(교수) |
| 진행 중 상담 | `POST /api/consultation/active?page&size` | 공통 | - | Page 응답 (`consultationStatus IN_PROGRESS`) | 실시간 탭 |
| 상담 이력 | `POST /api/consultation/history` | 공통 | Body: `startDate?`, `endDate?`, `page?`, `size?` | Page 응답 (`consultationStatus=COMPLETED`) | 히스토리 탭 |
| 상담 상세 | `GET /api/consultation/{requestIdx}` | 공통 | Path: 상담 ID | `ConsultationRequestDto` | 상세 패널, 채팅 헤더 |
| 읽지 않은 요청 수 | `GET /api/consultation/unread-count` | 교수 | - | `{ recipientUserCode, unreadCount }` | 상단 뱃지 |
| 읽음 처리 | `POST /api/consultation/read` | 공통 | Body: `requestIdx` | `{ success, readAt, allMessagesRead, lastActivityAt, partnerUserCode }` | 채팅창 활성화 시 호출 |

**에러 포맷**: `{ "success": false, "message": "...", "timestamp": ... }` (400/403/500).  
**페이징**: Spring `Page` 포맷(`content`, `totalElements`, `totalPages`, `size`, `number`).  
**날짜 필드**: `yyyy-MM-dd HH:mm:ss` 문자열.

### 4.2 채팅 로그 REST

| 기능 | Method & Path | 설명 |
|------|---------------|------|
| Redis 메시지 조회 | `GET /api/chat/messages/{requestIdx}` | 상담 참여자만 허용. 응답 `{ success, data: { messages: ChatMessageDto[] } }` |
| 현재 로그 다운로드 | `POST /api/chat/history/download/{requestIdx}` | Redis 메시지를 텍스트 파일로 다운로드 (Blob 처리) |
| 종료 후 아카이브 다운로드 | `GET /api/chat/archive/download/{requestIdx}` | MinIO에 저장된 최종 로그 다운로드. 존재하지 않으면 404 |

프론트에서는 `axios`로 Blob 응답을 받아 파일 저장 처리(`URL.createObjectURL` 활용).

---

## 5. WebSocket 연동

### 5.1 연결 흐름

1. 로그인 후 `accessToken` 확보.
2. SockJS + STOMP 클라이언트로 `https://bluecrab.chickenkiller.com/BlueCrab-1.0.0/ws/chat?token=${accessToken}` 연결.
3. 연결 성공 시 구독:
   - `/user/queue/chat`: 실시간 메시지 수신.
   - `/user/queue/read-receipts`: 상대방 읽음 이벤트.
   - (선택) `/user/queue/errors`: 서버 에러 브로드캐스트 대비.
4. 상담방 입장 시 REST로 기존 메시지 preload → 이후 STOMP 메시지로 append.
5. 메시지 전송: `/app/chat.send` destination에 `{ requestIdx, content }` payload.
6. 읽음 처리: `/app/chat.read` destination에 `{ requestIdx }`.

### 5.2 STOMP 클라이언트 샘플

```ts
import { Client } from '@stomp/stompjs';
import SockJS from 'sockjs-client';

export function createChatClient(accessToken, onMessage, onReceipt, onError) {
  const client = new Client({
    webSocketFactory: () =>
      new SockJS(`https://bluecrab.chickenkiller.com/BlueCrab-1.0.0/ws/chat?token=${accessToken}`),
    connectHeaders: { Authorization: `Bearer ${accessToken}` },
    debug: (str) => console.log('[STOMP]', str),
    reconnectDelay: 5000,
  });

  client.onConnect = () => {
    client.subscribe('/user/queue/chat', (frame) => {
      onMessage(JSON.parse(frame.body));
    });
    client.subscribe('/user/queue/read-receipts', (frame) => {
      onReceipt(JSON.parse(frame.body));
    });
  };

  client.onStompError = (frame) => onError?.(frame.headers['message']);
  client.onWebSocketError = (evt) => onError?.(evt);

  client.activate();
  return client;
}
```

### 5.3 메시지 포맷

- **수신 메시지 (`ChatMessageDto`)**
  ```json
  {
    "requestIdx": 123,
    "sender": "202012345",
    "senderName": "홍길동",
    "content": "안녕하세요 교수님.",
    "sentAt": "2025-10-24 10:05:11"
  }
  ```
- **읽음 이벤트 (`ChatReadReceiptDto`)**
  ```json
  {
    "requestIdx": 123,
    "reader": "P001",
    "readerName": "김교수",
    "readAt": "2025-10-24 10:10:03",
    "lastActivityAt": "2025-10-24 10:08:57",
    "allMessagesRead": true
  }
  ```

### 5.4 예외 처리

- 토큰 만료 → 서버가 CONNECT 거부 ⇒ `onWebSocketError` → 로그인 재시도.
- `allMessagesRead=false` 상태가 유지되면 `/api/consultation/read` 재호출 및 UI 뱃지 유지.
- 재연결 시: 기존 구독 자동 재생성. 필요한 경우 현재 선택된 상담 ID로 `/api/chat/messages/{id}` 다시 당겨오며 역순 정합성 확인.

---

## 6. 프론트 UI & 상태 설계 (PC)

### 6.1 화면 와이어(텍스트)

```
┌─────────────────────────────────────────────┬───────────────────────────────────────┐
│ 좌측 패널 (목록 & 필터)                       │ 우측 패널 (상세 + 채팅)                 │
│ - 탭: [요청함|진행중|이력|받은요청(교수)]      │ ┌ 상담 정보 카드 ┐                     │
│ - 필터: 상태, 검색(제목/상대), 기간            │ │ 제목 / 유형 / 상태 / 희망일 / 메모  │
│ - 리스트: requestIdx, 상대, 상태, 미확인 뱃지  │ └───────────────┘                     │
│                                                 │ ┌ 채팅 메시지 영역 ┐                 │
│                                                 │ │ Scroll + 메시지 bubble (좌/우) │
│                                                 │ │ 입력창 + 전송 버튼             │
│                                                 │ └─────────────────────────────┘     │
└─────────────────────────────────────────────┴───────────────────────────────────────┘
```

### 6.2 상태 관리 제안

- **전역 스토어**: React Query 또는 Zustand 추천.
  - Query Keys 예: `['consultations', 'myRequests', params]`, `['consultations', 'detail', requestIdx]`.
- **로컬 상태**
  - `selectedTab`, `selectedRequestIdx`, `messageList`, `pendingMessage`, `connectionStatus`.
- **역할 분기**
  - `isProfessor = user.data.user.userStudent === 1`.
  - 사이드바(`component/common/MyPages/MyPageSidebar.jsx:73`) already toggles 메뉴. 동일 로직으로 탭 구성.

### 6.3 필수 UX 요소

- 요청 생성 모달(학생).
- 승인/반려/메모 모달(교수).
- `hasUnreadMessages` true인 항목에 뱃지/볼드 처리.
- 자동 종료 임박(23h50m 이상) 알림: `consultationStatus` + `lastActivityAt` 활용해 프론트 경고 표시.
- 로그 다운로드 버튼(조건: COMPLETED 상담).
- 네트워크 끊김 표시 (STOMP 연결 상태).

### 6.4 API/소켓 호출 타이밍

| 액션 | REST | WS | 비고 |
|------|------|----|------|
| 탭 진입 | `my-requests` / `received` / `active` / `history` | - | Tab별로 서버 paging |
| 상담 선택 | `GET /api/consultation/{id}` → `GET /api/chat/messages/{id}` | `/app/chat.read` | 메시지 로드 후 읽음 처리 |
| 메시지 전송 | - | `/app/chat.send` | 실패 시 toast + 재전송 UI |
| 메모 저장 | `POST /memo` | - | 성공 시 상세 재조회 |
| 상담 종료 | `POST /end` | - | 성공 시 목록 리프레시, STOMP disconnect |
| 새 상담 요청 | `POST /request` | - | 성공 후 리스트 refresh |

---

## 7. 구현 단계 체크리스트

1. **환경 세팅**
   - [ ] axios 클라이언트 공통화 (`Authorization` 헤더 주입, 401 핸들러).
   - [ ] STOMP 헬퍼 유틸 제작 (`createChatClient` 등).
2. **데이터 계층**
   - [ ] 상담 API 모듈 작성 (`src/api/consultation.ts` 등).
   - [ ] DTO 매핑 타입 정의(TypeScript 사용 시 인터페이스 생성).
3. **UI 구조**
   - [ ] `Consult.jsx` 업데이트: 레이아웃 + 탭 컴포넌트 구성.
   - [ ] 목록 컴포넌트: 페이징, 필터, 역할별 뷰.
   - [ ] 상세 카드: 상태별 버튼(승인/반려/취소/시작/종료/메모).
   - [ ] 채팅 컴포넌트: 메시지 리스트, 입력, 자동 스크롤, typing indicator(선택).
4. **상태 & 소켓**
   - [ ] React Query 쿼리/뮤테이션 구성, optimistic update 전략 검토.
   - [ ] STOMP 연결 생명주기 관리 (마운트/언마운트).
5. **예외 처리**
   - [ ] 401/403 → 토스트 + 로그인 리다이렉트.
   - [ ] `PENDING` 아닌 요청에 대한 승인/반려/취소 버튼 비활성화.
   - [ ] `IN_PROGRESS` 아닌 상담에 대해 채팅 입력 비활성화.
   - [ ] 자동 종료 후 수신 메시지(서버 알림) UI 표시.
6. **QA**
   - [ ] 학생·교수 계정으로 전체 플로우(요청→승인→채팅→종료) 테스트.
   - [ ] 네트워크 끊김 후 재연결 시 메시지 중복/손실 없는지 확인.
   - [ ] 읽음 처리 동기화(둘 다 `allMessagesRead` true 되는지) 확인.
   - [ ] 로그 다운로드 파일 내용 UTF-8 검증.
   - [ ] 2h 비활성 자동 종료(스케줄러) 대비 안내 문구 확인(백엔드 로그 필요 시 협업).

---

## 8. 운영 및 에지 케이스

- **Redis TTL 36h**: 장기 상담은 자동 종료/데이터 만료 위험 → 상담 재시작 안내 배너 고려.
- **MinIO 평문 저장**: 개인정보 포함 메시지 주의 안내(프론트에서 공지 문구/약관 표시 권장).
- **자동 종료**: 
  - 2시간 활동 없음 → 백엔드가 `COMPLETED` 전환. UI에서 "자동 종료" 라벨 표시.
  - 24시간 경과 → 강제 종료. 경과 시간 표기 (예: `durationMinutes`).
- **알림 연동**: FCM 데이터 payload에서 `type: consultation` 등 활용해 상담 화면으로 deep link 예정. PC 1차 구현에서는 실시간 뱃지 업데이트에 집중.
- **권한 오류 대비**: 요청자가 아닌 상담 접근 시 서버에서 403 → UI에서 "권한 없음" 안내 후 목록으로 리다이렉트.
- **동시편집**: 메모는 마지막 저장 기준 덮어쓰기. 저장 전 최신 상세 재조회 권장.

---

## 9. 참고 코드 포인터

- 백엔드 컨트롤러/서비스
  - `backend/BlueCrab/src/main/java/BlueCrab/com/example/controller/ConsultationController.java`
  - `backend/BlueCrab/src/main/java/BlueCrab/com/example/controller/ChatController.java`
  - `backend/BlueCrab/src/main/java/BlueCrab/com/example/controller/ChatRestController.java`
  - `backend/BlueCrab/src/main/java/BlueCrab/com/example/service/ConsultationRequestServiceImpl.java`
  - `backend/BlueCrab/src/main/java/BlueCrab/com/example/service/impl/ChatServiceImpl.java`
- 프론트 진입점
  - `component/common/MyPages/Consult.jsx` (빈 껍데기 – 본문 구현 필요)
  - `component/common/MyPages/MyPageSidebar.jsx` (사이드 메뉴)
  - `context/UserContext.jsx` / `component/auth/TokenAuth.jsx` (인증 상태)

---

## 10. 질문 가이드

작업 중 아래 항목이 불확실하면 백엔드 팀에 바로 확인하세요.

1. 교수/학생을 식별하는 표준 필드가 변경될 가능성?
2. 추가 상담 유형이나 상태 값 계획?
3. 자동 종료 알림(23h50m 경고) 스펙 확정 여부?
4. 향후 모바일/반응형 요구사항 예상 일정?
5. 실시간 알림(Firebase)과의 연동 순서?

---

### 부록 A. 주요 응답 샘플

#### 상담 상세 (`GET /api/consultation/{id}`)

```json
{
  "requestIdx": 123,
  "requesterUserCode": "202012345",
  "requesterName": "홍길동",
  "recipientUserCode": "P001",
  "recipientName": "김교수",
  "consultationType": "ACADEMIC",
  "title": "학점 상담 요청",
  "content": "학점 관리가 어렵습니다.",
  "desiredDate": "2025-10-25 14:00:00",
  "requestStatus": "APPROVED",
  "acceptMessage": "오후 2시에 연구실 방문",
  "consultationStatus": "SCHEDULED",
  "scheduledStartAt": null,
  "startedAt": null,
  "endedAt": null,
  "durationMinutes": null,
  "lastActivityAt": "2025-10-24 11:05:00",
  "createdAt": "2025-10-24 10:00:00",
  "hasUnreadMessages": true
}
```

#### 진행 중 목록 (`POST /api/consultation/active`)

```json
{
  "content": [
    {
      "requestIdx": 123,
      "recipientName": "김교수",
      "consultationType": "ACADEMIC",
      "title": "학점 상담 요청",
      "consultationStatus": "IN_PROGRESS",
      "lastActivityAt": "2025-10-24 10:58:00",
      "hasUnreadMessages": false
    }
  ],
  "totalElements": 1,
  "totalPages": 1,
  "size": 20,
  "number": 0
}
```

---

## 11. 마무리

- 본 문서는 PC 기능 구현이 완료될 때까지 living document로 유지합니다.
- 작업 진행 중 발견되는 백엔드/프론트 스펙 차이는 **즉시 문서 반영** 후 팀 공유 바랍니다.
- 1차 기능 릴리스 후, 반응형 레이아웃 및 모바일 UX 요구사항을 별도 섹션으로 추가 예정입니다.

> ⚙️ **다음 단계 제안**  
> 1. API 모듈 & STOMP 헬퍼부터 구현 → 목업 데이터로 UI 틀 작성.  
> 2. 학생/교수 더미 계정으로 end-to-end 현황 캡처 후 QA 체크리스트 갱신.  
> 3. 종료/아카이브 로그 다운로드 기능까지 검증되면, 반응형/푸시 알림 작업 순차 진행.

