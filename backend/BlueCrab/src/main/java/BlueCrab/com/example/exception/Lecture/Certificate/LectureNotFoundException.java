package BlueCrab.com.example.exception.Lecture.Certificate;

/**
 * 강의 정보를 찾을 수 없을 때 발생하는 예외
 */
public class LectureNotFoundException extends RuntimeException {
    
    private final Integer lecIdx;
    
    public LectureNotFoundException(Integer lecIdx) {
        super("강의 정보를 찾을 수 없습니다. Lecture IDX: " + lecIdx);
        this.lecIdx = lecIdx;
    }
    
    public LectureNotFoundException(Integer lecIdx, String additionalMessage) {
        super("강의 정보를 찾을 수 없습니다. Lecture IDX: " + lecIdx + " - " + additionalMessage);
        this.lecIdx = lecIdx;
    }
    
    public Integer getLecIdx() {
        return lecIdx;
    }
}
