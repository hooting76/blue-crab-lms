# 성적확인서 API

학생이 본인의 전체 수강 이력과 성적을 조회할 수 있는 API

**작성일**: 2025-10-29  
**버전**: 1.0.0

---

## 📋 개요

### 주요 기능

- ✅ 학생 본인의 전체 수강 이력 조회
- ✅ 수료/중도포기/낙제 상태 구분
- ✅ 이수학점, 성적(A~F), GPA(4.0 만점) 표시
- ✅ 학기별 통계 (학기 GPA, 취득 학점)
- ✅ 전체 통계 (누적 GPA, 총 취득 학점)

### 성적 체계

- **등급**: A, B, C, D, F (5단계)
- **GPA**: 4.0 만점 (A=4.0, B=3.0, C=2.0, D=1.0, F=0.0)
- **상태**: COMPLETED, FAILED, IN_PROGRESS, NOT_GRADED, DROPPED

---

## 📡 API 엔드포인트

### 성적확인서 조회

```http
POST /api/transcript/view
Authorization: Bearer {JWT_TOKEN}
```

**요청**:

```json
{
  "studentIdx": 2
}
```

**응답**:

```json
{
  "status": "success",
  "message": "성적확인서 조회 성공",
  "data": {
    "student": { "..." : "..." },
    "courses": [ "..." ],
    "semesterSummaries": { "..." : "..." },
    "overallSummary": { "..." : "..." },
    "issuedAt": "2025-10-29T14:35:22",
    "certificateNumber": "TR-202500106114-20251029143522"
  }
}
```

### 헬스 체크

```http
GET /api/transcript/health
```

---

## 📚 상세 문서

### 1. [API 응답 형식](./API-응답형식.md)

- 성공/실패 응답 예시
- 전체 필드 설명
- HTTP 상태 코드

### 2. [데이터베이스 스키마](./데이터베이스-스키마.md)

- ENROLLMENT_DATA JSON 구조
- 관련 테이블 (USER_TBL, LEC_TBL, ENROLLMENT_EXTENDED_TBL)

### 3. [비즈니스 로직](./비즈니스-로직.md)

- 성적 상태 판정
- GPA 계산 로직
- 학기별 통계 계산

### 4. [프론트엔드 가이드](./프론트엔드-가이드.md)

- 인증 처리
- UI 구성 제안
- 데이터 바인딩 방법

---

## 🧪 테스트

### 브라우저 콘솔 테스트

```javascript
// 1. 로그인 (user-login.js)
await login();

// 2. 성적확인서 조회
await viewTranscript();
```

**테스트 파일**: `transcript-test.js`

---

## 🔒 보안

- **JWT 인증**: 모든 요청에 토큰 필수
- **본인 확인**: 학생은 본인 성적만 조회 가능
- **권한 체크**: 토큰의 USER_IDX와 요청 studentIdx 일치 확인

---

## 📂 파일 구조

```
성적확인서 API/
├── README.md                    (이 파일)
├── API-응답형식.md              (응답 예시 및 필드 설명)
├── 데이터베이스-스키마.md        (DB 구조)
├── 비즈니스-로직.md             (계산 로직)
├── 프론트엔드-가이드.md          (UI 구현 가이드)
├── 설계문서.md                  (전체 설계 문서 - 통합본)
└── transcript-test.js           (브라우저 콘솔 테스트)
```

---

## 🚀 구현 상태

- [x] TranscriptController.java
- [x] TranscriptService.java
- [x] CourseDto.java
- [x] TranscriptResponseDto.java
- [x] 브라우저 콘솔 테스트 코드
- [x] API 문서
- [ ] 단위 테스트
- [ ] 프론트엔드 구현

---
