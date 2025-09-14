package BlueCrab.com.example.repository;

import BlueCrab.com.example.entity.AdminTbl;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * 관리자 정보 데이터베이스 접근을 위한 JPA 리포지토리 인터페이스
 * AdminTbl 엔티티에 대한 CRUD 작업 및 커스텀 쿼리를 제공
 *
 * 주요 기능:
 * - 관리자 조회 (관리자ID, 이메일)
 * - 관리자 계정 상태 관리
 * - 정지된 관리자 계정 조회
 * - 관리자 존재 여부 확인
 *
 * 사용되는 주요 시나리오:
 * - 관리자 로그인 시 관리자ID로 조회
 * - 계정 상태 확인 및 관리
 * - 정지 기간 만료된 계정 자동 복구
 * - 관리자 등록 시 중복 확인
 */
@Repository
public interface AdminTblRepository extends JpaRepository<AdminTbl, Integer> {

    /**
     * 관리자 ID로 관리자 정보 조회
     * 로그인 시 사용
     * 
     * @param adminId 관리자 로그인 ID
     * @return Optional<AdminTbl> 관리자 정보 (존재하지 않으면 empty)
     */
    Optional<AdminTbl> findByAdminId(String adminId);

    /**
     * 관리자 ID 존재 여부 확인
     * 관리자 등록 시 중복 체크용
     * 
     * @param adminId 관리자 로그인 ID
     * @return boolean 존재하면 true, 없으면 false
     */
    boolean existsByAdminId(String adminId);

    // TODO: 계정 상태 관련 필드들이 AdminTbl 엔티티에 추가되면 아래 메서드들을 활성화할 예정
    // 현재는 AdminTbl에 status, suspendUntil, lastLoginAt 필드가 없어 주석 처리
    
    /*
     * 특정 상태의 관리자 목록 조회
     * 
     * @param status 계정 상태 (active, suspended, banned)
     * @return List<AdminTbl> 해당 상태의 관리자 목록
     */
    // List<AdminTbl> findByStatus(String status);

    /*
     * 정지 기간이 만료된 관리자 계정 조회
     * 배치 작업에서 자동 복구에 사용
     * 
     * @param now 현재 시간
     * @return List<AdminTbl> 정지 기간이 만료된 관리자 목록
     */
    // @Query("SELECT a FROM AdminTbl a WHERE a.status = 'suspended' AND a.suspendUntil <= :now")
    // List<AdminTbl> findSuspendedUntilBefore(@Param("now") LocalDateTime now);

    /*
     * 활성 상태의 관리자 수 조회
     * 통계용
     * 
     * @return long 활성 관리자 수
     */
    // @Query("SELECT COUNT(a) FROM AdminTbl a WHERE a.status = 'active'")
    // long countActiveAdmins();

    /*
     * 정지 상태의 관리자 수 조회
     * 통계용
     * 
     * @return long 정지된 관리자 수
     */
    // @Query("SELECT COUNT(a) FROM AdminTbl a WHERE a.status = 'suspended'")
    // long countSuspendedAdmins();

    /*
     * 특정 기간 이후에 마지막 로그인한 관리자 조회
     * 비활성 계정 관리용
     * 
     * @param date 기준 날짜
     * @return List<AdminTbl> 해당 기간 이후 로그인한 관리자 목록
     */
    // @Query("SELECT a FROM AdminTbl a WHERE a.lastLoginAt >= :date")
    // List<AdminTbl> findByLastLoginAtAfter(@Param("date") LocalDateTime date);
}
