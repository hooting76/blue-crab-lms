# FCM 알림 필터링 - 프론트엔드 구현 가이드

> ℹ️ **참고**  
> 이 문서는 FCM 필터 API를 연동할 때 참고할 수 있는 예시 UI/상태 구조를 설명합니다. 현재 코드베이스에는 아래 컴포넌트가 존재하지 않으며, 실제 구현 시 요구사항과 디자인에 맞춰 조정해야 합니다.

## 📁 파일 구조

```
component/admin/
  ├── notifications/
  │   ├── NotificationSend.jsx        # 메인 페이지
  │   ├── FilterSelector.jsx          # 필터 선택
  │   ├── TargetPreview.jsx           # 대상자 미리보기
  │   └── NotificationHistory.jsx     # 전송 이력
  └── common/
      └── AdNav.jsx                    # 네비게이션 (수정)
```

---

## 🎨 NotificationSend.jsx

```jsx
import { useState } from 'react';

export default function NotificationSend() {
  const [filterType, setFilterType] = useState('ALL');
  const [filterParams, setFilterParams] = useState({});
  const [targetCount, setTargetCount] = useState(0);
  const [title, setTitle] = useState('');
  const [body, setBody] = useState('');
  const [loading, setLoading] = useState(false);

  // 대상자 미리보기
  const previewTargets = async () => {
    setLoading(true);
    try {
      const response = await fetch('/api/admin/notifications/preview', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          'Authorization': `Bearer ${localStorage.getItem('accessToken')}`
        },
        body: JSON.stringify({
          filterType,
          ...filterParams
        })
      });
      
      const data = await response.json();
      setTargetCount(data.targetCount);
    } catch (error) {
      alert('미리보기 오류');
    } finally {
      setLoading(false);
    }
  };

  // 알림 전송
  const sendNotification = async () => {
    if (!title || !body) {
      alert('제목과 내용을 입력하세요');
      return;
    }

    if (targetCount === 0) {
      alert('대상자가 없습니다');
      return;
    }

    if (!confirm(`${targetCount}명에게 알림을 전송하시겠습니까?`)) {
      return;
    }

    setLoading(true);
    try {
      const response = await fetch('/api/admin/notifications/send', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          'Authorization': `Bearer ${localStorage.getItem('accessToken')}`
        },
        body: JSON.stringify({
          title,
          body,
          filterCriteria: {
            filterType,
            ...filterParams
          }
        })
      });
      
      const result = await response.json();
      alert(`전송 완료\n성공: ${result.successCount}명\n실패: ${result.failureCount}명`);
      
      // 초기화
      setTitle('');
      setBody('');
      setTargetCount(0);
    } catch (error) {
      alert('전송 오류');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div>
      <h2>푸시 알림 전송</h2>
      
      {/* 필터 선택 */}
      <FilterSelector 
        filterType={filterType}
        setFilterType={setFilterType}
        filterParams={filterParams}
        setFilterParams={setFilterParams}
      />

      {/* 미리보기 */}
      <button onClick={previewTargets} disabled={loading}>
        대상자 확인 ({targetCount}명)
      </button>

      {/* 알림 내용 */}
      <div>
        <input
          type="text"
          placeholder="제목"
          value={title}
          onChange={(e) => setTitle(e.target.value)}
        />
        <textarea
          placeholder="내용"
          value={body}
          onChange={(e) => setBody(e.target.value)}
        />
      </div>

      {/* 전송 */}
      <button 
        onClick={sendNotification}
        disabled={loading || targetCount === 0}
      >
        알림 전송
      </button>
    </div>
  );
}
```

---

## 🎛️ FilterSelector.jsx

```jsx
export default function FilterSelector({ 
  filterType, 
  setFilterType, 
  filterParams, 
  setFilterParams 
}) {
  return (
    <div>
      <select value={filterType} onChange={(e) => {
        setFilterType(e.target.value);
        setFilterParams({});
      }}>
        <option value="ALL">전체 사용자</option>
        <option value="ROLE">역할별 (학생/교수)</option>
        <option value="FACULTY">학부별</option>
        <option value="DEPARTMENT">학과별</option>
        <option value="ADMISSION_YEAR">입학년도별</option>
        <option value="GRADE">학년별</option>
        <option value="COURSE">강의별</option>
        <option value="CUSTOM">직접 선택</option>
      </select>

      {/* 역할별 */}
      {filterType === 'ROLE' && (
        <div>
          <label>
            <input 
              type="radio" 
              name="role"
              value={0}
              onChange={() => setFilterParams({ userStudent: 0 })}
            />
            학생
          </label>
          <label>
            <input 
              type="radio" 
              name="role"
              value={1}
              onChange={() => setFilterParams({ userStudent: 1 })}
            />
            교수
          </label>
        </div>
      )}

      {/* 학부별 */}
      {filterType === 'FACULTY' && (
        <div>
          {['01-해양', '02-보건', '03-자연', '04-인문', '05-공학'].map(item => {
            const [code, name] = item.split('-');
            return (
              <label key={code}>
                <input
                  type="checkbox"
                  value={code}
                  onChange={(e) => {
                    const codes = filterParams.facultyCodes || [];
                    if (e.target.checked) {
                      setFilterParams({ facultyCodes: [...codes, code] });
                    } else {
                      setFilterParams({ 
                        facultyCodes: codes.filter(c => c !== code) 
                      });
                    }
                  }}
                />
                {name}학부
              </label>
            );
          })}
        </div>
      )}

      {/* 학년별 */}
      {filterType === 'GRADE' && (
        <div>
          {[1, 2, 3, 4].map(grade => (
            <label key={grade}>
              <input
                type="checkbox"
                value={grade}
                onChange={(e) => {
                  const grades = filterParams.gradeYears || [];
                  if (e.target.checked) {
                    setFilterParams({ gradeYears: [...grades, grade] });
                  } else {
                    setFilterParams({ 
                      gradeYears: grades.filter(g => g !== grade) 
                    });
                  }
                }}
              />
              {grade}학년
            </label>
          ))}
        </div>
      )}

      {/* 입학년도별 */}
      {filterType === 'ADMISSION_YEAR' && (
        <div>
          <input
            type="number"
            placeholder="입학년도 (예: 2025)"
            onChange={(e) => {
              const year = parseInt(e.target.value);
              if (year) {
                setFilterParams({ admissionYears: [year] });
              }
            }}
          />
        </div>
      )}
    </div>
  );
}
```

---

## 📜 NotificationHistory.jsx

```jsx
import { useState, useEffect } from 'react';

export default function NotificationHistory() {
  const [history, setHistory] = useState([]);
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);

  useEffect(() => {
    fetchHistory();
  }, [page]);

  const fetchHistory = async () => {
    try {
      const response = await fetch('/api/admin/notifications/history', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          'Authorization': `Bearer ${localStorage.getItem('accessToken')}`
        },
        body: JSON.stringify({ page, size: 20 })
      });
      
      const data = await response.json();
      setHistory(data.content);
      setTotalPages(data.totalPages);
    } catch (error) {
      alert('이력 조회 오류');
    }
  };

  return (
    <div>
      <h3>전송 이력</h3>
      <table>
        <thead>
          <tr>
            <th>제목</th>
            <th>필터</th>
            <th>대상</th>
            <th>성공</th>
            <th>실패</th>
            <th>전송일시</th>
          </tr>
        </thead>
        <tbody>
          {history.map(item => (
            <tr key={item.id}>
              <td>{item.title}</td>
              <td>{item.filterType}</td>
              <td>{item.targetCount}명</td>
              <td>{item.successCount}명</td>
              <td>{item.failureCount}명</td>
              <td>{new Date(item.sentAt).toLocaleString()}</td>
            </tr>
          ))}
        </tbody>
      </table>

      {/* 페이징 */}
      <div>
        <button 
          onClick={() => setPage(p => Math.max(0, p - 1))}
          disabled={page === 0}
        >
          이전
        </button>
        <span>{page + 1} / {totalPages}</span>
        <button 
          onClick={() => setPage(p => p + 1)}
          disabled={page >= totalPages - 1}
        >
          다음
        </button>
      </div>
    </div>
  );
}
```

---

## 🧭 AdNav.jsx 수정

```jsx
// 기존 코드에서 "푸시알림" 부분 수정
<ul>
  <li>통계자료</li>
  <ul>
    <li>열람실</li>
    <li onClick={() => setCurrentPage('푸시알림')}>푸시알림</li>
    <li onClick={() => setCurrentPage('강의안내문')}>강의안내문</li>
    <li onClick={() => setCurrentPage('강의 등록')}>강의 등록</li>
  </ul>
</ul>
```

---

## 🎨 CSS 권장 구조

```css
/* NotificationSend.module.css */
.container {
  max-width: 1200px;
  margin: 0 auto;
  padding: 20px;
}

.filterSection {
  background: #f5f5f5;
  padding: 20px;
  border-radius: 8px;
  margin-bottom: 20px;
}

.previewButton {
  background: #4CAF50;
  color: white;
  padding: 10px 20px;
  border: none;
  border-radius: 4px;
  cursor: pointer;
}

.previewButton:disabled {
  background: #ccc;
  cursor: not-allowed;
}

.sendButton {
  background: #2196F3;
  color: white;
  padding: 12px 30px;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  font-size: 16px;
  font-weight: bold;
}

.targetCount {
  font-size: 24px;
  color: #2196F3;
  margin: 20px 0;
}
```

---

## ⚠️ 에러 처리

```javascript
const handleApiError = (error, response) => {
  if (response?.status === 401) {
    alert('로그인이 필요합니다');
    window.location.href = '/admin/login';
  } else if (response?.status === 403) {
    alert('권한이 없습니다');
  } else if (response?.status === 400) {
    alert('입력값을 확인해주세요');
  } else {
    alert('서버 오류가 발생했습니다');
  }
};
```

---

## ✅ 체크리스트

- [ ] NotificationSend.jsx 생성
- [ ] FilterSelector.jsx 생성
- [ ] NotificationHistory.jsx 생성
- [ ] AdNav.jsx 수정
- [ ] CSS 파일 생성
- [ ] Admin.jsx에서 라우팅 설정
- [ ] 권한 체크 추가
- [ ] 에러 처리 추가
- [ ] 로딩 상태 UI 추가
- [ ] 테스트 (각 필터 타입별)

---

## 📚 더 보기

- [API 빠른 참조](./FCM-Filter-API-Quick-Reference.md)
- [상세 API 명세서](./FCM-Filter-API-Specification.md)
