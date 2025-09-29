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

    // ========== 게시글 열람 기능 ==========

    // 게시글 목록 조회 (페이징)
    @PostMapping("/list")
    // HTTP POST 요청을 처리하는 엔드포인트 매핑 (명확한 기능 구분: /api/boards/list)
    public ResponseEntity<?> getAllBoards(@RequestBody java.util.Map<String, Integer> request) {
        // @RequestBody Map<String, Integer> request : POST 요청 본문에서 페이지 정보를 받음
        // JSON 형태: {"page": 0, "size": 10}
        Integer page = request.getOrDefault("page", 0);
        Integer size = request.getOrDefault("size", 10);
        
        // ...기존 코드...
        try {
            Page<BoardTbl> result = boardService.getAllBoards(page, size);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            logger.error("Error occurred while fetching board list: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body("Server error occurred while fetching board list");
        }
    // BoardService의 getAllBoards 메서드를 호출하여 페이징된 게시글 목록 반환
    }

    // 특정 게시글 조회 + 조회수 증가
    @PostMapping("/detail")
    // 특정 게시글 상세 조회를 위한 엔드포인트 매핑 (POST 방식)
    public ResponseEntity<?> getBoardDetail (@RequestBody java.util.Map<String, Integer> request) {
        // @RequestBody Map<String, Integer> request : POST 요청 본문에서 boardIdx를 추출
        // JSON 형태: {"boardIdx": 1}
        Integer boardIdx = request.get("boardIdx");
        
        // ...기존 코드...
        
        Optional<BoardTbl> board = boardService.getBoardDetail(boardIdx);
        
        if (board.isPresent()) {
            return ResponseEntity.ok(board.get());
        } else {
            return ResponseEntity.notFound().build();
        }
    // BoardService의 getBoardDetail 메서드를 호출하여 해당 게시글 상세 정보 반환
    // Optional 처리를 통해 존재하지 않을 경우 404 반환, 존재하면 실제 객체 반환
    // BoardService의 getBoardDetail 메서드에는 조회수 증가 기능이 포함되어 있음
    }






    // ========== 게시글 종류(코드) 별 조회 ==========

    // 코드 별 게시글 조회 (페이징)
    @PostMapping("/bycode")
    // 특정 코드별 게시글 조회를 위한 엔드포인트 매핑 (POST 방식)
    public ResponseEntity<?> getBoardsByCode(@RequestBody java.util.Map<String, Integer> request) {
        // @RequestBody Map<String, Integer> request : POST 요청 본문에서 조회 정보를 받음
        // JSON 형태: {"boardCode": 0, "page": 0, "size": 10}
        Integer boardCode = request.get("boardCode");
        Integer page = request.getOrDefault("page", 0);
        Integer size = request.getOrDefault("size", 10);
        
        // ...기존 코드...
        try {
            Page<BoardTbl> result = boardService.getBoardsByCode(boardCode, page, size);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            logger.error("Error occurred while fetching boards by code - boardCode: {}, error: {}", boardCode, e.getMessage(), e);
            return ResponseEntity.internalServerError().body("Server error occurred while fetching boards by code");
        }
    // BoardService의 getBoardsByCode 메서드를 호출하여 특정 코드별 페이징된 게시글 목록 반환
    }


    
}
