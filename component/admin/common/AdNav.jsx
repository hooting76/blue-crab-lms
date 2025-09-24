import AdNavCss from "../../../css/admin/module/AdNav.module.css";
import { useState } from 'react';

// custom hook
function AdNav({currentPage, setCurrentPage}){
    const [currentPage, setCurrentPage] = useState("");
    return(
        <nav className={AdNavCss.nav}>
            <div onClick={setCurrentPage("학사공지")}>공지</div>
        </nav>
    );
}
export default AdNav;