(function() {
    const state = {
        faculties: [],
        departments: [],
        courses: [],
        roles: [
            { value: 1, label: '학생' },
            { value: 0, label: '교수' }
        ],
        loadingOptions: false
    };

    const elements = {};

    document.addEventListener('DOMContentLoaded', () => {
        const section = document.getElementById('notificationTesterSection');
        if (!section) {
            return;
        }

        try {
            if (typeof baseURL !== 'undefined' && typeof apiClient !== 'undefined') {
                apiClient.baseUrl = baseURL;
            }
        } catch (error) {
            console.warn('⚠️ baseURL 또는 apiClient를 설정하지 못했습니다.', error);
        }

        initializeNotificationTester();
    });

    function initializeNotificationTester() {
        cacheElements();
        bindEvents();

        if (typeof apiClient === 'undefined' || typeof apiClient.post !== 'function') {
            console.error('apiClient가 초기화되지 않았습니다.');
            setFilterStatus('API 클라이언트를 초기화할 수 없습니다. 페이지를 새로고침 후 다시 시도하세요.', 'error');
            return;
        }

        resetPreview();
        clearNotificationLog();
        onFilterTypeChange();
        loadNotificationFilterOptions();
    }

    function cacheElements() {
        elements.section = document.getElementById('notificationTesterSection');
        elements.filterType = document.getElementById('notificationFilterType');
        elements.roleSelect = document.getElementById('notificationRoleSelect');
        elements.courseSelect = document.getElementById('notificationCourseSelect');
        elements.userCodes = document.getElementById('notificationUserCodes');
        elements.facultySelect = document.getElementById('notificationFacultySelect');
        elements.departmentSelect = document.getElementById('notificationDepartmentSelect');
        elements.admissionInput = document.getElementById('notificationAdmissionYears');
        elements.gradeInput = document.getElementById('notificationGradeYears');
        elements.previewBtn = document.getElementById('notificationPreviewBtn');
        elements.sendBtn = document.getElementById('notificationSendBtn');
        elements.clearLogBtn = document.getElementById('notificationClearLogBtn');
        elements.reloadBtn = document.getElementById('notificationReloadOptionsBtn');
        elements.historyBtn = document.getElementById('notificationHistoryBtn');
        elements.historyPage = document.getElementById('notificationHistoryPage');
        elements.historySize = document.getElementById('notificationHistorySize');
        elements.previewCount = document.getElementById('notificationPreviewCount');
        elements.previewDetail = document.getElementById('notificationPreviewDetail');
        elements.log = document.getElementById('notificationLog');
        elements.historyList = document.getElementById('notificationHistoryList');
        elements.filterStatus = document.getElementById('notificationFilterStatus');
        elements.title = document.getElementById('notificationTitle');
        elements.body = document.getElementById('notificationBody');
        elements.data = document.getElementById('notificationData');
    }

    function bindEvents() {
        elements.filterType.addEventListener('change', onFilterTypeChange);
        elements.previewBtn.addEventListener('click', handlePreviewClick);
        elements.sendBtn.addEventListener('click', handleSendClick);
        elements.clearLogBtn.addEventListener('click', clearNotificationLog);
        elements.reloadBtn.addEventListener('click', () => loadNotificationFilterOptions(true));
        elements.historyBtn.addEventListener('click', loadNotificationHistory);
    }

    function loadNotificationFilterOptions(force = false) {
        if (state.loadingOptions && !force) {
            return;
        }

        state.loadingOptions = true;
        setFilterStatus('필터 옵션을 불러오는 중입니다...', 'info');
        toggleFilterButtons(true);

        (async () => {
            try {
                const coursesRaw = await apiClient.post('/api/admin/filter-options/courses', {});
                const facultiesRaw = await apiClient.post('/api/admin/filter-options/faculties', {});
                const departmentsRaw = await apiClient.post('/api/admin/filter-options/departments', {});

                let rolesRaw;
                try {
                    rolesRaw = await apiClient.post('/api/admin/filter-options/roles', {});
                } catch (roleError) {
                    addNotificationLog('⚠️ 역할 옵션 로딩 실패: 기본 값을 사용합니다.', 'warning');
                    rolesRaw = state.roles;
                }

                const courses = unwrapApiResponse(coursesRaw, '강좌 목록을 불러오지 못했습니다.');
                const faculties = unwrapApiResponse(facultiesRaw, '학부 목록을 불러오지 못했습니다.');
                const departments = unwrapApiResponse(departmentsRaw, '학과 목록을 불러오지 못했습니다.');
                const roles = Array.isArray(rolesRaw) ? rolesRaw : unwrapApiResponse(rolesRaw, '역할 옵션을 불러오지 못했습니다.');

                state.courses = (courses || []).map(course => ({
                    id: course.id ?? course.lecIdx ?? null,
                    name: course.name ?? course.lecTit ?? '',
                    code: course.code ?? course.lecSerial ?? '',
                    original: course
                })).filter(course => course.id !== null);

                state.faculties = (faculties || []).map(faculty => ({
                    id: faculty.id ?? faculty.facultyId ?? null,
                    code: faculty.code ?? faculty.facultyCode ?? '',
                    name: faculty.name ?? faculty.facultyName ?? '',
                    establishedAt: faculty.establishedAt ?? null,
                    original: faculty
                })).filter(faculty => faculty.code);

                state.departments = (departments || []).map(dept => ({
                    id: dept.id ?? dept.deptId ?? null,
                    code: dept.code ?? dept.deptCode ?? '',
                    name: dept.name ?? dept.deptName ?? '',
                    facultyId: dept.facultyId ?? null,
                    facultyCode: dept.facultyCode ?? '',
                    facultyName: dept.facultyName ?? '',
                    original: dept
                })).filter(dept => dept.code && dept.facultyCode);

                state.roles = (roles || []).map(role => ({
                    value: String(role.value ?? role.id ?? '').trim(),
                    label: role.label ?? role.name ?? `코드 ${role.value ?? role.id ?? ''}`
                })).filter(role => role.value !== '');

                populateCourseSelect();
                populateFacultySelect();
                populateDepartmentSelect();
                populateRoleSelect();

                setFilterStatus('필터 옵션이 준비되었습니다. 조건을 선택해 미리보기를 실행하세요.', 'success');
            } catch (error) {
                console.error('Failed to load notification filter options:', error);
                setFilterStatus(`필터 옵션 로딩 실패: ${error.message || error}`, 'error');
                addNotificationLog(`❌ 필터 옵션 로딩 실패: ${error.message || error}`, 'error');
            } finally {
                state.loadingOptions = false;
                toggleFilterButtons(false);
            }
        })();
    }

    function populateRoleSelect() {
        const select = elements.roleSelect;
        if (!select) return;

        select.innerHTML = '<option value="">-- 역할 선택 --</option>';
        state.roles.forEach(role => {
            const option = document.createElement('option');
            option.value = role.value;
            option.textContent = `${role.label} (${role.value})`;
            select.appendChild(option);
        });
    }

    function populateCourseSelect() {
        const select = elements.courseSelect;
        if (!select) return;

        if (!state.courses.length) {
            select.innerHTML = '<option value="">강좌 데이터 없음</option>';
            return;
        }

        const sorted = [...state.courses].sort((a, b) => {
            const codeCompare = (a.code || '').localeCompare(b.code || '');
            if (codeCompare !== 0) return codeCompare;
            return (a.name || '').localeCompare(b.name || '');
        });

        select.innerHTML = '';
        sorted.forEach(course => {
            const option = document.createElement('option');
            option.value = course.id;
            const codeLabel = course.code ? `[${course.code}] ` : '';
            option.textContent = `${codeLabel}${course.name || '이름 없음'} (#${course.id})`;
            select.appendChild(option);
        });
    }

    function populateFacultySelect() {
        const select = elements.facultySelect;
        if (!select) return;

        if (!state.faculties.length) {
            select.innerHTML = '<option value="">학부 데이터 없음</option>';
            return;
        }

        const sorted = [...state.faculties].sort((a, b) => (a.code || '').localeCompare(b.code || ''));
        select.innerHTML = '';
        sorted.forEach(faculty => {
            const option = document.createElement('option');
            option.value = faculty.code;
            option.textContent = `${faculty.code} · ${faculty.name || '학부명 미상'}`;
            select.appendChild(option);
        });
    }

    function populateDepartmentSelect() {
        const select = elements.departmentSelect;
        if (!select) return;

        if (!state.departments.length) {
            select.innerHTML = '<option value="">학과 데이터 없음</option>';
            return;
        }

        const grouped = state.departments.reduce((acc, dept) => {
            const key = dept.facultyCode || '미지정';
            if (!acc[key]) {
                acc[key] = [];
            }
            acc[key].push(dept);
            return acc;
        }, {});

        const facultyNameMap = state.faculties.reduce((map, faculty) => {
            map[faculty.code] = faculty.name || '';
            return map;
        }, {});

        select.innerHTML = '';
        Object.keys(grouped).sort().forEach(facultyCode => {
            const group = document.createElement('optgroup');
            const facultyName = facultyNameMap[facultyCode] || '학부 미상';
            group.label = `${facultyCode} · ${facultyName}`;

            grouped[facultyCode]
                .sort((a, b) => (a.code || '').localeCompare(b.code || ''))
                .forEach(dept => {
                    const option = document.createElement('option');
                    option.value = `${facultyCode}|${dept.code}`;
                    option.textContent = `${dept.code} · ${dept.name || '학과명 미상'}`;
                    group.appendChild(option);
                });

            select.appendChild(group);
        });
    }

    function onFilterTypeChange() {
        const filterType = elements.filterType.value;
        const conditionalSections = elements.section.querySelectorAll('.notification-conditional');
        conditionalSections.forEach(section => {
            section.classList.remove('active');
            if (section.dataset.filter === filterType) {
                section.classList.add('active');
            }
        });

        resetPreview();
    }

    function handlePreviewClick() {
        (async () => {
            try {
                const filterCriteria = buildFilterCriteria();
                elements.previewBtn.disabled = true;
                setFilterStatus('대상자 미리보기 요청 중...', 'info');

                const responseRaw = await apiClient.post('/api/admin/notifications/preview', filterCriteria);
                const response = unwrapApiResponse(responseRaw, '대상자 미리보기에 실패했습니다.');

                const targetCount = Number.isFinite(response.targetCount)
                    ? response.targetCount
                    : Number(response?.data?.targetCount ?? 0);

                updatePreviewDisplay(targetCount, filterCriteria);
                setFilterStatus(`미리보기 완료: 총 ${targetCount.toLocaleString()}명 대상`, 'success');
                addNotificationLog(`🎯 대상 미리보기 완료 · ${targetCount.toLocaleString()}명`, 'success');
            } catch (error) {
                console.error('Preview failed:', error);
                setFilterStatus(`미리보기 실패: ${error.message || error}`, 'error');
                addNotificationLog(`❌ 미리보기 실패: ${error.message || error}`, 'error');
            } finally {
                elements.previewBtn.disabled = false;
            }
        })();
    }

    function handleSendClick() {
        (async () => {
            try {
                const filterCriteria = buildFilterCriteria();
                const title = (elements.title.value || '').trim();
                const body = (elements.body.value || '').trim();

                if (!title) {
                    throw new Error('알림 제목을 입력하세요.');
                }
                if (!body) {
                    throw new Error('알림 내용을 입력하세요.');
                }

                const payload = {
                    title,
                    body,
                    filterCriteria
                };

                const dataText = (elements.data.value || '').trim();
                if (dataText) {
                    payload.data = parseDataPayload(dataText);
                }

                elements.sendBtn.disabled = true;
                setFilterStatus('알림 발송 요청 중...', 'info');

                const responseRaw = await apiClient.post('/api/admin/notifications/send', payload);
                const response = unwrapApiResponse(responseRaw, '알림 발송에 실패했습니다.');

                const targetCount = Number.isFinite(response.targetCount)
                    ? response.targetCount
                    : Number(response?.targetCount ?? 0);
                const successCount = Number.isFinite(response.successCount)
                    ? response.successCount
                    : Number(response?.successCount ?? 0);
                const failureCount = Number.isFinite(response.failureCount)
                    ? response.failureCount
                    : Number(response?.failureCount ?? 0);

                updatePreviewDisplay(targetCount, filterCriteria);
                addNotificationLog(
                    `🚀 알림 발송 완료 · 대상 ${targetCount.toLocaleString()}명 · 성공 ${successCount.toLocaleString()} · 실패 ${failureCount.toLocaleString()}`,
                    failureCount > 0 ? 'warning' : 'success'
                );
                setFilterStatus('알림 발송이 완료되었습니다.', 'success');

                // 발송 이력을 최신 상태로 갱신
                loadNotificationHistory();
            } catch (error) {
                console.error('Send notification failed:', error);
                setFilterStatus(`알림 발송 실패: ${error.message || error}`, 'error');
                addNotificationLog(`❌ 알림 발송 실패: ${error.message || error}`, 'error');
            } finally {
                elements.sendBtn.disabled = false;
            }
        })();
    }

    function loadNotificationHistory() {
        (async () => {
            try {
                const page = Math.max(0, parseInt(elements.historyPage.value, 10) || 0);
                const size = Math.max(1, parseInt(elements.historySize.value, 10) || 10);

                elements.historyBtn.disabled = true;
                const responseRaw = await apiClient.post('/api/admin/notifications/history', { page, size });
                const response = unwrapApiResponse(responseRaw, '발송 이력 조회에 실패했습니다.');

                renderNotificationHistory(response);
                addNotificationLog(`📚 발송 이력 조회 · 페이지 ${page + 1}/${response.totalPages ?? '?'} `, 'info');
            } catch (error) {
                console.error('History fetch failed:', error);
                addNotificationLog(`❌ 발송 이력 조회 실패: ${error.message || error}`, 'error');
            } finally {
                elements.historyBtn.disabled = false;
            }
        })();
    }

    function buildFilterCriteria() {
        const type = elements.filterType.value;
        const criteria = { filterType: type };

        switch (type) {
            case 'ROLE': {
                const value = elements.roleSelect.value;
                if (!value) {
                    throw new Error('역할을 선택하세요.');
                }
                criteria.userStudent = parseInt(value, 10);
                if (Number.isNaN(criteria.userStudent)) {
                    throw new Error('역할 값이 올바르지 않습니다.');
                }
                break;
            }
            case 'COURSE': {
                const lectureIds = Array.from(elements.courseSelect.selectedOptions || [])
                    .map(option => parseInt(option.value, 10))
                    .filter(id => !Number.isNaN(id));
                if (!lectureIds.length) {
                    throw new Error('최소 한 개 이상의 강좌를 선택하세요.');
                }
                criteria.lectureIds = Array.from(new Set(lectureIds));
                break;
            }
            case 'CUSTOM': {
                const codes = parseUserCodes(elements.userCodes.value);
                if (!codes.length) {
                    throw new Error('userCode를 입력하세요.');
                }
                criteria.userCodes = codes;
                break;
            }
            case 'FACULTY': {
                const facultyCodes = Array.from(elements.facultySelect.selectedOptions || [])
                    .map(option => option.value.trim())
                    .filter(Boolean);
                if (!facultyCodes.length) {
                    throw new Error('학부를 선택하세요.');
                }
                criteria.facultyCodes = Array.from(new Set(facultyCodes));
                break;
            }
            case 'DEPARTMENT': {
                const departments = Array.from(elements.departmentSelect.selectedOptions || [])
                    .map(option => option.value.split('|'))
                    .filter(parts => parts.length === 2 && parts[0] && parts[1])
                    .map(([facultyCode, deptCode]) => ({
                        facultyCode: facultyCode.trim(),
                        deptCode: deptCode.trim()
                    }));
                if (!departments.length) {
                    throw new Error('학부/학과를 선택하세요.');
                }
                criteria.departments = departments;
                break;
            }
            case 'ADMISSION_YEAR': {
                const years = parseNumberList(elements.admissionInput.value, '입학년도', 4);
                if (!years.length) {
                    throw new Error('입학년도를 입력하세요.');
                }
                criteria.admissionYears = years;
                break;
            }
            case 'GRADE': {
                const grades = parseNumberList(elements.gradeInput.value, '학년').filter(grade => grade > 0);
                if (!grades.length) {
                    throw new Error('학년을 입력하세요.');
                }
                criteria.gradeYears = grades;
                break;
            }
            case 'ALL':
            default:
                break;
        }

        return criteria;
    }

    function parseUserCodes(value) {
        const tokens = (value || '')
            .split(/[\n,;\s]+/)
            .map(token => token.trim())
            .filter(Boolean);
        return Array.from(new Set(tokens));
    }

    function parseNumberList(value, label, digits) {
        const tokens = (value || '')
            .split(/[\n,;\s]+/)
            .map(token => token.trim())
            .filter(Boolean);

        if (!tokens.length) {
            return [];
        }

        const numbers = tokens.map(token => {
            const num = parseInt(token, 10);
            if (Number.isNaN(num)) {
                throw new Error(`${label} 값 "${token}"은 숫자가 아닙니다.`);
            }
            if (digits && token.length !== digits) {
                throw new Error(`${label}는 ${digits}자리 형식이어야 합니다. (입력값: ${token})`);
            }
            return num;
        });

        return Array.from(new Set(numbers));
    }

    function parseDataPayload(raw) {
        try {
            const data = JSON.parse(raw);
            if (data && typeof data === 'object' && !Array.isArray(data)) {
                return Object.fromEntries(Object.entries(data).map(([key, value]) => [key, String(value)]));
            }
            throw new Error('객체 형태의 JSON 이어야 합니다.');
        } catch (error) {
            throw new Error(`데이터 payload가 올바른 JSON 형식이 아닙니다: ${error.message}`);
        }
    }

    function updatePreviewDisplay(count, criteria) {
        const safeCount = Number.isFinite(count) ? count : 0;
        elements.previewCount.textContent = `${safeCount.toLocaleString()}명`;
        elements.previewDetail.textContent = buildCriteriaSummary(criteria);
    }

    function resetPreview() {
        if (!elements.previewCount || !elements.previewDetail) return;
        elements.previewCount.textContent = '-명';
        elements.previewDetail.textContent = '필터를 선택하고 "대상 미리보기"를 실행하세요.';
    }

    function clearNotificationLog() {
        if (!elements.log) return;
        elements.log.innerHTML = '<div class="notification-log-empty">아직 로그가 없습니다.</div>';
    }

    function addNotificationLog(message, level = 'info') {
        if (!elements.log) return;

        const logEntry = document.createElement('div');
        logEntry.className = `notification-log-entry ${level}`;

        const meta = document.createElement('div');
        meta.className = 'notification-log-meta';
        meta.textContent = `${formatDateTime(new Date())} · ${level.toUpperCase()}`;

        const body = document.createElement('div');
        body.className = 'notification-log-message';
        body.textContent = message;

        logEntry.appendChild(meta);
        logEntry.appendChild(body);

        if (elements.log.querySelector('.notification-log-empty')) {
            elements.log.innerHTML = '';
        }

        elements.log.prepend(logEntry);
    }

    function setFilterStatus(message, level = 'info') {
        if (!elements.filterStatus) return;
        elements.filterStatus.textContent = message;
        elements.filterStatus.className = `notification-status ${level}`;
    }

    function toggleFilterButtons(disabled) {
        if (elements.previewBtn) elements.previewBtn.disabled = disabled;
        if (elements.reloadBtn) elements.reloadBtn.disabled = disabled;
    }

    function renderNotificationHistory(page) {
        if (!elements.historyList) return;

        if (!page || !Array.isArray(page.content) || !page.content.length) {
            elements.historyList.innerHTML = '<div class="notification-history-empty">조회 결과가 여기에 표시됩니다.</div>';
            return;
        }

        const meta = document.createElement('div');
        meta.className = 'notification-history-meta';
        meta.textContent = `총 ${page.totalElements?.toLocaleString?.() ?? '?'}건 · 전체 ${page.totalPages ?? '?'}페이지 · 현재 ${page.number + 1}페이지`;

        const fragment = document.createDocumentFragment();
        fragment.appendChild(meta);

        page.content.forEach(item => {
            const entry = document.createElement('div');
            entry.className = 'notification-history-item';

            const title = document.createElement('div');
            title.className = 'history-title';
            title.textContent = item.title || '(제목 없음)';

            const counts = document.createElement('div');
            counts.className = 'history-counts';
            counts.innerHTML = `대상 ${item.targetCount?.toLocaleString?.() ?? 0}명 · 성공 ${item.successCount?.toLocaleString?.() ?? 0} · <span class="failure">실패 ${item.failureCount?.toLocaleString?.() ?? 0}</span>`;

            const metaRow = document.createElement('div');
            metaRow.className = 'history-meta';
            metaRow.innerHTML = [
                formatDateTime(item.sentAt) || '발송 시간 미상',
                item.filterType ? `필터: ${item.filterType}` : null,
                item.createdBy ? `관리자: ${item.createdBy}` : null
            ].filter(Boolean).join(' · ');

            const body = document.createElement('div');
            body.className = 'history-body-text';
            body.textContent = truncateText(item.body || '', 140);

            entry.appendChild(title);
            entry.appendChild(counts);
            entry.appendChild(metaRow);
            entry.appendChild(body);
            fragment.appendChild(entry);
        });

        elements.historyList.innerHTML = '';
        elements.historyList.appendChild(fragment);
    }

    function buildCriteriaSummary(criteria) {
        if (!criteria || typeof criteria !== 'object') {
            return '필터 정보 없음';
        }

        const lines = [`필터 타입: ${criteria.filterType}`];
        switch (criteria.filterType) {
            case 'ROLE': {
                const label = criteria.userStudent === 1 ? '학생(1)' :
                    criteria.userStudent === 0 ? '교수(0)' : String(criteria.userStudent);
                lines.push(`역할: ${label}`);
                break;
            }
            case 'COURSE': {
                const ids = criteria.lectureIds || [];
                const labels = ids.map(id => {
                    const course = state.courses.find(c => c.id === id);
                    if (course) {
                        const codeLabel = course.code ? `[${course.code}] ` : '';
                        return `${codeLabel}${course.name}`;
                    }
                    return `ID ${id}`;
                });
                lines.push(`강좌 (${ids.length}개): ${labels.join(', ')}`);
                break;
            }
            case 'CUSTOM': {
                const codes = criteria.userCodes || [];
                const sample = codes.slice(0, 5).join(', ');
                lines.push(`userCode ${codes.length}개${codes.length > 5 ? ` (예시: ${sample} …)` : codes.length ? `: ${sample}` : ''}`);
                break;
            }
            case 'FACULTY': {
                const codes = criteria.facultyCodes || [];
                const labels = codes.map(code => {
                    const faculty = state.faculties.find(f => f.code === code);
                    return faculty ? `${code} · ${faculty.name}` : code;
                });
                lines.push(`학부 (${codes.length}개): ${labels.join(', ')}`);
                break;
            }
            case 'DEPARTMENT': {
                const departments = criteria.departments || [];
                const labels = departments.map(({ facultyCode, deptCode }) => {
                    const dept = state.departments.find(d => d.facultyCode === facultyCode && d.code === deptCode);
                    if (dept) {
                        return `${facultyCode} · ${deptCode} · ${dept.name}`;
                    }
                    return `${facultyCode}/${deptCode}`;
                });
                lines.push(`학과 (${departments.length}개): ${labels.join(', ')}`);
                break;
            }
            case 'ADMISSION_YEAR': {
                const years = criteria.admissionYears || [];
                lines.push(`입학년도 (${years.length}개): ${years.join(', ')}`);
                break;
            }
            case 'GRADE': {
                const grades = criteria.gradeYears || [];
                lines.push(`학년 (${grades.length}개): ${grades.join(', ')}`);
                break;
            }
            case 'ALL':
            default:
                lines.push('전체 사용자 대상으로 발송합니다.');
                break;
        }

        return lines.join('\n');
    }

    function unwrapApiResponse(raw, fallbackMessage) {
        if (raw == null) {
            throw new Error(fallbackMessage);
        }

        if (typeof raw.success === 'boolean') {
            if (!raw.success) {
                throw new Error(raw.message || fallbackMessage);
            }
            if (raw.data !== undefined && raw.data !== null) {
                return raw.data;
            }
        }

        if (typeof raw.status === 'number' && raw.status >= 400) {
            throw new Error(raw.message || raw.error || fallbackMessage);
        }

        return raw;
    }

    function formatDateTime(value) {
        if (!value) {
            return '-';
        }

        try {
            const date = value instanceof Date ? value : new Date(String(value).replace(' ', 'T'));
            if (Number.isNaN(date.getTime())) {
                return String(value);
            }
            return date.toLocaleString('ko-KR', {
                year: 'numeric',
                month: '2-digit',
                day: '2-digit',
                hour: '2-digit',
                minute: '2-digit',
                second: '2-digit'
            });
        } catch (error) {
            return String(value);
        }
    }

    function truncateText(text, maxLength) {
        if (!text) return '';
        if (text.length <= maxLength) return text;
        return `${text.slice(0, maxLength)}…`;
    }
})();
