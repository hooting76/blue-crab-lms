package BlueCrab.com.example.dto;

import java.util.Map;

/**
 * 증명서 발급 이력 저장 API 요청 DTO
 * POST /api/registry/cert/issue 요청 시 사용
 *
 * 필수 필드:
 * - type: 증명서 유형
 *
 * 선택 필드:
 * - asOf: 스냅샷 기준일
 * - format: 발급 형식 (기본값: html)
 * - snapshot: 스냅샷 데이터 (서버에서 자동 생성 가능)
 *
 * 사용 예시:
 * {
 *   "type": "enrollment",
 *   "asOf": "2025-03-01",
 *   "format": "html"
 * }
 *
 * @author BlueCrab Development Team
 * @version 1.0.0
 * @since 2025-10-13
 */
public class CertIssueRequestDTO {

    /**
     * 증명서 유형 (필수)
     * 
     * 허용 값:
     * - "enrollment": 재학증명서
     * - "graduation_expected": 졸업예정증명서
     * - "graduation": 졸업증명서
     * - "transcript": 성적증명서
     * - "certificate": 수료증명서
     */
    private String type;

    /**
     * 스냅샷 기준일 (선택)
     * 증명서 발급 시 기준이 되는 날짜
     *
     * 형식: "YYYY-MM-DD"
     * 예시: "2025-03-01"
     *
     * NULL 허용:
     * - 미입력 시 현재 시점 기준
     * - 입력 시 해당 날짜 기준 스냅샷
     */
    private String asOf;

    /**
     * 발급 형식 (선택)
     * 기본값: "html"
     *
     * 허용 값:
     * - "html": HTML 형식
     * - "pdf": PDF 파일
     * - "image": 이미지 파일
     */
    private String format = "html";

    /**
     * 스냅샷 데이터 (선택)
     * 프론트엔드에서 이미 조회한 학적 데이터를 전달할 때 사용
     *
     * NULL 허용:
     * - 미입력 시 서버에서 registry/me 결과를 자동으로 조회하여 삽입
     * - 입력 시 해당 데이터를 JSON으로 저장
     *
     * 주의:
     * - Map 형태로 받아서 JSON 문자열로 변환
     * - 프론트와 백엔드 간 불일치 방지 위해 서버 자동 생성 권장
     */
    private Map<String, Object> snapshot;

    // ========== Constructors ==========

    public CertIssueRequestDTO() {}

    public CertIssueRequestDTO(String type) {
        this.type = type;
    }

    public CertIssueRequestDTO(String type, String asOf, String format) {
        this.type = type;
        this.asOf = asOf;
        this.format = format;
    }

    // ========== Validation Helper ==========

    /**
     * 증명서 유형 유효성 검증
     * @return 유효한 유형인지 여부
     */
    public boolean isValidType() {
        if (type == null || type.trim().isEmpty()) {
            return false;
        }
        return type.equals("enrollment") ||
               type.equals("graduation_expected") ||
               type.equals("graduation") ||
               type.equals("transcript") ||
               type.equals("certificate");
    }

    /**
     * 발급 형식 유효성 검증
     * @return 유효한 형식인지 여부
     */
    public boolean isValidFormat() {
        if (format == null || format.trim().isEmpty()) {
            format = "html";
            return true;
        }
        return format.equals("html") || format.equals("pdf") || format.equals("image");
    }

    // ========== Getters and Setters ==========

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getAsOf() {
        return asOf;
    }

    public void setAsOf(String asOf) {
        this.asOf = asOf;
    }

    public String getFormat() {
        return format;
    }

    public void setFormat(String format) {
        this.format = format;
    }

    public Map<String, Object> getSnapshot() {
        return snapshot;
    }

    public void setSnapshot(Map<String, Object> snapshot) {
        this.snapshot = snapshot;
    }

    @Override
    public String toString() {
        return "CertIssueRequestDTO{" +
                "type='" + type + '\'' +
                ", asOf='" + asOf + '\'' +
                ", format='" + format + '\'' +
                ", hasSnapshot=" + (snapshot != null) +
                '}';
    }
}
