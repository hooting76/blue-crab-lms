package BlueCrab.com.example.controller;

import BlueCrab.com.example.dto.Consultation.*;
import BlueCrab.com.example.entity.UserTbl;
import BlueCrab.com.example.repository.UserTblRepository;
import BlueCrab.com.example.service.ConsultationRequestService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.HashMap;
import java.util.Map;

/**
 * 상담 요청/진행 관리 REST API 컨트롤러
 *
 * 주요 기능:
 * - 학생: 상담 요청, 취소, 내 요청 목록 조회
 * - 교수: 요청 승인/반려, 받은 요청 목록 조회, 메모 작성
 * - 공통: 상담 시작/종료, 진행 중인 상담 조회, 읽음 처리
 *
 * 엔드포인트:
 * - POST /api/consultation/request (학생)
 * - POST /api/consultation/approve (교수)
 * - POST /api/consultation/reject (교수)
 * - POST /api/consultation/cancel (학생)
 * - POST /api/consultation/start (학생/교수)
 * - POST /api/consultation/end (학생/교수)
 * - POST /api/consultation/memo (교수)
 * - POST /api/consultation/my-requests (학생)
 * - POST /api/consultation/received (교수)
 * - POST /api/consultation/active (학생/교수)
 * - POST /api/consultation/history (학생/교수)
 * - GET  /api/consultation/{id} (학생/교수)
 * - GET  /api/consultation/unread-count (교수)
 * - POST /api/consultation/read (학생/교수)
 *
 * @author BlueCrab Development Team
 * @version 1.0.0
 * @since 2025-10-24
 */
@Slf4j
@RestController
@RequestMapping("/api/consultation")
public class ConsultationController {

    @Autowired
    private ConsultationRequestService consultationService;

    @Autowired
    private UserTblRepository userRepository;

    /**
     * 상담 요청 생성 (학생용)
     *
     * @param createDto 상담 요청 정보
     * @param authentication Spring Security 인증 정보
     * @return 생성된 상담 요청 정보
     */
    @PostMapping("/request")
    public ResponseEntity<?> createRequest(
            @Valid @RequestBody ConsultationRequestCreateDto createDto,
            Authentication authentication) {
        try {
            // 인증 확인
            if (authentication == null || !authentication.isAuthenticated()) {
                log.warn("인증되지 않은 상담 요청 시도");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(createErrorResponse("인증이 필요합니다."));
            }

            // 사용자 정보 조회
            String userEmail = authentication.getName();
            UserTbl user = userRepository.findByUserEmail(userEmail)
                    .orElseThrow(() -> new RuntimeException("사용자 정보를 찾을 수 없습니다."));

            // JWT에서 추출한 사용자 정보를 DTO에 설정
            createDto.setRequesterUserCode(user.getUserCode());

            log.info("상담 요청 생성: requester={}, recipient={}",
                     createDto.getRequesterUserCode(), createDto.getRecipientUserCode());

            // Service 호출
            ConsultationRequestDto result = consultationService.createRequest(createDto);

            log.info("상담 요청 생성 완료: requestIdx={}", result.getRequestIdx());
            return ResponseEntity.ok(result);

        } catch (Exception e) {
            log.error("상담 요청 생성 실패", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("상담 요청 생성에 실패했습니다: " + e.getMessage()));
        }
    }

    /**
     * 상담 요청 승인 (교수용)
     *
     * @param approveDto 승인 정보
     * @param authentication Spring Security 인증 정보
     * @return 승인된 상담 요청 정보
     */
    @PostMapping("/approve")
    public ResponseEntity<?> approveRequest(
            @Valid @RequestBody ConsultationApproveDto approveDto,
            Authentication authentication) {
        try {
            // 인증 및 권한 확인
            UserTbl user = validateProfessorAuth(authentication);

            log.info("상담 요청 승인: requestIdx={}, professor={}",
                     approveDto.getRequestIdx(), user.getUserCode());

            // Service 호출
            ConsultationRequestDto result = consultationService.approveRequest(approveDto);

            log.info("상담 요청 승인 완료: requestIdx={}", result.getRequestIdx());
            return ResponseEntity.ok(result);

        } catch (SecurityException e) {
            log.warn("권한 없음: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(createErrorResponse(e.getMessage()));
        } catch (Exception e) {
            log.error("상담 요청 승인 실패", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("상담 요청 승인에 실패했습니다: " + e.getMessage()));
        }
    }

    /**
     * 상담 요청 반려 (교수용)
     *
     * @param rejectDto 반려 정보
     * @param authentication Spring Security 인증 정보
     * @return 반려된 상담 요청 정보
     */
    @PostMapping("/reject")
    public ResponseEntity<?> rejectRequest(
            @Valid @RequestBody ConsultationRejectDto rejectDto,
            Authentication authentication) {
        try {
            // 인증 및 권한 확인
            UserTbl user = validateProfessorAuth(authentication);

            log.info("상담 요청 반려: requestIdx={}, professor={}",
                     rejectDto.getRequestIdx(), user.getUserCode());

            // Service 호출
            ConsultationRequestDto result = consultationService.rejectRequest(rejectDto);

            log.info("상담 요청 반려 완료: requestIdx={}", result.getRequestIdx());
            return ResponseEntity.ok(result);

        } catch (SecurityException e) {
            log.warn("권한 없음: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(createErrorResponse(e.getMessage()));
        } catch (Exception e) {
            log.error("상담 요청 반려 실패", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("상담 요청 반려에 실패했습니다: " + e.getMessage()));
        }
    }

    /**
     * 상담 요청 취소 (학생용)
     *
     * @param cancelDto 취소 정보
     * @param authentication Spring Security 인증 정보
     * @return 취소된 상담 요청 정보
     */
    @PostMapping("/cancel")
    public ResponseEntity<?> cancelRequest(
            @Valid @RequestBody ConsultationCancelDto cancelDto,
            Authentication authentication) {
        try {
            // 인증 확인
            if (authentication == null || !authentication.isAuthenticated()) {
                log.warn("인증되지 않은 상담 취소 시도");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(createErrorResponse("인증이 필요합니다."));
            }

            String userEmail = authentication.getName();
            UserTbl user = userRepository.findByUserEmail(userEmail)
                    .orElseThrow(() -> new RuntimeException("사용자 정보를 찾을 수 없습니다."));

            log.info("상담 요청 취소: requestIdx={}, student={}",
                     cancelDto.getRequestIdx(), user.getUserCode());

            // Service 호출
            ConsultationRequestDto result = consultationService.cancelRequest(cancelDto);

            log.info("상담 요청 취소 완료: requestIdx={}", result.getRequestIdx());
            return ResponseEntity.ok(result);

        } catch (Exception e) {
            log.error("상담 요청 취소 실패", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("상담 요청 취소에 실패했습니다: " + e.getMessage()));
        }
    }

    /**
     * 상담 시작 (학생/교수)
     *
     * @param idDto 상담 ID
     * @param authentication Spring Security 인증 정보
     * @return 시작된 상담 정보
     */
    @PostMapping("/start")
    public ResponseEntity<?> startConsultation(
            @Valid @RequestBody ConsultationIdDto idDto,
            Authentication authentication) {
        try {
            // 인증 확인
            UserTbl user = validateAuth(authentication);

            log.info("상담 시작: requestIdx={}, userCode={}",
                     idDto.getRequestIdx(), user.getUserCode());

            // Service 호출
            ConsultationRequestDto result = consultationService.startConsultation(idDto);

            log.info("상담 시작 완료: requestIdx={}", result.getRequestIdx());
            return ResponseEntity.ok(result);

        } catch (IllegalStateException e) {
            log.warn("상담 시작 실패 (상태 위반): requestIdx={}, reason={}",
                     idDto.getRequestIdx(), e.getMessage());
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(createErrorResponse(e.getMessage()));
        } catch (Exception e) {
            log.error("상담 시작 실패", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("상담 시작에 실패했습니다: " + e.getMessage()));
        }
    }

    /**
     * 상담 종료 (학생/교수)
     *
     * @param idDto 상담 ID
     * @param authentication Spring Security 인증 정보
     * @return 종료된 상담 정보
     */
    @PostMapping("/end")
    public ResponseEntity<?> endConsultation(
            @Valid @RequestBody ConsultationIdDto idDto,
            Authentication authentication) {
        try {
            // 인증 확인
            UserTbl user = validateAuth(authentication);

            log.info("상담 종료: requestIdx={}, userCode={}",
                     idDto.getRequestIdx(), user.getUserCode());

            // Service 호출
            ConsultationRequestDto result = consultationService.endConsultation(idDto);

            log.info("상담 종료 완료: requestIdx={}, duration={}분",
                     result.getRequestIdx(), result.getDurationMinutes());
            return ResponseEntity.ok(result);

        } catch (IllegalStateException e) {
            log.warn("상담 종료 실패 (상태 위반): requestIdx={}, reason={}",
                     idDto.getRequestIdx(), e.getMessage());
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(createErrorResponse(e.getMessage()));
        } catch (Exception e) {
            log.error("상담 종료 실패", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("상담 종료에 실패했습니다: " + e.getMessage()));
        }
    }

    /**
     * 메모 작성/수정 (교수용)
     *
     * @param memoDto 메모 정보
     * @param authentication Spring Security 인증 정보
     * @return 메모가 업데이트된 상담 정보
     */
    @PostMapping("/memo")
    public ResponseEntity<?> updateMemo(
            @Valid @RequestBody ConsultationMemoDto memoDto,
            Authentication authentication) {
        try {
            // 인증 및 권한 확인
            UserTbl user = validateProfessorAuth(authentication);

            log.info("메모 업데이트: requestIdx={}, professor={}",
                     memoDto.getRequestIdx(), user.getUserCode());

            // Service 호출
            ConsultationRequestDto result = consultationService.updateMemo(memoDto);

            log.info("메모 업데이트 완료: requestIdx={}", result.getRequestIdx());
            return ResponseEntity.ok(result);

        } catch (SecurityException e) {
            log.warn("권한 없음: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(createErrorResponse(e.getMessage()));
        } catch (Exception e) {
            log.error("메모 업데이트 실패", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("메모 업데이트에 실패했습니다: " + e.getMessage()));
        }
    }

    /**
     * 내가 보낸 상담 요청 목록 조회 (학생용)
     *
     * @param page 페이지 번호 (기본값: 0)
     * @param size 페이지 크기 (기본값: 20)
     * @param status 요청 상태 필터 (선택)
     * @param authentication Spring Security 인증 정보
     * @return 상담 요청 목록
     */
    @PostMapping("/my-requests")
    public ResponseEntity<?> getMyRequests(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String status,
            Authentication authentication) {
        try {
            // 인증 확인
            UserTbl user = validateAuth(authentication);

            log.info("내 요청 조회: requester={}, status={}, page={}, size={}",
                     user.getUserCode(), status, page, size);

            Pageable pageable = PageRequest.of(page, size);
            Page<ConsultationRequestDto> result = consultationService.getMyRequests(
                    user.getUserCode(), status, pageable);

            log.info("내 요청 조회 완료: totalElements={}", result.getTotalElements());
            return ResponseEntity.ok(result);

        } catch (Exception e) {
            log.error("내 요청 조회 실패", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("내 요청 조회에 실패했습니다: " + e.getMessage()));
        }
    }

    /**
     * 받은 상담 요청 목록 조회 (교수용)
     *
     * @param page 페이지 번호 (기본값: 0)
     * @param size 페이지 크기 (기본값: 20)
     * @param status 요청 상태 필터 (선택)
     * @param authentication Spring Security 인증 정보
     * @return 상담 요청 목록
     */
    @PostMapping("/received")
    public ResponseEntity<?> getReceivedRequests(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String status,
            Authentication authentication) {
        try {
            // 인증 및 권한 확인
            UserTbl user = validateProfessorAuth(authentication);

            log.info("받은 요청 조회: recipient={}, status={}, page={}, size={}",
                     user.getUserCode(), status, page, size);

            Pageable pageable = PageRequest.of(page, size);
            Page<ConsultationRequestDto> result = consultationService.getReceivedRequests(
                    user.getUserCode(), status, pageable);

            log.info("받은 요청 조회 완료: totalElements={}", result.getTotalElements());
            return ResponseEntity.ok(result);

        } catch (SecurityException e) {
            log.warn("권한 없음: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(createErrorResponse(e.getMessage()));
        } catch (Exception e) {
            log.error("받은 요청 조회 실패", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("받은 요청 조회에 실패했습니다: " + e.getMessage()));
        }
    }

    /**
     * 진행 중인 상담 목록 조회 (학생/교수)
     *
     * @param page 페이지 번호 (기본값: 0)
     * @param size 페이지 크기 (기본값: 20)
     * @param authentication Spring Security 인증 정보
     * @return 진행 중인 상담 목록
     */
    @PostMapping("/active")
    public ResponseEntity<?> getActiveConsultations(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Authentication authentication) {
        try {
            // 인증 확인
            UserTbl user = validateAuth(authentication);

            log.info("진행 중인 상담 조회: userCode={}, page={}, size={}",
                     user.getUserCode(), page, size);

            Pageable pageable = PageRequest.of(page, size);
            Page<ConsultationRequestDto> result = consultationService.getActiveConsultations(
                    user.getUserCode(), pageable);

            log.info("진행 중인 상담 조회 완료: totalElements={}", result.getTotalElements());
            return ResponseEntity.ok(result);

        } catch (Exception e) {
            log.error("진행 중인 상담 조회 실패", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("진행 중인 상담 조회에 실패했습니다: " + e.getMessage()));
        }
    }

    /**
     * 완료된 상담 이력 조회 (학생/교수)
     *
     * @param historyDto 조회 조건
     * @param authentication Spring Security 인증 정보
     * @return 완료된 상담 목록
     */
    @PostMapping("/history")
    public ResponseEntity<?> getConsultationHistory(
            @Valid @RequestBody ConsultationHistoryRequestDto historyDto,
            Authentication authentication) {
        try {
            // 인증 확인
            UserTbl user = validateAuth(authentication);

            // JWT에서 추출한 사용자 정보를 DTO에 설정
            historyDto.setUserCode(user.getUserCode());

            log.info("상담 이력 조회: userCode={}, startDate={}, endDate={}",
                     historyDto.getUserCode(), historyDto.getStartDate(), historyDto.getEndDate());

            Page<ConsultationRequestDto> result = consultationService.getConsultationHistory(historyDto);

            log.info("상담 이력 조회 완료: totalElements={}", result.getTotalElements());
            return ResponseEntity.ok(result);

        } catch (Exception e) {
            log.error("상담 이력 조회 실패", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("상담 이력 조회에 실패했습니다: " + e.getMessage()));
        }
    }

    /**
     * 상담 상세 정보 조회 (학생/교수)
     *
     * @param requestIdx 상담 요청 ID
     * @param authentication Spring Security 인증 정보
     * @return 상담 상세 정보
     */
    @GetMapping("/{requestIdx}")
    public ResponseEntity<?> getConsultationDetail(
            @PathVariable Long requestIdx,
            Authentication authentication) {
        try {
            // 인증 확인
            UserTbl user = validateAuth(authentication);

            log.info("상담 상세 조회: requestIdx={}, userCode={}",
                     requestIdx, user.getUserCode());

            ConsultationRequestDto result = consultationService.getConsultationDetail(
                    requestIdx, user.getUserCode());

            log.info("상담 상세 조회 완료: requestIdx={}", requestIdx);
            return ResponseEntity.ok(result);

        } catch (SecurityException e) {
            log.warn("권한 없음: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(createErrorResponse(e.getMessage()));
        } catch (Exception e) {
            log.error("상담 상세 조회 실패", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("상담 상세 조회에 실패했습니다: " + e.getMessage()));
        }
    }

    /**
     * 읽지 않은 상담 요청 개수 조회 (교수용)
     *
     * @param authentication Spring Security 인증 정보
     * @return 읽지 않은 요청 개수
     */
    @GetMapping("/unread-count")
    public ResponseEntity<?> getUnreadRequestCount(Authentication authentication) {
        try {
            // 인증 및 권한 확인
            UserTbl user = validateProfessorAuth(authentication);

            log.info("읽지 않은 요청 개수 조회: recipient={}", user.getUserCode());

            long count = consultationService.getUnreadRequestCount(user.getUserCode());

            Map<String, Object> response = new HashMap<>();
            response.put("recipientUserCode", user.getUserCode());
            response.put("unreadCount", count);

            log.info("읽지 않은 요청 개수 조회 완료: count={}", count);
            return ResponseEntity.ok(response);

        } catch (SecurityException e) {
            log.warn("권한 없음: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(createErrorResponse(e.getMessage()));
        } catch (Exception e) {
            log.error("읽지 않은 요청 개수 조회 실패", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("읽지 않은 요청 개수 조회에 실패했습니다: " + e.getMessage()));
        }
    }

    /**
     * 읽음 시간 업데이트 (학생/교수)
     *
     * @param idDto 상담 ID
     * @param authentication Spring Security 인증 정보
     * @return 성공 메시지
     */
    @PostMapping("/read")
    public ResponseEntity<?> updateReadTime(
            @Valid @RequestBody ConsultationIdDto idDto,
            Authentication authentication) {
        try {
            // 인증 확인
            UserTbl user = validateAuth(authentication);

            log.info("읽음 처리: requestIdx={}, userCode={}",
                     idDto.getRequestIdx(), user.getUserCode());

            ConsultationReadReceiptDto receipt =
                    consultationService.updateReadTime(idDto.getRequestIdx(), user.getUserCode());

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "읽음 처리가 완료되었습니다.");
            response.put("requestIdx", idDto.getRequestIdx());
            response.put("readAt", receipt.getReadAt());
            response.put("allMessagesRead", receipt.isAllMessagesRead());
            response.put("lastActivityAt", receipt.getLastActivityAt());
            response.put("partnerUserCode", receipt.getPartnerUserCode());

            log.info("읽음 처리 완료: requestIdx={}", idDto.getRequestIdx());
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("읽음 처리 실패", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("읽음 처리에 실패했습니다: " + e.getMessage()));
        }
    }

    // ========== Private Helper Methods ==========

    /**
     * 인증 확인 (학생/교수 공통)
     */
    private UserTbl validateAuth(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new SecurityException("인증이 필요합니다.");
        }

        String userEmail = authentication.getName();
        return userRepository.findByUserEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("사용자 정보를 찾을 수 없습니다."));
    }

    /**
     * 교수 권한 확인
     */
    private UserTbl validateProfessorAuth(Authentication authentication) {
        UserTbl user = validateAuth(authentication);

        // 교수 권한 확인 (userStudent = 1: 교수, 0: 학생)
        if (user.getUserStudent() != 1) {
            throw new SecurityException("교수 권한이 필요합니다.");
        }

        return user;
    }

    /**
     * 에러 응답 생성
     */
    private Map<String, Object> createErrorResponse(String message) {
        Map<String, Object> error = new HashMap<>();
        error.put("success", false);
        error.put("message", message);
        error.put("timestamp", System.currentTimeMillis());
        return error;
    }
}
