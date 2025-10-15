// 작성자: AI Assistant
// 학과 엔티티

package BlueCrab.com.example.entity.Lecture;

import javax.persistence.*;

/**
 * 학과(Department) 정보를 저장하는 JPA 엔티티 클래스
 * DEPARTMENT 테이블과 매핑되며, 학부 하위 학과 정보 관리에 사용
 *
 * 주요 기능:
 * - 학과 기본 정보 저장 (학과코드, 학과명 등)
 * - 학부와의 관계 관리
 * - 학과 정원 관리
 * - 설립 연도 정보
 *
 * 데이터베이스 매핑:
 * - 테이블명: DEPARTMENT
 * - 기본키: dept_id (자동 생성)
 * - 외래키: faculty_id → FACULTY 테이블 참조
 * - 유니크 제약: (faculty_id, dept_code) 조합
 *
 * @version 1.0.0
 * @since 2025-10-15
 */
@Entity
@Table(name = "DEPARTMENT",
       uniqueConstraints = @UniqueConstraint(name = "uq_faculty_dept", columnNames = {"faculty_id", "dept_code"}))
public class Department {

    /**
     * 학과 고유 식별자
     * 데이터베이스에서 자동 생성되는 기본키
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "dept_id")
    private Integer deptId;

    /**
     * 학과 코드
     * 두 자리 숫자 문자열 (예: "01", "03")
     * faculty_id와 조합하여 유니크 제약
     * 필수 입력 필드
     */
    @Column(name = "dept_code", nullable = false, length = 2)
    private String deptCode;

    /**
     * 학과명
     * 필수 입력 필드
     */
    @Column(name = "dept_name", nullable = false, length = 100)
    private String deptName;

    /**
     * 소속 학부 ID
     * FACULTY 테이블의 faculty_id를 참조하는 외래키
     * 필수 입력 필드
     */
    @Column(name = "faculty_id", nullable = false)
    private Integer facultyId;

    /**
     * 설립 연도
     * YEAR 타입
     * 필수 입력 필드
     */
    @Column(name = "established_at", nullable = false)
    private Integer establishedAt;

    /**
     * 정원
     * 기본값 0
     */
    @Column(name = "capacity", nullable = false)
    private Integer capacity = 0;

    /**
     * 기본 생성자
     * JPA 엔티티 생성을 위해 필수
     */
    public Department() {}

    /**
     * 필수 필드 초기화 생성자
     *
     * @param deptCode 학과 코드 (두 자리 숫자)
     * @param deptName 학과명
     * @param facultyId 소속 학부 ID
     * @param establishedAt 설립 연도
     */
    public Department(String deptCode, String deptName, Integer facultyId, Integer establishedAt) {
        this.deptCode = deptCode;
        this.deptName = deptName;
        this.facultyId = facultyId;
        this.establishedAt = establishedAt;
    }

    // Getters and Setters

    public Integer getDeptId() {
        return deptId;
    }

    public void setDeptId(Integer deptId) {
        this.deptId = deptId;
    }

    public String getDeptCode() {
        return deptCode;
    }

    public void setDeptCode(String deptCode) {
        this.deptCode = deptCode;
    }

    public String getDeptName() {
        return deptName;
    }

    public void setDeptName(String deptName) {
        this.deptName = deptName;
    }

    public Integer getFacultyId() {
        return facultyId;
    }

    public void setFacultyId(Integer facultyId) {
        this.facultyId = facultyId;
    }

    public Integer getEstablishedAt() {
        return establishedAt;
    }

    public void setEstablishedAt(Integer establishedAt) {
        this.establishedAt = establishedAt;
    }

    public Integer getCapacity() {
        return capacity;
    }

    public void setCapacity(Integer capacity) {
        this.capacity = capacity;
    }

    @Override
    public String toString() {
        return "Department{" +
                "deptId=" + deptId +
                ", deptCode='" + deptCode + '\'' +
                ", deptName='" + deptName + '\'' +
                ", facultyId=" + facultyId +
                ", establishedAt=" + establishedAt +
                ", capacity=" + capacity +
                '}';
    }
}
