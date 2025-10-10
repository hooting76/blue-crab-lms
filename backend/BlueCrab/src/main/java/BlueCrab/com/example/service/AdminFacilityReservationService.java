package BlueCrab.com.example.service;

import BlueCrab.com.example.dto.*;
import BlueCrab.com.example.entity.AdminTbl;
import BlueCrab.com.example.entity.FacilityReservationLog;
import BlueCrab.com.example.entity.FacilityReservationTbl;
import BlueCrab.com.example.entity.FacilityTbl;
import BlueCrab.com.example.entity.UserTbl;
import BlueCrab.com.example.enums.ReservationStatus;
import BlueCrab.com.example.exception.ResourceNotFoundException;
import BlueCrab.com.example.exception.UnauthorizedException;
import BlueCrab.com.example.repository.AdminTblRepository;
import BlueCrab.com.example.repository.FacilityReservationLogRepository;
import BlueCrab.com.example.repository.FacilityReservationRepository;
import BlueCrab.com.example.repository.FacilityRepository;
import BlueCrab.com.example.repository.UserTblRepository;
import BlueCrab.com.example.repository.projection.DashboardStatsProjection;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 관리자용 시설 예약 관리 서비스
 */
@Service
@Transactional
public class AdminFacilityReservationService {

    private static final Logger logger = LoggerFactory.getLogger(AdminFacilityReservationService.class);

    private final FacilityReservationRepository reservationRepository;
    private final FacilityRepository facilityRepository;
    private final UserTblRepository userRepository;
    private final AdminTblRepository adminRepository;
    private final FacilityReservationLogRepository logRepository;
    private final FacilityReservationService facilityReservationService;
    private final ObjectMapper objectMapper;

    public AdminFacilityReservationService(FacilityReservationRepository reservationRepository,
                                            FacilityRepository facilityRepository,
                                            UserTblRepository userRepository,
                                            AdminTblRepository adminRepository,
                                            FacilityReservationLogRepository logRepository,
                                            FacilityReservationService facilityReservationService,
                                            ObjectMapper objectMapper) {
        this.reservationRepository = reservationRepository;
        this.facilityRepository = facilityRepository;
        this.userRepository = userRepository;
        this.adminRepository = adminRepository;
        this.logRepository = logRepository;
        this.facilityReservationService = facilityReservationService;
        this.objectMapper = objectMapper;
    }

    public List<ReservationDto> getPendingReservations(String adminId) {
        validateAdmin(adminId);

        List<FacilityReservationTbl> reservations = reservationRepository.findByStatusOrderByCreatedAtDesc("PENDING");
        return convertToDtoList(reservations);
    }

    public ReservationDto approveReservation(String adminId, AdminApproveRequestDto request) {
        // ① 관리자 권한 검증
        validateAdmin(adminId);

        // ② 예약 조회 및 상태 확인
        FacilityReservationTbl reservation = reservationRepository.findById(request.getReservationIdx())
            .orElseThrow(() -> ResourceNotFoundException.forId("예약", request.getReservationIdx()));

        if (reservation.getStatusEnum() != ReservationStatus.PENDING) {
            throw new RuntimeException("대기 중인 예약만 승인할 수 있습니다.");
        }

        // ③ 비관적 락으로 시설 조회 - 동시 승인 시 동시성 제어
        // 두 관리자가 동시에 겹치는 시간대의 예약을 승인하는 것을 방지
        FacilityTbl facility = facilityRepository.findByIdWithLock(reservation.getFacilityIdx())
            .orElseThrow(() -> ResourceNotFoundException.forId("시설", reservation.getFacilityIdx()));

        if (!facility.getIsActive()) {
            throw new RuntimeException("비활성화된 시설 예약은 승인할 수 없습니다.");
        }

        // ④ 락을 획득한 상태에서 가용성 체크 (현재 예약 제외)
        FacilityAvailabilityDto availability = facilityReservationService.checkAvailabilityWithExclusion(
            reservation.getFacilityIdx(), 
            reservation.getStartTime(), 
            reservation.getEndTime(), 
            reservation.getReservationIdx());

        if (!Boolean.TRUE.equals(availability.getIsAvailable())) {
            List<FacilityAvailabilityDto.TimeSlot> conflicts = availability.getConflictingReservations();
            if (conflicts != null && !conflicts.isEmpty()) {
                FacilityAvailabilityDto.TimeSlot conflict = conflicts.get(0);
                throw new RuntimeException(String.format("이미 승인된 다른 예약과 시간이 겹칩니다. 충돌 시간: %s ~ %s",
                    conflict.getStartTime(), conflict.getEndTime()));
            }
            throw new RuntimeException("이미 승인된 다른 예약과 시간이 겹칩니다.");
        }

        logger.debug("Approving reservation: ID={}, Facility={}, Admin={}", 
            reservation.getReservationIdx(), facility.getFacilityName(), adminId);



        reservation.setStatusEnum(ReservationStatus.APPROVED);
        reservation.setApprovedBy(adminId);
        reservation.setApprovedAt(LocalDateTime.now());
        reservation.setAdminNote(request.getAdminNote());

        FacilityReservationTbl saved = reservationRepository.save(reservation);

        createLog(saved.getReservationIdx(), "APPROVED", "ADMIN", adminId, request);

        logger.info("Reservation approved: ID={}, Admin={}", saved.getReservationIdx(), adminId);
        return convertToDto(saved);
    }

    public ReservationDto rejectReservation(String adminId, AdminRejectRequestDto request) {
        validateAdmin(adminId);

        FacilityReservationTbl reservation = reservationRepository.findById(request.getReservationIdx())
            .orElseThrow(() -> ResourceNotFoundException.forId("예약", request.getReservationIdx()));

        if (reservation.getStatusEnum() != ReservationStatus.PENDING) {
            throw new RuntimeException("대기 중인 예약만 반려할 수 있습니다.");
        }

        reservation.setStatusEnum(ReservationStatus.REJECTED);
        reservation.setRejectionReason(request.getRejectionReason());

        FacilityReservationTbl saved = reservationRepository.save(reservation);

        createLog(saved.getReservationIdx(), "REJECTED", "ADMIN", adminId, request);

        logger.info("Reservation rejected: ID={}, Admin={}", saved.getReservationIdx(), adminId);
        return convertToDto(saved);
    }

    public DashboardStatsProjection getReservationStats(String adminId, LocalDateTime startDate, LocalDateTime endDate) {
        validateAdmin(adminId);

        if (startDate == null) {
            startDate = LocalDateTime.now().minusDays(30);
        }
        if (endDate == null) {
            endDate = LocalDateTime.now();
        }
        return reservationRepository.getReservationStats(startDate, endDate);
    }

    public long getPendingCount(String adminId) {
        validateAdmin(adminId);
        return reservationRepository.countPendingReservations();
    }

    /**
     * 관리자용 전체 예약 목록 조회 (필터링 지원)
     * @param adminId 관리자 ID
     * @param status 예약 상태 필터 (optional)
     * @param facilityIdx 시설 ID 필터 (optional)
     * @param startDate 시작 날짜 필터 (optional)
     * @param endDate 종료 날짜 필터 (optional)
     * @return 필터링된 예약 목록
     */
    public List<ReservationDto> getAllReservations(String adminId, String status, Integer facilityIdx,
                                                    LocalDateTime startDate, LocalDateTime endDate) {
        validateAdmin(adminId);

        List<FacilityReservationTbl> reservations;

        // 필터링 조건에 따라 쿼리 선택
        if (status != null && facilityIdx != null) {
            // 상태 + 시설 필터
            reservations = reservationRepository.findByStatusAndFacilityIdxOrderByCreatedAtDesc(status, facilityIdx);
        } else if (status != null) {
            // 상태 필터만
            reservations = reservationRepository.findByStatusOrderByCreatedAtDesc(status);
        } else if (facilityIdx != null) {
            // 시설 필터만
            reservations = reservationRepository.findByFacilityIdxOrderByCreatedAtDesc(facilityIdx);
        } else {
            // 필터 없음: 전체 조회
            reservations = reservationRepository.findAllOrderByCreatedAtDesc();
        }

        // 날짜 범위 필터링 (애플리케이션 레벨)
        if (startDate != null && endDate != null) {
            reservations = reservations.stream()
                .filter(r -> !r.getStartTime().isBefore(startDate) && !r.getEndTime().isAfter(endDate))
                .collect(Collectors.toList());
        } else if (startDate != null) {
            reservations = reservations.stream()
                .filter(r -> !r.getStartTime().isBefore(startDate))
                .collect(Collectors.toList());
        } else if (endDate != null) {
            reservations = reservations.stream()
                .filter(r -> !r.getEndTime().isAfter(endDate))
                .collect(Collectors.toList());
        }

        logger.info("Admin {} retrieved {} reservations with filters: status={}, facilityIdx={}, startDate={}, endDate={}",
            adminId, reservations.size(), status, facilityIdx, startDate, endDate);

        return convertToDtoList(reservations);
    }

    private void validateAdmin(String adminId) {
        AdminTbl admin = adminRepository.findByAdminId(adminId)
            .orElseThrow(() -> UnauthorizedException.forAdmin());
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

