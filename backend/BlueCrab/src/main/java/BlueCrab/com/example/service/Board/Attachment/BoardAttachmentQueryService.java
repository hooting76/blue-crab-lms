// 작성자 : 성태준
// 게시글 첨부파일 조회 전용 서비스
// 파일 조회 관련 비즈니스 로직 처리

package BlueCrab.com.example.service.Board.Attachment;

// ========== 임포트 구문 ==========

// ========== Java 표준 라이브러리 ==========
import java.util.List;

// ========== Spring Framework ==========
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

// ========== 로깅 ==========
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// ========== 프로젝트 내부 클래스 ==========
import BlueCrab.com.example.entity.Board.Attachment.BoardAttachmentTbl;
import BlueCrab.com.example.repository.Board.Attachment.BoardAttachmentRepository;

@Service
public class BoardAttachmentQueryService {

    private static final Logger logger = LoggerFactory.getLogger(BoardAttachmentQueryService.class);

    // ========== 의존성 주입 ==========
    
    @Autowired
    private BoardAttachmentRepository attachmentRepository;

    // ========== 첨부파일 조회 메서드 ==========

    /**
     * 게시글별 활성화된 첨부파일 목록 조회
     * @param boardIdx 게시글 IDX
     * @return 활성화된 첨부파일 목록
     */
    public List<BoardAttachmentTbl> getActiveAttachmentsByBoardId(Integer boardIdx) {
        
        logger.debug("게시글별 활성화된 첨부파일 목록 조회 - 게시글 IDX: {}", boardIdx);
        
        try {
            List<BoardAttachmentTbl> attachments = attachmentRepository.findByBoardIdxAndIsActive(boardIdx, 1);
            logger.info("게시글별 첨부파일 목록 조회 완료 - 게시글 IDX: {}, 첨부파일 수: {}", boardIdx, attachments.size());
            return attachments;
            
        } catch (Exception e) {
            logger.error("게시글별 첨부파일 목록 조회 실패 - 게시글 IDX: {}, 오류: {}", boardIdx, e.getMessage(), e);
            throw new RuntimeException("첨부파일 목록 조회 실패: " + e.getMessage(), e);
        }
    }

    /**
     * 첨부파일 IDX로 단일 첨부파일 조회
     * @param attachmentIdx 첨부파일 IDX
     * @return 첨부파일 엔티티 (Optional)
     */
    public BoardAttachmentTbl getAttachmentById(Integer attachmentIdx) {
        
        logger.debug("첨부파일 단일 조회 - 첨부파일 IDX: {}", attachmentIdx);
        
        try {
            return attachmentRepository.findById(attachmentIdx)
                    .orElseThrow(() -> new RuntimeException("첨부파일을 찾을 수 없습니다: " + attachmentIdx));
            
        } catch (Exception e) {
            logger.error("첨부파일 단일 조회 실패 - 첨부파일 IDX: {}, 오류: {}", attachmentIdx, e.getMessage(), e);
            throw new RuntimeException("첨부파일 조회 실패: " + e.getMessage(), e);
        }
    }

    /**
     * 게시글별 첨부파일 수 조회
     * @param boardIdx 게시글 IDX
     * @return 활성화된 첨부파일 수
     */
    public int getAttachmentCountByBoardId(Integer boardIdx) {
        
        logger.debug("게시글별 첨부파일 수 조회 - 게시글 IDX: {}", boardIdx);
        
        try {
            int count = attachmentRepository.countActiveAttachmentsByBoardIdx(boardIdx);
            logger.debug("첨부파일 수 조회 완료 - 게시글 IDX: {}, 첨부파일 수: {}", boardIdx, count);
            return count;
            
        } catch (Exception e) {
            logger.error("첨부파일 수 조회 실패 - 게시글 IDX: {}, 오류: {}", boardIdx, e.getMessage(), e);
            throw new RuntimeException("첨부파일 수 조회 실패: " + e.getMessage(), e);
        }
    }
}