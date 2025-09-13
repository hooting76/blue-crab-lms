//공지 카테고리 탭
//로그인 권한과 무관하게 모두 접근 가능(카테고리 전환만 담당)
//각 카테고리별 공지 페이지로 이동
import React from "react";
import { useNavigate } from "react-router-dom";
// import NoticeTabsCss from '../css/modules/NoticeTabs.module.css';

const Tabs = [
  { key: "academy", label: "학사공지", path: "/community/academy" },
  { key: "admin", label: "행정공지", path: "/community/notice-admin" },
  { key: "etc", label: "기타공지", path: "/community/etc" },
];

export default function NoticeTabs({ active = "academy" }) {
    const navigate = useNavigate();
    return(
        <div className="tab-menu">
            {Tabs.map(tab => (
                <button
                    key={tab.key}   
                    className={`tab ${tab.key === active ? "active" : ""}`}
                    onClick={() => navigate(tab.path)} //탭 클릭 시 해당 카테고리 페이지로 이동
                >
                    {tab.label}
                </button>
            ))}
        </div>    
    );
}
