# 주소 업데이트 API

> 📅 2025-10-23

## API 스펙

```
POST /api/profile/address/update
```

### Headers
```
Authorization: Bearer {JWT_TOKEN}
Content-Type: application/json
```

### Request Body
```json
{
  "postalCode": "05852",
  "roadAddress": "서울 송파구 위례광장로 120",
  "detailAddress": "장지동, 위례중앙푸르지오1단지"
}
```

| 필드 | 타입 | 필수 | 제약 |
|------|------|------|------|
| postalCode | String | ✅ | 5자리 숫자 |
| roadAddress | String | ✅ | 최대 200자 |
| detailAddress | String | ❌ | 최대 100자 |

### Response (200 OK)
```json
{
  "success": true,
  "message": "주소가 성공적으로 업데이트되었습니다.",
  "data": {
    "zipCode": "05852",
    "mainAddress": "서울 송파구 위례광장로 120",
    "detailAddress": "장지동, 위례중앙푸르지오1단지"
  },
  "timestamp": "2025-10-23T15:30:45.123Z"
}
```

### Error Response

| Status | Message |
|--------|---------|
| 400 | "우편번호는 5자리 숫자여야 합니다" |
| 401 | "인증 토큰이 필요합니다." |
| 404 | "사용자를 찾을 수 없습니다" |

---

## 참고사항

- 우편번호 0 패딩 자동 처리 (05852 → DB 저장 5852 → 조회 시 "05852")
- `react-daum-postcode` 패키지 이미 설치됨
- 기존 `POST /api/profile/me`로 조회 시 주소 포함됨
