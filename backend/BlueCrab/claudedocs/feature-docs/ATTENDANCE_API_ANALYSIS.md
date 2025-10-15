# 📋 출석 관리 API 구조 분석 보고서
**작성일:** 2025-10-15  
**Phase:** 6.8.2 - 출석 API 구조 점검

---

## 🔍 1. 현재 구조 분석

### 1.1 API 엔드포인트
출석 기능은 독립된 Controller가 없고 `EnrollmentController`에 통합되어 있습니다.

| HTTP Method | Endpoint | 설명 | 현재 응답 구조 |
|------------|----------|------|---------------|
| `PUT` | `/api/enrollments/{id}/attendance` | 출석 정보 업데이트 (간단) | ❌ Entity 직접 반환 |
| `GET` | `/api/enrollments?lecIdx={id}` | 수강생 목록 (출석 포함) | ✅ ApiResponse + DTO |
| `GET` | `/api/enrollments/{id}` | 특정 수강생 상세 (출석 포함) | ⚠️ 확인 필요 |

### 1.2 Service 메서드

#### `EnrollmentService.java`

```java
// 간단 버전 - attended, absent, late 횟수만 업데이트
public EnrollmentExtendedTbl updateAttendance(Integer enrollmentIdx, 
                                               Integer attended, 
                                               Integer absent, 
                                               Integer late)

// 상세 버전 - 날짜별 출석 기록 배열 저장
public EnrollmentExtendedTbl updateAttendanceData(Integer enrollmentIdx, 
                                                   List<Map<String, Object>> attendanceData)
```

**특징:**
- `enrollmentData` (JSON 컬럼)에 출석 정보 저장
- 두 가지 업데이트 방식 제공:
  - **간단 버전:** 통계만 저장 (출석/결석/지각 횟수)
  - **상세 버전:** 날짜별 기록 저장 (현재 Controller에 매핑 없음)

### 1.3 DTO 구조

#### `AttendanceDto.java` (위치: `dto/Lecture/`)

```java
public class AttendanceDto {
    private String date;                // 출결 날짜 (YYYY-MM-DD)
    private String status;              // 출결 상태 (PRESENT, LATE, ABSENT, EXCUSED)
    private String requestReason;       // 신청 사유
    private String approvalStatus;      // 승인 상태 (PENDING, APPROVED, REJECTED)
    private Integer approvedBy;         // 승인자 IDX
    private String approvedAt;          // 승인 일시
}
```

#### `EnrollmentDto.java`의 출석 관련 필드

```java
public class EnrollmentDto {
    // ...
    private List<AttendanceDto> attendanceRecords;  // 출석 기록 배열
    private Integer attended;                       // 총 출석 횟수
    private Integer absent;                         // 총 결석 횟수
    private Integer late;                           // 총 지각 횟수
    // ...
}
```

---

## ❌ 2. 문제점 발견

### 2.1 **출석 업데이트 API가 ApiResponse 래퍼 미사용**

**파일:** `EnrollmentController.java:231`

```java
@PutMapping("/{enrollmentIdx}/attendance")
public ResponseEntity<?> updateAttendance(...) {
    // ...
    EnrollmentExtendedTbl updated = enrollmentService.updateAttendance(...);
    return ResponseEntity.ok(updated);  // ❌ Entity 직접 반환
}
```

**문제:**
- `ApiResponse` 래퍼 없이 `EnrollmentExtendedTbl` 엔티티를 직접 반환
- Phase 6.8 DTO 패턴과 불일치
- `success`, `message`, `timestamp` 필드 없음
- JSON 구조가 다른 API들과 다름

### 2.2 **성적 업데이트 API도 동일한 문제**

**파일:** `EnrollmentController.java:253`

```java
@PutMapping("/{enrollmentIdx}/grade")
public ResponseEntity<?> updateGrade(...) {
    // ...
    EnrollmentExtendedTbl updated = enrollmentService.updateGrade(...);
    return ResponseEntity.ok(updated);  // ❌ Entity 직접 반환
}
```

### 2.3 **상세 출석 기록 API 미구현**

`EnrollmentService.updateAttendanceData()` 메서드는 존재하지만:
- Controller에 매핑된 엔드포인트가 없음
- 날짜별 상세 출석 기록을 저장할 수 없음

---

## ✅ 3. 정상 작동하는 API

### 3.1 수강생 목록 조회 (출석 포함)

**파일:** `EnrollmentController.java:80-125`

```java
@GetMapping
public ResponseEntity<?> getEnrollments(...) {
    // ...
    return ResponseEntity.ok(dtoList);  // ✅ List<EnrollmentDto> 반환 (DTO 사용)
    // 또는
    return ResponseEntity.ok(dtoPage);  // ✅ Page<EnrollmentDto> 반환
}
```

**특징:**
- DTO 패턴 적용됨
- `EnrollmentDto.attendanceRecords`에 출석 기록 포함
- ApiResponse 래퍼는 없지만 DTO는 사용

---

## 🔧 4. 권장 수정 사항

### 4.1 출석 업데이트 API 수정 (Priority: HIGH)

**현재:**
```java
return ResponseEntity.ok(updated);  // EnrollmentExtendedTbl
```

**수정 후:**
```java
EnrollmentDto dto = enrollmentService.toDto(updated);
return ResponseEntity.ok(createSuccessResponse("출석 정보가 업데이트되었습니다.", dto));
```

### 4.2 상세 출석 기록 API 추가 (Priority: MEDIUM)

**새 엔드포인트 추가:**
```java
@PostMapping("/{enrollmentIdx}/attendance/details")
public ResponseEntity<?> updateAttendanceDetails(
        @PathVariable Integer enrollmentIdx,
        @RequestBody List<AttendanceDto> attendanceRecords) {
    try {
        EnrollmentExtendedTbl updated = enrollmentService.updateAttendanceData(
                enrollmentIdx, convertToMap(attendanceRecords));
        EnrollmentDto dto = enrollmentService.toDto(updated);
        return ResponseEntity.ok(createSuccessResponse("출석 기록이 저장되었습니다.", dto));
    } catch (Exception e) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(createErrorResponse("출석 기록 저장 중 오류가 발생했습니다."));
    }
}
```

### 4.3 성적 업데이트 API도 동일하게 수정 (Priority: HIGH)

---

## 📊 5. 데이터 구조 예시

### 5.1 출석 정보가 포함된 수강생 조회 응답 (현재)

```json
{
  "success": true,
  "message": "조회 성공",
  "data": {
    "content": [
      {
        "enrollmentIdx": 1,
        "studentIdx": 6,
        "studentName": "테스터",
        "lecIdx": 1,
        "attendanceRecords": [
          {
            "date": "2025-10-15",
            "status": "PRESENT"
          },
          {
            "date": "2025-10-16",
            "status": "LATE"
          }
        ],
        "attended": 10,
        "absent": 2,
        "late": 1
      }
    ],
    "totalElements": 1
  }
}
```

### 5.2 출석 업데이트 요청

**간단 버전 (통계만):**
```json
{
  "attended": 10,
  "absent": 2,
  "late": 1
}
```

**상세 버전 (날짜별 기록):**
```json
[
  {
    "date": "2025-10-15",
    "status": "PRESENT"
  },
  {
    "date": "2025-10-16",
    "status": "LATE",
    "requestReason": "교통 체증"
  }
]
```

---

## 📝 6. 테스트 스크립트 작성 완료

**파일:** `lecture-test-7-attendance.js`

**제공 함수:**
- `getEnrollments()` - 수강생 목록 조회 (출석 포함)
- `getStudentAttendance()` - 특정 수강생 출석 조회
- `updateAttendance()` - 출석 정보 업데이트 (간단)
- `updateAttendanceDetail()` - 상세 기록 업데이트 (API 미구현 경고)

---

## 🎯 7. 다음 단계

### Phase 6.8.2 작업 항목

1. ✅ **출석 API 구조 분석 완료**
2. ✅ **테스트 스크립트 작성 완료**
3. ⚠️ **백엔드 수정 필요 (사용자 합의 후):**
   - [ ] `PUT /enrollments/{id}/attendance` - ApiResponse 래퍼 적용
   - [ ] `PUT /enrollments/{id}/grade` - ApiResponse 래퍼 적용
   - [ ] `POST /enrollments/{id}/attendance/details` - 신규 엔드포인트 추가 (선택)

### 확인 필요 사항

- `GET /enrollments/{id}` API가 DTO 패턴을 사용하는지 확인
- 다른 Enrollment 관련 API들도 일관성 점검

---

## 📌 요약

| 항목 | 상태 | 비고 |
|-----|------|------|
| 출석 조회 API | ✅ 정상 | DTO 패턴 적용 |
| 출석 업데이트 API | ❌ 문제 | Entity 직접 반환 |
| 성적 업데이트 API | ❌ 문제 | Entity 직접 반환 |
| 상세 출석 기록 API | ❌ 미구현 | Controller 매핑 없음 |
| AttendanceDto | ✅ 존재 | 구조 양호 |
| 테스트 스크립트 | ✅ 완료 | lecture-test-7-attendance.js |

**핵심 이슈:** 출석/성적 업데이트 API가 Phase 6.8 DTO 패턴을 따르지 않음
