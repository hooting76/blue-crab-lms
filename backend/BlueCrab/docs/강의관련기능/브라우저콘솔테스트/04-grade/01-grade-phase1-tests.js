/**
 * 📊 Phase 1: 성적 관리 테스트 (독립 실행)
 * 
 * 🚀 사용법:
 *    gradePhase1.setLecture('ETH201')         // 강의 지정
 *    await gradePhase1.runAll()               // 전체 테스트 (5개)
 * 
 * ⚙️  로컬 설정 변경 (서버 전송 전):
 *    gradePhase1.quickAttendanceConfig(80, 0.5)  // 출석 만점, 지각 페널티 변경
 *    gradePhase1.promptConfig()                  // 대화형 입력으로 변경
 *    gradePhase1.updateConfig({...})             // 객체로 직접 변경
 *    gradePhase1.getConfig()                     // 현재 로컬 설정 조회
 * 
 * 📋 개별 API 테스트:
 *    gradePhase1.config()        // 로컬 설정을 서버에 저장
 *    gradePhase1.studentInfo()   // 학생 성적 조회
 *    gradePhase1.professorView() // 교수용 성적 조회
 *    gradePhase1.gradeList()     // 성적 목록 조회
 *    gradePhase1.finalize()      // 최종 등급 배정
 */

(function() {
    'use strict';
    
    // ============================================
    // 기본 설정
    // ============================================
    const API_BASE = 'https://bluecrab.chickenkiller.com/BlueCrab-1.0.0/api';
    
    // 테스트 설정 (lecSerial + studentIdx 방식)
    const config = {
        lecSerial: null,  // 프롬프트 또는 setLecture()로 설정 (강의 코드, 예: "ETH201")
        studentIdx: null,  // USER_IDX
        passingThreshold: 60.0,
        attendanceMaxScore: 80,
        assignmentTotalMaxScore: 100,  // 🔒 읽기 전용 - 과제 생성 시 자동 누적 (서버에서 계산)
        latePenaltyPerSession: 0.5,
        gradeDistribution: {
            "A": 30,  // 상위 30%
            "B": 40,  // 30~70%
            "C": 20,  // 70~90%
            "D": 10   // 90~100%
        }
    };
    
    // ============================================
    // 유틸리티 함수
    // ============================================
    
    function getToken() {
        return window.authToken || localStorage.getItem('jwtAccessToken');
    }
    
    // 강의 코드 검증 및 설정 (중복 제거용)
    function ensureLectureSet() {
        if (!config.lecSerial) {
            console.warn('⚠️  강의 코드 미설정!');
            promptLecture();
        }
        return config.lecSerial;
    }
    
    // 학생 IDX 검증 및 설정 (중복 제거용)
    function ensureStudentSet() {
        if (!config.studentIdx) {
            console.warn('⚠️  학생 IDX 미설정!');
            promptStudent();
        }
        return config.studentIdx;
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
    // 설정 함수
    // ============================================
    
    function setLecture(lecSerial, studentIdx = null) {
        config.lecSerial = lecSerial;
        if (studentIdx !== null) config.studentIdx = studentIdx;
        console.log('✅ 설정 완료:', { lecSerial: config.lecSerial, studentIdx: config.studentIdx });
        return config;
    }
    
    function promptLecture() {
        const lecSerial = prompt('강의 코드 (예: ETH201):', config.lecSerial || '');
        if (lecSerial) config.lecSerial = lecSerial;
        console.log('✅ 강의 설정:', { lecSerial: config.lecSerial });
        return config;
    }
    
    function promptStudent() {
        const studentIdx = prompt('학생 IDX (USER_IDX):', config.studentIdx || '');
        if (studentIdx) config.studentIdx = parseInt(studentIdx);
        console.log('✅ 학생 설정:', { studentIdx: config.studentIdx });
        return config;
    }
    
    function promptConfig() {
        console.log('\n⚙️  성적 구성 설정 입력');
        console.log('━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━');
        console.log('💡 과제 총점은 과제 생성 시 자동 누적되므로 설정 불필요');
        console.log('');
        
        const attendance = prompt('출석 만점 (기본: 80):', config.attendanceMaxScore);
        const latePenalty = prompt('지각 감점/회 (기본: 0.5):', config.latePenaltyPerSession);
        
        console.log('\n📊 등급 분포 설정 (합계 100%)');
        const gradeA = prompt('A 등급 비율 (기본: 30%):', config.gradeDistribution.A);
        const gradeB = prompt('B 등급 비율 (기본: 40%):', config.gradeDistribution.B);
        const gradeC = prompt('C 등급 비율 (기본: 20%):', config.gradeDistribution.C);
        const gradeD = prompt('D 등급 비율 (기본: 10%):', config.gradeDistribution.D);
        
        if (attendance) config.attendanceMaxScore = parseInt(attendance);
        if (latePenalty) config.latePenaltyPerSession = parseFloat(latePenalty);
        
        if (gradeA || gradeB || gradeC || gradeD) {
            config.gradeDistribution = {
                A: gradeA ? parseInt(gradeA) : config.gradeDistribution.A,
                B: gradeB ? parseInt(gradeB) : config.gradeDistribution.B,
                C: gradeC ? parseInt(gradeC) : config.gradeDistribution.C,
                D: gradeD ? parseInt(gradeD) : config.gradeDistribution.D
            };
            
            // 합계 검증
            const total = config.gradeDistribution.A + config.gradeDistribution.B + 
                         config.gradeDistribution.C + config.gradeDistribution.D;
            if (total !== 100) {
                console.warn(`⚠️  등급 분포 합계가 100%가 아닙니다: ${total}%`);
            }
        }
        
        console.log('\n✅ 성적 구성 업데이트:');
        console.log(`   출석 만점: ${config.attendanceMaxScore}점`);
        console.log(`   지각 페널티: ${config.latePenaltyPerSession}점/회`);
        console.log(`   등급 분포:`, config.gradeDistribution);
        console.log('');
        console.log('💡 과제 총점은 서버에서 자동 계산됩니다.');
        console.log('   최종 점수 = (출석율 × 출석만점) + (과제점수합계)');
        
        return config;
    }
    
    function updateConfig(updates) {
        if (updates.attendanceMaxScore !== undefined) {
            config.attendanceMaxScore = updates.attendanceMaxScore;
        }
        if (updates.assignmentTotalMaxScore !== undefined) {
            console.warn('⚠️  assignmentTotalMaxScore는 과제 생성 시 자동 누적됩니다.');
            console.warn('   이 값은 참고용이며 서버로 전송되지 않습니다.');
            config.assignmentTotalMaxScore = updates.assignmentTotalMaxScore;
        }
        if (updates.latePenaltyPerSession !== undefined) {
            config.latePenaltyPerSession = updates.latePenaltyPerSession;
        }
        if (updates.gradeDistribution) {
            config.gradeDistribution = { ...config.gradeDistribution, ...updates.gradeDistribution };
        }
        
        console.log('✅ 설정 업데이트:', config);
        return config;
    }
    
    // 출석 관련 설정만 빠르게 변경 (가장 자주 수정하는 항목)
    function quickAttendanceConfig(maxScore = null, latePenalty = null) {
        if (maxScore !== null) config.attendanceMaxScore = maxScore;
        if (latePenalty !== null) config.latePenaltyPerSession = latePenalty;
        
        console.log('✅ 출석 설정 업데이트:');
        console.log(`   - 출석 만점: ${config.attendanceMaxScore}점`);
        console.log(`   - 지각 페널티: ${config.latePenaltyPerSession}점/회`);
        console.log('');
        console.log('💡 서버에 반영하려면: await gradePhase1.config()');
        
        return config;
    }
    
    // ============================================
    // 서버 설정 직접 조회/수정 (개선된 방식)
    // ============================================
    
    /**
     * 서버에 저장된 현재 설정 조회
     */
    async function getServerConfig() {
        console.log('\n━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━');
        console.log('📥 서버 설정 조회');
        console.log('━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━');
        
        ensureLectureSet();
        ensureStudentSet();
        
        console.log(`📤 강의: ${config.lecSerial}, 학생: ${config.studentIdx}`);
        
        const result = await apiCall('/enrollments/grade-info', {
            action: 'get-grade',
            lecSerial: config.lecSerial,
            studentIdx: config.studentIdx
        });
        
        if (result?.success && result.data) {
            const d = result.data.data || result.data;
            const serverConfig = d.gradeConfig;
            
            if (serverConfig) {
                console.log('\n📊 서버 설정:');
                console.log(`   출석 만점: ${serverConfig.attendanceMaxScore || 'N/A'}점`);
                console.log(`   과제 총점: ${serverConfig.assignmentTotalScore || 'N/A'}점`);
                console.log(`   시험 총점: ${serverConfig.examTotalScore || 'N/A'}점`);
                console.log(`   지각 페널티: ${serverConfig.latePenaltyPerSession || 0}점/회`);
                console.log(`   총 만점: ${serverConfig.totalMaxScore || 'N/A'}점`);
                
                if (serverConfig.gradeDistribution) {
                    console.log(`   등급 분포:`, serverConfig.gradeDistribution);
                }
                
                if (serverConfig.configuredAt) {
                    console.log(`   설정일시: ${serverConfig.configuredAt}`);
                }
                
                // 로컬 설정과 비교
                console.log('\n🔍 로컬 vs 서버 비교:');
                if (config.attendanceMaxScore !== serverConfig.attendanceMaxScore) {
                    console.log(`   ⚠️  출석 만점: 로컬(${config.attendanceMaxScore}) ≠ 서버(${serverConfig.attendanceMaxScore})`);
                }
                if (config.latePenaltyPerSession !== serverConfig.latePenaltyPerSession) {
                    console.log(`   ⚠️  지각 페널티: 로컬(${config.latePenaltyPerSession}) ≠ 서버(${serverConfig.latePenaltyPerSession || 0})`);
                }
                
                console.log('\n✅ 성공!');
                console.log('━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n');
                return { success: true, data: serverConfig };
            } else {
                console.log('\n⚠️  서버에 설정이 없습니다. 먼저 설정을 저장하세요.');
                console.log('   예: await gradePhase1.config()');
                console.log('━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n');
                return { success: false, error: '서버 설정 없음' };
            }
        } else {
            console.log('\n❌ 실패:', result.error);
            console.log('━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n');
            return result;
        }
    }
    
    /**
     * 프롬프트로 입력받아 즉시 서버에 저장 (간편 버전)
     */
    async function quickConfig() {
        console.log('\n━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━');
        console.log('⚡ 빠른 설정 (즉시 서버 저장)');
        console.log('━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━');
        
        ensureLectureSet();
        
        console.log(`📚 강의: ${config.lecSerial}`);
        console.log('\n💡 빈칸으로 두면 기본값 사용\n');
        
        const attendance = prompt('출석 만점 (기본: 80):', '');
        const latePenalty = prompt('지각 감점/회 (기본: 0.5):', '');
        
        // 로컬 config 업데이트
        if (attendance) config.attendanceMaxScore = parseInt(attendance);
        if (latePenalty) config.latePenaltyPerSession = parseFloat(latePenalty);
        
        console.log('\n📤 서버 저장 중...');
        console.log(`   출석 만점: ${config.attendanceMaxScore}점`);
        console.log(`   지각 페널티: ${config.latePenaltyPerSession}점/회`);
        
        // 즉시 서버 저장
        const result = await testGradeConfig();
        
        if (result?.success) {
            console.log('⚡ 빠른 설정 완료! (로컬 → 서버 자동 저장)');
        }
        
        return result;
    }
    
    /**
     * 서버 설정 직접 수정 (프롬프트 없이)
     */
    async function updateServerConfig(updates) {
        console.log('\n━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━');
        console.log('📤 서버 설정 직접 수정');
        console.log('━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━');
        
        ensureLectureSet();
        
        // 로컬 config 업데이트
        if (updates.attendanceMaxScore !== undefined) {
            config.attendanceMaxScore = updates.attendanceMaxScore;
        }
        if (updates.latePenaltyPerSession !== undefined) {
            config.latePenaltyPerSession = updates.latePenaltyPerSession;
        }
        if (updates.gradeDistribution) {
            config.gradeDistribution = { ...config.gradeDistribution, ...updates.gradeDistribution };
        }
        
        console.log('📊 수정 내용:');
        if (updates.attendanceMaxScore !== undefined) {
            console.log(`   출석 만점: ${updates.attendanceMaxScore}점`);
        }
        if (updates.latePenaltyPerSession !== undefined) {
            console.log(`   지각 페널티: ${updates.latePenaltyPerSession}점/회`);
        }
        if (updates.gradeDistribution) {
            console.log(`   등급 분포:`, updates.gradeDistribution);
        }
        
        console.log('\n📤 서버 저장 중...');
        
        // 즉시 서버 저장
        const result = await testGradeConfig();
        
        if (result?.success) {
            console.log('\n✅ 서버 설정 수정 완료!');
        }
        
        return result;
    }
    
    // ============================================
    // 1. 성적 구성 설정을 서버에 저장
    // POST /enrollments/grade-config
    // ============================================
    
    async function testGradeConfig() {
        console.log('\n━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━');
        console.log('⚙️  성적 구성 설정 → 서버 저장');
        console.log('━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━');
        
        ensureLectureSet();
        
        const data = {
            action: 'set-config',
            lecSerial: config.lecSerial,
            attendanceMaxScore: config.attendanceMaxScore,
            // assignmentTotalMaxScore 제외 - 과제 생성 시 자동 누적
            latePenaltyPerSession: config.latePenaltyPerSession,
            gradeDistribution: config.gradeDistribution
        };
        
        console.log(`📤 강의 코드: ${config.lecSerial}`);
        console.log(`   출석 만점: ${data.attendanceMaxScore}점`);
        console.log(`   지각 페널티: ${data.latePenaltyPerSession}점/회`);
        console.log(`   등급 분포:`, data.gradeDistribution);
        console.log('');
        console.log('💡 과제 총점은 서버에서 자동 계산 (과제 생성 시 누적)');
        
        const result = await apiCall(`/enrollments/grade-config`, data);
        
        if (result?.success) {
            console.log('\n✅ 성공!');
            if (result.data) console.log('📊 결과:', result.data);
        } else {
            console.log('\n❌ 실패:', result.error);
        }
        
        console.log('━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n');
        return result;
    }
    
    // ============================================
    // 2. 학생 성적 조회
    // POST /enrollments/grade-info (action: get-grade)
    // ============================================
    
    async function testStudentGradeInfo() {
        console.log('\n━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━');
        console.log('📊 학생 성적 조회');
        console.log('━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━');
        
        ensureLectureSet();
        ensureStudentSet();
        
        console.log(`📤 강의 코드: ${config.lecSerial}`);
        console.log(`   학생 IDX: ${config.studentIdx}`);
        
        const data = {
            action: 'get-grade',
            lecSerial: config.lecSerial,
            studentIdx: config.studentIdx
        };
        
        const result = await apiCall(`/enrollments/grade-info`, data);
        
        if (result?.success && result.data) {
            const d = result.data.data || result.data;
            console.log('\n📊 성적 정보:');
            
            // gradeConfig 표시
            if (d.gradeConfig) {
                console.log('  ⚙️  성적 구성:');
                console.log(`    - 출석 만점: ${d.gradeConfig.attendanceMaxScore || 'N/A'}점`);
                console.log(`    - 과제 총점: ${d.gradeConfig.assignmentTotalScore || 'N/A'}점`);
                console.log(`    - 시험 총점: ${d.gradeConfig.examTotalScore || 'N/A'}점`);
                console.log(`    - 지각 페널티: ${d.gradeConfig.latePenaltyPerSession || 0}점/회`);
            }
            
            // attendance 처리
            if (d.attendance || d.attendanceScore !== undefined) {
                console.log('  📅 출석:');
                
                if (d.attendance && typeof d.attendance === 'object') {
                    // attendance 객체인 경우
                    console.log(`    - 현재 점수: ${d.attendance.currentScore?.toFixed(2) || 'N/A'}`);
                    console.log(`    - 백분율: ${d.attendance.percentage?.toFixed(2) || 'N/A'}%`);
                    if (d.attendance.presentCount !== undefined) console.log(`    - 출석: ${d.attendance.presentCount}회`);
                    if (d.attendance.lateCount !== undefined) console.log(`    - 지각: ${d.attendance.lateCount}회`);
                    if (d.attendance.absentCount !== undefined) console.log(`    - 결석: ${d.attendance.absentCount}회`);
                    if (d.attendance.latePenalty) console.log(`    - 지각 감점: ${d.attendance.latePenalty.toFixed(2)}`);
                } else {
                    // 개별 필드인 경우
                    console.log(`    - 출석: ${d.presentCount || 0}회`);
                    console.log(`    - 지각: ${d.lateCount || 0}회`);
                    console.log(`    - 결석: ${d.absentCount || 0}회`);
                    if (d.attendanceRate !== undefined) console.log(`    - 출석율: ${d.attendanceRate.toFixed(2)}%`);
                    if (d.attendanceScore !== undefined) console.log(`    - 점수: ${d.attendanceScore.toFixed(2)}`);
                    if (d.latePenalty) console.log(`    - 지각 감점: ${d.latePenalty.toFixed(2)}`);
                }
            }
            
            if (d.assignmentScores?.length || (d.assignments && Array.isArray(d.assignments))) {
                const assignments = d.assignmentScores || d.assignments;
                console.log(`  📝 과제: ${assignments.length}개`);
                assignments.forEach((a, i) => {
                    console.log(`    ${i+1}. ${a.name || a.assignmentName}: ${a.score}/${a.maxScore} (${a.percentage?.toFixed(2) || 'N/A'}%)`);
                });
            }
            
            // total 처리
            if (d.total || d.totalScore !== undefined) {
                if (d.total && typeof d.total === 'object') {
                    console.log(`  💯 총점: ${d.total.score?.toFixed(2) || 'N/A'}/${d.total.maxScore || 'N/A'} (${d.total.percentage?.toFixed(2) || 'N/A'}%)`);
                } else if (d.totalScore !== undefined) {
                    console.log(`  💯 총점: ${d.totalScore.toFixed(2)} (${d.percentage?.toFixed(2) || 'N/A'}%)`);
                }
            }
            
            // grade 처리 (객체인 경우 letterGrade 필드 확인)
            if (d.letterGrade) {
                console.log(`  🏆 등급: ${d.letterGrade}`);
            } else if (d.grade) {
                if (typeof d.grade === 'string') {
                    console.log(`  🏆 등급: ${d.grade}`);
                } else if (typeof d.grade === 'object' && d.grade.letterGrade) {
                    console.log(`  🏆 등급: ${d.grade.letterGrade}`);
                } else {
                    console.log(`  🏆 등급: ${JSON.stringify(d.grade)}`);
                }
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
    // POST /enrollments/grade-info (action: professor-view)
    // ============================================
    
    async function testProfessorGradeView() {
        console.log('\n━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━');
        console.log('👨‍🏫 교수용 성적 조회');
        console.log('━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━');
        
        ensureLectureSet();
        ensureStudentSet();
        
        const data = {
            action: 'professor-view',
            lecSerial: config.lecSerial,
            studentIdx: config.studentIdx
        };
        
        const result = await apiCall(`/enrollments/grade-info`, data);
        
        if (result?.success && result.data) {
            const d = result.data.data || result.data;
            console.log('\n📊 교수용 정보:');
            
            if (d.studentName) console.log(`  👤 학생: ${d.studentName}`);
            if (d.totalScore !== undefined) {
                console.log(`  💯 총점: ${d.totalScore.toFixed(2)} (${d.percentage.toFixed(2)}%)`);
            }
            if (d.grade) console.log(`  🏆 등급: ${d.grade}`);
            
            if (d.classStats) {
                console.log(`\n  📈 반 통계:`);
                console.log(`    - 평균: ${d.classStats.average?.toFixed(2)}`);
                console.log(`    - 최고: ${d.classStats.max?.toFixed(2)}`);
                console.log(`    - 최저: ${d.classStats.min?.toFixed(2)}`);
                console.log(`    - 순위: ${d.classStats.rank}`);
            }
            
            console.log('\n✅ 성공!');
        } else {
            console.log('\n❌ 실패:', result.error);
        }
        
        console.log('━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n');
        return result;
    }
    
    // ============================================
    // 4. 성적 목록 조회
    // POST /enrollments/grade-list (action: list-all)
    // ============================================
    
    async function testGradeList() {
        console.log('\n━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━');
        console.log('📋 성적 목록 조회');
        console.log('━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━');
        
        ensureLectureSet();
        
        const data = {
            action: 'list-all',
            lecSerial: config.lecSerial,
            page: 0,
            size: 20,
            sortBy: 'percentage',
            sortOrder: 'desc'
        };
        
        const result = await apiCall(`/enrollments/grade-list`, data);
        
        if (result?.success && result.data) {
            const d = result.data.data || result.data;
            console.log('\n📊 조회 결과:');
            console.log(`  - 총 학생: ${d.totalElements || d.content?.length || 0}명`);
            console.log(`  - 페이지: ${d.number || 0}/${d.totalPages || 1}`);
            
            if (d.content?.length) {
                console.log('\n  📊 상위 5명:');
                d.content.slice(0, 5).forEach((s, i) => {
                    console.log(`    ${i+1}. ${s.studentName || 'N/A'} - ${s.percentage?.toFixed(2) || '0.00'}% (${s.grade || 'N/A'})`);
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
    // 5. 최종 등급 배정
    // POST /enrollments/grade-finalize (action: finalize)
    // ============================================
    
    async function testGradeFinalize() {
        console.log('\n━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━');
        console.log('🏆 최종 등급 배정');
        console.log('━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━');
        
        ensureLectureSet();
        
        const data = {
            action: 'finalize',
            lecSerial: config.lecSerial,
            passingThreshold: config.passingThreshold,
            gradeDistribution: config.gradeDistribution
        };
        
        console.log(`📤 합격 기준: ${data.passingThreshold}%`);
        console.log(`   등급 분포:`, data.gradeDistribution);
        
        const result = await apiCall(`/enrollments/grade-finalize`, data);
        
        if (result?.success && result.data) {
            const d = result.data.data || result.data;
            console.log('\n📊 등급 배정 결과:');
            
            if (d.gradeStats) {
                console.log('  📈 등급별 인원:');
                Object.entries(d.gradeStats).forEach(([grade, count]) => {
                    console.log(`    ${grade}: ${count}명`);
                });
            }
            
            if (d.totalStudents !== undefined) console.log(`\n  👥 총 학생: ${d.totalStudents}명`);
            if (d.passingStudents !== undefined) console.log(`  ✅ 합격: ${d.passingStudents}명`);
            if (d.failingStudents !== undefined) console.log(`  ❌ 낙제: ${d.failingStudents}명`);
            if (d.averageScore !== undefined) console.log(`  📊 평균: ${d.averageScore.toFixed(2)}%`);
            
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
        console.log('\n🚀 Phase 1 전체 테스트 시작');
        console.log('━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n');
        
        const results = { total: 5, success: 0, failed: 0, tests: [] };
        
        try {
            const tests = [
                { name: '설정 서버 저장', fn: testGradeConfig },
                { name: '학생 성적 조회', fn: testStudentGradeInfo },
                { name: '교수용 조회', fn: testProfessorGradeView },
                { name: '성적 목록 조회', fn: testGradeList },
                { name: '최종 등급 배정', fn: testGradeFinalize }
            ];
            
            for (const test of tests) {
                const r = await test.fn();
                results.tests.push({ name: test.name, success: r?.success || false });
                if (r?.success) results.success++; else results.failed++;
            }
            
            console.log('\n━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━');
            console.log('📊 Phase 1 결과');
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
    
    window.gradePhase1 = {
        // 설정
        setLecture,
        promptLecture,
        promptStudent,
        promptConfig,
        updateConfig,
        quickAttendanceConfig,  // 출석 설정 빠른 변경 (로컬만)
        getConfig: () => config,
        
        // ⚡ 개선된 서버 설정 관리
        getServerConfig,        // 서버 설정 조회
        updateServerConfig,     // 서버 설정 직접 수정 (즉시 저장)
        quickConfig,            // 프롬프트 입력 → 즉시 서버 저장
        
        // 개별 테스트
        config: testGradeConfig,
        studentInfo: testStudentGradeInfo,
        professorView: testProfessorGradeView,
        gradeList: testGradeList,
        finalize: testGradeFinalize,
        
        // 전체 실행
        runAll: runAllTests
    };
    
    console.log('✅ Phase 1 테스트 로드 완료');
    console.log('💡 상단 주석 참고: gradePhase1.setLecture("ETH201"); await gradePhase1.runAll()');
    console.log('━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━');
    
})();
