# 구현 파일 목록 및 구조

## 컨트롤러 (Controller) 파일들

### 게시글 관련
- **`BoardController.java`**
  - 위치: `src/main/java/BlueCrab/com/example/controller/Board/`
  - 역할: 게시글 조회 전용 (목록, 상세, 첨부파일 연결)
  - 주요 메서드:
    - `getAllBoards()` - 게시글 목록 조회
    - `getBoardsByCode()` - 코드별 게시글 조회
    - `getBoardDetail()` - 게시글 상세 조회
    - `linkAttachments()` - 첨부파일 연결

- **`BoardCreateController.java`**
  - 위치: `src/main/java/BlueCrab/com/example/controller/Board/`
  - 역할: 게시글 생성 전용
  - 주요 메서드:
    - `createBoard()` - 게시글 생성

- **`BoardUpdateController.java`**
  - 위치: `src/main/java/BlueCrab/com/example/controller/Board/`
  - 역할: 게시글 수정 전용
  - 상태: 구현 예정

### 첨부파일 관련
- **`BoardAttachmentUploadController.java`**
  - 위치: `src/main/java/BlueCrab/com/example/controller/Board/Attachment/`
  - 역할: 첨부파일 업로드 처리

- **`BoardAttachmentDownloadController.java`**
  - 위치: `src/main/java/BlueCrab/com/example/controller/Board/Attachment/`
  - 역할: 첨부파일 다운로드 처리

- **`BoardAttachmentDeleteController.java`**
  - 위치: `src/main/java/BlueCrab/com/example/controller/Board/Attachment/`
  - 역할: 첨부파일 삭제 처리

### 통계/모니터링
- **`BoardStatisticsController.java`**
  - 위치: `src/main/java/BlueCrab/com/example/controller/Board/`
  - 역할: 게시판 통계 정보 제공

## 서비스 (Service) 파일들

### 게시글 서비스
- **`BoardService.java`**
  - 위치: `src/main/java/BlueCrab/com/example/service/Board/`
  - 역할: 게시글 조회 전용 비즈니스 로직
  - 주요 메서드:
    - `getAllBoardsForList()` - 전체 목록 조회
    - `getBoardsByCodeForList()` - 코드별 목록 조회
    - `getBoardsByCodeAndLecSerialForList()` - 강의별 목록 조회
    - `getBoardDetail()` - 상세 조회 + 조회수 증가

- **`BoardManagementService.java`**
  - 위치: `src/main/java/BlueCrab/com/example/service/Board/`
  - 역할: 게시글 생성/수정/삭제 비즈니스 로직
  - 주요 메서드:
    - `createBoard()` - 게시글 생성 + 권한 확인

### 첨부파일 서비스
- **`BoardAttachmentService.java`**
  - 위치: `src/main/java/BlueCrab/com/example/service/Board/Attachment/`
  - 역할: 첨부파일 업로드/삭제 비즈니스 로직

- **`BoardAttachmentQueryService.java`**
  - 위치: `src/main/java/BlueCrab/com/example/service/Board/Attachment/`
  - 역할: 첨부파일 조회 전용 로직
  - 주요 메서드:
    - `getActiveAttachmentsByBoardId()` - 게시글별 첨부파일 조회

- **`BoardAttachmentUploadService.java`**
  - 위치: `src/main/java/BlueCrab/com/example/service/Board/Attachment/`
  - 역할: 파일 업로드 처리

- **`BoardAttachmentDeleteService.java`**
  - 위치: `src/main/java/BlueCrab/com/example/service/Board/Attachment/`
  - 역할: 파일 삭제 처리

### 통계 서비스
- **`BoardStatsService.java`**
  - 위치: `src/main/java/BlueCrab/com/example/service/Board/`
  - 역할: 게시판 통계 및 분석 로직

## 레포지토리 (Repository) 파일들

- **`BoardRepository.java`**
  - 위치: `src/main/java/BlueCrab/com/example/repository/Board/`
  - 역할: 게시글 데이터베이스 접근 계층
  - 주요 메서드:
    - `findBoardListWithoutContent()` - 목록 조회용 (본문 제외)
    - `findBoardListByCodeWithoutContent()` - 코드별 목록 조회
    - `findBoardListByCodeAndLecSerialWithoutContent()` - 강의별 목록 조회
    - `incrementBoardView()` - 조회수 증가
    - `findByBoardIdxAndBoardOn()` - 활성 게시글 조회

- **`BoardAttachmentRepository.java`**
  - 위치: `src/main/java/BlueCrab/com/example/repository/Board/Attachment/`
  - 역할: 첨부파일 데이터베이스 접근 계층

## 엔티티 (Entity) 파일들

- **`BoardTbl.java`**
  - 위치: `src/main/java/BlueCrab/com/example/entity/Board/`
  - 역할: 게시글 데이터 모델
  - 주요 필드:
    - `boardIdx` - 게시글 ID
    - `boardCode` - 게시판 코드
    - `boardTitle` - 제목 (Base64 인코딩)
    - `boardContent` - 내용 (Base64 인코딩)
    - `lecSerial` - 강의 시리얼
    - `attachmentIdx` - 첨부파일 IDX 목록

- **`BoardAttachmentTbl.java`**
  - 위치: `src/main/java/BlueCrab/com/example/entity/Board/Attachment/`
  - 역할: 첨부파일 데이터 모델

## DTO 파일들

- **`AttachmentLinkRequest.java`**
  - 위치: `src/main/java/BlueCrab/com/example/dto/Board/Attachment/`
  - 역할: 첨부파일 연결 요청 DTO

## 유틸리티 (Util) 파일들

- **`JwtUtil.java`**
  - 위치: `src/main/java/BlueCrab/com/example/util/`
  - 역할: JWT 토큰 검증 및 처리
  - 주요 메서드:
    - `isTokenValid()` - 토큰 유효성 검증
    - `isAccessToken()` - 액세스 토큰 여부 확인
    - `extractUsername()` - 사용자명 추출

- **`Base64ValidationUtil.java`**
  - 위치: `src/main/java/BlueCrab/com/example/util/`
  - 역할: 텍스트 길이 검증 및 Base64 인코딩
  - 주요 메서드:
    - `validateAndEncodeTitleIfValid()` - 제목 검증 및 인코딩
    - `validateAndEncodeContentIfValid()` - 내용 검증 및 인코딩

## 아키텍처 패턴

### 계층 구조
```
Controller Layer (API 엔드포인트)
    ↓
Service Layer (비즈니스 로직)
    ↓
Repository Layer (데이터 접근)
    ↓
Entity Layer (데이터 모델)
```

### 관심사 분리 (Separation of Concerns)

#### 1. 기능별 분리
- **조회 전용**: `BoardController` + `BoardService`
- **생성 전용**: `BoardCreateController` + `BoardManagementService`
- **첨부파일**: 별도 컨트롤러 및 서비스

#### 2. 책임 분리
- **Controller**: HTTP 요청/응답 처리, 인증 확인
- **Service**: 비즈니스 로직, 권한 확인, 데이터 변환
- **Repository**: 데이터베이스 쿼리, 성능 최적화
- **Entity**: 데이터 모델, 유틸리티 메서드

## 의존성 관계

### 주요 의존성 흐름
```
BoardController
├── BoardService
│   └── BoardRepository
├── BoardAttachmentQueryService
│   └── BoardAttachmentRepository
└── JwtUtil

BoardCreateController
├── BoardManagementService
│   └── BoardRepository
├── JwtUtil
└── Base64ValidationUtil
```

## 설정 파일들

### 데이터베이스 설정
- `application.properties` 또는 `application.yml`
- JPA/Hibernate 설정

### 보안 설정
- Spring Security 설정
- JWT 설정

### 파일 업로드 설정
- Multipart 설정
- 파일 크기 제한

## 테스트 파일들

### 기존 테스트 파일들
- `test-1-create.js` - 게시글 생성 테스트
- `test-2-read.js` - 게시글 조회 테스트
- `test-3-update-delete.js` - 수정/삭제 테스트

### 권장 추가 테스트
- Unit Tests (JUnit)
- Integration Tests
- API Tests (Postman/Newman)

## 개발 가이드라인

### 코딩 컨벤션
- 작성자 주석 필수
- 로깅 레벨 적절히 활용
- 예외 처리 철저히

### 성능 최적화
- 목록 조회 시 본문 제외
- 페이징 처리 활용
- 인덱스 최적화

### 보안 가이드라인
- JWT 토큰 검증 필수
- SQL 인젝션 방지
- 파일 업로드 보안