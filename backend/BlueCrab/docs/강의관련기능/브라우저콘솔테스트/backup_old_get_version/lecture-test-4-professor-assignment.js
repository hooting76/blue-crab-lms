// ===================================================================
// 📄 교수 과제 관리 테스트
// Blue Crab LMS - 교수 과제 생성 및 관리 테스트
// 
// ⚠️ 사전 준비: 먼저 교수 계정으로 로그인하세요!
// 📁 위치: docs/일반유저 로그인+게시판/test-1-login.js
// 📝 실행: await login() (교수 계정 사용)
// ===================================================================

const API_BASE_URL = 'https://bluecrab.chickenkiller.com/BlueCrab-1.0.0';

// 전역 변수 (test-1-login.js에서 설정한 토큰 사용)
if (typeof window.authToken === 'undefined') window.authToken = null;

// ========== JWT 디코딩 ==========
function decodeJWT(token) {
    try {
        const base64Url = token.split('.')[1];
        const base64 = base64Url.replace(/-/g, '+').replace(/_/g, '/');
        const jsonPayload = decodeURIComponent(atob(base64).split('').map(c => 
            '%' + ('00' + c.charCodeAt(0).toString(16)).slice(-2)
        ).join(''));
        return JSON.parse(jsonPayload);
    } catch (e) {
        console.error('JWT 디코딩 실패:', e);
        return null;
    }
}

// ========== 로그인 상태 확인 ==========
function checkAuth() {
    const token = window.authToken;
    const user = window.currentUser;
    
    if (!token) {
        console.log('\n⚠️ 로그인이 필요합니다!');
        console.log('🔧 docs/일반유저 로그인+게시판/test-1-login.js 실행 → await login()');
        return false;
    }
    return true;
}

// ========== JWT에서 교수 정보 추출 ==========
async function getProfessorFromToken() {
    if (!window.authToken) return null;
    
    console.log('🔍 프로필 API로 USER_CODE 조회 중...');
    
    // 프로필 API로 USER_CODE 조회 (majorCode가 학번/교번)
    try {
        const response = await fetch(`${API_BASE_URL}/api/profile/me`, {
            method: 'POST',
            headers: {
                'Authorization': `Bearer ${window.authToken}`,
                'Content-Type': 'application/json'
            }
        });
        
        if (!response.ok) {
            console.log(`⚠️ 프로필 조회 실패 (${response.status})`);
            return null;
        }
        
        const result = await response.json();
        
        if (!result.success) {
            console.log('⚠️ 프로필 조회 실패:', result.message);
            return null;
        }
        
        const userData = result.data;
        const userCode = userData.majorCode; // majorCode가 학번/교번 (USER_CODE)
        const userName = userData.userName;
        const userType = userData.userTypeText;
        
        if (userCode) {
            console.log(`✅ 프로필 조회 성공: ${userName} (${userType}) → userCode="${userCode}"`);
            return userCode;
        } else {
            console.log('⚠️ 프로필에 majorCode(USER_CODE)가 없습니다.');
            console.log('응답:', userData);
            return null;
        }
    } catch (error) {
        console.log('❌ 프로필 조회 에러:', error.message);
        return null;
    }
}

// ========== JWT 토큰 디버깅 ==========
async function debugTokenInfo() {
    if (!window.authToken) {
        console.log('❌ 로그인 토큰이 없습니다.');
        console.log('💡 먼저 로그인하세요: await login()');
        return;
    }
    
    const payload = decodeJWT(window.authToken);
    if (!payload) {
        console.log('❌ JWT 디코딩 실패');
        return;
    }
    
    console.log('\n🔍 JWT Payload 전체 내용:');
    console.log('═══════════════════════════════════════════════════════');
    console.log(JSON.stringify(payload, null, 2));
    
    console.log('\n📋 모든 필드 나열:');
    console.log('═══════════════════════════════════════════════════════');
    Object.keys(payload).forEach(key => {
        console.log(`   ${key}: ${JSON.stringify(payload[key])}`);
    });
    
    console.log('\n🔎 userId 확인:');
    console.log('═══════════════════════════════════════════════════════');
    const userId = payload.userId || payload.USER_ID || payload.user_id || payload.id;
    if (userId) {
        console.log(`   ✅ userId: ${userId}`);
    } else {
        console.log('   ❌ userId를 찾을 수 없음');
    }
    
    console.log('\n👨‍🏫 USER_CODE 조회 시도:');
    console.log('═══════════════════════════════════════════════════════');
    const userCode = await getProfessorFromToken();
    if (userCode) {
        console.log(`   ✅ 최종 USER_CODE: "${userCode}"`);
    } else {
        console.log('   ❌ USER_CODE 조회 실패 - 수동 입력이 필요합니다.');
    }
    
    console.log('\n💡 currentUser 정보:');
    console.log('═══════════════════════════════════════════════════════');
    if (window.currentUser) {
        console.log(JSON.stringify(window.currentUser, null, 2));
    } else {
        console.log('   (없음)');
    }
}

// ========== 교수 강의 목록 조회 ==========
if (typeof window.authToken === 'undefined') window.authToken = null;

// ========== 내 강의 목록 조회 ==========
async function getMyLectures() {
    if (!checkAuth()) return;
    const token = window.authToken;
    
    // JWT에서 userId 추출 후 USER_CODE 조회
    console.log('🔄 교수번호 조회 중...');
    const professorCode = await getProfessorFromToken();
    
    if (!professorCode) {
        console.log('⚠️ 자동으로 교수번호를 조회할 수 없습니다.');
        console.log('💡 수동으로 교수번호를 입력하세요.');
    }
    
    const professor = professorCode || prompt('👨‍🏫 교수번호 (예: 11, PROF001):', '11');
    const page = parseInt(prompt('📄 페이지 번호 (0부터 시작):', '0'));
    const size = parseInt(prompt('📄 페이지 크기:', '10'));

    console.log('\n📚 담당 강의 목록 조회');
    console.log('═══════════════════════════════════════════════════════');
    console.log(`👨‍🏫 교수번호: ${professor}`);

    try {
        const url = `${API_BASE_URL}/lectures`;
        console.log('📡 요청 URL:', url);

        const response = await fetch(url, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                'Authorization': `Bearer ${token}`
            },
            body: JSON.stringify({
                professor: professor,
                page: page,
                size: size
            })
        });

        console.log(`📡 HTTP 상태: ${response.status}`);
        const result = await response.json();

        console.log('\n🔍 전체 응답 JSON:');
        console.log(JSON.stringify(result, null, 2));

        // 응답 구조 확인
        if (Array.isArray(result)) {
            // 배열 직접 반환
            console.log(`\n✅ 조회 성공! 총 ${result.length}개 강의`);
            console.log('📋 강의 목록:');
            result.forEach((lecture, idx) => {
                console.log(`\n${idx + 1}. ${lecture.lecTit || lecture.LEC_TIT} (${lecture.lecSerial || lecture.LEC_SERIAL})`);
                console.log(`   강의 IDX: ${lecture.lecIdx || lecture.LEC_IDX}`);
                console.log(`   교수코드: ${lecture.lecProf || lecture.LEC_PROF}`);
                console.log(`   교수명: ${lecture.lecProfName || lecture.LEC_PROF_NAME || 'N/A'}`);
                console.log(`   학기: ${lecture.lecYear || lecture.LEC_YEAR}-${lecture.lecSemester || lecture.LEC_SEMESTER}`);
                console.log(`   정원: ${lecture.lecCurrent || 0}/${lecture.lecMany || lecture.LEC_MANY}명`);
                
                // 첫 번째 강의 IDX 저장
                if (idx === 0) {
                    window.lastLectureIdx = lecture.lecIdx || lecture.LEC_IDX;
                    console.log(`   💾 저장됨: window.lastLectureIdx = ${window.lastLectureIdx}`);
                }
            });
        } else if (result.success) {
            // success 래핑된 응답
            console.log('\n✅ 조회 성공!');
            console.log(`📊 총 ${result.data.totalElements}개 강의`);
            console.log('📋 강의 목록:');
            result.data.content.forEach((lecture, idx) => {
                console.log(`\n${idx + 1}. ${lecture.lecTit || lecture.LEC_TIT} (${lecture.lecSerial || lecture.LEC_SERIAL})`);
                console.log(`   강의 IDX: ${lecture.lecIdx || lecture.LEC_IDX}`);
                console.log(`   교수코드: ${lecture.lecProf || lecture.LEC_PROF}`);
                console.log(`   교수명: ${lecture.lecProfName || lecture.LEC_PROF_NAME || 'N/A'}`);
                console.log(`   학기: ${lecture.lecYear || lecture.LEC_YEAR}-${lecture.lecSemester || lecture.LEC_SEMESTER}`);
                console.log(`   정원: ${lecture.lecCurrent || 0}/${lecture.lecMany || lecture.LEC_MANY}명`);
                
                // 첫 번째 강의 IDX 저장
                if (idx === 0) {
                    window.lastLectureIdx = lecture.lecIdx || lecture.LEC_IDX;
                    console.log(`   💾 저장됨: window.lastLectureIdx = ${window.lastLectureIdx}`);
                }
            });
        } else {
            console.log('❌ 조회 실패 [' + response.status + ']:', result.message || result);
        }
    } catch (error) {
        console.log('❌ 에러 발생:', error.message);
    }
}

// ========== 과제 생성 ==========
async function createAssignment() {
    if (!checkAuth()) return;
    const token = window.authToken;
    
    const lectureIdx = window.lastLectureIdx || parseInt(prompt('📚 강의 IDX:', '1'));
    const title = prompt('📝 과제 제목:', '1주차 과제');
    const description = prompt('📝 과제 설명:', '자바 프로그래밍 기초 과제입니다.');
    const maxScore = 10;  // ✅ 항상 10점으로 고정
    const dueDate = prompt('📅 마감일 (YYYY-MM-DD):', '2025-12-31');

    if (!title || !lectureIdx) {
        console.log('❌ 필수 입력값이 없습니다.');
        return;
    }

    console.log('\n📄 과제 생성');
    console.log('═══════════════════════════════════════════════════════');
    console.log(`📚 강의 IDX: ${lectureIdx}`);
    console.log(`📝 제목: ${title}`);
    console.log(`📅 마감일: ${dueDate}`);
    console.log(`💯 배점: 10점 (고정)`);

    // ✅ DTO 패턴 - camelCase 필드명 사용
    const assignmentData = {
        lecIdx: lectureIdx,
        title: title,
        body: description,
        maxScore: maxScore,
        dueDate: dueDate
    };

    console.log('\n📤 요청 데이터:');
    console.log(JSON.stringify(assignmentData, null, 2));

try {
        const response = await fetch(`${API_BASE_URL}/api/assignments`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                'Authorization': `Bearer ${token}`
            },
            body: JSON.stringify(assignmentData)
        });

        console.log(`📡 HTTP 상태: ${response.status}`);
        const result = await response.json();

        console.log('\n🔍 전체 응답:');
        console.log(JSON.stringify(result, null, 2));

        if (response.ok || result.success) {
            console.log('\n✅ 과제 생성 성공!');
            const assignment = result.data || result;
            console.log('📊 생성된 과제:');
            console.log(`   IDX: ${assignment.assignmentIdx || assignment.ASSIGNMENT_IDX}`);
            console.log(`   제목: ${assignment.title || assignment.ASSIGNMENT_TITLE}`);
            console.log(`   마감일: ${assignment.dueDate || assignment.DUE_DATE}`);
            console.log(`   배점: ${assignment.maxScore || assignment.MAX_SCORE}점`);
            
            window.lastAssignmentIdx = assignment.assignmentIdx || assignment.ASSIGNMENT_IDX;
            console.log(`💾 저장됨: window.lastAssignmentIdx = ${window.lastAssignmentIdx}`);
        } else {
            console.log('❌ 과제 생성 실패 [' + response.status + ']:', result.message || result);
        }
    } catch (error) {
        console.log('❌ 에러 발생:', error.message);
    }
}

// ========== 과제 목록 조회 ==========
async function getAssignments() {
    if (!checkAuth()) return;
    const token = window.authToken;
    
    const lectureIdx = window.lastLectureIdx || parseInt(prompt('📚 강의 IDX:', '1'));
    const page = parseInt(prompt('📄 페이지 번호 (0부터 시작):', '0'));
    const size = parseInt(prompt('📄 페이지 크기:', '10'));

    console.log('\n📄 과제 목록 조회');
    console.log('═══════════════════════════════════════════════════════');
    console.log(`📚 강의 IDX: ${lectureIdx}`);

    try {
        // ✅ DTO 패턴: POST 방식으로 변경
        const url = `${API_BASE_URL}/api/assignments/list`;
        console.log('📡 요청 URL:', url);

        const response = await fetch(url, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                'Authorization': `Bearer ${token}`
            },
            body: JSON.stringify({
                lecIdx: lectureIdx,
                page: page,
                size: size,
                action: 'list'  // 목록 조회 액션
            })
        });

        console.log(`📡 HTTP 상태: ${response.status}`);
        
        // 응답 텍스트 먼저 확인
        const responseText = await response.text();
        console.log('\n📄 응답 텍스트 (원본):');
        console.log(responseText);
        
        // JSON 파싱 시도
        let result;
        try {
            result = JSON.parse(responseText);
            console.log('\n🔍 파싱된 JSON:');
            console.log(JSON.stringify(result, null, 2));
        } catch (parseError) {
            console.log('❌ JSON 파싱 실패:', parseError.message);
            console.log('서버가 JSON이 아닌 응답을 반환했습니다.');
            return;
        }

        // Page 객체 응답 처리
        if (result.content) {
            console.log(`\n✅ 조회 성공! 총 ${result.totalElements}개 과제`);
            console.log('📋 과제 목록:');
            result.content.forEach((assignment, idx) => {
                console.log(`\n${idx + 1}. 과제 IDX: ${assignment.assignmentIdx}`);
                console.log(`   강의 IDX: ${assignment.lecIdx}`);
                console.log(`   생성일: ${assignment.createdDate}`);
                
                // assignmentData JSON 파싱
                try {
                    const data = JSON.parse(assignment.assignmentData || '{}');
                    const assignmentInfo = data.assignment || {};
                    console.log(`   제목: ${assignmentInfo.title || 'N/A'}`);
                    console.log(`   설명: ${assignmentInfo.description || 'N/A'}`);
                    console.log(`   마감일: ${assignmentInfo.dueDate || 'N/A'}`);
                    console.log(`   배점: ${assignmentInfo.maxScore || 'N/A'}점`);
                } catch (e) {
                    console.log(`   ⚠️ JSON 파싱 실패: ${e.message}`);
                }
                
                // 첫 번째 과제 IDX 저장
                if (idx === 0) {
                    window.lastAssignmentIdx = assignment.assignmentIdx;
                    console.log(`   💾 저장됨: window.lastAssignmentIdx = ${window.lastAssignmentIdx}`);
                }
            });
        } else {
            console.log('❌ 조회 실패 [' + response.status + ']:', result.message || result);
        }
    } catch (error) {
        console.log('❌ 에러 발생:', error.message);
    }
}

// ========== 학생별 제출 현황 조회 ==========
// 📌 각 학생의 제출 여부와 점수 확인
async function getSubmissions() {
    if (!checkAuth()) return;
    const token = window.authToken;
    
    const assignmentIdx = window.lastAssignmentIdx || parseInt(prompt('📄 과제 IDX:', '1'));

    console.log('\n📄 학생별 제출 현황 조회');
    console.log('═══════════════════════════════════════════════════════');
    console.log(`📄 과제 IDX: ${assignmentIdx}`);
    console.log('💡 실제 제출물: 서면/메일 등으로 수령');
    console.log('💡 DB 기록: 제출 여부 + 점수만 관리');

    try {
        // assignmentData 조회 - POST로 변경
        const url = `${API_BASE_URL}/api/assignments/submissions`;
        console.log('📡 요청 URL:', url);

        const response = await fetch(url, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                'Authorization': `Bearer ${token}`
            },
            body: JSON.stringify({
                assignmentIdx: assignmentIdx
            })
        });

        console.log(`📡 HTTP 상태: ${response.status}`);
        const result = await response.json();

        console.log('\n🔍 전체 응답:');
        console.log(JSON.stringify(result, null, 2));

        if (response.ok) {
            const submissions = result.submissions || result.data || [];
            console.log(`\n✅ 조회 성공! 총 ${submissions.length}명 기록`);
            
            if (submissions.length === 0) {
                console.log('📋 제출 기록이 없습니다.');
            } else {
                console.log('📋 학생별 제출 현황:');
                submissions.forEach((submission, idx) => {
                    console.log(`\n${idx + 1}. 학생 IDX: ${submission.studentIdx || submission.STUDENT_IDX}`);
                    console.log(`   학생명: ${submission.studentName || 'N/A'}`);
                    console.log(`   제출 여부: ${submission.submitted ? '✅ 제출함' : '❌ 미제출'}`);
                    console.log(`   제출 방식: ${submission.submissionMethod || 'N/A'}`);
                    console.log(`   제출일: ${submission.submittedAt || 'N/A'}`);
                    console.log(`   점수: ${submission.score !== undefined && submission.score !== null ? submission.score + '점' : '미채점'}`);
                    console.log(`   피드백: ${submission.feedback || 'N/A'}`);
                    if (submission.gradedAt) {
                        console.log(`   채점일: ${submission.gradedAt}`);
                    }
                });
            }
        } else {
            console.log('❌ 조회 실패 [' + response.status + ']:', result.message || result);
        }
    } catch (error) {
        console.log('❌ 에러 발생:', error.message);
    }
}

// ========== 과제 채점 (제출 여부 + 점수 기록) ==========
// 📌 학생이 서면/메일로 제출한 과제를 확인 후 점수 부여
async function gradeAssignment() {
    if (!checkAuth()) return;
    const token = window.authToken;
    
    const assignmentIdx = window.lastAssignmentIdx || parseInt(prompt('📄 과제 IDX:', '1'));
    const studentIdx = parseInt(prompt('👨‍🎓 학생 IDX:', '1'));
    
    console.log('\n📝 과제 채점 정보');
    console.log('═══════════════════════════════════════════════════════');
    console.log('💡 학생이 서면/메일로 제출한 과제를 확인하셨나요?');
    console.log('💡 실제 제출물 수령 후 점수를 입력하세요.');
    
    const submitted = confirm('✅ 학생이 과제를 제출했습니까? (확인/취소)');
    
    let score = null;
    let submissionMethod = '';
    let feedback = '';
    
    if (submitted) {
        // 제출 방식 입력
        submissionMethod = prompt('📮 제출 방식을 입력하세요:', '서면 제출 (2025-10-15)');
        score = parseInt(prompt('💯 점수 (0-10):', '8'));
        feedback = prompt('📝 피드백 (선택사항):', '잘 작성했습니다.');
        
        // ✅ 10점 초과 시 경고 (서버에서도 10점으로 자동 변환됨)
        if (score > 10) {
            console.log(`⚠️ 입력된 점수 ${score}점이 10점을 초과합니다. 서버에서 10점으로 변환됩니다.`);
        }
    } else {
        console.log('❌ 미제출로 기록합니다.');
        submissionMethod = '미제출';
        score = 0;
        feedback = '미제출';
    }

    console.log('\n💯 과제 채점');
    console.log('═══════════════════════════════════════════════════════');
    console.log(`📄 과제 IDX: ${assignmentIdx}`);
    console.log(`👨‍🎓 학생 IDX: ${studentIdx}`);
    console.log(`제출 여부: ${submitted ? '✅ 제출' : '❌ 미제출'}`);
    console.log(`제출 방식: ${submissionMethod}`);
    console.log(`점수: ${score}점`);
    console.log(`피드백: ${feedback}`);

    // ✅ DTO 패턴 - camelCase 필드명 사용
    // submissionMethod를 feedback 앞에 추가하여 구분
    const gradingData = {
        studentIdx: studentIdx,
        submitted: submitted,
        submissionMethod: submissionMethod,  // 제출 방식 추가
        score: score,
        feedback: feedback
    };

    console.log('\n📤 요청 데이터:');
    console.log(JSON.stringify(gradingData, null, 2));

    try {
        const response = await fetch(`${API_BASE_URL}/api/assignments/${assignmentIdx}/grade`, {
            method: 'PUT',  // ✅ PUT 메서드
            headers: {
                'Content-Type': 'application/json',
                'Authorization': `Bearer ${token}`
            },
            body: JSON.stringify(gradingData)
        });

        console.log(`📡 HTTP 상태: ${response.status}`);
        const result = await response.json();

        console.log('\n🔍 전체 응답:');
        console.log(JSON.stringify(result, null, 2));

        if (response.ok) {
            console.log('\n✅ 채점 완료!');
            console.log(`📊 학생 ${studentIdx}의 과제가 채점되었습니다.`);
            console.log(`   제출 여부: ${submitted ? '제출함' : '미제출'}`);
            console.log(`   제출 방식: ${submissionMethod}`);
            console.log(`   점수: ${score}점`);
            console.log(`   피드백: ${feedback}`);
        } else {
            console.log('❌ 채점 실패 [' + response.status + ']:', result.message || result);
        }
    } catch (error) {
        console.log('❌ 에러 발생:', error.message);
    }
}

// ========== 과제 수정 ==========
async function updateAssignment() {
    if (!checkAuth()) return;
    const token = window.authToken;
    
    const assignmentIdx = window.lastAssignmentIdx || parseInt(prompt('✏️ 수정할 과제 IDX:', '1'));
    const title = prompt('📝 새 제목 (선택사항):');
    const body = prompt('📝 새 설명 (선택사항):');
    const dueDate = prompt('📅 새 마감일 (선택사항, YYYY-MM-DD):');
    // ✅ maxScore는 서버에서 10점으로 고정되므로 입력받지 않음

    console.log('\n✏️ 과제 수정');
    console.log('═══════════════════════════════════════════════════════');
    console.log(`📄 과제 IDX: ${assignmentIdx}`);

    // ✅ DTO 패턴 - camelCase 필드명 사용
    const updateData = {};
    if (title) updateData.title = title;
    if (body) updateData.body = body;
    if (dueDate) updateData.dueDate = dueDate;
    // maxScore는 제외 (서버에서 항상 10점으로 설정)

    if (Object.keys(updateData).length === 0) {
        console.log('❌ 수정할 내용이 없습니다.');
        return;
    }

    console.log('\n📤 요청 데이터:');
    console.log(JSON.stringify(updateData, null, 2));

try {
        const response = await fetch(`${API_BASE_URL}/api/assignments/${assignmentIdx}`, {
            method: 'PUT',
            headers: {
                'Content-Type': 'application/json',
                'Authorization': `Bearer ${token}`
            },
            body: JSON.stringify(updateData)
        });

        console.log(`📡 HTTP 상태: ${response.status}`);
        const result = await response.json();

        console.log('\n🔍 전체 응답:');
        console.log(JSON.stringify(result, null, 2));

        if (response.ok) {
            console.log('\n✅ 수정 성공!');
            console.log('📊 수정된 과제:');
            console.log(`   IDX: ${result.assignmentIdx}`);
            console.log(`   강의 IDX: ${result.lecIdx}`);
        } else {
            console.log('❌ 수정 실패 [' + response.status + ']:', result.message || result);
        }
    } catch (error) {
        console.log('❌ 에러 발생:', error.message);
    }
}

// ========== 과제 삭제 ==========
async function deleteAssignment() {
    if (!checkAuth()) return;
    const token = window.authToken;
    
    const assignmentIdx = window.lastAssignmentIdx || parseInt(prompt('🗑️ 삭제할 과제 IDX:', '1'));
    const confirm = prompt('⚠️ 정말 삭제하시겠습니까? (yes/no):', 'no');

    if (confirm.toLowerCase() !== 'yes') {
        console.log('❌ 삭제 취소됨');
        return;
    }

    console.log('\n🗑️ 과제 삭제');
    console.log('═══════════════════════════════════════════════════════');
    console.log(`📄 과제 IDX: ${assignmentIdx}`);

    try {
        const response = await fetch(`${API_BASE_URL}/api/assignments/${assignmentIdx}`, {
            method: 'DELETE',
            headers: {
                'Authorization': `Bearer ${token}`
            }
        });

        console.log(`📡 HTTP 상태: ${response.status}`);

        if (response.ok) {
            // 204 No Content 또는 200 OK
            if (response.status === 204) {
                console.log('\n✅ 삭제 성공! (No Content)');
            } else {
                const result = await response.json();
                console.log('\n� 전체 응답:');
                console.log(JSON.stringify(result, null, 2));
                console.log('\n✅ 삭제 성공!');
            }
        } else {
            const result = await response.json();
            console.log('❌ 삭제 실패 [' + response.status + ']:', result.message || result);
        }
    } catch (error) {
        console.log('❌ 에러 발생:', error.message);
    }
}
// ========== 도움말 ==========
function help() {
    console.log('\n👨‍🏫 교수 과제 관리 테스트');
    console.log('═══════════════════════════════════════════════════════\n');
    
    console.log('� 기본 테스트 시나리오 (복사해서 사용하세요):\n');
    console.log('// 1️⃣ 내 강의 목록 조회');
    console.log('await getMyLectures();\n');
    
    console.log('// 2️⃣ 과제 생성');
    console.log('await createAssignment();\n');
    
    console.log('// 3️⃣ 과제 목록 확인');
    console.log('await getAssignments();\n');
    
    console.log('// 4️⃣ 학생 제출 현황 조회');
    console.log('await getSubmissions();\n');
    
    console.log('// 5️⃣ 과제 채점 (제출 방식 + 점수 입력)');
    console.log('await gradeAssignment();\n');
    
    console.log('═══════════════════════════════════════════════════════\n');
    console.log('� 전체 함수 목록:\n');
    console.log('await getMyLectures()      - 담당 강의 목록');
    console.log('await createAssignment()   - 과제 생성');
    console.log('await getAssignments()     - 과제 목록');
    console.log('await getSubmissions()     - 제출 현황');
    console.log('await gradeAssignment()    - 채점 (제출방식+점수)');
    console.log('await updateAssignment()   - 과제 수정');
    console.log('await deleteAssignment()   - 과제 삭제');
    console.log('await debugTokenInfo()     - JWT 토큰 디버깅\n');
    
    console.log('💡 과제 시스템: 오프라인 제출 + DB는 점수만 기록');
    console.log('💡 배점: 모든 과제는 10점 만점으로 고정');
    console.log('💡 채점: 10점 초과 입력 시 자동으로 10점으로 변환');
    console.log('💡 window.lastLectureIdx, window.lastAssignmentIdx 자동 저장');
}

// 초기 메시지
console.log('✅ 교수 과제 관리 테스트 스크립트 로드 완료!');
console.log('💡 help() 를 입력하면 사용 가능한 함수 목록을 볼 수 있습니다.');

// JWT 디코딩 테스트 (async 함수이므로 즉시 실행하지 않음)
if (window.authToken) {
    console.log('🔄 교수번호 조회 중...');
    getProfessorFromToken().then(professorCode => {
        if (professorCode) {
            console.log(`👨‍🏫 인식된 교수번호: ${professorCode}`);
        } else {
            console.log('⚠️ JWT에서 교수번호(USER_CODE)를 자동 추출할 수 없습니다. 수동 입력이 필요합니다.');
        }
    }).catch(err => {
        console.log('⚠️ 교수번호 조회 실패:', err.message);
    });
}
