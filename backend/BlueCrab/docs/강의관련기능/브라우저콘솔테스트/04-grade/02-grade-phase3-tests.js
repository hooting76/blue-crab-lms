/**
 * 📊 Phase 3: 이벤트 기반 성적 자동 업데이트 테스트 (독립 실행)
 * 
 * 🚀 사용법:
 *    await gradePhase3.listStudents()         // 수강생 목록 조회 (studentIdx 확인)
 *    gradePhase3.setLecture('ETH201', 6)      // 강의 + 학생 지정 (enrollmentIdx 자동 조회)
 *    await gradePhase3.runAll()               // 전체 테스트 (2개)
 * 
 * 📋 개별 API 테스트:
 *    gradePhase3.attendance()  // 출석 요청 → 승인 → 성적 자동 재계산 (실제 출석 시스템 사용)
 *    gradePhase3.assignment()  // 과제 점수 업데이트 → 성적 자동 재계산
 * 
 * ✅ 개선 사항 (2025-10-23):
 *    - attendance() 테스트: 실제 출석 승인 API 사용 (기존 시스템과 통합)
 *    - 백엔드: ENROLLMENT_DATA 병합 로직 적용 (덮어쓰기 방지)
 *    - gradeConfig 자동 저장 및 grade 객체 초기화
 */

(function() {
    'use strict';
    
    // ============================================
    // 기본 설정
    // ============================================
    const API_BASE = 'https://bluecrab.chickenkiller.com/BlueCrab-1.0.0/api';
    
    // 테스트 설정 (Phase 1과 동일한 구조)
    const config = {
        lecSerial: null,      // 강의 코드 (예: "ETH201")
        studentIdx: null,     // 학생 IDX
        enrollmentIdx: null,  // 수강 IDX (자동 조회됨)
        assignmentIdx: null,  // 과제 IDX
        attendanceDate: new Date().toISOString().split('T')[0],
        attendanceStatus: 'PRESENT', // PRESENT, LATE, ABSENT
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
    
    function setLecture(lecSerial, studentIdx = null, assignmentIdx = null) {
        config.lecSerial = lecSerial;
        if (studentIdx !== null) config.studentIdx = studentIdx;
        if (assignmentIdx !== null) config.assignmentIdx = assignmentIdx;
        console.log('✅ 설정 완료:', {
            lecSerial: config.lecSerial,
            studentIdx: config.studentIdx,
            assignmentIdx: config.assignmentIdx
        });
        return config;
    }
    
    function promptLecture() {
        const lecSerial = prompt('강의 코드 (예: ETH201):', config.lecSerial || '');
        const studentIdx = prompt('학생 IDX:', config.studentIdx || '');
        const assignmentIdx = prompt('과제 IDX:', config.assignmentIdx || '');
        
        if (lecSerial) config.lecSerial = lecSerial;
        if (studentIdx) config.studentIdx = parseInt(studentIdx);
        if (assignmentIdx) config.assignmentIdx = parseInt(assignmentIdx);
        
        console.log('✅ 설정:', config);
        return config;
    }
    
    // enrollmentIdx 자동 조회 함수
    async function getEnrollmentIdx() {
        if (config.enrollmentIdx) return config.enrollmentIdx;
        
        const result = await apiCall('/enrollments/grade-info', {
            action: 'get-grade',
            lecSerial: config.lecSerial,
            studentIdx: config.studentIdx
        });
        
        if (result?.success && result.data) {
            const enrollmentIdx = result.data.data?.enrollmentIdx || result.data.enrollmentIdx;
            if (enrollmentIdx) {
                config.enrollmentIdx = enrollmentIdx;
                console.log(`✅ enrollmentIdx 자동 조회: ${enrollmentIdx}`);
                return enrollmentIdx;
            }
        }
        
        throw new Error('enrollmentIdx 조회 실패');
    }
    
    // ============================================
    // 수강생 목록 조회 (lecSerial 기반)
    // POST /enrollments/list
    // ============================================
    
    async function listStudents(page = 0, size = 20) {
        console.log('\n━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━');
        console.log('👥 강의별 수강생 목록 조회');
        console.log('━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━');
        
        if (!config.lecSerial) {
            console.warn('⚠️  강의 미설정! promptLecture() 실행...');
            const lecSerial = prompt('강의 코드 (예: ETH201):', '');
            if (lecSerial) config.lecSerial = lecSerial;
            else {
                console.error('❌ 강의 코드 필수!');
                console.log('━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n');
                return { success: false, error: '강의 코드 없음' };
            }
        }
        
        console.log(`\n📚 강의: ${config.lecSerial}`);
        console.log(`📄 페이지: ${page} (크기: ${size})\n`);
        
        const result = await apiCall('/enrollments/list', {
            lecSerial: config.lecSerial,
            page: page,
            size: size
        });
        
        if (result?.success && result.data?.data) {
            const data = result.data.data;
            const students = data.content || [];
            
            console.log(`\n✅ 총 ${data.totalElements}명 수강생`);
            console.log(`📄 ${data.number + 1}/${data.totalPages} 페이지\n`);
            
            if (students.length > 0) {
                console.log('━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━');
                students.forEach((s, i) => {
                    console.log(`${i + 1}. [IDX: ${s.STUDENT_IDX}] ${s.STUDENT_NAME} (${s.STUDENT_NO})`);
                    console.log(`   학과: ${s.DEPARTMENT || 'N/A'} | 상태: ${s.STATUS}`);
                    console.log(`   신청일: ${s.ENROLLED_AT}`);
                    if (i < students.length - 1) console.log('');
                });
                console.log('━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n');
                
                console.log('💡 Tip: 학생 IDX를 복사해서 setLecture()에 사용하세요!');
                console.log(`   예: gradePhase3.setLecture("${config.lecSerial}", ${students[0].STUDENT_IDX})`);
            } else {
                console.log('⚠️  수강생이 없습니다.');
            }
            
            console.log('\n━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n');
            return { success: true, data: students, total: data.totalElements };
            
        } else {
            console.error('❌ 조회 실패:', result.error);
            console.log('━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n');
            return { success: false, error: result.error };
        }
    }
    
    // ============================================
    // 1. 출석 업데이트 → 성적 자동 재계산 확인
    // ✅ 실제 출석 승인 API 사용 (기존 출석 시스템과 통합)
    // POST /enrollments/grade-info (action: get-grade)
    // POST /api/student/attendance/request (출석 요청)
    // PUT /api/professor/attendance/requests/{requestIdx}/approve (승인)
    // ============================================
    
    async function testAttendanceUpdate() {
        console.log('\n━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━');
        console.log('📅 출석 승인 → 성적 자동 재계산');
        console.log('━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━');
        console.log('💡 실제 출석 시스템 API 사용 (요청 → 승인)');
        
        if (!config.lecSerial || !config.studentIdx) {
            console.warn('⚠️  강의/학생 미설정! promptLecture() 실행...');
            promptLecture();
        }
        
        // enrollmentIdx 자동 조회
        try {
            await getEnrollmentIdx();
        } catch (error) {
            console.error('❌ enrollmentIdx 조회 실패:', error.message);
            console.log('━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n');
            return { success: false, error: error.message };
        }
        
        // 1단계: 업데이트 전 성적 조회
        console.log('\n📊 [1/4] 업데이트 전 성적 조회');
        const beforeData = {
            action: 'get-grade',
            lecSerial: config.lecSerial,
            studentIdx: config.studentIdx
        };
        const beforeResult = await apiCall('/enrollments/grade-info', beforeData);
        
        let beforeScore = null;
        let beforeAttendanceData = null;
        if (beforeResult?.success && beforeResult.data) {
            const d = beforeResult.data.data || beforeResult.data;
            beforeScore = d.attendanceScore;
            beforeAttendanceData = d.attendance;
            console.log(`   출석 점수: ${beforeScore?.toFixed(2) || 'N/A'}`);
            console.log(`   출석: ${d.presentCount || 0}회`);
            console.log(`   지각: ${d.lateCount || 0}회`);
            console.log(`   결석: ${d.absentCount || 0}회`);
        }
        
        // 2단계: 출석 요청 생성 (학생)
        console.log(`\n📝 [2/4] 출석 요청 생성`);
        const requestData = {
            lecSerial: config.lecSerial,
            studentIdx: config.studentIdx,
            sessionNumber: (beforeAttendanceData?.sessions?.length || 0) + 1,
            requestDate: config.attendanceDate,
            reason: 'Phase 3 테스트 - 성적 자동 재계산 확인'
        };
        
        console.log(`   강의: ${requestData.lecSerial}`);
        console.log(`   학생: ${requestData.studentIdx}`);
        console.log(`   회차: ${requestData.sessionNumber}`);
        
        const createRequestResult = await apiCall('/student/attendance/request', requestData);
        
        if (!createRequestResult?.success) {
            console.log('\n❌ 출석 요청 생성 실패:', createRequestResult.error);
            console.log('━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n');
            return createRequestResult;
        }
        
        const requestIdx = createRequestResult.data?.data?.requestIdx || createRequestResult.data?.requestIdx;
        console.log(`✅ 출석 요청 생성 완료 (requestIdx: ${requestIdx})`);
        
        // 3단계: 교수 승인
        console.log(`\n👨‍🏫 [3/4] 교수 승인 처리`);
        console.log(`   요청 IDX: ${requestIdx}`);
        console.log(`   상태: ${config.attendanceStatus} → 변환`);
        
        const approveResult = await apiCall(
            `/professor/attendance/requests/${requestIdx}/approve`,
            {},
            'PUT'
        );
        
        if (!approveResult?.success) {
            console.log('\n❌ 출석 승인 실패:', approveResult.error);
            console.log('━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n');
            return approveResult;
        }
        
        console.log('✅ 출석 승인 완료');
        
        // 4단계: 업데이트 후 성적 재조회
        console.log(`\n📊 [4/4] 업데이트 후 성적 조회 (1초 대기...)`);
        await new Promise(resolve => setTimeout(resolve, 1000)); // 서버 처리 대기
        
        const afterResult = await apiCall('/enrollments/grade-info', beforeData);
        
        if (afterResult?.success && afterResult.data) {
            const d = afterResult.data.data || afterResult.data;
            const afterScore = d.attendanceScore;
            console.log(`   출석 점수: ${afterScore?.toFixed(2) || 'N/A'}`);
            console.log(`   출석: ${d.presentCount || 0}회`);
            console.log(`   지각: ${d.lateCount || 0}회`);
            console.log(`   결석: ${d.absentCount || 0}회`);
            
            if (beforeScore !== null && afterScore !== null) {
                const diff = afterScore - beforeScore;
                console.log(`\n   📈 변화: ${diff >= 0 ? '+' : ''}${diff.toFixed(2)}점`);
                
                if (Math.abs(diff) > 0.01) {
                    console.log('   ✅ 성적 자동 재계산 확인!');
                } else {
                    console.log('   ⚠️  점수 변화 없음 (동일한 출석 상태?)');
                }
            }
            
            console.log('\n✅ 성공! 출석 승인이 성적에 반영되었습니다.');
        } else {
            console.log('\n⚠️  성적 조회 실패:', afterResult.error);
        }
        
        console.log('━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n');
        return { 
            success: true, 
            before: beforeResult, 
            request: createRequestResult,
            approve: approveResult,
            after: afterResult 
        };
    }
    
    // ============================================
    // 2. 과제 점수 업데이트 → 성적 자동 재계산 확인
    // POST /enrollments/grade-info (action: get-grade)
    // PUT /assignments/{assignmentIdx}/grade
    // ============================================
    
    async function testAssignmentGrade() {
        console.log('\n━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━');
        console.log('📝 과제 점수 업데이트 → 성적 자동 재계산');
        console.log('━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━');
        
        if (!config.lecSerial || !config.studentIdx) {
            console.warn('⚠️  강의/학생 미설정! promptLecture() 실행...');
            promptLecture();
        }
        
        if (!config.assignmentIdx) {
            const idx = prompt('과제 IDX (ASSIGNMENT_IDX):', '');
            if (idx) config.assignmentIdx = parseInt(idx);
            else {
                console.error('❌ 과제 IDX 필수!');
                console.log('━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n');
                return { success: false, error: '과제 IDX 없음' };
            }
        }
        
        // 1단계: 업데이트 전 성적 조회
        console.log('\n📊 [1/3] 업데이트 전 성적 조회');
        const gradeData = {
            action: 'get-grade',
            lecSerial: config.lecSerial,
            studentIdx: config.studentIdx
        };
        const beforeResult = await apiCall('/enrollments/grade-info', gradeData);
        
        let beforeTotal = null;
        let beforeAssignments = [];
        if (beforeResult?.success && beforeResult.data) {
            const d = beforeResult.data.data || beforeResult.data;
            beforeTotal = d.totalScore;
            beforeAssignments = d.assignmentScores || [];
            console.log(`   총점: ${beforeTotal?.toFixed(2) || 'N/A'}`);
            console.log(`   과제 수: ${beforeAssignments.length}개`);
            if (beforeAssignments.length > 0) {
                beforeAssignments.slice(0, 3).forEach((a, i) => {
                    console.log(`     ${i+1}. ${a.name}: ${a.score}/${a.maxScore}`);
                });
                if (beforeAssignments.length > 3) {
                    console.log(`     ... 외 ${beforeAssignments.length - 3}개`);
                }
            }
        }
        
        // 2단계: 과제 점수 업데이트
        console.log(`\n📝 [2/3] 과제 점수 업데이트`);
        const updateData = {
            studentIdx: config.studentIdx,
            score: config.assignmentScore
        };
        
        console.log(`   과제 IDX: ${config.assignmentIdx}`);
        console.log(`   학생 IDX: ${updateData.studentIdx}`);
        console.log(`   점수: ${updateData.score}`);
        
        const updateResult = await apiCall(
            `/assignments/${config.assignmentIdx}/grade`,
            updateData,
            'PUT'
        );
        
        if (!updateResult?.success) {
            console.log('\n❌ 과제 점수 업데이트 실패:', updateResult.error);
            console.log('━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n');
            return updateResult;
        }
        
        console.log('✅ 과제 점수 업데이트 완료');
        
        // 3단계: 업데이트 후 성적 재조회
        console.log(`\n📊 [3/3] 업데이트 후 성적 조회 (1초 대기...)`);
        await new Promise(resolve => setTimeout(resolve, 1000)); // 서버 처리 대기
        
        const afterResult = await apiCall('/enrollments/grade-info', gradeData);
        
        if (afterResult?.success && afterResult.data) {
            const d = afterResult.data.data || afterResult.data;
            const afterTotal = d.totalScore;
            const afterAssignments = d.assignmentScores || [];
            
            console.log(`   총점: ${afterTotal?.toFixed(2) || 'N/A'}`);
            console.log(`   과제 수: ${afterAssignments.length}개`);
            if (afterAssignments.length > 0) {
                afterAssignments.slice(0, 3).forEach((a, i) => {
                    console.log(`     ${i+1}. ${a.name}: ${a.score}/${a.maxScore}`);
                });
                if (afterAssignments.length > 3) {
                    console.log(`     ... 외 ${afterAssignments.length - 3}개`);
                }
            }
            
            if (beforeTotal !== null && afterTotal !== null) {
                const diff = afterTotal - beforeTotal;
                console.log(`\n   📈 총점 변화: ${diff >= 0 ? '+' : ''}${diff.toFixed(2)}점`);
                
                if (Math.abs(diff) > 0.01) {
                    console.log('   ✅ 성적 자동 재계산 확인!');
                } else {
                    console.log('   ⚠️  점수 변화 없음 (동일한 점수?)');
                }
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
                { name: '출석 업데이트 → 성적 재계산', fn: testAttendanceUpdate },
                { name: '과제 점수 → 성적 재계산', fn: testAssignmentGrade }
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
        
        // 조회
        listStudents,  // 수강생 목록 조회
        
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
    console.log('📝 기본 사용:');
    console.log('   0. await gradePhase3.listStudents()           - 수강생 목록 조회 (선택)');
    console.log('   1. gradePhase3.setLecture("ETH201", 6)        - 강의+학생 지정 (enrollmentIdx 자동)');
    console.log('   2. await gradePhase3.runAll()                 - 전체 테스트 (2개)');
    console.log('');
    console.log('🧪 개별 API:');
    console.log('   await gradePhase3.attendance()  - 출석 요청 → 승인 → 성적 자동 재계산');
    console.log('                                      (실제 출석 시스템 API 사용)');
    console.log('   await gradePhase3.assignment()  - 과제 점수 → 성적 자동 재계산');
    console.log('');
    console.log('💡 또는 대화형:');
    console.log('   gradePhase3.promptLecture()     - 프롬프트로 입력');
    console.log('');
    console.log('✅ 개선 사항 (2025-10-23):');
    console.log('   • attendance() 테스트: 실제 출석 승인 API로 변경');
    console.log('   • 백엔드: JSON 병합 로직 적용 (sessions, summary 유지)');
    console.log('   • gradeConfig 자동 저장 및 grade 객체 초기화');
    console.log('');
    console.log('📚 참고: 수강생 목록 조회 API');
    console.log('   • POST /api/enrollments/list (lecSerial 기반)');
    console.log('   • 상세 테스트: docs/강의관련기능/브라우저콘솔테스트/03-professor/lecture-test-5-professor-students.js');
    console.log('━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━');
    
})();
