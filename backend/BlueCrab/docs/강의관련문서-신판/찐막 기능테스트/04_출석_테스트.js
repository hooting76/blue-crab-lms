/**
 * 📅 출석 API 테스트 (독립 실행)
 * 
 * 🚀 사용법:
 *    await attendanceTest.runAll()        // 전체 테스트
 * 
 * 📋 개별 API 테스트:
 *    await attendanceTest.myStatus()      // 내 출석 현황
 *    await attendanceTest.request()       // 출석 인정 요청
 *    await attendanceTest.myRequests()    // 내 요청 목록
 *    await attendanceTest.profRequests()  // 교수: 요청 목록
 *    await attendanceTest.approve()       // 교수: 요청 승인
 *    await attendanceTest.reject()        // 교수: 요청 반려
 *    await attendanceTest.mark()          // 교수: 출석 직접 입력
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
    // POST /api/student/attendance/detail
    // ============================================
    
    async function testMyAttendanceStatus() {
        console.log('\n━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━');
        console.log('📊 내 출석 현황');
        console.log('━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━');
        
        const enrollmentIdx = prompt('수강신청 IDX (enrollmentIdx):', '');
        if (!enrollmentIdx) {
            console.log('❌ enrollmentIdx가 필요합니다.');
            return { success: false, error: 'enrollmentIdx 미입력' };
        }
        
        const data = { enrollmentIdx: parseInt(enrollmentIdx) };
        console.log(`📤 enrollmentIdx: ${enrollmentIdx}`);
        
        const result = await apiCall('/student/attendance/detail', data);
        
        if (result?.success && result.data) {
            const d = result.data.data || result.data;
            console.log('\n📊 출석 정보:');
            console.log(`  출석 문자열: ${d.attendanceStr || 'N/A'}`);
            console.log(`  출석률: ${d.attendanceRate || 'N/A'}`);
            
            if (d.attendanceDetails && Array.isArray(d.attendanceDetails)) {
                console.log(`\n📋 상세 (${d.attendanceDetails.length}회):`);
                d.attendanceDetails.forEach((att, i) => {
                    console.log(`  ${att.sessionNumber}회차: ${att.status}`);
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
    // POST /api/student/attendance/request
    // ============================================
    
    async function testAttendanceRequest() {
        console.log('\n━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━');
        console.log('📝 출석 인정 요청');
        console.log('━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━');
        
        const lecSerial = prompt('강의 코드:', '');
        const sessionNumber = prompt('회차 번호:', '');
        const reason = prompt('요청 사유:', '');
        
        if (!lecSerial || !sessionNumber || !reason) {
            console.log('❌ 모든 정보가 필요합니다.');
            return { success: false, error: '필수 정보 미입력' };
        }
        
        const data = {
            lecSerial,
            sessionNumber: parseInt(sessionNumber),
            requestReason: reason
        };
        
        console.log(`📤 강의: ${lecSerial}, 회차: ${sessionNumber}`);
        console.log(`   사유: ${reason}`);
        
        const result = await apiCall('/student/attendance/request', data);
        
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
    // 3. 학생: 내 출석 요청 목록
    // POST /api/student/attendance/requests
    // ============================================
    
    async function testMyRequests() {
        console.log('\n━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━');
        console.log('📋 내 출석 요청 목록');
        console.log('━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━');
        
        const studentIdx = prompt('학생 IDX:', '');
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
    // 4. 교수: 출석 요청 목록 조회
    // POST /api/professor/attendance/requests
    // ============================================
    
    async function testProfessorRequests() {
        console.log('\n━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━');
        console.log('👨‍🏫 교수: 출석 요청 목록');
        console.log('━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━');
        
        const lecIdx = prompt('강의 IDX:', '');
        const status = prompt('상태 필터 (PENDING/APPROVED/REJECTED, 빈칸: 전체):', '');
        const page = prompt('페이지 (기본: 0):', '0');
        const size = prompt('크기 (기본: 20):', '20');
        
        if (!lecIdx) {
            console.log('❌ 강의 IDX가 필요합니다.');
            return { success: false, error: '강의 IDX 미입력' };
        }
        
        const data = {
            lecIdx: parseInt(lecIdx),
            page: parseInt(page) || 0,
            size: parseInt(size) || 20
        };
        if (status) data.status = status;
        
        console.log(`📤 강의: ${lecIdx}${status ? `, 상태: ${status}` : ''}`);
        
        const result = await apiCall('/professor/attendance/requests', data);
        
        if (result?.success && result.data) {
            const d = result.data.data || result.data;
            const requests = d.content || [];
            console.log(`\n📊 요청 개수: ${requests.length}건`);
            console.log(`   페이지: ${d.number || 0}/${d.totalPages || 1}`);
            
            if (requests.length > 0) {
                console.log('\n📋 요청 목록:');
                requests.forEach((req, i) => {
                    console.log(`  ${i+1}. [${req.requestIdx}] ${req.studentName} - ${req.sessionNumber}회차`);
                    console.log(`     상태: ${req.status}, 사유: ${req.requestReason}`);
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
    // 5. 교수: 출석 요청 승인
    // PUT /api/professor/attendance/requests/{requestIdx}/approve
    // ============================================
    
    async function testApproveRequest() {
        console.log('\n━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━');
        console.log('✅ 출석 요청 승인');
        console.log('━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━');
        
        const requestIdx = prompt('요청 IDX:', '');
        const note = prompt('승인 메모 (선택):', '');
        
        if (!requestIdx) {
            console.log('❌ 요청 IDX가 필요합니다.');
            return { success: false, error: '요청 IDX 미입력' };
        }
        
        const data = {};
        if (note) data.approvalNote = note;
        
        console.log(`📤 요청 IDX: ${requestIdx}`);
        
        const result = await apiCall(`/professor/attendance/requests/${requestIdx}/approve`, data, 'PUT');
        
        if (result?.success) {
            console.log('\n✅ 출석 요청 승인 완료!');
            if (result.data) console.log('📊 결과:', result.data);
        } else {
            console.log('\n❌ 실패:', result.error);
        }
        
        console.log('━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n');
        return result;
    }
    
    // ============================================
    // 6. 교수: 출석 요청 반려
    // PUT /api/professor/attendance/requests/{requestIdx}/reject
    // ============================================
    
    async function testRejectRequest() {
        console.log('\n━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━');
        console.log('❌ 출석 요청 반려');
        console.log('━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━');
        
        const requestIdx = prompt('요청 IDX:', '');
        const reason = prompt('반려 사유:', '');
        
        if (!requestIdx || !reason) {
            console.log('❌ 요청 IDX와 반려 사유가 필요합니다.');
            return { success: false, error: '필수 정보 미입력' };
        }
        
        const data = { rejectionReason: reason };
        console.log(`📤 요청 IDX: ${requestIdx}`);
        console.log(`   반려 사유: ${reason}`);
        
        const result = await apiCall(`/professor/attendance/requests/${requestIdx}/reject`, data, 'PUT');
        
        if (result?.success) {
            console.log('\n✅ 출석 요청 반려 완료!');
            if (result.data) console.log('📊 결과:', result.data);
        } else {
            console.log('\n❌ 실패:', result.error);
        }
        
        console.log('━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n');
        return result;
    }
    
    // ============================================
    // 7. 교수: 출석 직접 입력
    // POST /api/professor/attendance/mark
    // ============================================
    
    async function testMarkAttendance() {
        console.log('\n━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━');
        console.log('✏️  출석 직접 입력');
        console.log('━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━');
        
        const enrollmentIdx = prompt('enrollmentIdx:', '');
        const sessionNumber = prompt('회차 번호:', '');
        const status = prompt('출석 상태 (출/결/지/조):', '');
        
        if (!enrollmentIdx || !sessionNumber || !status) {
            console.log('❌ 모든 정보가 필요합니다.');
            return { success: false, error: '필수 정보 미입력' };
        }
        
        const data = {
            enrollmentIdx: parseInt(enrollmentIdx),
            sessionNumber: parseInt(sessionNumber),
            status
        };
        
        console.log(`📤 enrollmentIdx: ${enrollmentIdx}, 회차: ${sessionNumber}`);
        console.log(`   상태: ${status}`);
        
        const result = await apiCall('/professor/attendance/mark', data);
        
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
        
        const results = { total: 3, success: 0, failed: 0, tests: [] };
        
        try {
            const tests = [
                { name: '내 출석 현황', fn: testMyAttendanceStatus },
                { name: '내 요청 목록', fn: testMyRequests },
                { name: '교수: 요청 목록', fn: testProfessorRequests }
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
        myRequests: testMyRequests,
        profRequests: testProfessorRequests,
        approve: testApproveRequest,
        reject: testRejectRequest,
        mark: testMarkAttendance,
        runAll: runAllTests
    };
    
    console.log('✅ 출석 API 테스트 로드 완료');
    console.log('💡 사용: await attendanceTest.runAll() 또는 개별 함수 실행');
    console.log('━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━');
    
})();
