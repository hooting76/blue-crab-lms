// custom hook
import { UseAdmin } from '../../../hook/UseAdmin';

function AdFooter(){
    const { admin, isAuthenticated, isLoading } = UseAdmin();
    // console.log(admin);
    return(
        <footer>
            footer
        </footer>
    );
}

export default AdFooter;