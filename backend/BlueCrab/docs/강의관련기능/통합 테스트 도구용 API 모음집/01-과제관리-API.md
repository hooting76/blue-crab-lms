# 📝 과제 관리 API 명세서

> **Base URL**: `/api/assignments`
> 
> **작성일**: 2025-10-24
> 
> **컨트롤러**: `AssignmentController.java`

---

## 📋 목차
1. [과제 목록 조회](#1-과제-목록-조회)
2. [과제 상세 조회](#2-과제-상세-조회)
3. [과제 데이터 조회](#3-과제-데이터-조회)
4. [제출 현황 조회](#4-제출-현황-조회)
5. [과제 생성](#5-과제-생성)
6. [과제 제출](#6-과제-제출)
7. [과제 수정](#7-과제-수정)
8. [과제 채점](#8-과제-채점)
9. [과제 삭제](#9-과제-삭제)

---

## 1. 과제 목록 조회

### `POST /api/assignments/list`

과제 목록을 조회합니다. 강의별 필터, 통계 조회, 페이징을 지원합니다.

#### Request Body
```json
{
  "lecSerial": "ETH201",        // [선택] 강의 코드 (필터)
  "withLecture": false,         // [선택] 강의 정보 포함 여부
  "stats": false,               // [선택] 통계 조회 여부
  "action": "list",             // [선택] 액션 타입
  "page": 0,                    // [선택] 페이지 번호 (0부터 시작)
  "size": 20                    // [선택] 페이지 크기
}
```

#### Response (목록 조회)
```json
{
  "content": [
    {
      "assignmentIdx": 6,
      "lecIdx": 48,
      "assignmentData": "{...}",
      "assignmentName": "실험실을 박살내기",
      "maxScore": 10,
      "dueDate": "20251231",
      "createdDate": "2025-10-24T16:52:23"
    }
  ],
  "pageable": {...},
  "totalElements": 4,
  "totalPages": 1,
  "size": 20,
  "number": 0
}
```

#### Response (통계 조회)
```json
{
  "assignmentCount": 4,
  "lecIdx": 48
}
```

#### 테스트 예시 (브라우저 콘솔)
```javascript
await apiCall('/api/assignments/list', 'POST', {
    lecSerial: 'ETH201',
    page: 0,
    size: 20
});
```

---

## 2. 과제 상세 조회

### `POST /api/assignments/detail`

특정 과제의 상세 정보를 조회합니다.

#### Request Body
```json
{
  "assignmentIdx": 6,           // [필수] 과제 IDX
  "action": "detail"            // [선택] 액션 타입
}
```

#### Response
```json
{
  "assignmentIdx": 6,
  "lecIdx": 48,
  "assignmentData": "{\"submissions\":[...],\"assignment\":{...}}",
  "assignmentName": "실험실을 박살내기",
  "maxScore": 10,
  "dueDate": "20251231",
  "createdDate": "2025-10-24T16:52:23"
}
```

---

## 3. 과제 데이터 조회

### `POST /api/assignments/data`

과제의 JSON 데이터를 파싱하여 반환합니다.

#### Request Body
```json
{
  "assignmentIdx": 6,           // [필수] 과제 IDX
  "action": "data"              // [선택] 액션 타입
}
```

#### Response
```json
{
  "assignment": {
    "title": "실험실을 박살내기",
    "description": "실험실을 박살내줘요.",
    "dueDate": "20251231",
    "maxScore": 10
  },
  "submissions": [
    {
      "studentIdx": 6,
      "score": 9,
      "feedback": "잘 작성되었습니다.",
      "submittedAt": "2025-10-24 16:53:23",
      "gradedAt": "2025-10-24 16:53:23",
      "submissionMethod": "offline"
    }
  ]
}
```

---

## 4. 제출 현황 조회

### `POST /api/assignments/submissions`

과제 제출 현황을 학생별로 조회합니다 (교수 전용).

#### Request Body
```json
{
  "assignmentIdx": 6,           // [필수] 과제 IDX
  "page": 0,                    // [선택] 페이지 번호
  "size": 10,                   // [선택] 페이지 크기
  "action": "get_submissions"   // [선택] 액션 타입
}
```

#### Response
```json
{
  "content": [
    {
      "studentIdx": 6,
      "studentId": "20241234",
      "studentName": "김철수",
      "submitted": true,
      "graded": true,
      "score": 9,
      "maxScore": 10,
      "submissionMethod": "offline",
      "submittedAt": "2025-10-24 16:53:23"
    }
  ],
  "totalElements": 15,
  "totalPages": 2
}
```

---

## 5. 과제 생성

### `POST /api/assignments`

새로운 과제를 생성합니다 (교수 전용).

#### Request Body
```json
{
  "lecSerial": "ETH201",        // [필수] 강의 코드
  "title": "중간고사",          // [필수] 과제 제목
  "body": "중간고사입니다.",    // [선택] 과제 설명
  "dueDate": "20251231",        // [필수] 마감일 (YYYYMMDD)
  "maxScore": 30                // [선택] 만점 (기본값: 10)
}
```

#### Response (성공)
```json
{
  "assignmentIdx": 9,
  "lecIdx": 48,
  "assignmentData": "{\"submissions\":[],\"assignment\":{...}}"
}
```

#### Response (실패)
```json
{
  "success": false,
  "message": "maxScore는 0보다 커야 합니다."
}
```

#### 테스트 예시 (브라우저 콘솔)
```javascript
await apiCall('/api/assignments', 'POST', {
    lecSerial: 'ETH201',
    title: '중간고사',
    body: '중간고사입니다.',
    dueDate: '20251231',
    maxScore: 30
});
```

---

## 6. 과제 제출

### `POST /api/assignments/{assignmentIdx}/submit`

학생이 과제를 제출합니다.

#### Request Body
```json
{
  "studentIdx": 6,              // [필수] 학생 IDX
  "content": "과제 내용...",    // [필수] 제출 내용
  "fileUrl": "http://..."       // [선택] 첨부 파일 URL
}
```

#### Response
```json
{
  "success": true,
  "message": "과제가 제출되었습니다.",
  "data": {
    "assignmentIdx": 6,
    "lecIdx": 48,
    "assignmentData": "{...}"
  }
}
```

---

## 7. 과제 수정

### `PUT /api/assignments/{assignmentIdx}`

과제 정보를 수정합니다 (교수 전용).

#### Request Body
```json
{
  "title": "새 제목",           // [선택] 수정할 제목
  "body": "새 설명",            // [선택] 수정할 설명
  "dueDate": "20251231",        // [선택] 수정할 마감일
  "maxScore": 30                // [선택] 수정할 만점
}
```

#### Response
```json
{
  "success": true,
  "message": "과제가 수정되었습니다.",
  "data": {
    "assignmentIdx": 6,
    "lecIdx": 48,
    "assignmentData": "{...}"
  }
}
```

---

## 8. 과제 채점

### `PUT /api/assignments/{assignmentIdx}/grade`

과제를 채점합니다 (교수 전용). 오프라인 제출 방식을 지원합니다.

#### Request Body
```json
{
  "studentIdx": 6,              // [필수] 학생 IDX
  "score": 27,                  // [필수] 부여할 점수
  "feedback": "잘 작성되었습니다."  // [선택] 평가 코멘트
}
```

#### 동작 방식
1. 과제의 `maxScore` 조회
2. 입력 점수가 `maxScore`를 초과하면 **자동으로 maxScore로 변환**
3. 학생 submission이 없으면 자동 생성 (오프라인 제출 방식)
4. 성적 재계산 이벤트 발행

#### Response
```json
{
  "assignmentIdx": 6,
  "lecIdx": 48,
  "assignmentData": "{\"submissions\":[{\"studentIdx\":6,\"score\":27,...}],...}"
}
```

#### 백엔드 로그 확인 사항
```
✅ 점수 35점이 만점 30점으로 변환됨 (assignmentIdx=9, studentIdx=6)
✅ 오프라인 제출 학생 submission 생성: assignmentIdx=9, studentIdx=6, score=30
✅ 과제 채점으로 인한 성적 재계산 이벤트 발행: lecIdx=48, studentIdx=6
✅ 성적 재계산 시작: lecIdx=48, studentIdx=6
✅ 성적 재계산 완료
```

#### 테스트 예시 (브라우저 콘솔)
```javascript
await apiCall('/api/assignments/6/grade', 'PUT', {
    studentIdx: 1,
    score: 35,  // maxScore 30이면 자동으로 30으로 변환됨
    feedback: '잘 작성되었습니다.'
});
```

---

## 9. 과제 삭제

### `DELETE /api/assignments/{assignmentIdx}`

과제를 삭제합니다 (교수 전용).

#### Request
- URL Parameter: `assignmentIdx` (과제 IDX)

#### Response
```json
{
  "success": true,
  "message": "과제가 삭제되었습니다."
}
```

---

## 🔧 공통 에러 응답

### 400 Bad Request
```json
{
  "success": false,
  "message": "lecSerial, title, dueDate는 필수입니다."
}
```

### 404 Not Found
```json
{
  "success": false,
  "message": "존재하지 않는 과제입니다: 999"
}
```

### 500 Internal Server Error
```json
{
  "success": false,
  "message": "과제 생성 중 오류가 발생했습니다."
}
```

---

## 📊 주요 비즈니스 로직

### 1. maxScore 동적 제한
- 과제 생성 시 `maxScore` 설정 가능 (기본값: 10)
- 채점 시 입력 점수가 `maxScore` 초과하면 자동으로 `maxScore`로 변환
- 로그: `"점수 35점이 만점 30점으로 변환됨"`

### 2. 오프라인 제출 자동 생성
- 학생이 시스템에 제출하지 않고 오프라인으로 제출한 경우
- 교수가 채점 시 `submission` 자동 생성
- `submissionMethod: "offline"` 표시

### 3. 성적 자동 재계산
- 과제 채점 완료 시 `GradeUpdateEvent` 발행
- `GradeUpdateEventListener`가 비동기로 성적 재계산
- `@Transactional(propagation = REQUIRES_NEW)` 보장

---

## 🧪 통합 테스트 스크립트

### 과제 생성 → 채점 → 성적 확인 (전체 플로우)

```javascript
// 1. 로그인
await loginProfessor();

// 2. 과제 생성 (30점 만점)
const result = await apiCall('/api/assignments', 'POST', {
    lecSerial: 'ETH201',
    title: '중간고사',
    body: '중간고사입니다.',
    dueDate: '20251231',
    maxScore: 30
});
const assignmentIdx = result.data.assignmentIdx;

// 3. 과제 채점 (35점 입력 → 30점으로 자동 변환)
await apiCall(`/api/assignments/${assignmentIdx}/grade`, 'PUT', {
    studentIdx: 6,
    score: 35,  // 만점 초과
    feedback: '잘 작성되었습니다.'
});

// 4. 성적 확인 (3초 후)
await sleep(3000);
await apiCall('/api/enrollments/grade-info', 'POST', {
    action: 'get-grade',
    lecSerial: 'ETH201',
    studentIdx: 6
});
```

---

## 📌 참고 사항

1. **인증 필요**: 모든 API는 `Authorization: Bearer {token}` 헤더 필요
2. **권한 구분**:
   - 학생: 과제 조회, 제출
   - 교수: 모든 기능 (생성, 수정, 삭제, 채점)
3. **JSON 필드**: `assignmentData`는 JSON 문자열로 저장됨 (압축 형식)
4. **성적 재계산**: 채점 후 3초 대기하여 비동기 처리 완료 확인

---

**작성자**: GitHub Copilot  
**최종 수정**: 2025-10-24
