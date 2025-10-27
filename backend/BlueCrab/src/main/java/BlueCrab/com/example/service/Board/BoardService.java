// 작성자 : 성태준
// 게시글 서비스 인터페이스

package BlueCrab.com.example.service.Board;

// ========== 임포트 구문 ==========

// ========== Java 표준 라이브러리 ==========
import java.util.Optional;
import java.util.HashMap;
import java.util.Map;

// ========== Spring Framework ==========
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// ========== 프로젝트 내부 클래스 ==========
import BlueCrab.com.example.entity.Board.BoardTbl;
import BlueCrab.com.example.repository.Board.BoardRepository;


@Service        // 게시글 조회 서비스 (목록, 상세)
@Transactional(readOnly = true)  // 조회 전용 서비스이므로 읽기 전용 트랜잭션
public class BoardService {

    // ========== 의존성 주입 ==========
    @Autowired
    private BoardRepository boardRepository;

    // 상수 정의
    private static final Integer BOARD_ACTIVE = 1; // 활성 상태 코드



    // ========== 게시글 조회 관련 메서드 ==========

    // ========== 목록 조회 메서드 (성능 최적화 버전) ==========
    
    // 전체 게시글 목록 조회 (본문 제외, 성능 최적화용)
    @Transactional(readOnly = true)
    public Page<Map<String, Object>> getAllBoardsForList(Integer page, Integer size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Object[]> results = boardRepository.findBoardListWithoutContent(BOARD_ACTIVE, pageable);
        
        return results.map(this::convertObjectArrayToMap);
    }   // getAllBoardsForList 끝

    // 코드별 게시글 목록 조회 (본문 제외, 성능 최적화용)
    @Transactional(readOnly = true)
    public Page<Map<String, Object>> getBoardsByCodeForList(Integer boardCode, Integer page, Integer size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Object[]> results = boardRepository.findBoardListByCodeWithoutContent(BOARD_ACTIVE, boardCode, pageable);
        
        return results.map(this::convertObjectArrayToMap);
    }   // getBoardsByCodeForList 끝

    // 코드 + 강의 코드별 게시글 목록 조회 (강의 공지 전용)
    @Transactional(readOnly = true)
    public Page<Map<String, Object>> getBoardsByCodeAndLecSerialForList(Integer boardCode, String lecSerial, Integer page, Integer size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Object[]> results = boardRepository.findBoardListByCodeAndLecSerialWithoutContent(BOARD_ACTIVE, boardCode, lecSerial, pageable);
        
        return results.map(this::convertObjectArrayToMap);
    }   // getBoardsByCodeAndLecSerialForList 끝

    // Object[] 결과를 Map으로 변환하는 헬퍼 메서드 (본문 제외 버전)
    private Map<String, Object> convertObjectArrayToMap(Object[] result) {
        Map<String, Object> map = new HashMap<>();
        
        // Repository 쿼리 순서에 맞춰 매핑
        // SELECT b.boardIdx, b.boardCode, b.boardOn, b.boardWriter, 
        //        b.boardTitle, b.boardImg, b.boardFile, b.boardView,
        //        b.boardReg, b.boardLast, b.boardIp, b.boardWriterIdx, b.boardWriterType
        map.put("boardIdx", result[0]);      // boardIdx(0)
        map.put("boardCode", result[1]);     // boardCode(1)
        map.put("boardOn", result[2]);       // boardOn(2)
        map.put("boardWriter", result[3]);   // boardWriter(3)
        map.put("boardTitle", result[4]);    // boardTitle(4)
        map.put("boardImg", result[5]);      // boardImg(5)
        map.put("boardFile", result[6]);     // boardFile(6)
        map.put("boardView", result[7]);     // boardView(7)
        map.put("boardReg", result[8]);      // boardReg(8)
        map.put("boardLast", result[9]);     // boardLast(9)
        map.put("boardIp", result[10]);      // boardIp(10)
        map.put("boardWriterIdx", result[11]); // boardWriterIdx(11)
        map.put("boardWriterType", result[12]); // boardWriterType(12)
        
        return map;
    }   // convertObjectArrayToMap 끝

    // 특정 게시글 조회 (미삭제 게시글 만) + 조회수 증가
    @Transactional  // 조회수 증가를 위해 쓰기 트랜잭션 필요
    public Optional<BoardTbl> getBoardDetail(Integer boardIdx) {
        Optional<BoardTbl> board = boardRepository.findByBoardIdxAndBoardOn(boardIdx, BOARD_ACTIVE);
        if (board.isPresent()) {
            // 게시글이 존재하면
            boardRepository.incrementBoardView(boardIdx);
            // 조회수 증가
        }
        return board;
        // Optional로 감싸서 반환 (존재하지 않을 수도 있으므로)
    }   // getBoardDetail 끝

    // ========== 게시글 수정 관련 메서드 ==========

    // 게시글 ID로 조회 (활성 게시글만, 조회수 증가 없음)
    @Transactional(readOnly = true)
    public Optional<BoardTbl> findById(Integer boardIdx) {
        return boardRepository.findByBoardIdxAndBoardOn(boardIdx, BOARD_ACTIVE);
    }   // findById 끝

    // 게시글 업데이트 (첨부파일 연결 등)
    @Transactional
    public BoardTbl updateBoard(BoardTbl board) {
        return boardRepository.save(board);
    }   // updateBoard 끝

    // ========== 검색 관련 메서드 ==========
    
    // 향후 추가 예정:
    // - 제목/내용으로 검색
    // - 작성자로 검색
    // - 날짜 범위로 검색 등

}
