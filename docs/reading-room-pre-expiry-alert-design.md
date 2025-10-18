# Reading Room Pre-Expiry Alert (15-Minute Notification)

## 1. 목표
- 열람실 좌석 사용자에게 퇴실 예정 15분 전에 Data-only FCM 알림을 발송한다.
- 기존 FCM 인프라(`FcmTokenService`, `FirebasePushService`)를 재사용한다.
- 중복/누락 없이 한 번만 발송하도록 안전 장치를 둔다.
- 서비스 성능에 영향을 주지 않는 경량 주기 작업으로 구성한다.

## 2. 동작 조건
- 좌석 상태: `ReadingSeat.isOccupied = 1` (사용 중).
- 좌석 종료 예정 시각: `end_time`이 현재 시각 + 15분 전후(±1분 버퍼) 구간에 포함.
- 동일 사용 기록(`ReadingUsageLog`)에 대해 사전 알림이 아직 발송되지 않음.
- 학번 기준으로 토큰 조회가 가능해야 한다.

## 3. 데이터 모델 변경
| 테이블 | 컬럼 | 타입 | 설명 |
|--------|------|------|------|
| `LAMP_USAGE_LOG` | `pre_notice_sent_at` | `datetime null` | 사전 알림 발송 시각 기록 (null이면 미발송) |
| `LAMP_USAGE_LOG` | `pre_notice_token_count` | `int null` | 발송 당시 성공적으로 수집된 토큰 수 (모니터링용) |

- `ReadingUsageLog` 엔티티에 위 컬럼을 매핑하는 필드를 추가한다.
- 좌석 사용 종료나 해제 시에는 컬럼을 건드릴 필요 없다(이력 유지 목적).

## 4. 주요 컴포넌트
### 4.1 Scheduler (Spring @Scheduled)
- 클래스: `ReadingRoomPreExpiryNotifier`
- 주기: 1분마다 실행 (`cron = "0 * * * * *"` 권장).
- 동작:
  1. 현재 시각 `now` 계산.
  2. `targetWindowStart = now.plusMinutes(14)` / `targetWindowEnd = now.plusMinutes(16)`.
  3. `ReadingSeatRepository`를 통해 `isOccupied = 1`이고 `end_time`이 위 구간에 있는 좌석 목록 조회.
  4. 각 좌석에 대해 연결된 Active Usage Log( `end_time IS NULL` )를 조회.
  5. Usage Log에 `pre_notice_sent_at`이 비어 있어야 발송 대상.

### 4.2 Repository 확장
- `ReadingSeatRepository`
  - `@Lock(LockModeType.PESSIMISTIC_READ)` 옵션의 쿼리 메서드 추가:
    ```java
    List<ReadingSeat> findActiveSeatsEndingBetween(LocalDateTime start, LocalDateTime end);
    ```
- `ReadingUsageLogRepository`
  - `Optional<ReadingUsageLog> findBySeatNumberAndEndTimeIsNull(Integer seatNumber);` (이미 존재하면 재사용).
  - `@Modifying` 업데이트 메서드 추가: `markPreNoticeSent(Long logId, LocalDateTime sentAt, int tokenCount)`.

### 4.3 Notification Builder
- 구성 요소: `ReadingRoomNotificationFactory`
  - Input: 좌석 번호, 종료 예정 시각, 남은 시간, 사용자 학번, 사용자 이메일(선택).
  - Output: `FcmDataOnlySendRequest`.
  - Data Payload 예시:
    ```json
    {
      "type": "reading_room_pre_expiry",
      "seatNumber": 42,
      "minutesLeft": 15,
      "endTime": "2025-10-17T18:00:00",
      "deeplink": "/reading-room"
    }
    ```

### 4.4 Notification Orchestrator
- `ReadingRoomPreExpiryNotifier` 내부 로직:
  1. `userCode` 리스트 구성.
  2. `FcmTokenService.collectTokensByUserCodes(userCodes, platforms=ALL, includeTemporary=false)` 호출.
  3. 토큰 결과가 없으면 Usage Log에 알림 미발송 사유를 debug 로그로 기록 후 skip.
  4. 토큰이 존재하면 `FirebasePushService.sendDataOnlyBatch(request)` 실행.
  5. 성공 시 Usage Log에 `pre_notice_sent_at`, `pre_notice_token_count` 업데이트.
  6. 예외 발생 시 catch & retry 정책: 기본적으로 로그에 남기고 다음 주기에 다시 탐지되도록 한다 (단, 무한 반복 방지를 위해 `pre_notice_sent_at` 업데이트 전까지 동일 데이터 재시도 가능).

## 5. 예외 처리 & 경합 제어
- **동시 실행 보호**: Scheduler가 중복 실행되지 않도록 `@SchedulerLock` (ShedLock 등) 도입 고려. 단일 인스턴스면 필수 아님.
- **예약 조작 동시성**: 승인/퇴실 등의 트랜잭션과 충돌을 피하기 위해 Seat/Usage Log 조회 시 비관적 읽기 잠금 사용.
- **토큰 미등록**: `pre_notice_token_count = 0`으로 기록하고 WARN 로그 남긴다. 클라이언트 토큰 등록 UX 개선 지표로 활용.
- **FCM 오류**: `UNREGISTERED` 등 오류 발생 시 `FcmTokenService`에 토큰 정리 요청(기존 데이터-only 로직 참고) 후 WARN 로그. Usage Log에는 발송 실패 상태를 표기하기 위해 별도 컬럼이 필요하면 `pre_notice_status`(ENUM) 추가 고려.

## 6. 테스트 전략
- **단위 테스트**: Notification Factory, Repository 메서드, Notifier 서비스.
- **통합 테스트**: H2 DB 기반으로 가상 좌석/사용 기록을 생성하고 @Scheduled 메서드 수동 호출.
- **엔드투엔드**: 실제 서버에서 가속 모드(끝나는 시간을 현재+16분 등)로 좌석 예약 후 대기. `bluecrab-app.log`에서 `ReadingRoomPreExpiryNotifier` 로그 확인 및 단말 FCM 수신 검증.
- **회귀 테스트**: 기존 좌석 자동 만료(`cleanupExpiredSeats`)와 충돌하지 않는지 확인.

## 7. 배포 체크리스트
1. DB 스키마 변경 (`ALTER TABLE LAMP_USAGE_LOG ADD COLUMN ...`).
2. `ReadingUsageLog` 엔티티/Repository 업데이트.
3. Scheduler + Factory + Service 코드 추가.
4. `application.yml`에 Scheduler 활성화 및 필요 시 ShedLock 설정.
5. 운영 서버에 배포 후 로그 모니터링 (`ReadingRoomPreExpiryNotifier`, `FirebasePushService`).
6. 1주일간 정상 수신 여부 모니터링, 토큰 수집 통계 검토.

## 8. 향후 확장
- 10분/5분 전 추가 알림도 같은 구조로 확장 가능 (window 규칙만 추가).
- 알림 발송 결과를 사용자 알림 히스토리 테이블에 기록하여 MyPage에서 확인할 수 있게 제공.
- 좌석 자동 연장 기능 도입 시, 동일 노티를 활용해 연장 CTA 제공.
