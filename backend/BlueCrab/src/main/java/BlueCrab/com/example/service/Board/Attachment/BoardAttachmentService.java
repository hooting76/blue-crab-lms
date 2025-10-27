// 작성자 : 성태준
// 게시글 첨부파일 통합 서비스 (Facade Pattern)
// 업로드, 삭제, 조회 서비스들을 통합하여 단일 진입점 제공

package BlueCrab.com.example.service.Board.Attachment;

// ========== 임포트 구문 ==========

// ========== Java 표준 라이브러리 ==========
import java.util.List;
import java.util.Map;

// ========== Spring Framework ==========
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

// ========== 로깅 ==========
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// ========== 프로젝트 내부 클래스 ==========
import BlueCrab.com.example.entity.Board.Attachment.BoardAttachmentTbl;

@Service
public class BoardAttachmentService {

    private static final Logger logger = LoggerFactory.getLogger(BoardAttachmentService.class);

    // ========== 의존성 주입 - 분리된 서비스들 ==========
    
    @Autowired
    private BoardAttachmentUploadService uploadService;
    
    @Autowired
    private BoardAttachmentDeleteService deleteService;
    
    @Autowired
    private BoardAttachmentQueryService queryService;

    // ========== 파일 업로드 메서드 (Facade) ==========

    /**
     * 다중 파일 업로드 처리
     * @param boardIdx 게시글 IDX
     * @param files 업로드할 파일들
     * @return 업로드된 첨부파일 엔티티 목록
     */
    public List<BoardAttachmentTbl> uploadFiles(Integer boardIdx, List<MultipartFile> files) {
        logger.info("다중 파일 업로드 요청 - 게시글 IDX: {}, 파일 수: {}", boardIdx, files.size());
        return uploadService.uploadFiles(boardIdx, files);
    }

    /**
     * 단일 파일 업로드 처리
     * @param boardIdx 게시글 IDX
     * @param file 업로드할 파일
     * @return 업로드된 첨부파일 엔티티
     */
    public BoardAttachmentTbl uploadSingleFile(Integer boardIdx, MultipartFile file) {
        logger.info("단일 파일 업로드 요청 - 게시글 IDX: {}, 파일명: {}", boardIdx, file.getOriginalFilename());
        return uploadService.uploadSingleFile(boardIdx, file);
    }

    // ========== 파일 삭제 메서드 (Facade) ==========

    /**
     * 단일 첨부파일 삭제 (논리적 삭제)
     * @param attachmentIdx 삭제할 첨부파일 IDX
     * @return 삭제 성공 여부
     */
    public boolean deleteAttachment(Integer attachmentIdx) {
        logger.info("단일 첨부파일 삭제 요청 - 첨부파일 IDX: {}", attachmentIdx);
        return deleteService.deleteAttachment(attachmentIdx);
    }

    /**
     * 다중 첨부파일 삭제
     * @param attachmentIds 삭제할 첨부파일 IDX 목록
     * @return 삭제 결과 정보
     */
    public Map<String, Object> deleteMultipleAttachments(List<Integer> attachmentIds) {
        logger.info("다중 첨부파일 삭제 요청 - 삭제 대상: {}개", attachmentIds.size());
        return deleteService.deleteMultipleAttachments(attachmentIds);
    }

    /**
     * 게시글별 모든 첨부파일 삭제
     * @param boardIdx 게시글 IDX
     * @return 삭제 결과 정보
     */
    public Map<String, Object> deleteAllAttachmentsByBoard(Integer boardIdx) {
        logger.info("게시글별 모든 첨부파일 삭제 요청 - 게시글 IDX: {}", boardIdx);
        return deleteService.deleteAllAttachmentsByBoard(boardIdx);
    }

    /**
     * 물리적 첨부파일 삭제 (MinIO에서 실제 파일 삭제)
     * @param attachmentIdx 삭제할 첨부파일 IDX
     * @return 삭제 성공 여부
     */
    public boolean deleteAttachmentPhysically(Integer attachmentIdx) {
        logger.info("물리적 첨부파일 삭제 요청 - 첨부파일 IDX: {}", attachmentIdx);
        return deleteService.deleteAttachmentPhysically(attachmentIdx);
    }

    // ========== 파일 조회 메서드 (Facade) ==========

    /**
     * 게시글별 활성화된 첨부파일 목록 조회
     * @param boardIdx 게시글 IDX
     * @return 활성화된 첨부파일 목록
     */
    public List<BoardAttachmentTbl> getActiveAttachmentsByBoardId(Integer boardIdx) {
        logger.debug("게시글별 첨부파일 목록 조회 요청 - 게시글 IDX: {}", boardIdx);
        return queryService.getActiveAttachmentsByBoardId(boardIdx);
    }

    /**
     * 첨부파일 IDX로 단일 첨부파일 조회
     * @param attachmentIdx 첨부파일 IDX
     * @return 첨부파일 엔티티
     */
    public BoardAttachmentTbl getAttachmentById(Integer attachmentIdx) {
        logger.debug("첨부파일 단일 조회 요청 - 첨부파일 IDX: {}", attachmentIdx);
        return queryService.getAttachmentById(attachmentIdx);
    }

    /**
     * 게시글별 첨부파일 수 조회
     * @param boardIdx 게시글 IDX
     * @return 활성화된 첨부파일 수
     */
    public int getAttachmentCountByBoardId(Integer boardIdx) {
        logger.debug("게시글별 첨부파일 수 조회 요청 - 게시글 IDX: {}", boardIdx);
        return queryService.getAttachmentCountByBoardId(boardIdx);
    }
}
