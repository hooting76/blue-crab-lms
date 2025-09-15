// 작업자 : 성태준
// 관리자 인증 응답 생성 유틸리티 클래스
// 관리자 인증 성공/실패 응답 생성 기능을 담당

package BlueCrab.com.example.util;

// ========== 임포트 구문 ==========

// ========== Java 표준 라이브러리 ==========
import java.util.Map;

// ========== 외부 라이브러리 ==========
import lombok.extern.slf4j.Slf4j;

// ========== Spring Framework ==========
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

// ========== 프로젝트 내부 클래스 ==========
import BlueCrab.com.example.dto.AuthResponse;
import BlueCrab.com.example.entity.AdminTbl;

@Component
@Slf4j
public class AdminAuthResponseBuilder {
    // ========== 관리자 인증 응답 생성 메서드 ==========
    
    // 관리자 인증 성공 응답 생성
    public ResponseEntity<AuthResponse> buildSuccessResponse(
        AdminJwtTokenBuilder.TokenPair tokens, AdminTbl admin) {
        // 토큰과 관리자 정보를 받아 성공 응답 생성
        
        // try-catch 블록으로 예외 처리
        try {
            log.debug("Building success response for admin: {}", admin.getAdminId());
            // 디버그 로그 기록
            
            int adminSys = admin.getAdminSys() != null ? admin.getAdminSys() : 0;
            // 관리자 시스템 권한 설정 (null인 경우 0으로 기본 설정)
            
            Map<String, Object> responseData = new java.util.HashMap<>(); // 응답 데이터 맵 생성
            responseData.put("accessToken", tokens.getAccessToken()); // 액세스 토큰
            responseData.put("refreshToken", tokens.getRefreshToken()); // 리프레시 토큰
            responseData.put("adminSys", adminSys); // 시스템 권한
            responseData.put("expiresIn", tokens.getExpiresIn()); // 액세스 토큰 만료 시간 (초 단위)
            responseData.put("adminId", admin.getAdminId()); // 관리자 ID
            responseData.put("adminName", admin.getName()); // 관리자 이름
            
            AuthResponse response = new AuthResponse("이메일 인증 성공! 토큰이 발급되었습니다.", true);
            // 성공 메시지와 상태 설정
            response.setData(responseData);
            // 응답 데이터 설정
            
            log.info("Successfully built auth success response for admin: {}", admin.getAdminId());
            // 성공 로그 기록
            return ResponseEntity.ok(response);
            // 200 OK 응답 반환
            
        } catch (Exception e) {
            // 예외 처리
            log.error("Failed to build success response for admin: {}, Error: {}", 
                admin.getAdminId(), e.getMessage(), e);
            // 예외 발생 시 에러 응답 반환
            return buildErrorResponse("응답 생성 중 오류가 발생했습니다.", HttpStatus.INTERNAL_SERVER_ERROR);
            // 500 Internal Server Error 응답 반환
        } // try-catch 끝
    } // buildSuccessResponse 끝
    
    // 관리자 인증 실패 응답 생성
    public ResponseEntity<AuthResponse> buildErrorResponse(String message, HttpStatus status) {
        // ResponseEntity<AuthResponse> 형식의 에러 응답 생성
        // 메시지와 HTTP 상태 코드를 매개변수로 받음
        log.warn("Building error response - Status: {}, Message: {}", status, message);
        // 경고 로그 기록
        
        AuthResponse response = new AuthResponse(message, false);
        // 실패 메시지와 상태 설정
        return ResponseEntity.status(status).body(response);
        // 실패 응답 반환
    } // buildErrorResponse 끝
    
    // 인증 코드 발송 성공 응답 생성
    public ResponseEntity<AuthResponse> buildCodeSentResponse(int expiryMinutes) {
        // ResponseEntity<AuthResponse> 형식의 성공 응답 생성
        String message = String.format("인증코드가 발송되었습니다. %d분 이내에 인증을 완료해주세요.", expiryMinutes);
        // 성공 메시지 생성
        log.debug("Building code sent response with expiry: {} minutes", expiryMinutes);
        // 디버그 로그 기록
        
        AuthResponse response = new AuthResponse(message, true);
        // 성공 메시지와 상태 설정
        return ResponseEntity.ok(response);
        // 성공 응답 반환
    } // buildCodeSentResponse 끝
} // AdminAuthResponseBuilder 클래스 끝