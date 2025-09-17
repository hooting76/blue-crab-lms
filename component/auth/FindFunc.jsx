const API_BASE_URL = 'https://bluecrab.chickenkiller.com/BlueCrab-1.0.0/api/account';

// id찾기
async function FindFunc(userCode, userName, userPhone){
    const ResTxtTrg = document.getElementById("returnError");

    try {
        const response = await fetch(`${API_BASE_URL}/FindId`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({userCode, userName, userPhone})
        });

        const result = await response.json();

        if(result.success && result.data.success) {
            // ok
            let okResBody = `찾으신 정보는 <span style='color:blue;'>${result.data.maskedEmail}</span> 입니다.`;
            ResTxtTrg.innerHTML = okResBody;
        }else{
            // no
            let errTxt = `<span style='color:red;'>${result.data.message}</span>`;
            ResTxtTrg.innerHTML = errTxt;
        }
    } catch (error) {
        console.error('💥 오류:', error);
        // let errTxt = `${result.data.message}`;
        // ResTxtTrg.innerHTML(errTxt);
    }
}
export default FindFunc;