package BlueCrab.com.example.dto;

/**
 * 증명서 발급 이력 저장 API 응답 DTO
 * POST /api/registry/cert/issue 응답 데이터
 *
 * 포함 정보:
 * - 발급 ID (고유 식별자)
 * - 발급 일시
 *
 * ApiResponse<CertIssueResponseDTO> 형태로 반환됨
 *
 * @author BlueCrab Development Team
 * @version 1.0.0
 * @since 2025-10-13
 */
public class CertIssueResponseDTO {

    /**
     * 발급 ID
     * 형식: C20250302-000123
     * C + 날짜(YYYYMMDD) + - + 시퀀스(6자리)
     */
    private String issueId;

    /**
     * 발급 일시
     * ISO-8601 형식
     * 예시: "2025-03-02T10:00:02Z"
     */
    private String issuedAt;

    // ========== Constructors ==========

    public CertIssueResponseDTO() {}

    public CertIssueResponseDTO(String issueId, String issuedAt) {
        this.issueId = issueId;
        this.issuedAt = issuedAt;
    }

    // ========== Static Factory Method ==========

    /**
     * CertIssueTbl Entity로부터 DTO 생성
     *
     * @param issueId 발급 ID
     * @param issuedAt 발급 일시 (ISO-8601)
     * @return CertIssueResponseDTO
     */
    public static CertIssueResponseDTO of(String issueId, String issuedAt) {
        return new CertIssueResponseDTO(issueId, issuedAt);
    }

    // ========== Getters and Setters ==========

    public String getIssueId() {
        return issueId;
    }

    public void setIssueId(String issueId) {
        this.issueId = issueId;
    }

    public String getIssuedAt() {
        return issuedAt;
    }

    public void setIssuedAt(String issuedAt) {
        this.issuedAt = issuedAt;
    }

    @Override
    public String toString() {
        return "CertIssueResponseDTO{" +
                "issueId='" + issueId + '\'' +
                ", issuedAt='" + issuedAt + '\'' +
                '}';
    }
}
