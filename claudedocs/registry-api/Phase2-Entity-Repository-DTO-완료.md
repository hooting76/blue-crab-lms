# 📊 Phase 2: Entity, Repository, DTO 구현 완료 보고서

## ✅ 완료 시간
**2025년 10월 13일**

---

## 🎯 Phase 2 목표
학적 및 증명서 발급 이력 관리를 위한 데이터 계층 구현

---

## 📦 생성된 파일 목록

### 1. Entity 클래스 (2개)

#### ✅ RegistryTbl.java
**경로**: `backend/BlueCrab/src/main/java/BlueCrab/com/example/entity/RegistryTbl.java`

**주요 특징**:
- `REGIST_TABLE` 테이블 매핑
- `UserTbl`과 Many-to-One 관계 (LAZY 로딩)
- 10개 필드 매핑 (학적상태, 입학경로, 이수학기 등)
- `LocalDateTime` 타입 사용 (adminReg)

**핵심 필드**:
```java
- regIdx: Integer (PK, AUTO_INCREMENT)
- user: UserTbl (FK, LAZY)
- userCode: String (학번/교번)
- joinPath: String (입학경로, 기본값: "신규")
- stdStat: String (학적상태, 기본값: "재학")
- stdRestDate: String (휴학기간)
- cntTerm: Integer (이수학기수, 기본값: 0)
- adminName: String (처리 관리자명)
- adminReg: LocalDateTime (처리일시)
- adminIp: String (처리 IP)
```

#### ✅ CertIssueTbl.java
**경로**: `backend/BlueCrab/src/main/java/BlueCrab/com/example/entity/CertIssueTbl.java`

**주요 특징**:
- `CERT_ISSUE_TBL` 테이블 매핑
- `UserTbl`과 Many-to-One 관계 (LAZY 로딩)
- JSON 컬럼 지원 (`@Lob`, `columnDefinition = "JSON"`)
- 발급 ID 생성 메서드 (`generateIssueId()`)

**핵심 필드**:
```java
- certIdx: Integer (PK, AUTO_INCREMENT)
- user: UserTbl (FK, LAZY)
- certType: String (증명서 유형)
- asOfDate: LocalDate (스냅샷 기준일)
- format: String (발급 형식, 기본값: "html")
- snapshotJson: String (JSON 스냅샷)
- issuedAt: LocalDateTime (발급 일시)
- issuedIp: String (발급 IP)
```

---

### 2. Repository 인터페이스 (2개)

#### ✅ RegistryRepository.java
**경로**: `backend/BlueCrab/src/main/java/BlueCrab/com/example/repository/RegistryRepository.java`

**제공 메서드**:
```java
// 최신 학적 조회
Optional<RegistryTbl> findLatestByUserEmail(String userEmail)
Optional<RegistryTbl> findLatestByUserIdx(Integer userIdx)
Optional<RegistryTbl> findLatestByUserCode(String userCode)

// 특정 시점 기준 조회 (As-Of Query)
Optional<RegistryTbl> findLatestByUserEmailAsOf(String userEmail, LocalDateTime asOfDate)

// 학적 이력 조회
List<RegistryTbl> findAllHistoryByUserEmail(String userEmail)

// 이수 학기 수 기준 조회
List<RegistryTbl> findByCntTermGreaterThanEqual(Integer minTerms)

// 존재 여부 확인
boolean existsByUserEmail(String userEmail)

// 통계
long countByStdStat(String stdStat)
```

**주요 특징**:
- **Fetch Join** 사용 (N+1 문제 방지)
- **정렬**: `ORDER BY adminReg DESC NULLS LAST, regIdx DESC`
- **Default 메서드**로 단건 조회 편의성 제공
- **As-Of Query** 지원 (특정 시점 스냅샷)

#### ✅ CertIssueRepository.java
**경로**: `backend/BlueCrab/src/main/java/BlueCrab/com/example/repository/CertIssueRepository.java`

**제공 메서드**:
```java
// 발급 이력 조회
List<CertIssueTbl> findAllByUserEmailOrderByIssuedAtDesc(String userEmail)
List<CertIssueTbl> findByUserEmailAndCertType(String userEmail, String certType)

// 최근 발급 이력 조회 (남발 방지)
Optional<CertIssueTbl> findLatestByUserEmail(String userEmail)
Optional<CertIssueTbl> findLatestByUserEmailAndCertType(String userEmail, String certType)

// 시간 기준 조회
List<CertIssueTbl> findByUserEmailAndIssuedAtAfter(String userEmail, LocalDateTime afterTime)
List<CertIssueTbl> findByUserEmailAndCertTypeAndIssuedAtAfter(...)

// 통계
long countByCertType(String certType)
long countByIssuedAtBetween(LocalDateTime start, LocalDateTime end)
long countByUserEmail(String userEmail)
long countByUserEmailAndCertType(String userEmail, String certType)

// 보안 감사
List<CertIssueTbl> findByIssuedIpOrderByIssuedAtDesc(String issuedIp)

// 관리자용
List<CertIssueTbl> findAllOrderByIssuedAtDesc()
List<CertIssueTbl> findByIssuedAtBetween(LocalDateTime start, LocalDateTime end)
```

**주요 특징**:
- **남발 방지** 쿼리 지원 (최근 N분 이내 발급 체크)
- **Fetch Join** 사용
- **정렬**: `ORDER BY issuedAt DESC`
- 통계 및 보안 감사 기능 제공

---

### 3. DTO 클래스 (4개)

#### ✅ RegistryRequestDTO.java
**경로**: `backend/BlueCrab/src/main/java/BlueCrab/com/example/dto/RegistryRequestDTO.java`

**필드**:
```java
- asOf: String (선택, 스냅샷 기준일 "YYYY-MM-DD")
```

**용도**: `POST /api/registry/me` 요청 바디

---

#### ✅ RegistryResponseDTO.java
**경로**: `backend/BlueCrab/src/main/java/BlueCrab/com/example/dto/RegistryResponseDTO.java`

**필드**:
```java
- userName: String
- userEmail: String
- studentCode: String
- academicStatus: String
- admissionRoute: String
- enrolledTerms: Integer
- restPeriod: String
- facultyName: String
- departmentName: String
- expectedGraduateAt: String
- address: AddressDTO (중첩 객체)
  - zipCode: String
  - mainAddress: String
  - detailAddress: String
- issuedAt: String (ISO-8601)
```

**주요 특징**:
- **Builder 패턴** 지원
- 중첩 DTO (`AddressDTO`)
- Profile API와 동일한 구조

**용도**: `POST /api/registry/me` 응답 데이터

---

#### ✅ CertIssueRequestDTO.java
**경로**: `backend/BlueCrab/src/main/java/BlueCrab/com/example/dto/CertIssueRequestDTO.java`

**필드**:
```java
- type: String (필수, 증명서 유형)
- asOf: String (선택, 스냅샷 기준일)
- format: String (선택, 기본값: "html")
- snapshot: Map<String, Object> (선택, 스냅샷 데이터)
```

**유효성 검증 메서드**:
```java
boolean isValidType()    // enrollment, graduation_expected, ...
boolean isValidFormat()  // html, pdf, image
```

**용도**: `POST /api/registry/cert/issue` 요청 바디

---

#### ✅ CertIssueResponseDTO.java
**경로**: `backend/BlueCrab/src/main/java/BlueCrab/com/example/dto/CertIssueResponseDTO.java`

**필드**:
```java
- issueId: String (형식: C20250302-000123)
- issuedAt: String (ISO-8601)
```

**주요 특징**:
- Static Factory Method (`of()`)
- 간결한 응답 구조

**용도**: `POST /api/registry/cert/issue` 응답 데이터

---

## 🎯 핵심 구현 포인트

### 1. 최신 학적 조회 로직
```java
@Query("SELECT r FROM RegistryTbl r " +
       "JOIN FETCH r.user u " +
       "WHERE u.userEmail = :userEmail " +
       "ORDER BY r.adminReg DESC NULLS LAST, r.regIdx DESC")
```

**정렬 우선순위**:
1. `adminReg DESC NULLS LAST` (처리일시 최신순, NULL은 마지막)
2. `regIdx DESC` (생성순 최신순, 보조 기준)

**이유**:
- 여러 학적 이력 중 **가장 최신 학적**만 조회
- `PROFILE_VIEW`의 한계 극복 (중복 행 문제)

---

### 2. Fetch Join 사용
```java
JOIN FETCH r.user u
```

**효과**:
- N+1 문제 방지
- `UserTbl` 즉시 로딩
- 성능 최적화

---

### 3. As-Of Query (시점 기준 조회)
```java
WHERE (r.adminReg IS NULL OR r.adminReg <= :asOfDate)
ORDER BY r.adminReg DESC NULLS LAST, r.regIdx DESC
```

**용도**:
- "2025년 3월 1일 기준 학적 상태" 조회
- 증명서 발급 시 특정 날짜 기준 스냅샷 제공

---

### 4. JSON 스냅샷 저장
```java
@Lob
@Column(name = "SNAPSHOT_JSON", nullable = false, columnDefinition = "JSON")
private String snapshotJson;
```

**중요성**:
- 발급 당시 학적/프로필 정보를 **불변하게** 저장
- 이후 학적 변경되어도 발급 당시 상태 확인 가능
- 감사 추적 및 재발급 검증에 활용

---

### 5. 남발 방지 쿼리
```java
List<CertIssueTbl> findByUserEmailAndIssuedAtAfter(
    String userEmail, 
    LocalDateTime afterTime
)
```

**사용 시나리오**:
```java
// 최근 5분 이내 발급 이력 체크
LocalDateTime fiveMinutesAgo = LocalDateTime.now().minusMinutes(5);
List<CertIssueTbl> recent = certIssueRepository.findByUserEmailAndIssuedAtAfter(
    "student@univ.edu", 
    fiveMinutesAgo
);
if (!recent.isEmpty()) {
    throw new RuntimeException("최근 5분 이내에 이미 발급되었습니다.");
}
```

---

## ✅ Phase 2 완료 체크리스트

- [x] **RegistryTbl** Entity 생성 (REGIST_TABLE 매핑)
- [x] **CertIssueTbl** Entity 생성 (CERT_ISSUE_TBL 매핑)
- [x] **RegistryRepository** 인터페이스 구현 (15개 쿼리 메서드)
- [x] **CertIssueRepository** 인터페이스 구현 (18개 쿼리 메서드)
- [x] **RegistryRequestDTO** 생성 (학적 조회 요청)
- [x] **RegistryResponseDTO** 생성 (학적 조회 응답, Builder 패턴)
- [x] **CertIssueRequestDTO** 생성 (발급 요청, 유효성 검증)
- [x] **CertIssueResponseDTO** 생성 (발급 응답, Factory Method)
- [x] Fetch Join으로 N+1 문제 방지
- [x] As-Of Query 지원 (시점 기준 조회)
- [x] 남발 방지 쿼리 구현
- [x] JSON 스냅샷 컬럼 설계

---

## 🚀 Next Steps (Phase 3)

### ✅ TODO List
5. **RegistryService 구현** → Service 로직 작성 중
6. **CertIssueService 구현**
7. **RegistryController 구현**
8. **예외 클래스 추가** (`RegistryNotFoundException`)
9. **DDL 스크립트 작성** (`CERT_ISSUE_TBL`)
10. **API 테스트 파일 작성**

---

## 📊 코드 통계

| 구분 | 파일 수 | 총 라인 수 (추정) |
|------|---------|-------------------|
| Entity | 2 | ~600 |
| Repository | 2 | ~500 |
| DTO | 4 | ~500 |
| **합계** | **8** | **~1,600** |

---

**Phase 2 완료 ✅**  
다음 Phase 3에서 Service 로직 구현을 시작합니다.
