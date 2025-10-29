/**
 * 📅 출석 API 테스트 (독립 실행) - 최신 API 엔드포인트
 * 
 * 🚀 사용법:
 *    await attendanceTest.runAll()        // 전체 테스트 (학생+교수 조회)
 * 
 * 📋 개별 API 테스트:
 *    await attendanceTest.myStatus()      // 학생: 내 출석 현황
 *    await attendanceTest.request()       // 학생: 출석 인정 요청
 *    await attendanceTest.profView()      // 교수: 출석 현황 조회
 *    await attendanceTest.approve()       // 교수: 출석 승인/입력
 *    await attendanceTest.mark()          // 교수: 출석 직접 입력 (approve와 동일)
 * 
 * ⚠️ 비활성화 API (백엔드 미구현):
 *    - myRequests()   // 내 요청 목록
 *    - reject()       // 출석 요청 반려
 */

(function() {
    'use strict';
    
    const API_BASE = 'https://bluecrab.chickenkiller.com/BlueCrab-1.0.0/api';
    
    // ============================================
    // 유틸리티 함수
    // ============================================
    
    function getToken() {
        return window.authToken || localStorage.getItem('jwtAccessToken');
    }
    
    async function apiCall(endpoint, data, method = 'POST') {
        const token = getToken();
        if (!token) {
            console.error('❌ 로그인 필요!');
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
                body: JSON.stringify(data)
            });
            
            const duration = (performance.now() - start).toFixed(2);
            
            if (!response.ok) {
                const error = await response.json().catch(() => ({ message: response.statusText }));
                console.error(`❌ HTTP ${response.status}:`, error.message || response.statusText);
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
    // 1. 학생: 내 출석 현황 조회
    // POST /api/attendance/student/view
    // ============================================
    
    async function testMyAttendanceStatus() {
        console.log('\n━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━');
        console.log('📊 내 출석 현황');
        console.log('━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━');
        
        const lecSerial = prompt('강의 코드 (lecSerial):', 'ETH201');
        if (!lecSerial) {
            console.log('❌ lecSerial이 필요합니다.');
            return { success: false, error: 'lecSerial 미입력' };
        }
        
        const data = { lecSerial: lecSerial };
        console.log(`📤 lecSerial: ${lecSerial}`);
        
        const result = await apiCall('/attendance/student/view', data);
        
        if (result?.success && result.data) {
            const attendanceData = result.data.data || result.data;
            const summary = attendanceData.summary || {};
            
            console.log('\n📊 출석 정보:');
            console.log(`  출석 문자열: ${attendanceData.attendanceStr || 'N/A'}`);
            console.log(`  출석: ${summary.attended || 0}회`);
            console.log(`  지각: ${summary.late || 0}회`);
            console.log(`  결석: ${summary.absent || 0}회`);
            console.log(`  출석률: ${summary.attendanceRate || 'N/A'}%`);
            
            if (attendanceData.details && Array.isArray(attendanceData.details)) {
                console.log(`\n📋 상세 (${attendanceData.details.length}회):`);
                attendanceData.details.forEach((att, i) => {
                    console.log(`  ${att.sessionNumber}회차: ${att.status} (${att.date || 'N/A'})`);
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
    // 2. 학생: 출석 인정 요청
    // POST /api/attendance/request
    // ============================================
    
    async function testAttendanceRequest() {
        console.log('\n━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━');
        console.log('📝 출석 인정 요청');
        console.log('━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━');
        
        const lecSerial = prompt('강의 코드:', 'ETH201');
        const sessionNumber = prompt('회차 번호:', '');
        
        if (!lecSerial || !sessionNumber) {
            console.log('❌ 모든 정보가 필요합니다.');
            return { success: false, error: '필수 정보 미입력' };
        }
        
        const data = {
            lecSerial,
            sessionNumber: parseInt(sessionNumber)
        };
        
        console.log(`📤 강의: ${lecSerial}, 회차: ${sessionNumber}`);
        
        const result = await apiCall('/attendance/request', data);
        
        if (result?.success) {
            console.log('\n✅ 출석 인정 요청 제출 완료!');
            if (result.data) console.log('📊 결과:', result.data);
        } else {
            console.log('\n❌ 실패:', result.error);
        }
        
        console.log('━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n');
        return result;
    }
    
    // ============================================
    // 3. 학생: 내 출석 요청 목록 (통합 테스트 코드에는 없음 - 유지)
    // POST /api/student/attendance/requests
    // ============================================
    
    async function testMyRequests() {
        console.log('\n━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━');
        console.log('📋 내 출석 요청 목록');
        console.log('━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━');
        console.log('⚠️  이 API는 현재 백엔드에 구현되지 않았을 수 있습니다.');
        
        const studentIdx = prompt('학생 IDX:', window.currentUser?.id || '');
        const lecSerial = prompt('강의 코드 (선택, 빈칸: 전체):', '');
        
        if (!studentIdx) {
            console.log('❌ 학생 IDX가 필요합니다.');
            return { success: false, error: '학생 IDX 미입력' };
        }
        
        const data = { studentIdx: parseInt(studentIdx) };
        if (lecSerial) data.lecSerial = lecSerial;
        
        console.log(`📤 학생: ${studentIdx}${lecSerial ? `, 강의: ${lecSerial}` : ''}`);
        
        const result = await apiCall('/student/attendance/requests', data);
        
        if (result?.success && result.data) {
            const requests = result.data.data || result.data || [];
            console.log(`\n📊 요청 개수: ${requests.length}건`);
            
            if (requests.length > 0) {
                console.log('\n📋 요청 목록:');
                requests.forEach((req, i) => {
                    console.log(`  ${i+1}. ${req.lecSerial} ${req.sessionNumber}회차`);
                    console.log(`     상태: ${req.status}, 요청일: ${req.requestedAt || 'N/A'}`);
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
    // 4. 교수: 출석 현황 조회 (전체 학생)
    // POST /api/attendance/professor/view
    // ============================================
    
    async function testProfessorRequests() {
        console.log('\n━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━');
        console.log('👨‍🏫 교수: 출석 현황 조회');
        console.log('━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━');
        
        const lecSerial = prompt('강의 코드:', 'ETH201');
        
        if (!lecSerial) {
            console.log('❌ 강의 코드가 필요합니다.');
            return { success: false, error: '강의 코드 미입력' };
        }
        
        const data = { lecSerial: lecSerial };
        
        console.log(`📤 강의: ${lecSerial}`);
        
        const result = await apiCall('/attendance/professor/view', data);
        
        if (result?.success && result.data) {
            const students = result.data.data || [];
            console.log(`\n📊 학생 수: ${students.length}명`);
            
            if (students.length > 0) {
                console.log('\n📋 출석 현황:');
                students.forEach((student, i) => {
                    const summary = student.attendanceData?.summary || {};
                    console.log(`  ${i+1}. ${student.studentName} (${student.studentCode})`);
                    console.log(`     출석: ${summary.attended || 0}, 지각: ${summary.late || 0}, 결석: ${summary.absent || 0}`);
                    console.log(`     출석률: ${summary.attendanceRate || 0}%`);
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
    // 5. 교수: 출석 승인 (요청 기반이 아닌 직접 입력 방식)
    // POST /api/attendance/approve
    // ============================================
    
    async function testApproveRequest() {
        console.log('\n━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━');
        console.log('✅ 출석 승인 (회차별 출석 입력)');
        console.log('━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━');
        
        const lecSerial = prompt('강의 코드:', 'ETH201');
        const sessionNumber = prompt('회차 번호:', '');
        const studentIdx = prompt('학생 IDX:', '');
        const status = prompt('출석 상태 (출/지/결):', '출');
        
        if (!lecSerial || !sessionNumber || !studentIdx || !status) {
            console.log('❌ 모든 정보가 필요합니다.');
            return { success: false, error: '필수 정보 미입력' };
        }
        
        const data = {
            lecSerial: lecSerial,
            sessionNumber: parseInt(sessionNumber),
            attendanceRecords: [
                {
                    studentIdx: parseInt(studentIdx),
                    status: status
                }
            ]
        };
        
        console.log(`📤 강의: ${lecSerial}, 회차: ${sessionNumber}`);
        console.log(`   학생 IDX: ${studentIdx}, 상태: ${status}`);
        
        const result = await apiCall('/attendance/approve', data);
        
        if (result?.success) {
            console.log('\n✅ 출석 승인 완료!');
            if (result.data) console.log('📊 결과:', result.data);
        } else {
            console.log('\n❌ 실패:', result.error);
        }
        
        console.log('━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n');
        return result;
    }
    
    // ============================================
    // 6. 교수: 출석 요청 반려 (현재 API 없음 - 비활성화)
    // ============================================
    
    async function testRejectRequest() {
        console.log('\n━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━');
        console.log('❌ 출석 요청 반려');
        console.log('━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━');
        console.log('⚠️  이 API는 현재 구현되지 않았습니다.');
        console.log('� 대신 출석 승인 API로 상태를 변경하세요.');
        
        console.log('━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n');
        return { success: false, error: 'API 미구현' };
    }
    
    // ============================================
    // 7. 교수: 출석 직접 입력 (approve API와 동일하게 변경)
    // POST /api/attendance/approve
    // ============================================
    
    async function testMarkAttendance() {
        console.log('\n━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━');
        console.log('✏️  출석 직접 입력');
        console.log('━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━');
        
        const lecSerial = prompt('강의 코드:', 'ETH201');
        const sessionNumber = prompt('회차 번호:', '');
        const studentIdx = prompt('학생 IDX:', '');
        const status = prompt('출석 상태 (출/결/지):', '');
        
        if (!lecSerial || !sessionNumber || !studentIdx || !status) {
            console.log('❌ 모든 정보가 필요합니다.');
            return { success: false, error: '필수 정보 미입력' };
        }
        
        const data = {
            lecSerial: lecSerial,
            sessionNumber: parseInt(sessionNumber),
            attendanceRecords: [
                {
                    studentIdx: parseInt(studentIdx),
                    status: status
                }
            ]
        };
        
        console.log(`📤 강의: ${lecSerial}, 회차: ${sessionNumber}`);
        console.log(`   학생 IDX: ${studentIdx}, 상태: ${status}`);
        
        const result = await apiCall('/attendance/approve', data);
        
        if (result?.success) {
            console.log('\n✅ 출석 입력 완료!');
            if (result.data) console.log('📊 결과:', result.data);
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
        console.log('\n🚀 출석 API 전체 테스트 시작');
        console.log('━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n');
        
        const results = { total: 2, success: 0, failed: 0, tests: [] };
        
        try {
            const tests = [
                { name: '내 출석 현황', fn: testMyAttendanceStatus },
                { name: '교수: 출석 현황 조회', fn: testProfessorRequests }
            ];
            
            for (const test of tests) {
                const r = await test.fn();
                results.tests.push({ name: test.name, success: r?.success || false });
                if (r?.success) results.success++; else results.failed++;
            }
            
            console.log('\n━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━');
            console.log('📊 테스트 결과');
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
    
    window.attendanceTest = {
        myStatus: testMyAttendanceStatus,
        request: testAttendanceRequest,
        myRequests: testMyRequests,  // 백엔드 미구현 (경고 표시)
        profView: testProfessorRequests,
        approve: testApproveRequest,
        reject: testRejectRequest,   // 백엔드 미구현 (경고 표시)
        mark: testMarkAttendance,
        runAll: runAllTests
    };
    
    console.log('✅ 출석 API 테스트 로드 완료 (최신 엔드포인트)');
    console.log('💡 사용: await attendanceTest.runAll() 또는 개별 함수 실행');
    console.log('');
    console.log('📋 주요 API:');
    console.log('   - myStatus()  : 학생 출석 현황 조회 (lecSerial 기반)');
    console.log('   - request()   : 출석 인정 요청');
    console.log('   - profView()  : 교수 출석 현황 조회 (전체 학생)');
    console.log('   - approve()   : 출석 승인/입력');
    console.log('━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━');
    
})();
