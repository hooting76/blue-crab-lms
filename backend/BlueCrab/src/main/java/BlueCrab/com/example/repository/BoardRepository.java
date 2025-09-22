// 작성자 : 성태준
// 게시글 테이블 Repository 인터페이스

package BlueCrab.com.example.repository;

// ========== 임포트 구문 ==========

// ========== Spring Data JPA ==========
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

// ========== Java 표준 라이브러리 ==========
import java.util.Optional;

// ========== 프로젝트 내부 클래스 ==========
import BlueCrab.com.example.entity.BoardTbl;

public interface BoardRepository extends JpaRepository<BoardTbl, Integer> {

    // ========== 커스텀 쿼리 메서드 ==========

    // ========== 조회 관련 메서드 ==========

    // 미삭제(활성화) 개시글 만을 조회(최신순)
    Page<BoardTbl> findByBoardOnOrderByBoardRegDesc(Integer boardOn, Pageable pageable);
    
    // 코드별 + 미삭제(활성화) 게시글 조회(최신순)
    Page<BoardTbl> findByBoardOnAndBoardCodeOrderByBoardRegDesc(Integer boardOn, Integer boardCode, Pageable pageable);

    // 조회수 증가 메서드
    @Modifying // 증가 작업임을 명시
    @Query("UPDATE BoardTbl b SET b.boardView = b.boardView + 1 WHERE b.boardIdx = :boardIdx")  // JPQL 쿼리
    void incrementBoardView(@Param("boardIdx") Integer boardIdx);   // 특정 게시글의 조회수 1 증가

    // 특정 게시글 상세 조회 (활성화된 것만)
    Optional<BoardTbl> findByBoardIdxAndBoardOn(Integer boardIdx, Integer boardOn);

    // ========== 검색 관련 메서드 ==========
    // 향후 필요시 활용 

    // 제목 검색 (활성화된 게시글만)
    Page<BoardTbl> findByBoardOnAndBoardTitleContainingOrderByBoardRegDesc(Integer boardOn, String keyword, Pageable pageable);
    // findByBoardOnAndBoardTitleContainingOrderByBoardRegDesc : Spring Data JPA의 메서드 네이밍 규칙을 활용한 쿼리 메서드
    // - findBy : 조회 메서드 시작
    // - BoardOn : boardOn 필드로 필터링 (활성화 여부)
    // - And : AND 조건 결합
    // - BoardTitleContaining : boardTitle 필드에 keyword가 포함된 레코드 조회 (부분 검색)
    // - OrderByBoardRegDesc : boardReg 필드 기준 내림차순 정렬
    // Integer boardOn : 활성화 여부 (1: 활성, 0: 비활성)
    // String keyword : 제목에 포함될 검색어
    // Pageable pageable : 페이징 및 정렬 정보

    // 작성자 검색
    Page<BoardTbl> findByBoardOnAndBoardWriterOrderByBoardRegDesc(Integer boardOn, String writer, Pageable pageable);
    // findByBoardOnAndBoardWriterOrderByBoardRegDesc : Spring Data JPA의 메서드 네이밍 규칙을 활용한 쿼리 메서드
    // - BoardWriter : boardWriter 필드로 필터링 (작성자)
    // String writer : 작성자
    // 나머지 파라미터 설명은 위와 동일

    // ========== 통계 관련 메서드 ==========

    // 미삭제(활성화) 게시글 총 개수
    long countByBoardOn(Integer boardOn);
    // countByBoardOn : Spring Data JPA의 메서드 네이밍 규칙을 활용한 카운트 메서드
    // Integer boardOn : 활성화 여부 (1: 활성, 0: 비활성)

    // 특정 종류(코드)의 미삭제(활성화) 게시글 개수
    long countByBoardOnAndBoardCode(Integer boardOn, Integer boardCode);
    // countByBoardOnAndBoardCode : Spring Data JPA의 메서드 네이밍 규칙을 활용한 카운트 메서드
    // Integer boardCode : 게시글 코드 (0: 학교공지, 1: 학사공지, 2: 학과공지, 3: 교수공지)

    // ========== 제어/관리 관련 메서드 ==========
    // 향후 필요시 작성


}