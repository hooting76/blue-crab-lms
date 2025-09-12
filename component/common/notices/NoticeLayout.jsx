//공지 공용 틀: 배너 + 탭 + 좌측 본문(children) + 우측 사이드(커뮤니티 메뉴)
import React from "react";
import { useState, useEffect } from 'react';
import { Outlet, useLocation } from "react-router-dom";
//import { UseUser } from "../../../hook/UseUser";
import NoticeTabs from "../notices/NoticeTabs";
import CommunitySidebar from "../notices/CommunitySidebar";
import "../../../css/Communities/Notices.css";


export default function NoticeLayout(){
    const { pathname } = useLocation();
    const active =
    pathname.startsWith("/community/notice-admin") ? "admin" :
    pathname.startsWith("/community/etc") ? "etc" : "academy"; //기본값 학사공지

    return(
        <main className="notice-page">
            {/* 상단배너 */}
            <section className="notice-banner"><h2>공지사항</h2></section>
            
            {/* 카테고리 탭 */}
            {/* active: academy, admin, etc */}
            <NoticeTabs active={active} />

            {/* 좌측 본문 + 우측 사이드 2단 레이아웃 */}
            <section className="grid"> 
                
                {/* 좌측: 공지 본문(목록 or 상세) */}
                <section className="left">
                    <Outlet /> 
                </section>

                {/* 우측: 커뮤니티 사이드바 */} 
                <aside className="right">
                    <CommunitySidebar />
                </aside>
            </section>
            </main>
    );
}