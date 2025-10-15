// 작성자: AI Assistant
// 학부 엔티티

package BlueCrab.com.example.entity.Lecture;

import javax.persistence.*;

/**
 * 학부(Faculty) 정보를 저장하는 JPA 엔티티 클래스
 * FACULTY 테이블과 매핑되며, 대학 내 단과대학/학부 정보 관리에 사용
 *
 * 주요 기능:
 * - 학부 기본 정보 저장 (학부코드, 학부명 등)
 * - 학부 정원 관리
 * - 설립 연도 정보
 *
 * 데이터베이스 매핑:
 * - 테이블명: FACULTY
 * - 기본키: faculty_id (자동 생성)
 * - 유니크 제약: faculty_code (학부 코드)
 *
 * @version 1.0.0
 * @since 2025-10-15
 */
@Entity
@Table(name = "FACULTY")
public class Faculty {

    /**
     * 학부 고유 식별자
     * 데이터베이스에서 자동 생성되는 기본키
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "faculty_id")
    private Integer facultyId;

    /**
     * 학부 코드
     * 두 자리 숫자 문자열 (예: "01", "02")
     * 유니크 제약 조건
     * 필수 입력 필드
     */
    @Column(name = "faculty_code", nullable = false, length = 2, unique = true)
    private String facultyCode;

    /**
     * 학부명
     * 필수 입력 필드
     */
    @Column(name = "faculty_name", nullable = false, length = 50)
    private String facultyName;

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
    public Faculty() {}

    /**
     * 필수 필드 초기화 생성자
     *
     * @param facultyCode 학부 코드 (두 자리 숫자)
     * @param facultyName 학부명
     * @param establishedAt 설립 연도
     */
    public Faculty(String facultyCode, String facultyName, Integer establishedAt) {
        this.facultyCode = facultyCode;
        this.facultyName = facultyName;
        this.establishedAt = establishedAt;
    }

    // Getters and Setters

    public Integer getFacultyId() {
        return facultyId;
    }

    public void setFacultyId(Integer facultyId) {
        this.facultyId = facultyId;
    }

    public String getFacultyCode() {
        return facultyCode;
    }

    public void setFacultyCode(String facultyCode) {
        this.facultyCode = facultyCode;
    }

    public String getFacultyName() {
        return facultyName;
    }

    public void setFacultyName(String facultyName) {
        this.facultyName = facultyName;
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
        return "Faculty{" +
                "facultyId=" + facultyId +
                ", facultyCode='" + facultyCode + '\'' +
                ", facultyName='" + facultyName + '\'' +
                ", establishedAt=" + establishedAt +
                ", capacity=" + capacity +
                '}';
    }
}
