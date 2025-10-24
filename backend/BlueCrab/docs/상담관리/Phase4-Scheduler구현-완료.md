# Phase 4: Scheduler 구현 완료 보고서

**작업일**: 2025-10-24
**담당자**: BlueCrab Development Team
**상태**: ✅ 완료

---

## 📋 작업 개요

상담 요청/진행 관리 시스템의 스케줄러 기능을 구현했습니다.

### 주요 산출물

1. **ConsultationAutoCloseScheduler** - 상담 자동 종료 처리
2. **ChatBackupScheduler** - Redis 메시지 MinIO 백업 (Phase 5 구현 예정)
3. **OrphanedRoomCleanupScheduler** - 고아 데이터 정리 (Phase 5 구현 예정)

---

## 🎯 구현 완료 항목

### 1. ConsultationAutoCloseScheduler (완전 구현)

**파일**: [ConsultationAutoCloseScheduler.java](../../src/main/java/BlueCrab/com/example/scheduler/ConsultationAutoCloseScheduler.java)

#### 스케줄러 메서드 (3개)

| 메서드 | 실행 주기 | Cron 표현식 | 설명 |
|--------|----------|------------|------|
| `autoEndInactiveConsultations()` | 매 1시간 (정각) | `0 0 * * * *` | 2시간 비활성 상담 자동 종료 |
| `autoEndLongRunningConsultations()` | 매일 오전 5시 | `0 0 5 * * *` | 24시간 초과 상담 강제 종료 |
| `sendAutoEndWarnings()` | 매 10분 | `0 */10 * * * *` | 종료 예정 경고 알림 (Phase 5 구현) |

#### 자동 종료 로직

**비활성 상담 종료 (2시간 기준)**
```java
@Scheduled(cron = "0 0 * * * *")
public void autoEndInactiveConsultations() {
    log.info("[스케줄러] 비활성 상담 자동 종료 시작");
    
    try {
        // Service Layer 호출
        int count = consultationRequestService.autoEndInactiveConsultations();
        
        if (count > 0) {
            log.info("[스케줄러] 비활성 상담 자동 종료 완료: {}건", count);
        } else {
            log.debug("[스케줄러] 자동 종료 대상 없음 (비활성)");
        }
    } catch (Exception e) {
        log.error("[스케줄러] 비활성 상담 자동 종료 실패", e);
    }
}
```

**종료 조건:**
- `consultation_status = 'IN_PROGRESS'`
- `last_activity_at < NOW() - INTERVAL 2 HOUR`

**종료 처리 절차:**
1. DB에서 조건에 맞는 상담 조회 (`findInactiveConsultations`)
2. `consultation_status`를 `COMPLETED`로 변경
3. `ended_at` 기록 (현재 시각)
4. `duration_minutes` 자동 계산
5. Redis 메시지 조회 후 MinIO archive/에 저장 (Phase 5)
6. Redis 키 삭제 (Phase 5)

---

**장시간 실행 상담 종료 (24시간 제한)**
```java
@Scheduled(cron = "0 0 5 * * *")
public void autoEndLongRunningConsultations() {
    log.info("[스케줄러] 장시간 실행 상담 자동 종료 시작");
    
    try {
        int count = consultationRequestService.autoEndLongRunningConsultations();
        
        if (count > 0) {
            log.warn("[스케줄러] 장시간 실행 상담 자동 종료 완료: {}건 (24시간 제한)", count);
        } else {
            log.debug("[스케줄러] 자동 종료 대상 없음 (장시간)");
        }
    } catch (Exception e) {
        log.error("[스케줄러] 장시간 실행 상담 자동 종료 실패", e);
    }
}
```

**종료 조건:**
- `consultation_status = 'IN_PROGRESS'`
- `started_at < NOW() - INTERVAL 24 HOUR`

**실행 시간 선택 이유:**
- 오전 5시: 서버 부하가 가장 낮은 시간대
- 사용자 접속이 거의 없는 새벽 시간
- DB 및 Redis 부하 최소화

---

**경고 알림 발송 (Phase 5 구현 예정)**
```java
@Scheduled(cron = "0 */10 * * * *")
public void sendAutoEndWarnings() {
    log.debug("[스케줄러] 상담 종료 경고 알림 체크 (미구현)");
    // TODO: Phase 5에서 FCM 푸시 알림 연동 시 구현
}
```

**경고 조건:**
- `consultation_status = 'IN_PROGRESS'`
- `started_at < NOW() - INTERVAL 23 HOUR 50 MINUTE`
- `started_at >= NOW() - INTERVAL 24 HOUR`

**알림 내용:**
- 제목: "상담 자동 종료 예정"
- 내용: "10분 후 상담이 자동으로 종료됩니다. 채팅 기록은 보존됩니다."
- 발송 대상: 학생 + 교수 (상담 참여자 모두)

---

### 2. ChatBackupScheduler (Phase 5 구현 예정)

**파일**: [ChatBackupScheduler.java](../../src/main/java/BlueCrab/com/example/scheduler/ChatBackupScheduler.java)

#### 백업 전략

**실행 주기:** 매 5분마다 (`0 */5 * * * *`)

**백업 대상:**
- `consultation_status = 'IN_PROGRESS'` 상태의 모든 상담
- Redis에 저장된 채팅 메시지 (Key: `chat:room:{requestIdx}`)

**백업 절차:**
1. DB에서 IN_PROGRESS 상담 목록 조회
2. 각 상담의 Redis 메시지 전체 조회 (`LRANGE chat:room:{requestIdx} 0 -1`)
3. 텍스트 파일 포맷팅 (타임스탬프, 발신자, 메시지 내용)
4. MinIO `consultations/temp/`에 업로드
   - 파일명: `chat_{requestIdx}_snapshot_{yyyyMMddHHmmss}.txt`
5. 동일 `requestIdx`의 이전 스냅샷 삭제

**백업 파일 예시:**
```
consultations/temp/chat_123_snapshot_20251024140530.txt
consultations/temp/chat_456_snapshot_20251024140535.txt
```

**정리 규칙:**
- 정상 종료 시: 해당 `requestIdx`의 모든 temp/ 스냅샷 삭제
- 고아 스냅샷: `OrphanedRoomCleanupScheduler`가 60시간 후 삭제

**데이터 유실 최소화:**
- Redis AOF (Append Only File) 활성화: 1초마다 fsync
- 5분 간격 MinIO 백업: 최대 5분 데이터 유실
- TTL 36시간: 스케줄러 실패 시 자동 정리

---

### 3. OrphanedRoomCleanupScheduler (Phase 5 구현 예정)

**파일**: [OrphanedRoomCleanupScheduler.java](../../src/main/java/BlueCrab/com/example/scheduler/OrphanedRoomCleanupScheduler.java)

#### 정리 전략

**실행 주기:** 매일 새벽 3시 (`0 0 3 * * *`)

**정리 대상:**
1. MinIO `consultations/temp/` 디렉토리의 60시간 이상 된 스냅샷
2. DB에 없는 고아 상담 기록

**정리 절차:**
1. MinIO `consultations/temp/` 파일 목록 조회
2. 파일명에서 타임스탬프 추출 (예: `20251024140530`)
3. 60시간 이상 경과한 파일 필터링
   - 60시간 = Redis TTL 36시간 + 안전 여유 24시간
4. 고아 파일 삭제
5. DB에 없는 상담 기록 처리
   - 최신 스냅샷을 `consultations/archive/`로 이동
   - temp/ 스냅샷 전체 삭제

**타임스탬프 임계값:**
```
60시간 = 36시간 (Redis TTL) + 24시간 (안전 여유)
```

**파일명 파싱 예시:**
```
chat_123_snapshot_20251024140530.txt
                  └─ yyyyMMddHHmmss (2025-10-24 14:05:30)
```

**고아 상담 복구:**
- MinIO temp/에 스냅샷이 있지만 DB에 종료 기록이 없는 경우
- 최신 스냅샷을 archive/로 복사
- DB에 강제 종료 기록 생성

---

## 📊 코드 통계

| 항목 | 값 |
|------|-----|
| 스케줄러 클래스 수 | 3개 |
| 스케줄 메서드 수 (총) | 4개 |
| - 완전 구현 | 2개 (비활성 종료, 장시간 종료) |
| - Phase 5 구현 예정 | 2개 (백업, 고아 정리) + 경고 알림 |
| Cron 표현식 | 4개 |
| Service 메서드 호출 | 2개 |

---

## 🕐 스케줄 실행 타임테이블

| 시간 | 스케줄러 | 메서드 | 작업 내용 |
|------|---------|--------|----------|
| 00:00 | ConsultationAutoClose | `autoEndInactiveConsultations()` | 2시간 비활성 종료 |
| 00:00, 00:05, ..., 23:55 | ChatBackup | `backupActiveConsultationChats()` | Redis → MinIO 백업 |
| 00:00, 00:10, ..., 23:50 | ConsultationAutoClose | `sendAutoEndWarnings()` | 경고 알림 (미구현) |
| 01:00 ~ 23:00 (매시) | ConsultationAutoClose | `autoEndInactiveConsultations()` | 2시간 비활성 종료 |
| 03:00 | OrphanedRoomCleanup | `cleanupOrphanedSnapshots()` | 60시간 이상 스냅샷 삭제 |
| 05:00 | ConsultationAutoClose | `autoEndLongRunningConsultations()` | 24시간 초과 강제 종료 |

---

## 💡 구현 특징

### 1. 예외 처리

```java
try {
    int count = consultationRequestService.autoEndInactiveConsultations();
    
    if (count > 0) {
        log.info("[스케줄러] 비활성 상담 자동 종료 완료: {}건", count);
    } else {
        log.debug("[스케줄러] 자동 종료 대상 없음 (비활성)");
    }
} catch (Exception e) {
    log.error("[스케줄러] 비활성 상담 자동 종료 실패", e);
}
```

**특징:**
- 예외 발생 시에도 스케줄러 중단되지 않음
- 에러 로깅으로 모니터링 가능
- 다음 실행 주기에 재시도

### 2. 로깅 전략

| 로그 레벨 | 사용 시점 |
|----------|-----------|
| `INFO` | 스케줄러 시작, 종료 건수 기록 |
| `WARN` | 장시간 실행 상담 강제 종료 (주의 필요) |
| `DEBUG` | 처리 대상 없음, 미구현 기능 실행 |
| `ERROR` | 예외 발생, 실패 케이스 |

**로그 메시지 예시:**
```
[INFO ] [스케줄러] 비활성 상담 자동 종료 시작
[INFO ] [스케줄러] 비활성 상담 자동 종료 완료: 3건
[WARN ] [스케줄러] 장시간 실행 상담 자동 종료 완료: 1건 (24시간 제한)
[DEBUG] [스케줄러] 자동 종료 대상 없음 (비활성)
[ERROR] [스케줄러] 비활성 상담 자동 종료 실패
```

### 3. 구현 단계별 분리

**Phase 4 (현재):**
- ✅ 스케줄러 구조 및 메서드 정의
- ✅ Service Layer 메서드 호출
- ✅ DB 기반 자동 종료 처리
- ✅ 로깅 및 예외 처리

**Phase 5 (예정):**
- ⏳ Redis 메시지 조회 및 삭제
- ⏳ MinIO 파일 업로드/다운로드/삭제
- ⏳ ChatService 구현
- ⏳ FCM 푸시 알림 연동

---

## 🧪 테스트 시나리오

### 1. 비활성 상담 자동 종료 테스트

**준비:**
1. IN_PROGRESS 상담 생성
2. `last_activity_at`을 2시간 1분 전으로 설정 (수동)
   ```sql
   UPDATE CONSULTATION_REQUEST_TBL
   SET last_activity_at = DATE_SUB(NOW(), INTERVAL 121 MINUTE)
   WHERE request_idx = 123;
   ```

**실행:**
- 스케줄러 대기 (다음 정각) 또는 수동 실행

**검증:**
1. `consultation_status`가 `COMPLETED`로 변경되었는지 확인
2. `ended_at`이 기록되었는지 확인
3. `duration_minutes`가 계산되었는지 확인
4. 로그에 "비활성 상담 자동 종료 완료: 1건" 출력 확인

---

### 2. 장시간 실행 상담 자동 종료 테스트

**준비:**
1. IN_PROGRESS 상담 생성
2. `started_at`을 24시간 1분 전으로 설정
   ```sql
   UPDATE CONSULTATION_REQUEST_TBL
   SET started_at = DATE_SUB(NOW(), INTERVAL 1441 MINUTE)
   WHERE request_idx = 456;
   ```

**실행:**
- 오전 5시 대기 또는 수동 실행

**검증:**
1. `consultation_status`가 `COMPLETED`로 변경
2. `ended_at` 기록
3. 로그에 "장시간 실행 상담 자동 종료 완료: 1건 (24시간 제한)" 출력

---

### 3. 스케줄러 예외 처리 테스트

**시나리오:**
- Service Layer에서 예외 발생 시 스케줄러 동작 확인

**준비:**
- Service 메서드에 고의로 예외 발생 코드 추가 (테스트용)

**검증:**
1. 예외 발생해도 스케줄러 중단되지 않음
2. 에러 로그 출력 확인
3. 다음 실행 주기에 정상 동작 확인

---

## 🔍 운영 모니터링

### 로그 모니터링

**정상 동작 확인:**
```bash
# 비활성 상담 종료 로그
grep "비활성 상담 자동 종료" logs/application.log

# 장시간 실행 상담 종료 로그
grep "장시간 실행 상담" logs/application.log

# 에러 로그
grep "ERROR.*스케줄러" logs/application.log
```

**Cron 표현식 검증:**
```java
// Spring Boot Actuator 활용
GET /actuator/scheduledtasks

// 응답 예시:
{
  "cron": [
    {
      "runnable": {
        "target": "ConsultationAutoCloseScheduler.autoEndInactiveConsultations"
      },
      "expression": "0 0 * * * *"
    },
    ...
  ]
}
```

---

## 📝 다음 단계: Phase 5

### WebSocket 및 채팅 기능 구현

**주요 작업:**

1. **ChatService 구현**
   - Redis 메시지 저장/조회/삭제
   - 메시지 포맷팅 (텍스트 파일 생성)
   - MinIO 업로드/다운로드

2. **WebSocketConfig 설정**
   - STOMP 엔드포인트 설정
   - 메시지 브로커 설정
   - JWT 인증 통합

3. **ChatController 구현**
   - `/app/chat.send` - 메시지 전송
   - 개인 큐 방식 구현 (`/user/queue/chat`)
   - 권한 검증 (참여자만 메시지 송수신)

4. **MinIO 통합**
   - `consultations/temp/` 백업 구현
   - `consultations/archive/` 영구 보관 구현
   - 파일 업로드/다운로드/삭제

5. **스케줄러 구현 완성**
   - `ChatBackupScheduler.backupActiveConsultationChats()` 구현
   - `OrphanedRoomCleanupScheduler.cleanupOrphanedSnapshots()` 구현
   - `ConsultationAutoCloseScheduler.sendAutoEndWarnings()` 구현 (FCM 푸시)

**예상 소요 시간:** 10-12시간

---

## ✅ 검증 완료 사항

### 컴파일 및 빌드
- [x] 모든 스케줄러 클래스 컴파일 성공
- [x] @EnableScheduling 설정 확인
- [x] 의존성 주입 검증

### 코드 품질
- [x] JavaDoc 주석 작성
- [x] 로깅 처리 (INFO, WARN, DEBUG, ERROR)
- [x] 예외 처리
- [x] Cron 표현식 검증

### 스케줄러 설정
- [x] Cron 표현식 올바름
- [x] 실행 주기 적절함
- [x] Service Layer 메서드 연동

### 구현 범위
- [x] Phase 4 범위 구현 완료
- [x] Phase 5 의존성 TODO 주석 처리
- [x] 단계별 구현 명확히 분리

---

## 📌 참고 사항

### Cron 표현식 가이드

| 표현식 | 의미 | 실행 예시 |
|--------|------|----------|
| `0 0 * * * *` | 매 시간 정각 | 00:00, 01:00, 02:00, ... |
| `0 */5 * * * *` | 매 5분 | 00:00, 00:05, 00:10, ... |
| `0 */10 * * * *` | 매 10분 | 00:00, 00:10, 00:20, ... |
| `0 0 5 * * *` | 매일 오전 5시 | 05:00 (매일) |
| `0 0 3 * * *` | 매일 오전 3시 | 03:00 (매일) |

**필드 순서:** 초 분 시 일 월 요일

### Service Layer 메서드

**ConsultationRequestService 인터페이스:**
```java
int autoEndInactiveConsultations();        // 2시간 비활성 상담 종료
int autoEndLongRunningConsultations();     // 24시간 초과 상담 종료
```

**반환값:**
- 자동 종료된 상담 건수 (int)
- 처리 대상 없으면 0 반환

---

## 📞 문의

**작성자**: BlueCrab Development Team
**작성일**: 2025-10-24
**버전**: 1.0.0
