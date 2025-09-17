//공지 카테고리 탭
//각 카테고리별 공지 페이지로 이동
import React, { useState } from "react";
import "../../../css/Communities/Notices.css";

const TABS = ["학사공지", "행정공지", "기타공지"];

export default function NoticeTabs({ currentPage, setCurrentPage}) {
    const [open,setOpen] = useState(false);

    const onSelect = (name) => {
        setCurrentPage(name);
        setOpen(false);
    };

    return (
        <div className="tabs-wrap">
        {/* 드롭다운(모바일/좁은화면) */}
        <div className="tabs-dropdown">
            <button
                type="button"
                className="tabs-dropdown-btn"
                aria-haspopup="listbox"
                aria-expanded={open}
                onClick={()=>setOpen((v) => !v)}
            >
                {currentPage || "공지선택"}
                </button>

            {open && (
                <ul className="tabs-dropdown-list" role="listbox">
                    {TABS.map((name) => (
                        <li key={name}>
                            <button
                                type="button"
                                role="option"
                                aria-selected={currentPage === name}
                                ClassName=
                                {`tabs-dropdown-item ${currentPage === name ? "is-active" : ""}`}
                                onClick={() => onSelect(name)}
                            >
                                {name}
                            </button>
                        </li>
                    ))}
                </ul>
            )}
        </div>
        {/* 가로탭(데스크탑/넓은화면) */}
        <div className="tabs-bar" role="tablist" aria-label="공지카테고리">
            {TABS.map((name) => (
                <button
                    key={name}
                    type="button"
                    role="tab"
                    aria-selected={currentPage === name}
                    className={`tabs-bar-item ${currentPage === name ? "is-active" : ""}`}
                    onClick={() => onSelect(name)}
                >
                    {name}
                </button>
            ))}
        </div>
        </div>
    );
}