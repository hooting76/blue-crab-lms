package BlueCrab.com.example.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * 블랙리스트 토큰 영구 저장 및 관리 서비스
 * 
 * 기능:
 * - 블랙리스트 토큰 영구 저장 (보안 감사용)
 * - 자동 정리 (만료 + N일 후 삭제)
 * - 악의적 패턴 감지 및 기록
 * - 관리자 조회 API 제공
 * - 용량 관리 및 로테이션
 * 
 * 저장 정보:
 * - 이메일 (사용자 식별)
 * - 토큰 해시 (보안을 위해 원본 토큰은 해시화)
 * - 블랙리스트 등록 시간
 * - 토큰 원래 만료 시간  
 * - 블랙리스트 사유 (LOGOUT, SECURITY_VIOLATION, EXPIRED 등)
 * - IP 주소 (선택사항)
 * 
 * @author BlueCrab LMS Development Team
 * @version 1.0 (보안 감사 기능)
 */
@Service
public class TokenBlacklistArchiveService {
    
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;
    
    // 저장소 설정
    private static final String BLACKLIST_ARCHIVE_DIR = "security/blacklist";
    private static final String BLACKLIST_REDIS_KEY = "bluecrab:security:blacklist:archive";
    private static final String SUSPICIOUS_PATTERN_KEY = "bluecrab:security:suspicious";
    
    // 보관 정책
    private static final int DAYS_TO_KEEP_AFTER_EXPIRY = 7; // 토큰 만료 후 7일 더 보관
    private static final int MAX_ARCHIVE_RECORDS = 10000; // 최대 보관 레코드 수
    private static final int SUSPICIOUS_THRESHOLD = 5; // 의심 임계값 (5회 이상 블랙리스트)
    
    @PostConstruct
    public void initialize() {
        // 보관 디렉토리 생성
        createArchiveDirectory();
        System.out.println("토큰 블랙리스트 아카이브 서비스가 초기화되었습니다.");
    }
    
    /**
     * 블랙리스트 토큰 아카이브 저장
     */
    public void archiveBlacklistedToken(String email, String token, LocalDateTime expiryTime, 
                                      String reason, String ipAddress) {
        try {
            BlacklistRecord record = new BlacklistRecord(
                email,
                hashToken(token), // 토큰은 해시화해서 저장
                LocalDateTime.now(),
                expiryTime,
                reason,
                ipAddress
            );
            
            // Redis에 저장 (빠른 조회용)
            saveToRedis(record);
            
            // 파일에 영구 저장 (감사용)
            saveToFile(record);
            
            // 의심스러운 패턴 체크
            checkSuspiciousPattern(email, reason);
            
        } catch (Exception e) {
            System.err.println("블랙리스트 토큰 아카이브 저장 오류: " + e.getMessage());
        }
    }
    
    /**
     * Redis에 블랙리스트 레코드 저장
     */
    private void saveToRedis(BlacklistRecord record) {
        try {
            // Hash 구조로 저장: bluecrab:security:blacklist:archive:recordId
            String recordKey = BLACKLIST_REDIS_KEY + ":" + record.getId();
            
            Map<String, Object> recordMap = new HashMap<>();
            recordMap.put("email", record.getEmail());
            recordMap.put("tokenHash", record.getTokenHash());
            recordMap.put("blacklistedAt", record.getBlacklistedAt().toString());
            recordMap.put("expiryTime", record.getExpiryTime().toString());
            recordMap.put("reason", record.getReason());
            recordMap.put("ipAddress", record.getIpAddress());
            
            redisTemplate.opsForHash().putAll(recordKey, recordMap);
            
            // 만료 시간 설정 (토큰 만료 + 보관 일수)
            long ttlDays = DAYS_TO_KEEP_AFTER_EXPIRY;
            if (record.getExpiryTime().isAfter(LocalDateTime.now())) {
                ttlDays += java.time.temporal.ChronoUnit.DAYS.between(LocalDateTime.now(), record.getExpiryTime());
            }
            
            redisTemplate.expire(recordKey, ttlDays, TimeUnit.DAYS);
            
            // 인덱스 리스트에도 추가 (조회용)
            redisTemplate.opsForList().leftPush(BLACKLIST_REDIS_KEY + ":index", record.getId());
            redisTemplate.opsForList().trim(BLACKLIST_REDIS_KEY + ":index", 0, MAX_ARCHIVE_RECORDS - 1);
            
        } catch (Exception e) {
            System.err.println("Redis 블랙리스트 저장 오류: " + e.getMessage());
        }
    }
    
    /**
     * 파일에 블랙리스트 레코드 저장 (CSV 형식)
     */
    private void saveToFile(BlacklistRecord record) {
        try {
            String fileName = BLACKLIST_ARCHIVE_DIR + "/blacklist_" + 
                            LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM")) + ".csv";
            
            File file = new File(fileName);
            boolean isNewFile = !file.exists();
            
            // CSV 헤더 (새 파일인 경우)
            if (isNewFile) {
                String header = "ID,Email,TokenHash,BlacklistedAt,ExpiryTime,Reason,IPAddress\n";
                Files.write(Paths.get(fileName), header.getBytes(), StandardOpenOption.CREATE);
            }
            
            // 레코드 추가
            String csvLine = String.format("%s,%s,%s,%s,%s,%s,%s\n",
                record.getId(),
                record.getEmail(),
                record.getTokenHash(),
                record.getBlacklistedAt().toString(),
                record.getExpiryTime().toString(),
                record.getReason(),
                record.getIpAddress() != null ? record.getIpAddress() : ""
            );
            
            Files.write(Paths.get(fileName), csvLine.getBytes(), StandardOpenOption.APPEND);
            
        } catch (Exception e) {
            System.err.println("파일 블랙리스트 저장 오류: " + e.getMessage());
        }
    }
    
    /**
     * 의심스러운 패턴 감지
     */
    private void checkSuspiciousPattern(String email, String reason) {
        try {
            String suspiciousKey = SUSPICIOUS_PATTERN_KEY + ":" + email;
            
            // 해당 사용자의 블랙리스트 횟수 증가
            Long count = redisTemplate.opsForValue().increment(suspiciousKey);
            
            // null 체크 및 처음이면 TTL 설정 (24시간)
            if (count != null && count == 1) {
                redisTemplate.expire(suspiciousKey, 24, TimeUnit.HOURS);
            }
            
            // 임계값 초과 시 의심 사용자로 기록
            if (count != null && count >= SUSPICIOUS_THRESHOLD) {
                recordSuspiciousUser(email, count, reason);
            }
            
        } catch (Exception e) {
            System.err.println("의심 패턴 체크 오류: " + e.getMessage());
        }
    }
    
    /**
     * 의심 사용자 기록
     */
    private void recordSuspiciousUser(String email, Long violationCount, String reason) {
        try {
            Map<String, Object> suspiciousRecord = new HashMap<>();
            suspiciousRecord.put("email", email);
            suspiciousRecord.put("violationCount", violationCount);
            suspiciousRecord.put("reason", reason);
            suspiciousRecord.put("detectedAt", LocalDateTime.now().toString());
            
            String recordKey = SUSPICIOUS_PATTERN_KEY + ":records:" + System.currentTimeMillis();
            redisTemplate.opsForHash().putAll(recordKey, suspiciousRecord);
            redisTemplate.expire(recordKey, 30, TimeUnit.DAYS); // 30일 보관
            
            System.out.println("⚠️ 의심 사용자 감지: " + email + " (위반 " + violationCount + "회)");
            
        } catch (Exception e) {
            System.err.println("의심 사용자 기록 오류: " + e.getMessage());
        }
    }
    
    /**
     * 블랙리스트 아카이브 조회 (관리자용)
     */
    public List<BlacklistRecord> getBlacklistArchive(int limit) {
        List<BlacklistRecord> records = new ArrayList<>();
        
        try {
            // Redis 인덱스에서 최근 레코드 ID 조회
            List<Object> recordIds = redisTemplate.opsForList().range(BLACKLIST_REDIS_KEY + ":index", 0, limit - 1);
            
            if (recordIds != null) {
                for (Object recordId : recordIds) {
                    String recordKey = BLACKLIST_REDIS_KEY + ":" + recordId;
                    Map<Object, Object> recordData = redisTemplate.opsForHash().entries(recordKey);
                    
                    if (!recordData.isEmpty()) {
                        BlacklistRecord record = BlacklistRecord.fromMap(recordData);
                        records.add(record);
                    }
                }
            }
            
        } catch (Exception e) {
            System.err.println("블랙리스트 아카이브 조회 오류: " + e.getMessage());
        }
        
        return records;
    }
    
    /**
     * 의심 사용자 목록 조회
     */
    public List<Map<String, Object>> getSuspiciousUsers() {
        List<Map<String, Object>> suspiciousUsers = new ArrayList<>();
        
        try {
            Set<String> keys = redisTemplate.keys(SUSPICIOUS_PATTERN_KEY + ":records:*");
            
            if (keys != null) {
                for (String key : keys) {
                    Map<Object, Object> userData = redisTemplate.opsForHash().entries(key);
                    if (!userData.isEmpty()) {
                        Map<String, Object> userMap = new HashMap<>();
                        userData.forEach((k, v) -> userMap.put(k.toString(), v));
                        suspiciousUsers.add(userMap);
                    }
                }
            }
            
        } catch (Exception e) {
            System.err.println("의심 사용자 조회 오류: " + e.getMessage());
        }
        
        return suspiciousUsers;
    }
    
    /**
     * 자동 정리 스케줄러 (매일 새벽 2시)
     */
    @Scheduled(cron = "0 0 2 * * ?")
    public void cleanupExpiredRecords() {
        try {
            System.out.println("블랙리스트 아카이브 정리 작업 시작...");
            
            int cleanedCount = 0;
            List<Object> allRecordIds = redisTemplate.opsForList().range(BLACKLIST_REDIS_KEY + ":index", 0, -1);
            
            if (allRecordIds != null) {
                for (Object recordId : allRecordIds) {
                    String recordKey = BLACKLIST_REDIS_KEY + ":" + recordId;
                    Map<Object, Object> recordData = redisTemplate.opsForHash().entries(recordKey);
                    
                    if (!recordData.isEmpty()) {
                        String expiryTimeStr = (String) recordData.get("expiryTime");
                        if (expiryTimeStr != null) {
                            LocalDateTime expiryTime = LocalDateTime.parse(expiryTimeStr);
                            LocalDateTime cleanupTime = expiryTime.plusDays(DAYS_TO_KEEP_AFTER_EXPIRY);
                            
                            if (LocalDateTime.now().isAfter(cleanupTime)) {
                                redisTemplate.delete(recordKey);
                                // Redis List에서 해당 레코드 ID 제거 (count=1로 첫 번째 일치하는 항목만 제거)
                                redisTemplate.opsForList().remove(BLACKLIST_REDIS_KEY + ":index", 1, recordId);
                                cleanedCount++;
                            }
                        }
                    }
                }
            }
            
            System.out.println("블랙리스트 아카이브 정리 완료: " + cleanedCount + "개 레코드 삭제");
            
        } catch (Exception e) {
            System.err.println("블랙리스트 아카이브 정리 오류: " + e.getMessage());
        }
    }
    
    /**
     * 보관 디렉토리 생성
     */
    private void createArchiveDirectory() {
        try {
            Files.createDirectories(Paths.get(BLACKLIST_ARCHIVE_DIR));
        } catch (Exception e) {
            System.err.println("아카이브 디렉토리 생성 오류: " + e.getMessage());
        }
    }
    
    /**
     * 토큰 해시화 (보안)
     */
    private String hashToken(String token) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(token.getBytes());
            StringBuilder hexString = new StringBuilder();
            
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            
            return hexString.toString();
        } catch (Exception e) {
            return "hash_error_" + System.currentTimeMillis();
        }
    }
    
    /**
     * 블랙리스트 레코드 클래스
     */
    public static class BlacklistRecord {
        private String id;
        private String email;
        private String tokenHash;
        private LocalDateTime blacklistedAt;
        private LocalDateTime expiryTime;
        private String reason;
        private String ipAddress;
        
        public BlacklistRecord(String email, String tokenHash, LocalDateTime blacklistedAt,
                             LocalDateTime expiryTime, String reason, String ipAddress) {
            this.id = "BL_" + System.currentTimeMillis() + "_" + (int)(Math.random() * 1000);
            this.email = email;
            this.tokenHash = tokenHash;
            this.blacklistedAt = blacklistedAt;
            this.expiryTime = expiryTime;
            this.reason = reason;
            this.ipAddress = ipAddress;
        }
        
        public static BlacklistRecord fromMap(Map<Object, Object> map) {
            BlacklistRecord record = new BlacklistRecord(
                (String) map.get("email"),
                (String) map.get("tokenHash"),
                LocalDateTime.parse((String) map.get("blacklistedAt")),
                LocalDateTime.parse((String) map.get("expiryTime")),
                (String) map.get("reason"),
                (String) map.get("ipAddress")
            );
            record.id = (String) map.get("id");
            return record;
        }
        
        // Getters
        public String getId() { return id; }
        public String getEmail() { return email; }
        public String getTokenHash() { return tokenHash; }
        public LocalDateTime getBlacklistedAt() { return blacklistedAt; }
        public LocalDateTime getExpiryTime() { return expiryTime; }
        public String getReason() { return reason; }
        public String getIpAddress() { return ipAddress; }
    }
}