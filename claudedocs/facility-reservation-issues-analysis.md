# 시설 예약 시스템 - 문제점 상세 분석 및 해결 방안

**작성일**: 2025-10-06
**분석 대상**: facility-reservation-plan.md, facility-reservation-backend-blueprint.md
**문제 제기**: 프론트엔드 UI 요구사항과 백엔드 플랜 간 불일치

---

## 🔴 P0: API 계약 불일치 (즉시 해결 필수)

### 1. 시설 유형 값 대소문자 불일치

**문제**
- 백엔드 플랜: `STUDY`, `SEMINAR`, `AUDITORIUM`, `GYM` (대문자 Enum)
- 프론트엔드: `study`, `seminar`, `auditorium`, `gym` (소문자 문자열)

**영향**
- API 요청/응답 시 값 불일치로 필터링 실패
- 프론트 → 백엔드 요청 시 빈 결과 반환
- 백엔드 → 프론트 응답 시 클라이언트 매핑 에러

**해결 방안**
```java
// Enum 정의 시 JSON 직렬화 값 지정
public enum FacilityType {
    @JsonProperty("study")
    STUDY,

    @JsonProperty("seminar")
    SEMINAR,

    @JsonProperty("auditorium")
    AUDITORIUM,

    @JsonProperty("gym")
    GYM;

    // Jackson 직렬화/역직렬화 지원
    @JsonValue
    public String toValue() {
        return name().toLowerCase();
    }

    @JsonCreator
    public static FacilityType fromValue(String value) {
        return valueOf(value.toUpperCase());
    }
}
```

**적용 위치**
- `FacilityTbl.facilityType` 필드
- `FacilityListRequestDto.facilityType` 필드
- `AdminReservationFilterRequestDto.facilityType` 필드

---

### 2. 예약 상태 값 대소문자 불일치

**문제**
- 백엔드 플랜: `PENDING`, `APPROVED`, `REJECTED`, `CANCELLED`, `COMPLETED`
- 프론트엔드: `pending`, `approved`, `rejected`, `cancelled`, `completed`

**해결 방안**
```java
public enum ReservationStatus {
    @JsonProperty("pending")
    PENDING,

    @JsonProperty("approved")
    APPROVED,

    @JsonProperty("rejected")
    REJECTED,

    @JsonProperty("cancelled")
    CANCELLED,

    @JsonProperty("completed")
    COMPLETED;

    @JsonValue
    public String toValue() {
        return name().toLowerCase();
    }

    @JsonCreator
    public static ReservationStatus fromValue(String value) {
        return valueOf(value.toUpperCase());
    }
}
```

---

### 3. 시간 슬롯 응답 구조 미정의

**문제**
- 플랜에 "TimeSlotStatusDto" 언급만 있고 구체적인 필드 명세 없음
- 프론트엔드는 `{ time: "09:00", status: "available" }` 형식 기대

**해결 방안**
```java
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TimeSlotStatusDto {
    /**
     * 시간 슬롯 (HH:mm 형식)
     * 예: "09:00", "10:00", "11:00"
     */
    private String time;

    /**
     * 슬롯 상태
     * - "available": 예약 가능
     * - "reserved": 이미 예약됨 (APPROVED)
     * - "pending": 승인 대기 중 (PENDING)
     * - "selected": 사용자가 선택한 슬롯 (클라이언트 전용)
     */
    private String status;

    /**
     * 예약 ID (status가 reserved 또는 pending일 때만)
     */
    private Integer reservationId;

    /**
     * 예약자 이름 (status가 reserved 또는 pending일 때만)
     */
    private String reserver;
}
```

**API 응답 예시**
```json
{
  "success": true,
  "data": {
    "date": "2025-10-06",
    "slots": [
      { "time": "09:00", "status": "available" },
      { "time": "10:00", "status": "reserved", "reservationId": 123, "reserver": "홍길동" },
      { "time": "11:00", "status": "pending", "reservationId": 124, "reserver": "김철수" },
      { "time": "12:00", "status": "available" }
    ]
  }
}
```

---

### 4. 시간 문자열 형식 불명확

**문제**
- 백엔드는 `LocalDateTime` 사용하지만 JSON 직렬화 형식 미지정
- 프론트엔드는 `YYYY-MM-DD`, `HH:MM-HH:MM` 텍스트 표시 필요

**해결 방안**
```java
@Data
public class ReservationResponseDto {
    private Integer reservationId;

    /**
     * ISO 8601 형식 (서버-클라이언트 간 전송)
     * 예: "2025-10-06T14:30:00+09:00"
     */
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ssXXX")
    private LocalDateTime startTime;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ssXXX")
    private LocalDateTime endTime;

    /**
     * 표시용 포맷된 문자열 (클라이언트 편의)
     */
    private String dateFormatted;      // "2025-10-06"
    private String timeRangeFormatted; // "14:30-16:30"
}
```

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

## 🟡 P1: 핵심 기능 명세 부족

### 5. 수용 인원 필드 의미 불명확

**문제**
- 프론트엔드는 `capacity * 50` 계산식 사용 중
- `capacity`가 실제 인원수인지, 단위(unit) 개수인지 불명확

**해결 방안**
```java
@Entity
@Table(name = "FACILITY_TBL")
public class FacilityTbl {
    // ...

    /**
     * 최대 수용 인원 (실제 사람 수)
     * 예: 50명, 100명, 200명
     * null이면 인원 제한 없음 (장비류)
     */
    @Column(name = "CAPACITY")
    private Integer capacity;

    // capacityUnit 필드 제거
    // 프론트엔드 계산식(capacity * 50) 제거 → 백엔드가 실제 값 제공
}
```

**DTO 매핑**
```java
public FacilityDto toDto(FacilityTbl entity) {
    return FacilityDto.builder()
        .facilityId(entity.getFacilityIdx())
        .facilityName(entity.getFacilityName())
        .capacity(entity.getCapacity()) // 실제 수용 인원 그대로 전달
        .build();
}
```

---

### 6. 슬롯 다중 선택 처리 로직 미정의

**문제**
- 프론트엔드는 `["09:00", "10:00", "11:00"]` 배열 전송
- 백엔드에서 `startTime`/`endTime` 계산 로직 설명 없음

**해결 방안**
```java
@Service
public class FacilityReservationService {

    /**
     * 다중 슬롯을 시작/종료 시간으로 변환
     * @param date 예약 날짜
     * @param selectedSlots 선택된 시간 슬롯 배열 (예: ["09:00", "10:00", "11:00"])
     * @return 시작/종료 시간
     */
    public TimeRange calculateTimeRange(LocalDate date, List<String> selectedSlots) {
        if (selectedSlots == null || selectedSlots.isEmpty()) {
            throw new IllegalArgumentException("최소 1개 이상의 시간을 선택해야 합니다.");
        }

        // 1. 슬롯을 LocalTime으로 변환 후 정렬
        List<LocalTime> times = selectedSlots.stream()
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
        LocalTime endTime = times.get(times.size() - 1).plusHours(1); // 마지막 슬롯 + 1시간

        return new TimeRange(
            LocalDateTime.of(date, startTime),
            LocalDateTime.of(date, endTime)
        );
    }
}

@Data
@AllArgsConstructor
public class TimeRange {
    private LocalDateTime startTime;
    private LocalDateTime endTime;
}
```

**예시**
```
입력: date=2025-10-06, slots=["09:00", "10:00", "11:00"]
출력:
  startTime = 2025-10-06T09:00:00
  endTime   = 2025-10-06T12:00:00
  duration  = 3시간
```

---

### 7. 관리자 통계 필드 미정의

**문제**
- 대시보드 카드에 필요한 통계 필드가 플랜에 명시되지 않음
- 프론트엔드는 오늘 전체/이용중/예정, 주간/월간 건수 표시

**해결 방안**
```java
@Data
@Builder
public class AdminReservationStatsDto {
    /**
     * 오늘 통계
     */
    private TodayStats today;

    /**
     * 주간 총 예약 건수 (월요일~일요일)
     */
    private Integer thisWeek;

    /**
     * 월간 총 예약 건수
     */
    private Integer thisMonth;
}

@Data
@Builder
public class TodayStats {
    /**
     * 오늘 전체 예약 건수 (모든 상태 포함)
     */
    private Integer total;

    /**
     * 현재 이용 중 (startTime <= now < endTime & status=APPROVED)
     */
    private Integer inUse;

    /**
     * 이용 예정 (startTime > now & status=APPROVED)
     */
    private Integer upcoming;

    /**
     * 완료 (endTime < now OR status=COMPLETED)
     */
    private Integer completed;
}
```

**쿼리 전략**
```java
@Repository
public interface FacilityReservationRepository extends JpaRepository<FacilityReservationTbl, Integer> {

    @Query("""
        SELECT new AdminReservationStatsDto(
            COUNT(CASE WHEN DATE(r.startTime) = :today THEN 1 END),
            COUNT(CASE WHEN DATE(r.startTime) = :today
                        AND r.startTime <= :now
                        AND r.endTime > :now
                        AND r.status = 'APPROVED' THEN 1 END),
            COUNT(CASE WHEN DATE(r.startTime) = :today
                        AND r.startTime > :now
                        AND r.status = 'APPROVED' THEN 1 END),
            COUNT(CASE WHEN DATE(r.startTime) >= :weekStart THEN 1 END),
            COUNT(CASE WHEN DATE(r.startTime) >= :monthStart THEN 1 END)
        )
        FROM FacilityReservationTbl r
        WHERE DATE(r.startTime) >= :today OR r.status IN ('PENDING', 'APPROVED')
    """)
    AdminReservationStatsDto getDashboardStats(
        @Param("today") LocalDate today,
        @Param("now") LocalDateTime now,
        @Param("weekStart") LocalDate weekStart,
        @Param("monthStart") LocalDate monthStart
    );
}
```

**인덱스 전략**
```sql
-- 통계 쿼리 성능 최적화
CREATE INDEX idx_reservation_date_status
ON FACILITY_RESERVATION_TBL(START_TIME, STATUS);

CREATE INDEX idx_reservation_time_range
ON FACILITY_RESERVATION_TBL(START_TIME, END_TIME);
```

---

### 8. 관리자 필터 명세 불충분

**문제**
- 관리자 화면에서 시설명 한글 검색, 유형 필터, 날짜 범위 필터 필요
- API 파라미터와 쿼리 방식 미정의

**해결 방안**
```java
@Data
public class AdminReservationFilterDto {
    /**
     * 시설 유형 필터 (소문자, optional)
     * 예: "study", "seminar"
     */
    private String facilityType;

    /**
     * 시설명 부분 일치 검색 (optional)
     * 예: "열람실" → "제1열람실", "제2열람실" 모두 매칭
     */
    private String facilityName;

    /**
     * 예약 상태 필터 (소문자, optional)
     * 예: "pending", "approved"
     */
    private String status;

    /**
     * 예약 날짜 시작 (optional, inclusive)
     */
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate dateFrom;

    /**
     * 예약 날짜 종료 (optional, inclusive)
     */
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate dateTo;

    /**
     * 페이징 정보
     */
    private Integer page = 0;
    private Integer size = 20;
    private String sortBy = "startTime";
    private String sortDir = "DESC";
}
```

**Repository 동적 쿼리**
```java
@Repository
public interface FacilityReservationRepository extends JpaRepository<FacilityReservationTbl, Integer> {

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
        @Param("facilityType") String facilityType,
        @Param("facilityName") String facilityName,
        @Param("status") String status,
        @Param("dateFrom") LocalDate dateFrom,
        @Param("dateTo") LocalDate dateTo,
        Pageable pageable
    );
}
```

---

### 9. 취소 정책 상세 미정의

**문제**
- "시작 N시간 전 취소 제한"만 언급, 구체적 시간 기준 없음
- 플랜에는 `CANCELLATION_DEADLINE_HOURS = 2`이지만 프론트 UI는 24시간 가정

**해결 방안**
```java
@Configuration
@ConfigurationProperties(prefix = "facility.reservation.policy")
public class ReservationPolicyProperties {
    /**
     * 예약 취소 가능 기한 (예약 시작 시간 기준, 시간)
     * 기본값: 24시간
     */
    private int cancelDeadlineHours = 24;

    /**
     * 최대 예약 가능 기간 (오늘 기준, 일)
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
    private int maxDurationMinutes = 240; // 4시간
}
```

**application.yml**
```yaml
facility:
  reservation:
    policy:
      cancel-deadline-hours: 24
      max-advance-days: 14
      max-active-reservations-per-user: 3
      min-duration-minutes: 60
      max-duration-minutes: 240
```

**서비스 로직**
```java
@Service
public class FacilityReservationService {
    @Autowired
    private ReservationPolicyProperties policy;

    public void cancelReservation(Integer reservationId, String userCode) {
        FacilityReservationTbl reservation = repository.findById(reservationId)
            .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 예약입니다."));

        // 본인 확인
        if (!reservation.getUserCode().equals(userCode)) {
            throw new UnauthorizedException("본인의 예약만 취소할 수 있습니다.");
        }

        // 취소 가능 시간 확인
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime deadline = reservation.getStartTime()
            .minusHours(policy.getCancelDeadlineHours());

        if (now.isAfter(deadline)) {
            throw new CancellationDeadlineException(
                "예약 시작 " + policy.getCancelDeadlineHours() +
                "시간 전까지만 취소 가능합니다."
            );
        }

        // 취소 처리
        reservation.setStatus(ReservationStatus.CANCELLED);
        reservation.setUpdatedAt(now);
        repository.save(reservation);

        // 로그 기록
        logRepository.save(FacilityReservationLog.builder()
            .reservationIdx(reservationId)
            .eventType("CANCELLED")
            .actorType("USER")
            .actorCode(userCode)
            .build());
    }
}
```

---

## 🟢 P2: 개선 사항

### 10. 시설 강조(Highlight) 표시

**문제**
- 프론트엔드 UI에서 특정 시설(체육관 등)을 `highlight` 처리
- 엔티티에 해당 필드 없음

**해결 방안 A: 계산 필드 (추천)**
```java
@Data
public class FacilitySummaryDto {
    private Integer facilityId;
    private String facilityName;
    private String facilityType;

    /**
     * 강조 표시 여부 (비즈니스 로직으로 결정)
     * - 체육관(gym)은 항상 true
     * - 수용 인원 100명 이상도 true
     */
    private Boolean highlighted;
}

// Service에서 계산
public FacilitySummaryDto toSummaryDto(FacilityTbl entity) {
    boolean highlight = "gym".equals(entity.getFacilityType().toValue())
                     || (entity.getCapacity() != null && entity.getCapacity() >= 100);

    return FacilitySummaryDto.builder()
        .facilityId(entity.getFacilityIdx())
        .facilityName(entity.getFacilityName())
        .highlighted(highlight)
        .build();
}
```

**해결 방안 B: DB 필드 추가**
```java
@Entity
@Table(name = "FACILITY_TBL")
public class FacilityTbl {
    // ...

    /**
     * 강조 표시 여부 (관리자가 수동 설정)
     */
    @Column(name = "IS_HIGHLIGHTED")
    private Integer isHighlighted = 0; // 0: 일반, 1: 강조
}
```

---

### 11. 사용자 정보 조인 전략

**문제**
- 관리자 테이블은 이름·학번·이메일 표시 필요
- `FacilityReservationTbl`에는 `userCode`만 있음

**해결 방안**
```java
@Repository
public interface FacilityReservationRepository extends JpaRepository<FacilityReservationTbl, Integer> {

    @Query("""
        SELECT new AdminReservationDetailDto(
            r.reservationIdx,
            f.facilityName,
            f.facilityType,
            r.startTime,
            r.endTime,
            r.status,
            r.purpose,
            r.partySize,
            u.userName,
            u.userCode,
            u.userEmail
        )
        FROM FacilityReservationTbl r
        JOIN FacilityTbl f ON r.facilityIdx = f.facilityIdx
        JOIN UserTbl u ON r.userCode = u.userCode
        WHERE r.status = 'PENDING'
        ORDER BY r.createdAt ASC
    """)
    List<AdminReservationDetailDto> findPendingWithUserInfo();
}
```

**DTO 정의**
```java
@Data
@AllArgsConstructor
public class AdminReservationDetailDto {
    // 예약 정보
    private Integer reservationId;
    private String facilityName;
    private String facilityType;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private String status;
    private String purpose;
    private Integer partySize;

    // 사용자 정보 (조인으로 가져옴)
    private String userName;
    private String studentId;  // userCode
    private String email;
}
```

---

### 12. FCM 알림 연동 흐름

**문제**
- "향후 확장"으로만 언급, 구체적인 발송 시점과 템플릿 없음

**해결 방안**
```java
@Service
public class AdminFacilityReservationService {
    @Autowired
    private FcmTokenService fcmService;

    @Transactional
    public void approveReservation(Integer reservationId, String adminCode, String note) {
        FacilityReservationTbl reservation = repository.findById(reservationId)
            .orElseThrow();

        // 상태 변경
        reservation.setStatus(ReservationStatus.APPROVED);
        reservation.setApprovedBy(adminCode);
        reservation.setApprovedAt(LocalDateTime.now());
        reservation.setAdminNote(note);
        repository.save(reservation);

        // FCM 알림 발송
        sendApprovalNotification(reservation);
    }

    @Async
    private void sendApprovalNotification(FacilityReservationTbl reservation) {
        try {
            FacilityTbl facility = facilityRepository.findById(reservation.getFacilityIdx())
                .orElse(null);

            String title = "시설 예약 승인";
            String body = String.format(
                "%s 예약이 승인되었습니다.\n일시: %s %s-%s",
                facility != null ? facility.getFacilityName() : "시설",
                reservation.getStartTime().toLocalDate(),
                reservation.getStartTime().toLocalTime(),
                reservation.getEndTime().toLocalTime()
            );

            fcmService.sendToUser(reservation.getUserCode(), title, body);

        } catch (Exception e) {
            logger.error("FCM 알림 발송 실패: reservationId={}", reservation.getReservationIdx(), e);
            // 알림 실패는 예약 승인에 영향 없음 (비동기)
        }
    }

    @Async
    private void sendRejectionNotification(FacilityReservationTbl reservation, String reason) {
        String title = "시설 예약 반려";
        String body = String.format(
            "예약이 반려되었습니다.\n사유: %s",
            reason
        );
        fcmService.sendToUser(reservation.getUserCode(), title, body);
    }
}
```

---

### 13. DTO 패키지 구조 개선

**문제**
- 현재 모든 DTO가 `dto/` 루트에 평탄하게 배치
- 시설 예약 DTO 추가 시 관리 어려움

**해결 방안**
```
dto/
├── auth/                    # 인증 관련 (기존)
│   ├── LoginRequest.java
│   ├── LoginResponse.java
│   └── ...
├── reading/                 # 열람실 관련 (기존 이동)
│   ├── SeatReserveRequestDto.java
│   ├── ReadingRoomStatusDto.java
│   └── ...
├── fcm/                     # FCM 알림 (기존 이동)
│   ├── FcmSendRequest.java
│   └── ...
├── facility/                # 시설 예약 (신규)
│   ├── request/
│   │   ├── FacilityListRequestDto.java
│   │   ├── FacilityReservationRequestDto.java
│   │   └── ReservationCancelRequestDto.java
│   ├── response/
│   │   ├── FacilitySummaryDto.java
│   │   ├── FacilityDetailDto.java
│   │   ├── TimeSlotStatusDto.java
│   │   └── ReservationResponseDto.java
│   └── admin/
│       ├── AdminReservationFilterDto.java
│       ├── AdminReservationStatsDto.java
│       └── AdminReservationDetailDto.java
└── common/                  # 공통 (기존 이동)
    ├── ApiResponse.java
    └── ImageRequest.java
```

**IDE 리팩토링 순서**
1. IntelliJ `Refactor → Move` 사용 (자동 import 갱신)
2. `./gradlew clean build` 실행하여 컴파일 확인
3. `grep -r "import.*\.dto\.[A-Z]" src/` 명시적 import 검증

---

## 📝 보완된 데이터 계약 명세

### Enum 표준
| Enum | DB/Java | JSON API | 비고 |
|------|---------|----------|------|
| FacilityType | `STUDY`, `SEMINAR`, `AUDITORIUM`, `GYM` | `"study"`, `"seminar"`, `"auditorium"`, `"gym"` | @JsonValue로 소문자 변환 |
| ReservationStatus | `PENDING`, `APPROVED`, `REJECTED`, `CANCELLED`, `COMPLETED` | `"pending"`, `"approved"`, ... | 동일 |

### 날짜/시간 형식
| 필드 | 형식 | 예시 | 용도 |
|------|------|------|------|
| 전송용 | ISO 8601 | `"2025-10-06T14:30:00+09:00"` | API 요청/응답 |
| 표시용 | YYYY-MM-DD | `"2025-10-06"` | 프론트 UI 표시 |
| 시간 범위 | HH:MM-HH:MM | `"14:30-16:30"` | 프론트 UI 표시 |
| 슬롯 | HH:MM | `"09:00"` | 시간 선택 UI |

### API 응답 공통 구조
```json
{
  "success": true,
  "message": "요청이 처리되었습니다.",
  "data": { /* 실제 데이터 */ },
  "errorCode": null,
  "timestamp": "2025-10-06T15:30:00+09:00"
}
```

---

## 🎯 다음 단계 권장 사항

### 즉시 수정 필요 (P0)
1. **Enum 정의 수정**: `@JsonValue`, `@JsonCreator` 추가
2. **TimeSlotStatusDto 구체화**: 필드 명세 및 Javadoc 작성
3. **시간 형식 표준화**: Jackson 설정 + DTO 포맷 필드 추가
4. **슬롯 처리 로직**: `calculateTimeRange()` 메서드 구현

### 다음 스프린트 (P1)
5. **통계 API 설계**: Repository 쿼리 작성 + 인덱스 설계
6. **필터 동적 쿼리**: Specification 또는 JPQL 기반 구현
7. **취소 정책 설정**: Properties 파일 + 서비스 검증 로직
8. **사용자 정보 조인**: JPQL JOIN FETCH 쿼리 작성

### 개선 사항 (P2)
9. **FCM 연동**: 비동기 알림 발송 메서드 추가
10. **DTO 패키지 정리**: IDE 리팩토링 도구 사용
11. **Highlight 로직**: 계산 필드 또는 DB 필드 선택

### 문서화
12. **API 계약서 작성**: `docs/facility-reservation-api-contract.md`
13. **플랜 업데이트**: 위 해결 방안을 기존 플랜에 반영
14. **Postman 컬렉션**: 프론트엔드 연동 테스트용

---

**총 문제점**: 13개
**P0 (즉시)**: 4개
**P1 (다음)**: 5개
**P2 (개선)**: 4개

각 항목은 독립적으로 해결 가능하며, P0 → P1 → P2 순서로 진행 권장.
