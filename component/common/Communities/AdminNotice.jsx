//"행정공지" 전용 페이지 래퍼

import NoticeLayout from "../notices/NoticeLayout";
import NoticeList from "../notices/NoticeList";
import NoticeTabs from "../notices/NoticeTabs";
//import "../../css/Communities/AdminNotice.css"

export default function AdminNotice() {
    return(
        <NoticeLAyout fixedCategory="행정공지">
        <NoticeList/>
        </NoticeLAyout>
    );
}

