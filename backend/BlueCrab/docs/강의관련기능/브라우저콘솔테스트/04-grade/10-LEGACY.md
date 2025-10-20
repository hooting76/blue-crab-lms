# 📜 레거시 정보

이전 버전 및 통합 파일에 대한 참고 정보입니다.

---

## 📁 레거시 파일

### `grade-management-test.js.backup`

**상태:** 백업 (참고용)  
**라인 수:** 744 lines  
**버전:** v3.0 통합 버전

#### 특징

- ✅ **올인원 파일**: 모든 기능이 한 파일에 통합
- ✅ **빠른 테스트**: 파일 1개만 로드
- ❌ **높은 결합도**: 유지보수 어려움
- ❌ **확장성 낮음**: 기능 추가 시 복잡도 증가

#### 용도

- 참고 자료
- 레거시 코드 비교
- 긴급 단일 파일 테스트

---

## 🆚 버전 비교

### v3.0 통합 버전 vs v4.0 모듈화 버전

| 항목 | v3.0 (통합) | v4.0 (모듈화) |
|------|-------------|---------------|
| **파일 수** | 1개 | 4개 |
| **총 라인 수** | 744 lines | 915 lines |
| **로드 방식** | 단일 로드 | 순차 로드 (01-04) |
| **의존성 관리** | 없음 | 명확 (체인 구조) |
| **테스트 방식** | 배치 실행 | 단계별 확인 |
| **유지보수성** | 낮음 | 높음 |
| **확장성** | 낮음 | 높음 |
| **디버깅** | 어려움 | 용이 |
| **학습 곡선** | 낮음 | 중간 |

---

## 📊 v3.0 통합 버전 정보

### 포함 기능

#### Phase 1 테스트 (5개)

1. **성적 구성 설정**
   - API: `POST /api/enrollments/grade-config`
   - 출석 만점, 과제 만점, 지각 페널티, 등급 분포 설정

2. **학생 성적 조회**
   - API: `GET /api/enrollments/{lecIdx}/{studentIdx}/grade`
   - 출석율 자동 계산: (출석 + 지각) / 80
   - 지각 페널티 적용
   - 과제 점수 집계, 총점 계산

3. **교수용 성적 조회**
   - API: `GET /api/enrollments/professor/grade`
   - 학생 성적 상세 정보
   - 통계: 평균, 최고점, 최저점, 순위
   - 등급 부여 권한

4. **전체 성적 목록**
   - API: `GET /api/enrollments/grade-list`
   - 성적순 정렬 (percentage/grade)
   - 페이징 지원 (page, size)
   - 정렬 방향 (asc/desc)

5. **최종 등급 배정**
   - API: `POST /api/enrollments/finalize-grades`
   - 60% 기준 합격/불합격
   - 상대평가 (A+~D)
   - 동점자 처리 (같은 점수 → 상위 등급)
   - 하위 침범 방식 등급 배정

#### Phase 3 테스트 (2개)

6. **출석 업데이트 + 이벤트**
   - API: `PUT /api/enrollments/{enrollmentIdx}/attendance`
   - 출석/지각/결석 기록
   - 자동 성적 재계산 이벤트 발행
   - 비동기 처리 (@Async)

7. **과제 채점 + 이벤트**
   - API: `PUT /api/assignments/{assignmentIdx}/grade`
   - 학생별 과제 채점
   - 자동 성적 재계산 이벤트 발행
   - 비동기 처리 (@Async)

---

## 🔄 마이그레이션 가이드

### v3.0 → v4.0 전환

#### 이전 방식 (v3.0)

```javascript
// 1개 파일 로드
// grade-management-test.js 복사 → 붙여넣기

// 전체 테스트 실행
await gradeTests.runAll()
```

#### 새로운 방식 (v4.0)

```javascript
// 4개 파일 순차 로드
// 01-grade-test-utils.js
// 02-grade-phase1-tests.js
// 03-grade-phase3-tests.js
// 04-grade-test-runner.js

// 전체 테스트 실행
await gradeTests.runAll()
```

#### 주요 변경 사항

1. **네임스페이스 변경 없음**
   - `gradeTests.runAll()` 동일
   - API 호출 방식 동일

2. **추가된 기능**
   - 단계별 테스트 (각 모듈 개별 실행)
   - 로드 시점 확인 (window 객체 체크)
   - 모듈별 문서화

3. **제거된 기능**
   - 없음 (모든 기능 유지)

---

## 🎯 언제 레거시 버전을 사용하나?

### 통합 버전 (v3.0) 사용 권장

- ✅ 긴급 테스트 (빠른 검증)
- ✅ 일회성 테스트
- ✅ 레거시 시스템 호환
- ✅ 단순 기능 확인

### 모듈화 버전 (v4.0) 사용 권장

- ✅ **정규 테스트** (Phase 4)
- ✅ **유지보수** (지속적 개선)
- ✅ **팀 협업** (역할 분담)
- ✅ **학습/교육** (단계별 이해)
- ✅ **기능 확장** (새 테스트 추가)

---

## 📋 v3.0 사용법 (참고)

### 로드

```javascript
// VSCode에서 파일 열기
// grade-management-test.js.backup

// 전체 복사 → 브라우저 콘솔 붙여넣기
```

### 실행

```javascript
// 전체 테스트
await gradeTests.runAll()

// 개별 테스트
await gradeTests.config()
await gradeTests.studentInfo()
// ...

// 시나리오 테스트
await gradeTests.scenario()
```

### 데이터 관리

```javascript
// 조회
gradeTests.getData()

// 수정
gradeTests.setData(2, 101, 22)

// 커스텀 테스트
await gradeTests.customTest(1, 100)
```

---

## 🔍 주요 개선 사항 (v3.0 → v4.0)

### 1. 모듈화
- **v3.0**: 744 lines 단일 파일
- **v4.0**: 4개 모듈 (183, 367, 142, 223 lines)

### 2. 의존성 관리
- **v3.0**: 암묵적 의존성
- **v4.0**: 명시적 체인 (01→02/03→04)

### 3. 테스트 방식
- **v3.0**: 배치 실행만
- **v4.0**: 단계별 확인 + 배치 실행

### 4. 디버깅
- **v3.0**: 전체 파일 재로드
- **v4.0**: 문제 모듈만 재로드

### 5. 문서화
- **v3.0**: 단일 README
- **v4.0**: 6개 문서 (역할별 분리)

### 6. 확장성
- **v3.0**: 파일 내 추가 → 복잡도 증가
- **v4.0**: 새 모듈 추가 → 독립적 확장

---

## 📚 참고 문서

### 현재 버전 (v4.0)

- [00-README.md](./00-README.md) - 목차 & 개요
- [05-QUICK-START.md](./05-QUICK-START.md) - 빠른 시작
- [06-USAGE-GUIDE.md](./06-USAGE-GUIDE.md) - 사용법 상세
- [07-TESTING-GUIDE.md](./07-TESTING-GUIDE.md) - Phase 4 가이드
- [08-MODULE-REFERENCE.md](./08-MODULE-REFERENCE.md) - 모듈 참조
- [09-TROUBLESHOOTING.md](./09-TROUBLESHOOTING.md) - 문제 해결

### 레거시 버전 (v3.0)

- `grade-management-test.js.backup` - 통합 파일 (삭제됨)
- 이전 README 섹션 (아래 참조)

---

## 📖 v3.0 README 전체 내용

<details>
<summary>클릭하여 펼치기</summary>

### 성적 관리 시스템 통합 테스트 (Phase 1-3 완료)

#### ✅ 주요 기능

1. **성적 구성 설정** (Phase 1)
2. **학생 성적 조회** (Phase 1)
3. **교수용 성적 조회** (Phase 1)
4. **전체 성적 목록** (Phase 1)
5. **최종 등급 배정** (Phase 1)
6. **출석 업데이트 + 이벤트** (Phase 3)
7. **과제 채점 + 이벤트** (Phase 3)

#### 🚀 사용 방법

```javascript
// 1. 교수로 로그인
await login()

// 2. 전체 테스트 실행
await gradeTests.runAll()

// 3. 개별 테스트
await gradeTests.config()
await gradeTests.studentInfo()
// ...

// 4. 시나리오 테스트
await gradeTests.scenario()

// 5. 커스텀 데이터
gradeTests.getData()
gradeTests.setData(2, 101, 22)
await gradeTests.customTest(1, 100)
```

#### 📊 예상 출력 (v2.0)

```
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
📊 학생 성적 조회 테스트
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
📤 요청 데이터: {lecIdx: 1, studentIdx: 100}
✅ /grade-info 성공 (125.34ms)
📝 메시지: 성적 조회가 완료되었습니다.

📊 성적 상세 정보:
  📅 출석: 18.5 / 80 (23.1%)
  📝 과제: 85.0 / 100 (85.0%)
  📈 총점: 103.5 (51.8%)

✅ 테스트 성공
```

#### 🎯 이벤트 기반 자동 계산

```javascript
// 출석 이벤트
POST /api/professor/attendance/mark
→ GradeUpdateEvent(ATTENDANCE)
→ 출석 점수 재계산
→ 총점 자동 업데이트

// 과제 채점 이벤트
PUT /api/assignments/{idx}/grade
→ GradeUpdateEvent(ASSIGNMENT)
→ 과제 점수 재계산
→ 총점 자동 업데이트
```

#### ⚠️ 주의사항

- 교수 권한 필수 (userStudent = 1)
- 강의 권한: 본인 담당 강의만
- JSON 저장: ENROLLMENT_EXTENDED_TBL.ENROLLMENT_DATA
- 실시간 반영: 출석/과제 변경 시 자동 업데이트
- 최종 등급: 수동으로 finalizeGrades() 실행

</details>

---

## 🎓 결론

### v4.0 모듈화 버전 사용 권장

**이유:**
- ✅ 단계별 검증 → 오류 조기 발견
- ✅ 모듈 독립성 → 유지보수 용이
- ✅ 명확한 의존성 → 학습 효과
- ✅ 문서 분리 → 역할별 참조
- ✅ 확장 가능 → 미래 기능 추가

**v3.0은 언제?**
- 긴급 상황 (빠른 검증)
- 레거시 호환 필요
- 참고 자료 확인

---

**작성**: GitHub Copilot  
**날짜**: 2024  
**버전**: Legacy Documentation v1.0
