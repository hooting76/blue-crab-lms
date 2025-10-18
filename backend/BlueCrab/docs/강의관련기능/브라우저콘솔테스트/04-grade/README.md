# 📊 성적 관리 시스템 테스트

Phase4 신규 기능인 JSON 기반 성적 관리 시스템 테스트 스크립트입니다.

## 📁 파일 목록

### `grade-management-test.js` (v2.0)
**성적 관리 시스템 통합 테스트**

#### ✅ 주요 기능

1. **성적 구성 설정**
   - POST /api/enrollments/grade-config
   - 출석, 과제, 시험 배점 설정
   - 등급 분포 기준 설정

2. **학생 성적 조회**
   - POST /api/enrollments/grade-info (action: "get-grade")
   - 출석율 자동 계산
   - 과제 점수 자동 집계
   - 총점 및 백분율 계산

3. **교수용 성적 조회**
   - POST /api/enrollments/grade-info (action: "professor-view")
   - 학생 성적 상세 정보
   - 등급 부여 권한

4. **전체 성적 목록**
   - POST /api/enrollments/grade-list
   - 성적순 정렬
   - 페이징 지원

5. **최종 등급 배정**
   - POST /api/enrollments/grade-finalize
   - 60% 기준 합격/불합격 분류
   - 상대평가 (A, B, C, D 배정)
   - 동점자 처리 (같은 점수 = 상위 등급)

## 🆕 v2.0 개선사항

### 1. **에러 처리 강화**
```javascript
✅ HTTP 상태 코드 검증
✅ API success 필드 체크
✅ 응답 시간 측정 (ms)
✅ 명확한 성공/실패 표시 (✅/❌)
```

### 2. **응답 데이터 구조화**
```javascript
✅ 출석 정보 (현재 점수, 만점, 비율)
✅ 과제 목록 (과제명, 점수, 만점)
✅ 총점 정보 (점수, 만점, 백분율)
✅ 등급 정보 (A, B, C, D, F)
```

### 3. **동적 테스트 데이터**
```javascript
✅ setTestData(lecIdx, studentIdx, professorIdx)
✅ customTest(lecIdx, studentIdx)
✅ getData() - 현재 데이터 확인
```

### 4. **테스트 결과 요약**
```javascript
✅ 전체 테스트 통계 (성공/실패)
✅ 개별 테스트 결과
✅ 실행 시간 측정
```

## 🚀 사용 방법

### 1. 교수로 로그인
```javascript
// docs/일반유저 로그인+게시판/test-1-login.js
await login()
// 교수 계정 필수 (userStudent: 1)
```

### 2. 전체 테스트 실행 (권장)
```javascript
await gradeTests.runAll()
```

### 3. 개별 테스트
```javascript
// 성적 구성 설정
await gradeTests.config()

// 학생 성적 조회
await gradeTests.studentInfo()

// 교수용 성적 조회
await gradeTests.professorView()

// 성적 목록 조회
await gradeTests.gradeList()

// 최종 등급 배정
await gradeTests.finalize()
```

### 4. 고급 기능
```javascript
// 현재 테스트 데이터 확인
gradeTests.getData()

// 테스트 데이터 변경
gradeTests.setData(2, 101, 22)

// 커스텀 데이터로 테스트
await gradeTests.customTest(1, 100)
```

## 📊 예상 출력 (v2.0)

```
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
📊 학생 성적 조회 테스트
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
📤 요청 데이터: {lecIdx: 1, studentIdx: 100, action: "get-grade"}
✅ /grade-info 성공 (125.34ms)
📝 메시지: 성적 조회가 완료되었습니다.

📊 성적 상세 정보:
  📅 출석:
    - 현재 점수: 18.5
    - 만점: 20
    - 비율: 92.5%
  📝 과제: 3개
    1. 과제1: 9/10
    2. 중간고사: 85/100
    3. 기말고사: 92/100
  💯 총점:
    - 점수: 204.5
    - 만점: 230
    - 백분율: 88.9%

✅ 테스트 성공

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
📊 테스트 결과 요약
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
총 테스트: 5개
✅ 성공: 5개
❌ 실패: 0개

상세 결과:
  ✅ 1. 성적 구성 설정
  ✅ 2. 학생 성적 조회
  ✅ 3. 교수용 성적 조회
  ✅ 4. 성적 목록 조회
  ✅ 5. 최종 등급 배정
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

🎉 모든 테스트가 성공했습니다!
```

## 🎯 이벤트 기반 자동 계산

성적 관리 시스템은 이벤트 기반으로 자동 업데이트됩니다:

### 출석 이벤트
```javascript
// 교수가 출석 체크
POST /api/professor/attendance/mark

// 🔥 자동 이벤트 발생
→ GradeUpdateEvent(lecIdx, studentIdx, "ATTENDANCE")
→ 출석 점수 재계산
→ 총점 자동 업데이트
```

### 과제 채점 이벤트
```javascript
// 교수가 과제 채점
PUT /api/assignments/{idx}/grade

// 🔥 자동 이벤트 발생
→ GradeUpdateEvent(lecIdx, studentIdx, "ASSIGNMENT")
→ 과제 점수 재계산
→ 총점 자동 업데이트
```

## ⚠️ 주의사항

- **교수 권한 필수**: userStudent = 1
- **강의 권한**: 본인 담당 강의만 접근
- **JSON 저장**: ENROLLMENT_EXTENDED_TBL.ENROLLMENT_DATA
- **실시간 반영**: 출석/과제 변경 시 자동 업데이트
- **최종 등급**: 수동으로 finalizeGrades() 실행 필요

## 📋 등급 배정 로직

### 60% 기준 + 상대평가

```
1단계: 합격/불합격 분류
  - 60% 이상 → 합격 (상대평가 대상)
  - 60% 미만 → F등급 확정

2단계: 등급 배정 (합격자만)
  - 기본 비율: A(30%), B(40%), C(20%), D(10%)
  - 낙제자가 하위 등급부터 차지
  - 동점자는 모두 상위 등급

3단계: 결과
  - 낙제자 많으면 → 합격자 상위 등급 배정
  - 예: 100명 중 75명 낙제 → 합격 25명 모두 A등급
```

## 🔗 관련 문서

- [상위 README](../README.md)
- [성적 관리 시스템 구현 가이드](../../성적관리/성적관리시스템_구현가이드.md)
- [테스트 검토 리포트](../TEST_REVIEW.md)
- [교수 테스트](../03-professor/)

## 📚 참고 자료

- [EnrollmentController.java](../../../../src/main/java/BlueCrab/com/example/controller/Lecture/EnrollmentController.java)
- [EnrollmentService.java](../../../../src/main/java/BlueCrab/com/example/service/Lecture/EnrollmentService.java)
- [성적관리시스템_설계안_수정.drawio](../../성적관리/성적관리시스템_설계안_수정.drawio)
