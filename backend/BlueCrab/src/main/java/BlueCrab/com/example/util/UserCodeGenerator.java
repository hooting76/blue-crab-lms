package BlueCrab.com.example.util;

import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.time.LocalDate;

/**
 * 사용자 코드 생성 유틸리티
 * 
 * 학번/교직원번호를 자동 생성하는 클래스
 * 
 * 코드 형식: YYYY-ABC-DE-FGH
 * - YYYY: 입학년도 또는 임용년도 (4자리)
 * - A: 학생/교수 구분값 (1자리)
 *   * 0: 학생
 *   * 1: 교수
 * - BC: 학부/단과대학 코드 (2자리)
 *   * 00: 미지정
 *   * 01~99: 각 단과대학 코드
 * - DE: 학과 코드 (2자리)
 *   * 00: 미지정
 *   * 01~99: 각 학과 코드
 * - FGH: 고유 난수 (3자리, 000~999)
 * 
 * 예시:
 * - 2025-001-05-847: 2025학번 학생, 01학부, 05학과, 난수 847
 * - 2024-110-02-321: 2024년 임용 교수, 10학부, 02학과, 난수 321
 * 
 * @author BlueCrab Development Team
 * @version 1.0.0
 * @since 2025-10-14
 */
@Component
public class UserCodeGenerator {
    
    private static final SecureRandom RANDOM = new SecureRandom();
    
    /**
     * 학생/교수 구분 코드
     */
    public enum UserType {
        STUDENT(0),    // 학생
        PROFESSOR(1);  // 교수
        
        private final int code;
        
        UserType(int code) {
            this.code = code;
        }
        
        public int getCode() {
            return code;
        }
    }
    
    /**
     * 사용자 코드 생성
     * 
     * @param year 입학년도 또는 임용년도 (예: 2025)
     * @param userType 사용자 유형 (학생/교수)
     * @param facultyCode 학부/단과대학 코드 (00~99)
     * @param departmentCode 학과 코드 (00~99)
     * @return 생성된 사용자 코드 (예: "2025-001-05-847")
     */
    public String generateUserCode(int year, UserType userType, int facultyCode, int departmentCode) {
        validateInput(year, facultyCode, departmentCode);
        
        // 고유 난수 생성 (000~999)
        int uniqueNumber = RANDOM.nextInt(1000);
        
        // 형식: YYYY-ABC-DE-FGH
        return String.format("%04d-%d%02d-%02d-%03d", 
            year, 
            userType.getCode(), 
            facultyCode, 
            departmentCode, 
            uniqueNumber
        );
    }
    
    /**
     * 현재 연도를 기준으로 사용자 코드 생성
     * 
     * @param userType 사용자 유형 (학생/교수)
     * @param facultyCode 학부/단과대학 코드 (00~99)
     * @param departmentCode 학과 코드 (00~99)
     * @return 생성된 사용자 코드
     */
    public String generateUserCode(UserType userType, int facultyCode, int departmentCode) {
        int currentYear = LocalDate.now().getYear();
        return generateUserCode(currentYear, userType, facultyCode, departmentCode);
    }
    
    /**
     * 미지정 학부/학과로 사용자 코드 생성 (기본값)
     * 
     * @param year 입학년도 또는 임용년도
     * @param userType 사용자 유형 (학생/교수)
     * @return 생성된 사용자 코드 (학부/학과는 00으로 설정됨)
     */
    public String generateUserCodeDefault(int year, UserType userType) {
        return generateUserCode(year, userType, 0, 0);
    }
    
    /**
     * 현재 연도, 미지정 학부/학과로 사용자 코드 생성
     * 
     * @param userType 사용자 유형 (학생/교수)
     * @return 생성된 사용자 코드
     */
    public String generateUserCodeDefault(UserType userType) {
        int currentYear = LocalDate.now().getYear();
        return generateUserCode(currentYear, userType, 0, 0);
    }
    
    /**
     * 입력 값 검증
     */
    private void validateInput(int year, int facultyCode, int departmentCode) {
        if (year < 1900 || year > 9999) {
            throw new IllegalArgumentException("년도는 1900~9999 사이여야 합니다: " + year);
        }
        if (facultyCode < 0 || facultyCode > 99) {
            throw new IllegalArgumentException("학부 코드는 00~99 사이여야 합니다: " + facultyCode);
        }
        if (departmentCode < 0 || departmentCode > 99) {
            throw new IllegalArgumentException("학과 코드는 00~99 사이여야 합니다: " + departmentCode);
        }
    }
    
    /**
     * 사용자 코드 파싱 (디버깅/검증용)
     * 
     * @param userCode 사용자 코드
     * @return 파싱된 정보를 담은 맵
     */
    public static UserCodeInfo parseUserCode(String userCode) {
        if (userCode == null || !userCode.matches("\\d{4}-\\d{3}-\\d{2}-\\d{3}")) {
            throw new IllegalArgumentException("올바른 사용자 코드 형식이 아닙니다: " + userCode);
        }
        
        String[] parts = userCode.split("-");
        int year = Integer.parseInt(parts[0]);
        int userTypeCode = Integer.parseInt(parts[1].substring(0, 1));
        int facultyCode = Integer.parseInt(parts[1].substring(1, 3));
        int departmentCode = Integer.parseInt(parts[2]);
        int uniqueNumber = Integer.parseInt(parts[3]);
        
        UserType userType = userTypeCode == 0 ? UserType.STUDENT : UserType.PROFESSOR;
        
        return new UserCodeInfo(year, userType, facultyCode, departmentCode, uniqueNumber);
    }
    
    /**
     * 사용자 코드 정보를 담는 내부 클래스
     */
    public static class UserCodeInfo {
        private final int year;
        private final UserType userType;
        private final int facultyCode;
        private final int departmentCode;
        private final int uniqueNumber;
        
        public UserCodeInfo(int year, UserType userType, int facultyCode, int departmentCode, int uniqueNumber) {
            this.year = year;
            this.userType = userType;
            this.facultyCode = facultyCode;
            this.departmentCode = departmentCode;
            this.uniqueNumber = uniqueNumber;
        }
        
        public int getYear() { return year; }
        public UserType getUserType() { return userType; }
        public int getFacultyCode() { return facultyCode; }
        public int getDepartmentCode() { return departmentCode; }
        public int getUniqueNumber() { return uniqueNumber; }
        
        @Override
        public String toString() {
            return String.format("UserCodeInfo{year=%d, userType=%s, faculty=%02d, department=%02d, unique=%03d}",
                year, userType, facultyCode, departmentCode, uniqueNumber);
        }
    }
}
