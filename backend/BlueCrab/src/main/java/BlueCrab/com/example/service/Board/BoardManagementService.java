// 작성자 : 성태준
// 게시글 관리 서비스 (작성, 수정, 삭제)

package BlueCrab.com.example.service.Board;

// ========== 임포트 구문 ==========

// ========== Java 표준 라이브러리 ==========
import java.time.LocalDateTime;
import java.util.Optional;

// ========== Spring Framework ==========
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// ========== 로깅 ==========
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// ========== 프로젝트 내부 클래스 ==========
import BlueCrab.com.example.entity.AdminTbl;
import BlueCrab.com.example.entity.Board.BoardTbl;
import BlueCrab.com.example.entity.UserTbl;
import BlueCrab.com.example.entity.Lecture.LecTbl;
import BlueCrab.com.example.repository.Board.BoardRepository;
import BlueCrab.com.example.repository.AdminTblRepository;
import BlueCrab.com.example.repository.UserTblRepository;
import BlueCrab.com.example.repository.Lecture.LecTblRepository;

import java.util.List;
import java.util.stream.Collectors;


@Service        // 게시글 관리 서비스
@Transactional  // 트랜잭션 관리 (필요시 메서드별로 오버라이드 가능)
public class BoardManagementService {
    
    // ========== 로거 ==========
    private static final Logger logger = LoggerFactory.getLogger(BoardManagementService.class);

    // ========== 의존성 주입 ==========
    @Autowired
    private BoardRepository boardRepository;

    @Autowired
    private AdminTblRepository adminTblRepository;

    @Autowired
    private UserTblRepository userTblRepository;

    @Autowired
    private LecTblRepository lecTblRepository;

    // 상수 정의
    private static final Integer BOARD_ACTIVE = 1; // 활성 상태 코드
    private static final Integer BOARD_INACTIVE = 0; // 비활성 상태 코드
    private static final Integer BOARD_CODE_LECTURE = 3; // 강의 공지 코드

    // ========== 게시글 작성 ==========
    
    // 게시글 작성
    public Optional<BoardTbl> createBoard(BoardTbl boardTbl, String currentUserEmail) {
        logger.info("게시글 생성 시도 - 사용자: {}", currentUserEmail);

        // 관리자 및 교수 권한 확인을 위한 로직
        try {
            // AdminTbl 조회 (한 번만 조회하여 재사용)
            Optional<AdminTbl> admin = adminTblRepository.findByAdminId(currentUserEmail);
            boolean isAdmin = admin.isPresent();
            logger.info("관리자 확인 결과: {} (이메일: {})", isAdmin, currentUserEmail);

            // 교수 인지 확인을 위한 로직 (userStudent = 1)
            // UserTblRepository 사용
            Optional<UserTbl> user = userTblRepository.findByUserEmail(currentUserEmail);
            logger.info("사용자 확인 결과: {} (이메일: {})", user.isPresent(), currentUserEmail);

            boolean isProfessor = user.isPresent() && user.get().getUserStudent() == 1;
            logger.info("교수 확인 결과: {} (사용자 존재: {}, userStudent: {})", 
                       isProfessor, user.isPresent(), user.isPresent() ? user.get().getUserStudent() : "N/A");

            // ========== BOARD_CODE 3 (강의 공지) 권한 검증 ==========
            if (boardTbl.getBoardCode() != null && boardTbl.getBoardCode() == BOARD_CODE_LECTURE) {
                // 강의 공지는 관리자와 교수 모두 작성 가능
                if (!isAdmin && !isProfessor) {
                    logger.warn("강의 공지 작성 권한 없음 - 이메일: {}", currentUserEmail);
                    throw new IllegalArgumentException("강의 공지는 관리자 또는 교수만 작성할 수 있습니다.");
                }
                
                // LEC_SERIAL 필수 검증
                if (boardTbl.getLecSerial() == null || boardTbl.getLecSerial().trim().isEmpty()) {
                    logger.warn("강의 공지 작성 시 LEC_SERIAL 누락 - 이메일: {}", currentUserEmail);
                    throw new IllegalArgumentException("강의 공지 작성 시 강의 코드(LEC_SERIAL)는 필수입니다.");
                }
                
                // 교수인 경우: 본인 담당 강의인지 검증
                if (isProfessor) {
                    Integer userIdx = user.get().getUserIdx();  // USER_IDX (교수 고유 번호)
                    String userName = user.get().getUserName();
                    
                    // LEC_PROF는 USER_IDX를 저장하므로 userIdx를 문자열로 변환하여 검색
                    List<LecTbl> professorLectures = lecTblRepository.findByLecProf(String.valueOf(userIdx));
                    List<String> validLecSerials = professorLectures.stream()
                        .map(LecTbl::getLecSerial)
                        .collect(Collectors.toList());
                    
                    logger.info("교수 담당 강의 조회 - 교수 IDX: {}, 이름: {}, 담당강의: {}", 
                               userIdx, userName, validLecSerials);
                    
                    if (!validLecSerials.contains(boardTbl.getLecSerial())) {
                        logger.warn("교수가 담당하지 않는 강의에 공지 작성 시도 - 교수 IDX: {}, 이름: {}, 강의: {}, 담당강의: {}", 
                                   userIdx, userName, boardTbl.getLecSerial(), validLecSerials);
                        throw new IllegalArgumentException("본인이 담당하는 강의에만 공지를 작성할 수 있습니다. (강의코드: " + boardTbl.getLecSerial() + ")");
                    }
                    logger.info("교수 담당 강의 검증 완료 - 교수 IDX: {}, 강의: {}", userIdx, boardTbl.getLecSerial());
                }
                
                // 관리자인 경우: 모든 강의에 공지 작성 가능, 강의 존재 여부만 확인
                if (isAdmin) {
                    Optional<LecTbl> lecture = lecTblRepository.findByLecSerial(boardTbl.getLecSerial());
                    if (!lecture.isPresent()) {
                        logger.warn("관리자가 존재하지 않는 강의에 공지 작성 시도 - 강의: {}", boardTbl.getLecSerial());
                        throw new IllegalArgumentException("존재하지 않는 강의입니다. (강의코드: " + boardTbl.getLecSerial() + ")");
                    }
                    logger.info("관리자 강의 존재 검증 완료 - 강의: {}", boardTbl.getLecSerial());
                }
            } else {
                // BOARD_CODE 0~2 (학사/행정/기타 공지)는 관리자만 작성 가능
                if (!isAdmin) {
                    logger.warn("학사/행정/기타 공지 작성 권한 없음 - 교수 계정 - 이메일: {}", currentUserEmail);
                    throw new IllegalArgumentException("학사/행정/기타 공지는 관리자만 작성할 수 있습니다.");
                }
            }

            if (!isAdmin && !isProfessor) {
                // 관리자도 아니고 교수도 아니면 권한 없음으로 Optional.empty() 반환
                logger.warn("권한 없음 - 관리자: {}, 교수: {} (이메일: {})", isAdmin, isProfessor, currentUserEmail);
                return Optional.empty();
            }
            
            logger.info("권한 확인 완료 - 관리자: {}, 교수: {} (이메일: {})", isAdmin, isProfessor, currentUserEmail);
        
            // ========== 작성자 이름 설정 ==========
            if (isAdmin) {
                // 관리자인 경우 AdminTbl에서 이름 가져오기 (이미 조회된 admin 객체 재사용)
                if (admin.isPresent()) {
                    boardTbl.setBoardWriter(admin.get().getName());
                    logger.info("Admin name : {}", admin.get().getName());
                } else {
                    // 관리자 정보를 찾을 수 없는 경우 이메일을 작성자로 설정
                    boardTbl.setBoardWriter(currentUserEmail);
                    logger.warn("No admin info found, setting email as writer: {}", currentUserEmail);
                }
            } else if (isProfessor) {
                // 교수인 경우 UserTbl에서 이름 가져오기 (user는 이미 위에서 조회했음)
                boardTbl.setBoardWriter(user.get().getUserName());
                logger.info("Professor name : {}", user.get().getUserName());
            }

            // ========== 작성자 식별 정보 설정(DB에만 저장) ==========
            if (isAdmin) {
                // 관리자인 경우 AdminTbl에서 AdminIdx 가져와서 설정 (이미 조회된 admin 객체 재사용)
                if (admin.isPresent()) {
                    boardTbl.setBoardWriterIdx(admin.get().getAdminIdx());
                    boardTbl.setBoardWriterType(1);
                    logger.info("Complete to set admin writerIdx: {}, writerType: 1", admin.get().getAdminIdx());
                } else {
                    // 관리자 정보를 찾을 수 없을 경우 IDX 값을 0으로 설정(작성자 불명)
                    boardTbl.setBoardWriterIdx(0);
                    boardTbl.setBoardWriterType(3);
                    logger.warn("No admin info found, setting writerIdx and writerType to 0");
                }
            } else if (isProfessor) {
                // 교수인 경우 UserTbl에서 UserIdx 가져와서 설정
                boardTbl.setBoardWriterIdx(user.get().getUserIdx());
                boardTbl.setBoardWriterType(0);
                logger.info("Complete to set professor writerIdx: {}, writerType: 0", user.get().getUserIdx());
            }

            // ========== 기본 설정 ==========
            boardTbl.setBoardOn(BOARD_ACTIVE);          // 개시글 상태(삭제되지 않음)
            boardTbl.setBoardView(0);              // 조회수 0 부터
            boardTbl.setBoardReg(LocalDateTime.now().toString());  // 현재 시간을 문자열로 작성일 설정
            boardTbl.setBoardLast(LocalDateTime.now().toString()); // 현재 시간을 문자열로 최종 수정일 설정

            // ========== 제목 기본값 설정 (코드별) ==========
            if (boardTbl.getBoardTitle() == null || boardTbl.getBoardTitle().trim().isEmpty() || "공지사항".equals(boardTbl.getBoardTitle().trim())) {
                // 제목이 null이거나 빈 문자열이거나 기본값인 경우에만 코드에 따라 설정
                if (boardTbl.getBoardCode() == null) {
                    throw new IllegalArgumentException("게시글 코드는 null일 수 없습니다.");
                } else if (boardTbl.getBoardCode() == 0) {
                    boardTbl.setBoardTitle("학사 공지사항");
                } else if (boardTbl.getBoardCode() == 1) {
                    boardTbl.setBoardTitle("행정 공지사항");
                } else if (boardTbl.getBoardCode() == 2) {
                    boardTbl.setBoardTitle("기타 공지사항");
                } else if (boardTbl.getBoardCode() == 3) {
                    boardTbl.setBoardTitle("강의 공지사항");
                } else {
                    throw new IllegalArgumentException("유효하지 않은 게시글 코드입니다: " + boardTbl.getBoardCode());
                }
                logger.info("default title set: {}", boardTbl.getBoardTitle());
            }
            // 사용자가 제목을 입력한 경우 그대로 유지됨

            // 게시글 데이터베이스에 저장하고 Optional로 감싸서 반환
            logger.info("Saving board... title: {}, writer: {}", boardTbl.getBoardTitle(), boardTbl.getBoardWriter());
            return Optional.of(boardRepository.save(boardTbl));
            // 저장 후 저장된 엔티티 반환 (ID 포함)
            
        } catch (Exception e) {
            logger.error("게시글 생성 중 오류 발생 - 사용자: {}, 오류: {}", currentUserEmail, e.getMessage(), e);
            throw new RuntimeException("게시글 생성 중 오류가 발생했습니다: " + e.getMessage(), e);
        }
    }   // createBoard 끝

    // ========== 게시글 수정/삭제 ==========

    // 게시글 수정 (Optional 반환으로 예외 처리 대신 사용)
    public Optional<BoardTbl> updateBoard(Integer boardIdx, BoardTbl updatedBoard, String currentUserEmail) {
        Optional<BoardTbl> existingBoard = boardRepository.findByBoardIdxAndBoardOn(boardIdx, BOARD_ACTIVE);
        // 수정할 게시글 조회 (활성 상태인 것만)
        // Optional<BoardTbl> board : Optional로 감싸서 반환 (존재하지 않을 수도 있으므로)
           
            if (existingBoard.isPresent()) {
                // 수정할 게시글이 존재하면

                // 작성자 권한 확인 로직
                BoardTbl board = existingBoard.get();

                // 작성자 유형과 작성자 식별 정보 가져오기
                Integer boardWriterIdx = board.getBoardWriterIdx();
                Integer boardWriterType = board.getBoardWriterType();

                logger.info("checking permission - boardIdx: {}, writerIdx: {}, writerType: {}, requester: {}",
                            boardIdx, boardWriterIdx, boardWriterType, currentUserEmail);

                if (boardWriterType == 1) {
                    // 작성자가 관리자(boardWriterType이 1)인 경우
                    Optional<AdminTbl> admin = adminTblRepository.findByAdminId(currentUserEmail);
                    if (!admin.isPresent() || !admin.get().getAdminIdx().equals(boardWriterIdx)) {
                        // 현재 사용자가 관리자 테이블에 없거나, AdminIdx가 일치하지 않으면
                        logger.warn("Permission denied - not the admin writer. requester: {}", currentUserEmail);
                        // 권한 없음 로그
                        return Optional.empty();
                        // Optional.empty() 반환
                    }
                } else if (boardWriterType == 0) {
                    // 작성자가 교수(boardWriterType이 0)인 경우
                    Optional<UserTbl> user = userTblRepository.findByUserEmail(currentUserEmail);
                    if (!user.isPresent() || !user.get().getUserIdx().equals(boardWriterIdx) || user.get().getUserStudent() != 1) {
                        // 현재 사용자가 사용자 테이블에 없거나, UserIdx가 일치하지 않거나, 교수(userStudent != 1)가 아니면
                        logger.warn("Permission denied - not the professor writer. requester: {}", currentUserEmail);
                        // 권한 없음 로그
                        return Optional.empty();
                        // Optional.empty() 반환
                    }
                } else {
                    // 작성자 유형이 관리자(1)도 교수(0)도 아닌 경우
                    logger.warn("Permission denied - unknown writer type: {}. requester: {}", boardWriterType, currentUserEmail);
                    // 권한 없음 로그
                    return Optional.empty();
                    // Optional.empty() 반환
                }

                logger.info("Permission Check passed - proceeding with update. boardIdx: {}, requester: {}",
                            boardIdx, currentUserEmail);

                if (updatedBoard.getBoardTitle() != null) {
                    // 제목이 null이 아니면
                    board.setBoardTitle(updatedBoard.getBoardTitle());
                    // 제목 업데이트
                }   // 제목 null 체크 끝
                if (updatedBoard.getBoardContent() != null) {
                    // 내용이 null이 아니면
                    board.setBoardContent(updatedBoard.getBoardContent());
                    // 내용 업데이트
                }   // 내용 null 체크 끝
                board.setBoardLast(LocalDateTime.now().toString()); 
                // 수정 시점으로 최종 수정일 갱신
                return Optional.of(boardRepository.save(board));
                // 변경된 내용 저장 후 Optional로 감싸서 반환
            } else {
                return Optional.empty();
                // 게시글이 존재하지 않으면 Optional.empty() 반환
            }   // if-else 끝
            
    }   // updateBoard 끝

    // 게시글 삭제 (비활성 상태로 변경)
    public boolean deleteBoard (Integer boardIdx, String currentUserEmail) {
        Optional<BoardTbl> board = boardRepository.findByBoardIdxAndBoardOn(boardIdx, BOARD_ACTIVE);
        // 삭제할 게시글 조회 (활성 상태인 것만)
        // Optional<BoardTbl> board : Optional로 감싸서 반환 (존재하지 않을 수도 있으므로)

        if (board.isPresent()) {
            // 삭제할 게시글이 존재하면

            // 작성자 권한 확인 로직
            Integer boardWriterIdx = board.get().getBoardWriterIdx();
            Integer boardWriterType = board.get().getBoardWriterType();

            logger.info("Checking permission for delete - boardIdx: {}, writerIdx: {}, writerType: {}, requester: {}",
                        boardIdx, boardWriterIdx, boardWriterType, currentUserEmail);
            
            if (boardWriterType == 1) {
                // 작성자가 관리자(boardWriterType이 1)인 경우
                Optional<AdminTbl> admin = adminTblRepository.findByAdminId(currentUserEmail);
                if (!admin.isPresent() || !admin.get().getAdminIdx().equals(boardWriterIdx)) {
                    // 현재 사용자가 관리자 테이블에 없거나, AdminIdx가 일치하지 않으면
                    logger.warn("Permission denied for delete - not the admin writer. requester: {}", currentUserEmail);
                    // 권한 없음 로그
                    return false;
                    // 삭제 실패 반환
                }
                
            } else if (boardWriterType == 0) {
                // 작성자가 교수(boardWriterType이 0)인 경우
                Optional<UserTbl> user = userTblRepository.findByUserEmail(currentUserEmail);
                if (!user.isPresent() || !user.get().getUserIdx().equals(boardWriterIdx) || user.get().getUserStudent() != 1) {
                    // 현재 사용자가 사용자 테이블에 없거나, UserIdx가 일치하지 않거나, 교수(userStudent != 1)가 아니면
                    logger.warn("Permission denied for delete - not the professor writer. requester: {}", currentUserEmail);
                    // 권한 없음 로그
                    return false;
                    // 삭제 실패 반환
                }
            } else {
                // 작성자 유형이 관리자(1)도 교수(0)도 아닌 경우
                logger.warn("Permission denied for delete - unknown writer type: {}. requester: {}", boardWriterType, currentUserEmail);
                // 권한 없음 로그
                return false;
                // 삭제 실패 반환
            }

            logger.info("permission check for delete passed - proceeding with delete. boardIdx: {}, requester: {}",
                        boardIdx, currentUserEmail);

            BoardTbl boardToDelete = board.get();
            // Optional에서 실제 엔티티 추출
            boardToDelete.setBoardOn(BOARD_INACTIVE);
            // 비활성 상태로 변경
            boardToDelete.setBoardLast(LocalDateTime.now().toString());
            // 최종 수정일 갱신

            boardRepository.save(boardToDelete);
            // 변경된 내용 저장

            return true; // 삭제 성공
        }   // if 끝
        return false; // 삭제 실패
    }   // deleteBoard 끝

}