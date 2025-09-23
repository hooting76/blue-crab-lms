// 작성자 : 성태준
// 게시글 서비스 인터페이스

package BlueCrab.com.example.service;

// ========== 임포트 구문 ==========

// ========== Java 표준 라이브러리 ==========
import java.time.LocalDateTime;
import java.util.Optional;

// ========== Spring Framework ==========
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// ========== 프로젝트 내부 클래스 ==========
import BlueCrab.com.example.entity.BoardTbl;
import BlueCrab.com.example.repository.BoardRepository;


@Service        // 게시글 서비스
@Transactional  // 트랜잭션 관리 (필요시 메서드별로 오버라이드 가능)
public class BoardService {

    // 의존성 주입
    @Autowired
    private BoardRepository boardRepository;

    // 상수 정의
    private static final Integer BOARD_ACTIVE = 1; // 활성 상태 코드
    private static final Integer BOARD_INACTIVE = 0; // 비활성 상태 코드

    // 게시글 작성
    public BoardTbl createBoard(BoardTbl boardTbl) {
        // ========== 기본 설정 ==========
        boardTbl.setBoardOn(BOARD_ACTIVE);          // 개시글 상태(삭제되지 않음)
        boardTbl.setBoardView(0);              // 조회수 0 부터
        boardTbl.setBoardReg(LocalDateTime.now());  // 현재 시간으로 작성일 설정
        boardTbl.setBoardLast(LocalDateTime.now()); // 현재 시간으로 최종 수정일 설정

        return boardRepository.save(boardTbl);
        // 저장 후 저장된 엔티티 반환 (ID 포함)
    }   // createBoard 끝

    // ========== 게시글 조회 관련 메서드 ==========

    // 전체 게시글 조회 (미삭제 게시글 만, 최신순, 페이징)
    @Transactional(readOnly = true) // 게시글을 조회만 하므로 읽기 전용 트랜잭션
    public Page<BoardTbl> getAllBoards(Integer page, Integer size) {
        Pageable pageable = PageRequest.of(page, size);
        return boardRepository.findByBoardOnOrderByBoardRegDesc(BOARD_ACTIVE, pageable);
    }   // getAllBoards 끝

    // 특정 게시글 조회 (미삭제 게시글 만) + 조회수 증가
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

    // ========== 게시글 수정/삭제 관련 메서드 ==========

    // 게시글 수정
    public BoardTbl updateBoard(Integer boardIdx, BoardTbl updatedBoard) {
        Optional<BoardTbl> existingBoard = boardRepository.findByBoardIdxAndBoardOn(boardIdx, BOARD_ACTIVE);
        // 수정할 게시글 조회 (활성 상태인 것만)
        // Optional<BoardTbl> board : Optional로 감싸서 반환 (존재하지 않을 수도 있으므로)

        if (existingBoard.isPresent()) {
            // 수정할 게시글이 존재하면
            BoardTbl board = existingBoard.get();
            // null 체크 후 업데이트
            if (updatedBoard.getBoardTitle() != null) {
                // 제목이 null이 아니면
                board.setBoardTitle(updatedBoard.getBoardTitle());
                // 제목 업데이트
            }   // 제목 null 체크 끝
            board.setBoardLast(LocalDateTime.now()); 
            // 수정 시점으로 최종 수정일 갱신
            return boardRepository.save(board);
            // 변경된 내용 저장 후 저장된 엔티티 반환
        } else {
            throw new RuntimeException("Can not find board ID : " + boardIdx);
            // 게시글이 존재하지 않으면 예외 발생
        }   // if-else 끝
    }   // updateBoard 끝

    // 게시글 삭제 (비활성 상태로 변경)
    public boolean deleteBoard (Integer boardIdx) {
        Optional<BoardTbl> board = boardRepository.findByBoardIdxAndBoardOn(boardIdx, BOARD_ACTIVE);
        // 삭제할 게시글 조회 (활성 상태인 것만)
        // Optional<BoardTbl> board : Optional로 감싸서 반환 (존재하지 않을 수도 있으므로)

        if (board.isPresent()) {
            // 삭제할 게시글이 존재하면
            BoardTbl boardToDelete = board.get();
            // Optional에서 실제 엔티티 추출
            boardToDelete.setBoardOn(BOARD_INACTIVE);
            // 비활성 상태로 변경
            boardToDelete.setBoardLast(LocalDateTime.now());
            // 최종 수정일 갱신

            boardRepository.save(boardToDelete);
            // 변경된 내용 저장

            return true; // 삭제 성공
        }   // if 끝
        return false; // 삭제 실패
    }   // deleteBoard 끝

    // ========== 검색 관련 메서드 ==========

    // ========== 기타 유틸리티 메서드 ==========
    // 게시글의 총 개수 조회 (미삭제 게시글 만)
    @Transactional(readOnly = true)
    public long getActiveBoardCount() {
        return boardRepository.countByBoardOn(BOARD_ACTIVE);
    }

    // 게시글의 존재 여부 확인 (미삭제 게시글 만)
    @Transactional(readOnly = true)
    public boolean isBoardExists(Integer boardIdx) {
        return boardRepository.findByBoardIdxAndBoardOn(boardIdx, BOARD_ACTIVE).isPresent();
    }
}
