import { useNavigate } from 'react-router-dom';
import AdNavCss from "../../../css/admin/module/AdNav.module.css";
import { UseAdmin } from "../../../hook/UseAdmin";
import Logo from "../../../public/favicon/android-icon-72x72.png"

// custom hook
function AdNav({currentPage, setCurrentPage}){
    const navigate = useNavigate();
    const { admin } = UseAdmin();

    const Reset = () => {
        navigate('/admin');            // SPA 네비게이션
        setCurrentPage("/admin");      // 대시보드 등 기본으로
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
{admin.adminSys == 0 
    ? <>
            <ul>
                <li>공지게시판</li>
                <ul>
                    <li onClick={() => setCurrentPage("학사공지")}>
                        학사공지
                    </li>
                    <li onClick={() => setCurrentPage("행정공지")}>
                        행정공지
                    </li>
                    <li onClick={() => setCurrentPage("기타공지")}>
                        기타공지
                    </li>
                </ul>
            </ul>
            <ul>
                <li>통계자료</li>
                <ul>
                    <li>열람실</li>
                    <li>푸시알림</li>
                    {/* 강의안내문, 클릭 시 관리자 안내문 폼으로 */}
                    <li
                        onClick={() => setCurrentPage('강의안내문')}
                        className={currentPage === '강의안내문' ? AdNavCss.active : ''}
                        role="button"
                        tabIndex={0}
                        onKeyDown={(e) => (e.key === 'Enter' || e.key === ' ') && setCurrentPage('강의안내문')}
                    >
                        강의안내문
                    </li>
                    <li onClick={() => setCurrentPage('강의 등록')}>
                        강의 등록
                    </li>
                </ul>
            </ul>
            {/* 좌측 메뉴: '시설물관리' 하나만 */}
            <ul>
                
                <li onClick={() => setCurrentPage('시설물관리')}
                    className={currentPage === '시설물관리' ? AdNavCss.active : ''}
                    role="button"
                    tabIndex={0}
                    onKeyDown={(e) => (e.key === 'Enter' || e.key === ' ') && setCurrentPage('시설물관리')}>시설물관리</li>
                </ul>
            <ul>
                <li>일정등록</li>
                <ul>
                    <li>학사일정</li>
                </ul>
            </ul>
        </>
    :
        <>
            <ul>
                <li>학적등록</li>
                <ul>
                    <li>학생등록</li>
                    <li>교수등록</li>
                    <li>학사어드민등록</li>
                </ul>
            </ul>
            <ul>
                <li>학적목록</li>
                <ul>
                    <li>학적목록</li>
                </ul>
            </ul>            
        </>
}
        </nav>
    );
}
export default AdNav;
