// src/component/common/Header.jsx
import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import HeaderCss from '../../css/modules/Header.module.css';
import SessionTimer from './SessionTimer';
import { UseUser } from '../../hook/UseUser';

function FuncAniBtn(){
  let lines = document.querySelectorAll('header > div > span');
  let mobMenu = document.querySelector('header').nextSibling;

  if(lines[0].style.background === 'transparent'){
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

function Header({ currentPage, setCurrentPage }) {
  const navigate = useNavigate();

  const { user, isAuthenticated, logout } = UseUser();

  const Reset = () => {
    navigate('/');           // SPA 네비게이션
    setCurrentPage("");      // 대시보드 등 기본으로
    closeAllSubMenus();      // 서브메뉴 닫기
  };

  const [subMenu1Visibility, setSubMenu1Visibility] = useState("hidden");
  const [subMenu2Visibility, setSubMenu2Visibility] = useState("hidden");
  const [subMenu3Visibility, setSubMenu3Visibility] = useState("hidden");
  const showSubMenu1 = () => setSubMenu1Visibility("visible");
  const hideSubMenu1 = () => setSubMenu1Visibility("hidden");
  const showSubMenu2 = () => setSubMenu2Visibility("visible");
  const hideSubMenu2 = () => setSubMenu2Visibility("hidden");
  const showSubMenu3 = () => setSubMenu3Visibility("visible");
  const hideSubMenu3 = () => setSubMenu3Visibility("hidden");
  const closeAllSubMenus = () => {hideSubMenu1(); hideSubMenu2(); hideSubMenu3();};
  
  return (
    <>
      <header>
        {isAuthenticated && (
          <div className={HeaderCss.mobNavBtn} onClick={FuncAniBtn}>
            <span></span><span></span><span></span>
          </div>
        )}

        <h1 className={HeaderCss.h1}>
          <picture
            className={HeaderCss.logoImg}
            onClick={Reset}
            onMouseOver={() => { hideSubMenu1(); hideSubMenu2(); hideSubMenu3(); }}
          >
            <img src='./favicon/android-icon-72x72.png' alt='Logo'/>
          </picture>

          <span
            onMouseOver={() => { hideSubMenu1(); hideSubMenu2(); hideSubMenu3(); }}
            onClick={Reset}
          >
            Blue-Crab LMS
          </span>

          {isAuthenticated && (
            <div className={HeaderCss.navMenu}>
              <ul>
                <li onMouseOver={() => { showSubMenu1(); hideSubMenu2(); hideSubMenu3(); }}>학교소개</li>
                <li onMouseOver={() => {hideSubMenu1(); showSubMenu2(); hideSubMenu3();}}
                    onClick={() => setCurrentPage('학사공지')}>커뮤니티</li>
                <li onMouseOver={() => { hideSubMenu1(); hideSubMenu2(); showSubMenu3(); }}>마이페이지</li>
                  {/* 수강신청: 드롭다운 없이 한 번에 진입 */}
                <li onMouseOver={() => { hideSubMenu1(); hideSubMenu2(); hideSubMenu3(); }}
                    onClick={() => { setCurrentPage('수강신청'); closeAllSubMenus(); }}>수강신청</li>
                <li onMouseOver={() => { hideSubMenu1(); hideSubMenu2(); hideSubMenu3(); }}
                    onClick={() => { setCurrentPage('증명서'); closeAllSubMenus(); }}>증명서</li>
              </ul>

              {/* 학교소개 */}
              <table
                className={HeaderCss.navSubMenu1}
                onMouseOver={showSubMenu1}
                onMouseOut={hideSubMenu1}
                style={{ visibility: subMenu1Visibility }}
              >
                <tbody>
                  <tr><td onClick={() => { Reset(); setCurrentPage("총장 인사"); }}>총장 인사</td></tr>
                  <tr><td onClick={() => { Reset(); setCurrentPage("오시는 길"); }}>오시는 길</td></tr>
                  <tr><td onClick={() => { Reset(); setCurrentPage("학교 조직도"); }}>학교 조직도</td></tr>
                  <tr><td onClick={() => { Reset(); setCurrentPage("연혁"); }}>연혁</td></tr>
                </tbody>
              </table>

              {/* 커뮤니티 (공지사항/FAQ/시설&문의) */}
              <table
                className={HeaderCss.navSubMenu2}
                onMouseOver={showSubMenu2}
                onMouseOut={hideSubMenu2}
                style={{ visibility: subMenu2Visibility }}
              >
                <tbody>
                {/* 공지사항 → 기본 학사공지로 진입 */}
                  <tr><td onClick={() => { setCurrentPage("학사공지"); closeAllSubMenus(); }}>공지사항</td></tr>
                
                {/* FAQ */}
                  <tr><td onClick={() => { setCurrentPage("FAQ"); closeAllSubMenus(); }}>FAQ</td></tr>

                {/* 시설 & 문의 묶음 */}
                  <tr><td onClick={() => { setCurrentPage("시설물 예약"); closeAllSubMenus(); }}>시설&문의</td></tr>
                </tbody>
              </table>

              {/* 마이페이지 */}
              <table
                className={HeaderCss.navSubMenu3}
                onMouseOver={showSubMenu3}
                onMouseOut={hideSubMenu3}
                style={{ visibility: subMenu3Visibility }}>
                
                {/* URL 변경 없이 상태 전환만 (통일 로직) */}
                <tbody>
                  <tr><td onClick={() => { setCurrentPage("개인정보"); closeAllSubMenus(); }}>개인정보</td></tr>
                  {/* 두 항목(수강중인/공지사항)을 하나로 통합 → '나의강의실' 클릭 시 수강중인 과목으로 진입 */}
                  <tr><td onClick={() => { Reset(); setCurrentPage("수강중인 과목"); closeAllSubMenus(); }}>나의강의실</td></tr>
                  <tr><td onClick={() => { Reset(); setCurrentPage("실시간 상담"); closeAllSubMenus(); }}>실시간 상담</td></tr>
                </tbody>
              </table>
            </div>
          )}
        </h1>

        {/* 세션타이머(15분). 로그인 중에만 활성화. 만료시 로그아웃됨. */}
        {isAuthenticated && (
          <div className={HeaderCss.sessionTimer}>
            <SessionTimer currentPage={currentPage}/>
          </div>
        )}

        {/* 로그인된 사용자 정보 */}
        {isAuthenticated && (
          <div>
            <div>
              {/* user_idx */}
              {user && user.data?.user && (
                <input type='hidden' id='idx' value={JSON.stringify(user.data.user.id)} />
              )}

              {/* user_name */}
              <div className={HeaderCss.userInfo}>
                {user && user.data?.user && (
                  <p>
                    <span className={HeaderCss.userName}>
                      {String(user.data.user.name)}
                    </span>
                    {user.data.user.student == 1 && ('교수')} 님 안녕하세요
                  </p>
                )}
              </div>
            </div>

            <div className="btnCol">
              {/* 우측 '개인정보' 버튼도 동일한 상태 전환 로직 적용 (URL 변경 없음) */}
              <button
                onClick={() => { setCurrentPage('개인정보'); closeAllSubMenus(); }}
                title="개인정보"
                className={HeaderCss.userInfoBtn}
              >
                개인정보
              </button>

              <button
                onClick={logout}
                title="로그아웃"
                className={HeaderCss.logoutBtn}
              >
                로그아웃
              </button>
            </div>
          </div>
        )}

        {/* 비로그인 상태에서는 렌더 없음 */}
        {!isAuthenticated && null}
      </header>

      {/* 모바일 메뉴: 로그인시에만 노출 */}
      {isAuthenticated && (
        <div className={HeaderCss.mobMenu}>
          <ul>
            <li onClick={() => { setCurrentPage("총장 인사"); closeAllSubMenus(); }}>학교소개</li>
            <li onClick={() => { setCurrentPage("학사공지"); closeAllSubMenus(); }}>커뮤니티</li>
            {/* 마이페이지 → 수강중인 과목으로 진입 */}
            <li onClick={() => { setCurrentPage("시설물 예약"); closeAllSubMenus(); }}>시설&문의</li>
            <li onClick={() => { setCurrentPage("수강중인 과목"); closeAllSubMenus(); }}>마이페이지</li>
            <li onClick={() => { setCurrentPage("수강신청"); closeAllSubMenus(); }}>수강신청</li>
          </ul>
        </div>
      )}
    </>
  );
}

export default Header;
