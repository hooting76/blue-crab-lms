const API_BASE_URL = 'https://bluecrab.chickenkiller.com/BlueCrab-1.0.0/api/account';

// id찾기
async function FindFunc(userCode, userName, userPhone){
    try {
        const response = await fetch(`${API_BASE_URL}/FindId`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({userCode, userName, userPhone})
        });

        const result = await response.json();
        console.log(result);

        if(result.success && result.data.success) {
            console.log(`✅ 성공! 이메일: ${result.data.maskedEmail}`);
        }else{
            console.log(`❌ 실패: ${result.data.message}`);
        }
        
        return result;
    } catch (error) {
        console.error('💥 오류:', error);
        return null;
    }
}
export default FindFunc;