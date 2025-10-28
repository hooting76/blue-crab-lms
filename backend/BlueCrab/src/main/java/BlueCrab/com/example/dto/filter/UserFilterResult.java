package BlueCrab.com.example.dto.filter;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.util.List;

/**
 * 사용자 필터링 결과 DTO
 *
 * 필터링된 사용자 목록과 관련 정보를 담는 DTO
 *
 * @author BlueCrab Development Team
 * @version 1.0.0
 * @since 2025-10-28
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserFilterResult {

    /**
     * 필터링된 userCode 리스트
     * 실제 FCM 발송 대상
     */
    private List<String> userCodes;

    /**
     * 총 대상 수
     */
    private Integer totalCount;

    /**
     * 사용된 필터 조건
     * 이력 저장 및 추적용
     */
    private UserFilterCriteria criteria;
}
