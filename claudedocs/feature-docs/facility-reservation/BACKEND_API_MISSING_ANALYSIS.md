# 시설물 예약 백엔드 API 누락/개선 사항 분석

**분석일:** 2025-10-10  
**분석 대상:** 시설 예약 시스템 백엔드 API

---

## 📋 Executive Summary

### 현재 상태
- ✅ **구현 완료:** 15개 API
- ❌ **누락:** 2개 API
- ⚠️ **개선 필요:** 3개 API

---

## ❌ 누락된 API (2개)

### 1. 관리자: 전체 예약 목록 조회 API

**문제:**
- 현재 `/api/admin/reservations/pending`만 있음 (승인 대기만)
- **전체 예약 목록** 조회 API가 없음

**필요성:**
- 관리자가 모든 예약 상태를 한 번에 볼 수 없음
- 승인됨(APPROVED), 반려됨(REJECTED), 취소됨(CANCELLED) 예약도 봐야 함
- 통계나 이력 관리를 위해 필수

**제안:**
```java
// AdminFacilityReservationController.java

@PostMapping("/all")
public ResponseEntity<ApiResponse<List<ReservationDto>>> getAllReservations(
        @RequestParam(required = false) String status,
        @RequestParam(required = false) Integer facilityIdx,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
        HttpServletRequest request) {
    String adminId = getAdminIdFromToken(request);
    List<ReservationDto> reservations = adminReservationService.getAllReservations(
        adminId, status, facilityIdx, startDate, endDate);
    return ResponseEntity.ok(ApiResponse.success("예약 목록을 조회했습니다.", reservations));
}
```

**사용 예시:**
```http
# 전체 예약
POST /api/admin/reservations/all

# 특정 상태만
POST /api/admin/reservations/all?status=APPROVED

# 특정 시설만
POST /api/admin/reservations/all?facilityIdx=1

# 기간 필터
POST /api/admin/reservations/all?startDate=2025-10-01T00:00:00&endDate=2025-10-31T23:59:59
```

**우선순위:** 🔴 **High** - 관리자 기능 필수

---

### 2. 관리자: 시설 관리 API (CRUD)

**문제:**
- 시설 조회 API만 있고, **시설 생성/수정/삭제 API가 없음**
- 관리자가 시설을 추가하거나 수정할 수 없음

**필요한 API:**

#### 2-1. 시설 생성
```java
// AdminFacilityController.java (새 컨트롤러 필요)

@PostMapping
public ResponseEntity<ApiResponse<FacilityDto>> createFacility(
        @Valid @RequestBody FacilityCreateRequestDto request,
        HttpServletRequest httpRequest) {
    String adminId = getAdminIdFromToken(httpRequest);
    FacilityDto facility = adminFacilityService.createFacility(adminId, request);
    return ResponseEntity.ok(ApiResponse.success("시설이 생성되었습니다.", facility));
}
```

#### 2-2. 시설 수정
```java
@PutMapping("/{facilityIdx}")
public ResponseEntity<ApiResponse<FacilityDto>> updateFacility(
        @PathVariable Integer facilityIdx,
        @Valid @RequestBody FacilityUpdateRequestDto request,
        HttpServletRequest httpRequest) {
    String adminId = getAdminIdFromToken(httpRequest);
    FacilityDto facility = adminFacilityService.updateFacility(adminId, facilityIdx, request);
    return ResponseEntity.ok(ApiResponse.success("시설 정보가 수정되었습니다.", facility));
}
```

#### 2-3. 시설 활성화/비활성화
```java
@PatchMapping("/{facilityIdx}/toggle-active")
public ResponseEntity<ApiResponse<FacilityDto>> toggleFacilityActive(
        @PathVariable Integer facilityIdx,
        HttpServletRequest httpRequest) {
    String adminId = getAdminIdFromToken(httpRequest);
    FacilityDto facility = adminFacilityService.toggleActive(adminId, facilityIdx);
    return ResponseEntity.ok(ApiResponse.success("시설 상태가 변경되었습니다.", facility));
}
```

#### 2-4. 시설 차단 기간 설정
```java
@PostMapping("/{facilityIdx}/block")
public ResponseEntity<ApiResponse<Void>> blockFacility(
        @PathVariable Integer facilityIdx,
        @Valid @RequestBody FacilityBlockRequestDto request,
        HttpServletRequest httpRequest) {
    String adminId = getAdminIdFromToken(httpRequest);
    adminFacilityService.blockFacility(adminId, facilityIdx, request);
    return ResponseEntity.ok(ApiResponse.success("시설 차단이 설정되었습니다."));
}
```

**Base Path:** `/api/admin/facilities`

**우선순위:** 🟡 **Medium** - 관리 편의성 향상

---

## ⚠️ 개선이 필요한 API (3개)

### 3. 예약 수정 API 없음

**문제:**
- 예약 생성, 조회, 취소는 있지만 **수정 API가 없음**
- 사용자가 시간이나 인원을 변경하려면 취소 후 재생성해야 함

**제안:**
```java
// FacilityReservationController.java

@PutMapping("/{reservationIdx}")
public ResponseEntity<ApiResponse<ReservationDto>> updateReservation(
        @PathVariable Integer reservationIdx,
        @Valid @RequestBody ReservationUpdateRequestDto request,
        HttpServletRequest httpRequest) {
    String userCode = getUserCodeFromToken(httpRequest);
    ReservationDto reservation = reservationService.updateReservation(
        userCode, reservationIdx, request);
    return ResponseEntity.ok(ApiResponse.success("예약이 수정되었습니다.", reservation));
}
```

**제약 조건:**
- PENDING 상태만 수정 가능
- APPROVED 상태는 수정 불가 (취소 후 재생성)
- 시간 변경 시 가용성 재확인 필수

**우선순위:** 🟡 **Medium** - 사용자 편의성 향상

---

### 4. 시설 정책 조회/수정 API 없음

**문제:**
- `FacilityPolicyTbl` 테이블이 있지만 API가 없음
- 관리자가 시설별 정책(승인 필요 여부, 최대 예약 시간 등)을 변경할 수 없음

**제안:**
```java
// AdminFacilityController.java

@GetMapping("/{facilityIdx}/policy")
public ResponseEntity<ApiResponse<FacilityPolicyDto>> getFacilityPolicy(
        @PathVariable Integer facilityIdx,
        HttpServletRequest request) {
    String adminId = getAdminIdFromToken(request);
    FacilityPolicyDto policy = adminFacilityService.getFacilityPolicy(adminId, facilityIdx);
    return ResponseEntity.ok(ApiResponse.success("시설 정책을 조회했습니다.", policy));
}

@PutMapping("/{facilityIdx}/policy")
public ResponseEntity<ApiResponse<FacilityPolicyDto>> updateFacilityPolicy(
        @PathVariable Integer facilityIdx,
        @Valid @RequestBody FacilityPolicyUpdateRequestDto request,
        HttpServletRequest httpRequest) {
    String adminId = getAdminIdFromToken(httpRequest);
    FacilityPolicyDto policy = adminFacilityService.updateFacilityPolicy(
        adminId, facilityIdx, request);
    return ResponseEntity.ok(ApiResponse.success("시설 정책이 수정되었습니다.", policy));
}
```

**정책 설정 항목:**
- `requiresApproval`: 승인 필요 여부
- `maxAdvanceDays`: 최대 예약 가능 일수
- `minDurationMinutes`: 최소 예약 시간
- `maxDurationMinutes`: 최대 예약 시간
- `allowMultiplePerDay`: 하루 여러 번 예약 허용 여부

**우선순위:** 🟢 **Low** - 운영 중 필요 시 추가

---

### 5. 예약 이력 조회 API 부족

**문제:**
- `FacilityReservationLog` 테이블이 있지만 조회 API가 없음
- 예약의 변경 이력을 확인할 수 없음

**제안:**
```java
// AdminFacilityReservationController.java

@GetMapping("/{reservationIdx}/logs")
public ResponseEntity<ApiResponse<List<ReservationLogDto>>> getReservationLogs(
        @PathVariable Integer reservationIdx,
        HttpServletRequest request) {
    String adminId = getAdminIdFromToken(request);
    List<ReservationLogDto> logs = adminReservationService.getReservationLogs(
        adminId, reservationIdx);
    return ResponseEntity.ok(ApiResponse.success("예약 이력을 조회했습니다.", logs));
}
```

**로그 항목:**
- `CREATED`: 예약 생성
- `AUTO_APPROVED`: 자동 승인
- `APPROVED`: 관리자 승인
- `REJECTED`: 반려
- `CANCELLED`: 취소
- `UPDATED`: 수정 (구현 시)

**우선순위:** 🟢 **Low** - 감사/추적 목적

---

## 📊 API 현황 정리

### 유저 API (10개) ✅

| No | Method | Endpoint | 상태 | 비고 |
|----|--------|----------|------|------|
| 1 | POST | `/api/facilities` | ✅ 완료 | 전체 시설 목록 |
| 2 | POST | `/api/facilities/type/{type}` | ✅ 완료 | 유형별 시설 |
| 3 | POST | `/api/facilities/{id}` | ✅ 완료 | 시설 상세 |
| 4 | POST | `/api/facilities/search?keyword=` | ✅ 완료 | 시설 검색 |
| 5 | POST | `/api/facilities/{id}/availability?...` | ✅ 완료 | 가용성 확인 |
| 6 | POST | `/api/reservations` | ✅ 완료 | 예약 생성 |
| 7 | POST | `/api/reservations/my` | ✅ 완료 | 내 예약 목록 |
| 8 | POST | `/api/reservations/my/status/{status}` | ✅ 완료 | 상태별 조회 |
| 9 | POST | `/api/reservations/{id}` | ✅ 완료 | 예약 상세 |
| 10 | DELETE | `/api/reservations/{id}` | ✅ 완료 | 예약 취소 |
| 11 | PUT | `/api/reservations/{id}` | ❌ 누락 | 예약 수정 |

### 관리자 API - 예약 관리 (5개)

| No | Method | Endpoint | 상태 | 비고 |
|----|--------|----------|------|------|
| 1 | POST | `/api/admin/reservations/pending` | ✅ 완료 | 승인 대기 목록 |
| 2 | POST | `/api/admin/reservations/pending/count` | ✅ 완료 | 대기 건수 |
| 3 | POST | `/api/admin/reservations/approve` | ✅ 완료 | 승인 |
| 4 | POST | `/api/admin/reservations/reject` | ✅ 완료 | 반려 |
| 5 | POST | `/api/admin/reservations/stats` | ✅ 완료 | 통계 |
| 6 | POST | `/api/admin/reservations/all` | ❌ 누락 | 전체 예약 목록 |
| 7 | GET | `/api/admin/reservations/{id}/logs` | ❌ 누락 | 예약 이력 |

### 관리자 API - 시설 관리 (0개) ❌

| No | Method | Endpoint | 상태 | 비고 |
|----|--------|----------|------|------|
| 1 | POST | `/api/admin/facilities` | ❌ 누락 | 시설 생성 |
| 2 | PUT | `/api/admin/facilities/{id}` | ❌ 누락 | 시설 수정 |
| 3 | PATCH | `/api/admin/facilities/{id}/toggle-active` | ❌ 누락 | 활성화 토글 |
| 4 | POST | `/api/admin/facilities/{id}/block` | ❌ 누락 | 차단 설정 |
| 5 | GET | `/api/admin/facilities/{id}/policy` | ❌ 누락 | 정책 조회 |
| 6 | PUT | `/api/admin/facilities/{id}/policy` | ❌ 누락 | 정책 수정 |

---

## 🎯 우선순위별 구현 계획

### Phase 1: 필수 기능 (즉시 구현) 🔴

#### 1. 관리자 전체 예약 목록 API
```
POST /api/admin/reservations/all
```

**구현 시간:** 2시간
**이유:** 관리자가 시스템 전체 예약을 볼 수 없음

**구현 내역:**
```java
// AdminFacilityReservationService.java
public List<ReservationDto> getAllReservations(
    String adminId, 
    String status, 
    Integer facilityIdx,
    LocalDateTime startDate, 
    LocalDateTime endDate) {
    
    validateAdmin(adminId);
    
    // 필터링 조건 적용
    List<FacilityReservationTbl> reservations;
    
    if (status != null && facilityIdx != null) {
        reservations = reservationRepository
            .findByStatusAndFacilityIdxOrderByCreatedAtDesc(status, facilityIdx);
    } else if (status != null) {
        reservations = reservationRepository
            .findByStatusOrderByCreatedAtDesc(status);
    } else if (facilityIdx != null) {
        reservations = reservationRepository
            .findByFacilityIdxOrderByCreatedAtDesc(facilityIdx);
    } else {
        reservations = reservationRepository
            .findAllByOrderByCreatedAtDesc();
    }
    
    // 날짜 필터링
    if (startDate != null && endDate != null) {
        reservations = reservations.stream()
            .filter(r -> !r.getStartTime().isBefore(startDate) 
                      && !r.getEndTime().isAfter(endDate))
            .collect(Collectors.toList());
    }
    
    return convertToDtoList(reservations);
}
```

---

### Phase 2: 편의 기능 (2주 내) 🟡

#### 2. 예약 수정 API
```
PUT /api/reservations/{id}
```

**구현 시간:** 4시간
**이유:** 사용자가 예약을 수정할 수 있어야 함

**제약 조건:**
- PENDING 상태만 수정 가능
- 시간 변경 시 가용성 재확인
- 승인된 예약은 수정 불가 (취소 후 재생성)

---

### Phase 3: 관리 기능 (1개월 내) 🟢

#### 3. 시설 관리 API
```
POST   /api/admin/facilities
PUT    /api/admin/facilities/{id}
PATCH  /api/admin/facilities/{id}/toggle-active
POST   /api/admin/facilities/{id}/block
```

**구현 시간:** 1일
**이유:** 관리자가 시설을 관리할 수 있어야 함

#### 4. 시설 정책 API
```
GET /api/admin/facilities/{id}/policy
PUT /api/admin/facilities/{id}/policy
```

**구현 시간:** 3시간
**이유:** 시설별 정책 설정

#### 5. 예약 이력 API
```
GET /api/admin/reservations/{id}/logs
```

**구현 시간:** 2시간
**이유:** 감사 추적

---

## 📝 리포지토리 메서드 추가 필요

### FacilityReservationRepository

```java
// 전체 예약 조회 (관리자용)
List<FacilityReservationTbl> findAllByOrderByCreatedAtDesc();

// 시설별 예약 조회
List<FacilityReservationTbl> findByFacilityIdxOrderByCreatedAtDesc(Integer facilityIdx);

// 상태 + 시설 필터
List<FacilityReservationTbl> findByStatusAndFacilityIdxOrderByCreatedAtDesc(
    String status, Integer facilityIdx);

// 예약 수정 시 본인 확인
Optional<FacilityReservationTbl> findByReservationIdxAndUserCode(
    Integer reservationIdx, String userCode);
```

---

## 🔧 DTO 추가 필요

### 1. FacilityCreateRequestDto
```java
public class FacilityCreateRequestDto {
    @NotBlank(message = "시설명은 필수입니다.")
    private String facilityName;
    
    @NotBlank(message = "시설 유형은 필수입니다.")
    private String facilityType;
    
    private String facilityDesc;
    
    @Min(value = 1, message = "수용 인원은 1명 이상이어야 합니다.")
    private Integer capacity;
    
    @NotBlank(message = "위치는 필수입니다.")
    private String location;
    
    private String defaultEquipment;
    
    private Boolean requiresApproval = true;
}
```

### 2. FacilityUpdateRequestDto
```java
public class FacilityUpdateRequestDto {
    private String facilityName;
    private String facilityDesc;
    private Integer capacity;
    private String location;
    private String defaultEquipment;
}
```

### 3. FacilityBlockRequestDto
```java
public class FacilityBlockRequestDto {
    @NotNull(message = "차단 시작 시간은 필수입니다.")
    private LocalDateTime blockStart;
    
    @NotNull(message = "차단 종료 시간은 필수입니다.")
    private LocalDateTime blockEnd;
    
    @NotBlank(message = "차단 사유는 필수입니다.")
    private String blockReason;
}
```

### 4. ReservationUpdateRequestDto
```java
public class ReservationUpdateRequestDto {
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Integer partySize;
    private String purpose;
    private String requestedEquipment;
}
```

### 5. ReservationLogDto
```java
public class ReservationLogDto {
    private Integer logIdx;
    private Integer reservationIdx;
    private String eventType;
    private String actorType;
    private String actorCode;
    private String payload;
    private LocalDateTime createdAt;
}
```

---

## 📌 요약

### 즉시 구현 필요 (Phase 1) 🔴
1. ✅ **관리자 전체 예약 목록 API** - 가장 중요!
   - `POST /api/admin/reservations/all`
   - 필터링: 상태, 시설, 날짜 범위

### 2주 내 구현 (Phase 2) 🟡
2. **예약 수정 API**
   - `PUT /api/reservations/{id}`

### 1개월 내 구현 (Phase 3) 🟢
3. **시설 관리 API** (6개)
   - CRUD + 차단 + 정책

4. **예약 이력 API**
   - 감사 추적

### 총 구현 필요
- **필수:** 1개 API
- **권장:** 11개 API
- **총:** 12개 API

---

## 🚀 다음 단계

### 즉시 작업
1. `AdminFacilityReservationService.getAllReservations()` 구현
2. `AdminFacilityReservationController.getAllReservations()` 추가
3. 필요한 리포지토리 메서드 추가
4. 테스트

### 문서 업데이트
- API 문서에 새 엔드포인트 추가
- Postman Collection 업데이트

---

**작성자:** GitHub Copilot  
**작성일:** 2025-10-10  
**상태:** 분석 완료, 구현 대기
