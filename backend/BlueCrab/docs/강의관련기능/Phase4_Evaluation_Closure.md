# Phase 4: 평가 및 종료 단계 API 가이드

> **단계**: 평가 및 종료 (학생 + 교수 + 관리자)
> **주요 액터**: 학생, 교수, 관리자
> **목적**: 학기 종료 평가 및 결과 정리

## 📋 단계 개요

학기가 종료되는 단계로 성적 입력, 강의평가, 최종 통계 확인이 이루어집니다.

### 주요 기능
- 성적 관리 (교수: 입력, 학생: 조회)
- 강의평가 (학생: 제출, 관리자: 결과 조회)
- 최종 통계 및 보고서

---

## 🔧 API 명세서

### 1. 성적 관리: 성적 입력 (교수)

**엔드포인트**: `PUT /api/grades`

**목적**: 교수님이 학생별 성적을 입력합니다.

**Request Body**:
```json
{
  "lecSerial": "CS101",  // 강의 코드 (필수)
  "studentIdx": 100,                // 학생 ID (필수)
  "gradeType": "FINAL",             // 성적 타입: MID/MIDTERM, FINAL/FINAL_EXAM (필수)
  "score": 95,                      // 점수 (0-100, 필수)
  "grade": "A+",                    // 학점 (A+, A, B+, B, C+, C, D+, D, F, 필수)
  "notes": "우수한 성적"             // 비고 (선택)
}
```

**Response (성공)**:
```json
{
  "success": true,
  "message": "성적이 입력되었습니다.",
  "data": {
    "gradeIdx": 123,
    "lecIdx": 1,
    "studentIdx": 100,
    "gradeType": "FINAL",
    "score": 95,
    "grade": "A+",
    "notes": "우수한 성적",
    "inputBy": 22,
    "inputDate": "2025-06-20T10:00:00Z"
  }
}
```

**프론트엔드 호출 예시**:
```javascript
const gradeResponse = await fetch('/api/grades', {
  method: 'PUT',
  headers: {
    'Authorization': `Bearer ${token}`,
    'Content-Type': 'application/json'
  },
  body: JSON.stringify({
    lecSerial: currentLecture.lecSerial,
    studentIdx: selectedStudent.studentIdx,
    gradeType: "FINAL",
    score: 95,
    grade: "A+",
    notes: "프로젝트 우수"
  })
});
```

---

### 2. 성적 관리: 성적 조회 (학생)

**엔드포인트**: `GET /api/grades/my-grades`

**목적**: 학생이 자신의 성적을 조회합니다.

**Query Parameter**:
- `year`: 학년도 필터 (선택)
- `semester`: 학기 필터 (선택)

**Response (성공)**:
```json
[
  {
    "lecSerial": "CS101",
    "lecTit": "자료구조",
    "lecProfName": "김교수",
    "midtermScore": 88,
    "midtermGrade": "B+",
    "finalScore": 95,
    "finalGrade": "A+",
    "totalScore": 91.5,
    "totalGrade": "A",
    "gpa": 4.0,
    "notes": "우수한 성적"
  }
]
```

**프론트엔드 호출 예시**:
```javascript
const myGradesResponse = await fetch('/api/grades/my-grades?year=2025&semester=1', {
  method: 'GET',
  headers: {
    'Authorization': `Bearer ${token}`
  }
});

const myGrades = await myGradesResponse.json();
// 성적표 표시
```

---

### 3. 강의평가: 평가 항목 조회 (학생)

**엔드포인트**: `GET /api/evaluations/items/{lecSerial}`

**목적**: 강의평가 항목을 조회합니다.

**Path Parameter**:
- `lecSerial`: 강의 코드

**Response (성공)**:
```json
{
  "lecture": {
    "lecSerial": "CS101",
    "lecTit": "자료구조",
    "lecProfName": "김교수"
  },
  "evaluationItems": [
    {
      "itemIdx": 1,
      "question": "강의 내용이 이해하기 쉬웠습니까?",
      "type": "RATING",
      "required": true,
      "minRating": 1,
      "maxRating": 5
    },
    {
      "itemIdx": 2,
      "question": "교수님의 강의 스타일은 어떠했습니까?",
      "type": "TEXT",
      "required": false,
      "maxLength": 500
    }
  ],
  "deadline": "2025-06-30T23:59:00Z"
}
```

---

### 4. 강의평가: 평가 제출 (학생)

**엔드포인트**: `POST /api/evaluations`

**목적**: 학생이 강의평가를 제출합니다.

**Request Body**:
```json
{
  "lecSerial": "CS101",  // 강의 코드 (필수)
  "studentIdx": 100,                // 학생 ID (필수)
  "responses": [
    {
      "itemIdx": 1,
      "rating": 5,
      "text": null
    },
    {
      "itemIdx": 2,
      "rating": null,
      "text": "매우 좋았습니다. 이해하기 쉬웠어요."
    }
  ]
}
```

**Response (성공)**:
```json
{
  "success": true,
  "message": "강의평가가 제출되었습니다.",
  "data": {
    "evaluationIdx": 456,
    "lecIdx": 1,
    "studentIdx": 100,
    "submittedDate": "2025-06-25T10:00:00Z",
    "responses": [
      {
        "itemIdx": 1,
        "rating": 5
      },
      {
        "itemIdx": 2,
        "text": "매우 좋았습니다. 이해하기 쉬웠어요."
      }
    ]
  }
}
```

---

### 5. 강의평가: 평가 결과 조회 (관리자/교수)

**엔드포인트**: `GET /api/evaluations/results/{lecSerial}`

**목적**: 강의평가 결과를 통계로 조회합니다.

**Path Parameter**:
- `lecSerial`: 강의 코드

**Response (성공)**:
```json
{
  "lecture": {
    "lecSerial": "CS101",
    "lecTit": "자료구조",
    "lecProfName": "김교수"
  },
  "summary": {
    "totalResponses": 23,
    "totalStudents": 25,
    "responseRate": 92.0,
    "averageRating": 4.2
  },
  "itemResults": [
    {
      "itemIdx": 1,
      "question": "강의 내용이 이해하기 쉬웠습니까?",
      "averageRating": 4.3,
      "ratingDistribution": {
        "5": 15,
        "4": 6,
        "3": 2,
        "2": 0,
        "1": 0
      },
      "textResponses": []
    },
    {
      "itemIdx": 2,
      "question": "교수님의 강의 스타일은 어떠했습니까?",
      "averageRating": null,
      "textResponses": [
        "매우 좋았습니다.",
        "조금 빠르지만 이해하기 쉬웠어요.",
        "실습이 많아서 좋았습니다."
      ]
    }
  ]
}
```

---

### 6. 최종 통계: 학기 종합 보고서 (관리자)

**엔드포인트**: `GET /api/reports/semester-summary`

**목적**: 학기 종료 후 종합 통계 보고서를 생성합니다.

**Query Parameter**:
- `year`: 학년도 (필수)
- `semester`: 학기 (필수)

**Response (성공)**:
```json
{
  "period": {
    "year": 2025,
    "semester": 1
  },
  "lectureStats": {
    "totalLectures": 45,
    "activeLectures": 43,
    "cancelledLectures": 2,
    "averageEnrollmentRate": 87.5
  },
  "studentStats": {
    "totalStudents": 1200,
    "activeStudents": 1180,
    "averageGPA": 3.2,
    "graduationRate": 95.0
  },
  "evaluationStats": {
    "averageSatisfaction": 4.1,
    "responseRate": 89.0,
    "topRatedLectures": [
      {
        "lecSerial": "CS101",
        "lecTit": "자료구조",
        "averageRating": 4.8
      }
    ]
  },
  "generatedDate": "2025-07-01T00:00:00Z"
}
```

---

## 🔄 플로우 다이어그램

```
학기 종료
    ↓
교수: 성적 입력
    ↓
학생: 성적 조회
    ↓
학생: 강의평가 제출 (종강 후 1주일)
    ↓
관리자: 평가 결과 분석
    ↓
관리자: 학기 종합 보고서 생성
```

## 📝 구현 가이드

### 프론트엔드 구현 포인트
1. **평가 기간 관리**: 종강 후 1주일 제한
2. **실시간 통계**: 평가 제출 즉시 결과 업데이트
3. **익명성 보장**: 평가 결과에 학생 정보 노출 금지
4. **다중 평가 지원**: 중간/기말 평가 분리

### 강의평가 UI/UX
```javascript
// 평가 항목 타입별 렌더링
const renderEvaluationItem = (item) => {
  switch(item.type) {
    case 'RATING':
      return <StarRating min={item.minRating} max={item.maxRating} />;
    case 'TEXT':
      return <TextArea maxLength={item.maxLength} />;
    case 'MULTIPLE_CHOICE':
      return <RadioGroup options={item.options} />;
  }
};
```

### 성적 관리 고려사항
- **학점 계산**: 중간(40%) + 기말(60%) 자동 계산
- **GPA 변환**: 학점 → GPA 포인트 변환
- **재시험 지원**: 추가 평가 입력 기능
- **등급 분포**: 학급 전체 성적 분포 표시

### 보고서 생성
- **PDF 내보내기**: 종합 보고서 다운로드
- **차트 시각화**: 평가 결과, 성적 분포 그래프
- **필터링**: 학과별, 교수별 세부 분석
- **비교 분석**: 전 학기 대비 성과 비교

### 데이터 검증
- **마감일 체크**: 평가 제출 기한 검증
- **중복 제출 방지**: 학생별 1회 제출 제한
- **필수 항목 검증**: required 필드 체크
- **점수 범위 검증**: 0-100점, 유효한 학점

---

**문서 종료**: 모든 단계의 API 가이드를 확인했습니다.

**추가 참고**: [README.md](../README.md)에서 전체 목차를 확인하세요.</content>
<parameter name="filePath">F:\main_project\works\blue-crab-lms\backend\BlueCrab\docs\강의관련기능\Phase4_Evaluation_Closure.md