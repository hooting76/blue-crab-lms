# 게시판 API 사용 가이드 (프론트엔드)

> **작성일**: 2025년 10월 21일  
> **버전**: 1.0  
> **대상**: 프론트엔드 개발자

---

## 📋 변경 사항 요약

게시판 시스템의 BOARD_CODE 체계가 변경되었으며, **강의 공지** 기능이 추가되었습니다.

### 🔄 BOARD_CODE 변경

| Code | 변경 전 | 변경 후 | 권한 | 비고 |
|------|---------|---------|------|------|
| 0 | 학교공지 | **학사공지** | 관리자 | - |
| 1 | 학사공지 | **행정공지** | 관리자 | - |
| 2 | 학과공지 | **기타공지** | 관리자 | - |
| 3 | 교수공지 | **강의공지** | 관리자 + 교수 | ⭐ **lecSerial 필수** |

---

## 🔐 권한 체계

### 사용자별 작성 권한

| 사용자 | BOARD_CODE 0-2 | BOARD_CODE 3 (강의공지) |
|--------|----------------|------------------------|
| 👑 **관리자** | ✅ 모든 공지 작성 가능 | ✅ 모든 강의 공지 작성 |
| 🎓 **교수** | ❌ 작성 불가 | ✅ 담당 강의만 작성 가능 |
| 📚 **학생** | ❌ 작성 불가 | ❌ 작성 불가 |

### 중요 사항
- **BOARD_CODE 3 (강의공지)**: `lecSerial` 필드 필수
- **교수 권한**: 본인이 담당하는 강의(LEC_PROF = 본인 USER_IDX)만 작성 가능
- **관리자 권한**: 모든 강의에 공지 작성 가능

---

## 🚀 API 사용 방법

### 📤 게시글 생성 API

**Endpoint**: `POST /api/boards/create`

**Headers**:
```
Content-Type: application/json
Authorization: Bearer {JWT_TOKEN}
```

#### 예제 1: 학사공지 (관리자만)

```json
{
  "boardCode": 0,
  "boardTitle": "2025학년도 1학기 수강신청 안내",
  "boardContent": "수강신청 기간은 3월 1일부터..."
}
```

#### 예제 2: 강의공지 (관리자 + 교수) ⭐

```json
{
  "boardCode": 3,
  "lecSerial": "ETH201",
  "boardTitle": "중간고사 일정 변경",
  "boardContent": "중간고사 일정이 다음과 같이 변경됩니다..."
}
```

**✅ 성공 응답 (200 OK)**:
```json
{
  "success": true,
  "data": {
    "boardIdx": 123,
    "boardCode": 3,
    "lecSerial": "ETH201",
    "boardTitle": "중간고사 일정 변경",
    "boardContent": "중간고사 일정이...",
    "boardWriter": "김교수",
    "boardReg": "2025-10-21T10:30:00"
  }
}
```

---

## ⚠️ 에러 처리

### 주요 에러 케이스

#### 1. BOARD_CODE 범위 초과
```json
{
  "errorCode": "VALIDATION_ERROR",
  "message": "게시글 코드는 3 이하여야 합니다"
}
```
→ **해결**: BOARD_CODE는 0~3만 사용

#### 2. lecSerial 누락 (BOARD_CODE=3)
```json
{
  "errorCode": "BOARD_CREATION_ERROR",
  "message": "강의 공지 작성 시 강의 코드(LEC_SERIAL)는 필수입니다."
}
```
→ **해결**: BOARD_CODE가 3이면 반드시 `lecSerial` 필드 포함

#### 3. 권한 없음 (교수가 BOARD_CODE 0-2 시도)
```json
{
  "errorCode": "BOARD_CREATION_ERROR",
  "message": "학사/행정/기타 공지는 관리자만 작성할 수 있습니다."
}
```
→ **해결**: 교수 계정은 BOARD_CODE 3만 사용

#### 4. 담당 강의 아님 (교수)
```json
{
  "errorCode": "BOARD_CREATION_ERROR",
  "message": "본인이 담당하는 강의에만 공지를 작성할 수 있습니다. (강의코드: ETH201)"
}
```
→ **해결**: 교수가 담당하는 강의 코드만 사용

---

## 🎨 프론트엔드 구현 가이드

### 1. BOARD_CODE 선택 UI

```javascript
const BOARD_CODES = {
  0: { name: '학사공지', auth: 'admin', requiresLecSerial: false },
  1: { name: '행정공지', auth: 'admin', requiresLecSerial: false },
  2: { name: '기타공지', auth: 'admin', requiresLecSerial: false },
  3: { name: '강의공지', auth: 'admin-or-professor', requiresLecSerial: true }
};

// 사용자 역할에 따라 선택 가능한 BOARD_CODE 필터링
function getAvailableBoardCodes(userRole) {
  if (userRole === 'admin') {
    return [0, 1, 2, 3]; // 관리자: 모든 코드
  } else if (userRole === 'professor') {
    return [3]; // 교수: 강의공지만
  } else {
    return []; // 학생: 작성 불가
  }
}
```

### 2. lecSerial 필드 조건부 표시

```javascript
function shouldShowLecSerialField(boardCode) {
  return boardCode === 3;
}

// BOARD_CODE 3 선택 시 lecSerial 입력 필드 표시
if (shouldShowLecSerialField(selectedBoardCode)) {
  // lecSerial 입력 필드 표시
  // 교수인 경우: 본인 담당 강의 목록에서 선택
  // 관리자인 경우: 모든 강의 목록에서 선택
}
```

### 3. 유효성 검증

```javascript
function validateBoardData(data, userRole) {
  // BOARD_CODE 범위 검증
  if (data.boardCode < 0 || data.boardCode > 3) {
    return { valid: false, error: 'BOARD_CODE는 0~3만 사용 가능합니다.' };
  }
  
  // 권한 검증
  if (userRole === 'professor' && data.boardCode !== 3) {
    return { valid: false, error: '교수는 강의공지만 작성할 수 있습니다.' };
  }
  
  // lecSerial 필수 검증
  if (data.boardCode === 3 && !data.lecSerial) {
    return { valid: false, error: '강의공지는 강의 코드(lecSerial)가 필수입니다.' };
  }
  
  return { valid: true };
}
```

### 4. 게시글 생성 함수 예제

```javascript
async function createBoard(boardData) {
  try {
    const response = await fetch('/api/boards/create', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${getAuthToken()}`
      },
      body: JSON.stringify(boardData)
    });
    
    const result = await response.json();
    
    if (response.ok) {
      console.log('게시글 생성 성공:', result.data);
      return { success: true, data: result.data };
    } else {
      console.error('게시글 생성 실패:', result.message);
      return { success: false, error: result.message };
    }
  } catch (error) {
    console.error('네트워크 오류:', error);
    return { success: false, error: '네트워크 오류가 발생했습니다.' };
  }
}
```

---

## 🧪 테스트 파일 사용법

이 폴더에는 브라우저 콘솔에서 직접 실행 가능한 테스트 파일들이 포함되어 있습니다.

### 📁 파일 구조

```
docs/게시판/
├── test-1-create.js        # 게시글 생성 테스트
├── test-2-read.js          # 게시글 조회 테스트
├── test-3-update-delete.js # 게시글 수정/삭제 테스트
└── README.md              # 이 문서
```

### 🔧 test-1-create.js 주요 함수

```javascript
// 기본 생성 (프롬프트로 입력)
await createBoard()

// 개별 타입 생성
await createAcademicNotice()     // 학사공지 (관리자)
await createAdminNotice()        // 행정공지 (관리자)
await createOtherNotice()        // 기타공지 (관리자)
await createLectureNotice('ETH201')  // 강의공지 (관리자+교수)

// 검증 테스트
await testLectureNoticeValidation()  // lecSerial 필수 검증

// 전체 타입 테스트
await createAllTypes()
```

### 🔍 test-2-read.js 주요 함수

```javascript
// 상세 조회
await getBoardDetail(1)

// 목록 조회
await getBoardList()              // 전체 목록
await getBoardListByCode(3)       // 강의공지만
await getLectureNotices('ETH201') // 특정 강의 공지

// 빠른 조회
await quickViewAll()              // 각 유형별 최신 5개
```

---

## 📊 UI/UX 권장사항

### 게시글 작성 폼

1. **BOARD_CODE 선택**
   - 사용자 권한에 따라 선택 가능한 옵션만 표시
   - 각 코드에 대한 설명 툴팁 제공

2. **lecSerial 필드 (BOARD_CODE=3)**
   - BOARD_CODE 3 선택 시에만 표시
   - 교수: 담당 강의 드롭다운 (자동 필터링)
   - 관리자: 전체 강의 드롭다운

3. **제목/내용**
   - 필수 입력 필드
   - 적절한 유효성 검증

### 게시글 목록 화면

1. **필터링**
   - BOARD_CODE별 탭 또는 필터
   - 강의공지: 강의별 추가 필터

2. **표시 정보**
   - BOARD_CODE 뱃지 (색상 구분)
   - 강의공지: 강의 코드 표시
   - 작성자, 작성일, 조회수

---

## 📞 문의 및 지원

### 백엔드 API 이슈
- **담당**: 백엔드 개발팀
- **GitHub Issues**: [blue-crab-lms/issues](https://github.com/hooting76/blue-crab-lms/issues)

### 추가 문서
- [전체 API 명세서](../api-endpoints/)
- [프론트엔드 연동 가이드](../../프론트엔드_완전_연동_가이드.md)

---

## 📋 체크리스트

### 프론트엔드 구현 시 확인사항

- [ ] BOARD_CODE 선택 UI에 새 체계 반영 (0:학사, 1:행정, 2:기타, 3:강의)
- [ ] 사용자 권한별 BOARD_CODE 필터링 구현
- [ ] BOARD_CODE 3 선택 시 lecSerial 입력 필드 표시
- [ ] lecSerial 필수 검증 추가
- [ ] 교수 권한일 때 담당 강의만 선택 가능하도록 구현
- [ ] 에러 메시지 사용자 친화적으로 표시
- [ ] 게시글 목록에 강의 코드 표시 (BOARD_CODE=3)
- [ ] 강의별 필터링 기능 추가 (선택사항)

---

**마지막 업데이트**: 2025년 10월 21일

> 💡 **참고**: 이 문서는 게시판 API 사용을 위한 프론트엔드 가이드입니다.  
> 백엔드 구현 세부사항은 별도 문서를 참조하세요.
