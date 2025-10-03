# 첨부파일 삭제 기능 API 문서

## 📋 개요

BlueCrab LMS의 게시글 첨부파일 삭제 기능을 제공하는 API 문서입니다.  
**논리적 삭제**와 **물리적 삭제** 두 가지 방식을 지원합니다.

---

## 🎯 구현된 삭제 API 목록

### 1. **단일 첨부파일 삭제**
**엔드포인트**: `POST /api/board-attachments/delete/{attachmentIdx}`

**헤더**:
```
Authorization: Bearer {JWT_TOKEN}
Content-Type: application/json
```

**경로 파라미터**:
- `attachmentIdx`: 삭제할 첨부파일 IDX (Integer)adf

**응답 예시**:
```json
{
  "success": true,
  "message": "첨부파일이 성공적으로 삭제되었습니다.",
  "attachmentIdx": 123
}
```

### 2. **다중 첨부파일 삭제**
**엔드포인트**: `POST /api/board-attachments/delete-multiple`

**헤더**:
```
Authorization: Bearer {JWT_TOKEN}
Content-Type: application/json
```

**요청 Body**:
```json
{
  "attachmentIds": [123, 124, 125]
}
```

**응답 예시**:
```json
{
  "success": true,
  "message": "다중 첨부파일 삭제가 완료되었습니다.",
  "successCount": 2,
  "failureCount": 1,
  "successIds": [123, 124],
  "failureIds": [125]
}
```

### 3. **게시글별 모든 첨부파일 삭제**
**엔드포인트**: `POST /api/board-attachments/delete-all/{boardIdx}`

**헤더**:
```
Authorization: Bearer {JWT_TOKEN}
Content-Type: application/json
```

**경로 파라미터**:
- `boardIdx`: 게시글 IDX (Integer)

**응답 예시**:
```json
{
  "success": true,
  "message": "게시글의 모든 첨부파일이 삭제되었습니다.",
  "boardIdx": 456,
  "deletedCount": 3,
  "message": "3개의 첨부파일이 삭제되었습니다."
}
```

---

## 🔧 삭제 방식 설명

### 📌 **논리적 삭제 (기본 방식)**
- **동작**: `isActive` 필드를 `0`으로 설정
- **장점**: 데이터 복구 가능, 안전성 높음
- **단점**: 저장공간 계속 사용
- **사용 API**: 위의 모든 삭제 API

### 📌 **물리적 삭제 (고급 기능)**
- **동작**: MinIO와 DB에서 완전 삭제
- **장점**: 저장공간 절약
- **단점**: 복구 불가능
- **사용**: 관리자 기능 또는 배치 작업

---

## 🔒 보안 및 권한

### **JWT 토큰 검증**
- 모든 삭제 API는 **JWT 토큰 필수**
- `Authorization: Bearer {token}` 헤더 포함 필요
- 토큰 검증 실패 시 **401 Unauthorized** 응답

### **권한 확인 (향후 개선 사항)**
- 현재: JWT 토큰 검증만 수행
- 향후: 작성자 본인 또는 관리자만 삭제 가능하도록 개선 예정

---

## 📊 에러 응답 예시

### **인증 실패 (401)**
```json
{
  "success": false,
  "message": "인증이 필요합니다."
}
```

### **파일 없음 (404)**
```json
{
  "success": false,
  "message": "첨부파일을 찾을 수 없거나 이미 삭제되었습니다."
}
```

### **서버 오류 (500)**
```json
{
  "success": false,
  "message": "첨부파일 삭제 중 오류가 발생했습니다: {상세메시지}"
}
```

---

## 🧪 프론트엔드 연동 예시

### **JavaScript/Axios 예시**

#### 단일 파일 삭제
```javascript
const deleteAttachment = async (attachmentIdx, token) => {
  try {
    const response = await axios.post(
      `/api/board-attachments/delete/${attachmentIdx}`,
      {},
      {
        headers: {
          'Authorization': `Bearer ${token}`,
          'Content-Type': 'application/json'
        }
      }
    );
    
    if (response.data.success) {
      console.log('삭제 성공:', response.data.message);
      // UI 업데이트 로직
    }
  } catch (error) {
    console.error('삭제 실패:', error.response.data.message);
  }
};
```

#### 다중 파일 삭제
```javascript
const deleteMultipleAttachments = async (attachmentIds, token) => {
  try {
    const response = await axios.post(
      '/api/board-attachments/delete-multiple',
      { attachmentIds },
      {
        headers: {
          'Authorization': `Bearer ${token}`,
          'Content-Type': 'application/json'
        }
      }
    );
    
    const { successCount, failureCount } = response.data;
    console.log(`삭제 완료: 성공 ${successCount}개, 실패 ${failureCount}개`);
    
  } catch (error) {
    console.error('다중 삭제 실패:', error.response.data.message);
  }
};
```

#### 게시글 모든 파일 삭제
```javascript
const deleteAllAttachments = async (boardIdx, token) => {
  if (confirm('게시글의 모든 첨부파일을 삭제하시겠습니까?')) {
    try {
      const response = await axios.post(
        `/api/board-attachments/delete-all/${boardIdx}`,
        {},
        {
          headers: {
            'Authorization': `Bearer ${token}`,
            'Content-Type': 'application/json'
          }
        }
      );
      
      console.log(`${response.data.deletedCount}개 파일 삭제 완료`);
      
    } catch (error) {
      console.error('전체 삭제 실패:', error.response.data.message);
    }
  }
};
```

---

## 🔄 관련 API와의 연동

### **업로드 → 삭제 플로우**
1. `POST /api/board-attachments/upload/{boardIdx}` - 파일 업로드
2. `POST /api/boards/link-attachments/{boardIdx}` - 게시글 연결
3. `POST /api/board-attachments/delete/{attachmentIdx}` - 파일 삭제

### **조회 → 삭제 플로우**
1. `POST /api/boards/detail/{boardIdx}` - 게시글 상세 조회 (첨부파일 포함)
2. `POST /api/board-attachments/delete/{attachmentIdx}` - 특정 파일 삭제

---

## 📈 성능 및 최적화

### **배치 삭제 작업**
- **만료된 파일 정리**: `findExpiredAttachments()` 메서드 활용
- **주기적 실행**: Spring Scheduler 활용 권장
- **물리적 삭제**: 저장공간 확보를 위한 정기 작업

### **데이터베이스 최적화**
- `boardIdx + isActive` 복합 인덱스 권장
- `expiryDate` 인덱스로 만료 파일 조회 성능 향상

---

## ⚠️ 주의사항

1. **논리적 삭제**: 기본적으로 파일이 완전히 삭제되지 않음
2. **MinIO 연결**: 물리적 삭제 시 MinIO 서버 연결 필요
3. **권한 확인**: 현재는 JWT 토큰만 확인 (향후 권한 로직 강화 예정)
4. **트랜잭션**: 다중 삭제 시 일부 실패 가능성 있음

---

## 🚀 향후 개선 계획

1. **권한 강화**: 작성자/관리자 권한 확인 추가
2. **복구 기능**: 삭제된 파일 복구 API 구현
3. **배치 작업**: 만료 파일 자동 정리 스케줄러 구현
4. **모니터링**: 삭제 작업 로깅 및 통계 기능 추가