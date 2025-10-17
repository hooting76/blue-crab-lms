// 작성자: 성태준

package BlueCrab.com.example.dto.Lecture;

import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 * 강의 기본 정보 전송을 위한 DTO 클래스
 * 강의 목록 조회, 검색 결과 등에 사용
 * 
 * ⚠️ lecIdx는 내부 로직용이며 프론트엔드에 노출되지 않음
 * ✅ lecSerial(강의 코드)를 주 식별자로 사용
 */
public class LectureDto {

    @JsonIgnore  // 프론트엔드에 노출하지 않음
    private Integer lecIdx;
    private String lecSerial;
    private String lecTit;
    private String lecProf;        // 교수코드 (USER_CODE)
    private String lecProfName;    // 교수 이름 (USER_NAME)
    private String lecSummary;     // 강의 설명
    private Integer lecPoint;
    private Integer lecMajor;
    private Integer lecMust;
    private String lecTime;
    private Integer lecAssign;
    private Integer lecOpen;
    private Integer lecMany;
    private Integer lecCurrent;
    private String lecMcode;
    private String lecMcodeDep;
    private Integer lecMin;
    private Integer lecYear;
    private Integer lecSemester;
    private Integer availableSeats;
    private Boolean isFull;

    public LectureDto() {}

    public LectureDto(Integer lecIdx, String lecSerial, String lecTit, String lecProf,
                      Integer lecPoint, Integer lecMajor, Integer lecMust, String lecTime,
                      Integer lecAssign, Integer lecOpen, Integer lecMany, Integer lecCurrent,
                      String lecMcode, String lecMcodeDep, Integer lecMin,
                      Integer lecYear, Integer lecSemester) {
        this.lecIdx = lecIdx;
        this.lecSerial = lecSerial;
        this.lecTit = lecTit;
        this.lecProf = lecProf;
        this.lecPoint = lecPoint;
        this.lecMajor = lecMajor;
        this.lecMust = lecMust;
        this.lecTime = lecTime;
        this.lecAssign = lecAssign;
        this.lecOpen = lecOpen;
        this.lecMany = lecMany;
        this.lecCurrent = lecCurrent != null ? lecCurrent : 0;
        this.lecMcode = lecMcode;
        this.lecMcodeDep = lecMcodeDep;
        this.lecMin = lecMin;
        this.lecYear = lecYear;
        this.lecSemester = lecSemester;
        this.availableSeats = calculateAvailableSeats();
        this.isFull = checkIsFull();
    }

    private Integer calculateAvailableSeats() {
        if (lecMany == null || lecCurrent == null) {
            return 0;
        }
        return Math.max(0, lecMany - lecCurrent);
    }

    private Boolean checkIsFull() {
        if (lecMany == null || lecCurrent == null) {
            return false;
        }
        return lecCurrent >= lecMany;
    }

    // Getters and Setters

    public Integer getLecIdx() {
        return lecIdx;
    }

    public void setLecIdx(Integer lecIdx) {
        this.lecIdx = lecIdx;
    }

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

    public String getLecProfName() {
        return lecProfName;
    }

    public void setLecProfName(String lecProfName) {
        this.lecProfName = lecProfName;
    }

    public String getLecSummary() {
        return lecSummary;
    }

    public void setLecSummary(String lecSummary) {
        this.lecSummary = lecSummary;
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
        this.availableSeats = calculateAvailableSeats();
        this.isFull = checkIsFull();
    }

    public Integer getLecCurrent() {
        return lecCurrent;
    }

    public void setLecCurrent(Integer lecCurrent) {
        this.lecCurrent = lecCurrent;
        this.availableSeats = calculateAvailableSeats();
        this.isFull = checkIsFull();
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

    public Integer getAvailableSeats() {
        return availableSeats;
    }

    public void setAvailableSeats(Integer availableSeats) {
        this.availableSeats = availableSeats;
    }

    public Boolean getIsFull() {
        return isFull;
    }

    public void setIsFull(Boolean isFull) {
        this.isFull = isFull;
    }
}
