//"행정공지" 전용 페이지 래퍼

import React, { useState, useEffect } from "react";
import "../notices/NoticeLayout";
import NoticeLayout from "../notices/NoticeLayout";
import NoticeList from "../notices/NoticeList";

//import "../../css/Communities/AdminNotice.css"

export default function AdminNotice({currentPage,setCurrentPage}) {
    const [page,setPage] = useState(1);
    useEffect(() => {setPage(1);}, [currentPage]); //탭 바뀌면 1 페이지로
    useEffect(() => {
        if (currentPage !== "행정공지" && currentPage !== "Admin 공지 작성")
          setCurrentPage("행정공지");
      }, [currentPage, setCurrentPage]);
    
      if (currentPage === "Admin 공지 작성") {
        return <AdminNoticeWritingPage currentPage={currentPage} setCurrentPage={setCurrentPage} />;
      }

    return(
        <NoticeLayout currentPage={currentPage} setCurrentPage={setCurrentPage}>
            <h2>행정공지 테스트 출력</h2>
            <NoticeList
              boardCode="1"
              page={page}
              size={10}
              onPageChange={setPage}
              onWrite={() => setCurrentPage("Admin 공지 작성")}
              />
        </NoticeLayout>
    );
}