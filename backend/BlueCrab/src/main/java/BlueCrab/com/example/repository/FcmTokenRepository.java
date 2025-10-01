package BlueCrab.com.example.repository;

import BlueCrab.com.example.entity.FcmToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface FcmTokenRepository extends JpaRepository<FcmToken, Integer> {

    /**
     * 사용자 ID로 FCM 토큰 정보 조회
     */
    Optional<FcmToken> findByUserIdx(Integer userIdx);

    /**
     * 사용자 코드로 FCM 토큰 정보 조회
     */
    Optional<FcmToken> findByUserCode(String userCode);

    /**
     * 특정 FCM 토큰이 등록되어 있는지 확인 (모든 플랫폼 검색)
     */
    @Query("SELECT f FROM FcmToken f WHERE f.fcmTokenAndroid = :token OR f.fcmTokenIos = :token OR f.fcmTokenWeb = :token")
    Optional<FcmToken> findByAnyToken(@Param("token") String token);

    /**
     * 안드로이드 토큰으로 검색
     */
    Optional<FcmToken> findByFcmTokenAndroid(String fcmTokenAndroid);

    /**
     * iOS 토큰으로 검색
     */
    Optional<FcmToken> findByFcmTokenIos(String fcmTokenIos);

    /**
     * 웹 토큰으로 검색
     */
    Optional<FcmToken> findByFcmTokenWeb(String fcmTokenWeb);

    /**
     * 특정 기간 이상 미사용 토큰 조회 (배치 정리용)
     */
    @Query("SELECT f FROM FcmToken f WHERE " +
           "(f.fcmTokenAndroid IS NOT NULL AND (f.fcmTokenAndroidLastUsed IS NULL OR f.fcmTokenAndroidLastUsed < :cutoffDate)) OR " +
           "(f.fcmTokenIos IS NOT NULL AND (f.fcmTokenIosLastUsed IS NULL OR f.fcmTokenIosLastUsed < :cutoffDate)) OR " +
           "(f.fcmTokenWeb IS NOT NULL AND (f.fcmTokenWebLastUsed IS NULL OR f.fcmTokenWebLastUsed < :cutoffDate))")
    List<FcmToken> findInactiveTokens(@Param("cutoffDate") LocalDateTime cutoffDate);

    /**
     * 모든 유효한 안드로이드 토큰 조회
     */
    @Query("SELECT f.fcmTokenAndroid FROM FcmToken f WHERE f.fcmTokenAndroid IS NOT NULL")
    List<String> findAllAndroidTokens();

    /**
     * 모든 유효한 iOS 토큰 조회
     */
    @Query("SELECT f.fcmTokenIos FROM FcmToken f WHERE f.fcmTokenIos IS NOT NULL")
    List<String> findAllIosTokens();

    /**
     * 모든 유효한 웹 토큰 조회
     */
    @Query("SELECT f.fcmTokenWeb FROM FcmToken f WHERE f.fcmTokenWeb IS NOT NULL")
    List<String> findAllWebTokens();
}
