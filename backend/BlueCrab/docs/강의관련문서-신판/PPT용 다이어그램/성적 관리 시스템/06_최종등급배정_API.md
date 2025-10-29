# 06. 최종 등급 배정 API

> 교수가 상대평가로 A/B/C/D/F 등급을 배정하는 API

---

## 📌 기본 정보

- **엔드포인트**: `POST /api/enrollments/assign-grades`
- **권한**: 교수
- **설명**: 전체 수강생의 점수 분포를 기준으로 상대평가 등급 배정

---

## 📥 Request

```json
{
  "lecSerial": "ETH201",
  "gradeDistribution": {
    "A": 30,
    "B": 40,
    "C": 20,
    "D": 5,
    "F": 5
  }
}
```

| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| lecSerial | String | ✅ | 강의 코드 |
| gradeDistribution | Object | ✅ | 등급별 비율 (합계 100%) |

---

## 📤 Response

```json
{
  "success": true,
  "message": "42명의 최종 등급이 배정되었습니다.",
  "gradeResult": {
    "A": 13,
    "B": 17,
    "C": 8,
    "D": 2,
    "F": 2
  }
}
```

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
      "percentage": 43.5
    },
    "letterGrade": "C"
  }
}
```

### 하위 10% (D/F)

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

---

## 🔄 시퀀스 다이어그램

```plaintext
교수 → API: 등급 배정 요청 (A 30%, B 40%, ...)
API → DB: 해당 강의 전체 수강생 조회
API → 로직: percentage 기준 내림차순 정렬
API → 로직: 상위 30%에 A, 30~70%에 B, ... 배정
API → DB: 각 수강생의 ENROLLMENT_DATA.letterGrade 업데이트
DB → API: 42건 업데이트 완료
API → 교수: 등급 배정 결과 (A 13명, B 17명, ...)
```

---

## 💡 등급 배정 알고리즘

### 1단계: 점수 정렬

```java
List<Enrollment> students = enrollmentRepository.findByLecIdx(lecIdx);
students.sort((a, b) -> {
    double percentageA = a.getEnrollmentData().getGrade().getTotal().getPercentage();
    double percentageB = b.getEnrollmentData().getGrade().getTotal().getPercentage();
    return Double.compare(percentageB, percentageA); // 내림차순
});
```

### 2단계: 비율 계산

- 총 42명
- A (30%): 42 × 0.30 = 12.6 → **13명**
- B (40%): 42 × 0.40 = 16.8 → **17명**
- C (20%): 42 × 0.20 = 8.4 → **8명**
- D (5%): 42 × 0.05 = 2.1 → **2명**
- F (5%): 42 × 0.05 = 2.1 → **2명**

### 3단계: 등급 할당

| 순위 | 등급 | 인원 |
|------|------|------|
| 1~13 | A | 13명 |
| 14~30 | B | 17명 |
| 31~38 | C | 8명 |
| 39~40 | D | 2명 |
| 41~42 | F | 2명 |

---

## ⚠️ 주의사항

### 동점자 처리

- **동일 percentage**: 먼저 수강신청한 학생 우선
- **권장**: 소수점 3자리까지 계산하여 동점자 최소화

### 비율 합계 검증

```java
int sum = distribution.values().stream().mapToInt(Integer::intValue).sum();
if (sum != 100) {
    throw new IllegalArgumentException("등급 비율의 합계는 100%여야 합니다.");
}
```

---

## 📊 최종 성적표 예시

| 학생 | 출석 | 과제 | 합계 | % | 등급 |
|------|------|------|------|-----|------|
| 이영희 | 105.0 | 140.5 | 245.5 | 88.6% | **A** |
| 박민수 | 100.5 | 97.5 | 198.0 | 71.5% | **B** |
| 김철수 | 75.5 | 45.0 | 120.5 | 43.5% | **C** |
| 최하나 | 30.0 | 25.0 | 55.0 | 19.9% | **F** |

---

## 📋 완료

등급 배정 완료 후:

- 학생은 **성적 조회 API**에서 최종 등급 확인 가능
- 교수는 **성적 확인서 발급 API**로 성적 증명서 생성 가능

→ [05. 성적 조회 API](./05_성적조회_API.md) (letterGrade 표시됨)
