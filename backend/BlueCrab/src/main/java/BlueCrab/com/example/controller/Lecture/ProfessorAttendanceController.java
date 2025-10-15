package BlueCrab.com.example.controller.Lecture;

import BlueCrab.com.example.dto.ApiResponse;
import BlueCrab.com.example.dto.Lecture.AttendanceDto;
import BlueCrab.com.example.dto.Lecture.AttendanceRequestDto;
import BlueCrab.com.example.entity.Lecture.AttendanceRequestTbl;
import BlueCrab.com.example.service.Lecture.AttendanceService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 교수용 출석 관리 컨트롤러
 * 
 * 엔드포인트:
 * - GET /api/professor/attendance/requests?lecIdx={id} - 출석 인정 요청 목록
 * - PUT /api/professor/attendance/requests/{id}/approve - 요청 승인
 * - PUT /api/professor/attendance/requests/{id}/reject - 요청 반려
 * - POST /api/professor/attendance/mark - 출석 체크
 */
@RestController
@RequestMapping("/api/professor/attendance")
@Slf4j
public class ProfessorAttendanceController {

    @Autowired
    private AttendanceService attendanceService;

    /**
     * 출석 인정 요청 목록 조회
     * 
     * GET /api/professor/attendance/requests?lecIdx=1&page=0&size=20&status=PENDING
     */
    @GetMapping("/requests")
    public ResponseEntity<ApiResponse<Page<AttendanceRequestDto>>> getRequests(
            @RequestParam Integer lecIdx,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String status) {
        
        try {
            Pageable pageable = PageRequest.of(page, size);
            Page<AttendanceRequestTbl> requestPage;

            // status 파라미터에 따라 필터링
            if ("PENDING".equals(status)) {
                requestPage = attendanceService.getPendingRequestsByLecture(lecIdx, pageable);
            } else {
                requestPage = attendanceService.getAllRequestsByLecture(lecIdx, pageable);
            }

            // Entity → DTO 변환
            Page<AttendanceRequestDto> dtoPage = requestPage.map(AttendanceRequestDto::new);

            return ResponseEntity.ok(ApiResponse.success("출석 인정 요청 목록 조회 성공", dtoPage));

        } catch (Exception e) {
            log.error("출석 인정 요청 목록 조회 실패: lecIdx={}", lecIdx, e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.failure("출석 인정 요청 목록 조회 실패: " + e.getMessage()));
        }
    }

    /**
     * 대기 중인 요청 개수 조회
     * 
     * GET /api/professor/attendance/requests/count?lecIdx=1
     */
    @GetMapping("/requests/count")
    public ResponseEntity<ApiResponse<Map<String, Long>>> getPendingCount(
            @RequestParam Integer lecIdx) {
        
        try {
            long count = attendanceService.countPendingRequests(lecIdx);
            
            Map<String, Long> result = new HashMap<>();
            result.put("pendingCount", count);

            return ResponseEntity.ok(ApiResponse.success("대기 중인 요청 개수 조회 성공", result));

        } catch (Exception e) {
            log.error("대기 중인 요청 개수 조회 실패: lecIdx={}", lecIdx, e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.failure("요청 개수 조회 실패: " + e.getMessage()));
        }
    }

    /**
     * 출석 인정 요청 승인 (결 → 출)
     * 
     * PUT /api/professor/attendance/requests/{requestIdx}/approve
     * Body: { "professorIdx": 1 }
     */
    @PutMapping("/requests/{requestIdx}/approve")
    public ResponseEntity<ApiResponse<AttendanceRequestDto>> approveRequest(
            @PathVariable Long requestIdx,
            @RequestBody Map<String, Integer> payload) {
        
        try {
            Integer professorIdx = payload.get("professorIdx");
            if (professorIdx == null) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.failure("professorIdx는 필수입니다."));
            }

            // 승인 처리 (출석 문자열도 함께 업데이트됨)
            attendanceService.approveRequest(requestIdx, professorIdx);

            // 승인된 요청 정보 반환
            AttendanceRequestTbl approved = attendanceService.getStudentRequests(null)
                    .stream()
                    .filter(r -> r.getRequestIdx().equals(requestIdx))
                    .findFirst()
                    .orElseThrow(() -> new RuntimeException("요청을 찾을 수 없습니다."));

            AttendanceRequestDto dto = new AttendanceRequestDto(approved);

            return ResponseEntity.ok(ApiResponse.success("출석 인정 승인 완료", dto));

        } catch (Exception e) {
            log.error("출석 인정 승인 실패: requestIdx={}", requestIdx, e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.failure("승인 실패: " + e.getMessage()));
        }
    }

    /**
     * 출석 인정 요청 반려
     * 
     * PUT /api/professor/attendance/requests/{requestIdx}/reject
     * Body: { "professorIdx": 1, "rejectReason": "사유 불충분" }
     */
    @PutMapping("/requests/{requestIdx}/reject")
    public ResponseEntity<ApiResponse<AttendanceRequestDto>> rejectRequest(
            @PathVariable Long requestIdx,
            @RequestBody Map<String, Object> payload) {
        
        try {
            Integer professorIdx = (Integer) payload.get("professorIdx");
            String rejectReason = (String) payload.get("rejectReason");

            if (professorIdx == null || rejectReason == null) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.failure("professorIdx와 rejectReason은 필수입니다."));
            }

            // 반려 처리
            AttendanceRequestTbl rejected = attendanceService.rejectRequest(
                    requestIdx, professorIdx, rejectReason);

            AttendanceRequestDto dto = new AttendanceRequestDto(rejected);

            return ResponseEntity.ok(ApiResponse.success("출석 인정 요청 반려 완료", dto));

        } catch (Exception e) {
            log.error("출석 인정 요청 반려 실패: requestIdx={}", requestIdx, e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.failure("반려 실패: " + e.getMessage()));
        }
    }

    /**
     * 출석 체크 (교수가 출결 기록)
     * 
     * POST /api/professor/attendance/mark
     * Body: { "enrollmentIdx": 1, "sessionNumber": 3, "status": "출" }
     */
    @PostMapping("/mark")
    public ResponseEntity<ApiResponse<AttendanceDto>> markAttendance(
            @RequestBody Map<String, Object> payload) {
        
        try {
            Integer enrollmentIdx = (Integer) payload.get("enrollmentIdx");
            Integer sessionNumber = (Integer) payload.get("sessionNumber");
            String status = (String) payload.get("status");

            if (enrollmentIdx == null || sessionNumber == null || status == null) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.failure("enrollmentIdx, sessionNumber, status는 필수입니다."));
            }

            // 상태 값 검증 (출/결/지만 허용)
            if (!status.equals("출") && !status.equals("결") && !status.equals("지")) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.failure("status는 '출', '결', '지' 중 하나여야 합니다."));
            }

            // 출석 기록
            attendanceService.markAttendance(enrollmentIdx, sessionNumber, status);

            // DTO 생성
            AttendanceDto dto = new AttendanceDto(sessionNumber, status);

            return ResponseEntity.ok(ApiResponse.success("출석 체크 완료", dto));

        } catch (Exception e) {
            log.error("출석 체크 실패", e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.failure("출석 체크 실패: " + e.getMessage()));
        }
    }
}
