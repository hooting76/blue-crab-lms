package BlueCrab.com.example.service;

import BlueCrab.com.example.dto.*;
import BlueCrab.com.example.entity.ReadingSeat;
import BlueCrab.com.example.entity.ReadingUsageLog;
import BlueCrab.com.example.repository.ReadingSeatRepository;
import BlueCrab.com.example.repository.ReadingUsageLogRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 열람실 좌석 관리 서비스 클래스
 * 좌석 예약, 퇴실, 현황 조회 등의 비즈니스 로직을 담당
 *
 * 주요 기능:
 * - 좌석 현황 조회 및 실시간 업데이트
 * - 좌석 예약 및 검증
 * - 퇴실 처리 및 사용 기록 관리
 * - 만료 좌석 자동 정리
 * - 사용자별 이용 제한 관리
 *
 * 비즈니스 룰:
 * - 1인 1좌석 제한
 * - 기본 사용시간: 2시간
 * - 자동 퇴실: 시간 만료 시
 *
 * @author BlueCrab Development Team
 * @version 1.0.0
 * @since 2025-09-29
 */
@Service
@Transactional(readOnly = true)
public class ReadingRoomService {
    
    @Autowired
    private ReadingSeatRepository readingSeatRepository;
    
    @Autowired
    private ReadingUsageLogRepository readingUsageLogRepository;
    
    /**
     * 열람실 전체 현황을 조회합니다.
     * 만료된 좌석들을 자동으로 정리한 후 현황을 반환합니다.
     * 
     * @return 열람실 현황 정보
     */
    @Transactional
    public ReadingRoomStatusDto getReadingRoomStatus() {
        // 만료된 좌석들 자동 정리
        cleanupExpiredSeats();
        
        // 전체 좌석 조회
        List<ReadingSeat> allSeats = readingSeatRepository.findAllByOrderBySeatNumber();
        
        // DTO 변환
        List<ReadingSeatDto> seatDtos = allSeats.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
        
        // 통계 계산
        int totalSeats = allSeats.size();
        int occupiedSeats = (int) allSeats.stream().filter(seat -> seat.getIsOccupied() == 1).count();
        int availableSeats = totalSeats - occupiedSeats;
        
        return new ReadingRoomStatusDto(seatDtos, totalSeats, availableSeats, occupiedSeats);
    }
    
    /**
     * 좌석을 예약합니다.
     * 
     * @param seatNumber 예약할 좌석 번호
     * @param userCode 사용자 학번/교번
     * @return 예약 결과 정보
     * @throws IllegalStateException 예약 불가능한 경우
     */
    @Transactional
    public SeatReserveResponseDto reserveSeat(Integer seatNumber, String userCode) {
        // 1. 사용자 중복 예약 검증
        if (readingSeatRepository.existsByUserCodeAndIsOccupied(userCode, 1)) {
            throw new IllegalStateException("이미 다른 좌석을 사용 중입니다.");
        }
        
        // 2. 좌석 존재 및 사용 가능 여부 확인
        Optional<ReadingSeat> seatOpt = readingSeatRepository.findBySeatNumber(seatNumber);
        if (!seatOpt.isPresent()) {
            throw new IllegalArgumentException("존재하지 않는 좌석 번호입니다.");
        }
        
        ReadingSeat seat = seatOpt.get();
        if (seat.getIsOccupied() == 1) {
            throw new IllegalStateException("해당 좌석은 이미 다른 사용자가 예약했습니다.");
        }
        
        // 3. 좌석 예약 처리
        seat.reserve(userCode);
        readingSeatRepository.save(seat);
        
        // 4. 사용 기록 생성
        ReadingUsageLog usageLog = new ReadingUsageLog(seatNumber, userCode);
        readingUsageLogRepository.save(usageLog);
        
        return new SeatReserveResponseDto(seat.getSeatNumber(), seat.getStartTime(), seat.getEndTime());
    }
    
    /**
     * 퇴실 처리를 합니다.
     * 
     * @param seatNumber 퇴실할 좌석 번호
     * @param userCode 사용자 학번/교번
     * @return 퇴실 결과 정보
     * @throws IllegalStateException 퇴실 불가능한 경우
     */
    @Transactional
    public SeatCheckoutResponseDto checkoutSeat(Integer seatNumber, String userCode) {
        // 1. 좌석 존재 확인
        Optional<ReadingSeat> seatOpt = readingSeatRepository.findBySeatNumber(seatNumber);
        if (!seatOpt.isPresent()) {
            throw new IllegalArgumentException("존재하지 않는 좌석 번호입니다.");
        }
        
        ReadingSeat seat = seatOpt.get();
        
        // 2. 권한 검증 (본인 좌석인지 확인)
        if (seat.getIsOccupied() != 1 || !userCode.equals(seat.getUserCode())) {
            throw new IllegalStateException("본인이 예약한 좌석이 아닙니다.");
        }
        
        // 3. 사용 기록 완료 처리
        Optional<ReadingUsageLog> activeLogOpt = readingUsageLogRepository
                .findByUserCodeAndEndTimeIsNull(userCode);
        
        long usageTime = 0;
        if (activeLogOpt.isPresent()) {
            ReadingUsageLog activeLog = activeLogOpt.get();
            activeLog.checkout();
            readingUsageLogRepository.save(activeLog);
            usageTime = activeLog.getUsageTimeInMinutes();
        }
        
        // 4. 좌석 해제
        seat.release();
        readingSeatRepository.save(seat);
        
        return new SeatCheckoutResponseDto(seatNumber, usageTime);
    }
    
    /**
     * 특정 사용자의 현재 예약 좌석을 조회합니다.
     * 
     * @param userCode 사용자 학번/교번
     * @return 예약 좌석 정보, 예약이 없으면 null
     */
    public ReadingSeatDto getCurrentReservation(String userCode) {
        Optional<ReadingSeat> seatOpt = readingSeatRepository
                .findByUserCodeAndIsOccupied(userCode, 1);
        
        return seatOpt.map(this::convertToDto).orElse(null);
    }
    
    /**
     * 만료된 좌석들을 자동으로 정리합니다.
     * 스케줄러에서 주기적으로 호출되거나, 현황 조회 시 호출됩니다.
     */
    @Transactional
    public void cleanupExpiredSeats() {
        LocalDateTime now = LocalDateTime.now();
        List<ReadingSeat> expiredSeats = readingSeatRepository.findExpiredSeats(now);
        
        for (ReadingSeat seat : expiredSeats) {
            // 사용 기록 완료 처리
            Optional<ReadingUsageLog> activeLogOpt = readingUsageLogRepository
                    .findBySeatNumberAndEndTimeIsNull(seat.getSeatNumber());
            
            if (activeLogOpt.isPresent()) {
                ReadingUsageLog activeLog = activeLogOpt.get();
                activeLog.setEndTime(seat.getEndTime()); // 원래 종료 예정 시간으로 설정
                readingUsageLogRepository.save(activeLog);
            }
            
            // 좌석 해제
            seat.release();
        }
        
        if (!expiredSeats.isEmpty()) {
            readingSeatRepository.saveAll(expiredSeats);
        }
    }
    
    /**
     * 열람실 초기 데이터를 설정합니다.
     * 1~80번 좌석을 모두 빈 상태로 생성합니다.
     */
    @Transactional
    public void initializeSeats() {
        // 기존 좌석 수 확인
        long existingCount = readingSeatRepository.count();
        if (existingCount >= 80) {
            return; // 이미 초기화되어 있음
        }
        
        // 1~80번 좌석 생성
        for (int i = 1; i <= 80; i++) {
            if (!readingSeatRepository.existsById(i)) {
                ReadingSeat seat = new ReadingSeat(i);
                readingSeatRepository.save(seat);
            }
        }
    }
    
    /**
     * ReadingSeat 엔티티를 DTO로 변환합니다.
     * 
     * @param seat 좌석 엔티티
     * @return 좌석 DTO
     */
    private ReadingSeatDto convertToDto(ReadingSeat seat) {
        return new ReadingSeatDto(
                seat.getSeatNumber(),
                seat.getIsOccupied() == 1, // Integer를 Boolean으로 변환
                seat.getEndTime()
        );
    }
}