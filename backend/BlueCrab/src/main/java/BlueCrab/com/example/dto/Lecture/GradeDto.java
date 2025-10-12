// 작성자: 성태준

package BlueCrab.com.example.dto.Lecture;

/**
 * 성적 정보 전송을 위한 DTO 클래스
 * 성적 조회, 성적 입력 시 사용
 */
public class GradeDto {

    private Double midterm;             // 중간고사 점수
    private Double finalExam;           // 기말고사 점수
    private Double assignment;          // 과제 점수
    private Double participation;       // 참여도 점수
    private Double total;               // 총점
    private String letterGrade;         // 학점 (A, B+, B, C+, C, D+, D, F)
    private String status;              // 성적 상태 (IN_PROGRESS, FINALIZED)
    private String gradedAt;            // 성적 확정 일시

    public GradeDto() {}

    public GradeDto(Double midterm, Double finalExam, Double assignment, Double participation,
                    Double total, String letterGrade, String status) {
        this.midterm = midterm;
        this.finalExam = finalExam;
        this.assignment = assignment;
        this.participation = participation;
        this.total = total;
        this.letterGrade = letterGrade;
        this.status = status;
    }

    // Getters and Setters

    public Double getMidterm() {
        return midterm;
    }

    public void setMidterm(Double midterm) {
        this.midterm = midterm;
    }

    public Double getFinalExam() {
        return finalExam;
    }

    public void setFinalExam(Double finalExam) {
        this.finalExam = finalExam;
    }

    public Double getAssignment() {
        return assignment;
    }

    public void setAssignment(Double assignment) {
        this.assignment = assignment;
    }

    public Double getParticipation() {
        return participation;
    }

    public void setParticipation(Double participation) {
        this.participation = participation;
    }

    public Double getTotal() {
        return total;
    }

    public void setTotal(Double total) {
        this.total = total;
    }

    public String getLetterGrade() {
        return letterGrade;
    }

    public void setLetterGrade(String letterGrade) {
        this.letterGrade = letterGrade;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getGradedAt() {
        return gradedAt;
    }

    public void setGradedAt(String gradedAt) {
        this.gradedAt = gradedAt;
    }
}
