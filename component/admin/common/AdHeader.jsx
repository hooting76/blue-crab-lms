import AdHeaderCss from '../../../css/admin/module/AdHeader.module.css';
import { UseAdmin } from '../../../hook/UseAdmin';

function AdHeader(){
    const { AdLogout } = UseAdmin();
    return(
        <header className={AdHeaderCss.header}>
            <button onClick={AdLogout}>
                로그아웃
            </button>
        </header>
    );
}// AdHeader end

export default AdHeader;