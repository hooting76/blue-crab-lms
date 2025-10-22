# 📢 안내문 API 구현 가이드

> 작성일: 2025-10-22  
> 상태: ✅ 구현 완료

---

## 📋 개요

수강신청 안내문 조회 및 관리 기능이 구현되었습니다.

### 주요 기능
- 안내문 조회 (공개)
- 안내문 저장/수정 (관리자/교수)
- 단일 레코드 관리 (최신 안내문만 유지)
- 자동 타임스탬프 관리

---

## 🔌 API 엔드포인트

### 1. 안내문 조회 (공개)

```http
POST /notice/course-apply/view
Content-Type: application/json
```

**응답 (성공)**
```json
{
    "success": true,
    "message": "2025학년도 1학기 수강신청 안내\n\n수강신청 기간: 2025년 2월 1일 ~ 2월 7일",
    "updatedAt": "2025-10-22T14:30:00",
    "updatedBy": "admin"
}
```

**응답 (안내문 없음)**
```json
{
    "success": false,
    "message": "안내문이 없습니다."
}
```

---

### 2. 안내문 저장 (인증 필요)

```http
POST /notice/course-apply/save
Content-Type: application/json
Authorization: Bearer {JWT_TOKEN}

{
    "message": "수강신청 안내 메시지"
}
```

**응답 (성공)**
```json
{
    "success": true,
    "message": "안내문이 저장되었습니다.",
    "data": {
        "noticeIdx": 1,
        "message": "수강신청 안내 메시지",
        "updatedAt": "2025-10-22T14:30:00",
        "updatedBy": "admin"
    }
}
```

**응답 (인증 실패)**
```json
{
    "success": false,
    "message": "인증이 필요합니다."
}
```

---

## 📦 구현된 파일

### Entity
- `entity/Lecture/CourseApplyNotice.java`
  - `@PrePersist`: 생성 시 자동 타임스탬프
  - `@PreUpdate`: 수정 시 자동 타임스탬프

### Repository
- `repository/Lecture/CourseApplyNoticeRepository.java`
  - `findTopByOrderByUpdatedAtDesc()`: 최신 안내문 조회

### DTO
- `dto/Lecture/NoticeViewResponse.java`: 조회 응답
- `dto/Lecture/NoticeSaveRequest.java`: 저장 요청
- `dto/Lecture/NoticeSaveResponse.java`: 저장 응답

### Controller
- `controller/Lecture/NoticeController.java`
  - POST `/notice/course-apply/view` (공개)
  - POST `/notice/course-apply/save` (인증 필요)

---

## 🗄️ 데이터베이스

### DDL 스크립트
위치: `docs/ddl/course_apply_notice.sql`

```sql
CREATE TABLE IF NOT EXISTS COURSE_APPLY_NOTICE (
    notice_idx INT AUTO_INCREMENT PRIMARY KEY,
    message TEXT NOT NULL,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    updated_by VARCHAR(50) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE INDEX idx_updated_at ON COURSE_APPLY_NOTICE (updated_at DESC);
```

### 초기 데이터
```sql
INSERT INTO COURSE_APPLY_NOTICE (message, updated_by) 
VALUES (
    '2025학년도 1학기 수강신청 안내\n\n수강신청 기간: 2025년 2월 1일 ~ 2월 7일\n최대 신청학점: 21학점\n\n문의사항은 학사지원팀으로 연락주시기 바랍니다.',
    'system'
);
```

---

## 🔒 보안 설정

`config/SecurityConfig.java`에 다음 설정이 추가되었습니다:

```java
.requestMatchers("/notice/course-apply/view").permitAll()
```

- `/view`: 누구나 접근 가능 (인증 불필요)
- `/save`: JWT 인증 필요 (ROLE_ADMIN, ROLE_PROFESSOR)

---

## 🧪 테스트 방법

### 브라우저 콘솔 테스트

#### 1. 안내문 조회
```javascript
fetch('http://localhost:8090/notice/course-apply/view', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' }
})
.then(res => res.json())
.then(data => console.log('조회:', data));
```

#### 2. 안내문 저장 (JWT 토큰 필요)
```javascript
const token = 'YOUR_JWT_TOKEN';

fetch('http://localhost:8090/notice/course-apply/save', {
    method: 'POST',
    headers: {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${token}`
    },
    body: JSON.stringify({
        message: '수강신청 안내 메시지'
    })
})
.then(res => res.json())
.then(data => console.log('저장:', data));
```

---

## 🚀 배포 체크리스트

- [ ] 데이터베이스 테이블 생성 (`course_apply_notice.sql` 실행)
- [ ] Spring Boot 애플리케이션 재시작
- [ ] 안내문 조회 API 테스트
- [ ] 안내문 저장 API 테스트 (JWT 토큰 사용)
- [ ] 권한 없는 사용자 접근 차단 확인

---

## 📝 주의사항

### Upsert 패턴
- 기존 안내문이 있으면 **수정**
- 기존 안내문이 없으면 **새로 생성**
- 항상 최신 안내문 1개만 유지

### 타임스탬프 관리
- `createdAt`: 최초 생성 시 자동 설정 (`@PrePersist`)
- `updatedAt`: 수정 시 자동 갱신 (`@PreUpdate`)

### 권한 체크
- `/save` 엔드포인트는 JWT 토큰 필수
- `Authentication` 객체에서 username 자동 추출
- `updatedBy` 필드에 자동 저장

---

## 💡 향후 개선 사항

1. **권한 제어 강화**
   - Controller에 `@PreAuthorize` 추가
   - 역할 기반 접근 제어 명시화

2. **입력 검증**
   - `@Valid` + `@NotBlank` 어노테이션
   - 메시지 길이 제한

3. **캐싱**
   - `@Cacheable`로 조회 성능 향상

4. **이력 관리**
   - 안내문 수정 이력 별도 저장
