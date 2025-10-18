# 강의 관리 시스템 API 가이드

> **작성일**: 2025-10-17
> **버전**: 2.3 (테스트 코드 우선 참고 안내 추가)
> **대상**: 프론트엔드 담당자
> **작성자**: 백엔드 담당자 (성태준)

---

## ⚠️ 중요 주의사항

### 🔴 문서 불일치 시 우선순위

**마크다운 문서와 실제 구현 간 불일치가 있을 경우:**

1. **최우선 참고**: `브라우저콘솔테스트/` 디렉토리의 **테스트 코드**
   - 실제 백엔드 API와 통신하여 검증된 코드
   - 실제 요청/응답 구조가 그대로 반영됨
   - 최신 API 변경사항이 실시간 반영됨

2. **참고**: Phase1~4 마크다운 문서
   - 전체적인 API 흐름 이해용
   - 일부 예시가 실제와 다를 수 있음

**📁 테스트 코드 위치**: `docs/강의관련기능/브라우저콘솔테스트/`
- `lecture-test-1-admin-create.js`: 관리자 강의 관리
- `lecture-test-2a-student-enrollment.js`: 학생 수강신청
- `lecture-test-2b-student-my-courses.js`: 학생 수강 관리
- `lecture-test-3-student-assignment.js`: 학생 과제 제출
- `lecture-test-4a-professor-assignment-create.js`: 교수 과제 생성
- `lecture-test-4b-professor-assignment-grade.js`: 교수 과제 채점
- `lecture-test-5-professor-students.js`: 교수 수강생 관리
- `lecture-test-6-admin-statistics.js`: 관리자 통계

### ⚡ 주요 불일치 가능 항목

1. **요청 필드명**: 문서에서 설명한 필드명과 실제 API가 다를 수 있음
   - 예: `professor` (문서) vs 실제 USER_IDX 숫자
   
2. **응답 구조**: ApiResponse 래퍼 사용 여부
   - 일부 API는 `{ success, data }` 구조
   - 일부 API는 엔티티 직접 반환
   - 테스트 코드에서 실제 구조 확인 필수

3. **필드 타입**: 문자열 vs 숫자
   - 예: `lecProf`는 숫자 문자열 "22" (USER_IDX)
   - 예: `lecProfName`은 문자열 "김교수" (조회된 이름)

**💡 권장 개발 프로세스**:
```
1. Phase 마크다운으로 전체 흐름 파악
2. 브라우저 콘솔 테스트 코드로 실제 API 확인
3. 테스트 코드의 요청/응답 구조를 프론트엔드에 적용
```

---

## 📋 문서 목적

본 문서는 Blue Crab LMS 강의 관리 시스템의 백엔드 API를 프론트엔드 담당자와의 효율적인 협업을 위해 작성되었습니다.

- **주요 대상**: 프론트엔드 개발자, UI/UX 디자이너
- **목적**: API 명세서, 요청/응답 예시, 데이터 플로우 제공
- **특징**: 단계별 분할로 이해하기 쉽게 구성
- **검증**: DB 스키마와 API 파라미터 일치성 검증 완료

## 📚 문서 구조

### 단계별 API 가이드
1. **[Phase1_Lecture_Preparation.md](Phase1_Lecture_Preparation.md)**: 학기 준비 단계
   - 강의 등록/관리 API
   - 강의 목록 조회 API
   - 강의 통계 API

2. **[Phase2_Enrollment_Process.md](Phase2_Enrollment_Process.md)**: 수강신청 단계
   - 수강 가능 강의 조회 API
   - 수강신청 API
   - 수강목록 관리 API

3. **[Phase3_Semester_Progress.md](Phase3_Semester_Progress.md)**: 학기 진행 단계
   - 출결 관리 API
   - 과제 관리 API
   - 학생 목록 조회 API

4. **[Phase4_Evaluation_Closure.md](Phase4_Evaluation_Closure.md)**: 평가 및 종료 단계
   - 성적 관리 API
   - 강의평가 API
   - 최종 통계 API

### 참고 문서
- `요구사항분석.md`: 상세 요구사항 및 비즈니스 로직
- `ERD_Diagram.drawio`: 데이터베이스 구조
- `브라우저콘솔테스트/`: 실제 API 테스트 코드

## 🔧 공통 사항

### API 기본 정보
- **프로젝트 Base URL**: `https://bluecrab.chickenkiller.com/BlueCrab-1.0.0`
- **강의 관련 API 경로**: 본 문서의 모든 엔드포인트는 `/api/`로 시작
  - 예: `/api/lectures`, `/api/enrollments/enroll`, `/api/assignments`
  - 전체 URL 예시: `https://bluecrab.chickenkiller.com/BlueCrab-1.0.0/api/lectures`
- **인증**: JWT Bearer Token (Authorization 헤더)
- **Content-Type**: `application/json`
- **HTTP Method**: 주로 POST (복합 쿼리 지원)

### 공통 응답 형식

**⚠️ 중요**: 현재 모든 API가 아래 형식을 따르지는 않습니다. 일부 엔드포인트는 엔티티/DTO를 직접 반환합니다.

**표준 응답 (일부 API만 사용)**:
```json
{
  "success": true,
  "data": { ... },
  "message": "성공 메시지",
  "timestamp": "2025-10-17T10:00:00Z"
}
```

**실제 많은 API의 응답**:
```json
{
  "lecIdx": 1,
  "lecSerial": "CS101",
  ...
}
```
또는
```json
{
  "content": [...],
  "totalElements": 100,
  "totalPages": 5
}
```

### 에러 응답 형식
```json
{
  "success": false,
  "error": "에러 코드",
  "message": "에러 메시지",
  "timestamp": "2025-10-17T10:00:00Z"
}
```

### 페이징 파라미터
```json
{
  "page": 0,     // 페이지 번호 (0부터 시작)
  "size": 20     // 페이지 크기
}
```

## 🎯 주요 데이터 모델

### 강의 정보 (LectureDto)
```json
{
  "lecIdx": 1,
  "lecSerial": "CS101",
  "lecTit": "자료구조",
  "lecProf": 22,
  "lecProfName": "김교수",
  "lecYear": 1,
  "lecSemester": 1,
  "lecMany": 30,
  "lecCurrent": 25,
  "lecPoint": 3,
  "lecTime": "월1,수1",
  "lecMcode": "01",
  "lecMcodeDep": "001"
}
```

### 수강 정보 (EnrollmentDto)
```json
{
  "enrollmentIdx": 1,
  "studentIdx": 100,
  "lecIdx": 1,
  "lecSerial": "CS101",
  "enrollmentDate": "20250301",
  "status": "ACTIVE"
}
```

### 과제 정보 (AssignmentDto)
```json
{
  "assignmentIdx": 1,
  "lecIdx": 1,
  "lecSerial": "CS101",
  "title": "과제1",
  "description": "자료구조 과제",
  "dueDate": "20250315",
  "maxScore": 100
}
```

**💡 날짜 형식**: `YYYYMMDD` 형식 사용 (예: `20250315`)

## 🚀 빠른 시작

### 1. 인증 토큰 획득
```javascript
// 로그인 후 JWT 토큰을 localStorage 또는 변수에 저장
const token = localStorage.getItem('jwtAccessToken');
```

### 2. API 호출 예시
```javascript
fetch('/api/lectures', {
  method: 'POST',
  headers: {
    'Authorization': `Bearer ${token}`,
    'Content-Type': 'application/json'
  },
  body: JSON.stringify({
    page: 0,
    size: 10
  })
})
.then(response => response.json())
.then(data => console.log(data));
```

### 3. 테스트 환경
- 브라우저 콘솔에서 테스트 가능
- `브라우저콘솔테스트/` 폴더의 JS 파일들 참고

## 📞 협업 가이드

### 백엔드 ↔ 프론트엔드 협업 포인트
1. **API 명세서 공유**: 각 단계별 문서 확인
2. **요청/응답 형식**: 예시 코드 적극 활용
3. **에러 처리**: 공통 에러 응답 형식 준수
4. **페이징**: 표준 파라미터 사용
5. **실시간 테스트**: 브라우저 콘솔로 즉시 검증

### 주의사항
- **인증 필수**: 모든 API 호출 시 JWT 토큰 포함
- **lecSerial 사용**: 강의 식별자로 lecIdx 대신 lecSerial 사용
- **POST 방식**: 복합 쿼리를 위해 POST 방식 채택
- **0값 규칙**: 학부/학과 필터링 시 "0"은 전체 허용 의미

## 🔄 업데이트 내역

- **v2.2**: LEC_SERIAL 예시 값 실제 코드에 맞게 수정 ("CS101-001-2025-1" → "CS101")
  - 모든 Phase 문서 및 README의 lecSerial 예시 일괄 수정
- **v2.1**: DB 스키마 검증 완료
  - Phase1: LEC_MAJOR, LEC_OPEN 필드 설명 정정
  - Phase2: SERIAL_CODE_TABLE 기반 수강 자격 검증 로직 수정
  - Phase3: DB 스키마와 일치하지 않는 필드 주석 처리
- **v2.0**: 단계별 문서 분할, API 명세서 상세화
- **v1.0**: 초기 통합 가이드 작성

---

**다음 단계**: [Phase1_Lecture_Preparation.md](Phase1_Lecture_Preparation.md)에서 학기 준비 단계 API를 확인하세요.</content>
<parameter name="filePath">F:\main_project\works\blue-crab-lms\backend\BlueCrab\docs\강의관련기능\README.md