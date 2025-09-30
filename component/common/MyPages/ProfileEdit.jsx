// component/common/MyPages/ProfileEdit.jsx
import { useEffect, useMemo, useState } from 'react';
import { getMyProfile, getMyProfileImage } from "../../../src/api/profileApi";

// 저장 API 아직 없을 때 버튼 참조 에러 방지용 상수
const UPDATE_ENABLED = false; // 나중에 true로 변경

// 전화번호 자동 하이픈
const phoneMask = (v='') =>
  v.replace(/[^\d]/g,'').replace(/(\d{3})(\d{3,4})(\d{4})/, '$1-$2-$3');
const yyyymmddOk = (s) => /^\d{8}$/.test(s);

// API 응답 대기 시간 제한 (무한로딩 방지)
const REQ_TIMEOUT_MS = 10000; // 10초
const withTimeout = (p, ms) =>
  Promise.race([
    p,
    new Promise((_, reject) => setTimeout(() => reject(new Error('요청 시간 초과')), ms))
  ]);

export default function ProfileEdit() {
  const accessToken =
    localStorage.getItem('authToken') || localStorage.getItem('accessToken') || '';

  const [loading, setLoading] = useState(true);
  const [err, setErr] = useState('');
  const [msg, setMsg] = useState('');
  const [imageUrl, setImageUrl] = useState('');
  const [origin, setOrigin] = useState(null); // 원본 프로필
  const [form, setForm] = useState(null);     // 편집 폼

  // 재시도용 키
  const [reloadKey, setReloadKey] = useState(0);

  // 1) 프로필 + 이미지 조회
  useEffect(() => {
    let unmounted = false;

    (async () => {
      try {
        setLoading(true);
        setErr('');
        setMsg('');

        // 토큰 없으면 즉시 종료(무한 로딩 방지)
        if (!accessToken) {
          throw new Error('로그인이 필요합니다. (토큰이 없습니다)');
        }

        //  타임아웃 적용해서 무한로딩 방지
        const wrapper = await withTimeout(getMyProfile(accessToken), REQ_TIMEOUT_MS); // { success, message, data, ... }
        if (unmounted) return;

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
          birthDate: p.birthDate || '',         // YYYYMMDD
          academicStatus: p.academicStatus || '',
          admissionRoute: p.admissionRoute || '',
          majorFacultyCode: p.majorFacultyCode || '',
          majorDeptCode: p.majorDeptCode || '',
          minorFacultyCode: p.minorFacultyCode || '',
          minorDeptCode: p.minorDeptCode || '',
          image: p.image || null,
        });

        // 이미지 로드 (있을 때만)
        if (p?.image?.hasImage && p.image.imageKey) {
          // 이미지도 타임아웃 적용 (이미지는 실패해도 치명적 아님)
          try {
            const url = await withTimeout(getMyProfileImage(accessToken, p.image.imageKey), REQ_TIMEOUT_MS);
            if (!unmounted) setImageUrl(url);
          } catch (_) {
            if (!unmounted) setImageUrl(''); // 실패시 기본 이미지로
          }
        } else {
          setImageUrl('');
        }
      } catch (e) {
        // 어떤 에러든 화면에 표시하고 로딩을 반드시 끔
        if (!unmounted) setErr(e.message || '프로필을 불러오지 못했습니다.');
      } finally {
        if (!unmounted) setLoading(false);
      }
    })();

    return () => {
      unmounted = true;
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [reloadKey]); //  재시도 시 다시 실행

  // objectURL 정리 (메모리 누수 방지)
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
    if (k === 'userPhone') v = v.replace(/[^\d]/g,'').slice(0,11);   // 숫자만, 최대 11
    if (k === 'zipCode')   v = v.replace(/[^\d]/g,'').slice(0,5);    // 5자리
    if (k === 'birthDate') v = v.replace(/[^\d]/g,'').slice(0,8);    // 8자리(YYYYMMDD)
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
    // 기본 클라 유효성
    if (!form?.userName?.trim()) return setMsg('이름을 입력하세요.');
    if (!/^\d{10,11}$/.test(form.userPhone)) return setMsg('전화번호는 숫자 10~11자리여야 합니다.');
    if (form.birthDate && !yyyymmddOk(form.birthDate)) return setMsg('생년월일은 YYYYMMDD 8자리 형식입니다.');

    try {
      if (!accessToken) throw new Error('로그인이 필요합니다.');

      if (!UPDATE_ENABLED) {
        setMsg('현재 개발 중입니다. 저장 API가 아직 연결되지 않았습니다.');
        return;
      }

      // TODO: 저장 API 확정 시 연결
      // const payload = { ... 필요한 필드들 ... };
      // const result = await updateMyProfile(accessToken, payload);
      // if (result?.success) { setMsg('저장되었습니다.'); setOrigin({ ...origin, ...payload }); }
      // else { setMsg(result?.message || '저장에 실패했습니다.'); }

    } catch (e) {
      setMsg(e.message || '저장 중 오류가 발생했습니다.');
    }
  };

  //  로딩/에러/재시도 UI 강화
  if (loading) return <div>불러오는 중…</div>;

  if (err) {
    return (
      <div style={{ color: 'crimson' }}>
        에러: {err}
        <div style={{ marginTop: 8 }}>
          <button className="btn-ghost" onClick={() => setReloadKey(k => k + 1)}>다시 시도</button>
        </div>
      </div>
    );
  }

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
          <button
            className="btn-primary"
            onClick={onSave}
            disabled={!UPDATE_ENABLED}
            title={!UPDATE_ENABLED ? '저장 API가 아직 연결되지 않았습니다.' : undefined}
          >저장</button>
          <button className="btn-ghost" onClick={onCancel}>취소</button>
        </div>

        {msg && <div style={{ marginTop:8, color: msg.startsWith('저장') ? '#0a7' : '#d00' }}>{msg}</div>}
      </div>
    </div>
  );
}
