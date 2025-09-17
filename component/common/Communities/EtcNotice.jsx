//"기타공지" 전용 페이지 래퍼
import React, { useState, useEffect } from "react";
import NoticeLayout from "../notices/NoticeLayout";
import "../../../css/Communities/Notices.css";
import NoticeList from "../notices/NoticeList";

export default function EtcNotice({currentPage,setCurrentPage}) {
    const [page,setPage] = useState(1);
    useEffect(() => {setPage(1);}, [currentPage]); //탭 바뀌면 1 페이지로

    return(
        <NoticeLayout currentPage={currentPage} setCurrentPage={setCurrentPage}>
            <h2>기타공지 테스트 출력</h2>
            <NoticeList
              category="etc"
              page={page}
              size={10}
              onPageChange={setPage}
              />
        </NoticeLayout>
    );
}