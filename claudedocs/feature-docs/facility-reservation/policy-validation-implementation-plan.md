# 시설 예약 정책 검증 구현 계획

## 개요
현재 시설 예약 시스템에서 미구현된 5가지 정책 검증 로직을 추가하여 시스템의 완결성을 향상시킵니다.

**문서 버전**: 1.0
**작성일**: 2025-10-13
**대상 파일**: `FacilityReservationService.java`

---

## 현재 검증 플로우 분석

### 예약 생성 플로우 (createReservation)
```
① validateReservationRequest(request)        // 글로벌 기본 검증
② 정책 조회 (FacilityPolicyTbl)
③ validateReservationPolicy(policy, ...)    // 시설별 정책 검증
④ 시설 조회 및 활성 상태 확인
⑤ 승인 정책 분기:
   - requiresApproval=true → validateNotBlocked만 체크
   - requiresApproval=false → checkAvailabilityWithFacility (비관적 락)
⑥ 예약 저장
```

### 예약 취소 플로우 (cancelReservation)
```
① 예약 조회
② 본인 예약 확인
③ 상태 확인 (PENDING/APPROVED만 취소 가능)
④ CANCELLED로 변경
⑤ 로그 기록
```

**삽입 포인트 식별:**
- **정원 검증**: ③ 직후, ④ facility 조회 후
- **사용자당 예약 수 제한**: ③ 직후 (DB 쿼리 필요)
- **주말 예약 제한**: ③ `validateReservationPolicy` 내부
- **취소 마감 시간**: 취소 플로우 ③ 직후

---

## 구현 계획

### 1. 정원/인원수 검증 (partySize/capacity)

#### 📍 위치
- `validateReservationRequest()` 메서드 내부 (line 330-361)
- `createReservation()` 메서드 내부, facility 조회 직후 (line 89-90 또는 104-105)

#### 🎯 구현 전략
**옵션 A: DTO 레벨 검증 (권장)**
```java
// ReservationCreateRequestDto.java에 추가
@NotNull(message = "인원수는 필수 입력 항목입니다.")
@Min(value = 1, message = "인원수는 최소 1명 이상이어야 합니다.")
private Integer partySize;
```

**옵션 B: 서비스 레벨 검증**
```java
// FacilityReservationService.validateReservationRequest()에 추가
if (request.getPartySize() == null || request.getPartySize() < 1) {
    throw new RuntimeException("인원수는 최소 1명 이상이어야 합니다.");
}
```

**옵션 C: 시설별 정원 검증 (새 메서드 생성)**
```java
// FacilityReservationService에 새 메서드 추가
private void validateCapacity(FacilityTbl facility, Integer partySize) {
    if (partySize == null || partySize < 1) {
        throw new RuntimeException("인원수는 최소 1명 이상이어야 합니다.");
    }

    if (facility.getCapacity() != null && partySize > facility.getCapacity()) {
        throw new RuntimeException(
            String.format("요청 인원(%d명)이 시설 정원(%d명)을 초과합니다.",
                partySize, facility.getCapacity()));
    }
}

// createReservation()에서 호출
facility = facilityRepository.findById(request.getFacilityIdx())...
validateCapacity(facility, request.getPartySize());  // ← 여기 추가
```

#### ✅ 권장 사항
**3단계 검증 조합:**
1. DTO 레벨: `@NotNull`, `@Min(1)` 추가 (기본 검증)
2. 서비스 레벨: `validateCapacity()` 메서드 생성
3. 호출 위치: `createReservation()` 내 facility 조회 직후

---

### 2. 사용자당 예약 수 제한 (maxReservationsPerUser)

#### 📍 위치
- `validateReservationPolicy()` 메서드 확장 (line 369-406)

#### 🎯 구현 전략
**새 메서드 생성:**
```java
/**
 * 사용자의 활성 예약 수 확인
 * @param userCode 사용자 코드
 * @param facilityIdx 시설 ID
 * @param policy 시설 정책
 */
private void validateUserReservationLimit(String userCode, Integer facilityIdx,
                                          FacilityPolicyTbl policy) {
    Integer maxReservations = policy.getEffectiveMaxReservationsPerUser();

    // NULL이면 제한 없음
    if (maxReservations == null) {
        return;
    }

    // 해당 시설에 대한 사용자의 활성 예약 수 조회
    List<String> activeStatuses = Arrays.asList(
        ReservationStatus.PENDING.toDbValue(),
        ReservationStatus.APPROVED.toDbValue()
    );

    long activeCount = reservationRepository
        .countByUserCodeAndFacilityIdxAndStatusIn(userCode, facilityIdx, activeStatuses);

    if (activeCount >= maxReservations) {
        throw new RuntimeException(
            String.format("이 시설에 대한 예약 가능 횟수(%d회)를 초과했습니다. " +
                         "현재 활성 예약: %d건", maxReservations, activeCount));
    }
}

// createReservation()에서 호출
validateReservationPolicy(policy, request.getStartTime(), request.getEndTime());
validateUserReservationLimit(userCode, request.getFacilityIdx(), policy);  // ← 추가
```

#### 📦 필요한 Repository 메서드
```java
// FacilityReservationRepository.java에 추가
long countByUserCodeAndFacilityIdxAndStatusIn(
    String userCode, Integer facilityIdx, List<String> statuses);
```

#### ⚠️ 고려사항
- **범위 선택**: 시설별 제한 vs 전체 시설 제한
  - 현재 설계: 시설별 제한 (권장)
  - 대안: `facilityIdx` 파라미터 제거하면 전체 시설 제한 가능
- **성능**: 인덱스 확인 필요 (`USER_CODE`, `FACILITY_IDX`, `STATUS`)

---

### 3. 주말 예약 허용 여부 (allowWeekendBooking)

#### 📍 위치
- `validateReservationPolicy()` 메서드 내부 확장 (line 369-406)

#### 🎯 구현 전략
```java
// validateReservationPolicy() 메서드 끝에 추가

// 4. 주말 예약 제한 체크
if (!policy.isEffectiveAllowWeekendBooking()) {
    DayOfWeek dayOfWeek = startTime.getDayOfWeek();
    if (dayOfWeek == DayOfWeek.SATURDAY || dayOfWeek == DayOfWeek.SUNDAY) {
        throw new RuntimeException("이 시설은 주말 예약이 허용되지 않습니다.");
    }
}
```

#### 📦 필요한 import
```java
import java.time.DayOfWeek;
```

#### ⚠️ 고려사항
- **공휴일 처리**: 현재 구현은 주말만 체크, 공휴일은 별도 처리 필요
- **날짜 범위**: `startTime`만 체크 (당일 예약이므로 충분)

---

### 4. 취소 마감 시간 (cancellationDeadlineHours)

#### 📍 위치
- `cancelReservation()` 메서드 내부 (line 309-328)

#### 🎯 구현 전략
```java
public void cancelReservation(Integer reservationIdx, String userCode) {
    FacilityReservationTbl reservation = reservationRepository.findById(reservationIdx)
        .orElseThrow(() -> ResourceNotFoundException.forId("예약", reservationIdx));

    if (!reservation.getUserCode().equals(userCode)) {
        throw new RuntimeException("본인의 예약만 취소할 수 있습니다.");
    }

    if (reservation.getStatusEnum() != ReservationStatus.PENDING &&
        reservation.getStatusEnum() != ReservationStatus.APPROVED) {
        throw new RuntimeException("취소할 수 없는 상태입니다.");
    }

    // ==================== 여기에 추가 ====================
    // 취소 마감 시간 체크
    FacilityPolicyTbl policy = policyRepository
        .findByFacilityIdx(reservation.getFacilityIdx())
        .orElseThrow(() -> new RuntimeException("시설 정책을 찾을 수 없습니다."));

    int deadlineHours = policy.getEffectiveCancellationDeadlineHours();
    LocalDateTime deadline = reservation.getStartTime().minusHours(deadlineHours);
    LocalDateTime now = LocalDateTime.now();

    if (now.isAfter(deadline)) {
        throw new RuntimeException(
            String.format("예약 시작 %d시간 전까지만 취소 가능합니다. " +
                         "취소 마감 시간: %s",
                         deadlineHours,
                         deadline.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))));
    }
    // =====================================================

    reservation.setStatusEnum(ReservationStatus.CANCELLED);
    reservationRepository.save(reservation);

    createLog(reservationIdx, "CANCELLED", "USER", userCode, null);

    logger.info("Reservation cancelled: ID={}, User={}", reservationIdx, userCode);
}
```

#### 📦 필요한 import
```java
import java.time.format.DateTimeFormatter;
```

#### ⚠️ 고려사항
- **관리자 취소**: 관리자는 마감시간 무시하도록 할지 결정 필요
- **메시지 개선**: 남은 시간 표시 가능

---

### 5. PENDING 예약 충돌 처리 개선

#### 📍 위치
- `checkAvailabilityInternal()` 메서드 (line 226-274)
- `AdminFacilityReservationService.approveReservation()`

#### 🎯 구현 전략

**옵션 A: 승인 시에만 APPROVED 체크 (권장)**
```java
// checkAvailabilityInternal()에 checkPendingConflicts 파라미터 추가
private FacilityAvailabilityDto checkAvailabilityInternal(
    FacilityTbl facility,
    LocalDateTime startTime,
    LocalDateTime endTime,
    Integer excludeReservationIdx,
    boolean checkPendingConflicts  // ← 새 파라미터
) {
    // ... 차단 체크 코드 ...

    List<String> activeStatuses;
    if (checkPendingConflicts) {
        // 사용자 예약 생성 시: PENDING + APPROVED 모두 체크
        activeStatuses = Arrays.asList(
            ReservationStatus.PENDING.toDbValue(),
            ReservationStatus.APPROVED.toDbValue()
        );
    } else {
        // 관리자 승인 시: APPROVED만 체크
        activeStatuses = Arrays.asList(
            ReservationStatus.APPROVED.toDbValue()
        );
    }

    List<FacilityReservationTbl> conflicts = reservationRepository
        .findConflictingReservations(facility.getFacilityIdx(),
            startTime, endTime, activeStatuses, excludeReservationIdx);

    // ... 나머지 코드 ...
}

// 호출부 수정
private FacilityAvailabilityDto checkAvailabilityWithFacility(...) {
    return checkAvailabilityInternal(facility, startTime, endTime, null, true);
}

FacilityAvailabilityDto checkAvailabilityWithExclusion(...) {
    return checkAvailabilityInternal(facility, startTime, endTime,
                                     excludeReservationIdx, false);  // ← false로 변경
}
```

**옵션 B: PENDING 예약 관리 UI 개선**
- 관리자 화면에 겹치는 PENDING 예약들을 그룹으로 표시
- 일괄 처리 기능 제공 (하나 승인, 나머지 자동 반려)

**옵션 C: 우선순위 기반 자동 처리**
- 선착순: `createdAt` 기준 가장 먼저 신청한 것 자동 승인
- 인원수 우선: `partySize` 기준 정렬

#### ✅ 권장 사항
**옵션 A 구현 + 관리자 UI 개선**
1. `checkAvailabilityInternal()`에 `checkPendingConflicts` 파라미터 추가
2. 사용자 예약 생성: PENDING+APPROVED 체크 (현재 그대로)
3. 관리자 승인: APPROVED만 체크 (충돌 가능성 감소)
4. 관리자 화면: 겹치는 PENDING 예약 경고 표시

---

## 구현 우선순위

### Phase 1: 기본 안전성 확보 (High Priority)
1. **정원/인원수 검증** - 즉시 구현 필요 (안전 문제)
2. **취소 마감 시간 적용** - 운영 효율성 향상

### Phase 2: 정책 완결성 (Medium Priority)
3. **사용자당 예약 수 제한** - 리소스 공평 분배
4. **주말 예약 제한** - 시설 특성별 운영 정책

### Phase 3: 사용자 경험 개선 (Low Priority)
5. **PENDING 충돌 처리 개선** - 관리자 워크플로우 최적화

---

## 데이터베이스 변경사항

### 새로운 Repository 메서드
```java
// FacilityReservationRepository.java
long countByUserCodeAndFacilityIdxAndStatusIn(
    String userCode, Integer facilityIdx, List<String> statuses);
```

### 인덱스 권장사항
```sql
-- 사용자별 예약 수 조회 성능 향상
CREATE INDEX idx_reservation_user_facility_status
ON FACILITY_RESERVATION_TBL(USER_CODE, FACILITY_IDX, STATUS);
```

---

## 테스트 시나리오

### 1. 정원 검증 테스트
- [ ] partySize = null → 에러
- [ ] partySize = 0 → 에러
- [ ] partySize = -1 → 에러
- [ ] partySize > capacity → 에러
- [ ] partySize = capacity → 성공
- [ ] capacity = null인 시설 → partySize 양수면 성공

### 2. 예약 수 제한 테스트
- [ ] maxReservationsPerUser = null → 무제한 예약 가능
- [ ] 활성 예약 = 0, maxReservationsPerUser = 3 → 예약 가능
- [ ] 활성 예약 = 2, maxReservationsPerUser = 3 → 예약 가능
- [ ] 활성 예약 = 3, maxReservationsPerUser = 3 → 예약 불가
- [ ] CANCELLED 예약은 카운트 제외 확인

### 3. 주말 예약 제한 테스트
- [ ] allowWeekendBooking = true, 토요일 → 예약 가능
- [ ] allowWeekendBooking = false, 토요일 → 예약 불가
- [ ] allowWeekendBooking = false, 일요일 → 예약 불가
- [ ] allowWeekendBooking = false, 평일 → 예약 가능
- [ ] allowWeekendBooking = null → 토요일 예약 가능 (기본값 true)

### 4. 취소 마감 시간 테스트
- [ ] 시작 24시간 전 취소 시도 → 성공
- [ ] 시작 25시간 전 취소 시도 → 성공
- [ ] 시작 23시간 전 취소 시도 → 실패
- [ ] 시작 1시간 전 취소 시도 → 실패
- [ ] cancellationDeadlineHours = 0 설정 → 언제든 취소 가능

### 5. PENDING 충돌 처리 테스트
- [ ] 겹치는 시간대 2개 PENDING 예약 생성 → 둘 다 성공
- [ ] 첫 번째 PENDING 승인 → 성공
- [ ] 두 번째 PENDING 승인 시도 → 실패 (APPROVED 충돌)
- [ ] 사용자가 APPROVED 시간대 예약 시도 → 자동승인 시설은 즉시 실패

---

## 롤백 계획

### 긴급 비활성화 방법
각 검증 로직은 정책값을 통해 제어 가능:
- 정원 검증: `capacity = null` 설정 → 검증 스킬
- 예약 수 제한: `maxReservationsPerUser = null` → 무제한
- 주말 제한: `allowWeekendBooking = true` → 주말 허용
- 취소 마감: `cancellationDeadlineHours = 0` → 언제든 취소

### 코드 레벨 비활성화
```java
// 임시 비활성화 플래그 추가 (application.yml)
reservation:
  policy:
    enforce-capacity: true
    enforce-max-reservations: true
    enforce-weekend-restriction: true
    enforce-cancellation-deadline: true
```

---

## 예상 영향 분석

### 긍정적 영향
- 시설 정원 초과 방지 → 안전성 향상
- 특정 사용자의 독점 예약 방지 → 공평성 향상
- 취소 마감 시간 적용 → 시설 활용도 향상
- 주말 운영 정책 통제 → 운영 유연성 향상

### 부정적 영향 (주의사항)
- 기존 사용자 경험 변화 → 사전 공지 필요
- 취소 마감 시간 적용 시 불만 가능 → 합리적 기본값 설정 (24시간)
- 예약 수 제한 시 불편 발생 가능 → 적절한 제한값 설정 필요

---

## 구현 체크리스트

### Phase 1
- [ ] ReservationCreateRequestDto에 `@NotNull`, `@Min` 추가
- [ ] validateCapacity() 메서드 구현
- [ ] createReservation()에서 validateCapacity() 호출
- [ ] cancelReservation()에 취소 마감 시간 검증 추가
- [ ] Phase 1 테스트 작성 및 실행

### Phase 2
- [ ] FacilityReservationRepository에 countByUserCodeAndFacilityIdxAndStatusIn 추가
- [ ] validateUserReservationLimit() 메서드 구현
- [ ] createReservation()에서 validateUserReservationLimit() 호출
- [ ] validateReservationPolicy()에 주말 예약 검증 추가
- [ ] Phase 2 테스트 작성 및 실행

### Phase 3
- [ ] checkAvailabilityInternal()에 checkPendingConflicts 파라미터 추가
- [ ] AdminFacilityReservationService 승인 로직 수정
- [ ] 관리자 UI에 PENDING 충돌 경고 추가
- [ ] Phase 3 테스트 작성 및 실행

### 최종 검증
- [ ] 통합 테스트 실행
- [ ] 성능 테스트 (인덱스 확인)
- [ ] 문서 업데이트 (facility-reservation.md)
- [ ] 롤백 절차 검증

---

## 참고사항

### 관련 파일
- `FacilityReservationService.java` (주요 수정 대상)
- `FacilityPolicyTbl.java` (정책 엔티티)
- `ReservationCreateRequestDto.java` (DTO 검증)
- `FacilityReservationRepository.java` (새 쿼리 메서드)
- `AdminFacilityReservationService.java` (승인 로직)

### 정책 기본값 (FacilityPolicyTbl.java)
```java
getEffectiveMinDurationMinutes()              → 30분
getEffectiveMaxDurationMinutes()              → 480분 (8시간)
getEffectiveMinDaysInAdvance()                → 0일 (즉시 예약 가능)
getEffectiveMaxDaysInAdvance()                → 30일
getEffectiveCancellationDeadlineHours()       → 24시간
getEffectiveMaxReservationsPerUser()          → null (무제한)
isEffectiveAllowWeekendBooking()              → true (허용)
```