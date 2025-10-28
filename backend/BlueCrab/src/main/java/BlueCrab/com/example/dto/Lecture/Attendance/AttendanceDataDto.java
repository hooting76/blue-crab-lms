package BlueCrab.com.example.dto.Lecture.Attendance;

import java.util.List;

/**
 * 전체 출석 데이터 Response DTO
 * ENROLLMENT_DATA.attendance 전체 구조
 * 
 * GET /api/attendance/student/{lecSerial} - 학생용 출석 조회
 * GET /api/attendance/professor/{lecSerial} - 교수용 출석 조회
 */
public class AttendanceDataDto {

    private AttendanceSummaryDto summary;                   // 출석 요약 정보
    private List<AttendanceSessionDto> sessions;            // 확정된 출석 기록 배열
    private List<AttendancePendingRequestDto> pendingRequests;  // 대기 중인 요청 배열
    
    // 프론트엔드 호환성 필드 (summary 데이터 기반 자동 계산)
    private String attendanceRate;                          // "n/80" 형식 문자열
    private String attendanceStr;                           // 출석 문자열 (출/지/결 나열)

    // Constructors

    public AttendanceDataDto() {}

    public AttendanceDataDto(AttendanceSummaryDto summary, List<AttendanceSessionDto> sessions, 
                             List<AttendancePendingRequestDto> pendingRequests) {
        this.summary = summary;
        this.sessions = sessions;
        this.pendingRequests = pendingRequests;
    }

    // Getters and Setters

    public AttendanceSummaryDto getSummary() {
        return summary;
    }

    public void setSummary(AttendanceSummaryDto summary) {
        this.summary = summary;
    }

    public List<AttendanceSessionDto> getSessions() {
        return sessions;
    }

    public void setSessions(List<AttendanceSessionDto> sessions) {
        this.sessions = sessions;
    }

    public List<AttendancePendingRequestDto> getPendingRequests() {
        return pendingRequests;
    }

    public void setPendingRequests(List<AttendancePendingRequestDto> pendingRequests) {
        this.pendingRequests = pendingRequests;
    }

    public String getAttendanceRate() {
        return attendanceRate;
    }

    public void setAttendanceRate(String attendanceRate) {
        this.attendanceRate = attendanceRate;
    }

    public String getAttendanceStr() {
        return attendanceStr;
    }

    public void setAttendanceStr(String attendanceStr) {
        this.attendanceStr = attendanceStr;
    }

    @Override
    public String toString() {
        return "AttendanceDataDto{" +
                "summary=" + summary +
                ", sessions=" + sessions +
                ", pendingRequests=" + pendingRequests +
                '}';
    }
}
