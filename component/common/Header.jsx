import React from 'react';
import { UseUser } from '../../hook/UseUser';

function Header() {
  const { user, isAuthenticated, logout } = UseUser();

  return (
    <header className="bg-white shadow-sm">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-4">
        <div className="flex justify-between items-center">
          <div>
            <h1 className="text-xl font-bold text-gray-900">
              유저 상태 관리 데모
            </h1>
            <p className="text-sm text-gray-500 mt-1">
              React Context API를 활용한 전역 상태 관리
            </p>
          </div>
          
          {/* 로그인된 사용자 정보 */}
          {isAuthenticated && (
            <div className="flex items-center space-x-4">
              <div className="text-right">
                <div className="flex items-center space-x-2">
                  <span className="text-lg">{user.avatar}</span>
                  <div>
                    <p className="text-sm font-medium text-gray-900">
                      {user.name}
                    </p>
                    <p className="text-xs text-gray-500">
                      {user.email}
                    </p>
                  </div>
                </div>
              </div>
              
              {/* 간단한 로그아웃 버튼 */}
              <button
                onClick={logout}
                className="text-sm bg-gray-100 hover:bg-gray-200 text-gray-700 px-3 py-1 rounded transition duration-200"
                title="로그아웃"
              >
                로그아웃
              </button>
            </div>
          )}
          
          {/* 로그인되지 않은 상태 */}
          {!isAuthenticated && (
            <div className="text-sm text-gray-500">
              로그인이 필요합니다
            </div>
          )}
        </div>
      </div>
    </header>
  );
}

export default Header;