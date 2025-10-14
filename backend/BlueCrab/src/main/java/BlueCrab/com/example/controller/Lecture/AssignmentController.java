// 작성자: 성태준
// 과제 관리 컨트롤러

package BlueCrab.com.example.controller.Lecture;

import BlueCrab.com.example.entity.Lecture.AssignmentExtendedTbl;
import BlueCrab.com.example.service.Lecture.AssignmentService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/* 과제 관리 REST API 컨트롤러 (통합 버전)
 * 
 * 주요 엔드포인트:
 * - GET /api/assignments - 과제 조회 (쿼리 파라미터로 필터링)
 * - GET /api/assignments/{id} - 과제 상세 조회
 * - GET /api/assignments/{id}/data - JSON 데이터 파싱
 * - POST /api/assignments - 과제 등록
 * - POST /api/assignments/{id}/submit - 과제 제출
 * - PUT /api/assignments/{id} - 과제 수정
 * - PUT /api/assignments/{id}/grade - 과제 채점
 * - DELETE /api/assignments/{id} - 과제 삭제
 */
@RestController
@RequestMapping("/api/assignments")
public class AssignmentController {

    private static final Logger logger = LoggerFactory.getLogger(AssignmentController.class);

    @Autowired
    private AssignmentService assignmentService;

    /* 과제 목록 조회 (통합 엔드포인트)
     * 
     * 쿼리 파라미터:
     * - lecIdx: 강의 ID로 필터
     * - withLecture: 강의 정보 포함 여부 (lecIdx 필요)
     * - stats: 통계 조회
     * - page, size: 페이징
     */
    @GetMapping
    public ResponseEntity<?> getAssignments(
            @RequestParam(required = false) Integer lecIdx,
            @RequestParam(defaultValue = "false") boolean withLecture,
            @RequestParam(defaultValue = "false") boolean stats,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        try {
            // 1. 통계 조회
            if (stats) {
                Map<String, Object> statistics = new HashMap<>();
                if (lecIdx != null) {
                    statistics.put("assignmentCount", assignmentService.countAssignmentsByLecture(lecIdx));
                    statistics.put("lecIdx", lecIdx);
                } else {
                    statistics.put("totalCount", assignmentService.countAllAssignments());
                }
                return ResponseEntity.ok(statistics);
            }
            
            // 2. 강의별 과제 목록 (강의 정보 포함)
            if (lecIdx != null && withLecture) {
                List<AssignmentExtendedTbl> assignments = 
                        assignmentService.getAssignmentsWithLecture(lecIdx);
                return ResponseEntity.ok(assignments);
            }
            
            // 3. 강의별 과제 목록 (페이징)
            if (lecIdx != null) {
                Pageable pageable = PageRequest.of(page, size);
                Page<AssignmentExtendedTbl> assignments = 
                        assignmentService.getAssignmentsByLecturePaged(lecIdx, pageable);
                return ResponseEntity.ok(assignments);
            }
            
            // 4. 전체 목록 (페이징)
            Pageable pageable = PageRequest.of(page, size);
            Page<AssignmentExtendedTbl> assignments = assignmentService.getAllAssignments(pageable);
            return ResponseEntity.ok(assignments);
            
        } catch (Exception e) {
            logger.error("과제 목록 조회 실패", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("과제 목록 조회 중 오류가 발생했습니다."));
        }
    }

    /* 과제 상세 조회 */
    @GetMapping("/{assignmentIdx}")
    public ResponseEntity<?> getAssignmentById(@PathVariable Integer assignmentIdx) {
        try {
            return assignmentService.getAssignmentById(assignmentIdx)
                    .map(ResponseEntity::ok)
                    .orElse(ResponseEntity.notFound().build());
        } catch (Exception e) {
            logger.error("과제 조회 실패: assignmentIdx={}", assignmentIdx, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("과제 조회 중 오류가 발생했습니다."));
        }
    }

    /* assignmentData JSON 파싱 조회 */
    @GetMapping("/{assignmentIdx}/data")
    public ResponseEntity<?> getAssignmentData(@PathVariable Integer assignmentIdx) {
        try {
            AssignmentExtendedTbl assignment = assignmentService.getAssignmentById(assignmentIdx)
                    .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 과제입니다."));
            
            Map<String, Object> parsedData = assignmentService.parseAssignmentData(assignment.getAssignmentData());
            return ResponseEntity.ok(parsedData);
        } catch (IllegalArgumentException e) {
            logger.warn("assignmentData 조회 실패: {}", e.getMessage());
            return ResponseEntity.badRequest().body(createErrorResponse(e.getMessage()));
        } catch (Exception e) {
            logger.error("assignmentData 조회 실패: assignmentIdx={}", assignmentIdx, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("데이터 조회 중 오류가 발생했습니다."));
        }
    }

    /* 과제 등록 */
    @PostMapping
    public ResponseEntity<?> createAssignment(@RequestBody Map<String, Object> request) {
        try {
            Integer lecIdx = (Integer) request.get("lecIdx");
            String title = (String) request.get("title");
            String body = (String) request.get("body");  // ✅ DTO 패턴: body 필드
            String dueDate = (String) request.get("dueDate");
            Integer maxScore = (Integer) request.get("maxScore");
            
            if (lecIdx == null || title == null || dueDate == null) {
                return ResponseEntity.badRequest()
                        .body(createErrorResponse("lecIdx, title, dueDate는 필수입니다."));
            }
            
            AssignmentExtendedTbl assignment = assignmentService.createAssignment(
                    lecIdx, title, body, dueDate, maxScore);  // body 전달
            return ResponseEntity.status(HttpStatus.CREATED).body(assignment);
        } catch (IllegalArgumentException e) {
            logger.warn("과제 등록 실패: {}", e.getMessage());
            return ResponseEntity.badRequest().body(createErrorResponse(e.getMessage()));
        } catch (Exception e) {
            logger.error("과제 등록 실패", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("과제 등록 중 오류가 발생했습니다."));
        }
    }

    /* 과제 제출 */
    @PostMapping("/{assignmentIdx}/submit")
    public ResponseEntity<?> submitAssignment(
            @PathVariable Integer assignmentIdx,
            @RequestBody Map<String, Object> request) {
        try {
            Integer studentIdx = (Integer) request.get("studentIdx");
            String fileUrl = (String) request.get("fileUrl");
            
            if (studentIdx == null || fileUrl == null) {
                return ResponseEntity.badRequest()
                        .body(createErrorResponse("studentIdx와 fileUrl은 필수입니다."));
            }
            
            AssignmentExtendedTbl assignment = assignmentService.submitAssignment(
                    assignmentIdx, studentIdx, fileUrl);
            return ResponseEntity.ok(assignment);
        } catch (IllegalArgumentException e) {
            logger.warn("과제 제출 실패: {}", e.getMessage());
            return ResponseEntity.badRequest().body(createErrorResponse(e.getMessage()));
        } catch (Exception e) {
            logger.error("과제 제출 실패: assignmentIdx={}", assignmentIdx, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("과제 제출 중 오류가 발생했습니다."));
        }
    }

    /* 과제 수정 */
    @PutMapping("/{assignmentIdx}")
    public ResponseEntity<?> updateAssignment(
            @PathVariable Integer assignmentIdx,
            @RequestBody Map<String, Object> request) {
        try {
            String title = (String) request.get("title");
            String body = (String) request.get("body");  // ✅ DTO 패턴: body 필드
            String dueDate = (String) request.get("dueDate");
            Integer maxScore = (Integer) request.get("maxScore");
            
            AssignmentExtendedTbl updated = assignmentService.updateAssignment(
                    assignmentIdx, title, body, dueDate, maxScore);  // body 전달
            return ResponseEntity.ok(updated);
        } catch (IllegalArgumentException e) {
            logger.warn("과제 수정 실패: {}", e.getMessage());
            return ResponseEntity.badRequest().body(createErrorResponse(e.getMessage()));
        } catch (Exception e) {
            logger.error("과제 수정 실패: assignmentIdx={}", assignmentIdx, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("과제 수정 중 오류가 발생했습니다."));
        }
    }

    /* 과제 채점 */
    @PutMapping("/{assignmentIdx}/grade")
    public ResponseEntity<?> gradeAssignment(
            @PathVariable Integer assignmentIdx,
            @RequestBody Map<String, Object> request) {
        try {
            Integer studentIdx = (Integer) request.get("studentIdx");
            Integer score = (Integer) request.get("score");
            String feedback = (String) request.get("feedback");
            
            if (studentIdx == null || score == null) {
                return ResponseEntity.badRequest()
                        .body(createErrorResponse("studentIdx와 score는 필수입니다."));
            }
            
            AssignmentExtendedTbl assignment = assignmentService.gradeAssignment(
                    assignmentIdx, studentIdx, score, feedback);
            return ResponseEntity.ok(assignment);
        } catch (IllegalArgumentException e) {
            logger.warn("과제 채점 실패: {}", e.getMessage());
            return ResponseEntity.badRequest().body(createErrorResponse(e.getMessage()));
        } catch (Exception e) {
            logger.error("과제 채점 실패: assignmentIdx={}", assignmentIdx, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("과제 채점 중 오류가 발생했습니다."));
        }
    }

    /* 과제 삭제 */
    @DeleteMapping("/{assignmentIdx}")
    public ResponseEntity<?> deleteAssignment(@PathVariable Integer assignmentIdx) {
        try {
            assignmentService.deleteAssignment(assignmentIdx);
            return ResponseEntity.ok(createSuccessResponse("과제가 삭제되었습니다."));
        } catch (IllegalArgumentException e) {
            logger.warn("과제 삭제 실패: {}", e.getMessage());
            return ResponseEntity.badRequest().body(createErrorResponse(e.getMessage()));
        } catch (Exception e) {
            logger.error("과제 삭제 실패: assignmentIdx={}", assignmentIdx, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("과제 삭제 중 오류가 발생했습니다."));
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
}
