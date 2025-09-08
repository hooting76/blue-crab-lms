import "../../css/Introductions/Introduction.css"

import { useNavigate, Outlet, useLocation } from "react-router-dom";
import { useState, useEffect } from 'react';

function Introduction() {
    const [currentIntroduction, setCurrentIntroduction] = useState("");
    const navigate = useNavigate();
    const location = useLocation();

    useEffect(() =>{
        switch (location.pathname) {
            case "/Introduction/PresidentSaysHi":
                setCurrentIntroduction("총장 인사");
                break;
            case "/Introduction/WayHere":
                setCurrentIntroduction("오시는 길");
                break;
            case "/Introduction/Organization":
                setCurrentIntroduction("학교 조직도");
                break;
            case "/Introduction/BlueCrabHistory":
                setCurrentIntroduction("연혁");
                break;
            default:
                setCurrentIntroduction("");
                break;
        }
    }, [location.pathname]);

    const toPresidentSaysHi = () => {navigate("/Introduction/PresidentSaysHi")};
    const toWayHere = () => {navigate("/Introduction/WayHere")};
    const toOrganization = () => {navigate("/Introduction/Organization")};
    const toBlueCrabAcademyHistory = () => {navigate("/Introduction/BlueCrabHistory")};

    return(
        <main>
            <div className="introductiontitle">
                <div id="schoolIntroductionText">학교 소개</div><br/>
                <span onClick={toPresidentSaysHi} style={{cursor: "pointer", fontWeight: currentIntroduction === "총장 인사" ? "bold" : "normal"}}>총장 인사</span>
                <span onClick={toWayHere} style={{cursor: "pointer", fontWeight: currentIntroduction === "오시는 길" ? "bold" : "normal"}}>오시는 길</span>
                <span onClick={toOrganization} style={{cursor: "pointer", fontWeight: currentIntroduction === "학교 조직도" ? "bold" : "normal"}}>학교 조직도</span>
                <span onClick={toBlueCrabAcademyHistory} style={{cursor: "pointer", fontWeight: currentIntroduction === "연혁" ? "bold" : "normal"}}>연혁</span>
            </div>
            <div>
                <Outlet/>
            </div>
        </main>
    )

}

export default Introduction;