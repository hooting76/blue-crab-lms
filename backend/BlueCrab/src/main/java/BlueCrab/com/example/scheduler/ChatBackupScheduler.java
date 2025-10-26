package BlueCrab.com.example.scheduler;

import BlueCrab.com.example.dto.Consultation.ChatMessageDto;
import BlueCrab.com.example.entity.ConsultationRequest;
import BlueCrab.com.example.repository.ConsultationRequestRepository;
import BlueCrab.com.example.service.ChatService;
import BlueCrab.com.example.service.MinIOService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Chat Message Backup Scheduler
 * Backs up active consultation chat messages from Redis to MinIO temp directory every 5 minutes.
 *
 * Execution: Every 5 minutes
 * Purpose: Minimize data loss in case of Redis failure or server crash
 *
 * @author BlueCrab Development Team
 * @version 1.1.0
 * @since 2025-10-24
 */
@Slf4j
@Component
public class ChatBackupScheduler {

    private static final DateTimeFormatter SNAPSHOT_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    private final ChatService chatService;
    private final ConsultationRequestRepository consultationRequestRepository;
    private final MinIOService minIOService;

    private final boolean backupEnabled;
    private final String chatBucket;
    private final String tempPrefix;

    public ChatBackupScheduler(ChatService chatService,
                               ConsultationRequestRepository consultationRequestRepository,
                               MinIOService minIOService,
                               @Value("${app.chat.scheduler.backup-enabled:true}") boolean backupEnabled,
                               @Value("${app.chat.minio.bucket:consultation-chats}") String chatBucket,
                               @Value("${app.chat.minio.temp-prefix:temp}") String tempPrefix) {
        this.chatService = chatService;
        this.consultationRequestRepository = consultationRequestRepository;
        this.minIOService = minIOService;
        this.backupEnabled = backupEnabled;
        this.chatBucket = chatBucket;
        this.tempPrefix = normalizePrefix(tempPrefix);
    }

    /**
     * Backup active consultation chats to MinIO temp directory
     *
     * <pre>
     * Cron: "0 *&#47;5 * * * *" (Every 5 minutes)
     * File location: &#123;tempPrefix&#125;/chat_&#123;requestIdx&#125;_snapshot_&#123;timestamp&#125;.txt
     * </pre>
     */
    @Scheduled(cron = "0 */5 * * * *")
    public void backupActiveConsultationChats() {
        if (!backupEnabled) {
            return;
        }

        List<ConsultationRequest> inProgressConsultations =
            consultationRequestRepository.findByConsultationStatus("IN_PROGRESS");

        if (inProgressConsultations.isEmpty()) {
            log.debug("[Scheduler] 백업 대상 상담이 없습니다.");
            return;
        }

        log.info("[Scheduler] Chat backup started - targets={}", inProgressConsultations.size());

        for (ConsultationRequest consultation : inProgressConsultations) {
            try {
                backupConsultation(consultation);
            } catch (Exception e) {
                log.warn("[Scheduler] 상담 백업 실패 - requestIdx={}, error={}",
                        consultation.getRequestIdx(), e.getMessage(), e);
            }
        }
    }

    private void backupConsultation(ConsultationRequest consultation) throws Exception {
        Long requestIdx = consultation.getRequestIdx();

        List<ChatMessageDto> messages = chatService.getAllMessages(requestIdx);
        if (messages.isEmpty()) {
            log.debug("[Scheduler] 메시지 없음으로 백업 생략 - requestIdx={}", requestIdx);
            return;
        }

        String chatLog = chatService.formatChatLog(requestIdx);
        byte[] bytes = chatLog.getBytes(StandardCharsets.UTF_8);

        deleteExistingSnapshots(requestIdx);

        String timestamp = LocalDateTime.now().format(SNAPSHOT_FORMATTER);
        String objectName = buildSnapshotObjectName(requestIdx, timestamp);

        try (ByteArrayInputStream inputStream = new ByteArrayInputStream(bytes)) {
            minIOService.uploadChatLog(chatBucket, objectName, inputStream, bytes.length);
            log.info("[Scheduler] 상담 백업 완료 - requestIdx={}, object={}", requestIdx, objectName);
        }
    }

    private void deleteExistingSnapshots(Long requestIdx) {
        String prefix = buildSnapshotPrefix(requestIdx);
        try {
            List<String> existing = minIOService.listObjects(chatBucket, prefix);
            for (String objectName : existing) {
                minIOService.deleteObject(chatBucket, objectName);
                log.debug("[Scheduler] 이전 스냅샷 삭제 - {}", objectName);
            }
        } catch (Exception e) {
            log.warn("[Scheduler] 이전 스냅샷 삭제 실패 - requestIdx={}, error={}",
                    requestIdx, e.getMessage());
        }
    }

    private String buildSnapshotObjectName(Long requestIdx, String timestamp) {
        String prefix = tempPrefix.isEmpty() ? "" : tempPrefix + "/";
        return prefix + "chat_" + requestIdx + "_snapshot_" + timestamp + ".txt";
    }

    private String buildSnapshotPrefix(Long requestIdx) {
        String prefix = tempPrefix.isEmpty() ? "" : tempPrefix + "/";
        return prefix + "chat_" + requestIdx + "_snapshot_";
    }

    private String normalizePrefix(String prefix) {
        if (prefix == null || prefix.isBlank()) {
            return "";
        }
        String normalized = prefix;
        if (normalized.startsWith("/")) {
            normalized = normalized.substring(1);
        }
        if (normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        return normalized;
    }
}
