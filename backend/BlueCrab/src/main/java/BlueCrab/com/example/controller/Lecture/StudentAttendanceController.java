package BlueCrab.com.example.controller.Lecture;

import BlueCrab.com.example.dto.ApiResponse;
import BlueCrab.com.example.dto.Lecture.AttendanceDto;
import BlueCrab.com.example.dto.Lecture.AttendanceRequestDto;
import BlueCrab.com.example.entity.Lecture.AttendanceRequestTbl;
import BlueCrab.com.example.entity.Lecture.EnrollmentExtendedTbl;
import BlueCrab.com.example.repository.Lecture.EnrollmentExtendedTblRepository;
import BlueCrab.com.example.service.Lecture.AttendanceService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 학생용 출석 관리 컨트롤러
 * 
 * 엔드포인트:
 * - POST /api/student/attendance/detail - 내 출석 조회
 * - POST /api/student/attendance/request - 출석 인정 신청
 * - POST /api/student/attendance/requests - 내 신청 목록
 */
@RestController
@RequestMapping("/api/student/attendance")
@Slf4j
public class StudentAttendanceController {

    @Autowired
    private AttendanceService attendanceService;

    @Autowired
    private EnrollmentExtendedTblRepository enrollmentRepository;

    @Autowired
    private ObjectMapper objectMapper;

    /**
     * 내 출석 조회 - POST 방식
     * 
     * POST /api/student/attendance/detail
     * Body: { "enrollmentIdx": 1 }
     * 
     * Response: {
     *   "attendanceStr": "1출2출3결4지...",
     *   "attendanceRate": "75/80",
     *   "details": [
     *     {"sessionNumber": 1, "status": "출"},
     *     {"sessionNumber": 2, "status": "출"},
     *     ...
     *   ]
     * }
     */
    @PostMapping("/detail")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getMyAttendance(
            @RequestBody Map<String, Object> request) {
        
        Integer enrollmentIdx = null;
        try {
            enrollmentIdx = request.get("enrollmentIdx") != null ? 
                    ((Number) request.get("enrollmentIdx")).intValue() : null;
            
            if (enrollmentIdx == null) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.failure("enrollmentIdx는 필수입니다."));
            }
            
            EnrollmentExtendedTbl enrollment = enrollmentRepository.findById(enrollmentIdx)
                    .orElseThrow(() -> new RuntimeException("수강신청을 찾을 수 없습니다."));

            String enrollmentDataStr = enrollment.getEnrollmentData();
            if (enrollmentDataStr == null || enrollmentDataStr.isEmpty()) {
                // 데이터가 없으면 빈 응답
                Map<String, Object> emptyResult = new HashMap<>();
                emptyResult.put("attendanceStr", "");
                emptyResult.put("attendanceRate", "0/80");
                emptyResult.put("details", Collections.emptyList());
                
                return ResponseEntity.ok(ApiResponse.success("출석 조회 성공", emptyResult));
            }

            // JSON 파싱
            JsonNode enrollmentData = objectMapper.readTree(enrollmentDataStr);
            String attendanceStr = enrollmentData.has("attendance") ? 
                    enrollmentData.get("attendance").asText() : "";
            String attendanceRate = enrollmentData.has("attendanceRate") ? 
                    enrollmentData.get("attendanceRate").asText() : "0/80";

            // 문자열 파싱해서 상세 리스트 생성
            Map<Integer, String> attendanceMap = attendanceService.parseAttendanceString(attendanceStr);
            List<AttendanceDto> details = attendanceMap.entrySet().stream()
                    .sorted(Map.Entry.comparingByKey())
                    .map(entry -> new AttendanceDto(entry.getKey(), entry.getValue()))
                    .collect(Collectors.toList());

            // 응답 구성
            Map<String, Object> result = new HashMap<>();
            result.put("attendanceStr", attendanceStr);
            result.put("attendanceRate", attendanceRate);
            result.put("details", details);

            return ResponseEntity.ok(ApiResponse.success("출석 조회 성공", result));

        } catch (Exception e) {
            log.error("출석 조회 실패: enrollmentIdx={}", enrollmentIdx, e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.failure("출석 조회 실패: " + e.getMessage()));
        }
    }

    /**
     * 출석 인정 신청
     * 
     * POST /api/student/attendance/request
     * Body: {
     *   "enrollmentIdx": 1,
     *   "sessionNumber": 3,
     *   "requestReason": "병원 진료로 인한 결석"
     * }
     */
    @PostMapping("/request")
    public ResponseEntity<ApiResponse<AttendanceRequestDto>> requestExcuse(
            @RequestBody Map<String, Object> payload) {
        
        try {
            Integer enrollmentIdx = (Integer) payload.get("enrollmentIdx");
            Integer sessionNumber = (Integer) payload.get("sessionNumber");
            String requestReason = (String) payload.get("requestReason");

            if (enrollmentIdx == null || sessionNumber == null || requestReason == null) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.failure("enrollmentIdx, sessionNumber, requestReason은 필수입니다."));
            }

            // 회차 범위 검증 (1~80)
            if (sessionNumber < 1 || sessionNumber > 80) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.failure("sessionNumber는 1~80 사이여야 합니다."));
            }

            // 요청 생성
            AttendanceRequestTbl request = attendanceService.requestExcuse(
                    enrollmentIdx, sessionNumber, requestReason);

            AttendanceRequestDto dto = new AttendanceRequestDto(request);

            return ResponseEntity.ok(ApiResponse.success("출석 인정 신청 완료", dto));

        } catch (Exception e) {
            log.error("출석 인정 신청 실패", e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.failure("신청 실패: " + e.getMessage()));
        }
    }

    /**
     * 내 출석 인정 신청 목록 조회 - POST 방식
     * 
     * POST /api/student/attendance/requests
     * Body: { "enrollmentIdx": 1 }
     */
    @PostMapping("/requests")
    public ResponseEntity<ApiResponse<List<AttendanceRequestDto>>> getMyRequests(
            @RequestBody Map<String, Object> request) {
        
        try {
            Integer enrollmentIdx = request.get("enrollmentIdx") != null ? 
                    ((Number) request.get("enrollmentIdx")).intValue() : null;
            
            if (enrollmentIdx == null) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.failure("enrollmentIdx는 필수입니다."));
            }
            
            List<AttendanceRequestTbl> requests = attendanceService.getStudentRequests(enrollmentIdx);

            // Entity → DTO 변환
            List<AttendanceRequestDto> dtoList = requests.stream()
                    .map(AttendanceRequestDto::new)
                    .collect(Collectors.toList());

            return ResponseEntity.ok(ApiResponse.success("출석 인정 신청 목록 조회 성공", dtoList));

        } catch (Exception e) {
            log.error("출석 인정 신청 목록 조회 실패", e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.failure("신청 목록 조회 실패: " + e.getMessage()));
        }
    }
}
