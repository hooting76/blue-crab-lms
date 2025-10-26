package BlueCrab.com.example.service.impl;

import BlueCrab.com.example.dto.Consultation.ChatMessageDto;
import BlueCrab.com.example.repository.ConsultationRequestRepository;
import BlueCrab.com.example.repository.UserTblRepository;
import BlueCrab.com.example.service.ChatService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 채팅 메시지 관리 Service 구현체
 * Redis를 사용하여 실시간 채팅 메시지를 저장하고 관리합니다.
 * 
 * @author BlueCrab Development Team
 * @version 1.0.0
 * @since 2025-10-24
 */
@Slf4j
@Service
public class ChatServiceImpl implements ChatService {

    private static final String CHAT_KEY_PREFIX = "chat:room:";
    private static final long TTL_HOURS = 36; // 36시간
    private static final int DEFAULT_MESSAGE_COUNT = 50;

    private final StringRedisTemplate stringRedisTemplate;
    private final ConsultationRequestRepository consultationRequestRepository;
    private final UserTblRepository userTblRepository;
    private final ObjectMapper objectMapper;

    public ChatServiceImpl(StringRedisTemplate stringRedisTemplate,
                          ConsultationRequestRepository consultationRequestRepository,
                          UserTblRepository userTblRepository) {
        this.stringRedisTemplate = stringRedisTemplate;
        this.consultationRequestRepository = consultationRequestRepository;
        this.userTblRepository = userTblRepository;
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
    }

    /**
     * Redis 키 생성
     */
    private String getChatKey(Long requestIdx) {
        return CHAT_KEY_PREFIX + requestIdx;
    }

    @Override
    public void saveMessage(ChatMessageDto message) {
        String key = getChatKey(message.getRequestIdx());
        
        try {
            // 메시지를 JSON 문자열로 변환
            String messageJson = objectMapper.writeValueAsString(message);
            
            // Redis List에 추가 (RPUSH - 오른쪽에 추가)
            stringRedisTemplate.opsForList().rightPush(key, messageJson);
            
            // TTL 36시간 재설정 (메시지 전송할 때마다 연장)
            setTTL(message.getRequestIdx());
            
            log.debug("메시지 저장 완료: requestIdx={}, sender={}", 
                message.getRequestIdx(), message.getSender());
            
        } catch (JsonProcessingException e) {
            log.error("메시지 JSON 변환 실패: requestIdx={}", message.getRequestIdx(), e);
            throw new RuntimeException("메시지 저장 실패", e);
        }
    }

    @Override
    public List<ChatMessageDto> getMessages(Long requestIdx, int count) {
        String key = getChatKey(requestIdx);
        
        // Redis에서 최근 N개 메시지 조회 (LRANGE -count -1)
        List<String> messageJsonList = stringRedisTemplate.opsForList()
            .range(key, -count, -1);
        
        if (messageJsonList == null || messageJsonList.isEmpty()) {
            log.debug("메시지 없음: requestIdx={}", requestIdx);
            return new ArrayList<>();
        }
        
        // JSON 문자열을 DTO로 변환
        return messageJsonList.stream()
            .map(this::parseMessage)
            .collect(Collectors.toList());
    }

    @Override
    public List<ChatMessageDto> getAllMessages(Long requestIdx) {
        String key = getChatKey(requestIdx);
        
        // Redis에서 전체 메시지 조회 (LRANGE 0 -1)
        List<String> messageJsonList = stringRedisTemplate.opsForList()
            .range(key, 0, -1);
        
        if (messageJsonList == null || messageJsonList.isEmpty()) {
            log.debug("전체 메시지 없음: requestIdx={}", requestIdx);
            return new ArrayList<>();
        }
        
        log.info("전체 메시지 조회: requestIdx={}, count={}", requestIdx, messageJsonList.size());
        
        // JSON 문자열을 DTO로 변환
        return messageJsonList.stream()
            .map(this::parseMessage)
            .collect(Collectors.toList());
    }

    @Override
    public long getMessageCount(Long requestIdx) {
        String key = getChatKey(requestIdx);
        Long count = stringRedisTemplate.opsForList().size(key);
        return count != null ? count : 0;
    }

    @Override
    public void setTTL(Long requestIdx) {
        String key = getChatKey(requestIdx);
        
        // TTL 36시간 설정
        stringRedisTemplate.expire(key, TTL_HOURS, TimeUnit.HOURS);
        
        log.debug("TTL 설정: requestIdx={}, ttl={}시간", requestIdx, TTL_HOURS);
    }

    @Override
    public boolean isParticipant(Long requestIdx, String userCode) {
        String actualUserCode = convertToUserCode(userCode);

        if (actualUserCode == null) {
            log.warn("채팅 참여자 확인 실패 - 사용자 코드 없음: requestIdx={}, identifier={}", requestIdx, userCode);
            return false;
        }

        return consultationRequestRepository.isParticipant(requestIdx, actualUserCode);
    }

    /**
     * 이메일을 USER_CODE로 변환
     * JWT에서는 이메일(sub)이 전달되지만, DB에는 USER_CODE(학번)로 저장되어 있음
     *
     * @param emailOrUserCode 이메일 또는 USER_CODE
     * @return USER_CODE (학번)
     */
    private String convertToUserCode(String emailOrUserCode) {
        if (emailOrUserCode == null || emailOrUserCode.isBlank()) {
            return null;
        }

        if (!emailOrUserCode.contains("@")) {
            return emailOrUserCode;
        }

        return userTblRepository.findByUserEmail(emailOrUserCode)
            .map(user -> user.getUserCode())
            .orElseGet(() -> {
                log.warn("사용자 이메일로 userCode 찾을 수 없음: {}", emailOrUserCode);
                return null;
            });
    }

    @Override
    public void deleteMessages(Long requestIdx) {
        String key = getChatKey(requestIdx);
        
        // Redis 키 삭제
        Boolean deleted = stringRedisTemplate.delete(key);
        
        if (Boolean.TRUE.equals(deleted)) {
            log.info("메시지 삭제 완료: requestIdx={}", requestIdx);
        } else {
            log.warn("메시지 삭제 실패 (키 없음): requestIdx={}", requestIdx);
        }
    }

    @Override
    public String formatChatLog(Long requestIdx) {
        List<ChatMessageDto> messages = getAllMessages(requestIdx);
        
        if (messages.isEmpty()) {
            return "채팅 내역이 없습니다.";
        }
        
        StringBuilder sb = new StringBuilder();
        sb.append("==================================================\n");
        sb.append("       상담 채팅 기록\n");
        sb.append("==================================================\n\n");
        sb.append("상담 요청 번호: ").append(requestIdx).append("\n");
        sb.append("생성 일시: ").append(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))).append("\n");
        sb.append("총 메시지 수: ").append(messages.size()).append("\n");
        sb.append("\n==================================================\n\n");
        
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        
        for (ChatMessageDto msg : messages) {
                        LocalDateTime sentAt = msg.getSentAt();
                        String formattedSentAt = sentAt != null ? sentAt.format(formatter) : "시간 정보 없음";

                        sb.append("[").append(formattedSentAt).append("]\n");
                        sb.append("발신: ")
                            .append(msg.getSenderName() != null ? msg.getSenderName() : "알 수 없음")
                            .append(" (")
                            .append(msg.getSender() != null ? msg.getSender() : "-" )
                            .append(")\n");
                        sb.append("내용: ").append(msg.getContent() != null ? msg.getContent() : "").append("\n");
            sb.append("--------------------------------------------------\n");
        }
        
        sb.append("\n==================================================\n");
        sb.append("채팅 기록 끝\n");
        sb.append("==================================================\n");
        
        return sb.toString();
    }

    /**
     * JSON 문자열을 ChatMessageDto로 파싱
     */
    private ChatMessageDto parseMessage(String messageJson) {
        try {
            return objectMapper.readValue(messageJson, ChatMessageDto.class);
        } catch (JsonProcessingException e) {
            log.error("메시지 JSON 파싱 실패: {}", messageJson, e);
            throw new RuntimeException("메시지 파싱 실패", e);
        }
    }
}
