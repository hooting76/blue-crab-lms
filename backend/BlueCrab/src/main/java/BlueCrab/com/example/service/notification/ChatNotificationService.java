package BlueCrab.com.example.service.notification;

import BlueCrab.com.example.dto.Consultation.ChatMessageDto;
import BlueCrab.com.example.dto.Consultation.ConsultationReadReceiptDto;
import BlueCrab.com.example.dto.FcmDataOnlySendRequest;
import BlueCrab.com.example.dto.FcmDataOnlySendResponse;
import BlueCrab.com.example.dto.FcmDataOnlyUserResult;
import BlueCrab.com.example.entity.ConsultationRequest;
import BlueCrab.com.example.entity.UserTbl;
import BlueCrab.com.example.repository.ConsultationRequestRepository;
import BlueCrab.com.example.repository.UserTblRepository;
import BlueCrab.com.example.service.FcmTokenService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.PreDestroy;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

/**
 * 상담 채팅 관련 FCM 알림 및 배치 전송을 담당하는 서비스.
 */
@Slf4j
@Service
public class ChatNotificationService {

    private final FcmTokenService fcmTokenService;
    private final ConsultationRequestRepository consultationRequestRepository;
    private final UserTblRepository userTblRepository;

    private final boolean notificationsEnabled;
    private final boolean readReceiptPushEnabled;
    private final long batchWindowSeconds;
    private final int batchMaxMessages;

    private final DateTimeFormatter isoFormatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    private final Map<String, BatchState> pendingBatches = new ConcurrentHashMap<>();
    private final ScheduledExecutorService scheduler;

    public ChatNotificationService(FcmTokenService fcmTokenService,
                                   ConsultationRequestRepository consultationRequestRepository,
                                   UserTblRepository userTblRepository,
                                   @Value("${app.chat.notification.enabled:true}") boolean notificationsEnabled,
                                   @Value("${app.chat.notification.batch-window-seconds:6}") long batchWindowSeconds,
                                   @Value("${app.chat.notification.max-messages:5}") int batchMaxMessages,
                                   @Value("${app.chat.notification.push-read-receipts:false}") boolean readReceiptPushEnabled) {
        this.fcmTokenService = fcmTokenService;
        this.consultationRequestRepository = consultationRequestRepository;
        this.userTblRepository = userTblRepository;
        this.notificationsEnabled = notificationsEnabled;
        this.batchWindowSeconds = batchWindowSeconds;
        this.batchMaxMessages = Math.max(1, batchMaxMessages);
        this.readReceiptPushEnabled = readReceiptPushEnabled;
        this.scheduler = Executors.newSingleThreadScheduledExecutor(new ChatNotificationThreadFactory());
    }

    /**
     * 새 메시지 알림을 배치 큐에 적재한다.
     */
    public void notifyNewMessage(ChatMessageDto message, String recipientUserCode) {
        if (!notificationsEnabled || message == null || message.getRequestIdx() == null) {
            return;
        }

        if (recipientUserCode == null || recipientUserCode.isBlank()) {
            log.debug("채팅 알림 대상이 없어 전송을 건너뜁니다.");
            return;
        }

        String key = buildBatchKey(recipientUserCode, message.getRequestIdx());
        ChatMessageDto copy = cloneMessage(message);

        BatchState state = pendingBatches.compute(key, (k, current) -> {
            if (current == null) {
                current = new BatchState();
            }
            current.addMessage(copy);
            return current;
        });

        if (state == null) {
            return;
        }

        scheduleIfNeeded(key, state);

        if (state.size() >= batchMaxMessages) {
            flushBatch(key);
        }
    }

    /**
     * 상담 시작 알림 전송.
     */
    public void notifyConsultationStarted(ConsultationRequest consultation) {
        if (!notificationsEnabled || consultation == null || consultation.getRequestIdx() == null) {
            return;
        }

        sendConsultationEvent(
            consultation,
            consultation.getRequesterUserCode(),
            "CONSULTATION_STARTED",
            "상담이 시작되었습니다.",
            buildCounterpartMessage(consultation, consultation.getRequesterUserCode(), "상담이 시작되었습니다."),
            Collections.emptyMap()
        );

        sendConsultationEvent(
            consultation,
            consultation.getRecipientUserCode(),
            "CONSULTATION_STARTED",
            "상담이 시작되었습니다.",
            buildCounterpartMessage(consultation, consultation.getRecipientUserCode(), "상담이 시작되었습니다."),
            Collections.emptyMap()
        );
    }

    /**
     * 상담 종료 알림 전송.
     */
    public void notifyConsultationEnded(ConsultationRequest consultation) {
        if (!notificationsEnabled || consultation == null || consultation.getRequestIdx() == null) {
            return;
        }

        sendConsultationEvent(
            consultation,
            consultation.getRequesterUserCode(),
            "CONSULTATION_ENDED",
            "상담이 종료되었습니다.",
            buildCounterpartMessage(consultation, consultation.getRequesterUserCode(), "상담이 종료되었습니다."),
            Collections.emptyMap()
        );

        sendConsultationEvent(
            consultation,
            consultation.getRecipientUserCode(),
            "CONSULTATION_ENDED",
            "상담이 종료되었습니다.",
            buildCounterpartMessage(consultation, consultation.getRecipientUserCode(), "상담이 종료되었습니다."),
            Collections.emptyMap()
        );
    }

    /**
     * 상담 요청 생성 알림 전송.
     */
    public void notifyConsultationRequested(ConsultationRequest consultation) {
        if (!notificationsEnabled || consultation == null || consultation.getRequestIdx() == null) {
            return;
        }

        Map<String, String> extra = new HashMap<>();
        if (consultation.getDesiredDate() != null) {
            extra.put("desiredDate", consultation.getDesiredDate().format(isoFormatter));
        }

        sendConsultationEvent(
            consultation,
            consultation.getRecipientUserCode(),
            "CONSULTATION_REQUESTED",
            "새 상담 요청이 도착했습니다.",
            buildCounterpartMessage(consultation, consultation.getRecipientUserCode(), "상담 요청이 도착했습니다."),
            extra
        );
    }

    /**
     * 상담 요청 승인 알림 전송.
     */
    public void notifyConsultationApproved(ConsultationRequest consultation) {
        if (!notificationsEnabled || consultation == null || consultation.getRequestIdx() == null) {
            return;
        }

        Map<String, String> extra = new HashMap<>();
        if (consultation.getDesiredDate() != null) {
            extra.put("scheduledStartAt", consultation.getDesiredDate().format(isoFormatter));
        }
        String approveMessage = consultation.getStatusReason();
        if (approveMessage == null) {
            approveMessage = consultation.getAcceptMessage();
        }
        if (approveMessage != null) {
            extra.put("acceptMessage", approveMessage);
        }

        sendConsultationEvent(
            consultation,
            consultation.getRequesterUserCode(),
            "CONSULTATION_APPROVED",
            "상담 요청이 승인되었습니다.",
            buildCounterpartMessage(consultation, consultation.getRequesterUserCode(), "상담이 승인되었습니다."),
            extra
        );
    }

    /**
     * 상담 요청 반려 알림 전송.
     */
    public void notifyConsultationRejected(ConsultationRequest consultation) {
        if (!notificationsEnabled || consultation == null || consultation.getRequestIdx() == null) {
            return;
        }

        Map<String, String> extra = new HashMap<>();
        String rejectionReason = consultation.getStatusReason();
        if (rejectionReason == null) {
            rejectionReason = consultation.getRejectionReason();
        }
        if (rejectionReason != null) {
            extra.put("rejectionReason", rejectionReason);
        }

        sendConsultationEvent(
            consultation,
            consultation.getRequesterUserCode(),
            "CONSULTATION_REJECTED",
            "상담 요청이 반려되었습니다.",
            buildCounterpartMessage(consultation, consultation.getRequesterUserCode(), "상담 요청이 반려되었습니다."),
            extra
        );
    }

    /**
     * 상담 요청 취소 알림 전송.
     */
    public void notifyConsultationCancelled(ConsultationRequest consultation) {
        if (!notificationsEnabled || consultation == null || consultation.getRequestIdx() == null) {
            return;
        }

        Map<String, String> extra = new HashMap<>();
        String cancelReason = consultation.getStatusReason();
        if (cancelReason == null) {
            cancelReason = consultation.getCancelReason();
        }
        if (cancelReason != null) {
            extra.put("cancelReason", cancelReason);
        }

        sendConsultationEvent(
            consultation,
            consultation.getRecipientUserCode(),
            "CONSULTATION_CANCELLED",
            "상담 요청이 취소되었습니다.",
            buildCounterpartMessage(consultation, consultation.getRecipientUserCode(), "상담 요청이 취소되었습니다."),
            extra
        );
    }

    /**
     * 읽음 처리에 대한 데이터 전송 (기본 비활성).
     */
    public void notifyReadReceipt(ConsultationReadReceiptDto receipt) {
        if (!notificationsEnabled || !readReceiptPushEnabled || receipt == null) {
            return;
        }

        if (receipt.getPartnerUserCode() == null) {
            return;
        }

        Map<String, String> data = new HashMap<>();
        data.put("type", "CHAT");
        data.put("event", "READ_RECEIPT");
        data.put("requestIdx", String.valueOf(receipt.getRequestIdx()));
        data.put("reader", receipt.getReaderUserCode());
        if (receipt.getReadAt() != null) {
            data.put("readAt", receipt.getReadAt().format(isoFormatter));
        }
        data.put("allMessagesRead", String.valueOf(receipt.isAllMessagesRead()));

        sendDataOnlyNotification(receipt.getPartnerUserCode(),
            "채팅이 읽음 처리되었습니다.",
            "상대방이 채팅을 확인했습니다.",
            data);
    }

    @PreDestroy
    public void shutdownScheduler() {
        scheduler.shutdownNow();
    }

    private void scheduleIfNeeded(String key, BatchState state) {
        synchronized (state) {
            ScheduledFuture<?> future = state.getScheduledFuture();
            if (future == null || future.isDone()) {
                state.setScheduledFuture(
                    scheduler.schedule(() -> flushBatch(key), batchWindowSeconds, TimeUnit.SECONDS)
                );
            }
        }
    }

    private void flushBatch(String key) {
        BatchState state = pendingBatches.remove(key);
        if (state == null) {
            return;
        }

        List<ChatMessageDto> messages = Collections.emptyList();
        ScheduledFuture<?> futureToCancel = null;

        synchronized (state) {
            futureToCancel = state.getScheduledFuture();
            messages = state.drainMessages();
        }

        if (futureToCancel != null && !futureToCancel.isDone()) {
            futureToCancel.cancel(false);
        }

        if (messages.isEmpty()) {
            return;
        }

        try {
            sendBatchNotification(key, messages);
        } catch (Exception e) {
            log.warn("채팅 배치 알림 전송 실패: key={}, error={}", key, e.getMessage(), e);
        }
    }

    private void sendBatchNotification(String key, List<ChatMessageDto> messages) {
        String[] parts = key.split("\\|", 2);
        if (parts.length != 2) {
            log.warn("잘못된 배치 키 형식: {}", key);
            return;
        }

        String recipientUserCode = parts[0];
        Long requestIdx;
        try {
            requestIdx = Long.parseLong(parts[1]);
        } catch (NumberFormatException e) {
            log.warn("배치 키에서 requestIdx 파싱 실패: {}", key);
            return;
        }

        ChatMessageDto latest = messages.get(messages.size() - 1);
        ensureSenderName(latest);

        String consultationTitle = resolveConsultationTitle(requestIdx).orElse("상담");

        String title;
        if (messages.size() == 1) {
            title = String.format("[%s] 새 메시지", consultationTitle);
        } else {
            title = String.format("[%s] 새 메시지 %d건", consultationTitle, messages.size());
        }

        String body = formatBody(latest, messages.size());

        Map<String, String> data = new HashMap<>();
        data.put("type", "CHAT");
        data.put("event", "NEW_MESSAGES");
        data.put("requestIdx", String.valueOf(requestIdx));
        data.put("messageCount", String.valueOf(messages.size()));
        data.put("consultationTitle", consultationTitle);

        if (latest.getSender() != null) {
            data.put("latestSender", latest.getSender());
        }
        if (latest.getSenderName() != null) {
            data.put("latestSenderName", latest.getSenderName());
        }
        if (latest.getSentAt() != null) {
            data.put("latestSentAt", latest.getSentAt().format(isoFormatter));
        }
        if (latest.getContent() != null) {
            data.put("preview", truncate(latest.getContent()));
        }

        sendNotification(recipientUserCode, title, body, data);
    }

    private void sendConsultationEvent(ConsultationRequest consultation,
                                       String targetUserCode,
                                       String event,
                                       String title,
                                       String body,
                                       Map<String, String> extraData) {
        if (targetUserCode == null || targetUserCode.isBlank()) {
            return;
        }

        Map<String, String> data = new HashMap<>();
        data.put("type", "CHAT");
        data.put("event", event);
        data.put("requestIdx", String.valueOf(consultation.getRequestIdx()));
        if (consultation.getTitle() != null) {
            data.put("consultationTitle", consultation.getTitle());
        }

        String counterpartCode = targetUserCode.equals(consultation.getRequesterUserCode())
            ? consultation.getRecipientUserCode()
            : consultation.getRequesterUserCode();

        data.put("counterpart", counterpartCode);
        data.put("counterpartName", resolveUserName(counterpartCode).orElse("상대방"));
        if (extraData != null && !extraData.isEmpty()) {
            data.putAll(extraData);
        }

        sendNotification(targetUserCode, title, body, data);
    }

    private void sendNotification(String userCode, String title, String body, Map<String, String> data) {
        try {
            String safeTitle = title != null && !title.isEmpty() ? title : "알림";
            String safeBody = body != null && !body.isEmpty() ? body : "";

            FcmDataOnlySendRequest request = new FcmDataOnlySendRequest(
                Collections.singletonList(userCode),
                safeTitle,
                safeBody,
                data,
                null,
                null
            );

            FcmDataOnlySendResponse response = fcmTokenService.sendDataOnlyByUser(request);
            FcmDataOnlyUserResult result = response.getResults() != null && !response.getResults().isEmpty()
                ? response.getResults().get(0)
                : null;

            if (response.getTotalTokens() == 0 || result == null || result.getSuccessCount() == 0) {
                log.warn("FCM Data-only 전송 실패 또는 대상 토큰 없음 - userCode={}, missingPlatforms={}",
                        userCode, result != null ? result.getMissingPlatforms() : "unknown");
            } else {
                log.debug("FCM Data-only 전송 성공 - userCode={}, successTokens={}",
                        userCode, result.getSuccessCount());
            }
        } catch (Exception e) {
            log.warn("FCM 전송 실패 - userCode={}, title={}, error={}", userCode, title, e.getMessage());
        }
    }

    private void sendDataOnlyNotification(String userCode, String title, String body, Map<String, String> data) {
        sendNotification(userCode, title, body, data);
    }

    private void ensureSenderName(ChatMessageDto message) {
        if (message.getSenderName() != null || message.getSender() == null) {
            return;
        }

        resolveUserName(message.getSender()).ifPresent(message::setSenderName);
    }

    private Optional<String> resolveConsultationTitle(Long requestIdx) {
        if (requestIdx == null) {
            return Optional.empty();
        }

        try {
            return consultationRequestRepository.findById(requestIdx)
                .map(ConsultationRequest::getTitle);
        } catch (Exception e) {
            log.debug("상담 제목 조회 실패: requestIdx={}, error={}", requestIdx, e.getMessage());
            return Optional.empty();
        }
    }

    private Optional<String> resolveUserName(String userCode) {
        if (userCode == null) {
            return Optional.empty();
        }

        try {
            return userTblRepository.findByUserCode(userCode)
                .map(UserTbl::getUserName);
        } catch (Exception e) {
            log.debug("사용자 이름 조회 실패: userCode={}, error={}", userCode, e.getMessage());
            return Optional.empty();
        }
    }

    private String buildCounterpartMessage(ConsultationRequest consultation, String targetUserCode, String suffix) {
        String counterpartCode = targetUserCode.equals(consultation.getRequesterUserCode())
            ? consultation.getRecipientUserCode()
            : consultation.getRequesterUserCode();

        String counterpartName = resolveUserName(counterpartCode).orElse("상대방");
        return counterpartName + "님과의 상담이 " + suffix;
    }

    private ChatMessageDto cloneMessage(ChatMessageDto original) {
        ChatMessageDto copy = new ChatMessageDto();
        copy.setRequestIdx(original.getRequestIdx());
        copy.setSender(original.getSender());
        copy.setSenderName(original.getSenderName());
        copy.setContent(original.getContent());
        copy.setSentAt(original.getSentAt());
        return copy;
    }

    private String formatBody(ChatMessageDto latest, int totalCount) {
        String senderName = latest.getSenderName() != null
            ? latest.getSenderName()
            : resolveUserName(latest.getSender()).orElse("상대방");

        String preview = latest.getContent() != null ? truncate(latest.getContent()) : "새 메시지";

        if (totalCount <= 1) {
            return senderName + ": " + preview;
        }

        return senderName + ": " + preview + " 외 " + (totalCount - 1) + "건";
    }

    private String truncate(String content) {
        if (content == null) {
            return "";
        }
        if (content.length() <= 120) {
            return content;
        }
        return content.substring(0, 117) + "...";
    }

    private String buildBatchKey(String recipientUserCode, Long requestIdx) {
        return recipientUserCode + "|" + requestIdx;
    }

    private static final class BatchState {
        private final List<ChatMessageDto> messages = new ArrayList<>();
        private ScheduledFuture<?> scheduledFuture;

        synchronized void addMessage(ChatMessageDto message) {
            messages.add(message);
        }

        synchronized int size() {
            return messages.size();
        }

        synchronized List<ChatMessageDto> drainMessages() {
            List<ChatMessageDto> snapshot = new ArrayList<>(messages);
            messages.clear();
            return snapshot;
        }

        synchronized ScheduledFuture<?> getScheduledFuture() {
            return scheduledFuture;
        }

        synchronized void setScheduledFuture(ScheduledFuture<?> scheduledFuture) {
            this.scheduledFuture = scheduledFuture;
        }
    }

    private static final class ChatNotificationThreadFactory implements ThreadFactory {
        @Override
        public Thread newThread(Runnable r) {
            Thread thread = new Thread(r);
            thread.setDaemon(true);
            thread.setName("chat-notification-batcher");
            return thread;
        }
    }
}
