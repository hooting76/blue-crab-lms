//커뮤니티 섹션의 페이지 셸(공통 레이아웃)
//[구성] 상단 배너 + 탭(학사,행정,기타) + 좌(본문/Outlet)/우(사이드메뉴)
import "../../css/Communities/Community.css" //공통 css(배너/탭/그리드/테이블/사이드)

import { NavLink, Outlet, useLocation } from "react-router-dom";
import CommunitySidebar from "../notices/CommunitySidebar";
// 상태 전환을 위한 훅 + 시설 폼/FAQ 더미(또는 실제 컴포넌트)
import { useMemo, useState, useEffect } from "react";
import FacilityRequest from "../Facilities/FacilityRequest";
import FAQ from "../FAQ/FAQ"; 


export default function Community() {
    const location = useLocation();

    // 라우트 기반 현재 공지 탭(학사/행정/기타)
    const current = useMemo(() => {
    const p = location.pathname;
    if (p.startsWith("/Community/AcademyNotice")) return "학사공지";
    if (p.startsWith("/Community/notice-admin")) return "행정공지";
    if (p.startsWith("/Community/EtcNotice"))    return "기타공지";
    return "학사공지";
}, [location.pathname]);

    // 상태 전환 기반 현재 페이지 (공지/FAQ/시설 등)
    const [currentPage, setCurrentPage] = useState(currentRouteTab ?? "학사공지"); 

    // 헤더에서 navigate('/Community', { state: { page: '시설신청' } })로 들어온 경우 반영
    useEffect(() => {
    const p = location.state?.page ?? window.history.state?.page;
    if (p) setCurrentPage(p);
}, [location.state?.page]);

    //  공지 탭으로 라우트가 바뀌면 상태도 동기화
    useEffect(() => {
    if (currentRouteTab) setCurrentPage(currentRouteTab);
}, [currentRouteTab])

    // 시설 계열 여부
    const isFacility = ["시설신청","신청폼","나의 신청목록","열람실 신청"].includes(currentPage);
    
    // 커뮤니티 레이아웃 사용 안 하고, 시설은 자체 레이아웃으로 바로 렌더
    if (isFacility) {
    return (
        <FacilityRequest
        currentPage={currentPage}
        setCurrentPage={setCurrentPage}
        />
    );
  }
    //공지/FAQ 등 커뮤니티 레이아웃 쓰는 경우만 기존대로
    const isNotice = ["학사공지","행정공지","기타공지"].includes(currentPage);
    const isFAQ = currentPage === "FAQ";


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
                <section className="left" aria-label={`${currentPage || "본문"}`}>
                {isNotice   && <Outlet /> }
                {isFAQ      && <FAQ /> }
                {isFacility && <FacilityRequest currentPage={currentPage} setCurrentPage={setCurrentPage} />}
                </section>

                {/* 우측: 커뮤니티 사이드바 */}
                <aside className="right" aria-label="사이드 메뉴">
                    <CommunitySidebar
                    currentPage={currentPage}          
                    setCurrentPage={setCurrentPage}    
                    />
                </aside>
            </section>
        </main>
    );
}

