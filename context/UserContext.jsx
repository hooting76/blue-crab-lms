import React, { createContext, useReducer, useEffect } from 'react';
import { GetTokens } from '../component/auth/TokenAuth';

// 초기 상태
const initialState = {
  user: null,
  isLoading: false,
  error: null,
  isAuthenticated: false
};

// 액션 타입들
export const LOGIN_START = 'LOGIN_START';
export const LOGIN_SUCCESS = 'LOGIN_SUCCESS';
export const LOGIN_FAILURE = 'LOGIN_FAILURE';
export const LOGOUT = 'LOGOUT';
export const CLEAR_ERROR = 'CLEAR_ERROR';

// 리듀서 함수
function userReducer(state, action) {
  switch (action.type) {
    case LOGIN_START:
      return {
        ...state,
        isLoading: true,
        error: null
      };
    case LOGIN_SUCCESS:
      return {
        ...state,
        isLoading: false,
        user: action.payload,
        isAuthenticated: true,
        error: null
      };
    case LOGIN_FAILURE:
      return {
        ...state,
        isLoading: false,
        error: action.payload,
        isAuthenticated: false
      };
    case LOGOUT:
      return {
        ...state,
        user: null,
        isAuthenticated: false,
        error: null
      };
    case CLEAR_ERROR:
      return {
        ...state,
        error: null
      };
    default:
      return state;
  }
}

// Context 생성
export const UserContext = createContext();

// UserProvider 컴포넌트
export function UserProvider({ children }) {
  const [state, dispatch] = useReducer(userReducer, initialState);

  // 컴포넌트 마운트 시 저장된 유저 정보 불러오기
  useEffect(() => {
    const savedUser = localStorage.getItem('user');
    if (savedUser) {
      try {
        const user = JSON.parse(savedUser);
        dispatch({ type: LOGIN_SUCCESS, payload: user });
      } catch (error) {
        localStorage.removeItem('user');
      }
    }
  }, []);

  // 로그인 함수
  const login = async (email, password) => {
    dispatch({ type: LOGIN_START });
    
    const dt = {username: email, password: password};

    try {
        const URL = 'https://bluecrab.chickenkiller.com/BlueCrab-1.0.0/api/auth/login';

        const res = await fetch(URL, {
          method: 'POST',
          credentials: "same-origin",
          mode: "cors",
          cache: 'no-cache',
          headers: { 'Content-Type': 'application/json'},
          body: JSON.stringify(dt),
        });

        // console.log(res);
        const ct = res.headers.get('Content-type') || '';
        const data = ct.includes('application/json') ? await res.json() : await res.text();

        // response 
        if (res.ok) {
          // token
          if (typeof data === 'object' && data?.accessToken) localStorage.setItem('accessToken', data.accessToken);
          if (typeof data === 'object' && data?.refreshToken) localStorage.setItem('refreshToken', data.refreshToken);

          // console.log('✅ 로그인 성공', data);
          localStorage.setItem('user', JSON.stringify(data));
          // console.log(sessionStorage.user);
          dispatch({ type: LOGIN_SUCCESS, payload: data });
          return data, { success: true };
          
        } else {
          // console.error('❌ 로그인 실패', res.status, data);
          throw new Error('이메일 또는 비밀번호가 올바르지 않습니다.');
        } // response end

    } catch (error) {
      dispatch({ type: LOGIN_FAILURE, payload: error.message });
      return { success: false, error: error.message };
    };
  };

  // 로그아웃 함수
  const logout = async() => {
    dispatch({ type: LOGOUT });
    const tokens = GetTokens();
    const { accessToken, refreshToken } = tokens;

    // take a token from localStorage
    try {
      const response = await fetch('https://bluecrab.chickenkiller.com/BlueCrab-1.0.0/api/auth/logout', {
        method: 'POST',
        headers: { 
          'Content-Type': 'application/json',
          'Authorization': `Bearer ${accessToken}`
        },
        body: JSON.stringify({ 
          refreshToken: refreshToken 
        })
      });
      const data = await response.json();
      // console.log('✅ 응답 데이터:', data);
    } catch (error) {
        // 네트워크 오류 시에도 클라이언트 토큰 삭제
        localStorage.removeItem('user');
        sessionStorage.removeItem('user');      
    }
    localStorage.removeItem('user');
    sessionStorage.removeItem('user');
    // console.log('🏁 Redis 통합 로그아웃 테스트 완료');
  };

  // 에러 클리어 함수
  const clearError = () => {
    dispatch({ type: CLEAR_ERROR });
  };

  // Context에 제공할 값들
  const contextValue = {
    user: state.user,
    isLoading: state.isLoading,
    error: state.error,
    isAuthenticated: state.isAuthenticated,
    login,
    logout,
    clearError
  };

  return (
    <UserContext.Provider value={contextValue} >
      {children}
    </UserContext.Provider>
  );
}