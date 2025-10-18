# 📊 Phase 1: 백엔드 구조 분석 완료 보고서

## ✅ 분석 완료 시간
**2025년 10월 13일**

---

## 🎯 분석 목표
학적 조회(`/api/registry/me`) 및 증명서 발급 이력(`/api/registry/cert/issue`) API 구현을 위한 기존 백엔드 구조 파악

---

## 📦 1. 데이터베이스 테이블 구조

### 1.1 REGIST_TABLE (학적 정보 테이블)

| 컬럼명 | 타입 | NULL | 설명 | 비고 |
|--------|------|------|------|------|
| `REG_IDX` | int(11) | NO | 학적 이력 행 ID (PK) | AUTO_INCREMENT |
| `USER_IDX` | int(11) | NO | 학생 ID (FK → USER_TBL) | 조인 키 |
| `USER_CODE` | varchar(50) | NO | 학번/교번 | 조회 보조 |
| `JOIN_PATH` | varchar(100) | NO | 입학경로 | 기본값: '신규' |
| `STD_STAT` | varchar(100) | NO | 학적상태 | 기본값: '재학' |
| `STD_REST_DATE` | varchar(200) | YES | 휴학기간 (문자열) | NULL 허용 |
| `CNT_TERM` | int(11) | NO | 이수완료 학기 수 | 기본값: 0 |
| `ADMIN_NAME` | varchar(200) | YES | 처리 관리자명 | NULL 허용 |
| `ADMIN_REG` | datetime | YES | 처리일시 | NULL 허용 |
| `ADMIN_IP` | varchar(45) | YES | 처리발생 IP | NULL 허용 |

**샘플 데이터:**
```
REG_IDX=1, USER_IDX=1, USER_CODE=0, JOIN_PATH='신규', STD_STAT='재학', CNT_TERM=0
```

**인덱스 권장:**
```sql
INDEX idx_user_admin (USER_IDX, ADMIN_REG DESC)
```

---

### 1.2 USER_TBL (사용자 기본 정보 테이블)

| 컬럼명 | 타입 | NULL | 설명 |
|--------|------|------|------|
| `USER_IDX` | int(200) | NO | 사용자 ID (PK) |
| `USER_EMAIL` | varchar(200) | NO | 이메일 (로그인 ID) |
| `USER_PW` | varchar(200) | NO | 비밀번호 |
| `USER_NAME` | varchar(50) | NO | 실명 |
| `USER_CODE` | varchar(255) | NO | 학번/교번 |
| `USER_PHONE` | char(11) | NO | 전화번호 |
| `USER_BIRTH` | varchar(100) | NO | 생년월일 |
| `USER_STUDENT` | int(1) | NO | 사용자 유형 (0: 학생, 1: 교수) |
| `USER_ZIP` | int(5) | YES | 우편번호 |
| `USER_FIRST_ADD` | varchar(200) | YES | 기본주소 |
| `USER_LAST_ADD` | varchar(100) | YES | 상세주소 |
| `PROFILE_IMAGE_KEY` | varchar(255) | YES | 프로필 이미지 키 |

---

### 1.3 PROFILE_VIEW (뷰 테이블)

**뷰 생성 로직:**
```sql
SELECT 
  u.USER_EMAIL, u.USER_NAME, u.USER_PHONE,
  u.USER_STUDENT AS user_type,
  u.USER_CODE AS major_code,
  LPAD(COALESCE(u.USER_ZIP, 0), 5, '0') AS zip_code,
  u.USER_FIRST_ADD AS main_address,
  u.USER_LAST_ADD AS detail_address,
  u.PROFILE_IMAGE_KEY,
  u.USER_BIRTH AS birth_date,
  r.STD_STAT AS academic_status,
  r.JOIN_PATH AS admission_route,
  mf.faculty_code AS major_faculty_code,
  md.dept_code AS major_dept_code,
  minf.faculty_code AS minor_faculty_code,
  mind.dept_code AS minor_dept_code
FROM USER_TBL u
LEFT JOIN REGIST_TABLE r ON u.USER_IDX = r.USER_IDX
LEFT JOIN SERIAL_CODE_TABLE sc ON u.USER_IDX = sc.USER_IDX
LEFT JOIN FACULTY mf ON sc.SERIAL_CODE = mf.faculty_code
LEFT JOIN DEPARTMENT md ON mf.faculty_id = md.faculty_id AND sc.SERIAL_SUB = md.dept_code
LEFT JOIN FACULTY minf ON sc.SERIAL_CODE_ND = minf.faculty_code
LEFT JOIN DEPARTMENT mind ON minf.faculty_id = mind.faculty_id AND sc.SERIAL_SUB_ND = mind.dept_code
```

**특징:**
- `REGIST_TABLE`과 `USER_TBL`을 LEFT JOIN으로 조인
- **문제점**: 여러 학적 이력이 있을 경우 중복 행 발생 가능 → Registry API에서는 **최신 학적만 조회** 필요

---

## 🏗️ 2. 백엔드 아키텍처 패턴

### 2.1 레이어 구조
```
Controller (REST API 엔드포인트)
    ↓
Service (비즈니스 로직)
    ↓
Repository (데이터 액세스)
    ↓
Entity (JPA 엔티티)
```

### 2.2 기존 Profile API 구현 패턴

#### **ProfileController.java**
```java
@RestController
@RequestMapping("/api/profile")
public class ProfileController {
    @Autowired
    private ProfileService profileService;
    
    @Autowired
    private JwtUtil jwtUtil;
    
    @PostMapping("/me")
    public ResponseEntity<ApiResponse<ProfileView>> getMyProfile(HttpServletRequest request) {
        String token = extractToken(request);
        String userEmail = jwtUtil.getUserEmailFromToken(token);
        
        ProfileView profile = profileService.getMyProfile(userEmail);
        
        return ResponseEntity.ok(
            ApiResponse.success("프로필 정보를 성공적으로 조회했습니다.", profile)
        );
    }
}
```

#### **ProfileService.java**
```java
@Service
public class ProfileService {
    @Autowired
    private ProfileViewRepository profileViewRepository;
    
    public ProfileView getMyProfile(String userEmail) {
        return profileViewRepository.findByUserEmail(userEmail)
            .orElseThrow(() -> new RuntimeException("프로필 정보를 찾을 수 없습니다."));
    }
}
```

#### **ProfileViewRepository.java**
```java
@Repository
public interface ProfileViewRepository extends JpaRepository<ProfileView, String> {
    Optional<ProfileView> findByUserEmail(String userEmail);
    boolean existsByUserEmail(String userEmail);
}
```

#### **ProfileView.java (Entity)**
```java
@Entity
@Table(name = "PROFILE_VIEW")
@org.hibernate.annotations.Immutable  // 읽기 전용
public class ProfileView {
    @Id
    @Column(name = "user_email")
    private String userEmail;
    
    @Column(name = "user_name")
    private String userName;
    
    @Column(name = "academic_status")
    private String academicStatus;
    
    // ... 기타 필드
}
```

---

## 📝 3. 공통 응답 포맷 (ApiResponse)

### 3.1 ApiResponse 구조
```java
public class ApiResponse<T> {
    private boolean success;      // 성공 여부
    private String message;       // 응답 메시지 (한국어)
    private T data;              // 실제 데이터
    private String timestamp;    // 응답 시간 (ISO-8601)
    private String errorCode;    // 에러 코드 (선택)
}
```

### 3.2 사용 예시
```java
// 성공 응답
ApiResponse.success("조회 성공", data);

// 실패 응답
ApiResponse.failure("조회 실패");

// 에러 코드 포함
ApiResponse.failure("인증 실패", null, "UNAUTHORIZED");
```

---

## ⚠️ 4. 예외 처리 구조

### 4.1 커스텀 예외 클래스
```java
// 리소스 없음 (404)
public class ResourceNotFoundException extends RuntimeException {
    public static ResourceNotFoundException forId(String resourceName, Object id);
    public static ResourceNotFoundException forField(String resourceName, String fieldName, Object fieldValue);
}

// 중복 리소스 (409)
public class DuplicateResourceException extends RuntimeException

// 인증 실패 (401)
public class UnauthorizedException extends RuntimeException
```

### 4.2 GlobalExceptionHandler
```java
@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleResourceNotFound(ResourceNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body(ApiResponse.failure(ex.getMessage(), null, "RESOURCE_NOT_FOUND"));
    }
    
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ApiResponse<Void>> handleRuntimeException(RuntimeException ex) {
        return ResponseEntity.badRequest()
            .body(ApiResponse.failure(ex.getMessage()));
    }
}
```

---

## 🔐 5. JWT 인증 패턴

### 5.1 토큰 추출 로직
```java
private String extractToken(HttpServletRequest request) {
    String bearerToken = request.getHeader("Authorization");
    if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
        return bearerToken.substring(7);
    }
    throw new UnauthorizedException("토큰이 존재하지 않습니다.");
}
```

### 5.2 사용자 정보 추출
```java
String userEmail = jwtUtil.getUserEmailFromToken(token);
```

---

## 📊 6. Registry API 구현 시 주요 차이점

| 구분 | Profile API | Registry API |
|------|-------------|--------------|
| **데이터 소스** | `PROFILE_VIEW` (뷰) | `REGIST_TABLE` (테이블 직접 조회) |
| **조인 방식** | 뷰에서 자동 조인 | Service에서 명시적 조인 |
| **학적 이력** | LEFT JOIN (최신 보장 X) | **ORDER BY + LIMIT 1** (최신만) |
| **응답 필드** | 프로필 + 학적 통합 | 학적 중심 + 사용자 기본 정보 |
| **추가 기능** | 없음 | `asOf` 파라미터 (특정 시점 스냅샷) |

---

## 🎯 7. 구현 시 고려사항

### 7.1 최신 학적 조회 로직
**문제점**: `PROFILE_VIEW`는 여러 학적 이력이 있을 경우 모든 행을 반환
**해결책**: `REGIST_TABLE`에서 직접 조회 시 정렬 + LIMIT 적용

```sql
SELECT *
FROM REGIST_TABLE rt
WHERE rt.USER_IDX = :userId
ORDER BY 
  rt.ADMIN_REG DESC NULLS LAST,  -- 관리자 처리일시 우선
  rt.REG_IDX DESC                -- 생성순 보조
LIMIT 1;
```

### 7.2 USER_TBL 조인
```java
// JPA Repository에서 fetch join 활용
@Query("SELECT r FROM RegistryTbl r JOIN FETCH r.user WHERE r.user.userEmail = :userEmail ORDER BY r.adminReg DESC NULLS LAST, r.regIdx DESC")
Optional<RegistryTbl> findLatestByUserEmail(@Param("userEmail") String userEmail);
```

### 7.3 DTO 변환 로직
```java
public RegistryResponseDTO toDTO(RegistryTbl registry, UserTbl user) {
    return RegistryResponseDTO.builder()
        .userName(user.getUserName())
        .userEmail(user.getUserEmail())
        .studentCode(registry.getUserCode())
        .academicStatus(registry.getStdStat())
        .admissionRoute(registry.getJoinPath())
        .enrolledTerms(registry.getCntTerm())
        .restPeriod(registry.getStdRestDate())
        // ... 주소 등 추가 필드
        .issuedAt(Instant.now().toString())
        .build();
}
```

---

## ✅ 8. Phase 1 완료 체크리스트

- [x] `REGIST_TABLE` 구조 파악 (10개 컬럼)
- [x] `USER_TBL` 구조 파악 (14개 주요 컬럼)
- [x] `PROFILE_VIEW` 조인 로직 분석
- [x] Profile API 구현 패턴 파악 (Controller → Service → Repository)
- [x] `ApiResponse<T>` 공통 응답 포맷 확인
- [x] JWT 인증 추출 로직 확인
- [x] 예외 처리 구조 파악 (GlobalExceptionHandler)
- [x] Repository 쿼리 메서드 패턴 확인
- [x] Entity 매핑 어노테이션 확인 (`@Entity`, `@Table`, `@Column`)

---

## 🚀 Next Steps (Phase 2)

### ✅ TODO List
1. **RegistryTbl Entity 생성** (`REGIST_TABLE` 매핑)
2. **CertIssueTbl Entity 생성** (`CERT_ISSUE_TBL` 매핑)
3. **RegistryRepository 구현** (최신 학적 조회 쿼리)
4. **CertIssueRepository 구현** (발급 이력 저장/조회)
5. **DTO 클래스 설계** (Request/Response)
6. **Service 로직 구현** (비즈니스 로직)
7. **Controller 엔드포인트 구현** (REST API)
8. **예외 클래스 추가** (`RegistryNotFoundException`)
9. **DDL 스크립트 작성** (`CERT_ISSUE_TBL`)
10. **API 테스트 파일 작성**

---

## 📌 핵심 인사이트

### 🎯 Registry API의 핵심 차별점
1. **실시간 학적 스냅샷**: `PROFILE_VIEW`가 아닌 `REGIST_TABLE` 직접 조회
2. **시점 기준 조회**: `asOf` 파라미터로 특정 날짜 기준 학적 상태 조회 가능
3. **발급 이력 저장**: 증명서 발급 시 JSON 스냅샷 저장 (감사 로그)
4. **프론트 폴백 구조**: 404 시 자동으로 `/api/profile/me`로 폴백 → 백엔드 구현 시 FE 수정 불필요

### ⚡ 성능 최적화 포인트
- `(USER_IDX, ADMIN_REG DESC)` 복합 인덱스 생성
- JPA Fetch Join으로 N+1 문제 방지
- DTO Projection으로 불필요한 컬럼 조회 제거

---

**분석 완료 ✅**  
다음 Phase 2에서 Entity 및 Repository 구현을 시작합니다.
