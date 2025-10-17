// 작성자: 성태준

package BlueCrab.com.example.dto.Lecture;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.util.List;

/* 과제 정보 전송을 위한 DTO 클래스
 * 과제 목록 조회, 과제 상세 조회 시 사용
 */
public class AssignmentDto {

    private Integer assignmentIdx;
    @JsonIgnore  // 프론트엔드에서 lecIdx 숨기기 - lecSerial 사용
    private Integer lecIdx;
    private String lecSerial;
    private String lecTit;
    private String title;               // 과제 제목
    private String description;         // 과제 설명
    private String dueDate;             // 마감일 (ISO 8601 형식)
    private String filePath;            // 과제 파일 경로
    private Integer maxScore;           // 최대 점수
    private String status;              // 과제 상태 (ACTIVE, DELETED)
    private String createdAt;           // 생성일시
    private String updatedAt;           // 수정일시
    private List<AssignmentSubmissionDto> submissions;  // 제출 목록
    private AssignmentStatisticsDto statistics;         // 통계 정보

    public AssignmentDto() {}

    public AssignmentDto(Integer assignmentIdx, Integer lecIdx, String lecSerial, String lecTit,
                         String title, String description, String dueDate, String filePath,
                         Integer maxScore, String status, String createdAt) {
        this.assignmentIdx = assignmentIdx;
        this.lecIdx = lecIdx;
        this.lecSerial = lecSerial;
        this.lecTit = lecTit;
        this.title = title;
        this.description = description;
        this.dueDate = dueDate;
        this.filePath = filePath;
        this.maxScore = maxScore;
        this.status = status;
        this.createdAt = createdAt;
    }

    // Getters and Setters

    public Integer getAssignmentIdx() {
        return assignmentIdx;
    }

    public void setAssignmentIdx(Integer assignmentIdx) {
        this.assignmentIdx = assignmentIdx;
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

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getDueDate() {
        return dueDate;
    }

    public void setDueDate(String dueDate) {
        this.dueDate = dueDate;
    }

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public Integer getMaxScore() {
        return maxScore;
    }

    public void setMaxScore(Integer maxScore) {
        this.maxScore = maxScore;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }

    public String getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(String updatedAt) {
        this.updatedAt = updatedAt;
    }

    public List<AssignmentSubmissionDto> getSubmissions() {
        return submissions;
    }

    public void setSubmissions(List<AssignmentSubmissionDto> submissions) {
        this.submissions = submissions;
    }

    public AssignmentStatisticsDto getStatistics() {
        return statistics;
    }

    public void setStatistics(AssignmentStatisticsDto statistics) {
        this.statistics = statistics;
    }
}
