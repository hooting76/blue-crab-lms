// 작성자 : 성태준
// 게시글 통계 전용 컨트롤러

package BlueCrab.com.example.controller;

// ========== 임포트 구문 ==========

// ========== Spring Framework ==========
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

// ========== 로깅 ==========
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// ========== 프로젝트 내부 클래스 ==========
import BlueCrab.com.example.service.BoardService;


@RestController
@RequestMapping("/api/boards")
public class BoardStatisticsController {
    
    // ========== 로거 ==========
    private static final Logger logger = LoggerFactory.getLogger(BoardStatisticsController.class);

    // =========== 의존성 주입 ==========

    @Autowired
    private BoardService boardService;
    // 중요 : 실제 게시글 관련 기능이 이루어지는 곳은 BoardService

    // ========== 게시글 통계 기능 ==========

    // 활성화된 게시글 총 개수 조회
    @PostMapping("/count")
    // 활성화된 게시글 총 개수 조회를 위한 엔드포인트 매핑 (POST 방식)
    public ResponseEntity<?> getActiveBoardCount() {
        try {
            long count = boardService.getActiveBoardCount();
            return ResponseEntity.ok(count);
        } catch (Exception e) {
            logger.error("Error occurred while fetching board count: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body("Server error occurred while fetching board count");
        }
    // BoardService의 getActiveBoardCount 메서드를 호출하여 활성화된 게시글 총 개수 반환
    }

    // 특정 게시글 존재 여부 확인
    @PostMapping("/exists")
    // 특정 게시글 존재 여부 확인을 위한 엔드포인트 매핑 (POST 방식)
    public ResponseEntity<?> isBoardExists(@RequestBody java.util.Map<String, Integer> request) {
        // @RequestBody Map<String, Integer> request : POST 요청 본문에서 boardIdx를 추출
        // JSON 형태: {"boardIdx": 1}
        Integer boardIdx = request.get("boardIdx");
        
        // ...기존 코드...
        try {
            boolean exists = boardService.isBoardExists(boardIdx);
            return ResponseEntity.ok(exists);
        } catch (Exception e) {
            logger.error("Error occurred while checking board existence - ID: {}, error: {}", boardIdx, e.getMessage(), e);
            return ResponseEntity.internalServerError().body("Server error occurred while checking board existence");
        }
    // BoardService의 isBoardExists 메서드를 호출하여 특정 게시글 존재 여부 반환
    }

    // 코드 별 게시글 총 개수 조회
    @PostMapping("/count/bycode")
    // 특정 코드별 게시글 총 개수 조회를 위한 엔드포인트 매핑 (POST 방식)
    public ResponseEntity<?> getBoardCountByCode(@RequestBody java.util.Map<String, Integer> request) {
        // @RequestBody Map<String, Integer> request : POST 요청 본문에서 boardCode를 추출
        // JSON 형태: {"boardCode": 0}
        Integer boardCode = request.get("boardCode");
        
        // ...기존 코드...
        try {
            long count = boardService.getBoardCountByCode(boardCode);
            return ResponseEntity.ok(count);
        } catch (Exception e) {
            logger.error("Error occurred while fetching board count by code - boardCode: {}, error: {}", boardCode, e.getMessage(), e);
            return ResponseEntity.internalServerError().body("Server error occurred while fetching board count by code");
        }
    // BoardService의 getBoardCountByCode 메서드를 호출하여 특정 코드별 게시글 총 개수 반환
    }
}