import React, { useEffect, useState } from "react";

// TODO: 실제 API 연동 시:
// GET  /api/users/me  → 초기 데이터 불러오기
// PUT  /api/users/me  → 저장
export default function ProfileEdit() {
  const [form, setForm] = useState({
    name: "",
    email: "",
    phone: "",
    dept: "",
    studentId: "",
    // 비밀번호 변경은 선택
    password: "",
    passwordConfirm: "",
  });

  // 예시: 로그인 정보에서 초기값 채우기
  useEffect(() => {
    try {
      const raw = localStorage.getItem("loggedInUser");
      if (raw) {
        const u = JSON.parse(raw);
        setForm((f) => ({
          ...f,
          name: u?.name || "",
          email: u?.email || "",
          phone: u?.phone || "",
          dept: u?.dept || "",
          studentId: u?.studentId || "",
        }));
      }
    } catch (_) {}
  }, []);

  const onChange = (e) => {
    const { name, value } = e.target;
    setForm((f) => ({ ...f, [name]: value }));
  };

  const onSubmit = (e) => {
    e.preventDefault();
    if (form.password && form.password !== form.passwordConfirm) {
      alert("비밀번호 확인이 일치하지 않습니다.");
      return;
    }
    // TODO: PUT /api/users/me 로 저장
    console.log("SAVE profile", form);
    alert("저장되었습니다. (API 연동 필요)");
  };

  return (
    <section style={{ padding: 16 }}>
      <h3 style={{ marginBottom: 12 }}>개인정보 수정</h3>

      <form onSubmit={onSubmit} className="profile-edit-form">
        <div className="row">
          <label>이름</label>
          <input name="name" value={form.name} onChange={onChange} required />
        </div>
        <div className="row">
          <label>이메일</label>
          <input name="email" type="email" value={form.email} onChange={onChange} required />
        </div>
        <div className="row">
          <label>전화번호</label>
          <input name="phone" value={form.phone} onChange={onChange} />
        </div>
        <div className="row">
          <label>학과</label>
          <input name="dept" value={form.dept} onChange={onChange} />
        </div>
        <div className="row">
          <label>학번</label>
          <input name="studentId" value={form.studentId} onChange={onChange} />
        </div>

        <hr style={{ margin: "16px 0" }} />

        <div className="row">
          <label>새 비밀번호 (선택)</label>
          <input name="password" type="password" value={form.password} onChange={onChange} />
        </div>
        <div className="row">
          <label>새 비밀번호 확인</label>
          <input name="passwordConfirm" type="password" value={form.passwordConfirm} onChange={onChange} />
        </div>

        <div style={{ marginTop: 16, display: "flex", gap: 8 }}>
          <button type="submit">저장</button>
          <button type="button" onClick={() => window.history.back?.()}>취소</button>
        </div>
      </form>
    </section>
  );
}
