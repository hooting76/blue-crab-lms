package BlueCrab.com.example.service;

import BlueCrab.com.example.dto.RegistryRequestDTO;
import BlueCrab.com.example.dto.RegistryResponseDTO;
import BlueCrab.com.example.entity.RegistryTbl;
import BlueCrab.com.example.entity.UserTbl;
import BlueCrab.com.example.exception.ResourceNotFoundException;
import BlueCrab.com.example.repository.RegistryRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Optional;

/**
 * 학적 정보 조회 서비스
 * POST /api/registry/me API의 비즈니스 로직을 담당
 *
 * 주요 기능:
 * - JWT 토큰 기반 본인 학적 정보 조회
 * - 특정 시점 기준 학적 스냅샷 조회 (As-Of Query)
 * - Entity → DTO 변환
 * - 학적 정보 존재 여부 확인
 *
 * 보안 고려사항:
 * - 본인의 학적 정보만 조회 가능 (JWT 이메일 기준)
 * - 민감한 정보는 로깅하지 않음
 *
 * 성능 최적화:
 * - Repository에서 Fetch Join 사용 (N+1 방지)
 * - 읽기 전용 트랜잭션 (@Transactional(readOnly = true))
 *
 * @author BlueCrab Development Team
 * @version 1.0.0
 * @since 2025-10-13
 */
@Service
@Transactional(readOnly = true)
public class RegistryService {

    private static final Logger logger = LoggerFactory.getLogger(RegistryService.class);

    @Autowired
    private RegistryRepository registryRepository;

    /**
     * 사용자 이메일로 최신 학적 정보 조회
     * JWT 토큰에서 추출한 이메일을 사용하여 현재 시점의 최신 학적 정보를 조회
     *
     * @param userEmail JWT 토큰에서 추출한 사용자 이메일
     * @return 학적 정보 응답 DTO
     * @throws ResourceNotFoundException 학적 정보가 존재하지 않는 경우
     *
     * 사용 예시:
     * RegistryResponseDTO registry = registryService.getMyRegistry("student@univ.edu");
     */
    public RegistryResponseDTO getMyRegistry(String userEmail) {
        logger.debug("학적 조회 요청 - 사용자: {}", userEmail);

        if (userEmail == null || userEmail.trim().isEmpty()) {
            logger.warn("학적 조회 실패 - 유효하지 않은 이메일: {}", userEmail);
            throw new IllegalArgumentException("유효하지 않은 사용자 이메일입니다.");
        }

        Optional<RegistryTbl> registryOpt = registryRepository.findLatestByUserEmail(userEmail);

        if (registryOpt.isEmpty()) {
            logger.warn("학적 조회 실패 - 존재하지 않는 사용자: {}", userEmail);
            throw ResourceNotFoundException.forField("학적 정보", "userEmail", userEmail);
        }

        RegistryTbl registry = registryOpt.get();
        logger.info("학적 조회 성공 - 사용자: {}, 학번: {}, 학적상태: {}", 
                    userEmail, registry.getUserCode(), registry.getStdStat());

        return convertToDTO(registry);
    }

    /**
     * 사용자 이메일로 학적 정보 조회 (요청 DTO 사용)
     * asOf 파라미터가 있으면 특정 시점 기준 학적 조회
     *
     * @param userEmail JWT 토큰에서 추출한 사용자 이메일
     * @param requestDTO 학적 조회 요청 DTO (asOf 포함)
     * @return 학적 정보 응답 DTO
     * @throws ResourceNotFoundException 학적 정보가 존재하지 않는 경우
     *
     * 사용 예시:
     * RegistryRequestDTO request = new RegistryRequestDTO("2025-03-01");
     * RegistryResponseDTO registry = registryService.getMyRegistry("student@univ.edu", request);
     */
    public RegistryResponseDTO getMyRegistry(String userEmail, RegistryRequestDTO requestDTO) {
        if (requestDTO == null || requestDTO.getAsOf() == null || requestDTO.getAsOf().trim().isEmpty()) {
            // asOf가 없으면 현재 시점 기준 조회
            return getMyRegistry(userEmail);
        }

        String asOfStr = requestDTO.getAsOf().trim();
        logger.debug("학적 조회 요청 (시점 기준) - 사용자: {}, 기준일: {}", userEmail, asOfStr);

        // asOf 날짜 파싱
        LocalDateTime asOfDateTime;
        try {
            LocalDate asOfDate = LocalDate.parse(asOfStr, DateTimeFormatter.ISO_LOCAL_DATE);
            // 해당 날짜의 23:59:59로 설정 (하루의 끝)
            asOfDateTime = asOfDate.atTime(23, 59, 59);
        } catch (DateTimeParseException e) {
            logger.warn("학적 조회 실패 - 잘못된 날짜 형식: {}", asOfStr);
            throw new IllegalArgumentException("잘못된 날짜 형식입니다. 'YYYY-MM-DD' 형식을 사용하세요. 예: 2025-03-01");
        }

        Optional<RegistryTbl> registryOpt = registryRepository.findLatestByUserEmailAsOf(userEmail, asOfDateTime);

        if (registryOpt.isEmpty()) {
            logger.warn("학적 조회 실패 - 해당 시점에 학적 정보 없음: {}, 기준일: {}", userEmail, asOfStr);
            throw ResourceNotFoundException.forField("학적 정보", "userEmail (asOf: " + asOfStr + ")", userEmail);
        }

        RegistryTbl registry = registryOpt.get();
        logger.info("학적 조회 성공 (시점 기준) - 사용자: {}, 학번: {}, 기준일: {}, 학적상태: {}", 
                    userEmail, registry.getUserCode(), asOfStr, registry.getStdStat());

        return convertToDTO(registry);
    }

    /**
     * 학적 정보 존재 여부 확인
     * 사용자가 학적에 등록되어 있는지 확인
     *
     * @param userEmail 사용자 이메일
     * @return 학적 정보 존재 여부
     */
    public boolean hasRegistry(String userEmail) {
        if (userEmail == null || userEmail.trim().isEmpty()) {
            return false;
        }

        boolean exists = registryRepository.existsByUserEmail(userEmail);
        logger.debug("학적 존재 여부 확인 - 사용자: {}, 존재: {}", userEmail, exists);

        return exists;
    }

    /**
     * 학번으로 최신 학적 정보 조회 (관리자용)
     * 관리자 페이지에서 학번으로 검색 시 사용
     *
     * @param userCode 학번/교번
     * @return 학적 정보 응답 DTO
     * @throws ResourceNotFoundException 학적 정보가 존재하지 않는 경우
     */
    public RegistryResponseDTO getRegistryByUserCode(String userCode) {
        logger.debug("학적 조회 요청 (학번 기준) - 학번: {}", userCode);

        if (userCode == null || userCode.trim().isEmpty()) {
            throw new IllegalArgumentException("유효하지 않은 학번입니다.");
        }

        Optional<RegistryTbl> registryOpt = registryRepository.findLatestByUserCode(userCode);

        if (registryOpt.isEmpty()) {
            logger.warn("학적 조회 실패 - 존재하지 않는 학번: {}", userCode);
            throw ResourceNotFoundException.forField("학적 정보", "userCode", userCode);
        }

        RegistryTbl registry = registryOpt.get();
        logger.info("학적 조회 성공 (학번 기준) - 학번: {}, 이름: {}", 
                    userCode, registry.getUser().getUserName());

        return convertToDTO(registry);
    }

    /**
     * RegistryTbl Entity를 RegistryResponseDTO로 변환
     * 
     * @param registry 학적 엔티티
     * @return 학적 응답 DTO
     */
    private RegistryResponseDTO convertToDTO(RegistryTbl registry) {
        UserTbl user = registry.getUser();

        // 주소 정보 생성
        RegistryResponseDTO.AddressDTO addressDTO = new RegistryResponseDTO.AddressDTO();
        if (user.getUserZip() != null) {
            addressDTO.setZipCode(String.format("%05d", user.getUserZip()));
        }
        addressDTO.setMainAddress(user.getUserFirstAdd());
        addressDTO.setDetailAddress(user.getUserLastAdd());

        // DTO 빌드
        return RegistryResponseDTO.builder()
                .userName(user.getUserName())
                .userEmail(user.getUserEmail())
                .studentCode(registry.getUserCode())
                .academicStatus(registry.getStdStat())
                .admissionRoute(registry.getJoinPath())
                .enrolledTerms(registry.getCntTerm())
                .restPeriod(registry.getStdRestDate())
                .facultyName(null)  // TODO: FACULTY 테이블 조인 시 추가
                .departmentName(null)  // TODO: DEPARTMENT 테이블 조인 시 추가
                .expectedGraduateAt(calculateExpectedGraduateDate(registry))
                .address(addressDTO)
                .issuedAt(Instant.now().toString())
                .build();
    }

    /**
     * 졸업 예정일 계산 (간단한 로직)
     * 입학일 기준 + 8학기(4년) = 졸업 예정일
     * 
     * 주의: 실제 졸업 예정일은 복잡한 로직 필요
     * - 입학일 정보 필요
     * - 휴학 기간 고려
     * - 학과별 졸업 요건
     * 
     * 현재는 placeholder로 구현
     * 
     * @param registry 학적 정보
     * @return 졸업 예정일 (YYYY-MM-DD 형식, 또는 null)
     */
    private String calculateExpectedGraduateDate(RegistryTbl registry) {
        // TODO: 실제 입학일 정보가 있을 때 구현
        // 임시: 8학기 이상 이수 시 "졸업 예정" 표시
        if (registry.getCntTerm() != null && registry.getCntTerm() >= 7) {
            // 예시: 현재 년도 + 1년 2월 말
            int currentYear = LocalDate.now().getYear();
            return (currentYear + 1) + "-02-28";
        }
        return null;
    }

    /**
     * 학적 상태별 사용자 수 조회 (통계용)
     * 
     * @param stdStat 학적 상태 (재학, 휴학, 졸업 등)
     * @return 해당 상태의 사용자 수
     */
    public long countByAcademicStatus(String stdStat) {
        return registryRepository.countByStdStat(stdStat);
    }
}
