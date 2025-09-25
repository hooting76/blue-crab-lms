import React, { useRef, useState } from 'react';
import { Editor } from '@toast-ui/react-editor';
import '@toast-ui/editor/dist/toastui-editor.css';
import '@toast-ui/editor/dist/i18n/ko-kr';
import { UseUser } from '../../../hook/UseUser';

function AdminNoticeWritingPage() {
  const editorRef = useRef();
  const [boardTitle, setBoardTitle] = useState('');
  const [boardCode, setBoardCode] = useState('');

  const {user} = UseUser();

const handleSubmit = async (e) => {
  e.preventDefault();

  const boardContent = editorRef.current.getInstance().getMarkdown();
  if (!boardTitle || !boardCode || !boardContent.trim()) {
    alert('모든 필드를 입력해주세요.');
    return;
  }

  const boardWriter = user.data.user.name;
  const boardReg = new Date().toISOString();

  const NoticeByAdmin = {
    boardTitle,
    boardCode,
    boardContent,
    boardWriter,
    boardReg
  };

  try {
    const response = await fetch('https://bluecrab.chickenkiller.com/Bluecrab-1.0.0/api/boards/create', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify(NoticeByAdmin),
    });

    if (!response.ok) {
      throw new Error('서버 에러가 발생했습니다.');
    }

    const result = await response.json();
    alert('공지사항이 성공적으로 등록되었습니다!');
    setBoardTitle('');
    setBoardCode('');
    editorRef.current.getInstance().setMarkdown('');
  } catch (error) {
    alert(error.message);
  }
};


  return (
    <form onSubmit={handleSubmit}>
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
          onChange={(e) => setBoardCode(e.target.value)}
          required
          style={{ width: '100%', padding: '8px', marginBottom: '16px' }}
        >
          <option value="">카테고리를 선택하세요</option>
          <option value="0">학사공지</option>
          <option value="1">행정공지</option>
          <option value="2">기타공지</option>
        </select>
      </div>

      <div>
        <label>본문</label>
        <Editor
          ref={editorRef}
          previewStyle="vertical"
          height="300px"
          initialEditType="wysiwyg"
          useCommandShortcut={true}
          language="ko-KR"
        />
      </div>

      <button type="submit" style={{ marginTop: '20px', padding: '10px 20px' }}>
        게시하기
      </button>
    </form>
  );
}

export default AdminNoticeWritingPage;