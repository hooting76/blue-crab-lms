/**
 * 📊 출석 승인 + 성적 자동 재계산 통합 테스트
 * 
 * 🚀 사용법:
 *    await login()  // 먼저 로그인!
 *    await testAttendanceGradeIntegration("ETH201", 6, 3)
 * 
 * ✅ 개선 사항 (2025-10-24):
 *    - 신버전 API 사용: /api/attendance/approve + /api/enrollments/grade-info
 *    - 간소화된 테스트 구조 (출석 승인 + 성적 재계산 검증)
 *    - Bearer 토큰 인증 (window.authToken 사용)
 */

// ========== 신버전 출석 + 성적 자동 재계산 통합 테스트 ==========

const API_BASE = 'https://bluecrab.chickenkiller.com/BlueCrab-1.0.0';

// 토큰 가져오기 함수
function getToken() {
  return window.authToken || localStorage.getItem('jwtAccessToken');
}

// 전체 테스트 함수
async function testAttendanceGradeIntegration(lecSerial, studentIdx, sessionNumber) {
  console.log('\n🧪 출석 승인 + 성적 자동 재계산 통합 테스트');
  console.log('━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n');
  
  const token = getToken();
  if (!token) {
    console.error('❌ 로그인 필요! await login() 실행하세요.');
    return;
  }
  
  // 1. 승인 전 성적 조회
  console.log('📊 1. 승인 전 성적 조회...');
  const beforeGrade = await fetch(`${API_BASE}/api/enrollments/grade-info`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      'Authorization': `Bearer ${token}`
    },
    body: JSON.stringify({
      action: 'get-grade',
      lecSerial,
      studentIdx
    })
  }).then(r => r.json());
  
  const beforeScore = beforeGrade.data?.attendanceScore || 0;
  const beforeSessions = beforeGrade.data?.presentCount || 0;
  console.log(`   현재 점수: ${beforeScore}`);
  console.log(`   출석 회차: ${beforeSessions}회\n`);
  
  // 2. 출석 승인
  console.log('✅ 2. 출석 승인 실행...');
  const approveResult = await fetch(`${API_BASE}/api/attendance/approve`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      'Authorization': `Bearer ${token}`
    },
    body: JSON.stringify({
      lecSerial,
      sessionNumber,
      attendanceRecords: [{studentIdx, status: '출'}]
    })
  }).then(r => r.json());
  
  if (!approveResult.success) {
    console.error('❌ 승인 실패:', approveResult.message);
    return;
  }
  console.log(`   승인 완료: ${approveResult.message}\n`);
  
  // 3. 승인 후 성적 조회 (2초 대기)
  console.log('⏳ 3. 성적 재계산 대기 (2초)...');
  await new Promise(r => setTimeout(r, 2000));
  
  const afterGrade = await fetch(`${API_BASE}/api/enrollments/grade-info`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      'Authorization': `Bearer ${token}`
    },
    body: JSON.stringify({
      action: 'get-grade',
      lecSerial,
      studentIdx
    })
  }).then(r => r.json());
  
  const afterScore = afterGrade.data?.attendanceScore || 0;
  const afterSessions = afterGrade.data?.presentCount || 0;
  console.log(`   현재 점수: ${afterScore}`);
  console.log(`   출석 회차: ${afterSessions}회\n`);
  
  // 4. 결과 비교
  console.log('━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━');
  console.log('📈 결과:');
  console.log(`   점수 변화: ${beforeScore} → ${afterScore} (+${(afterScore - beforeScore).toFixed(4)})`);
  console.log(`   회차 증가: ${beforeSessions} → ${afterSessions}`);
  
  const expectedScore = (afterSessions / 80 * 77).toFixed(4);
  console.log(`   예상 점수: ${expectedScore}`);
  console.log(`   일치 여부: ${Math.abs(afterScore - parseFloat(expectedScore)) < 0.01 ? '✅' : '❌'}`);
  console.log('━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n');
}

// 실행 예시
console.log('✅ 출석 + 성적 통합 테스트 로드 완료 (2025-10-24)');
console.log('━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━');
console.log('🚀 사용법:');
console.log('   1. await login()  // 먼저 로그인!');
console.log('   2. await testAttendanceGradeIntegration("ETH201", 6, 3)');
console.log('');
console.log('📝 파라미터:');
console.log('   - lecSerial: 강의 코드 (예: "ETH201")');
console.log('   - studentIdx: 학생 IDX (예: 6)');
console.log('   - sessionNumber: 승인할 회차 번호 (예: 3)');
console.log('');
console.log('📡 사용 API:');
console.log('   - /api/enrollments/grade-info (성적 조회)');
console.log('   - /api/attendance/approve (출석 승인)');
console.log('━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━');
