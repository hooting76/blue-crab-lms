package BlueCrab.com.example.controller;

import BlueCrab.com.example.dto.ApiResponse;
import BlueCrab.com.example.dto.FindIdRequest;
import BlueCrab.com.example.dto.FindIdResponse;
import BlueCrab.com.example.service.UserTblService;
import BlueCrab.com.example.util.AccountRecoveryRateLimiter;
import BlueCrab.com.example.util.RequestUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;

/**
 * ID 찾기 기능을 위한 REST API 컨트롤러
 * 사용자가 학번, 이름, 전화번호를 입력하여 이메일 주소를 찾는 기능을 제공
 *
 * 주요 기능:
 * - 사용자 신원 확인을 통한 이메일 주소 찾기
 * - 이메일 마스킹 처리로 보안 강화
 * - 레이트 리미팅 및 보안 로깅
 *
 * API 응답 형식:
 * - 모든 응답은 ApiResponse<FindIdResponse> 형식으로 통일
 * - 성공/실패 여부와 관계없이 일관된 메시지 제공
 * - 보안을 위해 계정 존재 여부를 직접적으로 노출하지 않음
 *
 * 보안 고려사항:
 * - 타이밍 공격 방지를 위한 일관된 응답 시간
 * - 사용자 입력값 검증 및 sanitization
 * - 클라이언트 IP 및 User-Agent 로깅
 * - 레이트 리미팅 (추후 구현)
 *
 * @author BlueCrab Development Team
 * @version 1.0.0
 * @since 2024-01-01
 */
@RestController
@RequestMapping("/api/account")
public class FindIdController {
    
    private static final Logger logger = LoggerFactory.getLogger(FindIdController.class);
    
    @Autowired
    private UserTblService userTblService;
    
    @Autowired
    private AccountRecoveryRateLimiter rateLimiter;
    
    /**
     * ID 찾기 API 엔드포인트
     * 학번, 이름, 전화번호를 입력받아 해당하는 사용자의 이메일 주소를 마스킹하여 반환
     *
     * HTTP Method: POST
     * Content-Type: application/json
     * 
     * @param findIdRequest 사용자 입력 정보 (학번, 이름, 전화번호)
     * @param request HTTP 요청 객체 (클라이언트 정보 추출용)
     * @return ResponseEntity<ApiResponse<FindIdResponse>> API 응답
     *
     * 요청 예시:
     * POST /api/account/FindId
     * {
     *   "userCode": 202012345,
     *   "userName": "홍길동",
     *   "userPhone": "01012345678"
     * }
     *
     * 성공 응답 예시:
     * {
     *   "success": true,
     *   "message": "이메일 주소를 찾았습니다.",
     *   "data": {
     *     "success": true,
     *     "maskedEmail": "h****g@university.edu",
     *     "message": "이메일 주소를 찾았습니다."
     *   },
     *   "timestamp": "2024-01-01T12:00:00.000Z"
     * }
     *
     * 실패 응답 예시:
     * {
     *   "success": false,
     *   "message": "해당 정보로 만들어진 Id가 존재 하지 않습니다.",
     *   "data": {
     *     "success": false,
     *     "maskedEmail": null,
     *     "message": "해당 정보로 만들어진 Id가 존재 하지 않습니다."
     *   },
     *   "timestamp": "2024-01-01T12:00:00.000Z"
     * }
     */
    @PostMapping("/FindId")
    public ResponseEntity<ApiResponse<FindIdResponse>> findId(
            @Valid @RequestBody FindIdRequest findIdRequest,
            HttpServletRequest request) {
        
        long startTime = System.currentTimeMillis();
        String clientIp = RequestUtils.getClientIpAddress(request);
        String userAgent = request.getHeader("User-Agent");
        
        try {
            // 보안 로깅 - 요청 시작
            logger.info("ID 찾기 요청 시작 - IP: {}, User-Agent: {}, UserCode: {}", 
                clientIp, userAgent, findIdRequest.getUserCode());
            
            // 레이트 리미팅 검사
            if (!rateLimiter.isAllowedForFindId(clientIp, userAgent)) {
                long waitTime = rateLimiter.getRemainingWaitTime(clientIp, "find_id");
                logger.warn("ID 찾기 레이트 리미트 도달 - IP: {}, 대기시간: {}초", clientIp, waitTime);
                
                FindIdResponse rateLimitResponse = new FindIdResponse(false, null, 
                    waitTime > 0 ? String.format("너무 많은 시도로 인해 %d초 후 다시 시도해주세요.", waitTime) 
                                 : "일일 시도 횟수를 초과했습니다. 내일 다시 시도해주세요.");
                return ResponseEntity.ok(ApiResponse.success(rateLimitResponse.getMessage(), rateLimitResponse));
            }
            
            // 비즈니스 로직 수행
            FindIdResponse response = userTblService.findUserEmail(
                findIdRequest.getUserCode(),
                findIdRequest.getUserName(),
                findIdRequest.getUserPhone()
            );
            
            // 성공/실패에 따른 로깅 및 레이트 리미팅 기록
            if (response.isSuccess()) {
                logger.info("ID 찾기 성공 - IP: {}, UserCode: {}, 실행시간: {}ms", 
                    clientIp, findIdRequest.getUserCode(), 
                    System.currentTimeMillis() - startTime);
                rateLimiter.recordSuccess(clientIp, "find_id");
            } else {
                logger.warn("ID 찾기 실패 - IP: {}, UserCode: {}, 실행시간: {}ms", 
                    clientIp, findIdRequest.getUserCode(), 
                    System.currentTimeMillis() - startTime);
                rateLimiter.recordFailure(clientIp, "find_id");
            }
            
            // 성공 여부와 관계없이 200 OK 반환 (보안상 실패 정보 노출 방지)
            return ResponseEntity.ok(ApiResponse.success(response.getMessage(), response));
            
        } catch (Exception e) {
            // 예외 발생 시 로깅 및 일관된 실패 응답 반환
            logger.error("ID 찾기 처리 중 예외 발생 - IP: {}, UserCode: {}, Error: {}", 
                clientIp, findIdRequest.getUserCode(), e.getMessage(), e);
            
            // 예외 상황도 실패로 기록
            rateLimiter.recordFailure(clientIp, "find_id");
            
            FindIdResponse failureResponse = FindIdResponse.failure();
            return ResponseEntity.ok(ApiResponse.success(failureResponse.getMessage(), failureResponse));
        }
    }
}