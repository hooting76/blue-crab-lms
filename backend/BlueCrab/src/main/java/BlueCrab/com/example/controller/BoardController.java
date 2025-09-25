// 작성자 : 성태준
// 게시판 관련 통합 컨트롤러

package BlueCrab.com.example.controller;

// ========== 임포트 구문 ==========

// ========== Java 표준 라이브러리 ==========
import java.util.Optional;

// ========== Spring Framework ==========
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

// ========== 로깅 ==========
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// ========== 프로젝트 내부 클래스 ==========
import BlueCrab.com.example.entity.BoardTbl;
import BlueCrab.com.example.service.BoardService;


@RestController
@RequestMapping("/api/boards")
public class BoardController {
    
    // ========== 로거 ==========
    private static final Logger logger = LoggerFactory.getLogger(BoardController.class);

    // =========== 의존성 주입 ==========

    @Autowired
    private BoardService boardService;
    // 중요 : 실제 게시글 괄련 기능이 이루어 지는 곳은 BoardService

    // ========== 기본 기능 ==========

    // 게시글 작성
    @PostMapping("/create")
    // HTTP POST 요청을 처리하는 엔드포인트 매핑 (명확한 기능 구분: /api/boards/create)
    public ResponseEntity<?> createBoard(@RequestBody BoardTbl boardTbl) {
        // @RequestBody BoardTbl boardTbl : 요청 본문에 포함된 JSON 데이터를 BoardTbl 객체로 매핑
        logger.info("BoardController.createBoard 호출됨 - 제목: {}, 코드: {}", 
                   boardTbl.getBoardTitle(), boardTbl.getBoardCode());
        
        try {
            Optional<BoardTbl> result = boardService.createBoard(boardTbl);
            // BoardService의 createBoard 메서드를 호출하여 게시글 생성해 DB에 저장
            
            logger.info("BoardService.createBoard 완료 - 결과: {}", result.isPresent() ? "성공" : "실패");
        
            if (result.isPresent()) {
                logger.info("게시글 생성 성공 - ID: {}, 제목: {}", result.get().getBoardIdx(), result.get().getBoardTitle());
                return ResponseEntity.ok(result.get());
                // 게시글 생성 성공 - 200 OK와 함께 생성된 게시글 반환
            } else {
                logger.warn("게시글 생성 실패 - 권한 없음");
                return ResponseEntity.badRequest()
                        .body("관리자 또는 교수만 게시글을 작성할 수 있습니다");
                // 게시글 생성 실패 (권한 없음) - 400 Bad Request와 함께 에러 메시지 반환
            }
            
        } catch (Exception e) {
            logger.error("BoardController.createBoard 에러 발생: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body("게시글 생성 중 서버 오류가 발생했습니다: " + e.getMessage());
        }
    }

    // 게시글 목록 조회 (페이징)
    @GetMapping("/list")
    // HTTP GET 요청을 처리하는 엔드포인트 매핑 (명확한 기능 구분: /api/boards/list)
    public Page<BoardTbl> getAllBoards(@RequestParam(defaultValue = "0") Integer page,
                                       @RequestParam(defaultValue = "10") Integer size) {
        // @RequestParam : 쿼리 파라미터로 페이지 번호와 페이지 크기를 받음, 기본값 각각 0과 10
        // 페이지 번호 : 0부터 시작
        // 페이지 크기 : 한 페이지에 표시할 게시글 수

        return boardService.getAllBoards(page, size);
        // BoardService의 getAllBoards 메서드를 호출하여 페이징된 게시글 목록 반환
    }

    // 특정 게시글 조회 + 조회수 증가
    @GetMapping("/{boardIdx}")
    // 특정 게시글 상세 조회를 위한 엔드포인트 매핑
    public ResponseEntity<?> getBoardDetail (@PathVariable Integer boardIdx) {
        // @PathVariable Integer boardIdx : URL 경로에서 게시글 번호(boardIdx)를 추출
        // boardIdx : 조회할 게시글의 고유 번호
        Optional<BoardTbl> board = boardService.getBoardDetail(boardIdx);
        
        if (board.isPresent()) {
            return ResponseEntity.ok(board.get());
        } else {
            return ResponseEntity.notFound().build();
        }
        // BoardService의 getBoardDetail 메서드를 호출하여 해당 게시글 상세 정보 반환
        // Optional 처리를 통해 존재하지 않을 경우 404 반환, 존재하면 실제 객체 반환
        // BoardService의 getBoardDetail 메서드 에는 조회수 증가 기능이 포함되어 있음
    }

    // 게시글 수정
    @PutMapping("/update/{boardIdx}")
    // 특정 게시글 수정을 위한 엔드포인트 매핑 (명확한 기능 구분: /api/boards/update/{boardIdx})
    public ResponseEntity<?> updateBoard(@PathVariable Integer boardIdx, 
                                @RequestBody BoardTbl updatedBoard) {
        
        Optional<BoardTbl> result = boardService.updateBoard(boardIdx, updatedBoard);
        // BoardService의 updateBoard 메서드를 호출하여 게시글 수정 시도
        
        if (result.isPresent()) {
            return ResponseEntity.ok(result.get());
            // 게시글 수정 성공 - 200 OK와 함께 수정된 게시글 반환
        } else {
            return ResponseEntity.notFound().build();
            // 게시글 수정 실패 (게시글을 찾을 수 없음) - 404 Not Found 반환
        }
    }

    // 게시글 삭제 (비활성화)
    @DeleteMapping("/delete/{boardIdx}")
    // 특정 게시글 삭제를 위한 엔드포인트 매핑 (명확한 기능 구분: /api/boards/delete/{boardIdx})
    public boolean deleteBoard(@PathVariable Integer boardIdx) {

        return boardService.deleteBoard(boardIdx);
        // BoardService의 deleteBoard 메서드를 호출하여 게시글 비활성화 처리 후 성공 여부 반환
    }

    // ========== 기타 유틸리티 메서드 ==========

    // 활성화된 게시글 총 개수 조회
    @GetMapping("/count")
    // 활성화된 게시글 총 개수 조회를 위한 엔드포인트 매핑
    public long getActiveBoardCount() {

        return boardService.getActiveBoardCount();
        // BoardService의 getActiveBoardCount 메서드를 호출하여 활성화된 게시글 총 개수 반환
    }

    // 특정 게시글 존재 여부 확인
    @GetMapping("/exists/{boardIdx}")
    // 특정 게시글 존재 여부 확인을 위한 엔드포인트 매핑
    public boolean isBoardExists(@PathVariable Integer boardIdx) {

        return boardService.isBoardExists(boardIdx);
        // BoardService의 isBoardExists 메서드를 호출하여 특정 게시글 존재 여부 반환
    }

    // ========== 게시글 종류(코드) 별 조회 ==========

    // 코드 별 게시글 조회 (페이징)
    @GetMapping("/bycode/{boardCode}")
    // 특정 코드별 게시글 조회를 위한 엔드포인트 매핑
    public Page<BoardTbl> getBoardsByCode(@PathVariable Integer boardCode,
                                          @RequestParam(defaultValue = "0") Integer page,
                                          @RequestParam(defaultValue = "10") Integer size) {
        // @PathVariable Integer boardCode : URL 경로에서 게시글 코드(boardCode)를 추출
        // boardCode : 조회할 게시글의 코드 (0: 학교공지, 1: 학사공지, 2: 학과공지, 3: 교수공지)
        // @RequestParam : 쿼리 파라미터로 페이지 번호와 페이지 크기를 받음, 기본값 각각 0과 10
        // 페이지 번호 : 0부터 시작
        // 페이지 크기 : 한 페이지에 표시할 게시글 수

        return boardService.getBoardsByCode(boardCode, page, size);
        // BoardService의 getBoardsByCode 메서드를 호출하여 특정 코드별 페이징된 게시글 목록 반환
    }

    // 코드 별 게시글 총 개수 조회
    @GetMapping("/count/bycode/{boardCode}")
    // 특정 코드별 게시글 총 개수 조회를 위한 엔드포인트 매핑
    public long getBoardCountByCode(@PathVariable Integer boardCode) {
        return boardService.getBoardCountByCode(boardCode);
        // BoardService의 getBoardCountByCode 메서드를 호출하여 특정 코드별 게시글 총 개수 반환
    }
    
    // ========== 디버깅용 엔드포인트 ==========
    
    // 서버 상태 확인용
    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Board API is working! Server time: " + java.time.LocalDateTime.now());
    }
    
    // 간단한 게시글 작성 테스트 (제목 없이)
    @PostMapping("/simple-create")
    public ResponseEntity<?> simpleCreateBoard() {
        // 하드코딩된 간단한 게시글 생성
        BoardTbl boardTbl = new BoardTbl();
        boardTbl.setBoardCode(3); // 교수공지
        boardTbl.setBoardContent("테스트 게시글입니다.");
        // 제목은 기본값 사용 (설정 안함)
        
        Optional<BoardTbl> result = boardService.createBoard(boardTbl);
        
        if (result.isPresent()) {
            return ResponseEntity.ok(result.get());
        } else {
            return ResponseEntity.badRequest()
                    .body("게시글 생성 실패");
        }
    }
    
}
