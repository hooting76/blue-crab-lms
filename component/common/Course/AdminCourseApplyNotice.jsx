// component/common/Course/AdminCourseApplyNotice.jsx
import React, { useEffect, useState, useRef } from 'react';
import '../../../css/Course/AdminCourseApplyNotice.css';
import {
  viewCourseApplyNotice,
  saveCourseApplyNotice,
} from '../../../src/api/courseRegistrationApi';

export default function AdminCourseApplyNotice() {
  const [message, setMessage] = useState('');
  const [saving, setSaving] = useState(false);
  const [lastMeta, setLastMeta] = useState(null);
  const bcRef = useRef(null);

  // 안내문 불러오기(최신)
  const loadNotice = async () => {
    try {
      const res = await viewCourseApplyNotice();
      if (res?.success) {
        setMessage(res.message || '');
        setLastMeta({ updatedAt: res.updatedAt, updatedBy: res.updatedBy });
      } else {
        setMessage('');
        setLastMeta(null);
      }
    } catch (e) {
      console.error('안내문 조회 실패:', e);
    }
  };

  useEffect(() => {
    loadNotice();
    // 브로드캐스트 채널 준비
    bcRef.current = new BroadcastChannel('course-apply-notice');
    return () => {
      try { bcRef.current?.close(); } catch {/* noop */}
    };
  }, []);

  const broadcastUpdated = () => {
    // 1) 같은 탭/라우팅을 위한 CustomEvent
    window.dispatchEvent(new CustomEvent('course-apply-notice-updated', { detail: { message } }));
    // 2) 다른 탭/창까지 즉시 반영을 위한 BroadcastChannel
    try {
      bcRef.current?.postMessage({ type: 'updated' });
    } catch (e) {
      // 채널이 없거나 에러가 나더라도 CustomEvent로는 이미 반영됨
    }
  };

  const handleSave = async () => {
    if (!message.trim()) {
      alert('안내문 내용을 입력해 주세요.');
      return;
    }
    setSaving(true);
    try {
      const res = await saveCourseApplyNotice({ message });
      if (res?.success) {
        setLastMeta({
          updatedAt: res?.data?.updatedAt ?? null,
          updatedBy: res?.data?.updatedBy ?? null,
        });
        // 저장 직후 전파
        broadcastUpdated();
        alert('저장되었습니다.');
      } else {
        alert(res?.message || '저장 실패');
      }
    } catch (e) {
      console.error('저장 실패:', e);
      alert(e?.message || '저장 중 오류가 발생했습니다.');
    } finally {
      setSaving(false);
    }
  };

  return (
    <div className="admin-notice-wrap">
      <header className="admin-notice-head">
        <h2>📢 수강신청 안내문 관리</h2>
        <div className="meta">
          {lastMeta?.updatedAt ? (
            <>
              <span>최근 수정: {lastMeta.updatedAt}</span>
              {lastMeta?.updatedBy && <span> · {lastMeta.updatedBy}</span>}
            </>
          ) : (
            <span>최근 수정 없음</span>
          )}
        </div>
      </header>

      <section className="admin-notice-editor">
        <textarea
          className="notice-textarea"
          rows={14}
          placeholder={'수강신청 기간 및 주의사항을 입력하세요.\n\n예)\n수강신청 기간: 2025-03-01 ~ 2025-03-15\n- 선착순, 정원 초과 시 대기 처리'}
          value={message}
          onChange={(e) => setMessage(e.target.value)}
        />
      </section>

      <footer className="admin-notice-actions">
        <button className="btn secondary" onClick={loadNotice} disabled={saving}>
          불러오기
        </button>
        <button className="btn btn-primary" onClick={handleSave} disabled={saving}>
          {saving ? '저장 중…' : '저장'}
        </button>
      </footer>
    </div>
  );
}
