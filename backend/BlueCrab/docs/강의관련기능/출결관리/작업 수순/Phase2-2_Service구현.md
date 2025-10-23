# Phase 2-2: Service 계층 구현

## 📋 작업 개요

**목표**: 출석 요청/승인 비즈니스 로직을 처리하는 Service 계층 구현  
**예상 소요 시간**: 4시간  
**상태**: ✅ 완료 (2025-10-23)

---

## 🎯 작업 내용

### 1. Service 인터페이스 생성

**파일 위치**: `src/main/java/BlueCrab/com/example/service/Lecture/AttendanceService.java`

```java
package BlueCrab.com.example.service.Lecture;

import BlueCrab.com.example.dto.Lecture.attendance.*;
import java.util.List;

/**
 * 출석 요청/승인 시스템 Service 인터페이스
 */
public interface AttendanceService {
    
    /**
     * 학생의 출석 요청 처리
     * 
     * @param lecSerial 강의 코드
     * @param sessionNumber 회차 번호 (1~80)
     * @param studentIdx 학생 USER_IDX
     * @param requestReason 요청 사유 (선택)
     * @return 출석 요청 결과 (현재 출석 데이터 포함)
     */
    AttendanceResponseDto<AttendanceDataDto> requestAttendance(
        String lecSerial,
        Integer sessionNumber,
        Integer studentIdx,
        String requestReason
    );
    
    /**
     * 교수의 출석 승인/반려 처리
     * 
     * @param lecSerial 강의 코드
     * @param sessionNumber 회차 번호
     * @param approvalRecords 승인 레코드 배열
     * @param professorIdx 교수 USER_IDX
     * @return 승인 처리 결과
     */
    AttendanceResponseDto<Void> approveAttendance(
        String lecSerial,
        Integer sessionNumber,
        List<AttendanceApprovalRecordDto> approvalRecords,
        Integer professorIdx
    );
    
    /**
     * 학생의 출석 현황 조회
     * 
     * @param lecSerial 강의 코드
     * @param studentIdx 학생 USER_IDX
     * @return 출석 데이터 (summary, sessions, pendingRequests)
     */
    AttendanceResponseDto<AttendanceDataDto> getStudentAttendance(
        String lecSerial,
        Integer studentIdx
    );
    
    /**
     * 교수의 강의 출석 현황 조회 (전체 학생)
     * 
     * @param lecSerial 강의 코드
     * @param professorIdx 교수 USER_IDX
     * @return 학생별 출석 데이터 배열
     */
    AttendanceResponseDto<List<StudentAttendanceDto>> getProfessorAttendance(
        String lecSerial,
        Integer professorIdx
    );
}
```

---

### 2. Service 구현 클래스

**파일 위치**: `src/main/java/BlueCrab/com/example/service/Lecture/impl/AttendanceServiceImpl.java`

#### 클래스 구조

```java
package BlueCrab.com.example.service.Lecture.impl;

import BlueCrab.com.example.dto.Lecture.attendance.*;
import BlueCrab.com.example.entity.Lecture.EnrollmentExtendedTbl;
import BlueCrab.com.example.repository.Lecture.EnrollmentExtendedTblRepository;
import BlueCrab.com.example.service.Lecture.AttendanceService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

@Slf4j
@Service
@Transactional
public class AttendanceServiceImpl implements AttendanceService {
    
    private final EnrollmentExtendedTblRepository enrollmentRepository;
    private final ObjectMapper objectMapper;
    
    @Autowired
    public AttendanceServiceImpl(
        EnrollmentExtendedTblRepository enrollmentRepository,
        ObjectMapper objectMapper
    ) {
        this.enrollmentRepository = enrollmentRepository;
        this.objectMapper = objectMapper;
    }
    
    // ... 메서드 구현
}
```

---

### 3. 핵심 메서드 구현

#### ① `requestAttendance()` - 출석 요청

```java
@Override
public AttendanceResponseDto<AttendanceDataDto> requestAttendance(
    String lecSerial,
    Integer sessionNumber,
    Integer studentIdx,
    String requestReason
) {
    try {
        // 1. 수강 정보 조회
        EnrollmentExtendedTbl enrollment = enrollmentRepository
            .findByLecSerialAndStudentIdx(lecSerial, studentIdx)
            .orElseThrow(() -> new NoSuchElementException("수강 정보를 찾을 수 없습니다."));
        
        // 2. JSON 파싱
        AttendanceDataDto attendanceData = parseAttendanceData(enrollment.getEnrollmentData());
        
        // 3. 중복 검증
        if (isDuplicateRequest(attendanceData, sessionNumber)) {
            return AttendanceResponseDto.error("이미 출석 요청이 존재합니다.");
        }
        
        // 4. pendingRequests에 추가
        AttendancePendingRequestDto newRequest = new AttendancePendingRequestDto();
        newRequest.setSessionNumber(sessionNumber);
        newRequest.setRequestDate(LocalDateTime.now());
        newRequest.setExpiresAt(LocalDateTime.now().plusDays(7)); // 7일 후 만료
        newRequest.setTempApproved(true);
        
        attendanceData.getPendingRequests().add(newRequest);
        
        // 5. JSON 직렬화 및 저장
        String updatedJson = serializeAttendanceData(attendanceData);
        enrollment.setEnrollmentData(updatedJson);
        enrollmentRepository.save(enrollment);
        
        log.info("출석 요청 성공: lecSerial={}, sessionNumber={}, studentIdx={}",
                 lecSerial, sessionNumber, studentIdx);
        
        return AttendanceResponseDto.success("출석 요청이 완료되었습니다.", attendanceData);
        
    } catch (NoSuchElementException e) {
        log.error("출석 요청 실패: {}", e.getMessage());
        return AttendanceResponseDto.error(e.getMessage());
    } catch (Exception e) {
        log.error("출석 요청 중 오류 발생", e);
        return AttendanceResponseDto.error("출석 요청 처리 중 오류가 발생했습니다.");
    }
}
```

#### ② `approveAttendance()` - 출석 승인

```java
@Override
public AttendanceResponseDto<Void> approveAttendance(
    String lecSerial,
    Integer sessionNumber,
    List<AttendanceApprovalRecordDto> approvalRecords,
    Integer professorIdx
) {
    try {
        // 1. 교수 권한 검증
        List<EnrollmentExtendedTbl> enrollments = 
            enrollmentRepository.findByLecSerialAndProfessorIdx(lecSerial, professorIdx);
        
        if (enrollments.isEmpty()) {
            return AttendanceResponseDto.error("해당 강의의 담당 교수가 아닙니다.");
        }
        
        // 2. 각 학생의 출석 처리
        for (AttendanceApprovalRecordDto record : approvalRecords) {
            processApproval(lecSerial, sessionNumber, record, professorIdx);
        }
        
        log.info("출석 승인 완료: lecSerial={}, sessionNumber={}, count={}",
                 lecSerial, sessionNumber, approvalRecords.size());
        
        return AttendanceResponseDto.success("출석 승인이 완료되었습니다.");
        
    } catch (Exception e) {
        log.error("출석 승인 중 오류 발생", e);
        return AttendanceResponseDto.error("출석 승인 처리 중 오류가 발생했습니다.");
    }
}

private void processApproval(
    String lecSerial,
    Integer sessionNumber,
    AttendanceApprovalRecordDto record,
    Integer professorIdx
) {
    // 1. 수강 정보 조회
    EnrollmentExtendedTbl enrollment = enrollmentRepository
        .findByLecSerialAndStudentIdx(lecSerial, record.getStudentIdx())
        .orElseThrow(() -> new NoSuchElementException("수강 정보를 찾을 수 없습니다."));
    
    // 2. JSON 파싱
    AttendanceDataDto attendanceData = parseAttendanceData(enrollment.getEnrollmentData());
    
    // 3. pendingRequests에서 제거
    AttendancePendingRequestDto pendingRequest = removePendingRequest(
        attendanceData.getPendingRequests(), sessionNumber
    );
    
    if (pendingRequest == null) {
        throw new IllegalStateException("대기 중인 출석 요청을 찾을 수 없습니다.");
    }
    
    // 4. sessions에 추가
    AttendanceSessionDto session = new AttendanceSessionDto();
    session.setSessionNumber(sessionNumber);
    session.setStatus(record.getStatus());
    session.setRequestDate(pendingRequest.getRequestDate());
    session.setApprovedDate(LocalDateTime.now());
    session.setApprovedBy(professorIdx);
    session.setTempApproved(false);
    
    attendanceData.getSessions().add(session);
    
    // 5. summary 재계산
    AttendanceSummaryDto summary = calculateSummary(attendanceData.getSessions());
    attendanceData.setSummary(summary);
    
    // 6. JSON 직렬화 및 저장
    String updatedJson = serializeAttendanceData(attendanceData);
    enrollment.setEnrollmentData(updatedJson);
    enrollmentRepository.save(enrollment);
}
```

---

### 4. 유틸리티 메서드

#### JSON 파싱

```java
/**
 * ENROLLMENT_DATA JSON에서 attendance 데이터 파싱
 */
private AttendanceDataDto parseAttendanceData(String enrollmentData) {
    try {
        if (enrollmentData == null || enrollmentData.isEmpty()) {
            return createEmptyAttendanceData();
        }
        
        Map<String, Object> jsonMap = objectMapper.readValue(enrollmentData, Map.class);
        Map<String, Object> attendanceMap = (Map<String, Object>) jsonMap.get("attendance");
        
        if (attendanceMap == null) {
            return createEmptyAttendanceData();
        }
        
        AttendanceDataDto dto = new AttendanceDataDto();
        dto.setSummary(parseSummary(attendanceMap.get("summary")));
        dto.setSessions(parseSessions(attendanceMap.get("sessions")));
        dto.setPendingRequests(parsePendingRequests(attendanceMap.get("pendingRequests")));
        
        return dto;
        
    } catch (Exception e) {
        log.error("JSON 파싱 오류", e);
        return createEmptyAttendanceData();
    }
}

private AttendanceDataDto createEmptyAttendanceData() {
    AttendanceDataDto dto = new AttendanceDataDto();
    dto.setSummary(new AttendanceSummaryDto(0, 0, 0, 0, 0.0, LocalDateTime.now()));
    dto.setSessions(new ArrayList<>());
    dto.setPendingRequests(new ArrayList<>());
    return dto;
}
```

#### 출석률 계산

```java
/**
 * sessions 배열을 기반으로 summary 통계 계산
 */
private AttendanceSummaryDto calculateSummary(List<AttendanceSessionDto> sessions) {
    int attended = 0;
    int late = 0;
    int absent = 0;
    
    for (AttendanceSessionDto session : sessions) {
        switch (session.getStatus()) {
            case "출": attended++; break;
            case "지": late++; break;
            case "결": absent++; break;
        }
    }
    
    int totalSessions = sessions.size();
    double attendanceRate = totalSessions > 0 ? (attended * 100.0) / totalSessions : 0.0;
    
    return new AttendanceSummaryDto(
        attended,
        late,
        absent,
        totalSessions,
        Math.round(attendanceRate * 100.0) / 100.0, // 소수점 2자리
        LocalDateTime.now()
    );
}
```

#### 중복 검증

```java
/**
 * 중복 요청 여부 검증
 */
private boolean isDuplicateRequest(AttendanceDataDto attendanceData, Integer sessionNumber) {
    // sessions에 이미 존재하는지 확인
    boolean existsInSessions = attendanceData.getSessions().stream()
        .anyMatch(s -> s.getSessionNumber().equals(sessionNumber));
    
    // pendingRequests에 이미 존재하는지 확인
    boolean existsInPending = attendanceData.getPendingRequests().stream()
        .anyMatch(p -> p.getSessionNumber().equals(sessionNumber));
    
    return existsInSessions || existsInPending;
}
```

#### JSON 직렬화

```java
/**
 * AttendanceDataDto를 JSON 문자열로 변환
 */
private String serializeAttendanceData(AttendanceDataDto attendanceData) {
    try {
        Map<String, Object> attendanceMap = new HashMap<>();
        attendanceMap.put("summary", attendanceData.getSummary());
        attendanceMap.put("sessions", attendanceData.getSessions());
        attendanceMap.put("pendingRequests", attendanceData.getPendingRequests());
        
        return objectMapper.writeValueAsString(Map.of("attendance", attendanceMap));
        
    } catch (Exception e) {
        log.error("JSON 직렬화 오류", e);
        throw new RuntimeException("JSON 변환 중 오류가 발생했습니다.");
    }
}
```

---

## 📋 체크리스트

### 인터페이스 & 구현
- [ ] `AttendanceService` 인터페이스 생성
- [ ] `AttendanceServiceImpl` 클래스 생성
- [ ] `@Service`, `@Transactional` 어노테이션 적용
- [ ] 생성자 의존성 주입 구현

### 핵심 메서드
- [ ] `requestAttendance()` 구현
- [ ] `approveAttendance()` 구현
- [ ] `getStudentAttendance()` 구현
- [ ] `getProfessorAttendance()` 구현

### 유틸리티 메서드
- [ ] `parseAttendanceData()` - JSON 파싱
- [ ] `serializeAttendanceData()` - JSON 직렬화
- [ ] `calculateSummary()` - 출석률 계산
- [ ] `isDuplicateRequest()` - 중복 검증
- [ ] `removePendingRequest()` - 대기 요청 제거

### 에러 핸들링
- [ ] `NoSuchElementException` 처리
- [ ] `IllegalStateException` 처리
- [ ] `AccessDeniedException` 처리
- [ ] 로깅 추가 (`@Slf4j`)

---

## 🎯 다음 단계

**Phase 3-1: Controller 구현**
- `AttendanceController` 클래스 생성
- REST API 엔드포인트 구현
- Request Validation

---

## 📝 예상 산출물

- `AttendanceService.java` (인터페이스)
- `AttendanceServiceImpl.java` (구현 클래스, 약 400~500 라인)

---

## ⚠️ 주의사항

### 1. JSON 파싱 안전성
- null 체크 필수
- 빈 JSON 처리
- 파싱 실패 시 기본값 반환

### 2. 트랜잭션 관리
- `@Transactional` 적용
- 롤백 정책 설정
- 예외 발생 시 자동 롤백

### 3. 출석률 계산
- 소수점 2자리 반올림
- 0으로 나누기 방지
- 백분율 범위 (0.0~100.0)

### 4. 대기 요청 관리
- 만료 시간: requestDate + 7일
- `tempApproved`: 자동승인 플래그
- 최대 80개 제한 (강의 회차)

---

## 📚 참고 문서

- [출석요청승인_플로우.md](../../출석요청승인_플로우.md)
- [ENROLLMENT_DATA_JSON_명세서.md](../../../전체가이드/데이터구조/ENROLLMENT_DATA_JSON_명세서.md)
- Repository: `EnrollmentExtendedTblRepository.java`
- DTO 패키지: `dto/Lecture/attendance/`
