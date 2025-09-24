package BlueCrab.com.example.security;

// 명시적 import 추가
import BlueCrab.com.example.entity.UserTbl;
import BlueCrab.com.example.repository.UserTblRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Spring Security의 UserDetailsService를 구현한 커스텀 사용자 인증 서비스
 * 사용자 이메일로 인증을 수행하고, 역할 기반 권한을 부여하는 핵심 컴포넌트
 *
 * 주요 기능:
 * - 사용자명(이메일)으로 사용자 정보 조회
 * - 계정 활성화 상태 검증
 * - 역할 기반 권한 부여 (학생/교수/관리자 구분)
 * - Spring Security UserDetails 객체 생성
 *
 * 권한 체계:
 * - ROLE_USER: 모든 로그인 사용자에게 부여되는 기본 권한
 * - ROLE_STUDENT: 학생 사용자 (userStudent = 1)
 * - ROLE_PROFESSOR: 교수 사용자 (userStudent = 0)
 * - ROLE_ADMIN: 관리자 권한 (이메일이 "prof01"로 시작하는 경우)
 *
 * 작동 방식:
 * 1. Spring Security가 로그인 요청을 받음
 * 2. 이 서비스의 loadUserByUsername() 호출
 * 3. 사용자 정보 조회 및 검증
 * 4. 권한 정보 생성
 * 5. UserDetails 객체 반환
 * 6. Spring Security가 인증/인가 처리 수행
 *
 * 보안 고려사항:
 * - 사용자명은 이메일로 사용하여 유니크성 보장
 * - 계정 비활성화 상태 확인으로 보안 강화
 * - 권한 정보는 데이터베이스에서 실시간 조회
 *
 * ⚠️ 추후 변경 가능성:
 * - 관리자 권한 기준이 이메일 접두사에서 별도 필드로 변경될 수 있음
 * - 권한 체계가 더 세부적으로 확장될 수 있음 (학과별, 기능별 권한)
 * - JWT 토큰에 권한 정보를 포함하는 방식으로 변경될 수 있음
 * - 캐싱 메커니즘이 추가될 수 있음 (권한 정보 캐시)
 *
 * 의존성:
 * - UserTblRepository: 사용자 정보 조회
 * - UserTbl.isActive(): 계정 활성화 상태 확인
 *
 * @author BlueCrab Development Team
 * @version 1.0.0
 * @since 2024-01-01
 */
@Service
public class CustomUserDetailsService implements UserDetailsService {

    /**
     * 사용자 리포지토리
     * 이메일로 사용자 정보를 조회하기 위한 의존성 주입
     */
    @Autowired
    private UserTblRepository userTblRepository;

    /**
     * 사용자명(이메일)으로 사용자 정보를 로드하는 핵심 메서드
     * Spring Security의 인증 프로세스에서 자동으로 호출됨
     *
     * 처리 단계:
     * 1. 이메일로 데이터베이스에서 사용자 조회
     * 2. 사용자 존재 여부 확인 (없으면 예외 발생)
     * 3. 계정 활성화 상태 확인 (비활성화 시 예외 발생)
     * 4. 권한 정보 생성
     * 5. Spring Security UserDetails 객체 생성 및 반환
     *
     * Spring Security UserDetails 필드 설명:
     * - username: 사용자명 (이메일 주소)
     * - password: 비밀번호 (해싱된 값)
     * - enabled: 계정 활성화 상태
     * - accountNonExpired: 계정 만료 여부
     * - credentialsNonExpired: 자격 증명 만료 여부
     * - accountNonLocked: 계정 잠금 여부
     * - authorities: 부여된 권한 목록
     *
     * 예외 발생 상황:
     * - UsernameNotFoundException: 사용자를 찾을 수 없는 경우
     * - UsernameNotFoundException: 계정이 비활성화된 경우
     *
     * @param username 로그인 시도한 사용자명 (이메일 주소)
     * @return UserDetails Spring Security가 사용할 사용자 정보 객체
     * @throws UsernameNotFoundException 사용자를 찾을 수 없거나 계정이 비활성화된 경우
     *
     * 사용 예시:
     * // Spring Security가 자동으로 호출
     * UserDetails userDetails = customUserDetailsService.loadUserByUsername("student@university.edu");
     * // 이후 Spring Security가 인증/인가 처리 수행
     */
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        // 단계 1: 이메일로 사용자 조회
        // 데이터베이스에서 이메일로 사용자 정보 검색
        // 존재하지 않으면 UsernameNotFoundException 발생
        UserTbl user = userTblRepository.findByUserEmail(username)
            .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));

        // 단계 2: 계정 활성화 상태 확인
        // UserTbl.isActive() 메서드로 계정 상태 검증
        // 비활성화된 계정은 로그인 불가
        if (!user.isActive()) {
            throw new UsernameNotFoundException("User account is deactivated: " + username);
        }

        // 단계 3: Spring Security UserDetails 객체 생성 및 반환
        // 사용자 이메일, 비밀번호, 권한 정보를 포함한 User 객체 생성
        // 모든 계정 상태 플래그는 true로 설정 (현재 시스템에서는 사용하지 않음)
        return new org.springframework.security.core.userdetails.User(
            user.getUserEmail(),        // 사용자명 (이메일)
            user.getUserPw(),           // 비밀번호 (해싱된 값)
            true,                       // enabled: 계정 활성화 상태
            true,                       // accountNonExpired: 계정 만료 여부
            true,                       // credentialsNonExpired: 자격 증명 만료 여부
            true,                       // accountNonLocked: 계정 잠금 여부
            getAuthorities(user)        // 부여된 권한 목록
        );
    }

    /**
     * 사용자에게 부여할 권한 목록을 생성하는 메서드
     * 역할 기반 접근 제어(RBAC)를 위한 권한 정보를 생성
     *
     * 권한 부여 로직:
     * 1. 모든 사용자에게 기본 권한 "ROLE_USER" 부여
     * 2. userStudent 값에 따라 학생/교수 권한 부여
     * 3. 이메일 접두사에 따라 관리자 권한 부여
     *
     * 현재 권한 체계:
     * - ROLE_USER: 기본 사용자 권한 (모든 로그인 사용자)
     * - ROLE_STUDENT: 학생 전용 권한 (userStudent = 1)
     * - ROLE_PROFESSOR: 교수 전용 권한 (userStudent = 0)
     * - ROLE_ADMIN: 관리자 권한 (이메일이 "prof01"로 시작)
     *
     * ⚠️ 추후 변경 가능성:
     * - 관리자 권한 기준이 이메일에서 별도 필드(userRole 등)로 변경될 수 있음
     * - 더 세부적인 권한 체계 도입 가능 (학과별, 기능별 권한)
     * - 데이터베이스 테이블로 권한 관리하는 방식으로 확장될 수 있음
     * - 권한 캐싱 메커니즘이 추가될 수 있음
     *
     * @param user 권한을 부여할 사용자 정보
     * @return Collection<? extends GrantedAuthority> 부여된 권한 목록
     *
     * 사용 예시:
     * Collection<GrantedAuthority> authorities = getAuthorities(user);
     * // ["ROLE_USER", "ROLE_STUDENT"] 형태의 권한 목록 반환
     */
    private Collection<? extends GrantedAuthority> getAuthorities(UserTbl user) {
        // 권한 목록을 저장할 리스트 생성
        List<GrantedAuthority> authorities = new ArrayList<>();
        
        // 단계 1: 기본 권한 부여
        // 모든 로그인 사용자에게 부여되는 기본 권한
        authorities.add(new SimpleGrantedAuthority("ROLE_USER"));
        
        // 단계 2: 학생/교수 구분 권한 부여
        // userStudent 필드 값에 따라 역할 구분
        // 0: 학생, 1: 교수
        if (user.getUserStudent() == 0) {
            authorities.add(new SimpleGrantedAuthority("ROLE_STUDENT"));
        } else {
            authorities.add(new SimpleGrantedAuthority("ROLE_PROFESSOR"));
        }
        
        // 단계 3: 관리자 권한 부여
        // 이메일이 "prof01"로 시작하는 경우 관리자 권한 부여
        // ⚠️ 추후 변경 가능성 높음: 별도 관리자 플래그 필드 도입 예정
        if (user.getUserEmail().toLowerCase().startsWith("prof01")) {
            authorities.add(new SimpleGrantedAuthority("ROLE_ADMIN"));
        }
        
        // 생성된 권한 목록 반환
        return authorities;
    }
}
