// ===================================================================
// 관리자 강의 관리 테스트 (POST 방식)
// Blue Crab LMS
// ===================================================================

const API_BASE_URL = 'https://bluecrab.chickenkiller.com/BlueCrab-1.0.0';

function checkAuth() {
    const token = window.authToken || localStorage.getItem('jwtAccessToken');
    if (!token) {
        console.log(' 로그인 필요!');
        return false;
    }
    console.log(' 로그인 확인됨');
    return true;
}

async function getLectures() {
    if (!checkAuth()) return;
    const token = window.authToken || localStorage.getItem('jwtAccessToken');
    
    const page = parseInt(prompt('페이지:', '0'));
    const size = parseInt(prompt('크기:', '10'));
    const professor = prompt('교수 코드 (선택):', '');
    const year = prompt('학년도 (선택):', '');
    const semester = prompt('학기 (선택):', '');

    console.log('\n 강의 목록 (POST)');
    
    try {
        const requestBody = { page, size };
        if (professor) requestBody.professor = professor;
        if (year) requestBody.year = parseInt(year);
        if (semester) requestBody.semester = parseInt(semester);

        const response = await fetch(`${API_BASE_URL}/lectures`, {
            method: 'POST',
            headers: {
                'Authorization': `Bearer ${token}`,
                'Content-Type': 'application/json'
            },
            body: JSON.stringify(requestBody)
        });

        const data = await response.json();
        if (data.success) {
            console.log(` ${data.data.totalElements}개\n`);
            data.data.content.forEach((lec, i) => {
                console.log(`${i+1}. ${lec.lecName} (IDX:${lec.lecIdx})`);
                console.log(`   교수:${lec.lecProfName||'N/A'} ${lec.lecYear}-${lec.lecSemester}`);
                console.log(`   ${lec.lecCurrent}/${lec.lecMax} ${lec.lecRoom||''} ${lec.lecTime||''}\n`);
            });
            if (data.data.content.length > 0) window.lastLectureIdx = data.data.content[0].lecIdx;
        } else {
            console.log('', data.message);
        }
    } catch (error) {
        console.log('', error.message);
    }
}

async function getLectureDetail() {
    if (!checkAuth()) return;
    const token = window.authToken || localStorage.getItem('jwtAccessToken');
    const lecIdx = parseInt(prompt('LECTURE_IDX:', window.lastLectureIdx || '1'));

    console.log('\n 강의 상세 (POST)');
    
    try {
        const response = await fetch(`${API_BASE_URL}/lectures/detail`, {
            method: 'POST',
            headers: {
                'Authorization': `Bearer ${token}`,
                'Content-Type': 'application/json'
            },
            body: JSON.stringify({ lecIdx })
        });

        const data = await response.json();
        if (data.success) {
            const lec = data.data;
            console.log(` ${lec.lecName} (${lec.lecCode})`);
            console.log(`교수:${lec.lecProfName||'N/A'} ${lec.lecYear}-${lec.lecSemester}`);
            console.log(`${lec.lecCurrent}/${lec.lecMax} ${lec.lecRoom||''} ${lec.lecTime||''}`);
            window.lastLectureIdx = lec.lecIdx;
        } else {
            console.log('', data.message);
        }
    } catch (error) {
        console.log('', error.message);
    }
}

async function getLectureStats() {
    if (!checkAuth()) return;
    const token = window.authToken || localStorage.getItem('jwtAccessToken');
    const lecIdx = parseInt(prompt('LECTURE_IDX:', window.lastLectureIdx || '1'));

    console.log('\n 강의 통계 (POST)');
    
    try {
        const response = await fetch(`${API_BASE_URL}/lectures/stats`, {
            method: 'POST',
            headers: {
                'Authorization': `Bearer ${token}`,
                'Content-Type': 'application/json'
            },
            body: JSON.stringify({ lecIdx })
        });

        const data = await response.json();
        if (data.success) {
            const s = data.data;
            console.log(`수강생:${s.totalStudents} 과제:${s.totalAssignments} 제출:${s.submittedAssignments} (${s.submissionRate}%)`);
        } else {
            console.log('', data.message);
        }
    } catch (error) {
        console.log('', error.message);
    }
}

function help() {
    console.log('\n관리자 강의 테스트 (POST)');
    console.log('checkAuth() getLectures() getLectureDetail() getLectureStats()');
    console.log('POST /lectures, /lectures/detail, /lectures/stats');
}

console.log(' 관리자 테스트 로드 (POST)');
console.log('help() 로 함수 목록');
