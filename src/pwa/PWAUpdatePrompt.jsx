import React, { useState, useEffect } from 'react'

function PWAUpdatePrompt() {
    const [needRefresh, setNeedRefresh] = useState(false)
    const [updateSW, setUpdateSW] = useState(null)

    useEffect(() => {
        if ('ServiceWorker' in navigator) {
            import('virtual:pwa-register').then(({ registerSW }) => {
                const updateSW = registerSW({
                    onNeedRefresh() { setNeedRefresh(true); },
                    onOfflineReady() { alert('오프라인 환경에서는 사용이 불가합니다.'); return; },
                });
                setUpdateSW(() => updateSW);
            });
        };
    }, []);

    const close = () => {
        setNeedRefresh(false);
    };

    const handleUpdate = () => {
        if (updateSW) {
            updateSW(true);
        };
    };
    
    return (
        needRefresh && (
            <div className="pwa-toast" role="alert">
                <p>웹앱 업데이트</p>
                <div className="buttons">
                    <button className="pwa-refresh" onClick={handleUpdate}>
                        새로고침
                    </button>
                    <button className="pwa-cancel" onClick={close}>
                        나중에
                    </button>
                </div>
            </div>
        )
    );
};

export default PWAUpdatePrompt