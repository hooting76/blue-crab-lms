import React, { useRef, useState, useEffect } from 'react';
import { Editor } from '@toast-ui/react-editor';
import '@toast-ui/editor/dist/toastui-editor.css';
import '@toast-ui/editor/dist/i18n/ko-kr';
import { UseUser } from '../../../hook/UseUser';
import ClassAttendingNotice from './ClassAttendingNotice';

function ProfNoticeWritingPage({ notice, accessToken: propToken, currentPage, setCurrentPage }) {

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
  const [boardCode, setBoardCode] = useState(3);
  const [selectedLectureId, setSelectedLectureId] = useState('');
  const [existingAttachments, setExistingAttachments] = useState([]);
  const [deletedAttachments, setDeletedAttachments] = useState([]);
  const [selectedFiles, setSelectedFiles] = useState([]);
  const [boardIdx, setBoardIdx] = useState(notice?.boardIdx || null); // 🔧 boardIdx 상태 추가
  const [lectureList, setLectureList] = useState([]);
  const BASE_URL = 'https://bluecrab.chickenkiller.com/BlueCrab-1.0.0/api';

  const { isAuthenticated, user, isUserAuth } = UseUser();

  const getAccessToken = () => {
    const storedToken = localStorage.getItem('accessToken');
    if (storedToken) return storedToken;
    if (isUserAuth && user?.data?.accessToken) return user.data.accessToken;
    return null;
  };

  const accessToken = propToken || getAccessToken();


  const fetchLectureList = async (accessToken, user) => {
    try {

        const requestBody = {
            page: 0,
            size: 20,
            professor: String(user.data.user.id)
        };

        const response = await fetch(`${BASE_URL}/lectures`, {
            method: "POST",
            headers: {
                'Authorization': `Bearer ${accessToken}`,
                'Content-Type': 'application/json'
            },
            body: JSON.stringify(requestBody)
        });
        console.log("Request body:", requestBody);

        if (!response.ok) throw new Error('강의 목록을 불러오는 데 실패했습니다.');

        const data = await response.json();
        setLectureList(data); // ✅ 받아온 데이터 저장
    } catch (error) {
        console.error('강의 목록 조회 에러:', error);
    }
};

useEffect(() => {
  fetchLectureList(accessToken, user);
}, [accessToken, user]); // ✅ accessToken이 생겼을 때 호출


  // 🔧 boardIdx가 바뀔 때 첨부파일 불러오기
useEffect(() => {
  const fetchAttachments = async () => {
    if (!boardIdx) {
      setExistingAttachments([]);
      return;
    }

    try {
      const attListRes = await fetch(`https://bluecrab.chickenkiller.com/BlueCrab-1.0.0/api/boards/detail`, {
        method: 'POST',
        headers: {
          'Authorization': `Bearer ${accessToken}`,
          'Content-Type': 'application/json'
        },
        body: JSON.stringify({ boardIdx })
      });

      const attList = await attListRes.json();
      console.log("📦 board detail 응답:", attList);

      if (attListRes.ok) {
        // 확인: attachments가 어디에 있는지 로그로 체크
        setExistingAttachments(attList.attachmentDetails || []);

      } else {
        console.error("❌ 첨부파일 요청 실패:", attListRes.status);
        setExistingAttachments([]);
      }
    } catch (e) {
      console.error("🚨 첨부파일 요청 중 예외 발생:", e);
      setExistingAttachments([]);
    }
  };

  fetchAttachments();
}, [boardIdx, accessToken]);



  useEffect(() => {
    if (notice?.boardTitle) {
      setBoardTitle(decodeBase64(notice.boardTitle));
    }
   // if (typeof notice?.boardContent === 'string' && editorRef.current) {
    //  editorRef.current.getInstance().setMarkdown(decodeBase64(notice.boardContent));
   // }
    if (typeof notice?.boardCode === 'number') {
      setBoardCode(notice.boardCode);
    }
    if (notice?.boardIdx) {
      setBoardIdx(notice.boardIdx); // 🔧 초기 진입 시 boardIdx 설정
    }
  }, [notice]);

  if (!isAuthenticated) {
    return <p>교수 인증 정보를 불러오는 중입니다...</p>;
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

    const NoticeByProf = {
      boardTitle,
      boardCode: 3,
      boardContent,
      boardWriterIdx: String(user.data.user.id),
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
        body: JSON.stringify(NoticeByProf),
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
        const attachmentIdxs = Array.isArray(uploadResult.attachments)
          ? uploadResult.attachments.map(att => att.attachmentIdx)
          : [];
          console.log("📂 uploadResult:", uploadResult);



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
      setTimeout(() => {
        setCurrentPage("수강/강의과목 공지사항");
      }, 100);
      setBoardTitle('');
      setBoardCode(null);
      setSelectedFiles([]);
      editorRef.current.getInstance().setMarkdown('');
      console.log("currentPage : ", currentPage);
    } catch (error) {
      alert(error.message);
    }
  };


// 공지 수정
  const handleEdit = async (e) => {
    e.preventDefault();

    if (!boardIdx) return alert("수정할 게시물 ID가 없습니다.");

    const boardContent = editorRef.current.getInstance().getMarkdown();
    if (!boardTitle || !boardContent.trim()) {
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
      boardCode: 3,
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
        const attachmentIdxs = Array.isArray(uploadResult.attachments)
          ? uploadResult.attachments.map(att => att.attachmentIdx)
          : [];
          console.log("📂 uploadResult:", uploadResult);


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
      setTimeout(() => {
        setCurrentPage("수강/강의과목 공지사항");
      }, 100);
      setBoardTitle('');
      setBoardCode(null);
      setSelectedFiles([]);
      setExistingAttachments([]);
      setDeletedAttachments([]);
      editorRef.current.getInstance().setMarkdown('');
      console.log("currentPage : ", currentPage);
    } catch (error) {
      alert(error.message);
    }
  };



  if (currentPage === "수강/강의과목 공지사항")
    return <ClassAttendingNotice currentPage={currentPage} setCurrentPage={setCurrentPage} />;

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
        <label>과목</label><br />
        <select value={selectedLectureId} onChange={(e) => setSelectedLectureId(e.target.value)}>
            {lectureList.length > 0 ? (
                lectureList.map((cls) => (
                    <option key={cls.lecIdx} value={cls.lecIdx}>
                        {cls.lecTit}
                    </option>
                  ))
                ) : (
                    <option disabled>강의 목록 없음</option>
                )}
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
              {att.originalFileName} ({(att.fileSize / 1024).toFixed(1)} KB)
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
              {file.originalFileName} ({(file.size / 1024).toFixed(1)} KB)
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

export default ProfNoticeWritingPage;
