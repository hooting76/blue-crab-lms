// ==================== WebSocket 채팅 테스터 모듈 ====================

// 전역 변수
let stompClient = null;
let currentRequestIdx = null;
let chatMessages = [];
let isConnected = false;
let autoReadReceiptEnabled = true;
let readReceiptEvents = [];
let scheduledReadTask = null;
let cachedUserInfo = null;

// ==================== 유틸리티 ====================

/**
 * 로컬 스토리지에 저장된 JWT에서 사용자 정보를 추출한다.
 */
function getCurrentUserInfo(forceRefresh = false) {
    if (!forceRefresh && cachedUserInfo) {
        return cachedUserInfo;
    }

    const accessToken = localStorage.getItem('bluecrab_access_token');
    if (!accessToken) {
        return null;
    }

    try {
        const payloadPart = accessToken.split('.')[1];
        if (!payloadPart) {
            return null;
        }

        const payload = JSON.parse(atob(payloadPart));
        const userCode = payload.userCode || payload.sub || payload.email || null;
        const userName = payload.userName || payload.name || payload.userFullName || null;

        cachedUserInfo = {
            token: accessToken,
            userCode,
            userName,
            payload
        };

        return cachedUserInfo;
    } catch (error) {
        console.warn('JWT 파싱 실패:', error);
        return null;
    }
}

function getCurrentUserCode() {
    const info = getCurrentUserInfo();
    return info ? info.userCode : null;
}

function getCurrentUserName() {
    const info = getCurrentUserInfo();
    return info ? info.userName : null;
}

function formatDateTime(value) {
    if (!value) {
        return '-';
    }

    try {
        const date = value instanceof Date ? value : new Date(value.toString().replace(' ', 'T'));
        return date.toLocaleString('ko-KR', {
            month: '2-digit',
            day: '2-digit',
            hour: '2-digit',
            minute: '2-digit',
            second: '2-digit'
        });
    } catch (error) {
        return value;
    }
}

// ==================== WebSocket 연결 관리 ====================

/**
 * WebSocket 연결 시작
 */
function connectWebSocket() {
    const requestIdxInput = document.getElementById('chatRequestIdx');
    const requestIdx = requestIdxInput.value.trim();

    if (!requestIdx || isNaN(requestIdx)) {
        addChatLog('상담 요청 번호를 올바르게 입력해주세요.', 'error');
        return;
    }

    // JWT 토큰 확인
    const userInfo = getCurrentUserInfo(true);
    if (!userInfo || !userInfo.token) {
        addChatLog('로그인이 필요합니다. 먼저 JWT 토큰을 발급받으세요.', 'error');
        return;
    }

    const accessToken = userInfo.token;
    currentRequestIdx = parseInt(requestIdx);
    autoReadReceiptEnabled = document.getElementById('chatAutoReadToggle')
        ? document.getElementById('chatAutoReadToggle').checked
        : true;

    // 연결 상태 업데이트
    updateConnectionStatus('connecting');
    addChatLog(`WebSocket 연결 시도 중... (요청 번호: ${currentRequestIdx})`, 'info');

    try {
        // SockJS 연결 (JWT 토큰을 쿼리 파라미터로 전달)
        const socket = new SockJS(`${baseURL}/ws/chat?token=${accessToken}`);

        // STOMP over SockJS
        stompClient = Stomp.over(socket);

        // 디버그 로그 비활성화 (운영 환경)
        stompClient.debug = null;

        // 연결 시작
        stompClient.connect(
            {},  // 빈 헤더 (인증은 쿼리 파라미터로 처리)
            onWebSocketConnected,
            onWebSocketError
        );
    } catch (error) {
        addChatLog(`연결 실패: ${error.message}`, 'error');
        updateConnectionStatus('disconnected');
    }
}

/**
 * WebSocket 연결 성공 콜백
 */
function onWebSocketConnected(frame) {
    isConnected = true;
    updateConnectionStatus('connected');
    addChatLog('✅ WebSocket 연결 성공', 'success');

    const userInfo = getCurrentUserInfo();
    const userCode = userInfo?.userCode || 'UNKNOWN';
    const userName = userInfo?.userName;

    addChatLog(`👤 로그인 사용자: ${userName ? `${userName} (${userCode})` : userCode}`, 'info');

    // 채팅 메시지 큐 구독
    stompClient.subscribe('/user/queue/chat', function(message) {
        const chatMessage = JSON.parse(message.body);
        receiveMessage(chatMessage);
    });

    // 에러 메시지 큐 구독
    stompClient.subscribe('/user/queue/errors', function(message) {
        const errorMessage = message.body;
        addChatLog(`❌ 서버 에러: ${errorMessage}`, 'error');
    });

    // 읽음 확인 이벤트 구독
    stompClient.subscribe('/user/queue/read-receipts', function(message) {
        try {
            const receipt = JSON.parse(message.body);
            handleReadReceipt(receipt);
        } catch (error) {
            console.warn('읽음 이벤트 파싱 실패:', error);
            addChatLog(`읽음 이벤트 파싱 실패: ${error.message}`, 'warning');
        }
    });

    addChatLog(`📡 STOMP 구독 완료: /user/queue/chat, /user/queue/read-receipts, /user/queue/errors`, 'success');

    // 기존 메시지 로드
    fetchChatMessages(currentRequestIdx);
}

/**
 * WebSocket 연결 에러 콜백
 */
function onWebSocketError(error) {
    isConnected = false;
    updateConnectionStatus('disconnected');
    addChatLog(`❌ WebSocket 연결 실패: ${error}`, 'error');
    console.error('STOMP error:', error);
}

/**
 * WebSocket 연결 해제
 */
function disconnectWebSocket() {
    if (stompClient !== null && isConnected) {
        if (scheduledReadTask) {
            clearTimeout(scheduledReadTask);
            scheduledReadTask = null;
        }

        stompClient.disconnect(function() {
            isConnected = false;
            updateConnectionStatus('disconnected');
            addChatLog('🔌 WebSocket 연결 해제됨', 'info');
        });
    } else {
        addChatLog('연결되어 있지 않습니다.', 'warning');
    }
}

/**
 * 연결 상태 UI 업데이트
 */
function updateConnectionStatus(status) {
    const statusElement = document.getElementById('chatConnectionStatus');
    const iconElement = statusElement.querySelector('.status-icon');
    const textElement = statusElement.querySelector('.status-text');

    // 기존 클래스 제거
    statusElement.classList.remove('status-connected', 'status-disconnected', 'status-connecting');

    switch (status) {
        case 'connected':
            statusElement.classList.add('status-connected');
            iconElement.textContent = '🟢';
            textElement.textContent = 'Connected';
            break;
        case 'connecting':
            statusElement.classList.add('status-connecting');
            iconElement.textContent = '🟡';
            textElement.textContent = 'Connecting...';
            break;
        case 'disconnected':
        default:
            statusElement.classList.add('status-disconnected');
            iconElement.textContent = '🔴';
            textElement.textContent = 'Disconnected';
            break;
    }
}

// ==================== 메시지 송수신 ====================

/**
 * 메시지 전송
 */
function sendChatMessage() {
    const messageInput = document.getElementById('chatMessageInput');
    const content = messageInput.value.trim();

    if (!content) {
        addChatLog('메시지를 입력해주세요.', 'warning');
        return;
    }

    if (!isConnected || stompClient === null) {
        addChatLog('WebSocket에 연결되어 있지 않습니다.', 'error');
        return;
    }

    if (!currentRequestIdx) {
        addChatLog('상담 요청 번호가 설정되지 않았습니다.', 'error');
        return;
    }

    try {
        // STOMP 메시지 전송
        stompClient.send("/app/chat.send", {}, JSON.stringify({
            requestIdx: currentRequestIdx,
            content: content
        }));

        addChatLog(`📤 메시지 전송: "${content.substring(0, 30)}${content.length > 30 ? '...' : ''}"`, 'success');

        // 입력창 초기화
        messageInput.value = '';
        messageInput.focus();

    } catch (error) {
        addChatLog(`메시지 전송 실패: ${error.message}`, 'error');
    }
}

/**
 * 메시지 수신 (WebSocket)
 */
function receiveMessage(message) {
    addChatLog(`📥 메시지 수신: ${message.senderName || message.sender}`, 'info');

    // 메시지 목록에 추가
    chatMessages.push(message);

    // UI에 표시
    displayMessage(message);

    // 메시지 카운트 업데이트
    updateMessageCount();

    if (autoReadReceiptEnabled) {
        scheduleReadReceipt();
    }
}

/**
 * 메시지 UI 표시
 */
function displayMessage(message) {
    const messagesContainer = document.getElementById('chatMessagesContainer');

    // 빈 상태 메시지 제거
    const emptyState = messagesContainer.querySelector('.chat-messages-empty');
    if (emptyState) {
        emptyState.remove();
    }

    // JWT에서 현재 사용자 정보 추출
    const currentUserCode = getCurrentUserCode();

    // 메시지 아이템 생성
    const messageItem = document.createElement('div');
    const isSent = message.sender === currentUserCode;
    messageItem.className = `chat-message-item ${isSent ? 'sent' : 'received'}`;

    // 메시지 버블
    const bubble = document.createElement('div');
    bubble.className = `message-bubble ${isSent ? 'sent' : 'received'}`;

    // 발신자 이름 (받은 메시지만)
    if (!isSent) {
        const senderDiv = document.createElement('div');
        senderDiv.className = 'message-sender';
        senderDiv.textContent = message.senderName || message.sender;
        bubble.appendChild(senderDiv);
    }

    // 메시지 내용
    const contentDiv = document.createElement('div');
    contentDiv.className = 'message-content';
    contentDiv.textContent = message.content;
    bubble.appendChild(contentDiv);

    // 타임스탬프
    const timestampDiv = document.createElement('div');
    timestampDiv.className = 'message-timestamp';
    timestampDiv.textContent = formatMessageTime(message.sentAt);
    bubble.appendChild(timestampDiv);

    messageItem.appendChild(bubble);
    messagesContainer.appendChild(messageItem);

    // 자동 스크롤 (최신 메시지로)
    messagesContainer.scrollTop = messagesContainer.scrollHeight;
}

/**
 * 시간 포맷팅
 */
function formatMessageTime(dateTimeString) {
    if (!dateTimeString) {
        return new Date().toLocaleTimeString('ko-KR', { hour: '2-digit', minute: '2-digit' });
    }

    // "2025-10-24 14:15:30" 또는 ISO 8601 형식 파싱
    let date;
    if (dateTimeString.includes('T')) {
        date = new Date(dateTimeString);
    } else {
        // "yyyy-MM-dd HH:mm:ss" 형식
        date = new Date(dateTimeString.replace(' ', 'T'));
    }

    return date.toLocaleTimeString('ko-KR', { hour: '2-digit', minute: '2-digit' });
}

/**
 * 채팅 메시지 전체 삭제
 */
function clearChatMessages() {
    if (!confirm('채팅창의 모든 메시지를 삭제하시겠습니까?\n(서버의 메시지는 삭제되지 않습니다)')) {
        return;
    }

    chatMessages = [];
    const messagesContainer = document.getElementById('chatMessagesContainer');
    messagesContainer.innerHTML = '<div class="chat-messages-empty"><div class="chat-messages-empty-icon">💬</div><div>메시지가 없습니다.<br>WebSocket 연결 후 메시지를 전송해보세요.</div></div>';

    updateMessageCount();
    addChatLog('채팅창 메시지 삭제됨 (UI only)', 'info');
}

/**
 * 메시지 카운트 업데이트
 */
function updateMessageCount() {
    const countElement = document.getElementById('chatMessageCount');
    countElement.textContent = `메시지: ${chatMessages.length}개`;
}

// ==================== 읽음 확인 (Read Receipts) ====================

function scheduleReadReceipt(delayMs = 400) {
    if (scheduledReadTask) {
        clearTimeout(scheduledReadTask);
    }

    if (!autoReadReceiptEnabled || !currentRequestIdx || !isConnected || !stompClient) {
        return;
    }

    scheduledReadTask = setTimeout(() => {
        sendReadReceiptWs(false);
    }, delayMs);
}

function sendReadReceiptWs(manual = true) {
    if (scheduledReadTask) {
        clearTimeout(scheduledReadTask);
        scheduledReadTask = null;
    }

    if (!currentRequestIdx) {
        addChatLog('읽음 처리 실패: 상담 요청 번호가 설정되지 않았습니다.', 'error');
        return;
    }

    if (!isConnected || !stompClient) {
        addChatLog('읽음 처리 실패: WebSocket 연결이 필요합니다.', 'error');
        return;
    }

    try {
        stompClient.send("/app/chat.read", {}, JSON.stringify({
            requestIdx: currentRequestIdx
        }));

        addChatLog(manual ? '🟢 WebSocket 읽음 처리 요청 전송' : '🟢 WebSocket 자동 읽음 처리 전송', 'info');
    } catch (error) {
        addChatLog(`읽음 처리 전송 실패: ${error.message}`, 'error');
    }
}

async function sendReadReceiptRest() {
    if (!currentRequestIdx) {
        addChatLog('읽음 처리 실패: 상담 요청 번호가 설정되지 않았습니다.', 'error');
        return;
    }

    const userInfo = getCurrentUserInfo();
    if (!userInfo || !userInfo.token) {
        addChatLog('읽음 처리 실패: JWT 토큰이 필요합니다.', 'error');
        return;
    }

    try {
        addChatLog('📮 REST 읽음 처리 요청 중...', 'info');

        const response = await fetch(`${baseURL}/api/consultation/read`, {
            method: 'POST',
            headers: {
                'Authorization': `Bearer ${userInfo.token}`,
                'Content-Type': 'application/json'
            },
            body: JSON.stringify({ requestIdx: currentRequestIdx })
        });

        const data = await response.json();

        if (response.ok && data.success) {
            addChatLog('✅ REST 읽음 처리 완료', 'success');

            handleReadReceipt({
                requestIdx: data.requestIdx ?? currentRequestIdx,
                reader: getCurrentUserCode(),
                readerName: getCurrentUserName(),
                readAt: data.readAt || new Date().toISOString(),
                lastActivityAt: data.lastActivityAt,
                allMessagesRead: data.allMessagesRead,
                partnerUserCode: data.partnerUserCode,
                source: 'REST'
            });
        } else {
            const message = data.message || '읽음 처리 실패';
            addChatLog(`❌ REST 읽음 처리 실패: ${message}`, 'error');
        }
    } catch (error) {
        addChatLog(`REST 읽음 처리 오류: ${error.message}`, 'error');
    }
}

function handleReadReceipt(receipt) {
    if (!receipt) {
        return;
    }

    const event = {
        ...receipt,
        source: receipt.source || 'WS',
        receivedAt: new Date()
    };

    if (!event.readerName && event.reader && event.reader === getCurrentUserCode()) {
        event.readerName = getCurrentUserName();
    }

    readReceiptEvents.unshift(event);
    if (readReceiptEvents.length > 20) {
        readReceiptEvents.pop();
    }

    updateReadReceiptStatus();
    renderReadReceiptLog();

    const readerLabel = event.readerName || event.reader || 'Unknown';
    const readAtText = formatDateTime(event.readAt || event.receivedAt);
    const statusLabel = event.allMessagesRead === true ? '모든 메시지 읽음' : '읽지 않은 메시지 존재';
    const sourceLabel = event.source === 'REST' ? 'REST' : 'WS';

    addChatLog(`📗 읽음 이벤트(${sourceLabel}): ${readerLabel} @ ${readAtText} (${statusLabel})`, 'success');
}

function updateReadReceiptStatus() {
    const statusElement = document.getElementById('chatReadReceiptStatus');
    if (!statusElement) {
        return;
    }

    if (readReceiptEvents.length === 0) {
        statusElement.innerHTML = `
            <div class="chat-read-status-item">
                <span class="label">읽음 정보</span>
                <span class="value">수신된 읽음 이벤트가 없습니다.</span>
            </div>
        `;
        return;
    }

    const latest = readReceiptEvents[0];

    statusElement.innerHTML = `
        <div class="chat-read-status-item">
            <span class="label">최근 읽음 처리자</span>
            <span class="value">${latest.readerName || latest.reader || '-'}</span>
        </div>
        <div class="chat-read-status-item">
            <span class="label">읽음 시간</span>
            <span class="value">${formatDateTime(latest.readAt)}</span>
        </div>
        <div class="chat-read-status-item">
            <span class="label">마지막 활동</span>
            <span class="value">${formatDateTime(latest.lastActivityAt)}</span>
        </div>
        <div class="chat-read-status-item">
            <span class="label">모든 메시지 읽음?</span>
            <span class="value ${latest.allMessagesRead ? 'status-ok' : 'status-pending'}">
                ${latest.allMessagesRead ? '✅ Yes' : '⌛ No'}
            </span>
        </div>
    `;
}

function renderReadReceiptLog() {
    const logElement = document.getElementById('chatReadReceiptLog');
    if (!logElement) {
        return;
    }

    if (readReceiptEvents.length === 0) {
        logElement.innerHTML = '<div class="chat-read-placeholder">읽음 이벤트가 표시됩니다...</div>';
        return;
    }

    logElement.innerHTML = '';
    readReceiptEvents.slice(0, 10).forEach(event => {
        const item = document.createElement('div');
        item.className = 'chat-read-log-item';

        const readerLabel = event.readerName || event.reader || 'Unknown';
        const readAtText = formatDateTime(event.readAt);
        const sourceLabel = event.source === 'REST' ? 'REST' : 'WS';

        item.innerHTML = `
            <div class="row">
                <span class="reader">${readerLabel}</span>
                <span class="source-badge">${sourceLabel}</span>
            </div>
            <div class="row">
                <span class="read-at">${readAtText}</span>
                <span class="messages-status ${event.allMessagesRead ? 'status-ok' : 'status-pending'}">
                    ${event.allMessagesRead ? '모두 읽음' : '미확인 있음'}
                </span>
            </div>
        `;

        logElement.appendChild(item);
    });
}

function clearReadReceiptLog() {
    readReceiptEvents = [];
    renderReadReceiptLog();
    updateReadReceiptStatus();
    addChatLog('읽음 로그를 초기화했습니다.', 'info');
}

function onAutoReadToggleChange(event) {
    autoReadReceiptEnabled = event.target.checked;

    if (!autoReadReceiptEnabled && scheduledReadTask) {
        clearTimeout(scheduledReadTask);
        scheduledReadTask = null;
    }

    addChatLog(autoReadReceiptEnabled ? '✅ 자동 읽음 처리를 활성화했습니다.' : '⏸️ 자동 읽음 처리를 비활성화했습니다.', 'info');

    if (autoReadReceiptEnabled && isConnected && currentRequestIdx) {
        scheduleReadReceipt(200);
    }
}

// ==================== REST API 통합 ====================

/**
 * 채팅 메시지 조회 (GET /api/chat/messages/{requestIdx})
 */
async function fetchChatMessages(requestIdx) {
    if (!requestIdx) {
        requestIdx = currentRequestIdx;
    }

    if (!requestIdx) {
        addChatLog('상담 요청 번호가 설정되지 않았습니다.', 'error');
        return;
    }

    const accessToken = localStorage.getItem('bluecrab_access_token');
    if (!accessToken) {
        addChatLog('로그인이 필요합니다.', 'error');
        return;
    }

    try {
        addChatLog(`메시지 조회 중... (요청 번호: ${requestIdx})`, 'info');

        const response = await fetch(`${baseURL}/api/chat/messages/${requestIdx}`, {
            method: 'GET',
            headers: {
                'Authorization': `Bearer ${accessToken}`,
                'Content-Type': 'application/json'
            }
        });

        const data = await response.json();

        if (response.ok && data.success) {
            const messages = data.data?.messages || [];
            addChatLog(`✅ 메시지 ${messages.length}개 조회 완료`, 'success');

            // UI에 표시
            chatMessages = messages;
            renderAllMessages(messages);
            updateMessageCount();

            if (autoReadReceiptEnabled && isConnected) {
                scheduleReadReceipt(250);
            }

            // 응답창에도 표시
            if (typeof showResponse === 'function') {
                showResponse(JSON.stringify(data, null, 2), 'success');
            }
        } else {
            const errorMsg = data.message || '메시지 조회 실패';
            addChatLog(`❌ ${errorMsg}`, 'error');

            if (typeof showResponse === 'function') {
                showResponse(JSON.stringify(data, null, 2), 'error');
            }
        }
    } catch (error) {
        addChatLog(`네트워크 오류: ${error.message}`, 'error');

        if (typeof showResponse === 'function') {
            showResponse(`네트워크 오류: ${error.message}`, 'error');
        }
    }
}

/**
 * 모든 메시지 렌더링
 */
function renderAllMessages(messages) {
    const messagesContainer = document.getElementById('chatMessagesContainer');
    messagesContainer.innerHTML = '';

    if (messages.length === 0) {
        messagesContainer.innerHTML = '<div class="chat-messages-empty"><div class="chat-messages-empty-icon">💬</div><div>메시지가 없습니다.<br>WebSocket 연결 후 메시지를 전송해보세요.</div></div>';
        return;
    }

    messages.forEach(message => {
        displayMessage(message);
    });
}

/**
 * 채팅 히스토리 다운로드 (POST /api/chat/history/download/{requestIdx})
 */
async function downloadChatHistory() {
    const requestIdx = currentRequestIdx;

    if (!requestIdx) {
        addChatLog('상담 요청 번호가 설정되지 않았습니다.', 'error');
        return;
    }

    const accessToken = localStorage.getItem('bluecrab_access_token');
    if (!accessToken) {
        addChatLog('로그인이 필요합니다.', 'error');
        return;
    }

    try {
        addChatLog(`채팅 히스토리 다운로드 중... (요청 번호: ${requestIdx})`, 'info');

        const response = await fetch(`${baseURL}/api/chat/history/download/${requestIdx}`, {
            method: 'POST',
            headers: {
                'Authorization': `Bearer ${accessToken}`
            }
        });

        if (response.ok) {
            const blob = await response.blob();
            const url = window.URL.createObjectURL(blob);
            const a = document.createElement('a');
            a.href = url;
            a.download = `chat-log-${requestIdx}.txt`;
            document.body.appendChild(a);
            a.click();
            document.body.removeChild(a);
            window.URL.revokeObjectURL(url);

            addChatLog(`✅ 채팅 히스토리 다운로드 완료: chat-log-${requestIdx}.txt`, 'success');

            if (typeof showResponse === 'function') {
                showResponse('채팅 히스토리 다운로드 성공', 'success');
            }
        } else {
            const text = await response.text();
            addChatLog(`❌ 다운로드 실패: ${text}`, 'error');

            if (typeof showResponse === 'function') {
                showResponse(`다운로드 실패: ${text}`, 'error');
            }
        }
    } catch (error) {
        addChatLog(`네트워크 오류: ${error.message}`, 'error');

        if (typeof showResponse === 'function') {
            showResponse(`네트워크 오류: ${error.message}`, 'error');
        }
    }
}

/**
 * 아카이브 채팅 로그 다운로드 (GET /api/chat/archive/download/{requestIdx})
 */
async function downloadArchivedChat() {
    const requestIdx = currentRequestIdx;

    if (!requestIdx) {
        addChatLog('상담 요청 번호가 설정되지 않았습니다.', 'error');
        return;
    }

    const accessToken = localStorage.getItem('bluecrab_access_token');
    if (!accessToken) {
        addChatLog('로그인이 필요합니다.', 'error');
        return;
    }

    try {
        addChatLog(`아카이브 채팅 로그 다운로드 중... (요청 번호: ${requestIdx})`, 'info');

        const response = await fetch(`${baseURL}/api/chat/archive/download/${requestIdx}`, {
            method: 'GET',
            headers: {
                'Authorization': `Bearer ${accessToken}`
            }
        });

        if (response.ok) {
            const blob = await response.blob();
            const url = window.URL.createObjectURL(blob);
            const a = document.createElement('a');
            a.href = url;
            a.download = `archived-chat-log-${requestIdx}.txt`;
            document.body.appendChild(a);
            a.click();
            document.body.removeChild(a);
            window.URL.revokeObjectURL(url);

            addChatLog(`✅ 아카이브 로그 다운로드 완료: archived-chat-log-${requestIdx}.txt`, 'success');

            if (typeof showResponse === 'function') {
                showResponse('아카이브 로그 다운로드 성공', 'success');
            }
        } else {
            const text = await response.text();
            addChatLog(`❌ 다운로드 실패: ${text}`, 'error');

            if (typeof showResponse === 'function') {
                showResponse(`다운로드 실패 (상담이 종료되지 않았거나 아카이브가 존재하지 않음): ${text}`, 'error');
            }
        }
    } catch (error) {
        addChatLog(`네트워크 오류: ${error.message}`, 'error');

        if (typeof showResponse === 'function') {
            showResponse(`네트워크 오류: ${error.message}`, 'error');
        }
    }
}

// ==================== 로그 관리 ====================

/**
 * 채팅 로그 추가
 */
function addChatLog(message, type = 'info') {
    const logContainer = document.getElementById('chatConnectionLog');

    // 빈 상태 메시지 제거
    const placeholder = logContainer.querySelector('.chat-log-placeholder');
    if (placeholder) {
        placeholder.remove();
    }

    const logEntry = document.createElement('div');
    logEntry.className = `chat-log-entry ${type}`;

    const timestamp = new Date().toLocaleTimeString('ko-KR', {
        hour: '2-digit',
        minute: '2-digit',
        second: '2-digit'
    });

    logEntry.innerHTML = `<span class="chat-log-timestamp">[${timestamp}]</span> ${message}`;
    logContainer.appendChild(logEntry);

    // 자동 스크롤
    logContainer.scrollTop = logContainer.scrollHeight;

    // 최대 100개 제한
    const entries = logContainer.querySelectorAll('.chat-log-entry');
    if (entries.length > 100) {
        entries[0].remove();
    }
}

/**
 * 로그 전체 삭제
 */
function clearChatLog() {
    const logContainer = document.getElementById('chatConnectionLog');
    logContainer.innerHTML = '<div class="chat-log-placeholder">로그가 표시됩니다...</div>';
}

// ==================== 엔터키 이벤트 ====================

/**
 * 메시지 입력 시 엔터키 감지
 */
document.addEventListener('DOMContentLoaded', function() {
    const messageInput = document.getElementById('chatMessageInput');
    if (messageInput) {
        messageInput.addEventListener('keypress', function(e) {
            if (e.key === 'Enter' && !e.shiftKey) {
                e.preventDefault();
                sendChatMessage();
            }
        });
    }
});

// ==================== 초기화 ====================

/**
 * 채팅 테스터 초기화
 */
function initChatTester() {
    addChatLog('💬 WebSocket 채팅 테스터 초기화 완료', 'success');
    updateConnectionStatus('disconnected');
    updateMessageCount();
    updateReadReceiptStatus();
    renderReadReceiptLog();

    const autoToggle = document.getElementById('chatAutoReadToggle');
    if (autoToggle) {
        autoToggle.checked = autoReadReceiptEnabled;
        autoToggle.addEventListener('change', onAutoReadToggleChange);
    }
}

// DOM 로드 후 초기화
if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', initChatTester);
} else {
    initChatTester();
}
