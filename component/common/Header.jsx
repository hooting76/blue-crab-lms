import React from 'react';
import { UseUser } from '../../hook/UseUser';
import HeaderCss from '../../css/modules/Header.module.css';

function FuncAniBtn(){
  let lines = document.querySelectorAll('header > div > span');

  let mobMenu = document.querySelector('header').nextSibling;

  if(lines[0].style.background == 'transparent'){
    // css ani
    lines[0].style.background = '#333';
    lines[1].style.transform = 'rotate(0deg)';
    lines[2].style.transform = 'rotate(0deg)';
    lines[1].style.top = 'calc(50% - 5px)';    
    lines[2].style.top = 'calc(100% - 10px)';

    mobMenu.style.height = '0px';
  }else{
    // css ani
    lines[0].style.background = 'transparent';
    lines[1].style.transform = 'rotate(45deg)';
    lines[2].style.transform = 'rotate(-45deg)';
    lines[1].style.top = '50%';
    lines[2].style.top = '50%';

    mobMenu.style.height = '60px';
  }
}


function Header() {
  const { user, isAuthenticated, logout } = UseUser();

  const Reset = () => {
    window.location.replace('/');
  }

  return (
  <>
    <header>
      {isAuthenticated && (
        <div 
          className={HeaderCss.mobNavBtn}
          onClick={FuncAniBtn}
        >
          <span></span>
          <span></span>
          <span></span>
        </div>
      )}
      
        <h1 onClick={Reset} className={HeaderCss.h1}>
          <picture className={HeaderCss.logoImg}>
            <img src='./favicon/android-icon-72x72.png' alt='Logo'/>
          </picture>
          
          <span>Blue-Crab LMS</span>
          {/* user menu */}
          {isAuthenticated &&(
          <div className={HeaderCss.navMenu}>
            <ul>
              <li>학교소개</li>
              <li>커뮤니티</li>
              <li>마이페이지</li>
            </ul>
          </div>   
          )}
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

    {isAuthenticated && (
      <div className={HeaderCss.mobMenu}>
      <ul>
        <li>학교소개</li>
        <li>커뮤니티</li>
        <li>마이페이지</li>
      </ul>
    </div>
    )}    

  </>
  );
}

export default Header;