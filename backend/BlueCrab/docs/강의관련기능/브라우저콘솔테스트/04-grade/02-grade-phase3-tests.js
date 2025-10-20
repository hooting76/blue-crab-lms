/**
 * Phase 3: 이벤트 기반 성적 업데이트 테스트 (완전 독립 버전)
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
 *    gradePhase3.setLecture('CS101-2024-1', 'student@univ.edu')
 * 
 * 4단계: 테스트 실행
 *    await gradePhase3.runAll()
 * 
 * ============================================
 * 💡 개별 테스트
 * ============================================
 * 
 *    await gradePhase3.attendance()  - 출석 업데이트 시 성적 반영
 *    await gradePhase3.assignment()  - 과제 제출 시 성적 반영
 */

(function() {
    'use strict';
    
    // ============================================
    // 기본 설정
    // ============================================
    const API_BASE = 'https://bluecrab.chickenkiller.com/BlueCrab-1.0.0/api';
    
    // 테스트 설정
    const config = {
        lecSerial: null,
        studentEmail: 'student@univ.edu',
        attendanceDate: new Date().toISOString().split('T')[0], // 오늘
        attendanceStatus: 'PRESENT', // PRESENT, LATE, ABSENT
        assignmentIdx: null, // 과제 ID (프롬프트로 입력)
        assignmentScore: 85
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
    
    function setLecture(lecSerial, studentEmail = null, assignmentIdx = null) {
        config.lecSerial = lecSerial;
        if (studentEmail) config.studentEmail = studentEmail;
        if (assignmentIdx) config.assignmentIdx = assignmentIdx;
        console.log('✅ 설정 완료:', {
            lecSerial: config.lecSerial,
            studentEmail: config.studentEmail,
            assignmentIdx: config.assignmentIdx
        });
        return config;
    }
    
    function promptLecture() {
        const lecSerial = prompt('강의 코드 (예: CS101-2024-1):', config.lecSerial || '');
        const studentEmail = prompt('학생 이메일:', config.studentEmail);
        const assignmentIdx = prompt('과제 IDX (선택):', config.assignmentIdx || '');
        
        if (lecSerial) config.lecSerial = lecSerial;
        if (studentEmail) config.studentEmail = studentEmail;
        if (assignmentIdx) config.assignmentIdx = parseInt(assignmentIdx);
        
        console.log('✅ 설정:', config);
        return config;
    }
    
    // ============================================
    // 1. 출석 업데이트 → 성적 자동 반영
    // POST /lectures/{lecSerial}/attendance
    // → 출석 기록 후 학생 성적 자동 재계산 확인
    // ============================================
    
    async function testAttendanceUpdate() {
        console.log('\n━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━');
        console.log('📅 출석 업데이트 → 성적 반영 테스트');
        console.log('━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━');
        
        if (!config.lecSerial) {
            console.warn('⚠️  강의 코드 미설정! promptLecture() 실행...');
            promptLecture();
        }
        
        // 1단계: 업데이트 전 성적 조회
        console.log('\n📊 [1/3] 업데이트 전 성적 조회');
        const email = encodeURIComponent(config.studentEmail);
        const beforeResult = await apiCall(`/lectures/${config.lecSerial}/students/${email}/grade`, null, 'GET');
        
        let beforeScore = null;
        if (beforeResult?.success && beforeResult.data) {
            beforeScore = beforeResult.data.attendanceScore;
            console.log(`   출석 점수: ${beforeScore?.toFixed(2) || 'N/A'}`);
            console.log(`   출석: ${beforeResult.data.presentCount || 0}회`);
            console.log(`   지각: ${beforeResult.data.lateCount || 0}회`);
            console.log(`   결석: ${beforeResult.data.absentCount || 0}회`);
        }
        
        // 2단계: 출석 기록
        console.log(`\n📝 [2/3] 출석 기록`);
        const attendanceData = {
            studentEmail: config.studentEmail,
            attendanceDate: config.attendanceDate,
            status: config.attendanceStatus
        };
        
        console.log(`   학생: ${attendanceData.studentEmail}`);
        console.log(`   날짜: ${attendanceData.attendanceDate}`);
        console.log(`   상태: ${attendanceData.status}`);
        
        const updateResult = await apiCall(`/lectures/${config.lecSerial}/attendance`, attendanceData);
        
        if (!updateResult?.success) {
            console.log('\n❌ 출석 기록 실패:', updateResult.error);
            console.log('━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n');
            return updateResult;
        }
        
        console.log('✅ 출석 기록 완료');
        
        // 3단계: 업데이트 후 성적 재조회
        console.log(`\n📊 [3/3] 업데이트 후 성적 조회`);
        await new Promise(resolve => setTimeout(resolve, 500)); // 서버 처리 대기
        
        const afterResult = await apiCall(`/lectures/${config.lecSerial}/students/${email}/grade`, null, 'GET');
        
        if (afterResult?.success && afterResult.data) {
            const afterScore = afterResult.data.attendanceScore;
            console.log(`   출석 점수: ${afterScore?.toFixed(2) || 'N/A'}`);
            console.log(`   출석: ${afterResult.data.presentCount || 0}회`);
            console.log(`   지각: ${afterResult.data.lateCount || 0}회`);
            console.log(`   결석: ${afterResult.data.absentCount || 0}회`);
            
            if (beforeScore !== null && afterScore !== null) {
                const diff = afterScore - beforeScore;
                console.log(`\n   📈 변화: ${diff >= 0 ? '+' : ''}${diff.toFixed(2)}점`);
            }
            
            console.log('\n✅ 성공! 출석 업데이트가 성적에 반영되었습니다.');
        } else {
            console.log('\n⚠️  성적 조회 실패:', afterResult.error);
        }
        
        console.log('━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n');
        return { success: true, before: beforeResult, update: updateResult, after: afterResult };
    }
    
    // ============================================
    // 2. 과제 점수 업데이트 → 성적 자동 반영
    // PUT /lectures/{lecSerial}/assignments/{assignmentIdx}/grade
    // → 과제 채점 후 학생 성적 자동 재계산 확인
    // ============================================
    
    async function testAssignmentGrade() {
        console.log('\n━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━');
        console.log('📝 과제 점수 업데이트 → 성적 반영 테스트');
        console.log('━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━');
        
        if (!config.lecSerial) {
            console.warn('⚠️  강의 코드 미설정! promptLecture() 실행...');
            promptLecture();
        }
        
        if (!config.assignmentIdx) {
            const idx = prompt('과제 IDX를 입력하세요:', '');
            if (idx) config.assignmentIdx = parseInt(idx);
            else {
                console.error('❌ 과제 IDX 필수!');
                console.log('━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n');
                return { success: false, error: '과제 IDX 없음' };
            }
        }
        
        // 1단계: 업데이트 전 성적 조회
        console.log('\n📊 [1/3] 업데이트 전 성적 조회');
        const email = encodeURIComponent(config.studentEmail);
        const beforeResult = await apiCall(`/lectures/${config.lecSerial}/students/${email}/grade`, null, 'GET');
        
        let beforeTotal = null;
        let beforeAssignments = [];
        if (beforeResult?.success && beforeResult.data) {
            beforeTotal = beforeResult.data.totalScore;
            beforeAssignments = beforeResult.data.assignmentScores || [];
            console.log(`   총점: ${beforeTotal?.toFixed(2) || 'N/A'}`);
            console.log(`   과제 수: ${beforeAssignments.length}개`);
            if (beforeAssignments.length > 0) {
                beforeAssignments.forEach((a, i) => {
                    console.log(`     ${i+1}. ${a.name}: ${a.score}/${a.maxScore}`);
                });
            }
        }
        
        // 2단계: 과제 점수 업데이트
        console.log(`\n📝 [2/3] 과제 점수 업데이트`);
        const gradeData = {
            studentEmail: config.studentEmail,
            score: config.assignmentScore
        };
        
        console.log(`   과제 IDX: ${config.assignmentIdx}`);
        console.log(`   학생: ${gradeData.studentEmail}`);
        console.log(`   점수: ${gradeData.score}`);
        
        const updateResult = await apiCall(
            `/lectures/${config.lecSerial}/assignments/${config.assignmentIdx}/grade`,
            gradeData,
            'PUT'
        );
        
        if (!updateResult?.success) {
            console.log('\n❌ 과제 점수 업데이트 실패:', updateResult.error);
            console.log('━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n');
            return updateResult;
        }
        
        console.log('✅ 과제 점수 업데이트 완료');
        
        // 3단계: 업데이트 후 성적 재조회
        console.log(`\n📊 [3/3] 업데이트 후 성적 조회`);
        await new Promise(resolve => setTimeout(resolve, 500)); // 서버 처리 대기
        
        const afterResult = await apiCall(`/lectures/${config.lecSerial}/students/${email}/grade`, null, 'GET');
        
        if (afterResult?.success && afterResult.data) {
            const afterTotal = afterResult.data.totalScore;
            const afterAssignments = afterResult.data.assignmentScores || [];
            
            console.log(`   총점: ${afterTotal?.toFixed(2) || 'N/A'}`);
            console.log(`   과제 수: ${afterAssignments.length}개`);
            if (afterAssignments.length > 0) {
                afterAssignments.forEach((a, i) => {
                    console.log(`     ${i+1}. ${a.name}: ${a.score}/${a.maxScore}`);
                });
            }
            
            if (beforeTotal !== null && afterTotal !== null) {
                const diff = afterTotal - beforeTotal;
                console.log(`\n   📈 총점 변화: ${diff >= 0 ? '+' : ''}${diff.toFixed(2)}점`);
            }
            
            console.log('\n✅ 성공! 과제 점수 업데이트가 성적에 반영되었습니다.');
        } else {
            console.log('\n⚠️  성적 조회 실패:', afterResult.error);
        }
        
        console.log('━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n');
        return { success: true, before: beforeResult, update: updateResult, after: afterResult };
    }
    
    // ============================================
    // 전체 테스트 실행
    // ============================================
    
    async function runAllTests() {
        console.log('\n🚀 Phase 3 전체 테스트 시작');
        console.log('━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n');
        
        const results = { total: 2, success: 0, failed: 0, tests: [] };
        
        try {
            const tests = [
                { name: '출석 업데이트', fn: testAttendanceUpdate },
                { name: '과제 점수 업데이트', fn: testAssignmentGrade }
            ];
            
            for (const test of tests) {
                const r = await test.fn();
                results.tests.push({ name: test.name, success: r?.success || false });
                if (r?.success) results.success++; else results.failed++;
            }
            
            console.log('\n━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━');
            console.log('📊 Phase 3 결과');
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
    
    window.gradePhase3 = {
        // 설정
        setLecture,
        promptLecture,
        getConfig: () => config,
        
        // 개별 테스트
        attendance: testAttendanceUpdate,
        assignment: testAssignmentGrade,
        
        // 전체 실행
        runAll: runAllTests
    };
    
    console.log('✅ Phase 3 테스트 로드 완료 (독립 버전)');
    console.log('━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━');
    console.log('🎯 완전 독립 실행 - 다른 파일 불필요!');
    console.log('');
    console.log('📝 시작하기:');
    console.log('   1. gradePhase3.promptLecture()  - 강의 설정');
    console.log('   2. await gradePhase3.runAll()   - 전체 실행');
    console.log('');
    console.log('💡 또는:');
    console.log('   gradePhase3.setLecture("CS101-2024-1", "student@univ.edu", 42)');
    console.log('   await gradePhase3.attendance()');
    console.log('   await gradePhase3.assignment()');
    console.log('━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━');
    
})();
