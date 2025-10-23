# 🎓 교수 수강생 목록 조회 테스트 가이드

## 📋 현재 상황
- ✅ 프론트 코드 수정 완료 (camelCase 필드명 대응)
- ❓ ETH201에 실제 수강생이 있는지 확인 필요

## 🔍 1단계: DB 확인

### HeidiSQL에서 실행:
```sql
-- 수강생이 있는 강의 찾기
SELECT 
    l.LEC_SERIAL as '강의코드',
    l.LEC_TIT as '강의명',
    COUNT(e.ENROLLMENT_IDX) as '수강생수'
FROM LEC_TBL l
INNER JOIN ENROLLMENT_EXTENDED_TBL e ON l.LEC_IDX = e.LEC_IDX
GROUP BY l.LEC_IDX, l.LEC_SERIAL, l.LEC_TIT
ORDER BY COUNT(e.ENROLLMENT_IDX) DESC;
```

**결과 예시**:
```
강의코드  | 강의명              | 수강생수
---------|---------------------|--------
CS2310   | 컴퓨터과학개론       | 1
ETH201   | 윤리학개론          | 1
...
```

## 🧪 2단계: 브라우저 테스트

### 2-1. 로그인 (이미 완료했다면 Skip)
```javascript
await login();  // prof.octopus@univ.edu로 로그인
```

### 2-2. 수강생 목록 조회
```javascript
await getStudents();
// 프롬프트에서 위 SQL 결과에 나온 강의 코드 입력 (예: CS2310)
```

**예상 결과**:
```
✅ 조회 성공!
📊 총 1명 수강생
📋 수강생 목록:

1. 홍길동 (2021001)
   IDX: 6
   강의 코드: CS2310
   등록일: 2025-10-23
```

## 🐛 3단계: 문제 해결

### Case 1: "⚠️ 수강생이 없습니다"
→ 해당 강의에 실제로 수강생이 없음
→ DB에서 수강생이 있는 다른 강의 코드로 테스트

### Case 2: 필드가 undefined 또는 N/A로 표시
→ DTO 필드명 불일치 가능성
→ 디버그 스크립트 사용:

```javascript
// lecture-test-5-professor-students-debug.js 로드 후
await debugGetStudents();
// 응답 구조 상세 확인
```

### Case 3: 여전히 500 에러
→ 다른 강의 코드도 중복일 가능성
→ SQL 실행:
```sql
SELECT LEC_SERIAL, COUNT(*) as CNT
FROM LEC_TBL
GROUP BY LEC_SERIAL
HAVING COUNT(*) > 1;
```

## 📝 4단계: EnrollmentDto 필드명 확인

현재 백엔드 DTO 필드 (camelCase):
- `enrollmentIdx`
- `studentIdx`
- `studentName`
- `studentCode`
- `lecSerial`
- `lecTit`
- `lecProf`
- `lecProfName`
- `enrollmentDate`
- `enrollmentStatus`

프론트 코드에서 사용:
- ✅ `student.studentName`
- ✅ `student.studentIdx`
- ✅ `student.lecSerial`
- ❌ `student.studentNo` (DTO에 없음 → `studentCode` 사용)
- ❌ `student.studentEmail` (DTO에 없음)
- ❌ `student.enrolledAt` (DTO에서는 `enrollmentDate`)

## 🔧 5단계: 프론트 코드 최종 수정

```javascript
result.content.forEach((student, idx) => {
    console.log(`\n${idx + 1}. ${student.studentName || 'N/A'} (${student.studentCode || 'N/A'})`);
    console.log(`   IDX: ${student.studentIdx || 'N/A'}`);
    console.log(`   강의: ${student.lecTit || 'N/A'} (${student.lecSerial || 'N/A'})`);
    console.log(`   담당교수: ${student.lecProfName || student.lecProf || 'N/A'}`);
    console.log(`   등록일: ${student.enrollmentDate || 'N/A'}`);
    console.log(`   상태: ${student.enrollmentStatus || 'N/A'}`);
});
```

## ✅ 성공 기준

1. HTTP 200 OK
2. `content` 배열에 데이터 존재
3. `totalElements` > 0
4. 학생 정보 정상 표시 (이름, 학번, 강의코드 등)
