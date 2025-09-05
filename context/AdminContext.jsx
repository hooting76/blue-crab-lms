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
    const AdLogin = async(email, password) =>{
        dispatch({ type: LOGIN_START });
        const adData = {username: email, password: password};

        try {
            const adUrl = 'https://bluecrab.chickenkiller.com/BlueCrab-1.0.0/api/';

            const res = await fetch(adUrl, {
                method: 'POST',
                credentials: "same-origin",
                mode: "cors",
                cache: 'no-cache',
                headers: { 'Content-Type': 'application/json'},
                body: JSON.stringify(adData),                
            });

            // console.log(res);
            const ct = res.headers.get('Content-type') || '';
            const data = ct.includes('application/json') ? await res.json() : await res.text();

            // response
            if(res.ok){
                
            }else{

            }// response end

        } catch (error) {
            
        }
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