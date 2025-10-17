// ===================================================================
// 📚 관리자 강의 관리 테스트 (POST 방식)

// ===================================================================

const API_BASE_URL = 'https://bluecrab.chickenkiller.com/BlueCrab-1.0.0/api';

// ========== 로그인 상태 확인 ==========
function checkAuth() {
    const token = window.authToken || localStorage.getItem('jwtAccessToken');
    if (!token) {
        console.log('\n⚠️ 로그인이 필요합니다!');
        console.log('🔧 docs/관리자 로그인/admin-login-to-board-test.js 실행');
        console.log('   1. await adminLogin()');
        console.log('   2. await sendAuthCode()');
        console.log('   3. await verifyAuthCode()');
        return false;
    }
    console.log('✅ 로그인 확인됨');
    return true;
}

// ========== 강의 목록 조회 ==========
async function getLectures() {
    if (!checkAuth()) return;
    const token = window.authToken || localStorage.getItem('jwtAccessToken');
    
    const page = parseInt(prompt('📄 페이지 번호 (0부터 시작):', '0'));
    const size = parseInt(prompt('📄 페이지 크기:', '10'));
    const professor = prompt('👨‍🏫 교수 코드 (선택, 공백=전체):', '');
    const year = prompt('🎯 대상 학년 (선택, 1-4, 공백=전체):', '');
    const semester = prompt('📅 학기 (선택, 1/2, 공백=전체):', '');

    console.log('\n📚 강의 목록 조회 - POST');
    console.log('═══════════════════════════════════════════════════════');
    
    try {
        const requestBody = { page, size };
        if (professor) requestBody.professor = professor;
        if (year) requestBody.year = parseInt(year);
        if (semester) requestBody.semester = parseInt(semester);

        const response = await fetch(`${API_BASE_URL}/lectures`, {
            method: 'POST',
            headers: {
                'Authorization': `Bearer ${token}`,
                'Content-Type': 'application/json'
            },
            body: JSON.stringify(requestBody)
        });

        console.log(`📡 응답 상태: ${response.status} ${response.statusText}`);
        
        if (!response.ok) {
            const errorText = await response.text();
            console.log(`❌ HTTP 에러 발생:`);
            console.log(`   상태 코드: ${response.status}`);
            console.log(`   응답 본문: ${errorText}`);
            return;
        }

        const data = await response.json();
        console.log('📋 전체 응답 데이터:', JSON.stringify(data, null, 2));
        
        // Spring Page 객체 직접 처리 (success 필드 없음)
        if (data.content && Array.isArray(data.content)) {
            console.log(`\n✅ 총 ${data.totalElements}개 강의 (${data.totalPages}페이지)\n`);
            
            data.content.forEach((lec, i) => {
                console.log(`${i+1}. ${lec.lecTit} (코드:${lec.lecSerial})`);
                console.log(`   📋 코드: ${lec.lecSerial || 'N/A'}`);
                console.log(`   👨‍🏫 교수: ${lec.lecProfName || lec.lecProf || 'N/A'}`);
                console.log(`   🎯 대상학년/학기: ${lec.lecYear || 'N/A'}학년 ${lec.lecSemester || 'N/A'}학기`);
                console.log(`   👥 수강인원: ${lec.lecCurrent}/${lec.lecMany}명 (잔여: ${lec.availableSeats}석)`);
                console.log(`   🏢 강의실: 정보 없음`);
                console.log(`   ⏰ 시간: ${lec.lecTime || 'N/A'}`);
                console.log(`   🎓 학점: ${lec.lecPoint}학점`);
                
                // 🆕 Phase 9: 백엔드 필터링 정보
                if (lec.lecMcode || lec.lecMcodeDep) {
                    console.log(`   🎓 필터링 정보:`);
                    console.log(`      - 학부 코드: ${lec.lecMcode || '0 (전체 가능)'}`);
                    console.log(`      - 학과 코드: ${lec.lecMcodeDep || '0 (전체 가능)'}`);
                    
                    if (lec.lecMcode === '0' && lec.lecMcodeDep === '0') {
                        console.log(`      ✅ 모든 학생 수강 가능 (0값 규칙)`);
                    } else if (lec.lecMcode === '0') {
                        console.log(`      ✅ 학과 ${lec.lecMcodeDep} 학생만 수강 가능`);
                    } else if (lec.lecMcodeDep === '0') {
                        console.log(`      ✅ 학부 ${lec.lecMcode} 학생만 수강 가능`);
                    } else {
                        console.log(`      ✅ 학부 ${lec.lecMcode} + 학과 ${lec.lecMcodeDep} 학생만 수강 가능`);
                    }
                }
                console.log('');
            });
            
            if (data.content.length > 0) window.lastLectureSerial = data.content[0].lecSerial;
            
        } else if (data.success) {
            // 래핑된 응답 구조 처리
            console.log(`\n✅ 총 ${data.data.totalElements}개 강의\n`);
            data.data.content.forEach((lec, i) => {
                console.log(`${i+1}. ${lec.lecTit || lec.lecName} (IDX:${lec.lecIdx})`);
                console.log(`   📋 코드: ${lec.lecSerial || lec.lecCode || 'N/A'}`);
                console.log(`   👨‍🏫 교수: ${lec.lecProfName || lec.lecProf || 'N/A'}`);
                console.log(`   🎯 대상학년/학기: ${lec.lecYear}학년 ${lec.lecSemester}학기`);
                console.log(`   👥 수강인원: ${lec.lecCurrent}/${lec.lecMany || lec.lecMax}명`);
                console.log(`   ⏰ 시간: ${lec.lecTime || 'N/A'}`);
                console.log(`   🎓 학점: ${lec.lecPoint}학점`);                
                // 🆕 Phase 9: 백엔드 필터링 정보
                if (lec.lecMcode || lec.lecMcodeDep) {
                    console.log(`   🎓 필터링 정보:`);
                    console.log(`      - 학부 코드: ${lec.lecMcode || '0 (전체 가능)'}`);
                    console.log(`      - 학과 코드: ${lec.lecMcodeDep || '0 (전체 가능)'}`);
                    
                    if (lec.lecMcode === '0' && lec.lecMcodeDep === '0') {
                        console.log(`      ✅ 모든 학생 수강 가능 (0값 규칙)`);
                    } else if (lec.lecMcode === '0') {
                        console.log(`      ✅ 학과 ${lec.lecMcodeDep} 학생만 수강 가능`);
                    } else if (lec.lecMcodeDep === '0') {
                        console.log(`      ✅ 학부 ${lec.lecMcode} 학생만 수강 가능`);
                    } else {
                        console.log(`      ✅ 학부 ${lec.lecMcode} + 학과 ${lec.lecMcodeDep} 학생만 수강 가능`);
                    }
                }
                console.log('');
            });
            if (data.data.content.length > 0) window.lastLectureSerial = data.data.content[0].lecSerial;
        } else {
            console.log('❌ 조회 실패');
            console.log('   - 응답 구조를 인식할 수 없습니다');
            console.log('   - data.content:', data.content ? '있음' : '없음');
            console.log('   - data.success:', data.success);
        }
    } catch (error) {
        console.log('❌ 에러 발생:', error.message);
    }
}

// ========== 강의 상세 조회 ==========
async function getLectureDetail() {
    if (!checkAuth()) return;
    const token = window.authToken || localStorage.getItem('jwtAccessToken');
    const lecSerial = prompt('🔍 조회할 강의 코드 (예: CS101):', window.lastLectureSerial || 'CS101');

    console.log('\n📚 강의 상세 조회 - POST');
    console.log('═══════════════════════════════════════════════════════');
    
    try {
        const response = await fetch(`${API_BASE_URL}/lectures/detail`, {
            method: 'POST',
            headers: {
                'Authorization': `Bearer ${token}`,
                'Content-Type': 'application/json'
            },
            body: JSON.stringify({ lecSerial })
        });

        console.log(`📡 응답 상태: ${response.status} ${response.statusText}`);
        
        if (!response.ok) {
            const errorText = await response.text();
            console.log(`❌ HTTP 에러 발생:`);
            console.log(`   상태 코드: ${response.status}`);
            console.log(`   응답 본문: ${errorText}`);
            return;
        }

        const data = await response.json();
        console.log('📋 전체 응답 데이터:', JSON.stringify(data, null, 2));
        
        // 직접 객체 반환인지 확인
        if (data.lecIdx) {
            // Spring 엔티티 직접 반환
            const lec = data;
            console.log(`\n✅ 강의 정보:`);
            console.log(`📋 강의명: ${lec.lecTit || 'N/A'} (${lec.lecSerial || 'N/A'})`);
            console.log(`👨‍🏫 교수: ${lec.lecProfName || lec.lecProf || 'N/A'}`);
            console.log(`🎯 대상학년/학기: ${lec.lecYear || 'N/A'}학년 ${lec.lecSemester || 'N/A'}학기`);
            console.log(`👥 수강인원: ${lec.lecCurrent}/${lec.lecMany}명 (잔여: ${lec.availableSeats}석)`);
            console.log(`⏰ 시간: ${lec.lecTime || 'N/A'}`);
            console.log(`🎓 학점: ${lec.lecPoint}학점`);
            console.log(`📚 요약: ${lec.lecSummary || 'N/A'}`);
            
            // 🆕 Phase 9: 백엔드 필터링 정보
            console.log(`\n🎓 수강 자격 정보 (Phase 9):`);
            console.log(`   학부 코드 (LEC_MCODE): ${lec.lecMcode || '0 (제한없음)'}`);
            console.log(`   학과 코드 (LEC_MCODE_DEP): ${lec.lecMcodeDep || '0 (제한없음)'}`);
            
            if (lec.lecMcode === '0' && lec.lecMcodeDep === '0') {
                console.log(`   ✅ 모든 학생 수강 가능 (0값 규칙)`);
            } else {
                console.log(`   ✅ 조건: 학생의 전공 OR 부전공이 일치해야 함`);
            }
            
            if (lec.lecMin) {
                console.log(`   최소 학년: ${lec.lecMin}학년 이상`);
            }
            
            window.lastLectureIdx = lec.lecIdx;
            
        } else if (data.success) {
            // 래핑된 응답 구조
            const lec = data.data;
            console.log(`\n✅ 강의 정보:`);
            console.log(`📋 강의명: ${lec.lecTit || lec.lecName} (${lec.lecSerial || lec.lecCode || 'N/A'})`);
            console.log(`👨‍🏫 교수: ${lec.lecProfName || lec.lecProf || 'N/A'}`);
            console.log(`🎯 대상학년/학기: ${lec.lecYear}학년 ${lec.lecSemester}학기`);
            console.log(`👥 수강인원: ${lec.lecCurrent}/${lec.lecMany || lec.lecMax}명`);
            console.log(`⏰ 시간: ${lec.lecTime || 'N/A'}`);
            console.log(`🎓 학점: ${lec.lecPoint}학점`);            
            // 🆕 Phase 9: 백엔드 필터링 정보
            console.log(`\n🎓 수강 자격 정보 (Phase 9):`);
            console.log(`   학부 코드 (LEC_MCODE): ${lec.lecMcode || '0 (제한없음)'}`);
            console.log(`   학과 코드 (LEC_MCODE_DEP): ${lec.lecMcodeDep || '0 (제한없음)'}`);
            
            if (lec.lecMcode === '0' && lec.lecMcodeDep === '0') {
                console.log(`   ✅ 모든 학생 수강 가능 (0값 규칙)`);
            } else {
                console.log(`   ✅ 조건: 학생의 전공 OR 부전공이 일치해야 함`);
            }
            
            if (lec.lecMin) {
                console.log(`   최소 학년: ${lec.lecMin}학년 이상 (⏸️ 미구현)`);
            }
            
            window.lastLectureIdx = lec.lecIdx;
        } else {
            console.log('❌ 조회 실패');
            console.log('   - 응답 구조를 인식할 수 없습니다');
            console.log('   - data.lecIdx:', data.lecIdx);
            console.log('   - data.success:', data.success);
        }
    } catch (error) {
        console.log('❌ 에러 발생:', error.message);
    }
}

// ========== 강의 통계 조회 ==========
async function getLectureStats() {
    if (!checkAuth()) return;
    const token = window.authToken || localStorage.getItem('jwtAccessToken');
    const lecSerial = prompt('📊 통계 조회할 강의 코드 (예: CS101):', window.lastLectureSerial || 'CS101');

    console.log('\n📊 강의 통계 조회 - POST');
    console.log('═══════════════════════════════════════════════════════');
    
    try {
        const response = await fetch(`${API_BASE_URL}/lectures/stats`, {
            method: 'POST',
            headers: {
                'Authorization': `Bearer ${token}`,
                'Content-Type': 'application/json'
            },
            body: JSON.stringify({ lecSerial })
        });

        console.log(`📡 응답 상태: ${response.status} ${response.statusText}`);
        
        if (!response.ok) {
            const errorText = await response.text();
            console.log(`❌ HTTP 에러 발생:`);
            console.log(`   상태 코드: ${response.status}`);
            console.log(`   응답 본문: ${errorText}`);
            return;
        }

        const data = await response.json();
        console.log('📋 전체 응답 데이터:', JSON.stringify(data, null, 2));
        
        // 직접 객체 반환 확인 (totalStudents 필드로 통계 객체 식별)
        if (data.totalStudents !== undefined) {
            console.log('\n✅ 통계 정보:');
            console.log(`👥 수강생: ${data.totalStudents}명`);
            console.log(`📄 총 과제: ${data.totalAssignments}개`);
            console.log(`✅ 제출된 과제: ${data.submittedAssignments}개`);
            console.log(`📊 제출률: ${data.submissionRate}%`);
        } else if (data.success) {
            const s = data.data;
            console.log('\n✅ 통계 정보:');
            console.log(`👥 수강생: ${s.totalStudents}명`);
            console.log(`📄 총 과제: ${s.totalAssignments}개`);
            console.log(`✅ 제출된 과제: ${s.submittedAssignments}개`);
            console.log(`📊 제출률: ${s.submissionRate}%`);
        } else {
            console.log('❌ 조회 실패');
            console.log('   - 응답 구조를 인식할 수 없습니다');
            console.log('   - data.totalStudents:', data.totalStudents);
            console.log('   - data.success:', data.success);
        }
    } catch (error) {
        console.log('❌ 에러 발생:', error.message);
    }
}

// ========== 강의 생성 ==========
async function createLecture() {
    if (!checkAuth()) return;
    const token = window.authToken || localStorage.getItem('jwtAccessToken');
    
    console.log('\n📝 강의 생성 - POST');
    console.log('═══════════════════════════════════════════════════════');
    
    const lecTit = prompt('📚 강의명:', '');
    const lecSerial = prompt('📋 강의 코드:', '');
    const lecProf = prompt('👨‍🏫 교수 코드:', '');
    const lecYear = parseInt(prompt('🎯 대상 학년 (1~4):', '1'));
    const lecSemester = parseInt(prompt('📅 학기 (1/2):', '1'));
    const lecMany = parseInt(prompt('👥 최대 수강 인원:', '30'));
    const lecTime = prompt('⏰ 강의 시간 (예: 월1월2수1수2):', '');
    const lecPoint = parseInt(prompt('🎓 학점:', '3'));
    const lecMin = parseInt(prompt('🎯 최소 학년 (0=제한없음):', '0'));
    const lecMcode = prompt('🎓 학부 코드 (0=전체):', '0');
    const lecMcodeDep = prompt('🎓 학과 코드 (0=전체):', '0');
    const lecSummary = prompt('📚 강의 요약:', '');
    const lecMajor = parseInt(prompt('📌 전공 여부 (1=전공, 0=교양):', '1'));
    const lecMust = parseInt(prompt('📌 필수 여부 (1=필수, 0=선택):', '0'));
    const lecAssign = parseInt(prompt('📝 과제 여부 (1=있음, 0=없음):', '0'));
    const lecOpen = parseInt(prompt('🔓 개방 여부 (1=개방, 0=미개방):', '1'));
    
    try {
        const requestBody = {
            lecTit,
            lecSerial,
            lecProf,
            lecYear,
            lecSemester,
            lecMany,
            lecTime,
            lecPoint,
            lecMin,
            lecMcode,
            lecMcodeDep,
            lecSummary,
            lecMajor,
            lecMust,
            lecAssign,
            lecOpen,
            lecCurrent: 0  // 초기 수강 인원 0
        };

        console.log('📤 요청 데이터:', JSON.stringify(requestBody, null, 2));

        const response = await fetch(`${API_BASE_URL}/lectures/create`, {
            method: 'POST',
            headers: {
                'Authorization': `Bearer ${token}`,
                'Content-Type': 'application/json'
            },
            body: JSON.stringify(requestBody)
        });

        console.log(`📡 응답 상태: ${response.status} ${response.statusText}`);
        
        if (!response.ok) {
            const errorText = await response.text();
            console.log(`❌ HTTP 에러 발생:`);
            console.log(`   상태 코드: ${response.status}`);
            console.log(`   응답 본문: ${errorText}`);
            return;
        }

        const data = await response.json();
        console.log('📋 전체 응답 데이터:', JSON.stringify(data, null, 2));
        
        // 직접 엔티티 반환 또는 래핑된 응답 처리
        if (data.lecSerial) {
            console.log('\n✅ 강의 생성 성공!');
            console.log(`생성된 강의 코드: ${data.lecSerial}`);
            console.log(`강의명: ${data.lecTit}`);
            window.lastLectureSerial = data.lecSerial;
        } else if (data.success) {
            console.log('\n✅ 강의 생성 성공!');
            console.log(`생성된 강의 코드: ${data.data.lecSerial}`);
            console.log(`강의명: ${data.data.lecTit || data.data.lecName || 'N/A'}`);
            window.lastLectureSerial = data.data.lecSerial;
        } else {
            console.log('❌ 생성 실패:', data.message || '알 수 없는 오류');
        }
    } catch (error) {
        console.log('❌ 에러 발생:', error.message);
    }
}

// ========== 강의 수정 ==========
async function updateLecture() {
    if (!checkAuth()) return;
    const token = window.authToken || localStorage.getItem('jwtAccessToken');
    
    const lecSerial = prompt('🔍 수정할 강의 코드 (예: CS101):', window.lastLectureSerial || 'CS101');
    
    console.log('\n✏️ 강의 수정 - POST');
    console.log('═══════════════════════════════════════════════════════');
    
    const lecTit = prompt('📚 강의명 (공백=유지):', '');
    const lecMany = prompt('👥 최대 수강 인원 (공백=유지):', '');
    const lecTime = prompt('⏰ 강의 시간 (공백=유지):', '');
    const lecPoint = prompt('🎓 학점 (공백=유지):', '');
    const lecMin = prompt('🎯 최소 학년 (공백=유지):', '');
    const lecSummary = prompt('📚 강의 요약 (공백=유지):', '');
    
    try {
        const requestBody = { lecSerial };
        if (lecTit) requestBody.lecTit = lecTit;
        if (lecMany) requestBody.lecMany = parseInt(lecMany);
        if (lecTime) requestBody.lecTime = lecTime;
        if (lecPoint) requestBody.lecPoint = parseInt(lecPoint);
        if (lecMin) requestBody.lecMin = parseInt(lecMin);
        if (lecSummary) requestBody.lecSummary = lecSummary;

        console.log('📤 요청 데이터:', JSON.stringify(requestBody, null, 2));

        const response = await fetch(`${API_BASE_URL}/lectures/update`, {
            method: 'POST',
            headers: {
                'Authorization': `Bearer ${token}`,
                'Content-Type': 'application/json'
            },
            body: JSON.stringify(requestBody)
        });

        console.log(`📡 응답 상태: ${response.status} ${response.statusText}`);
        
        if (!response.ok) {
            const errorText = await response.text();
            console.log(`❌ HTTP 에러 발생:`);
            console.log(`   상태 코드: ${response.status}`);
            console.log(`   응답 본문: ${errorText}`);
            return;
        }

        const data = await response.json();
        console.log('📋 전체 응답 데이터:', JSON.stringify(data, null, 2));
        
        if (data.lecIdx || data.success) {
            console.log('\n✅ 강의 수정 성공!');
        } else {
            console.log('❌ 수정 실패:', data.message || '알 수 없는 오류');
        }
    } catch (error) {
        console.log('❌ 에러 발생:', error.message);
    }
}

// ========== 강의 삭제 ==========
async function deleteLecture() {
    if (!checkAuth()) return;
    const token = window.authToken || localStorage.getItem('jwtAccessToken');
    
    const lecSerial = prompt('🗑️ 삭제할 강의 코드 (예: CS101):', window.lastLectureSerial || 'CS101');
    
    if (!confirm(`정말로 강의 코드 ${lecSerial}를 삭제하시겠습니까?`)) {
        console.log('❌ 삭제 취소됨');
        return;
    }
    
    console.log('\n🗑️ 강의 삭제 - POST');
    console.log('═══════════════════════════════════════════════════════');
    
    try {
        const response = await fetch(`${API_BASE_URL}/lectures/delete`, {
            method: 'POST',
            headers: {
                'Authorization': `Bearer ${token}`,
                'Content-Type': 'application/json'
            },
            body: JSON.stringify({ lecSerial })
        });

        console.log(`📡 응답 상태: ${response.status} ${response.statusText}`);
        
        if (!response.ok) {
            const errorText = await response.text();
            console.log(`❌ HTTP 에러 발생:`);
            console.log(`   상태 코드: ${response.status}`);
            console.log(`   응답 본문: ${errorText}`);
            return;
        }

        const data = await response.json();
        if (data.success) {
            console.log('\n✅ 강의 삭제 성공!');
        } else {
            console.log('❌ 삭제 실패:', data.message);
        }
    } catch (error) {
        console.log('❌ 에러 발생:', error.message);
    }
}

// ========== 도움말 ==========
function help() {
    console.log('\n📚 사용 가능한 함수:');
    console.log('checkAuth()          - 로그인 상태 확인');
    console.log('getLectures()        - 강의 목록 조회');
    console.log('getLectureDetail()   - 강의 상세 조회');
    console.log('getLectureStats()    - 강의 통계 조회');
    console.log('createLecture()      - 강의 생성');
    console.log('updateLecture()      - 강의 수정');
    console.log('deleteLecture()      - 강의 삭제');
}

// ========== 초기 메시지 ==========
console.log('✅ 관리자 강의 관리 테스트 로드 완료 (Phase 9)');

