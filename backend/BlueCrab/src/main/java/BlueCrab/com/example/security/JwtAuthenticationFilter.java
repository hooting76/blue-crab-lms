package BlueCrab.com.example.security;

import BlueCrab.com.example.dto.ApiResponse;
import BlueCrab.com.example.util.JwtUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.ExpiredJwtException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(JwtAuthenticationFilter.class);

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private UserDetailsService userDetailsService;

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response,
                                  @NonNull FilterChain chain) throws ServletException, IOException {

        final String requestTokenHeader = request.getHeader("Authorization");

        String username = null;
        String jwtToken = null;
        boolean isTokenExpired = false;

        // JWT Token은 "Bearer token" 형식으로 전달됨
        if (requestTokenHeader != null && requestTokenHeader.startsWith("Bearer ")) {
            jwtToken = requestTokenHeader.substring(7);
            try {
                username = jwtUtil.extractUsername(jwtToken);
            } catch (ExpiredJwtException e) {
                // 토큰이 만료된 경우 - 클라이언트에게 알리기 위한 헤더 추가
                logger.warn("JWT Token has expired: {}", e.getMessage());
                response.setHeader("X-Token-Expired", "true");
                isTokenExpired = true;
                writeExpiredTokenResponse(request, response);
                return;
            } catch (Exception e) {
                logger.error("Unable to get JWT Token", e);
            }
        }

        // 토큰이 있고 SecurityContext에 인증 정보가 없는 경우
        if (username != null && SecurityContextHolder.getContext().getAuthentication() == null && !isTokenExpired) {

            // 성태준 추가 - 관리자 이메일 인증 시스템: 세션 토큰 처리 로직
            // 세션 토큰은 인증에 사용하지 않음 (이메일 인증 등의 임시 토큰)
            if (jwtUtil.isSessionToken(jwtToken)) {
                logger.debug("Session token detected - skipping authentication for: {}", username);
                chain.doFilter(request, response);
                return;
            }
            // 성태준 추가 끝

            UserDetails userDetails = this.userDetailsService.loadUserByUsername(username);

            // 토큰이 유효하고 Access Token인지 확인
            if (jwtUtil.validateToken(jwtToken, userDetails.getUsername()) &&
                jwtUtil.isAccessToken(jwtToken)) {

                UsernamePasswordAuthenticationToken usernamePasswordAuthenticationToken =
                    new UsernamePasswordAuthenticationToken(
                        userDetails, null, userDetails.getAuthorities());

                usernamePasswordAuthenticationToken
                    .setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                SecurityContextHolder.getContext().setAuthentication(usernamePasswordAuthenticationToken);
            }
        }
        chain.doFilter(request, response);
    }

    private void writeExpiredTokenResponse(HttpServletRequest request, HttpServletResponse response) throws IOException {
        applyCorsHeaders(request, response);
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");

        ApiResponse<Object> errorResponse = ApiResponse.failure(
            "토큰이 만료되었습니다. 리프레시 토큰을 사용하여 새 토큰을 발급받아주세요."
        );

        final ObjectMapper mapper = new ObjectMapper();
        mapper.writeValue(response.getOutputStream(), errorResponse);
    }

    private void applyCorsHeaders(HttpServletRequest request, HttpServletResponse response) {
        String origin = request.getHeader("Origin");
        if (origin != null && !origin.isEmpty()) {
            response.setHeader("Access-Control-Allow-Origin", origin);
            response.setHeader("Access-Control-Allow-Credentials", "true");
            response.setHeader("Vary", "Origin");
        }
    }
}
