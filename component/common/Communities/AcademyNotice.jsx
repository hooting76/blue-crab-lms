//"학사공지" 전용 페이지 래퍼
//NoticeLayout에 카테고리 고정 + NoticeList 표시
import { useState, useEffect } from 'react';
import NoticeLayout from "../notices/NoticeLayout";
import NoticeList from "../notices/NoticeList";
import NoticeTabs from "../notices/NoticeTabs";



export default function AcademyNotice() {
    return(
        <NoticeLayout fixedCategory="학사공지">
        <NoticeList/>
        </NoticeLayout>
    );
}

// import "../../css/Communities/AcademyNotice.css"



