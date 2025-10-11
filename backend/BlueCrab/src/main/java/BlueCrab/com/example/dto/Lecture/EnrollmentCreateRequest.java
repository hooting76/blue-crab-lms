package BlueCrab.com.example.dto.Lecture;

/**
 * 수강신청 요청 DTO
 * 학생이 강의를 수강신청할 때 사용
 */
public class EnrollmentCreateRequest {

    private Integer lecIdx;         // 강의 IDX (필수)
    private Integer studentIdx;     // 학생 IDX (필수, 또는 토큰에서 추출)

    public EnrollmentCreateRequest() {}

    public EnrollmentCreateRequest(Integer lecIdx, Integer studentIdx) {
        this.lecIdx = lecIdx;
        this.studentIdx = studentIdx;
    }

    // Getters and Setters

    public Integer getLecIdx() {
        return lecIdx;
    }

    public void setLecIdx(Integer lecIdx) {
        this.lecIdx = lecIdx;
    }

    public Integer getStudentIdx() {
        return studentIdx;
    }

    public void setStudentIdx(Integer studentIdx) {
        this.studentIdx = studentIdx;
    }
}
