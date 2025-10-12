// 작성자: 성태준
// 수강신청 관리 컨트롤러

package BlueCrab.com.example.controller.Lecture;

import BlueCrab.com.example.entity.Lecture.EnrollmentExtendedTbl;
import BlueCrab.com.example.service.Lecture.EnrollmentService;
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

/* 수강신청 관리 REST API 컨트롤러 (통합 버전)
 * 
 * 주요 엔드포인트:
 * - GET /api/enrollments - 수강신청 조회 (쿼리 파라미터로 필터링)
 * - GET /api/enrollments/{id} - 수강신청 상세 조회
 * - GET /api/enrollments/{id}/data - JSON 데이터 파싱
 * - POST /api/enrollments - 수강신청
 * - DELETE /api/enrollments/{id} - 수강 취소
 * - PUT /api/enrollments/{id}/attendance - 출석 업데이트
 * - PUT /api/enrollments/{id}/grade - 성적 업데이트
 */
@RestController
@RequestMapping("/api/enrollments")
public class EnrollmentController {

    private static final Logger logger = LoggerFactory.getLogger(EnrollmentController.class);

    @Autowired
    private EnrollmentService enrollmentService;

    /* 수강신청 목록 조회 (통합 엔드포인트)
     * 
     * 쿼리 파라미터:
     * - studentIdx: 학생 ID로 필터
     * - lecIdx: 강의 ID로 필터
     * - checkEnrollment: 수강 여부 확인 (studentIdx + lecIdx 필요)
     * - enrolled: 현재 수강중인 목록만 (studentIdx 필요)
     * - stats: 통계 조회 (lecIdx 옵션)
     * - page, size: 페이징
     */
    @GetMapping
    public ResponseEntity<?> getEnrollments(
            @RequestParam(required = false) Integer studentIdx,
            @RequestParam(required = false) Integer lecIdx,
            @RequestParam(defaultValue = "false") boolean checkEnrollment,
            @RequestParam(defaultValue = "false") boolean enrolled,
            @RequestParam(defaultValue = "false") boolean stats,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        try {
            // 1. 수강 여부 확인
            if (checkEnrollment && studentIdx != null && lecIdx != null) {
                boolean isEnrolled = enrollmentService.isEnrolled(studentIdx, lecIdx);
                Map<String, Object> result = new HashMap<>();
                result.put("enrolled", isEnrolled);
                result.put("studentIdx", studentIdx);
                result.put("lecIdx", lecIdx);
                return ResponseEntity.ok(result);
            }
            
            // 2. 통계 조회
            if (stats) {
                Map<String, Object> statistics = new HashMap<>();
                if (lecIdx != null) {
                    statistics.put("enrollmentCount", enrollmentService.countEnrollmentsByLecture(lecIdx));
                    statistics.put("lecIdx", lecIdx);
                } else {
                    statistics.put("totalCount", enrollmentService.countAllEnrollments());
                }
                return ResponseEntity.ok(statistics);
            }
            
            // 3. 현재 수강중인 목록 (enrolled = true)
            if (enrolled && studentIdx != null) {
                List<EnrollmentExtendedTbl> enrollments = enrollmentService.getEnrolledByStudent(studentIdx);
                return ResponseEntity.ok(enrollments);
            }
            
            // 4. 학생별 수강 목록 (페이징)
            if (studentIdx != null && !enrolled) {
                Pageable pageable = PageRequest.of(page, size);
                Page<EnrollmentExtendedTbl> enrollments = 
                        enrollmentService.getEnrollmentsByStudentPaged(studentIdx, pageable);
                return ResponseEntity.ok(enrollments);
            }
            
            // 5. 강의별 수강생 목록 (페이징)
            if (lecIdx != null) {
                Pageable pageable = PageRequest.of(page, size);
                Page<EnrollmentExtendedTbl> enrollments = 
                        enrollmentService.getEnrollmentsByLecturePaged(lecIdx, pageable);
                return ResponseEntity.ok(enrollments);
            }
            
            // 6. 전체 목록
            Pageable pageable = PageRequest.of(page, size);
            Page<EnrollmentExtendedTbl> enrollments = enrollmentService.getAllEnrollments(pageable);
            return ResponseEntity.ok(enrollments);
            
        } catch (Exception e) {
            logger.error("수강신청 조회 실패", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("수강신청 조회 중 오류가 발생했습니다."));
        }
    }

    /* 수강신청 상세 조회 */
    @GetMapping("/{enrollmentIdx}")
    public ResponseEntity<?> getEnrollmentById(@PathVariable Integer enrollmentIdx) {
        try {
            return enrollmentService.getEnrollmentById(enrollmentIdx)
                    .map(ResponseEntity::ok)
                    .orElse(ResponseEntity.notFound().build());
        } catch (Exception e) {
            logger.error("수강신청 조회 실패: enrollmentIdx={}", enrollmentIdx, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("수강신청 조회 중 오류가 발생했습니다."));
        }
    }

    /* enrollmentData JSON 파싱 조회 */
    @GetMapping("/{enrollmentIdx}/data")
    public ResponseEntity<?> getEnrollmentData(@PathVariable Integer enrollmentIdx) {
        try {
            EnrollmentExtendedTbl enrollment = enrollmentService.getEnrollmentById(enrollmentIdx)
                    .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 수강신청입니다."));
            
            Map<String, Object> parsedData = enrollmentService.parseEnrollmentData(enrollment.getEnrollmentData());
            return ResponseEntity.ok(parsedData);
        } catch (IllegalArgumentException e) {
            logger.warn("enrollmentData 조회 실패: {}", e.getMessage());
            return ResponseEntity.badRequest().body(createErrorResponse(e.getMessage()));
        } catch (Exception e) {
            logger.error("enrollmentData 조회 실패: enrollmentIdx={}", enrollmentIdx, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("데이터 조회 중 오류가 발생했습니다."));
        }
    }

    /* 수강신청 */
    @PostMapping
    public ResponseEntity<?> enrollInLecture(@RequestBody Map<String, Object> request) {
        try {
            Integer studentIdx = (Integer) request.get("studentIdx");
            Integer lecIdx = (Integer) request.get("lecIdx");
            
            if (studentIdx == null || lecIdx == null) {
                return ResponseEntity.badRequest()
                        .body(createErrorResponse("studentIdx와 lecIdx는 필수입니다."));
            }
            
            EnrollmentExtendedTbl enrollment = enrollmentService.enrollStudent(studentIdx, lecIdx);
            return ResponseEntity.status(HttpStatus.CREATED).body(enrollment);
        } catch (IllegalStateException | IllegalArgumentException e) {
            logger.warn("수강신청 실패: {}", e.getMessage());
            return ResponseEntity.badRequest().body(createErrorResponse(e.getMessage()));
        } catch (Exception e) {
            logger.error("수강신청 실패", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("수강신청 중 오류가 발생했습니다."));
        }
    }

    /* 수강 취소 */
    @DeleteMapping("/{enrollmentIdx}")
    public ResponseEntity<?> cancelEnrollment(@PathVariable Integer enrollmentIdx) {
        try {
            enrollmentService.cancelEnrollment(enrollmentIdx);
            return ResponseEntity.ok(createSuccessResponse("수강이 취소되었습니다."));
        } catch (IllegalArgumentException e) {
            logger.warn("수강 취소 실패: {}", e.getMessage());
            return ResponseEntity.badRequest().body(createErrorResponse(e.getMessage()));
        } catch (Exception e) {
            logger.error("수강 취소 실패: enrollmentIdx={}", enrollmentIdx, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("수강 취소 중 오류가 발생했습니다."));
        }
    }

    /* 출석 정보 업데이트 */
    @PutMapping("/{enrollmentIdx}/attendance")
    public ResponseEntity<?> updateAttendance(
            @PathVariable Integer enrollmentIdx,
            @RequestBody Map<String, Object> request) {
        try {
            Integer attended = (Integer) request.get("attended");
            Integer absent = (Integer) request.get("absent");
            Integer late = (Integer) request.get("late");
            
            EnrollmentExtendedTbl updated = enrollmentService.updateAttendance(
                    enrollmentIdx, attended, absent, late);
            return ResponseEntity.ok(updated);
        } catch (IllegalArgumentException e) {
            logger.warn("출석 정보 업데이트 실패: {}", e.getMessage());
            return ResponseEntity.badRequest().body(createErrorResponse(e.getMessage()));
        } catch (Exception e) {
            logger.error("출석 정보 업데이트 실패: enrollmentIdx={}", enrollmentIdx, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("출석 정보 업데이트 중 오류가 발생했습니다."));
        }
    }

    /* 성적 정보 업데이트 */
    @PutMapping("/{enrollmentIdx}/grade")
    public ResponseEntity<?> updateGrade(
            @PathVariable Integer enrollmentIdx,
            @RequestBody Map<String, Object> request) {
        try {
            String grade = (String) request.get("grade");
            Double score = request.get("score") != null ? 
                    ((Number) request.get("score")).doubleValue() : null;
            
            EnrollmentExtendedTbl updated = enrollmentService.updateGrade(enrollmentIdx, grade, score);
            return ResponseEntity.ok(updated);
        } catch (IllegalArgumentException e) {
            logger.warn("성적 정보 업데이트 실패: {}", e.getMessage());
            return ResponseEntity.badRequest().body(createErrorResponse(e.getMessage()));
        } catch (Exception e) {
            logger.error("성적 정보 업데이트 실패: enrollmentIdx={}", enrollmentIdx, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("성적 정보 업데이트 중 오류가 발생했습니다."));
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
