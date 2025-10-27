/**
 * 📝 수강신청 API 테스트 (독립 실행)
 * 
 * 🚀 사용법:
 *    await enrollmentTest.runAll()        // 전체 테스트
 * 
 * 📋 개별 API 테스트:
 *    await enrollmentTest.available()     // 수강신청 가능한 강의 목록
 *    await enrollmentTest.check()         // 수강 여부 확인
 *    await enrollmentTest.list()          // 내 수강 목록
 *    await enrollmentTest.enroll()        // 수강 신청
 *    await enrollmentTest.cancel()        // 수강 취소
 *    await enrollmentTest.stats()         // 통계 조회
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
                body: method !== 'DELETE' ? JSON.stringify(data) : undefined
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
    // 1. 수강신청 가능한 강의 목록 조회
    // POST /api/lectures/eligible
    // ============================================
    
    async function testAvailableLectures() {
        console.log('\n━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━');
        console.log('📚 수강신청 가능한 강의 목록');
        console.log('━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━');
        
        // 로그인된 사용자 정보에서 studentId 가져오기
        const studentId = window.currentUser?.id;
        if (!studentId) {
            console.log('❌ 로그인된 사용자 정보를 찾을 수 없습니다.');
            console.log('💡 먼저 await login()으로 로그인하세요.');
            return { success: false, error: '사용자 정보 없음' };
        }
        
        const page = prompt('페이지 번호 (기본: 0):', '0');
        const size = prompt('페이지 크기 (기본: 20):', '20');
        
        const data = {
            studentId: studentId,
            page: parseInt(page) || 0,
            size: parseInt(size) || 20
        };
        
        console.log(`📤 학생 ID: ${studentId}, 페이지: ${data.page}`);
        
        const result = await apiCall('/lectures/eligible', data);
        
        if (result?.success && result.data) {
            const response = result.data;
            const lectures = response.eligibleLectures || [];
            
            console.log(`\n📊 전체 강의: ${response.totalCount || 0}개`);
            console.log(`✅ 신청 가능: ${response.eligibleCount || 0}개`);
            console.log(`❌ 신청 불가: ${response.ineligibleCount || 0}개`);
            
            if (lectures.length > 0) {
                console.log('\n📋 수강 가능한 강의 목록:');
                lectures.forEach((lec, i) => {
                    console.log(`  ${i+1}. [${lec.lecSerial || 'N/A'}] ${lec.lecTit || 'N/A'}`);
                    console.log(`     교수: ${lec.professorName || 'N/A'}, 정원: ${lec.lecCurrent || 0}/${lec.lecMany || 0}`);
                    console.log(`     학점: ${lec.lecPoint || 'N/A'}, 시간: ${lec.lecTime || 'N/A'}`);
                    if (lec.eligible === false) {
                        console.log(`     ⚠️ 부적격 사유: ${lec.reason || 'N/A'}`);
                    }
                });
            } else {
                console.log('💡 신청 가능한 강의가 없습니다.');
            }
            
            console.log('\n✅ 성공!');
        } else {
            console.log('\n❌ 실패:', result.error);
        }
        
        console.log('━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n');
        return result;
    }
    
    // ============================================
    // 2. 수강 여부 확인
    // POST /api/enrollments/list
    // ============================================
    
    async function testCheckEnrollment() {
        console.log('\n━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━');
        console.log('🔍 수강 여부 확인');
        console.log('━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━');
        
        const lecSerial = prompt('강의 코드 (예: CS284):', '');
        if (!lecSerial) {
            console.log('❌ 강의 코드가 필요합니다.');
            return { success: false, error: '강의 코드 미입력' };
        }
        
        const data = {
            lecSerial,
            checkEnrollment: true
            // studentIdx는 백엔드에서 JWT 토큰으로부터 자동 추출
        };
        
        console.log(`📤 강의: ${lecSerial} (학생 정보는 JWT에서 자동 추출)`);
        
        const result = await apiCall('/enrollments/list', data);
        
        if (result?.success && result.data) {
            const enrolled = result.data.enrolled || false;
            console.log(`\n📊 수강 여부: ${enrolled ? '✅ 수강중' : '❌ 미수강'}`);
            
            console.log('\n✅ 성공!');
        } else {
            console.log('\n❌ 실패:', result.error);
        }
        
        console.log('━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n');
        return result;
    }
    
    // ============================================
    // 3. 내 수강 목록 조회
    // POST /api/enrollments/list
    // ============================================
    
    async function testEnrollmentList() {
        console.log('\n━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━');
        console.log('📋 내 수강 목록 조회');
        console.log('━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━');
        
        const page = prompt('페이지 번호 (기본: 0):', '0');
        const size = prompt('페이지 크기 (기본: 20):', '20');
        
        const data = {
            page: parseInt(page) || 0,
            size: parseInt(size) || 20
            // ✅ studentIdx는 백엔드에서 JWT 토큰으로부터 자동 추출
        };
        
        console.log(`📤 페이지: ${data.page} (학생 ID는 JWT에서 자동 추출)`);
        
        const result = await apiCall('/enrollments/list', data);
        
        if (result?.success && result.data) {
            const enrollments = result.data.content || [];
            console.log(`\n📊 수강 중인 강의: ${enrollments.length}개`);
            console.log(`📄 전체 페이지: ${result.data.totalPages || 0}페이지 (총 ${result.data.totalElements || 0}건)`);
            
            if (enrollments.length > 0) {
                console.log('\n📋 수강 목록:');
                console.log('━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━');
                enrollments.forEach((enr, i) => {
                    console.log(`\n  ${i+1}. ${enr.lecTit || 'N/A'} [${enr.lecSerial || 'N/A'}]`);
                    console.log(`     👨‍🏫 교수: ${enr.lecProfName || 'N/A'}`);
                    console.log(`     📚 학점: ${enr.lecPoint || 'N/A'}점`);
                    console.log(`     ⏰ 시간: ${enr.lecTime || 'N/A'}`);
                });
                console.log('\n━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━');
            } else {
                console.log('💡 수강 중인 강의가 없습니다.');
            }
            
            console.log('\n✅ 성공!');
        } else {
            console.log('\n❌ 실패:', result.error);
        }
        
        console.log('━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n');
        return result;
    }
    
    // ============================================
    // 4. 수강 신청
    // POST /api/enrollments/enroll
    // ============================================
    
    async function testEnroll() {
        console.log('\n━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━');
        console.log('➕ 수강 신청');
        console.log('━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━');
        
        const lecSerial = prompt('강의 코드 (예: CS284):', '');
        if (!lecSerial) {
            console.log('❌ 강의 코드가 필요합니다.');
            return { success: false, error: '강의 코드 미입력' };
        }
        
        const data = {
            lecSerial
            // studentIdx는 백엔드에서 JWT 토큰으로부터 자동 추출
        };
        
        console.log(`📤 강의: ${lecSerial} (학생 정보는 JWT에서 자동 추출)`);
        
        const result = await apiCall('/enrollments/enroll', data);
        
        if (result?.success) {
            console.log('\n✅ 수강 신청 성공!');
            if (result.data) {
                console.log('📊 결과:', result.data);
            }
        } else {
            console.log('\n❌ 실패:', result.error);
        }
        
        console.log('━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n');
        return result;
    }
    
    // ============================================
    // 5. 수강 취소
    // DELETE /api/enrollments/{enrollmentIdx}
    // ============================================
    
    async function testCancelEnrollment() {
        console.log('\n━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━');
        console.log('🗑️  수강 취소');
        console.log('━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━');
        
        const enrollmentIdx = prompt('수강신청 IDX (enrollmentIdx):', '');
        if (!enrollmentIdx) {
            console.log('❌ 수강신청 IDX가 필요합니다.');
            return { success: false, error: 'enrollmentIdx 미입력' };
        }
        
        const confirm = prompt(`⚠️  정말 수강을 취소하시겠습니까? (yes/no):`, 'no');
        if (confirm.toLowerCase() !== 'yes') {
            console.log('❌ 취소됨');
            return { success: false, error: '사용자 취소' };
        }
        
        console.log(`📤 수강신청 IDX: ${enrollmentIdx}`);
        
        const result = await apiCall(`/enrollments/${enrollmentIdx}`, null, 'DELETE');
        
        if (result?.success) {
            console.log('\n✅ 수강 취소 성공!');
        } else {
            console.log('\n❌ 실패:', result.error);
        }
        
        console.log('━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n');
        return result;
    }
    
    // ============================================
    // 6. 수강신청 통계
    // POST /api/enrollments/list
    // ============================================
    
    async function testEnrollmentStats() {
        console.log('\n━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━');
        console.log('📊 수강신청 통계');
        console.log('━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━');
        
        const statsType = prompt('통계 타입 (1: 전체, 2: 강의별):', '1');
        
        let data = { stats: true };
        
        if (statsType === '2') {
            const lecSerial = prompt('강의 코드:', '');
            if (lecSerial) data.lecSerial = lecSerial;
        }
        
        console.log('📤 통계 조회:', data);
        
        const result = await apiCall('/enrollments/list', data);
        
        if (result?.success && result.data) {
            console.log('\n📊 통계 정보:');
            
            if (result.data.totalCount !== undefined) {
                console.log(`  전체 수강신청: ${result.data.totalCount}건`);
            }
            if (result.data.enrollmentCount !== undefined) {
                console.log(`  강의별 수강신청: ${result.data.enrollmentCount}건`);
            }
            if (result.data.lecSerial) {
                console.log(`  강의 코드: ${result.data.lecSerial}`);
            }
            
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
        console.log('\n🚀 수강신청 API 전체 테스트 시작');
        console.log('━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n');
        
        const results = { total: 4, success: 0, failed: 0, tests: [] };
        
        try {
            const tests = [
                { name: '수강신청 가능한 강의 목록', fn: testAvailableLectures },
                { name: '수강 여부 확인', fn: testCheckEnrollment },
                { name: '내 수강 목록', fn: testEnrollmentList },
                { name: '통계 조회', fn: testEnrollmentStats }
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
    
    window.enrollmentTest = {
        available: testAvailableLectures,
        check: testCheckEnrollment,
        list: testEnrollmentList,
        enroll: testEnroll,
        cancel: testCancelEnrollment,
        stats: testEnrollmentStats,
        runAll: runAllTests
    };
    
    console.log('✅ 수강신청 API 테스트 로드 완료');
    console.log('💡 사용: await enrollmentTest.runAll() 또는 개별 함수 실행');
    console.log('━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━');
    
})();
