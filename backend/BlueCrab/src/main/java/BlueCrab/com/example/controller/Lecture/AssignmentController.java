// 작성자: 성태준

package BlueCrab.com.example.controller.Lecture;

import BlueCrab.com.example.entity.Lecture.AssignmentExtendedTbl;
import BlueCrab.com.example.event.Lecture.GradeUpdateEvent;
import BlueCrab.com.example.service.Lecture.AssignmentService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/assignments")
public class AssignmentController {

    private static final Logger logger = LoggerFactory.getLogger(AssignmentController.class);

    @Autowired
    private AssignmentService assignmentService;

    @Autowired
    private ApplicationEventPublisher eventPublisher;

    /* 과제 목록 조회 (통합 엔드포인트) - POST 방식
     * 
     * Request Body:
     * - lecSerial: 강의 코드로 필터 (lecIdx 대신 사용)
     * - withLecture: 강의 정보 포함 여부 (lecSerial 필요)
     * - stats: 통계 조회
     * - action: "list" (목록 조회 액션)
     * - page, size: 페이징
     */
    @PostMapping("/list")
    public ResponseEntity<?> getAssignments(@RequestBody(required = false) Map<String, Object> request) {
        // Request Body에서 파라미터 추출 (null 처리)
        if (request == null) {
            request = new HashMap<>();
        }
        
        String lecSerial = request.get("lecSerial") != null ? request.get("lecSerial").toString() : null;
        boolean withLecture = request.get("withLecture") != null ? (Boolean) request.get("withLecture") : false;
        boolean stats = request.get("stats") != null ? (Boolean) request.get("stats") : false;
        int page = request.get("page") != null ? ((Number) request.get("page")).intValue() : 0;
        int size = request.get("size") != null ? ((Number) request.get("size")).intValue() : 20;
        try {
            // lecSerial → lecIdx 변환 (필요한 경우)
            Integer lecIdx = null;
            if (lecSerial != null && !lecSerial.isEmpty()) {
                lecIdx = assignmentService.getLectureIdxBySerial(lecSerial);
                if (lecIdx == null) {
                    return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("success", false, "message", "강의 코드(lecSerial)를 찾을 수 없습니다: " + lecSerial));
                }
            }
            
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

    /* 과제 상세 조회 - POST 방식 */
    @PostMapping("/detail")
    public ResponseEntity<?> getAssignmentById(@RequestBody Map<String, Object> request) {
        try {
            Integer assignmentIdx = request.get("assignmentIdx") != null ? 
                    ((Number) request.get("assignmentIdx")).intValue() : null;
            
            if (assignmentIdx == null) {
                return ResponseEntity.badRequest()
                        .body(createErrorResponse("assignmentIdx는 필수입니다."));
            }
            
            return assignmentService.getAssignmentById(assignmentIdx)
                    .map(ResponseEntity::ok)
                    .orElse(ResponseEntity.notFound().build());
        } catch (Exception e) {
            logger.error("과제 조회 실패", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("과제 조회 중 오류가 발생했습니다."));
        }
    }

    /* assignmentData JSON 파싱 조회 - POST 방식 */
    @PostMapping("/data")
    public ResponseEntity<?> getAssignmentData(@RequestBody Map<String, Object> request) {
        try {
            Integer assignmentIdx = request.get("assignmentIdx") != null ? 
                    ((Number) request.get("assignmentIdx")).intValue() : null;
            
            if (assignmentIdx == null) {
                return ResponseEntity.badRequest()
                        .body(createErrorResponse("assignmentIdx는 필수입니다."));
            }
            
            AssignmentExtendedTbl assignment = assignmentService.getAssignmentById(assignmentIdx)
                    .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 과제입니다."));
            
            Map<String, Object> parsedData = assignmentService.parseAssignmentData(assignment.getAssignmentData());
            return ResponseEntity.ok(parsedData);
        } catch (IllegalArgumentException e) {
            logger.warn("assignmentData 조회 실패: {}", e.getMessage());
            return ResponseEntity.badRequest().body(createErrorResponse(e.getMessage()));
        } catch (Exception e) {
            logger.error("assignmentData 조회 실패", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("데이터 조회 중 오류가 발생했습니다."));
        }
    }

    /* 학생별 제출 현황 조회 - POST 방식 */
    @PostMapping("/submissions")
    public ResponseEntity<?> getSubmissions(@RequestBody Map<String, Object> request) {
        try {
            Integer assignmentIdx = request.get("assignmentIdx") != null ? 
                    ((Number) request.get("assignmentIdx")).intValue() : null;
            
            if (assignmentIdx == null) {
                return ResponseEntity.badRequest()
                        .body(createErrorResponse("assignmentIdx는 필수입니다."));
            }
            
            AssignmentExtendedTbl assignment = assignmentService.getAssignmentById(assignmentIdx)
                    .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 과제입니다."));
            
            Map<String, Object> parsedData = assignmentService.parseAssignmentData(assignment.getAssignmentData());
            return ResponseEntity.ok(parsedData);
        } catch (IllegalArgumentException e) {
            logger.warn("제출 현황 조회 실패: {}", e.getMessage());
            return ResponseEntity.badRequest().body(createErrorResponse(e.getMessage()));
        } catch (Exception e) {
            logger.error("제출 현황 조회 실패", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("제출 현황 조회 중 오류가 발생했습니다."));
        }
    }

    /* 과제 등록 */
    @PostMapping
    public ResponseEntity<?> createAssignment(@RequestBody Map<String, Object> request) {
        try {
            String lecSerial = (String) request.get("lecSerial");
            String title = (String) request.get("title");
            String body = (String) request.get("body");  // ✅ DTO 패턴: body 필드
            String dueDate = (String) request.get("dueDate");
            // ✅ maxScore는 항상 10점으로 고정 (요청값 무시)
            Integer maxScore = 10;
            
            if (lecSerial == null || title == null || dueDate == null) {
                return ResponseEntity.badRequest()
                        .body(createErrorResponse("lecSerial, title, dueDate는 필수입니다."));
            }
            
            // lecSerial → lecIdx 변환
            Integer lecIdx = assignmentService.getLectureIdxBySerial(lecSerial);
            if (lecIdx == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(createErrorResponse("강의 코드(lecSerial)를 찾을 수 없습니다: " + lecSerial));
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
            // ✅ maxScore 수정 시에도 10점으로 고정 (요청값 무시)
            Integer maxScore = request.containsKey("maxScore") ? 10 : null;
            
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
            
            // ✅ 점수가 10점을 초과하면 자동으로 10점으로 변환
            if (score > 10) {
                logger.info("점수 {}점이 10점으로 변환됨 (assignmentIdx={}, studentIdx={})", 
                    score, assignmentIdx, studentIdx);
                score = 10;
            }
            
            AssignmentExtendedTbl assignment = assignmentService.gradeAssignment(
                    assignmentIdx, studentIdx, score, feedback);
            
            // 과제 채점이 완료되면 성적 재계산 이벤트 발행
            Integer lecIdx = assignment.getLecIdx();
            if (lecIdx != null) {
                eventPublisher.publishEvent(
                    new GradeUpdateEvent(this, lecIdx, studentIdx, "ASSIGNMENT")
                );
                logger.info("과제 채점으로 인한 성적 재계산 이벤트 발행: lecIdx={}, studentIdx={}", 
                    lecIdx, studentIdx);
            }
            
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
