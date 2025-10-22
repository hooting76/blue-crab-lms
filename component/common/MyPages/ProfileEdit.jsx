import { useEffect, useMemo, useState } from 'react';
import { getMyProfile, getMyProfileImage } from '../../../src/api/profileApi';
import Modal from 'react-modal';
import DaumPostCode from 'react-daum-postcode';
import { FaUser, FaWindowClose, FaArrowRight} from 'react-icons/fa';
import '../../../css/MyPages/ProfileEdit.css';
import ProfileModalCss from '../../../css/modules/profileModal.module.css'

const phoneMask = (v = '') =>
  v.replace(/[^\d]/g, '').replace(/(\d{3})(\d{3,4})(\d{4})/, '$1-$2-$3');
const yyyymmddOk = (s) => /^\d{8}$/.test(s);

export default function ProfileEdit() {
  const [loading, setLoading] = useState(true);
  const [err, setErr] = useState('');
  const [msg, setMsg] = useState('');
  const [imageUrl, setImageUrl] = useState('');
  const [origin, setOrigin] = useState(null);
  const [form, setForm] = useState(null);

  const [changeProfileImg, setChangeProfileImg] = useState('');  
  const [imgFile, setImgFile] = useState('');
  const [modalLoading, setModalLoading] = useState(false);

  // profile img upload state start
  const [profileModalOpen, setProfileModalOpen] = useState(false);

  const openProfileModal = () => {
    setProfileModalOpen(true);
    setChangeProfileImg('');
    setImgFile('');
  }
  const closeProfileModal = () => {
    setProfileModalOpen(false);
    setChangeProfileImg('');
    setImgFile('');
  }
  
  Modal.setAppElement("#root"); // 접근 설정

  // importImg start
  function importImg(file){
    const fileTrg = file.target.files[0]; 
    const fileRegrex = /(.*?)\.(jpg|png)$/;
    const reader = new FileReader();
    const maxSize = 5 * 1024 * 1024; // 5MB

    if(fileTrg.size > maxSize){ // file size check
      alert("파일 용량을 5MB 이하로 맞춰주세요.");
      file.target.files == null;
      return;
    }else{
      // file type check
      if(!fileRegrex.test(file.target.value)){
        alert("업로드할 파일을 잘못선택했거나, 파일이 없습니다.");
        return;
      }
      // file url get
      reader.onload = function(evt) {
        const filePath = evt.target.result;
        setChangeProfileImg(filePath);
        setImgFile(Object(fileTrg));
        // 객체타입으로 파일 이미지 저장
        return;
      };
      reader.readAsDataURL(fileTrg); // 파일 내용 data url 포맷
    }
  }; // importImg end


  // change profile func init
  async function requestChangeProfileImg(){
    if(!imgFile || !changeProfileImg){
      alert('이미지 업로드를 확인하세요.');
      return;
    }; // first branch

    // enum val
    const BASE_URL = 'https://bluecrab.chickenkiller.com/BlueCrab-1.0.0/api';    
    const fileObj = imgFile;
    const token = localStorage.getItem('accessToken');

    const formData = new FormData();
    formData.append('file',fileObj);

    console.log('formData',formData);    

    // second brench
    if(confirm('정말 해당 이미지로 프로필사진을 등록할까요?')){
      try {
        const response = await fetch(`${BASE_URL}/profile/me/upload-image`,{
          method: 'POST',
          headers: {
            Authorization: `Bearer ${token}`,
          },
          body: formData,
        });
        const result = await response.json();

        // console.log('result', result);
        // response fail
        if(!response.ok || !result.success){
          throw new Error(result.message || '업로드 실패');
        };

        // response success
        if(result.success){
          console.log('result :', result);
        };

      } catch (error) {
        alert('이미지 업로드에 실패했습니다.');
        console.log('에러 :', error);
        return;
      } // try catch end
    }else{
      alert('사용자 요청으로 취소되었습니다.');
      return;
    };
  };// change profile func end


  // postcode state start
  
  // postcode state end

  useEffect(() => {
    let revoked = false;
    (async () => {
      try {
        setLoading(true);
        setErr('');
        setMsg('');

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
    return () => {
      revoked = true;
    };
  }, []);

  useEffect(() => {
    return () => {
      if (imageUrl && imageUrl.startsWith('blob:')) {
        try {
          URL.revokeObjectURL(imageUrl);
        } catch {}
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

  // 역할 텍스트
  const roleText = useMemo(() => {
    if (!form) return '학생';
    const t = (form.userTypeText || '').toString().trim();
    return t || '학생';
  }, [form]);

  const onChange = (k) => (e) => {
    let v = e.target.value ?? '';
    if (k === 'userPhone') v = v.replace(/[^\d]/g, '').slice(0, 11);
    if (k === 'zipCode') v = v.replace(/[^\d]/g, '').slice(0, 5);
    if (k === 'birthDate') v = v.replace(/[^\d]/g, '').slice(0, 8);
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
    if (!/^\d{10,11}$/.test(form.userPhone))
      return setMsg('전화번호는 숫자 10~11자리여야 합니다.');
    if (form.birthDate && !yyyymmddOk(form.birthDate))
      return setMsg('생년월일은 YYYYMMDD 8자리 형식입니다.');
    setMsg('현재 개발 중입니다. 저장 API가 아직 연결되지 않았습니다.');
  };

  if (loading) return <div>불러오는 중…</div>;
  if (err) return <div style={{ color: 'crimson' }}>에러: {err}</div>;
  if (!form) return null;
  
  return (
    <div id="bc-profile">
      <div className="profile-card">
        <div className="head-row">
          <div 
            className="avatar-col"
            onClick={openProfileModal}
          >
            {imageUrl ?
            (<img
              src={imageUrl}
              alt="프로필"
              className="avatar avatar-lg"
            />) : (
              <FaUser />
            )}
          </div>
          <div className="who">
            <div className="who-name-row">
              <span className="who-name">{form.userName}</span>
              <span className="who-sep">|</span>
              <span className="who-role">{roleText}</span>
            </div>
            <div className="who-sub">
              학번 <strong>{idText}</strong>
            </div>
          </div>
        </div>

        {/* 폼 */}
        <div className="form-grid">
          <label className="field">
            <span>이름</span>
            <input 
              type='text'
              value={form.userName} 
              onChange={onChange('userName')} 
            />
          </label>

          <label className="field">
            <span>생년월일</span>
            <input 
              type='text'
              value={form.birthDate} 
              disabled
            />
          </label>

          <label className="field">
            <span>휴대폰</span>
            <input
              type='text'
              value={phoneMask(form.userPhone)}
              onChange={onChange('userPhone')}
              inputMode="numeric"
            />
          </label>

          <label className="field">
            <span>이메일</span>
            <input 
              type='text'
              value={form.userEmail} 
              disabled 
            />
          </label>

          {/* 주소: 라벨 오른쪽 한 줄에 3분할 (네가 준 CSS의 .addr-group 구조) */}
          <label className="field">
            <span>주소</span>
            <div className="addr-group">
              <div className="zipWrap">
                <input
                  type='number'
                  className="zip"
                  value={form.zipCode}
                  onChange={onChange('zipCode')}
                  placeholder="우편번호(5자)"
                  inputMode="numeric"
                  name="zip-code"
                />
                <button>
                  검색
                </button>
              </div>

              <input
                type='text'
                className="addr-main"
                value={form.mainAddress}
                onChange={onChange('mainAddress')}
                placeholder="기본 주소"
                name="main-address"
              />

              <input
                type='text'
                className="addr-detail"
                value={form.detailAddress}
                onChange={onChange('detailAddress')}
                placeholder="상세 주소"
                name="sub-address"
              />
            </div>
          </label>
        </div>

        <div className="actions">
          <button className="btn secondary" onClick={onCancel}>
            취소
          </button>
          <button className="btn" onClick={onSave} title="저장 API 준비 중">
            저장
          </button>
        </div>

        {msg && (
          <div className="form-msg" data-ok={String(msg.startsWith('저장'))}>
            {msg}
          </div>
        )}

        {/* profile img uploadFrm modal start */}
        <Modal
          isOpen={!!profileModalOpen}
          onRequestClose={closeProfileModal}
          contentLabel='프로필사진 바꾸기'
          style={{
              overlay:{
                  zIndex: 9999,
                  backgroundColor: "rgba(0, 0, 0, .25)",
              },
              content:{
                  width: "100%",
                  maxWidth: "450px",
                  height: "100%",
                  maxHeight: "75%",
                  boxSizing: "border-box",
                  margin: "auto",
                  borderRadius: "10px",
                  padding: "20px",
              },
          }}
        >{/* profile modal header */}
          {profileModalOpen && (
            <div className={ProfileModalCss.profileModalWrap}>
              <div className={ProfileModalCss.modalHeader}>
                <h5>
                  프로필 변경
                </h5>
                <button 
                  onClick={closeProfileModal}
                  className={ProfileModalCss.closeBtn}
                >
                  <FaWindowClose />
                </button>
              </div>{/* header end */}


              <div className={ProfileModalCss.modalMain}>
                <div className={ProfileModalCss.elModalList}>
                  <h6>기존 프로필</h6>
                  <div className={ProfileModalCss.beforeProfile}>
                    {imageUrl 
                      ? (<img src={imageUrl} alt="기존 프로필"/>)
                      : (<p>등록한 기존 프로필이 없습니다.</p>)
                    }
                  </div>
                </div>

                <div className={ProfileModalCss.elModalList}>
                  <FaArrowRight/>
                </div>

                <div className={ProfileModalCss.elModalList}>
                  <h6>변경할 프로필</h6>
                  <div className={ProfileModalCss.afterProfile}>
                    <input 
                      type="file" 
                      name="changeImg" 
                      id="changeImg" 
                      accept=".jpg, .png"
                      onChange={importImg}
                      placeholder='jpg, png만 허용'
                    />
                    <div className={ProfileModalCss.imgWrap}>
                      {changeProfileImg
                        ? (<img src={changeProfileImg} alt="교체할 프로필"/>)
                        : (<p>아직 업로드된 이미지가 없습니다.</p>)
                      }
                    </div>
                  </div>
                </div>                
              </div>

              <div className={ProfileModalCss.botBtn}>
                <input type="button" value="취소" onClick={closeProfileModal} />
                <input type="button" value="확인" onClick={requestChangeProfileImg}/>
              </div>

              {modalLoading && (
                <div className={ProfileModalCss.modalLoading}>

                </div>
              )}
            </div>
          )}
        </Modal>
        {/* profile modal end */}
        {/* profile img uploadFrm modal end */}
      </div>
    </div>
  );
}
