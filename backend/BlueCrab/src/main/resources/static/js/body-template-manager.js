// ==================== 요청 본문 템플릿 관리 모듈 ====================

const BODY_TEMPLATE_STORAGE_KEY = 'bluecrab_body_templates';

function getStoredBodyTemplates() {
    try {
        const raw = localStorage.getItem(BODY_TEMPLATE_STORAGE_KEY);
        return raw ? JSON.parse(raw) : {};
    } catch (error) {
        console.error('본문 템플릿 로드 실패:', error);
        return {};
    }
}

// Badge 업데이트 함수
function updateBodyTemplatesBadge() {
    const badge = document.getElementById('bodyTemplatesBadge');
    if (!badge) {
        return;
    }

    const templates = getStoredBodyTemplates();
    const count = Object.keys(templates).length;
    
    badge.textContent = `${count}개 저장됨`;
}

function persistBodyTemplates(templates) {
    try {
        localStorage.setItem(BODY_TEMPLATE_STORAGE_KEY, JSON.stringify(templates));
    } catch (error) {
        console.error('본문 템플릿 저장 실패:', error);
        alert('로컬스토리지에 템플릿을 저장하지 못했습니다.');
    }
}

function loadBodyTemplateList(preferredSelection) {
    const select = document.getElementById('savedBodyTemplates');
    if (!select) {
        return;
    }

    const templates = getStoredBodyTemplates();
    const names = Object.keys(templates).sort((a, b) => a.localeCompare(b, 'ko'));
    const currentSelection = preferredSelection || select.value;

    select.innerHTML = '';

    const placeholder = document.createElement('option');
    placeholder.value = '';
    placeholder.textContent = '-- 저장된 템플릿 선택 --';
    select.appendChild(placeholder);

    names.forEach(name => {
        const option = document.createElement('option');
        option.value = name;
        option.textContent = name;
        select.appendChild(option);
    });

    if (names.includes(currentSelection)) {
        select.value = currentSelection;
    } else {
        select.value = '';
    }

    // Badge 업데이트
    updateBodyTemplatesBadge();
}

function saveBodyTemplate() {
    const nameInput = document.getElementById('bodyTemplateName');
    const requestBody = document.getElementById('requestBody');

    if (!nameInput || !requestBody) {
        return;
    }

    const name = nameInput.value.trim();
    const body = requestBody.value.trim();

    if (!name || !body) {
        alert('템플릿 이름과 요청 본문을 입력해주세요.');
        return;
    }

    const templates = getStoredBodyTemplates();
    templates[name] = body;
    persistBodyTemplates(templates);

    loadBodyTemplateList(name);
    updateBodyTemplatesBadge();
    alert(`템플릿 "${name}" 저장 완료!`);
}

function loadBodyTemplate() {
    const select = document.getElementById('savedBodyTemplates');
    if (!select) {
        return;
    }

    const name = select.value;
    if (!name) {
        return;
    }

    const templates = getStoredBodyTemplates();
    if (!templates[name]) {
        alert('선택한 템플릿을 찾을 수 없습니다.');
        loadBodyTemplateList();
        return;
    }

    const requestBody = document.getElementById('requestBody');
    if (requestBody) {
        requestBody.value = templates[name];
        autoResizeRequestBody();
    }

    const nameInput = document.getElementById('bodyTemplateName');
    if (nameInput) {
        nameInput.value = name;
    }
}

function deleteBodyTemplate() {
    const select = document.getElementById('savedBodyTemplates');
    if (!select) {
        return;
    }

    const name = select.value;
    if (!name) {
        alert('삭제할 템플릿을 선택해주세요.');
        return;
    }

    const templates = getStoredBodyTemplates();
    if (!templates[name]) {
        alert('이미 삭제되었거나 존재하지 않는 템플릿입니다.');
        loadBodyTemplateList();
        return;
    }

    if (!confirm(`"${name}" 템플릿을 삭제할까요?`)) {
        return;
    }

    delete templates[name];
    persistBodyTemplates(templates);
    loadBodyTemplateList();
    updateBodyTemplatesBadge();

    const nameInput = document.getElementById('bodyTemplateName');
    if (nameInput && nameInput.value === name) {
        nameInput.value = '';
    }

    const requestBody = document.getElementById('requestBody');
    if (requestBody) {
        autoResizeRequestBody();
    }
}

function autoResizeRequestBody() {
    const requestBody = document.getElementById('requestBody');
    if (!requestBody) {
        return;
    }

    requestBody.style.height = 'auto';
    requestBody.style.height = `${requestBody.scrollHeight}px`;
}

document.addEventListener('DOMContentLoaded', () => {
    const requestBody = document.getElementById('requestBody');
    if (requestBody) {
        requestBody.addEventListener('input', autoResizeRequestBody);
        autoResizeRequestBody();
    }

    // 페이지 로드 시 badge 초기화
    updateBodyTemplatesBadge();
    loadBodyTemplateList();
});
