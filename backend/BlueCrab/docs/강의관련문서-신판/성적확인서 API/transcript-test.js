// ===================================================================
// 📄 성적확인서 API 테스트
// Blue Crab LMS - Transcript API
// 
// ⚠️ 사전 준비: 먼저 학생 계정으로 로그인하세요!
// 📁 위치: docs/강의관련기능/브라우저콘솔테스트/00-login/user-login.js
// 📝 실행: await login() (학생 계정 사용)
// 
// 기능:
//    - 학생 본인의 성적확인서 조회
//    - 수료/중도포기/낙제한 강의 목록
//    - 이수학점, 성적(A~F), GPA(4.0 만점) 표시
//    - 학기별/전체 통계 조회
// ===================================================================

const API_BASE_URL = 'https://bluecrab.chickenkiller.com/BlueCrab-1.0.0/api';

// 전역 변수 (user-login.js에서 설정한 토큰 및 사용자 정보 사용)
if (typeof window.authToken === 'undefined') window.authToken = null;
if (typeof window.currentUser === 'undefined') window.currentUser = null;

// ========== 사용자 정보 추출 ==========
function getUserInfo() {
    const user = window.currentUser;
    if (!user) return null;
    
    return {
        userIdx: user.id || user.userIdx || user.userId,
        email: user.email || user.userEmail,
        name: user.name || user.userName,
        role: user.role || user.authority,
        studentCode: user.userCode || user.studentCode
    };
}

// ========== 로그인 상태 확인 ==========
function checkAuth() {
    const token = window.authToken;
    const user = window.currentUser;
    
    if (!token) {
        console.log('\n⚠️ 로그인이 필요합니다!');
        console.log('📁 docs/강의관련기능/브라우저콘솔테스트/00-login/user-login.js → await login()');
        return false;
    }
    
    if (!user || !user.id) {
        console.log('\n⚠️ 사용자 정보가 없습니다!');
        console.log('📁 다시 로그인해주세요: await login()');
        return false;
    }
    
    console.log('✅ 로그인 확인됨:', {
        userIdx: user.id,
        email: user.email,
        name: user.name
    });
    
    return true;
}

// ========== 성적확인서 조회 ==========
async function viewTranscript(studentIdx = null) {
    if (!checkAuth()) return;
    
    const token = window.authToken;
    const userInfo = getUserInfo();
    
    if (!userInfo || !userInfo.userIdx) {
        console.log('❌ 사용자 정보를 불러올 수 없습니다. 다시 로그인해주세요.');
        return;
    }
    
    // studentIdx가 없으면 본인 IDX 사용
    const targetStudentIdx = studentIdx || userInfo.userIdx;
    
    console.log('\n📄 성적확인서 조회 - POST /api/transcript/view');
    console.log('═══════════════════════════════════════════════════════');
    console.log('📊 조회 대상 학생 IDX:', targetStudentIdx);
    
    try {
        const requestBody = {
            studentIdx: targetStudentIdx
        };
        
        console.log('📤 요청 데이터:', JSON.stringify(requestBody, null, 2));
        
        const response = await fetch(`${API_BASE_URL}/transcript/view`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                'Authorization': `Bearer ${token}`
            },
            body: JSON.stringify(requestBody)
        });
        
        const result = await response.json();
        
        console.log('📥 HTTP Status:', response.status, response.statusText);
        console.log('📥 응답:', result);
        
        if (result.status === 'success' && result.data) {
            console.log('\n✅ 성적확인서 조회 성공!');
            displayTranscript(result.data);
        } else {
            console.log('\n❌ 성적확인서 조회 실패');
            console.log('메시지:', result.message);
        }
        
        return result;
        
    } catch (error) {
        console.error('❌ 네트워크 오류:', error);
        return null;
    }
}

// ========== 헬스 체크 ==========
async function healthCheck() {
    console.log('\n🏥 Transcript API 헬스 체크 - GET /api/transcript/health');
    console.log('═══════════════════════════════════════════════════════');
    
    try {
        const response = await fetch(`${API_BASE_URL}/transcript/health`, {
            method: 'GET',
            headers: {
                'Content-Type': 'application/json'
            }
        });
        
        const result = await response.json();
        
        console.log('📥 HTTP Status:', response.status, response.statusText);
        console.log('📥 응답:', result);
        
        if (result.status === 'success') {
            console.log('✅ API 정상 작동 중');
        } else {
            console.log('⚠️ API 응답 이상');
        }
        
        return result;
        
    } catch (error) {
        console.error('❌ 네트워크 오류:', error);
        return null;
    }
}

// ========== 타인 성적 조회 테스트 (권한 확인) ==========
async function testUnauthorizedAccess() {
    console.log('\n🚫 타인 성적 조회 테스트 (권한 확인)');
    console.log('═══════════════════════════════════════════════════════');
    console.log('⚠️ 403 Forbidden 응답이 나오면 정상입니다.');
    
    // 존재하지 않는 학생 IDX로 시도
    await viewTranscript(99999);
}

// ========== 성적확인서 내용 출력 ==========
function displayTranscript(transcript) {
    console.log('\n━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━');
    console.log('                    성 적 확 인 서');
    console.log('━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n');
    
    // 학생 정보
    console.log('【 학생 정보 】');
    console.log(`  학    번: ${transcript.student.studentCode || 'N/A'}`);
    console.log(`  성    명: ${transcript.student.name || 'N/A'}`);
    console.log(`  입학년도: ${transcript.student.admissionYear || 'N/A'}`);
    console.log(`  현재학년: ${transcript.student.currentGrade ? transcript.student.currentGrade + '학년' : 'N/A'}`);
    console.log();
    
    // 전체 요약
    console.log('【 전체 요약 】');
    const overall = transcript.overallSummary;
    console.log(`  총 수강 과목:  ${overall.totalCourses}개`);
    console.log(`  취득 학점:     ${overall.totalEarnedCredits} / ${overall.totalAttemptedCredits}`);
    console.log(`  누적 평점:     ${overall.cumulativeGpa.toFixed(2)} / 4.0`);
    console.log(`  평균 백분율:   ${overall.averagePercentage.toFixed(2)}%`);
    console.log(`  학점 취득률:   ${overall.completionRate.toFixed(2)}%`);
    console.log(`  학점 분포:     A(${overall.totalGradeACount}) B(${overall.totalGradeBCount}) C(${overall.totalGradeCCount}) D(${overall.totalGradeDCount}) F(${overall.totalGradeFCount})`);
    console.log();
    
    // 학기별 요약
    if (Object.keys(transcript.semesterSummaries).length > 0) {
        console.log('【 학기별 요약 】');
        Object.entries(transcript.semesterSummaries)
            .sort(([a], [b]) => a.localeCompare(b))
            .forEach(([key, summary]) => {
                console.log(`\n  📚 ${summary.year}학년도 ${summary.semester}학기`);
                console.log(`     과목 수:     ${summary.courseCount}개`);
                console.log(`     취득 학점:   ${summary.earnedCredits} / ${summary.attemptedCredits}`);
                console.log(`     학기 평점:   ${summary.semesterGpa.toFixed(2)} / 4.0`);
                console.log(`     평균 백분율: ${summary.averagePercentage.toFixed(2)}%`);
                console.log(`     학점 분포:   A(${summary.gradeACount}) B(${summary.gradeBCount}) C(${summary.gradeCCount}) D(${summary.gradeDCount}) F(${summary.gradeFCount})`);
            });
        console.log();
    }
    
    // 과목 목록 (테이블 형식)
    if (transcript.courses && transcript.courses.length > 0) {
        console.log('【 수강 내역 】\n');
        
        console.table(
            transcript.courses.map(course => ({
                '학기': course.year && course.semester ? `${course.year}-${course.semester}` : 'N/A',
                '과목명': course.lecTitle || 'N/A',
                '교수': course.professorName || 'N/A',
                '학점': course.credits || 0,
                '성적': course.letterGrade || 'N/A',
                'GPA': course.gpa ? course.gpa.toFixed(2) : '0.00',
                '백분율': course.percentage ? course.percentage.toFixed(2) + '%' : '0.00%',
                '등수': course.rank && course.totalStudents ? `${course.rank}/${course.totalStudents}` : '-',
                '상태': getStatusDisplay(course.status)
            }))
        );
    }
    
    // 발급 정보
    console.log('\n【 발급 정보 】');
    console.log(`  발급일시: ${formatDateTime(transcript.issuedAt)}`);
    console.log(`  발급번호: ${transcript.certificateNumber}`);
    console.log();
    console.log('━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n');
}

// ========== 상태 표시 문자열 ==========
function getStatusDisplay(status) {
    const statusMap = {
        'COMPLETED': '✅ 수료',
        'FAILED': '❌ 낙제',
        'IN_PROGRESS': '⏳ 진행중',
        'NOT_GRADED': '⏸️ 미확정',
        'DROPPED': '🚫 중도포기'
    };
    return statusMap[status] || status;
}

// ========== 날짜시간 포맷팅 ==========
function formatDateTime(dateTimeStr) {
    if (!dateTimeStr) return 'N/A';
    const date = new Date(dateTimeStr);
    return date.toLocaleString('ko-KR', {
        year: 'numeric',
        month: '2-digit',
        day: '2-digit',
        hour: '2-digit',
        minute: '2-digit',
        second: '2-digit'
    });
}

// ========== 간단한 통계 요약 ==========
function showSummary() {
    if (!checkAuth()) return;
    
    console.log('\n📊 성적확인서 간단 통계 조회');
    console.log('═══════════════════════════════════════════════════════');
    console.log('💡 전체 성적확인서를 조회하려면: await viewTranscript()');
}

// ========== 사용법 안내 ==========
console.log(`
📄 성적확인서 API 테스트
═══════════════════════════════════════════════════════

✅ 사용 방법:
1. await viewTranscript()          - 본인 성적확인서 조회
2. await healthCheck()              - API 헬스 체크
3. await testUnauthorizedAccess()   - 권한 테스트 (403 확인)
4. showSummary()                    - 사용법 다시 보기

📊 성적확인서 내용:
- 학생 기본 정보 (학번, 이름, 입학년도, 현재학년)
- 전체 요약 (총 과목 수, 취득/신청 학점, 누적 GPA, 평균 백분율, 학점 취득률)
- 학기별 요약 (각 학기별 통계)
- 수강 내역 (모든 과목의 상세 정보)
- 발급 정보 (발급일시, 발급번호)

📌 과목 상태:
✅ 수료      - 성적 확정, F가 아닌 등급
❌ 낙제      - 성적 확정, F 등급
⏳ 진행중    - 성적 미확정, 60% 이상
⏸️ 미확정   - 성적 미확정, 60% 미만
🚫 중도포기  - 수강 포기

💡 예시:
await viewTranscript();  // 본인 성적확인서 조회

⚠️ 주의사항:
- 반드시 로그인 후 사용하세요 (await login())
- 본인의 성적만 조회 가능합니다
- 타인 성적 조회 시 403 Forbidden 응답

🔗 관련 파일:
- 로그인: docs/강의관련기능/브라우저콘솔테스트/00-login/user-login.js
- 설계 문서: docs/강의관련문서-신판/성적확인서 API/설계문서.md
`);
