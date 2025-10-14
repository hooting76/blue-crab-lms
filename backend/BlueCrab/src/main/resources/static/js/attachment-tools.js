// ==================== 첨부파일 워크플로 도구 ====================

(function() {
    const attachmentState = {
        lastBoard: null,
        lastAttachments: []
    };

    function getBaseUrl() {
        if (typeof baseURL !== 'undefined') {
            return baseURL;
        }
        return window.location.origin + window.location.pathname.replace(/\/$/, '');
    }

    function logAttachment(message, type = 'info') {
        const logContainer = document.getElementById('attachmentLog');
        if (!logContainer) {
            return;
        }

        const placeholder = logContainer.querySelector('.attachment-log-placeholder');
        if (placeholder) {
            placeholder.remove();
        }

        const entry = document.createElement('div');
        entry.className = `attachment-log-entry ${type}`;
        entry.textContent = `[${new Date().toLocaleTimeString('ko-KR')}] ${message}`;
        logContainer.appendChild(entry);
        logContainer.scrollTop = logContainer.scrollHeight;
    }

    function clearAttachmentLog() {
        const logContainer = document.getElementById('attachmentLog');
        if (!logContainer) {
            return;
        }
        logContainer.innerHTML = '<div class="attachment-log-placeholder">여기에 첨부 파일 흐름 로그가 표시됩니다.</div>';
    }

    function ensureAuthenticated() {
        if (!window.accessToken) {
            logAttachment('Access Token이 없습니다. 먼저 로그인해주세요.', 'error');
            return false;
        }
        return true;
    }

    function getBoardIdFromInput() {
        const input = document.getElementById('attachmentBoardId');
        if (!input) {
            return null;
        }
        const value = parseInt(input.value, 10);
        return Number.isNaN(value) ? null : value;
    }

    function setBoardId(value) {
        const input = document.getElementById('attachmentBoardId');
        if (input) {
            input.value = value != null ? value : '';
        }
    }

    function setManualAttachmentIds(ids) {
        const input = document.getElementById('attachmentManualIds');
        if (input) {
            input.value = Array.isArray(ids) ? ids.join(', ') : '';
        }
    }

    function formatFileSize(bytes) {
        if (!Number.isFinite(bytes) || bytes <= 0) {
            return '0 Bytes';
        }
        const units = ['Bytes', 'KB', 'MB', 'GB'];
        const index = Math.floor(Math.log(bytes) / Math.log(1024));
        const size = bytes / Math.pow(1024, index);
        return `${size.toFixed(2)} ${units[index]}`;
    }

    function tryDecodeBase64(value) {
        if (typeof value !== 'string') {
            return value;
        }
        try {
            return decodeURIComponent(escape(atob(value)));
        } catch (error) {
            return value;
        }
    }

    function updateAttachmentSummary() {
        const boardEl = document.getElementById('attachmentStateBoard');
        const filesEl = document.getElementById('attachmentStateFiles');

        if (boardEl) {
            if (attachmentState.lastBoard && attachmentState.lastBoard.boardIdx) {
                const title = attachmentState.lastBoard.boardTitle
                    ? ` (${attachmentState.lastBoard.boardTitle})`
                    : '';
                boardEl.textContent = `${attachmentState.lastBoard.boardIdx}${title}`;
            } else {
                boardEl.textContent = '없음';
            }
        }

        if (filesEl) {
            filesEl.textContent = attachmentState.lastAttachments.length > 0
                ? attachmentState.lastAttachments.join(', ')
                : '없음';
        }
    }

    async function attachmentCreateBoard(options = {}) {
        if (!ensureAuthenticated()) {
            return null;
        }

        const titleInput = document.getElementById('attachmentTitle');
        const contentInput = document.getElementById('attachmentContent');
        const boardCodeInput = document.getElementById('attachmentBoardCode');

        const boardTitle = titleInput && titleInput.value.trim()
            ? titleInput.value.trim()
            : `테스트 게시글 ${new Date().toLocaleTimeString('ko-KR')}`;
        const boardContent = contentInput && contentInput.value.trim()
            ? contentInput.value.trim()
            : `이 페이지에서 생성된 테스트 게시글입니다.\n생성 시각: ${new Date().toISOString()}`;
        const rawBoardCode = boardCodeInput ? parseInt(boardCodeInput.value, 10) : 3;
        const boardCode = Number.isNaN(rawBoardCode) ? 3 : rawBoardCode;

        if (!options.quiet) {
            logAttachment('📝 게시글 생성 요청을 전송합니다...', 'info');
        }

        try {
            const response = await fetch(`${getBaseUrl()}/api/boards/create`, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                    'Authorization': `Bearer ${accessToken}`
                },
                body: JSON.stringify({
                    boardTitle,
                    boardContent,
                    boardCode
                })
            });

            const parsed = await parseResponseBody(response);

            if (!response.ok || (parsed.isJson && parsed.body && parsed.body.success === false)) {
                const message = parsed.isJson
                    ? (parsed.body && parsed.body.message) || JSON.stringify(parsed.body, null, 2)
                    : parsed.body || response.statusText;
                logAttachment(`게시글 생성 실패: ${message}`, 'error');
                return null;
            }

            const body = parsed.isJson ? parsed.body : null;
            const boardData = body?.board || body?.data || body;
            const boardIdx = body?.boardIdx || boardData?.boardIdx;
            const decodedTitle = tryDecodeBase64(boardData?.boardTitle) || boardTitle;

            if (!boardIdx) {
                logAttachment('게시글 생성 응답에서 IDX를 확인할 수 없습니다.', 'warning');
                return null;
            }

            attachmentState.lastBoard = {
                boardIdx,
                boardTitle: decodedTitle
            };
            setBoardId(boardIdx);
            updateAttachmentSummary();

            if (!options.quiet) {
                logAttachment(`✅ 게시글 생성 성공! (IDX: ${boardIdx}, 제목: ${decodedTitle})`, 'success');
            }

            return attachmentState.lastBoard;
        } catch (error) {
            logAttachment(`게시글 생성 중 오류 발생: ${error.message}`, 'error');
            return null;
        }
    }

    async function attachmentUploadFiles(options = {}) {
        const isNumberArg = typeof options === 'number';
        const actualOptions = isNumberArg ? { boardIdx: options } : options;
        const quiet = actualOptions.silent === true;
        const boardIdx = actualOptions.boardIdx != null ? actualOptions.boardIdx : getBoardIdFromInput();

        if (!ensureAuthenticated()) {
            return [];
        }

        if (!boardIdx) {
            logAttachment('게시글 IDX를 먼저 입력하거나 생성하세요.', 'error');
            return [];
        }

        const fileInput = document.getElementById('attachmentFileInput');
        if (!fileInput || fileInput.files.length === 0) {
            logAttachment('업로드할 파일을 선택해주세요.', 'warning');
            return [];
        }

        const files = Array.from(fileInput.files);
        if (files.length > 5) {
            logAttachment('최대 5개 파일까지만 업로드할 수 있습니다.', 'warning');
            return [];
        }

        if (!quiet) {
            logAttachment(`📤 게시글 ${boardIdx}번에 ${files.length}개 파일 업로드 시작`, 'info');
        }

        const formData = new FormData();
        files.forEach(file => formData.append('files', file));

        try {
            const response = await fetch(`${getBaseUrl()}/api/board-attachments/upload/${boardIdx}`, {
                method: 'POST',
                headers: {
                    'Authorization': `Bearer ${accessToken}`
                },
                body: formData
            });

            const parsed = await parseResponseBody(response);

            if (!response.ok || (parsed.isJson && parsed.body && parsed.body.success === false)) {
                const message = parsed.isJson
                    ? (parsed.body && parsed.body.message) || JSON.stringify(parsed.body, null, 2)
                    : parsed.body || response.statusText;
                logAttachment(`파일 업로드 실패: ${message}`, 'error');
                return [];
            }

            const body = parsed.isJson ? parsed.body : null;
            const attachments = Array.isArray(body?.attachments) ? body.attachments : [];
            const attachmentIds = attachments
                .map(item => item?.attachmentIdx)
                .filter(id => id != null);

            if (attachmentIds.length === 0) {
                logAttachment('업로드 응답에서 첨부파일 ID를 찾을 수 없습니다.', 'warning');
                return [];
            }

            attachmentState.lastAttachments = attachmentIds;
            setManualAttachmentIds(attachmentIds);
            updateAttachmentSummary();

            if (!quiet) {
                logAttachment(`✅ 업로드 완료! 첨부파일 IDX: [${attachmentIds.join(', ')}]`, 'success');
            }

            return attachmentIds;
        } catch (error) {
            logAttachment(`파일 업로드 중 오류 발생: ${error.message}`, 'error');
            return [];
        }
    }

    async function attachmentLinkFiles(options = {}) {
        const isNumberArg = typeof options === 'number';
        const actualOptions = isNumberArg ? { boardIdx: options } : options;
        const quiet = actualOptions.silent === true;
        const boardIdx = actualOptions.boardIdx != null ? actualOptions.boardIdx : getBoardIdFromInput();

        if (!ensureAuthenticated()) {
            return false;
        }

        if (!boardIdx) {
            logAttachment('게시글 IDX를 먼저 입력하거나 생성하세요.', 'error');
            return false;
        }

        let attachmentIds = Array.isArray(actualOptions.attachmentIds)
            ? actualOptions.attachmentIds
            : attachmentState.lastAttachments;

        const manualField = document.getElementById('attachmentManualIds');
        if (manualField && manualField.value.trim()) {
            attachmentIds = manualField.value
                .split(',')
                .map(id => parseInt(id.trim(), 10))
                .filter(id => !Number.isNaN(id));
        }

        if (!attachmentIds || attachmentIds.length === 0) {
            logAttachment('연결할 첨부파일 IDX가 없습니다.', 'warning');
            return false;
        }

        if (!quiet) {
            logAttachment(`🔗 게시글 ${boardIdx}번에 첨부 IDX [${attachmentIds.join(', ')}] 연결`, 'info');
        }

        try {
            const response = await fetch(`${getBaseUrl()}/api/boards/link-attachments/${boardIdx}`, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                    'Authorization': `Bearer ${accessToken}`
                },
                body: JSON.stringify({ attachmentIdx: attachmentIds })
            });

            const parsed = await parseResponseBody(response);

            if (!response.ok || (parsed.isJson && parsed.body && parsed.body.success === false)) {
                const message = parsed.isJson
                    ? (parsed.body && parsed.body.message) || JSON.stringify(parsed.body, null, 2)
                    : parsed.body || response.statusText;
                logAttachment(`첨부파일 연결 실패: ${message}`, 'error');
                return false;
            }

            if (!quiet) {
                logAttachment('✅ 첨부파일 연결 성공!', 'success');
            }
            return true;
        } catch (error) {
            logAttachment(`첨부파일 연결 중 오류 발생: ${error.message}`, 'error');
            return false;
        }
    }

    async function attachmentCreateBoardWithFiles() {
        const fileInput = document.getElementById('attachmentFileInput');
        if (!fileInput || fileInput.files.length === 0) {
            logAttachment('먼저 업로드할 파일을 선택해주세요.', 'warning');
            return;
        }

        logAttachment('🚀 게시글 생성부터 첨부 연결까지 자동 실행합니다.', 'info');
        const boardInfo = await attachmentCreateBoard({ quiet: false });
        if (!boardInfo || !boardInfo.boardIdx) {
            logAttachment('게시글 생성 단계에서 중단되었습니다.', 'warning');
            return;
        }

        const uploadedIds = await attachmentUploadFiles({ boardIdx: boardInfo.boardIdx, silent: false });
        if (!uploadedIds || uploadedIds.length === 0) {
            logAttachment('파일 업로드 단계에서 중단되었습니다.', 'warning');
            return;
        }

        const linked = await attachmentLinkFiles({
            boardIdx: boardInfo.boardIdx,
            attachmentIds: uploadedIds,
            silent: false
        });

        if (linked) {
            logAttachment(`🎉 전체 워크플로 완료! 게시글 ${boardInfo.boardIdx}번에 ${uploadedIds.length}개 첨부 연결`, 'success');
        }
    }

    function attachmentUseLastBoard() {
        if (!attachmentState.lastBoard || !attachmentState.lastBoard.boardIdx) {
            logAttachment('최근 생성된 게시글 정보가 없습니다.', 'warning');
            return;
        }
        setBoardId(attachmentState.lastBoard.boardIdx);
        logAttachment(`게시글 IDX 입력란에 ${attachmentState.lastBoard.boardIdx} 적용`, 'info');
    }

    function attachmentUseLastAttachments() {
        if (!attachmentState.lastAttachments || attachmentState.lastAttachments.length === 0) {
            logAttachment('최근 업로드된 첨부파일 정보가 없습니다.', 'warning');
            return;
        }
        setManualAttachmentIds(attachmentState.lastAttachments);
        logAttachment(`첨부파일 IDX 입력란에 [${attachmentState.lastAttachments.join(', ')}] 적용`, 'info');
    }

    function registerFileInputListener() {
        const fileInput = document.getElementById('attachmentFileInput');
        if (!fileInput) {
            return;
        }

        fileInput.addEventListener('change', event => {
            const files = Array.from(event.target.files);
            if (files.length === 0) {
                logAttachment('파일 선택이 취소되었습니다.', 'warning');
                return;
            }

            logAttachment(`${files.length}개 파일이 선택되었습니다.`, 'info');
            files.forEach((file, index) => {
                logAttachment(`  ${index + 1}. ${file.name} (${formatFileSize(file.size)})`, 'info');
            });
        });
    }

    document.addEventListener('DOMContentLoaded', () => {
        // 기본 입력값 초기화
        const titleInput = document.getElementById('attachmentTitle');
        const contentInput = document.getElementById('attachmentContent');

        if (titleInput && !titleInput.value) {
            titleInput.value = `테스트 게시글 ${new Date().toLocaleTimeString('ko-KR')}`;
        }

        if (contentInput && !contentInput.value) {
            contentInput.value = `이 페이지에서 생성된 테스트 게시글입니다.\n생성 시각: ${new Date().toISOString()}`;
        }

        updateAttachmentSummary();
        registerFileInputListener();
        logAttachment('📎 첨부파일 워크플로 도구가 준비되었습니다.', 'success');
    });

    // 전역에 함수 노출
    window.attachmentCreateBoard = attachmentCreateBoard;
    window.attachmentUploadFiles = attachmentUploadFiles;
    window.attachmentLinkFiles = attachmentLinkFiles;
    window.attachmentCreateBoardWithFiles = attachmentCreateBoardWithFiles;
    window.attachmentUseLastBoard = attachmentUseLastBoard;
    window.attachmentUseLastAttachments = attachmentUseLastAttachments;
    window.clearAttachmentLog = clearAttachmentLog;
})();
