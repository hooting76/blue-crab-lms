# Phase 1-2: DTO 클래스 생성

## 📋 작업 개요

**목표**: 출석 요청/승인 시스템을 위한 DTO 클래스 생성  
**소요 시간**: 1시간  
**상태**: ✅ 완료

---

## 🎯 작업 내용

### 1. 패키지 구조 설계

**패키지 경로**: `BlueCrab.com.example.dto.Lecture.attendance`

```
dto/Lecture/attendance/
├── AttendanceRequestRequestDto.java       - 출석 요청 Request
├── AttendanceApproveRequestDto.java       - 출석 승인 Request
├── AttendanceApprovalRecordDto.java       - 승인 레코드 (배열 요소)
├── AttendanceResponseDto.java             - 공통 Response (제네릭)
├── AttendanceDataDto.java                 - 전체 출석 데이터
├── AttendanceSessionDto.java              - 확정 출석 세션
├── AttendancePendingRequestDto.java       - 대기 요청
├── AttendanceSummaryDto.java              - 출석 요약 통계
├── StudentAttendanceDto.java              - 학생 출석 정보 (교수용)
└── README.md                              - 패키지 문서
```

---

## 📦 생성된 DTO 클래스

### 1. Request DTO

#### `AttendanceRequestRequestDto`
**용도**: 학생의 출석 인정 요청  
**API**: `POST /api/attendance/request`

```java
public class AttendanceRequestRequestDto {
    @NotNull(message = "강의 코드는 필수입니다")
    private String lecSerial;           // 강의 코드 (LEC_SERIAL)
    
    @NotNull(message = "회차 번호는 필수입니다")
    @Min(value = 1, message = "회차 번호는 1 이상이어야 합니다")
    @Max(value = 80, message = "회차 번호는 80 이하여야 합니다")
    private Integer sessionNumber;      // 회차 번호 (1~80)
    
    private String requestReason;       // 요청 사유 (선택)
}
```

#### `AttendanceApproveRequestDto`
**용도**: 교수의 출석 승인/반려  
**API**: `PUT /api/attendance/approve`

```java
public class AttendanceApproveRequestDto {
    @NotNull(message = "강의 코드는 필수입니다")
    private String lecSerial;           // 강의 코드
    
    @NotNull(message = "회차 번호는 필수입니다")
    @Min(value = 1)
    @Max(value = 80)
    private Integer sessionNumber;      // 회차 번호
    
    @NotEmpty(message = "출석 승인 레코드는 최소 1개 이상이어야 합니다")
    @Valid
    private List<AttendanceApprovalRecordDto> attendanceRecords;
}
```

#### `AttendanceApprovalRecordDto`
**용도**: 승인 요청의 배열 요소

```java
public class AttendanceApprovalRecordDto {
    @NotNull(message = "학생 USER_IDX는 필수입니다")
    private Integer studentIdx;         // 학생 USER_IDX
    
    @NotNull(message = "출석 상태는 필수입니다")
    @Pattern(regexp = "^(출|지|결)$", message = "출석 상태는 '출', '지', '결' 중 하나여야 합니다")
    private String status;              // 출석 상태 (출/지/결)
    
    private String rejectReason;        // 반려 사유 (선택)
}
```

### 2. Response DTO

#### `AttendanceResponseDto<T>`
**용도**: 모든 출석 API의 공통 응답 형식

```java
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AttendanceResponseDto<T> {
    private Boolean success;            // 성공 여부
    private String message;             // 응답 메시지
    private T data;                     // 응답 데이터 (선택)
    
    // Factory Methods
    public static <T> AttendanceResponseDto<T> success(String message);
    public static <T> AttendanceResponseDto<T> success(String message, T data);
    public static <T> AttendanceResponseDto<T> error(String message);
    public static <T> AttendanceResponseDto<T> error(String message, T data);
}
```

### 3. 데이터 구조 DTO

#### `AttendanceDataDto`
**용도**: ENROLLMENT_DATA.attendance 전체 구조

```java
public class AttendanceDataDto {
    private AttendanceSummaryDto summary;                   // 출석 요약
    private List<AttendanceSessionDto> sessions;            // 확정 출석 (최대 80)
    private List<AttendancePendingRequestDto> pendingRequests;  // 대기 요청 (최대 80)
}
```

#### `AttendanceSessionDto`
**용도**: 확정된 출석 기록 (sessions 배열 요소)

```java
public class AttendanceSessionDto {
    private Integer sessionNumber;                          // 회차 번호
    private String status;                                  // 출석 상태 (출/지/결)
    
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime requestDate;                      // 요청 일시
    
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime approvedDate;                     // 승인 일시
    
    private Integer approvedBy;                             // 승인자 USER_IDX
    private Boolean tempApproved;                           // 자동승인 여부
}
```

#### `AttendancePendingRequestDto`
**용도**: 대기 중인 출석 요청 (pendingRequests 배열 요소)

```java
public class AttendancePendingRequestDto {
    private Integer sessionNumber;                          // 회차 번호
    
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime requestDate;                      // 요청 일시
    
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime expiresAt;                        // 만료 일시 (7일 후)
    
    private Boolean tempApproved;                           // 자동승인 예약
}
```

#### `AttendanceSummaryDto`
**용도**: 출석 통계 (summary 객체)

```java
public class AttendanceSummaryDto {
    private Integer attended;                               // 출석 횟수
    private Integer late;                                   // 지각 횟수
    private Integer absent;                                 // 결석 횟수
    private Integer totalSessions;                          // 총 회차
    private Double attendanceRate;                          // 출석률 (0.0~100.0)
    
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updatedAt;                        // 최종 갱신 일시
}
```

#### `StudentAttendanceDto`
**용도**: 교수용 학생별 출석 조회 응답

```java
public class StudentAttendanceDto {
    private Integer studentIdx;                             // 학생 USER_IDX
    private String studentCode;                             // 학번
    private String studentName;                             // 학생 이름
    private AttendanceDataDto attendanceData;               // 출석 데이터
}
```

---

## 🔍 주요 특징

### 1. Validation 적용
- `@NotNull`: 필수 필드 검증
- `@Min`, `@Max`: 회차 번호 범위 (1~80)
- `@Pattern`: 출석 상태 제한 ("출", "지", "결")
- `@NotEmpty`: 배열 최소 1개 이상
- `@Valid`: 중첩 객체 검증

### 2. JSON 포맷 설정
- `@JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")` - 날짜/시간 포맷
- `@JsonInclude(Include.NON_NULL)` - null 필드 제외

### 3. Factory Methods
```java
// 성공 응답
AttendanceResponseDto.success("출석 요청이 완료되었습니다.");
AttendanceResponseDto.success("조회 성공", attendanceData);

// 실패 응답
AttendanceResponseDto.error("이미 출석 요청이 존재합니다.");
```

---

## 📋 체크리스트

### Request DTO
- [x] `AttendanceRequestRequestDto` 생성
- [x] `AttendanceApproveRequestDto` 생성
- [x] `AttendanceApprovalRecordDto` 생성
- [x] Validation 어노테이션 적용

### Response DTO
- [x] `AttendanceResponseDto<T>` 생성 (제네릭)
- [x] Factory Methods 구현
- [x] `@JsonInclude` 적용

### 데이터 구조 DTO
- [x] `AttendanceDataDto` 생성
- [x] `AttendanceSessionDto` 생성
- [x] `AttendancePendingRequestDto` 생성
- [x] `AttendanceSummaryDto` 생성
- [x] `StudentAttendanceDto` 생성
- [x] `@JsonFormat` 적용

### 문서화
- [x] 패키지 README.md 작성
- [x] 각 DTO 클래스 JavaDoc 주석

---

## 🎯 다음 단계

**Phase 2-1: Repository 계층 구현**
- `EnrollmentExtendedRepository` 메서드 추가
- lecSerial 기반 조회 쿼리
- 교수 권한 검증용 메서드

---

## 📝 산출물

- ✅ DTO 클래스 9개
- ✅ Validation 어노테이션 적용
- ✅ JSON 직렬화/역직렬화 설정
- ✅ Factory Methods 구현
- ✅ 패키지 README.md 작성

---

## 📚 참고 문서

- [출석요청승인_플로우.md](../../출석요청승인_플로우.md)
- [ENROLLMENT_DATA_JSON_명세서.md](../../../전체가이드/데이터구조/ENROLLMENT_DATA_JSON_명세서.md)
- DTO 패키지: `dto/Lecture/attendance/README.md`
