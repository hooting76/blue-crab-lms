import React from 'react';
import { apiurl } from '../auth/AuthFunc';

async function FindFunc(useEmail, userPhone, userName){
    let jsonBody = {
        userphone: userPhone,
        username: userName
    };

    if(useEmail !== null){
        jsonBody = {
            useremail: useEmail,
            userphone: userPhone,
            username: userName
        };        
    }
    const JsonBody = jsonBody;

    try {
        const setUrl = apiurl('아이디/비밀번호찾기 api연결');

        // find id/pw api protocol
        const findRes = await fetch(setUrl, {
            methode: 'POST',
            credentials: 'include',
            headers: {'Content-Type': 'application/json'},
            body: JSON.stringify(JsonBody),
        });

        const checkCt = findRes.headers.get('content-type') || '';
        const data = 
            checkCt.includes('application/json') ? await findRes.json() : await findRes.text();
            
        if(data.ok){
            if(typeof data === 'object' && data?.accessToken){
                sessionStorage(setItem)('accessToken', data.accessToken);
            }
            console.log(data);
            return data;
        }else{
            throw new Error('입력폼을 확인해주세요.');
        } // response end
        
    } catch (error) {
        alert('에러가 발생했습니다.');
        return error;
    }
}
export default FindFunc;