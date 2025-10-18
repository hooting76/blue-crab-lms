# 시설물 예약 시스템 - 플로우 요약

## 🎯 당신의 예상 vs 실제 구현

### ✅ 당신의 예상 플로우 (기본적으로 정확!)
```
시설물 신청 컴포넌트 이동 
  → 시설물 조회 API 호출 
  → DB에서 사용가능 시설 확인 후 전송 
  → 예약할 시설물 클릭 
  → 시설물 정보와 예약 현황 전달
  → 사유와 장비 담아서 예약 생성 호출 
  → 정책 확인, 중복 확인 
  → DB에서 예약 생성
```

### ⭐ 실제 완전한 플로우 (추가 단계 포함)

```
📱 Frontend                          🖥️ Backend                    💾 Database
─────────────────────────────────────────────────────────────────────────────

1️⃣ 페이지 로드
   /Facility/FacilityRequest
          │
          ▼
   POST /api/facilities ────────▶ getAllFacilities() ──────▶ SELECT * FROM 
          │                      FacilityService             FACILITY_TBL
          │                                                  WHERE IS_ACTIVE=1
          │◀────────────────── [시설물 목록] ◀───────────────
          │
          ▼
   시설물 드롭다운 표시
   • 대강의실 A (승인 필요 ⏳)
   • 체육관 (즉시 승인 ✅)
   

2️⃣ 시설물 선택 + 날짜/시간 선택
   시설: 대강의실 A
   날짜: 2025-10-15
   시간: 14:00 ~ 16:00
          │
          ▼
   POST /api/facilities/1/availability ──▶ checkAvailability() ─▶ SELECT 차단기간
   ?startTime=...&endTime=...            FacilityService          SELECT 충돌예약
          │
          │◀─────────── { isAvailable: false, conflicts: [...] }
          ▼
   ❌ 해당 시간 예약 불가 표시
   (충돌하는 예약 시간 표시)
          │
          ▼
   사용자가 다른 시간 선택: 16:00 ~ 18:00
          │
          ▼
   다시 가용성 체크 ────────────────────▶ checkAvailability()
          │
          │◀─────────────── { isAvailable: true }
          ▼
   ✅ 예약 가능! 진행 버튼 활성화


3️⃣ 사유/장비 입력 후 [신청하기] 클릭
   {
     facilityIdx: 1,
     startTime: "2025-10-15T16:00",
     endTime: "2025-10-15T18:00",
     purpose: "학술 세미나",
     partySize: 50,
     requestedEquipment: "빔프로젝터"
   }
          │
          ▼
   POST /api/reservations ──────────────▶ createReservation()
   + JWT Token                            │
                                          ▼
                                   ① validateRequest()
                                   (시간 검증, 기간 검증)
                                          │
                                          ▼
                                   ② 정책 조회 ──────────▶ SELECT * FROM
                                   { requiresApproval: true }  FACILITY_POLICY_TBL
                                          │
                           ┌──────────────┴──────────────┐
                           ▼                             ▼
                    [승인 필요 시설]              [즉시 승인 시설]
                           │                             │
                    ③-A 락 없이 조회               ③-B 비관적 락 획득
                    차단만 체크 ────────▶          완전한 가용성 체크 ─▶
                    (충돌 체크 생략)                (차단+충돌 체크)
                           │                             │
                    상태: PENDING                  상태: APPROVED
                           │                             │
                           └──────────────┬──────────────┘
                                          ▼
                                   ④ DB 저장 ────────────▶ INSERT INTO
                                                           RESERVATION_TBL
                                          │
                                          ▼
                                   ⑤ 로그 기록 ──────────▶ INSERT INTO
                                                           RESERVATION_LOG
          │
          │◀──────────── { status: "PENDING", ... }
          ▼
   알림: "예약 신청 완료! 관리자 승인 대기 중"
   페이지 이동: /Facility/MyFacilityRequests
```

---

## 📊 핵심 차이점

### 당신의 예상에 없었던 중요한 단계:

1. **실시간 가용성 체크** (Step 2)
   - 사용자가 시간 선택할 때마다 실시간으로 확인
   - 충돌하는 예약이 있으면 즉시 알림

2. **정책 기반 처리 분기** (Step 3-②)
   - 승인 필요: 락 없이 빠르게 PENDING 저장
   - 즉시 승인: 락으로 중복 방지

3. **차단 기간 체크**
   - 관리자가 설정한 예약 불가 기간 확인

---

## 🔍 현재 상태 체크

### ✅ 백엔드: 완벽하게 구현됨
- 모든 API 엔드포인트 존재
- 동시성 제어 (락 전략)
- 정책 기반 승인 처리
- 가용성 체크
- 차단 기간 관리

### ❌ 프론트엔드: API 연동 필요
```jsx
// 현재 FacilityRequest.jsx
<select id="Facility">
  <option value="Fac01">시설물01</option>  // ❌ 하드코딩
</select>

const handleSubmit = () => {
  alert("신청 완료");  // ❌ 실제 API 호출 없음
};
```

### ✅ 필요한 작업:
1. 시설물 목록 API 연동
2. 가용성 체크 API 연동
3. 예약 생성 API 연동
4. 예약 현황 표시
5. 에러 처리

---

## 🎯 결론

**당신의 예상 플로우는 정확합니다!** ✅

다만 프로덕션 레벨에서는:
- **실시간 가용성 체크** 추가
- **정책 기반 처리** 고려
- **프론트엔드 API 연동** 필요

백엔드는 완벽하게 준비되어 있으니, 프론트엔드 연동만 하면 됩니다! 🚀
