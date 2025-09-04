import React from 'react';

// 로컬 스토리지에서 토큰 추출 함수 (수정된 버전)
export const GetTokens = () => {
    const userString = localStorage.getItem('user'); 
    
    if (!userString) {
        return { accessToken: null, refreshToken: null };
    }
    
    try {
        const userData = JSON.parse(userString);
        
        // 토큰이 data 객체 안에 있는 구조
        const accessToken = userData.data?.accessToken;
        const refreshToken = userData.data?.refreshToken;
        
        return { accessToken, refreshToken };
        
    } catch (error) {
        // 파싱 오류 처리
        return { accessToken: null, refreshToken: null };
    }
};

// 개별 토큰 추출 함수들
export const GetAccessToken = () => {
    const tokens = GetTokens();
    return tokens.accessToken;
};

export const GetRefreshToken = () => {
    const tokens = GetTokens();
    return tokens.refreshToken;
};

// 토큰 상태 확인
export const CheckTokenStatus = () => {
    const tokens = GetTokens();
    
    if (!tokens.accessToken || !tokens.refreshToken) {
        return false;
    }
    return true;
};