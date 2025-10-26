package BlueCrab.com.example.dto.Consultation;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDateTime;

/**
 * WebSocket을 통한 채팅 읽음 확인 이벤트 DTO.
 */
public class ChatReadReceiptDto {

    private Long requestIdx;
    private String reader;
    private String readerName;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime readAt;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime lastActivityAt;

    private boolean allMessagesRead;

    public ChatReadReceiptDto() {
    }

    public ChatReadReceiptDto(Long requestIdx,
                              String reader,
                              String readerName,
                              LocalDateTime readAt,
                              LocalDateTime lastActivityAt,
                              boolean allMessagesRead) {
        this.requestIdx = requestIdx;
        this.reader = reader;
        this.readerName = readerName;
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

    public String getReader() {
        return reader;
    }

    public void setReader(String reader) {
        this.reader = reader;
    }

    public String getReaderName() {
        return readerName;
    }

    public void setReaderName(String readerName) {
        this.readerName = readerName;
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

