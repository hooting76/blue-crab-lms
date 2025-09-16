import React, { useState } from 'react';
import { useLocation } from 'react-router-dom';
import { Link } from 'react-router-dom';
import { FaCircleLeft } from 'react-icons/fa6';
import { FaSpinner } from 'react-icons/fa';

import FindFuncId from "./FindFunc";
import FindFuncPw from "./FindFuncPw";
import FindinfoCss from '../../css/modules/findinfo.module.css';

function FindInfo(){
    const location = useLocation();
    const receiveData = location.state;

    // auth state check
    // useProps is true: id / false: pw
    const [useProps, setUseProps] = useState(false);
    const [useEmail, setUseEmail] = useState('');
    const [userName, setUserName] = useState('');
    const [userPhone, setUserPhone] = useState('');
    const [userCode, setUserCode] = useState('');

    const [doing, setDoing] = useState(false); // sendBtn state    

    if(useProps === undefined){
        //if is undefined
        setUseProps(receiveData.userPrs);
    }else{
        if(useProps != receiveData.userPrs){
            setUseProps(receiveData.userPrs);
        }
    }

    const handlingInput = async() => {
        setDoing(true);
        if(useProps == false){ // find pw
            if(!useEmail || !userCode || !userPhone || !userName){
                alert('입력값을 확인하세요.');
                setDoing(false);
                return;
            }else{
                await FindFuncPw(useEmail, userCode, userPhone, userName);
            }
        }else{// find id
            if(!userCode || !userPhone || !userName){
                alert('입력값을 확인하세요.');
                setDoing(false);
                return;
            }else{
                await FindFuncId(userCode, userPhone, userName);
            }
        };
        setDoing(false);
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
            <span className={useProps ? null : FindinfoCss.off }><Link to="/FindInfoId" state={{userPrs: true }}>아이디찾기</Link></span>

            <span className={useProps ? FindinfoCss.off : null }><Link to="/FindInfoPw" state={{userPrs: false }}>비밀번호찾기</Link></span>
        </h2>

        <div className={FindinfoCss.div}>
            {/* true : id / false : pw */}
            {!useProps && (
                <div className={FindinfoCss.row}>
                    <label htmlFor='frm_id'>이메일</label>
                    <input
                        type="text"
                        value={useEmail}
                        onChange={(e) => setUseEmail(e.target.value)}
                        id='frm_id'
                        placeholder="ex) example@google.com"
                    />
                </div>)
            }

            <div className={FindinfoCss.row}>
                <label htmlFor='userCode'>학번</label>
                <input
                    type="number"
                    value={userCode}
                    onChange={(e) => setUserCode(e.target.value)}
                    id='userCode'
                    placeholder="ex) 본인학번(숫자)"
                />
            </div>

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
                    placeholder='ex) 01012345678 / "-" 제외' 
                />
            </div>

            {/* 비밀번호찾기: 이메일 인증코드 */}
            {!useProps 
                && (
                <div className={FindinfoCss.row}>
                    <label htmlFor="fd_code">인증코드</label>
                    <input
                        type="text" 
                        value={userCode} 
                        onChange={(evt) => setUserCode(evt.target.value)} 
                        class={FindinfoCss.fd_code} 
                        id="fd_code"
                        placeholder='이메일 인증코드'
                    />
                    <span>05:00</span>
                    <button className={FindinfoCss.sendCode} id="sendCode">
                        {doing ? (<FaSpinner/>) :'코드 전송'}
                    </button>
                </div>)
            }
            
            <div className={FindinfoCss.row}>
                <button className={FindinfoCss.findInfo} onClick={handlingInput}>
                    {useProps 
                        ? (doing ? (<FaSpinner/>) :'아이디찾기') 
                        : (doing ? (<FaSpinner/>) :'비밀번호변경')
                    }
                </button>
            </div>
            <div className={FindinfoCss.resultWrap}>
                asdf
            </div>                
        </div>    
    </div>
    </>);
};

export default FindInfo;