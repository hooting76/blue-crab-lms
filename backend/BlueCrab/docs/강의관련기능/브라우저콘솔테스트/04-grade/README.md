# 브라우저 콘솔 성적 관리 테스트

이 디렉토리에는 LMS 성적 관리 시스템의 브라우저 콘솔 테스트 파일들이 있습니다.

## 📁 파일 목록

### 01-grade-phase1-tests.js
**Phase 1: 성적 관리 테스트 (독립 실행)**

성적 관리의 기본적인 CRUD 기능을 테스트하는 독립 실행 파일입니다.

#### 🚀 주요 기능
- **성적 구성 설정 저장**: 출석 만점, 지각 페널티, 등급 분포 설정
- **학생 성적 조회**: 개별 학생의 상세 성적 정보 확인
- **교수용 성적 조회**: 반 통계와 순위 정보 포함
- **성적 목록 조회**: 전체 학생 성적 목록 및 페이지네이션
- **최종 등급 배정**: 자동 A/B/C/D 등급 계산 및 배정

#### 📋 사용법

```javascript
// 1. 강의 설정
gradePhase1.setLecture('ETH201');  // 강의 코드 지정

// 2. 전체 테스트 실행 (권장)
await gradePhase1.runAll();  // 5개 API 테스트 일괄 실행

// 또는 개별 API 테스트
await gradePhase1.config();        // 설정 서버 저장
await gradePhase1.studentInfo();   // 학생 성적 조회
await gradePhase1.professorView(); // 교수용 조회
await gradePhase1.gradeList();     // 성적 목록 조회
await gradePhase1.finalize();      // 최종 등급 배정
```

#### ⚙️ 설정 관리

```javascript
// 빠른 설정 (권장 - 즉시 서버 저장)
await gradePhase1.quickConfig();  // 프롬프트로 입력 + 즉시 저장

// 직접 설정 수정
await gradePhase1.updateServerConfig({
    attendanceMaxScore: 80,
    latePenaltyPerSession: 0.5,
    gradeDistribution: { A: 30, B: 40, C: 20, D: 10 }
});

// 서버 설정 조회
await gradePhase1.getServerConfig();
```

### 04-enrollment-list-utility.js
**Phase 4: 수강생 목록 조회 유틸리티**

강의의 수강생 목록과 통계를 조회하는 유틸리티 파일입니다.

#### 🚀 주요 기능
- **수강생 목록 조회**: 강의별 수강생 전체 목록 확인
- **수강 통계 조회**: 강의별 수강생 수 통계 확인

#### 📋 사용법

```javascript
// 수강생 목록 조회
await getEnrolledStudents("ETH201");     // ETH201 강의 수강생 목록

// 수강 통계 조회
await getEnrollmentStats("ETH201");      // ETH201 강의 수강 통계
```

#### 🔗 API 엔드포인트
- `POST /api/enrollments/grade-config` - 성적 구성 설정
- `POST /api/enrollments/grade-info` - 성적 정보 조회
- `POST /api/enrollments/grade-list` - 성적 목록 조회
- `POST /api/enrollments/grade-finalize` - 최종 등급 배정

---

### 02-grade-phase3-tests.js
**출석 승인 + 성적 자동 재계산 통합 테스트**

출석 승인과 성적 자동 재계산 기능의 통합을 테스트하는 파일입니다.

#### 🚀 주요 기능
- **출석 승인**: 특정 회차의 학생 출석을 승인
- **성적 자동 재계산**: 출석 승인 후 실시간 성적 재계산 검증
- **결과 비교**: 승인 전후 성적 변화 분석

#### 📋 사용법

```javascript
// 1. 먼저 로그인 (필수)
await login();

// 2. 통합 테스트 실행
await testAttendanceGradeIntegration("ETH201", 6, 3);

// 파라미터:
// - "ETH201": 강의 코드
// - 6: 학생 IDX
// - 3: 승인할 회차 번호
```

#### 📊 테스트 결과
- 승인 전/후 출석 점수 비교
- 출석 회차 증가 확인
- 예상 점수 계산 및 일치 여부 검증

#### 🔗 API 엔드포인트
- `POST /api/enrollments/grade-info` - 성적 조회
- `POST /api/attendance/approve` - 출석 승인

---

## 🔐 인증 요구사항

### 필수 로그인 계정

#### Phase 1 테스트용
- **학생 계정 (ROLE_STUDENT)**: 학생 성적 조회 테스트
- **교수 계정 (ROLE_PROFESSOR)**: 성적 관리 및 등급 배정
- **관리자 계정 (ROLE_ADMIN)**: 모든 기능 접근

#### Phase 3 테스트용
- **교수/관리자 계정**: 출석 승인 권한 필요

### 인증 방법
```javascript
// JWT 토큰 설정 (브라우저 콘솔에서)
window.authToken = "your-jwt-token-here";

// 또는 localStorage에 저장
localStorage.setItem('jwtAccessToken', 'your-jwt-token-here');
```

---

## 📊 성적 계산 로직

### 최종 점수 계산
```
최종 점수 = (출석율 × 출석만점) + 과제점수합계
```

### 등급 배정 방식
- **백분율 기반 분포**: A(30%), B(40%), C(20%), D(10%)
- **하위 침범 방식**: 낙제자가 하위 등급부터 차지
- **동점자 처리**: 같은 백분율은 동일 등급 부여

### 합격 기준
- 기본값: 60.0% (설정 가능)

---

## 🛠️ 개발자 참고사항

### 파일 구조
```
04-grade/
├── 01-grade-phase1-tests.js    # 독립 성적 관리 테스트
├── 02-grade-phase3-tests.js    # 출석+성적 통합 테스트
└── README.md                   # 이 파일
```

### 테스트 실행 순서
1. **Phase 1**: 기본 성적 관리 기능 검증
2. **Phase 3**: 출석 승인과 성적 재계산 통합 검증

### 주의사항
- 각 테스트는 독립적으로 실행 가능
- Phase 1은 다른 파일 의존성 없음
- Phase 3은 로그인 필수
- API 엔드포인트는 동일한 인증 체계 사용

---

## 🔄 업데이트 이력

- **2025-10-25**: README 작성
- **2025-10-24**: Phase 3 파일 개선 (신버전 API 적용)
- **2025-10-24**: Phase 1 파일 코드 중복 제거 및 유틸리티 함수 추가