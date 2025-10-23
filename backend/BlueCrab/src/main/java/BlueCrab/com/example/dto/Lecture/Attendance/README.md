# 출석 관리 DTO 패키지

출석 요청/승인 시스템을 위한 DTO 클래스들

## 📦 패키지 구조

```
BlueCrab.com.example.dto.Lecture.attendance/
├── AttendanceRequestRequestDto.java      - 출석 요청 Request
├── AttendanceApproveRequestDto.java      - 출석 승인 Request
├── AttendanceApprovalRecordDto.java      - 출석 승인 레코드 (배열 요소)
├── AttendanceResponseDto.java            - 공통 Response (제네릭)
├── AttendanceDataDto.java                - 전체 출석 데이터
├── AttendanceSessionDto.java             - 확정된 출석 세션
├── AttendancePendingRequestDto.java      - 대기 중인 요청
├── AttendanceSummaryDto.java             - 출석 요약 통계
└── StudentAttendanceDto.java             - 학생 출석 정보 (교수용)
```

## 📋 DTO 클래스 상세

### 1. Request DTO

#### `AttendanceRequestRequestDto`
- **용도**: 학생의 출석 인정 요청
- **API**: `POST /api/attendance/request`
- **필드**:
  - `lecSerial` (String, 필수): 강의 코드
  - `sessionNumber` (Integer, 필수, 1~80): 회차 번호
  - `requestReason` (String, 선택): 요청 사유
- **Validation**: `@NotNull`, `@Min(1)`, `@Max(80)`

#### `AttendanceApproveRequestDto`
- **용도**: 교수의 출석 승인/반려
- **API**: `PUT /api/attendance/approve`
- **필드**:
  - `lecSerial` (String, 필수): 강의 코드
  - `sessionNumber` (Integer, 필수, 1~80): 회차 번호
  - `attendanceRecords` (List, 필수): 승인 레코드 배열
- **Validation**: `@NotNull`, `@NotEmpty`, `@Valid`

#### `AttendanceApprovalRecordDto`
- **용도**: 승인 요청의 배열 요소
- **필드**:
  - `studentIdx` (Integer, 필수): 학생 USER_IDX
  - `status` (String, 필수, 출/지/결): 출석 상태
  - `rejectReason` (String, 선택): 반려 사유
- **Validation**: `@NotNull`, `@Pattern(regexp = "^(출|지|결)$")`

### 2. Response DTO

#### `AttendanceResponseDto<T>`
- **용도**: 모든 출석 API의 공통 응답 형식
- **필드**:
  - `success` (Boolean): 성공 여부
  - `message` (String): 응답 메시지
  - `data` (T, 선택): 응답 데이터
- **Factory Methods**:
  - `AttendanceResponseDto.success(message)`
  - `AttendanceResponseDto.success(message, data)`
  - `AttendanceResponseDto.error(message)`
  - `AttendanceResponseDto.error(message, data)`

### 3. 데이터 구조 DTO

#### `AttendanceDataDto`
- **용도**: ENROLLMENT_DATA.attendance 전체 구조
- **필드**:
  - `summary` (AttendanceSummaryDto): 출석 요약
  - `sessions` (List<AttendanceSessionDto>): 확정된 출석 기록
  - `pendingRequests` (List<AttendancePendingRequestDto>): 대기 중인 요청

#### `AttendanceSessionDto`
- **용도**: 확정된 출석 기록 (sessions 배열 요소)
- **필드**:
  - `sessionNumber` (Integer): 회차 번호
  - `status` (String): 출석 상태 (출/지/결)
  - `requestDate` (LocalDateTime): 요청 일시
  - `approvedDate` (LocalDateTime): 승인 일시
  - `approvedBy` (Integer): 승인자 USER_IDX
  - `tempApproved` (Boolean): 자동승인 여부

#### `AttendancePendingRequestDto`
- **용도**: 대기 중인 출석 요청 (pendingRequests 배열 요소)
- **필드**:
  - `sessionNumber` (Integer): 회차 번호
  - `requestDate` (LocalDateTime): 요청 일시
  - `expiresAt` (LocalDateTime): 만료 일시 (7일 후)
  - `tempApproved` (Boolean): 자동승인 예약 여부

#### `AttendanceSummaryDto`
- **용도**: 출석 통계 (summary 객체)
- **필드**:
  - `attended` (Integer): 출석 횟수
  - `late` (Integer): 지각 횟수
  - `absent` (Integer): 결석 횟수
  - `totalSessions` (Integer): 총 회차 수
  - `attendanceRate` (Double): 출석률 (0.0~100.0)
  - `updatedAt` (LocalDateTime): 최종 갱신 일시

#### `StudentAttendanceDto`
- **용도**: 교수용 학생별 출석 조회 응답
- **API**: `GET /api/attendance/professor/{lecSerial}`
- **필드**:
  - `studentIdx` (Integer): 학생 USER_IDX
  - `studentCode` (String): 학번
  - `studentName` (String): 학생 이름
  - `attendanceData` (AttendanceDataDto): 출석 데이터

## 🔄 DTO 사용 흐름

### 학생 출석 요청
```
AttendanceRequestRequestDto
  → Service Layer (JSON 업데이트)
  → AttendanceResponseDto<AttendanceDataDto>
```

### 교수 출석 승인
```
AttendanceApproveRequestDto
  (attendanceRecords: List<AttendanceApprovalRecordDto>)
  → Service Layer (JSON 업데이트)
  → AttendanceResponseDto<Void>
```

### 학생 출석 조회
```
GET Request
  → Service Layer (JSON 파싱)
  → AttendanceResponseDto<AttendanceDataDto>
```

### 교수 출석 조회
```
GET Request
  → Service Layer (JSON 파싱 + 학생 정보 조인)
  → AttendanceResponseDto<List<StudentAttendanceDto>>
```

## ⚠️ 주의사항

1. **날짜/시간 포맷**: `@JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")`
2. **Validation**: javax.validation 어노테이션 사용
3. **출석 상태**: 반드시 "출", "지", "결" 중 하나 (한글)
4. **회차 번호**: 1~80 범위 제한
5. **자동승인**: 7일 경과 시 `tempApproved = true`로 자동 승인

## 📝 작성자

- Phase 1-2: DTO 클래스 생성
- 작성일: 2025-10-23
- 연관 문서: `전체가이드/데이터구조/ENROLLMENT_DATA_JSON_명세서.md`
