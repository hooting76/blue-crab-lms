import { useEffect } from 'react';
import CalendarComp from '../common/Calendar/CalendarComp';
import Aside from '../common/Aside';

import UserDashCss from '../../css/modules/UserDashboard.module.css';

// firebase  service-worker config
import PushNotificationManager from '../../firebase/PushNotification';

function UserDashboard() {

  //firebase start========================
    useEffect(() => {
        const pushManager = new PushNotificationManager();
        pushManager.initialize();
    }, []);
  //firebase end===========================  

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
        <Aside />
      </aside>    
    </>
  );
}

export default UserDashboard;