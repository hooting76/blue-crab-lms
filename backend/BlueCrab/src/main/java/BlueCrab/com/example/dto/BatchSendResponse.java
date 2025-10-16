package BlueCrab.com.example.dto;

import java.util.List;

/**
 * 배치 푸시 알림 전송 결과를 나타내는 DTO.
 */
public class BatchSendResponse {

    private int successCount;
    private int failureCount;
    private List<TokenSendResult> responses;

    public BatchSendResponse() {
    }

    public BatchSendResponse(int successCount, int failureCount, List<TokenSendResult> responses) {
        this.successCount = successCount;
        this.failureCount = failureCount;
        this.responses = responses;
    }

    public int getSuccessCount() {
        return successCount;
    }

    public void setSuccessCount(int successCount) {
        this.successCount = successCount;
    }

    public int getFailureCount() {
        return failureCount;
    }

    public void setFailureCount(int failureCount) {
        this.failureCount = failureCount;
    }

    public List<TokenSendResult> getResponses() {
        return responses;
    }

    public void setResponses(List<TokenSendResult> responses) {
        this.responses = responses;
    }

    public int getTotalCount() {
        return successCount + failureCount;
    }
}
