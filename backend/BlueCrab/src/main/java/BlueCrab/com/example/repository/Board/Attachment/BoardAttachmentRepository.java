// 작성자 : 성태준
// 게시글 첨부파일 Repository
// 첨부파일 데이터베이스 CRUD 작업 - 업로드 기능 위주로 우선 구현

package BlueCrab.com.example.repository.Board.Attachment;

// ========== 임포트 구문 ==========

// ========== Java 표준 라이브러리 ==========
import java.util.List;
import java.util.Optional;

// ========== Spring Data JPA ==========
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

// ========== 프로젝트 내부 클래스 ==========
import BlueCrab.com.example.entity.Board.Attachment.BoardAttachmentTbl;

@Repository
public interface BoardAttachmentRepository extends JpaRepository<BoardAttachmentTbl, Integer> {

    // ========== 업로드 기능용 기본 조회 메서드 ==========

    /**
     * 게시글 IDX로 활성화된 첨부파일 목록 조회 (업로드 시 개수 확인용)
     * @param boardIdx 게시글 IDX
     * @param isActive 활성화 상태 (1: 활성, 0: 비활성)
     * @return 활성화된 첨부파일 목록
     */
    List<BoardAttachmentTbl> findByBoardIdxAndIsActive(Integer boardIdx, Integer isActive);

    /**
     * 게시글별 활성화된 첨부파일 수 조회 (업로드 제한 확인용)
     * @param boardIdx 게시글 IDX.
     * @return 첨부파일 수
     */
    @Query("SELECT COUNT(a) FROM BoardAttachmentTbl a WHERE a.boardIdx = :boardIdx AND a.isActive = 1")
    int countActiveAttachmentsByBoardIdx(@Param("boardIdx") Integer boardIdx);

    // ========== 삭제 기능용 메서드 ==========

    /**
     * 첨부파일 IDX로 활성화된 첨부파일 조회 (삭제 권한 확인용)
     * @param attachmentIdx 첨부파일 IDX
     * @param isActive 활성화 상태 (1: 활성, 0: 비활성)
     * @return 첨부파일 엔티티 (Optional)
     */
    @Query("SELECT a FROM BoardAttachmentTbl a WHERE a.attachmentIdx = :attachmentIdx AND a.isActive = :isActive")
    Optional<BoardAttachmentTbl> findByAttachmentIdxAndIsActive(@Param("attachmentIdx") Integer attachmentIdx, @Param("isActive") Integer isActive);

    /**
     * 게시글별 활성화된 첨부파일 일괄 비활성화 (논리적 삭제)
     * @param boardIdx 게시글 IDX
     * @return 업데이트된 레코드 수
     */
    @Query("UPDATE BoardAttachmentTbl a SET a.isActive = 0 WHERE a.boardIdx = :boardIdx AND a.isActive = 1")
    @Modifying
    int deactivateAllAttachmentsByBoardIdx(@Param("boardIdx") Integer boardIdx);

    /**
     * 만료된 첨부파일 조회 (배치 삭제용)
     * @param currentDate 현재 날짜 문자열 (yyyy-MM-dd HH:mm:ss 형식)
     * @return 만료된 첨부파일 목록
     */
    @Query("SELECT a FROM BoardAttachmentTbl a WHERE a.expiryDate < :currentDate AND a.isActive = 1")
    List<BoardAttachmentTbl> findExpiredAttachments(@Param("currentDate") String currentDate);
}