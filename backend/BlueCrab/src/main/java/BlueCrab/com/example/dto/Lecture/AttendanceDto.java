package BlueCrab.com.example.dto.Lecture;

/**
 * 출결 정보 전송을 위한 DTO 클래스
 * 출결 조회, 출결 체크 시 사용
 * 
 * 출석 상태:
 * - 출: 출석
 * - 결: 결석
 * - 지: 지각
 */
public class AttendanceDto {

    private Integer sessionNumber;      // 회차 번호 (1~80)
    private String status;              // 출석 상태 (출/결/지)
    private String attendanceStr;       // 출석 문자열 전체 (예: "1출2출3결4지...")
    private String attendanceRate;      // 출석률 (예: "75/80")

    public AttendanceDto() {}

    public AttendanceDto(Integer sessionNumber, String status) {
        this.sessionNumber = sessionNumber;
        this.status = status;
    }

    public AttendanceDto(String attendanceStr, String attendanceRate) {
        this.attendanceStr = attendanceStr;
        this.attendanceRate = attendanceRate;
    }

    // Getters and Setters

    public Integer getSessionNumber() {
        return sessionNumber;
    }

    public void setSessionNumber(Integer sessionNumber) {
        this.sessionNumber = sessionNumber;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getAttendanceStr() {
        return attendanceStr;
    }

    public void setAttendanceStr(String attendanceStr) {
        this.attendanceStr = attendanceStr;
    }

    public String getAttendanceRate() {
        return attendanceRate;
    }

    public void setAttendanceRate(String attendanceRate) {
        this.attendanceRate = attendanceRate;
    }
}
