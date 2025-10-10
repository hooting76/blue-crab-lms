package BlueCrab.com.example.service;

import BlueCrab.com.example.config.ReservationPolicyProperties;
import BlueCrab.com.example.dto.*;
import BlueCrab.com.example.entity.FacilityBlockTbl;
import BlueCrab.com.example.entity.FacilityPolicyTbl;
import BlueCrab.com.example.entity.FacilityReservationLog;
import BlueCrab.com.example.entity.FacilityReservationTbl;
import BlueCrab.com.example.entity.FacilityTbl;
import BlueCrab.com.example.entity.UserTbl;
import BlueCrab.com.example.enums.ReservationStatus;
import BlueCrab.com.example.exception.ResourceNotFoundException;
import BlueCrab.com.example.repository.FacilityBlockRepository;
import BlueCrab.com.example.repository.FacilityPolicyRepository;
import BlueCrab.com.example.repository.FacilityReservationLogRepository;
import BlueCrab.com.example.repository.FacilityReservationRepository;
import BlueCrab.com.example.repository.FacilityRepository;
import BlueCrab.com.example.repository.UserTblRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 시설 예약 관리를 위한 서비스 클래스
 */
@Service
@Transactional
public class FacilityReservationService {

    private static final Logger logger = LoggerFactory.getLogger(FacilityReservationService.class);

    private final FacilityReservationRepository reservationRepository;
    private final FacilityRepository facilityRepository;
    private final FacilityPolicyRepository policyRepository;
    private final FacilityBlockRepository blockRepository;
    private final UserTblRepository userRepository;
    private final FacilityReservationLogRepository logRepository;
    private final ReservationPolicyProperties policyProperties;
    private final ObjectMapper objectMapper;

    public FacilityReservationService(FacilityReservationRepository reservationRepository,
                                       FacilityRepository facilityRepository,
                                       FacilityPolicyRepository policyRepository,
                                       FacilityBlockRepository blockRepository,
                                       UserTblRepository userRepository,
                                       FacilityReservationLogRepository logRepository,
                                       ReservationPolicyProperties policyProperties,
                                       ObjectMapper objectMapper) {
        this.reservationRepository = reservationRepository;
        this.facilityRepository = facilityRepository;
        this.policyRepository = policyRepository;
        this.blockRepository = blockRepository;
        this.userRepository = userRepository;
        this.logRepository = logRepository;
        this.policyProperties = policyProperties;
        this.objectMapper = objectMapper;
    }

    public ReservationDto createReservation(String userCode, ReservationCreateRequestDto request) {
        // ① 기본 검증 (가벼운 작업 우선)
        validateReservationRequest(request);

        // ② 정책 먼저 조회 (락 획득 전에 확인하여 불필요한 락 방지)
        FacilityPolicyTbl policy = policyRepository.findByFacilityIdx(request.getFacilityIdx())
            .orElseThrow(() -> new RuntimeException("시설 정책을 찾을 수 없습니다."));

        boolean requiresApproval = Boolean.TRUE.equals(policy.getRequiresApproval());
        FacilityTbl facility;

        // ③ 승인 정책에 따라 락 전략 차별화
        if (requiresApproval) {
            // ③-A: 승인 필요 시설 → 락 없이 빠르게 처리
            // PENDING 상태로 저장되므로 동시 요청 허용 (관리자 승인 시 충돌 체크)
            facility = facilityRepository.findById(request.getFacilityIdx())
                .orElseThrow(() -> ResourceNotFoundException.forId("시설", request.getFacilityIdx()));

            if (!facility.getIsActive()) {
                throw new RuntimeException("사용 중지된 시설입니다.");
            }

            // 차단 기간만 체크 (예약 충돌은 승인 시점에 검증)
            validateNotBlocked(request.getFacilityIdx(), request.getStartTime(), request.getEndTime());

            logger.debug("Creating reservation for approval-required facility: {}", facility.getFacilityName());

        } else {
            // ③-B: 즉시 승인 시설 → 비관적 락으로 동시성 제어
            // 다른 트랜잭션이 이 시설에 대한 예약을 생성하려면 현재 트랜잭션이 완료될 때까지 대기
            facility = facilityRepository.findByIdWithLock(request.getFacilityIdx())
                .orElseThrow(() -> ResourceNotFoundException.forId("시설", request.getFacilityIdx()));

            if (!facility.getIsActive()) {
                throw new RuntimeException("사용 중지된 시설입니다.");
            }

            // 락을 획득한 상태에서 가용성 체크 - race condition 방지
            FacilityAvailabilityDto availability = checkAvailabilityWithFacility(
                facility, request.getStartTime(), request.getEndTime());

            if (!Boolean.TRUE.equals(availability.getIsAvailable())) {
                List<FacilityAvailabilityDto.TimeSlot> conflicts = availability.getConflictingReservations();
                if (conflicts != null && !conflicts.isEmpty()) {
                    FacilityAvailabilityDto.TimeSlot conflict = conflicts.get(0);
                    throw new RuntimeException(String.format("해당 시간에는 이미 다른 예약이 존재합니다. 충돌 시간: %s ~ %s",
                        conflict.getStartTime(), conflict.getEndTime()));
                }
                throw new RuntimeException("해당 시간에는 이미 다른 예약이 존재합니다.");
            }

            logger.debug("Creating auto-approved reservation for facility: {}", facility.getFacilityName());
        }

        // ④ 시설별 승인 정책에 따라 초기 상태 결정
        ReservationStatus initialStatus;
        String logEventType;

        if (requiresApproval) {
            // 관리자 승인 필요
            initialStatus = ReservationStatus.PENDING;
            logEventType = "CREATED";
        } else {
            // 즉시 예약 (자동 승인)
            initialStatus = ReservationStatus.APPROVED;
            logEventType = "AUTO_APPROVED";
        }

        FacilityReservationTbl reservation = new FacilityReservationTbl();
        reservation.setFacilityIdx(request.getFacilityIdx());
        reservation.setUserCode(userCode);
        reservation.setStartTime(request.getStartTime());
        reservation.setEndTime(request.getEndTime());
        reservation.setPartySize(request.getPartySize());
        reservation.setPurpose(request.getPurpose());
        reservation.setRequestedEquipment(request.getRequestedEquipment());
        reservation.setStatusEnum(initialStatus);

        // 자동 승인인 경우 승인 정보 설정
        if (initialStatus == ReservationStatus.APPROVED) {
            reservation.setApprovedBy("SYSTEM");
            reservation.setApprovedAt(LocalDateTime.now());
        }

        FacilityReservationTbl saved = reservationRepository.save(reservation);

        createLog(saved.getReservationIdx(), logEventType, "USER", userCode, request);

        logger.info("Reservation created: ID={}, User={}, Facility={}, Status={}, RequiresApproval={}",
            saved.getReservationIdx(), userCode, request.getFacilityIdx(),
            initialStatus, policy.getRequiresApproval());

        return convertToDto(saved);
    }

    public FacilityAvailabilityDto checkAvailability(Integer facilityIdx, LocalDateTime startTime, LocalDateTime endTime) {
        FacilityTbl facility = facilityRepository.findById(facilityIdx)
            .orElseThrow(() -> ResourceNotFoundException.forId("시설", facilityIdx));
        return checkAvailabilityInternal(facility, startTime, endTime, null);
    }

    /**
     * 이미 조회된 시설 정보로 가용성 체크 (중복 조회 방지)
     */
    private FacilityAvailabilityDto checkAvailabilityWithFacility(FacilityTbl facility, LocalDateTime startTime, LocalDateTime endTime) {
        return checkAvailabilityInternal(facility, startTime, endTime, null);
    }

    FacilityAvailabilityDto checkAvailabilityWithExclusion(Integer facilityIdx, LocalDateTime startTime, LocalDateTime endTime, Integer excludeReservationIdx) {
        FacilityTbl facility = facilityRepository.findById(facilityIdx)
            .orElseThrow(() -> ResourceNotFoundException.forId("시설", facilityIdx));
        return checkAvailabilityInternal(facility, startTime, endTime, excludeReservationIdx);
    }

    /**
     * 시설 차단 여부 검증
     * 관리자가 설정한 차단 기간에 해당하는지 확인
     */
    private void validateNotBlocked(Integer facilityIdx, LocalDateTime startTime, LocalDateTime endTime) {
        List<FacilityBlockTbl> blocks = blockRepository.findConflictingBlocks(facilityIdx, startTime, endTime);
        if (!blocks.isEmpty()) {
            FacilityBlockTbl block = blocks.get(0);
            throw new RuntimeException(
                String.format("해당 시설은 %s부터 %s까지 예약이 불가합니다. 사유: %s",
                    block.getBlockStart(), block.getBlockEnd(), block.getBlockReason()));
        }
    }

    /**
     * 가용성 체크 내부 로직 (중복 조회 제거 버전)
     * @param facility 이미 조회된 시설 정보
     */
    private FacilityAvailabilityDto checkAvailabilityInternal(FacilityTbl facility, LocalDateTime startTime, LocalDateTime endTime, Integer excludeReservationIdx) {
        // 시설 차단 여부 확인
        List<FacilityBlockTbl> blocks = blockRepository.findConflictingBlocks(
            facility.getFacilityIdx(), startTime, endTime);



        if (!blocks.isEmpty()) {

            FacilityBlockTbl block = blocks.get(0);

            throw new RuntimeException(

                String.format("해당 시설은 %s부터 %s까지 예약이 불가합니다. 사유: %s",
                    block.getBlockStart(), block.getBlockEnd(), block.getBlockReason()));

        }



        List<String> activeStatuses = java.util.Arrays.asList(
            ReservationStatus.PENDING.toDbValue(),
            ReservationStatus.APPROVED.toDbValue()
        );

        // 예약 충돌 체크
        List<FacilityReservationTbl> conflicts = reservationRepository.findConflictingReservations(
            facility.getFacilityIdx(), startTime, endTime, activeStatuses, excludeReservationIdx);



        boolean isAvailable = conflicts.isEmpty();



        List<FacilityAvailabilityDto.TimeSlot> conflictingSlots = conflicts.stream()

            .map(r -> new FacilityAvailabilityDto.TimeSlot(r.getStartTime(), r.getEndTime()))

            .collect(Collectors.toList());
        return new FacilityAvailabilityDto(

            facility.getFacilityIdx(),
            facility.getFacilityName(),
            isAvailable,
            conflictingSlots
        );

    }



    public List<ReservationDto> getMyReservations(String userCode) {

        List<FacilityReservationTbl> reservations = reservationRepository.findByUserCodeOrderByCreatedAtDesc(userCode);

        return convertToDtoList(reservations);

    }



    public List<ReservationDto> getMyReservationsByStatus(String userCode, ReservationStatus status) {

        List<FacilityReservationTbl> reservations = reservationRepository.findByUserCodeAndStatusOrderByCreatedAtDesc(userCode, status.toDbValue());

        return convertToDtoList(reservations);

    }



    public ReservationDto getReservationById(Integer reservationIdx, String userCode) {
        FacilityReservationTbl reservation = reservationRepository.findById(reservationIdx)
            .orElseThrow(() -> ResourceNotFoundException.forId("예약", reservationIdx));

        if (!reservation.getUserCode().equals(userCode)) {
            throw new RuntimeException("본인의 예약만 조회할 수 있습니다.");
        }

        return convertToDto(reservation);
    }

    public void cancelReservation(Integer reservationIdx, String userCode) {
        FacilityReservationTbl reservation = reservationRepository.findById(reservationIdx)
            .orElseThrow(() -> ResourceNotFoundException.forId("예약", reservationIdx));

        if (!reservation.getUserCode().equals(userCode)) {
            throw new RuntimeException("본인의 예약만 취소할 수 있습니다.");
        }

        if (reservation.getStatusEnum() != ReservationStatus.PENDING &&
            reservation.getStatusEnum() != ReservationStatus.APPROVED) {
            throw new RuntimeException("취소할 수 없는 상태입니다.");
        }

        reservation.setStatusEnum(ReservationStatus.CANCELLED);
        reservationRepository.save(reservation);

        createLog(reservationIdx, "CANCELLED", "USER", userCode, null);

        logger.info("Reservation cancelled: ID={}, User={}", reservationIdx, userCode);
    }

    private void validateReservationRequest(ReservationCreateRequestDto request) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime maxDate = now.plusDays(policyProperties.getMaxDaysInAdvance());

        if (request.getStartTime().isBefore(now)) {
            throw new RuntimeException("시작 시간은 현재 시간 이후여야 합니다.");
        }

        if (request.getEndTime().isBefore(request.getStartTime()) ||
            request.getEndTime().isEqual(request.getStartTime())) {
            throw new RuntimeException("종료 시간은 시작 시간 이후여야 합니다.");
        }

        // 연속된 시간대 검증: 같은 날짜 내에서만 예약 가능
        if (!request.getStartTime().toLocalDate().equals(request.getEndTime().toLocalDate())) {
            throw new RuntimeException("예약은 같은 날짜 내에서만 가능합니다. 여러 날에 걸친 예약이 필요한 경우 각 날짜별로 따로 신청해주세요.");
        }

        if (request.getStartTime().isAfter(maxDate)) {
            throw new RuntimeException(policyProperties.getMaxDaysInAdvance() + "일 이내의 예약만 가능합니다.");
        }

        long durationMinutes = ChronoUnit.MINUTES.between(request.getStartTime(), request.getEndTime());

        if (durationMinutes < policyProperties.getMinDurationMinutes()) {
            throw new RuntimeException("최소 예약 시간은 " + policyProperties.getMinDurationMinutes() + "분입니다.");
        }

        if (durationMinutes > policyProperties.getMaxDurationMinutes()) {
            throw new RuntimeException("최대 예약 시간은 " + policyProperties.getMaxDurationMinutes() + "분입니다.");
        }
    }

    private void createLog(Integer reservationIdx, String eventType, String actorType,
                          String actorCode, Object payload) {
        try {
            String payloadJson = payload != null ? objectMapper.writeValueAsString(payload) : null;
            FacilityReservationLog log = new FacilityReservationLog(
                reservationIdx, eventType, actorType, actorCode, payloadJson);
            logRepository.save(log);
        } catch (Exception e) {
            logger.error("Failed to create log for reservation {}: {}", reservationIdx, e.getMessage());
        }
    }

    /**
     * 예약 목록을 DTO로 변환 (N+1 문제 방지를 위한 배치 페치)
     */
    private List<ReservationDto> convertToDtoList(List<FacilityReservationTbl> reservations) {
        if (reservations.isEmpty()) {
            return java.util.Collections.emptyList();
        }

        // ① 모든 고유한 시설 ID 수집
        java.util.Set<Integer> facilityIds = reservations.stream()
            .map(FacilityReservationTbl::getFacilityIdx)
            .collect(Collectors.toSet());

        // ② 모든 고유한 사용자 코드 수집
        java.util.Set<String> userCodes = reservations.stream()
            .map(FacilityReservationTbl::getUserCode)
            .collect(Collectors.toSet());

        // ③ 한 번의 쿼리로 모든 시설 정보 조회
        Map<Integer, String> facilityNameCache = facilityRepository.findAllById(facilityIds)
            .stream()
            .collect(Collectors.toMap(
                FacilityTbl::getFacilityIdx,
                FacilityTbl::getFacilityName
            ));

        // ④ 한 번의 쿼리로 모든 사용자 정보 조회
        Map<String, String> userNameCache = userRepository.findAllByUserCodeIn(new java.util.ArrayList<>(userCodes))
            .stream()
            .collect(Collectors.toMap(
                UserTbl::getUserCode,
                UserTbl::getUserName
            ));

        // ⑤ 캐시를 사용하여 DTO 변환
        return reservations.stream()
            .map(reservation -> convertToDto(reservation, facilityNameCache, userNameCache))
            .collect(Collectors.toList());
    }



    private ReservationDto convertToDto(FacilityReservationTbl reservation) {

        return convertToDto(reservation, new HashMap<>(), new HashMap<>());

    }



    private ReservationDto convertToDto(FacilityReservationTbl reservation,
                                        Map<Integer, String> facilityNameCache,
                                        Map<String, String> userNameCache) {

        String facilityName = facilityNameCache.computeIfAbsent(

            reservation.getFacilityIdx(),
            idx -> facilityRepository.findById(idx)

                .map(FacilityTbl::getFacilityName)
                .orElse("Unknown")
        );



        String userName = userNameCache.computeIfAbsent(

            reservation.getUserCode(),
            code -> userRepository.findByUserCode(code)

                .map(UserTbl::getUserName)
                .orElse("Unknown")
        );
        return new ReservationDto(
            reservation.getReservationIdx(),
            reservation.getFacilityIdx(),
            facilityName,
            reservation.getUserCode(),
            userName,
            reservation.getStartTime(),
            reservation.getEndTime(),
            reservation.getPartySize(),
            reservation.getPurpose(),
            reservation.getRequestedEquipment(),
            reservation.getStatusEnum(),
            reservation.getAdminNote(),
            reservation.getRejectionReason(),
            reservation.getApprovedBy(),
            reservation.getApprovedAt(),
            reservation.getCreatedAt()
        );

    }

}

