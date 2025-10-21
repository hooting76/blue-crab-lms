# 🔧 미구현 기능 POST 방식 통일 가이드

> **작성일**: 2025-10-18  
> **업데이트**: 2025-10-21 (성적 관리 구현 완료 반영)  
> **목적**: 기존 시스템과의 일관성 확보를 위한 POST 방식 API 재설계  
> **대상**: 백엔드 개발자, 프론트엔드 개발자

---

## 📋 개요

Blue Crab LMS의 강의 관리 시스템은 복합 쿼리 지원을 위해 **POST 방식을 채택**하고 있습니다.

### ✅ 구현 완료 (93%)
**성적 관리 시스템** - Phase 1~3 완료
- 7개 API 구현 완료
- 이벤트 기반 자동 재계산
- 등급 배정 알고리즘 (하위 침범 방식)
- 📁 **상세 문서**: `성적관리/` 디렉토리

### ⏳ 미구현 (7%)
Phase4의 일부 기능들도 이 패턴에 따라 모두 POST 방식으로 통일하여 구현하도록 재설계되었습니다.

### 🎯 POST 방식 채택 이유
1. **복합 쿼리 지원**: Request Body에서 다양한 필터링 조건 지원
2. **일관된 패턴**: 기존 구현된 API들과 동일한 구조
3. **확장성**: 향후 기능 추가 시 유연한 대응
4. **보안**: GET 방식 대비 민감한 정보 보호

---

## 🚫 기존 문제점

### 1. Phase4 문서의 REST 방식 혼재
```markdown
❌ 잘못된 설계 (REST 방식 혼재)
- PUT /api/grades
- GET /api/grades/my-grades?year=2025
- GET /api/evaluations/items/{lecSerial}
- POST /api/evaluations
- GET /api/evaluations/results/{lecSerial}
- GET /api/reports/semester-summary?year=2025&semester=1
```

### 2. 테스트 코드의 엔드포인트 불일치
```javascript
❌ 존재하지 않는 엔드포인트 호출
fetch(`${API_BASE_URL}/api/assignments/grade`, { method: 'POST' })
fetch(`${API_BASE_URL}/lectures/${lectureIdx}/students/${studentIdx}/grades`)
```

---

## ✅ POST 방식 통일 설계

### 1. 성적 관리 API

#### 성적 입력 (교수)
```javascript
POST /api/grades/input
{
  "lecSerial": "CS101",
  "studentIdx": 100,
  "gradeType": "FINAL", // MID/MIDTERM, FINAL/FINAL_EXAM
  "score": 95,
  "grade": "A+",
  "notes": "우수한 성적",
  "action": "input"
}
```

#### 성적 조회 (학생)
```javascript
POST /api/grades/my-grades
{
  "studentIdx": 100,    // JWT에서 추출 가능
  "year": 2025,         // 선택적 필터
  "semester": 1,        // 선택적 필터
  "action": "list"
}
```

#### 성적 조회 (교수용 - 학생별)
```javascript
POST /api/grades/my-grades
{
  "studentIdx": 100,
  "lecSerial": "CS101",
  "professorIdx": 22,   // JWT에서 추출 가능
  "action": "professor-view"
}
```

### 2. 강의평가 API

#### 평가 항목 조회
```javascript
POST /api/evaluations/items
{
  "lecSerial": "CS101",
  "studentIdx": 100,    // JWT에서 추출 가능
  "action": "get-items"
}
```

#### 평가 제출
```javascript
POST /api/evaluations/submit
{
  "lecSerial": "CS101",
  "studentIdx": 100,
  "action": "submit",
  "responses": [
    {
      "itemIdx": 1,
      "rating": 5,
      "textResponse": "매우 만족합니다"
    }
  ]
}
```

#### 평가 결과 조회
```javascript
POST /api/evaluations/results
{
  "lecSerial": "CS101",
  "professorIdx": 22,   // 교수용
  "adminIdx": 1,        // 관리자용
  "action": "results"
}
```

### 3. 학기 보고서 API

#### 종합 보고서 생성
```javascript
POST /api/reports/semester-summary
{
  "year": 2025,
  "semester": 1,
  "adminIdx": 1,        // JWT에서 추출 가능
  "action": "generate-summary"
}
```

---

## 🔧 구현 가이드

### 백엔드 컨트롤러 패턴

```java
@RestController
@RequestMapping("/api/grades")
public class GradeController {

    @PostMapping("/input")
    public ResponseEntity<?> inputGrade(@RequestBody Map<String, Object> request) {
        String action = (String) request.get("action");
        
        if ("input".equals(action)) {
            // 성적 입력 로직
            return handleGradeInput(request);
        }
        
        return ResponseEntity.badRequest()
            .body(createErrorResponse("지원하지 않는 액션입니다."));
    }

    @PostMapping("/my-grades")
    public ResponseEntity<?> getGrades(@RequestBody Map<String, Object> request) {
        String action = (String) request.get("action");
        
        switch (action) {
            case "list":
                return handleStudentGradeList(request);
            case "professor-view":
                return handleProfessorGradeView(request);
            default:
                return ResponseEntity.badRequest()
                    .body(createErrorResponse("지원하지 않는 액션입니다."));
        }
    }
}
```

### 프론트엔드 호출 패턴

```javascript
// 공통 API 호출 함수
async function callAPI(endpoint, data, token) {
    const response = await fetch(`${API_BASE_URL}${endpoint}`, {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json',
            'Authorization': `Bearer ${token}`
        },
        body: JSON.stringify(data)
    });
    
    return response.json();
}

// 사용 예시
const gradeResult = await callAPI('/grades/input', {
    lecSerial: 'CS101',
    studentIdx: 100,
    gradeType: 'FINAL',
    score: 95,
    grade: 'A+',
    action: 'input'
}, authToken);
```

---

## 📊 개발 우선순위

### ✅ 완료: 성적 관리 시스템 (93%)
- **GradeManagementController, GradeCalculationService 구현 완료**
- `POST /enrollments/grade-config` - 성적 구성 설정
- `POST /enrollments/grade-info` - 성적 조회 (학생/교수)
- `POST /enrollments/grade-list` - 성적 목록 조회
- `POST /enrollments/grade-finalize` - 최종 등급 배정
- `PUT /enrollments/{enrollmentIdx}/attendance` - 출석 업데이트
- `PUT /assignments/{assignmentIdx}/grade` - 과제 채점
- **완료 기간**: 2025-10 (약 3주)
- **📁 상세 문서**: `성적관리/` 디렉토리

### 🟡 2단계: 강의평가 시스템 (Medium Priority)
- **EvaluationController 신규 생성 필요**
- `POST /api/evaluations/items` - 평가 항목 조회
- `POST /api/evaluations/submit` - 평가 제출
- `POST /api/evaluations/results` - 평가 결과 조회
- **예상 개발 기간**: 2-3주

### 🟢 3단계: 보고서 시스템 (Low Priority)
- **ReportController 신규 생성 필요**
- `POST /api/reports/semester-summary` - 종합 보고서
- **예상 개발 기간**: 1주

---

## 🔄 테스트 코드 수정 사항

### 수정 완료 항목
1. **lecture-test-4b-professor-assignment-grade.js**
   ```javascript
   ❌ 기존: POST /api/assignments/grade
   ✅ 수정: PUT /assignments/{assignmentIdx}/grade
   ```

2. **lecture-test-5-professor-students.js**
   ```javascript
   ❌ 기존: GET /lectures/{lectureIdx}/students/{studentIdx}/grades
   ✅ 수정: POST /grades/my-grades (action: "professor-view")
   ```

### 추가 생성 필요 테스트
1. **lecture-test-7-grade-management.js** - 성적 관리 테스트
2. **lecture-test-8-evaluation.js** - 강의평가 테스트
3. **lecture-test-9-reports.js** - 보고서 테스트

---

## 📝 데이터베이스 설계 고려사항

### 신규 테이블 필요
```sql
-- 성적 테이블
CREATE TABLE GRADE_TBL (
    GRADE_IDX INT PRIMARY KEY AUTO_INCREMENT,
    LEC_IDX INT NOT NULL,
    STUDENT_IDX INT NOT NULL,
    GRADE_TYPE VARCHAR(20) NOT NULL, -- MID, FINAL
    SCORE DECIMAL(5,2),
    GRADE VARCHAR(2),                -- A+, A, B+, B, C+, C, D+, D, F
    NOTES TEXT,
    INPUT_BY INT NOT NULL,           -- 교수 ID
    INPUT_DATE DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (LEC_IDX) REFERENCES LEC_TBL(LEC_IDX),
    FOREIGN KEY (STUDENT_IDX) REFERENCES USER_TBL(USER_IDX),
    FOREIGN KEY (INPUT_BY) REFERENCES USER_TBL(USER_IDX)
);

-- 강의평가 테이블
CREATE TABLE EVALUATION_TBL (
    EVAL_IDX INT PRIMARY KEY AUTO_INCREMENT,
    LEC_IDX INT NOT NULL,
    STUDENT_IDX INT NOT NULL,
    RESPONSES JSON NOT NULL,         -- 평가 응답 데이터
    SUBMIT_DATE DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (LEC_IDX) REFERENCES LEC_TBL(LEC_IDX),
    FOREIGN KEY (STUDENT_IDX) REFERENCES USER_TBL(USER_IDX)
);
```

---

## 🎯 결론

✅ **성적 관리 구현 완료** (93%)
- 일관성 확보: POST 방식 통일
- 확장성 증대: 복합 쿼리 및 필터링 지원
- 이벤트 시스템: 자동 재계산 구현
- 등급 배정: 하위 침범 알고리즘 구현

⏳ **다음 단계**
- 2단계: 강의평가 시스템 (2-3주)
- 3단계: 보고서 시스템 (1주)

**📁 참고**: 성적 관리 시스템 상세 구현은 `성적관리/` 디렉토리 참조
