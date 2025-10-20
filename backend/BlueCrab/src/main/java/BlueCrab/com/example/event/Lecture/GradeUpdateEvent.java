// 작성자: 성태준
// 성적 업데이트 이벤트

package BlueCrab.com.example.event.Lecture;

import org.springframework.context.ApplicationEvent;

/**
 * 성적 업데이트 이벤트
 * 출석 체크 또는 과제 채점 시 발생하여 성적 자동 재계산을 트리거
 */
public class GradeUpdateEvent extends ApplicationEvent {

    private final Integer lecIdx;
    private final Integer studentIdx;
    private final String updateType;  // "ATTENDANCE" 또는 "ASSIGNMENT"

    /**
     * 성적 업데이트 이벤트 생성
     * 
     * @param source 이벤트를 발생시킨 객체
     * @param lecIdx 강의 IDX
     * @param studentIdx 학생 IDX
     * @param updateType 업데이트 유형 ("ATTENDANCE" 또는 "ASSIGNMENT")
     */
    public GradeUpdateEvent(Object source, Integer lecIdx, Integer studentIdx, String updateType) {
        super(source);
        this.lecIdx = lecIdx;
        this.studentIdx = studentIdx;
        this.updateType = updateType;
    }

    public Integer getLecIdx() {
        return lecIdx;
    }

    public Integer getStudentIdx() {
        return studentIdx;
    }

    public String getUpdateType() {
        return updateType;
    }

    @Override
    public String toString() {
        return "GradeUpdateEvent{" +
                "lecIdx=" + lecIdx +
                ", studentIdx=" + studentIdx +
                ", updateType='" + updateType + '\'' +
                '}';
    }
}
