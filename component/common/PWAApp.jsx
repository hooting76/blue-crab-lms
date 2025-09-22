import { useState } from 'react';
import { RefreshCw, Wifi, WifiOff, Download, X } from 'lucide-react';

function usePWA() {
    const [needRefresh, setNeedRefresh] = useState(false);
    const [offlineReady, setOfflineReady] = useState(false);
    const [updateSW, setUpdateSW] = useState(null);

    useEffect(() => {
        import('virtual:pwa-register')
        .then(({ registerSW }) => {
            const updateSW = registerSW({
                onNeedRefresh() {
                    console.log('새로운 서비스 워커 버전이 사용 가능합니다.');
                    setNeedRefresh(true);
                },
                onOfflineReady() {
                    console.log('앱이 오프라인에서 사용할 준비가 되었습니다.');
                    setOfflineReady(true);
                },
                onRegistered(r) {
                    console.log('서비스 워커가 등록되었습니다:', r);
                },
                onRegisterError(error) {
                    console.error('서비스 워커 등록에 실패했습니다:', error);
                },
                onRegisteredSW(swUrl, r) {
                    console.log('서비스 워커가 등록되었습니다:', swUrl, r);
                }
            });
            setUpdateSW(() => updateSW);
        })
        .catch((error) => {
            console.error('PWA 모듈을 로드할 수 없습니다:', error);
            // // 개발 환경이나 PWA가 비활성화된 경우의 fallback
            // console.log('PWA 기능이 비활성화되었거나 개발 모드입니다.');
        });
    }, []);

    const close = () => {
        setOfflineReady(false);
        setNeedRefresh(false);
    };

    const updateServiceWorker = (reloadPage) => {
        if (updateSW) {
            updateSW(reloadPage);
        }
    };

    return { needRefresh, offlineReady, updateServiceWorker, close };
}; // usePWA end

function useOnlineStatus() {
    const [isOnline, setIsOnline] = useState(true);
    useEffect(() => {
        const handleOnline = () => setIsOnline(true);
        const handleOffline = () => setIsOnline(false);

        window.addEventListener('online', handleOnline);
        window.addEventListener('offline', handleOffline);

        return () => {
            window.removeEventListener('online', handleOnline);
            window.removeEventListener('offline', handleOffline);
        };
    }, []);

    return isOnline;
}; // useOnlineStatus end

function useInstallPrompt() {
    const [showInstallPrompt, setShowInstallPrompt] = useState(false);
    
    useEffect(() => {
        // 데모용으로 3초 후 설치 프롬프트 표시
        const timer = setTimeout(() => setShowInstallPrompt(true), 3000);
        return () => clearTimeout(timer);
    }, []);

    const installApp = async () => {
        setShowInstallPrompt(false);
        return true;
    };

    const dismissPrompt = () => {
        setShowInstallPrompt(false);
    };

    return { showInstallPrompt, installApp, dismissPrompt };
}; // useInstallPrompt end

function PWAUpdatePrompt() {
    const { needRefresh, offlineReady, updateServiceWorker, close } = usePWA();

    if (needRefresh) {
        return (
        <div className="fixed bottom-4 left-4 right-4 bg-blue-600 text-white p-4 rounded-lg shadow-lg z-50">
            <div className="flex items-center justify-between">
                <div className="flex items-center space-x-3">
                    <RefreshCw className="h-5 w-5" />
                    <div>
                        <p className="font-semibold">새 버전 사용 가능</p>
                        <p className="text-sm opacity-90">앱을 새로고침하여 업데이트하세요.</p>
                    </div>
                </div>
                <div className="flex space-x-2">
                    <button
                        onClick={() => updateServiceWorker(true)}
                        className="bg-white text-blue-600 px-3 py-1 rounded font-semibold hover:bg-blue-50 transition-colors"
                    >
                        새로고침
                    </button>

                    <button
                        onClick={close}
                        className="text-white hover:bg-blue-700 p-1 rounded transition-colors"
                    >
                        <X className="h-4 w-4" />
                    </button>
                </div>
            </div>
        </div>);
    }; // needRefresh END
    // network is offline
    if (offlineReady) {
        return (
        <div className="fixed bottom-4 left-4 right-4 bg-green-600 text-white p-4 rounded-lg shadow-lg z-50">
            <div className="flex items-center justify-between">
            <div className="flex items-center space-x-3">
                <WifiOff className="h-5 w-5" />
                <div>
                <p className="font-semibold">오프라인 준비 완료</p>
                <p className="text-sm opacity-90">이제 인터넷 연결 없이도 앱을 사용할 수 있습니다.</p>
                </div>
            </div>
            <button
                onClick={close}
                className="text-white hover:bg-green-700 p-1 rounded transition-colors"
            >
                <X className="h-4 w-4" />
            </button>
            </div>
        </div>
        );
    };

    return null;
}; // PWAUpdatePrompt end


function OnlineStatusIndicator() {
    const isOnline = useOnlineStatus();
    return (
        <div 
            className={`fixed top-4 right-4 px-3 py-1 rounded-full text-sm font-medium 
                ${isOnline 
                    ? 'bg-green-100 text-green-800' 
                    : 'bg-red-100 text-red-800'}`}
        >
            <div className="flex items-center space-x-2">
                {isOnline 
                    ? <Wifi className="h-4 w-4" /> 
                    : <WifiOff className="h-4 w-4" />}
                <span>{isOnline ? '온라인' : '오프라인'}</span>
            </div>
        </div>
  );
}; // OnlineStatusIndicator end

function InstallPrompt() {
    const { showInstallPrompt, installApp, dismissPrompt } = useInstallPrompt();
    
    if (!showInstallPrompt) return null;
    
    return (
        <div className="fixed bottom-4 left-4 right-4 bg-white border border-gray-200 rounded-lg shadow-xl p-4 z-50">
            <div className="flex items-center justify-between">
                <div className="flex items-center space-x-3">
                    <Download className="h-6 w-6 text-blue-600" />
                    <div>
                        <p className="font-semibold text-gray-900">앱 설치</p>
                        <p className="text-sm text-gray-600">홈 화면에 앱을 추가하여 더 빠르게 접근하세요.</p>
                    </div>
                </div>
                <div className="flex space-x-2">
                    <button
                        onClick={installApp}
                        className="bg-blue-600 text-white px-4 py-2 rounded-lg font-semibold hover:bg-blue-700 transition-colors"
                    >
                        설치
                    </button>
                    <button
                        onClick={dismissPrompt}
                        className="text-gray-500 hover:text-gray-700 p-2 transition-colors"
                    >
                        <X className="h-4 w-4" />
                    </button>
                </div>
            </div>
        </div>
    );
}; // InstallPrompt end


function PWAStatusDashboard() {
    const { offlineReady } = usePWA();
    const isOnline = useOnlineStatus();
    const { showInstallPrompt } = useInstallPrompt();

    return (
        <div className="bg-white rounded-lg shadow-lg p-6">
            <h3 className="text-lg font-semibold mb-4">PWA 상태</h3>
        
            <div className="space-y-3">
                <div className="flex items-center justify-between">
                    <span className="text-gray-600">Service Worker</span>
                    <span className={`px-2 py-1 rounded text-sm ${
                        offlineReady ? 'bg-green-100 text-green-800' : 'bg-yellow-100 text-yellow-800'
                    }`}>
                        {offlineReady ? '활성화됨' : '준비중'}
                    </span>
                </div>
                
                <div className="flex items-center justify-between">
                    <span className="text-gray-600">네트워크 상태</span>
                    <span className={`px-2 py-1 rounded text-sm ${
                        isOnline ? 'bg-green-100 text-green-800' : 'bg-red-100 text-red-800'
                    }`}>
                        {isOnline ? '온라인' : '오프라인'}
                    </span>
                </div>
                
                <div className="flex items-center justify-between">
                    <span className="text-gray-600">설치 가능</span>
                    <span 
                        className={`px-2 py-1 rounded text-sm 
                        ${showInstallPrompt 
                            ? 'bg-blue-100 text-blue-800' 
                            : 'bg-gray-100 text-gray-600'}`}>
                        {showInstallPrompt ? '설치 가능' : '설치됨/불가'}
                    </span>
                </div>
            </div>
        </div>
    );
}; // PWAStatusDashboard end

// 5. 메인 PWA 앱 컴포넌트
function PWAApp() {
    return (
        <>
            <PWAStatusDashboard />
            <OnlineStatusIndicator />
            <PWAUpdatePrompt />
            <InstallPrompt />        
        </>
    );
};

export default PWAApp;