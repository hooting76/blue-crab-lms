package BlueCrab.com.example.entity;

import javax.persistence.*;

/**
 * PROFILE_VIEW 뷰테이블과 매핑되는 JPA Entity
 * 사용자의 종합적인 프로필 정보를 조회하기 위한 읽기 전용 엔티티
 *
 * 포함 정보:
 * - 기본 사용자 정보 (이메일, 이름, 전화번호, 생년월일)
 * - 주소 정보 (우편번호, 기본주소, 상세주소)
 * - 학적 정보 (학적상태, 입학경로)
 * - 전공 정보 (주전공, 부전공)
 * - 프로필 이미지 키
 *
 * @Entity 어노테이션으로 JPA 엔티티로 선언
 * @Table로 PROFILE_VIEW 뷰와 매핑
 * @Immutable로 읽기 전용 설정 (수정 불가)
 */
@Entity
@Table(name = "PROFILE_VIEW")
@org.hibernate.annotations.Immutable
public class ProfileView {

    @Id
    @Column(name = "user_email")
    private String userEmail;

    @Column(name = "user_name")
    private String userName;

    @Column(name = "user_phone")
    private String userPhone;

    @Column(name = "user_type")
    private Integer userType;  // 0: 학생, 1: 교수

    @Column(name = "major_code")
    private String majorCode;  // 학번 (큰 숫자이므로 String 타입 사용)

    @Column(name = "zip_code")
    private String zipCode;

    @Column(name = "main_address")
    private String mainAddress;

    @Column(name = "detail_address")
    private String detailAddress;

    @Column(name = "profile_image_key")
    private String profileImageKey;

    @Column(name = "birth_date")
    private String birthDate;

    @Column(name = "academic_status")
    private String academicStatus;

    @Column(name = "admission_route")
    private String admissionRoute;

    @Column(name = "major_faculty_code")
    private String majorFacultyCode;

    @Column(name = "major_dept_code")
    private String majorDeptCode;

    @Column(name = "minor_faculty_code")
    private String minorFacultyCode;

    @Column(name = "minor_dept_code")
    private String minorDeptCode;

    /**
     * 기본 생성자 (JPA 요구사항)
     */
    public ProfileView() {}

    /**
     * 사용자 유형을 한국어로 반환
     * @return "학생" 또는 "교수"
     */
    public String getUserTypeText() {
        return userType != null && userType == 1 ? "교수" : "학생";
    }

    /**
     * 완전한 주소를 반환 (우편번호 + 기본주소 + 상세주소)
     * @return 완전한 주소 문자열
     */
    public String getFullAddress() {
        StringBuilder address = new StringBuilder();

        if (zipCode != null && !zipCode.trim().isEmpty() && !"00000".equals(zipCode)) {
            address.append("(").append(zipCode).append(") ");
        }
        if (mainAddress != null && !mainAddress.trim().isEmpty()) {
            address.append(mainAddress);
        }
        if (detailAddress != null && !detailAddress.trim().isEmpty()) {
            address.append(" ").append(detailAddress);
        }

        return address.toString().trim();
    }

    /**
     * 주전공 정보가 있는지 확인
     * @return 주전공 정보 존재 여부
     */
    public boolean hasMajorInfo() {
        return majorFacultyCode != null && majorDeptCode != null;
    }

    /**
     * 부전공 정보가 있는지 확인
     * @return 부전공 정보 존재 여부
     */
    public boolean hasMinorInfo() {
        return minorFacultyCode != null && minorDeptCode != null;
    }

    // Getters and Setters
    public String getUserEmail() {
        return userEmail;
    }

    public void setUserEmail(String userEmail) {
        this.userEmail = userEmail;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getUserPhone() {
        return userPhone;
    }

    public void setUserPhone(String userPhone) {
        this.userPhone = userPhone;
    }

    public Integer getUserType() {
        return userType;
    }

    public void setUserType(Integer userType) {
        this.userType = userType;
    }

    public String getMajorCode() {
        return majorCode;
    }

    public void setMajorCode(String majorCode) {
        this.majorCode = majorCode;
    }

    public String getZipCode() {
        return zipCode;
    }

    public void setZipCode(String zipCode) {
        this.zipCode = zipCode;
    }

    public String getMainAddress() {
        return mainAddress;
    }

    public void setMainAddress(String mainAddress) {
        this.mainAddress = mainAddress;
    }

    public String getDetailAddress() {
        return detailAddress;
    }

    public void setDetailAddress(String detailAddress) {
        this.detailAddress = detailAddress;
    }

    public String getProfileImageKey() {
        return profileImageKey;
    }

    public void setProfileImageKey(String profileImageKey) {
        this.profileImageKey = profileImageKey;
    }

    public String getBirthDate() {
        return birthDate;
    }

    public void setBirthDate(String birthDate) {
        this.birthDate = birthDate;
    }

    public String getAcademicStatus() {
        return academicStatus;
    }

    public void setAcademicStatus(String academicStatus) {
        this.academicStatus = academicStatus;
    }

    public String getAdmissionRoute() {
        return admissionRoute;
    }

    public void setAdmissionRoute(String admissionRoute) {
        this.admissionRoute = admissionRoute;
    }

    public String getMajorFacultyCode() {
        return majorFacultyCode;
    }

    public void setMajorFacultyCode(String majorFacultyCode) {
        this.majorFacultyCode = majorFacultyCode;
    }

    public String getMajorDeptCode() {
        return majorDeptCode;
    }

    public void setMajorDeptCode(String majorDeptCode) {
        this.majorDeptCode = majorDeptCode;
    }

    public String getMinorFacultyCode() {
        return minorFacultyCode;
    }

    public void setMinorFacultyCode(String minorFacultyCode) {
        this.minorFacultyCode = minorFacultyCode;
    }

    public String getMinorDeptCode() {
        return minorDeptCode;
    }

    public void setMinorDeptCode(String minorDeptCode) {
        this.minorDeptCode = minorDeptCode;
    }

    @Override
    public String toString() {
        return "ProfileView{" +
                "userEmail='" + userEmail + '\'' +
                ", userName='" + userName + '\'' +
                ", userPhone='" + userPhone + '\'' +
                ", userType=" + userType +
                ", majorCode=" + majorCode +
                ", zipCode='" + zipCode + '\'' +
                ", mainAddress='" + mainAddress + '\'' +
                ", detailAddress='" + detailAddress + '\'' +
                ", profileImageKey='" + profileImageKey + '\'' +
                ", birthDate='" + birthDate + '\'' +
                ", academicStatus='" + academicStatus + '\'' +
                ", admissionRoute='" + admissionRoute + '\'' +
                ", majorFacultyCode='" + majorFacultyCode + '\'' +
                ", majorDeptCode='" + majorDeptCode + '\'' +
                ", minorFacultyCode='" + minorFacultyCode + '\'' +
                ", minorDeptCode='" + minorDeptCode + '\'' +
                '}';
    }
}