// 작성자 : 성태준
// 게시글 첨부파일 삭제 전용 서비스
// 파일 삭제 관련 비즈니스 로직 처리

package BlueCrab.com.example.service.Board.Attachment;

// ========== 임포트 구문 ==========

// ========== Java 표준 라이브러리 ==========
import java.util.List;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

// ========== Spring Framework ==========
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

// ========== 로깅 ==========
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// ========== 프로젝트 내부 클래스 ==========
import BlueCrab.com.example.entity.Board.Attachment.BoardAttachmentTbl;
import BlueCrab.com.example.repository.Board.Attachment.BoardAttachmentRepository;
import BlueCrab.com.example.util.MinIOFileUtil;

@Service
public class BoardAttachmentDeleteService {

    private static final Logger logger = LoggerFactory.getLogger(BoardAttachmentDeleteService.class);

    // ========== 의존성 주입 ==========
    
    @Autowired
    private MinIOFileUtil minIOFileUtil;
    
    @Autowired
    private BoardAttachmentRepository attachmentRepository;

    // ========== 논리적 삭제 메서드 ==========

    /**
     * 단일 첨부파일 삭제 (논리적 삭제)
     * @param attachmentIdx 삭제할 첨부파일 IDX
     * @return 삭제 성공 여부
     */
    public boolean deleteAttachment(Integer attachmentIdx) {
        
        logger.info("단일 첨부파일 삭제 시작 - 첨부파일 IDX: {}", attachmentIdx);
        
        try {
            Optional<BoardAttachmentTbl> optionalAttachment = attachmentRepository.findById(attachmentIdx);
            
            if (optionalAttachment.isPresent()) {
                BoardAttachmentTbl attachment = optionalAttachment.get();
                
                if (attachment.getIsActive() == 0) {
                    logger.warn("이미 삭제된 첨부파일 - 첨부파일 IDX: {}", attachmentIdx);
                    return false;
                }
                
                attachment.setIsActive(0);
                attachmentRepository.save(attachment);
                
                logger.info("단일 첨부파일 삭제 완료 - 첨부파일 IDX: {}", attachmentIdx);
                return true;
                
            } else {
                logger.warn("존재하지 않는 첨부파일 - 첨부파일 IDX: {}", attachmentIdx);
                return false;
            }
            
        } catch (Exception e) {
            logger.error("단일 첨부파일 삭제 실패 - 첨부파일 IDX: {}, 오류: {}", attachmentIdx, e.getMessage(), e);
            throw new RuntimeException("첨부파일 삭제 실패: " + e.getMessage(), e);
        }
    }

    /**
     * 다중 첨부파일 삭제
     * @param attachmentIds 삭제할 첨부파일 IDX 목록
     * @return 삭제 결과 정보
     */
    public Map<String, Object> deleteMultipleAttachments(List<Integer> attachmentIds) {
        
        logger.info("다중 첨부파일 삭제 시작 - 삭제 대상: {}개", attachmentIds.size());
        
        Map<String, Object> result = new HashMap<>();
        List<Integer> successIds = new ArrayList<>();
        List<Integer> failureIds = new ArrayList<>();
        
        for (Integer attachmentIdx : attachmentIds) {
            try {
                boolean deleteResult = deleteAttachment(attachmentIdx);
                
                if (deleteResult) {
                    successIds.add(attachmentIdx);
                } else {
                    failureIds.add(attachmentIdx);
                }
                
            } catch (Exception e) {
                logger.error("다중 삭제 중 오류 - 첨부파일 IDX: {}, 오류: {}", attachmentIdx, e.getMessage());
                failureIds.add(attachmentIdx);
            }
        }
        
        result.put("successCount", successIds.size());
        result.put("failureCount", failureIds.size());
        result.put("successIds", successIds);
        result.put("failureIds", failureIds);
        
        logger.info("다중 첨부파일 삭제 완료 - 성공: {}개, 실패: {}개", successIds.size(), failureIds.size());
        return result;
    }

    /**
     * 게시글별 모든 첨부파일 삭제
     * @param boardIdx 게시글 IDX
     * @return 삭제 결과 정보
     */
    public Map<String, Object> deleteAllAttachmentsByBoard(Integer boardIdx) {
        
        logger.info("게시글별 모든 첨부파일 삭제 시작 - 게시글 IDX: {}", boardIdx);
        
        try {
            List<BoardAttachmentTbl> activeAttachments = attachmentRepository.findByBoardIdxAndIsActive(boardIdx, 1);
            
            if (activeAttachments.isEmpty()) {
                Map<String, Object> result = new HashMap<>();
                result.put("deletedCount", 0);
                result.put("message", "삭제할 첨부파일이 없습니다.");
                return result;
            }
            
            int deletedCount = 0;
            for (BoardAttachmentTbl attachment : activeAttachments) {
                attachment.setIsActive(0);
                attachmentRepository.save(attachment);
                deletedCount++;
            }
            
            Map<String, Object> result = new HashMap<>();
            result.put("deletedCount", deletedCount);
            result.put("message", deletedCount + "개의 첨부파일이 삭제되었습니다.");
            
            logger.info("게시글별 모든 첨부파일 삭제 완료 - 게시글 IDX: {}, 삭제 수: {}", boardIdx, deletedCount);
            return result;
            
        } catch (Exception e) {
            logger.error("게시글별 첨부파일 삭제 실패 - 게시글 IDX: {}, 오류: {}", boardIdx, e.getMessage(), e);
            throw new RuntimeException("게시글 첨부파일 삭제 실패: " + e.getMessage(), e);
        }
    }

    // ========== 물리적 삭제 메서드 ==========

    /**
     * 물리적 첨부파일 삭제 (MinIO에서 실제 파일 삭제)
     * @param attachmentIdx 삭제할 첨부파일 IDX
     * @return 삭제 성공 여부
     */
    public boolean deleteAttachmentPhysically(Integer attachmentIdx) {
        
        logger.info("물리적 첨부파일 삭제 시작 - 첨부파일 IDX: {}", attachmentIdx);
        
        try {
            Optional<BoardAttachmentTbl> optionalAttachment = attachmentRepository.findById(attachmentIdx);
            
            if (optionalAttachment.isPresent()) {
                BoardAttachmentTbl attachment = optionalAttachment.get();
                
                // MinIO에서 파일 삭제
                minIOFileUtil.deleteFile(attachment.getFilePath());
                
                // 데이터베이스에서 완전 삭제
                attachmentRepository.delete(attachment);
                
                logger.info("물리적 첨부파일 삭제 완료 - 첨부파일 IDX: {}", attachmentIdx);
                return true;
                
            } else {
                logger.warn("존재하지 않는 첨부파일 - 첨부파일 IDX: {}", attachmentIdx);
                return false;
            }
            
        } catch (Exception e) {
            logger.error("물리적 첨부파일 삭제 실패 - 첨부파일 IDX: {}, 오류: {}", attachmentIdx, e.getMessage(), e);
            throw new RuntimeException("물리적 첨부파일 삭제 실패: " + e.getMessage(), e);
        }
    }
}