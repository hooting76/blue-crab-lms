package BlueCrab.com.example.dto.filter;

/**
 * 사용자 필터링 타입 (MVP 버전)
 *
 * FCM 알림 발송 대상 선택을 위한 필터 타입 정의
 *
 * @author BlueCrab Development Team
 * @version 1.0.0
 * @since 2025-10-28
 */
public enum FilterType {
    /**
     * 전체 사용자
     */
    ALL,

    /**
     * 역할별 (학생/교수)
     */
    ROLE,

    /**
     * 강좌별 (수강생)
     */
    COURSE,

    /**
     * 직접 입력 (userCode 리스트)
     */
    CUSTOM,

    /**
     * 학부별 (주전공 학부 코드)
     */
    FACULTY,

    /**
     * 학과별 (학부+학과 조합)
     */
    DEPARTMENT,

    /**
     * 입학년도별 (학생)
     */
    ADMISSION_YEAR,

    /**
     * 학년별 (현재 연도 기준 계산)
     */
    GRADE
}
