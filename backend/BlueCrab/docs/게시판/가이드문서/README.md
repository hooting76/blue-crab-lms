# 게시판 API 문서 허브 v2.0

## 📋 개요

Blue Crab LMS 게시판 시스템의 종합 문서 허브입니다.
실제 백엔드 구현에 맞춰 작성된 최신 문서들을 제공합니다.

### 🎯 핵심 특징
- **JWT 기반 인증** - Bearer 토큰 방식
- **RESTful API** - JSON 기반 통신
- **분리된 첨부파일 관리** - 3단계 워크플로우
- **성능 최적화** - 페이징 및 선택적 조회
- **권한 기반 접근제어** - 사용자 유형별 권한 관리

## 🗂️ 문서 구조

### 📚 API 가이드 (api-guides/)
상세한 API 사용법과 예제를 제공합니다.

| 문서 | 내용 | 대상 |
|------|------|------|
| [**게시글 생성 가이드**](./api-guides/board-creation-guide.md) | 게시글 작성 API 완전 가이드 | 프론트엔드 개발자 |
| [**게시글 조회 가이드**](./api-guides/board-query-guide.md) | 목록/상세 조회 API 가이드 | 프론트엔드 개발자 |
| [**첨부파일 관리 가이드**](./api-guides/attachment-guide.md) | 파일 업로드/다운로드 가이드 | 프론트엔드 개발자 |

### 🔧 구현 정보 (implementation/)
백엔드 구현 세부사항과 아키텍처 정보를 제공합니다.

| 문서 | 내용 | 대상 |
|------|------|------|
| [**파일 구조 및 클래스**](./implementation/file-structure.md) | 사용된 모든 클래스 파일 목록과 역할 | 백엔드 개발자 |

### 💡 사용 예제 (examples/)
실제 환경에서 사용할 수 있는 완전한 예제 코드를 제공합니다.

| 문서 | 내용 | 대상 |
|------|------|------|
| [**실제 사용 예제**](./examples/usage-examples.md) | JavaScript, PowerShell, Python 예제 | 모든 개발자 |

## 🚀 빠른 시작

### 1. 게시글 생성
```javascript
// 기본 게시글 생성
const response = await fetch('/api/boards/create', {
  method: 'POST',
  headers: {
    'Content-Type': 'application/json',
    'Authorization': 'Bearer YOUR_JWT_TOKEN'
  },
  body: JSON.stringify({
    boardTitle: "제목",
    boardContent: "내용",
    boardCode: 0
  })
});
```

### 2. 게시글 목록 조회
```javascript
// 공지사항 목록 조회
const response = await fetch('/api/boards/list', {
  method: 'POST',
  headers: { 'Content-Type': 'application/json' },
  body: JSON.stringify({ boardCode: 0 })
});
```

### 3. 게시글 상세 조회
```javascript
// 특정 게시글 상세 조회
const response = await fetch('/api/boards/detail', {
  method: 'POST',
  headers: { 'Content-Type': 'application/json' },
  body: JSON.stringify({ boardIdx: 123 })
});
```

## 📊 게시판 코드 체계

| 코드 | 명칭 | 용도 | 생성 권한 |
|------|------|------|----------|
| 0 | 공지사항 | 학원 전체 공지 | 관리자 |
| 1 | 학사 정보 | 입학/학사일정 | 관리자 |
| 2 | 시설 정보 | 시설 안내/공지 | 관리자 |
| 3 | 강의 공지 | 강의별 공지 | 강사, 관리자 |

## 🔐 인증 및 권한

### JWT 토큰 사용
모든 쓰기 작업(생성, 수정, 삭제)에는 JWT 액세스 토큰이 필요합니다.

```http
Authorization: Bearer {JWT_ACCESS_TOKEN}
```

### 권한 매트릭스

| 작업 | 공지사항(0) | 학사정보(1) | 시설정보(2) | 강의공지(3) |
|------|-------------|-------------|-------------|-------------|
| 생성 | 관리자 | 관리자 | 관리자 | 강사+관리자 |
| 조회 | 전체 | 전체 | 전체 | 수강생+작성자 |
| 수정 | 관리자 | 관리자 | 관리자 | 작성자+관리자 |
| 삭제 | 관리자 | 관리자 | 관리자 | 작성자+관리자 |

## 🏗️ 아키텍처 개요

### 계층 구조
```
📱 Frontend (React/Vue/Angular)
    ↓ HTTP/JSON
🌐 Controller Layer (REST API)
    ↓ Business Logic
⚙️ Service Layer (비즈니스 로직)
    ↓ Data Access
🗄️ Repository Layer (JPA/Hibernate)
    ↓ SQL
💾 Database (MySQL/PostgreSQL)
```

### 주요 컴포넌트

#### Controllers (7개)
- `BoardController` - 조회 전용
- `BoardCreateController` - 생성 전용  
- `BoardUpdateController` - 수정 전용
- `BoardAttachment*Controller` - 파일 관리 (4개)

#### Services (6개)
- `BoardService` - 조회 비즈니스 로직
- `BoardManagementService` - 생성/수정/삭제 로직
- `BoardAttachment*Service` - 파일 관리 (4개)

#### 핵심 Entity
- `BoardTbl` - 게시글 데이터 모델
- `BoardAttachmentTbl` - 첨부파일 데이터 모델

## 📈 성능 최적화

### 목록 조회 최적화
- **본문 제외**: 목록 조회 시 `boardContent` 필드 제외
- **선택적 필드**: 필요한 필드만 SELECT
- **페이징 처리**: 대량 데이터 효율적 처리

### 데이터 처리 최적화
- **Base64 인코딩**: 제목/내용 자동 인코딩/디코딩
- **조회수 최적화**: 상세 조회 시에만 증가
- **인덱스 활용**: 게시판 코드, 강의 시리얼 인덱싱

## 🛠️ 개발 환경 설정

### 필수 도구
- **Java 11+** - 백엔드 런타임
- **Spring Boot 2.7+** - 프레임워크
- **MySQL/PostgreSQL** - 데이터베이스
- **Postman/Thunder Client** - API 테스트

### 개발 서버 실행
```bash
# 백엔드 서버 시작
cd backend/BlueCrab
./mvnw spring-boot:run

# API 테스트
curl -X POST "http://localhost:8080/api/boards/list" \
-H "Content-Type: application/json" \
-d '{}'
```

## 🧪 테스트

### 기존 테스트 파일
- `test-1-create.js` - 게시글 생성 테스트
- `test-2-read.js` - 게시글 조회 테스트  
- `test-3-update-delete.js` - 수정/삭제 테스트

### 자동화된 테스트
- **PowerShell 스크립트**: [examples/usage-examples.md](./examples/usage-examples.md#powershell-스크립트-예제)
- **Python 클라이언트**: [examples/usage-examples.md](./examples/usage-examples.md#python-예제)

## 🐛 문제 해결

### 자주 발생하는 오류

| 오류 | 원인 | 해결책 |
|------|------|--------|
| 401 Unauthorized | JWT 토큰 없음/만료 | 토큰 재발급 후 재시도 |
| 403 Forbidden | 권한 없음 | 사용자 권한 확인 |
| 400 Bad Request | 필수 필드 누락 | 요청 데이터 검증 |
| 404 Not Found | 게시글 없음 | boardIdx 확인 |

### 디버깅 팁
1. **로그 확인**: 서버 로그에서 상세 오류 메시지 확인
2. **JWT 검증**: JWT 토큰 유효성 및 타입 확인
3. **권한 확인**: 사용자 권한과 게시판 코드 매치 확인

## 📝 버전 이력

### v2.0 (현재) - 2024.10.27
- ✅ 실제 구현 기반 문서 재작성
- ✅ JWT 인증 방식 반영
- ✅ 분리된 파일 관리 시스템 문서화
- ✅ 상세 가이드 문서 분리
- ✅ 실제 클래스 파일 목록 정리

### v1.1 (백업) - README_v1.0_backup.md
- lecSerial 타입 버그 수정 (Integer → String)
- 설계 단계 문서 (실제 구현과 차이)

## 🤝 기여 가이드

### 문서 개선
1. 오타나 정보 오류 발견 시 이슈 등록
2. 새로운 사용 예제 추가
3. API 변경 시 문서 즉시 업데이트

### 코드 기여
1. 새로운 기능 추가 시 해당 문서 업데이트
2. 테스트 파일 추가
3. 성능 개선 사항 문서화

## 📞 지원

### 문의 채널
- **이슈 등록**: GitHub Issues
- **실시간 문의**: 개발팀 Slack
- **문서 피드백**: Pull Request

### 추가 개발 예정 기능
- [ ] 게시글 검색 API
- [ ] 게시글 수정 API  
- [ ] 게시글 삭제 API
- [ ] 댓글 시스템
- [ ] 이미지 업로드 최적화
- [ ] 실시간 알림

---

> 📌 **중요**: 이 문서는 실제 구현과 100% 일치하도록 유지됩니다.  
> API 변경 시 즉시 문서를 업데이트해 주세요.
