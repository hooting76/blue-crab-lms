package BlueCrab.com.example.entity;

import javax.persistence.*;
import java.time.LocalDateTime;

/**
 * 학적 정보를 저장하는 JPA 엔티티 클래스
 * REGIST_TABLE 테이블과 매핑되며, 학생의 학적 이력을 관리
 *
 * 주요 기능:
 * - 학적 상태 저장 (재학, 휴학, 졸업 등)
 * - 입학 경로 정보 (정시, 수시, 편입 등)
 * - 이수 학기 수 추적
 * - 학적 변경 이력 관리 (관리자, 처리일시, IP)
 *
 * 데이터베이스 매핑:
 * - 테이블명: REGIST_TABLE
 * - 기본키: REG_IDX (자동 생성)
 * - 외래키: USER_IDX (USER_TBL 참조)
 *
 * 사용 시나리오:
 * - 학적 조회 API (/api/registry/me)
 * - 증명서 발급 (재학증명서, 졸업예정증명서)
 * - 학적 상태 변경 이력 추적
 *
 * 특징:
 * - 한 학생당 여러 학적 이력이 존재할 수 있음
 * - 최신 학적 조회 시 ADMIN_REG DESC, REG_IDX DESC 정렬 필요
 * - USER_TBL과 Many-to-One 관계
 *
 * @author BlueCrab Development Team
 * @version 1.0.0
 * @since 2025-10-13
 */
@Entity
@Table(name = "REGIST_TABLE")
public class RegistryTbl {

    /**
     * 학적 이력 고유 식별자
     * 데이터베이스에서 자동 생성되는 기본키
     * 학적 변경 순서를 나타내는 시퀀스 값
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "REG_IDX")
    private Integer regIdx;

    /**
     * 사용자 고유 식별자 (외래키)
     * USER_TBL의 USER_IDX를 참조
     * 어떤 학생의 학적 정보인지 식별
     *
     * 관계:
     * - Many-to-One: 한 학생당 여러 학적 이력 가능
     * - LAZY 로딩으로 성능 최적화
     * - fetch join 시 즉시 로딩
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "USER_IDX", nullable = false)
    private UserTbl user;

    /**
     * 학번 또는 교번
     * 조회 보조용으로 저장 (USER_TBL.USER_CODE 중복)
     * 빠른 학적 검색을 위한 비정규화 필드
     *
     * 유효성:
     * - 필수 입력 (nullable = false)
     * - 최대 길이: 50자
     *
     * 사용 예시: "202500101000"
     */
    @Column(name = "USER_CODE", nullable = false, length = 50)
    private String userCode;

    /**
     * 입학 경로
     * 학생이 입학한 전형 유형
     *
     * 기본값: "신규"
     *
     * 주요 값:
     * - "신규": 최초 입학
     * - "정시": 정시 전형
     * - "수시": 수시 전형
     * - "편입": 편입학
     * - "특별전형": 특별 전형
     *
     * 사용 시나리오:
     * - 증명서 발급 시 입학경로 표시
     * - 통계 분석
     */
    @Column(name = "JOIN_PATH", nullable = false, length = 100)
    private String joinPath = "신규";

    /**
     * 학적 상태
     * 현재 학생의 학적 상태를 나타냄
     *
     * 기본값: "재학"
     *
     * 주요 값:
     * - "재학": 정상 재학 중
     * - "휴학": 휴학 중
     * - "복학": 복학 처리
     * - "졸업": 졸업 완료
     * - "졸업예정": 졸업 예정
     * - "제적": 제적 처리
     * - "자퇴": 자퇴 처리
     *
     * 사용 시나리오:
     * - 재학증명서 발급 조건 체크
     * - 졸업예정증명서 발급 조건 체크
     * - 수강신청 가능 여부 판단
     */
    @Column(name = "STD_STAT", nullable = false, length = 100)
    private String stdStat = "재학";

    /**
     * 휴학 기간 (문자열)
     * 휴학 중인 경우 휴학 기간 정보
     *
     * NULL 허용:
     * - 재학 중이거나 휴학이 아닌 경우 NULL
     *
     * 형식 예시:
     * - "2024-03-01 ~ 2024-08-31"
     * - "2024년 1학기"
     *
     * 사용 시나리오:
     * - 증명서에 휴학 기간 표시
     * - 복학 가능 시기 계산
     */
    @Column(name = "STD_REST_DATE", length = 200)
    private String stdRestDate;

    /**
     * 이수 완료 학기 수
     * 학생이 완료한 총 학기 수
     *
     * 기본값: 0
     *
     * 사용 시나리오:
     * - 졸업 요건 체크 (보통 8학기 이상)
     * - 졸업예정증명서 발급 조건 (7학기 이상)
     * - 학기별 통계
     */
    @Column(name = "CNT_TERM", nullable = false)
    private Integer cntTerm = 0;

    /**
     * 처리 관리자명
     * 이 학적 이력을 생성/수정한 관리자 이름
     *
     * NULL 허용:
     * - 시스템 자동 생성 시 NULL
     * - 관리자가 수동 처리 시 이름 기록
     *
     * 사용 시나리오:
     * - 학적 변경 이력 감사
     * - 책임 추적
     */
    @Column(name = "ADMIN_NAME", length = 200)
    private String adminName;

    /**
     * 처리 일시
     * 이 학적 이력이 생성/수정된 시간
     *
     * NULL 허용:
     * - 최초 등록 시 NULL 가능
     * - 이후 변경 시 필수 기록
     *
     * 사용 시나리오:
     * - 최신 학적 조회 시 정렬 기준 (DESC)
     * - 학적 변경 이력 타임라인
     * - 증명서 발급 시점 기준
     */
    @Column(name = "ADMIN_REG")
    private LocalDateTime adminReg;

    /**
     * 처리 발생 IP 주소
     * 학적 변경이 발생한 클라이언트 IP
     *
     * NULL 허용:
     * - 시스템 내부 처리 시 NULL
     * - 웹 요청 시 IP 기록
     *
     * 최대 길이: 45자 (IPv6 지원)
     *
     * 사용 시나리오:
     * - 보안 감사
     * - 이상 접근 탐지
     */
    @Column(name = "ADMIN_IP", length = 45)
    private String adminIp;

    /**
     * 기본 생성자 (JPA 요구사항)
     */
    public RegistryTbl() {}

    /**
     * 필수 필드 생성자
     * 새로운 학적 이력 생성 시 사용
     *
     * @param user 사용자 엔티티
     * @param userCode 학번/교번
     * @param joinPath 입학경로
     * @param stdStat 학적상태
     * @param cntTerm 이수학기수
     */
    public RegistryTbl(UserTbl user, String userCode, String joinPath, String stdStat, Integer cntTerm) {
        this.user = user;
        this.userCode = userCode;
        this.joinPath = joinPath;
        this.stdStat = stdStat;
        this.cntTerm = cntTerm;
    }

    // ========== Getters and Setters ==========

    public Integer getRegIdx() {
        return regIdx;
    }

    public void setRegIdx(Integer regIdx) {
        this.regIdx = regIdx;
    }

    public UserTbl getUser() {
        return user;
    }

    public void setUser(UserTbl user) {
        this.user = user;
    }

    public String getUserCode() {
        return userCode;
    }

    public void setUserCode(String userCode) {
        this.userCode = userCode;
    }

    public String getJoinPath() {
        return joinPath;
    }

    public void setJoinPath(String joinPath) {
        this.joinPath = joinPath;
    }

    public String getStdStat() {
        return stdStat;
    }

    public void setStdStat(String stdStat) {
        this.stdStat = stdStat;
    }

    public String getStdRestDate() {
        return stdRestDate;
    }

    public void setStdRestDate(String stdRestDate) {
        this.stdRestDate = stdRestDate;
    }

    public Integer getCntTerm() {
        return cntTerm;
    }

    public void setCntTerm(Integer cntTerm) {
        this.cntTerm = cntTerm;
    }

    public String getAdminName() {
        return adminName;
    }

    public void setAdminName(String adminName) {
        this.adminName = adminName;
    }

    public LocalDateTime getAdminReg() {
        return adminReg;
    }

    public void setAdminReg(LocalDateTime adminReg) {
        this.adminReg = adminReg;
    }

    public String getAdminIp() {
        return adminIp;
    }

    public void setAdminIp(String adminIp) {
        this.adminIp = adminIp;
    }

    /**
     * 엔티티 정보를 문자열로 반환 (디버깅용)
     */
    @Override
    public String toString() {
        return "RegistryTbl{" +
                "regIdx=" + regIdx +
                ", userCode='" + userCode + '\'' +
                ", joinPath='" + joinPath + '\'' +
                ", stdStat='" + stdStat + '\'' +
                ", cntTerm=" + cntTerm +
                ", adminReg=" + adminReg +
                '}';
    }
}
