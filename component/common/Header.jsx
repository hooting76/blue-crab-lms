import React from 'react';
import { UseUser } from '../../hook/UseUser';

function Header() {
  const { user, isAuthenticated, logout } = UseUser();

  return (
    <header>
      <div>
        <div>
          {/* 로그인된 사용자 정보 */}
          {isAuthenticated && (
            <div>
              <div>
                <div>
                  <span>아이콘</span>
                  
                  {/* user_idx */}
                  {user && user.data.user && (
                    <input type='hidden' id='idx' value={JSON.stringify(user.data.user.id)} />
                  )}

                  <div>
                      {/* user_name*/}
                      {user && user.data.user && (
                        <p>{JSON.stringify(user.data.user.name)}</p>
                      )}

                      {/* user_email */}
                      {user && user.data.user && (
                        <p>{JSON.stringify(user.data.user.email)}</p>
                      )}
                  </div>
                </div>
              </div>
              
              {/* 간단한 로그아웃 버튼 */}
              <button onClick={logout} title="로그아웃" >
                로그아웃
              </button>
            </div>
          )}
          
          {/* 로그인되지 않은 상태 */}
          {!isAuthenticated && (
            <div>
              로그인이 필요합니다
            </div>
          )}
          
        </div>
      </div>
    </header>
  );
}

export default Header;