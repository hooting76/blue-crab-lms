import React from "react";
import { BrowserRouter, Routes, Route, Navigate} from 'react-router-dom';
import { useState, useEffect } from 'react';

// user state control
import { AdminProvider } from '../context/AdminContext';

// custom hook
import { UseAdmin } from '../hook/UseAdmin';

import AdminBdCss from "../css/admin/module/admin.module.css";

import InAppFunction from '../component/common/InAppFunc';
import LoadingSpinner from '../component/common/LoadingSpinner';
import AdminDashboard from '../component/admin/AdminDashboard';
import AdminLogin from '../component/admin/AdminLogin';

import AdHeader from "../component/admin/common/AdHeader";
import AdFooter from "../component/admin/common/AdFooter";
import AdNav from "../component/admin/common/AdNav";

// 커뮤니티 페이지들(상태 전환으로 렌더)
//import Community from '../component/common/Community';
import AcademyNotice from '../component/common/Communities/AcademyNotice';
import AdminNotice from '../component/common/Communities/AdminNotice';
import EtcNotice from '../component/common/Communities/EtcNotice';
import AdminNoticeWritingPage from '../component/common/Communities/AdminNoticeWritingPage';


// InApp filter function
function InAppFilter(){
  const userAgent = navigator.userAgent.toLowerCase();
  const targetUrl = location.href;

  return(
    InAppFunction(userAgent, targetUrl)
  );
};

// currentPage 상태를 이 컴포넌트에서 보유
  const [currentPage, setCurrentPage] = useState("");

  // 현재 URL 경로 사용 (관리자 라우팅 분기에서 필요)
  const { pathname } = useLocation();

  // 컴포넌트 마운트 시 localStorage에서 currentPage 읽기
  useEffect(() => {
    const savedCurrentPage = localStorage.getItem('currentPage');
    if (savedCurrentPage) {
      setCurrentPage(savedCurrentPage);
    }
  }, []);

  // currentPage가 바뀔 때 localStorage에 저장
  useEffect(() => {
    localStorage.setItem('currentPage', currentPage);
  }, [currentPage]);

const renderPage = () => {
    switch (currentPage) {
        // ===== 커뮤니티 섹션 (상태 전환만 사용) =====
      case '학사공지':
        return <AcademyNotice currentPage={currentPage} setCurrentPage={setCurrentPage} />;
      case '행정공지':
        return <AdminNotice currentPage={currentPage} setCurrentPage={setCurrentPage} />;
      case '기타공지':
        return <EtcNotice currentPage={currentPage} setCurrentPage={setCurrentPage} />;
      case 'Admin 공지 작성':
        return <AdminNoticeWritingPage currentPage={currentPage} setCurrentPage={setCurrentPage}/>;

      default:
        return <AdminDashboard/>;
    }
}

function AuthTof(){
    const { isAuthenticated, isLoading } = UseAdmin();    

    if (isLoading) {
        return <LoadingSpinner/>
    }

    return (
        <div id="wrap" className={AdminBdCss.wrap}>
            {isAuthenticated ? <AdNav/> : null}
            <div id="content" className={AdminBdCss.content}>
                {isAuthenticated ? <AdHeader /> : null }
                {isAuthenticated ? {renderPage} : <AdminLogin/>}
                {isAuthenticated ? <AdFooter /> : null }
            </div>
        </div>
    );
}

function Admin() {
    return(
        <>  
            <InAppFilter />
            <AdminProvider>
                <AuthTof />
            </AdminProvider>
        </>  
    );
}

export default Admin;