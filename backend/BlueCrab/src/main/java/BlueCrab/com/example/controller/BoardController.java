// 작성자 : 성태준
// 게시판 열람 전용 컨트롤러 (목록 조회, 상세 조회, 코드별 조회)

package BlueCrab.com.example.controller;

// ========== 임포트 구문 ==========

// ========== Java 표준 라이브러리 ==========
import java.util.List;
import java.util.Optional;

// ========== Spring Framework ==========
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

// ========== Validation ==========
import javax.validation.Valid;

// ========== Servlet ==========
import javax.servlet.http.HttpServletRequest;

// ========== 로깅 ==========
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// ========== 프로젝트 내부 클래스 ==========
import BlueCrab.com.example.entity.BoardTbl;
import BlueCrab.com.example.entity.BoardAttachmentTbl;
import BlueCrab.com.example.service.BoardService;
import BlueCrab.com.example.service.BoardAttachmentQueryService;
import BlueCrab.com.example.dto.AttachmentLinkRequest;
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
    private BoardAttachmentQueryService boardAttachmentService;
    // 첨부파일 관련 서비스

    @Autowired
    private JwtUtil jwtUtil;
    // JWT 토큰 검증을 위한 유틸리티

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
        
        logger.info("게시글 상세 조회 요청 - boardIdx: {}", boardIdx);
        
        Optional<BoardTbl> boardOptional = boardService.getBoardDetail(boardIdx);
        
        if (boardOptional.isPresent()) {
            BoardTbl board = boardOptional.get();
            
            // 첨부파일 목록 조회하여 board 객체에 설정
            List<BoardAttachmentTbl> attachments = boardAttachmentService.getActiveAttachmentsByBoardId(boardIdx);
            board.setAttachmentDetails(attachments);
            
            logger.info("게시글 상세 조회 완료 - boardIdx: {}, 첨부파일 수: {}", 
                       boardIdx, attachments.size());
            
            return ResponseEntity.ok(board);
        } else {
            logger.warn("게시글을 찾을 수 없음 - boardIdx: {}", boardIdx);
            return ResponseEntity.notFound().build();
        }
    }   // getBoardDetail 끝

    // ========== 첨부파일 연결 기능 ==========

    // 게시글에 첨부파일 IDX 목록 연결 (게시글 작성 후 호출)
    @PostMapping("/link-attachments/{boardIdx}")
    public ResponseEntity<?> linkAttachments(
            @PathVariable Integer boardIdx,
            @Valid @RequestBody AttachmentLinkRequest request,
            HttpServletRequest httpRequest) {
        
        logger.info("첨부파일 연결 요청 - boardIdx: {}, attachmentIdx: {}", 
                   boardIdx, request.getAttachmentIdx());
        
        // JWT 토큰 검증
        String authHeader = httpRequest.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            logger.warn("JWT 토큰이 제공되지 않음 - 첨부파일 연결 거부");
            return ResponseEntity.status(401)
                    .body(java.util.Map.of(
                        "success", false,
                        "message", "첨부파일 연결에는 로그인이 필요합니다.",
                        "errorCode", "AUTHENTICATION_REQUIRED"
                    ));
        }
        
        String jwtToken = authHeader.substring(7);
        if (!jwtUtil.isTokenValid(jwtToken) || !jwtUtil.isAccessToken(jwtToken)) {
            logger.warn("유효하지 않은 JWT 토큰 - 첨부파일 연결 거부");
            return ResponseEntity.status(401)
                    .body(java.util.Map.of(
                        "success", false,
                        "message", "유효하지 않은 토큰입니다.",
                        "errorCode", "INVALID_TOKEN"
                    ));
        }
        
        try {
            // 1. 게시글 존재 여부 확인
            Optional<BoardTbl> boardOpt = boardService.findById(boardIdx);
            if (!boardOpt.isPresent()) {
                logger.warn("게시글을 찾을 수 없음 - boardIdx: {}", boardIdx);
                return ResponseEntity.badRequest().body("게시글을 찾을 수 없습니다.");
            }
            
            // 2. 첨부파일 IDX 목록이 유효한지 확인
            if (request.isEmpty()) {
                logger.warn("첨부파일 IDX 목록이 비어있음 - boardIdx: {}", boardIdx);
                return ResponseEntity.badRequest().body("첨부파일 IDX 목록이 필요합니다.");
            }
            
            // 3. 게시글에 첨부파일 IDX 설정
            BoardTbl board = boardOpt.get();
            board.setAttachmentIdx(request.getAttachmentIdx());
            
            // 4. 업데이트된 게시글 저장
            boardService.updateBoard(board);
            
            logger.info("첨부파일 연결 성공 - boardIdx: {}, attachmentCount: {}", 
                       boardIdx, request.getAttachmentCount());
            
            return ResponseEntity.ok().body(java.util.Map.of(
                "success", true,
                "message", "첨부파일이 성공적으로 연결되었습니다.",
                "boardIdx", boardIdx,
                "attachmentCount", request.getAttachmentCount(),
                "attachmentIdx", request.getAttachmentIdx()
            ));
            
        } catch (Exception e) {
            logger.error("첨부파일 연결 중 오류 발생 - boardIdx: {}, error: {}", 
                        boardIdx, e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body("첨부파일 연결 중 서버 오류가 발생했습니다.");
        }
    }   // linkAttachments 끝


    
}
