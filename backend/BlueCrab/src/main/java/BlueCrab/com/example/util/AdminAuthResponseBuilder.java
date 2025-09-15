// 작업자 : 성태준
// 관리자 인증 응답 생성 유틸리티 클래스
// 관리자 인증 성공/실패 응답 생성 기능을 담당

package BlueCrab.com.example.util;

// ========== 임포트 구문 ==========
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import lombok.extern.slf4j.Slf4j;

import BlueCrab.com.example.dto.AuthResponse;
import BlueCrab.com.example.entity.AdminTbl;
import java.util.Map;

/* 관리자 인증 응답 생성을 담당하는 유틸리티 클래스
 * 
 * 주요 기능:
 * - 관리자 인증 성공 응답 생성
 * - 관리자 인증 실패 응답 생성
 * - 토큰 정보가 포함된 응답 데이터 생성
 * 
 * 설계 원칙:
 * - 단일 책임: 응답 객체 생성에만 집중
 * - 재사용성: 다양한 관리자 컨트롤러에서 활용 가능
 * - 일관성: 응답 형식의 표준화
 */
@Component
@Slf4j
public class AdminAuthResponseBuilder {
    
    // 관리자 인증 성공 응답 생성
    public ResponseEntity<AuthResponse> buildSuccessResponse(
        AdminJwtTokenBuilder.TokenPair tokens, AdminTbl admin) {
        
        try {
            log.debug("Building success response for admin: {}", admin.getAdminId());
            
            // 관리자 시스템 권한 설정 (null인 경우 0으로 기본 설정)
            int adminSys = admin.getAdminSys() != null ? admin.getAdminSys() : 0;
            
            // 응답 데이터 맵 생성
            Map<String, Object> responseData = new java.util.HashMap<>();
            responseData.put("accessToken", tokens.getAccessToken());
            responseData.put("refreshToken", tokens.getRefreshToken());
            responseData.put("adminSys", adminSys);
            responseData.put("expiresIn", tokens.getExpiresIn());
            responseData.put("adminId", admin.getAdminId());
            responseData.put("adminName", admin.getName());
            
            // 성공 응답 객체 생성
            AuthResponse response = new AuthResponse("이메일 인증 성공! 토큰이 발급되었습니다.", true);
            response.setData(responseData);
            
            log.info("Successfully built auth success response for admin: {}", admin.getAdminId());
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Failed to build success response for admin: {}, Error: {}", 
                admin.getAdminId(), e.getMessage(), e);
            // 예외 발생 시 에러 응답 반환
            return buildErrorResponse("응답 생성 중 오류가 발생했습니다.", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
    
    // 관리자 인증 실패 응답 생성
    public ResponseEntity<AuthResponse> buildErrorResponse(String message, HttpStatus status) {
        log.warn("Building error response - Status: {}, Message: {}", status, message);
        
        AuthResponse response = new AuthResponse(message, false);
        return ResponseEntity.status(status).body(response);
    }
    
    // 인증 코드 발송 성공 응답 생성
    public ResponseEntity<AuthResponse> buildCodeSentResponse(int expiryMinutes) {
        String message = String.format("인증코드가 발송되었습니다. %d분 이내에 인증을 완료해주세요.", expiryMinutes);
        log.debug("Building code sent response with expiry: {} minutes", expiryMinutes);
        
        AuthResponse response = new AuthResponse(message, true);
        return ResponseEntity.ok(response);
    }
}