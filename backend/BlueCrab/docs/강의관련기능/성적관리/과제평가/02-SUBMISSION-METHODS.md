# 📤 과제 제출 방법 코드표

제출 방법(submissionMethod) 표준 코드 정의

---

## 📋 제출 방법 코드

### 표준 코드 목록

| 코드 | 한글명 | 영문명 | 설명 |
|------|--------|--------|------|
| `email` | 이메일 제출 | Email Submission | 학생이 과제를 이메일로 제출 |
| `print` | 서면 제출 | Printed Submission | 출력물을 직접 제출 |
| `hands` | 손수 전달 | Hand-delivered | 학생이 교수에게 직접 전달 |
| `absent` | 미제출 | Not Submitted | 과제를 제출하지 않음 |

---

## 🔍 상세 설명

### 1. email (이메일 제출)

**사용 시나리오**:
- 학생이 교수 이메일로 과제 파일 전송
- 교수가 이메일 확인 후 평가 입력

**입력 예시**:
```javascript
{
  "submissionMethod": "email",
  "score": 9
}
```

**특징**:
- ✅ 제출 증거 명확 (이메일 기록)
- ✅ 파일 첨부 가능
- ⏱️ 제출 시간 확인 가능 (이메일 타임스탬프)

---

### 2. print (서면 제출)

**사용 시나리오**:
- 학생이 과제를 출력하여 제출
- 수업 시간 또는 연구실에 직접 제출

**입력 예시**:
```javascript
{
  "submissionMethod": "print",
  "score": 8
}
```

**특징**:
- ✅ 물리적 증거물 존재
- ✅ 수기 작성 과제에 적합
- ⚠️ 분실 위험 존재

---

### 3. hands (손수 전달)

**사용 시나리오**:
- 학생이 교수에게 직접 만나서 전달
- USB, CD 등 매체로 전달
- 비공식적 제출

**입력 예시**:
```javascript
{
  "submissionMethod": "hands",
  "score": 10
}
```

**특징**:
- ✅ 즉시 확인 가능
- ✅ 학생-교수 직접 소통
- ⚠️ 제출 증거 부족

---

### 4. absent (미제출)

**사용 시나리오**:
- 학생이 과제를 제출하지 않음
- 0점 처리

**입력 예시**:
```javascript
{
  "submissionMethod": "absent",
  "score": 0
}
```

**특징**:
- ⚠️ 자동으로 0점 처리
- 📊 제출률 통계에서 미제출로 집계

---

## 💻 프론트엔드 UI 예시

### 드롭다운 메뉴

```html
<select id="submissionMethod">
  <option value="">-- 제출 방법 선택 --</option>
  <option value="email">📧 이메일 제출</option>
  <option value="print">📄 서면 제출</option>
  <option value="hands">🤝 손수 전달</option>
  <option value="absent">❌ 미제출</option>
</select>
```

### 라디오 버튼

```html
<div class="submission-method">
  <label>
    <input type="radio" name="method" value="email">
    📧 이메일 제출
  </label>
  <label>
    <input type="radio" name="method" value="print">
    📄 서면 제출
  </label>
  <label>
    <input type="radio" name="method" value="hands">
    🤝 손수 전달
  </label>
  <label>
    <input type="radio" name="method" value="absent">
    ❌ 미제출
  </label>
</div>
```

### JavaScript 검증

```javascript
function validateSubmissionMethod(method) {
  const validMethods = ['email', 'print', 'hands', 'absent'];
  
  if (!validMethods.includes(method)) {
    throw new Error(`잘못된 제출 방법: ${method}`);
  }
  
  return true;
}

// 사용 예시
try {
  validateSubmissionMethod('email'); // ✅ 통과
  validateSubmissionMethod('online'); // ❌ 에러
} catch (error) {
  console.error(error.message);
}
```

---

## 📊 통계 표시

### 제출 방법별 집계

```javascript
function getMethodStatistics(submissions) {
  const stats = {
    email: 0,
    print: 0,
    hands: 0,
    absent: 0
  };
  
  submissions
    .filter(s => s.submitted)
    .forEach(s => {
      if (stats.hasOwnProperty(s.submissionMethod)) {
        stats[s.submissionMethod]++;
      }
    });
  
  return stats;
}

// 사용 예시
const stats = getMethodStatistics(submissions);
console.log('제출 방법별 통계:');
console.log(`📧 이메일: ${stats.email}명`);
console.log(`📄 서면: ${stats.print}명`);
console.log(`🤝 손수 전달: ${stats.hands}명`);
console.log(`❌ 미제출: ${stats.absent}명`);
```

### 차트 데이터 변환

```javascript
function toChartData(stats) {
  return {
    labels: ['이메일', '서면', '손수 전달', '미제출'],
    data: [stats.email, stats.print, stats.hands, stats.absent],
    colors: ['#4CAF50', '#2196F3', '#FF9800', '#F44336']
  };
}

// Chart.js 예시
const chartData = toChartData(stats);
new Chart(ctx, {
  type: 'pie',
  data: {
    labels: chartData.labels,
    datasets: [{
      data: chartData.data,
      backgroundColor: chartData.colors
    }]
  }
});
```

---

## 🌐 다국어 지원

### 한글-영문 매핑

```javascript
const methodNames = {
  ko: {
    email: '이메일 제출',
    print: '서면 제출',
    hands: '손수 전달',
    absent: '미제출'
  },
  en: {
    email: 'Email Submission',
    print: 'Printed Submission',
    hands: 'Hand-delivered',
    absent: 'Not Submitted'
  }
};

function getMethodName(code, lang = 'ko') {
  return methodNames[lang][code] || code;
}

// 사용 예시
console.log(getMethodName('email', 'ko')); // "이메일 제출"
console.log(getMethodName('email', 'en')); // "Email Submission"
```

---

## ⚠️ 주의사항

### 1. 대소문자 구분

```javascript
✅ 올바른 코드: 'email', 'print', 'hands', 'absent'
❌ 잘못된 코드: 'Email', 'PRINT', 'Hands', 'ABSENT'
```

### 2. 한글 사용 금지

```javascript
✅ 올바른 코드: 'email'
❌ 잘못된 코드: '이메일', '이메일 제출'
```

### 3. 커스텀 코드 금지

```javascript
✅ 표준 코드만 사용: 'email', 'print', 'hands', 'absent'
❌ 임의 코드 사용: 'online', 'upload', 'direct'
```

### 4. 미제출 처리

`absent` 코드 사용 시 **자동으로 0점**이 입력되어야 합니다:

```javascript
// 미제출 처리 시 점수 자동 설정
if (submissionMethod === 'absent') {
  score = 0;
}
```

---

## 🔄 향후 확장 가능성

### 추가 가능한 제출 방법

현재는 4가지 방법만 지원하지만, 향후 다음과 같은 방법 추가 가능:

```javascript
// 향후 추가 가능 코드 (현재 미지원)
{
  "usb": "USB 제출",
  "cloud": "클라우드 링크 제출",
  "git": "GitHub 제출",
  "presentation": "발표 제출"
}
```

### 확장 시 고려사항

1. **백엔드 검증 로직 업데이트**
2. **프론트엔드 UI 수정**
3. **통계 집계 로직 확장**
4. **문서 업데이트**

---

> **참고**: [01-API-SPECIFICATION.md](./01-API-SPECIFICATION.md)에서 API 사용 예시 확인
