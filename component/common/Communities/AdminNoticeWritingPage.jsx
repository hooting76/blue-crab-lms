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
  const [existingAttachments, setExistingAttachments] = useState([]); // 기존 첨부파일
  const [deletedAttachments, setDeletedAttachments] = useState([]); // 삭제 예정 첨부파일
  const [selectedFiles, setSelectedFiles] = useState([]); // 새로 선택한 파일
  const { isAuthenticated, admin, isAdminAuth } = UseAdmin();

  const getAccessToken = () => {
    const storedToken = localStorage.getItem('accessToken');
    if (storedToken) return storedToken;

    if (isAdminAuth && admin?.data?.accessToken) return admin.data.accessToken;

    return null;
  };

  const accessToken = propToken || getAccessToken();

  // notice 변경 시 기존 첨부파일 목록을 서버에서 재조회
  useEffect(() => {
    const fetchAttachments = async () => {
      if (notice?.boardIdx) {
        try {
          const attListRes = await fetch(`https://bluecrab.chickenkiller.com/BlueCrab-1.0.0/api/board-attachments/${notice.boardIdx}`, {
            method: 'GET',
            headers: {
              'Authorization': `Bearer ${accessToken}`
            }
          });
          if (attListRes.ok) {
            const attList = await attListRes.json();
            setExistingAttachments(attList.attachments || []);
            return;
          }
        } catch (e) {
          setExistingAttachments([]);
        }
      }
      // boardIdx 없으면 기존 notice 객체의 attachments 사용
      if (notice?.attachments) {
        setExistingAttachments(notice.attachments);
      } else {
        setExistingAttachments([]);
      }
    };
    fetchAttachments();
  }, [notice, accessToken]);

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

  // 파일 선택 핸들러 (최대 5개 제한)
  // 파일 input 변경 핸들러
  const handleFileChange = (e) => {
    const files = Array.from(e.target.files);

    // 최대 5개 제한 검사
    if (existingAttachments.length + selectedFiles.length + files.length > 5) {
      alert('첨부파일은 최대 5개까지 첨부할 수 있습니다.');
      return;
    }

    setSelectedFiles(prev => [...prev, ...files]);
  };

  // 기존 첨부파일 삭제 버튼 클릭
  const handleDeleteExistingAttachment = (attachmentIdx) => {
    setExistingAttachments(prev => prev.filter(att => att.attachmentIdx !== attachmentIdx));
    setDeletedAttachments(prev => [...prev, attachmentIdx]);
  };

  // 새로 선택한 파일 삭제 버튼 클릭
  const handleDeleteSelectedFile = (fileName) => {
    setSelectedFiles(prev => prev.filter(file => file.name !== fileName));
  };

console.log('업로드할 파일 목록:', selectedFiles);


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
      boardOn : 1
    };

    try {
      // 1단계: 게시글 생성
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
      const boardIdx = result.boardIdx;

      // 2단계: 파일 첨부 업로드
      if (selectedFiles.length > 0) {
        const formData = new FormData();
        selectedFiles.forEach(file => formData.append('files', file));
        const uploadResponse = await fetch(`https://bluecrab.chickenkiller.com/BlueCrab-1.0.0/api/board-attachments/upload/${boardIdx}`, {
          method: 'POST',
          headers: {
            'Authorization': `Bearer ${accessToken}`
            // Content-Type 자동으로 multipart/form-data가 설정됨
          },
          body: formData,
        });

        if (!uploadResponse.ok) {
          throw new Error('파일 업로드 중 에러가 발생했습니다.');
        }

        const uploadResult = await uploadResponse.json();
        const attachmentIdxs = uploadResult.attachments.map(att => att.attachmentIdx);

        // 3단계: 첨부파일 연결
        const linkResponse = await fetch(`https://bluecrab.chickenkiller.com/BlueCrab-1.0.0/api/boards/link-attachments/${boardIdx}`, {
          method: 'POST',
          headers: {
            'Content-Type': 'application/json',
            'Authorization': `Bearer ${accessToken}`
          },
          body: JSON.stringify({ attachmentIdx: attachmentIdxs }),
        });

        if (!linkResponse.ok) {
          throw new Error('첨부파일 연결 중 에러가 발생했습니다.');
        }

        await linkResponse.json();
      }

      // 첨부파일 목록을 서버에서 재조회
      try {
        const attListRes = await fetch(`https://bluecrab.chickenkiller.com/BlueCrab-1.0.0/api/board-attachments/${boardIdx}`, {
          method: 'GET',
          headers: {
            'Authorization': `Bearer ${accessToken}`
          }
        });
        if (attListRes.ok) {
          const attList = await attListRes.json();
          setExistingAttachments(attList.attachments || []);
        }
      } catch (e) {
        setExistingAttachments([]);
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

    const boardIdx = notice?.boardIdx;
    console.log('수정할 공지사항 IDX:', boardIdx);

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

      if (!response.ok) {
        throw new Error('서버 에러가 발생했습니다.');
      }

      if (selectedFiles.length > 0) {
        const formData = new FormData();
        selectedFiles.forEach(file => formData.append('files', file));

        const uploadResponse = await fetch(`https://bluecrab.chickenkiller.com/BlueCrab-1.0.0/api/board-attachments/upload/${boardIdx}`, {
          method: 'POST',
          headers: {
            'Authorization': `Bearer ${accessToken}`
          },
          body: formData,
        });

        if (!uploadResponse.ok) {
          throw new Error('파일 업로드 중 에러가 발생했습니다.');
        }

        const uploadResult = await uploadResponse.json();
        const attachmentIdxs = uploadResult.attachments.map(att => att.attachmentIdx);

        // 첨부파일 연결 API 호출
        const linkResponse = await fetch(`https://bluecrab.chickenkiller.com/BlueCrab-1.0.0/api/boards/link-attachments/${boardIdx}`, {
          method: 'POST',
          headers: {
            'Content-Type': 'application/json',
            'Authorization': `Bearer ${accessToken}`
          },
          body: JSON.stringify({ attachmentIdx: attachmentIdxs }),
        });

        if (!linkResponse.ok) {
          throw new Error('첨부파일 연결 중 에러가 발생했습니다.');
        }

        await linkResponse.json();
      }

      // 첨부파일 목록을 서버에서 재조회
      try {
        const attListRes = await fetch(`https://bluecrab.chickenkiller.com/BlueCrab-1.0.0/api/board-attachments/${boardIdx}`, {
          method: 'GET',
          headers: {
            'Authorization': `Bearer ${accessToken}`
          }
        });
        if (attListRes.ok) {
          const attList = await attListRes.json();
          setExistingAttachments(attList.attachments || []);
        }
      } catch (e) {
        setExistingAttachments([]);
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
