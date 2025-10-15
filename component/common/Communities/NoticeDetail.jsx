import "../../../css/Communities/NoticeDetail.css"
import React, { useEffect, useState } from 'react';
import { getNoticeDetail, deleteNotice } from '../../api/noticeAPI';
import { UseUser } from "../../../hook/UseUser";
import { UseAdmin } from "../../../hook/UseAdmin";
import { Viewer } from '@toast-ui/react-editor';
import '@toast-ui/editor/dist/toastui-editor-viewer.css';


const NoticeDetail = ({ boardIdx, onFetchComplete }) => {
  const [notice, setNotice] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

   // 사용자 컨텍스트
    const userContext = UseUser();
    const { user, isAuthenticated: isUserAuth } = userContext || { user: null, isAuthenticated: false };

    // 관리자 컨텍스트
    const adminContext = UseAdmin() || { admin: null, isAuthenticated: false };
    const { admin, isAuthenticated: isAdminAuth } = adminContext;

    // Admin 또는 User의 accessToken 가져오기
    const getAccessToken = () => {
        // 로컬스토리지에서 먼저 확인 (가장 최신 토큰)
        const storedToken = localStorage.getItem('accessToken');
        if (storedToken) return storedToken;

        // Admin 토큰 확인
        if (isAdminAuth && admin.accessToken) {
            return admin.accessToken;
        }

        // User 토큰 확인
        if (isUserAuth && user?.data?.accessToken) {
            return user.data.accessToken;
        }

        return null;
    };

    const accessToken = getAccessToken();


  // boardCode에 따른 공지 종류 반환
  const getNoticeCode = (boardCode) => {
    switch (boardCode) {
      case 0:
        return "학사공지";
      case 1:
        return "행정공지";
      case 2:
        return "기타공지";
      default:
        return "공지";
    }
  };

  const formattedReg = (boardReg) => {
    return boardReg.replace('T', ' ').slice(0, 16);
  }

  const formattedLatest = (boardLast, boardReg) => {
    return boardLast ? boardLast.replace('T', ' ').slice(0, 16) : formattedReg(boardReg);
  }

  useEffect(() => {
    const fetchData = async () => {
      try {
        const data = await getNoticeDetail(accessToken, boardIdx);
        console.log("✅ 공지 fetch 완료:", data);
        setNotice(data);
        onFetchComplete?.(data);
      } catch (err) {
        setError(err.message || '데이터를 불러오는데 실패했습니다.');
      } finally {
        setLoading(false);
      }
    };

    if (accessToken && boardIdx) {
      fetchData();
    }
  }, [accessToken, boardIdx]);



  

  if (loading) return <div>불러오는 중...</div>;
  if (error) return <div>오류: {error}</div>;
  if (!notice) return <div>데이터가 없습니다.</div>;

  const handleDelete = async (accessToken, boardIdx) => {
  try {
    await deleteNotice(accessToken, boardIdx);
    alert("삭제되었습니다.");
  } catch (error) {
    alert("삭제 중 오류 발생: " + error.message);
  }
};


const decodeBase64 = (str) => {
  try {
    const cleanStr = str.replace(/\s/g, '');
    const binary = atob(cleanStr);
    const decoded = decodeURIComponent(Array.prototype.map.call(binary, (ch) =>
      '%' + ('00' + ch.charCodeAt(0).toString(16)).slice(-2)
    ).join(''));
    return decoded;
  } catch (e) {
    console.error("Base64 디코딩 오류:", e);
    return "";
  }
};

const handleDownload = async (attachmentIdx, fileName) => {
  try {
    const response = await fetch(
      'https://bluecrab.chickenkiller.com/BlueCrab-1.0.0/api/board-attachments/download',
      {
        method: 'POST',
        headers: { 
          'Content-Type': 'application/json',
          'Authorization': `Bearer ${accessToken}`
        },
        body: JSON.stringify({ attachmentIdx })
      }
    );

    if (!response.ok) {
      throw new Error("파일 다운로드 실패");
    }

    const blob = await response.blob();
    const url = window.URL.createObjectURL(blob);

    const link = document.createElement("a");
    link.href = url;
    link.setAttribute("download", fileName);
    document.body.appendChild(link);
    link.click();

    link.parentNode.removeChild(link);
    window.URL.revokeObjectURL(url);
  } catch (err) {
    alert("다운로드 중 오류 발생: " + err.message);
  }
};


const markdown = decodeBase64(notice.boardContent);

 return (
  <div className="noticeDetailContainer">
    <div className="noticeDetailTitleAndCode">
      <span className="noticeDetailTitle">제목 : {decodeBase64(notice.boardTitle)}</span>
      <span className="noticeDetailCode">{getNoticeCode(notice.boardCode)}</span>
    </div>

    <div className="noticeDetailWriterAndView">
      <span className="noticeDetailWriter">작성자 : {notice.boardWriter}</span>
      <span className="noticeDetailView">조회수 : {notice.boardView}</span>
    </div>

    <div className="noticeDetailRegAndLast">
      <span className="noticeDetailReg">작성일 : {formattedReg(notice.boardReg)}</span>
      <span className="noticeDetailLast">최종 수정일 : {formattedLatest(notice.boardLast, notice.boardReg)}</span>
    </div>

    <div className="noticeDetailAttachment">
      <span>첨부파일:</span>
      {notice.attachmentDetails && notice.attachmentDetails.length > 0 ? (
        notice.attachmentDetails.map((att, index) => (
          <div key={index}>
            <button
              className="attachmentLink"
              onClick={() => handleDownload(att.attachmentIdx, att.originalFileName)}
            >
              📎 {att.originalFileName}
            </button>
          </div>
        ))
      ) : (
        <p>첨부파일 없음</p>
      )}
    </div>

    <div className="noticeDetailContent">
      <Viewer initialValue={markdown} />
    </div>

    <button className="noticeDeleteButton" onClick={() => handleDelete(accessToken, notice.boardIdx)}>
      공지 삭제
    </button>
  </div>
);

};

export default NoticeDetail;
