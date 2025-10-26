# Phase 5: WebSocket 실시간 채팅 시스템 구현

**작성일**: 2025-10-24  
**버전**: 1.0.0  
**상태**: Stage 1 & 2 완료

---

## 📋 목차

1. [개요](#개요)
2. [Stage 1: 기본 채팅 시스템](#stage-1-기본-채팅-시스템)
3. [Stage 2: MinIO 아카이빙 통합](#stage-2-minio-아카이빙-통합)
4. [데이터 플로우](#데이터-플로우)
5. [API 문서](#api-문서)
6. [테스트 가이드](#테스트-가이드)
7. [다음 단계](#다음-단계)

---

## 개요

### 목표
상담 관리 시스템에 실시간 채팅 기능을 추가하여 학생과 교수 간 1:1 상담을 원활하게 진행할 수 있도록 구현합니다.

### 주요 기능
- ✅ WebSocket 기반 실시간 양방향 통신
- ✅ Redis를 활용한 임시 메시지 저장 (TTL 36시간)
- ✅ 상담 종료 시 MinIO 자동 아카이빙
- ✅ JWT 기반 인증 및 권한 검증
- ✅ Personal Queue 패턴으로 1:1 메시징

### 기술 스택
| 기술 | 목적 | 버전 |
|-----|------|-----|
| WebSocket (STOMP) | 실시간 통신 | spring-boot-starter-websocket |
| Redis | 임시 메시지 저장 | StringRedisTemplate |
| MinIO | 영구 채팅 로그 아카이빙 | io.minio:minio |
| JWT | 인증 및 권한 관리 | 기존 JwtUtil |
| Spring Boot | 프레임워크 | 2.7.x |

---

## Stage 1: 기본 채팅 시스템

### 1.1 Redis 설정

#### RedisConfig.java 수정
```java
@Bean
public StringRedisTemplate stringRedisTemplate(RedisConnectionFactory connectionFactory) {
    return new StringRedisTemplate(connectionFactory);
}
```

**목적**: 채팅 메시지를 JSON 문자열로 저장하기 위한 템플릿 추가

---

### 1.2 WebSocket 설정

#### WebSocketConfig.java (신규 생성)
**경로**: `src/main/java/BlueCrab/com/example/config/WebSocketConfig.java`

```java
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {
    
    private final JwtUtil jwtUtil;
    
    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.enableSimpleBroker("/queue", "/topic");
        registry.setApplicationDestinationPrefixes("/app");
        registry.setUserDestinationPrefix("/user");
    }
    
    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws/chat")
                .setAllowedOriginPatterns("*")
                .withSockJS();
    }
    
    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(new ChannelInterceptor() {
            @Override
            public Message<?> preSend(Message<?> message, MessageChannel channel) {
                // JWT 인증 로직
            }
        });
    }
}
```

**주요 특징**:
- STOMP 엔드포인트: `/ws/chat`
- Personal Queue: `/user/{userCode}/queue/chat`
- JWT 토큰 검증을 CONNECT 시점에 수행
- SockJS fallback 지원

---

### 1.3 채팅 서비스

#### ChatService.java (신규 생성)
**경로**: `src/main/java/BlueCrab/com/example/service/ChatService.java`

```java
public interface ChatService {
    void saveMessage(Long requestIdx, ChatMessageDto message);
    List<ChatMessageDto> getMessages(Long requestIdx, int count);
    List<ChatMessageDto> getAllMessages(Long requestIdx);
    long getMessageCount(Long requestIdx);
    void setTTL(Long requestIdx);
    boolean isParticipant(Long requestIdx, String userCode);
    void deleteMessages(Long requestIdx);
    String formatChatLog(Long requestIdx);
}
```

#### ChatServiceImpl.java (신규 생성)
**경로**: `src/main/java/BlueCrab/com/example/service/impl/ChatServiceImpl.java`

**Redis 키 패턴**: `chat:room:{requestIdx}`

**주요 메서드**:

1. **saveMessage()**: 메시지 저장
   - JSON 직렬화 (ObjectMapper)
   - Redis RPUSH 명령
   - TTL 36시간 갱신

2. **getMessages()**: 최근 N개 메시지 조회
   - Redis LRANGE 명령
   - JSON 역직렬화

3. **getAllMessages()**: 전체 메시지 조회
   - LRANGE 0 -1
   - 아카이빙 시 사용

4. **isParticipant()**: 참여자 확인
   - requester_user_code 또는 recipient_user_code 검증
   - 권한 체크에 사용

5. **formatChatLog()**: 텍스트 파일 포맷팅 (Stage 2에서 추가)

6. **deleteMessages()**: Redis 메시지 삭제 (Stage 2에서 추가)

---

### 1.4 WebSocket 메시지 핸들러

#### ChatController.java (신규 생성)
**경로**: `src/main/java/BlueCrab/com/example/controller/ChatController.java`

```java
@Controller
public class ChatController {
    
    @MessageMapping("/chat.send")
    public void sendMessage(
        @Payload ChatMessageDto message,
        SimpMessageHeaderAccessor headerAccessor
    ) {
        String senderUserCode = (String) headerAccessor.getSessionAttributes().get("userCode");
        
        // 1. 참여자 검증
        if (!chatService.isParticipant(message.getRequestIdx(), senderUserCode)) {
            throw new IllegalArgumentException("채팅 참여자가 아닙니다.");
        }
        
        // 2. 메시지 저장
        message.setSender(senderUserCode);
        message.setSentAt(LocalDateTime.now());
        chatService.saveMessage(message.getRequestIdx(), message);
        
        // 3. 상담 자동 시작
        markStartedIfNeeded(message.getRequestIdx());
        
        // 4. 상대방에게 전송 (Personal Queue)
        String recipientUserCode = getRecipientUserCode(message.getRequestIdx(), senderUserCode);
        messagingTemplate.convertAndSendToUser(
            recipientUserCode,
            "/queue/chat",
            message
        );
        
        // 5. 본인에게도 전송 (멀티 디바이스 지원)
        messagingTemplate.convertAndSendToUser(
            senderUserCode,
            "/queue/chat",
            message
        );
        
        // 6. 마지막 활동 시간 업데이트
        updateLastActivityTime(message.getRequestIdx());
    }
}
```

**메시지 플로우**:
```
Client → /app/chat.send → Server Processing → /user/{userCode}/queue/chat
```

**자동 상담 시작**:
- 첫 메시지 발송 시 `started_at`이 null이면 자동으로 IN_PROGRESS 상태로 변경
- SCHEDULED → IN_PROGRESS 자동 전환

---

### 1.5 의존성 추가

#### pom.xml
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-websocket</artifactId>
</dependency>
```

---

## Stage 2: MinIO 아카이빙 통합

### 2.1 목표
상담 종료 시 Redis에 저장된 채팅 메시지를 MinIO에 영구 보관하고, Redis에서는 삭제하여 메모리 최적화

### 2.2 MinIO 서비스 확장

#### MinIOService.java 수정
**경로**: `src/main/java/BlueCrab/com/example/service/MinIOService.java`

**추가된 메서드**:

```java
/**
 * 채팅 로그 파일을 MinIO에 업로드
 */
public void uploadChatLog(String bucketName, String objectName, 
                          InputStream inputStream, long size) throws Exception {
    PutObjectArgs putObjectArgs = PutObjectArgs.builder()
            .bucket(bucketName)
            .object(objectName)
            .stream(inputStream, size, -1)
            .contentType("text/plain; charset=utf-8")
            .build();
    
    ObjectWriteResponse response = minioClient.putObject(putObjectArgs);
    logger.info("채팅 로그 업로드 성공 - Object: {}, ETag: {}", 
                objectName, response.etag());
}

/**
 * MinIO에서 채팅 로그 파일 다운로드
 */
public InputStream downloadChatLog(String bucketName, String objectName) throws Exception {
    return minioClient.getObject(
        GetObjectArgs.builder()
            .bucket(bucketName)
            .object(objectName)
            .build()
    );
}
```

**버킷 구조**:
```
consultation-chats/
├── archive/
│   ├── chat_123_final.txt
│   └── chat_{requestIdx}_final.txt
└── temp/
    ├── chat_123_snapshot_20251024140530.txt
    └── chat_{requestIdx}_snapshot_{yyyyMMddHHmmss}.txt
```

---

### 2.3 ChatService 확장

#### formatChatLog() 메서드 추가

```java
@Override
public String formatChatLog(Long requestIdx) {
    List<ChatMessageDto> messages = getAllMessages(requestIdx);
    
    if (messages.isEmpty()) {
        return "채팅 내역이 없습니다.";
    }
    
    StringBuilder sb = new StringBuilder();
    sb.append("==================================================\n");
    sb.append("       상담 채팅 기록\n");
    sb.append("==================================================\n\n");
    sb.append("상담 요청 번호: ").append(requestIdx).append("\n");
    sb.append("생성 일시: ").append(LocalDateTime.now()
        .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))).append("\n");
    sb.append("총 메시지 수: ").append(messages.size()).append("\n");
    sb.append("\n==================================================\n\n");
    
    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    
    for (ChatMessageDto msg : messages) {
        sb.append("[").append(msg.getSentAt().format(formatter)).append("]\n");
        sb.append("발신: ").append(msg.getSenderName())
          .append(" (").append(msg.getSender()).append(")\n");
        sb.append("내용: ").append(msg.getContent()).append("\n");
        sb.append("--------------------------------------------------\n");
    }
    
    sb.append("\n==================================================\n");
    sb.append("채팅 기록 끝\n");
    sb.append("==================================================\n");
    
    return sb.toString();
}
```

**출력 예시**:
```
==================================================
       상담 채팅 기록
==================================================

상담 요청 번호: 123
생성 일시: 2025-10-24 14:30:22
총 메시지 수: 15

==================================================

[2025-10-24 14:15:30]
발신: 홍길동 (STU001)
내용: 교수님 안녕하세요, 수업 관련 질문이 있습니다.
--------------------------------------------------
[2025-10-24 14:16:05]
발신: 김교수 (PROF001)
내용: 네, 무엇이 궁금하신가요?
--------------------------------------------------
...
```

---

### 2.4 상담 종료 플로우 수정

#### ConsultationRequestServiceImpl.java 수정

```java
@Override
public ConsultationRequestDto endConsultation(ConsultationIdDto idDto) {
    try {
        ConsultationRequest consultation = consultationRepository
            .findById(idDto.getRequestIdx())
            .orElseThrow(() -> new NoSuchElementException("상담 요청을 찾을 수 없습니다."));
        
        // 상태 검증
        if (!ConsultationStatus.IN_PROGRESS.equals(consultation.getConsultationStatusEnum())) {
            throw new IllegalStateException("진행 중인 상담만 종료할 수 있습니다.");
        }
        
        // ========== 1. Redis → MinIO 아카이빙 ==========
        archiveChatLog(consultation.getRequestIdx());
        
        // ========== 2. 상담 종료 처리 ==========
        LocalDateTime now = LocalDateTime.now();
        consultation.setConsultationStatusEnum(ConsultationStatus.COMPLETED);
        consultation.setEndedAt(now);
        
        // 상담 시간 계산 (분 단위)
        if (consultation.getStartedAt() != null) {
            Duration duration = Duration.between(consultation.getStartedAt(), now);
            consultation.setDurationMinutes((int) duration.toMinutes());
        }
        
        ConsultationRequest saved = consultationRepository.save(consultation);
        
        log.info("상담 종료 완료: requestIdx={}, duration={}분",
                 saved.getRequestIdx(), saved.getDurationMinutes());
        
        return toDto(saved);
        
    } catch (Exception e) {
        log.error("상담 종료 실패: {}", e.getMessage(), e);
        throw new RuntimeException("상담 종료에 실패했습니다: " + e.getMessage(), e);
    }
}
```

**종료 시퀀스**:
```
1. 상태 검증 (IN_PROGRESS 확인)
2. Redis 메시지 개수 확인
3. formatChatLog() 호출
4. MinIO 업로드 (consultation-chats 버킷)
5. Redis 메시지 삭제
6. 상담 상태 → COMPLETED
7. ended_at, duration_minutes 계산
8. DB 저장
```

---

### 2.5 REST API 추가

#### ChatRestController.java (신규 생성)
**경로**: `src/main/java/BlueCrab/com/example/controller/ChatRestController.java`

#### 2.5.1 채팅 메시지 조회

**Endpoint**: `GET /api/chat/messages/{requestIdx}`

**Request**:
```http
GET /api/chat/messages/123
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
```

**Response**:
```json
{
  "requestIdx": 123,
  "messageCount": 15,
  "messages": [
    {
      "requestIdx": 123,
      "sender": "STU001",
      "senderName": "홍길동",
      "content": "안녕하세요",
      "sentAt": "2025-10-24 14:15:30"
    },
    ...
  ]
}
```

**특징**:
- JWT 인증 필수
- isParticipant() 검증
- Redis에서 실시간 조회

---

#### 2.5.2 채팅 히스토리 다운로드 (현재)

**Endpoint**: `POST /api/chat/history/download/{requestIdx}`

**Request**:
```http
POST /api/chat/history/download/123
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
```

**Response**:
```
Content-Type: text/plain; charset=utf-8
Content-Disposition: attachment; filename="chat-log-123.txt"

==================================================
       상담 채팅 기록
==================================================
...
```

**사용 사례**:
- 상담 진행 중 중간 백업
- 관리자 모니터링
- 즉시 다운로드 필요 시

---

#### 2.5.3 아카이빙된 채팅 로그 다운로드

**Endpoint**: `GET /api/chat/archive/download/{requestIdx}`

**Request**:
```http
GET /api/chat/archive/download/123
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
```

**Response**:
```
Content-Type: text/plain; charset=utf-8
Content-Disposition: attachment; filename="archived-chat-log-123.txt"

(MinIO에 저장된 파일 내용)
```

**사용 사례**:
- 상담 종료 후 히스토리 조회
- 36시간 TTL 경과 후 조회
- MinIO 영구 저장소에서 검색

---

## 데이터 플로우

### 전체 시스템 아키텍처

```
┌─────────────┐         WebSocket          ┌──────────────┐
│   Client    │◄─────────────────────────►│ ChatController│
│  (Browser)  │      STOMP over SockJS     │   (Spring)   │
└─────────────┘                             └──────────────┘
                                                    │
                                                    ▼
                                            ┌──────────────┐
                                            │ ChatService  │
                                            │    (Impl)    │
                                            └──────────────┘
                                                    │
                        ┌──────────────────────────┼──────────────────────────┐
                        ▼                          ▼                          ▼
                ┌──────────────┐          ┌──────────────┐          ┌──────────────┐
                │    Redis     │          │   MariaDB    │          │    MinIO     │
                │ (Chat Cache) │          │  (Metadata)  │          │  (Archive)   │
                └──────────────┘          └──────────────┘          └──────────────┘
                  TTL 36 hours              Consultation             consultation-
                  chat:room:{id}            Request Table            chats bucket
```

### 메시지 송수신 플로우

```
[송신]
Client
  │ SEND /app/chat.send
  ▼
ChatController
  │ 1. JWT 검증
  │ 2. isParticipant() 확인
  │ 3. saveMessage() → Redis
  │ 4. markStartedIfNeeded()
  │ 5. convertAndSendToUser()
  ▼
Recipient Client
  │ SUBSCRIBE /user/{userCode}/queue/chat
  └─► Message Received

[조회]
Client
  │ GET /api/chat/messages/{id}
  ▼
ChatRestController
  │ JWT 검증
  ▼
ChatService.getAllMessages()
  │ Redis LRANGE
  └─► JSON Array
```

### 상담 종료 플로우

```
[종료 요청]
Client → POST /api/consultations/end
  ▼
ConsultationRequestServiceImpl.endConsultation()
  │
  ├─► 1. 상태 검증 (IN_PROGRESS?)
  │
  ├─► 2. chatService.getMessageCount()
  │        │
  │        ├─► messageCount > 0?
  │        │   ├─ YES ─► formatChatLog()
  │        │   │              │
  │        │   │              ├─► Redis LRANGE 0 -1
  │        │   │              │
  │        │   │              ├─► StringBuilder 포맷팅
  │        │   │              │
  │        │   │              └─► String chatLog
  │        │   │
  │        │   ├─► minIOService.uploadChatLog()
  │        │   │        │
  │        │   │        ├─► ByteArrayInputStream
  │        │   │        │
  │        │   │        ├─► MinIO PUT (archive/chat_{id}_final.txt)
  │        │   │        │
  │        │   │        └─► consultation-chats/archive/...
  │        │   │
  │        │   ├─► chatService.deleteMessages()
  │        │   │        │
  │        │   │        └─► Redis DEL chat:room:{id}
  │        │   │
  │        │   └─► clearTempSnapshots()
  │        │
  │        └─ NO ─► Skip archiving
  │
  ├─► 3. consultation.setConsultationStatusEnum(COMPLETED)
  │
  ├─► 4. consultation.setEndedAt(now)
  │
  ├─► 5. Calculate duration_minutes
  │
  └─► 6. consultationRepository.save()

[결과]
✓ Redis 메시지 삭제 (메모리 확보)
✓ MinIO 영구 저장 (archive/chat_{id}_final.txt)
✓ MinIO temp 스냅샷 정리
✓ DB 상태 업데이트
```

---

## API 문서

### WebSocket API

#### CONNECT
```javascript
const socket = new SockJS('/ws/chat');
const stompClient = Stomp.over(socket);

stompClient.connect({
    'Authorization': 'Bearer ' + jwtToken
}, function(frame) {
    console.log('Connected: ' + frame);
    
    // Personal Queue 구독
    stompClient.subscribe('/user/queue/chat', function(message) {
        const chatMessage = JSON.parse(message.body);
        displayMessage(chatMessage);
    });
});
```

#### SEND MESSAGE
```javascript
stompClient.send("/app/chat.send", {}, JSON.stringify({
    'requestIdx': 123,
    'content': '안녕하세요'
}));
```

**서버 자동 처리**:
- `sender`: JWT에서 추출
- `senderName`: DB 조회
- `sentAt`: 서버 타임스탬프

---

### REST API

#### 1. 채팅 메시지 조회

```http
GET /api/chat/messages/{requestIdx}
Authorization: Bearer {token}
```

**Response 200 OK**:
```json
{
  "requestIdx": 123,
  "messageCount": 5,
  "messages": [
    {
      "requestIdx": 123,
      "sender": "STU001",
      "senderName": "홍길동",
      "content": "안녕하세요",
      "sentAt": "2025-10-24 14:15:30"
    }
  ]
}
```

**Response 403 Forbidden**:
```json
{
  "error": "채팅 참여자만 조회할 수 있습니다."
}
```

---

#### 2. 채팅 히스토리 다운로드

```http
POST /api/chat/history/download/{requestIdx}
Authorization: Bearer {token}
```

**Response 200 OK**:
```
Content-Type: text/plain; charset=utf-8
Content-Disposition: attachment; filename="chat-log-123.txt"

(텍스트 파일 내용)
```

---

#### 3. 아카이빙된 로그 다운로드

```http
GET /api/chat/archive/download/{requestIdx}
Authorization: Bearer {token}
```

**Response 200 OK**: 파일 다운로드

**Response 404 Not Found**:
```json
{
  "error": "아카이빙된 채팅 로그를 찾을 수 없습니다."
}
```

---

## 테스트 가이드

### 1. WebSocket 연결 테스트

```javascript
// 1. JWT 토큰 준비
const token = localStorage.getItem('jwtToken');

// 2. SockJS 연결
const socket = new SockJS('http://localhost:8080/ws/chat');
const stompClient = Stomp.over(socket);

// 3. 연결 시도
stompClient.connect({
    'Authorization': 'Bearer ' + token
}, function(frame) {
    console.log('✅ WebSocket 연결 성공');
    
    // 4. 구독 설정
    stompClient.subscribe('/user/queue/chat', function(message) {
        console.log('📩 메시지 수신:', JSON.parse(message.body));
    });
    
    // 5. 메시지 전송
    stompClient.send("/app/chat.send", {}, JSON.stringify({
        requestIdx: 123,
        content: 'Test message'
    }));
}, function(error) {
    console.error('❌ WebSocket 연결 실패:', error);
});
```

---

### 2. 메시지 저장 테스트 (Redis)

```bash
# Redis CLI 접속
redis-cli

# 채팅 키 확인
KEYS chat:room:*

# 특정 채팅 메시지 조회
LRANGE chat:room:123 0 -1

# TTL 확인 (36시간 = 129600초)
TTL chat:room:123
```

**예상 결과**:
```
127.0.0.1:6379> KEYS chat:room:*
1) "chat:room:123"

127.0.0.1:6379> LRANGE chat:room:123 0 -1
1) "{\"requestIdx\":123,\"sender\":\"STU001\",\"senderName\":\"홍길동\",..."

127.0.0.1:6379> TTL chat:room:123
(integer) 129580
```

---

### 3. 상담 종료 및 아카이빙 테스트

#### 3.1 상담 종료 요청

```http
POST /api/consultations/end
Content-Type: application/json
Authorization: Bearer {token}

{
  "requestIdx": 123
}
```

#### 3.2 MinIO 파일 확인

```bash
# MinIO Client (mc) 설치 후
mc ls minio/consultation-chats/

# 예상 출력
[2025-10-24 14:30:22 KST]  15KiB archive/chat_123_final.txt   # temp/ 스냅샷은 정리되어 목록에 표시되지 않음
```

#### 3.3 Redis 삭제 확인

```bash
redis-cli

EXISTS chat:room:123
# (integer) 0  ← 삭제됨
```

#### 3.4 로그 다운로드 테스트

```http
GET /api/chat/archive/download/123
Authorization: Bearer {token}
```

---

### 4. 통합 테스트 시나리오

#### 시나리오 1: 정상 채팅 플로우

```
1. [학생] 상담 요청 생성 → PENDING
2. [교수] 상담 승인 → SCHEDULED
3. [학생] 첫 메시지 전송 → IN_PROGRESS (자동 시작)
4. [교수] 응답 메시지 전송
5. [학생/교수] 10회 메시지 교환
6. [교수] 상담 종료
   ✓ Redis 메시지 확인 (10개)
   ✓ MinIO 파일 생성
   ✓ Redis 메시지 삭제
   ✓ DB 상태 COMPLETED
7. [학생] 아카이빙된 로그 다운로드
```

#### 시나리오 2: 메시지 없는 상담 종료

```
1. 상담 요청 → 승인 → SCHEDULED
2. 메시지 전송 없이 종료
   ✓ Redis 키 없음
   ✓ MinIO 파일 생성 안 됨
   ✓ DB 상태만 COMPLETED
```

#### 시나리오 3: JWT 인증 실패

```
1. 잘못된 토큰으로 WebSocket 연결 시도
   ❌ CONNECT 실패
2. 만료된 토큰으로 REST API 호출
   ❌ 401 Unauthorized
```

---

### 5. 성능 테스트

#### 메시지 처리 성능

```bash
# JMeter 또는 Artillery로 부하 테스트

# 시나리오
- 동시 접속: 100명
- 메시지 전송: 초당 50개
- 테스트 시간: 5분

# 측정 항목
- WebSocket 연결 성공률
- 메시지 전송 지연시간 (RTT)
- Redis 메모리 사용량
- CPU/메모리 사용률
```

---

## 다음 단계

### Stage 3: 알림 및 확장 기능 (예정)

#### 3.1 FCM Push 알림
- [x] 새 메시지 알림
- [x] 상담 시작/종료 알림
- [x] 상담 요청/승인/반려/취소 알림
- [x] 배치 알림 (N개 메시지 묶음)

#### 3.2 읽음 확인 (Read Receipts)
- [x] 메시지 읽음 상태 추적
- [x] 읽음 시간 기록
- [ ] UI 표시 (WhatsApp 스타일)

#### 3.3 스케줄러 완성
- [x] ChatBackupScheduler 구현 (5분마다 임시 백업)
- [x] OrphanedRoomCleanupScheduler 구현 (60시간 이상 방 청소)

#### 3.4 고급 기능
- [ ] 파일 첨부 (이미지, 문서)
- [ ] 메시지 검색
- [ ] 채팅 내보내기 (PDF, Excel)
- [ ] 통계 대시보드

---

## 부록

### A. Redis 키 구조

| 키 패턴 | 타입 | TTL | 용도 |
|--------|------|-----|------|
| `chat:room:{id}` | List | 36h | 채팅 메시지 저장 |

### B. MinIO 버킷 구조

```
consultation-chats/
├── archive/
│   └── chat_{requestIdx}_final.txt      # 상담 종료 시 생성
└── temp/
    └── chat_{requestIdx}_snapshot_{yyyyMMddHHmmss}.txt
```

### C. DB 스키마 변경 (필요 시)

```sql
-- 옵션: 채팅 로그 경로 저장
ALTER TABLE CONSULTATION_REQUEST 
ADD COLUMN chat_log_url VARCHAR(500) COMMENT 'MinIO 채팅 로그 경로';
```

### D. 설정 파일

#### application.properties
```properties
# WebSocket
spring.websocket.allowed-origins=*

# Redis (기존)
spring.redis.host=localhost
spring.redis.port=6379

# MinIO (기존)
app.minio.endpoint=http://localhost:9000
app.minio.access-key=minioadmin
app.minio.secret-key=minioadmin
app.minio.bucket-name=blue-crab-lms

# 채팅 설정
app.chat.redis-ttl-hours=36
app.chat.minio-bucket=consultation-chats
```

---

## 문서 이력

| 버전 | 날짜 | 작성자 | 변경 내역 |
|-----|------|--------|----------|
| 1.0.0 | 2025-10-24 | BlueCrab Team | Stage 1 & 2 구현 완료 문서 작성 |

---

## 참고 자료

- [Spring WebSocket Documentation](https://docs.spring.io/spring-framework/docs/current/reference/html/web.html#websocket)
- [STOMP Protocol](https://stomp.github.io/)
- [Redis Lists](https://redis.io/docs/data-types/lists/)
- [MinIO Java SDK](https://min.io/docs/minio/linux/developers/java/API.html)

---

**문서 끝**
