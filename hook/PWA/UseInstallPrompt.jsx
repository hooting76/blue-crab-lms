import { useState, useEffect } from 'react';

export default function UseInstallPrompt() {
    const [deferredPrompt, setDeferredPrompt] = useState(null);
    const [showInstallPrompt, setShowInstallPrompt] = useState(false);
    const [isInstallable, setIsInstallable] = useState(false);
    const [isInstalled, setIsInstalled] = useState(false);

    useEffect(() => {
        // 설치 프롬프트 이벤트 처리
        const handleBeforeInstallPrompt = (e) => {
            e.preventDefault();
            setDeferredPrompt(e);
            setIsInstallable(true);
        }; // handleBeforeInstallPrompt end

        // 앱이 성공적으로 설치되었을 때
        const handleAppInstalled = () => {
            // console.log('✅ 성공적으로 설치되었습니다');
            alert('✅ 성공적으로 설치되었습니다');
            setShowInstallPrompt(false);
            setDeferredPrompt(null);
            setIsInstalled(true);
            setIsInstallable(false);
            
            // 설치 성공 추적 (analytics 등)
            trackInstallEvent('success');
        }; // handleAppInstalled end

        // 이미 설치된 상태인지 확인
        if (window.matchMedia && window.matchMedia('(display-mode: standalone)').matches) {
            setIsInstalled(true);
            console.log('📱 이미 PWA로 실행 중');
        };

        window.addEventListener('beforeinstallprompt', handleBeforeInstallPrompt);
        window.addEventListener('appinstalled', handleAppInstalled);

        return () => {
            window.removeEventListener('beforeinstallprompt', handleBeforeInstallPrompt);
            window.removeEventListener('appinstalled', handleAppInstalled);
        };
    }, []);

    // 설치 실행
    const installApp = async() => {
        if (!deferredPrompt) {
            alert('설치 프롬프트를 사용할 수 없습니다');
            return false;
        }

        try {
            // 설치 프롬프트 표시
            deferredPrompt.prompt();
            
            // 사용자 선택 대기
            const { outcome } = await deferredPrompt.userChoice;
            
            if (outcome === 'accepted') {
                trackInstallEvent('accepted');
                // console.log('✅ 사용자가 설치를 승인했습니다');
            } else {
                trackInstallEvent('dismissed');
                // console.log('❌ 사용자가 설치를 거부했습니다');
            }
            
            setDeferredPrompt(null);
            setShowInstallPrompt(false);
            
            return outcome === 'accepted';

        } catch (error) {
            alert('설치 도중에 에러가 발생했습니다.');
            trackInstallEvent('error', error.message);
            return false;
        }
    };

    // 프롬프트 닫기/연기
    const dismissPrompt = () => {
        setShowInstallPrompt(!showInstallPrompt);
    };

    // 분석/추적 함수
    const trackInstallEvent = (action, details = null) => {
        // Google Analytics, 사용자 정의 추적 등
        if (window.gtag) {
            window.gtag('event', 'pwa_install', {
                event_category: 'PWA',
                event_label: action,
                value: details
            });
        }
    };

    return {
        showInstallPrompt,
        installApp,
        dismissPrompt,
        isInstallable,
        isInstalled
    };
};