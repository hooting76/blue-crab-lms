package BlueCrab.com.example.entity;

import BlueCrab.com.example.enums.FacilityType;

import javax.persistence.*;
import java.time.LocalDateTime;

/**
 * 시설 정보를 저장하는 JPA 엔티티 클래스
 * FACILITY_TBL 테이블과 매핑
 */
@Entity
@Table(name = "FACILITY_TBL")
public class FacilityTbl {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "FACILITY_IDX")
    private Integer facilityIdx;

    @Column(name = "FACILITY_NAME", nullable = false, length = 100)
    private String facilityName;

    @Column(name = "FACILITY_TYPE", nullable = false, length = 20)
    private String facilityType;

    @Column(name = "FACILITY_DESC", columnDefinition = "TEXT")
    private String facilityDesc;

    @Column(name = "CAPACITY")
    private Integer capacity;

    @Column(name = "LOCATION", length = 200)
    private String location;

    @Column(name = "DEFAULT_EQUIPMENT", columnDefinition = "TEXT")
    private String defaultEquipment;

    @Column(name = "IS_ACTIVE", nullable = false)
    private Boolean isActive = true;

    @Column(name = "CREATED_AT", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "UPDATED_AT", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public FacilityTbl() {}

    public Integer getFacilityIdx() {
        return facilityIdx;
    }

    public void setFacilityIdx(Integer facilityIdx) {
        this.facilityIdx = facilityIdx;
    }

    public String getFacilityName() {
        return facilityName;
    }

    public void setFacilityName(String facilityName) {
        this.facilityName = facilityName;
    }

    public String getFacilityType() {
        return facilityType;
    }

    public void setFacilityType(String facilityType) {
        this.facilityType = facilityType;
    }

    public FacilityType getFacilityTypeEnum() {
        return FacilityType.fromDbValue(this.facilityType);
    }

    public void setFacilityTypeEnum(FacilityType facilityType) {
        this.facilityType = facilityType != null ? facilityType.toDbValue() : null;
    }

    public String getFacilityDesc() {
        return facilityDesc;
    }

    public void setFacilityDesc(String facilityDesc) {
        this.facilityDesc = facilityDesc;
    }

    public Integer getCapacity() {
        return capacity;
    }

    public void setCapacity(Integer capacity) {
        this.capacity = capacity;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getDefaultEquipment() {
        return defaultEquipment;
    }

    public void setDefaultEquipment(String defaultEquipment) {
        this.defaultEquipment = defaultEquipment;
    }

    public Boolean getIsActive() {
        return isActive;
    }

    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
