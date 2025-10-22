# 📢 수강신청 안내문 API 명세서

새로 구현할 Notice System 설계 문서

---

## 🎯 개요

수강신청 기간과 관련된 공지사항을 관리하는 시스템입니다.

**특징**:
- ✅ 단일 레코드 관리 (항상 최신 안내문 1개만 유지)
- ✅ 공개 조회 (로그인 불필요)
- ✅ 관리자/교수만 수정 가능
- ✅ 간단한 구조 (1개 테이블만 사용)

---

## 📊 데이터베이스 설계

### COURSE_APPLY_NOTICE 테이블

```sql
CREATE TABLE COURSE_APPLY_NOTICE (
    NOTICE_IDX INT PRIMARY KEY AUTO_INCREMENT COMMENT '안내문 ID (PK)',
    MESSAGE TEXT NOT NULL COMMENT '안내 메시지',
    UPDATED_AT DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '수정 시각',
    UPDATED_BY VARCHAR(100) COMMENT '수정자 (admin/professor)',
    CREATED_AT DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '생성 시각'
) COMMENT='수강신청 안내문 (단일 레코드)';
```

**초기 데이터**:
```sql
INSERT INTO COURSE_APPLY_NOTICE (MESSAGE, UPDATED_BY) VALUES 
('수강신청 기간: 2025-02-01 ~ 2025-02-15', 'admin');
```

---

## 🔌 API 명세

### 1️⃣ 안내문 조회 (공개)

#### POST /notice/course-apply/view

**설명**: 현재 안내문을 조회합니다. (로그인 불필요)

**권한**: `permitAll` (공개)

**Request**:
```http
POST /notice/course-apply/view
Content-Type: application/json

{}
```

**Response (200 OK)**:
```json
{
  "success": true,
  "message": "수강신청 기간: 2025-02-01 ~ 2025-02-15",
  "updatedAt": "2025-01-20T14:30:00Z",
  "updatedBy": "admin"
}
```

**Response (404 Not Found)**:
```json
{
  "success": false,
  "message": "안내문이 없습니다.",
  "timestamp": "2025-10-22T10:00:00Z"
}
```

---

### 2️⃣ 안내문 저장 (관리자/교수)

#### POST /notice/course-apply/save

**설명**: 안내문을 작성하거나 수정합니다.

**권한**: `ROLE_ADMIN`, `ROLE_PROFESSOR`

**Headers**:
```http
Authorization: Bearer {JWT_TOKEN}
Content-Type: application/json
```

**Request**:
```json
{
  "message": "수강신청 기간: 2025-03-01 ~ 2025-03-15\n\n주의사항: 선착순입니다."
}
```

**Response (200 OK)**:
```json
{
  "success": true,
  "message": "안내문이 저장되었습니다.",
  "data": {
    "noticeIdx": 1,
    "message": "수강신청 기간: 2025-03-01 ~ 2025-03-15\n\n주의사항: 선착순입니다.",
    "updatedAt": "2025-10-22T15:00:00Z",
    "updatedBy": "professor_kim"
  }
}
```

**Response (403 Forbidden)**:
```json
{
  "success": false,
  "message": "권한이 없습니다.",
  "timestamp": "2025-10-22T10:00:00Z"
}
```

---

## 💻 백엔드 구현 (Java Spring Boot)

### 1. Entity

```java
package com.bluecrab.lms.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "COURSE_APPLY_NOTICE")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CourseApplyNotice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "NOTICE_IDX")
    private Integer noticeIdx;

    @Column(name = "MESSAGE", columnDefinition = "TEXT", nullable = false)
    private String message;

    @UpdateTimestamp
    @Column(name = "UPDATED_AT", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "UPDATED_BY", length = 100)
    private String updatedBy;

    @Column(name = "CREATED_AT", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
}
```

---

### 2. Repository

```java
package com.bluecrab.lms.repository;

import com.bluecrab.lms.entity.CourseApplyNotice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CourseApplyNoticeRepository extends JpaRepository<CourseApplyNotice, Integer> {
    
    // 가장 최근 안내문 1개만 조회
    Optional<CourseApplyNotice> findTopByOrderByUpdatedAtDesc();
}
```

---

### 3. DTO

```java
package com.bluecrab.lms.dto;

import lombok.*;

import java.time.LocalDateTime;

// 조회용 응답 DTO
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NoticeViewResponse {
    private Boolean success;
    private String message;
    private LocalDateTime updatedAt;
    private String updatedBy;
}

// 저장용 요청 DTO
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class NoticeSaveRequest {
    private String message;
}

// 저장용 응답 DTO
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NoticeSaveResponse {
    private Boolean success;
    private String message;
    private NoticeData data;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class NoticeData {
        private Integer noticeIdx;
        private String message;
        private LocalDateTime updatedAt;
        private String updatedBy;
    }
}
```

---

### 4. Controller

```java
package com.bluecrab.lms.controller;

import com.bluecrab.lms.dto.*;
import com.bluecrab.lms.entity.CourseApplyNotice;
import com.bluecrab.lms.repository.CourseApplyNoticeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/notice/course-apply")
@RequiredArgsConstructor
public class NoticeController {

    private final CourseApplyNoticeRepository noticeRepository;

    /**
     * 안내문 조회 (공개)
     */
    @PostMapping("/view")
    public ResponseEntity<NoticeViewResponse> viewNotice() {
        CourseApplyNotice notice = noticeRepository
                .findTopByOrderByUpdatedAtDesc()
                .orElse(null);

        if (notice == null) {
            return ResponseEntity.status(404).body(
                NoticeViewResponse.builder()
                    .success(false)
                    .message("안내문이 없습니다.")
                    .build()
            );
        }

        return ResponseEntity.ok(
            NoticeViewResponse.builder()
                .success(true)
                .message(notice.getMessage())
                .updatedAt(notice.getUpdatedAt())
                .updatedBy(notice.getUpdatedBy())
                .build()
        );
    }

    /**
     * 안내문 저장 (관리자/교수)
     */
    @PostMapping("/save")
    @PreAuthorize("hasAnyRole('ADMIN', 'PROFESSOR')")
    public ResponseEntity<NoticeSaveResponse> saveNotice(
            @RequestBody NoticeSaveRequest request,
            Authentication authentication) {
        
        String username = authentication.getName();

        // 기존 안내문 찾기 (있으면 수정, 없으면 신규)
        CourseApplyNotice notice = noticeRepository
                .findTopByOrderByUpdatedAtDesc()
                .orElse(new CourseApplyNotice());

        notice.setMessage(request.getMessage());
        notice.setUpdatedBy(username);

        CourseApplyNotice saved = noticeRepository.save(notice);

        return ResponseEntity.ok(
            NoticeSaveResponse.builder()
                .success(true)
                .message("안내문이 저장되었습니다.")
                .data(NoticeSaveResponse.NoticeData.builder()
                    .noticeIdx(saved.getNoticeIdx())
                    .message(saved.getMessage())
                    .updatedAt(saved.getUpdatedAt())
                    .updatedBy(saved.getUpdatedBy())
                    .build())
                .build()
        );
    }
}
```

---

### 5. Security Configuration

```java
// SecurityConfig.java에 추가
http
    .authorizeHttpRequests(auth -> auth
        .requestMatchers("/notice/course-apply/view").permitAll()  // 공개
        .requestMatchers("/notice/course-apply/save").hasAnyRole("ADMIN", "PROFESSOR")  // 제한
        // ... 기타 설정
    );
```

---

## 🧪 테스트 코드 (브라우저 콘솔)

### 학생/비로그인 사용자 테스트

```javascript
// 안내문 조회 (로그인 불필요)
async function viewNotice() {
  const response = await fetch('/notice/course-apply/view', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json'
    },
    body: JSON.stringify({})
  });
  
  const data = await response.json();
  console.log('안내문:', data);
  return data;
}

// 실행
await viewNotice();
```

---

### 관리자/교수 테스트

```javascript
// 로그인 (관리자/교수)
async function loginAsAdmin() {
  const response = await fetch('/login/action', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({
      username: 'admin',
      password: 'admin123'
    })
  });
  
  const data = await response.json();
  localStorage.setItem('authToken', data.token);
  console.log('로그인 완료');
}

// 안내문 저장
async function saveNotice(message) {
  const token = localStorage.getItem('authToken');
  
  const response = await fetch('/notice/course-apply/save', {
    method: 'POST',
    headers: {
      'Authorization': `Bearer ${token}`,
      'Content-Type': 'application/json'
    },
    body: JSON.stringify({ message })
  });
  
  const data = await response.json();
  console.log('저장 결과:', data);
  return data;
}

// 실행
await loginAsAdmin();
await saveNotice('수강신청 기간: 2025-03-01 ~ 2025-03-15\n\n주의: 선착순입니다.');
```

---

## 🎨 프론트엔드 통합 예시

### React 컴포넌트

```jsx
import React, { useState, useEffect } from 'react';

function CourseApplyNotice() {
  const [notice, setNotice] = useState('');
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    loadNotice();
  }, []);

  const loadNotice = async () => {
    try {
      const response = await fetch('/notice/course-apply/view', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({})
      });
      
      const data = await response.json();
      if (data.success) {
        setNotice(data.message);
      }
    } catch (error) {
      console.error('안내문 로드 실패:', error);
    } finally {
      setLoading(false);
    }
  };

  if (loading) return <div>로딩 중...</div>;

  return (
    <div className="notice-box">
      <h2>📢 수강신청 안내</h2>
      <pre>{notice || '안내문이 없습니다.'}</pre>
    </div>
  );
}

export default CourseApplyNotice;
```

---

### 관리자 편집 컴포넌트

```jsx
import React, { useState } from 'react';

function NoticeEditor() {
  const [message, setMessage] = useState('');
  const token = localStorage.getItem('authToken');

  const handleSave = async () => {
    try {
      const response = await fetch('/notice/course-apply/save', {
        method: 'POST',
        headers: {
          'Authorization': `Bearer ${token}`,
          'Content-Type': 'application/json'
        },
        body: JSON.stringify({ message })
      });

      const data = await response.json();
      if (data.success) {
        alert('안내문이 저장되었습니다.');
      }
    } catch (error) {
      console.error('저장 실패:', error);
      alert('저장 중 오류가 발생했습니다.');
    }
  };

  return (
    <div>
      <h2>안내문 편집</h2>
      <textarea
        value={message}
        onChange={(e) => setMessage(e.target.value)}
        rows={10}
        cols={60}
        placeholder="수강신청 안내문을 입력하세요..."
      />
      <button onClick={handleSave}>저장</button>
    </div>
  );
}

export default NoticeEditor;
```

---

## 📅 구현 일정

### Phase 1: 데이터베이스 (30분)
- ✅ COURSE_APPLY_NOTICE 테이블 생성
- ✅ 초기 데이터 INSERT

### Phase 2: 백엔드 (1.5시간)
- ✅ Entity 작성
- ✅ Repository 작성
- ✅ DTO 작성
- ✅ Controller 작성
- ✅ Security 설정

### Phase 3: 테스트 (1시간)
- ✅ 브라우저 콘솔 테스트
- ✅ 권한 검증 테스트
- ✅ CRUD 동작 확인

**총 예상 시간**: 3시간  
**완료 예정**: 오늘(10/22) 중

---

## ⚠️ 주의사항

### 1. 단일 레코드 관리

항상 최신 안내문 1개만 유지:
```java
findTopByOrderByUpdatedAtDesc()  // 가장 최근 1개만
```

### 2. 권한 분리

- `/view`: 누구나 조회 가능 (permitAll)
- `/save`: 관리자/교수만 가능 (ROLE_ADMIN, ROLE_PROFESSOR)

### 3. 멀티라인 지원

안내문은 여러 줄 입력 가능:
```
수강신청 기간: 2025-03-01 ~ 2025-03-15

주의사항:
- 선착순입니다
- 정원 초과 시 대기
```

---

## 🔗 관련 문서

- [00-요청검토결과.md](./00-요청검토결과.md)
- [가이드문서/01-API-SPECIFICATION.md](../../가이드문서/01-API-SPECIFICATION.md)

---

## 📞 추가 문의

구현 중 궁금한 사항이나 요구사항 변경이 있으시면 언제든 말씀해주세요!

---

> **새로 구현 필요**: 이 기능은 아직 구현되지 않았습니다. (예상 시간: 3시간)
