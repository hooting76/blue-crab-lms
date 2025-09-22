// 작성자 : 성태준
// 개시글 테이블 엔티티

package BlueCrab.com.example.entity;

// ========== 임포트 구문 ==========

// ========== Java 표준 라이브러리 ==========
import java.time.LocalDateTime;

// ========== JPA 어노테이션 ==========
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Table;


@Entity
@Table(name = "BOARD_TBL")
public class BoardTbl {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "BOARD_IDX", columnDefinition = "INTEGER(200)", nullable = false)
    private Integer boardIdx;
    // 개시글 번호(기본키, 자동생성)

    @Column(name = "BOARD_CODE", columnDefinition = "INTEGER(5)", nullable = false)
    private String boardCode;
    // 개시글 코드(0 : 학교공지 / 1 : 학사공지 / 2 : 학과공지 / 3 : 교수공지)

    @Column(name = "BOARD_ON", columnDefinition = "INTEGER(10)", nullable = false)
    private Integer boardOn;
    // 개시글 활성화 여부(1 : 활성 / 0 : 비활성)
    // 기본값 1로 설정, 삭제된 글은 0으로 변경해 일반 사용자의 열람을 막음

    @Column(name = "BOARD_WRITER", columnDefinition = "VARCHAR(200)", nullable = false)
    private String boardWriter;
    // 개시글 작성자

    @Column(name = "BOARD_TIT", length = 50, columnDefinition = "VARCHAR(100)", nullable = true)
    private String boardTitle = "공지사항";
    // 개시글 제목
    // 기본값 "공지사항"으로 설정

    @Column(name = "BOARD_CONT", columnDefinition = "VARCHAR(200)", nullable = false)
    private String boardContent;
    // 개시글 내용

    @Column(name = "BOARD_POST", length = 250, columnDefinition = "VARCHAR(250)")
    private String boardPost;
    // 개시글 내용



} // BoardTbl 끝