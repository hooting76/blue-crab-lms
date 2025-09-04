import React from 'react';
import { UseUser } from '../../hook/UseUser';
import HeaderCss from '../../css/modules/Header.module.css';

function Header() {
  const { user, isAuthenticated, logout } = UseUser();

  const Reset = () => {
    window.location.replace('/');
  }

  return (
    <header>
        <h1 onClick={Reset}>
          <picture className={HeaderCss.logoImg}>
            <img src='./favicon/android-icon-72x72.png' alt='Logo'/>
          </picture>
          Blue-Crab LMS
        </h1>

          {/* 로그인된 사용자 정보 */}
          {isAuthenticated && (
            <div>
              <div>
              {/* user info init */}

                  {/* user_idx */}
                  {user && user.data.user && (
                    <input type='hidden' id='idx' value={JSON.stringify(user.data.user.id)} />
                  )}

                  {/* user_name*/}
                  <div className={HeaderCss.userInfo}>
                      {user && user.data.user && (
                        <p>
                          <span className={HeaderCss.userName}>
                            {JSON.stringify(user.data.user.name).replace('"','').replace('"','')}
                          </span>
                          {user && user.data.user && (
                            <>{JSON.stringify(user.data.user.student) == 1 && ('교수')}</>
                          )}
                          님 안녕하세요
                        </p>
                      )}
                  </div>
              </div>
              {/* user info end */}
            
              <div className="btnCol">
                <button
                  onClick={() => alert('개인정보 수정 페이지는 현재 준비 중입니다.')}
                  title="개인정보"
                  className={HeaderCss.userInfoBtn}>
                  개인정보
                </button>

                <button 
                  onClick={logout} 
                  title="로그아웃" 
                  className={HeaderCss.logoutBtn}>
                  로그아웃
                </button>
              </div>
            </div>
          )}
          
          {/* 로그인되지 않은 상태 */}
          {!isAuthenticated && ( null )}
    </header>
  );
}

export default Header;