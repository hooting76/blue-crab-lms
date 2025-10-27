/**
 * 📊 성적 API 테스트 (독립 실행)
 *
 * 🚀 사용법:
 *    gradeTest.setContext({ lecSerial: 'CS284', studentIdx: 6 }) // 기본 컨텍스트 설정
 *    await gradeTest.runAll()                                    // 전체 테스트
 *
 * 📋 개별 API 테스트:
 *    await gradeTest.config()           // 성적 구성 설정
 *    await gradeTest.studentInfo()      // 학생 성적 조회
 *    await gradeTest.professorView()    // 교수용 성적 조회
 *    await gradeTest.gradeList()        // 전체 성적 목록
 *    await gradeTest.manualUpdate()     // 수동 성적 수정
 *    await gradeTest.finalize()         // 최종 등급 배정
 */

(function () {
    'use strict';

    const API_BASE = 'https://bluecrab.chickenkiller.com/BlueCrab-1.0.0/api';

    const context = {
        lecSerial: null,
        studentIdx: null
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
                body: method === 'GET' || method === 'DELETE' ? undefined : JSON.stringify(data)
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

    function ensureStudentSet() {
        if (!context.studentIdx) {
            promptStudent();
        }
        return context.studentIdx;
    }

    function promptLecture() {
        const lecSerial = prompt('강의 코드 (예: CS284):', context.lecSerial || '');
        if (lecSerial) {
            context.lecSerial = lecSerial;
            console.log('✅ 강의 설정:', context.lecSerial);
        }
        return context.lecSerial;
    }

    function promptStudent() {
        const studentIdx = prompt('학생 IDX (USER_IDX):', context.studentIdx || '');
        if (studentIdx) {
            context.studentIdx = parseInt(studentIdx, 10);
            console.log('✅ 학생 설정:', context.studentIdx);
        }
        return context.studentIdx;
    }

    function setContext(next) {
        if (next.lecSerial) context.lecSerial = next.lecSerial;
        if (next.studentIdx) context.studentIdx = next.studentIdx;
        console.log('✅ 컨텍스트 업데이트:', context);
        return { ...context };
    }

    // ============================================
    // 1. 성적 구성 설정
    // POST /api/enrollments/grade-config
    // ============================================

    async function testGradeConfig() {
        console.log('\n━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━');
        console.log('⚙️  성적 구성 설정');
        console.log('━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━');

        const lecSerial = ensureLectureSet();
        if (!lecSerial) {
            return { success: false, error: '강의 코드 미설정' };
        }

        const attendanceMaxScore = prompt('출석 만점 (기본: 20):', '20');
        const assignmentTotalScore = prompt('과제 총점 (기본: 50):', '50');
        const latePenalty = prompt('지각 감점/회 (기본: 0):', '0');

        console.log('\n📊 등급 분포 (합계 100%)');
        const gradeA = prompt('A 비율 (기본: 30):', '30');
        const gradeB = prompt('B 비율 (기본: 40):', '40');
        const gradeC = prompt('C 비율 (기본: 20):', '20');
        const gradeD = prompt('D 비율 (기본: 10):', '10');

        const data = {
            action: 'set-config',
            lecSerial,
            attendanceMaxScore: parseFloat(attendanceMaxScore) || 20,
            assignmentTotalScore: parseFloat(assignmentTotalScore) || 50,
            latePenaltyPerSession: parseFloat(latePenalty) || 0,
            gradeDistribution: {
                A: parseInt(gradeA, 10) || 30,
                B: parseInt(gradeB, 10) || 40,
                C: parseInt(gradeC, 10) || 20,
                D: parseInt(gradeD, 10) || 10
            }
        };

        const totalPercent = Object.values(data.gradeDistribution).reduce((sum, v) => sum + v, 0);
        if (totalPercent !== 100) {
            console.warn(`⚠️  등급 분포 합계가 100%가 아닙니다: ${totalPercent}%`);
        }

        console.log('📤 설정 데이터:', data);
        const result = await apiCall('/enrollments/grade-config', data);

        if (result?.success) {
            console.log('\n✅ 성적 구성 저장 완료!');
            if (result.data) console.log('📊 응답:', result.data);
        } else {
            console.log('\n❌ 실패:', result.error);
        }

        console.log('━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n');
        return result;
    }

    // ============================================
    // 2. 학생 성적 조회
    // POST /api/enrollments/grade-info (action: get-grade)
    // ============================================

    async function testStudentGradeInfo() {
        console.log('\n━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━');
        console.log('🎓 학생 성적 조회');
        console.log('━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━');

        const lecSerial = ensureLectureSet();
        const studentIdx = ensureStudentSet();
        if (!lecSerial || !studentIdx) {
            return { success: false, error: '강의/학생 정보 미설정' };
        }

        const data = {
            action: 'get-grade',
            lecSerial,
            studentIdx
        };

        console.log('📤 요청 데이터:', data);
        const result = await apiCall('/enrollments/grade-info', data);

        if (result?.success && result.data) {
            const info = result.data.data || result.data;
            console.log('\n📊 학생 성적 정보:');
            if (info.gradeConfig) {
                console.log('  ⚙️  성적 구성:', info.gradeConfig);
            }
            if (info.total) {
                console.log(`  💯 총점: ${info.total.score}/${info.total.maxScore} (${(info.total.percentage || 0).toFixed?.(2) || info.total.percentage || 'N/A'}%)`);
            }
            if (info.grade || info.letterGrade) {
                console.log(`  🏆 등급: ${info.letterGrade || info.grade}`);
            }
            if (info.assignments || info.assignmentScores) {
                const assignments = info.assignments || info.assignmentScores;
                console.log(`  📝 과제 ${assignments.length || 0}건`);
            }
            console.log('\n✅ 성공!');
        } else {
            console.log('\n❌ 실패:', result.error);
        }

        console.log('━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n');
        return result;
    }

    // ============================================
    // 3. 교수용 성적 조회
    // POST /api/enrollments/grade-info (action: professor-view)
    // ============================================

    async function testProfessorView() {
        console.log('\n━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━');
        console.log('👨‍🏫 교수용 성적 조회');
        console.log('━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━');

        const lecSerial = ensureLectureSet();
        const studentIdx = ensureStudentSet();
        if (!lecSerial || !studentIdx) {
            return { success: false, error: '강의/학생 정보 미설정' };
        }

        const data = {
            action: 'professor-view',
            lecSerial,
            studentIdx
            // professorIdx는 JWT 토큰에서 자동 추출됩니다
        };

        console.log('📤 요청 데이터:', data);
        console.log('💡 professorIdx는 JWT 토큰에서 자동으로 추출됩니다');
        const result = await apiCall('/enrollments/grade-info', data);

        if (result?.success && result.data) {
            const info = result.data.data || result.data;
            console.log('\n📊 교수용 성적 정보:');
            console.log(`  학생: ${info.studentName || studentIdx}`);
            console.log(`  총점: ${info.totalScore || (info.total?.score ?? 'N/A')}`);
            console.log(`  백분율: ${info.percentage || info.total?.percentage || 'N/A'}`);
            console.log(`  등급: ${info.grade || info.letterGrade || 'N/A'}`);
            if (info.classStats) {
                console.log('  📈 반 통계:', info.classStats);
            }
            console.log('\n✅ 성공!');
        } else {
            console.log('\n❌ 실패:', result.error);
        }

        console.log('━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n');
        return result;
    }

    // ============================================
    // 4. 전체 수강생 성적 목록
    // POST /api/enrollments/grade-list
    // ============================================

    async function testGradeList() {
        console.log('\n━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━');
        console.log('📋 성적 목록 조회');
        console.log('━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━');

        const lecSerial = ensureLectureSet();
        if (!lecSerial) {
            return { success: false, error: '강의 코드 미설정' };
        }

        const page = prompt('페이지 번호 (기본: 0):', '0');
        const size = prompt('페이지 크기 (기본: 20):', '20');
        const sortBy = prompt('정렬 기준 (percentage/name/studentId):', 'percentage');
        const sortOrder = prompt('정렬 순서 (desc/asc):', 'desc');

        const data = {
            action: 'list-all',
            lecSerial,
            page: parseInt(page, 10) || 0,
            size: parseInt(size, 10) || 20,
            sortBy: sortBy || 'percentage',
            sortOrder: sortOrder || 'desc'
        };

        console.log('📤 요청 데이터:', data);
        const result = await apiCall('/enrollments/grade-list', data);

        if (result?.success && result.data) {
            const list = result.data.data || result.data;
            const items = list.content || [];
            console.log(`\n📊 총 ${list.totalElements || items.length || 0}명 (페이지 ${list.number || 0}/${list.totalPages || 1})`);
            items.slice(0, 5).forEach((row, idx) => {
                console.log(`  ${idx + 1}. ${row.studentName || row.studentIdx} - ${row.percentage?.toFixed?.(2) || row.percentage || 'N/A'}% (${row.grade || 'N/A'})`);
            });
            console.log('\n✅ 성공!');
        } else {
            console.log('\n❌ 실패:', result.error);
        }

        console.log('━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n');
        return result;
    }

    // ============================================
    // 5. 성적 수동 수정
    // PUT /api/enrollments/{enrollmentIdx}/grade
    // ============================================

    async function testManualGradeUpdate() {
        console.log('\n━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━');
        console.log('✏️  성적 수동 수정');
        console.log('━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━');

        const enrollmentIdx = prompt('enrollmentIdx:', '');
        const letterGrade = prompt('등급 (예: A0, B+):', '');
        const score = prompt('점수 (예: 85.5):', '');

        if (!enrollmentIdx || !letterGrade || !score) {
            console.log('❌ 모든 정보가 필요합니다.');
            return { success: false, error: '필수 정보 미입력' };
        }

        const data = {
            grade: letterGrade,
            score: parseFloat(score)
        };

        console.log(`📤 enrollmentIdx: ${enrollmentIdx}, 등급: ${letterGrade}, 점수: ${data.score}`);
        const result = await apiCall(`/enrollments/${enrollmentIdx}/grade`, data, 'PUT');

        if (result?.success) {
            console.log('\n✅ 성적 수정 완료!');
            if (result.data) console.log('📊 응답:', result.data);
        } else {
            console.log('\n❌ 실패:', result.error);
        }

        console.log('━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n');
        return result;
    }

    // ============================================
    // 6. 최종 등급 배정
    // POST /api/enrollments/grade-finalize
    // ============================================

    async function testFinalizeGrades() {
        console.log('\n━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━');
        console.log('🏁 최종 등급 배정');
        console.log('━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━');

        const lecSerial = ensureLectureSet();
        if (!lecSerial) {
            return { success: false, error: '강의 코드 미설정' };
        }

        const threshold = prompt('통과 기준 점수 (기본: 60):', '60');
        const useCustomDist = prompt('등급 분포 직접 입력? (yes/no):', 'no');

        const data = {
            action: 'finalize',
            lecSerial,
            passingThreshold: parseFloat(threshold) || 60
        };

        if (useCustomDist.toLowerCase() === 'yes') {
            console.log('\n📊 등급 분포 (합계 100%)');
            data.gradeDistribution = {
                A: parseInt(prompt('A 비율:', '30'), 10) || 30,
                B: parseInt(prompt('B 비율:', '40'), 10) || 40,
                C: parseInt(prompt('C 비율:', '20'), 10) || 20,
                D: parseInt(prompt('D 비율:', '10'), 10) || 10
            };
        }

        console.log('📤 요청 데이터:', data);
        const result = await apiCall('/enrollments/grade-finalize', data);

        if (result?.success && result.data) {
            const stats = result.data.data || result.data;
            console.log('\n📊 등급 배정 결과:', stats);
            console.log('\n✅ 성공!');
        } else {
            console.log('\n❌ 실패:', result.error);
        }

        console.log('━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n');
        return result;
    }

    // ============================================
    // 전체 실행
    // ============================================

    async function runAllTests() {
        console.log('\n🚀 성적 API 전체 테스트 시작');
        console.log('━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n');

        const results = { total: 3, success: 0, failed: 0, tests: [] };

        try {
            const tests = [
                { name: '성적 구성 설정', fn: testGradeConfig },
                { name: '학생 성적 조회', fn: testStudentGradeInfo },
                { name: '성적 목록 조회', fn: testGradeList }
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
                console.log(`  ${t.success ? '✅' : '❌'} ${i + 1}. ${t.name}`);
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

    window.gradeTest = {
        setContext,
        promptLecture,
        promptStudent,
        config: testGradeConfig,
        studentInfo: testStudentGradeInfo,
        professorView: testProfessorView,
        gradeList: testGradeList,
        manualUpdate: testManualGradeUpdate,
        finalize: testFinalizeGrades,
        runAll: runAllTests,
        getContext: () => ({ ...context })
    };

    console.log('✅ 성적 API 테스트 로드 완료');
    console.log('💡 사용: gradeTest.setContext({ lecSerial, studentIdx }); await gradeTest.runAll()');
    console.log('━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━');
})();
