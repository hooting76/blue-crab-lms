package BlueCrab.com.example.service;

import BlueCrab.com.example.dto.Consultation.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * 상담 요청/진행 관리를 위한 Service 인터페이스
 *
 * @author BlueCrab Development Team
 * @version 1.0.0
 * @since 2025-10-24
 */
public interface ConsultationRequestService {

    /**
     * 상담 요청 생성
     *
     * @param createDto 상담 요청 생성 정보
     * @return 생성된 상담 요청 정보
     */
    ConsultationRequestDto createRequest(ConsultationRequestCreateDto createDto);

    /**
     * 상담 요청 승인
     *
     * @param approveDto 승인 정보 (requestIdx, acceptMessage, scheduledStartAt)
     * @return 승인된 상담 요청 정보
     */
    ConsultationRequestDto approveRequest(ConsultationApproveDto approveDto);

    /**
     * 상담 요청 반려
     *
     * @param rejectDto 반려 정보 (requestIdx, rejectionReason)
     * @return 반려된 상담 요청 정보
     */
    ConsultationRequestDto rejectRequest(ConsultationRejectDto rejectDto);

    /**
     * 상담 요청 취소 (학생용)
     *
     * @param cancelDto 취소 정보 (requestIdx, cancelReason)
     * @return 취소된 상담 요청 정보
     */
    ConsultationRequestDto cancelRequest(ConsultationCancelDto cancelDto);

    /**
     * 상담 시작
     *
     * @param idDto requestIdx 포함한 DTO
     * @return 시작된 상담 정보
     */
    ConsultationRequestDto startConsultation(ConsultationIdDto idDto);

    /**
     * 상담 종료
     *
     * @param idDto requestIdx 포함한 DTO
     * @return 종료된 상담 정보
     */
    ConsultationRequestDto endConsultation(ConsultationIdDto idDto);

    /**
     * 메모 작성/수정 (교수 전용)
     *
     * @param memoDto 메모 정보 (requestIdx, memo)
     * @return 메모가 업데이트된 상담 정보
     */
    ConsultationRequestDto updateMemo(ConsultationMemoDto memoDto);

    /**
     * 학생이 보낸 상담 요청 목록 조회
     *
     * @param requesterUserCode 요청자 USER_CODE
     * @param requestStatus 요청 상태 필터 (null이면 전체)
     * @param pageable 페이징 정보
     * @return 상담 요청 목록
     */
    Page<ConsultationRequestDto> getMyRequests(String requesterUserCode, String requestStatus, Pageable pageable);

    /**
     * 교수가 받은 상담 요청 목록 조회
     *
     * @param recipientUserCode 수신자 USER_CODE
     * @param requestStatus 요청 상태 필터 (null이면 전체)
     * @param pageable 페이징 정보
     * @return 상담 요청 목록
     */
    Page<ConsultationRequestDto> getReceivedRequests(String recipientUserCode, String requestStatus, Pageable pageable);

    /**
     * 진행 중인 상담 목록 조회
     *
     * @param userCode 사용자 USER_CODE
     * @param pageable 페이징 정보
     * @return 진행 중인 상담 목록
     */
    Page<ConsultationRequestDto> getActiveConsultations(String userCode, Pageable pageable);

    /**
     * 완료된 상담 이력 조회
     *
     * @param historyDto 조회 조건 (userCode, startDate, endDate, page, size)
     * @return 완료된 상담 목록
     */
    Page<ConsultationRequestDto> getConsultationHistory(ConsultationHistoryRequestDto historyDto);

    /**
     * 상담 상세 정보 조회
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

    /**
     * 읽음 시간 업데이트
     *
     * @param requestIdx 상담 요청 ID
     * @param userCode 사용자 USER_CODE
     */
    void updateReadTime(Long requestIdx, String userCode);

    /**
     * 비활성 상담 자동 종료 (스케줄러용)
     * - 2시간 동안 활동이 없는 상담 자동 종료
     *
     * @return 자동 종료된 상담 건수
     */
    int autoEndInactiveConsultations();

    /**
     * 장시간 실행 상담 자동 종료 (스케줄러용)
     * - 24시간 이상 진행 중인 상담 자동 종료
     *
     * @return 자동 종료된 상담 건수
     */
    int autoEndLongRunningConsultations();
}
