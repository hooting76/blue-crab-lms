// public/firebase-messaging-sw.js
importScripts('https://www.gstatic.com/firebasejs/9.0.0/firebase-app-compat.js');
importScripts('https://www.gstatic.com/firebasejs/9.0.0/firebase-messaging-compat.js');

console.log('🔧 Firebase Service Worker 로드됨');

const firebaseConfig = {
    apiKey: "AIzaSyDMxR4Xvug2sGmbHGo3fYeJCb9d0WorVSE",
    authDomain: "lms-project-b8489.firebaseapp.com",
    projectId: "lms-project-b8489",
    storageBucket: "lms-project-b8489.firebasestorage.app",
    messagingSenderId: "46298862215",
    appId: "1:46298862215:web:62d4fa4f39af2fca2cde22",
    measurementId: "G-RMFS6V42HN"
};

firebase.initializeApp(firebaseConfig);
const messaging = firebase.messaging();

console.log('✅ Firebase Messaging 초기화 완료');

// 백그라운드 메시지 수신 리스너
messaging.onBackgroundMessage((payload) => {
    console.log('📨 [백그라운드] 메시지 수신:', payload);
    console.log('  - 제목:', payload.notification?.title);
    console.log('  - 본문:', payload.notification?.body);

    const notificationTitle = payload.notification?.title || '새 알림';
    const notificationOptions = {
        body: payload.notification?.body || '새로운 메시지가 도착했습니다.',
        icon: payload.notification?.icon || '/firebase-logo.png',
        badge: '/badge-icon.png',
        tag: 'notification-' + Date.now(),
        data: payload.data,
        requireInteraction: false,
        vibrate: [200, 100, 200]
    };

    console.log('🔔 알림 표시 중...');
    return self.registration.showNotification(notificationTitle, notificationOptions);
});

// 알림 클릭 이벤트
self.addEventListener('notificationclick', (event) => {
    console.log('👆 알림 클릭됨:', event.notification.tag);
    event.notification.close();

    // 특정 URL로 이동
    event.waitUntil(
        clients.openWindow('https://dtmch.synology.me:56000/')
    );
});