# Phase 1: Entity, Enum, DTO 생성 완료 보고서

**작업일**: 2025-10-24
**담당자**: BlueCrab Development Team
**상태**: ✅ 완료

---

## 📋 작업 개요

상담 요청/진행 관리 시스템의 데이터 구조(Entity, Enum, DTO, Repository)를 설계 및 구현했습니다.

### 주요 산출물

1. **ConsultationRequest** Entity (단일 테이블 전략)
2. **Enum 클래스 3개** (ConsultationType, RequestStatus, ConsultationStatus)
3. **DTO 클래스 8개** (요청, 승인, 반려, 취소, 메모, 조회용)
4. **Repository** 인터페이스 (JPA + Custom Query)
5. **Database Migration** DDL

---

## 🎯 데이터베이스 설계

### 테이블 구조: CONSULTATION_REQUEST_TBL

**설계 원칙**: 단일 테이블 통합 전략 (요청 + 진행 정보 통합)

```sql
CREATE TABLE CONSULTATION_REQUEST_TBL (
    -- 기본키
    request_idx BIGINT AUTO_INCREMENT PRIMARY KEY,

    -- [요청 정보]
    requester_user_code VARCHAR(20) NOT NULL COMMENT '요청자 USER_CODE (학생)',
    recipient_user_code VARCHAR(20) NOT NULL COMMENT '수신자 USER_CODE (교수)',
    consultation_type VARCHAR(50) NOT NULL COMMENT '상담 유형 (ACADEMIC, CAREER, CAMPUS_LIFE, ETC)',
    title VARCHAR(100) NOT NULL COMMENT '상담 제목',
    content VARCHAR(1000) COMMENT '상담 내용',
    desired_date DATETIME COMMENT '희망 상담 날짜',

    -- [요청 처리]
    request_status VARCHAR(20) NOT NULL DEFAULT 'PENDING' COMMENT '요청 상태 (PENDING, APPROVED, REJECTED, CANCELLED)',
    accept_message VARCHAR(500) COMMENT '승인 메시지',
    rejection_reason VARCHAR(500) COMMENT '반려 사유',
    cancel_reason VARCHAR(500) COMMENT '취소 사유',

    -- [상담 진행]
    consultation_status VARCHAR(20) COMMENT '상담 진행 상태 (SCHEDULED, IN_PROGRESS, COMPLETED, CANCELLED)',
    started_at DATETIME COMMENT '상담 시작 시간',
    ended_at DATETIME COMMENT '상담 종료 시간',
    duration_minutes INT COMMENT '상담 소요 시간 (분)',
    last_activity_at DATETIME COMMENT '마지막 활동 시간',

    -- [읽음 처리]
    last_read_time_student DATETIME COMMENT '학생 마지막 읽음 시간',
    last_read_time_professor DATETIME COMMENT '교수 마지막 읽음 시간',

    -- [메모]
    memo TEXT COMMENT '교수 전용 메모',

    -- [타임스탬프]
    created_at DATETIME NOT NULL COMMENT '생성 시간',
    updated_at DATETIME NOT NULL COMMENT '수정 시간',

    -- [인덱스]
    INDEX idx_requester (requester_user_code),
    INDEX idx_recipient (recipient_user_code),
    INDEX idx_request_status (request_status),
    INDEX idx_consultation_status (consultation_status),
    INDEX idx_created_at (created_at DESC)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='상담 요청 및 진행 정보';
```

---

## 📦 생성된 파일

### 1. Entity

**파일**: [ConsultationRequest.java](../../src/main/java/BlueCrab/com/example/entity/ConsultationRequest.java) (296줄)

#### 주요 특징
- ✅ JPA Entity 매핑
- ✅ @PrePersist/@PreUpdate로 타임스탬프 자동 관리
- ✅ Enum ↔ String 변환 메서드 제공
- ✅ 모든 필드에 대한 Getter/Setter

#### 핵심 필드
```java
@Id
@GeneratedValue(strategy = GenerationType.IDENTITY)
@Column(name = "request_idx")
private Long requestIdx;

// 요청 정보
private String requesterUserCode;      // 요청자 (학생)
private String recipientUserCode;      // 수신자 (교수)
private String consultationType;       // 상담 유형 (Enum → String)
private String title;                  // 제목
private String content;                // 내용
private LocalDateTime desiredDate;     // 희망 날짜

// 요청 처리
private String requestStatus = "PENDING";  // 요청 상태
private String acceptMessage;              // 승인 메시지
private String rejectionReason;            // 반려 사유
private String cancelReason;               // 취소 사유

// 상담 진행
private String consultationStatus;     // 진행 상태
private LocalDateTime startedAt;       // 시작 시간
private LocalDateTime endedAt;         // 종료 시간
private Integer durationMinutes;       // 소요 시간
private LocalDateTime lastActivityAt;  // 마지막 활동

// 읽음 처리
private LocalDateTime lastReadTimeStudent;   // 학생 읽음 시간
private LocalDateTime lastReadTimeProfessor; // 교수 읽음 시간

// 메모
private String memo;  // 교수 전용 메모
```

---

### 2. Enum 클래스

#### ConsultationType.java (55줄)
```java
public enum ConsultationType {
    ACADEMIC("학업상담"),
    CAREER("진로상담"),
    CAMPUS_LIFE("학교생활"),
    ETC("기타");

    // JSON 직렬화/역직렬화
    @JsonValue
    public String getDescription() { ... }

    @JsonCreator
    public static ConsultationType fromString(String value) { ... }

    // DB 값 변환
    public static ConsultationType fromDbValue(String dbValue) { ... }
    public String toDbValue() { ... }
}
```

#### RequestStatus.java (55줄)
```java
public enum RequestStatus {
    PENDING("대기중"),       // 요청 대기
    APPROVED("승인됨"),      // 승인 완료
    REJECTED("반려됨"),      // 반려
    CANCELLED("취소됨");     // 취소

    // 동일한 변환 메서드 제공
}
```

#### ConsultationStatus.java (55줄)
```java
public enum ConsultationStatus {
    SCHEDULED("예정됨"),     // 예정
    IN_PROGRESS("진행중"),   // 진행 중
    COMPLETED("완료됨"),     // 완료
    CANCELLED("취소됨");     // 취소

    // 동일한 변환 메서드 제공
}
```

---

### 3. DTO 클래스 (8개)

#### ConsultationRequestDto.java (224줄)
**용도**: 상담 요청 정보 전송 (조회, 상세)

```java
public class ConsultationRequestDto {
    private Long requestIdx;
    private String requesterUserCode;
    private String requesterName;           // 자동 조회
    private String recipientUserCode;
    private String recipientName;           // 자동 조회
    private ConsultationType consultationType;
    private String title;
    private String content;
    private LocalDateTime desiredDate;
    private RequestStatus requestStatus;
    private String acceptMessage;
    private String rejectionReason;
    private String cancelReason;
    private ConsultationStatus consultationStatus;
    private LocalDateTime startedAt;
    private LocalDateTime endedAt;
    private Integer durationMinutes;
    private LocalDateTime lastActivityAt;
    private LocalDateTime createdAt;
    private Boolean hasUnreadMessages;      // 읽지 않은 메시지 여부
}
```

#### ConsultationRequestCreateDto.java (75줄)
**용도**: 상담 요청 생성

```java
public class ConsultationRequestCreateDto {
    private String requesterUserCode;       // JWT에서 추출

    @NotBlank
    private String recipientUserCode;       // 교수 USER_CODE

    @NotNull
    private ConsultationType consultationType;

    @NotBlank
    @Size(max = 100)
    private String title;

    @Size(max = 1000)
    private String content;

    @Future
    private LocalDateTime desiredDate;
}
```

#### ConsultationApproveDto.java
**용도**: 상담 요청 승인

```java
public class ConsultationApproveDto {
    private Long requestIdx;
    private String acceptMessage;           // 승인 메시지 (선택)
}
```

#### ConsultationRejectDto.java
**용도**: 상담 요청 반려

```java
public class ConsultationRejectDto {
    private Long requestIdx;
    private String rejectionReason;         // 반려 사유 (필수)
}
```

#### ConsultationCancelDto.java
**용도**: 상담 요청 취소 (학생)

```java
public class ConsultationCancelDto {
    private Long requestIdx;
    private String cancelReason;            // 취소 사유 (선택)
}
```

#### ConsultationMemoDto.java
**용도**: 교수 메모 작성/수정

```java
public class ConsultationMemoDto {
    private Long requestIdx;
    private String memo;                    // 메모 내용 (TEXT)
}
```

#### ConsultationIdDto.java
**용도**: 상담 시작/종료

```java
public class ConsultationIdDto {
    private Long requestIdx;                // 요청 ID만 전달
}
```

#### ConsultationHistoryRequestDto.java (55줄)
**용도**: 완료된 상담 이력 조회

```java
public class ConsultationHistoryRequestDto {
    private String userCode;                // 조회자 USER_CODE
    private LocalDateTime startDate;        // 조회 시작 날짜 (선택)
    private LocalDateTime endDate;          // 조회 종료 날짜 (선택)
    private Integer page = 0;
    private Integer size = 20;
}
```

---

### 4. Repository

**파일**: [ConsultationRequestRepository.java](../../src/main/java/BlueCrab/com/example/repository/ConsultationRequestRepository.java) (100줄)

#### 기본 조회 메서드 (4개)
```java
// 요청자 기준 목록 (학생이 보낸 요청)
Page<ConsultationRequest> findByRequesterUserCodeOrderByCreatedAtDesc(
    String requesterUserCode, Pageable pageable);

// 수신자 기준 목록 (교수가 받은 요청)
Page<ConsultationRequest> findByRecipientUserCodeOrderByCreatedAtDesc(
    String recipientUserCode, Pageable pageable);

// 요청자 + 상태 필터
Page<ConsultationRequest> findByRequesterUserCodeAndRequestStatusOrderByCreatedAtDesc(
    String requesterUserCode, String requestStatus, Pageable pageable);

// 수신자 + 상태 필터
Page<ConsultationRequest> findByRecipientUserCodeAndRequestStatusOrderByCreatedAtDesc(
    String recipientUserCode, String requestStatus, Pageable pageable);
```

#### Custom Query (8개)
```java
// 진행 중인 상담 조회
@Query("SELECT c FROM ConsultationRequest c " +
       "WHERE (c.requesterUserCode = :userCode OR c.recipientUserCode = :userCode) " +
       "AND c.consultationStatus IN ('SCHEDULED', 'IN_PROGRESS') " +
       "ORDER BY c.lastActivityAt DESC")
Page<ConsultationRequest> findActiveConsultations(@Param("userCode") String userCode, Pageable pageable);

// 자동 종료 대상 조회 (2시간 비활성)
@Query("SELECT c FROM ConsultationRequest c " +
       "WHERE c.consultationStatus = 'IN_PROGRESS' " +
       "AND c.lastActivityAt < :threshold")
List<ConsultationRequest> findInactiveConsultations(@Param("threshold") LocalDateTime threshold);

// 자동 종료 대상 조회 (24시간 제한)
@Query("SELECT c FROM ConsultationRequest c " +
       "WHERE c.consultationStatus = 'IN_PROGRESS' " +
       "AND c.startedAt < :threshold")
List<ConsultationRequest> findLongRunningConsultations(@Param("threshold") LocalDateTime threshold);

// 완료된 상담 이력 조회
@Query("SELECT c FROM ConsultationRequest c " +
       "WHERE (c.requesterUserCode = :userCode OR c.recipientUserCode = :userCode) " +
       "AND c.consultationStatus = 'COMPLETED' " +
       "AND (:startDate IS NULL OR c.endedAt >= :startDate) " +
       "AND (:endDate IS NULL OR c.endedAt <= :endDate) " +
       "ORDER BY c.endedAt DESC")
Page<ConsultationRequest> findCompletedConsultations(...);

// 읽지 않은 요청 개수 (교수용)
@Query("SELECT COUNT(c) FROM ConsultationRequest c " +
       "WHERE c.recipientUserCode = :recipientUserCode " +
       "AND c.requestStatus = 'PENDING'")
long countUnreadRequests(@Param("recipientUserCode") String recipientUserCode);

// 읽지 않은 메시지가 있는 상담 조회
@Query("SELECT c FROM ConsultationRequest c " +
       "WHERE c.consultationStatus = 'IN_PROGRESS' " +
       "AND ((c.requesterUserCode = :userCode AND c.lastReadTimeStudent < c.lastActivityAt) " +
       "OR (c.recipientUserCode = :userCode AND c.lastReadTimeProfessor < c.lastActivityAt))")
List<ConsultationRequest> findConsultationsWithUnreadMessages(@Param("userCode") String userCode);

// 참여자 확인 (권한 검증용)
@Query("SELECT CASE WHEN COUNT(c) > 0 THEN true ELSE false END FROM ConsultationRequest c " +
       "WHERE c.requestIdx = :requestIdx " +
       "AND (c.requesterUserCode = :userCode OR c.recipientUserCode = :userCode)")
boolean isParticipant(@Param("requestIdx") Long requestIdx, @Param("userCode") String userCode);

// 상대방 userCode 조회
@Query("SELECT CASE " +
       "WHEN c.requesterUserCode = :userCode THEN c.recipientUserCode " +
       "ELSE c.requesterUserCode END " +
       "FROM ConsultationRequest c " +
       "WHERE c.requestIdx = :requestIdx")
String findPartnerUserCode(@Param("requestIdx") Long requestIdx, @Param("userCode") String userCode);
```

---

## 📊 파일 통계

| 파일 | 위치 | 라인 수 |
|------|------|---------|
| ConsultationRequest.java | entity/ | 296줄 |
| ConsultationType.java | enums/ | 55줄 |
| RequestStatus.java | enums/ | 55줄 |
| ConsultationStatus.java | enums/ | 55줄 |
| ConsultationRequestDto.java | dto/Consultation/ | 224줄 |
| ConsultationRequestCreateDto.java | dto/Consultation/ | 75줄 |
| ConsultationApproveDto.java | dto/Consultation/ | ~40줄 |
| ConsultationRejectDto.java | dto/Consultation/ | ~40줄 |
| ConsultationCancelDto.java | dto/Consultation/ | ~40줄 |
| ConsultationMemoDto.java | dto/Consultation/ | ~40줄 |
| ConsultationIdDto.java | dto/Consultation/ | ~30줄 |
| ConsultationHistoryRequestDto.java | dto/Consultation/ | 55줄 |
| ConsultationRequestRepository.java | repository/ | 100줄 |
| **합계** | | **~1,105줄** |

---

## 🔧 설계 특징

### 1. 단일 테이블 전략
- **장점**: 조인 불필요, 조회 성능 우수
- **단점**: NULL 허용 컬럼 증가
- **판단**: 상담 요청과 진행이 밀접하게 연관되어 있어 통합이 효율적

### 2. Enum 타입 관리
```java
// DB 저장: String 타입 (예: "PENDING")
@Column(name = "request_status")
private String requestStatus = "PENDING";

// Java 사용: Enum 타입
public RequestStatus getRequestStatusEnum() {
    return RequestStatus.fromDbValue(this.requestStatus);
}

public void setRequestStatusEnum(RequestStatus requestStatus) {
    this.requestStatus = requestStatus != null ? requestStatus.toDbValue() : null;
}
```

### 3. 읽음 처리 설계
- **학생 읽음 시간**: `lastReadTimeStudent`
- **교수 읽음 시간**: `lastReadTimeProfessor`
- **마지막 활동 시간**: `lastActivityAt`
- **읽지 않은 판별**: `lastReadTime < lastActivityAt`

### 4. 자동 타임스탬프
```java
@PrePersist
protected void onCreate() {
    this.createdAt = LocalDateTime.now();
    this.updatedAt = LocalDateTime.now();
}

@PreUpdate
protected void onUpdate() {
    this.updatedAt = LocalDateTime.now();
}
```

---

## 🔄 상태 전이 다이어그램

### RequestStatus (요청 상태)
```
PENDING (대기중)
    ├─> APPROVED (승인됨)  → 교수가 승인
    ├─> REJECTED (반려됨)  → 교수가 반려
    └─> CANCELLED (취소됨) → 학생이 취소
```

### ConsultationStatus (진행 상태)
```
NULL
  ↓ (승인 시)
SCHEDULED (예정됨)
  ↓ (시작 시)
IN_PROGRESS (진행중)
  ↓ (종료 시 또는 자동 종료)
COMPLETED (완료됨)

또는

SCHEDULED → CANCELLED (학생 취소)
IN_PROGRESS → CANCELLED (중단)
```

---

## 💡 비즈니스 규칙

### 요청 생성
- 학생만 가능
- 교수 USER_CODE 필수
- 제목 필수, 최대 100자
- 내용 선택, 최대 1000자

### 요청 승인/반려
- 교수만 가능
- PENDING 상태에서만 가능
- 승인 시 consultationStatus = SCHEDULED
- 반려 사유 필수

### 요청 취소
- 학생만 가능
- PENDING 또는 APPROVED 상태에서만 가능
- 취소 사유 선택

### 상담 시작
- 학생 또는 교수 가능
- SCHEDULED 상태에서만 가능
- startedAt, lastActivityAt 자동 기록

### 상담 종료
- 학생 또는 교수 가능
- IN_PROGRESS 상태에서만 가능
- endedAt 자동 기록
- durationMinutes 자동 계산

### 자동 종료
- 2시간 비활성: lastActivityAt < now - 2h
- 24시간 초과: startedAt < now - 24h

---

## 🧪 데이터 예시

```json
{
  "requestIdx": 1,
  "requesterUserCode": "202012345",
  "requesterName": "홍길동",
  "recipientUserCode": "P001",
  "recipientName": "김교수",
  "consultationType": "ACADEMIC",
  "title": "학점 상담 요청",
  "content": "학점 관리에 대해 상담받고 싶습니다.",
  "desiredDate": "2025-10-25T14:00:00",
  "requestStatus": "APPROVED",
  "acceptMessage": "오후 2시에 연구실로 오세요.",
  "consultationStatus": "SCHEDULED",
  "createdAt": "2025-10-24T10:00:00",
  "hasUnreadMessages": false
}
```

---

## 📝 다음 단계: Phase 2

### Service Layer 구현 (완료)
- ConsultationRequestService 인터페이스
- ConsultationRequestServiceImpl 구현
- 비즈니스 로직 검증
- 트랜잭션 관리

### Phase 3 예정
- Controller Layer
- REST API 엔드포인트
- Security 설정
- API 문서화

---

## 📌 참고 사항

### 패키지 구조
```
BlueCrab.com.example
├── entity
│   └── ConsultationRequest.java
├── enums
│   ├── ConsultationType.java
│   ├── RequestStatus.java
│   └── ConsultationStatus.java
├── dto
│   └── Consultation
│       ├── ConsultationRequestDto.java
│       ├── ConsultationRequestCreateDto.java
│       ├── ConsultationApproveDto.java
│       ├── ConsultationRejectDto.java
│       ├── ConsultationCancelDto.java
│       ├── ConsultationMemoDto.java
│       ├── ConsultationIdDto.java
│       └── ConsultationHistoryRequestDto.java
└── repository
    └── ConsultationRequestRepository.java
```

### Validation 어노테이션
- `@NotBlank`: 필수 문자열
- `@NotNull`: 필수 값
- `@Size`: 문자열 길이 제한
- `@Future`: 미래 날짜만 허용

### Jackson 어노테이션
- `@JsonFormat`: 날짜 포맷 지정
- `@JsonValue`: Enum 직렬화 값
- `@JsonCreator`: Enum 역직렬화 메서드

---

## 📞 문의

**작성자**: BlueCrab Development Team
**작성일**: 2025-10-24
**버전**: 1.0.0
