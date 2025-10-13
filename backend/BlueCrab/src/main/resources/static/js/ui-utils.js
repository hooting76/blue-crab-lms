// ==================== UI 유틸리티 모듈 ====================

// 응답 파싱 함수 (공통 사용)
async function parseResponseBody(response) {
    const contentType = response.headers.get('content-type') || '';
    const status = response.status;

    if (status === 204 || status === 205) {
        return { body: null, isJson: false, rawText: '' };
    }

    const text = await response.text();
    if (!text) {
        return { body: null, isJson: false, rawText: '' };
    }

    const isJsonType = contentType.includes('application/json');
    if (isJsonType) {
        try {
            return { body: JSON.parse(text), isJson: true, rawText: text };
        } catch (error) {
            console.warn('JSON 파싱 실패, 텍스트로 처리합니다.', error);
        }
    }

    try {
        return { body: JSON.parse(text), isJson: true, rawText: text };
    } catch (error) {
        return { body: text, isJson: false, rawText: text };
    }
}

// 응답 표시용 포맷터
function formatResponseDisplay(response, parsed) {
    const statusLine = `Status: ${response.status} ${response.statusText}`;
    if (!parsed) {
        return statusLine;
    }

    const body = parsed.isJson
        ? JSON.stringify(parsed.body, null, 2)
        : (parsed.rawText || '(no content)');
    return `${statusLine}\n\n${body}`;
}

// 토큰 페이로드 추출
function extractTokenPayload(payload) {
    if (!payload || typeof payload !== 'object') {
        return null;
    }

    if (payload.accessToken || payload.refreshToken) {
        return payload;
    }

    if (payload.data && typeof payload.data === 'object') {
        return extractTokenPayload(payload.data) || payload.data;
    }

    return null;
}

// 응답 표시 함수
function showResponse(message, type = 'info') {
    const responseDiv = document.getElementById('response');
    if (!responseDiv) {
        return;
    }
    responseDiv.textContent = message;

    responseDiv.style.background = type === 'success' ? '#27ae60' :
                                 type === 'error' ? '#e74c3c' :
                                 type === 'info' ? '#3498db' :
                                 type === 'warning' ? '#f39c12' : '#2c3e50';
}

// 응답 지우기 함수
function clearResponse() {
    const responseDiv = document.getElementById('response');
    if (!responseDiv) {
        return;
    }
    responseDiv.textContent = '여기에 API 응답이 표시됩니다...';
    responseDiv.style.background = '#2c3e50';
}

// 관리자 인증 상태 표시 헬퍼 함수
function showAdminStatus(message, type = 'info') {
    const adminStatus = document.getElementById('adminAuthStatus');
    const adminStatusText = document.getElementById('adminAuthStatusText');

    if (!adminStatus || !adminStatusText) {
        return;
    }

    adminStatus.style.display = 'block';
    adminStatusText.textContent = message;

    if (type === 'success') {
        adminStatus.style.background = '#e8f5e8';
        adminStatus.style.borderColor = '#4caf50';
    } else if (type === 'error') {
        adminStatus.style.background = '#ffebee';
        adminStatus.style.borderColor = '#f44336';
    } else {
        adminStatus.style.background = '#e3f2fd';
        adminStatus.style.borderColor = '#2196f3';
    }
}

// 도움말 모달 열기
function showHelpModal() {
    const modal = document.getElementById('helpModal');
    if (!modal) {
        return;
    }
    modal.classList.add('show');
    modal.style.display = 'flex';
}

// 도움말 모달 닫기
function closeHelpModal(event) {
    const modal = document.getElementById('helpModal');
    if (!modal) {
        return;
    }

    // event가 없거나 (버튼 클릭) 또는 모달 배경 클릭인 경우에만 닫기
    if (!event || event.target === modal) {
        modal.classList.remove('show');
        modal.style.display = 'none';
    }
}

// 접을 수 있는 섹션 토글
function toggleSection(sectionId) {
    const section = document.getElementById(sectionId);
    if (!section) {
        return;
    }
    const header = section.previousElementSibling;

    if (section.classList.contains('collapsed')) {
        section.classList.remove('collapsed');
        if (header) {
            header.classList.add('expanded');
        }
    } else {
        section.classList.add('collapsed');
        if (header) {
            header.classList.remove('expanded');
        }
    }
}

// 인증 탭 전환
function switchAuthTab(tabName) {
    // 모든 탭 버튼에서 active 클래스 제거
    const allTabs = document.querySelectorAll('.auth-tab');
    allTabs.forEach(tab => tab.classList.remove('active'));

    // 모든 탭 콘텐츠 숨기기
    const allContents = document.querySelectorAll('.auth-tab-content');
    allContents.forEach(content => content.classList.remove('active'));

    // 클릭된 탭과 해당 콘텐츠 활성화
    const clickedTab = event.target.closest('.auth-tab');
    if (clickedTab) {
        clickedTab.classList.add('active');
    }

    const targetContent = document.getElementById(`${tabName}-auth-tab`);
    if (targetContent) {
        targetContent.classList.add('active');
    }
}
