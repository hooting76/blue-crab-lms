# 게시판 첨부파일 API 가이드

> **버전**: 2.0  
> **대상**: 프론트엔드 개발자  
> **상태**: ✅ 프로덕션 준비 완료

---

## 🎯 핵심 요약

### 3단계 워크플로우 (첨부파일 포함 게시글 작성)

```
1️⃣ 게시글 생성 (JSON)
   POST /api/boards/create
   ↓
2️⃣ 파일 업로드 (FormData) - 선택사항
   POST /api/board-attachments/upload/{boardIdx}
   ↓
3️⃣ 첨부파일 연결 (JSON)
   POST /api/boards/link-attachments/{boardIdx}
```

### 파일 제한
- **최대 개수**: 5개/게시글
- **최대 크기**: 10MB/파일
- **허용 형식**: PDF, DOC, DOCX, XLS, XLSX, PPT, PPTX, TXT, JPG, JPEG, PNG, GIF

---

## 📋 API 목록

| API | 엔드포인트 | 인증 | 설명 |
|-----|-----------|------|------|
| 파일 업로드 | `POST /api/board-attachments/upload/{boardIdx}` | ✅ | FormData, 최대 5개 |
| 파일 다운로드 | `POST /api/board-attachments/download` | ❌ | 브라우저 자동 처리 |
| 파일 정보 조회 | `POST /api/board-attachments/info` | ❌ | 메타데이터만 |
| 파일 삭제 | `POST /api/board-attachments/delete/{attachmentIdx}` | ✅ | 단일 삭제 |
| 다중 삭제 | `POST /api/board-attachments/delete-multiple` | ✅ | 여러 개 삭제 |
| 전체 삭제 | `POST /api/board-attachments/delete-all/{boardIdx}` | ✅ | 게시글 전체 |
| 첨부파일 연결 | `POST /api/boards/link-attachments/{boardIdx}` | ✅ | 3단계 마지막 |

---

## 🚀 빠른 시작 코드

### 1️⃣ 게시글 생성 (첨부파일 없음)

```javascript
const response = await fetch('/api/boards/create', {
  method: 'POST',
  headers: {
    'Content-Type': 'application/json',
    'Authorization': `Bearer ${jwtToken}`
  },
  body: JSON.stringify({
    boardCode: 0,
    boardTitle: '제목',
    boardContent: '내용'
  })
});

const { boardIdx } = await response.json();
```

### 2️⃣ 파일 업로드 (FormData 필수!)

```javascript
// ❌ JSON으로 보내면 안 됨!
// body: JSON.stringify({ files: [...] })

// ✅ FormData 사용
const formData = new FormData();
files.forEach(file => formData.append('files', file));

const response = await fetch(`/api/board-attachments/upload/${boardIdx}`, {
  method: 'POST',
  headers: {
    'Authorization': `Bearer ${jwtToken}`
    // Content-Type 생략! FormData가 자동 설정
  },
  body: formData
});

const { attachments } = await response.json();
// attachments: [{ attachmentIdx: 45, originalFileName: "문서.pdf", ... }]
```

### 3️⃣ 첨부파일 연결

```javascript
const attachmentIdxList = attachments.map(a => a.attachmentIdx);

await fetch(`/api/boards/link-attachments/${boardIdx}`, {
  method: 'POST',
  headers: {
    'Content-Type': 'application/json',
    'Authorization': `Bearer ${jwtToken}`
  },
  body: JSON.stringify({ attachmentIdxList })
});
```

### 📥 파일 다운로드

```javascript
async function downloadFile(attachmentIdx, fileName) {
  const response = await fetch('/api/board-attachments/download', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ attachmentIdx })
  });
  
  const blob = await response.blob();
  const url = window.URL.createObjectURL(blob);
  const a = document.createElement('a');
  a.href = url;
  a.download = fileName;
  a.click();
  window.URL.revokeObjectURL(url);
}

// 사용 예시
downloadFile(45, '문서.pdf');
```

### 🗑️ 파일 삭제

```javascript
// 단일 삭제
await fetch(`/api/board-attachments/delete/${attachmentIdx}`, {
  method: 'POST',
  headers: { 'Authorization': `Bearer ${jwtToken}` }
});

// 다중 삭제
await fetch('/api/board-attachments/delete-multiple', {
  method: 'POST',
  headers: {
    'Content-Type': 'application/json',
    'Authorization': `Bearer ${jwtToken}`
  },
  body: JSON.stringify({ attachmentIdxList: [45, 46, 47] })
});

// 게시글 전체 삭제
await fetch(`/api/board-attachments/delete-all/${boardIdx}`, {
  method: 'POST',
  headers: { 'Authorization': `Bearer ${jwtToken}` }
});
```

---

## 🎨 React 컴포넌트 예제

### 파일 업로드 컴포넌트

```jsx
function FileUploadForm({ boardIdx, onSuccess }) {
  const [files, setFiles] = useState([]);
  const [uploading, setUploading] = useState(false);

  const handleFileChange = (e) => {
    const selectedFiles = Array.from(e.target.files);
    
    // 개수 제한
    if (selectedFiles.length > 5) {
      alert('최대 5개까지만 업로드 가능합니다.');
      return;
    }
    
    // 크기 제한
    const oversized = selectedFiles.find(f => f.size > 10 * 1024 * 1024);
    if (oversized) {
      alert(`${oversized.name}의 크기가 10MB를 초과합니다.`);
      return;
    }
    
    setFiles(selectedFiles);
  };

  const handleUpload = async () => {
    if (files.length === 0) return;
    
    setUploading(true);
    try {
      // 2단계: 파일 업로드
      const formData = new FormData();
      files.forEach(file => formData.append('files', file));
      
      const uploadResponse = await fetch(
        `/api/board-attachments/upload/${boardIdx}`,
        {
          method: 'POST',
          headers: { 'Authorization': `Bearer ${getToken()}` },
          body: formData
        }
      );
      
      if (!uploadResponse.ok) throw new Error('업로드 실패');
      const { attachments } = await uploadResponse.json();
      
      // 3단계: 첨부파일 연결
      const attachmentIdxList = attachments.map(a => a.attachmentIdx);
      const linkResponse = await fetch(
        `/api/boards/link-attachments/${boardIdx}`,
        {
          method: 'POST',
          headers: {
            'Content-Type': 'application/json',
            'Authorization': `Bearer ${getToken()}`
          },
          body: JSON.stringify({ attachmentIdxList })
        }
      );
      
      if (!linkResponse.ok) throw new Error('연결 실패');
      
      alert('파일 업로드 성공!');
      onSuccess();
    } catch (error) {
      alert(`업로드 실패: ${error.message}`);
    } finally {
      setUploading(false);
    }
  };

  return (
    <div>
      <input 
        type="file" 
        multiple 
        max="5"
        onChange={handleFileChange}
        accept=".pdf,.doc,.docx,.xls,.xlsx,.ppt,.pptx,.txt,.jpg,.jpeg,.png,.gif"
      />
      <p>선택된 파일: {files.length}개 (최대 5개, 각 10MB 이하)</p>
      <button onClick={handleUpload} disabled={uploading || files.length === 0}>
        {uploading ? '업로드 중...' : '업로드'}
      </button>
    </div>
  );
}
```

### 첨부파일 목록 컴포넌트

```jsx
function AttachmentList({ attachmentDetails }) {
  const handleDownload = async (attachmentIdx, fileName) => {
    try {
      const response = await fetch('/api/board-attachments/download', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ attachmentIdx })
      });
      
      const blob = await response.blob();
      const url = window.URL.createObjectURL(blob);
      const a = document.createElement('a');
      a.href = url;
      a.download = fileName;
      a.click();
      window.URL.revokeObjectURL(url);
    } catch (error) {
      alert('다운로드 실패');
    }
  };

  if (!attachmentDetails || attachmentDetails.length === 0) {
    return <p>첨부파일이 없습니다.</p>;
  }

  return (
    <div>
      <h3>첨부파일 ({attachmentDetails.length}개)</h3>
      <ul>
        {attachmentDetails.map(file => (
          <li key={file.attachmentIdx}>
            <span>{file.originalFileName}</span>
            <span>({(file.fileSize / 1024).toFixed(1)} KB)</span>
            <button 
              onClick={() => handleDownload(file.attachmentIdx, file.originalFileName)}
            >
              다운로드
            </button>
          </li>
        ))}
      </ul>
    </div>
  );
}
```

### 전체 워크플로우 (게시글 + 첨부파일)

```jsx
function CreateBoardForm() {
  const [title, setTitle] = useState('');
  const [content, setContent] = useState('');
  const [files, setFiles] = useState([]);
  const [submitting, setSubmitting] = useState(false);

  const handleSubmit = async (e) => {
    e.preventDefault();
    setSubmitting(true);
    
    try {
      // 1단계: 게시글 생성
      const boardResponse = await fetch('/api/boards/create', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          'Authorization': `Bearer ${getToken()}`
        },
        body: JSON.stringify({
          boardCode: 0,
          boardTitle: title,
          boardContent: content
        })
      });
      
      if (!boardResponse.ok) throw new Error('게시글 생성 실패');
      const { boardIdx } = await boardResponse.json();
      
      // 첨부파일이 있는 경우에만 2, 3단계 실행
      if (files.length > 0) {
        // 2단계: 파일 업로드
        const formData = new FormData();
        files.forEach(file => formData.append('files', file));
        
        const uploadResponse = await fetch(
          `/api/board-attachments/upload/${boardIdx}`,
          {
            method: 'POST',
            headers: { 'Authorization': `Bearer ${getToken()}` },
            body: formData
          }
        );
        
        if (!uploadResponse.ok) throw new Error('파일 업로드 실패');
        const { attachments } = await uploadResponse.json();
        
        // 3단계: 첨부파일 연결
        const attachmentIdxList = attachments.map(a => a.attachmentIdx);
        const linkResponse = await fetch(
          `/api/boards/link-attachments/${boardIdx}`,
          {
            method: 'POST',
            headers: {
              'Content-Type': 'application/json',
              'Authorization': `Bearer ${getToken()}`
            },
            body: JSON.stringify({ attachmentIdxList })
          }
        );
        
        if (!linkResponse.ok) throw new Error('첨부파일 연결 실패');
      }
      
      alert('게시글 작성 완료!');
      // 목록 페이지로 이동
      window.location.href = '/boards';
      
    } catch (error) {
      alert(`작성 실패: ${error.message}`);
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <form onSubmit={handleSubmit}>
      <input 
        type="text" 
        placeholder="제목"
        value={title}
        onChange={(e) => setTitle(e.target.value)}
        required
      />
      <textarea 
        placeholder="내용"
        value={content}
        onChange={(e) => setContent(e.target.value)}
        required
      />
      <input 
        type="file"
        multiple
        max="5"
        onChange={(e) => setFiles(Array.from(e.target.files))}
        accept=".pdf,.doc,.docx,.xls,.xlsx,.ppt,.pptx,.txt,.jpg,.jpeg,.png,.gif"
      />
      <p>{files.length}개 선택됨 (최대 5개)</p>
      <button type="submit" disabled={submitting}>
        {submitting ? '작성 중...' : '작성하기'}
      </button>
    </form>
  );
}
```

---

## ⚠️ 주의사항

### 🔴 필수 확인

1. **파일 업로드는 FormData만 가능**
   ```javascript
   // ❌ 이렇게 하면 안 됨
   body: JSON.stringify({ files: files })
   
   // ✅ 이렇게 해야 함
   const formData = new FormData();
   files.forEach(f => formData.append('files', f));
   body: formData
   ```

2. **Content-Type 헤더**
   - JSON 전송: `'Content-Type': 'application/json'`
   - FormData 전송: **헤더 생략** (자동 설정)

3. **다운로드는 브라우저가 자동 처리**
   - Content-Disposition 헤더로 구현됨
   - fetch → blob → createObjectURL → a.click()
   - 한글 파일명 지원

4. **3단계 순서 엄수**
   - 1단계: 게시글 생성 → boardIdx 획득
   - 2단계: 파일 업로드 → attachmentIdx 획득
   - 3단계: 첨부파일 연결 → 완료

---

## 📊 에러 코드

| 코드 | 메시지 | 해결 방법 |
|------|--------|-----------|
| 400 | "파일을 선택해주세요" | files 필드 확인 |
| 400 | "최대 5개까지만 업로드 가능합니다" | 파일 개수 제한 |
| 413 | "파일 크기가 10MB를 초과합니다" | 파일 크기 확인 |
| 404 | "게시글을 찾을 수 없습니다" | boardIdx 확인 |
| 410 | "삭제된 파일입니다" | 파일 존재 여부 확인 |

---

## 🔧 프론트엔드 구현 체크리스트

### 게시글 작성 페이지
- [ ] 제목/내용 입력 폼
- [ ] 파일 선택 input (multiple, max 5개)
- [ ] 파일 크기/개수 클라이언트 검증
- [ ] 3단계 API 순차 호출
- [ ] 로딩 상태 표시
- [ ] 에러 처리 및 피드백

### 게시글 상세 페이지
- [ ] 첨부파일 목록 표시 (attachmentDetails 배열)
- [ ] 파일명, 크기 표시
- [ ] 다운로드 버튼
- [ ] 다운로드 클릭 시 blob 처리

---

## 📞 문의

**백엔드 팀**: 성태준  
**상태**: ✅ 즉시 연동 가능  
**API 버전**: v2.0

---

**최종 업데이트**: 2025년 10월 21일
