# 강의 관리 시스템 가이드

> **최종 업데이트**: 2025-10-17  
> **현재 버전**: v5.0 (lecIdx → lecSerial 마이그레이션 완료)  
> **프로젝트 현황**: Blue Crab LMS - 강의 관리 및 시기 관리 시스템 완료

---

## 📑 목차

1. [시스템 개요](#-시스템-개요)
2. [문서 구조](#-문서-구조)
3. [빠른 시작](#-빠른-시작)
4. [주요 기능](#-주요-기능)
5. [최신 업데이트](#-최신-업데이트)
   - [v5.0 - lecIdx → lecSerial 마이그레이션](#-v50---lecidx--lecserial-마이그레이션-2025-10-17)
   - [v4.0 - POST 방식 완전 통일](#-v40---post-방식-완전-통일-2025-10-16)
   - [v3.0 - 백엔드 필터링 완전 구현](#-v30---백엔드-필터링-완전-구현-2025-10-16)
6. [개발 진행 상황](#-개발-진행-상황)
7. [기여 가이드](#-기여-가이드)
8. [지원](#-지원)

---

## 🎯 시스템 개요

Blue Crab LMS의 강의 관리 시스템은 대학교 LMS에서 강의와 관련된 모든 기능을 통합 관리합니다.

### 핵심 특징
- ✅ **3단계 시기 관리**: 방학/수강신청/개강 자동 전환
- ✅ **0값 규칙**: 제한없음 표시 (학부=0, 학과=0, 학년=0)
- ✅ **lecSerial 기반 API**: 프론트엔드에서 lecIdx 완전 숨김 ⭐ NEW
- ✅ **백엔드 변환 레이어**: lecSerial ↔ lecIdx 자동 변환 ⭐ NEW
- ✅ **백엔드 필터링**: 학생 전공/부전공 기반 자동 필터링 ⭐
- ✅ **수강 가능성 검증**: 실시간 자격 요건 확인 + 상세 사유 제공
- ✅ **POST 방식 통일**: 모든 API가 Request Body 기반 통신 ⭐ NEW
- ✅ **완전한 REST API**: 표준 CRUD + 고급 검색 기능

---

## 📚 문서 구조

```
📁 docs/강의관련기능/
├── 📄 README.md                                 ← 현재 파일 (시작점)
├── 📄 요구사항분석.md                           ← 시스템 요구사항 정의
│
├── 📋 핵심 설계 문서
│   ├── 📄 00-강의시간형식-업데이트-요약.md      ← 강의 시간 형식 표준
│   ├── 📄 01-데이터베이스설계.md                ← DB 스키마 설계
│   ├── 📄 02-API명세서.md                       ← 전체 API 명세 (v4.0)
│   ├── 📄 03-비즈니스로직.md                    ← 비즈니스 룰 정의
│   ├── 📄 04-관리자플로우.md                    ← 관리자 사용 시나리오
│   ├── 📄 05-교수플로우.md                      ← 교수 사용 시나리오
│   ├── 📄 06-학생플로우.md                      ← 학생 사용 시나리오
│   ├── 📄 07-구현순서.md                        ← 개발 단계별 가이드
│   └── 📄 08-프론트엔드연동.md                  ← 프론트엔드 연동 가이드
│
├── 📊 진행 상황 및 API 문서
│   ├── 📄 IMPLEMENTATION_PROGRESS.md            ← 개발 진척도 및 로드맵
│   ├── 📄 API_CONTROLLER_MAPPING.md             ← API 엔드포인트 전체 명세 (v5.0)
│   ├── 📄 PHASE9_COMPLETION_SUMMARY.md          ← Phase 9-10 완료 요약
│   └── 📄 POST방식통일-작업완료보고서.md        ← POST 방식 통일 보고서
│
├── 📄 백엔드 필터링 및 마이그레이션
│   ├── 📄 BACKEND_FILTERING_IMPLEMENTATION.md   ← 백엔드 필터링 구현 보고서
│   ├── 📄 FILTERING_TEST_GUIDE.md               ← 필터링 테스트 가이드
│   └── 📄 MIGRATION_COMPLETE_SUMMARY.md         ← lecIdx→lecSerial 마이그레이션 보고서 ⭐ NEW
│
├── 📊 다이어그램
│   ├── 📄 ERD_Diagram.drawio                    ← ERD 다이어그램 (v4.0)
│   └── 📄 ERD_Diagram.drawio.png                ← ERD 이미지
│
├── 📁 시기관련/                                  ← 학기 시스템 관련 문서
│   ├── 📄 01-학기시스템플로우.md                ← 시기 관리 상세 명세
│   ├── 📄 02-시기별API명세서.md                 ← 시기별 API 동작
│   ├── 📄 03-수강신청자격검증로직.md            ← 수강 자격 검증 로직
│   ├── 📄 semester-system-flow.drawio           ← 시기 플로우 다이어그램
│   └── 📄 semester-system-flow.drawio.png       ← 시기 플로우 이미지
│
└── 📁 브라우저콘솔테스트/                        ← 테스트 파일 및 가이드
    ├── 📄 README.md                             ← 테스트 가이드
    ├── 📄 usage-diagram.drawio                  ← 테스트 플로우 다이어그램 (v5.0)
    ├── 📄 usage-diagram.drawio.png              ← 테스트 플로우 이미지
    ├── 📄 MIGRATION_COMPLETE_SUMMARY.md         ← lecSerial 마이그레이션 완료 보고서 ⭐
    ├── 📄 lecture-test-1-admin-create.js        ← 관리자: 강의 생성 테스트
    ├── 📄 lecture-test-2a-student-enrollment.js ← 학생: 수강신청 테스트
    ├── 📄 lecture-test-2b-student-my-courses.js ← 학생: 내 수강 목록 테스트
    ├── 📄 lecture-test-3-student-assignment.js  ← 학생: 과제 제출 테스트
    ├── 📄 lecture-test-4a-professor-assignment-create.js ← 교수: 과제 생성 테스트
    ├── 📄 lecture-test-4b-professor-assignment-grade.js  ← 교수: 과제 채점 테스트
    ├── 📄 lecture-test-5-professor-students.js  ← 교수: 수강생 관리 테스트
    └── 📄 lecture-test-6-admin-statistics.js    ← 관리자: 통계 조회 테스트
```

---

## 🚀 빠른 시작

### 📖 문서 읽기 순서 (추천)

#### 1단계: 시스템 이해
1. [`요구사항분석.md`](./요구사항분석.md) - 전체 시스템 요구사항
2. [`01-데이터베이스설계.md`](./01-데이터베이스설계.md) - DB 구조 이해
3. [`ERD_Diagram.drawio`](./ERD_Diagram.drawio) - 시각적 DB 구조

#### 2단계: API 및 기능 파악
4. [`API_CONTROLLER_MAPPING.md`](./API_CONTROLLER_MAPPING.md) - **전체 API 구조** (v5.0) ⭐ 필수
5. [`POST방식통일-작업완료보고서.md`](./POST방식통일-작업완료보고서.md) - POST 통일 내용 ⭐ 필수
6. [`02-API명세서.md`](./02-API명세서.md) - 상세 API 명세 (v4.0)

#### 3단계: 백엔드 필터링 이해 (Phase 9)
7. [`BACKEND_FILTERING_IMPLEMENTATION.md`](./BACKEND_FILTERING_IMPLEMENTATION.md) - 필터링 구현 상세 ⭐
8. [`FILTERING_TEST_GUIDE.md`](./FILTERING_TEST_GUIDE.md) - 테스트 시나리오 ⭐

#### 4단계: 시기 시스템 이해
9. [`시기관련/01-학기시스템플로우.md`](./시기관련/01-학기시스템플로우.md) - 시기 관리 시스템
10. [`시기관련/03-수강신청자격검증로직.md`](./시기관련/03-수강신청자격검증로직.md) - 자격 검증

#### 5단계: 테스트 실행
11. [`브라우저콘솔테스트/README.md`](./브라우저콘솔테스트/README.md) - 테스트 가이드
12. 각 테스트 파일 실행 (lecture-test-*.js)

### 🎯 역할별 추천 문서

**프론트엔드 개발자**:
1. [`API_CONTROLLER_MAPPING.md`](./API_CONTROLLER_MAPPING.md) - API 전체 목록 (v5.0)
2. [`POST방식통일-작업완료보고서.md`](./POST방식통일-작업완료보고서.md) - POST 방식 이해
3. [`08-프론트엔드연동.md`](./08-프론트엔드연동.md) - 연동 가이드
4. [`브라우저콘솔테스트/`](./브라우저콘솔테스트/) - API 테스트 예제

**백엔드 개발자**:
1. [`01-데이터베이스설계.md`](./01-데이터베이스설계.md) - DB 스키마
2. [`03-비즈니스로직.md`](./03-비즈니스로직.md) - 비즈니스 룰
3. [`BACKEND_FILTERING_IMPLEMENTATION.md`](./BACKEND_FILTERING_IMPLEMENTATION.md) - 필터링 로직
4. [`IMPLEMENTATION_PROGRESS.md`](./IMPLEMENTATION_PROGRESS.md) - 진행 상황

**테스터/QA**:
1. [`FILTERING_TEST_GUIDE.md`](./FILTERING_TEST_GUIDE.md) - 필터링 테스트
2. [`브라우저콘솔테스트/README.md`](./브라우저콘솔테스트/README.md) - 브라우저 테스트
3. [`04-관리자플로우.md`](./04-관리자플로우.md) - 관리자 시나리오
4. [`05-교수플로우.md`](./05-교수플로우.md) - 교수 시나리오
5. [`06-학생플로우.md`](./06-학생플로우.md) - 학생 시나리오

**프로젝트 매니저**:
1. [`IMPLEMENTATION_PROGRESS.md`](./IMPLEMENTATION_PROGRESS.md) - 전체 진행도
2. [`PHASE9_COMPLETION_SUMMARY.md`](./PHASE9_COMPLETION_SUMMARY.md) - Phase 9-10 요약
3. [`POST방식통일-작업완료보고서.md`](./POST방식통일-작업완료보고서.md) - 최신 업데이트

---

## 🔧 주요 기능

### 강의 관리
- **기본 CRUD**: 강의 생성, 조회, 수정, 삭제 (100% POST 방식)
- **검색 기능**: 강의명, 교수명, 학과별 필터링
- **수강 관리**: 수강 신청, 취소, 대기열 관리
- **백엔드 필터링** ⭐: 학생 전공/부전공 기반 자동 필터링
- **권한 제어**: 교수/학생 역할별 접근 제어
- **통일된 통신**: 모든 API가 Request Body 기반 ⭐ NEW

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

## 📋 최신 업데이트

### 🆕 v5.0 - lecIdx → lecSerial 마이그레이션 (2025-10-17)

#### 식별자 마이그레이션 완료
**핵심 개념**: "백엔드 로직은 기본적으로 IDX로 돌아가지만, 그 IDX를 강의코드(lecSerial)를 이용해서 해당하는 IDX를 추출해서 활용"

```
프론트엔드 (lecSerial: "CS101")
    ↓
Controller: lecSerial 받음
    ↓
Service: lectureService.getLectureBySerial(lecSerial) → lecIdx 추출
    ↓
Repository/DB: lecIdx로 처리 (기존 로직 유지)
    ↓
Response: @JsonIgnore로 lecIdx 숨김, lecSerial만 반환
```

#### 완료된 작업
**백엔드 (8개 파일)**:
- ✅ DTO에 @JsonIgnore 추가 (LectureDto, EnrollmentDto, AssignmentDto)
- ✅ Controller 변환 로직 (LectureController, EnrollmentController, AssignmentController)
- ✅ Service 헬퍼 메서드 (EnrollmentService, AssignmentService)

**프론트엔드 (5개 파일)**:
- ✅ lecture-test-1-admin-create.js (6개 함수 수정)
- ✅ lecture-test-2a-student-enrollment.js
- ✅ lecture-test-2b-student-my-courses.js
- ✅ lecture-test-4a-professor-assignment-create.js (3개 함수 수정)
- ✅ lecture-test-5-professor-students.js (2개 함수 수정)

**변경 패턴**:
```javascript
// BEFORE
const lecIdx = parseInt(prompt('LECTURE_IDX:', '1'));
body: JSON.stringify({ lecIdx })

// AFTER
const lecSerial = prompt('강의 코드 (예: CS101):', 'CS101');
body: JSON.stringify({ lecSerial })
```

**상세 문서**: [`브라우저콘솔테스트/MIGRATION_COMPLETE_SUMMARY.md`](./브라우저콘솔테스트/MIGRATION_COMPLETE_SUMMARY.md)

---

### 🆕 v4.0 - POST 방식 완전 통일 (2025-10-16)

#### API 통신 방식 표준화
**모든 API를 POST 방식으로 통일**:
- ✅ LectureController: 7개 엔드포인트 POST 통일
- ✅ EnrollmentController: 7개 엔드포인트 POST 통일
- ✅ AssignmentController: 9개 엔드포인트 POST 통일
- ✅ AttendanceController: 모든 엔드포인트 POST 통일

**주요 변경사항**:
```javascript
// 이전 (다양한 방식)
GET /api/lectures?professor=P001
PUT /api/lectures/{id}
DELETE /api/lectures/{id}

// 현재 (POST 통일)
POST /api/lectures       Body: {professor: "P001"}
POST /api/lectures/update Body: {lecIdx: 10, lecTit: "..."}
POST /api/lectures/delete Body: {lecIdx: 10}
```

**장점**:
- 🎯 일관된 통신 방식
- 🔒 보안 강화 (URL에 파라미터 노출 없음)
- 📦 복잡한 데이터 전송 용이
- 🧪 테스트 코드 표준화

---

### 🆕 v3.0 - 백엔드 필터링 완전 구현 (2025-10-16)

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
**최신 버전**: v4.0 (POST 방식 완전 통일 + 백엔드 필터링 완전 구현)
