import AuthThenPwChange from "./AuthThenPwChange";
//pw 찾기
const API_BASE_URL = 'https://bluecrab.chickenkiller.com/BlueCrab-1.0.0/api/auth';

async function FindFuncPw(email, userCode, userName, userPhone){
    try {
        const response = await fetch(`${API_BASE_URL}/password-reset/verify-identity`,{
            method: 'POST',
            headers: { 
                'Content-Type': 'application/json',
                'Accept': 'application/json'
            },
            body: JSON.stringify({ email, userCode, userName, userPhone})
        });

        const result = await response.json();
        // console.log(result);
        // console.log(result.data.identityToken);
        // console.log(result.data.maskedEmail);

        // is ok
        if(result.success && result.data.success){
            if(result.data.identityToken && result.data.maskedEmail){
                // console.log('IRT Token saved');
                let tmp = result.data.identityToken;
                sendEmail(tmp);
            }else{
                console.log(`⚠️  요청 접수됨 (추가 정보 확인 필요)`);
            }
        }else{
            console.log(`❌ 실패: ${result.message || '알 수 없는 오류'}`);
        }
        return result;

    } catch (error) {
        console.error('💥 오류:', error);
        return null;        
    }
}

// 이메일 발송 함수
async function sendEmail(token){
    if(!token){
        alert('잘못된 방식입니다. 다시 재인증하세요.');
        return null;
    }

    try {
        const response = await fetch(`${API_BASE_URL}/password-reset/send-email`,{
            method: 'POST',
            headers: { 
                'Content-Type': 'application/json',
                'Accept': 'application/json'
            },
            body: JSON.stringify({ irtToken: token })
        });

        const responseTxt = await response.text();

        let result;
        try {
            result = JSON.stringify(responseTxt);
        } catch (e) {
            console.log('⚠️  JSON 파싱 실패, 원본 텍스트 응답');
            result = { rawResponse: responseTxt };            
        }

        result = result.replace(/\\/g, '');
        result = result.slice(1, -1);
        result = JSON.parse(result);

        // console.log(typeof(result));
        console.log(result);
        // console.log(result.success);

        switch(response.status){
            case 200:
                if(result.success == true){
                    alert('이메일 인증코드 발송에 성공했습니다.');

                    window.token = token;

                    //timer
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
                            }
                            timeLeft--;
                        }, 1000);
                    })();
                }else{ 
                    alert('이메일 인증코드 발송에 실패했습니다.');
                } 
                break;
            case 400:
                alert('잘못된 요청입니다.');
                break;
            case 401:
                alert('인증이 만료되었습니다. 처음부터 다시 진행해주세요.');
                break;
            case 404:
                alert('요청사항을 찾을수없습니다. 다시 확인해주세요.');
                break;
            case 409:
                alert('이미 다른곳에서의 요청을 처리중입니다.');
                break;
            case 429:
                alert('요청 제한이 초과되었습니다. 잠시 후에 다시 시도해주세요.');
                break;
            default:
                alert('에러가 발생했습니다. 시스템 관리자에게 문의하세요.');
                break;
        }; // switch end
        return result;

    } catch (error) {
        console.error('💥 2단계 네트워크 오류:', error);
        return null;        
    }
}

export default FindFuncPw;