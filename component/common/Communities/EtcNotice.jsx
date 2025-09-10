//"기타공지" 전용 페이지 래퍼
import NoticeLayout from "../notices/NoticeLayout";
import NoticeList from "../notices/NoticeList";
import NoticeTabs from "../notices/NoticeTabs";


export default function EtcNotice() {
    return(
        <NoticeLayout fixedCategory="기타공지">
        <NoticeList/>
        </NoticeLayout>
    );
}