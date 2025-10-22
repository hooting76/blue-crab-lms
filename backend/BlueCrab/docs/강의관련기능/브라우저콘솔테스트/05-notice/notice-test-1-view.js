/**
 * 📢 안내문 조회 테스트 (공개)
 * 
 * 인증 불필요 - 누구나 현재 안내문 조회 가능
 * 
 * 사용법:
 * 1. 브라우저 콘솔에 이 코드 복사/붙여넣기
 * 2. await testViewNotice() 실행
 */

const API_BASE_URL = 'https://bluecrab.chickenkiller.com/BlueCrab-1.0.0';

/**
 * 안내문 조회 (공개)
 */
async function testViewNotice() {
  console.log('🔍 안내문 조회 테스트 시작...\n');
  
  try {
    const response = await fetch(`${API_BASE_URL}/api/notice/course-apply/view`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json'
      }
    });

    console.log('📡 응답 상태:', response.status);

    const data = await response.json();
    console.log('📋 응답 데이터:', data);

    if (response.ok && data.success) {
      console.log('\n✅ 조회 성공!\n');
      console.log('📄 안내문 내용:');
      console.log(data.message);
      console.log('\n⏰ 최종 수정:', data.updatedAt);
      console.log('👤 수정자:', data.updatedBy);
      
      return data;
    } else {
      console.warn('\n⚠️ 안내문 없음');
      console.log('메시지:', data.message);
      
      return null;
    }

  } catch (error) {
    console.error('\n❌ 조회 실패:', error.message);
    console.error('상세:', error);
    throw error;
  }
}

/**
 * 간편 실행 함수
 */
async function viewNotice() {
  return await testViewNotice();
}

console.log('📢 안내문 조회 테스트 로드 완료');
console.log('실행: await testViewNotice()');
console.log('또는: await viewNotice()');
