// pushNotificationService
import { getToken, onMessage, getMessaging } from 'firebase/messaging';
import { messaging } from './firebase-config';
import vapid from "./key.json";

const BACKEND_API_URL = 'https://bluecrab.chickenkiller.com/BlueCrab-1.0.0/api/';
const VAPID_KEY = vapid.vapid; // Firebase 콘솔에서 발급받은 VAPID 키

class PushNotificationService {
    constructor() {
        this.currentToken = null;
    };

    // 1. Service Worker 등록
    async registerServiceWorker() {
        try {
            if ('serviceWorker' in navigator) {
                const registration = await navigator.serviceWorker.register('/firebase-messaging-sw.js');
                // console.log('✅ Service Worker 등록 성공:', registration.scope);
                return registration;
            }
        } catch (error) {
            // console.error('❌ Service Worker 등록 실패:', error);
            throw error;
        };
    };

    // 2. 알림 권한 요청
    async requestPermission() {
        try {
            const permission = await Notification.requestPermission();
            // console.log('알림 권한 상태:', permission);
            
            if (permission === 'granted') {
                // console.log('✅ 알림 권한 허용됨');
                return true;
            } else {
                // console.log('❌ 알림 권한 거부됨');
                return false;
            }
        } catch (error) {
            // console.error('❌ 권한 요청 실패:', error);
            return false;
        };
    };

    // 3. FCM 토큰 발급
    async getFCMToken() {
        try {
            const token = await getToken(messaging, {
                vapidKey: VAPID_KEY
            });

            if (token) {
                // console.log('✅ FCM 토큰 발급 성공');
                // console.log('토큰:', token);
                this.currentToken = token;
                return token;
            } else {
                // console.log('❌ FCM 토큰 발급 실패');
                return null;
            }
        } catch (error) {
            // console.error('❌ 토큰 발급 오류:', error);
            return null;
        }
    };

    // 4. 백엔드에 토큰 저장 => /api/fcm/register
    async saveTokenToBackend(token, userId) {
        // deviceInfo: navigator.userAgent
        const agentSwitch = navigator.userAgent;
        const authTk = localStorage.getItem('accessToken');

        // user agent info filter start
        let platform;
        if(agentSwitch.includes('Windows')){
            platform = "WEB";
        }else if(agentSwitch.includes('Android')){
            platform = "ANDROID";
        }else if(agentSwitch.includes('iPhone')){
            platform = "IOS";
        }; // user agent info filter end

        try {
            const response = await fetch(`${BACKEND_API_URL}fcm/register`, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                    'Authorization': `Bearer ${authTk}`,
                },
                body: JSON.stringify({
                    fcmToken: token,
                    platform: platform
                })
            });
            // console.log(response);

            if (response.ok) {
                const data = await response.json();
                // console.log('✅ 백엔드에 토큰 저장 성공:', data);
                // status : registered/renewed/conflict/temporary/replaced
                // keepSignedIn : null(default)
                // console.log(data.data);
                
                // resister status cntlr
                const status = ['registered', 'renewed', 'conflict', 'temporary', 'replaced'];

                if(status.includes(data.data.status)){
                    let listNum = status.indexOf(data.data.status) + 1;
                    // console.log(listNum);
                    switch(listNum){
                        case 1 : 
                            // console.log('registered');
                            break;
                        case 2 :
                            // console.log('renewed');
                            break;
                        case 3 :
                            // 다른 기기 토큰과 충돌 발생 (이미 다른기기가 등록됨)
                            // console.log('conflict');
                            break;
                        case 4 :
                            // 로그인 동안만 알림 수신(임시)temporaryOnly: true 전달 필요
                            // console.log('temporary');
                            break;
                        case 5 :
                            // 기존 토큰 제거 후 현재 기기로 강제 교체
                            // console.log('replaced');
                            break;
                        default : 
                            // console.log('비 정상적인 리턴');
                    } // switch end
                }else{
                    // alert('웹서버 통신 오류!');
                    return false;
                }; // status.includes end

                return true;
            } else {
                // console.error('❌ 백엔드 토큰 저장 실패:', response.status);
                return false;
            }
        } catch (error) {
            // console.error('❌ 백엔드 통신 오류:', error);
            return false;
        }
    };

    // 5. 포그라운드 메시지 리스너 설정
    setupForegroundListener() {
        onMessage(messaging, (payload) => {
            console.log('📨 포그라운드 메시지 수신:', payload);
            
            const dataType = payload.data;

            const notificationTit = dataType?.title || '새 알림';
            const notificationOpt = {
                body: dataType?.body || '새로운 포그라운드 메시지가 도착했습니다.',
                icon: '/favicon/android-icon-96x96.png',
                badge: '/favicon/android-icon-96x96.png',
                tag: 'foreground-' + Date.now(),
                data: {dataType},
                requireInteraction: true,
                vibrate: [200, 100, 200]
            };

            if(Notification.permission === "granted"){
                navigator.serviceWorker.ready.then((registration) => {
                    registration.showNotification(notificationTit,notificationOpt);
                });
            };
        });
    };

    // 6. 전체 초기화 프로세스
    async initialize(userId) {
        try {
            // console.log('🚀 푸시 알림 초기화 시작...');

            // Step 1: Service Worker 등록
            await this.registerServiceWorker();

            // Step 2: 알림 권한 요청
            const hasPermission = await this.requestPermission();
            if (!hasPermission) {
                // console.log('⚠️ 알림 권한이 없어 초기화를 중단합니다.');
                return false;
            }

            // Step 3: FCM 토큰 발급
            const token = await this.getFCMToken();
            if (!token) {
                // console.log('⚠️ FCM 토큰 발급 실패로 초기화를 중단합니다.');
                return false;
            }

            // Step 4: 백엔드에 토큰 저장
            await this.saveTokenToBackend(token, userId);

            // Step 5: 포그라운드 메시지 리스너 설정
            this.setupForegroundListener();
            
            sessionStorage.setItem('fcm', token);

            // console.log('✅ 푸시 알림 초기화 완료!');
            return true;
        } catch (error) {
            // console.error('❌ 푸시 알림 초기화 실패:', error);
            return false;
        };
    };

    // 토큰 갱신
    async refreshToken(userId) {
        const token = await this.getFCMToken();
        if (token) {
            await this.saveTokenToBackend(token, userId);
        }
        return token;
    };

    // 현재 토큰 가져오기
    getCurrentToken() {
        return this.currentToken;
    };
}

// 싱글톤 인스턴스 생성
const pushNotificationService = new PushNotificationService();
export default pushNotificationService;