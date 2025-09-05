import React, { useState } from "react";
import { UseAdmin } from "../../hook/UseAdmin";

import LoginFrm from '../../css/modules/LoginForm.module.css';
import AdminLoginCss from "../../css/modules/AdminLoginCss.module.css"

export default function AdminLogin(){
    const { admin, isLoading, error, clearError, isAuthenticated} = UseAdmin();

    const [email, setEmail] = useState('');
    const [password, setPassword] = useState('');
    const [isCheckCode, setIsCheckCode] = useState('');

    if(isAuthenticated){
        alert('잘못된 접근입니다.');
        window.location.reload();
    };

    function handleInputChange(evt){
        let trgEvt = evt.target.value;

        if(evt.target.id == 'frm_id'){
            setEmail(trgEvt);
        }else if(evt.target.id == 'frm_pw'){
            setPassword(trgEvt);
        }else{
            setIsCheckCode(trgEvt);
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
                            id='frm_code'/> 
                        <button className={AdminLoginCss.sendBtn}>코드 전송</button>
                        <span>10:00</span>
                    </div>
                </div>

                <button
                    className={LoginFrm.submit}>
                    로그인
                </button>                
            </div>
        </div>        
    );
};