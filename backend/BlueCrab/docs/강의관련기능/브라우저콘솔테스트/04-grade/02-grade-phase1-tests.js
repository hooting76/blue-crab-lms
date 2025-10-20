/**
 * Phase 1: 핵심 성적 관리 기능 테스트
 * - 성적 구성 설정
 * - 학생 성적 조회
 * - 교수용 성적 조회
 * - 성적 목록 조회
 * - 최종 등급 배정
 */

// 유틸리티 함수 import (grade-test-utils.js 먼저 로드 필요)
const { apiCall, apiGet, apiPut, testData } = window.gradeTestUtils;

// ============================================
// 1. 성적 구성 설정 테스트
// POST /api/enrollments/grade-config
// ============================================
async function testGradeConfig() {
    console.log('━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━');
    console.log('⚙️  성적 구성 설정 테스트');
    console.log('━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━');
    
    const requestData = {
        lecIdx: testData.lecIdx,
        attendanceMaxScore: testData.attendanceMaxScore,
        assignmentTotalMaxScore: testData.assignmentTotalMaxScore,
        latePenaltyPerSession: testData.latePenaltyPerSession,
        gradeDistribution: testData.gradeDistribution
    };
    
    console.log('📤 요청 데이터:', requestData);
    console.log('   - 출석 만점:', requestData.attendanceMaxScore);
    console.log('   - 과제 만점:', requestData.assignmentTotalMaxScore);
    console.log('   - 지각 페널티:', requestData.latePenaltyPerSession + '점/회');
    
    const result = await apiCall('/enrollments/grade-config', requestData);
    
    if (result && result.success) {
        console.log('\n✅ 성적 구성 설정 성공');
        if (result.data) {
            console.log('📊 설정된 값:', result.data);
        }
    } else {
        console.log('\n❌ 성적 구성 설정 실패');
        console.log('오류:', result.error);
    }
    
    console.log('━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n');
    return result;
}

// ============================================
// 2. 개별 학생 성적 조회 테스트
// GET /api/enrollments/{lecIdx}/{studentIdx}/grade
// ============================================
async function testStudentGradeInfo() {
    console.log('━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━');
    console.log('📊 학생 성적 조회 테스트');
    console.log('━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━');
    
    console.log(`📤 강의 IDX: ${testData.lecIdx}, 학생 IDX: ${testData.studentIdx}`);
    
    const result = await apiGet(`/enrollments/${testData.lecIdx}/${testData.studentIdx}/grade`);
    
    if (result && result.success && result.data) {
        console.log('\n📊 성적 상세 정보:');
        const data = result.data;
        
        if (data.attendanceScore !== undefined) {
            console.log('  📅 출석:');
            console.log('    - 출석 수:', data.presentCount || 0);
            console.log('    - 지각 수:', data.lateCount || 0);
            console.log('    - 결석 수:', data.absentCount || 0);
            console.log('    - 출석율:', (data.attendanceRate || 0).toFixed(2) + '%');
            console.log('    - 출석 점수:', data.attendanceScore.toFixed(2));
            console.log('    - 출석 백분율:', data.attendancePercentage.toFixed(2) + '%');
            if (data.latePenalty) {
                console.log('    - 지각 감점:', data.latePenalty.toFixed(2) + '점');
            }
        }
        
        if (data.assignmentScores && data.assignmentScores.length > 0) {
            console.log('  📝 과제:', data.assignmentScores.length + '개');
            let totalAssignment = 0;
            data.assignmentScores.forEach((assignment, idx) => {
                console.log(`    ${idx + 1}. ${assignment.name}: ${assignment.score}/${assignment.maxScore} (${assignment.percentage.toFixed(2)}%)`);
                totalAssignment += assignment.score;
            });
            console.log('    - 과제 총점:', totalAssignment.toFixed(2));
        }
        
        if (data.totalScore !== undefined) {
            console.log('  💯 총점:');
            console.log('    - 점수:', data.totalScore.toFixed(2));
            console.log('    - 백분율:', data.percentage.toFixed(2) + '%');
        }
        
        if (data.grade) {
            console.log('  🏆 등급:', data.grade);
        }
        
        console.log('\n✅ 학생 성적 조회 성공');
    } else {
        console.log('\n❌ 학생 성적 조회 실패');
        console.log('오류:', result.error);
    }
    
    console.log('━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n');
    return result;
}

// ============================================
// 3. 교수용 성적 조회 테스트 (통계 포함)
// GET /api/enrollments/professor/grade
// ============================================
async function testProfessorGradeView() {
    console.log('━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━');
    console.log('👨‍🏫 교수용 성적 조회 테스트');
    console.log('━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━');
    
    console.log(`📤 강의 IDX: ${testData.lecIdx}, 학생 IDX: ${testData.studentIdx}`);
    
    const result = await apiGet(`/enrollments/professor/grade?lecIdx=${testData.lecIdx}&studentIdx=${testData.studentIdx}`);
    
    if (result && result.success && result.data) {
        console.log('\n📊 교수용 성적 정보:');
        const data = result.data;
        
        if (data.studentName) {
            console.log('  👤 학생:', data.studentName);
        }
        
        if (data.attendanceScore !== undefined) {
            console.log('  📅 출석:');
            console.log('    - 출석 수:', data.presentCount || 0);
            console.log('    - 지각 수:', data.lateCount || 0);
            console.log('    - 결석 수:', data.absentCount || 0);
            console.log('    - 출석율:', (data.attendanceRate || 0).toFixed(2) + '%');
            console.log('    - 출석 점수:', data.attendanceScore.toFixed(2));
            console.log('    - 출석 백분율:', data.attendancePercentage.toFixed(2) + '%');
            if (data.latePenalty) {
                console.log('    - 지각 감점:', data.latePenalty.toFixed(2) + '점');
            }
        }
        
        if (data.assignmentScores && data.assignmentScores.length > 0) {
            console.log('  📝 과제:', data.assignmentScores.length + '개');
            data.assignmentScores.forEach((assignment, idx) => {
                console.log(`    ${idx + 1}. ${assignment.name}: ${assignment.score}/${assignment.maxScore} (${assignment.percentage.toFixed(2)}%)`);
            });
        }
        
        if (data.totalScore !== undefined) {
            console.log('  💯 총점:', data.totalScore.toFixed(2), `(${data.percentage.toFixed(2)}%)`);
        }
        
        if (data.grade) {
            console.log('  🏆 등급:', data.grade);
        }
        
        if (data.classStats) {
            console.log('\n  📈 반 통계:');
            console.log('    - 평균:', data.classStats.average?.toFixed(2));
            console.log('    - 최고점:', data.classStats.max?.toFixed(2));
            console.log('    - 최저점:', data.classStats.min?.toFixed(2));
            console.log('    - 학생 순위:', data.classStats.rank);
        }
        
        console.log('\n✅ 교수용 성적 조회 성공');
    } else {
        console.log('\n❌ 교수용 성적 조회 실패');
        console.log('오류:', result.error);
    }
    
    console.log('━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n');
    return result;
}

// ============================================
// 4. 전체 성적 목록 조회 테스트
// GET /api/enrollments/grade-list
// ============================================
async function testGradeList() {
    console.log('━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━');
    console.log('📋 성적 목록 조회 테스트');
    console.log('━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━');
    
    const params = new URLSearchParams({
        lecIdx: testData.lecIdx,
        page: 0,
        size: 20,
        sortBy: 'percentage',
        sortOrder: 'desc'
    });
    
    console.log(`📤 강의 IDX: ${testData.lecIdx}, 정렬: percentage (내림차순)`);
    
    const result = await apiGet(`/enrollments/grade-list?${params.toString()}`);
    
    if (result && result.success && result.data) {
        const data = result.data;
        
        console.log('\n📊 조회 결과:');
        console.log('  - 총 학생 수:', data.totalElements || (data.content ? data.content.length : 0));
        console.log('  - 현재 페이지:', data.number || 0);
        console.log('  - 페이지 크기:', data.size || 20);
        console.log('  - 전체 페이지:', data.totalPages || 1);
        
        if (data.content && data.content.length > 0) {
            console.log('\n  📊 상위 5명:');
            data.content.slice(0, 5).forEach((student, idx) => {
                console.log(`    ${idx + 1}. ${student.studentName || 'N/A'} - ${student.percentage?.toFixed(2) || '0.00'}% (${student.grade || 'N/A'})`);
            });
        }
        
        console.log('\n✅ 성적 목록 조회 성공');
    } else {
        console.log('\n❌ 성적 목록 조회 실패');
        console.log('오류:', result.error);
    }
    
    console.log('━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n');
    return result;
}

// ============================================
// 5. 최종 등급 배정 테스트
// POST /api/enrollments/finalize-grades
// ============================================
async function testGradeFinalize() {
    console.log('━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━');
    console.log('🏆 최종 등급 배정 테스트');
    console.log('━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━');
    
    const requestData = {
        lecIdx: testData.lecIdx,
        passingThreshold: testData.passingThreshold,
        gradeDistribution: testData.gradeDistribution
    };
    
    console.log('📤 요청 데이터:', requestData);
    console.log('   - 합격 기준:', requestData.passingThreshold + '%');
    console.log('   - 등급 분포:', requestData.gradeDistribution);
    
    const result = await apiCall('/enrollments/finalize-grades', requestData);
    
    if (result && result.success && result.data) {
        console.log('\n📊 등급 배정 결과:');
        const data = result.data;
        
        if (data.gradeStats) {
            console.log('  📈 등급별 학생 수:');
            Object.entries(data.gradeStats).forEach(([grade, count]) => {
                console.log(`    ${grade}: ${count}명`);
            });
        }
        
        if (data.totalStudents !== undefined) {
            console.log(`\n  👥 총 학생: ${data.totalStudents}명`);
        }
        if (data.passingStudents !== undefined) {
            console.log(`  ✅ 합격자: ${data.passingStudents}명`);
        }
        if (data.failingStudents !== undefined) {
            console.log(`  ❌ 낙제자: ${data.failingStudents}명`);
        }
        if (data.averageScore !== undefined) {
            console.log(`  📊 평균: ${data.averageScore.toFixed(2)}%`);
        }
        
        console.log('\n✅ 최종 등급 배정 성공');
    } else {
        console.log('\n❌ 최종 등급 배정 실패');
        console.log('오류:', result.error);
    }
    
    console.log('━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n');
    return result;
}

// ============================================
// Phase 1 전체 테스트 실행
// ============================================
async function runPhase1Tests() {
    console.log('🚀 Phase 1: 핵심 성적 관리 기능 테스트 시작');
    console.log('━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n');
    
    const results = {
        total: 5,
        success: 0,
        failed: 0,
        tests: []
    };
    
    try {
        const r1 = await testGradeConfig();
        results.tests.push({ name: '성적 구성 설정', success: r1?.success || false });
        if (r1?.success) results.success++; else results.failed++;
        
        const r2 = await testStudentGradeInfo();
        results.tests.push({ name: '학생 성적 조회', success: r2?.success || false });
        if (r2?.success) results.success++; else results.failed++;
        
        const r3 = await testProfessorGradeView();
        results.tests.push({ name: '교수용 성적 조회', success: r3?.success || false });
        if (r3?.success) results.success++; else results.failed++;
        
        const r4 = await testGradeList();
        results.tests.push({ name: '성적 목록 조회', success: r4?.success || false });
        if (r4?.success) results.success++; else results.failed++;
        
        const r5 = await testGradeFinalize();
        results.tests.push({ name: '최종 등급 배정', success: r5?.success || false });
        if (r5?.success) results.success++; else results.failed++;
        
        console.log('━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━');
        console.log('📊 Phase 1 테스트 결과');
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
        
        return results;
        
    } catch (error) {
        console.error('❌ 테스트 실행 중 예외 발생:', error);
        return results;
    }
}

// ============================================
// 전역으로 노출
// ============================================
window.gradePhase1Tests = {
    // 개별 테스트
    config: testGradeConfig,
    studentInfo: testStudentGradeInfo,
    professorView: testProfessorGradeView,
    gradeList: testGradeList,
    finalize: testGradeFinalize,
    
    // 전체 실행
    runAll: runPhase1Tests
};

console.log('✅ Phase 1 테스트 로드 완료 (grade-phase1-tests.js)');
