importScripts('https://www.gstatic.com/firebasejs/9.0.0/firebase-app-compat.js');
importScripts('https://www.gstatic.com/firebasejs/9.0.0/firebase-messaging-compat.js');

const firebaseConfig = {
    apiKey: "AIzaSyDMxR4Xvug2sGmbHGo3fYeJCb9d0WorVSE",
    authDomain: "lms-project-b8489.firebaseapp.com",
    projectId: "lms-project-b8489",
    storageBucket: "lms-project-b8489.firebasestorage.app",
    messagingSenderId: "46298862215",
    appId: "1:46298862215:web:62d4fa4f39af2fca2cde22",
};

firebase.initializeApp(firebaseConfig);

const messaging = firebase.messaging();

// 백그라운드 메시지 처리
messaging.onBackgroundMessage((payload) => {
    console.log('백그라운드 메시지 수신:', payload);

    const notificationTitle = payload.notification.title;
    const notificationOptions = {
        body: payload.notification.body,
        icon: payload.notification.icon || '../favicon/android-icon-96x96.png'
    };

    self.registration.showNotification(notificationTitle, notificationOptions);
});