# FCM 푸시 로직 코드 리뷰 보고서

**작성일:** 2025-10-04
**검토 범위:** FCM 토큰 관리 및 알림 전송 시스템
**검토자:** Claude Code

---

## 📋 목차

1. [전반적인 평가](#전반적인-평가)
2. [중요 이슈 (Critical)](#중요-이슈-critical)
3. [개선 권장사항 (Important)](#개선-권장사항-important)
4. [좋은 점](#좋은-점)
5. [테스트 시나리오](#테스트-시나리오)
6. [우선순위 수정 항목](#우선순위-수정-항목)
7. [최종 권장사항](#최종-권장사항)

---

## 전반적인 평가

✅ **종합 평가:** 구현된 FCM 푸시 로직은 전반적으로 잘 설계되었으며, 멀티 기기 관리, 충돌 감지, Redis 기반 임시 토큰 관리 등 복잡한 요구사항을 체계적으로 처리하고 있습니다.

**주요 강점:**
- 체계적인 충돌 감지 및 해결 메커니즘
- Redis를 활용한 임시 세션 토큰 관리
- 자동 무효 토큰 정리 및 스케줄링
- 명확한 트랜잭션 관리 및 로깅

**주요 개선 필요 사항:**
- Redis 임시 토큰이 일부 전송 경로에서 누락됨 (일괄 전송, 브로드캐스트)
- 일부 엔티티 저장 로직 누락
- N+1 쿼리 최적화 필요

---

## 🔴 중요 이슈 (Critical)

### 1. registerForce 메서드에서 엔티티 저장 누락

**파일:** `FcmTokenService.java:247`

**문제:**
```java
FcmToken fcmTokenEntity = fcmTokenRepository.findByUserIdx(user.getUserIdx())
        .orElseGet(() -> new FcmToken(user.getUserIdx(), user.getUserCode()));
```

`orElseGet()`에서 새 엔티티를 생성했지만 **저장하지 않고** 바로 사용합니다.

**영향:**
신규 사용자가 강제 등록(`registerForce`)을 시도할 때 `detached entity passed to persist` 예외 발생 가능

**해결 방법:**
```java
FcmToken fcmTokenEntity = fcmTokenRepository.findByUserIdx(user.getUserIdx())
        .orElseGet(() -> {
            FcmToken newToken = new FcmToken(user.getUserIdx(), user.getUserCode());
            return fcmTokenRepository.save(newToken);
        });
```

**우선순위:** 🔴 Critical (즉시 수정 필요)

---

### 2. 일괄 전송에서 Redis 임시 토큰 미처리

**파일:** `FcmTokenService.java:498-519`

**문제:**
```java
for (String platform : targetPlatforms) {
    String token = fcmToken.getTokenByPlatform(platform);  // ← DB 토큰만 확인
    String key = platform.toLowerCase(Locale.ROOT);
    if (token == null) {
        sent.put(key, false);
        failed.put(key, "토큰이 등록되지 않았습니다");
        totalFailure++;
    } else {
        DeliveryAttemptResult result = sendToSingleToken(token, ...);
        // ...
    }
}
```

일괄 전송(`sendBatchNotification`) 메서드에서 **Redis 임시 토큰을 전혀 확인하지 않음**

**영향:**
- 임시 기기로 로그인한 사용자는 일괄 알림을 받지 못함
- `sendNotification()` (개별 전송)에서는 Redis 토큰을 확인하는데, 일괄 전송에서는 누락됨
- 기능 일관성 문제

**해결 방법:**
```java
for (String platform : targetPlatforms) {
    String dbToken = fcmToken.getTokenByPlatform(platform);
    String tempToken = fcmSessionService.getTemporaryToken(fcmToken.getUserIdx(), platform);
    String key = platform.toLowerCase(Locale.ROOT);

    List<DeliveryAttemptResult> attempts = new ArrayList<>();

    // DB 토큰 전송
    if (dbToken != null) {
        attempts.add(sendToSingleToken(dbToken, request.getTitle(), request.getBody(),
                request.getData(), fcmToken, platform));
    }

    // Redis 임시 토큰 전송 (DB 토큰과 다른 경우만)
    if (tempToken != null && !Objects.equals(tempToken, dbToken)) {
        attempts.add(sendToSingleToken(tempToken, request.getTitle(), request.getBody(),
                request.getData(), fcmToken, platform));
    }

    // 결과 처리
    if (attempts.isEmpty()) {
        sent.put(key, false);
        failed.put(key, "토큰이 등록되지 않았습니다");
        totalFailure++;
    } else {
        boolean success = attempts.stream().anyMatch(DeliveryAttemptResult::isSuccess);
        sent.put(key, success);

        if (success) {
            totalSuccess++;
            failed.remove(key);
        } else {
            String reason = attempts.stream()
                    .map(DeliveryAttemptResult::getFailureReason)
                    .filter(Objects::nonNull)
                    .distinct()
                    .collect(Collectors.joining("; "));
            failed.put(key, reason.isEmpty() ? "전송 실패" : reason);
            totalFailure++;
        }
    }
}
```

**우선순위:** 🔴 Critical (즉시 수정 필요)

---

### 3. 브로드캐스트에서 Redis 임시 토큰 미수집

**파일:** `FcmTokenService.java:552-559`

**문제:**
```java
// 플랫폼별로 토큰 수집
for (FcmToken fcmToken : allTokens) {
    for (String platform : targetPlatforms) {
        String token = fcmToken.getTokenByPlatform(platform);  // ← DB 토큰만 수집
        if (token != null) {
            allValidTokens.add(token);
        }
    }
}
```

브로드캐스트(`sendBroadcast`) 메서드에서도 **Redis 임시 토큰을 수집하지 않음**

**영향:**
- 임시 로그인 사용자는 전체 공지/브로드캐스트 알림을 받지 못함
- 긴급 공지 시 일부 로그인 사용자가 누락될 수 있음

**해결 방법:**
```java
LinkedHashSet<String> allValidTokens = new LinkedHashSet<>();
List<String> invalidTokens = new ArrayList<>();

// 플랫폼별로 토큰 수집
for (FcmToken fcmToken : allTokens) {
    for (String platform : targetPlatforms) {
        // DB 토큰 수집
        String dbToken = fcmToken.getTokenByPlatform(platform);
        if (dbToken != null) {
            allValidTokens.add(dbToken);
        }

        // Redis 임시 토큰 수집 (DB 토큰과 다른 경우만)
        String tempToken = fcmSessionService.getTemporaryToken(fcmToken.getUserIdx(), platform);
        if (tempToken != null && !Objects.equals(tempToken, dbToken)) {
            allValidTokens.add(tempToken);
        }
    }
}
```

**우선순위:** 🔴 Critical (즉시 수정 필요)

---

### 4. API 경로 불일치 (문서 vs 구현)

**파일:** `FcmTokenController.java:40`

**문제:**
```java
@PostMapping("/register/force")  // ← 슬래시 사용
```

프론트엔드 구현 가이드 문서(`fcm-redis-flow.md:138`)에는 `/api/fcm/register-force`로 명시됨 (하이픈 사용)

**영향:**
- 프론트엔드 연동 시 404 오류 발생 가능
- 문서와 실제 구현 불일치로 혼란 초래

**해결 방법:**

**옵션 1: 코드 수정 (권장)**
```java
@PostMapping("/register-force")  // 하이픈으로 변경하여 문서와 통일
```

**옵션 2: 문서 수정**
```javascript
// fcm-redis-flow.md, fcm-frontend-flow.md 수정
await api.post('/api/fcm/register/force', {  // 슬래시로 변경
```

**권장:** RESTful API 관례상 하이픈(`kebab-case`)이 더 일반적이므로 코드를 문서에 맞춰 수정하는 것을 권장

**우선순위:** 🟡 Important (단기 수정 필요)

---

## 🟡 개선 권장사항 (Important)

### 5. 중복 코드 제거 - 다른 사용자 토큰 제거 로직

**파일:** `FcmTokenService.java:155-165, 236-243`

**문제:**
`register()`와 `registerForce()` 메서드에서 동일한 "다른 사용자 토큰 제거" 로직이 중복됨

**개선 방법:**
```java
/**
 * 다른 사용자에게 등록된 토큰 제거
 */
private void removeTokenFromOtherUser(String fcmToken, Integer currentUserIdx, String platform) {
    fcmTokenRepository.findByAnyToken(fcmToken)
        .filter(token -> !token.getUserIdx().equals(currentUserIdx))
        .ifPresent(otherUserToken -> {
            otherUserToken.setTokenByPlatform(platform, null);
            otherUserToken.setLastUsedByPlatform(platform, null);
            otherUserToken.setKeepSignedInByPlatform(platform, null);
            fcmTokenRepository.save(otherUserToken);
            logger.warn("다른 사용자({})의 {} 토큰 제거됨 - 현재 사용자: {}",
                       otherUserToken.getUserCode(), platform, currentUserIdx);
        });
}

// 사용 예시
@Transactional
public FcmRegisterResponse register(Authentication authentication, FcmRegisterRequest request) {
    UserTbl user = getUser(authentication);
    String fcmToken = request.getFcmToken();
    String platform = normalizePlatform(request.getPlatform());

    logger.info("FCM 토큰 등록 요청 - 사용자: {}, 플랫폼: {}", user.getUserCode(), platform);

    // 1. 해당 토큰이 다른 사용자에게 등록되어 있는지 확인 및 제거
    removeTokenFromOtherUser(fcmToken, user.getUserIdx(), platform);

    // 2. 현재 사용자의 FCM 토큰 정보 조회 또는 생성
    // ...
}
```

**우선순위:** 🟢 Nice to Have (중기 개선)

---

### 6. Redis Scan 성능 최적화

**파일:** `FcmSessionService.java:125-148`

**문제:**
```java
private Set<String> findKeysByPattern(String pattern) {
    Set<String> keys = redisTemplate.execute((RedisConnection connection) -> {
        ScanOptions options = ScanOptions.scanOptions().match(pattern).count(1000).build();
        // ...
        try (Cursor<byte[]> cursor = connection.scan(options)) {
            while (cursor.hasNext()) {
                // ...
            }
        }
    });
}
```

`count(1000)` 설정이 있지만 대규모 키 환경에서는 여전히 성능 이슈 가능

**개선 방법:**

**옵션 1: 키 네이밍 전략 변경**
```java
// 현재: fcm:temp:{userIdx}:{platform}
//       fcm:temp:123:WEB
//       fcm:temp:123:ANDROID

// 개선: fcm:temp:{platform}:{userIdx}
//       fcm:temp:WEB:123
//       fcm:temp:ANDROID:123

// 장점: 플랫폼별 SET 자료구조 활용 가능
```

**옵션 2: SET 자료구조 활용**
```java
// fcm:temp:users:WEB → SET {123, 456, 789, ...}
// fcm:temp:token:WEB:123 → "actual_token_value"

public void addTemporaryToken(Integer userIdx, String platform, String fcmToken) {
    String setKey = "fcm:temp:users:" + platform;
    String tokenKey = "fcm:temp:token:" + platform + ":" + userIdx;

    redisTemplate.opsForSet().add(setKey, userIdx.toString());
    redisTemplate.opsForValue().set(tokenKey, fcmToken, TEMP_TOKEN_TTL);
}

public Set<String> getTemporaryPlatforms(Integer userIdx) {
    Set<String> platforms = new HashSet<>();
    for (String platform : Arrays.asList("ANDROID", "IOS", "WEB")) {
        String setKey = "fcm:temp:users:" + platform;
        if (Boolean.TRUE.equals(redisTemplate.opsForSet().isMember(setKey, userIdx.toString()))) {
            platforms.add(platform);
        }
    }
    return platforms;
}
```

**우선순위:** 🟢 Nice to Have (장기 개선, 현재 트래픽으로는 문제없음)

---

### 7. unregister 메서드 로직 단순화

**파일:** `FcmTokenService.java:265-314`

**문제:**
로그아웃 처리가 Redis → DB 순서로 진행되면서 early return으로 인해 추적이 어려움

**개선 방법:**
```java
@Transactional
public void unregister(Authentication authentication, FcmUnregisterRequest request) {
    UserTbl user = getUser(authentication);
    String fcmToken = request.getFcmToken();
    String platform = normalizePlatform(request.getPlatform());

    logger.info("FCM 토큰 삭제 요청 - 사용자: {}, 플랫폼: {}", user.getUserCode(), platform);

    // 1단계: Redis 임시 토큰 처리
    boolean wasTemporary = unregisterTemporaryToken(user.getUserIdx(), platform, fcmToken);

    // 2단계: DB 토큰 처리 (임시 토큰이 아닌 경우만)
    if (!wasTemporary) {
        unregisterDatabaseToken(user, platform, fcmToken, request.getForceDelete());
    }
}

/**
 * Redis 임시 토큰 삭제
 * @return 임시 토큰이었으면 true, 아니면 false
 */
private boolean unregisterTemporaryToken(Integer userIdx, String platform, String fcmToken) {
    String tempToken = fcmSessionService.getTemporaryToken(userIdx, platform);
    if (tempToken != null && tempToken.equals(fcmToken)) {
        fcmSessionService.removeTemporaryToken(userIdx, platform);
        logger.info("FCM 임시 토큰 삭제 완료 - 사용자 인덱스: {}, 플랫폼: {}", userIdx, platform);
        return true;
    }
    return false;
}

/**
 * DB 토큰 삭제
 */
private void unregisterDatabaseToken(UserTbl user, String platform, String fcmToken, Boolean forceDelete) {
    Optional<FcmToken> fcmTokenOpt = fcmTokenRepository.findByUserIdx(user.getUserIdx());
    if (!fcmTokenOpt.isPresent()) {
        logger.warn("FCM 토큰 정보 없음 - 사용자: {}", user.getUserCode());
        return;
    }

    FcmToken fcmTokenEntity = fcmTokenOpt.get();
    String currentToken = fcmTokenEntity.getTokenByPlatform(platform);

    // 요청한 토큰과 DB의 토큰이 일치하는 경우만 처리
    if (currentToken == null || !currentToken.equals(fcmToken)) {
        logger.warn("FCM 토큰 불일치로 삭제 실패 - 사용자: {}, 플랫폼: {}", user.getUserCode(), platform);
        return;
    }

    Boolean keepSignedIn = fcmTokenEntity.getKeepSignedInByPlatform(platform);

    // forceDelete가 true면 무조건 삭제, 아니면 keepSignedIn 확인
    if (forceDelete != null && forceDelete) {
        clearStoredToken(fcmTokenEntity, platform);
        logger.info("FCM 토큰 강제 삭제 완료 - 사용자: {}, 플랫폼: {}", user.getUserCode(), platform);
    } else if (keepSignedIn != null && keepSignedIn) {
        logger.info("FCM 토큰 유지 (로그인 상태 유지 설정) - 사용자: {}, 플랫폼: {}",
                   user.getUserCode(), platform);
    } else {
        clearStoredToken(fcmTokenEntity, platform);
        logger.info("FCM 토큰 삭제 완료 - 사용자: {}, 플랫폼: {}", user.getUserCode(), platform);
    }
}

private void clearStoredToken(FcmToken fcmToken, String platform) {
    fcmToken.setTokenByPlatform(platform, null);
    fcmToken.setLastUsedByPlatform(platform, null);
    fcmToken.setKeepSignedInByPlatform(platform, null);
    fcmTokenRepository.save(fcmToken);
}
```

**우선순위:** 🟢 Nice to Have (코드 가독성 개선)

---

### 8. 브로드캐스트 예외 타입 체크 최적화

**파일:** `FcmTokenService.java:595-610`

**현재 코드:**
```java
if (!sr.isSuccessful() && sr.getException() != null) {
    // Exception 타입 체크 후 캐스팅
    if (sr.getException() instanceof FirebaseMessagingException) {
        FirebaseMessagingException fme = (FirebaseMessagingException) sr.getException();
        String errorCode = fme.getErrorCode() != null ? fme.getErrorCode().name() : "UNKNOWN";

        if (isInvalidTokenError(errorCode)) {
            String invalidToken = batch.get(j);
            invalidTokens.add(invalidToken);
            removeInvalidToken(invalidToken);
        }
    } else {
        logger.warn("FCM 브로드캐스트 - 예상치 못한 예외 타입: {}",
                   sr.getException().getClass().getName());
    }
}
```

**개선 방법:**
Firebase SDK 명세상 `SendResponse.getException()`은 항상 `FirebaseMessagingException` 또는 null을 반환하므로, 타입 체크 없이 직접 캐스팅해도 안전함

```java
if (!sr.isSuccessful() && sr.getException() != null) {
    FirebaseMessagingException fme = (FirebaseMessagingException) sr.getException();
    String errorCode = fme.getErrorCode() != null ? fme.getErrorCode().name() : "UNKNOWN";

    if (isInvalidTokenError(errorCode)) {
        String invalidToken = batch.get(j);
        invalidTokens.add(invalidToken);
        removeInvalidToken(invalidToken);
    }
}
```

**우선순위:** 🟢 Nice to Have (코드 간결성)

---

### 9. N+1 쿼리 문제 해결

**파일:** `FcmTokenService.java:710-719`

**문제:**
```java
private List<FcmToken> filterTokensByUserType(List<FcmToken> tokens, Integer userStudent) {
    List<FcmToken> filtered = new ArrayList<>();
    for (FcmToken token : tokens) {
        Optional<UserTbl> userOpt = userTblRepository.findById(token.getUserIdx());  // ← N번 쿼리
        if (userOpt.isPresent() && userOpt.get().getUserStudent().equals(userStudent)) {
            filtered.add(token);
        }
    }
    return filtered;
}
```

사용자 수만큼 DB 조회가 발생하는 N+1 쿼리 문제

**영향:**
- 브로드캐스트 전송 시 사용자 필터링 과정에서 성능 저하
- 1000명 필터링 시 1000번의 쿼리 발생

**해결 방법:**

**옵션 1: IN 쿼리로 일괄 조회**
```java
private List<FcmToken> filterTokensByUserType(List<FcmToken> tokens, Integer userStudent) {
    if (tokens.isEmpty()) {
        return new ArrayList<>();
    }

    // 1. 모든 userIdx 수집
    List<Integer> userIdxList = tokens.stream()
        .map(FcmToken::getUserIdx)
        .collect(Collectors.toList());

    // 2. 일괄 조회 (1번의 쿼리로 모든 사용자 조회)
    Map<Integer, UserTbl> userMap = userTblRepository.findAllById(userIdxList).stream()
        .collect(Collectors.toMap(UserTbl::getUserIdx, Function.identity()));

    // 3. 필터링
    return tokens.stream()
        .filter(token -> {
            UserTbl user = userMap.get(token.getUserIdx());
            return user != null && user.getUserStudent().equals(userStudent);
        })
        .collect(Collectors.toList());
}
```

**옵션 2: Repository에 커스텀 쿼리 추가 (더 효율적)**
```java
// FcmTokenRepository.java
@Query("SELECT f FROM FcmToken f JOIN UserTbl u ON f.userIdx = u.userIdx " +
       "WHERE u.userStudent = :userStudent")
List<FcmToken> findByUserStudent(@Param("userStudent") Integer userStudent);

// FcmTokenService.java
private List<FcmToken> filterTokensByUserType(List<FcmToken> tokens, Integer userStudent) {
    // 처음부터 필터링된 토큰만 조회하도록 변경
    return fcmTokenRepository.findByUserStudent(userStudent);
}
```

**우선순위:** 🟡 Important (성능 개선)

---

### 10. 설정값 외부화

**파일:** `FcmSessionService.java:28`

**문제:**
```java
private static final Duration TEMP_TOKEN_TTL = Duration.ofHours(24);
```

TTL이 하드코딩되어 있어 변경 시 재컴파일 필요

**개선 방법:**

**1. application.yml 설정 추가**
```yaml
# application.yml
fcm:
  temp-token:
    ttl: 24h  # 또는 1440m (분 단위)
```

**2. 설정 클래스 생성**
```java
// FcmProperties.java
@Configuration
@ConfigurationProperties(prefix = "fcm.temp-token")
public class FcmProperties {
    private Duration ttl = Duration.ofHours(24);  // 기본값

    public Duration getTtl() {
        return ttl;
    }

    public void setTtl(Duration ttl) {
        this.ttl = ttl;
    }
}
```

**3. FcmSessionService 수정**
```java
@Service
public class FcmSessionService {

    private final RedisTemplate<String, String> redisTemplate;
    private final FcmProperties fcmProperties;

    public FcmSessionService(RedisTemplate<String, String> redisTemplate,
                            FcmProperties fcmProperties) {
        this.redisTemplate = redisTemplate;
        this.fcmProperties = fcmProperties;
    }

    public void addTemporaryToken(Integer userIdx, String platform, String fcmToken) {
        String key = buildKey(userIdx, platform);
        redisTemplate.opsForValue().set(key, fcmToken, fcmProperties.getTtl());
    }
}
```

**우선순위:** 🟢 Nice to Have (운영 편의성)

---

## ✅ 좋은 점

### 1. 체계적인 충돌 감지 메커니즘

**위치:** `FcmTokenService.java:155-165`

```java
// 1. 해당 토큰이 다른 사용자에게 등록되어 있는지 확인
Optional<FcmToken> existingTokenOwner = fcmTokenRepository.findByAnyToken(fcmToken);
if (existingTokenOwner.isPresent() && !existingTokenOwner.get().getUserIdx().equals(user.getUserIdx())) {
    // 다른 사용자에게 등록된 토큰 제거 (로그아웃 없이 브라우저 공유 케이스)
    FcmToken otherUserToken = existingTokenOwner.get();
    otherUserToken.setTokenByPlatform(platform, null);
    // ...
}
```

**장점:**
- DB 등록 전에 다른 사용자의 토큰 소유 여부를 확인
- 브라우저 공유 케이스를 자동으로 처리
- 사용자에게 명확한 선택권 제공 (conflict 응답)

---

### 2. Redis 임시 저장소 활용

**위치:** `FcmSessionService.java`, `FcmTokenService.java:348-390`

**장점:**
- 로그인 세션 동안만 유효한 임시 토큰 관리
- DB 부하 감소
- 멀티 기기 동시 알림 지원 (DB 1개 + Redis N개)
- 24시간 TTL로 자동 정리

**설계:**
```
집 PC (DB): keepSignedIn=true → 로그아웃 후에도 알림 받음
학교 PC (Redis): temporaryOnly=true → 로그인 중에만 알림 받음
→ 둘 다 동시 알림 가능! 🎉
```

---

### 3. 플랫폼 정규화

**위치:** `FcmTokenService.java:101-114`

```java
private String normalizePlatform(String platform) {
    if (platform == null) {
        throw new InvalidPlatformException("null");
    }
    String normalized = platform.trim().toUpperCase(Locale.ROOT);
    switch (normalized) {
        case "ANDROID":
        case "IOS":
        case "WEB":
            return normalized;
        default:
            throw new InvalidPlatformException(platform);
    }
}
```

**장점:**
- 대소문자 무관하게 입력 받아 통일된 형식으로 저장
- 잘못된 플랫폼 값에 대한 명확한 예외 처리
- Locale 안전성 확보 (`Locale.ROOT` 사용)

---

### 4. 무효 토큰 자동 정리

**위치:** `FcmTokenService.java:431-442, 759-807`

**4-1. 전송 실패 시 즉시 삭제**
```java
if (isInvalidTokenError(errorCode)) {
    if (isTemporary) {
        fcmSessionService.removeTemporaryToken(fcmToken.getUserIdx(), platform);
        logger.warn("FCM 임시 토큰 무효화 - 사용자: {}, 플랫폼: {}, 오류: {}",
                fcmToken.getUserCode(), platform, errorCode);
    } else {
        clearStoredToken(fcmToken, platform);
        logger.warn("FCM 토큰 무효화 - 사용자: {}, 플랫폼: {}, 오류: {}",
                fcmToken.getUserCode(), platform, errorCode);
    }
}
```

**4-2. 스케줄러 기반 정기 정리**
```java
@Scheduled(cron = "0 0 3 * * *")  // 매일 새벽 3시
@Transactional
public void cleanupInactiveTokens() {
    LocalDateTime cutoffDate = LocalDateTime.now().minusDays(90);
    List<FcmToken> inactiveTokens = fcmTokenRepository.findInactiveTokens(cutoffDate);
    // 90일 이상 미사용 토큰 삭제
}
```

**장점:**
- FCM 오류 코드에 따른 즉각적인 무효 토큰 처리
- 정기적인 배치 작업으로 DB 정리
- Redis 임시 토큰도 동일하게 처리

---

### 5. 트랜잭션 관리

**위치:** `FcmTokenService.java` 전반

```java
@Transactional
public FcmRegisterResponse register(Authentication authentication, FcmRegisterRequest request) {
    // 복잡한 비즈니스 로직
}

@Transactional
public void unregister(Authentication authentication, FcmUnregisterRequest request) {
    // 토큰 삭제 로직
}
```

**장점:**
- 중요한 작업에 `@Transactional` 적용으로 데이터 일관성 보장
- 예외 발생 시 자동 롤백
- 멀티 스텝 작업의 원자성 보장

---

### 6. 체계적인 로깅 전략

**장점:**
- 모든 주요 작업에 INFO 레벨 로그 출력
- 예외 상황에 WARN/ERROR 로그 출력
- 사용자 추적 가능 (userCode 포함)
- 디버깅 용이성

**예시:**
```java
logger.info("FCM 토큰 등록 요청 - 사용자: {}, 플랫폼: {}", user.getUserCode(), platform);
logger.warn("FCM 토큰 충돌 감지 - 사용자: {}, 플랫폼: {}, 마지막 사용: {}", ...);
logger.error("FCM 알림 전송 실패 - 사용자: {}, 플랫폼: {}, 임시: {}, 오류: {}", ...);
```

---

### 7. 유연한 플랫폼 선택

**위치:** `FcmTokenService.java:116-125`

```java
private List<String> resolvePlatforms(List<String> platforms) {
    if (platforms == null || platforms.isEmpty()) {
        return Arrays.asList("ANDROID", "IOS", "WEB");  // 기본값: 전체 플랫폼
    }

    return platforms.stream()
            .map(this::normalizePlatform)
            .distinct()
            .collect(Collectors.toList());
}
```

**장점:**
- 일괄 전송/브로드캐스트 시 플랫폼 선택 가능
- 기본값으로 전체 플랫폼 지원
- 중복 제거 처리

---

### 8. 명확한 응답 DTO 설계

**위치:** `FcmRegisterResponse.java`

```java
public static FcmRegisterResponse registered(Boolean keepSignedIn) { /* ... */ }
public static FcmRegisterResponse renewed(Boolean keepSignedIn) { /* ... */ }
public static FcmRegisterResponse conflict(String platform, LocalDateTime lastUsed) { /* ... */ }
public static FcmRegisterResponse replaced(Boolean keepSignedIn) { /* ... */ }
public static FcmRegisterResponse temporary() { /* ... */ }
```

**장점:**
- Static factory 패턴으로 명확한 의도 표현
- 프론트엔드에서 상태별 분기 처리 용이
- 일관된 메시지 제공

---

### 9. 보안 고려

**위치:** `FcmTokenController.java`

```java
@PostMapping("/register")
@PreAuthorize("isAuthenticated()")  // 인증 필요
public ResponseEntity<ApiResponse<FcmRegisterResponse>> register(...) { }

@PostMapping("/send")
@PreAuthorize("hasRole('ADMIN')")  // 관리자만
public ResponseEntity<ApiResponse<FcmSendResponse>> send(...) { }
```

**장점:**
- 토큰 등록/삭제는 인증된 사용자만 가능
- 알림 전송은 관리자만 가능
- Spring Security 기반 권한 검증

---

### 10. Validation 적용

**위치:** DTO 클래스들

```java
@NotBlank(message = "FCM 토큰은 필수입니다")
private String fcmToken;

@Pattern(regexp = "(?i)^(ANDROID|IOS|WEB)$",
         message = "플랫폼은 ANDROID, IOS, WEB 중 하나여야 합니다")
private String platform;
```

**장점:**
- 요청 데이터 유효성 검증
- 명확한 에러 메시지
- 대소문자 구분 없는 정규식 패턴

---

## 🧪 테스트 시나리오

### ✅ 정상 동작 예상

| # | 시나리오 | 예상 결과 | 검증 포인트 |
|---|---------|----------|-----------|
| 1 | 신규 사용자 최초 등록 | `REGISTERED` 응답 | DB에 토큰 저장 확인 |
| 2 | 동일 기기 재로그인 | `RENEWED` 응답 | `lastUsed` 갱신 확인 |
| 3 | 다른 기기 로그인 | `CONFLICT` 응답 | `lastUsed` 포함 확인 |
| 4 | 충돌 후 강제 등록 | `REPLACED` 응답 | 기존 토큰 덮어쓰기 확인 |
| 5 | 임시 토큰 등록 | `TEMPORARY` 응답 | Redis에만 저장 확인 |
| 6 | keepSignedIn=true 로그아웃 | DB 토큰 유지 | 재로그인 없이 알림 수신 |
| 7 | keepSignedIn=false 로그아웃 | DB 토큰 삭제 | 로그인 전까지 알림 미수신 |
| 8 | 개별 알림 전송 (DB+Redis) | 둘 다 수신 | 임시 토큰에도 전송 확인 |
| 9 | 무효 토큰 전송 시 | 자동 삭제 | DB/Redis에서 삭제 확인 |
| 10 | 90일 미사용 토큰 | 스케줄러 정리 | 배치 작업 로그 확인 |

---

### ⚠️ 버그 존재 (수정 필요)

| # | 시나리오 | 현재 동작 | 예상 동작 | 관련 이슈 |
|---|---------|----------|----------|----------|
| 4 | 임시 토큰 등록 후 일괄 알림 | ❌ 임시 토큰 미수신 | ✅ DB+Redis 모두 수신 | 이슈 #2 |
| 5 | 임시 토큰 등록 후 브로드캐스트 | ❌ 임시 토큰 미수신 | ✅ DB+Redis 모두 수신 | 이슈 #3 |
| 11 | 신규 사용자 강제 등록 | ❌ 예외 발생 가능 | ✅ 정상 등록 | 이슈 #1 |

---

### 🔍 추가 테스트 권장

| # | 시나리오 | 목적 | 검증 방법 |
|---|---------|------|----------|
| 12 | 동일 토큰 중복 등록 | 멱등성 확인 | 여러 번 등록 시 상태 확인 |
| 13 | 플랫폼 대소문자 혼용 | 정규화 검증 | `android`, `Android`, `ANDROID` 모두 성공 |
| 14 | 잘못된 플랫폼 값 | 예외 처리 | `InvalidPlatformException` 발생 확인 |
| 15 | 다른 사용자 토큰 재사용 | 자동 제거 | 이전 사용자에게서 토큰 삭제 확인 |
| 16 | Redis 연결 끊김 | Fallback 동작 | DB 토큰만으로 동작 확인 |
| 17 | 대량 브로드캐스트 (1000명) | 성능 측정 | 응답 시간 및 배치 처리 확인 |
| 18 | 동시 등록 요청 (Race Condition) | 동시성 제어 | 트랜잭션 격리 수준 확인 |

---

## 📊 우선순위 수정 항목

| 순위 | 파일 | 라인 | 내용 | 중요도 | 예상 공수 |
|------|------|------|------|--------|----------|
| 1 | `FcmTokenService.java` | 247 | `registerForce`에서 엔티티 저장 누락 | 🔴 Critical | 5분 |
| 2 | `FcmTokenService.java` | 498-519 | 일괄 전송에서 임시 토큰 미처리 | 🔴 Critical | 30분 |
| 3 | `FcmTokenService.java` | 552-559 | 브로드캐스트에서 임시 토큰 미처리 | 🔴 Critical | 20분 |
| 4 | `FcmTokenController.java` | 40 | API 경로 불일치 (문서와 상이) | 🟡 Important | 2분 |
| 5 | `FcmTokenService.java` | 155-165 | 중복 코드 제거 | 🟢 Nice to Have | 15분 |
| 6 | `FcmTokenService.java` | 710-719 | N+1 쿼리 최적화 | 🟡 Important | 30분 |
| 7 | `FcmSessionService.java` | 125-148 | Redis Scan 성능 최적화 | 🟢 Nice to Have | 2시간 |
| 8 | `FcmTokenService.java` | 265-314 | unregister 로직 단순화 | 🟢 Nice to Have | 45분 |
| 9 | `FcmSessionService.java` | 28 | 설정값 외부화 | 🟢 Nice to Have | 20분 |

**총 예상 공수:** ~5시간 (Critical 이슈만 1시간 이내)

---

## 🎯 최종 권장사항

### 즉시 수정 (Critical)

**대상:** 이슈 #1, #2, #3

**이유:**
- 기능 결함으로 일부 사용자가 알림을 받지 못하는 문제
- 신규 사용자 강제 등록 시 예외 발생 가능
- 임시 토큰의 핵심 기능(멀티 기기 알림)이 일괄/브로드캐스트에서 동작하지 않음

**예상 효과:**
- 모든 로그인 사용자에게 일괄/브로드캐스트 알림 정상 전달
- 신규 사용자 등록 오류 제거
- 시스템 안정성 향상

---

### 단기 개선 (1주일 이내)

**대상:** 이슈 #4, #5, #6

**이유:**
- API 경로 불일치는 프론트엔드 연동 시 혼란 야기
- 중복 코드는 유지보수성 저하
- N+1 쿼리는 브로드캐스트 성능에 직접적 영향

**예상 효과:**
- 프론트엔드 개발자와의 커뮤니케이션 비용 감소
- 코드 가독성 및 유지보수성 향상
- 대량 브로드캐스트 성능 개선 (1000명 기준 1초 → 0.1초)

---

### 중장기 개선 (1개월 이내)

**대상:** 이슈 #7, #8, #9

**이유:**
- Redis Scan 성능은 현재 트래픽에서는 문제없지만 확장성 대비
- 로직 단순화는 신규 개발자 온보딩에 도움
- 설정 외부화는 운영 편의성 향상

**예상 효과:**
- 대규모 트래픽 환경 대비
- 코드 복잡도 감소
- 설정 변경 시 재배포 불필요

---

### 테스트 계획

**1단계: Critical 이슈 수정 후 회귀 테스트**
```
✓ 신규 사용자 강제 등록 → 정상 동작 확인
✓ 임시 토큰 등록 후 일괄 알림 → DB+Redis 모두 수신
✓ 임시 토큰 등록 후 브로드캐스트 → DB+Redis 모두 수신
```

**2단계: 통합 테스트**
```
✓ 집 PC (DB) + 학교 PC (Redis) 동시 로그인 → 둘 다 알림
✓ 학교 PC 로그아웃 → Redis 삭제, 집 PC만 알림
✓ 1000명 브로드캐스트 → 성능 측정
```

**3단계: 부하 테스트**
```
✓ 동시 등록 100명 → Race Condition 확인
✓ 브로드캐스트 10,000명 → 타임아웃 없이 완료
✓ Redis 장애 시 Fallback → DB 토큰만으로 동작
```

---

## 📝 리뷰 요약

### 전반적인 평가
**점수: 85/100** ⭐⭐⭐⭐☆

**평가 근거:**
- ✅ 설계: 90/100 (체계적인 아키텍처, 명확한 책임 분리)
- ⚠️ 구현: 75/100 (일부 로직 누락, N+1 쿼리)
- ✅ 보안: 95/100 (인증/인가, 입력 검증)
- ✅ 유지보수성: 85/100 (로깅, 트랜잭션 관리)
- ⚠️ 성능: 80/100 (N+1 쿼리, Redis Scan)

### 주요 강점
1. Redis 임시 저장소를 활용한 멀티 기기 알림 지원
2. 체계적인 충돌 감지 및 해결 메커니즘
3. 자동 무효 토큰 정리 (즉시 + 스케줄러)
4. 명확한 트랜잭션 관리 및 로깅

### 주요 개선점
1. 임시 토큰 로직이 일괄/브로드캐스트에 누락 (Critical)
2. 신규 사용자 강제 등록 시 엔티티 저장 누락 (Critical)
3. N+1 쿼리 최적화 필요 (Important)
4. API 경로 문서와 불일치 (Important)

### 최종 의견
**전반적으로 잘 설계된 시스템**이지만, Redis 임시 토큰 관련 로직이 일부 전송 메서드에서 누락되어 있습니다.

위에서 명시한 **3가지 Critical 이슈(#1, #2, #3)를 수정**하면 프로덕션 환경에서 안정적으로 운영 가능합니다.

특히 **임시 토큰 기능은 멀티 기기 알림의 핵심**이므로, 일괄 전송과 브로드캐스트에서도 정상 동작하도록 즉시 수정이 필요합니다.

---

**검토 완료일:** 2025-10-04
**다음 리뷰:** Critical 이슈 수정 후 재검토 권장
