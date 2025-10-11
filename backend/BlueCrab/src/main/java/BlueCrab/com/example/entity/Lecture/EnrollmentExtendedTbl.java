package BlueCrab.com.example.entity.Lecture;

import BlueCrab.com.example.entity.UserTbl;

import javax.persistence.*;

/**
 * 수강신청 정보를 저장하는 JPA 엔티티 클래스
 * ENROLLMENT_EXTENDED_TBL 테이블과 매핑되며, 수강신청, 출결, 성적 관리에 사용
 *
 * 주요 기능:
 * - 수강신청 정보 저장 (학생 + 강의 연결)
 * - 출결 정보 JSON 저장
 * - 성적 정보 JSON 저장
 * - 수강 상태 관리
 *
 * 데이터베이스 매핑:
 * - 테이블명: ENROLLMENT_EXTENDED_TBL
 * - 기본키: ENROLLMENT_IDX (자동 생성)
 * - 외래키: LEC_IDX (LEC_TBL 참조), STUDENT_IDX (USER_TBL 참조)
 *
 * JSON 데이터 구조:
 * ENROLLMENT_DATA 필드에 다음 정보를 JSON 형식으로 저장:
 * {
 *   "enrollment": {
 *     "status": "ENROLLED",
 *     "enrollmentDate": "2025-03-01T09:00:00",
 *     "cancelDate": null,
 *     "cancelReason": null
 *   },
 *   "attendance": [
 *     {
 *       "date": "2025-03-10",
 *       "status": "PRESENT",
 *       "requestReason": null,
 *       "approvalStatus": null
 *     }
 *   ],
 *   "grade": {
 *     "midterm": 85.5,
 *     "final": 92.0,
 *     "assignment": 88.0,
 *     "participation": 90.0,
 *     "total": 88.9,
 *     "letterGrade": "A",
 *     "status": "FINALIZED"
 *   }
 * }
 *
 * 사용 예시:
 * - 학생이 수강신청 시 새로운 EnrollmentExtendedTbl 객체 생성
 * - 교수가 출결 체크 시 JSON 데이터 업데이트
 * - 교수가 성적 입력 시 JSON 데이터 업데이트
 *
 * @author BlueCrab Development Team
 * @version 1.0.0
 * @since 2025-10-11
 */
@Entity
@Table(name = "ENROLLMENT_EXTENDED_TBL")
public class EnrollmentExtendedTbl {

    /**
     * 수강신청 고유 식별자
     * 데이터베이스에서 자동 생성되는 기본키
     * 수강신청 조회, 수정, 삭제 시 사용
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ENROLLMENT_IDX")
    private Integer enrollmentIdx;

    /**
     * 강의 IDX (외래키)
     * LEC_TBL의 LEC_IDX를 참조
     * 필수 입력 필드
     *
     * 관계: ENROLLMENT_EXTENDED_TBL.LEC_IDX → LEC_TBL.LEC_IDX
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
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "LEC_IDX", referencedColumnName = "LEC_IDX", insertable = false, updatable = false)
    private LecTbl lecture;

    /**
     * 학생 IDX (외래키)
     * USER_TBL의 USER_IDX를 참조
     * 필수 입력 필드
     *
     * 관계: ENROLLMENT_EXTENDED_TBL.STUDENT_IDX → USER_TBL.USER_IDX
     * ON UPDATE CASCADE, ON DELETE CASCADE
     *
     * 유효성:
     * - 필수 입력 (nullable = false)
     * - USER_TBL에 존재하는 USER_IDX여야 함
     * - userStudent = 1 (학생)인 사용자여야 함
     */
    @Column(name = "STUDENT_IDX", nullable = false)
    private Integer studentIdx;

    /**
     * UserTbl 엔티티 참조
     * 학생 정보를 조회할 때 사용
     * Lazy Loading으로 필요 시에만 로드
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "STUDENT_IDX", referencedColumnName = "USER_IDX", insertable = false, updatable = false)
    private UserTbl student;

    /**
     * 수강신청/출결/성적 JSON 데이터
     * 선택 입력 필드, LONGTEXT 타입
     *
     * JSON 구조:
     * {
     *   "enrollment": {
     *     "status": "ENROLLED|CANCELLED|COMPLETED",
     *     "enrollmentDate": "2025-03-01T09:00:00",
     *     "cancelDate": "2025-03-15T10:00:00",
     *     "cancelReason": "시간표 중복"
     *   },
     *   "attendance": [
     *     {
     *       "date": "2025-03-10",
     *       "status": "PRESENT|LATE|ABSENT|EXCUSED",
     *       "requestReason": "병원 진료",
     *       "approvalStatus": "PENDING|APPROVED|REJECTED",
     *       "approvedBy": 1,
     *       "approvedAt": "2025-03-11T14:00:00"
     *     }
     *   ],
     *   "grade": {
     *     "midterm": 85.5,
     *     "final": 92.0,
     *     "assignment": 88.0,
     *     "participation": 90.0,
     *     "total": 88.9,
     *     "letterGrade": "A",
     *     "status": "IN_PROGRESS|FINALIZED",
     *     "gradedAt": "2025-06-20T10:00:00"
     *   }
     * }
     *
     * 유효성:
     * - 선택 입력 (nullable 허용)
     * - LONGTEXT 타입으로 대용량 JSON 저장 가능
     * - 유효한 JSON 형식이어야 함
     */
    @Column(name = "ENROLLMENT_DATA", columnDefinition = "LONGTEXT")
    private String enrollmentData;

    /**
     * 기본 생성자
     * JPA 엔티티 생성을 위해 필수
     */
    public EnrollmentExtendedTbl() {}

    /**
     * 필수 필드 초기화 생성자
     * 수강신청 시 새로운 수강신청 객체 생성에 사용
     *
     * @param lecIdx 강의 IDX
     * @param studentIdx 학생 IDX
     */
    public EnrollmentExtendedTbl(Integer lecIdx, Integer studentIdx) {
        this.lecIdx = lecIdx;
        this.studentIdx = studentIdx;
    }

    /**
     * 전체 필드 초기화 생성자
     * 
     * @param lecIdx 강의 IDX
     * @param studentIdx 학생 IDX
     * @param enrollmentData JSON 데이터
     */
    public EnrollmentExtendedTbl(Integer lecIdx, Integer studentIdx, String enrollmentData) {
        this.lecIdx = lecIdx;
        this.studentIdx = studentIdx;
        this.enrollmentData = enrollmentData;
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

    public LecTbl getLecture() {
        return lecture;
    }

    public void setLecture(LecTbl lecture) {
        this.lecture = lecture;
    }

    public Integer getStudentIdx() {
        return studentIdx;
    }

    public void setStudentIdx(Integer studentIdx) {
        this.studentIdx = studentIdx;
    }

    public UserTbl getStudent() {
        return student;
    }

    public void setStudent(UserTbl student) {
        this.student = student;
    }

    public String getEnrollmentData() {
        return enrollmentData;
    }

    public void setEnrollmentData(String enrollmentData) {
        this.enrollmentData = enrollmentData;
    }

    /**
     * 객체의 문자열 표현을 반환
     * 디버깅 및 로깅 시 사용
     * JSON 데이터는 길이가 길 수 있으므로 제외
     *
     * @return 수강신청 주요 정보의 문자열 표현
     */
    @Override
    public String toString() {
        return "EnrollmentExtendedTbl{" +
                "enrollmentIdx=" + enrollmentIdx +
                ", lecIdx=" + lecIdx +
                ", studentIdx=" + studentIdx +
                ", hasData=" + (enrollmentData != null && !enrollmentData.isEmpty()) +
                '}';
    }
}
