# 📢 안내문 API 문서

수강신청 안내문 시스템 관련 문서 모음입니다.

---

## 📑 문서 목록

### 1. API 명세서
**파일**: [03-안내문API명세.md](./03-안내문API명세.md)

수강신청 안내문 API의 기본 사양과 엔드포인트 정의:
- 기본 정보 (Base URL, 인증 방식)
- API 엔드포인트 (조회 /view, 저장 /save)
- 요청/응답 형식 (DTO 구조)
- 보안 설정 (public/authenticated)
- 예외 처리 및 상태 코드

**대상 독자**: 프론트엔드 개발자, API 연동 담당자

### 2. 구현 가이드
**파일**: [07-안내문API구현가이드.md](./07-안내문API구현가이드.md)

백엔드 구현 완료 가이드 (5 Layers):
- Database 레이어 (DDL 스크립트)
- Entity 레이어 (JPA 엔티티, 라이프사이클 콜백)
- Repository 레이어 (단일 레코드 조회)
- DTO 레이어 (View/Save Request/Response)
- Controller 레이어 (REST 엔드포인트, 보안)
- 패키지 구조 (BlueCrab.com.example.*.Lecture)
- 테스트 방법 (브라우저 콘솔, curl)

**대상 독자**: 백엔드 개발자, 시스템 유지보수 담당자

---

## 🎯 빠른 시작

### 프론트엔드 개발자
1. [03-안내문API명세.md](./03-안내문API명세.md) 읽기
2. 브라우저 콘솔 테스트 실행: [../../브라우저콘솔테스트/05-notice/](../../../../브라우저콘솔테스트/05-notice/)
3. 프론트엔드 컴포넌트에 연동

### 백엔드 개발자
1. [07-안내문API구현가이드.md](./07-안내문API구현가이드.md) 읽기
2. DDL 스크립트 확인: `docs/ddl/course_apply_notice.sql`
3. 소스 코드 확인: `entity/Lecture/`, `repository/Lecture/`, `dto/Lecture/`, `controller/Lecture/`

---

## 🏗️ 시스템 개요

### 주요 특징
- **단일 레코드 관리**: 최신 안내문 1개만 유지 (Upsert 패턴)
- **공개 조회**: 인증 없이 누구나 안내문 조회 가능
- **제한적 수정**: ADMIN, PROFESSOR만 안내문 저장 가능
- **자동 타임스탬프**: JPA 라이프사이클로 생성/수정 시각 자동 관리
- **단순 구조**: 메시지 텍스트만 저장 (제목, 카테고리, 첨부파일 없음)

### 기술 스택
- Spring Boot 2.x
- Spring Data JPA (javax.persistence)
- Spring Security JWT
- MySQL 8.0+
- Lombok

### 엔드포인트
```
POST /notice/course-apply/view   → 안내문 조회 (public)
POST /notice/course-apply/save   → 안내문 저장 (authenticated)
```

---

## 📊 문서 버전 관리

| 날짜 | 버전 | 작성자 | 변경 내용 |
|------|------|--------|-----------|
| 2025.01.05 | 1.0 | AI Assistant | 초기 작성 (API 명세, 구현 가이드) |
| 2025.01.05 | 1.1 | AI Assistant | 문서 구조 재정리 (안내문 폴더 분리) |
| 2025.01.05 | 1.2 | AI Assistant | ✅ 테스트 완료 (브라우저 콘솔 테스트 성공) |

---

## ✅ 테스트 결과 (2025.01.05)

### 통합 테스트 완료

**테스트 환경:**
- URL: `https://bluecrab.chickenkiller.com/BlueCrab-1.0.0`
- 계정: 교수 계정 (prof.jellyfish@univ.edu, userStudent=1)
- 도구: 브라우저 콘솔 테스트 스크립트

**테스트 항목:**

| 항목 | 결과 | 비고 |
|------|------|------|
| 🔓 공개 조회 API | ✅ 성공 | 인증 없이 정상 작동 |
| 🔐 저장 API (교수) | ✅ 성공 | JWT 인증 정상 작동 |
| 📝 프롬프트 입력 | ✅ 성공 | 사용자 입력 UI 정상 작동 |
| 🔄 Upsert 패턴 | ✅ 성공 | 단일 레코드 업데이트 확인 |
| ⏰ 자동 타임스탬프 | ✅ 성공 | updatedAt/updatedBy 자동 설정 |
| 🚫 권한 검증 | ✅ 성공 | 교수 권한 정상 확인 |

**주요 수정 사항:**
- API URL 수정: `/api/notice/course-apply/*` → `/notice/course-apply/*`
- 프롬프트 입력 기능 추가
- 삭제 확인 다이얼로그 추가

**결론:** 🎉 **안내문 API 시스템 정상 작동 확인**

---

## 🔗 관련 문서

- **종합 가이드**: [../../가이드문서/00-README.md](../../가이드문서/00-README.md)
- **브라우저 콘솔 테스트**: [../../../../브라우저콘솔테스트/05-notice/](../../../../브라우저콘솔테스트/05-notice/)
- **DDL 스크립트**: `backend/BlueCrab/docs/ddl/course_apply_notice.sql`
- **백엔드 답변 목록**: [../프런트팀의 요청/백엔드답변/README.md](../프런트팀의 요청/백엔드답변/README.md)

---

## 💡 문의 및 지원

문서 관련 질문이나 API 사용 중 문제가 발생하면 백엔드 팀에 문의하세요.

- API 연동 문제: [03-안내문API명세.md](./03-안내문API명세.md) 참조
- 백엔드 구현 질문: [07-안내문API구현가이드.md](./07-안내문API구현가이드.md) 참조
- 테스트 방법: [브라우저 콘솔 테스트](../../../../브라우저콘솔테스트/05-notice/) 참조
