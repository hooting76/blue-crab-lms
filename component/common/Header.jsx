import React, {useState } from 'react';
import { UseUser } from '../../hook/UseUser';
import { useNavigate } from "react-router-dom";
import HeaderCss from '../../css/modules/Header.module.css';
import SessionTimer from './SessionTimer';

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
  const navigate = useNavigate();

  const Reset = () => {
    window.location.replace('/');
  }

  const [subMenu1Visibility, setSubMenu1Visibility] = useState("hidden");
  const [subMenu2Visibility, setSubMenu2Visibility] = useState("hidden");
  const [subMenu3Visibility, setSubMenu3Visibility] = useState("hidden");
  const showSubMenu1 = () => setSubMenu1Visibility("visible");
  const hideSubMenu1 = () => setSubMenu1Visibility("hidden");
  const showSubMenu2 = () => setSubMenu2Visibility("visible");
  const hideSubMenu2 = () => setSubMenu2Visibility("hidden");
  const showSubMenu3 = () => setSubMenu3Visibility("visible");
  const hideSubMenu3 = () => setSubMenu3Visibility("hidden");

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
      
        <h1 className={HeaderCss.h1}>
          <picture className={HeaderCss.logoImg} onClick={Reset}
          onMouseOver={() => {hideSubMenu1(); hideSubMenu2(); hideSubMenu3();}}>
            <img src='./favicon/android-icon-72x72.png' alt='Logo'/>
          </picture>
          
          <span onMouseOver={() => {hideSubMenu1(); hideSubMenu2(); hideSubMenu3();}} onClick={Reset}>Blue-Crab LMS</span>
          {/* user menu */}
            {isAuthenticated &&(  
          <div className={HeaderCss.navMenu}>
            <ul>
              <li onMouseOver={() => {showSubMenu1(); hideSubMenu2(); hideSubMenu3();}}>학교소개</li>
              <li onMouseOver={() => {hideSubMenu1(); showSubMenu2(); hideSubMenu3();}}>커뮤니티</li>
              <li onMouseOver={() => {hideSubMenu1(); hideSubMenu2(); showSubMenu3();}}>마이페이지</li>
            </ul>

            <table
                className={HeaderCss.navSubMenu1}
                onMouseOver={showSubMenu1}
                onMouseOut={hideSubMenu1}
                style={{ visibility: subMenu1Visibility }}
            >
                <tbody>
                    <tr>
                        <td onClick={() => navigate("/Introduction/PresidentSaysHi")}>총장 인사</td>
                    </tr>
                    <tr>
                        <td onClick={() => navigate("/Introduction/WayHere")}>오시는 길</td>
                    </tr>
                    <tr>
                        <td onClick={() => navigate("/Introduction/Organization")}>학교 조직도</td>
                    </tr>
                    <tr>
                        <td onClick={() => navigate("/Introduction/BlueCrabHistory")}>연혁</td>
                    </tr>
                </tbody>
            </table>

            <table
                className={HeaderCss.navSubMenu2}
                onMouseOver={showSubMenu2}
                onMouseOut={hideSubMenu2}
                style={{ visibility: subMenu2Visibility }}
            >
                <tbody>
                    <tr>
                        <td onClick={() => navigate("/community/academy")}>학사공지</td>
                    </tr>
                    <tr>
                        <td onClick={() => navigate("/community/notice-admin")}>행정공지</td>
                    </tr>
                    <tr>
                        <td onClick={() => navigate("/community/etc")}>기타공지</td>
                    </tr>
                </tbody>
            </table>

            <table
                className={HeaderCss.navSubMenu3}
                onMouseOver={showSubMenu3}
                onMouseOut={hideSubMenu3}
                style={{ visibility: subMenu3Visibility }}
            >
                <tbody>
                    <tr>
                        <td onClick={() => navigate("/MyPage/ClassAttendingList")}>수강중인 과목</td>
                    </tr>
                    <tr>
                        <td onClick={() => navigate("/MyPage/ClassAttendingProgress")}>과목별 진행사항</td>
                    </tr>
                    <tr>
                        <td onClick={() => navigate("/MyPage/ClassAttendingNotice")}>수강과목 공지사항</td>
                    </tr>
                    <tr>
                        <td onClick={() => navigate("/MyPage/Consult")}>실시간 상담</td>
                    </tr>
                </tbody>
            </table>
          </div>   
         )}  
        </h1>

        {/* 세션타이머(15분). 로그인 중에만 활성화. 만료시 로그아웃.*/}
          {isAuthenticated && (  
        <div className={HeaderCss.sessionTimer}>
        <SessionTimer/>
        </div>
          )}  

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
                  onClick={async () => {logout; window.location.replace('/');}} 
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