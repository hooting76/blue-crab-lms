import React, { useState } from 'react';
import { Link, Router, Route, Routes } from 'react-router-dom';
import { FaSpinner } from 'react-icons/fa';
import { UseUser } from '../../hook/UseUser';

import LoginFrm from '../../css/modules/LoginForm.module.css';

function LoginForm() {
    const { login, isLoading, error, clearError, isAuthenticated } = UseUser();
    const [email, setEmail] = useState('');
    const [password, setPassword] = useState('');

    if(isAuthenticated){
        alert('잘못된 접근입니다.');
        return;
    }

    const handleLogin = async () => {
        if (!email || !password) {
            return;
        }
        
        clearError();
        await login(email, password);
    };

    const handleKeyPress = (e) => {
        if (e.key === 'Enter') {
            handleLogin();
        }
    };

    return (
    <div className={LoginFrm.frm_wrap}>
        <h2 className={LoginFrm.h2}>로그인</h2>

        <div>
            {/* 이메일 입력 */}
            <div className={LoginFrm.frm_row}>
                <label htmlFor='frm_id'>이메일</label>
                <input
                    type="text"
                    value={email}
                    onChange={(e) => setEmail(e.target.value)}
                    onKeyDown={handleKeyPress}
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
                    onChange={(e) => setPassword(e.target.value)}
                    onKeyDown={handleKeyPress}
                    name='frm_pw'
                    id='frm_pw'
                />
            </div>

        {/* 에러 메시지 */}
        {error && (
            <div className="err">
                {error}
            </div>
        )}

        {/* 로그인 버튼 */}
            <button
                onClick={handleLogin}
                disabled={isLoading || !email || !password}
                className={LoginFrm.submit}>
                {isLoading ? <FaSpinner /> : '로그인'}
            </button>
        </div>

        <div className={LoginFrm.sub}>

        </div>
    </div>
    );
};

export default LoginForm;