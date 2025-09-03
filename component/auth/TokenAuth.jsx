import React from 'react';

// 로컬 스토리지에서 토큰 추출 함수 (수정된 버전)
export const GetTokens = () => {
    const userString = localStorage.getItem('user'); // sessionStorage → localStorage로 변경
    
    if (!userString) {
        console.log('❌ 로컬 스토리지에 user 데이터가 없습니다');
        return { accessToken: null, refreshToken: null };
    }
    
    try {
        const userData = JSON.parse(userString);
        
        // 토큰이 data 객체 안에 있는 구조
        const accessToken = userData.data?.accessToken;
        const refreshToken = userData.data?.refreshToken;
        
        console.log('=== 토큰 추출 결과 (로컬 스토리지) ===');
        console.log('AccessToken:', accessToken ? '있음 (' + accessToken.substring(0, 30) + '...)' : '없음');
        console.log('RefreshToken:', refreshToken ? '있음 (' + refreshToken.substring(0, 30) + '...)' : '없음');
        
        return { accessToken, refreshToken };
        
    } catch (error) {
        console.error('❌ user 데이터 파싱 실패:', error);
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
        console.log('⚠️ 로그아웃을 위해서는 AccessToken과 RefreshToken이 모두 필요합니다');
        return false;
    }
    
    return true;
};