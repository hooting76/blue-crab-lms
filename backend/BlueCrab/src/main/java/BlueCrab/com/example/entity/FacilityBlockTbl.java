package BlueCrab.com.example.entity;

import javax.persistence.*;
import java.time.LocalDateTime;

/**
 * 시설 차단 정보를 저장하는 JPA 엔티티 클래스
 * FACILITY_BLOCK_TBL 테이블과 매핑
 * 유지보수, 긴급 상황 등으로 시설 사용 불가 기간 관리
 */
@Entity
@Table(name = "FACILITY_BLOCK_TBL")
public class FacilityBlockTbl {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "BLOCK_IDX")
    private Integer blockIdx;

    @Column(name = "FACILITY_IDX", nullable = false)
    private Integer facilityIdx;

    @Column(name = "BLOCK_START", nullable = false)
    private LocalDateTime blockStart;

    @Column(name = "BLOCK_END", nullable = false)
    private LocalDateTime blockEnd;

    @Column(name = "BLOCK_REASON", nullable = false, length = 200)
    private String blockReason;

    @Column(name = "BLOCK_TYPE", length = 20)
    private String blockType = "MAINTENANCE";

    @Column(name = "CREATED_BY", nullable = false, length = 50)
    private String createdBy;

    @Column(name = "CREATED_AT", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    public FacilityBlockTbl() {}

    public Integer getBlockIdx() {
        return blockIdx;
    }

    public void setBlockIdx(Integer blockIdx) {
        this.blockIdx = blockIdx;
    }

    public Integer getFacilityIdx() {
        return facilityIdx;
    }

    public void setFacilityIdx(Integer facilityIdx) {
        this.facilityIdx = facilityIdx;
    }

    public LocalDateTime getBlockStart() {
        return blockStart;
    }

    public void setBlockStart(LocalDateTime blockStart) {
        this.blockStart = blockStart;
    }

    public LocalDateTime getBlockEnd() {
        return blockEnd;
    }

    public void setBlockEnd(LocalDateTime blockEnd) {
        this.blockEnd = blockEnd;
    }

    public String getBlockReason() {
        return blockReason;
    }

    public void setBlockReason(String blockReason) {
        this.blockReason = blockReason;
    }

    public String getBlockType() {
        return blockType;
    }

    public void setBlockType(String blockType) {
        this.blockType = blockType;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
