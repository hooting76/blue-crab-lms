import React from 'react';
import { BrowserRouter, Routes, Route, Navigate ,useLocation} from 'react-router-dom';
import { useState, useEffect } from 'react';

// user state control
import { UserProvider } from '../context/UserContext';

// custom hook
import { UseUser } from '../hook/UseUser';

// component
import Header from '../component/common/Header';
import LoadingSpinner from '../component/common/LoadingSpinner';
import LoginForm from '../component/auth/LoginForm';
import UserDashboard from '../component/auth/UserDashboard';
import Footer from '../component/common/Footer';
import InAppFunction from '../component/common/InAppFunc';
import FindInfo from '../component/auth/FindInfo';

// 학교소개 페이지들
import Introduction from '../component/common/Introduction';

// 커뮤니티 페이지들(상태 전환으로 렌더)
//import Community from '../component/common/Community';
import AcademyNotice from '../component/common/Communities/AcademyNotice';
import AdminNotice from '../component/common/Communities/AdminNotice';
import EtcNotice from '../component/common/Communities/EtcNotice';
import AdminNoticeWritingPage from '../component/common/Communities/AdminNoticeWritingPage';

// FAQ
import FAQ from '../component/common/FAQ/FAQ';

// Facilities
import FacilityRequest from '../component/common/Facilities/FacilityRequest';
import MyFacilityRequests from '../component/common/Facilities/MyFacilityRequests';
import ReadingRoom from '../component/common/Facilities/ReadingRoom';

// 마이페이지 페이지들
import MyPage from '../component/common/MyPage';

// 관리자 페이지
import Admin from './Admin';

// css
import '../css/App.css';

// InApp filter function
function InAppFilter(){
  const userAgent = navigator.userAgent.toLowerCase();
  const targetUrl = window.location.href;
  return InAppFunction(userAgent, targetUrl);
};

// main app component
function AppContent() {

  // user state ctrl
  const { isAuthenticated, isLoading } = UseUser();
  //user state ctrl end
  if (isLoading) {
  return <LoadingSpinner/>
  }

  //const isAuthenticated = true;   // ← 로그인 상태로 강제

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

  // 관리자 경로는 '정확/접두' 매칭만 (includes 금지: /community/admin과 충돌 방지)
  if (pathname === '/admin' || pathname.startsWith('/admin/')) {
    return <Admin />;
  }


  const renderPage = () => {
    switch (currentPage) {

      // 학교소개 페이지
      case "총장 인사":
      case "오시는 길":
      case "학교 조직도":
      case "연혁":
        return <Introduction currentPage={currentPage} setCurrentPage={setCurrentPage}/>;
      
      // 마이페이지
      case "수강중인 과목":
      case "수강과목 공지사항":
      case "실시간 상담":
        return <MyPage currentPage={currentPage} setCurrentPage={setCurrentPage}/>;
      
      // ===== 커뮤니티 섹션 (상태 전환만 사용) =====
      case '학사공지':
        return <AcademyNotice currentPage={currentPage} setCurrentPage={setCurrentPage} />;
      case '행정공지':
        return <AdminNotice currentPage={currentPage} setCurrentPage={setCurrentPage} />;
      case '기타공지':
        return <EtcNotice currentPage={currentPage} setCurrentPage={setCurrentPage} />;
      case 'Admin 공지 작성':
        return <AdminNoticeWritingPage currentPage={currentPage} setCurrentPage={setCurrentPage}/>;

      // ===== FAQ =====
      case 'FAQ':
        return <FAQ currentPage={currentPage} setCurrentPage={setCurrentPage} />;

      // ===== 시설 & 문의 =====
      case '시설신청':
        return <FacilityRequest currentPage={currentPage} setCurrentPage={setCurrentPage} />;
      case '나의 신청목록':
        return <MyFacilityRequests currentPage={currentPage} setCurrentPage={setCurrentPage} />;
      case '열람실 신청':
        return <ReadingRoom currentPage={currentPage} setCurrentPage={setCurrentPage} />;
    
  // 기본값: 대시보드
      default:
        return <UserDashboard/>;
    };
  }; // paging ctrl end

  return (
    <div id="wrap">
      <Header currentPage={currentPage} setCurrentPage={setCurrentPage}/>

      <div id="content">
          {isAuthenticated ?(
            renderPage()
          ) : (
          // 로그인 안했으면 로그인폼 or 아이디/비번 찾기폼 
            <Routes>
              <Route path="/" element={<LoginForm/>} />
              <Route path="/FindInfoId" element={<FindInfo/>} />
              <Route path="/FindInfoPw" element={<FindInfo/>} />
              {/* 비로그인 상태에서 다른 경로로 오면 로그인으로 돌림 */}
              <Route path="*" element={<Navigate to="/" replace />} />
            </Routes>
          )}
      </div>
      
      {/* 푸터 */}
      <Footer />
   </div>
    ); // return end
} // AppContent end

function App() {
  return (
    <>
      <InAppFilter />
      <UserProvider>
        <BrowserRouter>
          <AppContent />
        </BrowserRouter>
      </UserProvider>
    </>
  );
}

export default App;