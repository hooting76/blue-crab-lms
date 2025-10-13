# 시설 예약 정책 개선 - Phase 1.4 완료 보고서

## 📋 개요
- **작업 일자**: 2025-10-13
- **작업 내용**: 시설별 예약 정책 상세화
  1. 최소 사전 예약 일수 정책 추가 (MIN_DAYS_IN_ADVANCE)
  2. 시설별 최대 예약 기간 적용 (MAX_DAYS_IN_ADVANCE 활용)
  3. 시설별 예약 시간 제한 적용

## 🎯 구현 내용

### 1. 데이터베이스 스키마 변경

#### 추가된 컬럼
```sql
ALTER TABLE FACILITY_POLICY_TBL 
ADD COLUMN MIN_DAYS_IN_ADVANCE INT NULL 
COMMENT '최소 사전 예약 일수: NULL=제한없음(즉시예약), 0=당일예약가능, 1=최소하루전, 3=최소3일전까지만예약가능';
```

#### 정책 예시
| 시설 | MIN_DAYS_IN_ADVANCE | 의미 |
|------|---------------------|------|
| 강당 | 3 | 최소 3일 전에 예약 필요 |
| 회의실 | 1 | 최소 1일 전에 예약 필요 |
| 스터디룸 | 0 | 당일 예약 가능 |
| 세미나실 | NULL | 즉시 예약 가능 (제한 없음) |

### 2. 엔티티 클래스 업데이트

**파일**: `FacilityPolicyTbl.java`

#### 추가된 필드
```java
@Column(name = "MIN_DAYS_IN_ADVANCE")
private Integer minDaysInAdvance;
```

#### 추가된 메서드
```java
/**
 * 최소 사전 예약 일수 - NULL이면 즉시 예약 가능 (0일)
 * 예: 3이면 최소 3일 전까지만 예약 가능 (당일/1일전/2일전 불가)
 */
public int getEffectiveMinDaysInAdvance() {
    return minDaysInAdvance != null ? minDaysInAdvance : 0;
}
```

### 3. Service 로직 구현

**파일**: `FacilityReservationService.java`

#### 새로운 검증 메서드 추가
```java
private void validateReservationPolicy(FacilityPolicyTbl policy, 
                                      LocalDateTime startTime, 
                                      LocalDateTime endTime)
```

#### 검증 항목
1. **최소 사전 예약 일수 검증**
   ```java
   int minDaysInAdvance = policy.getEffectiveMinDaysInAdvance();
   if (minDaysInAdvance > 0) {
       LocalDateTime minDate = now.plusDays(minDaysInAdvance);
       if (startTime.isBefore(minDate)) {
           throw new RuntimeException(
               String.format("이 시설은 최소 %d일 전에 예약해야 합니다.", minDaysInAdvance));
       }
   }
   ```

2. **최대 사전 예약 일수 검증 (시설별)**
   ```java
   int maxDaysInAdvance = policy.getEffectiveMaxDaysInAdvance();
   LocalDateTime maxDate = now.plusDays(maxDaysInAdvance);
   if (startTime.isAfter(maxDate)) {
       throw new RuntimeException(
           String.format("이 시설은 최대 %d일 이내만 예약 가능합니다.", maxDaysInAdvance));
   }
   ```

3. **예약 시간 제한 검증 (시설별)**
   - 최소 예약 시간 (분): `policy.getEffectiveMinDurationMinutes()`
   - 최대 예약 시간 (분): `policy.getEffectiveMaxDurationMinutes()`

#### 적용 지점
- ✅ **예약 생성** (`createReservation`)
- ✅ **가용성 확인** (`checkAvailability`)

### 4. Enum 수정

**파일**: `FacilityType.java`

#### 추가된 타입
```java
MEETING_ROOM("미팅룸")  // DB의 MEETING_ROOM 타입 지원
```

## 📊 정책 적용 예시

### 시나리오 1: 강당 (3일 전 예약 필수)
```
현재: 2025-10-13
설정: MIN_DAYS_IN_ADVANCE = 3

❌ 2025-10-13 예약 시도 → "최소 3일 전에 예약해야 합니다"
❌ 2025-10-14 예약 시도 → "최소 3일 전에 예약해야 합니다"
❌ 2025-10-15 예약 시도 → "최소 3일 전에 예약해야 합니다"
✅ 2025-10-16 이후 예약 가능
```

### 시나리오 2: 회의실 (1일 전 예약 필수)
```
현재: 2025-10-13
설정: MIN_DAYS_IN_ADVANCE = 1

❌ 2025-10-13 예약 시도 → "최소 1일 전에 예약해야 합니다"
✅ 2025-10-14 이후 예약 가능
```

### 시나리오 3: 스터디룸 (당일 예약 가능)
```
현재: 2025-10-13
설정: MIN_DAYS_IN_ADVANCE = 0

✅ 2025-10-13 당일 예약 가능
✅ 이후 날짜도 모두 가능
```

### 시나리오 4: 세미나실 (즉시 예약)
```
현재: 2025-10-13 14:00
설정: MIN_DAYS_IN_ADVANCE = NULL

✅ 2025-10-13 15:00 즉시 예약 가능
✅ 현재 시간 이후면 언제든 가능
```

## 🔧 시설별 정책 조합 예시

| 시설명 | MIN_DAYS | MAX_DAYS | MIN_DURATION | MAX_DURATION | 설명 |
|--------|----------|----------|--------------|--------------|------|
| 강당 | 3일 | 30일 | 60분 | 480분 | 대형 행사, 사전 준비 필요 |
| 회의실 | 1일 | 14일 | 30분 | 240분 | 업무용, 하루 전 계획 |
| 스터디룸 | 0일 | 7일 | 30분 | 180분 | 학습용, 당일 가능 |
| 세미나실 | NULL | 60일 | 60분 | 360분 | 교육용, 즉시 예약 |
| 실습실 | 2일 | 21일 | 90분 | 300분 | 실습 준비, 2일 전 |

## 🎯 기능 동작 흐름

```
1. 사용자가 예약 요청
   ↓
2. Controller: 기본 검증 (과거 날짜, 시작/종료 시간)
   ↓
3. Service: 시설별 정책 조회
   ↓
4. validateReservationPolicy() 호출
   ├─ 최소 사전 예약 일수 확인
   ├─ 최대 사전 예약 일수 확인
   └─ 예약 시간 제한 확인
   ↓
5. 차단 기간 확인
   ↓
6. 예약 충돌 확인
   ↓
7. 예약 생성 (PENDING 또는 APPROVED)
```

## 📝 테스트 방법

### 1. DB 마이그레이션 실행
```sql
-- SSH로 DB 접속 후
source /path/to/migrations/20251013_add_min_days_in_advance.sql;
```

### 2. 테스트 데이터 설정
```sql
-- 강당: 3일 전 예약
UPDATE FACILITY_POLICY_TBL 
SET MIN_DAYS_IN_ADVANCE = 3 
WHERE FACILITY_IDX = (SELECT FACILITY_IDX FROM FACILITY_TBL WHERE FACILITY_NAME = '강당');

-- 회의실: 1일 전 예약
UPDATE FACILITY_POLICY_TBL 
SET MIN_DAYS_IN_ADVANCE = 1 
WHERE FACILITY_IDX = (SELECT FACILITY_IDX FROM FACILITY_TBL WHERE FACILITY_NAME LIKE '%회의실%');

-- 스터디룸: 당일 예약 가능
UPDATE FACILITY_POLICY_TBL 
SET MIN_DAYS_IN_ADVANCE = 0 
WHERE FACILITY_IDX = (SELECT FACILITY_IDX FROM FACILITY_TBL WHERE FACILITY_NAME LIKE '%스터디%');
```

### 3. API 테스트
```bash
# 1. 당일 예약 시도 (강당 - 3일 전 필수)
POST /api/facilities/1/availability
{
  "startTime": "2025-10-13 15:00:00",
  "endTime": "2025-10-13 17:00:00"
}
# 예상 결과: "이 시설은 최소 3일 전에 예약해야 합니다"

# 2. 4일 후 예약 시도 (통과)
POST /api/facilities/1/availability
{
  "startTime": "2025-10-17 15:00:00",
  "endTime": "2025-10-17 17:00:00"
}
# 예상 결과: isAvailable: true
```

## 🚀 배포 체크리스트

- [x] DB 마이그레이션 SQL 작성
- [x] Entity 클래스 업데이트
- [x] Service 로직 구현
- [x] Controller 연동
- [x] Enum 수정 (MEETING_ROOM 추가)
- [ ] DB 마이그레이션 실행 (SSH)
- [ ] 서버 재시작
- [ ] API 테스트
- [ ] 시설별 정책 설정

## 📌 주의사항

1. **DB 마이그레이션 필수**
   - `MIN_DAYS_IN_ADVANCE` 컬럼이 없으면 서버 시작 실패

2. **기존 시설 정책**
   - NULL 값은 즉시 예약 가능을 의미
   - 기존 동작을 유지하려면 NULL로 유지

3. **정책 충돌 방지**
   - MIN_DAYS_IN_ADVANCE ≤ MAX_DAYS_IN_ADVANCE 확인 필요

4. **에러 메시지**
   - 사용자 친화적인 메시지로 설정됨
   - 예약 가능 시작일도 함께 안내

## 🔗 관련 파일

```
backend/BlueCrab/src/main/java/BlueCrab/com/example/
├── entity/
│   └── FacilityPolicyTbl.java          (수정)
├── service/
│   └── FacilityReservationService.java (수정)
├── controller/
│   └── FacilityController.java         (수정)
└── enums/
    └── FacilityType.java               (수정)

claudedocs/database/migrations/
└── 20251013_add_min_days_in_advance.sql (신규)
```

## 📈 개선 효과

1. **시설별 맞춤 정책**
   - 시설 특성에 맞는 예약 제한 가능
   - 대형 시설은 사전 준비 시간 확보

2. **관리 편의성**
   - DB에서 직접 정책 조정 가능
   - 코드 수정 없이 정책 변경

3. **사용자 경험**
   - 명확한 예약 가능 기준 제시
   - 예약 실패 시 구체적인 안내

## ✅ 완료 상태

- **Phase 1.1**: 2-row 레이아웃 ✅
- **Phase 1.2**: Body 템플릿 배지 업데이트 ✅
- **Phase 1.3**: Auth Tab Navigation ✅
- **Phase 1.4**: 시설별 예약 정책 상세화 ✅
  - ✅ MIN_DAYS_IN_ADVANCE 추가
  - ✅ 시설별 MAX_DAYS_IN_ADVANCE 적용
  - ✅ 시설별 예약 시간 제한 적용
  - ✅ MEETING_ROOM 타입 추가

---

**다음 단계**: DB 마이그레이션 실행 및 테스트
