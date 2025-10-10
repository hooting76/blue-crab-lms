import React, { useRef, useState, useEffect } from 'react';
import { Editor } from '@toast-ui/react-editor';
import '@toast-ui/editor/dist/toastui-editor.css';
import '@toast-ui/editor/dist/i18n/ko-kr';
import { UseAdmin } from '../../../hook/UseAdmin';
import AcademyNotice from './AcademyNotice';
import AdminNotice from './AdminNotice';
import EtcNotice from './EtcNotice';


function AdminNoticeWritingPage({ notice, accessToken: propToken, currentPage, setCurrentPage }) {

function decodeBase64(str) {
  if (typeof str !== 'string' || str.trim() === '') {
    console.warn("Base64 디코딩 대상이 없음 또는 잘못된 입력:", str);
    return '';
  }

  try {
    return decodeURIComponent(escape(atob(str)));
  } catch (e) {
    console.error("Base64 디코딩 오류:", e);
    return str;
  }
}


  const editorRef = useRef();
  const [boardTitle, setBoardTitle] = useState('');
  const [boardCode, setBoardCode] = useState(null);
  const { isAuthenticated, admin, isAdminAuth } = UseAdmin();

  const getAccessToken = () => {
    const storedToken = localStorage.getItem('accessToken');
    if (storedToken) return storedToken;

    if (isAdminAuth && admin?.data?.accessToken) return admin.data.accessToken;

    return null;
  };

  const accessToken = propToken || getAccessToken();  // ✅ props가 없으면 직접 가져오기


useEffect(() => {
  if (notice?.boardTitle) {
    setBoardTitle(decodeBase64(notice.boardTitle));
  }
  if (typeof notice?.boardContent === 'string' && editorRef.current) {
    editorRef.current.getInstance().setMarkdown(decodeBase64(notice.boardContent));
  }
  if (typeof notice?.boardCode === 'number') {
    setBoardCode(notice.boardCode);
  }
}, [notice]);



 if (!isAuthenticated) {
  return <p>관리자 인증 정보를 불러오는 중입니다...</p>;
}

const [selectedFiles, setSelectedFiles] = useState([]);

const handleFileChange = (e) => {
  const files = Array.from(e.target.files);
  const totalFiles = selectedFiles.length + files.length;

  if (totalFiles > 5) {
    alert("파일은 최대 5개까지 업로드할 수 있습니다.");
    return;
  }

  setSelectedFiles(prev => [...prev, ...files]);
};


const linkAttachmentsToBoard = async (boardIdx, attachmentIdxArray) => {
  const response = await fetch(`/api/boards/link-attachments/${boardIdx}`, {
    method: "POST",
    headers: {
      "Authorization": `Bearer ${accessToken}`,
      "Content-Type": "application/json"
    },
    body: JSON.stringify({ attachmentIdx: attachmentIdxArray })
  });

  const result = await response.json();
  if (!result.success) {
    throw new Error("첨부파일 연결 실패");
  }
};

const uploadFiles = async (boardIdx, files) => {
  const formData = new FormData();
  files.forEach((file) => {
    formData.append("files", file);
  });

  const response = await fetch(`/api/board-attachments/upload/${boardIdx}`, {
    method: "POST",
    headers: {
      "Authorization": `Bearer ${accessToken}`,
    },
    body: formData
  });

  const result = await response.json();
  if (result.success) {
    return result.data.map(file => file.attachmentIdx);
  } else {
    throw new Error("파일 업로드 실패");
  }
};


// 공지 작성
const handleSubmit = async (e) => {
  e.preventDefault();

  const boardContent = editorRef.current.getInstance().getMarkdown();
  if (!boardTitle || boardCode === null || !boardContent.trim()) {
    alert('모든 필드를 입력해주세요.');
    return;
  }

  const date = new Date().toLocaleString("sv-SE", {
    timeZone: "Asia/Seoul",
    hour12: false,
  });
  const boardReg = date.replace(" ", "T");

  const NoticeByAdmin = {
    boardWriterIdx,
    boardWriterType,
    boardTitle,
    boardCode: Number(boardCode),
    boardContent,
    boardReg,
    boardOn: 1
  };

  try {
    // 게시글 생성
    const response = await fetch('/api/boards/create', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${accessToken}`
      },
      body: JSON.stringify(NoticeByAdmin),
    });

    if (!response.ok) {
      throw new Error('서버 에러가 발생했습니다.');
    }

    const result = await response.json();
    const boardIdx = result?.data?.boardIdx;

    // ✅ 파일이 있으면 업로드 후 첨부 연결
    if (selectedFiles.length > 0 && boardIdx) {
      const attachmentIdxArray = await uploadFiles(boardIdx, selectedFiles);
      await linkAttachmentsToBoard(boardIdx, attachmentIdxArray);
    }

    alert('공지사항이 성공적으로 등록되었습니다!');
    setBoardTitle('');
    setBoardCode(null);
    editorRef.current.getInstance().setMarkdown('');
    setSelectedFiles([]);

    setCurrentPage(
      boardCode === 0 ? '학사공지' :
      boardCode === 1 ? '행정공지' :
      boardCode === 2 ? '기타공지' : ''
    );
  } catch (error) {
    alert(error.message);
  }
};

console.log('fetch 시작 - 수정 요청');
console.log('URL:', `/api/boards/update/${boardIdx}`);
console.log('Body:', JSON.stringify(updatedNotice));


// 공지 수정
const handleEdit = async (e) => {
  e.preventDefault();

  const boardIdx = notice?.boardIdx;
  const boardContent = editorRef.current.getInstance().getMarkdown();

  if (!boardTitle || boardCode === null || !boardContent.trim()) {
    alert('모든 필드를 입력해주세요.');
    return;
  }

  const date = new Date().toLocaleString("sv-SE", {
    timeZone: "Asia/Seoul",
    hour12: false,
  });
  const boardLast = date.replace(" ", "T");

  const updatedNotice = {
    boardTitle,
    boardCode: Number(boardCode),
    boardContent,
    boardLast
  };

  try {
    const response = await fetch(`/api/boards/update/${boardIdx}`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${accessToken}`
      },
      body: JSON.stringify(updatedNotice),
    });

    if (!response.ok) {
      throw new Error('서버 에러가 발생했습니다.');
    }

    // ✅ 수정 중 파일 업로드 + 연결
    if (selectedFiles.length > 0 && boardIdx) {
      const attachmentIdxArray = await uploadFiles(boardIdx, selectedFiles);
      await linkAttachmentsToBoard(boardIdx, attachmentIdxArray);
    }

    alert('공지사항이 성공적으로 수정되었습니다!');
    setBoardTitle('');
    setBoardCode(null);
    editorRef.current.getInstance().setMarkdown('');
    setSelectedFiles([]);

    setCurrentPage(
      boardCode === 0 ? '학사공지' :
      boardCode === 1 ? '행정공지' :
      boardCode === 2 ? '기타공지' : ''
    );
  } catch (error) {
    alert(error.message);
  }
};


// 작성 또는 수정 후 목록으로 돌아가기
if (currentPage === "학사공지")
    return <AcademyNotice currentPage={currentPage} setCurrentPage={setCurrentPage} />;
if (currentPage === "행정공지")
    return <AdminNotice currentPage={currentPage} setCurrentPage={setCurrentPage} />;
if (currentPage === "기타공지")
    return <EtcNotice currentPage={currentPage} setCurrentPage={setCurrentPage} />;


  return (
    <form>
      <div>
        <label>제목</label><br />
        <input
          type="text"
          value={boardTitle}
          onChange={(e) => setBoardTitle(e.target.value)}
          required
          style={{ width: '100%', padding: '8px', marginBottom: '16px' }}
        />
      </div>

      <div>
        <label>카테고리</label><br />
        <select
          value={boardCode}
          onChange={(e) => setBoardCode(Number(e.target.value))}
          required
          style={{ width: '100%', padding: '8px', marginBottom: '16px' }}
        >
          <option value={null}>카테고리를 선택하세요</option>
          <option value={0}>학사공지</option>
          <option value={1}>행정공지</option>
          <option value={2}>기타공지</option>
        </select>
      </div>

      <input
        type="file"
        multiple
        accept=".jpg,.jpeg,.png,.pdf"
        onChange={handleFileChange}
      />

      <div>
        <label>본문</label>
        <Editor
          ref={editorRef}
          previewStyle="vertical"
          height="300px"
          initialEditType="wysiwyg"
          initialValue={
            typeof notice?.boardContent === 'string'
              ? decodeBase64(notice.boardContent)
              : ''
          }
          useCommandShortcut={true}
          language="ko-KR"
        />
      </div>

      {notice ? 
        (<button type="button" onClick={handleEdit} style={{ marginTop: '20px', padding: '10px 20px' }}>
          수정하기
        </button>)
       : 
      (<button type="button" onClick={handleSubmit} style={{ marginTop: '20px', padding: '10px 20px' }}>
        게시하기
      </button>)}
    </form>
  );
}

export default AdminNoticeWritingPage;