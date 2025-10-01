import { UseUser } from "../../hook/UseUser";
import { UseAdmin } from "../../hook/UseAdmin";

        // 사용자 컨텍스트
        const userContext = UseUser();
        const { user, isAuthenticated: isUserAuth } = userContext || { user: null, isAuthenticated: false };
    
        // 관리자 컨텍스트
        const adminContext = UseAdmin() || { admin: null, isAuthenticated: false };
        const { admin, isAuthenticated: isAdminAuth } = adminContext;
    
        // Admin 또는 User의 accessToken 가져오기
        const getAccessToken = () => {
            // 로컬스토리지에서 먼저 확인 (가장 최신 토큰)
            const storedToken = localStorage.getItem('accessToken');
            if (storedToken) return storedToken;
    
            // Admin 토큰 확인
            if (isAdminAuth && admin?.data?.accessToken) {
                return admin.data.accessToken;
            }
    
            // User 토큰 확인
            if (isUserAuth && user?.data?.accessToken) {
                return user.data.accessToken;
            }
    
            return null;
        };

export default getAccessToken;