// setDoing should be passed as a prop or argument, not imported directly

/**
 * Sends a request to the server to initiate email authentication.
 * @param {string} token - The session token for authorization.
 */

async function RequestCode(token) {

    let sessionToken = token;
    const baseUrl = "https://bluecrab.chickenkiller.com/BlueCrab-1.0.0";

    if(!sessionToken){
        console.error('❌ sessionToken이 없습니다.');
        return;
    }

    if (!sessionToken.startsWith('Bearer ')) {
        sessionToken = 'Bearer ' + sessionToken;
    }

    // email code request
    try {
        const response = await fetch(`${baseUrl}/api/admin/email-auth/request`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                'Authorization': sessionToken
            }            
        });

        const result = await response.json();
        
        if (response.status === 500) {
            console.log("Server Error!!");
            console.log('에러 메시지:', result.message);
        } else if (result.success) {
            //console.log('✅ 2단계 성공! 이메일 확인 후 T.verify() 실행');
        }

        // send code ok alert
        alert(result.message);
        
        //timer // one time use func
        (function() {
            const timerElement = document.getElementById('authTimer');
            let timeLeft = 300;
            const countdownTimer = setInterval(() => {
                const minutes = Math.floor(timeLeft / 60);
                const seconds = timeLeft % 60;
                timerElement.textContent = `${minutes}:${seconds.toString().padStart(2, '0')}`;
                
                if (timeLeft <= 0) {
                    clearInterval(countdownTimer);
                    timerElement.textContent = '0:00';
                    // alert("인증코드가 만료되었습니다.\n 새로 인증을 진행해주세요.");
                    // location.reload();
                }
                timeLeft--;
            }, 1000);
        })();

        return result;
    } catch (error) {
        console.error('❌ 네트워크 오류:', error);
    }
};

export default RequestCode;