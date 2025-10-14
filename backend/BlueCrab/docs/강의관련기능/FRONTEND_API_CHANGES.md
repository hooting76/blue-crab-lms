# 프론트엔드 팀 필독: API 변경사항

> **날짜**: 2025-10-14  
> **중요도**: 🔴 HIGH - 즉시 반영 필요  
> **영향 범위**: 수강신청 관련 모든 화면

---

## 📢 변경 요약

**수강신청 API 응답 구조가 변경**되었습니다. 교수 정보 표시 방식이 개선되어 **교수 코드**와 **교수 이름**이 분리되었습니다.

---

## 🔄 변경 내용

### **1. API 응답 구조**

#### **변경 전 (Old)**
```json
{
  "enrollmentIdx": 1,
  "lecTit": "자바 프로그래밍",
  "lecProf": "김교수",           // ❌ 교수 이름만 있음
  "studentName": "홍길동"
}
```

#### **변경 후 (New)**
```json
{
  "enrollmentIdx": 1,
  "lecTit": "자바 프로그래밍",
  "lecProf": "PROF001",          // ✅ 교수 코드 (USER_CODE)
  "lecProfName": "김교수",       // ✅ 교수 이름 (USER_NAME) - 신규!
  "studentName": "홍길동"
}
```

### **2. 필드 설명**

| 필드명 | 타입 | 설명 | 예시 값 |
|--------|------|------|---------|
| `lecProf` | String | 교수 코드 (USER_CODE) | "PROF001", "11" |
| `lecProfName` | String | **교수 이름 (USER_NAME)** ⭐ | "김교수", "굴림체" |

---

## 💻 프론트엔드 코드 수정 가이드

### **React 컴포넌트 예시**

#### **수정 전 (Old Code)**
```jsx
// ❌ 더 이상 작동하지 않음
function EnrollmentCard({ enrollment }) {
  return (
    <Card>
      <Typography variant="h6">{enrollment.lecTit}</Typography>
      <Typography variant="body2">
        교수: {enrollment.lecProf}  {/* "PROF001" 같은 코드가 표시됨 */}
      </Typography>
    </Card>
  );
}
```

#### **수정 후 (New Code)**
```jsx
// ✅ 올바른 방식
function EnrollmentCard({ enrollment }) {
  return (
    <Card>
      <Typography variant="h6">{enrollment.lecTit}</Typography>
      <Typography variant="body2">
        교수: {enrollment.lecProfName}  {/* "김교수" 정상 표시 */}
      </Typography>
    </Card>
  );
}
```

#### **Fallback 처리 (권장)**
```jsx
// ✅ 안전한 방식 (구버전 호환)
function EnrollmentCard({ enrollment }) {
  const professorName = enrollment.lecProfName 
    || enrollment.professorName 
    || enrollment.lecProf;

  return (
    <Card>
      <Typography variant="h6">{enrollment.lecTit}</Typography>
      <Typography variant="body2">
        교수: {professorName}
      </Typography>
    </Card>
  );
}
```

### **Vue.js 예시**
```vue
<template>
  <div class="enrollment-card">
    <h3>{{ enrollment.lecTit }}</h3>
    <!-- ✅ 수정 -->
    <p>교수: {{ enrollment.lecProfName }}</p>
  </div>
</template>
```

### **Angular 예시**
```typescript
@Component({
  selector: 'app-enrollment-card',
  template: `
    <div class="enrollment-card">
      <h3>{{ enrollment.lecTit }}</h3>
      <!-- ✅ 수정 -->
      <p>교수: {{ enrollment.lecProfName }}</p>
    </div>
  `
})
export class EnrollmentCardComponent {
  @Input() enrollment: Enrollment;
}
```

---

## 🎯 영향받는 페이지/컴포넌트

### **확인 필요 항목**

- [ ] **학생 - 수강신청 목록** (`/student/enrollments`)
- [ ] **학생 - 내 강의** (`/student/my-courses`)
- [ ] **학생 - 강의 상세** (`/student/courses/:id`)
- [ ] **교수 - 수강생 목록** (`/professor/students`)
- [ ] **관리자 - 수강신청 현황** (`/admin/enrollments`)
- [ ] **LectureCard 컴포넌트**
- [ ] **EnrollmentCard 컴포넌트**
- [ ] **CourseList 컴포넌트**

### **검색 키워드**

다음 키워드로 프로젝트 전체 검색을 권장합니다:
```
enrollment.lecProf
enrollment.professorName
lecture.professorName
```

---

## 🔍 테스트 가이드

### **1. API 응답 확인**

**브라우저 개발자 도구 → Network 탭에서 확인:**

```bash
# 요청
GET /api/enrollments?studentIdx=6&page=0&size=10

# 응답 (확인 사항)
{
  "content": [{
    "lecProf": "PROF001",      // ✅ 코드 형태인지 확인
    "lecProfName": "김교수"    // ✅ 이 필드가 있는지 확인
  }]
}
```

### **2. 화면 표시 확인**

- [ ] 교수 이름이 정상적으로 표시되는가?
- [ ] "PROF001" 같은 코드가 화면에 보이지 않는가?
- [ ] null 또는 undefined로 인한 에러가 없는가?

### **3. 브라우저 콘솔 테스트**

```javascript
// 테스트 스크립트
await fetch('/api/enrollments?studentIdx=6', {
  headers: {
    'Authorization': 'Bearer YOUR_TOKEN'
  }
})
  .then(r => r.json())
  .then(data => {
    const enrollment = data.content[0];
    console.log('교수 코드:', enrollment.lecProf);
    console.log('교수 이름:', enrollment.lecProfName);
  });
```

---

## 📝 TypeScript 타입 정의

```typescript
// types/enrollment.ts

// ❌ 기존 타입 정의
interface EnrollmentOld {
  enrollmentIdx: number;
  lecTit: string;
  lecProf: string;  // 교수 이름
  studentName: string;
}

// ✅ 새로운 타입 정의
interface Enrollment {
  enrollmentIdx: number;
  lecIdx: number;
  lecSerial: string;
  lecTit: string;
  lecProf: string;      // 교수 코드 (USER_CODE)
  lecProfName: string;  // 교수 이름 (USER_NAME) - 신규!
  lecPoint: number;
  lecTime: string;
  studentIdx: number;
  studentCode: string;
  studentName: string;
  enrollmentStatus: 'ENROLLED' | 'CANCELLED';
  enrollmentDate: string;
  cancelDate?: string;
  cancelReason?: string;
  attendanceRecords?: AttendanceRecord[];
  grade?: Grade;
}
```

---

## 🚀 배포 전 체크리스트

- [ ] 모든 컴포넌트에서 `lecProf` → `lecProfName` 변경 완료
- [ ] TypeScript 타입 정의 업데이트 (있는 경우)
- [ ] 단위 테스트 업데이트
- [ ] E2E 테스트 업데이트
- [ ] 로컬 환경에서 화면 확인
- [ ] 개발 서버에서 통합 테스트
- [ ] 교수 이름이 정상 표시되는지 확인

---

## ❓ FAQ

### Q1: 기존 데이터는 어떻게 처리되나요?
**A:** 백엔드에서 자동으로 변환됩니다. 모든 API 응답에 `lecProfName` 필드가 포함됩니다.

### Q2: lecProf 필드는 삭제되나요?
**A:** 아니오, `lecProf`는 교수 코드로 유지됩니다. `lecProfName`이 추가된 것입니다.

### Q3: 구버전 호환이 필요한가요?
**A:** Fallback 처리 (`lecProfName || lecProf`)를 권장하지만, 필수는 아닙니다.

### Q4: 다른 API도 영향받나요?
**A:** 수강신청 관련 API만 영향받습니다. 강의 목록 조회 등은 변경 없습니다.

### Q5: 언제부터 적용되나요?
**A:** 2025-10-14 배포부터 즉시 적용됩니다.

---

## 📞 문의

수정 중 문제가 발생하면:
- **백엔드 담당**: 성태준
- **문서**: `docs/강의관련기능/02-API명세서.md`
- **이슈 트래킹**: GitHub Issues에 `[Frontend] API 변경` 태그로 등록

---

**작성자**: 백엔드 팀  
**검토자**: -  
**최종 수정**: 2025-10-14
