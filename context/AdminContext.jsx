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
        const storedAdmin = localStorage.getItem('admin');
        if (storedAdmin) {
            try {
                dispatch({ type: LOGIN_SUCCESS, payload: JSON.parse(storedAdmin) });
            } catch (error) {
                localStorage.removeItem('admin');
            }
        }
    }, []);

    // 관리자 로그인 함수
    const AdLogin = async(code) =>{
        const token = localStorage.getItem('tmp_token');
        dispatch({ type: LOGIN_START });

        const authCode = code;
        const serverDomain = "https://bluecrab.chickenkiller.com/BlueCrab-1.0.0";
        // console.log("authCode",authCode);
        console.log("token",token);

        fetch(`${serverDomain}/api/admin/email-auth/verify`, {
            method: 'POST',
            headers: {
                'Authorization': `Bearer ${token}`,
                'Content-Type': 'application/json'
            },
            body: JSON.stringify({
                authCode: authCode
            })
        }).then(response => {
            console.log("📊 검증 응답 상태:", response.status);
            
            if (response.status === 200) {
                console.log("✅ 인증코드 검증 요청 성공!");
            } else if (response.status === 401) {
                console.log("❌ 401 오류 - 세션 토큰이 만료되었거나 유효하지 않음");
            } else if (response.status === 400) {
                console.log("❌ 400 오류 - 인증코드가 잘못되었거나 만료됨");
            }
            
            return response.text();
        }).then(data => {
            const result = JSON.parse(data);
            
            console.log(result);
        })
    };

    const AdLogout = async() => {
        dispatch({ type: LOGOUT });
    }

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