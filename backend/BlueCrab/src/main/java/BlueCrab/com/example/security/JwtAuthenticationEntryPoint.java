package BlueCrab.com.example.security;

import BlueCrab.com.example.dto.ApiResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * JWT 인증 실패 시 처리하는 Spring Security AuthenticationEntryPoint 구현체
 * 인증되지 않은 요청에 대해 표준화된 401 Unauthorized 응답을 반환하는 핵심 컴포넌트
 *
 * 주요 기능:
 * - JWT 토큰이 없는 요청 처리
 * - 유효하지 않은 JWT 토큰 요청 처리
 * - 만료된 JWT 토큰 요청 처리
 * - JSON 형식의 일관된 에러 응답 반환
 *
 * 작동 시점:
 * - Spring Security 필터 체인에서 인증 실패가 발생한 경우
 * - 컨트롤러에 요청이 도달하기 전에 호출됨
 * - GlobalExceptionHandler보다 먼저 실행되어 인증 관련 예외를 처리
 *
 * 처리하는 인증 실패 상황:
 * 1. Authorization 헤더가 없는 경우
 * 2. 잘못된 형식의 JWT 토큰
 * 3. 만료된 JWT 토큰 (ExpiredJwtException)
 * 4. 서명 검증 실패 토큰 (SignatureException)
 * 5. 지원하지 않는 JWT 토큰 형식
 * 6. 기타 Spring Security 인증 예외
 *
 * 응답 형식:
 * HTTP 401 Unauthorized
 * Content-Type: application/json
 * Body: {
 *   "success": false,
 *   "message": "인증이 필요합니다. 유효한 Access Token을 제공하거나 토큰을 갱신해주세요.",
 *   "data": null,
 *   "timestamp": "에러 발생 시간"
 * }
 *
 * 다른 인증 실패 처리 방식과의 차이점:
 * - GlobalExceptionHandler: 컨트롤러 레벨에서 발생한 예외 처리
 * - 이 클래스는 필터 레벨에서 미인증 요청을 차단
 *
 * 보안 고려사항:
 * - 구체적인 에러 메시지를 노출하지 않아 정보 유출 방지
 * - 일관된 에러 응답 형식으로 클라이언트 처리 용이
 * - 로그에는 상세한 예외 정보 기록 가능
 *
 * ⚠️ 추후 개선 가능성:
 * - 에러 코드 분류 (토큰 만료, 잘못된 토큰 등)
 * - 리프레시 토큰 자동 재발급 기능 추가
 * - 클라이언트 타입별 다른 응답 형식 지원
 * - Rate Limiting 적용으로 브루트 포스 공격 방지
 *
 * 의존성:
 * - ApiResponse: 표준화된 응답 객체
 * - ObjectMapper: JSON 직렬화
 *
 * @author BlueCrab Development Team
 * @version 1.0.0
 * @since 2024-01-01
 */
@Component
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {

    /**
     * 인증 실패 시 호출되는 핵심 메서드
     * Spring Security가 자동으로 호출하며, 인증되지 않은 요청에 대한 처리를 수행
     *
     * 처리 단계:
     * 1. HTTP 응답 헤더 설정 (Content-Type, Status Code)
     * 2. 표준화된 에러 응답 객체 생성
     * 3. JSON 형식으로 응답 본문 작성
     * 4. 클라이언트에게 응답 전송
     *
     * HTTP 응답 설정:
     * - Status: 401 Unauthorized
     * - Content-Type: application/json;charset=UTF-8
     *
     * 에러 메시지 전략:
     * - 구체적인 실패 이유를 노출하지 않음 (보안)
     * - 클라이언트가 이해하기 쉬운 일반적인 메시지 사용
     * - 토큰 재발급 유도하는 내용 포함
     *
     * @param request HTTP 요청 객체 (실패한 요청 정보)
     * @param response HTTP 응답 객체 (401 응답을 작성할 객체)
     * @param authException 발생한 인증 예외 (구체적인 실패 이유)
     * @throws IOException 응답 작성 중 입출력 오류 발생 시
     *
     * 사용 예시:
     * // Spring Security가 자동으로 호출
     * commence(request, response, new BadCredentialsException("Invalid token"));
     *
     * // 클라이언트가 받는 응답:
     * // HTTP 401 Unauthorized
     * // {
     * //   "success": false,
     * //   "message": "인증이 필요합니다...",
     * //   "data": null,
     * //   "timestamp": "2024-01-01T12:00:00"
     * // }
     */
    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                        AuthenticationException authException) throws IOException {

        // 단계 1: HTTP 응답 헤더 설정
        // JSON 형식의 응답을 명시하고 401 상태 코드를 설정
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);

        // 단계 2: 표준화된 에러 응답 객체 생성
        // ApiResponse.failure()를 사용하여 일관된 형식의 에러 응답 생성
        // 구체적인 에러 메시지를 노출하지 않고 일반적인 인증 필요 메시지 사용
        ApiResponse<Object> errorResponse = ApiResponse.failure(
            "인증이 필요합니다. 유효한 Access Token을 제공하거나 토큰을 갱신해주세요."
        );

        // 단계 3: JSON 직렬화 및 응답 전송
        // ObjectMapper를 사용하여 ApiResponse 객체를 JSON 문자열로 변환
        // response.getOutputStream()을 통해 HTTP 응답 본문에 작성
        final ObjectMapper mapper = new ObjectMapper();
        mapper.writeValue(response.getOutputStream(), errorResponse);
    }
}
