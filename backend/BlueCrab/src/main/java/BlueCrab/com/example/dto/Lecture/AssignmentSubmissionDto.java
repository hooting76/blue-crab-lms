package BlueCrab.com.example.dto.Lecture;

/**
 * 과제 제출 정보 전송을 위한 DTO 클래스
 * 과제 제출 내역 조회, 제출 시 사용
 */
public class AssignmentSubmissionDto {

    private Integer submissionIdx;
    private Integer studentIdx;
    private String studentName;
    private String studentCode;
    private String content;             // 과제 설명/내용
    private String filePath;            // 제출 파일 경로
    private String submittedAt;         // 제출 일시
    private Double score;               // 점수
    private String feedback;            // 피드백
    private String gradedAt;            // 채점 일시
    private String status;              // 제출 상태 (SUBMITTED, GRADED, LATE)

    public AssignmentSubmissionDto() {}

    public AssignmentSubmissionDto(Integer submissionIdx, Integer studentIdx, String studentName,
                                   String studentCode, String content, String filePath,
                                   String submittedAt, String status) {
        this.submissionIdx = submissionIdx;
        this.studentIdx = studentIdx;
        this.studentName = studentName;
        this.studentCode = studentCode;
        this.content = content;
        this.filePath = filePath;
        this.submittedAt = submittedAt;
        this.status = status;
    }

    // Getters and Setters

    public Integer getSubmissionIdx() {
        return submissionIdx;
    }

    public void setSubmissionIdx(Integer submissionIdx) {
        this.submissionIdx = submissionIdx;
    }

    public Integer getStudentIdx() {
        return studentIdx;
    }

    public void setStudentIdx(Integer studentIdx) {
        this.studentIdx = studentIdx;
    }

    public String getStudentName() {
        return studentName;
    }

    public void setStudentName(String studentName) {
        this.studentName = studentName;
    }

    public String getStudentCode() {
        return studentCode;
    }

    public void setStudentCode(String studentCode) {
        this.studentCode = studentCode;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public String getSubmittedAt() {
        return submittedAt;
    }

    public void setSubmittedAt(String submittedAt) {
        this.submittedAt = submittedAt;
    }

    public Double getScore() {
        return score;
    }

    public void setScore(Double score) {
        this.score = score;
    }

    public String getFeedback() {
        return feedback;
    }

    public void setFeedback(String feedback) {
        this.feedback = feedback;
    }

    public String getGradedAt() {
        return gradedAt;
    }

    public void setGradedAt(String gradedAt) {
        this.gradedAt = gradedAt;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
