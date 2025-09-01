import React from 'react';
import { UseUser } from '../../hook/UseUser';

function UserDashboard() {
  const { user, logout } = UseUser();

  return (
    <div className="">
      <div className="">
        <div className="">{user.avatar}</div>
        <h2 className="">환영합니다!</h2>
        
        {/* 사용자 정보 */}
        <div className="">
          <p className="">
            <strong>이름:</strong> {user.name}
          </p>
          <p className="">
            <strong>이메일:</strong> {user.email}
          </p>
        </div>
        
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