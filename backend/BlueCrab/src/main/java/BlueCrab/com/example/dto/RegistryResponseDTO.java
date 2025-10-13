package BlueCrab.com.example.dto;

/**
 * 학적 조회 API 응답 DTO
 * POST /api/registry/me 응답 데이터
 *
 * 포함 정보:
 * - 사용자 기본 정보 (이름, 이메일, 학번)
 * - 학적 정보 (학적상태, 입학경로, 이수학기)
 * - 주소 정보
 * - 학부/학과 정보
 * - 발급 시각
 *
 * ApiResponse<RegistryResponseDTO> 형태로 반환됨
 *
 * @author BlueCrab Development Team
 * @version 1.0.0
 * @since 2025-10-13
 */
public class RegistryResponseDTO {

    /**
     * 사용자 실명
     */
    private String userName;

    /**
     * 사용자 이메일
     */
    private String userEmail;

    /**
     * 학번 또는 교번
     * 예시: "202500101000"
     */
    private String studentCode;

    /**
     * 학적 상태
     * 재학, 휴학, 졸업, 졸업예정 등
     */
    private String academicStatus;

    /**
     * 입학 경로
     * 신규, 정시, 수시, 편입 등
     */
    private String admissionRoute;

    /**
     * 이수 완료 학기 수
     */
    private Integer enrolledTerms;

    /**
     * 휴학 기간 (문자열)
     * NULL 가능: 재학 중이거나 휴학이 아닌 경우
     */
    private String restPeriod;

    /**
     * 학부명 (선택)
     * 추후 조인 시 추가 가능
     */
    private String facultyName;

    /**
     * 학과명 (선택)
     * 추후 조인 시 추가 가능
     */
    private String departmentName;

    /**
     * 졸업 예정일 (선택)
     * 계산 로직으로 추가 가능
     * 예시: "2027-02-28"
     */
    private String expectedGraduateAt;

    /**
     * 주소 정보 (중첩 객체)
     */
    private AddressDTO address;

    /**
     * 발급(조회) 시각
     * ISO-8601 형식
     */
    private String issuedAt;

    // ========== Inner Class: AddressDTO ==========

    /**
     * 주소 정보를 담는 내부 DTO
     */
    public static class AddressDTO {
        private String zipCode;
        private String mainAddress;
        private String detailAddress;

        public AddressDTO() {}

        public AddressDTO(String zipCode, String mainAddress, String detailAddress) {
            this.zipCode = zipCode;
            this.mainAddress = mainAddress;
            this.detailAddress = detailAddress;
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

        @Override
        public String toString() {
            return "AddressDTO{" +
                    "zipCode='" + zipCode + '\'' +
                    ", mainAddress='" + mainAddress + '\'' +
                    ", detailAddress='" + detailAddress + '\'' +
                    '}';
        }
    }

    // ========== Constructors ==========

    public RegistryResponseDTO() {}

    // ========== Builder Pattern (선택) ==========

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private final RegistryResponseDTO dto = new RegistryResponseDTO();

        public Builder userName(String userName) {
            dto.userName = userName;
            return this;
        }

        public Builder userEmail(String userEmail) {
            dto.userEmail = userEmail;
            return this;
        }

        public Builder studentCode(String studentCode) {
            dto.studentCode = studentCode;
            return this;
        }

        public Builder academicStatus(String academicStatus) {
            dto.academicStatus = academicStatus;
            return this;
        }

        public Builder admissionRoute(String admissionRoute) {
            dto.admissionRoute = admissionRoute;
            return this;
        }

        public Builder enrolledTerms(Integer enrolledTerms) {
            dto.enrolledTerms = enrolledTerms;
            return this;
        }

        public Builder restPeriod(String restPeriod) {
            dto.restPeriod = restPeriod;
            return this;
        }

        public Builder facultyName(String facultyName) {
            dto.facultyName = facultyName;
            return this;
        }

        public Builder departmentName(String departmentName) {
            dto.departmentName = departmentName;
            return this;
        }

        public Builder expectedGraduateAt(String expectedGraduateAt) {
            dto.expectedGraduateAt = expectedGraduateAt;
            return this;
        }

        public Builder address(AddressDTO address) {
            dto.address = address;
            return this;
        }

        public Builder issuedAt(String issuedAt) {
            dto.issuedAt = issuedAt;
            return this;
        }

        public RegistryResponseDTO build() {
            return dto;
        }
    }

    // ========== Getters and Setters ==========

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getUserEmail() {
        return userEmail;
    }

    public void setUserEmail(String userEmail) {
        this.userEmail = userEmail;
    }

    public String getStudentCode() {
        return studentCode;
    }

    public void setStudentCode(String studentCode) {
        this.studentCode = studentCode;
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

    public Integer getEnrolledTerms() {
        return enrolledTerms;
    }

    public void setEnrolledTerms(Integer enrolledTerms) {
        this.enrolledTerms = enrolledTerms;
    }

    public String getRestPeriod() {
        return restPeriod;
    }

    public void setRestPeriod(String restPeriod) {
        this.restPeriod = restPeriod;
    }

    public String getFacultyName() {
        return facultyName;
    }

    public void setFacultyName(String facultyName) {
        this.facultyName = facultyName;
    }

    public String getDepartmentName() {
        return departmentName;
    }

    public void setDepartmentName(String departmentName) {
        this.departmentName = departmentName;
    }

    public String getExpectedGraduateAt() {
        return expectedGraduateAt;
    }

    public void setExpectedGraduateAt(String expectedGraduateAt) {
        this.expectedGraduateAt = expectedGraduateAt;
    }

    public AddressDTO getAddress() {
        return address;
    }

    public void setAddress(AddressDTO address) {
        this.address = address;
    }

    public String getIssuedAt() {
        return issuedAt;
    }

    public void setIssuedAt(String issuedAt) {
        this.issuedAt = issuedAt;
    }

    @Override
    public String toString() {
        return "RegistryResponseDTO{" +
                "userName='" + userName + '\'' +
                ", userEmail='" + userEmail + '\'' +
                ", studentCode='" + studentCode + '\'' +
                ", academicStatus='" + academicStatus + '\'' +
                ", admissionRoute='" + admissionRoute + '\'' +
                ", enrolledTerms=" + enrolledTerms +
                ", address=" + address +
                ", issuedAt='" + issuedAt + '\'' +
                '}';
    }
}
