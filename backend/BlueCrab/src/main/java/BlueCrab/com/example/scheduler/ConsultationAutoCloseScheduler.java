package BlueCrab.com.example.scheduler;

import BlueCrab.com.example.service.ConsultationRequestService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 상담 자동 종료 스케줄러
 * 진행 중인 상담의 비활성 상태 및 장시간 실행 여부를 모니터링하여 자동으로 종료 처리합니다.
 * 
 * 자동 종료 조건:
 * - 2시간 비활성: 마지막 활동 이후 2시간 경과
 * - 24시간 제한: 상담 시작 이후 24시간 경과
 * 
 * 실행 주기:
 * - 비활성 체크: 매 1시간마다
 * - 장시간 체크: 매일 오전 5시
 * 
 * @author BlueCrab Development Team
 * @version 1.0.0
 * @since 2025-10-24
 */
@Slf4j
@Component
public class ConsultationAutoCloseScheduler {

    private final ConsultationRequestService consultationRequestService;

    public ConsultationAutoCloseScheduler(ConsultationRequestService consultationRequestService) {
        this.consultationRequestService = consultationRequestService;
    }

    /**
     * 비활성 상담 자동 종료
     * 2시간 동안 활동이 없는 상담(IN_PROGRESS 상태)을 자동으로 종료합니다.
     * 
     * 실행 주기: 매 1시간마다 (정각)
     * Cron: "0 0 * * * *" (00:00, 01:00, 02:00, ..., 23:00)
     * 
     * 종료 조건: last_activity_at이 2시간 이전
     * 
     * 종료 처리:
     * 1. consultation_status를 COMPLETED로 변경
     * 2. ended_at 기록
     * 3. duration_minutes 계산
     * 4. Redis 메시지를 MinIO archive/에 저장
     * 5. Redis 키 삭제
     */
    @Scheduled(cron = "0 0 * * * *")
    public void autoEndInactiveConsultations() {
        log.info("[스케줄러] 비활성 상담 자동 종료 시작");

        try {
            int count = consultationRequestService.autoEndInactiveConsultations();

            if (count > 0) {
                log.info("[스케줄러] 비활성 상담 자동 종료 완료: {}건", count);
            } else {
                log.debug("[스케줄러] 자동 종료 대상 없음 (비활성)");
            }
        } catch (Exception e) {
            log.error("[스케줄러] 비활성 상담 자동 종료 실패", e);
        }
    }

    /**
     * 장시간 실행 상담 자동 종료
     * 24시간 이상 진행 중인 상담(IN_PROGRESS 상태)을 강제로 종료합니다.
     * 
     * 실행 주기: 매일 오전 5시
     * Cron: "0 0 5 * * *" (서버 부하가 적은 새벽 시간대)
     * 
     * 종료 조건: started_at이 24시간 이전
     * 
     * 종료 처리:
     * 1. consultation_status를 COMPLETED로 변경
     * 2. ended_at 기록
     * 3. duration_minutes 계산
     * 4. Redis 메시지를 MinIO archive/에 저장
     * 5. Redis 키 삭제
     */
    @Scheduled(cron = "0 0 5 * * *")
    public void autoEndLongRunningConsultations() {
        log.info("[스케줄러] 장시간 실행 상담 자동 종료 시작");

        try {
            int count = consultationRequestService.autoEndLongRunningConsultations();

            if (count > 0) {
                log.warn("[스케줄러] 장시간 실행 상담 자동 종료 완료: {}건 (24시간 제한)", count);
            } else {
                log.debug("[스케줄러] 자동 종료 대상 없음 (장시간)");
            }
        } catch (Exception e) {
            log.error("[스케줄러] 장시간 실행 상담 자동 종료 실패", e);
        }
    }

    /**
     * 상담 종료 경고 알림 발송
     * 매 10분마다 실행되어 23시간 50분 이상 진행 중인 상담에 대해
     * 10분 후 자동 종료 경고를 발송합니다.
     * 
     * 실행 주기: 매 10분마다 (00:00, 00:10, 00:20, ..., 23:50)
     * 경고 조건: 상담 시작 후 23시간 50분 ~ 24시간 사이
     * 
     * NOTE: 실제 구현은 Phase 5 (FCM 푸시 알림)에서 추가될 예정입니다.
     */
    @Scheduled(cron = "0 */10 * * * *")
    public void sendAutoEndWarnings() {
        log.debug("[스케줄러] 상담 종료 경고 알림 체크 (미구현)");
        // TODO: Phase 5에서 FCM 푸시 알림 연동 시 구현
        // 1. 23시간 50분 ~ 24시간 사이의 상담 조회
        // 2. 참여자 FCM 토큰 조회
        // 3. 푸시 알림 발송
    }
}
