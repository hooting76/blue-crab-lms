package BlueCrab.com.example.service;

import BlueCrab.com.example.entity.ProfileView;
import BlueCrab.com.example.repository.ProfileViewRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * 사용자 프로필 정보 조회 서비스
 * PROFILE_VIEW 뷰테이블을 통해 종합적인 프로필 정보를 제공
 *
 * 주요 기능:
 * - JWT 토큰 기반 본인 프로필 정보 조회
 * - 프로필 정보 존재 여부 확인
 * - 사용자 유형 및 학적 상태 조회
 *
 * 보안 고려사항:
 * - 본인의 프로필 정보만 조회 가능
 * - JWT 토큰에서 추출한 이메일을 통해 권한 확인
 * - 민감한 정보는 로깅하지 않음
 */
@Service
public class ProfileService {

    private static final Logger logger = LoggerFactory.getLogger(ProfileService.class);

    @Autowired
    private ProfileViewRepository profileViewRepository;

    /**
     * 사용자 이메일을 통해 프로필 정보 조회
     * JWT 토큰에서 추출한 사용자 이메일로 해당 사용자의 전체 프로필 정보를 조회
     *
     * @param userEmail JWT 토큰에서 추출한 사용자 이메일
     * @return 사용자의 종합 프로필 정보
     * @throws RuntimeException 프로필 정보가 존재하지 않는 경우
     */
    public ProfileView getMyProfile(String userEmail) {
        logger.debug("프로필 조회 요청 - 사용자: {}", userEmail);

        if (userEmail == null || userEmail.trim().isEmpty()) {
            logger.warn("프로필 조회 실패 - 유효하지 않은 이메일: {}", userEmail);
            throw new RuntimeException("유효하지 않은 사용자 이메일입니다.");
        }

        Optional<ProfileView> profileOpt = profileViewRepository.findByUserEmail(userEmail);

        if (profileOpt.isEmpty()) {
            logger.warn("프로필 조회 실패 - 존재하지 않는 사용자: {}", userEmail);
            throw new RuntimeException("프로필 정보를 찾을 수 없습니다.");
        }

        ProfileView profile = profileOpt.get();
        logger.info("프로필 조회 성공 - 사용자: {}, 이름: {}", userEmail, profile.getUserName());

        return profile;
    }

    /**
     * 프로필 정보 존재 여부 확인
     * 사용자의 프로필 정보가 시스템에 등록되어 있는지 확인
     *
     * @param userEmail 확인할 사용자 이메일
     * @return 프로필 정보 존재 여부
     */
    public boolean hasProfile(String userEmail) {
        if (userEmail == null || userEmail.trim().isEmpty()) {
            return false;
        }

        boolean exists = profileViewRepository.existsByUserEmail(userEmail);
        logger.debug("프로필 존재 여부 확인 - 사용자: {}, 존재: {}", userEmail, exists);

        return exists;
    }

    /**
     * 사용자 유형 조회 (학생/교수 구분)
     * 권한 체크나 화면 분기 처리에 사용
     *
     * @param userEmail 사용자 이메일
     * @return 사용자 유형 (0: 학생, 1: 교수)
     */
    public Optional<Integer> getUserType(String userEmail) {
        if (userEmail == null || userEmail.trim().isEmpty()) {
            return Optional.empty();
        }

        Optional<Integer> userType = profileViewRepository.findUserTypeByUserEmail(userEmail);
        logger.debug("사용자 유형 조회 - 사용자: {}, 유형: {}", userEmail,
                    userType.map(type -> type == 1 ? "교수" : "학생").orElse("없음"));

        return userType;
    }

    /**
     * 학적 상태 조회
     * 사용자의 현재 학적 상태를 확인
     *
     * @param userEmail 사용자 이메일
     * @return 학적 상태
     */
    public Optional<String> getAcademicStatus(String userEmail) {
        if (userEmail == null || userEmail.trim().isEmpty()) {
            return Optional.empty();
        }

        Optional<String> academicStatus = profileViewRepository.findAcademicStatusByUserEmail(userEmail);
        logger.debug("학적 상태 조회 - 사용자: {}, 상태: {}", userEmail, academicStatus.orElse("없음"));

        return academicStatus;
    }

    /**
     * 프로필 정보 간단 검증
     * 프로필 정보가 완전한지 확인 (필수 정보 누락 체크)
     *
     * @param profile 검증할 프로필 정보
     * @return 프로필 완성도 정보
     */
    public ProfileCompleteness checkProfileCompleteness(ProfileView profile) {
        if (profile == null) {
            return new ProfileCompleteness(false, "프로필 정보가 없습니다.");
        }

        StringBuilder missingFields = new StringBuilder();
        boolean isComplete = true;

        // 필수 정보 체크
        if (profile.getUserName() == null || profile.getUserName().trim().isEmpty()) {
            missingFields.append("이름, ");
            isComplete = false;
        }
        if (profile.getUserPhone() == null || profile.getUserPhone().trim().isEmpty()) {
            missingFields.append("전화번호, ");
            isComplete = false;
        }
        if (profile.getBirthDate() == null || profile.getBirthDate().trim().isEmpty()) {
            missingFields.append("생년월일, ");
            isComplete = false;
        }

        String message;
        if (isComplete) {
            message = "프로필 정보가 완성되었습니다.";
        } else {
            String missing = missingFields.toString();
            if (missing.endsWith(", ")) {
                missing = missing.substring(0, missing.length() - 2);
            }
            message = "다음 정보가 누락되었습니다: " + missing;
        }

        logger.debug("프로필 완성도 체크 - 사용자: {}, 완성: {}, 메시지: {}",
                    profile.getUserEmail(), isComplete, message);

        return new ProfileCompleteness(isComplete, message);
    }

    /**
     * 프로필 완성도 정보를 담는 내부 클래스
     */
    public static class ProfileCompleteness {
        private final boolean complete;
        private final String message;

        public ProfileCompleteness(boolean complete, String message) {
            this.complete = complete;
            this.message = message;
        }

        public boolean isComplete() {
            return complete;
        }

        public String getMessage() {
            return message;
        }
    }
}