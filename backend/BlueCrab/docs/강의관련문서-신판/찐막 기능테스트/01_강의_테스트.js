/**
 * 📚 강의 API 테스트 (독립 실행)
 * 
 * 🚀 사용법:
 *    await lectureTest.runAll()           // 전체 테스트
 * 
 * 📋 개별 API 테스트:
 *    await lectureTest.list()             // 강의 목록 조회
 *    await lectureTest.search()           // 강의 검색
 *    await lectureTest.detail()           // 강의 상세 조회
 *    await lectureTest.create()           // 강의 생성
 *    await lectureTest.update()           // 강의 수정
 *    await lectureTest.delete()           // 강의 삭제
 *    await lectureTest.stats()            // 강의 통계
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
    // 1. 강의 목록 조회
    // POST /api/lectures
    // ============================================
    
    async function testLectureList() {
        console.log('\n━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━');
        console.log('📚 강의 목록 조회');
        console.log('━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━');
        
        const page = prompt('페이지 번호 (기본: 0):', '0');
        const size = prompt('페이지 크기 (기본: 20):', '20');
        
        const data = {
            page: parseInt(page) || 0,
            size: parseInt(size) || 20
        };
        
        console.log(`📤 페이지: ${data.page}, 크기: ${data.size}`);
        
        const result = await apiCall('/lectures', data);
        
        if (result?.success && result.data) {
            const lectures = Array.isArray(result.data) ? result.data : result.data.content || [];
            console.log(`\n📊 조회 결과: ${lectures.length}개`);
            
            if (lectures.length > 0) {
                console.log('\n📋 강의 목록 (최대 5개):');
                lectures.slice(0, 5).forEach((lec, i) => {
                    console.log(`  ${i+1}. [${lec.lecSerial}] ${lec.lecTit}`);
                    console.log(`     교수: ${lec.professorName || 'N/A'}, 정원: ${lec.lecCurrent}/${lec.lecMany}`);
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
    // 2. 강의 검색
    // POST /api/lectures
    // ============================================
    
    async function testLectureSearch() {
        console.log('\n━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━');
        console.log('🔍 강의 검색');
        console.log('━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━');
        
        const searchType = prompt('검색 타입 (1: 강의명, 2: 교수명, 3: 학년+학기):', '1');
        
        let data = {};
        
        if (searchType === '1') {
            const title = prompt('강의명 입력:', '');
            if (title) data.title = title;
        } else if (searchType === '2') {
            const professor = prompt('교수명 입력:', '');
            if (professor) data.professor = professor;
        } else if (searchType === '3') {
            const year = prompt('학년 (1-4):', '');
            const semester = prompt('학기 (1-2):', '');
            if (year) data.year = parseInt(year);
            if (semester) data.semester = parseInt(semester);
        }
        
        console.log('📤 검색 조건:', data);
        
        const result = await apiCall('/lectures', data);
        
        if (result?.success && result.data) {
            const lectures = Array.isArray(result.data) ? result.data : result.data.content || [];
            console.log(`\n📊 검색 결과: ${lectures.length}개`);
            
            if (lectures.length > 0) {
                console.log('\n📋 검색된 강의:');
                lectures.slice(0, 5).forEach((lec, i) => {
                    console.log(`  ${i+1}. [${lec.lecSerial}] ${lec.lecTit}`);
                    console.log(`     교수: ${lec.professorName || 'N/A'}`);
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
    // 3. 강의 상세 조회
    // POST /api/lectures
    // ============================================
    
    async function testLectureDetail() {
        console.log('\n━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━');
        console.log('📖 강의 상세 조회');
        console.log('━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━');
        
        const serial = prompt('강의 코드 입력 (예: CS284):', '');
        if (!serial) {
            console.log('❌ 강의 코드가 필요합니다.');
            return { success: false, error: '강의 코드 미입력' };
        }
        
        const data = { serial };
        console.log(`📤 강의 코드: ${serial}`);
        
        const result = await apiCall('/lectures', data);
        
        if (result?.success && result.data) {
            const lec = result.data;
            console.log('\n📊 강의 정보:');
            console.log(`  코드: ${lec.lecSerial}`);
            console.log(`  강의명: ${lec.lecTit}`);
            console.log(`  교수: ${lec.professorName || 'N/A'}`);
            console.log(`  학점: ${lec.lecPoint}`);
            console.log(`  정원: ${lec.lecCurrent}/${lec.lecMany}`);
            console.log(`  시간: ${lec.lecTime || 'N/A'}`);
            console.log(`  설명: ${lec.lecSummary || 'N/A'}`);
            console.log(`  개설: ${lec.lecOpen === 1 ? '개설' : '미개설'}`);
            
            console.log('\n✅ 성공!');
        } else {
            console.log('\n❌ 실패:', result.error);
        }
        
        console.log('━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n');
        return result;
    }
    
    // ============================================
    // 4. 강의 생성 (교수/관리자)
    // POST /api/lectures/create
    // ============================================
    
    async function testLectureCreate() {
        console.log('\n━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━');
        console.log('➕ 강의 생성');
        console.log('━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━');
        
        const serial = prompt('강의 코드 (예: CS999):', '');
        const title = prompt('강의명:', '');
        const point = prompt('학점 (1-3):', '3');
        const many = prompt('정원:', '50');
        const summary = prompt('강의 설명:', '');
        const time = prompt('강의 시간 (예: 월1월2수1수2):', '');
        
        if (!serial || !title) {
            console.log('❌ 강의 코드와 강의명은 필수입니다.');
            return { success: false, error: '필수 정보 미입력' };
        }
        
        const data = {
            lecSerial: serial,
            lecTit: title,
            lecPoint: parseInt(point) || 3,
            lecMany: parseInt(many) || 50,
            lecSummary: summary || '',
            lecTime: time || '',
            lecOpen: 1,
            lecMajor: 1,
            lecMust: 0
        };
        
        console.log('📤 생성할 강의:', data);
        
        const result = await apiCall('/lectures/create', data);
        
        if (result?.success) {
            console.log('\n✅ 강의 생성 성공!');
            if (result.data) console.log('📊 결과:', result.data);
        } else {
            console.log('\n❌ 실패:', result.error);
        }
        
        console.log('━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n');
        return result;
    }
    
    // ============================================
    // 5. 강의 수정 (교수/관리자)
    // POST /api/lectures/update
    // ============================================
    
    async function testLectureUpdate() {
        console.log('\n━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━');
        console.log('✏️  강의 수정');
        console.log('━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━');
        
        const serial = prompt('수정할 강의 코드:', '');
        if (!serial) {
            console.log('❌ 강의 코드가 필요합니다.');
            return { success: false, error: '강의 코드 미입력' };
        }
        
        console.log('\n💡 수정하지 않을 항목은 빈칸으로 두세요.\n');
        
        const title = prompt('강의명:', '');
        const point = prompt('학점:', '');
        const many = prompt('정원:', '');
        const summary = prompt('강의 설명:', '');
        const open = prompt('개설 여부 (1: 개설, 0: 미개설):', '');
        
        const data = { lecSerial: serial };
        
        if (title) data.lecTit = title;
        if (point) data.lecPoint = parseInt(point);
        if (many) data.lecMany = parseInt(many);
        if (summary) data.lecSummary = summary;
        if (open) data.lecOpen = parseInt(open);
        
        console.log('📤 수정 내용:', data);
        
        const result = await apiCall('/lectures/update', data);
        
        if (result?.success) {
            console.log('\n✅ 강의 수정 성공!');
            if (result.data) console.log('📊 결과:', result.data);
        } else {
            console.log('\n❌ 실패:', result.error);
        }
        
        console.log('━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n');
        return result;
    }
    
    // ============================================
    // 6. 강의 삭제 (교수/관리자)
    // POST /api/lectures/delete
    // ============================================
    
    async function testLectureDelete() {
        console.log('\n━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━');
        console.log('🗑️  강의 삭제');
        console.log('━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━');
        
        const serial = prompt('삭제할 강의 코드:', '');
        if (!serial) {
            console.log('❌ 강의 코드가 필요합니다.');
            return { success: false, error: '강의 코드 미입력' };
        }
        
        const confirm = prompt(`⚠️  정말 "${serial}" 강의를 삭제하시겠습니까? (yes/no):`, 'no');
        if (confirm.toLowerCase() !== 'yes') {
            console.log('❌ 삭제 취소됨');
            return { success: false, error: '사용자 취소' };
        }
        
        const data = { lecSerial: serial };
        console.log(`📤 삭제할 강의: ${serial}`);
        
        const result = await apiCall('/lectures/delete', data);
        
        if (result?.success) {
            console.log('\n✅ 강의 삭제 성공!');
        } else {
            console.log('\n❌ 실패:', result.error);
        }
        
        console.log('━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n');
        return result;
    }
    
    // ============================================
    // 7. 강의 통계
    // POST /api/lectures/stats
    // ============================================
    
    async function testLectureStats() {
        console.log('\n━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━');
        console.log('📊 강의 통계');
        console.log('━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━');
        
        const serial = prompt('강의 코드 (빈칸: 전체 통계):', '');
        
        const data = serial ? { lecSerial: serial } : {};
        console.log(`📤 ${serial ? `강의: ${serial}` : '전체 통계'}`);
        
        const result = await apiCall('/lectures/stats', data);
        
        if (result?.success && result.data) {
            console.log('\n📊 통계 정보:');
            console.log(result.data);
            
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
        console.log('\n🚀 강의 API 전체 테스트 시작');
        console.log('━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n');
        
        const results = { total: 3, success: 0, failed: 0, tests: [] };
        
        try {
            const tests = [
                { name: '강의 목록 조회', fn: testLectureList },
                { name: '강의 상세 조회', fn: testLectureDetail },
                { name: '강의 통계', fn: testLectureStats }
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
    
    window.lectureTest = {
        list: testLectureList,
        search: testLectureSearch,
        detail: testLectureDetail,
        create: testLectureCreate,
        update: testLectureUpdate,
        delete: testLectureDelete,
        stats: testLectureStats,
        runAll: runAllTests
    };
    
    console.log('✅ 강의 API 테스트 로드 완료');
    console.log('💡 사용: await lectureTest.runAll() 또는 개별 함수 실행');
    console.log('━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━');
    
})();
