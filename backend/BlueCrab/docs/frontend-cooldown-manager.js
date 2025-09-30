// 프론트엔드 API 쿨타임 매니저
class ApiCooldownManager {
    constructor() {
        this.cooldowns = new Map();
        this.loadingStates = new Map();
    }

    /**
     * API 요청 전 쿨타임 체크 및 로딩 상태 설정
     * @param {string} apiKey - API 식별 키 (예: 'reading-room-status')
     * @param {number} cooldownSeconds - 쿨타임 시간(초)
     * @returns {boolean} - 요청 가능 여부
     */
    canRequest(apiKey, cooldownSeconds = 5) {
        const now = Date.now();
        const lastRequest = this.cooldowns.get(apiKey);
        
        // 이미 로딩 중인 경우
        if (this.loadingStates.get(apiKey)) {
            return false;
        }
        
        // 쿨타임 체크
        if (lastRequest && (now - lastRequest) < (cooldownSeconds * 1000)) {
            const remainingTime = Math.ceil((lastRequest + cooldownSeconds * 1000 - now) / 1000);
            this.showCooldownMessage(apiKey, remainingTime);
            return false;
        }
        
        return true;
    }

    /**
     * API 요청 시작 시 호출
     * @param {string} apiKey - API 식별 키
     */
    startRequest(apiKey) {
        this.loadingStates.set(apiKey, true);
        this.cooldowns.set(apiKey, Date.now());
        this.updateButtonStates(apiKey, true);
    }

    /**
     * API 요청 완료 시 호출
     * @param {string} apiKey - API 식별 키
     */
    endRequest(apiKey) {
        this.loadingStates.set(apiKey, false);
        this.updateButtonStates(apiKey, false);
    }

    /**
     * 쿨타임 메시지 표시
     * @param {string} apiKey - API 식별 키
     * @param {number} remainingTime - 남은 시간(초)
     */
    showCooldownMessage(apiKey, remainingTime) {
        const messages = {
            'reading-room-status': `현황 조회는 ${remainingTime}초 후 다시 가능합니다.`,
            'reading-room-reserve': `좌석 예약은 ${remainingTime}초 후 다시 가능합니다.`,
            'reading-room-checkout': `퇴실 처리는 ${remainingTime}초 후 다시 가능합니다.`,
            'reading-room-my-reservation': `예약 조회는 ${remainingTime}초 후 다시 가능합니다.`
        };
        
        const message = messages[apiKey] || `${remainingTime}초 후 다시 시도해주세요.`;
        
        // 토스트 메시지 표시 (실제 구현에 맞게 수정)
        this.showToast(message, 'warning');
    }

    /**
     * 버튼 상태 업데이트
     * @param {string} apiKey - API 식별 키
     * @param {boolean} isLoading - 로딩 상태
     */
    updateButtonStates(apiKey, isLoading) {
        const buttonMappings = {
            'reading-room-status': '.refresh-status-btn',
            'reading-room-reserve': '.reserve-seat-btn',
            'reading-room-checkout': '.checkout-btn',
            'reading-room-my-reservation': '.my-reservation-btn'
        };

        const selector = buttonMappings[apiKey];
        if (selector) {
            const buttons = document.querySelectorAll(selector);
            buttons.forEach(button => {
                button.disabled = isLoading;
                if (isLoading) {
                    button.classList.add('loading');
                    const originalText = button.textContent;
                    button.dataset.originalText = originalText;
                    button.textContent = '처리중...';
                } else {
                    button.classList.remove('loading');
                    const originalText = button.dataset.originalText;
                    if (originalText) {
                        button.textContent = originalText;
                    }
                }
            });
        }
    }

    /**
     * 토스트 메시지 표시 (실제 구현에 맞게 수정)
     * @param {string} message - 메시지 내용
     * @param {string} type - 메시지 타입 ('success', 'warning', 'error')
     */
    showToast(message, type = 'info') {
        // 실제 토스트 라이브러리나 알림 시스템에 맞게 구현
        console.warn(`[쿨타임] ${message}`);
        
        // 간단한 알림 예시
        if (typeof alert !== 'undefined') {
            // 개발 중에는 alert, 실제로는 더 나은 UI 사용
            setTimeout(() => alert(message), 100);
        }
    }

    /**
     * 특정 API 쿨타임 리셋
     * @param {string} apiKey - API 식별 키
     */
    resetCooldown(apiKey) {
        this.cooldowns.delete(apiKey);
        this.loadingStates.set(apiKey, false);
        this.updateButtonStates(apiKey, false);
    }

    /**
     * 모든 쿨타임 리셋
     */
    resetAllCooldowns() {
        this.cooldowns.clear();
        this.loadingStates.clear();
    }
}

// 전역 쿨타임 매니저 인스턴스
const apiCooldownManager = new ApiCooldownManager();

// 기존 API 호출 함수에 쿨타임 적용
async function callReadingRoomAPIWithCooldown(endpoint, data = {}, cooldownSeconds = 5) {
    const apiKey = `reading-room${endpoint.replace('/', '-')}`;
    
    // 쿨타임 체크
    if (!apiCooldownManager.canRequest(apiKey, cooldownSeconds)) {
        return null;
    }

    // 요청 시작
    apiCooldownManager.startRequest(apiKey);
    
    try {
        const result = await callReadingRoomAPI(endpoint, data);
        return result;
    } catch (error) {
        throw error;
    } finally {
        // 요청 완료
        apiCooldownManager.endRequest(apiKey);
    }
}

// 사용 예시 - 쿨타임이 적용된 API 호출
async function getReadingRoomStatusWithCooldown() {
    return await callReadingRoomAPIWithCooldown('/status', {}, 10); // 10초 쿨타임
}

async function reserveSeatWithCooldown(seatNumber) {
    return await callReadingRoomAPIWithCooldown('/reserve', { seatNumber }, 60); // 60초 쿨타임
}

async function checkoutSeatWithCooldown(seatNumber) {
    return await callReadingRoomAPIWithCooldown('/checkout', { seatNumber }, 30); // 30초 쿨타임
}

async function getMyReservationWithCooldown() {
    return await callReadingRoomAPIWithCooldown('/my-reservation', {}, 5); // 5초 쿨타임
}

/* CSS 스타일 예시 */
/*
.loading {
    opacity: 0.6;
    cursor: not-allowed;
    position: relative;
}

.loading::after {
    content: '';
    position: absolute;
    top: 50%;
    left: 50%;
    width: 16px;
    height: 16px;
    margin: -8px 0 0 -8px;
    border: 2px solid #ffffff;
    border-radius: 50%;
    border-top-color: transparent;
    animation: spin 1s linear infinite;
}

@keyframes spin {
    to {
        transform: rotate(360deg);
    }
}
*/