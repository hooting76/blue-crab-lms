// component/common/Course/AdminCourseApplyNotice.jsx
import React, { useEffect, useRef, useState } from 'react';
import '../../../css/Course/AdminCourseApplyNotice.css';
import { Editor } from '@toast-ui/react-editor';
import '@toast-ui/editor/dist/toastui-editor.css';
import { viewCourseApplyNotice, saveCourseApplyNotice } from '../../../src/api/courseRegistrationApi';

export default function AdminCourseApplyNotice() {
  const [message, setMessage] = useState('');
  const [saving, setSaving] = useState(false);
  const [updatedAt, setUpdatedAt] = useState(null);
  const [dirty, setDirty] = useState(false);
  const editorRef = useRef(null);
  const bcRef = useRef(null);

  const fmt = (ts) =>
    ts ? new Date(ts).toISOString().slice(0, 10) : null;

  const loadNotice = async () => {
    try {
      const r = await viewCourseApplyNotice();
      const msg = r?.success ? (r.message || '') : '';
      setMessage(msg);
      setUpdatedAt(r?.updatedAt ?? null);
      requestAnimationFrame(() => {
        editorRef.current?.getInstance().setHTML(msg);
        setDirty(false);
      });
    } catch {}
  };

  const handleSave = async () => {
    if (!message || !message.replace(/<[^>]*>/g, '').trim()) {
      alert('안내문 내용을 입력해 주세요.');
      return;
    }
    setSaving(true);
    try {
      const r = await saveCourseApplyNotice({ message });
      if (r?.success) {
        setUpdatedAt(r?.data?.updatedAt ?? new Date().toISOString());
        setDirty(false);
        try {
          window.dispatchEvent(new CustomEvent('course-apply-notice-updated', { detail: { message } }));
          bcRef.current?.postMessage({ type: 'NOTICE_UPDATED', message });
        } catch {}
        alert('저장되었습니다.');
      } else {
        alert(r?.message || '저장 실패');
      }
    } finally { setSaving(false); }
  };

  // 초기 로드 + 채널
  useEffect(() => {
    loadNotice();
    bcRef.current = new BroadcastChannel('course-apply-notice');
    return () => { try { bcRef.current?.close(); } catch{} };
  }, []);

  // 에디터 변경
  const onChange = () => {
    const html = editorRef.current?.getInstance().getHTML() || '';
    setMessage(html);
    setDirty(true);
  };

  // 맞춤법 밑줄 끄기
  useEffect(() => {
    const root = editorRef.current?.getRootElement?.() || editorRef.current?.getRootEl?.();
    const ww = root?.querySelector('.toastui-editor-ww-container .ProseMirror');
    ww?.setAttribute('spellcheck', 'false');
    const md = root?.querySelector('.toastui-editor-md-container textarea');
    md?.setAttribute('spellcheck', 'false');
  }, [message]);

  return (
    <div className="admin-notice-wrap">
      {/* 제목 상자 */}
      <div className="admin-title-box">
        <div className="title-rail top" />
        <h2 className="admin-notice-title">📣 수강신청 안내문 관리</h2>
        <div className="title-rail bottom" />
      </div>

      {/* 에디터 카드 */}
      <section className="admin-notice-editor">
        <Editor
          ref={editorRef}
          initialValue={message}
          initialEditType="wysiwyg"
          hideModeSwitch
          usageStatistics={false}
          autofocus={false}
          height="420px"
          placeholder="수강신청 기간 및 주의사항을 입력하세요."
          toolbarItems={[
            ['heading', 'bold', 'italic', 'strike'],
            ['hr', 'quote'],
            ['ul', 'ol', 'task'],
            ['table', 'link'],
            ['code', 'codeblock'],
          ]}
          onChange={onChange}
        />
      </section>

      {/* 하단 회색 바 + 버튼 + 최근 수정 */}
      <footer className="admin-actions-bar">
        <div className="actions-left">
          <button className="btn secondary" onClick={loadNotice} disabled={saving}>되돌리기</button>
          <button className="btn btn-primary" onClick={handleSave} disabled={saving}>
            {saving ? '저장 중…' : '저장'}
          </button>
        </div>
        <div className="actions-right">
          <span>최근 수정 : {fmt(updatedAt) || '없음'}</span>
          {dirty && <span className="dirty-dot" title="수정됨"> • 수정됨</span>}
        </div>
      </footer>
    </div>
  );
}
