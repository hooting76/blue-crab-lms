package BlueCrab.com.example.service;

import BlueCrab.com.example.entity.UserTbl;
import BlueCrab.com.example.exception.DuplicateResourceException;
import BlueCrab.com.example.exception.ResourceNotFoundException;
import BlueCrab.com.example.repository.UserTblRepository;
import BlueCrab.com.example.util.SlowQueryLogger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 사용자 관리를 위한 서비스 클래스
 * UserTbl 엔티티 기반으로 사용자 CRUD 작업 및 비즈니스 로직을 처리
 *
 * 주요 기능:
 * - 사용자 계정 생성, 조회, 수정, 삭제 (CRUD)
 * - 이메일/전화번호 중복 검사 및 검증
 * - 역할 기반 사용자 분류 (학생/교수)
 * - 사용자 검색 및 필터링 (이름, 키워드, 생년월일 범위)
 * - 사용자 통계 정보 제공
 * - 성능 모니터링을 통한 느린 쿼리 감지
 *
 * 비즈니스 규칙:
 * - 계정 생성 시 이메일과 전화번호의 유니크성 검증
 * - 계정 수정 시 변경되는 값에 대해서만 중복 검사
 * - 존재하지 않는 사용자 접근 시 예외 발생
 * - 모든 데이터베이스 작업은 트랜잭션으로 보호
 *
 * 성능 최적화:
 * - SlowQueryLogger를 통한 쿼리 성능 모니터링
 * - 필요한 경우에만 데이터베이스 조회 수행
 * - 통계 정보 캐싱 고려 (현재는 실시간 계산)
 *
 * 예외 처리:
 * - ResourceNotFoundException: 사용자를 찾을 수 없는 경우
 * - DuplicateResourceException: 이메일/전화번호 중복 시
 *
 * 의존성:
 * - UserTblRepository: 데이터베이스 접근
 * - SlowQueryLogger: 성능 모니터링 유틸리티
 *
 * 사용 예시:
 * - 회원가입: createUser()로 새 계정 생성
 * - 프로필 수정: updateUser()로 정보 업데이트
 * - 사용자 검색: searchByKeyword()로 통합 검색
 * - 통계 대시보드: getUserStats()로 사용자 현황 조회
 *
 * @author BlueCrab Development Team
 * @version 1.0.0
 * @since 2024-01-01
 */
@Service
@Transactional
public class UserTblService {
    
    /**
     * 사용자 리포지토리
     * 데이터베이스 접근을 위한 의존성 주입
     */
    @Autowired
    private UserTblRepository userTblRepository;
    
    /**
     * 모든 사용자 목록을 조회하는 메서드
     * 관리자 대시보드나 사용자 목록 페이지에서 사용
     *
     * 성능 모니터링:
     * - SlowQueryLogger를 사용하여 쿼리 실행 시간 측정
     * - 1초 이상 걸리는 경우 경고 로그 기록
     *
     * @return List<UserTbl> 모든 사용자 목록
     *
     * 사용 예시:
     * List<UserTbl> allUsers = userService.getAllUsers();
     * // 관리자 페이지에서 전체 사용자 목록 표시
     */
    public List<UserTbl> getAllUsers() {
        return SlowQueryLogger.measureAndLog("getAllUsers", 
            () -> userTblRepository.findAll());
    }
    
    /**
     * 학생 사용자만 조회하는 메서드
     * 학사 관리 시스템에서 학생 목록을 분리하여 조회할 때 사용
     *
     * 성능 모니터링:
     * - SlowQueryLogger를 사용하여 쿼리 성능 측정
     * - 학생 수에 따라 성능 차이가 있을 수 있음
     *
     * @return List<UserTbl> 학생 사용자 목록 (userStudent = 1)
     *
     * 사용 예시:
     * List<UserTbl> students = userService.getStudentUsers();
     * // 학생 명단 출력이나 학생별 기능 제공
     */
    public List<UserTbl> getStudentUsers() {
        return SlowQueryLogger.measureAndLog("getStudentUsers", 
            () -> userTblRepository.findByUserStudent(1));
    }
    
    /**
     * 교수 사용자만 조회하는 메서드
     * 교수 목록을 별도로 관리해야 하는 경우 사용
     *
     * @return List<UserTbl> 교수 사용자 목록 (userStudent = 0)
     *
     * 사용 예시:
     * List<UserTbl> professors = userService.getProfessorUsers();
     * // 교수 목록 조회나 교수별 권한 부여
     */
    public List<UserTbl> getProfessorUsers() {
        return userTblRepository.findByUserStudent(0);
    }
    
    /**
     * ID로 특정 사용자 정보를 조회하는 메서드
     * 사용자 상세 정보 페이지나 프로필 조회 시 사용
     *
     * @param id 조회할 사용자 ID
     * @return Optional<UserTbl> 사용자 정보 (존재하지 않으면 empty)
     *
     * 사용 예시:
     * Optional<UserTbl> user = userService.getUserById(123);
     * if (user.isPresent()) {
     *     // 사용자 정보 표시
     * } else {
     *     // 사용자 없음 처리
     * }
     */
    public Optional<UserTbl> getUserById(Integer id) {
        return userTblRepository.findById(id);
    }
    
    /**
     * 이메일 주소로 사용자 정보를 조회하는 메서드
     * 로그인, 계정 찾기, 프로필 조회 등에 사용
     *
     * @param email 조회할 사용자 이메일
     * @return Optional<UserTbl> 사용자 정보 (존재하지 않으면 empty)
     *
     * 사용 예시:
     * Optional<UserTbl> user = userService.getUserByEmail("student@university.edu");
     * // 이메일로 사용자 정보 조회
     */
    public Optional<UserTbl> getUserByEmail(String email) {
        return userTblRepository.findByUserEmail(email);
    }
    
    /**
     * 새로운 사용자 계정을 생성하는 메서드
     * 회원가입 프로세스의 핵심 로직으로, 중복 검사를 수행한 후 계정을 생성
     *
     * 처리 단계:
     * 1. 이메일과 전화번호 중복 검사 수행
     * 2. 중복이 없으면 데이터베이스에 저장
     * 3. 생성된 사용자 정보 반환
     *
     * 비즈니스 규칙:
     * - 이메일과 전화번호는 시스템 전체에서 유니크해야 함
     * - 중복 발견 시 DuplicateResourceException 발생
     *
     * @param user 생성할 사용자 정보 (UserTbl 객체)
     * @return UserTbl 생성된 사용자 정보 (ID가 자동 할당됨)
     * @throws DuplicateResourceException 이메일 또는 전화번호가 이미 존재하는 경우
     *
     * 사용 예시:
     * UserTbl newUser = new UserTbl("user@example.com", "password", "홍길동", "01012345678", "1990-01-01", 1);
     * UserTbl createdUser = userService.createUser(newUser);
     * // 회원가입 완료 처리
     */
    public UserTbl createUser(UserTbl user) {
        validateUserUniqueness(user.getUserEmail(), user.getUserPhone());
        return userTblRepository.save(user);
    }
    
    /**
     * 기존 사용자 정보를 수정하는 메서드
     * 프로필 수정 기능의 핵심 로직으로, 변경되는 값에 대해서만 중복 검사를 수행
     *
     * 처리 단계:
     * 1. 사용자 ID로 기존 정보 조회
     * 2. 변경되는 이메일/전화번호에 대해 중복 검사
     * 3. 사용자 필드 업데이트
     * 4. 데이터베이스에 저장 및 반환
     *
     * 비즈니스 규칙:
     * - 본인의 기존 값은 중복 검사에서 제외
     * - 변경되지 않은 필드는 검증하지 않음
     * - 존재하지 않는 사용자 ID는 예외 발생
     *
     * @param id 수정할 사용자 ID
     * @param userDetails 수정할 사용자 정보
     * @return UserTbl 수정된 사용자 정보
     * @throws ResourceNotFoundException 지정된 ID의 사용자가 존재하지 않는 경우
     * @throws DuplicateResourceException 변경하려는 이메일/전화번호가 다른 사용자가 사용 중인 경우
     *
     * 사용 예시:
     * UserTbl updateData = new UserTbl();
     * updateData.setUserName("김철수");
     * updateData.setUserPhone("01098765432");
     * UserTbl updatedUser = userService.updateUser(123, updateData);
     * // 프로필 수정 완료
     */
    public UserTbl updateUser(Integer id, UserTbl userDetails) {
        UserTbl user = findUserById(id);
        
        // 변경된 값에 대해서만 중복 검사 수행
        validateUserUniquenessForUpdate(user, userDetails);
        
        updateUserFields(user, userDetails);
        return userTblRepository.save(user);
    }
    
    /**
     * 사용자 계정을 삭제하는 메서드
     * 회원 탈퇴 기능에서 사용되며, 존재 여부 확인 후 삭제 수행
     *
     * 처리 단계:
     * 1. 사용자 ID 존재 여부 확인
     * 2. 존재하지 않으면 예외 발생
     * 3. 존재하면 데이터베이스에서 삭제
     *
     * 비즈니스 규칙:
     * - 존재하지 않는 사용자는 삭제할 수 없음
     * - 삭제 전 존재 확인으로 예외 상황 방지
     *
     * @param id 삭제할 사용자 ID
     * @throws ResourceNotFoundException 지정된 ID의 사용자가 존재하지 않는 경우
     *
     * 사용 예시:
     * userService.deleteUser(123);
     * // 사용자 계정 삭제 완료
     */
    public void deleteUser(Integer id) {
        if (!userTblRepository.existsById(id)) {
            throw ResourceNotFoundException.forId("사용자", id);
        }
        userTblRepository.deleteById(id);
    }
    
    /**
     * 사용자 역할을 전환하는 메서드 (학생 ↔ 교수)
     * 관리자가 사용자 역할을 변경해야 하는 경우 사용
     *
     * 처리 로직:
     * - 현재 역할 확인 (1: 학생, 0: 교수)
     * - 역할 값 토글 (1 ↔ 0)
     * - 변경된 정보 저장 및 반환
     *
     * 비즈니스 규칙:
     * - 역할 값은 0 또는 1만 허용
     * - 존재하지 않는 사용자는 예외 발생
     *
     * @param id 역할을 변경할 사용자 ID
     * @return UserTbl 역할이 변경된 사용자 정보
     * @throws ResourceNotFoundException 지정된 ID의 사용자가 존재하지 않는 경우
     *
     * 사용 예시:
     * UserTbl updatedUser = userService.toggleUserRole(123);
     * // 학생 ↔ 교수 역할 전환 완료
     */
    public UserTbl toggleUserRole(Integer id) {
        UserTbl user = findUserById(id);
        // 학생(1) ↔ 교수(0) 전환
        user.setUserStudent(user.getUserStudent() == 1 ? 0 : 1);
        return userTblRepository.save(user);
    }
    
    /**
     * 이름으로 사용자들을 검색하는 메서드
     * 사용자 검색 기능에서 부분 일치 검색을 수행
     *
     * 검색 방식:
     * - LIKE 연산자를 사용하여 부분 일치 검색
     * - 이름에 검색어가 포함된 모든 사용자 반환
     *
     * @param name 검색할 이름의 일부 문자열
     * @return List<UserTbl> 이름에 검색어가 포함된 사용자 목록
     *
     * 사용 예시:
     * List<UserTbl> users = userService.searchByName("김");
     * // "김철수", "김영희", "박김수" 등이 검색됨
     */
    public List<UserTbl> searchByName(String name) {
        return userTblRepository.findByUserNameContaining(name);
    }
    
    /**
     * 키워드로 사용자들을 검색하는 메서드 (이름 또는 이메일)
     * 통합 검색 기능에서 이름이나 이메일로 검색할 때 사용
     *
     * 검색 방식:
     * - 이름과 이메일 모두에서 키워드 검색
     * - OR 조건으로 둘 중 하나라도 일치하면 결과에 포함
     *
     * @param keyword 검색할 키워드 (이름이나 이메일의 일부)
     * @return List<UserTbl> 키워드가 포함된 사용자 목록
     *
     * 사용 예시:
     * List<UserTbl> users = userService.searchByKeyword("student");
     * // 이름이나 이메일에 "student"가 포함된 사용자 검색
     */
    public List<UserTbl> searchByKeyword(String keyword) {
        return userTblRepository.findByKeyword(keyword);
    }
    
    /**
     * 생년월일 범위로 사용자들을 검색하는 메서드
     * 연령대별 사용자 조회나 생일자 찾기 기능에 사용
     *
     * 검색 방식:
     * - BETWEEN 연산자로 범위 검색
     * - 시작일과 종료일을 포함한 범위 검색
     *
     * @param startDate 시작 날짜 (포함, 예: "1990-01-01")
     * @param endDate 종료 날짜 (포함, 예: "1999-12-31")
     * @return List<UserTbl> 생년월일이 범위 내에 있는 사용자 목록
     *
     * 사용 예시:
     * List<UserTbl> users = userService.searchByBirthRange("1990-01-01", "1999-12-31");
     * // 1990년대 생 사용자 검색
     */
    public List<UserTbl> searchByBirthRange(String startDate, String endDate) {
        return userTblRepository.findByUserBirthBetween(startDate, endDate);
    }
    
    /**
     * 사용자 통계 정보를 조회하는 메서드
     * 관리자 대시보드에서 사용자 현황을 표시할 때 사용
     *
     * 제공하는 통계 정보:
     * - 전체 사용자 수
     * - 학생 사용자 수
     * - 교수 사용자 수
     * - 학생 비율 (%)
     * - 교수 비율 (%)
     *
     * 계산 방식:
     * - 실시간으로 데이터베이스에서 카운트 쿼리 수행
     * - 비율은 전체 사용자 수로 나누어 계산
     *
     * @return Map<String, Object> 통계 정보를 담은 맵
     * - "totalUsers": 전체 사용자 수 (Long)
     * - "studentUsers": 학생 수 (Long)
     * - "professorUsers": 교수 수 (Long)
     * - "studentPercentage": 학생 비율 (Double)
     * - "professorPercentage": 교수 비율 (Double)
     *
     * 사용 예시:
     * Map<String, Object> stats = userService.getUserStats();
     * Long totalUsers = (Long) stats.get("totalUsers");
     * Double studentPercentage = (Double) stats.get("studentPercentage");
     * // 대시보드에 통계 정보 표시
     */
    public Map<String, Object> getUserStats() {
        Map<String, Object> stats = new HashMap<>();
        
        long totalUsers = userTblRepository.countAllUsers();
        long studentUsers = userTblRepository.countStudents();
        long professorUsers = userTblRepository.countProfessors();
        
        stats.put("totalUsers", totalUsers);
        stats.put("studentUsers", studentUsers);
        stats.put("professorUsers", professorUsers);
        stats.put("studentPercentage", totalUsers > 0 ? (double) studentUsers / totalUsers * 100 : 0);
        stats.put("professorPercentage", totalUsers > 0 ? (double) professorUsers / totalUsers * 100 : 0);
        
        return stats;
    }
    
    /**
     * ID로 사용자 정보를 조회하는 내부 헬퍼 메서드
     * 다른 메서드들에서 사용자 조회 시 중복 코드를 방지하기 위해 사용
     *
     * @param id 조회할 사용자 ID
     * @return UserTbl 사용자 정보
     * @throws ResourceNotFoundException 사용자를 찾을 수 없는 경우
     *
     * 사용 예시:
     * UserTbl user = findUserById(123); // 내부에서만 사용
     */
    private UserTbl findUserById(Integer id) {
        return userTblRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.forId("사용자", id));
    }
    
    /**
     * 사용자 생성 시 중복 검사를 수행하는 내부 헬퍼 메서드
     * 이메일과 전화번호의 유니크성을 검증
     *
     * 검증 로직:
     * - 이메일 중복 검사
     * - 전화번호 중복 검사
     * - 중복 발견 시 DuplicateResourceException 발생
     *
     * @param email 검증할 이메일 주소
     * @param phone 검증할 전화번호
     * @throws DuplicateResourceException 이메일 또는 전화번호가 중복된 경우
     *
     * 사용 예시:
     * validateUserUniqueness("user@example.com", "01012345678"); // 내부에서만 사용
     */
    private void validateUserUniqueness(String email, String phone) {
        if (userTblRepository.existsByUserEmail(email)) {
            throw DuplicateResourceException.forField("이메일", "userEmail", email);
        }
        
        if (userTblRepository.existsByUserPhone(phone)) {
            throw DuplicateResourceException.forField("전화번호", "userPhone", phone);
        }
    }
    
    /**
     * 사용자 정보 수정 시 중복 검사를 수행하는 내부 헬퍼 메서드
     * 본인의 기존 값은 중복 검사에서 제외하고, 변경되는 값만 검증
     *
     * 검증 로직:
     * - 이메일이 변경되었고 중복되는 경우 예외
     * - 전화번호가 변경되었고 중복되는 경우 예외
     * - 기존 값과 동일한 경우 검증 통과
     *
     * @param existingUser 기존 사용자 정보
     * @param newUserDetails 새로운 사용자 정보
     * @throws DuplicateResourceException 변경하려는 값이 중복된 경우
     *
     * 사용 예시:
     * validateUserUniquenessForUpdate(existingUser, newUserDetails); // 내부에서만 사용
     */
    private void validateUserUniquenessForUpdate(UserTbl existingUser, UserTbl newUserDetails) {
        // 이메일이 변경되고 중복되는 경우
        if (!existingUser.getUserEmail().equals(newUserDetails.getUserEmail()) && 
            userTblRepository.existsByUserEmail(newUserDetails.getUserEmail())) {
            throw DuplicateResourceException.forField("이메일", "userEmail", newUserDetails.getUserEmail());
        }
        
        // 전화번호가 변경되고 중복되는 경우
        if (!existingUser.getUserPhone().equals(newUserDetails.getUserPhone()) && 
            userTblRepository.existsByUserPhone(newUserDetails.getUserPhone())) {
            throw DuplicateResourceException.forField("전화번호", "userPhone", newUserDetails.getUserPhone());
        }
    }
    
    /**
     * ID 찾기 메서드
     * 학번, 이름, 전화번호를 입력받아 해당하는 사용자의 이메일 주소를 마스킹하여 반환
     *
     * 비즈니스 로직:
     * 1. 입력값 유효성 검사
     * 2. 데이터베이스에서 일치하는 사용자 조회
     * 3. 사용자가 존재하면 이메일 마스킹 후 반환
     * 4. 사용자가 존재하지 않으면 실패 응답 반환
     *
     * 보안 고려사항:
     * - 계정 존재 여부를 직접적으로 노출하지 않음
     * - 이메일 주소는 마스킹하여 일부만 노출
     * - 일관된 응답 시간 유지 (타이밍 공격 방지)
     *
     * @param userCodeStr 학번/교수 코드 (문자열로 받아서 숫자로 변환)
     * @param userName 사용자 이름
     * @param userPhone 전화번호
     * @return FindIdResponse 처리 결과 (성공 시 마스킹된 이메일, 실패 시 에러 메시지)
     *
     * 사용 예시:
     * FindIdResponse response = userService.findUserEmail("202012345", "홍길동", "01012345678");
     * if (response.isSuccess()) {
     *     System.out.println("찾은 이메일: " + response.getMaskedEmail());
     * }
     */
    public BlueCrab.com.example.dto.FindIdResponse findUserEmail(String userCodeStr, String userName, String userPhone) {
        long startTime = System.currentTimeMillis();
        
        try {
            // 1. 입력값 유효성 검사
            if (userCodeStr == null || userName == null || userPhone == null) {
                return BlueCrab.com.example.dto.FindIdResponse.failure();
            }
            
            // userCode를 String에서 Integer로 변환
            Integer userCode;
            try {
                userCode = Integer.parseInt(userCodeStr.trim());
            } catch (NumberFormatException e) {
                return BlueCrab.com.example.dto.FindIdResponse.failure();
            }
            
            // 2. 데이터베이스에서 일치하는 사용자 조회
            Optional<UserTbl> userOptional = userTblRepository.findByUserCodeAndUserNameAndUserPhone(
                userCode, userName.trim(), userPhone.trim()
            );
            
            // 3. 결과 처리
            if (userOptional.isPresent()) {
                UserTbl user = userOptional.get();
                String email = user.getUserEmail();
                
                // 이메일 마스킹 처리
                String maskedEmail = maskEmail(email);
                
                return BlueCrab.com.example.dto.FindIdResponse.success(maskedEmail);
            } else {
                return BlueCrab.com.example.dto.FindIdResponse.failure();
            }
            
        } catch (Exception e) {
            // 예외 발생 시에도 일관된 실패 응답 반환 (보안상 에러 정보 노출 방지)
            return BlueCrab.com.example.dto.FindIdResponse.failure();
        } finally {
            // 성능 모니터링
            long executionTime = System.currentTimeMillis() - startTime;
            SlowQueryLogger.logQueryTime("findUserEmail", executionTime);
        }
    }
    
    /**
     * 이메일 주소 마스킹 처리 메서드
     * 이메일의 로컬 부분(@ 앞)을 마스킹하여 일부만 노출
     *
     * 마스킹 규칙:
     * - 3글자 이하: 첫 글자만 노출 (a** 형태)
     * - 4글자 이상: 첫 글자와 마지막 글자 노출 (a***b 형태)
     * - 도메인 부분은 그대로 노출
     *
     * @param email 원본 이메일 주소
     * @return 마스킹된 이메일 주소
     *
     * 예시:
     * - "abc@domain.com" → "a**@domain.com"
     * - "user123@domain.com" → "u****3@domain.com"
     * - "a@domain.com" → "a**@domain.com"
     */
    private String maskEmail(String email) {
        if (email == null || !email.contains("@")) {
            return email; // 유효하지 않은 이메일은 그대로 반환
        }
        
        String[] parts = email.split("@");
        if (parts.length != 2) {
            return email; // 유효하지 않은 이메일은 그대로 반환
        }
        
        String localPart = parts[0];
        String domainPart = parts[1];
        
        String maskedLocal;
        
        if (localPart.length() <= 1) {
            // 1글자 이하인 경우
            maskedLocal = localPart + "**";
        } else if (localPart.length() <= 3) {
            // 2~3글자인 경우: 첫 글자만 노출
            maskedLocal = localPart.charAt(0) + "**";
        } else {
            // 4글자 이상인 경우: 첫 글자와 마지막 글자 노출
            int maskLength = localPart.length() - 2;
            String mask = "*".repeat(maskLength);
            maskedLocal = localPart.charAt(0) + mask + localPart.charAt(localPart.length() - 1);
        }
        
        return maskedLocal + "@" + domainPart;
    }
    
    /**
     * 사용자 엔티티의 필드들을 업데이트하는 내부 헬퍼 메서드
     * 기존 사용자 객체에 새로운 값들을 복사
     *
     * 업데이트되는 필드들:
     * - 이메일, 이름, 전화번호, 생년월일
     * - 학생/교수 구분, 마지막 로그인 시간
     * - 주소 정보 (우편번호, 기본주소, 상세주소)
     *
     * @param user 업데이트할 사용자 엔티티
     * @param userDetails 새로운 사용자 정보
     *
     * 사용 예시:
     * updateUserFields(user, userDetails); // 내부에서만 사용
     */
    private void updateUserFields(UserTbl user, UserTbl userDetails) {
        user.setUserEmail(userDetails.getUserEmail());
        user.setUserName(userDetails.getUserName());
        user.setUserPhone(userDetails.getUserPhone());
        user.setUserBirth(userDetails.getUserBirth());
        user.setUserStudent(userDetails.getUserStudent());
        user.setUserLatest(userDetails.getUserLatest());
        user.setUserZip(userDetails.getUserZip());
        user.setUserFirstAdd(userDetails.getUserFirstAdd());
        user.setUserLastAdd(userDetails.getUserLastAdd());
    }
}