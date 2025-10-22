package BlueCrab.com.example.dto.Lecture;

import lombok.*;

import java.time.LocalDateTime;

/**
 * 안내문 저장 응답 DTO
 * 관리자/교수가 안내문을 저장한 후 받는 응답
 *
 * @version 1.0.0
 * @since 2025-10-22
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NoticeSaveResponse {
    
    /**
     * 응답 성공 여부
     */
    private Boolean success;
    
    /**
     * 응답 메시지
     */
    private String message;
    
    /**
     * 저장된 안내문 데이터
     */
    private NoticeData data;

    /**
     * 안내문 상세 데이터
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class NoticeData {
        
        /**
         * 안내문 ID
         */
        private Integer noticeIdx;
        
        /**
         * 안내 메시지 내용
         */
        private String message;
        
        /**
         * 수정 시각
         */
        private LocalDateTime updatedAt;
        
        /**
         * 수정자 (관리자/교수 username)
         */
        private String updatedBy;
    }
}
