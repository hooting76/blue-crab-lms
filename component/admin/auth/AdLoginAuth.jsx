async function AdLoginAuth() { 
    // ===== 관리자 이메일 인증 시스템 전체 테스트 =====
    // console.log("🚀 서버 재시작 후 전체 시스템 테스트 시작");

    const serverDomain = "https://bluecrab.chickenkiller.com/BlueCrab-1.0.0";

    // 1단계: 새로운 관리자 로그인 (세션 토큰 발급)
    // console.log("\n=== 1단계: 관리자 로그인 ===");

    await fetch(`${serverDomain}/api/admin/login`, {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json',
            'Accept': 'application/json'
        },
        body: JSON.stringify({
            adminId: "bluecrabtester9@gmail.com",
            password: "Bluecrab256@"
        })
    })
    .then(response => {
        // console.log("🔐 로그인 응답 상태:", response.status);
        return response.text();
    })
    .then(data => {
        // console.log("📝 로그인 응답:", data);
    
        try {
            const loginResult = JSON.parse(data);
            
            if (loginResult.success && loginResult.data?.sessionToken) {
                const sessionToken = loginResult.data.sessionToken;

                localStorage.setItem('tmp_token', sessionToken);
                // 토큰 만료 시간 확인
                const payload = JSON.parse(atob(sessionToken.split('.')[1]));
                // console.log("🕐 토큰 만료 시간:", new Date(payload.exp * 1000));
                // console.log("🕐 현재 시간:", new Date());
                
                // 2단계: 이메일 인증코드 요청
                // console.log("\n=== 2단계: 이메일 인증코드 요청 ===");
                
                
                return fetch(`${serverDomain}/api/admin/email-auth/request`, {
                    method: 'POST',
                    headers: {
                        'Authorization': `Bearer ${sessionToken}`,
                        'Content-Type': 'application/json'
                    }
                });
            
            } else {
                throw new Error("로그인 실패: " + JSON.stringify(loginResult));
            }
        } catch (e) {
            throw new Error("로그인 응답 파싱 실패: " + data);
        }
    })
    .then(response => {
        console.log("📧 이메일 발송 응답 상태:", response.status);
        if (response.status === 200) {
            // console.log("🎉 SecurityConfig 수정 성공! 401 오류 해결됨!");
        } else if (response.status === 401) {
            // console.log("❌ 아직도 401 오류 발생 - 서버 재시작이 제대로 안됐거나 설정 미적용");
        } 
        return response.text();
    })
    .then(data => {
        try {
            const emailResult = JSON.parse(data);

            if (emailResult.success || emailResult.message?.includes("발송")) {

                // Start 5-minute countdown timer
                const timerElement = document.getElementById('authTimer');
                let timeLeft = 300; // 5 minutes in seconds
                const timer = setInterval(() => {
                    const minutes = Math.floor(timeLeft / 60);
                    const seconds = timeLeft % 60;
                    timerElement.textContent = `${minutes}:${seconds.toString().padStart(2, '0')}`;
                    if (timeLeft === 0) {
                        clearInterval(timer);
                        authCode.disabled = true;
                        alert('인증 시간이 만료되었습니다.');
                    }
                    timeLeft--;
                }, 1000);

                let msg = emailResult.message.replace("%d", emailResult.data);

                alert(msg);
            } else {
                // console.log("❌ 이메일 발송 실패:", emailResult);
            }
        } catch (e) {
            // console.log("이메일 응답 파싱 실패:", data);
        }
    })
    .catch(error => {
        // console.error("❌ 전체 테스트 실패:", error);
        // console.log("\n💡 문제 해결 가이드:");
        // console.log("1. 서버가 정상 재시작되었는지 확인");
        // console.log("2. SecurityConfig 변경사항이 적용되었는지 확인");
        // console.log("3. 네트워크 연결 상태 확인");
        // console.log("4. 토큰 만료 시간 확인");
    });
    

    // 추가 진단 정보
    // console.log("\n🔍 테스트 환경 정보:");
    // console.log("- 서버 도메인:", serverDomain);
    // console.log("- 테스트 계정:", "bluecrabtester9@gmail.com");
    // console.log("- 현재 시간:", new Date());
    // console.log("- SecurityConfig 수정 사항: /api/admin/email-auth/** 경로 허용 추가");
}

export default AdLoginAuth;