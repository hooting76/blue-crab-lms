// ==================== 히스토리 관리 모듈 ====================

// 히스토리에 저장
function saveToHistory(request) {
    let history = JSON.parse(localStorage.getItem('bluecrab_request_history') || '[]');

    const historyItem = {
        id: Date.now().toString(),
        ...request,
        timestamp: new Date().toISOString()
    };

    history.unshift(historyItem); // 최신 항목을 앞에

    // 최대 10개만 유지
    if (history.length > 10) {
        history = history.slice(0, 10);
    }

    localStorage.setItem('bluecrab_request_history', JSON.stringify(history));
    loadRequestHistory();
}

// 히스토리 목록 로드
function loadRequestHistory() {
    const history = JSON.parse(localStorage.getItem('bluecrab_request_history') || '[]');
    const container = document.getElementById('historyList');

    if (history.length === 0) {
        container.innerHTML = '<div class="storage-empty">히스토리가 없습니다</div>';
        return;
    }

    container.innerHTML = history.map(item => {
        // 요청 본문이 있으면 미리보기 생성
        let bodyPreview = '';
        if (item.body && item.body.trim()) {
            try {
                const parsedBody = JSON.parse(item.body);
                const keys = Object.keys(parsedBody);

                if (keys.length > 0) {
                    // 첫 2개 키만 표시
                    const previewKeys = keys.slice(0, 2);
                    const previewPairs = previewKeys.map(key => {
                        let value = parsedBody[key];
                        // 값이 너무 길면 자르기
                        if (typeof value === 'string' && value.length > 20) {
                            value = value.substring(0, 20) + '...';
                        } else if (typeof value === 'object') {
                            value = '{...}';
                        }
                        return `${key}: ${JSON.stringify(value)}`;
                    }).join(', ');

                    const moreText = keys.length > 2 ? `, +${keys.length - 2}개` : '';
                    bodyPreview = `<div class="history-body-preview" title="${escapeHtml(item.body)}">
                        📄 { ${previewPairs}${moreText} }
                    </div>`;
                }
            } catch (e) {
                // JSON 파싱 실패 시 원본 텍스트의 일부만 표시
                const preview = item.body.substring(0, 50);
                bodyPreview = `<div class="history-body-preview" title="${escapeHtml(item.body)}">
                    📄 ${escapeHtml(preview)}${item.body.length > 50 ? '...' : ''}
                </div>`;
            }
        }

        return `
            <div class="history-item">
                <div class="history-item-header">
                    <span class="history-method ${item.method}">${item.method}</span>
                    <span class="history-status ${item.success ? 'success' : 'error'}">
                        ${item.status} | ${item.duration}ms
                    </span>
                </div>
                <div style="font-size: 11px; color: #555; margin-bottom: 5px;">
                    ${item.endpoint}
                </div>
                ${bodyPreview}
                <div class="storage-actions">
                    <button onclick="reExecuteRequest('${item.id}')" class="success">재실행</button>
                    <button onclick="deleteHistory('${item.id}')" class="danger">삭제</button>
                </div>
            </div>
        `;
    }).join('');
}

// HTML 이스케이프 헬퍼 함수
function escapeHtml(text) {
    const div = document.createElement('div');
    div.textContent = text;
    return div.innerHTML;
}

// 히스토리에서 요청 재실행
function reExecuteRequest(id) {
    const history = JSON.parse(localStorage.getItem('bluecrab_request_history') || '[]');
    const item = history.find(h => h.id === id);

    if (!item) {
        alert('히스토리 항목을 찾을 수 없습니다.');
        return;
    }

    // 폼 필드 채우기
    document.getElementById('httpMethod').value = item.method;

    if (item.body) {
        document.getElementById('requestBody').value = item.body;
    }

    // 커스텀 URL 설정
    document.getElementById('testEndpoint').value = 'custom';
    document.getElementById('customUrl').value = item.endpoint;
    updateEndpointInfo();

    // 자동 실행
    sendRequest();
}

// 히스토리 항목 삭제
function deleteHistory(id) {
    let history = JSON.parse(localStorage.getItem('bluecrab_request_history') || '[]');
    history = history.filter(h => h.id !== id);
    localStorage.setItem('bluecrab_request_history', JSON.stringify(history));

    loadRequestHistory();
}

// 히스토리 전체 삭제
function clearHistory() {
    if (!confirm('정말 모든 히스토리를 삭제하시겠습니까?')) {
        return;
    }

    localStorage.removeItem('bluecrab_request_history');
    loadRequestHistory();
}
