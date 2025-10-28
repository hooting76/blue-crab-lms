/**
 * 📅 출석 API 대량 작업 테스트 (데이터 채우기용)
 * 
 * 🚀 주요 기능:
 *    1. 학생/교수 로그인 전환
 *    2. n~m회차 범위 출석 인정 요청 (학생용)
 *    3. n~m회차 범위 출석 승인 (교수용)
 * 
 * 🔐 로그인:
 *    await bulkAttendance.loginStudent()   // 학생 로그인 (프롬프트로 이메일/비밀번호 입력)
 *    await bulkAttendance.loginProf()      // 교수 로그인 (프롬프트로 이메일/비밀번호 입력)
 * 
 * 📋 대량 작업:
 *    await bulkAttendance.bulkRequest()    // 출석 인정 요청 (프롬프트로 강의코드, 회차 범위 입력)
 *    await bulkAttendance.bulkApprove()    // 특정 학생 출석 승인 (프롬프트로 입력)
 *    await bulkAttendance.bulkApproveAll() // 특정 회차 전체 학생 출석 승인 (프롬프트로 입력)
 * 
 * 🎯 빠른 시작:
 *    await bulkAttendance.quickSetup()  // 대화형 설정 및 실행
 */

(function() {
    'use strict';
    
    const API_BASE = 'https://bluecrab.chickenkiller.com/BlueCrab-1.0.0/api';
    
    // 현재 로그인 상태 저장
    let currentUser = {
        token: null,
        userCode: null,
        userType: null,  // 'student' or 'professor'
        idx: null
    };
    
    // ============================================
    // 유틸리티 함수
    // ============================================
    
    function getToken() {
        return currentUser.token || window.authToken || localStorage.getItem('jwtAccessToken');
    }
    
    async function apiCall(endpoint, data, method = 'POST') {
        const token = getToken();
        if (!token) {
            console.error('❌ 로그인 필요! bulkAttendance.loginStudent() 또는 loginProf() 실행');
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
            return { success: true, data: result, duration };
            
        } catch (error) {
            const duration = (performance.now() - start).toFixed(2);
            console.error(`🔥 예외:`, error.message);
            return { success: false, error: error.message, duration };
        }
    }
    
    function sleep(ms) {
        return new Promise(resolve => setTimeout(resolve, ms));
    }
    
    // ============================================
    // 로그인 기능
    // ============================================
    
    /**
     * 학생 로그인
     */
    async function loginAsStudent(userEmail, password) {
        console.log('\n━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━');
        console.log('🔐 학생 로그인');
        console.log('━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━');
        
        if (!userEmail || !password) {
            userEmail = prompt('학생 이메일 (예: student@example.com):', 'student@example.com');
            password = prompt('비밀번호:', 'Bluecrab256@');
        }
        
        if (!userEmail || !password) {
            console.log('❌ 학생 이메일과 비밀번호가 필요합니다.');
            return { success: false };
        }
        
        try {
            const response = await fetch(`${API_BASE}/auth/login`, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ username: userEmail, password: password })
            });
            
            if (!response.ok) {
                const error = await response.json().catch(() => ({}));
                console.error('❌ 로그인 실패:', error.message || response.statusText);
                return { success: false, error: error.message };
            }
            
            const result = await response.json();
            
            if (result.success && result.data) {
                const loginResponse = result.data;
                currentUser.token = loginResponse.accessToken;
                currentUser.userCode = loginResponse.user?.userName || userEmail;
                currentUser.userType = 'student';
                currentUser.idx = loginResponse.user?.userIdx;
                
                // 로컬스토리지에도 저장
                if (currentUser.token) {
                    localStorage.setItem('jwtAccessToken', currentUser.token);
                    window.authToken = currentUser.token;
                    window.currentUser = loginResponse.user;
                }
                
                console.log(`✅ 학생 로그인 성공!`);
                console.log(`   이메일: ${userEmail}`);
                console.log(`   이름: ${loginResponse.user?.userName || 'N/A'}`);
                console.log(`   IDX: ${currentUser.idx}`);
                console.log(`   토큰 타입: ${loginResponse.tokenType || 'Bearer'}`);
                console.log(`   만료 시간: ${loginResponse.expiresIn || 'N/A'}초`);
                console.log('━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n');
                
                return { success: true, data: loginResponse };
            } else {
                console.error('❌ 로그인 응답 오류:', result);
                return { success: false, error: '로그인 실패' };
            }
            
        } catch (error) {
            console.error('🔥 로그인 예외:', error.message);
            return { success: false, error: error.message };
        }
    }
    
    /**
     * 교수 로그인
     */
    async function loginAsProfessor(userEmail, password) {
        console.log('\n━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━');
        console.log('🔐 교수 로그인');
        console.log('━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━');
        
        if (!userEmail || !password) {
            userEmail = prompt('교수 이메일 (예: prof.octopus@univ.edu):', 'prof.octopus@univ.edu');
            password = prompt('비밀번호:', 'Bluecrab256@');
        }
        
        if (!userEmail || !password) {
            console.log('❌ 교수 이메일과 비밀번호가 필요합니다.');
            return { success: false };
        }
        
        try {
            const response = await fetch(`${API_BASE}/auth/login`, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ username: userEmail, password: password })
            });
            
            if (!response.ok) {
                const error = await response.json().catch(() => ({}));
                console.error('❌ 로그인 실패:', error.message || response.statusText);
                return { success: false, error: error.message };
            }
            
            const result = await response.json();
            
            if (result.success && result.data) {
                const loginResponse = result.data;
                currentUser.token = loginResponse.accessToken;
                currentUser.userCode = loginResponse.user?.userName || userEmail;
                currentUser.userType = 'professor';
                currentUser.idx = loginResponse.user?.userIdx;
                
                // 로컬스토리지에도 저장
                if (currentUser.token) {
                    localStorage.setItem('jwtAccessToken', currentUser.token);
                    window.authToken = currentUser.token;
                    window.currentUser = loginResponse.user;
                }
                
                console.log(`✅ 교수 로그인 성공!`);
                console.log(`   이메일: ${userEmail}`);
                console.log(`   이름: ${loginResponse.user?.userName || 'N/A'}`);
                console.log(`   IDX: ${currentUser.idx}`);
                console.log(`   토큰 타입: ${loginResponse.tokenType || 'Bearer'}`);
                console.log(`   만료 시간: ${loginResponse.expiresIn || 'N/A'}초`);
                
                // 교수 계정 확인
                if (loginResponse.user?.userStudent === 1) {
                    console.log('🎓 교수 계정 확인됨');
                }
                console.log('━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n');
                
                return { success: true, data: loginResponse };
            } else {
                console.error('❌ 로그인 응답 오류:', result);
                return { success: false, error: '로그인 실패' };
            }
            
        } catch (error) {
            console.error('🔥 로그인 예외:', error.message);
            return { success: false, error: error.message };
        }
    }
    
    /**
     * 현재 로그인 상태 확인
     */
    function checkLoginStatus() {
        console.log('\n━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━');
        console.log('👤 현재 로그인 상태');
        console.log('━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━');
        
        if (!currentUser.token) {
            console.log('❌ 로그인되지 않음');
            console.log('💡 bulkAttendance.loginStudent() 또는 loginProf() 실행');
        } else {
            console.log(`✅ 로그인됨`);
            console.log(`   타입: ${currentUser.userType === 'student' ? '학생' : '교수'}`);
            console.log(`   코드: ${currentUser.userCode}`);
            console.log(`   IDX: ${currentUser.idx}`);
            console.log(`   토큰: ${currentUser.token.substring(0, 30)}...`);
        }
        
        console.log('━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n');
        return currentUser;
    }
    
    // ============================================
    // 학생: 대량 출석 인정 요청
    // ============================================
    
    /**
     * n회차부터 m회차까지 출석 인정 요청
     */
    async function bulkAttendanceRequest(lecSerial, fromSession, toSession, delayMs) {
        console.log('\n━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━');
        console.log('📝 대량 출석 인정 요청 (학생)');
        console.log('━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━');
        
        if (currentUser.userType !== 'student') {
            console.error('❌ 학생으로 로그인해야 합니다!');
            console.log('💡 bulkAttendance.loginStudent() 실행');
            return { success: false, error: '권한 없음' };
        }
        
        // 프롬프트로 파라미터 입력받기
        if (!lecSerial) {
            lecSerial = prompt('강의 코드 (예: ETH201):', 'ETH201');
        }
        if (fromSession === undefined) {
            fromSession = parseInt(prompt('시작 회차:', '1'));
        }
        if (toSession === undefined) {
            toSession = parseInt(prompt('종료 회차:', '10'));
        }
        if (delayMs === undefined) {
            delayMs = parseInt(prompt('요청 간격 (ms):', '500'));
        }
        
        if (!lecSerial || isNaN(fromSession) || isNaN(toSession)) {
            console.log('❌ 강의 코드와 회차 범위가 필요합니다.');
            return { success: false, error: '파라미터 부족' };
        }
        
        console.log(`📤 강의: ${lecSerial}`);
        console.log(`📊 회차 범위: ${fromSession} ~ ${toSession}`);
        console.log(`⏱️  요청 간격: ${delayMs}ms`);
        console.log('');
        
        const results = {
            total: toSession - fromSession + 1,
            success: 0,
            failed: 0,
            errors: []
        };
        
        for (let session = fromSession; session <= toSession; session++) {
            console.log(`\n[${session - fromSession + 1}/${results.total}] ${session}회차 요청 중...`);
            
            const data = {
                lecSerial,
                sessionNumber: session
            };
            
            const result = await apiCall('/attendance/request', data);
            
            if (result.success) {
                results.success++;
                console.log(`  ✅ ${session}회차 요청 완료 (${result.duration}ms)`);
            } else {
                results.failed++;
                results.errors.push({ session, error: result.error });
                console.log(`  ❌ ${session}회차 실패: ${result.error}`);
            }
            
            // 다음 요청 전 대기 (마지막 요청 제외)
            if (session < toSession) {
                await sleep(delayMs);
            }
        }
        
        console.log('\n━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━');
        console.log('📊 대량 요청 결과');
        console.log('━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━');
        console.log(`총 요청: ${results.total}건`);
        console.log(`✅ 성공: ${results.success}건`);
        console.log(`❌ 실패: ${results.failed}건`);
        console.log(`📈 성공률: ${((results.success / results.total) * 100).toFixed(1)}%`);
        
        if (results.errors.length > 0) {
            console.log('\n❌ 실패 목록:');
            results.errors.forEach((err, i) => {
                console.log(`  ${i + 1}. ${err.session}회차: ${err.error}`);
            });
        }
        
        console.log('━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n');
        
        return results;
    }
    
    // ============================================
    // 교수: 대량 출석 승인
    // ============================================
    
    /**
     * n회차부터 m회차까지 특정 학생 출석 승인
     */
    async function bulkAttendanceApprove(lecSerial, fromSession, toSession, userIdx, status, delayMs) {
        console.log('\n━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━');
        console.log('✅ 대량 출석 승인 (교수 - 특정 학생)');
        console.log('━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━');
        
        if (currentUser.userType !== 'professor') {
            console.error('❌ 교수로 로그인해야 합니다!');
            console.log('💡 bulkAttendance.loginProf() 실행');
            return { success: false, error: '권한 없음' };
        }
        
        // 프롬프트로 파라미터 입력받기
        if (!lecSerial) {
            lecSerial = prompt('강의 코드 (예: ETH201):', 'ETH201');
        }
        if (fromSession === undefined) {
            fromSession = parseInt(prompt('시작 회차:', '1'));
        }
        if (toSession === undefined) {
            toSession = parseInt(prompt('종료 회차:', '10'));
        }
        if (!userIdx) {
            userIdx = parseInt(prompt('학생 USER_IDX (학생 고유번호):', ''));
        }
        if (!status) {
            status = prompt('출석 상태 (출/지/결/조):', '출');
        }
        if (delayMs === undefined) {
            delayMs = parseInt(prompt('요청 간격 (ms):', '500'));
        }
        
        if (!lecSerial || isNaN(fromSession) || isNaN(toSession) || isNaN(userIdx)) {
            console.log('❌ 강의 코드, 회차 범위, 학생 USER_IDX가 필요합니다.');
            return { success: false, error: '파라미터 부족' };
        }
        
        console.log(`📤 강의: ${lecSerial}`);
        console.log(`📊 회차 범위: ${fromSession} ~ ${toSession}`);
        console.log(`👤 학생 USER_IDX: ${userIdx}`);
        console.log(`📝 출석 상태: ${status}`);
        console.log(`⏱️  요청 간격: ${delayMs}ms`);
        console.log('');
        
        const results = {
            total: toSession - fromSession + 1,
            success: 0,
            failed: 0,
            errors: []
        };
        
        for (let session = fromSession; session <= toSession; session++) {
            console.log(`\n[${session - fromSession + 1}/${results.total}] ${session}회차 승인 중...`);
            
            const data = {
                lecSerial,
                sessionNumber: session,
                attendanceRecords: [
                    {
                        studentIdx: userIdx,
                        status: status
                    }
                ]
            };
            
            const result = await apiCall('/attendance/approve', data);
            
            if (result.success) {
                results.success++;
                console.log(`  ✅ ${session}회차 승인 완료 (${result.duration}ms)`);
            } else {
                results.failed++;
                results.errors.push({ session, error: result.error });
                console.log(`  ❌ ${session}회차 실패: ${result.error}`);
            }
            
            // 다음 요청 전 대기 (마지막 요청 제외)
            if (session < toSession) {
                await sleep(delayMs);
            }
        }
        
        console.log('\n━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━');
        console.log('📊 대량 승인 결과');
        console.log('━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━');
        console.log(`총 요청: ${results.total}건`);
        console.log(`✅ 성공: ${results.success}건`);
        console.log(`❌ 실패: ${results.failed}건`);
        console.log(`📈 성공률: ${((results.success / results.total) * 100).toFixed(1)}%`);
        
        if (results.errors.length > 0) {
            console.log('\n❌ 실패 목록:');
            results.errors.forEach((err, i) => {
                console.log(`  ${i + 1}. ${err.session}회차: ${err.error}`);
            });
        }
        
        console.log('━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n');
        
        return results;
    }
    
    /**
     * 특정 회차 전체 학생 출석 승인
     */
    async function bulkApproveAllStudents(lecSerial, sessionNumber, status) {
        console.log('\n━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━');
        console.log('✅ 전체 학생 출석 승인 (교수)');
        console.log('━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━');
        
        if (currentUser.userType !== 'professor') {
            console.error('❌ 교수로 로그인해야 합니다!');
            console.log('💡 bulkAttendance.loginProf() 실행');
            return { success: false, error: '권한 없음' };
        }
        
        // 프롬프트로 파라미터 입력받기
        if (!lecSerial) {
            lecSerial = prompt('강의 코드 (예: ETH201):', 'ETH201');
        }
        if (sessionNumber === undefined) {
            sessionNumber = parseInt(prompt('회차 번호:', '1'));
        }
        if (!status) {
            status = prompt('출석 상태 (출/지/결/조):', '출');
        }
        
        if (!lecSerial || isNaN(sessionNumber)) {
            console.log('❌ 강의 코드와 회차 번호가 필요합니다.');
            return { success: false, error: '파라미터 부족' };
        }
        
        console.log(`📤 강의: ${lecSerial}`);
        console.log(`📊 회차: ${sessionNumber}`);
        console.log(`📝 출석 상태: ${status}`);
        console.log('');
        
        // 1. 먼저 전체 학생 목록 조회
        console.log('👥 수강생 목록 조회 중...');
        const viewResult = await apiCall('/attendance/professor/view', { lecSerial });
        
        if (!viewResult.success || !viewResult.data) {
            console.error('❌ 학생 목록 조회 실패');
            return { success: false, error: '학생 목록 조회 실패' };
        }
        
        const students = viewResult.data.data || [];
        console.log(`✅ 학생 ${students.length}명 발견\n`);
        
        if (students.length === 0) {
            console.log('⚠️  수강생이 없습니다.');
            return { success: true, total: 0 };
        }
        
        // 2. 전체 학생에 대해 출석 승인
        const attendanceRecords = students.map(student => ({
            studentIdx: student.studentIdx,
            status: status
        }));
        
        console.log(`📝 ${students.length}명 출석 승인 요청 중...`);
        
        const data = {
            lecSerial,
            sessionNumber,
            attendanceRecords
        };
        
        const result = await apiCall('/attendance/approve', data);
        
        if (result.success) {
            console.log(`\n✅ 전체 학생 출석 승인 완료!`);
            console.log(`   승인 학생: ${students.length}명`);
            console.log(`   회차: ${sessionNumber}`);
            console.log(`   상태: ${status}`);
        } else {
            console.log(`\n❌ 출석 승인 실패: ${result.error}`);
        }
        
        console.log('━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n');
        
        return result;
    }
    
    /**
     * n회차부터 m회차까지 전체 학생 출석 승인
     */
    async function bulkApproveAllStudentsRange(lecSerial, fromSession, toSession, status, delayMs) {
        console.log('\n━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━');
        console.log('✅ 범위 전체 학생 출석 승인 (교수)');
        console.log('━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━');
        
        // 프롬프트로 파라미터 입력받기
        if (!lecSerial) {
            lecSerial = prompt('강의 코드 (예: ETH201):', 'ETH201');
        }
        if (fromSession === undefined) {
            fromSession = parseInt(prompt('시작 회차:', '1'));
        }
        if (toSession === undefined) {
            toSession = parseInt(prompt('종료 회차:', '10'));
        }
        if (!status) {
            status = prompt('출석 상태 (출/지/결/조):', '출');
        }
        if (delayMs === undefined) {
            delayMs = parseInt(prompt('요청 간격 (ms):', '1000'));
        }
        
        console.log(`📤 강의: ${lecSerial}`);
        console.log(`📊 회차 범위: ${fromSession} ~ ${toSession}`);
        console.log(`📝 출석 상태: ${status}`);
        console.log(`⏱️  요청 간격: ${delayMs}ms`);
        console.log('');
        
        const results = {
            total: toSession - fromSession + 1,
            success: 0,
            failed: 0,
            errors: []
        };
        
        for (let session = fromSession; session <= toSession; session++) {
            console.log(`\n[${session - fromSession + 1}/${results.total}] ${session}회차 전체 승인 중...`);
            
            const result = await bulkApproveAllStudents(lecSerial, session, status);
            
            if (result.success) {
                results.success++;
            } else {
                results.failed++;
                results.errors.push({ session, error: result.error });
            }
            
            // 다음 요청 전 대기 (마지막 요청 제외)
            if (session < toSession) {
                await sleep(delayMs);
            }
        }
        
        console.log('\n━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━');
        console.log('📊 범위 전체 승인 결과');
        console.log('━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━');
        console.log(`총 회차: ${results.total}개`);
        console.log(`✅ 성공: ${results.success}회차`);
        console.log(`❌ 실패: ${results.failed}회차`);
        console.log(`📈 성공률: ${((results.success / results.total) * 100).toFixed(1)}%`);
        
        if (results.errors.length > 0) {
            console.log('\n❌ 실패 목록:');
            results.errors.forEach((err, i) => {
                console.log(`  ${i + 1}. ${err.session}회차: ${err.error}`);
            });
        }
        
        console.log('━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n');
        
        return results;
    }
    
    // ============================================
    // 빠른 시작 (대화형)
    // ============================================
    
    async function quickSetup() {
        console.log('\n╔════════════════════════════════════════╗');
        console.log('║   🚀 출석 대량 작업 빠른 시작          ║');
        console.log('╚════════════════════════════════════════╝\n');
        
        // 1. 로그인 타입 선택
        const userType = prompt('사용자 타입 선택 (1: 학생, 2: 교수):', '1');
        
        if (userType === '1') {
            // 학생 로그인
            const loginResult = await loginAsStudent();
            if (!loginResult.success) {
                console.log('❌ 로그인 실패. 종료합니다.');
                return;
            }
            
            // 출석 인정 요청
            const lecSerial = prompt('강의 코드:', 'ETH201');
            const fromSession = parseInt(prompt('시작 회차:', '1'));
            const toSession = parseInt(prompt('종료 회차:', '10'));
            
            console.log('\n🚀 대량 출석 인정 요청을 시작합니다...\n');
            await bulkAttendanceRequest(lecSerial, fromSession, toSession);
            
        } else if (userType === '2') {
            // 교수 로그인
            const loginResult = await loginAsProfessor();
            if (!loginResult.success) {
                console.log('❌ 로그인 실패. 종료합니다.');
                return;
            }
            
            // 승인 타입 선택
            const approveType = prompt('승인 타입 (1: 특정 학생, 2: 전체 학생):', '2');
            const lecSerial = prompt('강의 코드:', 'ETH201');
            
            if (approveType === '1') {
                // 특정 학생 승인
                const fromSession = parseInt(prompt('시작 회차:', '1'));
                const toSession = parseInt(prompt('종료 회차:', '10'));
                const studentIdx = parseInt(prompt('학생 IDX:', '2001'));
                const status = prompt('출석 상태 (출/지/결/조):', '출');
                
                console.log('\n🚀 특정 학생 대량 승인을 시작합니다...\n');
                await bulkAttendanceApprove(lecSerial, fromSession, toSession, studentIdx, status);
                
            } else {
                // 전체 학생 승인
                const fromSession = parseInt(prompt('시작 회차:', '1'));
                const toSession = parseInt(prompt('종료 회차:', '10'));
                const status = prompt('출석 상태 (출/지/결/조):', '출');
                
                console.log('\n🚀 전체 학생 대량 승인을 시작합니다...\n');
                await bulkApproveAllStudentsRange(lecSerial, fromSession, toSession, status);
            }
        } else {
            console.log('❌ 잘못된 선택입니다.');
        }
        
        console.log('\n✅ 모든 작업이 완료되었습니다!');
    }
    
    // ============================================
    // 전역 노출
    // ============================================
    
    window.bulkAttendance = {
        // 로그인
        loginStudent: loginAsStudent,
        loginProf: loginAsProfessor,
        status: checkLoginStatus,
        
        // 학생용: 대량 출석 인정 요청
        bulkRequest: bulkAttendanceRequest,
        
        // 교수용: 대량 출석 승인
        bulkApprove: bulkAttendanceApprove,           // 특정 학생 범위 승인
        bulkApproveAll: bulkApproveAllStudents,        // 특정 회차 전체 학생 승인
        bulkApproveAllRange: bulkApproveAllStudentsRange,  // 범위 회차 전체 학생 승인
        
        // 빠른 시작
        quickSetup: quickSetup
    };
    
    console.log('✅ 출석 대량 작업 도구 로드 완료!');
    console.log('');
    console.log('━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━');
    console.log('🚀 빠른 시작:');
    console.log('   await bulkAttendance.quickSetup()');
    console.log('');
    console.log('🔐 로그인:');
    console.log('   await bulkAttendance.loginStudent("student@example.com", "Bluecrab256@")');
    console.log('   await bulkAttendance.loginProf("prof.octopus@univ.edu", "Bluecrab256@")');
    console.log('   bulkAttendance.status()  // 현재 상태 확인');
    console.log('');
    console.log('📝 학생 - 출석 인정 요청:');
    console.log('   await bulkAttendance.bulkRequest()');
    console.log('   // 프롬프트로 강의 코드, 회차 범위 입력');
    console.log('');
    console.log('✅ 교수 - 출석 승인:');
    console.log('   // 특정 학생 범위 승인');
    console.log('   await bulkAttendance.bulkApprove()');
    console.log('   // 프롬프트로 강의 코드, 회차, 학생 USER_IDX, 상태 입력');
    console.log('');
    console.log('   // 특정 회차 전체 학생 승인');
    console.log('   await bulkAttendance.bulkApproveAll()');
    console.log('   // 프롬프트로 강의 코드, 회차, 상태 입력');
    console.log('');
    console.log('   // 범위 회차 전체 학생 승인');
    console.log('   await bulkAttendance.bulkApproveAllRange()');
    console.log('   // 프롬프트로 강의 코드, 회차 범위, 상태 입력');
    console.log('━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━');
    
})();
