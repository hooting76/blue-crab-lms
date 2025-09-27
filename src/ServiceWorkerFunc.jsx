// serviceworker 등록 및 상태관리
    export async function registerServiceWorker(){
        if ("serviceWorker" in navigator) {
            try {
                const registration = await navigator.serviceWorker.register('/sw.js');

                if(registration.installing){
                    console.log('서비스 워커 설치 중...');
                    trackInstalling(registration.installing);                
                }else if(registration.waiting){
                    console.log('새 서비스 워커가 대기 중');
                    showUpdateAvailable();
                }else if(registration.active){
                    console.log('서비스 워커 활성화됨');
                }
                return registration;
            } catch (error) {
                console.error('서비스 워커 등록 실패:', error);
            }
        }; // if end
    }; // async func end


    // serviceworker 설치상태 추적
    export function trackInstalling(worker){
        worker.addEventListener('statechange',() =>{
            console.log('worker', worker.state);

            switch(worker.state){
                case 'installed':
                    console.log('새 서비스워커 설치 원료, 활성화 대기 중');
                    break;
                case 'activated':
                    console.log('서비스 워커 활성화중.');
                    window.location.reload();
                    break;
            };
        });
    }; // serviceworker 설치상태 추적 end


    // 업데이트 감지 및 처리함수
    export function handleServiceWorkerUpdates(registration){
        registration.addEventListener('updatefound', () =>{
            console.log('새로운 서비스워커 발견');

            const newWorker = registration.installing;
            newWorker.addEventListener('statechange',() => {
                if(newWorker.state === 'installed' && registration.active){
                    // 새 버전 사용가능 알림 표시
                    showUpdateNotification();
                }
            });
        });
    }; // 업데이트 감지 및 처리함수 end


    // 푸시알림 구독 
    export async function subscribeToPushNotifications(registration){
        try {
            // 알림 권한 요청
            const permission = await Notification.requestPermission();
            if(permission !== 'granted'){
                throw new Error('알림 권한이 거부되었음!');
            }

            const vapidPublicKey = 'BFH1FPAvU0xBq1_SuGJs_4TYFN6a9D7qktiEzOcsE2OHhjfLRqlyvelzI8ZiLQVwd2FJN-4gXjQ4Yc0Xpo-bQ2E'; // Replace with your actual VAPID public key
            // 푸시 구독 생성
            const subscription = await registration.pushManager.subscribe({
                userVisibleOnly: true,
                applicationServerKey: urlBase64ToUint8Array(vapidPublicKey)
            });

            console.log('푸시알림 구독성공:', subscription);

            // 서버에 구독정도 전송
            await sendSubscriptionToServer(subscription);

        } catch (error) {
            console.error('푸시 구독 실패:', error);
        }
    }; // 푸시알림 구독 end


    // 알림표시
    export async function showNotification(registration){
        try {
            await registration.showNotification('새 메시지', {
                body: '새로운 업데이트가 있습니다.',
                icon: '/favicon/android-icon-192x192.png',
                badge: '/favicon/android-icon-72x72.png',
                action: [
                    {action: 'view', title: '보기'},
                    {action: 'dismiss', title: '닫기'}
                ],
                data: {
                    url: 'https://www.dtmch.synology.me:56000'
                },
                tag:'update-notification'
            });
        } catch (error) {
            console.error('알림 표시 실패:', error);
        }
    }; // 알림표시 end


    // 기존 알림 조회
    export async function getExistingNotifications(registration) {
        try {
            const notifications = await registration.getNotifications();
            console.log('기존 알림들:', notifications);
        
            // 중복 알림 방지
            const hasUpdateNotification = notifications.some(
                notification => notification.tag === 'update-notification'
            );
        
            if (!hasUpdateNotification) {
                await showNotification(registration);
            };
        } catch (error) {
            console.error('알림 조회 실패:', error);
        }
    }; // 기존 알림 조회 end


    // 백그라운드 동기화 등록
    export async function registerBackgroundSync(registration) {
        try {
            await registration.sync.register('background-sync');
            console.log('백그라운드 동기화 등록됨');
        } catch (error) {
            console.error('백그라운드 동기화 등록 실패:', error);
        }
    }; // 백그라운드 동기화 등록 end


    // 주기적 백그라운드 동기화
    export async function registerPeriodicSync(registration) {
        try {
            // 권한 상태 확인
            const status = await navigator.permissions.query({
                name: 'periodic-background-sync'
            });
        
            if (status.state === 'granted') {
                await registration.periodicSync.register('news-sync', {
                    minInterval: 24 * 60 * 60 * 1000 // 24시간
                });
                console.log('주기적 동기화 등록됨');
            };
        } catch (error) {
            console.error('주기적 동기화 등록 실패:', error);
        }
    }; // 주기적 백그라운드 동기화 end


    // 서비스 워커 업데이트 실행
    export async function updateServiceWorker(registration) {
        try {
            console.log('서비스 워커 업데이트 확인 중...');
            await registration.update();
            console.log('업데이트 확인 완료');
        } catch (error) {
            console.error('서비스 워커 업데이트 실패:', error);
        }
    }; // 서비스 워커 업데이트 강제 실행 end



    // 서비스 워커 등록 해제
    export async function unregisterServiceWorker(registration) {
        try {
            const result = await registration.unregister();
            if (result) {
                console.log('서비스 워커 등록 해제됨');
            }
        } catch (error) {
            console.error('서비스 워커 등록 해제 실패:', error);
        }
    };// 서비스 워커 등록 해제 end



    // 캐시 업데이트 정책 확인
    export function checkCacheUpdatePolicy(registration) {
        console.log('캐시 업데이트 정책:', registration.updateViaCache);
        // 가능한 값: 'imports', 'all', 'none'
        
        switch (registration.updateViaCache) {
            case 'imports':
                console.log('import된 스크립트만 캐시에서 업데이트');
                break;
            case 'all':
                console.log('모든 스크립트를 캐시에서 업데이트');
                break;
            case 'none':
                console.log('캐시를 사용하지 않고 항상 네트워크에서 업데이트');
                break;
        };
    }; // 캐시 업데이트 정책 확인 end



    // 완전한 초기화 함수
    export async function initializeServiceWorker() {
        const registration = await registerServiceWorker();
        if (registration) {
            handleServiceWorkerUpdates(registration);
            checkCacheUpdatePolicy(registration);
            await registerBackgroundSync(registration);
            
            // 정기적으로 업데이트 확인
            setInterval(() => {
                updateServiceWorker(registration);
            }, 600000); // 10분마다
        }
    }; // 완전한 초기화 함수 end
    


    // 유틸리티 함수

    // 푸시 구독 관련 키값 함수
    export function urlBase64ToUint8Array(base64String) {
        const padding = '='.repeat((4 - base64String.length % 4) % 4);
        const base64 = (base64String + padding).replace(/\-/g, '+').replace(/_/g, '/');

        const rawData = window.atob(base64);
        const outputArray = new Uint8Array(rawData.length);

        for (let i = 0; i < rawData.length; ++i) {
            outputArray[i] = rawData.charCodeAt(i);
        }
        return outputArray;
    }; // 푸시 구독 관련 키값 함수 end


    // ui에 업데이트 표시
    export function showUpdateAvailable() {
        // UI에 업데이트 알림 표시
        const updateBanner = document.createElement('div');
        updateBanner.innerHTML = `
            <div style="background: #4CAF50; color: white; padding: 10px; text-align: center; display:flex; justify-content: center; align-items: center; gap: 7.5px;">
                새 버전이 업데이트 되었습니다!
                <button 
                    onclick="window.location.reload()"
                    style="padding:2.5px; border-radius:5px; font-weight:bold;">
                    새로고침
                </button>
            </div>
        `;
        document.body.insertBefore(updateBanner, document.body.firstChild);
    }; // ui에 업데이트 표시 end



    // 사용자에게 새 버전 알림
    export function showUpdateNotification() {
        console.log('새 버전 사용 가능 - 사용자에게 알림');
        showUpdateAvailable();
    }; // 사용자에게 새 버전 알림 end


    // 실제 서버 엔드포인트로 구독 정보 전송
    export async function sendSubscriptionToServer(subscription) {
        try {
            const response = await fetch('/api/push-subscriptions', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                },
                body: JSON.stringify(subscription)
            });
            
            if (!response.ok) {
                throw new Error('구독 정보 전송 실패');
            }
        } catch (error) {
            console.error('서버 전송 실패:', error);
        }
    }; // 실제 서버 엔드포인트로 구독 정보 전송 end

document.addEventListener('DOMContentLoaded', initializeServiceWorker);