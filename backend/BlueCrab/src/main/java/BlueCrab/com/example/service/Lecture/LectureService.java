// 작성자: 성태준
// 강의 관리 서비스

package BlueCrab.com.example.service.Lecture;

import BlueCrab.com.example.entity.Lecture.Faculty;
import BlueCrab.com.example.entity.Lecture.LecTbl;
import BlueCrab.com.example.repository.Lecture.DepartmentRepository;
import BlueCrab.com.example.repository.Lecture.FacultyRepository;
import BlueCrab.com.example.repository.Lecture.LecTblRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;

/* 강의 관리 비즈니스 로직 서비스
 * 강의 CRUD, 수강 인원 관리, 강의 검색 등의 비즈니스 로직 처리
 *
 * 주요 기능:
 * - 강의 등록/수정/삭제 (CRUD)
 * - 강의 조회 및 검색
 * - 수강 인원 관리
 * - 강의 통계 조회
 *
 * 트랜잭션 관리:
 * - 조회 작업: @Transactional(readOnly = true)
 * - 변경 작업: @Transactional
 */
@Service
@Transactional(readOnly = true)
public class LectureService {

    @Autowired
    private LecTblRepository lecTblRepository;

    @Autowired
    private FacultyRepository facultyRepository;

    @Autowired
    private DepartmentRepository departmentRepository;

    // 강의 시간 형식 검증용 정규식 패턴
    // 형식: (요일명+교시) 반복 - 예: "월1월2수3수4", "화2목2"
    // - 요일명: 월/화/수/목/금 (평일만)
    // - 교시: 1~8
    private static final Pattern LECTURE_TIME_PATTERN = Pattern.compile("^([월화수목금][1-8])+$");

    // ========== 유효성 검증 메서드 ==========

    /**
     * 강의 시간 형식 검증
     * 
     * @param lecTime 검증할 강의 시간 문자열
     * @throws IllegalArgumentException 형식이 올바르지 않은 경우
     */
    private void validateLectureTime(String lecTime) {
        if (lecTime == null || lecTime.trim().isEmpty()) {
            throw new IllegalArgumentException("강의 시간은 필수 입력 항목입니다.");
        }
        
        String trimmed = lecTime.trim();
        
        if (!LECTURE_TIME_PATTERN.matcher(trimmed).matches()) {
            throw new IllegalArgumentException(
                "강의 시간 형식이 올바르지 않습니다. " +
                "올바른 형식: 월1월2수3수4 (요일+교시 반복), " +
                "요일: 월/화/수/목/금, 교시: 1~8, 공백/쉼표 사용 불가. " +
                "입력값: " + trimmed
            );
        }
    }

    /**
     * 학부/학과 코드 형식 및 존재 여부 검증
     * 
     * @param facultyCode 학부 코드
     * @param deptCode 학과 코드
     * @throws IllegalArgumentException 형식이 올바르지 않거나 존재하지 않는 경우
     */
    private void validateFacultyAndDepartment(String facultyCode, String deptCode) {
        // 1. 형식 검증 (두 자리 숫자)
        if (facultyCode == null || !facultyCode.matches("^\\d{2}$")) {
            throw new IllegalArgumentException(
                "학부 코드 형식이 올바르지 않습니다. " +
                "두 자리 숫자여야 합니다. (예: 01, 03) " +
                "입력값: " + facultyCode
            );
        }
        
        if (deptCode == null || !deptCode.matches("^\\d{2}$")) {
            throw new IllegalArgumentException(
                "학과 코드 형식이 올바르지 않습니다. " +
                "두 자리 숫자여야 합니다. (예: 01, 03) " +
                "입력값: " + deptCode
            );
        }
        
        // 2. 학부 존재 여부 검증
        Faculty faculty = facultyRepository.findByFacultyCode(facultyCode)
                .orElseThrow(() -> new IllegalArgumentException(
                    "존재하지 않는 학부 코드입니다. " +
                    "입력값: " + facultyCode + " " +
                    "(학부 마스터 테이블에 등록된 학부 코드를 사용하세요)"
                ));
        
        // 3. 학과 존재 여부 검증 (해당 학부 내에서)
        boolean deptExists = departmentRepository.existsByFacultyIdAndDeptCode(
            faculty.getFacultyId(), 
            deptCode
        );
        
        if (!deptExists) {
            throw new IllegalArgumentException(
                "존재하지 않는 학과 코드입니다. " +
                "학부: " + facultyCode + " (" + faculty.getFacultyName() + "), " +
                "학과: " + deptCode + " " +
                "(해당 학부에 속한 학과 코드를 사용하세요)"
            );
        }
    }

    /**
     * 강의 데이터 전체 유효성 검증
     * 
     * @param lecture 검증할 강의 객체
     * @throws IllegalArgumentException 유효성 검증 실패 시
     */
    private void validateLectureData(LecTbl lecture) {
        // 강의 시간 검증
        validateLectureTime(lecture.getLecTime());
        
        // 학부/학과 코드 검증 (형식 + 존재 여부)
        validateFacultyAndDepartment(lecture.getLecMcode(), lecture.getLecMcodeDep());
        
        // 학점 범위 검증 (0~10)
        if (lecture.getLecPoint() != null && (lecture.getLecPoint() < 0 || lecture.getLecPoint() > 10)) {
            throw new IllegalArgumentException("이수학점은 0~10 사이여야 합니다. 입력값: " + lecture.getLecPoint());
        }
        
        // 대상 학년 검증 (1~4)
        if (lecture.getLecYear() != null && (lecture.getLecYear() < 1 || lecture.getLecYear() > 4)) {
            throw new IllegalArgumentException("대상 학년은 1~4 사이여야 합니다. 입력값: " + lecture.getLecYear());
        }
        
        // 학기 검증 (1 or 2)
        if (lecture.getLecSemester() != null && lecture.getLecSemester() != 1 && lecture.getLecSemester() != 2) {
            throw new IllegalArgumentException("학기는 1(1학기) 또는 2(2학기)여야 합니다. 입력값: " + lecture.getLecSemester());
        }
        
        // 정원 검증 (0~500)
        if (lecture.getLecMany() != null && (lecture.getLecMany() < 0 || lecture.getLecMany() > 500)) {
            throw new IllegalArgumentException("수강 정원은 0~500 사이여야 합니다. 입력값: " + lecture.getLecMany());
        }
    }

    // ========== 강의 조회 메서드 ==========

    /* 강의 IDX로 단건 조회 */
    public Optional<LecTbl> getLectureById(Integer lecIdx) {
        return lecTblRepository.findById(lecIdx);
    }

    /* 강의 코드로 조회 */
    public Optional<LecTbl> getLectureBySerial(String lecSerial) {
        return lecTblRepository.findByLecSerial(lecSerial);
    }

    /* 교수명으로 강의 목록 조회 */
    public List<LecTbl> getLecturesByProfessor(String lecProf) {
        return lecTblRepository.findByLecProf(lecProf);
    }

    /* 강의명 검색 (부분 일치) */
    public List<LecTbl> searchLecturesByTitle(String lecTit) {
        return lecTblRepository.findByLecTitContaining(lecTit);
    }

    /* 학년별 강의 목록 조회 */
    public List<LecTbl> getLecturesByYear(Integer lecYear) {
        return lecTblRepository.findByLecYear(lecYear);
    }

    /* 학기별 강의 목록 조회 */
    public List<LecTbl> getLecturesBySemester(Integer lecSemester) {
        return lecTblRepository.findByLecSemester(lecSemester);
    }

    /* 학년 및 학기로 강의 목록 조회 (페이징) */
    public Page<LecTbl> getLecturesByYearAndSemester(Integer lecYear, Integer lecSemester, Pageable pageable) {
        return lecTblRepository.findByLecYearAndLecSemester(lecYear, lecSemester, pageable);
    }

    /* 전공/교양별 강의 목록 조회 (페이징) */
    public Page<LecTbl> getLecturesByMajor(Integer lecMajor, Pageable pageable) {
        return lecTblRepository.findByLecMajor(lecMajor, pageable);
    }

    /* 교수명 및 학기로 강의 목록 조회 */
    public List<LecTbl> getLecturesByProfessorAndSemester(String lecProf, Integer lecSemester) {
        return lecTblRepository.findByLecProfAndLecSemester(lecProf, lecSemester);
    }

    /* 복합 조건 검색 (학년, 학기, 전공/교양, 수강신청 열림/닫힘) */
    public Page<LecTbl> searchLectures(Integer lecYear, Integer lecSemester, 
                                       Integer lecMajor, Integer lecOpen, Pageable pageable) {
        return lecTblRepository.searchLectures(lecYear, lecSemester, lecMajor, lecOpen, pageable);
    }

    /* 전체 강의 목록 조회 (페이징) */
    public Page<LecTbl> getAllLectures(Pageable pageable) {
        return lecTblRepository.findAll(pageable);
    }

    // ========== 강의 등록/수정/삭제 메서드 ==========

    /* 강의 등록
     * 강의 코드 중복 확인 후 등록
     */
    @Transactional
    public LecTbl createLecture(LecTbl lecture) {
        // 유효성 검증
        validateLectureData(lecture);
        
        // 강의 코드 중복 확인
        if (lecTblRepository.existsByLecSerial(lecture.getLecSerial())) {
            throw new IllegalArgumentException("이미 존재하는 강의 코드입니다: " + lecture.getLecSerial());
        }
        
        // 초기값 설정
        if (lecture.getLecCurrent() == null) {
            lecture.setLecCurrent(0);  // 현재 수강 인원 0으로 초기화
        }
        
        return lecTblRepository.save(lecture);
    }

    /* 강의 수정 (오버로드 - LecTbl 객체로 수정) */
    @Transactional
    public LecTbl updateLecture(LecTbl updatedLecture) {
        if (updatedLecture.getLecIdx() == null) {
            throw new IllegalArgumentException("강의 IDX가 필요합니다.");
        }
        return updateLecture(updatedLecture.getLecIdx(), updatedLecture);
    }

    /* 강의 수정 */
    @Transactional
    public LecTbl updateLecture(Integer lecIdx, LecTbl updatedLecture) {
        LecTbl lecture = lecTblRepository.findById(lecIdx)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 강의입니다: " + lecIdx));
        
        // 수정 가능한 필드만 업데이트
        if (updatedLecture.getLecTit() != null) {
            lecture.setLecTit(updatedLecture.getLecTit());
        }
        if (updatedLecture.getLecProf() != null) {
            lecture.setLecProf(updatedLecture.getLecProf());
        }
        if (updatedLecture.getLecPoint() != null) {
            // 학점 범위 검증
            if (updatedLecture.getLecPoint() < 0 || updatedLecture.getLecPoint() > 10) {
                throw new IllegalArgumentException("이수학점은 0~10 사이여야 합니다. 입력값: " + updatedLecture.getLecPoint());
            }
            lecture.setLecPoint(updatedLecture.getLecPoint());
        }
        if (updatedLecture.getLecTime() != null) {
            // 강의 시간 형식 검증
            validateLectureTime(updatedLecture.getLecTime());
            lecture.setLecTime(updatedLecture.getLecTime());
        }
        if (updatedLecture.getLecMany() != null) {
            // 정원 범위 검증
            if (updatedLecture.getLecMany() < 0 || updatedLecture.getLecMany() > 500) {
                throw new IllegalArgumentException("수강 정원은 0~500 사이여야 합니다. 입력값: " + updatedLecture.getLecMany());
            }
            lecture.setLecMany(updatedLecture.getLecMany());
        }
        if (updatedLecture.getLecOpen() != null) {
            lecture.setLecOpen(updatedLecture.getLecOpen());
        }
        if (updatedLecture.getLecMajor() != null) {
            lecture.setLecMajor(updatedLecture.getLecMajor());
        }
        if (updatedLecture.getLecMust() != null) {
            lecture.setLecMust(updatedLecture.getLecMust());
        }
        if (updatedLecture.getLecYear() != null) {
            // 대상 학년 검증
            if (updatedLecture.getLecYear() < 1 || updatedLecture.getLecYear() > 4) {
                throw new IllegalArgumentException("대상 학년은 1~4 사이여야 합니다. 입력값: " + updatedLecture.getLecYear());
            }
            lecture.setLecYear(updatedLecture.getLecYear());
        }
        if (updatedLecture.getLecSemester() != null) {
            // 학기 검증
            if (updatedLecture.getLecSemester() != 1 && updatedLecture.getLecSemester() != 2) {
                throw new IllegalArgumentException("학기는 1(1학기) 또는 2(2학기)여야 합니다. 입력값: " + updatedLecture.getLecSemester());
            }
            lecture.setLecSemester(updatedLecture.getLecSemester());
        }
        if (updatedLecture.getLecSummary() != null) {
            lecture.setLecSummary(updatedLecture.getLecSummary());
        }
        if (updatedLecture.getLecMcode() != null && updatedLecture.getLecMcodeDep() != null) {
            // 학부/학과 코드 둘 다 제공된 경우: 존재 여부 검증
            validateFacultyAndDepartment(updatedLecture.getLecMcode(), updatedLecture.getLecMcodeDep());
            lecture.setLecMcode(updatedLecture.getLecMcode());
            lecture.setLecMcodeDep(updatedLecture.getLecMcodeDep());
        } else if (updatedLecture.getLecMcode() != null || updatedLecture.getLecMcodeDep() != null) {
            // 학부 또는 학과 코드 중 하나만 제공된 경우: 에러
            throw new IllegalArgumentException(
                "학부 코드와 학과 코드는 함께 수정해야 합니다. " +
                "둘 다 제공하거나 둘 다 생략하세요."
            );
        }
        
        return lecTblRepository.save(lecture);
    }

    /* 강의 삭제 */
    @Transactional
    public void deleteLecture(Integer lecIdx) {
        if (!lecTblRepository.existsById(lecIdx)) {
            throw new IllegalArgumentException("존재하지 않는 강의입니다: " + lecIdx);
        }
        lecTblRepository.deleteById(lecIdx);
    }

    // ========== 수강 인원 관리 메서드 ==========

    /* 수강 인원 증가
     * 수강신청 시 호출
     * 반환값: 업데이트 성공 여부 (false면 정원 초과 또는 강의 없음)
     */
    @Transactional
    public boolean incrementEnrollment(Integer lecIdx) {
        int updated = lecTblRepository.incrementLecCurrent(lecIdx);
        return updated > 0;
    }

    /* 수강 인원 감소
     * 수강 취소 시 호출
     * 반환값: 업데이트 성공 여부 (false면 이미 0명이거나 강의 없음)
     */
    @Transactional
    public boolean decrementEnrollment(Integer lecIdx) {
        int updated = lecTblRepository.decrementLecCurrent(lecIdx);
        return updated > 0;
    }

    /* 정원 여부 확인 */
    public boolean hasAvailableSeats(Integer lecIdx) {
        return lecTblRepository.hasAvailableSeats(lecIdx);
    }

    // ========== 통계 메서드 ==========

    /* 강의별 통계 조회 (수강생 수, 정원, 여석 등) */
    public Map<String, Object> getLectureStatistics(Integer lecIdx) {
        LecTbl lecture = lecTblRepository.findById(lecIdx)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 강의입니다: " + lecIdx));
        
        Map<String, Object> stats = new java.util.HashMap<>();
        stats.put("lecIdx", lecture.getLecIdx());
        stats.put("lecTit", lecture.getLecTit());
        stats.put("lecProf", lecture.getLecProf());
        stats.put("capacity", lecture.getLecMany());  // 정원
        stats.put("enrolled", lecture.getLecCurrent());  // 현재 수강생
        stats.put("available", lecture.getLecMany() - lecture.getLecCurrent());  // 여석
        stats.put("enrollmentRate", lecture.getLecMany() > 0 ? 
                (lecture.getLecCurrent() * 100.0 / lecture.getLecMany()) : 0);  // 수강률
        
        return stats;
    }

    /* 전체 강의 수 조회 */
    public long getTotalLectureCount() {
        return lecTblRepository.countAllLectures();
    }

    /* 교수별 강의 수 조회 */
    public long getLectureCountByProfessor(String lecProf) {
        return lecTblRepository.countByLecProf(lecProf);
    }

    /* 전공/교양별 강의 수 조회 */
    public long getLectureCountByMajor(Integer lecMajor) {
        return lecTblRepository.countByLecMajor(lecMajor);
    }

    // ========== 유틸리티 메서드 ==========

    /* 강의 존재 여부 확인 */
    public boolean existsLecture(Integer lecIdx) {
        return lecTblRepository.existsById(lecIdx);
    }

    /* 강의 코드 존재 여부 확인 */
    public boolean existsBySerial(String lecSerial) {
        return lecTblRepository.existsByLecSerial(lecSerial);
    }

    /* 수강 자격 검증을 위한 모든 강의 조회 */
    public List<LecTbl> getAllLecturesForEligibility() {
        return lecTblRepository.findAll();
    }
}
