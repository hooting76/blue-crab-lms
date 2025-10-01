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

    public FcmTokenService(FcmTokenRepository fcmTokenRepository,
                           UserTblRepository userTblRepository,
                           FirebaseMessaging firebaseMessaging) {
        this.fcmTokenRepository = fcmTokenRepository;
        this.userTblRepository = userTblRepository;
        this.firebaseMessaging = firebaseMessaging;
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
            // 최초 등록
            fcmTokenEntity.setTokenByPlatform(platform, fcmToken);
            fcmTokenEntity.setLastUsedByPlatform(platform, LocalDateTime.now());
            fcmTokenRepository.save(fcmTokenEntity);
            logger.info("FCM 토큰 최초 등록 - 사용자: {}, 플랫폼: {}", user.getUserCode(), platform);
            return FcmRegisterResponse.registered();

        } else if (currentToken.equals(fcmToken)) {
            // 동일 토큰 재등록 (갱신)
            fcmTokenEntity.setLastUsedByPlatform(platform, LocalDateTime.now());
            fcmTokenRepository.save(fcmTokenEntity);
            logger.info("FCM 토큰 갱신 - 사용자: {}, 플랫폼: {}", user.getUserCode(), platform);
            return FcmRegisterResponse.renewed();

        } else {
            // 다른 토큰 (충돌)
            LocalDateTime lastUsed = fcmTokenEntity.getLastUsedByPlatform(platform);
            logger.info("FCM 토큰 충돌 감지 - 사용자: {}, 플랫폼: {}, 마지막 사용: {}",
                       user.getUserCode(), platform, lastUsed);
            return FcmRegisterResponse.conflict(platform, lastUsed);
        }
    }

    /**
     * FCM 토큰 강제 변경
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
            fcmTokenRepository.save(otherUserToken);
        }

        // 2. 현재 사용자의 FCM 토큰 정보 조회 또는 생성
        FcmToken fcmTokenEntity = fcmTokenRepository.findByUserIdx(user.getUserIdx())
                .orElseGet(() -> new FcmToken(user.getUserIdx(), user.getUserCode()));

        // 3. 강제로 토큰 변경
        fcmTokenEntity.setTokenByPlatform(platform, fcmToken);
        fcmTokenEntity.setLastUsedByPlatform(platform, LocalDateTime.now());
        fcmTokenRepository.save(fcmTokenEntity);

        logger.info("FCM 토큰 강제 변경 완료 - 사용자: {}, 플랫폼: {}", user.getUserCode(), platform);
        return FcmRegisterResponse.replaced();
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

        Optional<FcmToken> fcmTokenOpt = fcmTokenRepository.findByUserIdx(user.getUserIdx());
        if (fcmTokenOpt.isPresent()) {
            FcmToken fcmTokenEntity = fcmTokenOpt.get();
            String currentToken = fcmTokenEntity.getTokenByPlatform(platform);

            // 요청한 토큰과 DB의 토큰이 일치하는 경우만 삭제
            if (currentToken != null && currentToken.equals(fcmToken)) {
                fcmTokenEntity.setTokenByPlatform(platform, null);
                fcmTokenEntity.setLastUsedByPlatform(platform, null);
                fcmTokenRepository.save(fcmTokenEntity);
                logger.info("FCM 토큰 삭제 완료 - 사용자: {}, 플랫폼: {}", user.getUserCode(), platform);
            } else {
                logger.warn("FCM 토큰 불일치로 삭제 실패 - 사용자: {}, 플랫폼: {}", user.getUserCode(), platform);
            }
        } else {
            logger.warn("FCM 토큰 정보 없음 - 사용자: {}", user.getUserCode());
        }
    }

    /**
     * 특정 사용자에게 알림 전송
     */
    @Transactional
    public FcmSendResponse sendNotification(FcmSendRequest request) {
        String userCode = request.getUserCode();

        logger.info("FCM 알림 전송 요청 - 대상: {}, 제목: {}", userCode, request.getTitle());

        Optional<FcmToken> fcmTokenOpt = fcmTokenRepository.findByUserCode(userCode);
        if (!fcmTokenOpt.isPresent()) {
            logger.warn("FCM 토큰 정보 없음 - 사용자: {}", userCode);
            throw new RuntimeException("사용자의 FCM 토큰 정보를 찾을 수 없습니다");
        }

        FcmToken fcmToken = fcmTokenOpt.get();
        Map<String, Boolean> sent = new HashMap<>();
        Map<String, String> failedReasons = new HashMap<>();

        // 안드로이드 전송
        sendToPlatform(fcmToken, "ANDROID", request, sent, failedReasons);

        // iOS 전송
        sendToPlatform(fcmToken, "IOS", request, sent, failedReasons);

        // 웹 전송
        sendToPlatform(fcmToken, "WEB", request, sent, failedReasons);

        return new FcmSendResponse("success", sent, failedReasons);
    }

    /**
     * 특정 플랫폼으로 알림 전송
     */
    private void sendToPlatform(FcmToken fcmToken, String platform, FcmSendRequest request,
                                Map<String, Boolean> sent, Map<String, String> failedReasons) {
        String platformKey = platform.toLowerCase(Locale.ROOT);
        String token = fcmToken.getTokenByPlatform(platform);

        if (token == null) {
            sent.put(platformKey, false);
            failedReasons.put(platformKey, "토큰이 등록되지 않았습니다");
            return;
        }

        try {
            // FCM 메시지 생성
            Message.Builder messageBuilder = Message.builder()
                    .setToken(token)
                    .setNotification(Notification.builder()
                            .setTitle(request.getTitle())
                            .setBody(request.getBody())
                            .build());

            // 추가 데이터 있으면 포함
            if (request.getData() != null && !request.getData().isEmpty()) {
                messageBuilder.putAllData(request.getData());
            }

            Message message = messageBuilder.build();

            // 전송
            String response = firebaseMessaging.send(message);

            // 전송 성공
            sent.put(platformKey, true);
            fcmToken.setLastUsedByPlatform(platform, LocalDateTime.now());
            fcmTokenRepository.save(fcmToken);

            logger.info("FCM 알림 전송 성공 - 사용자: {}, 플랫폼: {}, 응답: {}",
                       fcmToken.getUserCode(), platform, response);

        } catch (FirebaseMessagingException e) {
            sent.put(platformKey, false);
            String errorCode = e.getErrorCode() != null ? e.getErrorCode().name() : "UNKNOWN";

            // 토큰 무효화 케이스 처리
            if (isInvalidTokenError(errorCode)) {

                fcmToken.setTokenByPlatform(platform, null);
                fcmToken.setLastUsedByPlatform(platform, null);
                fcmTokenRepository.save(fcmToken);

                failedReasons.put(platformKey, "토큰이 무효화되어 삭제되었습니다");
                logger.warn("FCM 토큰 무효화 - 사용자: {}, 플랫폼: {}, 오류: {}",
                           fcmToken.getUserCode(), platform, errorCode);
            } else {
                failedReasons.put(platformKey, "전송 실패: " + e.getMessage());
                logger.error("FCM 알림 전송 실패 - 사용자: {}, 플랫폼: {}, 오류: {}",
                            fcmToken.getUserCode(), platform, errorCode, e);
            }
        } catch (Exception e) {
            sent.put(platformKey, false);
            failedReasons.put(platformKey, "시스템 오류: " + e.getMessage());
            logger.error("FCM 알림 전송 중 예외 발생 - 사용자: {}, 플랫폼: {}",
                        fcmToken.getUserCode(), platform, e);
        }
    }

    /**
     * 여러 사용자에게 일괄 알림 전송
     */
    @Transactional
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
                String token = fcmToken.getTokenByPlatform(platform);
                String key = platform.toLowerCase(Locale.ROOT);
                if (token == null) {
                    sent.put(key, false);
                    failed.put(key, "토큰이 등록되지 않았습니다");
                    totalFailure++;
                } else {
                    boolean success = sendToSingleToken(token, request.getTitle(), request.getBody(),
                                                       request.getData(), fcmToken, platform);
                    sent.put(key, success);

                    if (success) {
                        totalSuccess++;
                    } else {
                        failed.put(key, "전송 실패");
                        totalFailure++;
                    }
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
     * 전체 사용자에게 브로드캐스트 알림 전송
     */
    @Transactional
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
        for (FcmToken fcmToken : allTokens) {
            for (String platform : targetPlatforms) {
                String token = fcmToken.getTokenByPlatform(platform);
                if (token != null) {
                    allValidTokens.add(token);
                }
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
     * 단일 토큰으로 알림 전송 (내부 헬퍼 메서드)
     */
    private boolean sendToSingleToken(String token, String title, String body,
                                     Map<String, String> data, FcmToken fcmToken, String platform) {
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
            firebaseMessaging.send(message);

            // 전송 성공 - 마지막 사용 시간 업데이트
            fcmToken.setLastUsedByPlatform(platform, LocalDateTime.now());
            fcmTokenRepository.save(fcmToken);

            return true;

        } catch (FirebaseMessagingException e) {
            String errorCode = e.getErrorCode() != null ? e.getErrorCode().name() : "UNKNOWN";

            // 토큰 무효화 케이스
            if (isInvalidTokenError(errorCode)) {
                fcmToken.setTokenByPlatform(platform, null);
                fcmToken.setLastUsedByPlatform(platform, null);
                fcmTokenRepository.save(fcmToken);
            }

            return false;
        } catch (Exception e) {
            return false;
        }
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

            if (token.equals(fcmToken.getFcmTokenAndroid())) {
                fcmToken.setFcmTokenAndroid(null);
                fcmToken.setFcmTokenAndroidLastUsed(null);
            } else if (token.equals(fcmToken.getFcmTokenIos())) {
                fcmToken.setFcmTokenIos(null);
                fcmToken.setFcmTokenIosLastUsed(null);
            } else if (token.equals(fcmToken.getFcmTokenWeb())) {
                fcmToken.setFcmTokenWeb(null);
                fcmToken.setFcmTokenWebLastUsed(null);
            }

            fcmTokenRepository.save(fcmToken);
            logger.info("무효화된 토큰 제거 완료 - 사용자: {}", fcmToken.getUserCode());
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
                updated = true;
            }

            if (token.getFcmTokenIos() != null &&
                (token.getFcmTokenIosLastUsed() == null || token.getFcmTokenIosLastUsed().isBefore(cutoffDate))) {
                token.setFcmTokenIos(null);
                token.setFcmTokenIosLastUsed(null);
                updated = true;
            }

            if (token.getFcmTokenWeb() != null &&
                (token.getFcmTokenWebLastUsed() == null || token.getFcmTokenWebLastUsed().isBefore(cutoffDate))) {
                token.setFcmTokenWeb(null);
                token.setFcmTokenWebLastUsed(null);
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
