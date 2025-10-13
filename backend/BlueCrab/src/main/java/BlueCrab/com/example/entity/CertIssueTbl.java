package BlueCrab.com.example.entity;

import javax.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 증명서 발급 이력을 저장하는 JPA 엔티티 클래스
 * CERT_ISSUE_TBL 테이블과 매핑되며, 증명서 발급 감사 로그를 관리
 *
 * 주요 기능:
 * - 증명서 발급 이력 저장 (누가, 언제, 무엇을)
 * - 발급 당시 학적/프로필 스냅샷 JSON 저장
 * - 발급 형식 및 유형 기록
 * - 보안 감사 (발급 IP 추적)
 *
 * 데이터베이스 매핑:
 * - 테이블명: CERT_ISSUE_TBL
 * - 기본키: CERT_IDX (자동 생성)
 * - 외래키: USER_IDX (USER_TBL 참조)
 *
 * 사용 시나리오:
 * - 증명서 발급 이력 저장 API (/api/registry/cert/issue)
 * - 발급 통계 및 리포트
 * - 이상 발급 패턴 감지
 * - 재발급 방지 (최근 발급 이력 체크)
 *
 * 특징:
 * - JSON 컬럼으로 발급 당시 스냅샷 저장 (불변성 보장)
 * - 시점 기준 조회 가능 (AS_OF_DATE)
 * - 발급 유형별 필터링 가능
 *
 * @author BlueCrab Development Team
 * @version 1.0.0
 * @since 2025-10-13
 */
@Entity
@Table(name = "CERT_ISSUE_TBL")
public class CertIssueTbl {

    /**
     * 발급 이력 고유 식별자
     * 데이터베이스에서 자동 생성되는 기본키
     * 발급 순서를 나타내는 시퀀스 값
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "CERT_IDX")
    private Integer certIdx;

    /**
     * 사용자 고유 식별자 (외래키)
     * USER_TBL의 USER_IDX를 참조
     * 누구의 증명서가 발급되었는지 식별
     *
     * 관계:
     * - Many-to-One: 한 사용자당 여러 발급 이력 가능
     * - LAZY 로딩으로 성능 최적화
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "USER_IDX", nullable = false)
    private UserTbl user;

    /**
     * 증명서 유형
     * 어떤 증명서가 발급되었는지 구분
     *
     * 주요 값:
     * - "enrollment": 재학증명서
     * - "graduation_expected": 졸업예정증명서
     * - "graduation": 졸업증명서
     * - "transcript": 성적증명서
     * - "certificate": 수료증명서
     *
     * 사용 시나리오:
     * - 증명서 유형별 통계
     * - 발급 제한 정책 적용
     * - 특정 증명서 발급 이력 조회
     */
    @Column(name = "CERT_TYPE", nullable = false, length = 50)
    private String certType;

    /**
     * 스냅샷 기준일
     * 증명서 발급 시 기준이 된 날짜
     *
     * NULL 허용:
     * - 현재 시점 기준 발급 시 NULL 또는 발급일
     * - 과거 특정 시점 기준 발급 시 해당 날짜
     *
     * 사용 시나리오:
     * - "2025-03-01 기준 재학증명서" 발급
     * - 소급 발급 시 기준일 기록
     */
    @Column(name = "AS_OF_DATE")
    private LocalDate asOfDate;

    /**
     * 발급 형식
     * 증명서가 어떤 형식으로 발급되었는지 기록
     *
     * 기본값: "html"
     *
     * 주요 값:
     * - "html": HTML 형식 (브라우저 출력용)
     * - "pdf": PDF 파일 (다운로드/인쇄용)
     * - "image": 이미지 파일
     *
     * 사용 시나리오:
     * - 발급 형식별 통계
     * - PDF 변환 서비스 연동 여부 판단
     */
    @Column(name = "FORMAT", nullable = false, length = 20)
    private String format = "html";

    /**
     * 발급 당시 학적/프로필 데이터 스냅샷 (JSON)
     * 발급 시점의 사용자 정보를 JSON 형태로 저장
     *
     * 저장 데이터 예시:
     * {
     *   "userName": "홍길동",
     *   "studentCode": "202500101000",
     *   "academicStatus": "재학",
     *   "enrolledTerms": 2,
     *   "departmentName": "컴퓨터공학과",
     *   ...
     * }
     *
     * 중요성:
     * - 발급 당시의 정보를 불변하게 저장
     * - 이후 학적 변경되어도 발급 당시 상태 확인 가능
     * - 감사 추적 및 재발급 검증에 활용
     *
     * MariaDB/MySQL:
     * - JSON 타입 (MariaDB 10.2+)
     * - TEXT 타입으로 폴백 가능
     */
    @Lob
    @Column(name = "SNAPSHOT_JSON", nullable = false, columnDefinition = "JSON")
    private String snapshotJson;

    /**
     * 발급 일시
     * 증명서가 발급된 정확한 시간
     *
     * 기본값: CURRENT_TIMESTAMP (DB 레벨)
     *
     * 사용 시나리오:
     * - 발급 이력 조회 시 정렬 기준
     * - 발급 간격 체크 (남발 방지)
     * - 통계 및 리포트
     */
    @Column(name = "ISSUED_AT", nullable = false)
    private LocalDateTime issuedAt;

    /**
     * 발급 발생 IP 주소
     * 증명서 발급 요청이 발생한 클라이언트 IP
     *
     * NULL 허용:
     * - 시스템 내부 발급 시 NULL
     * - 웹 요청 시 IP 기록
     *
     * 최대 길이: 45자 (IPv6 지원)
     *
     * 사용 시나리오:
     * - 보안 감사
     * - 이상 접근 탐지
     * - 지역별 발급 통계
     */
    @Column(name = "ISSUED_IP", length = 45)
    private String issuedIp;

    /**
     * 기본 생성자 (JPA 요구사항)
     */
    public CertIssueTbl() {}

    /**
     * 필수 필드 생성자
     * 새로운 증명서 발급 이력 생성 시 사용
     *
     * @param user 사용자 엔티티
     * @param certType 증명서 유형
     * @param format 발급 형식
     * @param snapshotJson 스냅샷 JSON
     * @param issuedAt 발급 일시
     */
    public CertIssueTbl(UserTbl user, String certType, String format, String snapshotJson, LocalDateTime issuedAt) {
        this.user = user;
        this.certType = certType;
        this.format = format;
        this.snapshotJson = snapshotJson;
        this.issuedAt = issuedAt;
    }

    /**
     * 발급 ID 생성
     * 발급 일시와 시퀀스를 조합한 고유 ID
     * 형식: C20250302-000123
     *
     * @return 발급 ID 문자열
     */
    public String generateIssueId() {
        if (issuedAt == null || certIdx == null) {
            return null;
        }
        String dateStr = issuedAt.toLocalDate().toString().replace("-", "");
        return String.format("C%s-%06d", dateStr, certIdx);
    }

    // ========== Getters and Setters ==========

    public Integer getCertIdx() {
        return certIdx;
    }

    public void setCertIdx(Integer certIdx) {
        this.certIdx = certIdx;
    }

    public UserTbl getUser() {
        return user;
    }

    public void setUser(UserTbl user) {
        this.user = user;
    }

    public String getCertType() {
        return certType;
    }

    public void setCertType(String certType) {
        this.certType = certType;
    }

    public LocalDate getAsOfDate() {
        return asOfDate;
    }

    public void setAsOfDate(LocalDate asOfDate) {
        this.asOfDate = asOfDate;
    }

    public String getFormat() {
        return format;
    }

    public void setFormat(String format) {
        this.format = format;
    }

    public String getSnapshotJson() {
        return snapshotJson;
    }

    public void setSnapshotJson(String snapshotJson) {
        this.snapshotJson = snapshotJson;
    }

    public LocalDateTime getIssuedAt() {
        return issuedAt;
    }

    public void setIssuedAt(LocalDateTime issuedAt) {
        this.issuedAt = issuedAt;
    }

    public String getIssuedIp() {
        return issuedIp;
    }

    public void setIssuedIp(String issuedIp) {
        this.issuedIp = issuedIp;
    }

    /**
     * 엔티티 정보를 문자열로 반환 (디버깅용)
     * 민감한 정보(JSON 스냅샷 전체)는 제외
     */
    @Override
    public String toString() {
        return "CertIssueTbl{" +
                "certIdx=" + certIdx +
                ", certType='" + certType + '\'' +
                ", asOfDate=" + asOfDate +
                ", format='" + format + '\'' +
                ", issuedAt=" + issuedAt +
                ", issueId='" + generateIssueId() + '\'' +
                '}';
    }
}
