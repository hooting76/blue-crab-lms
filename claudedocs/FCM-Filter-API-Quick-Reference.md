# FCM 알림 필터링 API - 빠른 참조

## 🚀 빠른 시작

### Base URL
```
https://bluecrab.chickenkiller.com/BlueCrab-1.0.0
```

---

## 📌 핵심 API 3개

### 1. 대상자 미리보기
```http
POST /api/admin/notifications/preview
```

**Request**
```json
{
  "filterType": "FACULTY",
  "facultyCodes": ["01"]
}
```

**Response**
```json
{
  "targetCount": 11
}
```

---

### 2. 알림 전송
```http
POST /api/admin/notifications/send
```

**Request**
```json
{
  "title": "공지사항",
  "body": "내일 전체 조회가 있습니다",
  "filterCriteria": {
    "filterType": "FACULTY",
    "facultyCodes": ["01"]
  }
}
```

**Response**
```json
{
  "notificationId": 123,
  "targetCount": 11,
  "successCount": 10,
  "failureCount": 1
}
```

---

### 3. 전송 이력
```http
POST /api/admin/notifications/history
```

**Request**
```json
{
  "page": 0,
  "size": 20
}
```

**Response**
```json
{
  "content": [...],
  "totalElements": 50,
  "totalPages": 3
}
```

---

## 🎯 FilterType 종류

| Type | 필수 파라미터 | 예시 |
|------|--------------|------|
| `ALL` | 없음 | 전체 사용자 |
| `ROLE` | `userStudent` | 학생(0) or 교수(1) |
| `FACULTY` | `facultyCodes` | ["01", "02"] |
| `DEPARTMENT` | `departments` | [{facultyCode:"01", deptCode:"01"}] |
| `ADMISSION_YEAR` | `admissionYears` | [2024, 2025] |
| `GRADE` | `gradeYears` | [1, 2, 3, 4] |
| `COURSE` | `lectureIds` | [123, 456] |
| `CUSTOM` | `userCodes` | ["202510101001"] |

---

## 🏫 학부/학과 코드

### 학부
| 코드 | 이름 |
|------|------|
| 01 | 해양학부 |
| 02 | 보건학부 |
| 03 | 자연과학부 |
| 04 | 인문학부 |
| 05 | 공학부 |

### 주요 학과
**해양학부(01)**: 01-항해학과, 02-해양경찰, 03-해군사관, 06-조선학과  
**보건학부(02)**: 01-간호학, 02-치위생, 03-약학과  
**공학부(05)**: 01-컴퓨터공학, 02-기계공학, 04-ICT융합

[전체 학과 코드 보기](./FCM-Filter-API-Specification.md#4-학부학과-코드-참조)

---

## 💡 빠른 예시

### 학부 전체
```json
{
  "filterType": "FACULTY",
  "facultyCodes": ["01"]
}
```

### 특정 학과
```json
{
  "filterType": "DEPARTMENT",
  "departments": [
    {"facultyCode": "01", "deptCode": "01"},
    {"facultyCode": "01", "deptCode": "02"}
  ]
}
```

### 학생만
```json
{
  "filterType": "ROLE",
  "userStudent": 0
}
```

### 1학년만
```json
{
  "filterType": "GRADE",
  "gradeYears": [1]
}
```

---

## ⚠️ 주의사항

- **USER_STUDENT**: 0=학생, 1=교수
- 전송 이력 API는 현재 `page`, `size`만 처리하며 날짜 필터는 미지원
- FCM 토큰 없는 사용자는 전송 실패
- `failureCount`는 토큰 없는 사용자 수
- Authorization 헤더 필수: `Bearer {token}`

---

## 📚 더 보기

- [상세 API 명세서](./FCM-Filter-API-Specification.md)
- [프론트엔드 구현 가이드](./FCM-Filter-Frontend-Guide.md)
