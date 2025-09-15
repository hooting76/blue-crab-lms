import React,{ createContext, useReducer,useEffect } from "react";

// 초기 상태
const AdminState = {
    admin: null,
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

// reducer 함수
function AdminReducer(state, action) {
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
                admin: action.payload,
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
                admin: null,
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

export const AdminContext = createContext();

export const AdminProvider = ({ children }) => {
    const [state, dispatch] = useReducer(AdminReducer, AdminState);

    useEffect(() => {
        // 컴포넌트 마운트 시 저장된 관리자 정보 불러오기
        const storedAdmin = localStorage.getItem('Admin');
        if (storedAdmin) {
            try {
                const Admin = JSON.parse(storedAdmin);
                dispatch({ type: LOGIN_SUCCESS, payload: Admin });
            } catch (error) {
                localStorage.removeItem('Admin');
            }
        }
    }, []);

    // 관리자 로그인 함수
    const AdLogin = async(code) =>{
        
        if (!code || code.length !== 6) {
            prompt('6자리 인증코드를 입력하세요:');
            return;
        }
        
        dispatch({ type: LOGIN_START });

        let sessionToken = localStorage.getItem('sessionToken');
        const baseUrl = "https://bluecrab.chickenkiller.com/BlueCrab-1.0.0";

        if (!sessionToken.startsWith('Bearer ')) {
            sessionToken = 'Bearer ' + sessionToken;
        };
        
        // final login auth
        try {
            const response = await fetch(`${baseUrl}/api/admin/email-auth/verify`,{
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                    'Authorization': sessionToken
                },
                body: JSON.stringify({ authCode: code })                
            });

            const result = await response.json();

            if (result.message =="이메일 인증 성공! 토큰이 발급되었습니다." && result.data){
                localStorage.setItem('accessToken', result.data.accessToken);
                localStorage.setItem('Admin', result.data);
                dispatch({ type: LOGIN_SUCCESS, payload: result.data });
                return result.data, {success: true};
            }else{
                throw new Error('입력을 다시 확인하세요.');
            }
        } catch (error) {
            dispatch({ type: LOGIN_FAILURE, payload: error.message });
            return { success: false, error: error.message };
            // console.error('❌ 네트워크 오류:', error);
        }
    };

    // 로그아웃 함수
    const AdLogout = async() => {
        dispatch({ type: LOGOUT });
    }

    // 에러 클리어 함수
    const clearError = () => {
        dispatch({ type: CLEAR_ERROR });
    };    

    // Context에 제공할 값들
    const contextValue = {
        admin: state.admin,
        isLoading: state.isLoading,
        error: state.error,
        isAuthenticated: state.isAuthenticated,
        AdLogin,
        AdLogout,
        clearError
    };

    return(
        <AdminContext.Provider value={contextValue}>
            {children}
        </AdminContext.Provider>
    )
}