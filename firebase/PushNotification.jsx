import { getToken, onMessage } from 'firebase/messaging';
import { messaging } from './Firebase-Config';

import vapid from './key.json';

class PushNotificationManager {
    // vapid
    constructor() {
        this.vapidKey = vapid.vapid;
    };

    // 알림 권한 요청 start
    async requestPermission() {
        try {
            const permission = await Notification.requestPermission();
        
            if (permission === 'granted') {
                // console.log('알림 권한이 승인되었습니다.');
                return true;
            } else {
                // console.log('알림 권한이 거부되었습니다.');
                return false;
            }
        } catch (error) {
            // console.error('권한 요청 중 오류 발생:', error);
            return false;
        }
    }; // 알림 권한 요청 end


    // FCM 토큰 가져오기 start
    async getToken() {
        try {
            const currentToken = await getToken(messaging, {vapidKey: this.vapidKey});

            if (currentToken) {
                console.log('FCM 토큰:', currentToken);
                return currentToken;
            } else {
                console.log('토큰을 가져올 수 없습니다. 권한을 확인하세요.');
                return null;
            };
        } catch (error) {
            console.error('토큰 가져오기 실패:', error);
            return null;
        }
    };// FCM 토큰 가져오기 end


    // 포그라운드 메시지 리스너 설정 start
    setupForegroundListener() {
        let usr = JSON.parse(localStorage.getItem('user'));
        let tmp = usr;
        usr = usr.data.user;

        // messaging 있는 경우
        onMessage(messaging, (payload) => {
            // console.log('qwer');
            console.log('포그라운드 메시지 수신:', payload);
    
            // 커스텀 알림 표시
            this.showCustomNotification(payload);
            return;
        });

        // messaging 없는 경우
        let payload = {notification:
            {title: `${usr.name}님! 좋은하루 보내세요!`,
            body: `접속계정: ${usr.email} \n접속시간: ${new Date().toLocaleString()}`,
            icon: null}};
        this.showCustomNotification(payload);
    }; // 포그라운드 메시지 리스너 설정 end


    // 커스텀 알림 표시 start
    showCustomNotification(payload) {
        const { title, body, icon } = payload.notification;
        if (Notification.permission === 'granted') {
            new Notification(title, {
                body: body,
                icon: icon || '../favicon/android-icon-96x96.png',
                tag: 'blue-crab LMS'
            });
        }
    };// 커스텀 알림 표시 end


    // 초기화 메서드 start
    async initialize() {
        try {
            // Service Worker 등록
            if ('serviceWorker' in navigator) {
                const registration = await navigator.serviceWorker.register('/firebase-messaging-sw.js');
                console.log('Service Worker 등록 완료:', registration);
            };

            // filter
            if(!("Notification" in window)){
                alert("알림기능을 지원하지 않는 환경입니다.");
                return;
            };

            // 권한 요청 / 권한 확인
            const hasPermission = await this.requestPermission();
            if (hasPermission) {
                const token = await this.getToken();

                // 서버에 토큰 저장 (필요시)
                if (token) { this.saveTokenToServer(token);};
                
                // 포그라운드 메시지 리스너 설정
                this.setupForegroundListener();
            };
            
        } catch (error) {
            // console.error('푸시 알림 초기화 실패:', error);
            alert('푸시 알림 초기화 실패!');
        }
    }; // 초기화 메서드 end


    // 서버에 토큰 저장 (선택사항) start
    async saveTokenToServer(token) {
        try {
            // 여기에 서버 API 호출 코드 작성
            // console.log('토큰을 서버에 저장:', token);
            // await fetch('/api/save-token', {
            //   method: 'POST',
            //   headers: { 'Content-Type': 'application/json' },
            //   body: JSON.stringify({ token })
            // });
        } catch (error) {
            // console.error('토큰 저장 실패:', error);
        }
    }; // 서버에 토큰 저장 (선택사항) end
};

export default PushNotificationManager;