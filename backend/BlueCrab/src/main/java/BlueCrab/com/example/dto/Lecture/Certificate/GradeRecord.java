package BlueCrab.com.example.dto.Lecture.Certificate;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 개별 강의의 성적 정보를 담는 DTO
 * 성적확인서에 표시될 한 강의의 성적 데이터
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GradeRecord {
    
    /**
     * 강의 고유 ID
     */
    private Integer lecIdx;
    
    /**
     * 강의 코드 (예: CS284, ETH201)
     */
    private String lecSerial;
    
    /**
     * 강의명
     */
    private String lecTitle;
    
    /**
     * 이수학점
     */
    private Integer credits;
    
    /**
     * 담당 교수 IDX
     */
    private Integer professorIdx;
    
    /**
     * 담당 교수 이름
     */
    private String professorName;
    
    /**
     * 수강 년도
     */
    private Integer year;
    
    /**
     * 수강 학기 (1: 1학기, 2: 2학기)
     */
    private Integer semester;
    
    /**
     * 총점 (실제 획득 점수)
     */
    private BigDecimal totalScore;
    
    /**
     * 만점
     */
    private BigDecimal maxScore;
    
    /**
     * 백분율 점수 (0~100)
     */
    private BigDecimal percentage;
    
    /**
     * 최종 성적 등급 (A+, A0, B+, B0, C+, C0, D+, D0, F)
     */
    private String letterGrade;
    
    /**
     * 출석 점수
     */
    private BigDecimal attendanceScore;
    
    /**
     * 출석 만점
     */
    private BigDecimal attendanceMaxScore;
    
    /**
     * 과제 점수 합계
     */
    private BigDecimal assignmentScore;
    
    /**
     * 과제 만점 합계
     */
    private BigDecimal assignmentMaxScore;
    
    /**
     * 출석률 (%)
     */
    private Integer attendanceRate;
    
    /**
     * 성적 상태
     * - IN_PROGRESS: 수강 중
     * - COMPLETED: 수강 완료 (성적 확정)
     * - NOT_GRADED: 성적 미입력
     */
    private String gradeStatus;
    
    /**
     * 성적이 GPA 계산에 포함되는지 여부
     * F 등급이거나 미완료 과목은 false
     */
    private Boolean includedInGpa;
    
    /**
     * 비고 (재수강, 계절학기 등)
     */
    private String remarks;
}
