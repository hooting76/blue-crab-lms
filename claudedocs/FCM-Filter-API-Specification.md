# FCM 알림 필터링 API 명세서

## 개요
관리자가 학생들에게 FCM 푸시 알림을 전송할 때 사용하는 필터링 API입니다.
학부, 학과, 입학년도, 학년, 강의 등 다양한 조건으로 학생을 필터링하여 알림을 전송할 수 있습니다.

---

## 1. 알림 대상 미리보기 API

### Endpoint
```
POST /api/admin/notifications/preview
```

### 설명
알림을 전송하기 전에 필터 조건에 해당하는 학생 목록을 미리 확인합니다.

### Request Headers
```
Authorization: Bearer {access_token}
Content-Type: application/json
```

### Request Body
```json
{
  "filterType": "FACULTY",  // 필터 타입 (필수)
  "userStudent": 0,  // 0=학생, 1=교수 (ROLE 타입일 때 필수)
  "facultyCodes": ["01", "02"],  // 학부 코드 리스트 (FACULTY 타입일 때 필수)
  "departments": [  // 학과 조건 리스트 (DEPARTMENT 타입일 때 필수)
    {
      "facultyCode": "01",
      "deptCode": "01"
    },
    {
      "facultyCode": "01",
      "deptCode": "02"
    }
  ],
  "admissionYears": [2024, 2025],  // 입학년도 리스트 (ADMISSION_YEAR 타입일 때 필수)
  "gradeYears": [1, 2, 3, 4],  // 학년 리스트 (GRADE 타입일 때 필수)
  "lectureIds": [1, 2, 3],  // 강의 ID 리스트 (COURSE 타입일 때 필수)
  "userCodes": ["202510101001", "202510201002"]  // 특정 학생 학번 리스트 (CUSTOM 타입일 때 필수)
}
```

### FilterType 옵션

| FilterType | 설명 | 필수 파라미터 |
|------------|------|---------------|
| `ALL` | 전체 사용자 | 없음 |
| `ROLE` | 역할별 (학생/교수) | `userStudent` |
| `FACULTY` | 학부별 | `facultyCodes` |
| `DEPARTMENT` | 학과별 | `departments` |
| `ADMISSION_YEAR` | 입학년도별 | `admissionYears` |
| `GRADE` | 학년별 | `gradeYears` |
| `COURSE` | 강의별 (수강생) | `lectureIds` |
| `CUSTOM` | 직접 선택 | `userCodes` |

### Response
```json
{
  "targetCount": 15
}
```

### Response Fields

| Field | Type | Description |
|-------|------|-------------|
| `targetCount` | number | 필터링된 전체 대상자 수 |

---

## 2. 알림 전송 API

### Endpoint
```
POST /api/admin/notifications/send
```

### 설명
필터링된 학생들에게 FCM 푸시 알림을 전송합니다.

### Request Headers
```
Authorization: Bearer {access_token}
Content-Type: application/json
```

### Request Body
```json
{
  "title": "공지사항",  // 알림 제목 (필수)
  "body": "내일 전체 조회가 있습니다.",  // 알림 내용 (필수)
  "data": {  // 추가 데이터 (선택, FCM data payload)
    "type": "announcement",
    "link": "/notices/123"
  },
  "filterCriteria": {  // 필터 조건 (필수)
    "filterType": "FACULTY",
    "facultyCodes": ["01", "02"]
  }
}
```

### Response
```json
{
  "notificationId": 123,
  "targetCount": 15,
  "successCount": 14,
  "failureCount": 1
}
```

### Response Fields

| Field | Type | Description |
|-------|------|-------------|
| `notificationId` | number | 알림 ID |
| `targetCount` | number | 대상자 수 |
| `successCount` | number | 전송 성공 수 |
| `failureCount` | number | 전송 실패 수 (FCM 토큰 없음 등) |

---

## 3. 알림 전송 이력 조회 API

### Endpoint
```
POST /api/admin/notifications/history
```

### 설명
과거 전송한 알림 이력을 조회합니다.

### Request Headers
```
Authorization: Bearer {access_token}
Content-Type: application/json
```

### Request Body
```json
{
  "page": 0,  // 페이지 번호 (0부터 시작)
  "size": 20  // 페이지 크기
}
```

### Response
```json
{
  "content": [
    {
      "id": 123,
      "title": "공지사항",
      "body": "내일 전체 조회가 있습니다.",
      "filterType": "FACULTY",
      "targetCount": 15,
      "successCount": 14,
      "failureCount": 1,
      "sentAt": "2025-10-28T10:30:05",
      "createdBy": "admin@bluecrab.com"
    }
  ],
  "totalElements": 50,
  "totalPages": 3,
  "number": 0,
  "size": 20
}
```

---

> ℹ️ **참고**  
> 현재 이력 API는 페이지 기반 조회만 지원합니다. 날짜 범위나 기타 조건 필터링은 향후 확장 항목으로 계획되어 있으며, 실제 구현 전에 문서가 먼저 갱신되는 일을 방지하기 위해 본 명세에서는 제외합니다.

## 4. 학부/학과 코드 참조

### 학부 코드 (FACULTY_CODE)

| 코드 | 학부명 | 정원 |
|------|--------|------|
| 01 | 해양학부 | 410명 |
| 02 | 보건학부 | 340명 |
| 03 | 자연과학부 | 120명 |
| 04 | 인문학부 | 320명 |
| 05 | 공학부 | 320명 |

### 학과 코드 (DEPT_CODE)

#### 해양학부 (01)
| 코드 | 학과명 | 정원 |
|------|--------|------|
| 01 | 항해학과 | 50명 |
| 02 | 해양경찰 | 70명 |
| 03 | 해군사관 | 120명 |
| 04 | 도선학과 | 50명 |
| 05 | 해양수산학 | 40명 |
| 06 | 조선학과 | 80명 |

#### 보건학부 (02)
| 코드 | 학과명 | 정원 |
|------|--------|------|
| 01 | 간호학 | 120명 |
| 02 | 치위생 | 80명 |
| 03 | 약학과 | 80명 |
| 04 | 보건정책학 | 60명 |

#### 자연과학부 (03)
| 코드 | 학과명 | 정원 |
|------|--------|------|
| 01 | 물리학 | 50명 |
| 02 | 수학 | 30명 |
| 03 | 분자화학 | 40명 |

#### 인문학부 (04)
| 코드 | 학과명 | 정원 |
|------|--------|------|
| 01 | 철학 | 20명 |
| 02 | 국어국문 | 50명 |
| 03 | 역사학 | 40명 |
| 04 | 경영 | 70명 |
| 05 | 경제 | 50명 |
| 06 | 정치외교 | 30명 |
| 07 | 영어영문 | 60명 |

#### 공학부 (05)
| 코드 | 학과명 | 정원 |
|------|--------|------|
| 01 | 컴퓨터공학 | 100명 |
| 02 | 기계공학 | 60명 |
| 03 | 전자공학 | 35명 |
| 04 | ICT융합 | 125명 |

---

## 5. 사용 예시

### 예시 1: 해양학부 전체 학생에게 알림 전송

1. **미리보기 요청**
```json
POST /api/admin/notifications/preview
{
  "filterType": "FACULTY",
  "facultyCodes": ["01"]
}
```

2. **알림 전송 요청**
```json
POST /api/admin/notifications/send
{
  "title": "해양학부 공지",
  "body": "해양학부 학생회 총회가 있습니다",
  "filterCriteria": {
    "filterType": "FACULTY",
    "facultyCodes": ["01"]
  }
}
```

### 예시 2: 특정 학과(해양학부 항해학과, 해양경찰) 학생에게 알림

```json
POST /api/admin/notifications/preview
{
  "filterType": "DEPARTMENT",
  "departments": [
    {
      "facultyCode": "01",
      "deptCode": "01"
    },
    {
      "facultyCode": "01",
      "deptCode": "02"
    }
  ]
}
```

### 예시 3: 2025년 입학생 전체에게 알림

```json
POST /api/admin/notifications/preview
{
  "filterType": "ADMISSION_YEAR",
  "admissionYears": [2025]
}
```

### 예시 4: 1학년 전체에게 알림

```json
POST /api/admin/notifications/preview
{
  "filterType": "GRADE",
  "gradeYears": [1]
}
```

### 예시 5: 특정 강의 수강생에게 알림

```json
POST /api/admin/notifications/preview
{
  "filterType": "COURSE",
  "lectureIds": [123, 456]
}
```

### 예시 6: 특정 학생들에게 직접 알림

```json
POST /api/admin/notifications/preview
{
  "filterType": "CUSTOM",
  "userCodes": ["202510101001", "202510201002", "202510301003"]
}
```

### 예시 7: 학생과 교수 구분하여 알림

**학생 전체에게 알림**
```json
POST /api/admin/notifications/send
{
  "title": "학생 공지",
  "body": "내일 개강총회가 있습니다",
  "filterCriteria": {
    "filterType": "ROLE",
    "userStudent": 0
  }
}
```

**교수 전체에게 알림**
```json
POST /api/admin/notifications/send
{
  "title": "교수 공지",
  "body": "내일 교수회의가 있습니다",
  "filterCriteria": {
    "filterType": "ROLE",
    "userStudent": 1
  }
}
```

---

## 6. 에러 코드

| HTTP Status | Error Code | Message | 설명 |
|-------------|------------|---------|------|
| 400 | INVALID_FILTER_TYPE | 유효하지 않은 필터 타입입니다 | filterType이 잘못됨 |
| 400 | MISSING_FILTER_PARAM | 필수 필터 파라미터가 누락되었습니다 | filterType에 필요한 파라미터 누락 |
| 400 | INVALID_FACULTY_CODE | 유효하지 않은 학부 코드입니다 | 존재하지 않는 학부 코드 |
| 400 | INVALID_DEPT_CODE | 유효하지 않은 학과 코드입니다 | 존재하지 않는 학과 코드 |
| 400 | EMPTY_TARGET_LIST | 알림 대상이 없습니다 | 필터 조건에 해당하는 학생 없음 |
| 401 | UNAUTHORIZED | 인증이 필요합니다 | 토큰 누락 또는 만료 |
| 403 | FORBIDDEN | 권한이 없습니다 | 관리자 권한 필요 |
| 500 | FCM_SEND_ERROR | FCM 전송 중 오류가 발생했습니다 | FCM 서버 오류 |

---

## 7. 주의사항

### 데이터 정합성
- **USER_STUDENT 플래그**: `0 = 학생`, `1 = 교수`
- 학부/학과 정보는 `SERIAL_CODE_TABLE`에서 관리됩니다
- 학생이 FCM 토큰을 등록하지 않은 경우 알림이 전송되지 않습니다

### 필터 조합
- 여러 필터를 동시에 사용할 수 없습니다
- 하나의 `filterType`만 선택하여 사용해야 합니다

### 성능 고려사항
- 대량 알림 전송 시 시간이 소요될 수 있습니다
- 1000명 이상 전송 시 예약 전송을 권장합니다

### FCM 토큰 관리
- 학생이 앱에 로그인 시 FCM 토큰이 자동으로 등록됩니다
- 앱 재설치 또는 로그아웃 시 토큰이 갱신될 수 있습니다
- `failedCount`는 주로 FCM 토큰이 없는 학생 수입니다

---

## 8. 테스트 데이터

현재 시스템에 등록된 학생 수:
- **학부 01 (해양학부)**: 11명
- **학부 02 (보건학부)**: 6명
- **학부 03 (자연과학부)**: 5명
- **학부 04 (인문학부)**: 6명
- **학부 05 (공학부)**: 6명

**총 34명**의 학생이 등록되어 있으며, FCM 토큰이 등록된 학생에게만 실제 푸시 알림이 전송됩니다.

---

## 10. 프론트엔드 구현 가이드

### 페이지 구조

관리자 대시보드의 **통계자료 > 푸시알림** 메뉴에서 구현

#### 필요한 컴포넌트

1. **NotificationSend.jsx** - 알림 전송 메인 페이지
2. **FilterSelector.jsx** - 필터 조건 선택 컴포넌트
3. **TargetPreview.jsx** - 대상자 미리보기 컴포넌트
4. **NotificationHistory.jsx** - 전송 이력 조회 컴포넌트

### 상태 관리 예시

```javascript
const [filterType, setFilterType] = useState('ALL');
const [filterParams, setFilterParams] = useState({});
const [targetCount, setTargetCount] = useState(0);
const [title, setTitle] = useState('');
const [body, setBody] = useState('');
```

### API 호출 예시

#### 1. 대상자 미리보기
```javascript
const previewTargets = async () => {
  try {
    const response = await fetch('/api/admin/notifications/preview', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${accessToken}`
      },
      body: JSON.stringify({
        filterType: filterType,
        ...filterParams
      })
    });
    
    const data = await response.json();
    setTargetCount(data.targetCount);
  } catch (error) {
    console.error('미리보기 오류:', error);
  }
};
```

#### 2. 알림 전송
```javascript
const sendNotification = async () => {
  try {
    const response = await fetch('/api/admin/notifications/send', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${accessToken}`
      },
      body: JSON.stringify({
        title: title,
        body: body,
        data: {
          type: 'announcement'
        },
        filterCriteria: {
          filterType: filterType,
          ...filterParams
        }
      })
    });
    
    const result = await response.json();
    alert(`전송 완료: 성공 ${result.successCount}건, 실패 ${result.failureCount}건`);
  } catch (error) {
    console.error('전송 오류:', error);
  }
};
```

#### 3. 전송 이력 조회
```javascript
const fetchHistory = async (page = 0) => {
  try {
    const response = await fetch('/api/admin/notifications/history', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${accessToken}`
      },
      body: JSON.stringify({
        page: page,
        size: 20
      })
    });
    
    const data = await response.json();
    setHistoryList(data.content);
    setTotalPages(data.totalPages);
  } catch (error) {
    console.error('이력 조회 오류:', error);
  }
};
```

### UI/UX 권장사항

#### 필터 선택 UI
```jsx
<select value={filterType} onChange={(e) => setFilterType(e.target.value)}>
  <option value="ALL">전체 사용자</option>
  <option value="ROLE">역할별 (학생/교수)</option>
  <option value="FACULTY">학부별</option>
  <option value="DEPARTMENT">학과별</option>
  <option value="ADMISSION_YEAR">입학년도별</option>
  <option value="GRADE">학년별</option>
  <option value="COURSE">강의별</option>
  <option value="CUSTOM">직접 선택</option>
</select>
```

#### 조건부 입력 필드
```jsx
{filterType === 'FACULTY' && (
  <div>
    <label>학부 선택</label>
    <CheckboxGroup
      options={[
        { value: '01', label: '해양학부' },
        { value: '02', label: '보건학부' },
        { value: '03', label: '자연과학부' },
        { value: '04', label: '인문학부' },
        { value: '05', label: '공학부' }
      ]}
      value={filterParams.facultyCodes || []}
      onChange={(selected) => setFilterParams({...filterParams, facultyCodes: selected})}
    />
  </div>
)}

{filterType === 'ROLE' && (
  <div>
    <label>대상 선택</label>
    <RadioGroup
      value={filterParams.userStudent}
      onChange={(value) => setFilterParams({...filterParams, userStudent: value})}
    >
      <Radio value={0}>학생</Radio>
      <Radio value={1}>교수</Radio>
    </RadioGroup>
  </div>
)}
```

#### 미리보기 및 전송 버튼
```jsx
<div className="action-buttons">
  <button 
    onClick={previewTargets}
    disabled={!isFilterValid()}
  >
    대상자 확인 ({targetCount}명)
  </button>
  
  <button 
    onClick={sendNotification}
    disabled={!title || !body || targetCount === 0}
    className="primary"
  >
    알림 전송
  </button>
</div>
```

### 유효성 검사

```javascript
const isFilterValid = () => {
  switch(filterType) {
    case 'ALL':
      return true;
    case 'ROLE':
      return filterParams.userStudent !== undefined;
    case 'FACULTY':
      return filterParams.facultyCodes?.length > 0;
    case 'DEPARTMENT':
      return filterParams.departments?.length > 0;
    case 'ADMISSION_YEAR':
      return filterParams.admissionYears?.length > 0;
    case 'GRADE':
      return filterParams.gradeYears?.length > 0;
    case 'COURSE':
      return filterParams.lectureIds?.length > 0;
    case 'CUSTOM':
      return filterParams.userCodes?.length > 0;
    default:
      return false;
  }
};
```

### 에러 처리

```javascript
const handleApiError = (error, response) => {
  if (response?.status === 401) {
    alert('로그인이 필요합니다');
    navigate('/admin/login');
  } else if (response?.status === 403) {
    alert('권한이 없습니다');
  } else if (response?.status === 400) {
    alert('입력값을 확인해주세요');
  } else {
    alert('오류가 발생했습니다');
  }
};
```

---

## 11. 버전 정보

- **API Version**: 1.0.0
- **Last Updated**: 2025-10-28
- **Base URL**: `https://bluecrab.chickenkiller.com/BlueCrab-1.0.0`
