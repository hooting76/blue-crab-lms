// 작성자: 성태준

package BlueCrab.com.example.entity.Lecture;

import com.fasterxml.jackson.annotation.JsonIgnore;

import javax.persistence.*;

/**
 * 과제 정보를 저장하는 JPA 엔티티 클래스
 * ASSIGNMENT_EXTENDED_TBL 테이블과 매핑되며, 과제 등록 및 제출 관리에 사용
 *
 * 주요 기능:
 * - 과제 기본 정보 저장 (과제명, 설명, 마감일 등)
 * - 과제 제출 정보 JSON 저장
 * - 과제 채점 정보 JSON 저장
 *
 * 데이터베이스 매핑:
 * - 테이블명: ASSIGNMENT_EXTENDED_TBL
 * - 기본키: ASSIGNMENT_IDX (자동 생성)
 * - 외래키: LEC_IDX (LEC_TBL 참조)
 *
 * JSON 데이터 구조:
 * ASSIGNMENT_DATA 필드에 다음 정보를 JSON 형식으로 저장:
 * {
 *   "assignment": {
 *     "title": "자바 프로그래밍 과제 1",
 *     "description": "클래스와 객체 구현",
 *     "dueDate": "2025-03-25T23:59:59",
 *     "filePath": "/assignments/lecture1/assignment1.pdf",
 *     "status": "ACTIVE|DELETED",
 *     "createdAt": "2025-03-10T10:00:00"
 *   },
 *   "submissions": [
 *     {
 *       "studentIdx": 123,
 *       "studentName": "홍길동",
 *       "content": "과제 설명 내용",
 *       "filePath": "/submissions/student123/assignment1_submit.zip",
 *       "submittedAt": "2025-03-24T18:30:00",
 *       "score": 85.0,
 *       "feedback": "좋은 구현입니다.",
 *       "gradedAt": "2025-03-26T14:00:00"
 *     }
 *   ]
 * }
 *
 * 사용 예시:
 * - 교수가 과제 등록 시 새로운 AssignmentExtendedTbl 객체 생성
 * - 학생이 과제 제출 시 JSON 데이터 업데이트
 * - 교수가 과제 채점 시 JSON 데이터 업데이트
 *
 * @version 1.0.0
 * @since 2025-10-11
 */
@Entity
@Table(name = "ASSIGNMENT_EXTENDED_TBL")
public class AssignmentExtendedTbl {

    /**
     * 과제 고유 식별자
     * 데이터베이스에서 자동 생성되는 기본키
     * 과제 조회, 수정, 삭제 시 사용
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ASSIGNMENT_IDX")
    private Integer assignmentIdx;

    /**
     * 강의 IDX (외래키)
     * LEC_TBL의 LEC_IDX를 참조
     * 필수 입력 필드
     *
     * 관계: ASSIGNMENT_EXTENDED_TBL.LEC_IDX → LEC_TBL.LEC_IDX
     * ON UPDATE CASCADE, ON DELETE CASCADE
     *
     * 유효성:
     * - 필수 입력 (nullable = false)
     * - LEC_TBL에 존재하는 LEC_IDX여야 함
     */
    @Column(name = "LEC_IDX", nullable = false)
    private Integer lecIdx;

    /**
     * LecTbl 엔티티 참조
     * 강의 정보를 조회할 때 사용
     * Lazy Loading으로 필요 시에만 로드
     * 
     * JSON 직렬화에서 제외 (@JsonIgnore):
     * - Lazy loading 프록시 객체가 세션 없이 접근되면 예외 발생
     * - JSON 응답에 lecture 정보가 필요한 경우 DTO 사용 권장
     */
    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "LEC_IDX", referencedColumnName = "LEC_IDX", insertable = false, updatable = false)
    private LecTbl lecture;

    /**
     * 과제 정보 및 제출 목록 JSON 데이터
     * 선택 입력 필드, LONGTEXT 타입
     *
     * JSON 구조:
     * {
     *   "assignment": {
     *     "title": "과제 제목",
     *     "description": "과제 설명",
     *     "dueDate": "2025-03-25T23:59:59",
     *     "filePath": "/assignments/file.pdf",
     *     "maxScore": 100,
     *     "status": "ACTIVE|DELETED",
     *     "createdAt": "2025-03-10T10:00:00",
     *     "updatedAt": "2025-03-10T10:00:00"
     *   },
     *   "submissions": [
     *     {
     *       "submissionIdx": 1,
     *       "studentIdx": 123,
     *       "studentName": "홍길동",
     *       "studentCode": "202500101",
     *       "content": "과제 내용 설명",
     *       "filePath": "/submissions/student123/file.zip",
     *       "submittedAt": "2025-03-24T18:30:00",
     *       "score": 85.0,
     *       "feedback": "잘 작성하였습니다.",
     *       "gradedAt": "2025-03-26T14:00:00",
     *       "status": "SUBMITTED|GRADED|LATE"
     *     }
     *   ],
     *   "statistics": {
     *     "totalStudents": 30,
     *     "submittedCount": 28,
     *     "gradedCount": 25,
     *     "averageScore": 82.5,
     *     "submissionRate": 93.3
     *   }
     * }
     *
     * 유효성:
     * - 선택 입력 (nullable 허용)
     * - LONGTEXT 타입으로 대용량 JSON 저장 가능
     * - 유효한 JSON 형식이어야 함
     */
    @Column(name = "ASSIGNMENT_DATA", columnDefinition = "LONGTEXT")
    private String assignmentData;

    /**
     * 기본 생성자
     * JPA 엔티티 생성을 위해 필수
     */
    public AssignmentExtendedTbl() {}

    /**
     * 필수 필드 초기화 생성자
     * 과제 등록 시 새로운 과제 객체 생성에 사용
     *
     * @param lecIdx 강의 IDX
     */
    public AssignmentExtendedTbl(Integer lecIdx) {
        this.lecIdx = lecIdx;
    }

    /**
     * 전체 필드 초기화 생성자
     * 
     * @param lecIdx 강의 IDX
     * @param assignmentData JSON 데이터
     */
    public AssignmentExtendedTbl(Integer lecIdx, String assignmentData) {
        this.lecIdx = lecIdx;
        this.assignmentData = assignmentData;
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

    public LecTbl getLecture() {
        return lecture;
    }

    public void setLecture(LecTbl lecture) {
        this.lecture = lecture;
    }

    public String getAssignmentData() {
        return assignmentData;
    }

    public void setAssignmentData(String assignmentData) {
        this.assignmentData = assignmentData;
    }

    /**
     * 객체의 문자열 표현을 반환
     * 디버깅 및 로깅 시 사용
     * JSON 데이터는 길이가 길 수 있으므로 제외
     *
     * @return 과제 주요 정보의 문자열 표현
     */
    @Override
    public String toString() {
        return "AssignmentExtendedTbl{" +
                "assignmentIdx=" + assignmentIdx +
                ", lecIdx=" + lecIdx +
                ", hasData=" + (assignmentData != null && !assignmentData.isEmpty()) +
                '}';
    }
}
