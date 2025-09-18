const API_BASE_URL = 'https://bluecrab.chickenkiller.com/BlueCrab-1.0.0/api/auth';

async function AuthThenPwChange(authCode, token){

    // authCode check
    if(!authCode){
        alert("인증코드가 없습니다.");
        return null;
    }

    if(authCode.length !== 6){
        alert("인증코드는 6자리여야 합니다.");
        return null;
    }

    // token check
    if(!token){
        alert("인증이 만료되었습니다.");
        return null;
    }

    // api call
    try {
        const response = await fetch(`${API_BASE_URL}/password-reset/verify-code`,{
            method: 'POST',
            headers: { 
                'Content-Type': 'application/json',
                'Accept': 'application/json'
            },
            body: JSON.stringify({ 
                irtToken: token,
                authCode: authCode
            })            
        });

        const result = await response.json();

        switch(response.status){
            case 200:{   
                if(result.success && result.resetToken){
                    localStorage.setItem('rtToken', result.resetToken);
                    // token is saved

                    const pwChWrap = document.getElementById('pwChWrap');
                    pwChWrap.style.left = "0%";
                }else{
                    // 실패 케이스 (200이지만 success: false)
                    // console.log('❌ 코드 검증 실패');
                    console.log(`   - 메시지: ${result.message || 'N/A'}`);
                    console.log(`   - 상태: ${result.status || 'N/A'}`);
                    console.log(`   - 남은 시도: ${result.remainingAttempts || 'N/A'}`);

                    if (result.remainingAttempts === 0) {
                        console.log('🚫 최대 시도 횟수 초과!');
                        console.log('🔄 1단계부터 다시 시작해야 합니다.');
                    }
                }}
                break;
            default:
                alert("오류가 발생했습니다. 나중에 다시 시도해주세요.");
                break;
        } //switch end

        return result;

    } catch (error) {
        return null;
    }
}

export default AuthThenPwChange;