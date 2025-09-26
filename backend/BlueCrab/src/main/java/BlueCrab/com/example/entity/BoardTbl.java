// 작성자 : 성태준
// 개시글 테이블 엔티티

package BlueCrab.com.example.entity;

// ========== 임포트 구문 ==========

// ========== Java 표준 라이브러리 ==========


// ========== JPA 어노테이션 ==========
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Table;

// ========== Validation 어노테이션 ==========
import javax.validation.constraints.NotNull;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;
import javax.validation.constraints.Min;
import javax.validation.constraints.Max;

@Entity
@Table(name = "BOARD_TBL")
public class BoardTbl {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "BOARD_IDX", length = 200, columnDefinition = "INTEGER", nullable = false)
    private Integer boardIdx;
    // 개시글 번호(기본키, 자동생성)

    @NotNull(message = "게시글 코드는 필수입니다")
    @Min(value = 0, message = "게시글 코드는 0 이상이어야 합니다")
    @Max(value = 3, message = "게시글 코드는 3 이하여야 합니다")
    @Column(name = "BOARD_CODE", length = 5, columnDefinition = "INTEGER", nullable = false)
    private Integer boardCode;
    // 개시글 코드(0 : 학교공지 / 1 : 학사공지 / 2 : 학과공지 / 3 : 교수공지)

    @Column(name = "BOARD_ON", length = 10, columnDefinition = "INTEGER", nullable = false)
    private Integer boardOn;
    // 개시글 활성화 여부(1 : 활성 / 0 : 비활성)
    // 기본값 1로 설정, 삭제된 글은 0으로 변경해 일반 사용자의 열람을 막음

    @Column(name = "BOARD_WRITER", length = 200, columnDefinition = "VARCHAR", nullable = false)
    private String boardWriter;
    // 개시글 작성자

    @Size(max = 100, message = "게시글 제목은 100자를 초과할 수 없습니다")
    @Column(name = "BOARD_TIT", length = 100, columnDefinition = "VARCHAR", nullable = true)
    private String boardTitle = "공지사항";
    // 개시글 제목
    // 기본값 "공지사항"으로 설정

    @NotBlank(message = "게시글 내용은 필수입니다")
    @Size(max = 200, message = "게시글 내용은 200자를 초과할 수 없습니다")
    @Column(name = "BOARD_CONT", length = 200, columnDefinition = "VARCHAR", nullable = true)
    private String boardContent;
    // 개시글 내용

    @Column(name = "BOARD_IMG", length = 250, columnDefinition = "VARCHAR", nullable = true)
    private String boardImg;
    // 개시글 이미지
    // DB상의 문구 : 게시판 내용을 대채하여 이미지로 올라갈 것을 대비/이미지 저장경로

    @Column(name = "BOARD_FILE", length = 250, columnDefinition = "VARCHAR", nullable = true)
    private String boardFile;
    // 개시글 첨부파일

    @Column(name = "BOARD_VIEW", length = 250, columnDefinition = "INTEGER", nullable = false)
    private Integer boardView;
    // 개시글 조회수

    @Column(name = "BOARD_REG", length = 250, columnDefinition = "VARCHAR", nullable = true)
    private String boardReg;
    // 개시글 작성일 (VARCHAR로 저장)

    @Column(name = "BOARD_LATEST", length = 250, columnDefinition = "VARCHAR", nullable = true)
    private String boardLast;
    // 개시글 수정일(마지막 수정일)
    // 기본값 현재 시간으로 설정

    @Column(name = "BOARD_IP", length = 250, columnDefinition = "VARCHAR", nullable = true)
    private String boardIp;
    // 개시글 작성자 IP

    @Column(name = "BOARD_WRITER_IDX", length = 11, columnDefinition = "INTEGER", nullable = false)
    private Integer boardWriterIdx;
    // 개시글 작성자 식별자 (UserTbl의 UserIdx 또는 AdminTbl의 AdminIdx)

    @Column(name = "BOARD_WRITER_TYPE", length = 11, columnDefinition = "INTEGER", nullable = false)
    private Integer boardWriterType;
    // 개시글 작성자 유형 (0: 일반 사용자(교수), 1: 관리자)

    // ========== Getter & Setter ==========
    public Integer getBoardIdx() {
        return boardIdx;
    }

    public void setBoardIdx(Integer boardIdx) {
        this.boardIdx = boardIdx;
    }
    
    public Integer getBoardCode() {
        return boardCode;
    }

    public void setBoardCode(Integer boardCode) {
        this.boardCode = boardCode;
    }

    public Integer getBoardOn() {
        return boardOn;
    }

    public void setBoardOn(Integer boardOn) {
        this.boardOn = boardOn;
    }

    public String getBoardWriter() {
        return boardWriter;
    }

    public void setBoardWriter(String boardWriter) {
        this.boardWriter = boardWriter;
    }

    public String getBoardTitle() {
        return boardTitle;
    }

    public void setBoardTitle(String boardTitle) {
        this.boardTitle = boardTitle;
    }

    public String getBoardContent() {
        return boardContent;
    }

    public void setBoardContent(String boardContent) {
        this.boardContent = boardContent;
    }

    public String getBoardImg() {
        return boardImg;
    }

    public void setBoardImg(String boardImg) {
        this.boardImg = boardImg;
    }

    public String getBoardFile() {
        return boardFile;
    }

    public void setBoardFile(String boardFile) {
        this.boardFile = boardFile;
    }

    public Integer getBoardView() {
        return boardView;
    }

    public void setBoardView(Integer boardView) {
        this.boardView = boardView;
    }

    public String getBoardReg() {
        return boardReg;
    }

    public void setBoardReg(String boardReg) {
        this.boardReg = boardReg;
    }

    public String getBoardLast() {
        return boardLast;
    }

    public void setBoardLast(String boardLast) {
        this.boardLast = boardLast;
    }

    public String getBoardIp() {
        return boardIp;
    }

    public void setBoardIp(String boardIp) {
        this.boardIp = boardIp;
    }

    public Integer getBoardWriterIdx() {
        return boardWriterIdx;
    }

    public void setBoardWriterIdx(Integer boardWriterIdx) {
        this.boardWriterIdx = boardWriterIdx;
    }
    
    public Integer getBoardWriterType() {
        return boardWriterType;
    }

    public void setBoardWriterType(Integer boardWriterType) {
        this.boardWriterType = boardWriterType;
    }

    // ========== toString ==========
    @Override
    public String toString() {
        return "BoardTbl{" +
                "boardIdx=" + boardIdx +
                ", boardCode='" + boardCode + '\'' +
                ", boardOn=" + boardOn +
                ", boardWriter='" + boardWriter + '\'' +
                ", boardTitle='" + boardTitle + '\'' +
                ", boardContent='" + boardContent + '\'' +
                ", boardImg='" + boardImg + '\'' +
                ", boardFile='" + boardFile + '\'' +
                ", boardView=" + boardView +
                ", boardReg=" + boardReg +
                ", boardLast=" + boardLast +
                ", boardIp='" + boardIp + '\'' +
                ", boardWriterIdx=" + boardWriterIdx +
                ", boardWriterType=" + boardWriterType +
                '}';
    }

} // BoardTbl 끝