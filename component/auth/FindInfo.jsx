import React, { useState } from 'react';
import { useLocation } from 'react-router-dom';
import { Link } from 'react-router-dom';
import { FaCircleLeft } from 'react-icons/fa6';

import FindFuncId from "./FindFunc";
import FindFuncPw from "./FindFuncPw";
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
    const [userCode, setUserCode] = useState('');

    if(useProps === undefined){
        //if is undefined
        setUseProps(receiveData.userPrs);
    }else{
        if(useProps != receiveData.userPrs){
            setUseProps(receiveData.userPrs);
        }
    }

    const handlingInput = async() => {
        if(useProps == false){ // if find id
            if(!userName || !userPhone || !userCode){
                alert('입력값을 확인하세요.');
                return;
            }else{
                await FindFuncId(userCode, userName, userPhone);
            }
        }else{
            if(!userName || !userPhone || !userCode || !useEmail){
                alert('입력값을 확인하세요.');
                return;
            }else{
                await FindFuncPw(userCode, userName, userPhone, useEmail);
            };
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
            <span className={useProps ? null : FindinfoCss.off }><Link to="/FindInfoId" >아이디찾기</Link></span>
            <span className={useProps ? FindinfoCss.off : null }><Link to="/FindInfoPw" >비밀번호찾기</Link></span>
        </h2>

        <div className={FindinfoCss.div}>

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
                    placeholder='ex) 01012345678 / "-" 자 없이' 
                />
            </div>

            {/* 아이디찾기: 결과창
                비밀번호찾기: 이메일 인증코드 */}
            <div className={FindinfoCss.row}>
                {useProps 
                ? (<p>조회된 결과 : <span></span></p>) 
                : (<>
                    <label htmlFor={FindinfoCss.fd_code}>인증코드</label>
                    <input
                        type="text" 
                        value={userCode} 
                        onChange={(evt) => setUserCode(evt.target.value)} 
                        id={FindinfoCss.fd_code} 
                        placeholder='이메일 인증코드'
                    />
                    <span>05:00</span>
                    <button className={FindinfoCss.sendCode}>코드 전송</button>
                </>) 
                }
            </div>

            <div className={FindinfoCss.row}>
                <button className={FindinfoCss.findInfo} onClick={handlingInput}>
                    {useProps ? '아이디찾기' : '비밀번호찾기' }
                </button>
            </div>
        </div>
    </div>
    </>);
};

export default FindInfo;