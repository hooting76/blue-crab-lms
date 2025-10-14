// 작성자: 성태준

package BlueCrab.com.example.entity.Lecture;

import javax.persistence.*;

/**
 * 강의 정보를 저장하는 JPA 엔티티 클래스
 * LEC_TBL 테이블과 매핑되며, 강의 개설, 수강신청, 강의 관리 등에 사용
 *
 * 주요 기능:
 * - 강의 기본 정보 저장 (강의명, 강의코드, 담당교수 등)
 * - 수강 정원 관리 (최대 인원, 현재 인원)
 * - 강의 시간 및 강의실 정보
 * - 학점, 전공/교양, 필수/선택 구분
 * - 수강신청 상태 관리
 *
 * 데이터베이스 매핑:
 * - 테이블명: LEC_TBL
 * - 기본키: LEC_IDX (자동 생성)
 *
 * 강의 시간 포맷:
 * - "요일명+교시" 조합 형식 (예: "월1월2수3수4" = 월요일 1,2교시 + 수요일 3,4교시)
 * - 요일명: 월, 화, 수, 목, 금 (평일만)
 * - 교시: 1~8교시
 *
 * 사용 예시:
 * - 관리자가 강의 개설 시 새로운 LecTbl 객체 생성
 * - 학생이 수강신청 가능 강의 목록 조회
 * - 교수가 담당 강의 목록 조회
 *
 * @version 1.0.0
 * @since 2025-10-11
 */
@Entity
@Table(name = "LEC_TBL")
public class LecTbl {

    /**
     * 강의 고유 식별자
     * 데이터베이스에서 자동 생성되는 기본키
     * 강의 조회, 수정, 삭제 시 사용
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "LEC_IDX")
    private Integer lecIdx;

    /**
     * 강의 코드
     * 강의를 식별하는 고유 코드
     * 필수 입력 필드
     *
     * 유효성:
     * - 필수 입력 (nullable = false)
     * - 최대 길이: 50자
     *
     * 사용 예시: "CS101", "MATH201"
     */
    @Column(name = "LEC_SERIAL", nullable = false, length = 50)
    private String lecSerial;

    /**
     * 강의명
     * 필수 입력 필드
     *
     * 유효성:
     * - 필수 입력 (nullable = false)
     * - 최대 길이: 50자
     *
     * 사용 예시: "자바 프로그래밍", "데이터베이스"
     */
    @Column(name = "LEC_TIT", nullable = false, length = 50)
    private String lecTit;

    /**
     * 담당 교수명
     * 필수 입력 필드
     * USER_TBL의 userName과 매칭됨 (userStudent = 0인 교수)
     *
     * 유효성:
     * - 필수 입력 (nullable = false)
     * - 최대 길이: 50자
     *
     * 사용 예시: "김교수", "이교수"
     */
    @Column(name = "LEC_PROF", nullable = false, length = 50)
    private String lecProf;

    /**
     * 이수 학점
     * 필수 입력 필드, 기본값 0
     *
     * 유효성:
     * - 필수 입력 (nullable = false)
     * - 일반적으로 1~4 범위
     *
     * 사용 예시: 3 (3학점 강의)
     */
    @Column(name = "LEC_POINT", nullable = false)
    private Integer lecPoint = 0;

    /**
     * 전공/교양 구분
     * 필수 입력 필드, 기본값 1
     *
     * 값 의미:
     * - 1: 전공 강의
     * - 0: 교양 강의
     *
     * 사용 예시: 전공 필수 과목 = 1
     */
    @Column(name = "LEC_MAJOR", nullable = false)
    private Integer lecMajor = 1;

    /**
     * 필수/선택 구분
     * 필수 입력 필드, 기본값 1
     *
     * 값 의미:
     * - 1: 필수 과목
     * - 0: 선택 과목
     *
     * 사용 예시: 전공 필수 = 1, 전공 선택 = 0
     */
    @Column(name = "LEC_MUST", nullable = false)
    private Integer lecMust = 1;

    /**
     * 강의 개요 내용
     * 선택 입력 필드, TEXT 타입
     *
     * 유효성:
     * - 선택 입력 (nullable 허용)
     * - TEXT 타입으로 긴 내용 저장 가능
     *
     * 사용 예시: "자바 기초부터 고급 기술까지 학습합니다."
     */
    @Column(name = "LEC_SUMMARY", columnDefinition = "TEXT")
    private String lecSummary;

    /**
     * 강의 시간
     * 필수 입력 필드
     *
     * 포맷: "요일명+교시" 반복
     * - 요일명: 월, 화, 수, 목, 금
     * - 교시: 1~8
     *
     * 유효성:
     * - 필수 입력 (nullable = false)
     * - 최대 길이: 100자
     * - "요일명+교시" 패턴 반복
     *
     * 사용 예시: 
     * - "월1월2" = 월요일 1,2교시
     * - "월1월2수3수4" = 월요일 1,2교시 + 수요일 3,4교시
     * - "화2목2" = 화요일 2교시 + 목요일 2교시
     */
    @Column(name = "LEC_TIME", nullable = false, length = 100)
    private String lecTime;

    /**
     * 과제 유무
     * 필수 입력 필드, 기본값 0
     *
     * 값 의미:
     * - 1: 과제 있음
     * - 0: 과제 없음
     */
    @Column(name = "LEC_ASSIGN", nullable = false)
    private Integer lecAssign = 0;

    /**
     * 수강신청 상태
     * 필수 입력 필드, 기본값 0
     *
     * 값 의미:
     * - 1: 수강신청 열림
     * - 0: 수강신청 닫힘
     *
     * 사용 예시: 수강신청 기간에 1로 설정
     */
    @Column(name = "LEC_OPEN", nullable = false)
    private Integer lecOpen = 0;

    /**
     * 수강 가능 최대 인원수
     * 필수 입력 필드, 기본값 0
     *
     * 유효성:
     * - 필수 입력 (nullable = false)
     * - 일반적으로 20~50 범위
     *
     * 사용 예시: 30 (최대 30명 수강 가능)
     */
    @Column(name = "LEC_MANY", nullable = false)
    private Integer lecMany = 0;

    /**
     * 학부 코드
     * 필수 입력 필드
     *
     * 유효성:
     * - 필수 입력 (nullable = false)
     * - 최대 길이: 50자
     *
     * 사용 예시: "ENGIN" (공학부), "HUMAN" (인문학부)
     */
    @Column(name = "LEC_MCODE", nullable = false, length = 50)
    private String lecMcode;

    /**
     * 학과 코드
     * 필수 입력 필드
     *
     * 유효성:
     * - 필수 입력 (nullable = false)
     * - 최대 길이: 50자
     *
     * 사용 예시: "COMP" (컴퓨터공학과), "ENGL" (영어영문학과)
     */
    @Column(name = "LEC_MCODE_DEP", nullable = false, length = 50)
    private String lecMcodeDep;

    /**
     * 수강 가능 최저 학년 제한
     * 필수 입력 필드, 기본값 0
     * 학기 수로 판별 (예: 2학년 1학기 = 3)
     *
     * 유효성:
     * - 필수 입력 (nullable = false)
     * - 0: 제한 없음
     * - 1~8: 학기 수 제한
     *
     * 사용 예시: 4 (2학년 이상 수강 가능)
     */
    @Column(name = "LEC_MIN", nullable = false)
    private Integer lecMin = 0;

    /**
     * 강의 등록일
     * 선택 입력 필드
     *
     * 유효성:
     * - 선택 입력 (nullable 허용)
     * - 최대 길이: 100자
     *
     * 사용 예시: "2025-03-01 10:00:00"
     */
    @Column(name = "LEC_REG", length = 100)
    private String lecReg;

    /**
     * 강의 등록 IP 주소
     * 선택 입력 필드
     *
     * 유효성:
     * - 선택 입력 (nullable 허용)
     * - 최대 길이: 100자
     *
     * 사용 예시: "192.168.1.100"
     */
    @Column(name = "LEC_IP", length = 100)
    private String lecIp;

    /**
     * 현재 수강 인원
     * 선택 입력 필드, 기본값 0
     * ENROLLMENT_EXTENDED_TBL에서 계산되어 업데이트됨
     *
     * 유효성:
     * - 선택 입력 (nullable 허용)
     * - 0 이상
     * - lecMany 이하여야 함
     *
     * 사용 예시: 25 (현재 25명 수강 중)
     */
    @Column(name = "LEC_CURRENT")
    private Integer lecCurrent = 0;

    /**
     * 대상 학년
     * 선택 입력 필드
     *
     * 유효성:
     * - 선택 입력 (nullable 허용)
     * - 1~4 범위 (1학년~4학년)
     *
     * 사용 예시: 2 (2학년 대상 강의)
     */
    @Column(name = "LEC_YEAR")
    private Integer lecYear;

    /**
     * 학기
     * 선택 입력 필드
     *
     * 값 의미:
     * - 1: 1학기
     * - 2: 2학기
     *
     * 사용 예시: 1 (1학기 강의)
     */
    @Column(name = "LEC_SEMESTER")
    private Integer lecSemester;

    /**
     * 기본 생성자
     * JPA 엔티티 생성을 위해 필수
     */
    public LecTbl() {}

    /**
     * 필수 필드 초기화 생성자
     * 강의 등록 시 새로운 강의 객체 생성에 사용
     *
     * @param lecSerial 강의 코드
     * @param lecTit 강의명
     * @param lecProf 담당 교수명
     * @param lecPoint 이수 학점
     * @param lecTime 강의 시간
     * @param lecMany 최대 수강 인원
     * @param lecMcode 학부 코드
     * @param lecMcodeDep 학과 코드
     */
    public LecTbl(String lecSerial, String lecTit, String lecProf, Integer lecPoint, 
                  String lecTime, Integer lecMany, String lecMcode, String lecMcodeDep) {
        this.lecSerial = lecSerial;
        this.lecTit = lecTit;
        this.lecProf = lecProf;
        this.lecPoint = lecPoint;
        this.lecTime = lecTime;
        this.lecMany = lecMany;
        this.lecMcode = lecMcode;
        this.lecMcodeDep = lecMcodeDep;
    }

    // Getters and Setters

    public Integer getLecIdx() {
        return lecIdx;
    }

    public void setLecIdx(Integer lecIdx) {
        this.lecIdx = lecIdx;
    }

    public String getLecSerial() {
        return lecSerial;
    }

    public void setLecSerial(String lecSerial) {
        this.lecSerial = lecSerial;
    }

    public String getLecTit() {
        return lecTit;
    }

    public void setLecTit(String lecTit) {
        this.lecTit = lecTit;
    }

    public String getLecProf() {
        return lecProf;
    }

    public void setLecProf(String lecProf) {
        this.lecProf = lecProf;
    }

    public Integer getLecPoint() {
        return lecPoint;
    }

    public void setLecPoint(Integer lecPoint) {
        this.lecPoint = lecPoint;
    }

    public Integer getLecMajor() {
        return lecMajor;
    }

    public void setLecMajor(Integer lecMajor) {
        this.lecMajor = lecMajor;
    }

    public Integer getLecMust() {
        return lecMust;
    }

    public void setLecMust(Integer lecMust) {
        this.lecMust = lecMust;
    }

    public String getLecSummary() {
        return lecSummary;
    }

    public void setLecSummary(String lecSummary) {
        this.lecSummary = lecSummary;
    }

    public String getLecTime() {
        return lecTime;
    }

    public void setLecTime(String lecTime) {
        this.lecTime = lecTime;
    }

    public Integer getLecAssign() {
        return lecAssign;
    }

    public void setLecAssign(Integer lecAssign) {
        this.lecAssign = lecAssign;
    }

    public Integer getLecOpen() {
        return lecOpen;
    }

    public void setLecOpen(Integer lecOpen) {
        this.lecOpen = lecOpen;
    }

    public Integer getLecMany() {
        return lecMany;
    }

    public void setLecMany(Integer lecMany) {
        this.lecMany = lecMany;
    }

    public String getLecMcode() {
        return lecMcode;
    }

    public void setLecMcode(String lecMcode) {
        this.lecMcode = lecMcode;
    }

    public String getLecMcodeDep() {
        return lecMcodeDep;
    }

    public void setLecMcodeDep(String lecMcodeDep) {
        this.lecMcodeDep = lecMcodeDep;
    }

    public Integer getLecMin() {
        return lecMin;
    }

    public void setLecMin(Integer lecMin) {
        this.lecMin = lecMin;
    }

    public String getLecReg() {
        return lecReg;
    }

    public void setLecReg(String lecReg) {
        this.lecReg = lecReg;
    }

    public String getLecIp() {
        return lecIp;
    }

    public void setLecIp(String lecIp) {
        this.lecIp = lecIp;
    }

    public Integer getLecCurrent() {
        return lecCurrent;
    }

    public void setLecCurrent(Integer lecCurrent) {
        this.lecCurrent = lecCurrent;
    }

    public Integer getLecYear() {
        return lecYear;
    }

    public void setLecYear(Integer lecYear) {
        this.lecYear = lecYear;
    }

    public Integer getLecSemester() {
        return lecSemester;
    }

    public void setLecSemester(Integer lecSemester) {
        this.lecSemester = lecSemester;
    }

    /**
     * 수강신청 가능 여부 확인
     * 
     * @return 수강신청 열림 상태이면 true
     */
    public boolean isOpenForEnrollment() {
        return this.lecOpen == 1;
    }

    /**
     * 정원 초과 여부 확인
     * 
     * @return 현재 인원이 최대 인원 이상이면 true
     */
    public boolean isFull() {
        return this.lecCurrent != null && this.lecMany != null && this.lecCurrent >= this.lecMany;
    }

    /**
     * 수강 가능 인원 수 반환
     * 
     * @return 남은 수강 가능 인원
     */
    public int getAvailableSeats() {
        if (this.lecMany == null || this.lecCurrent == null) {
            return 0;
        }
        return Math.max(0, this.lecMany - this.lecCurrent);
    }

    /**
     * 객체의 문자열 표현을 반환
     * 디버깅 및 로깅 시 사용
     *
     * @return 강의 주요 정보의 문자열 표현
     */
    @Override
    public String toString() {
        return "LecTbl{" +
                "lecIdx=" + lecIdx +
                ", lecSerial='" + lecSerial + '\'' +
                ", lecTit='" + lecTit + '\'' +
                ", lecProf='" + lecProf + '\'' +
                ", lecPoint=" + lecPoint +
                ", lecTime='" + lecTime + '\'' +
                ", lecCurrent=" + lecCurrent +
                ", lecMany=" + lecMany +
                ", lecOpen=" + lecOpen +
                '}';
    }
}
