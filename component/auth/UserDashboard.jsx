import React from 'react';
import { UseUser } from '../../hook/UseUser';
import CalendarComp from '../common/Calendar/CalendarComp';

import UserDashCss from '../../css/modules/UserDashboard.module.css';

function UserDashboard() {
  const { user, logout } = UseUser();

  return (
    <>
      <main className={UserDashCss.main}>
        {/* notice wrap */}
        <section className={UserDashCss.notice}>
          <div className={UserDashCss.noticeFirst}>
            <h4>공지사항</h4>
            <ul>
              <li>1. asdf</li>
              <li>2. qwer</li>
            </ul>
          </div>
          <div className={UserDashCss.noticeSecond}>
            <h4>취업정보</h4>
            <ul>
              <li>1. asdf</li>
              <li>2. qwer</li>
            </ul>
          </div>
        </section>
        {/* notice wrap end*/}

        {/* calendar wrap */}
        <section className={UserDashCss.calendar}>
          <CalendarComp />
        </section>
        {/* calendar wrap end*/}
      </main>
      <aside className={UserDashCss.aside}>
        <div className={UserDashCss.userInfo}>
          asdf
        </div>
        <div className={UserDashCss.service}>
          qwer
        </div>
        <div className={UserDashCss.quickMenu}>
          zxcv
        </div>
      </aside>    
    </>
  );
}

export default UserDashboard;