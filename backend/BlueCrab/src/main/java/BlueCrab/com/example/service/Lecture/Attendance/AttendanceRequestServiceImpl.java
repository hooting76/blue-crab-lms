package BlueCrab.com.example.service.Lecture.Attendance;

import BlueCrab.com.example.dto.Lecture.Attendance.*;
import BlueCrab.com.example.entity.Lecture.EnrollmentExtendedTbl;
import BlueCrab.com.example.repository.Lecture.EnrollmentExtendedTblRepository;
import BlueCrab.com.example.service.Lecture.AttendanceRequestService;
import BlueCrab.com.example.service.Lecture.GradeCalculationService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 출석 요청/승인 시스템 Service 구현 클래스
 * 
 * ENROLLMENT_DATA JSON 필드의 attendance 구조를 사용하여
 * 출석 요청, 승인, 조회 기능을 제공
 * 
 * @author BlueCrab Development Team
 * @version 1.0.0
 * @since 2025-10-23
 */
@Slf4j
@Service
@Transactional
public class AttendanceRequestServiceImpl implements AttendanceRequestService {
    
    private final EnrollmentExtendedTblRepository enrollmentRepository;
    private final ObjectMapper objectMapper;
    private final GradeCalculationService gradeCalculationService;
    
    @Autowired
    public AttendanceRequestServiceImpl(
        EnrollmentExtendedTblRepository enrollmentRepository,
        ObjectMapper objectMapper,
        @Lazy GradeCalculationService gradeCalculationService
    ) {
        this.enrollmentRepository = enrollmentRepository;
        this.objectMapper = objectMapper;
        this.gradeCalculationService = gradeCalculationService;
    }
    
    @Override
    public AttendanceResponseDto<AttendanceDataDto> requestAttendance(
        String lecSerial,
        Integer sessionNumber,
        Integer studentIdx,
        String requestReason
    ) {
        try {
            log.info("출석 요청 시작: lecSerial={}, sessionNumber={}, studentIdx={}", 
                     lecSerial, sessionNumber, studentIdx);
            
            // 1. 수강 정보 조회
            EnrollmentExtendedTbl enrollment = enrollmentRepository
                .findByLecSerialAndStudentIdx(lecSerial, studentIdx)
                .orElseThrow(() -> new NoSuchElementException("수강 정보를 찾을 수 없습니다."));
            
            // 2. JSON 파싱
            AttendanceDataDto attendanceData = parseAttendanceData(enrollment.getEnrollmentData());
            
            // 3. 중복 검증
            if (isDuplicateRequest(attendanceData, sessionNumber)) {
                log.warn("중복 출석 요청: sessionNumber={}", sessionNumber);
                return AttendanceResponseDto.error("이미 출석 요청이 존재합니다.");
            }
            
            // 4. pendingRequests에 추가
            AttendancePendingRequestDto newRequest = new AttendancePendingRequestDto();
            newRequest.setSessionNumber(sessionNumber);
            newRequest.setRequestDate(LocalDateTime.now());
            // 날짜 기준 7일 후 (시간 무관, 날짜만 카운트)
            // 예: 10/23 08:00 요청 → 10/30 05:00 스케줄러에서 처리
            newRequest.setExpiresAt(LocalDateTime.now().plusDays(7).withHour(0).withMinute(0).withSecond(0).withNano(0));
            newRequest.setTempApproved(true); // 자동승인 플래그
            
            if (attendanceData.getPendingRequests() == null) {
                attendanceData.setPendingRequests(new ArrayList<>());
            }
            attendanceData.getPendingRequests().add(newRequest);
            
            // 5. JSON 직렬화 및 저장
            String updatedJson = serializeToEnrollmentData(attendanceData, enrollment.getEnrollmentData());
            enrollment.setEnrollmentData(updatedJson);
            enrollmentRepository.save(enrollment);
            
            log.info("출석 요청 완료: lecSerial={}, sessionNumber={}, studentIdx={}", 
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
    
    @Override
    public AttendanceResponseDto<Void> approveAttendance(
        String lecSerial,
        Integer sessionNumber,
        List<AttendanceApprovalRecordDto> approvalRecords,
        Integer professorIdx
    ) {
        try {
            log.info("출석 승인 시작: lecSerial={}, sessionNumber={}, count={}", 
                     lecSerial, sessionNumber, approvalRecords.size());
            
            // 1. 교수 권한 검증
            List<EnrollmentExtendedTbl> enrollments = 
                enrollmentRepository.findByLecSerialAndProfessorIdx(lecSerial, professorIdx);
            
            if (enrollments.isEmpty()) {
                log.warn("권한 없음: professorIdx={}, lecSerial={}", professorIdx, lecSerial);
                return AttendanceResponseDto.error("해당 강의의 담당 교수가 아닙니다.");
            }
            
            // 2. 각 학생의 출석 처리
            int successCount = 0;
            for (AttendanceApprovalRecordDto record : approvalRecords) {
                try {
                    processApproval(lecSerial, sessionNumber, record, professorIdx);
                    successCount++;
                } catch (Exception e) {
                    log.error("출석 승인 실패: studentIdx={}, error={}", 
                              record.getStudentIdx(), e.getMessage(), e);
                    // 개별 실패는 로그만 남기고 계속 진행
                }
            }
            
            log.info("출석 승인 완료: lecSerial={}, sessionNumber={}, success={}/{}", 
                     lecSerial, sessionNumber, successCount, approvalRecords.size());
            
            return AttendanceResponseDto.success(
                String.format("출석 승인이 완료되었습니다. (%d/%d)", successCount, approvalRecords.size())
            );
            
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
            .orElseThrow(() -> new NoSuchElementException(
                "수강 정보를 찾을 수 없습니다: studentIdx=" + record.getStudentIdx()));
        
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
        session.setTempApproved(false); // 교수가 직접 승인
        
        if (attendanceData.getSessions() == null) {
            attendanceData.setSessions(new ArrayList<>());
        }
        attendanceData.getSessions().add(session);
        
        // 5. summary 재계산
        AttendanceSummaryDto summary = calculateSummary(attendanceData.getSessions());
        attendanceData.setSummary(summary);
        
        // 6. JSON 직렬화 및 저장
        String updatedJson = serializeToEnrollmentData(attendanceData, enrollment.getEnrollmentData());
        enrollment.setEnrollmentData(updatedJson);
        enrollmentRepository.save(enrollment);
        
        log.info("출석 데이터 저장 완료: lecSerial={}, studentIdx={}, sessionNumber={}", 
                 lecSerial, record.getStudentIdx(), sessionNumber);
        
        // 7. 성적 재계산 (출석 점수 자동 반영) - 별도 트랜잭션으로 실행 (REQUIRES_NEW)
        // 성적 재계산 실패해도 출석 승인은 유지됨
        Integer lecIdx = enrollment.getLecIdx();
        Integer studentIdx = record.getStudentIdx();
        
        // 트랜잭션 커밋 후 실행하기 위해 TransactionSynchronization 사용
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                try {
                    log.info("성적 재계산 시작: lecIdx={}, studentIdx={}", lecIdx, studentIdx);
                    gradeCalculationService.calculateStudentGrade(lecIdx, studentIdx);
                    log.info("성적 재계산 완료: lecIdx={}, studentIdx={}", lecIdx, studentIdx);
                } catch (Exception e) {
                    log.error("성적 재계산 실패: lecIdx={}, studentIdx={}, error={}", 
                              lecIdx, studentIdx, e.getMessage(), e);
                    // 성적 재계산 실패해도 출석 승인은 이미 커밋됨
                }
            }
        });
    }
    
    @Override
    public AttendanceResponseDto<AttendanceDataDto> getStudentAttendance(
        String lecSerial,
        Integer studentIdx
    ) {
        try {
            log.info("학생 출석 조회: lecSerial={}, studentIdx={}", lecSerial, studentIdx);
            
            EnrollmentExtendedTbl enrollment = enrollmentRepository
                .findByLecSerialAndStudentIdx(lecSerial, studentIdx)
                .orElseThrow(() -> new NoSuchElementException("수강 정보를 찾을 수 없습니다."));
            
            AttendanceDataDto attendanceData = parseAttendanceData(enrollment.getEnrollmentData());
            
            return AttendanceResponseDto.success("출석 현황 조회 성공", attendanceData);
            
        } catch (Exception e) {
            log.error("학생 출석 조회 실패", e);
            return AttendanceResponseDto.error("출석 현황 조회에 실패했습니다.");
        }
    }
    
    @Override
    public AttendanceResponseDto<List<StudentAttendanceDto>> getProfessorAttendance(
        String lecSerial,
        Integer professorIdx
    ) {
        try {
            log.info("교수 출석 조회: lecSerial={}, professorIdx={}", lecSerial, professorIdx);
            
            // 1. 권한 검증 및 수강생 조회
            List<EnrollmentExtendedTbl> enrollments = 
                enrollmentRepository.findByLecSerialAndProfessorIdx(lecSerial, professorIdx);
            
            if (enrollments.isEmpty()) {
                return AttendanceResponseDto.error("해당 강의의 담당 교수가 아닙니다.");
            }
            
            // 2. 각 학생의 출석 데이터 수집
            List<StudentAttendanceDto> studentAttendances = enrollments.stream()
                .map(enrollment -> {
                    StudentAttendanceDto dto = new StudentAttendanceDto();
                    dto.setStudentIdx(enrollment.getStudentIdx());
                    dto.setStudentCode(enrollment.getStudent().getUserCode());
                    dto.setStudentName(enrollment.getStudent().getUserName());
                    dto.setAttendanceData(parseAttendanceData(enrollment.getEnrollmentData()));
                    return dto;
                })
                .collect(Collectors.toList());
            
            return AttendanceResponseDto.success("출석 현황 조회 성공", studentAttendances);
            
        } catch (Exception e) {
            log.error("교수 출석 조회 실패", e);
            return AttendanceResponseDto.error("출석 현황 조회에 실패했습니다.");
        }
    }
    
    @Override
    public int processExpiredRequests() {
        log.info("만료된 출석 요청 자동 승인 처리 시작");
        
        int processedCount = 0;
        LocalDateTime now = LocalDateTime.now();
        
        try {
            // 모든 수강 정보 조회
            List<EnrollmentExtendedTbl> allEnrollments = enrollmentRepository.findAll();
            
            for (EnrollmentExtendedTbl enrollment : allEnrollments) {
                try {
                    AttendanceDataDto attendanceData = parseAttendanceData(enrollment.getEnrollmentData());
                    
                    if (attendanceData.getPendingRequests() == null || 
                        attendanceData.getPendingRequests().isEmpty()) {
                        continue;
                    }
                    
                    List<AttendancePendingRequestDto> expiredRequests = new ArrayList<>();
                    List<AttendancePendingRequestDto> remainingRequests = new ArrayList<>();
                    
                    // 만료된 요청과 유효한 요청 분리
                    for (AttendancePendingRequestDto request : attendanceData.getPendingRequests()) {
                        if (now.isAfter(request.getExpiresAt())) {
                            expiredRequests.add(request);
                        } else {
                            remainingRequests.add(request);
                        }
                    }
                    
                    if (expiredRequests.isEmpty()) {
                        continue;
                    }
                    
                    // 만료된 요청을 sessions로 이동 (자동 출석 처리)
                    if (attendanceData.getSessions() == null) {
                        attendanceData.setSessions(new ArrayList<>());
                    }
                    
                    for (AttendancePendingRequestDto expired : expiredRequests) {
                        AttendanceSessionDto session = new AttendanceSessionDto();
                        session.setSessionNumber(expired.getSessionNumber());
                        session.setStatus("출"); // 자동 출석 처리
                        session.setRequestDate(expired.getRequestDate());
                        session.setApprovedDate(now);
                        session.setApprovedBy(null); // 자동 승인은 승인자 없음
                        session.setTempApproved(true); // 자동 승인 플래그
                        
                        attendanceData.getSessions().add(session);
                        processedCount++;
                    }
                    
                    // pendingRequests 업데이트
                    attendanceData.setPendingRequests(remainingRequests);
                    
                    // summary 재계산
                    attendanceData.setSummary(calculateSummary(attendanceData.getSessions()));
                    
                    // 저장
                    String updatedJson = serializeToEnrollmentData(attendanceData, enrollment.getEnrollmentData());
                    enrollment.setEnrollmentData(updatedJson);
                    enrollmentRepository.save(enrollment);
                    
                } catch (Exception e) {
                    log.error("자동 승인 처리 실패: enrollmentIdx={}", enrollment.getEnrollmentIdx(), e);
                }
            }
            
            log.info("만료된 출석 요청 자동 승인 처리 완료: 처리 건수={}", processedCount);
            
        } catch (Exception e) {
            log.error("자동 승인 처리 중 오류 발생", e);
        }
        
        return processedCount;
    }
    
    // ========== 유틸리티 메서드 ==========
    
    /**
     * ENROLLMENT_DATA JSON에서 attendance 데이터 파싱
     */
    private AttendanceDataDto parseAttendanceData(String enrollmentData) {
        try {
            if (enrollmentData == null || enrollmentData.isEmpty()) {
                return createEmptyAttendanceData();
            }
            
            Map<String, Object> jsonMap = objectMapper.readValue(enrollmentData, 
                new TypeReference<Map<String, Object>>() {});
            
            Object attendanceObj = jsonMap.get("attendance");
            if (attendanceObj == null) {
                return createEmptyAttendanceData();
            }
            
            // attendance 객체를 AttendanceDataDto로 변환
            String attendanceJson = objectMapper.writeValueAsString(attendanceObj);
            AttendanceDataDto dto = objectMapper.readValue(attendanceJson, AttendanceDataDto.class);
            
            // null 체크 및 초기화
            if (dto.getSummary() == null) {
                dto.setSummary(new AttendanceSummaryDto(0, 0, 0, 0, 0.0, LocalDateTime.now()));
            }
            if (dto.getSessions() == null) {
                dto.setSessions(new ArrayList<>());
            }
            if (dto.getPendingRequests() == null) {
                dto.setPendingRequests(new ArrayList<>());
            }
            
            return dto;
            
        } catch (Exception e) {
            log.error("JSON 파싱 오류", e);
            return createEmptyAttendanceData();
        }
    }
    
    /**
     * 빈 출석 데이터 생성
     */
    private AttendanceDataDto createEmptyAttendanceData() {
        AttendanceDataDto dto = new AttendanceDataDto();
        dto.setSummary(new AttendanceSummaryDto(0, 0, 0, 0, 0.0, LocalDateTime.now()));
        dto.setSessions(new ArrayList<>());
        dto.setPendingRequests(new ArrayList<>());
        return dto;
    }
    
    /**
     * AttendanceDataDto를 ENROLLMENT_DATA JSON에 병합
     */
    private String serializeToEnrollmentData(AttendanceDataDto attendanceData, String existingData) {
        try {
            Map<String, Object> jsonMap;
            
            if (existingData == null || existingData.isEmpty()) {
                jsonMap = new HashMap<>();
            } else {
                jsonMap = objectMapper.readValue(existingData, 
                    new TypeReference<Map<String, Object>>() {});
            }
            
            // attendance 필드 업데이트
            Map<String, Object> attendanceMap = new HashMap<>();
            attendanceMap.put("summary", attendanceData.getSummary());
            attendanceMap.put("sessions", attendanceData.getSessions());
            attendanceMap.put("pendingRequests", attendanceData.getPendingRequests());
            
            jsonMap.put("attendance", attendanceMap);
            
            return objectMapper.writeValueAsString(jsonMap);
            
        } catch (Exception e) {
            log.error("JSON 직렬화 오류", e);
            throw new RuntimeException("JSON 변환 중 오류가 발생했습니다.", e);
        }
    }
    
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
        attendanceRate = Math.round(attendanceRate * 100.0) / 100.0; // 소수점 2자리
        
        return new AttendanceSummaryDto(
            attended,
            late,
            absent,
            totalSessions,
            attendanceRate,
            LocalDateTime.now()
        );
    }
    
    /**
     * 중복 요청 여부 검증
     */
    private boolean isDuplicateRequest(AttendanceDataDto attendanceData, Integer sessionNumber) {
        // sessions에 이미 존재하는지 확인
        if (attendanceData.getSessions() != null) {
            boolean existsInSessions = attendanceData.getSessions().stream()
                .anyMatch(s -> s.getSessionNumber().equals(sessionNumber));
            if (existsInSessions) return true;
        }
        
        // pendingRequests에 이미 존재하는지 확인
        if (attendanceData.getPendingRequests() != null) {
            boolean existsInPending = attendanceData.getPendingRequests().stream()
                .anyMatch(p -> p.getSessionNumber().equals(sessionNumber));
            if (existsInPending) return true;
        }
        
        return false;
    }
    
    /**
     * pendingRequests에서 특정 sessionNumber 제거 및 반환
     */
    private AttendancePendingRequestDto removePendingRequest(
        List<AttendancePendingRequestDto> pendingRequests,
        Integer sessionNumber
    ) {
        if (pendingRequests == null) {
            return null;
        }
        
        for (int i = 0; i < pendingRequests.size(); i++) {
            AttendancePendingRequestDto request = pendingRequests.get(i);
            if (request.getSessionNumber().equals(sessionNumber)) {
                pendingRequests.remove(i);
                return request;
            }
        }
        
        return null;
    }
}
