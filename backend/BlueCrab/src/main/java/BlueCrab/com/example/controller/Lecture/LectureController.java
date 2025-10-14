// 작성자: 성태준
// 강의 관리 컨트롤러

package BlueCrab.com.example.controller.Lecture;

import BlueCrab.com.example.dto.Lecture.LectureDto;
import BlueCrab.com.example.entity.Lecture.LecTbl;
import BlueCrab.com.example.repository.UserTblRepository;
import BlueCrab.com.example.service.Lecture.LectureService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/* 강의 관리 REST API 컨트롤러 (통합 버전)
 * 
 * 주요 엔드포인트:
 * - GET /api/lectures - 강의 조회 (쿼리 파라미터로 검색/필터링 통합)
 * - GET /api/lectures/{id} - 강의 상세 조회
 * - GET /api/lectures/{id}/stats - 강의별 통계
 * - POST /api/lectures - 강의 등록
 * - PUT /api/lectures/{id} - 강의 수정
 * - DELETE /api/lectures/{id} - 강의 삭제
 */
@RestController
@RequestMapping("/api/lectures")
public class LectureController {

    private static final Logger logger = LoggerFactory.getLogger(LectureController.class);

    @Autowired
    private LectureService lectureService;

    @Autowired
    private UserTblRepository userTblRepository;

    /* 강의 목록 조회 및 검색 (통합 엔드포인트)
     * 
     * 쿼리 파라미터:
     * - serial: 강의코드로 단일 조회
     * - professor: 교수명 필터
     * - year: 학년 필터
     * - semester: 학기 필터
     * - title: 강의명 검색
     * - major: 전공 코드 필터
     * - open: 개설 여부 (1/0)
     * - page, size: 페이징
     */
    @GetMapping
    public ResponseEntity<?> getLectures(
            @RequestParam(required = false) String serial,
            @RequestParam(required = false) String professor,
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer semester,
            @RequestParam(required = false) String title,
            @RequestParam(required = false) Integer major,
            @RequestParam(required = false) Integer open,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        try {
            // 1. 강의코드로 단일 조회
            if (serial != null) {
                return lectureService.getLectureBySerial(serial)
                        .map(this::convertToDto)
                        .map(ResponseEntity::ok)
                        .orElse(ResponseEntity.notFound().build());
            }
            
            // 2. 교수별 조회 (단일 조건)
            if (professor != null && year == null && title == null) {
                List<LecTbl> lectures = lectureService.getLecturesByProfessor(professor);
                List<LectureDto> dtoList = convertToDtoList(lectures);
                return ResponseEntity.ok(dtoList);
            }
            
            // 3. 강의명 검색 (단일 조건)
            if (title != null && professor == null && year == null) {
                List<LecTbl> lectures = lectureService.searchLecturesByTitle(title);
                List<LectureDto> dtoList = convertToDtoList(lectures);
                return ResponseEntity.ok(dtoList);
            }
            
            // 4. 복합 검색 또는 전체 목록
            Pageable pageable = PageRequest.of(page, size);
            if (year != null || semester != null || major != null || open != null) {
                Page<LecTbl> lectures = lectureService.searchLectures(year, semester, major, open, pageable);
                Page<LectureDto> dtoPage = convertToDtoPage(lectures);
                return ResponseEntity.ok(dtoPage);
            }
            
            // 5. 전체 목록 (필터 없음)
            Page<LecTbl> lectures = lectureService.getAllLectures(pageable);
            Page<LectureDto> dtoPage = convertToDtoPage(lectures);
            return ResponseEntity.ok(dtoPage);
            
        } catch (Exception e) {
            logger.error("강의 조회 실패", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("강의 조회 중 오류가 발생했습니다."));
        }
    }

    /* 강의 상세 조회 */
    @GetMapping("/{lecIdx}")
    public ResponseEntity<?> getLectureById(@PathVariable Integer lecIdx) {
        try {
            return lectureService.getLectureById(lecIdx)
                    .map(this::convertToDto)
                    .map(ResponseEntity::ok)
                    .orElse(ResponseEntity.notFound().build());
        } catch (Exception e) {
            logger.error("강의 조회 실패: lecIdx={}", lecIdx, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("강의 조회 중 오류가 발생했습니다."));
        }
    }

    /* 강의별 통계 조회 */
    @GetMapping("/{lecIdx}/stats")
    public ResponseEntity<?> getLectureStats(@PathVariable Integer lecIdx) {
        try {
            Map<String, Object> stats = lectureService.getLectureStatistics(lecIdx);
            return ResponseEntity.ok(stats);
        } catch (IllegalArgumentException e) {
            logger.warn("강의 통계 조회 실패: {}", e.getMessage());
            return ResponseEntity.badRequest().body(createErrorResponse(e.getMessage()));
        } catch (Exception e) {
            logger.error("강의 통계 조회 실패: lecIdx={}", lecIdx, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("통계 조회 중 오류가 발생했습니다."));
        }
    }

    /* 강의 등록 */
    @PostMapping
    public ResponseEntity<?> createLecture(@RequestBody LecTbl lecture) {
        try {
            LecTbl created = lectureService.createLecture(lecture);
            return ResponseEntity.status(HttpStatus.CREATED).body(created);
        } catch (IllegalArgumentException e) {
            logger.warn("강의 등록 실패: {}", e.getMessage());
            return ResponseEntity.badRequest().body(createErrorResponse(e.getMessage()));
        } catch (Exception e) {
            logger.error("강의 등록 실패", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("강의 등록 중 오류가 발생했습니다."));
        }
    }

    /* 강의 수정 */
    @PutMapping("/{lecIdx}")
    public ResponseEntity<?> updateLecture(
            @PathVariable Integer lecIdx,
            @RequestBody LecTbl lecture) {
        try {
            lecture.setLecIdx(lecIdx);
            LecTbl updated = lectureService.updateLecture(lecture);
            return ResponseEntity.ok(updated);
        } catch (IllegalArgumentException e) {
            logger.warn("강의 수정 실패: {}", e.getMessage());
            return ResponseEntity.badRequest().body(createErrorResponse(e.getMessage()));
        } catch (Exception e) {
            logger.error("강의 수정 실패: lecIdx={}", lecIdx, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("강의 수정 중 오류가 발생했습니다."));
        }
    }

    /* 강의 삭제 */
    @DeleteMapping("/{lecIdx}")
    public ResponseEntity<?> deleteLecture(@PathVariable Integer lecIdx) {
        try {
            lectureService.deleteLecture(lecIdx);
            return ResponseEntity.ok(createSuccessResponse("강의가 삭제되었습니다."));
        } catch (IllegalArgumentException e) {
            logger.warn("강의 삭제 실패: {}", e.getMessage());
            return ResponseEntity.badRequest().body(createErrorResponse(e.getMessage()));
        } catch (Exception e) {
            logger.error("강의 삭제 실패: lecIdx={}", lecIdx, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("강의 삭제 중 오류가 발생했습니다."));
        }
    }

    // ========== 유틸리티 메서드 ==========

    private Map<String, Object> createErrorResponse(String message) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", false);
        response.put("message", message);
        return response;
    }

    private Map<String, Object> createSuccessResponse(String message) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", message);
        return response;
    }

    /**
     * Entity를 DTO로 변환 (교수 이름 조회 포함)
     */
    private LectureDto convertToDto(LecTbl entity) {
        if (entity == null) {
            return null;
        }

        LectureDto dto = new LectureDto();
        dto.setLecIdx(entity.getLecIdx());
        dto.setLecSerial(entity.getLecSerial());
        dto.setLecTit(entity.getLecTit());
        dto.setLecProf(entity.getLecProf());  // 교수코드
        dto.setLecSummary(entity.getLecSummary());  // 강의 설명
        dto.setLecPoint(entity.getLecPoint());
        dto.setLecMajor(entity.getLecMajor());
        dto.setLecMust(entity.getLecMust());
        dto.setLecTime(entity.getLecTime());
        dto.setLecAssign(entity.getLecAssign());
        dto.setLecOpen(entity.getLecOpen());
        dto.setLecMany(entity.getLecMany());
        dto.setLecCurrent(entity.getLecCurrent());
        dto.setLecMcode(entity.getLecMcode());
        dto.setLecMcodeDep(entity.getLecMcodeDep());
        dto.setLecMin(entity.getLecMin());
        dto.setLecYear(entity.getLecYear());
        dto.setLecSemester(entity.getLecSemester());

        // 교수코드로 교수 이름 조회
        if (entity.getLecProf() != null && !entity.getLecProf().isEmpty()) {
            try {
                userTblRepository.findByUserCode(entity.getLecProf())
                    .ifPresent(professor -> dto.setLecProfName(professor.getUserName()));
            } catch (Exception e) {
                logger.warn("교수 정보 조회 실패 (USER_CODE: {}): {}", entity.getLecProf(), e.getMessage());
            }
        }

        return dto;
    }

    /**
     * Entity List를 DTO List로 변환
     */
    private List<LectureDto> convertToDtoList(List<LecTbl> entities) {
        if (entities == null) {
            return List.of();
        }
        return entities.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    /**
     * Entity Page를 DTO Page로 변환
     */
    private Page<LectureDto> convertToDtoPage(Page<LecTbl> entityPage) {
        if (entityPage == null) {
            return Page.empty();
        }
        List<LectureDto> dtoList = convertToDtoList(entityPage.getContent());
        return new PageImpl<>(dtoList, entityPage.getPageable(), entityPage.getTotalElements());
    }
}
