import React, { useState } from 'react';
import { useLocation } from 'react-router-dom';
import { Link } from 'react-router-dom';
import { FaCircleLeft } from 'react-icons/fa6';

import FindFunc from "./FindFunc";
import FindinfoCss from '../../css/modules/findinfo.module.css';

function FindInfo(){
    const location = useLocation();
    const receiveData = location.state;

    // auth state check
    // useProps is true: id / false: pw
    const [useProps, setUseProps] = useState();
    const [useEmail, setUseEmail] = useState('');
    const [userName, setUserName] = useState('');
    const [userPhone, setUserPhone] = useState('');

    if(useProps === undefined){
        //if is undefined
        setUseProps(receiveData.userPrs);
    }else{
        if(useProps != receiveData.userPrs){
            setUseProps(receiveData.userPrs);
        }
    }

    const handlingInput = async() => {
        if(useProps == false){ // if find pw
            if(!userName || !userPhone || !useEmail){
                alert('입력값을 확인하세요.');
                return;
            }else{
                await FindFunc(useEmail, userPhone, userName);
            }
        }else{
            if(!userName || !userPhone){
                alert('입력값을 확인하세요.');
                return;
            }else{
                await FindFunc(null, userPhone, userName);
            }
        };
    };    

    return(<>
    <div className={FindinfoCss.wrap}>
        <div className={FindinfoCss.backBtn}>
            <Link to="/" >
                <FaCircleLeft/>
                <span>뒤로가기</span>
            </Link>
        </div>
        <h2 className={FindinfoCss.h2}>
            <span className={useProps ? null : FindinfoCss.off }><Link to="/FindInfo" state={{userPrs: true }}>아이디찾기</Link></span>

            <span className={useProps ? FindinfoCss.off : null }><Link to="/FindInfo" state={{userPrs: false }}>비밀번호찾기</Link></span>
        </h2>

        <div className={FindinfoCss.div}>

            {!useProps && (
                // if pw
                <div className={FindinfoCss.row}>
                    <label htmlFor='frm_id'>이메일</label>
                    <input
                        type="text"
                        value={useEmail}
                        onChange={(e) => setUseEmail(e.target.value)}
                        id='frm_id'
                        placeholder="ex) example@google.com"
                    />
                </div>
            )}

            <div className={FindinfoCss.row}>
                <label htmlFor="fd_name">성함</label>
                <input
                    type="text" 
                    value={userName} 
                    onChange={
                        (evt) => setUserName(evt.target.value)}
                    id="fd_name" 
                    placeholder='ex) 홍길동' 
                />
            </div>

            <div className={FindinfoCss.row}>
                <label htmlFor="fd_phone">전화번호</label>
                <input
                    type="number" 
                    value={userPhone} 
                    onChange={(evt) => setUserPhone(evt.target.value)} 
                    id="fd_phone" 
                    placeholder='ex) 010-1234-5678' 
                />
            </div>

            <div className={FindinfoCss.row}>
                <button className="findInfo" onClick={handlingInput}>
                    {useProps ? '아이디찾기' : '비밀번호변경' }
                </button>
            </div>
        </div>
    </div>
    </>);
};

export default FindInfo;