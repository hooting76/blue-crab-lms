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

  const [today, setToday] = useState(new Date().toLocaleString());

  // image path base64
  const [changeProfileImg, setChangeProfileImg] = useState('');  
  const [imgFile, setImgFile] = useState(''); // image file object
  const [modalLoading, setModalLoading] = useState(false); // modal loading

  // postcode state init
  const [postcode, setPostcode] = useState(''); // postcode 
  const [mainAddress, setMainAddress] = useState(''); // main address
  const [subAddress, setSubAddress] = useState(''); // sub address
  const [postcodeModalOpen, setPostcodeModalOpen] = useState(false); // postcode modal open / close state

  // post modal open / close state reset
  const openPostModal = () => {
    setPostcodeModalOpen(true);
    setPostcode('');
    setMainAddress('');
    setSubAddress('');
  };

  const closePostModal = () => {
    setPostcodeModalOpen(false);
    setPostcode('');
    setMainAddress('');
    setSubAddress('');    
  };
  // postcode state end

  // postcode style set init
  const postcodeStyle = {
    display: "block",
    width: "100%",
    height: "100%",
  }
  // postcode style set end


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
  
  Modal.setAppElement("#root"); // 모달창 접근 설정

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


  // change profile img func init
  async function requestChangeProfileImg(){
    if(!imgFile || !changeProfileImg){
      alert('이미지 업로드를 확인하세요.');
      return;
    }; // first branch

    // second brench
    if(confirm('정말 해당 이미지로 프로필사진을 등록할까요?')){
      // enum val
      const BASE_URL = 'https://bluecrab.chickenkiller.com/BlueCrab-1.0.0/api';
      const fileObj = imgFile;
      const token = localStorage.getItem('accessToken');

      const formData = new FormData();
      formData.append('file',fileObj);

      const response = await fetch(`${BASE_URL}/profile/me/upload-image`,{
        method: 'POST',
        headers: {
          Authorization: `Bearer ${token}`,
        },
        body: formData,
      });
      const result = response.json();

      result.then((data) => {
        if(data.success){
          setImageUrl(changeProfileImg);
          alert('성공적으로 변경되었습니다.');
          setProfileModalOpen(false);
          setChangeProfileImg('');
          setImgFile('');
          return;
        }else{
          throw new Error(result.message || '업로드 실패');
        };
      });
    }else{
      alert('사용자 요청으로 취소되었습니다.');
      return;
    };
  };// change profile img func end


  // postcode state start
  const handleAddress = (data) => {
    let fullAddr = data.address;
    let subAddress = '';

    // result data selecting & formating
    if(data.bname !== '' && /[동|로|가]$/g.test(data.bname)){
      subAddress += data.bname;
    }
    if(data.buildingName !== '' && data.apartment === 'Y'){
      subAddress += subAddress !== ''
        ? `, ${data.buildingName}`
        : data.buildingName
    };
    if(subAddress !== ''){
      subAddress = `(${subAddress})`;
    };

    // state value insert
    setPostcode(data.zonecode);
    setMainAddress(fullAddr);
    setSubAddress(subAddress);
    form.zipCode = data.zonecode;
    form.mainAddress = fullAddr;
    form.detailAddress = subAddress;
    setPostcodeModalOpen(false);
  };// postcode state end


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

  const onSave = async () => {
    setMsg('');
    if(!form?.userName?.trim()){
      setMsg('이름을 입력하세요.');
      return alert(msg);
    };
    if(!/^\d{10,11}$/.test(form.userPhone)){
      setMsg('전화번호는 숫자 11자리여야 합니다.');
      return alert(msg);
    };
    if(form.birthDate && !yyyymmddOk(form.birthDate)){
      setMsg('생년월일은 YYYYMMDD 8자리 형식입니다.');
      return alert(msg);
    };

    if(confirm('현재 정보로 저장할까요?')){

      // address submit function init
      // enum val
      const BASE_URL = 'https://bluecrab.chickenkiller.com/BlueCrab-1.0.0/api';
      const token = localStorage.getItem('accessToken');

      // address request init
      const response = await fetch(`${BASE_URL}/profile/address/update`,{
        method: 'POST',
        headers: {
          "Authorization": `Bearer ${token}`,
          "Content-Type": 'application/json',
        },
        body: JSON.stringify({
          "postalCode": `${form.zipCode}`,
          "roadAddress": `${form.mainAddress}`,
          "detailAddress": `${form.detailAddress}`
        })
      });
      const result = response.json();
      // address request end

      // userInfo update request init
      const userPhone = form.userPhone.replace("-","");
      const responseUser = await fetch(`${BASE_URL}/profile/basic-info/update`,{
        method: 'POST',
        headers: {
          "Authorization": `Bearer ${token}`,
          "Content-Type": 'application/json',
        },
        body: JSON.stringify({
          "userName": `${form.userName}`,
          "userPhone": `${userPhone}`,
        })
      });
      const resultUser = responseUser.json();
      // userInfo update request end

      // address result
      result.then((data) => {
        if(!data.success){
          throw new Error(result.message || '개인정보 수정에 실패했습니다.');
        }else{
          // userInfo result
          resultUser.then((data) => {
            // console.log(data);
            if(data.success){
              alert("정보가 성공적으로 변경되었습니다.");
              setToday(new Date().toLocaleString());
              return;
            }else{
              throw new Error(result.message || '개인정보 수정에 실패했습니다.');
            };
          });
          // userInfo result end
        }
      });
      // address result end

      // confirm true end
    }else{
      alert('사용자의 요청으로 취소됐습니다.');
      return;
    };
  }; // onSave func end


  // etc...
  if(loading){
    return <div>불러오는 중…</div>;
  };
  if(err){
    return <div style={{ color: 'crimson' }}>에러: {err}</div>;
  };
  if(!form){
    return null;
  };
  // etc... end
  
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

          {/* 주소: 라벨 오른쪽 한 줄에 3분할 (내가 준 CSS의 .addr-group 구조) */}
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
                <button onClick={openPostModal}>
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
          <span className='registDate'>
            *기준일 | 
            <span>{today}</span>
          </span>
          <button className="btn" onClick={onSave} title="저장 API">
            저장
          </button>
        </div>

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
                  width: "85%",
                  height: "100%",
                  maxHeight: "75%",
                  borderRadius: "10px",
                  margin: "auto",
                  padding: "20px",
                  maxWidth: "768px",
                  boxSizing: "border-box",
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

                <div id="arrSvg">
                  <FaArrowRight/>
                </div>

                <div className={ProfileModalCss.elModalList}>
                  <h6 className={ProfileModalCss.afterAreaTit}>
                    <input 
                      type="file" 
                      name="changeImg" 
                      id="changeImg" 
                      accept=".jpg, .png"
                      onChange={importImg}
                      placeholder='jpg, png만 허용'
                    />
                    <label 
                      className={ProfileModalCss.fileLabel}
                      htmlFor="changeImg"
                    >
                      파일선택
                    </label>
                    <span className={ProfileModalCss.fileName}>
                      {
                        changeProfileImg 
                          ? `${changeProfileImg}`
                          : '선택된 파일이 없습니다.'
                      }
                    </span>
                  </h6>
                  <div className={ProfileModalCss.afterProfile}>
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
                <button onClick={requestChangeProfileImg}>
                  변경하기
                </button>
              </div>
            </div>
          )}
        </Modal>
        {/* profile img uploadFrm modal end */}


        {/* postcode modal init */}
        <Modal 
          isOpen={!!postcodeModalOpen}
          onRequestClose={closePostModal}
          contentLabel='우편번호 검색'
          style={{
              overlay:{
                  zIndex: 9999,
                  backgroundColor: "rgba(0, 0, 0, .25)",
              },
              content:{
                  width: "85%",
                  maxWidth: "768px",
                  height: "100%",
                  maxHeight: "75%",
                  boxSizing: "border-box",
                  margin: "auto",
                  borderRadius: "10px",
                  padding: "20px",
              },
          }}
        > {/* modal header */}
          <DaumPostCode 
            style={postcodeStyle}
            onComplete={handleAddress}
          />
        </Modal>
        {/* postcode modal end */}
      </div>
    </div>
  );
}
