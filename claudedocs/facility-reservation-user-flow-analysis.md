# 시설물 예약 기능 - 유저 플로우 상세 분석

## 📋 목차
1. [시설 목록 조회 플로우](#1-시설-목록-조회-플로우)
2. [시설 가용성 확인 플로우](#2-시설-가용성-확인-플로우)
3. [예약 신청 플로우](#3-예약-신청-플로우)
4. [내 예약 조회 플로우](#4-내-예약-조회-플로우)
5. [예약 취소 플로우](#5-예약-취소-플로우)
6. [데이터베이스 스키마 요약](#6-데이터베이스-스키마-요약)

---

## 1. 시설 목록 조회 플로우

### 1.1 전체 시설 조회 (`POST /api/facilities`)

#### 호출 흐름
```
Client → FacilityController.getAllFacilities()
         → FacilityService.getAllActiveFacilities()
         → FacilityRepository.findByIsActiveTrue()
         → FacilityBlockRepository.findActiveFacilityBlocks()
```

#### 클래스별 상세 동작

**🎯 Controller: FacilityController.java:33**
```java
public ResponseEntity<ApiResponse<List<FacilityDto>>> getAllFacilities()
```
- **역할**: HTTP 요청 수신 및 응답 반환
- **입력**: 없음
- **출력**: ApiResponse<List<FacilityDto>>

**🔧 Service: FacilityService.java:33**
```java
public List<FacilityDto> getAllActiveFacilities()
```
- **역할**: 활성화된 시설 목록 조회 및 DTO 변환
- **처리 로직**:
  1. Repository에서 활성 시설 목록 조회
  2. 각 시설에 대해 `convertToDto()` 호출하여 DTO 변환
  3. 변환된 DTO 리스트 반환

**💾 Repository: FacilityRepository.java:22**
```java
List<FacilityTbl> findByIsActiveTrue()
```
- **접근 DB**: `FACILITY_TBL`
- **조회 조건**: `IS_ACTIVE = true`
- **반환 데이터**:
  - FACILITY_IDX (시설 ID)
  - FACILITY_NAME (시설명)
  - FACILITY_TYPE (시설 유형: LECTURE_HALL, SEMINAR_ROOM 등)
  - FACILITY_DESC (시설 설명)
  - CAPACITY (수용 인원)
  - LOCATION (위치)
  - DEFAULT_EQUIPMENT (기본 장비)
  - IS_ACTIVE (활성 상태)
  - REQUIRES_APPROVAL (승인 필요 여부)

**🔄 DTO 변환 로직: FacilityService.java:60**
```java
private FacilityDto convertToDto(FacilityTbl facility)
```
1. 기본 시설 정보를 FacilityDto로 매핑
2. **추가 조회**: `FacilityBlockRepository.findActiveFacilityBlocks()` 호출
   - **접근 DB**: `FACILITY_BLOCK_TBL`
   - **조회 조건**:
     ```sql
     FACILITY_IDX = :facilityIdx
     AND BLOCK_END > :now (현재 시간 이후)
     ```
   - **확인 데이터**: 현재 활성화된 차단 정보
3. 차단 정보가 있으면 DTO에 차단 상태 설정 (isBlocked=true, blockReason)

---

### 1.2 시설 유형별 조회 (`POST /api/facilities/type/{facilityType}`)

#### 호출 흐름
```
Client → FacilityController.getFacilitiesByType()
         → FacilityService.getFacilitiesByType()
         → FacilityRepository.findByFacilityTypeAndActive()
```

**🔧 Service: FacilityService.java:40**
```java
public List<FacilityDto> getFacilitiesByType(FacilityType facilityType)
```

**💾 Repository: FacilityRepository.java:24**
```java
@Query("SELECT f FROM FacilityTbl f WHERE f.facilityType = :facilityType AND f.isActive = true")
List<FacilityTbl> findByFacilityTypeAndActive(@Param("facilityType") String facilityType)
```
- **접근 DB**: `FACILITY_TBL`
- **조회 조건**:
  - `FACILITY_TYPE = :facilityType` (예: 'LECTURE_HALL', 'SEMINAR_ROOM')
  - `IS_ACTIVE = true`

---

### 1.3 시설 검색 (`POST /api/facilities/search?keyword=...`)

**💾 Repository: FacilityRepository.java:27**
```java
@Query("SELECT f FROM FacilityTbl f WHERE (f.facilityName LIKE CONCAT('%', :keyword, '%')
       OR f.location LIKE CONCAT('%', :keyword, '%')) AND f.isActive = true")
List<FacilityTbl> searchFacilities(@Param("keyword") String keyword)
```
- **접근 DB**: `FACILITY_TBL`
- **검색 필드**:
  - `FACILITY_NAME` (시설명)
  - `LOCATION` (위치)
- **조건**: `IS_ACTIVE = true`

---

## 2. 시설 가용성 확인 플로우

### API: `POST /api/facilities/{facilityIdx}/availability`

#### 호출 흐름
```
Client → FacilityController.checkAvailability()
         → FacilityReservationService.checkAvailability()
         → checkAvailabilityInternal()
         → FacilityRepository.findById()
         → FacilityBlockRepository.findConflictingBlocks()
         → FacilityReservationRepository.findConflictingReservations()
```

#### 클래스별 상세 동작

**🎯 Controller: FacilityController.java:60**
```java
public ResponseEntity<ApiResponse<FacilityAvailabilityDto>> checkAvailability(
    @PathVariable Integer facilityIdx,
    @RequestParam LocalDateTime startTime,
    @RequestParam LocalDateTime endTime)
```
- **입력**:
  - facilityIdx: 시설 ID
  - startTime: 예약 시작 시간 (ISO DateTime 형식)
  - endTime: 예약 종료 시간

**🔧 Service: FacilityReservationService.java:143**
```java
public FacilityAvailabilityDto checkAvailability(Integer facilityIdx,
    LocalDateTime startTime, LocalDateTime endTime)
```
- **내부 호출**: `checkAvailabilityInternal(facilityIdx, startTime, endTime, null)`

**🔍 내부 검증 로직: FacilityReservationService.java:159**
```java
private FacilityAvailabilityDto checkAvailabilityInternal(
    Integer facilityIdx, LocalDateTime startTime, LocalDateTime endTime,
    Integer excludeReservationIdx)
```

**단계 1: 시설 존재 여부 확인**
```java
FacilityTbl facility = facilityRepository.findById(facilityIdx)
```
- **접근 DB**: `FACILITY_TBL`
- **조회 조건**: `FACILITY_IDX = :facilityIdx`
- **예외**: 시설이 없으면 `ResourceNotFoundException` 발생

**단계 2: 차단 기간 확인**
```java
List<FacilityBlockTbl> blocks = blockRepository.findConflictingBlocks(
    facilityIdx, startTime, endTime)
```
- **접근 DB**: `FACILITY_BLOCK_TBL`
- **조회 쿼리**:
  ```sql
  SELECT * FROM FACILITY_BLOCK_TBL
  WHERE FACILITY_IDX = :facilityIdx
  AND ((BLOCK_START < :endTime AND BLOCK_END > :startTime))
  ```
- **확인 데이터**:
  - BLOCK_START (차단 시작 시간)
  - BLOCK_END (차단 종료 시간)
  - BLOCK_REASON (차단 사유)
  - BLOCK_TYPE (차단 유형: MAINTENANCE 등)
- **처리**: 차단이 있으면 예외 발생 with 차단 사유

**단계 3: 예약 충돌 확인**
```java
List<String> activeStatuses = Arrays.asList(
    ReservationStatus.PENDING.toDbValue(),  // "PENDING"
    ReservationStatus.APPROVED.toDbValue()   // "APPROVED"
);

List<FacilityReservationTbl> conflicts =
    reservationRepository.findConflictingReservations(
        facilityIdx, startTime, endTime, activeStatuses, excludeReservationIdx)
```

**💾 Repository: FacilityReservationRepository.java:23**
```java
@Query("SELECT r FROM FacilityReservationTbl r WHERE r.facilityIdx = :facilityIdx
       AND r.status IN :statuses
       AND (:excludeReservationIdx IS NULL OR r.reservationIdx <> :excludeReservationIdx)
       AND ((r.startTime < :endTime AND r.endTime > :startTime))")
List<FacilityReservationTbl> findConflictingReservations(...)
```
- **접근 DB**: `FACILITY_RESERVATION_TBL`
- **조회 조건**:
  - `FACILITY_IDX = :facilityIdx`
  - `STATUS IN ('PENDING', 'APPROVED')` - 활성 예약만
  - `RESERVATION_IDX != :excludeReservationIdx` (수정 시 자기 제외)
  - 시간 중첩 조건: `(START_TIME < :endTime AND END_TIME > :startTime)`
- **확인 데이터**:
  - RESERVATION_IDX
  - START_TIME (예약 시작 시간)
  - END_TIME (예약 종료 시간)
  - STATUS (예약 상태)

**단계 4: 결과 반환**
```java
boolean isAvailable = conflicts.isEmpty();
List<TimeSlot> conflictingSlots = conflicts.stream()
    .map(r -> new TimeSlot(r.getStartTime(), r.getEndTime()))
    .collect(Collectors.toList());

return new FacilityAvailabilityDto(
    facilityIdx, facilityName, isAvailable, conflictingSlots)
```

---

## 3. 예약 신청 플로우

### API: `POST /api/reservations`

#### 호출 흐름
```
Client → FacilityReservationController.createReservation()
         → JwtUtil.getUserCodeFromToken() (JWT 토큰에서 사용자 인증)
         → FacilityReservationService.createReservation()
         → validateReservationRequest() (요청 유효성 검증)
         → FacilityRepository.findByIdWithLock() (비관적 락)
         → checkAvailability() (가용성 재확인)
         → FacilityReservationRepository.save()
         → createLog() (예약 로그 저장)
```

#### 클래스별 상세 동작

**🎯 Controller: FacilityReservationController.java:32**
```java
public ResponseEntity<ApiResponse<ReservationDto>> createReservation(
    @Valid @RequestBody ReservationCreateRequestDto request,
    HttpServletRequest httpRequest)
```
- **입력**: ReservationCreateRequestDto
  ```java
  {
    "facilityIdx": Integer,      // 시설 ID
    "startTime": LocalDateTime,  // 예약 시작 시간
    "endTime": LocalDateTime,    // 예약 종료 시간
    "partySize": Integer,        // 인원수
    "purpose": String,           // 사용 목적
    "requestedEquipment": String // 요청 장비
  }
  ```

**🔐 인증: FacilityReservationController.java:84**
```java
private String getUserCodeFromToken(HttpServletRequest request) {
    String token = jwtUtil.resolveToken(request);
    return jwtUtil.getUserCode(token);
}
```
- **역할**: HTTP 헤더에서 JWT 토큰 추출 및 사용자 코드 획득
- **반환**: userCode (학번/교수번호)

---

**🔧 Service: FacilityReservationService.java:63**
```java
public ReservationDto createReservation(String userCode,
    ReservationCreateRequestDto request)
```

### 🔍 단계 1: 요청 유효성 검증

**FacilityReservationService.java:273**
```java
private void validateReservationRequest(ReservationCreateRequestDto request)
```

**검증 항목**:

1. **시작 시간 유효성**
   - 조건: `startTime >= 현재 시간`
   - 예외: "시작 시간은 현재 시간 이후여야 합니다."

2. **종료 시간 유효성**
   - 조건: `endTime > startTime`
   - 예외: "종료 시간은 시작 시간 이후여야 합니다."

3. **예약 가능 기간**
   - 정책: `ReservationPolicyProperties.maxDaysInAdvance` (기본값: 30일)
   - 조건: `startTime <= 현재시간 + maxDaysInAdvance일`
   - 예외: "30일 이내의 예약만 가능합니다."

4. **최소 예약 시간**
   - 정책: `ReservationPolicyProperties.minDurationMinutes` (기본값: 30분)
   - 조건: `duration >= minDurationMinutes`
   - 예외: "최소 예약 시간은 30분입니다."

5. **최대 예약 시간**
   - 정책: `ReservationPolicyProperties.maxDurationMinutes` (기본값: 480분 = 8시간)
   - 조건: `duration <= maxDurationMinutes`
   - 예외: "최대 예약 시간은 480분입니다."

---

### 🔍 단계 2: 시설 조회 (동시성 제어)

**FacilityReservationService.java:68**
```java
FacilityTbl facility = facilityRepository.findByIdWithLock(request.getFacilityIdx())
    .orElseThrow(() -> ResourceNotFoundException.forId("시설", request.getFacilityIdx()))
```

**💾 Repository: FacilityRepository.java:38**
```java
@Lock(LockModeType.PESSIMISTIC_WRITE)
@QueryHints({@QueryHint(name = "javax.persistence.lock.timeout", value = "3000")})
@Query("SELECT f FROM FacilityTbl f WHERE f.facilityIdx = :facilityIdx")
Optional<FacilityTbl> findByIdWithLock(@Param("facilityIdx") Integer facilityIdx)
```

**⚠️ 동시성 제어**:
- **락 타입**: `PESSIMISTIC_WRITE` (비관적 쓰기 락)
- **락 타임아웃**: 3초 (3000ms)
- **역할**: 같은 시설에 대한 동시 예약 요청 차단
- **접근 DB**: `FACILITY_TBL`
- **잠금 범위**: 해당 시설 레코드
- **처리**: 다른 트랜잭션이 이 시설을 예약하려면 현재 트랜잭션 완료까지 대기

**시설 활성 상태 확인**
```java
if (!facility.getIsActive()) {
    throw new RuntimeException("사용 중지된 시설입니다.");
}
```

---

### 🔍 단계 3: 가용성 재확인 (Race Condition 방지)

**FacilityReservationService.java:76**
```java
FacilityAvailabilityDto availability = checkAvailability(
    request.getFacilityIdx(), request.getStartTime(), request.getEndTime())
```

- **역할**: 락을 획득한 상태에서 최종 가용성 확인
- **이유**: 요청 시점과 처리 시점 사이의 데이터 변경 감지
- **확인 내용**:
  1. 차단 기간 충돌 여부
  2. 다른 예약과의 시간 충돌 여부

**실패 시 처리**:
```java
if (!Boolean.TRUE.equals(availability.getIsAvailable())) {
    List<TimeSlot> conflicts = availability.getConflictingReservations();
    if (conflicts != null && !conflicts.isEmpty()) {
        TimeSlot conflict = conflicts.get(0);
        throw new RuntimeException(String.format(
            "해당 시간에는 이미 다른 예약이 존재합니다. 충돌 시간: %s ~ %s",
            conflict.getStartTime(), conflict.getEndTime()));
    }
}
```

---

### 🔍 단계 4: 승인 정책 결정

**FacilityReservationService.java:100**
```java
ReservationStatus initialStatus;
String logEventType;

if (Boolean.TRUE.equals(facility.getRequiresApproval())) {
    // 관리자 승인 필요
    initialStatus = ReservationStatus.PENDING;
    logEventType = "CREATED";
} else {
    // 즉시 예약 (자동 승인)
    initialStatus = ReservationStatus.APPROVED;
    logEventType = "AUTO_APPROVED";
}
```

**승인 정책 분기**:
- **시설의 `REQUIRES_APPROVAL = true`**:
  - 초기 상태: `PENDING` (승인 대기)
  - 로그 이벤트: `CREATED`
  - 메시지: "예약이 생성되었습니다. 관리자 승인 대기 중입니다."

- **시설의 `REQUIRES_APPROVAL = false`**:
  - 초기 상태: `APPROVED` (자동 승인)
  - 로그 이벤트: `AUTO_APPROVED`
  - 승인자: `SYSTEM`
  - 승인 시간: 현재 시간
  - 메시지: "예약이 자동으로 승인되었습니다."

---

### 🔍 단계 5: 예약 엔티티 생성 및 저장

**FacilityReservationService.java:116**
```java
FacilityReservationTbl reservation = new FacilityReservationTbl();
reservation.setFacilityIdx(request.getFacilityIdx());
reservation.setUserCode(userCode);
reservation.setStartTime(request.getStartTime());
reservation.setEndTime(request.getEndTime());
reservation.setPartySize(request.getPartySize());
reservation.setPurpose(request.getPurpose());
reservation.setRequestedEquipment(request.getRequestedEquipment());
reservation.setStatusEnum(initialStatus);

// 자동 승인인 경우
if (initialStatus == ReservationStatus.APPROVED) {
    reservation.setApprovedBy("SYSTEM");
    reservation.setApprovedAt(LocalDateTime.now());
}

FacilityReservationTbl saved = reservationRepository.save(reservation);
```

**💾 저장 DB**: `FACILITY_RESERVATION_TBL`
**저장 데이터**:
- FACILITY_IDX: 시설 ID
- USER_CODE: 예약자 코드 (JWT에서 추출)
- START_TIME: 예약 시작 시간
- END_TIME: 예약 종료 시간
- PARTY_SIZE: 인원수
- PURPOSE: 사용 목적
- REQUESTED_EQUIPMENT: 요청 장비
- STATUS: 예약 상태 (PENDING 또는 APPROVED)
- APPROVED_BY: 승인자 (자동 승인 시 'SYSTEM')
- APPROVED_AT: 승인 시간 (자동 승인 시 현재 시간)
- CREATED_AT: 생성 시간 (자동 설정 by @PrePersist)
- UPDATED_AT: 수정 시간 (자동 설정 by @PrePersist)

---

### 🔍 단계 6: 감사 로그 생성

**FacilityReservationService.java:134**
```java
createLog(saved.getReservationIdx(), logEventType, "USER", userCode, request);
```

**FacilityReservationService.java:301**
```java
private void createLog(Integer reservationIdx, String eventType,
    String actorType, String actorCode, Object payload) {
    String payloadJson = payload != null ? objectMapper.writeValueAsString(payload) : null;
    FacilityReservationLog log = new FacilityReservationLog(
        reservationIdx, eventType, actorType, actorCode, payloadJson);
    logRepository.save(log);
}
```

**💾 저장 DB**: `FACILITY_RESERVATION_LOG`
**저장 데이터**:
- RESERVATION_IDX: 예약 ID
- EVENT_TYPE: 이벤트 유형 (CREATED 또는 AUTO_APPROVED)
- ACTOR_TYPE: 행위자 유형 ('USER')
- ACTOR_CODE: 행위자 코드 (userCode)
- PAYLOAD: 요청 데이터 JSON
- EVENT_TIME: 이벤트 발생 시간 (자동 설정)

---

### 🔍 단계 7: DTO 변환 및 응답

**FacilityReservationService.java:140**
```java
return convertToDto(saved);
```

**FacilityReservationService.java:328-378**
```java
private ReservationDto convertToDto(FacilityReservationTbl reservation) {
    // 시설명 조회
    String facilityName = facilityRepository.findById(reservation.getFacilityIdx())
        .map(FacilityTbl::getFacilityName)
        .orElse("Unknown");

    // 사용자명 조회
    String userName = userRepository.findByUserCode(reservation.getUserCode())
        .map(UserTbl::getUserName)
        .orElse("Unknown");

    return new ReservationDto(...);
}
```

**추가 DB 조회**:
1. **FACILITY_TBL**: 시설명 조회
   - 조건: `FACILITY_IDX = reservation.facilityIdx`
   - 반환: `FACILITY_NAME`

2. **USER_TBL**: 사용자명 조회
   - 조건: `USER_CODE = reservation.userCode`
   - 반환: `USER_NAME`

**최종 응답 DTO**:
```java
ReservationDto {
    reservationIdx,
    facilityIdx,
    facilityName,        // 추가 조회
    userCode,
    userName,            // 추가 조회
    startTime,
    endTime,
    partySize,
    purpose,
    requestedEquipment,
    status,              // PENDING 또는 APPROVED
    adminNote,
    rejectionReason,
    approvedBy,          // 자동 승인 시 'SYSTEM'
    approvedAt,          // 자동 승인 시 현재 시간
    createdAt
}
```

---

## 4. 내 예약 조회 플로우

### 4.1 전체 예약 조회 (`POST /api/reservations/my`)

#### 호출 흐름
```
Client → FacilityReservationController.getMyReservations()
         → JwtUtil.getUserCodeFromToken()
         → FacilityReservationService.getMyReservations()
         → FacilityReservationRepository.findByUserCodeOrderByCreatedAtDesc()
         → convertToDtoList()
```

**🔧 Service: FacilityReservationService.java:221**
```java
public List<ReservationDto> getMyReservations(String userCode) {
    List<FacilityReservationTbl> reservations =
        reservationRepository.findByUserCodeOrderByCreatedAtDesc(userCode);
    return convertToDtoList(reservations);
}
```

**💾 Repository: FacilityReservationRepository.java:19**
```java
List<FacilityReservationTbl> findByUserCodeOrderByCreatedAtDesc(String userCode)
```
- **접근 DB**: `FACILITY_RESERVATION_TBL`
- **조회 조건**: `USER_CODE = :userCode`
- **정렬**: `CREATED_AT DESC` (최신순)
- **반환 데이터**: 사용자의 모든 예약 (모든 상태 포함)

---

### 4.2 상태별 예약 조회 (`POST /api/reservations/my/status/{status}`)

**💾 Repository: FacilityReservationRepository.java:21**
```java
List<FacilityReservationTbl> findByUserCodeAndStatusOrderByCreatedAtDesc(
    String userCode, String status)
```
- **접근 DB**: `FACILITY_RESERVATION_TBL`
- **조회 조건**:
  - `USER_CODE = :userCode`
  - `STATUS = :status` (PENDING, APPROVED, REJECTED, CANCELLED, COMPLETED 중 하나)
- **정렬**: `CREATED_AT DESC`

---

### 4.3 특정 예약 상세 조회 (`POST /api/reservations/{reservationIdx}`)

**🔧 Service: FacilityReservationService.java:241**
```java
public ReservationDto getReservationById(Integer reservationIdx, String userCode) {
    FacilityReservationTbl reservation =
        reservationRepository.findById(reservationIdx)
            .orElseThrow(() -> ResourceNotFoundException.forId("예약", reservationIdx));

    if (!reservation.getUserCode().equals(userCode)) {
        throw new RuntimeException("본인의 예약만 조회할 수 있습니다.");
    }

    return convertToDto(reservation);
}
```

**💾 Repository**: JPA 기본 메서드
```java
Optional<FacilityReservationTbl> findById(Integer reservationIdx)
```
- **접근 DB**: `FACILITY_RESERVATION_TBL`
- **조회 조건**: `RESERVATION_IDX = :reservationIdx`
- **권한 확인**: 예약자 본인만 조회 가능 (userCode 일치 확인)

---

### 4.4 예약 목록 DTO 변환 최적화

**FacilityReservationService.java:313**
```java
private List<ReservationDto> convertToDtoList(List<FacilityReservationTbl> reservations) {
    Map<Integer, String> facilityNameCache = new HashMap<>();
    Map<String, String> userNameCache = new HashMap<>();

    return reservations.stream()
        .map(reservation -> convertToDto(reservation, facilityNameCache, userNameCache))
        .collect(Collectors.toList());
}
```

**최적화 전략**:
- **캐싱**: 시설명과 사용자명을 Map에 캐싱하여 중복 DB 조회 방지
- **Lazy Loading**: 필요할 때만 DB 조회
- **computeIfAbsent 사용**:
  ```java
  String facilityName = facilityNameCache.computeIfAbsent(
      reservation.getFacilityIdx(),
      idx -> facilityRepository.findById(idx)
          .map(FacilityTbl::getFacilityName)
          .orElse("Unknown")
  );
  ```

**추가 DB 조회**:
- **FACILITY_TBL**: 각 고유 시설 ID당 1회만 조회
- **USER_TBL**: 각 고유 사용자 코드당 1회만 조회

---

## 5. 예약 취소 플로우

### API: `DELETE /api/reservations/{reservationIdx}`

#### 호출 흐름
```
Client → FacilityReservationController.cancelReservation()
         → JwtUtil.getUserCodeFromToken()
         → FacilityReservationService.cancelReservation()
         → FacilityReservationRepository.findById()
         → FacilityReservationRepository.save() (상태 업데이트)
         → createLog()
```

**🔧 Service: FacilityReservationService.java:252**
```java
public void cancelReservation(Integer reservationIdx, String userCode) {
    // 1. 예약 조회
    FacilityReservationTbl reservation =
        reservationRepository.findById(reservationIdx)
            .orElseThrow(() -> ResourceNotFoundException.forId("예약", reservationIdx));

    // 2. 권한 확인
    if (!reservation.getUserCode().equals(userCode)) {
        throw new RuntimeException("본인의 예약만 취소할 수 있습니다.");
    }

    // 3. 취소 가능 상태 확인
    if (reservation.getStatusEnum() != ReservationStatus.PENDING &&
        reservation.getStatusEnum() != ReservationStatus.APPROVED) {
        throw new RuntimeException("취소할 수 없는 상태입니다.");
    }

    // 4. 상태 변경 및 저장
    reservation.setStatusEnum(ReservationStatus.CANCELLED);
    reservationRepository.save(reservation);

    // 5. 감사 로그
    createLog(reservationIdx, "CANCELLED", "USER", userCode, null);
}
```

**취소 가능 조건**:
- ✅ 상태가 `PENDING` (승인 대기)
- ✅ 상태가 `APPROVED` (승인 완료)
- ❌ 상태가 `REJECTED` (거절됨 - 이미 처리 완료)
- ❌ 상태가 `CANCELLED` (이미 취소됨)
- ❌ 상태가 `COMPLETED` (사용 완료)

**💾 업데이트 DB**: `FACILITY_RESERVATION_TBL`
**업데이트 필드**:
- STATUS: `CANCELLED`
- UPDATED_AT: 현재 시간 (자동 설정 by @PreUpdate)

**💾 로그 DB**: `FACILITY_RESERVATION_LOG`
**로그 데이터**:
- EVENT_TYPE: `CANCELLED`
- ACTOR_TYPE: `USER`
- ACTOR_CODE: userCode
- EVENT_TIME: 현재 시간

---

## 6. 데이터베이스 스키마 요약

### 6.1 FACILITY_TBL (시설 정보)

| 컬럼명 | 타입 | 설명 | 제약조건 |
|--------|------|------|----------|
| FACILITY_IDX | INT | 시설 ID | PK, AUTO_INCREMENT |
| FACILITY_NAME | VARCHAR(100) | 시설명 | NOT NULL |
| FACILITY_TYPE | VARCHAR(20) | 시설 유형 | NOT NULL |
| FACILITY_DESC | TEXT | 시설 설명 | |
| CAPACITY | INT | 수용 인원 | |
| LOCATION | VARCHAR(200) | 위치 | |
| DEFAULT_EQUIPMENT | TEXT | 기본 장비 | |
| IS_ACTIVE | BOOLEAN | 활성 상태 | NOT NULL, DEFAULT true |
| REQUIRES_APPROVAL | BOOLEAN | 승인 필요 여부 | NOT NULL, DEFAULT true |
| CREATED_AT | DATETIME | 생성 시간 | NOT NULL |
| UPDATED_AT | DATETIME | 수정 시간 | NOT NULL |

**인덱스 권장사항**:
- `IS_ACTIVE` (조회 최적화)
- `FACILITY_TYPE, IS_ACTIVE` (복합 인덱스)

---

### 6.2 FACILITY_RESERVATION_TBL (시설 예약 정보)

| 컬럼명 | 타입 | 설명 | 제약조건 |
|--------|------|------|----------|
| RESERVATION_IDX | INT | 예약 ID | PK, AUTO_INCREMENT |
| FACILITY_IDX | INT | 시설 ID | NOT NULL, FK |
| USER_CODE | VARCHAR(50) | 예약자 코드 | NOT NULL, FK |
| START_TIME | DATETIME | 예약 시작 시간 | NOT NULL |
| END_TIME | DATETIME | 예약 종료 시간 | NOT NULL |
| PARTY_SIZE | INT | 인원수 | |
| PURPOSE | TEXT | 사용 목적 | |
| REQUESTED_EQUIPMENT | TEXT | 요청 장비 | |
| STATUS | VARCHAR(20) | 예약 상태 | NOT NULL, DEFAULT 'PENDING' |
| ADMIN_NOTE | TEXT | 관리자 메모 | |
| REJECTION_REASON | TEXT | 거절 사유 | |
| APPROVED_BY | VARCHAR(50) | 승인자 | |
| APPROVED_AT | DATETIME | 승인 시간 | |
| CREATED_AT | DATETIME | 생성 시간 | NOT NULL |
| UPDATED_AT | DATETIME | 수정 시간 | NOT NULL |

**인덱스 권장사항**:
- `USER_CODE, CREATED_AT DESC` (내 예약 조회)
- `USER_CODE, STATUS, CREATED_AT DESC` (상태별 조회)
- `FACILITY_IDX, STATUS, START_TIME, END_TIME` (충돌 확인)
- `STATUS, END_TIME` (만료 예약 조회)

**상태 값**:
- `PENDING`: 승인 대기
- `APPROVED`: 승인 완료
- `REJECTED`: 거절됨
- `CANCELLED`: 취소됨
- `COMPLETED`: 사용 완료

---

### 6.3 FACILITY_BLOCK_TBL (시설 차단 정보)

| 컬럼명 | 타입 | 설명 | 제약조건 |
|--------|------|------|----------|
| BLOCK_IDX | INT | 차단 ID | PK, AUTO_INCREMENT |
| FACILITY_IDX | INT | 시설 ID | NOT NULL, FK |
| BLOCK_START | DATETIME | 차단 시작 시간 | NOT NULL |
| BLOCK_END | DATETIME | 차단 종료 시간 | NOT NULL |
| BLOCK_REASON | VARCHAR(200) | 차단 사유 | NOT NULL |
| BLOCK_TYPE | VARCHAR(20) | 차단 유형 | DEFAULT 'MAINTENANCE' |
| CREATED_BY | VARCHAR(50) | 생성자 | NOT NULL |
| CREATED_AT | DATETIME | 생성 시간 | NOT NULL |

**인덱스 권장사항**:
- `FACILITY_IDX, BLOCK_END` (활성 차단 조회)
- `FACILITY_IDX, BLOCK_START, BLOCK_END` (충돌 확인)

---

### 6.4 FACILITY_RESERVATION_LOG (예약 감사 로그)

| 컬럼명 | 타입 | 설명 | 제약조건 |
|--------|------|------|----------|
| LOG_IDX | INT | 로그 ID | PK, AUTO_INCREMENT |
| RESERVATION_IDX | INT | 예약 ID | NOT NULL, FK |
| EVENT_TYPE | VARCHAR(50) | 이벤트 유형 | NOT NULL |
| ACTOR_TYPE | VARCHAR(20) | 행위자 유형 | NOT NULL |
| ACTOR_CODE | VARCHAR(50) | 행위자 코드 | NOT NULL |
| PAYLOAD | TEXT | 요청 데이터 JSON | |
| EVENT_TIME | DATETIME | 이벤트 발생 시간 | NOT NULL |

**이벤트 유형**:
- `CREATED`: 예약 생성 (사용자)
- `AUTO_APPROVED`: 자동 승인 (시스템)
- `APPROVED`: 관리자 승인
- `REJECTED`: 관리자 거절
- `CANCELLED`: 사용자 취소
- `COMPLETED`: 사용 완료 (스케줄러)

**인덱스 권장사항**:
- `RESERVATION_IDX, EVENT_TIME DESC` (예약별 이력 조회)

---

### 6.5 USER_TBL (사용자 정보)

| 컬럼명 | 타입 | 설명 | 제약조건 |
|--------|------|------|----------|
| USER_IDX | INT | 사용자 ID | PK, AUTO_INCREMENT |
| USER_CODE | VARCHAR(50) | 학번/교수번호 | NOT NULL, UNIQUE |
| USER_NAME | VARCHAR(50) | 사용자 이름 | NOT NULL |
| USER_EMAIL | VARCHAR(200) | 이메일 | NOT NULL |
| ... | ... | 기타 필드 | |

**예약 시스템 사용 필드**:
- USER_CODE: 예약자 식별 (FK)
- USER_NAME: 예약자명 표시

---

## 📊 데이터 흐름 다이어그램

### 예약 신청 플로우 요약

```
[클라이언트]
    ↓ POST /api/reservations
[FacilityReservationController]
    ↓ JWT 토큰 검증 → userCode 추출
[FacilityReservationService.createReservation()]
    ↓
    ├─ 1. validateReservationRequest()
    │   └─ 시간/기간/정책 검증
    ↓
    ├─ 2. FacilityRepository.findByIdWithLock()
    │   └─ FACILITY_TBL 조회 (비관적 락)
    │       └─ IS_ACTIVE, REQUIRES_APPROVAL 확인
    ↓
    ├─ 3. checkAvailability()
    │   ├─ FacilityBlockRepository.findConflictingBlocks()
    │   │   └─ FACILITY_BLOCK_TBL 조회 (차단 확인)
    │   └─ FacilityReservationRepository.findConflictingReservations()
    │       └─ FACILITY_RESERVATION_TBL 조회 (예약 충돌 확인)
    ↓
    ├─ 4. 승인 정책 결정
    │   ├─ REQUIRES_APPROVAL=true → STATUS=PENDING
    │   └─ REQUIRES_APPROVAL=false → STATUS=APPROVED
    ↓
    ├─ 5. FacilityReservationRepository.save()
    │   └─ FACILITY_RESERVATION_TBL INSERT
    ↓
    ├─ 6. createLog()
    │   └─ FACILITY_RESERVATION_LOG INSERT
    ↓
    └─ 7. convertToDto()
        ├─ FacilityRepository.findById() → 시설명 조회
        ├─ UserRepository.findByUserCode() → 사용자명 조회
        └─ ReservationDto 반환
```

---

## 🔐 보안 및 동시성 제어

### 1. 인증 및 권한
- **JWT 토큰 기반 인증**: 모든 예약 API는 JWT 토큰 필수
- **사용자 코드 추출**: JwtUtil을 통해 토큰에서 userCode 추출
- **권한 검증**: 본인 예약만 조회/취소 가능

### 2. 동시성 제어 (Race Condition 방지)
- **비관적 락**: 예약 생성 시 `findByIdWithLock()` 사용
- **락 범위**: 시설(FACILITY_TBL) 레코드 단위
- **락 타임아웃**: 3초
- **효과**: 동일 시설에 대한 동시 예약 요청 직렬화

### 3. 데이터 무결성
- **트랜잭션**: `@Transactional` 어노테이션으로 원자성 보장
- **재확인 로직**: 락 획득 후 가용성 재확인
- **상태 검증**: 취소 시 상태 확인 (PENDING, APPROVED만 허용)

---

## 📝 정책 설정 (ReservationPolicyProperties)

| 정책 | 기본값 | 설명 |
|------|--------|------|
| maxDaysInAdvance | 30일 | 최대 예약 가능 기간 |
| minDurationMinutes | 30분 | 최소 예약 시간 |
| maxDurationMinutes | 480분 (8시간) | 최대 예약 시간 |

**설정 파일**: `application.properties` 또는 `ReservationPolicyProperties.java`

---

## 🎯 핵심 기능 정리

### 예약 신청 프로세스
1. ✅ JWT 토큰 검증 및 사용자 인증
2. ✅ 요청 유효성 검증 (시간, 기간, 정책)
3. ✅ 시설 존재 및 활성 상태 확인 (비관적 락)
4. ✅ 차단 기간 확인
5. ✅ 예약 충돌 확인
6. ✅ 승인 정책에 따른 상태 결정 (자동/수동)
7. ✅ 예약 저장
8. ✅ 감사 로그 저장
9. ✅ DTO 변환 및 응답

### 데이터베이스 접근 패턴
- **시설 조회**: FACILITY_TBL (필터링: IS_ACTIVE, FACILITY_TYPE)
- **가용성 확인**: FACILITY_BLOCK_TBL + FACILITY_RESERVATION_TBL
- **예약 조회**: FACILITY_RESERVATION_TBL (필터링: USER_CODE, STATUS)
- **예약 생성**: FACILITY_RESERVATION_TBL + FACILITY_RESERVATION_LOG
- **예약 취소**: FACILITY_RESERVATION_TBL 업데이트 + 로그

---

## 🚀 성능 최적화 포인트

1. **캐싱**: 시설명/사용자명 캐싱으로 중복 조회 방지
2. **인덱스**: 주요 조회 조건에 인덱스 설정
3. **비관적 락**: 필요한 경우에만 사용 (예약 생성 시)
4. **배치 조회**: 목록 조회 시 N+1 문제 방지

---

**문서 작성일**: 2025-10-08
**작성자**: Claude Code Analysis
**버전**: 1.0
