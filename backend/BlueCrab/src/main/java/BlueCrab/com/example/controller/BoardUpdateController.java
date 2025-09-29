// 작성자 : 성태준
// 게시글 수정 및 삭제 전용 컨트롤러

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
import BlueCrab.com.example.service.BoardService;
import BlueCrab.com.example.util.JwtUtil;


@RestController
@RequestMapping("/api/boards")
public class BoardUpdateController {
    
    // ========== 로거 ==========
    private static final Logger logger = LoggerFactory.getLogger(BoardUpdateController.class);

    // =========== 의존성 주입 ==========

    @Autowired
    private BoardService boardService;
    // 중요 : 실제 게시글 관련 기능이 이루어지는 곳은 BoardService
    
    @Autowired
    private JwtUtil jwtUtil;
    // JWT 토큰 검증을 위한 유틸리티

    // ========== 게시글 수정 및 삭제 기능 ==========

    // 게시글 수정
    @PutMapping("/update/{boardIdx}")
    // 특정 게시글 수정을 위한 엔드포인트 매핑 (명확한 기능 구분: /api/boards/update/{boardIdx})
    public ResponseEntity<?> updateBoard(@PathVariable Integer boardIdx, 
                                @Valid @RequestBody BoardTbl updatedBoard, 
                                BindingResult bindingResult,
                                HttpServletRequest request) {
        
        logger.info("BoardUpdateController.updateBoard called - ID: {}, title: {}", 
                   boardIdx, updatedBoard.getBoardTitle());
        
        // JWT 토큰 수동 확인 (permitAll() 상황에서)
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            logger.warn("No JWT token provided for board update - ID: {}", boardIdx);
            return ResponseEntity.status(401)
                    .body(java.util.Map.of(
                        "success", false,
                        "message", "게시글 수정에는 로그인이 필요합니다. JWT 토큰을 제공해주세요.",
                        "errorCode", "AUTHENTICATION_REQUIRED"
                    ));
        }
        
        String jwtToken = authHeader.substring(7);
        logger.info("JWT token provided for board update: {}", jwtToken.substring(0, Math.min(20, jwtToken.length())) + "...");
        
        // JWT 토큰 검증
        boolean isValid = jwtUtil.isTokenValid(jwtToken);
        boolean isAccess = jwtUtil.isAccessToken(jwtToken);
        String tokenType = jwtUtil.extractTokenType(jwtToken);
        
        logger.info("JWT Token validation for update - Valid: {}, IsAccess: {}, Type: {}", isValid, isAccess, tokenType);
        
        if (!isValid) {
            logger.warn("JWT token is not valid for board update - ID: {}", boardIdx);
            return ResponseEntity.status(401)
                    .body(java.util.Map.of(
                        "success", false,
                        "message", "토큰이 만료되었거나 유효하지 않습니다. 다시 로그인해주세요.",
                        "errorCode", "INVALID_TOKEN"
                    ));
        }
        
        if (!isAccess) {
            logger.warn("JWT token is not an access token for update - Type: {}, ID: {}", tokenType, boardIdx);
            return ResponseEntity.status(401)
                    .body(java.util.Map.of(
                        "success", false,
                        "message", "액세스 토큰이 필요합니다. 올바른 토큰으로 다시 시도해주세요.",
                        "errorCode", "INVALID_TOKEN_TYPE"
                    ));
        }
        
        String userEmail = jwtUtil.extractUsername(jwtToken);
        logger.info("Valid JWT token for board update - user: {}, boardId: {}", userEmail, boardIdx);
        
        try {
            Optional<BoardTbl> result = boardService.updateBoard(boardIdx, updatedBoard, userEmail);
            // BoardService의 updateBoard 메서드를 호출하여 게시글 수정 시도 (작성자 본인 확인 포함)
            
            if (result.isPresent()) {
                logger.info("Board updated successfully - ID: {}, title: {}", boardIdx, result.get().getBoardTitle());
                return ResponseEntity.ok(result.get());
                // 게시글 수정 성공 - 200 OK와 함께 수정된 게시글 반환
            } else {
                logger.warn("Board update failed - ID: {}, user: {}", boardIdx, userEmail);
                return ResponseEntity.status(403)
                        .body(java.util.Map.of(
                            "success", false,
                            "message", "게시글을 수정할 권한이 없습니다. 작성자 본인만 수정할 수 있습니다.",
                            "errorCode", "UPDATE_PERMISSION_DENIED"
                        ));
            }
        } catch (Exception e) {
            logger.error("BoardUpdateController.updateBoard error - ID: {}, user: {}, error: {}", boardIdx, userEmail, e.getMessage(), e);
            return ResponseEntity.status(400)
                    .body(java.util.Map.of(
                        "success", false,
                        "message", e.getMessage(),
                        "errorCode", "BOARD_UPDATE_ERROR"
                    ));
        }
    }

    // 게시글 삭제 (비활성화)
    @DeleteMapping("/delete/{boardIdx}")
    // 특정 게시글 삭제를 위한 엔드포인트 매핑 (명확한 기능 구분: /api/boards/delete/{boardIdx})
    public ResponseEntity<?> deleteBoard(@PathVariable Integer boardIdx, HttpServletRequest request) {
        
        logger.info("BoardUpdateController.deleteBoard called - ID: {}", boardIdx);
        
        // JWT 토큰 수동 확인 (permitAll() 상황에서)
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            logger.warn("No JWT token provided for board deletion - ID: {}", boardIdx);
            return ResponseEntity.status(401)
                    .body(java.util.Map.of(
                        "success", false,
                        "message", "게시글 삭제에는 로그인이 필요합니다. JWT 토큰을 제공해주세요.",
                        "errorCode", "AUTHENTICATION_REQUIRED"
                    ));
        }
        
        String jwtToken = authHeader.substring(7);
        logger.info("JWT token provided for board deletion: {}", jwtToken.substring(0, Math.min(20, jwtToken.length())) + "...");
        
        // JWT 토큰 검증
        boolean isValid = jwtUtil.isTokenValid(jwtToken);
        boolean isAccess = jwtUtil.isAccessToken(jwtToken);
        String tokenType = jwtUtil.extractTokenType(jwtToken);
        
        logger.info("JWT Token validation for deletion - Valid: {}, IsAccess: {}, Type: {}", isValid, isAccess, tokenType);
        
        if (!isValid) {
            logger.warn("JWT token is not valid for board deletion - ID: {}", boardIdx);
            return ResponseEntity.status(401)
                    .body(java.util.Map.of(
                        "success", false,
                        "message", "토큰이 만료되었거나 유효하지 않습니다. 다시 로그인해주세요.",
                        "errorCode", "INVALID_TOKEN"
                    ));
        }
        
        if (!isAccess) {
            logger.warn("JWT token is not an access token for deletion - Type: {}, ID: {}", tokenType, boardIdx);
            return ResponseEntity.status(401)
                    .body(java.util.Map.of(
                        "success", false,
                        "message", "액세스 토큰이 필요합니다. 올바른 토큰으로 다시 시도해주세요.",
                        "errorCode", "INVALID_TOKEN_TYPE"
                    ));
        }
        
        String userEmail = jwtUtil.extractUsername(jwtToken);
        logger.info("Valid JWT token for board deletion - user: {}, boardId: {}", userEmail, boardIdx);
        
        try {
            boolean result = boardService.deleteBoard(boardIdx, userEmail);
            // BoardService의 deleteBoard 메서드를 호출하여 게시글 비활성화 처리 (작성자 본인 확인 포함)
            
            if (result) {
                logger.info("Board deleted successfully - ID: {}", boardIdx);
                return ResponseEntity.ok()
                        .body(java.util.Map.of(
                            "success", true,
                            "message", "게시글이 성공적으로 삭제되었습니다.",
                            "boardIdx", boardIdx
                        ));
            } else {
                logger.warn("Board deletion failed - ID: {}, user: {}", boardIdx, userEmail);
                return ResponseEntity.status(403)
                        .body(java.util.Map.of(
                            "success", false,
                            "message", "게시글을 삭제할 권한이 없거나 게시글을 찾을 수 없습니다.",
                            "errorCode", "DELETE_PERMISSION_DENIED"
                        ));
            }
        } catch (Exception e) {
            logger.error("BoardUpdateController.deleteBoard error - ID: {}, user: {}, error: {}", boardIdx, userEmail, e.getMessage(), e);
            return ResponseEntity.status(400)
                    .body(java.util.Map.of(
                        "success", false,
                        "message", e.getMessage(),
                        "errorCode", "BOARD_DELETE_ERROR"
                    ));
        }
    }
}