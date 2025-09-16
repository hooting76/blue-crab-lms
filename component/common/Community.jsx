//커뮤니티 섹션의 페이지 셸(공통 레이아웃)
//[구성] 상단 배너 + 탭(학사,행정,기타) + 좌(본문/Outlet)/우(사이드메뉴)
import "../../css/Communities/Community.css" //공통 css(배너/탭/그리드/테이블/사이드)

import { NavLink, Outlet, useLocation } from "react-router-dom";
import CommunitySidebar from "../notices/CommunitySidebar";
import { useMemo } from "react";

export default function Community() {
    const location = useLocation();
    const current = useMemo(() => {
    const p = location.pathname;
    if (p.startsWith("/Community/AcademyNotice")) return "학사공지";
    if (p.startsWith("/Community/notice-admin")) return "행정공지";
    if (p.startsWith("/Community/EtcNotice"))    return "기타공지";
    return "학사공지";
  }, [location.pathname]);

    return(
        <main className="community-page"> {/*공지커뮤니티 공통 컨테이너*/}
            {/* 상단배너 */}
            <section className="community-banner"><h2>커뮤니티</h2></section>
            {/* 카테고리 탭 */}
            <div className="tab-menu" aria-label="공지 카테고리">
                <NavLink to="AcademyNotice" end className={({isActive}) => isActive ? "active" : ""}>학사공지</NavLink>
                <NavLink to="notice-admin"  className={({isActive}) => isActive ? "active" : ""}>행정공지</NavLink>
                <NavLink to="EtcNotice"     className={({isActive}) => isActive ? "active" : ""}>기타공지</NavLink>
            </div>

            {/* 좌측 본문 + 우측 사이드 2단 레이아웃 */}
            <section className="grid">
                {/* 좌측: 공지 본문(목록 or 상세) */}
                <section className="left" aria-label={`${current || "공지"} 본문`}>
                    <Outlet />
                </section>

                {/* 우측: 커뮤니티 사이드바 */}
                <aside className="right" aria-label="사이드 메뉴">
                    <CommunitySidebar />
                </aside>
            </section>
        </main>
    );
}

