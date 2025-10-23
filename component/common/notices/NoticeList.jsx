import React, { useEffect, useState } from "react";
import NoticeTable from "./NoticeTable";
import Pagination from "../notices/Pagination";
import { UseUser } from "../../../hook/UseUser";
import { UseAdmin } from "../../../hook/UseAdmin";
import { getNoticesByCode } from "../../api/noticeAPI";
import "../../../css/Communities/Notice-ui.css";

export default function NoticeList({
  boardCode,
  page = 1,
  size,
  onPageChange,
  onWrite,
  currentPage,
  setCurrentPage,
  setSelectedNotice,
  onDeleteSuccess
}) {
  const userContext = UseUser();
  const { user, isAuthenticated: isUserAuth } = userContext || { user: null, isAuthenticated: false };

  const adminContext = UseAdmin() || { admin: null, isAuthenticated: false };
  const { admin, isAuthenticated: isAdminAuth } = adminContext;

  // 권한 판별
  const isAdmin = isAdminAuth || (isUserAuth && user?.data?.role === "ADMIN");

  // 상태 분리
  const [items, setItems] = useState([]);
  const [total, setTotal] = useState(0);
  const [loading, setLoading] = useState(true);

  // accessToken 얻기 함수 간소화
  const getAccessToken = () => {
    return (
      localStorage.getItem('accessToken') ||
      (isAdminAuth && admin?.accessToken) ||
      (isUserAuth && user?.data?.accessToken) ||
      null
    );
  };

  const accessToken = getAccessToken();

  useEffect(() => {
    let isMounted = true; // cleanup 플래그
    setLoading(true);

    const fetchNotices = async () => {
      if (!accessToken) {
        console.warn('접근 토큰이 없습니다. 로그인이 필요할 수 있습니다.');
        if (isMounted) {
          setItems([]);
          setTotal(0);
          setLoading(false);
        }
        return;
      }

      try {
        const res = await getNoticesByCode(accessToken, boardCode, page - 1, size);
        if (!isMounted) return;

        const fetchedItems = Array.isArray(res.content) ? res.content : [];

        // 작성일 기준 내림차순 정렬
        fetchedItems.sort((a, b) => (b.boardReg || "").localeCompare(a.boardReg || ""));

        setItems(fetchedItems);
        setTotal(res.totalElements || 0);
      } catch (error) {
        console.error('게시글 목록 조회 실패:', error);
        if (isMounted) {
          setItems([]);
          setTotal(0);
        }
      } finally {
        if (isMounted) setLoading(false);
      }
    };

    fetchNotices();

    return () => {
      isMounted = false;
    };
  }, [accessToken, boardCode, page, size]);

  // 페이지 변경 핸들러
  const handlePageChange = (nextPage) => {
    if (typeof onPageChange === "function" && nextPage !== page) {
      onPageChange(nextPage);
    }
  };

  // 작성하기 버튼 클릭
  const handleWriteClick = () => {
    if (!isAdmin) return;
    setSelectedNotice(null);
    if (typeof onWrite === "function") {
      onWrite();
    }
  };

  // 삭제 성공 시 콜백
  const handleDeleteSuccess = () => {
    setSelectedNotice(null);
    if (typeof onDeleteSuccess === "function") {
      onDeleteSuccess();
    }
  };

  if (loading) {
    return <div className="notice-skeleton">로딩중...</div>;
  }

  return (
    <>
      {isAdmin && (
        <div className="notice-actions">
          <button type="button" className="btn-primary" onClick={handleWriteClick}>
            작성하기
          </button>
        </div>
      )}

      <NoticeTable
        rows={items}
        total={total}
        page={page}
        size={size}
        currentPage={currentPage}
        setCurrentPage={setCurrentPage}
        selectedNotice={null}
        setSelectedNotice={setSelectedNotice}
        onDeleteSuccess={handleDeleteSuccess}
      />

      <Pagination
        page={page}
        size={size}
        total={total}
        onChange={handlePageChange}
      />
    </>
  );
}
