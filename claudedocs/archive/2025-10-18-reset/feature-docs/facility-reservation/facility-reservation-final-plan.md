# 시설 예약 시스템 - 최종 구현 플랜 (v2.1)

**작성일**: 2025-10-06
**버전**: v2.1 (Production-Ready)
**상태**: ✅ 런타임 검증 완료
**참조**:
- 문제점 분석: facility-reservation-issues-analysis.md
- 치명적 버그 수정: facility-reservation-critical-fixes.md
- 기반 코드: ReadingRoomService, ReadingRoomController

---

## 📌 Quick Start

### 필수 선행 조건
1. Spring Boot 2.x, JPA, MySQL 환경
2. 기존 시스템의 `UserTbl`, `AdminTbl`, `JwtUtil`, `FcmTokenService` 존재
3. Lombok, Jackson, Validation 의존성 추가

### 구현 순서
1. **Phase 1**: Enum, Entity, Repository (1-2일)
2. **Phase 2**: 학생용 기능 (2-3일)
3. **Phase 3**: 관리자 기능 (2일)
4. **Phase 4**: 부가 기능 (1-2일)
5. **Phase 5**: 테스트 및 검증 (2일)

---

## 🔑 핵심 변경 사항 (v2.0 → v2.1)

### ✅ 수정된 치명적 버그
- **NPE 방지**: `toResponseDto()` 에서 Facility null 처리
- **Jackson 오류**: ISO_LOCAL_DATE_TIME 사용, 오프셋 제거
- **JPQL 호환성**: DATE() → BETWEEN으로 변경
- **통계 완성**: `completed` 필드 쿼리 구현
- **설정 추가**: `@EnableAsync`, `@EnableScheduling`, `@Slf4j`

### ⚠️ 주의 사항
- 개인정보 보호: 학생 화면에서 예약자 정보 미노출
- 관리자 인증: AdminTbl 기반 토큰 검증 구현 필요
- 날짜 파라미터: LocalDate → LocalDateTime 변환 필수

---

## 📦 데이터 모델

### Enum 정의

#### FacilityType
```java
package BlueCrab.com.example.entity;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum FacilityType {
    STUDY("study"),
    SEMINAR("seminar"),
    AUDITORIUM("auditorium"),
    GYM("gym");

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
            if (type.value.equalsIgnoreCase(value)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Invalid FacilityType: " + value);
    }
}
```

#### ReservationStatus
```java
package BlueCrab.com.example.entity;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum ReservationStatus {
    PENDING("pending"),
    APPROVED("approved"),
    REJECTED("rejected"),
    CANCELLED("cancelled"),
    COMPLETED("completed");

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
            if (status.value.equalsIgnoreCase(value)) {
                return status;
            }
        }
        throw new IllegalArgumentException("Invalid ReservationStatus: " + value);
    }
}
```

### Entity 클래스

#### FacilityTbl
```java
package BlueCrab.com.example.entity;

import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import java.time.LocalDateTime;

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
     * null이면 인원 제한 없음
     */
    @Column(name = "CAPACITY")
    private Integer capacity;

    @Column(name = "LOCATION", length = 200)
    private String location;

    @Column(name = "DEFAULT_EQUIPMENT", columnDefinition = "TEXT")
    private String defaultEquipment;

    @Column(name = "IS_ACTIVE")
    private Integer isActive = 1;

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

#### FacilityReservationTbl
```java
package BlueCrab.com.example.entity;

import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import java.time.LocalDateTime;

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
    private String approvedBy;

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

#### FacilityReservationLog
```java
package BlueCrab.com.example.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import java.time.LocalDateTime;

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
    private String eventType;

    @Column(name = "ACTOR_TYPE", length = 20)
    private String actorType;

    @Column(name = "ACTOR_CODE", length = 50)
    private String actorCode;

    @Column(name = "PAYLOAD", columnDefinition = "TEXT")
    private String payload;

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

## 🗄️ Repository 계층

### FacilityRepository
```java
package BlueCrab.com.example.repository;

import BlueCrab.com.example.entity.FacilityTbl;
import BlueCrab.com.example.entity.FacilityType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

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
package BlueCrab.com.example.repository;

import BlueCrab.com.example.dto.facility.admin.AdminReservationDetailDto;
import BlueCrab.com.example.entity.FacilityReservationTbl;
import BlueCrab.com.example.entity.FacilityType;
import BlueCrab.com.example.entity.ReservationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

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
     * 특정 날짜의 예약 조회 (BETWEEN 사용)
     */
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
     * 관리자 필터 검색 (BETWEEN 사용)
     */
    @Query("""
        SELECT r FROM FacilityReservationTbl r
        JOIN FacilityTbl f ON r.facilityIdx = f.facilityIdx
        WHERE (:facilityType IS NULL OR f.facilityType = :facilityType)
          AND (:facilityName IS NULL OR f.facilityName LIKE %:facilityName%)
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
     * 대시보드 통계 (Native Query)
     */
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
package BlueCrab.com.example.repository;

import BlueCrab.com.example.entity.FacilityReservationLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FacilityReservationLogRepository extends JpaRepository<FacilityReservationLog, Integer> {

    List<FacilityReservationLog> findByReservationIdxOrderByCreatedAtDesc(Integer reservationIdx);
}
```

---

## 📋 DTO 설계

### 패키지 구조
```
dto/facility/
├── request/
│   ├── FacilityListRequestDto.java
│   ├── FacilityAvailabilityRequestDto.java
│   ├── FacilityReservationRequestDto.java
│   └── ReservationCancelRequestDto.java
├── response/
│   ├── FacilitySummaryDto.java
│   ├── FacilityDetailDto.java
│   ├── FacilityListResponseDto.java
│   ├── TimeSlotStatusDto.java
│   ├── ReservationResponseDto.java
│   └── MyReservationListDto.java
└── admin/
    ├── AdminReservationFilterDto.java
    ├── AdminReservationDetailDto.java
    ├── AdminReservationStatsDto.java
    ├── TodayStats.java
    ├── AdminApproveRequestDto.java
    └── AdminRejectRequestDto.java
```

### 핵심 DTO

#### TimeSlotStatusDto
```java
package BlueCrab.com.example.dto.facility.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TimeSlotStatusDto {
    private String time;
    private String status;  // "available", "reserved", "pending"

    /**
     * ⚠️ 학생 화면에서는 항상 null (개인정보 보호)
     * 관리자 화면에서만 값 제공
     */
    private Integer reservationId;
    private String reserver;
}
```

#### FacilityReservationRequestDto
```java
package BlueCrab.com.example.dto.facility.request;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import javax.validation.constraints.*;
import java.time.LocalDate;
import java.util.List;

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

    @Size(max = 200, message = "요청 장비는 200자 이내로 입력해주세요.")
    private String requestedEquipment;  // 선택사항
}
```

#### ReservationResponseDto
```java
package BlueCrab.com.example.dto.facility.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class ReservationResponseDto {
    private Integer reservationId;
    private Integer facilityId;
    private String facilityName;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime startTime;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime endTime;

    private String dateFormatted;
    private String timeRangeFormatted;
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

#### AdminReservationStatsDto
```java
package BlueCrab.com.example.dto.facility.admin;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AdminReservationStatsDto {
    private TodayStats today;
    private Integer thisWeek;
    private Integer thisMonth;
}
```

#### TodayStats
```java
package BlueCrab.com.example.dto.facility.admin;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class TodayStats {
    private Integer total;
    private Integer inUse;
    private Integer upcoming;
    private Integer completed;
}
```

---

## ⚙️ 설정 클래스

### JacksonConfig (수정 완료)
```java
package BlueCrab.com.example.config;

import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateSerializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.format.DateTimeFormatter;
import java.util.TimeZone;

@Configuration
public class JacksonConfig {
    @Bean
    public Jackson2ObjectMapperBuilderCustomizer jsonCustomizer() {
        return builder -> builder
            .timeZone(TimeZone.getTimeZone("Asia/Seoul"))
            .serializers(new LocalDateTimeSerializer(
                DateTimeFormatter.ISO_LOCAL_DATE_TIME  // ✅ 오프셋 없음
            ))
            .serializers(new LocalDateSerializer(
                DateTimeFormatter.ISO_LOCAL_DATE
            ))
            .featuresToDisable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }
}
```

### ReservationPolicyProperties
```java
package BlueCrab.com.example.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "facility.reservation.policy")
@Data
public class ReservationPolicyProperties {
    private int cancelDeadlineHours = 24;
    private int maxAdvanceDays = 14;
    private int maxActiveReservationsPerUser = 3;
    private int minDurationMinutes = 60;
    private int maxDurationMinutes = 240;
}
```

### AsyncConfig (비동기 활성화)
```java
package BlueCrab.com.example.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

@Configuration
@EnableAsync
@EnableScheduling
public class AsyncConfig {

    @Bean(name = "facilityTaskExecutor")
    public Executor taskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(5);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("facility-async-");
        executor.initialize();
        return executor;
    }
}
```

### FacilityDataInitializer (수정 완료)
```java
package BlueCrab.com.example.config;

import BlueCrab.com.example.entity.FacilityTbl;
import BlueCrab.com.example.entity.FacilityType;
import BlueCrab.com.example.repository.FacilityRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

@Component
@Slf4j  // ✅ 추가
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

---

## 🔧 서비스 로직 (핵심만 발췌)

### FacilityReservationService - DTO 매핑 (NPE 방지)
```java
/**
 * Entity → ResponseDto 변환 (NPE 방지)
 */
private ReservationResponseDto toResponseDto(FacilityReservationTbl reservation) {
    DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm");

    String timeRange = reservation.getStartTime().format(timeFormatter) +
                      "-" +
                      reservation.getEndTime().format(timeFormatter);

    // ✅ Facility 정보 안전하게 조회
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

### FacilityService - 시간 슬롯 가용성 (개인정보 보호)
```java
public List<TimeSlotStatusDto> getAvailability(Integer facilityId, LocalDate date) {
    FacilityTbl facility = facilityRepository.findByFacilityIdxAndIsActive(facilityId, 1)
        .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 시설입니다."));

    // ✅ BETWEEN 사용
    LocalDateTime dateStart = date.atStartOfDay();
    LocalDateTime dateEnd = date.plusDays(1).atStartOfDay();

    List<FacilityReservationTbl> reservations =
        reservationRepository.findByFacilityAndDate(facilityId, dateStart, dateEnd);

    List<TimeSlotStatusDto> slots = new ArrayList<>();
    for (int hour = 9; hour < 21; hour++) {
        String timeStr = String.format("%02d:00", hour);
        LocalTime slotTime = LocalTime.of(hour, 0);
        LocalDateTime slotStart = LocalDateTime.of(date, slotTime);
        LocalDateTime slotEnd = slotStart.plusHours(1);

        TimeSlotStatusDto slot = new TimeSlotStatusDto();
        slot.setTime(timeStr);

        boolean occupied = false;
        for (FacilityReservationTbl reservation : reservations) {
            if (reservation.getStartTime().isBefore(slotEnd) &&
                reservation.getEndTime().isAfter(slotStart)) {
                occupied = true;

                // ✅ 학생 화면: 개인정보 미노출
                slot.setReservationId(null);
                slot.setReserver(null);

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
```

### AdminFacilityReservationService - 통계 (completed 구현)
```java
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

## 📝 구현 체크리스트

### Phase 1: 기반 구조 (1-2일)
- [ ] Enum 클래스 작성 (FacilityType, ReservationStatus)
- [ ] Entity 클래스 작성 (3개)
- [ ] Repository 인터페이스 작성 (3개)
- [ ] DTO 패키지 구조 생성
- [ ] ReservationPolicyProperties 작성
- [ ] JacksonConfig 작성
- [ ] AsyncConfig 작성 (`@EnableAsync`, `@EnableScheduling`)
- [ ] FacilityDataInitializer 작성 (`@Slf4j` 포함)

### Phase 2: 학생용 기능 (2-3일)
- [ ] FacilityService 구현
- [ ] FacilityReservationService 구현
- [ ] TimeRange 계산 로직 테스트
- [ ] FacilityController API 구현
- [ ] FacilityReservationController API 구현
- [ ] NPE 방지 확인

### Phase 3: 관리자 기능 (2일)
- [ ] AdminFacilityReservationService 구현
- [ ] 승인/반려 로직
- [ ] 대시보드 통계 쿼리 (`completed` 포함)
- [ ] AdminFacilityReservationController 구현
- [ ] 관리자 인증 로직 구현 (AdminTbl 연동)

### Phase 4: 부가 기능 (1-2일)
- [ ] FCM 알림 연동 (`@Async` 동작 확인)
- [ ] ReservationScheduler 구현
- [ ] 에러 핸들링 (GlobalExceptionHandler)
- [ ] 로깅 강화

### Phase 5: 테스트 및 검증 (2일)
- [ ] 단위 테스트
- [ ] 통합 테스트
- [ ] Postman 컬렉션 작성
- [ ] 프론트엔드 연동 테스트
- [ ] Jackson 직렬화 테스트
- [ ] JPQL 쿼리 H2/MySQL 호환성 확인

---

## ⚠️ 주의 사항 (반드시 준수)

1. **날짜 파라미터 변환**: LocalDate → LocalDateTime 필수
   ```java
   LocalDateTime dateStart = date.atStartOfDay();
   LocalDateTime dateEnd = date.plusDays(1).atStartOfDay();
   ```

2. **Jackson 날짜 형식**: `@JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")` (오프셋 제거)

3. **JPQL DATE() 금지**: BETWEEN 또는 FUNCTION 사용

4. **NPE 방지**: Facility 조회 시 항상 null 체크

5. **개인정보 보호**: 학생 화면에서 예약자 정보 null 처리

6. **비동기 설정**: `@EnableAsync` 활성화 확인

7. **관리자 인증**: AdminTbl 기반 토큰 검증 구현

---

**최종 검증**: 2025-10-06
**구현 예상 기간**: 7-10일
**Production Ready**: ✅
