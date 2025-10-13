package BlueCrab.com.example.dto;

/**
 * 학적 조회 API 요청 DTO
 * POST /api/registry/me 요청 시 사용
 *
 * 선택 필드:
 * - asOf: 특정 시점 기준 학적 조회 (미입력 시 현재 시점)
 *
 * 사용 예시:
 * {
 *   "asOf": "2025-03-01"
 * }
 *
 * @author BlueCrab Development Team
 * @version 1.0.0
 * @since 2025-10-13
 */
public class RegistryRequestDTO {

    /**
     * 스냅샷 기준일 (선택)
     * 특정 시점 기준의 학적 상태를 조회하고자 할 때 사용
     *
     * 형식: "YYYY-MM-DD"
     * 예시: "2025-03-01"
     *
     * NULL 허용:
     * - 미입력 시 현재 시점 기준 최신 학적 조회
     * - 입력 시 해당 날짜까지의 학적 이력 중 최신 조회
     */
    private String asOf;

    // ========== Constructors ==========

    public RegistryRequestDTO() {}

    public RegistryRequestDTO(String asOf) {
        this.asOf = asOf;
    }

    // ========== Getters and Setters ==========

    public String getAsOf() {
        return asOf;
    }

    public void setAsOf(String asOf) {
        this.asOf = asOf;
    }

    @Override
    public String toString() {
        return "RegistryRequestDTO{" +
                "asOf='" + asOf + '\'' +
                '}';
    }
}
