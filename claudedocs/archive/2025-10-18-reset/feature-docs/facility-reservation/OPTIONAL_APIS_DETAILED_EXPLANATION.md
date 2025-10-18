# 선택적 API 상세 설명

**작성일:** 2025-10-10  
**대상:** 시설 정책 API & 예약 이력 API

---

## 📋 목차

1. [시설 정책 API](#1-시설-정책-api)
2. [예약 이력 API](#2-예약-이력-api)
3. [비교 및 권장사항](#3-비교-및-권장사항)

---

## 1. 시설 정책 API

### 🎯 목적

시설마다 다른 예약 규칙을 **동적으로 관리**하기 위한 API입니다.

### 📊 현재 상황

#### 데이터베이스 구조
```sql
-- FACILITY_POLICY_TBL 테이블 (이미 존재함)
CREATE TABLE FACILITY_POLICY_TBL (
    POLICY_IDX INT AUTO_INCREMENT PRIMARY KEY,
    FACILITY_IDX INT NOT NULL UNIQUE,
    REQUIRES_APPROVAL BOOLEAN NOT NULL DEFAULT TRUE,
    MIN_DURATION_MINUTES INT,
    MAX_DURATION_MINUTES INT,
    MAX_DAYS_IN_ADVANCE INT,
    CANCELLATION_DEADLINE_HOURS INT,
    MAX_RESERVATIONS_PER_USER INT,
    ALLOW_WEEKEND_BOOKING BOOLEAN,
    CREATED_AT DATETIME NOT NULL,
    UPDATED_AT DATETIME NOT NULL,
    FOREIGN KEY (FACILITY_IDX) REFERENCES FACILITY_TBL(FACILITY_IDX)
);
```

#### 엔티티 클래스 (이미 존재함)
```java
@Entity
@Table(name = "FACILITY_POLICY_TBL")
public class FacilityPolicyTbl {
    private Integer policyIdx;
    private Integer facilityIdx;
    private Boolean requiresApproval;        // 승인 필수 여부
    private Integer minDurationMinutes;      // 최소 예약 시간
    private Integer maxDurationMinutes;      // 최대 예약 시간
    private Integer maxDaysInAdvance;        // 최대 예약 가능 일수
    private Integer cancellationDeadlineHours; // 취소 가능 시간
    private Integer maxReservationsPerUser;  // 1인당 최대 예약 수
    private Boolean allowWeekendBooking;     // 주말 예약 허용
    // ...
}
```

#### 리포지토리 (이미 존재함)
```java
@Repository
public interface FacilityPolicyRepository extends JpaRepository<FacilityPolicyTbl, Integer> {
    Optional<FacilityPolicyTbl> findByFacilityIdx(Integer facilityIdx);
}
```

### ❌ 현재 문제점

1. **정책 조회 불가**
   - 프론트엔드에서 시설별 정책을 알 수 없음
   - 예: "이 시설은 최대 몇 시간 예약 가능한가?"

2. **정책 수정 불가**
   - 관리자가 정책을 변경하려면 DB 직접 접근 필요
   - 예: "세미나실은 이제 승인 없이 예약 가능하게 하고 싶다"

3. **유연성 부족**
   - 시설마다 다른 규칙 적용 어려움
   - 예: "회의실은 4시간 제한, 세미나실은 8시간 제한"

### ✅ 제안하는 API

#### API 1: 시설 정책 조회

**엔드포인트:**
```http
GET /api/admin/facilities/{facilityIdx}/policy
```

**요청 예시:**
```http
GET /api/admin/facilities/1/policy
Authorization: Bearer {ADMIN_TOKEN}
```

**응답 예시:**
```json
{
  "status": "success",
  "message": "시설 정책을 조회했습니다.",
  "data": {
    "policyIdx": 1,
    "facilityIdx": 1,
    "facilityName": "소회의실 A",
    "requiresApproval": true,
    "minDurationMinutes": 30,
    "maxDurationMinutes": 240,
    "maxDaysInAdvance": 14,
    "cancellationDeadlineHours": 24,
    "maxReservationsPerUser": 3,
    "allowWeekendBooking": true,
    "createdAt": "2025-01-01T00:00:00",
    "updatedAt": "2025-10-10T12:00:00"
  }
}
```

**구현 코드:**
```java
// AdminFacilityPolicyController.java
@RestController
@RequestMapping("/api/admin/facilities")
public class AdminFacilityPolicyController {

    @Autowired
    private AdminFacilityPolicyService policyService;

    @GetMapping("/{facilityIdx}/policy")
    public ResponseEntity<ApiResponse<FacilityPolicyDto>> getPolicy(
            @PathVariable Integer facilityIdx,
            HttpServletRequest request) {
        String adminId = getAdminIdFromToken(request);
        FacilityPolicyDto policy = policyService.getPolicy(adminId, facilityIdx);
        return ResponseEntity.ok(ApiResponse.success("시설 정책을 조회했습니다.", policy));
    }
}
```

---

#### API 2: 시설 정책 수정

**엔드포인트:**
```http
PUT /api/admin/facilities/{facilityIdx}/policy
```

**요청 예시:**
```http
PUT /api/admin/facilities/1/policy
Authorization: Bearer {ADMIN_TOKEN}
Content-Type: application/json

{
  "requiresApproval": false,
  "maxDurationMinutes": 180,
  "allowWeekendBooking": false
}
```

**응답 예시:**
```json
{
  "status": "success",
  "message": "시설 정책이 수정되었습니다.",
  "data": {
    "policyIdx": 1,
    "facilityIdx": 1,
    "facilityName": "소회의실 A",
    "requiresApproval": false,
    "minDurationMinutes": 30,
    "maxDurationMinutes": 180,
    "maxDaysInAdvance": 14,
    "cancellationDeadlineHours": 24,
    "maxReservationsPerUser": 3,
    "allowWeekendBooking": false,
    "updatedAt": "2025-10-10T14:30:00"
  }
}
```

**구현 코드:**
```java
// AdminFacilityPolicyService.java
@Service
public class AdminFacilityPolicyService {

    @Autowired
    private FacilityPolicyRepository policyRepository;
    
    @Autowired
    private FacilityRepository facilityRepository;

    public FacilityPolicyDto updatePolicy(String adminId, Integer facilityIdx, 
                                          PolicyUpdateRequest request) {
        // 1. 관리자 검증
        validateAdmin(adminId);

        // 2. 시설 존재 확인
        FacilityTbl facility = facilityRepository.findById(facilityIdx)
            .orElseThrow(() -> new RuntimeException("시설을 찾을 수 없습니다."));

        // 3. 정책 조회 또는 생성
        FacilityPolicyTbl policy = policyRepository.findByFacilityIdx(facilityIdx)
            .orElse(new FacilityPolicyTbl());

        // 4. 정책 수정
        if (request.getRequiresApproval() != null) {
            policy.setRequiresApproval(request.getRequiresApproval());
        }
        if (request.getMaxDurationMinutes() != null) {
            policy.setMaxDurationMinutes(request.getMaxDurationMinutes());
        }
        if (request.getAllowWeekendBooking() != null) {
            policy.setAllowWeekendBooking(request.getAllowWeekendBooking());
        }
        // ... 기타 필드

        // 5. 저장
        FacilityPolicyTbl saved = policyRepository.save(policy);

        // 6. DTO 변환
        return convertToDto(saved, facility.getFacilityName());
    }
}
```

---

### 💡 사용 시나리오

#### 시나리오 1: 승인 방식 변경
**상황:**
- 세미나실은 이제 인기가 많아서 승인 필수로 변경하고 싶음

**작업:**
```http
PUT /api/admin/facilities/3/policy
{
  "requiresApproval": true
}
```

**결과:**
- 세미나실 예약 시 자동으로 PENDING 상태로 생성됨
- 관리자 승인 필요

---

#### 시나리오 2: 최대 예약 시간 조정
**상황:**
- 회의실 A는 4시간만 예약 가능하게 제한

**작업:**
```http
PUT /api/admin/facilities/1/policy
{
  "maxDurationMinutes": 240
}
```

**결과:**
- 4시간 넘게 예약 시도 시 에러 발생
- 프론트엔드에서 미리 제한 표시 가능

---

#### 시나리오 3: 주말 예약 제한
**상황:**
- 주말에는 시설 관리가 어려워서 예약 차단

**작업:**
```http
PUT /api/admin/facilities/2/policy
{
  "allowWeekendBooking": false
}
```

**결과:**
- 토요일, 일요일 예약 시도 시 에러 발생

---

### ⚖️ 장단점

#### ✅ 장점

1. **유연한 운영**
   - 시설마다 다른 규칙 적용
   - 상황에 따라 즉시 변경 가능

2. **관리 편의성**
   - UI에서 직접 정책 수정
   - DB 직접 접근 불필요

3. **프론트엔드 연동**
   - API로 정책 조회 가능
   - 예약 전 제약사항 안내 가능

#### ❌ 단점

1. **사용 빈도 낮음**
   - 정책은 초기 설정 후 거의 변경 안 함
   - 월 1-2회 정도 수정할까?

2. **DB 직접 수정 가능**
   - 현재도 DB에서 수정 가능
   - 꼭 API가 필요한가?

3. **개발 시간 소요**
   - 약 3시간 예상
   - 다른 기능 개발 지연

---

## 2. 예약 이력 API

### 🎯 목적

예약의 **모든 상태 변경 이력**을 추적하기 위한 API입니다.

### 📊 현재 상황

#### 데이터베이스 구조
```sql
-- FACILITY_RESERVATION_LOG 테이블 (이미 존재함)
CREATE TABLE FACILITY_RESERVATION_LOG (
    LOG_IDX INT AUTO_INCREMENT PRIMARY KEY,
    RESERVATION_IDX INT NOT NULL,
    EVENT_TYPE VARCHAR(50) NOT NULL,
    ACTOR_TYPE VARCHAR(20),
    ACTOR_CODE VARCHAR(50),
    PAYLOAD TEXT,
    CREATED_AT DATETIME NOT NULL,
    FOREIGN KEY (RESERVATION_IDX) REFERENCES FACILITY_RESERVATION_TBL(RESERVATION_IDX)
);
```

#### 엔티티 클래스 (이미 존재함)
```java
@Entity
@Table(name = "FACILITY_RESERVATION_LOG")
public class FacilityReservationLog {
    private Integer logIdx;
    private Integer reservationIdx;
    private String eventType;        // CREATED, APPROVED, REJECTED, CANCELLED
    private String actorType;        // USER, ADMIN, SYSTEM
    private String actorCode;        // 사용자/관리자 ID
    private String payload;          // JSON 형태의 추가 정보
    private LocalDateTime createdAt;
}
```

#### 리포지토리 (이미 존재함)
```java
@Repository
public interface FacilityReservationLogRepository 
        extends JpaRepository<FacilityReservationLog, Integer> {
    
    List<FacilityReservationLog> findByReservationIdxOrderByCreatedAtAsc(Integer reservationIdx);
}
```

### ❌ 현재 문제점

1. **이력 조회 불가**
   - 예약이 언제 승인/반려되었는지 알 수 없음
   - 누가 승인/반려했는지 알 수 없음

2. **문제 추적 어려움**
   - 예약이 왜 취소되었는지 확인 불가
   - 디버깅 시 어려움

3. **감사 기능 부족**
   - 관리자 행동 추적 불가
   - 책임 소재 불분명

### ✅ 제안하는 API

#### API: 예약 이력 조회

**엔드포인트:**
```http
GET /api/admin/reservations/{reservationIdx}/logs
```

**요청 예시:**
```http
GET /api/admin/reservations/123/logs
Authorization: Bearer {ADMIN_TOKEN}
```

**응답 예시:**
```json
{
  "status": "success",
  "message": "예약 이력을 조회했습니다.",
  "data": {
    "reservationIdx": 123,
    "facilityName": "소회의실 A",
    "userName": "홍길동",
    "currentStatus": "APPROVED",
    "logs": [
      {
        "logIdx": 1,
        "eventType": "CREATED",
        "actorType": "USER",
        "actorCode": "user123",
        "actorName": "홍길동",
        "message": "예약이 생성되었습니다.",
        "createdAt": "2025-10-10T10:00:00"
      },
      {
        "logIdx": 2,
        "eventType": "APPROVED",
        "actorType": "ADMIN",
        "actorCode": "admin01",
        "actorName": "관리자",
        "message": "예약이 승인되었습니다.",
        "payload": {
          "approvalComment": "회의 목적 확인됨"
        },
        "createdAt": "2025-10-10T11:30:00"
      }
    ]
  }
}
```

**구현 코드:**
```java
// AdminFacilityReservationController.java
@RestController
@RequestMapping("/api/admin/reservations")
public class AdminFacilityReservationController {

    @Autowired
    private AdminFacilityReservationService reservationService;

    @GetMapping("/{reservationIdx}/logs")
    public ResponseEntity<ApiResponse<ReservationLogDto>> getLogs(
            @PathVariable Integer reservationIdx,
            HttpServletRequest request) {
        String adminId = getAdminIdFromToken(request);
        ReservationLogDto logs = reservationService.getReservationLogs(adminId, reservationIdx);
        return ResponseEntity.ok(ApiResponse.success("예약 이력을 조회했습니다.", logs));
    }
}
```

```java
// AdminFacilityReservationService.java
@Service
public class AdminFacilityReservationService {

    @Autowired
    private FacilityReservationLogRepository logRepository;
    
    @Autowired
    private FacilityReservationRepository reservationRepository;

    public ReservationLogDto getReservationLogs(String adminId, Integer reservationIdx) {
        // 1. 관리자 검증
        validateAdmin(adminId);

        // 2. 예약 조회
        FacilityReservationTbl reservation = reservationRepository.findById(reservationIdx)
            .orElseThrow(() -> new RuntimeException("예약을 찾을 수 없습니다."));

        // 3. 로그 조회
        List<FacilityReservationLog> logs = logRepository
            .findByReservationIdxOrderByCreatedAtAsc(reservationIdx);

        // 4. DTO 변환
        return convertToLogDto(reservation, logs);
    }
}
```

---

### 📝 로그 이벤트 타입

| Event Type | 설명 | Actor Type | 발생 시점 |
|-----------|------|------------|----------|
| CREATED | 예약 생성 | USER | 예약 신청 시 |
| APPROVED | 예약 승인 | ADMIN | 관리자 승인 시 |
| REJECTED | 예약 반려 | ADMIN | 관리자 반려 시 |
| CANCELLED | 예약 취소 | USER | 사용자 취소 시 |
| AUTO_APPROVED | 자동 승인 | SYSTEM | 자동승인 시설 |
| COMPLETED | 예약 완료 | SYSTEM | 예약 시간 종료 |
| EXPIRED | 예약 만료 | SYSTEM | 승인 기한 만료 |

---

### 💡 사용 시나리오

#### 시나리오 1: 승인 이력 확인
**상황:**
- 사용자가 "누가 내 예약을 승인했나요?" 문의

**작업:**
```http
GET /api/admin/reservations/123/logs
```

**결과:**
```json
{
  "logs": [
    {
      "eventType": "APPROVED",
      "actorType": "ADMIN",
      "actorName": "김관리",
      "createdAt": "2025-10-10T11:30:00"
    }
  ]
}
```

---

#### 시나리오 2: 취소 사유 확인
**상황:**
- 예약이 취소된 이유를 알고 싶음

**작업:**
```http
GET /api/admin/reservations/456/logs
```

**결과:**
```json
{
  "logs": [
    {
      "eventType": "CANCELLED",
      "actorType": "USER",
      "actorName": "홍길동",
      "payload": {
        "reason": "일정 변경으로 인한 취소"
      },
      "createdAt": "2025-10-10T09:00:00"
    }
  ]
}
```

---

#### 시나리오 3: 문제 디버깅
**상황:**
- 예약이 자동 승인되지 않고 PENDING 상태로 남음

**작업:**
```http
GET /api/admin/reservations/789/logs
```

**결과:**
```json
{
  "logs": [
    {
      "eventType": "CREATED",
      "actorType": "USER",
      "createdAt": "2025-10-10T14:00:00"
    }
  ]
}
```

**분석:** AUTO_APPROVED 이벤트가 없음 → 시설 정책 확인 필요

---

### ⚖️ 장단점

#### ✅ 장점

1. **투명성 증가**
   - 모든 변경 이력 추적
   - 누가, 언제, 무엇을 했는지 명확

2. **디버깅 용이**
   - 문제 발생 시 원인 파악 쉬움
   - 시스템 동작 확인 가능

3. **감사 추적**
   - 관리자 행동 기록
   - 책임 소재 명확

4. **사용자 신뢰**
   - 예약 처리 과정 공개
   - 투명한 운영

#### ❌ 단점

1. **사용 빈도 매우 낮음**
   - 대부분의 예약은 문제 없이 진행
   - 이력 조회가 필요한 경우 드뭄

2. **DB 직접 조회 가능**
   - SQL로 FACILITY_RESERVATION_LOG 조회 가능
   - 굳이 API 필요한가?

3. **프론트엔드 UI 복잡**
   - 로그 표시 화면 필요
   - 추가 개발 시간 소요

---

## 3. 비교 및 권장사항

### 📊 두 API 비교

| 항목 | 시설 정책 API | 예약 이력 API |
|-----|-------------|--------------|
| **개발 시간** | 3시간 | 2시간 |
| **사용 빈도** | 월 1-2회 | 월 0-1회 |
| **중요도** | Medium | Low |
| **대체 방법** | DB 직접 수정 | DB 직접 조회 |
| **프론트엔드 영향** | 정책 표시 필요 | 로그 화면 필요 |
| **즉시 필요성** | ⚠️ 낮음 | ⚠️ 매우 낮음 |

---

### 💡 권장 사항

#### Option 1: 둘 다 구현하지 않음 (권장) ✅

**이유:**
1. **핵심 기능 이미 완성**
   - 예약 생성, 조회, 승인, 반려 모두 가능
   - 시스템 동작에 문제 없음

2. **사용 빈도 매우 낮음**
   - 정책: 초기 설정 후 거의 변경 안 함
   - 이력: 문제 발생 시에만 필요

3. **대체 방법 존재**
   - 정책: DB에서 직접 수정 가능
   - 이력: DB에서 직접 조회 가능

4. **개발 리소스 최적화**
   - 5시간을 프론트엔드 연동에 투자하는 게 더 효율적

**다음 단계:**
- 즉시 프론트엔드 연동 시작
- 실제 사용 후 피드백 수집
- 정말 필요하다는 판단 시 추가 개발

---

#### Option 2: 시설 정책 API만 구현

**이유:**
- 정책 API가 이력 API보다 사용 빈도 높음
- 프론트엔드에서 정책 조회 필요할 수 있음

**포함 내용:**
- GET `/api/admin/facilities/{id}/policy` - 정책 조회
- PUT `/api/admin/facilities/{id}/policy` - 정책 수정

**예상 시간:** 3시간

---

#### Option 3: 둘 다 구현

**이유:**
- 관리 기능 완전성 향상
- 향후 확장 용이

**예상 시간:** 5시간

**주의:**
- 프론트엔드 연동 5시간 지연
- 실제 사용 빈도 낮을 가능성 높음

---

### 🎯 최종 권장

```
✅ 권장: Option 1 (구현하지 않음)

이유:
1. 핵심 기능 100% 완성됨
2. 프론트엔드 연동이 더 우선순위
3. 실제 필요성 검증 후 추가 개발

다음 단계:
→ 프론트엔드 API 연동 시작
→ 실제 사용 테스트
→ 피드백 수집
→ 필요시 Phase 2로 추가
```

---

## 📝 구현 가이드 (필요 시)

### 시설 정책 API 구현 순서 (3시간)

1. **DTO 생성** (30분)
   - FacilityPolicyDto
   - PolicyUpdateRequest

2. **서비스 계층** (1시간)
   - AdminFacilityPolicyService 생성
   - getPolicy() 메서드
   - updatePolicy() 메서드

3. **컨트롤러 계층** (30분)
   - AdminFacilityPolicyController 생성
   - GET, PUT 엔드포인트

4. **테스트** (1시간)
   - 정책 조회 테스트
   - 정책 수정 테스트
   - 권한 검증 테스트

---

### 예약 이력 API 구현 순서 (2시간)

1. **DTO 생성** (20분)
   - ReservationLogDto
   - LogEntryDto

2. **서비스 계층** (40분)
   - getReservationLogs() 메서드
   - DTO 변환 로직

3. **컨트롤러 계층** (20분)
   - GET 엔드포인트 추가

4. **테스트** (40분)
   - 로그 조회 테스트
   - 다양한 시나리오 테스트

---

## 🎉 결론

### 현재 상태
- ✅ **핵심 기능 100% 완성**
- ✅ **시스템 정상 동작 가능**
- ⚠️ 선택 기능은 실제 필요성 낮음

### 권장 방향
1. **현재 상태로 프론트엔드 연동 시작**
2. 실제 운영 후 피드백 수집
3. 정말 필요하다는 판단 시 추가 개발

### 핵심 메시지
> **"완벽보다 동작이 먼저"**
> 
> - 핵심 기능 완성됨 ✅
> - 나머지는 실제 필요성 검증 후 ⏰
> - 빠른 출시 > 모든 기능 완성 🚀

---

**문의:**
- 두 API에 대해 더 자세한 설명이 필요하신가요?
- 구현을 원하시나요? (3시간 또는 5시간 소요)
- 프론트엔드 연동부터 시작하시겠어요? (권장)
