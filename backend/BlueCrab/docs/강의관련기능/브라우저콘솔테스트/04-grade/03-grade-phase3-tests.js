/**
 * Phase 3: 이벤트 시스템 테스트
 * - 출석 업데이트 → 성적 재계산 이벤트
 * - 과제 채점 → 성적 재계산 이벤트
 */

// 유틸리티 함수 import (grade-test-utils.js 먼저 로드 필요)
const { apiPut, testData } = window.gradeTestUtils;

// ============================================
// 1. 출석 업데이트 테스트 (이벤트 시스템)
// PUT /api/enrollments/{enrollmentIdx}/attendance
// ============================================
async function testAttendanceUpdate() {
    console.log('━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━');
    console.log('📅 출석 업데이트 테스트 (이벤트 발행)');
    console.log('━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━');
    
    const requestData = {
        attended: 30,
        late: 5,
        absent: 5
    };
    
    console.log(`📤 Enrollment IDX: ${testData.enrollmentIdx}`);
    console.log('   - 출석:', requestData.attended);
    console.log('   - 지각:', requestData.late);
    console.log('   - 결석:', requestData.absent);
    
    const result = await apiPut(`/enrollments/${testData.enrollmentIdx}/attendance`, requestData);
    
    if (result && result.success) {
        console.log('\n✅ 출석 업데이트 성공');
        console.log('📡 성적 재계산 이벤트 발행됨 (비동기 처리)');
        console.log('💡 서버 로그에서 "성적 재계산 시작" 메시지 확인');
        
        if (result.data) {
            console.log('\n📊 업데이트된 출석 정보:', result.data);
        }
    } else {
        console.log('\n❌ 출석 업데이트 실패');
        console.log('오류:', result.error);
    }
    
    console.log('━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n');
    return result;
}

// ============================================
// 2. 과제 채점 테스트 (이벤트 시스템)
// PUT /api/assignments/{assignmentIdx}/grade
// ============================================
async function testAssignmentGrade() {
    console.log('━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━');
    console.log('📝 과제 채점 테스트 (이벤트 발행)');
    console.log('━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━');
    
    const requestData = {
        studentIdx: testData.studentIdx,
        score: 8,
        feedback: '잘 했습니다!'
    };
    
    console.log(`📤 Assignment IDX: ${testData.assignmentIdx}`);
    console.log('   - 학생 IDX:', requestData.studentIdx);
    console.log('   - 점수:', requestData.score);
    console.log('   - 피드백:', requestData.feedback);
    
    const result = await apiPut(`/assignments/${testData.assignmentIdx}/grade`, requestData);
    
    if (result && result.success) {
        console.log('\n✅ 과제 채점 성공');
        console.log('📡 성적 재계산 이벤트 발행됨 (비동기 처리)');
        console.log('💡 서버 로그에서 "성적 재계산 시작" 메시지 확인');
        
        if (result.data) {
            console.log('\n📊 채점 결과:', result.data);
        }
    } else {
        console.log('\n❌ 과제 채점 실패');
        console.log('오류:', result.error);
    }
    
    console.log('━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n');
    return result;
}

// ============================================
// Phase 3 전체 테스트 실행
// ============================================
async function runPhase3Tests() {
    console.log('🚀 Phase 3: 이벤트 시스템 테스트 시작');
    console.log('━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n');
    
    const results = {
        total: 2,
        success: 0,
        failed: 0,
        tests: []
    };
    
    try {
        const r1 = await testAttendanceUpdate();
        results.tests.push({ name: '출석 업데이트 (이벤트)', success: r1?.success || false });
        if (r1?.success) results.success++; else results.failed++;
        
        // 이벤트 처리 대기
        console.log('⏳ 이벤트 처리 대기 중 (2초)...\n');
        await new Promise(resolve => setTimeout(resolve, 2000));
        
        const r2 = await testAssignmentGrade();
        results.tests.push({ name: '과제 채점 (이벤트)', success: r2?.success || false });
        if (r2?.success) results.success++; else results.failed++;
        
        console.log('━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━');
        console.log('📊 Phase 3 테스트 결과');
        console.log('━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━');
        console.log(`총 테스트: ${results.total}개`);
        console.log(`✅ 성공: ${results.success}개`);
        console.log(`❌ 실패: ${results.failed}개`);
        console.log(`📈 성공률: ${((results.success / results.total) * 100).toFixed(1)}%`);
        console.log('\n상세 결과:');
        results.tests.forEach((test, idx) => {
            const icon = test.success ? '✅' : '❌';
            console.log(`  ${icon} ${idx + 1}. ${test.name}`);
        });
        console.log('━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n');
        
        if (results.success === results.total) {
            console.log('💡 서버 로그를 확인하여 이벤트 처리를 확인하세요:');
            console.log('   - "성적 재계산 시작: GradeUpdateEvent{...}"');
            console.log('   - "성적 재계산 완료: lecIdx=X, studentIdx=Y"\n');
        }
        
        return results;
        
    } catch (error) {
        console.error('❌ 테스트 실행 중 예외 발생:', error);
        return results;
    }
}

// ============================================
// 전역으로 노출
// ============================================
window.gradePhase3Tests = {
    // 개별 테스트
    attendance: testAttendanceUpdate,
    assignment: testAssignmentGrade,
    
    // 전체 실행
    runAll: runPhase3Tests
};

console.log('✅ Phase 3 테스트 로드 완료 (grade-phase3-tests.js)');
