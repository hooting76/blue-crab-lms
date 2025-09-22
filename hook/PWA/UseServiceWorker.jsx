import { useState, useEffect } from 'react';

function UseServiceWorker() {
    const [registration, setRegistration] = useState(null);
    const [isControlled, setIsControlled] = useState(false);
    const [waiting, setWaiting] = useState(null);

    useEffect(() => {
        if ('serviceWorker' in navigator) {
            navigator.serviceWorker.ready
                .then((reg) => {
                    setRegistration(reg);
                    setWaiting(reg.waiting);
                    setIsControlled(!!navigator.serviceWorker.controller);
                });

            const handleControllerChange = () => {
                setIsControlled(!!navigator.serviceWorker.controller);
            };

            navigator.serviceWorker.addEventListener('controllerchange', handleControllerChange);

            return () => {
                navigator.serviceWorker.removeEventListener('controllerchange', handleControllerChange);
            };
        }
    }, []);

    // 서비스워커에 메시지 전송
    const sendMessage = (message) => {
        if (navigator.serviceWorker.controller) {
            navigator.serviceWorker.controller.postMessage(message);
        };
    };

    // 캐시 정리
    const clearCaches = async () => {
        if ('caches' in window) {
            const cacheNames = await caches.keys();
            await Promise.all(
                cacheNames.map(cacheName => caches.delete(cacheName))
            );
            console.log('🗑️ 모든 캐시가 정리되었습니다');
        }
    };

    return {
        registration,
        isControlled,
        waiting,
        sendMessage,
        clearCaches
    };
};

export default UseServiceWorker;