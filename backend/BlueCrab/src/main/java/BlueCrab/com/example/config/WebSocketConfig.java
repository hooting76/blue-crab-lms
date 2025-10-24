package BlueCrab.com.example.config;

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
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

import java.util.Collections;

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

    public WebSocketConfig(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
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
                    // Authorization 헤더에서 JWT 토큰 추출
                    String token = accessor.getFirstNativeHeader("Authorization");

                    if (token != null && token.startsWith("Bearer ")) {
                        token = token.substring(7);  // "Bearer " 제거
                    }

                    // JWT 토큰 검증
                    if (token == null || !jwtUtil.validateToken(token)) {
                        log.warn("WebSocket 연결 실패: 유효하지 않은 JWT 토큰");
                        throw new IllegalArgumentException("Invalid JWT token");
                    }

                    // JWT에서 userCode 추출
                    String userCode = jwtUtil.getUserCode(token);
                    
                    // Spring Security User 객체 생성 및 설정
                    UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(userCode, null, Collections.emptyList());
                    
                    accessor.setUser(authentication);
                    
                    log.info("WebSocket 연결 성공: userCode={}", userCode);
                }

                return message;
            }
        });
    }
}
