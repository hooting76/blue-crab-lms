package BlueCrab.com.example.service;

import BlueCrab.com.example.dto.Consultation.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * 상담 요청/진행 관리를 위한 Service 인터페이스 (리팩토링 버전)
 *
 * 통합 API 구조:
 * - 단일 상태 관리 (status)
 * - 통합 조회/변경 메서드
 * - 불필요한 메서드 제거 (메모 등)
 *
 * @author BlueCrab Development Team
 * @version 2.0.0
 * @since 2025-10-28
 */
public interface ConsultationRequestService {

    // ========== 조회 API ==========

    /**
     * 상담 목록 통합 조회
     * ViewType에 따라 다른 목록 반환 (SENT/RECEIVED/ACTIVE/HISTORY)
     *
     * @param request ViewType, status, page, size 등
     * @param userCode 조회자 USER_CODE
     * @return 상담 목록 (페이징)
     */
    Page<ConsultationRequestDto> getConsultationList(ConsultationListRequest request, String userCode);

    /**
     * 상담 상세 조회
     *
     * @param requestIdx 상담 요청 ID
     * @param userCode 조회자 USER_CODE (권한 검증용)
     * @return 상담 상세 정보
     */
    ConsultationRequestDto getConsultationDetail(Long requestIdx, String userCode);

    /**
     * 읽지 않은 상담 요청 개수 조회 (교수용)
     *
     * @param recipientUserCode 수신자 USER_CODE
     * @return 읽지 않은 요청 개수
     */
    long getUnreadRequestCount(String recipientUserCode);

    // ========== 상담 관리 API ==========

    /**
     * 상담 생성
     *
     * @param createDto 상담 요청 생성 정보
     * @return 생성된 상담 요청 정보
     */
    ConsultationRequestDto createConsultation(ConsultationRequestCreateDto createDto);

    /**
     * 상담 상태 통합 변경
     * 모든 상태 전환을 단일 메서드로 처리:
     * - PENDING → APPROVED, REJECTED, CANCELLED
     * - APPROVED → IN_PROGRESS, CANCELLED
     * - IN_PROGRESS → COMPLETED, CANCELLED
     *
     * @param request 상담 ID, 변경할 상태, 사유
     * @param userCode 변경자 USER_CODE
     * @return 상태가 변경된 상담 정보
     */
    ConsultationRequestDto changeConsultationStatus(ConsultationStatusRequest request, String userCode);

    /**
     * 읽음 시간 업데이트
     *
     * @param requestIdx 상담 요청 ID
     * @param userCode 사용자 USER_CODE
     * @return 읽음 처리 결과
     */
    ConsultationReadReceiptDto updateReadTime(Long requestIdx, String userCode);

    // ========== 자동화 API (스케줄러용) ==========

    /**
     * 비활성 상담 자동 종료
     * - 2시간 동안 활동이 없는 상담 자동 종료
     *
     * @return 자동 종료된 상담 건수
     */
    int autoEndInactiveConsultations();

    /**
     * 장시간 실행 상담 자동 종료
     * - 24시간 이상 진행 중인 상담 자동 종료
     *
     * @return 자동 종료된 상담 건수
     */
    int autoEndLongRunningConsultations();

    // ========== 레거시 API (하위 호환) ==========

    @Deprecated
    ConsultationRequestDto createRequest(ConsultationRequestCreateDto createDto);

    @Deprecated
    ConsultationRequestDto approveRequest(ConsultationApproveDto approveDto);

    @Deprecated
    ConsultationRequestDto rejectRequest(ConsultationRejectDto rejectDto);

    @Deprecated
    ConsultationRequestDto cancelRequest(ConsultationCancelDto cancelDto);

    @Deprecated
    ConsultationRequestDto startConsultation(ConsultationIdDto idDto);

    @Deprecated
    ConsultationRequestDto endConsultation(ConsultationIdDto idDto);

    @Deprecated
    ConsultationRequestDto updateMemo(ConsultationMemoDto memoDto);

    @Deprecated
    Page<ConsultationRequestDto> getMyRequests(String requesterUserCode, String requestStatus, Pageable pageable);

    @Deprecated
    Page<ConsultationRequestDto> getReceivedRequests(String recipientUserCode, String requestStatus, Pageable pageable);

    @Deprecated
    Page<ConsultationRequestDto> getActiveConsultations(String userCode, Pageable pageable);

    @Deprecated
    Page<ConsultationRequestDto> getConsultationHistory(ConsultationHistoryRequestDto historyDto);
}
