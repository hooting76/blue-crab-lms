package BlueCrab.com.example.dto.notification;

import BlueCrab.com.example.dto.filter.UserFilterCriteria;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

/**
 * 대량 이메일 발송 요청 DTO
 *
 * FCM 필터 로직을 재사용해 대상자를 선정하고,
 * 메일 제목/본문을 전달받아 일괄 발송한다.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmailNotificationRequest {

    /**
     * 이메일 제목
     */
    @NotBlank(message = "이메일 제목은 필수입니다")
    private String subject;

    /**
     * 이메일 본문
     * HTML 지원 여부는 sendAsHtml 플래그로 제어
     */
    @NotBlank(message = "이메일 본문은 필수입니다")
    private String body;

    /**
     * 본문을 HTML 형식으로 전송할지 여부 (기본 true)
     */
    private Boolean sendAsHtml;

    /**
     * 발송 대상 필터 조건
     */
    @NotNull(message = "필터 조건은 필수입니다")
    @Valid
    private UserFilterCriteria filterCriteria;
}
