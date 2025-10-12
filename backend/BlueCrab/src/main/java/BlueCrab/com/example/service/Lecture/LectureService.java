// 작성자: 성태준
// 강의 관리 서비스

package BlueCrab.com.example.service.Lecture;

import BlueCrab.com.example.entity.Lecture.LecTbl;
import BlueCrab.com.example.repository.Lecture.LecTblRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Optional;

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
            lecture.setLecPoint(updatedLecture.getLecPoint());
        }
        if (updatedLecture.getLecTime() != null) {
            lecture.setLecTime(updatedLecture.getLecTime());
        }
        if (updatedLecture.getLecMany() != null) {
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
            lecture.setLecYear(updatedLecture.getLecYear());
        }
        if (updatedLecture.getLecSemester() != null) {
            lecture.setLecSemester(updatedLecture.getLecSemester());
        }
        if (updatedLecture.getLecSummary() != null) {
            lecture.setLecSummary(updatedLecture.getLecSummary());
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
}
