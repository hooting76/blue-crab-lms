/**
 * FCM 관리자 기능 테스트 스크립트 (브라우저 콘솔용)
 *
 * 사용법:
 * 1. 관리자 계정으로 로그인 후 브라우저 콘솔을 엽니다
 * 2. 이 스크립트를 복사해서 붙여넣기 합니다
 * 3. FCMAdmin.init('admin-jwt-token') 으로 관리자 토큰을 설정합니다
 * 4. FCMAdmin.send() 등의 메서드를 호출합니다
 */

const FCMAdmin = {
  token: null,
  apiUrl: 'https://bluecrab.chickenkiller.com/BlueCrab-1.0.0/api/fcm', // 실제 API 주소로 변경하세요

  /**
   * 관리자 토큰 설정
   */
  init(jwtToken) {
    this.token = jwtToken;
    console.log('✅ 관리자 JWT 토큰 설정 완료');
    console.log('\n📋 사용 가능한 명령어:');
    console.log('- FCMAdmin.stats() - FCM 통계 조회');
    console.log('- FCMAdmin.send(userCode, title, body, data) - 특정 사용자에게 알림 전송');
    console.log('- FCMAdmin.batch(userCodes, title, body, platforms, data) - 여러 사용자에게 일괄 전송');
    console.log('- FCMAdmin.broadcast(title, body, platforms, filter, data) - 전체 브로드캐스트');
    console.log('\n💡 도움말: FCMAdmin.help() 입력 시 예제 확인');
  },

  /**
   * HTTP 요청 헬퍼
   */
  async request(method, endpoint, body = null) {
    if (!this.token) {
      console.error('❌ 관리자 토큰이 설정되지 않았습니다. FCMAdmin.init("admin-token")을 먼저 실행하세요.');
      return;
    }

    const options = {
      method,
      headers: {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${this.token}`
      }
    };

    if (body) {
      options.body = JSON.stringify(body);
    }

    try {
      const response = await fetch(this.apiUrl + endpoint, options);
      const data = await response.json();

      if (response.ok) {
        console.log('✅ 성공:', data);
        return data;
      } else {
        console.error('❌ 실패:', data);
        return data;
      }
    } catch (error) {
      console.error('❌ 네트워크 오류:', error);
    }
  },

  /**
   * FCM 통계 조회
   */
  async stats() {
    console.log('\n📊 FCM 통계 조회 중...');
    const result = await this.request('GET', '/stats');

    if (result?.data) {
      console.log('\n📈 통계 요약:');
      console.log(`- 전체 사용자: ${result.data.totalUsers}명`);
      console.log(`- FCM 등록 사용자: ${result.data.registeredUsers}명`);
      console.log('\n플랫폼별 토큰 수:');
      console.log(`- Android: ${result.data.byPlatform.android}개`);
      console.log(`- iOS: ${result.data.byPlatform.ios}개`);
      console.log(`- Web: ${result.data.byPlatform.web}개`);
      console.log('\n활성 토큰 (30일 이내):');
      console.log(`- Android: ${result.data.activeTokens.android}개`);
      console.log(`- iOS: ${result.data.activeTokens.ios}개`);
      console.log(`- Web: ${result.data.activeTokens.web}개`);
      console.log('\n비활성 토큰 (90일 이상):');
      console.log(`- Android: ${result.data.inactiveTokens.android}개`);
      console.log(`- iOS: ${result.data.inactiveTokens.ios}개`);
      console.log(`- Web: ${result.data.inactiveTokens.web}개`);
    }

    return result;
  },

  /**
   * 특정 사용자에게 알림 전송
   * @param {string} userCode - 사용자 코드
   * @param {string} title - 알림 제목
   * @param {string} body - 알림 내용
   * @param {object} data - 추가 데이터 (선택)
   */
  async send(userCode, title, body, data = null) {
    if (!userCode || !title || !body) {
      console.error('❌ userCode, title, body는 필수입니다.');
      console.log('사용법: FCMAdmin.send("user001", "제목", "내용", {key: "value"})');
      return;
    }

    console.log(`\n📤 알림 전송 중...`);
    console.log(`대상: ${userCode}`);
    console.log(`제목: ${title}`);
    console.log(`내용: ${body}`);
    if (data) console.log(`추가 데이터:`, data);

    const result = await this.request('POST', '/send', {
      userCode: userCode,
      title: title,
      body: body,
      data: data
    });

    if (result?.data) {
      console.log('\n📊 전송 결과:');
      console.log(`- Android: ${result.data.sent.android ? '✅ 성공' : '❌ 실패'}`);
      console.log(`- iOS: ${result.data.sent.ios ? '✅ 성공' : '❌ 실패'}`);
      console.log(`- Web: ${result.data.sent.web ? '✅ 성공' : '❌ 실패'}`);

      if (result.data.failedReasons && Object.keys(result.data.failedReasons).length > 0) {
        console.log('\n❌ 실패 사유:');
        Object.entries(result.data.failedReasons).forEach(([platform, reason]) => {
          console.log(`- ${platform}: ${reason}`);
        });
      }
    }

    return result;
  },

  /**
   * 여러 사용자에게 일괄 알림 전송
   * @param {string[]} userCodes - 사용자 코드 배열
   * @param {string} title - 알림 제목
   * @param {string} body - 알림 내용
   * @param {string[]} platforms - 플랫폼 배열 (선택, 기본: 전체)
   * @param {object} data - 추가 데이터 (선택)
   */
  async batch(userCodes, title, body, platforms = null, data = null) {
    if (!userCodes || !Array.isArray(userCodes) || userCodes.length === 0) {
      console.error('❌ userCodes는 배열이어야 하며 최소 1개 이상의 사용자가 필요합니다.');
      console.log('사용법: FCMAdmin.batch(["user001", "user002"], "제목", "내용")');
      return;
    }

    if (!title || !body) {
      console.error('❌ title, body는 필수입니다.');
      return;
    }

    console.log(`\n📤 일괄 알림 전송 중...`);
    console.log(`대상: ${userCodes.length}명`);
    console.log(`제목: ${title}`);
    console.log(`내용: ${body}`);
    if (platforms) console.log(`플랫폼: ${platforms.join(', ')}`);
    if (data) console.log(`추가 데이터:`, data);

    const result = await this.request('POST', '/send/batch', {
      userCodes: userCodes,
      title: title,
      body: body,
      platforms: platforms,
      data: data
    });

    if (result?.data) {
      console.log('\n📊 전송 결과:');
      console.log(`- 총 사용자: ${result.data.totalUsers}명`);
      console.log(`- 성공: ${result.data.successCount}건`);
      console.log(`- 실패: ${result.data.failureCount}건`);

      if (result.data.details && result.data.details.length > 0) {
        console.log('\n📋 사용자별 상세:');
        result.data.details.forEach(detail => {
          const platforms = Object.entries(detail.sent)
            .map(([p, success]) => `${p}: ${success ? '✅' : '❌'}`)
            .join(', ');
          console.log(`- ${detail.userCode}: ${platforms}`);
        });
      }
    }

    return result;
  },

  /**
   * 전체 브로드캐스트 알림 전송
   * @param {string} title - 알림 제목
   * @param {string} body - 알림 내용
   * @param {string[]} platforms - 플랫폼 배열 (선택, 기본: 전체)
   * @param {object} filter - 필터 조건 (선택, 예: {userType: "STUDENT"})
   * @param {object} data - 추가 데이터 (선택)
   */
  async broadcast(title, body, platforms = null, filter = null, data = null) {
    if (!title || !body) {
      console.error('❌ title, body는 필수입니다.');
      console.log('사용법: FCMAdmin.broadcast("제목", "내용", ["WEB", "ANDROID"])');
      return;
    }

    console.log(`\n📡 브로드캐스트 알림 전송 중...`);
    console.log(`제목: ${title}`);
    console.log(`내용: ${body}`);
    if (platforms) console.log(`플랫폼: ${platforms.join(', ')}`);
    if (filter) console.log(`필터:`, filter);
    if (data) console.log(`추가 데이터:`, data);

    const result = await this.request('POST', '/send/broadcast', {
      title: title,
      body: body,
      platforms: platforms,
      filter: filter,
      data: data
    });

    if (result?.data) {
      console.log('\n📊 전송 결과:');
      console.log(`- 총 토큰: ${result.data.totalTokens}개`);
      console.log(`- 성공: ${result.data.successCount}건`);
      console.log(`- 실패: ${result.data.failureCount}건`);

      if (result.data.invalidTokens && result.data.invalidTokens.length > 0) {
        console.log(`- 무효화된 토큰: ${result.data.invalidTokens.length}개 (자동 삭제됨)`);
      }
    }

    return result;
  },

  /**
   * 도움말 출력
   */
  help() {
    console.log('='.repeat(70));
    console.log('📱 FCM 관리자 기능 테스트 가이드');
    console.log('='.repeat(70));

    console.log('\n1️⃣ 통계 조회');
    console.log('```javascript');
    console.log('FCMAdmin.stats()');
    console.log('```');

    console.log('\n2️⃣ 특정 사용자에게 알림 전송');
    console.log('```javascript');
    console.log('// 기본 전송');
    console.log('FCMAdmin.send("user001", "공지사항", "새로운 과제가 등록되었습니다")');
    console.log('');
    console.log('// 추가 데이터 포함');
    console.log('FCMAdmin.send(');
    console.log('  "user001",');
    console.log('  "과제 알림",');
    console.log('  "과제 제출 기한이 임박했습니다",');
    console.log('  { assignmentId: "123", type: "deadline" }');
    console.log(')');
    console.log('```');

    console.log('\n3️⃣ 여러 사용자에게 일괄 전송');
    console.log('```javascript');
    console.log('// 전체 플랫폼 전송');
    console.log('FCMAdmin.batch(');
    console.log('  ["user001", "user002", "user003"],');
    console.log('  "시험 공지",');
    console.log('  "중간고사 일정이 확정되었습니다"');
    console.log(')');
    console.log('');
    console.log('// 특정 플랫폼만 전송');
    console.log('FCMAdmin.batch(');
    console.log('  ["user001", "user002"],');
    console.log('  "긴급 공지",');
    console.log('  "오늘 오후 서버 점검 예정",');
    console.log('  ["WEB", "ANDROID"],  // iOS 제외');
    console.log('  { urgent: true }');
    console.log(')');
    console.log('```');

    console.log('\n4️⃣ 전체 브로드캐스트');
    console.log('```javascript');
    console.log('// 전체 사용자에게 전송');
    console.log('FCMAdmin.broadcast(');
    console.log('  "시스템 점검 안내",');
    console.log('  "2025년 1월 15일 새벽 2시-4시 서버 점검 예정"');
    console.log(')');
    console.log('');
    console.log('// 학생만 필터링');
    console.log('FCMAdmin.broadcast(');
    console.log('  "수강신청 안내",');
    console.log('  "수강신청이 시작되었습니다",');
    console.log('  ["WEB", "ANDROID", "IOS"],');
    console.log('  { userType: "STUDENT" },  // STUDENT, TEACHER, ALL');
    console.log('  { link: "/courses" }');
    console.log(')');
    console.log('');
    console.log('// 모바일만 전송');
    console.log('FCMAdmin.broadcast(');
    console.log('  "앱 업데이트",');
    console.log('  "새로운 버전이 출시되었습니다",');
    console.log('  ["ANDROID", "IOS"]  // WEB 제외');
    console.log(')');
    console.log('```');

    console.log('\n5️⃣ 연속 테스트 시나리오');
    console.log('```javascript');
    console.log('// 1. 통계 확인');
    console.log('await FCMAdmin.stats()');
    console.log('');
    console.log('// 2. 테스트 알림 전송');
    console.log('await FCMAdmin.send("user001", "테스트", "알림 테스트입니다")');
    console.log('');
    console.log('// 3. 일괄 전송 테스트');
    console.log('await FCMAdmin.batch(');
    console.log('  ["user001", "user002"],');
    console.log('  "일괄 테스트",');
    console.log('  "여러 명에게 동시 전송"');
    console.log(')');
    console.log('');
    console.log('// 4. 브로드캐스트 테스트');
    console.log('await FCMAdmin.broadcast(');
    console.log('  "전체 공지",');
    console.log('  "모든 사용자에게 전송",');
    console.log('  null,  // 모든 플랫폼');
    console.log('  null,  // 필터 없음');
    console.log('  { testMode: true }');
    console.log(')');
    console.log('');
    console.log('// 5. 통계 재확인 (성공/실패 확인)');
    console.log('await FCMAdmin.stats()');
    console.log('```');

    console.log('\n💡 추가 팁:');
    console.log('- 모든 함수는 Promise를 반환하므로 await 사용 가능');
    console.log('- 브라우저 콘솔에서 바로 실행하면 결과가 콘솔에 출력됨');
    console.log('- API URL은 FCMAdmin.apiUrl에서 변경 가능');
    console.log('- 실패 사유는 failedReasons에서 확인 가능');
    console.log('');
    console.log('='.repeat(70));
  }
};

// 초기 안내 메시지
console.log('='.repeat(70));
console.log('🔐 FCM 관리자 테스트 도구');
console.log('='.repeat(70));
console.log('\n🚀 시작하기:');
console.log('1. FCMAdmin.init("admin-jwt-token")');
console.log('   예: FCMAdmin.init("eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...")');
console.log('\n2. FCMAdmin.help()  - 자세한 사용법 보기');
console.log('\n📋 빠른 명령어:');
console.log('- FCMAdmin.stats() - 통계 조회');
console.log('- FCMAdmin.send(userCode, title, body) - 알림 전송');
console.log('- FCMAdmin.batch([userCodes], title, body) - 일괄 전송');
console.log('- FCMAdmin.broadcast(title, body) - 브로드캐스트');
console.log('');
console.log('='.repeat(70));
