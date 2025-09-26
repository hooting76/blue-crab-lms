//"기타공지" 전용 페이지 래퍼
import React, { useState, useEffect } from "react";
import NoticeLayout from "../notices/NoticeLayout";
import NoticeList from "../notices/NoticeList";

export default function EtcNotice({currentPage,setCurrentPage}) {
    const [page,setPage] = useState(1);
    useEffect(() => {setPage(1);}, [currentPage]); //탭 바뀌면 1 페이지로
    useEffect(() => {
            if (currentPage !== "기타공지" && currentPage !== "Admin 공지 작성")
              setCurrentPage("기타공지");
          }, [currentPage, setCurrentPage]);
        
          if (currentPage === "Admin 공지 작성") {
            return <AdminNoticeWritingPage currentPage={currentPage} setCurrentPage={setCurrentPage} />;
          }
    
    return(
        <NoticeLayout currentPage={currentPage} setCurrentPage={setCurrentPage}>
            <NoticeList
              boardCode={2}
              page={page}
              size={10}
              onPageChange={setPage}
              onWrite={() => setCurrentPage("Admin 공지 작성")}
              />
        </NoticeLayout>
    );
}