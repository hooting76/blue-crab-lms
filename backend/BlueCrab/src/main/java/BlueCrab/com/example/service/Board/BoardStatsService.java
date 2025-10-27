// 작성자 : 성태준
// 게시글 통계 서비스 (개수 조회, 존재 여부 확인 등)

package BlueCrab.com.example.service.Board;

// ========== 임포트 구문 ==========

// ========== Spring Framework ==========
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// ========== 로깅 ==========
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// ========== 프로젝트 내부 클래스 ==========
import BlueCrab.com.example.repository.Board.BoardRepository;


@Service        // 게시글 통계 서비스
@Transactional(readOnly = true)  // 통계는 읽기 전용으로만 사용
public class BoardStatsService {
    
    // ========== 로거 ==========
    private static final Logger logger = LoggerFactory.getLogger(BoardStatsService.class);

    // ========== 의존성 주입 ==========
    @Autowired
    private BoardRepository boardRepository;

    // 상수 정의
    private static final Integer BOARD_ACTIVE = 1; // 활성 상태 코드

    // ========== 게시글 통계 관련 메서드 ==========

    // 게시글의 총 개수 조회 (미삭제 게시글 만)
    public long getActiveBoardCount() {
        logger.debug("Fetching active board count");
        return boardRepository.countByBoardOn(BOARD_ACTIVE);
    }   // getActiveBoardCount 끝

    // 게시글의 존재 여부 확인 (미삭제 게시글 만)
    public boolean isBoardExists(Integer boardIdx) {
        logger.debug("Checking if board exists - boardIdx: {}", boardIdx);
        return boardRepository.findByBoardIdxAndBoardOn(boardIdx, BOARD_ACTIVE).isPresent();
    }   // isBoardExists 끝

    // 코드별 게시글 개수 조회 (미삭제 게시글 만)
    public long getBoardCountByCode(Integer boardCode) {
        logger.debug("Fetching board count by code - boardCode: {}", boardCode);
        return boardRepository.countByBoardOnAndBoardCode(BOARD_ACTIVE, boardCode);
        // 특정 코드의 미삭제 게시글 개수 조회
    }   // getBoardCountByCode 끝

    // ========== 확장 가능한 통계 메서드들 ==========
    
    // 향후 추가할 수 있는 통계 메서드들:
    // - 최근 N일간 게시글 개수
    // - 작성자별 게시글 개수  
    // - 조회수 TOP N 게시글
    // - 월별/일별 게시글 통계 등

}