package BlueCrab.com.example.dto.Lecture.Attendance;

/**
 * 학생 출석 정보 Response DTO (교수용)
 * GET /api/attendance/professor/{lecSerial} 응답 배열 요소
 * 
 * 교수가 강의별 전체 학생의 출석 현황을 조회할 때 사용
 */
public class StudentAttendanceDto {

    private Integer studentIdx;                             // 학생 USER_IDX
    private String studentCode;                             // 학번 (USER_CODE)
    private String studentName;                             // 학생 이름 (USER_NAME)
    private AttendanceDataDto attendanceData;               // 출석 데이터 (summary, sessions, pendingRequests)

    // Constructors

    public StudentAttendanceDto() {}

    public StudentAttendanceDto(Integer studentIdx, String studentCode, String studentName, 
                                AttendanceDataDto attendanceData) {
        this.studentIdx = studentIdx;
        this.studentCode = studentCode;
        this.studentName = studentName;
        this.attendanceData = attendanceData;
    }

    // Getters and Setters

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

    public AttendanceDataDto getAttendanceData() {
        return attendanceData;
    }

    public void setAttendanceData(AttendanceDataDto attendanceData) {
        this.attendanceData = attendanceData;
    }

    @Override
    public String toString() {
        return "StudentAttendanceDto{" +
                "studentIdx=" + studentIdx +
                ", studentCode='" + studentCode + '\'' +
                ", studentName='" + studentName + '\'' +
                ", attendanceData=" + attendanceData +
                '}';
    }
}
