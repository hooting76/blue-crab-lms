package BlueCrab.com.example.dto.Lecture;

/**
 * 과제 통계 정보 전송을 위한 DTO 클래스
 * 과제 제출 현황 통계 조회 시 사용
 */
public class AssignmentStatisticsDto {

    private Integer totalStudents;      // 전체 수강생 수
    private Integer submittedCount;     // 제출 완료 수
    private Integer gradedCount;        // 채점 완료 수
    private Double averageScore;        // 평균 점수
    private Double submissionRate;      // 제출률 (%)

    public AssignmentStatisticsDto() {}

    public AssignmentStatisticsDto(Integer totalStudents, Integer submittedCount, Integer gradedCount,
                                   Double averageScore, Double submissionRate) {
        this.totalStudents = totalStudents;
        this.submittedCount = submittedCount;
        this.gradedCount = gradedCount;
        this.averageScore = averageScore;
        this.submissionRate = submissionRate;
    }

    // Getters and Setters

    public Integer getTotalStudents() {
        return totalStudents;
    }

    public void setTotalStudents(Integer totalStudents) {
        this.totalStudents = totalStudents;
    }

    public Integer getSubmittedCount() {
        return submittedCount;
    }

    public void setSubmittedCount(Integer submittedCount) {
        this.submittedCount = submittedCount;
    }

    public Integer getGradedCount() {
        return gradedCount;
    }

    public void setGradedCount(Integer gradedCount) {
        this.gradedCount = gradedCount;
    }

    public Double getAverageScore() {
        return averageScore;
    }

    public void setAverageScore(Double averageScore) {
        this.averageScore = averageScore;
    }

    public Double getSubmissionRate() {
        return submissionRate;
    }

    public void setSubmissionRate(Double submissionRate) {
        this.submissionRate = submissionRate;
    }
}
