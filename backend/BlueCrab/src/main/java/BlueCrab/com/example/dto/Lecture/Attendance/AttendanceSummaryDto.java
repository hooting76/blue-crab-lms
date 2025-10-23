package BlueCrab.com.example.dto.Lecture.Attendance;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.LocalDateTime;

/**
 * 출석 요약 정보 DTO
 * ENROLLMENT_DATA.attendance.summary 객체
 * 
 * 전체 출석 통계 (자동 계산)
 */
public class AttendanceSummaryDto {

    private Integer attended;                               // 출석 횟수
    private Integer late;                                   // 지각 횟수
    private Integer absent;                                 // 결석 횟수
    private Integer totalSessions;                          // 총 회차 수
    private Double attendanceRate;                          // 출석률 (0.0 ~ 100.0)
    
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updatedAt;                        // 최종 갱신 일시

    // Constructors

    public AttendanceSummaryDto() {}

    public AttendanceSummaryDto(Integer attended, Integer late, Integer absent, 
                                Integer totalSessions, Double attendanceRate, LocalDateTime updatedAt) {
        this.attended = attended;
        this.late = late;
        this.absent = absent;
        this.totalSessions = totalSessions;
        this.attendanceRate = attendanceRate;
        this.updatedAt = updatedAt;
    }

    // Getters and Setters

    public Integer getAttended() {
        return attended;
    }

    public void setAttended(Integer attended) {
        this.attended = attended;
    }

    public Integer getLate() {
        return late;
    }

    public void setLate(Integer late) {
        this.late = late;
    }

    public Integer getAbsent() {
        return absent;
    }

    public void setAbsent(Integer absent) {
        this.absent = absent;
    }

    public Integer getTotalSessions() {
        return totalSessions;
    }

    public void setTotalSessions(Integer totalSessions) {
        this.totalSessions = totalSessions;
    }

    public Double getAttendanceRate() {
        return attendanceRate;
    }

    public void setAttendanceRate(Double attendanceRate) {
        this.attendanceRate = attendanceRate;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    @Override
    public String toString() {
        return "AttendanceSummaryDto{" +
                "attended=" + attended +
                ", late=" + late +
                ", absent=" + absent +
                ", totalSessions=" + totalSessions +
                ", attendanceRate=" + attendanceRate +
                ", updatedAt=" + updatedAt +
                '}';
    }
}
