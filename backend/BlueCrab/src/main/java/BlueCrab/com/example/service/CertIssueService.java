package BlueCrab.com.example.service;

import BlueCrab.com.example.dto.CertIssueRequestDTO;
import BlueCrab.com.example.dto.CertIssueResponseDTO;
import BlueCrab.com.example.dto.RegistryRequestDTO;
import BlueCrab.com.example.dto.RegistryResponseDTO;
import BlueCrab.com.example.entity.CertIssueTbl;
import BlueCrab.com.example.entity.UserTbl;
import BlueCrab.com.example.exception.ResourceNotFoundException;
import BlueCrab.com.example.repository.CertIssueRepository;
import BlueCrab.com.example.repository.UserTblRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;

/**
 * 증명서 발급 이력 저장 서비스
 * POST /api/registry/cert/issue API의 비즈니스 로직을 담당
 *
 * 주요 기능:
 * - 증명서 발급 이력 저장 (누가, 언제, 무엇을)
 * - 발급 당시 학적/프로필 스냅샷 JSON 저장
 * - 발급 남발 방지 체크 (최근 N분 이내 발급 제한)
 * - 발급 통계 제공
 *
 * 보안 고려사항:
 * - 본인의 증명서만 발급 가능 (JWT 이메일 기준)
 * - 발급 IP 주소 기록
 * - 남발 방지 정책 적용
 *
 * 성능 최적화:
 * - 쓰기 트랜잭션 (@Transactional)
 * - 스냅샷 자동 생성 (RegistryService 연동)
 *
 * @author BlueCrab Development Team
 * @version 1.0.0
 * @since 2025-10-13
 */
@Service
public class CertIssueService {

    private static final Logger logger = LoggerFactory.getLogger(CertIssueService.class);

    @Autowired
    private CertIssueRepository certIssueRepository;

    @Autowired
    private UserTblRepository userTblRepository;

    @Autowired
    private RegistryService registryService;

    @Autowired
    private ObjectMapper objectMapper;

    /**
     * 발급 간격 제한 (분)
     * 동일 증명서를 N분 이내에 재발급 불가
     */
    private static final int ISSUE_INTERVAL_MINUTES = 5;

    /**
     * 증명서 발급 이력 저장
     * 
     * @param userEmail JWT 토큰에서 추출한 사용자 이메일
     * @param requestDTO 증명서 발급 요청 DTO
     * @param clientIp 클라이언트 IP 주소
     * @return 증명서 발급 응답 DTO
     * @throws RuntimeException 유효성 검증 실패 또는 발급 제한
     */
    @Transactional
    public CertIssueResponseDTO issueCertificate(String userEmail, CertIssueRequestDTO requestDTO, String clientIp) {
        logger.debug("증명서 발급 요청 - 사용자: {}, 유형: {}, IP: {}", userEmail, requestDTO.getType(), clientIp);

        // 1. 입력 유효성 검증
        validateRequest(userEmail, requestDTO);

        // 2. 사용자 조회
        UserTbl user = userTblRepository.findByUserEmail(userEmail)
                .orElseThrow(() -> ResourceNotFoundException.forField("사용자", "userEmail", userEmail));

        // 3. 남발 방지 체크
        checkIssueInterval(userEmail, requestDTO.getType());

        // 4. 스냅샷 생성 또는 사용
        String snapshotJson = generateOrUseSnapshot(userEmail, requestDTO);

        // 5. 발급 이력 저장
        CertIssueTbl certIssue = new CertIssueTbl();
        certIssue.setUser(user);
        certIssue.setCertType(requestDTO.getType());
        certIssue.setFormat(requestDTO.getFormat() != null ? requestDTO.getFormat() : "html");
        certIssue.setSnapshotJson(snapshotJson);
        certIssue.setIssuedAt(LocalDateTime.now());
        certIssue.setIssuedIp(clientIp);

        // asOfDate 설정 (있을 경우)
        if (requestDTO.getAsOf() != null && !requestDTO.getAsOf().trim().isEmpty()) {
            try {
                LocalDate asOfDate = LocalDate.parse(requestDTO.getAsOf(), DateTimeFormatter.ISO_LOCAL_DATE);
                certIssue.setAsOfDate(asOfDate);
            } catch (DateTimeParseException e) {
                logger.warn("잘못된 asOf 날짜 형식 - 무시: {}", requestDTO.getAsOf());
            }
        }

        certIssue = certIssueRepository.save(certIssue);

        logger.info("증명서 발급 완료 - 사용자: {}, 유형: {}, 발급ID: {}", 
                    userEmail, requestDTO.getType(), certIssue.generateIssueId());

        // 6. 응답 DTO 생성
        return CertIssueResponseDTO.of(
            certIssue.generateIssueId(),
            certIssue.getIssuedAt().toString()
        );
    }

    /**
     * 요청 유효성 검증
     * 
     * @param userEmail 사용자 이메일
     * @param requestDTO 요청 DTO
     * @throws IllegalArgumentException 유효성 검증 실패
     */
    private void validateRequest(String userEmail, CertIssueRequestDTO requestDTO) {
        if (userEmail == null || userEmail.trim().isEmpty()) {
            throw new IllegalArgumentException("유효하지 않은 사용자 이메일입니다.");
        }

        if (requestDTO == null) {
            throw new IllegalArgumentException("요청 데이터가 없습니다.");
        }

        if (!requestDTO.isValidType()) {
            throw new IllegalArgumentException(
                "유효하지 않은 증명서 유형입니다. 허용: enrollment, graduation_expected, graduation, transcript, certificate"
            );
        }

        if (!requestDTO.isValidFormat()) {
            throw new IllegalArgumentException(
                "유효하지 않은 발급 형식입니다. 허용: html, pdf, image"
            );
        }
    }

    /**
     * 발급 간격 체크 (남발 방지)
     * 최근 N분 이내에 동일 증명서를 발급한 이력이 있으면 예외 발생
     * 
     * @param userEmail 사용자 이메일
     * @param certType 증명서 유형
     * @throws RuntimeException 발급 간격 제한 위반
     */
    private void checkIssueInterval(String userEmail, String certType) {
        LocalDateTime cutoffTime = LocalDateTime.now().minusMinutes(ISSUE_INTERVAL_MINUTES);

        List<CertIssueTbl> recentIssues = certIssueRepository.findByUserEmailAndCertTypeAndIssuedAtAfter(
            userEmail, certType, cutoffTime
        );

        if (!recentIssues.isEmpty()) {
            CertIssueTbl lastIssue = recentIssues.get(0);
            long minutesAgo = java.time.Duration.between(lastIssue.getIssuedAt(), LocalDateTime.now()).toMinutes();
            
            logger.warn("증명서 발급 제한 - 사용자: {}, 유형: {}, 최근 발급: {}분 전", 
                        userEmail, certType, minutesAgo);
            
            throw new RuntimeException(
                String.format("동일한 증명서를 %d분 이내에 재발급할 수 없습니다. (최근 발급: %d분 전)", 
                              ISSUE_INTERVAL_MINUTES, minutesAgo)
            );
        }
    }

    /**
     * 스냅샷 생성 또는 사용
     * - requestDTO.snapshot이 있으면 사용
     * - 없으면 RegistryService를 통해 자동 생성
     * 
     * @param userEmail 사용자 이메일
     * @param requestDTO 요청 DTO
     * @return JSON 스냅샷 문자열
     */
    private String generateOrUseSnapshot(String userEmail, CertIssueRequestDTO requestDTO) {
        try {
            if (requestDTO.getSnapshot() != null && !requestDTO.getSnapshot().isEmpty()) {
                // 프론트에서 전달한 스냅샷 사용
                logger.debug("프론트 제공 스냅샷 사용 - 사용자: {}", userEmail);
                return objectMapper.writeValueAsString(requestDTO.getSnapshot());
            } else {
                // 서버에서 자동 생성
                logger.debug("서버 자동 스냅샷 생성 - 사용자: {}", userEmail);
                
                RegistryRequestDTO registryRequest = new RegistryRequestDTO(requestDTO.getAsOf());
                RegistryResponseDTO registryData = registryService.getMyRegistry(userEmail, registryRequest);
                
                return objectMapper.writeValueAsString(registryData);
            }
        } catch (JsonProcessingException e) {
            logger.error("JSON 스냅샷 생성 실패 - 사용자: {}", userEmail, e);
            throw new RuntimeException("스냅샷 데이터 변환에 실패했습니다.", e);
        }
    }

    /**
     * 사용자별 발급 이력 조회
     * 
     * @param userEmail 사용자 이메일
     * @return 발급 이력 목록 (최신순)
     */
    @Transactional(readOnly = true)
    public List<CertIssueTbl> getIssueHistory(String userEmail) {
        logger.debug("발급 이력 조회 - 사용자: {}", userEmail);
        return certIssueRepository.findAllByUserEmailOrderByIssuedAtDesc(userEmail);
    }

    /**
     * 사용자별 특정 증명서 발급 이력 조회
     * 
     * @param userEmail 사용자 이메일
     * @param certType 증명서 유형
     * @return 해당 증명서 발급 이력 목록 (최신순)
     */
    @Transactional(readOnly = true)
    public List<CertIssueTbl> getIssueHistoryByType(String userEmail, String certType) {
        logger.debug("발급 이력 조회 (유형별) - 사용자: {}, 유형: {}", userEmail, certType);
        return certIssueRepository.findByUserEmailAndCertType(userEmail, certType);
    }

    /**
     * 증명서 유형별 총 발급 건수 조회 (통계용)
     * 
     * @param certType 증명서 유형
     * @return 총 발급 건수
     */
    @Transactional(readOnly = true)
    public long countByCertType(String certType) {
        return certIssueRepository.countByCertType(certType);
    }

    /**
     * 특정 기간 내 발급 건수 조회 (통계용)
     * 
     * @param startTime 시작 시간
     * @param endTime 종료 시간
     * @return 기간 내 총 발급 건수
     */
    @Transactional(readOnly = true)
    public long countByPeriod(LocalDateTime startTime, LocalDateTime endTime) {
        return certIssueRepository.countByIssuedAtBetween(startTime, endTime);
    }

    /**
     * 사용자별 총 발급 건수 조회
     * 
     * @param userEmail 사용자 이메일
     * @return 총 발급 건수
     */
    @Transactional(readOnly = true)
    public long countByUserEmail(String userEmail) {
        return certIssueRepository.countByUserEmail(userEmail);
    }
}
