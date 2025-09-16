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

// 커뮤니티 페이지들
//import Community from '../component/common/Community';
import AcademyNotice from '../component/common/Communities/AcademyNotice';
import AdminNotice from '../component/common/Communities/AdminNotice';
import EtcNotice from '../component/common/Communities/EtcNotice';

// 공지사항 공통 레이아웃
import NoticeLayout from '../component/common/notices/NoticeLayout';

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
  const { isAuthenticated, isLoading, logout } = UseUser();
  const [currentPage, setCurrentPage] = useState("");
  const location = useLocation(); // 현재 경로 확인용
  const path = location?.pathname ?? ""; //  안전한 문자열
  const lowerPath = path.toLowerCase();  //  대소문자 혼용 대비

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


  // 로딩 중일 때
  if (isLoading) {
    return <LoadingSpinner/>
  }

  // 관리자 페이지 접근
  // const userAgent = location.href;
  // if(userAgent.includes('/admin')){
  //   return(<Admin/>); 
  // }

  // ✅ 전체 URL은 window.location.href 로만 사용
  const href = (typeof window !== "undefined" && window.location && window.location.href) ? window.location.href : "";

  // ✅ 관리자 페이지 분기 (크래시 원인 제거)
  if ((href ?? "").toLowerCase().includes("/admin")) {
    return <Admin />;
  }

  const renderPage = () => {
    switch (currentPage) {
      case "총장 인사":
      case "오시는 길":
      case "학교 조직도":
      case "연혁":
        return <Introduction currentPage={currentPage} setCurrentPage={setCurrentPage}/>;
      case "수강중인 과목":
      case "수강과목 공지사항":
      case "실시간 상담":
        return <MyPage currentPage={currentPage} setCurrentPage={setCurrentPage}/>;
      default:
        return <UserDashboard/>;
    }
  };

  // 커뮤니티 경로인지 확인
  const pathname = (location?.pathname ?? "");
  const isCommunityPath = pathname.toLowerCase().startsWith("/community/");

  return (
    <div id="wrap">
      <Header currentPage={currentPage} setCurrentPage={setCurrentPage}/>

      <div id="content">
           {isAuthenticated ?(
            // isAuth true start
            isCommunityPath ? (
           // 커뮤니티만 URL 라우팅으로 처리
          <Routes>
            {/* 소문자 경로 */}
            {/* /community -> /community/academy 리다이렉트 */}
            <Route path="/community" element={<Navigate to="/community/academy" replace />} />
            <Route path="/community/academy" element={<AcademyNotice />} />
            <Route path="/community/notice-admin" element={<AdminNotice />} />
            <Route path="/community/etc" element={<EtcNotice />} />
           </Routes>
           ) : (
            // 커뮤니티 외에는 currentPage 상태로 화면 전환
            renderPage()
           )
          ) : (
            // isAuth false start
          // 로그인 안했으면 로그인폼 or 아이디/비번 찾기폼
          // 로그인 성공하면 isAuth true로 바뀌면서 위의 대로 렌더링됨  
           <Routes>
            <Route path="/" element={<LoginForm/>} />
            <Route path="/FindInfoId" element={<FindInfo/>}/>
            <Route path="/FindInfoPw" element={<FindInfo/>}/>
            {/* 비로그인 상태에서 다른 경로로 오면 로그인으로 돌림 */}
            <Route path="*" element={<Navigate to="/" replace />} />
          </Routes>
         )} 
         {/* isAuth false end */}
      </div>
      
      {/* 푸터 */}
      <Footer />
    </div>
  );
}

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