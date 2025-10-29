# 06. 최종 등급 배정 API

> 교수가 상대평가로 A/B/C/D/F 등급을 배정하는 API

---

## 📌 기본 정보

- **엔드포인트**: `POST /api/enrollments/grade-finalize`
- **권한**: 교수
- **설명**: 전체 수강생의 점수 분포를 기준으로 등급 배정
- **참고**: 테스트 코드 기준 엔드포인트 확인됨

## 📐 등급 배정 규칙

### 1차 분류: 절대평가 기준 (60%)
- **60% 미만**: F 등급 확정 (상대평가 비율 소진)
- **60% 이상**: A~D 등급 배정 대상 (상대평가)

### 2차 배정: 상대평가 (전체 학생 기준)

**핵심 원칙**: 
- 등급 분포 비율은 **전체 학생 수 기준**으로 계산
- F 등급 학생이 많으면 → A~D 배정 인원이 자동 감소
- 극단적인 경우 A 또는 B만 배정되고 C, D는 0명 가능

**예시 1**: 전체 42명, F 10명 (60% 미만)
- 전체의 30% = 12.6명 → A는 13명 배정 가능
- 하지만 F가 10명이므로 실제 A 대상자는 최대 32명
- A: min(13, 32) = 13명 ✅
- B: min(17, 32-13) = 17명 ✅
- C: min(8, 32-30) = 2명 (8명 배정 불가능)
- D: min(2, 32-32) = 0명 (배정 불가능)

**예시 2**: 전체 42명, F 30명 (60% 미만)
- 전체의 30% = 12.6명 → A는 13명 배정 가능
- F가 30명이므로 합격자는 12명뿐
- A: min(13, 12) = 12명 (전원 A)
- B, C, D: 0명 (배정 불가능)

---

## 📥 Request

```json
{
  "action": "finalize",
  "lecSerial": "ETH201",
  "passingThreshold": 60.0,
  "gradeDistribution": {
    "A": 30,
    "B": 40,
    "C": 20,
    "D": 10
  }
}
```

| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| action | String | ✅ | 반드시 `"finalize"` |
| lecSerial | String | ✅ | 강의 코드 |
| passingThreshold | Double | ❌ | 합격 기준 (기본: 60.0) |
| gradeDistribution | Object | ❌ | 등급 분포 (기본값: 성적 구성 설정에서 가져옴) |

**⚠️ 중요**: 
- `gradeDistribution`을 생략하면 **02. 성적 구성 설정**에서 저장한 값 사용
- `passingThreshold` 미만은 무조건 **F 등급** (절대평가)
- A+B+C+D 합계는 100% (F는 제외)
- **핵심**: 등급 비율은 **전체 학생 수** 기준이므로, F가 많으면 A~D 배정 인원이 자동으로 줄어듭니다

---

## 📤 Response

```json
{
  "success": true,
  "message": "42명의 최종 등급이 배정되었습니다.",
  "gradeResult": {
    "A": 10,
    "B": 13,
    "C": 6,
    "D": 3,
    "F": 10
  },
  "statistics": {
    "totalStudents": 42,
    "passedStudents": 32,
    "failedStudents": 10,
    "passRate": 76.2
  }
}
```

**설명**:
- `passedStudents`: 60% 이상 학생 (A~D 대상)
- `failedStudents`: 60% 미만 학생 (F 확정)
- `passRate`: 합격률 (60% 이상 비율)

---

## 🗄️ DB 변화

**모든 수강생의 ENROLLMENT_DATA 업데이트**:

### 상위 30% (A)

```json
{
  "grade": {
    "total": {
      "currentScore": 245.5,
      "percentage": 88.6
    },
    "letterGrade": "A"
  }
}
```

### 30~70% (B)

```json
{
  "grade": {
    "total": {
      "currentScore": 198.0,
      "percentage": 71.5
    },
    "letterGrade": "B"
  }
}
```

### 70~90% (C)

```json
{
  "grade": {
    "total": {
      "currentScore": 120.5,
      "percentage": 65.2
    },
    "letterGrade": "C"
  }
}
```

### 90~100% (D) - 60% 이상 중 하위

```json
{
  "grade": {
    "total": {
      "currentScore": 110.0,
      "percentage": 61.8
    },
    "letterGrade": "D"
  }
}
```

### 60% 미만 (F) - 절대평가

```json
{
  "grade": {
    "total": {
      "currentScore": 55.0,
      "percentage": 19.9
    },
    "letterGrade": "F"
  }
}
```

**⚠️ 중요**: F 등급은 60% 미만 학생들에게 자동 배정되며, 상대평가 비율 계산에서 제외됩니다.

---

## 🔄 시퀀스 다이어그램

```plaintext
교수 → API: 등급 배정 요청 (A 30%, B 40%, C 20%, D 10%)
API → DB: 해당 강의 전체 수강생 조회
API → 로직: 1단계 - 60% 미만 학생 F 등급 확정
API → 로직: 2단계 - 60% 이상 학생 percentage 기준 내림차순 정렬
API → 로직: 3단계 - 상위 30%에 A, 30~70%에 B, 70~90%에 C, 90~100%에 D
API → DB: 각 수강생의 ENROLLMENT_DATA.letterGrade 업데이트
DB → API: 42건 업데이트 완료
API → 교수: 등급 배정 결과 (A 10명, B 13명, C 6명, D 3명, F 10명)
```

**중요**: F 등급 배정은 상대평가 비율과 무관하게 60% 미만 기준으로 먼저 처리됩니다.

---

## 💡 등급 배정 알고리즘

### 0단계: 절대평가 기준 적용 (F 확정)

```java
final double PASS_THRESHOLD = 60.0; // 합격 기준선
final int TOTAL_STUDENTS = 42;      // 전체 학생 수

List<Enrollment> allStudents = enrollmentRepository.findByLecIdx(lecIdx);
List<Enrollment> passedStudents = new ArrayList<>();
List<Enrollment> failedStudents = new ArrayList<>();

// 60% 기준으로 분리
for (Enrollment student : allStudents) {
    double percentage = student.getEnrollmentData().getGrade().getTotal().getPercentage();
    if (percentage >= PASS_THRESHOLD) {
        passedStudents.add(student);
    } else {
        failedStudents.add(student);
        // 60% 미만은 즉시 F 등급 배정
        student.getEnrollmentData().getGrade().setLetterGrade("F");
    }
}
```

### 1단계: 합격 학생 점수 정렬 (60% 이상만)

```java
// 60% 이상 학생들만 상대평가 대상
passedStudents.sort((a, b) -> {
    double percentageA = a.getEnrollmentData().getGrade().getTotal().getPercentage();
    double percentageB = b.getEnrollmentData().getGrade().getTotal().getPercentage();
    return Double.compare(percentageB, percentageA); // 내림차순
});
```

### 2단계: 비율 계산 (⚠️ 전체 학생 수 기준)

**핵심**: F 학생이 등급 분포 "자리"를 차지함

**예시 1**: 전체 42명, 합격 32명, 불합격(F) 10명

```java
// 등급별 "목표" 인원 (전체 학생 수 기준)
int targetA = (int) Math.round(TOTAL_STUDENTS * 0.30); // 42 × 30% = 13명
int targetB = (int) Math.round(TOTAL_STUDENTS * 0.40); // 42 × 40% = 17명
int targetC = (int) Math.round(TOTAL_STUDENTS * 0.20); // 42 × 20% = 8명
int targetD = (int) Math.round(TOTAL_STUDENTS * 0.10); // 42 × 10% = 4명

// 실제 배정 (합격자 수에서 순차적으로 차감)
int actualA = Math.min(targetA, passedStudents.size()); // min(13, 32) = 13명 ✅
int remainingAfterA = passedStudents.size() - actualA;   // 32 - 13 = 19명

int actualB = Math.min(targetB, remainingAfterA);        // min(17, 19) = 17명 ✅
int remainingAfterB = remainingAfterA - actualB;         // 19 - 17 = 2명

int actualC = Math.min(targetC, remainingAfterB);        // min(8, 2) = 2명 ⚠️ (목표 미달)
int remainingAfterC = remainingAfterB - actualC;         // 2 - 2 = 0명

int actualD = Math.min(targetD, remainingAfterC);        // min(4, 0) = 0명 ❌ (배정 불가)
```

**결과**:
- A: 13명 (목표 달성)
- B: 17명 (목표 달성)
- C: 2명 (목표 8명이었으나 2명만 배정)
- D: 0명 (목표 4명이었으나 배정 불가)
- F: 10명 (60% 미만 자동 배정)

**예시 2**: 전체 42명, 합격 12명, 불합격(F) 30명

```java
int actualA = Math.min(13, 12); // 13명 목표 → 12명만 배정 가능
int actualB = Math.min(17, 0);  // 남은 학생 없음 → 0명
int actualC = Math.min(8, 0);   // 남은 학생 없음 → 0명
int actualD = Math.min(4, 0);   // 남은 학생 없음 → 0명
```

**결과**:
- A: 12명 (합격자 전원)
- B: 0명
- C: 0명
- D: 0명
- F: 30명

**⚠️ 핵심 원리**: 
- F 학생이 많을수록 A~D 등급을 받을 "자리"가 줄어듦
- 등급은 항상 A → B → C → D 순서로 채워짐
- 남은 자리가 없으면 D부터 0명이 되고, 심하면 C, B도 0명 가능

### 3단계: 등급 할당

**시나리오 1**: 전체 42명, 합격 32명, F 10명

| 대상 | 순위 | 등급 | 목표 인원 | 실제 배정 | 조건 |
|------|------|------|----------|----------|------|
| 합격자 | 1~13 | A | 13명 (42×30%) | 13명 ✅ | percentage >= 60% |
| 합격자 | 14~30 | B | 17명 (42×40%) | 17명 ✅ | percentage >= 60% |
| 합격자 | 31~32 | C | 8명 (42×20%) | **2명** ⚠️ | percentage >= 60% |
| 합격자 | - | D | 4명 (42×10%) | **0명** ❌ | 남은 학생 없음 |
| 불합격자 | - | F | - | 10명 | **percentage < 60%** |

**시나리오 2**: 전체 42명, 합격 12명, F 30명

| 대상 | 순위 | 등급 | 목표 인원 | 실제 배정 | 조건 |
|------|------|------|----------|----------|------|
| 합격자 | 1~12 | A | 13명 (42×30%) | **12명** ⚠️ | 합격자 전원 A |
| 합격자 | - | B | 17명 (42×40%) | **0명** ❌ | 남은 학생 없음 |
| 합격자 | - | C | 8명 (42×20%) | **0명** ❌ | 남은 학생 없음 |
| 합격자 | - | D | 4명 (42×10%) | **0명** ❌ | 남은 학생 없음 |
| 불합격자 | - | F | - | 30명 | **percentage < 60%** |

**⚠️ 핵심**:
- 목표 인원은 **전체 학생 수** 기준으로 계산 (42명 × 비율)
- 실제 배정은 **합격자 수**로 제한됨 (60% 이상 학생만)
- F가 많을수록 C, D가 먼저 사라지고, 극단적으로 A만 남을 수 있음

---

## ⚠️ 주의사항

### 절대평가 우선 원칙

- **60% 미만 학생은 무조건 F**
- 상대평가는 60% 이상 학생들에게만 적용
- F 등급은 gradeDistribution에 포함하지 않아도 됨

### 동점자 처리

- **동일 percentage**: 먼저 수강신청한 학생 우선
- **권장**: 소수점 3자리까지 계산하여 동점자 최소화
- **경계선**: 60% 정확히 일치하는 경우 합격 처리

### 비율 합계 검증

```java
// A + B + C + D = 100% (F는 제외)
int sum = distribution.get("A") + distribution.get("B") + 
          distribution.get("C") + distribution.get("D");
if (sum != 100) {
    throw new IllegalArgumentException("A~D 등급 비율의 합계는 100%여야 합니다.");
}
```

### 극단적 케이스 처리

**케이스 1**: 모든 학생이 60% 미만
- 전원 F 등급
- A, B, C, D는 모두 0명
- 상대평가 미실행

**케이스 2**: 합격자가 극소수 (전체 42명 중 5명만 60% 이상)
```
목표: A=13, B=17, C=8, D=4 (전체 42명 기준)
실제: A=5, B=0, C=0, D=0 (합격자 5명뿐)
결과: 5명 전원 A 등급
```

**케이스 3**: 합격자가 절반 (전체 42명 중 21명 합격)
```
목표: A=13, B=17, C=8, D=4 (전체 42명 기준)
실제: 
  - A: min(13, 21) = 13명 ✅
  - B: min(17, 21-13) = 8명 (목표 17명 → 실제 8명) ⚠️
  - C: min(8, 21-21) = 0명 ❌
  - D: min(4, 0) = 0명 ❌
결과: A 13명, B 8명, C·D 없음
```

**케이스 4**: 60%에 동점자가 많은 경우
- 60.0% 학생은 모두 합격 처리 (D 대상)
- 59.9%는 F 등급

**⚠️ 중요**: 
- F 학생이 **등급 분포 비율을 갉아먹는** 구조
- D → C → B 순서로 인원이 줄어들거나 사라짐
- 극단적으로 **A만 배정**되는 상황 발생 가능

---

## 📊 최종 성적표 예시

### 시나리오 1: 정상적인 분포 (전체 42명, 합격 32명, F 10명)

| 학생 | 출석 | 과제 | 합계 | % | 등급 | 비고 |
|------|------|------|------|-----|------|------|
| 이영희 | 18.5 | 70.1 | 88.6 | 88.6% | **A** | 상위 13명 (목표: 13명) |
| 박민수 | 17.2 | 54.3 | 71.5 | 71.5% | **B** | 14~30위 (목표: 17명) |
| 김철수 | 13.1 | 53.3 | 66.4 | 66.4% | **C** | 31~32위 (목표: 8명 → 실제 2명) |
| 정수진 | 12.3 | 49.4 | 61.7 | 61.7% | **C** | 32위 (D 배정 불가) |
| 최하나 | 5.0 | 14.9 | 19.9 | 19.9% | **F** | **60% 미만 (10명)** |

**등급 분포**:
- A: 13명 (42명의 30% 목표 → 달성 ✅)
- B: 17명 (42명의 40% 목표 → 달성 ✅)
- C: 2명 (42명의 20% 목표 8명 → **실제 2명만 배정** ⚠️)
- D: 0명 (42명의 10% 목표 4명 → **배정 불가** ❌)
- F: 10명 (60% 미만)

---

### 시나리오 2: 불합격자 다수 (전체 42명, 합격 12명, F 30명)

| 학생 | 출석 | 과제 | 합계 | % | 등급 | 비고 |
|------|------|------|------|-----|------|------|
| 이영희 | 18.5 | 70.1 | 88.6 | 88.6% | **A** | 합격자 12명 중 1위 |
| 박민수 | 17.2 | 54.3 | 71.5 | 71.5% | **A** | 합격자 12명 중 2위 |
| 김철수 | 13.1 | 53.3 | 66.4 | 66.4% | **A** | 합격자 12명 중 10위 |
| 정수진 | 12.3 | 49.4 | 61.7 | 61.7% | **A** | 합격자 12명 중 12위 |
| 최하나 | 5.0 | 14.9 | 19.9 | 19.9% | **F** | **60% 미만 (30명)** |

**등급 분포**:
- A: 12명 (42명의 30% 목표 13명 → **실제 12명만 배정** ⚠️)
- B: 0명 (42명의 40% 목표 17명 → **배정 불가** ❌)
- C: 0명 (42명의 20% 목표 8명 → **배정 불가** ❌)
- D: 0명 (42명의 10% 목표 4명 → **배정 불가** ❌)
- F: 30명 (60% 미만) → **F가 전체의 71%를 차지**

**⚠️ 핵심**:
- 총점 100점 기준, 60점 = 60%
- F 학생이 등급 비율의 "자리"를 차지함
- F가 많을수록 D → C → B → A 순서로 사라짐
- 극단적으로 **A 등급만 존재**하는 상황 발생 가능

---

## 📋 완료

등급 배정 완료 후:

- 학생은 **성적 조회 API**에서 최종 등급 확인 가능
- 교수는 **성적 확인서 발급 API**로 성적 증명서 생성 가능

→ [05. 성적 조회 API](./05_성적조회_API.md) (letterGrade 표시됨)
