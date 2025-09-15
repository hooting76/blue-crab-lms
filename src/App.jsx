import React from 'react';
import { BrowserRouter, Routes, Route, Navigate} from 'react-router-dom';

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
import PresidentSaysHi from '../component/common/Introductions/PresidentSaysHi';
import WayHere from '../component/common/Introductions/WayHere';
import Organization from '../component/common/Introductions/Organization';
import BlueCrabHistory from '../component/common/Introductions/BlueCrabHistory';

// 커뮤니티 페이지들
//import Community from '../component/common/Community';
import AcademyNotice from '../component/common/Communities/AcademyNotice';
import AdminNotice from '../component/common/Communities/AdminNotice';
import EtcNotice from '../component/common/Communities/EtcNotice';

// 공지사항 공통 레이아웃
import NoticeLayout from '../component/common/notices/NoticeLayout';

// 마이페이지 페이지들
import MyPage from '../component/common/MyPage';
import ClassAttendingList from '../component/common/MyPages/ClassAttendingList';
import ClassAttendingProgress from '../component/common/MyPages/ClassAttendingProgress';
import ClassAttendingNotice from '../component/common/MyPages/ClassAttendingNotice';
import Consult from '../component/common/MyPages/Consult';

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

            {/* 커뮤니티 페이지들 */}
              <Route path="/community/academy"       element={<AcademyNotice/>} />
              <Route path="/community/notice-admin"  element={<AdminNotice/>} />
              <Route path="/community/etc"           element={<EtcNotice/>} />
              <Route path="/community" element={<Navigate to="/community/academy" replace />} />
              <Route path="/community/admin" element={<Navigate to="/community/notice-admin" replace />} /> 
              {/* /community/admin 으로 들어가져도 자동우회 */}

            
            {/* 마이페이지 페이지들 */}
             <Route path="/MyPage/*" element={<MyPage />}>
              <Route path="ClassAttendingList" element={<ClassAttendingList />} />
              <Route path="ClassAttendingProgress" element={<ClassAttendingProgress />} />
              <Route path="ClassAttendingNotice" element={<ClassAttendingNotice />} />
              <Route path="Consult" element={<Consult />} />
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