import { UseUser } from '../../hook/UseUser';
import AsideCss from '../../css/modules/Aside.module.css';
import { FaUser } from 'react-icons/fa';

function Aside(){
    const { user } = UseUser();
    
    return(
        <>
            <div className={AsideCss.asideWrap}>
                <div className={AsideCss.idImg}>
                    {/* 나중에 사진 등록 유무에 대한 조건으로 변경 예정 */}
                    <FaUser />
                </div>
                <div className={AsideCss.Info}>
                    {user && user.data?.user && (
                        <>
                            <p>
                                <span>
                                    {user.data.user.name}
                                </span>
                                <span>
                                    {user.data.user.userCode}
                                </span>
                            </p>
                            <p>
                                <span>
                                    {user.data.user.email}
                                </span>
                            </p>
                        </>
                    )}

                </div>
            </div>
        </>
    );
};

export default Aside;