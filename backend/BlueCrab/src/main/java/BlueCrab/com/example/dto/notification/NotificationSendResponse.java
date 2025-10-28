package BlueCrab.com.example.dto.notification;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

/**
 * FCM 알림 발송 응답 DTO
 *
 * @author BlueCrab Development Team
 * @version 1.0.0
 * @since 2025-10-28
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationSendResponse {

    /**
     * 생성된 알림 ID
     */
    private Long notificationId;

    /**
     * 대상 수
     */
    private Integer targetCount;

    /**
     * 성공 수
     */
    private Integer successCount;

    /**
     * 실패 수
     */
    private Integer failureCount;
}
