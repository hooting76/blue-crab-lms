# 📢 백엔드 답변

**대상**: 신아름 (프론트엔드)  
**작성**: 2025-10-22

---

## 🎯 요약

| 기능 | 상태 | 시작 |
|-----|------|------|
| 기본 필터링 | ✅ 완료 | 즉시 |
| 취소 API | ✅ 완료 | 즉시 |
| 학부/학과명 | ✅ 완료 | 즉시 |
| 안내문 API | ✅ 완료 | [📄 ../안내문/](../안내문/) |
| 추가 검색 | ⏳ 구현 중 | 오늘(10/22) 중 |

---

## ✅ 즉시 사용 가능

### 1. 기본 필터링 (Phase 9)

```javascript
fetch('/api/lectures/eligible', {
  method: 'POST',
  headers: { 'Authorization': `Bearer ${token}` },  // JWT 토큰
  body: JSON.stringify({ studentId: 15, page: 0, size: 10 })
});
// → 백엔드가 JWT로 학생 자동 식별 + 학부/학과 자동 필터링
```

**💡 중요**: 백엔드가 모든 것을 자동 처리합니다!
- JWT 토큰으로 학생 자동 식별
- 학생의 학부/학과 자동 조회
- 수강 가능 강의만 자동 필터링
- 백엔드는 **코드만 반환** (`lecMcode`, `lecMcodeDep`)
- 프론트엔드가 **코드를 이름으로 변환** → [06-학부학과코드매핑.md](./06-학부학과코드매핑.md) 📚

자세히: [05-자동필터링원리.md](./05-자동필터링원리.md) ⭐ **필독**

### 2. 취소

```javascript
fetch(`/api/enrollments/${enrollmentIdx}`, {
  method: 'DELETE',
  headers: { 'Authorization': `Bearer ${token}` }
});
```

자세히: [02-취소API사용법.md](./02-취소API사용법.md)

---

## ✅ 안내문 API

```javascript
// 조회 (공개 - 인증 불필요)
fetch('http://localhost:8090/notice/course-apply/view', {
  method: 'POST',
  headers: { 'Content-Type': 'application/json' }
});

// 저장 (관리자/교수 - JWT 필요)
fetch('http://localhost:8090/notice/course-apply/save', {
  method: 'POST',
  headers: {
    'Content-Type': 'application/json',
    'Authorization': `Bearer ${token}`
  },
  body: JSON.stringify({ message: "수강신청 안내 메시지" })
});
```

**구현 완료**:

- Entity: `entity/Lecture/CourseApplyNotice.java`
- Repository: `repository/Lecture/CourseApplyNoticeRepository.java`
- Controller: `controller/Lecture/NoticeController.java`
- DTOs: 3개 (ViewResponse, SaveRequest, SaveResponse)
- DDL: `docs/ddl/course_apply_notice.sql` (테이블 생성 완료)
- Security: `/view` 공개, `/save` 인증 필요

자세히: [07-안내문API구현가이드.md](./07-안내문API구현가이드.md)

---

## ⏳ 안내문 (오늘 중 구현 예정)

```javascript
// 조회 (공개)
fetch('/notice/course-apply/view', {
  method: 'POST',
  body: JSON.stringify({})
});

// 저장 (관리자/교수)
fetch('/notice/course-apply/save', {
  method: 'POST',
  headers: { 'Authorization': `Bearer ${token}` },
  body: JSON.stringify({ message: "..." })
});
```

자세히: [03-안내문API명세.md](./03-안내문API명세.md)

---

## ⏳ 추가 검색 (오늘 중 구현 예정)

**파라미터 추가 시 AND 조건으로 필터링됩니다**

```json
{
  "studentId": 15,
  "lecYear": 2,       // 선택: 2학년 대상만
  "lecSemester": 1,   // 선택: 1학기만
  "lecMajor": 1,      // 선택: 전공만
  "page": 0,
  "size": 10
}
```

**⚠️ 강의명 검색은 로직이 복잡하여 지원하지 않습니다**

자세히: [01-필터링파라미터명세.md](./01-필터링파라미터명세.md)

---

## ⏳ 안내문 (오늘 중 구현 예정)

```javascript
// 조회 (공개)
fetch('/notice/course-apply/view', {
  method: 'POST',
  body: JSON.stringify({})
});

// 저장 (관리자/교수)
fetch('/notice/course-apply/save', {
  method: 'POST',
  headers: { 'Authorization': `Bearer ${token}` },
  body: JSON.stringify({ message: "..." })
});
```

자세히: [03-안내문API명세.md](./03-안내문API명세.md)

---

## 📚 전체 문서

- [00-요청검토결과.md](./00-요청검토결과.md) - 전체 검토
- [01-필터링파라미터명세.md](./01-필터링파라미터명세.md) - 추가 파라미터
- [02-취소API사용법.md](./02-취소API사용법.md) - 취소 가이드
- [03-안내문API명세.md](./03-안내문API명세.md) - 안내문 설계
- [04-통합요약.md](./04-통합요약.md) - 프론트팀 종합 ← 지금 보는 문서
- [05-자동필터링원리.md](./05-자동필터링원리.md) - ⭐ **백엔드 자동 처리 설명**
- [06-학부학과코드매핑.md](./06-학부학과코드매핑.md) - 📚 **학부/학과 코드 변환** (프론트엔드)

---

## ⚠️ 중요

**DB 컬럼명 사용**:
- `lecYear` (LEC_YEAR) - 학년
- `lecSemester` (LEC_SEMESTER) - 학기
- `lecMajor` (LEC_MAJOR) - 전공/교양
- `name` (LEC_TIT) - 강의명

**모두 선택 파라미터** - 생략 가능

**LEC_REG (등록일)** - 사용 안 함

---

> 권장: [04-통합요약.md](./04-통합요약.md) (이 문서)부터 시작
