/**
 * 📋 수강생 목록 조회 API 테스트
 * 
 * 🚀 사용법:
 *    enrollmentListTest.setContext({ lecSerial: 'ETH201' })
 *    await enrollmentListTest.list()
 * 
 * 📌 API 정보:
 *    - 엔드포인트: POST /api/enrollments/list
 *    - 요청: { lecSerial, page, size }
 *    - 응답: 페이징된 수강생 목록
 */

(function() {
    'use strict';

    const API_BASE = 'https://bluecrab.chickenkiller.com/BlueCrab-1.0.0/api';

    const context = {
        lecSerial: null
    };

    // ============================================
    // 공통 유틸리티
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
            console.error('🔥 예외:', error.message);
            return { success: false, error: error.message, duration };
        }
    }

    function ensureLectureSet() {
        if (!context.lecSerial) {
            promptLecture();
        }
        return context.lecSerial;
    }

    function promptLecture() {
        const lecSerial = prompt('강의 코드 (예: ETH201):', context.lecSerial || 'ETH201');
        if (lecSerial) {
            context.lecSerial = lecSerial;
            console.log('✅ 강의 설정:', context.lecSerial);
        }
        return context.lecSerial;
    }

    function setContext(next) {
        if (next.lecSerial) context.lecSerial = next.lecSerial;
        console.log('✅ 컨텍스트 업데이트:', context);
        return { ...context };
    }

    // ============================================
    // 수강생 목록 조회
    // POST /api/enrollments/list
    // ============================================

    async function testEnrollmentList() {
        console.log('\n━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━');
        console.log('👥 수강생 목록 조회');
        console.log('━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━');

        const lecSerial = ensureLectureSet();
        if (!lecSerial) {
            return { success: false, error: '강의 코드 미설정' };
        }

        const page = prompt('페이지 번호 (기본: 0):', '0');
        const size = prompt('페이지 크기 (기본: 100):', '100');

        const data = {
            lecSerial,
            page: parseInt(page, 10) || 0,
            size: parseInt(size, 10) || 100
        };

        console.log('📤 요청 데이터:', data);
        const result = await apiCall('/enrollments/list', data);

        if (result?.success && result.data) {
            const pageData = result.data;
            const students = pageData.content || [];
            
            console.log('\n📊 수강생 목록:');
            console.log(`  총 ${pageData.totalElements || students.length}명 (페이지 ${pageData.number || 0}/${pageData.totalPages || 1})`);
            console.log('\n👤 학생 정보:');
            
            students.forEach((student, idx) => {
                console.log(`  ${idx + 1}. [${student.studentIdx}] ${student.studentName || student.userName || 'N/A'}`);
                console.log(`     이메일: ${student.studentEmail || student.userEmail || 'N/A'}`);
                console.log(`     학번: ${student.studentId || 'N/A'}`);
                console.log(`     수강상태: ${student.enrollmentStatus || 'N/A'}`);
                if (student.enrollmentData) {
                    try {
                        const data = typeof student.enrollmentData === 'string' 
                            ? JSON.parse(student.enrollmentData) 
                            : student.enrollmentData;
                        if (data.gradeConfig) {
                            console.log(`     성적구성: 있음`);
                        }
                        if (data.grade) {
                            console.log(`     현재성적: ${data.grade.totalScore || 'N/A'}점 (${data.grade.letterGrade || 'N/A'})`);
                        }
                    } catch (e) {
                        // JSON 파싱 실패 시 무시
                    }
                }
                console.log('');
            });

            console.log('\n✅ 성공!');
            
            // studentIdx 목록 추출하여 콘솔에 출력 (복사하기 쉽게)
            const studentIdxList = students.map(s => s.studentIdx);
            console.log('\n📋 studentIdx 목록 (복사용):');
            console.log(JSON.stringify(studentIdxList));
            
            return { ...result, studentIdxList };
        } else {
            console.log('\n❌ 실패:', result.error);
        }

        console.log('━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n');
        return result;
    }

    // ============================================
    // 전역 노출
    // ============================================

    window.enrollmentListTest = {
        setContext,
        promptLecture,
        list: testEnrollmentList,
        getContext: () => ({ ...context })
    };

    console.log('✅ 수강생 목록 API 테스트 로드 완료');
    console.log('━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━');
    console.log('💡 사용법:');
    console.log('   enrollmentListTest.setContext({ lecSerial: "ETH201" })');
    console.log('   await enrollmentListTest.list()');
    console.log('━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━');
})();
