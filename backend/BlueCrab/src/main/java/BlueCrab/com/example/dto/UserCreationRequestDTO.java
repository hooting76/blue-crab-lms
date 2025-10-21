package BlueCrab.com.example.dto;

import BlueCrab.com.example.entity.UserTbl;
import lombok.Getter;
import lombok.Setter;

import javax.validation.constraints.*;

/**
 * 사용자 계정/학적 생성 요청 DTO.
 *
 * ⚠️ 유효성 검증은 테스트용 엔드포인트에서도 재사용되므로
 * 실제 도입 시 프런트 입력 형식에 맞게 패턴을 조정해야 합니다.
 */
@Getter
@Setter
public class UserCreationRequestDTO {

    // UserTbl fields
    @Email
    @NotBlank
    private String userEmail;

    @NotBlank
    @Size(min = 8, max = 100)
    private String userPw;

    @NotBlank
    private String userName;

    @NotBlank
    @Pattern(regexp = "\\d{10,11}", message = "전화번호는 숫자 10~11자리여야 합니다.")
    private String userPhone;

    @NotBlank
    private String userBirth;

    @Min(0)
    @Max(1)
    private int userStudent; // 0 for student, 1 for professor

    private Integer userZip;
    private String userFirstAdd;
    private String userLastAdd;
    private String userRegIp;
    private Integer admissionYear;   // 학번/교번 생성에 사용할 연도 (선택)

    // SerialCodeTable fields
    @Pattern(regexp = "\\d{2}", message = "전공 학부 코드는 2자리 숫자여야 합니다.")
    private String majorFacultyCode; // 주전공 학부 코드 (예: "01")

    @Pattern(regexp = "\\d{2}", message = "전공 학과 코드는 2자리 숫자여야 합니다.")
    private String majorDeptCode;    // 주전공 학과 코드 (예: "02")

    // 부전공 필드 (선택적)
    @Pattern(regexp = "\\d{2}", message = "부전공 학부 코드는 2자리 숫자여야 합니다.")
    private String minorFacultyCode; // 부전공 학부 코드

    @Pattern(regexp = "\\d{2}", message = "부전공 학과 코드는 2자리 숫자여야 합니다.")
    private String minorDeptCode;    // 부전공 학과 코드

    // RegistryTbl fields (optional overrides)
    private String joinPath;     // 입학 경로
    private String stdStat;      // 학적 상태
    private Integer cntTerm;     // 이수 학기 수
    private String stdRestDate;  // 휴학 기간
    private String adminName;    // 처리 관리자 이름
    private String adminIp;      // 처리 IP

    /**
     * DTO to UserTbl Entity conversion method.
     * 비밀번호는 서비스 계층에서 인코딩됩니다.
     * @return A UserTbl entity.
     */
    public UserTbl toUserTblEntity() {
        UserTbl user = new UserTbl();
        user.setUserEmail(this.userEmail);
        user.setUserPw(this.userPw);
        user.setUserName(this.userName);
        user.setUserPhone(this.userPhone);
        user.setUserBirth(this.userBirth);
        user.setUserStudent(this.userStudent);
        user.setUserZip(this.userZip);
        user.setUserFirstAdd(this.userFirstAdd);
        user.setUserLastAdd(this.userLastAdd);
        user.setUserRegIp(this.userRegIp);
        return user;
    }
}
