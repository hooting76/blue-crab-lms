import acJson from '../../../public/schedule.json';

function GetProcessedEvents(){
    // Calling fail...
    if (!Array.isArray(acJson)) return [];

    return acJson.map((data) => {
        // 이미 객체로 되어 있는 경우(이미 처리된 경우) 그대로 반환
        if (typeof data.start === 'object') return data;

        const startArr = data.start ? data.start.trim().split(',') : [];
        const endArr = data.end ? data.end.trim().split(',') : [];

        // 일정 시간에 대한 기본 보정값
        if (!endArr[3] || endArr[3].trim() === "") {
            endArr[3] = '23';
            startArr[3] = startArr[3] || '00';
        };
        if (!endArr[4] || endArr[4].trim() === "") {
            endArr[4] = '59';
            startArr[4] = startArr[4] || '00';
        };

        // 숫자로 변환
        for (let i = 0; i < startArr.length; i++) {
            startArr[i] = Number(String(startArr[i]).trim());
        };
        for (let i = 0; i < endArr.length; i++) {
            endArr[i] = Number(String(endArr[i]).trim());
        };

        return {
            ...data,
            start: new Date(startArr[0], startArr[1], startArr[2], startArr[3], startArr[4]),
            end: new Date(endArr[0], endArr[1], endArr[2], endArr[3], endArr[4])
        };
    });
};

export default GetProcessedEvents;