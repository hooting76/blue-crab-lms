package BlueCrab.com.example.dto.Lecture;

/**
 * 강의 생성 요청 DTO
 * 관리자가 새로운 강의를 개설할 때 사용
 */
public class LectureCreateRequest {

    private String lecSerial;       // 강의 코드 (필수)
    private String lecTit;          // 강의명 (필수)
    private String lecProf;         // 담당 교수 (필수)
    private Integer lecPoint;       // 이수 학점 (필수)
    private Integer lecMajor;       // 전공/교양 (기본값: 1)
    private Integer lecMust;        // 필수/선택 (기본값: 1)
    private String lecSummary;      // 강의 개요 (선택)
    private String lecTime;         // 강의 시간 (필수)
    private Integer lecAssign;      // 과제 유무 (기본값: 0)
    private Integer lecOpen;        // 수강신청 상태 (기본값: 0)
    private Integer lecMany;        // 최대 수강 인원 (필수)
    private String lecMcode;        // 학부 코드 (필수)
    private String lecMcodeDep;     // 학과 코드 (필수)
    private Integer lecMin;         // 최저 학년 제한 (기본값: 0)
    private Integer lecYear;        // 대상 학년 (선택)
    private Integer lecSemester;    // 학기 (선택)

    public LectureCreateRequest() {}

    // Getters and Setters

    public String getLecSerial() {
        return lecSerial;
    }

    public void setLecSerial(String lecSerial) {
        this.lecSerial = lecSerial;
    }

    public String getLecTit() {
        return lecTit;
    }

    public void setLecTit(String lecTit) {
        this.lecTit = lecTit;
    }

    public String getLecProf() {
        return lecProf;
    }

    public void setLecProf(String lecProf) {
        this.lecProf = lecProf;
    }

    public Integer getLecPoint() {
        return lecPoint;
    }

    public void setLecPoint(Integer lecPoint) {
        this.lecPoint = lecPoint;
    }

    public Integer getLecMajor() {
        return lecMajor;
    }

    public void setLecMajor(Integer lecMajor) {
        this.lecMajor = lecMajor;
    }

    public Integer getLecMust() {
        return lecMust;
    }

    public void setLecMust(Integer lecMust) {
        this.lecMust = lecMust;
    }

    public String getLecSummary() {
        return lecSummary;
    }

    public void setLecSummary(String lecSummary) {
        this.lecSummary = lecSummary;
    }

    public String getLecTime() {
        return lecTime;
    }

    public void setLecTime(String lecTime) {
        this.lecTime = lecTime;
    }

    public Integer getLecAssign() {
        return lecAssign;
    }

    public void setLecAssign(Integer lecAssign) {
        this.lecAssign = lecAssign;
    }

    public Integer getLecOpen() {
        return lecOpen;
    }

    public void setLecOpen(Integer lecOpen) {
        this.lecOpen = lecOpen;
    }

    public Integer getLecMany() {
        return lecMany;
    }

    public void setLecMany(Integer lecMany) {
        this.lecMany = lecMany;
    }

    public String getLecMcode() {
        return lecMcode;
    }

    public void setLecMcode(String lecMcode) {
        this.lecMcode = lecMcode;
    }

    public String getLecMcodeDep() {
        return lecMcodeDep;
    }

    public void setLecMcodeDep(String lecMcodeDep) {
        this.lecMcodeDep = lecMcodeDep;
    }

    public Integer getLecMin() {
        return lecMin;
    }

    public void setLecMin(Integer lecMin) {
        this.lecMin = lecMin;
    }

    public Integer getLecYear() {
        return lecYear;
    }

    public void setLecYear(Integer lecYear) {
        this.lecYear = lecYear;
    }

    public Integer getLecSemester() {
        return lecSemester;
    }

    public void setLecSemester(Integer lecSemester) {
        this.lecSemester = lecSemester;
    }
}
