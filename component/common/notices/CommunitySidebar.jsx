// 우측 커뮤니티 사이드메뉴
import React, { useState, useMemo, useEffect } from "react";
import "../../../css/Communities/Notice-nav.css";

// 페이지 이름 → 섹션 매핑 함수
const pageToSection = (p) => {
  if (["학사공지", "행정공지", "기타공지"].includes(p)) return "notice";
  if (p === "FAQ") return "faq";
  // 시설 & 문의 섹션 페이지들 매핑
  if (["시설물 예약","내 예약 현황", "신청폼", "나의 신청목록", "열람실 신청"].includes(p)) return "facility";
  return null;
};

function CommunitySidebar({ currentPage, setCurrentPage }) {
  // currentPage가 바뀔 때 어떤 섹션인지 계산해두지만, 실제 열고 닫는 동작은 useEffect에서 처리
  useMemo(() => pageToSection(currentPage), [currentPage]);

  // 아코디언 열림 상태
  const [open, setOpen] = useState({ notice: false, faq: false, facility: false });

  useEffect(() => {
    const sec = pageToSection(currentPage);
    if (sec) {
      setOpen({ notice: false, faq: false, facility: false, [sec]: true });
    }
  }, [currentPage]);

  // 아코디언 토글 함수
  const toggle = (sec) =>
    setOpen((s) => ({ notice: false, faq: false, facility: false, [sec]: !s[sec] }));

//공통 아이템 (label/name/text 모두 허용, page/value로 실제 페이지명 지정 가능)
  const Item = (props) => {
    const { text, label, name, page, value, pageValue } = props;
    const display = text ?? label ?? name; // 버튼에 표시할 텍스트
    const target = page ?? value ?? pageValue ?? display; // currentPage로 세팅할 실제 값
    const isActive = currentPage === target;

    return (
      <button
        type="button"
        className={`side-item ${isActive ? "is-active" : ""}`}
        aria-current={isActive ? "page" : undefined}
        onClick={() => typeof setCurrentPage === "function" && setCurrentPage(target)}
      >
        {display}
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
          onClick={() => toggle("notice")}
        >
          <span>공지사항</span>
          <span className="acc-arrow" aria-hidden="true">
            {open.notice ? "▾" : "▸"}
          </span>
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
          onClick={() => toggle("faq")}
        >
          <span>FAQ</span>
          <span className="acc-arrow" aria-hidden="true">
            {open.faq ? "▾" : "▸"}
          </span>
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
          onClick={() => toggle("facility")}
        >
          <span>시설 & 문의</span>
          <span className="acc-arrow" aria-hidden="true">
            {open.facility ? "▾" : "▸"}
          </span>
        </button>

        {open.facility && (
          <div className="acc-panel">
            <Item name="시설물 예약" pageValue="시설물 예약" />
            <Item name="내 예약 현황" pageValue="내 예약 현황" />
            <Item name="열람실 신청" pageValue="열람실 신청" />
          </div>
        )}
      </section>
    </aside>
  );
}

export default CommunitySidebar;
