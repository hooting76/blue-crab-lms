package BlueCrab.com.example.dto.Lecture;

import lombok.*;

import java.time.LocalDateTime;

/**
 * 안내문 조회 응답 DTO
 * 학생이나 비로그인 사용자가 안내문을 조회할 때 사용
 *
 * @version 1.0.0
 * @since 2025-10-22
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NoticeViewResponse {
    
    /**
     * 응답 성공 여부
     */
    private Boolean success;
    
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
