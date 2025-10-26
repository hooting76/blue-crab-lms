# 실시간 상담 기능 최종 정리

## 1. 개요
- **범위**: 1:1 실시간 텍스트 상담(학생 ↔ 교수) 백엔드 구현
- **주요 목표**: 상담 요청/승인/거절/취소, WebSocket 실시간 채팅, 상담 시작/종료, Redis ↔ MinIO 백업, 푸시 알림 연동
- **최종 결정**: 일부 고급 항목(주말 장기 TTL 확장, MinIO 암호화, 알림 이력 테이블)은 후속 과제로 이관

## 2. 시스템 아키텍처
- **데이터 저장소**
  - 상담 메타: `CONSULTATION_REQUEST_TBL` (요청 + 진행 상태 통합 관리)
  - 실시간 메시지: Redis List (`chat:room:{requestIdx}`)
  - 아카이빙: MinIO (`archive/`, `temp/`)
- **통신 채널**
  - 실시간: Spring WebSocket (STOMP, `/user/queue/chat`)
  - REST: `ConsultationController`, `ChatRestController`
  - 푸시: FCM(Data 메시지) via `ChatNotificationService`
- **주요 의존 모듈**
  - `ConsultationRequestServiceImpl`: 상담 전반 로직
  - `ChatServiceImpl`: Redis 입출력, 로그 포맷팅
  - `ChatBackupScheduler`, `ConsultationAutoCloseScheduler`, `OrphanedRoomCleanupScheduler`
  - `MinIOService`: S3 호환 스토리지 핸들러

## 3. 주요 기능별 정리
### 3.1 상담 라이프사이클
- **요청 생성**: 학생이 교수에게 상담 요청(`POST /api/consultation/request`)
- **승인/반려**: 교수 처리(`POST /approve`, `/reject`), 승인 시 `ConsultationStatus = SCHEDULED`
- **취소**: 학생 취소(`POST /cancel`), 예정/대기 상태만 허용
- **상담 시작**: 참여자가 `POST /start` 호출 → `IN_PROGRESS` 전환, 타이밍 보정 위해 WebSocket 첫 메시지에서도 자동 전환
- **상담 종료**: `POST /end` 또는 자동 스케줄러 → MinIO 아카이빙, Redis 정리, `COMPLETED`

### 3.2 실시간 채팅
- **WebSocket 엔드포인트**: `/ws/chat`
- **메시지 흐름**: 클라이언트 → `ChatController` → Redis 저장 → 상대방 전송
- **읽음 처리**: `/api/consultation/read` 및 WebSocket 알림으로 `last_read_time_*` 업데이트
- **참여자 검증**: 모든 REST/WebSocket 진입점에서 `ConsultationRequestRepository.isParticipant` 기반 확인

### 3.3 데이터 백업 & 정리
- **주기 백업**: `ChatBackupScheduler` (5분 간격) → MinIO `temp/`에 최신 스냅샷 저장
- **임시 스냅샷 정리**: `OrphanedRoomCleanupScheduler` (일 1회) → 오래되거나 종료된 상담의 temp 객체 삭제
- **자동 종료**: `ConsultationAutoCloseScheduler`
  - 비활성 2시간 경과 상담 자동 종료 (`autoEndInactiveConsultations`)
  - 진행 24시간 초과 상담 강제 종료 (`autoEndLongRunningConsultations`)
  - 23h50m 경고 스케줄러는 TODO 상태

### 3.4 아카이빙 & 조회
- **종료 시 플로우**
  1. `archiveChatLog` → Redis 전체 메시지 포맷팅
  2. MinIO `archive/chat_{requestIdx}_final.txt` 업로드
  3. Redis 메시지 삭제 및 temp 스냅샷 정리
- **히스토리 조회**
  - 진행 중/완료 상담 목록: `GET/POST` 조합 (`/active`, `/history`, `/my-requests`, `/received`)
  - 아카이브 다운로드: `GET /api/chat/archive/download/{requestIdx}`

## 4. 보안·운영 고려사항
- **인증/인가**
  - Spring Security로 모든 상담/채팅 REST API 인증 필수
  - 교수 전용 API에 `ROLE_PROFESSOR` / `ROLE_ADMIN` 요구
  - WebSocket 연결 시 JWT 기반 사용자 식별 후 참여자 검증
- **로그 정책**
  - 서비스 전반에 구조화된 `log.info/warn/error` 메시지 존재
  - 푸시 알림 성공/실패 로그는 남지만 DB 이력 테이블은 미구현
- **운영상 주의**
  - Redis TTL 36시간 유지: 주말/연휴 장기 상담은 별도 모니터링 필요
  - MinIO 저장 데이터는 **평문** → 민감 정보 테스트 업로드 금지, 실서비스 적용 전 암호화 도입 권장
  - temp 스냅샷/자동 종료 임계치가 60시간 → 장기 상담 시 영향 가능

## 5. 테스트 & 검증 현황
- 단위 로직 예외 처리 로그 확인 및 수동 시나리오 테스트 완료
- WebSocket 메시지 송수신, 상담 상태 전이, MinIO 업로드/다운로드 수동 확인
- 스케줄러는 Cron 설정 기준으로 로컬 강제 실행 (`consultationRequestService` 직접 호출)으로 검증
- 통합 테스트 모듈은 추후 추가 예정 (현 시점 자동화 테스트 미구현)

## 6. 후속 과제(To-do)
| 구분 | 항목 | 비고 |
|------|------|------|
| 운영 안정성 | Redis TTL 7일+활동 리셋 | 주말/연휴 상담 안전장치 보강 |
| 보안 | MinIO 암호화 저장 (AES-GCM) | `EncryptionService` 도입, 업/다운로드 연계 |
| 감시/감사 | Notification 로그 테이블 | 실패 재시도, 감사 추적 |
| 백업 성능 | 백업 병렬화 & 변경 감지 | ThreadPool + 조건부 업로드 + 암호화 스냅샷 |
| 모니터링 | 23h50m 자동 종료 경고 구현 | FCM 알림 + UI 피드백 |
| 테스트 | 핵심 E2E 테스트 케이스 | 종료/자동 종료/백업 시나리오 자동화 |

## 7. 운영 가이드라인
1. **주말 상담**: 36시간 TTL을 넘길 경우 상담 재시작 또는 수동 백업 필요
2. **민감 정보 관리**: 암호화 도입 전까지 실데이터 투입 자제
3. **장시간 상담 모니터링**: 60시간 cleanup 임계 전 상태 확인
4. **장애 대응**: MinIO에서 `archive/` 및 `temp/` 객체로 복구 가능, Redis에는 데이터 유지 X

---
문의나 후속 조치 필요 시 `ConsultationRequestServiceImpl`, `ChatServiceImpl`, `ChatNotificationService`를 우선 검토하세요.
