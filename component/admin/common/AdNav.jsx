import { useNavigate } from 'react-router-dom';
import AdNavCss from "../../../css/admin/module/AdNav.module.css";
import { UseAdmin } from "../../../hook/UseAdmin";
import Logo from "../../../public/favicon/android-icon-72x72.png"

// custom hook
function AdNav({currentPage, setCurrentPage}){
    const navigate = useNavigate();
    const { admin } = UseAdmin();
    // console.log(admin);

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
            {/* 통계: 열람실 이용내역 / 푸시알림 수신및 기타 통계자료 / 강의관련 통계 자료 / 시설물 신청 내역*/}
            {/* 시설물: 신청목록 / 사용내역 */}
            {/* 기타기능: 푸시알림 전송기능 */}

            {/* 시스템 어드민 */}
            {/* 학적 등록: 신입생 등록 / 편입생 등록 / 교수등록 / 학사어드민 등록*/}
            {/* 데이터 수정: 학적 수정 / 기타 정보 수정*/}
            {/* 데이터 로깅 조회: 기기 환경별 사용자 분포 / ip 추적 관련 로깅*/}
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