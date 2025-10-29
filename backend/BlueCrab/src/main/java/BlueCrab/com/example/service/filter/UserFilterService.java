package BlueCrab.com.example.service.filter;

import BlueCrab.com.example.dto.filter.UserFilterCriteria;
import BlueCrab.com.example.dto.filter.UserFilterCriteria.DepartmentCondition;
import BlueCrab.com.example.dto.filter.UserFilterResult;
import BlueCrab.com.example.entity.UserTbl;
import BlueCrab.com.example.repository.Lecture.EnrollmentExtendedTblRepository;
import BlueCrab.com.example.repository.UserTblRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 범용 사용자 필터링 서비스 (MVP 버전)
 *
 * FCM 알림 발송 대상 선택을 위한 필터링 로직
 * Phase 1에서는 4가지 필터 타입만 지원:
 * - ALL: 전체 사용자
 * - ROLE: 역할별 (학생/교수)
 * - COURSE: 강좌별 (수강생)
 * - CUSTOM: 직접 입력
 *
 * @author BlueCrab Development Team
 * @version 1.0.0
 * @since 2025-10-28
 */
@Service
public class UserFilterService {

    @Autowired
    private UserTblRepository userRepository;

    @Autowired
    private EnrollmentExtendedTblRepository enrollmentRepository;

    /**
     * 필터 조건으로 사용자 추출
     *
     * @param criteria 필터 조건
     * @return 필터링 결과 (userCode 리스트)
     */
    public UserFilterResult filterUsers(UserFilterCriteria criteria) {
        List<String> userCodes = resolveUserCodes(criteria);

        return UserFilterResult.builder()
            .userCodes(userCodes)
            .totalCount(userCodes.size())
            .criteria(criteria)
            .build();
    }

    /**
     * 필터 타입에 맞게 사용자 코드 목록을 계산
     */
    private List<String> resolveUserCodes(UserFilterCriteria criteria) {
        switch (criteria.getFilterType()) {
            case ALL:
                return getAllActiveUsers();
            case ROLE:
                return getUsersByRole(criteria.getUserStudent());
            case COURSE:
                return getUsersByCourses(criteria.getLectureIds());
            case CUSTOM:
                return getUsersByCustom(criteria.getUserCodes());
            case FACULTY:
                return getUsersByFaculty(criteria.getFacultyCodes());
            case DEPARTMENT:
                return getUsersByDepartment(criteria.getDepartments());
            case ADMISSION_YEAR:
                return getUsersByAdmissionYears(criteria.getAdmissionYears());
            case GRADE:
                return getUsersByGradeYears(criteria.getGradeYears());
            default:
                throw new IllegalArgumentException("지원하지 않는 필터 타입입니다: " + criteria.getFilterType());
        }
    }

    /**
     * 전체 사용자 조회
     *
     * @return userCode 리스트
     */
    private List<String> getAllActiveUsers() {
        return userRepository.findAll()
            .stream()
            .map(UserTbl::getUserCode)
            .filter(Objects::nonNull)
            .collect(Collectors.toList());
    }

    /**
     * 역할별 사용자 조회 (학생/교수)
     *
     * @param userStudent 0: 학생, 1: 교수
     * @return userCode 리스트
     */
    private List<String> getUsersByRole(Integer userStudent) {
        if (userStudent == null) {
            throw new IllegalArgumentException("역할별 필터 사용 시 userStudent 값이 필요합니다");
        }

        return userRepository.findAllByUserStudent(userStudent)
            .stream()
            .map(UserTbl::getUserCode)
            .filter(Objects::nonNull)
            .collect(Collectors.toList());
    }

    /**
     * 강좌별 사용자 조회 (수강생)
     *
     * @param lectureIds 강의 IDX 리스트
     * @return userCode 리스트
     */
    private List<String> getUsersByCourses(List<Integer> lectureIds) {
        if (lectureIds == null || lectureIds.isEmpty()) {
            throw new IllegalArgumentException("강좌별 필터 사용 시 lectureIds가 필요합니다");
        }

        List<String> userCodes = new ArrayList<>();

        for (Integer lecIdx : lectureIds) {
            List<String> courseUsers = enrollmentRepository.findByLecIdx(lecIdx)
                .stream()
                .map(enrollment -> {
                    Integer studentIdx = enrollment.getStudentIdx();
                    return userRepository.findById(studentIdx)
                        .map(UserTbl::getUserCode)
                        .orElse(null);
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
            userCodes.addAll(courseUsers);
        }

        // 중복 제거
        return userCodes.stream().distinct().collect(Collectors.toList());
    }

    /**
     * 직접 입력 사용자 조회 (CUSTOM 필터)
     *
     * @param userCodes 사용자 코드 리스트
     * @return userCode 리스트 (검증 및 정규화됨)
     */
    private List<String> getUsersByCustom(List<String> userCodes) {
        if (userCodes == null || userCodes.isEmpty()) {
            throw new IllegalArgumentException("직접 입력 필터 사용 시 userCodes가 필요합니다");
        }

        // null 및 빈 문자열 제거, 중복 제거
        List<String> cleanedCodes = userCodes.stream()
            .filter(Objects::nonNull)
            .map(String::trim)
            .filter(code -> !code.isEmpty())
            .distinct()
            .collect(Collectors.toList());

        if (cleanedCodes.isEmpty()) {
            throw new IllegalArgumentException("유효한 userCode가 없습니다");
        }

        return cleanedCodes;
    }

    /**
     * 학부별 사용자 조회
     */
    private List<String> getUsersByFaculty(List<String> facultyCodes) {
        if (facultyCodes == null || facultyCodes.isEmpty()) {
            throw new IllegalArgumentException("학부별 필터 사용 시 facultyCodes가 필요합니다");
        }

        List<String> normalized = facultyCodes.stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(code -> !code.isEmpty())
                .collect(Collectors.toList());

        if (normalized.isEmpty()) {
            throw new IllegalArgumentException("유효한 학부 코드가 없습니다");
        }

        Set<String> result = new LinkedHashSet<>();
        result.addAll(userRepository.findStudentCodesByFacultyCodes(normalized));
        return new ArrayList<>(result);
    }

    /**
     * 학과별 사용자 조회
     */
    private List<String> getUsersByDepartment(List<DepartmentCondition> departments) {
        if (departments == null || departments.isEmpty()) {
            throw new IllegalArgumentException("학과별 필터 사용 시 departments가 필요합니다");
        }

        Map<String, Set<String>> grouped = new LinkedHashMap<>();

        for (DepartmentCondition condition : departments) {
            if (condition == null) {
                continue;
            }

            String facultyCode = normalizeCode(condition.getFacultyCode());
            String deptCode = normalizeCode(condition.getDeptCode());

            if (facultyCode == null || deptCode == null) {
                continue;
            }

            grouped.computeIfAbsent(facultyCode, key -> new LinkedHashSet<>())
                    .add(deptCode);
        }

        if (grouped.isEmpty()) {
            throw new IllegalArgumentException("유효한 학부/학과 조합이 없습니다");
        }

        Set<String> result = new LinkedHashSet<>();

        grouped.forEach((facultyCode, deptCodes) -> {
            if (deptCodes.isEmpty()) {
                return;
            }
            List<String> codes = userRepository.findStudentCodesByFacultyAndDeptCodes(
                    facultyCode,
                    new ArrayList<>(deptCodes)
            );
            result.addAll(codes);
        });

        return new ArrayList<>(result);
    }

    /**
     * 입학년도별 사용자 조회
     */
    private List<String> getUsersByAdmissionYears(List<Integer> admissionYears) {
        if (admissionYears == null || admissionYears.isEmpty()) {
            throw new IllegalArgumentException("입학년도 필터 사용 시 admissionYears가 필요합니다");
        }

        List<String> years = admissionYears.stream()
                .filter(Objects::nonNull)
                .map(year -> String.format("%04d", year))
                .collect(Collectors.toList());

        if (years.isEmpty()) {
            throw new IllegalArgumentException("유효한 입학년도가 없습니다");
        }

        Set<String> result = new LinkedHashSet<>(userRepository.findStudentCodesByAdmissionYears(years));
        return new ArrayList<>(result);
    }

    /**
     * 학년별 사용자 조회
     */
    private List<String> getUsersByGradeYears(List<Integer> gradeYears) {
        if (gradeYears == null || gradeYears.isEmpty()) {
            throw new IllegalArgumentException("학년 필터 사용 시 gradeYears가 필요합니다");
        }

        int currentAcademicYear = LocalDate.now().getYear();

        Set<Integer> validGrades = gradeYears.stream()
                .filter(Objects::nonNull)
                .filter(grade -> grade >= 1)
                .collect(Collectors.toCollection(LinkedHashSet::new));

        if (validGrades.isEmpty()) {
            throw new IllegalArgumentException("유효한 학년이 없습니다");
        }

        Set<String> targetAdmissionYears = validGrades.stream()
                .map(grade -> currentAcademicYear - (grade - 1))
                .filter(year -> year > 0)
                .map(year -> String.format("%04d", year))
                .collect(Collectors.toCollection(LinkedHashSet::new));

        if (targetAdmissionYears.isEmpty()) {
            return new ArrayList<>();
        }

        return getUsersByAdmissionYears(
                targetAdmissionYears.stream()
                        .map(Integer::valueOf)
                        .collect(Collectors.toList())
        );
    }

    private String normalizeCode(String code) {
        if (code == null) {
            return null;
        }
        String trimmed = code.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    /**
     * 미리보기용 - 대상 수만 반환
     *
     * @param criteria 필터 조건
     * @return 대상 수
     */
    public Integer previewCount(UserFilterCriteria criteria) {
        UserFilterResult result = filterUsers(criteria);
        return result.getTotalCount();
    }
}
