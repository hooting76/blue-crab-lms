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
// ========== JWT에서 교수 정보 추출 ==========
async function getProfessorFromToken() {
    if (!window.authToken) return null;
    
    const payload = decodeJWT(window.authToken);
    if (!payload) return null;
    
    // JWT에서 userId (USER_IDX) 추출
    const userId = payload.userId || payload.USER_ID || payload.user_id || payload.id;
    
    if (!userId) {
        console.log('⚠️ JWT에서 userId를 찾을 수 없습니다.');
        return null;
    }
    
    console.log(`🔍 JWT에서 userId 추출: ${userId}`);
    
    // userId로 USER_CODE 조회
    try {
        const response = await fetch(`${API_BASE_URL}/users/${userId}`, {
            headers: {
                'Authorization': `Bearer ${window.authToken}`
            }
        });
        
        if (!response.ok) {
            console.log(`⚠️ 사용자 정보 조회 실패 (${response.status})`);
            return null;
        }
        
        const result = await response.json();
        const userData = result.data || result;
        const userCode = userData.userCode || userData.USER_CODE || userData.user_code;
        
        if (userCode) {
            console.log(`✅ USER_CODE 조회 성공: userId=${userId} → userCode="${userCode}"`);
            return userCode;
        } else {
            console.log('⚠️ 사용자 정보에 USER_CODE가 없습니다.');
            console.log('응답:', userData);
            return null;
        }
    } catch (error) {
        console.log('❌ USER_CODE 조회 에러:', error.message);
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
    console.log(`👨‍🏫 교수번호: ${professor}`);

    try {
        const url = `${API_BASE_URL}/lectures?professor=${encodeURIComponent(professor)}&page=${page}&size=${size}`;
        console.log('📡 요청 URL:', url);

        const response = await fetch(url, {
            headers: {
                'Authorization': `Bearer ${token}`
            }
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
    const maxScore = parseInt(prompt('💯 배점:', '100'));
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
        const response = await fetch(`${API_BASE_URL}/assignments`, {
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
        // ✅ DTO 패턴: lecIdx 파라미터 사용
        const url = `${API_BASE_URL}/assignments?lecIdx=${lectureIdx}&page=${page}&size=${size}`;
        console.log('📡 요청 URL:', url);

        const response = await fetch(url, {
            headers: {
                'Authorization': `Bearer ${token}`
            }
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

// ========== 제출된 과제 목록 조회 ==========
async function getSubmissions() {
    if (!checkAuth()) return;
    const token = window.authToken;
    
    const assignmentIdx = window.lastAssignmentIdx || parseInt(prompt('📄 과제 IDX:', '1'));

    console.log('\n📄 제출된 과제 목록 조회');
    console.log('═══════════════════════════════════════════════════════');
    console.log(`📄 과제 IDX: ${assignmentIdx}`);

    try {
        // assignmentData 조회
        const url = `${API_BASE_URL}/assignments/${assignmentIdx}/data`;
        console.log('📡 요청 URL:', url);

        const response = await fetch(url, {
            headers: {
                'Authorization': `Bearer ${token}`
            }
        });

        console.log(`📡 HTTP 상태: ${response.status}`);
        const result = await response.json();

        console.log('\n🔍 전체 응답:');
        console.log(JSON.stringify(result, null, 2));

        if (response.ok) {
            const submissions = result.submissions || [];
            console.log(`\n✅ 조회 성공! 총 ${submissions.length}개 제출`);
            
            if (submissions.length === 0) {
                console.log('📋 제출된 과제가 없습니다.');
            } else {
                console.log('📋 제출 목록:');
                submissions.forEach((submission, idx) => {
                    console.log(`\n${idx + 1}. 학생 IDX: ${submission.studentIdx}`);
                    console.log(`   제출일: ${submission.submittedAt || 'N/A'}`);
                    console.log(`   파일: ${submission.fileUrl || 'N/A'}`);
                    console.log(`   점수: ${submission.score || '미채점'}점`);
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

// ========== 과제 채점 ==========
async function gradeAssignment() {
    if (!checkAuth()) return;
    const token = window.authToken;
    
    const assignmentIdx = window.lastAssignmentIdx || parseInt(prompt('📄 과제 IDX:', '1'));
    const studentIdx = parseInt(prompt('👨‍🎓 학생 IDX:', '1'));
    const score = parseInt(prompt('💯 점수:', '85'));
    const feedback = prompt('📝 피드백:', '잘 작성했습니다.');

    console.log('\n💯 과제 채점');
    console.log('═══════════════════════════════════════════════════════');
    console.log(`📄 과제 IDX: ${assignmentIdx}`);
    console.log(`👨‍🎓 학생 IDX: ${studentIdx}`);

    // ✅ DTO 패턴 - camelCase 필드명 사용
    const gradingData = {
        studentIdx: studentIdx,
        score: score,
        feedback: feedback
    };

    console.log('\n📤 요청 데이터:');
    console.log(JSON.stringify(gradingData, null, 2));

try {
        const response = await fetch(`${API_BASE_URL}/assignments/${assignmentIdx}/grade`, {
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
            console.log('\n✅ 채점 성공!');
            console.log('📊 채점된 과제 정보가 업데이트되었습니다.');
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
    const maxScore = prompt('💯 새 배점 (선택사항):');

    console.log('\n✏️ 과제 수정');
    console.log('═══════════════════════════════════════════════════════');
    console.log(`📄 과제 IDX: ${assignmentIdx}`);

    // ✅ DTO 패턴 - camelCase 필드명 사용
    const updateData = {};
    if (title) updateData.title = title;
    if (body) updateData.body = body;
    if (dueDate) updateData.dueDate = dueDate;
    if (maxScore) updateData.maxScore = parseInt(maxScore);

    if (Object.keys(updateData).length === 0) {
        console.log('❌ 수정할 내용이 없습니다.');
        return;
    }

    console.log('\n📤 요청 데이터:');
    console.log(JSON.stringify(updateData, null, 2));

try {
        const response = await fetch(`${API_BASE_URL}/assignments/${assignmentIdx}`, {
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
        const response = await fetch(`${API_BASE_URL}/assignments/${assignmentIdx}`, {
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
    console.log('\n👨‍🏫 교수 과제 관리 테스트 함수 목록');
    console.log('═══════════════════════════════════════════════════════');
    console.log('⚠️ 먼저 로그인하세요!');
    console.log('📁 docs/일반유저 로그인+게시판/test-1-login.js → await login()');
    console.log('');
    console.log('� await debugTokenInfo()     - JWT 토큰 전체 내용 확인 (교수번호 추출 확인)');
    console.log('�📚 await getMyLectures()      - 담당 강의 목록 (JWT 자동 교수번호 추출)');
    console.log('📝 await createAssignment()   - 과제 생성 (DTO 패턴)');
    console.log('📋 await getAssignments()     - 과제 목록 조회');
    console.log('📄 await getSubmissions()     - 제출된 과제 목록');
    console.log('💯 await gradeAssignment()    - 과제 채점');
    console.log('✏️ await updateAssignment()   - 과제 수정');
    console.log('🗑️ await deleteAssignment()   - 과제 삭제');
    console.log('');
    console.log('💡 모든 함수는 async이므로 await를 붙여서 호출하세요!');
    console.log('💡 JWT 토큰에서 자동으로 교수번호(USER_CODE)를 추출합니다.');
    console.log('💡 Phase 6.8 DTO 패턴: camelCase 필드명 사용 (lecIdx, title, body 등)');
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
