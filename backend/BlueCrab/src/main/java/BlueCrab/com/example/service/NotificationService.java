package BlueCrab.com.example.service;

import BlueCrab.com.example.dto.FcmDataOnlySendRequest;
import BlueCrab.com.example.dto.FcmDataOnlySendResponse;
import BlueCrab.com.example.dto.filter.UserFilterCriteria;
import BlueCrab.com.example.dto.filter.UserFilterResult;
import BlueCrab.com.example.dto.notification.NotificationHistoryDto;
import BlueCrab.com.example.dto.notification.NotificationSendRequest;
import BlueCrab.com.example.dto.notification.NotificationSendResponse;
import BlueCrab.com.example.entity.NotificationEntity;
import BlueCrab.com.example.repository.NotificationRepository;
import BlueCrab.com.example.service.filter.UserFilterService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * FCM 알림 발송 서비스
 *
 * 관리자가 요청한 FCM 알림을 필터 조건에 따라 발송하고 이력을 관리
 *
 * @author BlueCrab Development Team
 * @version 1.0.0
 * @since 2025-10-28
 */
@Service
public class NotificationService {

    @Autowired
    private UserFilterService userFilterService;

    @Autowired
    private FcmTokenService fcmTokenService;

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private ObjectMapper objectMapper;

    /**
     * 알림 즉시 발송
     *
     * @param request 알림 발송 요청
     * @param adminId 발송한 관리자 ID
     * @return 발송 결과
     */
    @Transactional
    public NotificationSendResponse sendNotification(
            NotificationSendRequest request,
            String adminId) {

        try {
            // 1. 필터로 대상 추출
            UserFilterResult filterResult = userFilterService.filterUsers(
                request.getFilterCriteria()
            );

            // 2. Notification 엔티티 생성
            NotificationEntity notification = new NotificationEntity();
            notification.setTitle(request.getTitle());
            notification.setBody(request.getBody());
            notification.setFilterCriteriaJson(
                objectMapper.writeValueAsString(request.getFilterCriteria())
            );
            notification.setTargetCount(filterResult.getTotalCount());
            notification.setCreatedBy(adminId);

            notificationRepository.save(notification);

            // 3. 대상자 0명 처리 (감사 로그로 기록)
            if (filterResult.getTotalCount() == 0 || filterResult.getUserCodes().isEmpty()) {
                notification.setSuccessCount(0);
                notification.setFailureCount(0);
                notification.setSentAt(LocalDateTime.now());
                notificationRepository.save(notification);

                return NotificationSendResponse.builder()
                    .notificationId(notification.getId())
                    .targetCount(0)
                    .successCount(0)
                    .failureCount(0)
                    .build();
            }

            // 4. FCM 발송 요청 생성
            Map<String, String> dataPayload = new HashMap<>();
            if (request.getData() != null) {
                dataPayload.putAll(request.getData());
            }

            FcmDataOnlySendRequest fcmRequest = new FcmDataOnlySendRequest(
                filterResult.getUserCodes(),
                request.getTitle(),
                request.getBody(),
                dataPayload,
                null,  // platforms: null이면 모든 플랫폼
                true   // includeTemporary: 임시 토큰 포함
            );

            FcmDataOnlySendResponse fcmResponse = fcmTokenService.sendDataOnlyByUser(fcmRequest);

            // 5. 결과 업데이트
            notification.setSuccessCount(fcmResponse.getSuccessCount());
            notification.setFailureCount(fcmResponse.getFailureCount());
            notification.setSentAt(LocalDateTime.now());
            notificationRepository.save(notification);

            return NotificationSendResponse.builder()
                .notificationId(notification.getId())
                .targetCount(filterResult.getTotalCount())
                .successCount(fcmResponse.getSuccessCount())
                .failureCount(fcmResponse.getFailureCount())
                .build();

        } catch (Exception e) {
            throw new RuntimeException("알림 발송 중 오류가 발생했습니다: " + e.getMessage(), e);
        }
    }

    /**
     * 발송 이력 조회
     *
     * @param page 페이지 번호 (0부터 시작)
     * @param size 페이지 크기
     * @return 알림 이력 페이지
     */
    public Page<NotificationHistoryDto> getHistory(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<NotificationEntity> entities = notificationRepository
            .findAllByOrderBySentAtDesc(pageable);

        return entities.map(entity -> NotificationHistoryDto.builder()
            .id(entity.getId())
            .title(entity.getTitle())
            .body(entity.getBody())
            .targetCount(entity.getTargetCount())
            .successCount(entity.getSuccessCount())
            .failureCount(entity.getFailureCount())
            .filterType(extractFilterType(entity.getFilterCriteriaJson()))
            .sentAt(entity.getSentAt())
            .createdBy(entity.getCreatedBy())
            .build());
    }

    /**
     * JSON에서 FilterType 추출
     *
     * @param json FilterCriteria JSON
     * @return FilterType 문자열
     */
    private String extractFilterType(String json) {
        try {
            UserFilterCriteria criteria = objectMapper.readValue(json, UserFilterCriteria.class);
            return criteria.getFilterType().toString();
        } catch (Exception e) {
            return "UNKNOWN";
        }
    }
}
