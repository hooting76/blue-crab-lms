/**
 * 📝 과제 API 테스트 (독립 실행)
 * 
 * 🚀 사용법:
 *    await assignmentTest.runAll()        // 전체 테스트
 * 
 * 📋 개별 API 테스트:
 *    await assignmentTest.list()          // 과제 목록 조회
 *    await assignmentTest.detail()        // 과제 상세 조회
 *    await assignmentTest.create()        // 과제 생성
 *    await assignmentTest.grade()         // 과제 채점
 *    await assignmentTest.submissions()   // 제출 현황 조회
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
    // 1. 과제 목록 조회
    // POST /api/assignments/list
    // ============================================
    
    async function testAssignmentList() {
        console.log('\n━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━');
        console.log('📋 과제 목록 조회');
        console.log('━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━');
        
        const lecSerial = prompt('강의 코드 (예: ETH201):', 'ETH201');
        if (!lecSerial) {
            console.log('❌ 강의 코드가 필요합니다.');
            return { success: false, error: '강의 코드 미입력' };
        }
        
        const page = prompt('페이지 번호 (기본: 0):', '0');
        const size = prompt('페이지 크기 (기본: 20):', '20');
        
        const data = {
            lecSerial,
            page: parseInt(page) || 0,
            size: parseInt(size) || 20
        };
        
        console.log(`📤 강의: ${lecSerial}, 페이지: ${data.page}`);
        
        const result = await apiCall('/assignments/list', data);
        
        if (result?.success && result.data) {
            const assignments = result.data.content || result.data || [];
            console.log(`\n📊 과제 개수: ${assignments.length}개`);
            
            if (assignments.length > 0) {
                console.log('\n� 과제 목록:');
                assignments.forEach((asn, i) => {
                    // assignmentData가 문자열로 반환되므로 JSON.parse() 필요
                    let assignmentData = null;
                    if (asn.assignmentData && typeof asn.assignmentData === 'string') {
                        try {
                            assignmentData = JSON.parse(asn.assignmentData);
                        } catch (e) {
                            console.error('JSON 파싱 오류:', e);
                        }
                    }
                    
                    const assignment = assignmentData?.assignment || asn;
                    console.log(`  ${i+1}. ${assignment.title || asn.assignTitle || 'N/A'}`);
                    console.log(`     만점: ${assignment.maxScore || asn.assignMaxScore || 'N/A'}점, 마감: ${assignment.dueDate || asn.assignDueDate || 'N/A'}`);
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
    // 2. 과제 상세 조회
    // POST /api/assignments/detail
    // ============================================
    
    async function testAssignmentDetail() {
        console.log('\n━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━');
        console.log('📖 과제 상세 조회');
        console.log('━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━');
        
        const assignmentIdx = prompt('과제 IDX:', '');
        if (!assignmentIdx) {
            console.log('❌ 과제 IDX가 필요합니다.');
            return { success: false, error: '과제 IDX 미입력' };
        }
        
        const data = { assignmentIdx: parseInt(assignmentIdx) };
        console.log(`📤 과제 IDX: ${assignmentIdx}`);
        
        const result = await apiCall('/assignments/detail', data);
        
        if (result?.success && result.data) {
            const asn = result.data;
            
            // assignmentData가 문자열로 반환되므로 JSON.parse() 필요
            let assignmentData = null;
            if (asn.assignmentData && typeof asn.assignmentData === 'string') {
                try {
                    assignmentData = JSON.parse(asn.assignmentData);
                } catch (e) {
                    console.error('JSON 파싱 오류:', e);
                }
            }
            
            const assignment = assignmentData?.assignment || asn;
            
            console.log('\n📊 과제 정보:');
            console.log(`  제목: ${assignment.title || asn.assignTitle || 'N/A'}`);
            console.log(`  내용: ${assignment.description || asn.assignContent || 'N/A'}`);
            console.log(`  만점: ${assignment.maxScore || asn.assignMaxScore || 'N/A'}점`);
            console.log(`  마감일: ${assignment.dueDate || asn.assignDueDate || 'N/A'}`);
            console.log(`  생성일: ${asn.assignCreatedAt || asn.createdAt || 'N/A'}`);
            
            if (asn.assignFiles || assignment.files) {
                console.log(`  첨부파일: ${asn.assignFiles || assignment.files}`);
            }
            
            console.log('\n✅ 성공!');
        } else {
            console.log('\n❌ 실패:', result.error);
        }
        
        console.log('━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n');
        return result;
    }
    
    // ============================================
    // 3. 과제 생성 (교수)
    // POST /api/assignments
    // ============================================
    
    async function testCreateAssignment() {
        console.log('\n━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━');
        console.log('➕ 과제 생성');
        console.log('━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━');
        
        const lecSerial = prompt('강의 코드:', 'ETH201');
        const title = prompt('과제 제목:', '');
        const content = prompt('과제 내용:', '');
        const maxScore = prompt('만점 (기본: 100):', '100');
        const dueDate = prompt('마감일 (yyyyMMdd 형식, 예: 20251231):', '');
        
        if (!lecSerial || !title) {
            console.log('❌ 강의 코드와 제목은 필수입니다.');
            return { success: false, error: '필수 정보 미입력' };
        }
        
        const data = {
            lecSerial,
            title: title,
            description: content || '',
            maxScore: parseInt(maxScore) || 100,
            dueDate: dueDate || null
        };
        
        console.log('📤 과제 정보:', data);
        
        const result = await apiCall('/assignments', data);
        
        if (result?.success) {
            console.log('\n✅ 과제 생성 성공!');
            if (result.data) console.log('📊 결과:', result.data);
        } else {
            console.log('\n❌ 실패:', result.error);
        }
        
        console.log('━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n');
        return result;
    }
    
    // ============================================
    // 4. 과제 제출 (학생)
    // POST /api/assignments/{assignmentIdx}/submit
    // ============================================
    
    async function testSubmitAssignment() {
        console.log('\n━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━');
        console.log('📤 과제 제출');
        console.log('━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━');
        
        const assignmentIdx = prompt('과제 IDX:', '');
        const studentIdx = prompt('학생 IDX:', '');
        const content = prompt('제출 내용:', '');
        
        if (!assignmentIdx || !studentIdx) {
            console.log('❌ 과제 IDX와 학생 IDX가 필요합니다.');
            return { success: false, error: '필수 정보 미입력' };
        }
        
        const data = {
            studentIdx: parseInt(studentIdx),
            submissionContent: content || '',
            submittedAt: new Date().toISOString()
        };
        
        console.log(`📤 과제: ${assignmentIdx}, 학생: ${studentIdx}`);
        
        const result = await apiCall(`/assignments/${assignmentIdx}/submit`, data);
        
        if (result?.success) {
            console.log('\n✅ 과제 제출 성공!');
            if (result.data) console.log('📊 결과:', result.data);
        } else {
            console.log('\n❌ 실패:', result.error);
        }
        
        console.log('━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n');
        return result;
    }
    
    // ============================================
    // 5. 과제 채점 (교수)
    // PUT /api/assignments/{assignmentIdx}/grade
    // ============================================
    
    async function testGradeAssignment() {
        console.log('\n━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━');
        console.log('✏️  과제 채점');
        console.log('━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━');
        
        const assignmentIdx = prompt('과제 IDX:', '');
        const studentIdx = prompt('학생 IDX:', '');
        const score = prompt('점수:', '');
        const feedback = prompt('피드백 (선택):', '');
        
        if (!assignmentIdx || !studentIdx || !score) {
            console.log('❌ 과제 IDX, 학생 IDX, 점수가 필요합니다.');
            return { success: false, error: '필수 정보 미입력' };
        }
        
        const data = {
            studentIdx: parseInt(studentIdx),
            score: parseFloat(score),
            feedback: feedback || ''
        };
        
        console.log(`📤 과제: ${assignmentIdx}, 점수: ${score}점`);
        
        const result = await apiCall(`/assignments/${assignmentIdx}/grade`, data, 'PUT');
        
        if (result?.success) {
            console.log('\n✅ 채점 완료!');
            if (result.data) console.log('📊 결과:', result.data);
        } else {
            console.log('\n❌ 실패:', result.error);
        }
        
        console.log('━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n');
        return result;
    }
    
    // ============================================
    // 6. 제출 현황 조회 (교수)
    // POST /api/assignments/submissions
    // ============================================
    
    async function testSubmissions() {
        console.log('\n━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━');
        console.log('📊 과제 제출 현황');
        console.log('━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━');
        
        const assignmentIdx = prompt('과제 IDX:', '');
        if (!assignmentIdx) {
            console.log('❌ 과제 IDX가 필요합니다.');
            return { success: false, error: '과제 IDX 미입력' };
        }
        
        const data = { assignmentIdx: parseInt(assignmentIdx) };
        console.log(`📤 과제 IDX: ${assignmentIdx}`);
        
        const result = await apiCall('/assignments/submissions', data);
        
        if (result?.success && result.data) {
            const submissions = result.data.submissions || result.data || [];
            console.log(`\n📊 제출 현황: ${submissions.length}건`);
            
            if (submissions.length > 0) {
                console.log('\n📋 제출 목록:');
                submissions.forEach((sub, i) => {
                    console.log(`  ${i+1}. 학생: ${sub.studentName || sub.studentIdx}`);
                    console.log(`     제출일: ${sub.submittedAt || 'N/A'}, 점수: ${sub.score || '미채점'}`);
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
    // 전체 테스트 실행
    // ============================================
    
    async function runAllTests() {
        console.log('\n🚀 과제 API 전체 테스트 시작');
        console.log('━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n');
        
        const results = { total: 3, success: 0, failed: 0, tests: [] };
        
        try {
            const tests = [
                { name: '과제 목록 조회', fn: testAssignmentList },
                { name: '과제 상세 조회', fn: testAssignmentDetail },
                { name: '제출 현황 조회', fn: testSubmissions }
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
    
    window.assignmentTest = {
        list: testAssignmentList,
        detail: testAssignmentDetail,
        create: testCreateAssignment,
        submit: testSubmitAssignment,
        grade: testGradeAssignment,
        submissions: testSubmissions,
        runAll: runAllTests
    };
    
    console.log('✅ 과제 API 테스트 로드 완료');
    console.log('💡 사용: await assignmentTest.runAll() 또는 개별 함수 실행');
    console.log('━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━');
    
})();
