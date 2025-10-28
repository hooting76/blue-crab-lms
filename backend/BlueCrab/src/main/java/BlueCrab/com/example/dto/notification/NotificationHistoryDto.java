package BlueCrab.com.example.dto.notification;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.time.LocalDateTime;

/**
 * FCM 알림 발송 이력 DTO
 *
 * @author BlueCrab Development Team
 * @version 1.0.0
 * @since 2025-10-28
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationHistoryDto {

    /**
     * 알림 ID
     */
    private Long id;

    /**
     * 알림 제목
     */
    private String title;

    /**
     * 알림 내용
     */
    private String body;

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

    /**
     * 필터 타입 (ALL, ROLE, COURSE, CUSTOM)
     */
    private String filterType;

    /**
     * 발송 시간
     */
    private LocalDateTime sentAt;

    /**
     * 발송한 관리자 ID
     */
    private String createdBy;
}
