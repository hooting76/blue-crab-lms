import React, { useEffect, useState } from 'react';
import { UseUser } from '../../hook/UseUser';
import { Link } from 'react-router-dom';

import LoginFrm from '../../css/modules/LoginForm.module.css';
import { FaDownload } from 'react-icons/fa';
import { persistTokens } from '../../src/utils/authFlow';

function LoginForm() {
    const { login, isLoading, error, clearError, isAuthenticated } = UseUser();
    const [email, setEmail] = useState('');
    const [password, setPassword] = useState('');

    const [emailCheck, setEmailCheck] = useState(false);
    const [pwCheck, setPwCheck]       = useState(false);   

    if(isAuthenticated){
        alert('잘못된 접근입니다.');
        window.location.reload();
    };


    // app install start=========
    let deferredPrompt;
    window.addEventListener("beforeinstallprompt", event => {
        event.preventDefault();
        deferredPrompt = event;
    });

    const installApp = () => {
        if (!deferredPrompt) {
            alert("이미 앱이 설치되어 있거나 앱을 설치할 수 없는 환경입니다");
            return;
        };
        deferredPrompt.prompt();
    };
    // app install end=========

    
    function handleInputChange(evt) {
        // regexr
        const regEmail  = /^[0-9a-zA-Z]([-_.]?[0-9a-zA-Z])*@[0-9a-zA-Z]([-_.]?[0-9a-zA-Z])*\.[a-zA-Z]{2,3}$/i;
        const regPw     = /^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)(?=.*[@$!%*?&#.~_-])[A-Za-z\d@$!%*?&#.~_-]{8,20}$/i;
        // 8 ~ 20 + 하나 이상의 대문자 + 하나의 소문자 + 하나의 숫자 + 하나의 특수 문자 정규식

        // selector call value
        let trgVal = evt.target.value;

        // checking regEmail
        if(evt.target.id == "frm_id"){
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
        }else{
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
        };

        evt.target.addEventListener("blur", () => {
            evt.target.style.border = "1px solid #ccc";
        });
    };

    const handleLogin = async () => {
        if (emailCheck && pwCheck) {
            clearError();
            await login(email, password);
             //  추가: 로그인 이후, user에 담긴 토큰을 정규화/보강 저장
    try {
        const raw = localStorage.getItem('user');
        if (raw) {
            const obj = JSON.parse(raw);
            const accessToken  = obj?.data?.accessToken  || obj?.accessToken  || '';
            const refreshToken = obj?.data?.refreshToken || obj?.refreshToken || '';
            if (accessToken || refreshToken) {
            persistTokens({ accessToken, refreshToken });
            }
        }
      } catch { /* noop */ }
        
        }else{
            alert('이메일과 비밀번호 형식을 확인해주세요.');
            return;
        };
        
    };

    //enter key evt trigger
    const handleKeyPress = (e) => {
        if (e.key === 'Enter') {
            handleLogin();
        };
    };

    return (
        <>
            <div className={LoginFrm.frm_wrap}>
                <h2 className={LoginFrm.h2}>
                    로그인
                    {
                        <span onClick={installApp}>
                            <FaDownload />
                        </span>
                    }
                </h2>
                    
                <div>
                    {/* 이메일 입력 */}
                    <div className={LoginFrm.frm_row}>
                        <label htmlFor='frm_id'>이메일</label>
                        <input
                            type="text"
                            value={email}
                            onChange={handleInputChange}
                            onKeyDown={handleKeyPress}
                            name='frm_id'
                            id='frm_id'
                            placeholder="ex) example@google.com"
                            readOnly = {isLoading}
                        />
                    </div>

                    {/* 비밀번호 입력 */}
                    <div className={LoginFrm.frm_row}>
                        <label htmlFor='frm_pw'>비밀번호</label>
                        <input
                            type="password"
                            value={password}
                            onChange={handleInputChange}
                            onKeyDown={handleKeyPress}
                            name='frm_pw'
                            id='frm_pw'
                            readOnly = {isLoading} />
                    </div>

                {/* 에러 메시지 */}
                {error && (
                    <div className={LoginFrm.error} >
                        {error}
                    </div>
                )}

                {/* 로그인 버튼 */}
                    <button
                        onClick={handleLogin}
                        disabled={isLoading || !(emailCheck && pwCheck)}
                        className={LoginFrm.submit}>
                        로그인
                    </button>
                </div>
                
                <div className={LoginFrm.sub}>
                    <span><Link to='/FindInfoId' state={{userPrs: true }}>아이디찾기</Link></span>
                    <span><Link to='/FindInfoPw' state={{userPrs: false }}>비밀번호찾기</Link></span>
                </div>
            </div>        
        </>
    );
};

export default LoginForm;