package BlueCrab.com.example.controller;

import BlueCrab.com.example.dto.*;
import BlueCrab.com.example.repository.projection.DashboardStatsProjection;
import BlueCrab.com.example.service.AdminFacilityReservationService;
import BlueCrab.com.example.util.JwtUtil;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 관리자용 시설 예약 관리 API 컨트롤러
 */
@RestController
@RequestMapping("/api/admin/reservations")
public class AdminFacilityReservationController {

    private final AdminFacilityReservationService adminReservationService;
    private final JwtUtil jwtUtil;

    public AdminFacilityReservationController(AdminFacilityReservationService adminReservationService,
                                              JwtUtil jwtUtil) {
        this.adminReservationService = adminReservationService;
        this.jwtUtil = jwtUtil;
    }

    @PostMapping("/pending")
    public ResponseEntity<ApiResponse<List<ReservationDto>>> getPendingReservations(
            HttpServletRequest request) {
        String adminId = getAdminIdFromToken(request);
        List<ReservationDto> reservations = adminReservationService.getPendingReservations(adminId);
        return ResponseEntity.ok(ApiResponse.success("승인 대기 중인 예약 목록을 조회했습니다.", reservations));
    }

    @PostMapping("/pending/count")
    public ResponseEntity<ApiResponse<Long>> getPendingCount(
            HttpServletRequest request) {
        String adminId = getAdminIdFromToken(request);
        long count = adminReservationService.getPendingCount(adminId);
        return ResponseEntity.ok(ApiResponse.success("승인 대기 건수를 조회했습니다.", count));
    }

    @PostMapping("/approve")
    public ResponseEntity<ApiResponse<ReservationDto>> approveReservation(
            @Valid @RequestBody AdminApproveRequestDto approveRequest,
            HttpServletRequest request) {
        String adminId = getAdminIdFromToken(request);
        ReservationDto reservation = adminReservationService.approveReservation(adminId, approveRequest);
        return ResponseEntity.ok(ApiResponse.success("예약이 승인되었습니다.", reservation));
    }

    @PostMapping("/reject")
    public ResponseEntity<ApiResponse<ReservationDto>> rejectReservation(
            @Valid @RequestBody AdminRejectRequestDto rejectRequest,
            HttpServletRequest request) {
        String adminId = getAdminIdFromToken(request);
        ReservationDto reservation = adminReservationService.rejectReservation(adminId, rejectRequest);
        return ResponseEntity.ok(ApiResponse.success("예약이 반려되었습니다.", reservation));
    }

    @PostMapping("/stats")
    public ResponseEntity<ApiResponse<DashboardStatsProjection>> getReservationStats(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
            HttpServletRequest request) {
        String adminId = getAdminIdFromToken(request);
        DashboardStatsProjection stats = adminReservationService.getReservationStats(adminId, startDate, endDate);
        return ResponseEntity.ok(ApiResponse.success("통계 정보를 조회했습니다.", stats));
    }

    private String getAdminIdFromToken(HttpServletRequest request) {
        String token = jwtUtil.resolveToken(request);
        return jwtUtil.getAdminId(token);
    }
}
