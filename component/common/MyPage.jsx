import "../../css/MyPages/MyPage.css"

import { useNavigate, Outlet, useLocation } from "react-router-dom";
import { useState, useEffect } from 'react';

function MyPage() {
    const [currentMyPage, setCurrentMyPage] = useState("");
    const navigate = useNavigate();
    const location = useLocation();

    useEffect(() =>{
        switch (location.pathname) {
            case "/MyPage/ClassAttendingList":
                setCurrentMyPage("수강중인 과목");
                break;
            case "/MyPage/ClassAttendingProgress":
                setCurrentMyPage("과목별 진행사항");
                break;
            case "/MyPage/ClassAttendingNotice":
                setCurrentMyPage("수강과목 공지사항");
                break;
            case "/MyPage/Consult":
                setCurrentMyPage("실시간 상담");
                break;
            default:
                setCurrentMyPage("");
                break;
        }
    }, [location.pathname]);

    const toClassAttendingList = () => {navigate("/MyPage/ClassAttendingList")};
    const toClassAttendingProgress = () => {navigate("/MyPage/ClassAttendingProgress")};
    const toClassAttendingNotice = () => {navigate("/MyPage/ClassAttendingNotice")};
    const toConsult = () => {navigate("/MyPage/Consult")};

    return(
        <main>
            <div className="mypagetitle">
                <div id="schoolMyPageText">마이페이지</div><br/>
                <span onClick={toClassAttendingList} style={{cursor: "pointer", fontWeight: currentMyPage === "수강중인 과목" ? "bold" : "normal"}}>수강중인 과목</span>
                <span onClick={toClassAttendingProgress} style={{cursor: "pointer", fontWeight: currentMyPage === "과목별 진행사항" ? "bold" : "normal"}}>과목별 진행사항</span>
                <span onClick={toClassAttendingNotice} style={{cursor: "pointer", fontWeight: currentMyPage === "수강과목 공지사항" ? "bold" : "normal"}}>수강과목 공지사항</span>
                <span onClick={toConsult} style={{cursor: "pointer", fontWeight: currentMyPage === "실시간 상담" ? "bold" : "normal"}}>실시간 상담</span>
            </div>
            <div>
                <Outlet/>
            </div>
        </main>
    )

}

export default MyPage;