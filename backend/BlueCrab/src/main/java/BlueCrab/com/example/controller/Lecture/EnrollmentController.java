// 작성자: 성태준
// 수강신청 관리 컨트롤러

package BlueCrab.com.example.controller.Lecture;

import BlueCrab.com.example.dto.Lecture.EnrollmentDto;
import BlueCrab.com.example.entity.Lecture.EnrollmentExtendedTbl;
import BlueCrab.com.example.entity.Lecture.LecTbl;
import BlueCrab.com.example.entity.UserTbl;
import BlueCrab.com.example.service.Lecture.EnrollmentService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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
                List<EnrollmentDto> dtoList = convertToDtoList(enrollments);
                return ResponseEntity.ok(dtoList);
            }
            
            // 4. 학생별 수강 목록 (페이징) - DTO 변환
            if (studentIdx != null && !enrolled) {
                Pageable pageable = PageRequest.of(page, size);
                Page<EnrollmentExtendedTbl> enrollments = 
                        enrollmentService.getEnrollmentsByStudentPaged(studentIdx, pageable);
                // DTO 리스트로 변환 후 새 Page 객체 생성 (Entity 참조 제거)
                List<EnrollmentDto> dtoList = enrollments.getContent().stream()
                        .map(this::convertToDto)
                        .collect(Collectors.toList());
                Page<EnrollmentDto> dtoPage = new PageImpl<>(dtoList, pageable, enrollments.getTotalElements());
                return ResponseEntity.ok(dtoPage);
            }
            
            // 5. 강의별 수강생 목록 (페이징) - DTO 변환
            if (lecIdx != null) {
                Pageable pageable = PageRequest.of(page, size);
                Page<EnrollmentExtendedTbl> enrollments = 
                        enrollmentService.getEnrollmentsByLecturePaged(lecIdx, pageable);
                // DTO 리스트로 변환 후 새 Page 객체 생성 (Entity 참조 제거)
                List<EnrollmentDto> dtoList = enrollments.getContent().stream()
                        .map(this::convertToDto)
                        .collect(Collectors.toList());
                Page<EnrollmentDto> dtoPage = new PageImpl<>(dtoList, pageable, enrollments.getTotalElements());
                return ResponseEntity.ok(dtoPage);
            }
            
            // 6. 전체 목록 - DTO 변환
            Pageable pageable = PageRequest.of(page, size);
            Page<EnrollmentExtendedTbl> enrollments = enrollmentService.getAllEnrollments(pageable);
            // DTO 리스트로 변환 후 새 Page 객체 생성 (Entity 참조 제거)
            List<EnrollmentDto> dtoList = enrollments.getContent().stream()
                    .map(this::convertToDto)
                    .collect(Collectors.toList());
            Page<EnrollmentDto> dtoPage = new PageImpl<>(dtoList, pageable, enrollments.getTotalElements());
            return ResponseEntity.ok(dtoPage);
            
        } catch (Exception e) {
            logger.error("수강신청 조회 실패", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("수강신청 조회 중 오류가 발생했습니다."));
        }
    }

    /* 수강신청 상세 조회 - DTO 변환 */
    @GetMapping("/{enrollmentIdx}")
    public ResponseEntity<?> getEnrollmentById(@PathVariable Integer enrollmentIdx) {
        try {
            return enrollmentService.getEnrollmentById(enrollmentIdx)
                    .map(this::convertToDto)
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

    /**
     * EnrollmentExtendedTbl 엔티티를 EnrollmentDto로 변환
     * Lazy Loading 문제를 방지하고 필요한 정보만 포함
     * 
     * @param entity 수강신청 엔티티
     * @return EnrollmentDto
     */
    private EnrollmentDto convertToDto(EnrollmentExtendedTbl entity) {
        if (entity == null) {
            return null;
        }

        EnrollmentDto dto = new EnrollmentDto();
        dto.setEnrollmentIdx(entity.getEnrollmentIdx());
        dto.setLecIdx(entity.getLecIdx());
        dto.setStudentIdx(entity.getStudentIdx());
        dto.setEnrollmentStatus(entity.getEnrollmentData());

        // Lecture 정보 추가 (Lazy Loading 방지)
        try {
            LecTbl lecture = entity.getLecture();
            if (lecture != null) {
                dto.setLecSerial(lecture.getLecSerial());
                dto.setLecTit(lecture.getLecTit());
                dto.setLecProf(lecture.getLecProf());
                dto.setLecPoint(lecture.getLecPoint());
                dto.setLecTime(lecture.getLecTime());
            }
        } catch (Exception e) {
            logger.warn("강의 정보 조회 실패 (Lazy Loading): {}", e.getMessage());
        }

        // Student 정보 추가 (Lazy Loading 방지)
        try {
            UserTbl student = entity.getStudent();
            if (student != null) {
                dto.setStudentName(student.getUserName());
                dto.setStudentCode(student.getUserCode());
            }
        } catch (Exception e) {
            logger.warn("학생 정보 조회 실패 (Lazy Loading): {}", e.getMessage());
        }

        // JSON 데이터 파싱 (enrollment date 추출)
        try {
            if (entity.getEnrollmentData() != null && !entity.getEnrollmentData().isEmpty()) {
                ObjectMapper mapper = new ObjectMapper();
                JsonNode root = mapper.readTree(entity.getEnrollmentData());
                JsonNode enrollment = root.path("enrollment");
                if (enrollment.has("enrollmentDate")) {
                    dto.setEnrollmentDate(enrollment.get("enrollmentDate").asText());
                }
                if (enrollment.has("status")) {
                    dto.setEnrollmentStatus(enrollment.get("status").asText());
                }
            }
        } catch (Exception e) {
            logger.warn("JSON 데이터 파싱 실패: {}", e.getMessage());
        }

        return dto;
    }

    /**
     * List<EnrollmentExtendedTbl>을 List<EnrollmentDto>로 변환
     */
    private List<EnrollmentDto> convertToDtoList(List<EnrollmentExtendedTbl> entities) {
        if (entities == null) {
            return null;
        }
        return entities.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }
}
