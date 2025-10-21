import "../../css/Introductions/Introduction.css"
import PresidentSaysHi from "./Introductions/PresidentSaysHi";
import WayHere from "./Introductions/WayHere";
import Organization from "./Introductions/Organization";
import BlueCrabHistory from "./Introductions/BlueCrabHistory";
import UserDashboard from "../auth/UserDashboard";

function Introduction({currentPage, setCurrentPage}) {
    
    const toPresidentSaysHi = () => {setCurrentPage("총장 인사")};
    const toWayHere = () => {setCurrentPage("오시는 길")};
    const toOrganization = () => {setCurrentPage("학교 조직도")};
    const toBlueCrabAcademyHistory = () => {setCurrentPage("연혁")};

    const renderPage = () => {
        switch (currentPage) {
            case "총장 인사":
                return <PresidentSaysHi/>;
            case "오시는 길":
                return <WayHere/>;
            case "학교 조직도":
                return <Organization/>;
            case "연혁":
                return <BlueCrabHistory/>;
            default:
                return <UserDashboard/>;
        }
    };

    return(
        <main>
            <div className="introductiontitle">
                <div id="schoolIntroductionText">학교 소개</div>
                <div className="menuListTap">
                    <span 
                        onClick={toPresidentSaysHi}
                        style={{cursor: "pointer", 
                                fontWeight: currentPage === "총장 인사" ? "bold" : "normal"}}
                    >총장 인사</span>

                    <span 
                        onClick={toWayHere}
                        style={{cursor: "pointer", 
                                fontWeight: currentPage === "오시는 길" ? "bold" : "normal"}}
                    >오시는 길</span>
                    
                    <span 
                        onClick={toOrganization} 
                        style={{cursor: "pointer", 
                                fontWeight: currentPage === "학교 조직도" ? "bold" : "normal"}}
                    >학교 조직도</span>

                    <span 
                        onClick={toBlueCrabAcademyHistory}
                        style={{cursor: "pointer",
                        fontWeight: currentPage === "연혁" ? "bold" : "normal"}}
                    >연혁</span>
                </div>
            </div>
            <div>
                {renderPage()}
            </div>
        </main>
    )

}

export default Introduction;