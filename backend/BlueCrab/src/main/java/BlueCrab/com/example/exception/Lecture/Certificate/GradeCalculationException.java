package BlueCrab.com.example.exception.Lecture.Certificate;

/**
 * 성적 계산 중 오류가 발생했을 때 발생하는 예외
 */
public class GradeCalculationException extends RuntimeException {
    
    private final String calculationType;
    
    public GradeCalculationException(String message) {
        super("성적 계산 오류: " + message);
        this.calculationType = null;
    }
    
    public GradeCalculationException(String calculationType, String message) {
        super("성적 계산 오류 (" + calculationType + "): " + message);
        this.calculationType = calculationType;
    }
    
    public GradeCalculationException(String calculationType, String message, Throwable cause) {
        super("성적 계산 오류 (" + calculationType + "): " + message, cause);
        this.calculationType = calculationType;
    }
    
    public String getCalculationType() {
        return calculationType;
    }
}
