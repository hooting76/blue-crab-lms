/**
 * 📢 안내문 저장 테스트 (관리자/교수)
 * 
 * 권한 필요: ROLE_ADMIN 또는 ROLE_PROFESSOR
 * 
 * 사용법:
 * 1. 관리자 로그인 (00-login/admin-login.js)
 * 2. 브라우저 콘솔에 이 코드 복사/붙여넣기
 * 3. await testSaveNotice() 실행 (프롬프트에서 메시지 입력)
 * 
 * 간편 함수: save(), saveAndView(), testSample(), clear()
 */

const API_BASE_URL = 'https://bluecrab.chickenkiller.com/BlueCrab-1.0.0';

/**
 * 안내문 저장 (관리자/교수)
 * 
 * @param {string} message - 안내문 내용 (생략 시 프롬프트로 입력)
 */
async function testSaveNotice(message) {
  console.log('💾 안내문 저장 테스트 시작...\n');
  
  // JWT 토큰 확인
  const token = window.authToken || localStorage.getItem('authToken');
  
  if (!token) {
    console.error('❌ JWT 토큰이 없습니다. 먼저 로그인하세요.');
    console.log('힌트: await adminLogin() 실행');
    return;
  }

  // 메시지 입력 (매개변수가 없으면 프롬프트로 입력)
  if (!message) {
    message = prompt('📝 안내문 내용을 입력하세요:');
    if (!message) {
      console.error('❌ 안내문 내용이 입력되지 않았습니다.');
      return;
    }
  }

  // 메시지 검증
  if (message.trim().length === 0) {
    console.error('❌ 안내문 내용을 입력하세요.');
    return;
  }

  try {
    console.log('📝 저장할 내용:');
    console.log(message);
    console.log('\n📡 요청 전송 중...');

    const response = await fetch(`${API_BASE_URL}/notice/course-apply/save`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${token}`
      },
      body: JSON.stringify({ message })
    });

    console.log('📡 응답 상태:', response.status);

    const data = await response.json();
    console.log('📋 응답 데이터:', data);

    if (response.ok && data.success) {
      console.log('\n✅ 저장 성공!\n');
      console.log('📋 저장된 정보:');
      console.log('  - ID:', data.data.noticeIdx);
      console.log('  - 메시지:', data.data.message);
      console.log('  - 최종 수정:', data.data.updatedAt);
      console.log('  - 수정자:', data.data.updatedBy);
      
      return data;
    } else {
      console.error('\n❌ 저장 실패');
      console.log('메시지:', data.message);
      
      if (response.status === 401) {
        console.log('\n💡 해결 방법:');
        console.log('  1. await adminLogin() 실행');
        console.log('  2. JWT 토큰 만료 여부 확인');
      }
      
      return null;
    }

  } catch (error) {
    console.error('\n❌ 저장 실패:', error.message);
    console.error('상세:', error);
    throw error;
  }
}

/**
 * 안내문 저장 후 즉시 조회
 * 
 * @param {string} message - 안내문 내용 (생략 시 프롬프트로 입력)
 */
async function saveAndViewNotice(message) {
  console.log('🔄 저장 후 조회 테스트 시작...\n');
  
  // 메시지 입력 (매개변수가 없으면 프롬프트로 입력)
  if (!message) {
    message = prompt('📝 안내문 내용을 입력하세요:');
    if (!message) {
      console.error('❌ 안내문 내용이 입력되지 않았습니다.');
      return;
    }
  }
  
  // 1. 저장
  const saveResult = await testSaveNotice(message);
  
  if (!saveResult) {
    console.error('❌ 저장 실패로 조회를 건너뜁니다.');
    return;
  }
  
  console.log('\n⏳ 2초 대기 중...');
  await new Promise(resolve => setTimeout(resolve, 2000));
  
  // 2. 조회
  console.log('\n📢 저장된 안내문 조회 중...\n');
  const response = await fetch(`${API_BASE_URL}/notice/course-apply/view`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json'
    }
  });
  
  const viewData = await response.json();
  
  if (response.ok && viewData.success) {
    console.log('✅ 조회 성공!\n');
    console.log('📄 현재 안내문:');
    console.log(viewData.message);
    console.log('\n⏰ 최종 수정:', viewData.updatedAt);
    console.log('👤 수정자:', viewData.updatedBy);
  }
  
  return { save: saveResult, view: viewData };
}

/**
 * 안내문 삭제 (빈 메시지로 클리어)
 */
async function clearNotice() {
  console.log('🗑️ 안내문 클리어 (빈 내용으로 업데이트)...\n');
  console.log('⚠️ 주의: 실제로는 삭제가 아닌 빈 메시지로 업데이트됩니다.');
  console.log('데이터베이스에서 레코드는 유지됩니다.\n');
  
  const confirmClear = window.confirm('정말 안내문을 클리어하시겠습니까?');
  if (!confirmClear) {
    console.log('❌ 클리어가 취소되었습니다.');
    return null;
  }
  
  return await testSaveNotice('(안내문 없음 - 시스템 메시지)');
}

/**
 * 샘플 안내문으로 테스트
 */
async function testSampleNotice() {
  const sampleMessage = `2025학년도 1학기 수강신청 안내

📅 수강신청 기간
- 1차: 2025년 2월 1일 ~ 2월 7일
- 2차: 2025년 2월 10일 ~ 2월 14일

📝 주의사항
- 최대 신청학점: 21학점
- 선착순 마감 (정원 초과 시 자동 마감)
- 수강신청 기간 외 신청 불가

💡 문의사항
- 학사지원팀: 02-1234-5678
- 이메일: academic@bluecrab.edu`;

  return await testSaveNotice(sampleMessage);
}

/**
 * 간편 실행 함수들 (메인 함수들의 alias)
 */
async function save(message) {
  return await testSaveNotice(message);
}

async function saveAndView(message) {
  return await saveAndViewNotice(message);
}

async function testSample() {
  return await testSampleNotice();
}

async function clear() {
  return await clearNotice();
}

console.log('📢 안내문 저장 테스트 로드 완료');
console.log('\n📋 사용 가능한 함수:');
console.log('  1. await testSaveNotice()            - 프롬프트로 메시지 입력 후 저장');
console.log('  2. await testSaveNotice("메시지")    - 직접 메시지 전달하여 저장');
console.log('  3. await saveAndViewNotice()         - 프롬프트로 입력 후 저장+조회');
console.log('  4. await testSampleNotice()          - 샘플 안내문 저장');
console.log('  5. await clearNotice()               - 안내문 클리어 (빈 메시지)');
console.log('\n💡 간편 함수 (짧은 이름):');
console.log('  - await save("메시지")               - 저장');
console.log('  - await saveAndView("메시지")        - 저장+조회');
console.log('  - await testSample()                 - 샘플 저장');
console.log('  - await clear()                      - 클리어');
console.log('\n💡 먼저 관리자 로그인이 필요합니다: await adminLogin()');
