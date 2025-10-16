package BlueCrab.com.example.controller;

import BlueCrab.com.example.dto.AdminApproveRequestDto;
import BlueCrab.com.example.dto.AdminRejectRequestDto;
import BlueCrab.com.example.dto.AdminReservationDetailDto;
import BlueCrab.com.example.dto.AdminReservationSearchRequestDto;
import BlueCrab.com.example.dto.ApiResponse;
import BlueCrab.com.example.dto.PageResponse;
import BlueCrab.com.example.dto.ReservationDto;
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

    @PostMapping("/all")
    public ResponseEntity<ApiResponse<List<ReservationDto>>> getAllReservations(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Integer facilityIdx,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
            HttpServletRequest request) {
        String adminId = getAdminIdFromToken(request);
        List<ReservationDto> reservations = adminReservationService.getAllReservations(
            adminId, status, facilityIdx, startDate, endDate);
        return ResponseEntity.ok(ApiResponse.success("전체 예약 목록을 조회했습니다.", reservations));
    }

    @PostMapping("/search")
    public ResponseEntity<ApiResponse<PageResponse<ReservationDto>>> searchReservations(
            @Valid @RequestBody AdminReservationSearchRequestDto searchRequest,
            HttpServletRequest request) {
        String adminId = getAdminIdFromToken(request);
        PageResponse<ReservationDto> result = adminReservationService.searchReservations(adminId, searchRequest);
        return ResponseEntity.ok(ApiResponse.success("예약 목록 조회 성공", result));
    }

    @PostMapping("/{reservationIdx}")
    public ResponseEntity<ApiResponse<AdminReservationDetailDto>> getReservationDetail(
            @PathVariable Integer reservationIdx,
            HttpServletRequest request) {
        String adminId = getAdminIdFromToken(request);
        AdminReservationDetailDto detail = adminReservationService.getReservationDetail(adminId, reservationIdx);
        return ResponseEntity.ok(ApiResponse.success("예약 상세 조회 성공", detail));
    }

    private String getAdminIdFromToken(HttpServletRequest request) {
        String token = jwtUtil.resolveToken(request);
        return jwtUtil.getAdminId(token);
    }
}
