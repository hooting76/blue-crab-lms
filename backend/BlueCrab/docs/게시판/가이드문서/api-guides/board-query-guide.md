# 게시글 조회 API 가이드

## 개요

게시글 조회 기능은 목록 조회와 상세 조회로 구분됩니다.
- **목록 조회**: 본문을 제외한 요약 정보를 페이징으로 제공
- **상세 조회**: 본문 포함 전체 정보 + 첨부파일 상세 정보 + 조회수 증가

## API 엔드포인트

### 1. 게시글 목록 조회

```http
POST /api/boards/list
Content-Type: application/json
```

#### 요청 본문

```json
{
  "boardCode": 0,        // 선택사항: 게시판 코드별 필터링
  "lecSerial": "ETH201", // 선택사항: 강의별 필터링 (boardCode=3일 때)
  "page": 0,             // 선택사항: 페이지 번호 (기본값: 0)
  "size": 10             // 선택사항: 페이지 크기 (기본값: 10)
}
```

#### 조회 패턴

| 조회 유형 | boardCode | lecSerial | 설명 |
|-----------|-----------|-----------|------|
| 전체 조회 | 생략 | 생략 | 모든 활성 게시글 |
| 코드별 조회 | 0~3 | 생략 | 특정 게시판의 모든 게시글 |
| 강의별 조회 | 3 | "ETH201" | 특정 강의의 공지사항만 |

#### 응답 구조

```json
{
  "content": [
    {
      "boardIdx": 123,
      "boardCode": 3,
      "boardOn": 1,
      "boardWriter": "작성자명",
      "boardTitle": "게시글 제목",
      "boardImg": null,
      "boardFile": "101,102,103", // 첨부파일 IDX 목록
      "boardView": 15,
      "boardReg": "2024-01-01 10:00:00",
      "boardLast": "2024-01-01 10:00:00",
      "boardIp": "192.168.1.1",
      "boardWriterIdx": 456,
      "boardWriterType": 0
    }
  ],
  "totalElements": 25,    // 전체 게시글 수
  "totalPages": 3,        // 전체 페이지 수
  "size": 10,             // 페이지 크기
  "number": 0,            // 현재 페이지 번호
  "first": true,          // 첫 페이지 여부
  "last": false           // 마지막 페이지 여부
}
```

### 2. 게시글 상세 조회

```http
POST /api/boards/detail
Content-Type: application/json
```

#### 요청 본문

```json
{
  "boardIdx": 123
}
```

#### 응답 구조

```json
{
  "boardIdx": 123,
  "boardTitle": "게시글 제목",
  "boardContent": "게시글 본문 내용",
  "boardCode": 3,
  "lecSerial": "ETH201",
  "boardWriter": "작성자명",
  "boardView": 16,        // 조회수 자동 증가됨
  "boardReg": "2024-01-01 10:00:00",
  "boardLast": "2024-01-01 10:00:00",
  "boardWriterIdx": 456,
  "boardWriterType": 0,
  "attachmentDetails": [  // 첨부파일 상세 정보
    {
      "attachmentIdx": 101,
      "originalName": "document.pdf",
      "storedName": "20241001_document.pdf",
      "fileSize": 2048,
      "mimeType": "application/pdf",
      "uploadDate": "2024-01-01 09:30:00"
    }
  ]
}
```

## 특별 기능

### 자동 조회수 증가

게시글 상세 조회 시 자동으로 조회수가 1 증가합니다.
- 목록 조회에서는 조회수가 증가하지 않음
- 상세 조회 시에만 증가

### Base64 디코딩

저장된 제목과 내용은 Base64로 인코딩되어 있으나, API 응답에서는 자동으로 디코딩되어 반환됩니다.

### 첨부파일 정보

상세 조회 시 `attachmentDetails` 배열에 첨부파일의 상세 정보가 포함됩니다.

## 오류 처리

### 일반적인 오류

| HTTP 상태 | 설명 |
|-----------|------|
| 400 | 잘못된 요청 파라미터 |
| 404 | 게시글을 찾을 수 없음 |
| 500 | 서버 내부 오류 |

### 오류 응답 예시

```json
{
  "error": "게시글을 찾을 수 없습니다.",
  "status": 404
}
```

## 사용 예시

### PowerShell 예시

```powershell
# 전체 게시글 목록 조회
curl -X POST "http://localhost:8080/api/boards/list" `
-H "Content-Type: application/json" `
-d '{}'

# 공지사항 목록 조회
curl -X POST "http://localhost:8080/api/boards/list" `
-H "Content-Type: application/json" `
-d '{\"boardCode\":0}'

# 특정 강의 공지 조회
curl -X POST "http://localhost:8080/api/boards/list" `
-H "Content-Type: application/json" `
-d '{\"boardCode\":3,\"lecSerial\":\"ETH201\"}'

# 페이징이 있는 조회
curl -X POST "http://localhost:8080/api/boards/list" `
-H "Content-Type: application/json" `
-d '{\"boardCode\":0,\"page\":1,\"size\":5}'

# 게시글 상세 조회
curl -X POST "http://localhost:8080/api/boards/detail" `
-H "Content-Type: application/json" `
-d '{\"boardIdx\":123}'
```

### JavaScript 예시

```javascript
// 게시글 목록 조회 함수
async function fetchBoardList(filters = {}) {
  try {
    const response = await fetch('/api/boards/list', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json'
      },
      body: JSON.stringify(filters)
    });
    
    const result = await response.json();
    return result;
  } catch (error) {
    console.error('목록 조회 오류:', error);
    throw error;
  }
}

// 게시글 상세 조회 함수
async function fetchBoardDetail(boardIdx) {
  try {
    const response = await fetch('/api/boards/detail', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json'
      },
      body: JSON.stringify({ boardIdx })
    });
    
    if (response.ok) {
      return await response.json();
    } else {
      throw new Error('게시글을 찾을 수 없습니다.');
    }
  } catch (error) {
    console.error('상세 조회 오류:', error);
    throw error;
  }
}

// 사용 예시
// 공지사항 목록 조회
fetchBoardList({ boardCode: 0, page: 0, size: 10 })
  .then(result => {
    console.log('총 게시글 수:', result.totalElements);
    console.log('게시글 목록:', result.content);
  });

// 게시글 상세 조회
fetchBoardDetail(123)
  .then(board => {
    console.log('제목:', board.boardTitle);
    console.log('내용:', board.boardContent);
    console.log('첨부파일 수:', board.attachmentDetails.length);
  });
```

## 성능 최적화

### 목록 조회 최적화

목록 조회 시 본문(`boardContent`)을 제외하고 조회하여 성능을 최적화했습니다.
- `findBoardListWithoutContent()` 메서드 사용
- 페이징 처리로 대량 데이터 처리 최적화

### 쿼리 최적화

Repository에서 JPQL을 사용하여 필요한 필드만 선택적으로 조회합니다.

## 관련 파일

### Controller
- `BoardController.java` - 목록 및 상세 조회 담당

### Service
- `BoardService.java` - 조회 전용 비즈니스 로직
- `BoardAttachmentQueryService.java` - 첨부파일 조회 로직

### Repository
- `BoardRepository.java` - 데이터베이스 조회 쿼리

### Entity
- `BoardTbl.java` - 게시글 데이터 모델
- `BoardAttachmentTbl.java` - 첨부파일 데이터 모델