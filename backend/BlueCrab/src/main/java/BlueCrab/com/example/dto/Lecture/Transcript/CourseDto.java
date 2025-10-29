package BlueCrab.com.example.dto.Lecture.Transcript;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 개별 수강 과목 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CourseDto {
    
    // 강의 기본 정보
    private Integer lecIdx;
    private String lecSerial;        // 강의 코드 (예: ETH201)
    private String lecTitle;         // 강의명
    private Integer credits;         // 이수학점
    private String professorName;    // 교수 이름
    private Integer year;            // 수강 년도
    private Integer semester;        // 학기 (1 or 2)
    
    // 성적 정보
    private Double totalScore;       // 획득 점수
    private Double maxScore;         // 만점
    private Double percentage;       // 백분율 (0~100)
    private String letterGrade;      // 학점 등급 (A, B, C, D, F)
    private Double gpa;              // GPA (4.0 만점)
    
    // 석차 정보 (성적 확정 시)
    private Integer rank;            // 석차 (null 가능)
    private Integer totalStudents;   // 전체 학생 수 (null 가능)
    
    // 상태 정보
    private String status;           // COMPLETED, FAILED, IN_PROGRESS, NOT_GRADED, DROPPED
    
    // 세부 성적 (선택적)
    private Double attendanceScore;       // 출석 점수
    private Double attendanceMaxScore;    // 출석 만점
    private Integer attendanceRate;       // 출석률 (%)
    private Double assignmentScore;       // 과제 점수
    private Double assignmentMaxScore;    // 과제 만점
}
