package BlueCrab.com.example.dto.Consultation;

import BlueCrab.com.example.enums.ConsultationType;
import com.fasterxml.jackson.annotation.JsonFormat;

import javax.validation.constraints.Future;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.time.LocalDateTime;

/**
 * 상담 요청 생성을 위한 DTO
 */
public class ConsultationRequestCreateDto {

    private String requesterUserCode;

    @NotBlank(message = "수신자 교번은 필수 입력 항목입니다.")
    private String recipientUserCode;

    @NotNull(message = "상담 유형은 필수 입력 항목입니다.")
    private ConsultationType consultationType;

    @NotBlank(message = "상담 제목은 필수 입력 항목입니다.")
    @Size(max = 100, message = "제목은 100자 이하로 입력해주세요.")
    private String title;

    @Size(max = 1000, message = "내용은 1000자 이하로 입력해주세요.")
    private String content;

    @Future(message = "희망 날짜는 현재 시간 이후여야 합니다.")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime desiredDate;

    public ConsultationRequestCreateDto() {}

    public String getRequesterUserCode() {
        return requesterUserCode;
    }

    public void setRequesterUserCode(String requesterUserCode) {
        this.requesterUserCode = requesterUserCode;
    }

    public String getRecipientUserCode() {
        return recipientUserCode;
    }

    public void setRecipientUserCode(String recipientUserCode) {
        this.recipientUserCode = recipientUserCode;
    }

    public ConsultationType getConsultationType() {
        return consultationType;
    }

    public void setConsultationType(ConsultationType consultationType) {
        this.consultationType = consultationType;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public LocalDateTime getDesiredDate() {
        return desiredDate;
    }

    public void setDesiredDate(LocalDateTime desiredDate) {
        this.desiredDate = desiredDate;
    }
}
