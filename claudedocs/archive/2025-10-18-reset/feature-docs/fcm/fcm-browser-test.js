/**
 * FCM 토큰 테스트 스크립트 (브라우저 콘솔용)
 *
 * 사용법:
 * 1. 로그인 후 브라우저 콘솔을 엽니다
 * 2. 이 스크립트를 복사해서 붙여넣기 합니다
 * 3. FCMTest.init('your-jwt-token') 으로 인증 토큰을 설정합니다
 * 4. FCMTest.register() 등의 메서드를 호출합니다
 */

const FCMTest = {
  token: null,
  apiUrl: 'https://bluecrab.chickenkiller.com/BlueCrab-1.0.0/api/fcm', // 실제 API 주소로 변경하세요
  fcmToken: 'test-fcm-token-' + Date.now(), // 테스트용 FCM 토큰

  /**
   * 인증 토큰 설정
   */
  init(jwtToken) {
    this.token = jwtToken;
    console.log('✅ JWT 토큰 설정 완료');
    console.log('📱 테스트용 FCM 토큰:', this.fcmToken);
    console.log('\n사용 가능한 명령어:');
    console.log('- FCMTest.register(platform, keepSignedIn) - 토큰 등록');
    console.log('- FCMTest.registerTemp(platform) - 임시 토큰 등록');
    console.log('- FCMTest.registerForce(platform, keepSignedIn) - 강제 변경');
    console.log('- FCMTest.unregister(platform, forceDelete) - 토큰 삭제');
    console.log('- FCMTest.getStats() - 통계 조회 (관리자 전용)');
  },

  /**
   * HTTP 요청 헬퍼
   */
  async request(method, endpoint, body = null) {
    if (!this.token) {
      console.error('❌ 토큰이 설정되지 않았습니다. FCMTest.init("your-token")을 먼저 실행하세요.');
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
      } else {
        console.error('❌ 실패:', data);
      }

      return data;
    } catch (error) {
      console.error('❌ 네트워크 오류:', error);
    }
  },

  /**
   * FCM 토큰 등록 (충돌 감지)
   * @param {string} platform - 'WEB', 'ANDROID', 'IOS'
   * @param {boolean} keepSignedIn - 로그인 유지 여부 (기본값: false)
   */
  async register(platform = 'WEB', keepSignedIn = false) {
    console.log(`\n📝 FCM 토큰 등록 시도...`);
    console.log(`플랫폼: ${platform}, keepSignedIn: ${keepSignedIn}`);

    return await this.request('POST', '/register', {
      fcmToken: this.fcmToken,
      platform: platform,
      keepSignedIn: keepSignedIn
    });
  },

  /**
   * FCM 임시 토큰 등록 (충돌 시)
   * @param {string} platform - 'WEB', 'ANDROID', 'IOS'
   */
  async registerTemp(platform = 'WEB') {
    console.log(`\n📝 FCM 임시 토큰 등록 시도...`);
    console.log(`플랫폼: ${platform} (temporaryOnly: true)`);

    return await this.request('POST', '/register', {
      fcmToken: this.fcmToken,
      platform: platform,
      temporaryOnly: true
    });
  },

  /**
   * FCM 토큰 강제 변경
   * @param {string} platform - 'WEB', 'ANDROID', 'IOS'
   * @param {boolean} keepSignedIn - 로그인 유지 여부 (기본값: false)
   */
  async registerForce(platform = 'WEB', keepSignedIn = false) {
    console.log(`\n🔄 FCM 토큰 강제 변경 시도...`);
    console.log(`플랫폼: ${platform}, keepSignedIn: ${keepSignedIn}`);

    return await this.request('POST', '/register/force', {
      fcmToken: this.fcmToken,
      platform: platform,
      keepSignedIn: keepSignedIn
    });
  },

  /**
   * FCM 토큰 삭제 (로그아웃)
   * @param {string} platform - 'WEB', 'ANDROID', 'IOS'
   * @param {boolean} forceDelete - 강제 삭제 여부 (기본값: false)
   */
  async unregister(platform = 'WEB', forceDelete = false) {
    console.log(`\n🗑️  FCM 토큰 삭제 시도...`);
    console.log(`플랫폼: ${platform}, forceDelete: ${forceDelete}`);

    return await this.request('DELETE', '/unregister', {
      fcmToken: this.fcmToken,
      platform: platform,
      forceDelete: forceDelete
    });
  },

  /**
   * FCM 통계 조회 (관리자 전용)
   */
  async getStats() {
    console.log(`\n📊 FCM 통계 조회...`);
    return await this.request('GET', '/stats');
  },

  /**
   * 새로운 FCM 토큰 생성 (다른 기기 시뮬레이션)
   */
  newToken() {
    this.fcmToken = 'test-fcm-token-' + Date.now();
    console.log('🆕 새로운 FCM 토큰 생성:', this.fcmToken);
  },

  /**
   * 현재 FCM 토큰 확인
   */
  showToken() {
    console.log('📱 현재 FCM 토큰:', this.fcmToken);
  }
};

// 사용 예시 출력
console.log('='.repeat(60));
console.log('📱 FCM 토큰 테스트 도구');
console.log('='.repeat(60));
console.log('\n🚀 시작하기:');
console.log('1. FCMTest.init("your-jwt-token")');
console.log('   예: FCMTest.init("eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...")');
console.log('\n📝 기본 테스트 시나리오:');
console.log('');
console.log('// 시나리오 1: 최초 로그인');
console.log('FCMTest.register("WEB", false);');
console.log('');
console.log('// 시나리오 2: 다른 기기에서 로그인 (충돌 테스트)');
console.log('FCMTest.newToken();  // 새 토큰 생성');
console.log('FCMTest.register("WEB", false);  // 충돌 발생!');
console.log('');
console.log('// 시나리오 3: 임시 토큰 등록');
console.log('FCMTest.registerTemp("WEB");');
console.log('');
console.log('// 시나리오 4: 기기 변경 승인');
console.log('FCMTest.registerForce("WEB", true);');
console.log('');
console.log('// 시나리오 5: 로그아웃');
console.log('FCMTest.unregister("WEB");');
console.log('');
console.log('// 시나리오 6: keepSignedIn=true인 경우 로그아웃');
console.log('FCMTest.unregister("WEB", false);  // 삭제 안됨');
console.log('FCMTest.unregister("WEB", true);   // 강제 삭제');
console.log('');
console.log('='.repeat(60));
