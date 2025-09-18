//공지 카테고리 탭
//각 카테고리별 공지 페이지로 이동
import React from "react";
import "../../../css/Communities/Notice-nav.css";

const TABS = ["학사공지", "행정공지", "기타공지"];

export default function NoticeTabs({ currentPage, setCurrentPage}) {
    const go = (name) =>{
        if(typeof setCurrentPage === "function") {setCurrentPage(name);
        window.requestAnimationFrame(() => window.scrollTo({ top: 0, behavior: "smooth" }));
    }
  };
    
    return (
        <div className="tab-menu" role="tablist" aria-label="공지 카테고리">
            {TABS.map((name) => (
                <button
                    key={name}
                    type="button"
                    role="tab"
                    aria-selected={currentPage === name}
                    className={`tab ${currentPage === name ? "active" : ""}`}
                    onClick={() => go(name)}  // ← URL 변화 없음
                >
                {name}
        </button>
        ))}
    </div>
    );
}