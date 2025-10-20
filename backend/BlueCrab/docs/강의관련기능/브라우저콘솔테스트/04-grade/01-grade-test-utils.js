/**
 * 성적 관리 시스템 - 공통 유틸리티 (테스트 도구 모음)
 * 
 * ============================================
 * 📋 이 파일의 역할
 * ============================================
 * 
 * ❌ 이 파일 자체로는 테스트를 실행하지 않습니다
 * ✅ 다른 테스트 파일(02, 03)이 사용할 "도구"를 제공합니다
 * 
 * 비유:
 * - 이 파일 = 공구함 🧰 (망치, 드라이버 등 도구만 제공)
 * - 02, 03 파일 = 실제 작업 📋 (도구를 사용해서 테스트 실행)
 * 
 * 제공하는 기능:
 * 1. API 호출 함수 (apiCall, apiGet, apiPut)
 * 2. 테스트 데이터 입력/저장 (inputTestData, setTestData)
 * 3. 데이터 검증 (validateTestData)
 * 4. 인증 확인 (checkAuth)
 * 
 * ============================================
 * 🚀 사용 방법
 * ============================================
 * 
 * 1단계: 이 파일을 브라우저 콘솔에 복사 & 붙여넣기
 * 
 * 2단계: 로드 확인
 *    window.gradeTestUtils
 *    → { apiCall: ƒ, testData: {...}, ... } 출력되면 성공
 * 
 * 3단계: 테스트 데이터 입력 (필수!)
 *    gradeTestUtils.inputTestData()
 *    → 팝업 창에서 강의ID, 학생ID 등 입력
 * 
 * 4단계: 실제 테스트 실행을 위해 다음 파일 로드
 *    - 02-grade-phase1-tests.js (5개 핵심 테스트)
 *    - 03-grade-phase3-tests.js (2개 이벤트 테스트)
 * 
 * ============================================
 * 💡 확인 가능한 것
 * ============================================
 * 
 * ✅ 도구 준비 확인:
 *    console.log(window.gradeTestUtils)
 * 
 * ✅ 인증 상태 확인:
 *    gradeTestUtils.checkAuth()
 * 
 * ✅ 데이터 입력 기능 확인:
 *    gradeTestUtils.inputTestData()
 * 
 * ✅ 현재 데이터 확인:
 *    gradeTestUtils.getTestData()
 * 
 * ❌ 실제 API 테스트 실행:
 *    → 불가능! 02, 03 파일 로드 후 runPhase1Tests() 등 실행
 */

// ============================================
// 기본 설정
// ============================================
// 브라우저 콘솔 재로드 대응: 전역 변수로 관리
if (typeof window.GRADE_API_BASE_URL === 'undefined') {
    window.GRADE_API_BASE_URL = 'https://bluecrab.chickenkiller.com/BlueCrab-1.0.0/api';
}
const API_BASE_URL = window.GRADE_API_BASE_URL;

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
// 브라우저 콘솔 재로드 대응: 전역 객체로 관리
if (typeof window.gradeTestData === 'undefined') {
    window.gradeTestData = {
        lecIdx: null,
        studentIdx: null,
        professorIdx: null,
        enrollmentIdx: null,
        assignmentIdx: null,
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
}
const testData = window.gradeTestData;

// 대화형 데이터 입력
function inputTestData() {
    console.log('\n📝 테스트 데이터 입력');
    console.log('━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━');
    
    const lecIdx = prompt('강의 IDX를 입력하세요:', testData.lecIdx || '');
    const studentIdx = prompt('학생 IDX를 입력하세요:', testData.studentIdx || '');
    const professorIdx = prompt('교수 IDX를 입력하세요 (선택):', testData.professorIdx || '');
    const enrollmentIdx = prompt('수강신청 IDX를 입력하세요 (선택):', testData.enrollmentIdx || '');
    const assignmentIdx = prompt('과제 IDX를 입력하세요 (선택):', testData.assignmentIdx || '');
    
    if (lecIdx) testData.lecIdx = parseInt(lecIdx);
    if (studentIdx) testData.studentIdx = parseInt(studentIdx);
    if (professorIdx) testData.professorIdx = parseInt(professorIdx);
    if (enrollmentIdx) testData.enrollmentIdx = parseInt(enrollmentIdx);
    if (assignmentIdx) testData.assignmentIdx = parseInt(assignmentIdx);
    
    console.log('✅ 테스트 데이터 업데이트:', testData);
    console.log('━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n');
    return testData;
}

// 프로그래밍 방식 데이터 설정
function setTestData(lecIdx, studentIdx, professorIdx, enrollmentIdx, assignmentIdx) {
    if (lecIdx !== undefined) testData.lecIdx = lecIdx;
    if (studentIdx !== undefined) testData.studentIdx = studentIdx;
    if (professorIdx !== undefined) testData.professorIdx = professorIdx;
    if (enrollmentIdx !== undefined) testData.enrollmentIdx = enrollmentIdx;
    if (assignmentIdx !== undefined) testData.assignmentIdx = assignmentIdx;
    
    console.log('✅ 테스트 데이터 업데이트:', testData);
    return testData;
}

function getTestData() {
    return testData;
}

// 데이터 검증
function validateTestData() {
    const errors = [];
    if (!testData.lecIdx) errors.push('lecIdx (강의 IDX)');
    if (!testData.studentIdx) errors.push('studentIdx (학생 IDX)');
    
    if (errors.length > 0) {
        console.warn('⚠️ 필수 데이터 누락:', errors.join(', '));
        console.warn('💡 gradeTestUtils.inputTestData() 실행하여 데이터 입력하세요.');
        return false;
    }
    return true;
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
    inputTestData,      // 대화형 입력
    setTestData,        // 프로그래밍 방식
    getTestData,
    validateTestData,   // 데이터 검증
    
    // 설정
    API_BASE_URL
};

console.log('✅ [1/4] 성적 관리 테스트 유틸리티 로드 완료');
console.log('━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━');
console.log('📝 다음 단계: 테스트 데이터 입력');
console.log('');
console.log('   방법 1: gradeTestUtils.inputTestData()    (대화형 입력)');
console.log('   방법 2: gradeTestUtils.setData(6, 100)    (직접 입력)');
console.log('');
console.log('💡 데이터 입력 후 02-grade-phase1-tests.js 로드하세요');
console.log('━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━');
