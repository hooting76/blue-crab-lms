# 강의 관리 시스템 가이드

> **최종 업데이트**: 2025-10-16  
> **현재 버전**: v3.0 (백엔드 필터링 구현 완료)  
> **프로젝트 현황**: Blue Crab LMS - 강의 관리 및 시기 관리 시스템 완료

---

## 🎯 시스템 개요

Blue Crab LMS의 강의 관리 시스템은 대학교 LMS에서 강의와 관련된 모든 기능을 통합 관리합니다.

### 핵심 특징
- ✅ **3단계 시기 관리**: 방학/수강신청/개강 자동 전환
- ✅ **0값 규칙**: 제한없음 표시 (학부=0, 학과=0, 학년=0)
- ✅ **백엔드 필터링**: 학생 전공/부전공 기반 자동 필터링 ⭐ NEW
- ✅ **수강 가능성 검증**: 실시간 자격 요건 확인 + 상세 사유 제공
- ✅ **완전한 REST API**: 표준 CRUD + 고급 검색 기능

---

## 📚 문서 구조

```
📁 docs/강의관련기능/
├── 📄 README.md                           ← 현재 파일 (시작점)
├── 📄 IMPLEMENTATION_PROGRESS.md          ← 개발 진척도 및 로드맵
├── 📄 API_CONTROLLER_MAPPING.md           ← API 엔드포인트 전체 명세
├── 📄 BACKEND_FILTERING_IMPLEMENTATION.md ← 백엔드 필터링 구현 보고서 ⭐ NEW
├── 📄 FILTERING_TEST_GUIDE.md             ← 필터링 테스트 가이드 ⭐ NEW
├── 📁 시기관련/                           ← 학기 시스템 관련 문서
│   ├── 📄 semester-system-flow.drawio    ← 시기 플로우 다이어그램
│   ├── 📄 시기관련_시스템_기능.md         ← 시기 관리 상세 명세
│   └── 📄 0값규칙_상세명세.md             ← 제한없음 규칙 설명
└── 📁 테스트/                             ← 테스트 파일 및 가이드
    ├── 📄 lecture-test-eligible-lectures.js ← 수강 가능 강의 조회 테스트
    └── 📄 browser-console-testing-guide.md  ← 브라우저 테스트 가이드
```

---

## 🚀 빠른 시작

### 1. API 확인
가장 먼저 [`API_CONTROLLER_MAPPING.md`](./API_CONTROLLER_MAPPING.md)에서 전체 API 구조를 확인하세요.

### 2. 백엔드 필터링 이해 ⭐ NEW
[`BACKEND_FILTERING_IMPLEMENTATION.md`](./BACKEND_FILTERING_IMPLEMENTATION.md)에서 학생 전공/부전공 기반 필터링 구현을 확인하세요.

### 3. 필터링 테스트 ⭐ NEW
[`FILTERING_TEST_GUIDE.md`](./FILTERING_TEST_GUIDE.md)에서 테스트 시나리오와 예상 결과를 확인하세요.

### 4. 시기 시스템 이해
[`시기관련/`](./시기관련/) 폴더에서 학기 시스템의 작동 원리를 확인하세요.

### 5. 테스트 실행
[`테스트/`](./테스트/) 폴더의 브라우저 콘솔 테스트로 API를 검증하세요.

---

## 🔧 주요 기능

### 강의 관리
- **기본 CRUD**: 강의 생성, 조회, 수정, 삭제
- **검색 기능**: 강의명, 교수명, 학과별 필터링
- **수강 관리**: 수강 신청, 취소, 대기열 관리
- **백엔드 필터링** ⭐: 학생 전공/부전공 기반 자동 필터링
- **권한 제어**: 교수/학생 역할별 접근 제어

### 시기 관리
- **자동 전환**: 방학 → 수강신청 → 개강 자동 감지
- **관리자 제어**: 수동 시기 변경 가능
- **알림 시스템**: 시기 변경 시 FCM 푸시 알림

### 0값 규칙 (백엔드 필터링 적용)
- **학부**: `LEC_MCODE="0"` → 모든 학부 수강 가능
- **학과**: `LEC_MCODE_DEP="0"` → 모든 학과 수강 가능  
- **학년**: `LEC_MIN=0` → 모든 학년 수강 가능 (미구현)
- **전공/부전공 매칭**: 주전공 또는 부전공 일치 시 수강 가능 ⭐
- **자동 필터링**: 백엔드에서 학생 자격 검증 후 수강 가능 강의만 반환 ⭐

---

## 📋 최신 업데이트 (v3.0 - 백엔드 필터링)

### 🆕 백엔드 필터링 완전 구현 (2025-10-16)

#### 구현 내용
**기존 인프라 활용**:
- `ProfileView` 엔티티 (USER_TBL + SERIAL_CODE_TABLE 조인 뷰)
- `ProfileViewRepository` (전공/부전공 정보 조회)

**완전한 0값 규칙 구현**:
```java
// 학부 코드: "0" = 모든 학부 가능
if (!"0".equals(lecture.getLecMcode())) {
    // 주전공 또는 부전공 학부 일치 확인
    boolean majorMatch = lecture.getLecMcode().equals(studentProfile.getMajorFacultyCode());
    boolean minorMatch = lecture.getLecMcode().equals(studentProfile.getMinorFacultyCode());
    if (!majorMatch && !minorMatch) return false;
}
```

**전공/부전공 이중 매칭**:
- 학생의 **주전공** 또는 **부전공**과 일치하면 수강 가능
- 예: 경영학과(주전공) + 컴공(부전공) → 컴공 과목도 수강 가능!

#### 수강 가능 강의 조회 API
```
POST /api/lectures/eligible
Body: {studentId: 1, page: 0, size: 20}
```

#### 주요 특징
- **백엔드 필터링**: 서버에서 학생 전공 정보 조회 및 검증 ⭐
- **0값 규칙 적용**: 제한없는 강의 자동 포함
- **전공/부전공 매칭**: 주전공 또는 부전공 일치 시 수강 가능 ⭐
- **상세 사유 제공**: 수강 가능/불가 이유 명시 ⭐
- **페이징 지원**: 대량 데이터 효율적 처리

#### 응답 예시
```json
{
  "eligibleLectures": [
    {
      "lecIdx": 101,
      "lecSerial": "CS101",
      "lecTit": "자료구조",
      "lecProf": "P002",
      "lecMcode": "01",
      "lecMcodeDep": "05",
      "isEligible": true,
      "eligibilityReason": "수강 가능 (전공 일치: 01-05)"
    }
  ],
  "totalCount": 150,
  "eligibleCount": 45,
  "ineligibleCount": 105,
  "studentInfo": {
    "userIdx": 1,
    "userName": "홍길동",
    "majorFacultyCode": "01",
    "majorDeptCode": "05",
    "minorFacultyCode": "02",
    "minorDeptCode": "03",
    "hasMajorInfo": true,
    "hasMinorInfo": true
  }
}
```

#### 상세 사유 메시지
**수강 불가**:
- `"개설되지 않은 강의입니다"`
- `"정원이 초과되었습니다 (30/30)"`
- `"전공 정보가 등록되지 않았습니다 (0값 강의만 수강 가능)"`
- `"학부 코드 불일치 (요구: 01, 보유: 02/03)"`

**수강 가능**:
- `"수강 가능 (제한 없음 - 전체 학생 대상)"` ← 0값 강의
- `"수강 가능 (전공 일치: 01-05)"` ← 주전공
- `"수강 가능 (부전공 일치: 02-03)"` ← 부전공

---

## 🔍 개발 진행 상황

현재 **Phase 9 완료** 상태이며, 백엔드 필터링까지 모든 핵심 기능이 구현되었습니다.

| Phase | 상태 | 완료율 |
|-------|------|--------|
| Phase 1-6 | ✅ 완료 | 100% |
| Phase 7.1 | ✅ 완료 | 100% |
| Phase 8 | ✅ 완료 | 100% |
| Phase 9 (백엔드 필터링) | ✅ 완료 | 100% ⭐ |
| **전체** | **✅ 완료** | **100%** |

자세한 진행 상황은 [`IMPLEMENTATION_PROGRESS.md`](./IMPLEMENTATION_PROGRESS.md)를 참고하세요.

### 🎯 구현 완료 항목
- ✅ 강의 CRUD 및 검색
- ✅ 수강신청/취소
- ✅ 과제 관리
- ✅ 출석 관리
- ✅ 0값 규칙 (학부/학과)
- ✅ **백엔드 필터링 (전공/부전공 매칭)** ⭐ NEW
- ✅ **상세 사유 메시지** ⭐ NEW
- ⏸️ 학년 필터링 (보류 - 로직 합의 필요)

---

## 🤝 기여 가이드

### 코드 수정 시
1. 해당 기능의 테스트 파일을 먼저 확인
2. API 변경 시 문서 업데이트 필수
3. 새 기능 추가 시 테스트 케이스 작성

### 문서 업데이트 시
1. 이 README.md 파일의 버전 정보 업데이트
2. 관련 API 문서 동기화
3. 변경 사항을 IMPLEMENTATION_PROGRESS.md에 기록

---

## 📞 지원

- **기술 문의**: 프로젝트 이슈 트래커 활용
- **백엔드 필터링**: [`BACKEND_FILTERING_IMPLEMENTATION.md`](./BACKEND_FILTERING_IMPLEMENTATION.md) 참고 ⭐
- **테스트 가이드**: [`FILTERING_TEST_GUIDE.md`](./FILTERING_TEST_GUIDE.md) 참고 ⭐
- **버그 리포트**: 테스트 파일로 재현 후 리포트

---

**최종 수정일**: 2025-10-16  
**최신 버전**: v3.0 (백엔드 필터링 완전 구현)
