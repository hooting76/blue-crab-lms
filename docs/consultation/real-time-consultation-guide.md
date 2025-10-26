# 실시간 상담 통합 가이드

통합 문서로 백엔드 구조 요약부터 프론트엔드 연동, 운영 체크포인트, 후속 과제까지 한눈에 정리합니다.  
이 문서를 기준으로 전체 흐름을 파악하고, 세부 구현 시 기존 개별 문서(필요 시 `final-summary.md`, `frontend-integration-guide.md`)를 참고하세요.

---

## 1. 시스템 개요
- **범위**: 학생과 교수 간 1:1 실시간 상담 (요청 → 승인 → 채팅 → 종료 → 아카이빙).
- **핵심 요소**
  - DB: `CONSULTATION_REQUEST_TBL` 단일 테이블로 메타데이터 관리.
  - Redis: 실시간 채팅 메시지 저장(`chat:room:{requestIdx}`), TTL 36시간.
  - MinIO: 상담 종료 시 아카이빙(`archive/`) 및 주기 백업(`temp/`).
  - WebSocket: `/ws/chat` STOMP 엔드포인트, `/user/queue/chat` 구독.
  - REST API: 상담 요청/관리, 채팅 로그 조회, 읽음 처리 등.
  - FCM: 상담 알림(요청/승인/메시지/종료 등).
- **현재 결정 사항**
  - Redis TTL 36시간 유지(주말/장기 상담은 운영 가이드 필요).
  - MinIO 평문 저장(향후 AES-GCM 암호화 예정).
  - 푸시 알림 DB 로그 미구현(필요 시 추후 도입).

---

## 2. 백엔드 요약

### 2.1 주요 컴포넌트
- `ConsultationController`, `ChatRestController`: 상담 REST API.
- `ChatController`: WebSocket 메시지 처리.
- `ConsultationRequestServiceImpl`: 상담 비즈니스 로직.
- `ChatServiceImpl`: Redis 저장/조회, 로그 포맷팅.
- `MinIOService`: MinIO 업/다운로드.
- `ChatNotificationService`: FCM 알림 및 배치 처리.
- 스케줄러
  - `ChatBackupScheduler`: 5분마다 temp 백업(순차 처리).
  - `ConsultationAutoCloseScheduler`: 2h 비활성/24h 장시간 자동 종료.
  - `OrphanedRoomCleanupScheduler`: 60시간 지난 temp 스냅샷 정리.

### 2.2 상담 플로우
1. **요청**: `POST /api/consultation/request`.
2. **승인/반려**: 교수용 `/approve`, `/reject`.
3. **취소**: 학생 `/cancel` (PENDING/APPROVED에서만).
4. **시작**: `/start` → `IN_PROGRESS` 전환(또는 첫 메시지 시 자동 전환 보정).
5. **채팅**: WebSocket + Redis, 읽음 처리 `/read`.
6. **종료**: `/end` 또는 스케줄러 → MinIO 아카이빙, Redis 정리, 상태 `COMPLETED`.
7. **이력 조회**: `/history`, `/active`, `/my-requests`, `/received`, `/api/chat/archive/download/{requestIdx}` 등.

---

## 3. 프론트엔드 연동 순서

### Step 1. 준비
- JWT 로그인 플로우 확인(토큰 저장/갱신).
- `@stomp/stompjs`, `sockjs-client`, `axios` 등 설치.

### Step 2. REST API 모듈
- `axios` 인스턴스 생성 (`Authorization` 자동 주입).
- 상담 API 함수 구현:
  - 목록: `/my-requests`, `/received`, `/active`, `/history`.
  - 상세: `/api/consultation/{id}`.
  - 상태 변경: `/request`, `/approve`, `/reject`, `/cancel`, `/start`, `/end`, `/memo`, `/read`.
  - 채팅 로그: `/api/chat/messages/{requestIdx}`, `/api/chat/history/download/{requestIdx}`, `/api/chat/archive/download/{requestIdx}`.

### Step 3. 상태 관리 & 페이지 구조
- `component/common/MyPages/Consult.jsx` 확장:
  - 좌측: 상담 목록 + 필터(학생/교수 역할별).
  - 우측 상단: 상담 상세 정보(제목, 희망일, 상태, 메모).
  - 우측 하단: 채팅 영역(WebSocket 메시지 리스트 + 입력폼).
- 상태 관리 도구(React Query/Zustand/Context 등)로 상담 리스트/선택 상담/메시지 관리.

### Step 4. WebSocket 모듈
- STOMP 클라이언트 생성(연결/재연결/종료).
- 구독: `/user/queue/chat`, `/user/queue/errors`.
- 전송: `/pub/chat/send`에 `{ requestIdx, sender, content }`.
- 상담 화면 진입 시:
  1. REST로 기존 메시지/상담 정보 로드.
  2. WebSocket 연결 및 구독.
  3. `/api/consultation/read` 호출로 읽음 처리.
- 화면 이탈 시 WebSocket disconnect.

### Step 5. UI 액션 & 알림
- 승인/거절/취소/시작/종료 버튼은 상태에 따라 조건부 렌더링.
- 교수 대시보드에 `/unread-count`로 미처리 뱃지 표시.
- FCM 수신 시 상담 상세 페이지로 이동하도록 라우터 연동.
- 로그 다운로드 버튼(Blob 처리) 제공.

### Step 6. QA 시나리오
- 상담 전 라이프사이클 전체 플로우 테스트.
- WebSocket 재연결, 네트워크 끊김, 자동 종료(2h/24h) 메시지 확인.
- Redis TTL 만료 대비: 장기 상담 시 주기적으로 메시지/상태 리프레시.

---

## 4. 운영 및 보안 주의
- **TTL 36시간**: 주말/연휴 장기 상담은 수동 백업 또는 상담 재시작 안내 필요.
- **MinIO 평문 저장**: 민감 정보 업로드 지양, 운영 전 암호화 도입 권장.
- **푸시 알림**: FCM 전송 로그는 애플리케이션 로그에만 남음(별도 DB 로그 미구현).
- **자동 종료 임계치**: 2h 비활성, 24h 장시간 → UI에서 경고 배너/툴팁 제공.
- **오류 대응**: MinIO `archive/`에서 로그 복구 가능(단 종료된 상담만).

---

## 5. 후속 과제(To-do)

| 우선순위 | 항목 | 요약 |
|----------|------|------|
| 높음 | Redis TTL 7일 + 활동 리셋 | 주말 상담 데이터 보존 |
| 높음 | MinIO 암호화(AES-GCM) | 개인정보 보호 |
| 중간 | Notification 로그 테이블 | 푸시 재시도/감사 추적 |
| 중간 | 백업 병렬화 & 조건부 업로드 | 스냅샷 성능/보안 개선 |
| 중간 | 23h50m 자동 종료 경고 | 사용자 사전 알림 |
| 중간 | E2E 테스트 작성 | 종료/자동 종료/백업 시나리오 |

---

## 6. 참고 파일
- 백엔드 코드
  - `ConsultationRequestServiceImpl`, `ChatServiceImpl`, `ChatController`, `ChatRestController`, `ChatNotificationService`
  - 스케줄러: `ChatBackupScheduler`, `ConsultationAutoCloseScheduler`, `OrphanedRoomCleanupScheduler`
- 프론트 진입점
  - `component/common/MyPages/Consult.jsx` : 상담 메인 페이지 확장 필요
  - `src/api/` : 상담 REST 모듈 추가 위치
  - WebSocket 헬퍼: `utils/` 또는 `hook/` 내부 신규 생성 권장

필요 시 세부 구현 문서는 `docs/consultation/final-summary.md`, `docs/consultation/frontend-integration-guide.md`에서 확인하세요.
