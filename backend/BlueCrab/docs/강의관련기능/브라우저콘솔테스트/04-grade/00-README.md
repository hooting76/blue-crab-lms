# 📊 Blue Crab LMS 성적 관리 브라우저 콘솔 테스트 가이드

## 📋 개요

Blue Crab LMS의 성적 관리 시스템을 위한 완전 독립형 브라우저 콘솔 테스트 모음입니다. 실제 서버 환경에서 API를 테스트하고 성적 관리 기능을 검증할 수 있습니다.

### 🎯 주요 특징

- **완전 독립 실행**: 다른 파일 의존성 없음
- **실제 서버 테스트**: `https://bluecrab.chickenkiller.com` 환경 사용
- **JWT 인증**: 실제 사용자 토큰으로 인증
- **lecSerial 기반**: 강의 코드로 데이터 조회/관리
- **자동화 테스트**: 이벤트 기반 성적 재계산 검증

### 📁 파일 구조

```text
04-grade/
├── 00-README.md              # 📖 이 가이드 문서
├── 01-grade-phase1-tests.js  # Phase 1: 성적 관리 핵심 기능 (5개 API)
├── 02-grade-phase3-tests.js  # Phase 3: 이벤트 기반 업데이트 (2개 API)
├── 11-TEST-FLOW.drawio       # 테스트 플로우 다이어그램
└── 11-TEST-FLOW.drawio.png   # 플로우 차트 이미지
```

## 🚀 빠른 시작

### 1단계: 브라우저 콘솔에서 로그인

```javascript
// 교수 또는 학생 계정으로 로그인
await login()
```

### 2단계: 테스트 파일 로드

브라우저 콘솔에서 다음 파일들을 로드하세요:

```javascript
// Phase 1: 성적 관리 핵심 기능
// 파일: 01-grade-phase1-tests.js

// Phase 3: 이벤트 기반 업데이트
// 파일: 02-grade-phase3-tests.js
```

### 3단계: 기본 설정 및 실행

```javascript
// ===== Phase 1 실행 =====
// 강의 설정 (lecSerial만 필요)
gradePhase1.setLecture('ETH201')

// 전체 테스트 실행 (5개 API)
await gradePhase1.runAll()

// ===== Phase 3 실행 =====
// 수강생 목록 조회 (선택)
await gradePhase3.listStudents()

// 강의 + 학생 설정 (enrollmentIdx 자동 조회)
gradePhase3.setLecture('ETH201', 6)

// 전체 테스트 실행 (2개 API)
await gradePhase3.runAll()
```

## 📚 상세 사용법

### Phase 1: 성적 관리 핵심 기능

#### 🎯 테스트 목적
강의별 성적 구성 설정, 조회, 최종 등급 배정 등 성적 관리의 핵심 기능을 테스트합니다.

#### ⚙️ 기본 설정

```javascript
// 1. 강의 지정 (필수)
gradePhase1.setLecture('ETH201')  // lecSerial만 지정

// 2. 학생 지정 (선택: 조회 테스트용)
gradePhase1.setLecture('ETH201', 6)  // lecSerial + studentIdx

// 3. 대화형 설정
gradePhase1.promptLecture()  // 프롬프트로 입력
```

#### 🔧 로컬 설정 변경 (서버 전송 전)

```javascript
// 출석 설정만 빠르게 변경
gradePhase1.quickAttendanceConfig(80, 0.5)  // 만점, 지각페널티

// 전체 설정 대화형 입력
gradePhase1.promptConfig()

// 객체로 직접 변경
gradePhase1.updateConfig({
    attendanceMaxScore: 85,
    latePenaltyPerSession: 0.3,
    gradeDistribution: { A: 25, B: 35, C: 25, D: 15 }
})

// 현재 설정 조회
gradePhase1.getConfig()
```

#### 📋 개별 API 테스트

```javascript
// 1. 성적 구성 설정 저장
await gradePhase1.config()

// 2. 학생 성적 조회
await gradePhase1.studentInfo()

// 3. 교수용 성적 조회
await gradePhase1.professorView()

// 4. 성적 목록 조회
await gradePhase1.gradeList()

// 5. 최종 등급 배정
await gradePhase1.finalize()
```

#### 📊 테스트 결과 해석

```text
✅ POST /enrollments/grade-config (245ms)
✅ 설정 저장 완료

✅ POST /enrollments/grade-info (156ms)
📊 학생 성적: 87.5점 (A등급)
   출석: 18.5/20점
   과제: 45/50점
   시험: 24/30점
```

### Phase 3: 이벤트 기반 자동 업데이트

#### 🎯 이벤트 기반 테스트 목적
출석 기록, 과제 채점 시 자동으로 성적이 재계산되는 이벤트 기반 시스템을 테스트합니다.

#### ⚙️ 이벤트 기반 설정

```javascript
// 1. 수강생 목록 조회 (lecSerial 기반)
await gradePhase3.listStudents()

// 2. 강의 + 학생 + 과제 설정
gradePhase3.setLecture('ETH201', 6, 42)  // lecSerial, studentIdx, assignmentIdx

// 3. 대화형 설정
gradePhase3.promptLecture()
```

#### 📋 이벤트 기반 API 테스트

```javascript
// 1. 출석 업데이트 → 성적 자동 재계산
await gradePhase3.attendance()

// 2. 과제 점수 업데이트 → 성적 자동 재계산
await gradePhase3.assignment()
```

#### 🔄 이벤트 플로우 검증

```text
[1/3] 업데이트 전 성적 조회
   출석 점수: 18.5점

[2/3] 출석 기록
   상태: PRESENT

[3/3] 업데이트 후 성적 조회
   출석 점수: 19.0점 (+0.5점)
✅ 이벤트 기반 업데이트 성공!
```

## 🔗 API 엔드포인트 정리

### Phase 1 API

| # | 기능 | 엔드포인트 | 메서드 | 설명 |
|---|------|------------|--------|------|
| 1 | 성적 구성 설정 | `/enrollments/grade-config` | POST | 출석/과제 비율, 등급 분포 설정 |
| 2 | 학생 성적 조회 | `/enrollments/grade-info` | POST | action: "get-grade" |
| 3 | 교수용 조회 | `/enrollments/grade-info` | POST | action: "professor-view" |
| 4 | 성적 목록 조회 | `/enrollments/grade-list` | POST | action: "list-all" |
| 5 | 최종 등급 배정 | `/enrollments/grade-finalize` | POST | action: "finalize" |

### Phase 3 API

| # | 기능 | 엔드포인트 | 메서드 | 설명 |
|---|------|------------|--------|------|
| 1 | 출석 업데이트 | `/enrollments/{enrollmentIdx}/attendance` | PUT | 이벤트 발행 → 성적 재계산 |
| 2 | 과제 채점 | `/assignments/{assignmentIdx}/grade` | PUT | 이벤트 발행 → 성적 재계산 |

### 지원 API

| 기능 | 엔드포인트 | 메서드 | 설명 |
|------|------------|--------|------|
| 수강생 목록 | `/enrollments/list` | POST | lecSerial 기반 수강생 조회 |

## ⚠️ 주의사항

### 🔐 인증 요구사항

- **JWT 토큰 필수**: `await login()`으로 로그인 후 실행
- **권한 확인**: 교수 계정 권장 (모든 기능 테스트 가능)

### 📊 데이터 준비

- **강의 존재**: 테스트할 lecSerial이 실제 DB에 존재해야 함
- **수강생 등록**: studentIdx가 해당 강의에 등록되어 있어야 함
- **과제 존재**: Phase 3 과제 테스트 시 assignmentIdx 필요

### 🔄 이벤트 처리

- **자동 재계산**: 출석/과제 업데이트 시 1초 대기 후 결과 확인
- **실시간 반영**: 이벤트가 성공적으로 처리되어야 성적에 반영됨

## 🐛 문제 해결

### ❌ "로그인 필요!" 오류

```javascript
// 해결: 로그인 후 재실행
await login()
await gradePhase1.runAll()
```

### ❌ "존재하지 않는 강의 코드" 오류

```javascript
// 해결: 실제 존재하는 lecSerial 사용
gradePhase1.setLecture('CS101-2024-1')  // 실제 강의 코드로 변경
```

### ❌ "enrollmentIdx 조회 실패" 오류

```javascript
// 해결: 수강생 목록으로 studentIdx 확인
await gradePhase3.listStudents()
gradePhase3.setLecture('ETH201', 6)  // 확인된 studentIdx 사용
```

### ❌ "과제 IDX 미설정" 오류

```javascript
// 해결: 과제 IDX 입력
gradePhase3.setLecture('ETH201', 6, 42)  // assignmentIdx 추가
```

## 📖 참고 자료

### 📚 관련 문서

- [성적 관리 시스템 문서](../../성적관리/성적관리_시스템_문서.md)
- [브라우저 콘솔 테스트 메인 가이드](../00-README.md)
- [API 엔드포인트 정리](../../../../api-endpoints-documentation.json)

### 🔗 테스트 파일 링크

- [Phase 1 테스트 코드](./01-grade-phase1-tests.js)
- [Phase 3 테스트 코드](./02-grade-phase3-tests.js)

### 📊 테스트 플로우

- [테스트 플로우 다이어그램](./11-TEST-FLOW.drawio)

---

## 🎯 다음 단계

1. **기본 테스트**: Phase 1부터 시작하여 각 API 기능 확인
2. **이벤트 검증**: Phase 3으로 자동 재계산 기능 테스트
3. **커스텀 테스트**: 개별 API로 특정 시나리오 집중 테스트

**문제가 발생하면 이 가이드를 참고하거나 개발팀에 문의하세요!**

*작성일: 2025년 10월 22일*  
*Blue Crab LMS 개발팀*
