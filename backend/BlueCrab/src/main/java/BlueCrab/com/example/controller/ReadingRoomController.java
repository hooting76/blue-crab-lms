package BlueCrab.com.example.controller;

import BlueCrab.com.example.annotation.RateLimit;
import BlueCrab.com.example.dto.*;
import BlueCrab.com.example.entity.UserTbl;
import BlueCrab.com.example.repository.UserTblRepository;
import BlueCrab.com.example.service.ReadingRoomService;
import BlueCrab.com.example.util.JwtUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import java.util.Optional;

/**
 * 열람실 좌석 관리를 위한 REST API 컨트롤러
 * JWT 기반 인증을 통한 좌석 예약, 퇴실, 현황 조회 기능을 제공
 *
 * 모든 API는 JWT 토큰 인증이 필요하며, 응답은 ApiResponse<T> 형식으로 통일됩니다:
 * - success: 요청 성공/실패 여부
 * - message: 사용자에게 표시할 메시지 (한국어)
 * - data: 실제 응답 데이터
 * - errorCode: 실패 시 에러 코드 (선택적)
 * - timestamp: 응답 생성 시간
 *
 * 비즈니스 룰:
 * - 1인 1좌석 제한
 * - JWT 토큰에서 사용자 식별
 * - 본인 좌석만 퇴실 가능
 * - 2시간 기본 사용시간
 *
 * @author BlueCrab Development Team
 * @version 1.0.0
 * @since 2025-09-29
 */
@RestController
@RequestMapping("/api/reading-room")
public class ReadingRoomController {

    private static final Logger logger = LoggerFactory.getLogger(ReadingRoomController.class);

    @Autowired
    private ReadingRoomService readingRoomService;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private UserTblRepository userTblRepository;

    /**
     * 열람실 좌석 현황을 조회합니다.
     * 실시간 좌석 상태와 통계 정보를 제공합니다.
     *
     * @param request HTTP 요청 (JWT 토큰 검증용)
     * @return 열람실 전체 현황 정보
     *
     * 응답 예시:
     * {
     *   "success": true,
     *   "message": "열람실 현황 조회가 완료되었습니다.",
     *   "data": {
     *     "seats": [
     *       {"seatNumber": 1, "isOccupied": false, "endTime": null},
     *       {"seatNumber": 2, "isOccupied": true, "endTime": "2025-09-29T16:30:00"}
     *     ],
     *     "totalSeats": 80,
     *     "availableSeats": 45,
     *     "occupiedSeats": 35,
     *     "occupancyRate": 43.75
     *   }
     * }
     */
    @RateLimit(timeWindow = 10, maxRequests = 5, message = "현황 조회가 너무 빈번합니다. 10초 후 다시 시도해주세요.")
    @PostMapping("/status")
    public ResponseEntity<ApiResponse<ReadingRoomStatusDto>> getStatus(HttpServletRequest request) {
        try {
            // JWT 토큰 검증 (사용자 인증 확인)
            String token = extractTokenFromRequest(request);
            if (!jwtUtil.validateToken(token)) {
                return ResponseEntity.status(401)
                        .body(new ApiResponse<>(false, "인증이 필요합니다.", null, "UNAUTHORIZED"));
            }

            logger.info("열람실 현황 조회 요청");

            ReadingRoomStatusDto status = readingRoomService.getReadingRoomStatus();

            logger.info("열람실 현황 조회 완료 - 전체: {}, 사용중: {}, 사용가능: {}",
                    status.getTotalSeats(), status.getOccupiedSeats(), status.getAvailableSeats());

            return ResponseEntity.ok(new ApiResponse<>(true, "열람실 현황 조회가 완료되었습니다.", status));

        } catch (Exception e) {
            logger.error("열람실 현황 조회 중 오류 발생", e);
            return ResponseEntity.status(500)
                    .body(new ApiResponse<>(false, "현황 조회 중 오류가 발생했습니다.", null, "INTERNAL_ERROR"));
        }
    }

    /**
     * 좌석을 예약합니다.
     * JWT 토큰에서 사용자 정보를 추출하여 예약을 처리합니다.
     *
     * @param request HTTP 요청 (JWT 토큰)
     * @param reserveRequest 좌석 예약 요청 데이터
     * @return 예약 결과 정보
     *
     * 응답 예시 (성공):
     * {
     *   "success": true,
     *   "message": "좌석 예약이 완료되었습니다.",
     *   "data": {
     *     "seatNumber": 15,
     *     "startTime": "2025-09-29T14:30:00",
     *     "endTime": "2025-09-29T16:30:00",
     *     "usageTimeMinutes": 120
     *   }
     * }
     *
     * 응답 예시 (실패):
     * {
     *   "success": false,
     *   "message": "해당 좌석은 이미 다른 사용자가 예약했습니다.",
     *   "errorCode": "SEAT_ALREADY_OCCUPIED"
     * }
     */
    @RateLimit(timeWindow = 60, maxRequests = 3, message = "좌석 예약 요청이 너무 빈번합니다. 1분 후 다시 시도해주세요.")
    @PostMapping("/reserve")
    public ResponseEntity<ApiResponse<SeatReserveResponseDto>> reserveSeat(
            HttpServletRequest request,
            @Valid @RequestBody SeatReserveRequestDto reserveRequest) {

        try {
            // JWT 토큰 검증 및 사용자 정보 추출
            String token = extractTokenFromRequest(request);
            if (!jwtUtil.validateToken(token)) {
                return ResponseEntity.status(401)
                        .body(new ApiResponse<>(false, "인증이 필요합니다.", null, "UNAUTHORIZED"));
            }

            String userCode = getUserCodeFromToken(token);
            Integer seatNumber = reserveRequest.getSeatNumber();

            logger.info("좌석 예약 요청 - 사용자: {}, 좌석: {}", userCode, seatNumber);

            // 좌석 번호 유효성 검증
            if (seatNumber == null || seatNumber < 1 || seatNumber > 80) {
                return ResponseEntity.badRequest()
                        .body(new ApiResponse<>(false, "올바른 좌석 번호를 입력해주세요. (1~80)", null, "INVALID_SEAT_NUMBER"));
            }

            SeatReserveResponseDto response = readingRoomService.reserveSeat(seatNumber, userCode);

            logger.info("좌석 예약 완료 - 사용자: {}, 좌석: {}, 종료시간: {}",
                    userCode, seatNumber, response.getEndTime());

            return ResponseEntity.ok(new ApiResponse<>(true, "좌석 예약이 완료되었습니다.", response));

        } catch (IllegalStateException e) {
            logger.warn("좌석 예약 실패 - {}", e.getMessage());
            String errorCode = e.getMessage().contains("이미 다른 좌석") ? "USER_ALREADY_RESERVED" : "SEAT_ALREADY_OCCUPIED";
            return ResponseEntity.badRequest()
                    .body(new ApiResponse<>(false, e.getMessage(), null, errorCode));

        } catch (IllegalArgumentException e) {
            logger.warn("잘못된 요청 - {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(new ApiResponse<>(false, e.getMessage(), null, "INVALID_REQUEST"));

        } catch (Exception e) {
            logger.error("좌석 예약 중 오류 발생", e);
            return ResponseEntity.status(500)
                    .body(new ApiResponse<>(false, "좌석 예약 중 오류가 발생했습니다.", null, "INTERNAL_ERROR"));
        }
    }

    /**
     * 좌석에서 퇴실합니다.
     * JWT 토큰에서 사용자 정보를 추출하여 본인 좌석인지 검증 후 퇴실을 처리합니다.
     *
     * @param request HTTP 요청 (JWT 토큰)
     * @param checkoutRequest 퇴실 요청 데이터
     * @return 퇴실 결과 정보
     *
     * 응답 예시 (성공):
     * {
     *   "success": true,
     *   "message": "퇴실 처리가 완료되었습니다.",
     *   "data": {
     *     "seatNumber": 15,
     *     "usageTime": 87
     *   }
     * }
     *
     * 응답 예시 (실패):
     * {
     *   "success": false,
     *   "message": "본인이 예약한 좌석이 아닙니다.",
     *   "errorCode": "UNAUTHORIZED_SEAT"
     * }
     */
    @RateLimit(timeWindow = 30, maxRequests = 2, message = "퇴실 요청이 너무 빈번합니다. 30초 후 다시 시도해주세요.")
    @PostMapping("/checkout")
    public ResponseEntity<ApiResponse<SeatCheckoutResponseDto>> checkoutSeat(
            HttpServletRequest request,
            @Valid @RequestBody SeatCheckoutRequestDto checkoutRequest) {

        try {
            // JWT 토큰 검증 및 사용자 정보 추출
            String token = extractTokenFromRequest(request);
            if (!jwtUtil.validateToken(token)) {
                return ResponseEntity.status(401)
                        .body(new ApiResponse<>(false, "인증이 필요합니다.", null, "UNAUTHORIZED"));
            }

            String userCode = getUserCodeFromToken(token);
            Integer seatNumber = checkoutRequest.getSeatNumber();

            logger.info("퇴실 요청 - 사용자: {}, 좌석: {}", userCode, seatNumber);

            // 좌석 번호 유효성 검증
            if (seatNumber == null || seatNumber < 1 || seatNumber > 80) {
                return ResponseEntity.badRequest()
                        .body(new ApiResponse<>(false, "올바른 좌석 번호를 입력해주세요. (1~80)", null, "INVALID_SEAT_NUMBER"));
            }

            SeatCheckoutResponseDto response = readingRoomService.checkoutSeat(seatNumber, userCode);

            logger.info("퇴실 완료 - 사용자: {}, 좌석: {}, 사용시간: {}분",
                    userCode, seatNumber, response.getUsageTime());

            return ResponseEntity.ok(new ApiResponse<>(true, "퇴실 처리가 완료되었습니다.", response));

        } catch (IllegalStateException e) {
            logger.warn("퇴실 실패 - {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(new ApiResponse<>(false, e.getMessage(), null, "UNAUTHORIZED_SEAT"));

        } catch (IllegalArgumentException e) {
            logger.warn("잘못된 요청 - {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(new ApiResponse<>(false, e.getMessage(), null, "INVALID_REQUEST"));

        } catch (Exception e) {
            logger.error("퇴실 처리 중 오류 발생", e);
            return ResponseEntity.status(500)
                    .body(new ApiResponse<>(false, "퇴실 처리 중 오류가 발생했습니다.", null, "INTERNAL_ERROR"));
        }
    }

    /**
     * 현재 사용자의 예약 좌석을 조회합니다.
     *
     * @param request HTTP 요청 (JWT 토큰)
     * @return 현재 예약 좌석 정보
     */
    @RateLimit(timeWindow = 5, maxRequests = 10, message = "예약 조회가 너무 빈번합니다. 5초 후 다시 시도해주세요.")
    @PostMapping("/my-reservation")
    public ResponseEntity<ApiResponse<ReadingSeatDto>> getMyReservation(HttpServletRequest request) {
        try {
            // JWT 토큰 검증 및 사용자 정보 추출
            String token = extractTokenFromRequest(request);
            if (!jwtUtil.validateToken(token)) {
                return ResponseEntity.status(401)
                        .body(new ApiResponse<>(false, "인증이 필요합니다.", null, "UNAUTHORIZED"));
            }

            String userCode = getUserCodeFromToken(token);

            logger.info("내 예약 조회 요청 - 사용자: {}", userCode);

            ReadingSeatDto myReservation = readingRoomService.getCurrentReservation(userCode);

            if (myReservation != null) {
                logger.info("예약 좌석 조회 완료 - 사용자: {}, 좌석: {}", userCode, myReservation.getSeatNumber());
                return ResponseEntity.ok(new ApiResponse<>(true, "예약 좌석 정보를 조회했습니다.", myReservation));
            } else {
                return ResponseEntity.ok(new ApiResponse<>(true, "현재 예약된 좌석이 없습니다.", null));
            }

        } catch (Exception e) {
            logger.error("내 예약 조회 중 오류 발생", e);
            return ResponseEntity.status(500)
                    .body(new ApiResponse<>(false, "예약 조회 중 오류가 발생했습니다.", null, "INTERNAL_ERROR"));
        }
    }

    /**
     * HTTP 요청에서 JWT 토큰을 추출합니다.
     *
     * @param request HTTP 요청
     * @return JWT 토큰 문자열, 토큰이 없으면 null
     */
    private String extractTokenFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }

    /**
     * JWT 토큰에서 사용자 코드(학번/교번)를 추출합니다.
     * 기존 JWT 토큰에는 userId만 있으므로, DB에서 userCode를 조회합니다.
     *
     * @param token JWT 토큰
     * @return 사용자 코드 (학번/교번)
     * @throws RuntimeException 사용자가 존재하지 않는 경우
     */
    private String getUserCodeFromToken(String token) {
        // JWT에서 userId 추출
        Integer userId = jwtUtil.extractUserId(token);
        if (userId == null) {
            throw new RuntimeException("토큰에서 사용자 정보를 찾을 수 없습니다.");
        }

        // DB에서 userCode 조회 (userIdx는 기본키이므로 findById 사용)
        Optional<UserTbl> userOpt = userTblRepository.findById(userId);
        if (!userOpt.isPresent()) {
            throw new RuntimeException("존재하지 않는 사용자입니다.");
        }

        return userOpt.get().getUserCode();
    }
}