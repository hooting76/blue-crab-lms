package BlueCrab.com.example.exception.Lecture.Certificate;

/**
 * 성적 데이터가 유효하지 않을 때 발생하는 예외
 */
public class InvalidGradeDataException extends RuntimeException {
    
    private final Integer enrollmentIdx;
    private final String fieldName;
    
    public InvalidGradeDataException(Integer enrollmentIdx, String message) {
        super("성적 데이터 오류 (Enrollment IDX: " + enrollmentIdx + "): " + message);
        this.enrollmentIdx = enrollmentIdx;
        this.fieldName = null;
    }
    
    public InvalidGradeDataException(Integer enrollmentIdx, String fieldName, String message) {
        super("성적 데이터 오류 (Enrollment IDX: " + enrollmentIdx + ", Field: " + fieldName + "): " + message);
        this.enrollmentIdx = enrollmentIdx;
        this.fieldName = fieldName;
    }
    
    public Integer getEnrollmentIdx() {
        return enrollmentIdx;
    }
    
    public String getFieldName() {
        return fieldName;
    }
}
