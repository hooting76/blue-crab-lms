# 강의 관련 API 문서 (신판)

## 📋 개요

BlueCrab LMS 시스템의 강의 관련 API 문서입니다. 이 문서는 2025년 10월 26일 기준으로 작성되었으며, 현재 운영 중인 API의 정확한 엔드포인트와 요청/응답 형식을 정리합니다.

## 🎯 문서 목적

- 중대한 오류 발견으로 인한 재문서화 작업
- API 엔드포인트 및 요청/응답 형식의 명확한 정리
- DB 스키마와 API 연동 관계 명시
- 개발자를 위한 실용적인 레퍼런스 제공

## 📂 문서 구조

### 1. API 목록
- [01_강의_API.md](./01_강의_API.md) - 강의 조회, 검색, 생성, 수정, 삭제
- [02_수강신청_API.md](./02_수강신청_API.md) - 수강신청 조회, 등록, 취소, 통계
- [03_과제_API.md](./03_과제_API.md) - 과제 조회, 생성, 제출, 채점
- [04_출석_API.md](./04_출석_API.md) - 출석 체크, 요청, 승인, 조회
- [05_공지사항_API.md](./05_공지사항_API.md) - 수강신청 안내문 조회 및 저장
- [06_성적_API.md](./06_성적_API.md) - 성적 구성 설정, 조회, 계산, 최종 등급 배정

### 2. DB 스키마 연관
- [DB_스키마_매핑.md](./DB_스키마_매핑.md) - 각 API와 연관된 테이블 매핑 정보

### 3. 엔티티 관계
- [엔티티_관계도.md](./엔티티_관계도.md) - JPA 엔티티 간 관계 및 참조 구조

## 🔑 주요 특징

### API 설계 원칙
- **POST 기반 통합 엔드포인트**: 대부분의 조회/검색 API는 POST 방식 사용
- **Request Body 기반 파라미터**: Query Parameter 대신 JSON Body로 필터 조건 전달
- **lecSerial 우선 사용**: lecIdx 대신 강의 코드(lecSerial) 사용 권장
- **페이징 지원**: `page`, `size` 파라미터로 페이징 처리

### 공통 응답 형식
```json
{
  "success": true,
  "message": "성공 메시지",
  "data": { /* 실제 데이터 */ }
}
```

### 오류 응답 형식
```json
{
  "success": false,
  "message": "오류 메시지"
}
```

## 📊 DB 테이블 목록

### 강의 관련
- `LEC_TBL` - 강의 정보
- `FACULTY` - 학부 정보
- `DEPARTMENT` - 학과 정보

### 수강신청 관련
- `ENROLLMENT_EXTENDED_TBL` - 수강신청 정보

### 과제 관련
- `ASSIGNMENT_EXTENDED_TBL` - 과제 정보

### 출석 관련
- `ATTENDANCE_REQUEST_TBL` - 출석 요청 정보

### 공지사항 관련
- `COURSE_APPLY_NOTICE` - 수강신청 안내문

### 사용자 관련
- `USER_TBL` - 사용자 정보
- `PROFILE_VIEW` - 프로필 뷰

## 🚀 빠른 시작

### 1. 강의 목록 조회
```bash
POST /api/lectures
Content-Type: application/json

{
  "page": 0,
  "size": 20
}
```

### 2. 특정 강의 조회
```bash
POST /api/lectures
Content-Type: application/json

{
  "serial": "CS284"
}
```

### 3. 수강 여부 확인
```bash
POST /api/enrollments/list
Content-Type: application/json

{
  "studentIdx": 1,
  "lecSerial": "CS284",
  "checkEnrollment": true
}
```

### 4. 학생 성적 조회
```bash
POST /api/enrollments/grade-info
Content-Type: application/json

{
  "action": "get-grade",
  "lecSerial": "CS284",
  "studentIdx": 6
}
```

## 📝 버전 정보

- **문서 버전**: 1.0.0
- **작성일**: 2025-10-26
- **작성자**: BlueCrab Development Team
- **최종 수정일**: 2025-10-26

## ⚠️ 주의사항

1. **lecIdx vs lecSerial**: 가능한 경우 `lecSerial` (강의 코드) 사용을 권장합니다.
2. **인증 필요**: 대부분의 API는 JWT 토큰 인증이 필요합니다.
3. **권한 확인**: 교수/학생 권한에 따라 접근 가능한 API가 다릅니다.
4. **페이징**: 대량 데이터 조회 시 반드시 페이징 파라미터를 사용하세요.

## 📞 문의

API 관련 문의사항이나 버그 리포트는 개발팀에 문의하시기 바랍니다.

---

© 2025 BlueCrab LMS Development Team. All rights reserved.
