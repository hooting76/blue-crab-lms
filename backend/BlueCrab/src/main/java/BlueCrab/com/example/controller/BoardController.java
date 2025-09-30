// 작성자 : 성태준
// 게시판 열람 전용 컨트롤러 (목록 조회, 상세 조회, 코드별 조회)

package BlueCrab.com.example.controller;

// ========== 임포트 구문 ==========

// ========== Java 표준 라이브러리 ==========
import java.util.Optional;

// ========== Spring Framework ==========
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

// ========== 로깅 ==========
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// ========== 프로젝트 내부 클래스 ==========
import BlueCrab.com.example.entity.BoardTbl;
import BlueCrab.com.example.service.BoardService;


@RestController
@RequestMapping("/api/boards")
public class BoardController {
    
    // ========== 로거 ==========
    private static final Logger logger = LoggerFactory.getLogger(BoardController.class);

    // =========== 의존성 주입 ==========

    @Autowired
    private BoardService boardService;
    // 중요 : 실제 게시글 관련 기능이 이루어지는 곳은 BoardService

    // ========== 게시글 목록 조회 기능 (성능 최적화 버전) ==========

    // 전체 게시글 목록 조회 (본문 제외, 성능 최적화용)
    @PostMapping("/list")
    public ResponseEntity<?> getAllBoards(@RequestBody java.util.Map<String, Integer> request) {
        Integer page = request.getOrDefault("page", 0);
        Integer size = request.getOrDefault("size", 10);
        
        try {
            Page<java.util.Map<String, Object>> result = boardService.getAllBoardsForList(page, size);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            logger.error("Error occurred while fetching board list: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body("Server error occurred while fetching board list");
        }
    }   // getAllBoards 끝

    // 코드별 게시글 목록 조회 (본문 제외, 성능 최적화용)
    @PostMapping("/bycode")
    public ResponseEntity<?> getBoardsByCode(@RequestBody java.util.Map<String, Integer> request) {
        Integer boardCode = request.get("boardCode");
        Integer page = request.getOrDefault("page", 0);
        Integer size = request.getOrDefault("size", 10);
        
        try {
            Page<java.util.Map<String, Object>> result = boardService.getBoardsByCodeForList(boardCode, page, size);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            logger.error("Error occurred while fetching boards by code - boardCode: {}, error: {}", boardCode, e.getMessage(), e);
            return ResponseEntity.internalServerError().body("Server error occurred while fetching boards by code");
        }
    }   // getBoardsByCode 끝

    // ========== 게시글 상세 조회 기능 ==========

    // 특정 게시글 상세 조회 + 조회수 증가 (본문 포함)
    @PostMapping("/detail")
    public ResponseEntity<?> getBoardDetail (@RequestBody java.util.Map<String, Integer> request) {
        Integer boardIdx = request.get("boardIdx");
        
        Optional<BoardTbl> board = boardService.getBoardDetail(boardIdx);
        
        if (board.isPresent()) {
            return ResponseEntity.ok(board.get());
        } else {
            return ResponseEntity.notFound().build();
        }
    }   // getBoardDetail 끝


    
}
