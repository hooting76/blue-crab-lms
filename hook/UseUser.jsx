import { useContext } from 'react';
import { UserContext } from '../context/UserContext';

// useUser 커스텀 훅
export function UseUser() {
  const context = useContext(UserContext);
  
  if (!context) {
    throw new Error('useUser는 UserProvider 내부에서 사용되어야 합니다.');
  }
  
  return context;
}

// 사용 예시:
// const { user, login, logout, isAuthenticated, isLoading, error } = useUser();