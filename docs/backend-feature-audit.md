# Blue Crab LMS - 백엔드 구현 현황 종합 감사 보고서

**최종 업데이트:** 2025-10-22
**프로젝트:** Blue Crab LMS (Learning Management System + 실시간 텍스트 상담 시스템)
**백엔드:** Spring Boot 2.7 + JPA + MySQL + Redis + MinIO
**분석 범위:** 281개 Java 파일 (Entity 37개, Controller 52개, Service 31개, Repository 23개)

---

## 📊 전체 요약

### 구현 현황 한눈에 보기

| 도메인 | 구현률 | 상태 | 핵심 기능 | 비고 |
|--------|--------|------|----------|------|
| **사용자 관리** | 85% | 🟢 완료 | 로그인, JWT, 프로필, 비밀번호 재설정 | 회원가입 API 누락 |
| **강의 관리** | 90% | 🟢 완료 | 수강신청, 출석, 과제, 성적 | 강의 개설 API 누락 |
| **게시판** | 85% | 🟢 완료 | CRUD, 첨부파일(MinIO) | 댓글 기능 누락 |
| **시설/예약** | 90% | 🟢 완료 | 예약 생성/승인/통계 | 완전 구현 |
| **열람실** | 85% | 🟢 완료 | 좌석 예약/반납, 자동 알림 | 완전 구현 |
| **알림(FCM)** | 85% | 🟢 완료 | 푸시 알림, 토큰 관리 | DB 저장형 알림 누락 |
| **상담 요청** | 0% | 🔴 미구현 | - | 엔티티 없음 |
| **실시간 채팅** | 0% | 🔴 미구현 | - | WebSocket 미구성 |
| **상담방 관리** | 0% | 🔴 미구현 | - | 엔티티 없음 |
| **상담 이력** | 0% | 🔴 미구현 | - | 상담 시스템 의존 |
| **자동 종료** | 0% | 🔴 미구현 | - | 상담방 없음 |

**전체 평균: 52% (LMS 기능만 86%, 상담 시스템 0%)**

---

## 📋 개요

### 현재 상태
- **Spring Boot 2.7** 기반 REST API 백엔드
- **MySQL** 주 데이터베이스, **Redis** 캐시 및 세션 관리
- **MinIO** 객체 스토리지 (프로필 이미지, 게시글 첨부파일)
- **Firebase Cloud Messaging (FCM)** 푸시 알림
- **JWT** 토큰 기반 인증 (Access 15분, Refresh 24시간)

### 핵심 발견사항
1. ✅ **LMS 핵심 기능 완전 구현** - 강의/출석/과제/성적/게시판/시설예약
2. ❌ **상담 시스템 완전 미구현** - 엔티티, 컨트롤러, 서비스, WebSocket 모두 없음
3. ⚠️ **feature-list.md와 괴리** - 계획서의 상담 기능이 코드베이스에 반영되지 않음

## 기능 매트릭스
| 도메인 | 주요 기능 | 구현 증거 | 비고 |
| --- | --- | --- | --- |
| 인증 및 세션 | JWT 로그인, 갱신, 검증, 로그아웃; 관리자 토큰 갱신 | `AuthController`, `AdminAuthTokenController`, `TokenBlacklistService` | 액세스 토큰(15분) + 리프레시 토큰(24시간), 블랙리스트는 Oracle에 저장 |
| 관리자 로그인 강화 | 이메일 OTP를 사용한 2단계 관리자 로그인 | `AdminController`, `AdminEmailAuthController`, `AdminService` | `EmailVerificationService`를 통한 이메일 인증 |
| 계정 복구 | ID 찾기, 속도 제한이 있는 비밀번호 재설정 | `FindIdController`, `PasswordResetController`, `PasswordResetService` | Redis 기반 제한 및 이메일 코드 사용 |
| 사용자 관리 | CRUD, 역할 전환, 검색, 통계 | `UserController`, `UserTblService` | 인증된 JWT 필요, 학생/교수 구분 |
| 프로필 및 미디어 | 프로필 완성도, MinIO 기반 프로필 이미지, 캐싱 | `ProfileController`, `ImageCacheService`, `MinIOService` | 프록시 이미지 다운로드 및 사전 서명된 URL |
| 게시판 및 첨부파일 | 게시판 CRUD, 통계, 첨부파일 업로드/다운로드/삭제 | `BoardController`, `BoardCreateController`, `BoardAttachment*Controller`, `BoardService` | 첨부파일은 MinIO에 저장, 일괄 삭제 지원 |
| 시설 | 시설 목록, 가용성 확인, 일일 일정 | `FacilityController`, `FacilityService` | 유형 필터링, 시설별 일정, 가용성 시간대 검증 |
| 시설 예약 | 개인 예약 생성/취소/조회, 관리자 승인 및 통계 | `FacilityReservationController`, `AdminFacilityReservationController`, `FacilityReservationService` | 관리자 엔드포인트에서 대기 건수, 통계, 필터 노출 |
| 열람실 | 좌석 상태, 예약/퇴실, 내 예약 조회, 자동 알림 | `ReadingRoomController`, `ReadingRoomService`, `ReadingRoomPreExpiryNotifier`, `ReservationScheduler` | 좌석 정원 80석, 2시간 단위, 만료 15분 전 FCM 알림 |
| 알림 | FCM 토큰 등록, 푸시 전송, 일괄/브로드캐스트 | `FcmTokenController`, `FirebasePushService`, `PushNotificationController`, `FcmTokenService` | 충돌 감지, 토큰 통계, 웹 푸시(VAPID) 지원 |
| 학사(강의/수강신청/과제) | 강의, 수강신청, 과제에 대한 CRUD 및 통계, 성적 관리 | `LectureController`, `EnrollmentController`, `AssignmentController`, `service/Lecture/` 하위 서비스 | JSON 페이로드 파싱, 출석 및 성적 업데이트 |
| 학적 및 증명서 | 학적 조회, 증명서 발급 로그, 존재 여부 확인 | `RegistryController`, `RegistryService`, `CertIssueService` | IP 감사 추적과 함께 발급 기록 |
| 시스템 및 운영 | 헬스 체크, 메트릭, DB 탐색기, 로그 모니터 | `ApiController`, `MetricsController`, `DatabaseController`, `LogMonitorController` | 데이터베이스 탐색기는 SELECT 쿼리로 제한 |

## 도메인 세부사항
### 인증 및 관리자 보안
- JWT 유틸리티(`JwtUtil`)는 헤더에서 userId/userCode 추출, 토큰 해석 및 블랙리스트 확인을 제공합니다.
- 관리자 흐름은 자격 증명 검증(`/api/admin/login`)과 이메일 인증(`/api/admin/verify-email`, `/api/admin/email-auth/*`)으로 분리됩니다.

### 계정 복구 및 이메일
- `PasswordResetService`는 만료 시간이 있는 해시된 코드를 저장하고 Redis를 통해 사용자/이메일당 속도 제한을 적용합니다.
- `EmailService`는 구성된 SMTP와 통합되어 관리자 로그인 및 비밀번호 재설정 흐름에서 공유되는 인증 코드를 전송합니다.

### 사용자, 프로필 및 미디어
- `ProfileController`는 `/api/profile/me`, 완성도 점수, 프로필 이미지 조회/사전 서명 엔드포인트를 노출합니다.
- 프로필 이미지는 MinIO에 저장되며, `ImageCacheService`는 MinIO 왕복을 줄이기 위해 사전 서명된 URL을 캐시합니다.

### 게시판 모듈
- `BoardManagementService`는 트랜잭션 첨부파일 연결과 함께 게시판 생성/업데이트/삭제 작업을 조정합니다.
- `BoardAttachmentUploadService`는 multipart form data를 통한 다중 파일 업로드를 지원하고 메타데이터 레코드를 게시판 인덱스에 연결합니다.

### 시설 및 열람실
- 시설 가용성은 `FacilityReservationService` 내에서 업무 시간대와 충돌 감지를 사용합니다.
- 열람실 운영은 사용자당 단일 좌석을 강제하고 스케줄러를 사용하여 만료된 예약을 해제하고 만료 전 사용자에게 알립니다(`ReadingRoomPreExpiryNotifier`, `ReservationScheduler`).

### 알림
- FCM 등록은 디바이스당 하나의 활성 토큰을 보장하고 강제 재정의를 허용합니다.
- 푸시 엔드포인트는 데이터 전용 페이로드 및 프론트엔드를 위한 VAPID 공개 키 노출이 있는 웹 푸시를 지원합니다.
- 열람실 스케줄러는 `ReadingRoomNotificationFactory`를 재사용하여 현지화된 알림 페이로드를 포맷합니다.

### 학사 서비스
- 강의 엔드포인트는 일련번호, 교수, 년도/학기, 전공에 대한 필터링을 허용합니다. 페이지네이션은 Spring `Pageable`을 통해 처리됩니다.
- 수강신청 및 과제 서비스는 JSON 메타데이터 blob을 저장하고 파생 통계/출석 업데이트를 노출합니다.
- `GradeManagementService`는 성적 잠금을 포함한 성적 처리 워크플로우를 조정합니다(`GradeFinalizer`).

### 학적 및 증명서
- `RegistryService`는 학적 테이블을 쿼리하고 주소를 정규화합니다. 과거 스냅샷을 가져오기 위한 선택적 `asOf` 매개변수가 있습니다.
- `CertIssueService`는 증명서 발급 이벤트(발급 ID, 타임스탬프, IP, 템플릿 스냅샷)를 기록합니다.

### 시스템 지원
- `DatabaseController`는 디버깅을 위한 스키마, 열 및 샘플 데이터의 읽기 전용 뷰를 노출합니다.
- `MetricsController`는 JVM/DB 메트릭을 표시합니다. `ApiController`는 JVM 세부 정보와 함께 `/api/system-info`를 노출합니다.

## 상담 시스템 계획서와의 격차 분석
- **실시간 채팅 (1~4단계)**: WebSocket/STOMP 구성, 메시징 엔티티 또는 채팅 컨트롤러가 존재하지 않습니다. 프론트엔드에는 채팅 컴포넌트/API 호출이 없습니다. 기능이 구현되지 않은 상태입니다.
- **상담 요청/룸**: `consultation` 도메인 엔티티, 리포지토리 또는 엔드포인트가 없습니다. 상담과 연결된 스케줄러/알림 훅이 없습니다.
- **자동 상담 알람**: 열람실 자동 만료 알림만 존재합니다. 상담 비활성/24시간 타이머에 대한 알림은 없습니다.
- **상담 이력 및 검색**: 데이터베이스 스키마 및 서비스에 상담 로그나 검색 필터가 포함되어 있지 않습니다.
- **타이핑 표시기/읽음 확인**: 백엔드 또는 프론트엔드 코드베이스 어디에도 존재하지 않습니다.

## 제안된 문서화 작업
1. 현재 상담 시스템 기능 목록을 이 파일과 같은 기능 맵으로 교체한 다음, 상담 기능에 대한 별도의 백로그 문서를 유지합니다(미구현으로 표시).
2. API 참조(`docs/api`)를 위에 나열된 컨트롤러와 연결하도록 업데이트합니다. 변경사항이 적용되면 엔드포인트 카탈로그(`api-endpoints-documentation.json`)를 재생성합니다.
3. 각 도메인의 React 컴포넌트가 검증되면 프론트엔드 커버리지 노트를 추가합니다(예: 열람실 좌석 맵, 시설 예약).
4. 상담 기능 개발이 시작되면 백엔드에 새로운 도메인 모듈(`consultation`)을 생성하고 프론트엔드 연결 전에 `docs/api`에 스키마/API 계약을 기록합니다.
