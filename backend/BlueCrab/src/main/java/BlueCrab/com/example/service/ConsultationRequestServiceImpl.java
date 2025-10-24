package BlueCrab.com.example.service;

import BlueCrab.com.example.dto.Consultation.*;
import BlueCrab.com.example.entity.ConsultationRequest;
import BlueCrab.com.example.entity.UserTbl;
import BlueCrab.com.example.enums.ConsultationStatus;
import BlueCrab.com.example.enums.ConsultationType;
import BlueCrab.com.example.enums.RequestStatus;
import BlueCrab.com.example.repository.ConsultationRequestRepository;
import BlueCrab.com.example.repository.UserTblRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

/**
 * 상담 요청/진행 관리 Service 구현 클래스
 *
 * 주요 기능:
 * - 상담 요청 생성 및 승인/반려/취소
 * - 상담 시작/종료
 * - 상담 목록 조회 (학생/교수)
 * - 읽음 처리 및 자동 종료
 *
 * @author BlueCrab Development Team
 * @version 1.0.0
 * @since 2025-10-24
 */
@Slf4j
@Service
@Transactional
public class ConsultationRequestServiceImpl implements ConsultationRequestService {

    private final ConsultationRequestRepository consultationRepository;
    private final UserTblRepository userRepository;
    private final ChatService chatService;
    private final MinIOService minIOService;

    public ConsultationRequestServiceImpl(
        ConsultationRequestRepository consultationRepository,
        UserTblRepository userRepository,
        ChatService chatService,
        MinIOService minIOService
    ) {
        this.consultationRepository = consultationRepository;
        this.userRepository = userRepository;
        this.chatService = chatService;
        this.minIOService = minIOService;
    }

    @Override
    public ConsultationRequestDto createRequest(ConsultationRequestCreateDto createDto) {
        try {
            log.info("상담 요청 생성 시작: requester={}, recipient={}",
                     createDto.getRequesterUserCode(), createDto.getRecipientUserCode());

            // 사용자 존재 확인
            validateUserExists(createDto.getRequesterUserCode());
            validateUserExists(createDto.getRecipientUserCode());

            // 엔티티 생성
            ConsultationRequest consultation = new ConsultationRequest();
            consultation.setRequesterUserCode(createDto.getRequesterUserCode());
            consultation.setRecipientUserCode(createDto.getRecipientUserCode());
            consultation.setConsultationTypeEnum(createDto.getConsultationType());
            consultation.setTitle(createDto.getTitle());
            consultation.setContent(createDto.getContent());
            consultation.setDesiredDate(createDto.getDesiredDate());
            consultation.setRequestStatusEnum(RequestStatus.PENDING);

            // 저장
            ConsultationRequest saved = consultationRepository.save(consultation);

            log.info("상담 요청 생성 완료: requestIdx={}", saved.getRequestIdx());

            return toDto(saved);

        } catch (Exception e) {
            log.error("상담 요청 생성 실패: {}", e.getMessage(), e);
            throw new RuntimeException("상담 요청 생성에 실패했습니다: " + e.getMessage(), e);
        }
    }

    @Override
    public ConsultationRequestDto approveRequest(ConsultationApproveDto approveDto) {
        try {
            log.info("상담 요청 승인 시작: requestIdx={}", approveDto.getRequestIdx());

            ConsultationRequest consultation = consultationRepository
                .findById(approveDto.getRequestIdx())
                .orElseThrow(() -> new NoSuchElementException("상담 요청을 찾을 수 없습니다."));

            // 상태 검증
            if (!RequestStatus.PENDING.equals(consultation.getRequestStatusEnum())) {
                throw new IllegalStateException("대기 중인 요청만 승인할 수 있습니다. 현재 상태: "
                    + consultation.getRequestStatus());
            }

            // 승인 처리
            consultation.setRequestStatusEnum(RequestStatus.APPROVED);
            consultation.setAcceptMessage(approveDto.getAcceptMessage());
            consultation.setConsultationStatusEnum(ConsultationStatus.SCHEDULED);

            ConsultationRequest saved = consultationRepository.save(consultation);

            log.info("상담 요청 승인 완료: requestIdx={}", saved.getRequestIdx());

            return toDto(saved);

        } catch (Exception e) {
            log.error("상담 요청 승인 실패: {}", e.getMessage(), e);
            throw new RuntimeException("상담 요청 승인에 실패했습니다: " + e.getMessage(), e);
        }
    }

    @Override
    public ConsultationRequestDto rejectRequest(ConsultationRejectDto rejectDto) {
        try {
            log.info("상담 요청 반려 시작: requestIdx={}", rejectDto.getRequestIdx());

            ConsultationRequest consultation = consultationRepository
                .findById(rejectDto.getRequestIdx())
                .orElseThrow(() -> new NoSuchElementException("상담 요청을 찾을 수 없습니다."));

            // 상태 검증
            if (!RequestStatus.PENDING.equals(consultation.getRequestStatusEnum())) {
                throw new IllegalStateException("대기 중인 요청만 반려할 수 있습니다. 현재 상태: "
                    + consultation.getRequestStatus());
            }

            // 반려 처리
            consultation.setRequestStatusEnum(RequestStatus.REJECTED);
            consultation.setRejectionReason(rejectDto.getRejectionReason());

            ConsultationRequest saved = consultationRepository.save(consultation);

            log.info("상담 요청 반려 완료: requestIdx={}", saved.getRequestIdx());

            return toDto(saved);

        } catch (Exception e) {
            log.error("상담 요청 반려 실패: {}", e.getMessage(), e);
            throw new RuntimeException("상담 요청 반려에 실패했습니다: " + e.getMessage(), e);
        }
    }

    @Override
    public ConsultationRequestDto cancelRequest(ConsultationCancelDto cancelDto) {
        try {
            log.info("상담 요청 취소 시작: requestIdx={}", cancelDto.getRequestIdx());

            ConsultationRequest consultation = consultationRepository
                .findById(cancelDto.getRequestIdx())
                .orElseThrow(() -> new NoSuchElementException("상담 요청을 찾을 수 없습니다."));

            // 취소 가능 상태 검증 (PENDING 또는 APPROVED 만 취소 가능)
            RequestStatus currentStatus = consultation.getRequestStatusEnum();
            if (currentStatus != RequestStatus.PENDING && currentStatus != RequestStatus.APPROVED) {
                throw new IllegalStateException("취소할 수 없는 상태입니다. 현재 상태: "
                    + consultation.getRequestStatus());
            }

            // 취소 처리
            consultation.setRequestStatusEnum(RequestStatus.CANCELLED);
            consultation.setCancelReason(cancelDto.getCancelReason());

            // 상담이 예정되어 있었다면 상담 상태도 CANCELLED로 변경
            if (consultation.getConsultationStatusEnum() != null) {
                consultation.setConsultationStatusEnum(ConsultationStatus.CANCELLED);
            }

            ConsultationRequest saved = consultationRepository.save(consultation);

            log.info("상담 요청 취소 완료: requestIdx={}", saved.getRequestIdx());

            return toDto(saved);

        } catch (Exception e) {
            log.error("상담 요청 취소 실패: {}", e.getMessage(), e);
            throw new RuntimeException("상담 요청 취소에 실패했습니다: " + e.getMessage(), e);
        }
    }

    @Override
    public ConsultationRequestDto startConsultation(ConsultationIdDto idDto) {
        try {
            log.info("상담 시작: requestIdx={}", idDto.getRequestIdx());

            ConsultationRequest consultation = consultationRepository
                .findById(idDto.getRequestIdx())
                .orElseThrow(() -> new NoSuchElementException("상담 요청을 찾을 수 없습니다."));

            // 상태 검증
            if (!ConsultationStatus.SCHEDULED.equals(consultation.getConsultationStatusEnum())) {
                throw new IllegalStateException("예정된 상담만 시작할 수 있습니다. 현재 상태: "
                    + consultation.getConsultationStatus());
            }

            // 시작 처리
            LocalDateTime now = LocalDateTime.now();
            consultation.setConsultationStatusEnum(ConsultationStatus.IN_PROGRESS);
            consultation.setStartedAt(now);
            consultation.setLastActivityAt(now);

            ConsultationRequest saved = consultationRepository.save(consultation);

            log.info("상담 시작 완료: requestIdx={}", saved.getRequestIdx());

            return toDto(saved);

        } catch (Exception e) {
            log.error("상담 시작 실패: {}", e.getMessage(), e);
            throw new RuntimeException("상담 시작에 실패했습니다: " + e.getMessage(), e);
        }
    }

    @Override
    public ConsultationRequestDto endConsultation(ConsultationIdDto idDto) {
        try {
            log.info("상담 종료: requestIdx={}", idDto.getRequestIdx());

            ConsultationRequest consultation = consultationRepository
                .findById(idDto.getRequestIdx())
                .orElseThrow(() -> new NoSuchElementException("상담 요청을 찾을 수 없습니다."));

            // 상태 검증
            if (!ConsultationStatus.IN_PROGRESS.equals(consultation.getConsultationStatusEnum())) {
                throw new IllegalStateException("진행 중인 상담만 종료할 수 있습니다. 현재 상태: "
                    + consultation.getConsultationStatus());
            }

            // 1. Redis 채팅 로그 조회 및 MinIO 아카이빙
            Long requestIdx = idDto.getRequestIdx();
            long messageCount = chatService.getMessageCount(requestIdx);
            
            if (messageCount > 0) {
                log.info("채팅 로그 아카이빙 시작: requestIdx={}, messageCount={}", requestIdx, messageCount);
                
                try {
                    // 채팅 로그를 텍스트 형식으로 포맷
                    String chatLog = chatService.formatChatLog(requestIdx);
                    
                    // MinIO에 업로드
                    String fileName = String.format("chat-log-%d-%s.txt", 
                        requestIdx, 
                        LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss")));
                    
                    ByteArrayInputStream inputStream = new ByteArrayInputStream(
                        chatLog.getBytes(StandardCharsets.UTF_8));
                    
                    minIOService.uploadChatLog("consultation-chats", fileName, inputStream, chatLog.length());
                    
                    // 채팅 로그 경로를 DB에 저장 (chat_log_url 컬럼이 있다면)
                    // consultation.setChatLogUrl("consultation-chats/" + fileName);
                    
                    log.info("채팅 로그 아카이빙 완료: {}", fileName);
                    
                    // Redis에서 채팅 메시지 삭제
                    chatService.deleteMessages(requestIdx);
                    log.info("Redis 채팅 메시지 삭제 완료: requestIdx={}", requestIdx);
                    
                } catch (Exception e) {
                    log.error("채팅 로그 아카이빙 실패: requestIdx={}", requestIdx, e);
                    // 아카이빙 실패해도 상담 종료는 계속 진행
                }
            } else {
                log.info("채팅 메시지가 없어 아카이빙 생략: requestIdx={}", requestIdx);
            }

            // 2. 상담 종료 처리
            LocalDateTime now = LocalDateTime.now();
            consultation.setConsultationStatusEnum(ConsultationStatus.COMPLETED);
            consultation.setEndedAt(now);

            // 상담 시간 계산 (분 단위)
            if (consultation.getStartedAt() != null) {
                Duration duration = Duration.between(consultation.getStartedAt(), now);
                consultation.setDurationMinutes((int) duration.toMinutes());
            }

            ConsultationRequest saved = consultationRepository.save(consultation);

            log.info("상담 종료 완료: requestIdx={}, duration={}분",
                     saved.getRequestIdx(), saved.getDurationMinutes());

            return toDto(saved);

        } catch (Exception e) {
            log.error("상담 종료 실패: {}", e.getMessage(), e);
            throw new RuntimeException("상담 종료에 실패했습니다: " + e.getMessage(), e);
        }
    }

    @Override
    public ConsultationRequestDto updateMemo(ConsultationMemoDto memoDto) {
        try {
            log.info("메모 업데이트: requestIdx={}", memoDto.getRequestIdx());

            ConsultationRequest consultation = consultationRepository
                .findById(memoDto.getRequestIdx())
                .orElseThrow(() -> new NoSuchElementException("상담 요청을 찾을 수 없습니다."));

            // 메모 업데이트
            consultation.setMemo(memoDto.getMemo());

            ConsultationRequest saved = consultationRepository.save(consultation);

            log.info("메모 업데이트 완료: requestIdx={}", saved.getRequestIdx());

            return toDto(saved);

        } catch (Exception e) {
            log.error("메모 업데이트 실패: {}", e.getMessage(), e);
            throw new RuntimeException("메모 업데이트에 실패했습니다: " + e.getMessage(), e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ConsultationRequestDto> getMyRequests(String requesterUserCode, String requestStatus, Pageable pageable) {
        try {
            log.info("내 상담 요청 조회: requester={}, status={}", requesterUserCode, requestStatus);

            Page<ConsultationRequest> consultations;

            if (requestStatus == null || requestStatus.isEmpty()) {
                consultations = consultationRepository
                    .findByRequesterUserCodeOrderByCreatedAtDesc(requesterUserCode, pageable);
            } else {
                consultations = consultationRepository
                    .findByRequesterUserCodeAndRequestStatusOrderByCreatedAtDesc(
                        requesterUserCode, requestStatus, pageable);
            }

            return consultations.map(this::toDto);

        } catch (Exception e) {
            log.error("내 상담 요청 조회 실패: {}", e.getMessage(), e);
            throw new RuntimeException("내 상담 요청 조회에 실패했습니다: " + e.getMessage(), e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ConsultationRequestDto> getReceivedRequests(String recipientUserCode, String requestStatus, Pageable pageable) {
        try {
            log.info("받은 상담 요청 조회: recipient={}, status={}", recipientUserCode, requestStatus);

            Page<ConsultationRequest> consultations;

            if (requestStatus == null || requestStatus.isEmpty()) {
                consultations = consultationRepository
                    .findByRecipientUserCodeOrderByCreatedAtDesc(recipientUserCode, pageable);
            } else {
                consultations = consultationRepository
                    .findByRecipientUserCodeAndRequestStatusOrderByCreatedAtDesc(
                        recipientUserCode, requestStatus, pageable);
            }

            return consultations.map(this::toDto);

        } catch (Exception e) {
            log.error("받은 상담 요청 조회 실패: {}", e.getMessage(), e);
            throw new RuntimeException("받은 상담 요청 조회에 실패했습니다: " + e.getMessage(), e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ConsultationRequestDto> getActiveConsultations(String userCode, Pageable pageable) {
        try {
            log.info("진행 중인 상담 조회: userCode={}", userCode);

            Page<ConsultationRequest> consultations = consultationRepository
                .findActiveConsultations(userCode, pageable);

            return consultations.map(this::toDto);

        } catch (Exception e) {
            log.error("진행 중인 상담 조회 실패: {}", e.getMessage(), e);
            throw new RuntimeException("진행 중인 상담 조회에 실패했습니다: " + e.getMessage(), e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ConsultationRequestDto> getConsultationHistory(ConsultationHistoryRequestDto historyDto) {
        try {
            log.info("상담 이력 조회: userCode={}", historyDto.getUserCode());

            Pageable pageable = PageRequest.of(
                historyDto.getPage() != null ? historyDto.getPage() : 0,
                historyDto.getSize() != null ? historyDto.getSize() : 20
            );

            Page<ConsultationRequest> consultations = consultationRepository
                .findCompletedConsultations(
                    historyDto.getUserCode(),
                    historyDto.getStartDate(),
                    historyDto.getEndDate(),
                    pageable
                );

            return consultations.map(this::toDto);

        } catch (Exception e) {
            log.error("상담 이력 조회 실패: {}", e.getMessage(), e);
            throw new RuntimeException("상담 이력 조회에 실패했습니다: " + e.getMessage(), e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public ConsultationRequestDto getConsultationDetail(Long requestIdx, String userCode) {
        try {
            log.info("상담 상세 조회: requestIdx={}, userCode={}", requestIdx, userCode);

            ConsultationRequest consultation = consultationRepository
                .findById(requestIdx)
                .orElseThrow(() -> new NoSuchElementException("상담 요청을 찾을 수 없습니다."));

            // 권한 검증
            if (!isParticipant(consultation, userCode)) {
                throw new SecurityException("해당 상담을 조회할 권한이 없습니다.");
            }

            return toDto(consultation);

        } catch (Exception e) {
            log.error("상담 상세 조회 실패: {}", e.getMessage(), e);
            throw new RuntimeException("상담 상세 조회에 실패했습니다: " + e.getMessage(), e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public long getUnreadRequestCount(String recipientUserCode) {
        try {
            log.info("읽지 않은 요청 개수 조회: recipient={}", recipientUserCode);

            return consultationRepository.countUnreadRequests(recipientUserCode);

        } catch (Exception e) {
            log.error("읽지 않은 요청 개수 조회 실패: {}", e.getMessage(), e);
            throw new RuntimeException("읽지 않은 요청 개수 조회에 실패했습니다: " + e.getMessage(), e);
        }
    }

    @Override
    public void updateReadTime(Long requestIdx, String userCode) {
        try {
            log.info("읽음 시간 업데이트: requestIdx={}, userCode={}", requestIdx, userCode);

            ConsultationRequest consultation = consultationRepository
                .findById(requestIdx)
                .orElseThrow(() -> new NoSuchElementException("상담 요청을 찾을 수 없습니다."));

            // 권한 검증
            if (!isParticipant(consultation, userCode)) {
                throw new SecurityException("해당 상담을 조회할 권한이 없습니다.");
            }

            LocalDateTime now = LocalDateTime.now();

            // 요청자인 경우 학생 읽음 시간 업데이트
            if (consultation.getRequesterUserCode().equals(userCode)) {
                consultation.setLastReadTimeStudent(now);
            }
            // 수신자인 경우 교수 읽음 시간 업데이트
            else if (consultation.getRecipientUserCode().equals(userCode)) {
                consultation.setLastReadTimeProfessor(now);
            }

            consultationRepository.save(consultation);

            log.info("읽음 시간 업데이트 완료: requestIdx={}", requestIdx);

        } catch (Exception e) {
            log.error("읽음 시간 업데이트 실패: {}", e.getMessage(), e);
            throw new RuntimeException("읽음 시간 업데이트에 실패했습니다: " + e.getMessage(), e);
        }
    }

    @Override
    public int autoEndInactiveConsultations() {
        try {
            log.info("비활성 상담 자동 종료 시작");

            LocalDateTime threshold = LocalDateTime.now().minusHours(2);
            List<ConsultationRequest> inactiveConsultations = consultationRepository
                .findInactiveConsultations(threshold);

            int count = 0;
            for (ConsultationRequest consultation : inactiveConsultations) {
                consultation.setConsultationStatusEnum(ConsultationStatus.COMPLETED);
                consultation.setEndedAt(LocalDateTime.now());

                // 상담 시간 계산
                if (consultation.getStartedAt() != null) {
                    Duration duration = Duration.between(consultation.getStartedAt(), consultation.getEndedAt());
                    consultation.setDurationMinutes((int) duration.toMinutes());
                }

                consultationRepository.save(consultation);
                count++;
            }

            log.info("비활성 상담 자동 종료 완료: {}건", count);
            return count;

        } catch (Exception e) {
            log.error("비활성 상담 자동 종료 실패: {}", e.getMessage(), e);
            return 0;
        }
    }

    @Override
    public int autoEndLongRunningConsultations() {
        try {
            log.info("장시간 실행 상담 자동 종료 시작");

            LocalDateTime threshold = LocalDateTime.now().minusHours(24);
            List<ConsultationRequest> longRunningConsultations = consultationRepository
                .findLongRunningConsultations(threshold);

            int count = 0;
            for (ConsultationRequest consultation : longRunningConsultations) {
                consultation.setConsultationStatusEnum(ConsultationStatus.COMPLETED);
                consultation.setEndedAt(LocalDateTime.now());

                // 상담 시간 계산
                if (consultation.getStartedAt() != null) {
                    Duration duration = Duration.between(consultation.getStartedAt(), consultation.getEndedAt());
                    consultation.setDurationMinutes((int) duration.toMinutes());
                }

                consultationRepository.save(consultation);
                count++;
            }

            log.info("장시간 실행 상담 자동 종료 완료: {}건", count);
            return count;

        } catch (Exception e) {
            log.error("장시간 실행 상담 자동 종료 실패: {}", e.getMessage(), e);
            return 0;
        }
    }

    // ========== Private Helper Methods ==========

    /**
     * ConsultationRequest Entity를 DTO로 변환
     */
    private ConsultationRequestDto toDto(ConsultationRequest entity) {
        ConsultationRequestDto dto = new ConsultationRequestDto();

        dto.setRequestIdx(entity.getRequestIdx());
        dto.setRequesterUserCode(entity.getRequesterUserCode());
        dto.setRecipientUserCode(entity.getRecipientUserCode());
        dto.setConsultationType(entity.getConsultationTypeEnum());
        dto.setTitle(entity.getTitle());
        dto.setContent(entity.getContent());
        dto.setDesiredDate(entity.getDesiredDate());
        dto.setRequestStatus(entity.getRequestStatusEnum());
        dto.setAcceptMessage(entity.getAcceptMessage());
        dto.setRejectionReason(entity.getRejectionReason());
        dto.setCancelReason(entity.getCancelReason());
        dto.setConsultationStatus(entity.getConsultationStatusEnum());
        dto.setStartedAt(entity.getStartedAt());
        dto.setEndedAt(entity.getEndedAt());
        dto.setDurationMinutes(entity.getDurationMinutes());
        dto.setLastActivityAt(entity.getLastActivityAt());
        dto.setCreatedAt(entity.getCreatedAt());

        // 사용자 이름 조회
        getUserName(entity.getRequesterUserCode()).ifPresent(dto::setRequesterName);
        getUserName(entity.getRecipientUserCode()).ifPresent(dto::setRecipientName);

        return dto;
    }

    /**
     * 사용자 이름 조회
     */
    private Optional<String> getUserName(String userCode) {
        try {
            return userRepository.findByUserCode(userCode)
                .map(UserTbl::getUserName);
        } catch (Exception e) {
            log.warn("사용자 이름 조회 실패: userCode={}", userCode);
            return Optional.empty();
        }
    }

    /**
     * 사용자 존재 여부 확인
     */
    private void validateUserExists(String userCode) {
        if (!userRepository.existsByUserCode(userCode)) {
            throw new NoSuchElementException("사용자를 찾을 수 없습니다: " + userCode);
        }
    }

    /**
     * 참여자 확인
     */
    private boolean isParticipant(ConsultationRequest consultation, String userCode) {
        return consultation.getRequesterUserCode().equals(userCode)
            || consultation.getRecipientUserCode().equals(userCode);
    }
}
