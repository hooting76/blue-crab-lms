import React, { useState } from 'react';
import { useLocation } from 'react-router-dom';
import { Link } from 'react-router-dom';
import { FaCircleLeft } from 'react-icons/fa6';
import { FaSpinner, FaArrowRight } from 'react-icons/fa';

import FindFuncId from "./FindFunc";
import FindFuncPw from "./FindFuncPw";
import AuthThenPwChange from "./AuthThenPwChange";
import FindinfoCss from '../../css/modules/findinfo.module.css';

export default function FindInfo(){
    const location = useLocation();
    const receiveData = location.state;

    // auth state check
    // useProps is true: id / false: pw
    const [useProps, setUseProps] = useState(false);
    const [useEmail, setUseEmail] = useState('');
    const [userName, setUserName] = useState('');
    const [userPhone, setUserPhone] = useState('');
    const [userCode, setUserCode] = useState('');
    const [authCode, setAuthCode] = useState('');

    // ok state 
    const [emailOk, setEmailOk] = useState(false);
    const [nameOk, setNameOk] = useState(false);
    const [phoneOk, setPhoneOk] = useState(false);
    const [codeOk, setCodeOk] = useState(false);
    const [authOk, setAuthOk] = useState(false);

    // pw change state
    const [userChangePw, setUserChangePw] = useState('');
    const [userChangePwCf, setUserChangePwCf] = useState('');

    const [changePwOk, setChangePwOk] = useState(false);
    const [changePwCfOk, setChangePwCfOk] = useState(false);

    // etc state
    const [switchType01, setSwitchType01] = useState(true);
    const [switchType02, setSwitchType02] = useState(true);
    const [doing, setDoing] = useState(false);

    if(useProps === undefined){
        //if is undefined
        setUseProps(receiveData.userPrs);
    }else{
        if(useProps != receiveData.userPrs){
            setUseProps(receiveData.userPrs);
        }
    }

    const handleInputValue = (evt) => {
        // reg
        const regEmail  = /^[0-9a-zA-Z]([-_.]?[0-9a-zA-Z])*@[0-9a-zA-Z]([-_.]?[0-9a-zA-Z])*\.[a-zA-Z]{2,3}$/i;
        const regCode   = /^\d{1,9}$/;
        const regName   = /[a-zA-Z가-힣]+/;
        const regPhone  = /^\d{11,11}$/;
        const regAuth   = /[0-9A-Z]+/;
        const regPw     = /^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)(?=.*[@$!%*?&#.~_-])[A-Za-z\d@$!%*?&#.~_-]{8,20}$/i;
        // reg end

        let trgVal = evt.target.value;

        switch(evt.target.id){
            //email
            case 'frm_id':
                if(!regEmail.test(trgVal)){ //false
                    evt.target.style.border = "2px solid red";
                    if(!trgVal){
                        evt.target.style.border = "1px solid #ccc";
                    }
                    setEmailOk(false);
                }else{ //true
                    evt.target.style.border = "2px solid blue";
                    setEmailOk(true);
                }; 
                setUseEmail(trgVal);
                break;

            // userCode
            case 'userCode':
                if(!regCode.test(trgVal)){ //false
                    evt.target.style.border = "2px solid red";
                    if(!trgVal){
                        evt.target.style.border = "1px solid #ccc";
                    }
                    setCodeOk(false);
                }else{ //true
                    evt.target.style.border = "2px solid blue";
                    setCodeOk(true);
                }; 
                setUserCode(trgVal);
                break;

            // userName
            case 'fd_name':
                if(!regName.test(trgVal)){ //false
                    evt.target.style.border = "2px solid red";
                    if(!trgVal){
                        evt.target.style.border = "1px solid #ccc";
                    }
                    setNameOk(false);
                }else{ //true
                    evt.target.style.border = "2px solid blue";
                    setNameOk(true);
                }; 
                setUserName(trgVal);
                break;

            // fd_phone
            case 'fd_phone':
                if(!regPhone.test(trgVal)){ //false
                    evt.target.style.border = "2px solid red";
                    if(!trgVal){
                        evt.target.style.border = "1px solid #ccc";
                    }
                    setPhoneOk(false);
                }else{ //true
                    evt.target.style.border = "2px solid blue";
                    setPhoneOk(true);
                }; 
                setUserPhone(trgVal);
                break;

            // authCode
            case 'authCode':
                if(!regAuth.test(trgVal)){ //false
                    evt.target.style.border = "2px solid red";
                    if(!trgVal){
                        evt.target.style.border = "1px solid #ccc";
                    }
                    setAuthOk(false);
                }else{ //true
                    evt.target.style.border = "2px solid blue";
                    setAuthOk(true);
                }; 
                setAuthCode(trgVal);
                break;

            // userChangePw
            case 'userChangePw':
                if(!regPw.test(trgVal)){ //false
                    evt.target.style.border = "2px solid red";
                    if(!trgVal){
                        evt.target.style.border = "1px solid #ccc";
                    }
                    setChangePwOk(false);
                }else{ //true
                    evt.target.style.border = "2px solid blue";
                    setChangePwOk(true);
                }; 
                setUserChangePw(trgVal);
                break;          

            // userChangePwCf
            case 'userChangePwCf':
                if(userChangePw !== trgVal){
                    evt.target.style.border = "2px solid red";
                    if(!trgVal){
                        evt.target.style.border = "1px solid #ccc";
                    }
                    setChangePwCfOk(false);             
                }else{
                    evt.target.style.border = "2px solid blue";
                    setChangePwCfOk(true);
                };
                setUserChangePwCf(trgVal);
                break;
            } // switch end
    }; // handleInputValue end

    const handlingInput = async() => {
        setDoing(true);
        if(useProps == false){ // find pw
            if(!emailOk || !codeOk || !phoneOk || !nameOk){
                alert('입력값을 확인하세요.');
                setDoing(false);
                return;
            }else{
                await FindFuncPw(useEmail, userCode, userName, userPhone);
            }
        }else{// find id
            if(!codeOk || !phoneOk || !nameOk){
                alert('입력값을 확인하세요.');
                setDoing(false);
                return;
            }else{
                await FindFuncId(userCode, userName, userPhone);
            }
        };
        setTimeout(() => setDoing(false), 3000);
    };    

    async function AuthPwHandling(){
        setDoing(true);
        if(!emailOk || !codeOk || !phoneOk || !nameOk || !authOk){
            alert('입력값을 확인하세요.');
            setDoing(false);
            return;    
        }else{
            let token = window.token;
            await AuthThenPwChange(authCode, token);
        }
        setDoing(false);
    };

    const handleSwitchingType = (evt) => {
        let evtId = evt.target.id;

        if(evtId == "pw_first"){
            setSwitchType01(!switchType01);
        }else if(evtId == "pw_2nd"){
            setSwitchType02(!switchType02);
        }
    };

    const PasswordChange = async() => {
        let token = localStorage.getItem('rtToken');
        let newPw = userChangePwCf;

        try {
            const response = await fetch('https://bluecrab.chickenkiller.com/BlueCrab-1.0.0/api/auth/password-reset/change-password',{
                method: 'POST',
                headers: { 
                    'Content-Type': 'application/json',
                    'X-rt': `${token}`
                },
                body: JSON.stringify({ 
                    newPassword: newPw
                })
            });

            const result = await response.json();

            if(confirm("비밀번호를 정말 변경하시겠습니까?")){
                if(result.success){
                    // console.log('✅ 비밀번호 변경 성공!');
                    alert("비밀번호 변경이 완료되었습니다.");
                    window.location.href = "/";
                    location.reload();
                }else {
                    alert('비밀번호 변경에 실패했습니다. 처음부터 다시 진행해주세요.');
                    // return { success: false, error: result };
                }                
            }else{
                alert('사용자가 요청을 취소했습니다.');
            }
        } catch (error) {
            return { success: false, error: error.message };            
        }
    }

    return(<>
    <div className={FindinfoCss.wrap}>
        <div className={FindinfoCss.backBtn}>
            <Link to="/" >
                <FaCircleLeft/>
                <span>뒤로가기</span>
            </Link>
        </div>
        <h2 className={FindinfoCss.h2}>
            <span className={useProps ? null : FindinfoCss.off }>
                <Link 
                    to="/FindInfoId" 
                    state={{userPrs: true }} 
                >
                    아이디찾기
                </Link>
            </span>

            <span className={useProps ? FindinfoCss.off : null }>
                <Link 
                    to="/FindInfoPw" 
                    state={{userPrs: false }}
                >
                    비밀번호찾기
                </Link>
            </span>
        </h2>

        <div className={FindinfoCss.div}>
            {/* true : id / false : pw */}
            {!useProps && (
                <div className={FindinfoCss.row}>
                    <label htmlFor='frm_id'>이메일</label>
                    <input
                        type="text"
                        value={useEmail}
                        onChange={handleInputValue}
                        id='frm_id'
                        placeholder="ex) example@google.com"
                    />
                </div>)
            }

            <div className={FindinfoCss.row}>
                <label htmlFor='userCode'>학번/교번</label>
                <input
                    type="number"
                    value={userCode}
                    onChange={handleInputValue}
                    id='userCode'
                    placeholder="ex) 본인학번/교번"
                />
            </div>

            <div className={FindinfoCss.row}>
                <label htmlFor="fd_name">성함</label>
                <input
                    type="text" 
                    value={userName} 
                    onChange={handleInputValue}
                    id="fd_name" 
                    placeholder='ex) 홍길동' 
                />
            </div>

            <div className={FindinfoCss.row}>
                <label htmlFor="fd_phone">전화번호</label>
                <input
                    type="number" 
                    value={userPhone} 
                    onChange={handleInputValue} 
                    id="fd_phone" 
                    placeholder='ex) 01012345678 / "-" 제외' 
                />
            </div>

            {/* 비밀번호찾기: 이메일 인증코드 */}
            {!useProps 
                && (
                <div className={`${FindinfoCss.row} ${FindinfoCss.authRaw}`}>
                    <label htmlFor="authCode">인증코드</label>
                    <input
                        type="text" 
                        value={authCode} 
                        onChange={handleInputValue} 
                        className={FindinfoCss.fd_code} 
                        id="authCode"
                        placeholder='이메일 인증코드'
                    />
                    <span id="authTimer">5:00</span>
                    <button 
                        className={FindinfoCss.sendCode}
                        id="sendCode"
                        onClick={handlingInput}
                    >
                        {doing ? (<FaSpinner/>) :'코드 전송'}
                    </button>
                </div>)
            }

            {/* userCode, userName, userPhone */}
            <div className={FindinfoCss.row}>
                <button
                    className={FindinfoCss.findInfo}
                    onClick={
                        useProps ? handlingInput : AuthPwHandling
                    }
                    disabled={useProps
                        ? ( !(codeOk && nameOk && phoneOk) )
                        : ( !(codeOk && nameOk && phoneOk && emailOk) )
                    }
                >
                {useProps 
                    ? (doing ? (<FaSpinner/>) :'아이디찾기')
                    : (doing ? (<FaSpinner/>) : 
                    <>비밀번호변경<FaArrowRight className={FindinfoCss.arr}/></>)
                }
                </button>
            </div>

            {/* return message wrap */}
            <div className={FindinfoCss.err}>
                {useProps && (<p id="returnError"></p>)}
            </div>

            {/* pw change wrap */}
            <div className={FindinfoCss.pwChWrap} id="pwChWrap">
                <h4>변경할 비밀번호</h4>
                <div className={FindinfoCss.row} >
                    <input
                        type={switchType01
                            ? "password"
                            : "text"
                        }
                        value={userChangePw}
                        onChange={handleInputValue}
                        id='userChangePw'
                        placeholder="ex) 8-20자 사이에서 영어대소문자 + 숫자 + 특수문자 각 1자 이상 포함"                    
                    />
                    <p 
                        id="pw_first"
                        onClick={handleSwitchingType}
                    >
                        {switchType01 
                            ? <>
                                보기
                            </>
                            : <>
                                숨기기
                            </>
                        }
                    </p>
                </div>

                <h4>변경 비밀번호 확인</h4>
                <div className={FindinfoCss.row}>
                    <input
                        type={switchType02
                            ? "password"
                            : "text"
                        }
                        value={userChangePwCf}
                        onChange={handleInputValue}
                        id='userChangePwCf'
                        placeholder="위에 입력한 값 한번더 입력"
                    />
                    <p 
                        onClick={handleSwitchingType}
                        id="pw_2nd"
                    >
                        {/* (`${<FaEye/>} 보기`) */}
                        {switchType02 
                            ? <>
                                보기
                            </>
                            : <>
                                숨기기
                            </>
                        }
                    </p>
                </div>

                <div className={FindinfoCss.row}>
                    <button 
                        className={FindinfoCss.findInfo}
                        disabled={
                            !(changePwOk && changePwCfOk)
                        }
                        onClick={PasswordChange}
                    >
                        비밀번호 변경하기
                    </button>
                </div>                
            </div>                
        </div>    
    </div>
    </>);
};