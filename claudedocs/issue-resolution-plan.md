# 상담 시스템 구현 이슈 해결 계획서

**작성일:** 2025-10-23
**버전:** 1.0
**기반 문서:** [implementation-plan-v2.md](../docs/consultation/implementation-plan-v2.md)

---

## 목차

1. [이슈 요약](#1-이슈-요약)
2. [Critical 이슈 해결 방안](#2-critical-이슈-해결-방안)
3. [High Priority 이슈 해결 방안](#3-high-priority-이슈-해결-방안)
4. [Medium Priority 이슈](#4-medium-priority-이슈)
5. [구현 계획 업데이트](#5-구현-계획-업데이트)
6. [추가 확인 필요 사항](#6-추가-확인-필요-사항)

---

## 1. 이슈 요약

### 1.1 발견된 이슈 분류

| 우선순위 | 이슈 | 영향 | 조치 |
|---------|------|------|------|
| **🔴 Critical** | Redis TTL 48시간 고정 | 장기/주말 상담 데이터 유실 | 즉시 수정 |
| **🔴 Critical** | MinIO 평문 저장 | 개인정보 유출 위험 | 즉시 수정 |
| **🟡 High** | NOTIFICATION_TBL 제거 | 감사 추적 불가, 재전송 불가 | 대안 구현 |
| **🟡 High** | 백업 스케줄러 성능 | 규모 증가 시 I/O 병목 | 최적화 |
| **🟡 High** | 통합 테스트 삭제 | 배포 안정성 저하 | 최소 E2E 복원 |
| **🟢 Medium** | WebSocket 브로커 | 스케일아웃 불가 | 문서화 |

### 1.2 예상 영향

| 항목 | v2.0 (원본) | v2.1 (수정) | 변경 |
|------|-------------|-------------|------|
| **구현 시간** | 26시간 | **29.5시간** | +3.5h |
| **파일 수** | 24개 | **27개** | +3개 |
| **테이블 수** | 1개 | **2개** | +1개 |
| **안정성** | 중간 | **높음** | ⬆️⬆️ |
| **보안 수준** | 낮음 | **높음** | ⬆️⬆️⬆️ |

---

## 2. Critical 이슈 해결 방안

### 2.1 Redis TTL 48시간 문제 🔴

#### 문제 분석

**현재 구현:**
```java
// 상담 시작 시 한 번만 TTL 설정
redisTemplate.expire("chat:room:" + requestIdx, 48, TimeUnit.HOURS);
```

**문제 시나리오:**
```
[금요일 18:00] 상담 시작 → TTL 48시간 설정
[일요일 18:00] TTL 만료 → Redis 자동 삭제
[월요일 09:00] 학생/교수 재접속 → 채팅 내역 전부 유실 ❌
```

**추가 문제:**
- MinIO temp 백업도 48시간 후 삭제 → 복구 불가능
- 장기 상담(며칠에 걸친 상담) 지원 불가

#### 해결 방안

##### A) Redis TTL 동적 갱신

**구현 위치:** `ChatService.sendMessage()`

```java
@Service
@RequiredArgsConstructor
public class ChatService {

    private final RedisTemplate<String, Object> redisTemplate;
    private static final long CHAT_TTL_HOURS = 168; // 7일 (168시간)

    public void sendMessage(ChatMessageDto message) {
        String key = "chat:room:" + message.getRequestIdx();

        // 1. 메시지 저장
        redisTemplate.opsForList().rightPush(key, message);

        // 2. TTL 갱신 (매 메시지마다)
        redisTemplate.expire(key, CHAT_TTL_HOURS, TimeUnit.HOURS);

        log.debug("메시지 저장 및 TTL 갱신: key={}, ttl={}h", key, CHAT_TTL_HOURS);
    }
}
```

**효과:**
- 메시지 발생 시마다 TTL 리셋 → 활성 상담은 절대 만료 안 됨
- 48시간 → 7일로 연장 → 주말 상담 안전

##### B) MinIO temp 백업 보존 기간 연장

**현재:** temp 스냅샷도 48시간 후 삭제
**변경:** 7일 보존 → Redis TTL과 동기화

```java
@Scheduled(cron = "0 0 3 * * *") // 매일 새벽 3시
public void cleanupOldSnapshots() {
    LocalDateTime cutoff = LocalDateTime.now().minusDays(7); // 7일 기준

    // MinIO temp/에서 7일 이상 된 스냅샷 삭제
    List<String> oldSnapshots = minioService.listFiles("consultations/temp/")
        .stream()
        .filter(file -> extractTimestamp(file).isBefore(cutoff))
        .collect(Collectors.toList());

    oldSnapshots.forEach(minioService::deleteFile);

    log.info("오래된 스냅샷 {}개 삭제 완료", oldSnapshots.size());
}
```

##### C) 자동 종료 로직 강화

**추가 조건:** 7일 비활성 상담 강제 종료

```java
@Scheduled(fixedDelay = 3600000) // 1시간마다
public void checkLongInactiveConsultations() {
    LocalDateTime sevenDaysAgo = LocalDateTime.now().minusDays(7);

    List<ConsultationRequest> longInactive = repository
        .findByConsultationStatusAndLastActivityAtBefore(
            ConsultationStatus.IN_PROGRESS,
            sevenDaysAgo
        );

    for (ConsultationRequest consultation : longInactive) {
        log.warn("7일 비활성 상담 강제 종료: requestIdx={}",
            consultation.getRequestIdx());

        // 강제 종료 + MinIO archive 이동
        autoCloseConsultation(consultation);
    }
}
```

#### 구현 체크리스트

- [ ] ChatService.sendMessage()에 TTL 갱신 로직 추가
- [ ] TTL 기간: 48h → 168h (7일) 변경
- [ ] OrphanedRoomCleanupScheduler에 7일 기준 추가
- [ ] ConsultationAutoCloseScheduler에 7일 비활성 체크 추가
- [ ] Redis TTL 테스트 (메시지 전송 시 갱신 확인)

---

### 2.2 MinIO 평문 저장 보안 문제 🔴

#### 문제 분석

**현재 구현:**
```java
// 평문 텍스트로 업로드
String chatLog = formatChatLog(messages);
minioService.uploadFile("consultations/archive/chat_123_final.txt", chatLog);
```

**유출 시 노출 정보:**
```text
[상담 정보]
학생: 김철수 (2024001234)  ← 식별 정보
교수: 이교수 (P2024001)     ← 식별 정보

[채팅 내용]
[14:05] 김철수: 부모님이 이혼하셔서 학업에 집중이 안 됩니다  ← 민감 정보
[14:06] 이교수: 상담센터 연계가 필요할 것 같습니다          ← 민감 정보
```

**법적 리스크:**
- 개인정보보호법 위반 (민감정보 암호화 미적용)
- 대학 정보보호 정책 위반
- 유출 시 법적 책임

#### 해결 방안

##### A) 암호화 서비스 추가

**새 파일:** `EncryptionService.java`

```java
@Service
@Slf4j
public class EncryptionService {

    private static final String ALGORITHM = "AES/GCM/NoPadding";
    private static final int GCM_IV_LENGTH = 12;
    private static final int GCM_TAG_LENGTH = 128;

    @Value("${encryption.secret-key}")
    private String secretKeyString;

    private SecretKey secretKey;

    @PostConstruct
    public void init() {
        // Base64 인코딩된 키를 SecretKey로 변환
        byte[] decodedKey = Base64.getDecoder().decode(secretKeyString);
        this.secretKey = new SecretKeySpec(decodedKey, 0, decodedKey.length, "AES");
    }

    /**
     * AES-256-GCM 암호화
     */
    public String encrypt(String plainText) throws Exception {
        try {
            Cipher cipher = Cipher.getInstance(ALGORITHM);

            // IV 생성 (12 bytes for GCM)
            byte[] iv = new byte[GCM_IV_LENGTH];
            new SecureRandom().nextBytes(iv);
            GCMParameterSpec parameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);

            // 암호화
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, parameterSpec);
            byte[] cipherText = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));

            // IV + 암호문 결합
            byte[] encrypted = new byte[GCM_IV_LENGTH + cipherText.length];
            System.arraycopy(iv, 0, encrypted, 0, GCM_IV_LENGTH);
            System.arraycopy(cipherText, 0, encrypted, GCM_IV_LENGTH, cipherText.length);

            return Base64.getEncoder().encodeToString(encrypted);

        } catch (Exception e) {
            log.error("암호화 실패", e);
            throw new EncryptionException("데이터 암호화 중 오류 발생", e);
        }
    }

    /**
     * AES-256-GCM 복호화
     */
    public String decrypt(String encryptedText) throws Exception {
        try {
            byte[] decoded = Base64.getDecoder().decode(encryptedText);

            // IV 추출
            byte[] iv = new byte[GCM_IV_LENGTH];
            System.arraycopy(decoded, 0, iv, 0, GCM_IV_LENGTH);

            // 암호문 추출
            byte[] cipherText = new byte[decoded.length - GCM_IV_LENGTH];
            System.arraycopy(decoded, GCM_IV_LENGTH, cipherText, 0, cipherText.length);

            // 복호화
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            GCMParameterSpec parameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.DECRYPT_MODE, secretKey, parameterSpec);

            byte[] plainText = cipher.doFinal(cipherText);

            return new String(plainText, StandardCharsets.UTF_8);

        } catch (Exception e) {
            log.error("복호화 실패", e);
            throw new EncryptionException("데이터 복호화 중 오류 발생", e);
        }
    }
}
```

##### B) MinIO 업로드 전 암호화

**수정 파일:** `ConsultationService.endConsultation()`

```java
@Service
@RequiredArgsConstructor
public class ConsultationService {

    private final EncryptionService encryptionService;
    private final MinioService minioService;

    @Transactional
    public void endConsultation(Long requestIdx) throws Exception {
        // 1. Redis에서 메시지 수집
        List<ChatMessageDto> messages = chatService.getAllMessages(requestIdx);

        // 2. 텍스트 포맷팅
        String chatLog = formatChatLog(messages, requestIdx);

        // 3. 암호화 ⚡ 추가
        String encryptedLog = encryptionService.encrypt(chatLog);

        // 4. MinIO 업로드 (암호화된 데이터)
        String objectName = "consultations/archive/chat_" + requestIdx + "_final.enc";
        minioService.uploadEncryptedFile(objectName, encryptedLog);

        // 5. Redis 삭제
        chatService.deleteRoom(requestIdx);

        // 6. DB 업데이트
        updateConsultationStatus(requestIdx, ConsultationStatus.COMPLETED);

        log.info("상담 종료 및 암호화 저장 완료: requestIdx={}", requestIdx);
    }
}
```

##### C) 다운로드 후 복호화

**수정 파일:** `ConsultationService.getHistoryDetail()`

```java
public ConsultationDetailDto getHistoryDetail(Long requestIdx) throws Exception {
    // 1. DB에서 상담 정보 조회
    ConsultationRequest consultation = findByIdOrThrow(requestIdx);

    // 2. MinIO에서 암호화된 파일 다운로드
    String objectName = "consultations/archive/chat_" + requestIdx + "_final.enc";
    String encryptedLog = minioService.downloadFile(objectName);

    // 3. 복호화 ⚡ 추가
    String chatLog = encryptionService.decrypt(encryptedLog);

    // 4. DTO 생성
    return ConsultationDetailDto.builder()
        .requestIdx(requestIdx)
        .chatLog(chatLog)
        // ... 기타 필드
        .build();
}
```

##### D) 암호화 키 관리

**application.yml 또는 환경 변수:**

```yaml
encryption:
  # AES-256 키 (32 bytes = 256 bits, Base64 인코딩)
  # 실제 운영 환경에서는 AWS KMS, HashiCorp Vault 등 사용 권장
  secret-key: ${ENCRYPTION_SECRET_KEY:ChangeThisToRandomBase64EncodedKey==}
```

**키 생성 방법:**
```java
// 최초 1회 실행하여 키 생성
KeyGenerator keyGen = KeyGenerator.getInstance("AES");
keyGen.init(256);
SecretKey secretKey = keyGen.generateKey();
String base64Key = Base64.getEncoder().encodeToString(secretKey.getEncoded());
System.out.println("Generated Key: " + base64Key);
```

##### E) MinIO 버킷 정책 설정

**MinIO 버킷 접근 제어:**

```json
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Principal": {
        "AWS": ["arn:aws:iam::account-id:user/blue-crab-app"]
      },
      "Action": [
        "s3:GetObject",
        "s3:PutObject",
        "s3:DeleteObject"
      ],
      "Resource": ["arn:aws:s3:::consultations/*"]
    }
  ]
}
```

##### F) 보존 기간 정책

**MinIO Object Lifecycle 설정:**

```xml
<LifecycleConfiguration>
  <Rule>
    <ID>DeleteOldConsultations</ID>
    <Status>Enabled</Status>
    <Prefix>consultations/archive/</Prefix>
    <Expiration>
      <Days>1095</Days> <!-- 3년 (대학 행정 기준) -->
    </Expiration>
  </Rule>
</LifecycleConfiguration>
```

#### 구현 체크리스트

- [ ] EncryptionService.java 생성
- [ ] AES-256-GCM 암호화/복호화 구현
- [ ] application.yml에 암호화 키 설정
- [ ] ConsultationService 업로드 전 암호화 적용
- [ ] ConsultationService 다운로드 후 복호화 적용
- [ ] MinIO 버킷 정책 설정 (접근 제어)
- [ ] MinIO Lifecycle 정책 설정 (3년 보존)
- [ ] 암호화/복호화 단위 테스트
- [ ] E2E 테스트 (암호화된 파일 업로드/다운로드)

---

## 3. High Priority 이슈 해결 방안

### 3.1 NOTIFICATION_TBL 제거 리스크 🟡

#### 문제 분석

**현재 계획:** NOTIFICATION_TBL 완전 제거, FCM만 사용

**운영 리스크:**
1. **토큰 만료:** FCM 토큰 만료 시 재전송 불가 (기록 없음)
2. **전송 실패 추적:** 네트워크 오류, FCM 서버 장애 시 재시도 불가
3. **감사 요구:** "알림을 못 받았다"는 클레임 발생 시 증빙 불가
4. **사용자 알림 목록:** "내 알림 보기" 기능 제공 불가
5. **디버깅 어려움:** 알림 전송 이력이 없어 문제 추적 불가

**비즈니스 영향:**
- 상담 시스템 특성상 알림 누락은 치명적 (교수-학생 간 신뢰 문제)
- 대학 행정 시스템은 감사 추적(Audit Trail) 필수

#### 해결 방안

##### A) 경량 알림 로그 테이블 추가

**새 테이블:** `NOTIFICATION_LOG_TBL`

```sql
CREATE TABLE NOTIFICATION_LOG_TBL (
    log_idx BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '로그 고유 ID',

    -- 수신자 정보
    user_code VARCHAR(20) NOT NULL COMMENT '수신자 사용자 코드',

    -- 알림 메타데이터
    notification_type VARCHAR(50) NOT NULL COMMENT '알림 유형 (REQUEST_CREATED/REQUEST_APPROVED/NEW_MESSAGE/AUTO_CLOSE_WARNING)',
    reference_idx BIGINT COMMENT '참조 ID (request_idx)',

    -- 전송 정보
    sent_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '전송 시도 시각',
    status VARCHAR(20) NOT NULL COMMENT '전송 상태 (SUCCESS/FAILED/PENDING)',
    retry_count INT DEFAULT 0 COMMENT '재시도 횟수',

    -- 실패 정보
    error_message VARCHAR(500) COMMENT '실패 사유',

    -- 인덱스
    INDEX idx_user_code (user_code),
    INDEX idx_sent_at (sent_at),
    INDEX idx_status (status),
    INDEX idx_reference (reference_idx)

) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='알림 전송 로그 (경량)';
```

**특징:**
- ✅ 알림 **내용은 저장 안 함** (원본 데이터에서 재구성 가능)
- ✅ 발송 기록만 저장 (성공/실패, 재시도 횟수)
- ✅ 90일 자동 삭제 (보존 기간 제한)
- ✅ 감사 추적 가능, 디버깅 용이

##### B) 자동 삭제 스케줄러

```java
@Scheduled(cron = "0 0 4 * * *") // 매일 새벽 4시
public void cleanupOldNotificationLogs() {
    LocalDateTime cutoff = LocalDateTime.now().minusDays(90);

    int deleted = notificationLogRepository.deleteByCreatedAtBefore(cutoff);

    log.info("90일 이상 된 알림 로그 {}건 삭제 완료", deleted);
}
```

##### C) FCMService 수정

**전송 성공/실패 로그 기록:**

```java
@Service
@RequiredArgsConstructor
public class FCMService {

    private final NotificationLogRepository notificationLogRepository;

    public void sendConsultationNotification(
        String userCode,
        NotificationType type,
        Long referenceIdx
    ) {
        NotificationLog log = NotificationLog.builder()
            .userCode(userCode)
            .notificationType(type)
            .referenceIdx(referenceIdx)
            .sentAt(LocalDateTime.now())
            .status(NotificationStatus.PENDING)
            .retryCount(0)
            .build();

        try {
            // FCM 전송
            sendFcmMessage(userCode, type, referenceIdx);

            // 성공 기록
            log.setStatus(NotificationStatus.SUCCESS);

        } catch (Exception e) {
            // 실패 기록
            log.setStatus(NotificationStatus.FAILED);
            log.setErrorMessage(e.getMessage());

            log.error("FCM 전송 실패: userCode={}, type={}", userCode, type, e);
        } finally {
            // 로그 저장
            notificationLogRepository.save(log);
        }
    }
}
```

#### 구현 체크리스트

- [ ] NOTIFICATION_LOG_TBL 생성
- [ ] NotificationLog.java Entity 생성
- [ ] NotificationLogRepository.java 생성
- [ ] FCMService에 로그 기록 로직 추가
- [ ] 90일 자동 삭제 스케줄러 추가
- [ ] 재시도 로직 구현 (실패 시 3회까지 재시도)
- [ ] 관리자용 알림 로그 조회 API (선택)

---

### 3.2 백업 스케줄러 성능 문제 🟡

#### 문제 분석

**현재 설계:**

```java
@Scheduled(fixedRate = 300000) // 5분
public void backupActiveChats() {
    // 진행 중인 모든 상담 조회
    List<ConsultationRequest> activeConsultations =
        repository.findByConsultationStatus(ConsultationStatus.IN_PROGRESS);

    // 순차 처리
    for (ConsultationRequest consultation : activeConsultations) {
        List<ChatMessageDto> messages = chatService.getAllMessages(consultation.getRequestIdx());
        String snapshot = formatSnapshot(messages);
        minioService.uploadSnapshot(snapshot);
    }
}
```

**성능 병목:**
- 동시 상담 100개: 5분마다 100번 Redis LRANGE + 100번 MinIO 업로드
- 순차 처리: 한 건당 0.5초 → 100건 = 50초
- 규모 500개: 250초 (4분 이상) → 스케줄러 홀드
- 한 건 실패 시 전체 중단 가능

#### 해결 방안

##### A) 비동기 병렬 처리

```java
@Scheduled(fixedRate = 300000) // 5분
public void backupActiveChats() {
    List<ConsultationRequest> activeConsultations =
        repository.findByConsultationStatus(ConsultationStatus.IN_PROGRESS);

    log.info("백업 시작: 진행 중 상담 {}건", activeConsultations.size());

    // 병렬 처리 (최대 10개 동시)
    List<CompletableFuture<Void>> futures = activeConsultations.stream()
        .map(consultation -> CompletableFuture.runAsync(
            () -> backupSingleChat(consultation),
            backupExecutor // 별도 ThreadPool
        ))
        .collect(Collectors.toList());

    // 모든 백업 완료 대기 (타임아웃 3분)
    try {
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
            .get(3, TimeUnit.MINUTES);
        log.info("백업 완료");
    } catch (TimeoutException e) {
        log.error("백업 타임아웃", e);
    } catch (Exception e) {
        log.error("백업 실패", e);
    }
}

private void backupSingleChat(ConsultationRequest consultation) {
    try {
        Long requestIdx = consultation.getRequestIdx();

        // 1. 메시지 수집
        List<ChatMessageDto> messages = chatService.getAllMessages(requestIdx);

        // 2. 변경 여부 확인 (조건부 백업)
        if (hasChangedSinceLastBackup(requestIdx, messages.size())) {
            // 3. 스냅샷 생성
            String snapshot = formatSnapshot(messages, consultation);

            // 4. MinIO 업로드
            String objectName = String.format(
                "consultations/temp/chat_%d_snapshot_%s.enc",
                requestIdx,
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"))
            );

            // 5. 암호화 후 업로드
            String encrypted = encryptionService.encrypt(snapshot);
            minioService.uploadFile(objectName, encrypted);

            // 6. 마지막 백업 정보 갱신
            updateLastBackupInfo(requestIdx, messages.size());

            log.debug("백업 성공: requestIdx={}, messages={}", requestIdx, messages.size());
        }

    } catch (Exception e) {
        // 개별 실패 로그 (다른 백업은 계속 진행)
        log.error("개별 백업 실패: requestIdx={}",
            consultation.getRequestIdx(), e);
    }
}

@Bean
public Executor backupExecutor() {
    ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
    executor.setCorePoolSize(5);
    executor.setMaxPoolSize(10);
    executor.setQueueCapacity(100);
    executor.setThreadNamePrefix("backup-");
    executor.initialize();
    return executor;
}
```

##### B) 조건부 백업 (변경 감지)

**Redis에 마지막 백업 정보 저장:**

```java
private boolean hasChangedSinceLastBackup(Long requestIdx, int currentMessageCount) {
    String key = "backup:last:" + requestIdx;
    Integer lastCount = (Integer) redisTemplate.opsForValue().get(key);

    // 처음 백업이거나 메시지 수 증가 시 백업 필요
    return lastCount == null || lastCount < currentMessageCount;
}

private void updateLastBackupInfo(Long requestIdx, int messageCount) {
    String key = "backup:last:" + requestIdx;
    redisTemplate.opsForValue().set(key, messageCount, 7, TimeUnit.DAYS);
}
```

**효과:**
- 메시지 변경 없는 상담은 백업 스킵
- I/O 부하 대폭 감소

##### C) 성능 개선 효과

| 시나리오 | 기존 (순차) | 개선 (병렬+조건부) | 개선율 |
|---------|------------|-------------------|--------|
| **100개 상담** | 50초 | **5초** | 90% ⬇️ |
| **500개 상담** | 250초 | **25초** | 90% ⬇️ |
| **변경 없는 경우** | 50초 | **1초** | 98% ⬇️ |

#### 구현 체크리스트

- [ ] backupExecutor ThreadPool 설정
- [ ] CompletableFuture 병렬 처리 구현
- [ ] 조건부 백업 로직 추가 (Redis 변경 감지)
- [ ] 개별 실패 격리 (한 건 실패해도 계속 진행)
- [ ] 백업 타임아웃 설정 (3분)
- [ ] 성능 테스트 (100개 상담 백업 시간 측정)

---

### 3.3 통합 테스트 Phase 복원 🟡

#### 문제 분석

**현재 계획:** Phase 8 통합 테스트 완전 삭제 (-3시간)

**시스템 복잡도:**
- Redis (캐싱, 메시지 저장)
- MinIO (파일 저장, 백업)
- FCM (푸시 알림)
- 스케줄러 (자동 종료, 백업, 정리)
- WebSocket (실시간 통신)

**상호 의존성:**
```
메시지 전송 → Redis 저장 → 5분 백업 → MinIO temp
                ↓
상담 종료 → Redis 조회 → MinIO archive → Redis 삭제 → temp 삭제
                ↓
자동 종료 → Redis 조회 → MinIO 저장 → 알림 전송
                ↓
서버 크래시 → MinIO temp 조회 → archive 복구
```

**리스크:**
- 단위 테스트만으로는 통합 시나리오 검증 불가
- 스케줄러 타이밍 이슈 미검출
- Redis-MinIO 데이터 정합성 검증 부재
- 배포 후 프로덕션에서 버그 발견 → 롤백 비용, 신뢰 저하

#### 해결 방안

##### A) 핵심 E2E 시나리오만 선별 (1.5시간)

**Phase 8: 통합 테스트 (복원) - 1.5시간**

**테스트 시나리오:**

1. **정상 종료 E2E (30분)**
   ```java
   @Test
   @DisplayName("상담 생성 → 채팅 → 정상 종료 → MinIO 저장 확인")
   void testNormalConsultationFlow() {
       // 1. 상담 요청 생성
       Long requestIdx = createConsultationRequest();

       // 2. 교수 수락
       approveRequest(requestIdx);

       // 3. WebSocket 연결
       WebSocketSession studentSession = connectWebSocket(STUDENT_CODE);
       WebSocketSession professorSession = connectWebSocket(PROFESSOR_CODE);

       // 4. 메시지 송수신 (10개)
       for (int i = 0; i < 10; i++) {
           sendMessage(studentSession, requestIdx, "메시지 " + i);
           assertMessageReceived(professorSession);
       }

       // 5. Redis 저장 확인
       List<ChatMessageDto> messages = chatService.getAllMessages(requestIdx);
       assertThat(messages).hasSize(10);

       // 6. 상담 종료
       consultationService.endConsultation(requestIdx);

       // 7. MinIO 파일 생성 확인
       String objectName = "consultations/archive/chat_" + requestIdx + "_final.enc";
       assertTrue(minioService.fileExists(objectName));

       // 8. Redis 삭제 확인
       assertThat(chatService.getAllMessages(requestIdx)).isEmpty();

       // 9. 복호화 및 내용 확인
       String chatLog = consultationService.getHistoryDetail(requestIdx).getChatLog();
       assertThat(chatLog).contains("메시지 0");
   }
   ```

2. **자동 종료 E2E (30분)**
   ```java
   @Test
   @DisplayName("2시간 비활성 → 자동 종료 → MinIO 저장 확인")
   void testAutoCloseFlow() {
       // 1. 상담 시작
       Long requestIdx = startConsultation();

       // 2. 메시지 전송
       sendMessages(requestIdx, 5);

       // 3. 2시간 경과 시뮬레이션
       updateLastActivityAt(requestIdx, LocalDateTime.now().minusHours(2));

       // 4. 자동 종료 스케줄러 실행
       autoCloseScheduler.checkInactiveConsultations();

       // 5. 상담 상태 확인
       ConsultationRequest consultation = consultationRepository.findById(requestIdx).get();
       assertThat(consultation.getConsultationStatus()).isEqualTo(ConsultationStatus.COMPLETED);

       // 6. MinIO archive 저장 확인
       assertTrue(minioService.fileExists("consultations/archive/chat_" + requestIdx + "_final.enc"));

       // 7. Redis 삭제 확인
       assertThat(chatService.getAllMessages(requestIdx)).isEmpty();

       // 8. FCM 알림 전송 확인 (자동 종료 알림)
       verify(fcmService).sendConsultationNotification(
           any(), eq(NotificationType.AUTO_CLOSED), eq(requestIdx)
       );
   }
   ```

3. **크래시 복구 시뮬레이션 (30분)**
   ```java
   @Test
   @DisplayName("서버 크래시 → MinIO temp 복구 → archive 저장")
   void testCrashRecoveryFlow() {
       // 1. 상담 진행 중
       Long requestIdx = startConsultation();
       sendMessages(requestIdx, 20);

       // 2. 백업 스케줄러 실행 (MinIO temp 스냅샷)
       backupScheduler.backupActiveChats();

       // 3. Redis 강제 삭제 (크래시 시뮬레이션)
       redisTemplate.delete("chat:room:" + requestIdx);

       // 4. 복구 프로세스 실행
       recoveryService.recoverFromCrash();

       // 5. MinIO archive에 복구된 파일 확인
       assertTrue(minioService.fileExists("consultations/archive/chat_" + requestIdx + "_final.enc"));

       // 6. 상담 상태 COMPLETED 확인
       ConsultationRequest consultation = consultationRepository.findById(requestIdx).get();
       assertThat(consultation.getConsultationStatus()).isEqualTo(ConsultationStatus.COMPLETED);

       // 7. 복구된 메시지 개수 확인 (최대 5분 유실)
       String chatLog = consultationService.getHistoryDetail(requestIdx).getChatLog();
       assertThat(chatLog).contains("메시지");
   }
   ```

##### B) 테스트 인프라 설정

**TestContainers 활용:**

```java
@SpringBootTest
@Testcontainers
class ConsultationIntegrationTest {

    @Container
    static GenericContainer<?> redis = new GenericContainer<>("redis:7-alpine")
        .withExposedPorts(6379);

    @Container
    static GenericContainer<?> minio = new GenericContainer<>("minio/minio:latest")
        .withExposedPorts(9000)
        .withCommand("server /data");

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.redis.host", redis::getHost);
        registry.add("spring.redis.port", redis::getFirstMappedPort);
        registry.add("minio.endpoint", () -> "http://" + minio.getHost() + ":" + minio.getFirstMappedPort());
    }
}
```

#### 구현 체크리스트

- [ ] TestContainers 의존성 추가
- [ ] Redis + MinIO 테스트 컨테이너 설정
- [ ] 정상 종료 E2E 테스트 작성
- [ ] 자동 종료 E2E 테스트 작성
- [ ] 크래시 복구 시뮬레이션 테스트 작성
- [ ] CI/CD 파이프라인에 통합 테스트 추가

---

## 4. Medium Priority 이슈

### 4.1 WebSocket 브로커 스케일아웃 제약 🟢

#### 문제 분석

**현재 설계:** Spring SimpleBroker (인메모리)

**스케일아웃 시 문제:**
```
Instance A: 학생 WebSocket 연결
Instance B: 교수 WebSocket 연결

학생 → Instance A → SimpleBroker A (메모리)
교수 ← Instance B ← SimpleBroker B (메모리) ❌ 메시지 미수신
```

#### 처리 방안

##### A) 현재 단계: 문서화 + 추후 개선

**알려진 제약사항 문서 추가:**

```markdown
## 알려진 제약사항 (Known Limitations)

### 1. WebSocket 스케일아웃 미지원

**현재 구현:**
- Spring SimpleBroker (인메모리 브로커) 사용
- 단일 인스턴스 환경에서만 정상 동작

**제약사항:**
- 다중 인스턴스 배포 시 메시지 유실 가능
- 로드 밸런서 사용 불가 (세션 분산 문제)

**해결 방안 (추후):**
- Redis Pub/Sub 또는 RabbitMQ 브로커 전환
- STOMP Relay 프로토콜 적용

**현재 권장 배포 방식:**
- 단일 인스턴스 수직 확장 (Vertical Scaling)
- 동시 접속자 1,000명까지 지원 (예상)

**추후 개선 시점:**
- Phase 2 (동시 접속자 1,000명 초과 시)
```

##### B) 추후 개선 계획 (Phase 2)

**RabbitMQ 브로커 전환:**

```java
@Override
public void configureMessageBroker(MessageBrokerRegistry config) {
    // SimpleBroker 제거
    // config.enableSimpleBroker("/queue", "/topic");

    // RabbitMQ Relay 사용
    config.enableStompBrokerRelay("/queue", "/topic")
        .setRelayHost("localhost")
        .setRelayPort(61613)
        .setClientLogin("guest")
        .setClientPasscode("guest");
}
```

**예상 작업 시간:** 4시간 (RabbitMQ 설정 + 테스트)

#### 구현 체크리스트

- [ ] implementation-plan-v2.md에 "알려진 제약사항" 섹션 추가
- [ ] README.md에 배포 가이드 추가 (단일 인스턴스 권장)
- [ ] Phase 2 개선 백로그에 "RabbitMQ 전환" 항목 등록

---

## 5. 구현 계획 업데이트

### 5.1 작업 시간 재산정

| Phase | v2.0 (원본) | v2.1 (수정) | 변경 사유 |
|-------|-------------|-------------|----------|
| Phase 1: 기반 구축 | 5시간 | **6시간** | +1h (EncryptionService, NotificationLog 추가) |
| Phase 2: 상담 요청 | 4시간 | **4시간** | - |
| Phase 3: WebSocket & Redis | 7시간 | **7.5시간** | +0.5h (Redis TTL 갱신 로직) |
| Phase 4: MinIO 통합 | 3시간 | **4시간** | +1h (암호화/복호화 적용) |
| Phase 5: 알림 | 2시간 | **2.5시간** | +0.5h (NotificationLog 기록) |
| Phase 6: 자동 종료 + 백업 | 4시간 | **4.5시간** | +0.5h (병렬 처리, 조건부 백업) |
| Phase 7: 이력 | 2시간 | **2시간** | - |
| Phase 8: 통합 테스트 | ~~제거~~ | **1.5시간** | +1.5h (핵심 E2E 복원) |
| **총합** | **26시간** | **29.5시간** | **+3.5시간** |

### 5.2 파일 추가

| 파일 | 위치 | 목적 |
|------|------|------|
| **EncryptionService.java** | service/ | AES-256-GCM 암호화/복호화 |
| **NotificationLog.java** | entity/ | 알림 로그 Entity |
| **NotificationLogRepository.java** | repository/ | 알림 로그 Repository |

**파일 수:** 24개 → **27개** (+3개)

### 5.3 테이블 추가

| 테이블 | 목적 | 보존 기간 |
|--------|------|----------|
| **NOTIFICATION_LOG_TBL** | 알림 전송 로그 (경량) | 90일 |

**테이블 수:** 1개 → **2개** (+1개)

### 5.4 최종 비교

| 항목 | v2.0 (원본) | v2.1 (수정) | 효과 |
|------|-------------|-------------|------|
| **구현 시간** | 26시간 | **29.5시간** | v1.0 대비 여전히 -20% |
| **파일 수** | 24개 | **27개** | v1.0 대비 -18% |
| **테이블 수** | 1개 | **2개** | v1.0 대비 -50% |
| **안정성** | 중간 | **높음** | ⬆️⬆️ |
| **보안** | 낮음 | **높음** | ⬆️⬆️⬆️ |
| **운영 리스크** | 높음 | **낮음** | ⬇️⬇️ |

---

## 6. 추가 확인 필요 사항

### 6.1 제품·운영 측 확인 필요

#### A) 알림 관련

**질문:**
1. 사용자 "내 알림 목록" 기능이 필요한가요?
   - 현재: FCM 푸시만 제공 (휘발성)
   - 대안: NOTIFICATION_LOG_TBL 기반 알림 목록 제공 가능

2. 알림 전송 실패 시 재시도 정책은?
   - 현재 계획: 3회까지 재시도
   - 확인 필요: 재시도 간격, 최종 실패 시 처리 방법

3. 알림 로그 보존 기간은 90일이 적절한가요?
   - 감사 요구사항에 따라 조정 가능

#### B) 데이터 보존 관련

**질문:**
1. 상담 로그 보존 기간은 얼마나 되나요?
   - 현재 계획: 3년 (일반적인 대학 행정 기준)
   - 확인 필요: 학칙 또는 내부 규정

2. 보존 기간 경과 후 처리 방법은?
   - 자동 삭제 vs 아카이빙(압축 저장)

#### C) 암호화 키 관리

**질문:**
1. 암호화 키 관리 방식은?
   - 현재 계획: application.yml 환경 변수
   - 권장: AWS KMS, HashiCorp Vault 등 전문 키 관리 서비스
   - 예산 및 인프라 여건 확인 필요

2. 키 순환(Key Rotation) 정책은?
   - 정기적인 키 교체 필요 여부
   - 구현 시 추가 작업 필요

### 6.2 인프라 담당자 확인 필요

#### A) MinIO 설정

**확인 사항:**
1. MinIO 버킷 정책 설정 권한이 있나요?
2. Object Lifecycle 정책 설정 가능한가요?
3. Server-Side Encryption 사용 가능한가요?

#### B) Redis 설정

**확인 사항:**
1. Redis AOF 활성화 가능한가요?
   - 성능 영향 (Disk I/O 증가) 수용 가능 여부
2. Redis 재시작 시 복구 테스트 진행 가능한가요?

#### C) RabbitMQ (선택)

**확인 사항:**
1. 추후 RabbitMQ 도입 가능한가요? (Phase 2)
2. 운영 및 모니터링 역량이 있나요?

---

## 7. 다음 단계

### 7.1 즉시 조치 (Critical)

1. **Redis TTL 갱신 로직 구현** (30분)
   - ChatService.sendMessage()에 EXPIRE 추가
   - 48h → 168h 변경

2. **EncryptionService 구현** (2시간)
   - AES-256-GCM 암호화/복호화
   - 단위 테스트 작성

3. **MinIO 암호화 적용** (1시간)
   - 업로드 전 암호화
   - 다운로드 후 복호화

### 7.2 단기 조치 (High)

4. **NOTIFICATION_LOG_TBL 추가** (1.5시간)
   - 테이블 생성
   - Entity/Repository 구현
   - FCMService 수정

5. **백업 스케줄러 최적화** (1.5시간)
   - 비동기 병렬 처리
   - 조건부 백업

6. **핵심 E2E 테스트 작성** (1.5시간)
   - 정상 종료, 자동 종료, 크래시 복구

### 7.3 문서화 (Medium)

7. **알려진 제약사항 문서화** (30분)
   - WebSocket 스케일아웃 제약
   - 단일 인스턴스 배포 가이드

8. **보안 가이드 작성** (30분)
   - 암호화 키 관리 방법
   - MinIO 버킷 정책 설정 예시

---

**작성자:** Claude AI
**버전:** 1.0
**작성일:** 2025-10-23
**다음 리뷰:** Critical 이슈 구현 완료 후
