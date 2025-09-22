import { NavLink, Outlet, useLocation } from "react-router-dom";
import CommunitySidebar from "../notices/CommunitySidebar";
import { useMemo } from "react";

export default function Facility() {
    const location = useLocation();
    const current = useMemo(() => {
    const p = location.pathname;
    if (p.startsWith("/Facility/FacilityRequest")) return "신청폼";
    if (p.startsWith("/Facility/MyFacilityRequests")) return "나의 신청목록";
    if (p.startsWith("/Facility/ReadingRoom"))    return "열람실 신청";
    return "신청폼";
  }, [location.pathname]);

  return(
          <main className="community-page"> {/*공지커뮤니티 공통 컨테이너*/}
              {/* 상단배너 */}
              <section className="community-banner"><h2>커뮤니티</h2></section>
              {/* 카테고리 탭 */}
              <div className="tab-menu" aria-label="공지 카테고리">
                  <NavLink to="FacilityRequest" end className={({isActive}) => isActive ? "active" : ""}>신청폼</NavLink>
                  <NavLink to="MyFacilityRequests"  className={({isActive}) => isActive ? "active" : ""}>나의 신청목록</NavLink>
                  <NavLink to="ReadingRoom"     className={({isActive}) => isActive ? "active" : ""}>열람실 신청</NavLink>
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