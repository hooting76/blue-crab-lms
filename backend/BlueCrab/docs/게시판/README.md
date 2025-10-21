# 게시판 시스템 BOARD_CODE 업데이트 가이드

> **작성일**: 2025년 10월 21일  
> **버전**: 1.0  
> **대상**: 개발자, 테스터, 관리자

---

## 📋 개요

이 문서는 BlueCrab LMS 게시판 시스템의 BOARD_CODE 체계 변경 및 강의 공지 기능 추가에 대한 종합 가이드입니다.

### 🔄 주요 변경사항

| **항목** | **변경 전** | **변경 후** |
|---------|-----------|-----------|
| BOARD_CODE 0 | 학교공지 | **학사공지** |
| BOARD_CODE 1 | 학사공지 | **행정공지** |
| BOARD_CODE 2 | 학과공지 | **기타공지** |
| BOARD_CODE 3 | 교수공지 | **강의공지** ⭐ |
| 새 기능 | - | **LEC_SERIAL 필드** 추가 |
| 교수 권한 | - | **강의별 접근 제어** 추가 |

---

## 🏗️ 시스템 아키텍처

### 📊 BOARD_CODE 체계

```
┌─────────────────────────────────────────────┐
│                BOARD_CODE                   │
├─────────┬───────────┬─────────┬─────────────┤
│ Code    │ Type      │ Auth    │ LEC_SERIAL  │
├─────────┼───────────┼─────────┼─────────────┤
│ 0       │ 학사공지   │ Admin   │ Not Required│
│ 1       │ 행정공지   │ Admin   │ Not Required│
│ 2       │ 기타공지   │ Admin   │ Not Required│
│ 3       │ 강의공지   │ Admin+  │ Required ⭐ │
│         │           │ Prof    │             │
└─────────┴───────────┴─────────┴─────────────┘
```

### 🔐 권한 매트릭스

| **사용자 유형** | **BOARD_CODE 0-2** | **BOARD_CODE 3** | **LEC_SERIAL 검증** |
|---------------|-------------------|------------------|-------------------|
| 👑 **관리자** | ✅ 모든 공지 작성 가능 | ✅ 모든 강의 공지 작성 | 강의 존재 여부만 확인 |
| 🎓 **교수** | ❌ 작성 불가 | ✅ 담당 강의만 작성 | LEC_PROF = USER_CODE |
| 📚 **학생** | ❌ 작성 불가 | ❌ 작성 불가 | N/A |

---

## 🛠️ 기술적 구현

### 📁 변경된 파일들

#### 1. **BoardTbl.java** (Entity)
```java
// 새로운 필드 추가
@Column(name = "LEC_SERIAL", length = 10, nullable = true)
private String lecSerial;
// 강의 별 전용 게시글 (BOARD_CODE = 3일 때 필수)

// 유효성 검증 업데이트
@Max(value = 3, message = "게시글 코드는 3 이하여야 합니다")
private Integer boardCode;
// 0:학사공지 / 1:행정공지 / 2:기타공지 / 3:강의공지
```

#### 2. **BoardManagementService.java** (비즈니스 로직)
```java
// 강의 공지 상수 정의
private static final Integer BOARD_CODE_LECTURE = 3;

// 권한 검증 로직
if (boardCode == 3) {
    // LEC_SERIAL 필수
    if (lecSerial == null || empty) throw "필수";
    
    // 교수: 담당 강의 확인 (USER_IDX = LEC_PROF로 매칭)
    if (isProfessor) {
        Integer userIdx = user.get().getUserIdx();
        List<LecTbl> lectures = findByLecProf(String.valueOf(userIdx));  // LEC_PROF는 USER_IDX 저장
        if (!lectures.contains(lecSerial)) throw "담당강의만";
    }
    
    // 관리자: 강의 존재 확인
    if (isAdmin) {
        if (!lectureExists(lecSerial)) throw "존재하지않음";
    }
} else {
    // BOARD_CODE 0-2: 관리자만
    if (!isAdmin) throw "관리자만";
}
```

### 🗃️ 데이터베이스 변경

```sql
-- 새 컬럼 추가
ALTER TABLE BOARD_TBL 
ADD COLUMN LEC_SERIAL VARCHAR(10) NULL 
COMMENT '강의 별 전용 게시글';

-- 기존 데이터 BOARD_CODE 의미 변경 (수동 업데이트 필요)
-- 0: 학교공지 → 학사공지
-- 1: 학사공지 → 행정공지  
-- 2: 학과공지 → 기타공지
-- 3: 교수공지 → 강의공지 (LEC_SERIAL 필수)
```

---

## 🔍 유효성 검증

### 📝 BOARD_CODE 검증

1. **범위 검증**: `@Min(0)` + `@Max(3)` → 0~3만 허용
2. **필수값 검증**: `@NotNull`로 null 값 방지
3. **비즈니스 로직**: Service 레이어에서 권한별 추가 검증

### 🎓 LEC_SERIAL 검증 (BOARD_CODE = 3)

```java
// 1. 필수값 검증
if (lecSerial == null || lecSerial.trim().isEmpty()) {
    throw new IllegalArgumentException("강의 공지 작성 시 강의 코드(LEC_SERIAL)는 필수입니다.");
}

// 2. 교수 권한 검증
if (isProfessor) {
    Integer userIdx = user.get().getUserIdx();  // USER_IDX (LEC_PROF와 매칭)
    List<LecTbl> professorLectures = lecTblRepository.findByLecProf(String.valueOf(userIdx));
    List<String> validLecSerials = professorLectures.stream()
        .map(LecTbl::getLecSerial).collect(Collectors.toList());
    
    if (!validLecSerials.contains(lecSerial)) {
        throw new IllegalArgumentException(
            "본인이 담당하는 강의에만 공지를 작성할 수 있습니다. (강의코드: " + lecSerial + ")"
        );
    }
}

// 3. 관리자 권한 검증
if (isAdmin) {
    Optional<LecTbl> lecture = lecTblRepository.findByLecSerial(lecSerial);
    if (!lecture.isPresent()) {
        throw new IllegalArgumentException("존재하지 않는 강의입니다. (강의코드: " + lecSerial + ")");
    }
}
```

---

## 🧪 테스트 파일 가이드

### 📂 테스트 파일 구조

```
docs/게시판/
├── test-1-create.js        ✅ 게시글 생성 테스트
├── test-2-read.js          ✅ 게시글 조회 테스트  
├── test-3-update-delete.js ✅ 게시글 수정/삭제 테스트
└── README.md              📖 이 문서
```

### 🔧 test-1-create.js 주요 기능

```javascript
// BOARD_CODE 정보 객체
const BOARD_CODES = {
    0: { name: '학사공지', requiresAuth: 'admin', requiresLecSerial: false },
    1: { name: '행정공지', requiresAuth: 'admin', requiresLecSerial: false },
    2: { name: '기타공지', requiresAuth: 'admin', requiresLecSerial: false },
    3: { name: '강의공지', requiresAuth: 'admin-or-professor', requiresLecSerial: true }
};

// 주요 함수들
await createAcademicNotice()     // BOARD_CODE 0 (관리자만)
await createAdminNotice()        // BOARD_CODE 1 (관리자만)  
await createOtherNotice()        // BOARD_CODE 2 (관리자만)
await createLectureNotice('ETH201') // BOARD_CODE 3 (관리자+교수, lecSerial 필수)
await createAllTypes()           // 모든 유형 배치 생성
```

### 🔍 test-2-read.js 주요 기능

```javascript
// 조회 함수들
await getBoardDetail(1)          // 특정 게시글 상세 조회
await getBoardList()             // 전체 목록 (페이징)
await getBoardListByCode(3)      // 유형별 조회 (강의공지)
await getLectureNotices('ETH201') // 특정 강의 공지만
await quickViewAll()             // 각 유형별 최신 5개
```

### ✏️ test-3-update-delete.js 주요 기능

```javascript
// 수정/삭제 함수들
await updateBoard(1)             // 대화형 수정
await deleteBoard(1)             // 안전한 삭제 (확인 포함)
await quickUpdateTitle(1, "새제목") // 빠른 제목 수정
await updateLecSerial(1, "ETH202")  // 강의 코드 변경 (강의공지만)
```

---

## ⚠️ 에러 시나리오

### 🚫 권한 관련 에러

| **시나리오** | **에러 메시지** | **해결 방법** |
|-------------|----------------|---------------|
| 학생이 게시글 작성 시도 | "학사/행정/기타 공지는 관리자만 작성할 수 있습니다." | 관리자 계정으로 로그인 |
| 교수가 BOARD_CODE 0-2 시도 | "학사/행정/기타 공지는 관리자만 작성할 수 있습니다." | 관리자 계정 또는 BOARD_CODE 3 사용 |
| 교수가 타 강의 공지 시도 | "본인이 담당하는 강의에만 공지를 작성할 수 있습니다." | 담당 강의 코드로 변경 |

### 📝 데이터 관련 에러

| **시나리오** | **에러 메시지** | **해결 방법** |
|-------------|----------------|---------------|
| BOARD_CODE 4 입력 | "게시글 코드는 3 이하여야 합니다" | 0~3 범위 내 값 사용 |
| BOARD_CODE 3에 lecSerial 누락 | "강의 공지 작성 시 강의 코드(LEC_SERIAL)는 필수입니다." | lecSerial 필드 추가 |
| 존재하지 않는 강의 코드 | "존재하지 않는 강의입니다." | 유효한 강의 코드 사용 |

---

## 🚀 API 사용 예제

### 📤 게시글 생성 요청

#### 학사공지 (관리자만)
```json
POST /api/boards/create
Authorization: Bearer {adminToken}
{
    "boardCode": 0,
    "boardTitle": "2025년 1학기 수강신청 안내",
    "boardContent": "수강신청 기간은..."
}
```

#### 강의공지 (관리자 + 교수)
```json
POST /api/boards/create  
Authorization: Bearer {professorToken}
{
    "boardCode": 3,
    "lecSerial": "ETH201",
    "boardTitle": "중간고사 일정 변경",
    "boardContent": "중간고사 일정이 다음과 같이 변경됩니다..."
}
```

### 📥 성공 응답 예제

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

## 📈 마이그레이션 체크리스트

### ✅ 백엔드 작업
- [x] `BoardTbl.java` - lecSerial 필드 추가
- [x] `BoardManagementService.java` - 권한 검증 로직 구현
- [x] `BoardRepository.java` - 주석 업데이트
- [x] `LecTblRepository.findByLecProf()` - 교수 담당 강의 조회

### ✅ 데이터베이스 작업
- [x] `ALTER TABLE BOARD_TBL ADD COLUMN LEC_SERIAL`
- [ ] 기존 BOARD_CODE 데이터 의미 변경 (수동 업데이트)

### ✅ 테스트 작업
- [x] `test-1-create.js` - 생성 테스트 (lecSerial 검증 포함)
- [x] `test-2-read.js` - 조회 테스트 (강의별 필터링)
- [x] `test-3-update-delete.js` - 수정/삭제 (권한 검증)
- [x] 통합 테스트 문서 작성

### 🔄 운영 작업 (TODO)
- [ ] 기존 게시글 BOARD_CODE 의미 업데이트 공지
- [ ] 교수 계정 강의 배정 확인
- [ ] 프론트엔드 UI 업데이트 (BOARD_CODE 표시명)
- [ ] 사용자 매뉴얼 업데이트

---

## 🔧 개발자 참고사항

### 🏷️ 코드 컨벤션

```java
// BOARD_CODE 상수 정의
public static final Integer BOARD_CODE_ACADEMIC = 0;  // 학사공지
public static final Integer BOARD_CODE_ADMIN = 1;     // 행정공지  
public static final Integer BOARD_CODE_OTHER = 2;     // 기타공지
public static final Integer BOARD_CODE_LECTURE = 3;   // 강의공지
```

### 🔍 로깅 패턴

```java
// 권한 검증 로깅
logger.info("권한 확인 완료 - 관리자: {}, 교수: {} (이메일: {})", 
           isAdmin, isProfessor, currentUserEmail);

// 강의 검증 로깅
logger.info("교수 담당 강의 검증 완료 - 교수: {}, 강의: {}", 
           userCode, lecSerial);
```

### 🧪 테스트 패턴

```javascript
// 테스트 함수 명명 규칙
createAcademicNotice()    // BOARD_CODE별 생성 함수
getBoardListByCode(3)     // 유형별 조회 함수
updateLecSerial(1, "ETH202") // 특정 필드 수정 함수
```

---

## 📞 문의 및 지원

### 🐛 버그 리포트
- **GitHub Issues**: [blue-crab-lms/issues](https://github.com/hooting76/blue-crab-lms/issues)
- **이메일**: dev-team@bluecrab.com

### 📚 추가 문서
- [백엔드 API 명세서](../api-endpoints/)
- [데이터베이스 스키마](../database/)
- [프론트엔드 연동 가이드](../../프론트엔드_완전_연동_가이드.md)

---

## 📋 변경 이력

| **버전** | **날짜** | **변경사항** | **작성자** |
|---------|----------|-------------|-----------|
| 1.0 | 2025-10-21 | 초기 문서 작성, BOARD_CODE 체계 변경 완료 | GitHub Copilot |

---

> 💡 **참고**: 이 문서는 BlueCrab LMS 게시판 시스템의 BOARD_CODE 업데이트를 위한 종합 가이드입니다. 
> 개발, 테스트, 운영 시 이 문서를 참조하여 일관성을 유지해주세요.

**마지막 업데이트**: 2025년 10월 21일