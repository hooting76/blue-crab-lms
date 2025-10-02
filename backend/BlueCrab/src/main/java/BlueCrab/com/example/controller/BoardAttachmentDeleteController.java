// 작성자 : 성태준
// 게시글 첨부파일 삭제 전용 컨트롤러
// 첨부파일 단일/다중 삭제 API 제공

package BlueCrab.com.example.controller;

// ========== 임포트 구문 ==========

// ========== Java 표준 라이브러리 ==========
import java.util.List;
import java.util.HashMap;
import java.util.Map;

// ========== Spring Framework ==========
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

// ========== Servlet ==========
import javax.servlet.http.HttpServletRequest;

// ========== 로깅 ==========
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// ========== 프로젝트 내부 클래스 ==========
import BlueCrab.com.example.util.JwtUtil;
import BlueCrab.com.example.service.BoardAttachmentDeleteService;

@RestController
@RequestMapping("/api/board-attachments")
@CrossOrigin(origins = "*")
public class BoardAttachmentDeleteController {

    private static final Logger logger = LoggerFactory.getLogger(BoardAttachmentDeleteController.class);

    // ========== 의존성 주입 ==========
    
    @Autowired
    private BoardAttachmentDeleteService deleteService;

    @Autowired
    private JwtUtil jwtUtil;

    // ========== 첨부파일 삭제 API ==========

    /**
     * 단일 첨부파일 삭제 API
     * POST /api/board-attachments/delete/{attachmentIdx}
     * @param attachmentIdx 삭제할 첨부파일 IDX
     * @param request HTTP 요청 (토큰 검증용)
     * @return 삭제 결과
     */
    @PostMapping("/delete/{attachmentIdx}")
    public ResponseEntity<Map<String, Object>> deleteAttachment(
            @PathVariable Integer attachmentIdx,
            HttpServletRequest request) {

        Map<String, Object> response = new HashMap<>();
        
        try {
            logger.info("단일 첨부파일 삭제 요청 - 첨부파일 IDX: {}", attachmentIdx);

            // JWT 토큰 검증
            String token = extractTokenFromRequest(request);
            if (token == null || !jwtUtil.validateToken(token)) {
                logger.warn("토큰 검증 실패 - 첨부파일 삭제 거부");
                response.put("success", false);
                response.put("message", "인증이 필요합니다.");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
            }

            // 첨부파일 삭제 처리
            boolean deleteResult = deleteService.deleteAttachment(attachmentIdx);

            if (deleteResult) {
                logger.info("단일 첨부파일 삭제 완료 - 첨부파일 IDX: {}", attachmentIdx);
                
                response.put("success", true);
                response.put("message", "첨부파일이 성공적으로 삭제되었습니다.");
                response.put("attachmentIdx", attachmentIdx);

                return ResponseEntity.ok(response);
            } else {
                response.put("success", false);
                response.put("message", "첨부파일을 찾을 수 없거나 이미 삭제되었습니다.");
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
            }

        } catch (Exception e) {
            logger.error("단일 첨부파일 삭제 실패 - 첨부파일 IDX: {}, 오류: {}", attachmentIdx, e.getMessage(), e);
            
            response.put("success", false);
            response.put("message", "첨부파일 삭제 중 오류가 발생했습니다: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * 다중 첨부파일 삭제 API
     * POST /api/board-attachments/delete-multiple
     * @param request HTTP 요청 (토큰 검증용)
     * @return 삭제 결과
     */
    @PostMapping("/delete-multiple")
    public ResponseEntity<Map<String, Object>> deleteMultipleAttachments(
            @RequestBody Map<String, Object> requestBody,
            HttpServletRequest request) {

        Map<String, Object> response = new HashMap<>();
        
        try {
            // attachmentIds 파라미터 추출
            @SuppressWarnings("unchecked")
            List<Integer> attachmentIds = (List<Integer>) requestBody.get("attachmentIds");
            
            if (attachmentIds == null || attachmentIds.isEmpty()) {
                response.put("success", false);
                response.put("message", "삭제할 첨부파일 ID 목록이 필요합니다.");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
            }

            logger.info("다중 첨부파일 삭제 요청 - 삭제 대상: {}개", attachmentIds.size());

            // JWT 토큰 검증
            String token = extractTokenFromRequest(request);
            if (token == null || !jwtUtil.validateToken(token)) {
                logger.warn("토큰 검증 실패 - 다중 첨부파일 삭제 거부");
                response.put("success", false);
                response.put("message", "인증이 필요합니다.");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
            }

            // 다중 첨부파일 삭제 처리
            Map<String, Object> deleteResults = deleteService.deleteMultipleAttachments(attachmentIds);

            logger.info("다중 첨부파일 삭제 완료 - 성공: {}개, 실패: {}개", 
                       deleteResults.get("successCount"), deleteResults.get("failureCount"));

            response.put("success", true);
            response.put("message", "다중 첨부파일 삭제가 완료되었습니다.");
            response.putAll(deleteResults);  // 상세 결과 포함

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("다중 첨부파일 삭제 실패 - 오류: {}", e.getMessage(), e);
            
            response.put("success", false);
            response.put("message", "다중 첨부파일 삭제 중 오류가 발생했습니다: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * 게시글별 모든 첨부파일 삭제 API
     * POST /api/board-attachments/delete-all/{boardIdx}
     * @param boardIdx 게시글 IDX
     * @param request HTTP 요청 (토큰 검증용)
     * @return 삭제 결과
     */
    @PostMapping("/delete-all/{boardIdx}")
    public ResponseEntity<Map<String, Object>> deleteAllAttachmentsByBoard(
            @PathVariable Integer boardIdx,
            HttpServletRequest request) {

        Map<String, Object> response = new HashMap<>();
        
        try {
            logger.info("게시글별 모든 첨부파일 삭제 요청 - 게시글 IDX: {}", boardIdx);

            // JWT 토큰 검증
            String token = extractTokenFromRequest(request);
            if (token == null || !jwtUtil.validateToken(token)) {
                logger.warn("토큰 검증 실패 - 게시글별 첨부파일 삭제 거부");
                response.put("success", false);
                response.put("message", "인증이 필요합니다.");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
            }

            // 게시글별 모든 첨부파일 삭제 처리
            Map<String, Object> deleteResults = deleteService.deleteAllAttachmentsByBoard(boardIdx);

            logger.info("게시글별 모든 첨부파일 삭제 완료 - 게시글 IDX: {}, 삭제 수: {}", 
                       boardIdx, deleteResults.get("deletedCount"));

            response.put("success", true);
            response.put("message", "게시글의 모든 첨부파일이 삭제되었습니다.");
            response.put("boardIdx", boardIdx);
            response.putAll(deleteResults);  // 상세 결과 포함

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("게시글별 첨부파일 삭제 실패 - 게시글 IDX: {}, 오류: {}", boardIdx, e.getMessage(), e);
            
            response.put("success", false);
            response.put("message", "게시글 첨부파일 삭제 중 오류가 발생했습니다: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    // ========== 유틸리티 메서드 ==========

    /**
     * HTTP 요청에서 JWT 토큰 추출
     * @param request HTTP 요청
     * @return JWT 토큰 (없으면 null)
     */
    private String extractTokenFromRequest(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }
        return null;
    }
}