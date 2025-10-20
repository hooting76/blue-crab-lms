// 작성자: 성태준
// 최종 등급 배정 처리 클래스

package BlueCrab.com.example.service.Lecture;

import BlueCrab.com.example.entity.Lecture.EnrollmentExtendedTbl;
import BlueCrab.com.example.repository.Lecture.EnrollmentExtendedTblRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 최종 등급 배정 처리 클래스
 * - 60% 기준 합격/불합격 분류
 * - 하위 침범 방식 등급 배정
 * - 동점자 처리
 */
public class GradeFinalizer {

    private final List<EnrollmentExtendedTbl> allEnrollments;
    private final double passingThreshold;
    private final Map<String, Integer> gradeDistribution;
    private final EnrollmentExtendedTblRepository enrollmentRepository;
    private final ObjectMapper objectMapper;

    public GradeFinalizer(List<EnrollmentExtendedTbl> allEnrollments,
                          double passingThreshold,
                          Map<String, Integer> gradeDistribution,
                          EnrollmentExtendedTblRepository enrollmentRepository,
                          ObjectMapper objectMapper) {
        this.allEnrollments = allEnrollments;
        this.passingThreshold = passingThreshold;
        this.gradeDistribution = gradeDistribution;
        this.enrollmentRepository = enrollmentRepository;
        this.objectMapper = objectMapper;
    }

    /**
     * 등급 배정 실행
     */
    public Map<String, Object> execute() {
        try {
            // 1. 학생 성적 정보 수집
            List<StudentGradeInfo> allGrades = collectGradeInfo();

            // 2. 합격/불합격 분류
            List<StudentGradeInfo> passingStudents = new java.util.ArrayList<>();
            List<StudentGradeInfo> failingStudents = new java.util.ArrayList<>();

            for (StudentGradeInfo grade : allGrades) {
                if (grade.percentage >= passingThreshold) {
                    passingStudents.add(grade);
                } else {
                    failingStudents.add(grade);
                    grade.letterGrade = "F";
                }
            }

            // 3. 등급 배정
            int totalStudents = allGrades.size();
            assignGrades(passingStudents, failingStudents.size(), totalStudents);

            // 4. ENROLLMENT_DATA 업데이트
            updateEnrollmentData(allGrades, totalStudents, passingStudents.size());

            // 5. 통계 생성
            return createStatistics(allGrades, passingStudents.size(), failingStudents.size());

        } catch (Exception e) {
            throw new RuntimeException("등급 배정 처리 실패: " + e.getMessage(), e);
        }
    }

    /**
     * 학생 성적 정보 수집
     */
    private List<StudentGradeInfo> collectGradeInfo() {
        List<StudentGradeInfo> grades = new java.util.ArrayList<>();

        for (EnrollmentExtendedTbl enrollment : allEnrollments) {
            Map<String, Object> enrollmentData = parseEnrollmentData(enrollment.getEnrollmentData());
            @SuppressWarnings("unchecked")
            Map<String, Object> gradeData = (Map<String, Object>) enrollmentData.getOrDefault("grade", new HashMap<>());

            StudentGradeInfo gradeInfo = new StudentGradeInfo();
            gradeInfo.enrollment = enrollment;
            gradeInfo.studentIdx = enrollment.getStudentIdx();

            // 백분율 추출
            if (gradeData.containsKey("total")) {
                @SuppressWarnings("unchecked")
                Map<String, Object> totalData = (Map<String, Object>) gradeData.get("total");
                gradeInfo.percentage = totalData.containsKey("percentage")
                    ? ((Number) totalData.get("percentage")).doubleValue()
                    : 0.0;
            } else {
                gradeInfo.percentage = 0.0;
            }

            grades.add(gradeInfo);
        }

        return grades;
    }

    /**
     * 등급 배정 (하위 침범 방식 + 동점자 처리)
     */
    private void assignGrades(List<StudentGradeInfo> passingStudents, int failingCount, int totalStudents) {
        if (passingStudents.isEmpty()) {
            return;
        }

        // 기본 비율 계산
        int originalA = (int) Math.ceil(totalStudents * gradeDistribution.getOrDefault("A", 30) / 100.0);
        int originalB = (int) Math.ceil(totalStudents * gradeDistribution.getOrDefault("B", 40) / 100.0);
        int originalC = (int) Math.ceil(totalStudents * gradeDistribution.getOrDefault("C", 20) / 100.0);
        int originalD = (int) Math.ceil(totalStudents * gradeDistribution.getOrDefault("D", 10) / 100.0);

        // 낙제자가 하위 등급부터 차지
        int[] actualGrades = calculateActualGrades(originalA, originalB, originalC, originalD, failingCount);

        // 합격자 정렬
        passingStudents.sort((a, b) -> Double.compare(b.percentage, a.percentage));

        // 등급 배정
        int currentIndex = 0;
        int currentRank = 1;

        currentIndex = assignGradeToGroup(passingStudents, currentIndex, actualGrades[0], "A", currentRank);
        currentRank = currentIndex + 1;

        currentIndex = assignGradeToGroup(passingStudents, currentIndex, actualGrades[1], "B", currentRank);
        currentRank = currentIndex + 1;

        currentIndex = assignGradeToGroup(passingStudents, currentIndex, actualGrades[2], "C", currentRank);
        currentRank = currentIndex + 1;

        currentIndex = assignGradeToGroup(passingStudents, currentIndex, actualGrades[3], "D", currentRank);

        // 남은 학생들은 A등급
        while (currentIndex < passingStudents.size()) {
            passingStudents.get(currentIndex).letterGrade = "A";
            passingStudents.get(currentIndex).rank = currentIndex + 1;
            currentIndex++;
        }
    }

    /**
     * 실제 등급 인원 계산 (하위 침범 방식)
     */
    private int[] calculateActualGrades(int originalA, int originalB, int originalC, int originalD, int failingCount) {
        int actualA = originalA;
        int actualB = originalB;
        int actualC = originalC;
        int actualD = originalD;

        int remainingFailing = failingCount;

        if (remainingFailing >= originalD) {
            actualD = 0;
            remainingFailing -= originalD;

            if (remainingFailing >= originalC) {
                actualC = 0;
                remainingFailing -= originalC;

                if (remainingFailing >= originalB) {
                    actualB = 0;
                    remainingFailing -= originalB;

                    if (remainingFailing > 0) {
                        actualA = Math.max(0, originalA - remainingFailing);
                    }
                } else {
                    actualB = originalB - remainingFailing;
                }
            } else {
                actualC = originalC - remainingFailing;
            }
        } else {
            actualD = originalD - remainingFailing;
        }

        return new int[]{actualA, actualB, actualC, actualD};
    }

    /**
     * 등급 그룹에 동점자를 포함하여 배정
     */
    private int assignGradeToGroup(List<StudentGradeInfo> students, int startIndex,
                                   int targetCount, String grade, int startRank) {
        if (targetCount <= 0 || startIndex >= students.size()) {
            return startIndex;
        }

        int currentIndex = startIndex;
        int assignedCount = 0;

        while (currentIndex < students.size() && assignedCount < targetCount) {
            double currentScore = students.get(currentIndex).percentage;
            int currentGroup = currentIndex;

            // 같은 점수를 가진 학생들을 모두 찾기
            while (currentGroup < students.size() &&
                   students.get(currentGroup).percentage == currentScore) {
                students.get(currentGroup).letterGrade = grade;
                students.get(currentGroup).rank = startRank;
                currentGroup++;
                assignedCount++;
            }

            currentIndex = currentGroup;
        }

        return currentIndex;
    }

    /**
     * ENROLLMENT_DATA 업데이트
     */
    private void updateEnrollmentData(List<StudentGradeInfo> allGrades, int totalStudents, int passingStudents) {
        for (StudentGradeInfo gradeInfo : allGrades) {
            try {
                Map<String, Object> enrollmentData = parseEnrollmentData(gradeInfo.enrollment.getEnrollmentData());
                @SuppressWarnings("unchecked")
                Map<String, Object> gradeData = (Map<String, Object>) enrollmentData
                    .computeIfAbsent("grade", k -> new HashMap<>());

                gradeData.put("letterGrade", gradeInfo.letterGrade);
                gradeData.put("finalized", true);
                gradeData.put("finalizedDate", getCurrentDateTime());
                gradeData.put("rank", gradeInfo.rank);
                gradeData.put("totalStudents", totalStudents);
                gradeData.put("passingStudents", passingStudents);

                String updatedJson = objectMapper.writeValueAsString(enrollmentData);
                gradeInfo.enrollment.setEnrollmentData(updatedJson);
                enrollmentRepository.save(gradeInfo.enrollment);

            } catch (JsonProcessingException e) {
                throw new RuntimeException("등급 업데이트 실패: " + e.getMessage(), e);
            }
        }
    }

    /**
     * 통계 생성
     */
    private Map<String, Object> createStatistics(List<StudentGradeInfo> allGrades, int passingCount, int failingCount) {
        Map<String, Integer> gradeStats = new HashMap<>();
        gradeStats.put("A", 0);
        gradeStats.put("B", 0);
        gradeStats.put("C", 0);
        gradeStats.put("D", 0);
        gradeStats.put("F", 0);

        for (StudentGradeInfo grade : allGrades) {
            String letterGrade = grade.letterGrade != null ? grade.letterGrade : "F";
            gradeStats.put(letterGrade, gradeStats.getOrDefault(letterGrade, 0) + 1);
        }

        double averageScore = allGrades.stream()
            .mapToDouble(g -> g.percentage)
            .average()
            .orElse(0.0);

        return Map.of(
            "success", true,
            "message", "최종 등급 배정이 완료되었습니다.",
            "data", Map.of(
                "gradeStats", gradeStats,
                "totalStudents", allGrades.size(),
                "passingStudents", passingCount,
                "failingStudents", failingCount,
                "averageScore", Math.round(averageScore * 100.0) / 100.0,
                "passingThreshold", passingThreshold
            )
        );
    }

    /**
     * JSON 파싱
     */
    private Map<String, Object> parseEnrollmentData(String enrollmentJson) {
        if (enrollmentJson == null || enrollmentJson.trim().isEmpty() || "{}".equals(enrollmentJson)) {
            return new HashMap<>();
        }
        try {
            return objectMapper.readValue(enrollmentJson, new TypeReference<Map<String, Object>>() {});
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("enrollmentData 파싱 실패", e);
        }
    }

    /**
     * 현재 날짜/시간 문자열
     */
    private String getCurrentDateTime() {
        return LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }

    /**
     * 학생 성적 정보 내부 클래스
     */
    static class StudentGradeInfo {
        EnrollmentExtendedTbl enrollment;
        Integer studentIdx;
        Double percentage;
        String letterGrade;
        int rank;
    }
}
