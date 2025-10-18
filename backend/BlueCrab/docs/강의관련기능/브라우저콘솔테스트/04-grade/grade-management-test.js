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

// 테스트 데이터 (동적 변경 가능)
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

// 테스트 데이터 동적 변경 함수
function setTestData(lecIdx, studentIdx, professorIdx) {
    testData.lecIdx = lecIdx;
    testData.studentIdx = studentIdx;
    if (professorIdx !== undefined) {
        testData.professorIdx = professorIdx;
    }
    console.log('✅ 테스트 데이터 업데이트:', testData);
    return testData;
}

// 공통 fetch 함수 (개선된 에러 처리)
async function apiCall(endpoint, data) {
    const token = checkAuth();
    if (!token) {
        console.error('❌ 인증 토큰이 없습니다. 먼저 로그인하세요.');
        return { success: false, error: '인증 토큰 없음' };
    }

    const startTime = performance.now();

    try {
        const response = await fetch(`${API_BASE_URL}${endpoint}`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                'Authorization': `Bearer ${token}`
            },
            body: JSON.stringify(data)
        });
        
        const endTime = performance.now();
        const duration = (endTime - startTime).toFixed(2);
        
        // HTTP 상태 코드 확인
        if (!response.ok) {
            console.error(`❌ HTTP ${response.status}: ${response.statusText}`);
            const errorData = await response.json().catch(() => ({ message: response.statusText }));
            return { success: false, error: errorData.message, status: response.status, duration };
        }
        
        const result = await response.json();
        
        // API 응답 success 필드 확인
        if (result.success) {
            console.log(`✅ ${endpoint} 성공 (${duration}ms)`);
            console.log('📝 메시지:', result.message);
            return { ...result, duration };
        } else {
            console.error(`❌ ${endpoint} 실패 (${duration}ms)`);
            console.error('📝 메시지:', result.message);
            return { ...result, duration };
        }
        
    } catch (error) {
        const endTime = performance.now();
        const duration = (endTime - startTime).toFixed(2);
        console.error(`🔥 ${endpoint} 예외 발생 (${duration}ms):`, error.message);
        return { success: false, error: error.message, duration };
    }
}

// 1. 성적 구성 설정 테스트 - 개선된 버전
async function testGradeConfig() {
    console.log('━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━');
    console.log('⚙️  성적 구성 설정 테스트');
    console.log('━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━');
    
    const requestData = {
        lecIdx: testData.lecIdx,
        attendanceMaxScore: 20,
        assignmentTotalScore: 50,
        examTotalScore: 30,
        gradeDistribution: testData.gradeDistribution,
        action: "set-config"
    };
    
    console.log('📤 요청 데이터:', requestData);
    
    const result = await apiCall('/grade-config', requestData);
    
    if (result && result.success) {
        console.log('\n✅ 테스트 성공\n');
    } else {
        console.log('\n❌ 테스트 실패\n');
    }
    
    return result;
}

// 2. 개별 성적 조회 테스트 (학생용) - 개선된 버전
async function testStudentGradeInfo() {
    console.log('━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━');
    console.log('📊 학생 성적 조회 테스트');
    console.log('━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━');
    
    const requestData = {
        lecIdx: testData.lecIdx,
        studentIdx: testData.studentIdx,
        action: "get-grade"
    };
    
    console.log('📤 요청 데이터:', requestData);
    
    const result = await apiCall('/grade-info', requestData);
    
    // 응답 데이터 상세 출력
    if (result && result.success && result.data) {
        console.log('\n📊 성적 상세 정보:');
        
        if (result.data.grade) {
            const grade = result.data.grade;
            
            // 출석 정보
            if (grade.attendance) {
                console.log('  📅 출석:');
                console.log('    - 현재 점수:', grade.attendance.currentScore);
                console.log('    - 만점:', grade.attendance.maxScore);
                console.log('    - 비율:', grade.attendance.rate + '%');
            }
            
            // 과제 정보
            if (grade.assignments && grade.assignments.length > 0) {
                console.log('  📝 과제:', grade.assignments.length + '개');
                grade.assignments.forEach((assignment, idx) => {
                    console.log(`    ${idx + 1}. ${assignment.name}: ${assignment.score}/${assignment.maxScore}`);
                });
            }
            
            // 총점 정보
            if (grade.total) {
                console.log('  💯 총점:');
                console.log('    - 점수:', grade.total.score);
                console.log('    - 만점:', grade.total.maxScore);
                console.log('    - 백분율:', grade.total.percentage + '%');
            }
            
            // 등급 정보
            if (grade.letterGrade) {
                console.log('  🏆 등급:', grade.letterGrade);
            }
        }
        
        console.log('\n✅ 테스트 성공\n');
    } else {
        console.log('\n❌ 테스트 실패\n');
    }
    
    return result;
}

// 3. 교수용 성적 조회 테스트 - 개선된 버전
async function testProfessorGradeView() {
    console.log('━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━');
    console.log('👨‍🏫 교수용 성적 조회 테스트');
    console.log('━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━');
    
    const requestData = {
        lecIdx: testData.lecIdx,
        studentIdx: testData.studentIdx,
        professorIdx: testData.professorIdx,
        action: "professor-view"
    };
    
    console.log('📤 요청 데이터:', requestData);
    
    const result = await apiCall('/grade-info', requestData);
    
    if (result && result.success) {
        console.log('\n✅ 테스트 성공\n');
    } else {
        console.log('\n❌ 테스트 실패\n');
    }
    
    return result;
}

// 4. 전체 성적 목록 조회 테스트 - 개선된 버전
async function testGradeList() {
    console.log('━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━');
    console.log('📋 성적 목록 조회 테스트');
    console.log('━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━');
    
    const requestData = {
        lecIdx: testData.lecIdx,
        page: 0,
        size: 20,
        sortBy: "percentage",
        sortOrder: "desc",
        action: "list-all"
    };
    
    console.log('📤 요청 데이터:', requestData);
    
    const result = await apiCall('/grade-list', requestData);
    
    // 목록 데이터 요약 출력
    if (result && result.success && result.data) {
        if (result.data.content) {
            console.log('\n📊 조회 결과:');
            console.log('  - 총 학생 수:', result.data.totalElements || result.data.content.length);
            console.log('  - 현재 페이지:', result.data.currentPage || 0);
            console.log('  - 페이지 크기:', result.data.content.length);
        }
        console.log('\n✅ 테스트 성공\n');
    } else {
        console.log('\n❌ 테스트 실패\n');
    }
    
    return result;
}

// 5. 최종 등급 배정 테스트 - 개선된 버전
async function testGradeFinalize() {
    console.log('━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━');
    console.log('🏆 최종 등급 배정 테스트');
    console.log('━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━');
    
    const requestData = {
        lecIdx: testData.lecIdx,
        passingThreshold: testData.passingThreshold,
        gradeDistribution: testData.gradeDistribution,
        action: "finalize"
    };
    
    console.log('📤 요청 데이터:', requestData);
    
    const result = await apiCall('/grade-finalize', requestData);
    
    // 등급 배정 결과 출력
    if (result && result.success && result.data) {
        console.log('\n📊 등급 배정 결과:');
        
        if (result.data.gradeStats) {
            console.log('  등급별 학생 수:');
            Object.entries(result.data.gradeStats).forEach(([grade, count]) => {
                console.log(`    ${grade}: ${count}명`);
            });
        }
        
        if (result.data.totalStudents !== undefined) {
            console.log(`  총 학생: ${result.data.totalStudents}명`);
        }
        if (result.data.passingStudents !== undefined) {
            console.log(`  합격자: ${result.data.passingStudents}명`);
        }
        if (result.data.failingStudents !== undefined) {
            console.log(`  낙제자: ${result.data.failingStudents}명`);
        }
        if (result.data.averageScore !== undefined) {
            console.log(`  평균 점수: ${result.data.averageScore}`);
        }
        
        console.log('\n✅ 테스트 성공\n');
    } else {
        console.log('\n❌ 테스트 실패\n');
    }
    
    return result;
}

// 전체 테스트 실행 - 개선된 버전
async function runAllTests() {
    console.log('🚀 성적 관리 시스템 API 테스트 시작');
    console.log('━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━');
    console.log('📋 테스트 데이터:', testData);
    console.log('━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n');
    
    const results = {
        total: 5,
        success: 0,
        failed: 0,
        tests: []
    };
    
    try {
        // 1. 성적 구성 설정
        const r1 = await testGradeConfig();
        results.tests.push({ name: '성적 구성 설정', success: r1?.success || false });
        if (r1?.success) results.success++; else results.failed++;
        
        // 2. 학생 성적 조회
        const r2 = await testStudentGradeInfo();
        results.tests.push({ name: '학생 성적 조회', success: r2?.success || false });
        if (r2?.success) results.success++; else results.failed++;
        
        // 3. 교수용 성적 조회
        const r3 = await testProfessorGradeView();
        results.tests.push({ name: '교수용 성적 조회', success: r3?.success || false });
        if (r3?.success) results.success++; else results.failed++;
        
        // 4. 성적 목록 조회
        const r4 = await testGradeList();
        results.tests.push({ name: '성적 목록 조회', success: r4?.success || false });
        if (r4?.success) results.success++; else results.failed++;
        
        // 5. 최종 등급 배정
        const r5 = await testGradeFinalize();
        results.tests.push({ name: '최종 등급 배정', success: r5?.success || false });
        if (r5?.success) results.success++; else results.failed++;
        
        // 최종 결과 출력
        console.log('━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━');
        console.log('📊 테스트 결과 요약');
        console.log('━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━');
        console.log(`총 테스트: ${results.total}개`);
        console.log(`✅ 성공: ${results.success}개`);
        console.log(`❌ 실패: ${results.failed}개`);
        console.log('\n상세 결과:');
        results.tests.forEach((test, idx) => {
            const icon = test.success ? '✅' : '❌';
            console.log(`  ${icon} ${idx + 1}. ${test.name}`);
        });
        console.log('━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n');
        
        if (results.success === results.total) {
            console.log('🎉 모든 테스트가 성공했습니다!\n');
        } else {
            console.log('⚠️  일부 테스트가 실패했습니다.\n');
        }
        
        return results;
        
    } catch (error) {
        console.error('❌ 테스트 실행 중 예외 발생:', error);
        return results;
    }
}

// 커스텀 테스트 함수
async function testWithCustomData(lecIdx, studentIdx, professorIdx) {
    setTestData(lecIdx, studentIdx, professorIdx);
    return await testStudentGradeInfo();
}

// 개별 테스트 함수들을 전역으로 노출
window.gradeTests = {
    // 테스트 실행
    runAll: runAllTests,
    config: testGradeConfig,
    studentInfo: testStudentGradeInfo,
    professorView: testProfessorGradeView,
    gradeList: testGradeList,
    finalize: testGradeFinalize,
    
    // 유틸리티
    setData: setTestData,
    customTest: testWithCustomData,
    
    // 현재 테스트 데이터 조회
    getData: () => testData
};

// 사용법 안내
console.log(`
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
🎯 성적 관리 시스템 API 테스트 스크립트 v2.0 로드됨
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

⚠️  사전 준비: 먼저 교수 계정으로 로그인하세요!
📁 위치: docs/일반유저 로그인+게시판/test-1-login.js
📝 실행: await login() (교수 계정 사용)

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
📋 기본 사용법:
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

  gradeTests.runAll()         전체 테스트 실행 (권장)
  
  gradeTests.config()          성적 구성 설정
  gradeTests.studentInfo()     학생 성적 조회
  gradeTests.professorView()   교수용 성적 조회
  gradeTests.gradeList()       성적 목록 조회
  gradeTests.finalize()        최종 등급 배정

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
🛠️  고급 기능:
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

  gradeTests.getData()         현재 테스트 데이터 확인
  gradeTests.setData(1, 100)   테스트 데이터 변경
  gradeTests.customTest(1, 100) 커스텀 데이터로 테스트

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
✨ 개선사항 (v2.0):
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

  ✅ HTTP 상태 코드 검증
  ✅ 성공/실패 명확한 표시
  ✅ 응답 시간 측정
  ✅ 응답 데이터 구조화 출력
  ✅ 동적 테스트 데이터 변경
  ✅ 테스트 결과 요약

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
🚀 시작하기:
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

  await gradeTests.runAll()

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
`);