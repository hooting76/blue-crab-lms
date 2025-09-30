// src/component/common/MyPages/ProfileEdit.jsx
import { useEffect, useMemo, useState } from 'react';
import { getMyProfile, getMyProfileImage } from '../../../src/api/profileApi';

// 전화번호 자동 하이픈
const phoneMask = (v='') =>
  v.replace(/[^\d]/g,'').replace(/(\d{3})(\d{3,4})(\d{4})/, '$1-$2-$3');
const yyyymmddOk = (s) => /^\d{8}$/.test(s);

export default function ProfileEdit() {
  const [loading, setLoading] = useState(true);
  const [err, setErr] = useState('');
  const [msg, setMsg] = useState('');
  const [imageUrl, setImageUrl] = useState('');
  const [origin, setOrigin] = useState(null); // 원본 프로필
  const [form, setForm] = useState(null);  // 편집 폼

  // 프로필 + 이미지 조회
  useEffect(() => {
    let revoked = false;
    (async () => {
      try {
        setLoading(true);
        setErr('');
        setMsg('');

        const wrapper = await getMyProfile(); // { success, message, data }
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

    return () => {
      revoked = true;
    };
  }, []);

  // objectURL 정리
  useEffect(() => {
    return () => {
      if (imageUrl && imageUrl.startsWith('blob:')) {
        try { URL.revokeObjectURL(imageUrl); } catch (_) {}
      }
    };
  }, [imageUrl]);

  const fullAddress = useMemo(() => {
    if (!form) return '';
    return form.zipCode
      ? `(${form.zipCode}) ${form.mainAddress} ${form.detailAddress}`.trim()
      : `${form.mainAddress} ${form.detailAddress}`.trim();
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
    setForm({
      userEmail: origin.userEmail || '',
      userName: origin.userName || '',
      userPhone: origin.userPhone || '',
      userType: origin.userType ?? 0,
      userTypeText: origin.userTypeText || '',
      majorCode: origin.majorCode || '',
      zipCode: origin.zipCode || '',
      mainAddress: origin.mainAddress || '',
      detailAddress: origin.detailAddress || '',
      birthDate: origin.birthDate || '',
      academicStatus: origin.academicStatus || '',
      admissionRoute: origin.admissionRoute || '',
      majorFacultyCode: origin.majorFacultyCode || '',
      majorDeptCode: origin.majorDeptCode || '',
      minorFacultyCode: origin.minorFacultyCode || '',
      minorDeptCode: origin.minorDeptCode || '',
      image: origin.image || null,
    });
    setMsg('');
  };

  const onSave = async () => {
    setMsg('');
    if (!form?.userName?.trim()) return setMsg('이름을 입력하세요.');
    if (!/^\d{10,11}$/.test(form.userPhone)) return setMsg('전화번호는 숫자 10~11자리여야 합니다.');
    if (form.birthDate && !yyyymmddOk(form.birthDate)) return setMsg('생년월일은 YYYYMMDD 8자리 형식입니다.');

    // 아직 저장 API 미구성: 안내만
    setMsg('현재 개발 중입니다. 저장 API가 아직 연결되지 않았습니다.');
  };

  if (loading) return <div>불러오는 중…</div>;
  if (err) return <div style={{ color: 'crimson' }}>에러: {err}</div>;
  if (!form) return null;

  return (
    <div className="profile-card">
      <div style={{ display:'flex', gap:16, alignItems:'center', marginBottom:12 }}>
        <img
          src={imageUrl || '/assets/default-profile.png'}
          alt="프로필"
          style={{ width:64, height:64, borderRadius:'50%', objectFit:'cover', background:'#eee' }}
        />
        <div>
          <div style={{ fontWeight:700 }}>{form.userName}</div>
          <div style={{ color:'#667' }}>{form.userEmail}</div>
        </div>
      </div>

      <div className="form">
        <label className="field"><span>이름</span>
          <input value={form.userName} onChange={onChange('userName')} />
        </label>

        <label className="field"><span>이메일</span>
          <input value={form.userEmail} readOnly />
        </label>

        <label className="field"><span>전화번호</span>
          <input value={phoneMask(form.userPhone)} onChange={onChange('userPhone')} inputMode="numeric" />
        </label>

        <label className="field"><span>생년월일(YYYYMMDD)</span>
          <input value={form.birthDate} onChange={onChange('birthDate')} inputMode="numeric" />
        </label>

        <label className="field"><span>우편번호</span>
          <input value={form.zipCode} onChange={onChange('zipCode')} inputMode="numeric" />
        </label>

        <label className="field"><span>주소</span>
          <input value={form.mainAddress} onChange={onChange('mainAddress')} />
        </label>

        <label className="field"><span>상세주소</span>
          <input value={form.detailAddress} onChange={onChange('detailAddress')} />
        </label>

        <div style={{ marginTop:8, color:'#667' }}>전체 주소: {fullAddress}</div>

        <div className="actions" style={{ marginTop:12 }}>
          <button className="btn-primary" onClick={onSave} title="저장 API 준비 중">저장</button>
          <button className="btn-ghost" onClick={onCancel}>취소</button>
        </div>

        {msg && <div style={{ marginTop:8, color: msg.startsWith('저장') ? '#0a7' : '#d00' }}>{msg}</div>}
      </div>
    </div>
  );
}
