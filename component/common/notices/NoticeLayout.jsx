//공지 공용 틀: 배너 + 탭 + 좌측 본문(children) + 우측 사이드(커뮤니티 메뉴)
import React, { useEffect,useMemo } from "react";
import NoticeTabs from "../notices/NoticeTabs";
import CommunitySidebar from "../notices/CommunitySidebar";
import "../../../css/Communities/Notice-ui.css";
import "../../../css/Communities/Notice-nav.css";

const NOTICE_PAGES = ["학사공지", "행정공지", "기타공지"];

export default function NoticeLayout({ 
    currentPage ="학사공지", 
    setCurrentPage = () => {}, 
    children,
 }) {
  // 공지 전용 레이아웃이므로 보정
  useEffect(() => {
   if (!NOTICE_PAGES.includes(currentPage)) setCurrentPage("학사공지");
  }, [currentPage, setCurrentPage]);

  const isNotice = NOTICE_PAGES.includes(currentPage);
  const bannerTitle = useMemo(() => (isNotice ? currentPage : "학사공지"), [isNotice, currentPage]);

  return (
    <div className="notice-page">
      {/* 배너 (현재 공지 카테고리 표기) */}
      <div className="banner" aria-label="공지 배너">
        <h2>{bannerTitle}</h2>
      </div>

      {/* 공지 세부탭: 공지 화면일 때만 노출 */}
      {isNotice && (
        <NoticeTabs currentPage={currentPage} setCurrentPage={setCurrentPage} />
      )}

      {/* 본문 2단 레이아웃 */}
      <div className="grid">
        <main className="left">{children}</main>
        <aside className="right">
          <CommunitySidebar currentPage={currentPage} setCurrentPage={setCurrentPage} />
        </aside>
      </div>
    </div>
  );
}