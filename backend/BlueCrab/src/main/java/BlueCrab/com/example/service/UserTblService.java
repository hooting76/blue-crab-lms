
package BlueCrab.com.example.service;

import BlueCrab.com.example.dto.UserCreationRequestDTO;
import BlueCrab.com.example.entity.RegistryTbl;
import BlueCrab.com.example.entity.SerialCodeTable;
import BlueCrab.com.example.entity.UserTbl;
import BlueCrab.com.example.exception.DuplicateResourceException;
import BlueCrab.com.example.exception.ResourceNotFoundException;
import BlueCrab.com.example.repository.RegistryRepository;
import BlueCrab.com.example.repository.SerialCodeTableRepository;
import BlueCrab.com.example.repository.UserTblRepository;
import BlueCrab.com.example.util.SlowQueryLogger;
import BlueCrab.com.example.util.UserCodeGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@Transactional
public class UserTblService {

    private static final DateTimeFormatter REG_TIMESTAMP_FORMATTER =
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private static final Logger logger = LoggerFactory.getLogger(UserTblService.class);

    private static final long PROFILE_IMAGE_MAX_SIZE = 15L * 1024 * 1024;

    private static final List<String> ALLOWED_IMAGE_EXTENSIONS =
        Arrays.asList("jpg", "jpeg", "png", "gif");

    private static final List<String> ALLOWED_IMAGE_MIME_TYPES =
        Arrays.asList("image/jpeg", "image/png", "image/gif", "image/jpg");

    @Autowired
    private UserTblRepository userTblRepository;

    @Autowired
    private SerialCodeTableRepository serialCodeTableRepository;

    @Autowired
    private RegistryRepository registryRepository;

    @Autowired
    private UserCodeGenerator userCodeGenerator;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private MinIOService minioService;

    @Autowired
    private ImageCacheService imageCacheService;

    public List<UserTbl> getAllUsers() {
        return SlowQueryLogger.measureAndLog("getAllUsers",
            () -> userTblRepository.findAll());
    }

    public List<UserTbl> getStudentUsers() {
        return SlowQueryLogger.measureAndLog("getStudentUsers",
            () -> userTblRepository.findByUserStudent(0));
    }

    public List<UserTbl> getProfessorUsers() {
        return userTblRepository.findByUserStudent(1);
    }

    public Optional<UserTbl> getUserById(Integer id) {
        return userTblRepository.findById(id);
    }

    public Optional<UserTbl> getUserByEmail(String email) {
        return userTblRepository.findByUserEmail(email);
    }

    public UserTbl createUser(UserCreationRequestDTO dto) {
        validateUserUniqueness(dto.getUserEmail(), dto.getUserPhone());

        UserTbl user = dto.toUserTblEntity();
        user.setUserPw(passwordEncoder.encode(user.getUserPw()));
        user.setUserReg(LocalDateTime.now().format(REG_TIMESTAMP_FORMATTER));

        String userCode = generateUserCode(dto);
        user.setUserCode(userCode);

        UserTbl savedUser = userTblRepository.save(user);

        if (savedUser.getUserStudent() == 0) {
            persistSerialCode(savedUser, dto);
            persistRegistry(savedUser, userCode, dto);
        }

        return savedUser;
    }

    private void persistSerialCode(UserTbl savedUser, UserCreationRequestDTO dto) {
        if (!hasText(dto.getMajorFacultyCode()) || !hasText(dto.getMajorDeptCode())) {
            throw new IllegalArgumentException("학생 전공 정보가 누락되었습니다.");
        }

        SerialCodeTable serialCode = new SerialCodeTable();
        serialCode.setUserIdx(savedUser.getUserIdx());
        serialCode.setSerialCode(dto.getMajorFacultyCode());
        serialCode.setSerialSub(dto.getMajorDeptCode());

        if (hasText(dto.getMinorFacultyCode()) && hasText(dto.getMinorDeptCode())) {
            serialCode.setSerialCodeNd(dto.getMinorFacultyCode());
            serialCode.setSerialSubNd(dto.getMinorDeptCode());
        }

        serialCodeTableRepository.save(serialCode);
    }

    private void persistRegistry(UserTbl savedUser, String userCode, UserCreationRequestDTO dto) {
        RegistryTbl registry = new RegistryTbl();
        registry.setUser(savedUser);
        registry.setUserCode(userCode);

        if (hasText(dto.getJoinPath())) {
            registry.setJoinPath(dto.getJoinPath());
        }
        if (hasText(dto.getStdStat())) {
            registry.setStdStat(dto.getStdStat());
        } else {
            registry.setStdStat("재학");
        }

        registry.setCntTerm(dto.getCntTerm() != null ? dto.getCntTerm() : 0);
        registry.setStdRestDate(dto.getStdRestDate());
        registry.setAdminName(dto.getAdminName());
        registry.setAdminIp(dto.getAdminIp());
        registry.setAdminReg(LocalDateTime.now());

        registryRepository.save(registry);
    }

    private String generateUserCode(UserCreationRequestDTO dto) {
        UserCodeGenerator.UserType userType = resolveUserType(dto.getUserStudent());
        int facultyCode = parseTwoDigitCode(dto.getMajorFacultyCode());
        int departmentCode = parseTwoDigitCode(dto.getMajorDeptCode());

        Integer admissionYear = dto.getAdmissionYear();
        if (admissionYear != null) {
            return userCodeGenerator.generateUserCode(
                admissionYear,
                userType,
                facultyCode,
                departmentCode
            );
        }

        return userCodeGenerator.generateUserCode(userType, facultyCode, departmentCode);
    }

    private UserCodeGenerator.UserType resolveUserType(int userStudentFlag) {
        return userStudentFlag == 0
            ? UserCodeGenerator.UserType.STUDENT
            : UserCodeGenerator.UserType.PROFESSOR;
    }

    private int parseTwoDigitCode(String code) {
        if (!hasText(code)) {
            return 0;
        }
        try {
            return Integer.parseInt(code);
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("전공 코드는 숫자 2자리여야 합니다: " + code);
        }
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
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
        // 학생(0) ↔ 교수(1) 전환
        user.setUserStudent(user.getUserStudent() == 1 ? 0 : 1);
        return userTblRepository.save(user);
    }

    /**
     * 프로필 이미지 업로드 및 교체
     *
     * @param userEmail 프로필을 수정할 사용자 이메일
     * @param file      업로드할 이미지 파일
     * @return 신규 프로필 이미지 키
     */
    public String updateProfileImage(String userEmail, MultipartFile file) {
        validateImageFile(file);

        UserTbl user = userTblRepository.findByUserEmail(userEmail)
            .orElseThrow(() -> ResourceNotFoundException.forField("사용자", "userEmail", userEmail));

        Integer userIdx = user.getUserIdx();
        if (userIdx == null) {
            throw new IllegalStateException("사용자 식별자가 존재하지 않습니다.");
        }

        String oldImageKey = user.getProfileImageKey();
        String newImageKey = generateProfileImageKey(userIdx, file);

        logger.info("프로필 이미지 업로드 시작 - 사용자: {}, 기존 이미지: {}", userEmail, oldImageKey);

        minioService.uploadProfileImage(file, newImageKey);

        try {
            user.setProfileImageKey(newImageKey);
            userTblRepository.save(user);
        } catch (RuntimeException ex) {
            logger.error("프로필 이미지 DB 업데이트 실패 - 사용자: {}, 오류: {}", userEmail, ex.getMessage());
            rollbackUploadedProfileImage(newImageKey, userEmail);
            throw ex;
        }

        // 기존 이미지 캐시 및 파일 정리
        if (oldImageKey != null && !oldImageKey.trim().isEmpty()) {
            imageCacheService.evictImageCache(oldImageKey);
            deleteOldProfileImage(oldImageKey, userEmail);
        }

        // 사용자 캐시 전체 무효화
        imageCacheService.evictUserImageCache(userEmail);

        logger.info("프로필 이미지 업로드 완료 - 사용자: {}, 신규 이미지: {}", userEmail, newImageKey);
        return newImageKey;
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
    
    private void validateImageFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("업로드할 파일이 없습니다.");
        }

        if (file.getSize() > PROFILE_IMAGE_MAX_SIZE) {
            throw new IllegalArgumentException("파일 크기는 15MB를 초과할 수 없습니다.");
        }

        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || originalFilename.trim().isEmpty()) {
            throw new IllegalArgumentException("파일명이 유효하지 않습니다.");
        }

        String extension = getFileExtension(originalFilename).toLowerCase();
        if (!ALLOWED_IMAGE_EXTENSIONS.contains(extension)) {
            throw new IllegalArgumentException("허용되지 않는 파일 형식입니다. (jpg, jpeg, png, gif만 가능)");
        }

        String contentType = file.getContentType();
        String mimeType = contentType != null ? contentType.toLowerCase() : "";
        if (!ALLOWED_IMAGE_MIME_TYPES.contains(mimeType)) {
            throw new IllegalArgumentException("허용되지 않는 이미지 형식입니다. (JPEG, PNG, GIF만 가능)");
        }
    }

    private String generateProfileImageKey(Integer userIdx, MultipartFile file) {
        String extension = getFileExtension(file.getOriginalFilename()).toLowerCase();
        long timestamp = System.currentTimeMillis();
        return String.format("profile_%d_%d.%s", userIdx, timestamp, extension);
    }

    private String getFileExtension(String filename) {
        int lastDotIndex = filename != null ? filename.lastIndexOf('.') : -1;
        if (lastDotIndex == -1 || lastDotIndex == filename.length() - 1) {
            throw new IllegalArgumentException("파일 확장자를 찾을 수 없습니다.");
        }
        return filename.substring(lastDotIndex + 1);
    }

    private void deleteOldProfileImage(String oldImageKey, String userEmail) {
        try {
            minioService.deleteProfileImage(oldImageKey);
            logger.info("기존 프로필 이미지 삭제 완료 - 사용자: {}, 이미지: {}", userEmail, oldImageKey);
        } catch (RuntimeException ex) {
            logger.warn("기존 프로필 이미지 삭제 실패 - 사용자: {}, 이미지: {}, 오류: {}", userEmail, oldImageKey, ex.getMessage());
        }
    }

    private void rollbackUploadedProfileImage(String imageKey, String userEmail) {
        try {
            minioService.deleteProfileImage(imageKey);
            logger.info("신규 프로필 이미지 롤백 완료 - 사용자: {}, 이미지: {}", userEmail, imageKey);
        } catch (RuntimeException ex) {
            logger.warn("신규 프로필 이미지 롤백 실패 - 사용자: {}, 이미지: {}, 오류: {}", userEmail, imageKey, ex.getMessage());
        }
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
            
            // 2. 데이터베이스에서 일치하는 사용자 조회
            Optional<UserTbl> userOptional = userTblRepository.findByUserCodeAndUserNameAndUserPhone(
                userCodeStr.trim(), userName.trim(), userPhone.trim()
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

    /**
     * 사용자 주소 정보 업데이트
     * JWT 인증된 사용자의 주소 정보를 업데이트하는 메서드
     *
     * 처리 단계:
     * 1. 사용자 이메일로 UserTbl 조회
     * 2. 우편번호를 String에서 Integer로 변환
     * 3. 주소 필드 업데이트 (USER_ZIP, USER_FIRST_ADD, USER_LAST_ADD)
     * 4. 데이터베이스에 저장 (dirty checking으로 자동 UPDATE)
     *
     * 데이터베이스 매핑:
     * - postalCode (String) → USER_ZIP (Integer)
     * - roadAddress (String) → USER_FIRST_ADD (VARCHAR 200)
     * - detailAddress (String) → USER_LAST_ADD (VARCHAR 100)
     *
     * 비즈니스 규칙:
     * - 우편번호는 5자리 숫자 문자열을 Integer로 변환
     * - 상세주소는 선택 사항 (null 허용, 빈 문자열로 저장)
     * - 트랜잭션 내에서 실행되어 롤백 가능
     *
     * @param userEmail 사용자 이메일 (JWT 토큰에서 추출)
     * @param postalCode 우편번호 (5자리 숫자 문자열, 예: "05852")
     * @param roadAddress 도로명주소 (예: "서울 송파구 위례광장로 120")
     * @param detailAddress 상세주소 (선택, 예: "장지동, 위례중앙푸르지오1단지")
     * @throws ResourceNotFoundException 사용자를 찾을 수 없는 경우
     * @throws IllegalArgumentException 우편번호 형식이 잘못된 경우
     *
     * 사용 예시:
     * userTblService.updateUserAddress(
     *     "user@example.com",
     *     "05852",
     *     "서울 송파구 위례광장로 120",
     *     "장지동, 위례중앙푸르지오1단지"
     * );
     * // 주소 업데이트 완료
     */
    @Transactional
    public void updateUserAddress(String userEmail, String postalCode,
                                  String roadAddress, String detailAddress) {

        logger.info("주소 업데이트 시작 - 사용자: {}, 우편번호: {}", userEmail, postalCode);

        // 1. 사용자 조회
        UserTbl user = userTblRepository.findByUserEmail(userEmail)
            .orElseThrow(() -> new ResourceNotFoundException(
                "사용자를 찾을 수 없습니다: " + userEmail));

        // 2. 우편번호 변환 (String → Integer)
        Integer zipCode = null;
        try {
            zipCode = Integer.parseInt(postalCode);
        } catch (NumberFormatException e) {
            logger.warn("유효하지 않은 우편번호 형식 - 사용자: {}, 우편번호: {}",
                       userEmail, postalCode);
            throw new IllegalArgumentException("유효하지 않은 우편번호 형식입니다: " + postalCode);
        }

        // 3. 주소 필드 업데이트
        user.setUserZip(zipCode);
        user.setUserFirstAdd(roadAddress);
        user.setUserLastAdd(detailAddress != null ? detailAddress : "");

        // 4. DB 저장 (dirty checking으로 자동 UPDATE)
        userTblRepository.save(user);

        logger.info("주소 업데이트 완료 - 사용자: {}, 우편번호: {}, 도로명: {}",
                   userEmail, postalCode, roadAddress);
    }
}
