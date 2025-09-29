import "../../../css/Communities/NoticeDetail.css"
import React, { useEffect, useState } from 'react';
import { getNoticeDetail } from '../../api/noticeAPI';
import { UseUser } from "../../../hook/UseUser";

const NoticeDetail = ({ boardIdx }) => {
  const [notice, setNotice] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const { user, isAuthenticated } = UseUser();

  const accessToken = isAuthenticated ? user.data.accessToken : null;

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
        setNotice(data);
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

  return (
    <div>
      <div>
        <span className='noticeDetailTitle'>제목 : {notice.boardTitle}</span>
        <span className='noticeDetailCode'>{getNoticeCode(notice.boardCode)}</span>
      </div>
      <div>
        <span className='noticeDetailWriter'>작성자 : {notice.boardWriter}</span>
        <span className='noticeDetailView'>조회수 : {notice.boardView}</span>
      </div>
      <div>
        <span className='noticeDetailReg'>작성일 : {formattedReg(notice.boardReg)}</span>
        <span className='noticeDetailLast'>최종 수정일: {formattedLatest(notice.boardLast)}</span>
      </div>
      <div className='noticeDetailContent'>{notice.boardContent}</div>
    </div>
  );
};

export default NoticeDetail;
