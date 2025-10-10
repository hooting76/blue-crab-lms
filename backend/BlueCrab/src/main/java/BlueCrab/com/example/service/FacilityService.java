package BlueCrab.com.example.service;

import BlueCrab.com.example.dto.FacilityDto;
import BlueCrab.com.example.entity.FacilityBlockTbl;
import BlueCrab.com.example.entity.FacilityPolicyTbl;
import BlueCrab.com.example.entity.FacilityTbl;
import BlueCrab.com.example.enums.FacilityType;
import BlueCrab.com.example.exception.ResourceNotFoundException;
import BlueCrab.com.example.repository.FacilityBlockRepository;
import BlueCrab.com.example.repository.FacilityPolicyRepository;
import BlueCrab.com.example.repository.FacilityRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 시설 관리를 위한 서비스 클래스
 */
@Service
@Transactional
public class FacilityService {

    private final FacilityRepository facilityRepository;
    private final FacilityPolicyRepository policyRepository;
    private final FacilityBlockRepository blockRepository;

    public FacilityService(FacilityRepository facilityRepository,
                           FacilityPolicyRepository policyRepository,
                           FacilityBlockRepository blockRepository) {
        this.facilityRepository = facilityRepository;
        this.policyRepository = policyRepository;
        this.blockRepository = blockRepository;
    }

    public List<FacilityDto> getAllActiveFacilities() {
        return facilityRepository.findByIsActiveTrue()
            .stream()
            .map(this::convertToDto)
            .collect(Collectors.toList());
    }

    public List<FacilityDto> getFacilitiesByType(FacilityType facilityType) {
        return facilityRepository.findByFacilityTypeAndActive(facilityType.toDbValue())
            .stream()
            .map(this::convertToDto)
            .collect(Collectors.toList());
    }

    public FacilityDto getFacilityById(Integer facilityIdx) {
        FacilityTbl facility = facilityRepository.findById(facilityIdx)
            .orElseThrow(() -> ResourceNotFoundException.forId("시설", facilityIdx));
        return convertToDto(facility);
    }

    public List<FacilityDto> searchFacilities(String keyword) {
        return facilityRepository.searchFacilities(keyword)
            .stream()
            .map(this::convertToDto)
            .collect(Collectors.toList());
    }

    private FacilityDto convertToDto(FacilityTbl facility) {
        FacilityDto dto = new FacilityDto(
            facility.getFacilityIdx(),
            facility.getFacilityName(),
            facility.getFacilityTypeEnum(),
            facility.getFacilityDesc(),
            facility.getCapacity(),
            facility.getLocation(),
            facility.getDefaultEquipment(),
            facility.getIsActive()
        );

        // 승인 정책 조회 및 설정
        FacilityPolicyTbl policy = policyRepository.findByFacilityIdx(facility.getFacilityIdx())
            .orElse(null);

        if (policy != null) {
            dto.setRequiresApproval(policy.getRequiresApproval());
        } else {
            // 정책이 없는 경우 기본값 (관리자 승인 필요)
            dto.setRequiresApproval(true);
        }

        // 현재 시점 기준 활성 차단 정보 조회 및 설정
        List<FacilityBlockTbl> activeBlocks = blockRepository.findActiveFacilityBlocks(
            facility.getFacilityIdx(), LocalDateTime.now());

        if (!activeBlocks.isEmpty()) {
            FacilityBlockTbl block = activeBlocks.get(0);
            dto.setIsBlocked(true);
            dto.setBlockReason(block.getBlockReason());
        } else {
            dto.setIsBlocked(false);
            dto.setBlockReason(null);
        }

        return dto;
    }
}
