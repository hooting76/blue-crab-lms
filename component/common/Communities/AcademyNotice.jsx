//"학사공지" 전용 페이지 래퍼
//NoticeLayout에 카테고리 고정 + NoticeList 표시
import React, { useState, useEffect } from "react";
import "../notices/NoticeLayout"; // side-effect import 방지용 X,아래처럼 정확히 임포트
import NoticeLayout from "../notices/NoticeLayout";
import NoticeList from "../notices/NoticeList";

export default function AcademyNotice({currentPage,setCurrentPage}) {
    const [page,setPage] = useState(1);
    useEffect(() => {setPage(1);}, [currentPage]); //탭 바뀌면 1 페이지로

    return(
        <NoticeLayout currentPage={currentPage} setCurrentPage={setCurrentPage}>
            <h2>학사공지 테스트 출력</h2>
            <NoticeList
              category="academy"
              page={page}
              size={10}
              onPageChange={setPage}
              />
        </NoticeLayout>
    );
}



