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
    if (typeof str !== 'string' || str.trim() === '') return '';
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
  const [existingAttachments, setExistingAttachments] = useState([]);
  const [deletedAttachments, setDeletedAttachments] = useState([]);
  const [selectedFiles, setSelectedFiles] = useState([]);
  const [boardIdx, setBoardIdx] = useState(notice?.boardIdx || null); // 🔧 boardIdx 상태 추가

  const { isAuthenticated, admin, isAdminAuth } = UseAdmin();

  const getAccessToken = () => {
    const storedToken = localStorage.getItem('accessToken');
    if (storedToken) return storedToken;
    if (isAdminAuth && admin?.data?.accessToken) return admin.data.accessToken;
    return null;
  };

  const accessToken = propToken || getAccessToken();

  // 🔧 boardIdx가 바뀔 때 첨부파일 불러오기
  useEffect(() => {
    const fetchAttachments = async () => {
      if (!boardIdx) {
        setExistingAttachments([]);
        return;
      }

      try {
        const attListRes = await fetch(`https://bluecrab.chickenkiller.com/BlueCrab-1.0.0/api/boards/detail/${boardIdx}`, {
          method: 'POST',
          headers: {
            'Authorization': `Bearer ${accessToken}`
          }
        });
        if (attListRes.ok) {
          const attList = await attListRes.json();
          setExistingAttachments(attList.attachments || []);
        } else {
          setExistingAttachments([]);
        }
      } catch (e) {
        setExistingAttachments([]);
      }
    };

    fetchAttachments();
  }, [boardIdx, accessToken]); // 🔧 notice 대신 boardIdx를 의존성에 추가

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
    if (notice?.boardIdx) {
      setBoardIdx(notice.boardIdx); // 🔧 초기 진입 시 boardIdx 설정
    }
  }, [notice]);

  if (!isAuthenticated) {
    return <p>관리자 인증 정보를 불러오는 중입니다...</p>;
  }

  const handleFileChange = (e) => {
    const files = Array.from(e.target.files);
    if (existingAttachments.length + selectedFiles.length + files.length > 5) {
      alert('첨부파일은 최대 5개까지 첨부할 수 있습니다.');
      return;
    }
    setSelectedFiles(prev => [...prev, ...files]);
  };

  const handleDeleteExistingAttachment = (attachmentIdx) => {
    setExistingAttachments(prev => prev.filter(att => att.attachmentIdx !== attachmentIdx));
    setDeletedAttachments(prev => [...prev, attachmentIdx]);
  };

  const handleDeleteSelectedFile = (fileName) => {
    setSelectedFiles(prev => prev.filter(file => file.name !== fileName));
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
      boardTitle,
      boardCode: Number(boardCode),
      boardContent,
      boardWriterIdx: admin?.data?.adminIdx,
      boardReg,
      boardOn: 1
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

      if (!response.ok) throw new Error('서버 에러가 발생했습니다.');

      const result = await response.json();
      const createdIdx = result.boardIdx;
      setBoardIdx(createdIdx); // 🔧 게시글 생성 후 boardIdx 저장

      // 파일 업로드
      if (selectedFiles.length > 0) {
        const formData = new FormData();
        selectedFiles.forEach(file => formData.append('files', file));
        const uploadResponse = await fetch(`https://bluecrab.chickenkiller.com/BlueCrab-1.0.0/api/board-attachments/upload/${createdIdx}`, {
          method: 'POST',
          headers: { 'Authorization': `Bearer ${accessToken}` },
          body: formData,
        });

        if (!uploadResponse.ok) throw new Error('파일 업로드 중 에러가 발생했습니다.');

        const uploadResult = await uploadResponse.json();
        const attachmentIdxs = uploadResult.attachments.map(att => att.attachmentIdx);

        await fetch(`https://bluecrab.chickenkiller.com/BlueCrab-1.0.0/api/boards/link-attachments/${createdIdx}`, {
          method: 'POST',
          headers: {
            'Content-Type': 'application/json',
            'Authorization': `Bearer ${accessToken}`
          },
          body: JSON.stringify({ attachmentIdx: attachmentIdxs }),
        });
      }

      alert('공지사항이 성공적으로 등록되었습니다!');
      setBoardTitle('');
      setBoardCode(null);
      setSelectedFiles([]);
      editorRef.current.getInstance().setMarkdown('');
      setCurrentPage(
        boardCode === 0 ? '학사공지' :
        boardCode === 1 ? '행정공지' :
        boardCode === 2 ? '기타공지' : ''
      );
    } catch (error) {
      alert(error.message);
    }
  };


// 공지 수정
  const handleEdit = async (e) => {
    e.preventDefault();

    if (!boardIdx) return alert("수정할 게시물 ID가 없습니다.");

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
      const response = await fetch(`https://bluecrab.chickenkiller.com/BlueCrab-1.0.0/api/boards/update/${boardIdx}`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          'Authorization': `Bearer ${accessToken}`
        },
        body: JSON.stringify(updatedNotice),
      });

      if (!response.ok) throw new Error('서버 에러가 발생했습니다.');

      if (selectedFiles.length > 0) {
        const formData = new FormData();
        selectedFiles.forEach(file => formData.append('files', file));

        const uploadResponse = await fetch(`https://bluecrab.chickenkiller.com/BlueCrab-1.0.0/api/board-attachments/upload/${boardIdx}`, {
          method: 'POST',
          headers: { 'Authorization': `Bearer ${accessToken}` },
          body: formData,
        });

        if (!uploadResponse.ok) throw new Error('파일 업로드 중 에러가 발생했습니다.');

        const uploadResult = await uploadResponse.json();
        const attachmentIdxs = uploadResult.attachments.map(att => att.attachmentIdx);

        await fetch(`https://bluecrab.chickenkiller.com/BlueCrab-1.0.0/api/boards/link-attachments/${boardIdx}`, {
          method: 'POST',
          headers: {
            'Content-Type': 'application/json',
            'Authorization': `Bearer ${accessToken}`
          },
          body: JSON.stringify({ attachmentIdx: attachmentIdxs }),
        });
      }

      alert('공지사항이 성공적으로 수정되었습니다!');
      setBoardTitle('');
      setBoardCode(null);
      setSelectedFiles([]);
      setExistingAttachments([]);
      setDeletedAttachments([]);
      editorRef.current.getInstance().setMarkdown('');
      setCurrentPage(
        boardCode === 0 ? '학사공지' :
        boardCode === 1 ? '행정공지' :
        boardCode === 2 ? '기타공지' : ''
      );
    } catch (error) {
      alert(error.message);
    }
  };

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
          initialValue={notice?.boardContent ? decodeBase64(notice.boardContent) : ''}
          useCommandShortcut={true}
          language="ko-KR"
        />
      </div>

      <div>
        <label>첨부파일 (최대 5개)</label><br />
        <input
          type="file"
          multiple
          onChange={handleFileChange}
          accept="*"
        />
      </div>

      <div>
        <h4>기존 첨부파일</h4>
        <ul>
          {existingAttachments.map(att => (
            <li key={att.attachmentIdx}>
              {att.fileName} ({(att.fileSize / 1024).toFixed(1)} KB)
              <button type="button" onClick={() => handleDeleteExistingAttachment(att.attachmentIdx)}>삭제</button>
            </li>
          ))}
        </ul>
      </div>

      <div>
        <h4>선택한 새 파일</h4>
        <ul>
          {selectedFiles.map(file => (
            <li key={file.name}>
              {file.name} ({(file.size / 1024).toFixed(1)} KB)
              <button type="button" onClick={() => handleDeleteSelectedFile(file.name)}>삭제</button>
            </li>
          ))}
        </ul>
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
