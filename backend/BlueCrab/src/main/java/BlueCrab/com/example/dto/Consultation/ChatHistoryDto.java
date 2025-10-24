package BlueCrab.com.example.dto.Consultation;

import java.util.List;

/**
 * 채팅 이력 조회 응답을 위한 DTO
 */
public class ChatHistoryDto {

    private Long requestIdx;
    private List<ChatMessageDto> messages;
    private Integer totalCount;

    public ChatHistoryDto() {}

    public ChatHistoryDto(Long requestIdx, List<ChatMessageDto> messages, Integer totalCount) {
        this.requestIdx = requestIdx;
        this.messages = messages;
        this.totalCount = totalCount;
    }

    public Long getRequestIdx() {
        return requestIdx;
    }

    public void setRequestIdx(Long requestIdx) {
        this.requestIdx = requestIdx;
    }

    public List<ChatMessageDto> getMessages() {
        return messages;
    }

    public void setMessages(List<ChatMessageDto> messages) {
        this.messages = messages;
    }

    public Integer getTotalCount() {
        return totalCount;
    }

    public void setTotalCount(Integer totalCount) {
        this.totalCount = totalCount;
    }
}
