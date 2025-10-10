# 시설물 예약 백엔드 구성 플랜

## 📋 분석 결과
기존 **열람실 예약 시스템**이 완벽한 예제로 존재하며, Spring Boot + JPA 기반으로 구현되어 있습니다.

## 🏗️ 아키텍처 패턮 (열람실 예제 기반)

```
Controller → Service → Repository → Entity
     ↓          ↓           ↓          ↓
   DTO ←─── Business ──→ JPA ───→ DB Table
```

## 📦 구현할 컴포넌트 (시설물 예약용)

### 1️⃣ **Entity** (DB 테이블 매핑)

#### `Facility.java` - 시설물 정보
```java
@Entity
@Table(name = "FACILITY_TBL")
public class Facility {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "FACILITY_IDX")
    private Integer facilityId;

    @Column(name = "FACILITY_NAME", nullable = false)
    private String facilityName;

    @Column(name = "FACILITY_TYPE", nullable = false)
    private String facilityType; // ROOM, EQUIPMENT, SPACE 등

    @Column(name = "FACILITY_DESC")
    private String description;

    @Column(name = "CAPACITY")
    private Integer capacity; // 수용 인원

    @Column(name = "LOCATION")
    private String location;

    @Column(name = "IS_ACTIVE")
    private Integer isActive = 1; // 0: 비활성, 1: 활성

    @Column(name = "CREATED_AT")
    private LocalDateTime createdAt;

    @Column(name = "UPDATED_AT")
    private LocalDateTime updatedAt;
}
```

#### `FacilityReservation.java` - 예약 정보
```java
@Entity
@Table(name = "FACILITY_RESERVATION_TBL")
public class FacilityReservation {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "RESERVATION_IDX")
    private Integer reservationId;

    @Column(name = "FACILITY_IDX", nullable = false)
    private Integer facilityId;

    @Column(name = "USER_CODE", nullable = false)
    private String userCode;

    @Column(name = "START_TIME", nullable = false)
    private LocalDateTime startTime;

    @Column(name = "END_TIME", nullable = false)
    private LocalDateTime endTime;

    @Column(name = "PARTY_SIZE")
    private Integer partySize; // 사용 인원

    @Column(name = "PURPOSE")
    private String purpose; // 사용 목적

    @Column(name = "STATUS", nullable = false)
    private String status; // PENDING, CONFIRMED, COMPLETED, CANCELLED, REJECTED

    @Column(name = "CREATED_AT")
    private LocalDateTime createdAt;

    @Column(name = "UPDATED_AT")
    private LocalDateTime updatedAt;
}
```

#### `FacilityUsageLog.java` - 사용 이력
```java
@Entity
@Table(name = "FACILITY_USAGE_LOG")
public class FacilityUsageLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "LOG_IDX")
    private Integer logId;

    @Column(name = "RESERVATION_IDX", nullable = false)
    private Integer reservationId;

    @Column(name = "FACILITY_IDX", nullable = false)
    private Integer facilityId;

    @Column(name = "USER_CODE", nullable = false)
    private String userCode;

    @Column(name = "ACTUAL_START_TIME")
    private LocalDateTime actualStartTime;

    @Column(name = "ACTUAL_END_TIME")
    private LocalDateTime actualEndTime;

    @Column(name = "ACTION_TYPE")
    private String actionType; // RESERVED, CONFIRMED, USED, CANCELLED

    @Column(name = "CREATED_AT")
    private LocalDateTime createdAt;
}
```

### 2️⃣ **Repository**

#### `FacilityRepository.java`
```java
@Repository
public interface FacilityRepository extends JpaRepository<Facility, Integer> {
    List<Facility> findByIsActive(Integer isActive);
    List<Facility> findByFacilityTypeAndIsActive(String facilityType, Integer isActive);
    List<Facility> findByFacilityNameContainingAndIsActive(String keyword, Integer isActive);
    Optional<Facility> findByFacilityIdAndIsActive(Integer facilityId, Integer isActive);
}
```

#### `FacilityReservationRepository.java`
```java
@Repository
public interface FacilityReservationRepository extends JpaRepository<FacilityReservation, Integer> {
    // 시간대 중복 체크
    @Query("SELECT fr FROM FacilityReservation fr WHERE fr.facilityId = :facilityId " +
           "AND fr.status IN ('PENDING', 'CONFIRMED') " +
           "AND ((fr.startTime < :endTime AND fr.endTime > :startTime))")
    List<FacilityReservation> findConflictingReservations(
        @Param("facilityId") Integer facilityId,
        @Param("startTime") LocalDateTime startTime,
        @Param("endTime") LocalDateTime endTime
    );

    // 사용자별 예약 목록
    List<FacilityReservation> findByUserCodeAndStatusInOrderByStartTimeDesc(
        String userCode, List<String> statuses
    );

    // 사용자의 활성 예약 개수
    long countByUserCodeAndStatusIn(String userCode, List<String> statuses);

    // 특정 시설물의 예약 목록
    List<FacilityReservation> findByFacilityIdAndStatusInOrderByStartTime(
        Integer facilityId, List<String> statuses
    );

    // 만료된 예약 조회
    @Query("SELECT fr FROM FacilityReservation fr WHERE fr.status = 'CONFIRMED' " +
           "AND fr.endTime < :currentTime")
    List<FacilityReservation> findExpiredReservations(@Param("currentTime") LocalDateTime currentTime);
}
```

#### `FacilityUsageLogRepository.java`
```java
@Repository
public interface FacilityUsageLogRepository extends JpaRepository<FacilityUsageLog, Integer> {
    List<FacilityUsageLog> findByReservationId(Integer reservationId);
    List<FacilityUsageLog> findByUserCodeOrderByCreatedAtDesc(String userCode);
    List<FacilityUsageLog> findByFacilityIdOrderByCreatedAtDesc(Integer facilityId);
}
```

### 3️⃣ **DTO** (Request/Response)

#### Request DTOs
```java
// FacilityListRequestDto.java
public class FacilityListRequestDto {
    private String facilityType; // optional: ROOM, EQUIPMENT, SPACE
}

// FacilityReservationRequestDto.java
public class FacilityReservationRequestDto {
    private Integer facilityId;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Integer partySize;
    private String purpose;
}

// ReservationCancelRequestDto.java
public class ReservationCancelRequestDto {
    private Integer reservationId;
}

// CheckAvailabilityRequestDto.java
public class CheckAvailabilityRequestDto {
    private Integer facilityId;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
}
```

#### Response DTOs
```java
// FacilityDto.java
public class FacilityDto {
    private Integer facilityId;
    private String facilityName;
    private String facilityType;
    private String description;
    private Integer capacity;
    private String location;
}

// FacilityListResponseDto.java
public class FacilityListResponseDto {
    private List<FacilityDto> facilities;
    private Integer totalCount;
}

// FacilityDetailDto.java
public class FacilityDetailDto extends FacilityDto {
    private List<TimeSlot> availableTimeSlots;
    private List<ReservationSummaryDto> upcomingReservations;
}

// FacilityReservationResponseDto.java
public class FacilityReservationResponseDto {
    private Integer reservationId;
    private Integer facilityId;
    private String facilityName;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private String status;
    private String message;
}

// MyReservationDto.java
public class MyReservationDto {
    private Integer reservationId;
    private FacilityDto facility;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Integer partySize;
    private String purpose;
    private String status;
    private LocalDateTime createdAt;
}

// MyReservationListDto.java
public class MyReservationListDto {
    private List<MyReservationDto> upcomingReservations;
    private List<MyReservationDto> pastReservations;
}

// AvailabilityCheckDto.java
public class AvailabilityCheckDto {
    private Boolean isAvailable;
    private String message;
    private List<TimeSlot> suggestedTimeSlots; // 대안 시간대
}
```

### 4️⃣ **Service**

#### `FacilityService.java`
```java
@Service
public class FacilityService {
    // 시설물 목록 조회 (타입별 필터 옵션)
    public FacilityListResponseDto getFacilities(String facilityType);

    // 시설물 상세 조회 (예약 가능 시간대 포함)
    public FacilityDetailDto getFacilityDetail(Integer facilityId, LocalDate date);

    // 시설물 등록 (관리자)
    public FacilityDto createFacility(FacilityDto facilityDto);

    // 시설물 수정 (관리자)
    public FacilityDto updateFacility(Integer facilityId, FacilityDto facilityDto);

    // 시설물 삭제/비활성화 (관리자)
    public void deactivateFacility(Integer facilityId);
}
```

#### `FacilityReservationService.java`
```java
@Service
public class FacilityReservationService {
    // 예약 가능 여부 확인
    public AvailabilityCheckDto checkAvailability(
        Integer facilityId, LocalDateTime startTime, LocalDateTime endTime
    );

    // 예약 생성
    public FacilityReservationResponseDto createReservation(
        FacilityReservationRequestDto request, String userCode
    );

    // 예약 취소
    public void cancelReservation(Integer reservationId, String userCode);

    // 내 예약 목록 조회
    public MyReservationListDto getMyReservations(String userCode);

    // 예약 승인 (관리자)
    public void confirmReservation(Integer reservationId);

    // 예약 거절 (관리자)
    public void rejectReservation(Integer reservationId, String reason);

    // 만료된 예약 자동 정리 (스케줄러)
    @Scheduled(cron = "0 */10 * * * *") // 10분마다
    public void cleanupExpiredReservations();
}
```

### 5️⃣ **Controller**

#### `FacilityController.java`
```java
@RestController
@RequestMapping("/api/facilities")
public class FacilityController {

    @PostMapping("/list")
    public ResponseEntity<ApiResponse<FacilityListResponseDto>> getFacilities(
        @RequestBody FacilityListRequestDto request
    );

    @PostMapping("/{id}/detail")
    public ResponseEntity<ApiResponse<FacilityDetailDto>> getFacilityDetail(
        @PathVariable Integer id,
        @RequestParam(required = false) LocalDate date
    );

    @PostMapping("/{id}/available-times")
    public ResponseEntity<ApiResponse<List<TimeSlot>>> getAvailableTimes(
        @PathVariable Integer id,
        @RequestParam LocalDate date
    );
}
```

#### `FacilityReservationController.java`
```java
@RestController
@RequestMapping("/api/facility-reservations")
public class FacilityReservationController {

    @RateLimit(timeWindow = 60, maxRequests = 5)
    @PostMapping("/check-availability")
    public ResponseEntity<ApiResponse<AvailabilityCheckDto>> checkAvailability(
        HttpServletRequest request,
        @Valid @RequestBody CheckAvailabilityRequestDto checkRequest
    );

    @RateLimit(timeWindow = 60, maxRequests = 3)
    @PostMapping("/reserve")
    public ResponseEntity<ApiResponse<FacilityReservationResponseDto>> createReservation(
        HttpServletRequest request,
        @Valid @RequestBody FacilityReservationRequestDto reservationRequest
    );

    @RateLimit(timeWindow = 30, maxRequests = 2)
    @PostMapping("/cancel")
    public ResponseEntity<ApiResponse<Void>> cancelReservation(
        HttpServletRequest request,
        @Valid @RequestBody ReservationCancelRequestDto cancelRequest
    );

    @RateLimit(timeWindow = 10, maxRequests = 10)
    @PostMapping("/my-reservations")
    public ResponseEntity<ApiResponse<MyReservationListDto>> getMyReservations(
        HttpServletRequest request
    );
}
```

### 6️⃣ **Config**

#### `FacilityConfig.java`
```java
@Configuration
public class FacilityConfig {
    // 최대 예약 가능 일수 (예: 7일 이내)
    public static final int MAX_RESERVATION_DAYS_AHEAD = 7;

    // 최소 예약 시간 (분)
    public static final int MIN_RESERVATION_MINUTES = 30;

    // 최대 예약 시간 (분)
    public static final int MAX_RESERVATION_MINUTES = 240; // 4시간

    // 1인당 최대 활성 예약 개수
    public static final int MAX_ACTIVE_RESERVATIONS_PER_USER = 3;

    // 예약 취소 가능 시간 (시작 시간 기준, 시간)
    public static final int CANCELLATION_DEADLINE_HOURS = 2;

    @Bean
    public CommandLineRunner initFacilityData(FacilityRepository facilityRepository) {
        return args -> {
            if (facilityRepository.count() == 0) {
                // 초기 시설물 데이터 생성
                List<Facility> facilities = Arrays.asList(
                    new Facility("세미나실 A", "ROOM", "중형 세미나실", 20, "본관 3층"),
                    new Facility("세미나실 B", "ROOM", "대형 세미나실", 50, "본관 4층"),
                    new Facility("스터디룸 1", "ROOM", "소형 스터디룸", 6, "도서관 2층"),
                    new Facility("스터디룸 2", "ROOM", "소형 스터디룸", 6, "도서관 2층"),
                    new Facility("빔 프로젝터", "EQUIPMENT", "고해상도 프로젝터", null, "교무실"),
                    new Facility("노트북", "EQUIPMENT", "업무용 노트북", null, "교무실")
                );
                facilityRepository.saveAll(facilities);
            }
        };
    }
}
```

## 🔑 주요 비즈니스 로직

### 1. 예약 가능 검증
```java
// FacilityReservationService.java
public AvailabilityCheckDto checkAvailability(
    Integer facilityId, LocalDateTime startTime, LocalDateTime endTime
) {
    // 1. 시설물 존재 및 활성화 확인
    Facility facility = facilityRepository.findByFacilityIdAndIsActive(facilityId, 1)
        .orElseThrow(() -> new IllegalArgumentException("존재하지 않거나 사용 불가능한 시설입니다."));

    // 2. 예약 시간 유효성 검증
    LocalDateTime now = LocalDateTime.now();
    LocalDateTime maxReservationDate = now.plusDays(MAX_RESERVATION_DAYS_AHEAD);

    if (startTime.isBefore(now)) {
        return new AvailabilityCheckDto(false, "과거 시간은 예약할 수 없습니다.", null);
    }

    if (startTime.isAfter(maxReservationDate)) {
        return new AvailabilityCheckDto(false,
            MAX_RESERVATION_DAYS_AHEAD + "일 이내만 예약 가능합니다.", null);
    }

    // 3. 예약 시간 길이 검증
    long durationMinutes = ChronoUnit.MINUTES.between(startTime, endTime);
    if (durationMinutes < MIN_RESERVATION_MINUTES) {
        return new AvailabilityCheckDto(false,
            "최소 " + MIN_RESERVATION_MINUTES + "분 이상 예약해야 합니다.", null);
    }
    if (durationMinutes > MAX_RESERVATION_MINUTES) {
        return new AvailabilityCheckDto(false,
            "최대 " + MAX_RESERVATION_MINUTES + "분까지 예약 가능합니다.", null);
    }

    // 4. 시간대 중복 확인
    List<FacilityReservation> conflicts =
        facilityReservationRepository.findConflictingReservations(
            facilityId, startTime, endTime
        );

    if (!conflicts.isEmpty()) {
        // 대안 시간대 제안
        List<TimeSlot> suggestedSlots = suggestAlternativeTimeSlots(
            facilityId, startTime.toLocalDate()
        );
        return new AvailabilityCheckDto(false,
            "해당 시간대에 이미 예약이 있습니다.", suggestedSlots);
    }

    return new AvailabilityCheckDto(true, "예약 가능합니다.", null);
}
```

### 2. 예약 생성
```java
@Transactional
public FacilityReservationResponseDto createReservation(
    FacilityReservationRequestDto request, String userCode
) {
    // 1. 사용자의 활성 예약 개수 확인
    long activeCount = facilityReservationRepository.countByUserCodeAndStatusIn(
        userCode, Arrays.asList("PENDING", "CONFIRMED")
    );

    if (activeCount >= MAX_ACTIVE_RESERVATIONS_PER_USER) {
        throw new IllegalStateException(
            "최대 " + MAX_ACTIVE_RESERVATIONS_PER_USER + "개까지 예약 가능합니다."
        );
    }

    // 2. 예약 가능 여부 확인
    AvailabilityCheckDto availability = checkAvailability(
        request.getFacilityId(), request.getStartTime(), request.getEndTime()
    );

    if (!availability.getIsAvailable()) {
        throw new IllegalStateException(availability.getMessage());
    }

    // 3. 시설물 정보 조회
    Facility facility = facilityRepository.findById(request.getFacilityId())
        .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 시설입니다."));

    // 4. 인원 수 검증 (수용 인원이 있는 경우)
    if (facility.getCapacity() != null &&
        request.getPartySize() > facility.getCapacity()) {
        throw new IllegalArgumentException(
            "최대 " + facility.getCapacity() + "명까지 수용 가능합니다."
        );
    }

    // 5. 예약 생성
    FacilityReservation reservation = new FacilityReservation();
    reservation.setFacilityId(request.getFacilityId());
    reservation.setUserCode(userCode);
    reservation.setStartTime(request.getStartTime());
    reservation.setEndTime(request.getEndTime());
    reservation.setPartySize(request.getPartySize());
    reservation.setPurpose(request.getPurpose());
    reservation.setStatus("CONFIRMED"); // 또는 "PENDING" (승인 필요 시)
    reservation.setCreatedAt(LocalDateTime.now());
    reservation.setUpdatedAt(LocalDateTime.now());

    FacilityReservation saved = facilityReservationRepository.save(reservation);

    // 6. 사용 이력 기록
    FacilityUsageLog log = new FacilityUsageLog();
    log.setReservationId(saved.getReservationId());
    log.setFacilityId(saved.getFacilityId());
    log.setUserCode(userCode);
    log.setActionType("RESERVED");
    log.setCreatedAt(LocalDateTime.now());
    facilityUsageLogRepository.save(log);

    return new FacilityReservationResponseDto(
        saved.getReservationId(),
        facility.getFacilityId(),
        facility.getFacilityName(),
        saved.getStartTime(),
        saved.getEndTime(),
        saved.getStatus(),
        "예약이 완료되었습니다."
    );
}
```

### 3. 예약 취소
```java
@Transactional
public void cancelReservation(Integer reservationId, String userCode) {
    // 1. 예약 조회 및 권한 확인
    FacilityReservation reservation = facilityReservationRepository
        .findById(reservationId)
        .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 예약입니다."));

    if (!reservation.getUserCode().equals(userCode)) {
        throw new IllegalStateException("본인의 예약만 취소할 수 있습니다.");
    }

    // 2. 취소 가능 상태 확인
    if (Arrays.asList("CANCELLED", "COMPLETED", "REJECTED")
            .contains(reservation.getStatus())) {
        throw new IllegalStateException("취소할 수 없는 예약입니다.");
    }

    // 3. 취소 가능 시간 확인
    LocalDateTime now = LocalDateTime.now();
    LocalDateTime cancellationDeadline = reservation.getStartTime()
        .minusHours(CANCELLATION_DEADLINE_HOURS);

    if (now.isAfter(cancellationDeadline)) {
        throw new IllegalStateException(
            "시작 " + CANCELLATION_DEADLINE_HOURS + "시간 전까지만 취소 가능합니다."
        );
    }

    // 4. 예약 취소 처리
    reservation.setStatus("CANCELLED");
    reservation.setUpdatedAt(now);
    facilityReservationRepository.save(reservation);

    // 5. 사용 이력 기록
    FacilityUsageLog log = new FacilityUsageLog();
    log.setReservationId(reservationId);
    log.setFacilityId(reservation.getFacilityId());
    log.setUserCode(userCode);
    log.setActionType("CANCELLED");
    log.setCreatedAt(now);
    facilityUsageLogRepository.save(log);
}
```

### 4. 자동 정리 스케줄러
```java
@Transactional
@Scheduled(cron = "0 */10 * * * *") // 10분마다 실행
public void cleanupExpiredReservations() {
    LocalDateTime now = LocalDateTime.now();

    // 만료된 예약 조회
    List<FacilityReservation> expiredReservations =
        facilityReservationRepository.findExpiredReservations(now);

    for (FacilityReservation reservation : expiredReservations) {
        reservation.setStatus("COMPLETED");
        reservation.setUpdatedAt(now);

        // 사용 이력 기록
        FacilityUsageLog log = new FacilityUsageLog();
        log.setReservationId(reservation.getReservationId());
        log.setFacilityId(reservation.getFacilityId());
        log.setUserCode(reservation.getUserCode());
        log.setActionType("AUTO_COMPLETED");
        log.setActualEndTime(reservation.getEndTime());
        log.setCreatedAt(now);
        facilityUsageLogRepository.save(log);
    }

    if (!expiredReservations.isEmpty()) {
        facilityReservationRepository.saveAll(expiredReservations);
        logger.info("만료된 예약 {}건 자동 정리 완료", expiredReservations.size());
    }
}
```

## 🎯 열람실 패턴과의 차이점

| 항목 | 열람실 예약 | 시설물 예약 |
|------|------------|------------|
| **선택 방식** | 좌석 번호 (1~80) | 시설물 ID (동적) |
| **예약 시간** | 고정 2시간 | 사용자 지정 (시작~종료) |
| **승인 프로세스** | 즉시 확정 | 즉시 확정 또는 관리자 승인 |
| **중복 체크** | 1인 1좌석 | 시간대별 중복 체크 |
| **추가 정보** | 없음 | 인원수, 사용 목적 |
| **예약 제한** | 현재 사용 중인 좌석 여부 | 최대 예약 일수, 개수 제한 |
| **취소 정책** | 언제든지 퇴실 가능 | 시작 시간 N시간 전까지 |
| **상태 관리** | 사용중/비어있음 (2가지) | PENDING/CONFIRMED/COMPLETED/CANCELLED/REJECTED (5가지) |

## 📝 API 엔드포인트 명세

### 시설물 관리 API

```http
POST /api/facilities/list
Content-Type: application/json
Authorization: Bearer <token>

Request:
{
    "facilityType": "ROOM"  // optional
}

Response:
{
    "success": true,
    "message": "시설물 목록을 조회했습니다.",
    "data": {
        "facilities": [
            {
                "facilityId": 1,
                "facilityName": "세미나실 A",
                "facilityType": "ROOM",
                "description": "중형 세미나실",
                "capacity": 20,
                "location": "본관 3층"
            }
        ],
        "totalCount": 1
    }
}
```

```http
POST /api/facilities/{id}/detail
Authorization: Bearer <token>

Response:
{
    "success": true,
    "message": "시설물 상세 정보를 조회했습니다.",
    "data": {
        "facilityId": 1,
        "facilityName": "세미나실 A",
        "facilityType": "ROOM",
        "description": "중형 세미나실",
        "capacity": 20,
        "location": "본관 3층",
        "availableTimeSlots": [
            {"startTime": "2025-10-06T10:00:00", "endTime": "2025-10-06T12:00:00"},
            {"startTime": "2025-10-06T14:00:00", "endTime": "2025-10-06T16:00:00"}
        ],
        "upcomingReservations": [
            {
                "reservationId": 5,
                "startTime": "2025-10-06T13:00:00",
                "endTime": "2025-10-06T14:00:00"
            }
        ]
    }
}
```

### 예약 API

```http
POST /api/facility-reservations/check-availability
Content-Type: application/json
Authorization: Bearer <token>

Request:
{
    "facilityId": 1,
    "startTime": "2025-10-06T14:00:00",
    "endTime": "2025-10-06T16:00:00"
}

Response:
{
    "success": true,
    "message": "예약 가능 여부를 확인했습니다.",
    "data": {
        "isAvailable": true,
        "message": "예약 가능합니다.",
        "suggestedTimeSlots": null
    }
}
```

```http
POST /api/facility-reservations/reserve
Content-Type: application/json
Authorization: Bearer <token>

Request:
{
    "facilityId": 1,
    "startTime": "2025-10-06T14:00:00",
    "endTime": "2025-10-06T16:00:00",
    "partySize": 10,
    "purpose": "학습 스터디 모임"
}

Response:
{
    "success": true,
    "message": "예약이 완료되었습니다.",
    "data": {
        "reservationId": 15,
        "facilityId": 1,
        "facilityName": "세미나실 A",
        "startTime": "2025-10-06T14:00:00",
        "endTime": "2025-10-06T16:00:00",
        "status": "CONFIRMED",
        "message": "예약이 완료되었습니다."
    }
}
```

```http
POST /api/facility-reservations/cancel
Content-Type: application/json
Authorization: Bearer <token>

Request:
{
    "reservationId": 15
}

Response:
{
    "success": true,
    "message": "예약이 취소되었습니다.",
    "data": null
}
```

```http
POST /api/facility-reservations/my-reservations
Authorization: Bearer <token>

Response:
{
    "success": true,
    "message": "내 예약 목록을 조회했습니다.",
    "data": {
        "upcomingReservations": [
            {
                "reservationId": 20,
                "facility": {
                    "facilityId": 1,
                    "facilityName": "세미나실 A",
                    "facilityType": "ROOM",
                    "capacity": 20,
                    "location": "본관 3층"
                },
                "startTime": "2025-10-07T10:00:00",
                "endTime": "2025-10-07T12:00:00",
                "partySize": 15,
                "purpose": "프로젝트 회의",
                "status": "CONFIRMED",
                "createdAt": "2025-10-05T15:30:00"
            }
        ],
        "pastReservations": []
    }
}
```

## 🔧 구현 순서

1. ✅ **Entity 클래스 생성** (DB 테이블 매핑)
   - Facility.java
   - FacilityReservation.java
   - FacilityUsageLog.java

2. ✅ **Repository 인터페이스 작성**
   - FacilityRepository.java
   - FacilityReservationRepository.java
   - FacilityUsageLogRepository.java

3. ✅ **DTO 클래스 작성**
   - Request DTOs (4개)
   - Response DTOs (7개)

4. ✅ **Service 로직 구현**
   - FacilityService.java
   - FacilityReservationService.java

5. ✅ **Controller 엔드포인트 작성**
   - FacilityController.java
   - FacilityReservationController.java

6. ✅ **Config 및 초기 데이터 설정**
   - FacilityConfig.java
   - 초기 시설물 데이터 생성

7. ✅ **테스트 및 검증**
   - API 테스트 (Postman/cURL)
   - 비즈니스 로직 검증
   - 예외 처리 확인

## 📊 데이터베이스 스키마

### FACILITY_TBL (시설물 테이블)
```sql
CREATE TABLE FACILITY_TBL (
    FACILITY_IDX INT PRIMARY KEY AUTO_INCREMENT,
    FACILITY_NAME VARCHAR(100) NOT NULL,
    FACILITY_TYPE VARCHAR(50) NOT NULL,
    FACILITY_DESC TEXT,
    CAPACITY INT,
    LOCATION VARCHAR(200),
    IS_ACTIVE INT DEFAULT 1,
    CREATED_AT DATETIME DEFAULT CURRENT_TIMESTAMP,
    UPDATED_AT DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);
```

### FACILITY_RESERVATION_TBL (예약 테이블)
```sql
CREATE TABLE FACILITY_RESERVATION_TBL (
    RESERVATION_IDX INT PRIMARY KEY AUTO_INCREMENT,
    FACILITY_IDX INT NOT NULL,
    USER_CODE VARCHAR(255) NOT NULL,
    START_TIME DATETIME NOT NULL,
    END_TIME DATETIME NOT NULL,
    PARTY_SIZE INT,
    PURPOSE TEXT,
    STATUS VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    CREATED_AT DATETIME DEFAULT CURRENT_TIMESTAMP,
    UPDATED_AT DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (FACILITY_IDX) REFERENCES FACILITY_TBL(FACILITY_IDX),
    FOREIGN KEY (USER_CODE) REFERENCES USER_TBL(USER_CODE),
    INDEX idx_facility_time (FACILITY_IDX, START_TIME, END_TIME),
    INDEX idx_user_status (USER_CODE, STATUS),
    INDEX idx_status_time (STATUS, END_TIME)
);
```

### FACILITY_USAGE_LOG (사용 이력 테이블)
```sql
CREATE TABLE FACILITY_USAGE_LOG (
    LOG_IDX INT PRIMARY KEY AUTO_INCREMENT,
    RESERVATION_IDX INT NOT NULL,
    FACILITY_IDX INT NOT NULL,
    USER_CODE VARCHAR(255) NOT NULL,
    ACTUAL_START_TIME DATETIME,
    ACTUAL_END_TIME DATETIME,
    ACTION_TYPE VARCHAR(20) NOT NULL,
    CREATED_AT DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (RESERVATION_IDX) REFERENCES FACILITY_RESERVATION_TBL(RESERVATION_IDX),
    INDEX idx_reservation (RESERVATION_IDX),
    INDEX idx_user (USER_CODE),
    INDEX idx_facility (FACILITY_IDX)
);
```

## 🚀 다음 단계

1. **프론트엔드 연동**
   - React 컴포넌트 개발
   - API 클라이언트 작성 (facilityApi.js)
   - UI/UX 구현

2. **관리자 기능 추가**
   - 예약 승인/거절
   - 시설물 관리 (CRUD)
   - 통계 대시보드

3. **알림 기능**
   - 예약 확정 알림
   - 예약 시작 리마인더
   - 예약 취소 알림

4. **고급 기능**
   - 예약 대기열 (예약 불가 시 대기)
   - 반복 예약 (매주 같은 시간)
   - 예약 승인 워크플로우

---

**작성일**: 2025-10-06
**버전**: 1.0.0
**참조**: ReadingRoom 예약 시스템 (backend/BlueCrab/src/main/java/.../ReadingRoom*)
