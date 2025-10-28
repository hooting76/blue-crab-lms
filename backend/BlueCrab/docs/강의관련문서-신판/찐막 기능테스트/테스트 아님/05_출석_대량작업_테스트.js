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
    
    /**
     * 🎲 랜덤 출석 상태로 전체 학생 출석 승인 (출/지/결)
     */
    async function bulkApproveAllStudentsRandomRange(lecSerial, fromSession, toSession, delayMs, attendRate, lateRate, absentRate) {
        console.log('\n━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━');
        console.log('🎲 랜덤 출석 승인 (교수)');
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
            toSession = parseInt(prompt('종료 회차:', '80'));
        }
        if (attendRate === undefined) {
            attendRate = parseInt(prompt('출석 비율 (%):', '70'));
        }
        if (lateRate === undefined) {
            lateRate = parseInt(prompt('지각 비율 (%):', '20'));
        }
        if (absentRate === undefined) {
            absentRate = parseInt(prompt('결석 비율 (%):', '10'));
        }
        if (delayMs === undefined) {
            delayMs = parseInt(prompt('요청 간격 (ms):', '1000'));
        }
        
        // 비율 검증
        const totalRate = attendRate + lateRate + absentRate;
        if (totalRate !== 100) {
            console.error(`❌ 비율 합계가 100%가 아닙니다! (현재: ${totalRate}%)`);
            return { success: false, error: '비율 오류' };
        }
        
        console.log(`📤 강의: ${lecSerial}`);
        console.log(`📊 회차 범위: ${fromSession} ~ ${toSession}`);
        console.log(`🎲 출석 비율: 출석 ${attendRate}% / 지각 ${lateRate}% / 결석 ${absentRate}%`);
        console.log(`⏱️  요청 간격: ${delayMs}ms`);
        console.log('');
        
        // 랜덤 상태 결정 함수
        function getRandomStatus() {
            const rand = Math.random() * 100;
            if (rand < attendRate) return '출';
            if (rand < attendRate + lateRate) return '지';
            return '결';
        }
        
        const totalResults = {
            totalSessions: toSession - fromSession + 1,
            successSessions: 0,
            failedSessions: 0,
            statistics: {
                출: 0,
                지: 0,
                결: 0
            },
            sessionDetails: []
        };
        
        // 각 회차별로 처리
        for (let session = fromSession; session <= toSession; session++) {
            console.log(`\n╔════════════════════════════════════════╗`);
            console.log(`║ [${session - fromSession + 1}/${totalResults.totalSessions}] ${session}회차 처리 중...`);
            console.log(`╚════════════════════════════════════════╝`);
            
            // 1. 먼저 전체 학생 목록 조회
            console.log('👥 수강생 목록 조회 중...');
            const viewResult = await apiCall('/attendance/professor/view', { lecSerial });
            
            if (!viewResult.success || !viewResult.data) {
                console.log(`❌ ${session}회차 학생 목록 조회 실패`);
                totalResults.failedSessions++;
                totalResults.sessionDetails.push({
                    session,
                    success: false,
                    error: '학생 목록 조회 실패'
                });
                
                if (session < toSession) {
                    await sleep(delayMs);
                }
                continue;
            }
            
            const students = viewResult.data.data || [];
            console.log(`✅ 학생 ${students.length}명 발견`);
            
            if (students.length === 0) {
                console.log(`⚠️  ${session}회차 수강생 없음`);
                totalResults.failedSessions++;
                totalResults.sessionDetails.push({
                    session,
                    success: false,
                    error: '수강생 없음'
                });
                
                if (session < toSession) {
                    await sleep(delayMs);
                }
                continue;
            }
            
            // 2. 각 학생에게 랜덤 출석 상태 부여
            const attendanceRecords = students.map(student => {
                const status = getRandomStatus();
                totalResults.statistics[status]++;
                return {
                    studentIdx: student.studentIdx,
                    status: status
                };
            });
            
            // 통계 출력
            const sessionStats = {
                출: attendanceRecords.filter(r => r.status === '출').length,
                지: attendanceRecords.filter(r => r.status === '지').length,
                결: attendanceRecords.filter(r => r.status === '결').length
            };
            
            console.log(`\n📊 ${session}회차 랜덤 분배 결과:`);
            console.log(`   ✅ 출석: ${sessionStats.출}명 (${((sessionStats.출 / students.length) * 100).toFixed(1)}%)`);
            console.log(`   ⏰ 지각: ${sessionStats.지}명 (${((sessionStats.지 / students.length) * 100).toFixed(1)}%)`);
            console.log(`   ❌ 결석: ${sessionStats.결}명 (${((sessionStats.결 / students.length) * 100).toFixed(1)}%)`);
            
            // 3. 출석 승인 요청
            console.log(`\n📝 ${session}회차 출석 승인 요청 중...`);
            
            const data = {
                lecSerial,
                sessionNumber: session,
                attendanceRecords
            };
            
            const result = await apiCall('/attendance/approve', data);
            
            if (result.success) {
                totalResults.successSessions++;
                totalResults.sessionDetails.push({
                    session,
                    success: true,
                    studentCount: students.length,
                    stats: sessionStats
                });
                console.log(`✅ ${session}회차 승인 완료 (${result.duration}ms)`);
            } else {
                totalResults.failedSessions++;
                totalResults.sessionDetails.push({
                    session,
                    success: false,
                    error: result.error
                });
                console.log(`❌ ${session}회차 승인 실패: ${result.error}`);
            }
            
            // 다음 회차 전 대기 (마지막 회차 제외)
            if (session < toSession) {
                console.log(`\n⏳ 다음 회차까지 ${delayMs}ms 대기...\n`);
                await sleep(delayMs);
            }
        }
        
        // 최종 결과 출력
        console.log('\n\n');
        console.log('╔════════════════════════════════════════╗');
        console.log('║   📊 랜덤 출석 승인 최종 결과          ║');
        console.log('╚════════════════════════════════════════╝');
        console.log(`\n총 회차 수: ${totalResults.totalSessions}회차`);
        console.log(`✅ 성공: ${totalResults.successSessions}회차`);
        console.log(`❌ 실패: ${totalResults.failedSessions}회차`);
        console.log(`📈 성공률: ${((totalResults.successSessions / totalResults.totalSessions) * 100).toFixed(1)}%`);
        
        console.log('\n━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━');
        console.log('📊 전체 출석 통계:');
        console.log('━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━');
        const totalCount = totalResults.statistics.출 + totalResults.statistics.지 + totalResults.statistics.결;
        console.log(`✅ 출석: ${totalResults.statistics.출}건 (${((totalResults.statistics.출 / totalCount) * 100).toFixed(1)}%)`);
        console.log(`⏰ 지각: ${totalResults.statistics.지}건 (${((totalResults.statistics.지 / totalCount) * 100).toFixed(1)}%)`);
        console.log(`❌ 결석: ${totalResults.statistics.결}건 (${((totalResults.statistics.결 / totalCount) * 100).toFixed(1)}%)`);
        console.log(`📋 총 처리: ${totalCount}건`);
        
        console.log('\n━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━');
        console.log('📋 회차별 상세 결과:');
        console.log('━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━');
        
        totalResults.sessionDetails.forEach((detail, i) => {
            console.log(`\n${i + 1}. ${detail.session}회차`);
            if (detail.success) {
                console.log(`   ✅ 성공 - 학생 ${detail.studentCount}명`);
                console.log(`      출석: ${detail.stats.출}명, 지각: ${detail.stats.지}명, 결석: ${detail.stats.결}명`);
            } else {
                console.log(`   ❌ 실패 - ${detail.error}`);
            }
        });
        
        console.log('\n━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n');
        
        return totalResults;
    }
    
    // ============================================
    // 다중 학생 대량 실행
    // ============================================
    
    /**
     * USER_IDX 33~55 학생들 모두 출석 인정 요청
     */
    async function bulkRequestAllStudents(lecSerial, fromSession, toSession, delayMs, studentDelayMs) {
        console.log('\n━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━');
        console.log('👥 전체 학생 대량 출석 인정 요청');
        console.log('━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━');
        
        // USER_IDX 33~55 학생 정보 (USER_TBL 기반)
        const students = [
            { idx: 33, email: 'student033@bluecrab.edu', name: '김민준' },
            { idx: 34, email: 'student034@bluecrab.edu', name: '이서연' },
            { idx: 35, email: 'student035@bluecrab.edu', name: '박지훈' },
            { idx: 36, email: 'student036@bluecrab.edu', name: '최수아' },
            { idx: 37, email: 'student037@bluecrab.edu', name: '정하윤' },
            { idx: 38, email: 'student038@bluecrab.edu', name: '강도윤' },
            { idx: 39, email: 'student039@bluecrab.edu', name: '조예은' },
            { idx: 40, email: 'student040@bluecrab.edu', name: '윤시우' },
            { idx: 41, email: 'student041@bluecrab.edu', name: '장서준' },
            { idx: 42, email: 'student042@bluecrab.edu', name: '임채원' },
            { idx: 43, email: 'student043@bluecrab.edu', name: '한지우' },
            { idx: 44, email: 'student044@bluecrab.edu', name: '오민서' },
            { idx: 45, email: 'student045@bluecrab.edu', name: '신유진' },
            { idx: 46, email: 'student046@bluecrab.edu', name: '권현우' },
            { idx: 47, email: 'student047@bluecrab.edu', name: '송지아' },
            { idx: 48, email: 'student048@bluecrab.edu', name: '홍준서' },
            { idx: 49, email: 'student049@bluecrab.edu', name: '배소율' },
            { idx: 50, email: 'student050@bluecrab.edu', name: '노은우' },
            { idx: 51, email: 'student051@bluecrab.edu', name: '고민준' },
            { idx: 52, email: 'student052@bluecrab.edu', name: '류서아' },
            { idx: 53, email: 'student053@bluecrab.edu', name: '문도현' },
            { idx: 54, email: 'student054@bluecrab.edu', name: '양하은' },
            { idx: 55, email: 'student055@bluecrab.edu', name: '손지호' }
        ];
        
        const password = 'Bluecrab256@';
        
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
            delayMs = parseInt(prompt('회차별 요청 간격 (ms):', '500'));
        }
        if (studentDelayMs === undefined) {
            studentDelayMs = parseInt(prompt('학생별 대기 시간 (ms):', '2000'));
        }
        
        if (!lecSerial || isNaN(fromSession) || isNaN(toSession)) {
            console.log('❌ 강의 코드와 회차 범위가 필요합니다.');
            return { success: false, error: '파라미터 부족' };
        }
        
        console.log(`📤 강의: ${lecSerial}`);
        console.log(`📊 회차 범위: ${fromSession} ~ ${toSession}`);
        console.log(`👥 학생 수: ${students.length}명`);
        console.log(`⏱️  회차 간격: ${delayMs}ms`);
        console.log(`⏱️  학생 간격: ${studentDelayMs}ms`);
        console.log('');
        
        const totalResults = {
            totalStudents: students.length,
            successStudents: 0,
            failedStudents: 0,
            studentResults: []
        };
        
        // 각 학생별로 순차 실행
        for (let i = 0; i < students.length; i++) {
            const student = students[i];
            
            console.log(`\n╔════════════════════════════════════════╗`);
            console.log(`║ [${i + 1}/${students.length}] ${student.name} (IDX: ${student.idx})`);
            console.log(`╚════════════════════════════════════════╝`);
            
            // 학생 로그인
            const loginResult = await loginAsStudent(student.email, password);
            
            if (!loginResult.success) {
                console.log(`❌ ${student.name} 로그인 실패`);
                totalResults.failedStudents++;
                totalResults.studentResults.push({
                    student: student.name,
                    idx: student.idx,
                    success: false,
                    error: '로그인 실패'
                });
                
                // 다음 학생으로
                if (i < students.length - 1) {
                    await sleep(studentDelayMs);
                }
                continue;
            }
            
            // 출석 인정 요청 실행
            const requestResult = await bulkAttendanceRequest(lecSerial, fromSession, toSession, delayMs);
            
            if (requestResult.success !== false) {
                totalResults.successStudents++;
                totalResults.studentResults.push({
                    student: student.name,
                    idx: student.idx,
                    success: true,
                    sessionsSuccess: requestResult.success,
                    sessionsFailed: requestResult.failed,
                    sessionsTotal: requestResult.total
                });
            } else {
                totalResults.failedStudents++;
                totalResults.studentResults.push({
                    student: student.name,
                    idx: student.idx,
                    success: false,
                    error: requestResult.error
                });
            }
            
            // 다음 학생 전 대기 (마지막 학생 제외)
            if (i < students.length - 1) {
                console.log(`\n⏳ 다음 학생 처리까지 ${studentDelayMs}ms 대기...\n`);
                await sleep(studentDelayMs);
            }
        }
        
        // 최종 결과 출력
        console.log('\n\n');
        console.log('╔════════════════════════════════════════╗');
        console.log('║   📊 전체 학생 대량 요청 최종 결과     ║');
        console.log('╚════════════════════════════════════════╝');
        console.log(`\n총 학생 수: ${totalResults.totalStudents}명`);
        console.log(`✅ 성공: ${totalResults.successStudents}명`);
        console.log(`❌ 실패: ${totalResults.failedStudents}명`);
        console.log(`📈 성공률: ${((totalResults.successStudents / totalResults.totalStudents) * 100).toFixed(1)}%`);
        
        console.log('\n━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━');
        console.log('📋 학생별 상세 결과:');
        console.log('━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━');
        
        totalResults.studentResults.forEach((result, i) => {
            console.log(`\n${i + 1}. ${result.student} (IDX: ${result.idx})`);
            if (result.success) {
                console.log(`   ✅ 성공 - 출석 ${result.sessionsSuccess}/${result.sessionsTotal}회차`);
            } else {
                console.log(`   ❌ 실패 - ${result.error}`);
            }
        });
        
        console.log('\n━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n');
        
        return totalResults;
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
        bulkRequestAll: bulkRequestAllStudents,  // 🆕 전체 학생 대량 요청
        
        // 교수용: 대량 출석 승인
        bulkApprove: bulkAttendanceApprove,           // 특정 학생 범위 승인
        bulkApproveAll: bulkApproveAllStudents,        // 특정 회차 전체 학생 승인
        bulkApproveAllRange: bulkApproveAllStudentsRange,  // 범위 회차 전체 학생 승인
        bulkApproveRandom: bulkApproveAllStudentsRandomRange,  // 🆕 랜덤 출석 상태로 승인
        
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
    console.log('👥 전체 학생 (IDX 33~55) 대량 요청:');
    console.log('   await bulkAttendance.bulkRequestAll()');
    console.log('   // 23명의 학생 모두 자동 로그인 후 출석 요청');
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
    console.log('');
    console.log('🎲 교수 - 랜덤 출석 승인 (출/지/결):');
    console.log('   await bulkAttendance.bulkApproveRandom()');
    console.log('   // 각 학생에게 랜덤하게 출석/지각/결석 분배');
    console.log('   // 프롬프트로 강의 코드, 회차 범위, 비율(%) 입력');
    console.log('━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━');
    
})();
