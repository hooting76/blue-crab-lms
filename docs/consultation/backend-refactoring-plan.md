# 상담 API 간소화 리팩토링 계획

> **작성일**: 2025-10-28
> **대상**: 백엔드 개발팀
> **목적**: 기존 15개 엔드포인트를 7개로 간소화하여 유지보수성과 일관성 향상

---

## 목차

1. [개요](#1-개요)
2. [현재 구조 분석](#2-현재-구조-분석)
3. [변경 계획](#3-변경-계획)
4. [상세 변경 작업](#4-상세-변경-작업)
5. [작업 순서](#5-작업-순서)
6. [파일 변경 목록](#6-파일-변경-목록)
7. [테스트 계획](#7-테스트-계획)
8. [하위 호환성 전략](#8-하위-호환성-전략)
9. [리스크 및 대응 방안](#9-리스크-및-대응-방안)
10. [예상 효과](#10-예상-효과)
11. [중요 보완 사항](#11-중요-보완-사항)

---

## 1. 개요

### 1.1 리팩토링 목표

- **API 간소화**: 15개 엔드포인트 → 7개 (53% 감소)
- **상태 관리 단순화**: 이중 상태 → 단일 상태 통합
- **코드 중복 제거**: 비슷한 기능 통합
- **유지보수성 향상**: 명확한 책임 분리
- **일관성 개선**: RESTful 패턴 준수 (POST-Only)

### 1.2 주요 변경사항

| 항목 | 기존 | 변경 후 | 효과 |
|------|------|---------|------|
| 엔드포인트 수 | 15개 | 7개 | 53% 감소 |
| 상태 필드 | 2개 (requestStatus, consultationStatus) | 1개 (status) | 단순화 |
| 상태 값 | 7개 분산 | 6개 통합 | 명확화 |
| 목록 조회 API | 4개 | 1개 | 통합 |
| 상태 변경 API | 5개 | 1개 | 통합 |
| 메모 기능 | 있음 | 제거 | 불필요 |

---

## 2. 현재 구조 분석

### 2.1 현재 엔드포인트 (15개)

```
상담 관리 (8개)
1.  POST /api/consultation/request        - 상담 생성
2.  POST /api/consultation/approve        - 승인
3.  POST /api/consultation/reject         - 반려
4.  POST /api/consultation/cancel         - 취소
5.  POST /api/consultation/start          - 시작
6.  POST /api/consultation/end            - 종료
7.  POST /api/consultation/memo           - 메모
8.  POST /api/consultation/read           - 읽음 처리

조회 (6개)
9.  POST /api/consultation/my-requests    - 내가 보낸 요청
10. POST /api/consultation/received       - 받은 요청
11. POST /api/consultation/active         - 진행 중
12. POST /api/consultation/history        - 이력
13. GET  /api/consultation/{id}           - 상세
14. GET  /api/consultation/unread-count   - 읽지 않은 수

채팅 (1개)
15. WebSocket /ws/consultation/chat       - 채팅
```

### 2.2 현재 상태 구조

**이중 상태 관리**
```
RequestStatus (요청 상태)
- PENDING
- APPROVED
- REJECTED
- CANCELLED

ConsultationStatus (진행 상태)
- SCHEDULED
- IN_PROGRESS
- COMPLETED
```

**문제점**
- 상태가 2개 필드에 분산되어 혼란
- 상태 전환 규칙이 명확하지 않음
- 어떤 상태를 확인해야 하는지 불명확

### 2.3 현재 Entity 구조

```java
@Entity
@Table(name = "CONSULTATION_REQUEST_TBL")
public class ConsultationRequest {
    // 요청 정보
    private String requesterUserCode;
    private String recipientUserCode;
    private String consultationType;
    private String title;
    private String content;

    // 이중 상태 (문제!)
    private String requestStatus;       // PENDING, APPROVED, REJECTED, CANCELLED
    private String consultationStatus;  // SCHEDULED, IN_PROGRESS, COMPLETED

    // 사유 필드
    private String acceptMessage;
    private String rejectionReason;
    private String cancelReason;

    // 메모 (사용 안 함)
    private String memo;

    // 기타
    private LocalDateTime startedAt;
    private LocalDateTime endedAt;
    // ...
}
```

---

## 3. 변경 계획

### 3.1 새로운 엔드포인트 구조 (7개)

```
핵심 CRUD (4개)
1. POST /api/consultation/list           - 목록 조회 통합 ⭐
2. POST /api/consultation/detail         - 상세 조회
3. POST /api/consultation/create         - 상담 생성
4. POST /api/consultation/status         - 상태 변경 통합 ⭐

보조 기능 (2개)
5. POST /api/consultation/read           - 읽음 처리
6. GET  /api/consultation/unread-count   - 읽지 않은 수

채팅 (1개)
7. WebSocket /ws/consultation/chat       - 채팅
```

### 3.2 통합된 상태 관리

**단일 status 필드**
```
PENDING      → 요청 대기 (초기 상태)
APPROVED     → 승인됨
REJECTED     → 반려됨 ⚫ 종료 상태
CANCELLED    → 취소됨 ⚫ 종료 상태
IN_PROGRESS  → 진행 중
COMPLETED    → 완료됨 ⚫ 종료 상태
```

**상태 전환 규칙**
```
PENDING      → APPROVED | REJECTED
APPROVED     → IN_PROGRESS | CANCELLED
IN_PROGRESS  → COMPLETED | CANCELLED
REJECTED     → (종료)
CANCELLED    → (종료)
COMPLETED    → (종료)
```

### 3.3 제거되는 기능

| 기능 | 이유 |
|------|------|
| 메모 엔드포인트 | DTO에 누락, 사용 안 함 |
| 재신청 로직 | 프론트엔드에서 새로 생성으로 대체 |
| approve/reject/cancel/start/end | status 엔드포인트로 통합 |
| my-requests/received/active/history | list 엔드포인트로 통합 |

---

## 4. 상세 변경 작업

### 4.1 Phase 1: DTO 생성 및 수정

#### 4.1.1 새 DTO 생성

**1. ConsultationListRequest.java**
```java
package BlueCrab.com.example.dto.Consultation;

import lombok.Data;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Min;
import javax.validation.constraints.Max;
import java.time.LocalDate;

@Data
public class ConsultationListRequest {

    @NotNull(message = "조회 유형은 필수입니다")
    private ViewType viewType;  // SENT, RECEIVED, ACTIVE, HISTORY

    private String status;      // 선택: 상태 필터

    private LocalDate startDate;  // 선택: 기간 필터
    private LocalDate endDate;

    @Min(0)
    private int page = 0;

    @Min(1)
    @Max(100)
    private int size = 10;
}
```

**2. ConsultationStatusRequest.java**
```java
package BlueCrab.com.example.dto.Consultation;

import lombok.Data;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

@Data
public class ConsultationStatusRequest {

    @NotNull(message = "상담 ID는 필수입니다")
    private Long id;

    @NotNull(message = "변경할 상태는 필수입니다")
    private String status;  // APPROVED, REJECTED, CANCELLED, IN_PROGRESS, COMPLETED

    @Size(max = 500, message = "사유/메시지는 500자 이내로 작성해주세요")
    private String reason;
    // - APPROVED → 승인 안내사항 (선택)
    // - REJECTED → 반려 사유 (필수)
    // - CANCELLED → 취소 사유 (필수)
    // - IN_PROGRESS, COMPLETED → 사용 안 함
}
```

**3. ConsultationDetailRequest.java**
```java
package BlueCrab.com.example.dto.Consultation;

import lombok.Data;
import javax.validation.constraints.NotNull;

@Data
public class ConsultationDetailRequest {

    @NotNull(message = "상담 ID는 필수입니다")
    private Long id;
}
```

**4. ViewType.java (Enum)**
```java
package BlueCrab.com.example.dto.Consultation;

public enum ViewType {
    SENT,      // 내가 보낸 요청
    RECEIVED,  // 받은 요청
    ACTIVE,    // 진행 중
    HISTORY    // 이력
}
```

#### 4.1.2 기존 DTO 수정

**ConsultationRequestDto.java**
```java
// 제거
- private String requestStatus;
- private String consultationStatus;
- private String acceptMessage;      // statusReason으로 통합
- private String rejectionReason;    // statusReason으로 통합
- private String cancelReason;       // statusReason으로 통합
- private String memo;

// 추가
+ private String status;          // 통합된 상태
+ private String statusReason;    // 통합된 사유
  // APPROVED → 승인 안내사항 (선택)
  // REJECTED → 반려 사유 (필수)
  // CANCELLED → 취소 사유 (필수)
```

**ConsultationRequestCreateDto.java**
```java
// 재신청 관련 필드 제거
- private Long resubmitId;
- private Long originalConsultationId;

// 순수 생성 필드만 유지
- recipientUserCode
- consultationType
- title
- content
- desiredDate
```

#### 4.1.3 사용 중단할 DTO

다음 DTO를 `@Deprecated` 표시 후 제거:
- ConsultationApproveDto.java
- ConsultationRejectDto.java
- ConsultationCancelDto.java
- ConsultationMemoDto.java
- ConsultationHistoryRequestDto.java

---

### 4.2 Phase 2: Entity 수정

**ConsultationRequest.java**

```java
@Entity
@Table(name = "CONSULTATION_REQUEST_TBL")
public class ConsultationRequest {

    // ... 기존 필드 유지 ...

    // ========== 제거할 필드 (마이그레이션 후) ==========
    @Deprecated
    @Column(name = "request_status", length = 20)
    private String requestStatus;  // 제거 예정

    @Deprecated
    @Column(name = "consultation_status", length = 20)
    private String consultationStatus;  // 제거 예정

    @Deprecated
    @Column(name = "memo", columnDefinition = "TEXT")
    private String memo;  // 제거 예정

    @Deprecated
    @Column(name = "accept_message", length = 500)
    private String acceptMessage;  // 제거 예정 (statusReason으로 통합)

    @Deprecated
    @Column(name = "rejection_reason", length = 500)
    private String rejectionReason;  // 제거 예정 (statusReason으로 통합)

    @Deprecated
    @Column(name = "cancel_reason", length = 500)
    private String cancelReason;  // 제거 예정 (statusReason으로 통합)

    // ========== 새로운 필드 ==========
    @Column(name = "status", nullable = false, length = 20)
    private String status = "PENDING";  // 통합된 상태

    @Column(name = "status_reason", length = 500)
    private String statusReason;  // 통합된 사유
    // - APPROVED → 승인 안내사항 (선택)
    // - REJECTED → 반려 사유 (필수)
    // - CANCELLED → 취소 사유 (필수)

    @Column(name = "status_changed_at")
    private LocalDateTime statusChangedAt;  // 상태 변경 시각

    @Column(name = "status_changed_by", length = 20)
    private String statusChangedBy;  // 상태 변경자

    // ... 기타 필드 ...
}
```

**마이그레이션 전략**

1. **단계 1**: 새 컬럼 추가
   ```sql
   ALTER TABLE CONSULTATION_REQUEST_TBL
   ADD COLUMN status VARCHAR(20) DEFAULT 'PENDING';

   ALTER TABLE CONSULTATION_REQUEST_TBL
   ADD COLUMN status_reason VARCHAR(500);

   ALTER TABLE CONSULTATION_REQUEST_TBL
   ADD COLUMN status_changed_at TIMESTAMP;

   ALTER TABLE CONSULTATION_REQUEST_TBL
   ADD COLUMN status_changed_by VARCHAR(20);
   ```

2. **단계 2**: 기존 데이터 마이그레이션
   ```sql
   -- 상태 매핑 로직
   UPDATE CONSULTATION_REQUEST_TBL
   SET status =
       CASE
           WHEN request_status = 'PENDING' THEN 'PENDING'
           WHEN request_status = 'APPROVED' AND consultation_status IS NULL THEN 'APPROVED'
           WHEN request_status = 'APPROVED' AND consultation_status = 'IN_PROGRESS' THEN 'IN_PROGRESS'
           WHEN request_status = 'APPROVED' AND consultation_status = 'COMPLETED' THEN 'COMPLETED'
           WHEN request_status = 'REJECTED' THEN 'REJECTED'
           WHEN request_status = 'CANCELLED' THEN 'CANCELLED'
           ELSE 'PENDING'
       END;

   -- 사유 통합
   UPDATE CONSULTATION_REQUEST_TBL
   SET status_reason =
       COALESCE(rejection_reason, cancel_reason);
   ```

3. **단계 3**: 기존 컬럼 제거 (리팩토링 완료 후)
   ```sql
   ALTER TABLE CONSULTATION_REQUEST_TBL
   DROP COLUMN request_status,
   DROP COLUMN consultation_status,
   DROP COLUMN memo,
   DROP COLUMN accept_message,
   DROP COLUMN rejection_reason,
   DROP COLUMN cancel_reason;
   ```

---

### 4.3 Phase 3: Service 레이어 리팩토링

#### 4.3.1 StatusTransitionValidator 생성

```java
package BlueCrab.com.example.validator;

import java.util.Map;
import java.util.Set;

/**
 * 상담 상태 전환 검증 클래스
 */
public class StatusTransitionValidator {

    // 허용된 상태 전환 규칙
    private static final Map<String, Set<String>> VALID_TRANSITIONS = Map.of(
        "PENDING", Set.of("APPROVED", "REJECTED"),
        "APPROVED", Set.of("IN_PROGRESS", "CANCELLED"),
        "IN_PROGRESS", Set.of("COMPLETED", "CANCELLED"),
        "REJECTED", Set.of(),
        "CANCELLED", Set.of(),
        "COMPLETED", Set.of()
    );

    /**
     * 상태 전환 가능 여부 검증
     *
     * @param currentStatus 현재 상태
     * @param newStatus 새로운 상태
     * @throws IllegalStateException 잘못된 상태 전환 시
     */
    public static void validate(String currentStatus, String newStatus) {
        if (currentStatus == null || newStatus == null) {
            throw new IllegalArgumentException("상태 값은 필수입니다");
        }

        Set<String> allowedStatuses = VALID_TRANSITIONS.get(currentStatus);

        if (allowedStatuses == null) {
            throw new IllegalArgumentException("알 수 없는 상태: " + currentStatus);
        }

        if (!allowedStatuses.contains(newStatus)) {
            throw new IllegalStateException(
                String.format("상태 전환 불가: %s -> %s", currentStatus, newStatus)
            );
        }
    }

    /**
     * 종료 상태 여부 확인
     */
    public static boolean isTerminalStatus(String status) {
        return Set.of("REJECTED", "CANCELLED", "COMPLETED").contains(status);
    }

    /**
     * 사유 필수 여부 확인
     */
    public static boolean requiresReason(String status) {
        return Set.of("REJECTED", "CANCELLED").contains(status);
    }
}
```

#### 4.3.2 Service 인터페이스 수정

**ConsultationRequestService.java**

```java
public interface ConsultationRequestService {

    // ========== 새로운 메서드 ==========

    /**
     * 상담 목록 조회 (통합)
     */
    Page<ConsultationRequestDto> getConsultationList(
        ConsultationListRequest request,
        String userCode
    );

    /**
     * 상담 상세 조회
     */
    ConsultationRequestDto getConsultationDetail(Long id, String userCode);

    /**
     * 상담 생성
     */
    ConsultationRequestDto createConsultation(ConsultationRequestCreateDto dto);

    /**
     * 상태 변경 (통합)
     */
    ConsultationRequestDto changeConsultationStatus(
        ConsultationStatusRequest request,
        String userCode
    );

    // ========== 유지되는 메서드 ==========

    /**
     * 읽음 처리
     */
    ConsultationReadReceiptDto updateReadTime(Long requestIdx, String userCode);

    /**
     * 읽지 않은 요청 수
     */
    long getUnreadRequestCount(String userCode);

    // ========== 제거될 메서드 (@Deprecated) ==========

    @Deprecated
    ConsultationRequestDto createRequest(ConsultationRequestCreateDto createDto);

    @Deprecated
    ConsultationRequestDto approveRequest(ConsultationApproveDto approveDto);

    @Deprecated
    ConsultationRequestDto rejectRequest(ConsultationRejectDto rejectDto);

    @Deprecated
    ConsultationRequestDto cancelRequest(ConsultationCancelDto cancelDto);

    @Deprecated
    ConsultationRequestDto startConsultation(ConsultationIdDto idDto);

    @Deprecated
    ConsultationRequestDto endConsultation(ConsultationIdDto idDto);

    @Deprecated
    ConsultationRequestDto updateMemo(ConsultationMemoDto memoDto);

    @Deprecated
    Page<ConsultationRequestDto> getMyRequests(String userCode, String status, Pageable pageable);

    @Deprecated
    Page<ConsultationRequestDto> getReceivedRequests(String userCode, String status, Pageable pageable);

    @Deprecated
    Page<ConsultationRequestDto> getActiveConsultations(String userCode, Pageable pageable);

    @Deprecated
    Page<ConsultationRequestDto> getConsultationHistory(ConsultationHistoryRequestDto historyDto);
}
```

#### 4.3.3 Service 구현

**ConsultationRequestServiceImpl.java**

```java
@Service
@Slf4j
public class ConsultationRequestServiceImpl implements ConsultationRequestService {

    @Autowired
    private ConsultationRequestRepository consultationRepository;

    // ========== 1. 목록 조회 (통합) ==========

    @Override
    public Page<ConsultationRequestDto> getConsultationList(
        ConsultationListRequest request,
        String userCode) {

        Pageable pageable = PageRequest.of(request.getPage(), request.getSize());
        Page<ConsultationRequest> result;

        switch (request.getViewType()) {
            case SENT:
                result = getSentConsultations(userCode, request.getStatus(), pageable);
                break;
            case RECEIVED:
                result = getReceivedConsultations(userCode, request.getStatus(), pageable);
                break;
            case ACTIVE:
                result = getActiveConsultations(userCode, pageable);
                break;
            case HISTORY:
                result = getHistoryConsultations(userCode, request, pageable);
                break;
            default:
                throw new IllegalArgumentException("알 수 없는 ViewType: " + request.getViewType());
        }

        return result.map(this::toDto);
    }

    private Page<ConsultationRequest> getSentConsultations(
        String userCode, String status, Pageable pageable) {

        if (status != null && !status.isEmpty()) {
            return consultationRepository.findByRequesterUserCodeAndStatus(
                userCode, status, pageable);
        } else {
            return consultationRepository.findByRequesterUserCode(
                userCode, pageable);
        }
    }

    private Page<ConsultationRequest> getReceivedConsultations(
        String userCode, String status, Pageable pageable) {

        if (status != null && !status.isEmpty()) {
            return consultationRepository.findByRecipientUserCodeAndStatus(
                userCode, status, pageable);
        } else {
            return consultationRepository.findByRecipientUserCode(
                userCode, pageable);
        }
    }

    private Page<ConsultationRequest> getActiveConsultations(
        String userCode, Pageable pageable) {

        return consultationRepository
            .findByStatusAndParticipant(
                "IN_PROGRESS", userCode, userCode, pageable);
    }

    private Page<ConsultationRequest> getHistoryConsultations(
        String userCode, ConsultationListRequest request, Pageable pageable) {

        Set<String> terminalStatuses = Set.of("COMPLETED", "CANCELLED", "REJECTED");

        if (request.getStartDate() != null && request.getEndDate() != null) {
            return consultationRepository
                .findByStatusInAndParticipantAndDateRange(
                    terminalStatuses,
                    userCode,
                    userCode,
                    request.getStartDate().atStartOfDay(),
                    request.getEndDate().atTime(23, 59, 59),
                    pageable);
        } else {
            return consultationRepository
                .findByStatusInAndParticipant(
                    terminalStatuses, userCode, userCode, pageable);
        }
    }

    // ========== 2. 상세 조회 ==========

    @Override
    public ConsultationRequestDto getConsultationDetail(Long id, String userCode) {
        ConsultationRequest consultation = consultationRepository
            .findById(id)
            .orElseThrow(() -> new RuntimeException("상담을 찾을 수 없습니다"));

        // 권한 확인
        validateParticipant(consultation, userCode);

        return toDto(consultation);
    }

    // ========== 3. 상담 생성 ==========

    @Override
    public ConsultationRequestDto createConsultation(ConsultationRequestCreateDto dto) {
        ConsultationRequest consultation = new ConsultationRequest();
        consultation.setRequesterUserCode(dto.getRequesterUserCode());
        consultation.setRecipientUserCode(dto.getRecipientUserCode());
        consultation.setConsultationType(dto.getConsultationType());
        consultation.setTitle(dto.getTitle());
        consultation.setContent(dto.getContent());
        consultation.setDesiredDate(dto.getDesiredDate());
        consultation.setStatus("PENDING");
        consultation.setCreatedAt(LocalDateTime.now());
        consultation.setUpdatedAt(LocalDateTime.now());

        ConsultationRequest saved = consultationRepository.save(consultation);

        log.info("상담 생성 완료: requestIdx={}", saved.getRequestIdx());
        return toDto(saved);
    }

    // ========== 4. 상태 변경 (통합) ==========

    @Override
    @Transactional
    public ConsultationRequestDto changeConsultationStatus(
        ConsultationStatusRequest request,
        String userCode) {

        // 1. 상담 조회
        ConsultationRequest consultation = consultationRepository
            .findById(request.getId())
            .orElseThrow(() -> new RuntimeException("상담을 찾을 수 없습니다"));

        // 2. 권한 검증
        validateStatusChangePermission(consultation, userCode, request.getStatus());

        // 3. 상태 전환 규칙 검증
        StatusTransitionValidator.validate(
            consultation.getStatus(),
            request.getStatus()
        );

        // 4. 필수 필드 검증
        if (StatusTransitionValidator.requiresReason(request.getStatus())) {
            if (request.getReason() == null || request.getReason().isEmpty()) {
                throw new IllegalArgumentException("사유는 필수입니다");
            }
        }

        // 5. 상태 업데이트
        String previousStatus = consultation.getStatus();
        consultation.setStatus(request.getStatus());
        consultation.setStatusReason(request.getReason());
        consultation.setStatusChangedAt(LocalDateTime.now());
        consultation.setStatusChangedBy(userCode);

        // 6. 상태별 추가 처리
        handleStatusSpecificLogic(consultation, request.getStatus());

        // 7. 저장
        ConsultationRequest saved = consultationRepository.save(consultation);

        log.info("상태 변경 완료: requestIdx={}, {} -> {}",
            saved.getRequestIdx(), previousStatus, request.getStatus());

        return toDto(saved);
    }

    private void validateStatusChangePermission(
        ConsultationRequest consultation,
        String userCode,
        String newStatus) {

        String currentStatus = consultation.getStatus();

        // PENDING → APPROVED/REJECTED: 교수만 가능
        if ("PENDING".equals(currentStatus) &&
            ("APPROVED".equals(newStatus) || "REJECTED".equals(newStatus))) {

            if (!consultation.getRecipientUserCode().equals(userCode)) {
                throw new SecurityException("본인에게 온 요청만 처리 가능합니다");
            }
            return;
        }

        // APPROVED → CANCELLED: 학생/교수 모두 가능
        if ("APPROVED".equals(currentStatus) && "CANCELLED".equals(newStatus)) {
            validateParticipant(consultation, userCode);
            return;
        }

        // APPROVED → IN_PROGRESS: 학생/교수 모두 가능
        if ("APPROVED".equals(currentStatus) && "IN_PROGRESS".equals(newStatus)) {
            validateParticipant(consultation, userCode);
            return;
        }

        // IN_PROGRESS → COMPLETED/CANCELLED: 학생/교수 모두 가능
        if ("IN_PROGRESS".equals(currentStatus) &&
            ("COMPLETED".equals(newStatus) || "CANCELLED".equals(newStatus))) {
            validateParticipant(consultation, userCode);
            return;
        }

        throw new SecurityException("권한이 없습니다");
    }

    private void validateParticipant(ConsultationRequest consultation, String userCode) {
        boolean isParticipant =
            consultation.getRequesterUserCode().equals(userCode) ||
            consultation.getRecipientUserCode().equals(userCode);

        if (!isParticipant) {
            throw new SecurityException("상담 참여자만 접근 가능합니다");
        }
    }

    private void handleStatusSpecificLogic(
        ConsultationRequest consultation,
        String newStatus) {

        switch (newStatus) {
            case "IN_PROGRESS":
                if (consultation.getStartedAt() == null) {
                    consultation.setStartedAt(LocalDateTime.now());
                }
                consultation.setLastActivityAt(LocalDateTime.now());
                break;

            case "COMPLETED":
                if (consultation.getEndedAt() == null) {
                    consultation.setEndedAt(LocalDateTime.now());
                }
                if (consultation.getStartedAt() != null) {
                    long minutes = ChronoUnit.MINUTES.between(
                        consultation.getStartedAt(),
                        consultation.getEndedAt()
                    );
                    consultation.setDurationMinutes((int) minutes);
                }
                break;

            default:
                // 기타 상태는 추가 처리 없음
                break;
        }
    }

    // ========== Helper Methods ==========

    private ConsultationRequestDto toDto(ConsultationRequest entity) {
        ConsultationRequestDto dto = new ConsultationRequestDto();

        // 기본 정보
        dto.setRequestIdx(entity.getRequestIdx());
        dto.setRequesterUserCode(entity.getRequesterUserCode());
        dto.setRecipientUserCode(entity.getRecipientUserCode());
        dto.setConsultationType(entity.getConsultationTypeEnum());
        dto.setTitle(entity.getTitle());
        dto.setContent(entity.getContent());
        dto.setDesiredDate(entity.getDesiredDate());
        dto.setScheduledStartAt(entity.getDesiredDate());

        // 상태 정보
        dto.setStatus(entity.getStatus());
        dto.setStatusReason(entity.getStatusReason());

        // 진행 정보
        dto.setStartedAt(entity.getStartedAt());
        dto.setEndedAt(entity.getEndedAt());
        dto.setDurationMinutes(entity.getDurationMinutes());
        dto.setLastActivityAt(entity.getLastActivityAt());
        dto.setCreatedAt(entity.getCreatedAt());

        // 사용자 표시 정보
        getUserName(entity.getRequesterUserCode()).ifPresent(dto::setRequesterName);
        getUserName(entity.getRecipientUserCode()).ifPresent(dto::setRecipientName);

        // TODO: hasUnreadMessages 계산 로직 적용
        return dto;
    }
}
```

---

### 4.4 Phase 4: Repository 수정

**ConsultationRequestRepository.java**

```java
public interface ConsultationRequestRepository extends JpaRepository<ConsultationRequest, Long> {

    // ========== 새로운 쿼리 메서드 ==========

    // 내가 보낸 요청
    Page<ConsultationRequest> findByRequesterUserCode(
        String requesterUserCode, Pageable pageable);

    Page<ConsultationRequest> findByRequesterUserCodeAndStatus(
        String requesterUserCode, String status, Pageable pageable);

    // 받은 요청
    Page<ConsultationRequest> findByRecipientUserCode(
        String recipientUserCode, Pageable pageable);

    Page<ConsultationRequest> findByRecipientUserCodeAndStatus(
        String recipientUserCode, String status, Pageable pageable);

    // 진행 중 (참여자 기준)
    @Query("SELECT c FROM ConsultationRequest c " +
           "WHERE c.status = :status " +
           "AND (c.requesterUserCode = :requester OR c.recipientUserCode = :recipient)")
    Page<ConsultationRequest> findByStatusAndParticipant(
        @Param("status") String status,
        @Param("requester") String requester,
        @Param("recipient") String recipient,
        Pageable pageable);

    // 이력 (종료 상태, 참여자 기준)
    @Query("SELECT c FROM ConsultationRequest c " +
           "WHERE c.status IN :statuses " +
           "AND (c.requesterUserCode = :requester OR c.recipientUserCode = :recipient)")
    Page<ConsultationRequest> findByStatusInAndParticipant(
        @Param("statuses") Set<String> statuses,
        @Param("requester") String requester,
        @Param("recipient") String recipient,
        Pageable pageable);

    // 이력 + 기간 필터
    @Query("SELECT c FROM ConsultationRequest c " +
           "WHERE c.status IN :statuses " +
           "AND (c.requesterUserCode = :requester OR c.recipientUserCode = :recipient) " +
           "AND c.createdAt BETWEEN :startDate AND :endDate")
    Page<ConsultationRequest> findByStatusInAndParticipantAndDateRange(
        @Param("statuses") Set<String> statuses,
        @Param("requester") String requester,
        @Param("recipient") String recipient,
        @Param("startDate") LocalDateTime startDate,
        @Param("endDate") LocalDateTime endDate,
        Pageable pageable);

    // 읽지 않은 요청 수 (교수용)
    @Query("SELECT COUNT(c) FROM ConsultationRequest c " +
           "WHERE c.recipientUserCode = :recipientUserCode " +
           "AND c.status = 'PENDING' " +
           "AND c.lastReadTimeProfessor IS NULL")
    long countUnreadRequestsByRecipient(@Param("recipientUserCode") String recipientUserCode);

    // ========== 제거될 쿼리 (@Deprecated) ==========

    @Deprecated
    Page<ConsultationRequest> findByRequesterUserCodeAndRequestStatus(
        String requesterUserCode, String requestStatus, Pageable pageable);

    @Deprecated
    Page<ConsultationRequest> findByRecipientUserCodeAndRequestStatus(
        String recipientUserCode, String requestStatus, Pageable pageable);

    @Deprecated
    @Query("SELECT c FROM ConsultationRequest c " +
           "WHERE c.consultationStatus = :consultationStatus " +
           "AND (c.requesterUserCode = :userCode OR c.recipientUserCode = :userCode)")
    Page<ConsultationRequest> findActiveConsultations(
        @Param("consultationStatus") String consultationStatus,
        @Param("userCode") String userCode,
        Pageable pageable);
}
```

---

### 4.5 Phase 5: Controller 리팩토링

**ConsultationController.java**

```java
@Slf4j
@RestController
@RequestMapping("/api/consultation")
public class ConsultationController {

    @Autowired
    private ConsultationRequestService consultationService;

    @Autowired
    private UserTblRepository userRepository;

    // ========== 새로운 엔드포인트 ==========

    /**
     * 목록 조회 (통합)
     * - SENT: 내가 보낸 요청
     * - RECEIVED: 받은 요청
     * - ACTIVE: 진행 중
     * - HISTORY: 이력
     */
    @PostMapping("/list")
    public ResponseEntity<?> getConsultationList(
            @Valid @RequestBody ConsultationListRequest request,
            Authentication authentication) {
        try {
            UserTbl user = validateAuth(authentication);

            log.info("목록 조회: viewType={}, userCode={}",
                request.getViewType(), user.getUserCode());

            Page<ConsultationRequestDto> result =
                consultationService.getConsultationList(request, user.getUserCode());

            log.info("목록 조회 완료: totalElements={}", result.getTotalElements());
            return ResponseEntity.ok(result);

        } catch (Exception e) {
            log.error("목록 조회 실패", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("목록 조회에 실패했습니다: " + e.getMessage()));
        }
    }

    /**
     * 상세 조회
     */
    @PostMapping("/detail")
    public ResponseEntity<?> getConsultationDetail(
            @Valid @RequestBody ConsultationDetailRequest request,
            Authentication authentication) {
        try {
            UserTbl user = validateAuth(authentication);

            log.info("상세 조회: id={}, userCode={}",
                request.getId(), user.getUserCode());

            ConsultationRequestDto result =
                consultationService.getConsultationDetail(request.getId(), user.getUserCode());

            log.info("상세 조회 완료: requestIdx={}", result.getRequestIdx());
            return ResponseEntity.ok(result);

        } catch (SecurityException e) {
            log.warn("권한 없음: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(createErrorResponse(e.getMessage()));
        } catch (Exception e) {
            log.error("상세 조회 실패", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("상세 조회에 실패했습니다: " + e.getMessage()));
        }
    }

    /**
     * 상담 생성
     */
    @PostMapping("/create")
    public ResponseEntity<?> createConsultation(
            @Valid @RequestBody ConsultationRequestCreateDto createDto,
            Authentication authentication) {
        try {
            UserTbl user = validateAuth(authentication);

            // JWT에서 추출한 사용자 정보를 DTO에 설정
            createDto.setRequesterUserCode(user.getUserCode());

            log.info("상담 생성: requester={}, recipient={}",
                createDto.getRequesterUserCode(), createDto.getRecipientUserCode());

            ConsultationRequestDto result = consultationService.createConsultation(createDto);

            log.info("상담 생성 완료: requestIdx={}", result.getRequestIdx());
            return ResponseEntity.ok(result);

        } catch (Exception e) {
            log.error("상담 생성 실패", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("상담 생성에 실패했습니다: " + e.getMessage()));
        }
    }

    /**
     * 상태 변경 (통합)
     * - APPROVED: 승인
     * - REJECTED: 반려
     * - CANCELLED: 취소
     * - IN_PROGRESS: 시작
     * - COMPLETED: 종료
     */
    @PostMapping("/status")
    public ResponseEntity<?> changeConsultationStatus(
            @Valid @RequestBody ConsultationStatusRequest request,
            Authentication authentication) {
        try {
            UserTbl user = validateAuth(authentication);

            log.info("상태 변경: id={}, status={}, userCode={}",
                request.getId(), request.getStatus(), user.getUserCode());

            ConsultationRequestDto result =
                consultationService.changeConsultationStatus(request, user.getUserCode());

            log.info("상태 변경 완료: requestIdx={}, status={}",
                result.getRequestIdx(), result.getStatus());
            return ResponseEntity.ok(result);

        } catch (IllegalStateException e) {
            log.warn("상태 전환 불가: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(createErrorResponse(e.getMessage()));
        } catch (SecurityException e) {
            log.warn("권한 없음: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(createErrorResponse(e.getMessage()));
        } catch (Exception e) {
            log.error("상태 변경 실패", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("상태 변경에 실패했습니다: " + e.getMessage()));
        }
    }

    // ========== 유지되는 엔드포인트 ==========

    /**
     * 읽음 처리
     */
    @PostMapping("/read")
    public ResponseEntity<?> updateReadTime(
            @Valid @RequestBody ConsultationIdDto idDto,
            Authentication authentication) {
        // 기존 로직 유지
        // ...
    }

    /**
     * 읽지 않은 수
     */
    @GetMapping("/unread-count")
    public ResponseEntity<?> getUnreadRequestCount(Authentication authentication) {
        // 기존 로직 유지
        // ...
    }

    // ========== Helper Methods ==========

    private UserTbl validateAuth(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new SecurityException("인증이 필요합니다.");
        }

        String userEmail = authentication.getName();
        return userRepository.findByUserEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("사용자 정보를 찾을 수 없습니다."));
    }

    private Map<String, Object> createErrorResponse(String message) {
        Map<String, Object> error = new HashMap<>();
        error.put("success", false);
        error.put("message", message);
        error.put("timestamp", System.currentTimeMillis());
        return error;
    }
}
```

---

## 5. 작업 순서

### Step 1: 준비 작업 ✅
- [x] 백업 브랜치 생성
- [x] 테스트 환경 구성
- [x] 변경 계획 문서 작성

### Step 2: DTO 레이어
- [ ] `ViewType.java` Enum 생성
- [ ] `ConsultationListRequest.java` 생성
- [ ] `ConsultationStatusRequest.java` 생성
- [ ] `ConsultationDetailRequest.java` 생성
- [ ] `ConsultationRequestDto.java` 수정 (status 통합)
- [ ] `ConsultationRequestCreateDto.java` 수정 (재신청 제거)
- [ ] 기존 DTO에 `@Deprecated` 표시

### Step 3: Entity 레이어
- [ ] `ConsultationRequest.java`에 새 필드 추가
  - status
  - statusReason
  - statusChangedAt
  - statusChangedBy
- [ ] 기존 필드에 `@Deprecated` 표시
- [ ] 마이그레이션 SQL 스크립트 작성

### Step 4: Validator 레이어
- [ ] `StatusTransitionValidator.java` 생성
- [ ] 단위 테스트 작성

### Step 5: Repository 레이어
- [ ] 새 쿼리 메서드 추가
- [ ] 기존 쿼리에 `@Deprecated` 표시
- [ ] 쿼리 테스트 작성

### Step 6: Service 레이어
- [ ] `ConsultationRequestService.java` 인터페이스 수정
- [ ] `getConsultationList()` 구현
- [ ] `getConsultationDetail()` 구현
- [ ] `createConsultation()` 구현
- [ ] `changeConsultationStatus()` 구현
- [ ] 기존 메서드에 `@Deprecated` 표시
- [ ] 단위 테스트 작성

### Step 7: Controller 레이어
- [ ] `/list` 엔드포인트 추가
- [ ] `/detail` 엔드포인트 추가
- [ ] `/create` 엔드포인트 추가
- [ ] `/status` 엔드포인트 추가
- [ ] 기존 엔드포인트에 `@Deprecated` 표시
- [ ] 통합 테스트 작성

### Step 8: 데이터 마이그레이션
- [ ] 마이그레이션 스크립트 실행
- [ ] 데이터 검증
- [ ] 기존 컬럼 제거

### Step 9: 레거시 코드 제거
- [ ] 사용 중단된 DTO 삭제
- [ ] 사용 중단된 엔드포인트 삭제
- [ ] 사용 중단된 Service 메서드 삭제
- [ ] 사용 중단된 Repository 쿼리 삭제

### Step 10: 문서화 및 배포
- [ ] API 문서 업데이트
- [ ] CHANGELOG 작성
- [ ] 프론트엔드 팀 공유
- [ ] 배포 및 모니터링

---

## 6. 파일 변경 목록

### 6.1 새로 생성할 파일

```
src/main/java/BlueCrab/com/example/
├── dto/Consultation/
│   ├── ConsultationListRequest.java       ✨ 새 파일
│   ├── ConsultationStatusRequest.java     ✨ 새 파일
│   ├── ConsultationDetailRequest.java     ✨ 새 파일
│   └── ViewType.java                      ✨ 새 파일 (Enum)
│
└── validator/
    └── StatusTransitionValidator.java     ✨ 새 파일

src/main/resources/db/migration/
└── V2_0__simplify_consultation_api.sql    ✨ 새 파일
```

### 6.2 수정할 파일

```
src/main/java/BlueCrab/com/example/
├── entity/
│   └── ConsultationRequest.java          📝 status 필드 통합
│
├── dto/Consultation/
│   ├── ConsultationRequestDto.java       📝 status 통합
│   └── ConsultationRequestCreateDto.java 📝 재신청 필드 제거
│
├── repository/
│   └── ConsultationRequestRepository.java 📝 새 쿼리 추가
│
├── service/
│   ├── ConsultationRequestService.java    📝 인터페이스 변경
│   └── ConsultationRequestServiceImpl.java 📝 구현 변경
│
└── controller/
    └── ConsultationController.java        📝 엔드포인트 통합
```

### 6.3 삭제할 파일 (레거시 제거 단계)

```
src/main/java/BlueCrab/com/example/dto/Consultation/
├── ConsultationApproveDto.java           🗑️ 삭제 예정
├── ConsultationRejectDto.java            🗑️ 삭제 예정
├── ConsultationCancelDto.java            🗑️ 삭제 예정
├── ConsultationMemoDto.java              🗑️ 삭제 예정
└── ConsultationHistoryRequestDto.java    🗑️ 삭제 예정
```

---

## 7. 테스트 계획

### 7.1 단위 테스트

**StatusTransitionValidatorTest.java**
```java
@Test
void testValidTransitions() {
    // PENDING -> APPROVED: 성공
    assertDoesNotThrow(() ->
        StatusTransitionValidator.validate("PENDING", "APPROVED"));

    // PENDING -> REJECTED: 성공
    assertDoesNotThrow(() ->
        StatusTransitionValidator.validate("PENDING", "REJECTED"));

    // PENDING -> COMPLETED: 실패
    assertThrows(IllegalStateException.class, () ->
        StatusTransitionValidator.validate("PENDING", "COMPLETED"));
}

@Test
void testTerminalStatusCheck() {
    assertTrue(StatusTransitionValidator.isTerminalStatus("COMPLETED"));
    assertTrue(StatusTransitionValidator.isTerminalStatus("CANCELLED"));
    assertTrue(StatusTransitionValidator.isTerminalStatus("REJECTED"));
    assertFalse(StatusTransitionValidator.isTerminalStatus("PENDING"));
}

@Test
void testRequiresReason() {
    assertTrue(StatusTransitionValidator.requiresReason("REJECTED"));
    assertTrue(StatusTransitionValidator.requiresReason("CANCELLED"));
    assertFalse(StatusTransitionValidator.requiresReason("APPROVED"));
}
```

**ConsultationRequestServiceImplTest.java**
```java
@Test
void testGetConsultationList_SENT() {
    // Given
    ConsultationListRequest request = new ConsultationListRequest();
    request.setViewType(ViewType.SENT);
    request.setPage(0);
    request.setSize(10);

    // When
    Page<ConsultationRequestDto> result =
        service.getConsultationList(request, "202012345");

    // Then
    assertNotNull(result);
    assertTrue(result.getTotalElements() >= 0);
}

@Test
void testChangeConsultationStatus_Approve() {
    // Given
    ConsultationStatusRequest request = new ConsultationStatusRequest();
    request.setId(1L);
    request.setStatus("APPROVED");

    // When
    ConsultationRequestDto result =
        service.changeConsultationStatus(request, "P001");

    // Then
    assertEquals("APPROVED", result.getStatus());
}

@Test
void testChangeConsultationStatus_InvalidTransition() {
    // Given
    ConsultationStatusRequest request = new ConsultationStatusRequest();
    request.setId(1L);
    request.setStatus("COMPLETED");

    // When & Then
    assertThrows(IllegalStateException.class, () ->
        service.changeConsultationStatus(request, "202012345"));
}
```

### 7.2 통합 테스트

**ConsultationControllerIntegrationTest.java**
```java
@Test
@WithMockUser(username = "student@test.com")
void testListEndpoint_SENT() throws Exception {
    mockMvc.perform(post("/api/consultation/list")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"viewType\":\"SENT\",\"page\":0,\"size\":10}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content").isArray());
}

@Test
@WithMockUser(username = "professor@test.com")
void testStatusEndpoint_Approve() throws Exception {
    mockMvc.perform(post("/api/consultation/status")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"id\":1,\"status\":\"APPROVED\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("APPROVED"));
}

@Test
@WithMockUser(username = "professor@test.com")
void testStatusEndpoint_RejectWithoutReason() throws Exception {
    mockMvc.perform(post("/api/consultation/status")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"id\":1,\"status\":\"REJECTED\"}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("사유는 필수입니다"));
}
```

### 7.3 E2E 테스트 시나리오

**시나리오 1: 학생 플로우**
```
1. 학생이 상담 신청 (POST /create)
   → status: PENDING
2. 교수가 승인 (POST /status - APPROVED)
   → status: APPROVED
3. 학생이 상담 시작 (POST /status - IN_PROGRESS)
   → status: IN_PROGRESS
4. 교수가 상담 종료 (POST /status - COMPLETED)
   → status: COMPLETED
```

**시나리오 2: 반려 플로우**
```
1. 학생이 상담 신청 (POST /create)
   → status: PENDING
2. 교수가 반려 (POST /status - REJECTED + reason)
   → status: REJECTED, statusReason 설정
3. 학생이 새로 신청 (POST /create)
   → 새 상담 생성 (독립적)
```

**시나리오 3: 취소 플로우**
```
1. 학생이 상담 신청 (POST /create)
   → status: PENDING
2. 교수가 승인 (POST /status - APPROVED)
   → status: APPROVED
3. 학생이 취소 (POST /status - CANCELLED + reason)
   → status: CANCELLED, statusReason 설정
```

---

## 8. 하위 호환성 전략

### 8.1 단계적 마이그레이션 (5주)

**Week 1-2: 새 API와 기존 API 병행 운영**
- 새 엔드포인트 추가 (/list, /detail, /create, /status)
- 기존 엔드포인트 유지 (/request, /approve, /reject, ...)
- 새 API 테스트 및 안정화
- 프론트엔드 팀에 새 API 문서 공유

**Week 3: 프론트엔드 마이그레이션**
- 프론트엔드가 새 API로 전환
- 기존 API는 여전히 동작
- 모니터링: 기존 API 사용량 감소 확인

**Week 4: 기존 API Deprecated**
- 기존 엔드포인트에 `@Deprecated` 표시
- 응답 헤더에 경고 추가: `X-Api-Deprecated: true`
- 로그에 경고 메시지 기록

**Week 5: 기존 API 제거**
- 기존 엔드포인트 완전 제거
- 기존 DTO 삭제
- 기존 Service 메서드 삭제
- Entity의 deprecated 컬럼 제거

### 8.2 클라이언트 공지

**공지 채널**
- Slack #dev-announcements
- 이메일 발송
- API 문서 상단에 배너

**공지 내용**
```markdown
# 상담 API 간소화 안내

## 변경 내역
- 15개 엔드포인트 → 7개로 간소화
- 상태 관리 단순화 (단일 status 필드)
- 메모 기능 제거

## 마이그레이션 가이드
- 신규 API 문서: /docs/consultation/frontend-consultation-integration.md
- 기존 API 지원 종료: 2025-12-01

## 문의
- 백엔드 팀: backend@bluecrab.com
```

### 8.3 Deprecated 응답 헤더 추가

```java
@PostMapping("/approve")
@Deprecated
public ResponseEntity<?> approveRequest(...) {
    HttpHeaders headers = new HttpHeaders();
    headers.add("X-Api-Deprecated", "true");
    headers.add("X-Api-Replacement", "POST /api/consultation/status");
    headers.add("X-Api-Sunset", "2025-12-01");

    // 기존 로직...

    return ResponseEntity.ok()
            .headers(headers)
            .body(result);
}
```

---

## 9. 리스크 및 대응 방안

### 9.1 리스크: 데이터 마이그레이션 실패

**발생 가능성**: 중간
**영향도**: 높음

**대응 방안**
1. **사전 백업**
   - 전체 DB 백업
   - 특정 테이블 백업
   ```sql
   CREATE TABLE CONSULTATION_REQUEST_TBL_BACKUP AS
   SELECT * FROM CONSULTATION_REQUEST_TBL;
   ```

2. **롤백 스크립트 준비**
   ```sql
   -- 롤백 시 실행
   DROP TABLE CONSULTATION_REQUEST_TBL;
   ALTER TABLE CONSULTATION_REQUEST_TBL_BACKUP
   RENAME TO CONSULTATION_REQUEST_TBL;
   ```

3. **단계적 마이그레이션**
   - 컬럼 추가 → 데이터 복사 → 검증 → 기존 컬럼 제거
   - 각 단계마다 검증

4. **테스트 환경에서 사전 실행**
   - 프로덕션과 동일한 데이터로 테스트

### 9.2 리스크: 프론트엔드 호환성 문제

**발생 가능성**: 중간
**영향도**: 중간

**대응 방안**
1. **기존 API 일정 기간 유지**
   - 최소 1개월 병행 운영
   - 점진적 전환 허용

2. **마이그레이션 가이드 제공**
   - Before/After 코드 예시
   - FAQ 작성

3. **모니터링**
   - 기존 API 사용량 추적
   - 에러 로그 모니터링

### 9.3 리스크: 상태 전환 로직 누락

**발생 가능성**: 낮음
**영향도**: 높음

**대응 방안**
1. **포괄적 테스트 케이스**
   - 모든 상태 전환 조합 테스트
   - Edge case 테스트 (동시 요청, 잘못된 상태 등)

2. **코드 리뷰**
   - 2명 이상 리뷰 필수
   - 상태 전환 매트릭스 검증

3. **프로덕션 모니터링**
   - 상태 전환 실패 로그 추적
   - Slack 알림 설정

### 9.4 리스크: 성능 저하

**발생 가능성**: 낮음
**영향도**: 중간

**대응 방안**
1. **쿼리 최적화**
   - 인덱스 생성
   ```sql
   CREATE INDEX idx_consultation_status
   ON CONSULTATION_REQUEST_TBL(status);

   CREATE INDEX idx_consultation_participant
   ON CONSULTATION_REQUEST_TBL(requester_user_code, recipient_user_code);
   ```

2. **성능 테스트**
   - JMeter로 부하 테스트
   - 기존 API와 비교

3. **모니터링**
   - API 응답 시간 측정
   - DB 쿼리 시간 모니터링

---

## 10. 예상 효과

### 10.1 정량적 효과

| 지표 | 기존 | 변경 후 | 개선율 |
|------|------|---------|--------|
| 엔드포인트 수 | 15개 | 7개 | **-53%** |
| Controller 메서드 수 | 14개 | 6개 | **-57%** |
| Service 메서드 수 | 14개 | 6개 | **-57%** |
| DTO 클래스 수 | 12개 | 7개 | **-42%** |
| 상태 필드 수 | 2개 | 1개 | **-50%** |
| 코드 라인 수 (Controller) | ~600줄 | ~350줄 | **-42%** |

### 10.2 정성적 효과

**개발자 경험**
- ✅ API 구조 이해 용이
- ✅ 신규 개발자 온보딩 시간 단축
- ✅ 유지보수 부담 감소
- ✅ 버그 발생 가능성 감소

**코드 품질**
- ✅ 중복 코드 제거
- ✅ 명확한 책임 분리
- ✅ 일관된 네이밍
- ✅ 테스트 커버리지 향상

**사용자 경험**
- ✅ 프론트엔드 학습 곡선 감소
- ✅ API 사용 오류 감소
- ✅ 문서 가독성 향상

### 10.3 예상 절감 효과

**개발 시간**
- 신규 기능 추가: 30% 단축
- 버그 수정: 40% 단축
- 코드 리뷰: 25% 단축

**유지보수 비용**
- 연간 유지보수 시간: 50시간 → 30시간 (40% 감소)
- 버그 발생률: 30% 감소 예상

---

## 11. 다음 단계

### 11.1 즉시 실행

1. ✅ 이 계획 문서 검토 및 승인
2. ✅ 프론트엔드 문서 작성 완료
3. [ ] 백엔드 팀 미팅 일정 잡기
4. [ ] Jira 티켓 생성

### 11.2 1주차 (DTO/Entity/Validator)

1. [ ] DTO 레이어 구현
2. [ ] Entity 수정
3. [ ] Validator 구현
4. [ ] 단위 테스트 작성

### 11.3 2주차 (Repository/Service/Controller)

1. [ ] Repository 구현
2. [ ] Service 구현
3. [ ] Controller 구현
4. [ ] 통합 테스트 작성

### 11.4 3주차 (테스트/배포)

1. [ ] E2E 테스트
2. [ ] 코드 리뷰
3. [ ] 테스트 환경 배포
4. [ ] 프론트엔드 팀 공유

### 11.5 4-5주차 (프로덕션 배포)

1. [ ] 프로덕션 배포
2. [ ] 모니터링
3. [ ] 프론트엔드 마이그레이션 지원
4. [ ] 레거시 코드 제거

---

## 부록 A. 마이그레이션 SQL 스크립트

```sql
-- ========================================
-- V2_0__simplify_consultation_api.sql
-- ========================================

-- Step 1: 새 컬럼 추가
ALTER TABLE CONSULTATION_REQUEST_TBL
ADD COLUMN status VARCHAR(20) DEFAULT 'PENDING' AFTER consultation_status;

ALTER TABLE CONSULTATION_REQUEST_TBL
ADD COLUMN status_reason VARCHAR(500) AFTER status;

ALTER TABLE CONSULTATION_REQUEST_TBL
ADD COLUMN status_changed_at TIMESTAMP AFTER status_reason;

ALTER TABLE CONSULTATION_REQUEST_TBL
ADD COLUMN status_changed_by VARCHAR(20) AFTER status_changed_at;

-- Step 2: 기존 데이터 마이그레이션
UPDATE CONSULTATION_REQUEST_TBL
SET status =
    CASE
        WHEN request_status = 'PENDING' THEN 'PENDING'
        WHEN request_status = 'APPROVED' AND (consultation_status IS NULL OR consultation_status = 'SCHEDULED') THEN 'APPROVED'
        WHEN request_status = 'APPROVED' AND consultation_status = 'IN_PROGRESS' THEN 'IN_PROGRESS'
        WHEN request_status = 'APPROVED' AND consultation_status = 'COMPLETED' THEN 'COMPLETED'
        WHEN request_status = 'REJECTED' THEN 'REJECTED'
        WHEN request_status = 'CANCELLED' THEN 'CANCELLED'
        ELSE 'PENDING'
    END,
    status_reason = COALESCE(rejection_reason, cancel_reason),
    status_changed_at = updated_at;

-- Step 3: 인덱스 생성
CREATE INDEX idx_consultation_status
ON CONSULTATION_REQUEST_TBL(status);

CREATE INDEX idx_consultation_participant
ON CONSULTATION_REQUEST_TBL(requester_user_code, recipient_user_code);

CREATE INDEX idx_consultation_status_created
ON CONSULTATION_REQUEST_TBL(status, created_at);

-- Step 4: NOT NULL 제약조건 추가
ALTER TABLE CONSULTATION_REQUEST_TBL
MODIFY COLUMN status VARCHAR(20) NOT NULL;

-- Step 5: 검증 쿼리
-- 마이그레이션 결과 확인
SELECT
    request_status,
    consultation_status,
    status,
    COUNT(*) as count
FROM CONSULTATION_REQUEST_TBL
GROUP BY request_status, consultation_status, status
ORDER BY request_status, consultation_status;

-- Step 6: 기존 컬럼 제거 (리팩토링 완료 후 실행)
-- 주의: 충분한 테스트 후에만 실행!
-- ALTER TABLE CONSULTATION_REQUEST_TBL
-- DROP COLUMN request_status,
-- DROP COLUMN consultation_status,
-- DROP COLUMN memo,
-- DROP COLUMN accept_message,
-- DROP COLUMN rejection_reason,
-- DROP COLUMN cancel_reason;
```

---

## 부록 B. 롤백 스크립트

```sql
-- ========================================
-- Rollback Script
-- ========================================

-- Step 1: 백업 테이블 확인
SELECT COUNT(*) FROM CONSULTATION_REQUEST_TBL_BACKUP;

-- Step 2: 현재 테이블 삭제
DROP TABLE IF EXISTS CONSULTATION_REQUEST_TBL;

-- Step 3: 백업에서 복원
CREATE TABLE CONSULTATION_REQUEST_TBL AS
SELECT * FROM CONSULTATION_REQUEST_TBL_BACKUP;

-- Step 4: 인덱스 재생성
CREATE INDEX idx_consultation_requester
ON CONSULTATION_REQUEST_TBL(requester_user_code);

CREATE INDEX idx_consultation_recipient
ON CONSULTATION_REQUEST_TBL(recipient_user_code);

-- Step 5: 검증
SELECT COUNT(*) FROM CONSULTATION_REQUEST_TBL;
```

---

## 부록 C. 참고 문서

- [프론트엔드 API 가이드](./frontend-consultation-integration.md)
- [기존 API 문서 (레거시)](./frontend-consultation-integration-legacy.md)
- [Spring Boot 공식 문서](https://spring.io/projects/spring-boot)
- [JPA 쿼리 메서드 가이드](https://docs.spring.io/spring-data/jpa/docs/current/reference/html/)

---

## 11. 중요 보완 사항

> ⚠️ **중요**: 초기 계획에서 누락된 의존성 분석 결과를 바탕으로 아래 사항들을 반드시 보완해야 합니다.

### 11.1 ChatNotificationService 마이그레이션

**문제점**:
- ChatNotificationService가 `acceptMessage`, `rejectionReason`, `cancelReason` 필드에 의존
- [ChatNotificationService.java:196, :218, :241]에서 FCM push 데이터 생성 시 사용
- 단일 `statusReason` 필드로 통합 시 Notification 레이어 수정 필수

**영향 코드**:
```java
// Line 186-206: notifyConsultationApproved()
if (consultation.getAcceptMessage() != null) {
    extra.put("acceptMessage", consultation.getAcceptMessage());
}

// Line 212-229: notifyConsultationRejected()
if (consultation.getRejectionReason() != null) {
    extra.put("rejectionReason", consultation.getRejectionReason());
}

// Line 235-252: notifyConsultationCancelled()
if (consultation.getCancelReason() != null) {
    extra.put("cancelReason", consultation.getCancelReason());
}
```

**해결 방안 (Option B 적용)**: ✅

**ChatNotificationService.java 수정**
```java
/**
 * 상담 요청 승인 알림 전송.
 */
public void notifyConsultationApproved(ConsultationRequest consultation) {
    if (!notificationsEnabled || consultation == null || consultation.getRequestIdx() == null) {
        return;
    }

    Map<String, String> extra = new HashMap<>();
    if (consultation.getDesiredDate() != null) {
        extra.put("scheduledStartAt", consultation.getDesiredDate().format(isoFormatter));
    }
    // statusReason을 acceptMessage로 매핑
    if (consultation.getStatusReason() != null) {
        extra.put("acceptMessage", consultation.getStatusReason());
    }

    sendConsultationEvent(
        consultation,
        consultation.getRequesterUserCode(),
        "CONSULTATION_APPROVED",
        "상담 요청이 승인되었습니다.",
        buildCounterpartMessage(consultation, consultation.getRequesterUserCode(), "상담이 승인되었습니다."),
        extra
    );
}

/**
 * 상담 요청 반려 알림 전송.
 */
public void notifyConsultationRejected(ConsultationRequest consultation) {
    if (!notificationsEnabled || consultation == null || consultation.getRequestIdx() == null) {
        return;
    }

    Map<String, String> extra = new HashMap<>();
    // statusReason을 rejectionReason으로 매핑
    if (consultation.getStatusReason() != null) {
        extra.put("rejectionReason", consultation.getStatusReason());
    }

    sendConsultationEvent(
        consultation,
        consultation.getRequesterUserCode(),
        "CONSULTATION_REJECTED",
        "상담 요청이 반려되었습니다.",
        buildCounterpartMessage(consultation, consultation.getRequesterUserCode(), "상담 요청이 반려되었습니다."),
        extra
    );
}

/**
 * 상담 요청 취소 알림 전송.
 */
public void notifyConsultationCancelled(ConsultationRequest consultation) {
    if (!notificationsEnabled || consultation == null || consultation.getRequestIdx() == null) {
        return;
    }

    Map<String, String> extra = new HashMap<>();
    // statusReason을 cancelReason으로 매핑
    if (consultation.getStatusReason() != null) {
        extra.put("cancelReason", consultation.getStatusReason());
    }

    sendConsultationEvent(
        consultation,
        consultation.getRecipientUserCode(),
        "CONSULTATION_CANCELLED",
        "상담 요청이 취소되었습니다.",
        buildCounterpartMessage(consultation, consultation.getRecipientUserCode(), "상담 요청이 취소되었습니다."),
        extra
    );
}
```

**핵심 변경사항**:
- `consultation.getAcceptMessage()` → `consultation.getStatusReason()`로 변경, FCM 키는 `acceptMessage` 유지
- `consultation.getRejectionReason()` → `consultation.getStatusReason()`로 변경, FCM 키는 `rejectionReason` 유지
- `consultation.getCancelReason()` → `consultation.getStatusReason()`로 변경, FCM 키는 `cancelReason` 유지
- FCM 클라이언트는 기존 키 이름 그대로 사용하므로 프론트엔드 호환성 유지

**추가 작업**:
- [ ] ChatNotificationService.java 수정
- [ ] Notification 통합 테스트 작성
- [ ] FCM push 페이로드 검증

---

### 11.2 Scheduled Jobs 마이그레이션

**문제점**:
- `consultationStatus` 필드에 의존하는 자동 종료 로직 존재
- Repository 쿼리 5개가 `consultationStatus` 사용
- Service 메소드 3개가 `setConsultationStatusEnum()` 호출

**영향 코드**:

**1. ConsultationRequestServiceImpl.java**
```java
// Line 535-560: autoEndInactiveConsultations()
List<ConsultationRequest> inactiveConsultations =
    consultationRepository.findInactiveConsultations(threshold);

// Line 564-589: autoEndLongRunningConsultations()
List<ConsultationRequest> longRunningConsultations =
    consultationRepository.findLongRunningConsultations(threshold);

// Line 656-683: completeConsultation()
consultation.setConsultationStatusEnum(ConsultationStatus.COMPLETED);
```

**2. ConsultationRequestRepository.java**
```java
// Line 41-45: findActiveConsultations
WHERE c.consultationStatus IN ('SCHEDULED', 'IN_PROGRESS')

// Line 48-50: findInactiveConsultations
WHERE c.consultationStatus = 'IN_PROGRESS'

// Line 54-56: findLongRunningConsultations
WHERE c.consultationStatus = 'IN_PROGRESS'

// Line 60-65: findCompletedConsultations
WHERE c.consultationStatus = 'COMPLETED'

// Line 80-83: findConsultationsWithUnreadMessages
WHERE c.consultationStatus = 'IN_PROGRESS'
```

**해결 방안**:

**1. Repository 쿼리 수정**
```java
// Before
@Query("SELECT c FROM ConsultationRequest c " +
       "WHERE c.consultationStatus = 'IN_PROGRESS' " +
       "AND c.lastActivityAt < :threshold")
List<ConsultationRequest> findInactiveConsultations(@Param("threshold") LocalDateTime threshold);

// After
@Query("SELECT c FROM ConsultationRequest c " +
       "WHERE c.status = 'IN_PROGRESS' " +
       "AND c.lastActivityAt < :threshold")
List<ConsultationRequest> findInactiveConsultations(@Param("threshold") LocalDateTime threshold);
```

**2. Service 로직 수정**
```java
// Before
consultation.setConsultationStatusEnum(ConsultationStatus.COMPLETED);

// After
consultation.setStatus("COMPLETED");
consultation.setStatusChangedAt(LocalDateTime.now());
consultation.setStatusChangedBy("SYSTEM_AUTO");
consultation.setStatusReason("자동 종료: 2시간 무활동");
```

※ 시스템이 상태를 변경하는 모든 경로에서 `statusChangedBy`에 식별 가능한 값(예: `"SYSTEM_AUTO"`)을 기록한다.

**3. 통합 상태 매핑**
```java
// 기존 이중 상태 → 새로운 단일 상태 매핑
RequestStatus.PENDING + ConsultationStatus.NULL       → status = "PENDING"
RequestStatus.APPROVED + ConsultationStatus.SCHEDULED → status = "APPROVED"
RequestStatus.APPROVED + ConsultationStatus.IN_PROGRESS → status = "IN_PROGRESS"
RequestStatus.APPROVED + ConsultationStatus.COMPLETED → status = "COMPLETED"
RequestStatus.REJECTED                                → status = "REJECTED"
RequestStatus.CANCELLED                               → status = "CANCELLED"
```

**추가 작업**:
- [ ] Repository 쿼리 5개 수정
- [ ] Service 메소드 3개 수정
- [ ] Scheduled Job 테스트 (2시간/24시간 자동 종료)
- [ ] 상태 전환 통합 테스트

---

### 11.3 toDto() 매핑 완전성 확보

**문제점**:
- 초기 계획의 toDto() 예시가 너무 단순화됨
- 실제 ConsultationRequestDto는 20+ 필드 보유
- 현재 구현은 거의 모든 필드를 매핑하고 있음 (Line 597-623)

**실제 DTO 필드** (ConsultationRequestDto.java:14-52):
```java
private Long requestIdx;
private String requesterUserCode;
private String requesterName;              // ⚠️ 매핑 필요
private String recipientUserCode;
private String recipientName;              // ⚠️ 매핑 필요
private ConsultationType consultationType;
private String title;
private String content;
private LocalDateTime desiredDate;
private RequestStatus requestStatus;       // → status로 변경
private String acceptMessage;              // ⚠️ 필드 전략에 따라 유지/제거
private String rejectionReason;            // → statusReason으로 통합
private String cancelReason;               // → statusReason으로 통합
private ConsultationStatus consultationStatus; // → 제거
private LocalDateTime scheduledStartAt;
private LocalDateTime startedAt;
private LocalDateTime endedAt;
private Integer durationMinutes;
private LocalDateTime lastActivityAt;
private LocalDateTime createdAt;
private Boolean hasUnreadMessages;         // ⚠️ 현재 매핑 누락
```

**올바른 toDto() 구현 (Option B 적용)**: ✅
```java
private ConsultationRequestDto toDto(ConsultationRequest entity) {
    ConsultationRequestDto dto = new ConsultationRequestDto();

    // 기본 정보
    dto.setRequestIdx(entity.getRequestIdx());
    dto.setRequesterUserCode(entity.getRequesterUserCode());
    dto.setRecipientUserCode(entity.getRecipientUserCode());
    dto.setConsultationType(entity.getConsultationTypeEnum());
    dto.setTitle(entity.getTitle());
    dto.setContent(entity.getContent());
    dto.setDesiredDate(entity.getDesiredDate());
    dto.setScheduledStartAt(entity.getDesiredDate());

    // 상태 정보 (통합됨)
    dto.setStatus(entity.getStatus());
    dto.setStatusReason(entity.getStatusReason());
    // statusReason이 상태별로 다른 의미:
    // - APPROVED → 승인 안내사항
    // - REJECTED → 반려 사유
    // - CANCELLED → 취소 사유

    // 진행 정보
    dto.setStartedAt(entity.getStartedAt());
    dto.setEndedAt(entity.getEndedAt());
    dto.setDurationMinutes(entity.getDurationMinutes());
    dto.setLastActivityAt(entity.getLastActivityAt());
    dto.setCreatedAt(entity.getCreatedAt());

    // 사용자 이름 조회 (기존 로직 유지)
    getUserName(entity.getRequesterUserCode()).ifPresent(dto::setRequesterName);
    getUserName(entity.getRecipientUserCode()).ifPresent(dto::setRecipientName);

    // 읽지 않은 메시지 여부 (TODO: 구현 필요)
    // dto.setHasUnreadMessages(calculateUnreadMessages(entity, userCode));

    return dto;
}
```

**핵심 변경사항**:
- `acceptMessage`, `rejectionReason`, `cancelReason` 필드 제거
- 단일 `statusReason` 필드만 사용
- 상태별로 `statusReason`의 의미가 다름 (주석으로 명시)

**추가 작업**:
- [ ] toDto() 메소드 완전한 매핑 구현
- [ ] hasUnreadMessages 계산 로직 추가
- [ ] DTO 매핑 단위 테스트

---

### 11.4 마이그레이션 SQL 수정

**기존 계획 (Line 376-394)**의 문제점:
- `acceptMessage` 처리 누락
- 상태 매핑 로직이 불완전

**수정된 마이그레이션 SQL**:

**최종 마이그레이션 SQL (Option B 적용)**: ✅
```sql
-- ========================================
-- V2_0__simplify_consultation_api.sql
-- Option B: 단일 statusReason으로 완전 통합
-- ========================================

-- Step 1: 새 컬럼 추가
ALTER TABLE CONSULTATION_REQUEST_TBL
ADD COLUMN status VARCHAR(20) DEFAULT 'PENDING',
ADD COLUMN status_reason VARCHAR(500),
ADD COLUMN status_changed_at TIMESTAMP,
ADD COLUMN status_changed_by VARCHAR(20);

-- Step 2: 상태 마이그레이션 (이중 상태 → 단일 상태)
UPDATE CONSULTATION_REQUEST_TBL
SET status = CASE
    WHEN request_status = 'PENDING' THEN 'PENDING'
    WHEN request_status = 'APPROVED' AND (consultation_status IS NULL OR consultation_status = 'SCHEDULED') THEN 'APPROVED'
    WHEN request_status = 'APPROVED' AND consultation_status = 'IN_PROGRESS' THEN 'IN_PROGRESS'
    WHEN request_status = 'APPROVED' AND consultation_status = 'COMPLETED' THEN 'COMPLETED'
    WHEN request_status = 'REJECTED' THEN 'REJECTED'
    WHEN request_status = 'CANCELLED' THEN 'CANCELLED'
    ELSE 'PENDING'
END;

-- Step 3: 모든 사유/메시지 필드 통합
-- 우선순위: rejection_reason > cancel_reason > accept_message
UPDATE CONSULTATION_REQUEST_TBL
SET status_reason = COALESCE(
    rejection_reason,    -- 거절 사유 (우선)
    cancel_reason,       -- 취소 사유
    accept_message       -- 승인 시 안내사항
);

-- Step 4: 인덱스 생성 (성능 최적화)
CREATE INDEX idx_consultation_status
ON CONSULTATION_REQUEST_TBL(status);

CREATE INDEX idx_consultation_participant
ON CONSULTATION_REQUEST_TBL(requester_user_code, recipient_user_code);

CREATE INDEX idx_consultation_status_created
ON CONSULTATION_REQUEST_TBL(status, created_at);

-- Step 5: NOT NULL 제약조건 추가
ALTER TABLE CONSULTATION_REQUEST_TBL
MODIFY COLUMN status VARCHAR(20) NOT NULL;

-- Step 6: 검증 쿼리 (마이그레이션 결과 확인)
SELECT
    request_status,
    consultation_status,
    status,
    COUNT(*) as count
FROM CONSULTATION_REQUEST_TBL
GROUP BY request_status, consultation_status, status
ORDER BY request_status, consultation_status;

-- Step 7: 기존 컬럼 제거 (리팩토링 완료 후 실행)
-- ⚠️ 주의: 충분한 테스트 및 프론트엔드 마이그레이션 후 실행!
-- ALTER TABLE CONSULTATION_REQUEST_TBL
-- DROP COLUMN request_status,
-- DROP COLUMN consultation_status,
-- DROP COLUMN memo,
-- DROP COLUMN accept_message,
-- DROP COLUMN rejection_reason,
-- DROP COLUMN cancel_reason;
```

**핵심 포인트**:
- `accept_message`, `rejection_reason`, `cancel_reason` 모두 `status_reason`으로 통합
- COALESCE 우선순위: rejection > cancel > accept (거절/취소 사유가 승인 메시지보다 중요)
- 기존 필드는 충분한 검증 후 제거 (5주차)

---

### 11.5 업데이트된 작업 체크리스트

**Phase 0: 의존성 분석 완료** ✅
- [x] ChatNotificationService 분석
- [x] Scheduled Jobs 분석
- [x] Repository 쿼리 분석
- [x] DTO 매핑 분석

**Phase 1: 필드 전략 결정** ✅ 완료
- [x] Option B 선택 (단일 statusReason 통합)
- [x] ChatNotificationService 마이그레이션 전략 확정
- [x] 마이그레이션 SQL 최종 확정

**Phase 2: DTO 레이어** (Option B 적용)
- [ ] ViewType, ListRequest, StatusRequest, DetailRequest 생성
- [ ] ConsultationRequestDto 수정
  - [ ] `requestStatus`, `consultationStatus` 제거
  - [ ] `acceptMessage`, `rejectionReason`, `cancelReason` 제거
  - [ ] `status`, `statusReason` 추가
- [ ] toDto() 완전한 매핑 구현 (acceptMessage 필드 제거)

**Phase 3: Entity 레이어** (Option B 적용)
- [ ] 새 필드 추가 (status, statusReason, statusChangedAt, statusChangedBy)
- [ ] 기존 필드 @Deprecated 표시
  - [ ] requestStatus, consultationStatus
  - [ ] acceptMessage, rejectionReason, cancelReason (모두 제거 예정)
  - [ ] memo

**Phase 4: Validator 레이어** (기존 계획)
- [ ] StatusTransitionValidator 생성
- [ ] 단위 테스트

**Phase 5: Repository 레이어** (기존 계획 + 보완)
- [ ] 새 쿼리 메서드 추가
- [ ] **5개 기존 쿼리 수정** (consultationStatus → status)
  - [ ] findActiveConsultations
  - [ ] findInactiveConsultations
  - [ ] findLongRunningConsultations
  - [ ] findCompletedConsultations
  - [ ] findConsultationsWithUnreadMessages
- [ ] 쿼리 테스트

**Phase 6: Service 레이어** (기존 계획 + 보완)
- [ ] 새 메서드 구현 (getConsultationList, changeConsultationStatus 등)
- [ ] **Scheduled Jobs 수정**
  - [ ] autoEndInactiveConsultations()
  - [ ] autoEndLongRunningConsultations()
  - [ ] completeConsultation()
- [ ] 단위 테스트 + Scheduled Job 테스트

**Phase 7: Notification 레이어** ⚠️ 추가됨
- [ ] ChatNotificationService 수정 (acceptMessage/rejectionReason/cancelReason 처리)
- [ ] FCM push 페이로드 검증
- [ ] Notification 통합 테스트

**Phase 8: Controller 레이어** (기존 계획)
- [ ] 새 엔드포인트 추가
- [ ] 통합 테스트

**Phase 9: 데이터 마이그레이션** (기존 계획)
- [ ] 마이그레이션 스크립트 실행
- [ ] 데이터 검증

**Phase 10: 레거시 제거** (기존 계획)
- [ ] Deprecated 코드 제거

---

### 11.6 예상 추가 작업 시간

| 작업 | 기존 계획 | 추가 작업 | 합계 |
|------|----------|----------|------|
| ChatNotificationService 마이그레이션 | 0시간 | **4시간** | 4시간 |
| Repository 쿼리 5개 수정 | 포함 | **2시간** | 2시간 |
| Scheduled Jobs 수정 | 0시간 | **3시간** | 3시간 |
| toDto() 완전 매핑 | 포함 | **1시간** | 1시간 |
| Notification 통합 테스트 | 0시간 | **2시간** | 2시간 |
| **총 추가 작업** | - | **12시간** | 12시간 |

**수정된 전체 일정**: 기존 3주 → **4주 권장**

---

### 11.7 최종 결정: Option B 채택 ✅

**결정 사항**: 단일 `statusReason` 필드로 완전 통합

**이유**:
1. **일관성**: 모든 상태 변경이 동일한 패턴 (`status` + `statusReason`)
2. **단순성**: Entity 필드 최소화 (acceptMessage, rejectionReason, cancelReason → statusReason)
3. **유연성**: `statusReason` 용도를 상태별로 다르게 해석
   - `APPROVED` → 승인 안내사항 (선택)
   - `REJECTED` → 반려 사유 (필수)
   - `CANCELLED` → 취소 사유 (필수)
4. **포트폴리오 적합성**: 깔끔한 구조, 이해하기 쉬운 설계

**구현 전략**:
1. **Entity 필드**: `status`, `statusReason`, `statusChangedAt`, `statusChangedBy` 4개만
2. **ChatNotificationService**: 상태별로 `statusReason`을 다른 키로 매핑
3. **검증 로직**: `StatusTransitionValidator.requiresReason()` 활용

---

## 12. 2026-02-14 구현 현황 스냅샷

> ✅ Spring 백엔드 1차 리팩토링 적용 완료 (엔드포인트·서비스·알림 레이어 연동)

### 12.1 코드 변경 요약
- **엔티티**: `backend/BlueCrab/src/main/java/BlueCrab/com/example/entity/ConsultationRequest.java`에 통합 상태 필드(`status`, `statusReason`, `statusChangedAt`, `statusChangedBy`)를 도입하고, 레거시 필드와 양방향 동기화 로직을 추가. 상태 변경 시 적절한 메시지 필드가 자동 세팅되도록 보강.
- **DTO**: `ConsultationRequestDto`를 단일 상태 구조로 재작성하고, 신규 요청 DTO 4종(`ConsultationListRequest`, `ConsultationStatusRequest`, `ConsultationDetailRequest`, `ViewType`)을 생성하여 `/list`, `/status`, `/detail` 엔드포인트에서 사용.
- **Repository**: `ConsultationRequestRepository`가 `status` 기반 조회를 수행하도록 수정하고, 통합 목록/이력 조회용 JPQL(`findByStatusAndParticipant`, `findByStatusInAndParticipant*`)을 추가.
- **Service**: `ConsultationRequestServiceImpl`에 `getConsultationList`, `createConsultation`, `changeConsultationStatus` 등 신규 API 로직을 구현하고, `StatusTransitionValidator`(새 헬퍼 클래스)를 활용해 상태 전환을 검증. 자동 종료 시 `statusChangedBy="SYSTEM_AUTO"` 등 메타데이터 기록.
- **Controller**: `ConsultationController`에 `/api/consultation/list|detail|create|status` 엔드포인트를 추가하고, 기존 인증 헬퍼(`validateAuth`)로 권한을 일원화.
- **알림**: `ChatNotificationService`가 `statusReason`을 우선 사용하도록 변경하여 승인/반려/취소 알림 메시지 출처를 통합.

### 12.2 DB 마이그레이션 안내
- **필수 작업**: `CONSULTATION_REQUEST_TBL`에 아래 컬럼을 추가하고 인덱스를 재구성.
  - `status` (VARCHAR(20) NOT NULL DEFAULT 'PENDING')
  - `status_reason` (VARCHAR(500)), `status_changed_at` (TIMESTAMP), `status_changed_by` (VARCHAR(20))
  - 인덱스: `idx_consultation_status`, `idx_consultation_participant`, `idx_consultation_status_created`
- **데이터 이관**: `status` 값은 `request_status/consultation_status` 조합을 기준으로 매핑, `status_reason`은 `COALESCE(rejection_reason, cancel_reason, accept_message)` 순으로 통합.
- **레거시 컬럼 제거**: 충분한 검증 후 `request_status`, `consultation_status`, `accept_message`, `rejection_reason`, `cancel_reason`, `memo`를 드랍 (현재 코드는 @Deprecated 필드로 후방 호환 유지).

### 12.3 남은 TODO
1. `mvn` 빌드 환경 구성 및 컴파일 검증 (현재 CLI에 Maven 미설치).
2. 마이그레이션 SQL 실행 → 데이터 검증 → 통합 테스트(E2E/스케줄러/알림) 재확인.
3. 프론트엔드에서 신규 API(`/list`, `/status` 등)로 전환 후 레거시 엔드포인트 단계적 폐기.
4. Deprecated DTO/Service 메서드 제거 시점 조율 (프론트 전환 완료 후 정리).

> 참고: 위 작업은 `docs/consultation/frontend-consultation-integration-legacy.md`와 동기화 계획이 필요함.

---

**문서 끝**

> 💡 **피드백**: 이 계획에 대한 의견은 백엔드 팀 미팅에서 논의합니다.
> 📧 **문의**: backend@bluecrab.com
> ⚠️ **업데이트**: 2025-10-28 - 의존성 분석 기반 보완 사항 추가
