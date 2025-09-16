import { useLocation } from 'react-router-dom';
import React, { useState, useEffect, useRef } from 'react';
import { UseUser } from '../../hook/UseUser';

const SessionTimer = () => {
  const [timeLeft, setTimeLeft] = useState(900); // 15분
  const location = useLocation();
  const timerRef = useRef(null);
  const { logout } = UseUser();

  // 페이지 경로를 기준으로 currentPage 정의
  const currentPage = location.pathname;

  // 시간 형식 변환
  const formatTime = (seconds) => {
    const minutes = Math.floor(seconds / 60);
    const secs = seconds % 60;
    return `${minutes}분 ${secs < 10 ? '0' : ''}${secs}초`;
  };

  // 로그아웃 핸들링
  const handleLogout = async () => {
    alert("로그아웃되었습니다.");
    await logout();
    window.location.replace('/');
  };

  // currentPage 변경 시 타이머 초기화 및 재시작
  useEffect(() => {
    clearInterval(timerRef.current); // 기존 타이머 정리
    setTimeLeft(900); // 타이머 초기화

    // 새 타이머 시작
    timerRef.current = setInterval(() => {
      setTimeLeft((prev) => {
        if (prev <= 1) {
          clearInterval(timerRef.current);
          handleLogout();
          return 0;
        }
        return prev - 1;
      });
    }, 1000);

    // 클린업 함수로 타이머 제거
    return () => clearInterval(timerRef.current);
  }, [currentPage]); // currentPage가 변경될 때만 실행

  // 연장 버튼: 타이머 리셋
  const handleExtend = () => setTimeLeft(900);

  return (
    <>
      {formatTime(timeLeft)} 후<br />자동으로 로그아웃 |
      <button onClick={handleExtend}>연장</button>
    </>
  );
};

export default SessionTimer;
