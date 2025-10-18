# 시설 예약 시스템 - 구현 전 사전 확인 사항

**작성일**: 2025-10-06
**목적**: 기존 시스템 확인 및 플랜 검증

---

## ✅ 사전 확인 완료 항목

### 1. UserTbl 필드 확인 ✅

**필요한 필드**: `userName`, `userCode`, `userEmail`

**확인 결과**: 모두 존재
```java
// UserTbl.java 확인 완료
@Column(name = "USER_NAME", nullable = false, length = 50)
private String userName;  // ✅ 존재

@Column(name = "USER_CODE", nullable = false)
private String userCode;  // ✅ 존재 (학번/교번)

@Column(name = "USER_EMAIL", nullable = false, length = 200)
private String userEmail;  // ✅ 존재
```

**플랜 검증**: JPQL JOIN 쿼리 정상 작동
```java
// FacilityReservationRepository.findPendingWithUserInfo()
JOIN UserTbl u ON r.userCode = u.userCode
// ✅ userCode로 조인 가능
// ✅ u.userName, u.userCode, u.userEmail 모두 사용 가능
```

---

### 2. AdminTbl 필드 확인 ✅

**필요한 필드**: `adminIdx`, `adminId`, `name`

**확인 결과**: 모두 존재, 추가 필드도 확인
```java
// AdminTbl.java 확인 완료
@Column(name = "ADMIN_IDX")
private Integer adminIdx;  // ✅ 존재 (PK)

@Column(name = "ADMIN_ID", nullable = false, unique = true, length = 100)
private String adminId;  // ✅ 존재 (로그인 ID, 이메일 형식)

@Column(name = "ADMIN_NAME", nullable = false, length = 100)
private String name;  // ✅ 존재 (관리자 이름)

// 추가 유용한 필드
@Column(name = "ADMIN_SYS", nullable = false)
private Integer adminSys = 0;  // 시스템 관리자 여부 (1: 시스템 관리자, 0: 일반)

@Column(name = "ADMIN_PHONE", length = 11)
private String adminPhone;  // 전화번호

@Column(name = "ADMIN_OFFICE", length = 11)
private String adminOffice;  // 사무실
```

**플랜 검증**: 관리자 정보 저장 가능
```java
// FacilityReservationTbl
@Column(name = "APPROVED_BY", length = 50)
private String approvedBy;  // ✅ adminId 저장 가능 (이메일 형식)
```

---

### 3. JWT 토큰 구조 확인 ✅

**JwtUtil 메서드 확인**

**사용자용**:
```java
// JwtUtil.java
public String generateAccessToken(String username, Integer userId) {
    Map<String, Object> claims = new HashMap<>();
    claims.put("userId", userId);  // ✅ UserTbl.userIdx
    claims.put("type", "access");
    return createToken(claims, username, expiration);
}

public Integer extractUserId(String token) {
    Claims claims = extractAllClaims(token);
    return Integer.valueOf(claims.get("userId").toString());  // ✅ 추출 가능
}
```

**관리자용**:
```java
// AdminService.java (확인 완료)
String accessToken = jwtUtil.generateAccessToken(admin.getAdminId(), admin.getAdminIdx());
// ✅ username = adminId (이메일 형식)
// ✅ userId = adminIdx (관리자 PK)
```

**플랜 검증**: 관리자 인증 로직 구현 가능
```java
// AdminFacilityReservationController에서 사용
private String getAdminCodeFromToken(HttpServletRequest request) {
    String token = extractToken(request);
    if (!jwtUtil.validateToken(token)) {
        throw new UnauthorizedException("관리자 인증이 필요합니다.");
    }

    // ✅ 방법 1: 토큰에서 adminIdx 추출 후 DB 조회
    Integer adminIdx = jwtUtil.extractUserId(token);  // adminIdx 반환
    AdminTbl admin = adminRepository.findById(adminIdx)
        .orElseThrow(() -> new UnauthorizedException("존재하지 않는 관리자입니다."));

    return admin.getAdminId();  // ✅ adminId (이메일) 반환

    // ✅ 방법 2: 토큰 subject에서 직접 추출
    // String adminId = jwtUtil.extractUsername(token);  // adminId 직접 반환
    // return adminId;
}
```

---

### 4. AdminTblRepository 확인 ✅

**확인 결과**: 이미 존재
```java
// AdminTblRepository.java
@Repository
public interface AdminTblRepository extends JpaRepository<AdminTbl, Integer> {
    Optional<AdminTbl> findByAdminId(String adminId);  // ✅ 사용 가능
    boolean existsByAdminId(String adminId);           // ✅ 사용 가능
}
```

**플랜 검증**: 추가 생성 불필요
```java
// AdminFacilityReservationController
@Autowired
private AdminTblRepository adminRepository;  // ✅ 바로 사용 가능
```

---

## 📋 구현 전 최종 체크리스트

### Phase 0: 환경 확인 (완료) ✅

- [x] **UserTbl 필드 확인**
  - [x] userName 존재
  - [x] userCode 존재
  - [x] userEmail 존재
  - [x] userCode FK 사용 가능

- [x] **AdminTbl 필드 확인**
  - [x] adminIdx 존재 (PK)
  - [x] adminId 존재 (로그인 ID, 이메일 형식)
  - [x] name 존재

- [x] **JWT 토큰 확인**
  - [x] JwtUtil.generateAccessToken() 사용 가능
  - [x] JwtUtil.extractUserId() 사용 가능
  - [x] 관리자 토큰: username=adminId, userId=adminIdx

- [x] **AdminTblRepository 확인**
  - [x] 이미 존재
  - [x] findById() 사용 가능
  - [x] findByAdminId() 사용 가능

---

## 🔧 구현 가이드

### 1. 관리자 인증 로직 구현 (추천 방법)

**방법 A: adminIdx로 조회 (권장)**
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
    private AdminTblRepository adminRepository;

    /**
     * JWT 토큰에서 관리자 ID(이메일) 추출
     *
     * @param request HTTP 요청
     * @return adminId (이메일 형식)
     * @throws UnauthorizedException 인증 실패 시
     */
    private String getAdminIdFromToken(HttpServletRequest request) {
        String token = extractToken(request);

        if (token == null || !jwtUtil.validateToken(token)) {
            throw new UnauthorizedException("관리자 인증이 필요합니다.");
        }

        // JWT 토큰에서 adminIdx 추출
        Integer adminIdx = jwtUtil.extractUserId(token);
        if (adminIdx == null) {
            throw new UnauthorizedException("토큰에서 관리자 정보를 찾을 수 없습니다.");
        }

        // AdminTbl 조회
        AdminTbl admin = adminRepository.findById(adminIdx)
            .orElseThrow(() -> new UnauthorizedException("존재하지 않는 관리자입니다."));

        return admin.getAdminId();  // 이메일 형식의 adminId 반환
    }

    /**
     * HTTP 요청에서 JWT 토큰 추출
     */
    private String extractToken(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }

    /**
     * 승인 대기 목록 조회
     */
    @PostMapping("/pending")
    public ResponseEntity<ApiResponse<List<AdminReservationDetailDto>>> getPending(
        HttpServletRequest request
    ) {
        try {
            // 관리자 인증 확인
            getAdminIdFromToken(request);

            List<AdminReservationDetailDto> result = adminService.getPendingReservations();

            return ResponseEntity.ok(
                new ApiResponse<>(true, "대기 목록을 조회했습니다.", result)
            );

        } catch (UnauthorizedException e) {
            log.warn("관리자 인증 실패: {}", e.getMessage());
            return ResponseEntity.status(401)
                .body(new ApiResponse<>(false, e.getMessage(), null, "UNAUTHORIZED"));

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
            String adminId = getAdminIdFromToken(request);

            adminService.approveReservation(id, adminId, dto.getNote());

            return ResponseEntity.ok(
                new ApiResponse<>(true, "예약이 승인되었습니다.", null)
            );

        } catch (UnauthorizedException e) {
            log.warn("관리자 인증 실패: {}", e.getMessage());
            return ResponseEntity.status(401)
                .body(new ApiResponse<>(false, e.getMessage(), null, "UNAUTHORIZED"));

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
            String adminId = getAdminIdFromToken(request);

            adminService.rejectReservation(id, adminId, dto.getReason());

            return ResponseEntity.ok(
                new ApiResponse<>(true, "예약이 반려되었습니다.", null)
            );

        } catch (UnauthorizedException e) {
            log.warn("관리자 인증 실패: {}", e.getMessage());
            return ResponseEntity.status(401)
                .body(new ApiResponse<>(false, e.getMessage(), null, "UNAUTHORIZED"));

        } catch (Exception e) {
            log.error("예약 반려 실패: reservationId={}", id, e);
            return ResponseEntity.status(500)
                .body(new ApiResponse<>(false, "반려 중 오류가 발생했습니다.", null, "INTERNAL_ERROR"));
        }
    }
}
```

**방법 B: username에서 직접 추출 (간단)**
```java
private String getAdminIdFromToken(HttpServletRequest request) {
    String token = extractToken(request);

    if (token == null || !jwtUtil.validateToken(token)) {
        throw new UnauthorizedException("관리자 인증이 필요합니다.");
    }

    // JWT subject에 adminId가 저장되어 있음 (이메일 형식)
    String adminId = jwtUtil.extractUsername(token);

    // 선택: 관리자 존재 여부 확인
    if (!adminRepository.existsByAdminId(adminId)) {
        throw new UnauthorizedException("존재하지 않는 관리자입니다.");
    }

    return adminId;
}
```

---

### 2. UnauthorizedException 추가

**위치**: `BlueCrab/com/example/exception/UnauthorizedException.java`

```java
package BlueCrab.com.example.exception;

public class UnauthorizedException extends RuntimeException {
    public UnauthorizedException(String message) {
        super(message);
    }
}
```

**GlobalExceptionHandler 추가**
```java
@ExceptionHandler(UnauthorizedException.class)
public ResponseEntity<ApiResponse<Void>> handleUnauthorizedException(UnauthorizedException e) {
    return ResponseEntity.status(401)
        .body(new ApiResponse<>(false, e.getMessage(), null, "UNAUTHORIZED"));
}
```

---

### 3. DTO 추가 필요

**AdminApproveRequestDto**
```java
package BlueCrab.com.example.dto.facility.admin;

import lombok.Data;

@Data
public class AdminApproveRequestDto {
    private String note;  // 승인 메모 (선택사항)
}
```

**AdminRejectRequestDto**
```java
package BlueCrab.com.example.dto.facility.admin;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

@Data
public class AdminRejectRequestDto {
    @NotBlank(message = "반려 사유는 필수입니다.")
    @Size(max = 500, message = "반려 사유는 500자 이내로 입력해주세요.")
    private String reason;
}
```

---

## 🎯 최종 검증 결과

### ✅ 즉시 구현 가능

| 항목 | 상태 | 비고 |
|------|------|------|
| UserTbl 필드 | ✅ 확인 완료 | userName, userCode, userEmail 존재 |
| AdminTbl 필드 | ✅ 확인 완료 | adminIdx, adminId, name 존재 |
| JWT 토큰 구조 | ✅ 확인 완료 | 사용자/관리자 모두 동일 구조 |
| AdminTblRepository | ✅ 확인 완료 | 이미 존재, 추가 생성 불필요 |
| JPQL JOIN | ✅ 검증 완료 | userCode로 조인 가능 |
| 관리자 인증 로직 | ✅ 구현 가능 | 2가지 방법 제시 |

### ⚠️ 추가 구현 필요 (소요 시간: 30분)

- [ ] UnauthorizedException 클래스 생성
- [ ] GlobalExceptionHandler에 예외 처리 추가
- [ ] AdminApproveRequestDto 생성
- [ ] AdminRejectRequestDto 생성
- [ ] AdminFacilityReservationController 관리자 인증 로직 구현

---

## 📝 구현 순서 (업데이트)

### Phase 0: 사전 준비 (30분)
1. UnauthorizedException 클래스 생성
2. GlobalExceptionHandler 업데이트
3. AdminApproveRequestDto, AdminRejectRequestDto 생성

### Phase 1: 기반 구조 (1-2일)
- 사전 확인 완료로 **즉시 시작 가능**

### Phase 2: 학생 기능 (2-3일)
- 사전 확인 완료로 **즉시 시작 가능**

### Phase 3: 관리자 기능 (2일)
- Phase 0 완료 후 **즉시 시작 가능**
- 관리자 인증 로직 구현 (위 가이드 참조)

### Phase 4-5: 부가 기능 및 테스트 (3일)
- 사전 확인 완료로 **즉시 시작 가능**

---

## ✅ 결론

### 모든 사전 확인 완료 ✅

**기존 시스템 확인 결과**:
- UserTbl: 필요한 모든 필드 존재
- AdminTbl: 필요한 모든 필드 존재
- JWT: 사용자/관리자 동일 구조
- AdminTblRepository: 이미 존재

**추가 작업**: Phase 0 (30분) 후 **즉시 구현 가능**

**예상 기간**: 7-10일 (변경 없음)

---

**최종 업데이트**: 2025-10-06
**검증 상태**: ✅ 모든 사전 확인 완료
**구현 준비**: ✅ Ready to Start
