import { useLocation } from 'react-router-dom';
import React, { useState, useEffect, useRef } from 'react';
import { UseUser } from '../hooks/UseUser';

const SessionTimer = () => {
  const [timeLeft, setTimeLeft] = useState(900);
  const location = useLocation();
  const timerRef = useRef(null);
  const { logout } = UseUser();

  const formatTime = (seconds) => {
    const minutes = Math.floor(seconds / 60);
    const secs = seconds % 60;
    return `${minutes}분 ${secs < 10 ? '0' : ''}${secs}초`;
  };

  useEffect(() => {
    // URL이 바뀔 때마다 타이머 초기화
    setTimeLeft(900);
  }, [location]);

  useEffect(() => {
    timerRef.current = setInterval(() => {
      setTimeLeft(prev => {
        if (prev <= 1) {
          clearInterval(timerRef.current);
          alert("로그아웃되었습니다.");
          logout(); // 로그아웃 처리
          return 0;
        }
        return prev - 1;
      });
    }, 1000);

    return () => clearInterval(timerRef.current);
  }, []);

  return (
    <>
      {formatTime(timeLeft)} 후<br/>자동으로 로그아웃 |
      <button onClick={()=>setTimeLeft(900)}>연장</button>
    </>
  );
};

export default SessionTimer;