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
  const { isAuthenticated } = UseAdmin();

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
  const boardReg = date.replace(" ", "T"); // "2025-09-26T15:43:21"

  const NoticeByAdmin = {
    boardTitle,
    boardCode: Number(boardCode),
    boardContent,
    boardReg,
    boardOn : 1
  };

  try {
    const response = await fetch('https://bluecrab.chickenkiller.com/BlueCrab-1.0.0/api/boards/create', {
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
    alert('공지사항이 성공적으로 등록되었습니다!');
    setBoardTitle('');
    setBoardCode(null);
    editorRef.current.getInstance().setMarkdown('');
    setCurrentPage(
        boardCode === 0 ? '학사공지' :
        boardCode === 1 ? '행정공지' :
        boardCode === 2 ? '기타공지' : ''
    )
  } catch (error) {
    alert(error.message);
  }
};


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
  const boardLast = date.replace(" ", "T"); // "2025-09-26T15:43:21"

  const updatedNotice = {
    boardTitle,
    boardCode: Number(boardCode),
    boardContent,
    boardLast
  };

  try {
    const response = await fetch(`https://bluecrab.chickenkiller.com/BlueCrab-1.0.0/api/boards/update/${boardIdx}`, {
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

    const result = await response.json();
    alert('공지사항이 성공적으로 수정되었습니다!');
    setBoardTitle('');
    setBoardCode(null);
    editorRef.current.getInstance().setMarkdown('');
    setCurrentPage(
        boardCode === 0 ? '학사공지' :
        boardCode === 1 ? '행정공지' :
        boardCode === 2 ? '기타공지' : ''
    )
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