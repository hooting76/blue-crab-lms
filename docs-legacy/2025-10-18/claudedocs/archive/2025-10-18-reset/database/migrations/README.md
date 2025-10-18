# 데이터베이스 마이그레이션 가이드

## 📋 마이그레이션 목록

| # | 파일명 | 설명 | 실행일 | 상태 |
|---|--------|------|--------|------|
| 001 | [001_create_facility_policy_table.sql](001_create_facility_policy_table.sql) | 시설 정책 테이블 분리 | 미실행 | 대기 |
| 001-R | [001_rollback.sql](001_rollback.sql) | 마이그레이션 001 롤백 | - | 대기 |
| 002 | [002_validation_queries.sql](002_validation_queries.sql) | 데이터 검증 쿼리 | - | - |

---

## 🎯 001: 시설 정책 테이블 분리

### 목적
`FACILITY_TBL`의 `REQUIRES_APPROVAL` 컬럼과 신규 정책 필드를 별도의 `FACILITY_POLICY_TBL`로 분리하여 확장 가능한 구조 구축

### 주요 변경사항

#### Before (변경 전)
```sql
FACILITY_TBL
├─ FACILITY_IDX (PK)
├─ FACILITY_NAME
├─ FACILITY_TYPE
├─ REQUIRES_APPROVAL  ← 정책 관련
└─ ...
```

#### After (변경 후)
```sql
FACILITY_TBL (시설 정보)              FACILITY_POLICY_TBL (정책)
├─ FACILITY_IDX (PK)                  ├─ POLICY_IDX (PK)
├─ FACILITY_NAME                      ├─ FACILITY_IDX (FK, UNIQUE)
├─ FACILITY_TYPE                      ├─ REQUIRES_APPROVAL (이동)
└─ ...                                ├─ MIN_DURATION_MINUTES (신규)
                                      ├─ MAX_DURATION_MINUTES (신규)
                                      ├─ MAX_DAYS_IN_ADVANCE (신규)
                                      └─ ... (확장 가능)
```

---

## 🚀 실행 가이드

### 사전 준비

1. **DB 백업**
   ```bash
   mysqldump -u [user] -p blue_crab > backup_$(date +%Y%m%d_%H%M%S).sql
   ```

2. **접속 정보 확인**
   - 호스트: `121.165.24.26:55511`
   - 데이터베이스: `blue_crab`
   - 권한: ALTER, CREATE, DROP 필요

3. **애플리케이션 중단 (권장)**
   ```bash
   # 프로덕션 환경이라면 서비스 중단 필요
   systemctl stop blue-crab-backend
   ```

---

### 실행 순서

#### Step 1: 마이그레이션 전 검증
```bash
mysql -h 121.165.24.26 -P 55511 -u [user] -p blue_crab < 002_validation_queries.sql > pre_migration_report.txt
```

**확인 사항**:
- ✅ 현재 시설 수 확인
- ✅ REQUIRES_APPROVAL 분포 확인
- ✅ 백업 준비 완료

#### Step 2: 마이그레이션 실행
```bash
mysql -h 121.165.24.26 -P 55511 -u [user] -p blue_crab < 001_create_facility_policy_table.sql
```

**실행 내용**:
1. `FACILITY_TBL_BACKUP` 백업 테이블 생성
2. `FACILITY_POLICY_TBL` 테이블 생성
3. `REQUIRES_APPROVAL` 데이터 복사
4. 데이터 검증 (4단계)
5. `FACILITY_TBL`에서 `REQUIRES_APPROVAL` 컬럼 삭제
6. 최종 확인

#### Step 3: 마이그레이션 후 검증
```bash
mysql -h 121.165.24.26 -P 55511 -u [user] -p blue_crab < 002_validation_queries.sql > post_migration_report.txt
```

**확인 사항**:
- ✅ `FACILITY_POLICY_TBL` 생성됨
- ✅ 모든 시설에 정책 존재
- ✅ `REQUIRES_APPROVAL` 값 일치
- ✅ 외래키 제약조건 설정됨
- ✅ 인덱스 생성됨

#### Step 4: 애플리케이션 테스트
```bash
# 백엔드 재시작
systemctl start blue-crab-backend

# API 테스트
curl -X POST http://localhost:8080/api/facilities
curl -X POST http://localhost:8080/api/reservations -d '{...}'
```

**테스트 항목**:
- ✅ 시설 목록 조회
- ✅ 시설 상세 조회
- ✅ 예약 생성 (자동 승인)
- ✅ 예약 생성 (수동 승인)
- ✅ 가용성 확인

---

### 롤백 (문제 발생 시)

```bash
# 즉시 롤백
mysql -h 121.165.24.26 -P 55511 -u [user] -p blue_crab < 001_rollback.sql
```

**롤백 내용**:
1. `FACILITY_TBL`에 `REQUIRES_APPROVAL` 컬럼 복원
2. `FACILITY_POLICY_TBL`에서 데이터 복사
3. 데이터 검증
4. `FACILITY_POLICY_TBL` 삭제

---

## 📊 마이그레이션 체크리스트

### 실행 전
- [ ] DB 백업 완료
- [ ] 접속 권한 확인
- [ ] 현재 데이터 상태 확인
- [ ] 애플리케이션 중단 (프로덕션)
- [ ] 팀원 공지

### 실행 중
- [ ] STEP 1: 백업 테이블 생성 ✓
- [ ] STEP 2: 정책 테이블 생성 ✓
- [ ] STEP 3: 데이터 복사 ✓
- [ ] STEP 4: 검증 1 - 행 수 일치 ✓
- [ ] STEP 4: 검증 2 - 값 일치 ✓
- [ ] STEP 4: 검증 3 - 누락 없음 ✓
- [ ] STEP 4: 검증 4 - 중복 없음 ✓
- [ ] STEP 5: 컬럼 삭제 ✓
- [ ] STEP 6: 최종 확인 ✓

### 실행 후
- [ ] 정책 테이블 존재 확인
- [ ] 외래키 제약조건 확인
- [ ] 인덱스 생성 확인
- [ ] API 정상 동작 확인
- [ ] 성능 이슈 없음 확인
- [ ] 로그 에러 없음 확인

---

## ⚠️ 주의사항

### 1. 실행 순서 엄수
마이그레이션 SQL의 각 STEP은 **순차적으로 실행**되어야 합니다. 중간에 실패하면 롤백 스크립트 실행 필요.

### 2. 검증 필수
각 검증 단계에서 **FAIL**이 나오면 **즉시 중단**하고 원인 파악 필요.

### 3. 백업 보관
`FACILITY_TBL_BACKUP` 테이블은 **최소 1주일 보관** 권장.

### 4. 롤백 시점
다음 상황에서는 즉시 롤백:
- 데이터 검증 실패
- 애플리케이션 에러 발생
- 성능 저하 심각
- 외래키 제약조건 오류

### 5. 프로덕션 실행 시
- **스테이징 환경 먼저 테스트**
- **업무 시간 외 실행**
- **롤백 가능한 시간 확보** (최소 1시간)
- **담당자 대기**

---

## 📝 문제 해결

### Q1. "Table already exists" 에러
```sql
-- 이전 마이그레이션 정리
DROP TABLE IF EXISTS FACILITY_POLICY_TBL;
DROP TABLE IF EXISTS FACILITY_TBL_BACKUP;
```

### Q2. 외래키 제약조건 실패
```sql
-- 정책이 없는 시설 확인
SELECT f.FACILITY_IDX, f.FACILITY_NAME
FROM FACILITY_TBL f
LEFT JOIN FACILITY_POLICY_TBL p ON f.FACILITY_IDX = p.FACILITY_IDX
WHERE p.POLICY_IDX IS NULL;

-- 정책 수동 추가
INSERT INTO FACILITY_POLICY_TBL (FACILITY_IDX, REQUIRES_APPROVAL)
VALUES (?, 1);
```

### Q3. 데이터 불일치
```sql
-- 불일치 데이터 확인
SELECT
    f.FACILITY_IDX,
    f.FACILITY_NAME,
    f.REQUIRES_APPROVAL AS old_value,
    p.REQUIRES_APPROVAL AS new_value
FROM FACILITY_TBL f
INNER JOIN FACILITY_POLICY_TBL p ON f.FACILITY_IDX = p.FACILITY_IDX
WHERE f.REQUIRES_APPROVAL != p.REQUIRES_APPROVAL;

-- 수동 동기화
UPDATE FACILITY_POLICY_TBL p
INNER JOIN FACILITY_TBL f ON p.FACILITY_IDX = f.FACILITY_IDX
SET p.REQUIRES_APPROVAL = f.REQUIRES_APPROVAL;
```

### Q4. 롤백 후 애플리케이션 에러
```bash
# 백엔드 재시작
systemctl restart blue-crab-backend

# 캐시 삭제 (Redis 사용 시)
redis-cli FLUSHDB
```

---

## 📚 관련 문서

- [데이터베이스 스키마](../database-schema.md)
- [시설 예약 시스템 분석](../../feature-docs/facility-reservation/README.md)
- [백엔드 가이드](../../backend-guide/README.md)

---

## 📞 지원

문제 발생 시:
1. 롤백 스크립트 실행
2. 에러 로그 수집
3. 검증 리포트 확인
4. 개발팀 연락

---

**작성일**: 2025-10-10
**작성자**: Development Team
**버전**: 1.0
