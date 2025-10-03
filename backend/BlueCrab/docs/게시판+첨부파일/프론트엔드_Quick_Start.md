# 프론트엔드 Quick Start - 게시글 & 첨부파일 API

> **빠른 시작 가이드** - 5분 안에 API 연동 시작하기  
> 자세한 내용은 `프론트엔드_최신_연동_가이드_v2.md` 참조

---

## ⚡ 핵심 요약

### 📌 기본 정보
- **모든 API**: POST 메서드 사용
- **인증 필요**: 게시글 작성, 파일 업로드/삭제
- **인증 불필요**: 게시글 조회, 파일 다운로드

### 🔑 JWT 토큰 형식
```javascript
headers: {
  'Authorization': 'Bearer ' + 토큰
}
```

---

## 🚀 3단계 워크플로우 (게시글 + 첨부파일)

```javascript
// 1단계: 게시글 생성
const response1 = await fetch('/api/boards/create', {
  method: 'POST',
  headers: {
    'Content-Type': 'application/json',
    'Authorization': 'Bearer ' + JWT_TOKEN
  },
  body: JSON.stringify({
    boardCode: 0,
    boardTitle: "제목",
    boardContent: "내용",
    boardWriterIdx: 1,
    boardWriterType: 1
  })
});
const { boardIdx } = await response1.json();

// 2단계: 파일 업로드 (선택사항)
const formData = new FormData();
files.forEach(file => formData.append('files', file));

const response2 = await fetch(`/api/board-attachments/upload/${boardIdx}`, {
  method: 'POST',
  headers: { 'Authorization': 'Bearer ' + JWT_TOKEN },
  body: formData
});
const { attachments } = await response2.json();

// 3단계: 첨부파일 연결
const attachmentIds = attachments.map(a => a.attachmentIdx);
await fetch(`/api/boards/link-attachments/${boardIdx}`, {
  method: 'POST',
  headers: {
    'Content-Type': 'application/json',
    'Authorization': 'Bearer ' + JWT_TOKEN
  },
  body: JSON.stringify({ attachmentIdx: attachmentIds })
});
```

---

## 📖 게시글 조회

```javascript
// 목록 조회
const listResponse = await fetch('/api/boards/list', {
  method: 'POST',
  headers: { 'Content-Type': 'application/json' },
  body: JSON.stringify({ page: 0, size: 10 })
});
const { content, totalElements } = await listResponse.json();

// 상세 조회 (첨부파일 포함)
const detailResponse = await fetch('/api/boards/detail', {
  method: 'POST',
  headers: { 'Content-Type': 'application/json' },
  body: JSON.stringify({ boardIdx: 123 })
});
const board = await detailResponse.json();
// board.attachmentDetails 배열에 첨부파일 정보 포함
```

---

## 💾 파일 다운로드 (브라우저 자동 처리)

```javascript
// 방법 1: fetch + Blob
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

// 방법 2: Form Submit (간단)
function downloadFileSimple(attachmentIdx) {
  const form = document.createElement('form');
  form.method = 'POST';
  form.action = '/api/board-attachments/download';
  
  const input = document.createElement('input');
  input.type = 'hidden';
  input.name = 'attachmentIdx';
  input.value = attachmentIdx;
  
  form.appendChild(input);
  document.body.appendChild(form);
  form.submit();
  document.body.removeChild(form);
}
```

---

## 🗑️ 파일 삭제

```javascript
// 단일 또는 다중 삭제 (통합)
await fetch('/api/board-attachments/delete', {
  method: 'POST',
  headers: {
    'Content-Type': 'application/json',
    'Authorization': 'Bearer ' + JWT_TOKEN
  },
  body: JSON.stringify({ 
    attachmentIds: [45]  // 단일: 1개, 다중: 여러 개
  })
});

// 사용 예시
// 단일 삭제
deleteAttachments([45]);

// 다중 삭제
deleteAttachments([45, 46, 47]);

function deleteAttachments(ids) {
  return fetch('/api/board-attachments/delete', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      'Authorization': 'Bearer ' + getToken()
    },
    body: JSON.stringify({ attachmentIds: ids })
  });
}
```

---

## 📊 주요 API 목록

| 기능 | 엔드포인트 | 메서드 | 인증 |
|-----|-----------|--------|------|
| 게시글 목록 | `/api/boards/list` | POST | ❌ |
| 게시글 상세 | `/api/boards/detail` | POST | ❌ |
| 게시글 작성 | `/api/boards/create` | POST | ✅ |
| 파일 업로드 | `/api/board-attachments/upload/{boardIdx}` | POST | ✅ |
| 파일 연결 | `/api/boards/link-attachments/{boardIdx}` | POST | ✅ |
| 파일 다운로드 | `/api/board-attachments/download` | POST | ❌ |
| 파일 삭제 | `/api/board-attachments/delete` | POST | ✅ |
| 게시글별 파일 전체 삭제 | `/api/board-attachments/delete-all/{boardIdx}` | POST | ✅ |

---

## ⚠️ 제한사항

- **파일 개수**: 최대 5개/게시글
- **파일 크기**: 최대 10MB/파일
- **파일 형식**: PDF, DOC, DOCX, XLS, XLSX, PPT, PPTX, TXT, JPG, JPEG, PNG, GIF

---

## 🎯 React 컴포넌트 예제

```jsx
function BoardWrite() {
  const [files, setFiles] = useState([]);
  
  const handleSubmit = async (e) => {
    e.preventDefault();
    
    // 1. 게시글 생성
    const boardData = {
      boardCode: 0,
      boardTitle: e.target.title.value,
      boardContent: e.target.content.value,
      boardWriterIdx: 1,
      boardWriterType: 1
    };
    
    const res1 = await fetch('/api/boards/create', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Authorization': 'Bearer ' + getToken()
      },
      body: JSON.stringify(boardData)
    });
    const { boardIdx } = await res1.json();
    
    // 2. 파일 업로드
    if (files.length > 0) {
      const formData = new FormData();
      files.forEach(f => formData.append('files', f));
      
      const res2 = await fetch(`/api/board-attachments/upload/${boardIdx}`, {
        method: 'POST',
        headers: { 'Authorization': 'Bearer ' + getToken() },
        body: formData
      });
      const { attachments } = await res2.json();
      
      // 3. 첨부파일 연결
      const ids = attachments.map(a => a.attachmentIdx);
      await fetch(`/api/boards/link-attachments/${boardIdx}`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          'Authorization': 'Bearer ' + getToken()
        },
        body: JSON.stringify({ attachmentIdx: ids })
      });
    }
    
    alert('작성 완료!');
    navigate(`/board/${boardIdx}`);
  };
  
  return (
    <form onSubmit={handleSubmit}>
      <input name="title" placeholder="제목" />
      <textarea name="content" placeholder="내용" />
      <input 
        type="file" 
        multiple 
        onChange={e => setFiles(Array.from(e.target.files))}
        accept=".pdf,.doc,.docx,.jpg,.png"
      />
      <button type="submit">작성</button>
    </form>
  );
}

function BoardDetail({ boardIdx }) {
  const [board, setBoard] = useState(null);
  
  useEffect(() => {
    fetch('/api/boards/detail', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ boardIdx })
    })
    .then(res => res.json())
    .then(setBoard);
  }, [boardIdx]);
  
  if (!board) return <div>로딩...</div>;
  
  return (
    <div>
      <h1>{board.boardTitle}</h1>
      <div>{board.boardContent}</div>
      
      {board.attachmentDetails?.map(file => (
        <div key={file.attachmentIdx}>
          📎 {file.originalFileName}
          <button onClick={() => downloadFile(
            file.attachmentIdx, 
            file.originalFileName
          )}>
            다운로드
          </button>
        </div>
      ))}
    </div>
  );
}
```

---

## 📞 더 자세한 정보

👉 **상세 문서**: `프론트엔드_최신_연동_가이드_v2.md`  
👉 **API 응답 예제**: 위 문서의 각 API 섹션 참조  
👉 **에러 처리**: 위 문서의 "에러 처리 가이드" 참조

---

**문서 버전**: v2.0  
**최종 업데이트**: 2025년 10월 3일
