// 작성자 : 성태준
// 게시글 첨부파일 업로드 전용 컨트롤러
// 첨부파일 업로드 API만 제공

package BlueCrab.com.example.controller.Board.Attachment;

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
import org.springframework.web.multipart.MultipartFile;

// ========== Servlet ==========
import javax.servlet.http.HttpServletRequest;

// ========== 로깅 ==========
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// ========== 프로젝트 내부 클래스 ==========
import BlueCrab.com.example.util.JwtUtil;
import BlueCrab.com.example.service.Board.Attachment.BoardAttachmentUploadService;
import BlueCrab.com.example.entity.Board.Attachment.BoardAttachmentTbl;

@RestController
@RequestMapping("/api/board-attachments")
@CrossOrigin(origins = "*")
public class BoardAttachmentUploadController {

    private static final Logger logger = LoggerFactory.getLogger(BoardAttachmentUploadController.class);

    // ========== 의존성 주입 ==========
    
    @Autowired
    private BoardAttachmentUploadService uploadService;

    @Autowired
    private JwtUtil jwtUtil;

    // ========== 첨부파일 업로드 API ==========

    /* 첨부파일 업로드 API
     * POST /api/board-attachments/upload/{boardIdx}
     * @param boardIdx 게시글 IDX
     * @param files 업로드할 파일들 (단일 파일: 1개, 다중 파일: 여러개)
     * @param request HTTP 요청 (토큰 검증용)
     * @return 업로드 결과 (항상 배열 형태로 응답)
     */
    @PostMapping("/upload/{boardIdx}")
    public ResponseEntity<Map<String, Object>> uploadFiles(
            @PathVariable Integer boardIdx,
            @RequestParam("files") List<MultipartFile> files,
            HttpServletRequest request) {

        Map<String, Object> response = new HashMap<>();
        
        try {
            logger.info("첨부파일 업로드 요청 - 게시글 IDX: {}, 파일 수: {}", boardIdx, files.size());

            // JWT 토큰 검증
            String token = extractTokenFromRequest(request);
            if (token == null || !jwtUtil.validateToken(token)) {
                logger.warn("토큰 검증 실패 - 첨부파일 업로드 거부");
                response.put("success", false);
                response.put("message", "인증이 필요합니다.");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
            }

            // 첨부파일 업로드 처리 (Service 구현 완료)
            List<BoardAttachmentTbl> uploadedAttachments = uploadService.uploadFiles(boardIdx, files);

            logger.info("첨부파일 업로드 완료 - 게시글 IDX: {}, 성공 파일 수: {}", 
                       boardIdx, uploadedAttachments.size());

            response.put("success", true);
            response.put("message", "첨부파일 업로드가 완료되었습니다.");
            response.put("boardIdx", boardIdx);
            response.put("fileCount", uploadedAttachments.size());
            response.put("attachments", uploadedAttachments);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("첨부파일 업로드 실패 - 게시글 IDX: {}, 오류: {}", boardIdx, e.getMessage(), e);
            
            response.put("success", false);
            response.put("message", "첨부파일 업로드 중 오류가 발생했습니다: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    // ========== 유틸리티 메서드 ==========

    /* HTTP 요청에서 JWT 토큰 추출
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