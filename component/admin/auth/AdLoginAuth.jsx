
async function AdLoginAuth(adminId, password) {
    const apiUrl = 'https://bluecrab.chickenkiller.com/BlueCrab-1.0.0/api/admin/login';

    console.log(adminId, password);

    try {
        const res = await fetch(apiUrl, {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json',
            'Accept': 'application/json'
        },
            body: JSON.stringify({ adminId, password })
        });

        // 서버가 항상 JSON을 반환한다고 가정
        const data = await res.json();

        if (res.ok) {
            console.log('✅ Admin login success');
        } else {
            console.warn('❌ Admin login failed', res.status, data?.message ?? data);
        }

        return { ok: res.ok, status: res.status, data };
    } catch (err) {
        console.error('🚨 Network error', err);
        throw err;
    }
};
export default AdLoginAuth;