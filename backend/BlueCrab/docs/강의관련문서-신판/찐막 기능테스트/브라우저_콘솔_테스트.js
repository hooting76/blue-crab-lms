/**
 * ============================================
 * BlueCrab LMS 성적 기능 브라우저 콘솔 테스트
 * ============================================
 * 
 * 사용 방법:
 * 1. 브라우저에서 F12 눌러 개발자 도구 오픈
 * 2. Console 탭으로 이동
 * 3. 이 스크립트 전체를 복사
 * 4. 콘솔에 붙여넣기 후 Enter
 * 5. 각 함수를 호출하여 테스트
 * 
 * 작성일: 2025-10-27
 */

// ============================================
// 설정 변수
// ============================================
const API_BASE_URL = 'https://bluecrab.chickenkiller.com/BlueCrab-1.0.0';
const TEST_LEC_IDX = 48;  // 테스트용 강의 IDX (ETH201 - 현대 윤리학)
const TEST_STUDENT_IDX = 35;  // 테스트용 학생 IDX (박지훈)

// ============================================
// 유틸리티 함수
// ============================================

/**
 * API 호출 헬퍼 함수
 */
async function callAPI(endpoint, data) {
    const url = `${API_BASE_URL}/enrollments${endpoint}`;
    
    console.log(`\n📡 API 호출: /enrollments${endpoint}`);
    console.log('📤 Request:', JSON.stringify(data, null, 2));
    
    try {
        const response = await fetch(url, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
            },
            body: JSON.stringify(data)
        });
        
        const result = await response.json();
        
        console.log(`✅ Response Status: ${response.status}`);
        console.log('📥 Response:', JSON.stringify(result, null, 2));
        
        return result;
    } catch (error) {
        console.error('❌ API 호출 실패:', error);
        throw error;
    }
}

/**
 * JSON 데이터 예쁘게 출력
 */
function prettyPrint(label, data) {
    console.log(`\n${label}:`);
    console.log(JSON.stringify(data, null, 2));
}

/**
 * 테스트 결과 검증
 */
function assertValue(label, expected, actual) {
    const passed = expected === actual;
    const icon = passed ? '✅' : '❌';
    console.log(`${icon} ${label}: 예상=${expected}, 실제=${actual}`);
    return passed;
}

/**
 * 소수점 반올림 검증 (둘째자리)
 */
function assertFloat(label, expected, actual, precision = 2) {
    const roundedExpected = Math.round(expected * 100) / 100;
    const roundedActual = Math.round(actual * 100) / 100;
    const passed = Math.abs(roundedExpected - roundedActual) < 0.01;
    const icon = passed ? '✅' : '❌';
    console.log(`${icon} ${label}: 예상=${roundedExpected}, 실제=${roundedActual}`);
    return passed;
}

// ============================================
// 테스트 함수들
// ============================================

/**
 * TEST-1: 성적 구성 설정
 * 
 * 목적: 강의의 성적 배점 및 등급 분포 설정
 * 예상: gradeConfig가 모든 수강생에게 추가됨
 */
async function test1_setGradeConfig() {
    console.log('\n' + '='.repeat(60));
    console.log('🧪 TEST-1: 성적 구성 설정');
    console.log('='.repeat(60));
    
    const requestData = {
        action: "set-config",
        lecIdx: TEST_LEC_IDX,
        attendanceMaxScore: 20,
        assignmentTotalScore: 25,
        latePenaltyPerSession: 0.5,
        gradeDistribution: {
            A: 30,
            B: 40,
            C: 20,
            D: 10
        }
    };
    
    const result = await callAPI('/grade-config', requestData);
    
    // 검증
    console.log('\n📊 검증:');
    assertValue('success', true, result.success);
    assertValue('attendanceMaxScore', 20, result.data?.gradeConfig?.attendanceMaxScore);
    assertValue('assignmentTotalScore', 25, result.data?.gradeConfig?.assignmentTotalScore);
    assertValue('totalMaxScore', 45, result.data?.gradeConfig?.totalMaxScore);
    assertFloat('latePenaltyPerSession', 0.5, result.data?.gradeConfig?.latePenaltyPerSession);
    
    console.log(`\n✅ 업데이트된 수강생 수: ${result.data?.updatedEnrollments}`);
    console.log(`⚠️  실패한 수강생 수: ${result.data?.failedEnrollments}`);
    
    return result;
}

/**
 * TEST-2: 빈 데이터 학생 성적 조회
 * 
 * 목적: 출석/과제 데이터가 없는 학생의 성적이 0점으로 처리되는지 확인
 * 예상: currentScore=0, assignments=[], total.score=0
 */
async function test2_getEmptyGrade() {
    console.log('\n' + '='.repeat(60));
    console.log('🧪 TEST-2: 빈 데이터 학생 성적 조회');
    console.log('='.repeat(60));
    
    const requestData = {
        action: "get-grade",
        lecIdx: TEST_LEC_IDX,
        studentIdx: 7  // 데이터 없는 학생
    };
    
    const result = await callAPI('/grade-info', requestData);
    
    // 검증
    console.log('\n📊 검증:');
    assertValue('success', true, result.success);
    assertFloat('attendanceScore.currentScore', 0.0, result.data?.grade?.attendanceScore?.currentScore);
    assertValue('assignments 개수', 0, result.data?.grade?.assignments?.length || 0);
    assertFloat('total.score', 0.0, result.data?.grade?.total?.score);
    
    prettyPrint('📋 성적 데이터', result.data?.grade);
    
    return result;
}

/**
 * TEST-3: 출석 데이터 있는 학생 성적 조회
 * 
 * 목적: 출석 점수 계산 및 지각 감점 적용 확인
 * 예상: 출석 8/10, 지각 2회 → 16.0 - 1.0 = 15.0점 (75%)
 */
async function test3_getAttendanceGrade() {
    console.log('\n' + '='.repeat(60));
    console.log('🧪 TEST-3: 출석 데이터 있는 학생 성적 조회');
    console.log('='.repeat(60));
    console.log('⚠️  사전에 SQL로 출석 데이터를 추가해야 합니다!');
    console.log('   UPDATE ENROLLMENT_EXTENDED_TBL');
    console.log('   SET ENROLLMENT_DATA = JSON_SET(...)');
    console.log('   WHERE LEC_IDX = 6 AND STUDENT_IDX = 6;');
    
    const requestData = {
        action: "get-grade",
        lecIdx: TEST_LEC_IDX,
        studentIdx: TEST_STUDENT_IDX
    };
    
    const result = await callAPI('/grade-info', requestData);
    
    // 검증
    console.log('\n📊 검증:');
    assertValue('success', true, result.success);
    
    const attScore = result.data?.grade?.attendanceScore;
    if (attScore) {
        console.log('\n📝 출석 점수 계산:');
        console.log(`   출석수: ${attScore.presentCount || 0}`);
        console.log(`   지각수: ${attScore.lateCount || 0}`);
        console.log(`   결석수: ${attScore.absentCount || 0}`);
        console.log(`   출석률: ${attScore.attendanceRate || 0}%`);
        
        // 예상 계산 (출석 8/10, 지각 2)
        const expectedScore = 16.0;  // (8/10) * 20
        const latePenalty = 1.0;  // 2 * 0.5
        const expectedFinal = 15.0;  // 16.0 - 1.0
        
        console.log(`\n🔢 계산 검증:`);
        console.log(`   출석 점수: (${attScore.presentCount}/10) × 20 = ${expectedScore}`);
        console.log(`   지각 감점: ${attScore.lateCount} × 0.5 = ${latePenalty}`);
        console.log(`   최종 점수: ${expectedScore} - ${latePenalty} = ${expectedFinal}`);
        
        assertFloat('currentScore', expectedFinal, attScore.currentScore);
        assertFloat('latePenalty', latePenalty, attScore.latePenalty || 0);
        assertFloat('percentage', 75.0, attScore.percentage);
    }
    
    prettyPrint('📋 성적 데이터', result.data?.grade);
    
    return result;
}

/**
 * TEST-4: 출석+과제 모두 있는 학생 성적 조회
 * 
 * 목적: 출석 + 과제 점수 합산 및 총점 계산 확인
 * 예상: 15.0(출석) + 22.0(과제) = 37.0점 (82.22%)
 */
async function test4_getFullGrade() {
    console.log('\n' + '='.repeat(60));
    console.log('🧪 TEST-4: 출석+과제 모두 있는 학생 성적 조회');
    console.log('='.repeat(60));
    console.log('⚠️  사전에 SQL로 과제 제출 데이터를 추가해야 합니다!');
    console.log('   UPDATE ASSIGNMENT_EXTENDED_TBL');
    console.log('   SET ASSIGNMENT_DATA = JSON_ARRAY_APPEND(...)');
    console.log('   WHERE ASSIGNMENT_IDX = 1;');
    
    const requestData = {
        action: "get-grade",
        lecIdx: TEST_LEC_IDX,
        studentIdx: TEST_STUDENT_IDX
    };
    
    const result = await callAPI('/grade-info', requestData);
    
    // 검증
    console.log('\n📊 검증:');
    assertValue('success', true, result.success);
    
    const grade = result.data?.grade;
    if (grade) {
        const attScore = grade.attendanceScore?.currentScore || 0;
        const assignments = grade.assignments || [];
        const assignmentScore = assignments.reduce((sum, a) => sum + (a.score || 0), 0);
        const totalScore = grade.total?.score || 0;
        const totalMax = grade.total?.maxScore || 0;
        const percentage = grade.total?.percentage || 0;
        
        console.log('\n🔢 총점 계산:');
        console.log(`   출석 점수: ${attScore}`);
        console.log(`   과제 개수: ${assignments.length}`);
        
        assignments.forEach((a, idx) => {
            console.log(`   - 과제${idx + 1}: ${a.score}/${a.maxScore} (${a.percentage}%)`);
        });
        
        console.log(`   과제 총점: ${assignmentScore}`);
        console.log(`   총점: ${attScore} + ${assignmentScore} = ${totalScore}`);
        console.log(`   총 만점: ${totalMax}`);
        console.log(`   백분율: (${totalScore} / ${totalMax}) × 100 = ${percentage}%`);
        
        // 예상 값 (출석 15.0 + 과제 22.0 = 37.0)
        const expectedTotal = attScore + assignmentScore;
        const expectedPercentage = (expectedTotal / totalMax) * 100;
        
        assertFloat('total.score', expectedTotal, totalScore);
        assertFloat('total.percentage', expectedPercentage, percentage);
    }
    
    prettyPrint('📋 전체 성적 데이터', grade);
    
    return result;
}

/**
 * TEST-5: 전체 수강생 성적 목록 조회
 * 
 * 목적: 전체 수강생의 성적 계산 및 순위 확인
 * 예상: 백분율 기준 내림차순 정렬, 통계 계산
 */
async function test5_getGradeList() {
    console.log('\n' + '='.repeat(60));
    console.log('🧪 TEST-5: 전체 수강생 성적 목록 조회');
    console.log('='.repeat(60));
    
    const requestData = {
        action: "list-all",
        lecIdx: TEST_LEC_IDX,
        professorIdx: 6  // 교수 IDX (필요시 변경)
    };
    
    const result = await callAPI('/grade-list', requestData);
    
    // 검증
    console.log('\n📊 검증:');
    assertValue('success', true, result.success);
    
    const students = result.data?.students || [];
    const stats = result.data?.statistics;
    
    console.log(`\n👥 수강생 수: ${students.length}`);
    
    if (students.length > 0) {
        console.log('\n🏆 순위별 성적:');
        students.forEach((student, idx) => {
            const rank = student.rank || (idx + 1);
            const name = student.studentName || 'Unknown';
            const percentage = student.grade?.total?.percentage || 0;
            const score = student.grade?.total?.score || 0;
            const maxScore = student.grade?.total?.maxScore || 0;
            
            console.log(`   ${rank}위: ${name} - ${score}/${maxScore} (${percentage}%)`);
        });
        
        // 순위 정확성 검증 (백분율 내림차순)
        console.log('\n✅ 순위 정렬 검증:');
        for (let i = 0; i < students.length - 1; i++) {
            const current = students[i].grade?.total?.percentage || 0;
            const next = students[i + 1].grade?.total?.percentage || 0;
            const isCorrect = current >= next;
            const icon = isCorrect ? '✅' : '❌';
            console.log(`   ${icon} ${i + 1}위(${current}%) >= ${i + 2}위(${next}%)`);
        }
    }
    
    if (stats) {
        console.log('\n📈 통계:');
        console.log(`   총 학생 수: ${stats.totalStudents}`);
        console.log(`   평균 점수: ${stats.averageScore}%`);
        console.log(`   최고 점수: ${stats.highestScore}%`);
        console.log(`   최저 점수: ${stats.lowestScore}%`);
    }
    
    prettyPrint('📋 전체 데이터', result.data);
    
    return result;
}

/**
 * TEST-6: 실제 JSON 구조 검증
 * 
 * 목적: ENROLLMENT_DATA의 JSON 구조가 예상대로인지 확인
 */
async function test6_verifyJSONStructure() {
    console.log('\n' + '='.repeat(60));
    console.log('🧪 TEST-6: JSON 구조 검증');
    console.log('='.repeat(60));
    
    const result = await test4_getFullGrade();
    const enrollmentData = result.data;
    
    console.log('\n📋 JSON 구조 검증:');
    
    // 필수 필드 확인
    const checks = [
        { path: 'grade', exists: !!enrollmentData?.grade },
        { path: 'grade.attendanceScore', exists: !!enrollmentData?.grade?.attendanceScore },
        { path: 'grade.assignments', exists: Array.isArray(enrollmentData?.grade?.assignments) },
        { path: 'grade.total', exists: !!enrollmentData?.grade?.total },
        { path: 'gradeConfig', exists: !!enrollmentData?.gradeConfig },
        { path: 'attendance', exists: !!enrollmentData?.attendance },
    ];
    
    checks.forEach(check => {
        const icon = check.exists ? '✅' : '❌';
        console.log(`   ${icon} ${check.path}: ${check.exists ? 'EXISTS' : 'MISSING'}`);
    });
    
    // 상세 구조 출력
    console.log('\n📦 전체 JSON 구조:');
    prettyPrint('ENROLLMENT_DATA', enrollmentData);
    
    // 제공된 예시 구조와 비교
    console.log('\n🔍 예시 구조와의 비교:');
    console.log('   attendanceScore: ', enrollmentData?.grade?.attendanceScore ? 'null이 아님' : 'null');
    console.log('   assignments 개수: ', enrollmentData?.grade?.assignments?.length || 0);
    console.log('   attendance.sessions 개수: ', enrollmentData?.attendance?.sessions?.length || 0);
    console.log('   attendance.pendingRequests 개수: ', enrollmentData?.attendance?.pendingRequests?.length || 0);
    
    return enrollmentData;
}

/**
 * TEST-7: 에러 케이스 테스트
 * 
 * 목적: 잘못된 요청에 대한 에러 처리 확인
 */
async function test7_errorCases() {
    console.log('\n' + '='.repeat(60));
    console.log('🧪 TEST-7: 에러 케이스 테스트');
    console.log('='.repeat(60));
    
    // 7-1: 존재하지 않는 강의
    console.log('\n📌 7-1: 존재하지 않는 강의');
    try {
        const result1 = await callAPI('/grade-info', {
            action: "get-grade",
            lecIdx: 99999,
            studentIdx: TEST_STUDENT_IDX
        });
        console.log(result1.success ? '⚠️  에러가 발생해야 하는데 성공함' : '✅ 정상적으로 에러 처리됨');
    } catch (error) {
        console.log('✅ 예상대로 에러 발생:', error.message);
    }
    
    // 7-2: 존재하지 않는 학생
    console.log('\n📌 7-2: 존재하지 않는 학생');
    try {
        const result2 = await callAPI('/grade-info', {
            action: "get-grade",
            lecIdx: TEST_LEC_IDX,
            studentIdx: 99999
        });
        console.log(result2.success ? '⚠️  에러가 발생해야 하는데 성공함' : '✅ 정상적으로 에러 처리됨');
    } catch (error) {
        console.log('✅ 예상대로 에러 발생:', error.message);
    }
    
    // 7-3: 잘못된 액션
    console.log('\n📌 7-3: 잘못된 액션');
    try {
        const result3 = await callAPI('/grade-info', {
            action: "invalid-action",
            lecIdx: TEST_LEC_IDX,
            studentIdx: TEST_STUDENT_IDX
        });
        console.log(result3.success ? '⚠️  에러가 발생해야 하는데 성공함' : '✅ 정상적으로 에러 처리됨');
    } catch (error) {
        console.log('✅ 예상대로 에러 발생:', error.message);
    }
}

/**
 * 전체 테스트 실행
 */
async function runAllTests() {
    console.log('\n' + '🚀'.repeat(30));
    console.log('🧪 BlueCrab LMS 성적 기능 전체 테스트 시작');
    console.log('🚀'.repeat(30));
    
    const startTime = Date.now();
    
    try {
        await test1_setGradeConfig();
        await new Promise(resolve => setTimeout(resolve, 1000)); // 1초 대기
        
        await test2_getEmptyGrade();
        await new Promise(resolve => setTimeout(resolve, 1000));
        
        await test3_getAttendanceGrade();
        await new Promise(resolve => setTimeout(resolve, 1000));
        
        await test4_getFullGrade();
        await new Promise(resolve => setTimeout(resolve, 1000));
        
        await test5_getGradeList();
        await new Promise(resolve => setTimeout(resolve, 1000));
        
        await test6_verifyJSONStructure();
        await new Promise(resolve => setTimeout(resolve, 1000));
        
        await test7_errorCases();
        
        const endTime = Date.now();
        const duration = ((endTime - startTime) / 1000).toFixed(2);
        
        console.log('\n' + '✅'.repeat(30));
        console.log(`✅ 전체 테스트 완료! (소요 시간: ${duration}초)`);
        console.log('✅'.repeat(30));
        
    } catch (error) {
        console.error('\n❌ 테스트 중 오류 발생:', error);
    }
}

// ============================================
// 사용 가이드 출력
// ============================================
console.log('\n' + '📖'.repeat(30));
console.log('BlueCrab LMS 성적 기능 브라우저 콘솔 테스트');
console.log('📖'.repeat(30));
console.log('\n🎯 사용 가능한 테스트 함수:');
console.log('   • runAllTests()           - 전체 테스트 실행');
console.log('   • test1_setGradeConfig()  - 성적 구성 설정');
console.log('   • test2_getEmptyGrade()   - 빈 데이터 성적 조회');
console.log('   • test3_getAttendanceGrade() - 출석 성적 조회');
console.log('   • test4_getFullGrade()    - 출석+과제 성적 조회');
console.log('   • test5_getGradeList()    - 전체 성적 목록');
console.log('   • test6_verifyJSONStructure() - JSON 구조 검증');
console.log('   • test7_errorCases()      - 에러 케이스 테스트');
console.log('\n💡 예시:');
console.log('   runAllTests()  // 전체 실행');
console.log('   test1_setGradeConfig()  // 개별 실행');
console.log('\n⚙️  설정:');
console.log(`   API_BASE_URL = ${API_BASE_URL}`);
console.log(`   Enrollment Endpoint = /api/enrollments/*`);
console.log(`   TEST_LEC_IDX = ${TEST_LEC_IDX} (ETH201 - 현대 윤리학)`);
console.log(`   TEST_STUDENT_IDX = ${TEST_STUDENT_IDX} (박지훈)`);
console.log('\n⚠️  주의사항:');
console.log('   1. 백엔드 서버가 실행 중이어야 합니다');
console.log('      (https://bluecrab.chickenkiller.com/BlueCrab-1.0.0)');
console.log('   2. TEST-3, TEST-4는 SQL로 데이터를 먼저 추가해야 합니다');
console.log('   3. CORS 이슈 발생 시 백엔드 설정을 확인하세요');
console.log('\n' + '='.repeat(60));
