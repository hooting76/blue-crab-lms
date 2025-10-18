# 시설 예약 시스템 - 최종 구현 플랜 (v2.0)

**작성일**: 2025-10-06
**상태**: Ready for Implementation
**참조**:
- 기존 플랜: facility-reservation-plan.md, facility-reservation-backend-blueprint.md
- 문제점 분석: facility-reservation-issues-analysis.md
- 기반 코드: ReadingRoomService, ReadingRoomController

---

## 📋 목차
1. [개요](#개요)
2. [데이터 계약 표준](#데이터-계약-표준)
3. [엔티티 설계](#엔티티-설계)
4. [Repository 계층](#repository-계층)
5. [DTO 설계](#dto-설계)
6. [서비스 로직](#서비스-로직)
7. [Controller API](#controller-api)
8. [설정 및 정책](#설정-및-정책)
9. [구현 체크리스트](#구현-체크리스트)

---

## 개요

### 프론트엔드 연동 범위
- **학생용**: `FacilityBookingSystem` 컴포넌트
  - 시설 목록 조회, 필터링
  - 시간대 기반 예약 신청
  - 내 예약 관리 (취소, 상태 확인)

- **관리자용**: `AdminBookingSystem` 컴포넌트
  - 예약 승인/반려
  - 통계 대시보드
  - 전체 예약 관리

### 기술 스택
- **Backend**: Spring Boot 2.x, JPA/Hibernate, MySQL
- **인증**: JWT (기존 AuthController 패턴)
- **Rate Limit**: `@RateLimit` 어노테이션 (기존 패턴)
- **응답 형식**: `ApiResponse<T>` 공통 래퍼
- **알림**: FCM (기존 FcmTokenService 활용)

---

## 데이터 계약 표준

### Enum 값 변환 규칙

#### FacilityType (시설 유형)
```java
public enum FacilityType {
    STUDY("study"),           // 스터디룸
    SEMINAR("seminar"),       // 세미나실
    AUDITORIUM("auditorium"), // 강당
    GYM("gym");               // 체육관

    private final String value;

    FacilityType(String value) {
        this.value = value;
    }

    @JsonValue
    public String toValue() {
        return value;
    }

    @JsonCreator
    public static FacilityType fromValue(String value) {
        for (FacilityType type : values()) {
            if (type.value.equals(value)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Invalid FacilityType: " + value);
    }
}
```

**JSON 표현**: `"study"`, `"seminar"`, `"auditorium"`, `"gym"` (소문자)
**DB/Java**: `STUDY`, `SEMINAR`, `AUDITORIUM`, `GYM` (대문자)

#### ReservationStatus (예약 상태)
```java
public enum ReservationStatus {
    PENDING("pending"),       // 승인 대기
    APPROVED("approved"),     // 승인됨
    REJECTED("rejected"),     // 반려됨
    CANCELLED("cancelled"),   // 사용자 취소
    COMPLETED("completed");   // 사용 완료

    private final String value;

    ReservationStatus(String value) {
        this.value = value;
    }

    @JsonValue
    public String toValue() {
        return value;
    }

    @JsonCreator
    public static ReservationStatus fromValue(String value) {
        for (ReservationStatus status : values()) {
            if (status.value.equals(value)) {
                return status;
            }
        }
        throw new IllegalArgumentException("Invalid ReservationStatus: " + value);
    }
}
```

### 날짜/시간 형식

| 용도 | 형식 | 예시 | 설명 |
|------|------|------|------|
| API 전송 | ISO 8601 | `2025-10-06T14:30:00+09:00` | LocalDateTime → JSON |
| 날짜 표시 | YYYY-MM-DD | `2025-10-06` | `dateFormatted` 필드 |
| 시간 범위 | HH:MM-HH:MM | `14:30-16:30` | `timeRangeFormatted` 필드 |
| 시간 슬롯 | HH:MM | `09:00` | 선택 UI용 |

**Jackson 설정**
```java
@Configuration
public class JacksonConfig {
    @Bean
    public Jackson2ObjectMapperBuilderCustomizer jsonCustomizer() {
        return builder -> builder
            .timeZone(TimeZone.getTimeZone("Asia/Seoul"))
            .serializers(new LocalDateTimeSerializer(
                DateTimeFormatter.ISO_OFFSET_DATE_TIME
            ));
    }
}
```

---

## 엔티티 설계

### FacilityTbl
```java
@Entity
@Table(name = "FACILITY_TBL")
@Data
@NoArgsConstructor
public class FacilityTbl {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "FACILITY_IDX")
    private Integer facilityIdx;

    @Column(name = "FACILITY_NAME", nullable = false, length = 100)
    private String facilityName;

    @Enumerated(EnumType.STRING)
    @Column(name = "FACILITY_TYPE", nullable = false, length = 20)
    private FacilityType facilityType;

    @Column(name = "FACILITY_DESC", columnDefinition = "TEXT")
    private String description;

    /**
     * 최대 수용 인원 (실제 사람 수)
     * 예: 50, 100, 200
     * null이면 인원 제한 없음 (장비류)
     */
    @Column(name = "CAPACITY")
    private Integer capacity;

    @Column(name = "LOCATION", length = 200)
    private String location;

    @Column(name = "DEFAULT_EQUIPMENT", columnDefinition = "TEXT")
    private String defaultEquipment; // CSV: "빔프로젝터,화이트보드,마이크"

    @Column(name = "IS_ACTIVE")
    private Integer isActive = 1; // 1: 활성, 0: 비활성

    @Column(name = "CREATED_AT", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "UPDATED_AT")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
```

**SQL DDL**
```sql
CREATE TABLE FACILITY_TBL (
    FACILITY_IDX INT PRIMARY KEY AUTO_INCREMENT,
    FACILITY_NAME VARCHAR(100) NOT NULL,
    FACILITY_TYPE VARCHAR(20) NOT NULL,
    FACILITY_DESC TEXT,
    CAPACITY INT,
    LOCATION VARCHAR(200),
    DEFAULT_EQUIPMENT TEXT,
    IS_ACTIVE INT DEFAULT 1,
    CREATED_AT DATETIME DEFAULT CURRENT_TIMESTAMP,
    UPDATED_AT DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_type_active (FACILITY_TYPE, IS_ACTIVE),
    INDEX idx_name (FACILITY_NAME)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

### FacilityReservationTbl
```java
@Entity
@Table(name = "FACILITY_RESERVATION_TBL")
@Data
@NoArgsConstructor
public class FacilityReservationTbl {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "RESERVATION_IDX")
    private Integer reservationIdx;

    @Column(name = "FACILITY_IDX", nullable = false)
    private Integer facilityIdx;

    @Column(name = "USER_CODE", nullable = false, length = 50)
    private String userCode;

    @Column(name = "START_TIME", nullable = false)
    private LocalDateTime startTime;

    @Column(name = "END_TIME", nullable = false)
    private LocalDateTime endTime;

    @Column(name = "PARTY_SIZE")
    private Integer partySize;

    @Column(name = "PURPOSE", columnDefinition = "TEXT")
    private String purpose;

    @Column(name = "REQUESTED_EQUIPMENT", columnDefinition = "TEXT")
    private String requestedEquipment;

    @Enumerated(EnumType.STRING)
    @Column(name = "STATUS", nullable = false, length = 20)
    private ReservationStatus status = ReservationStatus.PENDING;

    @Column(name = "ADMIN_NOTE", columnDefinition = "TEXT")
    private String adminNote;

    @Column(name = "REJECTION_REASON", columnDefinition = "TEXT")
    private String rejectionReason;

    @Column(name = "APPROVED_BY", length = 50)
    private String approvedBy; // adminCode

    @Column(name = "APPROVED_AT")
    private LocalDateTime approvedAt;

    @Column(name = "CREATED_AT", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "UPDATED_AT")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
```

**SQL DDL**
```sql
CREATE TABLE FACILITY_RESERVATION_TBL (
    RESERVATION_IDX INT PRIMARY KEY AUTO_INCREMENT,
    FACILITY_IDX INT NOT NULL,
    USER_CODE VARCHAR(50) NOT NULL,
    START_TIME DATETIME NOT NULL,
    END_TIME DATETIME NOT NULL,
    PARTY_SIZE INT,
    PURPOSE TEXT,
    REQUESTED_EQUIPMENT TEXT,
    STATUS VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    ADMIN_NOTE TEXT,
    REJECTION_REASON TEXT,
    APPROVED_BY VARCHAR(50),
    APPROVED_AT DATETIME,
    CREATED_AT DATETIME DEFAULT CURRENT_TIMESTAMP,
    UPDATED_AT DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (FACILITY_IDX) REFERENCES FACILITY_TBL(FACILITY_IDX),
    INDEX idx_facility_time (FACILITY_IDX, START_TIME, END_TIME),
    INDEX idx_user_status (USER_CODE, STATUS),
    INDEX idx_status_time (STATUS, START_TIME),
    INDEX idx_created (CREATED_AT DESC)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

### FacilityReservationLog
```java
@Entity
@Table(name = "FACILITY_RESERVATION_LOG")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FacilityReservationLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "LOG_IDX")
    private Integer logIdx;

    @Column(name = "RESERVATION_IDX", nullable = false)
    private Integer reservationIdx;

    @Column(name = "EVENT_TYPE", nullable = false, length = 50)
    private String eventType; // REQUESTED, APPROVED, REJECTED, CANCELLED, SYSTEM_COMPLETED

    @Column(name = "ACTOR_TYPE", length = 20)
    private String actorType; // USER, ADMIN, SYSTEM

    @Column(name = "ACTOR_CODE", length = 50)
    private String actorCode; // userCode or adminCode

    @Column(name = "PAYLOAD", columnDefinition = "TEXT")
    private String payload; // JSON: {"reason": "...", "note": "..."}

    @Column(name = "CREATED_AT", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
```

**SQL DDL**
```sql
CREATE TABLE FACILITY_RESERVATION_LOG (
    LOG_IDX INT PRIMARY KEY AUTO_INCREMENT,
    RESERVATION_IDX INT NOT NULL,
    EVENT_TYPE VARCHAR(50) NOT NULL,
    ACTOR_TYPE VARCHAR(20),
    ACTOR_CODE VARCHAR(50),
    PAYLOAD TEXT,
    CREATED_AT DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (RESERVATION_IDX) REFERENCES FACILITY_RESERVATION_TBL(RESERVATION_IDX),
    INDEX idx_reservation (RESERVATION_IDX),
    INDEX idx_created (CREATED_AT DESC)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

---

## Repository 계층

### FacilityRepository
```java
@Repository
public interface FacilityRepository extends JpaRepository<FacilityTbl, Integer> {

    List<FacilityTbl> findByIsActive(Integer isActive);

    List<FacilityTbl> findByFacilityTypeAndIsActive(FacilityType facilityType, Integer isActive);

    List<FacilityTbl> findByFacilityNameContainingIgnoreCaseAndIsActive(
        String keyword, Integer isActive
    );

    Optional<FacilityTbl> findByFacilityIdxAndIsActive(Integer facilityIdx, Integer isActive);
}
```

### FacilityReservationRepository
```java
@Repository
public interface FacilityReservationRepository extends JpaRepository<FacilityReservationTbl, Integer> {

    /**
     * 시간대 중복 체크
     */
    @Query("""
        SELECT r FROM FacilityReservationTbl r
        WHERE r.facilityIdx = :facilityIdx
          AND r.status IN ('PENDING', 'APPROVED')
          AND r.startTime < :endTime
          AND r.endTime > :startTime
    """)
    List<FacilityReservationTbl> findConflictingReservations(
        @Param("facilityIdx") Integer facilityIdx,
        @Param("startTime") LocalDateTime startTime,
        @Param("endTime") LocalDateTime endTime
    );

    /**
     * 특정 날짜의 모든 예약 조회 (시간 슬롯 계산용)
     */
    @Query("""
        SELECT r FROM FacilityReservationTbl r
        WHERE r.facilityIdx = :facilityIdx
          AND DATE(r.startTime) = :date
          AND r.status IN ('PENDING', 'APPROVED')
        ORDER BY r.startTime
    """)
    List<FacilityReservationTbl> findByFacilityAndDate(
        @Param("facilityIdx") Integer facilityIdx,
        @Param("date") LocalDate date
    );

    /**
     * 사용자별 예약 목록
     */
    List<FacilityReservationTbl> findByUserCodeAndStatusInOrderByStartTimeDesc(
        String userCode, List<ReservationStatus> statuses
    );

    /**
     * 사용자 활성 예약 개수
     */
    long countByUserCodeAndStatusIn(String userCode, List<ReservationStatus> statuses);

    /**
     * 승인 대기 목록 (사용자 정보 조인)
     */
    @Query("""
        SELECT new BlueCrab.com.example.dto.facility.admin.AdminReservationDetailDto(
            r.reservationIdx,
            f.facilityName,
            f.facilityType,
            r.startTime,
            r.endTime,
            r.status,
            r.purpose,
            r.partySize,
            r.requestedEquipment,
            u.userName,
            u.userCode,
            u.userEmail,
            r.createdAt
        )
        FROM FacilityReservationTbl r
        JOIN FacilityTbl f ON r.facilityIdx = f.facilityIdx
        JOIN UserTbl u ON r.userCode = u.userCode
        WHERE r.status = 'PENDING'
        ORDER BY r.createdAt ASC
    """)
    List<AdminReservationDetailDto> findPendingWithUserInfo();

    /**
     * 관리자 필터 검색
     */
    @Query("""
        SELECT r FROM FacilityReservationTbl r
        JOIN FacilityTbl f ON r.facilityIdx = f.facilityIdx
        WHERE (:facilityType IS NULL OR f.facilityType = :facilityType)
          AND (:facilityName IS NULL OR f.facilityName LIKE %:facilityName%)
          AND (:status IS NULL OR r.status = :status)
          AND (:dateFrom IS NULL OR DATE(r.startTime) >= :dateFrom)
          AND (:dateTo IS NULL OR DATE(r.startTime) <= :dateTo)
        ORDER BY r.startTime DESC
    """)
    Page<FacilityReservationTbl> findByAdminFilter(
        @Param("facilityType") FacilityType facilityType,
        @Param("facilityName") String facilityName,
        @Param("status") ReservationStatus status,
        @Param("dateFrom") LocalDate dateFrom,
        @Param("dateTo") LocalDate dateTo,
        Pageable pageable
    );

    /**
     * 대시보드 통계
     */
    @Query(value = """
        SELECT
            COUNT(CASE WHEN DATE(start_time) = :today THEN 1 END) as total_today,
            COUNT(CASE WHEN start_time <= :now AND end_time > :now
                        AND status = 'APPROVED' THEN 1 END) as in_use,
            COUNT(CASE WHEN start_time > :now AND status = 'APPROVED' THEN 1 END) as upcoming,
            COUNT(CASE WHEN DATE(start_time) >= :weekStart THEN 1 END) as week_total,
            COUNT(CASE WHEN DATE(start_time) >= :monthStart THEN 1 END) as month_total
        FROM facility_reservation_tbl
        WHERE DATE(start_time) >= :today OR status IN ('PENDING', 'APPROVED')
    """, nativeQuery = true)
    Map<String, Object> getDashboardStats(
        @Param("today") LocalDate today,
        @Param("now") LocalDateTime now,
        @Param("weekStart") LocalDate weekStart,
        @Param("monthStart") LocalDate monthStart
    );

    /**
     * 만료된 예약 조회 (스케줄러용)
     */
    @Query("""
        SELECT r FROM FacilityReservationTbl r
        WHERE r.status = 'APPROVED'
          AND r.endTime < :currentTime
    """)
    List<FacilityReservationTbl> findExpiredReservations(
        @Param("currentTime") LocalDateTime currentTime
    );
}
```

### FacilityReservationLogRepository
```java
@Repository
public interface FacilityReservationLogRepository extends JpaRepository<FacilityReservationLog, Integer> {

    List<FacilityReservationLog> findByReservationIdxOrderByCreatedAtDesc(Integer reservationIdx);
}
```

---

## DTO 설계

### 패키지 구조
```
dto/
├── facility/
│   ├── request/
│   │   ├── FacilityListRequestDto.java
│   │   ├── FacilityAvailabilityRequestDto.java
│   │   ├── FacilityReservationRequestDto.java
│   │   └── ReservationCancelRequestDto.java
│   ├── response/
│   │   ├── FacilitySummaryDto.java
│   │   ├── FacilityDetailDto.java
│   │   ├── TimeSlotStatusDto.java
│   │   ├── FacilityListResponseDto.java
│   │   ├── ReservationResponseDto.java
│   │   └── MyReservationListDto.java
│   └── admin/
│       ├── AdminReservationFilterDto.java
│       ├── AdminReservationDetailDto.java
│       ├── AdminReservationStatsDto.java
│       ├── AdminApproveRequestDto.java
│       └── AdminRejectRequestDto.java
```

### 핵심 DTO 정의

#### TimeSlotStatusDto
```java
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TimeSlotStatusDto {
    /**
     * 시간 슬롯 (HH:mm 형식)
     */
    private String time; // "09:00", "10:00", ...

    /**
     * 슬롯 상태
     * - "available": 예약 가능
     * - "reserved": 이미 예약됨 (APPROVED)
     * - "pending": 승인 대기 중 (PENDING)
     */
    private String status;

    /**
     * 예약 ID (status가 reserved/pending일 때만)
     */
    private Integer reservationId;

    /**
     * 예약자 이름 (status가 reserved/pending일 때만)
     */
    private String reserver;
}
```

#### FacilityReservationRequestDto
```java
@Data
public class FacilityReservationRequestDto {
    @NotNull
    private Integer facilityId;

    @NotNull
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate date;

    /**
     * 선택한 시간 슬롯 배열
     * 예: ["09:00", "10:00", "11:00"]
     */
    @NotEmpty
    private List<String> timeSlots;

    @NotNull
    @Min(1)
    private Integer partySize;

    @NotBlank
    @Size(max = 500)
    private String purpose;

    @Size(max = 200)
    private String requestedEquipment;
}
```

#### ReservationResponseDto
```java
@Data
@Builder
public class ReservationResponseDto {
    private Integer reservationId;
    private Integer facilityId;
    private String facilityName;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ssXXX")
    private LocalDateTime startTime;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ssXXX")
    private LocalDateTime endTime;

    /**
     * 표시용 포맷된 문자열
     */
    private String dateFormatted;      // "2025-10-06"
    private String timeRangeFormatted; // "09:00-12:00"

    private String status; // "pending", "approved", ...
    private Integer partySize;
    private String purpose;
    private String requestedEquipment;
    private String adminNote;
    private String rejectionReason;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ssXXX")
    private LocalDateTime createdAt;
}
```

#### AdminReservationStatsDto
```java
@Data
@Builder
public class AdminReservationStatsDto {
    private TodayStats today;
    private Integer thisWeek;
    private Integer thisMonth;
}

@Data
@Builder
public class TodayStats {
    private Integer total;      // 오늘 전체
    private Integer inUse;      // 현재 이용 중
    private Integer upcoming;   // 이용 예정
    private Integer completed;  // 완료
}
```

---

## 서비스 로직

### FacilityService
```java
@Service
@Slf4j
public class FacilityService {
    @Autowired
    private FacilityRepository facilityRepository;

    @Autowired
    private FacilityReservationRepository reservationRepository;

    /**
     * 시설 목록 조회
     */
    public FacilityListResponseDto getFacilities(String facilityType) {
        List<FacilityTbl> facilities;

        if (facilityType != null && !facilityType.isEmpty()) {
            FacilityType type = FacilityType.fromValue(facilityType);
            facilities = facilityRepository.findByFacilityTypeAndIsActive(type, 1);
        } else {
            facilities = facilityRepository.findByIsActive(1);
        }

        List<FacilitySummaryDto> dtoList = facilities.stream()
            .map(this::toSummaryDto)
            .collect(Collectors.toList());

        return FacilityListResponseDto.builder()
            .facilities(dtoList)
            .totalCount(dtoList.size())
            .build();
    }

    /**
     * 시설 상세 조회
     */
    public FacilityDetailDto getFacilityDetail(Integer facilityId) {
        FacilityTbl facility = facilityRepository.findByFacilityIdxAndIsActive(facilityId, 1)
            .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 시설입니다."));

        return toDetailDto(facility);
    }

    /**
     * 시간 슬롯 가용성 조회
     */
    public List<TimeSlotStatusDto> getAvailability(Integer facilityId, LocalDate date) {
        // 1. 시설 존재 확인
        FacilityTbl facility = facilityRepository.findByFacilityIdxAndIsActive(facilityId, 1)
            .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 시설입니다."));

        // 2. 해당 날짜의 예약 조회
        List<FacilityReservationTbl> reservations =
            reservationRepository.findByFacilityAndDate(facilityId, date);

        // 3. 시간 슬롯 생성 (09:00 ~ 21:00, 1시간 단위)
        List<TimeSlotStatusDto> slots = new ArrayList<>();
        for (int hour = 9; hour < 21; hour++) {
            String timeStr = String.format("%02d:00", hour);
            LocalTime slotTime = LocalTime.of(hour, 0);
            LocalDateTime slotStart = LocalDateTime.of(date, slotTime);
            LocalDateTime slotEnd = slotStart.plusHours(1);

            // 4. 해당 슬롯에 예약이 있는지 확인
            TimeSlotStatusDto slot = new TimeSlotStatusDto();
            slot.setTime(timeStr);

            boolean occupied = false;
            for (FacilityReservationTbl reservation : reservations) {
                if (reservation.getStartTime().isBefore(slotEnd) &&
                    reservation.getEndTime().isAfter(slotStart)) {
                    occupied = true;
                    slot.setReservationId(reservation.getReservationIdx());
                    // TODO: 사용자 이름 조회 (UserTbl JOIN)

                    if (reservation.getStatus() == ReservationStatus.APPROVED) {
                        slot.setStatus("reserved");
                    } else {
                        slot.setStatus("pending");
                    }
                    break;
                }
            }

            if (!occupied) {
                slot.setStatus("available");
            }

            slots.add(slot);
        }

        return slots;
    }

    private FacilitySummaryDto toSummaryDto(FacilityTbl entity) {
        // Highlight 로직: 체육관이거나 수용 인원 100명 이상
        boolean highlight = entity.getFacilityType() == FacilityType.GYM
                         || (entity.getCapacity() != null && entity.getCapacity() >= 100);

        return FacilitySummaryDto.builder()
            .facilityId(entity.getFacilityIdx())
            .facilityName(entity.getFacilityName())
            .facilityType(entity.getFacilityType().toValue())
            .capacity(entity.getCapacity())
            .location(entity.getLocation())
            .highlighted(highlight)
            .build();
    }

    private FacilityDetailDto toDetailDto(FacilityTbl entity) {
        return FacilityDetailDto.builder()
            .facilityId(entity.getFacilityIdx())
            .facilityName(entity.getFacilityName())
            .facilityType(entity.getFacilityType().toValue())
            .description(entity.getDescription())
            .capacity(entity.getCapacity())
            .location(entity.getLocation())
            .defaultEquipment(entity.getDefaultEquipment())
            .build();
    }
}
```

### FacilityReservationService
```java
@Service
@Slf4j
@Transactional
public class FacilityReservationService {
    @Autowired
    private FacilityRepository facilityRepository;

    @Autowired
    private FacilityReservationRepository reservationRepository;

    @Autowired
    private FacilityReservationLogRepository logRepository;

    @Autowired
    private ReservationPolicyProperties policy;

    /**
     * 예약 생성
     */
    public ReservationResponseDto createReservation(
        FacilityReservationRequestDto request, String userCode
    ) {
        // 1. 시설 존재 확인
        FacilityTbl facility = facilityRepository.findByFacilityIdxAndIsActive(request.getFacilityId(), 1)
            .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 시설입니다."));

        // 2. 시간 슬롯 → LocalDateTime 변환
        TimeRange timeRange = calculateTimeRange(request.getDate(), request.getTimeSlots());

        // 3. 예약 가능 여부 검증
        validateReservation(facility, timeRange, request, userCode);

        // 4. 예약 생성
        FacilityReservationTbl reservation = new FacilityReservationTbl();
        reservation.setFacilityIdx(facility.getFacilityIdx());
        reservation.setUserCode(userCode);
        reservation.setStartTime(timeRange.getStartTime());
        reservation.setEndTime(timeRange.getEndTime());
        reservation.setPartySize(request.getPartySize());
        reservation.setPurpose(request.getPurpose());
        reservation.setRequestedEquipment(request.getRequestedEquipment());
        reservation.setStatus(ReservationStatus.PENDING);

        FacilityReservationTbl saved = reservationRepository.save(reservation);

        // 5. 로그 기록
        logRepository.save(FacilityReservationLog.builder()
            .reservationIdx(saved.getReservationIdx())
            .eventType("REQUESTED")
            .actorType("USER")
            .actorCode(userCode)
            .build());

        log.info("예약 생성 완료: reservationId={}, userCode={}, facility={}, time={}-{}",
            saved.getReservationIdx(), userCode, facility.getFacilityName(),
            timeRange.getStartTime(), timeRange.getEndTime());

        return toResponseDto(saved, facility);
    }

    /**
     * 다중 슬롯 → 시작/종료 시간 계산
     */
    private TimeRange calculateTimeRange(LocalDate date, List<String> timeSlots) {
        if (timeSlots == null || timeSlots.isEmpty()) {
            throw new IllegalArgumentException("최소 1개 이상의 시간을 선택해야 합니다.");
        }

        // 1. 슬롯을 LocalTime으로 변환 후 정렬
        List<LocalTime> times = timeSlots.stream()
            .map(LocalTime::parse)
            .sorted()
            .collect(Collectors.toList());

        // 2. 연속성 검증
        for (int i = 1; i < times.size(); i++) {
            LocalTime prev = times.get(i - 1);
            LocalTime curr = times.get(i);
            if (Duration.between(prev, curr).toHours() != 1) {
                throw new IllegalArgumentException(
                    "선택한 시간은 연속되어야 합니다. (" + prev + " ~ " + curr + " 사이 간격)"
                );
            }
        }

        // 3. 시작/종료 시간 계산
        LocalTime startTime = times.get(0);
        LocalTime endTime = times.get(times.size() - 1).plusHours(1);

        return new TimeRange(
            LocalDateTime.of(date, startTime),
            LocalDateTime.of(date, endTime)
        );
    }

    /**
     * 예약 검증
     */
    private void validateReservation(
        FacilityTbl facility,
        TimeRange timeRange,
        FacilityReservationRequestDto request,
        String userCode
    ) {
        LocalDateTime now = LocalDateTime.now();

        // 1. 과거 시간 체크
        if (timeRange.getStartTime().isBefore(now)) {
            throw new IllegalArgumentException("과거 시간은 예약할 수 없습니다.");
        }

        // 2. 최대 예약 가능 일수 체크
        LocalDateTime maxDate = now.plusDays(policy.getMaxAdvanceDays());
        if (timeRange.getStartTime().isAfter(maxDate)) {
            throw new IllegalArgumentException(
                policy.getMaxAdvanceDays() + "일 이내만 예약 가능합니다."
            );
        }

        // 3. 예약 시간 길이 체크
        long durationMinutes = Duration.between(
            timeRange.getStartTime(), timeRange.getEndTime()
        ).toMinutes();

        if (durationMinutes < policy.getMinDurationMinutes()) {
            throw new IllegalArgumentException(
                "최소 " + policy.getMinDurationMinutes() + "분 이상 예약해야 합니다."
            );
        }

        if (durationMinutes > policy.getMaxDurationMinutes()) {
            throw new IllegalArgumentException(
                "최대 " + policy.getMaxDurationMinutes() + "분까지 예약 가능합니다."
            );
        }

        // 4. 수용 인원 체크
        if (facility.getCapacity() != null && request.getPartySize() > facility.getCapacity()) {
            throw new IllegalArgumentException(
                "최대 " + facility.getCapacity() + "명까지 수용 가능합니다."
            );
        }

        // 5. 시간대 중복 체크
        List<FacilityReservationTbl> conflicts = reservationRepository.findConflictingReservations(
            facility.getFacilityIdx(),
            timeRange.getStartTime(),
            timeRange.getEndTime()
        );

        if (!conflicts.isEmpty()) {
            throw new IllegalStateException("해당 시간대에 이미 예약이 있습니다.");
        }

        // 6. 사용자 예약 개수 제한
        long activeCount = reservationRepository.countByUserCodeAndStatusIn(
            userCode,
            Arrays.asList(ReservationStatus.PENDING, ReservationStatus.APPROVED)
        );

        if (activeCount >= policy.getMaxActiveReservationsPerUser()) {
            throw new IllegalStateException(
                "최대 " + policy.getMaxActiveReservationsPerUser() + "개까지 예약 가능합니다."
            );
        }
    }

    /**
     * 예약 취소
     */
    public void cancelReservation(Integer reservationId, String userCode) {
        // 1. 예약 조회
        FacilityReservationTbl reservation = reservationRepository.findById(reservationId)
            .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 예약입니다."));

        // 2. 본인 확인
        if (!reservation.getUserCode().equals(userCode)) {
            throw new IllegalStateException("본인의 예약만 취소할 수 있습니다.");
        }

        // 3. 취소 가능 상태 확인
        if (reservation.getStatus() == ReservationStatus.CANCELLED ||
            reservation.getStatus() == ReservationStatus.COMPLETED) {
            throw new IllegalStateException("이미 취소되었거나 완료된 예약입니다.");
        }

        // 4. 취소 가능 시간 확인
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime deadline = reservation.getStartTime()
            .minusHours(policy.getCancelDeadlineHours());

        if (now.isAfter(deadline)) {
            throw new IllegalStateException(
                "예약 시작 " + policy.getCancelDeadlineHours() + "시간 전까지만 취소 가능합니다."
            );
        }

        // 5. 취소 처리
        reservation.setStatus(ReservationStatus.CANCELLED);
        reservationRepository.save(reservation);

        // 6. 로그 기록
        logRepository.save(FacilityReservationLog.builder()
            .reservationIdx(reservationId)
            .eventType("CANCELLED")
            .actorType("USER")
            .actorCode(userCode)
            .build());

        log.info("예약 취소 완료: reservationId={}, userCode={}", reservationId, userCode);
    }

    /**
     * 내 예약 목록 조회
     */
    public MyReservationListDto getMyReservations(String userCode) {
        List<ReservationStatus> ongoingStatuses = Arrays.asList(
            ReservationStatus.PENDING, ReservationStatus.APPROVED
        );

        List<ReservationStatus> completedStatuses = Arrays.asList(
            ReservationStatus.COMPLETED, ReservationStatus.REJECTED, ReservationStatus.CANCELLED
        );

        List<FacilityReservationTbl> ongoing =
            reservationRepository.findByUserCodeAndStatusInOrderByStartTimeDesc(
                userCode, ongoingStatuses
            );

        List<FacilityReservationTbl> completed =
            reservationRepository.findByUserCodeAndStatusInOrderByStartTimeDesc(
                userCode, completedStatuses
            );

        return MyReservationListDto.builder()
            .ongoing(ongoing.stream().map(this::toSimpleDto).collect(Collectors.toList()))
            .completed(completed.stream().map(this::toSimpleDto).collect(Collectors.toList()))
            .build();
    }

    private ReservationResponseDto toResponseDto(
        FacilityReservationTbl reservation,
        FacilityTbl facility
    ) {
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm");

        String timeRange = reservation.getStartTime().format(timeFormatter) +
                          "-" +
                          reservation.getEndTime().format(timeFormatter);

        return ReservationResponseDto.builder()
            .reservationId(reservation.getReservationIdx())
            .facilityId(facility.getFacilityIdx())
            .facilityName(facility.getFacilityName())
            .startTime(reservation.getStartTime())
            .endTime(reservation.getEndTime())
            .dateFormatted(reservation.getStartTime().format(dateFormatter))
            .timeRangeFormatted(timeRange)
            .status(reservation.getStatus().toValue())
            .partySize(reservation.getPartySize())
            .purpose(reservation.getPurpose())
            .requestedEquipment(reservation.getRequestedEquipment())
            .adminNote(reservation.getAdminNote())
            .rejectionReason(reservation.getRejectionReason())
            .createdAt(reservation.getCreatedAt())
            .build();
    }

    private ReservationResponseDto toSimpleDto(FacilityReservationTbl reservation) {
        // TODO: FacilityTbl 조회 (캐시 또는 JOIN)
        return toResponseDto(reservation, null);
    }
}

@Data
@AllArgsConstructor
class TimeRange {
    private LocalDateTime startTime;
    private LocalDateTime endTime;
}
```

### AdminFacilityReservationService
```java
@Service
@Slf4j
@Transactional
public class AdminFacilityReservationService {
    @Autowired
    private FacilityReservationRepository reservationRepository;

    @Autowired
    private FacilityReservationLogRepository logRepository;

    @Autowired
    private FcmTokenService fcmService;

    /**
     * 승인 대기 목록 조회
     */
    public List<AdminReservationDetailDto> getPendingReservations() {
        return reservationRepository.findPendingWithUserInfo();
    }

    /**
     * 예약 승인
     */
    public void approveReservation(Integer reservationId, String adminCode, String note) {
        FacilityReservationTbl reservation = reservationRepository.findById(reservationId)
            .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 예약입니다."));

        if (reservation.getStatus() != ReservationStatus.PENDING) {
            throw new IllegalStateException("대기 중인 예약만 승인할 수 있습니다.");
        }

        reservation.setStatus(ReservationStatus.APPROVED);
        reservation.setApprovedBy(adminCode);
        reservation.setApprovedAt(LocalDateTime.now());
        reservation.setAdminNote(note);
        reservationRepository.save(reservation);

        // 로그 기록
        logRepository.save(FacilityReservationLog.builder()
            .reservationIdx(reservationId)
            .eventType("APPROVED")
            .actorType("ADMIN")
            .actorCode(adminCode)
            .payload("{\"note\":\"" + note + "\"}")
            .build());

        // FCM 알림 발송 (비동기)
        sendApprovalNotification(reservation);

        log.info("예약 승인 완료: reservationId={}, adminCode={}", reservationId, adminCode);
    }

    /**
     * 예약 반려
     */
    public void rejectReservation(Integer reservationId, String adminCode, String reason) {
        FacilityReservationTbl reservation = reservationRepository.findById(reservationId)
            .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 예약입니다."));

        if (reservation.getStatus() != ReservationStatus.PENDING) {
            throw new IllegalStateException("대기 중인 예약만 반려할 수 있습니다.");
        }

        reservation.setStatus(ReservationStatus.REJECTED);
        reservation.setRejectionReason(reason);
        reservationRepository.save(reservation);

        // 로그 기록
        logRepository.save(FacilityReservationLog.builder()
            .reservationIdx(reservationId)
            .eventType("REJECTED")
            .actorType("ADMIN")
            .actorCode(adminCode)
            .payload("{\"reason\":\"" + reason + "\"}")
            .build());

        // FCM 알림 발송
        sendRejectionNotification(reservation, reason);

        log.info("예약 반려 완료: reservationId={}, adminCode={}, reason={}",
            reservationId, adminCode, reason);
    }

    /**
     * 대시보드 통계
     */
    public AdminReservationStatsDto getDashboardStats() {
        LocalDate today = LocalDate.now();
        LocalDateTime now = LocalDateTime.now();
        LocalDate weekStart = today.with(DayOfWeek.MONDAY);
        LocalDate monthStart = today.withDayOfMonth(1);

        Map<String, Object> raw = reservationRepository.getDashboardStats(
            today, now, weekStart, monthStart
        );

        return AdminReservationStatsDto.builder()
            .today(TodayStats.builder()
                .total(((Number) raw.get("total_today")).intValue())
                .inUse(((Number) raw.get("in_use")).intValue())
                .upcoming(((Number) raw.get("upcoming")).intValue())
                .completed(0) // TODO: 계산 로직 추가
                .build())
            .thisWeek(((Number) raw.get("week_total")).intValue())
            .thisMonth(((Number) raw.get("month_total")).intValue())
            .build();
    }

    @Async
    private void sendApprovalNotification(FacilityReservationTbl reservation) {
        try {
            String title = "시설 예약 승인";
            String body = String.format(
                "예약이 승인되었습니다.\n일시: %s %s",
                reservation.getStartTime().toLocalDate(),
                reservation.getStartTime().toLocalTime()
            );
            fcmService.sendToUser(reservation.getUserCode(), title, body);
        } catch (Exception e) {
            log.error("FCM 알림 발송 실패: reservationId={}", reservation.getReservationIdx(), e);
        }
    }

    @Async
    private void sendRejectionNotification(FacilityReservationTbl reservation, String reason) {
        try {
            String title = "시설 예약 반려";
            String body = "예약이 반려되었습니다.\n사유: " + reason;
            fcmService.sendToUser(reservation.getUserCode(), title, body);
        } catch (Exception e) {
            log.error("FCM 알림 발송 실패: reservationId={}", reservation.getReservationIdx(), e);
        }
    }
}
```

---

## Controller API

### FacilityController (학생용)
```java
@RestController
@RequestMapping("/api/facilities")
@Slf4j
public class FacilityController {

    @Autowired
    private FacilityService facilityService;

    @Autowired
    private JwtUtil jwtUtil;

    /**
     * 시설 목록 조회
     */
    @RateLimit(timeWindow = 10, maxRequests = 10)
    @PostMapping("/list")
    public ResponseEntity<ApiResponse<FacilityListResponseDto>> getFacilities(
        HttpServletRequest request,
        @RequestBody(required = false) FacilityListRequestDto dto
    ) {
        try {
            // JWT 검증
            String token = extractToken(request);
            if (!jwtUtil.validateToken(token)) {
                return ResponseEntity.status(401)
                    .body(new ApiResponse<>(false, "인증이 필요합니다.", null, "UNAUTHORIZED"));
            }

            String facilityType = dto != null ? dto.getFacilityType() : null;
            FacilityListResponseDto result = facilityService.getFacilities(facilityType);

            return ResponseEntity.ok(
                new ApiResponse<>(true, "시설 목록을 조회했습니다.", result)
            );

        } catch (Exception e) {
            log.error("시설 목록 조회 실패", e);
            return ResponseEntity.status(500)
                .body(new ApiResponse<>(false, "조회 중 오류가 발생했습니다.", null, "INTERNAL_ERROR"));
        }
    }

    /**
     * 시설 상세 조회
     */
    @RateLimit(timeWindow = 5, maxRequests = 20)
    @PostMapping("/{id}/detail")
    public ResponseEntity<ApiResponse<FacilityDetailDto>> getFacilityDetail(
        HttpServletRequest request,
        @PathVariable Integer id
    ) {
        try {
            String token = extractToken(request);
            if (!jwtUtil.validateToken(token)) {
                return ResponseEntity.status(401)
                    .body(new ApiResponse<>(false, "인증이 필요합니다.", null, "UNAUTHORIZED"));
            }

            FacilityDetailDto result = facilityService.getFacilityDetail(id);

            return ResponseEntity.ok(
                new ApiResponse<>(true, "시설 상세 정보를 조회했습니다.", result)
            );

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                .body(new ApiResponse<>(false, e.getMessage(), null, "INVALID_FACILITY"));

        } catch (Exception e) {
            log.error("시설 상세 조회 실패: facilityId={}", id, e);
            return ResponseEntity.status(500)
                .body(new ApiResponse<>(false, "조회 중 오류가 발생했습니다.", null, "INTERNAL_ERROR"));
        }
    }

    /**
     * 시간 슬롯 가용성 조회
     */
    @RateLimit(timeWindow = 5, maxRequests = 30)
    @PostMapping("/{id}/availability")
    public ResponseEntity<ApiResponse<List<TimeSlotStatusDto>>> getAvailability(
        HttpServletRequest request,
        @PathVariable Integer id,
        @RequestBody FacilityAvailabilityRequestDto dto
    ) {
        try {
            String token = extractToken(request);
            if (!jwtUtil.validateToken(token)) {
                return ResponseEntity.status(401)
                    .body(new ApiResponse<>(false, "인증이 필요합니다.", null, "UNAUTHORIZED"));
            }

            List<TimeSlotStatusDto> slots = facilityService.getAvailability(id, dto.getDate());

            return ResponseEntity.ok(
                new ApiResponse<>(true, "시간대 정보를 조회했습니다.", slots)
            );

        } catch (Exception e) {
            log.error("시간대 조회 실패: facilityId={}, date={}", id, dto.getDate(), e);
            return ResponseEntity.status(500)
                .body(new ApiResponse<>(false, "조회 중 오류가 발생했습니다.", null, "INTERNAL_ERROR"));
        }
    }

    private String extractToken(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }
}
```

### FacilityReservationController (학생용)
```java
@RestController
@RequestMapping("/api/facility-reservations")
@Slf4j
public class FacilityReservationController {

    @Autowired
    private FacilityReservationService reservationService;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private UserTblRepository userRepository;

    /**
     * 예약 생성
     */
    @RateLimit(timeWindow = 60, maxRequests = 3)
    @PostMapping("/request")
    public ResponseEntity<ApiResponse<ReservationResponseDto>> createReservation(
        HttpServletRequest request,
        @Valid @RequestBody FacilityReservationRequestDto dto
    ) {
        try {
            String userCode = getUserCodeFromToken(request);

            ReservationResponseDto result = reservationService.createReservation(dto, userCode);

            return ResponseEntity.ok(
                new ApiResponse<>(true, "예약 신청이 완료되었습니다. 관리자 승인 후 확정됩니다.", result)
            );

        } catch (IllegalArgumentException | IllegalStateException e) {
            log.warn("예약 생성 실패: {}", e.getMessage());
            return ResponseEntity.badRequest()
                .body(new ApiResponse<>(false, e.getMessage(), null, "RESERVATION_FAILED"));

        } catch (Exception e) {
            log.error("예약 생성 중 오류 발생", e);
            return ResponseEntity.status(500)
                .body(new ApiResponse<>(false, "예약 중 오류가 발생했습니다.", null, "INTERNAL_ERROR"));
        }
    }

    /**
     * 예약 취소
     */
    @RateLimit(timeWindow = 30, maxRequests = 5)
    @PostMapping("/cancel")
    public ResponseEntity<ApiResponse<Void>> cancelReservation(
        HttpServletRequest request,
        @Valid @RequestBody ReservationCancelRequestDto dto
    ) {
        try {
            String userCode = getUserCodeFromToken(request);

            reservationService.cancelReservation(dto.getReservationId(), userCode);

            return ResponseEntity.ok(
                new ApiResponse<>(true, "예약이 취소되었습니다.", null)
            );

        } catch (IllegalArgumentException | IllegalStateException e) {
            log.warn("예약 취소 실패: {}", e.getMessage());
            return ResponseEntity.badRequest()
                .body(new ApiResponse<>(false, e.getMessage(), null, "CANCEL_FAILED"));

        } catch (Exception e) {
            log.error("예약 취소 중 오류 발생", e);
            return ResponseEntity.status(500)
                .body(new ApiResponse<>(false, "취소 중 오류가 발생했습니다.", null, "INTERNAL_ERROR"));
        }
    }

    /**
     * 내 예약 목록 조회
     */
    @RateLimit(timeWindow = 10, maxRequests = 10)
    @PostMapping("/my")
    public ResponseEntity<ApiResponse<MyReservationListDto>> getMyReservations(
        HttpServletRequest request
    ) {
        try {
            String userCode = getUserCodeFromToken(request);

            MyReservationListDto result = reservationService.getMyReservations(userCode);

            return ResponseEntity.ok(
                new ApiResponse<>(true, "내 예약 목록을 조회했습니다.", result)
            );

        } catch (Exception e) {
            log.error("예약 목록 조회 중 오류 발생", e);
            return ResponseEntity.status(500)
                .body(new ApiResponse<>(false, "조회 중 오류가 발생했습니다.", null, "INTERNAL_ERROR"));
        }
    }

    private String getUserCodeFromToken(HttpServletRequest request) {
        String token = extractToken(request);
        if (!jwtUtil.validateToken(token)) {
            throw new RuntimeException("인증이 필요합니다.");
        }

        Integer userId = jwtUtil.extractUserId(token);
        UserTbl user = userRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("존재하지 않는 사용자입니다."));

        return user.getUserCode();
    }

    private String extractToken(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }
}
```

### AdminFacilityReservationController (관리자용)
```java
@RestController
@RequestMapping("/api/admin/facility-reservations")
@Slf4j
public class AdminFacilityReservationController {

    @Autowired
    private AdminFacilityReservationService adminService;

    // TODO: AdminInterceptor 또는 @AdminOnly 어노테이션으로 관리자 인증

    /**
     * 승인 대기 목록 조회
     */
    @PostMapping("/pending")
    public ResponseEntity<ApiResponse<List<AdminReservationDetailDto>>> getPending() {
        try {
            List<AdminReservationDetailDto> result = adminService.getPendingReservations();

            return ResponseEntity.ok(
                new ApiResponse<>(true, "대기 목록을 조회했습니다.", result)
            );

        } catch (Exception e) {
            log.error("대기 목록 조회 실패", e);
            return ResponseEntity.status(500)
                .body(new ApiResponse<>(false, "조회 중 오류가 발생했습니다.", null, "INTERNAL_ERROR"));
        }
    }

    /**
     * 예약 승인
     */
    @PostMapping("/{id}/approve")
    public ResponseEntity<ApiResponse<Void>> approve(
        HttpServletRequest request,
        @PathVariable Integer id,
        @RequestBody AdminApproveRequestDto dto
    ) {
        try {
            String adminCode = getAdminCodeFromToken(request);

            adminService.approveReservation(id, adminCode, dto.getNote());

            return ResponseEntity.ok(
                new ApiResponse<>(true, "예약이 승인되었습니다.", null)
            );

        } catch (Exception e) {
            log.error("예약 승인 실패: reservationId={}", id, e);
            return ResponseEntity.status(500)
                .body(new ApiResponse<>(false, "승인 중 오류가 발생했습니다.", null, "INTERNAL_ERROR"));
        }
    }

    /**
     * 예약 반려
     */
    @PostMapping("/{id}/reject")
    public ResponseEntity<ApiResponse<Void>> reject(
        HttpServletRequest request,
        @PathVariable Integer id,
        @RequestBody AdminRejectRequestDto dto
    ) {
        try {
            String adminCode = getAdminCodeFromToken(request);

            adminService.rejectReservation(id, adminCode, dto.getReason());

            return ResponseEntity.ok(
                new ApiResponse<>(true, "예약이 반려되었습니다.", null)
            );

        } catch (Exception e) {
            log.error("예약 반려 실패: reservationId={}", id, e);
            return ResponseEntity.status(500)
                .body(new ApiResponse<>(false, "반려 중 오류가 발생했습니다.", null, "INTERNAL_ERROR"));
        }
    }

    /**
     * 대시보드 통계
     */
    @PostMapping("/stats")
    public ResponseEntity<ApiResponse<AdminReservationStatsDto>> getStats() {
        try {
            AdminReservationStatsDto result = adminService.getDashboardStats();

            return ResponseEntity.ok(
                new ApiResponse<>(true, "통계 정보를 조회했습니다.", result)
            );

        } catch (Exception e) {
            log.error("통계 조회 실패", e);
            return ResponseEntity.status(500)
                .body(new ApiResponse<>(false, "조회 중 오류가 발생했습니다.", null, "INTERNAL_ERROR"));
        }
    }

    private String getAdminCodeFromToken(HttpServletRequest request) {
        // TODO: AdminTbl 기반 인증 로직 구현
        return "admin001";
    }
}
```

---

## 설정 및 정책

### ReservationPolicyProperties
```java
@Configuration
@ConfigurationProperties(prefix = "facility.reservation.policy")
@Data
public class ReservationPolicyProperties {
    /**
     * 예약 취소 가능 기한 (시간)
     */
    private int cancelDeadlineHours = 24;

    /**
     * 최대 예약 가능 기간 (일)
     */
    private int maxAdvanceDays = 14;

    /**
     * 사용자당 최대 활성 예약 개수
     */
    private int maxActiveReservationsPerUser = 3;

    /**
     * 최소 예약 시간 (분)
     */
    private int minDurationMinutes = 60;

    /**
     * 최대 예약 시간 (분)
     */
    private int maxDurationMinutes = 240;
}
```

### application.yml
```yaml
facility:
  reservation:
    policy:
      cancel-deadline-hours: 24
      max-advance-days: 14
      max-active-reservations-per-user: 3
      min-duration-minutes: 60
      max-duration-minutes: 240

spring:
  jackson:
    time-zone: Asia/Seoul
    serialization:
      write-dates-as-timestamps: false
```

### FacilityDataInitializer
```java
@Component
public class FacilityDataInitializer implements ApplicationRunner {

    @Autowired
    private FacilityRepository facilityRepository;

    @Override
    public void run(ApplicationArguments args) throws Exception {
        if (facilityRepository.count() > 0) {
            return; // 이미 데이터 존재
        }

        List<FacilityTbl> facilities = Arrays.asList(
            createFacility("제1스터디룸", FacilityType.STUDY, 6, "도서관 2층", "화이트보드"),
            createFacility("제2스터디룸", FacilityType.STUDY, 8, "도서관 2층", "화이트보드,TV"),
            createFacility("세미나실 A", FacilityType.SEMINAR, 30, "본관 3층", "빔프로젝터,마이크"),
            createFacility("세미나실 B", FacilityType.SEMINAR, 50, "본관 4층", "빔프로젝터,마이크,음향시스템"),
            createFacility("대강당", FacilityType.AUDITORIUM, 200, "본관 1층", "빔프로젝터,음향시스템,무대조명"),
            createFacility("체육관", FacilityType.GYM, 100, "체육관동", "농구대,배구네트")
        );

        facilityRepository.saveAll(facilities);
        log.info("시설 초기 데이터 {} 건 생성 완료", facilities.size());
    }

    private FacilityTbl createFacility(
        String name, FacilityType type, Integer capacity,
        String location, String equipment
    ) {
        FacilityTbl facility = new FacilityTbl();
        facility.setFacilityName(name);
        facility.setFacilityType(type);
        facility.setCapacity(capacity);
        facility.setLocation(location);
        facility.setDefaultEquipment(equipment);
        facility.setIsActive(1);
        return facility;
    }
}
```

### ReservationScheduler
```java
@Component
@Slf4j
public class ReservationScheduler {

    @Autowired
    private FacilityReservationRepository reservationRepository;

    @Autowired
    private FacilityReservationLogRepository logRepository;

    /**
     * 만료된 예약 자동 완료 처리 (10분마다)
     */
    @Scheduled(cron = "0 */10 * * * *")
    @Transactional
    public void completeExpiredReservations() {
        LocalDateTime now = LocalDateTime.now();

        List<FacilityReservationTbl> expired =
            reservationRepository.findExpiredReservations(now);

        for (FacilityReservationTbl reservation : expired) {
            reservation.setStatus(ReservationStatus.COMPLETED);

            logRepository.save(FacilityReservationLog.builder()
                .reservationIdx(reservation.getReservationIdx())
                .eventType("SYSTEM_COMPLETED")
                .actorType("SYSTEM")
                .actorCode("scheduler")
                .build());
        }

        if (!expired.isEmpty()) {
            reservationRepository.saveAll(expired);
            log.info("만료된 예약 {}건 자동 완료 처리", expired.size());
        }
    }
}
```

---

## 구현 체크리스트

### Phase 1: 기반 구조 (1-2일)
- [ ] Enum 클래스 작성 (FacilityType, ReservationStatus)
- [ ] Entity 클래스 작성 (FacilityTbl, FacilityReservationTbl, FacilityReservationLog)
- [ ] Repository 인터페이스 작성
- [ ] DTO 패키지 구조 생성 및 기본 DTO 작성
- [ ] ReservationPolicyProperties 설정 클래스 작성

### Phase 2: 학생용 기능 (2-3일)
- [ ] FacilityService 구현 (목록, 상세, 가용성)
- [ ] FacilityReservationService 구현 (예약 생성, 취소, 내역)
- [ ] TimeRange 계산 로직 구현 및 테스트
- [ ] FacilityController API 구현
- [ ] FacilityReservationController API 구현

### Phase 3: 관리자 기능 (2일)
- [ ] AdminFacilityReservationService 구현
- [ ] 대기 목록 조회 (UserTbl JOIN)
- [ ] 승인/반려 로직
- [ ] 대시보드 통계 쿼리 및 DTO
- [ ] AdminFacilityReservationController API 구현

### Phase 4: 부가 기능 (1-2일)
- [ ] FCM 알림 연동 (승인/반려 시)
- [ ] FacilityDataInitializer 초기 데이터 생성
- [ ] ReservationScheduler 자동 완료 처리
- [ ] Jackson 날짜/시간 설정
- [ ] 에러 처리 및 로깅 강화

### Phase 5: 테스트 및 문서화 (2일)
- [ ] 단위 테스트 (Service 계층)
- [ ] 통합 테스트 (Controller API)
- [ ] Postman 컬렉션 작성
- [ ] API 문서 작성 (Swagger 또는 Markdown)
- [ ] 프론트엔드 연동 테스트

---

## 참고 사항

### 기존 코드 패턴 준수
- JWT 인증: `JwtUtil.validateToken()` → `extractUserId()` → `UserTbl` 조회
- Rate Limit: `@RateLimit(timeWindow, maxRequests, message)`
- 응답 형식: `ApiResponse<T>(success, message, data, errorCode)`
- 로깅: SLF4J `@Slf4j`, INFO/WARN/ERROR 레벨 구분
- 예외 처리: `GlobalExceptionHandler`에 커스텀 예외 추가

### 확장 고려사항
- 예약 대기열 (Waitlist) 기능
- 반복 예약 (매주 같은 시간)
- 시설별 운영 시간 설정
- 예약 이용 체크인/체크아웃 기능
- 통계 캐싱 (Redis)

---

**최종 업데이트**: 2025-10-06
**구현 예상 기간**: 7-10일
**우선순위**: Phase 1 → Phase 2 → Phase 3 → Phase 4 → Phase 5
