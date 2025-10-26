package BlueCrab.com.example.config;

import BlueCrab.com.example.repository.UserTblRepository;
import BlueCrab.com.example.util.JwtUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.util.Collections;
import java.util.Map;

/**
 * WebSocket 설정
 * STOMP 프로토콜을 사용한 실시간 채팅 기능 구현
 * 
 * @author BlueCrab Development Team
 * @version 1.0.0
 * @since 2025-10-24
 */
@Slf4j
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final JwtUtil jwtUtil;
    private final UserTblRepository userTblRepository;

    public WebSocketConfig(JwtUtil jwtUtil, UserTblRepository userTblRepository) {
        this.jwtUtil = jwtUtil;
        this.userTblRepository = userTblRepository;
    }

    /**
     * 메시지 브로커 설정
     * - /queue: 개인 메시지 전용 (1:1 채팅)
     * - /topic: 그룹 메시지 (사용 안 함)
     * - /app: 클라이언트에서 서버로 메시지 전송 시 prefix
     * - /user: 개인 큐 활성화
     */
    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        // Simple in-memory broker 활성화
        config.enableSimpleBroker("/queue", "/topic");
        
        // 클라이언트 → 서버 메시지 prefix
        config.setApplicationDestinationPrefixes("/app");
        
        // 개인 큐 prefix 설정 (기본값: /user)
        config.setUserDestinationPrefix("/user");
        
        log.info("WebSocket 메시지 브로커 설정 완료");
    }

    /**
     * STOMP 엔드포인트 등록
     * 클라이언트가 WebSocket 연결을 시작하는 엔드포인트
     *
     * - /ws/chat: WebSocket 연결 엔드포인트
     * - SockJS fallback 지원 (WebSocket 미지원 브라우저용)
     */
    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws/chat")
                .setAllowedOriginPatterns("*")  // CORS 설정 (프로덕션에서는 구체적으로 지정)
                .addInterceptors(new HandshakeInterceptor() {
                    @Override
                    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                                 WebSocketHandler wsHandler, Map<String, Object> attributes) throws Exception {
                        // 쿼리 파라미터에서 JWT 토큰 추출
                        if (request instanceof ServletServerHttpRequest) {
                            ServletServerHttpRequest servletRequest = (ServletServerHttpRequest) request;
                            String token = servletRequest.getServletRequest().getParameter("token");

                            if (token != null && !token.isEmpty()) {
                                // WebSocket 세션 속성에 토큰 저장
                                attributes.put("token", token);
                                log.info("WebSocket 핸드셰이크: JWT 토큰 확인됨");
                            } else {
                                log.warn("WebSocket 핸드셰이크: JWT 토큰 없음");
                            }
                        }
                        return true;
                    }

                    @Override
                    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                             WebSocketHandler wsHandler, Exception exception) {
                        // 핸드셰이크 완료 후 처리 (필요시)
                    }
                })
                .withSockJS();  // SockJS fallback 지원

        log.info("STOMP 엔드포인트 등록 완료: /ws/chat");
    }

    /**
     * 클라이언트 인바운드 채널 설정
     * JWT 인증 인터셉터 추가
     */
    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(new ChannelInterceptor() {
            @Override
            public Message<?> preSend(Message<?> message, MessageChannel channel) {
                StompHeaderAccessor accessor =
                    MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

                // CONNECT 명령 시 JWT 인증 처리
                if (accessor != null && StompCommand.CONNECT.equals(accessor.getCommand())) {
                    String token = null;

                    // 1. 먼저 WebSocket 세션 속성에서 토큰 확인 (쿼리 파라미터로 전달된 경우)
                    Map<String, Object> sessionAttributes = accessor.getSessionAttributes();
                    if (sessionAttributes != null && sessionAttributes.containsKey("token")) {
                        token = (String) sessionAttributes.get("token");
                        log.info("토큰 확인: WebSocket 세션 속성에서 추출");
                    }

                    // 2. 세션 속성에 없으면 Authorization 헤더에서 추출 시도
                    if (token == null || token.isEmpty()) {
                        token = accessor.getFirstNativeHeader("Authorization");
                        if (token != null && token.startsWith("Bearer ")) {
                            token = token.substring(7);  // "Bearer " 제거
                            log.info("토큰 확인: Authorization 헤더에서 추출");
                        }
                    }

                    // JWT 토큰 검증
                    if (token == null || token.isEmpty() || !jwtUtil.validateToken(token)) {
                        log.warn("WebSocket 연결 실패: 유효하지 않은 JWT 토큰");
                        throw new IllegalArgumentException("Invalid JWT token");
                    }

                    // JWT에서 사용자 식별자 추출 후 USER_CODE로 정규화
                    String identifier = jwtUtil.getUserCode(token);
                    String resolvedUserCode = resolveUserCode(identifier, token);

                    if (resolvedUserCode == null || resolvedUserCode.isBlank()) {
                        log.warn("WebSocket 연결: USER_CODE를 확인할 수 없어 원본 식별자를 사용합니다. identifier={}"
                            , identifier);
                        resolvedUserCode = identifier;
                    }

                    UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(resolvedUserCode, null, Collections.emptyList());

                    accessor.setUser(authentication);

                    log.info("WebSocket 연결 성공: principal={}", resolvedUserCode);
                }

                return message;
            }
        });
    }

    private String resolveUserCode(String identifier, String token) {
        try {
            if (identifier != null && !identifier.isBlank()) {
                if (!identifier.contains("@")) {
                    return identifier;
                }

                return userTblRepository.findByUserEmail(identifier)
                    .map(user -> user.getUserCode())
                    .orElse(null);
            }

            Integer userId = jwtUtil.extractUserId(token);
            if (userId != null) {
                return userTblRepository.findById(userId)
                    .map(user -> user.getUserCode())
                    .orElse(null);
            }
        } catch (Exception e) {
            log.error("WebSocket USER_CODE 변환 실패: identifier={}", identifier, e);
        }

        return null;
    }
}
