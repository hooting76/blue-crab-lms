import "../../css/MyPages/MyPage.css"
import ClassAttendingList from "./MyPages/ClassAttendingList";
import ClassAttendingNotice from "./MyPages/ClassAttendingNotice";
import Consult from "./MyPages/Consult";
import UserDashboard from "../auth/UserDashboard";

function MyPage({currentPage, setCurrentPage}) {

    const toClassAttendingList = () => {setCurrentPage("수강중인 과목")};
    const toClassAttendingNotice = () => {setCurrentPage("수강과목 공지사항")};
    const toConsult = () => {setCurrentPage("실시간 상담")};

    const renderPage = () => {
        switch (currentPage) {
            case "수강중인 과목":
                return <ClassAttendingList/>;
            case "수강과목 공지사항":
                return <ClassAttendingNotice/>;
            case "실시간 상담":
                return <Consult/>;
            default:
                return <UserDashboard/>; // 기본 페이지
        }
    };

    return(
        <main>
            <div className="mypagetitle">
                <div id="schoolMyPageText">마이페이지</div><br/>
                    <div className="myPageMenu">
                        <div onClick={toClassAttendingList} style={{cursor: "pointer", fontWeight: currentPage === "수강중인 과목" ? "bold" : "normal"}}>수강중인 과목</div>
                        <div onClick={toClassAttendingNotice} style={{cursor: "pointer", fontWeight: currentPage === "수강과목 공지사항" ? "bold" : "normal"}}>수강과목 공지사항</div>
                        <div onClick={toConsult} style={{cursor: "pointer", fontWeight: currentPage === "실시간 상담" ? "bold" : "normal"}}>실시간 상담</div>
                    </div>
                </div>
            <div>
                {renderPage()}
            </div>
        </main>
    )

}

export default MyPage;