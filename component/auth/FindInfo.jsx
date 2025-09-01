import React, { useState } from 'react';
import FindinfoCss from '../../css/modules/findinfo.module.css';

async function FindInfo(props){
    // props check / true: id / false: pw
    // auth state check
    const [useProps, setUseProps] = useState(props);
    const [useEmail, setUseEmail] = useState('');
    const [userName, setUserName] = useState('');
    const [userPhone, setUserPhone] = useState('');

    console.log(useProps);

    const handlingInput = async() => {
        if(useProps == false){ // if find pw
            if(!userName || !userPhone || !useEmail){
                alert('입력값을 확인하세요.');
                return;
            }else{
                await FindFunc(useEmail, userPhone, userName);
            }
        }else{
            if(!userName || !userPhone){
                alert('입력값을 확인하세요.');
                return;
            }else{
               await FindFunc(null, userPhone, userName);
            }
        };
    };    

    return(
    <div className={FindinfoCss.wrap}>
        <h2>{useProps ? '아이디찾기' : '비밀번호변경' }</h2>
        <div className={FindinfoCss.div}>

            {!useProps && (
                // if pw
                <div className={FindinfoCss.row}>
                    <label htmlFor='frm_id'>이메일</label>
                    <input
                        type="text"
                        value={useEmail}
                        onChange={(e) => setUseEmail(e.target.value)}
                        id='frm_id'
                        placeholder="ex) example@google.com"
                    />       
                </div>
            )}

            <div className={FindinfoCss.row}>
                <label htmlFor="fd_name">성함</label>
                <input
                    type="text" 
                    value={userName} 
                    onChange={
                        (evt) => setUserName(evt.target.value)}
                    id="fd_name" 
                    placeholder='ex) 홍길동' 
                />
            </div>

            <div className={FindinfoCss.row}>
                <label htmlFor="fd_phone">전화번호</label>
                <input
                    type="number" 
                    value={userPhone} 
                    onChange={(evt) => setUserPhone(evt.target.value)} 
                    id="fd_phone" 
                    placeholder='ex) 010-1234-5678' 
                />
            </div>

            <div className={FindinfoCss.row}>
                <button className="findInfo" onClick={handlingInput}>
                    {useProps ? '아이디찾기' : '비밀번호변경' }
                </button>
            </div>
        </div>
    </div>
    );
};

export default FindInfo;