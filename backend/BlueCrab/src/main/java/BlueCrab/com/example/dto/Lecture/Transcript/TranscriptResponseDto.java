package BlueCrab.com.example.dto.Lecture.Transcript;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 성적확인서 응답 DTO
 * 학생의 전체 성적 이력을 포함
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TranscriptResponseDto {
    
    /**
     * 학생 기본 정보
     */
    private StudentInfo student;
    
    /**
     * 수강 과목 목록
     */
    private List<CourseDto> courses;
    
    /**
     * 학기별 통계
     * Key: "2025-1" (년도-학기)
     */
    private Map<String, SemesterSummary> semesterSummaries;
    
    /**
     * 전체 통계
     */
    private OverallSummary overallSummary;
    
    /**
     * 발급 시각
     */
    private LocalDateTime issuedAt;
    
    /**
     * 성적확인서 발급 번호
     * 형식: TR-{학번}-{YYYYMMDDHHMMSS}
     */
    private String certificateNumber;
    
    /**
     * 학생 정보
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StudentInfo {
        private Integer studentIdx;
        private String studentCode;  // 학번
        private String name;
        private Integer admissionYear;  // 입학년도
        private Integer currentGrade;   // 현재 학년
    }
    
    /**
     * 학기별 요약 통계
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SemesterSummary {
        private String semesterKey;      // "2025-1"
        private Integer year;
        private Integer semester;
        private Integer courseCount;     // 수강 과목 수
        private Integer earnedCredits;   // 취득 학점 (COMPLETED만)
        private Integer attemptedCredits; // 신청 학점 (전체)
        private Double semesterGpa;      // 학기 평점 (4.0 만점)
        private Double averagePercentage; // 평균 백분율
        
        // 등급별 과목 수
        private Integer gradeACount;
        private Integer gradeBCount;
        private Integer gradeCCount;
        private Integer gradeDCount;
        private Integer gradeFCount;
    }
    
    /**
     * 전체 요약 통계
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OverallSummary {
        private Integer totalCourses;          // 총 수강 과목 수
        private Integer totalEarnedCredits;    // 총 취득 학점
        private Integer totalAttemptedCredits; // 총 신청 학점
        private Double cumulativeGpa;          // 누적 평점 (4.0 만점)
        private Double averagePercentage;      // 전체 평균 백분율
        private Double completionRate;         // 학점 취득률 (%)
        
        // 등급별 과목 수
        private Integer totalGradeACount;
        private Integer totalGradeBCount;
        private Integer totalGradeCCount;
        private Integer totalGradeDCount;
        private Integer totalGradeFCount;
    }
}
