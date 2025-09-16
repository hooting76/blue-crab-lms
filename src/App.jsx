import React from 'react';
import { BrowserRouter, Routes, Route, Navigate} from 'react-router-dom';
import { useState } from 'react';

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
  const targetUrl = location.href;

  return(
    InAppFunction(userAgent, targetUrl)
  );
};

// main app component
function AppContent() {
  const { isAuthenticated, isLoading, logout } = UseUser();
  const [currentPage, setCurrentPage] = useState("");

  // 로딩 중일 때
  if (isLoading) {
    return <LoadingSpinner/>
  }

  // 관리자 페이지 접근
  const userAgent = location.href;
  if(userAgent.includes('/admin')){
    return(<Admin/>); 
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

  return (
    <BrowserRouter>
    <div id="wrap">
      <Header currentPage={currentPage} setCurrentPage={setCurrentPage}/>

      <div id="content">
           {isAuthenticated ?
            renderPage()
          // isAuth end 

         : // isAuth false start    
           <Routes>
            <Route path="/" element={<LoginForm/>} />
            <Route path="/FindInfo" element={<FindInfo/>}/>
          </Routes>
         }
      </div>
      
      {/* 푸터 */}
      <Footer />
    </div>
    </BrowserRouter>
  );
}

function App() {
  return (
    <>
      <InAppFilter />
      <UserProvider>
        <AppContent />
      </UserProvider>
    </>
  );
}

export default App;