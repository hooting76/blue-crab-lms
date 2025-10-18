# 시설 예약 시스템 - 치명적 버그 수정 사항 (v2.0 → v2.1)

**작성일**: 2025-10-06
**수정 범위**: final-plan v2.0의 런타임 오류 및 구현 불가 이슈 해결

---

## 🔴 치명적 버그 수정

### 1. NPE 발생: `FacilityReservationService.toSimpleDto()`

**문제**
```java
private ReservationResponseDto toSimpleDto(FacilityReservationTbl reservation) {
    // TODO: FacilityTbl 조회 (캐시 또는 JOIN)
    return toResponseDto(reservation, null);  // ❌ NPE 발생!
}

private ReservationResponseDto toResponseDto(
    FacilityReservationTbl reservation,
    FacilityTbl facility  // null인 상태로 전달됨
) {
    return ReservationResponseDto.builder()
        .facilityId(facility.getFacilityIdx())  // ❌ NullPointerException!
        // ...
}
```

**해결 방안 A: Repository에서 Facility JOIN**
```java
// FacilityReservationRepository.java
@Query("""
    SELECT r FROM FacilityReservationTbl r
    JOIN FETCH FacilityTbl f ON r.facilityIdx = f.facilityIdx
    WHERE r.userCode = :userCode
      AND r.status IN :statuses
    ORDER BY r.startTime DESC
""")
List<FacilityReservationTbl> findByUserCodeWithFacility(
    @Param("userCode") String userCode,
    @Param("statuses") List<ReservationStatus> statuses
);

// FacilityReservationService.java
public MyReservationListDto getMyReservations(String userCode) {
    List<ReservationStatus> ongoingStatuses = Arrays.asList(
        ReservationStatus.PENDING, ReservationStatus.APPROVED
    );

    List<ReservationStatus> completedStatuses = Arrays.asList(
        ReservationStatus.COMPLETED, ReservationStatus.REJECTED, ReservationStatus.CANCELLED
    );

    List<FacilityReservationTbl> ongoing =
        reservationRepository.findByUserCodeWithFacility(userCode, ongoingStatuses);

    List<FacilityReservationTbl> completed =
        reservationRepository.findByUserCodeWithFacility(userCode, completedStatuses);

    return MyReservationListDto.builder()
        .ongoing(ongoing.stream()
            .map(r -> toResponseDto(r, getFacility(r.getFacilityIdx())))
            .collect(Collectors.toList()))
        .completed(completed.stream()
            .map(r -> toResponseDto(r, getFacility(r.getFacilityIdx())))
            .collect(Collectors.toList()))
        .build();
}

private FacilityTbl getFacility(Integer facilityIdx) {
    return facilityRepository.findById(facilityIdx)
        .orElse(null);  // 시설 삭제된 경우 대비
}
```

**해결 방안 B: DTO 매핑 분리 (권장)**
```java
private ReservationResponseDto toResponseDto(FacilityReservationTbl reservation) {
    DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm");

    String timeRange = reservation.getStartTime().format(timeFormatter) +
                      "-" +
                      reservation.getEndTime().format(timeFormatter);

    // Facility 정보는 별도 조회
    String facilityName = "시설 정보 없음";
    Integer facilityId = reservation.getFacilityIdx();

    if (facilityId != null) {
        FacilityTbl facility = facilityRepository.findById(facilityId).orElse(null);
        if (facility != null) {
            facilityName = facility.getFacilityName();
        }
    }

    return ReservationResponseDto.builder()
        .reservationId(reservation.getReservationIdx())
        .facilityId(facilityId)
        .facilityName(facilityName)
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
```

---

### 2. Jackson 날짜 직렬화 오류

**문제**
```java
// JacksonConfig.java
@Bean
public Jackson2ObjectMapperBuilderCustomizer jsonCustomizer() {
    return builder -> builder
        .timeZone(TimeZone.getTimeZone("Asia/Seoul"))
        .serializers(new LocalDateTimeSerializer(
            DateTimeFormatter.ISO_OFFSET_DATE_TIME  // ❌ LocalDateTime은 오프셋 없음!
        ));
}

// ReservationResponseDto.java
@JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ssXXX")  // ❌ XXX는 오프셋 필요
private LocalDateTime startTime;
```

**오류 메시지**
```
UnsupportedTemporalTypeException: Unsupported field: OffsetSeconds
```

**해결 방안**
```java
// JacksonConfig.java
@Configuration
public class JacksonConfig {
    @Bean
    public Jackson2ObjectMapperBuilderCustomizer jsonCustomizer() {
        return builder -> builder
            .timeZone(TimeZone.getTimeZone("Asia/Seoul"))
            .serializers(new LocalDateTimeSerializer(
                DateTimeFormatter.ISO_LOCAL_DATE_TIME  // ✅ "2025-10-06T14:30:00"
            ))
            .serializers(new LocalDateSerializer(
                DateTimeFormatter.ISO_LOCAL_DATE
            ))
            .featuresToDisable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }
}

// ReservationResponseDto.java
@Data
@Builder
public class ReservationResponseDto {
    private Integer reservationId;
    private Integer facilityId;
    private String facilityName;

    /**
     * ISO 8601 로컬 날짜/시간 (타임존 정보 없음)
     * 예: "2025-10-06T14:30:00"
     */
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")  // ✅ 오프셋 제거
    private LocalDateTime startTime;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime endTime;

    /**
     * 표시용 포맷된 문자열
     */
    private String dateFormatted;      // "2025-10-06"
    private String timeRangeFormatted; // "14:30-16:30"

    private String status;
    private Integer partySize;
    private String purpose;
    private String requestedEquipment;
    private String adminNote;
    private String rejectionReason;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime createdAt;
}
```

**대안: ZonedDateTime 사용 (타임존 포함 필요 시)**
```java
// Entity
@Column(name = "START_TIME", nullable = false, columnDefinition = "DATETIME")
private LocalDateTime startTime;  // DB는 그대로

// DTO
@JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ssXXX", timezone = "Asia/Seoul")
private ZonedDateTime startTime;

// 변환 로직
ZonedDateTime zonedStart = reservation.getStartTime()
    .atZone(ZoneId.of("Asia/Seoul"));
```

---

### 3. JPQL `DATE()` 함수 호환성 문제

**문제**
```java
@Query("""
    SELECT r FROM FacilityReservationTbl r
    WHERE r.facilityIdx = :facilityIdx
      AND DATE(r.startTime) = :date  // ❌ JPQL 표준 함수 아님
      AND r.status IN ('PENDING', 'APPROVED')
    ORDER BY r.startTime
""")
List<FacilityReservationTbl> findByFacilityAndDate(
    @Param("facilityIdx") Integer facilityIdx,
    @Param("date") LocalDate date
);
```

**MySQL에서는 동작하지만 H2/PostgreSQL 등에서 실패**

**해결 방안 A: BETWEEN 사용 (권장)**
```java
@Query("""
    SELECT r FROM FacilityReservationTbl r
    WHERE r.facilityIdx = :facilityIdx
      AND r.startTime >= :dateStart
      AND r.startTime < :dateEnd
      AND r.status IN ('PENDING', 'APPROVED')
    ORDER BY r.startTime
""")
List<FacilityReservationTbl> findByFacilityAndDate(
    @Param("facilityIdx") Integer facilityIdx,
    @Param("dateStart") LocalDateTime dateStart,
    @Param("dateEnd") LocalDateTime dateEnd
);

// Service에서 호출
LocalDate targetDate = request.getDate();
LocalDateTime dateStart = targetDate.atStartOfDay();
LocalDateTime dateEnd = targetDate.plusDays(1).atStartOfDay();

List<FacilityReservationTbl> reservations =
    reservationRepository.findByFacilityAndDate(facilityId, dateStart, dateEnd);
```

**해결 방안 B: FUNCTION 사용**
```java
@Query("""
    SELECT r FROM FacilityReservationTbl r
    WHERE r.facilityIdx = :facilityIdx
      AND FUNCTION('DATE', r.startTime) = :date
      AND r.status IN ('PENDING', 'APPROVED')
    ORDER BY r.startTime
""")
```

**관리자 필터 쿼리도 동일 수정**
```java
@Query("""
    SELECT r FROM FacilityReservationTbl r
    JOIN FacilityTbl f ON r.facilityIdx = f.facilityIdx
    WHERE (:facilityType IS NULL OR f.facilityType = :facilityType)
      AND (:facilityName IS NULL OR f.facilityName LIKE %:facilityName%)
      AND (:status IS NULL OR r.status = :status)
      AND (:dateFrom IS NULL OR r.startTime >= :dateFromStart)
      AND (:dateTo IS NULL OR r.startTime < :dateToEnd)
    ORDER BY r.startTime DESC
""")
Page<FacilityReservationTbl> findByAdminFilter(
    @Param("facilityType") FacilityType facilityType,
    @Param("facilityName") String facilityName,
    @Param("status") ReservationStatus status,
    @Param("dateFromStart") LocalDateTime dateFromStart,
    @Param("dateToEnd") LocalDateTime dateToEnd,
    Pageable pageable
);

// Service에서 LocalDate → LocalDateTime 변환
LocalDateTime dateFromStart = dateFrom != null ? dateFrom.atStartOfDay() : null;
LocalDateTime dateToEnd = dateTo != null ? dateTo.plusDays(1).atStartOfDay() : null;
```

---

### 4. 관리자 통계 `completed` 미구현

**문제**
```java
return AdminReservationStatsDto.builder()
    .today(TodayStats.builder()
        .total(((Number) raw.get("total_today")).intValue())
        .inUse(((Number) raw.get("in_use")).intValue())
        .upcoming(((Number) raw.get("upcoming")).intValue())
        .completed(0)  // ❌ TODO: 계산 로직 추가
        .build())
    // ...
```

**해결 방안**
```java
// FacilityReservationRepository.java
@Query(value = """
    SELECT
        COUNT(CASE WHEN DATE(start_time) = :today THEN 1 END) as total_today,
        COUNT(CASE WHEN start_time <= :now AND end_time > :now
                    AND status = 'APPROVED' THEN 1 END) as in_use,
        COUNT(CASE WHEN start_time > :now AND status = 'APPROVED' THEN 1 END) as upcoming,
        COUNT(CASE WHEN DATE(start_time) = :today
                    AND (end_time < :now OR status = 'COMPLETED') THEN 1 END) as completed,
        COUNT(CASE WHEN start_time >= :weekStart THEN 1 END) as week_total,
        COUNT(CASE WHEN start_time >= :monthStart THEN 1 END) as month_total
    FROM facility_reservation_tbl
    WHERE start_time >= :today OR status IN ('PENDING', 'APPROVED')
""", nativeQuery = true)
Map<String, Object> getDashboardStats(
    @Param("today") LocalDate today,
    @Param("now") LocalDateTime now,
    @Param("weekStart") LocalDate weekStart,
    @Param("monthStart") LocalDate monthStart
);

// AdminFacilityReservationService.java
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
            .completed(((Number) raw.get("completed")).intValue())  // ✅ 구현 완료
            .build())
        .thisWeek(((Number) raw.get("week_total")).intValue())
        .thisMonth(((Number) raw.get("month_total")).intValue())
        .build();
}
```

---

### 5. `FacilityDataInitializer` 로거 누락

**문제**
```java
@Component
public class FacilityDataInitializer implements ApplicationRunner {
    // ...
    log.info("시설 초기 데이터 {} 건 생성 완료", facilities.size());  // ❌ log 변수 없음
}
```

**해결 방안**
```java
@Component
@Slf4j  // ✅ Lombok 어노테이션 추가
public class FacilityDataInitializer implements ApplicationRunner {

    @Autowired
    private FacilityRepository facilityRepository;

    @Override
    public void run(ApplicationArguments args) throws Exception {
        if (facilityRepository.count() > 0) {
            log.info("시설 데이터가 이미 존재합니다. 초기화를 건너뜁니다.");
            return;
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

---

### 6. 비동기 알림 설정 누락

**문제**
```java
@Async  // ❌ @EnableAsync 설정 없으면 동작 안 함
private void sendApprovalNotification(FacilityReservationTbl reservation) {
    // ...
}
```

**해결 방안**
```java
// Application.java 또는 별도 Config 클래스
@SpringBootApplication
@EnableAsync  // ✅ 비동기 활성화
@EnableScheduling  // ✅ 스케줄러 활성화
public class BlueCrabApplication {
    public static void main(String[] args) {
        SpringApplication.run(BlueCrabApplication.class, args);
    }
}

// 또는 별도 Config
@Configuration
@EnableAsync
@EnableScheduling
public class AsyncConfig {
    @Bean
    public TaskExecutor taskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(5);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("async-");
        executor.initialize();
        return executor;
    }
}
```

---

### 7. 관리자 인증 로직 TODO 명확화

**문제**
```java
private String getAdminCodeFromToken(HttpServletRequest request) {
    // TODO: AdminTbl 기반 인증 로직 구현
    return "admin001";  // ❌ 더미 값
}
```

**해결 방안**
```java
@RestController
@RequestMapping("/api/admin/facility-reservations")
@Slf4j
public class AdminFacilityReservationController {

    @Autowired
    private AdminFacilityReservationService adminService;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private AdminTblRepository adminRepository;  // ✅ 추가 필요

    /**
     * JWT 토큰에서 관리자 코드 추출
     *
     * ⚠️ 구현 필요 사항:
     * 1. AdminController의 JWT 발급 로직 확인
     * 2. AdminTbl의 adminCode 필드 확인
     * 3. 토큰에 adminId 저장 여부 확인
     * 4. AdminInterceptor 사용 여부 확인
     */
    private String getAdminCodeFromToken(HttpServletRequest request) {
        String token = extractToken(request);

        if (!jwtUtil.validateToken(token)) {
            throw new UnauthorizedException("관리자 인증이 필요합니다.");
        }

        // 방법 1: 토큰에 adminId가 있는 경우
        Integer adminId = jwtUtil.extractUserId(token);  // 또는 extractAdminId()
        AdminTbl admin = adminRepository.findById(adminId)
            .orElseThrow(() -> new UnauthorizedException("존재하지 않는 관리자입니다."));
        return admin.getAdminCode();

        // 방법 2: AdminInterceptor 사용하는 경우
        // return (String) request.getAttribute("adminCode");
    }

    private String extractToken(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        throw new UnauthorizedException("인증 토큰이 없습니다.");
    }
}
```

---

### 8. 검증 어노테이션 보강

**문제**
```java
@Data
public class FacilityReservationRequestDto {
    @NotNull
    private Integer facilityId;

    @NotNull
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate date;

    @NotEmpty
    private List<String> timeSlots;

    @NotNull
    @Min(1)
    private Integer partySize;

    @NotBlank
    @Size(max = 500)
    private String purpose;

    @Size(max = 200)  // ❌ 필수 입력인데 @NotBlank 없음
    private String requestedEquipment;
}
```

**해결 방안**
```java
@Data
public class FacilityReservationRequestDto {
    @NotNull(message = "시설 ID는 필수입니다.")
    private Integer facilityId;

    @NotNull(message = "예약 날짜는 필수입니다.")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate date;

    @NotEmpty(message = "최소 1개 이상의 시간을 선택해야 합니다.")
    @Size(min = 1, max = 12, message = "최대 12시간까지 선택 가능합니다.")
    private List<String> timeSlots;

    @NotNull(message = "사용 인원은 필수입니다.")
    @Min(value = 1, message = "최소 1명 이상이어야 합니다.")
    private Integer partySize;

    @NotBlank(message = "사용 목적은 필수입니다.")
    @Size(max = 500, message = "사용 목적은 500자 이내로 입력해주세요.")
    private String purpose;

    /**
     * 요청 장비 (선택사항)
     * 프론트엔드에서 선택하지 않으면 null 또는 빈 문자열
     */
    @Size(max = 200, message = "요청 장비는 200자 이내로 입력해주세요.")
    private String requestedEquipment;  // ✅ 선택 사항으로 명시
}
```

---

### 9. TimeSlotStatusDto 개인정보 정책 명확화

**문제**
```java
public List<TimeSlotStatusDto> getAvailability(Integer facilityId, LocalDate date) {
    // ...
    slot.setReservationId(reservation.getReservationIdx());
    // TODO: 사용자 이름 조회 (UserTbl JOIN)  // ❌ 개인정보 노출 문제
}
```

**해결 방안**
```java
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TimeSlotStatusDto {
    /**
     * 시간 슬롯 (HH:mm 형식)
     */
    private String time;

    /**
     * 슬롯 상태
     * - "available": 예약 가능
     * - "reserved": 이미 예약됨 (APPROVED)
     * - "pending": 승인 대기 중 (PENDING)
     */
    private String status;

    /**
     * 예약 ID (관리자 전용)
     * ⚠️ 학생 화면에서는 항상 null 반환 (개인정보 보호)
     */
    private Integer reservationId;

    /**
     * 예약자 이름 (관리자 전용)
     * ⚠️ 학생 화면에서는 항상 null 반환 (개인정보 보호)
     */
    private String reserver;
}

// FacilityService.java - 학생용
public List<TimeSlotStatusDto> getAvailability(Integer facilityId, LocalDate date) {
    // ...
    for (FacilityReservationTbl reservation : reservations) {
        if (reservation.getStartTime().isBefore(slotEnd) &&
            reservation.getEndTime().isAfter(slotStart)) {
            occupied = true;

            // ✅ 학생 화면: 개인정보 노출 안 함
            slot.setReservationId(null);  // 보안상 미제공
            slot.setReserver(null);       // 보안상 미제공

            if (reservation.getStatus() == ReservationStatus.APPROVED) {
                slot.setStatus("reserved");
            } else {
                slot.setStatus("pending");
            }
            break;
        }
    }
    // ...
}

// AdminFacilityService.java - 관리자용 (별도 구현)
public List<AdminTimeSlotStatusDto> getAvailabilityForAdmin(
    Integer facilityId, LocalDate date
) {
    // ...
    // ✅ 관리자 화면: 예약자 정보 포함
    slot.setReservationId(reservation.getReservationIdx());
    slot.setReserver(getUserName(reservation.getUserCode()));  // UserTbl 조회
    // ...
}

private String getUserName(String userCode) {
    return userRepository.findByUserCode(userCode)
        .map(UserTbl::getUserName)
        .orElse("알 수 없음");
}
```

---

## 📝 수정된 주요 코드 요약

### JacksonConfig (최종 버전)
```java
@Configuration
public class JacksonConfig {
    @Bean
    public Jackson2ObjectMapperBuilderCustomizer jsonCustomizer() {
        return builder -> builder
            .timeZone(TimeZone.getTimeZone("Asia/Seoul"))
            .serializers(new LocalDateTimeSerializer(
                DateTimeFormatter.ISO_LOCAL_DATE_TIME
            ))
            .serializers(new LocalDateSerializer(
                DateTimeFormatter.ISO_LOCAL_DATE
            ))
            .featuresToDisable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }
}
```

### Repository 쿼리 (BETWEEN 사용)
```java
@Query("""
    SELECT r FROM FacilityReservationTbl r
    WHERE r.facilityIdx = :facilityIdx
      AND r.startTime >= :dateStart
      AND r.startTime < :dateEnd
      AND r.status IN ('PENDING', 'APPROVED')
    ORDER BY r.startTime
""")
List<FacilityReservationTbl> findByFacilityAndDate(
    @Param("facilityIdx") Integer facilityIdx,
    @Param("dateStart") LocalDateTime dateStart,
    @Param("dateEnd") LocalDateTime dateEnd
);
```

### DTO 매핑 (NPE 방지)
```java
private ReservationResponseDto toResponseDto(FacilityReservationTbl reservation) {
    String facilityName = "시설 정보 없음";
    if (reservation.getFacilityIdx() != null) {
        FacilityTbl facility = facilityRepository.findById(reservation.getFacilityIdx())
            .orElse(null);
        if (facility != null) {
            facilityName = facility.getFacilityName();
        }
    }
    // ... (나머지 매핑)
}
```

### 비동기/스케줄 활성화
```java
@SpringBootApplication
@EnableAsync
@EnableScheduling
public class BlueCrabApplication {
    public static void main(String[] args) {
        SpringApplication.run(BlueCrabApplication.class, args);
    }
}
```

---

## ✅ 체크리스트

- [x] NPE 발생 지점 수정 (toSimpleDto)
- [x] Jackson 날짜 직렬화 오류 수정
- [x] JPQL DATE() 함수 → BETWEEN 변경
- [x] 관리자 통계 completed 구현
- [x] 로거 어노테이션 추가 (@Slf4j)
- [x] 비동기 설정 명시 (@EnableAsync)
- [x] 관리자 인증 로직 가이드 추가
- [x] DTO 검증 어노테이션 보강
- [x] 개인정보 보호 정책 명시

---

**수정 완료**: 2025-10-06
**다음 단계**: final-plan v2.1에 반영
