package BlueCrab.com.example.controller;

import BlueCrab.com.example.entity.ReadingSeat;
import BlueCrab.com.example.repository.ReadingSeatRepository;
import BlueCrab.com.example.repository.ReadingUsageLogRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.HashMap;

/**
 * 데이터베이스 테이블 상태를 테스트하기 위한 임시 컨트롤러
 * 개발/디버깅 용도로만 사용
 */
@RestController
@RequestMapping("/api/test")
public class DatabaseTestController {
    
    @Autowired
    private ReadingSeatRepository readingSeatRepository;
    
    @Autowired  
    private ReadingUsageLogRepository readingUsageLogRepository;
    
    /**
     * LAMP_TBL 테이블 현재 상태 확인
     */
    @GetMapping("/lamp-table-status")
    public ResponseEntity<Map<String, Object>> getLampTableStatus() {
        try {
            // 전체 좌석 조회
            List<ReadingSeat> allSeats = readingSeatRepository.findAll();
            
            // 통계 계산
            long totalSeats = readingSeatRepository.count();
            long availableSeats = readingSeatRepository.countByIsOccupied(0);
            long occupiedSeats = readingSeatRepository.countByIsOccupied(1);
            
            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("message", "LAMP_TBL 테이블 접근 성공");
            response.put("totalSeats", totalSeats);
            response.put("availableSeats", availableSeats);
            response.put("occupiedSeats", occupiedSeats);
            response.put("sampleData", allSeats.size() > 5 ? allSeats.subList(0, 5) : allSeats);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("status", "error");
            errorResponse.put("message", "데이터베이스 오류: " + e.getMessage());
            errorResponse.put("error", e.getClass().getSimpleName());
            
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }
    
    /**
     * 테이블 구조 확인을 위한 단순 조회
     */
    @GetMapping("/lamp-table-simple")
    public ResponseEntity<Map<String, Object>> getSimpleTableTest() {
        try {
            long lampCount = readingSeatRepository.count();
            long logCount = readingUsageLogRepository.count();
            
            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("message", "데이터베이스 테이블 접근 성공");
            response.put("lampTableCount", lampCount);
            response.put("usageLogCount", logCount);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("status", "error");
            errorResponse.put("message", "데이터베이스 오류: " + e.getMessage());
            errorResponse.put("error", e.getClass().getSimpleName());
            errorResponse.put("stackTrace", e.getStackTrace()[0].toString());
            
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }
    
    /**
     * 좌석 초기화 테스트
     */
    @GetMapping("/initialize-seats")
    public ResponseEntity<Map<String, Object>> initializeSeats() {
        try {
            // 1~80번 좌석 생성
            int createdCount = 0;
            for (int i = 1; i <= 80; i++) {
                if (!readingSeatRepository.existsById(i)) {
                    ReadingSeat seat = new ReadingSeat(i);
                    readingSeatRepository.save(seat);
                    createdCount++;
                }
            }
            
            long totalSeats = readingSeatRepository.count();
            long availableSeats = readingSeatRepository.countByIsOccupied(0);
            long occupiedSeats = readingSeatRepository.countByIsOccupied(1);
            
            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("message", "좌석 초기화 완료");
            response.put("createdSeats", createdCount);
            response.put("totalSeats", totalSeats);
            response.put("availableSeats", availableSeats);
            response.put("occupiedSeats", occupiedSeats);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("status", "error");
            errorResponse.put("message", "좌석 초기화 오류: " + e.getMessage());
            errorResponse.put("error", e.getClass().getSimpleName());
            
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }
    
    /**
     * 모든 좌석 해제 (테스트용)
     */
    @GetMapping("/reset-all-seats")
    public ResponseEntity<Map<String, Object>> resetAllSeats() {
        try {
            List<ReadingSeat> allSeats = readingSeatRepository.findAll();
            for (ReadingSeat seat : allSeats) {
                seat.release();
            }
            readingSeatRepository.saveAll(allSeats);
            
            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("message", "모든 좌석 해제 완료");
            response.put("resetCount", allSeats.size());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("status", "error");
            errorResponse.put("message", "좌석 해제 오류: " + e.getMessage());
            errorResponse.put("error", e.getClass().getSimpleName());
            
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }
}