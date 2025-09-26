// 작성자 : 성태준
// 게시판 관련 통합 컨트롤러

package BlueCrab.com.example.controller;

// ========== 임포트 구문 ==========

// ========== Java 표준 라이브러리 ==========
import java.util.Optional;

// ========== Spring Framework ==========
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.validation.BindingResult;

// ========== Validation ==========
import javax.validation.Valid;

// ========== Servlet ==========
import javax.servlet.http.HttpServletRequest;

// ========== 로깅 ==========
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// ========== 프로젝트 내부 클래스 ==========
import BlueCrab.com.example.entity.BoardTbl;
import BlueCrab.com.example.service.BoardService;
import BlueCrab.com.example.util.JwtUtil;


@RestController
@RequestMapping("/api/boards")
public class BoardController {
    
    // ========== 로거 ==========
    private static final Logger logger = LoggerFactory.getLogger(BoardController.class);

    // =========== 의존성 주입 ==========

    @Autowired
    private BoardService boardService;
    // 중요 : 실제 게시글 관련 기능이 이루어지는 곳은 BoardService
    
    @Autowired
    private JwtUtil jwtUtil;
    // JWT 토큰 검증을 위한 유틸리티

    // ========== 기본 기능 ==========

    // 게시글 작성
    @PostMapping("/create")
    // HTTP POST 요청을 처리하는 엔드포인트 매핑 (명확한 기능 구분: /api/boards/create)
    public ResponseEntity<?> createBoard(@Valid @RequestBody BoardTbl boardTbl, 
                                       BindingResult bindingResult,
                                       HttpServletRequest request) {
    // @Valid : 입력 데이터 유효성 검사
    // @RequestBody BoardTbl boardTbl : 요청 본문에 포함된 JSON 데이터를 BoardTbl 객체로 매핑
    // BindingResult : 유효성 검사 결과를 담는 객체
    // HttpServletRequest request : JWT 토큰 추출을 위한 요청 객체
        
    logger.info("BoardController.createBoard called - title: {}, code: {}", 
           boardTbl.getBoardTitle(), boardTbl.getBoardCode());
        
        // JWT 토큰 수동 확인 (permitAll() 상황에서)
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            logger.warn("No JWT token provided for board creation");
            return ResponseEntity.status(401)
                    .body(java.util.Map.of(
                        "success", false,
                        "message", "게시글 작성에는 로그인이 필요합니다. JWT 토큰을 제공해주세요.",
                        "errorCode", "AUTHENTICATION_REQUIRED"
                    ));
        }
        
        String jwtToken = authHeader.substring(7);
        logger.info("JWT token provided for board creation: {}", jwtToken.substring(0, Math.min(20, jwtToken.length())) + "...");
        
        // JWT 토큰 검증 (디버깅을 위한 세분화)
        boolean isValid = jwtUtil.isTokenValid(jwtToken);
        boolean isAccess = jwtUtil.isAccessToken(jwtToken);
        String tokenType = jwtUtil.extractTokenType(jwtToken);
        
        logger.info("JWT Token validation - Valid: {}, IsAccess: {}, Type: {}", isValid, isAccess, tokenType);
        
        if (!isValid) {
            logger.warn("JWT token is not valid (expired or malformed)");
            return ResponseEntity.status(401)
                    .body(java.util.Map.of(
                        "success", false,
                        "message", "토큰이 만료되었거나 유효하지 않습니다. 다시 로그인해주세요.",
                        "errorCode", "INVALID_TOKEN"
                    ));
        }
        
        if (!isAccess) {
            logger.warn("JWT token is not an access token - Type: {}", tokenType);
            return ResponseEntity.status(401)
                    .body(java.util.Map.of(
                        "success", false,
                        "message", "액세스 토큰이 필요합니다. 올바른 토큰으로 다시 시도해주세요.",
                        "errorCode", "INVALID_TOKEN_TYPE"
                    ));
        }
        
        String userEmail = jwtUtil.extractUsername(jwtToken);
        logger.info("Valid JWT token for user: {}", userEmail);
        
        try {
            Optional<BoardTbl> result = boardService.createBoard(boardTbl, userEmail);
            // BoardService의 createBoard 메서드를 호출하여 게시글 생성해 DB에 저장
            
            logger.info("BoardService.createBoard finished - result: {}", result.isPresent() ? "success" : "fail");
            if (result.isPresent()) {
                logger.info("Board created successfully - ID: {}, title: {}", result.get().getBoardIdx(), result.get().getBoardTitle());
                return ResponseEntity.ok(result.get());
            } else {
                logger.warn("Board creation failed - no permission");
                return ResponseEntity.status(403)
                        .body(java.util.Map.of(
                            "success", false,
                            "message", "게시글 작성 권한이 없습니다. 관리자 또는 교수만 작성할 수 있습니다.",
                            "errorCode", "INSUFFICIENT_PERMISSION"
                        ));
            }
        } catch (Exception e) {
            logger.error("BoardController.createBoard error: {}", e.getMessage(), e);
            return ResponseEntity.status(400)
                    .body(java.util.Map.of(
                        "success", false,
                        "message", e.getMessage(),
                        "errorCode", "BOARD_CREATION_ERROR"
                    ));
        }
    }

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

    // 게시글 수정
    @PutMapping("/update/{boardIdx}")
    // 특정 게시글 수정을 위한 엔드포인트 매핑 (명확한 기능 구분: /api/boards/update/{boardIdx})
    public ResponseEntity<?> updateBoard(@PathVariable Integer boardIdx, 
                                @Valid @RequestBody BoardTbl updatedBoard, BindingResult bindingResult) {
        
        logger.info("BoardController.updateBoard called - ID: {}, title: {}", 
                   boardIdx, updatedBoard.getBoardTitle());
        // ...기존 코드...
        
        Optional<BoardTbl> result = boardService.updateBoard(boardIdx, updatedBoard);
    // BoardService의 updateBoard 메서드를 호출하여 게시글 수정 시도
        
        if (result.isPresent()) {
            return ResponseEntity.ok(result.get());
            // 게시글 수정 성공 - 200 OK와 함께 수정된 게시글 반환
        } else {
            return ResponseEntity.notFound().build();
            // 게시글 수정 실패 (게시글을 찾을 수 없음) - 404 Not Found 반환
        }
    }

    // 게시글 삭제 (비활성화)
    @DeleteMapping("/delete/{boardIdx}")
    // 특정 게시글 삭제를 위한 엔드포인트 매핑 (명확한 기능 구분: /api/boards/delete/{boardIdx})
    public ResponseEntity<?> deleteBoard(@PathVariable Integer boardIdx) {
        
        logger.info("BoardController.deleteBoard called - ID: {}", boardIdx);
        // ...기존 코드...
        try {
            boolean result = boardService.deleteBoard(boardIdx);
            if (result) {
                logger.info("Board deleted successfully - ID: {}", boardIdx);
                return ResponseEntity.ok(true);
            } else {
                logger.warn("Board deletion failed - ID: {}", boardIdx);
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            logger.error("Error occurred while deleting board - ID: {}, error: {}", boardIdx, e.getMessage(), e);
            return ResponseEntity.internalServerError().body("Server error occurred while deleting board");
        }
    // BoardService의 deleteBoard 메서드를 호출하여 게시글 비활성화 처리 후 성공 여부 반환
    }

    // ========== 기타 유틸리티 메서드 ==========

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
