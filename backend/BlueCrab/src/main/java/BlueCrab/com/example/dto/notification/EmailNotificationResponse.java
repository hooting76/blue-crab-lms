package BlueCrab.com.example.dto.notification;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 대량 이메일 발송 응답 DTO
 *
 * 필터링된 대상 수와 실제 전송 결과 정보를 포함한다.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmailNotificationResponse {

    /**
     * 필터로 계산된 전체 대상 수
     */
    private Integer targetCount;

    /**
     * 이메일 주소가 확보된 대상 수
     */
    private Integer resolvedEmailCount;

    /**
     * 메일 발송 성공 수
     */
    private Integer successCount;

    /**
     * 메일 발송 실패 수
     */
    private Integer failureCount;

    /**
     * 이메일 주소가 없어 발송에서 제외된 userCode 수
     */
    private Integer skippedWithoutEmail;

    /**
     * 발송 실패한 이메일 목록 (간단한 추적용)
     */
    private List<String> failedRecipients;
}
