package BlueCrab.com.example.dto.filter;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.util.List;

/**
 * 사용자 필터링 조건 DTO (MVP 버전)
 *
 * FCM 알림 발송 대상 선택을 위한 필터 조건
     * Phase 2까지 확장된 필터 타입 지원
     *
     * @author BlueCrab Development Team
     * @version 1.0.0
 * @since 2025-10-28
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserFilterCriteria {

    /**
     * 필터 타입
     * ALL: 전체 사용자
     * ROLE: 역할별 (학생/교수)
     * COURSE: 강좌별 (수강생)
     * CUSTOM: 직접 입력
     * FACULTY: 학부별 (학생)
     * DEPARTMENT: 학과별 (학생)
     * ADMISSION_YEAR: 입학년도별 (학생)
     * GRADE: 학년별 (학생)
     */
    @NotNull(message = "필터 타입은 필수입니다")
    private FilterType filterType;

    /**
     * 역할별 필터용
     * 0: 교수, 1: 학생
     * filterType=ROLE일 때 사용
     */
    private Integer userStudent;

    /**
     * 강좌별 필터용
     * 강의 IDX 리스트
     * filterType=COURSE일 때 사용
     */
    private List<Integer> lectureIds;

    /**
     * 직접 입력 필터용
     * userCode 리스트 (예: ["202500105847", "202411002321"])
     * filterType=CUSTOM일 때 사용
     */
    private List<String> userCodes;

    /**
     * 학부 코드 리스트 (예: ["01", "02"])
     * filterType=FACULTY일 때 사용
     */
    private List<String> facultyCodes;

    /**
     * 학과 조건 리스트
     * filterType=DEPARTMENT일 때 사용
     */
    @Valid
    private List<DepartmentCondition> departments;

    /**
     * 입학년도 리스트 (예: [2023, 2024])
     * filterType=ADMISSION_YEAR일 때 사용
     */
    private List<Integer> admissionYears;

    /**
     * 학년 리스트 (예: [1, 2, 3])
     * filterType=GRADE일 때 사용
     */
    private List<Integer> gradeYears;

    /**
     * 학과 필터 조건 표현 DTO
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class DepartmentCondition {

        /**
         * 학부 코드 (두 자리)
         */
        @NotBlank(message = "학부 코드는 필수입니다")
        private String facultyCode;

        /**
         * 학과 코드 (두 자리)
         */
        @NotBlank(message = "학과 코드는 필수입니다")
        private String deptCode;
    }
}
