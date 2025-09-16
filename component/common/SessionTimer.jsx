import { useLocation } from 'react-router-dom';
import React, { useState, useEffect, useRef } from 'react';
import { UseUser } from '../../hook/UseUser';

const SessionTimer = () => {
  const [timeLeft, setTimeLeft] = useState(900);
  const timerRef = useRef(null);
  const { logout } = UseUser();

  const formatTime = (seconds) => {
    const minutes = Math.floor(seconds / 60);
    const secs = seconds % 60;
    return `${minutes}분 ${secs < 10 ? '0' : ''}${secs}초`;
  };

  const handleLogout = async () => {
    alert("로그아웃되었습니다.");
    await logout();
  }

  useEffect(() => {
    clearInterval(timerRef.current);
    setTimeLeft(900);

    timerRef.current = setInterval(() => {
      setTimeLeft(prev => {
        if (prev <= 1) {
          clearInterval(timerRef.current);
          handleLogout();
          return 0;
        }
        return prev - 1;
      });
    }, 1000);

  return () => clearInterval(timerRef.current);
}, [currentPage]);


  return (
    <>
      {formatTime(timeLeft)} 후<br/>자동으로 로그아웃 |
      <button onClick={()=>setTimeLeft(900)}>연장</button>
    </>
  );
};

export default SessionTimer;