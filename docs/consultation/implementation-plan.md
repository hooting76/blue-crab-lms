# 상담 시스템 구현 계획서

**프로젝트:** Blue Crab LMS - 실시간 텍스트 상담 시스템
**작성일:** 2025-10-22
**수정일:** 2025-10-22 (v2.0 - 아키텍처 최적화)
**예상 작업 기간:** 28시간 (약 3.5일, 8시간/일 기준)
**목표:** feature-list.md의 상담 시스템 요구사항 완전 구현 (최적화된 아키텍처)

---

## 📋 목차

1. [개요](#1-개요)
2. [아키텍처 설계](#2-아키텍처-설계)
3. [데이터베이스 스키마](#3-데이터베이스-스키마)
4. [디렉토리 구조](#4-디렉토리-구조)
5. [API 엔드포인트](#5-api-엔드포인트)
6. [구현 순서](#6-구현-순서)
7. [보안 고려사항](#7-보안-고려사항)
8. [작업 체크리스트](#8-작업-체크리스트)

---

## 1. 개요

### 1.1 목표
Blue Crab LMS에 실시간 텍스트 상담 시스템을 추가하여 학생과 교수 간 1:1 상담을 지원합니다.

### 1.2 핵심 기능
- ✅ 상담 요청 생성/관리 (학생 → 교수)
- ✅ 상담 요청 수락/거절 (교수) - 반려 사유에 가능 시간 작성
- ✅ 실시간 채팅 (WebSocket + Redis)
- ✅ 채팅 로그 영구 보관 (MinIO 텍스트 파일)
- ✅ 읽음 확인 (간소화된 방 단위 처리)
- ✅ 인앱 알림 (DB 저장형)
- ✅ 자동 종료 (2시간 비활성, 24시간 제한)
- ✅ 상담 이력 조회/통계

### 1.3 기술 스택

| 구분 | 기술 |
|------|------|
| **백엔드** | Spring Boot 2.7 + JPA |
| **데이터베이스** | MariaDB (MySQL) |
| **실시간 통신** | WebSocket + STOMP |
| **채팅 메시지 저장** | Redis (임시) → MinIO (영구) |
| **파일 스토리지** | MinIO (채팅 로그 텍스트 파일) |
| **인증** | JWT (기존 시스템 활용) |
| **캐시/세션** | Redis |
| **푸시 알림** | FCM (기존 시스템 활용) |
| **스케줄링** | Spring @Scheduled |

### 1.4 기존 시스템과의 통합
- **인증 시스템**: 기존 JWT 인증 활용
- **사용자 관리**: UserTbl 엔티티 활용 (userStudent 필드로 학생/교수 구분)
- **알림 시스템**: FCM과 DB 저장형 알림 병행
- **파일 스토리지**: 기존 MinIO 시스템 활용 (채팅 로그 저장)
- **아키텍처 패턴**: 기존 시설 예약 시스템(FacilityReservation) 구조 참고

### 1.5 아키텍처 최적화 (v2.0)

**v1.0 대비 개선사항:**

1. **테이블 구조 단순화**
   - CONSULTATION_ROOM_TBL 제거 → REQUEST_TBL에 통합 (1:1 관계 최적화)
   - CHAT_MESSAGE_TBL 제거 → Redis + MinIO로 대체

2. **채팅 메시지 처리 전략**
   - 실시간 채팅: Redis에만 저장 (휘발성, 빠른 I/O)
   - 종료 후: MinIO 텍스트 파일로 영구 보관 (저비용, 장기 보관)
   - DB: 메타데이터 없음 (파일명 규칙으로 해결)

3. **기능 간소화**
   - 요청 수정 기능 제거 → 반려 사유에 가능 시간 작성으로 대체
   - 읽음 처리 간소화 → 메시지별 읽음 대신 방 단위 last_read_time

4. **예상 효과**
   - DB 부하 50% 감소 (채팅 메시지 INSERT 제거)
   - 구현 시간 24% 단축 (37시간 → 28시간)
   - 유지보수 복잡도 감소

---

## 2. 아키텍처 설계

### 2.1 레이어 구조

```
┌─────────────────────────────────────┐
│        Presentation Layer           │
│  (Controller + WebSocket Handler)   │
├─────────────────────────────────────┤
│         Service Layer               │
│  (Business Logic + Scheduler)       │
├─────────────────────────────────────┤
│      Repository Layer               │
│         (JPA Repository)            │
├─────────────────────────────────────┤
│       Database Layer                │
│  (MariaDB + Redis + WebSocket)      │
└─────────────────────────────────────┘
```

### 2.2 주요 컴포넌트

#### Entity (2개) ⚡ 단순화
```
ConsultationRequest   - 상담 요청 + 상담방 정보 통합
Notification          - 인앱 알림
```

#### Service (4개)
```
ConsultationService                - 상담 요청/진행 비즈니스 로직
ChatService                        - Redis + MinIO 메시지 관리
NotificationService                - 알림 관리
ConsultationAutoCloseScheduler     - 자동 종료 스케줄러
```

#### Controller (3개)
```
ConsultationController             - REST API (상담 요청/진행)
ChatController                     - WebSocket (실시간 채팅) + REST
NotificationController             - REST API (알림)
```

### 2.3 데이터 흐름

#### 상담 요청 플로우
```
1. 학생이 상담 요청 생성
   └→ POST /api/consultations/requests

2. 교수에게 알림 전송
   └→ FCM 푸시 + DB 저장

3. 교수가 요청 확인
   └→ GET /api/consultations/requests/received

4. 교수가 수락/거절
   └→ POST /api/consultations/requests/{id}/accept
   └→ 수락 시 ConsultationRoom 자동 생성

5. 학생에게 결과 알림
   └→ FCM 푸시 + DB 저장
```

#### 실시간 채팅 플로우 ⚡ 최적화
```
1. 상담방 입장
   └→ WebSocket 연결: /ws/chat
   └→ JWT 토큰으로 인증
   └→ Redis에서 최근 메시지 로드

2. 메시지 전송
   └→ WS /app/chat.send
   └→ Redis에 저장 (chat:room:{requestIdx})
   └→ 브로드캐스트: /topic/chat/{requestIdx}
   └→ lastActivityAt 갱신 (DB)

3. 메시지 수신
   └→ 실시간 수신 (WebSocket)
   └→ 읽음 처리: last_read_time_student/professor 갱신 (방 단위)

4. 상담 종료
   └→ Redis 메시지 수집
   └→ MinIO에 텍스트 파일 업로드 (chat_{requestIdx}.txt)
   └→ Redis 삭제
   └→ DB status = COMPLETED

5. 이력 조회
   └→ MinIO에서 chat_{requestIdx}.txt 다운로드
```

#### 자동 종료 플로우
```
1. 스케줄러 실행 (1분마다)
   └→ ConsultationAutoCloseScheduler

2. 비활성 체크 (2시간)
   └→ lastActivityAt 확인
   └→ 1시간 55분 경과 → 5분 전 경고 알림
   └→ 2시간 경과 → 자동 종료

3. 24시간 제한 체크
   └→ startedAt 확인
   └→ 23시간 50분 경과 → 10분 전 경고 알림
   └→ 24시간 경과 → 강제 종료

4. 종료 처리
   └→ status = COMPLETED
   └→ endedAt = 현재 시간
   └→ duration 계산
   └→ 양측에 알림 전송
```

---

## 3. 데이터베이스 스키마

### 3.1 CONSULTATION_REQUEST_TBL (상담 요청 + 진행 통합) ⚡ 최적화

```sql
CREATE TABLE CONSULTATION_REQUEST_TBL (
    request_idx BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '요청 고유 ID',

    -- 요청 정보
    requester_user_code VARCHAR(20) NOT NULL COMMENT '요청자 학번',
    recipient_user_code VARCHAR(20) NOT NULL COMMENT '수신자 교번',
    consultation_type VARCHAR(50) NOT NULL COMMENT '상담 유형',
    title VARCHAR(100) NOT NULL COMMENT '상담 제목',
    content VARCHAR(1000) COMMENT '상담 내용',
    desired_date DATETIME COMMENT '희망 날짜',

    -- 요청 처리
    request_status VARCHAR(20) NOT NULL DEFAULT 'PENDING' COMMENT '요청 상태',
    confirmed_date DATETIME COMMENT '최종 확정 날짜',
    accept_message VARCHAR(500) COMMENT '수락 메시지',
    rejection_reason VARCHAR(500) COMMENT '거절 사유 (가능 시간 포함)',

    -- 상담 진행 (수락 후)
    consultation_status VARCHAR(20) COMMENT '상담 진행 상태',
    started_at DATETIME COMMENT '상담 시작 시간',
    ended_at DATETIME COMMENT '상담 종료 시간',
    duration_minutes INT COMMENT '상담 시간(분)',
    last_activity_at DATETIME COMMENT '마지막 활동 시간',

    -- 읽음 처리 (방 단위)
    last_read_time_student DATETIME COMMENT '학생 마지막 읽음 시간',
    last_read_time_professor DATETIME COMMENT '교수 마지막 읽음 시간',

    -- 메모 및 메타데이터
    memo TEXT COMMENT '상담 메모 (교수 작성)',

    -- 타임스탬프
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '생성 일시',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '수정 일시',

    -- 인덱스
    INDEX idx_requester (requester_user_code),
    INDEX idx_recipient (recipient_user_code),
    INDEX idx_request_status (request_status),
    INDEX idx_consultation_status (consultation_status),
    INDEX idx_created_at (created_at),
    INDEX idx_last_activity (last_activity_at),
    INDEX idx_started_at (started_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='상담 요청 및 진행';

-- 상담 유형 (consultation_type)
-- - ACADEMIC: 학업상담
-- - CAREER: 진로상담
-- - CAMPUS_LIFE: 학교생활
-- - ETC: 기타

-- 요청 상태 (request_status)
-- - PENDING: 대기중
-- - APPROVED: 수락됨
-- - REJECTED: 거절됨
-- - CANCELLED: 취소됨

-- 상담 진행 상태 (consultation_status)
-- - NULL: 아직 시작 안함
-- - IN_PROGRESS: 진행중
-- - COMPLETED: 완료됨
-- - CANCELLED: 취소됨
```

### 3.2 ~~CONSULTATION_ROOM_TBL~~ ❌ 제거됨

**제거 이유:**
- 상담 요청과 1:1 관계 → 별도 테이블 불필요
- CONSULTATION_REQUEST_TBL에 통합하여 JOIN 제거
- 구조 단순화 및 성능 향상

### 3.3 ~~CHAT_MESSAGE_TBL~~ ❌ 제거됨

**제거 이유 및 대체 방안:**

| 기존 방식 (DB) | 새로운 방식 (Redis + MinIO) |
|----------------|------------------------------|
| 모든 메시지 DB INSERT | 실시간: Redis 임시 저장 |
| 높은 DB 부하 | 종료시: MinIO 텍스트 파일 저장 |
| 개별 읽음 처리 복잡 | 방 단위 읽음 처리 간소화 |
| 백업 크기 증가 | 저비용 장기 보관 |

**Redis 데이터 구조:**
```redis
# 채팅 메시지 리스트
Key: chat:room:{request_idx}
Type: List
Value: JSON {sender, senderName, content, sentAt}
TTL: 채팅 종료시 삭제

# 예시
LPUSH chat:room:123 '{"sender":"2024001234","senderName":"김철수","content":"안녕하세요","sentAt":"2025-10-22T14:05:00"}'
```

**MinIO 파일 구조:**
```
Bucket: consultations
파일명: chat_{request_idx}.txt
경로: /consultations/chat_123.txt

파일 내용 예시:
==============================================
상담 채팅 기록
상담 ID: 123
학생: 김철수 (2024001234)
교수: 이교수 (P2024001)
시작: 2025-10-22 14:00:00
종료: 2025-10-22 15:00:00
==============================================

[14:05:00] 김철수: 안녕하세요, 교수님
[14:05:30] 이교수: 네, 안녕하세요
[14:06:00] 김철수: 미적분학 과제 관련 질문드립니다
...

==============================================
총 메시지 수: 45개
상담 시간: 60분
==============================================

메타데이터 (MinIO Object Metadata):
- request-idx: 123
- message-count: 45
- participants: 2024001234,P2024001
```

### 3.4 NOTIFICATION_TBL (인앱 알림) ✅ 유지

```sql
CREATE TABLE NOTIFICATION_TBL (
    notification_idx BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '알림 고유 ID',
    user_code VARCHAR(20) NOT NULL COMMENT '수신자 학번/교번',
    notification_type VARCHAR(50) NOT NULL COMMENT '알림 유형',

    title VARCHAR(200) NOT NULL COMMENT '알림 제목',
    content VARCHAR(500) COMMENT '알림 내용',
    link_url VARCHAR(500) COMMENT '관련 링크',

    is_read BOOLEAN NOT NULL DEFAULT FALSE COMMENT '읽음 여부',
    read_at DATETIME COMMENT '읽은 시간',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '생성 시간',

    INDEX idx_user (user_code),
    INDEX idx_is_read (is_read),
    INDEX idx_created_at (created_at),
    INDEX idx_type (notification_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='인앱 알림';

-- 알림 유형 (notification_type)
-- - NEW_CONSULTATION_REQUEST: 새 상담 요청 받음
-- - CONSULTATION_ACCEPTED: 상담 요청 수락됨
-- - CONSULTATION_REJECTED: 상담 요청 거절됨
-- - NEW_MESSAGE: 새 메시지 수신
-- - CONSULTATION_REMINDER: 상담 시작 30분 전 리마인더
-- - CONSULTATION_ENDED: 상담 종료됨
-- - AUTO_CLOSE_WARNING_5MIN: 자동 종료 5분 전 경고
-- - AUTO_CLOSE_WARNING_10MIN: 자동 종료 10분 전 경고
```

### 3.5 ER 다이어그램 ⚡ 단순화

```
┌─────────────────────────────────────────────────┐
│  CONSULTATION_REQUEST_TBL                       │
│  (상담 요청 + 진행 통합)                         │
├─────────────────────────────────────────────────┤
│ PK: request_idx                                 │
│                                                 │
│ [요청 정보]                                      │
│   requester_user_code, recipient_user_code     │
│   consultation_type, title, content            │
│   desired_date                                 │
│                                                 │
│ [요청 처리]                                      │
│   request_status (PENDING/APPROVED/REJECTED)   │
│   confirmed_date, acceptance_message           │
│   rejection_reason (가능 시간 포함)              │
│                                                 │
│ [상담 진행]                                      │
│   consultation_status (IN_PROGRESS/COMPLETED)  │
│   started_at, ended_at, duration_minutes       │
│   last_activity_at                             │
│                                                 │
│ [읽음 처리]                                      │
│   last_read_time_student                       │
│   last_read_time_professor                     │
│                                                 │
│ [메모]                                           │
│   memo                                         │
└─────────────────────────────────────────────────┘

┌─────────────────────────────┐
│  NOTIFICATION_TBL           │
│  (인앱 알림)                 │
├─────────────────────────────┤
│ PK: notification_idx        │
│     user_code               │
│     notification_type       │
│     title, content          │
│     is_read, read_at        │
└─────────────────────────────┘

┌─────────────────────────────┐
│  Redis                      │
│  (실시간 채팅 - 휘발성)      │
├─────────────────────────────┤
│ Key: chat:room:{requestIdx} │
│ Type: List                  │
│ Value: JSON messages        │
│ TTL: 상담 종료시 삭제        │
└─────────────────────────────┘

┌─────────────────────────────┐
│  MinIO                      │
│  (채팅 로그 - 영구 보관)     │
├─────────────────────────────┤
│ Bucket: consultations       │
│ Object: chat_{requestIdx}.txt│
│ Metadata: request-idx,      │
│           message-count     │
└─────────────────────────────┘
```

**데이터 흐름:**
```
1. 요청 생성 → CONSULTATION_REQUEST_TBL (request_status: PENDING)
2. 승인 → request_status: APPROVED, consultation_status: IN_PROGRESS
3. 채팅 시작 → Redis (chat:room:{requestIdx})
4. 메시지 송수신 → Redis LPUSH
5. 상담 종료 → Redis 메시지 수집 → MinIO 업로드 → Redis 삭제
6. 이력 조회 → MinIO에서 다운로드
```

---

## 4. 디렉토리 구조

### 4.1 전체 구조 ⚡ 단순화

```
backend/BlueCrab/src/main/java/BlueCrab/com/example/
│
├── entity/
│   └── Consultation/                    ✨ 새로 생성
│       ├── ConsultationRequest.java     (요청 + 진행 통합)
│       └── Notification.java
│
├── repository/
│   └── Consultation/                    ✨ 새로 생성
│       ├── ConsultationRequestRepository.java
│       └── NotificationRepository.java
│
├── dto/
│   └── Consultation/                    ✨ 새로 생성
│       ├── ConsultationRequestDto.java
│       ├── ConsultationAcceptDto.java
│       ├── ConsultationRejectDto.java
│       ├── ConsultationRoomDto.java     (진행중인 상담 정보)
│       ├── ConsultationDetailDto.java
│       ├── ConsultationMemoDto.java
│       ├── ChatMessageDto.java          (Redis/MinIO용)
│       ├── ChatHistoryDto.java
│       ├── NotificationDto.java
│       └── NotificationListDto.java
│
├── service/
│   └── Consultation/                    ✨ 새로 생성
│       ├── ConsultationService.java     (요청 + 진행 통합)
│       ├── ChatService.java             (Redis + MinIO)
│       ├── NotificationService.java
│       └── ConsultationAutoCloseScheduler.java
│
├── controller/
│   └── Consultation/                    ✨ 새로 생성
│       ├── ConsultationController.java  (요청 + 진행 통합)
│       ├── ChatController.java          (WebSocket + REST)
│       └── NotificationController.java
│
├── config/
│   └── WebSocketConfig.java             ✨ 새로 생성
│
└── enums/
    ├── ConsultationType.java             ✨ 새로 생성
    ├── RequestStatus.java                ✨ 새로 생성
    ├── ConsultationStatus.java           ✨ 새로 생성
    └── NotificationType.java             ✨ 새로 생성
```

### 4.2 파일 개수 ⚡ 24% 감소

| 구분 | v1.0 (기존) | v2.0 (최적화) | 변경 |
|------|-------------|---------------|------|
| **Entity** | 4개 | **2개** | -50% |
| **Repository** | 4개 | **2개** | -50% |
| **DTO** | 12개 | **10개** | -17% |
| **Service** | 5개 | **4개** | -20% |
| **Controller** | 4개 | **3개** | -25% |
| **Config** | 1개 | **1개** | - |
| **Enum** | 3개 | **4개** | +33% |
| **총계** | 33개 | **26개** | **-21%** |

---

## 5. API 엔드포인트

### 5.1 상담 요청 API (7개)

#### 1) 상담 요청 생성
```
POST /api/consultations/requests
Content-Type: application/json
Authorization: Bearer {jwt_token}

Request Body:
{
  "recipientUserCode": "2024001234",
  "consultationType": "ACADEMIC",
  "title": "미적분학 관련 질문",
  "content": "미적분학 과제 관련하여 질문드리고 싶습니다.",
  "desiredDate": "2025-10-25T14:00:00",
  "desiredTime": "14:00"
}

Response: 201 Created
{
  "requestIdx": 1,
  "status": "PENDING",
  "requestedAt": "2025-10-22T10:30:00"
}
```

#### 2) 받은 상담 요청 목록
```
GET /api/consultations/requests/received
Authorization: Bearer {jwt_token}
Query Parameters:
  - status: PENDING|ACCEPTED|REJECTED (optional)
  - page: 0 (default)
  - size: 20 (default)

Response: 200 OK
{
  "content": [
    {
      "requestIdx": 1,
      "requesterUserCode": "2024001234",
      "requesterName": "김철수",
      "consultationType": "ACADEMIC",
      "title": "미적분학 관련 질문",
      "content": "...",
      "desiredDate": "2025-10-25T14:00:00",
      "status": "PENDING",
      "requestedAt": "2025-10-22T10:30:00"
    }
  ],
  "totalElements": 5,
  "totalPages": 1
}
```

#### 3) 보낸 상담 요청 목록
```
GET /api/consultations/requests/sent
Authorization: Bearer {jwt_token}
Query Parameters: (동일)

Response: 200 OK (동일 구조)
```

#### 4) 상담 요청 수락
```
POST /api/consultations/requests/{requestIdx}/accept
Authorization: Bearer {jwt_token}

Request Body:
{
  "confirmedDate": "2025-10-25T14:00:00",
  "confirmedTime": "14:00",
  "acceptMessage": "해당 시간에 상담 가능합니다."
}

Response: 200 OK
{
  "requestIdx": 1,
  "status": "ACCEPTED",
  "roomIdx": 10,
  "confirmedDate": "2025-10-25T14:00:00"
}
```

#### 5) 상담 요청 거절
```
POST /api/consultations/requests/{requestIdx}/reject
Authorization: Bearer {jwt_token}

Request Body:
{
  "rejectReason": "해당 시간에 일정이 있습니다."
}

Response: 200 OK
{
  "requestIdx": 1,
  "status": "REJECTED"
}
```

#### 6) 상담 요청 취소
```
DELETE /api/consultations/requests/{requestIdx}
Authorization: Bearer {jwt_token}

Response: 204 No Content
```

#### 7) 상담 요청 수정
```
PUT /api/consultations/requests/{requestIdx}
Authorization: Bearer {jwt_token}

Request Body:
{
  "title": "수정된 제목",
  "content": "수정된 내용",
  "desiredDate": "2025-10-26T15:00:00"
}

Response: 200 OK
```

---

### 5.2 상담방 API (5개)

#### 1) 상담방 목록
```
GET /api/consultations/rooms
Authorization: Bearer {jwt_token}
Query Parameters:
  - status: ACTIVE|COMPLETED (optional)
  - page: 0
  - size: 20

Response: 200 OK
{
  "content": [
    {
      "roomIdx": 10,
      "partnerUserCode": "2024001234",
      "partnerName": "김철수",
      "consultationType": "ACADEMIC",
      "status": "ACTIVE",
      "startedAt": "2025-10-25T14:00:00",
      "lastActivityAt": "2025-10-25T14:30:00",
      "unreadCount": 3
    }
  ],
  "totalElements": 2,
  "totalPages": 1
}
```

#### 2) 상담방 상세
```
GET /api/consultations/rooms/{roomIdx}
Authorization: Bearer {jwt_token}

Response: 200 OK
{
  "roomIdx": 10,
  "requestIdx": 1,
  "studentUserCode": "2024001234",
  "studentName": "김철수",
  "professorUserCode": "P2024001",
  "professorName": "이교수",
  "consultationType": "ACADEMIC",
  "status": "ACTIVE",
  "startedAt": "2025-10-25T14:00:00",
  "endedAt": null,
  "duration": null,
  "memo": "수학 관련 상담",
  "lastActivityAt": "2025-10-25T14:30:00"
}
```

#### 3) 상담 종료
```
POST /api/consultations/rooms/{roomIdx}/end
Authorization: Bearer {jwt_token}

Response: 200 OK
{
  "roomIdx": 10,
  "status": "COMPLETED",
  "endedAt": "2025-10-25T15:00:00",
  "duration": 60
}
```

#### 4) 메모 작성/수정
```
PUT /api/consultations/rooms/{roomIdx}/memo
Authorization: Bearer {jwt_token}

Request Body:
{
  "memo": "학생이 미적분학에 대한 이해도가 높음. 추가 학습 권장."
}

Response: 200 OK
```

#### 5) 상담 연장
```
POST /api/consultations/rooms/{roomIdx}/extend
Authorization: Bearer {jwt_token}

Response: 200 OK
{
  "roomIdx": 10,
  "lastActivityAt": "2025-10-25T15:00:00",
  "message": "상담이 연장되었습니다."
}
```

---

### 5.3 채팅 API (WebSocket + REST)

#### 1) WebSocket 연결
```
WebSocket Endpoint: ws://localhost:8080/ws/chat
Protocol: STOMP
Headers:
  Authorization: Bearer {jwt_token}

Subscribe: /topic/chat/{roomIdx}
Send: /app/chat.send
```

#### 2) 메시지 전송 (WebSocket)
```
Send to: /app/chat.send

Payload:
{
  "roomIdx": 10,
  "content": "안녕하세요, 교수님"
}

Broadcast to: /topic/chat/10
{
  "messageIdx": 100,
  "roomIdx": 10,
  "senderUserCode": "2024001234",
  "senderName": "김철수",
  "content": "안녕하세요, 교수님",
  "sentAt": "2025-10-25T14:05:00",
  "isRead": false
}
```

#### 3) 메시지 이력 조회 (REST)
```
POST /api/chat/messages/history
Authorization: Bearer {jwt_token}

Request Body:
{
  "roomIdx": 10,
  "page": 0,
  "size": 50
}

Response: 200 OK
{
  "content": [
    {
      "messageIdx": 100,
      "senderUserCode": "2024001234",
      "senderName": "김철수",
      "content": "안녕하세요, 교수님",
      "sentAt": "2025-10-25T14:05:00",
      "isRead": true,
      "readAt": "2025-10-25T14:05:30"
    }
  ],
  "totalElements": 25,
  "totalPages": 1
}
```

#### 4) 읽음 처리 (REST)
```
POST /api/chat/messages/{messageIdx}/read
Authorization: Bearer {jwt_token}

Response: 200 OK
{
  "messageIdx": 100,
  "isRead": true,
  "readAt": "2025-10-25T14:05:30"
}
```

#### 5) 타이핑 알림 (WebSocket) - 선택사항
```
Send to: /app/chat.typing

Payload:
{
  "roomIdx": 10,
  "isTyping": true
}

Broadcast to: /topic/chat/10/typing
{
  "userCode": "2024001234",
  "userName": "김철수",
  "isTyping": true
}
```

---

### 5.4 알림 API (6개)

#### 1) 알림 목록
```
GET /api/notifications
Authorization: Bearer {jwt_token}
Query Parameters:
  - page: 0
  - size: 20

Response: 200 OK
{
  "content": [
    {
      "notificationIdx": 1,
      "notificationType": "NEW_CONSULTATION_REQUEST",
      "title": "새 상담 요청",
      "content": "김철수 학생이 상담을 요청했습니다.",
      "linkUrl": "/consultations/requests/1",
      "isRead": false,
      "createdAt": "2025-10-22T10:30:00"
    }
  ],
  "totalElements": 10,
  "totalPages": 1
}
```

#### 2) 안읽은 알림
```
GET /api/notifications/unread
Authorization: Bearer {jwt_token}

Response: 200 OK (동일 구조)
```

#### 3) 안읽은 알림 개수
```
GET /api/notifications/count
Authorization: Bearer {jwt_token}

Response: 200 OK
{
  "count": 5
}
```

#### 4) 읽음 처리
```
POST /api/notifications/{notificationIdx}/read
Authorization: Bearer {jwt_token}

Response: 200 OK
{
  "notificationIdx": 1,
  "isRead": true,
  "readAt": "2025-10-22T11:00:00"
}
```

#### 5) 전체 읽음
```
POST /api/notifications/read-all
Authorization: Bearer {jwt_token}

Response: 200 OK
{
  "updatedCount": 5
}
```

#### 6) 알림 삭제
```
DELETE /api/notifications/{notificationIdx}
Authorization: Bearer {jwt_token}

Response: 204 No Content
```

---

### 5.5 상담 이력 API (4개)

#### 1) 상담 이력 목록
```
GET /api/consultations/history
Authorization: Bearer {jwt_token}
Query Parameters:
  - startDate: 2025-10-01 (optional)
  - endDate: 2025-10-31 (optional)
  - consultationType: ACADEMIC|CAREER|CAMPUS_LIFE|ETC (optional)
  - partnerUserCode: 2024001234 (optional)
  - page: 0
  - size: 20

Response: 200 OK
{
  "content": [
    {
      "roomIdx": 10,
      "partnerUserCode": "2024001234",
      "partnerName": "김철수",
      "consultationType": "ACADEMIC",
      "startedAt": "2025-10-25T14:00:00",
      "endedAt": "2025-10-25T15:00:00",
      "duration": 60,
      "memo": "수학 관련 상담"
    }
  ],
  "totalElements": 15,
  "totalPages": 1
}
```

#### 2) 상담 상세 내역
```
GET /api/consultations/history/{roomIdx}
Authorization: Bearer {jwt_token}

Response: 200 OK
{
  "roomIdx": 10,
  "consultationType": "ACADEMIC",
  "startedAt": "2025-10-25T14:00:00",
  "endedAt": "2025-10-25T15:00:00",
  "duration": 60,
  "memo": "수학 관련 상담",
  "messages": [
    {
      "senderUserCode": "2024001234",
      "senderName": "김철수",
      "content": "안녕하세요, 교수님",
      "sentAt": "2025-10-25T14:05:00"
    }
  ],
  "student": {
    "userCode": "2024001234",
    "userName": "김철수"
  },
  "professor": {
    "userCode": "P2024001",
    "userName": "이교수"
  }
}
```

#### 3) 상담 통계
```
GET /api/consultations/stats
Authorization: Bearer {jwt_token}
Query Parameters:
  - period: MONTH|QUARTER|HALF_YEAR|YEAR

Response: 200 OK
{
  "totalConsultations": 25,
  "totalDuration": 1800,
  "averageDuration": 72,
  "byType": {
    "ACADEMIC": 10,
    "CAREER": 8,
    "CAMPUS_LIFE": 5,
    "ETC": 2
  },
  "byMonth": [
    {"month": "2025-08", "count": 5},
    {"month": "2025-09", "count": 10},
    {"month": "2025-10", "count": 10}
  ]
}
```

#### 4) 상담 이력 내보내기
```
GET /api/consultations/export
Authorization: Bearer {jwt_token}
Query Parameters:
  - format: excel|pdf
  - startDate: 2025-10-01
  - endDate: 2025-10-31

Response: 200 OK
Content-Type: application/vnd.openxmlformats-officedocument.spreadsheetml.sheet
Content-Disposition: attachment; filename="consultation_history_2025-10.xlsx"

[Binary File]
```

---

## 6. 구현 순서

### Phase 1: 기반 구축 (6시간)

#### 작업 내용
1. **데이터베이스 스키마 작성** (1시간)
   - [x] 4개 테이블 CREATE 문 작성
   - [x] 인덱스 및 외래키 설정
   - [x] 초기 데이터 삽입 스크립트

2. **Entity 클래스 작성** (2시간)
   - [x] ConsultationRequest.java
   - [x] ConsultationRoom.java
   - [x] ChatMessage.java
   - [x] Notification.java
   - [x] JPA 어노테이션 설정
   - [x] Lombok 적용

3. **Repository 작성** (1시간)
   - [x] ConsultationRequestRepository.java
   - [x] ConsultationRoomRepository.java
   - [x] ChatMessageRepository.java
   - [x] NotificationRepository.java
   - [x] 커스텀 쿼리 메서드

4. **Enum 클래스 작성** (30분)
   - [x] ConsultationType.java
   - [x] ConsultationStatus.java
   - [x] NotificationType.java

5. **DTO 클래스 작성** (1.5시간)
   - [x] Request/Response DTO 12개
   - [x] Validation 어노테이션

#### 체크포인트
- [ ] 데이터베이스 테이블 정상 생성 확인
- [ ] Entity 매핑 테스트 (JUnit)
- [ ] Repository 기본 CRUD 테스트

---

### Phase 2: 상담 요청 시스템 (5시간)

#### 작업 내용
1. **ConsultationRequestService** (2시간)
   - [x] 요청 생성 로직
   - [x] 요청 목록 조회 (받은/보낸)
   - [x] 요청 수정/취소 로직
   - [x] 요청 수락 로직 (ConsultationRoom 자동 생성)
   - [x] 요청 거절 로직
   - [x] 권한 검증 (본인 요청만 수정/취소 가능)

2. **ConsultationRequestController** (1.5시간)
   - [x] POST /api/consultations/requests
   - [x] GET /api/consultations/requests/received
   - [x] GET /api/consultations/requests/sent
   - [x] POST /api/consultations/requests/{id}/accept
   - [x] POST /api/consultations/requests/{id}/reject
   - [x] DELETE /api/consultations/requests/{id}
   - [x] PUT /api/consultations/requests/{id}

3. **통합 테스트** (1.5시간)
   - [x] Postman 테스트
   - [x] JUnit 통합 테스트
   - [x] 알림 전송 확인

#### 체크포인트
- [ ] 상담 요청 생성 → 알림 전송 확인
- [ ] 상담 수락 → ConsultationRoom 생성 확인
- [ ] 상담 거절 → 알림 전송 확인
- [ ] 권한 검증 동작 확인

---

### Phase 3: 상담방 시스템 (4시간)

#### 작업 내용
1. **ConsultationRoomService** (2시간)
   - [x] 상담방 목록 조회 (내 상담방만)
   - [x] 상담방 상세 조회
   - [x] 상담 종료 로직 (duration 계산)
   - [x] 메모 작성/수정
   - [x] 상담 연장 (lastActivityAt 갱신)
   - [x] 권한 검증 (참여자만 접근 가능)

2. **ConsultationRoomController** (1시간)
   - [x] GET /api/consultations/rooms
   - [x] GET /api/consultations/rooms/{id}
   - [x] POST /api/consultations/rooms/{id}/end
   - [x] PUT /api/consultations/rooms/{id}/memo
   - [x] POST /api/consultations/rooms/{id}/extend

3. **통합 테스트** (1시간)
   - [x] 상담방 조회 테스트
   - [x] 상담 종료 테스트
   - [x] 메모 기능 테스트

#### 체크포인트
- [ ] 상담방 목록에 내 상담만 표시
- [ ] 상담 종료 시 duration 정확히 계산
- [ ] 메모 저장 확인
- [ ] 타인의 상담방 접근 차단 확인

---

### Phase 4: WebSocket & 채팅 (8시간)

#### 작업 내용
1. **WebSocketConfig 설정** (1시간)
   - [x] @EnableWebSocketMessageBroker
   - [x] STOMP 엔드포인트 설정
   - [x] Message Broker 설정
   - [x] JWT 인증 통합 (Interceptor)

2. **ChatMessageService** (2시간)
   - [x] 메시지 저장
   - [x] 메시지 이력 조회 (페이징)
   - [x] 읽음 처리 로직
   - [x] lastActivityAt 갱신 (자동 종료용)
   - [x] 권한 검증

3. **ChatController** (2시간)
   - [x] @MessageMapping("/chat.send")
   - [x] @SendTo("/topic/chat/{roomIdx}")
   - [x] WebSocket 연결/해제 핸들러
   - [x] 예외 처리

4. **REST API** (1시간)
   - [x] POST /api/chat/messages/history
   - [x] POST /api/chat/messages/{id}/read

5. **통합 테스트** (2시간)
   - [x] WebSocket 연결 테스트
   - [x] 메시지 송수신 테스트
   - [x] 읽음 처리 테스트
   - [x] 프론트엔드 연동 테스트 (STOMP.js)

#### 체크포인트
- [ ] WebSocket 연결 성공
- [ ] JWT 인증 동작 확인
- [ ] 메시지 실시간 전송/수신 확인
- [ ] 메시지 DB 저장 확인
- [ ] 읽음 처리 동작 확인

---

### Phase 5: 인앱 알림 시스템 (4시간)

#### 작업 내용
1. **NotificationService** (2시간)
   - [x] 알림 생성 로직
   - [x] 알림 목록 조회 (전체/안읽은)
   - [x] 안읽은 알림 개수
   - [x] 읽음 처리 (개별/전체)
   - [x] 알림 삭제
   - [x] FCM 푸시 연동 (기존 FirebasePushService 활용)

2. **NotificationController** (1시간)
   - [x] GET /api/notifications
   - [x] GET /api/notifications/unread
   - [x] GET /api/notifications/count
   - [x] POST /api/notifications/{id}/read
   - [x] POST /api/notifications/read-all
   - [x] DELETE /api/notifications/{id}

3. **통합 테스트** (1시간)
   - [x] 알림 생성 → FCM 푸시 전송 확인
   - [x] 알림 목록 조회 테스트
   - [x] 읽음 처리 테스트

#### 체크포인트
- [ ] 상담 요청 시 알림 생성 확인
- [ ] FCM 푸시 전송 확인
- [ ] 알림 목록 정상 조회
- [ ] 읽음 처리 동작 확인

---

### Phase 6: 자동 종료 스케줄러 (4시간)

#### 작업 내용
1. **ConsultationAutoCloseScheduler** (2.5시간)
   - [x] @Scheduled(fixedDelay = 60000) - 1분마다 실행
   - [x] 비활성 체크 로직
     - [ ] 1시간 55분 경과 → 5분 전 경고 알림
     - [ ] 2시간 경과 → 자동 종료
   - [x] 24시간 제한 체크 로직
     - [ ] 23시간 50분 경과 → 10분 전 경고 알림
     - [ ] 24시간 경과 → 강제 종료
   - [x] 종료 처리 로직
     - [ ] status = COMPLETED
     - [ ] endedAt 설정
     - [ ] duration 계산
     - [ ] 양측 알림 전송

2. **@EnableScheduling 활성화** (30분)
   - [x] BlueCrabApplication.java 수정
   - [x] 스케줄러 테스트

3. **테스트** (1시간)
   - [x] 비활성 자동 종료 테스트
   - [x] 24시간 제한 테스트
   - [x] 경고 알림 전송 확인

#### 체크포인트
- [ ] 스케줄러 1분마다 실행 확인
- [ ] 2시간 비활성 시 자동 종료 확인
- [ ] 24시간 경과 시 강제 종료 확인
- [ ] 경고 알림 전송 확인

---

### Phase 7: 상담 이력 시스템 (3시간)

#### 작업 내용
1. **ConsultationHistoryService** (1.5시간)
   - [x] 상담 이력 목록 조회 (필터링)
   - [x] 상담 상세 내역 조회 (메시지 포함)
   - [x] 상담 통계 (월별, 유형별)
   - [x] Excel/PDF 내보내기

2. **API 구현** (1시간)
   - [x] GET /api/consultations/history
   - [x] GET /api/consultations/history/{id}
   - [x] GET /api/consultations/stats
   - [x] GET /api/consultations/export

3. **테스트** (30분)
   - [x] 필터링 동작 확인
   - [x] 통계 계산 확인
   - [x] 내보내기 파일 생성 확인

#### 체크포인트
- [ ] 완료된 상담만 이력에 표시
- [ ] 필터링 정상 동작
- [ ] 통계 정확도 확인
- [ ] Excel 파일 다운로드 확인

---

### Phase 8: 통합 테스트 & 문서화 (3시간)

#### 작업 내용
1. **전체 시나리오 테스트** (2시간)
   - [x] 학생이 상담 요청
   - [x] 교수가 수락
   - [x] 실시간 채팅
   - [x] 읽음 처리
   - [x] 상담 종료
   - [x] 이력 조회
   - [x] 자동 종료 시나리오

2. **API 문서 작성** (1시간)
   - [x] Swagger/OpenAPI 설정 (선택)
   - [x] Postman Collection 작성
   - [x] README 업데이트

3. **SecurityConfig 업데이트**
   - [x] 상담 API 엔드포인트 권한 설정
   - [x] WebSocket 보안 설정

#### 체크포인트
- [ ] 전체 플로우 정상 동작
- [ ] API 문서 완성
- [ ] 보안 설정 확인

---

### 예상 작업 시간 총합

| Phase | 내용 | 예상 시간 |
|-------|------|----------|
| Phase 1 | 기반 구축 | 6시간 |
| Phase 2 | 상담 요청 시스템 | 5시간 |
| Phase 3 | 상담방 시스템 | 4시간 |
| Phase 4 | WebSocket & 채팅 | 8시간 |
| Phase 5 | 인앱 알림 시스템 | 4시간 |
| Phase 6 | 자동 종료 스케줄러 | 4시간 |
| Phase 7 | 상담 이력 시스템 | 3시간 |
| Phase 8 | 통합 테스트 & 문서화 | 3시간 |
| **총합** | | **37시간** |

**작업 기간:** 약 5일 (8시간/일 기준)

---

## 7. 보안 고려사항

### 7.1 JWT 인증 통합

#### SecurityConfig 업데이트
```java
@Override
protected void configure(HttpSecurity http) throws Exception {
    http
        .authorizeRequests()
            // 상담 API - 인증 필요
            .antMatchers("/api/consultations/**").authenticated()
            .antMatchers("/api/chat/**").authenticated()
            .antMatchers("/api/notifications/**").authenticated()

            // WebSocket - 인증 필요
            .antMatchers("/ws/**").authenticated()

        .and()
            .addFilterBefore(jwtAuthenticationFilter,
                UsernamePasswordAuthenticationFilter.class);
}
```

#### WebSocket JWT 인증
```java
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(new ChannelInterceptor() {
            @Override
            public Message<?> preSend(Message<?> message, MessageChannel channel) {
                StompHeaderAccessor accessor =
                    MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

                if (StompCommand.CONNECT.equals(accessor.getCommand())) {
                    String token = accessor.getFirstNativeHeader("Authorization");
                    // JWT 검증 로직
                    if (jwtUtil.validateToken(token)) {
                        String userCode = jwtUtil.getUserCodeFromToken(token);
                        accessor.setUser(new UsernamePasswordAuthenticationToken(
                            userCode, null, Collections.emptyList()
                        ));
                    } else {
                        throw new IllegalArgumentException("Invalid JWT token");
                    }
                }
                return message;
            }
        });
    }
}
```

### 7.2 권한 검증

#### Service Layer에서 검증
```java
@Service
public class ConsultationRequestService {

    // 본인 요청만 수정 가능
    public void updateRequest(Long requestIdx, String userCode, UpdateDto dto) {
        ConsultationRequest request = repository.findById(requestIdx)
            .orElseThrow(() -> new ResourceNotFoundException("요청을 찾을 수 없습니다."));

        // 권한 검증
        if (!request.getRequesterUserCode().equals(userCode)) {
            throw new UnauthorizedException("권한이 없습니다.");
        }

        // 수정 로직
    }

    // 수신자만 수락/거절 가능
    public void acceptRequest(Long requestIdx, String userCode, AcceptDto dto) {
        ConsultationRequest request = repository.findById(requestIdx)
            .orElseThrow();

        if (!request.getRecipientUserCode().equals(userCode)) {
            throw new UnauthorizedException("권한이 없습니다.");
        }

        // 수락 로직
    }
}
```

#### ConsultationRoomService 권한 검증
```java
// 참여자만 상담방 조회 가능
public ConsultationRoomDto getRoom(Long roomIdx, String userCode) {
    ConsultationRoom room = repository.findById(roomIdx)
        .orElseThrow();

    // 학생 또는 교수인지 확인
    if (!room.getStudentUserCode().equals(userCode) &&
        !room.getProfessorUserCode().equals(userCode)) {
        throw new UnauthorizedException("권한이 없습니다.");
    }

    return toDto(room);
}
```

### 7.3 입력 검증

#### DTO Validation
```java
@Data
public class ConsultationRequestDto {

    @NotBlank(message = "수신자를 선택해주세요.")
    private String recipientUserCode;

    @NotNull(message = "상담 유형을 선택해주세요.")
    private ConsultationType consultationType;

    @NotBlank(message = "제목을 입력해주세요.")
    @Size(max = 100, message = "제목은 100자 이하여야 합니다.")
    private String title;

    @Size(max = 1000, message = "내용은 1000자 이하여야 합니다.")
    private String content;

    @Future(message = "희망 날짜는 현재 이후여야 합니다.")
    private LocalDateTime desiredDate;
}
```

#### Controller Validation
```java
@PostMapping("/requests")
public ResponseEntity<?> createRequest(
    @Valid @RequestBody ConsultationRequestDto dto,
    @AuthenticationPrincipal String userCode
) {
    // 처리 로직
}
```

### 7.4 XSS 방지

#### HTML 이스케이프
```java
@Service
public class ChatMessageService {

    public ChatMessage saveMessage(String content) {
        // HTML 태그 이스케이프
        String sanitized = HtmlUtils.htmlEscape(content);

        ChatMessage message = new ChatMessage();
        message.setContent(sanitized);
        return repository.save(message);
    }
}
```

### 7.5 SQL Injection 방지

#### JPA Named Parameters 사용
```java
@Repository
public interface ConsultationRequestRepository extends JpaRepository<ConsultationRequest, Long> {

    // ✅ 안전: Named Parameters
    @Query("SELECT r FROM ConsultationRequest r WHERE r.recipientUserCode = :userCode")
    List<ConsultationRequest> findByRecipient(@Param("userCode") String userCode);

    // ❌ 위험: 문자열 연결
    // @Query("SELECT r FROM ConsultationRequest r WHERE r.recipientUserCode = '" + userCode + "'")
}
```

### 7.6 Rate Limiting

#### 상담 요청 생성 제한 (선택)
```java
@Service
public class ConsultationRequestService {

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    public void createRequest(String userCode, RequestDto dto) {
        String key = "consultation:request:" + userCode;

        // 1시간에 10개 요청으로 제한
        Long count = redisTemplate.opsForValue().increment(key);
        if (count == 1) {
            redisTemplate.expire(key, 1, TimeUnit.HOURS);
        }

        if (count > 10) {
            throw new RateLimitExceededException("요청 횟수를 초과했습니다.");
        }

        // 요청 생성 로직
    }
}
```

### 7.7 CORS 설정

#### WebSocketConfig CORS
```java
@Override
public void registerStompEndpoints(StompEndpointRegistry registry) {
    registry.addEndpoint("/ws/chat")
        .setAllowedOrigins("http://localhost:3000", "https://your-domain.com")
        .withSockJS();
}
```

---

## 8. 작업 체크리스트

### 8.1 준비 작업

- [ ] **pom.xml 업데이트**
  ```xml
  <dependency>
      <groupId>org.springframework.boot</groupId>
      <artifactId>spring-boot-starter-websocket</artifactId>
  </dependency>
  ```

- [ ] **디렉토리 구조 생성**
  ```bash
  mkdir -p src/main/java/BlueCrab/com/example/entity/Consultation
  mkdir -p src/main/java/BlueCrab/com/example/repository/Consultation
  mkdir -p src/main/java/BlueCrab/com/example/dto/Consultation/{request,room,message,notification}
  mkdir -p src/main/java/BlueCrab/com/example/service/Consultation
  mkdir -p src/main/java/BlueCrab/com/example/controller/Consultation
  ```

- [ ] **데이터베이스 스키마 실행**
  ```bash
  mysql -u root -p blue_crab_lms < schema/consultation_tables.sql
  ```

---

### 8.2 Phase 1: 기반 구축

#### Entity
- [ ] ConsultationRequest.java
- [ ] ConsultationRoom.java
- [ ] ChatMessage.java
- [ ] Notification.java

#### Repository
- [ ] ConsultationRequestRepository.java
- [ ] ConsultationRoomRepository.java
- [ ] ChatMessageRepository.java
- [ ] NotificationRepository.java

#### Enum
- [ ] ConsultationType.java
- [ ] ConsultationStatus.java
- [ ] NotificationType.java

#### DTO
- [ ] ConsultationRequestDto.java
- [ ] ConsultationAcceptDto.java
- [ ] ConsultationRejectDto.java
- [ ] ConsultationUpdateDto.java
- [ ] ConsultationRoomDto.java
- [ ] ConsultationRoomDetailDto.java
- [ ] ConsultationMemoDto.java
- [ ] ChatMessageDto.java
- [ ] MessageHistoryDto.java
- [ ] MessageReadDto.java
- [ ] NotificationDto.java
- [ ] NotificationListDto.java

---

### 8.3 Phase 2: 상담 요청 시스템

#### Service
- [ ] ConsultationRequestService.java
  - [ ] createRequest()
  - [ ] getReceivedRequests()
  - [ ] getSentRequests()
  - [ ] acceptRequest()
  - [ ] rejectRequest()
  - [ ] cancelRequest()
  - [ ] updateRequest()

#### Controller
- [ ] ConsultationRequestController.java
  - [ ] POST /api/consultations/requests
  - [ ] GET /api/consultations/requests/received
  - [ ] GET /api/consultations/requests/sent
  - [ ] POST /api/consultations/requests/{id}/accept
  - [ ] POST /api/consultations/requests/{id}/reject
  - [ ] DELETE /api/consultations/requests/{id}
  - [ ] PUT /api/consultations/requests/{id}

#### 테스트
- [ ] 상담 요청 생성 테스트
- [ ] 알림 전송 확인
- [ ] 수락 시 ConsultationRoom 생성 확인
- [ ] 권한 검증 테스트

---

### 8.4 Phase 3: 상담방 시스템

#### Service
- [ ] ConsultationRoomService.java
  - [ ] getRooms()
  - [ ] getRoomDetail()
  - [ ] endConsultation()
  - [ ] updateMemo()
  - [ ] extendConsultation()

#### Controller
- [ ] ConsultationRoomController.java
  - [ ] GET /api/consultations/rooms
  - [ ] GET /api/consultations/rooms/{id}
  - [ ] POST /api/consultations/rooms/{id}/end
  - [ ] PUT /api/consultations/rooms/{id}/memo
  - [ ] POST /api/consultations/rooms/{id}/extend

#### 테스트
- [ ] 상담방 목록 조회 테스트
- [ ] 상담 종료 테스트
- [ ] duration 계산 확인
- [ ] 메모 기능 테스트

---

### 8.5 Phase 4: WebSocket & 채팅

#### Config
- [ ] WebSocketConfig.java
  - [ ] @EnableWebSocketMessageBroker
  - [ ] registerStompEndpoints()
  - [ ] configureMessageBroker()
  - [ ] JWT 인증 Interceptor

#### Service
- [ ] ChatMessageService.java
  - [ ] saveMessage()
  - [ ] getMessageHistory()
  - [ ] markAsRead()
  - [ ] updateLastActivity()

#### Controller
- [ ] ChatController.java
  - [ ] @MessageMapping("/chat.send")
  - [ ] @SendTo("/topic/chat/{roomIdx}")
  - [ ] WebSocket 연결/해제 핸들러

#### REST API
- [ ] POST /api/chat/messages/history
- [ ] POST /api/chat/messages/{id}/read

#### 테스트
- [ ] WebSocket 연결 테스트
- [ ] 메시지 송수신 테스트
- [ ] JWT 인증 테스트
- [ ] 읽음 처리 테스트

---

### 8.6 Phase 5: 인앱 알림 시스템

#### Service
- [ ] NotificationService.java
  - [ ] createNotification()
  - [ ] getNotifications()
  - [ ] getUnreadNotifications()
  - [ ] getUnreadCount()
  - [ ] markAsRead()
  - [ ] markAllAsRead()
  - [ ] deleteNotification()
  - [ ] sendFcmPush() (FCM 연동)

#### Controller
- [ ] NotificationController.java
  - [ ] GET /api/notifications
  - [ ] GET /api/notifications/unread
  - [ ] GET /api/notifications/count
  - [ ] POST /api/notifications/{id}/read
  - [ ] POST /api/notifications/read-all
  - [ ] DELETE /api/notifications/{id}

#### 테스트
- [ ] 알림 생성 테스트
- [ ] FCM 푸시 전송 확인
- [ ] 알림 목록 조회 테스트
- [ ] 읽음 처리 테스트

---

### 8.7 Phase 6: 자동 종료 스케줄러

#### Scheduler
- [ ] ConsultationAutoCloseScheduler.java
  - [ ] @Scheduled checkInactiveRooms()
  - [ ] @Scheduled check24HourLimit()
  - [ ] send5MinWarning()
  - [ ] send10MinWarning()
  - [ ] autoCloseRoom()

#### Application
- [ ] BlueCrabApplication.java
  - [ ] @EnableScheduling 추가

#### 테스트
- [ ] 스케줄러 실행 확인
- [ ] 2시간 비활성 자동 종료 테스트
- [ ] 24시간 제한 테스트
- [ ] 경고 알림 전송 확인

---

### 8.8 Phase 7: 상담 이력 시스템

#### Service
- [ ] ConsultationHistoryService.java (또는 ConsultationRoomService에 통합)
  - [ ] getHistory()
  - [ ] getHistoryDetail()
  - [ ] getStats()
  - [ ] exportToExcel()
  - [ ] exportToPdf()

#### API
- [ ] GET /api/consultations/history
- [ ] GET /api/consultations/history/{id}
- [ ] GET /api/consultations/stats
- [ ] GET /api/consultations/export

#### 테스트
- [ ] 이력 목록 조회 테스트
- [ ] 필터링 테스트
- [ ] 통계 계산 테스트
- [ ] Excel 내보내기 테스트

---

### 8.9 Phase 8: 통합 테스트 & 문서화

#### 통합 테스트
- [ ] 전체 시나리오 E2E 테스트
  - [ ] 학생이 상담 요청
  - [ ] 교수가 수락
  - [ ] 실시간 채팅
  - [ ] 읽음 처리
  - [ ] 상담 종료
  - [ ] 이력 조회

#### 문서화
- [ ] API 문서 작성 (Swagger/Postman)
- [ ] README 업데이트
- [ ] 개발자 가이드 작성

#### 보안 설정
- [ ] SecurityConfig 업데이트
- [ ] WebSocket 보안 설정 확인
- [ ] CORS 설정 확인

---

### 8.10 배포 준비

- [ ] application.properties 환경별 설정
- [ ] 로깅 설정
- [ ] 모니터링 설정 (선택)
- [ ] 성능 테스트 (선택)

---

## 9. 참고 자료

### 9.1 관련 문서
- [feature-list.md](../feature-list.md) - 요구사항 명세
- [backend-implementation-status.md](../claudedocs/backend-implementation-status.md) - 현재 구현 현황
- [backend-feature-audit.md](../backend-feature-audit.md) - 기존 감사 보고서

### 9.2 기존 참고 코드
- **시설 예약 시스템**: `entity/FacilityReservationTbl.java`, `service/FacilityReservationService.java`
- **열람실 시스템**: `entity/ReadingSeat.java`, `scheduler/ReservationScheduler.java`
- **FCM 알림**: `service/FirebasePushService.java`, `controller/FcmTokenController.java`
- **JWT 인증**: `security/JwtAuthenticationFilter.java`, `util/JwtUtil.java`

### 9.3 외부 참고 자료
- Spring Boot WebSocket: https://spring.io/guides/gs/messaging-stomp-websocket/
- STOMP Protocol: https://stomp.github.io/
- Firebase Admin SDK: https://firebase.google.com/docs/admin/setup

---

**작성자:** Claude AI 개발 어시스턴트
**작성일:** 2025-10-22
**문서 버전:** 1.0
**상태:** 초안 완성

---

**다음 단계:**
1. 이 계획서를 팀과 검토
2. Phase 1부터 순차적으로 구현 시작
3. 각 Phase 완료 후 체크리스트 업데이트
4. 이슈 발생 시 이 문서에 기록

**질문/피드백:**
- 구현 순서 변경이 필요한가?
- 예상 작업 시간이 적절한가?
- 추가 고려사항이 있는가?
