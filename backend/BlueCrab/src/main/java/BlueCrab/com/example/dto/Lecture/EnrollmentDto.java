// 작성자: 성태준

package BlueCrab.com.example.dto.Lecture;

import java.util.List;

/**
 * 수강신청 정보 전송을 위한 DTO 클래스
 * 수강신청 조회, 수강 내역 확인 시 사용
 */
public class EnrollmentDto {

    private Integer enrollmentIdx;
    private Integer lecIdx;
    private String lecSerial;
    private String lecTit;
    private String lecProf;        // 교수코드 (USER_CODE)
    private String lecProfName;    // 교수 이름 (USER_NAME)
    private String lecSummary;     // 강의 설명
    private Integer lecPoint;
    private String lecTime;
    private Integer studentIdx;
    private String studentCode;
    private String studentName;
    private String enrollmentStatus;
    private String enrollmentDate;
    private String cancelDate;
    private String cancelReason;
    private List<AttendanceDto> attendanceRecords;
    private GradeDto grade;

    public EnrollmentDto() {}

    public EnrollmentDto(Integer enrollmentIdx, Integer lecIdx, String lecSerial, String lecTit,
                         String lecProf, Integer lecPoint, String lecTime, Integer studentIdx,
                         String studentCode, String studentName, String enrollmentStatus,
                         String enrollmentDate) {
        this.enrollmentIdx = enrollmentIdx;
        this.lecIdx = lecIdx;
        this.lecSerial = lecSerial;
        this.lecTit = lecTit;
        this.lecProf = lecProf;
        this.lecPoint = lecPoint;
        this.lecTime = lecTime;
        this.studentIdx = studentIdx;
        this.studentCode = studentCode;
        this.studentName = studentName;
        this.enrollmentStatus = enrollmentStatus;
        this.enrollmentDate = enrollmentDate;
    }

    // Getters and Setters

    public Integer getEnrollmentIdx() {
        return enrollmentIdx;
    }

    public void setEnrollmentIdx(Integer enrollmentIdx) {
        this.enrollmentIdx = enrollmentIdx;
    }

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

    public String getLecTime() {
        return lecTime;
    }

    public void setLecTime(String lecTime) {
        this.lecTime = lecTime;
    }

    public Integer getStudentIdx() {
        return studentIdx;
    }

    public void setStudentIdx(Integer studentIdx) {
        this.studentIdx = studentIdx;
    }

    public String getStudentCode() {
        return studentCode;
    }

    public void setStudentCode(String studentCode) {
        this.studentCode = studentCode;
    }

    public String getStudentName() {
        return studentName;
    }

    public void setStudentName(String studentName) {
        this.studentName = studentName;
    }

    public String getEnrollmentStatus() {
        return enrollmentStatus;
    }

    public void setEnrollmentStatus(String enrollmentStatus) {
        this.enrollmentStatus = enrollmentStatus;
    }

    public String getEnrollmentDate() {
        return enrollmentDate;
    }

    public void setEnrollmentDate(String enrollmentDate) {
        this.enrollmentDate = enrollmentDate;
    }

    public String getCancelDate() {
        return cancelDate;
    }

    public void setCancelDate(String cancelDate) {
        this.cancelDate = cancelDate;
    }

    public String getCancelReason() {
        return cancelReason;
    }

    public void setCancelReason(String cancelReason) {
        this.cancelReason = cancelReason;
    }

    public List<AttendanceDto> getAttendanceRecords() {
        return attendanceRecords;
    }

    public void setAttendanceRecords(List<AttendanceDto> attendanceRecords) {
        this.attendanceRecords = attendanceRecords;
    }

    public GradeDto getGrade() {
        return grade;
    }

    public void setGrade(GradeDto grade) {
        this.grade = grade;
    }
}
