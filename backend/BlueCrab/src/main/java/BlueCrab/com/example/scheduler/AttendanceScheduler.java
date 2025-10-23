package BlueCrab.com.example.scheduler;

import BlueCrab.com.example.dto.Lecture.Attendance.AttendanceDataDto;
import BlueCrab.com.example.dto.Lecture.Attendance.AttendancePendingRequestDto;
import BlueCrab.com.example.dto.Lecture.Attendance.AttendanceSessionDto;
import BlueCrab.com.example.entity.Lecture.EnrollmentExtendedTbl;
import BlueCrab.com.example.repository.Lecture.EnrollmentExtendedTblRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 출석 요청 자동 승인 스케줄러
 * 
 * 매일 오전 5시에 실행되어 만료된 출석 요청을 자동으로 승인 처리합니다.
 * 
 * 만료 기준:
 * - 날짜 기준 7일 후 (시간 무관)
 * - 예: 10/23 08:00 요청 → 10/30 00:00:00이 만료 시점
 * - 10/30 05:00 스케줄러 실행 시 자동 승인
 * 
 * 처리 로직:
 * 1. 모든 수강 정보 조회
 * 2. pendingRequests에서 만료된 요청 필터링
 * 3. sessions에 "출석"으로 이동
 * 4. pendingRequests에서 제거
 * 5. summary 재계산
 * 6. DB 업데이트
 * 
 * @author BlueCrab Development Team
 * @version 1.0.0
 * @since 2025-10-23
 */
@Slf4j
@Component
public class AttendanceScheduler {
    
    @Autowired
    private EnrollmentExtendedTblRepository enrollmentRepository;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    /**
     * 만료된 출석 요청 자동 승인 처리
     * 
     * 실행 시각: 매일 오전 5시
     * Cron 표현식: "0 0 5 * * *"
     * - 초(0) 분(0) 시(5) 일(*) 월(*) 요일(*)
     * 
     * 날짜 기준 만료 처리:
     * - 현재 날짜(LocalDate)와 expiresAt 날짜 비교
     * - 시간은 무관하게 날짜만으로 판단
     * - 예: expiresAt = 2025-10-30 00:00:00
     *      → 2025-10-30 05:00 스케줄러에서 처리
     */
    @Scheduled(cron = "0 0 5 * * *")
    @Transactional
    public void processExpiredRequests() {
        log.info("========================================");
        log.info("출석 요청 자동 승인 스케줄러 시작");
        log.info("실행 시각: {}", LocalDateTime.now());
        log.info("========================================");
        
        LocalDate today = LocalDate.now(); // 오늘 날짜 (시간 제외)
        int totalProcessed = 0;
        int totalApproved = 0;
        int totalFailed = 0;
        
        try {
            // 1. 모든 수강 정보 조회
            List<EnrollmentExtendedTbl> enrollments = enrollmentRepository.findAll();
            log.info("조회된 수강 정보: {}건", enrollments.size());
            
            for (EnrollmentExtendedTbl enrollment : enrollments) {
                try {
                    // 2. JSON 파싱
                    AttendanceDataDto attendanceData = parseAttendanceData(enrollment.getEnrollmentData());
                    
                    if (attendanceData == null || attendanceData.getPendingRequests() == null 
                        || attendanceData.getPendingRequests().isEmpty()) {
                        continue; // 대기 중인 요청 없음
                    }
                    
                    // 3. 만료된 요청 필터링 (날짜 기준)
                    List<AttendancePendingRequestDto> expiredRequests = attendanceData.getPendingRequests().stream()
                        .filter(request -> {
                            LocalDate expiresDate = request.getExpiresAt().toLocalDate();
                            // 만료 날짜가 오늘이거나 이전이면 만료됨
                            return !expiresDate.isAfter(today);
                        })
                        .collect(Collectors.toList());
                    
                    if (expiredRequests.isEmpty()) {
                        continue; // 만료된 요청 없음
                    }
                    
                    log.info("만료된 요청 발견: enrollmentIdx={}, lecIdx={}, studentIdx={}, count={}", 
                             enrollment.getEnrollmentIdx(), enrollment.getLecIdx(), 
                             enrollment.getStudentIdx(), expiredRequests.size());
                    
                    // 4. sessions에 "출석"으로 이동
                    for (AttendancePendingRequestDto expiredRequest : expiredRequests) {
                        moveToSessions(attendanceData, expiredRequest);
                        totalApproved++;
                    }
                    
                    // 5. pendingRequests에서 제거
                    attendanceData.getPendingRequests().removeAll(expiredRequests);
                    
                    // 6. summary 재계산
                    recalculateSummary(attendanceData);
                    
                    // 7. JSON 직렬화 및 DB 업데이트
                    String updatedJson = serializeToEnrollmentData(attendanceData, enrollment.getEnrollmentData());
                    enrollment.setEnrollmentData(updatedJson);
                    enrollmentRepository.save(enrollment);
                    
                    totalProcessed++;
                    
                    log.info("자동 승인 완료: enrollmentIdx={}, 승인 건수={}", 
                             enrollment.getEnrollmentIdx(), expiredRequests.size());
                    
                } catch (Exception e) {
                    totalFailed++;
                    log.error("수강 정보 처리 실패: enrollmentIdx={}, error={}", 
                             enrollment.getEnrollmentIdx(), e.getMessage(), e);
                    // 개별 실패는 로그만 남기고 계속 진행
                }
            }
            
            log.info("========================================");
            log.info("출석 요청 자동 승인 완료");
            log.info("처리된 수강 정보: {}건", totalProcessed);
            log.info("자동 승인된 요청: {}건", totalApproved);
            log.info("실패 건수: {}건", totalFailed);
            log.info("========================================");
            
        } catch (Exception e) {
            log.error("출석 요청 자동 승인 스케줄러 전체 오류", e);
        }
    }
    
    /**
     * 만료된 요청을 sessions에 "출석"으로 추가
     * 
     * @param attendanceData 출석 데이터
     * @param expiredRequest 만료된 요청
     */
    private void moveToSessions(AttendanceDataDto attendanceData, AttendancePendingRequestDto expiredRequest) {
        if (attendanceData.getSessions() == null) {
            attendanceData.setSessions(new ArrayList<>());
        }
        
        // 새로운 출석 세션 생성
        AttendanceSessionDto newSession = new AttendanceSessionDto();
        newSession.setSessionNumber(expiredRequest.getSessionNumber());
        newSession.setStatus("출"); // 자동 승인은 "출석"으로 처리
        newSession.setRequestDate(expiredRequest.getRequestDate());
        newSession.setApprovedDate(LocalDateTime.now());
        newSession.setApprovedBy(null); // 자동 승인 (시스템)
        newSession.setTempApproved(true); // 자동 승인 플래그
        
        // sessions에 추가 (회차 순으로 정렬 유지)
        attendanceData.getSessions().add(newSession);
        attendanceData.getSessions().sort((a, b) -> 
            Integer.compare(a.getSessionNumber(), b.getSessionNumber())
        );
        
        log.debug("세션 추가: sessionNumber={}, status=출석 (자동승인)", 
                 expiredRequest.getSessionNumber());
    }
    
    /**
     * 출석 통계 재계산
     * 
     * @param attendanceData 출석 데이터
     */
    private void recalculateSummary(AttendanceDataDto attendanceData) {
        if (attendanceData.getSummary() == null) {
            attendanceData.setSummary(new BlueCrab.com.example.dto.Lecture.Attendance.AttendanceSummaryDto());
        }
        
        int attended = 0;
        int late = 0;
        int absent = 0;
        
        if (attendanceData.getSessions() != null) {
            for (AttendanceSessionDto session : attendanceData.getSessions()) {
                switch (session.getStatus()) {
                    case "출":
                        attended++;
                        break;
                    case "지":
                        late++;
                        break;
                    case "결":
                        absent++;
                        break;
                }
            }
        }
        
        int total = attended + late + absent;
        double attendanceRate = total > 0 ? (double) (attended + late) / total * 100.0 : 0.0;
        attendanceRate = Math.round(attendanceRate * 10.0) / 10.0; // 소수점 첫째자리
        
        attendanceData.getSummary().setAttended(attended);
        attendanceData.getSummary().setLate(late);
        attendanceData.getSummary().setAbsent(absent);
        attendanceData.getSummary().setAttendanceRate(attendanceRate);
        attendanceData.getSummary().setUpdatedAt(LocalDateTime.now());
        
        log.debug("통계 재계산: 출석={}, 지각={}, 결석={}, 출석률={}%", 
                 attended, late, absent, attendanceRate);
    }
    
    /**
     * JSON 데이터 파싱
     * 
     * @param enrollmentData ENROLLMENT_DATA JSON 문자열
     * @return 파싱된 출석 데이터
     */
    private AttendanceDataDto parseAttendanceData(String enrollmentData) {
        try {
            if (enrollmentData == null || enrollmentData.trim().isEmpty()) {
                return new AttendanceDataDto();
            }
            
            Map<String, Object> dataMap = objectMapper.readValue(
                enrollmentData, 
                new TypeReference<Map<String, Object>>() {}
            );
            
            Object attendanceObj = dataMap.get("attendance");
            if (attendanceObj == null) {
                return new AttendanceDataDto();
            }
            
            return objectMapper.convertValue(attendanceObj, AttendanceDataDto.class);
            
        } catch (Exception e) {
            log.error("JSON 파싱 오류: {}", e.getMessage());
            return new AttendanceDataDto();
        }
    }
    
    /**
     * JSON 데이터 직렬화
     * 
     * @param attendanceData 출석 데이터
     * @param originalJson 원본 ENROLLMENT_DATA JSON
     * @return 업데이트된 JSON 문자열
     */
    private String serializeToEnrollmentData(AttendanceDataDto attendanceData, String originalJson) {
        try {
            Map<String, Object> dataMap;
            
            if (originalJson == null || originalJson.trim().isEmpty()) {
                dataMap = new java.util.HashMap<>();
            } else {
                dataMap = objectMapper.readValue(
                    originalJson, 
                    new TypeReference<Map<String, Object>>() {}
                );
            }
            
            dataMap.put("attendance", attendanceData);
            
            return objectMapper.writeValueAsString(dataMap);
            
        } catch (Exception e) {
            log.error("JSON 직렬화 오류: {}", e.getMessage());
            throw new RuntimeException("JSON 직렬화 실패", e);
        }
    }
    
    /**
     * 수동 실행 메서드 (테스트용)
     * 
     * 실제 배포 시에는 제거 또는 주석 처리
     * Controller나 Admin 페이지에서 수동으로 스케줄러를 실행할 수 있습니다.
     */
    public void manualTrigger() {
        log.info("=== 수동 실행: 출석 요청 자동 승인 ===");
        processExpiredRequests();
    }
}
