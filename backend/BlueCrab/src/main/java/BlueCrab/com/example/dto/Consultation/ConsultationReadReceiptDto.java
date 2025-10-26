package BlueCrab.com.example.dto.Consultation;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDateTime;

/**
 * 상담 채팅 읽음 처리 결과 DTO.
 * 읽음 처리 후 갱신된 정보를 WebSocket 및 REST 응답에 재사용한다.
 */
public class ConsultationReadReceiptDto {

    private Long requestIdx;
    private String readerUserCode;
    private String partnerUserCode;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime readAt;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime lastActivityAt;

    private boolean allMessagesRead;

    public ConsultationReadReceiptDto() {
    }

    public ConsultationReadReceiptDto(Long requestIdx,
                                      String readerUserCode,
                                      String partnerUserCode,
                                      LocalDateTime readAt,
                                      LocalDateTime lastActivityAt,
                                      boolean allMessagesRead) {
        this.requestIdx = requestIdx;
        this.readerUserCode = readerUserCode;
        this.partnerUserCode = partnerUserCode;
        this.readAt = readAt;
        this.lastActivityAt = lastActivityAt;
        this.allMessagesRead = allMessagesRead;
    }

    public Long getRequestIdx() {
        return requestIdx;
    }

    public void setRequestIdx(Long requestIdx) {
        this.requestIdx = requestIdx;
    }

    public String getReaderUserCode() {
        return readerUserCode;
    }

    public void setReaderUserCode(String readerUserCode) {
        this.readerUserCode = readerUserCode;
    }

    public String getPartnerUserCode() {
        return partnerUserCode;
    }

    public void setPartnerUserCode(String partnerUserCode) {
        this.partnerUserCode = partnerUserCode;
    }

    public LocalDateTime getReadAt() {
        return readAt;
    }

    public void setReadAt(LocalDateTime readAt) {
        this.readAt = readAt;
    }

    public LocalDateTime getLastActivityAt() {
        return lastActivityAt;
    }

    public void setLastActivityAt(LocalDateTime lastActivityAt) {
        this.lastActivityAt = lastActivityAt;
    }

    public boolean isAllMessagesRead() {
        return allMessagesRead;
    }

    public void setAllMessagesRead(boolean allMessagesRead) {
        this.allMessagesRead = allMessagesRead;
    }
}

