# 백엔드 성적 관리 수정 내역

## 🐛 문제점

### 중복 저장 문제
출석 만점 점수가 두 군데에 중복 저장되어 동기화 문제 발생:

```json
{
  "gradeConfig": {
    "attendanceMaxScore": 37,           // ❌ 교수가 설정한 값
    "latePenaltyPerSession": 0.7
  },
  "grade": {
    "attendance": {
      "maxScore": 20.0,                 // ❌ 하드코딩된 값 (중복!)
      "currentScore": 0.0,
      "percentage": 0.0
    }
  }
}
```

### 발생 원인
1. **AttendanceService** (383번 줄): 하드코딩된 `maxScore = 20.0`
2. **GradeManagementService** (109번 줄): `grade.attendance.maxScore`에 값 중복 저장
3. **2단계 설정 프로세스**: 로컬 config → 서버 저장 과정에서 동기화 실패

---

## ✅ 해결 방법

### 1. AttendanceService.java 수정

**파일**: `BlueCrab/src/main/java/BlueCrab/com/example/service/Lecture/AttendanceService.java`

#### 1-1. Import 추가 (5번 줄)
```java
import com.fasterxml.jackson.databind.JsonNode;  // ✅ 추가
```

#### 1-2. 점수 계산 로직 수정 (383~392번 줄)

**변경 전**:
```java
// 최대 점수: 80회차 = 20점 만점
double maxScore = 20.0;

// 출석율 기반 점수 계산 (80회 기준)
// 예: 77회 출석(출석+지각) / 80 * 20 = 19.25점
double currentScore = ((double) attendanceCount / 80.0) * maxScore;
```

**변경 후**:
```java
// ✅ gradeConfig에서 attendanceMaxScore 읽기 (중복 제거)
double maxScore = 20.0;  // 기본값
if (enrollmentData.has("gradeConfig")) {
    JsonNode gradeConfig = enrollmentData.get("gradeConfig");
    if (gradeConfig.has("attendanceMaxScore")) {
        maxScore = gradeConfig.get("attendanceMaxScore").asDouble();
    }
}

// 출석율 기반 점수 계산 (80회 기준)
// 예: 77회 출석(출석+지각) / 80 * attendanceMaxScore = 점수
double currentScore = ((double) attendanceCount / 80.0) * maxScore;
```

**효과**:
- 교수가 설정한 `gradeConfig.attendanceMaxScore` 값을 동적으로 읽어옴
- 하드코딩 제거 → 설정 변경 시 자동 반영

---

### 2. GradeManagementService.java 수정

**파일**: `BlueCrab/src/main/java/BlueCrab/com/example/service/Lecture/GradeManagementService.java`

#### 2-1. Grade 초기화 로직 수정 (102~120번 줄)

**변경 전**:
```java
// ✅ grade 객체 초기화 (gradeConfig 존재 시에만)
if (!currentData.containsKey("grade")) {
    Map<String, Object> gradeData = new HashMap<>();
    gradeData.put("attendance", Map.of(
        "maxScore", gradeConfig.get("attendanceMaxScore"),  // ❌ 중복!
        "currentScore", 0.0,
        "percentage", 0.0
    ));
    gradeData.put("assignments", new java.util.ArrayList<>());
    gradeData.put("total", Map.of(
        "maxScore", gradeConfig.get("attendanceMaxScore"),
        "score", 0.0,
        "percentage", 0.0
    ));
    currentData.put("grade", gradeData);
}
```

**변경 후**:
```java
// ✅ grade 객체 초기화 (중복 제거: maxScore는 gradeConfig에만 존재)
if (!currentData.containsKey("grade")) {
    Map<String, Object> gradeData = new HashMap<>();
    gradeData.put("attendance", Map.of(
        "currentScore", 0.0,  // ✅ maxScore 제거!
        "percentage", 0.0
    ));
    gradeData.put("assignments", new java.util.ArrayList<>());
    gradeData.put("total", Map.of(
        "maxScore", gradeConfig.get("attendanceMaxScore"),
        "score", 0.0,
        "percentage", 0.0
    ));
    currentData.put("grade", gradeData);
}
```

**효과**:
- `grade.attendance`에서 `maxScore` 필드 제거
- 단일 출처 원칙: `gradeConfig.attendanceMaxScore`만 사용

---

## 📊 수정 후 데이터 구조

### ✅ 올바른 JSON 구조

```json
{
  "gradeConfig": {
    "attendanceMaxScore": 37,           // ✅ 단일 출처!
    "assignmentTotalScore": 50,
    "examTotalScore": 30,
    "latePenaltyPerSession": 0.7,
    "gradeDistribution": {
      "A": 30,
      "B": 40,
      "C": 20,
      "D": 10
    },
    "configuredAt": "2025-10-24 15:30:00",
    "totalMaxScore": 117
  },
  "grade": {
    "attendance": {
      // ✅ maxScore 제거 (gradeConfig 참조)
      "currentScore": 0.0,
      "percentage": 0.0,
      "presentCount": 0,
      "lateCount": 0,
      "absentCount": 0,
      "attendanceRate": 0
    },
    "assignments": [],
    "total": {
      "maxScore": 37,    // gradeConfig.attendanceMaxScore 참조
      "score": 0.0,
      "percentage": 0.0
    }
  }
}
```

---

## 🎯 이점

### 1. 단일 출처 원칙 (Single Source of Truth)
- `attendanceMaxScore`는 `gradeConfig`에만 존재
- 중복 제거로 동기화 문제 해결

### 2. 동적 설정 반영
- 교수가 출석 만점을 변경하면 즉시 모든 학생에게 적용
- 재계산 불필요

### 3. 데이터 일관성
- 설정 값과 실제 계산 값이 항상 동일
- 37점 설정 → 실제 계산도 37점

### 4. 유지보수 용이
- 하드코딩 제거로 코드 유연성 증가
- 향후 점수 체계 변경 시 `gradeConfig`만 수정

---

## 🧪 테스트 권장 사항

### 1. 설정 변경 테스트
```javascript
// 1. 출석 만점 37점으로 설정
gradePhase1.quickAttendanceConfig(37, 0.7);
await gradePhase1.config();

// 2. 학생 성적 조회 → attendance.maxScore 필드 없음 확인
await gradePhase1.studentInfo();

// 3. gradeConfig.attendanceMaxScore = 37 확인
```

### 2. 점수 계산 테스트
```javascript
// 1. 출석 기록 (예: 77/80 출석)
// 2. 성적 조회 시 점수 = (77/80) * 37 = 35.6점 확인
```

### 3. 설정 재변경 테스트
```javascript
// 1. 출석 만점 50점으로 변경
gradePhase1.quickAttendanceConfig(50, 0.5);
await gradePhase1.config();

// 2. 성적 재조회 → 점수 = (77/80) * 50 = 48.1점 확인
```

---

## 📝 변경된 파일 목록

### 백엔드
1. ✅ `AttendanceService.java`
   - Import 추가: `JsonNode`
   - `calculateAttendanceScoreForGrade()` 메서드 수정

2. ✅ `GradeManagementService.java`
   - `configureGrade()` 메서드 수정
   - Grade 초기화 로직 변경

### 프론트엔드
- ℹ️ 변경 불필요 (기존 코드 그대로 사용 가능)
- `01-grade-phase1-tests.js`는 수정된 백엔드와 호환됨

---

## 🔍 추가 확인 필요 사항

### 1. 기존 데이터 마이그레이션
기존에 저장된 `grade.attendance.maxScore` 필드 처리:

**옵션 A**: 데이터 정리 스크립트
```java
// 모든 ENROLLMENT_DATA에서 grade.attendance.maxScore 제거
// gradeConfig.attendanceMaxScore만 참조하도록 정리
```

**옵션 B**: 하위 호환성 유지
```java
// attendanceMaxScore를 읽을 때 fallback 로직 추가
double maxScore = gradeConfig.get("attendanceMaxScore") != null 
    ? gradeConfig.get("attendanceMaxScore") 
    : attendance.get("maxScore");  // 기존 데이터 대응
```

### 2. 과제 점수 구조 확인
`assignments` 배열에도 유사한 중복이 있는지 확인 필요.

---

## 작성 정보
- **작성일**: 2025-10-24
- **작성자**: AI Assistant (GitHub Copilot)
- **관련 이슈**: Grade configuration duplication (attendanceMaxScore)
- **테스트 상태**: ⏳ 로컬 테스트 대기 중
