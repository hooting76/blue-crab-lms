# 첨부파일 관리 API 가이드

## 개요

첨부파일 관리는 업로드, 다운로드, 삭제, 연결 기능으로 구성됩니다.
게시글과 첨부파일은 분리된 워크플로우로 관리됩니다.

## 워크플로우

### 1. 첨부파일 포함 게시글 작성 시
1. 첨부파일 업로드 → 첨부파일 IDX 반환
2. 게시글 생성
3. 게시글에 첨부파일 IDX 연결

### 2. 기존 게시글에 첨부파일 추가 시
1. 첨부파일 업로드 → 첨부파일 IDX 반환
2. 게시글에 추가 첨부파일 IDX 연결

## API 엔드포인트

### 1. 첨부파일 업로드

```http
POST /api/board-attachments/upload
Content-Type: multipart/form-data
Authorization: Bearer {JWT_ACCESS_TOKEN}
```

#### 요청 형식

```http
Content-Type: multipart/form-data

files: [파일1, 파일2, 파일3]
```

#### 응답 예시

```json
{
  "success": true,
  "message": "파일 업로드가 완료되었습니다.",
  "uploadedFiles": [
    {
      "attachmentIdx": 101,
      "originalName": "document.pdf",
      "storedName": "20241027_101_document.pdf",
      "fileSize": 2048576,
      "mimeType": "application/pdf"
    },
    {
      "attachmentIdx": 102,
      "originalName": "image.jpg",
      "storedName": "20241027_102_image.jpg",
      "fileSize": 1024768,
      "mimeType": "image/jpeg"
    }
  ],
  "attachmentIdx": [101, 102]
}
```

### 2. 첨부파일 다운로드

```http
GET /api/board-attachments/download/{attachmentIdx}
```

#### 응답

- 성공 시: 파일 스트림 반환
- 실패 시: 404 또는 403 오류

### 3. 첨부파일 삭제

```http
DELETE /api/board-attachments/{attachmentIdx}
Authorization: Bearer {JWT_ACCESS_TOKEN}
```

#### 응답 예시

```json
{
  "success": true,
  "message": "첨부파일이 삭제되었습니다.",
  "attachmentIdx": 101
}
```

### 4. 게시글-첨부파일 연결

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

## 파일 관리 정책

### 지원 파일 형식

일반적으로 다음 파일 형식을 지원합니다:
- 문서: PDF, DOC, DOCX, XLS, XLSX, PPT, PPTX
- 이미지: JPG, JPEG, PNG, GIF
- 압축: ZIP, RAR
- 텍스트: TXT, CSV

### 파일 크기 제한

- 개별 파일: 최대 10MB (서버 설정에 따라 달라질 수 있음)
- 전체 업로드: 최대 50MB

### 파일 저장 구조

```
uploads/
├── boards/
│   ├── 2024/
│   │   ├── 10/
│   │   │   ├── 27/
│   │   │   │   ├── 20241027_101_document.pdf
│   │   │   │   └── 20241027_102_image.jpg
```

## 보안 정책

### 업로드 보안

- JWT 토큰 인증 필수
- 파일 확장자 검증
- MIME 타입 검증
- 악성 파일 스캔 (구현 예정)

### 다운로드 보안

- 게시글 조회 권한 확인
- 직접 경로 접근 차단
- 파일명 난독화

## 사용 예시

### PowerShell 예시

```powershell
# 파일 업로드
curl -X POST "http://localhost:8080/api/board-attachments/upload" `
-H "Authorization: Bearer YOUR_JWT_TOKEN" `
-F "files=@C:\path\to\document.pdf" `
-F "files=@C:\path\to\image.jpg"

# 파일 다운로드
curl -X GET "http://localhost:8080/api/board-attachments/download/101" `
-o "downloaded_file.pdf"

# 첨부파일 연결
curl -X POST "http://localhost:8080/api/boards/link-attachments/123" `
-H "Content-Type: application/json" `
-H "Authorization: Bearer YOUR_JWT_TOKEN" `
-d '{\"attachmentIdx\":[101,102]}'
```

### JavaScript 예시

```javascript
// 파일 업로드 함수
async function uploadFiles(files, jwtToken) {
  const formData = new FormData();
  
  // 여러 파일 추가
  files.forEach(file => {
    formData.append('files', file);
  });
  
  try {
    const response = await fetch('/api/board-attachments/upload', {
      method: 'POST',
      headers: {
        'Authorization': `Bearer ${jwtToken}`
      },
      body: formData
    });
    
    const result = await response.json();
    
    if (result.success) {
      console.log('업로드된 파일 IDX:', result.attachmentIdx);
      return result.attachmentIdx;
    } else {
      throw new Error(result.message);
    }
  } catch (error) {
    console.error('파일 업로드 오류:', error);
    throw error;
  }
}

// 첨부파일 연결 함수
async function linkAttachments(boardIdx, attachmentIdx, jwtToken) {
  try {
    const response = await fetch(`/api/boards/link-attachments/${boardIdx}`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${jwtToken}`
      },
      body: JSON.stringify({ attachmentIdx })
    });
    
    const result = await response.json();
    
    if (result.success) {
      console.log('첨부파일 연결 완료');
      return result;
    } else {
      throw new Error(result.message);
    }
  } catch (error) {
    console.error('첨부파일 연결 오류:', error);
    throw error;
  }
}

// 파일 다운로드 함수
function downloadFile(attachmentIdx, filename) {
  const link = document.createElement('a');
  link.href = `/api/board-attachments/download/${attachmentIdx}`;
  link.download = filename;
  document.body.appendChild(link);
  link.click();
  document.body.removeChild(link);
}

// 사용 예시
const fileInput = document.getElementById('fileInput');
const files = fileInput.files;

uploadFiles(files, 'your-jwt-token')
  .then(attachmentIdx => {
    console.log('업로드 완료, IDX:', attachmentIdx);
    // 게시글 생성 후 첨부파일 연결
    return linkAttachments(123, attachmentIdx, 'your-jwt-token');
  })
  .then(() => {
    console.log('게시글과 첨부파일 연결 완료');
  })
  .catch(error => {
    console.error('처리 오류:', error);
  });
```

## 오류 처리

### 업로드 오류

| 상황 | 응답 |
|------|------|
| 파일 크기 초과 | 413 Request Entity Too Large |
| 지원하지 않는 파일 형식 | 400 Bad Request |
| 권한 없음 | 401 Unauthorized |
| 서버 저장 공간 부족 | 507 Insufficient Storage |

### 다운로드 오류

| 상황 | 응답 |
|------|------|
| 파일 없음 | 404 Not Found |
| 권한 없음 | 403 Forbidden |
| 파일 손상 | 500 Internal Server Error |

## 관련 파일

### Controller
- `BoardAttachmentUploadController.java` - 파일 업로드 처리
- `BoardAttachmentDownloadController.java` - 파일 다운로드 처리
- `BoardAttachmentDeleteController.java` - 파일 삭제 처리
- `BoardController.java` - 첨부파일 연결 처리

### Service
- `BoardAttachmentService.java` - 첨부파일 비즈니스 로직
- `BoardAttachmentQueryService.java` - 첨부파일 조회 로직

### Entity
- `BoardAttachmentTbl.java` - 첨부파일 데이터 모델

### DTO
- `AttachmentLinkRequest.java` - 첨부파일 연결 요청 DTO

## 데이터베이스 스키마

### BOARD_ATTACHMENT_TBL 주요 필드

| 필드명 | 타입 | 설명 |
|--------|------|------|
| ATTACHMENT_IDX | INTEGER | 첨부파일 ID (기본키) |
| ORIGINAL_NAME | VARCHAR | 원본 파일명 |
| STORED_NAME | VARCHAR | 저장된 파일명 |
| FILE_PATH | VARCHAR | 파일 저장 경로 |
| FILE_SIZE | BIGINT | 파일 크기 (바이트) |
| MIME_TYPE | VARCHAR | MIME 타입 |
| UPLOAD_DATE | VARCHAR | 업로드 일시 |
| UPLOADER_IDX | INTEGER | 업로더 ID |
| IS_ACTIVE | INTEGER | 활성화 여부 |