import { useEffect, useState, useRef } from 'react';
import CalendarComp from '../common/Calendar/CalendarComp';
import Aside from '../common/Aside';
import { getNoticesByCode } from '../api/noticeAPI';
import UserDashCss from '../../css/modules/UserDashboard.module.css';

// firebase  service-worker config
import pushNotificationManager from '../../firebase/PushNotification';

function UserDashboard() {
  const [boardList, setBoardList] = useState([]); 
  //firebase start========================
    useEffect(() => {
      if(!sessionStorage.getItem('noRegister')){
        try {
          const pushManager = pushNotificationManager;
          pushManager.initialize();          
        } catch (error) {
          console.log('푸시 알림 초기화 실패', error);
        }
      }
    }, []);
  //firebase end=========================== 

  // board init=============
  useEffect(() => {
    const fetchNotices = async() => {
      try {
        const accessToken = localStorage.getItem('accessToken');
        const response = await getNoticesByCode(accessToken, 0, 0, 5);
        const notices = Array.isArray(response.content) ? response.content : [];

        const sortedNotices = notices.sort((a, b) => Number(b.boardIdx) - Number(a.boardIdx));
        setBoardList(sortedNotices);
      } catch (error) {
        console.log('공지사항 로딩 에러', error);
      }
    };
    fetchNotices();
  },[])

  const decodeBase64 = (str) => {
    try {
      const cleanStr = str.replace(/\s/g, '');
      const binary = atob(cleanStr);
      const decode = decodeURIComponent(Array.prototype.map.call(binary, (data) =>  '%' + ('00' + data.charCodeAt(0).toString(16)).slice(-2)).join(''));
      return decode;
    } catch (error) {
      console.error('base64 디코딩 실패:', error);
      return '제목 없음';
    };
  };
  // board end=============
  
  return (
    <>
      <main className={UserDashCss.main}>
          <section className={UserDashCss.notice}>
            <div className={UserDashCss.noticeFirst}>
              <h4>학사공지</h4>
              <ul>
                {boardList.map((notice) => (
                  <li 
                    key={notice.boardIdx}
                    onClick={() => {
                      document.getElementById('cmntP').click();
                      setTimeout(() => {
                        const rows = document.querySelectorAll('.notice-table > tbody > tr');
                        rows.forEach(row => {
                          if (row.textContent.includes(notice.boardIdx)) {
                            row.click();
                          };
                        });
                      }, 300);
                    }}>
                    {decodeBase64(notice.boardTitle)}
                  </li>
                ))}
              </ul>
            </div>
            <div className={UserDashCss.noticeSecond}>
              <h4>취업정보</h4>
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