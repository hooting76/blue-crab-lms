package BlueCrab.com.example.config;

import BlueCrab.com.example.service.ReadingRoomService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 열람실 시스템 초기화 및 스케줄링 설정
 * 애플리케이션 시작 시 좌석 데이터 초기화 및 주기적인 정리 작업 수행
 *
 * 주요 기능:
 * - 애플리케이션 시작 시 좌석 초기 데이터 설정
 * - 주기적인 만료 좌석 자동 정리
 * - 시스템 상태 모니터링
 *
 * @author BlueCrab Development Team
 * @version 1.0.0
 * @since 2025-09-29
 */
@Component
@EnableScheduling
public class ReadingRoomConfig implements ApplicationRunner {
    
    private static final Logger logger = LoggerFactory.getLogger(ReadingRoomConfig.class);
    
    @Autowired
    private ReadingRoomService readingRoomService;
    
    /**
     * 애플리케이션 시작 시 초기화 작업 수행
     * 좌석 데이터가 없으면 1~80번 좌석을 생성
     *
     * @param args 애플리케이션 실행 인자
     */
    @Override
    public void run(ApplicationArguments args) {
        try {
            logger.info("열람실 시스템 초기화 시작");
            
            // 좌석 초기 데이터 설정
            readingRoomService.initializeSeats();
            
            // 만료된 좌석 정리 (애플리케이션 재시작 시)
            readingRoomService.cleanupExpiredSeats();
            
            logger.info("열람실 시스템 초기화 완료");
            
        } catch (Exception e) {
            logger.error("열람실 시스템 초기화 중 오류 발생", e);
        }
    }
    
    /**
     * 만료된 좌석들을 주기적으로 자동 정리
     * 매 5분마다 실행하여 시간이 지난 좌석들을 해제
     */
    @Scheduled(fixedRate = 300000) // 5분마다 실행 (300,000ms)
    public void cleanupExpiredSeatsScheduled() {
        try {
            logger.debug("만료 좌석 자동 정리 작업 시작");
            readingRoomService.cleanupExpiredSeats();
            logger.debug("만료 좌석 자동 정리 작업 완료");
            
        } catch (Exception e) {
            logger.error("만료 좌석 정리 중 오류 발생", e);
        }
    }
    
    /**
     * 시스템 상태를 주기적으로 로깅
     * 매 시간마다 현재 열람실 이용 현황을 로그에 기록
     */
    @Scheduled(cron = "0 0 * * * *") // 매 시간 정각에 실행
    public void logSystemStatus() {
        try {
            BlueCrab.com.example.dto.ReadingRoomStatusDto status = readingRoomService.getReadingRoomStatus();
            
            logger.info("열람실 현황 - 전체: {}석, 사용중: {}석, 사용가능: {}석, 사용률: {}%",
                    status.getTotalSeats(),
                    status.getOccupiedSeats(),
                    status.getAvailableSeats(),
                    status.getOccupancyRate());
            
        } catch (Exception e) {
            logger.error("시스템 상태 로깅 중 오류 발생", e);
        }
    }
}