package BlueCrab.com.example.dto.Lecture.Certificate;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 성적확인서 전체 정보를 담는 응답 DTO
 * 학생의 전체 성적 이력과 통계를 포함
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TranscriptResponse {
    
    /**
     * 학생 정보
     */
    private StudentInfo student;
    
    /**
     * 성적 레코드 목록 (학기별로 정렬됨)
     */
    private List<GradeRecord> gradeRecords;
    
    /**
     * 학기별 성적 통계
     */
    private Map<String, SemesterSummary> semesterSummaries;
    
    /**
     * 전체 성적 통계
     */
    private OverallSummary overallSummary;
    
    /**
     * 성적확인서 발급 시각
     */
    private LocalDateTime issuedAt;
    
    /**
     * 성적확인서 발급 번호
     */
    private String certificateNumber;
    
    /**
     * 학생 기본 정보
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StudentInfo {
        /**
         * 학생 IDX
         */
        private Integer studentIdx;
        
        /**
         * 학번
         */
        private String studentCode;
        
        /**
         * 이름
         */
        private String name;
        
        /**
         * 학과 코드
         */
        private String departmentCode;
        
        /**
         * 학과명
         */
        private String departmentName;
        
        /**
         * 학년
         */
        private Integer grade;
        
        /**
         * 입학년도
         */
        private Integer admissionYear;
    }
    
    /**
     * 학기별 성적 요약
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SemesterSummary {
        /**
         * 학기 식별자 (예: "2025-1", "2025-2")
         */
        private String semesterKey;
        
        /**
         * 년도
         */
        private Integer year;
        
        /**
         * 학기 (1 or 2)
         */
        private Integer semester;
        
        /**
         * 수강 과목 수
         */
        private Integer courseCount;
        
        /**
         * 취득 학점 (F 제외)
         */
        private Integer earnedCredits;
        
        /**
         * 신청 학점 (전체)
         */
        private Integer attemptedCredits;
        
        /**
         * 학기 평균 백분율
         */
        private BigDecimal averagePercentage;
        
        /**
         * 학기 평균 등급 (4.5 만점)
         */
        private BigDecimal semesterGpa;
        
        /**
         * A 등급 과목 수
         */
        private Integer gradeACount;
        
        /**
         * B 등급 과목 수
         */
        private Integer gradeBCount;
        
        /**
         * C 등급 과목 수
         */
        private Integer gradeCCount;
        
        /**
         * D 등급 과목 수
         */
        private Integer gradeDCount;
        
        /**
         * F 등급 과목 수
         */
        private Integer gradeFCount;
    }
    
    /**
     * 전체 성적 요약
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OverallSummary {
        /**
         * 총 수강 과목 수
         */
        private Integer totalCourses;
        
        /**
         * 총 취득 학점 (F 제외)
         */
        private Integer totalEarnedCredits;
        
        /**
         * 총 신청 학점
         */
        private Integer totalAttemptedCredits;
        
        /**
         * 전체 평균 평점 (4.5 만점)
         */
        private BigDecimal cumulativeGpa;
        
        /**
         * 전체 평균 백분율
         */
        private BigDecimal averagePercentage;
        
        /**
         * 졸업 요구 학점
         */
        private Integer requiredCredits;
        
        /**
         * 졸업까지 남은 학점
         */
        private Integer remainingCredits;
        
        /**
         * 학점 취득률 (%)
         */
        private BigDecimal completionRate;
        
        /**
         * 전체 A 등급 과목 수
         */
        private Integer totalGradeACount;
        
        /**
         * 전체 B 등급 과목 수
         */
        private Integer totalGradeBCount;
        
        /**
         * 전체 C 등급 과목 수
         */
        private Integer totalGradeCCount;
        
        /**
         * 전체 D 등급 과목 수
         */
        private Integer totalGradeDCount;
        
        /**
         * 전체 F 등급 과목 수
         */
        private Integer totalGradeFCount;
        
        /**
         * 석차 (동일 학년 내)
         */
        private Integer rank;
        
        /**
         * 전체 학생 수 (동일 학년)
         */
        private Integer totalStudents;
        
        /**
         * 석차 백분위 (상위 몇 %)
         */
        private BigDecimal rankPercentile;
    }
}
