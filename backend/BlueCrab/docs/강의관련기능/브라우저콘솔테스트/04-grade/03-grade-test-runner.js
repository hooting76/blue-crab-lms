/**
 * 통합 테스트 러너 (완전 독립 버전)
 * 
 * 🎯 Phase 1 + Phase 3 통합 실행 편의 기능
 * 🔐 lecSerial + JWT 토큰 인증 방식
 * 
 * ============================================
 * 🚀 사용법
 * ============================================
 * 
 * 1단계: 교수 계정으로 로그인
 *    await login()
 * 
 * 2단계: Phase 1, 3 파일 로드 (v2 버전)
 *    - 02-grade-phase1-tests-v2.js
 *    - 03-grade-phase3-tests-v2.js
 * 
 * 3단계: 이 파일 로드
 *    - 04-grade-test-runner-v2.js
 * 
 * 4단계: 통합 실행
 *    gradeRunner.setup('CS101-2024-1', 'student@univ.edu')
 *    await gradeRunner.runAll()
 * 
 * ============================================
 * 💡 개별 Phase 실행
 * ============================================
 * 
 *    await gradeRunner.phase1()  - Phase 1만 실행
 *    await gradeRunner.phase3()  - Phase 3만 실행
 * 
 * ============================================
 * ⚠️  주의사항
 * ============================================
 * 
 * - 이 파일은 선택사항입니다
 * - Phase 1, 3 각각 독립 실행 가능합니다
 * - 이 러너는 단순히 편의 기능만 제공합니다
 */

(function() {
    'use strict';
    
    // ============================================
    // Phase 모듈 존재 확인
    // ============================================
    
    function checkPhases() {
        const missing = [];
        if (!window.gradePhase1) missing.push('Phase 1 (02-grade-phase1-tests-v2.js)');
        if (!window.gradePhase3) missing.push('Phase 3 (03-grade-phase3-tests-v2.js)');
        
        if (missing.length > 0) {
            console.error('❌ 필수 Phase 파일 미로드:');
            missing.forEach(m => console.error(`   - ${m}`));
            return false;
        }
        return true;
    }
    
    // ============================================
    // 통합 설정
    // ============================================
    
    function setup(lecSerial, studentEmail = 'student@univ.edu', assignmentIdx = null) {
        if (!checkPhases()) {
            console.error('❌ Phase 파일을 먼저 로드하세요!');
            return null;
        }
        
        console.log('\n⚙️  통합 설정');
        console.log('━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━');
        
        // Phase 1 설정
        if (window.gradePhase1) {
            window.gradePhase1.setLecture(lecSerial, studentEmail);
            console.log('✅ Phase 1 설정 완료');
        }
        
        // Phase 3 설정
        if (window.gradePhase3) {
            window.gradePhase3.setLecture(lecSerial, studentEmail, assignmentIdx);
            console.log('✅ Phase 3 설정 완료');
        }
        
        console.log('━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n');
        
        return { lecSerial, studentEmail, assignmentIdx };
    }
    
    function promptSetup() {
        if (!checkPhases()) {
            console.error('❌ Phase 파일을 먼저 로드하세요!');
            return null;
        }
        
        const lecSerial = prompt('강의 코드 (예: CS101-2024-1):', '');
        const studentEmail = prompt('학생 이메일:', 'student@univ.edu');
        const assignmentIdx = prompt('과제 IDX (선택, Phase 3용):', '');
        
        return setup(
            lecSerial,
            studentEmail,
            assignmentIdx ? parseInt(assignmentIdx) : null
        );
    }
    
    // ============================================
    // Phase 실행
    // ============================================
    
    async function runPhase1() {
        if (!window.gradePhase1) {
            console.error('❌ Phase 1 미로드! 02-grade-phase1-tests-v2.js 먼저 로드하세요.');
            return { success: false, error: 'Phase 1 미로드' };
        }
        
        console.log('\n🚀 Phase 1 실행 중...\n');
        return await window.gradePhase1.runAll();
    }
    
    async function runPhase3() {
        if (!window.gradePhase3) {
            console.error('❌ Phase 3 미로드! 03-grade-phase3-tests-v2.js 먼저 로드하세요.');
            return { success: false, error: 'Phase 3 미로드' };
        }
        
        console.log('\n🚀 Phase 3 실행 중...\n');
        return await window.gradePhase3.runAll();
    }
    
    // ============================================
    // 전체 통합 실행
    // ============================================
    
    async function runAll() {
        if (!checkPhases()) {
            console.error('❌ Phase 파일을 먼저 로드하세요!');
            return { success: false, error: 'Phase 미로드' };
        }
        
        console.log('\n━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━');
        console.log('🚀 성적 관리 통합 테스트 시작');
        console.log('━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n');
        
        const startTime = performance.now();
        const summary = {
            phase1: null,
            phase3: null,
            totalTests: 0,
            totalSuccess: 0,
            totalFailed: 0,
            duration: 0
        };
        
        try {
            // Phase 1 실행
            console.log('📦 Phase 1: 핵심 성적 관리 (5개 테스트)');
            console.log('─────────────────────────────────');
            summary.phase1 = await runPhase1();
            
            if (summary.phase1) {
                summary.totalTests += summary.phase1.total || 0;
                summary.totalSuccess += summary.phase1.success || 0;
                summary.totalFailed += summary.phase1.failed || 0;
            }
            
            console.log('\n');
            
            // Phase 3 실행
            console.log('📦 Phase 3: 이벤트 기반 업데이트 (2개 테스트)');
            console.log('─────────────────────────────────');
            summary.phase3 = await runPhase3();
            
            if (summary.phase3) {
                summary.totalTests += summary.phase3.total || 0;
                summary.totalSuccess += summary.phase3.success || 0;
                summary.totalFailed += summary.phase3.failed || 0;
            }
            
            summary.duration = ((performance.now() - startTime) / 1000).toFixed(2);
            
            // 통합 결과 출력
            console.log('\n━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━');
            console.log('📊 통합 테스트 결과');
            console.log('━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━');
            console.log(`⏱️  실행 시간: ${summary.duration}초`);
            console.log(`📦 총 테스트: ${summary.totalTests}개`);
            console.log(`✅ 성공: ${summary.totalSuccess}개`);
            console.log(`❌ 실패: ${summary.totalFailed}개`);
            console.log(`📈 성공률: ${((summary.totalSuccess / summary.totalTests) * 100).toFixed(1)}%`);
            
            console.log('\n📦 Phase별 상세:');
            if (summary.phase1) {
                console.log(`  Phase 1: ${summary.phase1.success}/${summary.phase1.total} 성공`);
            }
            if (summary.phase3) {
                console.log(`  Phase 3: ${summary.phase3.success}/${summary.phase3.total} 성공`);
            }
            
            console.log('━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n');
            
        } catch (error) {
            console.error('❌ 통합 실행 중 예외:', error);
            summary.error = error.message;
        }
        
        return summary;
    }
    
    // ============================================
    // 개별 테스트 바로가기
    // ============================================
    
    const shortcuts = {
        // Phase 1
        config: async () => window.gradePhase1?.config(),
        studentInfo: async () => window.gradePhase1?.studentInfo(),
        professorView: async () => window.gradePhase1?.professorView(),
        gradeList: async () => window.gradePhase1?.gradeList(),
        finalize: async () => window.gradePhase1?.finalize(),
        
        // Phase 3
        attendance: async () => window.gradePhase3?.attendance(),
        assignment: async () => window.gradePhase3?.assignment()
    };
    
    // ============================================
    // 전역 노출
    // ============================================
    
    window.gradeRunner = {
        // 설정
        setup,
        promptSetup,
        check: checkPhases,
        
        // Phase별 실행
        phase1: runPhase1,
        phase3: runPhase3,
        
        // 통합 실행
        runAll,
        
        // 바로가기
        ...shortcuts
    };
    
    console.log('✅ 통합 테스트 러너 로드 완료');
    console.log('━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━');
    console.log('🎯 Phase 1 + Phase 3 통합 실행');
    console.log('');
    console.log('📝 시작하기:');
    console.log('   1. gradeRunner.setup("CS101-2024-1", "student@univ.edu")');
    console.log('   2. await gradeRunner.runAll()');
    console.log('');
    console.log('💡 개별 실행:');
    console.log('   await gradeRunner.phase1()');
    console.log('   await gradeRunner.phase3()');
    console.log('');
    console.log('⚡ 바로가기:');
    console.log('   await gradeRunner.config()');
    console.log('   await gradeRunner.attendance()');
    console.log('━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━');
    
})();
