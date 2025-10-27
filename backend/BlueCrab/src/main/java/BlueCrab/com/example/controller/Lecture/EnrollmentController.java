// 작성자: 성태준

package BlueCrab.com.example.controller.Lecture;

import BlueCrab.com.example.dto.Lecture.EnrollmentDto;
import BlueCrab.com.example.entity.Lecture.EnrollmentExtendedTbl;
import BlueCrab.com.example.entity.Lecture.LecTbl;
import BlueCrab.com.example.entity.UserTbl;
import BlueCrab.com.example.event.Lecture.GradeUpdateEvent;
import BlueCrab.com.example.repository.UserTblRepository;
import BlueCrab.com.example.service.Lecture.EnrollmentService;
import BlueCrab.com.example.util.JwtUtil;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
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

@RestController
@RequestMapping("/api/enrollments")
public class EnrollmentController {

    private static final Logger logger = LoggerFactory.getLogger(EnrollmentController.class);

    @Autowired
    private EnrollmentService enrollmentService;

    @Autowired
    private UserTblRepository userTblRepository;

    @Autowired
    private ApplicationEventPublisher eventPublisher;

    @Autowired
    private JwtUtil jwtUtil;

    /* 수강신청 목록 조회 (통합 엔드포인트) - POST 방식
     * 
     * Request Body:
     * - studentIdx: 학생 ID로 필터 (선택, 없으면 JWT에서 자동 추출)
     * - lecSerial: 강의 코드로 필터 ✅
     * - checkEnrollment: 수강 여부 확인 (studentIdx + lecSerial 필요) ✅
     * - enrolled: 현재 수강중인 목록만 (studentIdx 필요)
     * - stats: 통계 조회 (lecSerial 옵션) ✅
     * - page, size: 페이징
     */
    @PostMapping("/list")
    public ResponseEntity<?> getEnrollments(
            @RequestBody(required = false) Map<String, Object> request,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        // Request Body에서 파라미터 추출 (null 처리)
        if (request == null) {
            request = new HashMap<>();
        }
        
        // studentIdx: request body에 있으면 사용, 없으면 JWT에서 추출
        Integer studentIdx = request.get("studentIdx") != null ? ((Number) request.get("studentIdx")).intValue() : null;
        
        // JWT에서 userId 추출 (studentIdx가 없을 때)
        if (studentIdx == null && authHeader != null && authHeader.startsWith("Bearer ")) {
            try {
                String token = authHeader.substring(7);
                studentIdx = jwtUtil.extractUserId(token);
                logger.info("JWT에서 studentIdx 자동 추출: {}", studentIdx);
            } catch (Exception e) {
                logger.warn("JWT에서 studentIdx 추출 실패: {}", e.getMessage());
            }
        }
        String lecSerial = (String) request.get("lecSerial");
        boolean checkEnrollment = request.get("checkEnrollment") != null ? (Boolean) request.get("checkEnrollment") : false;
        boolean enrolled = request.get("enrolled") != null ? (Boolean) request.get("enrolled") : false;
        boolean stats = request.get("stats") != null ? (Boolean) request.get("stats") : false;
        int page = request.get("page") != null ? ((Number) request.get("page")).intValue() : 0;
        int size = request.get("size") != null ? ((Number) request.get("size")).intValue() : 20;
        try {
            // lecSerial이 있으면 lecIdx로 변환
            Integer lecIdx = null;
            if (lecSerial != null && !lecSerial.trim().isEmpty()) {
                lecIdx = enrollmentService.getLectureIdxBySerial(lecSerial);
                if (lecIdx == null) {
                    return ResponseEntity.badRequest()
                            .body(createErrorResponse("존재하지 않는 강의 코드입니다: " + lecSerial));
                }
            }
            
            // 1. 수강 여부 확인
            if (checkEnrollment && studentIdx != null && lecIdx != null) {
                boolean isEnrolled = enrollmentService.isEnrolled(studentIdx, lecIdx);
                Map<String, Object> result = new HashMap<>();
                result.put("enrolled", isEnrolled);
                result.put("studentIdx", studentIdx);
                result.put("lecSerial", lecSerial);  // lecIdx 대신 lecSerial 반환
                return ResponseEntity.ok(result);
            }
            
            // 2. 통계 조회
            if (stats) {
                Map<String, Object> statistics = new HashMap<>();
                if (lecIdx != null) {
                    statistics.put("enrollmentCount", enrollmentService.countEnrollmentsByLecture(lecIdx));
                    statistics.put("lecSerial", lecSerial);  // lecIdx 대신 lecSerial 반환
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

    /* 수강신청 상세 조회 - POST 방식 */
    @PostMapping("/detail")
    public ResponseEntity<?> getEnrollmentById(@RequestBody Map<String, Object> request) {
        try {
            Integer enrollmentIdx = request.get("enrollmentIdx") != null ? 
                    ((Number) request.get("enrollmentIdx")).intValue() : null;
            
            if (enrollmentIdx == null) {
                return ResponseEntity.badRequest()
                        .body(createErrorResponse("enrollmentIdx는 필수입니다."));
            }
            
            return enrollmentService.getEnrollmentById(enrollmentIdx)
                    .map(this::convertToDto)
                    .map(ResponseEntity::ok)
                    .orElse(ResponseEntity.notFound().build());
        } catch (Exception e) {
            logger.error("수강신청 조회 실패", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("수강신청 조회 중 오류가 발생했습니다."));
        }
    }

    /* enrollmentData JSON 파싱 조회 - POST 방식 */
    @PostMapping("/data")
    public ResponseEntity<?> getEnrollmentData(@RequestBody Map<String, Object> request) {
        try {
            Integer enrollmentIdx = request.get("enrollmentIdx") != null ? 
                    ((Number) request.get("enrollmentIdx")).intValue() : null;
            
            if (enrollmentIdx == null) {
                return ResponseEntity.badRequest()
                        .body(createErrorResponse("enrollmentIdx는 필수입니다."));
            }
            
            EnrollmentExtendedTbl enrollment = enrollmentService.getEnrollmentById(enrollmentIdx)
                    .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 수강신청입니다."));
            
            Map<String, Object> parsedData = enrollmentService.parseEnrollmentData(enrollment.getEnrollmentData());
            return ResponseEntity.ok(parsedData);
        } catch (IllegalArgumentException e) {
            logger.warn("enrollmentData 조회 실패: {}", e.getMessage());
            return ResponseEntity.badRequest().body(createErrorResponse(e.getMessage()));
        } catch (Exception e) {
            logger.error("enrollmentData 조회 실패", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("데이터 조회 중 오류가 발생했습니다."));
        }
    }

    /* 수강신청
     * ✅ lecSerial (강의 코드) 기반으로 수강신청
     */
    @PostMapping("/enroll")
    public ResponseEntity<?> enrollInLecture(@RequestBody Map<String, Object> request) {
        try {
            Integer studentIdx = (Integer) request.get("studentIdx");
            String lecSerial = (String) request.get("lecSerial");
            
            if (studentIdx == null || lecSerial == null || lecSerial.trim().isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(createErrorResponse("studentIdx와 lecSerial(강의 코드)은 필수입니다."));
            }
            
            // lecSerial로 강의 ID 조회는 EnrollmentService에서 처리
            EnrollmentExtendedTbl enrollment = enrollmentService.enrollStudentBySerial(studentIdx, lecSerial);
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
            
            // 출석 정보가 업데이트되면 성적 재계산 이벤트 발행
            Integer lecIdx = updated.getLecIdx();
            Integer studentIdx = updated.getStudentIdx();
            if (lecIdx != null && studentIdx != null) {
                eventPublisher.publishEvent(
                    new GradeUpdateEvent(this, lecIdx, studentIdx, "ATTENDANCE")
                );
                logger.info("출석 업데이트로 인한 성적 재계산 이벤트 발행: lecIdx={}, studentIdx={}", 
                    lecIdx, studentIdx);
            }
            
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

    private Map<String, Object> createSuccessResponse(String message, Object data) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", message);
        response.put("data", data);
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
                dto.setLecProf(lecture.getLecProf());  // 교수코드 (USER_CODE)
                dto.setLecSummary(lecture.getLecSummary());  // 강의 설명
                dto.setLecPoint(lecture.getLecPoint());
                dto.setLecTime(lecture.getLecTime());
                
                // 교수 IDX(USER_IDX)로 교수 이름(USER_NAME) 조회
                if (lecture.getLecProf() != null && !lecture.getLecProf().isEmpty()) {
                    try {
                        Integer professorIdx = Integer.parseInt(lecture.getLecProf());
                        userTblRepository.findById(professorIdx)
                            .ifPresent(professor -> dto.setLecProfName(professor.getUserName()));
                    } catch (NumberFormatException e) {
                        logger.warn("교수 IDX 파싱 실패 (LEC_PROF: {}): {}", lecture.getLecProf(), e.getMessage());
                    } catch (Exception e) {
                        logger.warn("교수 정보 조회 실패 (USER_IDX: {}): {}", lecture.getLecProf(), e.getMessage());
                    }
                }
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

    // ========================================
    // 성적 관리 API 추가
    // ========================================

    /**
     * 성적 구성 설정 API
     * POST /api/enrollments/grade-config
     */
    @PostMapping("/grade-config")
    public ResponseEntity<?> setGradeConfig(@RequestBody Map<String, Object> request) {
        try {
            String action = (String) request.get("action");
            
            if ("set-config".equals(action)) {
                return handleGradeConfig(request);
            }
            
            return ResponseEntity.badRequest()
                .body(createErrorResponse("지원하지 않는 액션입니다."));
                
        } catch (Exception e) {
            logger.error("성적 구성 설정 실패", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(createErrorResponse("성적 구성 설정 중 오류가 발생했습니다."));
        }
    }

    /**
     * 개별 성적 조회 API
     * POST /api/enrollments/grade-info
     */
    @PostMapping("/grade-info")
    public ResponseEntity<?> getGradeInfo(
            @RequestBody Map<String, Object> request,
            @RequestHeader("Authorization") String authHeader) {
        try {
            String action = (String) request.get("action");
            
            switch (action) {
                case "get-grade":
                    return handleStudentGradeInfo(request);
                case "professor-view":
                    return handleProfessorGradeView(request, authHeader);
                default:
                    return ResponseEntity.badRequest()
                        .body(createErrorResponse("지원하지 않는 액션입니다."));
            }
            
        } catch (Exception e) {
            logger.error("성적 조회 실패", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(createErrorResponse("성적 조회 중 오류가 발생했습니다."));
        }
    }

    /**
     * 전체 수강생 성적 목록 API
     * POST /api/enrollments/grade-list
     */
    @PostMapping("/grade-list")
    public ResponseEntity<?> getGradeList(@RequestBody Map<String, Object> request) {
        try {
            String action = (String) request.get("action");
            
            if ("list-all".equals(action)) {
                return handleGradeList(request);
            }
            
            return ResponseEntity.badRequest()
                .body(createErrorResponse("지원하지 않는 액션입니다."));
                
        } catch (Exception e) {
            logger.error("성적 목록 조회 실패", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(createErrorResponse("성적 목록 조회 중 오류가 발생했습니다."));
        }
    }

    /**
     * 최종 등급 배정 API
     * POST /api/enrollments/grade-finalize
     */
    @PostMapping("/grade-finalize")
    public ResponseEntity<?> finalizeGrades(@RequestBody Map<String, Object> request) {
        try {
            String action = (String) request.get("action");
            
            if ("finalize".equals(action)) {
                return handleGradeFinalize(request);
            }
            
            return ResponseEntity.badRequest()
                .body(createErrorResponse("지원하지 않는 액션입니다."));
                
        } catch (Exception e) {
            logger.error("등급 배정 실패", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(createErrorResponse("등급 배정 중 오류가 발생했습니다."));
        }
    }

    // ========================================
    // 성적 관리 핸들러 메서드들
    // ========================================

    /**
     * 성적 구성 설정 핸들러
     */
    private ResponseEntity<?> handleGradeConfig(Map<String, Object> request) {
        Integer lecIdx = request.get("lecIdx") != null ? ((Number) request.get("lecIdx")).intValue() : null;
        String lecSerial = (String) request.get("lecSerial");
        
        // lecSerial이 제공된 경우 lecIdx로 변환
        if (lecIdx == null && lecSerial != null && !lecSerial.trim().isEmpty()) {
            lecIdx = enrollmentService.getLectureIdxBySerial(lecSerial);
            if (lecIdx == null) {
                return ResponseEntity.badRequest()
                    .body(createErrorResponse("존재하지 않는 강의 코드입니다: " + lecSerial));
            }
            request.put("lecIdx", lecIdx);  // 변환된 lecIdx를 request에 추가
        }
        
        if (lecIdx == null) {
            return ResponseEntity.badRequest()
                .body(createErrorResponse("lecIdx 또는 lecSerial은 필수 파라미터입니다."));
        }

        try {
            // 성적 구성 설정 로직을 EnrollmentService에 위임
            Map<String, Object> result = enrollmentService.configureGrade(request);
            return ResponseEntity.ok(createSuccessResponse("성적 구성이 설정되었습니다.", result));
            
        } catch (Exception e) {
            logger.error("성적 구성 설정 처리 실패", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(createErrorResponse("성적 구성 설정 처리 중 오류가 발생했습니다."));
        }
    }

    /**
     * 학생 성적 조회 핸들러
     */
    private ResponseEntity<?> handleStudentGradeInfo(Map<String, Object> request) {
        Integer lecIdx = request.get("lecIdx") != null ? ((Number) request.get("lecIdx")).intValue() : null;
        String lecSerial = (String) request.get("lecSerial");
        Integer studentIdx = request.get("studentIdx") != null ? ((Number) request.get("studentIdx")).intValue() : null;
        
        // lecSerial이 제공된 경우 lecIdx로 변환
        if (lecIdx == null && lecSerial != null && !lecSerial.trim().isEmpty()) {
            lecIdx = enrollmentService.getLectureIdxBySerial(lecSerial);
            if (lecIdx == null) {
                return ResponseEntity.badRequest()
                    .body(createErrorResponse("존재하지 않는 강의 코드입니다: " + lecSerial));
            }
        }
        
        if (lecIdx == null || studentIdx == null) {
            return ResponseEntity.badRequest()
                .body(createErrorResponse("lecIdx(또는 lecSerial)와 studentIdx는 필수 파라미터입니다."));
        }

        try {
            Map<String, Object> gradeInfo = enrollmentService.studentGradeInfo(lecIdx, studentIdx);
            return ResponseEntity.ok(createSuccessResponse("성적 조회가 완료되었습니다.", gradeInfo));
            
        } catch (Exception e) {
            logger.error("학생 성적 조회 실패", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(createErrorResponse("학생 성적 조회 중 오류가 발생했습니다."));
        }
    }

    /**
     * 교수용 성적 조회 핸들러
     */
    private ResponseEntity<?> handleProfessorGradeView(Map<String, Object> request, String authHeader) {
        Integer lecIdx = request.get("lecIdx") != null ? ((Number) request.get("lecIdx")).intValue() : null;
        String lecSerial = (String) request.get("lecSerial");
        Integer studentIdx = request.get("studentIdx") != null ? ((Number) request.get("studentIdx")).intValue() : null;
        Integer professorIdx = request.get("professorIdx") != null ? ((Number) request.get("professorIdx")).intValue() : null;
        
        // professorIdx가 요청에 없으면 JWT 토큰에서 추출
        if (professorIdx == null) {
            try {
                String token = authHeader.substring(7); // "Bearer " 제거
                professorIdx = jwtUtil.extractUserId(token);
                if (professorIdx == null) {
                    return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(createErrorResponse("JWT 토큰에서 교수 정보를 찾을 수 없습니다."));
                }
                logger.debug("JWT에서 professorIdx 추출: {}", professorIdx);
            } catch (Exception e) {
                logger.error("JWT 토큰 파싱 실패", e);
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(createErrorResponse("유효하지 않은 인증 토큰입니다."));
            }
        }
        
        // lecSerial이 제공된 경우 lecIdx로 변환
        if (lecIdx == null && lecSerial != null && !lecSerial.trim().isEmpty()) {
            lecIdx = enrollmentService.getLectureIdxBySerial(lecSerial);
            if (lecIdx == null) {
                return ResponseEntity.badRequest()
                    .body(createErrorResponse("존재하지 않는 강의 코드입니다: " + lecSerial));
            }
        }
        
        if (lecIdx == null || studentIdx == null || professorIdx == null) {
            return ResponseEntity.badRequest()
                .body(createErrorResponse("lecIdx(또는 lecSerial), studentIdx는 필수 파라미터입니다."));
        }

        try {
            Map<String, Object> gradeInfo = enrollmentService.professorGradeView(lecIdx, studentIdx, professorIdx);
            return ResponseEntity.ok(createSuccessResponse("교수용 성적 조회가 완료되었습니다.", gradeInfo));
            
        } catch (Exception e) {
            logger.error("교수용 성적 조회 실패", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(createErrorResponse("교수용 성적 조회 중 오류가 발생했습니다."));
        }
    }

    /**
     * 성적 목록 조회 핸들러
     */
    private ResponseEntity<?> handleGradeList(Map<String, Object> request) {
        Integer lecIdx = request.get("lecIdx") != null ? ((Number) request.get("lecIdx")).intValue() : null;
        String lecSerial = (String) request.get("lecSerial");
        int page = request.get("page") != null ? ((Number) request.get("page")).intValue() : 0;
        int size = request.get("size") != null ? ((Number) request.get("size")).intValue() : 20;
        String sortBy = (String) request.getOrDefault("sortBy", "percentage");
        String sortOrder = (String) request.getOrDefault("sortOrder", "desc");
        
        // lecSerial이 제공된 경우 lecIdx로 변환
        if (lecIdx == null && lecSerial != null && !lecSerial.trim().isEmpty()) {
            lecIdx = enrollmentService.getLectureIdxBySerial(lecSerial);
            if (lecIdx == null) {
                return ResponseEntity.badRequest()
                    .body(createErrorResponse("존재하지 않는 강의 코드입니다: " + lecSerial));
            }
        }
        
        if (lecIdx == null) {
            return ResponseEntity.badRequest()
                .body(createErrorResponse("lecIdx 또는 lecSerial은 필수 파라미터입니다."));
        }

        try {
            Pageable pageable = PageRequest.of(page, size);
            Map<String, Object> gradeList = enrollmentService.gradeList(lecIdx, pageable, sortBy, sortOrder);
            return ResponseEntity.ok(createSuccessResponse("성적 목록 조회가 완료되었습니다.", gradeList));
            
        } catch (Exception e) {
            logger.error("성적 목록 조회 실패", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(createErrorResponse("성적 목록 조회 중 오류가 발생했습니다."));
        }
    }

    /**
     * 최종 등급 배정 핸들러
     */
    private ResponseEntity<?> handleGradeFinalize(Map<String, Object> request) {
        Integer lecIdx = request.get("lecIdx") != null ? ((Number) request.get("lecIdx")).intValue() : null;
        String lecSerial = (String) request.get("lecSerial");
        Double passingThreshold = request.get("passingThreshold") != null ? 
            ((Number) request.get("passingThreshold")).doubleValue() : 60.0;
        
        // lecSerial이 제공된 경우 lecIdx로 변환
        if (lecIdx == null && lecSerial != null && !lecSerial.trim().isEmpty()) {
            lecIdx = enrollmentService.getLectureIdxBySerial(lecSerial);
            if (lecIdx == null) {
                return ResponseEntity.badRequest()
                    .body(createErrorResponse("존재하지 않는 강의 코드입니다: " + lecSerial));
            }
        }
        
        if (lecIdx == null) {
            return ResponseEntity.badRequest()
                .body(createErrorResponse("lecIdx 또는 lecSerial은 필수 파라미터입니다."));
        }

        try {
            Map<String, Object> result = enrollmentService.finalizeGrades(lecIdx, passingThreshold, request);
            return ResponseEntity.ok(createSuccessResponse("최종 등급 배정이 완료되었습니다.", result));
            
        } catch (Exception e) {
            logger.error("최종 등급 배정 실패", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(createErrorResponse("최종 등급 배정 중 오류가 발생했습니다."));
        }
    }
}
