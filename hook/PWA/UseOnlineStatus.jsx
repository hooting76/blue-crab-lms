import { useState, useEffect } from 'react';

function UseOnlineStatus() {
    const [isOnline, setIsOnline] = useState(navigator.onLine);
    const [lastOnlineTime, setLastOnlineTime] = useState(Date.now());
    const [lastOfflineTime, setLastOfflineTime] = useState(null);

    useEffect(() => {
        const handleOnline = () => {
            setIsOnline(true);
            setLastOnlineTime(Date.now());
            console.log('🌐 네트워크 연결됨');
        };

        const handleOffline = () => {
            setIsOnline(false);
            setLastOfflineTime(Date.now());
            console.log('📴 네트워크 연결 끊어짐');
        };

        window.addEventListener('online', handleOnline);
        window.addEventListener('offline', handleOffline);

        // 추가: 주기적으로 실제 네트워크 상태 확인 (선택사항)
        const checkRealConnection = async () => {
        if (navigator.onLine) {
            try {
                // 작은 이미지로 실제 연결 테스트
                await fetch('/favicon.ico?t=' + Date.now(), { 
                    mode: 'no-cors',
                    cache: 'no-cache' 
                });
                setIsOnline(true);
            } catch {
                setIsOnline(false);
            }
        }
    };

    const intervalId = setInterval(checkRealConnection, 30000); // 30초마다 체크

    return () => {
        window.removeEventListener('online', handleOnline);
        window.removeEventListener('offline', handleOffline);
        clearInterval(intervalId);
    };
    }, []);

    return {
        isOnline,
        lastOnlineTime,
        lastOfflineTime,
        isOfflineFor: lastOfflineTime ? Date.now() - lastOfflineTime : 0
    };
};

export default UseOnlineStatus;