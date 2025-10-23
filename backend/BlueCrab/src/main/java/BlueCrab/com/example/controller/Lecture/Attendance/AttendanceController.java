package BlueCrab.com.example.controller.Lecture.Attendance;

import BlueCrab.com.example.dto.Lecture.Attendance.*;
import BlueCrab.com.example.entity.UserTbl;
import BlueCrab.com.example.repository.UserTblRepository;
import BlueCrab.com.example.service.Lecture.AttendanceRequestService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 출석 요청/승인 REST API 컨트롤러
 * 
 * 주요 기능:
 * - 학생: 출석 요청, 자신의 출석 현황 조회
 * - 교수: 출석 승인/반려, 전체 수강생 출석 현황 조회
 * 
 * 엔드포인트:
 * - POST /api/attendance/request (학생)
 * - POST /api/attendance/approve (교수)
 * - POST /api/attendance/student/view (학생)
 * - POST /api/attendance/professor/view (교수)
 * 
 * @author BlueCrab Development Team
 * @version 1.0.0
 * @since 2025-10-23
 */
@Slf4j
@RestController
@RequestMapping("/api/attendance")
public class AttendanceController {
    
    @Autowired
    private AttendanceRequestService attendanceService;
    
    @Autowired
    private UserTblRepository userRepository;
    
    /**
     * 출석 요청 (학생용)
     * 
     * 학생이 특정 회차에 대한 출석 요청을 제출합니다.
     * JWT 토큰에서 학생 정보를 추출하여 처리합니다.
     * 
     * @param request 출석 요청 데이터 (lecSerial, sessionNumber, requestReason)
     * @param authentication Spring Security 인증 정보
     * @return 출석 요청 처리 결과 및 현재 출석 데이터
     */
    @PostMapping("/request")
    public ResponseEntity<?> requestAttendance(
            @Valid @RequestBody AttendanceRequestRequestDto request,
            Authentication authentication) {
        try {
            // 인증 확인
            if (authentication == null || !authentication.isAuthenticated()) {
                log.warn("인증되지 않은 출석 요청 시도");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(createErrorResponse("인증이 필요합니다."));
            }
            
            // 사용자 정보 조회
            String userEmail = authentication.getName();
            UserTbl user = userRepository.findByUserEmail(userEmail)
                    .orElseThrow(() -> new RuntimeException("사용자 정보를 찾을 수 없습니다."));
            
            log.info("출석 요청: userIdx={}, lecSerial={}, sessionNumber={}", 
                     user.getUserIdx(), request.getLecSerial(), request.getSessionNumber());
            
            // Service 호출
            AttendanceResponseDto<AttendanceDataDto> response = attendanceService.requestAttendance(
                    request.getLecSerial(),
                    request.getSessionNumber(),
                    user.getUserIdx(),
                    request.getRequestReason()
            );
            
            if (!response.getSuccess()) {
                log.warn("출석 요청 실패: {}", response.getMessage());
                return ResponseEntity.badRequest().body(response);
            }
            
            log.info("출석 요청 성공: userIdx={}, lecSerial={}", user.getUserIdx(), request.getLecSerial());
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("출석 요청 처리 중 오류 발생", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("출석 요청 처리 중 오류가 발생했습니다."));
        }
    }
    
    /**
     * 출석 승인/반려 (교수용)
     * 
     * 교수가 학생들의 출석 요청을 승인하거나 반려합니다.
     * 여러 학생의 출석을 한 번에 처리할 수 있습니다.
     * 
     * @param request 출석 승인 데이터 (lecSerial, sessionNumber, approvalRecords)
     * @param authentication Spring Security 인증 정보
     * @return 출석 승인 처리 결과
     */
    @PostMapping("/approve")
    public ResponseEntity<?> approveAttendance(
            @Valid @RequestBody AttendanceApproveRequestDto request,
            Authentication authentication) {
        try {
            // 인증 확인
            if (authentication == null || !authentication.isAuthenticated()) {
                log.warn("인증되지 않은 출석 승인 시도");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(createErrorResponse("인증이 필요합니다."));
            }
            
            // 사용자 정보 조회
            String userEmail = authentication.getName();
            UserTbl user = userRepository.findByUserEmail(userEmail)
                    .orElseThrow(() -> new RuntimeException("사용자 정보를 찾을 수 없습니다."));
            
            // 교수 권한 확인
            if (user.getUserStudent() != 1) {
                log.warn("교수 권한 없음: userIdx={}, userStudent={}", user.getUserIdx(), user.getUserStudent());
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(createErrorResponse("교수 권한이 필요합니다."));
            }
            
            log.info("출석 승인 요청: professorIdx={}, lecSerial={}, sessionNumber={}, count={}", 
                     user.getUserIdx(), request.getLecSerial(), request.getSessionNumber(), 
                     request.getAttendanceRecords().size());
            
            // Service 호출
            AttendanceResponseDto<Void> response = attendanceService.approveAttendance(
                    request.getLecSerial(),
                    request.getSessionNumber(),
                    request.getAttendanceRecords(),
                    user.getUserIdx()
            );
            
            if (!response.getSuccess()) {
                log.warn("출석 승인 실패: {}", response.getMessage());
                return ResponseEntity.badRequest().body(response);
            }
            
            log.info("출석 승인 성공: professorIdx={}, lecSerial={}", user.getUserIdx(), request.getLecSerial());
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("출석 승인 처리 중 오류 발생", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("출석 승인 처리 중 오류가 발생했습니다."));
        }
    }
    
    /**
     * 학생 출석 현황 조회
     * 
     * 학생이 자신의 출석 현황을 조회합니다.
     * 출석 통계(summary), 출석 기록(sessions), 대기 중인 요청(pendingRequests)을 모두 포함합니다.
     * 
     * @param request lecSerial을 포함한 요청 데이터
     * @param authentication Spring Security 인증 정보
     * @return 학생의 출석 현황
     */
    @PostMapping("/student/view")
    public ResponseEntity<?> getStudentAttendance(
            @RequestBody Map<String, String> request,
            Authentication authentication) {
        try {
            // 인증 확인
            if (authentication == null || !authentication.isAuthenticated()) {
                log.warn("인증되지 않은 출석 조회 시도");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(createErrorResponse("인증이 필요합니다."));
            }
            
            // lecSerial 추출
            String lecSerial = request.get("lecSerial");
            if (lecSerial == null || lecSerial.isEmpty()) {
                log.warn("lecSerial 누락");
                return ResponseEntity.badRequest()
                        .body(createErrorResponse("강의 코드(lecSerial)는 필수입니다."));
            }
            
            // 사용자 정보 조회
            String userEmail = authentication.getName();
            UserTbl user = userRepository.findByUserEmail(userEmail)
                    .orElseThrow(() -> new RuntimeException("사용자 정보를 찾을 수 없습니다."));
            
            log.info("학생 출석 조회: userIdx={}, lecSerial={}", user.getUserIdx(), lecSerial);
            
            // Service 호출
            AttendanceResponseDto<AttendanceDataDto> response = attendanceService.getStudentAttendance(
                    lecSerial,
                    user.getUserIdx()
            );
            
            if (!response.getSuccess()) {
                log.warn("학생 출석 조회 실패: {}", response.getMessage());
                return ResponseEntity.badRequest().body(response);
            }
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("학생 출석 조회 중 오류 발생", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("출석 현황 조회 중 오류가 발생했습니다."));
        }
    }
    
    /**
     * 교수 출석 현황 조회 (전체 수강생)
     * 
     * 교수가 해당 강의의 전체 수강생 출석 현황을 조회합니다.
     * 각 학생의 출석 통계, 출석 기록, 대기 중인 요청을 모두 포함합니다.
     * 
     * @param request lecSerial을 포함한 요청 데이터
     * @param authentication Spring Security 인증 정보
     * @return 전체 수강생의 출석 현황 목록
     */
    @PostMapping("/professor/view")
    public ResponseEntity<?> getProfessorAttendance(
            @RequestBody Map<String, String> request,
            Authentication authentication) {
        try {
            // 인증 확인
            if (authentication == null || !authentication.isAuthenticated()) {
                log.warn("인증되지 않은 교수 출석 조회 시도");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(createErrorResponse("인증이 필요합니다."));
            }
            
            // lecSerial 추출
            String lecSerial = request.get("lecSerial");
            if (lecSerial == null || lecSerial.isEmpty()) {
                log.warn("lecSerial 누락");
                return ResponseEntity.badRequest()
                        .body(createErrorResponse("강의 코드(lecSerial)는 필수입니다."));
            }
            
            // 사용자 정보 조회
            String userEmail = authentication.getName();
            UserTbl user = userRepository.findByUserEmail(userEmail)
                    .orElseThrow(() -> new RuntimeException("사용자 정보를 찾을 수 없습니다."));
            
            // 교수 권한 확인
            if (user.getUserStudent() != 1) {
                log.warn("교수 권한 없음: userIdx={}, userStudent={}", user.getUserIdx(), user.getUserStudent());
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(createErrorResponse("교수 권한이 필요합니다."));
            }
            
            log.info("교수 출석 조회: professorIdx={}, lecSerial={}", user.getUserIdx(), lecSerial);
            
            // Service 호출
            AttendanceResponseDto<List<StudentAttendanceDto>> response = 
                    attendanceService.getProfessorAttendance(lecSerial, user.getUserIdx());
            
            if (!response.getSuccess()) {
                log.warn("교수 출석 조회 실패: {}", response.getMessage());
                return ResponseEntity.badRequest().body(response);
            }
            
            log.info("교수 출석 조회 성공: professorIdx={}, 수강생 수={}", 
                     user.getUserIdx(), response.getData().size());
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("교수 출석 조회 중 오류 발생", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("출석 현황 조회 중 오류가 발생했습니다."));
        }
    }
    
    /**
     * 에러 응답 생성 유틸리티 메서드
     * 
     * @param message 에러 메시지
     * @return 에러 응답 Map
     */
    private Map<String, Object> createErrorResponse(String message) {
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("success", false);
        errorResponse.put("message", message);
        errorResponse.put("data", null);
        return errorResponse;
    }
}
