import React, { useRef, useState } from 'react';
import { Editor } from '@toast-ui/react-editor';
import '@toast-ui/editor/dist/toastui-editor.css';
import '@toast-ui/editor/dist/i18n/ko-kr';

function AdminNoticeWritingPage() {
  const editorRef = useRef();
  const [BOARD_TITLE, setBOARD_TITLE] = useState('');
  const [BOARD_CODE, setBOARD_CODE] = useState('');

const handleSubmit = async (e) => {
  e.preventDefault();

  const BOARD_POST = editorRef.current.getInstance().getMarkdown();
  if (!BOARD_TITLE || !BOARD_CODE || !BOARD_POST.trim()) {
    alert('모든 필드를 입력해주세요.');
    return;
  }

  const BOARD_DATE = new Date().toISOString();

  const NoticeByAdmin = {
    BOARD_TITLE,
    BOARD_CODE,
    BOARD_POST,
    BOARD_DATE
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
    setBOARD_TITLE('');
    setBOARD_CODE('');
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
          value={BOARD_TITLE}
          onChange={(e) => setBOARD_TITLE(e.target.value)}
          required
          style={{ width: '100%', padding: '8px', marginBottom: '16px' }}
        />
      </div>

      <div>
        <label>카테고리</label><br />
        <select
          value={BOARD_CODE}
          onChange={(e) => setBOARD_CODE(e.target.value)}
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
          height="600px"
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