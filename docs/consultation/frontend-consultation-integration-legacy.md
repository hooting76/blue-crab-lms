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
| HTTP 클라이언트 | **Native `fetch` API 사용** (프로젝트 표준 패턴)<br>- `src/api/profileApi.js`, `src/api/facility.js` 패턴 참조<br>- 401 자동 재시도 로직 포함 (`ensureAccessTokenOrRedirect`) |
| 인증 토큰 | `localStorage.user` 안의 `data.accessToken`, `data.refreshToken`<br>- 토큰 읽기: `readAccessToken()` 유틸 사용 (`utils/readAccessToken.js`)<br>- 참조: `component/auth/TokenAuth.jsx:15-16` |
| 필수 라이브러리 | **⚠️ 다음 라이브러리 설치 필수**: `@stomp/stompjs`, `sockjs-client` |
| 날짜 처리 | `date-fns` (✅ 이미 설치됨) - 날짜 계산 및 포맷팅 |
| 공통 헤더 | `Authorization: Bearer ${accessToken}`, `Content-Type: application/json` |
| 오류 처리 | 401/403 시 로그인 만료 알림 → 강제 로그아웃/재로그인 유도 |

**⚠️ 반드시 실행:**

```bash
npm install @stomp/stompjs sockjs-client
```

> **중요**:
> - **axios는 설치하지 않습니다** - 프로젝트는 native `fetch` API를 사용합니다.
> - 기존 API 모듈 (`profileApi.js`, `facility.js`)의 패턴을 반드시 따라주세요.
> - 401 재시도 로직은 `utils/authFlow.js`의 `ensureAccessTokenOrRedirect` 활용

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
| `ConsultationRequestDto` (응답) | `requestIdx`, `requesterUserCode`, `recipientUserCode`, `consultationType`, `title`, `requestStatus`, `consultationStatus`, `createdAt` | `content`, `desiredDate`, `acceptMessage`, `rejectionReason`, `cancelReason`, `scheduledStartAt`, `startedAt`, `endedAt`, `durationMinutes`, `lastActivityAt`, `hasUnreadMessages` | **전 목록/상세 응답 공통 포맷**<br>⚠️ **메모 필드 현재 DTO 누락** - Entity에는 존재하나 응답에 포함 안됨 |
| `ConsultationApproveDto` | `requestIdx` | `acceptMessage` | 교수용 |
| `ConsultationRejectDto` | `requestIdx`, `rejectionReason` |  | 교수용 |
| `ConsultationCancelDto` | `requestIdx`, `cancelReason` |  | 학생용 |
| `ConsultationIdDto` | `requestIdx` | | `/start`, `/end`, `/read` 등 공통 |
| `ConsultationMemoDto` | `requestIdx`, `memo` | | 교수만 작성 가능 (저장용) |
| `ConsultationHistoryRequestDto` | (`userCode`는 서버 세팅) | `startDate`, `endDate`, `page`, `size` | 날짜는 `yyyy-MM-dd HH:mm:ss` |
| `ChatMessageDto` | `requestIdx`, `content` | `sender`, `senderName`, `sentAt` | 전송 시 `sender*`는 서버 할당 |
| `ChatReadReceiptDto` (WebSocket) | `requestIdx`, `reader`, `readerName`, `readAt`, `lastActivityAt`, `allMessagesRead` | | 읽음 이벤트 payload |

> **⚠️ 중요 - 메모 기능 제한사항**:
> - `memo` 필드는 **메모 저장 API는 정상 작동**하나, **조회 시 응답 DTO에 누락**되어 있습니다.
> - 메모 표시가 필요하면 백엔드 팀에 `ConsultationRequestDto`에 `memo` 필드 추가를 요청하세요.
> - 임시 해결: 메모 저장 후 별도 상태로 관리하거나, 저장 성공 메시지만 표시

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

**프론트엔드 Blob 다운로드 구현 예시:**

```js
// src/api/consultationApi.js
import { readAccessToken } from '../utils/readAccessToken';

const BASE_URL = 'https://bluecrab.chickenkiller.com/BlueCrab-1.0.0/api/chat';

/**
 * 현재 진행 중인 채팅 로그 다운로드 (Redis)
 * @param {number} requestIdx - 상담 요청 ID
 */
export async function downloadChatLog(requestIdx) {
  const token = readAccessToken();
  if (!token) {
    throw new Error('인증이 필요합니다.');
  }

  const res = await fetch(`${BASE_URL}/history/download/${requestIdx}`, {
    method: 'POST',
    headers: {
      'Authorization': `Bearer ${token}`,
    }
  });

  if (!res.ok) {
    const errorText = await res.text();
    throw new Error(`다운로드 실패 (${res.status}): ${errorText}`);
  }

  const blob = await res.blob();
  const url = URL.createObjectURL(blob);

  const link = document.createElement('a');
  link.href = url;
  link.download = `chat-log-${requestIdx}-${new Date().getTime()}.txt`;
  document.body.appendChild(link);
  link.click();
  document.body.removeChild(link);

  URL.revokeObjectURL(url);
}

/**
 * 종료된 상담 아카이브 로그 다운로드 (MinIO)
 * @param {number} requestIdx - 상담 요청 ID
 */
export async function downloadArchivedLog(requestIdx) {
  const token = readAccessToken();
  if (!token) {
    throw new Error('인증이 필요합니다.');
  }

  const res = await fetch(`${BASE_URL}/archive/download/${requestIdx}`, {
    method: 'GET',
    headers: {
      'Authorization': `Bearer ${token}`,
    }
  });

  if (res.status === 404) {
    throw new Error('아카이브된 로그를 찾을 수 없습니다.');
  }

  if (!res.ok) {
    const errorText = await res.text();
    throw new Error(`다운로드 실패 (${res.status}): ${errorText}`);
  }

  const blob = await res.blob();
  const url = URL.createObjectURL(blob);

  const link = document.createElement('a');
  link.href = url;
  link.download = `archived-chat-log-${requestIdx}.txt`;
  document.body.appendChild(link);
  link.click();
  document.body.removeChild(link);

  URL.revokeObjectURL(url);
}
```

**사용 예시:**
```jsx
// 컴포넌트에서 사용
import { downloadChatLog, downloadArchivedLog } from '../api/consultationApi';

function ConsultationDetail({ consultation }) {
  const handleDownload = async () => {
    try {
      if (consultation.consultationStatus === 'COMPLETED') {
        // 종료된 상담 - 아카이브 시도, 실패 시 현재 로그
        try {
          await downloadArchivedLog(consultation.requestIdx);
        } catch (archiveError) {
          console.warn('아카이브 로그 없음, 현재 로그 다운로드:', archiveError);
          await downloadChatLog(consultation.requestIdx);
        }
      } else {
        // 진행 중 상담 - 현재 로그
        await downloadChatLog(consultation.requestIdx);
      }
    } catch (error) {
      alert(`다운로드 실패: ${error.message}`);
    }
  };

  return (
    <button onClick={handleDownload}>
      로그 다운로드
    </button>
  );
}
```

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

**완전한 구현 예시 (`src/utils/chatClient.js`):**

```js
// src/utils/chatClient.js
import { Client } from '@stomp/stompjs';
import SockJS from 'sockjs-client';
import { readAccessToken } from './readAccessToken';

const BASE_WS_URL = 'https://bluecrab.chickenkiller.com/BlueCrab-1.0.0/ws/chat';

/**
 * STOMP 채팅 클라이언트 생성
 * @param {Function} onMessage - 메시지 수신 콜백 (message: ChatMessageDto)
 * @param {Function} onReceipt - 읽음 이벤트 콜백 (receipt: ChatReadReceiptDto)
 * @param {Function} onError - 에러 콜백 (error: string)
 * @returns {Client} STOMP 클라이언트 인스턴스
 */
export function createChatClient(onMessage, onReceipt, onError) {
  const accessToken = readAccessToken();

  if (!accessToken) {
    const error = '인증 토큰이 없습니다. 로그인이 필요합니다.';
    console.error('[STOMP]', error);
    onError?.(error);
    throw new Error(error);
  }

  const client = new Client({
    webSocketFactory: () => new SockJS(`${BASE_WS_URL}?token=${accessToken}`),
    connectHeaders: {
      Authorization: `Bearer ${accessToken}`
    },
    debug: (str) => {
      if (process.env.NODE_ENV === 'development') {
        console.log('[STOMP]', str);
      }
    },
    reconnectDelay: 5000,
    heartbeatIncoming: 4000,
    heartbeatOutgoing: 4000,
  });

  client.onConnect = () => {
    console.log('[STOMP] 연결 성공');

    // 채팅 메시지 구독
    client.subscribe('/user/queue/chat', (frame) => {
      try {
        const message = JSON.parse(frame.body);
        console.log('[STOMP] 메시지 수신:', message);
        onMessage?.(message);
      } catch (error) {
        console.error('[STOMP] 메시지 파싱 실패:', error);
      }
    });

    // 읽음 이벤트 구독
    client.subscribe('/user/queue/read-receipts', (frame) => {
      try {
        const receipt = JSON.parse(frame.body);
        console.log('[STOMP] 읽음 이벤트 수신:', receipt);
        onReceipt?.(receipt);
      } catch (error) {
        console.error('[STOMP] 읽음 이벤트 파싱 실패:', error);
      }
    });
  };

  client.onStompError = (frame) => {
    const errorMsg = frame.headers['message'] || 'STOMP 프로토콜 에러';
    console.error('[STOMP] 에러:', errorMsg, frame);
    onError?.(errorMsg);
  };

  client.onWebSocketError = (evt) => {
    console.error('[STOMP] WebSocket 에러:', evt);
    onError?.('WebSocket 연결 실패. 네트워크를 확인하세요.');
  };

  client.onDisconnect = () => {
    console.log('[STOMP] 연결 종료');
  };

  client.activate();
  return client;
}

/**
 * 메시지 전송 헬퍼
 * @param {Client} client - STOMP 클라이언트 인스턴스
 * @param {number} requestIdx - 상담 요청 ID
 * @param {string} content - 메시지 내용
 */
export function sendChatMessage(client, requestIdx, content) {
  if (!client || !client.connected) {
    throw new Error('STOMP 클라이언트가 연결되지 않았습니다.');
  }

  client.publish({
    destination: '/app/chat.send',
    body: JSON.stringify({ requestIdx, content })
  });

  console.log('[STOMP] 메시지 전송:', { requestIdx, content });
}

/**
 * 읽음 처리 전송 헬퍼
 * @param {Client} client - STOMP 클라이언트 인스턴스
 * @param {number} requestIdx - 상담 요청 ID
 */
export function sendReadReceipt(client, requestIdx) {
  if (!client || !client.connected) {
    console.warn('[STOMP] 클라이언트 미연결, 읽음 처리 스킵');
    return;
  }

  client.publish({
    destination: '/app/chat.read',
    body: JSON.stringify({ requestIdx })
  });

  console.log('[STOMP] 읽음 처리 전송:', requestIdx);
}

/**
 * 클라이언트 정리 (컴포넌트 언마운트 시)
 * @param {Client} client - STOMP 클라이언트 인스턴스
 */
export function disconnectChatClient(client) {
  if (client && client.connected) {
    client.deactivate();
    console.log('[STOMP] 클라이언트 종료');
  }
}
```

**React 컴포넌트에서 사용 예시:**

```jsx
import { useEffect, useState, useCallback } from 'react';
import { createChatClient, sendChatMessage, sendReadReceipt, disconnectChatClient } from '../utils/chatClient';

function ChatRoom({ requestIdx }) {
  const [client, setClient] = useState(null);
  const [messages, setMessages] = useState([]);
  const [inputText, setInputText] = useState('');

  // STOMP 연결
  useEffect(() => {
    const stompClient = createChatClient(
      // onMessage
      (message) => {
        setMessages(prev => [...prev, message]);
      },
      // onReceipt
      (receipt) => {
        console.log('읽음 처리:', receipt);
        // 읽음 상태 UI 업데이트
      },
      // onError
      (error) => {
        alert(`채팅 연결 오류: ${error}`);
      }
    );

    setClient(stompClient);

    // 클린업
    return () => {
      disconnectChatClient(stompClient);
    };
  }, []);

  // 메시지 전송
  const handleSend = useCallback(() => {
    if (!inputText.trim() || !client) return;

    try {
      sendChatMessage(client, requestIdx, inputText);
      setInputText('');
    } catch (error) {
      alert(`전송 실패: ${error.message}`);
    }
  }, [client, requestIdx, inputText]);

  // 읽음 처리
  useEffect(() => {
    if (client && client.connected) {
      sendReadReceipt(client, requestIdx);
    }
  }, [client, requestIdx]);

  return (
    <div>
      <div className="messages">
        {messages.map((msg, idx) => (
          <div key={idx}>
            <strong>{msg.senderName}</strong>: {msg.content}
          </div>
        ))}
      </div>
      <input
        value={inputText}
        onChange={(e) => setInputText(e.target.value)}
        onKeyPress={(e) => e.key === 'Enter' && handleSend()}
      />
      <button onClick={handleSend}>전송</button>
    </div>
  );
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

**옵션 1 (추천): React Query 사용**
- 서버 상태 관리 최적화 (캐싱, 자동 리프레시, optimistic update)
- Query Keys 예: `['consultations', 'myRequests', params]`, `['consultations', 'detail', requestIdx]`
- **설치 필요**:
  ```bash
  npm install @tanstack/react-query
  ```

**옵션 2: 기존 Context API 확장**
- 추가 설치 없음
- `UserContext` 패턴을 따라 `ConsultationContext` 생성
- 장점: 일관된 코드 스타일 유지, 학습 곡선 없음
- 단점: 캐싱 및 서버 상태 동기화 수동 처리 필요

**로컬 상태**
- `selectedTab`, `selectedRequestIdx`, `messageList`, `pendingMessage`, `connectionStatus`

**역할 분기**
- `isProfessor = user.data.user.userStudent === 1`
- 사이드바(`component/common/MyPages/MyPageSidebar.jsx:99-101`) already toggles 메뉴. 동일 로직으로 탭 구성.

### 6.3 필수 UX 요소

**기본 UI 요소:**
- 요청 생성 모달(학생)
- 승인/반려/메모 모달(교수)
- `hasUnreadMessages === true`인 항목에 뱃지/볼드 처리
- 네트워크 끊김 표시 (STOMP 연결 상태)

**자동 종료 경고 표시:**
- **조건**: `consultationStatus === 'IN_PROGRESS'` AND `현재시간 - startedAt >= 23시간 50분`
- **계산 예시**:
  ```js
  import { differenceInMinutes } from 'date-fns';

  function isNearAutoClose(consultation) {
    if (consultation.consultationStatus !== 'IN_PROGRESS') return false;
    if (!consultation.startedAt) return false;

    const minutesElapsed = differenceInMinutes(
      new Date(),
      new Date(consultation.startedAt)
    );

    return minutesElapsed >= 23 * 60 + 50; // 1430분
  }
  ```
- **UI 위치**: 채팅 영역 상단에 경고 배너
- **메시지 예시**: "⚠️ 10분 후 자동 종료됩니다. 대화를 계속하려면 메시지를 전송하세요."

**로그 다운로드 버튼:**
- **표시 조건**: `consultationStatus === 'COMPLETED'` 또는 `'IN_PROGRESS'`
- **버튼 위치**: 상담 정보 카드 하단
- **동작**: 섹션 4.2의 `downloadChatLog` 또는 `downloadArchivedLog` 호출
- **에러 처리**: 404 시 "로그를 찾을 수 없습니다" 토스트 메시지

**상태별 UI 변화:**

| 상담 상태 | 표시할 버튼 | 채팅 입력창 | 비고 |
|-----------|-------------|-------------|------|
| `PENDING` (학생 요청) | [취소] | 비활성화 | "승인 대기 중" 안내 문구 |
| `PENDING` (교수 수신) | [승인] [반려] | 비활성화 | |
| `APPROVED` | [상담 시작] | 비활성화 | |
| `IN_PROGRESS` | [상담 종료] [메모(교수만)] | ✅ 활성화 | 자동 종료 경고 표시 |
| `COMPLETED` | [로그 다운로드] | 비활성화 | 종료 시각 표시 |
| `REJECTED` | - | 비활성화 | 반려 사유 표시 |
| `CANCELLED` | - | 비활성화 | 취소 사유 표시 |

### 6.4 API/소켓 호출 타이밍

**중요**: 호출 순서를 지켜야 데이터 정합성과 UX 품질을 보장할 수 있습니다.

#### 상세 호출 시퀀스

| 액션 | 호출 순서 | 비고 |
|------|-----------|------|
| **탭 진입** | 1. `GET /api/consultation/my-requests?page=0&size=20` (학생 - 내가 요청)<br>2. `GET /api/consultation/received?page=0&size=20` (교수 - 받은 요청)<br>3. `GET /api/consultation/active?page=0&size=20` (진행중)<br>4. `GET /api/consultation/history?page=0&size=20` (이력) | Tab별로 서버 페이징 적용, 미확인 뱃지 카운트 표시 |
| **상담 선택** | 1. `GET /api/consultation/{id}` (상담 상세 정보 로드)<br>2. `GET /api/chat/messages/{id}` (기존 채팅 메시지 로드)<br>3. STOMP `/app/chat.read` 전송 (읽음 처리)<br>4. STOMP 메시지 수신 대기 시작 | ⚠️ **순서 중요!** 메시지 로드 완료 후 읽음 처리 실행 |
| **메시지 전송** | 1. STOMP `/app/chat.send` 전송<br>2. 성공 시 자동으로 `/topic/chat/{requestIdx}` 수신<br>3. 본인 메시지도 토픽 통해 수신됨 (UI에 반영) | 실패 시 toast 표시 + 재전송 UI 제공 |
| **메모 저장 (교수만)** | 1. `POST /api/consultation/memo` (메모 저장)<br>2. 성공 시 `GET /api/consultation/{id}` 재조회<br>3. UI에 최신 메모 반영 | ⚠️ **주의**: 메모는 DTO에 없으므로 클라이언트 상태로만 보관 또는 별도 상태 관리 필요 |
| **상담 시작** | 1. `POST /api/consultation/start` (상담 시작)<br>2. 성공 시 `GET /api/consultation/{id}` 재조회<br>3. `consultationStatus`가 `IN_PROGRESS`로 변경 확인<br>4. 채팅 입력창 활성화 | 상태 변경 확인 후 UI 변경 |
| **상담 종료** | 1. `POST /api/consultation/end` (상담 종료)<br>2. 성공 시 STOMP 연결 끊기 (`disconnectChatClient`)<br>3. 목록 API 재호출 (리프레시)<br>4. 종료된 상담 UI 업데이트 | 종료 후 채팅 입력창 비활성화 |
| **새 상담 요청 (학생)** | 1. `POST /api/consultation/request` (새 요청 생성)<br>2. 성공 시 `GET /api/consultation/my-requests?page=0&size=20` 재호출<br>3. 생성된 요청으로 자동 포커스 | 성공 후 리스트 refresh하여 새 요청 표시 |
| **승인 (교수)** | 1. `POST /api/consultation/approve` (요청 승인)<br>2. 성공 시 `GET /api/consultation/{id}` 재조회<br>3. 목록 API 재호출<br>4. `requestStatus`가 `APPROVED`로 변경 확인 | 승인 후 "상담 시작" 버튼 표시 |
| **반려 (교수)** | 1. `POST /api/consultation/reject` (요청 반려)<br>2. 성공 시 목록 API 재호출<br>3. 해당 상담 목록에서 상태 업데이트 | 반려 사유 입력 모달 필수 |
| **취소 (학생)** | 1. `POST /api/consultation/cancel` (요청 취소)<br>2. 성공 시 목록 API 재호출<br>3. 해당 상담 목록에서 상태 업데이트 | `PENDING` 상태에서만 가능 |
| **로그 다운로드** | 1. `POST /api/chat/history/download/{id}` (진행중 상담)<br>   또는 `POST /api/chat/archived/download/{id}` (종료된 상담)<br>2. Blob 응답 처리<br>3. 파일 다운로드 트리거 | 섹션 4.2 참조, 404 시 "로그를 찾을 수 없습니다" 토스트 |

#### 호출 시 주의사항

1. **읽음 처리 타이밍**: 반드시 메시지 로드 후 실행 (순서 변경 시 읽음 처리 누락)
2. **상태 확인**: API 응답의 `consultationStatus`, `requestStatus` 필드로 상태 변화 검증 후 UI 변경
3. **STOMP 연결 관리**: 상담 종료 시 반드시 STOMP 연결 해제, 메모리 누수 방지
4. **에러 핸들링**: 모든 API 호출에 try-catch 적용, 실패 시 사용자 친화적 메시지 표시
5. **토큰 갱신**: 401 응답 시 토큰 갱신 후 자동 재시도 (기존 `ensureAccessTokenOrRedirect` 패턴 활용)

---

## 7. 구현 단계 체크리스트

### 1. 환경 세팅
- [ ] **STOMP 라이브러리 설치**
  ```bash
  npm install @stomp/stompjs sockjs-client
  ```
- [ ] **STOMP 헬퍼 유틸 제작** (`src/utils/chatClient.js`)
  - `createChatClient()` - STOMP 클라이언트 생성 (섹션 5.2 참조)
  - `sendChatMessage()` - 메시지 전송 헬퍼
  - `sendReadReceipt()` - 읽음 처리 헬퍼
  - `disconnectChatClient()` - 연결 종료 헬퍼
- [ ] **토큰 유틸 확인** (`src/utils/readAccessToken.js`)
  - 기존 `readAccessToken()` 활용
  - 401 자동 재시도 패턴 확인 (`ensureAccessTokenOrRedirect`)

### 2. 데이터 계층
- [ ] **상담 API 모듈 작성** (`src/api/consultationApi.js`)
  - Native fetch API 사용 (axios 사용 금지)
  - 기존 `profileApi.js`, `facility.js` 패턴 따르기
  - Authorization 헤더 자동 주입 (`Bearer ${readAccessToken()}`)
  - 401/403 에러 핸들링 (로그인 리다이렉트)
- [ ] **API 함수 구현**
  - `getMyRequests(page, size)` - 내 요청 목록
  - `getReceivedRequests(page, size)` - 받은 요청 목록 (교수)
  - `getActiveConsultations(page, size)` - 진행중 상담
  - `getHistory(page, size)` - 상담 이력
  - `getConsultationDetail(requestIdx)` - 상담 상세
  - `getChatMessages(requestIdx)` - 채팅 메시지 목록
  - `createRequest(data)` - 새 상담 요청
  - `approveRequest(requestIdx)` - 요청 승인
  - `rejectRequest(requestIdx, reason)` - 요청 반려
  - `cancelRequest(requestIdx, reason)` - 요청 취소
  - `startConsultation(requestIdx)` - 상담 시작
  - `endConsultation(requestIdx)` - 상담 종료
  - `saveMemo(requestIdx, memo)` - 메모 저장
  - `downloadChatLog(requestIdx)` - 로그 다운로드 (섹션 4.2 참조)
  - `downloadArchivedLog(requestIdx)` - 보관된 로그 다운로드

### 3. UI 구조
- [ ] **`Consult.jsx` 업데이트**
  - 전체 레이아웃 구성 (좌측 목록 패널 + 우측 상세 패널)
  - 탭 컴포넌트 구성 (요청함/진행중/이력/받은요청)
  - 역할별 탭 표시 (`user.data.user.userStudent === 1` 체크)
- [ ] **목록 컴포넌트 (`ConsultationList.jsx`)**
  - 페이징 처리 (서버 페이징 활용)
  - 필터링 (상태, 기간, 검색) - 클라이언트 사이드
  - 미확인 뱃지 표시 (`hasUnreadMessages === true`)
  - 선택된 항목 하이라이트
- [ ] **상세 카드 컴포넌트 (`ConsultationDetail.jsx`)**
  - 상담 정보 표시 (제목, 유형, 상태, 희망일, 메모)
  - 상태별 버튼 동적 표시 (섹션 6.3 표 참조)
    - `PENDING` (학생): [취소]
    - `PENDING` (교수): [승인] [반려]
    - `APPROVED`: [상담 시작]
    - `IN_PROGRESS`: [상담 종료] [메모(교수만)]
    - `COMPLETED`: [로그 다운로드]
  - 자동 종료 경고 배너 (`isNearAutoClose()` 활용)
- [ ] **채팅 컴포넌트 (`ChatPanel.jsx`)**
  - 메시지 리스트 (스크롤 영역)
  - 자동 스크롤 (새 메시지 수신 시 하단으로)
  - 메시지 말풍선 (본인/상대방 좌우 구분)
  - 입력창 + 전송 버튼
  - 상태별 입력창 활성화/비활성화 (섹션 6.3 표 참조)
  - STOMP 연결 상태 표시 (연결/끊김)

### 4. 상태 관리 & 소켓
- [ ] **상태 관리 선택**
  - **옵션 1 (추천)**: React Query 도입 (`npm install @tanstack/react-query`)
    - Query Keys: `['consultations', 'myRequests', params]`, `['consultations', 'detail', requestIdx]`
    - Optimistic update 전략 검토
  - **옵션 2**: 기존 Context API 확장 (`ConsultationContext.jsx` 생성)
- [ ] **STOMP 연결 생명주기 관리**
  - 컴포넌트 마운트 시 연결 (`useEffect`)
  - 상담 선택 시 구독 시작
  - 상담 종료 시 연결 해제
  - 컴포넌트 언마운트 시 클린업
- [ ] **로컬 상태 정의**
  - `selectedTab` - 현재 선택된 탭
  - `selectedRequestIdx` - 선택된 상담 ID
  - `messageList` - 채팅 메시지 리스트
  - `inputText` - 입력창 텍스트
  - `connectionStatus` - STOMP 연결 상태

### 5. 예외 처리
- [ ] **HTTP 에러 핸들링**
  - 401/403 → Toast 메시지 + 로그인 리다이렉트
  - 404 → "상담을 찾을 수 없습니다" 메시지
  - 500 → "서버 오류가 발생했습니다" 메시지
- [ ] **상태별 버튼 제어**
  - `PENDING` 아닌 요청: 승인/반려/취소 버튼 비활성화
  - `IN_PROGRESS` 아닌 상담: 채팅 입력창 비활성화
  - 권한 없는 작업: 버튼 숨김 또는 비활성화
- [ ] **STOMP 에러 핸들링**
  - 연결 실패 시 재연결 로직 (자동 재시도 설정됨)
  - 토큰 만료 시 로그인 리다이렉트
  - 메시지 전송 실패 시 Toast + 재전송 UI
- [ ] **자동 종료 안내**
  - 2시간 비활성 후 자동 종료 메시지 표시
  - 24시간 경과 강제 종료 안내
  - `isNearAutoClose()` 함수로 경고 배너 표시 (섹션 6.3 참조)

### 6. QA 테스트
- [ ] **전체 플로우 테스트**
  - 학생 계정: 요청 → 대기 → 승인 확인 → 채팅 → 종료 확인
  - 교수 계정: 요청 수신 → 승인 → 상담 시작 → 채팅 → 메모 저장 → 종료
- [ ] **WebSocket 안정성 테스트**
  - 네트워크 끊김 후 재연결 시 메시지 중복/손실 확인
  - 재연결 후 구독 자동 재생성 확인
  - 메시지 순서 보장 확인
- [ ] **읽음 처리 검증**
  - 양쪽 모두 `allMessagesRead === true` 동기화 확인
  - 읽음 처리 타이밍 순서 검증 (메시지 로드 후 읽음 처리)
  - 미확인 뱃지 동기화 확인
- [ ] **로그 다운로드 검증**
  - 진행중 상담 로그 다운로드 (UTF-8 인코딩 확인)
  - 종료된 상담 로그 다운로드 (MinIO 보관본)
  - 파일명 형식 확인 (`chat-log-{requestIdx}-{timestamp}.txt`)
- [ ] **UI/UX 검증**
  - 상태별 버튼 표시 정확성 (섹션 6.3 표 참조)
  - 자동 종료 경고 표시 (23시간 50분 이후)
  - 반려/취소 사유 표시 확인
  - 종료 시각 표시 확인

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

