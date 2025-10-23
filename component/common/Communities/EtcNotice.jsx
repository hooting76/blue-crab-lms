import React, { useState, useEffect } from "react";
import NoticeLayout from "../notices/NoticeLayout";
import NoticeList from "../notices/NoticeList";
import AdminNoticeWritingPage from "./AdminNoticeWritingPage";
import { UseUser } from "../../../hook/UseUser";
import { UseAdmin } from "../../../hook/UseAdmin";

export default function EtcNotice({ currentPage, setCurrentPage, setSelectedNotice }) {
  const [page, setPage] = useState(1);
  const [refreshKey, setRefreshKey] = useState(0); // ✅ 새로고침 트리거용

  useEffect(() => {
    setPage(1);
  }, [currentPage]);

  // 사용자 컨텍스트
  const userContext = UseUser();
  const { user, isAuthenticated: isUserAuth } = userContext || { user: null, isAuthenticated: false };

  // 관리자 컨텍스트
  const adminContext = UseAdmin() || { admin: null, isAuthenticated: false };
  const { admin, isAuthenticated: isAdminAuth } = adminContext;

  // 토큰 가져오기
  const getAccessToken = () => {
    const storedToken = localStorage.getItem("accessToken");
    if (storedToken) return storedToken;
    if (isAdminAuth && admin.accessToken) return admin.accessToken;
    if (isUserAuth && user?.data?.accessToken) return user.data.accessToken;
    return null;
  };
  const accessToken = getAccessToken();

  if (currentPage === "Admin 공지 작성") {
    if (!accessToken) return <p>인증 토큰을 불러오는 중입니다...</p>;

    return (
      <AdminNoticeWritingPage
        currentPage={currentPage}
        setCurrentPage={setCurrentPage}
      />
    );
  }

  // ✅ NoticeDetail에서 삭제 후 실행될 콜백
  const handleDeleteSuccess = () => {
    setSelectedNotice(null);        // 모달 닫기
    setRefreshKey(prev => prev + 1); // NoticeList 새로고침
  };

  return (
    <NoticeLayout currentPage={currentPage} setCurrentPage={setCurrentPage}>
      <NoticeList
        key={refreshKey}               // ✅ 새로고침 트리거
        boardCode={2}
        page={page}
        size={10}
        onPageChange={setPage}
        onWrite={() => setCurrentPage("Admin 공지 작성")}
        currentPage={currentPage}
        setCurrentPage={setCurrentPage}
        setSelectedNotice={setSelectedNotice}
        onDeleteSuccess={handleDeleteSuccess} // ✅ 전달
      />
    </NoticeLayout>
  );
}
