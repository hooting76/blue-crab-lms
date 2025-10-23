import React, { useEffect, useRef, useState } from 'react';
import '../../../css/Course/AdminCourseApplyNotice.css';
import { Editor } from '@toast-ui/react-editor';
import '@toast-ui/editor/dist/toastui-editor.css';
import {
  viewCourseApplyNotice,
  saveCourseApplyNotice,
} from '../../../src/api/courseRegistrationApi';

export default function AdminCourseApplyNotice() {
  const [message, setMessage] = useState('');
  const [saving, setSaving] = useState(false);
  const [updatedAt, setUpdatedAt] = useState(null);
  const [dirty, setDirty] = useState(false);
  const editorRef = useRef(null);
  const bcRef = useRef(null);

  // YYYY-MM-DD HH:MM:SS
  const fmt = (ts) => {
    if (!ts) return null;
    const d = new Date(ts);
    const pad = (n) => String(n).padStart(2, '0');
    const yyyy = d.getFullYear();
    const mm = pad(d.getMonth() + 1);
    const dd = pad(d.getDate());
    const hh = pad(d.getHours());
    const mi = pad(d.getMinutes());
    const ss = pad(d.getSeconds());
    return `${yyyy}-${mm}-${dd} ${hh}:${mi}:${ss}`;
  };

  // 최신 안내문 불러오기
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
    } catch {
      // noop
    }
  };

  // 저장 + 브로드캐스트
  const handleSave = async () => {
    const plain = (message || '').replace(/<[^>]*>/g, '').trim();
    if (!plain) {
      alert('안내문 내용을 입력해 주세요.');
      return;
    }
    setSaving(true);
    try {
      const r = await saveCourseApplyNotice({ message });
      if (r?.success) {
        const ts = r?.data?.updatedAt ?? new Date().toISOString();
        setUpdatedAt(ts);
        setDirty(false);

        // 같은 탭
        try {
          window.dispatchEvent(
            new CustomEvent('course-apply-notice-updated', { detail: { message } })
          );
        } catch {}

        // 다른 탭/창
        try {
          bcRef.current?.postMessage({ type: 'NOTICE_UPDATED', message });
        } catch {}

        // 저장 완료 알림
        alert('안내문 작성이 완료되었습니다.');
      } else {
        alert(r?.message || '저장 실패');
      }
    } finally {
      setSaving(false);
    }
  };

  // 초기 로딩 + 채널 준비
  useEffect(() => {
    loadNotice();
    try {
      bcRef.current = new BroadcastChannel('course-apply-notice');
    } catch {
      bcRef.current = null;
    }
    return () => {
      try { bcRef.current?.close(); } catch {}
    };
  }, []);

  // 에디터 변경
  const onChange = () => {
    const html = editorRef.current?.getInstance().getHTML() || '';
    setMessage(html);
    setDirty(true);
  };

  // 맞춤법 밑줄 끄기(WW/Md 모두)
  useEffect(() => {
    try {
      const root =
        editorRef.current?.getRootElement?.() || editorRef.current?.getRootEl?.();
      const ww = root?.querySelector('.toastui-editor-ww-container .ProseMirror');
      ww?.setAttribute('spellcheck', 'false');
      const md = root?.querySelector('.toastui-editor-md-container textarea');
      md?.setAttribute('spellcheck', 'false');
    } catch {}
  }, [message]);

  return (
    <div className="admin-notice-wrap">
      {/* 헤더 타이틀 */}
      <div className="admin-title-box">
        <div className="title-rail top" />
        <h2 className="admin-notice-title">📢 수강신청 안내문 관리</h2>
        <div className="title-rail bottom" />
      </div>

      {/* 에디터 */}
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

      {/* 하단 바: 저장 버튼만 유지 + 최근 수정 시간 HH:MM:SS 포함 */}
      <footer className="admin-actions-bar">
        <div className="actions-left" />
        <div className="actions-right" style={{ gap: 12, display: 'flex', alignItems: 'center' }}>
          <span>최근 수정 : {fmt(updatedAt) || '없음'}</span>
          {dirty && (
            <span className="dirty-dot" title="수정됨" style={{ color: '#ef4444' }}>
              • 수정됨
            </span>
          )}
          <button className="btn btn-primary" onClick={handleSave} disabled={saving}>
            {saving ? '저장 중…' : '저장'}
          </button>
        </div>
      </footer>
    </div>
  );
}
