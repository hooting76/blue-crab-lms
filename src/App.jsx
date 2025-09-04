import React from 'react';

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
  const { isAuthenticated, isLoading } = UseUser();

  // 로딩 중일 때
  if (isLoading) {
    return <LoadingSpinner/>
  }

  return (
    <div id="wrap">
      <Header />

      <div id="content">
        {isAuthenticated ? <UserDashboard /> : <LoginForm />}
      </div>
      
      {/* 푸터 */}
      <Footer />
    </div>
  );
}

function App() {
  return (
    <UserProvider>
      <InAppFilter />
      <AppContent />
    </UserProvider>
  );
}

export default App;