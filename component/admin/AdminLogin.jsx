import React, { useState } from "react";
import { UseAdmin } from "../../hook/UseAdmin";

import LoginFrm from '../../css/modules/LoginForm.module.css';
import AdminLoginCss from "../../css/modules/AdminLoginCss.module.css";

import AdLoginAuth from "./auth/AdLoginAuth";

function AdminLogin(){
    const { AdLogin, isLoading, error, clearError, isAuthenticated} = UseAdmin();

    const [email, setEmail] = useState('');
    const [password, setPassword] = useState('');
    const [isCheckCode, setIsCheckCode] = useState('');

    const [emailCheck, setEmailCheck] = useState(false);
    const [pwCheck, setPwCheck]       = useState(false);
    const [codeCheck, setCodeCheck]   = useState(false);

    if(isAuthenticated){
        alert('잘못된 접근입니다.');
        window.history.go(-1);
    };

    function handleInputChange(evt){
        // regexr
        const regEmail  = /^[0-9a-zA-Z]([-_.]?[0-9a-zA-Z])*@[0-9a-zA-Z]([-_.]?[0-9a-zA-Z])*\.[a-zA-Z]{2,3}$/i;
        const regPw     = /^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)(?=.*[@$!%*?&#.~_-])[A-Za-z\d@$!%*?&#.~_-]{8,20}$/i;    

        let trgVal = evt.target.value;

        if(evt.target.id == 'frm_id'){
            if(!regEmail.test(trgVal)){ //false
                evt.target.style.border = "2px solid red";
                if(!trgVal){
                    evt.target.style.border = "1px solid #ccc";
                }
                setEmailCheck(false);
            }else{// true
                evt.target.style.border = "2px solid blue";
                setEmailCheck(true);
            };
            setEmail(trgVal);
        }else if(evt.target.id == 'frm_pw'){
            if(!regPw.test(trgVal)){ //false
                evt.target.style.border = "2px solid red";
                if(!trgVal){
                    evt.target.style.border = "1px solid #ccc";
                }
                setPwCheck(false);
            }else{// true
                evt.target.style.border = "2px solid blue";
                setPwCheck(true);
            };
            setPassword(trgVal);
        }else if(evt.target.id == 'frm_code'){
            setIsCheckCode(trgVal);
            setCodeCheck(true);
        }
    };

    async function sendCode(){
        const authCode = document.getElementById('frm_code');
        const inputId = document.getElementById('frm_id');
        const inputPw = document.getElementById('frm_pw');
        if(emailCheck && pwCheck){ //아이디 비밀번호 입력폼 둘다 통과가 되면
            await AdLoginAuth(`bluecrabtester9@gmail.com`, `Bluecrab256@`);

            authCode.disabled = false;
            authCode.focus();
            
            inputId.disabled = true;
            inputPw.disabled = true;  
        }else{
            alert("아이디 혹은 비밀번호 입력을 확인해주세요.");
            return;
        }
    };

    async function submitBtn(){
        if(emailCheck && pwCheck && codeCheck){
            console.log(email, password, isCheckCode);
            await AdLogin(isCheckCode);
        }else{
            alert("아이디나 비밀번호, 인증코드의 입력값을 확인하세요.");
        }
    };

    return(
        <div className={LoginFrm.frm_wrap}>
            <h2 className={LoginFrm.h2}>관리자 시스템 로그인</h2>

            <div>
                {/* 이메일 입력 */}
                <div className={LoginFrm.frm_row}>
                    <label htmlFor='frm_id'>이메일</label>
                    <input
                        type="text"
                        value={email}
                        onChange={handleInputChange}
                        name='frm_id'
                        id='frm_id'
                        placeholder="ex) example@google.com"
                    />
                </div>

                {/* 비밀번호 입력 */}
                <div className={LoginFrm.frm_row}>
                    <label htmlFor='frm_pw'>비밀번호</label>
                    <input
                        type="password"
                        value={password}
                        onChange={handleInputChange}
                        name='frm_pw'
                        id='frm_pw'/>
                </div>

                {/* 이메일 인증코드 입력 */}
                <div className={LoginFrm.frm_row}>
                    <label htmlFor='frm_code'>인증코드</label>
                    <div>
                        <input
                            type="text"
                            value={isCheckCode}
                            onChange={handleInputChange}
                            name='frm_code'
                            id='frm_code'
                            disabled={true}
                        /> 
                        <span id='authTimer' className={AdminLoginCss.authTimer} >5:00</span>
                        <button 
                            className={AdminLoginCss.sendBtn} 
                            onClick={(emailCheck && pwCheck) 
                                ? sendCode
                                : null }
                            disabled={!(emailCheck && pwCheck)}
                            id='frm_btn'
                        >
                            코드 전송
                        </button>
                    </div>
                </div>

                <button
                    className={AdminLoginCss.submit}
                    disabled={!(emailCheck && pwCheck && codeCheck)}
                    id="frm_sbm"
                    onClick={submitBtn}
                >
                    로그인
                </button>                
            </div>
        </div>        
    );
};

export default AdminLogin;