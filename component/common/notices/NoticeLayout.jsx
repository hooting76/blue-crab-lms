//공지 공용 틀: 배너 + 탭 + 좌측 본문(children) + 우측 사이드(커뮤니티 메뉴)
import React from "react";
import { useLocation } from "react-router-dom";
import NoticeTabs from "../notices/NoticeTabs";
import CommunitySidebar from "../notices/CommunitySidebar";
import "../../../css/Communities/Notices.css";
import "../../../css/Communities/Community.css";

/** 
* props:
*  - currentPage, setCurrentPage
*  - children : 좌측 본문(테이블/리스트)
*/

export default function NoticeLayout({currentPage, setCurrentPage, children}){
return(
<div className="notice-page">
    <div className="banner" aria-label="공지배너" />
    <NoticeTabs currentPage={currentPage} setCurrentPage={setCurrentPage} />
    <div className="grid">
    <main className="left" aria-label="공지본문">
        {children}
        </main>
        <aside className="right">
            <CommunitySidebar currentPage={currentPage} setCurrentPage={setCurrentPage}/>
        </aside>
    </div>
</div>
);
}
