// 작성자: 성태준
// 강의 관리 컨트롤러

package BlueCrab.com.example.controller.Lecture;

import BlueCrab.com.example.dto.Lecture.LectureDto;
import BlueCrab.com.example.entity.Lecture.LecTbl;
import BlueCrab.com.example.entity.UserTbl;
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
 * - GET /api/lectures/eligible/{studentId} - 학생별 수강 가능 강의 조회 (0값 규칙 적용)
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

    /* 강의 목록 조회 및 검색 (통합 엔드포인트) - POST 방식
     * 
     * Request Body:
     * - serial: 강의코드로 단일 조회
     * - professor: 교수명 필터
     * - year: 학년 필터
     * - semester: 학기 필터
     * - title: 강의명 검색
     * - major: 전공 코드 필터
     * - open: 개설 여부 (1/0)
     * - page, size: 페이징
     */
    @PostMapping
    public ResponseEntity<?> getLectures(@RequestBody(required = false) Map<String, Object> request) {
        // Request Body에서 파라미터 추출 (null 처리)
        if (request == null) {
            request = new HashMap<>();
        }
        
        String serial = (String) request.get("serial");
        String professor = (String) request.get("professor");
        Integer year = request.get("year") != null ? ((Number) request.get("year")).intValue() : null;
        Integer semester = request.get("semester") != null ? ((Number) request.get("semester")).intValue() : null;
        String title = (String) request.get("title");
        Integer major = request.get("major") != null ? ((Number) request.get("major")).intValue() : null;
        Integer open = request.get("open") != null ? ((Number) request.get("open")).intValue() : null;
        int page = request.get("page") != null ? ((Number) request.get("page")).intValue() : 0;
        int size = request.get("size") != null ? ((Number) request.get("size")).intValue() : 20;
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

    /* 강의 상세 조회 - POST 방식 */
    @PostMapping("/detail")
    public ResponseEntity<?> getLectureById(@RequestBody Map<String, Object> request) {
        try {
            Integer lecIdx = request.get("lecIdx") != null ? ((Number) request.get("lecIdx")).intValue() : null;
            
            if (lecIdx == null) {
                return ResponseEntity.badRequest()
                        .body(createErrorResponse("lecIdx는 필수입니다."));
            }
            
            return lectureService.getLectureById(lecIdx)
                    .map(this::convertToDto)
                    .map(ResponseEntity::ok)
                    .orElse(ResponseEntity.notFound().build());
        } catch (Exception e) {
            logger.error("강의 조회 실패", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("강의 조회 중 오류가 발생했습니다."));
        }
    }

    /* 강의별 통계 조회 - POST 방식 */
    @PostMapping("/stats")
    public ResponseEntity<?> getLectureStats(@RequestBody Map<String, Object> request) {
        try {
            Integer lecIdx = request.get("lecIdx") != null ? ((Number) request.get("lecIdx")).intValue() : null;
            
            if (lecIdx == null) {
                return ResponseEntity.badRequest()
                        .body(createErrorResponse("lecIdx는 필수입니다."));
            }
            
            Map<String, Object> stats = lectureService.getLectureStatistics(lecIdx);
            return ResponseEntity.ok(stats);
        } catch (IllegalArgumentException e) {
            logger.warn("강의 통계 조회 실패: {}", e.getMessage());
            return ResponseEntity.badRequest().body(createErrorResponse(e.getMessage()));
        } catch (Exception e) {
            logger.error("강의 통계 조회 실패", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("통계 조회 중 오류가 발생했습니다."));
        }
    }

    /* 학생별 수강 가능 강의 조회 (0값 규칙 적용) - POST 방식 */
    @PostMapping("/eligible")
    public ResponseEntity<?> getEligibleLectures(@RequestBody Map<String, Object> request) {
        Integer studentId = null;
        try {
            studentId = request.get("studentId") != null ? ((Number) request.get("studentId")).intValue() : null;
            int page = request.get("page") != null ? ((Number) request.get("page")).intValue() : 0;
            int size = request.get("size") != null ? ((Number) request.get("size")).intValue() : 20;
            
            if (studentId == null) {
                return ResponseEntity.badRequest()
                        .body(createErrorResponse("studentId는 필수입니다."));
            }
            // 1. 학생 정보 조회
            UserTbl student = userTblRepository.findById(studentId).orElse(null);
            if (student == null) {
                return ResponseEntity.badRequest()
                        .body(createErrorResponse("존재하지 않는 학생입니다."));
            }

            // 2. 학생 권한 확인 (0: 학생, 1: 교수)
            if (student.getUserStudent() == null || student.getUserStudent() != 0) {
                return ResponseEntity.badRequest()
                        .body(createErrorResponse("학생 권한이 필요합니다."));
            }

            // 3. 수강 가능 강의 필터링 (0값 규칙 적용)
            List<LecTbl> allLectures = lectureService.getAllLecturesForEligibility();
            
            // 4. 수강 자격 검증 및 필터링
            List<Map<String, Object>> eligibleLectures = allLectures.stream()
                    .filter(lecture -> isEligibleForLecture(student, lecture))
                    .map(lecture -> createEligibilityResponse(student, lecture))
                    .collect(Collectors.toList());

            // 5. 페이징 처리
            int start = page * size;
            int end = Math.min(start + size, eligibleLectures.size());
            List<Map<String, Object>> pagedLectures = eligibleLectures.subList(start, end);

            // 6. 응답 구성
            Map<String, Object> response = new HashMap<>();
            response.put("eligibleLectures", pagedLectures);
            response.put("totalCount", allLectures.size());
            response.put("eligibleCount", eligibleLectures.size());
            response.put("ineligibleCount", allLectures.size() - eligibleLectures.size());
            response.put("pagination", createPaginationInfo(page, size, eligibleLectures.size()));
            response.put("studentInfo", createStudentInfo(student));

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("수강 가능 강의 조회 실패: studentId={}", studentId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("수강 가능 강의 조회 중 오류가 발생했습니다."));
        }
    }

    /**
     * 학생이 특정 강의를 수강할 수 있는지 검증 (0값 규칙 적용)
     */
    private boolean isEligibleForLecture(UserTbl student, LecTbl lecture) {
        // 현재 UserTbl에 학부/학과/학년 정보가 없으므로 기본 검증만 수행
        // TODO: 향후 UserTbl에 학부/학과/학년 정보 추가 시 확장 필요

        // 1. 개설 여부 확인 (필수)
        if (lecture.getLecOpen() == null || lecture.getLecOpen() != 1) {
            return false;
        }

        // 2. 정원 확인 (필수)
        if (lecture.getLecCurrent() != null && lecture.getLecMany() != null) {
            if (lecture.getLecCurrent() >= lecture.getLecMany()) {
                return false;
            }
        }

        // 3. 0값 규칙 적용 (향후 확장 시 사용)
        /*
        // 학부 코드 검증 (0이면 제한 없음)
        if (!"0".equals(lecture.getLecMcode()) && !lecture.getLecMcode().equals(student.getFacultyCode())) {
            return false;
        }

        // 학과 코드 검증 (0이면 제한 없음)
        if (!"0".equals(lecture.getLecMcodeDep()) && !lecture.getLecMcodeDep().equals(student.getDepartmentCode())) {
            return false;
        }

        // 최소 학년 검증 (0이면 제한 없음)
        if (lecture.getLecMin() != null && lecture.getLecMin() > 0) {
            if (student.getCurrentGrade() == null || student.getCurrentGrade() < lecture.getLecMin()) {
                return false;
            }
        }
        */

        return true;
    }

    /**
     * 수강 자격 검증 결과를 포함한 응답 생성
     */
    private Map<String, Object> createEligibilityResponse(UserTbl student, LecTbl lecture) {
        Map<String, Object> response = new HashMap<>();
        
        // 기본 강의 정보
        response.put("lecIdx", lecture.getLecIdx());
        response.put("lecSerial", lecture.getLecSerial());
        response.put("lecTit", lecture.getLecTit());
        response.put("lecProf", lecture.getLecProf());
        response.put("lecPoint", lecture.getLecPoint());
        response.put("lecTime", lecture.getLecTime());
        response.put("lecCurrent", lecture.getLecCurrent());
        response.put("lecMany", lecture.getLecMany());
        response.put("lecMcode", lecture.getLecMcode());
        response.put("lecMcodeDep", lecture.getLecMcodeDep());
        response.put("lecMin", lecture.getLecMin());

        // 수강 자격 정보
        boolean isEligible = isEligibleForLecture(student, lecture);
        response.put("isEligible", isEligible);
        response.put("eligibilityReason", getEligibilityReason(student, lecture, isEligible));

        return response;
    }

    /**
     * 수강 자격 검증 사유 생성
     */
    private String getEligibilityReason(UserTbl student, LecTbl lecture, boolean isEligible) {
        if (!isEligible) {
            // 개설 여부 확인
            if (lecture.getLecOpen() == null || lecture.getLecOpen() != 1) {
                return "개설되지 않은 강의입니다";
            }
            
            // 정원 확인
            if (lecture.getLecCurrent() != null && lecture.getLecMany() != null) {
                if (lecture.getLecCurrent() >= lecture.getLecMany()) {
                    return "정원이 초과되었습니다";
                }
            }
            
            return "수강 불가 (기타 사유)";
        }
        
        return "수강 가능";
    }

    /**
     * 페이징 정보 생성
     */
    private Map<String, Object> createPaginationInfo(int page, int size, int totalElements) {
        Map<String, Object> pagination = new HashMap<>();
        pagination.put("currentPage", page);
        pagination.put("pageSize", size);
        pagination.put("totalElements", totalElements);
        pagination.put("totalPages", (int) Math.ceil((double) totalElements / size));
        return pagination;
    }

    /**
     * 학생 정보 생성
     */
    private Map<String, Object> createStudentInfo(UserTbl student) {
        Map<String, Object> studentInfo = new HashMap<>();
        studentInfo.put("userIdx", student.getUserIdx());
        studentInfo.put("userName", student.getUserName());
        studentInfo.put("userEmail", student.getUserEmail());
        studentInfo.put("userStudent", student.getUserStudent());
        // TODO: 향후 학부/학과/학년 정보 추가
        return studentInfo;
    }

    /* 강의 등록 */
    @PostMapping("/create")
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
