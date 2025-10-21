# 🔧 구현 가이드# 📊 성적 관리 시스템 구현 가이드



API 설계, 데이터 구조, 핵심 알고리즘JSON 기반 성적 관리 시스템 구현



------



## 🗃️ 데이터 구조## 📋 개요



### ENROLLMENT_DATA (JSON 컬럼)Blue Crab LMS의 성적 관리 시스템은 **ENROLLMENT_EXTENDED_TBL의 ENROLLMENT_DATA (JSON 컬럼)**을 활용하여 유연하고 확장 가능한 성적 관리를 제공합니다.



```json### 🎯 설계 원칙

{1. **JSON 기반 저장**: 유연한 데이터 구조로 확장성 확보

  "gradeConfig": {2. **이벤트 기반 자동 계산**: 출석, 과제 점수 변경 시 자동 집계

    "attendanceMaxScore": 80,3. **POST 방식 통일**: 기존 시스템과의 일관성 유지

    "assignmentTotalMaxScore": 100,4. **백분위 등급 배정**: 상대평가 기준 등급 산출

    "latePenaltyPerSession": 0.5,

    "gradeDistribution": {---

      "A+": 10, "A": 15,

      "B+": 20, "B": 25,## 🗃️ 데이터 구조

      "C": 20, "D": 10

    }### ENROLLMENT_DATA JSON 스키마

  },

  "grade": {```json

    "attendance": {{

      "maxScore": 20.0,  "grade": {

      "currentScore": 18.0,    "attendance": {

      "percentage": 90.00,      "maxScore": 20,

      "presentCount": 75,      "currentScore": 18.5,

      "lateCount": 3,      "rate": 92.5

      "absentCount": 2,    },

      "attendanceRate": 78,    "assignments": [

      "latePenalty": 1.5      {

    },        "name": "과제1",

    "assignments": [        "score": 9,

      {        "maxScore": 10

        "name": "과제1",      },

        "score": 9.0,      {

        "maxScore": 10.0,        "name": "중간고사",

        "percentage": 90.00        "score": 85,

      }        "maxScore": 100

    ],      },

    "total": {      {

      "totalScore": 91.0,        "name": "기말고사",

      "maxScore": 100.0,        "score": 92,

      "percentage": 91.00        "maxScore": 100

    },      }

    "finalGrade": "A+"    ],

  }    "total": {

}      "score": 204.5,

```      "maxScore": 230,

      "percentage": 88.9

---    },

    "letterGrade": "A"

## 🔧 API 명세  }

}

### 1. 성적 구성 설정```

**POST** `/enrollments/grade-config`

### 데이터 구성 요소

```javascript

// 요청| 구분 | 설명 | 계산 방식 |

{|------|------|-----------|

  "lecSerial": "ETH201",              // 또는 lecIdx| **출석 점수** | 출석율 기반 자동 계산 | `출석율 × 지정 만점` |

  "attendanceMaxScore": 80,| **과제 점수** | 개별 과제별 점수 관리 | `ASSIGNMENT_TBL`에서 집계 |

  "assignmentTotalMaxScore": 100,| **중간/기말고사** | 과제의 특수 형태 | 과제명으로 구분 |

  "latePenaltyPerSession": 0.5,       // 지각 1회당 감점| **합계 점수** | 전체 점수 총합 | 이벤트 기반 자동 계산 |

  "gradeDistribution": {| **등급** | 백분위 기준 등급 | A, B, C, D, F |

    "A+": 10, "A": 15, "B+": 20,

    "B": 25, "C": 20, "D": 10---

  }

}## 🔧 API 설계



// 응답### 1. 성적 구성 설정

{

  "success": true,**엔드포인트**: `POST /api/enrollments/grade-config`

  "message": "성적 구성이 설정되었습니다.",

  "data": { ... }**강의 식별**: `lecIdx` (정수) 또는 `lecSerial` (문자열) 중 하나 사용  

}→ lecSerial 사용 시 백엔드가 자동으로 lecIdx로 변환

```

```javascript

---// 요청 예시 1 (lecIdx 사용)

{

### 2. 학생 성적 조회  "lecIdx": 1,

**GET** `/enrollments/grade-info`  "attendanceMaxScore": 20,

  "assignmentTotalScore": 50,

```javascript  "examTotalScore": 30,

// 요청 파라미터  "gradeDistribution": {

?lecSerial=ETH201&studentIdx=6    "A": 30,  // 상위 30%

    "B": 40,  // 30~70%

// 응답    "C": 20,  // 70~90%

{    "D": 10   // 나머지

  "studentInfo": {  },

    "studentIdx": 6,  "action": "set-config"

    "name": "학생이름"}

  },

  "attendance": {// 요청 예시 2 (lecSerial 사용 - 권장)

    "maxScore": 20.0,{

    "currentScore": 18.0,  "lecSerial": "ETH201",  // 강의 코드로 식별

    "percentage": 90.00,  "attendanceMaxScore": 20,

    "presentCount": 75,  "assignmentTotalScore": 50,

    "lateCount": 3,  "examTotalScore": 30,

    "absentCount": 2,  "gradeDistribution": {

    "attendanceRate": 78,    "A": 30,

    "latePenalty": 1.5    "B": 40,

  },    "C": 20,

  "assignments": [    "D": 10

    {  },

      "name": "과제1",  "action": "set-config"

      "score": 9.0,}

      "maxScore": 10.0,

      "percentage": 90.00// 응답 예시

    }{

  ],  "success": true,

  "total": {  "message": "성적 구성이 설정되었습니다.",

    "totalScore": 91.0,  "data": {

    "maxScore": 100.0,    "lecIdx": 1,

    "percentage": 91.00    "gradeConfig": { ... }

  }  }

}}

``````



---### 2. 개별 성적 조회



### 3. 교수용 성적 조회**엔드포인트**: `POST /api/enrollments/grade-info`

**GET** `/enrollments/grade-info` (교수 권한)

**강의 식별**: `lecIdx` (정수) 또는 `lecSerial` (문자열) 중 하나 사용

```javascript

// 요청 파라미터```javascript

?lecSerial=ETH201&studentIdx=6&professorIdx=22// 요청 예시 (학생 본인 조회 - lecSerial 사용)

{

// 응답 (학생 성적 + 통계)  "lecSerial": "ETH201",

{  "studentIdx": 100,

  "studentInfo": { ... },  "action": "get-grade"

  "attendance": { ... },}

  "assignments": [ ... ],

  "total": { ... },// 요청 예시 (교수용 조회 - lecIdx 사용)

  "statistics": {{

    "rank": 3,  "lecIdx": 1,

    "totalStudents": 45,  "studentIdx": 100,

    "classAverage": 85.5,  "professorIdx": 50,

    "highestScore": 95.0  "action": "professor-view"

  }}

}

```// 응답 예시

{

---  "success": true,

  "data": {

### 4. 성적 목록 조회    "studentInfo": {

**GET** `/enrollments/grade-list`      "studentIdx": 100,

      "studentName": "홍길동",

```javascript      "studentId": "STU001"

// 요청 파라미터    },

?lecSerial=ETH201&page=0&size=20&sortBy=percentage&sortOrder=desc    "grade": {

      "attendance": {

// 응답        "maxScore": 20,

{        "currentScore": 18.5,

  "content": [        "rate": 92.5

    {      },

      "studentIdx": 6,      "assignments": [ ... ],

      "name": "학생1",      "total": {

      "studentId": "2021001",        "score": 204.5,

      "totalScore": 91.0,        "maxScore": 230,

      "percentage": 91.00,        "percentage": 88.9

      "rank": 1      },

    },      "letterGrade": "A",

    ...      "rank": 3,

  ],      "totalStudents": 45

  "totalElements": 45,    }

  "totalPages": 3,  }

  "currentPage": 0}

}```

```

### 3. 전체 수강생 성적 목록

---

**엔드포인트**: `POST /api/enrollments/grade-list`

### 5. 최종 등급 배정

**POST** `/enrollments/grade-finalize````javascript

// 요청 예시

```javascript{

// 요청  "lecIdx": 1,

{  "page": 0,

  "lecSerial": "ETH201"  "size": 20,

}  "sortBy": "percentage",  // percentage, name, studentId

  "sortOrder": "desc",     // asc, desc

// 응답  "action": "list-all"

{}

  "success": true,

  "message": "최종 등급이 배정되었습니다.",// 응답 예시

  "data": {{

    "totalStudents": 45,  "success": true,

    "gradeDistribution": {  "data": {

      "A+": 4, "A": 7, "B+": 9,    "content": [

      "B": 11, "C": 9, "D": 3, "F": 2      {

    },        "studentIdx": 100,

    "statistics": {        "studentName": "홍길동",

      "averageScore": 85.5,        "studentId": "STU001",

      "highestScore": 95.0,        "attendance": { ... },

      "lowestScore": 45.0        "assignments": [ ... ],

    }        "total": { ... },

  }        "letterGrade": "A",

}        "rank": 1

```      }

    ],

---    "totalElements": 45,

    "totalPages": 3,

### 6. 출석 업데이트 (이벤트)    "currentPage": 0

**PUT** `/enrollments/{enrollmentIdx}/attendance`  }

}

```javascript```

// 요청

{### 4. 최종 등급 배정

  "attendanceStatus": "출",  // "출", "지", "결"

  "sessionNumber": 1**엔드포인트**: `POST /api/enrollments/grade-finalize`

}

```javascript

// 응답// 요청 예시

{{

  "success": true,  "lecIdx": 1,

  "message": "출석이 기록되었습니다.",  "passingThreshold": 60,  // 합격 기준 (60%)

  "gradeUpdateEvent": "TRIGGERED"  // 성적 재계산 이벤트 발행  "gradeDistribution": {

}    "A": 30,  // 합격자 중 상위 30%

```    "B": 40,  // 합격자 중 30~70%

    "C": 20,  // 합격자 중 70~90%

---    "D": 10   // 합격자 중 나머지 10%

  },

### 7. 과제 채점 (이벤트)  "action": "finalize"

**PUT** `/assignments/{assignmentIdx}/grade`}



```javascript// 응답 예시

// 요청{

{  "success": true,

  "studentIdx": 6,  "message": "최종 등급이 배정되었습니다.",

  "score": 9.0  "data": {

}    "gradeStats": {

      "A": 2,   // 2명 (합격자 중 상위)

// 응답      "B": 0,   // 0명

{      "C": 0,   // 0명  

  "success": true,      "D": 0,   // 0명

  "message": "과제가 채점되었습니다.",      "F": 18   // 18명 (60% 미만 + 중도포기)

  "gradeUpdateEvent": "TRIGGERED"  // 성적 재계산 이벤트 발행    },

}    "totalStudents": 20,

```    "passingStudents": 2,    // 합격자 수

    "failingStudents": 18,   // 낙제자 수

---    "averageScore": 45.2     // 전체 평균

  }

## 💡 핵심 알고리즘}

```

### 1. 출석 점수 계산

---

```java

// 1단계: 출석율 계산## 🏗️ 백엔드 구현

출석율 = (출석 수 + 지각 수) / 총 회차 × 100

### EnrollmentController 확장

// 2단계: 출석율 기반 점수

출석율_기반_점수 = 출석율 / 100 × 만점```java

@RestController

// 3단계: 지각 감점 적용@RequestMapping("/api/enrollments")

지각_감점 = 지각 횟수 × latePenaltyPerSessionpublic class EnrollmentController {

최종_출석_점수 = MAX(0, 출석율_기반_점수 - 지각_감점)

```    @Autowired

    private EnrollmentService enrollmentService;

**예시**:

- 출석 75회, 지각 3회, 결석 2회 (총 80회)    @PostMapping("/grade-config")

- 만점 20점, 지각 페널티 0.5점    public ResponseEntity<?> setGradeConfig(@RequestBody Map<String, Object> request) {

```        try {

출석율 = (75 + 3) / 80 × 100 = 97.5%            String action = (String) request.get("action");

출석율 기반 점수 = 0.975 × 20 = 19.5점            

지각 감점 = 3 × 0.5 = 1.5점            if ("set-config".equals(action)) {

최종 출석 점수 = 19.5 - 1.5 = 18.0점                return handleGradeConfig(request);

```            }

            

---            return ResponseEntity.badRequest()

                .body(createErrorResponse("지원하지 않는 액션입니다."));

### 2. 등급 배정 (하위 침범 방식)                

        } catch (Exception e) {

```java            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)

// 1단계: 합격/불합격 분류                .body(createErrorResponse("성적 구성 설정 중 오류가 발생했습니다."));

if (percentage < 60) {        }

    grade = "F";  // 불합격    }

} else {

    // 상대평가 대상    @PostMapping("/grade-info")

}    public ResponseEntity<?> getGradeInfo(@RequestBody Map<String, Object> request) {

        try {

// 2단계: 기본 비율 계산            String action = (String) request.get("action");

전체_학생_수 = 45명            

각_등급_인원 = 전체_학생_수 × 등급_비율 / 100            switch (action) {

                case "get-grade":

// 3단계: 하위 침범 방식 적용                    return handleStudentGradeInfo(request);

F등급_인원 = 2명  // 60% 미만                case "professor-view":

D등급_자리 = 5명  // 10%                    return handleProfessorGradeView(request);

                default:

if (F등급_인원 <= D등급_자리) {                    return ResponseEntity.badRequest()

    // D등급에 여유 있음 → 정상 배정                        .body(createErrorResponse("지원하지 않는 액션입니다."));

} else {            }

    // D등급을 F가 침범 → C등급부터 재배정            

}        } catch (Exception e) {

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)

// 4단계: 동점자 처리                .body(createErrorResponse("성적 조회 중 오류가 발생했습니다."));

if (같은_점수_그룹) {        }

    // 모두 상위 등급으로 배정    }

}

```    @PostMapping("/grade-list")

    public ResponseEntity<?> getGradeList(@RequestBody Map<String, Object> request) {

---        try {

            String action = (String) request.get("action");

## 🔄 이벤트 시스템            

            if ("list-all".equals(action)) {

### GradeUpdateEvent 발행                return handleGradeList(request);

```java            }

@Autowired            

private ApplicationEventPublisher eventPublisher;            return ResponseEntity.badRequest()

                .body(createErrorResponse("지원하지 않는 액션입니다."));

// 출석/과제 변경 시                

eventPublisher.publishEvent(        } catch (Exception e) {

    new GradeUpdateEvent(this, lecIdx, studentIdx, "ATTENDANCE")            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)

);                .body(createErrorResponse("성적 목록 조회 중 오류가 발생했습니다."));

```        }

    }

### EventListener 처리

```java    @PostMapping("/grade-finalize")

@EventListener    public ResponseEntity<?> finalizeGrades(@RequestBody Map<String, Object> request) {

@Async        try {

public void handleGradeUpdateEvent(GradeUpdateEvent event) {            String action = (String) request.get("action");

    // 1. enrollmentIdx 조회            

    Integer enrollmentIdx = getEnrollmentIdx(            if ("finalize".equals(action)) {

        event.getLecIdx(),                 return handleGradeFinalize(request);

        event.getStudentIdx()            }

    );            

                return ResponseEntity.badRequest()

    // 2. 성적 재계산                .body(createErrorResponse("지원하지 않는 액션입니다."));

    gradeCalculationService.calculateStudentGrade(                

        event.getLecIdx(),         } catch (Exception e) {

        event.getStudentIdx()            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)

    );                .body(createErrorResponse("등급 배정 중 오류가 발생했습니다."));

            }

    // 3. ENROLLMENT_DATA 업데이트    }

}}

``````



---### EnrollmentService 구현



## 🔐 강의 식별 방식```java

@Service

### lecSerial 자동 변환@Transactional

```javapublic class EnrollmentService {

// Controller에서 자동 처리

Integer lecIdx = request.get("lecIdx");    @Autowired

String lecSerial = request.get("lecSerial");    private EnrollmentExtendedRepository enrollmentExtendedRepository;

    

if (lecIdx == null && lecSerial != null) {    @Autowired

    // LEC_TBL 조회하여 lecIdx 획득    private AttendanceRepository attendanceRepository;

    lecIdx = enrollmentService.getLectureIdxBySerial(lecSerial);    

}    @Autowired

```    private AssignmentRepository assignmentRepository;



### 권장 사용 방식    /**

- ✅ **lecSerial 사용**: 사용자 친화적, 보안 강화     * 성적 구성 설정

- ⚠️ **lecIdx 사용**: 하위 호환성 (기존 코드)     */

    public ResponseEntity<?> setGradeConfig(Map<String, Object> request) {

---        Integer lecIdx = (Integer) request.get("lecIdx");

        

## 📊 데이터 흐름        // 강의별 성적 구성 설정 로직

        // ENROLLMENT_DATA JSON 업데이트

```        

사용자 액션        return ResponseEntity.ok(createSuccessResponse("성적 구성이 설정되었습니다."));

    ↓    }

Controller (API 호출)

    ↓    /**

Service (비즈니스 로직)     * 개별 학생 성적 계산 및 조회

    ↓     */

Repository (DB 조회)    public ResponseEntity<?> getStudentGradeInfo(Map<String, Object> request) {

    ↓        Integer lecIdx = (Integer) request.get("lecIdx");

ENROLLMENT_DATA JSON 파싱/수정        Integer studentIdx = (Integer) request.get("studentIdx");

    ↓        

JSON 데이터 저장        // 1. 출석 점수 계산

    ↓        Double attendanceScore = calculateAttendanceScore(lecIdx, studentIdx);

(출석/과제 변경 시) 이벤트 발행        

    ↓        // 2. 과제 점수 집계

EventListener (비동기)        List<AssignmentScore> assignmentScores = calculateAssignmentScores(lecIdx, studentIdx);

    ↓        

GradeCalculationService        // 3. 총점 계산

    ↓        GradeInfo gradeInfo = calculateTotalScore(attendanceScore, assignmentScores);

성적 재계산 및 업데이트        

```        // 4. ENROLLMENT_DATA 업데이트

        updateEnrollmentGradeData(lecIdx, studentIdx, gradeInfo);

---        

        return ResponseEntity.ok(createSuccessResponse(gradeInfo));

## 🛠️ 핵심 Service 메서드    }



### EnrollmentService    /**

```java     * 출석 점수 계산

// 1. 성적 구성 설정     */

configureGrade(Map<String, Object> request)    private Double calculateAttendanceScore(Integer lecIdx, Integer studentIdx) {

        // ATTENDANCE_TBL에서 출석율 계산

// 2. 학생 성적 조회        // 출석율 × 지정 만점 = 출석 점수

studentGradeInfo(Integer lecIdx, Integer studentIdx)        return attendanceRepository.calculateAttendanceScore(lecIdx, studentIdx);

    }

// 3. 교수용 성적 조회

professorGradeView(Integer lecIdx, Integer studentIdx, Integer professorIdx)    /**

     * 과제 점수 집계

// 4. 성적 목록 조회     */

gradeList(Integer lecIdx, Pageable pageable, String sortBy, String sortOrder)    private List<AssignmentScore> calculateAssignmentScores(Integer lecIdx, Integer studentIdx) {

        // ASSIGNMENT_TBL에서 해당 강의의 모든 과제 점수 조회

// 5. 최종 등급 배정        return assignmentRepository.getStudentAssignmentScores(lecIdx, studentIdx);

finalizeGrades(Integer lecIdx)    }



// 6. lecSerial → lecIdx 변환    /**

getLectureIdxBySerial(String lecSerial)     * 최종 등급 배정 (60% 기준 + 상대평가)

```     */

    public ResponseEntity<?> finalizeGrades(Map<String, Object> request) {

### GradeCalculationService        Integer lecIdx = (Integer) request.get("lecIdx");

```java        Double passingThreshold = (Double) request.get("passingThreshold"); // 기본값: 60.0

// 학생 성적 계산 (이벤트 콜백)        Map<String, Integer> gradeDistribution = (Map<String, Integer>) request.get("gradeDistribution");

calculateStudentGrade(Integer lecIdx, Integer studentIdx)        

        // 1. 전체 수강생 성적 조회

// 출석 점수 계산        List<StudentGrade> allGrades = getAllStudentGrades(lecIdx);

calculateAttendanceScore(Integer lecIdx, Integer studentIdx)        

        // 2. 합격/불합격 분류 (60% 기준)

// 과제 점수 계산        List<StudentGrade> passingStudents = new ArrayList<>();

calculateAssignmentScores(Integer lecIdx, Integer studentIdx)        List<StudentGrade> failingStudents = new ArrayList<>();

```        

        for (StudentGrade grade : allGrades) {

### AttendanceService            if (grade.getPercentage() >= passingThreshold) {

```java                passingStudents.add(grade);

// 출석 점수 계산 (성적용)            } else {

calculateAttendanceScoreForGrade(Integer lecIdx, Integer studentIdx)                failingStudents.add(grade);

```                grade.setLetterGrade("F"); // 60% 미만은 무조건 F

            }

### AssignmentService        }

```java        

// 과제 점수 조회 (성적용)        // 3. 합격자들만 성적순 정렬 후 상대평가

getStudentAssignmentScoresForGrade(Integer lecIdx, Integer studentIdx)        if (!passingStudents.isEmpty()) {

```            passingStudents.sort((a, b) -> Double.compare(b.getPercentage(), a.getPercentage()));

            assignRelativeGrades(passingStudents, gradeDistribution);

---        }

        

## 💡 특수 케이스 처리        // 4. ENROLLMENT_DATA 업데이트

        updateFinalGrades(allGrades);

### 1. 지각 처리        

- **출석율 계산**: 출석으로 인정        // 5. 통계 생성

- **성적 계산**: 교수 설정에 따라 감점        GradeStatistics stats = generateGradeStatistics(allGrades);

- **기본값**: latePenaltyPerSession = 0.0 (감점 없음)        

        return ResponseEntity.ok(createSuccessResponse("등급 배정이 완료되었습니다.", stats));

### 2. 동점자 처리    }

- 같은 점수는 모두 동일 순위

- 등급 배정 시 상위 등급으로 배정    /**

- 예: A등급 자리 5명, 동점자 7명 → 7명 모두 A등급     * 60점 미만자가 하위 등급을 차지하는 방식으로 등급 배정

     */

### 3. 하위 침범    private void assignRelativeGrades(List<StudentGrade> allGrades, Map<String, Integer> distribution, 

- 60% 미만 학생이 하위 등급 자리 차지                                     List<StudentGrade> passingStudents, int totalStudents) {

- 합격자는 남은 상위 등급에 자연스럽게 배정        

- 예: 100명 중 75명 낙제 → 합격자 25명 모두 A등급        int passingCount = passingStudents.size();

        int failingCount = totalStudents - passingCount;

### 4. enrollmentIdx 자동 조회        

- 사용자는 lecSerial + studentIdx만 제공        // 기본 비율 계산 (전체 학생 수 기준)

- 백엔드가 ENROLLMENT_EXTENDED_TBL 조회        int originalA = (int) Math.ceil(totalStudents * distribution.get("A") / 100.0);

- LEC_IDX + STUDENT_IDX로 ENROLLMENT_IDX 획득        int originalB = (int) Math.ceil(totalStudents * distribution.get("B") / 100.0);

        int originalC = (int) Math.ceil(totalStudents * distribution.get("C") / 100.0);

---        int originalD = (int) Math.ceil(totalStudents * distribution.get("D") / 100.0);

        

## 📚 참고 문서        // 60점 미만자가 하위 등급부터 차지

        int actualA = originalA;

- **테스트 가이드**: [01-QUICK-START.md](./01-QUICK-START.md)        int actualB = originalB;

- **작업 진행**: [03-WORK-PROGRESS.md](./03-WORK-PROGRESS.md)        int actualC = originalC;

- **브라우저 콘솔**: [../브라우저콘솔테스트/04-grade/](../브라우저콘솔테스트/04-grade/)        int actualD = originalD;

        

---        // F등급자가 D, C, B 순서로 자리 차지

        if (failingCount > 0) {

> **구현 완료**: Phase 1~3 (100%), Phase 4 테스트 대기 중            if (failingCount >= originalD) {

                actualD = 0;
                failingCount -= originalD;
                
                if (failingCount >= originalC) {
                    actualC = 0;
                    failingCount -= originalC;
                    
                    if (failingCount >= originalB) {
                        actualB = 0;
                        failingCount -= originalB;
                        
                        // A등급도 침범하는 경우
                        if (failingCount > 0) {
                            actualA = Math.max(0, originalA - failingCount);
                        }
                    } else {
                        actualB = originalB - failingCount;
                    }
                } else {
                    actualC = originalC - failingCount;
                }
            } else {
                actualD = originalD - failingCount;
            }
        }
        
        // 합격자들을 성적순으로 정렬하여 등급 배정
        passingStudents.sort((a, b) -> Double.compare(b.getPercentage(), a.getPercentage()));
        
        // 동점자 처리를 위한 등급 배정 로직
        int currentRank = 1;
        int assignedCount = 0;
        
        for (int i = 0; i < passingCount; i++) {
            StudentGrade currentStudent = passingStudents.get(i);
            
            // 동점자가 아닌 경우 순위 업데이트
            if (i > 0 && !currentStudent.getPercentage().equals(passingStudents.get(i-1).getPercentage())) {
                currentRank = i + 1;
            }
            
            // 현재 순위에 따른 등급 배정 (동점자는 모두 상위 등급)
            String grade = determineGradeByRank(currentRank, actualA, actualB, actualC, actualD);
            currentStudent.setLetterGrade(grade);
            
            if (!"".equals(grade)) {
                assignedCount++;
            }
        }
    }
    
    /**
     * 순위에 따른 등급 결정 (동점자 상위 등급 배정)
     */
    private String determineGradeByRank(int rank, int aCount, int bCount, int cCount, int dCount) {
        if (rank <= aCount) {
            return "A";
        } else if (rank <= aCount + bCount) {
            return "B";
        } else if (rank <= aCount + bCount + cCount) {
            return "C";
        } else if (rank <= aCount + bCount + cCount + dCount) {
            return "D";
        }
        return "A"; // 남은 학생들은 A등급으로 배정
        
        // 낙제자가 하위등급을 다 차지한 경우: 남은 합격자들은 자연스럽게 상위등급에 배치
        while (currentIndex < passingCount) {
            passingStudents.get(currentIndex++).setLetterGrade("A");
        }
    }
}
```

---

## 📝 성적 관리 프로세스

### 1️⃣ 성적 구성 설정
- 교수가 강의별 성적 비율 설정
- 출석, 과제, 시험 배점 결정
- 등급 분포 기준 설정

### 2️⃣ 출석/과제 점수 입력
- 출석 점수: ATTENDANCE_TBL에서 자동 계산
- 과제 점수: ASSIGNMENT_TBL에서 자동 집계
- 이벤트 기반 자동 반영 및 업데이트

### 3️⃣ 이벤트 기반 합계 계산
- JSON 데이터 선별적 자동 업데이트
- 백분율 계산
- 순위 산정

### 4️⃣ 최종 등급 배정 (교수 수동 실행)
- **60% 기준 분류**: 점수/만점 비율 60% 미만은 무조건 F
- **상대평가**: 60% 이상 합격자들만 A, B, C, D로 배정
- **하위 침범 방식**: 낙제자가 D→C→B→A 순서로 하위등급 자리 차지
- **자연스러운 결과**: 낙제자가 많으면 합격자들이 상대적으로 상위등급에 배치됨

## 🎯 최종 등급 배정 특별 로직

### 등급 배정 알고리즘

#### 1단계: 합격/불합격 분류
```
60% 이상 → 합격 (상대평가 대상)
60% 미만 → F등급 확정
```

#### 2단계: 등급 배정 알고리즘
```
기본 비율: A(30%), B(40%), C(20%), D(10%)
60점 미만자가 하위 등급부터 대체하는 방식
동점자 처리: 같은 점수는 모두 동일 순위로 상위 등급에 배정
```

#### 3단계: 실제 등급 배정 사례
```java
// 예시 1: 100명 수강, 모두 60점 이상
총 수강생: 100명
- A등급: 30명 (상위 30%)
- B등급: 40명 (30~70%)
- C등급: 20명 (70~90%)
- D등급: 10명 (90~100%)
- F등급: 0명

// 예시 2: 100명 수강, 15명이 60점 미만
총 수강생: 100명
- A등급: 30명 (상위 30%)
- B등급: 40명 (30~70%)
- C등급: 15명 (70~85%) ← D 10명 + C 일부가 C로
- D등급: 0명 ← 60점 미만자가 D 자리 차지
- F등급: 15명 (60점 미만)

// 예시 3: 100명 수강, 75명이 60점 미만
총 수강생: 100명
- A등급: 25명 (상위 25%) ← 합격자 중 상위만
- B등급: 0명 ← 60점 미만자가 B, C, D 자리 모두 차지
- C등급: 0명
- D등급: 0명
- F등급: 75명 (60점 미만)

// 예시 4: 20명 수강, 18명이 60점 미만
총 수강생: 20명
- A등급: 2명 (합격자 전부) ← 낙제자가 하위등급 다 차지해서 자연스럽게 상위등급만 남음
- B등급: 0명
- C등급: 0명  
- D등급: 0명
- F등급: 18명 (60점 미만)

// 예시 5: 10명 수강, 동점자가 있는 경우 (전원 합격)
총 수강생: 10명 (기본 비율: A 3명, B 4명, C 2명, D 1명)
점수 분포: 2명(100%), 2명(99%), 3명(98%), 3명(97%)
- A등급: 4명 (100% 2명 + 99% 2명) ← 동점자는 모두 상위등급
- B등급: 3명 (98% 3명) ← 공동 5위 그룹
- C등급: 3명 (97% 3명) ← 공동 8위 그룹  
- D등급: 0명 ← 배정할 학생 없음
```

### JSON 데이터 구조 (등급 배정 전후)

#### 등급 배정 전
```json
{
  "grade": {
    "total": {
      "score": 204.5,
      "maxScore": 230,
      "percentage": 88.9
    },
    "letterGrade": null,  // ← 공란 상태
    "finalized": false
  }
}
```

#### 등급 배정 후
```json
{
  "grade": {
    "total": {
      "score": 204.5,
      "maxScore": 230,
      "percentage": 88.9
    },
    "letterGrade": "A",   // ← 등급 확정
    "finalized": true,
    "finalizedDate": "2025-10-18T15:30:00Z",
    "rank": 1,
    "totalStudents": 20,
    "passingStudents": 2
  }
}
```

---

## 🎯 이벤트 기반 자동 계산 시스템

### 이벤트 트리거 포인트

#### 1️⃣ 출석 이벤트
```java
// AttendanceController에서 출석 체크 시
@PostMapping("/mark")
public ResponseEntity<?> markAttendance(@RequestBody AttendanceRequest request) {
    // 1. 출석 데이터 저장
    attendanceService.markAttendance(request);
    
    // 2. 🔥 성적 재계산 이벤트 발생
    applicationEventPublisher.publishEvent(
        new GradeUpdateEvent(request.getLecIdx(), request.getStudentIdx(), "ATTENDANCE")
    );
    
    return ResponseEntity.ok("출석 처리 완료");
}
```

#### 2️⃣ 과제 채점 이벤트
```java
// AssignmentController에서 과제 채점 시
@PutMapping("/{assignmentIdx}/grade")
public ResponseEntity<?> gradeAssignment(@RequestBody GradeRequest request) {
    // 1. 과제 점수 저장
    assignmentService.gradeAssignment(request);
    
    // 2. 🔥 성적 재계산 이벤트 발생
    applicationEventPublisher.publishEvent(
        new GradeUpdateEvent(request.getLecIdx(), request.getStudentIdx(), "ASSIGNMENT")
    );
    
    return ResponseEntity.ok("채점 완료");
}
```

### 이벤트 리스너 구현

```java
@Component
public class GradeUpdateEventListener {
    
    @Autowired
    private EnrollmentService enrollmentService;
    
    @EventListener
    @Async
    public void handleGradeUpdate(GradeUpdateEvent event) {
        try {
            // 해당 학생의 성적만 선별적으로 재계산
            enrollmentService.recalculateStudentGrade(
                event.getLecIdx(), 
                event.getStudentIdx(),
                event.getUpdateType()
            );
            
            log.info("성적 업데이트 완료: 강의={}, 학생={}, 타입={}", 
                event.getLecIdx(), event.getStudentIdx(), event.getUpdateType());
                
        } catch (Exception e) {
            log.error("성적 업데이트 실패", e);
        }
    }
}
```

### 선별적 재계산 로직

```java
/**
 * 특정 학생의 성적만 재계산 (전체가 아닌 변경 부분만)
 */
public void recalculateStudentGrade(Integer lecIdx, Integer studentIdx, String updateType) {
    // 1. 기존 JSON 데이터 조회
    EnrollmentExtended enrollment = findEnrollment(lecIdx, studentIdx);
    JSONObject gradeData = getGradeData(enrollment);
    
    // 2. 업데이트 타입에 따른 선별적 계산
    switch (updateType) {
        case "ATTENDANCE":
            updateAttendanceScore(gradeData, lecIdx, studentIdx); // 출석 점수만
            break;
        case "ASSIGNMENT":
            updateAssignmentScores(gradeData, lecIdx, studentIdx); // 과제 점수만
            break;
    }
    
    // 3. 총점 재계산 및 JSON 업데이트
    updateTotalScore(gradeData);
    saveEnrollmentData(enrollment, gradeData);
}
```

### 성능 최적화 특징

- **선별적 업데이트**: 전체 재계산이 아닌 변경된 부분만 처리
- **비동기 처리**: 백그라운드에서 처리하여 사용자 응답 속도에 영향 없음
- **중복 방지**: 같은 학생의 연속 이벤트 시 마지막 것만 처리
- **투명한 처리**: 사용자는 별도 새로고침 액션 불필요

---

## ✨ 주요 특징

### 🔄 이벤트 기반 자동 계산
- **자동 트리거**: 출석 체크, 과제 채점 시 즉시 재계산
- **선별적 업데이트**: 해당 학생의 성적만 업데이트
- **투명한 처리**: 사용자는 별도 액션 불필요
- **성능 최적화**: 변경된 데이터만 처리

### 📊 JSON 기반 유연성
- 확장 가능한 데이터 구조
- 동적 성적 구성 지원
- 복잡한 계산 로직 단순화

### 🎯 백분위 등급 배정
- 상대평가 기준 등급 산출
- 유연한 등급 분포 설정
- 공정한 성적 평가 보장

### 👨‍🏫 교수 전용 관리
- 교수별 권한 관리
- 강의별 독립적 성적 관리
- 직관적인 인터페이스 제공

---

## 🔧 개발 우선순위

### 1단계: 핵심 성적 관리 (2주)
- ✅ EnrollmentController 확장
- ✅ 성적 구성 설정 API
- ✅ 개별 성적 조회 API
- ✅ **이벤트 기반 자동 계산 로직 구현**
- ✅ AttendanceController에 이벤트 연동
- ✅ AssignmentController에 이벤트 연동

### 2단계: 고급 기능 (1주)
- ✅ 전체 성적 목록 API
- ✅ 최종 등급 배정 API
- ✅ 백분위 계산 로직

### 3단계: 최적화 (1주)
- ✅ 성능 최적화
- ✅ 오류 처리 강화
- ✅ 테스트 코드 작성

---

## 📋 체크리스트

### 백엔드 구현
- [x] **EnrollmentController 확장** ✅ 완료 (메서드명 간결화: get 접두사 제거)
- [x] **EnrollmentService.studentGradeInfo() 구현** ✅ 완료 (핵심 로직)
  - ✅ JSON 데이터 파싱 (기존 메서드 재사용)
  - ✅ 출석 점수 계산 메서드 (calculateAttendanceScore)
  - ✅ 과제 점수 집계 메서드 (calculateAssignmentScores)
  - ✅ 총점 자동 계산 메서드 (calculateTotalScore)
  - ✅ ENROLLMENT_DATA JSON 자동 업데이트
  - ⚠️ TODO: 실제 DB 연동 (현재 임시 데이터)
- [ ] **EnrollmentService 나머지 메서드 구현** 🔄 진행 중
  - [ ] configureGrade() - 성적 구성 설정
  - [ ] professorGradeView() - 교수용 성적 조회
  - [ ] gradeList() - 성적 목록 조회
  - [ ] finalizeGrades() - 최종 등급 배정
- [ ] **이벤트 리스너 구현** (GradeUpdateEventListener)
- [ ] **AttendanceController에 이벤트 발생 로직 추가**
- [ ] **AssignmentController에 이벤트 발생 로직 추가**
- [ ] **실제 DB 연동** (AttendanceRepository, AssignmentRepository)
- [ ] **백분위 등급 배정 로직**
- [ ] **오류 처리 및 검증**

### 데이터베이스
- [ ] ENROLLMENT_EXTENDED_TBL 활용
- [ ] JSON 스키마 정의
- [ ] 인덱스 최적화
- [ ] 성능 튜닝

### 테스트
- [ ] 단위 테스트 작성
- [ ] 통합 테스트 작성  
- [ ] 브라우저 콘솔 테스트 스크립트
- [ ] 성능 테스트

---

## 🎯 결론

JSON 기반 성적 관리 시스템을 통해:

✅ **유연성**: 동적 성적 구성 및 확장 가능  
✅ **효율성**: 실시간 자동 계산으로 관리 부담 감소  
✅ **공정성**: 백분위 기준 객관적 등급 배정  
✅ **일관성**: 기존 POST 방식과 통합된 설계  

**다음 단계**: EnrollmentController 확장 개발 시작

---

> **참고 문서**  
> - [성적관리시스템_설계안_수정.drawio](./성적관리시스템_설계안_수정.drawio)  
> - [미구현기능_POST방식_통일가이드.md](../미구현기능_POST방식_통일가이드.md)