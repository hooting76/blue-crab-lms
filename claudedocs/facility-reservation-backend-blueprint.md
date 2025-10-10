# 시설물 예약 백엔드 구현 청사진

## 1. 개요
- **프론트 연동 범위**: `FacilityBookingSystem`(학생)과 `AdminBookingSystem`(관리자) 컴포넌트가 요구하는 모든 데이터/액션을 API로 제공.
- **기반 아키텍처**: 기존 열람실(`ReadingRoom`) 모듈과 동일하게 `Controller → Service → Repository → Entity → DB` + `DTO` 계층 구조, 공통 `ApiResponse<T>` 응답, JWT 인증, `@RateLimit` 어노테이션 유지.
- **목표**: 시설 목록 조회, 시간대 기반 예약 신청, 내 예약 관리, 관리자 승인/반려, 통계 집계, 예약 로그 추적을 완결형 모듈로 제공.

## 2. 기능 요구사항 분해
| 구분 | 상세 요구 | 대응 API |
|------|-----------|----------|
| 시설 탐색 | 카테고리별 필터, 시설 상세(위치/수용인원/장비) | `POST /api/facilities/list`, `POST /api/facilities/{id}/detail` |
| 예약 가능 시간대 | 일자 선택 후 1시간 단위 슬롯 제공, 예약/승인/대기 상태 표시 | `POST /api/facilities/{id}/availability` |
| 예약 생성 | 목적, 인원, 장비 요청, 시간 슬롯 다중 선택 | `POST /api/facility-reservations/request` |
| 예약 검증 | 중복 시간대, 일일/주간 제한, 최대 예약일 | Service 레벨 검증 + `POST /.../check` |
| 내 예약 현황 | 진행중/완료 탭, 상태 필터, 세부 정보 토글 | `POST /api/facility-reservations/my` |
| 예약 취소 | 본인 예약만 취소, 시작 N시간 전 제한 | `POST /api/facility-reservations/cancel` |
| 관리자 승인 | 승인 대기 목록, 승인/반려 사유, 메모 저장 | `POST /api/admin/facility-reservations/pending`, `.../approve`, `.../reject` |
| 관리자 통계 | 승인 대기 수, 오늘/주/월 집계, 필터링 테이블 | `POST /api/admin/facility-reservations/stats`, `.../list` |
| 로그/이력 | 예약 진행 단계 기록, 감사 추적 | `FacilityReservationLogRepository` |

## 3. 데이터 모델 설계 (JPA Entity)
> 네이밍은 기존 `*_Tbl` 패턴을 따르되 도메인 가독성을 위해 클래스명은 CamelCase.

### 3.1 `FacilityTbl`
| 필드 | 타입 | 설명 |
|------|------|------|
| `facilityIdx` (PK) | `@Id Integer` | AUTO_INCREMENT |
| `facilityCode` | `String` | 검색용 코드 (optional) |
| `facilityName` | `String` | 시설명 |
| `facilityType` | `String` | `STUDY`, `SEMINAR`, `AUDITORIUM`, `GYM` |
| `capacity` | `Integer` | 수용 인원 |
| `location` | `String` | 위치 |
| `defaultEquipment` | `String` | 기본 제공 장비 (CSV) |
| `isActive` | `Integer` | 1: 사용, 0: 비활성 |
| `createdAt/updatedAt` | `LocalDateTime` | 자동 타임스탬프 |

### 3.2 `FacilityReservationTbl`
| 필드 | 타입 | 설명 |
|------|------|------|
| `reservationIdx` (PK) | Integer |
| `facilityIdx` | Integer | FK to Facility |
| `userIdx` | Integer | FK to `UserTbl` |
| `userCode` | String | 조회 편의 |
| `startTime/endTime` | LocalDateTime |
| `partySize` | Integer |
| `purpose` | String |
| `requestedEquipment` | String |
| `status` | Enum(String) | `PENDING`, `APPROVED`, `REJECTED`, `CANCELLED`, `COMPLETED` |
| `adminNote` | String | 승인 메모 |
| `rejectionReason` | String |
| `approvedBy` | Integer | FK to `AdminTbl` |
| `approvedAt/processedAt` | LocalDateTime |
| `createdAt/updatedAt` | LocalDateTime |

### 3.3 `FacilityReservationLog`
> 예약 상태 전이 검증 및 히스토리용 (선택 사항이지만 관리자 UI와 감사대비 강력 추천).

| 필드 | 설명 |
|------|------|
| `logIdx` PK | |
| `reservationIdx` | FK |
| `eventType` | `REQUESTED`, `APPROVED`, `REJECTED`, `CANCELLED`, `USAGE_RECORDED`, ... |
| `payload` | JSON/BLOB (사유, 변경값) |
| `actorType` | `USER`, `ADMIN`, `SYSTEM` |
| `actorCode` | userCode/adminId |
| `createdAt` | timestamp |

### 3.4 파생 뷰/통계 테이블 (옵션)
- `FacilityUsageDailySummary` (스케줄러가 집계) → 관리자 통계 API 캐싱.

## 4. Repository 계층 계약
- `FacilityRepository extends JpaRepository<FacilityTbl, Integer>`
  - `findByFacilityTypeAndIsActive(...)`
  - `findByFacilityNameContainingIgnoreCaseAndIsActive(...)`
- `FacilityReservationRepository extends JpaRepository<FacilityReservationTbl, Integer>`
  - `findConflicts(facilityIdx, startTime, endTime, status IN (PENDING, APPROVED))`
  - `findByUserCodeAndStatusInOrderByStartTimeDesc(...)`
  - `countByUserCodeAndStatusIn(...)` (예약 개수 제한)
  - `findByFacilityIdxAndDateRange(...)`
  - `findPendingReservations()`
  - `findDashboardStats(startOfDay, endOfDay, startOfWeek, startOfMonth)` (JPQL custom)
- `FacilityReservationLogRepository`
  - 상태 전이 이력 저장, 최신 로그 조회.

## 5. DTO & Mapper 설계
- **요청 DTO**
  - `FacilityListRequestDto(facilityType, keyword)`
  - `FacilityAvailabilityRequestDto(reserveDate)` → 하루 단위 슬롯 반환
  - `FacilityReservationRequestDto(facilityId, date, timeSlots[], purpose, partySize, equipment)`
  - `ReservationCancelRequestDto(reservationId)`
  - `AdminReservationDecisionRequestDto(reservationId, note, rejectionReason)`
  - `AdminReservationFilterRequestDto(status, facilityType, keyword, dateRange)`

- **응답 DTO**
  - `FacilitySummaryDto(id, name, type, capacity, location, highlight)`
  - `FacilityDetailDto(summary + defaultEquipment, description, upcomingReservations[])`
  - `TimeSlotStatusDto(time, status)` for availability grid → `status` aligns with `available/reserved/pending`
  - `ReservationSummaryDto(reservationId, facilityName, date, timeRange, status, purpose, participants, equipment, adminNote, rejectionReason)`
  - `MyReservationListDto(ongoing[], completed[])`
  - `AdminReservationListDto(pendingCount, reservations[], filters)`
  - `AdminReservationStatsDto(pending, todayTotal, todayInUse, weekTotal, monthTotal)`

> DTO 변환은 `MapStruct` 대신 Service 내 수동 변환 (현재 코드베이스도 수동 매핑).

## 6. 서비스 계층 시나리오

### 6.1 `FacilityService`
- `getFacilities(type, keyword)` → 활성 시설 필터링 + DTO 변환.
- `getFacilityDetail(facilityId, targetDate)` → 기본 정보 + `FacilityReservationRepository`에서 해당 날짜의 예약 상태 조회.
- `getAvailability(facilityId, date)`
  - 시간 슬롯 템플릿 생성 (`09:00~21:00` 등 Config 기반)
  - 예약/승인 상태와 매칭해 `available/reserved/pending` 결정.
- **관리자 전용**
  - `createFacility`, `updateFacility`, `toggleActive` (추후 확장 여지).

### 6.2 `FacilityReservationService`
1. **checkAvailability**
   - 시설 활성 여부 확인
   - 예약 가능 기간 검증 (예: 14일 이내)
   - 슬롯 to `LocalDateTime` 변환 → 기존 예약과 중복 여부 조회
   - 사용자 예약 제한 검증 (`countByUserCodeAndStatusIn`)

2. **createReservation(request, userCode)**
   - 위 검증 통과 후 `status = PENDING` 기본 값 (관리자 승인 필요)
   - 슬롯 배열 → 다중 예약 저장 시 `ReservationBundle`(same reservationId with start & end aggregated) 혹은 1건으로 start~end 계산
   - 로그 적재 (`eventType=REQUESTED`)
   - 응답 DTO 반환

3. **cancelReservation(reservationId, userCode)**
   - 본인 소유 확인, 상태 검사 (`APPROVED`일 경우 취소 제한 시간 체크)
   - 상태 `CANCELLED`, 로그 기록

4. **getMyReservations(userCode)**
   - 진행중(`PENDING`, `APPROVED`)과 완료(`COMPLETED`, `REJECTED`, `CANCELLED`) 분리

5. **completeExpiredReservations()** (스케줄러)
   - `endTime < now` & status `APPROVED` → `COMPLETED`
   - 사용 이력 기록 (`eventType=SYSTEM_COMPLETED`)

### 6.3 `AdminFacilityReservationService`
- `getPendingReservations(filter)` → 대기 목록 + 사용자 정보(`UserTbl`) 조인
- `approveReservation(reservationId, adminCode, note)`
  - 상태 전이 `PENDING → APPROVED`, `approvedBy`, `approvedAt` 기록
  - 로그 기록, 사용자 Push/Email 연동 (기존 FCM 활용 가능)
- `rejectReservation(reservationId, adminCode, reason)`
  - 상태 전이 `PENDING → REJECTED`
- `listAllReservations(filter)` → 페이징 & 정렬 (날짜 역순)
- `getDashboardStats()` → 오늘 예약/진행중, 주/월 합계, pending 카운트

## 7. 컨트롤러 설계

### 7.1 사용자용 `FacilityReservationController`
- Base Path: `/api/facility-reservations`
- 공통: `Authorization: Bearer <JWT>` 필수, 응답은 `ApiResponse<T>`
- 엔드포인트
  - `POST /check` → `AvailabilityCheckDto`
  - `POST /request` → 예약 생성
  - `POST /cancel` → 예약 취소
  - `POST /my` → 내 예약 리스트
  - `POST /history` (옵션, 과거 기록 페이징)
- 레이트리밋: `check/request/cancel`에 기존 패턴 적용 (`@RateLimit`)

### 7.2 시설 정보용 `FacilityController`
- Base Path: `/api/facilities`
- 엔드포인트
  - `POST /list`
  - `POST /{id}/detail`
  - `POST /{id}/availability`
- 비로그인 열람 허용 여부는 요구사항에 따라 결정 (기본은 로그인 필요→JWT 검증 후 시설 활용 로그 남김).

### 7.3 관리자용 `AdminFacilityReservationController`
- Base Path: `/api/admin/facility-reservations`
- 인증: JWT + `AdminInterceptor` (기존 `AdminController` 참고) → `AdminTbl` 검증
- 엔드포인트
  - `POST /pending` → 대기 목록 + 필터
  - `POST /list` → 전체 예약 (필터 + 페이징)
  - `POST /stats` → 대시보드 카드 데이터
  - `POST /{id}/approve`
  - `POST /{id}/reject`
  - `POST /{id}/note` (선택: 메모 업데이트)

## 8. 설정 & 유틸리티
- `FacilityConfig implements ApplicationRunner`
  - 초기 시설 Seed (예: front 샘플과 일치하는 10개 시설)
  - 예약 제한 정책 값 주입 (최대 예약일수, 최소 사용 시간, 최대 인원 등)
- `ReservationPolicyProperties`
  - `maxAdvanceDays`, `minDurationMinutes`, `maxDurationMinutes`, `maxConcurrentReservationsPerUser`
- `TimeSlotGenerator`
  - Config 기반으로 하루 슬롯 리스트 생성 (예약/availability 공통 활용)

## 9. 스케줄러 & 배치
- `@Scheduled(cron = "0 */10 * * * *")` → `completeExpiredReservations()`
- `@Scheduled(cron = "0 0 2 * * *")` → 일자별 통계 집계 및 `FacilityUsageDailySummary` 업데이트 (관리자 카드에 캐시 활용)
- 스케줄러 동작 시 로그 기록 + `FacilityReservationLog` 추가 (`SYSTEM` actor)

## 10. 인증/인가 전략
- **JWT 검증**: `JwtUtil` 활용, 사용자 ID → `UserTbl` 조회, `userCode` 확보 (기존 패턴 재사용).
- **Admin 검증**: `AdminStep` 인증 플로우 이후 발급된 토큰 사용. `AdminInterceptor` 또는 새로운 `@AdminOnly` 어노테이션 도입.
- **Rate Limit**: `check/request/cancel`, 관리자 승인 API에도 적용(남용 방지).
- **데이터 접근**: `User`는 본인 예약만 접근, `Admin`은 전체 조회. Service 단에서 이중 검증.

## 11. 예외 및 감사 로깅
- 커스텀 예외 `ReservationConflictException`, `ReservationPolicyViolationException` 추가 (GlobalExceptionHandler 매핑).
- 상태 전이 시 `FacilityReservationLog` 기록 + `Logger` INFO 레벨 남김.
- 관리자 액션 (approve/reject) 시 `SecurityContext` 기반 Actor 식별.

## 12. 테스트 전략
- **단위 테스트**: `FacilityReservationServiceTest`
  - 예약 중복, 정책 위반, 정상 케이스 검증 (Mockito로 Repository mock)
- **통합 테스트**: `@SpringBootTest`
  - `MockMvc`로 JWT 포함한 예약 생성/승인 시나리오
- **데이터 시나리오**: `data.sql` 혹은 `Testcontainers`로 시설/예약 seed
- **API 문서화**: `springdoc-openapi` 또는 기존 문서 포맷(`claudedocs/api-endpoints`) 업데이트

## 13. 구현 단계 체크리스트
1. **Entity & Repository 추가** (`entity`, `repository` 패키지)
2. **DTO 정의** (`dto/facility/...` 하위 폴더 생성)
3. **Service 구현** (`FacilityService`, `FacilityReservationService`, `AdminFacilityReservationService`)
4. **Controller 작성** (사용자/관리자 분리)
5. **Config & Properties** (정책 값, 초기 데이터)
6. **Scheduler 등록** (예약 만료 처리, 통계 집계)
7. **로그/예외 처리** (공통 예외, 로깅 전략 반영)
8. **테스트 케이스 작성 & 실행**
9. **프론트 연동 검증** (예시 컴포넌트 기준 API payload 매칭)
10. **문서 업데이트** (`docs/`, `claudedocs/api-endpoints`)

## 14. 향후 확장 아이디어
- 대기열(Waitlist) 테이블 도입 및 자동 승격 로직
- FCM Push 연동 (`fcm` 디렉터리 참고) → 승인/반려 알림 발송
- 관리자 대시보드 캐시 → Redis 도입 (이미 FCM Redis 흐름 분석 자료 존재)
- 다국어 대응을 위한 메시지 번들 분리 (`message_ko.properties` 등)
- BI 레포트용 별도 통계 API (일/주/월 트렌드)

---
**참고 소스**: `ReadingRoomService`, `ReadingRoomController`, `ReadingSeat`, `ReadingUsageLog` 등 기존 열람실 모듈을 패턴 레퍼런스로 삼아 동일한 레벨의 가독성/주석 스타일 유지.
