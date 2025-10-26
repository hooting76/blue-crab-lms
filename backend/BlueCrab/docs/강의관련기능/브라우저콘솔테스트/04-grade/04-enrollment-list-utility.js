/**
 * 📋 수강생 목록 조회 유틸리티
 *
 * 🚀 사용법:
 *    await getEnrolledStudents("ETH201")  // ETH201 강의 수강생 목록 조회
 */

(function() {
    'use strict';

    // ============================================
    // 기본 설정
    // ============================================
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
                body: JSON.stringify(data)
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
    // 수강생 목록 조회 함수
    // ============================================

    /**
     * 강의의 수강생 목록 조회
     * @param {string} lecSerial 강의 코드
     * @param {number} page 페이지 번호 (기본: 0)
     * @param {number} size 페이지 크기 (기본: 50)
     */
    async function getEnrolledStudents(lecSerial, page = 0, size = 50) {
        console.log(`\n📋 강의 수강생 목록 조회`);
        console.log(`   강의: ${lecSerial}, 페이지: ${page}, 크기: ${size}`);
        console.log('━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n');

        const result = await apiCall('/enrollments/list', {
            lecSerial: lecSerial,
            page: page,
            size: size
        });

        if (result?.success && result.data) {
            const enrollments = result.data.content || result.data; // 페이징 객체일 경우 content 사용
            const students = Array.isArray(enrollments) ? enrollments : [];

            console.log(`📊 수강생 목록 (${students.length}명):`);
            console.log('━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━');

            students.forEach((enrollment, index) => {
                const student = enrollment.student || enrollment;
                console.log(`${String(index + 1).padStart(2, ' ')}. ${student.userName || student.name} (ID: ${student.userIdx || student.studentIdx})`);
            });

            console.log('━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━');
            console.log(`✅ 총 ${students.length}명의 수강생을 조회했습니다.`);
            console.log('━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n');

            return {
                success: true,
                students: students,
                total: students.length,
                enrollments: enrollments
            };
        } else {
            console.log('\n❌ 수강생 목록 조회 실패:', result.error);
            console.log('━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n');
            return result;
        }
    }

    /**
     * 강의의 수강생 통계 조회
     * @param {string} lecSerial 강의 코드
     */
    async function getEnrollmentStats(lecSerial) {
        console.log(`\n📊 강의 수강 통계 조회`);
        console.log(`   강의: ${lecSerial}`);
        console.log('━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n');

        const result = await apiCall('/enrollments/list', {
            lecSerial: lecSerial,
            stats: true
        });

        if (result?.success && result.data) {
            const stats = result.data;
            console.log('📈 수강 통계:');
            console.log(`   수강생 수: ${stats.enrollmentCount || stats.totalCount || 'N/A'}`);
            console.log(`   강의 코드: ${stats.lecSerial || lecSerial}`);
            console.log('━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n');

            return { success: true, stats: stats };
        } else {
            console.log('\n❌ 통계 조회 실패:', result.error);
            console.log('━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n');
            return result;
        }
    }

    // 전역 객체로 등록
    window.getEnrolledStudents = getEnrolledStudents;
    window.getEnrollmentStats = getEnrollmentStats;

    console.log('✅ 수강생 목록 조회 유틸리티 로드 완료!');
    console.log('💡 사용법:');
    console.log('   await getEnrolledStudents("ETH201")     // 수강생 목록');
    console.log('   await getEnrollmentStats("ETH201")      // 수강 통계');

})();