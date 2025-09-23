// 작성자 : 성태준
// 게시글 서비스 인터페이스

package BlueCrab.com.example.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// ========== 임포트 구문 ==========

// ========== Java 표준 라이브러리 ==========
import java.time.LocalDateTime;
import java.util.Optional;

import javax.persistence.criteria.CriteriaBuilder.In;

// ========== Spring Framework ==========
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

// ========== 프로젝트 내부 클래스 ==========
import BlueCrab.com.example.entity.BoardTbl;
import BlueCrab.com.example.repository.BoardRepository;


@Service
@Transactional
public class BoardService {

    // 의존성 주입
    @Autowired
    private BoardRepository boardRepository;

    // 상수 정의
    private static final Integer BOARD_ACTIVE = 1; // 활성 상태 코드
    private static final Integer BOARD_INACTIVE = 0; // 비활성 상태 코드

    // 게시글 작성
    public BoardTbl createBoard(BoardTbl boardTbl) {
        // 기본값
        boardTbl.setBoardOn(BOARD_ACTIVE);          // 개시글 상태(삭제되지 않음)
        boardTbl.setBoardView(0);              // 조회수 0 부터
        boardTbl.setBoardReg(LocalDateTime.now());  // 현재 시간으로 작성일 설정
        boardTbl.setBoardLast(LocalDateTime.now()); // 현재 시간으로 최종 수정일 설정

        return boardRepository.save(boardTbl);
    }

    // ========== 게시글 조회 관련 메서드 ==========

    // 전체 게시글 조회 (미삭제 게시글 만, 최신순, 페이징)
    @Transactional(readOnly = true) // 게시글을 조회만 하므로 읽기 전용 트랜잭션
    public Page<BoardTbl> getAllBoards(Integer page, Integer size) {
        Pageable pageable = PageRequest.of(page, size);
        return boardRepository.findByBoardOnOrderByBoardRegDesc(BOARD_ACTIVE, pageable);
    }

}
