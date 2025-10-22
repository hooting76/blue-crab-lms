# 📘 BlueCrab LMS – 수강신청 모듈 백엔드 연동 요청서  
**작성자:** 신아름  
**대상:** 백엔드 담당자  
**파일 기준:** `component/common/Course/CourseApply.jsx`  
**목표:** 더미 제거 후 실제 서버 API 연동 (모든 통신은 `POST` 기반)

---

## 0️⃣ 개요

현재 프론트엔드에서는 수강신청 모듈의 전체 UI가 완성되어 있습니다.  
다만, 일부 영역은 아직 **더미 데이터**로 표시 중이며, 실제 서버와 연동하기 위한 백엔드 API 구현이 필요합니다.

백엔드 담당자분께서는 아래 항목들만 구현해주시면 됩니다.  
(`enroll`, `list`, `checkEnrollment` 등은 이미 정상 동작 중이므로 제외)

---

## 1️⃣ 개설 과목 목록 조회 + 검색/필터 + 페이지네이션 (메인 테이블)

### 🎯 목적
현재 프론트는 `/lectures/eligible`을 호출하여 과목 전체를 가져온 뒤  
프론트 단에서 간단히 필터를 적용하고 있습니다.  
→ 이를 서버에서 직접 처리하도록 API 확장이 필요합니다.

### ✅ 엔드포인트
`POST /lectures/eligible`

### ✅ 요청 바디
```json
{
  "studentId": 15,
  "page": 0,
  "size": 10,
  "year": 2025,
  "term": 2,
  "majorType": "ALL",
  "facultyCode": 0,
  "departmentCode": 0,
  "name": "자료구조"
}
```

### ✅ 응답 예시
```json
{
  "eligibleLectures": [
    {
      "lecSerial": "CS101",
      "lecTit": "자료구조",
      "lecProfName": "김교수",
      "lecPoint": 3,
      "lecTime": "월1수1",
      "lecCurrent": 25,
      "lecMany": 30,
      "lecMcodeName": "공학부",
      "lecMcodeDepName": "컴퓨터공학",
      "isEligible": true
    }
  ],
  "pagination": { "currentPage": 0, "pageSize": 10, "totalElements": 73, "totalPages": 8 }
}
```

### ✅ 동작 요구사항
- 서버에서 직접 필터(year/term/majorType/facultyCode/departmentCode/name) 처리  
- `size=10` 단위 페이지네이션 지원  
- 응답 내 `pagination.totalPages` 값으로 프론트 페이지네이션 표시  
- 필드 이름(`lecMcodeName`, `lecMcodeDepName`) 포함 권장  

### ✅ 접근 권한
- `ROLE_STUDENT` 전용  
- 교수/관리자는 **403 Forbidden**

---

## 2️⃣ 수강신청 취소 (신규 추가)

### 🎯 목적
프론트 UI에는 “취소” 버튼이 구현되어 있으나, 현재는 안내문만 표시됩니다.  
→ 실제 취소 처리를 위한 API가 필요합니다.

### ✅ 엔드포인트
`POST /enrollments/cancel`

### ✅ 요청 바디
```json
{ "studentIdx": 15, "lecSerial": "CS101" }
```

### ✅ 응답 예시
```json
{ "success": true, "message": "취소되었습니다." }
```

### ✅ 접근 권한
- `ROLE_STUDENT` 전용  
- 교수/관리자는 **403 Forbidden**

---

## 3️⃣ 안내문 (상단 카드) – POST 기반 조회/저장

### 🎯 목적
현재 수강신청 화면의 상단에는 안내문(공지)이 표시됩니다.  
이 부분은 지금은 프론트에서 하드코딩되어 있으나,  
향후 **교수 또는 관리자(Admin)** 가 수정 가능한 형태로 서버에 저장되어야 합니다.

### ✅ 전체 구조 요약

| 구분 | 역할 | 접근 권한 | 프론트 사용 |
|------|------|------------|--------------|
| `/notice/course-apply/view` | 안내문 조회 | permitAll (또는 로그인 사용자) | 학생용 화면에서 표시 |
| `/notice/course-apply/save` | 안내문 저장 | ROLE_ADMIN, ROLE_PROFESSOR | 관리자/교수 전용 페이지에서 사용 |

### ✅ 1. 조회 (학생용)
`POST /notice/course-apply/view`

#### 요청 바디
```json
{ "includeMeta": true }
```

#### 응답 예시
```json
{
  "message": "자세한 신청 관련 공지는 학사 공지사항을 확인해 주세요.",
  "updatedAt": "2025-10-21T10:00:00Z",
  "updatedBy": { "userId": 991, "userName": "관리자A", "role": "ADMIN" }
}
```

### ✅ 2. 저장 (관리자/교수 전용)
`POST /notice/course-apply/save`

#### 요청 바디
```json
{ "message": "올해 2학기 수강신청은 8/28(목) 10:00 오픈 예정입니다." }
```

#### 응답 예시
```json
{
  "success": true,
  "message": "저장되었습니다.",
  "notice": {
    "message": "올해 2학기 수강신청은 8/28(목) 10:00 오픈 예정입니다.",
    "updatedAt": "2025-10-22T09:10:00Z",
    "updatedBy": { "userId": 22, "userName": "홍교수", "role": "PROFESSOR" }
  }
}
```

### ✅ “어드민 화면에서 사용”의 의미

- **관리자/교수 전용 페이지(Admin Notice Editor)** 를 따로 두고,  
  거기서 안내문을 입력·수정 후 저장(`/save`)합니다.  
- 학생 수강신청 화면에서는 `/view`만 호출하여 표시합니다.

#### 예시 플로우
1. 관리자/교수 → `/admin/course-apply-notice` 페이지 접근  
2. 텍스트 입력 후 “저장” 클릭 → `POST /notice/course-apply/save` 호출  
3. 학생 페이지(`/course/apply`)에서 `/view`로 내용 표시  

### ✅ 접근 권한
| 구분 | 권한 | 설명 |
|------|------|------|
| `/view` | permitAll (또는 로그인 사용자) | 누구나 열람 가능 |
| `/save` | ROLE_ADMIN, ROLE_PROFESSOR | 관리자/교수만 수정 가능 |
| 실패 시 | 403 Forbidden | 권한 없음 응답 |

---

## 4️⃣ 응답 형식 통일 (권장)

```json
{ "success": false, "message": "에러 사유", "timestamp": "ISO-8601" }
```

---

## 5️⃣ 체크리스트

| 항목 | 설명 | 구현 상태 |
|------|------|------------|
| ✅ `/lectures/eligible` | 필터 + 페이지네이션 서버 처리 | 필요 |
| ✅ `/enrollments/cancel` | 수강신청 취소 API 신설 | 필요 |
| ✅ `/notice/course-apply/view` | 안내문 조회 | 필요 |
| ✅ `/notice/course-apply/save` | 안내문 저장 (관리자/교수 전용) | 필요 |
| ⚙️ 응답 표준화 | `{ success:false, message }` 통일 | 권장 |
| ⚙️ 이름 필드 추가 | `lecMcodeName`, `lecMcodeDepName` | 권장 |

---

## 📩 요약

| 구분 | 경로 | 메서드 | 역할 | 권한 |
|------|------|--------|------|------|
| 개설과목 조회 | `/lectures/eligible` | POST | 검색/필터/페이징 지원 | STUDENT |
| 수강신청 취소 | `/enrollments/cancel` | POST | 수강취소 처리 | STUDENT |
| 안내문 조회 | `/notice/course-apply/view` | POST | 안내문 조회 | permitAll |
| 안내문 저장 | `/notice/course-apply/save` | POST | 안내문 수정 | ADMIN, PROFESSOR |

---

📘 **정리**  
- 모든 요청은 `POST` 방식  
- 모든 응답은 `{ success, message, ... }` 형식  
- 안내문은 **학생은 조회만**, **관리자/교수는 저장 가능**  
- `/eligible`은 서버 측 필터/페이지네이션 필요  
- `/cancel` API 신규 필요  
