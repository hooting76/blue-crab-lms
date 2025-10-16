package BlueCrab.com.example.dto;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

/**
 * 관리자용 예약 검색 요청 DTO
 * 프론트에서 전달하는 필터 조건과 페이지 정보를 캡처한다.
 */
public class AdminReservationSearchRequestDto {

    @Pattern(regexp = "^(PENDING|APPROVED|REJECTED|CANCELLED|COMPLETED)$", message = "status는 대문자 예약 상태 코드여야 합니다.")
    private String status;

    private Integer facilityIdx;

    /**
     * YYYY-MM-DD 포맷의 시작일 문자열
     */
    private String dateFrom;

    /**
     * YYYY-MM-DD 포맷의 종료일 문자열
     */
    private String dateTo;

    @Size(max = 100, message = "검색어는 100자 이하여야 합니다.")
    private String query;

    @NotNull(message = "page 값은 필수입니다.")
    @Min(value = 0, message = "page는 0 이상이어야 합니다.")
    private Integer page;

    @NotNull(message = "size 값은 필수입니다.")
    @Min(value = 1, message = "size는 1 이상이어야 합니다.")
    @Max(value = 50, message = "size는 50 이하여야 합니다.")
    private Integer size;

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Integer getFacilityIdx() {
        return facilityIdx;
    }

    public void setFacilityIdx(Integer facilityIdx) {
        this.facilityIdx = facilityIdx;
    }

    public String getDateFrom() {
        return dateFrom;
    }

    public void setDateFrom(String dateFrom) {
        this.dateFrom = dateFrom;
    }

    public String getDateTo() {
        return dateTo;
    }

    public void setDateTo(String dateTo) {
        this.dateTo = dateTo;
    }

    public String getQuery() {
        return query;
    }

    public void setQuery(String query) {
        this.query = query;
    }

    public Integer getPage() {
        return page;
    }

    public void setPage(Integer page) {
        this.page = page;
    }

    public Integer getSize() {
        return size;
    }

    public void setSize(Integer size) {
        this.size = size;
    }
}
