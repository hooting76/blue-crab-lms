# 강의 관리 시스템 가이드

> **최종 업데이트**: 2025-10-16  
> **현재 버전**: v2.6 (수강 가능 강의 조회 API 추가)  
> **프로젝트 현황**: Blue Crab LMS - 강의 관리 및 시기 관리 시스템 완료

---

## 🎯 시스템 개요

Blue Crab LMS의 강의 관리 시스템은 대학교 LMS에서 강의와 관련된 모든 기능을 통합 관리합니다.

### 핵심 특징
- ✅ **3단계 시기 관리**: 방학/수강신청/개강 자동 전환
- ✅ **0값 규칙**: 제한없음 표시 (학부=0, 학과=0, 학년=0)
- ✅ **수강 가능성 검증**: 실시간 자격 요건 확인
- ✅ **완전한 REST API**: 표준 CRUD + 고급 검색 기능

---

## 📚 문서 구조

```
📁 docs/강의관련기능/
├── 📄 README.md                    ← 현재 파일 (시작점)
├── 📄 IMPLEMENTATION_PROGRESS.md   ← 개발 진척도 및 로드맵
├── 📄 API_CONTROLLER_MAPPING.md    ← API 엔드포인트 전체 명세
├── 📄 LECTURE_API_ENDPOINTS.md     ← 강의 API 상세 사용법
├── 📁 시기관련/                    ← 학기 시스템 관련 문서
│   ├── 📄 semester-system-flow.drawio ← 시기 플로우 다이어그램
│   ├── 📄 시기관련_시스템_기능.md     ← 시기 관리 상세 명세
│   └── 📄 0값규칙_상세명세.md         ← 제한없음 규칙 설명
└── 📁 테스트/                      ← 테스트 파일 및 가이드
    ├── 📄 lecture-test-eligible-lectures.js ← 수강 가능 강의 조회 테스트
    └── 📄 browser-console-testing-guide.md  ← 브라우저 테스트 가이드
```

---

## 🚀 빠른 시작

### 1. API 확인
가장 먼저 [`API_CONTROLLER_MAPPING.md`](./API_CONTROLLER_MAPPING.md)에서 전체 API 구조를 확인하세요.

### 2. 강의 API 사용
[`LECTURE_API_ENDPOINTS.md`](./LECTURE_API_ENDPOINTS.md)에서 강의 관련 API 사용법을 확인하세요.

### 3. 시기 시스템 이해
[`시기관련/`](./시기관련/) 폴더에서 학기 시스템의 작동 원리를 확인하세요.

### 4. 테스트 실행
[`테스트/`](./테스트/) 폴더의 브라우저 콘솔 테스트로 API를 검증하세요.

---

## 🔧 주요 기능

### 강의 관리
- **기본 CRUD**: 강의 생성, 조회, 수정, 삭제
- **검색 기능**: 강의명, 교수명, 학과별 필터링
- **수강 관리**: 수강 신청, 취소, 대기열 관리
- **권한 제어**: 교수/학생 역할별 접근 제어

### 시기 관리
- **자동 전환**: 방학 → 수강신청 → 개강 자동 감지
- **관리자 제어**: 수동 시기 변경 가능
- **알림 시스템**: 시기 변경 시 FCM 푸시 알림

### 0값 규칙
- **학부**: `LEC_MCODE="0"` → 모든 학부 수강 가능
- **학과**: `LEC_DCODE="0"` → 모든 학과 수강 가능  
- **학년**: `LEC_YEAR=0` → 모든 학년 수강 가능

---

## 📋 최신 업데이트 (v2.6)

### 새로운 API
```
GET /api/lectures/eligible/{studentId}
```
학생이 수강 가능한 모든 강의를 조회합니다.

#### 주요 특징
- **0값 규칙 적용**: 제한없는 강의 자동 포함
- **자격 검증**: 학부/학과/학년 요건 확인
- **상세 응답**: 수강 가능 이유, 통계 정보 포함
- **페이징 지원**: 대량 데이터 효율적 처리

#### 응답 예시
```json
{
  "success": true,
  "data": {
    "lectures": [
      {
        "lecIdx": 1,
        "lecName": "컴퓨터과학개론",
        "professorName": "김교수",
        "lecMcode": "0",
        "lecDcode": "CS",
        "lecYear": 1,
        "eligibilityReason": "학부 제한 없음 (MCODE=0)"
      }
    ],
    "totalCount": 15,
    "eligibleCount": 12,
    "restrictedCount": 3
  }
}
```

---

## 🔍 개발 진행 상황

현재 **Phase 8 완료** 상태이며, 모든 핵심 기능이 구현되었습니다.

| Phase | 상태 | 완료율 |
|-------|------|--------|
| Phase 1-6 | ✅ 완료 | 100% |
| Phase 7.1 | ✅ 완료 | 100% |
| Phase 8 | ✅ 완료 | 100% |
| **전체** | **✅ 완료** | **99%** |

자세한 진행 상황은 [`IMPLEMENTATION_PROGRESS.md`](./IMPLEMENTATION_PROGRESS.md)를 참고하세요.

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
- **API 문서**: [`LECTURE_API_ENDPOINTS.md`](./LECTURE_API_ENDPOINTS.md) 참고
- **버그 리포트**: 테스트 파일로 재현 후 리포트

---

**최종 수정일**: 2025-10-16
