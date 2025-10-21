package BlueCrab.com.example.service;

import BlueCrab.com.example.dto.*;
import BlueCrab.com.example.entity.FcmToken;
import BlueCrab.com.example.entity.UserTbl;
import BlueCrab.com.example.exception.InvalidPlatformException;
import BlueCrab.com.example.exception.ResourceNotFoundException;
import BlueCrab.com.example.repository.FcmTokenRepository;
import BlueCrab.com.example.repository.UserTblRepository;
import com.google.firebase.messaging.BatchResponse;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.MulticastMessage;
import com.google.firebase.messaging.Notification;
import com.google.firebase.messaging.SendResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.scheduling.annotation.Scheduled;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * FCM 토큰 관리 및 알림 전송 서비스
 */
@Service
public class FcmTokenService {

    private static final Logger logger = LoggerFactory.getLogger(FcmTokenService.class);

    private final FcmTokenRepository fcmTokenRepository;
    private final UserTblRepository userTblRepository;
    private final FirebaseMessaging firebaseMessaging;
    private final FcmSessionService fcmSessionService;
    private final FirebasePushService firebasePushService;

    private static final class DeliveryAttemptResult {
        private final boolean success;
        private final String failureReason;

        private DeliveryAttemptResult(boolean success, String failureReason) {
            this.success = success;
            this.failureReason = failureReason;
        }

        public static DeliveryAttemptResult success() {
            return new DeliveryAttemptResult(true, null);
        }

        public static DeliveryAttemptResult failure(String failureReason) {
            return new DeliveryAttemptResult(false, failureReason);
        }

        public boolean isSuccess() {
            return success;
        }

        public String getFailureReason() {
            return failureReason;
        }
    }

    private static final class PlatformDeliverySummary {
        private final boolean success;
        private final String failureReason;

        private PlatformDeliverySummary(boolean success, String failureReason) {
            this.success = success;
            this.failureReason = failureReason;
        }

        public boolean isSuccess() {
            return success;
        }

        public String getFailureReason() {
            return failureReason;
        }
    }

    public FcmTokenService(FcmTokenRepository fcmTokenRepository,
                           UserTblRepository userTblRepository,
                           FirebaseMessaging firebaseMessaging,
                           FcmSessionService fcmSessionService,
                           FirebasePushService firebasePushService) {
        this.fcmTokenRepository = fcmTokenRepository;
        this.userTblRepository = userTblRepository;
        this.firebaseMessaging = firebaseMessaging;
        this.fcmSessionService = fcmSessionService;
        this.firebasePushService = firebasePushService;
    }

    /**
     * JWT 토큰에서 사용자 정보 추출
     */
    private UserTbl getUser(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new RuntimeException("인증 정보가 필요합니다");
        }

        Object principal = authentication.getPrincipal();
        String username;

        if (principal instanceof UserDetails) {
            username = ((UserDetails) principal).getUsername();
        } else if (principal instanceof String) {
            username = (String) principal;
        } else {
            throw new RuntimeException("지원하지 않는 인증 주체 타입입니다");
        }

        return userTblRepository.findByUserEmail(username)
                .orElseThrow(() -> new ResourceNotFoundException("사용자를 찾을 수 없습니다"));
    }

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

    private List<String> resolvePlatforms(List<String> platforms) {
        if (platforms == null || platforms.isEmpty()) {
            return Arrays.asList("ANDROID", "IOS", "WEB");
        }

        return platforms.stream()
                .map(this::normalizePlatform)
                .distinct()
                .collect(Collectors.toList());
    }

    private List<String> normalizeUserCodes(List<String> userCodes) {
        if (userCodes == null) {
            return Collections.emptyList();
        }

        LinkedHashSet<String> normalized = new LinkedHashSet<>();
        for (String code : userCodes) {
            if (code == null) {
                continue;
            }
            String trimmed = code.trim();
            if (!trimmed.isEmpty()) {
                normalized.add(trimmed);
            }
        }

        return new ArrayList<>(normalized);
    }

    private boolean isValidToken(String token) {
        return token != null && !token.trim().isEmpty();
    }

    private String toPlatformKey(String platform) {
        return platform.toLowerCase(Locale.ROOT);
    }

    private Map<String, List<String>> copyPlatformMap(Map<String, List<String>> source) {
        Map<String, List<String>> copy = new LinkedHashMap<>();
        if (source == null) {
            return copy;
        }
        source.forEach((key, value) -> {
            if (value != null) {
                copy.put(key, new ArrayList<>(value));
            } else {
                copy.put(key, new ArrayList<>());
            }
        });
        return copy;
    }

    public List<FcmTokenLookupResponse> lookupTokens(FcmTokenLookupRequest request) {
        Objects.requireNonNull(request, "요청 객체는 null일 수 없습니다");
        boolean includeTemporary = request.getIncludeTemporary() == null || request.getIncludeTemporary();
        return lookupTokensInternal(request.getUserCodes(), request.getPlatforms(), includeTemporary);
    }

    public FcmTokenCollectionResult collectTokensByUserCodes(List<String> userCodes) {
        return collectTokensByUserCodes(userCodes, null, true);
    }

    public FcmTokenCollectionResult collectTokensByUserCodes(List<String> userCodes,
                                                             List<String> platforms,
                                                             boolean includeTemporary) {
        List<FcmTokenLookupResponse> lookupResults = lookupTokensInternal(userCodes, platforms, includeTemporary);

        Map<String, List<String>> tokensByUser = new LinkedHashMap<>();
        LinkedHashSet<String> flattenedTokens = new LinkedHashSet<>();

        for (FcmTokenLookupResponse lookup : lookupResults) {
            Map<String, List<String>> tokensByPlatform = lookup.getTokensByPlatform();
            if (tokensByPlatform == null || tokensByPlatform.isEmpty()) {
                continue;
            }

            List<String> aggregated = tokensByPlatform.values().stream()
                    .filter(Objects::nonNull)
                    .flatMap(Collection::stream)
                    .filter(this::isValidToken)
                    .distinct()
                    .collect(Collectors.toList());

            if (aggregated.isEmpty()) {
                continue;
            }

            tokensByUser.put(lookup.getUserCode(), aggregated);
            flattenedTokens.addAll(aggregated);
        }

        return new FcmTokenCollectionResult(lookupResults, tokensByUser, new ArrayList<>(flattenedTokens));
    }

    public FcmDataOnlySendResponse sendDataOnlyByUser(FcmDataOnlySendRequest request) {
        Objects.requireNonNull(request, "요청 객체는 null일 수 없습니다");
        boolean includeTemporary = request.getIncludeTemporary() == null || request.getIncludeTemporary();

        List<FcmTokenLookupResponse> lookupResults = lookupTokensInternal(
                request.getUserCodes(),
                request.getPlatforms(),
                includeTemporary
        );

        Map<String, FcmDataOnlyUserResultBuilder> builderMap = new LinkedHashMap<>();
        Map<String, TokenReference> tokenRouting = new LinkedHashMap<>();

        for (FcmTokenLookupResponse lookup : lookupResults) {
            Map<String, List<String>> tokensByPlatform = copyPlatformMap(lookup.getTokensByPlatform());
            List<String> missingPlatforms = lookup.getMissingPlatforms() != null
                    ? new ArrayList<>(lookup.getMissingPlatforms())
                    : new ArrayList<>();

            FcmDataOnlyUserResultBuilder builder = new FcmDataOnlyUserResultBuilder(
                    lookup.getUserCode(),
                    tokensByPlatform,
                    missingPlatforms
            );
            builderMap.put(lookup.getUserCode(), builder);

            tokensByPlatform.forEach((platformKey, tokenList) -> {
                for (String token : tokenList) {
                    if (!isValidToken(token)) {
                        continue;
                    }
                    tokenRouting.putIfAbsent(token, new TokenReference(lookup.getUserCode(), platformKey));
                }
            });
        }

        int successCount = 0;
        int failureCount = 0;

        if (!tokenRouting.isEmpty()) {
            logger.info("Data-only 알림 전송 요청 - 사용자 수: {}, 토큰 수: {}",
                    builderMap.size(), tokenRouting.size());

            BatchSendResponse batchResponse = firebasePushService.sendDataOnlyNotificationBatch(
                    new ArrayList<>(tokenRouting.keySet()),
                    request.getTitle(),
                    request.getBody(),
                    request.getData()
            );

            successCount = batchResponse.getSuccessCount();
            failureCount = batchResponse.getFailureCount();

            if (batchResponse.getResponses() != null) {
                for (TokenSendResult result : batchResponse.getResponses()) {
                    TokenReference reference = tokenRouting.get(result.getToken());
                    if (reference == null) {
                        continue;
                    }
                    FcmDataOnlyUserResultBuilder builder = builderMap.get(reference.getUserCode());
                    if (builder == null) {
                        continue;
                    }
                    if (result.isSuccess()) {
                        builder.addSuccess(result.getToken());
                    } else {
                        builder.addFailure(result.getToken(), result.getError());
                    }
                }
            }
        } else {
            logger.warn("Data-only 전송 대상 토큰이 없습니다. 사용자 수: {}", builderMap.size());
        }

        List<FcmDataOnlyUserResult> userResults = new ArrayList<>();
        for (FcmTokenLookupResponse lookup : lookupResults) {
            FcmDataOnlyUserResultBuilder builder = builderMap.get(lookup.getUserCode());
            if (builder != null) {
                userResults.add(builder.build());
            }
        }

        return new FcmDataOnlySendResponse(
                lookupResults.size(),
                tokenRouting.size(),
                successCount,
                failureCount,
                userResults
        );
    }

    private List<FcmTokenLookupResponse> lookupTokensInternal(List<String> userCodes,
                                                              List<String> platforms,
                                                              boolean includeTemporary) {
        List<String> normalizedUserCodes = normalizeUserCodes(userCodes);
        if (normalizedUserCodes.isEmpty()) {
            throw new IllegalArgumentException("조회할 사용자 코드가 필요합니다");
        }

        List<String> resolvedPlatforms = resolvePlatforms(platforms);

        Map<String, FcmToken> tokensByUser = fcmTokenRepository.findByUserCodeIn(normalizedUserCodes)
                .stream()
                .collect(Collectors.toMap(FcmToken::getUserCode, token -> token));

        List<FcmTokenLookupResponse> responses = new ArrayList<>();

        for (String userCode : normalizedUserCodes) {
            FcmToken entity = tokensByUser.get(userCode);
            if (entity == null) {
                logger.warn("FCM 토큰 정보 없음 - 사용자: {}", userCode);
            }

            Map<String, List<String>> tokensByPlatform = new LinkedHashMap<>();
            List<String> missingPlatforms = new ArrayList<>();

            for (String platform : resolvedPlatforms) {
                LinkedHashSet<String> tokenSet = new LinkedHashSet<>();

                if (entity != null) {
                    String storedToken = entity.getTokenByPlatform(platform);
                    if (isValidToken(storedToken)) {
                        tokenSet.add(storedToken);
                    }

                    if (includeTemporary) {
                        String temporaryToken = fcmSessionService.getTemporaryToken(entity.getUserIdx(), platform);
                        if (isValidToken(temporaryToken)) {
                            tokenSet.add(temporaryToken);
                        }
                    }
                }

                if (!tokenSet.isEmpty()) {
                    tokensByPlatform.put(toPlatformKey(platform), new ArrayList<>(tokenSet));
                } else {
                    missingPlatforms.add(toPlatformKey(platform));
                }
            }

            responses.add(new FcmTokenLookupResponse(
                    userCode,
                    tokensByPlatform,
                    missingPlatforms
            ));
        }

        return responses;
    }

    private static final class TokenReference {
        private final String userCode;
        private final String platform;

        private TokenReference(String userCode, String platform) {
            this.userCode = userCode;
            this.platform = platform;
        }

        public String getUserCode() {
            return userCode;
        }

        @SuppressWarnings("unused")
        public String getPlatform() {
            return platform;
        }
    }

    private static final class FcmDataOnlyUserResultBuilder {
        private final String userCode;
        private final Map<String, List<String>> tokensByPlatform;
        private final List<String> missingPlatforms;
        private final List<String> succeededTokens = new ArrayList<>();
        private final Map<String, String> failedTokens = new LinkedHashMap<>();
        private int successCount = 0;
        private int failureCount = 0;

        private FcmDataOnlyUserResultBuilder(String userCode,
                                             Map<String, List<String>> tokensByPlatform,
                                             List<String> missingPlatforms) {
            this.userCode = userCode;
            this.tokensByPlatform = tokensByPlatform;
            this.missingPlatforms = missingPlatforms;
        }

        private void addSuccess(String token) {
            succeededTokens.add(token);
            successCount++;
        }

        private void addFailure(String token, String reason) {
            failedTokens.put(token, reason == null ? "unknown" : reason);
            failureCount++;
        }

        private FcmDataOnlyUserResult build() {
            Map<String, List<String>> tokensCopy = new LinkedHashMap<>();
            tokensByPlatform.forEach((key, value) -> tokensCopy.put(key, new ArrayList<>(value)));

            return new FcmDataOnlyUserResult(
                    userCode,
                    tokensCopy,
                    new ArrayList<>(missingPlatforms),
                    new ArrayList<>(succeededTokens),
                    new LinkedHashMap<>(failedTokens),
                    successCount,
                    failureCount
            );
        }
    }

    private boolean isInvalidTokenError(String errorCode) {
        if (errorCode == null) {
            return false;
        }

        switch (errorCode) {
            case "NOT_FOUND":
            case "INVALID_ARGUMENT":
            case "REGISTRATION_TOKEN_NOT_REGISTERED":
            case "UNREGISTERED":
                return true;
            default:
                return false;
        }
    }

    /**
     * FCM 토큰 등록 (충돌 감지)
     */
    @Transactional
    public FcmRegisterResponse register(Authentication authentication, FcmRegisterRequest request) {
        UserTbl user = getUser(authentication);
        String fcmToken = request.getFcmToken();
        String platform = normalizePlatform(request.getPlatform());

        logger.info("FCM 토큰 등록 요청 - 사용자: {}, 플랫폼: {}", user.getUserCode(), platform);

        // 1. 해당 토큰이 다른 사용자에게 등록되어 있는지 확인
        Optional<FcmToken> existingTokenOwner = fcmTokenRepository.findByAnyToken(fcmToken);
        if (existingTokenOwner.isPresent() && !existingTokenOwner.get().getUserIdx().equals(user.getUserIdx())) {
            // 다른 사용자에게 등록된 토큰 제거 (로그아웃 없이 브라우저 공유 케이스)
            FcmToken otherUserToken = existingTokenOwner.get();
            otherUserToken.setTokenByPlatform(platform, null);
            otherUserToken.setLastUsedByPlatform(platform, null);
            otherUserToken.setKeepSignedInByPlatform(platform, null);
            fcmTokenRepository.save(otherUserToken);
            logger.warn("다른 사용자({})의 {} 토큰 제거됨 - 현재 사용자: {}",
                       otherUserToken.getUserCode(), platform, user.getUserCode());
        }

        // 2. 현재 사용자의 FCM 토큰 정보 조회 또는 생성
        FcmToken fcmTokenEntity = fcmTokenRepository.findByUserIdx(user.getUserIdx())
                .orElseGet(() -> {
                    FcmToken newToken = new FcmToken(user.getUserIdx(), user.getUserCode());
                    return fcmTokenRepository.save(newToken);
                });

        // 3. 해당 플랫폼의 기존 토큰 확인
        String currentToken = fcmTokenEntity.getTokenByPlatform(platform);

        if (currentToken == null) {
            // 최초 등록 - 사용자가 선택한 값 사용
            Boolean keepSignedIn = request.getKeepSignedIn() != null ? request.getKeepSignedIn() : false;
            fcmTokenEntity.setTokenByPlatform(platform, fcmToken);
            fcmTokenEntity.setLastUsedByPlatform(platform, LocalDateTime.now());
            fcmTokenEntity.setKeepSignedInByPlatform(platform, keepSignedIn);
            fcmTokenRepository.save(fcmTokenEntity);
            logger.info("FCM 토큰 최초 등록 - 사용자: {}, 플랫폼: {}, keepSignedIn: {}",
                       user.getUserCode(), platform, keepSignedIn);
            return FcmRegisterResponse.registered(keepSignedIn);

        } else if (currentToken.equals(fcmToken)) {
            // 동일 토큰 재등록 (같은 기기)
            Boolean keepSignedIn = fcmTokenEntity.getKeepSignedInByPlatform(platform);

            // 사용자가 명시적으로 변경 요청한 경우에만 업데이트
            if (request.getKeepSignedIn() != null) {
                keepSignedIn = request.getKeepSignedIn();
                fcmTokenEntity.setKeepSignedInByPlatform(platform, keepSignedIn);
            }

            fcmTokenEntity.setLastUsedByPlatform(platform, LocalDateTime.now());
            fcmTokenRepository.save(fcmTokenEntity);

            logger.info("FCM 토큰 갱신 (동일 기기) - 사용자: {}, 플랫폼: {}, keepSignedIn: {}",
                       user.getUserCode(), platform, keepSignedIn);
            return FcmRegisterResponse.renewed(keepSignedIn);

        } else {
            // 다른 토큰 (충돌)

            // temporaryOnly=true: DB 덮어쓰기 거부, Redis에만 임시 저장
            if (request.getTemporaryOnly() != null && request.getTemporaryOnly()) {
                fcmSessionService.addTemporaryToken(user.getUserIdx(), platform, fcmToken);
                logger.info("FCM 토큰 임시 등록 - 사용자: {}, 플랫폼: {} (로그인 중에만 알림)",
                           user.getUserCode(), platform);
                return FcmRegisterResponse.temporary();
            }

            // temporaryOnly=false 또는 null: 충돌 알림
            LocalDateTime lastUsed = fcmTokenEntity.getLastUsedByPlatform(platform);
            logger.info("FCM 토큰 충돌 감지 - 사용자: {}, 플랫폼: {}, 마지막 사용: {}",
                       user.getUserCode(), platform, lastUsed);
            return FcmRegisterResponse.conflict(platform, lastUsed);
        }
    }

    /**
     * FCM 토큰 강제 변경 (충돌 시 사용자 선택에 의한 기기 변경)
     */
    @Transactional
    public FcmRegisterResponse registerForce(Authentication authentication, FcmRegisterRequest request) {
        UserTbl user = getUser(authentication);
        String fcmToken = request.getFcmToken();
        String platform = normalizePlatform(request.getPlatform());

        logger.info("FCM 토큰 강제 변경 - 사용자: {}, 플랫폼: {}", user.getUserCode(), platform);

        // 1. 해당 토큰이 다른 사용자에게 등록되어 있으면 제거
        Optional<FcmToken> existingTokenOwner = fcmTokenRepository.findByAnyToken(fcmToken);
        if (existingTokenOwner.isPresent() && !existingTokenOwner.get().getUserIdx().equals(user.getUserIdx())) {
            FcmToken otherUserToken = existingTokenOwner.get();
            otherUserToken.setTokenByPlatform(platform, null);
            otherUserToken.setLastUsedByPlatform(platform, null);
            otherUserToken.setKeepSignedInByPlatform(platform, null);
            fcmTokenRepository.save(otherUserToken);
        }

        // 2. Redis 임시 토큰이 있으면 제거 (DB로 영구 등록되므로)
        if (fcmSessionService.hasTemporaryToken(user.getUserIdx(), platform)) {
            fcmSessionService.removeTemporaryToken(user.getUserIdx(), platform);
            logger.info("FCM 임시 토큰 제거 (DB 영구 등록으로 전환) - 사용자: {}, 플랫폼: {}",
                       user.getUserCode(), platform);
        }

        // 3. 현재 사용자의 FCM 토큰 정보 조회 또는 생성
    FcmToken fcmTokenEntity = fcmTokenRepository.findByUserIdx(user.getUserIdx())
        .orElseGet(() -> fcmTokenRepository.save(new FcmToken(user.getUserIdx(), user.getUserCode())));

        // 4. 강제로 토큰 변경
        Boolean keepSignedIn = request.getKeepSignedIn() != null ? request.getKeepSignedIn() : false;
        fcmTokenEntity.setTokenByPlatform(platform, fcmToken);
        fcmTokenEntity.setLastUsedByPlatform(platform, LocalDateTime.now());
        fcmTokenEntity.setKeepSignedInByPlatform(platform, keepSignedIn);
        fcmTokenRepository.save(fcmTokenEntity);

        logger.info("FCM 토큰 강제 변경 완료 - 사용자: {}, 플랫폼: {}, keepSignedIn: {}",
                   user.getUserCode(), platform, keepSignedIn);
        return FcmRegisterResponse.replaced(keepSignedIn);
    }

    /**
     * FCM 토큰 삭제 (로그아웃용)
     */
    @Transactional
    public void unregister(Authentication authentication, FcmUnregisterRequest request) {
        UserTbl user = getUser(authentication);
        String fcmToken = request.getFcmToken();
        String platform = normalizePlatform(request.getPlatform());

        logger.info("FCM 토큰 삭제 요청 - 사용자: {}, 플랫폼: {}", user.getUserCode(), platform);

        // 1. Redis 임시 토큰 삭제 (무조건 시도)
        String tempToken = fcmSessionService.getTemporaryToken(user.getUserIdx(), platform);
        if (tempToken != null && tempToken.equals(fcmToken)) {
            fcmSessionService.removeTemporaryToken(user.getUserIdx(), platform);
            logger.info("FCM 임시 토큰 삭제 완료 - 사용자: {}, 플랫폼: {}", user.getUserCode(), platform);
            // early return 제거: DB도 확인해야 함
        }

        // 2. DB 토큰 삭제 (keepSignedIn 확인)
        Optional<FcmToken> fcmTokenOpt = fcmTokenRepository.findByUserIdx(user.getUserIdx());
        if (fcmTokenOpt.isPresent()) {
            FcmToken fcmTokenEntity = fcmTokenOpt.get();
            String currentToken = fcmTokenEntity.getTokenByPlatform(platform);

            // 요청한 토큰과 DB의 토큰이 일치하는 경우만 처리
            if (currentToken != null && currentToken.equals(fcmToken)) {
                Boolean keepSignedIn = fcmTokenEntity.getKeepSignedInByPlatform(platform);
                Boolean forceDelete = request.getForceDelete();

                // forceDelete가 true면 무조건 삭제, 아니면 keepSignedIn 확인
                if (forceDelete != null && forceDelete) {
                    fcmTokenEntity.setTokenByPlatform(platform, null);
                    fcmTokenEntity.setLastUsedByPlatform(platform, null);
                    fcmTokenEntity.setKeepSignedInByPlatform(platform, null);
                    fcmTokenRepository.save(fcmTokenEntity);
                    logger.info("FCM 토큰 강제 삭제 완료 - 사용자: {}, 플랫폼: {}", user.getUserCode(), platform);
                } else if (keepSignedIn != null && keepSignedIn) {
                    logger.info("FCM 토큰 유지 (로그인 상태 유지 설정) - 사용자: {}, 플랫폼: {}",
                               user.getUserCode(), platform);
                } else {
                    fcmTokenEntity.setTokenByPlatform(platform, null);
                    fcmTokenEntity.setLastUsedByPlatform(platform, null);
                    fcmTokenEntity.setKeepSignedInByPlatform(platform, null);
                    fcmTokenRepository.save(fcmTokenEntity);
                    logger.info("FCM 토큰 삭제 완료 - 사용자: {}, 플랫폼: {}", user.getUserCode(), platform);
                }
            } else {
                logger.warn("FCM 토큰 불일치로 삭제 실패 - 사용자: {}, 플랫폼: {}", user.getUserCode(), platform);
            }
        } else {
            logger.warn("FCM 토큰 정보 없음 - 사용자: {}", user.getUserCode());
        }
    }

    /**
     * 특정 사용자에게 알림 전송
     * targetType과 targeta 필드를 사용한 새로운 방식 또는 userCode를 사용한 기존 방식 지원
     */
    public FcmSendResponse sendNotification(FcmSendRequest request) {
        // 새로운 방식: targetType과 targeta 사용
        if (request.getTargetType() != null && request.getTargeta() != null && !request.getTargeta().isEmpty()) {
            logger.info("FCM 알림 전송 요청 (새 방식) - 타입: {}, 대상 수: {}, 제목: {}",
                    request.getTargetType(), request.getTargeta().size(), request.getTitle());

            // USER 타입인 경우 여러 사용자에게 전송
            if ("USER".equalsIgnoreCase(request.getTargetType())) {
                return sendToMultipleUsers(request);
            } else {
                logger.warn("지원하지 않는 targetType: {}", request.getTargetType());
                throw new IllegalArgumentException("지원하지 않는 targetType입니다: " + request.getTargetType());
            }
        }

        // 기존 방식: userCode 사용
        String userCode = request.getUserCode();
        if (userCode == null || userCode.trim().isEmpty()) {
            throw new IllegalArgumentException("userCode 또는 (targetType + targeta)가 필요합니다");
        }

        logger.info("FCM 알림 전송 요청 (기존 방식) - 대상: {}, 제목: {}", userCode, request.getTitle());

        Optional<FcmToken> fcmTokenOpt = fcmTokenRepository.findByUserCode(userCode);
        if (!fcmTokenOpt.isPresent()) {
            logger.warn("FCM 토큰 정보 없음 - 사용자: {}", userCode);
            throw new ResourceNotFoundException("사용자의 FCM 토큰 정보를 찾을 수 없습니다");
        }

        FcmToken fcmToken = fcmTokenOpt.get();
        Map<String, Boolean> sent = new LinkedHashMap<>();
        Map<String, String> failedReasons = new LinkedHashMap<>();

        for (String platform : Arrays.asList("ANDROID", "IOS", "WEB")) {
            PlatformDeliverySummary summary = sendToPlatform(fcmToken, platform, request);
            String key = platform.toLowerCase(Locale.ROOT);
            sent.put(key, summary.isSuccess());
            if (summary.getFailureReason() != null && !summary.getFailureReason().isEmpty()) {
                failedReasons.put(key, summary.getFailureReason());
            } else {
                failedReasons.remove(key);
            }
        }

        return new FcmSendResponse("success", sent, failedReasons);
    }

    /**
     * 여러 사용자에게 알림 전송 (새로운 방식)
     */
    private FcmSendResponse sendToMultipleUsers(FcmSendRequest request) {
        Set<String> userCodes = request.getTargeta().stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(code -> !code.isEmpty())
                .collect(Collectors.toCollection(LinkedHashSet::new));

        if (userCodes.isEmpty()) {
            throw new IllegalArgumentException("targeta에 전송 대상이 필요합니다");
        }

        Map<String, Boolean> sent = new LinkedHashMap<>();
        Map<String, String> failedReasons = new LinkedHashMap<>();

        for (String userCode : userCodes) {
            Optional<FcmToken> fcmTokenOpt = fcmTokenRepository.findByUserCode(userCode);
            if (!fcmTokenOpt.isPresent()) {
                logger.warn("FCM 토큰 정보 없음 - 사용자: {}", userCode);
                sent.put(userCode, false);
                failedReasons.put(userCode, "사용자의 FCM 토큰 정보를 찾을 수 없습니다");
                continue;
            }

            FcmToken fcmToken = fcmTokenOpt.get();
            boolean anySuccess = false;
            List<String> failureMessages = new ArrayList<>();

            for (String platform : Arrays.asList("ANDROID", "IOS", "WEB")) {
                PlatformDeliverySummary summary = sendToPlatform(fcmToken, platform, request);
                if (summary.isSuccess()) {
                    anySuccess = true;
                } else if (summary.getFailureReason() != null && !summary.getFailureReason().isEmpty()) {
                    failureMessages.add(platform.toLowerCase(Locale.ROOT) + ": " + summary.getFailureReason());
                }
            }

            sent.put(userCode, anySuccess);
            if (!failureMessages.isEmpty()) {
                failedReasons.put(userCode, String.join("; ", failureMessages));
            }
        }

        return new FcmSendResponse("success", sent, failedReasons);
    }

    /**
     * 특정 플랫폼으로 알림 전송 (DB + Redis 임시 토큰)
     */
    private PlatformDeliverySummary sendToPlatform(FcmToken fcmToken, String platform, FcmSendRequest request) {
        List<DeliveryAttemptResult> attempts = new ArrayList<>();

        String dbToken = fcmToken.getTokenByPlatform(platform);
        String tempToken = fcmSessionService.getTemporaryToken(fcmToken.getUserIdx(), platform);

        if (dbToken != null) {
            attempts.add(sendToToken(dbToken, fcmToken, platform, request, false));
        }

        if (tempToken != null && !Objects.equals(tempToken, dbToken)) {
            attempts.add(sendToToken(tempToken, fcmToken, platform, request, true));
        }

        if (attempts.isEmpty()) {
            return new PlatformDeliverySummary(false, "토큰이 등록되지 않았습니다");
        }

        boolean success = attempts.stream().anyMatch(DeliveryAttemptResult::isSuccess);
        if (success) {
            return new PlatformDeliverySummary(true, null);
        }

        String reason = attempts.stream()
                .map(DeliveryAttemptResult::getFailureReason)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.joining("; "));

        if (reason.isEmpty()) {
            reason = "전송 실패";
        }

        return new PlatformDeliverySummary(false, reason);
    }

    /**
     * 개별 FCM 토큰으로 알림 전송
     */
    private DeliveryAttemptResult sendToToken(String token, FcmToken fcmToken, String platform,
                                              FcmSendRequest request, boolean isTemporary) {
        return sendMessageToToken(token, request.getTitle(), request.getBody(), request.getData(),
                fcmToken, platform, isTemporary);
    }

    private DeliveryAttemptResult sendMessageToToken(String token, String title, String body,
                                                     Map<String, String> data, FcmToken fcmToken,
                                                     String platform, boolean isTemporary) {
        try {
            Message.Builder messageBuilder = Message.builder()
                    .setToken(token)
                    .setNotification(Notification.builder()
                            .setTitle(title)
                            .setBody(body)
                            .build());

            if (data != null && !data.isEmpty()) {
                messageBuilder.putAllData(data);
            }

            Message message = messageBuilder.build();
            String response = firebaseMessaging.send(message);

            if (!isTemporary) {
                fcmToken.setLastUsedByPlatform(platform, LocalDateTime.now());
                fcmTokenRepository.save(fcmToken);
            }

            logger.info("FCM 알림 전송 성공 - 사용자: {}, 플랫폼: {}, 임시: {}, 응답: {}",
                    fcmToken.getUserCode(), platform, isTemporary, response);
            return DeliveryAttemptResult.success();

        } catch (FirebaseMessagingException e) {
            String errorCode = e.getErrorCode() != null ? e.getErrorCode().name() : "UNKNOWN";

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
                return DeliveryAttemptResult.failure("토큰이 무효화되어 삭제되었습니다");
            }

            logger.error("FCM 알림 전송 실패 - 사용자: {}, 플랫폼: {}, 임시: {}, 오류: {}",
                    fcmToken.getUserCode(), platform, isTemporary, errorCode, e);
            return DeliveryAttemptResult.failure("전송 실패: " + e.getMessage());

        } catch (Exception e) {
            logger.error("FCM 알림 전송 중 예외 발생 - 사용자: {}, 플랫폼: {}, 임시: {}",
                    fcmToken.getUserCode(), platform, isTemporary, e);
            return DeliveryAttemptResult.failure("시스템 오류: " + e.getMessage());
        }
    }

    private void clearStoredToken(FcmToken fcmToken, String platform) {
        fcmToken.setTokenByPlatform(platform, null);
        fcmToken.setLastUsedByPlatform(platform, null);
        fcmToken.setKeepSignedInByPlatform(platform, null);
        fcmTokenRepository.save(fcmToken);
    }

    /**
     * 여러 사용자에게 일괄 알림 전송
     */
    public FcmBatchSendResponse sendBatchNotification(FcmBatchSendRequest request) {
        logger.info("FCM 일괄 알림 전송 요청 - 대상 사용자 수: {}, 제목: {}",
                   request.getUserCodes().size(), request.getTitle());

        List<FcmBatchSendResponse.UserSendDetail> details = new ArrayList<>();
        int totalSuccess = 0;
        int totalFailure = 0;

        List<String> targetPlatforms = resolvePlatforms(request.getPlatforms());

        for (String userCode : request.getUserCodes()) {
            Optional<FcmToken> fcmTokenOpt = fcmTokenRepository.findByUserCode(userCode);

            if (!fcmTokenOpt.isPresent()) {
                Map<String, Boolean> sent = new HashMap<>();
                Map<String, String> failed = new HashMap<>();

                for (String platform : targetPlatforms) {
                    String key = platform.toLowerCase(Locale.ROOT);
                    sent.put(key, false);
                    failed.put(key, "사용자의 FCM 토큰 정보를 찾을 수 없습니다");
                }

                totalFailure += targetPlatforms.size();
                details.add(new FcmBatchSendResponse.UserSendDetail(userCode, sent, failed));
                continue;
            }

            FcmToken fcmToken = fcmTokenOpt.get();
            Map<String, Boolean> sent = new HashMap<>();
            Map<String, String> failed = new HashMap<>();

            // 요청된 플랫폼으로만 전송
            for (String platform : targetPlatforms) {
                String key = platform.toLowerCase(Locale.ROOT);
                List<DeliveryAttemptResult> attempts = new ArrayList<>();

                String dbToken = fcmToken.getTokenByPlatform(platform);
                if (dbToken != null) {
                    attempts.add(sendMessageToToken(dbToken, request.getTitle(), request.getBody(),
                            request.getData(), fcmToken, platform, false));
                }

                String tempToken = fcmSessionService.getTemporaryToken(fcmToken.getUserIdx(), platform);
                if (tempToken != null && !Objects.equals(tempToken, dbToken)) {
                    attempts.add(sendMessageToToken(tempToken, request.getTitle(), request.getBody(),
                            request.getData(), fcmToken, platform, true));
                }

                if (attempts.isEmpty()) {
                    sent.put(key, false);
                    failed.put(key, "토큰이 등록되지 않았습니다");
                    totalFailure++;
                    continue;
                }

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

            details.add(new FcmBatchSendResponse.UserSendDetail(userCode, sent, failed));
        }

        logger.info("FCM 일괄 알림 전송 완료 - 총 사용자: {}, 성공: {}, 실패: {}",
                   request.getUserCodes().size(), totalSuccess, totalFailure);

        return new FcmBatchSendResponse("success", request.getUserCodes().size(),
                                       totalSuccess, totalFailure, details);
    }

    /**
     * 전체 사용자에게 Data-only 브로드캐스트 알림 전송
     * 알림 표시 없이 데이터만 전송 (백그라운드 업데이트용)
     */
    public FcmDataOnlyBroadcastResponse sendDataOnlyBroadcast(FcmDataOnlyBroadcastRequest request) {
        logger.info("FCM Data-only 브로드캐스트 알림 전송 요청 - 제목: {}", request.getTitle());

        List<String> targetPlatforms = resolvePlatforms(request.getPlatforms());

        // 필터 적용
        List<FcmToken> allTokens = fcmTokenRepository.findAll();
        if (request.getFilter() != null && request.getFilter().getUserType() != null) {
            String userType = request.getFilter().getUserType().toUpperCase();
            if (!"ALL".equals(userType)) {
                Integer userStudent = "STUDENT".equals(userType) ? 0 : 1;
                allTokens = filterTokensByUserType(allTokens, userStudent);
            }
        }

        LinkedHashSet<String> allValidTokens = new LinkedHashSet<>();

        // 플랫폼별로 토큰 수집
        // 브로드캐스트는 DB에 등록된 영구 토큰만 사용 (임시 토큰 제외하여 중복 방지)
        for (FcmToken fcmToken : allTokens) {
            for (String platform : targetPlatforms) {
                String token = fcmToken.getTokenByPlatform(platform);
                if (token != null) {
                    allValidTokens.add(token);
                }
            }
        }

        if (allValidTokens.isEmpty()) {
            logger.warn("FCM Data-only 브로드캐스트 전송할 토큰이 없습니다");
            return new FcmDataOnlyBroadcastResponse("success", 0, 0, 0, new ArrayList<>());
        }

        logger.info("📤 Sending data-only broadcast to {} tokens", allValidTokens.size());

        // Data-only 배치 전송 사용
        BatchSendResponse batchResponse = firebasePushService.sendDataOnlyNotificationBatch(
                new ArrayList<>(allValidTokens),
                request.getTitle(),
                request.getBody(),
                request.getData()
        );

        int successCount = batchResponse.getSuccessCount();
        int failureCount = batchResponse.getFailureCount();
        List<String> invalidTokens = new ArrayList<>();

        // 실패한 토큰 중 무효화된 토큰 처리
        if (batchResponse.getResponses() != null) {
            for (TokenSendResult result : batchResponse.getResponses()) {
                if (!result.isSuccess()) {
                    String error = result.getError();
                    if (error != null && (error.contains("NOT_FOUND") ||
                                         error.contains("INVALID_ARGUMENT") ||
                                         error.contains("REGISTRATION_TOKEN_NOT_REGISTERED") ||
                                         error.contains("UNREGISTERED"))) {
                        invalidTokens.add(result.getToken());
                        removeInvalidToken(result.getToken());
                    }
                }
            }
        }

        logger.info("✅ FCM Data-only 브로드캐스트 전송 완료 - 총 토큰: {}, 성공: {}, 실패: {}, 무효화: {}",
                allValidTokens.size(), successCount, failureCount, invalidTokens.size());

        return new FcmDataOnlyBroadcastResponse("success", allValidTokens.size(), successCount,
                failureCount, invalidTokens);
    }

    /**
     * 전체 사용자에게 브로드캐스트 알림 전송
     */
    public FcmBroadcastResponse sendBroadcast(FcmBroadcastRequest request) {
        logger.info("FCM 브로드캐스트 알림 전송 요청 - 제목: {}", request.getTitle());

        List<String> targetPlatforms = resolvePlatforms(request.getPlatforms());

        // 필터 적용
        List<FcmToken> allTokens = fcmTokenRepository.findAll();
        if (request.getFilter() != null && request.getFilter().getUserType() != null) {
            String userType = request.getFilter().getUserType().toUpperCase();
            if (!"ALL".equals(userType)) {
                Integer userStudent = "STUDENT".equals(userType) ? 0 : 1;
                allTokens = filterTokensByUserType(allTokens, userStudent);
            }
        }

        LinkedHashSet<String> allValidTokens = new LinkedHashSet<>();
        List<String> invalidTokens = new ArrayList<>();

        // 플랫폼별로 토큰 수집
        // 브로드캐스트는 DB에 등록된 영구 토큰만 사용 (임시 토큰 제외하여 중복 방지)
        for (FcmToken fcmToken : allTokens) {
            for (String platform : targetPlatforms) {
                String token = fcmToken.getTokenByPlatform(platform);
                if (token != null) {
                    allValidTokens.add(token);
                }
                
                // ⚠️ 브로드캐스트에서는 임시 토큰 제외 (중복 알림 방지)
                // 임시 토큰은 개별 알림 전송에서만 사용
                
                // String tempToken = fcmSessionService.getTemporaryToken(fcmToken.getUserIdx(), platform);
                // if (tempToken != null && !Objects.equals(tempToken, token)) {
                //     allValidTokens.add(tempToken);
                // }
            }
        }

        if (allValidTokens.isEmpty()) {
            logger.warn("FCM 브로드캐스트 전송할 토큰이 없습니다");
            return new FcmBroadcastResponse("success", 0, 0, 0, invalidTokens);
        }

        int successCount = 0;
        int failureCount = 0;

        // FCM은 최대 500개씩 배치 전송 가능
        int batchSize = 500;
        List<String> tokenList = new ArrayList<>(allValidTokens);
        for (int i = 0; i < tokenList.size(); i += batchSize) {
            List<String> batch = tokenList.subList(i, Math.min(i + batchSize, tokenList.size()));

            try {
                MulticastMessage message = MulticastMessage.builder()
                        .addAllTokens(batch)
                        .setNotification(Notification.builder()
                                .setTitle(request.getTitle())
                                .setBody(request.getBody())
                                .build())
                        .putAllData(request.getData() != null ? request.getData() : new HashMap<>())
                        .build();

                BatchResponse response = firebaseMessaging.sendEachForMulticast(message);
                successCount += response.getSuccessCount();
                failureCount += response.getFailureCount();

                // 실패한 토큰 처리
                if (response.getFailureCount() > 0) {
                    List<SendResponse> responses = response.getResponses();
                    for (int j = 0; j < responses.size(); j++) {
                        SendResponse sr = responses.get(j);
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
                                // FirebaseMessagingException이 아닌 다른 예외
                                logger.warn("FCM 브로드캐스트 - 예상치 못한 예외 타입: {}",
                                           sr.getException().getClass().getName());
                            }
                        }
                    }
                }

            } catch (FirebaseMessagingException e) {
                logger.error("FCM 브로드캐스트 배치 전송 실패", e);
                failureCount += batch.size();
            }
        }

        logger.info("FCM 브로드캐스트 전송 완료 - 총 토큰: {}, 성공: {}, 실패: {}, 무효화: {}",
                   allValidTokens.size(), successCount, failureCount, invalidTokens.size());

        return new FcmBroadcastResponse("success", allValidTokens.size(), successCount,
                                       failureCount, invalidTokens);
    }

    /**
     * FCM 토큰 통계 조회
     */
    public FcmStatsResponse getStats() {
        logger.info("FCM 토큰 통계 조회 요청");

        long totalUsers = userTblRepository.count();
        long registeredUsers = fcmTokenRepository.count();

        // 플랫폼별 토큰 수
        Map<String, Integer> byPlatform = new HashMap<>();
        byPlatform.put("android", fcmTokenRepository.findAllAndroidTokens().size());
        byPlatform.put("ios", fcmTokenRepository.findAllIosTokens().size());
        byPlatform.put("web", fcmTokenRepository.findAllWebTokens().size());

        // 30일 이내 활성 토큰
        LocalDateTime activeThreshold = LocalDateTime.now().minusDays(30);
        List<FcmToken> allTokens = fcmTokenRepository.findAll();

        Map<String, Integer> activeTokens = new HashMap<>();
        Map<String, Integer> inactiveTokens = new HashMap<>();

        int androidActive = 0, iosActive = 0, webActive = 0;
        int androidInactive = 0, iosInactive = 0, webInactive = 0;

        LocalDateTime inactiveThreshold = LocalDateTime.now().minusDays(90);

        for (FcmToken token : allTokens) {
            // Android
            if (token.getFcmTokenAndroid() != null) {
                LocalDateTime lastUsed = token.getFcmTokenAndroidLastUsed();
                if (lastUsed != null && lastUsed.isAfter(activeThreshold)) {
                    androidActive++;
                } else if (lastUsed == null || lastUsed.isBefore(inactiveThreshold)) {
                    androidInactive++;
                }
            }

            // iOS
            if (token.getFcmTokenIos() != null) {
                LocalDateTime lastUsed = token.getFcmTokenIosLastUsed();
                if (lastUsed != null && lastUsed.isAfter(activeThreshold)) {
                    iosActive++;
                } else if (lastUsed == null || lastUsed.isBefore(inactiveThreshold)) {
                    iosInactive++;
                }
            }

            // Web
            if (token.getFcmTokenWeb() != null) {
                LocalDateTime lastUsed = token.getFcmTokenWebLastUsed();
                if (lastUsed != null && lastUsed.isAfter(activeThreshold)) {
                    webActive++;
                } else if (lastUsed == null || lastUsed.isBefore(inactiveThreshold)) {
                    webInactive++;
                }
            }
        }

        activeTokens.put("android", androidActive);
        activeTokens.put("ios", iosActive);
        activeTokens.put("web", webActive);

        inactiveTokens.put("android", androidInactive);
        inactiveTokens.put("ios", iosInactive);
        inactiveTokens.put("web", webInactive);

        return new FcmStatsResponse((int) totalUsers, (int) registeredUsers, byPlatform,
                                   activeTokens, inactiveTokens);
    }

    /**
     * 사용자 유형별 토큰 필터링
     */
    private List<FcmToken> filterTokensByUserType(List<FcmToken> tokens, Integer userStudent) {
        List<FcmToken> filtered = new ArrayList<>();
        for (FcmToken token : tokens) {
            Optional<UserTbl> userOpt = userTblRepository.findById(token.getUserIdx());
            if (userOpt.isPresent() && userOpt.get().getUserStudent().equals(userStudent)) {
                filtered.add(token);
            }
        }
        return filtered;
    }

    /**
     * 무효화된 토큰 제거
     */
    private void removeInvalidToken(String token) {
        Optional<FcmToken> fcmTokenOpt = fcmTokenRepository.findByAnyToken(token);
        if (fcmTokenOpt.isPresent()) {
            FcmToken fcmToken = fcmTokenOpt.get();

            boolean updated = false;

            if (token.equals(fcmToken.getFcmTokenAndroid())) {
                fcmToken.setFcmTokenAndroid(null);
                fcmToken.setFcmTokenAndroidLastUsed(null);
                fcmToken.setFcmTokenAndroidKeepSignedIn(null);
                updated = true;
            }

            if (token.equals(fcmToken.getFcmTokenIos())) {
                fcmToken.setFcmTokenIos(null);
                fcmToken.setFcmTokenIosLastUsed(null);
                fcmToken.setFcmTokenIosKeepSignedIn(null);
                updated = true;
            }

            if (token.equals(fcmToken.getFcmTokenWeb())) {
                fcmToken.setFcmTokenWeb(null);
                fcmToken.setFcmTokenWebLastUsed(null);
                fcmToken.setFcmTokenWebKeepSignedIn(null);
                updated = true;
            }

            if (updated) {
                fcmTokenRepository.save(fcmToken);
                logger.info("무효화된 토큰 제거 완료 - 사용자: {}", fcmToken.getUserCode());
            }
        }
    }

    @Scheduled(cron = "0 0 3 * * *")
    @Transactional
    public void cleanupInactiveTokens() {
        LocalDateTime cutoffDate = LocalDateTime.now().minusDays(90);
        List<FcmToken> inactiveTokens = fcmTokenRepository.findInactiveTokens(cutoffDate);

        if (inactiveTokens.isEmpty()) {
            logger.debug("정리 대상 미사용 FCM 토큰이 없습니다");
            return;
        }

        int clearedTokens = 0;

        for (FcmToken token : inactiveTokens) {
            boolean updated = false;

            if (token.getFcmTokenAndroid() != null &&
                (token.getFcmTokenAndroidLastUsed() == null || token.getFcmTokenAndroidLastUsed().isBefore(cutoffDate))) {
                token.setFcmTokenAndroid(null);
                token.setFcmTokenAndroidLastUsed(null);
                token.setFcmTokenAndroidKeepSignedIn(null);
                updated = true;
            }

            if (token.getFcmTokenIos() != null &&
                (token.getFcmTokenIosLastUsed() == null || token.getFcmTokenIosLastUsed().isBefore(cutoffDate))) {
                token.setFcmTokenIos(null);
                token.setFcmTokenIosLastUsed(null);
                token.setFcmTokenIosKeepSignedIn(null);
                updated = true;
            }

            if (token.getFcmTokenWeb() != null &&
                (token.getFcmTokenWebLastUsed() == null || token.getFcmTokenWebLastUsed().isBefore(cutoffDate))) {
                token.setFcmTokenWeb(null);
                token.setFcmTokenWebLastUsed(null);
                token.setFcmTokenWebKeepSignedIn(null);
                updated = true;
            }

            if (updated) {
                fcmTokenRepository.save(token);
                clearedTokens++;
            }
        }

        logger.info("미사용 FCM 토큰 정리 완료 - 총 대상: {}, 정리된 토큰: {}",
                inactiveTokens.size(), clearedTokens);
    }
}
