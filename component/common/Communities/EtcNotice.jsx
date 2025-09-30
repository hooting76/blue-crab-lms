//"기타공지" 전용 페이지 래퍼
import React, { useState, useEffect } from "react";
import NoticeLayout from "../notices/NoticeLayout";
import NoticeList from "../notices/NoticeList";
import AdminNoticeWritingPage from "./AdminNoticeWritingPage";
import { UseUser } from "../../../hook/UseUser";
import { UseAdmin } from "../../../hook/UseAdmin";

export default function EtcNotice({currentPage,setCurrentPage}) {
    const [page,setPage] = useState(1);
    useEffect(() => {setPage(1);}, [currentPage]); //탭 바뀌면 1 페이지로

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
                    if (isAdminAuth && admin?.data?.accessToken) {
                        return admin.data.accessToken;
                    }
            
                    // User 토큰 확인
                    if (isUserAuth && user?.data?.accessToken) {
                        return user.data.accessToken;
                    }
            
                    return null;
                };
            
                const accessToken = getAccessToken();
        
          if (currentPage === "Admin 공지 작성") {
    if (!accessToken) {
        return <p>인증 토큰을 불러오는 중입니다...</p>;
    }

    return (
        <AdminNoticeWritingPage
            currentPage={currentPage}
            accessToken={accessToken}
            setCurrentPage={setCurrentPage}
        />
    );
}
    
    return(
        <NoticeLayout currentPage={currentPage} setCurrentPage={setCurrentPage}>
            <NoticeList
              boardCode={2}
              page={page}
              size={10}
              onPageChange={setPage}
              onWrite={() => setCurrentPage("Admin 공지 작성")}
              />
        </NoticeLayout>
    );
}