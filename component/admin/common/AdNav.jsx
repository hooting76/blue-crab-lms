import { useNavigate } from 'react-router-dom';
import AdNavCss from "../../../css/admin/module/AdNav.module.css";
import { UseAdmin } from "../../../hook/UseAdmin";
import Logo from "../../../public/favicon/android-icon-72x72.png"

// custom hook
function AdNav({currentPage, setCurrentPage}){
    const navigate = useNavigate();
    const { admin } = UseAdmin();

    const Reset = () => {
        

    };    

    return(
        <nav className={AdNavCss.nav}>
            <div 
                className={AdNavCss.Adlogo}
                onClick={Reset}>
                    <img src={Logo} alt="Logo" />
                    <p>ADMIN</p>
            </div>
            <div className={AdNavCss.profile}>
                <p>
                    {admin.adminSys == 0 
                    ? ('학사어드민') 
                    : ('시스템어드민')}
                </p>
            </div>

            {/* 학사어드민 */}
            {/* 공지 게시판 : 학사공지/ 행정공지/ 기타공지*/}
            {/* 통계: 열람실 이용내역 / 푸시알림 / 강의 통계 자료 / 시설이용 비중 */}
            {/* 시설 예약: 신청목록 / 사용내역 */}
            {/* 학사일정 추가 */}

            {/* 시스템 어드민 */}
            {/* 학적 등록: 학생등록 / 교수등록 / 학사어드민 등록 기능*/}
            {/* 학적 목록: 전체 리스트 출력 및 row별 클릭시 모달로 정보 수정기능 제공*/}
            <div 
                onClick={() => {
                    console.log("학사공지 클릭됨");
                    setCurrentPage("학사공지");}
                }>
                공지
            </div>
        </nav>
    );
}
export default AdNav;