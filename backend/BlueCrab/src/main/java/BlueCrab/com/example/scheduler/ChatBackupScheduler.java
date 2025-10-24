package BlueCrab.com.example.scheduler;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Chat Message Backup Scheduler
 * Backs up active consultation chat messages from Redis to MinIO temp directory every 5 minutes.
 * 
 * Execution: Every 5 minutes
 * Purpose: Minimize data loss in case of Redis failure or server crash
 * 
 * NOTE: Implementation will be added in Phase 5 (ChatService and MinIO integration)
 * 
 * @author BlueCrab Development Team
 * @version 1.0.0
 * @since 2025-10-24
 */
@Slf4j
@Component
public class ChatBackupScheduler {

    // TODO: Phase 5에서 의존성 추가
    // private final ChatService chatService;
    // private final ConsultationRequestRepository consultationRequestRepository;

    /**
     * Backup active consultation chats to MinIO temp directory
     * 
     * Cron: "0 *&#47;5 * * * *" (Every 5 minutes)
     * File location: consultations/temp/chat_{requestIdx}_snapshot_{timestamp}.txt
     * 
     * Backup process:
     * - Find IN_PROGRESS consultations from DB
     * - Fetch messages from Redis for each consultation
     * - Format messages as text file
     * - Upload to MinIO temp directory with timestamp
     * - Delete previous snapshots for the same requestIdx
     */
    @Scheduled(cron = "0 */5 * * * *")
    public void backupActiveConsultationChats() {
        log.debug("[Scheduler] Chat backup started (Not implemented yet)");
        
        // TODO: Phase 5에서 구현
        // Implementation will be added when ChatService is ready
    }
}
