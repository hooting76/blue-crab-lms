package BlueCrab.com.example.dto.notification;

import BlueCrab.com.example.dto.filter.UserFilterCriteria;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.util.Map;

/**
 * FCM 알림 발송 요청 DTO
 *
 * @author BlueCrab Development Team
 * @version 1.0.0
 * @since 2025-10-28
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationSendRequest {

    /**
     * 알림 제목
     */
    @NotBlank(message = "알림 제목은 필수입니다")
    private String title;

    /**
     * 알림 내용
     */
    @NotBlank(message = "알림 내용은 필수입니다")
    private String body;

    /**
     * 추가 데이터 (선택사항)
     * FCM data payload
     */
    private Map<String, String> data;

    /**
     * 필터 조건
     */
    @NotNull(message = "필터 조건은 필수입니다")
    @Valid
    private UserFilterCriteria filterCriteria;
}
