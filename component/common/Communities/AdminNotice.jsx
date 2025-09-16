//"행정공지" 전용 페이지 래퍼

import NoticeLayout from "../notices/NoticeLayout";
import NoticeList from "../notices/NoticeList";
import NoticeTabs from "../notices/NoticeTabs";
//import "../../css/Communities/AdminNotice.css"

export default function AdminNotice() {
    return(
       <NoticeLayout>
         <h2>행정공지 테스트 출력</h2>
      <NoticeList category="admin" />
    </NoticeLayout>
    );
}

