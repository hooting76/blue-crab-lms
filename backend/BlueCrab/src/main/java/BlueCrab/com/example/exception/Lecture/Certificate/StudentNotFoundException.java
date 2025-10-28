package BlueCrab.com.example.exception.Lecture.Certificate;

/**
 * 학생을 찾을 수 없을 때 발생하는 예외
 */
public class StudentNotFoundException extends RuntimeException {
    
    private final Integer studentIdx;
    
    public StudentNotFoundException(Integer studentIdx) {
        super("학생을 찾을 수 없습니다. Student IDX: " + studentIdx);
        this.studentIdx = studentIdx;
    }
    
    public StudentNotFoundException(Integer studentIdx, String additionalMessage) {
        super("학생을 찾을 수 없습니다. Student IDX: " + studentIdx + " - " + additionalMessage);
        this.studentIdx = studentIdx;
    }
    
    public Integer getStudentIdx() {
        return studentIdx;
    }
}
