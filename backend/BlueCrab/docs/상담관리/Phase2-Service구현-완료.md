# Phase 2: Service Layer 구현 완료 보고서

**작업일**: 2025-10-24
**담당자**: BlueCrab Development Team
**상태**: ✅ 완료

---

## 📋 작업 개요

상담 요청/진행 관리 시스템의 Service Layer를 구현했습니다.

### 주요 산출물

1. **ConsultationRequestService** 인터페이스
2. **ConsultationRequestServiceImpl** 구현 클래스
3. **UserTblRepository** 확장 (existsByUserCode 메서드 추가)
4. **DTO 보완** (ConsultationRequestCreateDto, ConsultationHistoryRequestDto)

---

## 🎯 구현 완료 항목

### 1. Service 인터페이스 설계

**파일**: `ConsultationRequestService.java`

#### 핵심 메서드 (15개)

| 메서드 | 설명 | 권한 |
|--------|------|------|
| `createRequest()` | 상담 요청 생성 | 학생 |
| `approveRequest()` | 상담 요청 승인 | 교수 |
| `rejectRequest()` | 상담 요청 반려 | 교수 |
| `cancelRequest()` | 상담 요청 취소 | 학생 |
| `startConsultation()` | 상담 시작 | 학생/교수 |
| `endConsultation()` | 상담 종료 | 학생/교수 |
| `updateMemo()` | 메모 작성/수정 | 교수 |
| `getMyRequests()` | 내 요청 목록 조회 | 학생 |
| `getReceivedRequests()` | 받은 요청 목록 조회 | 교수 |
| `getActiveConsultations()` | 진행 중인 상담 조회 | 학생/교수 |
| `getConsultationHistory()` | 완료된 상담 이력 조회 | 학생/교수 |
| `getConsultationDetail()` | 상담 상세 조회 | 학생/교수 |
| `getUnreadRequestCount()` | 읽지 않은 요청 개수 | 교수 |
| `updateReadTime()` | 읽음 시간 업데이트 | 학생/교수 |
| `autoEndInactiveConsultations()` | 비활성 상담 자동 종료 | 스케줄러 |
| `autoEndLongRunningConsultations()` | 장시간 실행 상담 자동 종료 | 스케줄러 |

---

### 2. Service 구현 클래스

**파일**: `ConsultationRequestServiceImpl.java` (580줄)

#### 주요 기능

**상담 요청 관리**
- ✅ 요청 생성 시 사용자 존재 검증
- ✅ 승인/반려/취소 상태 검증
- ✅ 상태 변경 시 로깅

**상담 진행 관리**
- ✅ 상담 시작/종료 처리
- ✅ 상담 시간 자동 계산 (분 단위)
- ✅ 마지막 활동 시간 추적

**조회 기능**
- ✅ 페이징 처리
- ✅ 상태별 필터링
- ✅ 권한 검증 (참여자만 조회 가능)
- ✅ 사용자 이름 자동 조회

**자동화 기능**
- ✅ 2시간 비활성 상담 자동 종료
- ✅ 24시간 장시간 실행 상담 자동 종료
- ✅ 종료 시 duration 자동 계산

**보안 기능**
- ✅ 사용자 존재 검증
- ✅ 참여자 권한 검증
- ✅ 상태 변경 가능 여부 검증

---

### 3. DTO 보완

#### ConsultationRequestCreateDto
```java
// 추가된 필드
private String requesterUserCode;  // 요청자 USER_CODE (Controller에서 JWT로 주입)

// Getter/Setter 추가
public String getRequesterUserCode() { ... }
public void setRequesterUserCode(String requesterUserCode) { ... }
```

#### ConsultationHistoryRequestDto
```java
// 추가된 필드
private String userCode;  // 조회자 USER_CODE

// Getter/Setter 추가
public String getUserCode() { ... }
public void setUserCode(String userCode) { ... }
```

---

### 4. Repository 확장

**파일**: `UserTblRepository.java`

#### 추가된 메서드
```java
/**
 * USER_CODE의 존재 여부 확인
 * 상담 요청 생성 시 사용자 검증에 사용
 */
boolean existsByUserCode(String userCode);
```

**사용 목적**:
- 상담 요청 생성 시 requesterUserCode, recipientUserCode 검증
- 존재하지 않는 사용자에게 요청 방지

---

## 📊 코드 통계

| 항목 | 값 |
|------|-----|
| 인터페이스 메서드 수 | 15개 |
| 구현 클래스 라인 수 | 580줄 |
| Public 메서드 수 | 15개 |
| Private Helper 메서드 수 | 4개 |
| Repository 메서드 호출 | 17개 |
| 트랜잭션 처리 | @Transactional |
| 로깅 레벨 | INFO, WARN, ERROR |

---

## 🔧 기술 스택

- **Framework**: Spring Boot
- **Persistence**: JPA (Hibernate)
- **Transaction**: Spring @Transactional
- **Logging**: SLF4J + Lombok @Slf4j
- **Data Mapping**: Entity ↔ DTO 수동 변환
- **Validation**: Method 파라미터 검증
- **Exception Handling**: RuntimeException 기반

---

## 💡 구현 특징

### 1. 상태 관리 패턴
```java
// RequestStatus: PENDING → APPROVED/REJECTED/CANCELLED
// ConsultationStatus: SCHEDULED → IN_PROGRESS → COMPLETED/CANCELLED

// 상태 검증 예시
if (!RequestStatus.PENDING.equals(consultation.getRequestStatusEnum())) {
    throw new IllegalStateException("대기 중인 요청만 승인할 수 있습니다.");
}
```

### 2. Entity ↔ DTO 변환
```java
private ConsultationRequestDto toDto(ConsultationRequest entity) {
    ConsultationRequestDto dto = new ConsultationRequestDto();
    // 필드 매핑
    dto.setRequestIdx(entity.getRequestIdx());
    dto.setConsultationType(entity.getConsultationTypeEnum());
    // ...

    // 사용자 이름 자동 조회
    getUserName(entity.getRequesterUserCode()).ifPresent(dto::setRequesterName);
    getUserName(entity.getRecipientUserCode()).ifPresent(dto::setRecipientName);

    return dto;
}
```

### 3. Duration 계산
```java
// 상담 종료 시 자동 시간 계산
LocalDateTime now = LocalDateTime.now();
consultation.setEndedAt(now);

if (consultation.getStartedAt() != null) {
    Duration duration = Duration.between(consultation.getStartedAt(), now);
    consultation.setDurationMinutes((int) duration.toMinutes());
}
```

### 4. 읽음 처리 로직
```java
// 요청자(학생) vs 수신자(교수) 구분
if (consultation.getRequesterUserCode().equals(userCode)) {
    consultation.setLastReadTimeStudent(LocalDateTime.now());
} else if (consultation.getRecipientUserCode().equals(userCode)) {
    consultation.setLastReadTimeProfessor(LocalDateTime.now());
}
```

---

## 🧪 테스트 시나리오

### 단위 테스트 (예정)

1. **상담 요청 생성**
   - ✅ 정상 생성
   - ⚠️ 존재하지 않는 사용자
   - ⚠️ 필수 필드 누락

2. **요청 승인/반려**
   - ✅ PENDING 상태에서 승인
   - ⚠️ 이미 처리된 요청 승인 시도
   - ⚠️ 존재하지 않는 요청

3. **상담 진행**
   - ✅ SCHEDULED → IN_PROGRESS → COMPLETED
   - ⚠️ 잘못된 상태에서 시작/종료 시도
   - ✅ Duration 자동 계산

4. **권한 검증**
   - ✅ 참여자만 조회 가능
   - ⚠️ 제3자 조회 시도
   - ✅ 읽음 시간 업데이트

5. **자동 종료**
   - ✅ 2시간 비활성 상담 종료
   - ✅ 24시간 장시간 실행 상담 종료

---

## 📝 다음 단계: Phase 3

### Controller Layer 구현

**예상 작업 내용**:

1. **ConsultationController 생성**
   - REST API 엔드포인트 15개
   - JWT 인증 통합
   - 권한 검증 (학생/교수 구분)

2. **API 엔드포인트 설계**
   ```
   POST   /api/consultation/request          - 상담 요청
   POST   /api/consultation/approve          - 요청 승인
   POST   /api/consultation/reject           - 요청 반려
   POST   /api/consultation/cancel           - 요청 취소
   POST   /api/consultation/start            - 상담 시작
   POST   /api/consultation/end              - 상담 종료
   POST   /api/consultation/memo             - 메모 작성
   POST   /api/consultation/my-requests      - 내 요청 목록
   POST   /api/consultation/received         - 받은 요청 목록
   POST   /api/consultation/active           - 진행 중인 상담
   POST   /api/consultation/history          - 완료된 이력
   GET    /api/consultation/{id}             - 상담 상세
   GET    /api/consultation/unread-count     - 읽지 않은 개수
   POST   /api/consultation/read             - 읽음 처리
   ```

3. **Security 설정**
   - `/api/consultation/request` - authenticated (학생)
   - `/api/consultation/approve` - PROFESSOR, ADMIN
   - `/api/consultation/reject` - PROFESSOR, ADMIN
   - `/api/consultation/memo` - PROFESSOR, ADMIN

4. **응답 포맷 통일**
   - 성공: `ConsultationRequestDto` 반환
   - 에러: `ErrorResponse` 반환
   - 페이징: `Page<ConsultationRequestDto>` 반환

**예상 소요 시간**: 2-3시간

---

## 🔍 검증 완료 사항

### ✅ 컴파일 에러 해결
- [x] ConsultationRequestCreateDto.getRequesterUserCode() 추가
- [x] ConsultationHistoryRequestDto.getUserCode() 추가
- [x] UserTblRepository.existsByUserCode() 추가
- [x] @Autowired 제거 (생성자 주입으로 충분)

### ✅ 코드 품질
- [x] 모든 public 메서드 JavaDoc 주석
- [x] 로깅 처리 (INFO, WARN, ERROR)
- [x] 예외 처리 및 메시지
- [x] 트랜잭션 관리

### ✅ 비즈니스 로직
- [x] 상태 전이 검증
- [x] 권한 검증
- [x] 사용자 존재 확인
- [x] Duration 자동 계산

---

## 📌 참고 사항

### Enum 값 변환
- DB: `String` 형태 저장 (예: "PENDING", "IN_PROGRESS")
- Java: `Enum` 타입 사용
- 변환: `getRequestStatusEnum()`, `setRequestStatusEnum()`

### 읽음 처리
- 학생: `lastReadTimeStudent`
- 교수: `lastReadTimeProfessor`
- 읽지 않은 메시지 확인: `lastReadTime < lastActivityAt`

### 자동 종료 조건
- 비활성: 2시간 동안 `lastActivityAt` 업데이트 없음
- 장시간: 24시간 이상 `IN_PROGRESS` 상태

---

## 📞 문의

**작성자**: BlueCrab Development Team
**작성일**: 2025-10-24
**버전**: 1.0.0
