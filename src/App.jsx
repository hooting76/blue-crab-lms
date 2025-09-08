import React from 'react';
import { BrowserRouter, Routes, Route } from 'react-router-dom';

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
import PresidentSaysHi from '../component/common/Introduction/PresidentSaysHi';
import WayHere from '../component/common/Introduction/WayHere';
import Organization from '../component/common/Introduction/Organization';
import BlueCrabHistory from '../component/common/Introduction/BlueCrabHistory';

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

  // 로딩 중일 때
  if (isLoading) {
    return <LoadingSpinner/>
  }

  // 관리자 페이지 접근
  const userAgent = location.href;
  if(userAgent.indexOf('/admin') > -1){
    // 일반 회원과 세션이 겹치지 않게 하기 위한 초기화
    if(localStorage.key('user') || sessionStorage.key('user')){
      logout;
      window.location.reload();
    }
    return(<Admin/>); 
  }

  return (
    <BrowserRouter>
    <div id="wrap">
      <Header />

      <div id="content">
        {isAuthenticated ?
          <Routes>
            {/* 기본 페이지 */}
            <Route path="/" element={<UserDashboard />} />
            {/* 학교소개 페이지들 */}
            <Route path="/Introduction/*" element={<Introduction />}>
              <Route path="PresidentSaysHi" element={<PresidentSaysHi />} />
              <Route path="WayHere" element={<WayHere />} />
              <Route path="Organization" element={<Organization />} />
              <Route path="BlueCrabHistory" element={<BlueCrabHistory />} />
            </Route>
          </Routes>
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