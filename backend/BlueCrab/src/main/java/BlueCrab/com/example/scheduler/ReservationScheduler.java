package BlueCrab.com.example.scheduler;

import BlueCrab.com.example.config.ReservationPolicyProperties;
import BlueCrab.com.example.entity.FacilityReservationLog;
import BlueCrab.com.example.entity.FacilityReservationTbl;
import BlueCrab.com.example.enums.ReservationStatus;
import BlueCrab.com.example.repository.FacilityReservationLogRepository;
import BlueCrab.com.example.repository.FacilityReservationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 시설 예약 자동 처리를 위한 스케줄러
 */
@Component
public class ReservationScheduler {

    private static final Logger logger = LoggerFactory.getLogger(ReservationScheduler.class);

    private final FacilityReservationRepository reservationRepository;
    private final FacilityReservationLogRepository logRepository;
    private final ReservationPolicyProperties policyProperties;

    public ReservationScheduler(FacilityReservationRepository reservationRepository,
                                 FacilityReservationLogRepository logRepository,
                                 ReservationPolicyProperties policyProperties) {
        this.reservationRepository = reservationRepository;
        this.logRepository = logRepository;
        this.policyProperties = policyProperties;
    }

    /**
     * 종료된 예약을 자동으로 완료 처리
     * 매 시간마다 실행
     */
    @Scheduled(cron = "0 0 * * * *")
    public void completeExpiredReservations() {
        logger.info("Starting auto-complete of expired reservations");

        LocalDateTime threshold = LocalDateTime.now()
            .minusHours(policyProperties.getAutoCompleteHours());

        List<FacilityReservationTbl> expiredReservations =
            reservationRepository.findExpiredApprovedReservations(threshold);

        int completedCount = 0;
        for (FacilityReservationTbl reservation : expiredReservations) {
            reservation.setStatusEnum(ReservationStatus.COMPLETED);
            reservationRepository.save(reservation);

            FacilityReservationLog log = new FacilityReservationLog(
                reservation.getReservationIdx(),
                "AUTO_COMPLETED",
                "SYSTEM",
                "SCHEDULER",
                null
            );
            logRepository.save(log);

            completedCount++;
        }

        if (completedCount > 0) {
            logger.info("Auto-completed {} expired reservations", completedCount);
        }
    }
}
