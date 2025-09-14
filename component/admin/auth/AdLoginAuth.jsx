import RequestCode from "../../admin/auth/RequestCode";
// admin first auth

async function AdLoginAuth(email, password) { 
    if (!email || !password) {
        console.error('❌ 이메일과 비밀번호가 필요합니다.');
        return;
    }

    // param value stored
    const adEmail = email;
    const adminPw = password;
    const baseUrl = "https://bluecrab.chickenkiller.com/BlueCrab-1.0.0";

    const jsonBody = {adminId: adEmail, password: adminPw };

    try {
        const response = await fetch(`${baseUrl}/api/admin/login`, {
            method: 'POST',   
            headers: {'Content-Type': 'application/json'},
            body: JSON.stringify(jsonBody)
        });

        const result = await response.json();
        // console.log(result);

        if(result.success && result.data?.sessionToken) {
            localStorage.setItem('sessionToken', result.data.sessionToken);
            // console.log('✅ 1단계 성공! sessionToken 저장됨');
            RequestCode(localStorage.getItem('sessionToken'));
        }else{
            console.error('❌ 1단계 실패:', result.message);        
        }
        return result;
    } catch (error) {
        console.error('❌ 네트워크 오류:', error);
    }
};

export default AdLoginAuth;