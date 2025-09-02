import React from 'react';
import { UseUser } from '../../hook/UseUser';

function UserDashboard() {
  const { user, logout } = UseUser();

  return (
    <div className="">
      <div className="">
        <h2 className="">환영합니다!</h2>
        
        {/* 사용자 메뉴 */}
        <div className="">
          <button
            onClick={logout}
            className="">로그아웃
          </button>
        </div>
      </div>
    </div>
  );
}

export default UserDashboard;