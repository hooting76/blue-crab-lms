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

// css
import '../css/App.css';

// main app component
function AppContent() {
  const { isAuthenticated, isLoading } = UseUser();

  // 로딩 중일 때
  if (isLoading) {
    return <LoadingSpinner message="사용자 정보를 불러오는 중..." />;
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
      <AppContent />
    </UserProvider>
  );
}

export default App;