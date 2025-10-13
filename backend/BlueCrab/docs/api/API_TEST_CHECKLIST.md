# BlueCrab API 테스트 체크리스트

> 코드베이스(`backend/BlueCrab/src/main/java/BlueCrab/com/example/controller`) 기준으로 모든 공개 REST 엔드포인트를 정리했습니다.  
> 기본 응답 포맷은 `ApiResponse<T>`이며, 별도 표기가 없는 한 JSON 본문을 사용합니다.

## 1. 인증 · 계정 회복

| Endpoint | Method | Auth | 요청 데이터 | 주요 비고 |
|----------|--------|------|-------------|-----------|
| `/api/auth/login` | POST | - | Body `{ "username", "password" }` | 성공 시 `LoginResponse`(access/refresh 토큰) |
| `/api/auth/refresh` | POST | - | Body `{ "refreshToken" }` | 새 토큰 페어 발급 |
| `/api/auth/logout` | POST | `Authorization: Bearer <access>` | Body `{ "refreshToken" }` | Access·Refresh 모두 무효화, 검증 실패 시 400/401 |
| `/api/auth/validate` | GET | `Authorization: Bearer <access>` | - | 토큰 유효성 확인용 핑 |
| `/api/account/FindId` | POST | permitAll | Body `{ "userCode", "userName", "userPhone" }` | 이메일 마스킹 반환, 레이트리밋 적용 |
| `/api/auth/config` | GET | permitAll | - | 메일 인증 코드 정책(길이, TTL 등) |
| `/sendMail` | GET | `Authorization: Bearer <access>` | Header 기반 | 로그인 사용자의 이메일 인증 코드 발송 (MailAuthCheckController) |
| `/verifyCode` | POST | `Authorization: Bearer <access>` | Body `{ "authCode" }` | 인증 코드 검증 (MailAuthCheckController) |
| `/api/auth/password-reset/verify-identity` | POST | permitAll | Body `{ "email", "userCode", "userName", "userPhone" }` | 본인확인 단계, `identityToken` 반환 시 있음 |
| `/api/auth/password-reset/send-email` | POST | permitAll | Body `{ "irtToken" }` | 2단계 이메일 발송, IRT 토큰 필수 |
| `/api/auth/password-reset/verify-code` | POST | permitAll | Body `{ "irtToken", "authCode" }` | 코드 검증 결과/잔여 시도수 반환 |
| `/api/auth/password-reset/change-password` | POST | permitAll | Header `X-RT`, Body `{ "newPassword" }` | 리셋 토큰 단일 사용, 비밀번호 정책 정규식 적용 |
| `/api/auth/password-reset/rate-limit-status` | GET | permitAll | Query `email` (선택) | 디버그용 레이트리밋 현황 |

## 2. 관리자 인증 · 시설 승인

| Endpoint | Method | Auth | 요청 데이터 | 주요 비고 |
|----------|--------|------|-------------|-----------|
| `/api/admin/login` | POST | permitAll | Body `{ "adminId", "password" }` | 1차 로그인, 임시 세션 토큰 발급 |
| `/api/admin/verify-email` | GET | permitAll | Query `token` | 이메일 링크를 통한 2차 인증 |
| `/api/admin/email-auth/request` | POST | `Authorization: Bearer <admin-session>` | Header 기반 | 임시 토큰으로 인증 코드 요청 (Redis 저장) |
| `/api/admin/email-auth/verify` | POST | `Authorization: Bearer <admin-session>` | Body `{ "authCode" }` | 코드 검증 후 최종 JWT 발급 |
| `/api/admin/reservations/pending` | POST | `Authorization: Bearer <admin-access>` | - | 승인 대기 목록 조회 |
| `/api/admin/reservations/pending/count` | POST | `Authorization: Bearer <admin-access>` | - | 대기 건수 |
| `/api/admin/reservations/approve` | POST | `Authorization: Bearer <admin-access>` | Body `{ "reservationIdx", "adminNote" }` | 예약 승인 |
| `/api/admin/reservations/reject` | POST | `Authorization: Bearer <admin-access>` | Body `{ "reservationIdx", "rejectionReason" }` | 예약 반려 |
| `/api/admin/reservations/stats` | POST | `Authorization: Bearer <admin-access>` | Query `startDate`, `endDate` (ISO, 선택) | 대시보드 통계 |
| `/api/admin/reservations/all` | POST | `Authorization: Bearer <admin-access>` | Query `status`, `facilityIdx`, `startDate`, `endDate` | 필터링된 전체 예약 조회 |

## 3. 사용자 · 프로필

| Endpoint | Method | Auth | 요청 데이터 | 주요 비고 |
|----------|--------|------|-------------|-----------|
| `/api/users` | GET | `Authorization: Bearer <access>` | - | 전체 사용자 (보안 유의) |
| `/api/users/students` | GET | Bearer | - | 학생 사용자 목록 |
| `/api/users/professors` | GET | Bearer | - | 교수 사용자 목록 |
| `/api/users/{id}` | GET | Bearer | Path `id` | 개별 사용자 |
| `/api/users` | POST | Bearer | Body = `UserTbl` 필드 | 새 사용자 생성 |
| `/api/users/{id}` | PUT | Bearer | Body = `UserTbl` 필드 | 사용자 수정 |
| `/api/users/{id}` | DELETE | Bearer | Path `id` | 소프트 삭제 |
| `/api/users/{id}/toggle-role` | PATCH | Bearer | Path `id` | 학생↔교수 토글 |
| `/api/users/search` | GET | Bearer | Query `name` | 이름 부분 검색 |
| `/api/users/search-all` | GET | Bearer | Query `keyword` | 이름·이메일 통합 검색 |
| `/api/users/search-birth` | GET | Bearer | Query `startDate`,`endDate` (YYYYMMDD) | 생년월일 범위 |
| `/api/users/stats` | GET | Bearer | - | 전체/학생/교수 수 및 비율 |
| `/api/profile/me` | POST | Bearer | Header `Authorization` | 로그인 사용자 프로필 |
| `/api/profile/me/completeness` | POST | Bearer | Header `Authorization` | 프로필 완성도 체크 |
| `/api/profile/me/image` | POST | Bearer | Header `Authorization` | Presigned URL 등 이미지 정보 |
| `/api/profile/me/image/file` | POST | Bearer | Body `{ "imageKey" }` | 실제 이미지 바이너리 (본인 검증) |

## 4. 게시판 · 첨부파일

> `/api/boards/**`는 SecurityConfig에서 `permitAll`이지만, 작성/수정/삭제/첨부 관련 엔드포인트는 내부적으로 JWT 검증을 수행합니다.

| Endpoint | Method | Auth | 요청 데이터 | 주요 비고 |
|----------|--------|------|-------------|-----------|
| `/api/boards/list` | POST | - | Body `{ "page", "size" }` | 페이징 목록 (본문 제외) |
| `/api/boards/bycode` | POST | - | Body `{ "boardCode", "page", "size" }` | 코드별 목록 |
| `/api/boards/detail` | POST | - | Body `{ "boardIdx" }` | 상세 + 첨부 포함 |
| `/api/boards/create` | POST | Bearer(내부검증) | Body = `BoardTbl` | 제목·본문 Base64 인코딩 검증 |
| `/api/boards/update/{boardIdx}` | POST | Bearer(내부검증) | Body = `BoardTbl` | 작성자 권한 체크 |
| `/api/boards/delete/{boardIdx}` | POST | Bearer(내부검증) | Path `boardIdx` | 비활성화 삭제 |
| `/api/boards/link-attachments/{boardIdx}` | POST | Bearer(내부검증) | Body `{ "attachmentIdx": [...] }` | 게시글-첨부 연결 |
| `/api/boards/count` | POST | - | - | 활성 게시글 수 |
| `/api/boards/exists` | POST | - | Body `{ "boardIdx" }` | 존재 여부 |
| `/api/boards/count/bycode` | POST | - | Body `{ "boardCode" }` | 코드별 개수 |
| `/api/board-attachments/upload/{boardIdx}` | POST | Bearer | Multipart `files[]` | 첨부 업로드, JWT 필수 |
| `/api/board-attachments/delete` | POST | Bearer | Body `{ "attachmentIds": [...] }` | 첨부 삭제 |
| `/api/board-attachments/delete-all/{boardIdx}` | POST | Bearer | Path `boardIdx` | 게시글 첨부 일괄 삭제 |
| `/api/board-attachments/download` | POST | Bearer 권장 | Body `{ "attachmentIdx" }` | MinIO에서 스트리밍 다운로드 |
| `/api/board-attachments/download/health` | POST | - | - | 다운로드 서비스 헬스체크 |
| `/api/board-attachments/info` | POST | Bearer 권장 | Body `{ "attachmentIdx" }` | 메타데이터 확인 |

## 5. 시설 예약 · 열람실

| Endpoint | Method | Auth | 요청 데이터 | 주요 비고 |
|----------|--------|------|-------------|-----------|
| `/api/facilities` | POST | Bearer | - | 활성 시설 전체 |
| `/api/facilities/type/{facilityType}` | POST | Bearer | Path `facilityType` | `FacilityType` enum (예: `READING_ROOM`) |
| `/api/facilities/{facilityIdx}` | POST | Bearer | Path `facilityIdx` | 단일 시설 |
| `/api/facilities/search` | POST | Bearer | Query `keyword` | 명칭 검색 |
| `/api/facilities/{facilityIdx}/availability` | POST | Bearer | Query `startTime`,`endTime` (ISO) | 가용성 확인 |
| `/api/reservations` | POST | Bearer | Body `{ "facilityIdx", "startTime", "endTime", ... }` | 예약 생성, 자동 승인/대기 메시지 |
| `/api/reservations/my` | POST | Bearer | - | 내 예약 전체 |
| `/api/reservations/my/status/{status}` | POST | Bearer | Path `status` (`PENDING` 등) | 상태별 내 예약 |
| `/api/reservations/{reservationIdx}` | POST | Bearer | Path `reservationIdx` | 예약 상세 |
| `/api/reservations/{reservationIdx}` | DELETE | Bearer | Path `reservationIdx` | 예약 취소 |
| `/api/reading-room/status` | POST | Bearer | Header `Authorization` | 전체 좌석 현황, RateLimit(10초/5회) |
| `/api/reading-room/reserve` | POST | Bearer | Body `{ "seatNumber" }` | 좌석 예약, RateLimit(1분/3회) |
| `/api/reading-room/checkout` | POST | Bearer | Body `{ "seatNumber" }` | 좌석 퇴실, RateLimit(30초/2회) |
| `/api/reading-room/my-reservation` | POST | Bearer | Header `Authorization` | 내 좌석 조회, RateLimit(5초/10회) |

### 개발·점검용 (좌석·DB)

| Endpoint | Method | Auth | 요청 데이터 | 주요 비고 |
|----------|--------|------|-------------|-----------|
| `/api/test/lamp-table-status` | GET | Bearer | - | 좌석 테이블 상태 (샘플 포함) |
| `/api/test/lamp-table-simple` | GET | Bearer | - | 단순 카운트 |
| `/api/test/initialize-seats` | GET | Bearer | - | 좌석 1~80 초기화 |
| `/api/test/reset-all-seats` | GET | Bearer | - | 좌석 점유 해제 |

## 6. 알림 · 푸시 (FCM / WebPush)

| Endpoint | Method | Auth | 요청 데이터 | 주요 비고 |
|----------|--------|------|-------------|-----------|
| `/api/fcm/register` | POST | `Bearer (isAuthenticated)` | Body `{ "fcmToken", "platform", "keepSignedIn?", "temporaryOnly?" }` | 토큰 등록 (중복 감지) |
| `/api/fcm/register/force` | POST | Bearer | Body `{ ... }` | 기존 토큰 강제 교체 |
| `/api/fcm/unregister` | DELETE | Bearer | Body `{ "fcmToken" }` | 토큰 삭제 |
| `/api/fcm/send` | POST | `Bearer (ROLE_ADMIN)` | Body `{ "targets", "title", "body", "data" }` | 지정 사용자 알림 |
| `/api/fcm/send/batch` | POST | `Bearer (ROLE_ADMIN)` | Body `{ "tokens": [...], ... }` | 다중 토큰 일괄 전송 |
| `/api/fcm/send/broadcast` | POST | `Bearer (ROLE_ADMIN)` | Body `{ "title", "body", "data" }` | 전체 브로드캐스트 |
| `/api/fcm/stats` | GET | `Bearer (ROLE_ADMIN)` | - | 등록 토큰 통계 |
| `/api/push/vapid-key` | GET | permitAll (firebase.enabled=true 시) | - | WebPush VAPID 공개키 |
| `/api/push/send` | POST | `Bearer (ROLE_ADMIN)` | Body `{ "token", "title", "body", "data" }` | WebPush 단건 |
| `/api/push/send-to-topic` | POST | `Bearer (ROLE_ADMIN)` | Body `{ "topic", "title", "body", "data" }` | 토픽 전송 |
| `/api/test/firebase-status` | GET | Bearer (조건부) | - | Firebase 초기화 확인 |
| `/api/test/vapid-key` | GET | Bearer (조건부) | - | 테스트용 VAPID 키 |
| `/api/test/send-test-push` | POST | Bearer | Query `token`, `title?`, `body?`, `testKey` | `testKey=test123` 요구 |
| `/api/test/send-topic-test` | POST | Bearer | Query `topic?`, `title?`, `body?`, `testKey` | 토픽 테스트 (testKey 필요) |

## 7. 시스템 · 모니터링 · DB

| Endpoint | Method | Auth | 요청 데이터 | 주요 비고 |
|----------|--------|------|-------------|-----------|
| `/api/health` | GET | Bearer | - | 애플리케이션/DB/메모리 상태 |
| `/api/system-info` | GET | Bearer | - | JVM/OS/메모리 상세 |
| `/api/test` | GET | Bearer | - | API 연결 테스트 |
| `/api/test-auth` | GET | Bearer | Header `Authorization` | Principal 이름 포함 응답 |
| `/api/ping` | GET | permitAll | - | 가장 가벼운 핑 |
| `/api/database/info` | GET | Bearer | - | DB 메타데이터 |
| `/api/database/tables` | GET | Bearer | - | 테이블 목록 |
| `/api/database/tables/{tableName}/columns` | GET | Bearer | Path `tableName` | 컬럼 정의 |
| `/api/database/tables/{tableName}/sample` | GET | Bearer | Path `tableName` | 최대 10행 샘플 |
| `/api/database/query` | GET | Bearer | Query `sql=<SELECT ...>` | SELECT 한정, 결과 100행 제한 |
| `/admin/logs/monitor` | GET | permitAll | - | 로그 모니터링 HTML (SSE 클라이언트) |
| `/admin/logs/stream` | GET | permitAll | Query `logType?` (`app`,`auth`,`error`) | SSE 스트림 |
| `/admin/logs/status` | GET | permitAll | - | 연결/모니터링 현황 |
| `/admin/metrics/system` | GET | permitAll | - | JVM/시스템 메트릭 |
| `/admin/metrics/logs` | GET | permitAll | - | 로그 파일 크기/라인수 |

---

### 참고

- 대부분의 `/api/**` 엔드포인트는 Spring Security에서 기본적으로 JWT 인증을 요구하며, 위 표에 `permitAll`로 명시된 경로만 예외입니다.
- DTO 상세 필드는 `backend/BlueCrab/src/main/java/BlueCrab/com/example/dto`를 참고하세요.
- 테스트 코드 작성 시, 인증 필요한 엔드포인트는 `Authorization` 헤더(`Bearer ${accessToken}`) 또는 명시된 커스텀 헤더를 반드시 포함해야 합니다.
