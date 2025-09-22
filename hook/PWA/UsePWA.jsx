import { useState, useEffect } from 'react';

export function UsePWA() {
    const [needRefresh, setNeedRefresh] = useState(false);
    const [offlineReady, setOfflineReady] = useState(false);
    const [updateSW, setUpdateSW] = useState(null);
    const [isLoading, setIsLoading] = useState(true);
    const [error, setError] = useState(null);

    useEffect(() => {
        setIsLoading(true);
    
        import('virtual:pwa-register')
            .then(({ registerSW }) => {
                const updateSW = registerSW({
                    // 새 서비스 워커가 설치되고 활성화 대기 중일 때
                    onNeedRefresh() {
                        console.log('🔄 새로운 서비스 워커 버전이 사용 가능합니다.');
                        setNeedRefresh(true);
                    },

                    // 앱이 오프라인에서 작동할 준비가 완료되었을 때  
                    onOfflineReady() {
                        // console.log('🌐 앱이 오프라인에서 사용할 준비가 되었습니다.');
                        setOfflineReady(true);
                    },

                    // 서비스 워커가 성공적으로 등록되었을 때
                    onRegistered(registration) {
                        console.log('✅ 서비스 워커가 등록되었습니다:', registration);
                    },

                    // 서비스 워커 등록 실패 시
                    onRegisterError(error) {
                        console.error('❌ 서비스 워커 등록에 실패했습니다:', error);
                        setError(error);
                    },

                    // 서비스 워커가 등록되고 URL과 등록 객체 제공
                    onRegisteredSW(swUrl, registration) {
                        // console.log('📝 서비스 워커가 다음 URL에 등록되었습니다:', swUrl);
                        // console.log('등록 정보:', registration);
                        
                        // 주기적으로 업데이트 확인
                        if (registration) {
                            setInterval(() => {
                                registration.update();
                            }, 60000); // 1분마다 체크
                        }
                    }
                });

                setUpdateSW(() => updateSW);
                setError(null);
            })
            .catch((error) => {
                console.error('PWA 모듈을 로드할 수 없습니다:', error);
                setError(error);
        
                // PWA가 비활성화된 경우나 개발 환경에서의 처리
                if (error.message.includes('virtual:pwa-register')) {
                    console.log('💡 PWA 기능이 비활성화되었거나 개발 모드입니다.');
                }
            })
            .finally(() => {
                setIsLoading(false);
            });
        }, []);

    // 알림 닫기
    const close = () => {
        setOfflineReady(false);
        setNeedRefresh(false);
    };

    // 서비스 워커 업데이트 (새로고침 포함 옵션)
    const updateServiceWorker = (reloadPage = true) => {
        if (updateSW) {
            updateSW(reloadPage);
            setNeedRefresh(false);
        }
    };

    return {
        needRefresh,
        offlineReady,
        updateServiceWorker,
        close,
        isLoading,
        error
    };
};