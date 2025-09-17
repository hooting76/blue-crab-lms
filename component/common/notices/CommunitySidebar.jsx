// 우측 커뮤니티 메뉴
import React, { useEffect, useState, useMemo } from "react";
import "../../../css/Communities/Notices.css";


export default function CommunitySidebar({ currentPage, setCurrentPage }) {

    const sectionOf = useMemo(() => {
        const isNotice = ["학사공지", "행정공지", "기타공지"].includes(currentPage);
        const isFaq = currentPage === "FAQ";
        const isFacilities = ["시설신청", "나의 신청목록", "열람실 신청"].includes(currentPage);

        return{
            notice: isNotice,
            faq: isFaq,
            facility: isFacilities,
        };
    }, [currentPage]);

    //아코디언 열림 상태
    const [open, setOpen] = useState({
        notice: true,
        faq: false,
        facility: false,
    });

    // currentPage가 바뀌면 해당 섹션을 자동으로 펼침
    useEffect(()=>{
        setOpen((s)=> ({
            notice: sectionOf.notice || s.notice,
            faq: sectionOf.faq || s.faq,
            facility: sectionOf.facility || s.facility,
        }));
    }, [sectionOf.notice, sectionOf.faq, sectionOf.facility]);
    
    //공통 아이템 버튼
    const Item = ({ name, pageValue}) => {
        const value = pageValue ?? name;  // '시설신청'처럼 라벨과 값이 다를 때 pageValue 사용
        const active = currentPage === value;

        return(
        <button
            type="button"
            className={`side-item ${active ? "is-active" : ""}`}
            onClick={() => setCurrentPage(value)}
            >
        </button>
        );
    };

    return (
    <aside className="side-card" aria-label="커뮤니티 사이드바">
      <h3 className="side-title">커뮤니티</h3>

      {/* 공지사항 */}
      <section className="acc-item">
        <button
            className="acc-header"
            aria-expanded={open.notice}
            onClick={() => setOpen((s) => ({ ...s, notice: !s.notice }))}
        >
          공지사항
        </button>
        {open.notice && (
          <div className="acc-panel">
            <Item name="학사공지" />
            <Item name="행정공지" />
            <Item name="기타공지" />
          </div>
        )}
      </section>

      {/* FAQ */}
      <section className="acc-item">
        <button
            className="acc-header"
            aria-expanded={open.faq}
            onClick={() => setOpen((s) => ({ ...s, faq: !s.faq }))}
        >
          FAQ
        </button>
        {open.faq && (
          <div className="acc-panel">
            <Item name="FAQ" />
          </div>
        )}
      </section>

      {/* 시설 & 문의 */}
      <section className="acc-item">
        <button
          className="acc-header"
          aria-expanded={open.facility}
          onClick={() => setOpen((s) => ({ ...s, facility: !s.facility }))}
        >
          시설 & 문의
        </button>
        {open.facility && (
          <div className="acc-panel">
            {/* 라벨과 currentPage 값(컴포넌트 매핑용)을 분리하고 싶으면 pageValue로 전달 */}
            <Item name="신청폼" pageValue="시설신청" />
            <Item name="나의 신청목록" pageValue="나의 신청목록" />
            <Item name="열람실 신청" pageValue="열람실 신청" />
          </div>
        )}
      </section>
    </aside>
  );
}