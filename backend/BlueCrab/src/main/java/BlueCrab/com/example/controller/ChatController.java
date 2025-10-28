package BlueCrab.com.example.controller;

import BlueCrab.com.example.dto.Consultation.ChatMessageDto;
import BlueCrab.com.example.dto.Consultation.ChatReadReceiptDto;
import BlueCrab.com.example.dto.Consultation.ConsultationReadReceiptDto;
import BlueCrab.com.example.entity.ConsultationRequest;
import BlueCrab.com.example.entity.UserTbl;
import BlueCrab.com.example.enums.ConsultationStatus;
import BlueCrab.com.example.enums.RequestStatus;
import BlueCrab.com.example.repository.ConsultationRequestRepository;
import BlueCrab.com.example.repository.UserTblRepository;
import BlueCrab.com.example.service.ChatService;
import BlueCrab.com.example.service.ConsultationRequestService;
import BlueCrab.com.example.service.notification.ChatNotificationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.security.Principal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

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

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final SimpMessagingTemplate messagingTemplate;
    private final ChatService chatService;
    private final ConsultationRequestRepository consultationRequestRepository;
    private final UserTblRepository userTblRepository;
    private final ConsultationRequestService consultationRequestService;
    private final ChatNotificationService chatNotificationService;

    public ChatController(SimpMessagingTemplate messagingTemplate,
                         ChatService chatService,
                         ConsultationRequestRepository consultationRequestRepository,
                         UserTblRepository userTblRepository,
                         ConsultationRequestService consultationRequestService,
                         ObjectProvider<ChatNotificationService> chatNotificationServiceProvider) {
        this.messagingTemplate = messagingTemplate;
        this.chatService = chatService;
        this.consultationRequestRepository = consultationRequestRepository;
        this.userTblRepository = userTblRepository;
        this.consultationRequestService = consultationRequestService;
        this.chatNotificationService = chatNotificationServiceProvider.getIfAvailable();
    }

    /**
     * 채팅 메시지 전송
     * 
     * @param message 채팅 메시지
     * @param principal 인증된 사용자 (JWT에서 추출한 userCode)
     */
    @MessageMapping("/chat.send")
    public void sendMessage(@Payload ChatMessageDto message, Principal principal) {
        String senderEmailOrCode = principal.getName();
        String senderCode = convertToUserCode(senderEmailOrCode);

        if (senderCode == null) {
            log.warn("메시지 전송 실패 - 사용자 식별 불가: requestIdx={}, identifier={}",
                message.getRequestIdx(), senderEmailOrCode);
            sendErrorToUser(senderEmailOrCode, "사용자 정보를 확인할 수 없습니다. 다시 로그인해주세요.");
            return;
        }

        Long requestIdx = message.getRequestIdx();
        if (requestIdx == null) {
            log.warn("메시지 전송 실패 - 상담 ID 누락: sender={}", senderCode);
            sendErrorToUser(senderCode, "상담 정보를 확인할 수 없습니다. 다시 시도해주세요.");
            return;
        }

        log.info("메시지 수신: requestIdx={}, sender={}", requestIdx, senderCode);

        try {
            // 1. 권한 검증 (해당 상담방의 참여자인지)
            if (!chatService.isParticipant(requestIdx, senderCode)) {
                log.warn("권한 없음: requestIdx={}, userCode={}", requestIdx, senderCode);
                throw new SecurityException("해당 상담방에 접근 권한이 없습니다.");
            }

            ConsultationRequest consultation = consultationRequestRepository
                .findById(requestIdx)
                .orElseThrow(() -> new IllegalStateException("상담을 찾을 수 없습니다."));

            LocalDateTime now = LocalDateTime.now();
            validateChatAvailability(consultation, now);

            // 2. 메시지 정보 설정
            message.setSender(senderCode);
            message.setSenderName(getSenderName(senderCode));
            message.setSentAt(now);

            // 3. Redis에 저장 + TTL 36시간 연장
            chatService.saveMessage(message);

            // 4. DB 업데이트 (lastActivityAt)
            updateLastActivity(requestIdx);
            
            // 5. 첫 메시지인 경우 상담 시작 처리 (트리거 미발동 대비)
            markStartedIfNeeded(requestIdx);

            // 6. 상대방 찾기 (서버에서 결정 - 보안)
            String recipientCode = getPartnerUserCode(requestIdx, senderCode);

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

            if (chatNotificationService != null) {
                try {
                    chatNotificationService.notifyNewMessage(message, recipientCode);
                } catch (Exception notifyError) {
                    log.warn("채팅 알림 전송 실패: requestIdx={}, recipient={}, error={}",
                             message.getRequestIdx(), recipientCode, notifyError.getMessage(), notifyError);
                }
            }

        } catch (SecurityException e) {
            log.error("메시지 전송 실패 (권한): {}", e.getMessage());
            // 클라이언트에게 에러 메시지 전송 (선택 사항)
            sendErrorToUser(senderCode, "권한이 없습니다.");
        } catch (IllegalStateException e) {
            log.warn("메시지 전송 실패 (상태 위반): requestIdx={}, reason={}", requestIdx, e.getMessage());
            sendErrorToUser(senderCode, e.getMessage());
        } catch (Exception e) {
            log.error("메시지 전송 실패: requestIdx={}", message.getRequestIdx(), e);
            sendErrorToUser(senderCode, "메시지 전송에 실패했습니다.");
        }
    }

    /**
     * 채팅 읽음 처리
     *
     * @param payload   읽음 처리 대상 상담 정보
     * @param principal 인증된 사용자
     */
    @MessageMapping("/chat.read")
    public void acknowledgeRead(@Payload ChatReadReceiptDto payload, Principal principal) {
        if (payload == null || payload.getRequestIdx() == null) {
            log.warn("읽음 처리 실패 - 요청 ID 누락");
            return;
        }

        String readerIdentifier = principal.getName();
        String readerCode = convertToUserCode(readerIdentifier);

        if (readerCode == null) {
            log.warn("읽음 처리 실패 - 사용자 식별 불가: identifier={}", readerIdentifier);
            sendErrorToUser(readerIdentifier, "사용자 정보를 확인할 수 없습니다. 다시 로그인해주세요.");
            return;
        }

        Long requestIdx = payload.getRequestIdx();

        if (!chatService.isParticipant(requestIdx, readerCode)) {
            log.warn("읽음 처리 실패 - 권한 없음: requestIdx={}, reader={}", requestIdx, readerCode);
            sendErrorToUser(readerCode, "해당 상담방에 접근 권한이 없습니다.");
            return;
        }

        try {
            ConsultationReadReceiptDto receipt =
                consultationRequestService.updateReadTime(requestIdx, readerCode);

            ChatReadReceiptDto event = new ChatReadReceiptDto(
                receipt.getRequestIdx(),
                readerCode,
                getSenderName(readerCode),
                receipt.getReadAt(),
                receipt.getLastActivityAt(),
                receipt.isAllMessagesRead()
            );

            messagingTemplate.convertAndSendToUser(
                readerCode,
                "/queue/read-receipts",
                event
            );

            String partnerCode = receipt.getPartnerUserCode();
            if (partnerCode != null) {
                messagingTemplate.convertAndSendToUser(
                    partnerCode,
                    "/queue/read-receipts",
                    event
                );
            }

        } catch (Exception e) {
            log.error("읽음 처리 실패 (WebSocket): requestIdx={}, reader={}", requestIdx, readerCode, e);
            sendErrorToUser(readerCode, "읽음 처리에 실패했습니다.");
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

    private void validateChatAvailability(ConsultationRequest consultation, LocalDateTime now) {
        RequestStatus requestStatus = consultation.getRequestStatusEnum();
        if (!RequestStatus.APPROVED.equals(requestStatus)) {
            throw new IllegalStateException("교수 승인이 완료된 상담만 채팅을 사용할 수 있습니다.");
        }

        ConsultationStatus status = consultation.getConsultationStatusEnum();
        if (ConsultationStatus.CANCELLED.equals(status) || ConsultationStatus.COMPLETED.equals(status)) {
            throw new IllegalStateException("종료되었거나 취소된 상담입니다.");
        }
        if (status == null) {
            throw new IllegalStateException("상담이 아직 준비되지 않았습니다.");
        }

        LocalDateTime desired = consultation.getDesiredDate();
        if (desired != null && now.isBefore(desired)) {
            String formatted = desired.format(DATE_TIME_FORMATTER);
            throw new IllegalStateException("상담 예정 시간(" + formatted + ") 이전에는 채팅을 시작할 수 없습니다.");
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

            if (!RequestStatus.APPROVED.equals(consultation.getRequestStatusEnum())) {
                log.debug("자동 시작 건너뜀 - 승인되지 않은 상담: requestIdx={}", requestIdx);
                return;
            }

            if (ConsultationStatus.SCHEDULED.equals(consultation.getConsultationStatusEnum())
                && consultation.getStartedAt() == null) {

                LocalDateTime now = LocalDateTime.now();
                if (isBeforeScheduledStart(consultation, now)) {
                    log.info("자동 시작 대기 - 예정 시간 전: requestIdx={}", requestIdx);
                    return;
                }

                consultation.setConsultationStatusEnum(ConsultationStatus.IN_PROGRESS);
                consultation.setStartedAt(now);
                consultation.setLastActivityAt(now);
                ConsultationRequest saved = consultationRequestRepository.save(consultation);
                log.info("첫 메시지로 상담 자동 시작: requestIdx={}", saved.getRequestIdx());
            }
            
        } catch (Exception e) {
            log.error("상담 시작 처리 실패: requestIdx={}", requestIdx, e);
        }
    }

    private boolean isBeforeScheduledStart(ConsultationRequest consultation, LocalDateTime now) {
        LocalDateTime desired = consultation.getDesiredDate();
        return desired != null && now.isBefore(desired);
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
            .map(UserTbl::getUserCode)
            .orElseGet(() -> {
                log.warn("사용자 이메일로 userCode 찾을 수 없음: {}", emailOrUserCode);
                return null;
            });
    }
}
