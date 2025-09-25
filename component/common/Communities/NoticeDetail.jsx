import "../../../css/Communities/NoticeDetail.css"
import React, { useEffect, useState } from 'react';
import { getNoticeDetail } from '../../api/noticeAPI';

const NoticeDetail = ({ boardIdx, isAuthenticated, user }) => {
  const [notice, setNotice] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  const accessToken = isAuthenticated ? user?.data?.accessToken : null;

  // boardCode에 따른 공지 종류 반환
  const getNoticeCode = (boardCode) => {
    switch (boardCode) {
      case "0":
        return "학사공지";
      case "1":
        return "행정공지";
      case "2":
        return "기타공지";
      default:
        return "공지";
    }
  };

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
      <div className='noticeDetailTitle'>{notice.boardTitle}</div>
      <div className='noticeDetailCode'>{getNoticeCode(notice.boardCode)}</div>
      <div className='noticeDetailWriter'>{notice.boardWriter}</div>
      <div className='noticeDetailReg'>{notice.boardReg}</div>
      <div className='noticeDetailContent'>{notice.boardContent}</div>
    </div>
  );
};

export default NoticeDetail;
