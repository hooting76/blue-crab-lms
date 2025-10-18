# 시설 예약 기능

## 개요
시설 예약 기능은 Spring Boot 백엔드의 `backend/BlueCrab/src/main/java/BlueCrab/com/example` 경로에 구현되어 있으며, 다음 기능을 지원합니다:
- 사용자 예약 생성, 조회 및 취소 (`FacilityReservationController`)
- 시설 목록 조회 및 가용성 확인 (`FacilityController`)
- 관리자 승인, 반려 및 통계 조회 (`AdminFacilityReservationController`)
- 종료된 예약의 주기적 자동 완료 처리 (`ReservationScheduler`)

대부분의 쓰기 작업은 통일된 `ApiResponse<T>` 형식으로 응답을 반환합니다. 인증은 `JwtUtil`의 JWT 파싱을 통해 이루어지며, 요청에서 `userCode` 또는 `adminId`를 추출합니다.

## 핵심 데이터 모델
- `FacilityTbl` (`entity/FacilityTbl.java`): 예약 가능한 시설의 마스터 데이터 (`isActive`, `capacity` 포함)
- `FacilityPolicyTbl` (`entity/FacilityPolicyTbl.java`): 시설별 정책 설정 (승인 필요 여부, 예약 시간 제한, 주말 예약 허용 등). 현재 코드에서 `requiresApproval`, `minDaysInAdvance`, `maxDaysInAdvance`, `minDurationMinutes`, `maxDurationMinutes`가 적용됨.
- `FacilityBlockTbl` (`entity/FacilityBlockTbl.java`): 예약 불가 기간 설정
- `FacilityReservationTbl` (`entity/FacilityReservationTbl.java`): 예약 기록 (상태, 시간, 인원, 승인 정보 포함)
- `FacilityReservationLog` (`entity/FacilityReservationLog.java`): 예약 생애주기 이벤트 감사 로그 (생성, 승인, 반려, 취소, 자동완료)

API 경계에서 사용되는 DTO는 `dto/` 디렉토리에 위치합니다 (`ReservationCreateRequestDto`, `ReservationDto`, `FacilityDto`, `FacilityAvailabilityDto`, `AdminApproveRequestDto`, `AdminRejectRequestDto` 등).

## 사용자 API (`FacilityReservationController`, `FacilityController`)
모든 엔드포인트는 `POST` (또는 취소의 경우 `DELETE`)로 정의되며 JSON 형식으로 요청/응답합니다:

| 목적 | 메서드 & 경로 | 비고 |
| --- | --- | --- |
| 예약 생성 | `POST /api/reservations` | 바디: `ReservationCreateRequestDto`. 호출자의 `userCode` 사용. 유효성 검증, 승인 정책 적용, 생성 로그 기록 (참조: `FacilityReservationService.createReservation`). |
| 내 예약 목록 조회 | `POST /api/reservations/my` | `List<ReservationDto>` 반환, `createdAt` 내림차순 정렬. |
| 상태별 예약 목록 조회 | `POST /api/reservations/my/status/{status}` | `status`는 enum 이름 또는 한글 라벨 허용 (`ReservationStatus.fromString`으로 처리). |
| 예약 상세 조회 | `POST /api/reservations/{reservationIdx}` | 타인의 예약 접근 차단. |
| 예약 취소 | `DELETE /api/reservations/{reservationIdx}` | `PENDING` 또는 `APPROVED` 상태에서만 취소 가능. `CANCELLED` 로그 기록. |
| 활성 시설 목록 조회 | `POST /api/facilities` | 정책 및 차단 정보를 포함한 `List<FacilityDto>` 반환. |
| 타입별 시설 필터링 | `POST /api/facilities/type/{facilityType}` | `facilityType`은 `FacilityType.fromString`으로 매핑. |
| 시설 상세 조회 | `POST /api/facilities/{facilityIdx}` | ID로 조회. |
| 시설 검색 | `POST /api/facilities/search?keyword=` | 시설명/위치 대소문자 구분 없이 검색. |
| 가용성 확인 | `POST /api/facilities/{facilityIdx}/availability` | `startTime`, `endTime` 쿼리 파라미터 필요 (`yyyy-MM-dd HH:mm:ss` 형식). 글로벌 정책 및 차단 기간 검증 후 가용성 정보 반환. |

## 관리자 API (`AdminFacilityReservationController`)

| 목적 | 메서드 & 경로 | 비고 |
| --- | --- | --- |
| 승인 대기 예약 조회 | `POST /api/admin/reservations/pending` | JWT에서 관리자 정보 사용. |
| 승인 대기 건수 조회 | `POST /api/admin/reservations/pending/count` | 숫자형 건수 반환. |
| 예약 승인 | `POST /api/admin/reservations/approve` | 바디: `AdminApproveRequestDto`. 비관적 잠금 및 충돌 검사 적용 (하단 참조). |
| 예약 반려 | `POST /api/admin/reservations/reject` | 바디: `AdminRejectRequestDto` (`rejectionReason` 필수). |
| 대시보드 통계 | `POST /api/admin/reservations/stats` | 선택적 `startDate`/`endDate`. `FacilityReservationRepository`의 네이티브 집계 쿼리 사용. |
| 필터링된 목록 조회 | `POST /api/admin/reservations/all` | 선택적 필터: `status`, `facilityIdx`, `startDate`, `endDate`. 애플리케이션 레벨에서 날짜 필터링 추가 적용. |

## 예약 생애주기 및 동시성 제어
- 예약 생성 시 `FacilityReservationService.validateReservationRequest`를 통해 시간적 제약 검증 후 `PENDING` (승인 필요) 또는 `APPROVED` (자동 승인) 상태로 저장.
- 자동 승인의 경우 (`requiresApproval = false`), `FacilityRepository.findByIdWithLock`을 통해 트랜잭션이 끝날 때까지 비관적 쓰기 잠금 획득. `PENDING` 및 `APPROVED` 예약에 대해 충돌 검사 수행.
- 승인 프로세스 (`AdminFacilityReservationService.approveReservation`)는 비관적 잠금을 다시 획득한 후, `checkAvailabilityWithExclusion`을 호출하여 겹치는 예약이 없음을 확인한 뒤 `APPROVED`로 전환.
- 취소는 `PENDING` 또는 `APPROVED` 상태에서 가능하며, `CANCELLED`로 전환 후 로그 기록. 취소 마감 시간 제약은 아직 미적용.
- `ReservationScheduler.completeExpiredReservations`는 매시간 (`@Scheduled cron 0 0 * * * *`) 실행되어, `endTime`이 `autoCompleteHours` (기본 1시간) 이상 경과한 `APPROVED` 예약을 자동 완료 처리.

### 상태 코드 (`ReservationStatus`)
`PENDING`, `APPROVED`, `REJECTED`, `CANCELLED`, `COMPLETED` 상태 존재. 한글 설명 포함. 저장 시 enum 이름 사용, DTO는 한글 설명을 직렬화 (`@JsonValue`).

## 검증 및 비즈니스 규칙
- 글로벌 정책 기본값은 `reservation.policy.*` 설정 (`ReservationPolicyProperties`)에서 가져옴: `maxDaysInAdvance` (30), `minDurationMinutes` (30), `maxDurationMinutes` (480), `autoCompleteHours` (1).
- `ReservationCreateRequestDto`는 필수 필드와 `@Future` 타임스탬프 검증.
- `FacilityReservationService.validateReservationRequest`는 시작 시간 ≥ 현재, 종료 > 시작, 당일 예약만 허용, 글로벌 최소/최대 예약 시간 준수 확인.
- `FacilityReservationService.validateReservationPolicy`는 시설별 정책을 적용하여 `minDaysInAdvance`, `maxDaysInAdvance`, `minDurationMinutes`, `maxDurationMinutes` 검증.
- 차단 기간 검증은 두 곳에서 수행: 승인 필요 시설의 예약 생성 시 (`validateNotBlocked`) 및 가용성 체크 내부 (`checkAvailabilityInternal`).
- 시설 활성 상태는 예약 생성/승인 전에 확인.
- `FacilityService`는 `requiresApproval` (미설정 시 기본값 true)과 현재 차단 정보를 포함하여 `FacilityDto`를 구성.

## 로깅
`FacilityReservationLog` 엔트리는 사용자 액션 (`CREATED`, `AUTO_APPROVED`, `CANCELLED`) 및 관리자/시스템 액션 (`APPROVED`, `REJECTED`, `AUTO_COMPLETED`)에 대해 기록됨. 페이로드는 `ObjectMapper`를 통해 JSON으로 직렬화.

## 프론트엔드 진입점
`component/common/Facilities/FacilityRequest.jsx`는 현재 예약 폼 쉘 (시간 선택, 사유 선택)을 렌더링하지만, 제출 시 알림만 표시. 백엔드 연동이 아직 구현되지 않았으며, API 바인딩 및 에러 처리 구현 필요.

## 알려진 제약사항 및 후속 작업
1. **미사용 시설별 정책값 일부** – `maxReservationsPerUser`와 주말 예약 허용(`allowWeekendBooking`) 필드는 `FacilityPolicyTbl`에 존재하지만 실제 검증 로직에서 참조되지 않음. 사용자는 무제한 예약 생성 및 주말 예약 가능. (`getEffectiveMaxReservationsPerUser()`, `isEffectiveAllowWeekendBooking()` 미사용)
2. **취소 마감 시간 미적용** – `FacilityPolicyTbl.getEffectiveCancellationDeadlineHours()`는 존재하지만 `FacilityReservationService.cancelReservation`에서 강제하지 않음. 사용자는 시작 시간 직전까지 취소 가능.
3. **정원/인원수 미검증** – `ReservationCreateRequestDto`는 `partySize`가 양수이며 `FacilityTbl.capacity` 이내인지 검증하지 않음. 정원 초과 예약 가능.
4. **승인 충돌 검사에 대기 중인 예약 포함** – `checkAvailabilityInternal`은 `PENDING` 및 `APPROVED` 예약 모두를 충돌로 간주. 여러 대기 예약이 겹치는 경우, 관리자는 하나를 승인하기 전에 다른 것들을 반려해야 함 (`FacilityReservationService.checkAvailabilityWithExclusion` 220–227행 참조). 승인된 예약만 검사하거나 충돌 해결 도구 제공 고려 필요.
5. **READ 작업에 POST 사용** – 목록/상세 조회 액션이 GET이 아닌 POST로 노출되어 있어, 캐싱 또는 REST 도구 활용에 제약 있음.

위 사항들을 해결하면 런타임 동작이 데이터 모델 및 원래 정책 의도와 일치하게 됩니다.
