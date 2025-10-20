/**
 * 성적 관리 시스템 - 통합 테스트 러너
 * 모든 Phase 테스트를 실행하는 메인 파일
 */

// ============================================
// 전체 테스트 실행
// ============================================
async function runAllTests() {
    console.log('🚀 성적 관리 시스템 전체 테스트 시작');
    console.log('━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━');
    console.log('📋 테스트 데이터:', window.gradeTestUtils.testData);
    console.log('━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n');
    
    const allResults = {
        phase1: null,
        phase3: null,
        totalTests: 0,
        totalSuccess: 0,
        totalFailed: 0
    };
    
    try {
        // Phase 1 테스트 실행
        console.log('📌 Phase 1 테스트 시작...\n');
        allResults.phase1 = await window.gradePhase1Tests.runAll();
        
        console.log('⏳ Phase 간 대기 시간 (3초)...\n');
        await new Promise(resolve => setTimeout(resolve, 3000));
        
        // Phase 3 테스트 실행
        console.log('📌 Phase 3 테스트 시작...\n');
        allResults.phase3 = await window.gradePhase3Tests.runAll();
        
        // 전체 결과 집계
        allResults.totalTests = allResults.phase1.total + allResults.phase3.total;
        allResults.totalSuccess = allResults.phase1.success + allResults.phase3.success;
        allResults.totalFailed = allResults.phase1.failed + allResults.phase3.failed;
        
        // 최종 결과 출력
        console.log('\n━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━');
        console.log('📊 전체 테스트 결과 요약');
        console.log('━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━');
        console.log(`총 테스트: ${allResults.totalTests}개`);
        console.log(`✅ 성공: ${allResults.totalSuccess}개`);
        console.log(`❌ 실패: ${allResults.totalFailed}개`);
        console.log(`📈 전체 성공률: ${((allResults.totalSuccess / allResults.totalTests) * 100).toFixed(1)}%`);
        
        console.log('\n상세 결과:');
        console.log('  Phase 1 (핵심 기능):');
        allResults.phase1.tests.forEach((test, idx) => {
            const icon = test.success ? '✅' : '❌';
            console.log(`    ${icon} ${idx + 1}. ${test.name}`);
        });
        
        console.log('\n  Phase 3 (이벤트 시스템):');
        allResults.phase3.tests.forEach((test, idx) => {
            const icon = test.success ? '✅' : '❌';
            console.log(`    ${icon} ${idx + 1}. ${test.name}`);
        });
        
        console.log('━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n');
        
        if (allResults.totalSuccess === allResults.totalTests) {
            console.log('🎉 모든 테스트가 성공했습니다!');
            console.log('💡 서버 로그를 확인하여 이벤트 처리를 확인하세요.\n');
        } else {
            console.log('⚠️  일부 테스트가 실패했습니다.');
            console.log('💡 실패한 테스트를 개별적으로 재실행해보세요.\n');
        }
        
        return allResults;
        
    } catch (error) {
        console.error('❌ 전체 테스트 실행 중 예외 발생:', error);
        return allResults;
    }
}

// ============================================
// 시나리오 테스트: 전체 플로우
// ============================================
async function runScenarioTest() {
    console.log('🎬 시나리오 테스트: 성적 관리 전체 플로우');
    console.log('━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n');
    
    try {
        // 1단계: 성적 구성 설정
        console.log('📌 1단계: 성적 구성 설정');
        await window.gradePhase1Tests.config();
        await new Promise(resolve => setTimeout(resolve, 1000));
        
        // 2단계: 출석 기록
        console.log('📌 2단계: 출석 기록');
        await window.gradePhase3Tests.attendance();
        await new Promise(resolve => setTimeout(resolve, 2000)); // 이벤트 처리 대기
        
        // 3단계: 과제 채점
        console.log('📌 3단계: 과제 채점');
        await window.gradePhase3Tests.assignment();
        await new Promise(resolve => setTimeout(resolve, 2000)); // 이벤트 처리 대기
        
        // 4단계: 성적 확인
        console.log('📌 4단계: 학생 성적 확인');
        await window.gradePhase1Tests.studentInfo();
        await new Promise(resolve => setTimeout(resolve, 1000));
        
        // 5단계: 성적 목록 조회
        console.log('📌 5단계: 전체 성적 목록 조회');
        await window.gradePhase1Tests.gradeList();
        await new Promise(resolve => setTimeout(resolve, 1000));
        
        // 6단계: 최종 등급 배정
        console.log('📌 6단계: 최종 등급 배정');
        await window.gradePhase1Tests.finalize();
        
        console.log('\n━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━');
        console.log('✅ 시나리오 테스트 완료!');
        console.log('━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n');
        
    } catch (error) {
        console.error('❌ 시나리오 테스트 중 예외 발생:', error);
    }
}

// ============================================
// 커스텀 테스트
// ============================================
async function testWithCustomData(lecIdx, studentIdx, professorIdx, enrollmentIdx) {
    window.gradeTestUtils.setTestData(lecIdx, studentIdx, professorIdx, enrollmentIdx);
    return await window.gradePhase1Tests.studentInfo();
}

// ============================================
// 전역 객체 통합
// ============================================
window.gradeTests = {
    // === 전체 테스트 ===
    runAll: runAllTests,
    scenario: runScenarioTest,
    
    // === Phase 1 테스트 ===
    config: window.gradePhase1Tests.config,
    studentInfo: window.gradePhase1Tests.studentInfo,
    professorView: window.gradePhase1Tests.professorView,
    gradeList: window.gradePhase1Tests.gradeList,
    finalize: window.gradePhase1Tests.finalize,
    
    // === Phase 3 테스트 ===
    attendance: window.gradePhase3Tests.attendance,
    assignment: window.gradePhase3Tests.assignment,
    
    // === Phase별 전체 실행 ===
    phase1: window.gradePhase1Tests.runAll,
    phase3: window.gradePhase3Tests.runAll,
    
    // === 유틸리티 ===
    setData: window.gradeTestUtils.setTestData,
    getData: window.gradeTestUtils.getTestData,
    customTest: testWithCustomData
};

// ============================================
// 사용법 안내
// ============================================
console.log(`
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
🎯 성적 관리 시스템 API 테스트 v3.0 (모듈화) 로드 완료
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

⚠️  사전 준비: 먼저 교수 계정으로 로그인하세요!
📁 위치: docs/일반유저 로그인+게시판/test-1-login.js
📝 실행: await login() (교수 계정 사용)

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
📋 기본 사용법:
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

  ⭐ await gradeTests.runAll()      전체 테스트 실행 (7개)
  🎬 await gradeTests.scenario()    시나리오 테스트 (전체 플로우)
  
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
📝 Phase별 전체 실행:
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

  await gradeTests.phase1()         Phase 1 전체 (5개)
  await gradeTests.phase3()         Phase 3 전체 (2개)

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
🧪 개별 테스트:
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

  Phase 1 (핵심 기능):
  1️⃣ await gradeTests.config()         성적 구성 설정
  2️⃣ await gradeTests.studentInfo()    학생 성적 조회
  3️⃣ await gradeTests.professorView()  교수용 성적 조회
  4️⃣ await gradeTests.gradeList()      성적 목록 조회
  5️⃣ await gradeTests.finalize()       최종 등급 배정

  Phase 3 (이벤트 시스템):
  6️⃣ await gradeTests.attendance()     출석 업데이트 → 이벤트
  7️⃣ await gradeTests.assignment()     과제 채점 → 이벤트

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
🛠️  유틸리티:
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

  gradeTests.getData()                  현재 테스트 데이터 확인
  gradeTests.setData(1, 100, 22, 1)     테스트 데이터 변경
  gradeTests.customTest(1, 100)         커스텀 데이터로 조회

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
✨ v3.0 모듈화 구조:
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

  ✅ grade-test-utils.js       공통 유틸리티 (API 호출, 데이터 관리)
  ✅ grade-phase1-tests.js     Phase 1 테스트 (핵심 기능 5개)
  ✅ grade-phase3-tests.js     Phase 3 테스트 (이벤트 2개)
  ✅ grade-test-runner.js      통합 테스트 러너 (현재 파일)

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
🚀 빠른 시작:
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

  1. 로그인 먼저!
  2. await gradeTests.runAll()         (전체 테스트)
  3. 또는 await gradeTests.scenario()  (시나리오 테스트)

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
`);

console.log('✅ 통합 테스트 러너 로드 완료 (grade-test-runner.js)');
