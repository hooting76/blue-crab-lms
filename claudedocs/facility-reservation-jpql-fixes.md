# 시설 예약 시스템 - JPQL/JPA 문법 오류 수정 (v2.1 → v2.2)

**작성일**: 2025-10-06
**수정 범위**: JPQL 쿼리, Native Query, DTO 생성자, 초기 데이터 로더

---

## 🔴 치명적 JPQL/JPA 오류 수정

### 1. LIKE 문법 오류

**문제**
```java
@Query("""
    SELECT r FROM FacilityReservationTbl r
    JOIN FacilityTbl f ON r.facilityIdx = f.facilityIdx
    WHERE (:facilityName IS NULL OR f.facilityName LIKE %:facilityName%)  // ❌ 문법 오류
""")
```

**오류 메시지**
```
QuerySyntaxException: unexpected token: % near line 1
```

**해결 방안**
```java
@Query("""
    SELECT r FROM FacilityReservationTbl r
    JOIN FacilityTbl f ON r.facilityIdx = f.facilityIdx
    WHERE (:facilityName IS NULL
           OR f.facilityName LIKE CONCAT('%', :facilityName, '%'))  // ✅ CONCAT 사용
    ORDER BY r.startTime DESC
""")
Page<FacilityReservationTbl> findByAdminFilter(
    @Param("facilityName") String facilityName,
    // ...
);
```

**대안: 네이티브 메서드 사용**
```java
List<FacilityTbl> findByFacilityNameContainingIgnoreCaseAndIsActive(
    String keyword, Integer isActive
);
```

---

### 2. Native Query 반환 타입 오류

**문제**
```java
@Query(value = """
    SELECT
        COUNT(CASE WHEN DATE(start_time) = :today THEN 1 END) as total_today,
        COUNT(CASE WHEN start_time <= :now AND end_time > :now
                    AND status = 'APPROVED' THEN 1 END) as in_use,
        // ...
    FROM facility_reservation_tbl
    WHERE start_time >= :today OR status IN ('PENDING', 'APPROVED')
""", nativeQuery = true)
Map<String, Object> getDashboardStats(...);  // ❌ Map 단일 반환 불가
```

**오류**
```
Spring Data JPA does not support single Map return type for native queries
```

**해결 방안 A: Projection Interface (권장)**
```java
// Repository
@Query(value = """
    SELECT
        COUNT(CASE WHEN DATE(start_time) = :today THEN 1 END) as totalToday,
        COUNT(CASE WHEN start_time <= :now AND end_time > :now
                    AND status = 'APPROVED' THEN 1 END) as inUse,
        COUNT(CASE WHEN start_time > :now AND status = 'APPROVED' THEN 1 END) as upcoming,
        COUNT(CASE WHEN DATE(start_time) = :today
                    AND (end_time < :now OR status = 'COMPLETED') THEN 1 END) as completed,
        COUNT(CASE WHEN start_time >= :weekStart THEN 1 END) as weekTotal,
        COUNT(CASE WHEN start_time >= :monthStart THEN 1 END) as monthTotal
    FROM facility_reservation_tbl
    WHERE start_time >= :today OR status IN ('PENDING', 'APPROVED')
""", nativeQuery = true)
DashboardStatsProjection getDashboardStats(
    @Param("today") LocalDate today,
    @Param("now") LocalDateTime now,
    @Param("weekStart") LocalDate weekStart,
    @Param("monthStart") LocalDate monthStart
);

// Projection Interface (Repository 패키지에 생성)
package BlueCrab.com.example.repository.projection;

public interface DashboardStatsProjection {
    Integer getTotalToday();
    Integer getInUse();
    Integer getUpcoming();
    Integer getCompleted();
    Integer getWeekTotal();
    Integer getMonthTotal();
}

// Service 사용 예시
public AdminReservationStatsDto getDashboardStats() {
    LocalDate today = LocalDate.now();
    LocalDateTime now = LocalDateTime.now();
    LocalDate weekStart = today.with(DayOfWeek.MONDAY);
    LocalDate monthStart = today.withDayOfMonth(1);

    DashboardStatsProjection stats = reservationRepository.getDashboardStats(
        today, now, weekStart, monthStart
    );

    return AdminReservationStatsDto.builder()
        .today(TodayStats.builder()
            .total(stats.getTotalToday())
            .inUse(stats.getInUse())
            .upcoming(stats.getUpcoming())
            .completed(stats.getCompleted())
            .build())
        .thisWeek(stats.getWeekTotal())
        .thisMonth(stats.getMonthTotal())
        .build();
}
```

**해결 방안 B: Object[] 배열**
```java
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
List<Object[]> getDashboardStatsRaw(
    @Param("today") LocalDate today,
    @Param("now") LocalDateTime now,
    @Param("weekStart") LocalDate weekStart,
    @Param("monthStart") LocalDate monthStart
);

// Service
public AdminReservationStatsDto getDashboardStats() {
    // ...
    List<Object[]> result = reservationRepository.getDashboardStatsRaw(
        today, now, weekStart, monthStart
    );

    if (result.isEmpty()) {
        return createEmptyStats();
    }

    Object[] row = result.get(0);
    return AdminReservationStatsDto.builder()
        .today(TodayStats.builder()
            .total(((Number) row[0]).intValue())
            .inUse(((Number) row[1]).intValue())
            .upcoming(((Number) row[2]).intValue())
            .completed(((Number) row[3]).intValue())
            .build())
        .thisWeek(((Number) row[4]).intValue())
        .thisMonth(((Number) row[5]).intValue())
        .build();
}
```

---

### 3. DTO 생성자 누락

**문제**
```java
@Query("""
    SELECT new BlueCrab.com.example.dto.facility.admin.AdminReservationDetailDto(
        r.reservationIdx,
        f.facilityName,
        // ... 12개 필드
    )
    FROM FacilityReservationTbl r
    // ...
""")
List<AdminReservationDetailDto> findPendingWithUserInfo();
```

**AdminReservationDetailDto에 매칭되는 생성자가 없으면 컴파일 에러**

**해결 방안**
```java
package BlueCrab.com.example.dto.facility.admin;

import BlueCrab.com.example.entity.FacilityType;
import BlueCrab.com.example.entity.ReservationStatus;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor  // ✅ 필수: JPQL new 생성자용
public class AdminReservationDetailDto {
    // 예약 정보
    private Integer reservationId;
    private String facilityName;
    private FacilityType facilityType;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime startTime;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime endTime;

    private ReservationStatus status;
    private String purpose;
    private Integer partySize;
    private String requestedEquipment;

    // 사용자 정보 (조인으로 가져옴)
    private String userName;
    private String userCode;
    private String userEmail;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime createdAt;

    // ✅ JPQL new 생성자 (필드 순서 정확히 일치)
    public AdminReservationDetailDto(
        Integer reservationId,
        String facilityName,
        FacilityType facilityType,
        LocalDateTime startTime,
        LocalDateTime endTime,
        ReservationStatus status,
        String purpose,
        Integer partySize,
        String requestedEquipment,
        String userName,
        String userCode,
        String userEmail,
        LocalDateTime createdAt
    ) {
        this.reservationId = reservationId;
        this.facilityName = facilityName;
        this.facilityType = facilityType;
        this.startTime = startTime;
        this.endTime = endTime;
        this.status = status;
        this.purpose = purpose;
        this.partySize = partySize;
        this.requestedEquipment = requestedEquipment;
        this.userName = userName;
        this.userCode = userCode;
        this.userEmail = userEmail;
        this.createdAt = createdAt;
    }
}
```

---

### 4. Enum 문자열 리터럴 사용 오류

**문제**
```java
@Query("""
    SELECT r FROM FacilityReservationTbl r
    WHERE r.facilityIdx = :facilityIdx
      AND r.status IN ('PENDING', 'APPROVED')  // ❌ Enum 필드에 문자열 비교
""")
```

**오류**
```
Cannot compare Enum type with String literals
```

**해결 방안**
```java
// 방법 1: Enum 상수 직접 사용 (권장)
@Query("""
    SELECT r FROM FacilityReservationTbl r
    WHERE r.facilityIdx = :facilityIdx
      AND r.status IN (BlueCrab.com.example.entity.ReservationStatus.PENDING,
                       BlueCrab.com.example.entity.ReservationStatus.APPROVED)
    ORDER BY r.startTime
""")
List<FacilityReservationTbl> findByFacilityAndDate(
    @Param("facilityIdx") Integer facilityIdx,
    @Param("dateStart") LocalDateTime dateStart,
    @Param("dateEnd") LocalDateTime dateEnd
);

// 방법 2: 파라미터로 전달 (더 권장)
@Query("""
    SELECT r FROM FacilityReservationTbl r
    WHERE r.facilityIdx = :facilityIdx
      AND r.startTime >= :dateStart
      AND r.startTime < :dateEnd
      AND r.status IN :statuses
    ORDER BY r.startTime
""")
List<FacilityReservationTbl> findByFacilityAndDate(
    @Param("facilityIdx") Integer facilityIdx,
    @Param("dateStart") LocalDateTime dateStart,
    @Param("dateEnd") LocalDateTime dateEnd,
    @Param("statuses") List<ReservationStatus> statuses
);

// Service에서 호출
List<ReservationStatus> activeStatuses = Arrays.asList(
    ReservationStatus.PENDING,
    ReservationStatus.APPROVED
);

List<FacilityReservationTbl> reservations =
    reservationRepository.findByFacilityAndDate(
        facilityId, dateStart, dateEnd, activeStatuses
    );
```

**모든 JPQL 쿼리 수정 필요**
```java
// ❌ 기존
WHERE r.status = 'PENDING'

// ✅ 수정
WHERE r.status = :status

// Service
reservationRepository.findPendingReservations(ReservationStatus.PENDING);
```

---

### 5. 초기 데이터 로더 프로필 가드

**문제**
```java
@Component
public class FacilityDataInitializer implements ApplicationRunner {
    @Override
    public void run(ApplicationArguments args) throws Exception {
        if (facilityRepository.count() > 0) {
            return;  // ⚠️ 운영 DB에도 실행됨
        }
        // 샘플 데이터 삽입
    }
}
```

**해결 방안 A: @Profile 사용 (권장)**
```java
@Component
@Profile({"dev", "local", "test"})  // ✅ 개발 환경에서만 실행
@Slf4j
public class FacilityDataInitializer implements ApplicationRunner {

    @Autowired
    private FacilityRepository facilityRepository;

    @Override
    public void run(ApplicationArguments args) throws Exception {
        if (facilityRepository.count() > 0) {
            log.info("시설 데이터가 이미 존재합니다. 초기화를 건너뜁니다.");
            return;
        }

        log.info("개발 환경 초기 데이터 생성 시작...");

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

**해결 방안 B: @ConditionalOnProperty**
```java
@Component
@ConditionalOnProperty(
    name = "facility.data.init.enabled",
    havingValue = "true",
    matchIfMissing = false
)
@Slf4j
public class FacilityDataInitializer implements ApplicationRunner {
    // ...
}
```

**application-prod.yml**
```yaml
facility:
  data:
    init:
      enabled: false  # 운영 환경에서는 비활성화
```

**application-dev.yml**
```yaml
facility:
  data:
    init:
      enabled: true  # 개발 환경에서는 활성화
```

---

## 📝 수정된 전체 Repository 코드

### FacilityReservationRepository (최종 버전)
```java
package BlueCrab.com.example.repository;

import BlueCrab.com.example.dto.facility.admin.AdminReservationDetailDto;
import BlueCrab.com.example.entity.FacilityReservationTbl;
import BlueCrab.com.example.entity.FacilityType;
import BlueCrab.com.example.entity.ReservationStatus;
import BlueCrab.com.example.repository.projection.DashboardStatsProjection;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface FacilityReservationRepository extends JpaRepository<FacilityReservationTbl, Integer> {

    /**
     * 시간대 중복 체크
     */
    @Query("""
        SELECT r FROM FacilityReservationTbl r
        WHERE r.facilityIdx = :facilityIdx
          AND r.status IN :statuses
          AND r.startTime < :endTime
          AND r.endTime > :startTime
    """)
    List<FacilityReservationTbl> findConflictingReservations(
        @Param("facilityIdx") Integer facilityIdx,
        @Param("startTime") LocalDateTime startTime,
        @Param("endTime") LocalDateTime endTime,
        @Param("statuses") List<ReservationStatus> statuses
    );

    /**
     * 특정 날짜의 예약 조회
     */
    @Query("""
        SELECT r FROM FacilityReservationTbl r
        WHERE r.facilityIdx = :facilityIdx
          AND r.startTime >= :dateStart
          AND r.startTime < :dateEnd
          AND r.status IN :statuses
        ORDER BY r.startTime
    """)
    List<FacilityReservationTbl> findByFacilityAndDate(
        @Param("facilityIdx") Integer facilityIdx,
        @Param("dateStart") LocalDateTime dateStart,
        @Param("dateEnd") LocalDateTime dateEnd,
        @Param("statuses") List<ReservationStatus> statuses
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
        WHERE r.status = :status
        ORDER BY r.createdAt ASC
    """)
    List<AdminReservationDetailDto> findPendingWithUserInfo(
        @Param("status") ReservationStatus status
    );

    /**
     * 관리자 필터 검색
     */
    @Query("""
        SELECT r FROM FacilityReservationTbl r
        JOIN FacilityTbl f ON r.facilityIdx = f.facilityIdx
        WHERE (:facilityType IS NULL OR f.facilityType = :facilityType)
          AND (:facilityName IS NULL
               OR f.facilityName LIKE CONCAT('%', :facilityName, '%'))
          AND (:status IS NULL OR r.status = :status)
          AND (:dateFromStart IS NULL OR r.startTime >= :dateFromStart)
          AND (:dateToEnd IS NULL OR r.startTime < :dateToEnd)
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

    /**
     * 대시보드 통계 (Projection 사용)
     */
    @Query(value = """
        SELECT
            COUNT(CASE WHEN DATE(start_time) = :today THEN 1 END) as totalToday,
            COUNT(CASE WHEN start_time <= :now AND end_time > :now
                        AND status = 'APPROVED' THEN 1 END) as inUse,
            COUNT(CASE WHEN start_time > :now AND status = 'APPROVED' THEN 1 END) as upcoming,
            COUNT(CASE WHEN DATE(start_time) = :today
                        AND (end_time < :now OR status = 'COMPLETED') THEN 1 END) as completed,
            COUNT(CASE WHEN start_time >= :weekStart THEN 1 END) as weekTotal,
            COUNT(CASE WHEN start_time >= :monthStart THEN 1 END) as monthTotal
        FROM facility_reservation_tbl
        WHERE start_time >= :today OR status IN ('PENDING', 'APPROVED')
    """, nativeQuery = true)
    DashboardStatsProjection getDashboardStats(
        @Param("today") LocalDate today,
        @Param("now") LocalDateTime now,
        @Param("weekStart") LocalDate weekStart,
        @Param("monthStart") LocalDate monthStart
    );

    /**
     * 만료된 예약 조회
     */
    @Query("""
        SELECT r FROM FacilityReservationTbl r
        WHERE r.status = :status
          AND r.endTime < :currentTime
    """)
    List<FacilityReservationTbl> findExpiredReservations(
        @Param("status") ReservationStatus status,
        @Param("currentTime") LocalDateTime currentTime
    );
}
```

### DashboardStatsProjection
```java
package BlueCrab.com.example.repository.projection;

public interface DashboardStatsProjection {
    Integer getTotalToday();
    Integer getInUse();
    Integer getUpcoming();
    Integer getCompleted();
    Integer getWeekTotal();
    Integer getMonthTotal();
}
```

---

## 📋 수정된 Service 호출 코드

### FacilityService
```java
public List<TimeSlotStatusDto> getAvailability(Integer facilityId, LocalDate date) {
    // ...
    LocalDateTime dateStart = date.atStartOfDay();
    LocalDateTime dateEnd = date.plusDays(1).atStartOfDay();

    List<ReservationStatus> activeStatuses = Arrays.asList(
        ReservationStatus.PENDING,
        ReservationStatus.APPROVED
    );

    List<FacilityReservationTbl> reservations =
        reservationRepository.findByFacilityAndDate(
            facilityId, dateStart, dateEnd, activeStatuses
        );
    // ...
}
```

### FacilityReservationService
```java
private void validateReservation(...) {
    // ...
    List<ReservationStatus> conflictStatuses = Arrays.asList(
        ReservationStatus.PENDING,
        ReservationStatus.APPROVED
    );

    List<FacilityReservationTbl> conflicts =
        reservationRepository.findConflictingReservations(
            facility.getFacilityIdx(),
            timeRange.getStartTime(),
            timeRange.getEndTime(),
            conflictStatuses
        );
    // ...
}
```

### AdminFacilityReservationService
```java
public List<AdminReservationDetailDto> getPendingReservations() {
    return reservationRepository.findPendingWithUserInfo(
        ReservationStatus.PENDING
    );
}

public AdminReservationStatsDto getDashboardStats() {
    LocalDate today = LocalDate.now();
    LocalDateTime now = LocalDateTime.now();
    LocalDate weekStart = today.with(DayOfWeek.MONDAY);
    LocalDate monthStart = today.withDayOfMonth(1);

    DashboardStatsProjection stats = reservationRepository.getDashboardStats(
        today, now, weekStart, monthStart
    );

    return AdminReservationStatsDto.builder()
        .today(TodayStats.builder()
            .total(stats.getTotalToday())
            .inUse(stats.getInUse())
            .upcoming(stats.getUpcoming())
            .completed(stats.getCompleted())
            .build())
        .thisWeek(stats.getWeekTotal())
        .thisMonth(stats.getMonthTotal())
        .build();
}
```

### ReservationScheduler
```java
@Scheduled(cron = "0 */10 * * * *")
@Transactional
public void completeExpiredReservations() {
    LocalDateTime now = LocalDateTime.now();

    List<FacilityReservationTbl> expired =
        reservationRepository.findExpiredReservations(
            ReservationStatus.APPROVED,
            now
        );
    // ...
}
```

---

## ✅ 수정 체크리스트

- [x] LIKE 문법: `CONCAT('%', :param, '%')` 사용
- [x] Native Query: Projection Interface 사용
- [x] DTO 생성자: `@AllArgsConstructor` 추가
- [x] Enum 비교: 파라미터로 전달
- [x] 초기 데이터: `@Profile` 가드 추가
- [x] 모든 JPQL 쿼리 Enum 파라미터 변경
- [x] Service 호출 코드 업데이트

---

**수정 완료**: 2025-10-06
**다음 단계**: final-plan v2.2에 반영
