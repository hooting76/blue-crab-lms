package BlueCrab.com.example.controller;

import BlueCrab.com.example.dto.Consultation.ChatMessageDto;
import BlueCrab.com.example.entity.ConsultationRequest;
import BlueCrab.com.example.entity.UserTbl;
import BlueCrab.com.example.repository.ConsultationRequestRepository;
import BlueCrab.com.example.repository.UserTblRepository;
import BlueCrab.com.example.service.ChatService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.security.Principal;
import java.time.LocalDateTime;

/**
 * WebSocket 채팅 Controller
 * STOMP 프로토콜을 사용한 실시간 채팅 메시지 처리
 * 
 * 개인 큐 방식:
 * - 클라이언트: /app/chat.send로 메시지 전송
 * - 서버: /user/{userCode}/queue/chat로 메시지 전달
 * - 구독: /user/queue/chat (본인의 큐만 자동 구독)
 * 
 * @author BlueCrab Development Team
 * @version 1.0.0
 * @since 2025-10-24
 */
@Slf4j
@Controller
public class ChatController {

    private final SimpMessagingTemplate messagingTemplate;
    private final ChatService chatService;
    private final ConsultationRequestRepository consultationRequestRepository;
    private final UserTblRepository userTblRepository;

    public ChatController(SimpMessagingTemplate messagingTemplate,
                         ChatService chatService,
                         ConsultationRequestRepository consultationRequestRepository,
                         UserTblRepository userTblRepository) {
        this.messagingTemplate = messagingTemplate;
        this.chatService = chatService;
        this.consultationRequestRepository = consultationRequestRepository;
        this.userTblRepository = userTblRepository;
    }

    /**
     * 채팅 메시지 전송
     * 
     * @param message 채팅 메시지
     * @param principal 인증된 사용자 (JWT에서 추출한 userCode)
     */
    @MessageMapping("/chat.send")
    public void sendMessage(@Payload ChatMessageDto message, Principal principal) {
        String senderCode = principal.getName();
        
        log.info("메시지 수신: requestIdx={}, sender={}", message.getRequestIdx(), senderCode);

        try {
            // 1. 권한 검증 (해당 상담방의 참여자인지)
            if (!chatService.isParticipant(message.getRequestIdx(), senderCode)) {
                log.warn("권한 없음: requestIdx={}, userCode={}", message.getRequestIdx(), senderCode);
                throw new SecurityException("해당 상담방에 접근 권한이 없습니다.");
            }

            // 2. 메시지 정보 설정
            message.setSender(senderCode);
            message.setSenderName(getSenderName(senderCode));
            message.setSentAt(LocalDateTime.now());

            // 3. Redis에 저장 + TTL 36시간 연장
            chatService.saveMessage(message);

            // 4. DB 업데이트 (lastActivityAt)
            updateLastActivity(message.getRequestIdx());
            
            // 5. 첫 메시지인 경우 상담 시작 처리 (트리거 미발동 대비)
            markStartedIfNeeded(message.getRequestIdx());

            // 6. 상대방 찾기 (서버에서 결정 - 보안)
            String recipientCode = getPartnerUserCode(message.getRequestIdx(), senderCode);

            log.info("메시지 전송: recipient={}, content={}", recipientCode, message.getContent());

            // 7. 상대방에게 개인 큐로 전송
            messagingTemplate.convertAndSendToUser(
                recipientCode,
                "/queue/chat",
                message
            );

            // 8. 본인에게도 에코 (전송 확인용)
            messagingTemplate.convertAndSendToUser(
                senderCode,
                "/queue/chat",
                message
            );

        } catch (SecurityException e) {
            log.error("메시지 전송 실패 (권한): {}", e.getMessage());
            // 클라이언트에게 에러 메시지 전송 (선택 사항)
            sendErrorToUser(senderCode, "권한이 없습니다.");
            
        } catch (Exception e) {
            log.error("메시지 전송 실패: requestIdx={}", message.getRequestIdx(), e);
            sendErrorToUser(senderCode, "메시지 전송에 실패했습니다.");
        }
    }

    /**
     * 사용자 이름 조회
     */
    private String getSenderName(String userCode) {
        try {
            return userTblRepository.findByUserCode(userCode)
                .map(UserTbl::getUserName)
                .orElse("알 수 없음");
        } catch (Exception e) {
            log.warn("사용자 이름 조회 실패: userCode={}", userCode);
            return "알 수 없음";
        }
    }

    /**
     * 상대방 userCode 조회
     */
    private String getPartnerUserCode(Long requestIdx, String myUserCode) {
        ConsultationRequest consultation = consultationRequestRepository.findById(requestIdx)
            .orElseThrow(() -> new RuntimeException("상담을 찾을 수 없습니다."));

        // 내가 요청자면 수신자(교수) 반환, 아니면 요청자(학생) 반환
        if (consultation.getRequesterUserCode().equals(myUserCode)) {
            return consultation.getRecipientUserCode();
        } else {
            return consultation.getRequesterUserCode();
        }
    }

    /**
     * 마지막 활동 시간 업데이트
     */
    private void updateLastActivity(Long requestIdx) {
        try {
            ConsultationRequest consultation = consultationRequestRepository.findById(requestIdx)
                .orElseThrow(() -> new RuntimeException("상담을 찾을 수 없습니다."));

            consultation.setLastActivityAt(LocalDateTime.now());
            consultationRequestRepository.save(consultation);
            
        } catch (Exception e) {
            log.error("lastActivityAt 업데이트 실패: requestIdx={}", requestIdx, e);
        }
    }

    /**
     * 첫 메시지 시 상담 시작 처리 (트리거 미발동 대비)
     * consultation_status가 SCHEDULED이고 started_at이 null인 경우 자동 시작
     */
    private void markStartedIfNeeded(Long requestIdx) {
        try {
            ConsultationRequest consultation = consultationRequestRepository.findById(requestIdx)
                .orElseThrow(() -> new RuntimeException("상담을 찾을 수 없습니다."));

            // SCHEDULED 상태이고 started_at이 null인 경우
            if ("SCHEDULED".equals(consultation.getConsultationStatus()) 
                && consultation.getStartedAt() == null) {
                
                consultation.setConsultationStatus("IN_PROGRESS");
                consultation.setStartedAt(LocalDateTime.now());
                consultation.setLastActivityAt(LocalDateTime.now());
                consultationRequestRepository.save(consultation);
                
                log.info("첫 메시지로 상담 자동 시작: requestIdx={}", requestIdx);
            }
            
        } catch (Exception e) {
            log.error("상담 시작 처리 실패: requestIdx={}", requestIdx, e);
        }
    }

    /**
     * 에러 메시지를 특정 사용자에게 전송
     */
    private void sendErrorToUser(String userCode, String errorMessage) {
        try {
            messagingTemplate.convertAndSendToUser(
                userCode,
                "/queue/errors",
                errorMessage
            );
        } catch (Exception e) {
            log.error("에러 메시지 전송 실패: userCode={}", userCode, e);
        }
    }
}
