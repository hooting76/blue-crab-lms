# 📊 Phase 3: Service & Controller 구현 완료 보고서

## ✅ 완료 시간
**2025년 10월 13일**

---

## 🎯 Phase 3 목표
학적 조회 및 증명서 발급 API의 비즈니스 로직 및 REST 엔드포인트 구현

---

## 📦 생성된 파일 목록

### 1. Service 클래스 (2개)

#### ✅ RegistryService.java
**경로**: `backend/BlueCrab/src/main/java/BlueCrab/com/example/service/RegistryService.java`

**주요 메서드**:
```java
// 최신 학적 조회
RegistryResponseDTO getMyRegistry(String userEmail)

// 시점 기준 학적 조회 (As-Of Query)
RegistryResponseDTO getMyRegistry(String userEmail, RegistryRequestDTO requestDTO)

// 학적 존재 여부 확인
boolean hasRegistry(String userEmail)

// 학번으로 학적 조회 (관리자용)
RegistryResponseDTO getRegistryByUserCode(String userCode)

// 학적 상태별 통계
long countByAcademicStatus(String stdStat)

// 내부 메서드
- RegistryResponseDTO convertToDTO(RegistryTbl registry)
- String calculateExpectedGraduateDate(RegistryTbl registry)
```

**주요 특징**:
- `@Transactional(readOnly = true)` - 읽기 전용 트랜잭션
- **As-Of Query** 지원 (특정 시점 기준 조회)
- Entity → DTO 변환 로직
- 졸업 예정일 계산 (간단한 로직, 추후 개선 필요)
- 유효성 검증 및 예외 처리

**DTO 변환 예시**:
```java
return RegistryResponseDTO.builder()
        .userName(user.getUserName())
        .userEmail(user.getUserEmail())
        .studentCode(registry.getUserCode())
        .academicStatus(registry.getStdStat())
        .admissionRoute(registry.getJoinPath())
        .enrolledTerms(registry.getCntTerm())
        .restPeriod(registry.getStdRestDate())
        .address(addressDTO)
        .issuedAt(Instant.now().toString())
        .build();
```

---

#### ✅ CertIssueService.java
**경로**: `backend/BlueCrab/src/main/java/BlueCrab/com/example/service/CertIssueService.java`

**주요 메서드**:
```java
// 증명서 발급 이력 저장
CertIssueResponseDTO issueCertificate(String userEmail, CertIssueRequestDTO requestDTO, String clientIp)

// 발급 이력 조회
List<CertIssueTbl> getIssueHistory(String userEmail)
List<CertIssueTbl> getIssueHistoryByType(String userEmail, String certType)

// 통계
long countByCertType(String certType)
long countByPeriod(LocalDateTime startTime, LocalDateTime endTime)
long countByUserEmail(String userEmail)

// 내부 메서드
- void validateRequest(String userEmail, CertIssueRequestDTO requestDTO)
- void checkIssueInterval(String userEmail, String certType)
- String generateOrUseSnapshot(String userEmail, CertIssueRequestDTO requestDTO)
```

**주요 특징**:
- `@Transactional` - 쓰기 트랜잭션
- **남발 방지**: 최근 5분 이내 동일 증명서 재발급 제한
- **스냅샷 자동 생성**: RegistryService 연동
- **JSON 변환**: ObjectMapper 사용
- IP 주소 기록

**남발 방지 로직**:
```java
private static final int ISSUE_INTERVAL_MINUTES = 5;

private void checkIssueInterval(String userEmail, String certType) {
    LocalDateTime cutoffTime = LocalDateTime.now().minusMinutes(ISSUE_INTERVAL_MINUTES);
    
    List<CertIssueTbl> recentIssues = certIssueRepository.findByUserEmailAndCertTypeAndIssuedAtAfter(
        userEmail, certType, cutoffTime
    );
    
    if (!recentIssues.isEmpty()) {
        throw new RuntimeException("동일한 증명서를 5분 이내에 재발급할 수 없습니다.");
    }
}
```

---

### 2. Controller 클래스 (1개)

#### ✅ RegistryController.java
**경로**: `backend/BlueCrab/src/main/java/BlueCrab/com/example/controller/RegistryController.java`

**제공 엔드포인트**:
```java
// 1. 학적 조회
POST /api/registry/me
- Authorization: Bearer {token}
- Request Body: { "asOf": "2025-03-01" } (선택)
- Response: ApiResponse<RegistryResponseDTO>

// 2. 증명서 발급 이력 저장
POST /api/registry/cert/issue
- Authorization: Bearer {token}
- Request Body: { "type": "enrollment", "asOf": "...", "format": "html" }
- Response: ApiResponse<CertIssueResponseDTO>

// 3. 학적 존재 여부 확인 (헬스체크)
GET /api/registry/me/exists
- Authorization: Bearer {token}
- Response: ApiResponse<Boolean>
```

**주요 특징**:
- JWT 토큰 인증 필수
- Bearer 토큰 형식 검증
- IP 주소 추출 (프록시 환경 고려)
- 통일된 `ApiResponse<T>` 응답 구조
- 상세한 에러 로깅

**JWT 토큰 추출**:
```java
private String extractToken(HttpServletRequest request) {
    String bearerToken = request.getHeader("Authorization");
    
    if (bearerToken == null || !bearerToken.startsWith("Bearer ")) {
        throw new RuntimeException("인증 토큰이 제공되지 않았습니다.");
    }
    
    return bearerToken.substring(7);
}
```

**IP 주소 추출** (프록시 대응):
```java
private String extractClientIp(HttpServletRequest request) {
    String ip = request.getHeader("X-Forwarded-For");
    
    if (ip == null || "unknown".equalsIgnoreCase(ip)) {
        ip = request.getHeader("Proxy-Client-IP");
    }
    // ... 여러 헤더 체크
    if (ip == null) {
        ip = request.getRemoteAddr();
    }
    
    // 첫 번째 IP 사용 (X-Forwarded-For가 여러 IP 포함 시)
    if (ip != null && ip.contains(",")) {
        ip = ip.split(",")[0].trim();
    }
    
    return ip;
}
```

---

## 🎯 핵심 구현 포인트

### 1. As-Of Query (시점 기준 조회)

**사용 시나리오**:
- "2025년 3월 1일 기준 재학증명서" 발급
- 과거 특정 날짜의 학적 상태 조회

**구현**:
```java
String asOfStr = requestDTO.getAsOf();  // "2025-03-01"
LocalDate asOfDate = LocalDate.parse(asOfStr, DateTimeFormatter.ISO_LOCAL_DATE);
LocalDateTime asOfDateTime = asOfDate.atTime(23, 59, 59);  // 하루의 끝

Optional<RegistryTbl> registryOpt = registryRepository.findLatestByUserEmailAsOf(userEmail, asOfDateTime);
```

---

### 2. 증명서 발급 남발 방지

**정책**:
- 동일 증명서를 **5분 이내**에 재발급 불가
- 증명서 유형별로 독립적으로 적용

**구현**:
```java
LocalDateTime cutoffTime = LocalDateTime.now().minusMinutes(5);

List<CertIssueTbl> recentIssues = certIssueRepository.findByUserEmailAndCertTypeAndIssuedAtAfter(
    userEmail, certType, cutoffTime
);

if (!recentIssues.isEmpty()) {
    long minutesAgo = Duration.between(lastIssue.getIssuedAt(), LocalDateTime.now()).toMinutes();
    throw new RuntimeException(
        String.format("동일한 증명서를 5분 이내에 재발급할 수 없습니다. (최근 발급: %d분 전)", minutesAgo)
    );
}
```

---

### 3. 스냅샷 자동 생성

**로직**:
1. **프론트 제공 스냅샷이 있으면** → 그대로 사용
2. **없으면** → 서버에서 자동 생성 (`RegistryService` 호출)

**구현**:
```java
private String generateOrUseSnapshot(String userEmail, CertIssueRequestDTO requestDTO) {
    if (requestDTO.getSnapshot() != null && !requestDTO.getSnapshot().isEmpty()) {
        // 프론트 제공 스냅샷 사용
        return objectMapper.writeValueAsString(requestDTO.getSnapshot());
    } else {
        // 서버 자동 생성
        RegistryRequestDTO registryRequest = new RegistryRequestDTO(requestDTO.getAsOf());
        RegistryResponseDTO registryData = registryService.getMyRegistry(userEmail, registryRequest);
        return objectMapper.writeValueAsString(registryData);
    }
}
```

**장점**:
- 프론트/백엔드 데이터 불일치 방지
- 서버 자동 생성 권장 (일관성 보장)

---

### 4. JWT 인증 통합

**기존 코드 활용**:
```java
String token = extractToken(request);           // Bearer 토큰 추출
String userEmail = jwtUtil.extractUsername(token);  // 이메일 추출
```

**기존 `JwtUtil` 메서드**:
- `extractUsername(String token)` → 사용자 이메일 반환
- `extractUserId(String token)` → 사용자 ID 반환
- `validateToken(String token, String username)` → 토큰 유효성 검증

---

### 5. 통일된 응답 구조

**모든 API 응답**:
```json
{
  "success": true/false,
  "message": "한국어 메시지",
  "data": { ... },
  "errorCode": null,
  "timestamp": "2025-03-02T10:00:01Z"
}
```

**Controller 반환**:
```java
return ResponseEntity.ok(
    ApiResponse.success("학적 정보를 성공적으로 조회했습니다.", registry)
);
```

---

## 🔒 보안 구현

### 1. JWT 인증
- 모든 엔드포인트에서 `Authorization: Bearer {token}` 필수
- 토큰 형식 검증 (Bearer 접두사)
- 본인의 정보만 조회/발급 가능

### 2. IP 주소 기록
- 증명서 발급 시 클라이언트 IP 저장
- 프록시/로드밸런서 환경 대응 (`X-Forwarded-For` 헤더)

### 3. 남발 방지
- 5분 이내 동일 증명서 재발급 제한
- 발급 이력 감사 로그

---

## ✅ Phase 3 완료 체크리스트

- [x] **RegistryService** 구현 (6개 public 메서드)
- [x] **CertIssueService** 구현 (8개 public 메서드)
- [x] **RegistryController** 구현 (3개 엔드포인트)
- [x] JWT 토큰 인증 통합
- [x] As-Of Query 지원
- [x] 증명서 발급 남발 방지 로직
- [x] 스냅샷 자동 생성 로직
- [x] IP 주소 추출 (프록시 대응)
- [x] DTO 변환 로직
- [x] 유효성 검증
- [x] 예외 처리
- [x] 로깅

---

## 🚀 Next Steps (Phase 4)

### ✅ TODO List
8. **예외 클래스 추가** (선택) → 기존 `ResourceNotFoundException` 활용 중
9. **DDL 스크립트 작성** (`CERT_ISSUE_TBL`)
10. **API 테스트 파일 작성**

---

## 📊 코드 통계

| 구분 | 파일 수 | 총 라인 수 (추정) |
|------|---------|-------------------|
| Service | 2 | ~600 |
| Controller | 1 | ~280 |
| **합계** | **3** | **~880** |

**누적 (Phase 1~3)**:
- Entity: 2개 (~600줄)
- Repository: 2개 (~500줄)
- DTO: 4개 (~500줄)
- Service: 2개 (~600줄)
- Controller: 1개 (~280줄)
- **총계: 11개 파일, ~2,480줄**

---

## 🎯 API 요청/응답 예시

### 1. 학적 조회 (현재 시점)

**Request**:
```http
POST /api/registry/me
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
Content-Type: application/json

{}
```

**Response**:
```json
{
  "success": true,
  "message": "학적 정보를 성공적으로 조회했습니다.",
  "data": {
    "userName": "서혜진",
    "userEmail": "student001@univ.edu",
    "studentCode": "202500101000",
    "academicStatus": "재학",
    "admissionRoute": "정시",
    "enrolledTerms": 2,
    "restPeriod": null,
    "address": {
      "zipCode": "12345",
      "mainAddress": "서울특별시 강남구 테헤란로 124",
      "detailAddress": "VALUE 아파트 101동 501호"
    },
    "issuedAt": "2025-10-13T10:00:00Z"
  },
  "errorCode": null,
  "timestamp": "2025-10-13T10:00:01Z"
}
```

---

### 2. 학적 조회 (시점 기준)

**Request**:
```http
POST /api/registry/me
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
Content-Type: application/json

{
  "asOf": "2025-03-01"
}
```

---

### 3. 증명서 발급 이력 저장

**Request**:
```http
POST /api/registry/cert/issue
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
Content-Type: application/json

{
  "type": "enrollment",
  "asOf": "2025-03-01",
  "format": "html"
}
```

**Response**:
```json
{
  "success": true,
  "message": "증명서 발급 이력이 저장되었습니다.",
  "data": {
    "issueId": "C20251013-000001",
    "issuedAt": "2025-10-13T10:00:02Z"
  },
  "errorCode": null,
  "timestamp": "2025-10-13T10:00:02Z"
}
```

---

**Phase 3 완료 ✅**  
다음 Phase 4에서 DDL 스크립트 및 테스트 파일을 작성합니다.
