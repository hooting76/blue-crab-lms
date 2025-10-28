package BlueCrab.com.example.controller;

import BlueCrab.com.example.dto.filter.UserFilterCriteria;
import BlueCrab.com.example.dto.notification.NotificationHistoryDto;
import BlueCrab.com.example.dto.notification.NotificationSendRequest;
import BlueCrab.com.example.dto.notification.NotificationSendResponse;
import BlueCrab.com.example.service.NotificationService;
import BlueCrab.com.example.service.filter.UserFilterService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.HashMap;
import java.util.Map;

/**
 * 관리자용 알림 발송 컨트롤러
 *
 * 기능:
 * 1. 필터 조건 미리보기 (대상자 수 확인)
 * 2. 알림 발송
 * 3. 발송 이력 조회
 */
@RestController
@RequestMapping("/api/admin/notifications")
@PreAuthorize("hasRole('ADMIN')")
@Validated
public class AdminNotificationController {

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private UserFilterService userFilterService;

    /**
     * 필터 조건 미리보기 - 대상자 수 확인
     *
     * POST /api/admin/notifications/preview
     *
     * Request Body:
     * {
     *   "filterType": "ROLE",
     *   "userStudent": 1,
     *   "lectureIds": null,
     *   "userCodes": null
     * }
     *
     * Response:
     * {
     *   "targetCount": 150
     * }
     */
    @PostMapping("/preview")
    public ResponseEntity<Map<String, Integer>> previewNotification(
            @Valid @RequestBody UserFilterCriteria criteria) {

        Integer count = userFilterService.previewCount(criteria);

        Map<String, Integer> response = new HashMap<>();
        response.put("targetCount", count);

        return ResponseEntity.ok(response);
    }

    /**
     * 알림 발송
     *
     * POST /api/admin/notifications/send
     *
     * Request Body:
     * {
     *   "title": "공지사항",
     *   "body": "오늘 17시에 전체 회의가 있습니다.",
     *   "data": {
     *     "type": "announcement",
     *     "link": "/announcements/123"
     *   },
     *   "filterCriteria": {
     *     "filterType": "ALL"
     *   }
     * }
     *
     * Response:
     * {
     *   "notificationId": 42,
     *   "targetCount": 150,
     *   "successCount": 148,
     *   "failureCount": 2
     * }
     */
    @PostMapping("/send")
    public ResponseEntity<NotificationSendResponse> sendNotification(
            @Valid @RequestBody NotificationSendRequest request,
            Authentication authentication) {

        // 현재 로그인한 관리자 ID 추출
        String adminId = authentication.getName();

        NotificationSendResponse response = notificationService.sendNotification(
            request,
            adminId
        );

        return ResponseEntity.ok(response);
    }

    /**
     * 알림 발송 이력 조회 (페이지네이션)
     *
     * POST /api/admin/notifications/history
     *
     * Request Body:
     * {
     *   "page": 0,
     *   "size": 20
     * }
     *
     * Response:
     * {
     *   "content": [
     *     {
     *       "id": 42,
     *       "title": "공지사항",
     *       "body": "오늘 17시에...",
     *       "targetCount": 150,
     *       "successCount": 148,
     *       "failureCount": 2,
     *       "filterType": "ALL",
     *       "sentAt": "2025-10-28T14:30:00",
     *       "createdBy": "admin@example.com"
     *     }
     *   ],
     *   "totalElements": 100,
     *   "totalPages": 5,
     *   "number": 0,
     *   "size": 20
     * }
     */
    @PostMapping("/history")
    public ResponseEntity<Page<NotificationHistoryDto>> getHistory(
            @RequestBody(required = false) Map<String, Integer> request) {

        int page = request != null && request.containsKey("page") ? request.get("page") : 0;
        int size = request != null && request.containsKey("size") ? request.get("size") : 20;

        Page<NotificationHistoryDto> history = notificationService.getHistory(page, size);

        return ResponseEntity.ok(history);
    }
}
