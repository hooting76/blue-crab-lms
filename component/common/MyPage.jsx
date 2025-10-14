import "../../css/MyPages/MyPage.css";
import ClassAttending from "./MyPages/ClassAttending";
import ClassAttendingNotice from "./MyPages/ClassAttendingNotice";
import Consult from "./MyPages/Consult";
import UserDashboard from "../auth/UserDashboard";
import ClassAttendingProgress from "./MyPages/ClassAttendingProgress";
import ProfileEdit from "./MyPages/ProfileEdit"; // 개인정보 페이지
import MyPageSidebar from "./MyPages/MyPageSidebar"; // 사이드탭
import ProfNoticeWritingPage from "./MyPages/ProfNoticeWritingPage";
import { useState } from "react";


function MyPage({ currentPage, setCurrentPage }) {
    //수강중인 과목 화면 내에서만 쓰는 진행사항 표시용 선택 과목
    const [selectedCourseId, setSelectedCourseId] = useState("");

    const handleSelectCourse = (courseId) => {
        setSelectedCourseId(courseId); // 같은 화면 하단에 진행사항 표시
  };

    const body = () => {
    switch (currentPage) {
        case "개인정보":
            return <ProfileEdit />;
        case "수강중인 과목":
            return (
        <>
            <ClassAttending currentPage={currentPage} setCurrentPage={setCurrentPage} onSelectCourse={handleSelectCourse} />
            {selectedCourseId && (
            <div style={{ marginTop: 16 }}>
                <ClassAttendingProgress courseId={selectedCourseId} />
            </div>
            )}
        </>
        );
        case "수강과목 공지사항":
            return <ClassAttendingNotice />;
        case "과목별 공지 작성":
            return <ProfNoticeWritingPage/>;
        case "실시간 상담":
            return <Consult />;
        default:
        return <UserDashboard />;
    }
  };

    return (
    <main className="mypage-page">
        {/* 커뮤니티와 동일한 중앙 정렬  */}
        <div className="page-wrap"></div>
        {/* 배너영역 */}
        <div className="banner">
        <h2>마이페이지</h2>
    </div>

    <div className="grid">
        <section className="left">
            <div className="card">
                <div className="content-safe">   
                {body()}
            </div>
    </div>
    </section>


        {/* 우측: 사이드탭 */}
        <aside className="right">
        <MyPageSidebar currentPage={currentPage} setCurrentPage={setCurrentPage} />
        </aside>
        </div>
    </main>
  );
}

export default MyPage;
