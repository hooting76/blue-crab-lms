// component/common/Certificates/CertEnrollOrGrad.jsx
import { useEffect, useMemo, useState } from 'react';
import { getMyProfile, getMyProfileImage } from '../../../src/api/profileApi';
import { getMyRegistry } from '../../../src/api/registryApi';
import '../../../css/Certificates/Certificates.css';

export default function CertEnrollOrGrad() {
  const [loading, setLoading] = useState(true);
  const [err, setErr] = useState('');
  const [profile, setProfile] = useState(null);
  const [registry, setRegistry] = useState(null);
  const [imageUrl, setImageUrl] = useState('');
  const [docType, setDocType] = useState('enroll'); // 'enroll' | 'graduate'
  const [showPreview, setShowPreview] = useState(false);

  useEffect(() => {
    let revoked = false;
    (async () => {
      try {
        setLoading(true);
        setErr('');
        const profWrap = await getMyProfile();
        const p = profWrap?.data;
        if (!p) throw new Error('프로필 데이터 없음');
        setProfile(p);

        if (p?.image?.hasImage && p.image.imageKey) {
          try {
            const url = await getMyProfileImage(p.image.imageKey);
            if (!revoked) setImageUrl(url);
          } catch { setImageUrl(''); }
        } else {
          setImageUrl('');
        }

        const reg = await getMyRegistry();
        setRegistry(reg || null);
      } catch (e) {
        setErr(e.message || '증명서 데이터를 불러오지 못했습니다.');
      } finally {
        setLoading(false);
      }
    })();
    return () => { revoked = true; };
  }, []);

  const idText = useMemo(() => {
    const code = (registry?.userCode || profile?.majorCode || '').toString().trim();
    if (code) return code;
    const mail = (profile?.userEmail || '').toString();
    return mail.includes('@') ? mail.split('@')[0] : '-';
  }, [profile, registry]);

  const userName = profile?.userName || '-';
  const birthDate = profile?.birthDate || '';
  const birthText = birthDate ? `${birthDate.slice(0,4)}-${birthDate.slice(4,6)}-${birthDate.slice(6,8)}` : '-';
  const address = useMemo(() => {
    if (!profile) return '';
    const { zipCode, mainAddress, detailAddress } = profile;
    return zipCode
      ? `(${zipCode}) ${mainAddress || ''} ${detailAddress || ''}`.trim()
      : `${mainAddress || ''} ${detailAddress || ''}`.trim();
  }, [profile]);

  const statusText = registry?.stdStat || '재학';
  const joinPath = registry?.joinPath || '';
  const cntTerm = Number.isFinite(registry?.cntTerm) ? registry.cntTerm : null;
  const docTitle = docType === 'enroll' ? '재학증명서' : '졸업(예정)증명서';

  const openPreview = () => setShowPreview(true);
  const closePreview = () => setShowPreview(false);
  const doRealPrint = () => window.print();

  if (loading) return <div style={{ padding: 16 }}>불러오는 중…</div>;
  if (err) return <div style={{ padding: 16, color: 'crimson' }}>에러: {err}</div>;

  // 증명서 본문 (재사용)
  const Certificate = ({ printable }) => (
    <div className={`cert-paper${printable ? '' : ' no-print'}`} id={printable ? 'cert-print' : undefined}>
      <div className="cert-header">
        <div className="logo-area">
          <img src="/favicon/android-icon-72x72.png" alt="학교로고" />
        </div>
        <div className="title">{docTitle}</div>
      </div>

      <div className="cert-body">
        <div className="row">
          <div className="label">성명</div>
          <div className="value">{userName}</div>
          <div className="photo" aria-hidden="true">
            <img src={imageUrl || '/assets/default-profile.png'} alt="증명용 사진" />
          </div>
        </div>

        <div className="row">
          <div className="label">생년월일</div>
          <div className="value">{birthText}</div>
        </div>

        <div className="row">
          <div className="label">학번/교번</div>
          <div className="value">{idText}</div>
        </div>

        <div className="row">
          <div className="label">학적상태</div>
          <div className="value">{docType === 'enroll' ? (statusText || '재학') : '졸업예정'}</div>
        </div>

        {joinPath && (
          <div className="row">
            <div className="label">입학경로</div>
            <div className="value">{joinPath}</div>
          </div>
        )}

        {Number.isFinite(cntTerm) && (
          <div className="row">
            <div className="label">이수학기</div>
            <div className="value">{cntTerm}학기</div>
          </div>
        )}

        {address && (
          <div className="row">
            <div className="label">주소</div>
            <div className="value">{address}</div>
          </div>
        )}

        <div className="desc">위와 같이 {docTitle.replace('(예정)','')} 사실을 증명합니다.</div>
        <div className="sig">
          <div className="date">발급일자: {new Date().toISOString().slice(0,10)}</div>
          <div className="stamp">Blue-Crab University 총장 (인)</div>
        </div>
      </div>
    </div>
  );

  return (
    <div id="cert-page">
      {/* 상단 액션 (화면 전용) */}
      <div className="cert-actions no-print">
        <h2>증명서 발급</h2>
        <div className="spacer" />
        <button
          className={`tab ${docType === 'enroll' ? 'active' : ''}`}
          onClick={() => setDocType('enroll')}
        >
          재학증명서
        </button>
        <button
          className={`tab ${docType === 'graduate' ? 'active' : ''}`}
          onClick={() => setDocType('graduate')}
        >
          졸업(예정)증명서
        </button>

        <button className="btn primary" onClick={openPreview}>인쇄하기</button>
      </div>

      {/* 페이지 안에서 보이는 미리보기(인쇄 제외) */}
      <Certificate printable={false} />

      {/* 인쇄 미리보기 모달 — 한 번만 뜨도록 단일 렌더 */}
      {showPreview && (
        <div className="print-overlay no-print" role="dialog" aria-modal="true">
          <div className="print-modal">
            <div className="print-modal-head">
              <strong>인쇄 미리보기</strong>
              <div className="grow" />
              <button className="btn ghost" onClick={closePreview}>닫기</button>
              <button className="btn primary" onClick={doRealPrint}>실제 인쇄</button>
            </div>
            {/* 진짜 프린트 타깃 */}
            <Certificate printable />
          </div>
        </div>
      )}
    </div>
  );
}
