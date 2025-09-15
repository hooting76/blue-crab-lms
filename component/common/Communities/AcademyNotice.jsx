//"학사공지" 전용 페이지 래퍼
//NoticeLayout에 카테고리 고정 + NoticeList 표시
import NoticeLayout from "../notices/NoticeLayout";
import NoticeList from "../notices/NoticeList";
import NoticeTabs from "../notices/NoticeTabs";



export default function AcademyNotice() {
    return(
        <NoticeLayout>
        <NoticeList category="academy" />
        </NoticeLayout>
    );
}

// import "../../css/Communities/AcademyNotice.css"



