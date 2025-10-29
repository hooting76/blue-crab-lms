package BlueCrab.com.example.service;

import BlueCrab.com.example.dto.filter.UserFilterResult;
import BlueCrab.com.example.dto.notification.EmailNotificationRequest;
import BlueCrab.com.example.dto.notification.EmailNotificationResponse;
import BlueCrab.com.example.entity.UserTbl;
import BlueCrab.com.example.repository.UserTblRepository;
import BlueCrab.com.example.service.filter.UserFilterService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 대량 이메일 발송 서비스
 *
 * FCM 필터 로직을 재사용하여 대상자를 선택하고,
 * EmailService를 통해 일괄 메일 전송을 수행한다.
 */
@Service
public class EmailNotificationService {

    private static final Logger logger = LoggerFactory.getLogger(EmailNotificationService.class);

    @Autowired
    private UserFilterService userFilterService;

    @Autowired
    private UserTblRepository userRepository;

    @Autowired
    private EmailService emailService;

    @Value("${spring.mail.username:bluecrabacademy@gmail.com}")
    private String defaultFromAddress;

    /**
     * 필터 조건을 이용해 대상자를 찾고 이메일을 발송한다.
     *
     * @param request 메일 발송 요청
     * @param adminId 발송 요청 관리자 식별자 (로그용)
     * @return 발송 결과
     */
    public EmailNotificationResponse sendBulkEmail(EmailNotificationRequest request, String adminId) {
        UserFilterResult filterResult = userFilterService.filterUsers(request.getFilterCriteria());
        List<String> userCodes = filterResult.getUserCodes();
        int targetCount = filterResult.getTotalCount() != null
            ? filterResult.getTotalCount()
            : (userCodes == null ? 0 : userCodes.size());

        if (userCodes == null || userCodes.isEmpty()) {
            logger.info("Email notification skipped - no targets found. adminId={}, filterType={}",
                adminId, request.getFilterCriteria().getFilterType());
            return EmailNotificationResponse.builder()
                .targetCount(targetCount)
                .resolvedEmailCount(0)
                .successCount(0)
                .failureCount(0)
                .skippedWithoutEmail(targetCount)
                .failedRecipients(Collections.emptyList())
                .build();
        }

        List<UserTbl> users = userRepository.findByUserCodeIn(userCodes);

        Map<String, String> codeToEmail = new LinkedHashMap<>();
        Set<String> missingEmailCodes = new LinkedHashSet<>();

        for (UserTbl user : users) {
            String email = user.getUserEmail();
            if (StringUtils.hasText(email)) {
                codeToEmail.put(user.getUserCode(), email.trim());
            } else {
                missingEmailCodes.add(user.getUserCode());
            }
        }

        // userCodes 중 조회되지 않은 항목도 missing으로 처리
        userCodes.stream()
            .filter(code -> !codeToEmail.containsKey(code) && !missingEmailCodes.contains(code))
            .forEach(missingEmailCodes::add);

        Set<String> recipientEmails = codeToEmail.values().stream()
            .filter(StringUtils::hasText)
            .map(String::trim)
            .collect(Collectors.toCollection(LinkedHashSet::new));

        List<String> failedRecipients = new ArrayList<>();
        int successCount = 0;
        boolean sendAsHtml = request.getSendAsHtml() == null || request.getSendAsHtml();

        for (String email : recipientEmails) {
            try {
                if (sendAsHtml) {
                    emailService.sendMIMEMessage(defaultFromAddress, email, request.getSubject(), request.getBody());
                } else {
                    emailService.sendSimpleMessage(defaultFromAddress, email, request.getSubject(), request.getBody());
                }
                successCount++;
            } catch (Exception ex) {
                failedRecipients.add(email);
                logger.error("Email send failed - email={}, adminId={}, reason={}", email, adminId, ex.getMessage(), ex);
            }
        }

        if (!missingEmailCodes.isEmpty()) {
            logger.warn("Email notification missing addresses - adminId={}, count={}, samples={}",
                adminId, missingEmailCodes.size(),
                missingEmailCodes.stream().limit(5).collect(Collectors.toList()));
        }

        int resolvedEmailCount = recipientEmails.size();
        int failureCount = failedRecipients.size();
        int skippedWithoutEmail = Math.max(0, targetCount - codeToEmail.size());

        logger.info("Email notification completed - adminId={}, targetCount={}, resolvedEmails={}, successCount={}, failureCount={}, skippedWithoutEmail={}",
            adminId, targetCount, resolvedEmailCount, successCount, failureCount, skippedWithoutEmail);

        return EmailNotificationResponse.builder()
            .targetCount(targetCount)
            .resolvedEmailCount(resolvedEmailCount)
            .successCount(successCount)
            .failureCount(failureCount)
            .skippedWithoutEmail(skippedWithoutEmail)
            .failedRecipients(failedRecipients)
            .build();
    }
}
