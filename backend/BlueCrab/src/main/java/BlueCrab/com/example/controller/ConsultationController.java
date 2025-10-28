package BlueCrab.com.example.controller;

import BlueCrab.com.example.dto.Consultation.*;
import BlueCrab.com.example.entity.UserTbl;
import BlueCrab.com.example.repository.UserTblRepository;
import BlueCrab.com.example.service.ConsultationRequestService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.HashMap;
import java.util.Map;

/**
 * 상담 요청/진행 관리 REST API 컨트롤러 (리팩토링 버전)
 *
 * 통합 API 구조:
 * - 15개 엔드포인트 → 7개로 간소화
 * - 단일 상태 관리 (status)
 * - RESTful 패턴 준수
 *
 * 새로운 엔드포인트:
 * - POST /api/consultation/list     - 통합 목록 조회 (ViewType 기반)
 * - POST /api/consultation/detail   - 상세 조회
 * - POST /api/consultation/create   - 상담 생성
 * - POST /api/consultation/status   - 통합 상태 변경
 * - POST /api/consultation/read     - 읽음 처리
 * - GET  /api/consultation/unread   - 읽지 않은 수
 * - WebSocket /ws/consultation/chat - 채팅
 *
 * @author BlueCrab Development Team
 * @version 2.0.0
 * @since 2025-10-28
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
     * 상담 목록 통합 조회
     * ViewType에 따라 다른 목록 반환:
     * - SENT: 내가 보낸 요청
     * - RECEIVED: 받은 요청
     * - ACTIVE: 진행 중인 상담
     * - HISTORY: 완료된 이력
     *
     * @param listRequest ViewType, status, page, size 등 포함
     * @param authentication Spring Security 인증 정보
     * @return 상담 목록 (페이징)
     */
    @PostMapping("/list")
    public ResponseEntity<?> getConsultationList(
            @Valid @RequestBody ConsultationListRequest listRequest,
            Authentication authentication) {
        try {
            UserTbl user = validateAuth(authentication);
            Page<ConsultationRequestDto> result = consultationService.getConsultationList(
                listRequest, user.getUserCode());

            log.info("상담 목록 조회 완료: viewType={}, status={}, totalElements={}",
                listRequest.getViewType(), listRequest.getStatus(), result.getTotalElements());

            return ResponseEntity.ok(result);

        } catch (SecurityException e) {
            log.warn("상담 목록 조회 권한 없음: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(createErrorResponse(e.getMessage()));
        } catch (Exception e) {
            log.error("상담 목록 조회 실패", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(createErrorResponse("상담 목록 조회에 실패했습니다: " + e.getMessage()));
        }
    }

    /**
     * 상담 상세 조회
     *
     * @param detailRequest 상담 ID
     * @param authentication Spring Security 인증 정보
     * @return 상담 상세 정보
     */
    @PostMapping("/detail")
    public ResponseEntity<?> getConsultationDetail(
            @Valid @RequestBody ConsultationDetailRequest detailRequest,
            Authentication authentication) {
        try {
            UserTbl user = validateAuth(authentication);
            ConsultationRequestDto result = consultationService.getConsultationDetail(
                detailRequest.getId(), user.getUserCode());

            log.info("상담 상세 조회 완료: requestIdx={}", detailRequest.getId());
            return ResponseEntity.ok(result);

        } catch (SecurityException e) {
            log.warn("상담 상세 조회 권한 없음: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(createErrorResponse(e.getMessage()));
        } catch (Exception e) {
            log.error("상담 상세 조회 실패", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(createErrorResponse("상담 상세 조회에 실패했습니다: " + e.getMessage()));
        }
    }

    /**
     * 상담 생성
     *
     * @param createDto 상담 생성 정보
     * @param authentication Spring Security 인증 정보
     * @return 생성된 상담 정보
     */
    @PostMapping("/create")
    public ResponseEntity<?> createConsultation(
            @Valid @RequestBody ConsultationRequestCreateDto createDto,
            Authentication authentication) {
        try {
            UserTbl user = validateAuth(authentication);
            createDto.setRequesterUserCode(user.getUserCode());

            ConsultationRequestDto result = consultationService.createConsultation(createDto);

            log.info("상담 생성 완료: requestIdx={}, requester={}, recipient={}",
                result.getRequestIdx(), user.getUserCode(), createDto.getRecipientUserCode());

            return ResponseEntity.ok(result);

        } catch (SecurityException e) {
            log.warn("상담 생성 권한 없음: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(createErrorResponse(e.getMessage()));
        } catch (Exception e) {
            log.error("상담 생성 실패", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(createErrorResponse("상담 생성에 실패했습니다: " + e.getMessage()));
        }
    }

    /**
     * 상담 상태 통합 변경
     * 단일 엔드포인트로 모든 상태 전환 처리:
     * - PENDING → APPROVED (승인)
     * - PENDING → REJECTED (반려)
     * - PENDING → CANCELLED (취소)
     * - APPROVED → IN_PROGRESS (시작)
     * - APPROVED → CANCELLED (취소)
     * - IN_PROGRESS → COMPLETED (종료)
     * - IN_PROGRESS → CANCELLED (취소)
     *
     * @param statusRequest 상담 ID, 변경할 상태, 사유
     * @param authentication Spring Security 인증 정보
     * @return 상태가 변경된 상담 정보
     */
    @PostMapping("/status")
    public ResponseEntity<?> changeConsultationStatus(
            @Valid @RequestBody ConsultationStatusRequest statusRequest,
            Authentication authentication) {
        try {
            UserTbl user = validateAuth(authentication);
            ConsultationRequestDto result = consultationService.changeConsultationStatus(
                statusRequest, user.getUserCode());

            log.info("상담 상태 변경 완료: requestIdx={}, status={}, changedBy={}",
                statusRequest.getId(), statusRequest.getStatus(), user.getUserCode());

            return ResponseEntity.ok(result);

        } catch (SecurityException e) {
            log.warn("상담 상태 변경 권한 없음: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(createErrorResponse(e.getMessage()));
        } catch (IllegalArgumentException e) {
            log.warn("상담 상태 변경 요청 오류: {}", e.getMessage());
            return ResponseEntity.badRequest()
                .body(createErrorResponse(e.getMessage()));
        } catch (IllegalStateException e) {
            log.warn("상담 상태 변경 실패 (상태 전환 불가): {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(createErrorResponse(e.getMessage()));
        } catch (Exception e) {
            log.error("상담 상태 변경 실패", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(createErrorResponse("상담 상태 변경에 실패했습니다: " + e.getMessage()));
        }
    }

    /**
     * 읽음 처리
     *
     * @param idDto 상담 ID
     * @param authentication Spring Security 인증 정보
     * @return 읽음 처리 결과
     */
    @PostMapping("/read")
    public ResponseEntity<?> updateReadTime(
            @Valid @RequestBody ConsultationIdDto idDto,
            Authentication authentication) {
        try {
            UserTbl user = validateAuth(authentication);

            ConsultationReadReceiptDto receipt = consultationService.updateReadTime(
                idDto.getRequestIdx(), user.getUserCode());

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "읽음 처리가 완료되었습니다.");
            response.put("requestIdx", idDto.getRequestIdx());
            response.put("readAt", receipt.getReadAt());
            response.put("allMessagesRead", receipt.isAllMessagesRead());
            response.put("lastActivityAt", receipt.getLastActivityAt());
            response.put("partnerUserCode", receipt.getPartnerUserCode());

            log.info("읽음 처리 완료: requestIdx={}, reader={}",
                idDto.getRequestIdx(), user.getUserCode());

            return ResponseEntity.ok(response);

        } catch (SecurityException e) {
            log.warn("읽음 처리 권한 없음: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(createErrorResponse(e.getMessage()));
        } catch (Exception e) {
            log.error("읽음 처리 실패", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(createErrorResponse("읽음 처리에 실패했습니다: " + e.getMessage()));
        }
    }

    /**
     * 읽지 않은 상담 요청 개수 조회 (교수용)
     *
     * @param authentication Spring Security 인증 정보
     * @return 읽지 않은 요청 개수
     */
    @GetMapping("/unread")
    public ResponseEntity<?> getUnreadRequestCount(Authentication authentication) {
        try {
            UserTbl user = validateProfessorAuth(authentication);

            long count = consultationService.getUnreadRequestCount(user.getUserCode());

            Map<String, Object> response = new HashMap<>();
            response.put("recipientUserCode", user.getUserCode());
            response.put("unreadCount", count);

            log.info("읽지 않은 요청 개수 조회 완료: recipient={}, count={}",
                user.getUserCode(), count);

            return ResponseEntity.ok(response);

        } catch (SecurityException e) {
            log.warn("읽지 않은 요청 개수 조회 권한 없음: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(createErrorResponse(e.getMessage()));
        } catch (Exception e) {
            log.error("읽지 않은 요청 개수 조회 실패", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(createErrorResponse("읽지 않은 요청 개수 조회에 실패했습니다: " + e.getMessage()));
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
