// 작성자: 성태준
// 성적 계산 서비스

package BlueCrab.com.example.service.Lecture;

import BlueCrab.com.example.entity.Lecture.EnrollmentExtendedTbl;
import BlueCrab.com.example.repository.Lecture.EnrollmentExtendedTblRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 성적 계산 서비스
 * - 출석 점수 계산
 * - 과제 점수 집계
 * - 총점 및 백분율 계산
 * - ENROLLMENT_DATA JSON 업데이트
 */
@Service
@Transactional(readOnly = true)
public class GradeCalculationService {

    @Autowired
    private EnrollmentExtendedTblRepository enrollmentRepository;

    @Autowired
    private AttendanceService attendanceService;

    @Autowired
    private AssignmentService assignmentService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 학생 성적 정보 조회
     * - 출석 점수 계산
     * - 과제 점수 집계
     * - 총점 및 백분율 계산
     * - ENROLLMENT_DATA JSON 업데이트
     */
    @Transactional
    public Map<String, Object> calculateStudentGrade(Integer lecIdx, Integer studentIdx) {
        try {
            // 1. 수강신청 정보 조회
            EnrollmentExtendedTbl enrollment = enrollmentRepository
                .findByStudentIdxAndLecIdx(studentIdx, lecIdx)
                .orElseThrow(() -> new IllegalArgumentException("수강신청 정보를 찾을 수 없습니다."));

            // 2. 기존 JSON 데이터 파싱
            Map<String, Object> enrollmentData = parseEnrollmentData(enrollment.getEnrollmentData());
            @SuppressWarnings("unchecked")
            Map<String, Object> gradeData = (Map<String, Object>) enrollmentData
                .computeIfAbsent("grade", k -> new HashMap<>());

            // 3. 성적 구성 설정 조회 (지각 감점 설정 확인)
            @SuppressWarnings("unchecked")
            Map<String, Object> gradeConfig = (Map<String, Object>) enrollmentData.get("gradeConfig");
            double latePenaltyPerSession = 0.0;
            if (gradeConfig != null && gradeConfig.containsKey("latePenaltyPerSession")) {
                latePenaltyPerSession = ((Number) gradeConfig.get("latePenaltyPerSession")).doubleValue();
            }

            // 4. 출석 점수 계산
            Map<String, Object> attendanceData = calculateAttendanceScore(lecIdx, studentIdx);
            
            // 5. 지각 감점 적용 (교수 재량)
            if (latePenaltyPerSession > 0.0) {
                int lateCount = ((Number) attendanceData.get("lateCount")).intValue();
                double currentScore = ((Number) attendanceData.get("currentScore")).doubleValue();
                double penalty = lateCount * latePenaltyPerSession;
                double adjustedScore = Math.max(0.0, currentScore - penalty);  // 0점 이하로 내려가지 않음
                
                // 조정된 점수로 업데이트
                adjustedScore = Math.round(adjustedScore * 100.0) / 100.0;
                attendanceData.put("currentScore", adjustedScore);
                attendanceData.put("latePenalty", penalty);  // 감점 기록
                
                // 백분율 재계산
                double maxScore = ((Number) attendanceData.get("maxScore")).doubleValue();
                double percentage = (adjustedScore / maxScore) * 100.0;
                percentage = Math.round(percentage * 100.0) / 100.0;
                attendanceData.put("percentage", percentage);
            }
            
            gradeData.put("attendance", attendanceData);

            // 6. 과제 점수 집계
            List<Map<String, Object>> assignmentScores = calculateAssignmentScores(lecIdx, studentIdx);
            gradeData.put("assignments", assignmentScores);

            // 7. 총점 계산
            Map<String, Object> totalData = calculateTotalScore(attendanceData, assignmentScores);
            gradeData.put("total", totalData);

            // 8. JSON 데이터 업데이트
            String updatedJson = objectMapper.writeValueAsString(enrollmentData);
            enrollment.setEnrollmentData(updatedJson);
            enrollmentRepository.save(enrollment);

            // 9. 응답 데이터 구성
            return Map.of(
                "lecIdx", lecIdx,
                "studentIdx", studentIdx,
                "enrollmentIdx", enrollment.getEnrollmentIdx(),  // enrollmentIdx 추가
                "grade", gradeData,
                "updatedAt", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
            );

        } catch (JsonProcessingException e) {
            throw new RuntimeException("JSON 처리 중 오류가 발생했습니다.", e);
        } catch (Exception e) {
            throw new RuntimeException("학생 성적 정보 조회 중 오류가 발생했습니다.", e);
        }
    }

    /**
     * 출석 점수 계산
     * AttendanceService를 통해 실제 출석 데이터 조회
     */
    public Map<String, Object> calculateAttendanceScore(Integer lecIdx, Integer studentIdx) {
        return attendanceService.calculateAttendanceScoreForGrade(lecIdx, studentIdx);
    }

    /**
     * 과제 점수 집계
     * AssignmentService를 통해 실제 과제 데이터 조회
     */
    public List<Map<String, Object>> calculateAssignmentScores(Integer lecIdx, Integer studentIdx) {
        return assignmentService.getStudentAssignmentScoresForGrade(lecIdx, studentIdx);
    }

    /**
     * 총점 계산
     * 출석 점수 + 과제 점수들의 합계
     * 백분율은 0-100 범위, 소수점 둘째자리까지 반올림
     */
    public Map<String, Object> calculateTotalScore(Map<String, Object> attendanceData,
                                                    List<Map<String, Object>> assignmentScores) {
        // 출석 점수 (null 안전 처리)
        double attendanceScore = attendanceData != null && attendanceData.get("currentScore") != null
            ? ((Number) attendanceData.get("currentScore")).doubleValue()
            : 0.0;

        // 과제 점수 합계
        double assignmentScore = assignmentScores != null
            ? assignmentScores.stream()
                .mapToDouble(a -> ((Number) a.get("score")).doubleValue())
                .sum()
            : 0.0;

        // 총 만점 (null 안전 처리)
        double attendanceMax = attendanceData != null && attendanceData.get("maxScore") != null
            ? ((Number) attendanceData.get("maxScore")).doubleValue()
            : 0.0;
        double assignmentMax = assignmentScores != null
            ? assignmentScores.stream()
                .mapToDouble(a -> ((Number) a.get("maxScore")).doubleValue())
                .sum()
            : 0.0;

        // 합계
        double totalScore = attendanceScore + assignmentScore;
        double totalMax = attendanceMax + assignmentMax;
        
        // 백분율 계산 (0-100 범위, 소수점 셋째자리에서 반올림하여 둘째자리까지)
        double percentage = totalMax > 0 ? (totalScore / totalMax) * 100.0 : 0.0;
        percentage = Math.round(percentage * 100.0) / 100.0;  // 소수점 둘째자리 반올림
        
        // score들도 소수점 둘째자리 반올림
        totalScore = Math.round(totalScore * 100.0) / 100.0;
        totalMax = Math.round(totalMax * 100.0) / 100.0;

        return Map.of(
            "score", totalScore,
            "maxScore", totalMax,
            "percentage", percentage
        );
    }

    /**
     * enrollmentData JSON 파싱
     */
    private Map<String, Object> parseEnrollmentData(String enrollmentJson) {
        if (enrollmentJson == null || enrollmentJson.trim().isEmpty() || "{}".equals(enrollmentJson)) {
            return new HashMap<>();
        }
        try {
            return objectMapper.readValue(enrollmentJson, 
                new com.fasterxml.jackson.core.type.TypeReference<Map<String, Object>>() {});
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("enrollmentData 파싱 실패", e);
        }
    }
}
