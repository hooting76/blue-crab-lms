package BlueCrab.com.example.controller;

import BlueCrab.com.example.dto.ApiResponse;
import BlueCrab.com.example.dto.ReservationCreateRequestDto;
import BlueCrab.com.example.dto.ReservationDto;
import BlueCrab.com.example.enums.ReservationStatus;
import BlueCrab.com.example.service.FacilityReservationService;
import BlueCrab.com.example.util.JwtUtil;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import java.util.List;

/**
 * 시설 예약 관리 API 컨트롤러
 */
@RestController
@RequestMapping("/api/reservations")
public class FacilityReservationController {

    private final FacilityReservationService reservationService;
    private final JwtUtil jwtUtil;

    public FacilityReservationController(FacilityReservationService reservationService,
                                         JwtUtil jwtUtil) {
        this.reservationService = reservationService;
        this.jwtUtil = jwtUtil;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<ReservationDto>> createReservation(
            @Valid @RequestBody ReservationCreateRequestDto request,
            HttpServletRequest httpRequest) {
        String userCode = getUserCodeFromToken(httpRequest);
        ReservationDto reservation = reservationService.createReservation(userCode, request);

        // 예약 상태에 따라 응답 메시지 결정
        String message = reservation.getStatus().name().equals("APPROVED")
            ? "예약이 자동으로 승인되었습니다."
            : "예약이 생성되었습니다. 관리자 승인 대기 중입니다.";

        return ResponseEntity.ok(ApiResponse.success(message, reservation));
    }

    @PostMapping("/my")
    public ResponseEntity<ApiResponse<List<ReservationDto>>> getMyReservations(
            HttpServletRequest httpRequest) {
        String userCode = getUserCodeFromToken(httpRequest);
        List<ReservationDto> reservations = reservationService.getMyReservations(userCode);
        return ResponseEntity.ok(ApiResponse.success("내 예약 목록을 조회했습니다.", reservations));
    }

    @PostMapping("/my/status/{status}")
    public ResponseEntity<ApiResponse<List<ReservationDto>>> getMyReservationsByStatus(
            @PathVariable String status,
            HttpServletRequest httpRequest) {
        String userCode = getUserCodeFromToken(httpRequest);
        ReservationStatus reservationStatus = ReservationStatus.fromString(status);
        List<ReservationDto> reservations = reservationService.getMyReservationsByStatus(
            userCode, reservationStatus);
        return ResponseEntity.ok(ApiResponse.success("예약 목록을 조회했습니다.", reservations));
    }

    @PostMapping("/{reservationIdx}")
    public ResponseEntity<ApiResponse<ReservationDto>> getReservationById(
            @PathVariable Integer reservationIdx,
            HttpServletRequest httpRequest) {
        String userCode = getUserCodeFromToken(httpRequest);
        ReservationDto reservation = reservationService.getReservationById(reservationIdx, userCode);
        return ResponseEntity.ok(ApiResponse.success("예약 정보를 조회했습니다.", reservation));
    }

    @DeleteMapping("/{reservationIdx}")
    public ResponseEntity<ApiResponse<Void>> cancelReservation(
            @PathVariable Integer reservationIdx,
            HttpServletRequest httpRequest) {
        String userCode = getUserCodeFromToken(httpRequest);
        reservationService.cancelReservation(reservationIdx, userCode);
        return ResponseEntity.ok(ApiResponse.success("예약이 취소되었습니다."));
    }

    private String getUserCodeFromToken(HttpServletRequest request) {
        String token = jwtUtil.resolveToken(request);
        return jwtUtil.getUserCode(token);
    }
}
