package BlueCrab.com.example.controller;

import BlueCrab.com.example.dto.ApiResponse;
import BlueCrab.com.example.dto.CertIssueRequestDTO;
import BlueCrab.com.example.dto.CertIssueResponseDTO;
import BlueCrab.com.example.dto.RegistryRequestDTO;
import BlueCrab.com.example.dto.RegistryResponseDTO;
import BlueCrab.com.example.service.CertIssueService;
import BlueCrab.com.example.service.RegistryService;
import BlueCrab.com.example.util.JwtUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;

/**
 * 학적 및 증명서 발급 관리를 위한 REST API 컨트롤러
 * JWT 토큰 기반 인증을 통해 본인의 학적 정보만 조회 및 증명서 발급 가능
 *
 * 주요 기능:
 * - 학적 정보 조회 (POST /api/registry/me)
 * - 증명서 발급 이력 저장 (POST /api/registry/cert/issue)
 *
 * 보안 사항:
 * - JWT 토큰을 통한 인증 필수
 * - 본인의 학적 정보만 접근 가능
 * - Authorization 헤더에서 Bearer 토큰 추출
 * - 발급 IP 주소 기록
 *
 * 응답 형식:
 * 모든 응답은 ApiResponse<T> 형식으로 통일:
 * {
 *   "success": true/false,
 *   "message": "한국어 메시지",
 *   "data": 실제_데이터,
 *   "errorCode": "에러_코드",
 *   "timestamp": "2025-XX-XX..."
 * }
 *
 * @author BlueCrab Development Team
 * @version 1.0.0
 * @since 2025-10-13
 */
@RestController
@RequestMapping("/api/registry")
public class RegistryController {

    private static final Logger logger = LoggerFactory.getLogger(RegistryController.class);

    @Autowired
    private RegistryService registryService;

    @Autowired
    private CertIssueService certIssueService;

    @Autowired
    private JwtUtil jwtUtil;

    /**
     * 내 학적 정보 조회
     * JWT 토큰에서 사용자 정보를 추출하여 해당 사용자의 학적 정보를 조회
     *
     * @param request HTTP 요청 (Authorization 헤더에서 토큰 추출)
     * @param requestDTO 학적 조회 요청 DTO (asOf 파라미터 포함, 선택)
     * @return 사용자의 학적 정보
     *
     * 요청 예시:
     * POST /api/registry/me
     * Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
     * Content-Type: application/json
     * 
     * {
     *   "asOf": "2025-03-01"  // 선택: 특정 시점 기준 조회
     * }
     *
     * 응답 예시:
     * {
     *   "success": true,
     *   "message": "학적 정보를 성공적으로 조회했습니다.",
     *   "data": {
     *     "userName": "서혜진",
     *     "userEmail": "student001@univ.edu",
     *     "studentCode": "202500101000",
     *     "academicStatus": "재학",
     *     "admissionRoute": "정시",
     *     "enrolledTerms": 2,
     *     "restPeriod": null,
     *     "facultyName": null,
     *     "departmentName": null,
     *     "expectedGraduateAt": null,
     *     "address": {
     *       "zipCode": "12345",
     *       "mainAddress": "서울특별시 강남구 테헤란로 124",
     *       "detailAddress": "VALUE 아파트 101동 501호"
     *     },
     *     "issuedAt": "2025-03-02T10:00:00Z"
     *   },
     *   "errorCode": null,
     *   "timestamp": "2025-03-02T10:00:01Z"
     * }
     */
    @PostMapping("/me")
    public ResponseEntity<ApiResponse<RegistryResponseDTO>> getMyRegistry(
            HttpServletRequest request,
            @RequestBody(required = false) RegistryRequestDTO requestDTO
    ) {
        try {
            String token = extractToken(request);
            String userEmail = jwtUtil.extractUsername(token);

            logger.info("학적 조회 API 호출 - 사용자: {}", userEmail);

            RegistryResponseDTO registry = requestDTO != null && requestDTO.getAsOf() != null
                    ? registryService.getMyRegistry(userEmail, requestDTO)
                    : registryService.getMyRegistry(userEmail);

            return ResponseEntity.ok(
                ApiResponse.success("학적 정보를 성공적으로 조회했습니다.", registry)
            );

        } catch (Exception e) {
            logger.error("학적 조회 실패", e);
            throw e;
        }
    }

    /**
     * 증명서 발급 이력 저장
     * JWT 토큰에서 사용자 정보를 추출하여 증명서 발급 이력을 저장
     *
     * @param request HTTP 요청 (Authorization 헤더, IP 주소 추출)
     * @param requestDTO 증명서 발급 요청 DTO
     * @return 발급 이력 정보 (발급 ID, 발급 일시)
     *
     * 요청 예시:
     * POST /api/registry/cert/issue
     * Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
     * Content-Type: application/json
     *
     * {
     *   "type": "enrollment",           // 필수: 증명서 유형
     *   "asOf": "2025-03-01",          // 선택: 스냅샷 기준일
     *   "format": "html",              // 선택: 발급 형식 (기본값: html)
     *   "snapshot": { ... }            // 선택: 스냅샷 데이터 (서버 자동 생성 가능)
     * }
     *
     * 응답 예시:
     * {
     *   "success": true,
     *   "message": "증명서 발급 이력이 저장되었습니다.",
     *   "data": {
     *     "issueId": "C20250302-000123",
     *     "issuedAt": "2025-03-02T10:00:02Z"
     *   },
     *   "errorCode": null,
     *   "timestamp": "2025-03-02T10:00:02Z"
     * }
     */
    @PostMapping("/cert/issue")
    public ResponseEntity<ApiResponse<CertIssueResponseDTO>> issueCertificate(
            HttpServletRequest request,
            @RequestBody CertIssueRequestDTO requestDTO
    ) {
        try {
            String token = extractToken(request);
            String userEmail = jwtUtil.extractUsername(token);
            String clientIp = extractClientIp(request);

            logger.info("증명서 발급 API 호출 - 사용자: {}, 유형: {}, IP: {}", 
                        userEmail, requestDTO.getType(), clientIp);

            CertIssueResponseDTO response = certIssueService.issueCertificate(userEmail, requestDTO, clientIp);

            return ResponseEntity.ok(
                ApiResponse.success("증명서 발급 이력이 저장되었습니다.", response)
            );

        } catch (Exception e) {
            logger.error("증명서 발급 실패", e);
            throw e;
        }
    }

    /**
     * Authorization 헤더에서 JWT 토큰 추출
     * Bearer 토큰 형식: "Bearer {token}"
     *
     * @param request HTTP 요청
     * @return JWT 토큰 문자열
     * @throws RuntimeException 토큰이 없거나 형식이 잘못된 경우
     */
    private String extractToken(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        
        if (bearerToken == null || bearerToken.trim().isEmpty()) {
            logger.warn("토큰 없음 - IP: {}", request.getRemoteAddr());
            throw new RuntimeException("인증 토큰이 제공되지 않았습니다.");
        }

        if (!bearerToken.startsWith("Bearer ")) {
            logger.warn("잘못된 토큰 형식 - IP: {}", request.getRemoteAddr());
            throw new RuntimeException("잘못된 토큰 형식입니다. 'Bearer {token}' 형식을 사용하세요.");
        }

        return bearerToken.substring(7);
    }

    /**
     * 클라이언트 IP 주소 추출
     * 프록시/로드밸런서 환경 고려 (X-Forwarded-For 헤더 우선)
     *
     * @param request HTTP 요청
     * @return 클라이언트 IP 주소
     */
    private String extractClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        
        if (ip == null || ip.trim().isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.trim().isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.trim().isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("HTTP_CLIENT_IP");
        }
        if (ip == null || ip.trim().isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("HTTP_X_FORWARDED_FOR");
        }
        if (ip == null || ip.trim().isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }

        // X-Forwarded-For 헤더에 여러 IP가 있을 경우 첫 번째 IP 사용
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }

        return ip;
    }

    /**
     * 학적 정보 존재 여부 확인 (헬스체크용)
     * 
     * @param request HTTP 요청
     * @return 학적 정보 존재 여부
     */
    @GetMapping("/me/exists")
    public ResponseEntity<ApiResponse<Boolean>> checkRegistryExists(HttpServletRequest request) {
        try {
            String token = extractToken(request);
            String userEmail = jwtUtil.extractUsername(token);

            boolean exists = registryService.hasRegistry(userEmail);

            return ResponseEntity.ok(
                ApiResponse.success("학적 정보 존재 여부를 확인했습니다.", exists)
            );

        } catch (Exception e) {
            logger.error("학적 존재 여부 확인 실패", e);
            throw e;
        }
    }
}
