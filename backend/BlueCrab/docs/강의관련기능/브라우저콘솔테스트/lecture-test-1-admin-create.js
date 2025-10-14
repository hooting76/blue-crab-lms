// ===================================================================
// 📚 관리자 강의 등록 테스트
// Blue Crab LMS - 관리자 강의 생성 및 관리 테스트
// 
// ⚠️ 사전 준비: 먼저 관리자 로그인 테스트 파일을 실행하세요!
// 📁 위치: docs/관리자 로그인/admin-login-to-board-test.js
// 📝 실행: adminLogin() 또는 quickAdminLogin()
// ===================================================================

const API_BASE_URL = 'https://bluecrab.chickenkiller.com/BlueCrab-1.0.0/api';

// ========== 로그인 상태 확인 ==========
function checkAuth() {
    console.log('\n🔍 로그인 상태 확인');
    console.log('═══════════════════════════════════════════════════════');
    
    const token = window.authToken || localStorage.getItem('jwtAccessToken');
    const user = window.currentUser;
    
    console.log(`🔑 JWT 토큰: ${token ? '보유 (' + token.substring(0, 20) + '...)' : '❌ 없음'}`);
    console.log(`👤 사용자 정보: ${user ? user.userName + ' (' + user.role + ')' : '❌ 없음'}`);
    
    if (!token) {
        console.log('\n⚠️ 로그인이 필요합니다!');
        console.log('🔧 해결 방법:');
        console.log('   1. docs/관리자 로그인/admin-login-to-board-test.js 파일 실행');
        console.log('   2. adminLogin() 또는 quickAdminLogin() 실행');
        console.log('   3. 로그인 완료 후 이 파일의 함수들 사용');
        return false;
    }
    
    console.log('\n✅ 로그인됨 - 테스트 실행 가능!');
    return true;
}

// ========== 강의 시간 포맷 변환 유틸리티 ==========
function convertLectureTimeFormat(input) {
    // "월1,2 수3,4" 형식을 "월1월2수3수4" 형식으로 변환
    // 공백, 쉼표 제거하고 교시 번호를 개별적으로 처리
    
    if (!input || input.trim() === '') {
        return '';
    }
    
    let result = '';
    
    // 정규식: 요일명(월/화/수/목/금) 뒤에 숫자들이 오는 패턴
    // 예: "월1,2,3" → ["월1", "월2", "월3"]
    const pattern = /([월화수목금])([0-9,\s]+)/g;
    let match;
    
    while ((match = pattern.exec(input)) !== null) {
        const dayName = match[1];  // 요일명 (월/화/수/목/금)
        const periods = match[2];  // 교시들 (예: "1,2,3" 또는 "1 2 3")
        
        // 교시 번호들 추출 (쉼표, 공백 기준으로 분리)
        const periodNumbers = periods.match(/\d/g);
        
        if (periodNumbers) {
            // 각 교시마다 "요일명+교시" 형태로 추가
            periodNumbers.forEach(period => {
                result += dayName + period;
            });
        }
    }
    
    return result;
}

// 사용 예시:
// convertLectureTimeFormat('월1,2 수3,4') → '월1월2수3수4'
// convertLectureTimeFormat('화2,3 목2') → '화2화3목2'
// convertLectureTimeFormat('월1월2월3월4') → '월1월2월3월4' (이미 올바른 형식)

// ========== 강의 등록 테스트 ==========
async function createLecture() {
    // 로그인 확인
    if (!checkAuth()) return;
    
    const token = window.authToken || localStorage.getItem('jwtAccessToken');
    
    const lectureName = prompt('📝 강의명을 입력하세요:', '자바 프로그래밍');
    const lectureCode = prompt('📝 강의 코드를 입력하세요:', 'CS101');
    const professorCode = prompt('👨‍🏫 담당교수 번호를 입력하세요 (예: PROF001):', 'PROF001');
    const lecPoint = parseInt(prompt('📊 이수학점을 입력하세요 (0~10):', '3')) || 3;
    const lecMajor = parseInt(prompt('� 전공구분 (1:전공강의, 0:교양):', '1')) || 1;
    const lecMust = parseInt(prompt('✅ 필수구분 (1:필수과목, 0:선택과목):', '1')) || 1;
    const lecSummary = prompt('📝 강의 개요를 입력하세요:', '') || `${lectureName} 강의입니다.`;
    const lecTime = prompt('⏰ 강의 시간을 입력하세요 (예: 월1,2 수3,4 또는 월1월2수3수4):', '월1,2 수3,4');
    const lecAssign = parseInt(prompt('📋 과제유무 (1:과제있음, 0:과제없음):', '0')) || 0;
    const lecOpen = parseInt(prompt('🔓 수강신청 상태 (1:열림, 0:닫힘):', '1')) || 1;
    const maxStudents = parseInt(prompt('👥 최대 정원을 입력하세요:', '30'));
    const lecMcode = prompt('🏛️ 학부 코드 (선택사항, 공란=모든학과가능, 예: CS):', '') || '';
    const lecMcodeDep = prompt('🎓 학과 코드 (선택사항, 공란=모든학과가능, 예: CS01):', '') || '';
    const lecMin = parseInt(prompt('📈 최저학년제한 (학기수, 0=제한없음):', '0')) || 0;
    const targetGrade = parseInt(prompt('🎓 대상 학년을 입력하세요 (1~4학년):', '1'));
    const semester = parseInt(prompt('📅 학기를 입력하세요 (1/2):', '1'));

    // 필수 입력값 검증 (학부코드, 학과코드는 선택사항)
    if (!lectureName || !lectureCode || !professorCode || !lecTime) {
        console.log('❌ 필수 입력값이 누락되었습니다.');
        console.log('필수: 강의명, 강의코드, 담당교수번호, 강의시간');
        console.log('선택: 학부코드, 학과코드 (공란 시 모든 학과 학생 수강 가능)');
        return;
    }

    // 입력값 유효성 검증
    if (lecPoint < 0 || lecPoint > 10) {
        console.log('❌ 이수학점은 0~10 사이여야 합니다.');
        return;
    }
    
    if (![0, 1].includes(lecMajor)) {
        console.log('❌ 전공구분은 0(교양) 또는 1(전공강의)이어야 합니다.');
        return;
    }
    
    if (![0, 1].includes(lecMust)) {
        console.log('❌ 필수구분은 0(선택과목) 또는 1(필수과목)이어야 합니다.');
        return;
    }
    
    if (![0, 1].includes(lecAssign)) {
        console.log('❌ 과제유무는 0(과제없음) 또는 1(과제있음)이어야 합니다.');
        return;
    }
    
    if (![0, 1].includes(lecOpen)) {
        console.log('❌ 수강신청상태는 0(닫힘) 또는 1(열림)이어야 합니다.');
        return;
    }
    
    if (maxStudents < 0 || maxStudents > 500) {
        console.log('❌ 최대 정원은 0~500 사이여야 합니다.');
        return;
    }
    
    if (targetGrade < 1 || targetGrade > 4) {
        console.log('❌ 대상학년은 1~4 사이여야 합니다.');
        return;
    }
    
    if (![1, 2].includes(semester)) {
        console.log('❌ 학기는 1(1학기) 또는 2(2학기)여야 합니다.');
        return;
    }

    console.log('\n📚 강의 등록 테스트');
    console.log('═══════════════════════════════════════════════════════');
    
    // 강의 시간 포맷 변환 (입력받은 형식을 표준 형식으로)
    const formattedLecTime = convertLectureTimeFormat(lecTime);
    console.log(`⏰ 원본 입력: "${lecTime}"`);
    console.log(`⏰ 변환 결과: "${formattedLecTime}"`);
    
    if (!formattedLecTime) {
        console.log('❌ 강의 시간 형식이 올바르지 않습니다.');
        console.log('올바른 형식 예시: "월1,2 수3,4" 또는 "월1월2수3수4"');
        return;
    }
    
    // LEC_TBL 테이블 구조에 맞춘 데이터 (사용자 입력값 사용)
    const lectureData = {
        lecSerial: lectureCode,           // 강의 코드 (필수)
        lecTit: lectureName,             // 강의명칭 (필수)
        lecProf: professorCode,          // 강의 담당교수번호 (필수, USER_NAME) - 사용자 입력
        lecPoint: lecPoint,              // 이수학점 (사용자 입력, 0~10)
        lecMajor: lecMajor,              // 전공강의:1 / 교양:0 (사용자 입력)
        lecMust: lecMust,                // 필수과목:1 / 선택과목:0 (사용자 입력)
        lecSummary: lecSummary,          // 강의 개요 내용 (사용자 입력)
        lecTime: formattedLecTime,       // 강의 시간 (필수, 변환된 형식)
        lecAssign: lecAssign,            // 과제있음:1 / 과제없음:0 (사용자 입력)
        lecOpen: lecOpen,                // 강의열림:1 / 강의닫힘:0 (사용자 입력)
        lecMany: maxStudents,            // 수강가능 인원수 (사용자 입력)
        lecMcode: lecMcode || 0,         // 학부 코드 (공란 시 0, 모든 학부 허용)
        lecMcodeDep: lecMcodeDep || 0,   // 학과 코드 (공란 시 0, 모든 학과 허용)
        lecMin: lecMin,                  // 수강 가능한 최저 학년 제한 (사용자 입력)
        lecCurrent: 0,                   // 현재 수강 인원 (자동 0)
        lecYear: targetGrade,            // 대상 학년 (1~4학년, 사용자 입력)
        lecSemester: semester            // 학기 (1학기:1, 2학기:2, 사용자 입력)
        // lecIdx: AUTO_INCREMENT (자동생성)
        // lecReg, lecIp: 서버에서 자동 설정
    };

    console.log('\n📤 전송할 강의 데이터:');
    console.log('═══════════════════════════════════════════════════════');
    console.log(`📝 강의코드: ${lectureData.lecSerial}`);
    console.log(`📚 강의명: ${lectureData.lecTit}`);
    console.log(`👨‍🏫 담당교수번호: ${lectureData.lecProf}`);
    console.log(`📊 학점: ${lectureData.lecPoint}점`);
    console.log(`🎯 전공구분: ${lectureData.lecMajor ? '전공강의' : '교양'}`);
    console.log(`✅ 필수구분: ${lectureData.lecMust ? '필수과목' : '선택과목'}`);
    console.log(`📋 과제유무: ${lectureData.lecAssign ? '과제있음' : '과제없음'}`);
    console.log(`🔓 수강신청: ${lectureData.lecOpen ? '열림' : '닫힘'}`);
    console.log(`⏰ 강의시간: ${lectureData.lecTime}`);
    console.log(`👥 정원: ${lectureData.lecMany}명`);
    console.log(`🏛️ 학부/학과: ${lectureData.lecMcode}/${lectureData.lecMcodeDep}`);
    console.log(`🎓 대상: ${lectureData.lecYear}학년 ${lectureData.lecSemester}학기`);
    console.log(`📝 개요: ${lectureData.lecSummary}`);
    console.log('');

try {
        const response = await fetch(`${API_BASE_URL}/lectures`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                'Authorization': `Bearer ${token}`
            },
            body: JSON.stringify(lectureData)
        });

        console.log(`📡 HTTP 상태: ${response.status}`);
        
        if (response.status === 201) {
            const result = await response.json();
            console.log('\n✅ 강의 등록 성공!');
            console.log('📊 등록된 강의:', result);
            console.log(`   - 강의 ID: ${result.lecIdx}`);
            console.log(`   - 강의 코드: ${result.lecSerial}`);
            console.log(`   - 강의명: ${result.lecTit}`);
            console.log(`   - 담당교수코드: ${result.lecProf}`);
            console.log(`   - 담당교수명: ${result.lecProfName || 'N/A'}`);
            console.log(`   - 대상학년: ${result.lecYear}학년 ${result.lecSemester}학기`);
            window.lastLectureIdx = result.lecIdx;  // LecTbl의 lecIdx 필드
            console.log(`💾 저장된 lecIdx: ${window.lastLectureIdx}`);
        } else {
            const errorResult = await response.json();
            console.log(`❌ 강의 등록 실패 [${response.status}]`);
            console.log('📋 오류 내용:', errorResult);
        }
    } catch (error) {
        console.log('❌ 에러 발생:', error.message);
    }
}

// ========== 강의 목록 조회 ==========
async function getLectures() {
    // 로그인 확인
    if (!checkAuth()) return;
    
    const token = window.authToken || localStorage.getItem('jwtAccessToken');
    
    const page = parseInt(prompt('📄 페이지 번호 (0부터 시작):', '0'));
    const size = parseInt(prompt('📄 페이지 크기:', '10'));
    const targetGrade = prompt('🎓 대상 학년 필터 (선택사항, 1~4):', '');
    const semester = prompt('📅 학기 필터 (선택사항, 1/2):', '');

    console.log('\n📚 강의 목록 조회');
    console.log('═══════════════════════════════════════════════════════');

    try {
        let url = `${API_BASE_URL}/lectures?page=${page}&size=${size}`;
        if (targetGrade) url += `&year=${targetGrade}`;  // 백엔드는 year 파라미터 사용
        if (semester) url += `&semester=${semester}`;

        console.log('📡 요청 URL:', url);

        const response = await fetch(url, {
            headers: {
                'Authorization': `Bearer ${token}`
            }
        });

        console.log(`📡 HTTP 상태: ${response.status}`);
        
        if (response.status === 200) {
            const result = await response.json();
            console.log('\n✅ 조회 성공!');
            console.log(`📊 총 ${result.totalElements}개 강의 (${result.totalPages}페이지)`);
            console.log('📋 강의 목록:');
            result.content.forEach((lecture, idx) => {
                console.log(`\n${idx + 1}. ${lecture.lecTit} (${lecture.lecSerial})`);
                console.log(`   교수코드: ${lecture.lecProf}`);
                console.log(`   교수명: ${lecture.lecProfName || 'N/A'}`);
                console.log(`   정원: ${lecture.lecCurrent || 0}/${lecture.lecMany}`);
                console.log(`   상태: ${lecture.lecOpen ? '수강신청 열림' : '수강신청 닫힘'}`);
                console.log(`   대상: ${lecture.lecYear}학년 ${lecture.lecSemester}학기`);
                console.log(`   학점: ${lecture.lecPoint}점 (${lecture.lecMajor ? '전공' : '교양'})`);
            });
        } else {
            const errorResult = await response.json();
            console.log(`❌ 조회 실패 [${response.status}]`);
            console.log('📋 오류 내용:', errorResult);
        }
    } catch (error) {
        console.log('❌ 에러 발생:', error.message);
    }
}

// ========== 강의 상세 조회 ==========
async function getLectureDetail() {
    // 로그인 확인
    if (!checkAuth()) return;
    
    const token = window.authToken || localStorage.getItem('jwtAccessToken');
    
    const lectureIdx = parseInt(prompt('🔍 조회할 LECTURE_IDX:', window.lastLectureIdx || '1'));

    console.log('\n📚 강의 상세 조회');
    console.log('═══════════════════════════════════════════════════════');

    try {
        const response = await fetch(`${API_BASE_URL}/lectures/${lectureIdx}`, {
            headers: {
                'Authorization': `Bearer ${token}`
            }
        });

        console.log(`📡 HTTP 상태: ${response.status}`);
        
        if (response.status === 200) {
            const result = await response.json();
            console.log('\n✅ 조회 성공!');
            console.log('📊 강의 상세 정보:');
            console.log(`   - 강의 ID: ${result.lecIdx}`);
            console.log(`   - 강의 코드: ${result.lecSerial}`);
            console.log(`   - 강의명: ${result.lecTit}`);
            console.log(`   - 담당교수코드: ${result.lecProf}`);
            console.log(`   - 담당교수명: ${result.lecProfName || 'N/A'}`);
            console.log(`   - 학점: ${result.lecPoint}점`);
            console.log(`   - 전공구분: ${result.lecMajor ? '전공강의' : '교양'}`);
            console.log(`   - 필수구분: ${result.lecMust ? '필수과목' : '선택과목'}`);
            console.log(`   - 과제유무: ${result.lecAssign ? '과제있음' : '과제없음'}`);
            console.log(`   - 수강신청: ${result.lecOpen ? '열림' : '닫힘'}`);
            console.log(`   - 정원: ${result.lecCurrent || 0}/${result.lecMany}`);
            console.log(`   - 강의시간: ${result.lecTime}`);
            console.log(`   - 학부/학과: ${result.lecMcode}/${result.lecMcodeDep}`);
            console.log(`   - 대상: ${result.lecYear}학년 ${result.lecSemester}학기`);
            console.log(`   - 개요: ${result.lecSummary || '없음'}`);
        } else if (response.status === 404) {
            console.log('❌ 해당 강의를 찾을 수 없습니다.');
        } else {
            const errorResult = await response.json();
            console.log(`❌ 조회 실패 [${response.status}]`);
            console.log('📋 오류 내용:', errorResult);
        }
    } catch (error) {
        console.log('❌ 에러 발생:', error.message);
    }
}

// ========== 강의 수정 ==========
async function updateLecture() {
    // 로그인 확인
    if (!checkAuth()) return;
    
    const token = window.authToken || localStorage.getItem('jwtAccessToken');
    
    const lectureIdx = parseInt(prompt('✏️ 수정할 강의 IDX (lecIdx):', window.lastLectureIdx || '1'));
    const lectureName = prompt('📝 새 강의명 (lecTit, 빈값=수정안함):');
    const professorCode = prompt('👨‍🏫 새 담당교수번호 (lecProf, 예: PROF001, 빈값=수정안함):');
    const lecPoint = prompt('📊 새 이수학점 (lecPoint, 0~10, 빈값=수정안함):');
    const lecMajor = prompt('🎯 전공구분 (lecMajor, 1:전공 0:교양, 빈값=수정안함):');
    const lecMust = prompt('✅ 필수구분 (lecMust, 1:필수 0:선택, 빈값=수정안함):');
    const lecSummary = prompt('📝 강의개요 (lecSummary, 빈값=수정안함):');
    const lecTime = prompt('⏰ 강의시간 (lecTime, 빈값=수정안함):');
    const lecAssign = prompt('📋 과제유무 (lecAssign, 1:있음 0:없음, 빈값=수정안함):');
    const lecOpen = prompt('🔓 수강신청상태 (lecOpen, 1:열림 0:닫힘, 빈값=수정안함):');
    const maxStudents = prompt('👥 수강정원 (lecMany, 0~500, 빈값=수정안함):');
    const lecMcode = prompt('🏛️ 학부코드 (lecMcode, 빈값=수정안함):');
    const lecMcodeDep = prompt('🎓 학과코드 (lecMcodeDep, 빈값=수정안함):');
    const lecMin = prompt('📈 최저학년제한 (lecMin, 학기수, 빈값=수정안함):');
    const lecYear = prompt('🎓 대상학년 (lecYear, 1~4, 빈값=수정안함):');
    const lecSemester = prompt('📅 학기 (lecSemester, 1:1학기 2:2학기, 빈값=수정안함):');

    console.log('\n📚 강의 수정');
    console.log('═══════════════════════════════════════════════════════');

    // LEC_TBL 필드명에 맞춘 수정 데이터
    const updateData = {};
    if (lectureName) updateData.lecTit = lectureName;
    if (professorCode) updateData.lecProf = professorCode;
    if (lecPoint) {
        const point = parseInt(lecPoint);
        if (point >= 0 && point <= 10) {
            updateData.lecPoint = point;
        } else {
            console.log('❌ 이수학점은 0~10 사이여야 합니다.');
            return;
        }
    }
    if (lecMajor) {
        const major = parseInt(lecMajor);
        if ([0, 1].includes(major)) {
            updateData.lecMajor = major;
        } else {
            console.log('❌ 전공구분은 0(교양) 또는 1(전공강의)이어야 합니다.');
            return;
        }
    }
    if (lecMust) {
        const must = parseInt(lecMust);
        if ([0, 1].includes(must)) {
            updateData.lecMust = must;
        } else {
            console.log('❌ 필수구분은 0(선택과목) 또는 1(필수과목)이어야 합니다.');
            return;
        }
    }
    if (lecSummary) updateData.lecSummary = lecSummary;
    if (lecTime) updateData.lecTime = lecTime;
    if (lecAssign) {
        const assign = parseInt(lecAssign);
        if ([0, 1].includes(assign)) {
            updateData.lecAssign = assign;
        } else {
            console.log('❌ 과제유무는 0(과제없음) 또는 1(과제있음)이어야 합니다.');
            return;
        }
    }
    if (lecOpen) {
        const open = parseInt(lecOpen);
        if ([0, 1].includes(open)) {
            updateData.lecOpen = open;
        } else {
            console.log('❌ 수강신청상태는 0(닫힘) 또는 1(열림)이어야 합니다.');
            return;
        }
    }
    if (maxStudents) {
        const many = parseInt(maxStudents);
        if (many >= 0 && many <= 500) {
            updateData.lecMany = many;
        } else {
            console.log('❌ 수강정원은 0~500 사이여야 합니다.');
            return;
        }
    }
    if (lecMcode) updateData.lecMcode = lecMcode;
    if (lecMcodeDep) updateData.lecMcodeDep = lecMcodeDep;
    if (lecMin) {
        const min = parseInt(lecMin);
        if (min >= 0) {
            updateData.lecMin = min;
        } else {
            console.log('❌ 최저학년제한은 0 이상이어야 합니다.');
            return;
        }
    }
    if (lecYear) {
        const year = parseInt(lecYear);
        if (year >= 1 && year <= 4) {
            updateData.lecYear = year;
        } else {
            console.log('❌ 대상학년은 1~4 사이여야 합니다.');
            return;
        }
    }
    if (lecSemester) {
        const semester = parseInt(lecSemester);
        if ([1, 2].includes(semester)) {
            updateData.lecSemester = semester;
        } else {
            console.log('❌ 학기는 1(1학기) 또는 2(2학기)여야 합니다.');
            return;
        }
    }

    if (Object.keys(updateData).length === 0) {
        console.log('❌ 수정할 내용이 없습니다.');
        return;
    }

    console.log('\n📤 수정할 데이터:');
    Object.keys(updateData).forEach(key => {
        console.log(`   ${key}: ${updateData[key]}`);
    });
    console.log('');

try {
        const response = await fetch(`${API_BASE_URL}/lectures/${lectureIdx}`, {
            method: 'PUT',
            headers: {
                'Content-Type': 'application/json',
                'Authorization': `Bearer ${token}`
            },
            body: JSON.stringify(updateData)
        });

        console.log(`📡 HTTP 상태: ${response.status}`);
        
        if (response.status === 200) {
            const result = await response.json();
            console.log('\n✅ 수정 성공!');
            console.log('📊 수정된 강의:', result);
        } else {
            const errorResult = await response.json();
            console.log(`❌ 수정 실패 [${response.status}]`);
            console.log('📋 오류 내용:', errorResult);
        }
    } catch (error) {
        console.log('❌ 에러 발생:', error.message);
    }
}

// ========== 강의 삭제 (폐강) ==========
async function deleteLecture() {
    // 로그인 확인
    if (!checkAuth()) return;
    
    const token = window.authToken || localStorage.getItem('jwtAccessToken');
    
    const lectureIdx = parseInt(prompt('🗑️ 삭제할 LECTURE_IDX:', window.lastLectureIdx || '1'));
    const confirm = prompt('⚠️ 정말 삭제하시겠습니까? (yes/no):', 'no');

    if (confirm.toLowerCase() !== 'yes') {
        console.log('❌ 삭제 취소됨');
        return;
    }

    console.log('\n📚 강의 삭제');
    console.log('═══════════════════════════════════════════════════════');

    try {
        const response = await fetch(`${API_BASE_URL}/lectures/${lectureIdx}`, {
            method: 'DELETE',
            headers: {
                'Authorization': `Bearer ${token}`
            }
        });

        console.log(`📡 HTTP 상태: ${response.status}`);
        
        if (response.status === 200) {
            const result = await response.json();
            console.log('\n✅ 삭제 성공!');
            console.log('📊 결과:', result.message);
        } else {
            const errorResult = await response.json();
            console.log(`❌ 삭제 실패 [${response.status}]`);
            console.log('📋 오류 내용:', errorResult);
        }
    } catch (error) {
        console.log('❌ 에러 발생:', error.message);
    }
}

// ========== 도움말 ==========
function help() {
    console.log('\n📚 관리자 강의 관리 테스트 함수 목록');
    console.log('═══════════════════════════════════════════════════════');
    console.log('� checkAuth()        - 로그인 상태 확인');
    console.log('📝 createLecture()    - 강의 등록');
    console.log('📋 getLectures()      - 강의 목록 조회');
    console.log('🔍 getLectureDetail() - 강의 상세 조회');
    console.log('✏️ updateLecture()    - 강의 수정');
    console.log('🗑️ deleteLecture()    - 강의 삭제');
    console.log('═══════════════════════════════════════════════════════');
    console.log('⚠️ 사전 준비:');
    console.log('   1. docs/관리자 로그인/admin-login-to-board-test.js 실행');
    console.log('   2. adminLogin() 또는 quickAdminLogin() 실행');
    console.log('   3. 로그인 완료 후 이 파일의 함수들 사용');
}

// 초기 메시지
console.log('✅ 관리자 강의 관리 테스트 스크립트 로드 완료!');
console.log('💡 help() 를 입력하면 사용 가능한 함수 목록을 볼 수 있습니다.');
console.log('⚠️ 먼저 관리자 로그인을 완료하세요! (checkAuth()로 확인 가능)');
