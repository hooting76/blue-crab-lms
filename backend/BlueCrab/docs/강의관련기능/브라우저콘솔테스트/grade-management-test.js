/**
 * 성적 관리 시스템 API 테스트
 * 브라우저 콘솔에서 실행
 */

// 기본 설정
const API_BASE_URL = 'https://bluecrab.chickenkiller.com/BlueCrab-1.0.0/api/enrollments';

// 전역 변수 (test-1-login.js에서 설정한 토큰 사용)
if (typeof window.authToken === 'undefined') window.authToken = null;

// ========== 로그인 상태 확인 ==========
function checkAuth() {
    const token = window.authToken || localStorage.getItem('jwtAccessToken');
    if (!token) {
        console.log('\n⚠️ 로그인이 필요합니다!');
        console.log('🔧 docs/일반유저 로그인+게시판/test-1-login.js 실행');
        console.log('   await login() (교수 계정 사용)');
        return false;
    }
    return token;
}

// 테스트 데이터
const testData = {
    lecIdx: 1,
    studentIdx: 100,
    professorIdx: 22,
    passingThreshold: 60,
    gradeDistribution: {
        "A": 30,  // 상위 30%
        "B": 40,  // 30~70%
        "C": 20,  // 70~90%
        "D": 10   // 나머지
    }
};

// 공통 fetch 함수
async function apiCall(endpoint, data) {
    const token = checkAuth();
    if (!token) {
        console.error('❌ 인증 토큰이 없습니다. 먼저 로그인하세요.');
        return null;
    }

    try {
        const response = await fetch(`${API_BASE_URL}${endpoint}`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                'Authorization': `Bearer ${token}`
            },
            body: JSON.stringify(data)
        });
        
        const result = await response.json();
        console.log(`${endpoint} 결과:`, result);
        return result;
    } catch (error) {
        console.error(`${endpoint} 에러:`, error);
        return null;
    }
}

// 1. 성적 구성 설정 테스트
async function testGradeConfig() {
    console.log('=== 성적 구성 설정 테스트 ===');
    
    const requestData = {
        lecIdx: testData.lecIdx,
        attendanceMaxScore: 20,
        assignmentTotalScore: 50,
        examTotalScore: 30,
        gradeDistribution: testData.gradeDistribution,
        action: "set-config"
    };
    
    return await apiCall('/grade-config', requestData);
}

// 2. 개별 성적 조회 테스트 (학생용)
async function testStudentGradeInfo() {
    console.log('=== 학생 성적 조회 테스트 ===');
    
    const requestData = {
        lecIdx: testData.lecIdx,
        studentIdx: testData.studentIdx,
        action: "get-grade"
    };
    
    return await apiCall('/grade-info', requestData);
}

// 3. 교수용 성적 조회 테스트
async function testProfessorGradeView() {
    console.log('=== 교수용 성적 조회 테스트 ===');
    
    const requestData = {
        lecIdx: testData.lecIdx,
        studentIdx: testData.studentIdx,
        professorIdx: testData.professorIdx,
        action: "professor-view"
    };
    
    return await apiCall('/grade-info', requestData);
}

// 4. 전체 성적 목록 조회 테스트
async function testGradeList() {
    console.log('=== 성적 목록 조회 테스트 ===');
    
    const requestData = {
        lecIdx: testData.lecIdx,
        page: 0,
        size: 20,
        sortBy: "percentage",
        sortOrder: "desc",
        action: "list-all"
    };
    
    return await apiCall('/grade-list', requestData);
}

// 5. 최종 등급 배정 테스트
async function testGradeFinalize() {
    console.log('=== 최종 등급 배정 테스트 ===');
    
    const requestData = {
        lecIdx: testData.lecIdx,
        passingThreshold: testData.passingThreshold,
        gradeDistribution: testData.gradeDistribution,
        action: "finalize"
    };
    
    return await apiCall('/grade-finalize', requestData);
}

// 전체 테스트 실행
async function runAllTests() {
    console.log('🚀 성적 관리 시스템 API 테스트 시작');
    console.log('테스트 데이터:', testData);
    
    try {
        // 순차적으로 테스트 실행
        await testGradeConfig();
        console.log(''); // 빈 줄
        
        await testStudentGradeInfo();
        console.log('');
        
        await testProfessorGradeView();
        console.log('');
        
        await testGradeList();
        console.log('');
        
        await testGradeFinalize();
        console.log('');
        
        console.log('✅ 모든 테스트 완료');
        
    } catch (error) {
        console.error('❌ 테스트 실행 중 오류:', error);
    }
}

// 개별 테스트 함수들을 전역으로 노출
window.gradeTests = {
    runAll: runAllTests,
    config: testGradeConfig,
    studentInfo: testStudentGradeInfo,
    professorView: testProfessorGradeView,
    gradeList: testGradeList,
    finalize: testGradeFinalize
};

// 사용법 안내
console.log(`
🎯 성적 관리 시스템 API 테스트 스크립트 로드됨

⚠️ 사전 준비: 먼저 교수 계정으로 로그인하세요!
📁 위치: docs/일반유저 로그인+게시판/test-1-login.js
📝 실행: await login() (교수 계정 사용)

사용법:
- gradeTests.runAll()     : 전체 테스트 실행
- gradeTests.config()     : 성적 구성 설정 테스트
- gradeTests.studentInfo(): 학생 성적 조회 테스트  
- gradeTests.professorView(): 교수용 성적 조회 테스트
- gradeTests.gradeList()  : 성적 목록 조회 테스트
- gradeTests.finalize()   : 최종 등급 배정 테스트

시작하려면: gradeTests.runAll()
`);