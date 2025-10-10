package BlueCrab.com.example.dto;

import BlueCrab.com.example.enums.FacilityType;

/**
 * 시설 정보 전송을 위한 DTO 클래스
 */
public class FacilityDto {

    private Integer facilityIdx;
    private String facilityName;
    private FacilityType facilityType;
    private String facilityDesc;
    private Integer capacity;
    private String location;
    private String defaultEquipment;
    private Boolean isActive;
    private Boolean requiresApproval;
    private Boolean isBlocked;
    private String blockReason;

    public FacilityDto() {}

    public FacilityDto(Integer facilityIdx, String facilityName, FacilityType facilityType,
                        String facilityDesc, Integer capacity, String location,
                        String defaultEquipment, Boolean isActive) {
        this.facilityIdx = facilityIdx;
        this.facilityName = facilityName;
        this.facilityType = facilityType;
        this.facilityDesc = facilityDesc;
        this.capacity = capacity;
        this.location = location;
        this.defaultEquipment = defaultEquipment;
        this.isActive = isActive;
        this.isBlocked = false;
    }

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

    public FacilityType getFacilityType() {
        return facilityType;
    }

    public void setFacilityType(FacilityType facilityType) {
        this.facilityType = facilityType;
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

    public Boolean getRequiresApproval() {
        return requiresApproval;
    }

    public void setRequiresApproval(Boolean requiresApproval) {
        this.requiresApproval = requiresApproval;
    }

    public Boolean getIsBlocked() {
        return isBlocked;
    }

    public void setIsBlocked(Boolean isBlocked) {
        this.isBlocked = isBlocked;
    }

    public String getBlockReason() {
        return blockReason;
    }

    public void setBlockReason(String blockReason) {
        this.blockReason = blockReason;
    }
}
