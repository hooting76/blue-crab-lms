package BlueCrab.com.example.scheduler;

import BlueCrab.com.example.dto.Consultation.ConsultationIdDto;
import BlueCrab.com.example.entity.ConsultationRequest;
import BlueCrab.com.example.repository.ConsultationRequestRepository;
import BlueCrab.com.example.service.ConsultationRequestService;
import BlueCrab.com.example.service.MinIOService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Orphaned Room Cleanup Scheduler
 * Cleans up orphaned MinIO temp snapshots and forces completion of abandoned consultations.
 */
@Slf4j
@Component
public class OrphanedRoomCleanupScheduler {

    private static final Pattern SNAPSHOT_PATTERN =
        Pattern.compile("chat_(\\d+)_snapshot_(\\d{14})\\.txt");
    private static final DateTimeFormatter SNAPSHOT_FORMATTER =
        DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    private final MinIOService minioService;
    private final ConsultationRequestRepository consultationRequestRepository;
    private final ConsultationRequestService consultationRequestService;

    private final boolean cleanupEnabled;
    private final String chatBucket;
    private final String tempPrefix;
    private final long cleanupThresholdHours;

    public OrphanedRoomCleanupScheduler(MinIOService minioService,
                                        ConsultationRequestRepository consultationRequestRepository,
                                        ConsultationRequestService consultationRequestService,
                                        @Value("${app.chat.scheduler.cleanup-enabled:true}") boolean cleanupEnabled,
                                        @Value("${app.chat.minio.bucket:consultation-chats}") String chatBucket,
                                        @Value("${app.chat.minio.temp-prefix:temp}") String tempPrefix,
                                        @Value("${app.chat.cleanup.threshold-hours:60}") long cleanupThresholdHours) {
        this.minioService = minioService;
        this.consultationRequestRepository = consultationRequestRepository;
        this.consultationRequestService = consultationRequestService;
        this.cleanupEnabled = cleanupEnabled;
        this.chatBucket = chatBucket;
        this.tempPrefix = normalizePrefix(tempPrefix);
        this.cleanupThresholdHours = cleanupThresholdHours;
    }

    /**
     * Clean up orphaned MinIO temp snapshots.
     * Cron: {@code "0 0 3 * * *"} (Daily at 3:00 AM)
     */
    @Scheduled(cron = "0 0 3 * * *")
    public void cleanupOrphanedSnapshots() {
        if (!cleanupEnabled) {
            return;
        }

        log.info("[Scheduler] Orphaned room cleanup started");

        List<SnapshotInfo> snapshots = loadSnapshotInfos();
        if (snapshots.isEmpty()) {
            log.debug("[Scheduler] 정리할 스냅샷이 없습니다.");
            return;
        }

        LocalDateTime threshold = LocalDateTime.now().minusHours(cleanupThresholdHours);
        List<SnapshotInfo> activeSnapshots = new ArrayList<>();

        // 1. 오래된 스냅샷 제거
        for (SnapshotInfo snapshot : snapshots) {
            if (snapshot.getCreatedAt().isPresent() && snapshot.getCreatedAt().get().isBefore(threshold)) {
                deleteSnapshot(snapshot, "오래된 스냅샷 정리");
            } else {
                activeSnapshots.add(snapshot);
            }
        }

        if (activeSnapshots.isEmpty()) {
            log.debug("[Scheduler] 활성 스냅샷이 없습니다.");
            return;
        }

        Map<Long, List<SnapshotInfo>> snapshotsByRequest = groupByRequest(activeSnapshots);

        // 2. 상담 존재 여부 및 상태 확인
        for (Map.Entry<Long, List<SnapshotInfo>> entry : snapshotsByRequest.entrySet()) {
            Long requestIdx = entry.getKey();
            List<SnapshotInfo> requestSnapshots = entry.getValue();

            Optional<ConsultationRequest> consultationOpt = consultationRequestRepository.findById(requestIdx);

            if (!consultationOpt.isPresent()) {
                requestSnapshots.forEach(snapshot -> deleteSnapshot(snapshot, "상담 데이터 없음"));
                continue;
            }

            ConsultationRequest consultation = consultationOpt.get();

            if (!"IN_PROGRESS".equals(consultation.getStatus())) {
                requestSnapshots.forEach(snapshot -> deleteSnapshot(snapshot, "상담 종료 상태"));
                continue;
            }

            if (isAbandoned(consultation, threshold)) {
                try {
                    ConsultationIdDto dto = new ConsultationIdDto();
                    dto.setRequestIdx(requestIdx);
                    consultationRequestService.endConsultation(dto);
                    log.info("[Scheduler] 장기간 미사용 상담 자동 종료 - requestIdx={}", requestIdx);
                } catch (Exception e) {
                    log.warn("[Scheduler] 상담 자동 종료 실패 - requestIdx={}, error={}",
                            requestIdx, e.getMessage(), e);
                } finally {
                    requestSnapshots.forEach(snapshot -> deleteSnapshot(snapshot, "자동 종료 이후 정리"));
                }
            }
        }
    }

    private List<SnapshotInfo> loadSnapshotInfos() {
        List<SnapshotInfo> snapshots = new ArrayList<>();
        String listPrefix = tempPrefix.isEmpty() ? "" : tempPrefix;

        try {
            List<String> objects = minioService.listObjects(chatBucket, listPrefix);
            for (String objectName : objects) {
                parseSnapshot(objectName).ifPresent(snapshots::add);
            }
        } catch (Exception e) {
            log.warn("[Scheduler] 스냅샷 목록 조회 실패 - error={}", e.getMessage(), e);
        }

        return snapshots;
    }

    private Optional<SnapshotInfo> parseSnapshot(String objectName) {
        String fileName = objectName;
        int slashIndex = objectName.lastIndexOf('/');
        if (slashIndex >= 0) {
            fileName = objectName.substring(slashIndex + 1);
        }

        Matcher matcher = SNAPSHOT_PATTERN.matcher(fileName);
        if (!matcher.matches()) {
            deleteSnapshot(new SnapshotInfo(objectName, null, Optional.empty()), "잘못된 파일명");
            return Optional.empty();
        }

        Long requestIdx = Long.parseLong(matcher.group(1));
        String timestamp = matcher.group(2);

        try {
            LocalDateTime createdAt = LocalDateTime.parse(timestamp, SNAPSHOT_FORMATTER);
            return Optional.of(new SnapshotInfo(objectName, requestIdx, Optional.of(createdAt)));
        } catch (DateTimeParseException e) {
            deleteSnapshot(new SnapshotInfo(objectName, requestIdx, Optional.empty()), "타임스탬프 파싱 실패");
            return Optional.empty();
        }
    }

    private boolean isAbandoned(ConsultationRequest consultation, LocalDateTime threshold) {
        LocalDateTime lastActivity = consultation.getLastActivityAt();
        LocalDateTime startedAt = consultation.getStartedAt();

        if (lastActivity != null) {
            return lastActivity.isBefore(threshold);
        }

        if (startedAt != null) {
            return startedAt.isBefore(threshold);
        }

        LocalDateTime createdAt = consultation.getCreatedAt();
        return createdAt != null && createdAt.isBefore(threshold);
    }

    private Map<Long, List<SnapshotInfo>> groupByRequest(List<SnapshotInfo> snapshots) {
        Map<Long, List<SnapshotInfo>> grouped = new HashMap<>();
        for (SnapshotInfo snapshot : snapshots) {
            if (snapshot.getRequestIdx() == null) {
                deleteSnapshot(snapshot, "요청 ID 없음");
                continue;
            }
            grouped.computeIfAbsent(snapshot.getRequestIdx(), key -> new ArrayList<>())
                .add(snapshot);
        }
        return grouped;
    }

    private void deleteSnapshot(SnapshotInfo snapshot, String reason) {
        if (snapshot.getObjectName() == null) {
            return;
        }

        try {
            minioService.deleteObject(chatBucket, snapshot.getObjectName());
            log.debug("[Scheduler] 스냅샷 삭제 - object={}, reason={}", snapshot.getObjectName(), reason);
        } catch (Exception e) {
            log.warn("[Scheduler] 스냅샷 삭제 실패 - object={}, reason={}, error={}",
                    snapshot.getObjectName(), reason, e.getMessage());
        }
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

    private static final class SnapshotInfo {
        private final String objectName;
        private final Long requestIdx;
        private final Optional<LocalDateTime> createdAt;

        private SnapshotInfo(String objectName, Long requestIdx, Optional<LocalDateTime> createdAt) {
            this.objectName = objectName;
            this.requestIdx = requestIdx;
            this.createdAt = createdAt;
        }

        public String getObjectName() {
            return objectName;
        }

        public Long getRequestIdx() {
            return requestIdx;
        }

        public Optional<LocalDateTime> getCreatedAt() {
            return createdAt;
        }
    }
}
