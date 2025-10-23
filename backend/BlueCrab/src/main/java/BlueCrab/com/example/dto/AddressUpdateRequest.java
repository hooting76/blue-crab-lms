package BlueCrab.com.example.dto;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

/**
 * 주소 업데이트 요청 DTO
 * POST /api/profile/address/update 요청 시 사용
 *
 * 프론트엔드 플로우:
 * 1. 사용자가 Kakao/Daum 우편번호 서비스에서 주소 선택
 * 2. 선택한 주소 데이터를 이 DTO 형식으로 백엔드에 전송
 * 3. 백엔드에서 검증 후 USER_TBL 테이블에 저장
 *
 * 데이터베이스 매핑:
 * - postalCode → USER_ZIP (Integer)
 * - roadAddress → USER_FIRST_ADD (VARCHAR 200)
 * - detailAddress → USER_LAST_ADD (VARCHAR 100)
 *
 * 요청 예시:
 * {
 *   "postalCode": "05852",
 *   "roadAddress": "서울 송파구 위례광장로 120",
 *   "detailAddress": "장지동, 위례중앙푸르지오1단지"
 * }
 *
 * @author BlueCrab Development Team
 * @version 1.0.0
 * @since 2025-10-23
 */
public class AddressUpdateRequest {

    /**
     * 우편번호 (5자리 숫자)
     * 예시: "05852", "13529"
     *
     * 검증 규칙:
     * - 필수 입력
     * - 정확히 5자리 숫자
     */
    @NotBlank(message = "우편번호는 필수입니다")
    @Pattern(regexp = "^[0-9]{5}$", message = "우편번호는 5자리 숫자여야 합니다")
    private String postalCode;

    /**
     * 도로명주소
     * 예시: "서울 송파구 위례광장로 120", "경기 성남시 분당구 판교역로 166"
     *
     * 검증 규칙:
     * - 필수 입력
     * - 최대 200자
     */
    @NotBlank(message = "도로명주소는 필수입니다")
    @Size(max = 200, message = "도로명주소는 200자를 초과할 수 없습니다")
    private String roadAddress;

    /**
     * 상세주소 (선택)
     * 예시: "장지동, 위례중앙푸르지오1단지", "101동 1001호"
     *
     * 검증 규칙:
     * - 선택 입력
     * - 최대 100자
     */
    @Size(max = 100, message = "상세주소는 100자를 초과할 수 없습니다")
    private String detailAddress;

    // ========== Constructors ==========

    /**
     * 기본 생성자 (Jackson deserialization용)
     */
    public AddressUpdateRequest() {}

    /**
     * 전체 필드 생성자
     *
     * @param postalCode 우편번호 (5자리)
     * @param roadAddress 도로명주소
     * @param detailAddress 상세주소
     */
    public AddressUpdateRequest(String postalCode, String roadAddress, String detailAddress) {
        this.postalCode = postalCode;
        this.roadAddress = roadAddress;
        this.detailAddress = detailAddress;
    }

    // ========== Getters and Setters ==========

    public String getPostalCode() {
        return postalCode;
    }

    public void setPostalCode(String postalCode) {
        this.postalCode = postalCode;
    }

    public String getRoadAddress() {
        return roadAddress;
    }

    public void setRoadAddress(String roadAddress) {
        this.roadAddress = roadAddress;
    }

    public String getDetailAddress() {
        return detailAddress;
    }

    public void setDetailAddress(String detailAddress) {
        this.detailAddress = detailAddress;
    }

    @Override
    public String toString() {
        return "AddressUpdateRequest{" +
                "postalCode='" + postalCode + '\'' +
                ", roadAddress='" + roadAddress + '\'' +
                ", detailAddress='" + detailAddress + '\'' +
                '}';
    }
}
