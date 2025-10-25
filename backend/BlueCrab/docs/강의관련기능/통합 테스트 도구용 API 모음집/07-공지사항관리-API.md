# 📝 공지사항 관리 API 명세서

> **Base URL**: `/notice/course-apply`
> 
> **작성일**: 2025-10-25
> 
> **컨트롤러**: `NoticeController.java`

---

## 📋 목차
1. [안내문 조회 (공개)](#1-안내문-조회-공개)
2. [안내문 저장 (관리자/교수)](#2-안내문-저장-관리자교수)

---

## 1. 안내문 조회 (공개)

### `POST /notice/course-apply/view`

수강신청 안내문을 조회합니다. **로그인이 불필요한 공개 API**로, 누구나 현재 안내문을 확인할 수 있습니다.

#### Request Body
```json
{}
```
*빈 객체 또는 요청 본문 없이 호출 가능*

#### Response (성공)
```json
{
  "success": true,
  "message": "2025학년도 1학기 수강신청이 시작됩니다.\n\n- 신청 기간: 2025.02.15 09:00 ~ 2025.02.28 18:00\n- 신청 방법: 온라인 수강신청 시스템 이용\n- 문의사항: 학사지원처 (02-1234-5678)\n\n※ 수강신청 전 반드시 수강편람을 확인하시기 바랍니다.",
  "updatedAt": "2025-10-25T14:30:00",
  "updatedBy": "admin@bluecrab.ac.kr"
}
```

#### Response (안내문 없음)
```json
{
  "success": false,
  "message": "안내문이 없습니다."
}
```

#### Response (서버 오류)
```json
{
  "success": false,
  "message": "안내문 조회 중 오류가 발생했습니다."
}
```

#### HTTP Status Codes
- `200 OK`: 조회 성공
- `404 Not Found`: 안내문이 존재하지 않음
- `500 Internal Server Error`: 서버 오류

#### 특징
- **인증 불필요**: JWT 토큰 없이도 호출 가능
- **최신 안내문**: 가장 최근에 업데이트된 안내문 1개만 반환
- **공개 접근**: 누구나 접근 가능한 퍼블릭 API

#### 테스트 예제 (JavaScript)
```javascript
// 안내문 조회 (인증 불필요)
function getCourseApplyNotice() {
    return fetch(`${API_BASE_URL}/notice/course-apply/view`, {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json'
            // Authorization 헤더 불필요
        },
        body: JSON.stringify({})
    });
}

// 사용 예시
getCourseApplyNotice()
    .then(response => response.json())
    .then(data => {
        if (data.success) {
            console.log('수강신청 안내문:');
            console.log(data.message);
            console.log(`최종 수정: ${data.updatedAt} (${data.updatedBy})`);
        } else {
            console.log('안내문이 없습니다.');
        }
    })
    .catch(error => {
        console.error('조회 실패:', error);
    });

// HTML에 안내문 표시
function displayNotice() {
    getCourseApplyNotice()
        .then(response => response.json())
        .then(data => {
            const noticeElement = document.getElementById('notice-content');
            if (data.success) {
                noticeElement.innerHTML = `
                    <div class="notice-message">${data.message.replace(/\n/g, '<br>')}</div>
                    <div class="notice-meta">
                        최종 수정: ${new Date(data.updatedAt).toLocaleString()}
                        수정자: ${data.updatedBy}
                    </div>
                `;
            } else {
                noticeElement.innerHTML = '<p class="no-notice">현재 공지된 안내문이 없습니다.</p>';
            }
        });
}
```

---

## 2. 안내문 저장 (관리자/교수)

### `POST /notice/course-apply/save`

수강신청 안내문을 작성하거나 수정합니다. **관리자 또는 교수 권한이 필요**하며, 기존 안내문이 있으면 수정하고 없으면 새로 생성합니다.

#### Request Body
```json
{
  "message": "2025학년도 2학기 수강신청이 시작됩니다.\n\n- 신청 기간: 2025.08.15 09:00 ~ 2025.08.28 18:00\n- 신청 방법: 온라인 수강신청 시스템 이용\n- 변경사항: 교양선택 과목 증설\n- 문의사항: 학사지원처 (02-1234-5678)\n\n※ 수강신청 전 반드시 수강편람을 확인하시기 바랍니다."
}
```

#### Request Fields
- `message` (string, 필수): 안내문 내용
  - 줄바꿈은 `\n` 문자로 표현
  - HTML 태그 사용 불가 (플레인 텍스트)
  - 빈 문자열 또는 공백만 있는 경우 오류

#### Response (성공)
```json
{
  "success": true,
  "message": "안내문이 저장되었습니다.",
  "data": {
    "noticeIdx": 15,
    "message": "2025학년도 2학기 수강신청이 시작됩니다.\n\n- 신청 기간: 2025.08.15 09:00 ~ 2025.08.28 18:00\n- 신청 방법: 온라인 수강신청 시스템 이용\n- 변경사항: 교양선택 과목 증설\n- 문의사항: 학사지원처 (02-1234-5678)\n\n※ 수강신청 전 반드시 수강편람을 확인하시기 바랍니다.",
    "updatedAt": "2025-10-25T15:45:00",
    "updatedBy": "professor@bluecrab.ac.kr"
  }
}
```

#### Response (인증 실패)
```json
{
  "success": false,
  "message": "인증이 필요합니다."
}
```

#### Response (입력 오류)
```json
{
  "success": false,
  "message": "안내 메시지는 필수입니다."
}
```

#### Response (서버 오류)
```json
{
  "success": false,
  "message": "안내문 저장 중 오류가 발생했습니다."
}
```

#### HTTP Status Codes
- `200 OK`: 저장 성공
- `400 Bad Request`: 입력 데이터 오류
- `401 Unauthorized`: 인증 실패
- `500 Internal Server Error`: 서버 오류

#### 권한 요구사항
- **인증 필요**: 유효한 JWT 토큰 필요
- **권한 확인**: 관리자 또는 교수 권한 (자동 검증)
- **작성자 기록**: JWT에서 추출한 사용자 정보가 updatedBy에 기록

#### 비즈니스 로직
1. **기존 안내문 확인**: 최신 안내문이 있는지 조회
2. **수정 vs 신규**: 기존 안내문이 있으면 수정, 없으면 신규 생성
3. **메타데이터 자동 설정**: updatedAt(현재시간), updatedBy(인증된 사용자)
4. **단일 안내문**: 시스템에는 항상 최신 안내문 1개만 유지

#### 테스트 예제 (JavaScript)
```javascript
// 안내문 저장 (인증 필요)
function saveCourseApplyNotice(message) {
    return fetch(`${API_BASE_URL}/notice/course-apply/save`, {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json',
            'Authorization': `Bearer ${token}` // 관리자/교수 토큰 필요
        },
        body: JSON.stringify({
            message: message
        })
    });
}

// 사용 예시 - 새 안내문 작성
const noticeMessage = `2025학년도 2학기 수강신청 안내

수강신청 일정:
- 1차 신청: 2025.08.15 09:00 ~ 2025.08.22 18:00
- 2차 신청: 2025.08.26 09:00 ~ 2025.08.28 18:00

주요 변경사항:
- 교양선택 과목 20개 증설
- 전공선택 이수 학점 조정 (18학점 → 21학점)
- 새로운 융합전공 '데이터사이언스' 개설

문의처:
- 학사지원처: 02-1234-5678
- 이메일: academic@bluecrab.ac.kr

※ 수강신청 전 반드시 2025학년도 수강편람을 확인하시기 바랍니다.`;

saveCourseApplyNotice(noticeMessage)
    .then(response => response.json())
    .then(data => {
        if (data.success) {
            console.log('안내문 저장 완료');
            console.log(`공지 ID: ${data.data.noticeIdx}`);
            console.log(`작성자: ${data.data.updatedBy}`);
            
            // 저장 후 즉시 조회하여 확인
            getCourseApplyNotice().then(response => response.json()).then(result => {
                console.log('저장된 안내문 확인:', result.message);
            });
        } else {
            console.error('저장 실패:', data.message);
        }
    })
    .catch(error => {
        console.error('네트워크 오류:', error);
    });

// 관리자 페이지용 - 안내문 편집 폼
function setupNoticeEditor() {
    // 현재 안내문 불러오기
    getCourseApplyNotice()
        .then(response => response.json())
        .then(data => {
            const textarea = document.getElementById('notice-editor');
            if (data.success) {
                textarea.value = data.message;
                document.getElementById('last-updated').textContent = 
                    `최종 수정: ${new Date(data.updatedAt).toLocaleString()} (${data.updatedBy})`;
            } else {
                textarea.placeholder = '새 안내문을 작성하세요...';
            }
        });

    // 저장 버튼 이벤트
    document.getElementById('save-notice-btn').addEventListener('click', () => {
        const message = document.getElementById('notice-editor').value.trim();
        if (!message) {
            alert('안내문 내용을 입력해주세요.');
            return;
        }

        saveCourseApplyNotice(message)
            .then(response => response.json())
            .then(data => {
                if (data.success) {
                    alert('안내문이 저장되었습니다.');
                    location.reload(); // 페이지 새로고침으로 최신 정보 표시
                } else {
                    alert('저장 실패: ' + data.message);
                }
            });
    });
}
```

---

## 💡 중요 참고사항

### API 접근 권한
- **조회 API**: 누구나 접근 가능 (인증 불필요)
- **저장 API**: 관리자 또는 교수만 접근 가능 (JWT 인증 필요)

### 데이터 관리
- **단일 안내문**: 시스템에는 최신 안내문 1개만 존재
- **버전 관리**: 기존 안내문을 덮어쓰는 방식 (이력 관리 없음)
- **자동 메타데이터**: 수정 시간과 수정자는 자동으로 기록

### 텍스트 형식
- **플레인 텍스트**: HTML 태그 사용 불가
- **줄바꿈**: `\n` 문자로 표현
- **특수문자**: 모든 특수문자 허용 (이스케이프 처리 자동)

### 사용 시나리오
1. **홈페이지 공지**: 메인 페이지에서 수강신청 안내 표시
2. **관리자 작업**: 학기별 수강신청 일정 및 변경사항 공지
3. **실시간 업데이트**: 긴급 공지사항이나 일정 변경 시 즉시 반영

### 보안 고려사항
- **XSS 방지**: 플레인 텍스트만 허용하여 스크립트 주입 방지
- **권한 검증**: JWT 토큰 기반 인증으로 무단 수정 방지
- **입력 검증**: 빈 내용이나 너무 긴 텍스트 방지

### 연관 시스템
- **수강 관리**: 수강신청 페이지에서 안내문 표시
- **사용자 인터페이스**: 프론트엔드에서 자동으로 안내문 갱신
- **알림 시스템**: 안내문 변경 시 사용자 알림 (선택사항)