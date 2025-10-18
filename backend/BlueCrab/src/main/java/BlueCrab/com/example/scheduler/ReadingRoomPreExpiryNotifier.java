package BlueCrab.com.example.scheduler;

import BlueCrab.com.example.dto.BatchSendResponse;
import BlueCrab.com.example.dto.FcmTokenCollectionResult;
import BlueCrab.com.example.entity.ReadingSeat;
import BlueCrab.com.example.entity.ReadingUsageLog;
import BlueCrab.com.example.repository.ReadingSeatRepository;
import BlueCrab.com.example.repository.ReadingUsageLogRepository;
import BlueCrab.com.example.service.FirebasePushService;
import BlueCrab.com.example.service.FcmTokenService;
import BlueCrab.com.example.service.ReadingRoomNotificationFactory;
import BlueCrab.com.example.service.ReadingRoomNotificationFactory.NotificationTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

/**
 * Periodically sends 15-minute pre-expiry notifications for occupied reading room seats.
 */
@Component
public class ReadingRoomPreExpiryNotifier {

    private static final Logger log = LoggerFactory.getLogger(ReadingRoomPreExpiryNotifier.class);

    private final ReadingSeatRepository readingSeatRepository;
    private final ReadingUsageLogRepository readingUsageLogRepository;
    private final FcmTokenService fcmTokenService;
    private final FirebasePushService firebasePushService;
    private final ReadingRoomNotificationFactory notificationFactory;

    public ReadingRoomPreExpiryNotifier(ReadingSeatRepository readingSeatRepository,
                                        ReadingUsageLogRepository readingUsageLogRepository,
                                        FcmTokenService fcmTokenService,
                                        FirebasePushService firebasePushService,
                                        ReadingRoomNotificationFactory notificationFactory) {
        this.readingSeatRepository = readingSeatRepository;
        this.readingUsageLogRepository = readingUsageLogRepository;
        this.fcmTokenService = fcmTokenService;
        this.firebasePushService = firebasePushService;
        this.notificationFactory = notificationFactory;
    }

    @Scheduled(cron = "0 * * * * *")
    @Transactional
    public void sendPreExpiryNotifications() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime windowStart = now.plusMinutes(14);
        LocalDateTime windowEnd = now.plusMinutes(16);

        List<ReadingSeat> candidates = readingSeatRepository.findActiveSeatsEndingBetween(windowStart, windowEnd);
        if (candidates.isEmpty()) {
            return;
        }

        for (ReadingSeat seat : candidates) {
            processSeat(now, seat);
        }
    }

    private void processSeat(LocalDateTime now, ReadingSeat seat) {
        try {
            ReadingUsageLog usageLog = readingUsageLogRepository.findBySeatNumberAndEndTimeIsNull(seat.getSeatNumber())
                    .orElse(null);
            if (usageLog == null) {
                return;
            }

            if (usageLog.getPreNoticeSentAt() != null) {
                return;
            }

            String userCode = usageLog.getUserCode();
            if (userCode == null || userCode.trim().isEmpty()) {
                return;
            }

            FcmTokenCollectionResult tokenResult = fcmTokenService.collectTokensByUserCodes(
                    Collections.singletonList(userCode),
                    null,
                    false);

            List<String> tokens = tokenResult.getTokensByUser().get(userCode);
            if (tokens == null || tokens.isEmpty()) {
                int updated = readingUsageLogRepository.markPreNoticeSent(usageLog.getLogId(), now, 0);
                if (updated > 0) {
                    log.warn("Skipped pre-expiry alert: no tokens for user {} seat {}", userCode, seat.getSeatNumber());
                }
                return;
            }

            NotificationTemplate template = notificationFactory.createPreExpiryTemplate(seat, usageLog, now);
        BatchSendResponse response = firebasePushService.sendDataOnlyNotificationBatch(
            tokens,
            template.getTitle(),
            template.getBody(),
            template.getData());

        if (response.getSuccessCount() == 0) {
        log.warn("Pre-expiry alert failed (no successes) for seat {} user {}", seat.getSeatNumber(), userCode);
        return;
        }

        readingUsageLogRepository.markPreNoticeSent(usageLog.getLogId(), now, tokens.size());
        log.info("Pre-expiry alert sent for seat {} user {} tokens {} (success {} failure {})",
            seat.getSeatNumber(),
            userCode,
            tokens.size(),
            response.getSuccessCount(),
            response.getFailureCount());
        } catch (Exception ex) {
            log.error("Failed to send pre-expiry alert for seat {}", seat.getSeatNumber(), ex);
        }
    }
}
