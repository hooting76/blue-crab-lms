package BlueCrab.com.example.service.Lecture;

import BlueCrab.com.example.entity.Lecture.AttendanceRequestTbl;
import BlueCrab.com.example.entity.Lecture.EnrollmentExtendedTbl;
import BlueCrab.com.example.repository.Lecture.AttendanceRequestRepository;
import BlueCrab.com.example.repository.Lecture.EnrollmentExtendedTblRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 출석 관리 서비스
 * 
 * 기능:
 * 1. 출석 문자열 파싱: "1출2출3결4출...80출"
 * 2. 출석률 계산: "75/80"
 * 3. 출석 기록 업데이트
 * 4. 사유 신청 승인/반려
 */
@Service
@Slf4j
public class AttendanceService {

    @Autowired
    private EnrollmentExtendedTblRepository enrollmentRepository;

    @Autowired
    private AttendanceRequestRepository requestRepository;

    @Autowired
    private ObjectMapper objectMapper;

    // 출석 상태 상수 (출/결/지 3가지만 사용)
    public static final String STATUS_PRESENT = "출";  // 출석
    public static final String STATUS_ABSENT = "결";   // 결석
    public static final String STATUS_LATE = "지";     // 지각

    // 승인 상태 상수
    public static final String APPROVAL_PENDING = "PENDING";
    public static final String APPROVAL_APPROVED = "APPROVED";
    public static final String APPROVAL_REJECTED = "REJECTED";

    /**
     * 출석 문자열 파싱
     * 
     * @param attendanceStr "1출2출3결4지...80출"
     * @return Map<회차번호, 출석상태>
     */
    public Map<Integer, String> parseAttendanceString(String attendanceStr) {
        Map<Integer, String> result = new HashMap<>();
        
        if (attendanceStr == null || attendanceStr.isEmpty()) {
            return result;
        }

        // 정규식: 숫자 + 한글 1글자 (출/결/지 3가지만)
        Pattern pattern = Pattern.compile("(\\d+)([출결지])");
        Matcher matcher = pattern.matcher(attendanceStr);

        while (matcher.find()) {
            int sessionNumber = Integer.parseInt(matcher.group(1));
            String status = matcher.group(2);
            result.put(sessionNumber, status);
        }

        return result;
    }

    /**
     * Map을 출석 문자열로 변환
     * 
     * @param attendanceMap Map<회차번호, 출석상태>
     * @return "1출2출3결4출...80출"
     */
    public String buildAttendanceString(Map<Integer, String> attendanceMap) {
        StringBuilder sb = new StringBuilder();
        
        // 회차 번호 순으로 정렬
        List<Integer> sessions = new ArrayList<>(attendanceMap.keySet());
        Collections.sort(sessions);

        for (Integer session : sessions) {
            sb.append(session).append(attendanceMap.get(session));
        }

        return sb.toString();
    }

    /**
     * 출석률 계산
     * 
     * @param attendanceStr "1출2출3결4지...80출"
     * @param totalSessions 총 회차 수 (기본값 80)
     * @return "75/80" (출석+지각 합계 / 총 회차)
     */
    public String calculateAttendanceRate(String attendanceStr, int totalSessions) {
        Map<Integer, String> attendanceMap = parseAttendanceString(attendanceStr);

        int presentCount = 0;
        for (String status : attendanceMap.values()) {
            // 출석, 지각만 출석으로 인정
            if (STATUS_PRESENT.equals(status) || STATUS_LATE.equals(status)) {
                presentCount++;
            }
        }

        return presentCount + "/" + totalSessions;
    }

    /**
     * 출석 기록 (교수가 출석 체크)
     * 
     * @param enrollmentIdx 수강신청 IDX
     * @param sessionNumber 회차 번호 (1~80)
     * @param status 출석 상태 (출/결/지)
     */
    @Transactional
    public EnrollmentExtendedTbl markAttendance(Integer enrollmentIdx, 
                                                 Integer sessionNumber, 
                                                 String status) {
        EnrollmentExtendedTbl enrollment = enrollmentRepository.findById(enrollmentIdx)
                .orElseThrow(() -> new RuntimeException("수강신청을 찾을 수 없습니다."));

        try {
            // ENROLLMENT_DATA JSON 파싱
            ObjectNode enrollmentData;
            if (enrollment.getEnrollmentData() == null || enrollment.getEnrollmentData().isEmpty()) {
                enrollmentData = objectMapper.createObjectNode();
            } else {
                enrollmentData = (ObjectNode) objectMapper.readTree(enrollment.getEnrollmentData());
            }

            // 기존 출석 문자열 가져오기
            String attendanceStr = enrollmentData.has("attendance") ? 
                    enrollmentData.get("attendance").asText() : "";

            // Map으로 변환 후 업데이트
            Map<Integer, String> attendanceMap = parseAttendanceString(attendanceStr);
            attendanceMap.put(sessionNumber, status);

            // 다시 문자열로 변환
            String updatedAttendanceStr = buildAttendanceString(attendanceMap);

            // 출석률 계산
            String attendanceRate = calculateAttendanceRate(updatedAttendanceStr, 80);

            // JSON 업데이트
            enrollmentData.put("attendance", updatedAttendanceStr);
            enrollmentData.put("attendanceRate", attendanceRate);

            // 저장
            enrollment.setEnrollmentData(objectMapper.writeValueAsString(enrollmentData));
            enrollment = enrollmentRepository.save(enrollment);

            log.info("출석 기록 완료: enrollmentIdx={}, session={}, status={}, rate={}", 
                    enrollmentIdx, sessionNumber, status, attendanceRate);

            return enrollment;

        } catch (Exception e) {
            log.error("출석 기록 실패: enrollmentIdx={}", enrollmentIdx, e);
            throw new RuntimeException("출석 기록 중 오류가 발생했습니다.", e);
        }
    }

    /**
     * 사유 신청 (학생)
     * 
     * @param enrollmentIdx 수강신청 IDX
     * @param sessionNumber 회차 번호
     * @param reason 신청 사유
     * @return 생성된 요청
     */
    @Transactional
    public AttendanceRequestTbl requestExcuse(Integer enrollmentIdx, 
                                               Integer sessionNumber, 
                                               String reason) {
        // 이미 요청했는지 확인
        if (requestRepository.existsByEnrollmentIdxAndSessionNumber(enrollmentIdx, sessionNumber)) {
            throw new RuntimeException("해당 회차에 대한 사유 신청이 이미 존재합니다.");
        }

        // 수강신청 존재 여부 확인
        if (!enrollmentRepository.existsById(enrollmentIdx)) {
            throw new RuntimeException("수강신청을 찾을 수 없습니다.");
        }

        // 요청 생성
        AttendanceRequestTbl request = new AttendanceRequestTbl(
                enrollmentIdx, sessionNumber, reason);

        request = requestRepository.save(request);

        log.info("사유 신청 완료: enrollmentIdx={}, session={}", enrollmentIdx, sessionNumber);

        return request;
    }

    /**
     * 사유 승인 (교수) - 승인 시 "출석"으로 처리
     * 
     * @param requestIdx 요청 IDX
     * @param professorIdx 교수 IDX
     * @return 업데이트된 수강신청
     */
    @Transactional
    public EnrollmentExtendedTbl approveRequest(Long requestIdx, Integer professorIdx) {
        AttendanceRequestTbl request = requestRepository.findById(requestIdx)
                .orElseThrow(() -> new RuntimeException("요청을 찾을 수 없습니다."));

        if (!request.isPending()) {
            throw new RuntimeException("대기 중인 요청만 승인할 수 있습니다.");
        }

        // 승인 처리
        request.approve(professorIdx);
        requestRepository.save(request);

        // 출석 상태를 "결" → "출"로 변경 (순수하게 출석으로 인정)
        EnrollmentExtendedTbl enrollment = enrollmentRepository.findById(request.getEnrollmentIdx())
                .orElseThrow(() -> new RuntimeException("수강신청을 찾을 수 없습니다."));

        try {
            ObjectNode enrollmentData = (ObjectNode) objectMapper.readTree(enrollment.getEnrollmentData());
            String attendanceStr = enrollmentData.has("attendance") ? 
                    enrollmentData.get("attendance").asText() : "";

            Map<Integer, String> attendanceMap = parseAttendanceString(attendanceStr);
            
            // "결"이면 "출"로 변경, 없으면 "출" 추가
            attendanceMap.put(request.getSessionNumber(), STATUS_PRESENT);

            String updatedAttendanceStr = buildAttendanceString(attendanceMap);
            String attendanceRate = calculateAttendanceRate(updatedAttendanceStr, 80);

            enrollmentData.put("attendance", updatedAttendanceStr);
            enrollmentData.put("attendanceRate", attendanceRate);

            enrollment.setEnrollmentData(objectMapper.writeValueAsString(enrollmentData));
            enrollment = enrollmentRepository.save(enrollment);

            log.info("출석 인정 완료: requestIdx={}, session={} -> 출석 처리", 
                    requestIdx, request.getSessionNumber());

            return enrollment;

        } catch (Exception e) {
            log.error("출석 인정 실패: requestIdx={}", requestIdx, e);
            throw new RuntimeException("출석 인정 중 오류가 발생했습니다.", e);
        }
    }

    /**
     * 사유 반려 (교수)
     * 
     * @param requestIdx 요청 IDX
     * @param professorIdx 교수 IDX
     * @param rejectReason 반려 사유
     * @return 반려된 요청
     */
    @Transactional
    public AttendanceRequestTbl rejectRequest(Long requestIdx, 
                                               Integer professorIdx, 
                                               String rejectReason) {
        AttendanceRequestTbl request = requestRepository.findById(requestIdx)
                .orElseThrow(() -> new RuntimeException("요청을 찾을 수 없습니다."));

        if (!request.isPending()) {
            throw new RuntimeException("대기 중인 요청만 반려할 수 있습니다.");
        }

        // 반려 처리
        request.reject(professorIdx, rejectReason);
        request = requestRepository.save(request);

        log.info("사유 반려 완료: requestIdx={}, reason={}", requestIdx, rejectReason);

        return request;
    }

    /**
     * 강의별 대기 중인 요청 조회 (교수용)
     */
    public Page<AttendanceRequestTbl> getPendingRequestsByLecture(Integer lecIdx, Pageable pageable) {
        return requestRepository.findPendingRequestsByLecture(lecIdx, pageable);
    }

    /**
     * 강의별 모든 요청 조회 (교수용)
     */
    public Page<AttendanceRequestTbl> getAllRequestsByLecture(Integer lecIdx, Pageable pageable) {
        return requestRepository.findAllRequestsByLecture(lecIdx, pageable);
    }

    /**
     * 학생의 요청 목록 조회
     */
    public List<AttendanceRequestTbl> getStudentRequests(Integer enrollmentIdx) {
        return requestRepository.findByEnrollmentIdx(enrollmentIdx);
    }

    /**
     * 대기 중인 요청 개수 조회
     */
    public long countPendingRequests(Integer lecIdx) {
        return requestRepository.countPendingRequestsByLecture(lecIdx);
    }

    // ========================================
    // 성적 계산용 메서드 (GradeCalculationService에서 사용)
    // ========================================

    /**
     * 학생의 출석 점수 계산 (성적 관리용)
     * 
     * @param lecIdx 강의 IDX
     * @param studentIdx 학생 IDX
     * @return Map {maxScore: 20.0, currentScore: 18.5, percentage: 92.50}
     */
    public Map<String, Object> calculateAttendanceScoreForGrade(Integer lecIdx, Integer studentIdx) {
        try {
            // 수강신청 정보 조회
            EnrollmentExtendedTbl enrollment = enrollmentRepository
                .findByStudentIdxAndLecIdx(studentIdx, lecIdx)
                .orElseThrow(() -> new IllegalArgumentException("수강 정보를 찾을 수 없습니다."));

            // ENROLLMENT_DATA JSON 파싱
            ObjectNode enrollmentData;
            if (enrollment.getEnrollmentData() == null || enrollment.getEnrollmentData().isEmpty()) {
                // 출석 데이터가 없으면 0점 반환
                return Map.of(
                    "maxScore", 20.0,
                    "currentScore", 0.0,
                    "percentage", 0.00
                );
            }

            enrollmentData = (ObjectNode) objectMapper.readTree(enrollment.getEnrollmentData());
            String attendanceStr = enrollmentData.has("attendance") ? 
                    enrollmentData.get("attendance").asText() : "";

            if (attendanceStr == null || attendanceStr.isEmpty()) {
                // 출석 데이터가 없으면 0점 반환
                return Map.of(
                    "maxScore", 20.0,
                    "currentScore", 0.0,
                    "percentage", 0.00
                );
            }

            // 출석 문자열 파싱
            Map<Integer, String> attendanceMap = parseAttendanceString(attendanceStr);

            // 출석율 계산 (출석률 = 출석+지각 / 총 회차)
            // 출석(출): 출석으로 인정
            // 지각(지): 출석으로 인정 (출석율 계산 시)
            // 결석(결): 결석
            int presentCount = 0;  // 출석 수
            int lateCount = 0;     // 지각 수
            int absentCount = 0;   // 결석 수
            
            for (String status : attendanceMap.values()) {
                if (STATUS_PRESENT.equals(status)) {
                    presentCount++;
                } else if (STATUS_LATE.equals(status)) {
                    lateCount++;
                } else if (STATUS_ABSENT.equals(status)) {
                    absentCount++;
                }
            }
            
            int attendanceCount = presentCount + lateCount;  // 출석율 = 출석 + 지각

            // 최대 점수: 80회차 = 20점 만점
            double maxScore = 20.0;
            
            // 출석율 기반 점수 계산 (80회 기준)
            // 예: 77회 출석(출석+지각) / 80 * 20 = 19.25점
            // 지각에 대한 감점은 GradeCalculationService에서 교수 설정에 따라 처리
            double currentScore = ((double) attendanceCount / 80.0) * maxScore;
            
            // 백분율 계산 (0-100 범위, 소수점 셋째자리에서 반올림하여 둘째자리까지)
            double percentage = (currentScore / maxScore) * 100.0;
            percentage = Math.round(percentage * 100.0) / 100.0;  // 소수점 둘째자리 반올림
            
            // currentScore도 소수점 둘째자리 반올림
            currentScore = Math.round(currentScore * 100.0) / 100.0;

            log.debug("출석 점수 계산: lecIdx={}, studentIdx={}, 출석문자열={}, 출석={}회, 지각={}회, 결석={}회, currentScore={}, percentage={}%", 
                    lecIdx, studentIdx, attendanceStr, presentCount, lateCount, absentCount, currentScore, percentage);

            return Map.of(
                "maxScore", maxScore,
                "currentScore", currentScore,
                "percentage", percentage,
                "presentCount", presentCount,    // 출석 수
                "lateCount", lateCount,          // 지각 수
                "absentCount", absentCount,      // 결석 수
                "attendanceRate", attendanceCount  // 출석율 (출석+지각)
            );

        } catch (Exception e) {
            log.error("출석 점수 계산 실패: lecIdx={}, studentIdx={}", lecIdx, studentIdx, e);
            throw new RuntimeException("출석 점수 계산 중 오류가 발생했습니다.", e);
        }
    }
}
