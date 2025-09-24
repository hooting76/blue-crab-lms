import AdNavCss from "../../../css/admin/module/AdNav.module.css";

// custom hook
function AdNav({currentPage, setCurrentPage}){
    return(
        <nav className={AdNavCss.nav}>
            <div onClick={setCurrentPage("학사공지")}>공지</div>
        </nav>
    );
}
export default AdNav;