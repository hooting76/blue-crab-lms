/**
 * Phase 1: 핵심 성적 관리 기능 테스트 (완전 독립 버전)
 * 
 * 🎯 완전 독립 실행 가능 - 다른 파일 필요 없음!
 * 🔐 lecSerial + JWT 토큰 인증 방식
 * 
 * ============================================
 * 🚀 사용법
 * ============================================
 * 
 * 1단계: 교수 계정으로 로그인
 *    await login()
 * 
 * 2단계: 이 파일 전체 복사 → 브라우저 콘솔 붙여넣기
 * 
 * 3단계: 강의 코드 설정 (선택)
 *    gradePhase1.setLecture('CS101-2024-1')
 * 
 * 4단계: 테스트 실행
 *    await gradePhase1.runAll()
 * 
 * ============================================
 * 💡 개별 테스트
 * ============================================
 * 
 *    await gradePhase1.config()        - 성적 구성 설정
 *    await gradePhase1.studentInfo()   - 학생 성적 조회
 *    await gradePhase1.professorView() - 교수용 조회
 *    await gradePhase1.gradeList()     - 성적 목록
 *    await gradePhase1.finalize()      - 최종 등급 배정
 */

(function() {
    'use strict';
    
    // ============================================
    // 기본 설정
    // ============================================
    const API_BASE = 'https://bluecrab.chickenkiller.com/BlueCrab-1.0.0/api';
    
    // 테스트 설정 (lecSerial 방식)
    const config = {
        lecSerial: null,  // 프롬프트 또는 setLecture()로 설정
        studentEmail: 'student@univ.edu',  // 기본값
        passingThreshold: 60.0,
        attendanceMaxScore: 80,
        assignmentTotalMaxScore: 100,
        latePenaltyPerSession: 0.5,
        gradeDistribution: {
            "A+": 10, "A": 15, "B+": 20,
            "B": 25, "C": 20, "D": 10
        }
    };
    
    // ============================================
    // 유틸리티 함수
    // ============================================
    
    function getToken() {
        return window.authToken || localStorage.getItem('jwtAccessToken');
    }
    
    async function apiCall(endpoint, data = null, method = 'POST') {
        const token = getToken();
        if (!token) {
            console.error('❌ 로그인 필요! await login() 실행하세요.');
            return { success: false, error: '인증 토큰 없음' };
        }
        
        const start = performance.now();
        try {
            const response = await fetch(`${API_BASE}${endpoint}`, {
                method,
                headers: {
                    'Content-Type': 'application/json',
                    'Authorization': `Bearer ${token}`
                },
                body: method !== 'GET' ? JSON.stringify(data) : undefined
            });
            
            const duration = (performance.now() - start).toFixed(2);
            
            if (!response.ok) {
                const error = await response.json().catch(() => ({ message: response.statusText }));
                console.error(`❌ HTTP ${response.status}: ${error.message || response.statusText}`);
                return { success: false, error: error.message || error, status: response.status, duration };
            }
            
            const result = await response.json();
            console.log(`✅ ${method} ${endpoint} (${duration}ms)`);
            return { success: true, data: result, duration };
            
        } catch (error) {
            const duration = (performance.now() - start).toFixed(2);
            console.error(`🔥 예외:`, error.message);
            return { success: false, error: error.message, duration };
        }
    }
    
    // ============================================
    // 설정 함수
    // ============================================
    
    function setLecture(lecSerial, studentEmail = null) {
        config.lecSerial = lecSerial;
        if (studentEmail) config.studentEmail = studentEmail;
        console.log('✅ 설정 완료:', { lecSerial: config.lecSerial, studentEmail: config.studentEmail });
        return config;
    }
    
    function promptLecture() {
        const lecSerial = prompt('강의 코드 (예: CS101-2024-1):', config.lecSerial || '');
        const studentEmail = prompt('학생 이메일:', config.studentEmail);
        
        if (lecSerial) config.lecSerial = lecSerial;
        if (studentEmail) config.studentEmail = studentEmail;
        
        console.log('✅ 설정:', config);
        return config;
    }
    
    // ============================================
    // 1. 성적 구성 설정
    // POST /lectures/{lecSerial}/grade-config
    // ============================================
    
    async function testGradeConfig() {
        console.log('\n━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━');
        console.log('⚙️  성적 구성 설정');
        console.log('━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━');
        
        if (!config.lecSerial) {
            console.warn('⚠️  강의 코드 미설정! promptLecture() 실행...');
            promptLecture();
        }
        
        const data = {
            attendanceMaxScore: config.attendanceMaxScore,
            assignmentTotalMaxScore: config.assignmentTotalMaxScore,
            latePenaltyPerSession: config.latePenaltyPerSession,
            gradeDistribution: config.gradeDistribution
        };
        
        console.log(`📤 강의: ${config.lecSerial}`);
        console.log(`   출석: ${data.attendanceMaxScore}점`);
        console.log(`   과제: ${data.assignmentTotalMaxScore}점`);
        console.log(`   지각 페널티: ${data.latePenaltyPerSession}점/회`);
        
        const result = await apiCall(`/lectures/${config.lecSerial}/grade-config`, data);
        
        if (result?.success) {
            console.log('\n✅ 성공!');
            if (result.data) console.log('📊 결과:', result.data);
        } else {
            console.log('\n❌ 실패:', result.error);
        }
        
        console.log('━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n');
        return result;
    }
    
    // ============================================
    // 2. 학생 성적 조회
    // GET /lectures/{lecSerial}/students/{email}/grade
    // ============================================
    
    async function testStudentGradeInfo() {
        console.log('\n━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━');
        console.log('📊 학생 성적 조회');
        console.log('━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━');
        
        if (!config.lecSerial) {
            promptLecture();
        }
        
        console.log(`📤 강의: ${config.lecSerial}`);
        console.log(`   학생: ${config.studentEmail}`);
        
        const email = encodeURIComponent(config.studentEmail);
        const result = await apiCall(`/lectures/${config.lecSerial}/students/${email}/grade`, null, 'GET');
        
        if (result?.success && result.data) {
            const d = result.data;
            console.log('\n📊 성적 정보:');
            
            if (d.attendanceScore !== undefined) {
                console.log('  📅 출석:');
                console.log(`    - 출석: ${d.presentCount || 0}회`);
                console.log(`    - 지각: ${d.lateCount || 0}회`);
                console.log(`    - 결석: ${d.absentCount || 0}회`);
                console.log(`    - 출석율: ${(d.attendanceRate || 0).toFixed(2)}%`);
                console.log(`    - 점수: ${d.attendanceScore.toFixed(2)}`);
                if (d.latePenalty) console.log(`    - 지각 감점: ${d.latePenalty.toFixed(2)}`);
            }
            
            if (d.assignmentScores?.length) {
                console.log(`  📝 과제: ${d.assignmentScores.length}개`);
                d.assignmentScores.forEach((a, i) => {
                    console.log(`    ${i+1}. ${a.name}: ${a.score}/${a.maxScore} (${a.percentage.toFixed(2)}%)`);
                });
            }
            
            if (d.totalScore !== undefined) {
                console.log(`  💯 총점: ${d.totalScore.toFixed(2)} (${d.percentage.toFixed(2)}%)`);
            }
            
            if (d.grade) console.log(`  🏆 등급: ${d.grade}`);
            
            console.log('\n✅ 성공!');
        } else {
            console.log('\n❌ 실패:', result.error);
        }
        
        console.log('━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n');
        return result;
    }
    
    // ============================================
    // 3. 교수용 성적 조회
    // GET /lectures/{lecSerial}/professor/grade
    // ============================================
    
    async function testProfessorGradeView() {
        console.log('\n━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━');
        console.log('👨‍🏫 교수용 성적 조회');
        console.log('━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━');
        
        if (!config.lecSerial) promptLecture();
        
        const email = encodeURIComponent(config.studentEmail);
        const result = await apiCall(`/lectures/${config.lecSerial}/professor/grade?studentEmail=${email}`, null, 'GET');
        
        if (result?.success && result.data) {
            const d = result.data;
            console.log('\n📊 교수용 정보:');
            
            if (d.studentName) console.log(`  👤 학생: ${d.studentName}`);
            if (d.totalScore !== undefined) {
                console.log(`  💯 총점: ${d.totalScore.toFixed(2)} (${d.percentage.toFixed(2)}%)`);
            }
            if (d.grade) console.log(`  🏆 등급: ${d.grade}`);
            
            if (d.classStats) {
                console.log(`\n  📈 반 통계:`);
                console.log(`    - 평균: ${d.classStats.average?.toFixed(2)}`);
                console.log(`    - 최고: ${d.classStats.max?.toFixed(2)}`);
                console.log(`    - 최저: ${d.classStats.min?.toFixed(2)}`);
                console.log(`    - 순위: ${d.classStats.rank}`);
            }
            
            console.log('\n✅ 성공!');
        } else {
            console.log('\n❌ 실패:', result.error);
        }
        
        console.log('━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n');
        return result;
    }
    
    // ============================================
    // 4. 성적 목록 조회
    // GET /lectures/{lecSerial}/grade-list
    // ============================================
    
    async function testGradeList() {
        console.log('\n━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━');
        console.log('📋 성적 목록 조회');
        console.log('━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━');
        
        if (!config.lecSerial) promptLecture();
        
        const params = new URLSearchParams({
            page: 0,
            size: 20,
            sortBy: 'percentage',
            sortOrder: 'desc'
        });
        
        const result = await apiCall(`/lectures/${config.lecSerial}/grade-list?${params}`, null, 'GET');
        
        if (result?.success && result.data) {
            const d = result.data;
            console.log('\n📊 조회 결과:');
            console.log(`  - 총 학생: ${d.totalElements || d.content?.length || 0}명`);
            console.log(`  - 페이지: ${d.number || 0}/${d.totalPages || 1}`);
            
            if (d.content?.length) {
                console.log('\n  📊 상위 5명:');
                d.content.slice(0, 5).forEach((s, i) => {
                    console.log(`    ${i+1}. ${s.studentName || 'N/A'} - ${s.percentage?.toFixed(2) || '0.00'}% (${s.grade || 'N/A'})`);
                });
            }
            
            console.log('\n✅ 성공!');
        } else {
            console.log('\n❌ 실패:', result.error);
        }
        
        console.log('━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n');
        return result;
    }
    
    // ============================================
    // 5. 최종 등급 배정
    // POST /lectures/{lecSerial}/finalize-grades
    // ============================================
    
    async function testGradeFinalize() {
        console.log('\n━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━');
        console.log('🏆 최종 등급 배정');
        console.log('━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━');
        
        if (!config.lecSerial) promptLecture();
        
        const data = {
            passingThreshold: config.passingThreshold,
            gradeDistribution: config.gradeDistribution
        };
        
        console.log(`📤 합격 기준: ${data.passingThreshold}%`);
        console.log(`   등급 분포:`, data.gradeDistribution);
        
        const result = await apiCall(`/lectures/${config.lecSerial}/finalize-grades`, data);
        
        if (result?.success && result.data) {
            const d = result.data;
            console.log('\n📊 등급 배정 결과:');
            
            if (d.gradeStats) {
                console.log('  📈 등급별 인원:');
                Object.entries(d.gradeStats).forEach(([grade, count]) => {
                    console.log(`    ${grade}: ${count}명`);
                });
            }
            
            if (d.totalStudents !== undefined) console.log(`\n  👥 총 학생: ${d.totalStudents}명`);
            if (d.passingStudents !== undefined) console.log(`  ✅ 합격: ${d.passingStudents}명`);
            if (d.failingStudents !== undefined) console.log(`  ❌ 낙제: ${d.failingStudents}명`);
            if (d.averageScore !== undefined) console.log(`  📊 평균: ${d.averageScore.toFixed(2)}%`);
            
            console.log('\n✅ 성공!');
        } else {
            console.log('\n❌ 실패:', result.error);
        }
        
        console.log('━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n');
        return result;
    }
    
    // ============================================
    // 전체 테스트 실행
    // ============================================
    
    async function runAllTests() {
        console.log('\n🚀 Phase 1 전체 테스트 시작');
        console.log('━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n');
        
        const results = { total: 5, success: 0, failed: 0, tests: [] };
        
        try {
            const tests = [
                { name: '성적 구성 설정', fn: testGradeConfig },
                { name: '학생 성적 조회', fn: testStudentGradeInfo },
                { name: '교수용 조회', fn: testProfessorGradeView },
                { name: '성적 목록', fn: testGradeList },
                { name: '최종 등급 배정', fn: testGradeFinalize }
            ];
            
            for (const test of tests) {
                const r = await test.fn();
                results.tests.push({ name: test.name, success: r?.success || false });
                if (r?.success) results.success++; else results.failed++;
            }
            
            console.log('\n━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━');
            console.log('📊 Phase 1 결과');
            console.log('━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━');
            console.log(`총: ${results.total}개`);
            console.log(`✅ 성공: ${results.success}개`);
            console.log(`❌ 실패: ${results.failed}개`);
            console.log(`📈 성공률: ${((results.success / results.total) * 100).toFixed(1)}%`);
            console.log('\n상세:');
            results.tests.forEach((t, i) => {
                console.log(`  ${t.success ? '✅' : '❌'} ${i+1}. ${t.name}`);
            });
            console.log('━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n');
            
        } catch (error) {
            console.error('❌ 실행 중 예외:', error);
        }
        
        return results;
    }
    
    // ============================================
    // 전역 노출
    // ============================================
    
    window.gradePhase1 = {
        // 설정
        setLecture,
        promptLecture,
        getConfig: () => config,
        
        // 개별 테스트
        config: testGradeConfig,
        studentInfo: testStudentGradeInfo,
        professorView: testProfessorGradeView,
        gradeList: testGradeList,
        finalize: testGradeFinalize,
        
        // 전체 실행
        runAll: runAllTests
    };
    
    console.log('✅ Phase 1 테스트 로드 완료 (독립 버전)');
    console.log('━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━');
    console.log('🎯 완전 독립 실행 - 다른 파일 불필요!');
    console.log('');
    console.log('📝 시작하기:');
    console.log('   1. gradePhase1.promptLecture()  - 강의 설정');
    console.log('   2. await gradePhase1.runAll()   - 전체 실행');
    console.log('');
    console.log('💡 또는:');
    console.log('   gradePhase1.setLecture("CS101-2024-1")');
    console.log('   await gradePhase1.config()');
    console.log('━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━');
    
})();
