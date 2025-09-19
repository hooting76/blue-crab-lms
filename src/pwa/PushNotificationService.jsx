import { generateVAPIDKeys } from 'web-push';

const vapidKeys = generateVAPIDKeys();
// console.log('Public Key:', vapidKeys.publicKey);
// console.log('Private Key:', vapidKeys.privateKey);

class PushNotificationService {
    constructor() {
        this.isSupported = 'serviceWorker' in navigator && 'PushManager' in window;
    }

    // 푸시 알림 지원 여부 확인
    isNotificationSupported() {
        return this.isSupported;
    }

    // 알림 권한 요청
    async requestPermission() {
        if (!this.isSupported) {
        throw new Error('푸시 알림이 지원되지 않습니다.');
        }

        const permission = await Notification.requestPermission();
        
        if (permission === 'granted') {
        console.log('알림 권한이 허용되었습니다.');
        return true;
        } else if (permission === 'denied') {
        console.log('알림 권한이 거부되었습니다.');
        return false;
        } else {
        console.log('알림 권한 요청이 무시되었습니다.');
        return false;
        }
    }

    // 현재 알림 권한 상태 확인
    getPermissionStatus() {
        if (!this.isSupported) return 'unsupported';
        return Notification.permission;
    }

    // 즉시 알림 표시 (로컬 알림)
    showLocalNotification(title, options = {}) {
        if (Notification.permission !== 'granted') {
        console.error('알림 권한이 없습니다.');
        return null;
        }

        const defaultOptions = {
        body: '알림 메시지입니다.',
        icon: '/icons/icon-192x192.png',
        badge: '/icons/badge-72x72.png',
        tag: 'default',
        requireInteraction: false,
        silent: false,
        vibrate: [200, 100, 200],
        timestamp: Date.now(),
        actions: [
            {
            action: 'open',
            title: '열기',
            icon: '/icons/open-icon.png'
            },
            {
            action: 'close',
            title: '닫기',
            icon: '/icons/close-icon.png'
            }
        ]
        };

        const finalOptions = { ...defaultOptions, ...options };

        const notification = new Notification(title, finalOptions);

        // 알림 이벤트 핸들러
        notification.onclick = (event) => {
        event.preventDefault();
        window.focus();
        notification.close();
        
        if (options.onClick) {
            options.onClick(event);
        }
        };

        notification.onclose = (event) => {
        if (options.onClose) {
            options.onClose(event);
        }
        };

        notification.onerror = (event) => {
        console.error('알림 오류:', event);
        if (options.onError) {
            options.onError(event);
        }
        };

        return notification;
    }

    // 스케줄된 알림 (지연 알림)
    scheduleNotification(title, options = {}, delaySeconds = 0) {
        if (delaySeconds === 0) {
        return this.showLocalNotification(title, options);
        }

        return setTimeout(() => {
        this.showLocalNotification(title, options);
        }, delaySeconds * 1000);
    }

    // 반복 알림
    createRepeatingNotification(title, options = {}, intervalSeconds = 60) {
        if (Notification.permission !== 'granted') {
        throw new Error('알림 권한이 필요합니다.');
        }

        return setInterval(() => {
        this.showLocalNotification(title, {
            ...options,
            tag: `repeating-${Date.now()}` // 중복 방지
        });
        }, intervalSeconds * 1000);
    }

    // 알림 닫기
    closeNotification(tag) {
        // Service Worker를 통해 알림 닫기
        if ('serviceWorker' in navigator) {
        navigator.serviceWorker.ready.then(registration => {
            return registration.getNotifications({ tag: tag });
        }).then(notifications => {
            notifications.forEach(notification => {
            notification.close();
            });
        });
        }
    }

    // 모든 알림 닫기
    closeAllNotifications() {
        if ('serviceWorker' in navigator) {
        navigator.serviceWorker.ready.then(registration => {
            return registration.getNotifications();
        }).then(notifications => {
            notifications.forEach(notification => {
            notification.close();
            });
        });
        }
    }

    // 현재 표시된 알림 목록 가져오기
    async getActiveNotifications() {
        if ('serviceWorker' in navigator) {
        const registration = await navigator.serviceWorker.ready;
        return await registration.getNotifications();
        }
        return [];
    }
};

export default PushNotificationService;

