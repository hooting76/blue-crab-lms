# 게시글 생성 API 가이드

## 개요

게시글 생성은 3단계 워크플로우로 구성되어 있습니다:
1. 첨부파일 업로드 (선택사항)
2. 게시글 생성
3. 첨부파일 연결 (첨부파일이 있는 경우)

## 인증 요구사항

모든 게시글 생성 작업에는 **JWT 액세스 토큰**이 필요합니다.

```http
Authorization: Bearer {JWT_ACCESS_TOKEN}
```

## API 엔드포인트

### 1. 게시글 생성

```http
POST /api/boards/create
Content-Type: application/json
Authorization: Bearer {JWT_ACCESS_TOKEN}
```

#### 요청 본문

```json
{
  "boardTitle": "게시글 제목",
  "boardContent": "게시글 내용",
  "boardCode": 0,
  "lecSerial": "ETH201"  // boardCode=3일 때만 필수
}
```

#### 필수 필드

| 필드 | 타입 | 설명 | 필수 여부 |
|------|------|------|----------|
| `boardTitle` | String | 게시글 제목 | 필수 |
| `boardContent` | String | 게시글 내용 | 필수 |
| `boardCode` | Integer | 게시판 코드 (0~3) | 필수 |
| `lecSerial` | String | 강의 시리얼 코드 | boardCode=3일 때만 필수 |

#### 응답 예시

**성공 응답 (200 OK):**
```json
{
  "success": true,
  "message": "게시글이 성공적으로 작성되었습니다.",
  "board": {
    "boardIdx": 123,
    "boardTitle": "게시글 제목",
    "boardContent": "게시글 내용",
    "boardCode": 0,
    "boardWriter": "작성자명",
    "boardReg": "2024-01-01 10:00:00",
    "boardView": 0
  },
  "boardIdx": 123
}
```

### 2. 첨부파일 연결 (선택사항)

첨부파일이 있는 경우, 게시글 생성 후 이 API를 호출하여 첨부파일을 연결합니다.

```http
POST /api/boards/link-attachments/{boardIdx}
Content-Type: application/json
Authorization: Bearer {JWT_ACCESS_TOKEN}
```

#### 요청 본문

```json
{
  "attachmentIdx": [101, 102, 103]
}
```

#### 응답 예시

```json
{
  "success": true,
  "message": "첨부파일이 성공적으로 연결되었습니다.",
  "boardIdx": 123,
  "attachmentCount": 3,
  "attachmentIdx": [101, 102, 103]
}
```

## 오류 처리

### 일반적인 오류

| HTTP 상태 | 오류 코드 | 설명 |
|-----------|-----------|------|
| 400 | VALIDATION_ERROR | 필수 필드 누락 또는 유효성 검사 실패 |
| 401 | AUTHENTICATION_REQUIRED | JWT 토큰이 제공되지 않음 |
| 401 | INVALID_TOKEN | 유효하지 않은 JWT 토큰 |
| 401 | INVALID_TOKEN_TYPE | 액세스 토큰이 아님 |
| 403 | INSUFFICIENT_PERMISSION | 게시글 작성 권한 없음 |

### 오류 응답 예시

```json
{
  "success": false,
  "message": "게시글 제목은 필수입니다.",
  "errorCode": "VALIDATION_ERROR"
}
```

## 데이터 처리

### Base64 인코딩

제목과 내용은 서버에서 자동으로 Base64로 인코딩되어 저장됩니다.
- 클라이언트는 평문으로 전송
- 서버에서 길이 검증 후 Base64 인코딩
- 데이터베이스에 인코딩된 형태로 저장

### 길이 제한

- **제목**: 원문 기준 제한 (정확한 제한은 `Base64ValidationUtil` 참조)
- **내용**: 원문 기준 제한 (정확한 제한은 `Base64ValidationUtil` 참조)

## 권한 확인

게시글 생성 권한은 `BoardManagementService.createBoard()` 메서드에서 확인됩니다:

- **공지사항 (0), 학사정보 (1), 시설정보 (2)**: 관리자만 생성 가능
- **강의공지 (3)**: 강사 및 관리자 생성 가능

## 사용 예시

### PowerShell 예시

```powershell
# 공지사항 생성
curl -X POST "http://localhost:8080/api/boards/create" `
-H "Content-Type: application/json" `
-H "Authorization: Bearer YOUR_JWT_TOKEN" `
-d '{\"boardTitle\":\"중요 공지사항\",\"boardContent\":\"모든 학생들에게 알려드립니다.\",\"boardCode\":0}'

# 강의 공지 생성
curl -X POST "http://localhost:8080/api/boards/create" `
-H "Content-Type: application/json" `
-H "Authorization: Bearer YOUR_JWT_TOKEN" `
-d '{\"boardTitle\":\"과제 안내\",\"boardContent\":\"다음 주까지 과제를 제출해 주세요.\",\"boardCode\":3,\"lecSerial\":\"ETH201\"}'
```

### JavaScript 예시

```javascript
// 게시글 생성 함수
async function createBoard(boardData, jwtToken) {
  try {
    const response = await fetch('/api/boards/create', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${jwtToken}`
      },
      body: JSON.stringify(boardData)
    });
    
    const result = await response.json();
    
    if (result.success) {
      console.log('게시글 생성 성공:', result.boardIdx);
      return result;
    } else {
      console.error('게시글 생성 실패:', result.message);
      throw new Error(result.message);
    }
  } catch (error) {
    console.error('API 호출 오류:', error);
    throw error;
  }
}

// 사용 예시
const boardData = {
  boardTitle: "새로운 공지사항",
  boardContent: "공지 내용입니다.",
  boardCode: 0
};

createBoard(boardData, 'your-jwt-token-here')
  .then(result => console.log('생성된 게시글 ID:', result.boardIdx))
  .catch(error => console.error('오류:', error));
```

## 관련 파일

### Controller
- `BoardCreateController.java` - 게시글 생성 담당
- `BoardController.java` - 첨부파일 연결 담당

### Service
- `BoardManagementService.java` - 게시글 생성 비즈니스 로직
- `JwtUtil.java` - JWT 토큰 검증

### Utility
- `Base64ValidationUtil.java` - 텍스트 길이 검증 및 Base64 인코딩

### Entity
- `BoardTbl.java` - 게시글 데이터 모델