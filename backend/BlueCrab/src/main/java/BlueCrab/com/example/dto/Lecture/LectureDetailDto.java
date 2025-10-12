// 작성자: 성태준

package BlueCrab.com.example.dto.Lecture;

/**
 * 강의 상세 정보 전송을 위한 DTO 클래스
 * 강의 상세 조회 시 사용 (강의 개요 포함)
 */
public class LectureDetailDto extends LectureDto {

    private String lecSummary;
    private String lecReg;

    public LectureDetailDto() {
        super();
    }

    public LectureDetailDto(Integer lecIdx, String lecSerial, String lecTit, String lecProf,
                            Integer lecPoint, Integer lecMajor, Integer lecMust, String lecTime,
                            Integer lecAssign, Integer lecOpen, Integer lecMany, Integer lecCurrent,
                            String lecMcode, String lecMcodeDep, Integer lecMin,
                            Integer lecYear, Integer lecSemester, String lecSummary, String lecReg) {
        super(lecIdx, lecSerial, lecTit, lecProf, lecPoint, lecMajor, lecMust, lecTime,
              lecAssign, lecOpen, lecMany, lecCurrent, lecMcode, lecMcodeDep, lecMin,
              lecYear, lecSemester);
        this.lecSummary = lecSummary;
        this.lecReg = lecReg;
    }

    public String getLecSummary() {
        return lecSummary;
    }

    public void setLecSummary(String lecSummary) {
        this.lecSummary = lecSummary;
    }

    public String getLecReg() {
        return lecReg;
    }

    public void setLecReg(String lecReg) {
        this.lecReg = lecReg;
    }
}
