//마이페이지 사이드탭
import React, { useState, useMemo, useEffect } from "react";
import "../../../css/Communities/Notice-nav.css"; // 기존 커뮤니티 스타일 재사용
import { UseUser } from "../../../hook/UseUser";

const pageToSection = (p) => {
    if (p === "개인정보") return "profile";
    if (p === "실시간 상담") return "consult";
    if (["수강/강의중인 과목", "과목별 진행사항", "수강/강의과목 공지사항"].includes(p)) return "classroom";
        return "classroom";
};

export default function MyPageSidebar({ currentPage, setCurrentPage }) {
    useMemo(() => pageToSection(currentPage), [currentPage]);
    const [open, setOpen] = useState({ profile: false, classroom: true, consult: false });

    useEffect(() => {
        const sec = pageToSection(currentPage);
    setOpen({ profile: false, classroom: false, consult: false, [sec]: true });
  }, [currentPage]);

  const {user} = UseUser();

    const toggle = (sec) =>
    setOpen((s) => ({ profile: false, classroom: false, consult: false, [sec]: !s[sec] }));

    const Item = ({ name, pageValue }) => {
        const display = name;
        const target = pageValue ?? display;
        const isActive = currentPage === target;
    return (
    <button
        type="button"
        className={`side-item ${isActive ? "is-active" : ""}`}
        aria-current={isActive ? "page" : undefined}
        onClick={() => setCurrentPage?.(target)}
    >
        {display}
    </button>
    );
  };

    return (
        <aside className="side-card" aria-label="마이페이지 사이드바">
            <h3 className="side-title">마이페이지</h3>

        {/* 개인정보 */}
        <section className="acc-item">
            <button
                className="acc-header"
                aria-expanded={open.profile}
                onClick={() => toggle("profile")}
            >
            <span>개인정보</span>
            <span className="acc-arrow" aria-hidden="true">{open.profile ? "▾" : "▸"}</span>
        </button>
        {open.profile && (
        <div className="acc-panel">
            <Item name="개인정보" />
        </div>
        )}
        </section>

        {/* 나의강의실 */}
        <section className="acc-item">
            <button
                className="acc-header"
                aria-expanded={open.classroom}
                onClick={() => toggle("classroom")}
            >
            <span>나의강의실</span>
            <span className="acc-arrow" aria-hidden="true">{open.classroom ? "▾" : "▸"}</span>
        </button>
        {open.classroom && (
            <div className="acc-panel">
            <Item name="수강/강의중인 과목" />
            <Item name="수강/강의과목 공지사항" />
            {user.data.user.userStudent === 1 && // 교수일 경우 강의 수정 페이지 진입 가능
                <>
                    <Item name="강의 수정"/>
                </>
            }
            {/* 과목별 진행사항은 리스트 클릭 시 노출되므로 메뉴에는 미표시 */}
        </div>
        )}
        </section>

        {/* 실시간 상담 */}
        <section className="acc-item">
            <button
                className="acc-header"
                aria-expanded={open.consult}
                onClick={() => toggle("consult")}
            >
        <span>실시간 상담</span>
        <span className="acc-arrow" aria-hidden="true">{open.consult ? "▾" : "▸"}</span>
        </button>
        {open.consult && (
            <div className="acc-panel">
            <Item name="실시간 상담" />
        </div>
        )}
        </section>
    </aside>
  );
}
