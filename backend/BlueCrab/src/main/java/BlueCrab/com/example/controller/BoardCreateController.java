// 작성자 : 성태준
// 게시글 작성 전용 컨트롤러

package BlueCrab.com.example.controller;

// ========== 임포트 구문 ==========

// ========== Java 표준 라이브러리 ==========
import java.util.Optional;

// ========== Spring Framework ==========
import org.springframework.beans.factory.annotation.Autowired;
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
import BlueCrab.com.example.service.BoardManagementService;
import BlueCrab.com.example.util.JwtUtil;


@RestController
@RequestMapping("/api/boards")
public class BoardCreateController {
    
    // ========== 로거 ==========
    private static final Logger logger = LoggerFactory.getLogger(BoardCreateController.class);

    // =========== 의존성 주입 ==========

    @Autowired
    private BoardManagementService boardManagementService;
    // 게시글 작성, 수정, 삭제 관련 기능
    
    @Autowired
    private JwtUtil jwtUtil;
    // JWT 토큰 검증을 위한 유틸리티

    // ========== 게시글 작성 기능 ==========

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
        
        logger.info("BoardCreateController.createBoard called - title: {}, code: {}", 
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
            Optional<BoardTbl> result = boardManagementService.createBoard(boardTbl, userEmail);
            // BoardManagementService의 createBoard 메서드를 호출하여 게시글 생성해 DB에 저장
            
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
            logger.error("BoardCreateController.createBoard error: {}", e.getMessage(), e);
            return ResponseEntity.status(400)
                    .body(java.util.Map.of(
                        "success", false,
                        "message", e.getMessage(),
                        "errorCode", "BOARD_CREATION_ERROR"
                    ));
        }
    }
}