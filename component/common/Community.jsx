import "../../css/Communities/Community.css"

import { useNavigate, Outlet, useLocation } from "react-router-dom";
import { useState, useEffect } from 'react';

function Community() {
    const [currentCommunity, setCurrentCommunity] = useState("");
    const navigate = useNavigate();
    const location = useLocation();

    useEffect(() =>{
        switch (location.pathname) {
            case "/Community/AcademyNotice":
                setCurrentCommunity("학사공지");
                break;
            case "/Community/AdminNotice":
                setCurrentCommunity("행정공지");
                break;
            case "/Community/EtcNotice":
                setCurrentCommunity("기타공지");
                break;
            default:
                setCurrentCommunity("");
                break;
        }
    }, [location.pathname]);

    const toAcademyNotice = () => {navigate("/Community/AcademyNotice")};
    const toAdminNotice = () => {navigate("/Community/AdminNotice")};
    const toEtcNotice = () => {navigate("/Community/EtcNotice")};

    return(
        <main>
            <div className="communitytitle">
                <div id="schoolCommunityText">커뮤니티</div><br/>
                <span onClick={toAcademyNotice} style={{cursor: "pointer", fontWeight: currentCommunity === "학사공지" ? "bold" : "normal"}}>학사공지</span>
                <span onClick={toAdminNotice} style={{cursor: "pointer", fontWeight: currentCommunity === "행정공지" ? "bold" : "normal"}}>행정공지</span>
                <span onClick={toEtcNotice} style={{cursor: "pointer", fontWeight: currentCommunity === "기타공지" ? "bold" : "normal"}}>기타공지</span>
            </div>
            <div>
                <Outlet/>
            </div>
        </main>
    )

}

export default Community;