/**
 * 📢 공지사항 API 테스트 (독립 실행)
 * 
 * 🚀 사용법:
 *    await noticeTest.runAll()            // 전체 테스트
 * 
 * 📋 개별 API 테스트:
 *    await noticeTest.view()              // 공지사항 조회
 *    await noticeTest.save()              // 공지사항 저장
 */

(function() {
    'use strict';
    
    const API_BASE = 'https://bluecrab.chickenkiller.com/BlueCrab-1.0.0';
    
    // ============================================
    // 유틸리티 함수
    // ============================================
    
    function getToken() {
        return window.authToken || localStorage.getItem('jwtAccessToken');
    }
    
    async function apiCall(endpoint, data, method = 'POST', requireAuth = true) {
        const start = performance.now();
        try {
            const headers = { 'Content-Type': 'application/json' };
            
            if (requireAuth) {
                const token = getToken();
                if (!token) {
                    console.error('❌ 로그인 필요!');
                    return { success: false, error: '인증 토큰 없음' };
                }
                headers['Authorization'] = `Bearer ${token}`;
            }
            
            const response = await fetch(`${API_BASE}${endpoint}`, {
                method,
                headers,
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
    // 1. 공지사항 조회 (공개)
    // POST /notice/course-apply/view
    // ============================================
    
    async function testViewNotice() {
        console.log('\n━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━');
        console.log('📢 공지사항 조회');
        console.log('━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━');
        
        console.log('📤 공지사항 조회 요청 (인증 불필요)');
        
        const result = await apiCall('/notice/course-apply/view', {}, 'POST', false);
        
        if (result?.success && result.data) {
            console.log('\n📊 공지사항:');
            console.log(`  내용: ${result.data.message || 'N/A'}`);
            console.log(`  수정일: ${result.data.updatedAt || 'N/A'}`);
            console.log(`  작성자: ${result.data.updatedBy || 'N/A'}`);
            
            console.log('\n✅ 성공!');
        } else {
            console.log('\n❌ 실패:', result.error);
        }
        
        console.log('━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n');
        return result;
    }
    
    // ============================================
    // 2. 공지사항 저장 (관리자/교수)
    // POST /notice/course-apply/save
    // ============================================
    
    async function testSaveNotice() {
        console.log('\n━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━');
        console.log('💾 공지사항 저장');
        console.log('━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━');
        
        const message = prompt('공지사항 내용 (HTML 가능):', '');
        if (!message) {
            console.log('❌ 공지사항 내용이 필요합니다.');
            return { success: false, error: '내용 미입력' };
        }
        
        const data = { message };
        console.log('📤 내용:', message.substring(0, 100) + (message.length > 100 ? '...' : ''));
        
        const result = await apiCall('/notice/course-apply/save', data);
        
        if (result?.success) {
            console.log('\n✅ 공지사항 저장 성공!');
            if (result.data) {
                console.log('📊 결과:');
                console.log(`  IDX: ${result.data.noticeIdx || 'N/A'}`);
                console.log(`  수정일: ${result.data.updatedAt || 'N/A'}`);
                console.log(`  작성자: ${result.data.updatedBy || 'N/A'}`);
            }
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
        console.log('\n🚀 공지사항 API 전체 테스트 시작');
        console.log('━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n');
        
        const results = { total: 1, success: 0, failed: 0, tests: [] };
        
        try {
            const tests = [
                { name: '공지사항 조회', fn: testViewNotice }
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
    
    window.noticeTest = {
        view: testViewNotice,
        save: testSaveNotice,
        runAll: runAllTests
    };
    
    console.log('✅ 공지사항 API 테스트 로드 완료');
    console.log('💡 사용: await noticeTest.runAll() 또는 개별 함수 실행');
    console.log('━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━');
    
})();
