/**
 * 성적 관리 시스템 - 공통 유틸리티
 * 모든 테스트에서 사용하는 공통 함수들
 * 
 * ============================================
 * 사용 방법
 * ============================================
 * 
 * 1. 브라우저 콘솔에 파일 내용 전체 복사 & 붙여넣기
 * 
 * 2. 로드 확인:
 *    window.gradeTestUtils
 * 
 *    예상 결과:
 *    {
 *      apiCall: ƒ,
 *      apiGet: ƒ,
 *      apiPut: ƒ,
 *      checkAuth: ƒ,
 *      testData: { lecIdx: 1, studentIdx: 100, ... },
 *      setTestData: ƒ,
 *      getTestData: ƒ,
 *      API_BASE_URL: "https://bluecrab.chickenkiller.com/BlueCrab-1.0.0/api"
 *    }
 * 
 * 3. 인증 확인:
 *    gradeTestUtils.checkAuth()
 * 
 *    예상 결과 (로그인 완료):
 *    "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..." (JWT 토큰)
 * 
 *    예상 결과 (로그인 필요):
 *    ⚠️ 로그인이 필요합니다!
 *    🔧 docs/일반유저 로그인+게시판/test-1-login.js 실행
 *       await login() (교수 계정 사용)
 *    false
 * 
 * 4. 테스트 데이터 확인:
 *    gradeTestUtils.getTestData()
 * 
 *    예상 결과:
 *    {
 *      lecIdx: 1,
 *      studentIdx: 100,
 *      professorIdx: 22,
 *      enrollmentIdx: 1,
 *      assignmentIdx: 1,
 *      passingThreshold: 60,
 *      attendanceMaxScore: 80,
 *      assignmentTotalMaxScore: 100,
 *      latePenaltyPerSession: 0.5,
 *      gradeDistribution: { "A+": 10, "A": 15, "B+": 20, ... }
 *    }
 * 
 * 5. 테스트 데이터 변경 (선택):
 *    gradeTestUtils.setTestData(1, 100, 22, 1)
 * 
 *    예상 결과:
 *    ✅ 테스트 데이터 업데이트: { lecIdx: 1, studentIdx: 100, ... }
 * 
 * 6. API 호출 예제:
 *    await gradeTestUtils.apiGet('/enrollments/1/100/grade')
 * 
 *    예상 결과 (성공):
 *    ✅ GET /enrollments/1/100/grade 성공 (145.23ms)
 *    { success: true, data: { ... }, duration: "145.23" }
 * 
 *    예상 결과 (실패):
 *    ❌ HTTP 404: Not Found
 *    { success: false, error: "강의를 찾을 수 없습니다", status: 404, duration: "89.45" }
 * 
 * ============================================
 * 다음 단계
 * ============================================
 * 이 유틸리티를 로드한 후:
 * - grade-phase1-tests.js 로드 (Phase 1 테스트)
 * - grade-phase3-tests.js 로드 (Phase 3 이벤트 테스트)
 * - grade-test-runner.js 로드 (통합 실행)
 */

// ============================================
// 기본 설정
// ============================================
const API_BASE_URL = 'https://bluecrab.chickenkiller.com/BlueCrab-1.0.0/api';

// 전역 변수 (test-1-login.js에서 설정한 토큰 사용)
if (typeof window.authToken === 'undefined') window.authToken = null;

// ============================================
// 로그인 상태 확인
// ============================================
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

// ============================================
// 공통 Fetch 함수
// ============================================

// POST 요청
async function apiCall(endpoint, data, method = 'POST') {
    const token = checkAuth();
    if (!token) {
        console.error('❌ 인증 토큰이 없습니다. 먼저 로그인하세요.');
        return { success: false, error: '인증 토큰 없음' };
    }

    const startTime = performance.now();

    try {
        const response = await fetch(`${API_BASE_URL}${endpoint}`, {
            method: method,
            headers: {
                'Content-Type': 'application/json',
                'Authorization': `Bearer ${token}`
            },
            body: method !== 'GET' ? JSON.stringify(data) : undefined
        });
        
        const endTime = performance.now();
        const duration = (endTime - startTime).toFixed(2);
        
        if (!response.ok) {
            console.error(`❌ HTTP ${response.status}: ${response.statusText}`);
            const errorData = await response.json().catch(() => ({ message: response.statusText }));
            return { success: false, error: errorData.message || errorData, status: response.status, duration };
        }
        
        const result = await response.json();
        console.log(`✅ ${method} ${endpoint} 성공 (${duration}ms)`);
        return { success: true, data: result, duration };
        
    } catch (error) {
        const endTime = performance.now();
        const duration = (endTime - startTime).toFixed(2);
        console.error(`🔥 ${endpoint} 예외 발생 (${duration}ms):`, error.message);
        return { success: false, error: error.message, duration };
    }
}

// GET 요청
async function apiGet(endpoint) {
    const token = checkAuth();
    if (!token) {
        console.error('❌ 인증 토큰이 없습니다. 먼저 로그인하세요.');
        return { success: false, error: '인증 토큰 없음' };
    }

    const startTime = performance.now();

    try {
        const response = await fetch(`${API_BASE_URL}${endpoint}`, {
            method: 'GET',
            headers: {
                'Authorization': `Bearer ${token}`
            }
        });
        
        const endTime = performance.now();
        const duration = (endTime - startTime).toFixed(2);
        
        if (!response.ok) {
            console.error(`❌ HTTP ${response.status}: ${response.statusText}`);
            const errorData = await response.json().catch(() => ({ message: response.statusText }));
            return { success: false, error: errorData.message || errorData, status: response.status, duration };
        }
        
        const result = await response.json();
        console.log(`✅ GET ${endpoint} 성공 (${duration}ms)`);
        return { success: true, data: result, duration };
        
    } catch (error) {
        const endTime = performance.now();
        const duration = (endTime - startTime).toFixed(2);
        console.error(`🔥 ${endpoint} 예외 발생 (${duration}ms):`, error.message);
        return { success: false, error: error.message, duration };
    }
}

// PUT 요청
async function apiPut(endpoint, data) {
    return await apiCall(endpoint, data, 'PUT');
}

// ============================================
// 테스트 데이터 관리
// ============================================
const testData = {
    lecIdx: 1,
    studentIdx: 100,
    professorIdx: 22,
    enrollmentIdx: 1,
    assignmentIdx: 1,
    passingThreshold: 60.0,
    attendanceMaxScore: 80,
    assignmentTotalMaxScore: 100,
    latePenaltyPerSession: 0.5,
    gradeDistribution: {
        "A+": 10,
        "A": 15,
        "B+": 20,
        "B": 25,
        "C": 20,
        "D": 10
    }
};

function setTestData(lecIdx, studentIdx, professorIdx, enrollmentIdx) {
    testData.lecIdx = lecIdx;
    testData.studentIdx = studentIdx;
    if (professorIdx !== undefined) {
        testData.professorIdx = professorIdx;
    }
    if (enrollmentIdx !== undefined) {
        testData.enrollmentIdx = enrollmentIdx;
    }
    console.log('✅ 테스트 데이터 업데이트:', testData);
    return testData;
}

function getTestData() {
    return testData;
}

// ============================================
// 전역으로 노출
// ============================================
window.gradeTestUtils = {
    // API 호출
    apiCall,
    apiGet,
    apiPut,
    checkAuth,
    
    // 데이터 관리
    testData,
    setTestData,
    getTestData,
    
    // 설정
    API_BASE_URL
};

console.log('✅ 성적 관리 테스트 유틸리티 로드 완료 (grade-test-utils.js)');
