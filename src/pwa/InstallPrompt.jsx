import React, { useState, useEffect } from 'react';

import InstallPromptCss from "../../css/PWA/InstallPromptCss.module.css";

function InstallPrompt() {
    // state
    const [deferredPrompt, setDeferredPrompt] = useState(null);
    const [showInstall, setShowInstall] = useState(false);

    useEffect(() => {
        const handleBeforeInstallPrompt = (e) => {
            e.preventDefault();
            setDeferredPrompt(e);
            setShowInstall(true);
        };

        const handleAppInstalled = () => {
            alert('앱이 설치되었습니다.');
            setShowInstall(false);
            setDeferredPrompt(null);
        };

        window.addEventListener('beforeinstallprompt', handleBeforeInstallPrompt);
        window.addEventListener('appinstalled', handleAppInstalled);

        return () => {
            window.removeEventListener('beforeinstallprompt', handleBeforeInstallPrompt);
            window.removeEventListener('appinstalled', handleAppInstalled);
        }
    }, [])

    const handleInstallClick = async () => {
        if (!deferredPrompt){
            return;
        }

        deferredPrompt.prompt();
        const { outcome } = await deferredPrompt.userChoice;
    
        console.log(`User response to the install prompt: ${outcome}`);
        setDeferredPrompt(null);
        setShowInstall(false);
    };

    const handleDismiss = () => {
        setShowInstall(false);
    };

    if (!showInstall){
        return null;
    } 

    return (
        <div className="install-prompt">
            <p>이 웹을 설치할까요?</p>
            <div className="install-buttons">
                <button onClick={handleInstallClick} className="install-yes">
                    설치
                </button>
                <button onClick={handleDismiss} className="install-no">
                    나중에
                </button>
            </div>
        </div>
    );
}

export default InstallPrompt