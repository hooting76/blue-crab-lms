// 우측 커뮤니티 메뉴
import React from "react";
import { NavLink } from "react-router-dom";


export default function CommunitySidebar() {
    return(
        <nav className="community">
            <h3>커뮤니티</h3>
            <ul>
              <li><NavLink to="/community/academy" className={({isActive}) => isActive ? "active" : ""}>학사공지</NavLink></li>
              <li><NavLink to="/community/notice-admin"   className={({isActive}) => isActive ? "active" : ""}>행정공지</NavLink></li>
              <li><NavLink to="/community/etc"     className={({isActive}) => isActive ? "active" : ""}>기타공지</NavLink></li>
            </ul>
        </nav>
    );
}