// src/component/common/MyPages/ProfileEdit.jsx
import { useEffect, useMemo, useState } from 'react';
import { getMyProfile, getMyProfileImage } from '../../../src/api/profileApi';
import '../../../css/MyPages/ProfileEdit.css';

const phoneMask = (v='') =>
  v.replace(/[^\d]/g,'').replace(/(\d{3})(\d{3,4})(\d{4})/, '$1-$2-$3');
const yyyymmddOk = (s) => /^\d{8}$/.test(s);

export default function ProfileEdit() {
  const [loading, setLoading] = useState(true);
  const [err, setErr] = useState('');
  const [msg, setMsg] = useState('');
  const [imageUrl, setImageUrl] = useState('');
  const [origin, setOrigin] = useState(null);
  const [form, setForm] = useState(null);

  useEffect(() => {
    let revoked = false;
    (async () => {
      try {
        setLoading(true); setErr(''); setMsg('');
        const wrapper = await getMyProfile();
        const p = wrapper?.data;
        if (!p) throw new Error('프로필 데이터가 없습니다.');

        setOrigin(p);
        setForm({
          userEmail: p.userEmail || '',
          userName: p.userName || '',
          userPhone: p.userPhone || '',
          userType: p.userType ?? 0,
          userTypeText: p.userTypeText || '',
          majorCode: p.majorCode || '',
          zipCode: p.zipCode || '',
          mainAddress: p.mainAddress || '',
          detailAddress: p.detailAddress || '',
          birthDate: p.birthDate || '',
          academicStatus: p.academicStatus || '',
          admissionRoute: p.admissionRoute || '',
          majorFacultyCode: p.majorFacultyCode || '',
          majorDeptCode: p.majorDeptCode || '',
          minorFacultyCode: p.minorFacultyCode || '',
          minorDeptCode: p.minorDeptCode || '',
          image: p.image || null,
        });

        if (p?.image?.hasImage && p.image.imageKey) {
          const url = await getMyProfileImage(p.image.imageKey);
          if (!revoked) setImageUrl(url);
        } else {
          setImageUrl('');
        }
      } catch (e) {
        setErr(e.message || '프로필을 불러오지 못했습니다.');
      } finally {
        setLoading(false);
      }
    })();
    return () => { revoked = true; };
  }, []);

  useEffect(() => {
    return () => {
      if (imageUrl && imageUrl.startsWith('blob:')) {
        try { URL.revokeObjectURL(imageUrl); } catch (_) {}
      }
    };
  }, [imageUrl]);

  // 학번/교번 → majorCode, 없으면 이메일 아이디
  const idText = useMemo(() => {
    if (!form) return '-';
    const code = (form.majorCode || '').toString().trim();
    if (code) return code;
    const mail = (form.userEmail || '').toString();
    return mail.includes('@') ? mail.split('@')[0] : '-';
  }, [form]);

  // 역할 텍스트: userTypeText, 없으면 '학생'
  const roleText = useMemo(() => {
    if (!form) return '학생';
    const s = (form.userTypeText || '').toString().trim();
    return s || '학생';
  }, [form]);

  const onChange = (k) => (e) => {
    let v = e.target.value ?? '';
    if (k === 'userPhone') v = v.replace(/[^\d]/g,'').slice(0,11);
    if (k === 'zipCode')   v = v.replace(/[^\d]/g,'').slice(0,5);
    if (k === 'birthDate') v = v.replace(/[^\d]/g,'').slice(0,8);
    setForm((f) => ({ ...f, [k]: v }));
  };

  const onCancel = () => {
    if (!origin) return;
    setForm({ ...origin });
    setMsg('');
  };

  const onSave = async () => {
    setMsg('');
    if (!form?.userName?.trim()) return setMsg('이름을 입력하세요.');
    if (!/^\d{10,11}$/.test(form.userPhone)) return setMsg('전화번호는 숫자 10~11자리여야 합니다.');
    if (form.birthDate && !yyyymmddOk(form.birthDate)) return setMsg('생년월일은 YYYYMMDD 8자리 형식입니다.');
    setMsg('현재 개발 중입니다. 저장 API가 아직 연결되지 않았습니다.');
  };

  if (loading) return <div>불러오는 중…</div>;
  if (err) return <div style={{ color: 'crimson' }}>에러: {err}</div>;
  if (!form) return null;

  return (
    <div id="bc-profile">
      <div className="profile-card">
        {/* 상단 중앙 제목 (원하면 이 줄을 지워도 됨) */}
        <h2 className="profile-main-title">개인정보 수정</h2>

        {/* 헤더: 아바타 | (이름 / 학번 · 역할) - 이름 아래 줄로 학번 표시 */}
        <div className="head-row">
          <div className="avatar-col">
            <img
              src={imageUrl || '/assets/default-profile.png'}
              alt="프로필"
              className="avatar avatar-lg"
            />
          </div>
          <div className="who who-vertical">
            <div className="who-name">{form.userName}</div>
            <div className="who-meta">
              학번 <strong>{idText}</strong> · {roleText}
            </div>
          </div>
        </div>

        {/* 폼: 모든 필드를 동일 간격으로 */}
        <div className="form-grid">
          <label className="field">
            <span>이름</span>
            <input value={form.userName} onChange={onChange('userName')} />
          </label>

          <label className="field">
            <span>생년월일</span>
            <input value={form.birthDate} readOnly />
          </label>

          <label className="field">
            <span>휴대폰</span>
            <input value={phoneMask(form.userPhone)} onChange={onChange('userPhone')} inputMode="numeric" />
          </label>

          <label className="field">
            <span>이메일</span>
            <input value={form.userEmail} disabled />
          </label>

          <label className="field">
            <span>주소</span>
            <textarea
              rows={2}
              value={form.mainAddress}
              onChange={onChange('mainAddress')}
              placeholder="도로명 주소"
            />
          </label>
        </div>

        <div className="actions">
          <button className="btn secondary" onClick={onCancel}>취소</button>
          <button className="btn" onClick={onSave} title="저장 API 준비 중">저장</button>
        </div>

        {msg && <div className="form-msg" data-ok={String(msg.startsWith('저장'))}>{msg}</div>}
      </div>
    </div>
  );
}
