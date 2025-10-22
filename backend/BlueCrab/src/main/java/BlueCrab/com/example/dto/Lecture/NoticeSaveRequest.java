package BlueCrab.com.example.dto.Lecture;

import lombok.*;

/**
 * 안내문 저장 요청 DTO
 * 관리자/교수가 안내문을 작성하거나 수정할 때 사용
 *
 * @version 1.0.0
 * @since 2025-10-22
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class NoticeSaveRequest {
    
    /**
     * 저장할 안내 메시지 내용
     */
    private String message;
}
