/* 작업자 : 성태준
 * 사용자 이름 추출 전용 유틸리티 클래스
 * JWT 인증 정보 또는 이메일을 통해 DB에서 실제 사용자 이름을 조회
 * 일반 사용자(UserTbl)와 관리자(AdminTbl) 모두 지원하는 범용 유틸리티
 */

package BlueCrab.com.example.util;

// ========== 임포트 구문 ==========
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;

import BlueCrab.com.example.entity.UserTbl;
import BlueCrab.com.example.entity.AdminTbl;
import BlueCrab.com.example.repository.UserTblRepository;
import BlueCrab.com.example.repository.AdminTblRepository;

@Component
@Slf4j
public class UserNameExtractor {
    
    // ========== 기본값 상수 ==========
    private static final String DEFAULT_USER_NAME = "학생";
    // 일반 사용자 이름 조회 실패 시 기본값
    private static final String DEFAULT_ADMIN_NAME = "관리자";
    // 관리자 이름 조회 실패 시 기본값
    
    // ========== 의존성 주입 ==========
    @Autowired
    private UserTblRepository userTblRepository;
    // 일반 사용자 정보 조회를 위한 리포지토리
    
    @Autowired
    private AdminTblRepository adminTblRepository;
    // 관리자 정보 조회를 위한 리포지토리
    
    public String extractUserNameFromAuthentication(Authentication authentication) {
        if (authentication == null) {
            log.warn("Authentication is null, using default user name");
            return DEFAULT_USER_NAME;
        }
        
        String userEmail = authentication.getName();
        // JWT 토큰의 subject(sub) 클레임에서 이메일 추출
        
        return extractUserNameFromEmail(userEmail);
    }
    
    /* 이메일로 일반 사용자 이름 추출
     * JWT 없이도 사용 가능한 범용 메서드
     * 
     * @param userEmail 사용자 이메일 주소
     * @return String 사용자 이름 (조회 실패 시 기본값)
     */
    public String extractUserNameFromEmail(String userEmail) {
        if (userEmail == null || userEmail.trim().isEmpty()) {
            log.warn("User email is null or empty, using default name");
            return DEFAULT_USER_NAME;
        }
        
        try {
            log.debug("Extracted user email from JWT: {}", userEmail);
            // 추출된 이메일 로그 기록
            
            Optional<UserTbl> userOptional = userTblRepository.findByUserEmail(userEmail);
            // 이메일로 데이터베이스에서 사용자 정보 조회
            log.debug("DB query executed for user email: {}", userEmail);
            // DB 조회 시도 로그 기록
            
            if (userOptional.isPresent()) {
                // 사용자가 존재하는 경우
                UserTbl user = userOptional.get();
                // Optional에서 실제 사용자 객체 추출
                String dbUserName = user.getUserName();
                // DB에서 조회한 사용자 이름
                
                if (dbUserName != null && !dbUserName.trim().isEmpty()) {
                    // DB에서 조회한 이름이 유효한 경우
                    log.debug("User name extracted from DB - Email: {}, Name: {}", userEmail, dbUserName);
                    // 성공 로그 기록
                    return dbUserName;
                    // 조회한 이름 반환
                } else {
                    // DB에 이름이 없거나 빈 문자열인 경우
                    log.debug("Failed to extract user name from DB - Email: {}", userEmail);
                    // 디버그 로그 기록
                }
            } else {
                // 사용자가 존재하지 않는 경우
                log.warn("User not found in DB - Email: {}, Using default", userEmail);
                // 경고 로그 기록
            }
            
        } catch (Exception e) {
            // 예외 발생 시 기본값 유지
            log.error("Error occurred while extracting user name from DB: {}", e.getMessage());
            // 오류 로그 기록
        }
        
        return DEFAULT_USER_NAME;
        // 최종 사용자 이름 반환 (DB 조회 성공 시 실제 이름, 실패 시 기본값)
    }
    
    /* 관리자 ID로 관리자 이름 추출
     * 향후 AdminEmailVerification에서 사용 가능
     * 
     * @param adminId 관리자 ID (이메일)
     * @return String 관리자 이름 (조회 실패 시 기본값)
     */
    public String extractAdminNameFromId(String adminId) {
        if (adminId == null || adminId.trim().isEmpty()) {
            log.warn("Admin ID is null or empty, using default name");
            return DEFAULT_ADMIN_NAME;
        }
        
        try {
            log.debug("Extracting admin name for ID: {}", adminId);
            // 추출 시도 로그 기록
            
            Optional<AdminTbl> adminOptional = adminTblRepository.findByAdminId(adminId);
            // 관리자 ID로 데이터베이스에서 관리자 정보 조회
            log.debug("DB query executed for admin ID: {}", adminId);
            // DB 조회 시도 로그 기록
            
            if (adminOptional.isPresent()) {
                // 관리자가 존재하는 경우
                AdminTbl admin = adminOptional.get();
                // Optional에서 실제 관리자 객체 추출
                String adminName = admin.getName();
                // DB에서 조회한 관리자 이름
                
                if (adminName != null && !adminName.trim().isEmpty()) {
                    // DB에서 조회한 이름이 유효한 경우
                    log.debug("Admin name extracted from DB - ID: {}, Name: {}", adminId, adminName);
                    // 성공 로그 기록
                    return adminName;
                    // 조회한 이름 반환
                } else {
                    // DB에 이름이 없거나 빈 문자열인 경우
                    log.debug("Failed to extract admin name from DB - ID: {}", adminId);
                    // 디버그 로그 기록
                }
            } else {
                // 관리자가 존재하지 않는 경우
                log.warn("Admin not found in DB - ID: {}, Using default", adminId);
                // 경고 로그 기록
            }
            
        } catch (Exception e) {
            // 예외 발생 시 기본값 유지
            log.error("Error occurred while extracting admin name from DB: {}", e.getMessage());
            // 오류 로그 기록
        }
        
        return DEFAULT_ADMIN_NAME;
        // 최종 관리자 이름 반환 (DB 조회 성공 시 실제 이름, 실패 시 기본값)
    }
    
    /* 기본 사용자 이름 반환
     * 테스트나 특수한 경우에 사용
     * 
     * @return String 기본 사용자 이름
     */
    public String getDefaultUserName() {
        return DEFAULT_USER_NAME;
    }
    
    /* 기본 관리자 이름 반환
     * 테스트나 특수한 경우에 사용
     * 
     * @return String 기본 관리자 이름  
     */
    public String getDefaultAdminName() {
        return DEFAULT_ADMIN_NAME;
    }
}
